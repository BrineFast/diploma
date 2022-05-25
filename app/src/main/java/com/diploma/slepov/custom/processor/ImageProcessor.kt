package com.diploma.slepov.custom.processor

import com.diploma.slepov.custom.view.camera.Overlay
import java.nio.ByteBuffer

/** Интерфейс с методами обработки изображения **/
interface ImageProcessor {

    fun process(data: ByteBuffer, imageMeta: ImageMeta, Overlay: Overlay)

    fun stop()
}
