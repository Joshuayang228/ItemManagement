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
 * 状态与评级筛选组件
 * 
 * 功能：
 * 1. 管理开封状态多选
 * 2. 管理评分多选
 * 3. 管理季节多选
 * 4. 管理标签多选
 * 5. 支持动态展开/收起
 * 6. 与ViewModel同步状态
 */
class StatusRatingFilterComponent(
    private val binding: FragmentFilterBottomSheetBinding,
    private val viewModel: WarehouseViewModel,
    private val lifecycleOwner: LifecycleOwner,
    private val animationManager: FilterAnimationManager
) : BaseFilterComponent(), MultiSelectFilterComponent {
    
    companion object {
        private const val COMPONENT_ID = "status_rating_filter"
    }
    
    // 防止无限循环的状态更新标志
    private var isUpdatingFromState = false
    
    // 开封状态映射
    private val statusChips = mapOf(
        R.id.chipUnopened to false,
        R.id.chipOpened to true
    )
    
    // 评分映射
    private val ratingChips = mapOf(
        R.id.chipRating1 to 1f,
        R.id.chipRating2 to 2f,
        R.id.chipRating3 to 3f,
        R.id.chipRating4 to 4f,
        R.id.chipRating5 to 5f
    )
    
    // 当前选中的开封状态集合
    private var selectedOpenStatuses = mutableSetOf<Boolean>()
    
    // 当前选中的评分集合
    private var selectedRatings = mutableSetOf<Float>()
    
    // 当前选中的季节集合
    private var selectedSeasons = mutableSetOf<String>()
    
    // 当前选中的标签集合
    private var selectedTags = mutableSetOf<String>()
    
    // 可用季节列表
    private var availableSeasons = listOf<String>()
    
    // 可用标签列表
    private var availableTags = listOf<String>()
    
    override fun getComponentId(): String = COMPONENT_ID
    
    /**
     * 初始化组件
     */
    fun initialize() {
        setupOpenStatusChipGroup()
        setupRatingChipGroup()
        setupSeasonChipGroup()
        setupTagsChipGroup()
        observeViewModelData()
        setReady()
    }
    
    /**
     * 设置开封状态ChipGroup
     */
    private fun setupOpenStatusChipGroup() {
        binding.statusRatingSection.openStatusChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            // 如果正在从FilterState更新UI，跳过这个回调
            if (isUpdatingFromState) {
                return@setOnCheckedStateChangeListener
            }
            
            if (isReady()) {
                val newSelectedStatuses = checkedIds.mapNotNull { chipId ->
                    statusChips[chipId]
                }.toSet()
                
                if (newSelectedStatuses != selectedOpenStatuses) {
                    selectedOpenStatuses.clear()
                    selectedOpenStatuses.addAll(newSelectedStatuses)
                    
                    // 🔑 核心修复：传递Set副本，避免对象引用问题
                    viewModel.updateOpenStatuses(newSelectedStatuses)
                    notifyValueChanged(newSelectedStatuses)
                }
            }
        }
        
        // 设置展开/收起动画
        animationManager.setupChipGroupExpandCollapse(
            binding.statusRatingSection.openStatusChipGroup,
            binding.statusRatingSection.openStatusExpandButton,
            "open_status_expand"
        )
    }
    
    /**
     * 设置评分ChipGroup
     */
    private fun setupRatingChipGroup() {
        binding.statusRatingSection.ratingChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            // 如果正在从FilterState更新UI，跳过这个回调
            if (isUpdatingFromState) {
                return@setOnCheckedStateChangeListener
            }
            
            if (isReady()) {
                val newSelectedRatings = checkedIds.mapNotNull { chipId ->
                    ratingChips[chipId]
                }.toSet()
                
                if (newSelectedRatings != selectedRatings) {
                    selectedRatings.clear()
                    selectedRatings.addAll(newSelectedRatings)
                    
                    // 🔑 核心修复：传递Set副本，避免对象引用问题
                    viewModel.updateRatings(newSelectedRatings)
                    notifyValueChanged(newSelectedRatings)
                }
            }
        }
        
        // 设置展开/收起动画
        animationManager.setupChipGroupExpandCollapse(
            binding.statusRatingSection.ratingChipGroup,
            binding.statusRatingSection.ratingExpandButton,
            "rating_expand"
        )
    }
    
    /**
     * 设置季节ChipGroup
     */
    private fun setupSeasonChipGroup() {
        // 初始时为空，等待ViewModel数据更新
        
        // 设置展开/收起动画
        animationManager.setupChipGroupExpandCollapse(
            binding.statusRatingSection.seasonChipGroup,
            binding.statusRatingSection.seasonExpandButton,
            "season_expand"
        )
    }
    
    /**
     * 设置标签ChipGroup
     */
    private fun setupTagsChipGroup() {
        // 初始时为空，等待ViewModel数据更新
        
        // 设置展开/收起动画
        animationManager.setupChipGroupExpandCollapse(
            binding.statusRatingSection.tagsChipGroup,
            binding.statusRatingSection.tagsExpandButton,
            "tags_expand"
        )
    }
    
    /**
     * 观察ViewModel数据变化
     */
    private fun observeViewModelData() {
        // 观察季节列表变化
        viewModel.availableSeasons.observe(lifecycleOwner) { seasons ->
            availableSeasons = seasons ?: emptyList()
            updateSeasonsChipGroup(seasons ?: emptyList())
        }
        
        // 观察标签列表变化
        viewModel.availableTags.observe(lifecycleOwner) { tags ->
            availableTags = tags ?: emptyList()
            updateTagsChipGroup(tags ?: emptyList())
        }
    }
    
    /**
     * 更新季节ChipGroup
     */
    private fun updateSeasonsChipGroup(seasons: List<String>) {
        // 保存当前滚动位置
        val currentScrollY = binding.contentScrollView.scrollY
        
        // 清除现有chips
        binding.statusRatingSection.seasonChipGroup.removeAllViews()
        
        // 添加新的chips
        seasons.forEach { season ->
            val layoutInflater = LayoutInflater.from(binding.root.context)
            val chip = layoutInflater.inflate(
                R.layout.item_suggestion_chip,
                binding.statusRatingSection.seasonChipGroup,
                false
            ) as Chip
            
            chip.apply {
                text = season
                isCheckable = true
                isChecked = selectedSeasons.contains(season)
                setOnCheckedChangeListener { _, isChecked ->
                    // 如果正在从FilterState更新UI，跳过这个回调
                    if (isUpdatingFromState) {
                        return@setOnCheckedChangeListener
                    }
                    
                    if (isReady()) {
                        val currentSeasons = selectedSeasons.toMutableSet()
                        if (isChecked) {
                            currentSeasons.add(season)
                        } else {
                            currentSeasons.remove(season)
                        }
                        
                        if (currentSeasons != selectedSeasons) {
                            selectedSeasons.clear()
                            selectedSeasons.addAll(currentSeasons)
                            
                            // 🔑 核心修复：传递Set副本，避免对象引用问题
                            viewModel.updateSeasons(currentSeasons)
                            notifyValueChanged(currentSeasons)
                        }
                    }
                }
            }
            
            binding.statusRatingSection.seasonChipGroup.addView(chip)
        }
        
        // 重新设置展开/收起功能
        animationManager.setupChipGroupExpandCollapse(
            binding.statusRatingSection.seasonChipGroup,
            binding.statusRatingSection.seasonExpandButton,
            "season_expand"
        )
        
        // 恢复滚动位置
        binding.contentScrollView.post {
            binding.contentScrollView.scrollTo(0, currentScrollY)
        }
    }
    
    /**
     * 更新标签ChipGroup
     */
    private fun updateTagsChipGroup(tags: List<String>) {
        // 保存当前滚动位置
        val currentScrollY = binding.contentScrollView.scrollY
        
        // 清除现有chips
        binding.statusRatingSection.tagsChipGroup.removeAllViews()
        
        // 添加新的chips
        tags.forEach { tag ->
            val layoutInflater = LayoutInflater.from(binding.root.context)
            val chip = layoutInflater.inflate(
                R.layout.item_suggestion_chip,
                binding.statusRatingSection.tagsChipGroup,
                false
            ) as Chip
            
            chip.apply {
                text = tag
                isCheckable = true
                isChecked = selectedTags.contains(tag)
                setOnCheckedChangeListener { _, isChecked ->
                    // 如果正在从FilterState更新UI，跳过这个回调
                    if (isUpdatingFromState) {
                        return@setOnCheckedChangeListener
                    }
                    
                    if (isReady()) {
                        val currentTags = selectedTags.toMutableSet()
                        if (isChecked) {
                            currentTags.add(tag)
                        } else {
                            currentTags.remove(tag)
                        }
                        
                        if (currentTags != selectedTags) {
                            selectedTags.clear()
                            selectedTags.addAll(currentTags)
                            
                            // 🔑 核心修复：传递Set副本，避免对象引用问题
                            viewModel.updateTags(currentTags)
                            notifyValueChanged(currentTags)
                        }
                    }
                }
            }
            
            binding.statusRatingSection.tagsChipGroup.addView(chip)
        }
        
        // 重新设置展开/收起功能
        animationManager.setupChipGroupExpandCollapse(
            binding.statusRatingSection.tagsChipGroup,
            binding.statusRatingSection.tagsExpandButton,
            "tags_expand"
        )
        
        // 恢复滚动位置
        binding.contentScrollView.post {
            binding.contentScrollView.scrollTo(0, currentScrollY)
        }
    }
    
    override fun updateFromState(filterState: FilterState) {
        isUpdatingFromState = true
        try {
            // 更新开封状态
            updateOpenStatusesSelection(filterState.openStatuses)
        
            // 更新评分选择
            updateRatingsSelection(filterState.ratings)
            
            // 更新季节选择
            updateSeasonsSelection(filterState.seasons)
            
            // 更新标签选择
            updateTagsSelection(filterState.tags)
        } finally {
            isUpdatingFromState = false
        }
    }
    
    /**
     * 更新开封状态选择
     */
    private fun updateOpenStatusesSelection(statuses: Set<Boolean>) {
        if (statuses != selectedOpenStatuses) {
            selectedOpenStatuses.clear()
            selectedOpenStatuses.addAll(statuses)
            
            // 更新UI状态
            binding.statusRatingSection.chipUnopened.isChecked = statuses.contains(false)
            binding.statusRatingSection.chipOpened.isChecked = statuses.contains(true)
        }
    }
    
    /**
     * 更新评分选择
     */
    private fun updateRatingsSelection(ratings: Set<Float>) {
        if (ratings != selectedRatings) {
            selectedRatings.clear()
            selectedRatings.addAll(ratings)
            
            // 更新UI状态
            binding.statusRatingSection.chipRating1.isChecked = ratings.contains(1f)
            binding.statusRatingSection.chipRating2.isChecked = ratings.contains(2f)
            binding.statusRatingSection.chipRating3.isChecked = ratings.contains(3f)
            binding.statusRatingSection.chipRating4.isChecked = ratings.contains(4f)
            binding.statusRatingSection.chipRating5.isChecked = ratings.contains(5f)
        }
    }
    
    /**
     * 更新季节选择
     */
    private fun updateSeasonsSelection(seasons: Set<String>) {
        if (seasons != selectedSeasons) {
            selectedSeasons.clear()
            selectedSeasons.addAll(seasons)
            
            // 更新ChipGroup选中状态
            for (i in 0 until binding.statusRatingSection.seasonChipGroup.childCount) {
                val chip = binding.statusRatingSection.seasonChipGroup.getChildAt(i) as? Chip
                chip?.let {
                    it.isChecked = seasons.contains(it.text.toString())
                }
            }
        }
    }
    
    /**
     * 更新标签选择
     */
    private fun updateTagsSelection(tags: Set<String>) {
        if (tags != selectedTags) {
            selectedTags.clear()
            selectedTags.addAll(tags)
            
            // 更新ChipGroup选中状态
            for (i in 0 until binding.statusRatingSection.tagsChipGroup.childCount) {
                val chip = binding.statusRatingSection.tagsChipGroup.getChildAt(i) as? Chip
                chip?.let {
                    it.isChecked = tags.contains(it.text.toString())
                }
            }
        }
    }
    
    override fun resetToDefault() {
        // 重置开封状态
        selectedOpenStatuses.clear()
        binding.statusRatingSection.openStatusChipGroup.clearCheck()
        
        // 重置评分
        selectedRatings.clear()
        binding.statusRatingSection.ratingChipGroup.clearCheck()
        
        // 重置季节
        selectedSeasons.clear()
        binding.statusRatingSection.seasonChipGroup.clearCheck()
        
        // 重置标签
        selectedTags.clear()
        binding.statusRatingSection.tagsChipGroup.clearCheck()
    }
    
    // MultiSelectFilterComponent implementation (返回所有选中的值的组合)
    override fun getSelectedValues(): Set<String> {
        val allSelected = mutableSetOf<String>()
        
        // 添加开封状态
        selectedOpenStatuses.forEach { status ->
            allSelected.add("开封状态:${if (status) "已开封" else "未开封"}")
        }
        
        // 添加评分
        selectedRatings.forEach { rating ->
            allSelected.add("评分:${rating}星")
        }
        
        // 添加季节
        selectedSeasons.forEach { season ->
            allSelected.add("季节:$season")
        }
        
        // 添加标签
        selectedTags.forEach { tag ->
            allSelected.add("标签:$tag")
        }
        
        return allSelected
    }
    
    override fun setSelectedValues(values: Set<String>) {
        // 这个方法比较复杂，因为需要解析不同类型的值
        // 暂时不实现，使用具体的设置方法
    }
    
    override fun clearSelection() {
        resetToDefault()
    }
    
    override fun getAllOptions(): List<String> {
        val allOptions = mutableListOf<String>()
        
        // 添加开封状态选项
        allOptions.addAll(listOf("未开封", "已开封"))
        
        // 添加评分选项
        allOptions.addAll(listOf("1星", "2星", "3星", "4星", "5星"))
        
        // 添加季节选项
        allOptions.addAll(availableSeasons)
        
        // 添加标签选项
        allOptions.addAll(availableTags)
        
        return allOptions
    }
    
    override fun updateOptions(options: List<String>) {
        // 由于包含多种类型，这个方法不适用
        // 使用具体的更新方法
    }
    
    /**
     * 获取状态评级筛选摘要
     */
    fun getStatusRatingSummary(): String {
        val parts = mutableListOf<String>()
        
        if (selectedOpenStatuses.isNotEmpty()) {
            val statusNames = selectedOpenStatuses.map { if (it) "已开封" else "未开封" }
            parts.add("开封状态: ${statusNames.joinToString(", ")}")
        }
        
        if (selectedRatings.isNotEmpty()) {
            val ratingNames = selectedRatings.map { "${it}星" }
            parts.add("评分: ${ratingNames.joinToString(", ")}")
        }
        
        if (selectedSeasons.isNotEmpty()) {
            parts.add("季节: ${selectedSeasons.joinToString(", ")}")
        }
        
        if (selectedTags.isNotEmpty()) {
            parts.add("标签: ${selectedTags.joinToString(", ")}")
        }
        
        return if (parts.isEmpty()) {
            "未选择状态评级筛选"
        } else {
            parts.joinToString(" | ")
        }
    }
    
    /**
     * 单独获取各个筛选类型的选中值
     */
    fun getSelectedOpenStatuses(): Set<Boolean> = selectedOpenStatuses.toSet()
    fun getSelectedRatings(): Set<Float> = selectedRatings.toSet()
    fun getSelectedSeasons(): Set<String> = selectedSeasons.toSet()
    fun getSelectedTags(): Set<String> = selectedTags.toSet()
    
    /**
     * 单独设置各个筛选类型的选中值
     */
    fun setSelectedOpenStatuses(statuses: Set<Boolean>) {
        selectedOpenStatuses.clear()
        selectedOpenStatuses.addAll(statuses)
        updateOpenStatusesSelection(statuses)
        // 🔑 核心修复：传递Set副本，避免对象引用问题
        viewModel.updateOpenStatuses(statuses.toSet())
    }
    
    fun setSelectedRatings(ratings: Set<Float>) {
        selectedRatings.clear()
        selectedRatings.addAll(ratings)
        updateRatingsSelection(ratings)
        // 🔑 核心修复：传递Set副本，避免对象引用问题
        viewModel.updateRatings(ratings.toSet())
    }
    
    fun setSelectedSeasons(seasons: Set<String>) {
        selectedSeasons.clear()
        selectedSeasons.addAll(seasons)
        updateSeasonsSelection(seasons)
        // 🔑 核心修复：传递Set副本，避免对象引用问题
        viewModel.updateSeasons(seasons.toSet())
    }
    
    fun setSelectedTags(tags: Set<String>) {
        selectedTags.clear()
        selectedTags.addAll(tags)
        updateTagsSelection(tags)
        // 🔑 核心修复：传递Set副本，避免对象引用问题
        viewModel.updateTags(tags.toSet())
    }
    

    override fun cleanup() {
        super.cleanup()
        
        // 清理ChipGroup监听器
        binding.statusRatingSection.openStatusChipGroup.setOnCheckedStateChangeListener(null)
        binding.statusRatingSection.ratingChipGroup.setOnCheckedStateChangeListener(null)
        
        // 清理动态创建的chips的监听器
        clearChipListeners(binding.statusRatingSection.seasonChipGroup)
        clearChipListeners(binding.statusRatingSection.tagsChipGroup)
        
        // 清空选中状态
        selectedOpenStatuses.clear()
        selectedRatings.clear()
        selectedSeasons.clear()
        selectedTags.clear()
    }
    
    private fun clearChipListeners(chipGroup: com.google.android.material.chip.ChipGroup) {
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            chip?.setOnCheckedChangeListener(null)
        }
    }
}
