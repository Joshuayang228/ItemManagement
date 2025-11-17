package com.example.itemmanagement.data.repository

import com.example.itemmanagement.data.AppDatabase
import com.example.itemmanagement.data.dao.LocationDao
import com.example.itemmanagement.data.dao.TagDao
import com.example.itemmanagement.data.dao.PhotoDao
import com.example.itemmanagement.data.dao.PriceRecordDao
import com.example.itemmanagement.data.dao.BorrowDao
import com.example.itemmanagement.data.dao.unified.InventoryDetailDao
import com.example.itemmanagement.data.dao.unified.ItemStateDao
import com.example.itemmanagement.data.dao.unified.ShoppingDetailDao
import com.example.itemmanagement.data.dao.ShoppingListDao
import com.example.itemmanagement.data.dao.unified.UnifiedItemDao
import com.example.itemmanagement.data.entity.LocationEntity
import com.example.itemmanagement.data.entity.TagEntity
import com.example.itemmanagement.data.entity.PhotoEntity
import com.example.itemmanagement.data.entity.ItemTagCrossRef
import com.example.itemmanagement.data.entity.PriceRecord
import com.example.itemmanagement.data.entity.ShoppingItemPriority
import com.example.itemmanagement.data.entity.UrgencyLevel
import com.example.itemmanagement.data.entity.unified.InventoryDetailEntity
import com.example.itemmanagement.data.entity.unified.ItemStateEntity
import com.example.itemmanagement.data.entity.unified.ItemStateType
import com.example.itemmanagement.data.entity.unified.ShoppingDetailEntity
import com.example.itemmanagement.data.entity.unified.UnifiedItemEntity
import com.example.itemmanagement.data.model.ItemStatus
import com.example.itemmanagement.data.entity.BorrowStatus
import com.example.itemmanagement.data.view.InventoryItemView
import com.example.itemmanagement.data.view.ShoppingItemView
import com.example.itemmanagement.data.model.Item
import com.example.itemmanagement.data.model.WarehouseItem
import com.example.itemmanagement.data.relation.ItemWithDetails
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import java.util.Date

/**
 * 统一物品管理Repository
 * 处理所有物品状态的数据操作
 */
class UnifiedItemRepository(
    private val appDatabase: AppDatabase,
    private val unifiedItemDao: UnifiedItemDao,
    private val itemStateDao: ItemStateDao,
    private val shoppingDetailDao: ShoppingDetailDao,
    private val shoppingListDao: ShoppingListDao,
    private val inventoryDetailDao: InventoryDetailDao,
    // === 新增的DAO ===
    private val locationDao: LocationDao,
    private val tagDao: TagDao,
    private val photoDao: PhotoDao,
    private val priceRecordDao: PriceRecordDao,
    private val warrantyDao: com.example.itemmanagement.data.dao.WarrantyDao,
    private val borrowDao: BorrowDao  // ✅ 添加BorrowDao
) {

    // --- 通用物品操作 ---
    suspend fun insertUnifiedItem(item: UnifiedItemEntity): Long {
        return unifiedItemDao.insert(item)
    }

    suspend fun updateUnifiedItem(item: UnifiedItemEntity) {
        unifiedItemDao.update(item)
    }

    suspend fun getUnifiedItemById(itemId: Long): UnifiedItemEntity? {
        return unifiedItemDao.getById(itemId)
    }
    
    /**
     * 根据物品ID获取库存详情
     */
    suspend fun getInventoryDetailByItemId(itemId: Long): InventoryDetailEntity? {
        return inventoryDetailDao.getByItemId(itemId)
    }
    
    /**
     * 根据物品ID获取照片列表
     */
    suspend fun getPhotosByItemId(itemId: Long): List<PhotoEntity> {
        return try {
            photoDao.getPhotosByItemId(itemId)
        } catch (e: Exception) {
            android.util.Log.e("UnifiedItemRepository", "查询照片失败: itemId=$itemId", e)
            emptyList()
        }
    }
    
    /**
     * 根据物品ID获取标签列表
     */
    suspend fun getTagsByItemId(itemId: Long): List<TagEntity> {
        return try {
            tagDao.getTagsByItemId(itemId)
        } catch (e: Exception) {
            android.util.Log.e("UnifiedItemRepository", "查询标签失败: itemId=$itemId", e)
            emptyList()
        }
    }
    
    /**
     * 根据物品ID获取保修信息
     */
    suspend fun getWarrantyByItemId(itemId: Long): com.example.itemmanagement.data.entity.WarrantyEntity? {
        return try {
            warrantyDao.getByItemId(itemId)
        } catch (e: Exception) {
            android.util.Log.e("UnifiedItemRepository", "查询保修信息失败: itemId=$itemId", e)
            null
        }
    }
    
    /**
     * 根据位置ID获取位置信息
     */
    suspend fun getLocationById(locationId: Long): LocationEntity? {
        return try {
            locationDao.getById(locationId)
        } catch (e: Exception) {
            android.util.Log.e("UnifiedItemRepository", "查询位置失败: locationId=$locationId", e)
            null
        }
    }

    // --- 购物清单操作 ---
    suspend fun addShoppingItem(
        unifiedItem: UnifiedItemEntity,
        shoppingDetail: ShoppingDetailEntity,
        photoUris: List<android.net.Uri> = emptyList(),
        tags: Map<String, Set<String>> = emptyMap()
    ) {
        appDatabase.withTransaction {
            // 1. 插入基础物品信息
            val itemId = unifiedItemDao.insert(unifiedItem)
            
            // 2. 插入购物详情
            shoppingDetailDao.insert(shoppingDetail.copy(itemId = itemId))
            
            // 3. 插入物品状态
            itemStateDao.insert(ItemStateEntity(
                itemId = itemId,
                stateType = ItemStateType.SHOPPING,
                isActive = true,
                contextId = shoppingDetail.shoppingListId
            ))
            
            // 4. 保存照片
            photoUris.forEach { uri ->
                val photo = com.example.itemmanagement.data.entity.PhotoEntity(
                    itemId = itemId,
                    uri = uri.toString()
                )
                photoDao.insert(photo)
            }
            
            // 5. 保存标签
            tags.forEach { (category, tagNames) ->
                tagNames.forEach { tagName ->
                    // 查找或创建标签
                    val tagId = tagDao.findOrCreateTag(tagName, null)
                    // 创建关联
                    tagDao.insertItemTagCrossRef(
                        com.example.itemmanagement.data.entity.ItemTagCrossRef(
                            itemId = itemId,
                            tagId = tagId
                        )
                    )
                }
            }
        }
    }

    // --- 库存操作 ---
    suspend fun addInventoryItem(unifiedItem: UnifiedItemEntity, inventoryDetail: InventoryDetailEntity, tags: List<TagEntity> = emptyList(), photos: List<PhotoEntity> = emptyList()): Long {
        return appDatabase.withTransaction {
            android.util.Log.d("UnifiedItemRepository", "📦 开始保存库存物品: name='${unifiedItem.name}'")
            
            // 1. 保存基础物品信息
            val itemId = unifiedItemDao.insert(unifiedItem)
            android.util.Log.d("UnifiedItemRepository", "📦 物品基础信息保存成功: itemId=$itemId")
            
            // 2. 保存库存详情
            inventoryDetailDao.insert(inventoryDetail.copy(itemId = itemId))
            android.util.Log.d("UnifiedItemRepository", "📦 库存详情保存成功: locationId=${inventoryDetail.locationId}, rating=${unifiedItem.rating}")
            
            // 3. 保存物品状态
            itemStateDao.insert(ItemStateEntity(itemId = itemId, stateType = ItemStateType.INVENTORY, isActive = true))
            android.util.Log.d("UnifiedItemRepository", "📦 物品状态保存成功")
            
            // 4. 保存标签信息
            if (tags.isNotEmpty()) {
                android.util.Log.d("UnifiedItemRepository", "🏷️ 开始保存${tags.size}个标签")
                tags.forEach { tag ->
                    // 查找或创建标签
                    val existingTag = tagDao.getByName(tag.name)
                    val tagId = if (existingTag != null) {
                        android.util.Log.d("UnifiedItemRepository", "🏷️ 使用现有标签: ${tag.name} (ID=${existingTag.id})")
                        existingTag.id
                    } else {
                        android.util.Log.d("UnifiedItemRepository", "🏷️ 创建新标签: ${tag.name}")
                        tagDao.insert(tag)
                    }
                    
                    // 创建物品-标签关联
                    tagDao.insertItemTagCrossRef(ItemTagCrossRef(itemId, tagId))
                    android.util.Log.d("UnifiedItemRepository", "🏷️ 标签关联创建成功: itemId=$itemId, tagId=$tagId")
                }
            } else {
                android.util.Log.d("UnifiedItemRepository", "🏷️ 无标签需要保存")
            }
            
            // 5. 保存照片信息
            if (photos.isNotEmpty()) {
                android.util.Log.d("UnifiedItemRepository", "📸 开始保存${photos.size}张照片")
                photos.forEachIndexed { index, photo: PhotoEntity ->
                    val photoWithItemId = photo.copy(
                        itemId = itemId,
                        displayOrder = index,
                        isMain = index == 0 // 第一张照片设为主照片
                    )
                    photoDao.insert(photoWithItemId)
                    android.util.Log.d("UnifiedItemRepository", "📸 照片保存成功: uri='${photo.uri}', isMain=${photoWithItemId.isMain}")
                }
            } else {
                android.util.Log.d("UnifiedItemRepository", "📸 无照片需要保存")
            }
            
            android.util.Log.d("UnifiedItemRepository", "✅ 库存物品保存完成: itemId=$itemId")
            itemId  // ✅ 返回itemId
        }
    }

    /**
     * 查找或创建位置实体
     */
    suspend fun findOrCreateLocation(locationEntity: LocationEntity): Long {
        return appDatabase.withTransaction {
            android.util.Log.d("UnifiedItemRepository", "📍 开始查找或创建位置: area='${locationEntity.area}', container='${locationEntity.container}', sublocation='${locationEntity.sublocation}'")
            
            // 尝试查找现有位置
            val existingLocation = locationDao.findByAreaContainerSublocation(
                locationEntity.area,
                locationEntity.container,
                locationEntity.sublocation
            )
            
            if (existingLocation != null) {
                android.util.Log.d("UnifiedItemRepository", "📍 找到现有位置: ID=${existingLocation.id}")
                existingLocation.id
            } else {
                android.util.Log.d("UnifiedItemRepository", "📍 创建新位置")
                val newLocationId = locationDao.insert(locationEntity)
                android.util.Log.d("UnifiedItemRepository", "📍 新位置创建成功: ID=$newLocationId")
                newLocationId
            }
        }
    }

    // --- 状态流转服务 ---
    suspend fun moveToShoppingList(itemId: Long, shoppingListId: Long) {
        appDatabase.withTransaction {
            // 1. 激活购物状态
            itemStateDao.insert(ItemStateEntity(
                itemId = itemId,
                stateType = ItemStateType.SHOPPING,
                isActive = true,
                contextId = shoppingListId
            ))
        }
    }

    suspend fun purchaseAndMoveToInventory(itemId: Long) {
        appDatabase.withTransaction {
            // 1. 激活库存状态
            itemStateDao.insert(ItemStateEntity(
                itemId = itemId,
                stateType = ItemStateType.INVENTORY,
                isActive = true
            ))

            // 2. 从购物详情复制到库存详情
            val shoppingDetail = shoppingDetailDao.getByItemId(itemId)
            if (shoppingDetail != null) {
                inventoryDetailDao.insert(InventoryDetailEntity(
                    itemId = itemId,
                    quantity = shoppingDetail.quantity,
                    unit = shoppingDetail.quantityUnit,
                    price = shoppingDetail.actualPrice,
                    purchaseDate = shoppingDetail.purchaseDate,
                    status = ItemStatus.IN_STOCK
                ))

                // 3. 更新购物状态为已完成
                shoppingDetailDao.updatePurchaseStatus(itemId, true, Date(), Date())
            }
        }
    }

    suspend fun softDeleteItem(itemId: Long, reason: String) {
        appDatabase.withTransaction {
            // 获取物品信息用于日历事件
            val unifiedItem = unifiedItemDao.getById(itemId)
            
            // 停用所有现有状态
            itemStateDao.deactivateAllStates(itemId)

            // 添加删除状态
            itemStateDao.insert(ItemStateEntity(
                itemId = itemId,
                stateType = ItemStateType.DELETED,
                isActive = true,
                metadata = reason, // 将原因存储在metadata中
                activatedDate = Date()
            ))
            
            // 🗑️ 添加日历事件：记录删除物品操作
            if (unifiedItem != null) {
                try {
                    val event = com.example.itemmanagement.data.entity.CalendarEventEntity(
                        itemId = itemId,
                        eventType = com.example.itemmanagement.data.model.EventType.ITEM_DELETED,
                        title = "删除物品：${unifiedItem.name}",
                        description = "原因：$reason",
                        eventDate = Date(),
                        reminderDays = emptyList(),
                        priority = com.example.itemmanagement.data.model.Priority.LOW,
                        isCompleted = true,
                        recurrenceType = null
                    )
                    appDatabase.calendarEventDao().insertEvent(event)
                    android.util.Log.d("UnifiedItemRepository", "📅 已添加日历事件：删除物品 - ${unifiedItem.name}")
                } catch (e: Exception) {
                    android.util.Log.e("UnifiedItemRepository", "添加删除物品日历事件失败", e)
                }
            }
        }
    }

    // ==================== 兼容性方法（为旧代码提供支持） ====================

    /**
     * 获取所有库存物品（兼容方法）
     * 只返回状态为INVENTORY且isActive=true的物品
     */
    fun getAllItems(): Flow<List<Item>> {
        return inventoryDetailDao.getAllDetails().map { inventoryItems ->
            // 获取所有激活的库存状态
            val activeInventoryStates = runBlocking {
                itemStateDao.getActiveStatesByType(ItemStateType.INVENTORY).first()
            }
            val activeItemIds = activeInventoryStates.map { it.itemId }.toSet()
            
            inventoryItems.mapNotNull { inventoryDetail ->
                // 只处理激活的库存物品
                if (!activeItemIds.contains(inventoryDetail.itemId)) {
                    return@mapNotNull null
                }
                
                unifiedItemDao.getById(inventoryDetail.itemId)?.let { unifiedItem ->
                    // 查询照片信息
                    val photos = runBlocking {
                        photoDao.getPhotosByItemId(unifiedItem.id).map { photoEntity ->
                            com.example.itemmanagement.data.model.Photo(
                                id = photoEntity.id,
                                uri = photoEntity.uri,
                                isMain = photoEntity.isMain
                            )
                        }
                    }
                    
                    // 查询位置信息 (为算法提供完整数据)
                    val location = runBlocking {
                        inventoryDetail.locationId?.let { locationId ->
                            locationDao.getById(locationId)?.let { locationEntity ->
                                com.example.itemmanagement.data.model.Location(
                                    id = locationEntity.id,
                                    area = locationEntity.area,
                                    container = locationEntity.container,
                                    sublocation = locationEntity.sublocation
                                )
                            }
                        }
                    }
                    
                    // 查询标签信息 (为算法提供完整数据)
                    val tags = runBlocking {
                        tagDao.getTagsByItemId(unifiedItem.id).map { tagEntity ->
                            com.example.itemmanagement.data.model.Tag(
                                id = tagEntity.id,
                                name = tagEntity.name,
                                color = tagEntity.color
                            )
                        }
                    }
                    
                    Item(
                        id = unifiedItem.id,
                        name = unifiedItem.name,
                        quantity = inventoryDetail.quantity,
                        unit = inventoryDetail.unit,
                        location = location, // ✅ 现在查询真实的位置数据 (算法使用，UI不显示)
                        category = unifiedItem.category,
                        addDate = unifiedItem.createdDate,
                        productionDate = inventoryDetail.productionDate,
                        expirationDate = inventoryDetail.expirationDate,
                        openStatus = inventoryDetail.openStatus,
                        openDate = inventoryDetail.openDate,
                        brand = unifiedItem.brand,
                        specification = unifiedItem.specification,
                        status = inventoryDetail.status,
                        stockWarningThreshold = inventoryDetail.stockWarningThreshold,
                        price = inventoryDetail.price,
                        priceUnit = inventoryDetail.priceUnit,
                        purchaseChannel = inventoryDetail.purchaseChannel,
                        storeName = inventoryDetail.storeName,
                        subCategory = unifiedItem.subCategory,
                        customNote = unifiedItem.customNote,
                        season = unifiedItem.season, // 从UnifiedItemEntity读取
                        capacity = unifiedItem.capacity, // 从UnifiedItemEntity读取
                        capacityUnit = unifiedItem.capacityUnit, // 从UnifiedItemEntity读取
                        rating = unifiedItem.rating, // 从UnifiedItemEntity读取
                        totalPrice = inventoryDetail.totalPrice,
                        totalPriceUnit = inventoryDetail.totalPriceUnit,
                        purchaseDate = inventoryDetail.purchaseDate,
                        shelfLife = inventoryDetail.shelfLife,
                        // warrantyPeriod 和 warrantyEndDate 已移至 WarrantyEntity
                        warrantyPeriod = null,
                        warrantyEndDate = null,
                        serialNumber = unifiedItem.serialNumber, // 从UnifiedItemEntity读取
                        locationAddress = unifiedItem.locationAddress, // GPS地址
                        locationLatitude = unifiedItem.locationLatitude, // GPS纬度
                        locationLongitude = unifiedItem.locationLongitude, // GPS经度
                        isHighTurnover = inventoryDetail.isHighTurnover,
                        photos = photos, // ✅ 照片数据 (UI显示)
                        tags = tags // ✅ 现在查询真实的标签数据 (算法使用，UI不显示)
                    )
                }
            }
        }
    }

    /**
     * 根据ID获取物品（兼容方法）
     * 支持查询心愿单、购物清单、库存三种状态的物品
     */
    suspend fun getItemById(itemId: Long): Item? {
        android.util.Log.d("UnifiedItemRepository", "========== getItemById 开始 ==========")
        android.util.Log.d("UnifiedItemRepository", "查询物品ID: $itemId")
        
        val unifiedItem = unifiedItemDao.getById(itemId) ?: run {
            android.util.Log.w("UnifiedItemRepository", "未找到UnifiedItem")
            return null
        }
        
        // 查询物品状态以确定查询哪个detail表
        val itemStates = itemStateDao.getByItemId(itemId)
        android.util.Log.d("UnifiedItemRepository", "物品状态: ${itemStates.map { "${it.stateType}(active=${it.isActive})" }}")
        
        val inventoryDetail = inventoryDetailDao.getByItemId(itemId)
        val shoppingDetail = shoppingDetailDao.getByItemId(itemId)
        
        android.util.Log.d("UnifiedItemRepository", "InventoryDetail存在: ${inventoryDetail != null}")
        android.util.Log.d("UnifiedItemRepository", "ShoppingDetail存在: ${shoppingDetail != null}")
        
        // 查询照片
        val photos = photoDao.getPhotosByItemId(itemId).map { photoEntity ->
            com.example.itemmanagement.data.model.Photo(
                id = photoEntity.id,
                uri = photoEntity.uri,
                isMain = photoEntity.isMain
            )
        }
        android.util.Log.d("UnifiedItemRepository", "照片数量: ${photos.size}")
        
        // 查询标签
        val tags = tagDao.getTagsByItemId(itemId).map { tagEntity ->
            com.example.itemmanagement.data.model.Tag(
                id = tagEntity.id,
                name = tagEntity.name,
                color = tagEntity.color
            )
        }
        android.util.Log.d("UnifiedItemRepository", "标签数量: ${tags.size}")
        
        // ✅ 检查借出状态
        android.util.Log.d("UnifiedItemRepository", "━━━━━ 开始检查借出状态 ━━━━━")
        android.util.Log.d("UnifiedItemRepository", "正在查询itemId=$itemId 的借出记录...")
        val borrowRecords = borrowDao.getAll()
        android.util.Log.d("UnifiedItemRepository", "数据库中总共有 ${borrowRecords.size} 条借出记录")
        
        // 打印所有借出记录
        borrowRecords.forEachIndexed { index, record ->
            android.util.Log.d("UnifiedItemRepository", "  借出记录[$index]: itemId=${record.itemId}, borrower=${record.borrowerName}, status=${record.status}")
        }
        
        // 查找当前物品的未归还记录
        val currentBorrow = borrowRecords.find { 
            it.itemId == itemId && 
            (it.status == BorrowStatus.BORROWED || it.status == BorrowStatus.OVERDUE)
        }
        
        val itemStatus = if (currentBorrow != null) {
            android.util.Log.d("UnifiedItemRepository", "✅ 找到未归还记录！")
            android.util.Log.d("UnifiedItemRepository", "  - 借给: ${currentBorrow.borrowerName}")
            android.util.Log.d("UnifiedItemRepository", "  - 状态: ${currentBorrow.status}")
            android.util.Log.d("UnifiedItemRepository", "  - 预计归还: ${java.util.Date(currentBorrow.expectedReturnDate)}")
            android.util.Log.d("UnifiedItemRepository", "  → 设置物品状态为: BORROWED")
            ItemStatus.BORROWED
        } else {
            android.util.Log.d("UnifiedItemRepository", "❌ 未找到未归还的借出记录")
            val fallbackStatus = inventoryDetail?.status ?: ItemStatus.IN_STOCK
            android.util.Log.d("UnifiedItemRepository", "  → 使用库存状态: $fallbackStatus")
            fallbackStatus
        }
        android.util.Log.d("UnifiedItemRepository", "━━━━━ 最终物品状态: $itemStatus ━━━━━")
        
        return Item(
            id = unifiedItem.id,
            name = unifiedItem.name,
            quantity = inventoryDetail?.quantity ?: shoppingDetail?.quantity ?: 0.0,
            unit = inventoryDetail?.unit ?: shoppingDetail?.quantityUnit ?: "",
            location = null, // 需要额外查询LocationEntity
            category = unifiedItem.category,
            addDate = unifiedItem.createdDate,
            productionDate = inventoryDetail?.productionDate,
            expirationDate = inventoryDetail?.expirationDate,
            openStatus = inventoryDetail?.openStatus,
            openDate = inventoryDetail?.openDate,
            brand = unifiedItem.brand,
            specification = unifiedItem.specification,
            status = itemStatus,  // ✅ 使用检查后的状态
            stockWarningThreshold = inventoryDetail?.stockWarningThreshold,
            price = inventoryDetail?.price,
            priceUnit = inventoryDetail?.priceUnit,
            purchaseChannel = inventoryDetail?.purchaseChannel,
            storeName = inventoryDetail?.storeName ?: shoppingDetail?.storeName,
            subCategory = unifiedItem.subCategory,
            customNote = unifiedItem.customNote,
            season = unifiedItem.season, // 从UnifiedItemEntity读取
            capacity = unifiedItem.capacity, // 从UnifiedItemEntity读取
            capacityUnit = unifiedItem.capacityUnit, // 从UnifiedItemEntity读取
            rating = unifiedItem.rating, // 从UnifiedItemEntity读取
            totalPrice = inventoryDetail?.totalPrice,
            totalPriceUnit = inventoryDetail?.totalPriceUnit,
            purchaseDate = inventoryDetail?.purchaseDate,
            shelfLife = inventoryDetail?.shelfLife,
            // warrantyPeriod 和 warrantyEndDate 已移至 WarrantyEntity
            warrantyPeriod = null,
            warrantyEndDate = null,
            serialNumber = unifiedItem.serialNumber, // 从UnifiedItemEntity读取
            locationAddress = unifiedItem.locationAddress, // GPS地址
            locationLatitude = unifiedItem.locationLatitude, // GPS纬度
            locationLongitude = unifiedItem.locationLongitude, // GPS经度
            isHighTurnover = inventoryDetail?.isHighTurnover ?: false,
            photos = photos,
            tags = tags,
            // ✅ 添加shoppingDetail
            shoppingDetail = shoppingDetail
        )
    }

    /**
     * 删除物品（兼容方法）
     */
    suspend fun deleteItem(item: Item) {
        softDeleteItem(item.id, "UI层删除")
    }

    /**
     * 获取所有分类（兼容方法）
     */
    suspend fun getAllCategories(): List<String> {
        return unifiedItemDao.getAllItems().first().map { it.category }.distinct()
    }

    /**
     * 获取所有品牌（兼容方法）
     */
    suspend fun getAllBrands(): List<String> {
        return unifiedItemDao.getAllItems().first().mapNotNull { it.brand }.distinct()
    }

    /**
     * 获取所有标签（兼容方法）
     */
    suspend fun getAllTags(): List<String> {
        return try {
            tagDao.getAllTagNames()
        } catch (e: Exception) {
            android.util.Log.e("UnifiedItemRepository", "查询标签失败", e)
            emptyList()
        }
    }

    /**
     * 获取所有子分类（兼容方法）
     */
    suspend fun getAllSubCategories(): List<String> {
        return unifiedItemDao.getAllItems().first().mapNotNull { it.subCategory }.distinct()
    }

    /**
     * 获取所有季节（兼容方法）
     */
    suspend fun getAllSeasons(): List<String> {
        // 注意：season已移至UnifiedItemEntity
        val allSeasons = unifiedItemDao.getAllItems().first()
            .mapNotNull { it.season }
            .flatMap { seasonString ->
                // 将逗号分隔的季节字符串拆分成独立的季节
                seasonString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }
            .distinct()
        
        // 🌸🌞🍂❄️ 季节自然排序：春夏秋冬优先，其他自定义季节按字母排序
        val standardSeasons = listOf("春", "夏", "秋", "冬")
        val standardFound = mutableListOf<String>()
        val customSeasons = mutableListOf<String>()
        
        allSeasons.forEach { season ->
            if (standardSeasons.contains(season)) {
                standardFound.add(season)
            } else {
                customSeasons.add(season)
            }
        }
        
        // 按标准顺序排列标准季节，自定义季节按字母排序
        val sortedStandard = standardSeasons.filter { standardFound.contains(it) }
        val sortedCustom = customSeasons.sorted()
        
        return sortedStandard + sortedCustom
    }

    /**
     * 获取所有位置区域（兼容方法）
     */
    suspend fun getAllLocationAreas(): List<String> {
        return try {
            locationDao.getAllAreas()
        } catch (e: Exception) {
            android.util.Log.e("UnifiedItemRepository", "查询位置区域失败", e)
            emptyList()
        }
    }

    /**
     * 获取仓库物品（兼容方法）
     * 注意：这个实现暂时不包含位置、标签、照片的实时查询，因为Flow combine中无法调用suspend方法
     * 如需完整功能，请使用getAllWarehouseItemsWithDetails()方法
     */
    fun getWarehouseItems(): Flow<List<WarehouseItem>> {
        return combine(
            unifiedItemDao.getAllItems(),
            inventoryDetailDao.getAllDetails(),
            itemStateDao.getActiveStatesByType(ItemStateType.INVENTORY)
        ) { unifiedItems, inventoryDetails, inventoryStates ->
            // 创建映射表
            val unifiedItemMap = unifiedItems.associateBy { it.id }
            val inventoryStateMap = inventoryStates.associateBy { it.itemId }

            // 组装WarehouseItem对象（不包含异步查询的位置、标签、照片）
            inventoryDetails.mapNotNull { inventoryDetail ->
                val unifiedItem = unifiedItemMap[inventoryDetail.itemId]
                val itemState = inventoryStateMap[inventoryDetail.itemId]

                if (unifiedItem != null && itemState != null && itemState.isActive) {
                    WarehouseItem(
                        id = unifiedItem.id,
                        name = unifiedItem.name,
                        primaryPhotoUri = null, // 需要异步查询
                        quantity = inventoryDetail.quantity.toInt(),
                        expirationDate = inventoryDetail.expirationDate?.time,
                        locationArea = null, // 需要异步查询
                        locationContainer = null, // 需要异步查询
                        locationSublocation = null, // 需要异步查询
                        category = unifiedItem.category,
                        subCategory = unifiedItem.subCategory,
                        brand = unifiedItem.brand,
                        rating = unifiedItem.rating?.toFloat(), // 从UnifiedItemEntity读取
                        price = inventoryDetail.price,
                        priceUnit = inventoryDetail.priceUnit,
                        openStatus = when (inventoryDetail.openStatus) {
                            com.example.itemmanagement.data.model.OpenStatus.OPENED -> true
                            com.example.itemmanagement.data.model.OpenStatus.UNOPENED -> false
                            else -> null
                        },
                        addDate = unifiedItem.createdDate.time,
                        tagsList = null, // 需要异步查询
                        customNote = unifiedItem.customNote
                    )
                } else null
            }.sortedByDescending { it.addDate }
        }
    }

    /**
     * 获取完整的仓库物品（包含位置、标签、照片信息）
     */
    suspend fun getAllWarehouseItemsWithDetails(): List<WarehouseItem> {
        return try {
            android.util.Log.d("UnifiedItemRepository", "🚀 开始查询仓库物品详细信息")
            
            // 获取基础数据
            val unifiedItems = unifiedItemDao.getAllItems().first()
            android.util.Log.d("UnifiedItemRepository", "📋 查询到UnifiedItems: ${unifiedItems.size}个")
            unifiedItems.forEachIndexed { index, item ->
                android.util.Log.d("UnifiedItemRepository", "  [$index] UnifiedItem: id=${item.id}, name='${item.name}', category='${item.category}', brand='${item.brand}'")
            }
            
            val inventoryDetails = inventoryDetailDao.getAllDetails().first()
            android.util.Log.d("UnifiedItemRepository", "📦 查询到InventoryDetails: ${inventoryDetails.size}个")
            inventoryDetails.forEachIndexed { index, detail ->
                android.util.Log.d("UnifiedItemRepository", "  [$index] InventoryDetail: itemId=${detail.itemId}, locationId=${detail.locationId}, price=${detail.price}")
            }
            
            val inventoryStates = itemStateDao.getActiveStatesByType(ItemStateType.INVENTORY).first()
            android.util.Log.d("UnifiedItemRepository", "🔄 查询到InventoryStates: ${inventoryStates.size}个")
            inventoryStates.forEachIndexed { index, state ->
                android.util.Log.d("UnifiedItemRepository", "  [$index] ItemState: itemId=${state.itemId}, isActive=${state.isActive}")
            }
            
            // 创建映射表
            val unifiedItemMap = unifiedItems.associateBy { it.id }
            val inventoryStateMap = inventoryStates.associateBy { it.itemId }

            // 组装完整的WarehouseItem对象
            val warehouseItems = mutableListOf<WarehouseItem>()
            inventoryDetails.forEachIndexed { index, inventoryDetail ->
                android.util.Log.d("UnifiedItemRepository", "🔧 处理第[$index]个InventoryDetail: itemId=${inventoryDetail.itemId}")
                
                val unifiedItem = unifiedItemMap[inventoryDetail.itemId]
                val itemState = inventoryStateMap[inventoryDetail.itemId]

                android.util.Log.d("UnifiedItemRepository", "  找到UnifiedItem: ${unifiedItem != null}, 找到ItemState: ${itemState != null}, 状态激活: ${itemState?.isActive}")

                if (unifiedItem != null && itemState != null && itemState.isActive) {
                    android.util.Log.d("UnifiedItemRepository", "  ✅ 开始查询关联数据...")
                    
                    // 获取位置信息
                    val location = inventoryDetail.locationId?.let { locationId ->
                        android.util.Log.d("UnifiedItemRepository", "    📍 查询位置ID: $locationId")
                        val loc = locationDao.getById(locationId)
                        android.util.Log.d("UnifiedItemRepository", "    📍 位置结果: ${if (loc != null) "area='${loc.area}', container='${loc.container}', sublocation='${loc.sublocation}'" else "null"}")
                        loc
                    }
                    
                    // 获取标签信息
                    android.util.Log.d("UnifiedItemRepository", "    🏷️ 查询物品标签: itemId=${unifiedItem.id}")
                    val tags = tagDao.getTagsByItemId(unifiedItem.id)
                    android.util.Log.d("UnifiedItemRepository", "    🏷️ 标签结果: ${tags.size}个标签 - ${tags.map { "'${it.name}'" }}")
                    
                    // 获取封面照片（第一张照片）
                    android.util.Log.d("UnifiedItemRepository", "    📸 查询封面照片: itemId=${unifiedItem.id}")
                    
                    val coverPhoto = photoDao.getFirstPhotoByItemId(unifiedItem.id)
                    android.util.Log.d("UnifiedItemRepository", "    📸 封面照片结果: ${if (coverPhoto != null) "id=${coverPhoto.id}, uri='${coverPhoto.uri}', displayOrder=${coverPhoto.displayOrder}" else "null"}")
                    
                    val warehouseItem = WarehouseItem(
                        id = unifiedItem.id,
                        name = unifiedItem.name,
                        primaryPhotoUri = coverPhoto?.uri,
                        quantity = inventoryDetail.quantity.toInt(),
                        expirationDate = inventoryDetail.expirationDate?.time,
                        locationArea = location?.area,
                        locationContainer = location?.container,
                        locationSublocation = location?.sublocation,
                        category = unifiedItem.category,
                        subCategory = unifiedItem.subCategory,
                        brand = unifiedItem.brand,
                        rating = unifiedItem.rating?.toFloat(), // 从UnifiedItemEntity读取
                        price = inventoryDetail.price,
                        priceUnit = inventoryDetail.priceUnit,
                        openStatus = when (inventoryDetail.openStatus) {
                            com.example.itemmanagement.data.model.OpenStatus.OPENED -> true
                            com.example.itemmanagement.data.model.OpenStatus.UNOPENED -> false
                            else -> null
                        },
                        addDate = unifiedItem.createdDate.time,
                        tagsList = if (tags.isNotEmpty()) tags.take(3).joinToString(",") { it.name } else null,
                        customNote = unifiedItem.customNote,
                        season = unifiedItem.season // 从UnifiedItemEntity读取
                    )
                    
                    android.util.Log.d("UnifiedItemRepository", "  🎯 生成WarehouseItem: name='${warehouseItem.name}', locationArea='${warehouseItem.locationArea}', tagsList='${warehouseItem.tagsList}', rating=${warehouseItem.rating}")
                    warehouseItems.add(warehouseItem)
                } else {
                    android.util.Log.w("UnifiedItemRepository", "  ❌ 跳过此项: unifiedItem=${unifiedItem != null}, itemState=${itemState != null}, isActive=${itemState?.isActive}")
                }
            }

            val sortedItems = warehouseItems.sortedByDescending { it.addDate }
            android.util.Log.d("UnifiedItemRepository", "✅ 组装完成: ${sortedItems.size}个WarehouseItem")
            sortedItems.forEachIndexed { index, item ->
                android.util.Log.d("UnifiedItemRepository", "  最终结果[$index]: name='${item.name}', locationArea='${item.locationArea}', tagsList='${item.tagsList}', rating=${item.rating}")
            }

            sortedItems
        } catch (e: Exception) {
            android.util.Log.e("UnifiedItemRepository", "❌ 查询仓库物品失败", e)
            emptyList()
        }
    }

    /**
     * 根据ID获取物品详细信息（兼容方法）
     */
    suspend fun getItemWithDetailsById(itemId: Long): ItemWithDetails? {
        android.util.Log.d("UnifiedItemRepository", "🔍 查询物品详情，ID: $itemId")
        
        val unifiedItem = unifiedItemDao.getById(itemId)
        android.util.Log.d("UnifiedItemRepository", "📋 查询到的UnifiedItem: $unifiedItem")
        
        if (unifiedItem == null) {
            android.util.Log.w("UnifiedItemRepository", "❌ 未找到UnifiedItem，ID: $itemId")
            return null
        }
        
        var inventoryDetail = inventoryDetailDao.getByItemId(itemId)
        android.util.Log.d("UnifiedItemRepository", "📦 查询到的InventoryDetail: $inventoryDetail")
        
        // ✅ 检查借出状态并更新InventoryDetail的status
        android.util.Log.d("UnifiedItemRepository", "━━━━━ 开始检查借出状态 ━━━━━")
        android.util.Log.d("UnifiedItemRepository", "正在查询itemId=$itemId 的借出记录...")
        val borrowRecords = borrowDao.getAll()
        android.util.Log.d("UnifiedItemRepository", "数据库中总共有 ${borrowRecords.size} 条借出记录")
        
        // 打印所有借出记录
        borrowRecords.forEachIndexed { index, record ->
            android.util.Log.d("UnifiedItemRepository", "  借出记录[$index]: itemId=${record.itemId}, borrower=${record.borrowerName}, status=${record.status}")
        }
        
        // 查找当前物品的未归还记录
        val currentBorrow = borrowRecords.find { 
            it.itemId == itemId && 
            (it.status == BorrowStatus.BORROWED || it.status == BorrowStatus.OVERDUE)
        }
        
        if (currentBorrow != null) {
            android.util.Log.d("UnifiedItemRepository", "✅ 找到未归还记录！")
            android.util.Log.d("UnifiedItemRepository", "  - 借给: ${currentBorrow.borrowerName}")
            android.util.Log.d("UnifiedItemRepository", "  - 状态: ${currentBorrow.status}")
            android.util.Log.d("UnifiedItemRepository", "  - 预计归还: ${java.util.Date(currentBorrow.expectedReturnDate)}")
            android.util.Log.d("UnifiedItemRepository", "  → 设置物品状态为: BORROWED")
            // 更新InventoryDetail的status
            inventoryDetail = inventoryDetail?.copy(status = ItemStatus.BORROWED)
            android.util.Log.d("UnifiedItemRepository", "✅ InventoryDetail状态已更新: ${inventoryDetail?.status}")
        } else {
            android.util.Log.d("UnifiedItemRepository", "❌ 未找到未归还的借出记录")
            android.util.Log.d("UnifiedItemRepository", "  → 保持原有状态: ${inventoryDetail?.status}")
        }
        android.util.Log.d("UnifiedItemRepository", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        // 查询照片
        val photos = try {
            photoDao.getPhotosByItemId(itemId)
        } catch (e: Exception) {
            android.util.Log.e("UnifiedItemRepository", "❌ 查询照片失败: itemId=$itemId", e)
            emptyList()
        }
        android.util.Log.d("UnifiedItemRepository", "📸 查询到的Photos: ${photos.size}张")
        
        // 查询标签
        val tags = try {
            tagDao.getTagsByItemId(itemId)
        } catch (e: Exception) {
            android.util.Log.e("UnifiedItemRepository", "❌ 查询标签失败: itemId=$itemId", e)
            emptyList()
        }
        android.util.Log.d("UnifiedItemRepository", "🏷️ 查询到的Tags: ${tags.map { it.name }}")
        
        // 查询位置信息
        val location = inventoryDetail?.locationId?.let { locationId ->
            try {
                locationDao.getById(locationId)
            } catch (e: Exception) {
                android.util.Log.e("UnifiedItemRepository", "❌ 查询位置失败: locationId=$locationId", e)
                null
            }
        }
        android.util.Log.d("UnifiedItemRepository", "📍 查询到的Location: ${location?.let { "area='${it.area}', container='${it.container}', sublocation='${it.sublocation}'" } ?: "null"}")
        
        val itemWithDetails = ItemWithDetails(
            unifiedItem = unifiedItem,
            inventoryDetail = inventoryDetail,
            photos = photos,
            tags = tags
        ).apply {
            // 临时存储位置信息，供转换时使用
            this.locationEntity = location
        }
        
        android.util.Log.d("UnifiedItemRepository", "✅ 组装完成的ItemWithDetails: photos=${photos.size}, tags=${tags.size}, location=${location != null}")
        return itemWithDetails
    }

    /**
     * 更新物品详细信息（兼容方法）
     */
    suspend fun updateItemWithDetails(itemWithDetails: ItemWithDetails) {
        android.util.Log.d("UnifiedItemRepository", "🔄 开始更新物品详细信息: itemId=${itemWithDetails.unifiedItem.id}")
        android.util.Log.d("UnifiedItemRepository", "📍 UnifiedItem地点信息 - 地址: ${itemWithDetails.unifiedItem.locationAddress}, 纬度: ${itemWithDetails.unifiedItem.locationLatitude}, 经度: ${itemWithDetails.unifiedItem.locationLongitude}")
        
        appDatabase.withTransaction {
            // 1. 更新UnifiedItem
            android.util.Log.d("UnifiedItemRepository", "📋 更新UnifiedItem")
            unifiedItemDao.update(itemWithDetails.unifiedItem)
            
            // 2. 更新InventoryDetail
            itemWithDetails.inventoryDetail?.let { detail ->
                android.util.Log.d("UnifiedItemRepository", "📦 更新InventoryDetail")
                inventoryDetailDao.update(detail)
            }
            
            // 3. 更新位置信息
            val itemId = itemWithDetails.unifiedItem.id
            itemWithDetails.locationEntity?.let { location ->
                android.util.Log.d("UnifiedItemRepository", "📍 更新位置信息: area='${location.area}', container='${location.container}', sublocation='${location.sublocation}'")
                
                // 使用findOrCreateLocation方法获取位置ID
                val locationId = locationDao.findOrCreateLocation(
                    area = location.area,
                    container = location.container,
                    sublocation = location.sublocation
                )
                
                android.util.Log.d("UnifiedItemRepository", "📍 位置ID: $locationId")
                
                // 更新InventoryDetail中的locationId
                itemWithDetails.inventoryDetail?.let { detail ->
                    val updatedDetail = detail.copy(locationId = locationId)
                    inventoryDetailDao.update(updatedDetail)
                    android.util.Log.d("UnifiedItemRepository", "📦 已更新InventoryDetail的locationId")
                }
            }
            
            // 4. 更新照片
            android.util.Log.d("UnifiedItemRepository", "📸 更新照片: ${itemWithDetails.photos.size}张")
            
            // 删除旧照片
            photoDao.deletePhotosByItemId(itemId)
            
            // 插入新照片
            itemWithDetails.photos.forEach { photo ->
                photoDao.insert(photo.copy(itemId = itemId))
            }
            
            // 5. 更新标签
            android.util.Log.d("UnifiedItemRepository", "🏷️ 更新标签: ${itemWithDetails.tags.size}个")
            
            // 提取标签名称
            val tagNames = itemWithDetails.tags.map { it.name }
            android.util.Log.d("UnifiedItemRepository", "🏷️ 标签名称: $tagNames")
            
            // 使用TagDao的事务方法设置标签（会自动删除旧关联并创建新关联）
            tagDao.setItemTags(itemId, tagNames)
            
            android.util.Log.d("UnifiedItemRepository", "✅ 物品详细信息更新完成")
        }
    }

    /**
     * 插入购物物品简单版（兼容方法）
     */
    suspend fun insertShoppingItemSimple(
        unifiedItem: UnifiedItemEntity,
        shoppingDetail: ShoppingDetailEntity,
        photoUris: List<android.net.Uri> = emptyList(),
        tags: Map<String, Set<String>> = emptyMap()
    ): Long {
        return appDatabase.withTransaction {
            // 1. 插入基础物品信息
            val itemId = unifiedItemDao.insert(unifiedItem)
            
            // 2. 插入购物详情
            shoppingDetailDao.insert(shoppingDetail.copy(itemId = itemId))
            
            // 3. 插入物品状态
            itemStateDao.insert(ItemStateEntity(itemId = itemId, stateType = ItemStateType.SHOPPING))
            
            // 4. 保存照片
            photoUris.forEach { uri ->
                val photo = com.example.itemmanagement.data.entity.PhotoEntity(
                    itemId = itemId,
                    uri = uri.toString()
                )
                photoDao.insert(photo)
            }
            
            // 5. 保存标签
            tags.forEach { (category, tagNames) ->
                tagNames.forEach { tagName ->
                    // 查找或创建标签
                    val tagId = tagDao.findOrCreateTag(tagName, null)
                    // 创建关联
                    tagDao.insertItemTagCrossRef(
                        com.example.itemmanagement.data.entity.ItemTagCrossRef(
                            itemId = itemId,
                            tagId = tagId
                        )
                    )
                }
            }
            
            itemId
        }
    }

    /**
     * 获取容器按区域（兼容方法）
     */
    suspend fun getContainersByArea(area: String): List<String> {
        return try {
            locationDao.getContainersByArea(area)
        } catch (e: Exception) {
            android.util.Log.e("UnifiedItemRepository", "查询容器失败: area=$area", e)
            emptyList()
        }
    }

    /**
     * 获取子位置（兼容方法）
     */
    suspend fun getSublocations(area: String, container: String): List<String> {
        return try {
            locationDao.getSublocations(area, container)
        } catch (e: Exception) {
            android.util.Log.e("UnifiedItemRepository", "查询子位置失败: area=$area, container=$container", e)
            emptyList()
        }
    }

    /**
     * 获取所有物品详情（兼容方法）
     */
    fun getAllItemsWithDetails(): Flow<List<ItemWithDetails>> {
        return inventoryDetailDao.getAllDetails().map { inventoryItems ->
            inventoryItems.mapNotNull { inventoryDetail ->
                unifiedItemDao.getById(inventoryDetail.itemId)?.let { unifiedItem ->
                    ItemWithDetails(
                        unifiedItem = unifiedItem,
                        inventoryDetail = inventoryDetail,
                        photos = emptyList(), // TODO: 查询PhotoEntity
                        tags = emptyList() // TODO: 查询TagEntity
                    )
                }
            }
        }
    }
    
    /**
     * 获取所有物品详情（排除已删除的物品）
     */
    fun getActiveItemsWithDetails(): Flow<List<ItemWithDetails>> {
        return getAllItemsWithDetails().map { items ->
            items.filter { item ->
                // 检查物品是否被标记为删除状态
                val deletedStates = itemStateDao.getStatesByItemIdAndType(item.item.id, ItemStateType.DELETED)
                val hasActiveDeletedState = deletedStates.any { it.isActive }
                !hasActiveDeletedState
            }
        }
    }

    /**
     * 获取万物分析数据（统一架构版本）
     */
    suspend fun getInventoryAnalysisData(): com.example.itemmanagement.data.model.InventoryAnalysisData {
        try {
            android.util.Log.d("AnalysisData", "🔍 开始获取万物分析数据")
            
            // 获取所有活跃的库存物品
            val inventoryStates = itemStateDao.getActiveStatesByType(ItemStateType.INVENTORY).first()
            val activeItemIds = inventoryStates.map { it.itemId }.toSet()
            android.util.Log.d("AnalysisData", "📊 活跃库存物品数: ${activeItemIds.size}")
            
            // 获取所有相关数据
            val allUnifiedItems = unifiedItemDao.getAllItems().first()
            val allInventoryDetails = inventoryDetailDao.getAllDetails().first()
            
            // 过滤出活跃的库存物品
            val activeItems = allUnifiedItems.filter { it.id in activeItemIds }
            val activeDetails = allInventoryDetails.filter { it.itemId in activeItemIds }
            
            // 1. 计算核心统计指标
            val totalItems = activeItems.size
            val totalValue = activeDetails.sumOf { it.price ?: 0.0 }
            
            // 分类数量
            val categoriesCount = activeItems.map { it.category }.filter { !it.isNullOrBlank() }.distinct().size
            
            // 位置数量（从 InventoryDetail 的 locationId 统计）
            val locationIds = activeDetails.mapNotNull { it.locationId }.distinct()
            val locationsCount = locationIds.size
            
            // 标签数量
            val allTags = activeItems.flatMap { item ->
                runBlocking { tagDao.getTagsByItemId(item.id) }
            }
            val tagsCount = allTags.distinctBy { it.id }.size
            
            // 即将过期的物品（30天内）
            val now = java.util.Calendar.getInstance()
            val thirtyDaysLater = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, 30) }
            val expiringItems = activeDetails.count { detail ->
                detail.expirationDate?.let { expirationDate ->
                    val expDate = java.util.Calendar.getInstance().apply {
                        timeInMillis = expirationDate.time
                    }
                    expDate.after(now) && expDate.before(thirtyDaysLater)
                } ?: false
            }
            
            // 已过期的物品
            val expiredItems = activeDetails.count { detail ->
                detail.expirationDate?.let { expirationDate ->
                    expirationDate.before(java.util.Date())
                } ?: false
            }
            
            // 库存不足的物品
            val lowStockItems = activeDetails.count { detail ->
                val quantity: Double = detail.quantity ?: 0.0
                val threshold: Int = detail.stockWarningThreshold ?: 0
                quantity > 0.0 && threshold > 0 && quantity <= threshold.toDouble()
            }
            
            // 最近添加的物品（7天内）
            val sevenDaysAgo = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -7) }
            val recentlyAddedItems = activeItems.count { item ->
                item.createdDate?.let { createdDate ->
                    val createCal = java.util.Calendar.getInstance().apply {
                        timeInMillis = createdDate.time
                    }
                    createCal.after(sevenDaysAgo)
                } ?: false
            }
            
            android.util.Log.d("AnalysisData", "📈 核心指标: 总物品=$totalItems, 总价值=$totalValue, 分类=$categoriesCount, 位置=$locationsCount, 标签=$tagsCount")
            android.util.Log.d("AnalysisData", "⚠️ 预警指标: 即将过期=$expiringItems, 已过期=$expiredItems, 库存不足=$lowStockItems, 最近添加=$recentlyAddedItems")
            
            val inventoryStats = com.example.itemmanagement.data.model.InventoryStats(
                totalItems = totalItems,
                totalValue = totalValue,
                categoriesCount = categoriesCount,
                locationsCount = locationsCount,
                tagsCount = tagsCount,
                expiringItems = expiringItems,
                expiredItems = expiredItems,
                lowStockItems = lowStockItems,
                recentlyAddedItems = recentlyAddedItems
            )
            
            // 2. 分类分析
            val categoryMap = mutableMapOf<String, Pair<Int, Double>>()
            activeItems.forEach { item ->
                val category = item.category ?: "未分类"
                val detail = activeDetails.find { it.itemId == item.id }
                val value = detail?.price ?: 0.0
                
                val (count, totalVal) = categoryMap[category] ?: (0 to 0.0)
                categoryMap[category] = (count + 1) to (totalVal + value)
            }
            val categoryAnalysis = categoryMap.map { (category, pair) ->
                com.example.itemmanagement.data.model.CategoryValue(category, pair.first, pair.second)
            }.sortedByDescending { it.count }
            
            android.util.Log.d("AnalysisData", "📂 分类分析: ${categoryAnalysis.size}个分类")
            
            // 3. 位置分析
            val locationMap = mutableMapOf<String, Pair<Int, Double>>()
            activeDetails.forEach { detail ->
                val locationEntity = detail.locationId?.let { runBlocking { locationDao.getById(it) } }
                val locationStr = locationEntity?.let { "${it.area}-${it.container}" } ?: "未设置位置"
                val value = detail.price ?: 0.0
                
                val (count, totalVal) = locationMap[locationStr] ?: (0 to 0.0)
                locationMap[locationStr] = (count + 1) to (totalVal + value)
            }
            val locationAnalysis = locationMap.map { (location, pair) ->
                com.example.itemmanagement.data.model.LocationValue(location, pair.first, pair.second)
            }.sortedByDescending { it.count }
            
            android.util.Log.d("AnalysisData", "📍 位置分析: ${locationAnalysis.size}个位置")
            
            // 4. 标签分析
            val tagMap = mutableMapOf<String, Pair<Int, Double>>()
            activeItems.forEach { item ->
                val tags = runBlocking { tagDao.getTagsByItemId(item.id) }
                val detail = activeDetails.find { it.itemId == item.id }
                val value = detail?.price ?: 0.0
                
                tags.forEach { tag ->
                    val (count, totalVal) = tagMap[tag.name] ?: (0 to 0.0)
                    tagMap[tag.name] = (count + 1) to (totalVal + value)
                }
            }
            val tagAnalysis = tagMap.map { (tag, pair) ->
                com.example.itemmanagement.data.model.TagValue(tag, pair.first, pair.second)
            }.sortedByDescending { it.count }
            
            android.util.Log.d("AnalysisData", "🏷️ 标签分析: ${tagAnalysis.size}个标签")
            
            // 5. 月度趋势分析
            val monthFormat = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
            val monthMap = mutableMapOf<String, Pair<Int, Double>>()
            
            activeItems.forEach { item ->
                val month = item.createdDate?.let { monthFormat.format(it) } ?: "未知"
                val detail = activeDetails.find { it.itemId == item.id }
                val value = detail?.price ?: 0.0
                
                val (count, totalVal) = monthMap[month] ?: (0 to 0.0)
                monthMap[month] = (count + 1) to (totalVal + value)
            }
            val monthlyTrends = monthMap.map { (month, pair) ->
                com.example.itemmanagement.data.model.MonthlyTrend(month, pair.first, pair.second)
            }.sortedBy { it.month }
            
            android.util.Log.d("AnalysisData", "📅 月度趋势: ${monthlyTrends.size}个月份")
            
            android.util.Log.d("AnalysisData", "✅ 万物分析数据获取成功")
            
        return com.example.itemmanagement.data.model.InventoryAnalysisData(
                inventoryStats = inventoryStats,
                categoryAnalysis = categoryAnalysis,
                locationAnalysis = locationAnalysis,
                tagAnalysis = tagAnalysis,
                monthlyTrends = monthlyTrends
            )
            
        } catch (e: Exception) {
            android.util.Log.e("AnalysisData", "❌ 获取万物分析数据失败", e)
            // 返回空数据而不是抛出异常
            return com.example.itemmanagement.data.model.InventoryAnalysisData(
                inventoryStats = com.example.itemmanagement.data.model.InventoryStats(
                totalItems = 0,
                totalValue = 0.0,
                categoriesCount = 0,
                locationsCount = 0,
                tagsCount = 0
            ),
            categoryAnalysis = emptyList(),
            locationAnalysis = emptyList(),
            tagAnalysis = emptyList(),
            monthlyTrends = emptyList()
        )
        }
    }

    /**
     * 获取所有日历事件（兼容方法）
     */
    fun getAllCalendarEvents(): Flow<List<com.example.itemmanagement.data.entity.CalendarEventEntity>> {
        return appDatabase.calendarEventDao().getAllEvents()
    }

    /**
     * 根据日期范围获取日历事件（兼容方法）
     */
    fun getCalendarEventsBetweenDates(startDate: java.util.Date, endDate: java.util.Date): Flow<List<com.example.itemmanagement.data.entity.CalendarEventEntity>> {
        return appDatabase.calendarEventDao().getEventsBetweenDates(startDate, endDate)
    }

    /**
     * 插入日历事件（兼容方法）
     */
    suspend fun insertCalendarEvent(event: com.example.itemmanagement.data.entity.CalendarEventEntity) {
        appDatabase.calendarEventDao().insertEvent(event)
    }

    /**
     * 插入多个日历事件（兼容方法）
     */
    suspend fun insertCalendarEvents(events: List<com.example.itemmanagement.data.entity.CalendarEventEntity>) {
        appDatabase.calendarEventDao().insertEvents(events)
    }

    /**
     * 删除日历事件（兼容方法）
     */
    suspend fun deleteCalendarEvent(eventId: Long) {
        appDatabase.calendarEventDao().deleteEventById(eventId)
    }

    /**
     * 标记日历事件完成（兼容方法）
     */
    suspend fun markCalendarEventCompleted(eventId: Long) {
        appDatabase.calendarEventDao().markEventCompleted(eventId)
    }

    /**
     * 获取所有购物清单
     */
    fun getAllShoppingLists(): Flow<List<com.example.itemmanagement.data.entity.ShoppingListEntity>> {
        return shoppingListDao.getAllShoppingLists()
    }

    /**
     * 获取活跃的购物清单
     */
    fun getActiveShoppingLists(): Flow<List<com.example.itemmanagement.data.entity.ShoppingListEntity>> {
        return shoppingListDao.getActiveShoppingLists()
    }

    /**
     * 插入购物清单
     */
    suspend fun insertShoppingList(shoppingList: com.example.itemmanagement.data.entity.ShoppingListEntity): Long {
        return shoppingListDao.insertShoppingList(shoppingList)
    }

    /**
     * 更新购物清单
     */
    suspend fun updateShoppingList(shoppingList: com.example.itemmanagement.data.entity.ShoppingListEntity) {
        shoppingListDao.updateShoppingList(shoppingList)
    }

    /**
     * 删除购物清单
     */
    suspend fun deleteShoppingList(shoppingListId: Long) {
        shoppingListDao.deleteShoppingListById(shoppingListId)
    }

    /**
     * 根据ID获取购物清单
     */
    suspend fun getShoppingListById(listId: Long): com.example.itemmanagement.data.entity.ShoppingListEntity? {
        return shoppingListDao.getShoppingListById(listId)
    }

    /**
     * 获取购物物品数量（只统计活跃物品，排除已删除/已转移）
     * ⭐ 修复：使用新的 getActiveItemCountByListId 方法
     */
    suspend fun getShoppingItemsCountByListId(listId: Long): Int {
        return shoppingDetailDao.getActiveItemCountByListId(listId)
    }

    /**
     * 获取待购买物品数量（只统计活跃物品，排除已删除/已转移）
     * ⭐ 修复：使用新的 getActivePendingCountByListId 方法
     */
    suspend fun getPendingShoppingItemsCountByListId(listId: Long): Int {
        return shoppingDetailDao.getActivePendingCountByListId(listId)
    }
    
    /**
     * 获取已购买物品数量（只统计活跃物品，排除已删除/已转移）
     * ⭐ 修复：使用新的 getActivePurchasedCountByListId 方法
     */
    suspend fun getPurchasedShoppingItemsCountByListId(listId: Long): Int {
        return shoppingDetailDao.getActivePurchasedCountByListId(listId)
    }
    
    /**
     * 获取指定购物清单的所有物品（仅活跃状态）
     * 返回完整的Item对象，包含购物详情
     * 
     * ⭐ 关键：只返回 isActive=true 的购物物品
     * - 已转入库存的物品不会显示（逻辑删除）
     * - ShoppingDetailEntity 仍保留在数据库中作为历史记录
     * 
     * 修复：使用 combine 同时监听 shopping_details 和 item_states 表的变化
     * - 当删除/清除物品时，item_states 表会更新，Flow 会自动刷新
     */
    fun getItemsByShoppingList(listId: Long): Flow<List<Item>> {
        return combine(
            shoppingDetailDao.getByShoppingListId(listId),
            itemStateDao.getActiveStatesByType(ItemStateType.SHOPPING)
        ) { shoppingDetails, activeShoppingStates ->
            // 创建活跃购物状态的 itemId 集合，用于快速查找
            val activeShoppingItemIds = activeShoppingStates
                .filter { it.isActive && it.contextId == listId }
                .map { it.itemId }
                .toSet()
            
            shoppingDetails.mapNotNull { detail ->
                // ⭐ 检查该物品是否有活跃的购物状态
                if (!activeShoppingItemIds.contains(detail.itemId)) {
                    // 购物状态已停用，不显示（已转入库存或已删除）
                    return@mapNotNull null
                }
                
                val unifiedItem = unifiedItemDao.getById(detail.itemId) ?: return@mapNotNull null
                val photoEntities = photoDao.getPhotosByItemId(detail.itemId)
                val tagEntities = tagDao.getTagsByItemId(detail.itemId)
                
                // 转换为Photo和Tag模型
                val photos = photoEntities.map { 
                    com.example.itemmanagement.data.model.Photo(it.id, it.uri, it.isMain) 
                }
                val tags = tagEntities.map { 
                    com.example.itemmanagement.data.model.Tag(it.id, it.name) 
                }
            
            Item(
                id = unifiedItem.id,
                name = unifiedItem.name,
                quantity = detail.quantity,
                unit = detail.quantityUnit,
                location = null,
                category = unifiedItem.category,
                addDate = unifiedItem.createdDate,
                productionDate = null,
                expirationDate = null,
                openStatus = null,
                openDate = null,
                brand = unifiedItem.brand,
                specification = unifiedItem.specification,
                status = ItemStatus.IN_STOCK,
                stockWarningThreshold = null,
                price = detail.estimatedPrice,
                priceUnit = detail.estimatedPriceUnit,  // ✅ 使用独立的预估价格单位
                purchaseChannel = detail.purchaseChannel,
                storeName = detail.storeName,
                subCategory = unifiedItem.subCategory,
                customNote = unifiedItem.customNote,
                season = unifiedItem.season, // 从UnifiedItemEntity读取
                capacity = unifiedItem.capacity, // 从UnifiedItemEntity读取
                capacityUnit = unifiedItem.capacityUnit, // 从UnifiedItemEntity读取
                rating = unifiedItem.rating, // 从UnifiedItemEntity读取
                totalPrice = detail.totalPrice,
                totalPriceUnit = detail.totalPriceUnit,
                purchaseDate = detail.purchaseDate,
                shelfLife = null,
                warrantyPeriod = null,
                warrantyEndDate = null,
                serialNumber = unifiedItem.serialNumber, // 从UnifiedItemEntity读取
                locationAddress = unifiedItem.locationAddress, // GPS地址
                locationLatitude = unifiedItem.locationLatitude, // GPS纬度
                locationLongitude = unifiedItem.locationLongitude, // GPS经度
                isHighTurnover = false,
                photos = photos,
                tags = tags,
                shoppingDetail = detail
            )
            }
        }
    }
    
    /**
     * 更新购物详情
     */
    suspend fun updateShoppingDetail(detail: ShoppingDetailEntity) {
        shoppingDetailDao.update(detail)
    }
    
    /**
     * 更新购物物品（包含基础信息、购物详情、照片和标签）
     */
    suspend fun updateShoppingItem(
        itemId: Long,
        unifiedItem: UnifiedItemEntity,
        shoppingDetail: ShoppingDetailEntity,
        photoUris: List<android.net.Uri> = emptyList(),
        tags: Map<String, Set<String>> = emptyMap()
    ) {
        appDatabase.withTransaction {
            // 1. 更新 UnifiedItemEntity
            unifiedItemDao.update(unifiedItem.copy(id = itemId))
            
            // 2. 更新 ShoppingDetailEntity
            shoppingDetailDao.update(shoppingDetail.copy(itemId = itemId))
            
            // 3. 更新照片（先删除旧照片，再添加新照片）
            if (photoUris.isNotEmpty()) {
                // 删除旧照片
                photoDao.deletePhotosByItemId(itemId)
                // 添加新照片
                photoUris.forEach { uri ->
                    val photo = com.example.itemmanagement.data.entity.PhotoEntity(
                        itemId = itemId,
                        uri = uri.toString()
                    )
                    photoDao.insert(photo)
                }
            }
            
            // 4. 更新标签（先删除旧标签，再添加新标签）
            if (tags.isNotEmpty()) {
                // 删除旧标签的关联关系
                tagDao.deleteAllItemTagsByItemId(itemId)
                // 添加新标签
                tags.forEach { (category, tagNames) ->
                    tagNames.forEach { tagName ->
                        // 查找或创建标签
                        val tagId = tagDao.findOrCreateTag(tagName, null)
                        // 创建关联
                        tagDao.insertItemTagCrossRef(
                            com.example.itemmanagement.data.entity.ItemTagCrossRef(
                                itemId = itemId,
                                tagId = tagId
                            )
                        )
                    }
                }
            }
        }
    }
    
    /**
     * 删除购物详情
     */
    suspend fun deleteShoppingDetail(detail: ShoppingDetailEntity) {
        shoppingDetailDao.delete(detail)
    }
    
    /**
     * 停用物品的指定状态
     */
    suspend fun deactivateItemState(itemId: Long, stateType: ItemStateType) {
        val states = itemStateDao.getStatesByItemIdAndType(itemId, stateType)
        for (state in states) {
            if (state.isActive) {
                itemStateDao.update(state.deactivate("用户删除"))
            }
        }
    }
    
    /**
     * 获取购物详情（通过itemId）
     */
    suspend fun getShoppingDetailByItemId(itemId: Long): ShoppingDetailEntity? {
        return shoppingDetailDao.getByItemId(itemId)
    }
    
    /**
     * 获取完整的购物物品信息（用于转入库存）
     * @param itemId 物品ID
     * @return Item 对象，包含所有详情、照片、标签
     */
    suspend fun getCompleteShoppingItem(itemId: Long): Item? {
        // 1. 查询基础物品信息
        val unifiedItem = unifiedItemDao.getById(itemId) ?: return null
        
        // 2. 查询购物详情
        val shoppingDetail = shoppingDetailDao.getByItemId(itemId) ?: return null
        
        // 3. 查询照片
        val photoEntities = photoDao.getPhotosByItemId(itemId)
        val photos = photoEntities.map { 
            com.example.itemmanagement.data.model.Photo(it.id, it.uri, it.isMain) 
        }
        
        // 4. 查询标签
        val tagEntities = tagDao.getTagsByItemId(itemId)
        val tags = tagEntities.map { 
            com.example.itemmanagement.data.model.Tag(it.id, it.name) 
        }
        
        // 5. 构建 Item 对象
        return Item(
            id = unifiedItem.id,
            name = unifiedItem.name,
            quantity = shoppingDetail.quantity,
            unit = shoppingDetail.quantityUnit,
            location = null,
            category = unifiedItem.category,
            subCategory = unifiedItem.subCategory,
            addDate = unifiedItem.createdDate,
            productionDate = null,
            expirationDate = null,
            openStatus = null,
            openDate = null,
            brand = unifiedItem.brand,
            specification = unifiedItem.specification,
            status = ItemStatus.IN_STOCK,
            stockWarningThreshold = null,
            price = shoppingDetail.estimatedPrice,
            priceUnit = shoppingDetail.estimatedPriceUnit,
            purchaseChannel = shoppingDetail.purchaseChannel,
            storeName = shoppingDetail.storeName,
            customNote = unifiedItem.customNote,
            season = unifiedItem.season,
            capacity = unifiedItem.capacity,
            capacityUnit = unifiedItem.capacityUnit,
            rating = unifiedItem.rating?.toDouble(),
            totalPrice = shoppingDetail.totalPrice,
            totalPriceUnit = shoppingDetail.totalPriceUnit,
            purchaseDate = shoppingDetail.purchaseDate,
            shelfLife = null,
            warrantyPeriod = null,
            warrantyEndDate = null,
            serialNumber = unifiedItem.serialNumber,
            locationAddress = unifiedItem.locationAddress, // GPS地址
            locationLatitude = unifiedItem.locationLatitude, // GPS纬度
            locationLongitude = unifiedItem.locationLongitude, // GPS经度
            isHighTurnover = false,
            photos = photos,
            tags = tags,
            shoppingDetail = shoppingDetail
        )
    }
    
    /**
     * 获取指定物品的指定类型的所有状态记录
     */
    suspend fun getItemStatesByItemIdAndType(itemId: Long, stateType: ItemStateType): List<ItemStateEntity> {
        return itemStateDao.getStatesByItemIdAndType(itemId, stateType)
    }
    
    /**
     * 核心方法：将购物物品转换为库存物品（状态转换）
     * 
     * 这是"假转存"的核心实现：
     * 1. UnifiedItemEntity 保持不变（物品基础信息不变）
     * 2. 创建 InventoryDetailEntity（库存详情）
     * 3. 更新 ItemStateEntity: SHOPPING → INVENTORY（状态标记转换）
     * 4. ⭐ 保留 ShoppingDetailEntity（购物详情）- 作为历史记录
     * 
     * 设计理念：
     * - 逻辑删除（软删除）：通过 isActive 标记控制显示
     * - 保留历史数据：用于数据分析、预算对比等功能
     * - 统一架构优势：同一物品可以有多个状态的历史记录
     * 
     * 使用事务确保数据一致性
     */
    suspend fun transferShoppingToInventory(
        itemId: Long,
        shoppingDetail: ShoppingDetailEntity,
        inventoryDetail: InventoryDetailEntity
    ) {
        appDatabase.withTransaction {
            android.util.Log.d("StateTransfer", "========== 开始状态转换事务 ==========")
            android.util.Log.d("StateTransfer", "物品ID: $itemId")
            
            // 1. 创建库存详情
            inventoryDetailDao.insert(inventoryDetail)
            android.util.Log.d("StateTransfer", "✓ 步骤1: 已创建库存详情")
            
            // 2. 停用SHOPPING状态（逻辑删除，保留数据）
            val shoppingStates = itemStateDao.getStatesByItemIdAndType(itemId, ItemStateType.SHOPPING)
            val activeStatesCount = shoppingStates.count { it.isActive }
            for (state in shoppingStates) {
                if (state.isActive) {
                    itemStateDao.update(
                        state.copy(
                            isActive = false,  // ⭐ 标记为不活跃
                            deactivatedDate = java.util.Date(),
                            notes = "已转入库存 - 购物详情已归档"
                        )
                    )
                }
            }
            android.util.Log.d("StateTransfer", "✓ 步骤2: 已停用SHOPPING状态（${activeStatesCount}个）")
            
            // 3. 激活INVENTORY状态
            val newInventoryState = ItemStateEntity(
                itemId = itemId,
                stateType = ItemStateType.INVENTORY,
                isActive = true,
                activatedDate = java.util.Date(),
                contextId = null,
                notes = "从购物清单转入（购物详情ID: ${shoppingDetail.id}）"
            )
            itemStateDao.insert(newInventoryState)
            android.util.Log.d("StateTransfer", "✓ 步骤3: 已激活INVENTORY状态")
            
            // 4. ⭐ 保留购物详情（不删除，作为历史记录）
            // ShoppingDetailEntity 保留在数据库中
            // 通过 ItemStateEntity.isActive = false 来控制购物清单中不再显示
            // 好处：
            // - 可以查看购物历史
            // - 可以分析预算准确性（预估价格 vs 实际价格）
            // - 可以追溯采购决策
            android.util.Log.d("StateTransfer", "✓ 步骤4: 购物详情已归档保留（ID: ${shoppingDetail.id}）")
            
            // 5. 🛒 添加日历事件：记录购物入库操作
            try {
                val unifiedItem = unifiedItemDao.getById(itemId)
                if (unifiedItem != null) {
                    val event = com.example.itemmanagement.data.entity.CalendarEventEntity(
                        itemId = itemId,
                        eventType = com.example.itemmanagement.data.model.EventType.SHOPPING_TRANSFERRED,
                        title = "购物入库：${unifiedItem.name}",
                        description = "分类：${unifiedItem.category}",
                        eventDate = java.util.Date(),
                        reminderDays = emptyList(),
                        priority = com.example.itemmanagement.data.model.Priority.LOW,
                        isCompleted = true,
                        recurrenceType = null
                    )
                    appDatabase.calendarEventDao().insertEvent(event)
                    android.util.Log.d("StateTransfer", "📅 已添加日历事件：购物入库 - ${unifiedItem.name}")
                }
            } catch (e: Exception) {
                android.util.Log.e("StateTransfer", "添加购物入库日历事件失败", e)
            }
            
            android.util.Log.d("StateTransfer", "========== 状态转换事务完成 ==========")
        }
    }

    // ==================== 废料报告相关方法（兼容性） ====================

    /**
     * 检查并更新过期物品（兼容方法）
     * 自动将过期的库存物品状态从 IN_STOCK 更新为 EXPIRED
     */
    suspend fun checkAndUpdateExpiredItems(currentTime: Long) {
        android.util.Log.d("WasteReport", "开始检查过期物品，当前时间: ${java.util.Date(currentTime)}")
        
        // 1. 查询所有有过期日期且状态为IN_STOCK的物品
        val allDetails = inventoryDetailDao.getAllDetails().first()
        val currentDate = java.util.Date(currentTime)
        
        // 2. 筛选出已过期的物品
        val expiredDetails = allDetails.filter { detail ->
            detail.status == com.example.itemmanagement.data.model.ItemStatus.IN_STOCK &&
            detail.expirationDate != null &&
            detail.expirationDate.before(currentDate)
        }
        
        android.util.Log.d("WasteReport", "发现 ${expiredDetails.size} 个过期物品")
        
        // 3. 批量更新状态
        expiredDetails.forEach { detail ->
            // 更新库存详情状态
            inventoryDetailDao.update(
                detail.copy(
                    status = com.example.itemmanagement.data.model.ItemStatus.EXPIRED,
                    wasteDate = currentDate,
                    updatedDate = currentDate
                )
            )
            
            android.util.Log.d("WasteReport", "  - 更新物品 ${detail.itemId} 为EXPIRED状态")
        }
        
        android.util.Log.d("WasteReport", "过期物品检查完成")
    }

    /**
     * 获取废料报告数据（兼容方法）
     * 查询指定时间范围内的浪费物品（EXPIRED 或 DISCARDED 状态）
     */
    suspend fun getWasteReportData(startTime: Long, endTime: Long): List<com.example.itemmanagement.data.model.WastedItemInfo> {
        val startDate = java.util.Date(startTime)
        val endDate = java.util.Date(endTime)
        
        android.util.Log.d("WasteReport", "查询浪费报告数据: ${startDate} 到 ${endDate}")
        
        // 1. 查询所有库存详情
        val allDetails = inventoryDetailDao.getAllDetails().first()
        
        // 2. 筛选出浪费状态且在时间范围内的物品
        val wastedDetails = allDetails.filter { detail ->
            (detail.status == com.example.itemmanagement.data.model.ItemStatus.EXPIRED ||
             detail.status == com.example.itemmanagement.data.model.ItemStatus.DISCARDED) &&
            detail.wasteDate != null &&
            !detail.wasteDate.before(startDate) &&
            !detail.wasteDate.after(endDate)
        }
        
        android.util.Log.d("WasteReport", "找到 ${wastedDetails.size} 个浪费物品")
        
        // 3. 构建 WastedItemInfo 列表
        val wastedItemInfoList = wastedDetails.mapNotNull { detail ->
            val item = unifiedItemDao.getById(detail.itemId)
            if (item == null) {
                android.util.Log.w("WasteReport", "未找到物品ID ${detail.itemId} 的基础信息")
                return@mapNotNull null
            }
            
            // 获取主照片
            val photos = photoDao.getPhotosByItemId(detail.itemId)
            val photoUri = photos.firstOrNull { it.isMain }?.uri ?: photos.firstOrNull()?.uri
            
            // 计算物品价值（使用totalPrice或quantity*price）
            val value = detail.totalPrice ?: (detail.price?.let { it * detail.quantity } ?: 0.0)
            
            com.example.itemmanagement.data.model.WastedItemInfo(
                id = item.id,
                name = item.name,
                category = item.category,
                wasteDate = detail.wasteDate ?: detail.updatedDate, // fallback到updatedDate
                value = value,
                quantity = detail.quantity,
                unit = detail.unit,
                isQuantityUserInput = detail.isQuantityUserInput,
                status = detail.status.name,
                totalPrice = value,
                photoUri = photoUri
            )
        }
        
        // 4. 按浪费日期降序和价值降序排序
        val sortedList = wastedItemInfoList.sortedWith(
            compareByDescending<com.example.itemmanagement.data.model.WastedItemInfo> { it.wasteDate }
                .thenByDescending { it.value }
        )
        
        android.util.Log.d("WasteReport", "返回 ${sortedList.size} 个浪费物品，总价值: ${sortedList.sumOf { it.value }}")
        
        return sortedList
    }

    /**
     * 获取没有废料日期的物品（兼容方法）
     * 用于数据修复和调试，查询浪费状态但缺少 wasteDate 的物品
     */
    suspend fun getWastedItemsWithoutWasteDate(): List<com.example.itemmanagement.data.model.WastedItemInfo> {
        android.util.Log.d("WasteReport", "查询缺少wasteDate的浪费物品")
        
        // 1. 查询所有库存详情
        val allDetails = inventoryDetailDao.getAllDetails().first()
        
        // 2. 筛选出浪费状态但没有wasteDate的物品
        val wastedDetailsWithoutDate = allDetails.filter { detail ->
            (detail.status == com.example.itemmanagement.data.model.ItemStatus.EXPIRED ||
             detail.status == com.example.itemmanagement.data.model.ItemStatus.DISCARDED) &&
            detail.wasteDate == null
        }
        
        android.util.Log.d("WasteReport", "找到 ${wastedDetailsWithoutDate.size} 个缺少wasteDate的浪费物品")
        
        // 3. 构建 WastedItemInfo 列表
        val wastedItemInfoList = wastedDetailsWithoutDate.mapNotNull { detail ->
            val item = unifiedItemDao.getById(detail.itemId)
            if (item == null) {
                android.util.Log.w("WasteReport", "未找到物品ID ${detail.itemId} 的基础信息")
                return@mapNotNull null
            }
            
            // 获取主照片
            val photos = photoDao.getPhotosByItemId(detail.itemId)
            val photoUri = photos.firstOrNull { it.isMain }?.uri ?: photos.firstOrNull()?.uri
            
            // 计算物品价值
            val value = detail.totalPrice ?: (detail.price?.let { it * detail.quantity } ?: 0.0)
            
            // 使用 createdDate 作为临时的 wasteDate
            val fallbackDate = detail.updatedDate
            
            com.example.itemmanagement.data.model.WastedItemInfo(
                id = item.id,
                name = item.name,
                category = item.category,
                wasteDate = fallbackDate, // 使用fallback日期
                value = value,
                quantity = detail.quantity,
                unit = detail.unit,
                isQuantityUserInput = detail.isQuantityUserInput,
                status = detail.status.name,
                totalPrice = value,
                photoUri = photoUri
            )
        }
        
        android.util.Log.d("WasteReport", "返回 ${wastedItemInfoList.size} 个需要修复的物品")
        
        return wastedItemInfoList
    }

        /**
         * 修复没有废料日期的物品（兼容方法）
     * 自动为浪费状态但缺少 wasteDate 的物品设置日期
         */
        suspend fun fixWastedItemsWithoutWasteDate(currentTime: Long): Int {
        android.util.Log.d("WasteReport", "开始修复缺少wasteDate的浪费物品")
        
        val fallbackDate = java.util.Date(currentTime)
        
        // 1. 查询所有库存详情
        val allDetails = inventoryDetailDao.getAllDetails().first()
        
        // 2. 筛选出浪费状态但没有wasteDate的物品
        val wastedDetailsWithoutDate = allDetails.filter { detail ->
            (detail.status == com.example.itemmanagement.data.model.ItemStatus.EXPIRED ||
             detail.status == com.example.itemmanagement.data.model.ItemStatus.DISCARDED) &&
            detail.wasteDate == null
        }
        
        android.util.Log.d("WasteReport", "找到 ${wastedDetailsWithoutDate.size} 个需要修复的物品")
        
        // 3. 批量修复
        var fixedCount = 0
        wastedDetailsWithoutDate.forEach { detail ->
            // 如果是EXPIRED状态且有expirationDate，使用expirationDate
            // 否则使用当前时间
            val wasteDate = if (detail.status == com.example.itemmanagement.data.model.ItemStatus.EXPIRED && 
                                detail.expirationDate != null) {
                detail.expirationDate
            } else {
                fallbackDate
            }
            
            inventoryDetailDao.update(
                detail.copy(
                    wasteDate = wasteDate,
                    updatedDate = fallbackDate
                )
            )
            
            fixedCount++
            android.util.Log.d("WasteReport", "  - 修复物品 ${detail.itemId}，设置wasteDate为 $wasteDate")
        }
        
        android.util.Log.d("WasteReport", "修复完成，共修复 $fixedCount 个物品")
        
        return fixedCount
        }

        // ==================== 心愿单相关方法 ====================


    // ========================================
    // 价格记录管理
    // ========================================
    
    /**
     * 添加价格记录
     */
    suspend fun addPriceRecord(record: PriceRecord): Long {
        return priceRecordDao.insert(record)
    }
    
    /**
     * 删除价格记录
     */
    suspend fun deletePriceRecord(record: PriceRecord) {
        priceRecordDao.delete(record)
    }
    
    /**
     * 根据ID删除价格记录
     */
    suspend fun deletePriceRecordById(recordId: Long) {
        priceRecordDao.deleteById(recordId)
    }
    
    /**
     * 获取物品的所有价格记录（按日期倒序）
     */
    fun getPriceRecords(itemId: Long): Flow<List<PriceRecord>> {
        return priceRecordDao.getPriceRecords(itemId)
    }
    
    /**
     * 获取价格记录列表（suspend，直接返回）
     */
    suspend fun getPriceRecordsByItemId(itemId: Long): List<PriceRecord> {
        return priceRecordDao.getPriceRecordsListByItemId(itemId)
    }
    
    /**
     * 获取某时间段内的价格记录
     */
    fun getPriceRecordsInRange(itemId: Long, startDate: Date, endDate: Date): Flow<List<PriceRecord>> {
        return priceRecordDao.getPriceRecordsInRange(itemId, startDate, endDate)
    }
    
    /**
     * 获取价格统计信息
     */
    suspend fun getPriceStatistics(itemId: Long): PriceStatistics {
        val maxPrice = priceRecordDao.getMaxPrice(itemId) ?: 0.0
        val minPrice = priceRecordDao.getMinPrice(itemId) ?: 0.0
        val avgPrice = priceRecordDao.getAvgPrice(itemId) ?: 0.0
        val count = priceRecordDao.getRecordCount(itemId)
        
        return PriceStatistics(
            maxPrice = maxPrice,
            minPrice = minPrice,
            avgPrice = avgPrice,
            recordCount = count
        )
    }
    
    /**
     * 获取各渠道的最新价格
     */
    suspend fun getLatestPricesByChannel(itemId: Long): List<PriceRecord> {
        return priceRecordDao.getLatestPricesByChannel(itemId)
    }
    
    /**
     * 将已存在的物品添加到购物清单
     * 用于从库存物品添加到购物清单的场景
     */
    suspend fun addShoppingItemToExistingItem(
        itemId: Long,
        shoppingDetail: ShoppingDetailEntity
    ) {
        appDatabase.withTransaction {
            android.util.Log.d("UnifiedItemRepository", "🛒 开始添加购物详情: itemId=$itemId, listId=${shoppingDetail.shoppingListId}")
            
            // 1. 插入购物详情
            shoppingDetailDao.insert(shoppingDetail)
            android.util.Log.d("UnifiedItemRepository", "✓ 购物详情已插入")
            
            // 2. 创建购物状态
            val shoppingState = ItemStateEntity(
                itemId = itemId,
                stateType = ItemStateType.SHOPPING,
                contextId = shoppingDetail.shoppingListId,
                isActive = true,
                createdDate = java.util.Date(),
                activatedDate = java.util.Date()
            )
            itemStateDao.insert(shoppingState)
            android.util.Log.d("UnifiedItemRepository", "✓ 购物状态已创建")
            
            android.util.Log.d("UnifiedItemRepository", "✅ 添加到购物清单完成")
        }
    }
    
    // ==================== 导出功能相关方法 ====================
    
    /**
     * 获取所有位置（同步版本，用于导出）
     */
    suspend fun getAllLocationsSync(): List<LocationEntity> {
        return locationDao.getAllLocationsSync()
    }
    
    /**
     * 获取所有购物详情
     */
    fun getAllShoppingDetails(): Flow<List<ShoppingDetailEntity>> {
        return shoppingDetailDao.getAllPendingItems()
    }
    
    /**
     * 获取购物物品及统一物品信息（用于导出）
     */
    suspend fun getAllShoppingItemsWithUnifiedItem(): List<Pair<ShoppingDetailEntity, UnifiedItemEntity>> {
        return try {
            val shoppingDetails = shoppingDetailDao.getAllPendingItems().first()
            shoppingDetails.mapNotNull { detail ->
                unifiedItemDao.getById(detail.itemId)?.let { unifiedItem ->
                    detail to unifiedItem
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("UnifiedItemRepository", "获取购物清单物品失败", e)
            emptyList()
        }
    }
    
    // ==================== 日历事件操作 ====================
    
    /**
     * 添加日历事件
     */
    suspend fun addCalendarEvent(event: com.example.itemmanagement.data.entity.CalendarEventEntity): Long {
        return appDatabase.calendarEventDao().insertEvent(event)
    }
    
    /**
     * 删除物品的所有日历事件
     */
    suspend fun deleteCalendarEventsByItemId(itemId: Long) {
        appDatabase.calendarEventDao().deleteEventsByItem(itemId)
    }
}

/**
 * 价格统计信息
 */
data class PriceStatistics(
    val maxPrice: Double,
    val minPrice: Double,
    val avgPrice: Double,
    val recordCount: Int
)
