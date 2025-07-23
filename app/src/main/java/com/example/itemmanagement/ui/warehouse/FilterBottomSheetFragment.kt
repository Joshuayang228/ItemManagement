package com.example.itemmanagement.ui.warehouse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.activityViewModels
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentFilterBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class FilterBottomSheetFragment : BottomSheetDialogFragment() {
    
    private var _binding: FragmentFilterBottomSheetBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: WarehouseViewModel by activityViewModels {
        WarehouseViewModelFactory(
            (requireActivity().application as ItemManagementApplication).repository
        )
    }
    
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
        
        // 设置当前筛选状态
        setupCurrentFilter()
        
        // 设置下拉列表
        setupDropdowns()
        
        // 设置按钮点击事件
        setupButtons()
    }
    
    private fun setupCurrentFilter() {
        // 使用lifecycleScope观察StateFlow
        lifecycleScope.launch {
            viewModel.filterState.collectLatest { filterState ->
                // 设置分类
                binding.categoryDropdown.setText(filterState.category, false)
                
                // 设置子分类
                binding.subCategoryDropdown.setText(filterState.subCategory, false)
                
                // 设置品牌
                binding.brandDropdown.setText(filterState.brand, false)
                
                // 设置位置区域
                binding.locationAreaDropdown.setText(filterState.locationArea, false)
                
                // 设置容器
                binding.containerDropdown.setText(filterState.container, false)
                
                // 设置子位置
                binding.sublocationDropdown.setText(filterState.sublocation, false)
                
                // 设置数量范围
                binding.minQuantityInput.setText(filterState.minQuantity?.toString() ?: "")
                binding.maxQuantityInput.setText(filterState.maxQuantity?.toString() ?: "")
            }
        }
    }
    
    private fun setupDropdowns() {
        // 观察分类列表
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, categories)
            (binding.categoryDropdown as? AutoCompleteTextView)?.setAdapter(adapter)
        }
        
        // 设置分类下拉列表的选择监听
        (binding.categoryDropdown as? AutoCompleteTextView)?.setOnItemClickListener { _, _, position, _ ->
            val selectedCategory = (binding.categoryDropdown as? AutoCompleteTextView)?.adapter?.getItem(position) as String
            viewModel.setCategory(selectedCategory)
        }
        
        // 观察子分类列表
        viewModel.subCategories.observe(viewLifecycleOwner) { subCategories ->
            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, subCategories)
            (binding.subCategoryDropdown as? AutoCompleteTextView)?.setAdapter(adapter)
        }
        
        // 设置子分类下拉列表的选择监听
        (binding.subCategoryDropdown as? AutoCompleteTextView)?.setOnItemClickListener { _, _, position, _ ->
            val selectedSubCategory = (binding.subCategoryDropdown as? AutoCompleteTextView)?.adapter?.getItem(position) as String
            viewModel.setSubCategory(selectedSubCategory)
        }
        
        // 观察品牌列表
        viewModel.brands.observe(viewLifecycleOwner) { brands ->
            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, brands)
            (binding.brandDropdown as? AutoCompleteTextView)?.setAdapter(adapter)
        }
        
        // 设置品牌下拉列表的选择监听
        (binding.brandDropdown as? AutoCompleteTextView)?.setOnItemClickListener { _, _, position, _ ->
            val selectedBrand = (binding.brandDropdown as? AutoCompleteTextView)?.adapter?.getItem(position) as String
            viewModel.setBrand(selectedBrand)
        }
        
        // 观察位置区域列表
        viewModel.locationAreas.observe(viewLifecycleOwner) { areas ->
            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, areas)
            (binding.locationAreaDropdown as? AutoCompleteTextView)?.setAdapter(adapter)
        }
        
        // 观察容器列表
        viewModel.containers.observe(viewLifecycleOwner) { containers ->
            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, containers)
            (binding.containerDropdown as? AutoCompleteTextView)?.setAdapter(adapter)
        }
        
        // 观察子位置列表
        viewModel.sublocations.observe(viewLifecycleOwner) { sublocations ->
            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, sublocations)
            (binding.sublocationDropdown as? AutoCompleteTextView)?.setAdapter(adapter)
        }
        
        // 设置位置区域下拉列表的选择监听
        (binding.locationAreaDropdown as? AutoCompleteTextView)?.setOnItemClickListener { _, _, position, _ ->
            val selectedArea = (binding.locationAreaDropdown as? AutoCompleteTextView)?.adapter?.getItem(position) as String
            viewModel.setLocationArea(selectedArea)
            
            // 清空容器
            binding.containerDropdown.setText("", false)
        }
        
        // 设置容器下拉列表的选择监听
        (binding.containerDropdown as? AutoCompleteTextView)?.setOnItemClickListener { _, _, position, _ ->
            val selectedContainer = (binding.containerDropdown as? AutoCompleteTextView)?.adapter?.getItem(position) as String
            viewModel.setContainer(selectedContainer)
            
            // 清空子位置
            binding.sublocationDropdown.setText("", false)
        }
    }
    
    private fun setupButtons() {
        // 重置按钮
        binding.resetButton.setOnClickListener {
            viewModel.resetFilter()
            dismiss()
        }
        
        // 完成按钮
        binding.applyButton.setOnClickListener {
            applyFilter()
            dismiss()
        }
    }
    
    private fun applyFilter() {
        // 获取当前筛选状态
        val currentFilter = viewModel.filterState.value
        
        // 读取输入框的值
        val categoryInput = binding.categoryDropdown.text.toString().trim()
        val subCategoryInput = binding.subCategoryDropdown.text.toString().trim()
        val brandInput = binding.brandDropdown.text.toString().trim()
        val locationAreaInput = binding.locationAreaDropdown.text.toString().trim()
        val containerInput = binding.containerDropdown.text.toString().trim()
        val sublocationInput = binding.sublocationDropdown.text.toString().trim()
        val minQuantityInput = binding.minQuantityInput.text.toString()
        val maxQuantityInput = binding.maxQuantityInput.text.toString()
        
        // 构建新的筛选状态
        val newFilter = currentFilter.copy(
            // 分类
            category = categoryInput,
            subCategory = subCategoryInput,
            
            // 品牌
            brand = brandInput,
            
            // 位置
            locationArea = locationAreaInput,
            container = containerInput,
            sublocation = sublocationInput,
            
            // 数量范围
            minQuantity = minQuantityInput.toIntOrNull(),
            maxQuantity = maxQuantityInput.toIntOrNull()
        )
        
        // 如果状态相同，先设置一个临时状态再设置目标状态，强制触发更新
        if (currentFilter == newFilter) {
            viewModel.updateFilterState(FilterState())
        }
        
        viewModel.updateFilterState(newFilter)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 