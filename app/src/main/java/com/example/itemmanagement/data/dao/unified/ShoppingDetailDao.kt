package com.example.itemmanagement.data.dao.unified

import androidx.room.*
import com.example.itemmanagement.data.entity.unified.ShoppingDetailEntity
import com.example.itemmanagement.data.entity.ShoppingItemPriority
import com.example.itemmanagement.data.entity.UrgencyLevel
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 购物优先级统计数据类
 */
data class ShoppingPriorityCountInfo(
    val priority: String,
    val count: Int
)

/**
 * 购物详情数据访问接口
 * 管理物品在购物清单状态下的专用数据
 */
@Dao
interface ShoppingDetailDao {
    
    // === 基础CRUD操作 ===
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(detail: ShoppingDetailEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(details: List<ShoppingDetailEntity>): List<Long>
    
    @Update
    suspend fun update(detail: ShoppingDetailEntity)
    
    @Delete
    suspend fun delete(detail: ShoppingDetailEntity)
    
    @Query("DELETE FROM shopping_details WHERE itemId = :itemId")
    suspend fun deleteByItemId(itemId: Long): Int
    
    // === 查询操作 ===
    
    @Query("SELECT * FROM shopping_details WHERE itemId = :itemId")
    suspend fun getByItemId(itemId: Long): ShoppingDetailEntity?
    
    @Query("SELECT * FROM shopping_details WHERE shoppingListId = :listId ORDER BY priority DESC, deadline ASC")
    fun getByShoppingListId(listId: Long): Flow<List<ShoppingDetailEntity>>
    
    @Query("SELECT * FROM shopping_details WHERE shoppingListId = :listId AND isPurchased = 0 ORDER BY priority DESC, deadline ASC")
    fun getPendingItemsByListId(listId: Long): Flow<List<ShoppingDetailEntity>>
    
    @Query("SELECT * FROM shopping_details WHERE shoppingListId = :listId AND isPurchased = 1 ORDER BY purchaseDate DESC")
    fun getPurchasedItemsByListId(listId: Long): Flow<List<ShoppingDetailEntity>>
    
    @Query("SELECT * FROM shopping_details WHERE isPurchased = 0 ORDER BY priority DESC, deadline ASC")
    fun getAllPendingItems(): Flow<List<ShoppingDetailEntity>>
    
    @Query("SELECT * FROM shopping_details WHERE isPurchased = 1 ORDER BY purchaseDate DESC")
    fun getAllPurchasedItems(): Flow<List<ShoppingDetailEntity>>
    
    // === 优先级和紧急程度查询 ===
    
    @Query("SELECT * FROM shopping_details WHERE priority = :priority ORDER BY deadline ASC")
    fun getByPriority(priority: ShoppingItemPriority): Flow<List<ShoppingDetailEntity>>
    
    @Query("SELECT * FROM shopping_details WHERE urgencyLevel = :urgency ORDER BY deadline ASC")
    fun getByUrgency(urgency: UrgencyLevel): Flow<List<ShoppingDetailEntity>>
    
    @Query("""
        SELECT * FROM shopping_details 
        WHERE priority = 'URGENT' OR urgencyLevel = 'CRITICAL' OR urgencyLevel = 'URGENT'
        ORDER BY priority DESC, urgencyLevel DESC, deadline ASC
    """)
    fun getHighPriorityItems(): Flow<List<ShoppingDetailEntity>>
    
    // === 截止日期相关查询 ===
    
    @Query("""
        SELECT * FROM shopping_details 
        WHERE deadline IS NOT NULL 
        AND deadline <= :deadline 
        AND isPurchased = 0
        ORDER BY deadline ASC
    """)
    fun getItemsDueBefore(deadline: Date): Flow<List<ShoppingDetailEntity>>
    
    @Query("""
        SELECT * FROM shopping_details 
        WHERE deadline IS NOT NULL 
        AND deadline <= :overdue 
        AND isPurchased = 0
        ORDER BY deadline ASC
    """)
    fun getOverdueItems(overdue: Date = Date()): Flow<List<ShoppingDetailEntity>>
    
    // === 预算相关查询 ===
    
    @Query("SELECT * FROM shopping_details WHERE budgetLimit IS NOT NULL AND estimatedPrice > budgetLimit")
    fun getItemsOverBudget(): Flow<List<ShoppingDetailEntity>>
    
    @Query("SELECT SUM(estimatedPrice * quantity) FROM shopping_details WHERE shoppingListId = :listId AND isPurchased = 0")
    suspend fun getEstimatedTotalCost(listId: Long): Double?
    
    @Query("SELECT SUM(actualPrice * quantity) FROM shopping_details WHERE shoppingListId = :listId AND isPurchased = 1")
    suspend fun getActualTotalSpent(listId: Long): Double?
    
    // === 更新操作 ===
    
    @Query("UPDATE shopping_details SET isPurchased = :purchased, purchaseDate = :purchaseDate, completedDate = :completedDate WHERE itemId = :itemId")
    suspend fun updatePurchaseStatus(
        itemId: Long, 
        purchased: Boolean, 
        purchaseDate: Date? = if (purchased) Date() else null,
        completedDate: Date? = if (purchased) Date() else null
    )
    
    @Query("UPDATE shopping_details SET actualPrice = :actualPrice WHERE itemId = :itemId")
    suspend fun updateActualPrice(itemId: Long, actualPrice: Double?)
    
    @Query("UPDATE shopping_details SET priority = :priority WHERE itemId = :itemId")
    suspend fun updatePriority(itemId: Long, priority: ShoppingItemPriority)
    
    @Query("UPDATE shopping_details SET urgencyLevel = :urgency WHERE itemId = :itemId")
    suspend fun updateUrgency(itemId: Long, urgency: UrgencyLevel)
    
    @Query("UPDATE shopping_details SET deadline = :deadline WHERE itemId = :itemId")
    suspend fun updateDeadline(itemId: Long, deadline: Date?)
    
    @Query("UPDATE shopping_details SET quantity = :quantity WHERE itemId = :itemId")
    suspend fun updateQuantity(itemId: Long, quantity: Double)
    
    @Query("UPDATE shopping_details SET remindDate = :remindDate WHERE itemId = :itemId")
    suspend fun updateRemindDate(itemId: Long, remindDate: Date?)
    
    // === 统计查询 ===
    
    @Query("SELECT COUNT(*) FROM shopping_details WHERE shoppingListId = :listId")
    suspend fun getItemCountByListId(listId: Long): Int
    
    @Query("SELECT COUNT(*) FROM shopping_details WHERE shoppingListId = :listId AND isPurchased = 1")
    suspend fun getPurchasedCountByListId(listId: Long): Int
    
    @Query("SELECT COUNT(*) FROM shopping_details WHERE shoppingListId = :listId AND isPurchased = 0")
    suspend fun getPendingCountByListId(listId: Long): Int
    
    // === 活跃物品统计查询（排除已删除/已转移的物品）===
    
    /**
     * 获取活跃物品总数（只统计 isActive=true 的物品）
     * 通过 JOIN item_states 表过滤已删除和已转移的物品
     */
    @Query("""
        SELECT COUNT(DISTINCT sd.itemId) 
        FROM shopping_details sd
        INNER JOIN item_states ist ON sd.itemId = ist.itemId
        WHERE sd.shoppingListId = :listId 
        AND ist.stateType = 'SHOPPING'
        AND ist.contextId = :listId
        AND ist.isActive = 1
    """)
    suspend fun getActiveItemCountByListId(listId: Long): Int
    
    /**
     * 获取活跃的已购买物品数（只统计 isActive=true 的物品）
     */
    @Query("""
        SELECT COUNT(DISTINCT sd.itemId) 
        FROM shopping_details sd
        INNER JOIN item_states ist ON sd.itemId = ist.itemId
        WHERE sd.shoppingListId = :listId 
        AND sd.isPurchased = 1
        AND ist.stateType = 'SHOPPING'
        AND ist.contextId = :listId
        AND ist.isActive = 1
    """)
    suspend fun getActivePurchasedCountByListId(listId: Long): Int
    
    /**
     * 获取活跃的待购买物品数（只统计 isActive=true 的物品）
     */
    @Query("""
        SELECT COUNT(DISTINCT sd.itemId) 
        FROM shopping_details sd
        INNER JOIN item_states ist ON sd.itemId = ist.itemId
        WHERE sd.shoppingListId = :listId 
        AND sd.isPurchased = 0
        AND ist.stateType = 'SHOPPING'
        AND ist.contextId = :listId
        AND ist.isActive = 1
    """)
    suspend fun getActivePendingCountByListId(listId: Long): Int
    
    @Query("""
        SELECT priority, COUNT(*) as count 
        FROM shopping_details 
        WHERE shoppingListId = :listId 
        GROUP BY priority
    """)
    suspend fun getPriorityDistribution(listId: Long): List<ShoppingPriorityCountInfo>
    
    @Query("SELECT AVG(actualPrice) FROM shopping_details WHERE isPurchased = 1 AND actualPrice IS NOT NULL")
    suspend fun getAverageActualPrice(): Double?
    
    // === 高级查询 ===
    
    /**
     * 获取预算超支的物品
     */
    @Query("""
        SELECT * FROM shopping_details 
        WHERE actualPrice IS NOT NULL 
        AND budgetLimit IS NOT NULL 
        AND actualPrice > budgetLimit
        ORDER BY (actualPrice - budgetLimit) DESC
    """)
    fun getBudgetOverrunItems(): Flow<List<ShoppingDetailEntity>>
    
    /**
     * 获取周期性购买物品
     */
    @Query("SELECT * FROM shopping_details WHERE isRecurring = 1 ORDER BY recurringInterval ASC")
    fun getRecurringItems(): Flow<List<ShoppingDetailEntity>>
    
    /**
     * 获取需要提醒的物品
     */
    @Query("""
        SELECT * FROM shopping_details 
        WHERE remindDate IS NOT NULL 
        AND remindDate <= :now 
        AND isPurchased = 0
        ORDER BY remindDate ASC
    """)
    suspend fun getItemsNeedingReminder(now: Date = Date()): List<ShoppingDetailEntity>
    
    /**
     * 搜索购物详情
     */
    @Query("""
        SELECT * FROM shopping_details 
        WHERE recommendationReason LIKE '%' || :keyword || '%'
        OR addedReason LIKE '%' || :keyword || '%'
        OR storeName LIKE '%' || :keyword || '%'
        ORDER BY addDate DESC
    """)
    fun searchDetails(keyword: String): Flow<List<ShoppingDetailEntity>>
    
    // === 批量操作 ===
    
    @Query("UPDATE shopping_details SET isPurchased = 1, purchaseDate = :purchaseDate, completedDate = :completedDate WHERE itemId IN (:itemIds)")
    suspend fun batchMarkAsPurchased(itemIds: List<Long>, purchaseDate: Date = Date(), completedDate: Date = Date())
    
    @Query("UPDATE shopping_details SET priority = :priority WHERE itemId IN (:itemIds)")
    suspend fun batchUpdatePriority(itemIds: List<Long>, priority: ShoppingItemPriority)
    
    @Query("DELETE FROM shopping_details WHERE itemId IN (:itemIds)")
    suspend fun deleteByItemIds(itemIds: List<Long>): Int
    
    /**
     * 移动物品到其他购物清单
     */
    @Query("UPDATE shopping_details SET shoppingListId = :newListId WHERE itemId IN (:itemIds)")
    suspend fun moveToShoppingList(itemIds: List<Long>, newListId: Long): Int
    
    // === 清理操作 ===
    
    @Query("DELETE FROM shopping_details WHERE isPurchased = 1 AND completedDate < :beforeDate")
    suspend fun cleanupOldPurchasedItems(beforeDate: Date): Int
}
