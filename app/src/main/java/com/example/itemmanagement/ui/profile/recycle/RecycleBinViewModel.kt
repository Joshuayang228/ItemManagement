package com.example.itemmanagement.ui.profile.recycle

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.entity.DeletedItemEntity
import com.example.itemmanagement.data.repository.RecycleBinRepository
import kotlinx.coroutines.launch

/**
 * 回收站页面的ViewModel
 * 管理已删除物品的显示、恢复、删除等操作
 */
class RecycleBinViewModel(
    private val recycleBinRepository: RecycleBinRepository
) : ViewModel() {
    
    // ==================== 数据 ====================
    
    /**
     * 所有已删除物品的数据流
     */
    val deletedItems: LiveData<List<DeletedItemEntity>> = 
        recycleBinRepository.getAllDeletedItemsFlow().asLiveData()
    
    /**
     * 回收站物品数量
     */
    val deletedItemCount: LiveData<Int> = 
        recycleBinRepository.getDeletedItemCountFlow().asLiveData()
    
    /**
     * 操作结果
     */
    private val _operationResult = MutableLiveData<OperationResult>()
    val operationResult: LiveData<OperationResult> = _operationResult
    
    /**
     * 加载状态
     */
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    /**
     * 选中的物品ID集合（批量操作用）
     */
    private val _selectedItems = MutableLiveData<Set<Long>>(emptySet())
    val selectedItems: LiveData<Set<Long>> = _selectedItems
    
    /**
     * 是否处于选择模式
     */
    private val _isSelectMode = MutableLiveData<Boolean>(false)
    val isSelectMode: LiveData<Boolean> = _isSelectMode
    
    /**
     * 搜索关键词
     */
    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery
    
    /**
     * 搜索结果
     */
    private val _searchResults = MutableLiveData<List<DeletedItemEntity>>()
    val searchResults: LiveData<List<DeletedItemEntity>> = _searchResults
    
    /**
     * 回收站统计信息
     */
    private val _recycleBinStats = MutableLiveData<RecycleBinStats>()
    val recycleBinStats: LiveData<RecycleBinStats> = _recycleBinStats
    
    // ==================== 初始化 ====================
    
    init {
        loadRecycleBinStats()
    }
    
    // ==================== 恢复操作 ====================
    
    /**
     * 恢复单个物品
     */
    fun restoreItem(originalId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 获取要恢复的物品信息
                val itemEntity = recycleBinRepository.getItemForRestore(originalId)
                if (itemEntity != null) {
                    // TODO: 这里需要调用ItemRepository来实际恢复物品
                    // 现在先从回收站移除，实际恢复逻辑需要在调用方处理
                    recycleBinRepository.removeFromRecycleBin(originalId)
                    
                    _operationResult.value = OperationResult(
                        success = true,
                        message = "物品已恢复",
                        restoredItemEntity = itemEntity
                    )
                } else {
                    _operationResult.value = OperationResult(
                        success = false,
                        message = "物品无法恢复，可能已损坏"
                    )
                }
            } catch (e: Exception) {
                _operationResult.value = OperationResult(
                    success = false,
                    message = "恢复失败：${e.localizedMessage}"
                )
            } finally {
                _isLoading.value = false
                refreshStats()
            }
        }
    }
    
    /**
     * 批量恢复物品
     */
    fun restoreSelectedItems() {
        val selectedIds = _selectedItems.value ?: return
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            var successCount = 0
            val restoredItems = mutableListOf<com.example.itemmanagement.data.entity.ItemEntity>()
            
            try {
                selectedIds.forEach { originalId ->
                    try {
                        val itemEntity = recycleBinRepository.getItemForRestore(originalId)
                        if (itemEntity != null) {
                            recycleBinRepository.removeFromRecycleBin(originalId)
                            restoredItems.add(itemEntity)
                            successCount++
                        }
                    } catch (e: Exception) {
                        // 单个物品恢复失败，继续处理其他物品
                    }
                }
                
                _operationResult.value = OperationResult(
                    success = true,
                    message = "已恢复 $successCount 个物品",
                    restoredItems = restoredItems
                )
                
                // 清除选择
                clearSelection()
                
            } catch (e: Exception) {
                _operationResult.value = OperationResult(
                    success = false,
                    message = "批量恢复失败：${e.localizedMessage}"
                )
            } finally {
                _isLoading.value = false
                refreshStats()
            }
        }
    }
    
    // ==================== 彻底删除操作 ====================
    
    /**
     * 彻底删除单个物品
     */
    fun permanentDeleteItem(originalId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                recycleBinRepository.permanentDelete(originalId)
                _operationResult.value = OperationResult(
                    success = true,
                    message = "物品已彻底删除"
                )
            } catch (e: Exception) {
                _operationResult.value = OperationResult(
                    success = false,
                    message = "删除失败：${e.localizedMessage}"
                )
            } finally {
                _isLoading.value = false
                refreshStats()
            }
        }
    }
    
    /**
     * 批量彻底删除
     */
    fun permanentDeleteSelectedItems() {
        val selectedIds = _selectedItems.value ?: return
        if (selectedIds.isEmpty()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                recycleBinRepository.permanentDelete(selectedIds.toList())
                _operationResult.value = OperationResult(
                    success = true,
                    message = "已彻底删除 ${selectedIds.size} 个物品"
                )
                clearSelection()
            } catch (e: Exception) {
                _operationResult.value = OperationResult(
                    success = false,
                    message = "批量删除失败：${e.localizedMessage}"
                )
            } finally {
                _isLoading.value = false
                refreshStats()
            }
        }
    }
    
    /**
     * 清空回收站
     */
    fun clearRecycleBin() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                recycleBinRepository.clearRecycleBin()
                _operationResult.value = OperationResult(
                    success = true,
                    message = "回收站已清空"
                )
                clearSelection()
            } catch (e: Exception) {
                _operationResult.value = OperationResult(
                    success = false,
                    message = "清空失败：${e.localizedMessage}"
                )
            } finally {
                _isLoading.value = false
                refreshStats()
            }
        }
    }
    
    // ==================== 搜索功能 ====================
    
    /**
     * 搜索已删除物品
     */
    fun searchDeletedItems(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            try {
                val results = recycleBinRepository.searchDeletedItems(query)
                _searchResults.value = results
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            }
        }
    }
    
    // ==================== 选择管理 ====================
    
    /**
     * 切换选择模式
     */
    fun toggleSelectMode() {
        _isSelectMode.value = !(_isSelectMode.value ?: false)
        if (!(_isSelectMode.value ?: false)) {
            clearSelection()
        }
    }
    
    /**
     * 选择/取消选择物品
     */
    fun toggleItemSelection(originalId: Long) {
        val currentSelection = _selectedItems.value ?: emptySet()
        _selectedItems.value = if (originalId in currentSelection) {
            currentSelection - originalId
        } else {
            currentSelection + originalId
        }
    }
    
    /**
     * 全选
     */
    fun selectAll() {
        val allItems = deletedItems.value ?: return
        _selectedItems.value = allItems.map { it.originalId }.toSet()
    }
    
    /**
     * 清除选择
     */
    fun clearSelection() {
        _selectedItems.value = emptySet()
        _isSelectMode.value = false
    }
    
    // ==================== 自动清理 ====================
    
    /**
     * 执行自动清理（清理超过30天的物品）
     */
    fun performAutoClean() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val cleanCount = recycleBinRepository.performAutoClean()
                _operationResult.value = OperationResult(
                    success = true,
                    message = "已清理 $cleanCount 个过期物品"
                )
            } catch (e: Exception) {
                _operationResult.value = OperationResult(
                    success = false,
                    message = "自动清理失败：${e.localizedMessage}"
                )
            } finally {
                _isLoading.value = false
                refreshStats()
            }
        }
    }
    
    // ==================== 统计信息 ====================
    
    /**
     * 加载回收站统计信息
     */
    private fun loadRecycleBinStats() {
        viewModelScope.launch {
            try {
                val stats = recycleBinRepository.getRecycleBinStats()
                _recycleBinStats.value = RecycleBinStats(
                    totalCount = stats.totalCount,
                    nearAutoCleanCount = stats.nearAutoCleanCount,
                    categoryStats = stats.categoryStats
                )
            } catch (e: Exception) {
                // 统计信息加载失败，使用默认值
                _recycleBinStats.value = RecycleBinStats(0, 0, emptyList())
            }
        }
    }
    
    /**
     * 刷新统计信息
     */
    private fun refreshStats() {
        loadRecycleBinStats()
    }
    
    /**
     * 清除操作结果
     */
    fun clearOperationResult() {
        _operationResult.value = null
    }
}

/**
 * 操作结果数据类
 */
data class OperationResult(
    val success: Boolean,
    val message: String,
    val restoredItemEntity: com.example.itemmanagement.data.entity.ItemEntity? = null,
    val restoredItems: List<com.example.itemmanagement.data.entity.ItemEntity> = emptyList()
)

/**
 * 回收站统计信息
 */
data class RecycleBinStats(
    val totalCount: Int,
    val nearAutoCleanCount: Int,
    val categoryStats: List<com.example.itemmanagement.data.dao.CategoryCount>
)
