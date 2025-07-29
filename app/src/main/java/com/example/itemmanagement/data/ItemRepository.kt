package com.example.itemmanagement.data

import androidx.sqlite.db.SimpleSQLiteQuery
import com.example.itemmanagement.data.dao.ItemDao
import com.example.itemmanagement.data.entity.*
import com.example.itemmanagement.data.mapper.toItem
import com.example.itemmanagement.data.model.*
import com.example.itemmanagement.data.query.ItemQueryBuilder
import com.example.itemmanagement.data.relation.ItemWithDetails
import com.example.itemmanagement.ui.warehouse.FilterState
import com.example.itemmanagement.ui.warehouse.SortDirection
import com.example.itemmanagement.ui.warehouse.SortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ItemRepository(private val itemDao: ItemDao, private val database: AppDatabase) {
    
    /**
     * 获取所有物品列表（包含详细信息）
     * @return Flow<List<Item>> 物品列表的数据流
     */
    fun getAllItems(): Flow<List<Item>> {
        return itemDao.getAllItemsWithDetails().map { items ->
            items.map { it.toItem() }
        }
    }

    /**
     * 根据ID获取物品详情
     * @param id 物品ID
     * @return ItemWithDetails? 物品详情，如果不存在则返回null
     */
    suspend fun getItemWithDetailsById(id: Long): ItemWithDetails? {
        return itemDao.getItemWithDetailsById(id)
    }

    /**
     * 插入新物品（包含所有相关数据）
     * @param item 物品实体
     * @param location 位置实体
     * @param photos 照片列表
     * @param tags 标签列表
     * @return Long 插入的物品ID
     */
    suspend fun insertItemWithDetails(
        item: ItemEntity,
        location: LocationEntity?,
        photos: List<PhotoEntity>,
        tags: List<TagEntity>
    ): Long {
        return itemDao.insertItemWithDetails(item, location, photos, tags)
    }

    /**
     * 更新物品（包含所有相关数据）
     * @param itemId 物品ID
     * @param item 物品实体
     * @param location 位置实体
     * @param photos 照片列表
     * @param tags 标签列表
     */
    suspend fun updateItemWithDetails(
        itemId: Long,
        item: ItemEntity,
        location: LocationEntity?,
        photos: List<PhotoEntity>,
        tags: List<TagEntity>
    ) {
        itemDao.updateItemWithDetails(itemId, item, location, photos, tags)
    }

    /**
     * 删除物品
     * @param item 要删除的物品实体
     */
    suspend fun deleteItem(item: ItemEntity) {
        itemDao.deleteItem(item)
    }

    /**
     * 搜索位置
     * @param query 搜索关键词
     * @return List<LocationEntity> 匹配的位置列表
     */
    suspend fun searchLocations(query: String): List<LocationEntity> {
        return itemDao.searchLocations("%$query%")
    }

    /**
     * 搜索标签
     * @param query 搜索关键词
     * @return List<TagEntity> 匹配的标签列表
     */
    suspend fun searchTags(query: String): List<TagEntity> {
        return itemDao.searchTags("%$query%")
    }

    /**
     * 根据ID获取物品
     * @param id 物品ID
     * @return Item? 物品对象，如果不存在则返回null
     */
    suspend fun getItemById(id: Long): Item? {
        val itemWithDetails = itemDao.getItemWithDetailsById(id)
        return itemWithDetails?.toItem()
    }
    
    /**
     * 获取所有分类
     * @return List<String> 分类列表
     */
    suspend fun getAllCategories(): List<String> {
        return itemDao.getAllCategories()
    }
    
    /**
     * 获取所有子分类
     * @return List<String> 子分类列表
     */
    suspend fun getAllSubCategories(): List<String> {
        return itemDao.getAllSubCategories()
    }
    
    /**
     * 获取所有品牌
     * @return List<String> 品牌列表
     */
    suspend fun getAllBrands(): List<String> {
        return itemDao.getAllBrands()
    }
    
    /**
     * 获取所有标签
     * @return List<String> 标签列表
     */
    suspend fun getAllTags(): List<String> {
        return itemDao.getAllTags()
    }
    
    suspend fun getAllSeasons(): List<String> {
        val rawSeasons = itemDao.getAllSeasonsRaw()
        val seasonSet = mutableSetOf<String>()
        
        // 将合并的季节文本拆分成独立的季节
        rawSeasons.forEach { seasonText ->
            if (seasonText.isNotBlank()) {
                // 按常见分隔符拆分季节
                val separators = listOf(",", "，", "、", ";", "；", "/", "|")
                var seasons = listOf(seasonText)
                
                separators.forEach { separator ->
                    seasons = seasons.flatMap { it.split(separator) }
                }
                
                seasons.forEach { season ->
                    val trimmedSeason = season.trim()
                    if (trimmedSeason.isNotBlank()) {
                        seasonSet.add(trimmedSeason)
                    }
                }
            }
        }
        
        // 按照自然季节顺序排序
        val seasonOrder = listOf("春", "夏", "秋", "冬", "初春", "仲春", "暮春", "初夏", "仲夏", "晚夏", "初秋", "仲秋", "晚秋", "初冬", "仲冬", "晚冬")
        
        return seasonSet.toList().sortedWith { a, b ->
            val indexA = seasonOrder.indexOf(a)
            val indexB = seasonOrder.indexOf(b)
            
            when {
                indexA != -1 && indexB != -1 -> indexA.compareTo(indexB)
                indexA != -1 && indexB == -1 -> -1
                indexA == -1 && indexB != -1 -> 1
                else -> a.compareTo(b)
            }
        }
    }
    
    /**
     * 获取所有位置区域
     * @return List<String> 区域列表
     */
    suspend fun getAllLocationAreas(): List<String> {
        return itemDao.getAllLocationAreas()
    }
    
    /**
     * 获取指定区域的所有容器
     * @param area 区域名称
     * @return List<String> 容器列表
     */
    suspend fun getContainersByArea(area: String): List<String> {
        return itemDao.getContainersByArea(area)
    }
    
    /**
     * 获取指定区域和容器的所有子位置
     * @param area 区域名称
     * @param container 容器名称
     * @return List<String> 子位置列表
     */
    suspend fun getSublocations(area: String, container: String): List<String> {
        return itemDao.getSublocations(area, container)
    }

    /**
     * 使用新的查询构建器获取仓库物品列表
     * @param filterState 筛选条件
     * @return Flow<List<WarehouseItem>> 仓库物品列表的数据流
     */
    fun getWarehouseItems(filterState: FilterState): Flow<List<WarehouseItem>> {
        val queryBuilder = ItemQueryBuilder()
            .withSearchTerm(filterState.searchTerm)
            .withCategory(filterState.category)
            .withSubCategory(filterState.subCategory)
            .withBrand(filterState.brand)
            .withLocationArea(filterState.locationArea)
            .withLocationContainer(filterState.container)
            .withLocationSublocation(filterState.sublocation)
            .withQuantityRange(filterState.minQuantity, filterState.maxQuantity)
            .withPriceRange(filterState.minPrice, filterState.maxPrice)
            .apply {
                // 优先使用多选评分，如果没有则使用评分范围
                if (filterState.ratings.isNotEmpty()) {
                    withRatings(filterState.ratings)
                } else {
                    withRatingRange(filterState.minRating, filterState.maxRating)
                }
            }
            .apply {
                // 优先使用多选开封状态，如果没有则使用单选开封状态
                if (filterState.openStatuses.isNotEmpty()) {
                    withOpenStatuses(filterState.openStatuses)
                } else {
                    withOpenStatus(filterState.openStatus)
                }
            }
            .withSeasons(filterState.seasons)
            .withTags(filterState.tags)
            // 使用新的分离日期范围方法
            .withExpirationDateRange(filterState.expirationStartDate, filterState.expirationEndDate)
            .withCreationDateRange(filterState.creationStartDate, filterState.creationEndDate)
            .withPurchaseDateRange(filterState.purchaseStartDate, filterState.purchaseEndDate)
            .withProductionDateRange(filterState.productionStartDate, filterState.productionEndDate)
            .sortBy(filterState.sortOption, filterState.sortDirection)
        
        val query = queryBuilder.build()
        
        return itemDao.getWarehouseItems(query)
    }
    
    // ==================== 库存分析方法 ====================
    
    /**
     * 获取库存分析数据
     */
    suspend fun getInventoryAnalysisData(): InventoryAnalysisData {
        // 并行获取所有数据以提高性能
        val coreMetrics = CoreMetrics(
            totalItems = itemDao.getTotalItemsCount(),
            totalValue = itemDao.getTotalValue(),
            categoriesCount = itemDao.getCategoriesCount(),
            locationsCount = itemDao.getLocationsCount(),
            tagsCount = itemDao.getTagsCount()
        )
        
        val categoryAnalysis = itemDao.getCategoryAnalysis().map {
            CategoryValue(
                category = it.category,
                count = it.count,
                totalValue = it.totalValue
            )
        }
        
        val locationAnalysis = itemDao.getLocationAnalysis().map {
            LocationValue(
                location = it.location,
                count = it.count,
                totalValue = it.totalValue
            )
        }
        
        val tagAnalysis = itemDao.getTagAnalysis().map {
            TagValue(
                tag = it.tag,
                count = it.count,
                totalValue = it.totalValue
            )
        }
        
        val monthlyTrends = itemDao.getMonthlyTrends().map {
            MonthlyTrend(
                month = it.month,
                count = it.count,
                totalValue = it.totalValue
            )
        }
        
        return InventoryAnalysisData(
            coreMetrics = coreMetrics,
            categoryAnalysis = categoryAnalysis,
            locationAnalysis = locationAnalysis,
            tagAnalysis = tagAnalysis,
            monthlyTrends = monthlyTrends
        )
    }
    
    // ==================== 日历事件方法 ====================
    
    suspend fun getAllCalendarEvents() = database.calendarEventDao().getAllEvents()
    
    suspend fun getCalendarEventsBetweenDates(startDate: java.util.Date, endDate: java.util.Date) = 
        database.calendarEventDao().getEventsBetweenDates(startDate, endDate)
    
    suspend fun insertCalendarEvent(event: CalendarEventEntity): Long =
        database.calendarEventDao().insertEvent(event)
    
    suspend fun insertCalendarEvents(events: List<CalendarEventEntity>): List<Long> =
        database.calendarEventDao().insertEvents(events)
    
    suspend fun markCalendarEventCompleted(eventId: Long) =
        database.calendarEventDao().markEventCompleted(eventId)
    
    suspend fun deleteCalendarEvent(eventId: Long) =
        database.calendarEventDao().deleteEventById(eventId)
    
    // ==================== 购物清单功能 (简化版本) ====================
    
    /**
     * 获取所有购物清单项目
     */
    fun getAllShoppingItems(): Flow<List<ShoppingItemEntity>> {
        return database.shoppingDao().getAllShoppingItems()
    }
    
    /**
     * 获取指定清单的待购买物品
     */
    fun getPendingShoppingItemsByListId(listId: Long): Flow<List<ShoppingItemEntity>> {
        return database.shoppingDao().getPendingShoppingItemsByListId(listId)
    }
    
    /**
     * 获取指定清单的已购买物品
     */
    fun getPurchasedShoppingItemsByListId(listId: Long): Flow<List<ShoppingItemEntity>> {
        return database.shoppingDao().getPurchasedShoppingItemsByListId(listId)
    }
    
    /**
     * 添加新的购物项目
     */
    suspend fun insertShoppingItemSimple(item: ShoppingItemEntity): Long {
        return database.shoppingDao().insertShoppingItem(item)
    }
    
    /**
     * 更新购物项目（主要用于标记购买状态）
     */
    suspend fun updateShoppingItem(item: ShoppingItemEntity) {
        database.shoppingDao().updateShoppingItem(item)
    }
    
    /**
     * 删除购物项目
     */
    suspend fun deleteShoppingItem(item: ShoppingItemEntity) {
        database.shoppingDao().deleteShoppingItem(item)
    }
    
    /**
     * 通过ID删除购物项目
     */
    suspend fun deleteShoppingItemById(id: Long) {
        database.shoppingDao().deleteShoppingItemById(id)
    }
    
    /**
     * 清除指定清单的已购买项目
     */
    suspend fun clearPurchasedShoppingItemsByListId(listId: Long) {
        database.shoppingDao().clearPurchasedItemsByListId(listId)
    }
    
    /**
     * 获取指定清单的待购买项目数量
     */
    fun getPendingShoppingItemsCountByListId(listId: Long): Flow<Int> {
        return database.shoppingDao().getPendingItemsCountByListId(listId)
    }
    
    /**
     * 从库存物品创建购物项目
     */
    suspend fun createShoppingItemFromInventory(itemId: Long, listId: Long): Long? {
        val item = getItemWithDetailsById(itemId)?.toItem()
        return if (item != null) {
            val shoppingItem = ShoppingItemEntity(
                listId = listId,
                name = item.name,
                quantity = 1.0, // 默认数量为1
                category = item.category,
                brand = item.brand,
                sourceItemId = item.id,
                customNote = "从库存物品 '${item.name}' 添加"
            )
            insertShoppingItemSimple(shoppingItem)
        } else {
            null
        }
    }
    
    // =================== 浪费报告相关方法 ===================

    /**
     * 获取指定时间范围内的浪费物品
     */
    suspend fun getWastedItemsInPeriod(startDate: Long, endDate: Long) = 
        itemDao.getWastedItemsInPeriod(startDate, endDate)

    /**
     * 获取指定时间范围内按类别统计的浪费数据
     */
    suspend fun getWasteByCategoryInPeriod(startDate: Long, endDate: Long) = 
        itemDao.getWasteByCategoryInPeriod(startDate, endDate)

    /**
     * 获取指定时间范围内按日期统计的浪费数据
     */
    suspend fun getWasteByDateInPeriod(startDate: Long, endDate: Long) = 
        itemDao.getWasteByDateInPeriod(startDate, endDate)

    /**
     * 获取浪费报告统计概要
     */
    suspend fun getWasteSummaryInPeriod(startDate: Long, endDate: Long) = 
        itemDao.getWasteSummaryInPeriod(startDate, endDate)

    /**
     * 检查并更新过期物品
     * @param currentTime 当前时间戳
     */
    suspend fun checkAndUpdateExpiredItems(currentTime: Long) {
        itemDao.updateExpiredItems(currentTime)
    }

    // ====================== 购物清单相关方法 ======================
    
    /**
     * 获取所有购物清单
     */
    fun getAllShoppingLists(): Flow<List<ShoppingListEntity>> {
        return database.shoppingListDao().getAllShoppingLists()
    }
    
    /**
     * 获取活跃的购物清单
     */
    fun getActiveShoppingLists(): Flow<List<ShoppingListEntity>> {
        return database.shoppingListDao().getActiveShoppingLists()
    }
    
    /**
     * 根据ID获取购物清单
     */
    suspend fun getShoppingListById(id: Long): ShoppingListEntity? {
        return database.shoppingListDao().getShoppingListById(id)
    }
    
    /**
     * 插入购物清单
     */
    suspend fun insertShoppingList(shoppingList: ShoppingListEntity): Long {
        return database.shoppingListDao().insertShoppingList(shoppingList)
    }
    
    /**
     * 更新购物清单
     */
    suspend fun updateShoppingList(shoppingList: ShoppingListEntity) {
        database.shoppingListDao().updateShoppingList(shoppingList)
    }
    
    /**
     * 删除购物清单
     */
    suspend fun deleteShoppingList(shoppingList: ShoppingListEntity) {
        database.shoppingListDao().deleteShoppingList(shoppingList)
    }
    
    /**
     * 获取指定清单的物品列表
     */
    fun getShoppingItemsByListId(listId: Long): Flow<List<ShoppingItemEntity>> {
        return database.shoppingDao().getShoppingItemsByListId(listId)
    }
    
    companion object {
        @Volatile
        private var INSTANCE: ItemRepository? = null
        
        fun getInstance(application: android.app.Application): ItemRepository {
            return INSTANCE ?: synchronized(this) {
                val database = AppDatabase.getDatabase(application)
                val instance = ItemRepository(database.itemDao(), database)
                INSTANCE = instance
                instance
            }
        }
    }
} 