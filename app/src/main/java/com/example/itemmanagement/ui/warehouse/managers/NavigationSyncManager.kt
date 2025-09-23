package com.example.itemmanagement.ui.warehouse.managers

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itemmanagement.databinding.FragmentFilterBottomSheetBinding
import com.example.itemmanagement.ui.warehouse.FilterCategory
import com.example.itemmanagement.ui.warehouse.adapters.FilterNavigationAdapterV2
import com.example.itemmanagement.ui.warehouse.adapters.FilterNavigationItem

/**
 * 导航同步管理器 - 使用原始简洁算法重新实现
 * 
 * 核心功能:
 * 1. 根据滚动位置自动高亮对应的导航项（基于距离最近算法）
 * 2. 点击导航项时滚动到对应区域
 * 3. 简单可靠的状态管理，避免复杂的状态冲突
 * 
 * 算法来源: 直接复制原始FilterBottomSheetFragment.kt中的成熟算法
 */
class NavigationSyncManager(
    private val binding: FragmentFilterBottomSheetBinding,
    private val touchManager: BottomSheetTouchManager
) {
    // 导航RecyclerView适配器
    private lateinit var navigationAdapter: FilterNavigationAdapterV2
    
    // 筛选分类列表（完全按照原始代码）
    private val filterCategories = FilterCategory.values().toList()
    
    // 防止循环更新的简单标志（参考原始代码的isUpdatingUI）
    private var isUpdatingUI = false
    
    /**
     * 初始化导航功能
     */
    fun setupNavigation() {
        setupNavigationRecyclerView()
        setupScrollSyncWithNavigation()
    }
    
    /**
     * 设置导航RecyclerView
     */
    private fun setupNavigationRecyclerView() {
        navigationAdapter = FilterNavigationAdapterV2 { category, position ->
            scrollToSection(category)
        }
        
        // 将FilterCategory转换为FilterNavigationItem并提交给适配器
        val navigationItems = filterCategories.map { category ->
            FilterNavigationItem(category = category)
        }
        navigationAdapter.submitList(navigationItems)
        
        binding.navigationRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = navigationAdapter
        }
    }
    
    /**
     * 设置滚动同步（完全基于原始代码）
     */
    private fun setupScrollSyncWithNavigation() {
        binding.contentScrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            // 功能1: 处理导航高亮同步
            if (!isUpdatingUI) {
                updateNavigationHighlight(scrollY)
            }
            
            // 功能2: 通知TouchManager处理滚动状态
            touchManager.handleScrollStateChange(scrollY, oldScrollY)
        }
    }
    
    /**
     * 根据滚动位置更新导航高亮（基于可视区域上1/6锚点算法）
     * 
     * 算法逻辑：
     * 1. 使用可视区域上1/6点作为视觉锚点（更保守的切换点）
     * 2. 计算各区域中心到视觉锚点的距离
     * 3. 选择距离最近的区域进行高亮
     * 4. 比1/4点更接近顶部，让当前区域保持更久的高亮状态
     * 
     * 注：通过底部空白区域解决日期区域高亮问题，无需特殊边界保护逻辑
     */
    private fun updateNavigationHighlight(scrollY: Int) {
        val sections = listOf(
            FilterCategory.CORE to binding.coreSection.root,
            FilterCategory.LOCATION to binding.locationSection.root,
            FilterCategory.STATUS_RATING to binding.statusRatingSection.root,
            FilterCategory.VALUE_RANGE to binding.valueRangeSection.root,
            FilterCategory.DATE to binding.dateSection.root
        )
        
        // 获取ScrollView的高度用于计算可见区域
        val scrollViewHeight = binding.contentScrollView.height
        val visibleAnchor = scrollY + (scrollViewHeight * 0.167).toInt()  // 使用上1/6点作为锚点
        
        // 找到距离可视锚点最近的区域
        var currentSection = FilterCategory.CORE
        var minDistance = Int.MAX_VALUE
        
        for ((category, view) in sections) {
            val viewTop = view.top
            val viewBottom = view.bottom
            val viewCenter = (viewTop + viewBottom) / 2
            
            val distance = kotlin.math.abs(visibleAnchor - viewCenter)
            
            if (distance < minDistance) {
                minDistance = distance
                currentSection = category
            }
        }
        
        // 更新导航适配器的选中状态
        val position = filterCategories.indexOf(currentSection)
        if (position >= 0) {
            navigationAdapter.setSelectedPosition(position)
        }
    }
    
    /**
     * 滚动到指定区域（完全基于原始代码）
     * 
     * 原始逻辑：
     * 1. 设置isUpdatingUI标志防止滚动冲突
     * 2. 根据category选择对应的targetView
     * 3. 使用smoothScrollTo滚动到view.top
     * 4. 100ms后重置标志并重新应用滚动保护
     */
    private fun scrollToSection(category: FilterCategory) {
        val targetView = when (category) {
            FilterCategory.CORE -> binding.coreSection.root
            FilterCategory.LOCATION -> binding.locationSection.root
            FilterCategory.STATUS_RATING -> binding.statusRatingSection.root
            FilterCategory.VALUE_RANGE -> binding.valueRangeSection.root
            FilterCategory.DATE -> binding.dateSection.root
        }
        
        // 🔥 关键修复：设置更新标志，防止滚动过程中的导航更新冲突
        isUpdatingUI = true
        
        binding.contentScrollView.post {
            binding.contentScrollView.smoothScrollTo(0, targetView.top)
            
            // 导航点击后重新确保滚动保护有效并重置状态
            binding.contentScrollView.postDelayed({
                isUpdatingUI = false  // 🔥 重置标志
                touchManager.reapplyScrollProtection()
            }, 100) // 延迟100ms确保滚动完成后重新应用保护
        }
    }
    
    /**
     * 禁用导航更新（用于状态管理器更新UI时）
     */
    fun disableNavigationUpdates() {
        isUpdatingUI = true
    }
    
    /**
     * 启用导航更新
     */
    fun enableNavigationUpdates() {
        isUpdatingUI = false
    }
    
    /**
     * 设置选中位置（供外部调用）
     */
    fun setSelectedPosition(position: Int) {
        if (position in 0 until filterCategories.size) {
            navigationAdapter.setSelectedPosition(position)
        }
    }
    
    /**
     * 强制更新导航高亮
     */
    fun forceUpdateNavigation() {
        val currentScrollY = binding.contentScrollView.scrollY
        updateNavigationHighlight(currentScrollY)
    }
    
    /**
     * 平滑滚动到顶部
     */
    fun scrollToTop() {
        binding.contentScrollView.smoothScrollTo(0, 0)
        // 简单直接：立即选中第一个导航项
        setSelectedPosition(0)
    }
}