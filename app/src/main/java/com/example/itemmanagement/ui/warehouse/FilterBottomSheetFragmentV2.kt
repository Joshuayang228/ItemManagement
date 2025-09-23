package com.example.itemmanagement.ui.warehouse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.databinding.FragmentFilterBottomSheetBinding
import com.example.itemmanagement.ui.warehouse.FilterCategory
import com.example.itemmanagement.ui.warehouse.WarehouseViewModel
import com.example.itemmanagement.ui.warehouse.WarehouseViewModelFactory
import com.example.itemmanagement.ui.warehouse.components.*
import com.example.itemmanagement.ui.warehouse.managers.*
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
        
        // 按顺序初始化各个模块
        initializeManagers()
        initializeComponents()
        setupBottomSheetBehavior()
        setupResetButton()
        
        // 延迟启动状态观察和触摸处理，确保所有组件准备就绪
        view.post {
            startStateObservation()
            touchManager.setupTouchHandling()
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
        
        // 触摸事件管理器（提供BottomSheetBehavior访问）
        touchManager = BottomSheetTouchManager(binding) {
            bottomSheetBehavior
        }
        
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
        behavior.isDraggable = true
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
            android.widget.Toast.makeText(
                requireContext(),
                "请检查输入内容",
                android.widget.Toast.LENGTH_SHORT
            ).show()
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
