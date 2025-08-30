package com.example.itemmanagement.data.dao

import androidx.room.*
import com.example.itemmanagement.data.entity.CategoryThresholdEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface CategoryThresholdDao {
    
    /**
     * 获取所有分类阈值设置（Flow版本）
     */
    @Query("SELECT * FROM category_thresholds ORDER BY category ASC")
    fun getAllThresholdsFlow(): Flow<List<CategoryThresholdEntity>>
    
    /**
     * 获取所有分类阈值设置（一次性获取）
     */
    @Query("SELECT * FROM category_thresholds ORDER BY category ASC")
    suspend fun getAllThresholds(): List<CategoryThresholdEntity>
    
    /**
     * 获取所有启用的分类阈值
     */
    @Query("SELECT * FROM category_thresholds WHERE enabled = 1 ORDER BY category ASC")
    suspend fun getEnabledThresholds(): List<CategoryThresholdEntity>
    
    /**
     * 根据分类名称获取阈值设置
     */
    @Query("SELECT * FROM category_thresholds WHERE category = :category")
    suspend fun getThresholdByCategory(category: String): CategoryThresholdEntity?
    
    /**
     * 插入或更新分类阈值
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateThreshold(threshold: CategoryThresholdEntity)
    
    /**
     * 批量插入或更新分类阈值
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateThresholds(thresholds: List<CategoryThresholdEntity>)
    
    /**
     * 更新分类的最小数量
     */
    @Query("UPDATE category_thresholds SET minQuantity = :minQuantity, updatedAt = :updatedAt WHERE category = :category")
    suspend fun updateMinQuantity(category: String, minQuantity: Double, updatedAt: Date = Date())
    
    /**
     * 启用/禁用分类提醒
     */
    @Query("UPDATE category_thresholds SET enabled = :enabled, updatedAt = :updatedAt WHERE category = :category")
    suspend fun updateEnabled(category: String, enabled: Boolean, updatedAt: Date = Date())
    
    /**
     * 删除分类阈值设置
     */
    @Query("DELETE FROM category_thresholds WHERE category = :category")
    suspend fun deleteByCategory(category: String)
    
    /**
     * 获取所有不重复的分类名称
     */
    @Query("SELECT DISTINCT category FROM category_thresholds ORDER BY category ASC")
    suspend fun getAllCategories(): List<String>
    
    /**
     * 初始化默认分类阈值
     */
    suspend fun initializeDefaultThresholds() {
        val defaultThresholds = listOf(
            CategoryThresholdEntity("食品", 2.0, true, "个", "食品类物品"),
            CategoryThresholdEntity("药品", 5.0, true, "个", "药品保健品"),
            CategoryThresholdEntity("日用品", 1.0, true, "个", "日常生活用品"),
            CategoryThresholdEntity("清洁用品", 2.0, true, "个", "清洁护理用品"),
            CategoryThresholdEntity("电子产品", 1.0, false, "个", "电子设备")
        )
        insertOrUpdateThresholds(defaultThresholds)
    }
}
