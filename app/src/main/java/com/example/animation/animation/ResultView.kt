package com.example.animation.animation

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class ResultView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val progressRect = RectF()
    private val progressPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeWidth = PROGRESS_WIDTH // todo take this from styleables
        strokeCap = Paint.Cap.ROUND
        color = Color.GREEN // todo take this from styleables
    }
    var progressAngle: Float = 0.0f
        set(value) {
            field = value
            invalidate()
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
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.drawArc(progressRect, 270f, progressAngle, false, progressPaint)
    }

    companion object {
        private const val PROGRESS_WIDTH = 40f // todo remove this
    }

}