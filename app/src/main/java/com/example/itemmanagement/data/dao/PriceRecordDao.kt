package com.example.itemmanagement.data.dao

import androidx.room.*
import com.example.itemmanagement.data.entity.PriceRecord
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 价格记录 DAO
 */
@Dao
interface PriceRecordDao {
    
    @Insert
    suspend fun insert(record: PriceRecord): Long
    
    @Update
    suspend fun update(record: PriceRecord)
    
    @Delete
    suspend fun delete(record: PriceRecord)
    
    @Query("DELETE FROM price_records WHERE id = :recordId")
    suspend fun deleteById(recordId: Long)
    
    /**
     * 获取某物品的所有价格记录（按日期倒序）
     */
    @Query("SELECT * FROM price_records WHERE itemId = :itemId ORDER BY recordDate DESC")
    fun getPriceRecords(itemId: Long): Flow<List<PriceRecord>>
    
    /**
     * 获取某物品的所有价格记录（suspend，直接返回列表）
     */
    @Query("SELECT * FROM price_records WHERE itemId = :itemId ORDER BY recordDate DESC")
    suspend fun getPriceRecordsListByItemId(itemId: Long): List<PriceRecord>
    
    /**
     * 获取某时间段内的价格记录
     */
    @Query("SELECT * FROM price_records WHERE itemId = :itemId AND recordDate BETWEEN :startDate AND :endDate ORDER BY recordDate DESC")
    fun getPriceRecordsInRange(itemId: Long, startDate: Date, endDate: Date): Flow<List<PriceRecord>>
    
    /**
     * 获取最高价格
     */
    @Query("SELECT MAX(price) FROM price_records WHERE itemId = :itemId")
    suspend fun getMaxPrice(itemId: Long): Double?
    
    /**
     * 获取最低价格
     */
    @Query("SELECT MIN(price) FROM price_records WHERE itemId = :itemId")
    suspend fun getMinPrice(itemId: Long): Double?
    
    /**
     * 获取平均价格
     */
    @Query("SELECT AVG(price) FROM price_records WHERE itemId = :itemId")
    suspend fun getAvgPrice(itemId: Long): Double?
    
    /**
     * 获取记录总数
     */
    @Query("SELECT COUNT(*) FROM price_records WHERE itemId = :itemId")
    suspend fun getRecordCount(itemId: Long): Int
    
    /**
     * 获取各渠道的最新价格
     */
    @Query("""
        SELECT * FROM price_records 
        WHERE itemId = :itemId 
        AND id IN (
            SELECT MAX(id) FROM price_records 
            WHERE itemId = :itemId 
            GROUP BY purchaseChannel
        )
        ORDER BY price ASC
    """)
    suspend fun getLatestPricesByChannel(itemId: Long): List<PriceRecord>
}


