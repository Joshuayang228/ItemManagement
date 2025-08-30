package com.example.itemmanagement.data.repository

import com.example.itemmanagement.data.dao.BorrowDao
import com.example.itemmanagement.data.dao.BorrowStatistics
import com.example.itemmanagement.data.dao.BorrowWithItemInfo
import com.example.itemmanagement.data.entity.BorrowEntity
import com.example.itemmanagement.data.entity.BorrowStatus
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Date

/**
 * 借还管理数据仓库
 * 封装借还相关的数据访问逻辑，为上层提供简洁的数据接口
 */
class BorrowRepository(
    private val borrowDao: BorrowDao
) {
    
    // ==================== 基本CRUD操作 ====================
    
    /**
     * 添加借还记录
     * @param borrow 借还实体
     * @return 插入记录的ID
     */
    suspend fun insertBorrow(borrow: BorrowEntity): Long {
        return borrowDao.insert(borrow)
    }
    
    /**
     * 更新借还记录
     */
    suspend fun updateBorrow(borrow: BorrowEntity) {
        borrowDao.update(borrow)
    }
    
    /**
     * 删除借还记录
     */
    suspend fun deleteBorrow(borrow: BorrowEntity) {
        borrowDao.delete(borrow)
    }
    
    /**
     * 根据ID删除借还记录
     */
    suspend fun deleteBorrowById(borrowId: Long) {
        borrowDao.deleteById(borrowId)
    }
    
    /**
     * 根据ID获取借还记录
     */
    suspend fun getBorrowById(borrowId: Long): BorrowEntity? {
        return borrowDao.getById(borrowId)
    }
    
    // ==================== 查询操作 ====================
    
    /**
     * 获取所有借还记录（Flow）
     */
    fun getAllBorrowsFlow(): Flow<List<BorrowEntity>> {
        return borrowDao.getAllFlow()
    }
    
    /**
     * 获取所有借还记录（List）
     */
    suspend fun getAllBorrows(): List<BorrowEntity> {
        return borrowDao.getAll()
    }
    
    /**
     * 根据状态获取借还记录
     */
    fun getBorrowsByStatusFlow(status: BorrowStatus): Flow<List<BorrowEntity>> {
        return borrowDao.getByStatusFlow(status)
    }
    
    /**
     * 根据物品ID获取借还记录
     */
    fun getBorrowsByItemIdFlow(itemId: Long): Flow<List<BorrowEntity>> {
        return borrowDao.getByItemIdFlow(itemId)
    }
    
    /**
     * 获取当前未归还的记录
     */
    fun getCurrentBorrowedFlow(): Flow<List<BorrowEntity>> {
        return borrowDao.getCurrentBorrowedFlow()
    }
    
    /**
     * 获取借还记录统计信息
     */
    suspend fun getStatistics(): BorrowStatistics {
        return borrowDao.getStatistics()
    }
    
    // ==================== 联表查询操作 ====================
    
    /**
     * 获取所有借还记录（带物品信息）
     */
    fun getAllBorrowsWithItemInfoFlow(): Flow<List<BorrowWithItemInfo>> {
        return borrowDao.getAllWithItemInfoFlow()
    }
    
    /**
     * 根据状态获取借还记录（带物品信息）
     */
    fun getBorrowsWithItemInfoByStatusFlow(status: BorrowStatus): Flow<List<BorrowWithItemInfo>> {
        return borrowDao.getByStatusWithItemInfoFlow(status)
    }
    
    // ==================== 业务逻辑操作 ====================
    
    /**
     * 创建借出记录
     * @param itemId 物品ID
     * @param borrowerName 借用人姓名
     * @param borrowerContact 借用人联系方式
     * @param expectedReturnDate 预计归还日期
     * @param notes 备注
     * @return 创建的借还记录ID
     */
    suspend fun createBorrowRecord(
        itemId: Long,
        borrowerName: String,
        borrowerContact: String? = null,
        expectedReturnDate: Date,
        notes: String? = null
    ): Long {
        val now = System.currentTimeMillis()
        val borrowEntity = BorrowEntity(
            itemId = itemId,
            borrowerName = borrowerName,
            borrowerContact = borrowerContact,
            borrowDate = now,
            expectedReturnDate = expectedReturnDate.time,
            status = BorrowStatus.BORROWED,
            notes = notes,
            createdDate = now,
            updatedDate = now
        )
        return insertBorrow(borrowEntity)
    }
    
    /**
     * 归还物品
     * @param borrowId 借还记录ID
     * @return 更新是否成功
     */
    suspend fun returnItem(borrowId: Long): Boolean {
        val borrow = getBorrowById(borrowId) ?: return false
        
        if (borrow.status != BorrowStatus.BORROWED && borrow.status != BorrowStatus.OVERDUE) {
            return false // 只有BORROWED或OVERDUE状态才能归还
        }
        
        val now = System.currentTimeMillis()
        val updatedBorrow = borrow.copy(
            actualReturnDate = now,
            status = BorrowStatus.RETURNED,
            updatedDate = now
        )
        
        updateBorrow(updatedBorrow)
        return true
    }
    
    /**
     * 更新逾期的借还记录状态
     * @return 更新的记录数量
     */
    suspend fun updateOverdueRecords(): Int {
        val currentTime = System.currentTimeMillis()
        return borrowDao.updateOverdueStatus(currentTime, currentTime)
    }
    
    /**
     * 获取逾期的借还记录
     * @return 逾期记录列表
     */
    suspend fun getOverdueRecords(): List<BorrowEntity> {
        val currentTime = System.currentTimeMillis()
        return borrowDao.getOverdueRecords(currentTime)
    }
    
    /**
     * 获取即将到期的借还记录（带物品信息）
     * @param advanceDays 提前天数，默认3天
     * @return 即将到期的记录列表
     */
    suspend fun getSoonExpireRecordsWithItemInfo(advanceDays: Int = 3): List<BorrowWithItemInfo> {
        val currentTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTime
            add(Calendar.DAY_OF_YEAR, advanceDays)
        }
        val thresholdTime = calendar.timeInMillis
        
        return borrowDao.getSoonExpireWithItemInfo(currentTime, thresholdTime)
    }
    
    /**
     * 检查物品是否当前被借出
     * @param itemId 物品ID
     * @return 是否被借出
     */
    suspend fun isItemCurrentlyBorrowed(itemId: Long): Boolean {
        val borrows = borrowDao.getByItemIdFlow(itemId)
        // 这里需要考虑如何实现，暂时返回false
        // 实际实现可能需要通过suspend方法获取当前状态
        return false // TODO: 实现检查逻辑
    }
    
    /**
     * 获取物品的当前借出记录
     * @param itemId 物品ID
     * @return 当前借出记录，如果没有则返回null
     */
    suspend fun getCurrentBorrowRecordForItem(itemId: Long): BorrowEntity? {
        // 获取该物品的所有借还记录，找到状态为BORROWED或OVERDUE的记录
        val allBorrows = borrowDao.getAll()
        return allBorrows.find { 
            it.itemId == itemId && 
            (it.status == BorrowStatus.BORROWED || it.status == BorrowStatus.OVERDUE)
        }
    }
    
    /**
     * 获取借还历史记录
     * @param limit 限制数量，默认50条
     * @return 历史记录列表
     */
    suspend fun getBorrowHistory(limit: Int = 50): List<BorrowEntity> {
        // TODO: 实现带limit的查询
        return borrowDao.getAll().take(limit)
    }
}
