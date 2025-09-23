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
    
    // 获取所有标签
    @Query("SELECT DISTINCT name FROM tags WHERE name IS NOT NULL AND name != ''")
    suspend fun getAllTags(): List<String>
    
    // 获取所有季节
    @Query("SELECT DISTINCT season FROM items WHERE season IS NOT NULL AND season != ''")
    suspend fun getAllSeasonsRaw(): List<String>
    
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

    // ==================== 库存分析查询 ====================
    
    // 核心指标查询
    @Query("SELECT COUNT(*) as totalItems FROM items")
    suspend fun getTotalItemsCount(): Int
    
    @Query("SELECT COALESCE(SUM(price), 0.0) as totalValue FROM items WHERE price IS NOT NULL")
    suspend fun getTotalValue(): Double
    
    @Query("SELECT COUNT(DISTINCT category) as categoriesCount FROM items WHERE category IS NOT NULL AND category != ''")
    suspend fun getCategoriesCount(): Int
    
    @Query("SELECT COUNT(DISTINCT locationId) as locationsCount FROM items WHERE locationId IS NOT NULL")
    suspend fun getLocationsCount(): Int
    
    @Query("SELECT COUNT(DISTINCT t.name) as tagsCount FROM tags t INNER JOIN item_tag_cross_ref itcr ON t.id = itcr.tagId")
    suspend fun getTagsCount(): Int
    
    // 分类分析查询
    @Query("""
        SELECT COALESCE(category, '未分类') as category, 
               COUNT(*) as count, 
               COALESCE(SUM(price), 0.0) as totalValue 
        FROM items 
        GROUP BY COALESCE(category, '未分类')
        HAVING count > 0
        ORDER BY count DESC
    """)
    suspend fun getCategoryAnalysis(): List<CategoryAnalysisResult>
    
    // 位置分析查询
    @Query("""
        SELECT COALESCE(l.area, '未指定位置') as location, 
               COUNT(i.id) as count,
               COALESCE(SUM(i.price), 0.0) as totalValue
        FROM items i
        LEFT JOIN locations l ON i.locationId = l.id
        GROUP BY COALESCE(l.area, '未指定位置')
        HAVING count > 0
        ORDER BY count DESC
    """)
    suspend fun getLocationAnalysis(): List<LocationAnalysisResult>
    
    // 标签分析查询
    @Query("""
        SELECT COALESCE(t.name, '无标签') as tag, 
               COUNT(DISTINCT i.id) as count,
               COALESCE(SUM(i.price), 0.0) as totalValue
        FROM items i
        LEFT JOIN item_tag_cross_ref itcr ON i.id = itcr.itemId
        LEFT JOIN tags t ON itcr.tagId = t.id
        GROUP BY COALESCE(t.name, '无标签')
        HAVING count > 0
        ORDER BY count DESC
    """)
    suspend fun getTagAnalysis(): List<TagAnalysisResult>
    
    // 月度趋势查询
    @Query("""
        SELECT strftime('%Y-%m', addDate/1000, 'unixepoch') as month, 
               COUNT(*) as count, 
               COALESCE(SUM(price), 0.0) as totalValue
        FROM items 
        WHERE addDate IS NOT NULL AND addDate > 0
        GROUP BY strftime('%Y-%m', addDate/1000, 'unixepoch')
        HAVING month IS NOT NULL
        ORDER BY month DESC
        LIMIT 12
    """)
    suspend fun getMonthlyTrends(): List<MonthlyTrendResult>

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

    // =================== 浪费报告相关查询 ===================
    
    /**
     * 获取指定时间范围内的浪费物品（过期或丢弃）
     */
    @Query("""
        SELECT i.id, i.name, i.category, i.quantity, i.unit, i.status, 
               i.wasteDate, i.totalPrice,
               l.area, l.container, l.sublocation,
               (SELECT p.uri FROM photos p WHERE p.itemId = i.id 
                ORDER BY p.isMain DESC, p.displayOrder ASC LIMIT 1) as photoUri
        FROM items i
        LEFT JOIN locations l ON i.locationId = l.id
        WHERE (i.status = 'EXPIRED' OR i.status = 'DISCARDED')
        AND i.wasteDate IS NOT NULL
        AND i.wasteDate BETWEEN :startDate AND :endDate
        ORDER BY i.totalPrice DESC, i.wasteDate DESC
    """)
    suspend fun getWastedItemsInPeriod(startDate: Long, endDate: Long): List<WastedItemInfo>

    /**
     * 获取指定时间范围内按类别统计的浪费数据
     */
    @Query("""
        SELECT 
            category,
            COUNT(*) as itemCount,
            COALESCE(SUM(totalPrice), 0) as totalValue
        FROM items 
        WHERE (status = 'EXPIRED' OR status = 'DISCARDED')
        AND wasteDate IS NOT NULL
        AND wasteDate BETWEEN :startDate AND :endDate
        GROUP BY category
        ORDER BY totalValue DESC, itemCount DESC
    """)
    suspend fun getWasteByCategoryInPeriod(startDate: Long, endDate: Long): List<WasteCategoryInfo>

    /**
     * 获取指定时间范围内按日期统计的浪费数据
     */
    @Query("""
        SELECT 
            DATE(wasteDate/1000, 'unixepoch') as date,
            COUNT(*) as itemCount,
            COALESCE(SUM(totalPrice), 0) as totalValue
        FROM items 
        WHERE (status = 'EXPIRED' OR status = 'DISCARDED')
        AND wasteDate IS NOT NULL
        AND wasteDate BETWEEN :startDate AND :endDate
        GROUP BY DATE(wasteDate/1000, 'unixepoch')
        ORDER BY date ASC
    """)
    suspend fun getWasteByDateInPeriod(startDate: Long, endDate: Long): List<WasteDateInfo>

    /**
     * 获取浪费报告统计概要
     */
    @Query("""
        SELECT 
            COUNT(*) as totalItems,
            COALESCE(SUM(totalPrice), 0) as totalValue,
            SUM(CASE WHEN status = 'EXPIRED' THEN 1 ELSE 0 END) as expiredItems,
            SUM(CASE WHEN status = 'DISCARDED' THEN 1 ELSE 0 END) as discardedItems
        FROM items 
        WHERE (status = 'EXPIRED' OR status = 'DISCARDED')
        AND wasteDate IS NOT NULL
        AND wasteDate BETWEEN :startDate AND :endDate
    """)
    suspend fun getWasteSummaryInPeriod(startDate: Long, endDate: Long): WasteSummaryInfo

    /**
     * 检查并更新过期物品状态
     * @param currentTime 当前时间戳
     * @return 更新的物品数量
     */
    @Query("""
        UPDATE items 
        SET status = 'EXPIRED', wasteDate = :currentTime
        WHERE status = 'IN_STOCK' 
        AND expirationDate IS NOT NULL 
        AND expirationDate < :currentTime
    """)
    suspend fun updateExpiredItems(currentTime: Long)

    /**
     * 获取所有浪费状态但没有wasteDate的物品（用于调试）
     */
    @Query("""
        SELECT i.id, i.name, i.category, i.quantity, i.unit, i.status, 
               COALESCE(i.wasteDate, i.addDate) as wasteDate, i.totalPrice,
               l.area, l.container, l.sublocation,
               (SELECT p.uri FROM photos p WHERE p.itemId = i.id 
                ORDER BY p.isMain DESC, p.displayOrder ASC LIMIT 1) as photoUri
        FROM items i
        LEFT JOIN locations l ON i.locationId = l.id
        WHERE (i.status = 'EXPIRED' OR i.status = 'DISCARDED')
        AND i.wasteDate IS NULL
        ORDER BY i.addDate DESC
    """)
    suspend fun getWastedItemsWithoutWasteDate(): List<WastedItemInfo>

    /**
     * 自动修复没有wasteDate的浪费物品（这里使用addDate作为fallback）
     */
    @Query("""
        UPDATE items 
        SET wasteDate = CASE 
            WHEN status = 'EXPIRED' AND expirationDate IS NOT NULL THEN expirationDate
            ELSE :fallbackTime
        END
        WHERE (status = 'EXPIRED' OR status = 'DISCARDED')
        AND wasteDate IS NULL
    """)
    suspend fun fixWastedItemsWithoutWasteDate(fallbackTime: Long): Int
}

// 分析查询结果数据类
data class CategoryAnalysisResult(
    val category: String,
    val count: Int,
    val totalValue: Double
)

data class LocationAnalysisResult(
    val location: String,
    val count: Int,
    val totalValue: Double
)

data class TagAnalysisResult(
    val tag: String,
    val count: Int,
    val totalValue: Double
)

data class MonthlyTrendResult(
    val month: String,
    val count: Int,
    val totalValue: Double
)

// =================== 浪费报告查询结果数据类 ===================

data class WastedItemInfo(
    val id: Long,
    val name: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val status: String,
    val wasteDate: Long,
    val totalPrice: Double,
    val area: String?,
    val container: String?,
    val sublocation: String?,
    val photoUri: String?
)

data class WasteCategoryInfo(
    val category: String,
    val itemCount: Int,
    val totalValue: Double
)

data class WasteDateInfo(
    val date: String,
    val itemCount: Int,
    val totalValue: Double
)

/**
 * 浪费报告相关的数据查询结果类
 */
data class WasteSummaryInfo(
    val totalItems: Int,
    val totalValue: Double,
    val expiredItems: Int,
    val discardedItems: Int
) 