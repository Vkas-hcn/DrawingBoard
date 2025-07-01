package com.completely.silent.drawingboard.main

import android.graphics.drawable.Drawable
import androidx.annotation.Keep

@Keep
data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)