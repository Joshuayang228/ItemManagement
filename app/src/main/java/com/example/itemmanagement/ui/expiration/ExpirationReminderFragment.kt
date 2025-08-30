package com.example.itemmanagement.ui.expiration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.MainActivity
import com.example.itemmanagement.R
import com.example.itemmanagement.adapter.ItemAdapter
import com.example.itemmanagement.data.mapper.toItem
import com.example.itemmanagement.databinding.FragmentExpirationReminderBinding
import com.example.itemmanagement.reminder.model.ReminderSummary
import com.google.android.material.snackbar.Snackbar

class ExpirationReminderFragment : Fragment() {

    private var _binding: FragmentExpirationReminderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExpirationReminderViewModel by viewModels {
        val app = requireActivity().application as ItemManagementApplication
        ExpirationReminderViewModelFactory(
            app.repository,
            app.reminderSettingsRepository,
            app.reminderManager
        )
    }

    private lateinit var expiredAdapter: ItemAdapter
    private lateinit var expiringAdapter: ItemAdapter
    private lateinit var lowStockAdapter: ItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpirationReminderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerViews()
        observeViewModel()
        setupSettingsButton()
        setupTestNotificationButton()
        
        // 初始加载数据
        viewModel.loadReminderData()
    }
    
    private fun setupSettingsButton() {
        binding.buttonSettings.setOnClickListener {
            findNavController().navigate(R.id.action_expiration_reminder_to_reminder_settings)
        }
    }
    
    private fun setupTestNotificationButton() {
        // 添加测试立即提醒的功能
        binding.buttonTestNotification.setOnClickListener {
            val mainActivity = requireActivity() as MainActivity
            
            // 检查通知权限
            if (!mainActivity.hasNotificationPermission()) {
                Snackbar.make(
                    binding.root,
                    "请先授权通知权限，然后重新尝试",
                    Snackbar.LENGTH_LONG
                ).setAction("设置") {
                    // 重新申请权限
                    mainActivity.checkAndRequestNotificationPermission()
                }.show()
                return@setOnClickListener
            }
            
            val app = requireActivity().application as ItemManagementApplication
            app.reminderScheduler.sendImmediateReminder()
            
            // 显示提示消息
            Snackbar.make(
                binding.root,
                "已发送测试提醒通知 📱 请检查系统通知栏",
                Snackbar.LENGTH_LONG
            ).show()
            
            // 给用户一些视觉反馈
            it.isEnabled = false
            it.postDelayed({
                it.isEnabled = true
            }, 2000) // 2秒后重新启用按钮
        }
    }

    private fun setupRecyclerViews() {
        // 已过期物品列表
        expiredAdapter = ItemAdapter().apply {
            setOnItemClickListener { item ->
                navigateToItemDetail(item.id)
            }
        }
        binding.recyclerExpired.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = expiredAdapter
        }

        // 即将到期物品列表
        expiringAdapter = ItemAdapter().apply {
            setOnItemClickListener { item ->
                navigateToItemDetail(item.id)
            }
        }
        binding.recyclerExpiring.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = expiringAdapter
        }

        // 库存不足物品列表
        lowStockAdapter = ItemAdapter().apply {
            setOnItemClickListener { item ->
                navigateToItemDetail(item.id)
            }
        }
        binding.recyclerLowStock.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = lowStockAdapter
        }
    }

    private fun observeViewModel() {
        // 观察提醒数据
        viewModel.reminderSummary.observe(viewLifecycleOwner) { summary ->
            updateUI(summary)
        }

        // 观察加载状态
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // 观察错误消息
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    private fun updateUI(summary: ReminderSummary) {
        // 更新汇总信息
        val stats = viewModel.getReminderStats()
        updateSummaryInfo(stats)
        
        // 更新各类物品列表
        updateExpiredItems(summary)
        updateExpiringItems(summary)
        updateLowStockItems(summary)
    }

    private fun updateSummaryInfo(stats: ExpirationReminderViewModel.ReminderSummaryStats) {
        val summaryText = buildString {
            when {
                stats.expiredCount > 0 -> {
                    append("⚠️ 有 ${stats.expiredCount} 个物品已过期")
                    if (stats.upcomingExpiringCount > 0) {
                        append("，${stats.upcomingExpiringCount} 个即将到期")
                    }
                    if (stats.lowStockCount > 0) {
                        append("，${stats.lowStockCount} 个库存不足")
                    }
                }
                stats.upcomingExpiringCount > 0 -> {
                    append("📅 有 ${stats.upcomingExpiringCount} 个物品即将到期")
                    if (stats.lowStockCount > 0) {
                        append("，${stats.lowStockCount} 个库存不足")
                    }
                }
                stats.lowStockCount > 0 -> {
                    append("📦 有 ${stats.lowStockCount} 个物品库存不足")
                }
                else -> {
                    append("✅ 暂无需要关注的物品")
                }
            }
            
            if (stats.customRuleCount > 0) {
                append("，${stats.customRuleCount} 个自定义提醒")
            }
        }
        
        binding.textSummaryInfo.text = summaryText
        
        // 根据情况调整汇总信息的颜色
        when {
            stats.expiredCount > 0 -> {
                binding.textSummaryInfo.setTextColor(
                    requireContext().getColor(android.R.color.holo_red_dark)
                )
            }
            stats.upcomingExpiringCount > 0 || stats.lowStockCount > 0 -> {
                binding.textSummaryInfo.setTextColor(
                    requireContext().getColor(android.R.color.holo_orange_dark)
                )
            }
            else -> {
                binding.textSummaryInfo.setTextColor(
                    requireContext().getColor(android.R.color.darker_gray)
                )
            }
        }
    }

    private fun updateExpiredItems(summary: ReminderSummary) {
        val expiredItems = summary.expiredItems.map { it.toItem() }
        expiredAdapter.submitList(expiredItems)
        
        // 显示/隐藏相关的标题和RecyclerView
        if (expiredItems.isNotEmpty()) {
            binding.textExpiredTitle.visibility = View.VISIBLE
            binding.recyclerExpired.visibility = View.VISIBLE
        } else {
            binding.textExpiredTitle.visibility = View.GONE
            binding.recyclerExpired.visibility = View.GONE
        }
    }

    private fun updateExpiringItems(summary: ReminderSummary) {
        val expiringItems = summary.expiringItems.map { it.toItem() }
        expiringAdapter.submitList(expiringItems)
        
        // 显示/隐藏相关的标题和RecyclerView
        if (expiringItems.isNotEmpty()) {
            binding.textExpiringTitle.visibility = View.VISIBLE
            binding.recyclerExpiring.visibility = View.VISIBLE
        } else {
            binding.textExpiringTitle.visibility = View.GONE
            binding.recyclerExpiring.visibility = View.GONE
        }
    }

    private fun updateLowStockItems(summary: ReminderSummary) {
        val lowStockItems = summary.lowStockItems.map { it.item.toItem() }
        lowStockAdapter.submitList(lowStockItems)
        
        // 显示/隐藏相关的标题和RecyclerView
        if (lowStockItems.isNotEmpty()) {
            binding.textLowStockTitle.visibility = View.VISIBLE
            binding.recyclerLowStock.visibility = View.VISIBLE
        } else {
            binding.textLowStockTitle.visibility = View.GONE
            binding.recyclerLowStock.visibility = View.GONE
        }
    }

    private fun navigateToItemDetail(itemId: Long) {
        val bundle = Bundle().apply {
            putLong("itemId", itemId)
        }
        findNavController().navigate(R.id.action_expiration_reminder_to_item_detail, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}