package com.completely.silent.drawingboard.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.completely.silent.drawingboard.databinding.ItemAppBinding

class AppListAdapter(
    private val appList: List<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    class AppViewHolder(private val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            appInfo: AppInfo,
            onAppClick: (AppInfo) -> Unit,
            onAppLongClick: (AppInfo) -> Unit
        ) {
            binding.ivAppIcon.setImageDrawable(appInfo.icon)
            binding.tvAppName.text = appInfo.name

            binding.root.setOnClickListener {
                onAppClick(appInfo)
            }

            binding.root.setOnLongClickListener {
                onAppLongClick(appInfo)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(appList[position], onAppClick, onAppLongClick)
    }

    override fun getItemCount(): Int = appList.size
}