package com.example.animation

import android.view.View
import androidx.core.view.ViewCompat

fun animateAutoFollowViews(translationType: TranslationType, vararg views: View) {
    if (views.isEmpty())
        return

    val fromTranslationY: Float
    val toTranslationY: Float
    val fromAlpha: Float
    val toAlpha: Float

    when (translationType) {
        TranslationType.OUT_TOP -> {
            fromTranslationY = 0f
            toTranslationY = -200f
            fromAlpha = 1f
            toAlpha = 0f
        }
        TranslationType.OUT_BOTTOM -> {
            fromTranslationY = 0f
            toTranslationY = 200f
            fromAlpha = 1f
            toAlpha = 0f
        }
        TranslationType.IN_TOP -> {
            fromTranslationY = 200f
            toTranslationY = 0f
            fromAlpha = 0f
            toAlpha = 1f
        }
        TranslationType.IN_BOTTOM -> {
            fromTranslationY = -200f
            toTranslationY = 0f
            fromAlpha = 0f
            toAlpha = 1f
        }
    }.exhaustive

    views.forEach { view ->
        ViewCompat.animate(view).cancel()
        view.alpha = fromAlpha
        view.translationY = fromTranslationY

        ViewCompat.animate(view)
            .translationY(toTranslationY)
            .alpha(toAlpha)
    }
}

fun restoreAutoFollowViews(vararg views: View) {
    views.forEach { view ->
        view.alpha = 1f
        view.translationY = 0f
    }
}

enum class TranslationType {
    OUT_TOP, OUT_BOTTOM, IN_TOP, IN_BOTTOM
}
