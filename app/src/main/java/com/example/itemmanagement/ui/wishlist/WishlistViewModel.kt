package com.example.itemmanagement.ui.wishlist

import androidx.lifecycle.*
import com.example.itemmanagement.data.entity.WishlistItemEntity
import com.example.itemmanagement.data.entity.wishlist.WishlistPriority
import com.example.itemmanagement.data.entity.wishlist.WishlistUrgency
import com.example.itemmanagement.data.model.wishlist.WishlistItemDetails
import com.example.itemmanagement.data.repository.WishlistStats
import com.example.itemmanagement.data.repository.WishlistRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
/**
 * 心愿单主界面ViewModel
 * 遵循MVVM架构，管理心愿单的UI状态和业务逻辑
 */
class WishlistViewModel(
    private val wishlistRepository: WishlistRepository
) : ViewModel() {
    
    // === UI状态管理 ===
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage
    
    // === 数据状态管理 ===
    
    // 当前筛选条件
    private val _currentFilter = MutableLiveData<WishlistFilter>(WishlistFilter())
    val currentFilter: LiveData<WishlistFilter> = _currentFilter
    
    // 搜索关键词
    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery
    
    // 排序方式
    private val _sortOrder = MutableLiveData<WishlistSortOrder>(WishlistSortOrder.PRIORITY_DESC)
    val sortOrder: LiveData<WishlistSortOrder> = _sortOrder
    
    // === 核心数据 ===
    
    // 心愿单物品列表（经过筛选和排序）
    val wishlistItems: LiveData<List<WishlistItemEntity>> = 
        combine(
            wishlistRepository.getAllActiveItems(),
            _currentFilter.asFlow(),
            _searchQuery.asFlow(),
            _sortOrder.asFlow()
        ) { items, filter, query, sort ->
            applyFilterSortAndSearch(items, filter, query, sort)
        }.asLiveData()
    
    // 心愿单统计信息
    private val _wishlistStats = MutableLiveData<WishlistStats>()
    val wishlistStats: LiveData<WishlistStats> = _wishlistStats
    
    // 价格提醒物品
    private val _priceAlertItems = MutableLiveData<List<WishlistItemEntity>>()
    val priceAlertItems: LiveData<List<WishlistItemEntity>> = _priceAlertItems
    
    // 推荐物品
    private val _recommendedItems = MutableLiveData<List<WishlistItemDetails>>()
    val recommendedItems: LiveData<List<WishlistItemDetails>> = _recommendedItems
    
    // === 选择模式管理 ===
    
    private val _isSelectionMode = MutableLiveData<Boolean>(false)
    val isSelectionMode: LiveData<Boolean> = _isSelectionMode
    
    private val _selectedItems = MutableLiveData<Set<Long>>(emptySet())
    val selectedItems: LiveData<Set<Long>> = _selectedItems
    
    val selectedItemsCount: LiveData<Int> = _selectedItems.map { it.size }
    
    // === 界面交互管理 ===
    
    private val _navigationEvent = MutableLiveData<WishlistNavigationEvent?>()
    val navigationEvent: LiveData<WishlistNavigationEvent?> = _navigationEvent
    
    init {
        loadInitialData()
    }
    
    // === 数据加载方法 ===
    
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                refreshStats()
                loadPriceAlerts()
                loadRecommendations()
            } catch (e: Exception) {
                _errorMessage.value = "加载数据失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 刷新统计信息
     */
    fun refreshStats() {
        viewModelScope.launch {
            try {
                val stats = wishlistRepository.getWishlistStats()
                _wishlistStats.value = stats
            } catch (e: Exception) {
                _errorMessage.value = "刷新统计信息失败: ${e.message}"
            }
        }
    }
    
    /**
     * 加载价格提醒物品
     */
    private fun loadPriceAlerts() {
        viewModelScope.launch {
            try {
                val alertItems = wishlistRepository.getItemsNeedingPriceAttention()
                _priceAlertItems.value = alertItems
            } catch (e: Exception) {
                _errorMessage.value = "加载价格提醒失败: ${e.message}"
            }
        }
    }
    
    /**
     * 加载推荐物品
     */
    private fun loadRecommendations() {
        viewModelScope.launch {
            try {
                val recommendations = wishlistRepository.getRecommendationsBasedOnInventory()
                _recommendedItems.value = emptyList()
            } catch (e: Exception) {
                // 推荐功能失败不显示错误，只是空列表
                _recommendedItems.value = emptyList()
            }
        }
    }
    
    // === 心愿单操作方法 ===
    
    /**
     * 添加心愿单物品
     */
    fun addWishlistItem(itemDetails: WishlistItemDetails) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // 将WishlistItemDetails转换为WishlistItemEntity
                val entity = WishlistItemEntity(
                    name = itemDetails.name,
                    category = itemDetails.category,
                    subCategory = itemDetails.subCategory,
                    brand = itemDetails.brand,
                    specification = itemDetails.specification,
                    customNote = itemDetails.notes,
                    price = itemDetails.estimatedPrice,
                    targetPrice = itemDetails.targetPrice,
                    priority = itemDetails.priority,
                    urgency = itemDetails.urgency,
                    quantity = itemDetails.desiredQuantity,
                    quantityUnit = itemDetails.quantityUnit,
                    budgetLimit = itemDetails.budgetLimit,
                    purchaseChannel = itemDetails.preferredStore,
                    sourceUrl = itemDetails.sourceUrl,
                    imageUrl = itemDetails.imageUrl,
                    addedReason = itemDetails.addedReason
                )
                val itemId = wishlistRepository.addWishlistItem(entity)
                _successMessage.value = "已添加到心愿单"
                refreshStats()
            } catch (e: Exception) {
                _errorMessage.value = "添加失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 删除心愿单物品
     */
    fun deleteWishlistItem(itemId: Long) {
        viewModelScope.launch {
            try {
                wishlistRepository.deleteWishlistItem(itemId)
                _successMessage.value = "已从心愿单移除"
                refreshStats()
            } catch (e: Exception) {
                _errorMessage.value = "删除失败: ${e.message}"
            }
        }
    }
    
    /**
     * 更新优先级
     */
    fun updatePriority(itemId: Long, priority: WishlistPriority) {
        viewModelScope.launch {
            try {
                wishlistRepository.updatePriority(itemId, priority)
                _successMessage.value = "优先级已更新"
            } catch (e: Exception) {
                _errorMessage.value = "更新优先级失败: ${e.message}"
            }
        }
    }
    
    /**
     * 更新价格
     */
    fun updatePrice(itemId: Long, newPrice: Double) {
        viewModelScope.launch {
            try {
                wishlistRepository.updatePrice(itemId, newPrice)
                _successMessage.value = "价格已更新"
                loadPriceAlerts() // 重新加载价格提醒
            } catch (e: Exception) {
                _errorMessage.value = "更新价格失败: ${e.message}"
            }
        }
    }
    
    /**
     * 标记为已实现
     */
    fun markAsAchieved(itemId: Long) {
        viewModelScope.launch {
            try {
                wishlistRepository.markAsAchieved(itemId)
                _successMessage.value = "愿望已实现！"
                refreshStats()
            } catch (e: Exception) {
                _errorMessage.value = "标记失败: ${e.message}"
            }
        }
    }
    
    /**
     * 增加查看次数
     */
    fun incrementViewCount(itemId: Long) {
        viewModelScope.launch {
            try {
                wishlistRepository.incrementViewCount(itemId)
            } catch (e: Exception) {
                // 查看次数更新失败不显示错误
            }
        }
    }
    
    // === 筛选和搜索方法 ===
    
    /**
     * 设置搜索关键词
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * 设置筛选条件
     */
    fun setFilter(filter: WishlistFilter) {
        _currentFilter.value = filter
    }
    
    /**
     * 设置排序方式
     */
    fun setSortOrder(sortOrder: WishlistSortOrder) {
        _sortOrder.value = sortOrder
    }
    
    /**
     * 重置所有筛选条件
     */
    fun resetFilters() {
        _currentFilter.value = WishlistFilter()
        _searchQuery.value = ""
        _sortOrder.value = WishlistSortOrder.PRIORITY_DESC
    }
    
    // === 选择模式方法 ===
    
    /**
     * 切换选择模式
     */
    fun toggleSelectionMode() {
        val isCurrentlySelecting = _isSelectionMode.value ?: false
        _isSelectionMode.value = !isCurrentlySelecting
        if (!isCurrentlySelecting) {
            clearSelection()
        }
    }
    
    /**
     * 切换单个物品的选择状态
     */
    fun toggleItemSelection(itemId: Long) {
        val currentSelection = _selectedItems.value ?: emptySet()
        _selectedItems.value = if (currentSelection.contains(itemId)) {
            currentSelection - itemId
        } else {
            currentSelection + itemId
        }
    }
    
    /**
     * 全选/取消全选
     */
    fun toggleSelectAll() {
        val currentItems = wishlistItems.value ?: emptyList()
        val currentSelection = _selectedItems.value ?: emptySet()
        
        _selectedItems.value = if (currentSelection.size == currentItems.size) {
            emptySet()
        } else {
            currentItems.map { it.id }.toSet()
        }
    }
    
    /**
     * 清除选择
     */
    fun clearSelection() {
        _selectedItems.value = emptySet()
    }
    
    // === 批量操作方法 ===
    
    /**
     * 批量更新优先级
     */
    fun batchUpdatePriority(priority: WishlistPriority) {
        val selectedIds = _selectedItems.value?.toList() ?: return
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                wishlistRepository.batchUpdatePriority(selectedIds, priority)
                _successMessage.value = "已更新${selectedIds.size}个物品的优先级"
                clearSelection()
                _isSelectionMode.value = false
            } catch (e: Exception) {
                _errorMessage.value = "批量更新失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 批量删除
     */
    fun batchDelete() {
        val selectedIds = _selectedItems.value?.toList() ?: return
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                wishlistRepository.batchDeleteItems(selectedIds)
                _successMessage.value = "已删除${selectedIds.size}个物品"
                clearSelection()
                _isSelectionMode.value = false
                refreshStats()
            } catch (e: Exception) {
                _errorMessage.value = "批量删除失败: ${e.message}"
            } finally {
                _isLoading.value = false
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
        incrementViewCount(itemId)
        _navigationEvent.value = WishlistNavigationEvent.ItemDetail(itemId)
    }
    
    /**
     * 导航到设置页面
     */
    fun navigateToSettings() {
        _navigationEvent.value = WishlistNavigationEvent.Settings
    }
    
    /**
     * 消费导航事件
     */
    fun onNavigationEventConsumed() {
        _navigationEvent.value = null
    }
    
    // === 消息处理方法 ===
    
    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    /**
     * 清除成功消息
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
    
    // === 私有辅助方法 ===
    
    /**
     * 应用筛选、排序和搜索
     */
    private fun applyFilterSortAndSearch(
        items: List<WishlistItemEntity>,
        filter: WishlistFilter,
        searchQuery: String,
        sortOrder: WishlistSortOrder
    ): List<WishlistItemEntity> {
        var filteredItems = items
        
        // 应用搜索
        if (searchQuery.isNotBlank()) {
            filteredItems = filteredItems.filter { item ->
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.brand?.contains(searchQuery, ignoreCase = true) == true ||
                item.category.contains(searchQuery, ignoreCase = true)
            }
        }
        
        // 应用筛选
        filteredItems = filteredItems.filter { item ->
            (filter.categories.isEmpty() || filter.categories.contains(item.category)) &&
            (filter.priorities.isEmpty() || filter.priorities.contains(item.priority)) &&
            (filter.urgencies.isEmpty() || filter.urgencies.contains(item.urgency)) &&
            (filter.priceRange == null || 
             (item.currentPrice != null && item.currentPrice!! >= filter.priceRange.first && item.currentPrice!! <= filter.priceRange.second)) &&
            (!filter.onlyWithPriceTracking || item.isPriceTrackingEnabled) &&
            (!filter.onlyPriceDropped || item.hasPriceDrop()) &&
            (!filter.onlyReachedTarget || item.hasReachedTargetPrice())
        }
        
        // 应用排序
        return when (sortOrder) {
            WishlistSortOrder.PRIORITY_DESC -> filteredItems.sortedByDescending { it.priority.level }
            WishlistSortOrder.PRIORITY_ASC -> filteredItems.sortedBy { it.priority.level }
            WishlistSortOrder.PRICE_DESC -> filteredItems.sortedByDescending { it.currentPrice ?: 0.0 }
            WishlistSortOrder.PRICE_ASC -> filteredItems.sortedBy { it.currentPrice ?: Double.MAX_VALUE }
            WishlistSortOrder.DATE_DESC -> filteredItems.sortedByDescending { it.addDate }
            WishlistSortOrder.DATE_ASC -> filteredItems.sortedBy { it.addDate }
            WishlistSortOrder.NAME_ASC -> filteredItems.sortedBy { it.name }
            WishlistSortOrder.NAME_DESC -> filteredItems.sortedByDescending { it.name }
            WishlistSortOrder.URGENCY_DESC -> filteredItems.sortedByDescending { it.urgency.level }
        }
    }
}

/**
 * 心愿单筛选条件数据类
 */
data class WishlistFilter(
    val categories: Set<String> = emptySet(),
    val priorities: Set<WishlistPriority> = emptySet(),
    val urgencies: Set<WishlistUrgency> = emptySet(),
    val priceRange: Pair<Double, Double>? = null,
    val onlyWithPriceTracking: Boolean = false,
    val onlyPriceDropped: Boolean = false,
    val onlyReachedTarget: Boolean = false
)

/**
 * 心愿单排序方式枚举
 */
enum class WishlistSortOrder(val displayName: String) {
    PRIORITY_DESC("优先级降序"),
    PRIORITY_ASC("优先级升序"),
    PRICE_DESC("价格降序"),
    PRICE_ASC("价格升序"),
    DATE_DESC("最新添加"),
    DATE_ASC("最早添加"),
    NAME_ASC("名称升序"),
    NAME_DESC("名称降序"),
    URGENCY_DESC("紧急程度降序")
}

/**
 * 心愿单导航事件密封类
 */
sealed class WishlistNavigationEvent {
    object AddItem : WishlistNavigationEvent()
    data class ItemDetail(val itemId: Long) : WishlistNavigationEvent()
    object Settings : WishlistNavigationEvent()
}
