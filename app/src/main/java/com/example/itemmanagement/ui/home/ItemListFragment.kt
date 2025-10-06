package com.example.itemmanagement.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.adapter.WarehouseItemAdapter
import com.example.itemmanagement.databinding.FragmentItemListBinding

class ItemListFragment : Fragment() {

    private var _binding: FragmentItemListBinding? = null
    private val binding get() = _binding!!
    
    private val warehouseAdapter = WarehouseItemAdapter(
        onItemClick = { itemId ->
            val bundle = androidx.core.os.bundleOf("itemId" to itemId)
            findNavController().navigate(R.id.navigation_item_detail, bundle)
        },
        onEdit = { itemId ->
            // 暂时不实现编辑功能，或者可以跳转到编辑页面
        },
        onDelete = { itemId ->
            // 暂时不实现删除功能，或者可以添加删除确认对话框
        }
    )
    
    private val viewModel: ItemListViewModel by viewModels {
        ItemListViewModelFactory(
            (requireActivity().application as ItemManagementApplication).repository
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 获取传递的参数
        val listType = arguments?.getString("listType") ?: "all"
        val title = arguments?.getString("title") ?: "物品列表"
        
        setupUI(title)
        setupRecyclerView()
        loadData(listType)
        observeData()
    }
    
    private fun setupUI(title: String) {
        // 设置标题到ActionBar
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.let { activity ->
            activity.supportActionBar?.title = title
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = warehouseAdapter
        }
    }
    
    private fun loadData(listType: String) {
        when (listType) {
            "expiring" -> viewModel.loadExpiringItems()
            "expired" -> viewModel.loadExpiredItems()
            "low_stock" -> viewModel.loadLowStockItems()
            else -> viewModel.loadAllItems()
        }
    }
    
    private fun observeData() {
        viewModel.items.observe(viewLifecycleOwner) { items ->
            warehouseAdapter.submitList(items)
            binding.emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 