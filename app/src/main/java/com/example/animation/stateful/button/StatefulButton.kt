package com.example.animation.stateful.button

import android.animation.AnimatorSet
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.animation.doOnCancel
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import com.example.animation.*

class StatefulButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val text: TextView by lazy { findViewById<TextView>(R.id.stateful_button_text) }
    private val progressBar: ProgressBar by lazy { findViewById<ProgressBar>(R.id.stateful_button_progress) }
    private val successImage: ImageView by lazy { findViewById<ImageView>(R.id.stateful_button_success_image) }

    private var sequenceAnimatorSet: AnimatorSet? = null

    init {
        View.inflate(context, R.layout.stateful_button, this)
    }

    var state: State = State.TEXT
        set(value) {
            if (field != value) {
                field = value
                val newView = resolveCurrentState(field)
                animateViews(newView)
            }
        }

    private var currentStateElement: View = resolveCurrentState(state)

    private fun resolveCurrentState(state: State): View = when (state) {
        State.TEXT -> text
        State.PROGRESS -> progressBar
        State.SUCCESS -> successImage
    }

    private fun animateViews(newView: View) {
        restoreInitialState()

        val zoomOutAnimator = createZoomOutAnimator(DEFAULT_ANIMATION_DURATION_MILLIS, AnticipateOvershootInterpolator(), currentStateElement)
        val fadeOutAnimator = createFadeOutAnimator(DEFAULT_ANIMATION_DURATION_MILLIS, null, currentStateElement)
        val disappearAnimatorSet = AnimatorSet().apply {
            playTogether(zoomOutAnimator, fadeOutAnimator)
        }

        val zoomInAnimator = createZoomInAnimator(DEFAULT_ANIMATION_DURATION_MILLIS, OvershootInterpolator(), newView)
        val fadeInAnimator = createFadeInAnimator(DEFAULT_ANIMATION_DURATION_MILLIS, null, newView)
        val appearAnimatorSet = AnimatorSet().apply {
            doOnStart {
                currentStateElement.visibility = View.INVISIBLE
                newView.visibility = View.VISIBLE
            }
            playTogether(zoomInAnimator, fadeInAnimator)
        }

        sequenceAnimatorSet = AnimatorSet().apply {
            playSequentially(disappearAnimatorSet, appearAnimatorSet)
            start()
            doOnCancel {
                currentStateElement = newView
            }
            doOnEnd {
                currentStateElement = newView
            }
        }
    }

    private fun restoreInitialState() {
        sequenceAnimatorSet?.run {
            removeAllListeners()
            cancel()
            end()
        }

        if (text !== currentStateElement) {
            text.restoreInitialState()
        }

        if (progressBar !== currentStateElement) {
            progressBar.restoreInitialState()
        }

        if (successImage !== currentStateElement) {
            successImage.restoreInitialState()
        }
    }

    private fun View.restoreInitialState() {
        scaleX = 1f
        scaleY = 1f
        alpha = 1f
        visibility = View.INVISIBLE
    }

    enum class State {
        TEXT,
        PROGRESS,
        SUCCESS
    }

    companion object {
        private const val DEFAULT_ANIMATION_DURATION_MILLIS = 150L
    }

}