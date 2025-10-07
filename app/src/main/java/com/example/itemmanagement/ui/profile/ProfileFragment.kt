package com.example.itemmanagement.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.adapter.ProfileAdapter
import com.example.itemmanagement.databinding.FragmentProfileBinding
import com.example.itemmanagement.ui.utils.Material3Feedback
import com.example.itemmanagement.data.entity.UserProfileEntity

/**
 * "我的"主页面Fragment
 * 展示用户信息、统计数据和功能入口
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProfileViewModel
    private lateinit var profileAdapter: ProfileAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        // 初始化ViewModel
        val application = requireActivity().application as ItemManagementApplication
        val factory = ProfileViewModelFactory(application.userProfileRepository, application.repository)
        viewModel = ViewModelProvider(this, factory)[ProfileViewModel::class.java]

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupEdgeToEdge()
        setupRecyclerView()
        setupObservers()
        
        // 加载数据
        viewModel.loadUserData()
    }

    /**
     * 设置Edge-to-Edge显示
     */
    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // 只设置底部内边距，让个人信息直接从状态栏下方开始
            binding.recyclerView.setPadding(
                0,
                0,  // 移除顶部内边距
                0,
                systemBars.bottom
            )
            
            insets
        }
    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        profileAdapter = ProfileAdapter(
            onMenuItemClick = { menuId -> handleMenuItemClick(menuId) },
            onUserInfoClick = { navigateToEditProfile() }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = profileAdapter
        }
    }

    /**
     * 设置数据观察者
     */
    private fun setupObservers() {
        // 观察Profile列表项
        viewModel.profileItems.observe(viewLifecycleOwner) { items ->
            profileAdapter.submitList(items)
        }
        
        // 观察用户资料
        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let {
                profileAdapter.updateUserProfile(it)
            }
        }

        // 观察操作结果
        viewModel.operationResult.observe(viewLifecycleOwner) { message ->
            message?.let {
                view?.let { v -> Material3Feedback.showInfo(v, it) }
                viewModel.clearOperationResult()
            }
        }

        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // 可以在这里显示/隐藏加载指示器
        }
    }

    /**
     * 处理菜单项点击
     */
    private fun handleMenuItemClick(menuId: String) {
        when (menuId) {
            "recycle_bin" -> navigateToRecycleBin()
            "data_export" -> navigateToDataExport()
            "app_settings" -> navigateToAppSettings()
            "donation" -> navigateToDonation()
        }
    }

    /**
     * 导航到应用设置
     */
    private fun navigateToAppSettings() {
        try {
            findNavController().navigate(R.id.action_profile_to_app_settings)
        } catch (e: Exception) {
            Toast.makeText(context, "设置页面开发中", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 导航到个人资料编辑页面
     */
    private fun navigateToEditProfile() {
        try {
            findNavController().navigate(R.id.action_profile_to_edit_profile)
        } catch (e: Exception) {
            Toast.makeText(context, "个人资料编辑功能开发中", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 导航到回收站
     */
    private fun navigateToRecycleBin() {
        try {
            findNavController().navigate(R.id.action_profile_to_recycle_bin)
        } catch (e: Exception) {
            Toast.makeText(context, "回收站功能开发中", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 导航到数据导出
     */
    private fun navigateToDataExport() {
        try {
            findNavController().navigate(R.id.action_profile_to_data_export)
        } catch (e: Exception) {
            Toast.makeText(context, "数据导出功能开发中", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 导航到打赏页面
     */
    private fun navigateToDonation() {
        try {
            findNavController().navigate(R.id.action_profile_to_donation)
        } catch (e: Exception) {
            Toast.makeText(context, "打赏页面开发中", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * 格式化金额显示
         */
        private fun formatValue(value: Double): String {
            return when {
                value >= 10000 -> "¥${String.format("%.1f", value / 10000)}万"
                value >= 1000 -> "¥${String.format("%.1f", value / 1000)}K"
                else -> "¥${String.format("%.0f", value)}"
            }
        }
    }
}