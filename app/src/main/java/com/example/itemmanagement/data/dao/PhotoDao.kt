package com.example.itemmanagement.data.dao

import androidx.room.*
import com.example.itemmanagement.data.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    // === 基本操作 ===
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: PhotoEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<PhotoEntity>): List<Long>
    
    @Update
    suspend fun update(photo: PhotoEntity)
    
    @Delete
    suspend fun delete(photo: PhotoEntity)
    
    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    // === 查询操作 ===
    
    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun getById(id: Long): PhotoEntity?
    
    @Query("SELECT * FROM photos ORDER BY isMain DESC, displayOrder ASC")
    fun getAllPhotos(): Flow<List<PhotoEntity>>
    
    @Query("SELECT * FROM photos ORDER BY isMain DESC, displayOrder ASC")
    suspend fun getAllPhotosSync(): List<PhotoEntity>
    
    // === 物品照片查询 ===
    
    /**
     * 获取指定物品的所有照片
     */
    @Query("""
        SELECT * FROM photos 
        WHERE itemId = :itemId 
        ORDER BY isMain DESC, displayOrder ASC
    """)
    suspend fun getPhotosByItemId(itemId: Long): List<PhotoEntity>
    
    /**
     * 获取指定物品的所有照片（Flow版本）
     */
    @Query("""
        SELECT * FROM photos 
        WHERE itemId = :itemId 
        ORDER BY isMain DESC, displayOrder ASC
    """)
    fun getPhotosByItemIdFlow(itemId: Long): Flow<List<PhotoEntity>>
    
    /**
     * 获取指定物品的主照片
     */
    @Query("""
        SELECT * FROM photos 
        WHERE itemId = :itemId AND isMain = 1
        ORDER BY displayOrder ASC
        LIMIT 1
    """)
    suspend fun getPrimaryPhotoByItemId(itemId: Long): PhotoEntity?
    
    /**
     * 获取指定物品的第一张照片（如果没有主照片，则返回第一张）
     */
    @Query("""
        SELECT * FROM photos 
        WHERE itemId = :itemId 
        ORDER BY isMain DESC, displayOrder ASC
        LIMIT 1
    """)
    suspend fun getFirstPhotoByItemId(itemId: Long): PhotoEntity?
    
    /**
     * 获取指定物品的非主照片
     */
    @Query("""
        SELECT * FROM photos 
        WHERE itemId = :itemId AND isMain = 0
        ORDER BY displayOrder ASC
    """)
    suspend fun getSecondaryPhotosByItemId(itemId: Long): List<PhotoEntity>
    
    // === 主照片管理 ===
    
    /**
     * 设置主照片（先取消其他主照片，再设置指定照片为主照片）
     */
    @Transaction
    suspend fun setPrimaryPhoto(itemId: Long, photoId: Long) {
        // 取消该物品的所有主照片标记
        clearPrimaryPhotos(itemId)
        
        // 设置指定照片为主照片
        setPhotoAsPrimary(photoId)
    }
    
    @Query("UPDATE photos SET isMain = 0 WHERE itemId = :itemId")
    suspend fun clearPrimaryPhotos(itemId: Long)
    
    @Query("UPDATE photos SET isMain = 1 WHERE id = :photoId")
    suspend fun setPhotoAsPrimary(photoId: Long)
    
    // === 排序管理 ===
    
    /**
     * 更新照片显示顺序
     */
    @Query("UPDATE photos SET displayOrder = :displayOrder WHERE id = :photoId")
    suspend fun updateDisplayOrder(photoId: Long, displayOrder: Int)
    
    /**
     * 批量更新照片顺序
     */
    @Transaction
    suspend fun updatePhotosOrder(photoOrders: List<Pair<Long, Int>>) {
        photoOrders.forEach { (photoId, order) ->
            updateDisplayOrder(photoId, order)
        }
    }
    
    // === 批量操作 ===
    
    /**
     * 删除指定物品的所有照片
     */
    @Query("DELETE FROM photos WHERE itemId = :itemId")
    suspend fun deletePhotosByItemId(itemId: Long)
    
    /**
     * 获取指定物品的照片数量
     */
    @Query("SELECT COUNT(*) FROM photos WHERE itemId = :itemId")
    suspend fun getPhotoCountByItemId(itemId: Long): Int
    
    /**
     * 检查物品是否有照片
     */
    @Query("SELECT COUNT(*) > 0 FROM photos WHERE itemId = :itemId")
    suspend fun hasPhotos(itemId: Long): Boolean
    
    /**
     * 检查物品是否有主照片
     */
    @Query("SELECT COUNT(*) > 0 FROM photos WHERE itemId = :itemId AND isMain = 1")
    suspend fun hasPrimaryPhoto(itemId: Long): Boolean
    
    // === 统计查询 ===
    
    @Query("SELECT COUNT(*) FROM photos")
    suspend fun getTotalPhotoCount(): Int
    
    @Query("SELECT COUNT(DISTINCT itemId) FROM photos")
    suspend fun getItemsWithPhotosCount(): Int
    
    /**
     * 获取没有照片的物品ID列表
     */
    @Query("""
        SELECT ui.id FROM unified_items ui
        LEFT JOIN photos p ON ui.id = p.itemId
        WHERE p.itemId IS NULL
    """)
    suspend fun getItemIdsWithoutPhotos(): List<Long>
    
    /**
     * 获取有多张照片的物品统计
     */
    @Query("""
        SELECT itemId, COUNT(*) as photo_count
        FROM photos
        GROUP BY itemId
        HAVING photo_count > 1
        ORDER BY photo_count DESC
    """)
    suspend fun getItemsWithMultiplePhotos(): List<ItemPhotoCount>
    
    // === 清理操作 ===
    
    /**
     * 清理无效的照片记录（物品已删除）
     */
    @Query("""
        DELETE FROM photos 
        WHERE itemId NOT IN (SELECT id FROM unified_items)
    """)
    suspend fun cleanOrphanPhotos(): Int
    
    /**
     * 按URI查找照片
     */
    @Query("SELECT * FROM photos WHERE uri = :uri")
    suspend fun getPhotoByUri(uri: String): PhotoEntity?
    
    /**
     * 检查URI是否已存在
     */
    @Query("SELECT COUNT(*) > 0 FROM photos WHERE uri = :uri")
    suspend fun isUriExists(uri: String): Boolean
    
    // === 事务操作 ===
    
    /**
     * 为物品设置照片（替换现有照片）
     */
    @Transaction
    suspend fun setItemPhotos(itemId: Long, photos: List<PhotoEntity>) {
        // 删除现有照片
        deletePhotosByItemId(itemId)
        
        // 添加新照片
        val photosWithItemId = photos.mapIndexed { index, photo ->
            photo.copy(
                itemId = itemId,
                displayOrder = index,
                isMain = index == 0 // 第一张照片设为主照片
            )
        }
        insertAll(photosWithItemId)
    }
    
    /**
     * 为物品添加照片（保留现有照片）
     */
    @Transaction
    suspend fun addItemPhotos(itemId: Long, photos: List<PhotoEntity>) {
        val currentCount = getPhotoCountByItemId(itemId)
        val photosWithItemId = photos.mapIndexed { index, photo ->
            photo.copy(
                itemId = itemId,
                displayOrder = currentCount + index,
                isMain = currentCount == 0 && index == 0 // 如果是第一张照片且物品之前没有照片，设为主照片
            )
        }
        insertAll(photosWithItemId)
    }
}

/**
 * 物品照片数量统计数据类
 */
data class ItemPhotoCount(
    val itemId: Long,
    val photo_count: Int
)
