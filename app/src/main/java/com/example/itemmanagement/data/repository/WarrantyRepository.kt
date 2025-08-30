package com.example.itemmanagement.data.repository

import com.example.itemmanagement.data.dao.WarrantyDao
import com.example.itemmanagement.data.dao.WarrantyStatusCount
import com.example.itemmanagement.data.dao.WarrantyWithItemInfo
import com.example.itemmanagement.data.entity.WarrantyEntity
import com.example.itemmanagement.data.entity.WarrantyStatus
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Date


/**
 * 保修管理数据仓库
 * 封装保修相关的数据访问逻辑，为上层提供简洁的数据接口
 */
class WarrantyRepository(
    private val warrantyDao: WarrantyDao
) {
    
    // ==================== 基本CRUD操作 ====================
    
    /**
     * 添加保修记录
     * @param warranty 保修实体
     * @return 插入记录的ID
     */
    suspend fun insertWarranty(warranty: WarrantyEntity): Long {
        return warrantyDao.insert(warranty)
    }
    
    /**
     * 更新保修记录
     */
    suspend fun updateWarranty(warranty: WarrantyEntity) {
        warrantyDao.update(warranty.copy(updatedDate = Date()))
    }
    
    /**
     * 删除保修记录
     */
    suspend fun deleteWarranty(warranty: WarrantyEntity) {
        warrantyDao.delete(warranty)
    }
    
    /**
     * 根据ID删除保修记录
     */
    suspend fun deleteWarrantyById(warrantyId: Long) {
        warrantyDao.deleteById(warrantyId)
    }
    
    // ==================== 查询操作 ====================
    
    /**
     * 根据ID获取保修记录
     */
    suspend fun getWarrantyById(warrantyId: Long): WarrantyEntity? {
        return warrantyDao.getById(warrantyId)
    }
    
    /**
     * 根据物品ID获取保修记录
     */
    suspend fun getWarrantyByItemId(itemId: Long): WarrantyEntity? {
        return warrantyDao.getByItemId(itemId)
    }
    
    /**
     * 获取所有保修记录（Flow）
     * 支持实时更新
     */
    fun getAllWarrantiesStream(): Flow<List<WarrantyEntity>> {
        return warrantyDao.getAllWarranties()
    }
    
    /**
     * 获取包含物品信息的保修记录（Flow）
     * 注意：此功能需要后续实现复杂的JOIN查询
     */
    fun getWarrantiesWithItemInfoStream(): Flow<List<WarrantyWithItemInfo>> {
        return warrantyDao.getWarrantiesWithItemInfo()
    }

    
    /**
     * 搜索保修记录
     */
    suspend fun searchWarrantiesByItemName(itemName: String): List<WarrantyEntity> {
        return warrantyDao.searchWarrantiesByItemName(itemName)
    }
    
    // ==================== 到期提醒相关 ====================
    
    /**
     * 获取即将到期的保修记录
     * @param advanceDays 提前天数，默认7天
     */
    suspend fun getWarrantiesNearingExpiration(advanceDays: Int = 7): List<WarrantyEntity> {
        val currentTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTime
            add(Calendar.DAY_OF_YEAR, advanceDays)
        }
        val thresholdTime = calendar.timeInMillis
        
        return warrantyDao.getWarrantiesNearingExpiration(currentTime, thresholdTime)
    }
    
    /**
     * 获取即将到期的保修记录（带物品信息）
     * @param advanceDays 提前天数，默认7天
     */
    suspend fun getWarrantiesNearingExpirationWithItems(advanceDays: Int = 7): List<WarrantyWithItemInfo> {
        val currentTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTime
            add(Calendar.DAY_OF_YEAR, advanceDays)
        }
        val thresholdTime = calendar.timeInMillis
        
        return warrantyDao.getWarrantiesNearingExpirationWithItems(currentTime, thresholdTime)
    }
    
    /**
     * 更新过期的保修状态
     * @return 更新的记录数量
     */
    suspend fun updateExpiredWarranties(): Int {
        val currentTime = System.currentTimeMillis()
        return warrantyDao.updateExpiredWarranties(currentTime)
    }
    
    /**
     * 检查并获取过期但仍为ACTIVE状态的保修记录
     */
    suspend fun getExpiredActiveWarranties(): List<WarrantyEntity> {
        val currentTime = System.currentTimeMillis()
        return warrantyDao.getExpiredActiveWarranties(currentTime)
    }
    
    // ==================== 统计分析 ====================
    
    /**
     * 获取有效保修数量
     */
    suspend fun getActiveWarrantyCount(): Int {
        return warrantyDao.getActiveWarrantyCount()
    }
    
    /**
     * 获取已过期保修数量
     */
    suspend fun getExpiredWarrantyCount(): Int {
        return warrantyDao.getExpiredWarrantyCount()
    }
    
    /**
     * 获取按状态分组的保修统计
     */
    suspend fun getWarrantyStatusCounts(): List<WarrantyStatusCount> {
        return warrantyDao.getWarrantyStatusCounts()
    }
    
    /**
     * 获取未来指定天数内到期的保修数量
     */
    suspend fun getWarrantiesExpiringInDays(days: Int): Int {
        val currentTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTime
            add(Calendar.DAY_OF_YEAR, days)
        }
        val futureTime = calendar.timeInMillis
        
        return warrantyDao.getWarrantiesExpiringInDays(currentTime, futureTime)
    }
    
    // ==================== 便捷方法 ====================
    
    /**
     * 检查物品是否已有保修记录
     */
    suspend fun hasWarranty(itemId: Long): Boolean {
        return warrantyDao.hasWarranty(itemId) > 0
    }
    
    /**
     * 创建新的保修记录，自动计算到期日期
     * @param itemId 物品ID
     * @param purchaseDate 购买日期
     * @param warrantyPeriodMonths 保修期（月数）
     * @param receiptImageUris 凭证图片URI列表（可选）
     * @param notes 备注（可选）
     * @param warrantyProvider 保修服务商（可选）
     * @param contactInfo 联系方式（可选）
     */
    suspend fun createWarranty(
        itemId: Long,
        purchaseDate: Date,
        warrantyPeriodMonths: Int,
        receiptImageUris: List<String>? = null,
        notes: String? = null,
        warrantyProvider: String? = null,
        contactInfo: String? = null
    ): Long {
        // 计算保修到期日期
        val calendar = Calendar.getInstance().apply {
            time = purchaseDate
            add(Calendar.MONTH, warrantyPeriodMonths)
        }
        val warrantyEndDate = calendar.time
        
        // 将图片URI列表转换为JSON字符串
        val receiptUrisJson = receiptImageUris?.let { uris ->
            if (uris.isNotEmpty()) {
                com.google.gson.Gson().toJson(uris)
            } else null
        }
        
        val warranty = WarrantyEntity(
            itemId = itemId,
            purchaseDate = purchaseDate,
            warrantyPeriodMonths = warrantyPeriodMonths,
            warrantyEndDate = warrantyEndDate,
            receiptImageUris = receiptUrisJson,
            notes = notes,
            status = if (warrantyEndDate.before(Date())) WarrantyStatus.EXPIRED else WarrantyStatus.ACTIVE,
            warrantyProvider = warrantyProvider,
            contactInfo = contactInfo
        )
        
        return insertWarranty(warranty)
    }
    
    /**
     * 解析保修凭证图片URI列表
     */
    fun parseReceiptImageUris(receiptImageUris: String?): List<String> {
        if (receiptImageUris.isNullOrEmpty()) return emptyList()
        
        return try {
            com.google.gson.Gson().fromJson(
                receiptImageUris, 
                Array<String>::class.java
            ).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 批量插入保修记录
     */
    suspend fun insertWarranties(warranties: List<WarrantyEntity>): List<Long> {
        return warrantyDao.insertAll(warranties)
    }
    
    /**
     * 获取保修概览数据
     * @return Triple<总数, 有效数, 即将到期数>
     */
    suspend fun getWarrantyOverview(): Triple<Int, Int, Int> {
        val totalCount = warrantyDao.getAllWarrantiesOnce().size
        val activeCount = getActiveWarrantyCount()
        val nearExpirationCount = getWarrantiesExpiringInDays(30) // 30天内到期
        
        return Triple(totalCount, activeCount, nearExpirationCount)
    }
}
