package com.example.itemmanagement.data.repository

import androidx.room.withTransaction
import com.example.itemmanagement.data.AppDatabase
import com.example.itemmanagement.data.dao.wishlist.CategoryCount
import com.example.itemmanagement.data.dao.wishlist.PriorityCount
import com.example.itemmanagement.data.dao.wishlist.WishlistDao
import com.example.itemmanagement.data.dao.wishlist.WishlistPriceHistoryDao
import com.example.itemmanagement.data.entity.wishlist.*
import com.example.itemmanagement.data.model.wishlist.CategoryStat
import com.example.itemmanagement.data.model.wishlist.WishlistItemDetails
import com.example.itemmanagement.data.model.wishlist.WishlistStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import java.util.*
// 移除依赖注入注解，使用简单的构造函数

/**
 * 心愿单数据仓库
 * 遵循MVVM架构，作为Model层为ViewModel提供数据操作接口
 * 封装数据源复杂性，提供统一的业务数据访问
 */
class WishlistRepository(
    private val database: AppDatabase,
    private val wishlistDao: WishlistDao,
    private val priceHistoryDao: WishlistPriceHistoryDao
) {
    
    // === 心愿单物品基础操作 ===
    
    /**
     * 获取所有激活的心愿单物品
     */
    fun getAllActiveItems(): Flow<List<WishlistItemEntity>> {
        return wishlistDao.getAllActiveItems()
    }
    
    /**
     * 根据ID获取心愿单物品
     */
    suspend fun getItemById(id: Long): WishlistItemEntity? {
        return wishlistDao.getById(id)
    }
    
    /**
     * 添加新的心愿单物品
     */
    suspend fun addWishlistItem(itemDetails: WishlistItemDetails): Long {
        val now = Date()
        val item = WishlistItemEntity(
            name = itemDetails.name,
            category = itemDetails.category,
            subCategory = itemDetails.subCategory,
            brand = itemDetails.brand,
            specification = itemDetails.specification,
            price = itemDetails.estimatedPrice,
            targetPrice = itemDetails.targetPrice,
            priority = itemDetails.priority,
            urgency = itemDetails.urgency,
            quantity = itemDetails.desiredQuantity,
            quantityUnit = itemDetails.quantityUnit,
            budgetLimit = itemDetails.budgetLimit,
            purchaseChannel = itemDetails.preferredStore,
            customNote = itemDetails.notes,
            sourceUrl = itemDetails.sourceUrl,
            imageUrl = itemDetails.imageUrl,
            addedReason = itemDetails.addedReason,
            addDate = now,
            lastModified = now
        )
        
        return database.withTransaction {
            val itemId = wishlistDao.insert(item)
            
            // 如果有初始价格，记录价格历史
            itemDetails.estimatedPrice?.let { price ->
                val priceHistory = WishlistPriceHistoryEntity(
                    wishlistItemId = itemId,
                    price = price,
                    source = "manual_initial",
                    isManual = true,
                    notes = "初始预估价格"
                )
                priceHistoryDao.insert(priceHistory)
            }
            
            itemId
        }
    }
    
    /**
     * 更新心愿单物品
     */
    suspend fun updateWishlistItem(item: WishlistItemEntity) {
        val updatedItem = item.copy(lastModified = Date())
        wishlistDao.update(updatedItem)
    }
    
    /**
     * 删除心愿单物品
     */
    suspend fun deleteWishlistItem(id: Long) {
        database.withTransaction {
            // 删除关联的价格历史
            priceHistoryDao.deleteAllForItem(id)
            // 软删除心愿单物品
            wishlistDao.softDelete(id)
        }
    }
    
    /**
     * 彻底删除心愿单物品
     */
    suspend fun permanentlyDeleteWishlistItem(id: Long) {
        database.withTransaction {
            priceHistoryDao.deleteAllForItem(id)
            wishlistDao.deleteById(id)
        }
    }
    
    // === 心愿单物品状态管理 ===
    
    /**
     * 更新优先级
     */
    suspend fun updatePriority(id: Long, priority: WishlistPriority) {
        wishlistDao.updatePriority(id, priority)
    }
    
    /**
     * 暂停/恢复跟踪
     */
    suspend fun setPauseStatus(id: Long, isPaused: Boolean) {
        wishlistDao.setPauseStatus(id, isPaused)
    }
    
    /**
     * 标记为已实现（购买完成）
     */
    suspend fun markAsAchieved(id: Long, relatedItemId: Long? = null) {
        wishlistDao.markAsAchieved(id, System.currentTimeMillis(), relatedItemId)
    }
    
    /**
     * 增加查看次数
     */
    suspend fun incrementViewCount(id: Long) {
        wishlistDao.incrementViewCount(id)
    }
    
    // === 价格管理功能 ===
    
    /**
     * 更新物品价格
     */
    suspend fun updatePrice(itemId: Long, newPrice: Double, source: String, sourceUrl: String? = null) {
        database.withTransaction {
            val item = wishlistDao.getById(itemId)
            if (item != null) {
                val oldPrice = item.currentPrice
                
                // 更新物品的当前价格
                wishlistDao.updateCurrentPrice(itemId, newPrice, System.currentTimeMillis())
                
                // 记录价格历史
                val priceChange = if (oldPrice != null) newPrice - oldPrice else null
                val changePercentage = if (oldPrice != null && oldPrice != 0.0) {
                    ((newPrice - oldPrice) / oldPrice) * 100
                } else null
                
                val priceHistory = WishlistPriceHistoryEntity(
                    wishlistItemId = itemId,
                    price = newPrice,
                    source = source,
                    sourceUrl = sourceUrl,
                    previousPrice = oldPrice,
                    priceChange = priceChange,
                    changePercentage = changePercentage,
                    isManual = source == "manual"
                )
                
                priceHistoryDao.insert(priceHistory)
                
                // 更新最低价和最高价
                val lowestRecord = priceHistoryDao.getLowestPriceRecord(itemId)
                val highestRecord = priceHistoryDao.getHighestPriceRecord(itemId)
                
                val updatedItem = item.copy(
                    currentPrice = newPrice,
                    lowestPrice = lowestRecord?.price,
                    highestPrice = highestRecord?.price,
                    priceChangeCount = item.priceChangeCount + if (priceChange != null && priceChange != 0.0) 1 else 0,
                    lastPriceCheck = Date(),
                    lastModified = Date()
                )
                
                wishlistDao.update(updatedItem)
            }
        }
    }
    
    /**
     * 获取价格历史
     */
    fun getPriceHistory(itemId: Long): Flow<List<WishlistPriceHistoryEntity>> {
        return priceHistoryDao.getPriceHistory(itemId)
    }
    
    /**
     * 获取需要价格跟踪的物品
     */
    suspend fun getPriceTrackingItems(): List<WishlistItemEntity> {
        return wishlistDao.getPriceTrackingItems()
    }
    
    /**
     * 获取达到目标价格的物品
     */
    suspend fun getItemsReachedTargetPrice(): List<WishlistItemEntity> {
        return wishlistDao.getItemsReachedTargetPrice()
    }
    
    /**
     * 获取价格下降的物品
     */
    suspend fun getPriceDroppedItems(): List<WishlistItemEntity> {
        return wishlistDao.getPriceDroppedItems()
    }
    
    // === 筛选和搜索功能 ===
    
    /**
     * 搜索心愿单物品
     */
    suspend fun searchItems(searchText: String): List<WishlistItemEntity> {
        return wishlistDao.searchItems(searchText)
    }
    
    /**
     * 根据分类获取物品
     */
    fun getItemsByCategory(category: String): Flow<List<WishlistItemEntity>> {
        return wishlistDao.getItemsByCategory(category)
    }
    
    /**
     * 根据优先级获取物品
     */
    fun getItemsByPriority(priority: WishlistPriority): Flow<List<WishlistItemEntity>> {
        return wishlistDao.getItemsByPriority(priority)
    }
    
    /**
     * 获取高优先级物品
     */
    fun getHighPriorityItems(): Flow<List<WishlistItemEntity>> {
        return wishlistDao.getHighPriorityItems()
    }
    
    /**
     * 根据价格范围筛选
     */
    suspend fun getItemsByPriceRange(minPrice: Double, maxPrice: Double): List<WishlistItemEntity> {
        return wishlistDao.getItemsByPriceRange(minPrice, maxPrice)
    }
    
    // === 统计和分析功能 ===
    
    /**
     * 获取心愿单统计信息
     */
    suspend fun getWishlistStats(): WishlistStats {
        return database.withTransaction {
            val totalItems = wishlistDao.getActiveItemsCount()
            val achievedItems = wishlistDao.getAchievedItemsCount()
            val totalEstimatedValue = wishlistDao.getTotalEstimatedValue() ?: 0.0
            val totalCurrentValue = wishlistDao.getTotalCurrentValue() ?: 0.0
            
            val priceDroppedItems = wishlistDao.getPriceDroppedItems().size
            val reachedTargetPriceItems = wishlistDao.getItemsReachedTargetPrice().size
            
            val categoryStats = wishlistDao.getCategoryStats()
            val priorityStats = wishlistDao.getPriorityDistribution()
            
            // 构建分类分布Map
            val categoryDistribution = categoryStats.associate { it.category to it.count }
            val priorityDistribution = priorityStats.associate { it.priority to it.count }
            
            // 计算其他统计数据
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
            
            WishlistStats(
                totalItems = totalItems,
                activeItems = totalItems,
                achievedItems = achievedItems,
                totalEstimatedValue = totalEstimatedValue,
                totalCurrentValue = totalCurrentValue,
                averageItemPrice = if (totalItems > 0) totalCurrentValue / totalItems else 0.0,
                priceDroppedItems = priceDroppedItems,
                reachedTargetPriceItems = reachedTargetPriceItems,
                priorityDistribution = priorityDistribution,
                categoryDistribution = categoryDistribution,
                topCategories = categoryStats.take(5).map { 
                    CategoryStat(it.category, it.count, 0.0, 0.0) 
                },
                purchaseConversionRate = if (totalItems + achievedItems > 0) {
                    (achievedItems.toDouble() / (totalItems + achievedItems)) * 100
                } else 0.0
            )
        }
    }
    
    /**
     * 获取分类统计
     */
    suspend fun getCategoryStats(): List<CategoryCount> {
        return wishlistDao.getCategoryStats()
    }
    
    /**
     * 获取优先级分布
     */
    suspend fun getPriorityDistribution(): List<PriorityCount> {
        return wishlistDao.getPriorityDistribution()
    }
    
    // === 批量操作功能 ===
    
    /**
     * 批量更新优先级
     */
    suspend fun batchUpdatePriority(itemIds: List<Long>, priority: WishlistPriority) {
        wishlistDao.batchUpdatePriority(itemIds, priority)
    }
    
    /**
     * 批量删除物品
     */
    suspend fun batchDeleteItems(itemIds: List<Long>) {
        database.withTransaction {
            // 删除关联的价格历史
            itemIds.forEach { itemId ->
                priceHistoryDao.deleteAllForItem(itemId)
            }
            // 软删除心愿单物品
            wishlistDao.batchSoftDelete(itemIds)
        }
    }
    
    // === 数据维护功能 ===
    
    /**
     * 清理旧的已实现愿望
     */
    suspend fun cleanupOldAchievedItems(daysToKeep: Int = 30) {
        val cutoffDate = System.currentTimeMillis() - (daysToKeep * 24L * 60 * 60 * 1000)
        wishlistDao.cleanupOldAchievedItems(cutoffDate)
    }
    
    /**
     * 清理旧的价格记录
     */
    suspend fun cleanupOldPriceRecords(itemId: Long, keepCount: Int = 100) {
        priceHistoryDao.cleanupOldRecords(itemId, keepCount)
    }
    
    /**
     * 删除低可信度的价格记录
     */
    suspend fun deleteLowConfidencePriceRecords(minConfidence: Double = 0.5) {
        priceHistoryDao.deleteLowConfidenceRecords(minConfidence)
    }
    
    // === 智能推荐功能接口 ===
    
    /**
     * 根据库存消耗情况生成推荐
     * TODO: 需要与库存系统集成
     */
    suspend fun getRecommendationsBasedOnInventory(): List<WishlistItemDetails> {
        // 这里将来可以集成库存分析，推荐需要补充的物品
        return emptyList()
    }
    
    /**
     * 根据季节生成推荐
     */
    suspend fun getSeasonalRecommendations(): List<WishlistItemDetails> {
        // 这里将来可以根据当前季节推荐相关物品
        return emptyList()
    }
    
    /**
     * 获取价格提醒需要关注的物品
     */
    suspend fun getItemsNeedingPriceAttention(): List<WishlistItemEntity> {
        val priceDropped = getPriceDroppedItems()
        val reachedTarget = getItemsReachedTargetPrice()
        return (priceDropped + reachedTarget).distinctBy { it.id }
    }
    
    /**
     * 获取已购买的物品
     */
    fun getPurchasedItems(): Flow<List<WishlistItemEntity>> {
        return wishlistDao.getPurchasedItems()
    }
    
    /**
     * 获取未购买的物品
     */
    fun getUnpurchasedItems(): Flow<List<WishlistItemEntity>> {
        return wishlistDao.getUnpurchasedItems()
    }
    
    /**
     * 标记心愿单物品为已购买
     */
    suspend fun markAsPurchased(itemId: Long) {
        database.withTransaction {
            val existingItem = wishlistDao.getById(itemId)
            if (existingItem != null) {
                val updatedItem = existingItem.copy(
                    achievedDate = java.util.Date(),
                    lastModified = java.util.Date(),
                    isPaused = true  // 已购买的物品暂停跟踪
                )
                wishlistDao.update(updatedItem)
            }
        }
    }
}
