package com.completely.silent.drawingboard.main

import android.Manifest
import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.completely.silent.drawingboard.R
import com.completely.silent.drawingboard.ViewPager2Provider
import com.completely.silent.drawingboard.databinding.FragmentAppListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppListFragment : Fragment() {

    private var _binding: FragmentAppListBinding? = null
    private val binding get() = _binding!!
    private lateinit var appsPageAdapter: AppsPageAdapter
    private val appList = mutableListOf<AppInfo>()
    private val pages = mutableListOf<List<AppInfo>>()
    private val fixedApps = mutableListOf<AppInfo>()
    private var viewPager: ViewPager2? = null
    private var isViewPager2Set = false
    private lateinit var sharedPreferences: SharedPreferences


    companion object {
        private const val APPS_PER_PAGE = 20 // 4行 x 5列
        private const val PREF_NAME = "fixed_apps"
        private const val FIXED_APPS_KEY = "fixed_apps_packages"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppListBinding.inflate(inflater, container, false)
        sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
        setupIconClick()
        loadApps()
        if (isViewPager2Set) {
            viewPager?.let {
                setViewPager2(it)
            }
        }
    }

    private fun setupViewPager() {
        appsPageAdapter = AppsPageAdapter(
            pages,
            onAppClick = { appInfo -> openApp(appInfo) },
            onAppLongClick = { appInfo -> showAppOptionsDialog(appInfo) }
        )

        binding.viewPagerApps.adapter = appsPageAdapter
        binding.viewPagerApps.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDotsIndicator(position)
            }
        })

        // 使用包装容器处理滑动冲突，简化处理逻辑
        setupTouchConflictResolution()
    }
    private fun setupTouchConflictResolution() {
        binding.viewPagerApps.apply {
            // 设置预加载页面数量
            offscreenPageLimit = 1

            // 由于使用了NestedScrollableContainer，这里只需要简单的边界处理
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    updateDotsIndicator(position)
                }
            })
        }
    }
    private fun setupIconClick() {
        binding.iconIcon.setOnClickListener {
            if (viewPager == null) {
                Log.e("LaunchFragment", "ViewPager2 is null!")
            } else {
                Log.d("LaunchFragment", "Switching to HomeFragment")
                viewPager?.setCurrentItem(0, true)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ViewPager2Provider) {
            viewPager = context.getViewPager2()
        }
    }

    fun setViewPager2(viewPager2: ViewPager2) {
        this.viewPager = viewPager2
        isViewPager2Set = true
        Log.d("LaunchFragment", "ViewPager2 set successfully")
    }

    private fun loadApps() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val packageManager = requireContext().packageManager
                val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                val currentPackageName = requireContext().packageName

                val apps = installedApps
                    .filter { app ->
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

                    // 分页处理
                    createPages()
                    appsPageAdapter.notifyDataSetChanged()

                    // 设置页面指示器
                    setupDotsIndicator()

                    // 加载固定应用
                    loadFixedApps()

                    binding.progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed to load the app list", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createPages() {
        pages.clear()
        val totalApps = appList.size
        var startIndex = 0

        while (startIndex < totalApps) {
            val endIndex = minOf(startIndex + APPS_PER_PAGE, totalApps)
            val pageApps = appList.subList(startIndex, endIndex).toList()
            pages.add(pageApps)
            startIndex = endIndex
        }
    }

    private fun setupDotsIndicator() {
        binding.dotsIndicator.removeAllViews()

        val dotSize = 12
        val dotMargin = 8

        for (i in pages.indices) {
            val dot = View(requireContext())
            val params = LinearLayout.LayoutParams(dotSize, dotSize)
            params.setMargins(dotMargin, 0, dotMargin, 0)
            dot.layoutParams = params

            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.OVAL
            drawable.setColor(if (i == 0) Color.WHITE else Color.GRAY)
            dot.background = drawable

            binding.dotsIndicator.addView(dot)
        }
    }

    private fun updateDotsIndicator(position: Int) {
        for (i in 0 until binding.dotsIndicator.childCount) {
            val dot = binding.dotsIndicator.getChildAt(i)
            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.OVAL
            drawable.setColor(if (i == position) Color.WHITE else Color.GRAY)
            dot.background = drawable
        }
    }

    private fun loadFixedApps() {
        val savedPackages = sharedPreferences.getStringSet(FIXED_APPS_KEY, emptySet()) ?: emptySet()
        fixedApps.clear()
        val currentPackageName = requireContext().packageName
        for (packageName in savedPackages) {
            val app = appList.find { it.packageName == packageName &&it.packageName != currentPackageName }
            if (app != null) {
                fixedApps.add(app)
            }
        }

        // 如果固定应用少于5个，使用最常用的应用补充
        if (fixedApps.size < 5) {
            val remainingSlots = 5 - fixedApps.size
            val popularApps = appList.filter { !fixedApps.contains(it) }.take(remainingSlots)
            fixedApps.addAll(popularApps)
        }

        updateFixedAppsUI()
    }

    private fun updateFixedAppsUI() {
        binding.fixedAppsContainer.removeAllViews()

        for (i in 0 until 5) {
            val appContainer = createFixedAppView(if (i < fixedApps.size) fixedApps[i] else null, i)
            binding.fixedAppsContainer.addView(appContainer)
        }
    }

    private fun createFixedAppView(appInfo: AppInfo?, position: Int): View {
        val container = LayoutInflater.from(requireContext()).inflate(R.layout.item_app, binding.fixedAppsContainer, false)
        val iconView = container.findViewById<ImageView>(R.id.iv_app_icon)
        val nameView = container.findViewById<TextView>(R.id.tv_app_name)

        val layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        layoutParams.setMargins(4, 0, 4, 0)
        container.layoutParams = layoutParams

        if (appInfo != null) {
            iconView.setImageDrawable(appInfo.icon)
            nameView.text = appInfo.name
            nameView.maxLines = 1

            container.setOnClickListener { openApp(appInfo) }
            container.setOnLongClickListener {
                showFixedAppOptionsDialog(appInfo, position)
                true
            }
        } else {
            iconView.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_add))
            nameView.text = "Vacant space"

            container.setOnClickListener {
                showAppSelectionDialog(position)
            }
        }

        return container
    }

    private fun showFixedAppOptionsDialog(appInfo: AppInfo, position: Int) {
        val options = arrayOf("Open the app", "Remove the pin", "Application details")

        AlertDialog.Builder(requireContext())
            .setTitle(appInfo.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openApp(appInfo)
                    1 -> removeFixedApp(position)
                    2 -> openAppDetails(appInfo)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAppSelectionDialog(position: Int) {
        val availableApps = appList.filter { !fixedApps.contains(it) }
        val appNames = availableApps.map { it.name }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Select the app to be fixed")
            .setItems(appNames) { _, which ->
                addFixedApp(availableApps[which], position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addFixedApp(appInfo: AppInfo, position: Int) {
        if (position < fixedApps.size) {
            fixedApps[position] = appInfo
        } else {
            fixedApps.add(appInfo)
        }
        saveFixedApps()
        updateFixedAppsUI()
    }

    private fun removeFixedApp(position: Int) {
        if (position < fixedApps.size) {
            fixedApps.removeAt(position)
            saveFixedApps()
            updateFixedAppsUI()
        }
    }

    private fun saveFixedApps() {
        val packageNames = fixedApps.map { it.packageName }.toSet()
        sharedPreferences.edit()
            .putStringSet(FIXED_APPS_KEY, packageNames)
            .apply()
    }

    private fun openApp(appInfo: AppInfo) {
        try {
            val launchIntent = requireContext().packageManager.getLaunchIntentForPackage(appInfo.packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                Toast.makeText(requireContext(), "Unable to open the app", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to open the app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAppOptionsDialog(appInfo: AppInfo) {
        val options = arrayOf("Application details", "Uninstall the app", "Add to commonly used")

        AlertDialog.Builder(requireContext())
            .setTitle(appInfo.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openAppDetails(appInfo)
                    1 -> uninstallApp(appInfo)
                    2 -> addToFixedApps(appInfo)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addToFixedApps(appInfo: AppInfo) {
        if (fixedApps.size < 5 && !fixedApps.contains(appInfo)) {
            fixedApps.add(appInfo)
            saveFixedApps()
            updateFixedAppsUI()
            Toast.makeText(requireContext(), "Added to common applications", Toast.LENGTH_SHORT).show()
        } else if (fixedApps.contains(appInfo)) {
            Toast.makeText(requireContext(), "The application is already in the commonly used list", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Common applications are full, please remove other applications first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAppDetails(appInfo: AppInfo) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", appInfo.packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to open application details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uninstallApp(appInfo: AppInfo) {
        try {
            val packageManager = requireContext().packageManager
            val applicationInfo = packageManager.getApplicationInfo(appInfo.packageName, 0)

            if ((applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
                Toast.makeText(requireContext(), "System application cannot be uninstalled", Toast.LENGTH_SHORT).show()
                return
            }

            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", appInfo.packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to uninstall the app", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadApps()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}