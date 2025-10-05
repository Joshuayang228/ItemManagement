package com.example.itemmanagement.data.dao.unified

import androidx.room.*
import com.example.itemmanagement.data.entity.unified.InventoryDetailEntity
import com.example.itemmanagement.data.model.ItemStatus
import com.example.itemmanagement.data.model.OpenStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 库存状态统计数据类
 */
data class InventoryStatusCountInfo(
    val status: String,
    val count: Int
)

/**
 * 位置统计数据类
 */
data class LocationCountInfo(
    val locationId: Long,
    val count: Int
)

/**
 * 库存详情数据访问接口
 * 管理物品在库存状态下的专用数据
 */
@Dao
interface InventoryDetailDao {
    
    // === 基础CRUD操作 ===
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(detail: InventoryDetailEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(details: List<InventoryDetailEntity>): List<Long>
    
    @Update
    suspend fun update(detail: InventoryDetailEntity)
    
    @Delete
    suspend fun delete(detail: InventoryDetailEntity)
    
    @Query("DELETE FROM inventory_details WHERE itemId = :itemId")
    suspend fun deleteByItemId(itemId: Long): Int
    
    // === 查询操作 ===
    
    @Query("SELECT * FROM inventory_details WHERE itemId = :itemId")
    suspend fun getByItemId(itemId: Long): InventoryDetailEntity?
    
    @Query("SELECT * FROM inventory_details ORDER BY expirationDate ASC")
    fun getAllDetails(): Flow<List<InventoryDetailEntity>>
    
    @Query("SELECT * FROM inventory_details WHERE status = :status ORDER BY expirationDate ASC")
    fun getByStatus(status: ItemStatus): Flow<List<InventoryDetailEntity>>
    
    @Query("SELECT * FROM inventory_details WHERE locationId = :locationId ORDER BY expirationDate ASC")
    fun getByLocationId(locationId: Long): Flow<List<InventoryDetailEntity>>
    
    // === 过期和库存警告查询 ===
    
    @Query("""
        SELECT * FROM inventory_details 
        WHERE expirationDate IS NOT NULL 
        AND expirationDate <= :date
        ORDER BY expirationDate ASC
    """)
    fun getExpiredItems(date: Date = Date()): Flow<List<InventoryDetailEntity>>
    
    @Query("""
        SELECT * FROM inventory_details 
        WHERE expirationDate IS NOT NULL 
        AND expirationDate > :now 
        AND expirationDate <= :warningDate
        ORDER BY expirationDate ASC
    """)
    fun getItemsNearExpiration(now: Date = Date(), warningDate: Date): Flow<List<InventoryDetailEntity>>
    
    @Query("""
        SELECT * FROM inventory_details 
        WHERE stockWarningThreshold IS NOT NULL 
        AND quantity <= stockWarningThreshold
        ORDER BY quantity ASC
    """)
    fun getLowStockItems(): Flow<List<InventoryDetailEntity>>
    
    @Query("SELECT * FROM inventory_details WHERE status = 'OUT_OF_STOCK' ORDER BY updatedDate DESC")
    fun getOutOfStockItems(): Flow<List<InventoryDetailEntity>>
    
    // === 保修相关查询 ===
    
    @Query("""
        SELECT * FROM inventory_details 
        WHERE warrantyEndDate IS NOT NULL 
        AND warrantyEndDate > :now 
        AND warrantyEndDate <= :warningDate
        ORDER BY warrantyEndDate ASC
    """)
    fun getItemsWithWarrantyNearExpiration(now: Date = Date(), warningDate: Date): Flow<List<InventoryDetailEntity>>
    
    @Query("""
        SELECT * FROM inventory_details 
        WHERE warrantyEndDate IS NOT NULL 
        AND warrantyEndDate <= :date
        ORDER BY warrantyEndDate ASC
    """)
    fun getItemsWithExpiredWarranty(date: Date = Date()): Flow<List<InventoryDetailEntity>>
    
    // === 开封状态查询 ===
    
    @Query("SELECT * FROM inventory_details WHERE openStatus = :openStatus ORDER BY openDate DESC")
    fun getByOpenStatus(openStatus: OpenStatus): Flow<List<InventoryDetailEntity>>
    
    @Query("SELECT * FROM inventory_details WHERE openStatus = 'OPENED' AND openDate IS NOT NULL ORDER BY openDate ASC")
    fun getOpenedItems(): Flow<List<InventoryDetailEntity>>
    
    // === 更新操作 ===
    
    @Query("UPDATE inventory_details SET quantity = :quantity, updatedDate = :updateDate WHERE itemId = :itemId")
    suspend fun updateQuantity(itemId: Long, quantity: Double, updateDate: Date = Date())
    
    @Query("UPDATE inventory_details SET status = :status, updatedDate = :updateDate WHERE itemId = :itemId")
    suspend fun updateStatus(itemId: Long, status: ItemStatus, updateDate: Date = Date())
    
    @Query("UPDATE inventory_details SET locationId = :locationId, updatedDate = :updateDate WHERE itemId = :itemId")
    suspend fun updateLocation(itemId: Long, locationId: Long?, updateDate: Date = Date())
    
    @Query("UPDATE inventory_details SET openStatus = :openStatus, openDate = :openDate, updatedDate = :updateDate WHERE itemId = :itemId")
    suspend fun updateOpenStatus(
        itemId: Long, 
        openStatus: OpenStatus, 
        openDate: Date = Date(),
        updateDate: Date = Date()
    )
    
    @Query("UPDATE inventory_details SET price = :price, priceUnit = :priceUnit, updatedDate = :updateDate WHERE itemId = :itemId")
    suspend fun updatePrice(itemId: Long, price: Double?, priceUnit: String?, updateDate: Date = Date())
    
    @Query("UPDATE inventory_details SET stockWarningThreshold = :threshold WHERE itemId = :itemId")
    suspend fun updateStockWarningThreshold(itemId: Long, threshold: Int?)
    
    @Query("UPDATE inventory_details SET isHighTurnover = :isHighTurnover WHERE itemId = :itemId")
    suspend fun updateHighTurnoverStatus(itemId: Long, isHighTurnover: Boolean)
    
    @Query("UPDATE inventory_details SET wasteDate = :wasteDate WHERE itemId = :itemId")
    suspend fun updateWasteDate(itemId: Long, wasteDate: Date?)
    
    // === 统计查询 ===
    
    @Query("SELECT COUNT(*) FROM inventory_details")
    suspend fun getDetailCount(): Int
    
    @Query("SELECT COUNT(*) FROM inventory_details WHERE status = :status")
    suspend fun getCountByStatus(status: ItemStatus): Int
    
    @Query("SELECT COUNT(*) FROM inventory_details WHERE locationId = :locationId")
    suspend fun getCountByLocation(locationId: Long): Int
    
    @Query("SELECT SUM(quantity) FROM inventory_details WHERE status = 'IN_STOCK'")
    suspend fun getTotalInStockQuantity(): Double?
    
    @Query("SELECT COUNT(*) FROM inventory_details WHERE expirationDate <= :date")
    suspend fun getExpiredItemsCount(date: Date = Date()): Int
    
    @Query("SELECT COUNT(*) FROM inventory_details WHERE stockWarningThreshold IS NOT NULL AND quantity <= stockWarningThreshold")
    suspend fun getLowStockItemsCount(): Int
    
    @Query("""
        SELECT status, COUNT(*) as count 
        FROM inventory_details 
        GROUP BY status
    """)
    suspend fun getStatusDistribution(): List<InventoryStatusCountInfo>
    
    @Query("""
        SELECT locationId, COUNT(*) as count 
        FROM inventory_details 
        WHERE locationId IS NOT NULL 
        GROUP BY locationId
    """)
    suspend fun getLocationDistribution(): List<LocationCountInfo>
    
    // === 高级查询 ===
    
    /**
     * 获取高周转物品
     */
    @Query("SELECT * FROM inventory_details WHERE isHighTurnover = 1 ORDER BY updatedDate DESC")
    fun getHighTurnoverItems(): Flow<List<InventoryDetailEntity>>
    
    /**
     * 获取最近添加的库存
     */
    @Query("SELECT * FROM inventory_details ORDER BY createdDate DESC LIMIT :limit")
    suspend fun getRecentItems(limit: Int = 10): List<InventoryDetailEntity>
    
    /**
     * 获取最近更新的库存
     */
    @Query("SELECT * FROM inventory_details ORDER BY updatedDate DESC LIMIT :limit")
    suspend fun getRecentlyUpdatedItems(limit: Int = 10): List<InventoryDetailEntity>
    
    /**
     * 搜索库存详情
     * 注意：serialNumber、season等物品属性已移至UnifiedItemEntity，需通过JOIN查询
     */
    @Query("""
        SELECT * FROM inventory_details 
        WHERE purchaseChannel LIKE '%' || :keyword || '%'
        OR storeName LIKE '%' || :keyword || '%'
        ORDER BY updatedDate DESC
    """)
    fun searchDetails(keyword: String): Flow<List<InventoryDetailEntity>>
    
    /**
     * 获取需要关注的物品（即将过期+库存不足+保修到期）
     */
    @Query("""
        SELECT * FROM inventory_details 
        WHERE (expirationDate IS NOT NULL AND expirationDate <= :warningDate)
        OR (stockWarningThreshold IS NOT NULL AND quantity <= stockWarningThreshold)
        OR (warrantyEndDate IS NOT NULL AND warrantyEndDate <= :warrantyWarningDate)
        ORDER BY 
            CASE 
                WHEN expirationDate IS NOT NULL AND expirationDate <= :now THEN 1
                WHEN stockWarningThreshold IS NOT NULL AND quantity <= stockWarningThreshold THEN 2
                WHEN expirationDate IS NOT NULL AND expirationDate <= :warningDate THEN 3
                WHEN warrantyEndDate IS NOT NULL AND warrantyEndDate <= :warrantyWarningDate THEN 4
                ELSE 5
            END,
            expirationDate ASC
    """)
    fun getItemsNeedingAttention(
        now: Date = Date(),
        warningDate: Date,
        warrantyWarningDate: Date
    ): Flow<List<InventoryDetailEntity>>
    
    // === 批量操作 ===
    
    @Query("UPDATE inventory_details SET status = :status WHERE itemId IN (:itemIds)")
    suspend fun batchUpdateStatus(itemIds: List<Long>, status: ItemStatus)
    
    @Query("UPDATE inventory_details SET locationId = :locationId WHERE itemId IN (:itemIds)")
    suspend fun batchUpdateLocation(itemIds: List<Long>, locationId: Long?)
    
    @Query("DELETE FROM inventory_details WHERE itemId IN (:itemIds)")
    suspend fun deleteByItemIds(itemIds: List<Long>): Int
    
    /**
     * 批量调整库存数量
     */
    @Query("UPDATE inventory_details SET quantity = quantity + :adjustment, updatedDate = :updateDate WHERE itemId IN (:itemIds)")
    suspend fun batchAdjustQuantity(itemIds: List<Long>, adjustment: Double, updateDate: Date = Date())
    
    // === 清理操作 ===
    
    @Query("DELETE FROM inventory_details WHERE status = 'DISCARDED' AND updatedDate < :beforeDate")
    suspend fun cleanupOldDiscardedItems(beforeDate: Date): Int
    
    @Query("UPDATE inventory_details SET wasteDate = :wasteDate WHERE status IN ('EXPIRED', 'DISCARDED') AND wasteDate IS NULL")
    suspend fun updateMissingWasteDates(wasteDate: Date = Date())
}
