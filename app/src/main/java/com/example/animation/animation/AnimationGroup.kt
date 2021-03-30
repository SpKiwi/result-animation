package com.example.animation.animation

import android.animation.AnimatorSet
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
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

//    - Появляется circular view, крест, таймер
//    - Начинается анимация (цифры + progress bar)
//    - на 75%(80?) начинается fade out у цифр. Убирается крестик (увеличение + fade out)
//    - На 100% происходит заполнение circle in
//    - После circle in увеличивается чекбокс (взрыв)
//    - После 1 секунды увеличение и исчезновение кнопки, а потом ее замена на исходный элемент

//    private var fullAnimationDuration: Long = -1
//    private lateinit var progressBarTimeRange: LongRange
//    private lateinit var closeButtonTimeRange: LongRange
//    private lateinit var resultAnimationTimeRange: LongRange


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

        val progressPercentageValueHolder = PropertyValuesHolder.ofFloat(KEY_PROGRESS_PERCENTAGE_VALUE_HOLDER, 0f, 1f)
        val progressAngleValueHolder = PropertyValuesHolder.ofFloat(KEY_PROGRESS_ANGLE_VALUE_HOLDER, 0f, 360f)
        val progressTimeValueHolder = PropertyValuesHolder.ofInt(KEY_PROGRESS_TIME_VALUE_HOLDER, (mainAnimationDurationMillis / 1_000).toInt(), 0)
        progressAnimator.apply {
            setValues(progressPercentageValueHolder, progressAngleValueHolder, progressTimeValueHolder)
            duration = mainAnimationDurationMillis
            addUpdateListener {
                animateProgress()
            }
        }

        closeButton.visibility = View.VISIBLE


        val progressRevealValueHolder = PropertyValuesHolder.ofFloat(KEY_PROGRESS_REVEAL_VALUE_HOLDER, 0f, 1f)
        val revealAnimator = ValueAnimator().apply {
            setValues(progressRevealValueHolder)
            duration = (mainAnimationDurationMillis * 0.1).toLong()
            interpolator = OvershootInterpolator()
            addUpdateListener {
                val revealValue = it.getAnimatedValue(KEY_PROGRESS_REVEAL_VALUE_HOLDER) as Float
                animateProgressReveal(revealValue)
            }
        }
        val concealAnimator = ValueAnimator().apply {
            duration = (mainAnimationDurationMillis * 0.15).toLong()
            interpolator = OvershootInterpolator()
            addUpdateListener {
                animateProgressConceal()
            }
        }

        revealAnimator.start()
        AnimatorSet().apply {
            playSequentially(progressAnimator)
            start()
        }
    }

    private fun animateProgress() {
        val progressAngle = progressAnimator.getAnimatedValue(KEY_PROGRESS_ANGLE_VALUE_HOLDER) as Float
        val progressPercentage = progressAnimator.getAnimatedValue(KEY_PROGRESS_PERCENTAGE_VALUE_HOLDER) as Float
        val progressTimeMillis = progressAnimator.getAnimatedValue(KEY_PROGRESS_TIME_VALUE_HOLDER) as Int

        progressView.progressAngle = progressAngle
        progressTimer.text = progressTimeMillis.toString()
    }

    private fun animateProgressReveal(revealValue: Float) {
        closeButton.scaleX = revealValue
        closeButton.scaleY = revealValue
        closeButton.alpha = revealValue
        progressTimer.scaleX = revealValue
        progressTimer.scaleY = revealValue
        progressTimer.alpha = revealValue
    }

    private fun animateProgressConceal() {
        TODO()
    }

    interface AutofollowListener {
        fun onAutofollowStart()
        fun onAutofollow()
        fun onAutofollowEnd()
    }

    companion object {
        private const val KEY_PROGRESS_PERCENTAGE_VALUE_HOLDER = "KEY_PROGRESS_PERCENTAGE_VALUE_HOLDER"
        private const val KEY_PROGRESS_ANGLE_VALUE_HOLDER = "KEY_PROGRESS_ANGLE_VALUE_HOLDER"
        private const val KEY_PROGRESS_TIME_VALUE_HOLDER = "KEY_PROGRESS_TIME_VALUE_HOLDER"
        private const val KEY_PROGRESS_REVEAL_VALUE_HOLDER = "KEY_PROGRESS_REVEAL_VALUE_HOLDER"

        private const val DEFAULT_MAIN_ANIMATION_DURATION = 5_000L
        private const val DEFAULT_SECONDARY_ANIMATION_DURATION = 1_000L
        private const val CLOSE_ANIMATION_START_VALUE = 0.75
    }
}