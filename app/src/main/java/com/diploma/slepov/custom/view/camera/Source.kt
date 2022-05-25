package com.diploma.slepov.custom.view.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.Parameters
import android.preference.PreferenceManager
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.WindowManager
import com.google.android.gms.common.images.Size
import com.diploma.slepov.custom.R
import com.diploma.slepov.custom.view.Utils
import com.diploma.slepov.custom.processor.ImageMeta
import com.diploma.slepov.custom.processor.ImageProcessor
import java.io.IOException
import java.nio.ByteBuffer
import java.util.IdentityHashMap
import kotlin.math.abs
import kotlin.math.ceil

@Suppress("DEPRECATION")
class Source(private val Overlay: Overlay) {

    private var camera: Camera? = null
    private var rotationDegrees: Int = 0

    internal var previewSize: Size? = null
        private set

    private var processingThread: Thread? = null
    private val processingRunnable = FrameProcessingRunnable()

    private val processorLock = Object()
    private var imageProcessor: ImageProcessor? = null

    private val bytesToByteBuffer = IdentityHashMap<ByteArray, ByteBuffer>()
    private val context: Context = Overlay.context

    @Synchronized
    @Throws(IOException::class)
    internal fun start(surfaceHolder: SurfaceHolder) {
        if (camera != null) return

        camera = createCamera().apply {
            setPreviewDisplay(surfaceHolder)
            startPreview()
        }

        processingThread = Thread(processingRunnable).apply {
            processingRunnable.setActive(true)
            start()
        }
    }

    @Synchronized
    internal fun stop() {
        processingRunnable.setActive(false)
        processingThread?.let {
            try {
                it.join()
            } catch (e: InterruptedException) {}
            processingThread = null
        }

        camera?.let {
            it.stopPreview()
            it.setPreviewCallbackWithBuffer(null)
            try {
                it.setPreviewDisplay(null)
            } catch (e: Exception) {}
            it.release()
            camera = null
        }

        bytesToByteBuffer.clear()
    }

    fun release() {
        Overlay.clear()
        synchronized(processorLock) {
            stop()
            imageProcessor?.stop()
        }
    }

    fun setFrameProcessor(processor: ImageProcessor) {
        Overlay.clear()
        synchronized(processorLock) {
            imageProcessor?.stop()
            imageProcessor = processor
        }
    }

    fun updateFlashMode(flashMode: String) {
        val parameters = camera?.parameters
        parameters?.flashMode = flashMode
        camera?.parameters = parameters
    }

    @Throws(IOException::class)
    private fun createCamera(): Camera {
        val camera = Camera.open() ?: throw IOException("There is no back-facing camera.")
        val parameters = camera.parameters
        setPreviewAndPictureSize(camera, parameters)
        setRotation(camera, parameters)

        val previewFpsRange = selectPreviewFpsRange(camera)
            ?: throw IOException("Could not find suitable preview frames per second range.")
        parameters.setPreviewFpsRange(
            previewFpsRange[Parameters.PREVIEW_FPS_MIN_INDEX],
            previewFpsRange[Parameters.PREVIEW_FPS_MAX_INDEX]
        )

        parameters.previewFormat = IMAGE_FORMAT

        if (parameters.supportedFocusModes.contains(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.focusMode = Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
        } else {}

        camera.parameters = parameters

        camera.setPreviewCallbackWithBuffer(processingRunnable::setNextFrame)

        previewSize?.let {
            camera.addCallbackBuffer(createPreviewBuffer(it))
            camera.addCallbackBuffer(createPreviewBuffer(it))
            camera.addCallbackBuffer(createPreviewBuffer(it))
            camera.addCallbackBuffer(createPreviewBuffer(it))
        }

        return camera
    }

    @Throws(IOException::class)
    private fun setPreviewAndPictureSize(camera: Camera, parameters: Parameters) {

        val previewSizePrefKey = context.getString(R.string.pref_key_rear_camera_preview_size)
        val pictureSizePrefKey = context.getString(R.string.pref_key_rear_camera_picture_size)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val sizePair: SizePair = SizePair(
            Size.parseSize(sharedPreferences.getString(previewSizePrefKey, null)!!),
            Size.parseSize(sharedPreferences.getString(pictureSizePrefKey, null)!!)
        ) ?: run {
            val displayAspectRatioInLandscape: Float =
                if (Utils.isPortraitMode(Overlay.context)) {
                    Overlay.height.toFloat() / Overlay.width
                } else {
                    Overlay.width.toFloat() / Overlay.height
                }
            selectSizePair(camera, displayAspectRatioInLandscape)
        } ?: throw IOException("Could not find suitable preview size.")


        previewSize = sizePair.preview.also {
            parameters.setPreviewSize(it.width, it.height)
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(context.getString(R.string.pref_key_rear_camera_preview_size), it.toString())
                .apply()
        }

        sizePair.picture?.let { pictureSize ->
            parameters.setPictureSize(pictureSize.width, pictureSize.height)
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(context.getString(R.string.pref_key_rear_camera_picture_size), pictureSize.toString())
                .apply()
        }
    }

    private fun setRotation(camera: Camera, parameters: Parameters) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val degrees = when (val deviceRotation = windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> {
                0
            }
        }

        val cameraInfo = CameraInfo()
        Camera.getCameraInfo(CAMERA_FACING_BACK, cameraInfo)
        val angle = (cameraInfo.orientation - degrees + 360) % 360
        this.rotationDegrees = angle
        camera.setDisplayOrientation(angle)
        parameters.setRotation(angle)
    }
    private fun createPreviewBuffer(previewSize: Size): ByteArray {
        val bitsPerPixel = ImageFormat.getBitsPerPixel(IMAGE_FORMAT)
        val sizeInBits = previewSize.height.toLong() * previewSize.width.toLong() * bitsPerPixel.toLong()
        val bufferSize = ceil(sizeInBits / 8.0).toInt() + 1

        val byteArray = ByteArray(bufferSize)
        val byteBuffer = ByteBuffer.wrap(byteArray)
        check(!(!byteBuffer.hasArray() || !byteBuffer.array()!!.contentEquals(byteArray))) {
            "Failed to create valid buffer for camera source."
        }

        bytesToByteBuffer[byteArray] = byteBuffer
        return byteArray
    }

    private inner class FrameProcessingRunnable internal constructor() : Runnable {

        private val lock = Object()
        private var active = true

        private var pendingFrameData: ByteBuffer? = null

        internal fun setActive(active: Boolean) {
            synchronized(lock) {
                this.active = active
                lock.notifyAll()
            }
        }

        internal fun setNextFrame(data: ByteArray, camera: Camera) {
            synchronized(lock) {
                pendingFrameData?.let {
                    camera.addCallbackBuffer(it.array())
                    pendingFrameData = null
                }

                if (!bytesToByteBuffer.containsKey(data)) {
                    return
                }

                pendingFrameData = bytesToByteBuffer[data]

                lock.notifyAll()
            }
        }

        override fun run() {
            var data: ByteBuffer?

            while (true) {
                synchronized(lock) {
                    while (active && pendingFrameData == null) {
                        try {
                            lock.wait()
                        } catch (e: InterruptedException) {
                            return
                        }
                    }

                    if (!active) {
                        return
                    }
                    data = pendingFrameData
                    pendingFrameData = null
                }

                try {
                    synchronized(processorLock) {
                        val frameMetadata = ImageMeta(previewSize!!.width, previewSize!!.height, rotationDegrees)
                        data?.let {
                            imageProcessor?.process(it, frameMetadata, Overlay)
                        }
                    }
                } catch (t: Exception) {} finally {
                    data?.let {
                        camera?.addCallbackBuffer(it.array())
                    }
                }
            }
        }
    }

    companion object {

        const val CAMERA_FACING_BACK = CameraInfo.CAMERA_FACING_BACK

        private const val IMAGE_FORMAT = ImageFormat.NV21
        private const val MIN_CAMERA_PREVIEW_WIDTH = 400
        private const val MAX_CAMERA_PREVIEW_WIDTH = 1300
        private const val DEFAULT_REQUESTED_CAMERA_PREVIEW_WIDTH = 640
        private const val DEFAULT_REQUESTED_CAMERA_PREVIEW_HEIGHT = 360
        private const val REQUESTED_CAMERA_FPS = 30.0f

        private fun selectSizePair(camera: Camera, displayAspectRatioInLandscape: Float): SizePair? {
            val validPreviewSizes = Utils.makeValidPreviewSize(camera)

            var selectedPair: SizePair? = null
            var minAspectRatioDiff = Float.MAX_VALUE

            for (sizePair in validPreviewSizes) {
                val previewSize = sizePair.preview
                if (previewSize.width < MIN_CAMERA_PREVIEW_WIDTH || previewSize.width > MAX_CAMERA_PREVIEW_WIDTH) {
                    continue
                }

                val previewAspectRatio = previewSize.width.toFloat() / previewSize.height.toFloat()
                val aspectRatioDiff = abs(displayAspectRatioInLandscape - previewAspectRatio)
                if (abs(aspectRatioDiff - minAspectRatioDiff) < 0.01f) {
                    if (selectedPair == null || selectedPair.preview.width < sizePair.preview.width) {
                        selectedPair = sizePair
                    }
                } else if (aspectRatioDiff < minAspectRatioDiff) {
                    minAspectRatioDiff = aspectRatioDiff
                    selectedPair = sizePair
                }
            }

            if (selectedPair == null) {
                var minDiff = Integer.MAX_VALUE
                for (sizePair in validPreviewSizes) {
                    val size = sizePair.preview
                    val diff =
                        abs(size.width - DEFAULT_REQUESTED_CAMERA_PREVIEW_WIDTH) +
                                abs(size.height - DEFAULT_REQUESTED_CAMERA_PREVIEW_HEIGHT)
                    if (diff < minDiff) {
                        selectedPair = sizePair
                        minDiff = diff
                    }
                }
            }

            return selectedPair
        }

        private fun selectPreviewFpsRange(camera: Camera): IntArray? {
            val desiredPreviewFpsScaled = (REQUESTED_CAMERA_FPS * 1000f).toInt()
            var selectedFpsRange: IntArray? = null
            var minDiff = Integer.MAX_VALUE
            for (range in camera.parameters.supportedPreviewFpsRange) {
                val deltaMin = desiredPreviewFpsScaled - range[Parameters.PREVIEW_FPS_MIN_INDEX]
                val deltaMax = desiredPreviewFpsScaled - range[Parameters.PREVIEW_FPS_MAX_INDEX]
                val diff = abs(deltaMin) + abs(deltaMax)
                if (diff < minDiff) {
                    selectedFpsRange = range
                    minDiff = diff
                }
            }
            return selectedFpsRange
        }
    }
}
