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
                // 导航到编辑页面
                val bundle = androidx.core.os.bundleOf("itemId" to itemId)
                findNavController().navigate(R.id.editItemFragment, bundle)
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
        binding.sortDefault.setOnClickListener {
            updateSortButtonState(it as TextView)
            viewModel.setSortOption(SortOption.UPDATE_TIME)
            viewModel.setSortDirection(SortDirection.DESC)
        }
        
        binding.sortPrice.setOnClickListener {
            val textView = it as TextView
            updateSortButtonState(textView)
            
            // 切换排序方向
            if (viewModel.filterState.value.sortOption == SortOption.QUANTITY) {
                val newDirection = if (viewModel.filterState.value.sortDirection == SortDirection.ASC) 
                    SortDirection.DESC else SortDirection.ASC
                viewModel.setSortDirection(newDirection)
                
                // 更新显示的排序方向
                textView.text = "数量 ${if (newDirection == SortDirection.ASC) "↑" else "↓"}"
            } else {
                viewModel.setSortOption(SortOption.QUANTITY)
                viewModel.setSortDirection(SortDirection.DESC)
                textView.text = "数量 ↓"
            }
        }
        
        binding.sortExpiration.setOnClickListener {
            val textView = it as TextView
            updateSortButtonState(textView)
            
            // 切换排序方向
            if (viewModel.filterState.value.sortOption == SortOption.EXPIRATION_DATE) {
                val newDirection = if (viewModel.filterState.value.sortDirection == SortDirection.ASC) 
                    SortDirection.DESC else SortDirection.ASC
                viewModel.setSortDirection(newDirection)
                
                // 更新显示的排序方向
                textView.text = "保质期 ${if (newDirection == SortDirection.ASC) "↑" else "↓"}"
            } else {
                viewModel.setSortOption(SortOption.EXPIRATION_DATE)
                viewModel.setSortDirection(SortDirection.ASC)
                textView.text = "保质期 ↑"
            }
        }
        
        binding.sortRating.setOnClickListener {
            val textView = it as TextView
            updateSortButtonState(textView)
            
            // 切换排序方向
            if (viewModel.filterState.value.sortOption == SortOption.NAME) {
                val newDirection = if (viewModel.filterState.value.sortDirection == SortDirection.ASC) 
                    SortDirection.DESC else SortDirection.ASC
                viewModel.setSortDirection(newDirection)
                
                // 更新显示的排序方向
                textView.text = "名称 ${if (newDirection == SortDirection.ASC) "↑" else "↓"}"
            } else {
                viewModel.setSortOption(SortOption.NAME)
                viewModel.setSortDirection(SortDirection.ASC)
                textView.text = "名称 ↑"
            }
        }
    }
    
    private fun updateSortButtonState(selectedButton: TextView) {
        // 重置所有按钮的状态
        binding.sortDefault.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        binding.sortPrice.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        binding.sortRating.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        binding.sortExpiration.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        
        // 设置选中按钮的状态
        selectedButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
        
        // 重置按钮文本（除了选中的按钮）
        if (selectedButton != binding.sortDefault) {
            binding.sortDefault.text = "默认"
        }
        if (selectedButton != binding.sortPrice) {
            binding.sortPrice.text = "数量"
        }
        if (selectedButton != binding.sortRating) {
            binding.sortRating.text = "名称"
        }
        if (selectedButton != binding.sortExpiration) {
            binding.sortExpiration.text = "保质期"
        }
    }
    
    private fun setupFilterButton() {
        binding.filterButton.setOnClickListener {
            showFilterBottomSheet()
        }
    }
    
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.setSearchTerm(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { viewModel.setSearchTerm(it) }
                return true
            }
        })
    }
    
    private fun showFilterBottomSheet() {
        val filterBottomSheet = FilterBottomSheetFragment()
        filterBottomSheet.show(childFragmentManager, "FilterBottomSheetFragment")
    }

    private fun observeItems() {
        // 使用lifecycleScope观察StateFlow
        lifecycleScope.launch {
            viewModel.warehouseItems.collectLatest { items ->
                
                if (items.isEmpty()) {
                    binding.emptyView.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.emptyView.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    adapter.submitList(items)
                }
            }
        }
    }
    
    private fun observeFilterState() {
        // 使用lifecycleScope观察StateFlow
        lifecycleScope.launch {
            viewModel.filterState.collectLatest { filterState ->
                updateFilterChips(filterState)
            }
        }
    }
    
    private fun updateFilterChips(filterState: FilterState) {
        // 清空当前的筛选条件指示器
        binding.filterChipGroup.removeAllViews()
        
        val hasFilter = filterState != FilterState() // 检查是否有筛选条件
        
        if (hasFilter) {
            binding.filterChipGroup.visibility = View.VISIBLE
            
            // 添加搜索词筛选条件
            if (filterState.searchTerm.isNotBlank()) {
                addFilterChip("搜索: ${filterState.searchTerm}") {
                    viewModel.setSearchTerm("")
                }
            }
            
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
                    // 使用公开方法而不是直接访问私有成员
                    val newFilter = viewModel.filterState.value.copy(
                        minQuantity = null, 
                        maxQuantity = null
                    )
                    viewModel.updateFilterState(newFilter)
                }
            }
            
            // 添加清除全部筛选条件的按钮
            addClearAllChip()
        } else {
            binding.filterChipGroup.visibility = View.GONE
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
    
    private fun addClearAllChip() {
        val chip = Chip(requireContext()).apply {
            text = "清除全部"
            chipIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_clear)
            setOnClickListener {
                viewModel.resetFilter()
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