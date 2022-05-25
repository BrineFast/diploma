package com.diploma.slepov.custom

import android.graphics.Bitmap
import com.diploma.slepov.custom.processor.ImageMeta
import com.diploma.slepov.custom.view.Utils
import java.nio.ByteBuffer

interface InputInfo {
    fun getBitmap(): Bitmap
}

/** Вспомогательный класс для конвертации полученного изображения в BitMap **/
class CameraInputInfo(
    private val frameByteBuffer: ByteBuffer,
    private val imageMeta: ImageMeta
) : InputInfo {

    private var bitmap: Bitmap? = null

    @Synchronized
    override fun getBitmap(): Bitmap {
        return bitmap ?: let {
            bitmap = Utils.convertToBitmap(
                frameByteBuffer, imageMeta.width, imageMeta.height, imageMeta.rotation
            )
            bitmap!!
        }
    }
}

class BitmapInputInfo(private val bitmap: Bitmap) : InputInfo {
    override fun getBitmap(): Bitmap {
        return bitmap
    }
}
