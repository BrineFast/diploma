package com.diploma.slepov.custom.processor

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskExecutors
import com.diploma.slepov.custom.addOnFailureListener
import com.diploma.slepov.custom.addOnSuccessListener
import com.diploma.slepov.custom.CameraInputInfo
import com.diploma.slepov.custom.InputInfo
import com.diploma.slepov.custom.view.camera.Overlay
import com.google.mlkit.vision.common.InputImage
import java.nio.ByteBuffer

/** Класс с методами обработки изображения **/
abstract class ImageProcessorBase<T> : ImageProcessor {

    private var lastImage: ByteBuffer? = null

    private var lastImageMeta: ImageMeta? = null

    private var currentImage: ByteBuffer? = null

    private var currentImageMeta: ImageMeta? = null
    private val executor = SubsequentExecutor(TaskExecutors.MAIN_THREAD)

    /** Метод начала обработки изображения */
    @Synchronized
    override fun process(data: ByteBuffer, imageMeta: ImageMeta, Overlay: Overlay) {
        lastImage = data
        lastImageMeta = imageMeta
        if (currentImage == null && currentImageMeta == null) {
            processLatestFrame(Overlay)
        }
    }

    /** Метод покадровой обработки изображения для последующего детектирования */
    @Synchronized
    private fun processLatestFrame(Overlay: Overlay) {
        currentImage = lastImage
        currentImageMeta = lastImageMeta
        lastImage = null
        lastImageMeta = null
        val frame = currentImage ?: return
        val imgMeta = currentImageMeta ?: return
        val lastImage = InputImage.fromByteBuffer(
            frame,
            imgMeta.width,
            imgMeta.height,
            imgMeta.rotation,
            InputImage.IMAGE_FORMAT_NV21
        )

        detectInImage(lastImage)
            .addOnSuccessListener(executor) { results: T ->
                this@ImageProcessorBase.onSuccess(CameraInputInfo(frame, imgMeta), results, Overlay)
                processLatestFrame(Overlay)
            }
            .addOnFailureListener(executor) { e -> OnFailureListener { this@ImageProcessorBase.onFailure(it) } }
    }

    /** Стандартный метод закрытия **/
    override fun stop() {
        executor.shutdown()
    }

    protected abstract fun detectInImage(lastImage: InputImage): Task<T>

    /** Метод выхода при успешном детектировании объекта */
    protected abstract fun onSuccess(inputInfo: InputInfo, results: T, Overlay: Overlay)

    /** Метод выхода при недаучном детектировании объекта */
    protected abstract fun onFailure(e: Exception)
}
