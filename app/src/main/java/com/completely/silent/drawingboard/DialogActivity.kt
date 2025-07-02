package com.completely.silent.drawingboard


import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

class DialogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.quan_main_3)

        // 设置 Activity 为对话框样式
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        window.setGravity(Gravity.BOTTOM) // 显示在底部
        window.setBackgroundDrawableResource(android.R.color.transparent) // 背景透明

        // 点击空白区域关闭 Activity
        findViewById<ConstraintLayout>(R.id.quan_dialog_3)
            .setOnClickListener {
                finish()
            }
    }
}
