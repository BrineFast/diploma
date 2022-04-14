package com.diploma.slepov.custom.view.objectdetection

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Rect
import android.util.Log
import com.diploma.slepov.custom.InputInfo
import com.google.mlkit.vision.objects.DetectedObject
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Holds the detected object and its related image info.
 */
class DetectedObjectInfo(
    private val detectedObject: DetectedObject,
    val objectIndex: Int,
    private val inputInfo: InputInfo
) {

    private var bitmap: Bitmap? = null
    private var jpegBytes: ByteArray? = null

    val objectId: Int? = detectedObject.trackingId
    val boundingBox: Rect = detectedObject.boundingBox

    val imageData: ByteArray?
        @Synchronized get() {
            if (jpegBytes == null) {
                try {
                    ByteArrayOutputStream().use { stream ->
                        getBitmap().compress(CompressFormat.JPEG, /* quality= */ 100, stream)
                        jpegBytes = stream.toByteArray()
                    }
                } catch (e: IOException) {
                    return null
                }
            }
            return jpegBytes
        }

    @Synchronized
    fun getBitmap(): Bitmap {
        return bitmap ?: let {
            val image_width = 640
            val boundingBox = detectedObject.boundingBox
            val createdBitmap = Bitmap.createBitmap(
                inputInfo.getBitmap(),
                boundingBox.left,
                boundingBox.top,
                boundingBox.width(),
                boundingBox.height()
            )
            if (createdBitmap.width > image_width) {
                val dstHeight = (image_width.toFloat() / createdBitmap.width * createdBitmap.height).toInt()
                bitmap = Bitmap.createScaledBitmap(createdBitmap, image_width, dstHeight, /* filter= */ false)
            }
            createdBitmap
        }
    }

    companion object {
        fun hasValidLabels(detectedObject: DetectedObject): Boolean {
            return detectedObject.labels.isNotEmpty() &&
                    detectedObject.labels.none { label -> label.text == "N/A" }
        }
    }
}
