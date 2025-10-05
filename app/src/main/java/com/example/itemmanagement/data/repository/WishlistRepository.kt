package com.example.itemmanagement.data.repository

import androidx.room.withTransaction
import com.example.itemmanagement.data.AppDatabase
import com.example.itemmanagement.data.dao.unified.*
import com.example.itemmanagement.data.entity.unified.*
import com.example.itemmanagement.data.entity.wishlist.WishlistPriority
import com.example.itemmanagement.data.entity.wishlist.WishlistUrgency
import com.example.itemmanagement.data.entity.WishlistItemEntity
import com.example.itemmanagement.data.view.WishlistItemView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.*

/**
 * 统一架构的心愿单数据仓库
 * 基于UnifiedItemEntity和WishlistDetailEntity
 */
class WishlistRepository(
    private val appDatabase: AppDatabase,
    private val unifiedItemDao: UnifiedItemDao,
    private val itemStateDao: ItemStateDao,
    private val wishlistDetailDao: WishlistDetailDao
) {
    
    // ==================== 数据流 ====================
    
    /**
     * 获取所有激活的心愿单物品
     */
    fun getAllActiveWishlistItems(): Flow<List<WishlistItemView>> {
        return combine(
            itemStateDao.getActiveStatesByType(ItemStateType.WISHLIST),
            wishlistDetailDao.getAllDetails()
        ) { states, details ->
            states.mapNotNull { state ->
                val detail = details.find { it.itemId == state.itemId }
                val item = unifiedItemDao.getById(state.itemId)
                
                if (detail != null && item != null) {
                // 使用新的扁平化WishlistItemView构造
                createWishlistItemView(item, detail, state)
                } else null
            }
        }
    }
    
    /**
     * 获取按优先级排序的心愿单物品
     */
    fun getWishlistItemsByPriority(): Flow<List<WishlistItemView>> {
        return getAllActiveWishlistItems().map { items ->
            items.sortedWith(
                compareByDescending<WishlistItemView> { it.priority.ordinal }
                    .thenByDescending { it.urgency.ordinal }
                    .thenBy { it.addedToWishlistDate }
            )
        }
    }
    
    /**
     * 获取最近查看的心愿单物品
     */
    fun getRecentlyViewedItems(limit: Int = 10): Flow<List<WishlistItemView>> {
        return getAllActiveWishlistItems().map { items ->
            items.filter { it.lastViewDate != null }
                .sortedByDescending { it.lastViewDate }
                .take(limit)
        }
    }
    
    // ==================== 基本操作 ====================
    
    /**
     * 添加物品到心愿单
     */
    suspend fun addToWishlist(
        name: String,
        category: String,
        subCategory: String? = null,
        brand: String? = null,
        specification: String? = null,
        customNote: String? = null,
        price: Double? = null,
        targetPrice: Double? = null,
        priority: WishlistPriority = WishlistPriority.NORMAL,
        urgency: WishlistUrgency = WishlistUrgency.NORMAL,
        quantity: Double = 1.0,
        quantityUnit: String = "个",
        budgetLimit: Double? = null,
        purchaseChannel: String? = null,
        sourceUrl: String? = null,
        imageUrl: String? = null,
        addedReason: String? = null
    ): Long {
        return appDatabase.withTransaction {
            // 1. 创建统一物品
            val unifiedItem = UnifiedItemEntity(
                name = name,
                category = category,
                subCategory = subCategory,
                brand = brand,
                specification = specification,
                customNote = customNote,
                createdDate = Date(),
                updatedDate = Date()
            )
            
            val itemId = unifiedItemDao.insert(unifiedItem)
            
            // 2. 添加心愿单状态
            val wishlistState = ItemStateEntity(
                itemId = itemId,
                stateType = ItemStateType.WISHLIST,
                notes = addedReason ?: "用户添加",
                activatedDate = Date(),
                isActive = true
            )
            
            itemStateDao.insert(wishlistState)
            
            // 3. 创建心愿单详情
            val wishlistDetail = WishlistDetailEntity(
                itemId = itemId,
                    price = price,
                targetPrice = targetPrice,
                currentPrice = price,
                lowestPrice = price,
                highestPrice = price,
                priceUnit = "元",
                priority = priority,
                urgency = urgency,
                quantity = quantity,
                quantityUnit = quantityUnit,
                budgetLimit = budgetLimit,
                purchaseChannel = purchaseChannel,
                isPriceTrackingEnabled = price != null,
                priceDropThreshold = if (price != null && targetPrice != null) {
                    ((price - targetPrice) / price * 100).toDouble()
                } else null,
                lastPriceCheck = if (price != null) Date() else null,
                sourceUrl = sourceUrl,
                imageUrl = imageUrl,
                addedReason = addedReason,
                isPaused = false,
                achievedDate = null,
                createdDate = Date(),
                lastModified = Date(),
                viewCount = 0,
                lastViewDate = Date()
            )
            
            wishlistDetailDao.insert(wishlistDetail)
            
            itemId
        }
    }
    
    /**
     * 更新心愿单物品
     */
    suspend fun updateWishlistItem(
        itemId: Long,
        name: String? = null,
        category: String? = null,
        subCategory: String? = null,
        brand: String? = null,
        specification: String? = null,
        customNote: String? = null,
        price: Double? = null,
        targetPrice: Double? = null,
        priority: WishlistPriority? = null,
        urgency: WishlistUrgency? = null,
        quantity: Double? = null,
        quantityUnit: String? = null,
        budgetLimit: Double? = null,
        purchaseChannel: String? = null,
        sourceUrl: String? = null,
        imageUrl: String? = null
    ) {
        appDatabase.withTransaction {
            // 更新统一物品（如果有变化）
            val currentItem = unifiedItemDao.getById(itemId)
            if (currentItem != null) {
                val updatedItem = currentItem.copy(
                    name = name ?: currentItem.name,
                    category = category ?: currentItem.category,
                    subCategory = subCategory ?: currentItem.subCategory,
                    brand = brand ?: currentItem.brand,
                    specification = specification ?: currentItem.specification,
                    customNote = customNote ?: currentItem.customNote,
                    updatedDate = Date()
                )
                unifiedItemDao.update(updatedItem)
            }
            
            // 更新心愿单详情
            val currentDetail = wishlistDetailDao.getByItemId(itemId)
            if (currentDetail != null) {
                val updatedDetail = currentDetail.copy(
                    price = price ?: currentDetail.price,
                    targetPrice = targetPrice ?: currentDetail.targetPrice,
                    priority = priority ?: currentDetail.priority,
                    urgency = urgency ?: currentDetail.urgency,
                    quantity = quantity ?: currentDetail.quantity,
                    quantityUnit = quantityUnit ?: currentDetail.quantityUnit,
                    budgetLimit = budgetLimit ?: currentDetail.budgetLimit,
                    purchaseChannel = purchaseChannel ?: currentDetail.purchaseChannel,
                    sourceUrl = sourceUrl ?: currentDetail.sourceUrl,
                    imageUrl = imageUrl ?: currentDetail.imageUrl,
                    lastModified = Date()
                )
                wishlistDetailDao.update(updatedDetail)
            }
        }
    }
    
    /**
     * 从心愿单移除物品
     */
    suspend fun removeFromWishlist(itemId: Long) {
        appDatabase.withTransaction {
            // 停用心愿单状态
            itemStateDao.deactivateState(itemId, ItemStateType.WISHLIST)
            
            // 可选：删除心愿单详情（或者保留作为历史记录）
            wishlistDetailDao.deleteByItemId(itemId)
        }
    }
    
    /**
     * 标记心愿单物品为已实现
     */
    suspend fun markAsAchieved(itemId: Long, achievedDate: Date = Date()) {
        val detail = wishlistDetailDao.getByItemId(itemId)
        if (detail != null) {
            val updatedDetail = detail.copy(
                achievedDate = achievedDate,
                lastModified = Date()
            )
            wishlistDetailDao.update(updatedDetail)
        }
    }
    
    /**
     * 更新价格跟踪信息
     */
    suspend fun updatePriceTracking(
        itemId: Long,
        currentPrice: Double,
        source: String? = null
    ) {
        val detail = wishlistDetailDao.getByItemId(itemId)
        if (detail != null) {
            val updatedDetail = detail.copy(
                currentPrice = currentPrice,
                lowestPrice = minOf(detail.lowestPrice ?: currentPrice, currentPrice),
                highestPrice = maxOf(detail.highestPrice ?: currentPrice, currentPrice),
                lastPriceCheck = Date(),
                lastModified = Date()
            )
            wishlistDetailDao.update(updatedDetail)
        }
    }
    
    /**
     * 记录物品查看
     */
    suspend fun recordItemView(itemId: Long) {
        val detail = wishlistDetailDao.getByItemId(itemId)
        if (detail != null) {
            val updatedDetail = detail.copy(
                viewCount = detail.viewCount + 1,
                lastViewDate = Date(),
                lastModified = Date()
            )
            wishlistDetailDao.update(updatedDetail)
        }
    }
    
    // ==================== 查询操作 ====================
    
    /**
     * 根据ID获取心愿单物品
     */
    suspend fun getWishlistItemById(itemId: Long): WishlistItemView? {
        val item = unifiedItemDao.getById(itemId)
        val detail = wishlistDetailDao.getByItemId(itemId)
        val state = itemStateDao.getActiveStateByItemIdAndType(itemId, ItemStateType.WISHLIST)
        
        return if (item != null && detail != null && state != null) {
                // 使用新的扁平化WishlistItemView构造
                createWishlistItemView(item, detail, state)
        } else null
    }
    
    /**
     * 搜索心愿单物品
     */
    suspend fun searchWishlistItems(query: String): List<WishlistItemView> {
        // 简化实现，实际需要实现搜索逻辑
        return emptyList()
    }
    
    /**
     * 按分类获取心愿单物品
     */
    suspend fun getWishlistItemsByCategory(category: String): List<WishlistItemView> {
        // 简化实现，实际需要实现分类筛选逻辑
        return emptyList()
    }
    
    // ==================== 统计操作 ====================
    
    /**
     * 获取心愿单统计信息
     */
    suspend fun getWishlistStats(): WishlistStats {
        val totalCount = itemStateDao.getActiveItemCountByStateType(ItemStateType.WISHLIST)
        val achievedCount = wishlistDetailDao.getAchievedItemsCount()
        val priorityDistribution = wishlistDetailDao.getPriorityDistribution()
        
        return WishlistStats(
            totalItems = totalCount,
            achievedItems = achievedCount,
            pendingItems = totalCount - achievedCount,
            priorityDistribution = priorityDistribution.associate { 
                it.priority to it.count 
            }
        )
    }

    /**
     * 获取所有心愿单物品（包含详情）
     */
    fun getAllWishlistItems(): Flow<List<Pair<UnifiedItemEntity, WishlistDetailEntity>>> {
        return combine(
            unifiedItemDao.getAllItems(),
            wishlistDetailDao.getAllDetails()
        ) { unifiedItems, wishlistDetails ->
            unifiedItems.mapNotNull { unifiedItem ->
                wishlistDetails.find { it.itemId == unifiedItem.id }?.let { detail ->
                    Pair(unifiedItem, detail)
                }
            }
        }
    }

    // ================== 兼容性方法（用于UI层） ==================

    /**
     * 获取所有活跃的心愿单物品（兼容方法）
     */
    fun getAllActiveItems(): Flow<List<WishlistItemEntity>> {
        return getAllWishlistItems().map { items ->
            items.map { (unifiedItem, wishlistDetail) ->
                WishlistItemEntity(
                    id = unifiedItem.id,
                    name = unifiedItem.name,
                    category = unifiedItem.category,
                    subCategory = unifiedItem.subCategory,
                    brand = unifiedItem.brand,
                    specification = unifiedItem.specification,
                    customNote = unifiedItem.customNote,
                    price = wishlistDetail.price,
                    targetPrice = wishlistDetail.targetPrice,
                    currentPrice = wishlistDetail.currentPrice,
                    priority = wishlistDetail.priority,
                    urgency = wishlistDetail.urgency,
                    quantity = wishlistDetail.quantity,
                    quantityUnit = wishlistDetail.quantityUnit,
                    budgetLimit = wishlistDetail.budgetLimit,
                    purchaseChannel = wishlistDetail.purchaseChannel,
                    isPriceTrackingEnabled = wishlistDetail.isPriceTrackingEnabled,
                    sourceUrl = wishlistDetail.sourceUrl,
                    imageUrl = wishlistDetail.imageUrl,
                    addedReason = wishlistDetail.addedReason,
                    isPaused = wishlistDetail.isPaused,
                    addDate = wishlistDetail.createdDate,
                    lastModified = wishlistDetail.lastModified
                )
            }
        }
    }

    /**
     * 添加心愿单物品（兼容方法）
     */
    suspend fun addWishlistItem(item: WishlistItemEntity): Long {
        val unifiedItem = UnifiedItemEntity(
            name = item.name,
            category = item.category,
            subCategory = item.subCategory,
            brand = item.brand,
            specification = item.specification,
            customNote = item.customNote
        )
        
        val wishlistDetail = WishlistDetailEntity(
            itemId = 0, // 将在addToWishlist中设置
            price = item.price,
            targetPrice = item.targetPrice,
            currentPrice = item.currentPrice,
            priority = item.priority,
            urgency = item.urgency,
            quantity = item.quantity,
            quantityUnit = item.quantityUnit,
            budgetLimit = item.budgetLimit,
            purchaseChannel = item.purchaseChannel,
            isPriceTrackingEnabled = item.isPriceTrackingEnabled,
            sourceUrl = item.sourceUrl,
            imageUrl = item.imageUrl,
            addedReason = item.addedReason,
            isPaused = item.isPaused
        )
        
        return addToWishlist(
            name = unifiedItem.name,
            category = unifiedItem.category,
            subCategory = unifiedItem.subCategory,
            brand = unifiedItem.brand,
            specification = unifiedItem.specification,
            customNote = unifiedItem.customNote,
            price = wishlistDetail.price,
            targetPrice = wishlistDetail.targetPrice,
            priority = wishlistDetail.priority,
            urgency = wishlistDetail.urgency,
            quantity = wishlistDetail.quantity,
            quantityUnit = wishlistDetail.quantityUnit,
            budgetLimit = wishlistDetail.budgetLimit,
            purchaseChannel = wishlistDetail.purchaseChannel,
            sourceUrl = wishlistDetail.sourceUrl,
            imageUrl = wishlistDetail.imageUrl,
            addedReason = wishlistDetail.addedReason
        )
    }

    /**
     * 删除心愿单物品（兼容方法）
     */
    suspend fun deleteWishlistItem(itemId: Long) {
        removeFromWishlist(itemId)
    }

    /**
     * 更新优先级（兼容方法）
     */
    suspend fun updatePriority(itemId: Long, priority: WishlistPriority) {
        val current = wishlistDetailDao.getByItemId(itemId) ?: return
        wishlistDetailDao.update(current.copy(priority = priority, lastModified = Date()))
    }

    /**
     * 更新价格（兼容方法）
     */
    suspend fun updatePrice(itemId: Long, price: Double?) {
        val current = wishlistDetailDao.getByItemId(itemId) ?: return
        wishlistDetailDao.update(current.copy(currentPrice = price, lastModified = Date()))
    }

    /**
     * 增加查看次数（兼容方法）
     */
    suspend fun incrementViewCount(itemId: Long) {
        val current = wishlistDetailDao.getByItemId(itemId) ?: return
        wishlistDetailDao.update(
            current.copy(
                viewCount = current.viewCount + 1,
                lastViewDate = Date(),
                lastModified = Date()
            )
        )
    }

    /**
     * 批量更新优先级（兼容方法）
     */
    suspend fun batchUpdatePriority(itemIds: List<Long>, priority: WishlistPriority) {
        itemIds.forEach { itemId ->
            updatePriority(itemId, priority)
        }
    }

    /**
     * 批量删除物品（兼容方法）
     */
    suspend fun batchDeleteItems(itemIds: List<Long>) {
        itemIds.forEach { itemId ->
            deleteWishlistItem(itemId)
        }
    }

    /**
     * 获取需要价格关注的物品（兼容方法）
     */
    suspend fun getItemsNeedingPriceAttention(): List<WishlistItemEntity> {
        return getAllActiveItems().first().filter { item ->
            item.isPriceTrackingEnabled && 
            (item.currentPrice == null || item.currentPrice!! > (item.targetPrice ?: 0.0))
        }
    }

    /**
     * 基于库存获取推荐（兼容方法）
     */
    suspend fun getRecommendationsBasedOnInventory(): List<WishlistItemEntity> {
        // TODO: 实现基于库存的推荐逻辑
        return emptyList()
    }
}

/**
 * 心愿单统计信息
 */
data class WishlistStats(
    val totalItems: Int,
    val achievedItems: Int,
    val pendingItems: Int,
    val priorityDistribution: Map<String, Int>
)

/**
 * 创建WishlistItemView的私有辅助方法
 * 将分散的实体数据组装成扁平化的视图对象
 */
private fun createWishlistItemView(
    unifiedItem: UnifiedItemEntity,
    wishlistDetail: WishlistDetailEntity,
    itemState: ItemStateEntity
): WishlistItemView {
    return WishlistItemView(
        // 基础物品信息
        id = unifiedItem.id,
        name = unifiedItem.name,
        category = unifiedItem.category,
        subCategory = unifiedItem.subCategory,
        brand = unifiedItem.brand,
        specification = unifiedItem.specification,
        customNote = unifiedItem.customNote,
        
        // 心愿单专用信息
        price = wishlistDetail.price,
        targetPrice = wishlistDetail.targetPrice,
        priceUnit = wishlistDetail.priceUnit,
        currentPrice = wishlistDetail.currentPrice,
        lowestPrice = wishlistDetail.lowestPrice,
        highestPrice = wishlistDetail.highestPrice,
        
        // 购买计划
        priority = wishlistDetail.priority,
        urgency = wishlistDetail.urgency,
        quantity = wishlistDetail.quantity,
        quantityUnit = wishlistDetail.quantityUnit,
        budgetLimit = wishlistDetail.budgetLimit,
        purchaseChannel = wishlistDetail.purchaseChannel,
        
        // 价格跟踪设置
        isPriceTrackingEnabled = wishlistDetail.isPriceTrackingEnabled,
        priceDropThreshold = wishlistDetail.priceDropThreshold,
        lastPriceCheck = wishlistDetail.lastPriceCheck,
        
        // 状态信息
        isActive = itemState.isActive,
        isPaused = wishlistDetail.isPaused,
        addedToWishlistDate = itemState.activatedDate,
        lastModified = wishlistDetail.lastModified,
        achievedDate = wishlistDetail.achievedDate,
        
        // 扩展信息
        sourceUrl = wishlistDetail.sourceUrl,
        imageUrl = wishlistDetail.imageUrl,
        relatedInventoryItemId = wishlistDetail.relatedInventoryItemId,
        addedReason = wishlistDetail.addedReason,
        
        // 统计信息
        viewCount = wishlistDetail.viewCount,
        lastViewDate = wishlistDetail.lastViewDate,
        priceChangeCount = wishlistDetail.priceChangeCount
    )
}