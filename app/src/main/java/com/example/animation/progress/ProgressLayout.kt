package com.example.animation.progress

import android.animation.AnimatorSet
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import com.example.animation.*
import kotlin.math.max

class ProgressLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var clickStrategy: ClickStrategy = ClickStrategy.CLICK_LISTENER
        private set

    private var sequenceAnimator: AnimatorSet? = null
    private var rotationAnimatorSet: AnimatorSet? = null
    private var callbacks: Callbacks? = null

    private val circularProgressView: CircularProgressView = CircularProgressView(context, attrs)
    private val cancelAnimationButton: ImageView = ImageView(context, attrs).apply {
        setImageResource(R.drawable.ic_close)
        visibility = INVISIBLE
    }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ProgressLayout,
            0,
            0
        ).run {
            try {
                val defaultProgressColor = context.resources.getColor(R.color.circular_progress_view_default_color, context.theme)
                val progressColor = getColor(R.styleable.ProgressLayout_progressColor, defaultProgressColor)
                circularProgressView.progressColor = progressColor

                val defaultProgressWidth = context.resources.getDimensionPixelOffset(R.dimen.circular_progress_view_default_progress_width)
                val progressWidth = getDimensionPixelOffset(R.styleable.ProgressLayout_progressWidth, defaultProgressWidth)
                circularProgressView.progressWidth = progressWidth
            } finally {
                recycle()
            }
        }

        clipChildren = false
        clipToPadding = false

        addView(circularProgressView)
        addView(cancelAnimationButton)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        circularProgressView.bringToFront()

        val cancelAnimationButtonSize = max(MeasureSpec.getSize(widthMeasureSpec) / 3, MeasureSpec.getSize(heightMeasureSpec) / 3)

        cancelAnimationButton.updateLayoutParams<FrameLayout.LayoutParams> {
            width = cancelAnimationButtonSize
            height = cancelAnimationButtonSize
        }
        cancelAnimationButton.bringToFront()
    }

    final override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener {
            when (clickStrategy) {
                ClickStrategy.CLICK_LISTENER -> l?.onClick(it)
                ClickStrategy.CANCEL_PROGRESS -> cancelProgress(true)
            }
        }
    }

    fun startProgress(duration: Long, callbacks: Callbacks) {
        restoreInitialState()

        this.callbacks = callbacks

        val scaleDuration = (SCALE_PERCENT_DURATION_MULTIPLIER * duration).toLong()

        val viewsToShake = children.toMutableList().apply {
            remove(cancelAnimationButton)
            remove(circularProgressView)
        }.toTypedArray()

        sequenceAnimator = AnimatorSet().apply {
            val initialRotationsAnimatorSet = AnimatorSet().apply {
                val rotations = createShakingAnimators(*viewsToShake)
                playSequentially(rotations)
            }

            val progressAnimatorSet = AnimatorSet().apply {
                val progressAnimation = createDefaultAnimator(duration, null, PropertyValuesHolder.ofFloat("progress", 0f, 360f)) {
                    circularProgressView.progressAngle = it.getAnimatedValue("progress") as Float
                }
                val zoomInAnimator = createZoomInAnimator(scaleDuration, OvershootInterpolator(), cancelAnimationButton).apply {
                    doOnStart {
                        cancelAnimationButton.visibility = View.VISIBLE
                    }
                }
                val fadeInAnimator = createFadeInAnimator(scaleDuration, null, cancelAnimationButton)

                val zoomOutAnimator: ValueAnimator = createZoomOutAnimator(scaleDuration, AnticipateOvershootInterpolator(), cancelAnimationButton)
                val fadeOutAnimator: ValueAnimator = createFadeOutAnimator(scaleDuration, null, cancelAnimationButton)
                val concealStartTime = (duration * PROGRESS_CONCEAL_DURATION_MULTIPLIER).toLong()
                val concealAnimator: AnimatorSet = AnimatorSet().apply {
                    startDelay = concealStartTime
                    playTogether(zoomOutAnimator, fadeOutAnimator)
                }
                rotationAnimatorSet = AnimatorSet().apply {
                    val rotations = createShakingAnimators(*viewsToShake)
                    playSequentially(rotations)
                    doOnEnd {
                        start()
                    }
                }
                doOnStart {
                    clickStrategy = ClickStrategy.CANCEL_PROGRESS
                    callbacks.onProgressStart()
                }
                playTogether(progressAnimation, zoomInAnimator, fadeInAnimator, concealAnimator, rotationAnimatorSet)
            }
            playSequentially(initialRotationsAnimatorSet, progressAnimatorSet)
            doOnEnd {
                callbacks.onProgressEnd()
                restoreInitialState()
            }
            start()
        }
    }

    private fun createShakingAnimators(vararg view: View): List<ValueAnimator> = listOf(
        createRotationAnimator(SHAKE_DURATION, OvershootInterpolator(), 0f, -12f, *view),
        createRotationAnimator(SHAKE_DURATION, OvershootInterpolator(), -12f, 20f, *view),
        createRotationAnimator(SHAKE_DURATION, OvershootInterpolator(), 20f, -8f, *view),
        createRotationAnimator(SHAKE_DURATION, OvershootInterpolator(), -8f, 10f, *view),
        createRotationAnimator(SHAKE_DURATION, OvershootInterpolator(), 10f, -4f, *view),
        createRotationAnimator(SHAKE_DURATION, OvershootInterpolator(), -4f, 4f, *view),
        createRotationAnimator(SHAKE_DURATION, OvershootInterpolator(), 4f, 0f, *view)
    )

    fun cancelProgress(shouldTriggerCancelCallback: Boolean) {
        if (shouldTriggerCancelCallback) {
            callbacks?.onProgressCancel()
        }
        restoreInitialState()
    }

    private fun restoreInitialState() {
        callbacks = null
        clickStrategy = ClickStrategy.CLICK_LISTENER

        sequenceAnimator?.run {
            removeAllListeners()
            cancel()
        }

        rotationAnimatorSet?.run {
            removeAllListeners()
            cancel()
        }

        children.forEach {
            it.rotation = 0f
        }

        circularProgressView.progressAngle = 0.0f

        cancelAnimationButton.scaleX = 1.0f
        cancelAnimationButton.scaleY = 1.0f
        cancelAnimationButton.visibility = View.INVISIBLE
    }

    interface Callbacks {
        fun onProgressStart()
        fun onProgressEnd()
        fun onProgressCancel()
    }

    enum class ClickStrategy {
        CLICK_LISTENER,
        CANCEL_PROGRESS
    }

    companion object {
        private const val SHAKE_DURATION = 300L
        private const val SCALE_PERCENT_DURATION_MULTIPLIER = 0.07
        private const val PROGRESS_CONCEAL_DURATION_MULTIPLIER = 0.66f
    }

}