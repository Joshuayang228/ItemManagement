package com.example.itemmanagement.data.dao

import androidx.room.*
import com.example.itemmanagement.data.entity.*
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
        location?.let { updateLocation(it) }

        // 2. 更新物品
        updateItem(item)

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
} 