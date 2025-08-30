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
import com.example.itemmanagement.adapter.WarehouseItemAdapter
import com.example.itemmanagement.databinding.FragmentWarehouseBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController

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
        
        setupRecyclerView()
        setupSortBar()
        setupFilterButton()
        setupSearchView()
        observeItems()
        observeDeleteResult()
        observeFilterState()
    }

    private fun setupRecyclerView() {
        // 初始化适配器，传入所需的回调函数
        adapter = WarehouseItemAdapter(
            onItemClick = { itemId ->
                // 导航到详情页面
                val bundle = androidx.core.os.bundleOf("itemId" to itemId)
                findNavController().navigate(R.id.navigation_item_detail, bundle)
            },
            onEdit = { itemId ->
                // 导航到编辑页面（使用新架构）
                val bundle = androidx.core.os.bundleOf("itemId" to itemId)
                findNavController().navigate(R.id.action_navigation_warehouse_to_newEditItemFragment, bundle)
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
        // 重置所有按钮的状态
        binding.sortComprehensive.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        binding.sortQuantity.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        binding.sortPrice.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        binding.sortRating.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        binding.sortShelfLife.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        binding.sortAddTime.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        
        // 设置选中按钮的状态
        selectedButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        
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
    
    private fun setupFilterButton() {
        binding.filterButton.setOnClickListener {
            showFilterBottomSheet()
        }
    }
    
    private fun setupSearchView() {
        // 设置搜索框文本变化监听
        binding.searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchText = s?.toString() ?: ""
                viewModel.setSearchTerm(searchText)
                
                // 显示/隐藏清除按钮
                binding.clearButton.visibility = if (searchText.isNotEmpty()) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        
        // 设置清除按钮点击事件
        binding.clearButton.setOnClickListener {
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
        val filterBottomSheet = FilterBottomSheetFragment()
        filterBottomSheet.show(childFragmentManager, "FilterBottomSheetFragment")
    }

    private fun observeItems() {
        // 使用viewLifecycleOwner.lifecycleScope观察StateFlow，确保在View销毁时自动取消
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.warehouseItems.collectLatest { items ->
                
                if (items.isEmpty()) {
                    binding.emptyView.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.emptyView.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    adapter.submitList(items) {
                        // 在列表更新完成后滚动到顶部
                        binding.recyclerView.scrollToPosition(0)
                    }
                }
            }
        }
    }
    
    private fun observeFilterState() {
        // 使用viewLifecycleOwner.lifecycleScope观察StateFlow，确保在View销毁时自动取消
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filterState.collectLatest { filterState ->
                updateFilterChips(filterState)
                updateSortButtonsState(filterState)
            }
        }
    }
    
    private fun updateSortButtonsState(filterState: FilterState) {
        // 先重置所有按钮状态
        binding.sortComprehensive.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        binding.sortQuantity.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        binding.sortPrice.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        binding.sortRating.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        binding.sortShelfLife.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        binding.sortAddTime.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        
        // 设置当前选中的按钮和显示文本
        val selectedButton = when (filterState.sortOption) {
            SortOption.COMPREHENSIVE -> binding.sortComprehensive
            SortOption.QUANTITY -> binding.sortQuantity
            SortOption.PRICE -> binding.sortPrice
            SortOption.RATING -> binding.sortRating
            SortOption.REMAINING_SHELF_LIFE -> binding.sortShelfLife
            SortOption.UPDATE_TIME -> binding.sortAddTime
        }
        
        selectedButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        
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
        // 清空当前的筛选条件指示器
        binding.filterChipGroup.removeAllViews()
        
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
            
            // 添加分类筛选条件
            if (filterState.category.isNotBlank()) {
                addFilterChip("分类: ${filterState.category}") {
                    viewModel.setCategory("")
                }
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
            
            // 添加位置区域筛选条件
            if (filterState.locationArea.isNotBlank()) {
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
        val chip = Chip(requireContext()).apply {
            this.text = text
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                onClose()
            }
        }
        binding.filterChipGroup.addView(chip)
    }
    


    private fun observeDeleteResult() {
        viewModel.deleteResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "物品已删除", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}