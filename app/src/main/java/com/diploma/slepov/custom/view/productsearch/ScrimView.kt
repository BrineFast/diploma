package com.diploma.slepov.custom.view.productsearch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.common.base.Preconditions.checkArgument
import com.diploma.slepov.custom.R

/** Класс для отрисовка списка найденных продуктов при динамичном режиме детектирования **/
class ScrimView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val scrimPaint: Paint
    private val thumbnailPaint: Paint
    private val boxPaint: Paint
    private val thumbnailHeight: Int
    private val thumbnailMargin: Int
    private val boxCornerRadius: Int

    private var bitmap: Bitmap? = null
    private var rect: RectF? = null
    private var collapsedPercent: Float = 0f

    init {
        val resources = context.resources
        scrimPaint = Paint().apply {
            color = ContextCompat.getColor(context, R.color.dark)
        }

        thumbnailPaint = Paint()

        boxPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = resources.getDimensionPixelOffset(R.dimen.object_thumbnail_stroke_width).toFloat()
            color = Color.WHITE
        }

        thumbnailHeight = resources.getDimensionPixelOffset(R.dimen.object_thumbnail_height)
        thumbnailMargin = resources.getDimensionPixelOffset(R.dimen.object_thumbnail_margin)
        boxCornerRadius = resources.getDimensionPixelOffset(R.dimen.bounding_box_corner_radius)
    }

    fun updateWithThumbnailTranslate(
        bitmap: Bitmap,
        collapsedStateHeight: Int,
        slideOffset: Float,
        bottomSheet: View
    ) {
        this.bitmap = bitmap

        val currentSheetHeight: Float
        if (slideOffset < 0) {
            collapsedPercent = -slideOffset
            currentSheetHeight = collapsedStateHeight * (1 + slideOffset)
        } else {
            collapsedPercent = 0f
            currentSheetHeight = collapsedStateHeight + (bottomSheet.height - collapsedStateHeight) * slideOffset
        }

        rect = RectF().apply {
            val thumbnailWidth =
                bitmap.width.toFloat() / bitmap.height.toFloat() * thumbnailHeight.toFloat()
            left = thumbnailMargin.toFloat()
            top = height.toFloat() - currentSheetHeight - thumbnailMargin.toFloat() - thumbnailHeight.toFloat()
            right = left + thumbnailWidth
            bottom = top + thumbnailHeight
        }

        invalidate()
    }

    fun updateWithThumbnailTranslateAndScale(
        bitmap: Bitmap,
        collapsedStateHeight: Int,
        slideOffset: Float,
        srcrect: RectF
    ) {
        checkArgument(
            slideOffset <= 0
        )

        this.bitmap = bitmap
        this.collapsedPercent = 0f

        rect = RectF().apply {
            val dstX = thumbnailMargin.toFloat()
            val dstY = (height - collapsedStateHeight - thumbnailMargin - thumbnailHeight).toFloat()
            val dstHeight = thumbnailHeight.toFloat()
            val dstWidth = srcrect.width() / srcrect.height() * dstHeight
            val dstRect = RectF(dstX, dstY, dstX + dstWidth, dstY + dstHeight)

            val progressToCollapsedState = 1 + slideOffset
            left = srcrect.left + (dstRect.left - srcrect.left) * progressToCollapsedState
            top = srcrect.top + (dstRect.top - srcrect.top) * progressToCollapsedState
            right = srcrect.right + (dstRect.right - srcrect.right) * progressToCollapsedState
            bottom = srcrect.bottom + (dstRect.bottom - srcrect.bottom) * progressToCollapsedState
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val bitmap = bitmap ?: return
        val rect = rect ?: return
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
        if (collapsedPercent < 0.42f) {
            val alpha = ((1 - collapsedPercent / 0.42f) * 255).toInt()

            thumbnailPaint.alpha = alpha
            canvas.drawBitmap(bitmap, null, rect, thumbnailPaint)

            boxPaint.alpha = alpha
            canvas.drawRoundRect(rect, boxCornerRadius.toFloat(), boxCornerRadius.toFloat(), boxPaint)
        }
    }
}
