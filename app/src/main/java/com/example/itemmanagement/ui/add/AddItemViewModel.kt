package com.example.itemmanagement.ui.add

import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.repository.WarrantyRepository
import com.example.itemmanagement.data.entity.LocationEntity
import com.example.itemmanagement.data.entity.PhotoEntity
import com.example.itemmanagement.data.entity.TagEntity
import com.example.itemmanagement.data.entity.WarrantyEntity
import com.example.itemmanagement.data.entity.WarrantyStatus
import com.example.itemmanagement.data.mapper.toItemEntity
import com.example.itemmanagement.data.mapper.toLocationEntity
import com.example.itemmanagement.data.mapper.toItem
import com.example.itemmanagement.data.model.Item
import com.example.itemmanagement.ui.common.FieldProperties
import com.example.itemmanagement.ui.common.ValidationType  
import com.example.itemmanagement.ui.common.DisplayStyle
import com.example.itemmanagement.ui.base.BaseItemViewModel
import com.example.itemmanagement.ui.base.ItemStateCacheViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

/**
 * 添加物品 ViewModel
 * 
 * 继承自 BaseItemViewModel，专门处理添加新物品的业务逻辑。
 * 使用独立的缓存空间，确保与编辑模式的数据完全隔离。
 */
class AddItemViewModel(
    repository: ItemRepository,
    cacheViewModel: ItemStateCacheViewModel,
    private val warrantyRepository: WarrantyRepository? = null  // 保修仓库（可选，保持向后兼容）
) : BaseItemViewModel(repository, cacheViewModel) {

    init {
        Log.d("AddItemViewModel", "=== 初始化添加ViewModel ===")
        
        // 初始化字段属性
        initializeAllFieldProperties()
        
        // 尝试从缓存恢复数据，如果有的话
        if (cacheViewModel.hasAddItemCache()) {
            Log.d("AddItemViewModel", "发现添加缓存，从缓存恢复数据")
            loadFromCache()
        } else {
            Log.d("AddItemViewModel", "没有添加缓存，初始化默认字段")
            // 如果没有缓存，初始化默认字段
            initializeDefaultFields()
        }
        
        Log.d("AddItemViewModel", "添加ViewModel初始化完成，当前fieldValues: $fieldValues")
    }

    // --- 实现抽象方法 ---

    override fun getCurrentCache(): Any {
        return cacheViewModel.getAddItemCache()
    }

    override fun getCacheKey(): String {
        return "ADD_ITEM"
    }

    override fun loadDataFromCache() {
        Log.d("AddItemViewModel", "开始从缓存加载数据")
        val cache = cacheViewModel.getAddItemCache()
        Log.d("AddItemViewModel", "缓存内容: fieldValues=${cache.fieldValues}, selectedFields=${cache.selectedFields}")
        // 类型安全的缓存加载，不需要类型转换
        fieldValues = cache.fieldValues.toMutableMap()
        _selectedFields.value = cache.selectedFields
        _photoUris.value = cache.photoUris
        _selectedTags.value = cache.selectedTags
        customOptionsMap = cache.customOptions.toMutableMap()
        customUnitsMap = cache.customUnits.toMutableMap()
        customTagsMap = cache.customTags.toMutableMap()
        Log.d("AddItemViewModel", "缓存加载完成，当前fieldValues: $fieldValues")
    }

    override fun saveDataToCache() {
        val cache = cacheViewModel.getAddItemCache()
        // 类型安全的缓存保存，不需要类型转换
        cache.fieldValues = fieldValues.toMutableMap()
        cache.selectedFields = _selectedFields.value ?: setOf()
        cache.photoUris = _photoUris.value ?: emptyList()
        cache.selectedTags = _selectedTags.value ?: mapOf()
        cache.customOptions = customOptionsMap.toMutableMap()
        cache.customUnits = customUnitsMap.toMutableMap()
        cache.customTags = customTagsMap.toMutableMap()
    }

    override suspend fun saveOrUpdateItem() {
        Log.d("AddItemViewModel", "开始保存物品数据")
        
        // 详细记录所有字段值
        Log.d("AddItemViewModel", "=== 保存前字段值详情 ===")
        fieldValues.forEach { (key, value) ->
            Log.d("AddItemViewModel", "字段 '$key': $value (${value?.javaClass?.simpleName})")
        }
        
        // 验证数据
        val item = buildItemFromFields()
        Log.d("AddItemViewModel", "构建的Item对象: $item")
        
        val (isValid, errorMessage) = validateItem(item)
        
        if (!isValid) {
            Log.e("AddItemViewModel", "数据验证失败: $errorMessage")
            _errorMessage.value = errorMessage ?: "数据验证失败"
            _saveResult.value = false
            return
        }

        try {
            // 构建实体对象
            val itemEntity = item.toItemEntity()
            
            // 准备位置实体
            val locationEntity = item.location?.let { 
                if (it.area != "未指定") it.toLocationEntity() else null
            }
            
            // 准备照片实体
            val photoEntities = mutableListOf<PhotoEntity>()
            _photoUris.value?.forEachIndexed { index, uri ->
                photoEntities.add(
                    PhotoEntity(
                        itemId = 0, // 新增模式，itemId会在DAO中被更新
                        uri = uri.toString(),
                        isMain = index == 0,
                        displayOrder = index
                    )
                )
            }
            
            // 准备标签实体
            val tagEntities = mutableListOf<TagEntity>()
            item.tags.forEach { tag ->
                tagEntities.add(TagEntity(name = tag.name, color = tag.color))
            }
            
            // 保存到数据库
            val itemId = repository.insertItemWithDetails(itemEntity, locationEntity, photoEntities, tagEntities)
            
            if (itemId > 0) {
                // 物品保存成功，检查是否需要同步保修信息
                syncWarrantyInfoIfNeeded(itemId, item)
                
                _saveResult.value = true
                _errorMessage.value = "物品添加成功"
            } else {
                _errorMessage.value = "添加失败：数据库插入返回无效ID"
                _saveResult.value = false
            }
            
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "添加失败：未知错误"
            _saveResult.value = false
        }
    }

    // --- 私有辅助方法 ---

    /**
     * 从字段值构建Item对象
     */
    private fun buildItemFromFields(): Item {
        // 基础字段
        val name = (fieldValues["名称"] as? String)?.trim() ?: ""
        val quantityStr = (fieldValues["数量"] as? String)?.trim() ?: "1"
        val quantity = quantityStr.toDoubleOrNull() ?: 1.0
        
        // 位置信息构建
        val location = buildLocationFromFields()
        
        // 日期信息解析
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val productionDate = parseDate(fieldValues["生产日期"] as? String)
        val expirationDate = parseDate(fieldValues["保质过期时间"] as? String)
        val purchaseDate = parseDate(fieldValues["购买日期"] as? String)
        val addDate = parseDate(fieldValues["添加日期"] as? String) ?: Date()
        val warrantyEndDate = parseDate(fieldValues["保修到期时间"] as? String)
        val openDate = parseDate(fieldValues["开封时间"] as? String)
        
        // 开封状态
        val openStatus = when (fieldValues["开封状态"] as? String) {
            "已开封" -> com.example.itemmanagement.data.model.OpenStatus.OPENED
            "未开封" -> com.example.itemmanagement.data.model.OpenStatus.UNOPENED
            else -> null
        }
        
        // 标签信息
        val tags = buildTagsFromSelectedTags()
        
        // 获取数量单位
        val quantityUnit = fieldValues["数量_unit"] as? String ?: "个"
        
        // 获取布尔字段
        val isWishlistItem = when (fieldValues["加入心愿单"]) {
            is Boolean -> fieldValues["加入心愿单"] as Boolean
            is String -> (fieldValues["加入心愿单"] as String).toBoolean()
            else -> false
        }
        
        val isHighTurnover = when (fieldValues["高周转"]) {
            is Boolean -> fieldValues["高周转"] as Boolean
            is String -> (fieldValues["高周转"] as String).toBoolean()
            else -> false
        }
        
        // 获取评分 - 支持多种类型
        val rating = when (val ratingValue = fieldValues["评分"]) {
            is Float -> ratingValue.toDouble()
            is Double -> ratingValue
            is String -> ratingValue.toDoubleOrNull()
            else -> null
        }
        
        // 获取季节 - 期望是Set<String>
        val seasonSet = when (val seasonValue = fieldValues["季节"]) {
            is Set<*> -> seasonValue.mapNotNull { it as? String }.toSet()
            is Collection<*> -> seasonValue.mapNotNull { it as? String }.toSet()
            is String -> if (seasonValue.isNotEmpty()) setOf(seasonValue) else emptySet()
            else -> emptySet()
        }
        val seasonString = if (seasonSet.isNotEmpty()) seasonSet.joinToString(",") else null
        
        // 获取期限字段 - 处理Pair类型
        val shelfLife = when (val shelfLifeValue = fieldValues["保质期"]) {
            is Pair<*, *> -> (shelfLifeValue.first as? String)?.toIntOrNull()
            is String -> shelfLifeValue.toIntOrNull()
            else -> null
        }
        
        val warrantyPeriod = when (val warrantyValue = fieldValues["保修期"]) {
            is Pair<*, *> -> (warrantyValue.first as? String)?.toIntOrNull()
            is String -> warrantyValue.toIntOrNull()
            else -> null
        }
        
        // 获取单位字段
        val priceUnit = fieldValues["单价_unit"] as? String
        val totalPriceUnit = fieldValues["总价_unit"] as? String
        val capacityUnit = fieldValues["容量_unit"] as? String ?: fieldValues["容量单位"] as? String
        val shelfLifeUnit = when (val shelfLifeValue = fieldValues["保质期"]) {
            is Pair<*, *> -> shelfLifeValue.second as? String
            else -> fieldValues["保质期_unit"] as? String
        }
        val warrantyUnit = when (val warrantyValue = fieldValues["保修期"]) {
            is Pair<*, *> -> warrantyValue.second as? String
            else -> fieldValues["保修期_unit"] as? String
        }

        return Item(
            id = 0, // 新物品，ID为0
            name = name,
            quantity = quantity,
            unit = quantityUnit,
            location = location,
            category = fieldValues["分类"] as? String ?: "未指定",
            productionDate = productionDate,
            expirationDate = expirationDate,
            openStatus = openStatus,
            openDate = openDate,
            brand = fieldValues["品牌"] as? String,
            specification = fieldValues["规格"] as? String,
            stockWarningThreshold = (fieldValues["库存预警值"] as? String)?.toIntOrNull(),
            price = (fieldValues["单价"] as? String)?.toDoubleOrNull(),
            priceUnit = priceUnit,
            purchaseChannel = fieldValues["购买渠道"] as? String,
            storeName = fieldValues["商家名称"] as? String,
            subCategory = fieldValues["子分类"] as? String,
            customNote = fieldValues["备注"] as? String,
            season = seasonString,
            capacity = (fieldValues["容量"] as? String)?.toDoubleOrNull(),
            capacityUnit = capacityUnit,
            rating = rating,
            totalPrice = (fieldValues["总价"] as? String)?.toDoubleOrNull(),
            totalPriceUnit = totalPriceUnit,
            purchaseDate = purchaseDate,
            shelfLife = shelfLife,
            warrantyPeriod = warrantyPeriod,
            warrantyEndDate = warrantyEndDate,
            serialNumber = fieldValues["序列号"] as? String,
            addDate = addDate,
            isWishlistItem = isWishlistItem,
            isHighTurnover = isHighTurnover,
            tags = tags
        )
    }
    
    /**
     * 从字段构建位置信息
     */
    private fun buildLocationFromFields(): com.example.itemmanagement.data.model.Location? {
        val locationStr = fieldValues["位置"] as? String
        return if (!locationStr.isNullOrBlank() && locationStr != "未指定") {
            val parts = locationStr.split("-")
            when (parts.size) {
                1 -> com.example.itemmanagement.data.model.Location(area = parts[0], container = null, sublocation = null)
                2 -> com.example.itemmanagement.data.model.Location(area = parts[0], container = parts[1], sublocation = null)
                3 -> com.example.itemmanagement.data.model.Location(area = parts[0], container = parts[1], sublocation = parts[2])
                else -> null
            }
        } else {
            null
        }
    }
    
    /**
     * 解析日期字符串
     */
    private fun parseDate(dateStr: String?): Date? {
        if (dateStr.isNullOrBlank()) return null
        
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        )
        
        for (format in formats) {
            try {
                return format.parse(dateStr)
            } catch (e: Exception) {
                // 继续尝试下一个格式
            }
        }
        
        return null
    }
    
    /**
     * 从选中的标签构建标签列表
     */
    private fun buildTagsFromSelectedTags(): List<com.example.itemmanagement.data.model.Tag> {
        val tags = mutableListOf<com.example.itemmanagement.data.model.Tag>()
        
        _selectedTags.value?.forEach { (category, tagNames) ->
            tagNames.forEach { tagName ->
                tags.add(
                    com.example.itemmanagement.data.model.Tag(
                        name = tagName,
                        color = "#6200EE" // 默认颜色
                    )
                )
            }
        }
        
        return tags
    }

    /**
     * 验证Item对象
     */
    private fun validateItem(item: Item): Pair<Boolean, String?> {
        return when {
            item.name.isBlank() -> Pair(false, "物品名称不能为空")
            item.quantity <= 0.0 -> Pair(false, "数量必须大于0")
            else -> Pair(true, null)
        }
    }

    /**
     * 初始化默认字段
     */
    private fun initializeDefaultFields() {
        val defaultFields = listOf(
            Field("基础信息", "名称", true),
            Field("基础信息", "数量", true),
            Field("基础信息", "位置", true),
            Field("其他", "备注", true),
            Field("分类", "分类", true),
            Field("分类", "标签", true),  // 添加标签为默认选中
            Field("日期类", "添加日期", true),
            Field("基础信息", "加入心愿单", false),
            Field("基础信息", "高周转", false)
        )
        
        _selectedFields.value = defaultFields.toSet()
        
        // 为添加日期字段设置默认值为当前日期
        initializeDefaultDateValues()
        
        saveToCache() // 保存到缓存
    }
    
    /**
     * 初始化默认日期值
     */
    private fun initializeDefaultDateValues() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        
        // 设置添加日期的默认值为当前日期
        saveFieldValue("添加日期", currentDate)
    }

    /**
     * 初始化所有字段属性
     * 从原始AddItemViewModel复制完整的字段属性定义
     */
    private fun initializeAllFieldProperties() {
        // 调用父类的基础属性初始化
        super.initializeDefaultFieldProperties()
        
        // 添加更多字段属性定义...
        setFieldProperties("备注", FieldProperties(
            validationType = ValidationType.TEXT,
            hint = "请输入备注",
            isMultiline = true,
            maxLines = 5
        ))

        setFieldProperties("子分类", FieldProperties(
            options = emptyList(),
            isCustomizable = true
        ))

        setFieldProperties("标签", FieldProperties(
            displayStyle = DisplayStyle.TAG,
            isMultiSelect = true,
            isCustomizable = true,
            options = listOf("重要", "易碎", "易腐", "贵重", "常用")
        ))
        
        // 继续添加其他字段...
    }

    /**
     * 同步保修信息到独立的保修管理系统
     * 当用户在添加物品时填写了保修信息，自动创建对应的WarrantyEntity记录
     */
    private fun syncWarrantyInfoIfNeeded(itemId: Long, item: Item) {
        // 检查是否有WarrantyRepository依赖
        if (warrantyRepository == null) {
            Log.d("AddItemViewModel", "未提供WarrantyRepository，跳过保修信息同步")
            return
        }
        
        // 检查是否有保修相关信息需要同步
        val hasWarrantyPeriod = item.warrantyPeriod != null && item.warrantyPeriod > 0
        val hasWarrantyEndDate = item.warrantyEndDate != null
        val hasPurchaseDate = item.purchaseDate != null
        
        if (!hasWarrantyPeriod && !hasWarrantyEndDate) {
            Log.d("AddItemViewModel", "没有保修信息，跳过同步")
            return
        }
        
        viewModelScope.launch {
            try {
                // 构建保修实体
                val warrantyEntity = WarrantyEntity(
                    itemId = itemId,
                    purchaseDate = item.purchaseDate ?: item.addDate, // 如果没有购买日期，使用添加日期
                    warrantyPeriodMonths = convertWarrantyPeriodToMonths(item.warrantyPeriod),
                    warrantyEndDate = item.warrantyEndDate ?: calculateWarrantyEndDate(
                        item.purchaseDate ?: item.addDate,
                        convertWarrantyPeriodToMonths(item.warrantyPeriod)
                    ),
                    receiptImageUris = null, // 简单保修信息不包含图片
                    notes = "从添加物品界面自动同步",
                    status = if (item.warrantyEndDate?.before(Date()) == true) WarrantyStatus.EXPIRED else WarrantyStatus.ACTIVE,
                    warrantyProvider = null, // 简单保修信息不包含服务商
                    contactInfo = null,
                    createdDate = Date(),
                    updatedDate = Date()
                )
                
                // 保存到保修系统
                val warrantyId = warrantyRepository.insertWarranty(warrantyEntity)
                Log.d("AddItemViewModel", "保修信息同步成功，WarrantyId: $warrantyId")
                
            } catch (e: Exception) {
                Log.w("AddItemViewModel", "保修信息同步失败，但不影响物品添加", e)
                // 不影响主流程，仅记录警告
            }
        }
    }
    
    /**
     * 将保修期转换为月数
     * 支持从原有的保修期字段转换
     */
    private fun convertWarrantyPeriodToMonths(warrantyPeriod: Int?): Int {
        if (warrantyPeriod == null || warrantyPeriod <= 0) return 12 // 默认12个月
        
        // 获取保修期单位
        val warrantyUnit = when (val warrantyValue = fieldValues["保修期"]) {
            is Pair<*, *> -> warrantyValue.second as? String
            else -> fieldValues["保修期_unit"] as? String
        } ?: "月"
        
        return when (warrantyUnit) {
            "年" -> warrantyPeriod * 12
            "月" -> warrantyPeriod
            "日" -> maxOf(1, warrantyPeriod / 30) // 至少1个月
            else -> warrantyPeriod // 默认当作月处理
        }
    }
    
    /**
     * 根据购买日期和保修期计算保修到期日期
     */
    private fun calculateWarrantyEndDate(purchaseDate: Date, warrantyMonths: Int): Date {
        val calendar = Calendar.getInstance().apply {
            time = purchaseDate
            add(Calendar.MONTH, warrantyMonths)
        }
        return calendar.time
    }

    /**
     * 从购物清单项目预填充表单数据
     */
    fun prepareFormFromShoppingItem(shoppingItemEntity: com.example.itemmanagement.data.entity.ShoppingItemEntity) {
        viewModelScope.launch {
            try {
                // 基础信息填充
                saveFieldValue("名称", shoppingItemEntity.name)
                saveFieldValue("数量", shoppingItemEntity.quantity)
                saveFieldValue("分类", shoppingItemEntity.category)
                
                // 其他字段的填充逻辑...
                shoppingItemEntity.brand?.let { saveFieldValue("品牌", it) }
                shoppingItemEntity.specification?.let { saveFieldValue("规格", it) }
                
                // 设置当前日期为添加日期
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                saveFieldValue("添加日期", dateFormat.format(Date()))
                
            } catch (e: Exception) {
                _errorMessage.value = "预填充数据失败: ${e.message}"
            }
        }
    }
} 