package com.example.itemmanagement.data.dao.unified

import androidx.room.*
import com.example.itemmanagement.data.entity.unified.WishlistDetailEntity
import com.example.itemmanagement.data.entity.wishlist.WishlistPriority
import com.example.itemmanagement.data.entity.wishlist.WishlistUrgency
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 优先级统计数据类
 */
data class PriorityCountInfo(
    val priority: String,
    val count: Int
)

/**
 * 心愿单详情数据访问接口
 * 管理物品在心愿单状态下的专用数据
 */
@Dao
interface WishlistDetailDao {
    
    // === 基础CRUD操作 ===
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(detail: WishlistDetailEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(details: List<WishlistDetailEntity>): List<Long>
    
    @Update
    suspend fun update(detail: WishlistDetailEntity)
    
    @Delete
    suspend fun delete(detail: WishlistDetailEntity)
    
    @Query("DELETE FROM wishlist_details WHERE itemId = :itemId")
    suspend fun deleteByItemId(itemId: Long): Int
    
    // === 查询操作 ===
    
    @Query("SELECT * FROM wishlist_details WHERE itemId = :itemId")
    suspend fun getByItemId(itemId: Long): WishlistDetailEntity?
    
    @Query("SELECT * FROM wishlist_details ORDER BY priority DESC, createdDate DESC")
    fun getAllDetails(): Flow<List<WishlistDetailEntity>>
    
    @Query("SELECT * FROM wishlist_details WHERE priority = :priority ORDER BY createdDate DESC")
    fun getByPriority(priority: WishlistPriority): Flow<List<WishlistDetailEntity>>
    
    @Query("SELECT * FROM wishlist_details WHERE urgency = :urgency ORDER BY createdDate DESC")
    fun getByUrgency(urgency: WishlistUrgency): Flow<List<WishlistDetailEntity>>
    
    @Query("""
        SELECT * FROM wishlist_details 
        WHERE priority = 'URGENT' OR urgency = 'CRITICAL' OR urgency = 'URGENT'
        ORDER BY priority DESC, urgency DESC, createdDate DESC
    """)
    fun getHighPriorityItems(): Flow<List<WishlistDetailEntity>>
    
    // === 价格相关查询 ===
    
    @Query("SELECT * FROM wishlist_details WHERE targetPrice <= :maxPrice ORDER BY targetPrice ASC")
    fun getItemsUnderPrice(maxPrice: Double): Flow<List<WishlistDetailEntity>>
    
    @Query("SELECT * FROM wishlist_details WHERE currentPrice IS NOT NULL AND targetPrice IS NOT NULL AND currentPrice <= targetPrice ORDER BY currentPrice ASC")
    fun getItemsAtTargetPrice(): Flow<List<WishlistDetailEntity>>
    
    @Query("SELECT * FROM wishlist_details WHERE isPriceTrackingEnabled = 1 ORDER BY lastPriceCheck ASC")
    suspend fun getItemsNeedingPriceCheck(): List<WishlistDetailEntity>
    
    @Query("""
        SELECT * FROM wishlist_details 
        WHERE isPriceTrackingEnabled = 1 
        AND (lastPriceCheck IS NULL OR lastPriceCheck < :checkDate)
        ORDER BY CASE WHEN lastPriceCheck IS NULL THEN 0 ELSE 1 END, lastPriceCheck ASC
    """)
    suspend fun getItemsNeedingPriceCheckBefore(checkDate: Date): List<WishlistDetailEntity>
    
    // === 更新操作 ===
    
    @Query("UPDATE wishlist_details SET currentPrice = :price, lastPriceCheck = :checkDate, priceChangeCount = priceChangeCount + 1 WHERE itemId = :itemId")
    suspend fun updateCurrentPrice(itemId: Long, price: Double, checkDate: Date = Date())
    
    // 注意：achievedDate应该通过ItemStateEntity来管理，不在WishlistDetailEntity中
    
    @Query("UPDATE wishlist_details SET viewCount = viewCount + 1, lastViewDate = :viewDate WHERE itemId = :itemId")
    suspend fun incrementViewCount(itemId: Long, viewDate: Date = Date())
    
    @Query("UPDATE wishlist_details SET isPaused = :paused WHERE itemId = :itemId")
    suspend fun updatePauseStatus(itemId: Long, paused: Boolean)
    
    @Query("UPDATE wishlist_details SET priority = :priority WHERE itemId = :itemId")
    suspend fun updatePriority(itemId: Long, priority: WishlistPriority)
    
    @Query("UPDATE wishlist_details SET urgency = :urgency WHERE itemId = :itemId")
    suspend fun updateUrgency(itemId: Long, urgency: WishlistUrgency)
    
    @Query("UPDATE wishlist_details SET targetPrice = :targetPrice WHERE itemId = :itemId")
    suspend fun updateTargetPrice(itemId: Long, targetPrice: Double?)
    
    @Query("UPDATE wishlist_details SET budgetLimit = :budgetLimit WHERE itemId = :itemId")
    suspend fun updateBudgetLimit(itemId: Long, budgetLimit: Double?)
    
    // === 价格跟踪管理 ===
    
    @Query("UPDATE wishlist_details SET isPriceTrackingEnabled = :enabled WHERE itemId = :itemId")
    suspend fun updatePriceTrackingStatus(itemId: Long, enabled: Boolean)
    
    @Query("UPDATE wishlist_details SET priceDropThreshold = :threshold WHERE itemId = :itemId")
    suspend fun updatePriceDropThreshold(itemId: Long, threshold: Double?)
    
    @Query("UPDATE wishlist_details SET priceCheckInterval = :interval WHERE itemId = :itemId")
    suspend fun updatePriceCheckInterval(itemId: Long, interval: Int)
    
    @Query("""
        UPDATE wishlist_details 
        SET lowestPrice = CASE 
            WHEN lowestPrice IS NULL OR :price < lowestPrice THEN :price
            ELSE lowestPrice 
        END,
        highestPrice = CASE 
            WHEN highestPrice IS NULL OR :price > highestPrice THEN :price
            ELSE highestPrice 
        END
        WHERE itemId = :itemId
    """)
    suspend fun updatePriceRange(itemId: Long, price: Double)
    
    // === 统计查询 ===
    
    @Query("SELECT COUNT(*) FROM wishlist_details")
    suspend fun getDetailCount(): Int
    
    @Query("SELECT COUNT(*) FROM wishlist_details WHERE priority = :priority")
    suspend fun getCountByPriority(priority: WishlistPriority): Int
    
    @Query("SELECT COUNT(*) FROM wishlist_details WHERE isPriceTrackingEnabled = 1")
    suspend fun getPriceTrackingEnabledCount(): Int
    
    @Query("SELECT COUNT(*) FROM wishlist_details")
    suspend fun getAchievedItemsCount(): Int
    
    @Query("SELECT AVG(targetPrice) FROM wishlist_details WHERE targetPrice IS NOT NULL")
    suspend fun getAverageTargetPrice(): Double?
    
    @Query("""
        SELECT priority, COUNT(*) as count 
        FROM wishlist_details 
        GROUP BY priority
    """)
    suspend fun getPriorityDistribution(): List<PriorityCountInfo>
    
    // === 高级查询 ===
    
    /**
     * 获取最近添加的心愿单详情 (按照最后查看时间排序)
     */
    @Query("SELECT * FROM wishlist_details ORDER BY lastViewDate DESC LIMIT :limit")
    suspend fun getRecentDetails(limit: Int = 10): List<WishlistDetailEntity>
    
    /**
     * 获取最近查看的心愿单详情
     */
    @Query("SELECT * FROM wishlist_details WHERE lastViewDate IS NOT NULL ORDER BY lastViewDate DESC LIMIT :limit")
    suspend fun getRecentlyViewedDetails(limit: Int = 10): List<WishlistDetailEntity>
    
    /**
     * 获取价格变动频繁的物品
     */
    @Query("SELECT * FROM wishlist_details WHERE priceChangeCount > :minChanges ORDER BY priceChangeCount DESC")
    suspend fun getFrequentPriceChangeItems(minChanges: Int = 3): List<WishlistDetailEntity>
    
    /**
     * 搜索心愿单详情
     */
    @Query("""
        SELECT * FROM wishlist_details 
        WHERE addedReason LIKE '%' || :keyword || '%'
        OR purchaseChannel LIKE '%' || :keyword || '%'
        OR preferredBrand LIKE '%' || :keyword || '%'
        ORDER BY createdDate DESC
    """)
    fun searchDetails(keyword: String): Flow<List<WishlistDetailEntity>>
    
    // === 批量操作 ===
    
    @Query("UPDATE wishlist_details SET lastModified = :modifiedDate WHERE itemId IN (:itemIds)")
    suspend fun updateLastModified(itemIds: List<Long>, modifiedDate: Date = Date())
    
    @Query("DELETE FROM wishlist_details WHERE itemId IN (:itemIds)")
    suspend fun deleteByItemIds(itemIds: List<Long>): Int
    
    /**
     * 批量更新价格跟踪状态
     */
    @Query("UPDATE wishlist_details SET isPriceTrackingEnabled = :enabled WHERE itemId IN (:itemIds)")
    suspend fun batchUpdatePriceTracking(itemIds: List<Long>, enabled: Boolean)
    
    /**
     * 批量更新优先级
     */
    @Query("UPDATE wishlist_details SET priority = :priority WHERE itemId IN (:itemIds)")
    suspend fun batchUpdatePriority(itemIds: List<Long>, priority: WishlistPriority)
}
