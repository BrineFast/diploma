package com.diploma.slepov.custom.view.camera

import android.hardware.Camera
import com.google.android.gms.common.images.Size

/** Класс хранящий соотношение размеров изображения и экрана
 * для избежания искажения изображения и элементов интерфейса **/
class SizePair {
    val preview: Size
    val picture: Size?

    constructor(previewSize: Camera.Size, pictureSize: Camera.Size?) {
        preview = Size(previewSize.width, previewSize.height)
        picture = pictureSize?.let { Size(it.width, it.height) }
    }

    constructor(previewSize: Size, pictureSize: Size?) {
        preview = previewSize
        picture = pictureSize
    }
}
