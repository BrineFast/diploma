package com.diploma.slepov.custom.view.productsearch

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Rect
import com.diploma.slepov.custom.R
import com.diploma.slepov.custom.processor.Product
import com.diploma.slepov.custom.view.Utils
import com.diploma.slepov.custom.view.objectdetection.DetectedInfo

/** Класс, содержащий информацию о найденых объектах  */
class SearchedObject(
        resources: Resources,
        private val detectedObject: DetectedInfo,
        val productList: List<Product>
) {

    private val objectThumbnailCornerRadius: Int = resources.getDimensionPixelOffset(R.dimen.bounding_box_corner_radius)
    private var objectThumbnail: Bitmap? = null

    val objectIndex: Int
        get() = detectedObject.objectIndex

    val boundingBox: Rect
        get() = detectedObject.boundingBox

    @Synchronized
    fun getObjectThumbnail(): Bitmap = objectThumbnail ?: let {
        Utils.roundImageCorners(detectedObject.getBitmap(), objectThumbnailCornerRadius)
            .also { objectThumbnail = it }
    }
}
