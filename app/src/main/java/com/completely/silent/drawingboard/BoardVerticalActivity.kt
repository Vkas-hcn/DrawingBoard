package com.completely.silent.drawingboard

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
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
import com.completely.silent.drawingboard.detail.DrawingHistoryManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class BoardVerticalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBoardBinding
    private lateinit var drawingView: DrawingView
    private var currentTool = Tool.BRUSH
    private var currentColor = Color.BLACK
    private var currentBrushSize = 10f
    private var currentEraserSize = 20f

    enum class Tool {
        BRUSH, ERASER
    }

    // 权限请求
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            saveDrawingToGallery()
        } else {
            showPermissionDeniedDialog()
        }
    }

    // 设置页面跳转
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBoardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.board)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        this.supportActionBar?.hide()

        setupDrawingView()
        setupClickListeners()

        // 设置初始状态
        selectedTool = SelectedTool.BRUSH
        updateToolIcons() // 初始化图标状态

        onBackPressedDispatcher.addCallback {
            showBackConfirmDialog()
        }
    }

    private fun setupDrawingView() {
        drawingView = DrawingView(this)
        drawingView.setBrushSize(currentBrushSize)
        binding.flBoard.addView(drawingView)
        if (DataTool.mirrorImage != 0) {
            binding.carMirror.isVisible = true
            binding.imgMirror.setImageResource(DataTool.mirrorImage)
        } else {
            binding.carMirror.isVisible = false
        }
    }

    private fun setupClickListeners() {
        // 调色板
        binding.imgPalette.setOnClickListener {
            selectedTool = SelectedTool.PALETTE
            binding.llTool.isVisible =true
            updateToolIcons()
            showColorPalette()
        }

        // 画笔
        binding.imgHuabi.setOnClickListener {
            binding.llTool.isVisible = true
            currentTool = Tool.BRUSH
            selectedTool = SelectedTool.BRUSH
            updateToolIcons()
            // 延迟执行，避免布局变化影响DrawingView
            post {
                drawingView.setBrushSize(currentBrushSize)
                showBrushSizeSelector()
            }
        }

        // 橡皮擦
        binding.imgXiangpica.setOnClickListener {
            binding.llTool.isVisible = true
            currentTool = Tool.ERASER
            selectedTool = SelectedTool.ERASER
            updateToolIcons()
            // 延迟执行，避免布局变化影响DrawingView
            post {
                drawingView.setEraserSize(currentEraserSize)
                showEraserSizeSelector()
            }
        }

        // 下载
        binding.imgXiazai.setOnClickListener {
            selectedTool = SelectedTool.NONE
            updateToolIcons()
            binding.llTool.removeAllViews() // 清空工具栏
            checkPermissionAndSave()
        }

        // 完成
        binding.imgWancheng.setOnClickListener {
            selectedTool = SelectedTool.NONE
            updateToolIcons()
            binding.llTool.removeAllViews() // 清空工具栏
            saveDrawingPermanently()
        }

        // 撤销
        binding.imgHuitui.setOnClickListener {
            drawingView.undo()
        }

        // 重做
        binding.imgQianj.setOnClickListener {
            drawingView.redo()
        }

        // 返回
        binding.imgSet.setOnClickListener {
            showBackConfirmDialog()
        }
    }

    // 添加一个post方法来延迟执行
    private fun post(action: () -> Unit) {
        binding.root.post(action)
    }

    // 当前选中的工具类型
    enum class SelectedTool {
        PALETTE, BRUSH, ERASER, NONE
    }

    private var selectedTool = SelectedTool.BRUSH

    // 更新工具图标状态
    private fun updateToolIcons() {
        // 重置所有图标为未选中状态
        binding.imgPalette.setImageResource(R.drawable.ic_palette_2)
        binding.imgHuabi.setImageResource(R.drawable.ic_huabi_2)
        binding.imgXiangpica.setImageResource(R.drawable.ic_xiangpica_2)

        // 根据当前选中的工具设置对应图标为选中状态
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
                // 所有图标都是未选中状态，已在上面设置
            }
        }
    }

    private fun showColorPalette() {
        binding.llTool.removeAllViews()

        // 扩展颜色列表到18个颜色
        val colors = listOf(
            // 第一行9个颜色
            Color.BLACK, Color.RED, Color.BLUE, Color.GREEN,
            Color.YELLOW, Color.MAGENTA, Color.CYAN, Color.GRAY,
            Color.WHITE,
            // 第二行9个颜色
            Color.parseColor("#FF9800"), Color.parseColor("#9C27B0"),
            Color.parseColor("#4CAF50"), Color.parseColor("#F44336"),
            Color.parseColor("#2196F3"), Color.parseColor("#FF5722"),
            Color.parseColor("#607D8B"), Color.parseColor("#795548"),
            Color.parseColor("#9E9E9E")
        )

        // 创建主容器
        val mainContainer = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
        }

        // 创建第一行
        val firstRow = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
        }

        // 创建第二行
        val secondRow = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8.dpToPx()
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
        }

        colors.forEachIndexed { index, color ->
            val colorView = View(this).apply {
                val size = 24.dpToPx()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(6.dpToPx(), 6.dpToPx(), 6.dpToPx(), 6.dpToPx())
                }
                background = createCircleDrawable(color)
                setOnClickListener {
                    currentColor = color
                    drawingView.setPaintColor(color)
                    currentTool = Tool.BRUSH
                    selectedTool = SelectedTool.BRUSH
                    updateToolIcons()
                    binding.llTool.removeAllViews()
                }
            }

            if (index < 9) {
                firstRow.addView(colorView)
            } else {
                secondRow.addView(colorView)
            }
        }

        mainContainer.addView(firstRow)
        mainContainer.addView(secondRow)
        binding.llTool.addView(mainContainer)
    }

    private fun showBrushSizeSelector() {
        binding.llTool.removeAllViews()

        val seekBar = SeekBar(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(32.dpToPx(), 16.dpToPx(), 32.dpToPx(), 16.dpToPx())
            }
            max = 50
            progress = currentBrushSize.toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        currentBrushSize = (progress + 1).toFloat()
                        drawingView.setBrushSize(currentBrushSize)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    binding.llTool.removeAllViews()
                }
            })
        }
        binding.llTool.addView(seekBar)
    }

    private fun showEraserSizeSelector() {
        binding.llTool.removeAllViews()

        val seekBar = SeekBar(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(32.dpToPx(), 16.dpToPx(), 32.dpToPx(), 16.dpToPx())
            }
            max = 100
            progress = currentEraserSize.toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) { // 只响应用户操作
                        currentEraserSize = (progress + 5).toFloat()
                        drawingView.setEraserSize(currentEraserSize)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    binding.llTool.removeAllViews()
                }
            })
        }
        binding.llTool.addView(seekBar)
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
            val filename = "drawing_${
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.getDefault()
                ).format(Date())
            }.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                val uri = contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    Toast.makeText(
                        this,
                        "The painting has been saved to the album",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                val picturesDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val file = File(picturesDir, filename)
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }

                // 通知系统扫描新文件
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = Uri.fromFile(file)
                sendBroadcast(mediaScanIntent)

                Toast.makeText(this, "The painting has been saved to the album", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: IOException) {
            Toast.makeText(this, "Save failed：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveDrawingPermanently() {
        try {
            val bitmap = drawingView.getBitmap()
            val historyManager = DrawingHistoryManager(this)
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
            AlertDialog.Builder(this)
                .setTitle("Tip")
                .setMessage("Save your artwork before exiting?")
                .setPositiveButton("Back") { _, _ ->
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
    }


    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}