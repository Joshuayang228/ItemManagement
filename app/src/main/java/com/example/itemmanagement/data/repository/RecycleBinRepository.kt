package com.example.itemmanagement.data.repository

import com.example.itemmanagement.data.dao.CategoryCount
import com.example.itemmanagement.data.dao.RecycleBinDao
import com.example.itemmanagement.data.entity.DeletedItemEntity
import com.example.itemmanagement.data.entity.ItemEntity
import com.example.itemmanagement.data.relation.ItemWithDetails
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import java.util.Date

/**
 * 回收站数据仓库
 * 提供回收站相关的业务逻辑和数据操作
 */
class RecycleBinRepository(
    private val recycleBinDao: RecycleBinDao
) {
    
    // ==================== 数据流 ====================
    
    /**
     * 获取回收站中所有物品的实时数据流
     */
    fun getAllDeletedItemsFlow(): Flow<List<DeletedItemEntity>> {
        return recycleBinDao.getAllDeletedItemsFlow()
    }
    
    /**
     * 获取回收站物品数量的实时数据流
     */
    fun getDeletedItemCountFlow(): Flow<Int> {
        return recycleBinDao.getDeletedItemCountFlow()
    }
    
    // ==================== 添加到回收站 ====================
    
    /**
     * 将物品移入回收站
     * 从ItemWithDetails转换为DeletedItemEntity
     */
    suspend fun moveToRecycleBin(
        itemWithDetails: ItemWithDetails,
        deletedReason: String? = null,
        hasWarranty: Boolean = false,
        hasBorrowRecord: Boolean = false
    ) {
        val (item, location, photos, tags) = itemWithDetails
        
        val deletedItem = DeletedItemEntity(
            originalId = item.id,
            name = item.name,
            brand = item.brand,
            specification = item.specification,
            category = item.category,
            quantity = item.quantity,
            unit = item.unit,
            price = item.price,
            purchaseDate = item.purchaseDate,
            expirationDate = item.expirationDate,
            warrantyEndDate = item.warrantyEndDate,
            customNote = item.customNote,
            addDate = item.addDate,
            
            // 位置信息
            locationId = location?.id,
            locationArea = location?.area,
            locationContainer = location?.container,
            locationSublocation = location?.sublocation,
            
            // 照片信息（转换为JSON）
            photoUris = if (photos.isNotEmpty()) {
                JSONArray(photos.map { photo -> photo.uri }).toString()
            } else null,
            
            // 标签信息（转换为JSON）
            tagNames = if (tags.isNotEmpty()) {
                JSONArray(tags.map { tag -> tag.name }).toString()
            } else null,
            
            // 回收站特有信息
            deletedDate = Date(),
            deletedReason = deletedReason,
            canRestore = true,
            hasWarranty = hasWarranty,
            hasBorrowRecord = hasBorrowRecord
        )
        
        recycleBinDao.insert(deletedItem)
    }
    
    /**
     * 批量移入回收站
     */
    suspend fun moveToRecycleBin(
        itemsWithDetails: List<ItemWithDetails>,
        deletedReason: String? = null
    ) {
        val deletedItems = itemsWithDetails.map { itemWithDetails ->
            val (item, location, photos, tags) = itemWithDetails
            
            DeletedItemEntity(
                originalId = item.id,
                name = item.name,
                brand = item.brand,
                specification = item.specification,
                category = item.category,
                quantity = item.quantity,
                unit = item.unit,
                price = item.price,
                purchaseDate = item.purchaseDate,
                expirationDate = item.expirationDate,
                warrantyEndDate = item.warrantyEndDate,
                customNote = item.customNote,
                addDate = item.addDate,
                locationId = location?.id,
                locationArea = location?.area,
                locationContainer = location?.container,
                locationSublocation = location?.sublocation,
                            photoUris = if (photos.isNotEmpty()) {
                JSONArray(photos.map { photo -> photo.uri }).toString()
            } else null,
            tagNames = if (tags.isNotEmpty()) {
                JSONArray(tags.map { tag -> tag.name }).toString()
            } else null,
                deletedDate = Date(),
                deletedReason = deletedReason,
                canRestore = true
            )
        }
        
        recycleBinDao.insertAll(deletedItems)
    }
    
    // ==================== 恢复操作 ====================
    
    /**
     * 从回收站恢复单个物品
     * 返回恢复后的ItemEntity，需要调用方处理实际的恢复逻辑
     */
    suspend fun getItemForRestore(originalId: Long): ItemEntity? {
        val deletedItem = recycleBinDao.getDeletedItemByOriginalId(originalId) ?: return null
        
        if (!deletedItem.canRestore) return null
        
        return ItemEntity(
            id = 0, // 恢复时使用新的ID
            name = deletedItem.name,
            quantity = deletedItem.quantity ?: 1.0,
            unit = deletedItem.unit ?: "个",
            locationId = null, // 恢复时需要重新设置位置
            category = deletedItem.category,
            addDate = Date(), // 恢复时更新添加时间
            productionDate = null,
            expirationDate = deletedItem.expirationDate,
            openStatus = null,
            openDate = null,
            brand = deletedItem.brand,
            specification = deletedItem.specification,
            stockWarningThreshold = null,
            price = deletedItem.price,
            priceUnit = null,
            purchaseChannel = null,
            storeName = null,
            subCategory = null,
            customNote = deletedItem.customNote,
            season = null,
            capacity = null,
            capacityUnit = null,
            rating = null,
            totalPrice = null,
            totalPriceUnit = null,
            purchaseDate = deletedItem.purchaseDate,
            shelfLife = null,
            warrantyPeriod = null,
            warrantyEndDate = deletedItem.warrantyEndDate,
            serialNumber = null
        )
    }
    
    /**
     * 从回收站移除（恢复成功后调用）
     */
    suspend fun removeFromRecycleBin(originalId: Long) {
        recycleBinDao.deleteByOriginalId(originalId)
    }
    
    /**
     * 批量从回收站移除
     */
    suspend fun removeFromRecycleBin(originalIds: List<Long>) {
        recycleBinDao.deleteByOriginalIds(originalIds)
    }
    
    // ==================== 彻底删除 ====================
    
    /**
     * 彻底删除物品（不可恢复）
     */
    suspend fun permanentDelete(originalId: Long) {
        recycleBinDao.deleteByOriginalId(originalId)
    }
    
    /**
     * 批量彻底删除
     */
    suspend fun permanentDelete(originalIds: List<Long>) {
        recycleBinDao.deleteByOriginalIds(originalIds)
    }
    
    /**
     * 清空回收站
     */
    suspend fun clearRecycleBin() {
        recycleBinDao.clearRecycleBin()
    }
    
    // ==================== 查询和搜索 ====================
    
    /**
     * 获取所有已删除物品
     */
    suspend fun getAllDeletedItems(): List<DeletedItemEntity> {
        return recycleBinDao.getAllDeletedItems()
    }
    
    /**
     * 搜索回收站中的物品
     */
    suspend fun searchDeletedItems(query: String): List<DeletedItemEntity> {
        return recycleBinDao.searchDeletedItems(query)
    }
    
    /**
     * 按分类获取已删除物品
     */
    suspend fun getDeletedItemsByCategory(category: String): List<DeletedItemEntity> {
        return recycleBinDao.getDeletedItemsByCategory(category)
    }
    
    // ==================== 统计信息 ====================
    
    /**
     * 获取回收站统计信息
     */
    suspend fun getRecycleBinStats(): RecycleBinStats {
        val totalCount = recycleBinDao.getDeletedItemCount()
        val nearAutoCleanCount = recycleBinDao.getNearAutoCleanCount()
        val categoryStats = recycleBinDao.getDeletedItemCountByCategory()
        
        return RecycleBinStats(
            totalCount = totalCount,
            nearAutoCleanCount = nearAutoCleanCount,
            categoryStats = categoryStats
        )
    }
    
    // ==================== 自动清理 ====================
    
    /**
     * 执行自动清理（清理超过30天的物品）
     * @return 清理的物品数量
     */
    suspend fun performAutoClean(): Int {
        return recycleBinDao.cleanExpiredItems()
    }
    
    /**
     * 获取需要清理的过期物品
     */
    suspend fun getExpiredItems(): List<DeletedItemEntity> {
        return recycleBinDao.getExpiredItems()
    }
    
    // ==================== 检查操作 ====================
    
    /**
     * 检查物品是否在回收站中
     */
    suspend fun isItemInRecycleBin(originalId: Long): Boolean {
        return recycleBinDao.isItemInRecycleBin(originalId)
    }
    
    /**
     * 检查物品是否可以恢复
     */
    suspend fun canItemBeRestored(originalId: Long): Boolean {
        return recycleBinDao.canItemBeRestored(originalId) ?: false
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
