package com.example.itemmanagement.ui.wishlist.viewmodel

import androidx.lifecycle.*
import com.example.itemmanagement.data.entity.wishlist.WishlistItemEntity
import com.example.itemmanagement.data.entity.wishlist.WishlistPriority
import com.example.itemmanagement.data.repository.WishlistRepository
import kotlinx.coroutines.launch

/**
 * 心愿单选择模式ViewModel
 * 专门处理多选操作和批量操作的业务逻辑
 * 遵循MVVM原则，保持职责单一
 */
class WishlistSelectionViewModel(
    private val wishlistRepository: WishlistRepository
) : ViewModel() {
    
    // === 选择状态管理 ===
    
    private val _isSelectionMode = MutableLiveData<Boolean>(false)
    val isSelectionMode: LiveData<Boolean> = _isSelectionMode
    
    private val _selectedItems = MutableLiveData<Set<Long>>(emptySet())
    val selectedItems: LiveData<Set<Long>> = _selectedItems
    
    val selectedItemsCount: LiveData<Int> = _selectedItems.map { it.size }
    
    private val _isAllSelected = MutableLiveData<Boolean>(false)
    val isAllSelected: LiveData<Boolean> = _isAllSelected
    
    // === 操作状态管理 ===
    
    private val _isProcessing = MutableLiveData<Boolean>(false)
    val isProcessing: LiveData<Boolean> = _isProcessing
    
    private val _operationResult = MutableLiveData<SelectionOperationResult?>()
    val operationResult: LiveData<SelectionOperationResult?> = _operationResult
    
    // 当前显示的物品列表（用于全选判断）
    private var currentItems: List<WishlistItemEntity> = emptyList()
    
    // === 选择模式控制 ===
    
    /**
     * 启动选择模式
     */
    fun startSelectionMode() {
        _isSelectionMode.value = true
        clearSelection()
    }
    
    /**
     * 退出选择模式
     */
    fun exitSelectionMode() {
        _isSelectionMode.value = false
        clearSelection()
    }
    
    /**
     * 切换选择模式
     */
    fun toggleSelectionMode() {
        if (_isSelectionMode.value == true) {
            exitSelectionMode()
        } else {
            startSelectionMode()
        }
    }
    
    // === 选择操作 ===
    
    /**
     * 切换单个物品的选择状态
     */
    fun toggleItemSelection(itemId: Long) {
        val currentSelection = _selectedItems.value ?: emptySet()
        val newSelection = if (currentSelection.contains(itemId)) {
            currentSelection - itemId
        } else {
            currentSelection + itemId
        }
        _selectedItems.value = newSelection
        updateAllSelectedState()
    }
    
    /**
     * 选择单个物品
     */
    fun selectItem(itemId: Long) {
        val currentSelection = _selectedItems.value ?: emptySet()
        _selectedItems.value = currentSelection + itemId
        updateAllSelectedState()
    }
    
    /**
     * 取消选择单个物品
     */
    fun deselectItem(itemId: Long) {
        val currentSelection = _selectedItems.value ?: emptySet()
        _selectedItems.value = currentSelection - itemId
        updateAllSelectedState()
    }
    
    /**
     * 全选/取消全选
     */
    fun toggleSelectAll() {
        val currentSelection = _selectedItems.value ?: emptySet()
        val allItemIds = currentItems.map { it.id }.toSet()
        
        _selectedItems.value = if (currentSelection.size == allItemIds.size && allItemIds.isNotEmpty()) {
            emptySet() // 全部选中时，取消全选
        } else {
            allItemIds // 否则全选
        }
        updateAllSelectedState()
    }
    
    /**
     * 清除所有选择
     */
    fun clearSelection() {
        _selectedItems.value = emptySet()
        updateAllSelectedState()
    }
    
    /**
     * 更新当前显示的物品列表
     * （由主ViewModel调用，用于全选状态判断）
     */
    fun updateCurrentItems(items: List<WishlistItemEntity>) {
        currentItems = items
        updateAllSelectedState()
        
        // 移除不在当前列表中的选中项
        val currentSelection = _selectedItems.value ?: emptySet()
        val validItemIds = items.map { it.id }.toSet()
        val validSelection = currentSelection.intersect(validItemIds)
        
        if (validSelection != currentSelection) {
            _selectedItems.value = validSelection
        }
    }
    
    // === 批量操作 ===
    
    /**
     * 批量更新优先级
     */
    fun batchUpdatePriority(priority: WishlistPriority) {
        val selectedIds = _selectedItems.value?.toList() ?: return
        if (selectedIds.isEmpty()) {
            _operationResult.value = SelectionOperationResult.Error("未选择任何物品")
            return
        }
        
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                wishlistRepository.batchUpdatePriority(selectedIds, priority)
                
                val message = "已更新 ${selectedIds.size} 个物品的优先级为 ${priority.displayName}"
                _operationResult.value = SelectionOperationResult.Success(message)
                
                exitSelectionMode()
                
            } catch (e: Exception) {
                _operationResult.value = SelectionOperationResult.Error("批量更新失败: ${e.message}")
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * 批量删除
     */
    fun batchDelete() {
        val selectedIds = _selectedItems.value?.toList() ?: return
        if (selectedIds.isEmpty()) {
            _operationResult.value = SelectionOperationResult.Error("未选择任何物品")
            return
        }
        
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                wishlistRepository.batchDeleteItems(selectedIds)
                
                val message = "已删除 ${selectedIds.size} 个物品"
                _operationResult.value = SelectionOperationResult.Success(message)
                
                exitSelectionMode()
                
            } catch (e: Exception) {
                _operationResult.value = SelectionOperationResult.Error("批量删除失败: ${e.message}")
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * 批量标记为已实现
     */
    fun batchMarkAsAchieved() {
        val selectedIds = _selectedItems.value?.toList() ?: return
        if (selectedIds.isEmpty()) {
            _operationResult.value = SelectionOperationResult.Error("未选择任何物品")
            return
        }
        
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                
                // 逐个标记为已实现（目前Repository没有批量实现方法）
                selectedIds.forEach { itemId ->
                    wishlistRepository.markAsAchieved(itemId)
                }
                
                val message = "已标记 ${selectedIds.size} 个愿望为已实现！"
                _operationResult.value = SelectionOperationResult.Success(message)
                
                exitSelectionMode()
                
            } catch (e: Exception) {
                _operationResult.value = SelectionOperationResult.Error("批量标记失败: ${e.message}")
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * 批量暂停价格跟踪
     */
    fun batchPausePriceTracking() {
        val selectedIds = _selectedItems.value?.toList() ?: return
        if (selectedIds.isEmpty()) {
            _operationResult.value = SelectionOperationResult.Error("未选择任何物品")
            return
        }
        
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                
                // 逐个暂停价格跟踪
                selectedIds.forEach { itemId ->
                    wishlistRepository.setPauseStatus(itemId, true)
                }
                
                val message = "已暂停 ${selectedIds.size} 个物品的价格跟踪"
                _operationResult.value = SelectionOperationResult.Success(message)
                
                exitSelectionMode()
                
            } catch (e: Exception) {
                _operationResult.value = SelectionOperationResult.Error("批量暂停失败: ${e.message}")
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    // === 辅助方法 ===
    
    /**
     * 更新全选状态
     */
    private fun updateAllSelectedState() {
        val currentSelection = _selectedItems.value ?: emptySet()
        val allItemIds = currentItems.map { it.id }.toSet()
        _isAllSelected.value = currentSelection.size == allItemIds.size && allItemIds.isNotEmpty()
    }
    
    /**
     * 检查物品是否被选中
     */
    fun isItemSelected(itemId: Long): Boolean {
        return _selectedItems.value?.contains(itemId) ?: false
    }
    
    /**
     * 获取选中的物品数量
     */
    fun getSelectedCount(): Int {
        return _selectedItems.value?.size ?: 0
    }
    
    /**
     * 清除操作结果
     */
    fun clearOperationResult() {
        _operationResult.value = null
    }
    
    /**
     * 检查是否有选中的物品
     */
    fun hasSelectedItems(): Boolean {
        return (_selectedItems.value?.size ?: 0) > 0
    }
}

/**
 * 选择操作结果密封类
 */
sealed class SelectionOperationResult {
    data class Success(val message: String) : SelectionOperationResult()
    data class Error(val message: String) : SelectionOperationResult()
}

