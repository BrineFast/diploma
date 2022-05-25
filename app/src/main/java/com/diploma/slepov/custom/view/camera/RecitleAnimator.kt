package com.diploma.slepov.custom.view.camera

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/** Аниматор выделения области и прогресса детектирования объекта **/
class RecitleAnimator(Overlay: Overlay) {

    var angleScale = 0f
    var sizeScale = 0f
    var widthScale = 1f

    private val animatorSet: AnimatorSet

    init {
        val fadeIn = ValueAnimator.ofFloat(0f, 1f).setDuration(300L)
        fadeIn.addUpdateListener { animation ->
            angleScale = animation.animatedValue as Float
            Overlay.postInvalidate()
        }

        val fadeOut = ValueAnimator.ofFloat(1f, 0f).setDuration(500L)
        fadeOut.startDelay = 700L
        fadeOut.addUpdateListener { animation ->
            angleScale = animation.animatedValue as Float
            Overlay.postInvalidate()
        }

        val expand = ValueAnimator.ofFloat(0f, 1f).setDuration(800L)
        expand.startDelay = 300L
        expand.interpolator = FastOutSlowInInterpolator()
        expand.addUpdateListener { animation ->
            sizeScale = animation.animatedValue as Float
            Overlay.postInvalidate()
        }

        val width =
            ValueAnimator.ofFloat(1f, 0.5f).setDuration(800L)
        width.startDelay = 300L
        width.interpolator = FastOutSlowInInterpolator()
        width.addUpdateListener { animation ->
            widthScale = animation.animatedValue as Float
            Overlay.postInvalidate()
        }

        val restartDelay = ValueAnimator.ofInt(0, 0).setDuration(1300L)
        restartDelay.startDelay = 1100L
        animatorSet = AnimatorSet()
        animatorSet.playTogether(
            fadeIn,
            fadeOut,
            expand,
            width,
            restartDelay
        )
    }

    fun start() {
        if (!animatorSet.isRunning) animatorSet.start()
    }

    fun cancel() {
        animatorSet.cancel()
        angleScale = 0f
        sizeScale = 0f
        widthScale = 1f
    }
}
