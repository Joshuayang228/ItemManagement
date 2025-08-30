package com.example.itemmanagement.data.dao

import androidx.room.*
import com.example.itemmanagement.data.entity.BorrowEntity
import com.example.itemmanagement.data.entity.BorrowStatus
import kotlinx.coroutines.flow.Flow

/**
 * 借还记录数据访问接口
 * 提供借还数据的CRUD操作和查询功能
 */
@Dao
interface BorrowDao {
    
    // ==================== 基本CRUD操作 ====================
    
    /**
     * 插入借还记录
     * @param borrow 借还实体
     * @return 插入记录的ID
     */
    @Insert
    suspend fun insert(borrow: BorrowEntity): Long
    
    /**
     * 更新借还记录
     */
    @Update
    suspend fun update(borrow: BorrowEntity)
    
    /**
     * 删除借还记录
     */
    @Delete
    suspend fun delete(borrow: BorrowEntity)
    
    /**
     * 根据ID删除借还记录
     */
    @Query("DELETE FROM borrows WHERE id = :borrowId")
    suspend fun deleteById(borrowId: Long)
    
    // ==================== 查询操作 ====================
    
    /**
     * 根据ID获取借还记录
     */
    @Query("SELECT * FROM borrows WHERE id = :borrowId")
    suspend fun getById(borrowId: Long): BorrowEntity?
    
    /**
     * 获取所有借还记录（Flow）
     */
    @Query("SELECT * FROM borrows ORDER BY borrowDate DESC")
    fun getAllFlow(): Flow<List<BorrowEntity>>
    
    /**
     * 获取所有借还记录（List）
     */
    @Query("SELECT * FROM borrows ORDER BY borrowDate DESC")
    suspend fun getAll(): List<BorrowEntity>
    
    /**
     * 根据状态获取借还记录
     */
    @Query("SELECT * FROM borrows WHERE status = :status ORDER BY borrowDate DESC")
    fun getByStatusFlow(status: BorrowStatus): Flow<List<BorrowEntity>>
    
    /**
     * 根据物品ID获取借还记录
     */
    @Query("SELECT * FROM borrows WHERE itemId = :itemId ORDER BY borrowDate DESC")
    fun getByItemIdFlow(itemId: Long): Flow<List<BorrowEntity>>
    
    /**
     * 获取当前未归还的记录（BORROWED状态）
     */
    @Query("SELECT * FROM borrows WHERE status = 'BORROWED' ORDER BY expectedReturnDate ASC")
    fun getCurrentBorrowedFlow(): Flow<List<BorrowEntity>>
    
    /**
     * 获取已逾期的记录
     */
    @Query("""
        SELECT * FROM borrows 
        WHERE status = 'BORROWED' 
        AND expectedReturnDate < :currentTime
        ORDER BY expectedReturnDate ASC
    """)
    suspend fun getOverdueRecords(currentTime: Long): List<BorrowEntity>
    
    /**
     * 获取即将到期的记录（未归还且在指定时间范围内）
     */
    @Query("""
        SELECT * FROM borrows 
        WHERE status = 'BORROWED' 
        AND expectedReturnDate > :currentTime 
        AND expectedReturnDate <= :thresholdTime
        ORDER BY expectedReturnDate ASC
    """)
    suspend fun getSoonExpireRecords(currentTime: Long, thresholdTime: Long): List<BorrowEntity>
    
    /**
     * 批量更新逾期状态
     */
    @Query("""
        UPDATE borrows 
        SET status = 'OVERDUE', updatedDate = :updateTime 
        WHERE status = 'BORROWED' 
        AND expectedReturnDate < :currentTime
    """)
    suspend fun updateOverdueStatus(currentTime: Long, updateTime: Long): Int
    
    /**
     * 获取借还记录统计信息
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            COUNT(CASE WHEN status = 'BORROWED' THEN 1 END) as borrowed,
            COUNT(CASE WHEN status = 'RETURNED' THEN 1 END) as returned,
            COUNT(CASE WHEN status = 'OVERDUE' THEN 1 END) as overdue
        FROM borrows
    """)
    suspend fun getStatistics(): BorrowStatistics
    
    // ==================== 联表查询 ====================
    
    /**
     * 获取所有借还记录（带物品信息）
     */
    @Query("""
        SELECT 
            b.id, b.itemId, b.borrowerName, b.borrowerContact,
            b.borrowDate, b.expectedReturnDate, b.actualReturnDate,
            b.status, b.notes, b.createdDate, b.updatedDate,
            i.name as itemName, i.brand, i.category,
            (SELECT p.uri FROM photos p WHERE p.itemId = i.id LIMIT 1) as photoUri
        FROM borrows b
        INNER JOIN items i ON b.itemId = i.id
        ORDER BY b.borrowDate DESC
    """)
    fun getAllWithItemInfoFlow(): Flow<List<BorrowWithItemInfo>>
    
    /**
     * 根据状态获取借还记录（带物品信息）
     */
    @Query("""
        SELECT 
            b.id, b.itemId, b.borrowerName, b.borrowerContact,
            b.borrowDate, b.expectedReturnDate, b.actualReturnDate,
            b.status, b.notes, b.createdDate, b.updatedDate,
            i.name as itemName, i.brand, i.category,
            (SELECT p.uri FROM photos p WHERE p.itemId = i.id LIMIT 1) as photoUri
        FROM borrows b
        INNER JOIN items i ON b.itemId = i.id
        WHERE b.status = :status
        ORDER BY b.borrowDate DESC
    """)
    fun getByStatusWithItemInfoFlow(status: BorrowStatus): Flow<List<BorrowWithItemInfo>>
    
    /**
     * 获取即将到期的借还记录（带物品信息）
     * 用于提醒系统
     */
    @Query("""
        SELECT 
            b.id, b.itemId, b.borrowerName, b.borrowerContact,
            b.borrowDate, b.expectedReturnDate, b.actualReturnDate,
            b.status, b.notes, b.createdDate, b.updatedDate,
            i.name as itemName, i.brand, i.category,
            (SELECT p.uri FROM photos p WHERE p.itemId = i.id LIMIT 1) as photoUri
        FROM borrows b
        INNER JOIN items i ON b.itemId = i.id
        WHERE b.status = 'BORROWED' 
        AND b.expectedReturnDate > :currentTime 
        AND b.expectedReturnDate <= :thresholdTime
        ORDER BY b.expectedReturnDate ASC
    """)
    suspend fun getSoonExpireWithItemInfo(currentTime: Long, thresholdTime: Long): List<BorrowWithItemInfo>
}

/**
 * 借还统计数据
 */
data class BorrowStatistics(
    val total: Int,
    val borrowed: Int, 
    val returned: Int,
    val overdue: Int
)

/**
 * 包含物品信息的借还记录
 */
data class BorrowWithItemInfo(
    val id: Long,
    val itemId: Long,
    val borrowerName: String,
    val borrowerContact: String?,
    val borrowDate: Long,
    val expectedReturnDate: Long,
    val actualReturnDate: Long?,
    val status: BorrowStatus,
    val notes: String?,
    val createdDate: Long,
    val updatedDate: Long,
    // 物品信息
    val itemName: String,
    val brand: String?,
    val category: String,
    val photoUri: String?
)
