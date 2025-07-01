package com.completely.silent.drawingboard.detail


import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.core.graphics.scale
import com.completely.silent.drawingboard.R

class DrawingHistoryAdapter(
    private val historyList: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<DrawingHistoryAdapter.ViewHolder>() {

    private lateinit var historyManager: DrawingHistoryManager

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.iv_drawing_thumbnail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // 初始化历史记录管理器
        historyManager = DrawingHistoryManager(parent.context)

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_drawing_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val filename = historyList[position]

        // 设置点击事件
        holder.itemView.setOnClickListener {
            onItemClick(filename)
        }

        // 加载缩略图
        val bitmap = historyManager.loadDrawing(filename)
        if (bitmap != null) {
            // 创建缩略图
            val thumbnail = createThumbnail(bitmap, 200, 200)
            holder.imageView.setImageBitmap(thumbnail)
        } else {
            // 如果加载失败，显示默认图片
            holder.imageView.setImageResource(R.mipmap.ic_launcher_round)
        }
    }

    override fun getItemCount(): Int = historyList.size

    private fun createThumbnail(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val scale = minOf(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )

        val scaledWidth = (width * scale).toInt()
        val scaledHeight = (height * scale).toInt()

        return bitmap.scale(scaledWidth, scaledHeight)
    }


}