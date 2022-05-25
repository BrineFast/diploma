package com.diploma.slepov.custom.view.objectdetection

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Paint.Style
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader.TileMode
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.diploma.slepov.custom.view.camera.Overlay
import com.diploma.slepov.custom.view.camera.Overlay.Graphic
import com.diploma.slepov.custom.R
import com.google.mlkit.vision.objects.DetectedObject

/** Класс для анимирования области детектирования объекта **/
internal class DetectionAreaGraphic(
    overlay: Overlay,
    private val visionObject: DetectedObject,
    private val confirmationController: ConfirmationController
) : Graphic(overlay) {

    private val scrimPaint: Paint = Paint()
    private val eraserPaint: Paint
    private val boxPaint: Paint

    @ColorInt
    private val boxGradientStartColor: Int

    @ColorInt
    private val boxGradientEndColor: Int
    private val boxCornerRadius: Int

    init {
        scrimPaint.shader = if (confirmationController.confirmed) {
            LinearGradient(
                0f,
                0f,
                overlay.width.toFloat(),
                overlay.height.toFloat(),
                ContextCompat.getColor(context, R.color.object_confirmed_bg_gradient_start),
                ContextCompat.getColor(context, R.color.object_confirmed_bg_gradient_end),
                TileMode.CLAMP
            )
        } else {
            LinearGradient(
                0f,
                0f,
                overlay.width.toFloat(),
                overlay.height.toFloat(),
                ContextCompat.getColor(context, R.color.object_detected_bg_gradient_start),
                ContextCompat.getColor(context, R.color.object_detected_bg_gradient_end),
                TileMode.CLAMP
            )
        }

        eraserPaint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }

        boxPaint = Paint().apply {
            style = Style.STROKE
            strokeWidth = context
                .resources
                .getDimensionPixelOffset(
                    if (confirmationController.confirmed) {
                        R.dimen.bounding_box_confirmed_stroke_width
                    } else {
                        R.dimen.bounding_box_stroke_width
                    }
                ).toFloat()
            color = Color.WHITE
        }

        boxGradientStartColor = ContextCompat.getColor(context, R.color.bounding_box_gradient_start)
        boxGradientEndColor = ContextCompat.getColor(context, R.color.bounding_box_gradient_end)
        boxCornerRadius = context.resources.getDimensionPixelOffset(R.dimen.bounding_box_corner_radius)
    }

    override fun draw(canvas: Canvas) {
        val rect = overlay.translateRect(visionObject.boundingBox)

        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), scrimPaint)
        canvas.drawRoundRect(rect, boxCornerRadius.toFloat(), boxCornerRadius.toFloat(), eraserPaint)

        boxPaint.shader = if (confirmationController.confirmed) {
            null
        } else {
            LinearGradient(
                rect.left,
                rect.top,
                rect.left,
                rect.bottom,
                boxGradientStartColor,
                boxGradientEndColor,
                TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect, boxCornerRadius.toFloat(), boxCornerRadius.toFloat(), boxPaint)
    }
}
