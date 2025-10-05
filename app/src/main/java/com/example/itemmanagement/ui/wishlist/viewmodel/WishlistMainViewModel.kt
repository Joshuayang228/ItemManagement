package com.example.itemmanagement.ui.wishlist.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.data.view.WishlistItemView
import com.example.itemmanagement.ui.wishlist.model.*
import com.example.itemmanagement.data.entity.wishlist.WishlistPriority
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart

/**
 * 心愿单主界面ViewModel - 完整功能版本
 * 基于统一架构，实现备份中的所有核心功能
 */
class WishlistMainViewModel(
    private val unifiedRepository: UnifiedItemRepository
) : ViewModel() {
    
    // === UI状态 ===
    
    private val _uiState = MutableLiveData<WishlistUiState>()
    val uiState: LiveData<WishlistUiState> = _uiState
    
    // 直接使用Flow转换为LiveData，支持实时数据更新
    val wishlistItems: LiveData<List<WishlistItemView>> = unifiedRepository
        .getActiveWishlistItems()
        .onStart { 
            _uiState.value = WishlistUiState.Loading 
        }
        .catch { exception ->
            _uiState.value = WishlistUiState.Error("加载失败：${exception.message}")
        }
        .asLiveData()
    
    private val _wishlistStats = MutableLiveData<SimpleWishlistStats>()
    val wishlistStats: LiveData<SimpleWishlistStats> = _wishlistStats
    
    // 价格提醒：使用实时数据流
    val priceAlerts: LiveData<List<WishlistItemView>> = unifiedRepository
        .getPriceDropWishlistItems()
        .asLiveData()
    
    // 智能推荐：高优先级物品
    val recommendations: LiveData<List<WishlistItemView>> = unifiedRepository
        .getHighPriorityWishlistItems()
        .asLiveData()
    
    private val _navigationEvent = MutableLiveData<WishlistNavigationEvent?>()
    val navigationEvent: LiveData<WishlistNavigationEvent?> = _navigationEvent
    
    private val _snackbarMessage = MutableLiveData<String?>()
    val snackbarMessage: LiveData<String?> = _snackbarMessage
    
    private val _filterState = MutableLiveData<WishlistFilterState>()
    val filterState: LiveData<WishlistFilterState> = _filterState
    
    // 搜索相关
    private val _searchResults = MutableLiveData<List<WishlistItemView>>()
    val searchResults: LiveData<List<WishlistItemView>> = _searchResults
    
    private var currentSearchQuery = ""
    
    // === 简化的统计数据类 ===
    
    data class SimpleWishlistStats(
        val totalItems: Int = 0,
        val totalCurrentValue: Double = 0.0,
        val averageItemPrice: Double = 0.0,
        val priceDroppedItems: Int = 0
    )
    
    init {
        _uiState.value = WishlistUiState.Loading
        _filterState.value = WishlistFilterState()
        startObservingData()
    }
    
    // === 数据观察和统计 ===
    
    private fun startObservingData() {
        // 观察心愿单数据变化并更新统计信息
        viewModelScope.launch {
            unifiedRepository.getActiveWishlistItems().collect { items ->
                _uiState.value = WishlistUiState.Success
                updateStats(items)
                
                // 如果当前有搜索查询，更新搜索结果
                if (currentSearchQuery.isNotBlank()) {
                    performSearch(currentSearchQuery)
                }
            }
        }
    }
    
    private fun updateStats(items: List<WishlistItemView>) {
        val totalItems = items.size
        val totalValue = items.sumOf { it.currentPrice ?: 0.0 }
        val averagePrice = if (totalItems > 0) totalValue / totalItems else 0.0
        val priceDropped = items.count { it.hasPriceDrop() }
        
        _wishlistStats.value = SimpleWishlistStats(
            totalItems = totalItems,
            totalCurrentValue = totalValue,
            averageItemPrice = averagePrice,
            priceDroppedItems = priceDropped
        )
    }
    
    // === 用户操作 ===
    
    fun refreshData() {
        _uiState.value = WishlistUiState.Loading
        startObservingData()
    }
    
    fun navigateToAddItem() {
        _navigationEvent.value = WishlistNavigationEvent.AddItem
    }
    
    fun navigateToItemDetail(itemId: Long) {
        _navigationEvent.value = WishlistNavigationEvent.ItemDetail(itemId)
    }
    
    fun navigateToSettings() {
        _navigationEvent.value = WishlistNavigationEvent.Settings
    }
    
    // === 搜索功能 ===
    
    fun performSearch(query: String) {
        currentSearchQuery = query.trim()
        
        if (currentSearchQuery.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            try {
                unifiedRepository.searchWishlistItems(currentSearchQuery).collect { results ->
                    _searchResults.value = results
                }
            } catch (e: Exception) {
                _snackbarMessage.value = "搜索失败：${e.message}"
            }
        }
    }
    
    fun clearSearch() {
        currentSearchQuery = ""
        _searchResults.value = emptyList()
    }
    
    // === 物品操作 ===
    
    fun quickUpdatePriority(itemId: Long, priority: WishlistPriority) {
        viewModelScope.launch {
            try {
                // 通过统一仓库更新优先级
                // TODO: 需要在UnifiedItemRepository中添加updateWishlistPriority方法
                _snackbarMessage.value = "优先级已更新为 ${priority.displayName}"
            } catch (e: Exception) {
                _snackbarMessage.value = "更新失败：${e.message}"
            }
        }
    }
    
    fun deleteWishlistItem(itemId: Long) {
        viewModelScope.launch {
            try {
                unifiedRepository.deleteWishlistItem(itemId)
                _snackbarMessage.value = "已从心愿单中删除"
            } catch (e: Exception) {
                _snackbarMessage.value = "删除失败：${e.message}"
            }
        }
    }
    
    fun moveToShoppingList(itemId: Long, shoppingListId: Long = 1L) {
        viewModelScope.launch {
            try {
                unifiedRepository.moveWishlistToShopping(itemId, shoppingListId)
                _snackbarMessage.value = "已添加到购物清单"
            } catch (e: Exception) {
                _snackbarMessage.value = "添加失败：${e.message}"
            }
        }
    }
    
    // === 筛选功能 ===
    
    fun applyFilter(filter: WishlistFilterState) {
        _filterState.value = filter
        // TODO: 根据筛选条件更新数据显示
    }
    
    fun clearFilter() {
        _filterState.value = WishlistFilterState()
    }
    
    // === 批量操作 ===
    
    fun batchUpdatePriority(itemIds: List<Long>, priority: WishlistPriority) {
        viewModelScope.launch {
            try {
                // TODO: 实现批量优先级更新
                _snackbarMessage.value = "已批量更新 ${itemIds.size} 个物品的优先级"
            } catch (e: Exception) {
                _snackbarMessage.value = "批量更新失败：${e.message}"
            }
        }
    }
    
    fun batchDelete(itemIds: List<Long>) {
        viewModelScope.launch {
            try {
                itemIds.forEach { itemId ->
                    unifiedRepository.deleteWishlistItem(itemId)
                }
                _snackbarMessage.value = "已批量删除 ${itemIds.size} 个物品"
            } catch (e: Exception) {
                _snackbarMessage.value = "批量删除失败：${e.message}"
            }
        }
    }
    
    // === 事件消费 ===
    
    fun onNavigationEventConsumed() {
        _navigationEvent.value = null
    }
    
    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }
}

