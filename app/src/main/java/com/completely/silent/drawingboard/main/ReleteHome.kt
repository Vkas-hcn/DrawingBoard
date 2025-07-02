package com.completely.silent.drawingboard.main

// ... existing code ...
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner

object ReleteHome {
    private var isAppInForeground = false
    private var homePressedCallbacks = mutableListOf<() -> Unit>()
    private val handler = Handler(Looper.getMainLooper())
    private const val EXIT_DETECTION_DELAY = 200L

    // 用于检测应用前后台状态变化
    private val lifecycleObserver = object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun onAppForeground() {
            isAppInForeground = true
            handler.removeCallbacksAndMessages(null)
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun onAppBackground() {
            isAppInForeground = false
            // 延迟检测确保准确进入后台
            handler.postDelayed({
                if (!isAppInForeground) {
                    notifyHomePressed()
                }
            }, EXIT_DETECTION_DELAY)
        }
    }

    // 更精确的Home键广播接收器
    private val homeKeyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS) {
                val reason = intent.getStringExtra("reason") ?: return

                // 只响应明确的Home键事件
                if (reason == "homekey") {
                    handler.removeCallbacksAndMessages(null)
                    notifyHomePressed()
                }
            }
        }
    }

    fun initialize(application: Application) {
        // 注册应用生命周期监听
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)

        // 注册Home键广播接收器
        try {
            val filter = IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            ContextCompat.registerReceiver(
                application,
                homeKeyReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (e: Exception) {
            // 记录日志但不停止执行
            e.printStackTrace()
        }
    }

    // 注册Home按键回调
    fun addHomePressedListener(callback: () -> Unit): Int {
        homePressedCallbacks.add(callback)
        return homePressedCallbacks.size - 1
    }

    // 移除指定回调
    fun removeHomePressedListener(callbackId: Int) {
        if (callbackId in homePressedCallbacks.indices) {
            homePressedCallbacks.removeAt(callbackId)
        }
    }

    // 批量移除所有回调
    fun clearHomePressedListeners() {
        homePressedCallbacks.clear()
    }

    // 触发所有回调
    private fun notifyHomePressed() {
        // 创建副本避免并发修改
        val callbacksCopy = ArrayList(homePressedCallbacks)
        for (callback in callbacksCopy) {
            try {
                callback()
            } catch (e: Exception) {
                // 捕获单个回调异常，不影响其他回调
                e.printStackTrace()
            }
        }
    }
}

