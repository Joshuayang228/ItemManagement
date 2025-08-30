package com.example.itemmanagement.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentProfileBinding
// import com.example.itemmanagement.utils.formatCurrency
import java.text.SimpleDateFormat
import java.util.*

/**
 * "我的"主页面Fragment
 * 展示用户信息、统计数据和功能入口
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProfileViewModel
    
    private val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())

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

        setupObservers()
        setupClickListeners()
        
        // 加载数据
        viewModel.loadUserData()
    }

    /**
     * 设置数据观察者
     */
    private fun setupObservers() {
        // 观察用户统计摘要
        viewModel.userStatsSummary.observe(viewLifecycleOwner) { summary ->
            updateUserStats(summary)
        }

        // 观察库存概览
        viewModel.inventoryOverview.observe(viewLifecycleOwner) { overview ->
            updateInventoryOverview(overview)
        }

        // 观察操作结果
        viewModel.operationResult.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.clearOperationResult()
            }
        }

        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // 可以在这里显示/隐藏加载指示器
        }
    }

    /**
     * 更新用户统计信息
     */
    private fun updateUserStats(summary: UserStatsSummary) {
        binding.apply {
            tvNickname.text = summary.nickname
            tvLevelTitle.text = "LV.${summary.level} ${summary.levelTitle}"
            
            // 经验值显示
            val expText = if (summary.level >= 10) {
                "最高等级"
            } else {
                "${summary.experiencePoints}/${summary.experiencePoints + summary.expToNextLevel} EXP"
            }
            tvExperience.text = expText
            
            // 经验进度条
            progressExperience.progress = (summary.progress * 100).toInt()
            
            // 使用统计数据
            tvUsageDays.text = summary.usageDays.toString()
            tvTotalItems.text = summary.totalItemsManaged.toString()
            tvExpiredAvoided.text = summary.expiredItemsAvoided.toString()
            tvSavedValue.text = formatValue(summary.totalSavedValue)
        }
    }

    /**
     * 更新库存概览
     */
    private fun updateInventoryOverview(overview: InventoryOverview) {
        binding.apply {
            tvCurrentItems.text = overview.totalItems.toString()
            tvTotalValue.text = formatValue(overview.totalValue)
            tvExpiringSoon.text = overview.expiringSoon.toString()
            tvLowStock.text = overview.lowStock.toString()
        }
    }

    /**
     * 设置点击事件监听器
     */
    private fun setupClickListeners() {
        binding.apply {
            // 设置按钮点击
            btnSettings.setOnClickListener {
                navigateToAppSettings()
            }

            // 个人信息卡片点击（编辑个人资料）
            cardPersonalInfo.setOnClickListener {
                navigateToEditProfile()
            }
            
            // 库存概览卡片点击
            cardInventoryOverview.setOnClickListener {
                Toast.makeText(context, "库存详情功能开发中", Toast.LENGTH_SHORT).show()
            }
            
            // 功能菜单项点击事件
            itemRecycleBin.setOnClickListener {
                navigateToRecycleBin()
            }
            
            itemAppSettings.setOnClickListener {
                navigateToAppSettings()
            }
            
            itemDataExport.setOnClickListener {
                navigateToDataExport()
            }
            
            itemDonation.setOnClickListener {
                navigateToDonation()
            }
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
            findNavController().navigate(R.id.action_function_to_data_export)
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