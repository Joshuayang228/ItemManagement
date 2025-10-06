package com.example.itemmanagement.ui.shopping

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.adapter.ShoppingListAdapter
import com.example.itemmanagement.data.model.Item
import com.example.itemmanagement.databinding.FragmentShoppingListBinding
import com.google.android.material.snackbar.Snackbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 购物清单Fragment
 * 显示单个购物清单的所有物品
 */
class ShoppingListFragment : Fragment() {

    private var _binding: FragmentShoppingListBinding? = null
    private val binding get() = _binding!!
    
    private var listId: Long = 1L
    private var listName: String = "购物清单"
    
    private val viewModel: ShoppingListViewModel by viewModels {
        ShoppingListViewModelFactory(
            (requireActivity().application as ItemManagementApplication).repository,
            listId
        )
    }
    
    private lateinit var shoppingListAdapter: ShoppingListAdapter
    
    // 搜索和排序相关
    private var currentSearchQuery: String = ""
    private var currentSortType: SortType = SortType.COMPREHENSIVE
    private var currentSortDirection: SortDirection = SortDirection.DESC
    
    enum class SortType {
        COMPREHENSIVE,  // 综合
        IMPORTANCE,     // 重要性
        URGENCY,        // 紧急性
        PRICE,          // 价格
        QUANTITY        // 数量
    }
    
    enum class SortDirection {
        ASC,   // 升序 ↑
        DESC   // 降序 ↓
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu, inflater: android.view.MenuInflater) {
        inflater.inflate(R.menu.menu_shopping_list, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_purchased -> {
                showClearPurchasedDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

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
        
        setupRecyclerView()
        setupSearchAndSort()
        setupObservers()
        setupActions()
        
        // 隐藏底部导航栏
        hideBottomNavigation()
    }
    
    override fun onResume() {
        super.onResume()
        // 确保底部导航栏隐藏
        hideBottomNavigation()
    }
    
    private fun hideBottomNavigation() {
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.GONE
    }
    
    private fun showBottomNavigation() {
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.VISIBLE
    }
    
    private fun setupRecyclerView() {
        shoppingListAdapter = ShoppingListAdapter(
            onItemClick = { item ->
                // 导航到购物物品详情页
                try {
                    val action = ShoppingListFragmentDirections
                        .actionShoppingListToShoppingItemDetail(
                            itemId = item.id,
                            listId = listId,
                            listName = listName
                        )
                    findNavController().navigate(action)
                } catch (e: Exception) {
                    android.util.Log.e("ShoppingList", "导航到详情页失败", e)
                    android.widget.Toast.makeText(
                        requireContext(),
                        "打开详情页失败",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onRecordPrice = { item ->
                // 显示记录价格对话框
                showRecordPriceDialog(item)
            },
            onMarkPurchased = { item ->
                // 切换购买状态
                val newStatus = !item.shoppingDetail!!.isPurchased
                viewModel.toggleItemPurchaseStatus(item, newStatus)
                val message = if (newStatus) "已标记为已购买" else "已取消购买标记"
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
            },
            onTransferToInventory = { item ->
                // 转入库存
                showTransferToInventoryDialog(item)
            },
            onDelete = { item ->
                // 删除
                showDeleteConfirmDialog(item)
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = shoppingListAdapter
            
            // 添加分割线装饰器（在已购买和未购买之间）
            addItemDecoration(PurchasedDividerDecoration(requireContext(), shoppingListAdapter))
        }
    }
    
    /**
     * 显示转入库存半屏对话框（使用 BottomSheetDialogFragment）
     */
    private fun showTransferToInventoryDialog(item: com.example.itemmanagement.data.model.Item) {
        val dialog = TransferToInventoryFragment.newInstance(item.id)
        dialog.show(childFragmentManager, "TransferToInventory")
    }

    private fun setupObservers() {
        // TODO: 添加 ProgressBar 到布局后取消注释
        // viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
        //     binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        // }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                viewModel.onErrorHandled()
            }
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                viewModel.onMessageShown()
            }
        }

        viewModel.shoppingItems.observe(viewLifecycleOwner) { items ->
            applySearchAndSort(items)
        }
    }
    
    /**
     * 设置搜索和排序功能
     */
    private fun setupSearchAndSort() {
        // 搜索框监听
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentSearchQuery = s?.toString() ?: ""
                binding.clearSearchIcon.visibility = if (currentSearchQuery.isEmpty()) View.GONE else View.VISIBLE
                applySearchAndSort(viewModel.shoppingItems.value ?: emptyList())
            }
        })
        
        // 清除搜索按钮
        binding.clearSearchIcon.setOnClickListener {
            binding.searchEditText.text.clear()
        }
        
        // 排序按钮点击事件
        binding.sortComprehensive.setOnClickListener {
            setSortType(SortType.COMPREHENSIVE, binding.sortComprehensive)
        }
        binding.sortImportance.setOnClickListener {
            setSortType(SortType.IMPORTANCE, binding.sortImportance)
        }
        binding.sortUrgency.setOnClickListener {
            setSortType(SortType.URGENCY, binding.sortUrgency)
        }
        binding.sortPrice.setOnClickListener {
            setSortType(SortType.PRICE, binding.sortPrice)
        }
        binding.sortQuantity.setOnClickListener {
            setSortType(SortType.QUANTITY, binding.sortQuantity)
        }
        
        // 初始化选中状态（直接更新UI，不触发排序逻辑）
        updateSortButtonsUI()
    }
    
    /**
     * 设置排序类型
     */
    private fun setSortType(sortType: SortType, selectedView: TextView) {
        // 如果点击的是当前已选中的排序选项，切换排序方向
        if (currentSortType == sortType) {
            currentSortDirection = if (currentSortDirection == SortDirection.ASC) {
                SortDirection.DESC
            } else {
                SortDirection.ASC
            }
        } else {
            // 如果是新的排序选项，设置为默认排序方向
            currentSortType = sortType
            currentSortDirection = when (sortType) {
                SortType.COMPREHENSIVE -> SortDirection.DESC  // 综合默认降序（高优先级在前）
                SortType.IMPORTANCE -> SortDirection.DESC     // 重要性默认降序
                SortType.URGENCY -> SortDirection.DESC        // 紧急性默认降序
                SortType.PRICE -> SortDirection.DESC          // 价格默认降序（贵的在前）
                SortType.QUANTITY -> SortDirection.DESC       // 数量默认降序（多的在前）
            }
        }
        
        // 更新UI
        updateSortButtonsUI()
        
        // 应用新的排序
        applySearchAndSort(viewModel.shoppingItems.value ?: emptyList())
    }
    
    /**
     * 更新排序按钮的UI显示
     */
    private fun updateSortButtonsUI() {
        val arrow = if (currentSortDirection == SortDirection.ASC) "↑" else "↓"
        listOf(
            binding.sortComprehensive to SortType.COMPREHENSIVE,
            binding.sortImportance to SortType.IMPORTANCE,
            binding.sortUrgency to SortType.URGENCY,
            binding.sortPrice to SortType.PRICE,
            binding.sortQuantity to SortType.QUANTITY
        ).forEach { (textView, type) ->
            val name = when (type) {
                SortType.COMPREHENSIVE -> "综合"
                SortType.IMPORTANCE -> "重要性"
                SortType.URGENCY -> "紧急性"
                SortType.PRICE -> "价格"
                SortType.QUANTITY -> "数量"
            }
            
            if (type == currentSortType) {
                textView.text = "$name $arrow"
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.sort_text_selected))
            } else {
                textView.text = name
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.sort_text_normal))
            }
        }
    }
    
    /**
     * 应用搜索和排序
     */
    private fun applySearchAndSort(items: List<Item>) {
        var filteredItems = items
        
        // 1. 应用搜索过滤
        if (currentSearchQuery.isNotEmpty()) {
            filteredItems = items.filter { item ->
                item.name.contains(currentSearchQuery, ignoreCase = true) ||
                item.customNote?.contains(currentSearchQuery, ignoreCase = true) == true ||
                item.shoppingDetail?.storeName?.contains(currentSearchQuery, ignoreCase = true) == true
            }
        }
        
        // 2. 分离已购买和未购买的物品
        val unpurchasedItems = filteredItems.filter { it.shoppingDetail?.isPurchased != true }
        val purchasedItems = filteredItems.filter { it.shoppingDetail?.isPurchased == true }
        
        // 3. 分别对未购买和已购买的物品进行排序
        val sortedUnpurchased = sortItemsByType(unpurchasedItems)
        val sortedPurchased = sortItemsByType(purchasedItems)
        
        // 4. 合并列表：未购买在前，已购买在后
        val sortedItems = sortedUnpurchased + sortedPurchased
        
        // 5. 更新列表
        shoppingListAdapter.submitList(sortedItems)
        
        // 6. 更新分割线位置
        shoppingListAdapter.setPurchasedStartPosition(sortedUnpurchased.size)
        
        // 7. 更新空状态
        if (sortedItems.isEmpty()) {
                binding.emptyStateView.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyStateView.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }
        
    /**
     * 根据当前排序类型和方向对物品进行排序
     */
    private fun sortItemsByType(items: List<Item>): List<Item> {
        return when (currentSortType) {
            SortType.COMPREHENSIVE -> {
                // 综合排序：综合优先级（紧急性+重要性）
                if (currentSortDirection == SortDirection.DESC) {
                    items.sortedByDescending { it.shoppingDetail?.getOverallPriority() ?: 0 }
                } else {
                    items.sortedBy { it.shoppingDetail?.getOverallPriority() ?: 0 }
                }
            }
            SortType.IMPORTANCE -> {
                // 重要性排序
                if (currentSortDirection == SortDirection.DESC) {
                    items.sortedByDescending { it.shoppingDetail?.getOverallPriority() ?: 0 }
                } else {
                    items.sortedBy { it.shoppingDetail?.getOverallPriority() ?: 0 }
                }
            }
            SortType.URGENCY -> {
                // 紧急性排序
                if (currentSortDirection == SortDirection.DESC) {
                    items.sortedByDescending { it.shoppingDetail?.urgencyLevel?.level ?: 0 }
                } else {
                    items.sortedBy { it.shoppingDetail?.urgencyLevel?.level ?: 0 }
                }
            }
            SortType.PRICE -> {
                // 价格排序
                if (currentSortDirection == SortDirection.DESC) {
                    items.sortedByDescending { it.shoppingDetail?.estimatedPrice ?: 0.0 }
                } else {
                    items.sortedBy { it.shoppingDetail?.estimatedPrice ?: 0.0 }
                }
            }
            SortType.QUANTITY -> {
                // 数量排序
                if (currentSortDirection == SortDirection.DESC) {
                    items.sortedByDescending { it.shoppingDetail?.quantity ?: 0.0 }
                } else {
                    items.sortedBy { it.shoppingDetail?.quantity ?: 0.0 }
                }
            }
        }
    }

    /**
     * 显示记录价格对话框（完整版）
     */
    private fun showRecordPriceDialog(item: com.example.itemmanagement.data.model.Item) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_record_price, null)
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)
        
        // 初始化视图
        val itemNameText = dialogView.findViewById<TextView>(R.id.itemNameText)
        val priceInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.priceInput)
        val priceUnitInput = dialogView.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.priceUnitInput)
        val channelInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.channelInput)
        val dateInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.dateInput)
        val notesInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.notesInput)
        val btnSave = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave)
        
        // 设置物品名称
        itemNameText.text = item.name
        
        // 预填充价格（如果有预估价格）
        item.shoppingDetail?.estimatedPrice?.let { price ->
            priceInput.setText(price.toInt().toString())
        }
        
        // 价格单位选项（与库存字段单价单位一致）
        val priceUnits = arrayOf("元", "美元", "日元", "欧元")
        val unitAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, priceUnits)
        priceUnitInput?.setAdapter(unitAdapter)
        priceUnitInput?.setText("元", false)
        
        // Chip点击事件：填充到自定义输入框
        val chipJD = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.channelJD)
        val chipTmall = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.channelTmall)
        val chipPDD = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.channelPDD)
        val chipStore = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.channelStore)
        
        chipJD?.setOnClickListener { channelInput?.setText("京东") }
        chipTmall?.setOnClickListener { channelInput?.setText("天猫") }
        chipPDD?.setOnClickListener { channelInput?.setText("拼多多") }
        chipStore?.setOnClickListener { channelInput?.setText("实体店") }
        
        // 日期选择器
        var selectedDate = java.util.Date()
        val dateFormatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        dateInput?.setText(dateFormatter.format(selectedDate))
        dateInput?.setOnClickListener {
            val datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                .setTitleText("选择日期")
                .setSelection(selectedDate.time)
                .build()
            
            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedDate = java.util.Date(selection)
                dateInput.setText(dateFormatter.format(selectedDate))
            }
            
            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }
        
        // 保存按钮点击事件
        btnSave.setOnClickListener {
            val priceText = priceInput?.text?.toString()
            if (priceText.isNullOrBlank()) {
                priceInput?.error = "请输入价格"
                return@setOnClickListener
            }
            
            val price = priceText.toDoubleOrNull()
            if (price == null || price <= 0) {
                priceInput?.error = "请输入有效的价格"
                return@setOnClickListener
            }
            
            // 获取渠道（优先使用自定义输入）
            val channel = channelInput?.text?.toString()?.trim()?.takeIf { it.isNotBlank() } ?: "其他"
            
            // 获取备注
            val notes = notesInput?.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
            
            // 创建价格记录
            val priceRecord = com.example.itemmanagement.data.entity.PriceRecord(
                itemId = item.id,
                recordDate = selectedDate,
                price = price,
                purchaseChannel = channel,
                notes = notes
            )
            
            // 保存到数据库
            lifecycleScope.launch {
                try {
                    val repository = (requireActivity().application as com.example.itemmanagement.ItemManagementApplication).repository
                    repository.addPriceRecord(priceRecord)
                    
                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                        Snackbar.make(binding.root, "价格已记录", Snackbar.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Snackbar.make(binding.root, "记录失败：${e.message}", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        dialog.show()
    }

    /**
     * 显示删除确认对话框（M3风格）
     * 防止用户误操作
     */
    private fun showDeleteConfirmDialog(item: com.example.itemmanagement.data.model.Item) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除物品")
            .setMessage("确定要删除「${item.name}」吗？\n\n此操作将把物品移至回收站，可在回收站中恢复。")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteShoppingItem(item)
                Snackbar.make(binding.root, "已删除「${item.name}」", Snackbar.LENGTH_SHORT)
                    .setAction("撤销") {
                        // TODO: 实现撤销删除功能
                    }
                    .show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun setupActions() {
        // 添加购物物品悬浮按钮
        binding.fabAddItem.setOnClickListener {
            // 导航到添加购物物品页面
            try {
                val action = ShoppingListFragmentDirections
                    .actionShoppingListToAddShoppingItem(listId, listName)
                findNavController().navigate(action)
            } catch (e: Exception) {
                android.util.Log.e("ShoppingList", "添加购物物品导航失败: listId=$listId", e)
                android.widget.Toast.makeText(requireContext(), "打开添加页面失败", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showClearPurchasedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("清除已购买物品")
            .setMessage("确定要清除所有已购买的物品吗？")
            .setPositiveButton("确定") { _, _ ->
                viewModel.clearPurchasedItems()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 恢复底部导航栏
        showBottomNavigation()
        _binding = null
    }
}

