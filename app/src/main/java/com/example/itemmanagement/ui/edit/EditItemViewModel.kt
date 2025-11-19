package com.example.itemmanagement.ui.edit

import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.data.repository.WarrantyRepository
import com.example.itemmanagement.data.entity.WarrantyEntity
import com.example.itemmanagement.data.entity.WarrantyStatus
import com.example.itemmanagement.data.relation.ItemWithDetails
import com.example.itemmanagement.data.model.Item
import com.example.itemmanagement.data.mapper.toItemEntity
import com.example.itemmanagement.data.mapper.toLocationEntity
import com.example.itemmanagement.data.mapper.toItem

import com.example.itemmanagement.ui.add.Field
import com.example.itemmanagement.ui.base.BaseItemViewModel
import com.example.itemmanagement.ui.base.ItemStateCacheViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import android.net.Uri

/**
 * 编辑物品 ViewModel
 * 
 * 继承自 BaseItemViewModel，专门处理编辑现有物品的业务逻辑。
 * 使用独立的缓存空间，确保与添加模式的数据完全隔离。
 */
class EditItemViewModel(
    repository: UnifiedItemRepository,
    cacheViewModel: ItemStateCacheViewModel,
    private val itemId: Long,
    private val warrantyRepository: WarrantyRepository? = null  // 保修仓库（可选）
) : BaseItemViewModel(repository, cacheViewModel) {

    // 保存原始物品数据，用于保持某些字段不变
    private var originalItem: Item? = null

    init {
        Log.d("EditItemViewModel", "=== 初始化编辑ViewModel，物品ID: $itemId ===")
        Log.d("EditItemViewModel", "📦 WarrantyRepository状态: ${if (warrantyRepository != null) "已注入✅" else "未注入❌"}")
        
        // 初始化字段属性
        initializeDefaultFieldProperties()
        
        // 尝试从缓存恢复数据，如果有的话
        if (cacheViewModel.hasEditItemCache(itemId)) {
            Log.d("EditItemViewModel", "发现编辑缓存，从缓存恢复数据")
            loadFromCache()
        } else {
            Log.d("EditItemViewModel", "没有编辑缓存，加载物品数据进行编辑")
            // 如果没有缓存，加载物品数据进行编辑
            loadItemForEdit()
        }
        
        Log.d("EditItemViewModel", "编辑ViewModel初始化完成，当前fieldValues: $fieldValues")
    }

    // --- 实现抽象方法 ---

    override fun getCurrentCache(): Any {
        return cacheViewModel.getEditItemCache(itemId)
    }

    override fun getCacheKey(): String {
        return "EDIT_ITEM_$itemId"
    }

    override fun loadDataFromCache() {
        val cache = cacheViewModel.getEditItemCache(itemId)
        // 类型安全的缓存加载，编辑模式专用
        fieldValues = cache.fieldValues.toMutableMap()
        _selectedFields.value = cache.selectedFields
        _photoUris.value = cache.photoUris
        _selectedTags.value = cache.selectedTags
        customOptionsMap = cache.customOptions.toMutableMap()
        customUnitsMap = cache.customUnits.toMutableMap()
        customTagsMap = cache.customTags.toMutableMap()
    }

    override fun saveDataToCache() {
        val cache = cacheViewModel.getEditItemCache(itemId)
        // 类型安全的缓存保存，编辑模式专用
        cache.fieldValues = fieldValues.toMutableMap()
        cache.selectedFields = _selectedFields.value ?: setOf()
        cache.photoUris = _photoUris.value ?: emptyList()
        cache.selectedTags = _selectedTags.value ?: mapOf()
        cache.customOptions = customOptionsMap.toMutableMap()
        cache.customUnits = customUnitsMap.toMutableMap()
        cache.customTags = customTagsMap.toMutableMap()
        cache.originalItemId = itemId // 编辑模式特有的字段
    }

    override suspend fun saveOrUpdateItem() {
        // 验证数据
        val validationResult = validateItem()
        if (!validationResult.isValid) {
            _errorMessage.value = validationResult.errorMessage
            _saveResult.value = false
            return
        }

        try {
            // 构建Item对象
            val item = buildItemFromFields()
            
            // 构建ItemEntity
            val itemEntity = item.toItemEntity()
            
            // 构建LocationEntity（如果有位置信息）
            val locationEntity = buildLocationFromFields()?.toLocationEntity()
            
            // 构建PhotoEntity列表
            val photoEntities = _photoUris.value?.map { uri ->
                com.example.itemmanagement.data.entity.PhotoEntity(
                    itemId = itemId,
                    uri = uri.toString(),
                    isMain = false
                )
            } ?: emptyList()
            
            // 构建TagEntity列表（需要先创建TagEntity，然后创建关联关系）
            val tagEntities = buildTagsFromSelectedTags().map { tag ->
                com.example.itemmanagement.data.entity.TagEntity(
                    name = tag.name,
                    color = tag.color
                )
            }
            
            // 构建InventoryDetail
            val inventoryDetail = buildInventoryDetailFromFields(itemId)
            
            // 更新物品及其关联数据
            val itemWithDetails = ItemWithDetails(
                unifiedItem = itemEntity.copy(id = itemId),
                inventoryDetail = inventoryDetail,
                photos = photoEntities,
                tags = tagEntities
            ).apply {
                // 设置位置信息
                this.locationEntity = locationEntity
            }
            
            Log.d("EditItemViewModel", "准备更新物品: itemId=$itemId, inventoryDetail=${inventoryDetail != null}, photos=${photoEntities.size}, tags=${tagEntities.size}, location=${locationEntity != null}")
            repository.updateItemWithDetails(itemWithDetails)
            
            // ✏️ 添加日历事件：记录编辑物品操作
            addCalendarEventForItemEdited(itemId, item.name, item.category)
            
            // 保存或更新保修信息（如果有）
            saveOrUpdateWarrantyInfo()
            
            // 清除缓存
            clearStateAndCache()
            
            _saveResult.value = true
            
        } catch (e: Exception) {
            _errorMessage.value = "保存失败: ${e.message}"
            _saveResult.value = false
        }
    }

    /**
     * 加载物品数据进行编辑
     */
    private fun loadItemForEdit() {
        viewModelScope.launch {
            try {
                // 从数据库加载真实的物品数据（包含位置、标签、照片等完整信息）
                Log.d("EditItemViewModel", "正在加载物品详细信息 ID: $itemId")
                val itemWithDetails = repository.getItemWithDetailsById(itemId)
                if (itemWithDetails != null) {
                    // 将ItemWithDetails转换为Item对象
                    val item = itemWithDetails.toItem()
                    Log.d("EditItemViewModel", "找到物品: ${item.name}, 数量: ${item.quantity}, 标签数: ${item.tags.size}, 照片数: ${item.photos.size}")
                    
                    // 加载保修信息
                    Log.d("EditItemViewModel", "🔍 开始加载保修信息...")
                    val warranty = if (warrantyRepository != null) {
                        try {
                            warrantyRepository.getWarrantyByItemId(itemId)
                        } catch (e: Exception) {
                            Log.e("EditItemViewModel", "❌ 加载保修信息失败", e)
                            null
                        }
                    } else {
                        Log.w("EditItemViewModel", "⚠️ WarrantyRepository未注入，无法加载保修信息")
                        null
                    }
                    
                    if (warranty != null) {
                        Log.d("EditItemViewModel", "✅ 找到保修信息: ${warranty.warrantyPeriodMonths}个月, 到期: ${warranty.warrantyEndDate}")
                    } else {
                        Log.d("EditItemViewModel", "ℹ️ 无保修信息")
                    }
                    
                    // 加载物品数据和保修信息到字段
                    loadItemData(item, warranty)
                    
                    // 加载照片到UI
                    if (item.photos.isNotEmpty()) {
                        val photoUris = item.photos.map { photo: com.example.itemmanagement.data.model.Photo -> android.net.Uri.parse(photo.uri) }
                        _photoUris.value = photoUris
                        Log.d("EditItemViewModel", "已加载 ${photoUris.size} 张照片")
                    }
                } else {
                    Log.e("EditItemViewModel", "找不到物品 ID: $itemId")
                    _errorMessage.value = "找不到要编辑的物品"
                    // 如果没有找到物品，则初始化基础字段
                    initializeEditModeFields()
                }
                
            } catch (e: Exception) {
                Log.e("EditItemViewModel", "加载物品数据失败", e)
                _errorMessage.value = "加载物品数据失败: ${e.message}"
                // 发生错误时，也初始化基础字段
                initializeEditModeFields()
            }
        }
    }

    /**
     * 初始化编辑模式的默认字段
     */
    private fun initializeEditModeFields() {
        val editModeFields = setOf(
            Field("基础信息", "名称", true, getEditModeOrder("名称")),
            Field("基础信息", "数量", true, getEditModeOrder("数量")),
            Field("基础信息", "位置", true, getEditModeOrder("位置")),
            Field("基础信息", "备注", true, getEditModeOrder("备注")),
            Field("分类", "分类", true, getEditModeOrder("分类")),
            Field("分类", "子分类", true, getEditModeOrder("子分类")),
            Field("日期类", "添加日期", true, getEditModeOrder("添加日期"))
        )
        
        // 设置字段选择状态
        _selectedFields.value = editModeFields.toSet()
    }

    /**
     * 清除状态和缓存
     */
    override fun clearStateAndCache() {
        super.clearStateAndCache()
        // 清除缓存
        cacheViewModel.clearEditItemCache(itemId)
    }
    
    /**
     * 将物品数据填充到表单字段中
     */
    private fun loadItemData(item: Item, warranty: WarrantyEntity? = null) {
        Log.d("EditItemViewModel", "开始加载物品数据: ${item.name}")
        Log.d("EditItemViewModel", "保修信息参数: ${if (warranty != null) "有" else "无"}")
        
        // 保存原始物品数据
        originalItem = item
        
        // 清除当前数据
        fieldValues.clear()
        
        // 根据物品数据动态设置需要显示的字段
        val fieldsToShow = mutableSetOf<Field>()
        
        // 填充基础信息
        saveFieldValue("名称", item.name)
        fieldsToShow.add(Field("基础信息", "名称", true, getEditModeOrder("名称")))
        Log.d("EditItemViewModel", "设置名称: ${item.name}")
        
        // 处理数量字段 - 只有当用户输入过数量时才加载
        if (item.isQuantityUserInput) {
        val quantityStr = if (item.quantity == item.quantity.toInt().toDouble()) {
            item.quantity.toInt().toString()
        } else {
            item.quantity.toString()
        }
        saveFieldValue("数量", quantityStr)
        fieldsToShow.add(Field("基础信息", "数量", true, getEditModeOrder("数量")))
        
            // 保存数量单位
        item.unit?.let { if (it.isNotBlank()) saveFieldValue("数量_unit", it) }
        }
        
        // 只有当分类不是"未指定"时才保存分类字段
        if (!item.category.isNullOrBlank() && item.category != "未指定") {
            saveFieldValue("分类", item.category)
            fieldsToShow.add(Field("分类", "分类", true, getEditModeOrder("分类")))
        }
        
        // 只有当位置区域不是"未指定"或空时才保存位置字段
        item.location?.let { location ->
            if (!location.area.isNullOrBlank() && location.area != "未指定") {
                saveFieldValue("位置_area", location.area)
                fieldsToShow.add(Field("基础信息", "位置", true, getEditModeOrder("位置")))
                location.container?.let { if (it.isNotBlank()) saveFieldValue("位置_container", it) }
                location.sublocation?.let { if (it.isNotBlank()) saveFieldValue("位置_sublocation", it) }
            }
        }
        
        // 日期类字段
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        item.productionDate?.let { 
            saveFieldValue("生产日期", dateFormat.format(it))
            fieldsToShow.add(Field("日期类", "生产日期", true, getEditModeOrder("生产日期")))
        }
        item.expirationDate?.let { 
            saveFieldValue("保质过期时间", dateFormat.format(it))
            fieldsToShow.add(Field("日期类", "保质过期时间", true, getEditModeOrder("保质过期时间")))
        }
        item.purchaseDate?.let { 
            saveFieldValue("购买日期", dateFormat.format(it))
            fieldsToShow.add(Field("日期类", "购买日期", true, getEditModeOrder("购买日期")))
        }
        
        // 价格相关字段
        item.price?.let { if (it > 0) {
            saveFieldValue("单价", it.toString())
            fieldsToShow.add(Field("数字类", "单价", true, getEditModeOrder("单价")))
            
            // 保存价格单位
            item.priceUnit?.let { unit ->
                if (unit.isNotBlank()) saveFieldValue("单价_unit", unit)
            }
        }}
        // 其他信息字段
        item.brand?.let { if (it.isNotBlank()) {
            saveFieldValue("品牌", it)
            fieldsToShow.add(Field("其他信息", "品牌", true, getEditModeOrder("品牌")))
        }}
        item.specification?.let { if (it.isNotBlank()) {
            saveFieldValue("规格", it)
            fieldsToShow.add(Field("其他信息", "规格", true, getEditModeOrder("规格")))
        }}
        item.customNote?.let { if (it.isNotBlank()) {
            saveFieldValue("备注", it)
            fieldsToShow.add(Field("基础信息", "备注", true, getEditModeOrder("备注")))
        }}
        
        // 评分字段 - 移除 > 0 的限制，因为评分可能是0
        item.rating?.let { 
            Log.d("EditItemViewModel", "处理评分字段: $it")
            saveFieldValue("评分", it.toString())
            fieldsToShow.add(Field("数字类", "评分", true, getEditModeOrder("评分")))
            Log.d("EditItemViewModel", "评分字段已保存到fieldValues: ${getFieldValue("评分")}")
        } ?: Log.d("EditItemViewModel", "评分字段为null")
        
        // 添加日期字段（总是显示）
        val addDateStr = dateFormat.format(item.addDate)
        saveFieldValue("添加日期", addDateStr)
        fieldsToShow.add(Field("日期类", "添加日期", true, getEditModeOrder("添加日期")))
        
        // 开封状态和开封日期
        item.openStatus?.let { status ->
            val statusStr = when (status) {
                com.example.itemmanagement.data.model.OpenStatus.OPENED -> "已开封"
                com.example.itemmanagement.data.model.OpenStatus.UNOPENED -> "未开封"
            }
            saveFieldValue("开封状态", statusStr)
            fieldsToShow.add(Field("基础信息", "开封状态", true, getEditModeOrder("开封状态")))
        }
        
        // 商业信息字段
        item.purchaseChannel?.let { if (it.isNotBlank() && it != "未指定") {
            saveFieldValue("购买渠道", it)
            fieldsToShow.add(Field("商业类", "购买渠道", true, getEditModeOrder("购买渠道")))
        }}
        
        item.storeName?.let { if (it.isNotBlank() && it != "未指定") {
            saveFieldValue("商家名称", it)
            fieldsToShow.add(Field("商业类", "商家名称", true, getEditModeOrder("商家名称")))
        }}
        
        item.serialNumber?.let { if (it.isNotBlank()) {
            saveFieldValue("序列号", it)
            fieldsToShow.add(Field("商业类", "序列号", true, getEditModeOrder("序列号")))
        }}
        
        // 子分类
        item.subCategory?.let { if (it.isNotBlank() && it != "未指定") {
            saveFieldValue("子分类", it)
            fieldsToShow.add(Field("分类", "子分类", true, getEditModeOrder("子分类")))
        }}
        
        // 季节
        item.season?.let { seasonString ->
            Log.d("EditItemViewModel", "处理季节字段: '$seasonString'")
            if (seasonString.isNotBlank() && seasonString != "未指定") {
                // 将数据库中的逗号分隔字符串转换为Set<String>
                val seasonSet = seasonString.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                saveFieldValue("季节", seasonSet)
                fieldsToShow.add(Field("分类", "季节", true, getEditModeOrder("季节")))
                Log.d("EditItemViewModel", "季节字段已转换并保存: 原始='$seasonString' -> Set=$seasonSet")
            } else {
                Log.d("EditItemViewModel", "季节字段被跳过，值为空或未指定")
            }
        } ?: Log.d("EditItemViewModel", "季节字段为null")
        
        // 容量
        item.capacity?.let { if (it > 0) {
            saveFieldValue("容量", it.toString())
            fieldsToShow.add(Field("数字类", "容量", true, getEditModeOrder("容量")))
            
            item.capacityUnit?.let { unit ->
                if (unit.isNotBlank()) saveFieldValue("容量_unit", unit)
            }
        }}
        
        // 总价
        item.totalPrice?.let { if (it > 0) {
            saveFieldValue("总价", it.toString())
            fieldsToShow.add(Field("数字类", "总价", true, getEditModeOrder("总价")))
            
            // 保存总价单位
            item.totalPriceUnit?.let { unit ->
                if (unit.isNotBlank()) saveFieldValue("总价_unit", unit)
            }
        }}
        
        // 保质期
        item.shelfLife?.let { if (it > 0) {
            val (value, unit) = convertDaysToAppropriateUnit(it)
            saveFieldValue("保质期", Pair(value, unit))
            fieldsToShow.add(Field("日期类", "保质期", true, getEditModeOrder("保质期")))
            Log.d("EditItemViewModel", "保质期已设置: $value $unit (原始天数: $it)")
        }}
        
        // 保修信息 - 从WarrantyEntity加载
        warranty?.let { 
            Log.d("EditItemViewModel", "🔧 处理保修信息: ${it.warrantyPeriodMonths}个月")
            
            // 保修期 - 转换为Pair格式
            if (it.warrantyPeriodMonths > 0) {
                // 判断使用哪个单位
                val (value, unit) = when {
                    it.warrantyPeriodMonths % 12 == 0 -> {
                        // 整年
                        Pair((it.warrantyPeriodMonths / 12).toString(), "年")
                    }
                    else -> {
                        // 月
                        Pair(it.warrantyPeriodMonths.toString(), "月")
                    }
                }
                saveFieldValue("保修期", Pair(value, unit))
                saveFieldValue("保修期_unit", unit)
                fieldsToShow.add(Field("日期类", "保修期", true, getEditModeOrder("保修期")))
                Log.d("EditItemViewModel", "✅ 保修期已设置: $value $unit")
            }
            
            // 保修到期时间
            saveFieldValue("保修到期时间", dateFormat.format(it.warrantyEndDate))
            fieldsToShow.add(Field("日期类", "保修到期时间", true, getEditModeOrder("保修到期时间")))
            Log.d("EditItemViewModel", "✅ 保修到期时间已设置: ${dateFormat.format(it.warrantyEndDate)}")
        } ?: run {
            Log.d("EditItemViewModel", "ℹ️ 无保修信息需要加载")
        }
        
        // 标签处理
        if (item.tags.isNotEmpty()) {
            val tagNames = item.tags.map { it.name }.toSet()
            saveFieldValue("标签", tagNames)
            fieldsToShow.add(Field("分类", "标签", true, getEditModeOrder("标签")))
        }
        
        // GPS地点字段处理
        item.locationAddress?.let { address ->
            if (address.isNotBlank()) {
                saveFieldValue("地点", address)
                fieldsToShow.add(Field("位置", "地点", true, getEditModeOrder("地点")))
                android.util.Log.d("EditItemViewModel", "📍 加载地点地址: $address")
                
                // 保存经纬度（隐藏字段）
                item.locationLatitude?.let { 
                    saveFieldValue("地点_纬度", it.toString())
                    android.util.Log.d("EditItemViewModel", "📍 加载地点纬度: $it")
                }
                item.locationLongitude?.let { 
                    saveFieldValue("地点_经度", it.toString())
                    android.util.Log.d("EditItemViewModel", "📍 加载地点经度: $it")
                }
            }
        }
        
        // 设置字段选择状态
        Log.d("EditItemViewModel", "设置字段选择状态，总共 ${fieldsToShow.size} 个字段")
        fieldsToShow.forEach { field ->
            updateFieldSelection(field, field.isSelected)
            Log.d("EditItemViewModel", "设置字段: ${field.name}")
        }
        
        Log.d("EditItemViewModel", "当前fieldValues内容: $fieldValues")
        
        // 加载照片数据
        if (item.photos.isNotEmpty()) {
            Log.d("EditItemViewModel", "加载 ${item.photos.size} 张照片")
            val photoUris = item.photos.map { photo ->
                Uri.parse(photo.uri)
            }
            _photoUris.value = photoUris
            Log.d("EditItemViewModel", "照片URI已设置: $photoUris")
        } else {
            Log.d("EditItemViewModel", "没有照片数据需要加载")
            _photoUris.value = emptyList()
        }
        
        // 保存更新后的数据到缓存
        saveToCache()
    }
    
    /**
     * 从字段值构建InventoryDetailEntity对象（编辑模式）
     */
    private suspend fun buildInventoryDetailFromFields(itemId: Long): com.example.itemmanagement.data.entity.unified.InventoryDetailEntity {
        Log.d("EditItemViewModel", "🔧 开始构建InventoryDetailEntity for itemId: $itemId")
        
        // 基础字段 - 处理数量
        val rawQuantityStr = getFieldValue("数量")?.toString()?.trim()
        val quantityStr = rawQuantityStr?.takeIf { it.isNotEmpty() }
        
        val quantity: Double
        val quantityUnit: String
        val isQuantityUserInput: Boolean
        
        if (quantityStr != null) {
            // 用户填写了数量
            quantity = quantityStr.toDoubleOrNull() ?: 1.0
            quantityUnit = getFieldValue("数量_unit")?.toString()?.takeIf { it.isNotBlank() } ?: "个"
            isQuantityUserInput = true
        } else {
            // 用户没有填写数量，使用默认值
            quantity = 1.0
            quantityUnit = "个"
            isQuantityUserInput = false
        }
        
        // 位置信息 - 暂时设为null，通过locationEntity传递
        val locationId: Long? = null
        
        // 日期字段
        val productionDate = parseDate(getFieldValue("生产日期")?.toString())
        val expirationDate = parseDate(getFieldValue("保质过期时间")?.toString())
        val purchaseDate = parseDate(getFieldValue("购买日期")?.toString())
        // 保修信息已移至 WarrantyEntity
        // val warrantyEndDate = parseDate(getFieldValue("保修到期时间")?.toString())
        
        // 开封状态
        val openStatus = when (getFieldValue("开封状态")?.toString()) {
            "已开封" -> com.example.itemmanagement.data.model.OpenStatus.OPENED
            "未开封" -> com.example.itemmanagement.data.model.OpenStatus.UNOPENED
            else -> null
        }
        
        // 价格信息
        val price = getFieldValue("单价")?.toString()?.toDoubleOrNull()
        val priceUnit = getFieldValue("单价_unit")?.toString() ?: "元"
        val totalPrice = getFieldValue("总价")?.toString()?.toDoubleOrNull()
        val totalPriceUnit = getFieldValue("总价_unit")?.toString() ?: "元"
        
        // 其他字段
        val purchaseChannel = getFieldValue("购买渠道")?.toString()
        val storeName = getFieldValue("商家名称")?.toString()
        val serialNumber = getFieldValue("序列号")?.toString()
        val capacity = getFieldValue("容量")?.toString()?.toDoubleOrNull()
        val capacityUnit = getFieldValue("容量_unit")?.toString() ?: "ml"
        val rating = getFieldValue("评分")?.toString()?.toDoubleOrNull()
        
        // 季节处理
        val season = when (val seasonValue = getFieldValue("季节")) {
            is Set<*> -> seasonValue.filterIsInstance<String>().joinToString(",")
            is String -> seasonValue
            else -> null
        }
        
        // 保质期和保修期处理
        val shelfLife = getFieldValue("保质期")?.let { value ->
            when (value) {
                is Pair<*, *> -> {
                    val (num, unit) = value
                    convertTodays(num.toString().toIntOrNull() ?: 0, unit.toString())
                }
                else -> null
            }
        }
        
        // 保修信息已移至 WarrantyEntity，不再从字段值读取
        
        return com.example.itemmanagement.data.entity.unified.InventoryDetailEntity(
            itemId = itemId,
            quantity = quantity,
            unit = quantityUnit,
            isQuantityUserInput = isQuantityUserInput,
            locationId = locationId,
            productionDate = productionDate,
            expirationDate = expirationDate,
            openStatus = openStatus,
            openDate = null,
            status = com.example.itemmanagement.data.model.ItemStatus.IN_STOCK,
            stockWarningThreshold = null,
            price = price,
            priceUnit = priceUnit,
            purchaseChannel = purchaseChannel,
            storeName = storeName,
            // 注意：capacity, rating, season, serialNumber 已移至 UnifiedItemEntity
            totalPrice = totalPrice,
            totalPriceUnit = totalPriceUnit,
            purchaseDate = purchaseDate,
            shelfLife = shelfLife,
            // 保修信息已移至 WarrantyEntity
            isHighTurnover = false,
            createdDate = Date(),
            updatedDate = Date()
        )
    }

    /**
     * 将数值和单位转换为天数
     */
    private fun convertTodays(value: Int, unit: String): Int {
        return when (unit) {
            "天" -> value
            "周" -> value * 7
            "月" -> value * 30
            "年" -> value * 365
            else -> value
        }
    }

    /**
     * 从字段构建Item对象（编辑模式版本）
     */
    private fun buildItemFromFields(): Item {
        return Item(
            id = itemId,
            name = getFieldValue("名称")?.toString() ?: "",
            quantity = (getFieldValue("数量")?.toString())?.toDoubleOrNull() ?: 0.0,
            unit = getFieldValue("数量_unit")?.toString() ?: "个",
            category = getFieldValue("分类")?.toString() ?: "未指定",
            subCategory = getFieldValue("子分类")?.toString(),
            brand = getFieldValue("品牌")?.toString(),
            specification = getFieldValue("规格")?.toString(),
            customNote = getFieldValue("备注")?.toString(),
            price = (getFieldValue("单价")?.toString())?.toDoubleOrNull(),
            priceUnit = getFieldValue("单价_unit")?.toString() ?: "元",
            rating = (getFieldValue("评分")?.toString())?.toDoubleOrNull().also {
                Log.d("EditItemViewModel", "buildItemFromFields - 评分: fieldValue=${getFieldValue("评分")}, parsed=$it")
            },
            productionDate = parseDate(getFieldValue("生产日期")?.toString()),
            expirationDate = parseDate(getFieldValue("保质过期时间")?.toString()),
            purchaseDate = parseDate(getFieldValue("购买日期")?.toString()),
            addDate = originalItem?.addDate ?: Date(), // 保持原添加日期
            location = buildLocationFromFields(),
            photos = convertUrisToPhotos(_photoUris.value ?: emptyList()),
            tags = buildTagsFromSelectedTags(),
            // 从字段值获取其他字段
            openStatus = getOpenStatusFromField(),
            openDate = null,
            status = com.example.itemmanagement.data.model.ItemStatus.IN_STOCK,
            stockWarningThreshold = (getFieldValue("库存预警")?.toString())?.toIntOrNull(),
            purchaseChannel = getFieldValue("购买渠道")?.toString(),
            storeName = getFieldValue("商家名称")?.toString(),
            season = when (val seasonValue = getFieldValue("季节")) {
                is Set<*> -> seasonValue.filterIsInstance<String>().joinToString(",")
                is Collection<*> -> seasonValue.filterIsInstance<String>().joinToString(",")
                is String -> seasonValue
                else -> null
            }.also {
                Log.d("EditItemViewModel", "buildItemFromFields - 季节: fieldValue=${getFieldValue("季节")}, result=$it")
            },
            capacity = (getFieldValue("容量")?.toString())?.toDoubleOrNull(),
            capacityUnit = getFieldValue("容量_unit")?.toString(),
            totalPrice = (getFieldValue("总价")?.toString())?.toDoubleOrNull(),
            totalPriceUnit = getFieldValue("总价_unit")?.toString() ?: "元",
            shelfLife = getShelfLifeFromField(),
            // 保修信息已移至 WarrantyEntity
            warrantyPeriod = null,
            warrantyEndDate = null,
            serialNumber = getFieldValue("序列号")?.toString(),
            locationAddress = getFieldValue("地点")?.toString().also {
                android.util.Log.d("EditItemViewModel", "📍 读取地点地址: $it")
            },
            locationLatitude = getFieldValue("地点_纬度")?.toString()?.toDoubleOrNull().also {
                android.util.Log.d("EditItemViewModel", "📍 读取地点纬度: $it")
            },
            locationLongitude = getFieldValue("地点_经度")?.toString()?.toDoubleOrNull().also {
                android.util.Log.d("EditItemViewModel", "📍 读取地点经度: $it")
            },
            isHighTurnover = false
        ).also {
            android.util.Log.d("EditItemViewModel", "📍 构建的Item - 地点: ${it.locationAddress}, 纬度: ${it.locationLatitude}, 经度: ${it.locationLongitude}")
        }
    }
    
    /**
     * 从字段获取开封状态
     */
    private fun getOpenStatusFromField(): com.example.itemmanagement.data.model.OpenStatus? {
        val statusStr = getFieldValue("开封状态")?.toString()
        return when (statusStr) {
            "已开封" -> com.example.itemmanagement.data.model.OpenStatus.OPENED
            "未开封" -> com.example.itemmanagement.data.model.OpenStatus.UNOPENED
            else -> null
        }
    }
    
    /**
     * 从字段获取保质期（天数）
     */
    private fun getShelfLifeFromField(): Int? {
        val shelfLifeValue = getFieldValue("保质期")
        return when (shelfLifeValue) {
            is Pair<*, *> -> (shelfLifeValue.first as? String)?.toIntOrNull()
            is String -> shelfLifeValue.toIntOrNull()
            else -> null
        }
    }
    
    /**
     * 从字段获取保修期（天数）
     */
    private fun getWarrantyPeriodFromField(): Int? {
        val warrantyValue = getFieldValue("保修期")
        return when (warrantyValue) {
            is Pair<*, *> -> (warrantyValue.first as? String)?.toIntOrNull()
            is String -> warrantyValue.toIntOrNull()
            else -> null
        }
    }
    
    /**
     * 验证物品数据
     */
    private fun validateItem(): ValidationResult {
        val name = getFieldValue("名称")?.toString()
        if (name.isNullOrBlank()) {
            return ValidationResult(false, "物品名称不能为空")
        }
        
        // 数量字段可以为空（会使用默认值1），但如果填写了则必须是有效数字
        val quantityStr = getFieldValue("数量")?.toString()?.trim()
        if (!quantityStr.isNullOrBlank()) {
        val quantity = quantityStr.toDoubleOrNull()
            if (quantity == null || quantity < 0) {
                return ValidationResult(false, "数量必须是有效的非负数")
            }
        }
        
        return ValidationResult(true, "")
    }
    
    /**
     * 解析日期字符串
     */
    private fun parseDate(dateStr: String?): Date? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 从选中的标签构建标签列表
     */
    private fun buildTagsFromSelectedTags(): List<com.example.itemmanagement.data.model.Tag> {
        val tags = mutableListOf<com.example.itemmanagement.data.model.Tag>()
        
        _selectedTags.value?.forEach { (fieldName, tagNames) ->
            tagNames.forEach { tagName ->
                tags.add(com.example.itemmanagement.data.model.Tag(
                    name = tagName
                ))
            }
        }
        
        return tags
    }
    
    /**
     * 从字段构建位置对象
     */
    private fun buildLocationFromFields(): com.example.itemmanagement.data.model.Location? {
        val area = getFieldValue("位置_area")?.toString()
        if (area.isNullOrBlank()) return null
        
        return com.example.itemmanagement.data.model.Location(
            area = area,
            container = getFieldValue("位置_container")?.toString(),
            sublocation = getFieldValue("位置_sublocation")?.toString()
        )
    }
    
    /**
     * 将URI列表转换为Photo列表
     */
    private fun convertUrisToPhotos(uris: List<Uri>): List<com.example.itemmanagement.data.model.Photo> {
        return uris.mapIndexed { index, uri ->
            com.example.itemmanagement.data.model.Photo(
                uri = uri.toString(),
                isMain = index == 0,
                displayOrder = index
            )
        }
    }
    
    /**
     * 保存或更新保修信息
     * ✅ 新架构：直接从fieldValues读取保修信息
     */
    private suspend fun saveOrUpdateWarrantyInfo() {
        Log.d("EditItemViewModel", "🔧 开始检查保修信息...")
        
        // 提取保修期
        val warrantyPeriod = when (val warrantyValue = getFieldValue("保修期")) {
            is Pair<*, *> -> (warrantyValue.first as? String)?.toIntOrNull()
            is String -> warrantyValue.toIntOrNull()
            else -> null
        }
        
        // 提取保修期单位
        val warrantyUnit = when (val warrantyValue = getFieldValue("保修期")) {
            is Pair<*, *> -> warrantyValue.second as? String
            else -> getFieldValue("保修期_unit") as? String
        } ?: "月"
        
        // 提取保修到期日期
        val warrantyEndDate = parseDate(getFieldValue("保修到期时间")?.toString())
        
        // 提取购买日期
        val purchaseDate = parseDate(getFieldValue("购买日期")?.toString()) ?: Date()
        
        Log.d("EditItemViewModel", "🔧 保修信息: period=$warrantyPeriod $warrantyUnit, endDate=$warrantyEndDate")
        
        // 检查是否有WarrantyRepository依赖
        if (warrantyRepository == null) {
            Log.w("EditItemViewModel", "⚠️ 未提供WarrantyRepository，无法保存保修信息")
            return
        }
        
        try {
            // 检查是否已存在保修记录
            val existingWarranty = warrantyRepository.getWarrantyByItemId(itemId)
            
            if (warrantyPeriod == null || warrantyPeriod <= 0) {
                // 如果保修期为空或无效，删除现有保修记录
                if (existingWarranty != null) {
                    warrantyRepository.deleteWarranty(existingWarranty)
                    Log.d("EditItemViewModel", "🗑️ 保修信息已删除")
                }
                return
            }
            
            // 转换保修期为月数
            val warrantyMonths = when (warrantyUnit) {
                "年" -> warrantyPeriod * 12
                "月" -> warrantyPeriod
                "日" -> maxOf(1, warrantyPeriod / 30)
                else -> warrantyPeriod
            }
            
            // 计算保修到期日期（如果没有手动设置）
            val calculatedEndDate = warrantyEndDate ?: run {
                val calendar = Calendar.getInstance().apply {
                    time = purchaseDate
                    add(Calendar.MONTH, warrantyMonths)
                }
                calendar.time
            }
            
            if (existingWarranty != null) {
                // 更新现有保修记录
                val updatedWarranty = existingWarranty.copy(
                    purchaseDate = purchaseDate,
                    warrantyPeriodMonths = warrantyMonths,
                    warrantyEndDate = calculatedEndDate,
                    status = if (calculatedEndDate.before(Date())) WarrantyStatus.EXPIRED else WarrantyStatus.ACTIVE,
                    updatedDate = Date()
                )
                warrantyRepository.updateWarranty(updatedWarranty)
                Log.d("EditItemViewModel", "✅ 保修信息更新成功: period=${warrantyMonths}月, endDate=$calculatedEndDate")
            } else {
                // 创建新保修记录
                val warrantyEntity = WarrantyEntity(
                    itemId = itemId,
                    purchaseDate = purchaseDate,
                    warrantyPeriodMonths = warrantyMonths,
                    warrantyEndDate = calculatedEndDate,
                    receiptImageUris = null,
                    notes = "从编辑物品界面创建",
                    status = if (calculatedEndDate.before(Date())) WarrantyStatus.EXPIRED else WarrantyStatus.ACTIVE,
                    warrantyProvider = null,
                    contactInfo = null,
                    createdDate = Date(),
                    updatedDate = Date()
                )
                warrantyRepository.insertWarranty(warrantyEntity)
                Log.d("EditItemViewModel", "✅ 保修信息创建成功: period=${warrantyMonths}月, endDate=$calculatedEndDate")
            }
            
        } catch (e: Exception) {
            Log.e("EditItemViewModel", "❌ 保修信息保存失败，但不影响物品更新", e)
            // 不影响主流程，仅记录错误
        }
    }
    
    /**
     * 获取编辑模式的字段顺序
     * 现在与添加模式保持完全一致，确保用户体验统一
     */
    private fun getEditModeOrder(name: String): Int = when(name) {
        "名称" -> 1
        "数量" -> 2
        "位置" -> 3
        "备注" -> 4
        "分类" -> 5
        "子分类" -> 6
        "标签" -> 7
        "季节" -> 8
        "容量" -> 9
        "评分" -> 10
        "单价" -> 11
        "总价" -> 12
        "添加日期" -> 13
        "购买日期" -> 15
        "生产日期" -> 16
        "保质期" -> 17
        "保质过期时间" -> 18
        "保修期" -> 19
        "保修到期时间" -> 20
        "品牌" -> 21
        "开封状态" -> 22
        "购买渠道" -> 23
        "商家名称" -> 24
        "序列号" -> 25
        // 旧编辑模式特有字段保持向后兼容
        "规格" -> 28
        else -> Int.MAX_VALUE
    }

    /**
     * 验证结果数据类
     */
    private data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String
    )
    
    /**
     * 将天数转换为合适的单位（年、月、日）
     */
    private fun convertDaysToAppropriateUnit(days: Int): Pair<String, String> {
        return when {
            days >= 365 && days % 365 == 0 -> {
                val years = days / 365
                Pair(years.toString(), "年")
            }
            days >= 30 && days % 30 == 0 -> {
                val months = days / 30
                Pair(months.toString(), "月")
            }
            days >= 365 -> {
                // 如果超过一年但不是整年，优先用月
                val months = (days / 30.0).toInt()
                if (months > 0) {
                    Pair(months.toString(), "月")
                } else {
                    Pair(days.toString(), "日")
                }
            }
            days >= 30 -> {
                // 如果超过一个月但不是整月，优先用月
                val months = (days / 30.0).toInt()
                if (months > 0) {
                    Pair(months.toString(), "月")
                } else {
                    Pair(days.toString(), "日")
                }
            }
            else -> {
                Pair(days.toString(), "日")
            }
        }
    }
    
    /**
     * ✏️ 添加日历事件：记录编辑物品操作
     */
    private suspend fun addCalendarEventForItemEdited(itemId: Long, itemName: String, category: String) {
        try {
            val event = com.example.itemmanagement.data.entity.CalendarEventEntity(
                itemId = itemId,
                eventType = com.example.itemmanagement.data.model.EventType.ITEM_EDITED,
                title = "编辑物品：$itemName",
                description = "分类：$category",
                eventDate = java.util.Date(),
                reminderDays = emptyList(), // 操作记录不需要提醒
                priority = com.example.itemmanagement.data.model.Priority.LOW,
                isCompleted = true, // 操作记录默认为已完成
                recurrenceType = null
            )
            repository.addCalendarEvent(event)
            android.util.Log.d("EditItemViewModel", "📅 已添加日历事件：编辑物品 - $itemName")
        } catch (e: Exception) {
            android.util.Log.e("EditItemViewModel", "添加日历事件失败", e)
        }
    }
}
