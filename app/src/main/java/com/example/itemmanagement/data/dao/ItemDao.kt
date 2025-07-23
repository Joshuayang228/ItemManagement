package com.example.itemmanagement.data.dao

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.itemmanagement.data.entity.*
import com.example.itemmanagement.data.model.WarehouseItem
import com.example.itemmanagement.data.relation.ItemWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    // Item 基本操作
    @Insert
    suspend fun insertItem(item: ItemEntity): Long

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Delete
    suspend fun deleteItem(item: ItemEntity)

    // Location 操作
    @Insert
    suspend fun insertLocation(location: LocationEntity): Long

    @Update
    suspend fun updateLocation(location: LocationEntity)

    // Photo 操作
    @Insert
    suspend fun insertPhoto(photo: PhotoEntity): Long

    @Insert
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Delete
    suspend fun deletePhoto(photo: PhotoEntity)

    // Tag 操作
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTags(tags: List<TagEntity>): List<Long>

    // ItemTagCrossRef 操作
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItemTagCrossRef(crossRef: ItemTagCrossRef)

    @Delete
    suspend fun deleteItemTagCrossRef(crossRef: ItemTagCrossRef)

    // 复合查询
    @Transaction
    @Query("SELECT * FROM items")
    fun getAllItemsWithDetails(): Flow<List<ItemWithDetails>>

    @Transaction
    @Query("SELECT * FROM items WHERE id = :itemId")
    suspend fun getItemWithDetailsById(itemId: Long): ItemWithDetails?

    // 动态查询 - 支持筛选和排序
    @Transaction
    @RawQuery
    fun getFilteredAndSortedItems(query: SupportSQLiteQuery): Flow<List<ItemWithDetails>>
    
    // 获取所有分类
    @Query("SELECT DISTINCT category FROM items WHERE category IS NOT NULL AND category != ''")
    suspend fun getAllCategories(): List<String>
    
    // 获取所有子分类
    @Query("SELECT DISTINCT subCategory FROM items WHERE subCategory IS NOT NULL AND subCategory != ''")
    suspend fun getAllSubCategories(): List<String>
    
    // 获取所有品牌
    @Query("SELECT DISTINCT brand FROM items WHERE brand IS NOT NULL AND brand != ''")
    suspend fun getAllBrands(): List<String>
    
    // 获取所有位置区域
    @Query("SELECT DISTINCT area FROM locations WHERE area IS NOT NULL AND area != ''")
    suspend fun getAllLocationAreas(): List<String>
    
    // 获取指定区域的所有容器
    @Query("SELECT DISTINCT container FROM locations WHERE area = :area AND container IS NOT NULL AND container != ''")
    suspend fun getContainersByArea(area: String): List<String>
    
    // 获取指定区域和容器的所有子位置
    @Query("SELECT DISTINCT sublocation FROM locations WHERE area = :area AND container = :container AND sublocation IS NOT NULL AND sublocation != ''")
    suspend fun getSublocations(area: String, container: String): List<String>

    // 辅助查询
    @Query("SELECT * FROM locations WHERE area LIKE :query OR container LIKE :query OR sublocation LIKE :query")
    suspend fun searchLocations(query: String): List<LocationEntity>

    @Query("SELECT * FROM tags WHERE name LIKE :query")
    suspend fun searchTags(query: String): List<TagEntity>

    // 事务操作
    @Transaction
    suspend fun insertItemWithDetails(
        item: ItemEntity,
        location: LocationEntity?,
        photos: List<PhotoEntity>,
        tags: List<TagEntity>
    ): Long {
        // 1. 插入位置（如果有）
        val locationId = location?.let { insertLocation(it) }

        // 2. 插入物品（设置位置ID）
        val itemId = insertItem(item.copy(locationId = locationId))

        // 3. 插入照片
        insertPhotos(photos.map { it.copy(itemId = itemId) })

        // 4. 插入标签和关联关系
        val tagIds = insertTags(tags)
        tagIds.forEach { tagId ->
            insertItemTagCrossRef(ItemTagCrossRef(itemId, tagId))
        }

        return itemId
    }

    @Transaction
    suspend fun updateItemWithDetails(
        itemId: Long,
        item: ItemEntity,
        location: LocationEntity?,
        photos: List<PhotoEntity>,
        tags: List<TagEntity>
    ) {
        // 1. 更新位置
        var locationId: Long? = null
        if (location != null) {
            // 检查位置是否已存在
            if (location.id > 0) {
                // 如果位置已存在，更新它
                updateLocation(location)
                locationId = location.id
            } else {
                // 如果位置不存在，插入新位置
                locationId = insertLocation(location)
            }
        }
        
        // 2. 更新物品（确保locationId被正确设置）
        updateItem(item.copy(locationId = locationId))

        // 3. 更新照片（先删除旧的，再插入新的）
        val oldPhotos = getItemWithDetailsById(itemId)?.photos ?: emptyList()
        oldPhotos.forEach { deletePhoto(it) }
        // 确保设置正确的itemId
        insertPhotos(photos.map { it.copy(itemId = itemId) })

        // 4. 更新标签（先删除旧的关联，再创建新的）
        val oldTags = getItemWithDetailsById(itemId)?.tags ?: emptyList()
        oldTags.forEach { tag ->
            deleteItemTagCrossRef(ItemTagCrossRef(itemId, tag.id))
        }
        val tagIds = insertTags(tags)
        tagIds.forEach { tagId ->
            insertItemTagCrossRef(ItemTagCrossRef(itemId, tagId))
        }
    }

    // 为仓库列表页添加新的查询方法
    @RawQuery(observedEntities = [ItemEntity::class, LocationEntity::class, PhotoEntity::class])
    fun getWarehouseItems(query: SupportSQLiteQuery): Flow<List<WarehouseItem>>
} 