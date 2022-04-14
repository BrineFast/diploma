package com.diploma.slepov.custom.view.objectdetection

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.diploma.slepov.custom.R

/** Represents a detected object by drawing a circle dot at the center of object's bounding box.  */
class StaticObjectDotView @JvmOverloads constructor(context: Context, selected: Boolean = false) : View(context) {

    private val paint: Paint = Paint().apply {
        style = Paint.Style.FILL
    }
    private val unselectedDotRadius: Int =
        context.resources.getDimensionPixelOffset(R.dimen.static_image_dot_radius_unselected)
    private val radiusOffsetRange: Int

    private var currentRadiusOffset: Float = 0.toFloat()

    init {
        val selectedDotRadius = context.resources.getDimensionPixelOffset(R.dimen.static_image_dot_radius_selected)
        radiusOffsetRange = selectedDotRadius - unselectedDotRadius
        currentRadiusOffset = (if (selected) radiusOffsetRange else 0).toFloat()
    }

    fun playAnimationWithSelectedState(selected: Boolean) {
        val radiusOffsetAnimator: ValueAnimator =
            if (selected) {
                ValueAnimator.ofFloat(0f, radiusOffsetRange.toFloat())
                    .setDuration(DOT_SELECTION_ANIMATOR_DURATION_MS).apply {
                        startDelay = DOT_DESELECTION_ANIMATOR_DURATION_MS
                    }
            } else {
                ValueAnimator.ofFloat(radiusOffsetRange.toFloat(), 0f)
                    .setDuration(DOT_DESELECTION_ANIMATOR_DURATION_MS)
            }

        radiusOffsetAnimator.interpolator = FastOutSlowInInterpolator()
        radiusOffsetAnimator.addUpdateListener { animation ->
            currentRadiusOffset = animation.animatedValue as Float
            invalidate()
        }
        radiusOffsetAnimator.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        paint.color = Color.WHITE
        canvas.drawCircle(cx, cy, unselectedDotRadius + currentRadiusOffset, paint)
    }

    companion object {
        private const val DOT_SELECTION_ANIMATOR_DURATION_MS: Long = 116
        private const val DOT_DESELECTION_ANIMATOR_DURATION_MS: Long = 67
    }
}
