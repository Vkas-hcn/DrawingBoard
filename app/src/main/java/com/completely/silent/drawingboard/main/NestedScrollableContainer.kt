package com.completely.silent.drawingboard.main


import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import kotlin.math.abs


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
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = abs(ev.x - startX)
                val deltaY = abs(ev.y - startY)

                if (deltaX > 20 || deltaY > 20) {
                    isHorizontalScroll = deltaX > deltaY

                    if (isHorizontalScroll) {
                        parent?.requestDisallowInterceptTouchEvent(true)
                        return false
                    } else {
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
}