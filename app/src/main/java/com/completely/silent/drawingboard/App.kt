package com.completely.silent.drawingboard

import android.app.Application
import com.completely.silent.drawingboard.main.ReleteHome

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        SharedPrefsUtil.init(this)
        ReleteHome.initialize(this)

    }
}