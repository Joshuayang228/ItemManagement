package com.example.itemmanagement.data

import com.example.itemmanagement.data.dao.ItemDao
import com.example.itemmanagement.data.entity.*
import com.example.itemmanagement.data.mapper.toItem
import com.example.itemmanagement.data.model.Item
import com.example.itemmanagement.data.relation.ItemWithDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
} 