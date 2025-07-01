package com.completely.silent.drawingboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.completely.silent.drawingboard.databinding.ActivityGalleryBinding
import com.completely.silent.drawingboard.detail.DrawingDetailActivity
import com.completely.silent.drawingboard.detail.DrawingHistoryAdapter
import com.completely.silent.drawingboard.detail.DrawingHistoryManager

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var historyManager: DrawingHistoryManager
    private lateinit var adapter: DrawingHistoryAdapter
    private val historyList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.gallery)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        supportActionBar?.hide()
        historyManager = DrawingHistoryManager(this)
        setupRecyclerView()
        setupClickListeners()
        loadHistoryData()
    }

    override fun onResume() {
        super.onResume()
        // 每次返回时刷新数据
        loadHistoryData()
    }

    private fun setupRecyclerView() {
        adapter = DrawingHistoryAdapter(historyList) { filename ->
            // 点击项目时跳转到详情页面
            val intent = Intent(this, DrawingDetailActivity::class.java)
            intent.putExtra("filename", filename)
            startActivity(intent)
        }

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@GalleryActivity, 2) // 2列网格布局
            adapter = this@GalleryActivity.adapter
        }
    }

    private fun setupClickListeners() {
        // 返回按钮
        binding.btnBack.setOnClickListener {
            finish()
        }


    }

    private fun loadHistoryData() {
        historyList.clear()
        historyList.addAll(historyManager.getHistoryList())
        adapter.notifyDataSetChanged()

        // 更新UI状态
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (historyList.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
        }
    }

}