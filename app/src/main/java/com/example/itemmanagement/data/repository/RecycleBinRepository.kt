package com.example.itemmanagement.data.repository

import com.example.itemmanagement.data.dao.unified.*
import com.example.itemmanagement.data.entity.unified.*
import com.example.itemmanagement.data.view.DeletedItemView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.util.Date

/**
 * 统一架构的回收站数据仓库
 * 基于ItemStateEntity来管理删除状态
 */
class RecycleBinRepository(
    private val unifiedItemDao: UnifiedItemDao,
    private val itemStateDao: ItemStateDao
) {
    
    // ==================== 数据流 ====================
    
    /**
     * 获取所有已删除物品
     */
    fun getAllDeletedItems(): Flow<List<DeletedItemView>> {
        return itemStateDao.getActiveStatesByType(ItemStateType.DELETED).map { states ->
            states.mapNotNull { state ->
                val item = unifiedItemDao.getById(state.itemId)
                item?.let {
                    // 查找删除前的最后一个活跃状态
                    val previousState = itemStateDao.getByItemId(state.itemId)
                        .filter { s -> s.stateType != ItemStateType.DELETED && !s.isActive }
                        .maxByOrNull { s -> s.deactivatedDate ?: Date(0) }
                    
                    DeletedItemView(
                        unifiedItem = it,
                        deletedDate = state.activatedDate,
                        deletedReason = state.notes,
                        previousStateType = previousState?.stateType
                    )
                }
            }
        }
    }
    
    /**
     * 获取最近删除的物品
     */
    fun getRecentDeletedItems(days: Int = 30): Flow<List<DeletedItemView>> {
        val startDate = Date(System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L)
        return getAllDeletedItems().map { items ->
            items.filter { it.deletedDate >= startDate }
        }
    }
    
    /**
     * 获取回收站物品数量
     */
    suspend fun getDeletedItemCount(): Int {
        return itemStateDao.getActiveItemCountByStateType(ItemStateType.DELETED)
    }
    
    // ==================== 基本操作 ====================
    
    /**
     * 将物品移入回收站
     */
    suspend fun moveToRecycleBin(itemId: Long, reason: String = "用户删除") {
        // 1. 停用所有当前状态
        itemStateDao.deactivateAllStates(itemId)
        
        // 2. 添加删除状态
        val deleteState = ItemStateEntity(
            itemId = itemId,
            stateType = ItemStateType.DELETED,
            notes = reason,
            activatedDate = Date(),
            isActive = true
        )
        
        itemStateDao.insert(deleteState)
    }
    
    /**
     * 批量移入回收站
     */
    suspend fun moveToRecycleBin(itemIds: List<Long>, reason: String = "批量删除") {
        itemIds.forEach { itemId ->
            moveToRecycleBin(itemId, reason)
        }
    }
    
    /**
     * 从回收站恢复物品
     */
    suspend fun restoreFromRecycleBin(itemId: Long, targetState: ItemStateType = ItemStateType.INVENTORY) {
        // 1. 停用删除状态
        itemStateDao.deactivateState(itemId, ItemStateType.DELETED)
        
        // 2. 恢复到目标状态
        val restoreState = ItemStateEntity(
            itemId = itemId,
            stateType = targetState,
            notes = "从回收站恢复",
            activatedDate = Date(),
            isActive = true
        )
        
        itemStateDao.insert(restoreState)
    }
    
    /**
     * 永久删除物品
     */
    suspend fun permanentlyDelete(itemId: Long) {
        // 1. 删除所有状态记录
        itemStateDao.deleteAllStatesForItem(itemId)
        
        // 2. 删除统一项目记录
        unifiedItemDao.deleteById(itemId)
        
        // 注意：关联的详情记录会通过外键约束自动删除
    }
    
    /**
     * 批量永久删除
     */
    suspend fun permanentlyDelete(itemIds: List<Long>) {
        itemIds.forEach { itemId ->
            permanentlyDelete(itemId)
        }
    }
    
    /**
     * 清空回收站
     */
    suspend fun clearRecycleBin() {
        val deletedStates = itemStateDao.getActiveStatesByType(ItemStateType.DELETED).first()
        deletedStates.forEach { state ->
            permanentlyDelete(state.itemId)
        }
    }
    
    // ==================== 查询操作 ====================
    
    /**
     * 搜索回收站中的物品
     */
    suspend fun searchDeletedItems(query: String): List<DeletedItemView> {
        // 获取所有删除状态的物品，然后筛选
        val allDeletedItems = getAllDeletedItems()
        // 这里需要实现搜索逻辑，简化实现返回空列表
        return emptyList()
    }
    
    /**
     * 按分类获取已删除物品
     */
    suspend fun getDeletedItemsByCategory(category: String): List<DeletedItemView> {
        val allDeletedItems = getAllDeletedItems()
        // 这里需要实现分类筛选逻辑
        return emptyList()
    }
    
    /**
     * 检查物品是否在回收站中
     */
    suspend fun isItemInRecycleBin(itemId: Long): Boolean {
        val states = itemStateDao.getByItemId(itemId)
        return states.any { it.stateType == ItemStateType.DELETED && it.isActive }
    }
    
    /**
     * 检查物品是否可以恢复
     */
    suspend fun canItemBeRestored(itemId: Long): Boolean {
        return isItemInRecycleBin(itemId)
    }
    
    // ==================== 自动清理 ====================
    
    /**
     * 执行自动清理（清理超过30天的物品）
     */
    suspend fun performAutoClean(): Int {
        val expiredDate = Date(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L)
        val allDeletedStates = itemStateDao.getActiveStatesByType(ItemStateType.DELETED).first()
        val expiredItems = allDeletedStates.filter { it.activatedDate.before(expiredDate) }
        
        expiredItems.forEach { state ->
            permanentlyDelete(state.itemId)
        }
        
        return expiredItems.size
    }
    
    /**
     * 获取需要清理的过期物品
     */
    suspend fun getExpiredItems(): List<DeletedItemView> {
        val expiredDate = Date(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L)
        val allDeletedStates = itemStateDao.getActiveStatesByType(ItemStateType.DELETED).first()
        val expiredStates = allDeletedStates.filter { it.activatedDate.before(expiredDate) }
        
        return expiredStates.mapNotNull { state ->
            val item = unifiedItemDao.getById(state.itemId)
            item?.let {
                DeletedItemView(
                    unifiedItem = it,
                    deletedDate = state.activatedDate,
                    deletedReason = state.notes
                )
            }
        }
    }
}

/**
 * 回收站统计信息
 */
data class RecycleBinStats(
    val totalCount: Int,
    val nearAutoCleanCount: Int,
    val categoryStats: List<CategoryCount>
)

/**
 * 分类统计信息
 */
data class CategoryCount(
    val category: String,
    val count: Int
)