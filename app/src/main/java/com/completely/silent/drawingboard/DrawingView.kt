package com.completely.silent.drawingboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var drawPath = Path()
    private var drawPaint = Paint()
    private var canvasPaint = Paint(Paint.DITHER_FLAG)
    private var drawCanvas: Canvas? = null
    private var canvasBitmap: Bitmap? = null

    private val pathList = mutableListOf<PathData>()
    private val undoList = mutableListOf<PathData>()

    private var currentTool = BoardVerticalActivity.Tool.BRUSH

    // 添加标记来避免重复初始化
    private var isInitialized = false

    // 添加标志来跟踪是否有绘制内容
    private var hasDrawnContent = false

    data class PathData(
        val path: Path,
        val paint: Paint,
        val tool: BoardVerticalActivity.Tool
    )

    init {
        setupDrawing()
    }

    private fun setupDrawing() {
        drawPaint.apply {
            color = Color.BLACK
            isAntiAlias = true
            strokeWidth = 10f
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // 如果已经初始化且尺寸没有实际变化，则不重新创建bitmap
        if (isInitialized && oldw == w && oldh == h) {
            return
        }

        // 保存当前的绘画内容
        val oldBitmap = canvasBitmap

        // 创建新的bitmap和canvas
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawCanvas = Canvas(canvasBitmap!!)
        drawCanvas?.drawColor(Color.WHITE)

        // 如果之前有内容，恢复绘画内容
        if (oldBitmap != null && isInitialized) {
            drawCanvas?.drawBitmap(oldBitmap, 0f, 0f, null)
            oldBitmap.recycle() // 回收旧的bitmap
        } else {
            // 首次初始化时重绘所有路径
            redrawCanvas()
        }

        isInitialized = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvasBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, 0f, 0f, canvasPaint)
        }

        // 根据当前工具决定绘制时的颜色
        val displayPaint = if (currentTool == BoardVerticalActivity.Tool.ERASER) {
            Paint(drawPaint).apply {
                color = Color.WHITE
                xfermode = null
            }
        } else {
            drawPaint
        }

        canvas.drawPath(drawPath, displayPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                drawPath.moveTo(touchX, touchY)
            }
            MotionEvent.ACTION_MOVE -> {
                drawPath.lineTo(touchX, touchY)
            }
            MotionEvent.ACTION_UP -> {
                val newPaint = Paint(drawPaint)
                if (currentTool == BoardVerticalActivity.Tool.ERASER) {
                    newPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                }

                drawCanvas?.drawPath(drawPath, newPaint)
                pathList.add(PathData(Path(drawPath), newPaint, currentTool))
                undoList.clear()
                drawPath.reset()

                // 标记为已绘制内容
                hasDrawnContent = true
            }
            else -> return false
        }

        invalidate()
        return true
    }

    fun setPaintColor(color: Int) {
        drawPaint.color = color
        currentTool = BoardVerticalActivity.Tool.BRUSH
        drawPaint.xfermode = null
    }

    fun setBrushSize(size: Float) {
        drawPaint.strokeWidth = size
        currentTool = BoardVerticalActivity.Tool.BRUSH
        drawPaint.xfermode = null
    }

    fun setEraserSize(size: Float) {
        drawPaint.strokeWidth = size
        currentTool = BoardVerticalActivity.Tool.ERASER
    }

    fun undo() {
        if (pathList.isNotEmpty()) {
            undoList.add(pathList.removeAt(pathList.size - 1))
            redrawCanvas()
            // 更新绘制状态
            updateDrawnContentStatus()
        }
    }

    fun redo() {
        if (undoList.isNotEmpty()) {
            pathList.add(undoList.removeAt(undoList.size - 1))
            redrawCanvas()
            // 更新绘制状态
            updateDrawnContentStatus()
        }
    }

    private fun redrawCanvas() {
        canvasBitmap?.eraseColor(Color.WHITE)
        for (pathData in pathList) {
            drawCanvas?.drawPath(pathData.path, pathData.paint)
        }
        invalidate()
    }

    private fun updateDrawnContentStatus() {
        hasDrawnContent = pathList.isNotEmpty()
    }

    // 新增方法：检查是否有绘制内容
    fun hasDrawnContent(): Boolean {
        return hasDrawnContent
    }

    // 新增方法：清空绘制内容
    fun clearDrawing() {
        pathList.clear()
        undoList.clear()
        hasDrawnContent = false
        redrawCanvas()
    }

    fun getBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        draw(canvas)
        return bitmap
    }
}