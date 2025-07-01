package com.completely.silent.drawingboard.detail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * 画作历史记录管理器
 * 用于管理保存的画作历史记录
 */
class DrawingHistoryManager(private val context: Context) {

    companion object {
        private const val PREF_NAME = "drawing_history"
        private const val HISTORY_KEY = "history"
    }

    private val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /**
     * 保存画作到历史记录
     */
    fun saveDrawing(bitmap: Bitmap): String? {
        return try {
            val filename = "drawing_${System.currentTimeMillis()}.png"
            val file = File(context.filesDir, filename)

            file.outputStream().use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }

            // 添加到历史记录列表
            addToHistory(filename)
            filename
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取所有历史记录文件名
     */
    fun getHistoryList(): List<String> {
        val historySet = sharedPref.getStringSet(HISTORY_KEY, emptySet()) ?: emptySet()
        return historySet.toList().sortedDescending() // 按时间倒序排列
    }

    /**
     * 根据文件名加载画作
     */
    fun loadDrawing(filename: String): Bitmap? {
        return try {
            val file = File(context.filesDir, filename)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 删除历史记录
     */
    fun deleteDrawing(filename: String): Boolean {
        return try {
            val file = File(context.filesDir, filename)
            val deleted = file.delete()

            if (deleted) {
                removeFromHistory(filename)
            }
            deleted
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 清空所有历史记录
     */
    fun clearHistory() {
        val historyList = getHistoryList()
        historyList.forEach { filename ->
            File(context.filesDir, filename).delete()
        }
        sharedPref.edit().remove(HISTORY_KEY).apply()
    }

    /**
     * 获取历史记录数量
     */
    fun getHistoryCount(): Int {
        return getHistoryList().size
    }

    private fun addToHistory(filename: String) {
        val historySet = sharedPref.getStringSet(HISTORY_KEY, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        historySet.add(filename)
        sharedPref.edit().putStringSet(HISTORY_KEY, historySet).apply()
    }

    private fun removeFromHistory(filename: String) {
        val historySet = sharedPref.getStringSet(HISTORY_KEY, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        historySet.remove(filename)
        sharedPref.edit().putStringSet(HISTORY_KEY, historySet).apply()
    }
}