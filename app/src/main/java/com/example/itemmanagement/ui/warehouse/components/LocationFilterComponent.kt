package com.example.itemmanagement.ui.warehouse.components

import android.view.LayoutInflater
import androidx.lifecycle.LifecycleOwner
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentFilterBottomSheetBinding
import com.example.itemmanagement.ui.warehouse.FilterState
import com.example.itemmanagement.ui.warehouse.WarehouseViewModel
import com.example.itemmanagement.ui.warehouse.components.base.BaseFilterComponent
import com.example.itemmanagement.ui.warehouse.components.base.MultiSelectFilterComponent
import com.example.itemmanagement.ui.warehouse.managers.FilterAnimationManager
import com.google.android.material.chip.Chip

/**
 * 位置筛选组件
 * 
 * 功能：
 * 1. 管理区域多选ChipGroup
 * 2. 管理容器单选ChipGroup
 * 3. 支持动态展开/收起
 * 4. 与ViewModel同步状态
 */
class LocationFilterComponent(
    private val binding: FragmentFilterBottomSheetBinding,
    private val viewModel: WarehouseViewModel,
    private val lifecycleOwner: LifecycleOwner,
    private val animationManager: FilterAnimationManager
) : BaseFilterComponent(), MultiSelectFilterComponent {
    
    companion object {
        private const val COMPONENT_ID = "location_filter"
    }
    
    // 当前选中的位置区域集合
    private var selectedLocationAreas = mutableSetOf<String>()
    
    // 当前选中的容器（单选）
    private var selectedContainer = ""
    
    // 防止无限循环的状态更新标志
    private var isUpdatingFromState = false
    
    // 所有可用区域列表
    private var availableLocationAreas = listOf<String>()
    
    // 所有可用容器列表
    private var availableContainers = listOf<String>()
    
    override fun getComponentId(): String = COMPONENT_ID
    
    /**
     * 初始化组件
     */
    fun initialize() {
        setupLocationAreaChipGroup()
        setupContainerChipGroup()
        observeViewModelData()
        setReady()
    }
    
    /**
     * 设置位置区域ChipGroup
     */
    private fun setupLocationAreaChipGroup() {
        // 设置ChipGroup的多选监听器
        binding.locationSection.locationAreaChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            // 如果正在从FilterState更新UI，跳过这个回调
            if (isUpdatingFromState) {
                return@setOnCheckedStateChangeListener
            }
            
            if (isReady()) {
                val newSelectedAreas = checkedIds.mapNotNull { chipId ->
                    val chip = group.findViewById<Chip>(chipId)
                    chip?.text?.toString()
                }.toSet()
                
                if (newSelectedAreas != selectedLocationAreas) {
                    selectedLocationAreas.clear()
                    selectedLocationAreas.addAll(newSelectedAreas)
                    
                    // 🔑 核心修复：传递Set副本，避免对象引用问题
                    viewModel.updateLocationAreas(newSelectedAreas)
                    notifyValueChanged(newSelectedAreas)
                }
            }
        }
        
        // 设置展开/收起动画
        animationManager.setupChipGroupExpandCollapse(
            binding.locationSection.locationAreaChipGroup,
            binding.locationSection.locationAreaExpandButton,
            "location_area_expand"
        )
    }
    
    /**
     * 设置容器ChipGroup（单选模式）
     */
    private fun setupContainerChipGroup() {
        // 容器使用单选模式，但ChipGroup本身不支持真正的单选
        // 所以我们手动处理单选逻辑
        binding.locationSection.containerChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            // 如果正在从FilterState更新UI，跳过这个回调
            if (isUpdatingFromState) {
                return@setOnCheckedStateChangeListener
            }
            
            if (isReady()) {
                if (checkedIds.isEmpty()) {
                    // 没有选中任何容器
                    selectedContainer = ""
                    viewModel.setContainer("")
                    notifyValueChanged("")
                } else if (checkedIds.size == 1) {
                    // 选中一个容器
                    val chipId = checkedIds.first()
                    val chip = group.findViewById<Chip>(chipId)
                    val container = chip?.text?.toString() ?: ""
                    
                    if (container != selectedContainer) {
                        selectedContainer = container
                        viewModel.setContainer(container)
                        notifyValueChanged(container)
                    }
                } else {
                    // 选中多个容器，只保留最后一个
                    val lastSelectedId = checkedIds.last()
                    
                    // 取消其他选择，只保留最后一个
                    checkedIds.take(checkedIds.size - 1).forEach { chipId ->
                        val chip = group.findViewById<Chip>(chipId)
                        chip?.isChecked = false
                    }
                    
                    val lastSelectedChip = group.findViewById<Chip>(lastSelectedId)
                    val container = lastSelectedChip?.text?.toString() ?: ""
                    
                    if (container != selectedContainer) {
                        selectedContainer = container
                        viewModel.setContainer(container)
                        notifyValueChanged(container)
                    }
                }
            }
        }
        
        // 设置展开/收起动画
        animationManager.setupChipGroupExpandCollapse(
            binding.locationSection.containerChipGroup,
            binding.locationSection.containerExpandButton,
            "container_expand"
        )
    }
    
    /**
     * 观察ViewModel数据变化
     */
    private fun observeViewModelData() {
        // 观察位置区域列表变化
        viewModel.locationAreas.observe(lifecycleOwner) { areas ->
            availableLocationAreas = areas ?: emptyList()
            updateLocationAreasChipGroup(areas ?: emptyList())
        }
        
        // 观察容器列表变化
        viewModel.containers.observe(lifecycleOwner) { containers ->
            availableContainers = containers ?: emptyList()
            updateContainersChipGroup(containers ?: emptyList())
        }
    }
    
    /**
     * 更新位置区域ChipGroup
     */
    private fun updateLocationAreasChipGroup(areas: List<String>) {
        // 保存当前滚动位置
        val currentScrollY = binding.contentScrollView.scrollY
        
        // 清除现有chips
        binding.locationSection.locationAreaChipGroup.removeAllViews()
        
        // 添加新的chips
        areas.forEach { area ->
            val layoutInflater = LayoutInflater.from(binding.root.context)
            val chip = layoutInflater.inflate(
                R.layout.item_suggestion_chip, 
                binding.locationSection.locationAreaChipGroup, 
                false
            ) as Chip
            
            chip.apply {
                text = area
                isCheckable = true
                isChecked = selectedLocationAreas.contains(area)
            }
            
            binding.locationSection.locationAreaChipGroup.addView(chip)
        }
        
        // 重新设置展开/收起功能
        animationManager.setupChipGroupExpandCollapse(
            binding.locationSection.locationAreaChipGroup,
            binding.locationSection.locationAreaExpandButton,
            "location_area_expand"
        )
        
        // 恢复滚动位置
        binding.contentScrollView.post {
            binding.contentScrollView.scrollTo(0, currentScrollY)
        }
    }
    
    /**
     * 更新容器ChipGroup
     */
    private fun updateContainersChipGroup(containers: List<String>) {
        // 保存当前滚动位置
        val currentScrollY = binding.contentScrollView.scrollY
        
        // 清除现有chips
        binding.locationSection.containerChipGroup.removeAllViews()
        
        // 添加新的chips
        containers.forEach { container ->
            val layoutInflater = LayoutInflater.from(binding.root.context)
            val chip = layoutInflater.inflate(
                R.layout.item_suggestion_chip, 
                binding.locationSection.containerChipGroup, 
                false
            ) as Chip
            
            chip.apply {
                text = container
                isCheckable = true
                isChecked = container == selectedContainer
            }
            
            binding.locationSection.containerChipGroup.addView(chip)
        }
        
        // 重新设置展开/收起功能
        animationManager.setupChipGroupExpandCollapse(
            binding.locationSection.containerChipGroup,
            binding.locationSection.containerExpandButton,
            "container_expand"
        )
        
        // 恢复滚动位置
        binding.contentScrollView.post {
            binding.contentScrollView.scrollTo(0, currentScrollY)
        }
    }
    
    override fun updateFromState(filterState: FilterState) {
        isUpdatingFromState = true
        try {
            // 更新位置区域选择状态
            updateLocationAreasSelection(filterState.locationAreas)
            
            // 更新容器选择状态
            updateContainerSelection(filterState.container)
        } finally {
            isUpdatingFromState = false
        }
    }
    
    /**
     * 更新位置区域选择状态
     */
    private fun updateLocationAreasSelection(areas: Set<String>) {
        if (areas != selectedLocationAreas) {
            selectedLocationAreas.clear()
            selectedLocationAreas.addAll(areas)
            
            // 更新ChipGroup选中状态
            for (i in 0 until binding.locationSection.locationAreaChipGroup.childCount) {
                val chip = binding.locationSection.locationAreaChipGroup.getChildAt(i) as? Chip
                chip?.let {
                    it.isChecked = areas.contains(it.text.toString())
                }
            }
        }
    }
    
    /**
     * 更新容器选择状态
     */
    private fun updateContainerSelection(container: String) {
        if (container != selectedContainer) {
            selectedContainer = container
            
            // 更新ChipGroup选中状态
            for (i in 0 until binding.locationSection.containerChipGroup.childCount) {
                val chip = binding.locationSection.containerChipGroup.getChildAt(i) as? Chip
                chip?.let {
                    it.isChecked = it.text.toString() == container
                }
            }
        }
    }
    
    override fun resetToDefault() {
        // 重置位置区域选择
        selectedLocationAreas.clear()
        binding.locationSection.locationAreaChipGroup.clearCheck()
        
        // 重置容器选择
        selectedContainer = ""
        binding.locationSection.containerChipGroup.clearCheck()
    }
    
    // MultiSelectFilterComponent implementation (for location areas)
    override fun getSelectedValues(): Set<String> {
        return selectedLocationAreas.toSet()
    }
    
    override fun setSelectedValues(values: Set<String>) {
        selectedLocationAreas.clear()
        selectedLocationAreas.addAll(values)
        updateLocationAreasSelection(values)
        // 🔑 核心修复：传递Set副本，避免对象引用问题
        viewModel.updateLocationAreas(values.toSet())
    }
    
    override fun clearSelection() {
        setSelectedValues(emptySet())
        setSelectedContainer("")
    }
    
    override fun getAllOptions(): List<String> {
        return availableLocationAreas
    }
    
    override fun updateOptions(options: List<String>) {
        availableLocationAreas = options
        updateLocationAreasChipGroup(options)
    }
    
    /**
     * 获取当前选中的容器
     */
    fun getSelectedContainer(): String {
        return selectedContainer
    }
    
    /**
     * 设置选中的容器
     */
    fun setSelectedContainer(container: String) {
        selectedContainer = container
        updateContainerSelection(container)
        viewModel.setContainer(container)
    }
    
    /**
     * 获取所有可用容器
     */
    fun getAvailableContainers(): List<String> {
        return availableContainers
    }
    
    /**
     * 更新容器选项
     */
    fun updateContainerOptions(containers: List<String>) {
        availableContainers = containers
        updateContainersChipGroup(containers)
    }
    
    /**
     * 检查是否有选中的位置信息
     */
    fun hasLocationSelected(): Boolean {
        return selectedLocationAreas.isNotEmpty() || selectedContainer.isNotEmpty()
    }
    
    /**
     * 获取位置筛选摘要
     */
    fun getLocationSummary(): String {
        val parts = mutableListOf<String>()
        
        if (selectedLocationAreas.isNotEmpty()) {
            parts.add("区域: ${selectedLocationAreas.joinToString(", ")}")
        }
        
        if (selectedContainer.isNotEmpty()) {
            parts.add("容器: $selectedContainer")
        }
        
        return if (parts.isEmpty()) {
            "未选择位置"
        } else {
            parts.joinToString(" | ")
        }
    }
    

    override fun cleanup() {
        super.cleanup()
        
        // 清理ChipGroup监听器
        binding.locationSection.locationAreaChipGroup.setOnCheckedStateChangeListener(null)
        binding.locationSection.containerChipGroup.setOnCheckedStateChangeListener(null)
        
        // 清空选中状态
        selectedLocationAreas.clear()
        selectedContainer = ""
    }
}
