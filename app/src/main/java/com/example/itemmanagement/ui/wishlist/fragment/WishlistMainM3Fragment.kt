package com.example.itemmanagement.ui.wishlist.fragment

import android.os.Bundle
import android.view.*
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import android.widget.Toast
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.data.repository.WishlistRepository
import com.example.itemmanagement.databinding.FragmentWishlistMainM3Binding
import com.example.itemmanagement.test.WishlistTestDataInserter
import com.example.itemmanagement.ui.utils.Material3Feedback
// import com.example.itemmanagement.ui.utils.Material3Animations.fadeIn
// import com.example.itemmanagement.ui.utils.Material3Animations.animatePress
import com.example.itemmanagement.ui.wishlist.adapter.WishlistM3Adapter
import com.example.itemmanagement.ui.wishlist.model.*
import com.example.itemmanagement.ui.wishlist.viewmodel.*
import kotlinx.coroutines.launch

/**
 * 心愿单主界面Fragment - Material Design 3版本
 * 遵循MVVM架构，使用多个专门的ViewModel管理不同职责
 * 采用最新的Material Design 3设计规范
 */
class WishlistMainM3Fragment : Fragment(), MenuProvider {
    
    private var _binding: FragmentWishlistMainM3Binding? = null
    private val binding get() = _binding!!
    
    // === ViewModel实例 ===
    
    // 主要的业务逻辑ViewModel
    private val mainViewModel: WishlistMainViewModel by viewModels {
        createViewModelFactory()
    }
    
    // 选择模式专用ViewModel
    private val selectionViewModel: WishlistSelectionViewModel by viewModels {
        createViewModelFactory()
    }
    
    // === 适配器 ===
    
    private lateinit var wishlistAdapter: WishlistM3Adapter
    
    // === 状态变量 ===
    
    private var currentDisplayMode = WishlistDisplayMode.LIST
    private var searchView: androidx.appcompat.widget.SearchView? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWishlistMainM3Binding.inflate(inflater, container, false)
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
        
        // Material 3 进入动画
        // view.fadeIn(200)
    }
    
    // === UI设置 ===
    
    private fun setupUI() {
        // 设置Material 3下拉刷新样式
        with(binding.swipeRefresh) {
            setColorSchemeResources(
                R.color.primary,
                R.color.secondary,
                R.color.tertiary
            )
            setProgressBackgroundColorSchemeResource(R.color.surface)
            setOnRefreshListener {
                mainViewModel.refreshData()
            }
        }
        
        // 设置空状态视图
        setupEmptyState()
        
        // 设置统计卡片
        setupStatsCards()
    }
    
    private fun setupEmptyState() {
        binding.textEmptyTitle.text = "还没有心愿单物品"
        binding.textEmptyMessage.text = "添加您想要购买的物品，我们会为您跟踪价格变化"
        binding.buttonEmptyAction.text = "添加第一个心愿"
        binding.buttonEmptyAction.setOnClickListener {
            // it.animatePress()
            mainViewModel.navigateToAddItem()
        }
    }
    
    private fun setupStatsCards() {
        // 统计信息将通过Observer更新
    }
    
    private fun setupRecyclerView() {
        wishlistAdapter = WishlistM3Adapter(
            onItemClick = { itemId ->
                mainViewModel.navigateToItemDetail(itemId)
            },
            onItemLongClick = { itemId ->
                if (!selectionViewModel.isSelectionMode.value!!) {
                    selectionViewModel.startSelectionMode()
                }
                selectionViewModel.toggleItemSelection(itemId)
                true
            },
            onPriorityClick = { itemId, priority ->
                mainViewModel.quickUpdatePriority(itemId, priority)
            },
            onPriceClick = { itemId, currentPrice ->
                // TODO: 显示价格编辑对话框
                showPriceUpdateDialog(itemId, currentPrice)
            },
            onSelectionChanged = { itemId, isSelected ->
                if (isSelected) {
                    selectionViewModel.selectItem(itemId)
                } else {
                    selectionViewModel.deselectItem(itemId)
                }
            },
            onDeleteItem = { itemId ->
                deleteWishlistItem(itemId)
            },
            onMarkPurchased = { itemId ->
                markItemAsPurchased(itemId)
            }
        )
        
        updateRecyclerViewLayout()
    }
    
    private fun updateRecyclerViewLayout() {
        binding.recyclerViewWishlist.apply {
            layoutManager = when (currentDisplayMode) {
                WishlistDisplayMode.LIST -> LinearLayoutManager(requireContext())
                WishlistDisplayMode.GRID -> StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
                WishlistDisplayMode.COMPACT -> LinearLayoutManager(requireContext())
            }
            adapter = wishlistAdapter
        }
    }
    
    private fun setupClickListeners() {
        with(binding) {
            // 添加按钮
            fabAdd.setOnClickListener {
                // it.animatePress()
                mainViewModel.navigateToAddItem()
            }
            
            // 长按添加按钮生成心愿单测试数据
            fabAdd.setOnLongClickListener {
                insertWishlistTestData()
                true
            }
            
            // 筛选芯片点击处理
            chipUnpurchased.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    chipPurchased.isChecked = false
                    applyPurchaseStatusFilter(purchaseStatus = "unpurchased")
                } else if (!chipPurchased.isChecked) {
                    applyPurchaseStatusFilter(purchaseStatus = "all")
                }
            }
            
            chipPurchased.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    chipUnpurchased.isChecked = false
                    applyPurchaseStatusFilter(purchaseStatus = "purchased")
                } else if (!chipUnpurchased.isChecked) {
                    applyPurchaseStatusFilter(purchaseStatus = "all")
                }
            }
            
        }
    }
    
    // === 观察者设置 ===
    
    private fun setupObservers() {
        observeMainViewModel()
        observeSelectionViewModel()
    }
    
    private fun observeMainViewModel() {
        // UI状态观察
        mainViewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is WishlistUiState.Loading -> {
                    binding.swipeRefresh.isRefreshing = true
                    hideError()
                }
                is WishlistUiState.Success -> {
                    binding.swipeRefresh.isRefreshing = false
                    hideError()
                }
                is WishlistUiState.Error -> {
                    binding.swipeRefresh.isRefreshing = false
                    showError(state.message)
                }
            }
        }
        
        // 心愿单物品列表观察
        mainViewModel.wishlistItems.observe(viewLifecycleOwner) { items ->
            updateItemsList(items)
            selectionViewModel.updateCurrentItems(items)
        }
        
        // 统计信息观察
        mainViewModel.wishlistStats.observe(viewLifecycleOwner) { stats ->
            updateStatsDisplay(stats)
        }
        
        // 价格提醒观察
        mainViewModel.priceAlerts.observe(viewLifecycleOwner) { alerts ->
            updatePriceAlerts(alerts)
        }
        
        // 推荐物品观察
        mainViewModel.recommendations.observe(viewLifecycleOwner) { recommendations ->
            updateRecommendations(recommendations)
        }
        
        // 导航事件观察
        mainViewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            event?.let {
                handleNavigation(it)
                mainViewModel.onNavigationEventConsumed()
            }
        }
        
        // Snackbar消息观察
        mainViewModel.snackbarMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Material3Feedback.showSuccess(binding.root, it)
                mainViewModel.clearSnackbarMessage()
            }
        }
        
        // 筛选状态观察
        mainViewModel.filterState.observe(viewLifecycleOwner) { filterState ->
            updateFilterUI(filterState)
        }
    }
    
    private fun observeSelectionViewModel() {
        // 选择模式状态观察
        selectionViewModel.isSelectionMode.observe(viewLifecycleOwner) { isSelectionMode ->
            updateSelectionModeUI(isSelectionMode)
            wishlistAdapter.setSelectionMode(isSelectionMode)
        }
        
        // 选中项目观察
        selectionViewModel.selectedItems.observe(viewLifecycleOwner) { selectedItems ->
            wishlistAdapter.updateSelection(selectedItems)
            updateSelectionToolbar()
        }
        
        // 全选状态观察
        selectionViewModel.isAllSelected.observe(viewLifecycleOwner) { isAllSelected ->
            updateSelectAllButton(isAllSelected)
        }
        
        // 批量操作结果观察
        selectionViewModel.operationResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                when (it) {
                    is SelectionOperationResult.Success -> {
                        Material3Feedback.showSuccess(binding.root, it.message)
                    }
                    is SelectionOperationResult.Error -> {
                        Material3Feedback.showError(binding.root, it.message)
                    }
                }
                selectionViewModel.clearOperationResult()
            }
        }
        
        // 处理状态观察
        selectionViewModel.isProcessing.observe(viewLifecycleOwner) { isProcessing ->
            updateProcessingState(isProcessing)
        }
    }
    
    // === UI更新方法 ===
    
    private fun updateItemsList(items: List<com.example.itemmanagement.data.entity.wishlist.WishlistItemEntity>) {
        wishlistAdapter.submitList(items)
        
        // 更新空状态显示
        val isEmpty = items.isEmpty()
        binding.layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerViewWishlist.visibility = if (isEmpty) View.GONE else View.VISIBLE
        
        // 更新计数显示
        updateItemCount(items.size)
    }
    
    private fun updateStatsDisplay(stats: com.example.itemmanagement.data.model.wishlist.WishlistStats) {
        with(binding) {
            textTotalItems.text = stats.totalItems.toString()
            textTotalValue.text = "¥${String.format("%.0f", stats.totalCurrentValue)}"
            textAveragePrice.text = "¥${String.format("%.0f", stats.averageItemPrice)}"
            textPriceAlerts.text = stats.priceDroppedItems.toString()
        }
    }
    
    private fun updatePriceAlerts(alerts: List<com.example.itemmanagement.data.entity.wishlist.WishlistItemEntity>) {
        // 更新价格提醒显示
        val hasAlerts = alerts.isNotEmpty()
        binding.chipPriceAlerts.visibility = if (hasAlerts) View.VISIBLE else View.GONE
        binding.chipPriceAlerts.text = "价格提醒 (${alerts.size})"
    }
    
    private fun updateRecommendations(recommendations: List<com.example.itemmanagement.data.model.wishlist.WishlistItemDetails>) {
        // 更新推荐显示
        val hasRecommendations = recommendations.isNotEmpty()
        binding.chipRecommendations.visibility = if (hasRecommendations) View.VISIBLE else View.GONE
        binding.chipRecommendations.text = "推荐 (${recommendations.size})"
    }
    
    private fun updateFilterUI(filterState: WishlistFilterState) {
        // 筛选UI更新已移除，筛选功能通过系统菜单处理
    }
    
    private fun updateSelectionModeUI(isSelectionMode: Boolean) {
        with(binding) {
            // 更新FAB
            fabAdd.visibility = if (isSelectionMode) View.GONE else View.VISIBLE
        }
        
        // 触发菜单重新创建，切换菜单类型
        requireActivity().invalidateOptionsMenu()
    }
    
    private fun updateSelectionToolbar() {
        // 选择工具栏已移除，选择状态通过系统菜单显示
    }
    
    private fun updateSelectAllButton(isAllSelected: Boolean) {
        // 全选按钮已移除，功能通过系统菜单处理
    }
    
    private fun updateItemCount(count: Int) {
        // 物品计数显示已移除，信息可以通过统计卡片查看
    }
    
    private fun updateProcessingState(isProcessing: Boolean) {
        // 处理状态显示已移除，通过系统UI处理
    }
    
    private fun showError(message: String) {
        Material3Feedback.showError(binding.root, message)
    }
    
    private fun hideError() {
        // Error会自动隐藏
    }
    
    // === 交互方法 ===
    
    // 显示模式切换功能已移除，通过系统菜单处理
    
    private fun showPriceUpdateDialog(itemId: Long, currentPrice: Double?) {
        // TODO: 实现价格更新对话框
        Material3Feedback.showInfo(binding.root, "价格更新功能开发中")
    }
    
    // === 导航处理 ===
    
    private fun handleNavigation(event: WishlistNavigationEvent) {
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
            is WishlistNavigationEvent.Filter -> {
                // TODO: 显示筛选面板
                Material3Feedback.showInfo(binding.root, "筛选功能开发中")
            }
            is WishlistNavigationEvent.Stats -> {
                // TODO: 导航到统计页面
                Material3Feedback.showInfo(binding.root, "统计功能开发中")
            }
            is WishlistNavigationEvent.PriceTracking -> {
                // TODO: 导航到价格跟踪页面
                Material3Feedback.showInfo(binding.root, "价格跟踪功能开发中")
            }
            else -> {
                // 其他导航事件
            }
        }
    }
    
    // === 菜单处理 ===
    
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if (selectionViewModel.isSelectionMode.value == true) {
            menuInflater.inflate(R.menu.menu_wishlist_selection, menu)
        } else {
            menuInflater.inflate(R.menu.menu_wishlist_main, menu)
        }
    }
    
    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_search -> {
                // 搜索功能在SearchView中处理
                true
            }
            R.id.action_sort -> {
                showSortDialog()
                true
            }
            R.id.action_settings -> {
                mainViewModel.navigateToSettings()
                true
            }
            // 选择模式菜单项
            R.id.action_select_all -> {
                selectionViewModel.toggleSelectAll()
                true
            }
            R.id.action_batch_priority -> {
                showBatchPriorityDialog()
                true
            }
            R.id.action_batch_delete -> {
                showBatchDeleteConfirmation()
                true
            }
            else -> false
        }
    }
    
    private fun showSortDialog() {
        // TODO: 实现排序对话框
        Material3Feedback.showInfo(binding.root, "排序功能开发中")
    }
    
    private fun showBatchPriorityDialog() {
        // TODO: 实现批量优先级对话框
        Material3Feedback.showInfo(binding.root, "批量操作功能开发中")
    }
    
    private fun showBatchDeleteConfirmation() {
        // TODO: 实现批量删除确认对话框
        Material3Feedback.showInfo(binding.root, "批量删除功能开发中")
    }
    
    // === 生命周期管理 ===
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    // === 辅助方法 ===
    
    private fun createViewModelFactory(): WishlistViewModelFactory {
        val app = requireActivity().application as ItemManagementApplication
        val database = app.database
        val wishlistRepository = WishlistRepository(
            database = database,
            wishlistDao = database.wishlistDao(),
            priceHistoryDao = database.wishlistPriceHistoryDao()
        )
        return WishlistViewModelFactory(wishlistRepository)
    }
    
    /**
     * 删除心愿单物品
     */
    private fun deleteWishlistItem(itemId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val app = (requireActivity().application as ItemManagementApplication)
                app.wishlistRepository.deleteWishlistItem(itemId)
                if (isAdded && _binding != null) {
                    Material3Feedback.showSuccess(binding.root, "已从心愿单中删除")
                    mainViewModel.refreshData()
                }
            } catch (e: Exception) {
                if (isAdded && _binding != null) {
                    Material3Feedback.showError(binding.root, "删除失败：${e.message}")
                }
            }
        }
    }
    
    /**
     * 标记心愿单物品为已购买
     */
    private fun markItemAsPurchased(itemId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val app = (requireActivity().application as ItemManagementApplication)
                app.wishlistRepository.markAsPurchased(itemId)
                if (isAdded && _binding != null) {
                    Material3Feedback.showSuccess(binding.root, "已标记为已购买")
                    mainViewModel.refreshData()
                }
            } catch (e: Exception) {
                if (isAdded && _binding != null) {
                    Material3Feedback.showError(binding.root, "操作失败：${e.message}")
                }
            }
        }
    }
    
    /**
     * 应用购买状态筛选
     */
    private fun applyPurchaseStatusFilter(purchaseStatus: String) {
        val app = (requireActivity().application as ItemManagementApplication)
        viewLifecycleOwner.lifecycleScope.launch {
            when (purchaseStatus) {
                "all" -> {
                    // 显示所有物品（包括已购买和未购买）
                    app.wishlistRepository.getAllActiveItems().collect { items ->
                        wishlistAdapter.submitList(items)
                        updateEmptyState(items.isEmpty())
                    }
                }
                "unpurchased" -> {
                    // 只显示未购买的物品
                    app.wishlistRepository.getUnpurchasedItems().collect { items ->
                        wishlistAdapter.submitList(items)
                        updateEmptyState(items.isEmpty())
                    }
                }
                "purchased" -> {
                    // 只显示已购买的物品
                    app.wishlistRepository.getPurchasedItems().collect { items ->
                        wishlistAdapter.submitList(items)
                        updateEmptyState(items.isEmpty())
                    }
                }
            }
        }
    }
    
    /**
     * 更新空状态显示
     */
    private fun updateEmptyState(isEmpty: Boolean) {
        // 检查Fragment是否仍然存在，避免在Fragment销毁后访问binding
        if (!isAdded || _binding == null) return
        
        with(binding) {
            if (isEmpty) {
                layoutEmptyState.visibility = View.VISIBLE
                recyclerViewWishlist.visibility = View.GONE
                val currentFilter = when {
                    chipPurchased.isChecked -> "已购买"
                    chipUnpurchased.isChecked -> "未购买" 
                    else -> "全部"
                }
                textEmptyMessage.text = "暂无${currentFilter}的心愿单物品"
            } else {
                layoutEmptyState.visibility = View.GONE
                recyclerViewWishlist.visibility = View.VISIBLE
            }
        }
    }
    
    /**
     * 插入心愿单测试数据（临时功能）
     * 长按悬浮按钮触发
     */
    private fun insertWishlistTestData() {
        Toast.makeText(context, "正在生成心愿单测试数据...", Toast.LENGTH_SHORT).show()
        
        WishlistTestDataInserter.insertWishlistTestData(requireContext()) { success, message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            if (success) {
                // 刷新心愿单数据显示
                mainViewModel.refreshData()
            }
        }
    }
}
