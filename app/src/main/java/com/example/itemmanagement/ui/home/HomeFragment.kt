package com.example.itemmanagement.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
// SmartRefreshLayout 3.0 导入
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshListener
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.adapter.HomeAdapter
import com.example.itemmanagement.databinding.FragmentHomeBinding
import com.example.itemmanagement.test.TestDataInserter
import com.example.itemmanagement.ui.utils.CustomSmoothScroller
import com.example.itemmanagement.ui.utils.Material3Performance
// import com.example.itemmanagement.ui.utils.Material3Animations
// import com.example.itemmanagement.ui.utils.fadeIn
// import com.example.itemmanagement.ui.utils.showWithAnimation
import com.example.itemmanagement.ui.animation.SearchBoxAnimator
import com.example.itemmanagement.utils.SnackbarHelper

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeAdapter = HomeAdapter()
    
    private val viewModel: HomeViewModel by viewModels {
        val app = requireActivity().application as ItemManagementApplication
        HomeViewModelFactory(
            app.repository,
            app.userProfileRepository
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Material 3 进入动画
        // view.fadeIn(100)
        
        setupRecyclerView()
        setupSearchView()
        setupButtons()
        setupSmartRefresh()
        observeData()
        
        
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            // 使用自定义的StaggeredGridLayoutManager实现瀑布流布局和滑动速度控制
            layoutManager = createCustomStaggeredGridLayoutManager()
            
            // 设置智能间距装饰器
            addItemDecoration(object : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: android.graphics.Rect,
                    view: View,
                    parent: androidx.recyclerview.widget.RecyclerView,
                    state: androidx.recyclerview.widget.RecyclerView.State
                ) {
                    val position = parent.getChildAdapterPosition(view)
                    
                    // 🛡️ 安全检查：防止position无效导致崩溃
                    if (position == androidx.recyclerview.widget.RecyclerView.NO_POSITION || 
                        position < 0 || 
                        position >= homeAdapter.itemCount) {
                        // 使用默认间距
                        val itemSpacing = resources.getDimensionPixelSize(R.dimen.photo_grid_spacing)
                        outRect.set(itemSpacing, itemSpacing, itemSpacing, itemSpacing)
                        return
                    }
                    
                    val viewType = homeAdapter.getItemViewType(position)
                    
                    when (viewType) {
                        HomeAdapter.TYPE_HEADER -> {
                            // Header使用较小的间距
                            val headerSpacing = resources.getDimensionPixelSize(R.dimen.photo_grid_spacing)
                            outRect.set(headerSpacing, headerSpacing, headerSpacing, 0)
                        }
                        // TYPE_FEED已被移除，不需要特殊处理
                        else -> {
                            // 普通物品使用标准间距
                            val itemSpacing = resources.getDimensionPixelSize(R.dimen.photo_grid_spacing)
                            outRect.set(itemSpacing, itemSpacing, itemSpacing, itemSpacing)
                        }
                    }
                }
            })
            
            // Material 3性能优化
            Material3Performance.optimizeRecyclerView(this)
            Material3Performance.enableViewRecycling(this)
            
            // 设置适配器
            adapter = homeAdapter
            
            // 添加流畅滚动监听器
            addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    // 搜索框常驻显示，无需处理显示/隐藏
                    
                    // 只有在非搜索状态下才支持无限刷新
                    if (viewModel.isSearching.value != true && dy > 0) {
                        val layoutManager = recyclerView.layoutManager as? StaggeredGridLayoutManager
                        layoutManager?.let { lm ->
                            val visibleItemCount = lm.childCount
                            val totalItemCount = lm.itemCount
                            val firstVisibleItemPositions = IntArray(2)
                            lm.findFirstVisibleItemPositions(firstVisibleItemPositions)
                            val firstVisibleItem = firstVisibleItemPositions.minOrNull() ?: 0
                            
                            // 提前触发加载（距离底部15个物品时开始，更积极的预加载）
                            val itemsFromBottom = totalItemCount - (firstVisibleItem + visibleItemCount)
                            
                            if (itemsFromBottom <= 15) {
                                loadMoreItemsSmoothly()
                            }
                        }
                    }
                }
            })
        }

        // 设置物品点击事件
        homeAdapter.setOnItemClickListener { item ->
            val bundle = androidx.core.os.bundleOf("itemId" to item.id)
            findNavController().navigate(R.id.navigation_item_detail, bundle)
        }
        
        // 设置物品删除事件
        homeAdapter.setOnDeleteClickListener { item ->
            viewModel.deleteItem(item.id)
        }
        
        // 设置功能按钮点击事件
        homeAdapter.setOnFunctionClickListener { functionType ->
            when (functionType) {
                "expiring" -> navigateToItemList("expiring", "即将过期的物品")
                "expired" -> navigateToItemList("expired", "过期物品")
                "low_stock" -> navigateToItemList("low_stock", "库存不足的物品")
                "shopping_list" -> {
                    // 导航到购物清单管理页面
                    findNavController().navigate(R.id.navigation_shopping_list_management)
                }
            }
        }
        
        // 设置信息流卡片操作事件
        // Feed action listener已移除，现在使用统一的Item处理
    }

    private fun setupSearchView() {
        // 移除实时搜索，改为手动触发搜索
        // 文本变化监听器：控制清除按钮可见性 + 处理手动清空搜索
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                
                // 🎬 清除按钮动画控制
                if (query.isNotEmpty()) {
                    SearchBoxAnimator.animateClearButtonShow(binding.clearSearchIcon)
                } else {
                    SearchBoxAnimator.animateClearButtonHide(binding.clearSearchIcon)
                    
                    // ✅ 修复：当用户手动删除所有搜索内容时，自动显示所有内容
                    // 检查当前是否处于搜索状态，如果是则清空搜索
                    if (viewModel.isSearching.value == true) {
                        viewModel.clearSearch()
                    }
                }
            }
        })

        // 设置搜索按键监听器
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(binding.searchEditText.text.toString())
                // 隐藏键盘
                hideKeyboard()
                return@setOnEditorActionListener true
            }
            false
        }

        // 设置搜索图标点击事件
        binding.searchIcon.setOnClickListener {
            performSearch(binding.searchEditText.text.toString())
            hideKeyboard()
        }
        
        // 设置清除搜索图标点击事件
        binding.clearSearchIcon.setOnClickListener {
            clearSearch()
        }
        
        // 长按搜索图标清空搜索
        binding.searchIcon.setOnLongClickListener {
            clearSearch()
            true
        }
    }

    private fun setupButtons() {
        // 顶部添加按钮已移除，现在通过底部导航栏的添加按钮进行添加操作
    }
    
    /**
     * 设置SmartRefreshLayout 3.0 - 炫酷的AndroidX版本 🌟
     */
    private fun setupSmartRefresh() {
        binding.smartRefreshLayout.apply {
            // 🎨 设置Material Design主题色彩和动画参数
            setReboundDuration(370)                    // 回弹动画时长（增加到500ms，让动画更流畅）
            setHeaderHeight(60f)                  // 头部高度（增加到80，给动画更多空间）
            setFooterHeight(60f)                       // 底部高度
            
            // 🔄 设置下拉刷新监听器
            setOnRefreshListener(OnRefreshListener { refreshLayout ->
                // 添加触觉反馈，增强体验
                view?.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                
                // 🚀 立即调用ViewModel刷新数据（后台刷新速度不变）
                viewModel.refreshData()
                
                // 🎬 延长动画完成时间，让用户看到完整的刷新效果
                refreshLayout.finishRefresh(1000) // 增加到1200ms，让动画播放完整
            })
            
            // ⬆️ 设置上拉加载更多监听器
            setOnLoadMoreListener(OnLoadMoreListener { refreshLayout ->
                // 触发无限滚动加载
                viewModel.loadMoreItemsSmoothly()
                
                // 上拉加载保持较快的完成时间
                refreshLayout.finishLoadMore(600) // 适中的时间，不影响滚动体验
            })
            
            // ⚡ 启用功能配置
            setEnableAutoLoadMore(false)               // 禁用自动加载，使用我们的无限滚动
            setEnableLoadMore(true)                    // 启用上拉加载
            setEnableRefresh(true)                     // 启用下拉刷新
            setEnableOverScrollBounce(true)            // 启用越界回弹
            setEnableOverScrollDrag(true)              // 启用越界拖拽
        }
    }

    
    private fun navigateToItemList(listType: String, title: String) {
        val bundle = androidx.core.os.bundleOf(
            "listType" to listType,
            "title" to title
        )
        findNavController().navigate(R.id.action_navigation_home_to_itemListFragment, bundle)
    }

    private fun onAddButtonClick() {
        // 导航到添加物品页面（使用新架构）
        findNavController().navigate(R.id.action_navigation_home_to_addItemFragment)
    }
    
    /**
     * 插入测试数据（临时功能）
     * 长按悬浮按钮触发
     */
    private fun insertTestData() {
        val options = arrayOf(
            "仅生成库存测试数据",
            "生成组合测试数据"
        )
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("选择测试数据类型")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> insertInventoryTestData()
                    1 -> insertCombinedTestData()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 插入库存测试数据
     */
    private fun insertInventoryTestData() {
        SnackbarHelper.show(requireView(), "正在生成库存测试数据...")
        
        TestDataInserter.insertTestData(requireContext()) { success, message ->
            SnackbarHelper.showError(requireView(), message)
            if (success) {
                // 刷新数据显示
                viewModel.refreshData()
            }
        }
    }
    
    
    /**
     * 插入组合测试数据
     */
    private fun insertCombinedTestData() {
        SnackbarHelper.show(requireView(), "组合测试数据功能暂不可用")
        
        // TODO: 实现基于统一架构的组合测试数据插入
    }

    private fun performSearch(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isNotEmpty()) {
            viewModel.setSearchQuery(trimmedQuery)
            // 使用Material3Feedback显示搜索提示
            view?.let { v ->
                com.example.itemmanagement.ui.utils.Material3Feedback.showInfo(v, "搜索: $trimmedQuery")
            }
        } else {
            // 当搜索内容为空时，清空搜索结果
            viewModel.clearSearch()
            view?.let { v ->
                com.example.itemmanagement.ui.utils.Material3Feedback.showInfo(v, "已清空搜索")
            }
        }
    }
    
    private fun clearSearch() {
        binding.searchEditText.setText("")
        viewModel.clearSearch()
        hideKeyboard()
        view?.let { v ->
            com.example.itemmanagement.ui.utils.Material3Feedback.showInfo(v, "已清空搜索")
        }
    }
    
    private fun hideKeyboard() {
        val inputMethodManager = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) 
            as android.view.inputmethod.InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
    }
    
    /**
     * 流畅加载更多物品
     */
    private fun loadMoreItemsSmoothly() {
        viewModel.loadMoreItemsSmoothly()
    }
    
    // 搜索框现在常驻显示，无需动画处理

    private fun observeData() {
        // 观察展示数据（包含推荐理由信息）
        viewModel.items.observe(viewLifecycleOwner) { displayItems ->
            updateItemsWithAnimation(displayItems)
            updateEmptyView(displayItems.isEmpty())
            
            // SmartRefreshLayout自动处理刷新状态 ✨
        }
        
        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // 更新适配器的loading状态
            val currentItems = viewModel.items.value ?: emptyList()
            homeAdapter.submitDisplayItems(currentItems, isLoading)
        }
        
        // 观察搜索状态
        viewModel.isSearching.observe(viewLifecycleOwner) { _ ->
            // 可以根据搜索状态更新UI，比如显示搜索指示器
            // 这里暂时不做特殊处理，搜索结果会直接显示在列表中
        }
        
        viewModel.functionVisibility.observe(viewLifecycleOwner) { config ->
            homeAdapter.updateFunctionVisibility(config)
        }
    }
    
    /**
     * 流畅更新物品列表，支持动画效果
     */
    private fun updateItemsWithAnimation(newItems: List<HomeViewModel.HomeDisplayItem>) {
        // 获取当前加载状态
        val isLoading = viewModel.isLoading.value ?: false
        // 使用新的submitDisplayItems方法，支持DiffUtil动画和loading状态
        homeAdapter.submitDisplayItems(newItems, isLoading)
    }
    
    private fun updateEmptyView(isEmpty: Boolean) {
        val currentQuery = viewModel.getCurrentSearchQuery()
        val isSearching = !currentQuery.isNullOrBlank()
        
        if (isEmpty) {
            if (isSearching) {
                // 普通搜索无结果
                binding.emptyView.visibility = View.GONE
                binding.searchEmptyView.visibility = View.VISIBLE
                binding.searchEmptyHint.text = "未找到包含「$currentQuery」的物品\n试试更换关键词或清空搜索"
            } else {
                // 非搜索状态的空视图
                binding.emptyView.visibility = View.VISIBLE
                binding.searchEmptyView.visibility = View.GONE
            }
        } else {
            // 有数据时隐藏所有空视图
            binding.emptyView.visibility = View.GONE
            binding.searchEmptyView.visibility = View.GONE
        }
    }

    /**
     * 创建支持自定义滑动速度的StaggeredGridLayoutManager
     */
    private fun createCustomStaggeredGridLayoutManager(): StaggeredGridLayoutManager {
        return object : StaggeredGridLayoutManager(2, VERTICAL) {
            override fun smoothScrollToPosition(
                recyclerView: androidx.recyclerview.widget.RecyclerView?,
                state: androidx.recyclerview.widget.RecyclerView.State?,
                position: Int
            ) {
                recyclerView?.let { rv ->
                    val smoothScroller = CustomSmoothScroller(
                        rv.context,
                        CustomSmoothScroller.SPEED_NORMAL // 使用正常速度（可调节）
                    )
                    smoothScroller.targetPosition = position
                    startSmoothScroll(smoothScroller)
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 不再自动刷新，改为通过MainActivity的事件通知刷新
        // viewModel.refreshData()
    }
    
    /**
     * 刷新首页数据
     * 由MainActivity在从添加/编辑/详情页面返回时调用
     */
    fun refreshData() {
        viewModel.refreshData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}