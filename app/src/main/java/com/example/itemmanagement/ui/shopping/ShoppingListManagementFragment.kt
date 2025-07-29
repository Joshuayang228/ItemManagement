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
import com.example.itemmanagement.adapter.ShoppingListManagementAdapter
import com.example.itemmanagement.data.entity.ShoppingListEntity
import com.example.itemmanagement.databinding.FragmentShoppingListManagementBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/**
 * 购物清单管理页面
 * 显示所有购物清单，支持创建、编辑、删除清单
 */
class ShoppingListManagementFragment : Fragment() {

    private var _binding: FragmentShoppingListManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShoppingListManagementViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ShoppingListManagementViewModel(requireActivity().application) as T
            }
        }
    }

    private lateinit var adapter: ShoppingListManagementAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShoppingListManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFab()
        observeViewModel()
        setupToolbar()
    }

    private fun setupRecyclerView() {
        adapter = ShoppingListManagementAdapter(
            onItemClick = { shoppingList ->
                // 导航到清单详情页面
                navigateToListDetail(shoppingList.id)
            },
            onEditClick = { shoppingList ->
                // 显示编辑对话框
                showEditListDialog(shoppingList)
            },
            onDeleteClick = { shoppingList ->
                // 显示删除确认对话框
                showDeleteConfirmDialog(shoppingList)
            },
            onCompleteClick = { shoppingList ->
                // 切换清单状态
                when (shoppingList.status) {
                    com.example.itemmanagement.data.entity.ShoppingListStatus.ACTIVE -> {
                        viewModel.completeShoppingList(shoppingList)
                    }
                    else -> {
                        viewModel.reactivateShoppingList(shoppingList)
                    }
                }
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@ShoppingListManagementFragment.adapter
        }
    }

    private fun setupFab() {
        binding.fabAddList.setOnClickListener {
            showCreateListDialog()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = "购物清单管理"
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }
    }

    private fun observeViewModel() {
        // 观察所有购物清单
        viewModel.allShoppingLists.observe(viewLifecycleOwner) { lists ->
            adapter.submitList(lists)
            updateEmptyView(lists.isEmpty())
        }

        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // 观察消息
        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                viewModel.clearMessage()
            }
        }

        // 观察错误
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // 观察导航事件
        viewModel.navigateToListDetail.observe(viewLifecycleOwner) { listId ->
            listId?.let {
                navigateToListDetail(it)
                viewModel.navigateToListDetailComplete()
            }
        }
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyView.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showCreateListDialog() {
        val dialogView = LayoutInflater.from(context).inflate(
            com.example.itemmanagement.R.layout.dialog_create_shopping_list, null
        )
        
        val nameEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            com.example.itemmanagement.R.id.etListName
        )
        val descriptionEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            com.example.itemmanagement.R.id.etListDescription
        )
        val budgetEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            com.example.itemmanagement.R.id.etListBudget
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("创建购物清单")
            .setView(dialogView)
            .setPositiveButton("创建") { _, _ ->
                val name = nameEditText.text.toString()
                val description = descriptionEditText.text.toString()
                val budgetText = budgetEditText.text.toString()
                val budget = if (budgetText.isNotEmpty()) budgetText.toDoubleOrNull() else null

                if (name.isNotEmpty()) {
                    viewModel.createShoppingList(name, description, estimatedBudget = budget)
                } else {
                    Snackbar.make(binding.root, "请输入清单名称", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditListDialog(shoppingList: ShoppingListEntity) {
        val dialogView = LayoutInflater.from(context).inflate(
            com.example.itemmanagement.R.layout.dialog_create_shopping_list, null
        )
        
        val nameEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            com.example.itemmanagement.R.id.etListName
        )
        val descriptionEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            com.example.itemmanagement.R.id.etListDescription
        )
        val budgetEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            com.example.itemmanagement.R.id.etListBudget
        )

        // 预填充数据
        nameEditText.setText(shoppingList.name)
        descriptionEditText.setText(shoppingList.description)
        shoppingList.estimatedBudget?.let { budget ->
            budgetEditText.setText(budget.toString())
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("编辑购物清单")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val name = nameEditText.text.toString()
                val description = descriptionEditText.text.toString()
                val budgetText = budgetEditText.text.toString()
                val budget = if (budgetText.isNotEmpty()) budgetText.toDoubleOrNull() else null

                if (name.isNotEmpty()) {
                    val updatedList = shoppingList.copy(
                        name = name,
                        description = description,
                        estimatedBudget = budget
                    )
                    viewModel.updateShoppingList(updatedList)
                } else {
                    Snackbar.make(binding.root, "请输入清单名称", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDeleteConfirmDialog(shoppingList: ShoppingListEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除购物清单")
            .setMessage("确定要删除「${shoppingList.name}」吗？此操作无法撤销。")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteShoppingList(shoppingList)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun navigateToListDetail(listId: Long) {
        // 导航到购物清单详情页面
        // 这里暂时导航到现有的购物清单页面，后续可以改为新的详情页面
        findNavController().navigate(
            com.example.itemmanagement.R.id.navigation_shopping_list
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 