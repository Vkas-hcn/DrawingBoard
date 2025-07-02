package com.completely.silent.drawingboard.main


import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * 解决嵌套ViewPager2滑动冲突的包装容器
 * 将ViewPager2包装在这个容器中来处理触摸冲突
 */
class NestedScrollableContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var startX = 0f
    private var startY = 0f
    private var isHorizontalScroll = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.x
                startY = ev.y
                isHorizontalScroll = false
                // 允许子View先处理触摸事件
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = abs(ev.x - startX)
                val deltaY = abs(ev.y - startY)

                // 只在移动距离足够大时判断滑动方向
                if (deltaX > 20 || deltaY > 20) {
                    isHorizontalScroll = deltaX > deltaY

                    if (isHorizontalScroll) {
                        // 水平滑动，阻止父View拦截
                        parent?.requestDisallowInterceptTouchEvent(true)
                        return false // 让子ViewPager2处理
                    } else {
                        // 垂直滑动，让父View处理
                        parent?.requestDisallowInterceptTouchEvent(false)
                        return false
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                isHorizontalScroll = false
            }
        }

        return super.onInterceptTouchEvent(ev)
    }
}// 这个自定义ViewPager2类已经不需要了，因为ViewPager2是final类无法继承
// 我们改用触摸事件处理的方式来解决嵌套滑动冲突
// 请删除这个文件，使用AppListFragment中的触摸冲突处理方法