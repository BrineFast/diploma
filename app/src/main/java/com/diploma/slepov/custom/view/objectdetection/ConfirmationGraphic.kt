package com.diploma.slepov.custom.view.objectdetection

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Cap
import android.graphics.Paint.Style
import android.graphics.RectF
import androidx.core.content.ContextCompat
import com.diploma.slepov.custom.view.camera.Overlay
import com.diploma.slepov.custom.view.camera.Overlay.Graphic
import com.diploma.slepov.custom.R

/** Класс для визуализации прогресса подтверджения детектирования объекта **/
class ConfirmationGraphic internal constructor(
    overlay: Overlay,
    private val controller: ConfirmationController
) : Graphic(overlay) {

    private val ringFill: Paint
    private val ringStroke: Paint
    private val ringPaint: Paint
    private val progressRing: Paint
    private val ringRadius: Int
    private val ringRadiusFill: Int
    private val ringRadiusStroke: Int

    /** Конструктор, задающий исходные параметры кольца прогресса подтверждения детектирования **/
    init {
        val resources = overlay.resources
        ringFill = Paint().apply {
            style = Style.FILL
            color = ContextCompat.getColor(context, R.color.object_reticle_outer_ring_fill)
        }

        ringStroke = Paint().apply {
            style = Style.STROKE
            strokeWidth = resources.getDimensionPixelOffset(R.dimen.object_reticle_outer_ring_stroke_width).toFloat()
            strokeCap = Cap.ROUND
            color = ContextCompat.getColor(context, R.color.object_reticle_outer_ring_stroke)
        }

        progressRing = Paint().apply {
            style = Style.STROKE
            strokeWidth = resources.getDimensionPixelOffset(R.dimen.object_reticle_outer_ring_stroke_width).toFloat()
            strokeCap = Cap.ROUND
            color = ContextCompat.getColor(context, R.color.white)
        }

        ringPaint = Paint()

        ringPaint.style = Style.STROKE
        ringPaint.strokeWidth =
            resources.getDimensionPixelOffset(R.dimen.object_reticle_inner_ring_stroke_width).toFloat()
        ringPaint.strokeCap = Cap.ROUND
        ringPaint.color = ContextCompat.getColor(context, R.color.white)

        ringRadiusFill = resources.getDimensionPixelOffset(R.dimen.object_reticle_outer_ring_fill_radius)
        ringRadiusStroke = resources.getDimensionPixelOffset(R.dimen.object_reticle_outer_ring_stroke_radius)
        ringRadius = resources.getDimensionPixelOffset(R.dimen.object_reticle_inner_ring_stroke_radius)
    }

    /** Отрисовка кольца прогресса на основе полученных параметров **/
    override fun draw(canvas: Canvas) {
        val cx = canvas.width / 2f
        val cy = canvas.height / 2f
        canvas.drawCircle(cx, cy, ringRadiusFill.toFloat(), ringFill)
        canvas.drawCircle(cx, cy, ringRadiusStroke.toFloat(), ringStroke)
        canvas.drawCircle(cx, cy, ringRadius.toFloat(), ringPaint)

        val progressRect = RectF(
            cx - ringRadiusStroke,
            cy - ringRadiusStroke,
            cx + ringRadiusStroke,
            cy + ringRadiusStroke
        )
        val sweepAngle = controller.detectionProgress * 360
        canvas.drawArc(
            progressRect,
            0f,
            sweepAngle,
            false,
            progressRing
        )
    }
}
