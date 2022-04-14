package com.diploma.slepov.custom.view.camera

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/** Custom animator for the object reticle in live camera.  */
class CameraReticleAnimator(graphicOverlay: GraphicOverlay) {

    /** Returns the scale value of ripple alpha ranges in [0, 1].  */
    var rippleAlphaScale = 0f
        private set

    /** Returns the scale value of ripple size ranges in [0, 1].  */
    var rippleSizeScale = 0f
        private set

    /** Returns the scale value of ripple stroke width ranges in [0, 1].  */
    var rippleStrokeWidthScale = 1f
        private set

    private val animatorSet: AnimatorSet

    init {
        val rippleFadeInAnimator = ValueAnimator.ofFloat(0f, 1f).setDuration(300L)
        rippleFadeInAnimator.addUpdateListener { animation ->
            rippleAlphaScale = animation.animatedValue as Float
            graphicOverlay.postInvalidate()
        }

        val rippleFadeOutAnimator = ValueAnimator.ofFloat(1f, 0f).setDuration(500L)
        rippleFadeOutAnimator.startDelay = 700L
        rippleFadeOutAnimator.addUpdateListener { animation ->
            rippleAlphaScale = animation.animatedValue as Float
            graphicOverlay.postInvalidate()
        }

        val rippleExpandAnimator = ValueAnimator.ofFloat(0f, 1f).setDuration(800L)
        rippleExpandAnimator.startDelay = 300L
        rippleExpandAnimator.interpolator = FastOutSlowInInterpolator()
        rippleExpandAnimator.addUpdateListener { animation ->
            rippleSizeScale = animation.animatedValue as Float
            graphicOverlay.postInvalidate()
        }

        val rippleStrokeWidthShrinkAnimator =
            ValueAnimator.ofFloat(1f, 0.5f).setDuration(800L)
        rippleStrokeWidthShrinkAnimator.startDelay = 300L
        rippleStrokeWidthShrinkAnimator.interpolator = FastOutSlowInInterpolator()
        rippleStrokeWidthShrinkAnimator.addUpdateListener { animation ->
            rippleStrokeWidthScale = animation.animatedValue as Float
            graphicOverlay.postInvalidate()
        }

        val fakeAnimatorForRestartDelay = ValueAnimator.ofInt(0, 0).setDuration(1300L)
        fakeAnimatorForRestartDelay.startDelay = 1100L
        animatorSet = AnimatorSet()
        animatorSet.playTogether(
            rippleFadeInAnimator,
            rippleFadeOutAnimator,
            rippleExpandAnimator,
            rippleStrokeWidthShrinkAnimator,
            fakeAnimatorForRestartDelay
        )
    }

    fun start() {
        if (!animatorSet.isRunning) animatorSet.start()
    }

    fun cancel() {
        animatorSet.cancel()
        rippleAlphaScale = 0f
        rippleSizeScale = 0f
        rippleStrokeWidthScale = 1f
    }
}
