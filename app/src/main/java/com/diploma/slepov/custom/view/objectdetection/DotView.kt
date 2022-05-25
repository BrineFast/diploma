package com.diploma.slepov.custom.view.objectdetection

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.diploma.slepov.custom.R

/** Класс отрисовки точек на найденных объектах в статическом режиме **/
class DotView @JvmOverloads constructor(context: Context, selected: Boolean = false) : View(context) {

    private val paint: Paint = Paint().apply {
        style = Paint.Style.FILL
    }
    private val dotRadius: Int =
        context.resources.getDimensionPixelOffset(R.dimen.static_image_dot_radius_unselected)
    private val offset: Int

    private var currentRadiusOffset: Float = 0.toFloat()

    init {
        val selectedDotRadius = context.resources.getDimensionPixelOffset(R.dimen.static_image_dot_radius_selected)
        offset = selectedDotRadius - dotRadius
        currentRadiusOffset = (if (selected) offset else 0).toFloat()
    }

    /** Анимация нажатия на точку **/
    fun selectedAnimation(selected: Boolean) {
        val radiusOffsetAnimator: ValueAnimator =
            if (selected) {
                ValueAnimator.ofFloat(0f, offset.toFloat())
                    .setDuration(110).apply {
                        startDelay = 70
                    }
            } else {
                ValueAnimator.ofFloat(offset.toFloat(), 0f)
                    .setDuration(70)
            }

        radiusOffsetAnimator.interpolator = FastOutSlowInInterpolator()
        radiusOffsetAnimator.addUpdateListener { animation ->
            currentRadiusOffset = animation.animatedValue as Float
            invalidate()
        }
        radiusOffsetAnimator.start()
    }

    /** Отрисовка точек **/
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        paint.color = Color.WHITE
        canvas.drawCircle(cx, cy, dotRadius + currentRadiusOffset, paint)
    }
}
