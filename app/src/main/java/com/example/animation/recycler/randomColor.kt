package com.example.animation.recycler

import android.graphics.Color
import kotlin.random.Random

fun randomColor(): Int = Color.argb(
    255,
    Random.nextInt(256),
    Random.nextInt(256),
    Random.nextInt(256)
)