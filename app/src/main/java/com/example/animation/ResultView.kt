package com.example.animation

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View

class ResultView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

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

    private val viewSpace = RectF()
    private val lineWidth = 40f

    private val activeArcPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeWidth = lineWidth
        strokeCap = Paint.Cap.ROUND
        color = Color.GREEN
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val height = MeasureSpec.getSize(heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)

        val horizontalCenter = (width / 2).toFloat()
        val verticalCenter = (height / 2).toFloat()

        // When calculating radius we need to consider line width (stroke), so that the view would fit exactly
        val radius: Float
        radius = if (height >= width) {
            (width / 2).toFloat() - (lineWidth / 2)
        } else {
            (height / 2).toFloat() - (lineWidth / 2)
        }

        viewSpace.set(
            horizontalCenter - radius,
            verticalCenter - radius,
            horizontalCenter + radius,
            verticalCenter + radius
        )
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.let { drawProgress(it) }
    }

    private fun drawProgress(canvas: Canvas) {
        if (currentAnimationTimeStamp in progressBarTimeRange) {
            val percentageToFill = (360 * (currentAnimationTimeStamp.toFloat() / progressBarTimeRange.last))
            canvas.drawArc(viewSpace, 270f, percentageToFill, false, activeArcPaint)
        }
    }

    private fun drawCloseButton(canvas: Canvas) {
        
    }

    private var progressBarTimeRange: LongRange = LongRange.EMPTY
    private var closeTimeRange: LongRange = LongRange.EMPTY
    private var resultAnimationTimeRange: LongRange = LongRange.EMPTY

    private var fullAnimationDuration: Long = -1
    private var currentAnimationTimeStamp: Long = -1

    fun animateProgress(mainAnimationDuration: Long = DEFAULT_MAIN_ANIMATION_DURATION) {
        if (mainAnimationDuration <= 0) {
            throw IllegalStateException("Animation duration should be greater than zero")
        }
        fullAnimationDuration = mainAnimationDuration + DEFAULT_SECONDARY_ANIMATION_DURATION

        progressBarTimeRange = 0..mainAnimationDuration
        closeTimeRange = (mainAnimationDuration * CLOSE_ANIMATION_START_VALUE).toLong()..mainAnimationDuration
        resultAnimationTimeRange = mainAnimationDuration..fullAnimationDuration

        val valuesHolder = PropertyValuesHolder.ofFloat(KEY_PROGRESS_PERCENTAGE_VALUE_HOLDER, 0f, 1f)
        val animator = ValueAnimator().apply {
            setValues(valuesHolder)
            duration = fullAnimationDuration
            addUpdateListener {
                val fullAnimationPercentage = it.getAnimatedValue(KEY_PROGRESS_PERCENTAGE_VALUE_HOLDER) as Float
                currentAnimationTimeStamp = (fullAnimationDuration * fullAnimationPercentage).toLong()
                invalidate()
            }
        }
        animator.start()
    }

    interface AutofollowListener {
        fun onAutofollowStart()
        fun onAutofollow()
        fun onAutofollowEnd()
    }

    companion object {
        private const val KEY_PROGRESS_PERCENTAGE_VALUE_HOLDER = "KEY_PROGRESS_PERCENTAGE_VALUE_HOLDER"
        private const val DEFAULT_MAIN_ANIMATION_DURATION = 5_000L
        private const val DEFAULT_SECONDARY_ANIMATION_DURATION = 1_000L
        private const val CLOSE_ANIMATION_START_VALUE = 0.75
    }

}