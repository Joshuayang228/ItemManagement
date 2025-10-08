package com.example.itemmanagement.ui.shopping

import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.data.entity.unified.InventoryDetailEntity
import com.example.itemmanagement.data.model.ItemStatus
import com.example.itemmanagement.data.model.OpenStatus
import com.example.itemmanagement.ui.base.BaseItemViewModel
import com.example.itemmanagement.ui.base.ItemStateCacheViewModel
import com.example.itemmanagement.ui.add.Field
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 购物清单转入库存的ViewModel
 * 
 * 核心功能：实现"假转存"逻辑
 * - UnifiedItemEntity 保持不变（物品基础信息不变）
 * - ShoppingDetailEntity → InventoryDetailEntity（详情表转换）
 * - ItemStateEntity: SHOPPING → INVENTORY（状态标记转换）
 * 
 * 设计理念：
 * - 使用专用的 TransferToInventoryCache，与 AddItemCache 完全隔离
 * - 半屏和全屏共享同一个 ViewModel 实例（通过 activityViewModels）
 * - 数据自动同步，无需手动传递
 */
class TransferToInventoryViewModel(
    repository: UnifiedItemRepository,
    cacheViewModel: ItemStateCacheViewModel,
    private val sourceItemId: Long
) : BaseItemViewModel(repository, cacheViewModel) {

    private var shoppingListId: Long = 0L

    init {
        android.util.Log.d("TransferToInventory", "=== 初始化 TransferToInventoryViewModel ===")
        android.util.Log.d("TransferToInventory", "源物品ID: $sourceItemId")
        
        // 初始化字段属性
        initializeAllFieldProperties()
        
        // ⭐ 修复：始终从数据库加载最新数据，不依赖缓存
        // 这样可以确保每次打开都显示最新的购物物品信息
        if (sourceItemId > 0) {
            android.util.Log.d("TransferToInventory", "从数据库加载购物物品")
            loadFromShoppingItem()
        }
    }

    // --- 实现抽象方法 ---

    override fun getCurrentCache(): Any {
        return cacheViewModel.getTransferToInventoryCache(sourceItemId)
    }

    override fun getCacheKey(): String {
        return "TRANSFER_TO_INVENTORY_$sourceItemId"
    }

    override fun loadDataFromCache() {
        android.util.Log.d("TransferToInventory", "开始从缓存加载数据")
        val cache = cacheViewModel.getTransferToInventoryCache(sourceItemId)
        
        fieldValues = cache.fieldValues.toMutableMap()
        _selectedFields.value = cache.selectedFields
        _photoUris.value = cache.photoUris
        _selectedTags.value = cache.selectedTags
        customOptionsMap = cache.customOptions.toMutableMap()
        customUnitsMap = cache.customUnits.toMutableMap()
        customTagsMap = cache.customTags.toMutableMap()
        
        android.util.Log.d("TransferToInventory", "缓存加载完成，字段数: ${fieldValues.size}")
    }

    override fun saveDataToCache() {
        android.util.Log.d("TransferToInventory", "保存数据到缓存")
        val cache = cacheViewModel.getTransferToInventoryCache(sourceItemId)
        
        cache.fieldValues = fieldValues.toMutableMap()
        cache.selectedFields = _selectedFields.value ?: setOf()
        cache.photoUris = _photoUris.value ?: emptyList()
        cache.selectedTags = _selectedTags.value ?: mapOf()
        cache.customOptions = customOptionsMap.toMutableMap()
        cache.customUnits = customUnitsMap.toMutableMap()
        cache.customTags = customTagsMap.toMutableMap()
        
        android.util.Log.d("TransferToInventory", "缓存保存完成")
    }

    override suspend fun saveOrUpdateItem() {
        android.util.Log.d("TransferToInventory", "========== 开始状态转换 ==========")
        android.util.Log.d("TransferToInventory", "源物品ID: $sourceItemId")
        
        try {
            // 1. 读取购物详情
            val shoppingDetail = repository.getShoppingDetailByItemId(sourceItemId)
            if (shoppingDetail == null) {
                android.util.Log.e("TransferToInventory", "未找到购物详情")
                _errorMessage.value = "未找到购物物品信息"
                _saveResult.value = false
                return
            }
            
            android.util.Log.d("TransferToInventory", "✓ 找到购物详情: listId=${shoppingDetail.shoppingListId}")
            
            // 2. 从字段值创建库存详情
            val inventoryDetail = createInventoryDetailFromFields(sourceItemId)
            android.util.Log.d("TransferToInventory", "✓ 创建库存详情: quantity=${inventoryDetail.quantity}")
            
            // 3. 执行状态转换（事务操作）
            repository.transferShoppingToInventory(
                itemId = sourceItemId,
                shoppingDetail = shoppingDetail,
                inventoryDetail = inventoryDetail
            )
            
            android.util.Log.d("TransferToInventory", "✓ 状态转换成功")
            android.util.Log.d("TransferToInventory", "========== 转换完成 ==========")
            
            // 4. 清除缓存
            cacheViewModel.clearTransferToInventoryCache(sourceItemId)
            
            _saveResult.value = true
            _errorMessage.value = "物品已成功入库"
            
        } catch (e: Exception) {
            android.util.Log.e("TransferToInventory", "状态转换失败", e)
            _errorMessage.value = "入库失败: ${e.message}"
            _saveResult.value = false
        }
    }

    // --- 业务逻辑方法 ---

    /**
     * 从购物清单物品加载数据
     */
    fun loadFromShoppingItem() {
        viewModelScope.launch {
            try {
                android.util.Log.d("TransferToInventory", "开始加载购物物品: itemId=$sourceItemId")
                
                // ⭐ 使用 Repository 的专用方法查询完整购物物品
                val item = repository.getCompleteShoppingItem(sourceItemId)
                
                if (item == null) {
                    android.util.Log.e("TransferToInventory", "未找到购物物品: itemId=$sourceItemId")
                    _errorMessage.value = "未找到购物物品"
                    return@launch
                }
                
                val shoppingDetail = item.shoppingDetail
                if (shoppingDetail == null) {
                    android.util.Log.e("TransferToInventory", "购物详情为空: itemId=$sourceItemId")
                    _errorMessage.value = "购物详情为空"
                    return@launch
                }
                
                // 保存购物清单ID，用于后续状态转换
                shoppingListId = shoppingDetail.shoppingListId
                
                android.util.Log.d("TransferToInventory", "找到购物物品: name=${item.name}, listId=$shoppingListId")
                
                // 2. 预填充字段值
                fieldValues["名称"] = item.name
                fieldValues["数量"] = shoppingDetail.quantity.toString()
                fieldValues["数量_unit"] = shoppingDetail.quantityUnit
                fieldValues["分类"] = item.category
                
                item.subCategory?.let { fieldValues["子分类"] = it }
                item.brand?.let { fieldValues["品牌"] = it }
                item.specification?.let { fieldValues["规格"] = it }
                item.customNote?.let { fieldValues["备注"] = it }
                
                // 价格信息（从购物详情转换）
                val price = shoppingDetail.actualPrice ?: shoppingDetail.estimatedPrice
                val priceUnit = if (shoppingDetail.actualPrice != null) {
                    shoppingDetail.actualPriceUnit
                } else {
                    shoppingDetail.estimatedPriceUnit
                }
                
                price?.let {
                    fieldValues["单价"] = it.toString()
                    priceUnit?.let { unit -> fieldValues["单价_unit"] = unit }
                }
                
                shoppingDetail.totalPrice?.let {
                    fieldValues["总价"] = it.toString()
                    shoppingDetail.totalPriceUnit?.let { unit -> fieldValues["总价_unit"] = unit }
                }
                
                // 购买日期
                shoppingDetail.purchaseDate?.let {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    fieldValues["购买日期"] = dateFormat.format(it)
                }
                
                // 其他字段
                item.season?.let { fieldValues["季节"] = it }
                item.capacity?.let { 
                    fieldValues["容量"] = it.toString()
                    item.capacityUnit?.let { unit -> fieldValues["容量_unit"] = unit }
                }
                item.rating?.let { fieldValues["评分"] = it.toString() }
                
                // 3. 加载照片
                if (item.photos.isNotEmpty()) {
                    android.util.Log.d("TransferToInventory", "加载 ${item.photos.size} 张照片")
                    _photoUris.value = item.photos.map { android.net.Uri.parse(it.uri) }
                }
                
                // 4. 加载标签
                if (item.tags.isNotEmpty()) {
                    android.util.Log.d("TransferToInventory", "加载 ${item.tags.size} 个标签")
                    val tagsByCategory = item.tags.groupBy { "默认" }.mapValues { entry ->
                        entry.value.map { it.name }.toSet()
                    }
                    _selectedTags.value = tagsByCategory
                }
                
                android.util.Log.d("TransferToInventory", "预填充完成，字段数: ${fieldValues.size}")
                
                // 5. 触发UI更新
                _selectedFields.value = _selectedFields.value
                
                // 6. 保存到缓存
                saveToCache()
                
            } catch (e: Exception) {
                android.util.Log.e("TransferToInventory", "加载购物物品失败", e)
                _errorMessage.value = "加载失败: ${e.message}"
            }
        }
    }

    /**
     * 初始化所有字段属性
     */
    fun initializeAllFieldProperties() {
        android.util.Log.d("TransferToInventory", "初始化字段属性")
        // 调用父类的默认字段属性初始化
        initializeDefaultFieldProperties()
    }

    /**
     * 从字段值创建库存详情
     */
    private fun createInventoryDetailFromFields(itemId: Long): InventoryDetailEntity {
        val currentDate = Date()
        
        return InventoryDetailEntity(
            itemId = itemId,
            locationId = getFieldValueAsLong("位置") ?: 1L, // 默认位置
            quantity = getFieldValueAsDouble("数量") ?: 1.0,
            unit = getFieldValueAsString("数量_unit") ?: "个",
            status = ItemStatus.IN_STOCK,
            price = getFieldValueAsDouble("单价"),
            priceUnit = getFieldValueAsString("单价_unit") ?: "元",
            totalPrice = getFieldValueAsDouble("总价"),
            totalPriceUnit = getFieldValueAsString("总价_unit") ?: "元",
            purchaseDate = getFieldValueAsDate("购买日期") ?: currentDate,
            productionDate = getFieldValueAsDate("生产日期"),
            expirationDate = getFieldValueAsDate("保质过期时间"),
            purchaseChannel = getFieldValueAsString("购买渠道"),
            storeName = getFieldValueAsString("商家名称"),
            stockWarningThreshold = getFieldValueAsInt("库存预警阈值"),
            shelfLife = getFieldValueAsInt("保质期"),
            // 保修信息已移至 WarrantyEntity
            // warrantyPeriod = getFieldValueAsInt("保修期"),
            // warrantyEndDate = getFieldValueAsDate("保修到期时间"),
            isHighTurnover = false,
            openStatus = getFieldValueAsOpenStatus("开封状态"),
            openDate = null,
            wasteDate = null,
            createdDate = currentDate,
            updatedDate = currentDate
        )
    }

    // --- 辅助方法：获取字段值 ---

    private fun getFieldValueAsString(fieldName: String): String? {
        return (fieldValues[fieldName] as? String)?.takeIf { it.isNotBlank() }
    }

    private fun getFieldValueAsDouble(fieldName: String): Double? {
        val value = fieldValues[fieldName]
        return when (value) {
            is String -> value.toDoubleOrNull()
            is Number -> value.toDouble()
            else -> null
        }
    }

    private fun getFieldValueAsInt(fieldName: String): Int? {
        val value = fieldValues[fieldName]
        return when (value) {
            is String -> value.toIntOrNull()
            is Number -> value.toInt()
            else -> null
        }
    }

    private fun getFieldValueAsLong(fieldName: String): Long? {
        val value = fieldValues[fieldName]
        return when (value) {
            is String -> value.toLongOrNull()
            is Number -> value.toLong()
            else -> null
        }
    }

    private fun getFieldValueAsBoolean(fieldName: String): Boolean? {
        val value = fieldValues[fieldName]
        return when (value) {
            is Boolean -> value
            is String -> value.toBoolean()
            else -> null
        }
    }

    private fun getFieldValueAsDate(fieldName: String): Date? {
        val value = fieldValues[fieldName]
        return when (value) {
            is Date -> value
            is String -> {
                try {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    dateFormat.parse(value)
                } catch (e: Exception) {
                    android.util.Log.w("TransferToInventory", "日期解析失败: $value", e)
                    null
                }
            }
            else -> null
        }
    }

    private fun getFieldValueAsOpenStatus(fieldName: String): OpenStatus? {
        val value = fieldValues[fieldName] as? String
        return when (value) {
            "已开封" -> OpenStatus.OPENED
            "未开封" -> OpenStatus.UNOPENED
            else -> null
        }
    }
}
