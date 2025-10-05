package com.example.itemmanagement.ui.shopping

import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.data.entity.unified.ShoppingDetailEntity
import com.example.itemmanagement.data.entity.unified.UnifiedItemEntity
import com.example.itemmanagement.data.entity.UrgencyLevel
import com.example.itemmanagement.data.entity.ShoppingItemPriority
import com.example.itemmanagement.data.model.Item
import com.example.itemmanagement.ui.base.BaseItemViewModel
import com.example.itemmanagement.ui.base.ItemStateCacheViewModel
import com.example.itemmanagement.ui.add.Field
import com.example.itemmanagement.ui.common.FieldProperties
import com.example.itemmanagement.ui.common.ValidationType  
import com.example.itemmanagement.ui.common.DisplayStyle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 编辑购物物品 ViewModel
 * 
 * 继承自 BaseItemViewModel，专门处理编辑购物清单物品的业务逻辑
 */
class EditShoppingItemViewModel(
    repository: UnifiedItemRepository,
    cacheViewModel: ItemStateCacheViewModel,
    private val itemId: Long,
    private val listId: Long  // 购物清单ID
) : BaseItemViewModel(repository, cacheViewModel) {

    private var originalShoppingDetail: ShoppingDetailEntity? = null
    private var originalUnifiedItem: UnifiedItemEntity? = null

    init {
        Log.d("EditShoppingItem", "========== 初始化编辑ViewModel ==========")
        Log.d("EditShoppingItem", "物品ID: $itemId, 购物清单ID: $listId")
        
        // ✅ 初始化购物清单专用字段属性（与AddShoppingItemViewModel保持一致）
        initializeShoppingFieldProperties()
        
        // ⚠️ 编辑模式：始终从数据库重新加载最新数据，忽略缓存
        // 这确保用户看到的是最新保存的数据，而不是之前未保存的草稿
        Log.d("EditShoppingItem", "编辑模式：从数据库加载最新物品数据")
        loadItemForEdit()
    }
    
    /**
     * 初始化购物清单专用字段属性
     * 与 AddShoppingItemViewModel 保持完全一致
     */
    private fun initializeShoppingFieldProperties() {
        // 调用父类的基础属性初始化
        initializeDefaultFieldProperties()
        
        // 购物清单专用字段（与AddShoppingItemViewModel保持一致）
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
            validationType = ValidationType.TEXT,
            hint = "这个物品对你来说有多重要？",
            options = ShoppingItemPriority.getDisplayNames(),
            isCustomizable = false
        ))
        
        setFieldProperties("紧急程度", FieldProperties(
            validationType = ValidationType.TEXT,
            hint = "什么时候必须买到？",
            options = UrgencyLevel.getDisplayNames(),
            isCustomizable = false
        ))
        
        setFieldProperties("截止日期", FieldProperties(
            validationType = ValidationType.DATE,
            hint = "选择截止日期"
        ))
        
        // ✅ 统一的"购买商店"字段
        setFieldProperties("购买商店", FieldProperties(
            validationType = ValidationType.TEXT,
            hint = "请输入购买商店名称",
            isCustomizable = true
        ))
        
        setFieldProperties("购买渠道", FieldProperties(
            validationType = ValidationType.TEXT,
            hint = "请输入购买渠道",
            options = listOf("超市", "电商", "专卖店", "其他"),
            isCustomizable = true
        ))
        
        setFieldProperties("商店名称", FieldProperties(
            validationType = ValidationType.TEXT,
            hint = "请输入商店名称",
            isCustomizable = true
        ))
    }

    // --- 实现抽象方法 ---
    
    override fun getCurrentCache(): Any {
        return cacheViewModel.getEditShoppingItemCache(itemId)
    }

    override fun getCacheKey(): String {
        return "EDIT_SHOPPING_ITEM_${itemId}_${listId}"
    }

    override fun loadDataFromCache() {
        val cache = cacheViewModel.getEditShoppingItemCache(itemId)
        fieldValues = cache.fieldValues.toMutableMap()
        _selectedFields.value = cache.selectedFields
        _photoUris.value = cache.photoUris
        _selectedTags.value = cache.selectedTags
        customOptionsMap = cache.customOptions.toMutableMap()
        customUnitsMap = cache.customUnits.toMutableMap()
        customTagsMap = cache.customTags.toMutableMap()
    }

    override fun saveDataToCache() {
        val cache = cacheViewModel.getEditShoppingItemCache(itemId)
        cache.fieldValues = fieldValues.toMutableMap()
        cache.selectedFields = _selectedFields.value ?: setOf()
        cache.photoUris = _photoUris.value ?: emptyList()
        cache.selectedTags = _selectedTags.value ?: mapOf()
        cache.customOptions = customOptionsMap.toMutableMap()
        cache.customUnits = customUnitsMap.toMutableMap()
        cache.customTags = customTagsMap.toMutableMap()
        cache.originalItemId = itemId
    }

    override suspend fun saveOrUpdateItem() {
        try {
            Log.d("EditShoppingItem", "开始保存更新")
            
            // 1. 构建 UnifiedItemEntity
            val unifiedItem = buildUnifiedItemFromFields()
            
            // 2. 构建 ShoppingDetailEntity
            val shoppingDetail = buildShoppingDetailFromFields()
            
            Log.d("EditShoppingItem", "更新物品: ${unifiedItem.name}")
            Log.d("EditShoppingItem", "照片数量: ${_photoUris.value?.size ?: 0}")
            Log.d("EditShoppingItem", "标签: ${_selectedTags.value}")
            
            // 3. 更新数据库（包含照片和标签）
            repository.updateShoppingItem(
                itemId = itemId,
                unifiedItem = unifiedItem,
                shoppingDetail = shoppingDetail,
                photoUris = _photoUris.value ?: emptyList(),
                tags = _selectedTags.value ?: emptyMap()
            )
            
            // 4. 清除缓存
            cacheViewModel.clearEditShoppingItemCache(itemId)
            
            _saveResult.value = true
            _errorMessage.value = "物品已更新"
            
            Log.d("EditShoppingItem", "保存成功")
            
        } catch (e: Exception) {
            Log.e("EditShoppingItem", "更新失败", e)
            _errorMessage.value = "更新失败: ${e.message}"
            _saveResult.value = false
        }
    }

    // --- 私有辅助方法 ---
    
    /**
     * 加载物品数据进行编辑
     */
    private fun loadItemForEdit() {
        viewModelScope.launch {
            try {
                Log.d("EditShoppingItem", "开始加载物品数据: itemId=$itemId")
                
                // 加载物品数据
                val item = repository.getItemById(itemId)
                val shoppingDetail = repository.getShoppingDetailByItemId(itemId)
                val unifiedItem = repository.getUnifiedItemById(itemId)
                
                if (item != null && shoppingDetail != null && unifiedItem != null) {
                    Log.d("EditShoppingItem", "物品数据加载成功: ${item.name}")
                    originalShoppingDetail = shoppingDetail
                    originalUnifiedItem = unifiedItem
                    populateFieldsFromItem(item, shoppingDetail)
                } else {
                    Log.e("EditShoppingItem", "物品数据加载失败: item=$item, shoppingDetail=$shoppingDetail")
                    _errorMessage.value = "加载物品数据失败"
                }
            } catch (e: Exception) {
                Log.e("EditShoppingItem", "加载物品失败", e)
                _errorMessage.value = "加载失败: ${e.message}"
            }
        }
    }

    /**
     * 从Item和ShoppingDetail填充字段值
     */
    private fun populateFieldsFromItem(item: Item, shoppingDetail: ShoppingDetailEntity) {
        Log.d("EditShoppingItem", "开始填充字段数据")
        
        // 基础信息
        fieldValues["名称"] = item.name
        fieldValues["数量"] = shoppingDetail.quantity.toString()
        fieldValues["数量_unit"] = shoppingDetail.quantityUnit  // ✅ 修复：使用正确的key格式
        fieldValues["备注"] = item.customNote ?: ""
        
        // 分类信息
        fieldValues["分类"] = item.category ?: "未分类"
        fieldValues["子分类"] = item.subCategory ?: ""
        fieldValues["品牌"] = item.brand ?: ""
        fieldValues["规格"] = item.specification ?: ""
        
        // 购买信息
        fieldValues["购买渠道"] = shoppingDetail.purchaseChannel ?: ""
        // ✅ 统一使用"购买商店"字段，从 storeName 读取
        fieldValues["购买商店"] = shoppingDetail.storeName ?: ""
        
        // 价格信息
        fieldValues["预估价格"] = shoppingDetail.estimatedPrice?.toString() ?: ""
        fieldValues["预估价格_unit"] = shoppingDetail.estimatedPriceUnit  // ✅ 独立的预估价格单位
        fieldValues["实际价格"] = shoppingDetail.actualPrice?.toString() ?: ""
        fieldValues["实际价格_unit"] = shoppingDetail.actualPriceUnit  // ✅ 独立的实际价格单位
        fieldValues["预算上限"] = shoppingDetail.budgetLimit?.toString() ?: ""
        fieldValues["预算上限_unit"] = shoppingDetail.budgetLimitUnit  // ✅ 独立的预算上限单位
        
        // 重要程度和紧急程度 - 使用新的 displayName
        fieldValues["重要程度"] = shoppingDetail.priority.displayName
        fieldValues["紧急程度"] = shoppingDetail.urgencyLevel.displayName
        
        // 时间信息
        fieldValues["截止日期"] = formatDate(shoppingDetail.deadline)
        
        // 初始化默认选中字段（与AddShoppingItemViewModel保持一致的顺序）
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
        
        // 加载照片
        Log.d("EditShoppingItem", "从Item加载照片: item.photos.size=${item.photos.size}")
        item.photos.forEachIndexed { index, photo ->
            Log.d("EditShoppingItem", "照片[$index]: uri=${photo.uri}")
        }
        _photoUris.value = item.photos.map { Uri.parse(it.uri) }
        Log.d("EditShoppingItem", "照片加载完成: _photoUris.size=${_photoUris.value?.size}")
        
        // 加载标签（转换为Set<String>格式）
        _selectedTags.value = emptyMap() // 先简化处理，暂不加载标签
        
        Log.d("EditShoppingItem", "字段数据填充完成，共 ${fieldValues.size} 个字段")
    }

    /**
     * 从字段构建 UnifiedItemEntity
     */
    private fun buildUnifiedItemFromFields(): UnifiedItemEntity {
        val name = fieldValues["名称"] as? String ?: ""
        return UnifiedItemEntity(
            id = itemId,
            name = name,
            category = fieldValues["分类"] as? String ?: "未分类",
            subCategory = fieldValues["子分类"] as? String,
            brand = fieldValues["品牌"] as? String,
            specification = fieldValues["规格"] as? String,
            customNote = fieldValues["备注"] as? String,
            createdDate = originalUnifiedItem?.createdDate ?: Date()
        )
    }

    /**
     * 从字段构建 ShoppingDetailEntity
     */
    private fun buildShoppingDetailFromFields(): ShoppingDetailEntity {
        // 解析重要程度（从displayName转换）
        val priorityStr = (fieldValues["重要程度"] ?: fieldValues["优先级"]) as? String  // ✅ 兼容旧字段
        val priority = ShoppingItemPriority.fromDisplayName(priorityStr ?: "") ?: 
            when (priorityStr) {  // ✅ 兼容旧的中文值
                "低", "正常", "次要物品" -> ShoppingItemPriority.LOW
                "普通", "一般", "一般物品" -> ShoppingItemPriority.NORMAL
                "高", "重要", "重要物品" -> ShoppingItemPriority.HIGH
                "紧急", "关键", "关键物品" -> ShoppingItemPriority.CRITICAL
                else -> ShoppingItemPriority.NORMAL
            }
        
        // 解析紧急程度（从displayName转换）
        val urgencyStr = fieldValues["紧急程度"] as? String
        val urgency = UrgencyLevel.fromDisplayName(urgencyStr ?: "") ?: 
            when (urgencyStr) {  // ✅ 兼容旧的中文值
                "不急" -> UrgencyLevel.NOT_URGENT
                "普通", "一般" -> UrgencyLevel.NORMAL
                "急需", "紧急" -> UrgencyLevel.URGENT
                "非常急需", "立即" -> UrgencyLevel.CRITICAL
                else -> UrgencyLevel.NORMAL
            }
        
        // 读取数量和单位
        val quantity = (fieldValues["数量"] as? String)?.toDoubleOrNull() ?: 1.0
        val unit = fieldValues["数量_unit"] as? String ?: "个"  // ✅ 修复：使用正确的key格式
        
        Log.d("EditShoppingItem", "构建ShoppingDetail: 数量=$quantity, 单位=$unit")
        Log.d("EditShoppingItem", "fieldValues[\"数量_unit\"] = ${fieldValues["数量_unit"]}")
        
        // 读取各个价格字段的独立单位
        val estimatedPriceUnit = fieldValues["预估价格_unit"] as? String ?: "元"
        val actualPriceUnit = fieldValues["实际价格_unit"] as? String ?: "元"
        val budgetLimitUnit = fieldValues["预算上限_unit"] as? String ?: "元"
        
        Log.d("EditShoppingItem", "价格单位: 预估=$estimatedPriceUnit, 实际=$actualPriceUnit, 预算=$budgetLimitUnit")
        
        return ShoppingDetailEntity(
            id = originalShoppingDetail?.id ?: 0,
            itemId = itemId,
            shoppingListId = listId,
            quantity = quantity,
            quantityUnit = unit,
            estimatedPrice = (fieldValues["预估价格"] as? String)?.toDoubleOrNull(),
            estimatedPriceUnit = estimatedPriceUnit,  // ✅ 独立的预估价格单位
            actualPrice = (fieldValues["实际价格"] as? String)?.toDoubleOrNull(),
            actualPriceUnit = actualPriceUnit,  // ✅ 独立的实际价格单位
            budgetLimit = (fieldValues["预算上限"] as? String)?.toDoubleOrNull(),
            budgetLimitUnit = budgetLimitUnit,  // ✅ 独立的预算上限单位
            purchaseChannel = fieldValues["购买渠道"] as? String,
            // ✅ "购买商店"统一字段，只写入 storeName
            storeName = (fieldValues["购买商店"] ?: fieldValues["商店名称"]) as? String,
            priority = priority,
            urgencyLevel = urgency,
            deadline = parseDate(fieldValues["截止日期"] as? String),
            isPurchased = originalShoppingDetail?.isPurchased ?: false,
            purchaseDate = originalShoppingDetail?.purchaseDate,
            addedReason = originalShoppingDetail?.addedReason ?: "USER_MANUAL"
        )
    }

    /**
     * 格式化日期
     */
    private fun formatDate(date: Date?): String {
        if (date == null) return ""
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(date)
    }

    /**
     * 解析日期
     */
    private fun parseDate(dateString: String?): Date? {
        if (dateString.isNullOrEmpty()) return null
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            format.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析优先级
     */
    private fun parsePriority(priority: String?): ShoppingItemPriority {
        return when (priority?.uppercase()) {
            "HIGH" -> ShoppingItemPriority.HIGH
            "LOW" -> ShoppingItemPriority.LOW
            else -> ShoppingItemPriority.NORMAL
        }
    }

    /**
     * 解析紧急程度
     */
    private fun parseUrgency(urgency: String?): UrgencyLevel {
        return when (urgency?.uppercase()) {
            "URGENT" -> UrgencyLevel.URGENT
            "CRITICAL" -> UrgencyLevel.CRITICAL
            "NOT_URGENT" -> UrgencyLevel.NOT_URGENT
            else -> UrgencyLevel.NORMAL
        }
    }
}

