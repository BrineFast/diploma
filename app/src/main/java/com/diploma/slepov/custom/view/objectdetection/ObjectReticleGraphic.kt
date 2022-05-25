package com.diploma.slepov.custom.view.objectdetection

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Cap
import android.graphics.Paint.Style
import androidx.core.content.ContextCompat
import com.diploma.slepov.custom.view.camera.Overlay
import com.diploma.slepov.custom.view.camera.Overlay.Graphic
import com.diploma.slepov.custom.R
import com.diploma.slepov.custom.view.camera.RecitleAnimator

/** Отрисовка круга детектирования для отображения прогресса **/
internal class ObjectReticleGraphic(overlay: Overlay, private val animator: RecitleAnimator) :
    Graphic(overlay) {

    private val outerFill: Paint
    private val outerPaint: Paint
    private val innerPaint: Paint
    private val ripplePaint: Paint
    private val outerFillRadius: Int
    private val outerRingRadius: Int
    private val innerRingRaduis: Int
    private val rippleSize: Int
    private val rippleWidth: Int
    private val rippleAlpha: Int

    /** Конструктор, задающий исходные параметры круга относительно соотношения сторон экрана и предпросмотра **/
    init {
        val resources = overlay.resources
        outerFill = Paint().apply {
            style = Style.FILL
            color = ContextCompat.getColor(context, R.color.object_reticle_outer_ring_fill)
        }

        outerPaint = Paint().apply {
            style = Style.STROKE
            strokeWidth = resources.getDimensionPixelOffset(R.dimen.object_reticle_outer_ring_stroke_width).toFloat()
            strokeCap = Cap.ROUND
            color = ContextCompat.getColor(context, R.color.object_reticle_outer_ring_stroke)
        }

        innerPaint = Paint().apply {
            style = Style.STROKE
            strokeWidth = resources.getDimensionPixelOffset(R.dimen.object_reticle_inner_ring_stroke_width).toFloat()
            strokeCap = Cap.ROUND
            color = ContextCompat.getColor(context, R.color.white)
        }

        ripplePaint = Paint().apply {
            style = Style.STROKE
            color = ContextCompat.getColor(context, R.color.reticle_ripple)
        }

        outerFillRadius = resources.getDimensionPixelOffset(R.dimen.object_reticle_outer_ring_fill_radius)
        outerRingRadius = resources.getDimensionPixelOffset(R.dimen.object_reticle_outer_ring_stroke_radius)
        innerRingRaduis = resources.getDimensionPixelOffset(R.dimen.object_reticle_inner_ring_stroke_radius)
        rippleSize = resources.getDimensionPixelOffset(R.dimen.object_reticle_ripple_size_offset)
        rippleWidth = resources.getDimensionPixelOffset(R.dimen.object_reticle_ripple_stroke_width)
        rippleAlpha = ripplePaint.alpha
    }

    /** Отрисовка круга **/
    override fun draw(canvas: Canvas) {
        val cx = canvas.width / 2f
        val cy = canvas.height / 2f
        canvas.drawCircle(cx, cy, outerFillRadius.toFloat(), outerFill)
        canvas.drawCircle(cx, cy, outerRingRadius.toFloat(), outerPaint)
        canvas.drawCircle(cx, cy, innerRingRaduis.toFloat(), innerPaint)

        ripplePaint.alpha = (rippleAlpha * animator.angleScale).toInt()
        ripplePaint.strokeWidth = rippleWidth * animator.widthScale
        val radius = outerRingRadius + rippleSize * animator.sizeScale
        canvas.drawCircle(cx, cy, radius, ripplePaint)
    }
}
