package com.example.itemmanagement.data.dao.wishlist

import androidx.room.*
import com.example.itemmanagement.data.entity.wishlist.WishlistPriceHistoryEntity
import com.example.itemmanagement.data.entity.wishlist.PriceVerificationStatus
import kotlinx.coroutines.flow.Flow

/**
 * 心愿单价格历史数据访问对象
 * 专门处理价格历史记录的数据库操作
 */
@Dao
interface WishlistPriceHistoryDao {
    
    // === 基础查询操作 ===
    
    /**
     * 获取指定心愿单物品的所有价格历史
     */
    @Query("SELECT * FROM wishlist_price_history WHERE wishlistItemId = :wishlistItemId ORDER BY recordDate DESC")
    fun getPriceHistory(wishlistItemId: Long): Flow<List<WishlistPriceHistoryEntity>>
    
    /**
     * 获取指定心愿单物品的价格历史（挂起函数版本）
     */
    @Query("SELECT * FROM wishlist_price_history WHERE wishlistItemId = :wishlistItemId ORDER BY recordDate DESC")
    suspend fun getPriceHistorySync(wishlistItemId: Long): List<WishlistPriceHistoryEntity>
    
    /**
     * 获取最新的价格记录
     */
    @Query("SELECT * FROM wishlist_price_history WHERE wishlistItemId = :wishlistItemId ORDER BY recordDate DESC LIMIT 1")
    suspend fun getLatestPriceRecord(wishlistItemId: Long): WishlistPriceHistoryEntity?
    
    /**
     * 根据ID获取价格历史记录
     */
    @Query("SELECT * FROM wishlist_price_history WHERE id = :id")
    suspend fun getById(id: Long): WishlistPriceHistoryEntity?
    
    // === 价格分析查询 ===
    
    /**
     * 获取指定时间范围内的价格历史
     */
    @Query("SELECT * FROM wishlist_price_history WHERE wishlistItemId = :wishlistItemId AND recordDate BETWEEN :startDate AND :endDate ORDER BY recordDate ASC")
    suspend fun getPriceHistoryInRange(wishlistItemId: Long, startDate: Long, endDate: Long): List<WishlistPriceHistoryEntity>
    
    /**
     * 获取最低价格记录
     */
    @Query("SELECT * FROM wishlist_price_history WHERE wishlistItemId = :wishlistItemId ORDER BY price ASC LIMIT 1")
    suspend fun getLowestPriceRecord(wishlistItemId: Long): WishlistPriceHistoryEntity?
    
    /**
     * 获取最高价格记录
     */
    @Query("SELECT * FROM wishlist_price_history WHERE wishlistItemId = :wishlistItemId ORDER BY price DESC LIMIT 1")
    suspend fun getHighestPriceRecord(wishlistItemId: Long): WishlistPriceHistoryEntity?
    
    /**
     * 获取平均价格
     */
    @Query("SELECT AVG(price) FROM wishlist_price_history WHERE wishlistItemId = :wishlistItemId")
    suspend fun getAveragePrice(wishlistItemId: Long): Double?
    
    /**
     * 获取价格趋势数据（最近30天）
     */
    @Query("SELECT * FROM wishlist_price_history WHERE wishlistItemId = :wishlistItemId AND recordDate >= :thirtyDaysAgo ORDER BY recordDate ASC")
    suspend fun getRecentPriceTrend(wishlistItemId: Long, thirtyDaysAgo: Long): List<WishlistPriceHistoryEntity>
    
    // === 价格变动查询 ===
    
    /**
     * 获取所有降价记录
     */
    @Query("SELECT * FROM wishlist_price_history WHERE priceChange < 0 ORDER BY recordDate DESC")
    suspend fun getAllPriceDrops(): List<WishlistPriceHistoryEntity>
    
    /**
     * 获取指定物品的降价记录
     */
    @Query("SELECT * FROM wishlist_price_history WHERE wishlistItemId = :wishlistItemId AND priceChange < 0 ORDER BY recordDate DESC")
    suspend fun getPriceDropsForItem(wishlistItemId: Long): List<WishlistPriceHistoryEntity>
    
    /**
     * 获取最近的价格变动记录
     */
    @Query("SELECT * FROM wishlist_price_history WHERE priceChange IS NOT NULL AND priceChange != 0 ORDER BY recordDate DESC LIMIT :limit")
    suspend fun getRecentPriceChanges(limit: Int = 10): List<WishlistPriceHistoryEntity>
    
    /**
     * 获取促销价格记录
     */
    @Query("SELECT * FROM wishlist_price_history WHERE isPromotional = 1 ORDER BY recordDate DESC")
    suspend fun getPromotionalPrices(): List<WishlistPriceHistoryEntity>
    
    // === 数据来源查询 ===
    
    /**
     * 根据来源获取价格记录
     */
    @Query("SELECT * FROM wishlist_price_history WHERE source = :source ORDER BY recordDate DESC")
    suspend fun getPricesBySource(source: String): List<WishlistPriceHistoryEntity>
    
    /**
     * 获取手动录入的价格记录
     */
    @Query("SELECT * FROM wishlist_price_history WHERE isManual = 1 ORDER BY recordDate DESC")
    suspend fun getManualPriceRecords(): List<WishlistPriceHistoryEntity>
    
    /**
     * 获取自动获取的价格记录
     */
    @Query("SELECT * FROM wishlist_price_history WHERE isManual = 0 ORDER BY recordDate DESC")
    suspend fun getAutomaticPriceRecords(): List<WishlistPriceHistoryEntity>
    
    // === 统计查询 ===
    
    /**
     * 获取价格记录总数
     */
    @Query("SELECT COUNT(*) FROM wishlist_price_history WHERE wishlistItemId = :wishlistItemId")
    suspend fun getPriceRecordCount(wishlistItemId: Long): Int
    
    /**
     * 获取价格变动次数
     */
    @Query("SELECT COUNT(*) FROM wishlist_price_history WHERE wishlistItemId = :wishlistItemId AND priceChange IS NOT NULL AND priceChange != 0")
    suspend fun getPriceChangeCount(wishlistItemId: Long): Int
    
    /**
     * 获取最大降价幅度
     */
    @Query("SELECT MIN(priceChange) FROM wishlist_price_history WHERE wishlistItemId = :wishlistItemId AND priceChange < 0")
    suspend fun getMaxPriceDrop(wishlistItemId: Long): Double?
    
    /**
     * 获取最大涨价幅度
     */
    @Query("SELECT MAX(priceChange) FROM wishlist_price_history WHERE wishlistItemId = :wishlistItemId AND priceChange > 0")
    suspend fun getMaxPriceIncrease(wishlistItemId: Long): Double?
    
    // === 数据修改操作 ===
    
    /**
     * 插入新的价格记录
     */
    @Insert
    suspend fun insert(priceHistory: WishlistPriceHistoryEntity): Long
    
    /**
     * 批量插入价格记录
     */
    @Insert
    suspend fun insertAll(priceHistories: List<WishlistPriceHistoryEntity>)
    
    /**
     * 更新价格记录
     */
    @Update
    suspend fun update(priceHistory: WishlistPriceHistoryEntity)
    
    /**
     * 删除价格记录
     */
    @Delete
    suspend fun delete(priceHistory: WishlistPriceHistoryEntity)
    
    /**
     * 根据ID删除价格记录
     */
    @Query("DELETE FROM wishlist_price_history WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    /**
     * 删除指定心愿单物品的所有价格历史
     */
    @Query("DELETE FROM wishlist_price_history WHERE wishlistItemId = :wishlistItemId")
    suspend fun deleteAllForItem(wishlistItemId: Long)
    
    // === 数据维护操作 ===
    
    /**
     * 更新价格记录的验证状态
     */
    @Query("UPDATE wishlist_price_history SET verificationStatus = :status WHERE id = :id")
    suspend fun updateVerificationStatus(id: Long, status: PriceVerificationStatus)
    
    /**
     * 批量更新验证状态
     */
    @Query("UPDATE wishlist_price_history SET verificationStatus = :status WHERE id IN (:ids)")
    suspend fun batchUpdateVerificationStatus(ids: List<Long>, status: PriceVerificationStatus)
    
    /**
     * 清理旧的价格记录（保留最近N条）
     */
    @Query("""
        DELETE FROM wishlist_price_history 
        WHERE wishlistItemId = :wishlistItemId 
        AND id NOT IN (
            SELECT id FROM wishlist_price_history 
            WHERE wishlistItemId = :wishlistItemId 
            ORDER BY recordDate DESC 
            LIMIT :keepCount
        )
    """)
    suspend fun cleanupOldRecords(wishlistItemId: Long, keepCount: Int = 100)
    
    /**
     * 删除指定日期之前的记录
     */
    @Query("DELETE FROM wishlist_price_history WHERE recordDate < :cutoffDate")
    suspend fun deleteRecordsBeforeDate(cutoffDate: Long)
    
    /**
     * 删除低可信度的记录
     */
    @Query("DELETE FROM wishlist_price_history WHERE confidence < :minConfidence")
    suspend fun deleteLowConfidenceRecords(minConfidence: Double = 0.5)
    
    // === 高级查询操作 ===
    
    /**
     * 获取价格波动较大的物品记录
     */
    @Query("""
        SELECT * FROM wishlist_price_history 
        WHERE wishlistItemId = :wishlistItemId 
        AND ABS(changePercentage) > :threshold 
        ORDER BY ABS(changePercentage) DESC
    """)
    suspend fun getHighVolatilityRecords(wishlistItemId: Long, threshold: Double = 10.0): List<WishlistPriceHistoryEntity>
    
    /**
     * 获取连续降价的记录
     */
    @Query("""
        SELECT h1.* FROM wishlist_price_history h1
        INNER JOIN wishlist_price_history h2 ON h1.wishlistItemId = h2.wishlistItemId
        WHERE h1.wishlistItemId = :wishlistItemId 
        AND h1.priceChange < 0 
        AND h2.priceChange < 0
        AND h1.recordDate > h2.recordDate
        ORDER BY h1.recordDate DESC
    """)
    suspend fun getConsecutivePriceDrops(wishlistItemId: Long): List<WishlistPriceHistoryEntity>
    
    /**
     * 获取最活跃的价格更新物品统计
     */
    @Query("""
        SELECT wishlistItemId, COUNT(*) as updateCount 
        FROM wishlist_price_history 
        WHERE recordDate >= :sinceDate 
        GROUP BY wishlistItemId 
        ORDER BY updateCount DESC 
        LIMIT :limit
    """)
    suspend fun getMostActivelyTrackedItems(sinceDate: Long, limit: Int = 10): List<ItemUpdateCount>
}

/**
 * 物品更新次数统计数据类
 */
data class ItemUpdateCount(
    val wishlistItemId: Long,
    val updateCount: Int
)
