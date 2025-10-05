package com.example.itemmanagement.data.dao

import androidx.room.*
import com.example.itemmanagement.data.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    // === 基本操作 ===
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: LocationEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(locations: List<LocationEntity>): List<Long>
    
    @Update
    suspend fun update(location: LocationEntity)
    
    @Delete
    suspend fun delete(location: LocationEntity)
    
    @Query("DELETE FROM locations WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    // === 查询操作 ===
    
    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun getById(id: Long): LocationEntity?
    
    @Query("SELECT * FROM locations ORDER BY area ASC, container ASC, sublocation ASC")
    fun getAllLocations(): Flow<List<LocationEntity>>
    
    @Query("SELECT * FROM locations ORDER BY area ASC, container ASC, sublocation ASC")
    suspend fun getAllLocationsSync(): List<LocationEntity>
    
    // === 位置层级查询 ===
    
    /**
     * 获取所有位置区域（去重）
     */
    @Query("SELECT DISTINCT area FROM locations WHERE area IS NOT NULL AND area != '' ORDER BY area ASC")
    suspend fun getAllAreas(): List<String>
    
    /**
     * 根据区域获取所有容器（去重）
     */
    @Query("SELECT DISTINCT container FROM locations WHERE area = :area AND container IS NOT NULL AND container != '' ORDER BY container ASC")
    suspend fun getContainersByArea(area: String): List<String>
    
    /**
     * 根据区域和容器获取所有子位置（去重）
     */
    @Query("SELECT DISTINCT sublocation FROM locations WHERE area = :area AND container = :container AND sublocation IS NOT NULL AND sublocation != '' ORDER BY sublocation ASC")
    suspend fun getSublocations(area: String, container: String): List<String>
    
    // === 搜索查询 ===
    
    /**
     * 搜索位置（模糊查询）
     */
    @Query("""
        SELECT * FROM locations 
        WHERE area LIKE '%' || :query || '%' 
           OR container LIKE '%' || :query || '%' 
           OR sublocation LIKE '%' || :query || '%'
        ORDER BY area ASC, container ASC, sublocation ASC
    """)
    suspend fun searchLocations(query: String): List<LocationEntity>
    
    /**
     * 根据完整路径查找位置（精确匹配）
     */
    @Query("""
        SELECT * FROM locations 
        WHERE area = :area 
          AND (:container IS NULL AND container IS NULL OR container = :container)
          AND (:sublocation IS NULL AND sublocation IS NULL OR sublocation = :sublocation)
        LIMIT 1
    """)
    suspend fun findExactLocation(area: String, container: String?, sublocation: String?): LocationEntity?
    
    // === 统计查询 ===
    
    @Query("SELECT COUNT(*) FROM locations")
    suspend fun getLocationCount(): Int
    
    /**
     * 获取使用中的位置（有物品关联的位置）
     */
    @Query("""
        SELECT DISTINCT l.* FROM locations l
        INNER JOIN inventory_details i ON l.id = i.locationId
        ORDER BY l.area ASC, l.container ASC, l.sublocation ASC
    """)
    suspend fun getUsedLocations(): List<LocationEntity>
    
    /**
     * 获取空闲位置（没有物品关联的位置）
     */
    @Query("""
        SELECT l.* FROM locations l
        LEFT JOIN inventory_details i ON l.id = i.locationId
        WHERE i.locationId IS NULL
        ORDER BY l.area ASC, l.container ASC, l.sublocation ASC
    """)
    suspend fun getUnusedLocations(): List<LocationEntity>
    
    // === 批量操作 ===
    
    /**
     * 查找或创建位置
     * 如果位置已存在则返回ID，否则创建新位置并返回ID
     */
    @Transaction
    suspend fun findOrCreateLocation(area: String, container: String?, sublocation: String?): Long {
        // 先尝试查找现有位置
        val existing = findExactLocation(area, container, sublocation)
        if (existing != null) {
            return existing.id
        }
        
        // 创建新位置
        val newLocation = LocationEntity(
            area = area,
            container = container,
            sublocation = sublocation
        )
        return insert(newLocation)
    }
    
    /**
     * 清理未使用的位置
     */
    @Query("""
        DELETE FROM locations 
        WHERE id NOT IN (SELECT DISTINCT locationId FROM inventory_details WHERE locationId IS NOT NULL)
    """)
    suspend fun cleanUnusedLocations(): Int
    
    /**
     * 根据区域、容器、子位置查找位置
     */
    @Query("""
        SELECT * FROM locations 
        WHERE area = :area 
        AND (:container IS NULL AND container IS NULL OR container = :container)
        AND (:sublocation IS NULL AND sublocation IS NULL OR sublocation = :sublocation)
        LIMIT 1
    """)
    suspend fun findByAreaContainerSublocation(
        area: String,
        container: String?,
        sublocation: String?
    ): LocationEntity?
}
