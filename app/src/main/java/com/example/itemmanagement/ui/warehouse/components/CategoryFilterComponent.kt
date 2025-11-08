package com.example.itemmanagement.ui.warehouse.components

import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
 * 分类筛选组件
 * 
 * 功能：
 * 1. 管理分类多选ChipGroup
 * 2. 处理品牌输入框的自动完成功能
 * 3. 支持动态展开/收起
 * 4. 与ViewModel同步状态
 */
class CategoryFilterComponent(
    private val binding: FragmentFilterBottomSheetBinding,
    private val viewModel: WarehouseViewModel,
    private val lifecycleOwner: LifecycleOwner,
    private val animationManager: FilterAnimationManager
) : BaseFilterComponent(), MultiSelectFilterComponent {
    
    companion object {
        private const val COMPONENT_ID = "category_filter"
    }
    
    // 当前选中的分类集合
    private var selectedCategories = mutableSetOf<String>()
    
    // 所有可用分类列表
    private var availableCategories = listOf<String>()
    
    // 品牌输入监听器（用于清理）
    private var brandTextWatcher: android.text.TextWatcher? = null
    
    // 防止循环更新的标志
    private var isUpdatingFromState = false
    
    override fun getComponentId(): String = COMPONENT_ID
    
    /**
     * 初始化组件
     */
    fun initialize() {
        setupCategoryChipGroup()
        setupBrandDropdown()
        observeViewModelData()
        setReady()
    }
    
    /**
     * 设置分类ChipGroup
     */
    private fun setupCategoryChipGroup() {
        // 设置ChipGroup的多选监听器
        binding.coreSection.categoryChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            // 如果正在从FilterState更新UI，跳过这个回调
            if (isUpdatingFromState) {
                return@setOnCheckedStateChangeListener
            }
            
            if (isReady()) {
                val newSelectedCategories = checkedIds.mapNotNull { chipId ->
                    val chip = group.findViewById<Chip>(chipId)
                    chip?.text?.toString()
                }.toSet()
                
                if (newSelectedCategories != selectedCategories) {
                    selectedCategories.clear()
                    selectedCategories.addAll(newSelectedCategories)
                    
                    // 🔑 核心修复：传递新创建的Set副本，避免对象引用问题
                    viewModel.updateCategories(newSelectedCategories)
                    notifyValueChanged(newSelectedCategories)
                }
            }
        }
        
        // 设置展开/收起动画
        animationManager.setupChipGroupExpandCollapse(
            binding.coreSection.categoryChipGroup,
            binding.coreSection.categoryExpandButton,
            "category_expand"
        )
    }
    
    /**
     * 设置品牌下拉框
     */
    private fun setupBrandDropdown() {
        val brandDropdown = binding.coreSection.brandDropdown as? AutoCompleteTextView
        brandDropdown?.apply {
            // 基础配置
            threshold = 0
            setOnClickListener {
                if (text.isEmpty()) {
                    showDropDown()
                }
                requestFocus()
            }
            
            setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus && text.isEmpty()) {
                    (view as? AutoCompleteTextView)?.showDropDown()
                }
            }
            
            // 确保触摸模式下可以获得焦点
            isFocusableInTouchMode = true
            
            // 设置文本对齐
            gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
            textAlignment = android.view.View.TEXT_ALIGNMENT_VIEW_START
            textDirection = android.view.View.TEXT_DIRECTION_LTR
            
            // 设置文本变化监听器
            brandTextWatcher = object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val brandText = s?.toString() ?: ""
                    viewModel.setBrand(brandText)
                    notifyValueChanged(brandText)
                }
                
                override fun afterTextChanged(s: android.text.Editable?) {}
            }
            addTextChangedListener(brandTextWatcher)
            
            // 设置品牌选择监听
            setOnItemClickListener { _, _, position, _ ->
                val selectedBrand = adapter?.getItem(position) as String
                viewModel.setBrand(selectedBrand)
                notifyValueChanged(selectedBrand)
            }
        }
    }
    
    /**
     * 观察ViewModel数据变化
     */
    private fun observeViewModelData() {
        // 观察分类列表变化
        viewModel.categories.observe(lifecycleOwner) { categories ->
            availableCategories = categories ?: emptyList()
            updateCategoriesChipGroup(categories ?: emptyList())
        }
        
        // 观察品牌列表变化
        viewModel.brands.observe(lifecycleOwner) { brands ->
            updateBrandAdapter(brands ?: emptyList())
        }
    }
    
    /**
     * 更新分类ChipGroup
     */
    private fun updateCategoriesChipGroup(categories: List<String>) {
        // 保存当前滚动位置
        val currentScrollY = binding.contentScrollView.scrollY
        
        // 清除现有chips
        binding.coreSection.categoryChipGroup.removeAllViews()
        
        // 添加新的chips
        categories.forEach { category ->
            val layoutInflater = LayoutInflater.from(binding.root.context)
            val chip = layoutInflater.inflate(
                R.layout.item_suggestion_chip, 
                binding.coreSection.categoryChipGroup, 
                false
            ) as Chip
            
            chip.apply {
                text = category
                isCheckable = true
                isChecked = selectedCategories.contains(category)
            }
            
            binding.coreSection.categoryChipGroup.addView(chip)
        }
        
        // 重新设置展开/收起功能
        animationManager.setupChipGroupExpandCollapse(
            binding.coreSection.categoryChipGroup,
            binding.coreSection.categoryExpandButton,
            "category_expand"
        )
        
        // 恢复滚动位置
        binding.contentScrollView.post {
            binding.contentScrollView.scrollTo(0, currentScrollY)
        }
    }
    
    /**
     * 更新品牌适配器
     */
    private fun updateBrandAdapter(brands: List<String>) {
        val brandDropdown = binding.coreSection.brandDropdown as? AutoCompleteTextView
        brandDropdown?.let { dropdown ->
            val adapter = ArrayAdapter(dropdown.context, R.layout.item_dropdown, brands)
            dropdown.setAdapter(adapter)
        }
    }
    
    override fun updateFromState(filterState: FilterState) {
        // updateFromState是响应ViewModel状态变化，不应该再次通知ViewModel（防止循环）
        updateCategoriesSelection(filterState.categories, notifyViewModel = false)
        
        // 更新品牌输入框
        updateBrandInput(filterState.brand)
    }
    
    /**
     * 更新分类选择状态
     * @param categories 新的分类集合
     * @param notifyViewModel 是否通知ViewModel（默认false，防止循环调用）
     */
    private fun updateCategoriesSelection(categories: Set<String>, notifyViewModel: Boolean = false) {
        if (categories != selectedCategories) {
            // 设置标志，防止监听器回调
            isUpdatingFromState = true
            
            selectedCategories.clear()
            selectedCategories.addAll(categories)
            
            // 更新ChipGroup选中状态
            for (i in 0 until binding.coreSection.categoryChipGroup.childCount) {
                val chip = binding.coreSection.categoryChipGroup.getChildAt(i) as? Chip
                chip?.let {
                    it.isChecked = categories.contains(it.text.toString())
                }
            }
            
            // 恢复标志
            isUpdatingFromState = false
            
            // 只有在明确要求时才通知ViewModel（避免双向绑定循环）
            if (notifyViewModel) {
                // 🔑 核心修复：确保传递Set的副本，避免对象引用问题
                val categoriesCopy = categories.toSet()
                viewModel.updateCategories(categoriesCopy)
                notifyValueChanged(categoriesCopy)
            }
        }
    }
    
    /**
     * 更新品牌输入框
     */
    private fun updateBrandInput(brand: String) {
        val currentText = binding.coreSection.brandDropdown.text.toString()
        if (currentText != brand) {
            // 暂时移除监听器，防止循环更新
            brandTextWatcher?.let { 
                binding.coreSection.brandDropdown.removeTextChangedListener(it)
            }
            
            // 保存光标位置
            val currentSelection = binding.coreSection.brandDropdown.selectionStart
            binding.coreSection.brandDropdown.setText(brand, false)
            
            // 恢复光标位置
            val newSelection = minOf(currentSelection, brand.length)
            binding.coreSection.brandDropdown.setSelection(newSelection)
            
            // 重新添加监听器
            brandTextWatcher?.let { 
                binding.coreSection.brandDropdown.addTextChangedListener(it)
            }
        }
    }
    
    override fun resetToDefault() {
        // 重置分类选择
        selectedCategories.clear()
        binding.coreSection.categoryChipGroup.clearCheck()
        
        // 重置品牌输入
        brandTextWatcher?.let { 
            binding.coreSection.brandDropdown.removeTextChangedListener(it)
        }
        binding.coreSection.brandDropdown.setText("", false)
        brandTextWatcher?.let { 
            binding.coreSection.brandDropdown.addTextChangedListener(it)
        }
    }
    
    // MultiSelectFilterComponent implementation
    override fun getSelectedValues(): Set<String> {
        return selectedCategories.toSet()
    }
    
    override fun setSelectedValues(values: Set<String>) {
        // setSelectedValues是外部程序化调用，需要通知ViewModel
        updateCategoriesSelection(values, notifyViewModel = true)
    }
    
    override fun clearSelection() {
        setSelectedValues(emptySet())
    }
    
    override fun getAllOptions(): List<String> {
        return availableCategories
    }
    
    override fun updateOptions(options: List<String>) {
        availableCategories = options
        updateCategoriesChipGroup(options)
    }
    
    /**
     * 获取当前选中的品牌
     */
    fun getSelectedBrand(): String {
        return binding.coreSection.brandDropdown.text.toString()
    }
    
    /**
     * 设置品牌
     */
    fun setBrand(brand: String) {
        updateBrandInput(brand)
        viewModel.setBrand(brand)
    }
    
    /**
     * 获取所有可用品牌
     */
    fun getAvailableBrands(): List<String> {
        val adapter = (binding.coreSection.brandDropdown as? AutoCompleteTextView)?.adapter
        return if (adapter != null) {
            (0 until adapter.count).map { adapter.getItem(it).toString() }
        } else {
            emptyList()
        }
    }
    

    override fun cleanup() {
        super.cleanup()
        
        // 清理品牌输入框监听器
        brandTextWatcher?.let { 
            binding.coreSection.brandDropdown.removeTextChangedListener(it)
        }
        brandTextWatcher = null
        
        // 清理ChipGroup监听器
        binding.coreSection.categoryChipGroup.setOnCheckedStateChangeListener(null)
        
        // 清理品牌输入框监听器
        binding.coreSection.brandDropdown.setOnClickListener(null)
        binding.coreSection.brandDropdown.setOnFocusChangeListener(null)
        binding.coreSection.brandDropdown.onItemClickListener = null
        
        // 清空选中状态
        selectedCategories.clear()
    }
}

