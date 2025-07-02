package com.completely.silent.drawingboard

import android.app.Activity
import android.app.Application
import com.completely.silent.drawingboard.main.ReliableHomeButtonDetection

class App: Application() {
    companion object {
        var lastPausedActivity: Activity? = null
    }
    override fun onCreate() {
        super.onCreate()
        SharedPrefsUtil.init(this)
        ReliableHomeButtonDetection.initialize(this)

    }
}