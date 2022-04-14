package com.diploma.slepov.custom.processor

import androidx.annotation.GuardedBy
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskExecutors
import com.diploma.slepov.custom.addOnFailureListener
import com.diploma.slepov.custom.addOnSuccessListener
import com.diploma.slepov.custom.CameraInputInfo
import com.diploma.slepov.custom.InputInfo
import com.diploma.slepov.custom.view.camera.GraphicOverlay
import com.google.mlkit.vision.common.InputImage
import java.nio.ByteBuffer

/** Abstract base class of [FrameProcessor].  */
abstract class FrameProcessorBase<T> : FrameProcessor {

    // To keep the latest frame and its metadata.
    @GuardedBy("this")
    private var latestFrame: ByteBuffer? = null

    @GuardedBy("this")
    private var latestFrameMetaData: FrameMetadata? = null

    // To keep the frame and metadata in process.
    @GuardedBy("this")
    private var processingFrame: ByteBuffer? = null

    @GuardedBy("this")
    private var processingFrameMetaData: FrameMetadata? = null
    private val executor = ScopedExecutor(TaskExecutors.MAIN_THREAD)

    @Synchronized
    override fun process(
        data: ByteBuffer,
        frameMetadata: FrameMetadata,
        graphicOverlay: GraphicOverlay
    ) {
        latestFrame = data
        latestFrameMetaData = frameMetadata
        if (processingFrame == null && processingFrameMetaData == null) {
            processLatestFrame(graphicOverlay)
        }
    }

    @Synchronized
    private fun processLatestFrame(graphicOverlay: GraphicOverlay) {
        processingFrame = latestFrame
        processingFrameMetaData = latestFrameMetaData
        latestFrame = null
        latestFrameMetaData = null
        val frame = processingFrame ?: return
        val frameMetaData = processingFrameMetaData ?: return
        val image = InputImage.fromByteBuffer(
            frame,
            frameMetaData.width,
            frameMetaData.height,
            frameMetaData.rotation,
            InputImage.IMAGE_FORMAT_NV21
        )

        detectInImage(image)
            .addOnSuccessListener(executor) { results: T ->
                this@FrameProcessorBase.onSuccess(CameraInputInfo(frame, frameMetaData), results, graphicOverlay)
                processLatestFrame(graphicOverlay)
            }
            .addOnFailureListener(executor) { e -> OnFailureListener { this@FrameProcessorBase.onFailure(it) } }
    }

    override fun stop() {
        executor.shutdown()
    }

    protected abstract fun detectInImage(image: InputImage): Task<T>

    /** Be called when the detection succeeds.  */
    protected abstract fun onSuccess(
        inputInfo: InputInfo,
        results: T,
        graphicOverlay: GraphicOverlay
    )

    protected abstract fun onFailure(e: Exception)
}