package com.completely.silent.drawingboard.main

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.completely.silent.drawingboard.App

object ReliableHomeButtonDetection {
    private var isAppInForeground = false
    private var lastExitReason = ExitReason.UNKNOWN


    private const val BACKGROUND_DETECTION_DELAY = 300L

    private var homeButtonCallbacks = mutableListOf<() -> Unit>()

    private val handler = Handler(Looper.getMainLooper())

    private val backgroundDetectionRunnable = Runnable {
        if (!isAppInForeground) {
            if (lastExitReason != ExitReason.BACK_BUTTON) {
                lastExitReason = ExitReason.HOME_BUTTON

                notifyHomeButtonPressed()
            }
        }
    }


    fun initialize(application: Application) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            fun onAppForeground() {
                isAppInForeground = true
                handler.removeCallbacks(backgroundDetectionRunnable)
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            fun onAppBackground() {
                isAppInForeground = false
                handler.postDelayed(backgroundDetectionRunnable, BACKGROUND_DETECTION_DELAY)
            }
        })

        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {}

            override fun onActivityPaused(activity: Activity) {
                App.lastPausedActivity = activity
            }

            override fun onActivityStopped(activity: Activity) {
                if (activity == App.lastPausedActivity && !isChangingConfigurations(activity)) {
                    if (activity.isFinishing) {
                        lastExitReason = ExitReason.BACK_BUTTON
                    }
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}
        })

        try {
            val homeFilter = IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            ContextCompat.registerReceiver(application, object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val reason = intent.getStringExtra("reason")
                    if (reason != null && (reason == "homekey" || reason == "recentapps")) {
                        lastExitReason = ExitReason.HOME_BUTTON
                        notifyHomeButtonPressed()
                    }
                }
            }, homeFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isChangingConfigurations(activity: Activity): Boolean {
        return try {
            activity.isChangingConfigurations
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 注册Home按键检测回调
     * @param callback 检测到Home按键时执行的回调
     * @return 返回回调ID，用于后续取消注册
     */
    fun registerHomeButtonCallback(callback: () -> Unit): Int {
        homeButtonCallbacks.add(callback)
        return homeButtonCallbacks.size - 1
    }

    /**
     * 取消注册Home按键检测回调
     * @param callbackId 注册时返回的回调ID
     */
    fun unregisterHomeButtonCallback(callbackId: Int) {
        if (callbackId >= 0 && callbackId < homeButtonCallbacks.size) {
            homeButtonCallbacks.removeAt(callbackId)
        }
    }

    /**
     * 通知所有注册的回调Home按键被按下
     */
    private fun notifyHomeButtonPressed() {
        for (callback in homeButtonCallbacks) {
            callback()
        }
    }

    /**
     * 应用退出前台的原因枚举
     */
    enum class ExitReason {
        UNKNOWN,
        BACK_BUTTON,
        HOME_BUTTON
    }
}
