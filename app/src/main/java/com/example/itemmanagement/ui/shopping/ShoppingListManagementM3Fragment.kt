package com.example.itemmanagement.ui.shopping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.adapter.ShoppingListManagementAdapter
import com.example.itemmanagement.data.entity.ShoppingListEntity
import com.example.itemmanagement.data.entity.ShoppingListStatus
import com.example.itemmanagement.databinding.FragmentShoppingListManagementM3Binding
// import com.example.itemmanagement.ui.utils.Material3Animations
import com.example.itemmanagement.ui.utils.Material3Feedback
// import com.example.itemmanagement.ui.utils.showWithAnimation
// import com.example.itemmanagement.ui.utils.fadeIn
// import com.example.itemmanagement.ui.utils.animatePress
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.example.itemmanagement.adapter.ShoppingListProgress
import kotlinx.coroutines.runBlocking
import android.widget.TextView

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
        _binding = FragmentShoppingListManagementM3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupUI()
        setupAnimations()
        setupClickListeners()
        observeViewModel()
        
        // 加载概览统计数据
        viewModel.loadOverviewStats()
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
                    ShoppingListStatus.ACTIVE -> {
                        viewModel.completeShoppingList(shoppingList)
                    }
                    else -> {
                        viewModel.reactivateShoppingList(shoppingList)
                    }
                }
            },
            getProgressData = { listId ->
                // 获取真实进度数据
                runBlocking {
                    viewModel.getShoppingListProgress(listId)
                }
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@ShoppingListManagementM3Fragment.adapter
        }
    }

    private fun setupUI() {
        // 隐藏进度条
        binding.progressBar.visibility = View.GONE
    }

    private fun setupAnimations() {
        // FAB显示动画
        // binding.fabAddList.showWithAnimation(300)
        
        // 内容淡入动画
        // binding.root.fadeIn(100)
    }

    private fun setupClickListeners() {
        // 创建购物清单
        binding.fabAddList.setOnClickListener {
            // it.animatePress()
            showCreateListDialog()
        }
        
        // 刷新按钮
        try {
            val btnRefresh = binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnRefresh)
            btnRefresh?.setOnClickListener { button ->
                // button.animatePress()
                viewModel.loadOverviewStats()
                Material3Feedback.showInfo(binding.root, "数据已刷新")
            }
        } catch (e: Exception) {
            // 如果找不到刷新按钮，不影响其他功能
        }
    }

    private fun observeViewModel() {
        // 观察购物清单列表
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
                Material3Feedback.showSuccess(binding.root, message)
                viewModel.clearMessage()
            }
        }

        // 观察错误
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error.isNotEmpty()) {
                Material3Feedback.showError(binding.root, error)
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
        
        // 观察概览统计数据
        viewModel.overviewStats.observe(viewLifecycleOwner) { stats ->
            updateOverviewStats(stats)
        }
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyView?.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyView?.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }
    
    private fun updateOverviewStats(stats: com.example.itemmanagement.ui.shopping.ShoppingOverviewStats) {
        // 更新概览仪表板的真实数据
        try {
            // 活跃清单数
            val tvActiveListsCount = binding.root.findViewById<TextView>(R.id.tvActiveListsCount)
            tvActiveListsCount?.text = stats.activeListsCount.toString()
            
            // 待购物品数
            val tvPendingItemsCount = binding.root.findViewById<TextView>(R.id.tvPendingItemsCount)
            tvPendingItemsCount?.text = stats.totalPendingItems.toString()
            
            // 本周预算
            val tvWeeklyBudget = binding.root.findViewById<TextView>(R.id.tvWeeklyBudget)
            val budgetText = if (stats.totalBudget > 0) {
                "¥${String.format("%.0f", stats.totalBudget)}"
            } else {
                "¥0"
            }
            tvWeeklyBudget?.text = budgetText
            
        } catch (e: Exception) {
            // 如果更新失败，不影响其他功能
        }
    }

    private fun showCreateListDialog() {
        val dialogView = LayoutInflater.from(context).inflate(
            R.layout.dialog_create_shopping_list, null
        )
        
        val nameEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            R.id.etListName
        )
        val descriptionEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            R.id.etListDescription
        )
        val budgetEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            R.id.etListBudget
        )
        val chipGroupType = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(
            R.id.chipGroupListType
        )
        val tilCustomType = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(
            R.id.tilCustomType
        )
        val etCustomType = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            R.id.etCustomType
        )
        val chipCustom = dialogView.findViewById<com.google.android.material.chip.Chip>(
            R.id.chipCustom
        )

        // 设置自定义标签的显示逻辑
        chipGroupType.setOnCheckedStateChangeListener { _, checkedIds ->
            val isCustomSelected = checkedIds.contains(R.id.chipCustom)
            tilCustomType.visibility = if (isCustomSelected) View.VISIBLE else View.GONE
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("创建购物清单")
            .setView(dialogView)
            .setPositiveButton("创建") { _, _ ->
                val name = nameEditText.text.toString()
                val description = descriptionEditText.text.toString()
                val budgetText = budgetEditText.text.toString()
                val budget = if (budgetText.isNotEmpty()) budgetText.toDoubleOrNull() else null
                
                // 获取选中的类型
                val selectedType = getSelectedShoppingListType(chipGroupType, etCustomType)

                if (name.isNotEmpty()) {
                    viewModel.createShoppingList(name, description, selectedType, estimatedBudget = budget)
                } else {
                    Material3Feedback.showError(binding.root, "请输入清单名称")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun getSelectedShoppingListType(
        chipGroup: com.google.android.material.chip.ChipGroup,
        customTypeEditText: com.google.android.material.textfield.TextInputEditText
    ): com.example.itemmanagement.data.entity.ShoppingListType {
        return when (chipGroup.checkedChipId) {
            R.id.chipDaily -> com.example.itemmanagement.data.entity.ShoppingListType.DAILY
            R.id.chipWeekly -> com.example.itemmanagement.data.entity.ShoppingListType.WEEKLY
            R.id.chipParty -> com.example.itemmanagement.data.entity.ShoppingListType.PARTY
            R.id.chipTravel -> com.example.itemmanagement.data.entity.ShoppingListType.TRAVEL
            R.id.chipSpecial -> com.example.itemmanagement.data.entity.ShoppingListType.SPECIAL
            R.id.chipCustom -> {
                // 对于自定义类型，可以扩展枚举或使用CUSTOM类型
                com.example.itemmanagement.data.entity.ShoppingListType.CUSTOM
            }
            else -> com.example.itemmanagement.data.entity.ShoppingListType.DAILY
        }
    }

    private fun showEditListDialog(shoppingList: ShoppingListEntity) {
        val dialogView = LayoutInflater.from(context).inflate(
            R.layout.dialog_create_shopping_list, null
        )
        
        val nameEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            R.id.etListName
        )
        val descriptionEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            R.id.etListDescription
        )
        val budgetEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(
            R.id.etListBudget
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
                    viewModel.updateShoppingList(
                        shoppingList.copy(
                            name = name,
                            description = description,
                            estimatedBudget = budget
                        )
                    )
                } else {
                    Material3Feedback.showError(binding.root, "请输入清单名称")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDeleteConfirmDialog(shoppingList: ShoppingListEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除购物清单")
            .setMessage("确定要删除购物清单 \"${shoppingList.name}\" 吗？此操作不可撤销。")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteShoppingList(shoppingList)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun navigateToListDetail(listId: Long) {
        // 正确传递购物清单ID和名称参数
        val bundle = bundleOf(
            "listId" to listId,
            "listName" to "购物清单 #$listId"
        )
        findNavController().navigate(
            R.id.action_navigation_shopping_list_management_to_navigation_shopping_list,
            bundle
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
