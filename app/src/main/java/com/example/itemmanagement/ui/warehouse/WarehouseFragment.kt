package com.example.itemmanagement.ui.warehouse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.ui.utils.Material3Feedback
import com.example.itemmanagement.adapter.WarehouseItemAdapter
import com.example.itemmanagement.databinding.FragmentWarehouseBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController
import com.example.itemmanagement.ui.animation.SearchBoxAnimator
import com.example.itemmanagement.ui.warehouse.FilterBottomSheetFragmentV2

class WarehouseFragment : Fragment() {

    private var _binding: FragmentWarehouseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WarehouseViewModel by activityViewModels {
        WarehouseViewModelFactory(
            (requireActivity().application as ItemManagementApplication).repository
        )
    }

    private lateinit var adapter: WarehouseItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWarehouseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        android.util.Log.d("WarehouseFragment", "🔧 onViewCreated被调用")
        
        // 🔧 清理残留的FilterBottomSheet，防止自动弹出
        android.util.Log.d("WarehouseFragment", "🧹 检查并清理残留的FilterBottomSheet")
        val existingFilterSheet = childFragmentManager.findFragmentByTag("FilterBottomSheetFragmentV2")
        if (existingFilterSheet is FilterBottomSheetFragmentV2) {
            android.util.Log.d("WarehouseFragment", "🗑️ 发现残留的FilterBottomSheet，正在移除...")
            existingFilterSheet.dismissAllowingStateLoss()
        }
        
        setupRecyclerView()
        setupSortBar()
        setupFilterButton()
        setupSearchView()
        setupEmptyState()
        observeItems()
        observeDeleteResult()
        observeFilterState()
        
        android.util.Log.d("WarehouseFragment", "✅ onViewCreated完成")
    }

    private fun setupRecyclerView() {
        // 初始化适配器，传入所需的回调函数
        adapter = WarehouseItemAdapter(
            onItemClick = { itemId ->
                // 导航到详情页面
                android.util.Log.d("WarehouseFragment", "🎯 点击物品，itemId: $itemId")
                val bundle = androidx.core.os.bundleOf("itemId" to itemId)
                android.util.Log.d("WarehouseFragment", "🎯 准备导航到详情页，bundle: $bundle")
                findNavController().navigate(R.id.navigation_item_detail, bundle)
            },
            onEdit = { itemId ->
                // 导航到编辑页面（使用新架构）
                val bundle = androidx.core.os.bundleOf("itemId" to itemId)
                findNavController().navigate(R.id.action_navigation_warehouse_to_editItemFragment, bundle)
            },
            onDelete = { itemId ->
                // 显示删除确认对话框
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("确认删除")
                    .setMessage("确定要删除这个物品吗？")
                    .setPositiveButton("删除") { _, _ ->
                        viewModel.deleteItem(itemId)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@WarehouseFragment.adapter
        }
    }
    
    private fun setupSortBar() {
        // 设置排序按钮点击事件
        binding.sortComprehensive.setOnClickListener {
            setSortOption(it as TextView, SortOption.COMPREHENSIVE, "综合")
        }
        
        binding.sortQuantity.setOnClickListener {
            setSortOption(it as TextView, SortOption.QUANTITY, "数量")
        }
        
        binding.sortPrice.setOnClickListener {
            setSortOption(it as TextView, SortOption.PRICE, "单价")
        }
        
        binding.sortRating.setOnClickListener {
            setSortOption(it as TextView, SortOption.RATING, "评分")
        }
        
        binding.sortShelfLife.setOnClickListener {
            setSortOption(it as TextView, SortOption.REMAINING_SHELF_LIFE, "剩余保质期")
        }
        
        binding.sortAddTime.setOnClickListener {
            setSortOption(it as TextView, SortOption.UPDATE_TIME, "添加时间")
        }
    }
    
    private fun setSortOption(textView: TextView, sortOption: SortOption, displayName: String) {
        updateSortButtonState(textView)
        
        // 如果点击的是当前已选择的排序选项，切换排序方向
        if (viewModel.filterState.value.sortOption == sortOption) {
            val newDirection = if (viewModel.filterState.value.sortDirection == SortDirection.ASC) 
                SortDirection.DESC else SortDirection.ASC
            viewModel.setSortDirection(newDirection)
            
            // 更新显示的排序方向
            textView.text = "$displayName ${if (newDirection == SortDirection.ASC) "↑" else "↓"}"
        } else {
            // 如果是新的排序选项，设置默认排序方向
            viewModel.setSortOption(sortOption)
            val defaultDirection = when (sortOption) {
                SortOption.COMPREHENSIVE -> SortDirection.DESC // 综合排序默认降序
                SortOption.QUANTITY -> SortDirection.DESC // 数量默认降序
                SortOption.PRICE -> SortDirection.DESC // 单价默认降序
                SortOption.RATING -> SortDirection.DESC // 评分默认降序
                SortOption.REMAINING_SHELF_LIFE -> SortDirection.ASC // 剩余保质期默认升序（快过期的在前）
                SortOption.UPDATE_TIME -> SortDirection.DESC // 添加时间默认降序（新添加的在前）
            }
            viewModel.setSortDirection(defaultDirection)
            textView.text = "$displayName ${if (defaultDirection == SortDirection.ASC) "↑" else "↓"}"
        }
    }
    
    private fun updateSortButtonState(selectedButton: TextView) {
        // 重置所有按钮样式
        resetButtonStyle(binding.sortComprehensive)
        resetButtonStyle(binding.sortQuantity)
        resetButtonStyle(binding.sortPrice)
        resetButtonStyle(binding.sortRating)
        resetButtonStyle(binding.sortShelfLife)
        resetButtonStyle(binding.sortAddTime)
        
        // 设置选中按钮的状态 - 淘宝风格
        setSelectedButtonStyle(selectedButton)
        
        // 重置按钮文本（除了选中的按钮）
        if (selectedButton != binding.sortComprehensive) {
            binding.sortComprehensive.text = "综合"
        }
        if (selectedButton != binding.sortQuantity) {
            binding.sortQuantity.text = "数量"
        }
        if (selectedButton != binding.sortPrice) {
            binding.sortPrice.text = "单价"
        }
        if (selectedButton != binding.sortRating) {
            binding.sortRating.text = "评分"
        }
        if (selectedButton != binding.sortShelfLife) {
            binding.sortShelfLife.text = "剩余保质期"
        }
        if (selectedButton != binding.sortAddTime) {
            binding.sortAddTime.text = "添加时间"
        }
    }
    
    private fun resetButtonStyle(button: TextView) {
        // 重置为默认样式 - 使用简单安全的颜色获取方式
        val unselectedColor = com.google.android.material.R.attr.colorOnSurfaceVariant
        val typedArray = requireContext().obtainStyledAttributes(intArrayOf(unselectedColor))
        val color = typedArray.getColor(0, ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        typedArray.recycle()
        
        button.setTextColor(color)
        button.typeface = android.graphics.Typeface.DEFAULT
        
        // 添加缩放动画
        button.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(150)
            .start()
    }
    
    private fun setSelectedButtonStyle(button: TextView) {
        // 设置选中样式 - 使用简单安全的颜色获取方式
        val selectedAttr = com.google.android.material.R.attr.colorPrimary
        val typedArray = requireContext().obtainStyledAttributes(intArrayOf(selectedAttr))
        val color = typedArray.getColor(0, ContextCompat.getColor(requireContext(), com.google.android.material.R.color.design_default_color_primary))
        typedArray.recycle()
        
        button.setTextColor(color)
        button.typeface = android.graphics.Typeface.DEFAULT_BOLD
        
        // 添加淘宝风格的强调动画
        button.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(150)
            .start()
    }
    
    private fun setupFilterButton() {
        binding.filterButton.setOnClickListener {
            showFilterBottomSheet()
        }
    }
    
    private fun setupEmptyState() {
        // 设置空状态按钮点击事件
        binding.addFirstItemButton.setOnClickListener {
            // 使用触觉反馈
            view?.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            
            // 导航到添加物品页面
            findNavController().navigate(R.id.addItemFragment)
        }
    }
    
    private fun setupSearchView() {
        // 设置搜索框文本变化监听
        binding.searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchText = s?.toString() ?: ""
                viewModel.setSearchTerm(searchText)
                
                // 🎬 清除按钮动画控制
                if (searchText.isNotEmpty()) {
                    SearchBoxAnimator.animateClearButtonShow(binding.clearSearchIcon)
                } else {
                    SearchBoxAnimator.animateClearButtonHide(binding.clearSearchIcon)
                }
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        
        // 设置清除按钮点击事件
        binding.clearSearchIcon.setOnClickListener {
            binding.searchEditText.setText("")
            binding.searchEditText.clearFocus()
        }
        
        // 设置搜索容器点击事件，让整个区域都可以聚焦到输入框
        binding.searchContainer.setOnClickListener {
            binding.searchEditText.requestFocus()
            // 显示软键盘
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(binding.searchEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
        
        // 设置搜索图标点击事件
        binding.searchIcon.setOnClickListener {
            binding.searchEditText.requestFocus()
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(binding.searchEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
    }
    
    private fun showFilterBottomSheet() {
        val filterBottomSheet = FilterBottomSheetFragmentV2()
        filterBottomSheet.show(childFragmentManager, "FilterBottomSheetFragmentV2")
    }

    private fun observeItems() {
        android.util.Log.d("WarehouseFragment", "📱 WarehouseFragment开始观察仓库物品StateFlow")
        
        // 使用viewLifecycleOwner.lifecycleScope观察StateFlow，确保在View销毁时自动取消
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.warehouseItems.collectLatest { items ->
                android.util.Log.d("WarehouseFragment", "📱 WarehouseFragment收到StateFlow数据更新：${items.size}个物品")
                items.forEachIndexed { index, item ->
                    android.util.Log.d("WarehouseFragment", "  [$index] Fragment收到: name='${item.name}', locationArea='${item.locationArea}', tagsList='${item.tagsList}', rating=${item.rating}")
                }
                
                if (items.isEmpty()) {
                    android.util.Log.d("WarehouseFragment", "📱 显示空视图")
                    binding.emptyView.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    android.util.Log.d("WarehouseFragment", "📱 显示RecyclerView，准备提交数据到Adapter")
                    binding.emptyView.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    adapter.submitList(items) {
                        android.util.Log.d("WarehouseFragment", "✅ Adapter数据提交完成，滚动到顶部")
                        // 在列表更新完成后滚动到顶部
                        binding.recyclerView.scrollToPosition(0)
                    }
                }
            }
        }
    }
    
    private fun observeFilterState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filterState.collectLatest { filterState ->
                updateFilterChips(filterState)
                updateSortButtonsState(filterState)
            }
        }
    }
    
    private fun updateSortButtonsState(filterState: FilterState) {
        // 先重置所有按钮状态
        resetButtonStyle(binding.sortComprehensive)
        resetButtonStyle(binding.sortQuantity)
        resetButtonStyle(binding.sortPrice)
        resetButtonStyle(binding.sortRating)
        resetButtonStyle(binding.sortShelfLife)
        resetButtonStyle(binding.sortAddTime)
        
        // 设置当前选中的按钮和显示文本
        val selectedButton = when (filterState.sortOption) {
            SortOption.COMPREHENSIVE -> binding.sortComprehensive
            SortOption.QUANTITY -> binding.sortQuantity
            SortOption.PRICE -> binding.sortPrice
            SortOption.RATING -> binding.sortRating
            SortOption.REMAINING_SHELF_LIFE -> binding.sortShelfLife
            SortOption.UPDATE_TIME -> binding.sortAddTime
        }
        
        setSelectedButtonStyle(selectedButton)
        
        val directionSymbol = if (filterState.sortDirection == SortDirection.ASC) "↑" else "↓"
        when (filterState.sortOption) {
            SortOption.COMPREHENSIVE -> {
                binding.sortComprehensive.text = "综合 $directionSymbol"
                binding.sortQuantity.text = "数量"
                binding.sortPrice.text = "单价"
                binding.sortRating.text = "评分"
                binding.sortShelfLife.text = "剩余保质期"
                binding.sortAddTime.text = "添加时间"
            }
            SortOption.QUANTITY -> {
                binding.sortComprehensive.text = "综合"
                binding.sortQuantity.text = "数量 $directionSymbol"
                binding.sortPrice.text = "单价"
                binding.sortRating.text = "评分"
                binding.sortShelfLife.text = "剩余保质期"
                binding.sortAddTime.text = "添加时间"
            }
            SortOption.PRICE -> {
                binding.sortComprehensive.text = "综合"
                binding.sortQuantity.text = "数量"
                binding.sortPrice.text = "单价 $directionSymbol"
                binding.sortRating.text = "评分"
                binding.sortShelfLife.text = "剩余保质期"
                binding.sortAddTime.text = "添加时间"
            }
            SortOption.RATING -> {
                binding.sortComprehensive.text = "综合"
                binding.sortQuantity.text = "数量"
                binding.sortPrice.text = "单价"
                binding.sortRating.text = "评分 $directionSymbol"
                binding.sortShelfLife.text = "剩余保质期"
                binding.sortAddTime.text = "添加时间"
            }
            SortOption.REMAINING_SHELF_LIFE -> {
                binding.sortComprehensive.text = "综合"
                binding.sortQuantity.text = "数量"
                binding.sortPrice.text = "单价"
                binding.sortRating.text = "评分"
                binding.sortShelfLife.text = "剩余保质期 $directionSymbol"
                binding.sortAddTime.text = "添加时间"
            }
            SortOption.UPDATE_TIME -> {
                binding.sortComprehensive.text = "综合"
                binding.sortQuantity.text = "数量"
                binding.sortPrice.text = "单价"
                binding.sortRating.text = "评分"
                binding.sortShelfLife.text = "剩余保质期"
                binding.sortAddTime.text = "添加时间 $directionSymbol"
            }
        }
    }
    
    private fun updateFilterChips(filterState: FilterState) {
        android.util.Log.d("WarehouseFragment", "🔄 updateFilterChips被调用")
        android.util.Log.d("WarehouseFragment", "📊 当前filterChipGroup子视图数量: ${binding.filterChipGroup.childCount}")
        
        // 清空当前的筛选条件指示器
        binding.filterChipGroup.removeAllViews()
        android.util.Log.d("WarehouseFragment", "✅ 已清空filterChipGroup，当前子视图数量: ${binding.filterChipGroup.childCount}")
        
        // 检查是否有实际的筛选条件（排除排序和搜索相关的属性）
        val hasFilter = filterState.copy(
            searchTerm = "",
            sortOption = SortOption.COMPREHENSIVE,
            sortDirection = SortDirection.DESC
        ) != FilterState().copy(
            searchTerm = "",
            sortOption = SortOption.COMPREHENSIVE,
            sortDirection = SortDirection.DESC
        )
        
        if (hasFilter) {
            binding.filterChipContainer.visibility = View.VISIBLE
            binding.clearAllChip.visibility = View.VISIBLE
            binding.clearAllChip.setOnClickListener {
                // 保存当前的搜索词
                val currentSearchTerm = viewModel.filterState.value.searchTerm
                
                // 重置所有筛选和排序
                viewModel.resetFilter()
                
                // 如果有搜索词，恢复搜索词但不恢复其他筛选条件
                if (currentSearchTerm.isNotBlank()) {
                    viewModel.setSearchTerm(currentSearchTerm)
                }
            }
            
            // 不显示搜索词的chip，搜索框本身已经显示搜索内容
            
            // 添加分类筛选条件 - 支持多选显示
            android.util.Log.d("WarehouseFragment", "🎯 updateFilterChips检查分类:")
            android.util.Log.d("WarehouseFragment", "📊 categories: ${filterState.categories}")
            android.util.Log.d("WarehouseFragment", "📊 category: '${filterState.category}'")
            
            if (filterState.categories.isNotEmpty()) {
                val categoriesText = if (filterState.categories.size <= 3) {
                    filterState.categories.joinToString(",")
                } else {
                    "${filterState.categories.take(3).joinToString(",")}..."
                }
                android.util.Log.d("WarehouseFragment", "✅ 显示多选分类chip: $categoriesText")
                addFilterChip("分类: $categoriesText") {
                    // 删除时获取最新的filterState，避免闭包捕获旧状态
                    val currentState = viewModel.filterState.value
                    android.util.Log.d("WarehouseFragment", "🔄 点击删除多选分类chip")
                    android.util.Log.d("WarehouseFragment", "📊 创建时categories: ${filterState.categories}")
                    android.util.Log.d("WarehouseFragment", "📊 删除时最新categories: ${currentState.categories}")
                    viewModel.updateCategories(emptySet())
                }
            } else if (filterState.category.isNotBlank()) {
                // 向后兼容旧的单选分类
                android.util.Log.d("WarehouseFragment", "✅ 显示单选分类chip: ${filterState.category}")
                addFilterChip("分类: ${filterState.category}") {
                    android.util.Log.d("WarehouseFragment", "🔄 点击删除单选分类chip")
                    viewModel.setCategory("")
                }
            } else {
                android.util.Log.d("WarehouseFragment", "❌ 没有分类筛选条件")
            }
            
            // 添加子分类筛选条件
            if (filterState.subCategory.isNotBlank()) {
                addFilterChip("子分类: ${filterState.subCategory}") {
                    viewModel.setSubCategory("")
                }
            }
            
            // 添加品牌筛选条件
            if (filterState.brand.isNotBlank()) {
                addFilterChip("品牌: ${filterState.brand}") {
                    viewModel.setBrand("")
                }
            }
            
            // 添加位置区域筛选条件 - 支持多选显示
            if (filterState.locationAreas.isNotEmpty()) {
                val areasText = if (filterState.locationAreas.size <= 3) {
                    filterState.locationAreas.joinToString(",")
                } else {
                    "${filterState.locationAreas.take(3).joinToString(",")}..."
                }
                addFilterChip("区域: $areasText") {
                    viewModel.updateLocationAreas(emptySet())
                }
            } else if (filterState.locationArea.isNotBlank()) {
                // 向后兼容旧的单选区域
                addFilterChip("区域: ${filterState.locationArea}") {
                    viewModel.setLocationArea("")
                }
            }
            
            // 添加容器筛选条件
            if (filterState.container.isNotBlank()) {
                addFilterChip("容器: ${filterState.container}") {
                    viewModel.setContainer("")
                }
            }
            
            // 添加开封状态筛选条件 - 合并显示在一个chip中（参考其他多选字段）
            if (filterState.openStatuses.isNotEmpty()) {
                val statusTexts = filterState.openStatuses.map { if (it) "已开封" else "未开封" }
                val statusText = statusTexts.joinToString(",")
                addFilterChip("开封状态: $statusText") {
                    viewModel.updateOpenStatuses(emptySet())
                }
            } else if (filterState.openStatus != null) {
                // 向后兼容旧的单选开封状态
                val statusText = if (filterState.openStatus == true) "已开封" else "未开封"
                addFilterChip("开封状态: $statusText") {
                    viewModel.updateOpenStatus(null)
                }
            }
            
            // 添加评分筛选条件 - 合并显示在一个chip中（参考标签字段）
            if (filterState.ratings.isNotEmpty()) {
                val ratingsText = filterState.ratings.sorted().joinToString(",") { "${it.toInt()}颗星" }
                addFilterChip("评分: $ratingsText") {
                    viewModel.updateRatings(emptySet())
                }
            } else if (filterState.minRating != null) {
                addFilterChip("评分: ${filterState.minRating.toInt()}⭐+") {
                    viewModel.updateMinRating(null)
                }
            }
            
            // 添加季节筛选条件 - 合并显示在一个chip中（参考标签字段）
            if (filterState.seasons.isNotEmpty()) {
                val seasonText = filterState.seasons.joinToString(",")
                addFilterChip("季节: $seasonText") {
                    viewModel.updateSeasons(emptySet())
                }
            }
            
            // 添加标签筛选条件
            if (filterState.tags.isNotEmpty()) {
                val tagsText = if (filterState.tags.size <= 3) {
                    filterState.tags.joinToString(",")
                } else {
                    "${filterState.tags.take(3).joinToString(",")}..."
                }
                addFilterChip("标签: $tagsText") {
                    viewModel.updateTags(emptySet())
                }
            }
            
            // 添加数量范围筛选条件
            if (filterState.minQuantity != null || filterState.maxQuantity != null) {
                val quantityText = when {
                    filterState.minQuantity != null && filterState.maxQuantity != null ->
                        "数量: ${filterState.minQuantity}~${filterState.maxQuantity}"
                    filterState.minQuantity != null ->
                        "数量: ≥${filterState.minQuantity}"
                    else ->
                        "数量: ≤${filterState.maxQuantity}"
                }
                addFilterChip(quantityText) {
                    viewModel.updateQuantityRange(null, null)
                }
            }
            
            // 添加价格范围筛选条件
            if (filterState.minPrice != null || filterState.maxPrice != null) {
                val priceText = when {
                    filterState.minPrice != null && filterState.maxPrice != null ->
                        "价格: ${filterState.minPrice}~${filterState.maxPrice}"
                    filterState.minPrice != null ->
                        "价格: ≥${filterState.minPrice}"
                    else ->
                        "价格: ≤${filterState.maxPrice}"
                }
                addFilterChip(priceText) {
                    viewModel.updatePriceRange(null, null)
                }
            }
            
        } else {
            binding.filterChipContainer.visibility = View.GONE
            binding.clearAllChip.visibility = View.GONE
        }
    }
    
    private fun addFilterChip(text: String, onClose: () -> Unit) {
        android.util.Log.d("WarehouseFragment", "🏷️ 创建FilterChip: '$text'")
        val chip = Chip(requireContext()).apply {
            this.text = text
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                android.util.Log.d("WarehouseFragment", "❌ 点击删除FilterChip: '$text'")
                onClose()
            }
        }
        binding.filterChipGroup.addView(chip)
        android.util.Log.d("WarehouseFragment", "✅ FilterChip已添加，当前总数: ${binding.filterChipGroup.childCount}")
    }
    


    private fun observeDeleteResult() {
        viewModel.deleteResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                view?.let { 
                    Material3Feedback.showSuccess(it, "物品已删除")
                }
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                view?.let { 
                    Material3Feedback.showError(it, errorMessage)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 当Fragment重新可见时刷新数据（例如从添加物品页面返回）
        viewModel.refreshWarehouseItems()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        
        // 🔧 清理所有子Fragment（特别是FilterBottomSheet），防止状态被保留
        android.util.Log.d("WarehouseFragment", "🧹 onDestroyView - 清理子Fragment")
        childFragmentManager.fragments.forEach { fragment ->
            if (fragment is FilterBottomSheetFragmentV2) {
                android.util.Log.d("WarehouseFragment", "🗑️ 移除FilterBottomSheet: ${fragment.tag}")
                fragment.dismissAllowingStateLoss()
            }
        }
        
        _binding = null
    }
}