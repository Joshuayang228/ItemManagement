package com.example.itemmanagement.data.dao.unified

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.itemmanagement.data.entity.unified.UnifiedItemEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 统一物品数据访问接口
 * 管理核心物品表的CRUD操作
 */
@Dao
interface UnifiedItemDao {
    
    // === 基础CRUD操作 ===
    
    @Query("SELECT * FROM unified_items ORDER BY createdDate DESC")
    fun getAllItems(): Flow<List<UnifiedItemEntity>>
    
    @Query("SELECT * FROM unified_items WHERE id = :id")
    suspend fun getById(id: Long): UnifiedItemEntity?
    
    @Query("SELECT * FROM unified_items WHERE name = :name")
    suspend fun getByName(name: String): List<UnifiedItemEntity>
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: UnifiedItemEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<UnifiedItemEntity>): List<Long>
    
    @Update
    suspend fun update(item: UnifiedItemEntity)
    
    @Delete
    suspend fun delete(item: UnifiedItemEntity)
    
    @Query("DELETE FROM unified_items WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    // === 查询操作 ===
    
    @Query("SELECT * FROM unified_items WHERE category = :category ORDER BY name ASC")
    fun getByCategory(category: String): Flow<List<UnifiedItemEntity>>
    
    @Query("SELECT * FROM unified_items WHERE category = :category AND subCategory = :subCategory ORDER BY name ASC")
    fun getByCategoryAndSubCategory(category: String, subCategory: String): Flow<List<UnifiedItemEntity>>
    
    @Query("SELECT * FROM unified_items WHERE brand = :brand ORDER BY name ASC")
    fun getByBrand(brand: String): Flow<List<UnifiedItemEntity>>
    
    @Query("SELECT * FROM unified_items WHERE name LIKE :searchTerm OR brand LIKE :searchTerm OR specification LIKE :searchTerm ORDER BY name ASC")
    fun searchItems(searchTerm: String): Flow<List<UnifiedItemEntity>>
    
    @Query("SELECT DISTINCT category FROM unified_items ORDER BY category ASC")
    suspend fun getAllCategories(): List<String>
    
    @Query("SELECT DISTINCT subCategory FROM unified_items WHERE category = :category AND subCategory IS NOT NULL ORDER BY subCategory ASC")
    suspend fun getSubCategories(category: String): List<String>
    
    @Query("SELECT DISTINCT brand FROM unified_items WHERE brand IS NOT NULL ORDER BY brand ASC")
    suspend fun getAllBrands(): List<String>
    
    // === 统计查询 ===
    
    @Query("SELECT COUNT(*) FROM unified_items")
    suspend fun getItemCount(): Int
    
    @Query("SELECT COUNT(*) FROM unified_items WHERE category = :category")
    suspend fun getItemCountByCategory(category: String): Int
    
    @Query("SELECT COUNT(*) FROM unified_items WHERE createdDate >= :startDate")
    suspend fun getItemCountSince(startDate: Date): Int
    
    // === 批量操作 ===
    
    @Query("UPDATE unified_items SET updatedDate = :updateDate WHERE id IN (:ids)")
    suspend fun updateModificationDate(ids: List<Long>, updateDate: Date = Date())
    
    @Query("UPDATE unified_items SET category = :newCategory WHERE category = :oldCategory")
    suspend fun updateCategory(oldCategory: String, newCategory: String): Int
    
    @Query("UPDATE unified_items SET brand = :newBrand WHERE brand = :oldBrand")
    suspend fun updateBrand(oldBrand: String, newBrand: String): Int
    
    // === 高级查询 ===
    
    /**
     * 获取最近创建的物品
     */
    @Query("SELECT * FROM unified_items ORDER BY createdDate DESC LIMIT :limit")
    suspend fun getRecentItems(limit: Int = 10): List<UnifiedItemEntity>
    
    /**
     * 获取最近更新的物品
     */
    @Query("SELECT * FROM unified_items ORDER BY updatedDate DESC LIMIT :limit")
    suspend fun getRecentlyUpdatedItems(limit: Int = 10): List<UnifiedItemEntity>
    
    /**
     * 模糊搜索物品（支持中文）
     */
    @Query("""
        SELECT * FROM unified_items 
        WHERE name LIKE '%' || :keyword || '%'
        OR brand LIKE '%' || :keyword || '%'
        OR specification LIKE '%' || :keyword || '%'
        OR customNote LIKE '%' || :keyword || '%'
        OR category LIKE '%' || :keyword || '%'
        OR subCategory LIKE '%' || :keyword || '%'
        ORDER BY 
            CASE 
                WHEN name = :keyword THEN 1
                WHEN name LIKE :keyword || '%' THEN 2
                WHEN brand = :keyword THEN 3
                WHEN brand LIKE :keyword || '%' THEN 4
                ELSE 5
            END,
            name ASC
    """)
    fun searchByKeyword(keyword: String): Flow<List<UnifiedItemEntity>>
    
    /**
     * 检查是否存在相似物品（防重复）
     */
    @Query("""
        SELECT * FROM unified_items 
        WHERE name = :name 
        AND category = :category 
        AND (brand = :brand OR (:brand IS NULL AND brand IS NULL))
        AND (specification = :specification OR (:specification IS NULL AND specification IS NULL))
    """)
    suspend fun findSimilarItems(
        name: String, 
        category: String, 
        brand: String?, 
        specification: String?
    ): List<UnifiedItemEntity>
    
    /**
     * 获取指定ID列表的物品
     */
    @Query("SELECT * FROM unified_items WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<UnifiedItemEntity>
    
    /**
     * 批量删除
     */
    @Query("DELETE FROM unified_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>): Int
}
