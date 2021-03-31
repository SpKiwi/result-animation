package com.example.animation.animation

import android.animation.AnimatorSet
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.Interpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.animation.addListener
import com.example.animation.R

class AnimationGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val progressView: ResultView by lazy { findViewById<ResultView>(R.id.result_progress) }
    private val closeButton: ImageView by lazy { findViewById<ImageView>(R.id.result_close_image) }
    private val progressTimer: TextView by lazy { findViewById<TextView>(R.id.result_timer_text) }
    private val checkboxImage: ImageView by lazy { findViewById<ImageView>(R.id.result_checkbox) }

    init {
        View.inflate(context, R.layout.animation_group, this)
    }

    private var sequenceAnimator: AnimatorSet? = null

    fun changeProgressValues(mainAnimationDurationMillis: Long = DEFAULT_MAIN_ANIMATION_DURATION) {
        if (mainAnimationDurationMillis <= 0) {
            throw IllegalStateException("Animation duration should be greater than zero")
        }

//        sequenceAnimator?.cancel()
//        sequenceAnimator?.setupStartValues()

        closeButton.visibility = View.VISIBLE
        progressTimer.visibility = View.VISIBLE

        val progressAnimator: ValueAnimator = createProgressAnimator(mainAnimationDurationMillis)

        val zoomInCloseButtonAnimator: ValueAnimator = createZoomInAnimator((mainAnimationDurationMillis * 0.1).toLong(), closeButton, OvershootInterpolator())
        val fadeInCloseButtonAnimator: ValueAnimator = createFadeInAnimator((mainAnimationDurationMillis * 0.1).toLong(), closeButton, OvershootInterpolator())
        val zoomInProgressTimerAnimator: ValueAnimator = createZoomInAnimator((mainAnimationDurationMillis * 0.1).toLong(), progressTimer, OvershootInterpolator())
        val fadeInProgressTimerAnimator: ValueAnimator = createFadeInAnimator((mainAnimationDurationMillis * 0.1).toLong(), progressTimer, OvershootInterpolator())
        val revealAnimator: AnimatorSet = AnimatorSet().apply {
            playTogether(zoomInCloseButtonAnimator, fadeInCloseButtonAnimator, zoomInProgressTimerAnimator, fadeInProgressTimerAnimator)
        }

        val zoomOutCloseButtonAnimator: ValueAnimator = createZoomOutAnimator((mainAnimationDurationMillis * 0.1).toLong(), closeButton, AnticipateOvershootInterpolator())
        val fadeOutCloseButtonAnimator: ValueAnimator = createFadeOutAnimator((mainAnimationDurationMillis * 0.1).toLong(), closeButton, AnticipateOvershootInterpolator())
        val concealCloseButtonAnimator: AnimatorSet = AnimatorSet().apply {
            startDelay = (0.6 * mainAnimationDurationMillis).toLong()
            playTogether(zoomOutCloseButtonAnimator, fadeOutCloseButtonAnimator)
        }

        val fadeProgressAnimator: ValueAnimator = createFadeOutAnimator((mainAnimationDurationMillis * 0.15).toLong(), progressTimer)
        val circleInAnimator: ValueAnimator = createCircleInAnimator(300L)
        val checkboxAnimator: ValueAnimator = createSpringInAnimator(300L, checkboxImage).apply {
            addListener(onStart = {
                checkboxImage.visibility = View.VISIBLE
            })
        }
        val viewDisappearAnimator: ValueAnimator = createSpringOutAnimator(300, this).apply {
            startDelay = 500
        }

        sequenceAnimator = AnimatorSet().apply {
            playTogether(
                AnimatorSet().apply {
                    playSequentially(
                        AnimatorSet().apply {
                            playTogether(progressAnimator, revealAnimator)
                        },
                        circleInAnimator,
                        checkboxAnimator,
                        viewDisappearAnimator
                    )
                    start()
                },
                AnimatorSet().apply {
                    play(concealCloseButtonAnimator).before(fadeProgressAnimator)
                    start()
                }
            )
        }
    }

    private fun createSpringOutAnimator(duration: Long, view: View): ValueAnimator =
        createDefaultAnimator(duration, AnticipateOvershootInterpolator(), PropertyValuesHolder.ofFloat(KEY_GENERIC_VALUE, 1f, 0f)) {
            val scalePercentage = it.getAnimatedValue(KEY_GENERIC_VALUE) as Float
            view.scaleX = scalePercentage
            view.scaleY = scalePercentage
        }

    private fun createSpringInAnimator(duration: Long, view: View): ValueAnimator =
        createDefaultAnimator(duration, OvershootInterpolator(), PropertyValuesHolder.ofFloat(KEY_GENERIC_VALUE, 0f, 1f)) {
            val scalePercentage = it.getAnimatedValue(KEY_GENERIC_VALUE) as Float
            view.scaleX = scalePercentage
            view.scaleY = scalePercentage
        }

    private fun createProgressAnimator(duration: Long): ValueAnimator {
        val progressAngleValueHolder = PropertyValuesHolder.ofFloat(KEY_PROGRESS_ANGLE_VALUE_HOLDER, 0f, 360f)
        val progressTimeValueHolder = PropertyValuesHolder.ofInt(KEY_PROGRESS_TIME_VALUE_HOLDER, (duration / 1_000).toInt(), 0)
        return ValueAnimator().apply {
            setValues(progressAngleValueHolder, progressTimeValueHolder)
            this.duration = duration
            addUpdateListener {
                val progressAngle = it.getAnimatedValue(KEY_PROGRESS_ANGLE_VALUE_HOLDER) as Float
                val progressTimeMillis = it.getAnimatedValue(KEY_PROGRESS_TIME_VALUE_HOLDER) as Int

                progressView.progressAngle = progressAngle
                progressTimer.text = progressTimeMillis.toString()
            }
        }
    }

    private fun createCircleInAnimator(duration: Long, interpolator: Interpolator? = null): ValueAnimator =
        createDefaultAnimator(duration, interpolator, PropertyValuesHolder.ofFloat(KEY_GENERIC_VALUE, 1f, 0f)) {
            progressView.circleClipPercentage = it.getAnimatedValue(KEY_GENERIC_VALUE) as Float
        }

    private fun createFadeInAnimator(duration: Long, view: View, interpolator: Interpolator? = null): ValueAnimator =
        createDefaultAnimator(duration, interpolator, PropertyValuesHolder.ofFloat(KEY_GENERIC_VALUE, 0f, 1f)) { valueAnimator ->
            view.alpha = valueAnimator.getAnimatedValue(KEY_GENERIC_VALUE) as Float
        }

    private fun createZoomInAnimator(duration: Long, view: View, interpolator: Interpolator? = null): ValueAnimator =
        createDefaultAnimator(duration, interpolator, PropertyValuesHolder.ofFloat(KEY_GENERIC_VALUE, 0f, 1f)) { valueAnimator ->
            view.scaleX = valueAnimator.getAnimatedValue(KEY_GENERIC_VALUE) as Float
            view.scaleY = valueAnimator.getAnimatedValue(KEY_GENERIC_VALUE) as Float
        }

    private fun createZoomOutAnimator(duration: Long, view: View, interpolator: Interpolator? = null): ValueAnimator =
        createDefaultAnimator(duration, interpolator, PropertyValuesHolder.ofFloat(KEY_GENERIC_VALUE, 1f, 0f)) { valueAnimator ->
            view.scaleX = valueAnimator.getAnimatedValue(KEY_GENERIC_VALUE) as Float
            view.scaleY = valueAnimator.getAnimatedValue(KEY_GENERIC_VALUE) as Float
        }

    private fun createFadeOutAnimator(duration: Long, view: View, interpolator: Interpolator? = null): ValueAnimator =
        createDefaultAnimator(duration, interpolator, PropertyValuesHolder.ofFloat(KEY_GENERIC_VALUE, 1f, 0f)) { valueAnimator ->
            view.alpha = valueAnimator.getAnimatedValue(KEY_GENERIC_VALUE) as Float
        }

    private fun createDefaultAnimator(duration: Long, interpolator: Interpolator?, vararg values: PropertyValuesHolder, valueAnimator: (ValueAnimator) -> Unit): ValueAnimator =
        ValueAnimator().apply {
            setValues(*values)
            this.duration = duration
            interpolator?.let {
                this.interpolator = interpolator
            }
            addUpdateListener(valueAnimator)
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