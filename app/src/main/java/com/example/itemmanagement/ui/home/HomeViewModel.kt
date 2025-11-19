package com.example.itemmanagement.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.data.repository.UserProfileRepository
import com.example.itemmanagement.data.entity.unified.UnifiedItemEntity
import com.example.itemmanagement.data.mapper.toItem
import com.example.itemmanagement.data.model.Item
import com.example.itemmanagement.data.relation.ItemWithDetails
import com.example.itemmanagement.data.model.HomeFunctionConfig
import com.example.itemmanagement.ui.feed.SmartFeedAlgorithm
import com.example.itemmanagement.ui.feed.SmoothScrollManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest

class HomeViewModel(
    private val repository: UnifiedItemRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {
    
    /**
     * 展示物品数据类（包含推荐理由信息）
     */
    data class HomeDisplayItem(
        val item: Item,
        val showReason: Boolean = false,
        val reasonText: String? = null
    )
    
    // 智能信息流算法
    private val smartFeedAlgorithm = SmartFeedAlgorithm(repository)
    
    // 流畅滚动管理器
    private val smoothScrollManager = SmoothScrollManager()
    
    // 搜索关键词的MutableLiveData
    private val _searchQuery = MutableLiveData<String>()
    
    // 最终展示的物品数据（包含推荐理由）
    private val _displayItems = MutableLiveData<List<HomeDisplayItem>>()
    val items: LiveData<List<HomeDisplayItem>> = _displayItems
    
    // 加载状态
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    // 为了保持向后兼容，提供Item类型的数据
    val allItems: LiveData<List<Item>> = _displayItems.switchMap { displayItems ->
        androidx.lifecycle.liveData {
            emit(displayItems.map { it.item })
        }
    }
    
    // 搜索状态
    private val _isSearching = MutableLiveData<Boolean>(false)
    val isSearching: LiveData<Boolean> = _isSearching
    
    private val _functionVisibility = MutableLiveData(HomeFunctionConfig())
    val functionVisibility: LiveData<HomeFunctionConfig> = _functionVisibility
    
    init {
        // 初始化时显示所有物品
        _searchQuery.value = ""
        
        // 生成初始信息流内容
        refreshData()
        observeHomeFunctionConfig()
    }
    
    /**
     * 生成智能信息流
     */
    private fun generateSmartFeed(count: Int = 10) {
        viewModelScope.launch {
            try {
                val feedItems = smartFeedAlgorithm.generateFeed(count)
                val displayItems = feedItems.map { feedItem ->
                    HomeDisplayItem(
                        item = feedItem.item,
                        showReason = feedItem.showReason,
                        reasonText = feedItem.reasonText
                    )
                }
                _displayItems.value = displayItems
            } catch (e: Exception) {
                // 如果生成失败，直接显示所有物品
                fallbackToAllItems()
            }
        }
    }
    
    /**
     * 搜索模式：过滤物品
     */
    private fun performSearch(query: String) {
        viewModelScope.launch {
            try {
                val allItems = repository.getAllItems().first()
                val filteredItems = allItems.filter { item ->
                    // 在物品名称、备注和品牌中搜索
                    item.name.contains(query, ignoreCase = true) ||
                    (!item.customNote.isNullOrBlank() && item.customNote.contains(query, ignoreCase = true)) ||
                    (!item.brand.isNullOrBlank() && item.brand.contains(query, ignoreCase = true)) ||
                    (!item.category.isNullOrBlank() && item.category.contains(query, ignoreCase = true)) ||
                    (!item.subCategory.isNullOrBlank() && item.subCategory.contains(query, ignoreCase = true)) ||
                    // 也在标签中搜索
                    item.tags.any { tag -> tag.name.contains(query, ignoreCase = true) }
                }
                
                val displayItems = filteredItems.map { item ->
                    HomeDisplayItem(item = item, showReason = false, reasonText = null)
                }
                _displayItems.value = displayItems
            } catch (e: Exception) {
                _displayItems.value = emptyList()
            }
        }
    }
    
    /**
     * 兜底方案：显示所有物品
     */
    private fun fallbackToAllItems() {
        viewModelScope.launch {
            try {
                val allItems = repository.getAllItems().first()
                val displayItems = allItems.map { item ->
                    HomeDisplayItem(item = item, showReason = false, reasonText = null)
                }
                _displayItems.value = displayItems
            } catch (e: Exception) {
                _displayItems.value = emptyList()
            }
        }
    }
    
    /**
     * 设置搜索关键词
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _isSearching.value = query.isNotBlank()
        
        if (query.isBlank()) {
            // 搜索为空，显示智能信息流
            generateSmartFeed()
        } else {
            // 有搜索关键词，执行搜索
            performSearch(query)
        }
    }
    
    /**
     * 清空搜索
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _isSearching.value = false
        
        // 返回智能信息流
        generateSmartFeed()
    }
    
    /**
     * 获取当前搜索关键词
     */
    fun getCurrentSearchQuery(): String? {
        return _searchQuery.value
    }
    
    /**
     * 刷新数据
     */
    fun refreshData() {
        val currentQuery = _searchQuery.value ?: ""
        
        if (currentQuery.isBlank()) {
            // 无搜索时，刷新智能信息流
            generateSmartFeed()
        } else {
            // 有搜索时，重新执行搜索
            performSearch(currentQuery)
        }
    }
    
    /**
     * 流畅加载更多物品（新的主要方法）
     */
    fun loadMoreItemsSmoothly() {
        val currentQuery = _searchQuery.value ?: ""
        
        if (currentQuery.isBlank() && smoothScrollManager.getCurrentState() is SmoothScrollManager.LoadingState.Idle) {
            viewModelScope.launch {
                // 设置loading状态为true
                _isLoading.value = true
                
                val currentItems = _displayItems.value ?: emptyList()
                
                smoothScrollManager.loadMoreSmoothly(
                    currentItemCount = currentItems.size,
                    totalNeeded = currentItems.size + 24, // 每次目标增加24个，提供更充足的内容
                    onBatchLoaded = { batchItems ->
                        @Suppress("UNCHECKED_CAST")
                        appendDisplayItems(batchItems as List<HomeDisplayItem>)
                    },
                    itemGenerator = { count -> generateItemsBatch(count) }
                )
                
                // 加载完成后设置loading状态为false
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 生成一批物品
     */
    private suspend fun generateItemsBatch(count: Int): List<HomeDisplayItem> {
        return try {
            val feedItems = smartFeedAlgorithm.generateFeed(count)
            feedItems.map { feedItem ->
                HomeDisplayItem(
                    item = feedItem.item,
                    showReason = feedItem.showReason,
                    reasonText = feedItem.reasonText
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 追加物品到现有列表（而不是替换）
     */
    private fun appendDisplayItems(newItems: List<HomeDisplayItem>) {
        val currentItems = _displayItems.value?.toMutableList() ?: mutableListOf()
        currentItems.addAll(newItems)
        _displayItems.value = currentItems
    }
    
    /**
     * 加载更多物品（兼容旧接口）
     */
    fun loadMoreItems(additionalCount: Int = 10) {
        // 使用新的流畅加载方法
        loadMoreItemsSmoothly()
    }
    

    /**
     * 手动刷新推荐内容
     */
    fun refreshRecommendations() {
        refreshData()
    }
    
    /**
     * 标记物品为已开封
     */
    fun markItemAsOpened(itemId: Long) {
        if (itemId > 0) {
            viewModelScope.launch {
                try {
                    val item = repository.getItemWithDetailsById(itemId)
                    item?.let { itemWithDetails ->
                        val updatedInventoryDetail = itemWithDetails.inventoryDetail?.copy(
                            openStatus = com.example.itemmanagement.data.model.OpenStatus.OPENED,
                            openDate = java.util.Date()
                        )
                        val updatedItemWithDetails = ItemWithDetails(
                            unifiedItem = itemWithDetails.unifiedItem,
                            inventoryDetail = updatedInventoryDetail,
                            photos = itemWithDetails.photos,
                            tags = itemWithDetails.tags
                        )
                        repository.updateItemWithDetails(updatedItemWithDetails)
                        
                        // 重新生成信息流
                        refreshData()
                    }
                } catch (e: Exception) {
                    // 处理错误
                }
            }
        }
    }
    
    /**
     * 获取算法统计信息（用于调试）
     */
    fun getAlgorithmStatistics(): String {
        return viewModelScope.async {
            try {
                val stats = smartFeedAlgorithm.getAlgorithmStatistics()
                stats.toString()
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }.toString()
    }
    
    /**
     * 重置算法状态（用于测试）
     */
    fun resetAlgorithmState() {
        smartFeedAlgorithm.resetAlgorithmState()
        refreshData()
    }
    
    private fun observeHomeFunctionConfig() {
        viewModelScope.launch {
            // 确保存在用户资料记录
            runCatching { userProfileRepository.getUserProfile() }
            userProfileRepository.getUserProfileFlow().collectLatest { profile ->
                val config = if (profile != null) {
                    HomeFunctionConfig(
                        showExpiringEntry = profile.showExpiringEntry,
                        showExpiredEntry = profile.showExpiredEntry,
                        showLowStockEntry = profile.showLowStockEntry,
                        showShoppingListEntry = profile.showShoppingListEntry
                    )
                } else {
                    HomeFunctionConfig()
                }
                _functionVisibility.postValue(config)
            }
        }
    }
    
    // 删除物品
    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            // 先获取物品详情
            val item = repository.getItemWithDetailsById(itemId)
            // 如果物品存在，则删除
            item?.let { 
                repository.deleteItem(it.toItem())
            }
        }
    }
}

class HomeViewModelFactory(
    private val repository: UnifiedItemRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, userProfileRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 