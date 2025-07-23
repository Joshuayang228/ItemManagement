package com.example.itemmanagement.ui.home

import android.os.Bundle
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
import com.example.itemmanagement.adapter.ItemAdapter
import com.example.itemmanagement.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val itemAdapter = ItemAdapter()
    
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
        observeItems()
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
            adapter = itemAdapter
        }

        // 设置物品点击事件
        itemAdapter.setOnItemClickListener { item ->
            val bundle = androidx.core.os.bundleOf("itemId" to item.id)
            findNavController().navigate(R.id.navigation_item_detail, bundle)
        }
        
        // 设置物品删除事件
        itemAdapter.setOnDeleteClickListener { item ->
            viewModel.deleteItem(item.id)
        }
    }

    private fun setupSearchView() {
        binding.searchEditText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(v.text.toString())
                return@setOnEditorActionListener true
            }
            false
        }

        binding.searchIcon.setOnClickListener {
            performSearch(binding.searchEditText.text.toString())
        }
    }

    private fun setupButtons() {
        // 设置悬浮添加按钮点击事件
        binding.addButton.setOnClickListener {
            onAddButtonClick()
        }

        // 设置顶部添加按钮点击事件
        binding.topAddButton.setOnClickListener {
            onAddButtonClick()
        }
    }

    private fun onAddButtonClick() {
        // 导航到添加物品页面
        findNavController().navigate(R.id.action_navigation_home_to_addItemFragment)
    }

    private fun performSearch(query: String) {
        // TODO: 实现搜索功能
        Toast.makeText(context, "搜索: $query", Toast.LENGTH_SHORT).show()
    }

    private fun observeItems() {
        // 观察数据库中的物品数据
        viewModel.allItems.observe(viewLifecycleOwner) { items ->
            itemAdapter.submitList(items)
            binding.emptyView?.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}