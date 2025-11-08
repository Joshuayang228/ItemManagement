package com.example.itemmanagement.ui.warehouse

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.databinding.FragmentFilterBottomSheetBinding
import com.example.itemmanagement.ui.warehouse.FilterCategory
import com.example.itemmanagement.ui.warehouse.WarehouseViewModel
import com.example.itemmanagement.ui.warehouse.WarehouseViewModelFactory
import com.example.itemmanagement.ui.warehouse.components.*
import com.example.itemmanagement.ui.warehouse.managers.*
import com.example.itemmanagement.utils.SnackbarHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

/**
 * 筛选底部面板 Fragment V2 (重构版)
 * 
 * 重构改进：
 * 1. 模块化设计 - 使用专门的管理器和组件
 * 2. 更好的状态管理 - 防循环更新
 * 3. 优化的触摸事件处理 - 分层处理机制
 * 4. 性能优化 - 减少不必要的UI更新
 * 5. 内存管理 - 完善的资源清理
 * 
 * 功能与原版完全一致，但代码结构更清晰、更易维护
 */
class FilterBottomSheetFragmentV2 : BottomSheetDialogFragment() {
    
    private var _binding: FragmentFilterBottomSheetBinding? = null
    private val binding get() = _binding!!
    
    // ViewModel（与原版共享）
    private val viewModel: WarehouseViewModel by activityViewModels {
        WarehouseViewModelFactory(
            (requireActivity().application as ItemManagementApplication).repository
        )
    }
    
    // 核心管理器
    private lateinit var touchManager: BottomSheetTouchManager
    private lateinit var stateManager: FilterStateManager
    private lateinit var navigationManager: NavigationSyncManager
    private lateinit var animationManager: FilterAnimationManager
    
    // 筛选组件
    private lateinit var categoryComponent: CategoryFilterComponent
    private lateinit var locationComponent: LocationFilterComponent
    private lateinit var statusRatingComponent: StatusRatingFilterComponent
    private lateinit var valueRangeComponent: ValueRangeFilterComponent
    private lateinit var dateRangeComponent: DateRangeFilterComponent
    
    // BottomSheet行为
    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 🔄 强制重新加载筛选选项（确保数据最新）
        viewModel.loadFilterOptions()
        
        // 按顺序初始化各个模块
        initializeManagers()
        initializeComponents()
        setupBottomSheetBehavior()
        setupResetButton()
        setupCloseAutoApply()  // 🎯 设置关闭时自动应用（保留）
        
        // 延迟启动状态观察和触摸处理，确保所有组件准备就绪
        view.post {
            startStateObservation()
            touchManager.setupTouchHandling()  // 🎯 触摸管理器已集成失焦逻辑
            // 强制更新导航高亮到初始状态
            navigationManager.forceUpdateNavigation()
        }
    }
    
    /**
     * 初始化核心管理器
     */
    private fun initializeManagers() {
        // 动画管理器（不依赖其他管理器）
        animationManager = FilterAnimationManager()
        
        // 触摸事件管理器（提供BottomSheetBehavior访问和Fragment引用）
        touchManager = BottomSheetTouchManager(binding, {
            bottomSheetBehavior
        }, this)  // 🎯 传入Fragment引用
        
        // 导航同步管理器
        navigationManager = NavigationSyncManager(binding, touchManager)
        navigationManager.setupNavigation()
        
        // 状态管理器
        stateManager = FilterStateManager(viewModel, binding)
    }
    
    /**
     * 初始化筛选组件
     */
    private fun initializeComponents() {
        // 创建各个筛选组件
        categoryComponent = CategoryFilterComponent(
            binding, viewModel, viewLifecycleOwner, animationManager
        ).apply { 
            initialize()
            stateManager.registerFilterComponent(this)
        }
        
        locationComponent = LocationFilterComponent(
            binding, viewModel, viewLifecycleOwner, animationManager
        ).apply { 
            initialize()
            stateManager.registerFilterComponent(this)
        }
        
        statusRatingComponent = StatusRatingFilterComponent(
            binding, viewModel, viewLifecycleOwner, animationManager
        ).apply { 
            initialize()
            stateManager.registerFilterComponent(this)
        }
        
        valueRangeComponent = ValueRangeFilterComponent(
            binding, viewModel
        ).apply { 
            initialize()
            stateManager.registerFilterComponent(this)
        }
        
        dateRangeComponent = DateRangeFilterComponent(
            this, binding, viewModel
        ).apply { 
            initialize()
            stateManager.registerFilterComponent(this)
        }
        
        // 为组件设置值变化监听器
        setupComponentListeners()
    }
    
    /**
     * 设置组件值变化监听器
     */
    private fun setupComponentListeners() {
        categoryComponent.setOnValueChangedListener { value ->
            onComponentValueChanged("category", value)
        }
        
        locationComponent.setOnValueChangedListener { value ->
            onComponentValueChanged("location", value)
        }
        
        statusRatingComponent.setOnValueChangedListener { value ->
            onComponentValueChanged("status_rating", value)
        }
        
        valueRangeComponent.setOnValueChangedListener { value ->
            onComponentValueChanged("value_range", value)
        }
        
        dateRangeComponent.setOnValueChangedListener { value ->
            onComponentValueChanged("date_range", value)
        }
    }
    
    /**
     * 组件值变化回调
     */
    private fun onComponentValueChanged(componentType: String, value: Any) {
        // 可以在这里添加统一的值变化处理逻辑
        // 例如：日志记录、分析埋点等
        android.util.Log.d("FilterV2", "Component $componentType changed: $value")
    }
    
    /**
     * 设置BottomSheet行为
     */
    private fun setupBottomSheetBehavior() {
        dialog?.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as com.google.android.material.bottomsheet.BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                bottomSheetBehavior = BottomSheetBehavior.from(sheet)
                configureBottomSheetBehavior(bottomSheetBehavior!!)
            }
        }
    }
    
    /**
     * 配置BottomSheet行为参数
     */
    private fun configureBottomSheetBehavior(behavior: BottomSheetBehavior<View>) {
        // 配置多状态展开 - 恢复原始3状态配置支持悬浮按钮
        behavior.isFitToContents = false        // 关键：支持多状态
        behavior.halfExpandedRatio = 0.78f      // 半展开状态占屏幕78%
        behavior.isDraggable = false            // 🎯 禁用拖拽，筛选界面固定
        behavior.skipCollapsed = false          // 支持折叠状态  
        behavior.isHideable = true
        
        // 设置全展开状态的顶部偏移，保持圆角bottomsheet形态
        val displayMetrics = resources.displayMetrics
        val statusBarHeight = resources.getIdentifier("status_bar_height", "dimen", "android")
            .let { id -> if (id > 0) resources.getDimensionPixelSize(id) else 0 }
        val topOffset = statusBarHeight + (32 * displayMetrics.density).toInt() // 状态栏高度 + 32dp  
        behavior.expandedOffset = topOffset
        
        // 直接设置初始状态为78%屏幕（半展开状态），避免peekHeight引起的闪现
        behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        
        // 添加状态监听器
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                onBottomSheetStateChanged(newState)
            }
            
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                onBottomSheetSlide(slideOffset)
            }
        })
    }
    
    /**
     * BottomSheet状态变化回调
     */
    private fun onBottomSheetStateChanged(newState: Int) {
        when (newState) {
            BottomSheetBehavior.STATE_EXPANDED -> {
                // 全展开状态
            }
            BottomSheetBehavior.STATE_COLLAPSED -> {
                // 折叠状态
            }
            BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                // 半展开状态
            }
            BottomSheetBehavior.STATE_DRAGGING -> {
                // 🎯 用户开始拖动关闭时，立即失焦所有输入框
                clearAllInputFocus()
                hideKeyboard()
            }
            BottomSheetBehavior.STATE_HIDDEN -> {
                // 隐藏状态，关闭Fragment
                dismiss()
            }
        }
    }
    
    /**
     * BottomSheet滑动回调
     */
    private fun onBottomSheetSlide(slideOffset: Float) {
        // 可以根据滑动进度添加视觉效果
    }
    
    /**
     * 设置标题栏重置按钮
     */
    private fun setupResetButton() {
        binding.resetButton.setOnClickListener {
            resetAllFilters()
        }
    }
    
    /**
     * 开始状态观察
     */
    private fun startStateObservation() {
        stateManager.observeState(lifecycleScope)
    }
    
    /**
     * 重置所有筛选条件
     */
    private fun resetAllFilters() {
        lifecycleScope.launch {
            // 使用状态管理器统一重置
            stateManager.resetAllFilters()
            
            // 滚动到顶部（移除状态管理冲突）
            navigationManager.scrollToTop()
        }
    }
    
    /**
     * 验证所有输入
     */
    private fun validateAllInputs(): Boolean {
        var isValid = true
        
        // 验证数值范围输入
        if (!valueRangeComponent.validateAllInputs()) {
            isValid = false
        }
        
        // 验证日期范围输入
        if (!dateRangeComponent.validateAllDateRanges()) {
            isValid = false
        }
        
        if (!isValid) {
            SnackbarHelper.show(requireView(), "请检查输入内容")
        }
        
        return isValid
    }
    
    /**
     * 获取筛选摘要（用于调试或分析）
     */
    fun getFilterSummary(): Map<String, String> {
        return mapOf(
            "categories" to categoryComponent.getSelectedValues().joinToString(", "),
            "locations" to locationComponent.getLocationSummary(),
            "status_rating" to statusRatingComponent.getStatusRatingSummary(),
            "value_range" to valueRangeComponent.getValueRangeSummary(),
            "date_range" to dateRangeComponent.getDateFilterSummary()
        )
    }
    
    /**
     * 强制刷新UI
     */
    fun refreshUI() {
        lifecycleScope.launch {
            stateManager.forceUpdateUI()
        }
    }
    
    /**
     * 滚动到指定区域
     */
    fun scrollToSection(category: FilterCategory) {
        val position = FilterCategory.values().indexOf(category)
        navigationManager.setSelectedPosition(position)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        
        // 清理所有管理器
        cleanupManagers()
        
        // 清理所有组件
        cleanupComponents()
        
        // 清理绑定
        _binding = null
    }
    
    /**
     * 清理管理器
     */
    private fun cleanupManagers() {
        if (::touchManager.isInitialized) {
            touchManager.cleanup()
        }
        
        // NavigationManager 使用简洁算法，无需特别清理
        
        if (::animationManager.isInitialized) {
            animationManager.cleanup()
        }
        
        if (::stateManager.isInitialized) {
            stateManager.cleanup()
        }
    }
    
    /**
     * 清理组件
     */
    private fun cleanupComponents() {
        if (::categoryComponent.isInitialized) {
            stateManager.unregisterFilterComponent(categoryComponent)
            categoryComponent.cleanup()
        }
        
        if (::locationComponent.isInitialized) {
            stateManager.unregisterFilterComponent(locationComponent)
            locationComponent.cleanup()
        }
        
        if (::statusRatingComponent.isInitialized) {
            stateManager.unregisterFilterComponent(statusRatingComponent)
            statusRatingComponent.cleanup()
        }
        
        if (::valueRangeComponent.isInitialized) {
            stateManager.unregisterFilterComponent(valueRangeComponent)
            valueRangeComponent.cleanup()
        }
        
        if (::dateRangeComponent.isInitialized) {
            stateManager.unregisterFilterComponent(dateRangeComponent)
            dateRangeComponent.cleanup()
        }
    }
    
    // ==================== 🎯 失焦逻辑已集成到 BottomSheetTouchManager ====================
    
    /**
     * 设置关闭时自动应用
     * 功能：关闭筛选界面时强制所有输入框失焦，确保输入内容被应用
     */
    private fun setupCloseAutoApply() {
        // 方案A: 使用 onDismiss 监听
        dialog?.setOnDismissListener {
            clearAllInputFocus()
            hideKeyboard()
        }
    }
    
    /**
     * 清除所有输入框焦点
     */
    private fun clearAllInputFocus() {
        try {
            // 方法1: 清除当前焦点
            activity?.currentFocus?.clearFocus()
            
            // 方法2: 逐个清除所有输入框焦点（确保完整性）
            binding.valueRangeSection.minQuantityInput?.clearFocus()
            binding.valueRangeSection.maxQuantityInput?.clearFocus()
            binding.valueRangeSection.minPriceInput?.clearFocus()
            binding.valueRangeSection.maxPriceInput?.clearFocus()
            binding.coreSection.brandDropdown?.clearFocus()
            
            android.util.Log.d("FilterV2", "✅ 已清除所有输入框焦点")
        } catch (e: Exception) {
            android.util.Log.e("FilterV2", "清除输入框焦点失败: ${e.message}")
        }
    }
    
    /**
     * 隐藏软键盘
     */
    private fun hideKeyboard() {
        try {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view?.windowToken, 0)
            android.util.Log.d("FilterV2", "✅ 已隐藏软键盘")
        } catch (e: Exception) {
            android.util.Log.e("FilterV2", "隐藏键盘失败: ${e.message}")
        }
    }
    
    override fun onDismiss(dialog: DialogInterface) {
        // 🎯 关闭时强制失焦和应用
        clearAllInputFocus()
        hideKeyboard()
        
        // 延迟确保失焦事件完成
        view?.postDelayed({
            android.util.Log.d("FilterV2", "🔄 筛选界面关闭，所有输入已应用")
        }, 100)
        
        super.onDismiss(dialog)
    }
    
    
    companion object {
        /**
         * 创建新实例
         */
        fun newInstance(): FilterBottomSheetFragmentV2 {
            return FilterBottomSheetFragmentV2()
        }
        
        const val TAG = "FilterBottomSheetV2"
    }
}
