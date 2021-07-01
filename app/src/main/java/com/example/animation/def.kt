package com.example.animation

import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.Interpolator


fun createFadeInAnimator(duration: Long, interpolator: Interpolator?, vararg views: View): ValueAnimator =
    createDefaultAnimator(duration, interpolator, PropertyValuesHolder.ofFloat(KEY_GENERIC_VALUE, 0f, 1f)) { valueAnimator ->
        views.forEach { view ->
            view.alpha = valueAnimator.getAnimatedValue(KEY_GENERIC_VALUE) as Float
        }
    }

fun createZoomInAnimator(duration: Long, interpolator: Interpolator?, vararg views: View): ValueAnimator =
    createDefaultAnimator(duration, interpolator, PropertyValuesHolder.ofFloat(KEY_GENERIC_VALUE, 0f, 1f)) { valueAnimator ->
        views.forEach { view ->
            val scale = valueAnimator.getAnimatedValue(KEY_GENERIC_VALUE) as Float
            view.scaleX = scale
            view.scaleY = scale
        }
    }

fun createZoomOutAnimator(duration: Long, interpolator: Interpolator?, view: View): ValueAnimator =
    createDefaultAnimator(duration, interpolator, PropertyValuesHolder.ofFloat(KEY_GENERIC_VALUE, 1f, 0f)) { valueAnimator ->
        val scale = valueAnimator.getAnimatedValue(KEY_GENERIC_VALUE) as Float
        view.scaleX = scale
        view.scaleY = scale
    }

fun createFadeOutAnimator(duration: Long, interpolator: Interpolator?, view: View): ValueAnimator =
    createDefaultAnimator(duration, interpolator, PropertyValuesHolder.ofFloat(KEY_GENERIC_VALUE, 1f, 0f)) { valueAnimator ->
        view.alpha = valueAnimator.getAnimatedValue(KEY_GENERIC_VALUE) as Float
    }

fun createDefaultAnimator(duration: Long, interpolator: Interpolator?, vararg values: PropertyValuesHolder, valueAnimator: (ValueAnimator) -> Unit): ValueAnimator =
    ValueAnimator().apply {
        setValues(*values)
        this.duration = duration
        interpolator?.let {
            this.interpolator = interpolator
        }
        addUpdateListener(valueAnimator)
    }

private const val KEY_GENERIC_VALUE = "KEY_GENERIC_VALUE"

fun createRotationAnimator(duration: Long, interpolator: Interpolator?, from: Float, to: Float, view: View): ValueAnimator =
    createDefaultAnimator(duration, interpolator, PropertyValuesHolder.ofFloat(KEY_GENERIC_VALUE, from, to)) { valueAnimator ->
        view.rotation = valueAnimator.getAnimatedValue(KEY_GENERIC_VALUE) as Float
    }