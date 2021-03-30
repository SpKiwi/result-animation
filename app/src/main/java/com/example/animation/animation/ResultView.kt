package com.example.animation.animation

import android.animation.AnimatorSet
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.animation.R

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

    private val progressRect = RectF()

    private val progressPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeWidth = PROGRESS_WIDTH
        strokeCap = Paint.Cap.ROUND
        color = Color.GREEN
    }

    private val closePaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeWidth = CLOSE_WIDTH
        strokeCap = Paint.Cap.ROUND
        color = Color.BLACK
    }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        textSize = resources.getDimensionPixelOffset(R.dimen.result_text_size).toFloat() // todo change to percentages?
        textAlign = Paint.Align.CENTER
    }

    private var textXPos = -1f
    private var textYPos = -1f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val height = MeasureSpec.getSize(heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)

        val horizontalCenter = (width / 2).toFloat()
        val verticalCenter = (height / 2).toFloat()

        // When calculating radius we need to consider line width (stroke), so that the view would fit exactly
        val radius: Float
        radius = if (height >= width) {
            (width / 2).toFloat() - (PROGRESS_WIDTH / 2)
        } else {
            (height / 2).toFloat() - (PROGRESS_WIDTH / 2)
        }

        progressRect.set(
            horizontalCenter - radius,
            verticalCenter - radius,
            horizontalCenter + radius,
            verticalCenter + radius
        )

        textXPos = (width / 2).toFloat()
        textYPos = ((height / 2) - ((closePaint.descent() + closePaint.ascent()) / 2))
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.let { drawProgress(it) }
    }

    private fun drawProgress(canvas: Canvas) {
        if (progressAnimator.isRunning) {
            val progressAngle = progressAnimator.getAnimatedValue(KEY_PROGRESS_ANGLE_VALUE_HOLDER) as Float
            val progressPercentage = progressAnimator.getAnimatedValue(KEY_PROGRESS_PERCENTAGE_VALUE_HOLDER) as Float
            val progressTime = progressAnimator.getAnimatedValue(KEY_PROGRESS_TIME_VALUE_HOLDER) as Int

            canvas.drawArc(progressRect, 270f, progressAngle, false, progressPaint)
            canvas.drawText(progressTime.toString(), textXPos, textYPos, textPaint)
        }
    }

    private val progressAnimator: ValueAnimator = ValueAnimator()

    fun changeProgressValues(mainAnimationDuration: Long = DEFAULT_MAIN_ANIMATION_DURATION) {
        if (mainAnimationDuration <= 0) {
            throw IllegalStateException("Animation duration should be greater than zero")
        }

        val progressPercentageValueHolder = PropertyValuesHolder.ofFloat(KEY_PROGRESS_PERCENTAGE_VALUE_HOLDER, 0f, 1f)
        val progressAngleValueHolder = PropertyValuesHolder.ofFloat(KEY_PROGRESS_ANGLE_VALUE_HOLDER, 0f, 360f)
        val progressTimeValueHolder = PropertyValuesHolder.ofInt(KEY_PROGRESS_TIME_VALUE_HOLDER, mainAnimationDuration.toInt() / 1_000, 0)
        progressAnimator.apply {
            setValues(progressPercentageValueHolder, progressAngleValueHolder, progressTimeValueHolder)
            duration = mainAnimationDuration
            addUpdateListener { invalidate() }
        }

        AnimatorSet().apply {
            playSequentially(progressAnimator)
            start()
        }
    }

    interface AutofollowListener {
        fun onAutofollowStart()
        fun onAutofollow()
        fun onAutofollowEnd()
    }

    companion object {
        private const val PROGRESS_WIDTH = 40f // todo change to percentages OR convert from dp to px
        private const val CLOSE_WIDTH = 10f // todo change to percentages OR convert from dp to px
        private const val CLOSE_SIZE_PERCENTAGE = 0.33f

        private const val KEY_PROGRESS_PERCENTAGE_VALUE_HOLDER = "KEY_PROGRESS_PERCENTAGE_VALUE_HOLDER"
        private const val KEY_PROGRESS_ANGLE_VALUE_HOLDER = "KEY_PROGRESS_ANGLE_VALUE_HOLDER"
        private const val KEY_PROGRESS_TIME_VALUE_HOLDER = "KEY_PROGRESS_TIME_VALUE_HOLDER"

        private const val DEFAULT_MAIN_ANIMATION_DURATION = 5_000L
        private const val DEFAULT_SECONDARY_ANIMATION_DURATION = 1_000L
        private const val CLOSE_ANIMATION_START_VALUE = 0.75
    }

}