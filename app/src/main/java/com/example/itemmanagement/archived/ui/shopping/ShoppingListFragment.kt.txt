package com.example.itemmanagement.ui.shopping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.adapter.ShoppingListAdapter
import com.example.itemmanagement.databinding.FragmentShoppingListBinding

class ShoppingListFragment : Fragment() {

    private var _binding: FragmentShoppingListBinding? = null
    private val binding get() = _binding!!
    
    private var listId: Long = 1L
    private var listName: String = "购物清单"
    
    private lateinit var viewModel: ShoppingListViewModel
    private lateinit var shoppingListAdapter: ShoppingListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShoppingListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 获取传入的参数
        arguments?.let { args ->
            listId = args.getLong("listId", 1L)
            listName = args.getString("listName", "购物清单")
        }
        
        // 更新界面标题
        activity?.title = listName
        
        // 使用正确的listId初始化ViewModel
        val factory = ShoppingListViewModelFactory(
            (requireActivity().application as ItemManagementApplication).repository,
            listId
        )
        viewModel = ViewModelProvider(this, factory)[ShoppingListViewModel::class.java]
        
        // 初始化适配器
        initializeAdapter()
        
        setupRecyclerView()
        setupObservers()
        setupActions()
    }
    
    private fun initializeAdapter() {
        shoppingListAdapter = ShoppingListAdapter(
            onItemChecked = { item, isChecked ->
                viewModel.toggleItemPurchaseStatus(item, isChecked)
            },
            onItemDelete = { item ->
                viewModel.deleteShoppingItem(item)
            },
            onItemAddToInventory = { item ->
                // 导航到添加物品页面，预填充数据
                val action = ShoppingListFragmentDirections.actionShoppingListToAddFromShopping(item)
                findNavController().navigate(action)
            }
        )
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = shoppingListAdapter
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.errorText.text = error
                binding.errorText.visibility = View.VISIBLE
            } else {
                binding.errorText.visibility = View.GONE
            }
        }

        viewModel.shoppingItems.observe(viewLifecycleOwner) { items ->
            shoppingListAdapter.submitList(items)
            
            if (items.isEmpty()) {
                binding.emptyView.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyView.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }

        // 推荐功能已移除
        // viewModel.recommendations.observe(viewLifecycleOwner) { recommendations ->
        //     // 推荐相关UI已删除
        // }
    }

    private fun setupActions() {
        // 设置添加购物物品按钮点击事件
        binding.fabAddList.setOnClickListener {
            navigateToAddShoppingItem()
        }

        // 智能推荐按钮已删除
        // binding.smartRecommendationsButton.setOnClickListener {
        //     // 推荐功能已移除
        // }
    }
    
    private fun navigateToAddShoppingItem() {
        // 导航到添加购物物品页面，传递当前清单ID
        val bundle = Bundle().apply {
            putLong("listId", listId)
        }
        findNavController().navigate(
            com.example.itemmanagement.R.id.action_shopping_list_to_add_shopping_item,
            bundle
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 