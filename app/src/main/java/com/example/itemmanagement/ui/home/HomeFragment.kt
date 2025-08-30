package com.example.itemmanagement.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.adapter.HomeAdapter
import com.example.itemmanagement.databinding.FragmentHomeBinding
import com.example.itemmanagement.test.TestDataInserter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeAdapter = HomeAdapter()
    
    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(
            (requireActivity().application as ItemManagementApplication).repository
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearchView()
        setupButtons()
        observeData()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            // 使用StaggeredGridLayoutManager实现瀑布流布局
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL).apply {
                // 设置间距
                addItemDecoration(object : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: android.graphics.Rect,
                        view: View,
                        parent: androidx.recyclerview.widget.RecyclerView,
                        state: androidx.recyclerview.widget.RecyclerView.State
                    ) {
                        val spacing = resources.getDimensionPixelSize(R.dimen.photo_grid_spacing)
                        outRect.set(spacing, spacing, spacing, spacing)
                    }
                })
            }
            adapter = homeAdapter
        }

        // 设置物品点击事件
        homeAdapter.setOnItemClickListener { item ->
            val bundle = androidx.core.os.bundleOf("itemId" to item.id)
            findNavController().navigate(R.id.navigation_item_detail, bundle)
        }
        
        // 设置物品删除事件
        homeAdapter.setOnDeleteClickListener { item ->
            viewModel.deleteItem(item.id)
        }
        
        // 设置功能按钮点击事件
        homeAdapter.setOnFunctionClickListener { functionType ->
            when (functionType) {
                "expiring" -> navigateToItemList("expiring", "即将过期的物品")
                "expired" -> navigateToItemList("expired", "过期物品")
                "low_stock" -> navigateToItemList("low_stock", "库存不足的物品")
                "shopping_list" -> {
                    // 导航到购物清单管理页面
                    findNavController().navigate(R.id.navigation_shopping_list_management)
                }
            }
        }
    }

    private fun setupSearchView() {
        // 移除实时搜索，改为手动触发搜索
        // 只保留文本变化监听器来控制清除按钮的可见性
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                // 只控制清除按钮的可见性，不触发搜索
                binding.clearSearchIcon.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            }
        })

        // 设置搜索按键监听器
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(binding.searchEditText.text.toString())
                // 隐藏键盘
                hideKeyboard()
                return@setOnEditorActionListener true
            }
            false
        }

        // 设置搜索图标点击事件
        binding.searchIcon.setOnClickListener {
            performSearch(binding.searchEditText.text.toString())
            hideKeyboard()
        }
        
        // 设置清除搜索图标点击事件
        binding.clearSearchIcon.setOnClickListener {
            clearSearch()
        }
        
        // 长按搜索图标清空搜索
        binding.searchIcon.setOnLongClickListener {
            clearSearch()
            true
        }
    }

    private fun setupButtons() {
        // 设置顶部添加按钮点击事件
        binding.topAddButton.setOnClickListener {
            onAddButtonClick()
        }
        
        // 设置悬浮添加按钮点击事件
        binding.addButton.setOnClickListener {
            onAddButtonClick()
        }
        
        // 长按悬浮按钮插入测试数据（临时功能）
        binding.addButton.setOnLongClickListener {
            insertTestData()
            true
        }
    }
    
    private fun navigateToItemList(listType: String, title: String) {
        val bundle = androidx.core.os.bundleOf(
            "listType" to listType,
            "title" to title
        )
        findNavController().navigate(R.id.action_navigation_home_to_itemListFragment, bundle)
    }

    private fun onAddButtonClick() {
        // 导航到添加物品页面（使用新架构）
        findNavController().navigate(R.id.action_navigation_home_to_newAddItemFragment)
    }
    
    /**
     * 插入测试数据（临时功能）
     * 长按悬浮按钮触发
     */
    private fun insertTestData() {
        Toast.makeText(context, "正在生成测试数据...", Toast.LENGTH_SHORT).show()
        
        TestDataInserter.insertTestData(requireContext()) { success, message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            if (success) {
                // 刷新数据显示
                viewModel.refreshData()
            }
        }
    }

    private fun performSearch(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isNotEmpty()) {
            viewModel.setSearchQuery(trimmedQuery)
            // 显示搜索提示
            Toast.makeText(context, "搜索: $trimmedQuery", Toast.LENGTH_SHORT).show()
        } else {
            // 当搜索内容为空时，清空搜索结果
            viewModel.clearSearch()
            Toast.makeText(context, "已清空搜索", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun clearSearch() {
        binding.searchEditText.setText("")
        viewModel.clearSearch()
        hideKeyboard()
        Toast.makeText(context, "已清空搜索", Toast.LENGTH_SHORT).show()
    }
    
    private fun hideKeyboard() {
        val inputMethodManager = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) 
            as android.view.inputmethod.InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
    }

    private fun observeData() {
        // 观察物品数据
        viewModel.items.observe(viewLifecycleOwner) { items ->
            homeAdapter.submitList(items)
            updateEmptyView(items.isEmpty())
        }
        
        // 观察搜索状态
        viewModel.isSearching.observe(viewLifecycleOwner) { _ ->
            // 可以根据搜索状态更新UI，比如显示搜索指示器
            // 这里暂时不做特殊处理，搜索结果会直接显示在列表中
        }
    }
    
    private fun updateEmptyView(isEmpty: Boolean) {
        val currentQuery = viewModel.getCurrentSearchQuery()
        val isSearching = !currentQuery.isNullOrBlank()
        
        if (isEmpty) {
            if (isSearching) {
                // 普通搜索无结果
                binding.emptyView.visibility = View.GONE
                binding.searchEmptyView.visibility = View.VISIBLE
                binding.searchEmptyHint.text = "未找到包含「$currentQuery」的物品\n试试更换关键词或清空搜索"
            } else {
                // 非搜索状态的空视图
                binding.emptyView.visibility = View.VISIBLE
                binding.searchEmptyView.visibility = View.GONE
            }
        } else {
            // 有数据时隐藏所有空视图
            binding.emptyView.visibility = View.GONE
            binding.searchEmptyView.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}