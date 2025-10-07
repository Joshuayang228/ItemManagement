package com.example.itemmanagement.data.dao

import androidx.room.*
import com.example.itemmanagement.data.entity.WarrantyEntity
import com.example.itemmanagement.data.entity.WarrantyStatus
import kotlinx.coroutines.flow.Flow

/**
 * 保修信息数据访问对象
 * 提供保修数据的CRUD操作和查询功能
 */
@Dao
interface WarrantyDao {
    
    // ==================== 基本CRUD操作 ====================
    
    /**
     * 插入保修记录
     * @param warranty 保修实体
     * @return 插入记录的ID
     */
    @Insert
    suspend fun insert(warranty: WarrantyEntity): Long
    
    /**
     * 更新保修记录
     */
    @Update
    suspend fun update(warranty: WarrantyEntity)
    
    /**
     * 删除保修记录
     */
    @Delete
    suspend fun delete(warranty: WarrantyEntity)
    
    /**
     * 根据ID删除保修记录
     */
    @Query("DELETE FROM warranties WHERE id = :warrantyId")
    suspend fun deleteById(warrantyId: Long)
    
    // ==================== 查询操作 ====================
    
    /**
     * 根据ID获取保修记录
     */
    @Query("SELECT * FROM warranties WHERE id = :warrantyId")
    suspend fun getById(warrantyId: Long): WarrantyEntity?
    
    /**
     * 根据物品ID获取保修记录
     * 一个物品对应一个保修记录
     */
    @Query("SELECT * FROM warranties WHERE itemId = :itemId")
    suspend fun getByItemId(itemId: Long): WarrantyEntity?
    
    /**
     * 获取所有保修记录（Flow）
     * 用于实时更新UI
     */
    @Query("SELECT * FROM warranties ORDER BY warrantyEndDate ASC")
    fun getAllWarranties(): Flow<List<WarrantyEntity>>
    
    /**
     * 获取所有保修记录（一次性）
     */
    @Query("SELECT * FROM warranties ORDER BY warrantyEndDate ASC")
    suspend fun getAllWarrantiesOnce(): List<WarrantyEntity>
    
    // ==================== 到期提醒相关查询 ====================
    
    /**
     * 获取即将到期的保修记录
     * @param currentTime 当前时间戳
     * @param advanceDays 提前天数
     * @return 即将到期的保修记录
     */
    @Query("""
        SELECT * FROM warranties 
        WHERE status = 'ACTIVE' 
        AND warrantyEndDate > :currentTime 
        AND warrantyEndDate <= :thresholdTime
        ORDER BY warrantyEndDate ASC
    """)
    suspend fun getWarrantiesNearingExpiration(currentTime: Long, thresholdTime: Long): List<WarrantyEntity>
    
    /**
     * 获取已过期但状态仍为ACTIVE的保修记录
     * 用于批量更新状态
     */
    @Query("""
        SELECT * FROM warranties 
        WHERE status = 'ACTIVE' 
        AND warrantyEndDate < :currentTime
    """)
    suspend fun getExpiredActiveWarranties(currentTime: Long): List<WarrantyEntity>
    
    /**
     * 获取即将到期的保修记录(带物品信息)
     */
    @Query("""
        SELECT 
            w.id, w.itemId, w.purchaseDate, w.warrantyPeriodMonths, w.warrantyEndDate,
            w.receiptImageUris, w.notes, w.status, w.warrantyProvider, w.contactInfo,
            w.createdDate, w.updatedDate,
            i.name as itemName, i.brand, i.category,
            (SELECT uri FROM photos WHERE itemId = w.itemId LIMIT 1) as firstPhotoUri
        FROM warranties w
        INNER JOIN unified_items i ON w.itemId = i.id
        WHERE w.status = 'ACTIVE'
        AND w.warrantyEndDate > :currentTime 
        AND w.warrantyEndDate <= :thresholdTime
        ORDER BY w.warrantyEndDate ASC
    """)
    suspend fun getWarrantiesNearingExpirationWithItems(currentTime: Long, thresholdTime: Long): List<WarrantyWithItemInfo>
    
    /**
     * 批量更新过期保修状态
     */
    @Query("""
        UPDATE warranties 
        SET status = 'EXPIRED', updatedDate = :currentTime
        WHERE status = 'ACTIVE' 
        AND warrantyEndDate < :currentTime
    """)
    suspend fun updateExpiredWarranties(currentTime: Long): Int
    
    // ==================== 统计和分析查询 ====================
    
    /**
     * 获取有效保修数量
     */
    @Query("SELECT COUNT(*) FROM warranties WHERE status = 'ACTIVE'")
    suspend fun getActiveWarrantyCount(): Int
    
    /**
     * 获取已过期保修数量
     */
    @Query("SELECT COUNT(*) FROM warranties WHERE status = 'EXPIRED'")
    suspend fun getExpiredWarrantyCount(): Int
    
    /**
     * 获取按状态分组的保修统计
     */
    @Query("""
        SELECT status, COUNT(*) as count
        FROM warranties 
        GROUP BY status
    """)
    suspend fun getWarrantyStatusCounts(): List<WarrantyStatusCount>
    
    /**
     * 获取未来N天内到期的保修数量
     */
    @Query("""
        SELECT COUNT(*) FROM warranties 
        WHERE status = 'ACTIVE' 
        AND warrantyEndDate BETWEEN :currentTime AND :futureTime
    """)
    suspend fun getWarrantiesExpiringInDays(currentTime: Long, futureTime: Long): Int
    
    // ==================== 与物品关联的查询 ====================
    
    /**
     * 获取包含物品信息的保修记录
     */
    @Query("""
        SELECT 
            w.id, w.itemId, w.purchaseDate, w.warrantyPeriodMonths, w.warrantyEndDate,
            w.receiptImageUris, w.notes, w.status, w.warrantyProvider, w.contactInfo,
            w.createdDate, w.updatedDate,
            i.name as itemName, i.brand, i.category,
            (SELECT uri FROM photos WHERE itemId = w.itemId LIMIT 1) as firstPhotoUri
        FROM warranties w
        INNER JOIN unified_items i ON w.itemId = i.id
        ORDER BY w.warrantyEndDate ASC
    """)
    fun getWarrantiesWithItemInfo(): Flow<List<WarrantyWithItemInfo>>
    
    /**
     * 根据物品名称搜索保修记录
     */
    @Query("""
        SELECT w.* FROM warranties w
        INNER JOIN unified_items i ON w.itemId = i.id
        WHERE i.name LIKE '%' || :itemName || '%'
        ORDER BY w.warrantyEndDate ASC
    """)
    suspend fun searchWarrantiesByItemName(itemName: String): List<WarrantyEntity>
    
    // ==================== 批量操作 ====================
    
    /**
     * 批量插入保修记录
     */
    @Insert
    suspend fun insertAll(warranties: List<WarrantyEntity>): List<Long>
    
    /**
     * 检查物品是否已有保修记录
     */
    @Query("SELECT COUNT(*) FROM warranties WHERE itemId = :itemId")
    suspend fun hasWarranty(itemId: Long): Int
}

/**
 * 保修状态统计结果
 */
data class WarrantyStatusCount(
    val status: WarrantyStatus,
    val count: Int
)

/**
 * 包含物品信息的保修记录
 */
data class WarrantyWithItemInfo(
    val id: Long,
    val itemId: Long,
    val purchaseDate: Long,
    val warrantyPeriodMonths: Int,
    val warrantyEndDate: Long,
    val receiptImageUris: String?,
    val notes: String?,
    val status: WarrantyStatus,
    val warrantyProvider: String?,
    val contactInfo: String?,
    val createdDate: Long,
    val updatedDate: Long,
    val itemName: String,
    val brand: String?,
    val category: String,
    val firstPhotoUri: String?  // 物品的第一张照片URI
)
