package com.completely.silent.drawingboard.mirror


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.completely.silent.drawingboard.R

class MirrorAdapter(
    private val historyList: List<Int>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<MirrorAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.iv_drawing_thumbnail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_drawing_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imgRes = historyList[position]

        // 设置点击事件
        holder.itemView.setOnClickListener {
            onItemClick(imgRes)
        }

        // 加载缩略图
        holder.imageView.setImageResource(imgRes)

    }

    override fun getItemCount(): Int = historyList.size
}