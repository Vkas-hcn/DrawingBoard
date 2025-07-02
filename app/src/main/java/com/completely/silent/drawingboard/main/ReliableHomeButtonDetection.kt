package com.completely.silent.drawingboard.main

import android.annotation.SuppressLint
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
    // 标记应用是否在前台
    private var isAppInForeground = false

    // 保存上一次离开前台的原因
    private var lastExitReason = ExitReason.UNKNOWN


    // 后台检测的延迟时间（毫秒）
    private const val BACKGROUND_DETECTION_DELAY = 300L

    // Home键检测回调
    private var homeButtonCallbacks = mutableListOf<() -> Unit>()

    // 检测任务的Handler
    private val handler = Handler(Looper.getMainLooper())

    // 检测应用是否进入后台的Runnable
    private val backgroundDetectionRunnable = Runnable {
        if (!isAppInForeground) {
            // 如果应用确实进入了后台，并且不是因为Back键
            if (lastExitReason != ExitReason.BACK_BUTTON) {
                // 这很可能是Home键或Recent Apps键导致的
                lastExitReason = ExitReason.HOME_BUTTON

                // 通知所有注册的回调
                notifyHomeButtonPressed()
            }
        }
    }

    /**
     * 初始化检测系统，应在Application的onCreate中调用
     * @param application 应用的Application实例
     */
    fun initialize(application: Application) {
        // 注册应用生命周期监听
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            fun onAppForeground() {
                isAppInForeground = true
                handler.removeCallbacks(backgroundDetectionRunnable)
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            fun onAppBackground() {
                isAppInForeground = false
                // 延迟检测是否真的进入后台
                handler.postDelayed(backgroundDetectionRunnable, BACKGROUND_DETECTION_DELAY)
            }
        })

        // 注册Activity生命周期监听
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {}

            override fun onActivityPaused(activity: Activity) {
                App.lastPausedActivity = activity
            }

            override fun onActivityStopped(activity: Activity) {
                // 如果是最后一个暂停的Activity停止了，并且不是因为启动了新Activity
                if (activity == App.lastPausedActivity && !isChangingConfigurations(activity)) {
                    // 检查是否按下了Back键
                    if (activity.isFinishing) {
                        lastExitReason = ExitReason.BACK_BUTTON
                    }
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}
        })

        // 监听Home键的Intent过滤器（针对某些设备可能会发送的广播）
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
            // 某些Android版本可能不支持这种方式
            e.printStackTrace()
        }
    }

    /**
     * 检查Activity是否因配置更改而重启
     */
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
