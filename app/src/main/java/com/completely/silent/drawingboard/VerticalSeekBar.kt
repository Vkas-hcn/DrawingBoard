package com.completely.silent.drawingboard


import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class VerticalSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress = 0
    private var maxProgress = 100
    private var minProgress = 1

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#61467A")
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#61467A")
        style = Paint.Style.FILL
    }

    private val thumbStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private var thumbRadius = 24f
    private var trackLength = 0f
    private var thumbY = 0f

    // 监听器
    interface OnSeekBarChangeListener {
        fun onProgressChanged(seekBar: VerticalSeekBar, progress: Int, fromUser: Boolean)
        fun onStartTrackingTouch(seekBar: VerticalSeekBar)
        fun onStopTrackingTouch(seekBar: VerticalSeekBar)
    }

    private var onSeekBarChangeListener: OnSeekBarChangeListener? = null

    fun setOnSeekBarChangeListener(listener: OnSeekBarChangeListener?) {
        onSeekBarChangeListener = listener
    }

    fun setProgress(progress: Int) {
        this.progress = max(minProgress, min(maxProgress, progress))
        updateThumbPosition()
        onSeekBarChangeListener?.onProgressChanged(this, this.progress, false)
        invalidate()
    }

    fun getProgress(): Int = progress

    fun setMax(max: Int) {
        this.maxProgress = max
        if (progress > max) {
            setProgress(max)
        } else {
            updateThumbPosition()
            invalidate()
        }
    }

    fun setMin(min: Int) {
        this.minProgress = min
        if (progress < min) {
            setProgress(min)
        } else {
            updateThumbPosition()
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        trackLength = h - paddingTop - paddingBottom - thumbRadius * 2
        updateThumbPosition()
    }

    private fun updateThumbPosition() {
        if (trackLength > 0) {
            val progressRatio = (progress - minProgress).toFloat() / (maxProgress - minProgress)
            thumbY = paddingTop + thumbRadius + trackLength * (1f - progressRatio)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val trackTop = paddingTop + thumbRadius
        val trackBottom = height - paddingBottom - thumbRadius

        // 绘制轨道背景
        canvas.drawLine(centerX, trackTop, centerX, trackBottom, trackPaint)

        // 绘制已选择部分
        canvas.drawLine(centerX, thumbY, centerX, trackBottom, progressPaint)

        // 绘制滑块
        canvas.drawCircle(centerX, thumbY, thumbRadius, thumbPaint)
        canvas.drawCircle(centerX, thumbY, thumbRadius, thumbStrokePaint)

        // 绘制进度文字（可选）
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            textAlign = Paint.Align.CENTER
            textSize = 28f
        }
        canvas.drawText(
            progress.toString(),
            centerX,
            thumbY - thumbRadius - 20f,
            textPaint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                onSeekBarChangeListener?.onStartTrackingTouch(this)
                updateProgressFromTouch(event.y)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                updateProgressFromTouch(event.y)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(false)
                onSeekBarChangeListener?.onStopTrackingTouch(this)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateProgressFromTouch(touchY: Float) {
        val trackTop = paddingTop + thumbRadius
        val trackBottom = height - paddingBottom - thumbRadius
        val constrainedY = max(trackTop, min(trackBottom, touchY))

        // 注意：Y轴是倒序的（顶部是最大值）
        val progressRatio = 1f - (constrainedY - trackTop) / trackLength
        val newProgress = (minProgress + progressRatio * (maxProgress - minProgress)).toInt()

        if (newProgress != progress) {
            progress = newProgress
            thumbY = constrainedY
            onSeekBarChangeListener?.onProgressChanged(this, progress, true)
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (thumbRadius * 2 + paddingLeft + paddingRight + 100).toInt()
        val desiredHeight = 300

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(desiredWidth, widthSize)
            else -> desiredWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(desiredHeight, heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }
}