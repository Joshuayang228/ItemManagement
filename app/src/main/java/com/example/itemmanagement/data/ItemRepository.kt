package com.example.itemmanagement.data

import androidx.sqlite.db.SimpleSQLiteQuery
import com.example.itemmanagement.data.dao.ItemDao
import com.example.itemmanagement.data.entity.*
import com.example.itemmanagement.data.mapper.toItem
import com.example.itemmanagement.data.model.Item
import com.example.itemmanagement.data.model.WarehouseItem
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

class ItemRepository(private val itemDao: ItemDao) {
    
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
            .withRatingRange(filterState.minRating, filterState.maxRating)
            .withOpenStatus(filterState.openStatus)
            .sortBy(filterState.sortOption, filterState.sortDirection)
        
        val query = queryBuilder.build()
        
        return itemDao.getWarehouseItems(query)
    }
} 