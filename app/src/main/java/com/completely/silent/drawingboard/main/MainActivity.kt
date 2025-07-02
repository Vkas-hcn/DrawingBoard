package com.completely.silent.drawingboard.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.completely.silent.drawingboard.DataTool
import com.completely.silent.drawingboard.R
import com.completely.silent.drawingboard.SharedPrefsUtil
import com.completely.silent.drawingboard.ViewPager2Provider
import com.completely.silent.drawingboard.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), ViewPager2Provider {

    private lateinit var binding: ActivityMainBinding
    private var homeButtonCallbackId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        this.supportActionBar?.hide()
        onBackPressedDispatcher.addCallback {
            onBackPressedFun()
        }

        setupViewPager()
        clickListener()
        setDefaultLauncher()
    }

    private fun clickListener() {
        binding.inDialog1.quanDialog1.setOnClickListener { }
        binding.inDialog2.quanDialog2.setOnClickListener { }
        binding.inDialog1.mbContinue.setOnClickListener {
            binding.inDialog1.quanDialog1.isVisible = false
            DataTool.openHomeScreenSettings(this)
        }
        binding.inDialog2.tvSetAs.setOnClickListener {
            binding.inDialog2.quanDialog2.isVisible = false
            DataTool.openHomeScreenSettings(this)
        }
        binding.inDialog2.tvDiss.setOnClickListener {
            binding.inDialog2.quanDialog2.isVisible = false
        }
        homeButtonCallbackId = ReliableHomeButtonDetection.registerHomeButtonCallback {
            scrollToPage(1)
        }
    }

    private fun setDefaultLauncher() {
        if (DataTool.isDefaultLauncher(this)) {
            enableSwipe(true)
            binding.inDialog1.quanDialog1.isVisible = false
            binding.inDialog2.quanDialog2.isVisible = false
        } else {
            val isFirst = SharedPrefsUtil.getBoolean(DataTool.isFirstKey, false)
            val isSecond = SharedPrefsUtil.getBoolean(DataTool.isFirstMainKey, false)

            enableSwipe(false)
            if (isFirst) {
                if (!isSecond) {
                    binding.inDialog2.quanDialog2.isVisible = true
                    SharedPrefsUtil.saveBoolean(DataTool.isFirstMainKey, true)
                } else {
                    binding.inDialog2.quanDialog2.isVisible = false
                }
                binding.inDialog1.quanDialog1.isVisible = false
            } else {
                binding.inDialog1.quanDialog1.isVisible = true
                binding.inDialog2.quanDialog2.isVisible = false
                SharedPrefsUtil.saveBoolean(DataTool.isFirstKey, true)
            }
        }
    }

    private fun enableSwipe(enable: Boolean) {
        binding.viewPager.isUserInputEnabled = false
    }


    private fun setupViewPager() {
        val adapter = MainPagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.currentItem = 0

        binding.viewPager.apply {
            isUserInputEnabled = true

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)

                    when (position) {
                        0 -> {
                        }

                        1 -> {
                        }
                    }
                }
            })
        }
    }

    fun onBackPressedFun() {
        val currentPage = binding.viewPager.currentItem

        when {
            binding.inDialog1.quanDialog1.isVisible -> {
                binding.inDialog1.quanDialog1.isVisible = false
                setDefaultLauncher()
            }

            binding.inDialog2.quanDialog2.isVisible -> {
                binding.inDialog2.quanDialog2.isVisible = false
            }

            currentPage == 0 && DataTool.isDefaultLauncher(this) -> {
                scrollToPage(1)
            }

            currentPage == 1 -> {
            }

            else -> {
                finish()
            }
        }
    }

    private fun scrollToPage(position: Int) {
        if (DataTool.isDefaultLauncher(this)) {
            binding.viewPager.setCurrentItem(position, true)
        }
    }


    override fun onDestroy() {
        if (homeButtonCallbackId >= 0) {
            ReliableHomeButtonDetection.unregisterHomeButtonCallback(homeButtonCallbackId)
        }
        super.onDestroy()
    }

    override fun getViewPager2(): ViewPager2 {
        return binding.root.findViewById(R.id.viewPager)
    }

    class MainPagerAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> MainContentFragment()
                1 -> AppListFragment()
                else -> MainContentFragment()
            }
        }
    }
}