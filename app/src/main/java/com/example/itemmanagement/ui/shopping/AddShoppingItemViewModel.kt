package com.example.itemmanagement.ui.shopping

import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.data.entity.unified.ShoppingDetailEntity
import com.example.itemmanagement.data.entity.unified.UnifiedItemEntity
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
    repository: UnifiedItemRepository,
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
        // 可以添加购物清单特有的重要程度等信息
        cache.priority = (fieldValues["重要程度"] ?: fieldValues["优先级"]) as? String  // ✅ 兼容旧字段
        cache.urgencyLevel = fieldValues["紧急程度"] as? String
    }

    override suspend fun saveOrUpdateItem() {
        // 验证数据
        val (unifiedItem, shoppingDetail) = buildShoppingItemFromFields()
        val (isValid, errorMessage) = validateShoppingItem(unifiedItem, shoppingDetail)
        
        if (!isValid) {
            _errorMessage.value = errorMessage ?: "数据验证失败"
            _saveResult.value = false
            return
        }

        try {
            // 保存到数据库（包含照片和标签）
            val itemId = repository.insertShoppingItemSimple(
                unifiedItem = unifiedItem,
                shoppingDetail = shoppingDetail,
                photoUris = _photoUris.value ?: emptyList(),
                tags = _selectedTags.value ?: emptyMap()
            )
            
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
    private fun buildShoppingItemFromFields(): Pair<UnifiedItemEntity, ShoppingDetailEntity> {
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
        
        // 重要程度和紧急程度
        val priorityStr = (fieldValues["重要程度"] ?: fieldValues["优先级"]) as? String  // ✅ 兼容旧字段
        val priority = ShoppingItemPriority.fromDisplayName(priorityStr ?: "") ?: 
            when (priorityStr) {  // ✅ 兼容旧的中文值
                "低", "次要", "次要物品" -> ShoppingItemPriority.LOW
                "普通", "一般", "一般物品" -> ShoppingItemPriority.NORMAL
                "高", "重要", "重要物品" -> ShoppingItemPriority.HIGH
                "紧急", "关键", "关键物品" -> ShoppingItemPriority.CRITICAL
                else -> ShoppingItemPriority.NORMAL
            }
        
        val urgencyStr = fieldValues["紧急程度"] as? String
        val urgencyLevel = UrgencyLevel.fromDisplayName(urgencyStr ?: "") ?: 
            when (urgencyStr) {  // ✅ 兼容旧的中文值
                "不急" -> UrgencyLevel.NOT_URGENT
                "普通", "一般" -> UrgencyLevel.NORMAL
                "急需", "紧急" -> UrgencyLevel.URGENT
                "非常急需", "立即" -> UrgencyLevel.CRITICAL
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
        
        // 创建统一物品实体
        val unifiedItem = UnifiedItemEntity(
            name = name,
            category = fieldValues["分类"] as? String ?: "未指定",
            subCategory = fieldValues["子分类"] as? String,
            brand = fieldValues["品牌"] as? String,
            specification = fieldValues["规格"] as? String,
            customNote = fieldValues["备注"] as? String
        )
        
        // 创建购物详情实体
        val shoppingDetail = ShoppingDetailEntity(
            itemId = 0, // 将在插入时设置
            shoppingListId = listId,
            quantity = quantity,
            estimatedPrice = estimatedPrice,
            estimatedPriceUnit = (fieldValues["预估价格_unit"] as? String) ?: "元",
            actualPrice = actualPrice,
            actualPriceUnit = (fieldValues["实际价格_unit"] as? String) ?: "元",
            budgetLimit = budgetLimit,
            budgetLimitUnit = (fieldValues["预算上限_unit"] as? String) ?: "元",
            purchaseChannel = fieldValues["购买渠道"] as? String,
            // ✅ "购买商店"统一字段，只写入 storeName
            storeName = (fieldValues["购买商店"] ?: fieldValues["商店名称"]) as? String,
            priority = priority,
            urgencyLevel = urgencyLevel,
            deadline = deadline,
            // 注意：capacity, rating, season 已移至 UnifiedItemEntity
            recommendationReason = fieldValues["推荐原因"] as? String,
            remindDate = remindDate,
            isRecurring = fieldValues["周期性购买"] as? Boolean ?: false,
            recurringInterval = (fieldValues["周期间隔"] as? String)?.toIntOrNull(),
            tags = tagsString
        )
        
        return Pair(unifiedItem, shoppingDetail)
    }

    /**
     * 验证统一架构的购物物品对象
     */
    private fun validateShoppingItem(unifiedItem: UnifiedItemEntity, shoppingDetail: ShoppingDetailEntity): Pair<Boolean, String?> {
        return when {
            unifiedItem.name.isBlank() -> Pair(false, "物品名称不能为空")
            shoppingDetail.quantity <= 0.0 -> Pair(false, "数量必须大于0")
            shoppingDetail.budgetLimit != null && shoppingDetail.estimatedPrice != null && 
                shoppingDetail.estimatedPrice > shoppingDetail.budgetLimit -> Pair(false, "预估价格不能超过预算上限")
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
            Field("购买信息", "购买商店", false),  // ✅ 统一为"购买商店"
            Field("购买计划", "重要程度", false),  // ✅ 优化：优先级 -> 重要程度
            Field("购买计划", "紧急程度", false),  // ✅ 优化：分组名称调整
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
        
        setFieldProperties("重要程度", FieldProperties(
            options = ShoppingItemPriority.getDisplayNames(),
            hint = "这个物品对你来说有多重要？",
            isCustomizable = false
        ))
        
        setFieldProperties("紧急程度", FieldProperties(
            options = UrgencyLevel.getDisplayNames(),
            hint = "什么时候必须买到？",
            isCustomizable = false
        ))
        
        // ✅ 统一的"购买商店"字段（替代"首选商店"和"商店名称"）
        setFieldProperties("购买商店", FieldProperties(
            validationType = ValidationType.TEXT,
            hint = "请输入购买商店名称",
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
                    // ✅ 优先使用统一的"购买商店"字段
                    it.storeName?.let { store -> if (store != "未指定") saveFieldValue("购买商店", store) }
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
    
    /**
     * 保存购物物品
     */
    fun saveShoppingItem(callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                android.util.Log.d("AddShoppingItem", "========== 开始保存购物物品 ==========")
                android.util.Log.d("AddShoppingItem", "购物清单ID: $listId")
                
                // 1. 验证必填字段
                val name = (fieldValues["名称"] as? String)?.trim()
                if (name.isNullOrBlank()) {
                    callback(false, "请输入物品名称")
                    return@launch
                }
                
                val quantity = (fieldValues["数量"] as? String)?.toDoubleOrNull() ?: 1.0
                val unit = (fieldValues["单位"] as? String) ?: "个"
                val category = (fieldValues["分类"] as? String) ?: "未分类"
                
                android.util.Log.d("AddShoppingItem", "物品名称: $name")
                android.util.Log.d("AddShoppingItem", "数量: $quantity $unit")
                android.util.Log.d("AddShoppingItem", "分类: $category")
                
                // 2. 创建UnifiedItemEntity
                val unifiedItem = UnifiedItemEntity(
                    name = name,
                    category = category,
                    subCategory = fieldValues["子分类"] as? String,
                    brand = fieldValues["品牌"] as? String,
                    specification = fieldValues["规格"] as? String,
                    customNote = fieldValues["备注"] as? String,
                    createdDate = java.util.Date()
                )
                
                // 3. 创建ShoppingDetailEntity
                val shoppingDetail = ShoppingDetailEntity(
                    itemId = 0L, // 由Repository填充
                    shoppingListId = listId,
                    quantity = quantity,
                    quantityUnit = unit,
                    estimatedPrice = (fieldValues["预估价格"] as? String)?.toDoubleOrNull(),
                    estimatedPriceUnit = (fieldValues["预估价格_unit"] as? String) ?: "元",
                    actualPrice = (fieldValues["实际价格"] as? String)?.toDoubleOrNull(),
                    actualPriceUnit = (fieldValues["实际价格_unit"] as? String) ?: "元",
                    budgetLimit = (fieldValues["预算上限"] as? String)?.toDoubleOrNull(),
                    budgetLimitUnit = (fieldValues["预算上限_unit"] as? String) ?: "元",
                    purchaseChannel = fieldValues["购买渠道"] as? String,
                    // ✅ "购买商店"统一字段，只写入 storeName
                    storeName = (fieldValues["购买商店"] ?: fieldValues["商店名称"]) as? String,
                    priority = parsePriority((fieldValues["重要程度"] ?: fieldValues["优先级"]) as? String),  // ✅ 兼容
                    urgencyLevel = parseUrgency(fieldValues["紧急程度"] as? String),
                    deadline = parseDate(fieldValues["截止日期"] as? String),
                    addedReason = "USER_MANUAL"
                )
                
                android.util.Log.d("AddShoppingItem", "ShoppingDetail创建成功: shoppingListId=$listId, quantity=$quantity")
                
                // 4. 调用Repository保存（包含照片和标签）
                android.util.Log.d("AddShoppingItem", "开始调用Repository保存")
                android.util.Log.d("AddShoppingItem", "照片数量: ${_photoUris.value?.size ?: 0}")
                android.util.Log.d("AddShoppingItem", "标签: ${_selectedTags.value}")
                repository.addShoppingItem(
                    unifiedItem = unifiedItem,
                    shoppingDetail = shoppingDetail,
                    photoUris = _photoUris.value ?: emptyList(),
                    tags = _selectedTags.value ?: emptyMap()
                )
                android.util.Log.d("AddShoppingItem", "Repository保存成功")
                
                // 5. 清除缓存
                cacheViewModel.clearShoppingItemCache(listId)
                
                callback(true, "已添加「$name」到购物清单")
                
            } catch (e: Exception) {
                android.util.Log.e("AddShoppingItem", "保存失败", e)
                callback(false, "保存失败: ${e.message}")
            }
        }
    }
    
    private fun parsePriority(value: String?): ShoppingItemPriority {
        return ShoppingItemPriority.fromDisplayName(value?.trim() ?: "") ?: when (value?.trim()) {
            "低", "次要" -> ShoppingItemPriority.LOW
            "普通", "一般" -> ShoppingItemPriority.NORMAL
            "高", "重要" -> ShoppingItemPriority.HIGH
            "紧急", "关键" -> ShoppingItemPriority.CRITICAL
            else -> ShoppingItemPriority.NORMAL
        }
    }
    
    private fun parseUrgency(value: String?): UrgencyLevel {
        return UrgencyLevel.fromDisplayName(value?.trim() ?: "") ?: when (value?.trim()) {
            "不急" -> UrgencyLevel.NOT_URGENT
            "普通", "一般" -> UrgencyLevel.NORMAL
            "急需", "紧急" -> UrgencyLevel.URGENT
            "非常急需", "立即" -> UrgencyLevel.CRITICAL
            else -> UrgencyLevel.NORMAL
        }
    }
    
    /**
     * 解析日期字符串
     * 支持多种日期格式：yyyy-MM-dd, yyyy/MM/dd, yyyy年MM月dd日
     */
    private fun parseDate(value: String?): java.util.Date? {
        if (value.isNullOrBlank()) return null
        
        return try {
            // 尝试多种日期格式
            val formats = listOf(
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()),
                SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)
            )
            
            for (format in formats) {
                try {
                    return format.parse(value)
                } catch (e: Exception) {
                    // 尝试下一个格式
                }
            }
            
            android.util.Log.w("AddShoppingItem", "无法解析日期: $value")
            null
        } catch (e: Exception) {
            android.util.Log.e("AddShoppingItem", "日期解析异常: ${e.message}")
            null
        }
    }
} 