package com.completely.silent.drawingboard.main

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.completely.silent.drawingboard.databinding.FragmentAppListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListFragment : Fragment() {

    private var _binding: FragmentAppListBinding? = null
    private val binding get() = _binding!!
    private lateinit var appAdapter: AppListAdapter
    private val appList = mutableListOf<AppInfo>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadApps()
    }

    private fun setupRecyclerView() {
        appAdapter = AppListAdapter(
            appList,
            onAppClick = { appInfo -> openApp(appInfo) },
            onAppLongClick = { appInfo -> showAppOptionsDialog(appInfo) }
        )

        binding.recyclerView.apply {
            // 使用水平线性布局管理器实现左右滑动
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                requireContext(),
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = appAdapter
        }
    }

    private fun loadApps() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val packageManager = requireContext().packageManager
                val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                val currentPackageName = requireContext().packageName // 获取当前应用包名

                val apps = installedApps
                    .filter { app ->
                        // 过滤条件：
                        // 1. 不是当前应用本身
                        // 2. 只显示用户安装的应用或有启动图标的应用
                        app.packageName != currentPackageName &&
                                ((app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                                        packageManager.getLaunchIntentForPackage(app.packageName) != null)
                    }
                    .map { app ->
                        AppInfo(
                            name = app.loadLabel(packageManager).toString(),
                            packageName = app.packageName,
                            icon = app.loadIcon(packageManager)
                        )
                    }
                    .sortedBy { it.name }

                withContext(Dispatchers.Main) {
                    appList.clear()
                    appList.addAll(apps)
                    appAdapter.notifyDataSetChanged()
                    binding.progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "加载应用列表失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openApp(appInfo: AppInfo) {
        try {
            val launchIntent = requireContext().packageManager.getLaunchIntentForPackage(appInfo.packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                Toast.makeText(requireContext(), "无法打开该应用", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "打开应用失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAppOptionsDialog(appInfo: AppInfo) {
        val options = arrayOf("应用详情", "卸载应用")

        AlertDialog.Builder(requireContext())
            .setTitle(appInfo.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openAppDetails(appInfo)
                    1 -> uninstallApp(appInfo)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun openAppDetails(appInfo: AppInfo) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", appInfo.packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "无法打开应用详情", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uninstallApp(appInfo: AppInfo) {
        try {
            // 检查是否为系统应用
            val packageManager = requireContext().packageManager
            val appInfo = packageManager.getApplicationInfo(appInfo.packageName, 0)

            if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
                Toast.makeText(requireContext(), "系统应用无法卸载", Toast.LENGTH_SHORT).show()
                return
            }

            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", appInfo.packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "无法卸载应用", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // 当页面重新可见时，重新加载应用列表（可能有应用被卸载了）
        loadApps()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}