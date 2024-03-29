package com.diploma.slepov.custom.view.camera

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import com.google.android.gms.common.images.Size
import com.diploma.slepov.custom.R
import com.diploma.slepov.custom.view.Utils
import java.io.IOException

/** Класс отрисовки изображения с камеры **/
class CameraPreview(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    private val surfaceView: SurfaceView = SurfaceView(context).apply {
        holder.addCallback(SurfaceCallback())
        addView(this)
    }
    private var Overlay: Overlay? = null
    private var startRequested = false
    private var surfaceAvailable = false
    private var cameraSource: Source? = null
    private var cameraPreviewSize: Size? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        Overlay = findViewById(R.id.camera_preview_graphic_overlay)
    }

    @Throws(IOException::class)
    fun start(cameraSource: Source) {
        this.cameraSource = cameraSource
        startRequested = true
        startIfReady()
    }

    fun stop() {
        cameraSource?.let {
            it.stop()
            cameraSource = null
            startRequested = false
        }
    }

    @Throws(IOException::class)
    private fun startIfReady() {
        if (startRequested && surfaceAvailable) {
            cameraSource?.start(surfaceView.holder)
            requestLayout()
            Overlay?.let { overlay ->
                cameraSource?.let {
                    overlay.setCameraInfo(it)
                }
                overlay.clear()
            }
            startRequested = false
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val layoutWidth = right - left
        val layoutHeight = bottom - top

        cameraSource?.previewSize?.let { cameraPreviewSize = it }

        val previewSizeRatio = cameraPreviewSize?.let { size ->
            if (Utils.isPortraitMode(context)) {
                size.height.toFloat() / size.width
            } else {
                size.width.toFloat() / size.height
            }
        } ?: layoutWidth.toFloat() / layoutHeight.toFloat()

        val childHeight = (layoutWidth / previewSizeRatio).toInt()
        if (childHeight <= layoutHeight) {
            for (i in 0 until childCount) {
                getChildAt(i).layout(0, 0, layoutWidth, childHeight)
            }
        } else {
            val excessLenInHalf = (childHeight - layoutHeight) / 2
            for (i in 0 until childCount) {
                val childView = getChildAt(i)
                when (childView.id) {
                    R.id.static_overlay_container -> {
                        childView.layout(0, 0, layoutWidth, layoutHeight)
                    }
                    else -> {
                        childView.layout(
                            0, -excessLenInHalf, layoutWidth, layoutHeight + excessLenInHalf
                        )
                    }
                }
            }
        }

        try {
            startIfReady()
        } catch (e: IOException) {
            Log.e(TAG, "Could not start camera source.", e)
        }
    }

    private inner class SurfaceCallback : SurfaceHolder.Callback {
        override fun surfaceCreated(surface: SurfaceHolder) {
            surfaceAvailable = true
            try {
                startIfReady()
            } catch (e: IOException) {
                Log.e(TAG, "Could not start camera source.", e)
            }
        }

        override fun surfaceDestroyed(surface: SurfaceHolder) {
            surfaceAvailable = false
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        }
    }

    companion object {
        private const val TAG = "CameraSourcePreview"
    }
}
