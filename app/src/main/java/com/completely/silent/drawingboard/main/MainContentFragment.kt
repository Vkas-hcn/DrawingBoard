package com.completely.silent.drawingboard.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.completely.silent.drawingboard.BoardLandscapeActivity
import com.completely.silent.drawingboard.BoardVerticalActivity
import com.completely.silent.drawingboard.DataTool
import com.completely.silent.drawingboard.GalleryActivity
import com.completely.silent.drawingboard.PoplActivity
import com.completely.silent.drawingboard.SharedPrefsUtil
import com.completely.silent.drawingboard.ViewPager2Provider
import com.completely.silent.drawingboard.databinding.FragmentMainContentBinding
import com.completely.silent.drawingboard.mirror.MirrorActivity

class MainContentFragment : Fragment() {

    private var _binding: FragmentMainContentBinding? = null
    private val binding get() = _binding!!
    private var viewPager: ViewPager2? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        viewPager?.let {
            setViewPager2(it)
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
    }
    private fun setupClickListeners() {
        binding.llDialog.setOnClickListener {
            binding.llDialog.isVisible = false
        }

        binding.tvStart.setOnClickListener {
            binding.llDialog.isVisible = true
        }

        binding.tvLand.setOnClickListener {
            binding.llDialog.isVisible = false
            DataTool.mirrorImage = 0
            startActivity(Intent(requireContext(), BoardVerticalActivity::class.java))
        }

        binding.tvPortrait.setOnClickListener {
            binding.llDialog.isVisible = false
            DataTool.mirrorImage = 0
            startActivity(Intent(requireContext(), BoardLandscapeActivity::class.java))
        }

        binding.tvGallery.setOnClickListener {
            startActivity(Intent(requireContext(), GalleryActivity::class.java))
        }

        binding.tvDraw.setOnClickListener {
            startActivity(Intent(requireContext(), MirrorActivity::class.java))
        }
        binding.conJump.setOnClickListener {
            viewPager?.setCurrentItem(1, true)
        }
        binding.imgSet.setOnClickListener {
            startActivity(Intent(requireContext(), PoplActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        binding.conJump.isVisible = DataTool.isDefaultLauncher(requireContext())
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}