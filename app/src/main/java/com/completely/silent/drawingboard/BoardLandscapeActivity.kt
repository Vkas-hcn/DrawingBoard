package com.completely.silent.drawingboard

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.completely.silent.drawingboard.databinding.ActivityBoardBinding
import com.completely.silent.drawingboard.databinding.ActivityBoardLandBinding
import com.completely.silent.drawingboard.detail.DrawingHistoryManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class BoardLandscapeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBoardLandBinding
    private lateinit var drawingView: DrawingView
    private var currentTool = Tool.BRUSH
    private var currentColor = Color.BLACK
    private var currentBrushSize = 10f
    private var currentEraserSize = 20f

    private var verticalBrushSeekBar: VerticalSeekBar? = null
    private var verticalEraserSeekBar: VerticalSeekBar? = null

    private var hideToolsRunnable: Runnable? = null

    enum class Tool {
        BRUSH, ERASER
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            saveDrawingToGallery()
        } else {
            showPermissionDeniedDialog()
        }
    }

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBoardLandBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.board)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        this.supportActionBar?.hide()

        setupDrawingView()
        setupClickListeners()
        selectedTool = SelectedTool.BRUSH
        updateToolIcons()
        onBackPressedDispatcher.addCallback {
            showBackConfirmDialog()
        }
    }

    private fun setupDrawingView() {
        drawingView = DrawingView(this)
        drawingView.setBrushSize(currentBrushSize)
        binding.flBoard.addView(drawingView)
        if(DataTool.mirrorImage!=0){
            binding.carMirror.isVisible =true
            binding.imgMirror.setImageResource(DataTool.mirrorImage)
        }else{
            binding.carMirror.isVisible =false
        }
    }

    private fun setupClickListeners() {
        binding.imgPalette.setOnClickListener {
            cancelHideToolsTask()

            binding.llTool.isVisible = true
            selectedTool = SelectedTool.PALETTE
            updateToolIcons()
            showColorPalette()
        }

        binding.imgHuabi.setOnClickListener {
            cancelHideToolsTask()

            binding.llTool.isVisible = true
            currentTool = Tool.BRUSH
            selectedTool = SelectedTool.BRUSH
            updateToolIcons()
            drawingView.setBrushSize(currentBrushSize)
            post {
                showBrushSizeSelector()
            }
        }

        binding.imgXiangpica.setOnClickListener {
            cancelHideToolsTask()

            binding.llTool.isVisible = true
            currentTool = Tool.ERASER
            selectedTool = SelectedTool.ERASER
            updateToolIcons()
            drawingView.setEraserSize(currentEraserSize)
            post {
                showEraserSizeSelector()
            }
        }

        binding.imgXiazai.setOnClickListener {
            cancelHideToolsTask()
            selectedTool = SelectedTool.NONE
            updateToolIcons()
            hideAllTools()
            if (drawingView.hasDrawnContent()) {
                checkPermissionAndSave()
            } else {
                Toast.makeText(this, "Please draw something before saving", Toast.LENGTH_SHORT).show()
            }
        }

        binding.imgWancheng.setOnClickListener {
            cancelHideToolsTask()
            selectedTool = SelectedTool.NONE
            updateToolIcons()
            hideAllTools()
            if (drawingView.hasDrawnContent()) {
                saveDrawingPermanently()
            } else {
                Toast.makeText(this, "Please draw something before saving", Toast.LENGTH_SHORT).show()
            }
        }

        binding.imgHuitui.setOnClickListener {
            drawingView.undo()
        }

        binding.imgQianj.setOnClickListener {
            drawingView.redo()
        }

        binding.imgSet.setOnClickListener {
            showBackConfirmDialog()
        }
    }

    private fun post(action: () -> Unit) {
        binding.root.post(action)
    }

    enum class SelectedTool {
        PALETTE, BRUSH, ERASER, NONE
    }

    private var selectedTool = SelectedTool.BRUSH

    private fun updateToolIcons() {
        binding.imgPalette.setImageResource(R.drawable.ic_palette_2)
        binding.imgHuabi.setImageResource(R.drawable.ic_huabi_2)
        binding.imgXiangpica.setImageResource(R.drawable.ic_xiangpica_2)

        when (selectedTool) {
            SelectedTool.PALETTE -> {
                binding.imgPalette.setImageResource(R.drawable.ic_palette_1)
            }
            SelectedTool.BRUSH -> {
                binding.imgHuabi.setImageResource(R.drawable.ic_huabi_1)
            }
            SelectedTool.ERASER -> {
                binding.imgXiangpica.setImageResource(R.drawable.ic_xiangpica_1)
            }
            SelectedTool.NONE -> {
            }
        }
    }

    private fun cancelHideToolsTask() {
        hideToolsRunnable?.let {
            binding.root.removeCallbacks(it)
            hideToolsRunnable = null
        }
    }

    private fun hideAllTools() {
        binding.llTool.visibility = View.GONE
        binding.svColorPalette.visibility = View.GONE
        verticalBrushSeekBar = null
        verticalEraserSeekBar = null
    }

    private fun showBrushSizeSelector() {
        hideAllTools()
        binding.llTool.visibility = View.VISIBLE
        verticalBrushSeekBar = VerticalSeekBar(this).apply {
            setMin(1)
            setMax(50)
            setProgress(currentBrushSize.toInt())
            setOnSeekBarChangeListener(object : VerticalSeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: VerticalSeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        currentBrushSize = progress.toFloat()
                        drawingView.setBrushSize(currentBrushSize)
                    }
                }

                override fun onStartTrackingTouch(seekBar: VerticalSeekBar) {
                    // 开始触摸时取消之前的延迟隐藏任务
                    cancelHideToolsTask()
                }

                override fun onStopTrackingTouch(seekBar: VerticalSeekBar) {
                    cancelHideToolsTask()
                    hideToolsRunnable = Runnable {
                        hideAllTools()
                        hideToolsRunnable = null
                    }
                    binding.root.postDelayed(hideToolsRunnable!!, 1500)
                }
            })
        }

        binding.llTool.addView(verticalBrushSeekBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ))
    }

    private fun showEraserSizeSelector() {
        hideAllTools()
        binding.llTool.visibility = View.VISIBLE

        verticalEraserSeekBar = VerticalSeekBar(this).apply {
            setMin(5)
            setMax(100)
            setProgress(currentEraserSize.toInt())
            setOnSeekBarChangeListener(object : VerticalSeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: VerticalSeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        currentEraserSize = progress.toFloat()
                        drawingView.setEraserSize(currentEraserSize)
                    }
                }

                override fun onStartTrackingTouch(seekBar: VerticalSeekBar) {
                    cancelHideToolsTask()
                }

                override fun onStopTrackingTouch(seekBar: VerticalSeekBar) {
                    cancelHideToolsTask()
                    hideToolsRunnable = Runnable {
                        hideAllTools()
                        hideToolsRunnable = null
                    }
                    binding.root.postDelayed(hideToolsRunnable!!, 1500)
                }
            })
        }

        binding.llTool.addView(verticalEraserSeekBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ))
    }

    private fun showColorPalette() {
        hideAllTools()
        binding.llTool.visibility = View.VISIBLE
        binding.svColorPalette.visibility = View.VISIBLE

        binding.llColorContainer.removeAllViews()

        val colors = listOf(
            Color.BLACK, Color.RED, Color.BLUE, Color.GREEN,
            Color.YELLOW, Color.MAGENTA, Color.CYAN, Color.GRAY,
            Color.WHITE, Color.parseColor("#FF9800"),
            Color.parseColor("#9C27B0"), Color.parseColor("#4CAF50"),
            Color.parseColor("#F44336"), Color.parseColor("#2196F3"),
            Color.parseColor("#FF5722"), Color.parseColor("#607D8B"),
            Color.parseColor("#795548"), Color.parseColor("#9E9E9E"),
        )

        // 添加颜色块
        for (color in colors) {
            val colorView = View(this).apply {
                val size = 22.dpToPx()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(0, 3.dpToPx(), 0, 3.dpToPx())
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                background = createCircleDrawable(color)
                setOnClickListener {
                    // 选择颜色时取消延迟隐藏任务
                    cancelHideToolsTask()

                    currentColor = color
                    drawingView.setPaintColor(color)
                    currentTool = Tool.BRUSH
                    selectedTool = SelectedTool.BRUSH
                    updateToolIcons()
                    hideAllTools()
                }
            }
            binding.llColorContainer.addView(colorView)
        }
    }

    private fun createCircleDrawable(color: Int): Drawable {
        return object : Drawable() {
            override fun draw(canvas: Canvas) {
                val paint = Paint().apply {
                    this.color = color
                    isAntiAlias = true
                }
                val strokePaint = Paint().apply {
                    this.color = Color.GRAY
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                val bounds = getBounds()
                val radius = minOf(bounds.width(), bounds.height()) / 2f - 1f
                val centerX = bounds.centerX().toFloat()
                val centerY = bounds.centerY().toFloat()

                // 绘制颜色圆
                canvas.drawCircle(centerX, centerY, radius, paint)
                // 绘制边框
                canvas.drawCircle(centerX, centerY, radius, strokePaint)
            }

            override fun setAlpha(alpha: Int) {}
            override fun setColorFilter(colorFilter: ColorFilter?) {}
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }
    }

    private fun checkPermissionAndSave() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveDrawingToGallery()
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    saveDrawingToGallery()
                }
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) -> {
                    showPermissionRationaleDialog()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun saveDrawingToGallery() {
        try {
            val bitmap = drawingView.getBitmap()
            val filename = "drawing_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    Toast.makeText(this, "The painting has been saved to the album", Toast.LENGTH_SHORT).show()
                }
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val file = File(picturesDir, filename)
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }

                // 通知系统扫描新文件
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = Uri.fromFile(file)
                sendBroadcast(mediaScanIntent)

                Toast.makeText(this, "The painting has been saved to the album", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Save failed：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveDrawingPermanently() {
        try {
            val bitmap = drawingView.getBitmap()
            val  historyManager = DrawingHistoryManager(this)
            historyManager.saveDrawing(bitmap)
            finish()
        } catch (e: IOException) {
            Toast.makeText(this, "Save failed：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Storage permissions are required")
            .setMessage("In order to save your drawings to an album, you need to save permissions")
            .setPositiveButton("authorization") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission denied")
            .setMessage("Unable to save a drawing to an album. You can manually enable storage permissions in the settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                settingsLauncher.launch(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBackConfirmDialog() {
        // 只有在有绘制内容时才显示确认对话框
        if (drawingView.hasDrawnContent()) {
            AlertDialog.Builder(this)
                .setTitle("Tip")
                .setMessage("Save your artwork before exiting?")
                .setPositiveButton("Save and Exit") { _, _ ->
                    if (drawingView.hasDrawnContent()) {
                        saveDrawingPermanently()
                    } else {
                        finish()
                    }
                }
                .setNegativeButton("Exit without saving") { _, _ ->
                    finish()
                }
                .setNeutralButton("Cancel", null)
                .show()
        } else {
            // 没有绘制内容时直接退出
            finish()
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理延迟任务
        cancelHideToolsTask()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}