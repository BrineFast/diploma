package com.diploma.slepov.custom.view

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.YuvImage
import android.hardware.Camera
import android.net.Uri
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.exifinterface.media.ExifInterface
import com.diploma.slepov.custom.view.camera.SizePair
import com.google.mlkit.vision.common.InputImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.ArrayList
import kotlin.math.abs

/** Вспомогательные методы, использующиеся во всех модулях  **/
object Utils {

    /** Запрос необходимых приложению прав при запуске **/
    internal fun requestPermissions(activity: Activity) {

        val allNeededPermissions = getRequiredPermissions(activity).filter {
            checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (allNeededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity, allNeededPermissions.toTypedArray(), /* requestCode= */ 0
            )
        }
    }

    /** Проверка всех необходимых прав **/
    internal fun permissionsGranted(context: Context): Boolean = getRequiredPermissions(
        context
    )
        .all { checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    /** Вспомогательный метод для проверки прав **/
    private fun getRequiredPermissions(context: Context): Array<String> {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            val ps = info.requestedPermissions
            if (ps != null && ps.isNotEmpty()) ps else arrayOf()
        } catch (e: Exception) {
            arrayOf()
        }
    }

    /** Проверка ориентации экрана **/
    fun isPortraitMode(context: Context): Boolean =
        context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    /** Получение возможных размеров и соотношений сторон экрана предварительного просмотра в динамическом режиме **/
    fun makeValidPreviewSize(camera: Camera): List<SizePair> {
        val parameters = camera.parameters
        val supportedPreviewSizes = parameters.supportedPreviewSizes
        val supportedPictureSizes = parameters.supportedPictureSizes
        val validPreviewSizes = ArrayList<SizePair>()
        for (previewSize in supportedPreviewSizes) {
            val previewAspectRatio = previewSize.width.toFloat() / previewSize.height.toFloat()

            for (pictureSize in supportedPictureSizes) {
                val pictureAspectRatio = pictureSize.width.toFloat() / pictureSize.height.toFloat()
                if (abs(previewAspectRatio - pictureAspectRatio) < 0.01f) {
                    validPreviewSizes.add(SizePair(previewSize, pictureSize))
                    break
                }
            }
        }
        if (validPreviewSizes.isEmpty()) {
            for (previewSize in supportedPreviewSizes) {
                validPreviewSizes.add(SizePair(previewSize, null))
            }
        }

        return validPreviewSizes
    }

    /** Закругление краев изображений **/
    fun roundImageCorners(srcBitmap: Bitmap, cornerRadius: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(srcBitmap.width, srcBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.isAntiAlias = true
        val rectF = RectF(0f, 0f, srcBitmap.width.toFloat(), srcBitmap.height.toFloat())
        canvas.drawRoundRect(rectF, cornerRadius.toFloat(), cornerRadius.toFloat(), paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(srcBitmap, 0f, 0f, paint)
        return bitmap
    }

    /** Метод для перевода изображения в BitMap **/
    fun convertToBitmap(data: ByteBuffer, width: Int, height: Int, rotationDegrees: Int): Bitmap? {
        data.rewind()
        val imageInBuffer = ByteArray(data.limit())
        data.get(imageInBuffer, 0, imageInBuffer.size)
        try {
            val image = YuvImage(
                imageInBuffer, InputImage.IMAGE_FORMAT_NV21, width, height, null
            )
            val stream = ByteArrayOutputStream()
            image.compressToJpeg(Rect(0, 0, width, height), 80, stream)
            val bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size())
            stream.close()

            val matrix = Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        } catch (e: java.lang.Exception) { }
        return null
    }

    /** Открытие внутреннего хранилища для выбора изображения в статическом режиме **/
    internal fun openInternalStorage(activity: Activity) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        activity.startActivityForResult(intent, 1)
    }

    /** Загрузить найденное изображение **/
    @Throws(IOException::class)
    internal fun loadImage(context: Context, imageUri: Uri, maxImageDimension: Int): Bitmap? {
        var inputStreamForSize: InputStream? = null
        var inputStreamForImage: InputStream? = null
        try {
            inputStreamForSize = context.contentResolver.openInputStream(imageUri)
            var opts = BitmapFactory.Options()
            opts.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStreamForSize, null, opts)/* outPadding= */
            val inSampleSize = Math.max(opts.outWidth / maxImageDimension, opts.outHeight / maxImageDimension)

            opts = BitmapFactory.Options()
            opts.inSampleSize = inSampleSize
            inputStreamForImage = context.contentResolver.openInputStream(imageUri)
            val decodedBitmap = BitmapFactory.decodeStream(inputStreamForImage, null, opts)/* outPadding= */
            return bitmapTransform(
                context.contentResolver,
                imageUri,
                decodedBitmap
            )
        } finally {
            inputStreamForSize?.close()
            inputStreamForImage?.close()
        }
    }

    /** Всопомогательный метод для перевода изображения в BitMap.
     * Используется для приведения всех полученных изображений к одинаковому размеру и соотношению сторон **/
    private fun bitmapTransform(resolver: ContentResolver, uri: Uri, bitmap: Bitmap?): Bitmap? {
        val matrix: Matrix? = when (getCurrentOrientation(resolver, uri)) {
            ExifInterface.ORIENTATION_UNDEFINED, ExifInterface.ORIENTATION_NORMAL -> null
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> Matrix().apply { postScale(-1.0f, 1.0f) }

            ExifInterface.ORIENTATION_ROTATE_90 -> Matrix().apply { postRotate(90f) }
            ExifInterface.ORIENTATION_TRANSPOSE -> Matrix().apply { postScale(-1.0f, 1.0f) }
            ExifInterface.ORIENTATION_ROTATE_180 -> Matrix().apply { postRotate(180.0f) }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> Matrix().apply { postScale(1.0f, -1.0f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> Matrix().apply { postRotate(-90.0f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> Matrix().apply {
                postRotate(-90.0f)
                postScale(-1.0f, 1.0f)
            }
            else -> null
        }

        return if (matrix != null) {
            Bitmap.createBitmap(bitmap!!, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    /** Вспомогательный метод для получения текущей ориентации полученного изображения **/
    private fun getCurrentOrientation(resolver: ContentResolver, imageUri: Uri): Int {
        if (ContentResolver.SCHEME_CONTENT != imageUri.scheme && ContentResolver.SCHEME_FILE != imageUri.scheme) {
            return 0
        }
        var exif: ExifInterface? = null
        try {
            resolver.openInputStream(imageUri)?.use { inputStream -> exif = ExifInterface(inputStream) }
        } catch (e: IOException) {}

        return if (exif != null) {
            exif!!.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } else {
            ExifInterface.ORIENTATION_UNDEFINED
        }
    }
}
