package com.completely.silent.drawingboard.detail


import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.completely.silent.drawingboard.R
import com.completely.silent.drawingboard.databinding.ActivityDrawingDetailBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class DrawingDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDrawingDetailBinding
    private lateinit var historyManager: DrawingHistoryManager
    private var filename: String? = null
    private var currentBitmap: Bitmap? = null

    // 权限请求
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            downloadToGallery()
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
        binding = ActivityDrawingDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawing_detail)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 隐藏ActionBar
        supportActionBar?.hide()

        // 初始化
        historyManager = DrawingHistoryManager(this)
        filename = intent.getStringExtra("filename")

        setupClickListeners()
        loadDrawingDetail()
    }

    private fun setupClickListeners() {
        // 返回按钮
        binding.btnBack.setOnClickListener {
            finish()
        }

        // 删除按钮
        binding.btnDelete.setOnClickListener {
            showDeleteConfirmDialog()
        }

        // 下载按钮
        binding.btnDownload.setOnClickListener {
            checkPermissionAndDownload()
        }
    }

    private fun loadDrawingDetail() {
        filename?.let { name ->
            currentBitmap = historyManager.loadDrawing(name)
            currentBitmap?.let { bitmap ->
                binding.ivDrawingDetail.setImageBitmap(bitmap)

            } ?: run {
                Toast.makeText(this, "Failed to load painting", Toast.LENGTH_SHORT).show()
                finish()
            }
        } ?: run {
            Toast.makeText(this, "No painting files found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showDeleteConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete the painting")
            .setMessage("Are you sure you want to delete this painting? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteDrawing()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteDrawing() {
        filename?.let { name ->
            val success = historyManager.deleteDrawing(name)
            if (success) {
                Toast.makeText(this, "The painting has been deleted", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Deletion failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissionAndDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            downloadToGallery()
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    downloadToGallery()
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

    private fun downloadToGallery() {
        currentBitmap?.let { bitmap ->
            try {
                val filename = "downloaded_drawing_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.png"

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
                Toast.makeText(this, "Saving failed：${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "No paintings to save", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Requires storage permissions")
            .setMessage("To save the painting to the album, storage permissions are required")
            .setPositiveButton("Authorization") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission denied")
            .setMessage("The painting cannot be saved to the album. You can manually enable storage permissions in settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                settingsLauncher.launch(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}