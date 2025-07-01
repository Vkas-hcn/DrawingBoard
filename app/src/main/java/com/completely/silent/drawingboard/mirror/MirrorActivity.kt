package com.completely.silent.drawingboard.mirror


import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.completely.silent.drawingboard.BoardLandscapeActivity
import com.completely.silent.drawingboard.BoardVerticalActivity
import com.completely.silent.drawingboard.DataTool
import com.completely.silent.drawingboard.detail.DrawingDetailActivity
import com.completely.silent.drawingboard.R
import com.completely.silent.drawingboard.databinding.ActivityMirrorBinding
import kotlin.jvm.java


class MirrorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMirrorBinding
    private lateinit var adapter: MirrorAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMirrorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mirror)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        this.supportActionBar?.hide()
        setupRecyclerView()
        clickFun()

    }

    private fun clickFun() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.llDialog.setOnClickListener {
            binding.llDialog.isVisible = false
        }
        binding.tvLand.setOnClickListener {
            binding.llDialog.isVisible = false
            startActivity(Intent(this, BoardVerticalActivity::class.java))
        }
        binding.tvPortrait.setOnClickListener {
            binding.llDialog.isVisible = false
            startActivity(Intent(this, BoardLandscapeActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = MirrorAdapter(DataTool.imageList) { imgRes ->
            DataTool.mirrorImage = imgRes
            binding.llDialog.isVisible = true
        }

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@MirrorActivity, 2)
            adapter = this@MirrorActivity.adapter
        }
    }
}