package com.example.animation.animation

import android.animation.AnimatorSet
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.example.animation.R

class AnimationGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val progressView: ResultView by lazy { findViewById<ResultView>(R.id.result_progress) }
    private val closeButton: ImageView by lazy { findViewById<ImageView>(R.id.result_close_image) }
    private val progressTimer: TextView by lazy { findViewById<TextView>(R.id.result_timer_text) }

    init {
        View.inflate(context, R.layout.animation_group, this)
    }

    private val progressAnimator: ValueAnimator = ValueAnimator()
    private var mainAnimationDurationMillis: Long = 0

    fun changeProgressValues(mainAnimationDurationMillis: Long = DEFAULT_MAIN_ANIMATION_DURATION) {
        if (mainAnimationDurationMillis <= 0) {
            throw IllegalStateException("Animation duration should be greater than zero")
        }
        this.mainAnimationDurationMillis = mainAnimationDurationMillis

        closeButton.visibility = View.VISIBLE
        progressTimer.visibility = View.VISIBLE

        val progressAnimator: ValueAnimator = createProgressAnimator()
        val revealAnimator: ValueAnimator = createRevealAnimator((mainAnimationDurationMillis * 0.1).toLong(), closeButton, progressTimer)
        val concealCloseButtonAnimator: ValueAnimator = createConcealAnimator((mainAnimationDurationMillis * 0.1).toLong(), closeButton).apply {
            startDelay = (0.6 * mainAnimationDurationMillis).toLong()
        }
        val fadeProgressAnimator: ValueAnimator = createFadeOutAnimator((mainAnimationDurationMillis * 0.15).toLong(), progressTimer)
        val circleInAnimator: ValueAnimator = createCircleInAnimator(1_000L)

        AnimatorSet().apply {
            play(progressAnimator).before(circleInAnimator)
            start()
        }
        revealAnimator.start()
        AnimatorSet().apply {
            play(concealCloseButtonAnimator).before(fadeProgressAnimator)
            start()
        }
    }

    private fun createProgressAnimator(): ValueAnimator {
        val progressAngleValueHolder = PropertyValuesHolder.ofFloat(KEY_PROGRESS_ANGLE_VALUE_HOLDER, 0f, 360f)
        val progressTimeValueHolder = PropertyValuesHolder.ofInt(KEY_PROGRESS_TIME_VALUE_HOLDER, (mainAnimationDurationMillis / 1_000).toInt(), 0)
        return progressAnimator.apply {
            setValues(progressAngleValueHolder, progressTimeValueHolder)
            duration = mainAnimationDurationMillis
            addUpdateListener {
                val progressAngle = progressAnimator.getAnimatedValue(KEY_PROGRESS_ANGLE_VALUE_HOLDER) as Float
                val progressTimeMillis = progressAnimator.getAnimatedValue(KEY_PROGRESS_TIME_VALUE_HOLDER) as Int

                progressView.progressAngle = progressAngle
                progressTimer.text = progressTimeMillis.toString()
            }
        }
    }

    private fun createRevealAnimator(duration: Long, vararg views: View): ValueAnimator {
        val revealValueHolder = PropertyValuesHolder.ofFloat(KEY_GENERIC_VALUE, 0f, 1f)
        return ValueAnimator().apply {
            setValues(revealValueHolder)
            this.duration = duration
            interpolator = OvershootInterpolator()
            addUpdateListener {
                val revealValue = it.getAnimatedValue(KEY_GENERIC_VALUE) as Float
                views.forEach { view ->
                    view.scaleX = revealValue
                    view.scaleY = revealValue
                    view.alpha = revealValue
                }
            }
        }
    }

    private fun createConcealAnimator(duration: Long, view: View): ValueAnimator {
        val concealValueHolder = PropertyValuesHolder.ofFloat(KEY_GENERIC_VALUE, 1f, 0f)
        return ValueAnimator().apply {
            setValues(concealValueHolder)
            this.duration = duration
            interpolator = AnticipateOvershootInterpolator()
            addUpdateListener {
                val concealValue = it.getAnimatedValue(KEY_GENERIC_VALUE) as Float
                view.scaleX = concealValue
                view.scaleY = concealValue
                view.alpha = concealValue
            }
        }
    }

    private fun createCircleInAnimator(duration: Long): ValueAnimator {
        val circleValueHolder = PropertyValuesHolder.ofFloat(KEY_GENERIC_VALUE, 1f, 0f)
        return ValueAnimator().apply {
            setValues(circleValueHolder)
            this.duration = duration
            addUpdateListener {
                val showPercentage = it.getAnimatedValue(KEY_GENERIC_VALUE) as Float
                progressView.circleClipPercentage = showPercentage
            }
        }
    }

    private fun createFadeOutAnimator(duration: Long, view: View): ValueAnimator {
        val fadeOutValueHolder = PropertyValuesHolder.ofFloat(KEY_GENERIC_VALUE, 0f, 1f)
        return ValueAnimator().apply {
            setValues(fadeOutValueHolder)
            this.duration = duration
            addUpdateListener {
                val fadeOutValue = it.getAnimatedValue(KEY_GENERIC_VALUE) as Float
                view.alpha = fadeOutValue
            }
        }
    }

    interface AutofollowListener {
        fun onAutofollowStart()
        fun onAutofollow()
        fun onAutofollowEnd()
    }

    companion object {
        private const val KEY_GENERIC_VALUE = "KEY_GENERIC_VALUE"
        private const val KEY_PROGRESS_ANGLE_VALUE_HOLDER = "KEY_PROGRESS_ANGLE_VALUE_HOLDER"
        private const val KEY_PROGRESS_TIME_VALUE_HOLDER = "KEY_PROGRESS_TIME_VALUE_HOLDER"

        private const val DEFAULT_MAIN_ANIMATION_DURATION = 5_000L
    }
}