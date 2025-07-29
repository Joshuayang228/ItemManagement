package com.example.itemmanagement.ui.shopping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.adapter.ShoppingListAdapter
import com.example.itemmanagement.databinding.FragmentShoppingListBinding

class ShoppingListFragment : Fragment() {

    private var _binding: FragmentShoppingListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShoppingListViewModel by viewModels {
        ShoppingListViewModelFactory(
            (requireActivity().application as ItemManagementApplication).repository
        )
    }

    private val shoppingListAdapter = ShoppingListAdapter(
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
        setupRecyclerView()
        setupObservers()
        setupActions()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = shoppingListAdapter
        }

        shoppingListAdapter.setOnListClickListener { listId ->
            // TODO: 导航到购物清单详情
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

        viewModel.shoppingLists.observe(viewLifecycleOwner) { lists ->
            shoppingListAdapter.submitList(lists)
            
            if (lists.isEmpty()) {
                binding.emptyView.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyView.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }

        viewModel.recommendations.observe(viewLifecycleOwner) { recommendations ->
            if (recommendations.isNotEmpty()) {
                binding.recommendationsBadge.visibility = View.VISIBLE
                binding.recommendationsBadge.text = "${recommendations.size}"
            } else {
                binding.recommendationsBadge.visibility = View.GONE
            }
        }
    }

    private fun setupActions() {
        // 设置添加购物物品按钮点击事件
        binding.fabAddList.setOnClickListener {
            navigateToAddShoppingItem()
        }

        // 设置智能推荐按钮点击事件  
        binding.smartRecommendationsButton.setOnClickListener {
            // 暂时显示提示，但不显示"开发中"
            android.widget.Toast.makeText(context, "智能推荐功能即将推出", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun navigateToAddShoppingItem() {
        // 导航到添加购物物品页面
        findNavController().navigate(
            com.example.itemmanagement.R.id.action_shopping_list_to_add_shopping_item
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 