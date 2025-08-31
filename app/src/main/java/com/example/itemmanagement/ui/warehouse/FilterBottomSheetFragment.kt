package com.example.itemmanagement.ui.warehouse

import com.example.itemmanagement.ui.utils.showMaterial3DatePicker
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentFilterBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FilterBottomSheetFragment : BottomSheetDialogFragment() {
    
    private var _binding: FragmentFilterBottomSheetBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: WarehouseViewModel by activityViewModels {
        WarehouseViewModelFactory(
            (requireActivity().application as ItemManagementApplication).repository
        )
    }
    
    private lateinit var navigationAdapter: FilterNavigationAdapter
    private val filterCategories = FilterCategory.values().toList()
    
    // 日期相关
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    // 滚动位置保存
    private var savedScrollPosition = 0
    private var isUpdatingUI = false
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilterBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onStart() {
        super.onStart()
        
        // 简化ScrollView初始化
        binding.contentScrollView.post {
            binding.contentScrollView.requestFocus()
            
            // 强制启用滚动功能
            binding.contentScrollView.isVerticalScrollBarEnabled = true
            binding.contentScrollView.isScrollbarFadingEnabled = false
            
            // 确保内容高度足够触发滚动
            val contentHeight = binding.contentScrollView.getChildAt(0)?.height ?: 0
            val scrollViewHeight = binding.contentScrollView.height
            
            // 如果内容高度不够，强制设置最小高度
            if (contentHeight <= scrollViewHeight) {
                binding.contentScrollView.getChildAt(0)?.minimumHeight = scrollViewHeight + 100
            }
        }
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 确保ScrollView可以正常滚动 - 简化配置
        binding.contentScrollView.isFocusable = true
        binding.contentScrollView.isFocusableInTouchMode = true
        binding.contentScrollView.isScrollContainer = true
        
        // 移除可能导致冲突的监听器
        binding.contentScrollView.setOnTouchListener(null)
        
        setupNavigationRecyclerView()
        setupFilters()
        setupButtons()
        observeFilterState()
        setupDropdowns()
        setupDateInputs()
        setupScrollSyncWithNavigation()
        
        // 配置BottomSheet行为以确保滚动正常，但保持半屏模式
        dialog?.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as com.google.android.material.bottomsheet.BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(it)
                // 完全禁用拖拽，专注于内容滚动
                behavior.isDraggable = false
                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                
                // 设置固定高度
                val displayMetrics = resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels
                behavior.peekHeight = (screenHeight * 0.6f).toInt()
                
                // 完全移除BottomSheet的触摸监听器，让内容自由滚动
                bottomSheet.setOnTouchListener(null)
            }
        }
    }
    
    /**
     * 设置滚动与导航同步
     */
    private fun setupScrollSyncWithNavigation() {
        binding.contentScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            // 只在非UI更新期间才更新导航高亮
            if (!isUpdatingUI) {
                updateNavigationHighlight(scrollY)
            }
        }
    }
    
    /**
     * 根据滚动位置更新导航高亮
     */
    private fun updateNavigationHighlight(scrollY: Int) {
        val sections = listOf(
            FilterCategory.CORE to binding.coreSection,
            FilterCategory.LOCATION to binding.locationSection,
            FilterCategory.STATUS_RATING to binding.statusRatingSection,
            FilterCategory.VALUE_RANGE to binding.valueRangeSection,
            FilterCategory.DATE to binding.dateSection
        )
        
        // 获取ScrollView的高度用于计算可见区域
        val scrollViewHeight = binding.contentScrollView.height
        val visibleCenter = scrollY + scrollViewHeight / 2
        
        // 找到距离可见区域中心最近的区域
        var currentSection = FilterCategory.CORE
        var minDistance = Int.MAX_VALUE
        
        for ((category, view) in sections) {
            val viewTop = view.top
            val viewBottom = view.bottom
            val viewCenter = (viewTop + viewBottom) / 2
            
            val distance = kotlin.math.abs(visibleCenter - viewCenter)
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
    
    private fun setupNavigationRecyclerView() {
        navigationAdapter = FilterNavigationAdapter(filterCategories) { category, position ->
            scrollToSection(category)
        }
        
        binding.navigationRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = navigationAdapter
        }
    }
    
    private fun scrollToSection(category: FilterCategory) {
        val targetView = when (category) {
            FilterCategory.CORE -> binding.coreSection
            FilterCategory.LOCATION -> binding.locationSection
            FilterCategory.STATUS_RATING -> binding.statusRatingSection
            FilterCategory.VALUE_RANGE -> binding.valueRangeSection
            FilterCategory.DATE -> binding.dateSection
        }
        
        binding.contentScrollView.post {
            binding.contentScrollView.smoothScrollTo(0, targetView.top)
        }
    }
    
    private fun setupFilters() {
        setupStatusFilters()
        setupRatingFilters()
        setupSeasonFilters()
        setupTagsFilters()
    }
    
    private fun setupStatusFilters() {
        val statusChips = mapOf(
            R.id.chipUnopened to false,
            R.id.chipOpened to true
        )
        
        binding.openStatusChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            // 清除焦点，防止滚动到有焦点的控件
            clearAllFocus()
            
            val selectedStatuses = checkedIds.mapNotNull { chipId ->
                statusChips[chipId]
            }.toSet()
            
            viewModel.updateOpenStatuses(selectedStatuses)
        }
    }
    
    private fun setupRatingFilters() {
        val ratingChips = mapOf(
            R.id.chipRating1 to 1f,
            R.id.chipRating2 to 2f,
            R.id.chipRating3 to 3f,
            R.id.chipRating4 to 4f,
            R.id.chipRating5 to 5f
        )
        
        binding.ratingChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            // 清除焦点，防止滚动到有焦点的控件
            clearAllFocus()
            
            val selectedRatings = checkedIds.mapNotNull { chipId ->
                ratingChips[chipId]
            }.toSet()
            
            viewModel.updateRatings(selectedRatings)
        }
    }
    
    private fun setupSeasonFilters() {
        // 观察可用季节列表
        viewModel.availableSeasons.observe(viewLifecycleOwner) { availableSeasons ->
            updateSeasonsChipGroup(availableSeasons)
        }
    }
    
    private fun updateSeasonsChipGroup(availableSeasons: List<String>) {
        // 保存当前滚动位置
        val currentScrollY = binding.contentScrollView.scrollY
        
        // 清除焦点，防止后续UI更新时滚动到有焦点的控件
        clearAllFocus()
        
        binding.seasonChipGroup.removeAllViews()
        val currentSelectedSeasons = viewModel.filterState.value.seasons
        
        availableSeasons.forEach { season ->
            val chip = Chip(requireContext()).apply {
                text = season
                isCheckable = true
                isChecked = currentSelectedSeasons.contains(season)
                // 设置为Choice样式的外观，与开封状态、评分、标签保持一致的蓝色选中效果
                chipBackgroundColor = resources.getColorStateList(com.google.android.material.R.color.mtrl_choice_chip_background_color, requireContext().theme)
                setTextColor(resources.getColorStateList(com.google.android.material.R.color.mtrl_choice_chip_text_color, requireContext().theme))
                isChipIconVisible = false
                // 禁用选中图标（对钩），使用背景色变化表示选中状态
                isCheckedIconVisible = false
                setOnCheckedChangeListener { _, isChecked ->
                    // 保存滚动位置，防止季节状态改变时跳转
                    val scrollY = binding.contentScrollView.scrollY
                    
                    // 清除焦点，防止UI更新时跳转到有焦点的控件
                    clearAllFocus()
                    
                    val currentSeasons = viewModel.filterState.value.seasons.toMutableSet()
                    if (isChecked) {
                        currentSeasons.add(season)
                    } else {
                        currentSeasons.remove(season)
                    }
                    viewModel.updateSeasons(currentSeasons)
                    
                    // 延迟恢复滚动位置
                    binding.contentScrollView.post {
                        binding.contentScrollView.scrollTo(0, scrollY)
                    }
                }
            }
            binding.seasonChipGroup.addView(chip)
        }
        
        // 延迟恢复滚动位置
        binding.contentScrollView.post {
            binding.contentScrollView.scrollTo(0, currentScrollY)
        }
    }
    
    private fun setupTagsFilters() {
        // 观察可用标签列表
        viewModel.availableTags.observe(viewLifecycleOwner) { availableTags ->
            updateTagsChipGroup(availableTags)
        }
    }
    
    private fun updateTagsChipGroup(availableTags: List<String>) {
        // 保存当前滚动位置
        val currentScrollY = binding.contentScrollView.scrollY
        
        // 清除焦点，防止后续UI更新时滚动到有焦点的控件
        clearAllFocus()
        
        binding.tagsChipGroup.removeAllViews()
        val currentSelectedTags = viewModel.filterState.value.tags
        
        availableTags.forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text = tag
                isCheckable = true
                isChecked = currentSelectedTags.contains(tag)
                // 设置为Choice样式的外观，与开封状态、评分、季节保持一致的蓝色选中效果
                chipBackgroundColor = resources.getColorStateList(com.google.android.material.R.color.mtrl_choice_chip_background_color, requireContext().theme)
                setTextColor(resources.getColorStateList(com.google.android.material.R.color.mtrl_choice_chip_text_color, requireContext().theme))
                isChipIconVisible = false
                // 禁用选中图标（对钩），使用背景色变化表示选中状态
                isCheckedIconVisible = false
                setOnCheckedChangeListener { _, isChecked ->
                    // 保存滚动位置，防止标签状态改变时跳转
                    val scrollY = binding.contentScrollView.scrollY
                    
                    // 清除焦点，防止UI更新时跳转到有焦点的控件
                    clearAllFocus()
                    
                    val currentTags = viewModel.filterState.value.tags.toMutableSet()
                    if (isChecked) {
                        currentTags.add(tag)
                    } else {
                        currentTags.remove(tag)
                    }
                    viewModel.updateTags(currentTags)
                    
                    // 延迟恢复滚动位置
                    binding.contentScrollView.post {
                        binding.contentScrollView.scrollTo(0, scrollY)
                    }
                }
            }
            binding.tagsChipGroup.addView(chip)
        }
        
        // 恢复滚动位置
        binding.contentScrollView.post {
            binding.contentScrollView.scrollTo(0, currentScrollY)
        }
    }
    

    
    private fun setupDateInputs() {
        // 过期日期范围
        binding.expirationStartDateInput.setOnClickListener {
            showDatePicker { date ->
                binding.expirationStartDateInput.setText(dateFormat.format(Date(date)))
                viewModel.updateExpirationDateRange(date, viewModel.filterState.value.expirationEndDate)
            }
        }
        
        binding.expirationEndDateInput.setOnClickListener {
            showDatePicker { date ->
                binding.expirationEndDateInput.setText(dateFormat.format(Date(date)))
                viewModel.updateExpirationDateRange(viewModel.filterState.value.expirationStartDate, date)
            }
        }
        
        // 添加日期范围
        binding.creationStartDateInput.setOnClickListener {
            showDatePicker { date ->
                binding.creationStartDateInput.setText(dateFormat.format(Date(date)))
                viewModel.updateCreationDateRange(date, viewModel.filterState.value.creationEndDate)
            }
        }
        
        binding.creationEndDateInput.setOnClickListener {
            showDatePicker { date ->
                binding.creationEndDateInput.setText(dateFormat.format(Date(date)))
                viewModel.updateCreationDateRange(viewModel.filterState.value.creationStartDate, date)
            }
        }
        
        // 购买日期范围
        binding.purchaseStartDateInput.setOnClickListener {
            showDatePicker { date ->
                binding.purchaseStartDateInput.setText(dateFormat.format(Date(date)))
                viewModel.updatePurchaseDateRange(date, viewModel.filterState.value.purchaseEndDate)
            }
        }
        
        binding.purchaseEndDateInput.setOnClickListener {
            showDatePicker { date ->
                binding.purchaseEndDateInput.setText(dateFormat.format(Date(date)))
                viewModel.updatePurchaseDateRange(viewModel.filterState.value.purchaseStartDate, date)
            }
        }
        
        // 生产日期范围
        binding.productionStartDateInput.setOnClickListener {
            showDatePicker { date ->
                binding.productionStartDateInput.setText(dateFormat.format(Date(date)))
                viewModel.updateProductionDateRange(date, viewModel.filterState.value.productionEndDate)
            }
        }
        
        binding.productionEndDateInput.setOnClickListener {
            showDatePicker { date ->
                binding.productionEndDateInput.setText(dateFormat.format(Date(date)))
                viewModel.updateProductionDateRange(viewModel.filterState.value.productionStartDate, date)
            }
        }
    }
    
    /**
     * 显示Material 3日期选择器
     */
    private fun showDatePicker(onDateSelected: (Long) -> Unit) {
        showMaterial3DatePicker(
            title = "选择日期"
        ) { selectedDate ->
            onDateSelected(selectedDate.time)
        }
    }
    
    private fun setupButtons() {
        binding.resetButton.setOnClickListener {
            resetAllFilters()
        }
        
        binding.applyButton.setOnClickListener {
            dismiss()
        }
    }
    
    private fun resetAllFilters() {
        // 重置所有筛选条件
        viewModel.resetFilters()
        
        // 重置UI
        resetUIComponents()
    }
    
    private fun resetUIComponents() {
        // 重置下拉框
        binding.categoryDropdown.setText("", false)
        binding.subCategoryDropdown.setText("", false)
        binding.brandDropdown.setText("", false)
        binding.locationAreaDropdown.setText("", false)
        binding.containerDropdown.setText("", false)
        binding.sublocationDropdown.setText("", false)
        
        // 重置输入框
        binding.minQuantityInput.setText("")
        binding.maxQuantityInput.setText("")
        binding.minPriceInput.setText("")
        binding.maxPriceInput.setText("")
        
        // 重置日期输入框
        binding.expirationStartDateInput.setText("")
        binding.expirationEndDateInput.setText("")
        binding.creationStartDateInput.setText("")
        binding.creationEndDateInput.setText("")
        binding.purchaseStartDateInput.setText("")
        binding.purchaseEndDateInput.setText("")
        binding.productionStartDateInput.setText("")
        binding.productionEndDateInput.setText("")
        
        // 重置芯片组
        binding.openStatusChipGroup.clearCheck()
        binding.ratingChipGroup.clearCheck()
        binding.seasonChipGroup.clearCheck()
        binding.tagsChipGroup.clearCheck()
    }
    
    private fun observeFilterState() {
        lifecycleScope.launch {
            viewModel.filterState.collectLatest { filterState ->
                // 保存当前滚动位置
                savedScrollPosition = binding.contentScrollView.scrollY
                
                // 标记正在更新UI
                isUpdatingUI = true
                
                updateUIFromFilterState(filterState)
                
                // 恢复滚动位置
                binding.contentScrollView.post {
                    binding.contentScrollView.scrollTo(0, savedScrollPosition)
                    isUpdatingUI = false
                }
            }
        }
    }
    
    /**
     * 清除所有输入框的焦点
     */
    private fun clearAllFocus() {
        // 清除下拉框焦点
        binding.categoryDropdown.clearFocus()
        binding.subCategoryDropdown.clearFocus()
        binding.brandDropdown.clearFocus()
        binding.locationAreaDropdown.clearFocus()
        binding.containerDropdown.clearFocus()
        binding.sublocationDropdown.clearFocus()
        
        // 清除数值输入框焦点
        binding.minQuantityInput.clearFocus()
        binding.maxQuantityInput.clearFocus()
        binding.minPriceInput.clearFocus()
        binding.maxPriceInput.clearFocus()
        
        // 清除日期输入框焦点
        binding.expirationStartDateInput.clearFocus()
        binding.expirationEndDateInput.clearFocus()
        binding.creationStartDateInput.clearFocus()
        binding.creationEndDateInput.clearFocus()
        binding.purchaseStartDateInput.clearFocus()
        binding.purchaseEndDateInput.clearFocus()
        binding.productionStartDateInput.clearFocus()
        binding.productionEndDateInput.clearFocus()
    }
    
    private fun updateUIFromFilterState(filterState: FilterState) {
        // 暂时禁用监听器，防止无限循环
        binding.openStatusChipGroup.setOnCheckedStateChangeListener(null)
        binding.ratingChipGroup.setOnCheckedStateChangeListener(null)
        binding.seasonChipGroup.setOnCheckedStateChangeListener(null)
        
        // 更新下拉框
        binding.categoryDropdown.setText(filterState.category, false)
        binding.subCategoryDropdown.setText(filterState.subCategory, false)
        binding.brandDropdown.setText(filterState.brand, false)
        binding.locationAreaDropdown.setText(filterState.locationArea, false)
        binding.containerDropdown.setText(filterState.container, false)
        binding.sublocationDropdown.setText(filterState.sublocation, false)
        
        // 更新数值输入框
        binding.minQuantityInput.setText(filterState.minQuantity?.toString() ?: "")
        binding.maxQuantityInput.setText(filterState.maxQuantity?.toString() ?: "")
        binding.minPriceInput.setText(filterState.minPrice?.toString() ?: "")
        binding.maxPriceInput.setText(filterState.maxPrice?.toString() ?: "")
        
        // 更新日期输入框
        binding.expirationStartDateInput.setText(
            filterState.expirationStartDate?.let { dateFormat.format(Date(it)) } ?: ""
        )
        binding.expirationEndDateInput.setText(
            filterState.expirationEndDate?.let { dateFormat.format(Date(it)) } ?: ""
        )
        binding.creationStartDateInput.setText(
            filterState.creationStartDate?.let { dateFormat.format(Date(it)) } ?: ""
        )
        binding.creationEndDateInput.setText(
            filterState.creationEndDate?.let { dateFormat.format(Date(it)) } ?: ""
        )
        binding.purchaseStartDateInput.setText(
            filterState.purchaseStartDate?.let { dateFormat.format(Date(it)) } ?: ""
        )
        binding.purchaseEndDateInput.setText(
            filterState.purchaseEndDate?.let { dateFormat.format(Date(it)) } ?: ""
        )
        binding.productionStartDateInput.setText(
            filterState.productionStartDate?.let { dateFormat.format(Date(it)) } ?: ""
        )
        binding.productionEndDateInput.setText(
            filterState.productionEndDate?.let { dateFormat.format(Date(it)) } ?: ""
        )
        
        // 更新开封状态（多选）- 直接设置每个chip状态，不使用clearCheck()
        binding.chipUnopened.isChecked = filterState.openStatuses.contains(false)
        binding.chipOpened.isChecked = filterState.openStatuses.contains(true)
        
        // 更新评分（多选）- 直接设置每个chip状态，不使用clearCheck()
        binding.chipRating1.isChecked = filterState.ratings.contains(1f)
        binding.chipRating2.isChecked = filterState.ratings.contains(2f)
        binding.chipRating3.isChecked = filterState.ratings.contains(3f)
        binding.chipRating4.isChecked = filterState.ratings.contains(4f)
        binding.chipRating5.isChecked = filterState.ratings.contains(5f)
        
        // 更新季节选择状态（动态创建的季节chips）
        updateSeasonsSelectionState(filterState.seasons)
        
        // 更新标签选择状态
        updateTagsSelectionState(filterState.tags)
        
        // 重新启用监听器
        setupStatusFilters()
        setupRatingFilters()
        // 季节字段的监听器在动态创建时已设置，无需重新设置
    }
    
    private fun updateTagsSelectionState(selectedTags: Set<String>) {
        for (i in 0 until binding.tagsChipGroup.childCount) {
            val chip = binding.tagsChipGroup.getChildAt(i) as? Chip
            chip?.let {
                it.isChecked = selectedTags.contains(it.text.toString())
            }
        }
    }
    
    private fun updateSeasonsSelectionState(selectedSeasons: Set<String>) {
        for (i in 0 until binding.seasonChipGroup.childCount) {
            val chip = binding.seasonChipGroup.getChildAt(i) as? Chip
            chip?.let {
                it.isChecked = selectedSeasons.contains(it.text.toString())
            }
        }
    }
    
    private fun setupDropdowns() {
        // 观察并设置分类列表
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, categories)
            (binding.categoryDropdown as? AutoCompleteTextView)?.setAdapter(adapter)
        }
        
        // 设置分类选择监听
        (binding.categoryDropdown as? AutoCompleteTextView)?.setOnItemClickListener { _, _, position, _ ->
            val selectedCategory = (binding.categoryDropdown as? AutoCompleteTextView)?.adapter?.getItem(position) as String
            viewModel.setCategory(selectedCategory)
        }
        
        // 观察并设置子分类列表
        viewModel.subCategories.observe(viewLifecycleOwner) { subCategories ->
            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, subCategories)
            (binding.subCategoryDropdown as? AutoCompleteTextView)?.setAdapter(adapter)
        }
        
        // 设置子分类选择监听
        (binding.subCategoryDropdown as? AutoCompleteTextView)?.setOnItemClickListener { _, _, position, _ ->
            val selectedSubCategory = (binding.subCategoryDropdown as? AutoCompleteTextView)?.adapter?.getItem(position) as String
            viewModel.setSubCategory(selectedSubCategory)
        }
        
        // 观察并设置品牌列表
        viewModel.brands.observe(viewLifecycleOwner) { brands ->
            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, brands)
            (binding.brandDropdown as? AutoCompleteTextView)?.setAdapter(adapter)
        }
        
        // 设置品牌选择监听
        (binding.brandDropdown as? AutoCompleteTextView)?.setOnItemClickListener { _, _, position, _ ->
            val selectedBrand = (binding.brandDropdown as? AutoCompleteTextView)?.adapter?.getItem(position) as String
            viewModel.setBrand(selectedBrand)
        }
        
        // 设置位置相关下拉框
        setupLocationDropdowns()
        
        // 设置数值输入框监听
        setupValueInputListeners()
    }
    
    private fun setupLocationDropdowns() {
        // 位置区域
        viewModel.locationAreas.observe(viewLifecycleOwner) { areas ->
            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, areas)
            (binding.locationAreaDropdown as? AutoCompleteTextView)?.setAdapter(adapter)
        }
        
        (binding.locationAreaDropdown as? AutoCompleteTextView)?.setOnItemClickListener { _, _, position, _ ->
            val selectedArea = (binding.locationAreaDropdown as? AutoCompleteTextView)?.adapter?.getItem(position) as String
            viewModel.setLocationArea(selectedArea)
        }
        
        // 容器
        viewModel.containers.observe(viewLifecycleOwner) { containers ->
            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, containers)
            (binding.containerDropdown as? AutoCompleteTextView)?.setAdapter(adapter)
        }
        
        (binding.containerDropdown as? AutoCompleteTextView)?.setOnItemClickListener { _, _, position, _ ->
            val selectedContainer = (binding.containerDropdown as? AutoCompleteTextView)?.adapter?.getItem(position) as String
            viewModel.setContainer(selectedContainer)
        }
        
        // 子位置
        viewModel.sublocations.observe(viewLifecycleOwner) { sublocations ->
            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, sublocations)
            (binding.sublocationDropdown as? AutoCompleteTextView)?.setAdapter(adapter)
        }
        
        (binding.sublocationDropdown as? AutoCompleteTextView)?.setOnItemClickListener { _, _, position, _ ->
            val selectedSublocation = (binding.sublocationDropdown as? AutoCompleteTextView)?.adapter?.getItem(position) as String
            viewModel.setSublocation(selectedSublocation)
        }
    }
    
    private fun setupValueInputListeners() {
        // 数量输入框失去焦点时更新
        binding.minQuantityInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val minQuantity = binding.minQuantityInput.text.toString().toIntOrNull()
                viewModel.updateQuantityRange(minQuantity, viewModel.filterState.value.maxQuantity)
            }
        }
        
        binding.maxQuantityInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val maxQuantity = binding.maxQuantityInput.text.toString().toIntOrNull()
                viewModel.updateQuantityRange(viewModel.filterState.value.minQuantity, maxQuantity)
            }
        }
        
        // 价格输入框失去焦点时更新
        binding.minPriceInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val minPrice = binding.minPriceInput.text.toString().toDoubleOrNull()
                viewModel.updatePriceRange(minPrice, viewModel.filterState.value.maxPrice)
            }
        }
        
        binding.maxPriceInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val maxPrice = binding.maxPriceInput.text.toString().toDoubleOrNull()
                viewModel.updatePriceRange(viewModel.filterState.value.minPrice, maxPrice)
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 