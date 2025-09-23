package com.example.itemmanagement.ui.shopping

import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.entity.ShoppingItemEntity
import com.example.itemmanagement.data.entity.ShoppingItemPriority
import com.example.itemmanagement.data.entity.UrgencyLevel
import com.example.itemmanagement.ui.add.Field
import com.example.itemmanagement.ui.common.FieldProperties
import com.example.itemmanagement.ui.common.ValidationType  
import com.example.itemmanagement.ui.common.DisplayStyle
import com.example.itemmanagement.ui.base.BaseItemViewModel
import com.example.itemmanagement.ui.base.ItemStateCacheViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 添加购物清单物品 ViewModel
 * 
 * 继承自 BaseItemViewModel，专门处理添加购物清单物品的业务逻辑。
 * 使用独立的缓存空间，确保与普通物品添加的数据完全隔离。
 */
class AddShoppingItemViewModel(
    repository: ItemRepository,
    cacheViewModel: ItemStateCacheViewModel,
    private val listId: Long
) : BaseItemViewModel(repository, cacheViewModel) {

    init {
        // 初始化购物清单专用字段属性
        initializeShoppingFieldProperties()
        
        // 尝试从购物清单专用缓存恢复数据，如果有的话
        if (cacheViewModel.hasShoppingItemCache(listId)) {
            loadFromCache()
        } else {
            // 如果没有缓存，初始化默认字段
            initializeDefaultShoppingFields()
        }
    }

    // --- 实现抽象方法 ---

    override fun getCurrentCache(): Any {
        // 购物清单物品使用独立的缓存空间，避免与添加物品混淆
        return cacheViewModel.getShoppingItemCache(listId)
    }

    override fun getCacheKey(): String {
        return "ADD_SHOPPING_ITEM_LIST_$listId"
    }

    override fun loadDataFromCache() {
        // 使用购物清单专用的独立缓存，彻底避免数据污染
        val cache = cacheViewModel.getShoppingItemCache(listId)
        // 类型安全的缓存加载，购物清单模式
        fieldValues = cache.fieldValues.toMutableMap()
        _selectedFields.value = cache.selectedFields
        _photoUris.value = cache.photoUris
        _selectedTags.value = cache.selectedTags
        customOptionsMap = cache.customOptions.toMutableMap()
        customUnitsMap = cache.customUnits.toMutableMap()
        customTagsMap = cache.customTags.toMutableMap()
    }

    override fun saveDataToCache() {
        // 使用购物清单专用的独立缓存，不会影响添加物品缓存
        val cache = cacheViewModel.getShoppingItemCache(listId)
        // 类型安全的缓存保存，购物清单模式
        cache.fieldValues = fieldValues.toMutableMap()
        cache.selectedFields = _selectedFields.value ?: setOf()
        cache.photoUris = _photoUris.value ?: emptyList()
        cache.selectedTags = _selectedTags.value ?: mapOf()
        cache.customOptions = customOptionsMap.toMutableMap()
        cache.customUnits = customUnitsMap.toMutableMap()
        cache.customTags = customTagsMap.toMutableMap()
        
        // 购物清单专用字段
        cache.shoppingListId = listId
        // 可以添加购物清单特有的优先级等信息
        cache.priority = fieldValues["优先级"] as? String
        cache.urgencyLevel = fieldValues["紧急程度"] as? String
    }

    override suspend fun saveOrUpdateItem() {
        // 验证数据
        val shoppingItem = buildShoppingItemFromFields()
        val (isValid, errorMessage) = validateShoppingItem(shoppingItem)
        
        if (!isValid) {
            _errorMessage.value = errorMessage ?: "数据验证失败"
            _saveResult.value = false
            return
        }

        try {
            // 保存到数据库
            val itemId = repository.insertShoppingItemSimple(shoppingItem)
            
            if (itemId > 0) {
                _saveResult.value = true
                _errorMessage.value = "购物清单物品添加成功"
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
     * 从字段值构建ShoppingItemEntity对象
     */
    private fun buildShoppingItemFromFields(): ShoppingItemEntity {
        val name = fieldValues["名称"] as? String ?: ""
        val quantityStr = fieldValues["数量"] as? String ?: "1"
        val quantity = quantityStr.toDoubleOrNull() ?: 1.0
        
        // 价格相关
        val estimatedPriceStr = fieldValues["预估价格"] as? String
        val estimatedPrice = estimatedPriceStr?.toDoubleOrNull()
        
        val actualPriceStr = fieldValues["实际价格"] as? String
        val actualPrice = actualPriceStr?.toDoubleOrNull()
        
        val budgetLimitStr = fieldValues["预算上限"] as? String
        val budgetLimit = budgetLimitStr?.toDoubleOrNull()
        
        // 优先级和紧急程度
        val priorityStr = fieldValues["优先级"] as? String
        val priority = when (priorityStr) {
            "低" -> ShoppingItemPriority.LOW
            "高" -> ShoppingItemPriority.HIGH
            "紧急" -> ShoppingItemPriority.URGENT
            else -> ShoppingItemPriority.NORMAL
        }
        
        val urgencyStr = fieldValues["紧急程度"] as? String
        val urgencyLevel = when (urgencyStr) {
            "不急" -> UrgencyLevel.NOT_URGENT
            "急需" -> UrgencyLevel.URGENT
            "非常急需" -> UrgencyLevel.CRITICAL
            else -> UrgencyLevel.NORMAL
        }
        
        // 日期相关
        val deadlineStr = fieldValues["截止日期"] as? String
        val deadline = deadlineStr?.let { 
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)
        }
        
        val remindDateStr = fieldValues["提醒日期"] as? String
        val remindDate = remindDateStr?.let { 
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)
        }
        
        // 标签处理
        val tagsSet = fieldValues["标签"] as? Set<String>
        val tagsString = tagsSet?.joinToString(",")
        
        return ShoppingItemEntity(
            listId = listId,
            name = name,
            quantity = quantity,
            category = fieldValues["分类"] as? String ?: "未指定",
            subCategory = fieldValues["子分类"] as? String,
            brand = fieldValues["品牌"] as? String,
            specification = fieldValues["规格"] as? String,
            customNote = fieldValues["备注"] as? String,
            price = estimatedPrice,
            actualPrice = actualPrice,
            priceUnit = fieldValues["价格单位"] as? String ?: "元",
            budgetLimit = budgetLimit,
            totalPrice = if (quantity > 0 && estimatedPrice != null) quantity * estimatedPrice else null,
            purchaseChannel = fieldValues["购买渠道"] as? String,
            storeName = fieldValues["商店名称"] as? String,
            preferredStore = fieldValues["首选商店"] as? String,
            priority = priority,
            urgencyLevel = urgencyLevel,
            deadline = deadline,
            capacity = (fieldValues["容量"] as? String)?.toDoubleOrNull(),
            capacityUnit = fieldValues["容量单位"] as? String,
            rating = (fieldValues["评分"] as? String)?.toDoubleOrNull(),
            season = (fieldValues["季节"] as? Set<String>)?.joinToString(","),
            sourceItemId = fieldValues["来源物品ID"] as? Long,
            recommendationReason = fieldValues["推荐原因"] as? String,
            remindDate = remindDate,
            isRecurring = fieldValues["周期性购买"] as? Boolean ?: false,
            recurringInterval = (fieldValues["周期间隔"] as? String)?.toIntOrNull(),
            tags = tagsString
        )
    }

    /**
     * 验证ShoppingItemEntity对象
     */
    private fun validateShoppingItem(item: ShoppingItemEntity): Pair<Boolean, String?> {
        return when {
            item.name.isBlank() -> Pair(false, "物品名称不能为空")
            item.quantity <= 0.0 -> Pair(false, "数量必须大于0")
            item.budgetLimit != null && item.price != null && 
                item.price > item.budgetLimit -> Pair(false, "预估价格不能超过预算上限")
            else -> Pair(true, null)
        }
    }

    /**
     * 初始化购物清单专用的默认字段
     */
    private fun initializeDefaultShoppingFields() {
        val defaultFields = listOf(
            Field("基础信息", "名称", true),
            Field("基础信息", "数量", false),
            Field("分类", "分类", false),
            Field("价格", "预估价格", false),
            Field("价格", "预算上限", false),
            Field("购买信息", "购买渠道", false),
            Field("购买信息", "首选商店", false),
            Field("优先级", "优先级", false),
            Field("优先级", "紧急程度", false),
            Field("时间", "截止日期", false),
            Field("基础信息", "备注", false)
        )
        
        _selectedFields.value = defaultFields.toSet()
        saveToCache() // 保存到缓存
    }

    /**
     * 初始化购物清单专用的字段属性
     */
    private fun initializeShoppingFieldProperties() {
        // 调用父类的基础属性初始化
        super.initializeDefaultFieldProperties()
        
        // 购物清单专用字段
        setFieldProperties("预估价格", FieldProperties(
            validationType = ValidationType.NUMBER,
            hint = "请输入预估价格",
            unitOptions = listOf("元", "美元", "欧元", "日元"),
            isCustomizable = true
        ))
        
        setFieldProperties("实际价格", FieldProperties(
            validationType = ValidationType.NUMBER,
            hint = "请输入实际价格",
            unitOptions = listOf("元", "美元", "欧元", "日元"),
            isCustomizable = true
        ))
        
        setFieldProperties("预算上限", FieldProperties(
            validationType = ValidationType.NUMBER,
            hint = "请输入预算上限",
            unitOptions = listOf("元", "美元", "欧元", "日元"),
            isCustomizable = true
        ))
        
        setFieldProperties("优先级", FieldProperties(
            options = listOf("低", "普通", "高", "紧急"),
            isCustomizable = false
        ))
        
        setFieldProperties("紧急程度", FieldProperties(
            options = listOf("不急", "普通", "急需", "非常急需"),
            isCustomizable = false
        ))
        
        setFieldProperties("首选商店", FieldProperties(
            validationType = ValidationType.TEXT,
            hint = "请输入首选商店",
            isCustomizable = true
        ))
        
        setFieldProperties("截止日期", FieldProperties(
            validationType = ValidationType.DATE,
            defaultDate = false
        ))
        
        setFieldProperties("提醒日期", FieldProperties(
            validationType = ValidationType.DATE,
            defaultDate = false
        ))
        
        setFieldProperties("推荐原因", FieldProperties(
            validationType = ValidationType.TEXT,
            hint = "推荐原因",
            isMultiline = true,
            maxLines = 3
        ))
        
        setFieldProperties("周期性购买", FieldProperties(
            validationType = ValidationType.TEXT,
            defaultValue = "false"
        ))
        
        setFieldProperties("周期间隔", FieldProperties(
            validationType = ValidationType.NUMBER,
            hint = "间隔天数",
            min = 1
        ))
        
        // 重新定义标签字段，添加购物相关的标签
        setFieldProperties("标签", FieldProperties(
            displayStyle = DisplayStyle.TAG,
            isMultiSelect = true,
            isCustomizable = true,
            options = listOf("急需", "特价", "促销", "新品", "试用", "囤货", "礼品", "必需品")
        ))
    }

    /**
     * 从库存物品预填充购物清单物品数据
     */
    fun prepareFromInventoryItem(itemId: Long) {
        viewModelScope.launch {
            try {
                val item = repository.getItemById(itemId)
                item?.let {
                    // 基础信息填充
                    saveFieldValue("名称", it.name)
                    saveFieldValue("分类", it.category)
                    saveFieldValue("来源物品ID", itemId)
                    
                    // 可选信息填充
                    it.brand?.let { brand -> if (brand != "未指定") saveFieldValue("品牌", brand) }
                    it.specification?.let { spec -> if (spec != "未指定") saveFieldValue("规格", spec) }
                    it.subCategory?.let { subCat -> if (subCat != "未指定") saveFieldValue("子分类", subCat) }
                    
                    // 从库存物品的价格信息作为预估价格
                    it.price?.let { price -> saveFieldValue("预估价格", price.toString()) }
                    it.priceUnit?.let { unit -> saveFieldValue("价格单位", unit) }
                    
                    // 其他信息
                    it.purchaseChannel?.let { channel -> if (channel != "未指定") saveFieldValue("购买渠道", channel) }
                    it.storeName?.let { store -> if (store != "未指定") saveFieldValue("首选商店", store) }
                    it.capacity?.let { capacity -> saveFieldValue("容量", capacity.toString()) }
                    it.capacityUnit?.let { unit -> saveFieldValue("容量单位", unit) }
                    
                    // 标签信息
                    if (it.tags.isNotEmpty()) {
                        val tagNames = it.tags.map { tag -> tag.name }.toSet()
                        saveFieldValue("标签", tagNames)
                    }
                    
                    saveFieldValue("推荐原因", "基于库存物品：${it.name}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "从库存物品预填充数据失败: ${e.message}"
            }
        }
    }
} 