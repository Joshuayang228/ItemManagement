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
import com.example.itemmanagement.R
import com.example.itemmanagement.adapter.ShoppingListManagementAdapter
import com.example.itemmanagement.adapter.ShoppingListProgress
import com.example.itemmanagement.data.entity.ShoppingListEntity
import com.example.itemmanagement.data.entity.ShoppingListStatus
import com.example.itemmanagement.data.entity.ShoppingListType
import com.example.itemmanagement.databinding.FragmentShoppingListManagementM3Binding
import com.example.itemmanagement.utils.SnackbarHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.runBlocking
import java.util.Date

/**
 * Material 3购物清单管理界面
 * 现代化的购物管理体验，包含概览仪表板和清单列表
 */
class ShoppingListManagementM3Fragment : Fragment() {

    private var _binding: FragmentShoppingListManagementM3Binding? = null
    private val binding get() = _binding!!

    private val viewModel: ShoppingListManagementViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ShoppingListManagementViewModel(
                    requireActivity().application,
                    (requireActivity().application as ItemManagementApplication).repository
                ) as T
            }
        }
    }
    
    private lateinit var adapter: ShoppingListManagementAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShoppingListManagementM3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupUI()
        setupClickListeners()
        observeViewModel()
        
        // 加载概览统计数据
        viewModel.loadOverviewStats()
        
        // 隐藏底部导航栏
        hideBottomNavigation()
    }

    private fun setupRecyclerView() {
        adapter = ShoppingListManagementAdapter(
            onItemClick = { shoppingList ->
                // 导航到清单详情页面
                navigateToListDetail(shoppingList.id, shoppingList.name)
            },
            onEditClick = { shoppingList ->
                // 显示编辑对话框
                showEditListDialog(shoppingList)
            },
            onDeleteClick = { shoppingList ->
                // 显示删除确认对话框
                showDeleteConfirmDialog(shoppingList.id)
            },
            onCompleteClick = { shoppingList ->
                // 标记清单为已完成
                viewModel.completeShoppingList(shoppingList.id)
            },
            getProgressData = { listId ->
                // 从ViewModel获取进度数据
                runBlocking {
                    val totalItems = viewModel.getShoppingItemsCount(listId)
                    val completedItems = viewModel.getPurchasedItemsCount(listId)
                    val progress = if (totalItems > 0) (completedItems * 100 / totalItems) else 0
                    ShoppingListProgress(totalItems, completedItems, progress)
                }
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@ShoppingListManagementM3Fragment.adapter
        }
    }

    private fun setupUI() {
        // 设置标题（由于没有 toolbar，标题在 Activity 中设置）
        activity?.title = "购物清单管理"
    }

    private fun setupClickListeners() {
        // FAB创建新清单
        binding.fabAddList.setOnClickListener {
            showCreateListDialog()
        }
        
        // 刷新按钮
        binding.headerShoppingDashboard.root.findViewById<android.widget.ImageButton>(R.id.btnRefresh)?.setOnClickListener {
            // 刷新概览统计数据
            viewModel.loadOverviewStats()
            // 显示提示
            com.google.android.material.snackbar.Snackbar.make(
                binding.root,
                "已刷新",
                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun observeViewModel() {
        // 观察购物清单列表
        viewModel.activeShoppingLists.observe(viewLifecycleOwner) { lists ->
            adapter.submitList(lists)
            
            // 更新空状态
            if (lists.isEmpty()) {
                binding.recyclerView.visibility = View.GONE
                binding.emptyView.visibility = View.VISIBLE
            } else {
                binding.recyclerView.visibility = View.VISIBLE
                binding.emptyView.visibility = View.GONE
            }
        }
        
        // 观察概览统计（访问include布局中的view需要使用findViewById）
        viewModel.overviewStats.observe(viewLifecycleOwner) { stats ->
            if (stats != null) {
                binding.headerShoppingDashboard.root.findViewById<android.widget.TextView>(R.id.tvActiveListsCount)?.text = stats.activeListsCount.toString()
                // ⭐ 修复：使用正确的 ID tvPendingItemsCount（不是 tvTotalPendingItems）
                binding.headerShoppingDashboard.root.findViewById<android.widget.TextView>(R.id.tvPendingItemsCount)?.text = stats.totalPendingItems.toString()
                binding.headerShoppingDashboard.root.findViewById<android.widget.TextView>(R.id.tvWeeklyBudget)?.text = "¥${String.format("%.2f", stats.totalBudget)}"
            }
        }
        
        // 观察消息
        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
            }
        }
        
        // 观察错误
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            }
        }
        
        // 观察导航事件
        viewModel.navigateToListDetail.observe(viewLifecycleOwner) { listId ->
            if (listId != null) {
                viewModel.getShoppingListById(listId)?.let { list ->
                    navigateToListDetail(listId, list.name)
                }
                // 🔧 清除导航状态，防止返回时自动重新导航
                viewModel.navigateToListDetailComplete()
            }
        }
    }

    private fun navigateToListDetail(listId: Long, listName: String) {
        // 导航到购物清单详情页面
        try {
            val action = ShoppingListManagementM3FragmentDirections
                .actionNavigationShoppingListManagementToNavigationShoppingList(listId, listName)
            findNavController().navigate(action)
        } catch (e: Exception) {
            android.util.Log.e("ShoppingManagement", "导航失败: listId=$listId, name=$listName", e)
            SnackbarHelper.showError(requireView(), "打开购物清单失败")
        }
    }

    private fun showCreateListDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_shopping_list, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etListName)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etListDescription)
        val chipGroupListType = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupListType)
        val tilCustomType = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilCustomType)
        val etCustomType = dialogView.findViewById<TextInputEditText>(R.id.etCustomType)
        val chipCustom = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chipCustom)
        
        // 监听自定义chip的选中状态
        chipGroupListType.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.contains(R.id.chipCustom)) {
                tilCustomType.visibility = android.view.View.VISIBLE
            } else {
                tilCustomType.visibility = android.view.View.GONE
                etCustomType.text?.clear()
            }
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("创建购物清单")
            .setView(dialogView)
            .setPositiveButton("创建") { _, _ ->
                val name = etName.text.toString().trim()
                val description = etDescription.text.toString().trim()
                
                // 获取选中的清单类型
                val selectedType = when (chipGroupListType.checkedChipId) {
                    R.id.chipDaily -> ShoppingListType.DAILY
                    R.id.chipWeekly -> ShoppingListType.WEEKLY
                    R.id.chipParty -> ShoppingListType.PARTY
                    R.id.chipTravel -> ShoppingListType.TRAVEL
                    R.id.chipSpecial -> ShoppingListType.SPECIAL
                    R.id.chipCustom -> {
                        val customType = etCustomType.text.toString().trim()
                        if (customType.isEmpty()) {
                            Snackbar.make(binding.root, "请输入自定义类型名称", Snackbar.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        ShoppingListType.CUSTOM
                    }
                    else -> ShoppingListType.DAILY
                }
                
                if (name.isNotEmpty()) {
                    viewModel.createShoppingList(
                        name = name,
                        description = description.takeIf { it.isNotEmpty() },
                        type = selectedType
                    )
                } else {
                    Snackbar.make(binding.root, "请输入清单名称", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditListDialog(shoppingList: ShoppingListEntity) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_shopping_list, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etListName)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.etListDescription)
        val chipGroupListType = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupListType)
        val tilCustomType = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilCustomType)
        val etCustomType = dialogView.findViewById<TextInputEditText>(R.id.etCustomType)
        
        // 预填充数据
        etName.setText(shoppingList.name)
        etDescription.setText(shoppingList.description ?: "")
        
        // 预选清单类型
        val chipIdToCheck = when (shoppingList.type) {
            ShoppingListType.DAILY -> R.id.chipDaily
            ShoppingListType.WEEKLY -> R.id.chipWeekly
            ShoppingListType.PARTY -> R.id.chipParty
            ShoppingListType.TRAVEL -> R.id.chipTravel
            ShoppingListType.SPECIAL -> R.id.chipSpecial
            ShoppingListType.CUSTOM -> R.id.chipCustom
        }
        chipGroupListType.check(chipIdToCheck)
        
        // 如果是自定义类型，显示输入框
        if (shoppingList.type == ShoppingListType.CUSTOM) {
            tilCustomType.visibility = android.view.View.VISIBLE
        }
        
        // 监听自定义chip的选中状态
        chipGroupListType.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.contains(R.id.chipCustom)) {
                tilCustomType.visibility = android.view.View.VISIBLE
            } else {
                tilCustomType.visibility = android.view.View.GONE
                etCustomType.text?.clear()
            }
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("编辑购物清单")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val name = etName.text.toString().trim()
                val description = etDescription.text.toString().trim()
                
                // 获取选中的清单类型
                val selectedType = when (chipGroupListType.checkedChipId) {
                    R.id.chipDaily -> ShoppingListType.DAILY
                    R.id.chipWeekly -> ShoppingListType.WEEKLY
                    R.id.chipParty -> ShoppingListType.PARTY
                    R.id.chipTravel -> ShoppingListType.TRAVEL
                    R.id.chipSpecial -> ShoppingListType.SPECIAL
                    R.id.chipCustom -> {
                        val customType = etCustomType.text.toString().trim()
                        if (customType.isEmpty()) {
                            Snackbar.make(binding.root, "请输入自定义类型名称", Snackbar.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        ShoppingListType.CUSTOM
                    }
                    else -> ShoppingListType.DAILY
                }
                
                if (name.isNotEmpty()) {
                    val updatedList = shoppingList.copy(
                        name = name,
                        description = description.takeIf { it.isNotEmpty() },
                        type = selectedType
                    )
                    viewModel.updateShoppingList(updatedList)
                } else {
                    Snackbar.make(binding.root, "请输入清单名称", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDeleteConfirmDialog(listId: Long) {
        viewModel.getShoppingListById(listId)?.let { shoppingList ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("删除购物清单")
                .setMessage("确定要删除「${shoppingList.name}」吗？清单内的所有物品也会被删除。")
                .setPositiveButton("删除") { _, _ ->
                    viewModel.deleteShoppingList(shoppingList)
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 确保底部导航栏隐藏
        hideBottomNavigation()
        
        // ⭐ 刷新概览统计数据（从详情页返回时更新）
        viewModel.loadOverviewStats()
    }
    
    private fun hideBottomNavigation() {
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.GONE
    }
    
    private fun showBottomNavigation() {
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 恢复底部导航栏
        showBottomNavigation()
        _binding = null
    }
}

