package com.example.itemmanagement.ui.wishlist.fragment

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.Toast
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentWishlistMainM3Binding
import com.example.itemmanagement.ui.wishlist.adapter.WishlistM3Adapter
import com.example.itemmanagement.ui.wishlist.model.*
import com.example.itemmanagement.ui.wishlist.viewmodel.*
import kotlinx.coroutines.launch

/**
 * 心愿单主界面Fragment - Material Design 3版本
 * 基于统一架构，完全兼容备份功能
 */
class WishlistMainM3Fragment : Fragment() {

    private var _binding: FragmentWishlistMainM3Binding? = null
    private val binding get() = _binding!!

    // ViewModel实例（使用统一架构）
    private val mainViewModel: WishlistMainViewModel by viewModels {
        val app = requireActivity().application as ItemManagementApplication
        WishlistViewModelFactory(
            wishlistRepository = app.wishlistRepository,
            itemRepository = app.repository
        )
    }

    private val selectionViewModel: WishlistSelectionViewModel by viewModels {
        val app = requireActivity().application as ItemManagementApplication
        WishlistViewModelFactory(
            wishlistRepository = app.wishlistRepository,
            itemRepository = app.repository
        )
    }

    // 适配器
    private lateinit var wishlistAdapter: WishlistM3Adapter

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

        setupUI()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

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
    }

    private fun setupEmptyState() {
        binding.textEmptyTitle.text = "还没有心愿单物品"
        binding.textEmptyMessage.text = "添加您想要购买的物品，我们会为您跟踪价格变化"
        binding.buttonEmptyAction.text = "添加第一个心愿"
        binding.buttonEmptyAction.setOnClickListener {
            mainViewModel.navigateToAddItem()
        }
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

        binding.recyclerViewWishlist.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = wishlistAdapter
        }
    }

    private fun setupObservers() {
        // UI状态观察
        mainViewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is WishlistUiState.Loading -> {
                    binding.swipeRefresh.isRefreshing = true
                }
                is WishlistUiState.Success -> {
                    binding.swipeRefresh.isRefreshing = false
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
                showMessage(it)
                mainViewModel.clearSnackbarMessage()
            }
        }

        // 选择模式观察
        selectionViewModel.isSelectionMode.observe(viewLifecycleOwner) { isSelectionMode ->
            updateSelectionModeUI(isSelectionMode)
            wishlistAdapter.setSelectionMode(isSelectionMode)
        }

        // 选中项目观察
        selectionViewModel.selectedItems.observe(viewLifecycleOwner) { selectedItems ->
            wishlistAdapter.updateSelection(selectedItems)
        }
    }

    private fun setupClickListeners() {
        // 添加按钮
        binding.fabAdd.setOnClickListener {
            mainViewModel.navigateToAddItem()
        }

        // 长按添加按钮生成测试数据
        binding.fabAdd.setOnLongClickListener {
            insertWishlistTestData()
            true
        }
    }

    // === UI更新方法 ===

    private fun updateItemsList(items: List<com.example.itemmanagement.data.view.WishlistItemView>) {
        wishlistAdapter.submitList(items)

        // 更新空状态显示
        val isEmpty = items.isEmpty()
        binding.layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerViewWishlist.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun updateStatsDisplay(stats: WishlistMainViewModel.SimpleWishlistStats) {
        // 如果有统计卡片的话在这里更新
        // 目前简化版本暂时不显示
    }

    private fun updatePriceAlerts(alerts: List<com.example.itemmanagement.data.view.WishlistItemView>) {
        // 价格提醒逻辑
        if (alerts.isNotEmpty()) {
            showMessage("有 ${alerts.size} 个物品价格发生变化")
        }
    }

    private fun updateSelectionModeUI(isSelectionMode: Boolean) {
        // 更新FAB显示
        binding.fabAdd.visibility = if (isSelectionMode) View.GONE else View.VISIBLE
    }

    // === 导航处理 ===

    private fun handleNavigation(event: WishlistNavigationEvent) {
        try {
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
                else -> {
                    showMessage("导航功能开发中")
                }
            }
        } catch (e: Exception) {
            showError("导航失败：${e.message}")
        }
    }

    // === 交互方法 ===

    private fun showPriceUpdateDialog(itemId: Long, currentPrice: Double?) {
        showMessage("价格更新功能开发中")
    }

    private fun deleteWishlistItem(itemId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val app = (requireActivity().application as ItemManagementApplication)
                app.repository.deleteWishlistItem(itemId)
                showMessage("已从心愿单中删除")
            } catch (e: Exception) {
                showError("删除失败：${e.message}")
            }
        }
    }

    private fun markItemAsPurchased(itemId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val app = (requireActivity().application as ItemManagementApplication)
                app.repository.moveWishlistToShopping(itemId, shoppingListId = 1L)
                showMessage("已添加到购物清单")
            } catch (e: Exception) {
                showError("操作失败：${e.message}")
            }
        }
    }

    private fun insertWishlistTestData() {
        Toast.makeText(context, "正在生成心愿单测试数据...", Toast.LENGTH_SHORT).show()
        showMessage("测试数据生成功能开发中")
    }

    // === 辅助方法 ===

    private fun showMessage(message: String) {
        if (isAdded && _binding != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        if (isAdded && _binding != null) {
            Toast.makeText(context, "错误：$message", Toast.LENGTH_LONG).show()
        }
    }

    // === 生命周期管理 ===

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
