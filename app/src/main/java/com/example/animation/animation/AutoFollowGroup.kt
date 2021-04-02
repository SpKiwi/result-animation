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
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import com.example.animation.R

class AutoFollowGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val progressView: AutoFollowProgressView by lazy { findViewById<AutoFollowProgressView>(R.id.auto_follow_view) }
    private val closeButton: ImageView by lazy { findViewById<ImageView>(R.id.auto_follow_close_image) }
    private val progressTimer: TextView by lazy { findViewById<TextView>(R.id.auto_follow_timer_text) }
    private val checkboxImage: ImageView by lazy { findViewById<ImageView>(R.id.auto_follow_checkbox) }

    init {
        View.inflate(context, R.layout.auto_follow_group, this)
    }

    private var sequenceAnimator: AnimatorSet? = null
    private var progressAnimator: ValueAnimator? = null
    private lateinit var autoFollowListener: AutoFollowListener

    fun startAutoFollowAnimation(mainAnimationDurationMillis: Long, autoFollowListener: AutoFollowListener) {
        if (mainAnimationDurationMillis <= 0) {
            throw IllegalStateException("Animation duration should be greater than zero")
        }

        setOnClickListener {
            restoreInitialState()
            visibility = View.INVISIBLE
            autoFollowListener.onAutoFollowCancel()
            setOnClickListener(null)
        }

        this.autoFollowListener = autoFollowListener
        restoreInitialState()

        progressAnimator = createProgressAnimator(mainAnimationDurationMillis).apply {
            doOnEnd { setOnClickListener(null) }
        }

        val scaleDuration = (mainAnimationDurationMillis * SCALE_PERCENT_DURATION_MULTIPLIER).toLong()

        val zoomInProgressElementsAnimator: ValueAnimator = createZoomInAnimator(scaleDuration, OvershootInterpolator(), closeButton)
        val fadeInProgressElementsAnimator: ValueAnimator = createFadeInAnimator(scaleDuration, null, closeButton, progressTimer)
        val revealAnimator: AnimatorSet = AnimatorSet().apply {
            playTogether(zoomInProgressElementsAnimator, fadeInProgressElementsAnimator)
        }

        val zoomOutCloseButtonAnimator: ValueAnimator = createZoomOutAnimator(scaleDuration, AnticipateOvershootInterpolator(), closeButton)
        val fadeOutCloseButtonAnimator: ValueAnimator = createFadeOutAnimator(scaleDuration, null, closeButton)
        val concealStartTime = (mainAnimationDurationMillis * PROGRESS_CONCEAL_DURATION_MULTIPLIER).toLong()
        val concealCloseButtonAnimator: AnimatorSet = AnimatorSet().apply {
            startDelay = concealStartTime
            playTogether(zoomOutCloseButtonAnimator, fadeOutCloseButtonAnimator)
        }

        val fadeProgressAnimator: ValueAnimator = createFadeOutAnimator(mainAnimationDurationMillis - concealStartTime - scaleDuration, null, progressTimer)
        val circleInAnimator: ValueAnimator = createCircleInAnimator(STROKE_INSIDE_DURATION)
        val checkboxAnimator: ValueAnimator = createZoomInAnimator(CHECKBOX_DURATION, null, checkboxImage).apply {
            doOnStart { checkboxImage.visibility = View.VISIBLE }
        }
        val viewDisappearAnimator: ValueAnimator = createZoomOutAnimator(DISAPPEAR_DURATION, AnticipateOvershootInterpolator(), this).apply {
            startDelay = DISAPPEAR_DELAY
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
                },
                AnimatorSet().apply {
                    playSequentially(concealCloseButtonAnimator, fadeProgressAnimator)
                }
            )
            doOnEnd { autoFollowListener.onAutoFollowEnd() }
            start()
        }
    }

    private fun restoreInitialState() {
        progressAnimator?.removeAllListeners()
        sequenceAnimator?.run {
            removeAllListeners()
            cancel()
            end()
        }

        progressView.run {
            restoreInitialState(View.VISIBLE)
            circleClipPercentage = null
            progressAngle = 0f
        }

        closeButton.restoreInitialState(View.VISIBLE)
        progressTimer.restoreInitialState(View.VISIBLE)
        checkboxImage.restoreInitialState(View.INVISIBLE)

        restoreInitialState(View.VISIBLE)
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

    private fun createCircleInAnimator(duration: Long): ValueAnimator =
        createDefaultAnimator(duration, null, PropertyValuesHolder.ofFloat(KEY_GENERIC_VALUE, 1f, 0f)) {
            progressView.circleClipPercentage = it.getAnimatedValue(KEY_GENERIC_VALUE) as Float
        }

    private fun createFadeInAnimator(duration: Long, interpolator: Interpolator?, vararg views: View): ValueAnimator =
        createDefaultAnimator(duration, interpolator, PropertyValuesHolder.ofFloat(KEY_GENERIC_VALUE, 0f, 1f)) { valueAnimator ->
            views.forEach { view ->
                view.alpha = valueAnimator.getAnimatedValue(KEY_GENERIC_VALUE) as Float
            }
        }

    private fun createZoomInAnimator(duration: Long, interpolator: Interpolator?, vararg views: View): ValueAnimator =
        createDefaultAnimator(duration, interpolator, PropertyValuesHolder.ofFloat(KEY_GENERIC_VALUE, 0f, 1f)) { valueAnimator ->
            views.forEach { view ->
                val scale = valueAnimator.getAnimatedValue(KEY_GENERIC_VALUE) as Float
                view.scaleX = scale
                view.scaleY = scale
            }
        }

    private fun createZoomOutAnimator(duration: Long, interpolator: Interpolator?, view: View): ValueAnimator =
        createDefaultAnimator(duration, interpolator, PropertyValuesHolder.ofFloat(KEY_GENERIC_VALUE, 1f, 0f)) { valueAnimator ->
            val scale = valueAnimator.getAnimatedValue(KEY_GENERIC_VALUE) as Float
            view.scaleX = scale
            view.scaleY = scale
        }

    private fun createFadeOutAnimator(duration: Long, interpolator: Interpolator?, view: View): ValueAnimator =
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

    private fun View.restoreInitialState(visibility: Int) {
        scaleX = 1f
        scaleY = 1f
        alpha = 1f
        setVisibility(visibility)
    }

    interface AutoFollowListener {
        fun onAutoFollowCancel()
        fun onAutofollowTimerElapsed()
        fun onAutoFollowEnd()
    }

    companion object {
        private const val KEY_GENERIC_VALUE = "KEY_GENERIC_VALUE"
        private const val KEY_PROGRESS_ANGLE_VALUE_HOLDER = "KEY_PROGRESS_ANGLE_VALUE_HOLDER"
        private const val KEY_PROGRESS_TIME_VALUE_HOLDER = "KEY_PROGRESS_TIME_VALUE_HOLDER"

        private const val STROKE_INSIDE_DURATION = 500L
        private const val CHECKBOX_DURATION = 350L
        private const val DISAPPEAR_DURATION = 350L
        private const val DISAPPEAR_DELAY = 350L

        private const val SCALE_PERCENT_DURATION_MULTIPLIER = 0.07f
        private const val PROGRESS_CONCEAL_DURATION_MULTIPLIER = 0.6f
    }
}