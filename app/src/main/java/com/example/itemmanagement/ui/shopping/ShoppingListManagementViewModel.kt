package com.example.itemmanagement.ui.shopping

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.data.entity.ShoppingListEntity
import com.example.itemmanagement.data.entity.ShoppingListStatus
import com.example.itemmanagement.data.entity.ShoppingListType
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.example.itemmanagement.adapter.ShoppingListProgress

/**
 * 购物概览统计数据类
 */
data class ShoppingOverviewStats(
    val activeListsCount: Int,
    val totalPendingItems: Int,
    val totalBudget: Double
)

/**
 * 购物清单管理ViewModel
 * 处理购物清单的创建、编辑、删除等操作
 */
class ShoppingListManagementViewModel(
    application: Application,
    private val repository: UnifiedItemRepository
) : AndroidViewModel(application) {
    
    // 所有购物清单
    val allShoppingLists: LiveData<List<ShoppingListEntity>> = 
        repository.getAllShoppingLists().asLiveData()
    
    // 活跃的购物清单
    val activeShoppingLists: LiveData<List<ShoppingListEntity>> = 
        repository.getActiveShoppingLists().asLiveData()
    
    // 当前选中的购物清单
    private val _currentShoppingList = MutableLiveData<ShoppingListEntity?>()
    val currentShoppingList: LiveData<ShoppingListEntity?> = _currentShoppingList
    
    // 操作状态
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    // 导航事件
    private val _navigateToListDetail = MutableLiveData<Long?>()
    val navigateToListDetail: LiveData<Long?> = _navigateToListDetail
    
    // 概览统计数据
    private val _overviewStats = MutableLiveData<ShoppingOverviewStats>()
    val overviewStats: LiveData<ShoppingOverviewStats> = _overviewStats
    
    /**
     * 创建新的购物清单
     */
    fun createShoppingList(
        name: String,
        description: String? = null,
        type: ShoppingListType = ShoppingListType.DAILY,
        estimatedBudget: Double? = null
    ) {
        if (name.isBlank()) {
            _error.value = "清单名称不能为空"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newList = ShoppingListEntity(
                    name = name.trim(),
                    description = description?.trim(),
                    type = type,
                    status = ShoppingListStatus.ACTIVE,
                    estimatedBudget = estimatedBudget
                )
                
                val listId = repository.insertShoppingList(newList)
                _message.value = "购物清单「$name」创建成功"
                
                // 导航到新创建的清单详情
                _navigateToListDetail.value = listId
                
            } catch (e: Exception) {
                _error.value = "创建清单失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 更新购物清单
     */
    fun updateShoppingList(shoppingList: ShoppingListEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateShoppingList(shoppingList)
                _message.value = "清单「${shoppingList.name}」更新成功"
                
                // 更新当前选中的清单
                _currentShoppingList.value = shoppingList
                
            } catch (e: Exception) {
                _error.value = "更新清单失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 删除购物清单
     */
    fun deleteShoppingList(shoppingList: ShoppingListEntity) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteShoppingList(shoppingList.id)
                _message.value = "清单「${shoppingList.name}」已删除"
                
                // 如果删除的是当前选中的清单，清空选中状态
                if (_currentShoppingList.value?.id == shoppingList.id) {
                    _currentShoppingList.value = null
                }
                
            } catch (e: Exception) {
                _error.value = "删除清单失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 根据ID获取购物清单（同步方法）
     */
    fun getShoppingListById(id: Long): ShoppingListEntity? {
        return kotlinx.coroutines.runBlocking {
            try {
                repository.getShoppingListById(id)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * 获取购物清单的物品数量
     */
    suspend fun getShoppingItemsCount(listId: Long): Int {
        return repository.getShoppingItemsCountByListId(listId)
    }
    
    /**
     * 获取购物清单的已购买物品数量
     */
    suspend fun getPurchasedItemsCount(listId: Long): Int {
        return repository.getPurchasedShoppingItemsCountByListId(listId)
    }
    
    /**
     * 设置当前购物清单
     */
    fun setCurrentShoppingList(shoppingList: ShoppingListEntity) {
        _currentShoppingList.value = shoppingList
    }
    
    /**
     * 完成购物清单（标记为已完成）- 通过ID
     */
    fun completeShoppingList(listId: Long) {
        viewModelScope.launch {
            try {
                val shoppingList = repository.getShoppingListById(listId)
                if (shoppingList != null) {
                    val completedList = shoppingList.copy(
                        status = ShoppingListStatus.COMPLETED
                    )
                    repository.updateShoppingList(completedList)
                    _message.value = "清单已完成"
                }
            } catch (e: Exception) {
                _error.value = "完成清单失败: ${e.message}"
            }
        }
    }
    
    /**
     * 重新激活购物清单
     */
    fun reactivateShoppingList(shoppingList: ShoppingListEntity) {
        val activeList = shoppingList.copy(
            status = ShoppingListStatus.ACTIVE
        )
        updateShoppingList(activeList)
    }
    
    /**
     * 导航到清单详情完成后重置导航状态
     */
    fun navigateToListDetailComplete() {
        _navigateToListDetail.value = null
    }
    
    /**
     * 获取购物清单的真实进度数据
     */
    suspend fun getShoppingListProgress(listId: Long): ShoppingListProgress {
        return try {
            val totalItems = repository.getShoppingItemsCountByListId(listId)
            val pendingItems = repository.getPendingShoppingItemsCountByListId(listId)
            val completedItems = totalItems - pendingItems
            
            val progress = if (totalItems > 0) {
                (completedItems * 100) / totalItems
            } else {
                0
            }
            
            ShoppingListProgress(totalItems, completedItems, progress)
        } catch (e: Exception) {
            // 如果出错，返回默认值
            ShoppingListProgress(0, 0, 0)
        }
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _error.value = ""
    }
    
    /**
     * 清除消息
     */
    fun clearMessage() {
        _message.value = ""
    }
    
    /**
     * 加载购物概览统计数据
     */
    fun loadOverviewStats() {
        viewModelScope.launch {
            try {
                val activeLists = repository.getActiveShoppingLists().first()
                val activeListsCount = activeLists.size
                
                // 计算所有活跃清单的待购买物品总数
                var totalPendingItems = 0
                for (list in activeLists) {
                    totalPendingItems += repository.getPendingShoppingItemsCountByListId(list.id)
                }
                
                // 计算所有活跃清单的总预算
                val totalBudget = activeLists.sumOf { it.estimatedBudget ?: 0.0 }
                
                _overviewStats.value = ShoppingOverviewStats(
                    activeListsCount = activeListsCount,
                    totalPendingItems = totalPendingItems,
                    totalBudget = totalBudget
                )
            } catch (e: Exception) {
                _error.value = "获取概览数据失败: ${e.message}"
            }
        }
    }
    
    /**
     * 获取清单类型的显示名称
     */
    fun getListTypeDisplayName(type: ShoppingListType): String {
        return when (type) {
            ShoppingListType.DAILY -> "日常补充"
            ShoppingListType.WEEKLY -> "周采购"
            ShoppingListType.PARTY -> "聚会准备"
            ShoppingListType.TRAVEL -> "旅行购物"
            ShoppingListType.SPECIAL -> "特殊场合"
            ShoppingListType.CUSTOM -> "自定义"
        }
    }
    
    /**
     * 获取清单状态的显示名称
     */
    fun getListStatusDisplayName(status: ShoppingListStatus): String {
        return when (status) {
            ShoppingListStatus.ACTIVE -> "进行中"
            ShoppingListStatus.COMPLETED -> "已完成"
            ShoppingListStatus.ARCHIVED -> "已归档"
        }
    }
} 