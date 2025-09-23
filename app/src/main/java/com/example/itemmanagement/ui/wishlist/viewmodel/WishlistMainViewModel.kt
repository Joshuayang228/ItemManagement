package com.example.itemmanagement.ui.wishlist.viewmodel

import androidx.lifecycle.*
import com.example.itemmanagement.data.entity.wishlist.WishlistItemEntity
import com.example.itemmanagement.data.entity.wishlist.WishlistPriority
import com.example.itemmanagement.data.entity.wishlist.WishlistUrgency
import com.example.itemmanagement.data.model.wishlist.WishlistItemDetails
import com.example.itemmanagement.data.model.wishlist.WishlistStats
import com.example.itemmanagement.data.repository.WishlistRepository
import com.example.itemmanagement.ui.wishlist.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 心愿单主界面ViewModel
 * 遵循MVVM架构，专注于主界面的状态管理和核心业务逻辑
 * 将复杂的功能拆分为多个专门的ViewModel
 */
class WishlistMainViewModel(
    private val wishlistRepository: WishlistRepository
) : ViewModel() {
    
    // === UI状态管理 ===
    
    private val _uiState = MutableLiveData<WishlistUiState>(WishlistUiState.Loading)
    val uiState: LiveData<WishlistUiState> = _uiState
    
    private val _navigationEvent = MutableLiveData<WishlistNavigationEvent?>()
    val navigationEvent: LiveData<WishlistNavigationEvent?> = _navigationEvent
    
    private val _snackbarMessage = MutableLiveData<String?>()
    val snackbarMessage: LiveData<String?> = _snackbarMessage
    
    // === 数据状态管理 ===
    
    // 筛选和搜索状态
    private val _filterState = MutableLiveData<WishlistFilterState>(WishlistFilterState())
    val filterState: LiveData<WishlistFilterState> = _filterState
    
    // 心愿单物品列表（响应式）
    val wishlistItems: LiveData<List<WishlistItemEntity>> = 
        combine(
            wishlistRepository.getAllActiveItems(),
            _filterState.asFlow()
        ) { items, filterState ->
            applyFilterAndSort(items, filterState)
        }.asLiveData()
    
    // 心愿单统计信息
    private val _wishlistStats = MutableLiveData<WishlistStats>()
    val wishlistStats: LiveData<WishlistStats> = _wishlistStats
    
    // 价格提醒物品
    private val _priceAlerts = MutableLiveData<List<WishlistItemEntity>>(emptyList())
    val priceAlerts: LiveData<List<WishlistItemEntity>> = _priceAlerts
    
    // 推荐物品
    private val _recommendations = MutableLiveData<List<WishlistItemDetails>>(emptyList())
    val recommendations: LiveData<List<WishlistItemDetails>> = _recommendations
    
    init {
        loadInitialData()
    }
    
    // === 数据加载方法 ===
    
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                _uiState.value = WishlistUiState.Loading
                
                // 并行加载各种数据
                loadStats()
                loadPriceAlerts()
                loadRecommendations()
                
                _uiState.value = WishlistUiState.Success
                
            } catch (e: Exception) {
                _uiState.value = WishlistUiState.Error("加载数据失败: ${e.message}")
            }
        }
    }
    
    /**
     * 刷新数据
     */
    fun refreshData() {
        loadInitialData()
    }
    
    private suspend fun loadStats() {
        try {
            val stats = wishlistRepository.getWishlistStats()
            _wishlistStats.value = stats
        } catch (e: Exception) {
            // 统计信息加载失败不影响主流程
        }
    }
    
    private suspend fun loadPriceAlerts() {
        try {
            val alerts = wishlistRepository.getItemsNeedingPriceAttention()
            _priceAlerts.value = alerts
        } catch (e: Exception) {
            _priceAlerts.value = emptyList()
        }
    }
    
    private suspend fun loadRecommendations() {
        try {
            val recommendations = wishlistRepository.getRecommendationsBasedOnInventory()
            _recommendations.value = recommendations
        } catch (e: Exception) {
            _recommendations.value = emptyList()
        }
    }
    
    // === 搜索和筛选方法 ===
    
    /**
     * 设置搜索关键词
     */
    fun setSearchQuery(query: String) {
        val currentState = _filterState.value ?: WishlistFilterState()
        _filterState.value = currentState.copy(searchQuery = query)
    }
    
    /**
     * 设置排序方式
     */
    fun setSortOrder(sortOrder: WishlistSortOrder) {
        val currentState = _filterState.value ?: WishlistFilterState()
        _filterState.value = currentState.copy(sortOrder = sortOrder)
    }
    
    /**
     * 设置筛选条件
     */
    fun setFilter(filter: WishlistFilter) {
        val currentState = _filterState.value ?: WishlistFilterState()
        _filterState.value = currentState.copy(filter = filter)
    }
    
    /**
     * 重置所有筛选条件
     */
    fun resetFilters() {
        _filterState.value = WishlistFilterState()
    }
    
    // === 快速操作方法 ===
    
    /**
     * 快速更新优先级
     */
    fun quickUpdatePriority(itemId: Long, priority: WishlistPriority) {
        viewModelScope.launch {
            try {
                wishlistRepository.updatePriority(itemId, priority)
                showSnackbar("优先级已更新")
            } catch (e: Exception) {
                showSnackbar("更新失败: ${e.message}")
            }
        }
    }
    
    /**
     * 快速删除物品
     */
    fun quickDeleteItem(itemId: Long) {
        viewModelScope.launch {
            try {
                wishlistRepository.deleteWishlistItem(itemId)
                showSnackbar("已从心愿单移除")
                loadStats() // 重新加载统计数据
            } catch (e: Exception) {
                showSnackbar("删除失败: ${e.message}")
            }
        }
    }
    
    /**
     * 标记为已实现
     */
    fun markAsAchieved(itemId: Long, relatedItemId: Long? = null) {
        viewModelScope.launch {
            try {
                wishlistRepository.markAsAchieved(itemId, relatedItemId)
                showSnackbar("愿望已实现！恭喜！")
                loadStats()
            } catch (e: Exception) {
                showSnackbar("标记失败: ${e.message}")
            }
        }
    }
    
    // === 导航方法 ===
    
    /**
     * 导航到添加心愿单物品页面
     */
    fun navigateToAddItem() {
        _navigationEvent.value = WishlistNavigationEvent.AddItem
    }
    
    /**
     * 导航到物品详情页面
     */
    fun navigateToItemDetail(itemId: Long) {
        // 增加查看次数（异步）
        viewModelScope.launch {
            try {
                wishlistRepository.incrementViewCount(itemId)
            } catch (e: Exception) {
                // 查看次数更新失败不影响导航
            }
        }
        _navigationEvent.value = WishlistNavigationEvent.ItemDetail(itemId)
    }
    
    /**
     * 导航到筛选页面
     */
    fun navigateToFilter() {
        _navigationEvent.value = WishlistNavigationEvent.Filter
    }
    
    /**
     * 导航到统计页面
     */
    fun navigateToStats() {
        _navigationEvent.value = WishlistNavigationEvent.Stats
    }
    
    /**
     * 导航到价格跟踪页面
     */
    fun navigateToPriceTracking() {
        _navigationEvent.value = WishlistNavigationEvent.PriceTracking
    }

    fun navigateToSettings() {
        _navigationEvent.value = WishlistNavigationEvent.Settings
    }
    
    /**
     * 消费导航事件
     */
    fun onNavigationEventConsumed() {
        _navigationEvent.value = null
    }
    
    // === 私有辅助方法 ===
    
    /**
     * 应用筛选和排序
     */
    private fun applyFilterAndSort(
        items: List<WishlistItemEntity>,
        filterState: WishlistFilterState
    ): List<WishlistItemEntity> {
        var filteredItems = items
        
        // 应用搜索
        if (filterState.searchQuery.isNotBlank()) {
            filteredItems = filteredItems.filter { item ->
                item.name.contains(filterState.searchQuery, ignoreCase = true) ||
                item.brand?.contains(filterState.searchQuery, ignoreCase = true) == true ||
                item.category.contains(filterState.searchQuery, ignoreCase = true) ||
                item.customNote?.contains(filterState.searchQuery, ignoreCase = true) == true
            }
        }
        
        // 应用筛选条件
        filteredItems = filteredItems.filter { item ->
            filterState.filter.matches(item)
        }
        
        // 应用排序
        return when (filterState.sortOrder) {
            WishlistSortOrder.PRIORITY_DESC -> filteredItems.sortedByDescending { it.priority.level }
            WishlistSortOrder.PRIORITY_ASC -> filteredItems.sortedBy { it.priority.level }
            WishlistSortOrder.PRICE_DESC -> filteredItems.sortedByDescending { it.currentPrice ?: -1.0 }
            WishlistSortOrder.PRICE_ASC -> filteredItems.sortedBy { it.currentPrice ?: Double.MAX_VALUE }
            WishlistSortOrder.DATE_DESC -> filteredItems.sortedByDescending { it.addDate }
            WishlistSortOrder.DATE_ASC -> filteredItems.sortedBy { it.addDate }
            WishlistSortOrder.NAME_ASC -> filteredItems.sortedBy { it.name.lowercase() }
            WishlistSortOrder.NAME_DESC -> filteredItems.sortedByDescending { it.name.lowercase() }
            WishlistSortOrder.URGENCY_DESC -> filteredItems.sortedByDescending { it.urgency.level }
        }
    }
    
    /**
     * 显示Snackbar消息
     */
    private fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }
    
    /**
     * 清除Snackbar消息
     */
    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }
}

/**
 * 心愿单UI状态密封类
 */
sealed class WishlistUiState {
    object Loading : WishlistUiState()
    object Success : WishlistUiState()
    data class Error(val message: String) : WishlistUiState()
}

/**
 * 心愿单筛选状态数据类
 */
data class WishlistFilterState(
    val searchQuery: String = "",
    val sortOrder: WishlistSortOrder = WishlistSortOrder.PRIORITY_DESC,
    val filter: WishlistFilter = WishlistFilter()
)
