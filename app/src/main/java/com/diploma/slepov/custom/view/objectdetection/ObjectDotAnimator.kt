package com.diploma.slepov.custom.view.objectdetection

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.diploma.slepov.custom.view.camera.GraphicOverlay

/**
 * Custom animator for the object dot.
 */
internal class ObjectDotAnimator(graphicOverlay: GraphicOverlay) {

    private val animatorSet: AnimatorSet

    /** Returns the scale value of dot radius ranges in [0, 1].  */
    var radiusScale = 0f
        private set

    /** Returns the scale value of dot alpha ranges in [0, 1].  */
    var alphaScale = 0f
        private set

    init {
        val dotScaleUpAnimator = ValueAnimator.ofFloat(0f, 1.3f)
            .setDuration(200L)
        dotScaleUpAnimator.interpolator = FastOutSlowInInterpolator()
        dotScaleUpAnimator.addUpdateListener { animation ->
            radiusScale = animation.animatedValue as Float
            graphicOverlay.postInvalidate()
        }

        val dotScaleDownAnimator = ValueAnimator.ofFloat(1.3f, 1f)
            .setDuration(800L)
        dotScaleDownAnimator.startDelay = 200L
        dotScaleDownAnimator.interpolator = PathInterpolatorCompat
            .create(0.4f, 0f, 0f, 1f)
        dotScaleDownAnimator.addUpdateListener { animation ->
            radiusScale = animation.animatedValue as Float
            graphicOverlay.postInvalidate()
        }

        val dotFadeInAnimator = ValueAnimator.ofFloat(0f, 1f)
            .setDuration(200L)
        dotFadeInAnimator.addUpdateListener { animation ->
            alphaScale = animation.animatedValue as Float
            graphicOverlay.postInvalidate()
        }

        animatorSet = AnimatorSet()
        animatorSet.playTogether(dotScaleUpAnimator, dotScaleDownAnimator, dotFadeInAnimator)
    }

    fun start() {
        if (!animatorSet.isRunning) {
            animatorSet.start()
        }
    }
}
