package com.example.itemmanagement.ui.wishlist

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentWishlistMainBinding
import com.example.itemmanagement.data.repository.WishlistRepository
import com.example.itemmanagement.ui.utils.Material3Feedback
import com.example.itemmanagement.ui.wishlist.adapter.WishlistAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/**
 * 心愿单主界面Fragment
 * 遵循MVVM架构和Material Design 3规范
 */
class WishlistMainFragment : Fragment(), MenuProvider {
    
    private var _binding: FragmentWishlistMainBinding? = null
    private val binding get() = _binding!!
    
    // 使用工厂模式创建ViewModel
    private val viewModel: WishlistViewModel by viewModels {
        val app = requireActivity().application as ItemManagementApplication
        val database = app.database
        val wishlistRepository = WishlistRepository(
            database = database,
            wishlistDao = database.wishlistDao(),
            priceHistoryDao = database.wishlistPriceHistoryDao()
        )
        WishlistViewModelFactory(wishlistRepository)
    }
    
    private lateinit var wishlistAdapter: WishlistAdapter
    private var searchView: SearchView? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWishlistMainBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 添加菜单提供器
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        
        setupUI()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }
    
    private fun setupUI() {
        // 设置Material 3 样式的下拉刷新
        binding.swipeRefresh.setColorSchemeResources(
            R.color.primary,
            R.color.secondary,
            R.color.tertiary
        )
        
        // 设置空状态视图
        setupEmptyState()
    }
    
    private fun setupRecyclerView() {
        wishlistAdapter = WishlistAdapter(
            onItemClick = { item ->
                viewModel.navigateToItemDetail(item.id)
            },
            onItemLongClick = { item ->
                if (viewModel.isSelectionMode.value != true) {
                    viewModel.toggleSelectionMode()
                }
                viewModel.toggleItemSelection(item.id)
                true
            },
            onPriorityClick = { item ->
                showPrioritySelectionDialog(item.id, item.priority)
            },
            onPriceClick = { item ->
                showPriceUpdateDialog(item.id, item.currentPrice)
            },
            onSelectionChanged = { itemId, isSelected ->
                if (isSelected) {
                    viewModel.toggleItemSelection(itemId)
                }
            }
        )
        
        binding.recyclerViewWishlist.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = wishlistAdapter
            
            // 添加滑动删除功能
            val itemTouchHelper = ItemTouchHelper(WishlistItemTouchHelper())
            itemTouchHelper.attachToRecyclerView(this)
        }
    }
    
    private fun setupObservers() {
        // 观察心愿单物品数据
        viewModel.wishlistItems.observe(viewLifecycleOwner) { items ->
            wishlistAdapter.submitList(items)
            updateEmptyState(items.isEmpty())
        }
        
        // 观察统计信息
        viewModel.wishlistStats.observe(viewLifecycleOwner) { stats ->
            updateStatsView(stats)
        }
        
        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }
        
        // 观察错误消息
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                showErrorSnackbar(it)
                viewModel.clearErrorMessage()
            }
        }
        
        // 观察成功消息
        viewModel.successMessage.observe(viewLifecycleOwner) { successMessage ->
            successMessage?.let {
                showSuccessSnackbar(it)
                viewModel.clearSuccessMessage()
            }
        }
        
        // 观察选择模式
        viewModel.isSelectionMode.observe(viewLifecycleOwner) { isSelectionMode ->
            wishlistAdapter.setSelectionMode(isSelectionMode)
            updateSelectionModeUI(isSelectionMode)
        }
        
        // 观察选中的物品
        viewModel.selectedItems.observe(viewLifecycleOwner) { selectedItems ->
            wishlistAdapter.setSelectedItems(selectedItems)
        }
        
        // 观察导航事件
        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            event?.let {
                handleNavigationEvent(it)
                viewModel.onNavigationEventConsumed()
            }
        }
    }
    
    private fun setupClickListeners() {
        // 下拉刷新
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshStats()
        }
        
        // 添加心愿单物品按钮
        binding.fabAddWishlist.setOnClickListener {
            Material3Feedback.performHapticFeedback(it)
            viewModel.navigateToAddItem()
        }
        
    }
    
    // === UI更新方法 ===
    
    private fun updateStatsView(stats: com.example.itemmanagement.data.model.wishlist.WishlistStats) {
        binding.apply {
            textTotalItems.text = stats.totalItems.toString()
            textTotalValue.text = "¥%.0f".format(stats.totalCurrentValue)
            textPriceAlerts.text = stats.priceDroppedItems.toString()
            
            // 更新预算使用率
            val budgetUtilization = stats.getBudgetUtilization()
            progressBudget.progress = budgetUtilization.toInt()
            textBudgetPercentage.text = "%.1f%%".format(budgetUtilization)
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.apply {
            if (isEmpty) {
                recyclerViewWishlist.visibility = View.GONE
                layoutEmptyState.visibility = View.VISIBLE
            } else {
                recyclerViewWishlist.visibility = View.VISIBLE
                layoutEmptyState.visibility = View.GONE
            }
        }
    }
    
    private fun updateSelectionModeUI(isSelectionMode: Boolean) {
        binding.apply {
            if (isSelectionMode) {
                layoutSelectionActions.visibility = View.VISIBLE
                fabAddWishlist.hide()
                
                // 设置选择模式下的操作按钮
                buttonBatchDelete.setOnClickListener {
                    showBatchDeleteDialog()
                }
                
                buttonBatchPriority.setOnClickListener {
                    showBatchPriorityDialog()
                }
                
                buttonCancelSelection.setOnClickListener {
                    viewModel.toggleSelectionMode()
                }
                
            } else {
                layoutSelectionActions.visibility = View.GONE
                fabAddWishlist.show()
            }
        }
    }
    
    private fun setupEmptyState() {
        binding.apply {
            buttonAddFirstItem.setOnClickListener {
                viewModel.navigateToAddItem()
            }
            
            buttonImportFromShopping.setOnClickListener {
                // TODO: 实现从购物清单导入功能
                showSuccessSnackbar("功能开发中...")
            }
        }
    }
    
    // === 对话框方法 ===
    
    private fun showPrioritySelectionDialog(itemId: Long, currentPriority: com.example.itemmanagement.data.entity.wishlist.WishlistPriority) {
        val priorities = com.example.itemmanagement.data.entity.wishlist.WishlistPriority.values()
        val priorityNames = priorities.map { it.displayName }.toTypedArray()
        val currentIndex = priorities.indexOf(currentPriority)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择优先级")
            .setSingleChoiceItems(priorityNames, currentIndex) { dialog, which ->
                viewModel.updatePriority(itemId, priorities[which])
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showPriceUpdateDialog(itemId: Long, currentPrice: Double?) {
        // TODO: 实现价格更新对话框
        showSuccessSnackbar("价格更新功能开发中...")
    }
    
    private fun showFilterDialog() {
        // TODO: 实现筛选对话框
        showSuccessSnackbar("筛选功能开发中...")
    }
    
    private fun showSortDialog() {
        val sortOrders = WishlistSortOrder.values()
        val sortNames = sortOrders.map { it.displayName }.toTypedArray()
        val currentOrder = viewModel.sortOrder.value ?: WishlistSortOrder.PRIORITY_DESC
        val currentIndex = sortOrders.indexOf(currentOrder)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择排序方式")
            .setSingleChoiceItems(sortNames, currentIndex) { dialog, which ->
                viewModel.setSortOrder(sortOrders[which])
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showBatchDeleteDialog() {
        val selectedCount = viewModel.selectedItemsCount.value ?: 0
        if (selectedCount == 0) return
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("批量删除")
            .setMessage("确定要删除选中的 $selectedCount 个心愿单物品吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.batchDelete()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showBatchPriorityDialog() {
        val priorities = com.example.itemmanagement.data.entity.wishlist.WishlistPriority.values()
        val priorityNames = priorities.map { it.displayName }.toTypedArray()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("批量设置优先级")
            .setItems(priorityNames) { _, which ->
                viewModel.batchUpdatePriority(priorities[which])
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    // === 消息显示方法 ===
    
    private fun showErrorSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(requireContext().getColor(R.color.error))
            .setTextColor(requireContext().getColor(R.color.white))
            .show()
    }
    
    private fun showSuccessSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(requireContext().getColor(R.color.primary))
            .setTextColor(requireContext().getColor(R.color.on_primary))
            .show()
    }
    
    // === 导航处理 ===
    
    private fun handleNavigationEvent(event: WishlistNavigationEvent) {
        when (event) {
            is WishlistNavigationEvent.AddItem -> {
                // 导航到添加心愿单物品页面
                findNavController().navigate(R.id.action_wishlist_to_add)
            }
            is WishlistNavigationEvent.ItemDetail -> {
                // 导航到心愿单物品详情页面
                findNavController().navigate(
                    R.id.action_wishlist_to_detail,
                    Bundle().apply {
                        putLong("itemId", event.itemId)
                    }
                )
            }
            is WishlistNavigationEvent.Settings -> {
                // TODO: 导航到设置页面
                showSuccessSnackbar("设置页面开发中...")
            }
        }
    }
    
    // === 菜单处理 ===
    
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_wishlist_main, menu)
        
        // 设置搜索视图
        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem.actionView as SearchView
        searchView?.apply {
            queryHint = "搜索心愿单物品..."
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }
                
                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.setSearchQuery(newText ?: "")
                    return true
                }
            })
        }
    }
    
    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_settings -> {
                viewModel.navigateToSettings()
                true
            }
            R.id.action_select_all -> {
                if (viewModel.isSelectionMode.value == true) {
                    viewModel.toggleSelectAll()
                } else {
                    viewModel.toggleSelectionMode()
                }
                true
            }
            else -> false
        }
    }
    
    // === 滑动删除处理 ===
    
    private inner class WishlistItemTouchHelper : ItemTouchHelper.SimpleCallback(
        0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
    ) {
        override fun onMove(
            recyclerView: androidx.recyclerview.widget.RecyclerView,
            viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
            target: androidx.recyclerview.widget.RecyclerView.ViewHolder
        ): Boolean = false
        
        override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val item = wishlistAdapter.currentList[position]
            
            when (direction) {
                ItemTouchHelper.LEFT -> {
                    // 左滑删除
                    viewModel.deleteWishlistItem(item.id)
                }
                ItemTouchHelper.RIGHT -> {
                    // 右滑标记为已实现
                    viewModel.markAsAchieved(item.id)
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
