package com.example.itemmanagement.ui.wishlist.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.entity.wishlist.WishlistPriority
import com.example.itemmanagement.data.entity.wishlist.WishlistUrgency
import com.example.itemmanagement.data.model.wishlist.WishlistItemDetails
import com.example.itemmanagement.data.repository.WishlistRepository
import com.example.itemmanagement.ui.base.BaseItemViewModel
import com.example.itemmanagement.ui.base.ItemStateCacheViewModel
import com.example.itemmanagement.ui.wishlist.WishlistFieldManager
import kotlinx.coroutines.launch

/**
 * 心愿单编辑ViewModel
 * 基于BaseItemViewModel，专门用于编辑现有的心愿单物品
 * 
 * 核心功能：
 * 1. 复用BaseItemViewModel的完整字段管理系统
 * 2. 使用独立的心愿单编辑缓存空间（按物品ID隔离）
 * 3. 加载现有心愿单物品数据并填充到字段
 * 4. 将修改后的字段数据更新到数据库
 */
class WishlistEditViewModel(
    repository: ItemRepository,
    cacheViewModel: ItemStateCacheViewModel,
    private val wishlistRepository: WishlistRepository,
    private val itemId: Long
) : BaseItemViewModel(repository, cacheViewModel) {

    override fun getCurrentCache(): ItemStateCacheViewModel.WishlistEditCache {
        return cacheViewModel.getWishlistEditCache(itemId)
    }
    
    override fun getCacheKey(): String = "WishlistEdit_$itemId"
    
    override fun loadDataFromCache() {
        val cache = getCurrentCache()
        android.util.Log.d("WishlistEditViewModel", "从缓存加载数据 (ID:$itemId): fieldValues=${cache.fieldValues}")
        
        // 加载基础字段数据
        fieldValues = cache.fieldValues
        _selectedFields.value = cache.selectedFields
        _photoUris.value = cache.photoUris
        _selectedTags.value = cache.selectedTags
        customOptionsMap = cache.customOptions
        customUnitsMap = cache.customUnits
        customTagsMap = cache.customTags
    }
    
    override fun saveDataToCache() {
        val cache = getCurrentCache()
        android.util.Log.d("WishlistEditViewModel", "保存数据到缓存 (ID:$itemId): fieldValues=${fieldValues}")
        
        // 保存基础字段数据
        cache.fieldValues = fieldValues.toMutableMap()
        cache.selectedFields = _selectedFields.value ?: setOf()
        cache.photoUris = _photoUris.value ?: emptyList()
        cache.selectedTags = _selectedTags.value ?: mapOf()
        cache.customOptions = customOptionsMap.toMutableMap()
        cache.customUnits = customUnitsMap.toMutableMap()
        cache.customTags = customTagsMap.toMutableMap()
        
        // 保存心愿单专用字段
        cache.priorityLevel = getFieldValue("优先级") as? String
        cache.urgencyLevel = getFieldValue("紧急程度") as? String
        cache.targetPrice = getFieldValue("目标价格") as? Double
        cache.priceTrackingEnabled = (getFieldValue("价格跟踪") as? Boolean) ?: true
        cache.purchaseTiming = getFieldValue("购买计划") as? String
    }
    
    override suspend fun saveOrUpdateItem() {
        android.util.Log.d("WishlistEditViewModel", "开始更新心愿单物品 (ID:$itemId)")
        
        // 构建心愿单物品详情对象
        val wishlistDetails = buildWishlistItemDetails()
        
        // 验证数据
        val (isValid, errorMessage) = validateWishlistItem(wishlistDetails)
        if (!isValid) {
            _errorMessage.value = errorMessage ?: "数据验证失败"
            _saveResult.value = false
            return
        }

        try {
            // 获取现有实体并更新
            val existingItem = wishlistRepository.getItemById(itemId)
            if (existingItem == null) {
                _errorMessage.value = "找不到要更新的心愿单物品"
                _saveResult.value = false
                return
            }
            
            // 使用现有数据构建更新后的实体
            val updatedItem = buildUpdatedEntity(existingItem, wishlistDetails)
            wishlistRepository.updateWishlistItem(updatedItem)
            
            _saveResult.value = true
            _errorMessage.value = "心愿单物品更新成功"
            
            // 清理缓存
            cacheViewModel.clearWishlistEditCache(itemId)
            android.util.Log.d("WishlistEditViewModel", "心愿单物品更新成功 (ID:$itemId)")
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "更新失败：未知错误"
            _saveResult.value = false
            android.util.Log.e("WishlistEditViewModel", "更新心愿单物品失败", e)
        }
    }
    
    override fun initializeDefaultFieldProperties() {
        android.util.Log.d("WishlistEditViewModel", "🚀 开始初始化心愿单字段属性 (编辑模式)")
        android.util.Log.d("WishlistEditViewModel", "📋 当前fieldProperties大小: ${fieldProperties.size}")

        // 先调用父类初始化
        android.util.Log.d("WishlistEditViewModel", "📞 调用父类initializeDefaultFieldProperties()")
        super.initializeDefaultFieldProperties()
        android.util.Log.d("WishlistEditViewModel", "✅ 父类字段属性初始化完成，当前fieldProperties大小: ${fieldProperties.size}")

        // 添加心愿单专用字段属性
        android.util.Log.d("WishlistEditViewModel", "🔧 开始添加心愿单专用字段属性")
        val wishlistProperties = WishlistFieldManager.getWishlistFieldProperties()
        android.util.Log.d("WishlistEditViewModel", "📊 心愿单字段属性总数: ${wishlistProperties.size}")
        wishlistProperties.forEach { (name, properties) ->
            android.util.Log.d("WishlistEditViewModel", "🏷️ 设置字段属性: $name")
            android.util.Log.d("WishlistEditViewModel", "   📝 ValidationType: ${properties.validationType}")
            android.util.Log.d("WishlistEditViewModel", "   🎨 DisplayStyle: ${properties.displayStyle}")
            android.util.Log.d("WishlistEditViewModel", "   📋 Options: ${properties.options}")
            android.util.Log.d("WishlistEditViewModel", "   📏 UnitOptions: ${properties.unitOptions}")
            android.util.Log.d("WishlistEditViewModel", "   ✅ IsRequired: ${properties.isRequired}")
            android.util.Log.d("WishlistEditViewModel", "   📝 IsMultiline: ${properties.isMultiline}")
            android.util.Log.d("WishlistEditViewModel", "   🔧 IsCustomizable: ${properties.isCustomizable}")
            android.util.Log.d("WishlistEditViewModel", "   💬 Hint: ${properties.hint}")
            setFieldProperties(name, properties)
            // 验证设置结果
            val verifyProperties = getFieldProperties(name)
            android.util.Log.d("WishlistEditViewModel", "   ✔️ 验证设置结果: ${verifyProperties}")
        }

        android.util.Log.d("WishlistEditViewModel", "🎉 初始化心愿单字段属性完成，最终fieldProperties大小: ${fieldProperties.size}")

        // 打印最终的字段属性映射
        fieldProperties.forEach { (name: String, properties: com.example.itemmanagement.ui.common.FieldProperties) ->
            android.util.Log.d("WishlistEditViewModel", "📋 最终字段属性 $name: ValidationType=${properties.validationType}, Options=${properties.options}")
        }
    }
    
    /**
     * 加载现有心愿单物品数据
     * 在Fragment的onViewCreated中调用
     */
    fun loadWishlistItem() {
        android.util.Log.d("WishlistEditViewModel", "🚀 开始加载心愿单物品数据 (ID:$itemId)")
        
        // 确保字段属性已设置
        android.util.Log.d("WishlistEditViewModel", "🔍 检查字段属性是否已设置，当前fieldProperties大小: ${fieldProperties.size}")
        if (fieldProperties.isEmpty() || !fieldProperties.containsKey("优先级")) {
            android.util.Log.w("WishlistEditViewModel", "⚠️ 字段属性未设置或不完整，先初始化字段属性")
            initializeDefaultFieldProperties()
        } else {
            android.util.Log.d("WishlistEditViewModel", "✅ 字段属性已设置完成")
        }
        
        viewModelScope.launch {
            try {
                val wishlistItem = wishlistRepository.getItemById(itemId)
                
                if (wishlistItem != null) {
                    android.util.Log.d("WishlistEditViewModel", "📋 找到心愿单物品: ${wishlistItem.name}")
                    // 将实体数据转换为字段值
                    populateFieldsFromEntity(wishlistItem)
                    android.util.Log.d("WishlistEditViewModel", "✅ 成功加载心愿单物品数据: ${wishlistItem.name}")
                } else {
                    _errorMessage.value = "找不到指定的心愿单物品"
                    android.util.Log.e("WishlistEditViewModel", "❌ 心愿单物品不存在: ID=$itemId")
                }
            } catch (e: Exception) {
                _errorMessage.value = "加载心愿单物品失败: ${e.message}"
                android.util.Log.e("WishlistEditViewModel", "❌ 加载心愿单物品失败", e)
            }
        }
    }
    
    /**
     * 将WishlistItemEntity数据填充到字段中
     */
    private suspend fun populateFieldsFromEntity(wishlistItem: com.example.itemmanagement.data.entity.wishlist.WishlistItemEntity) {
        android.util.Log.d("WishlistEditViewModel", "🗂️ 开始填充字段数据")
        
        // 基础信息
        android.util.Log.d("WishlistEditViewModel", "📝 填充基础信息")
        saveFieldValue("名称", wishlistItem.name)
        android.util.Log.d("WishlistEditViewModel", "   名称: ${wishlistItem.name}")
        saveFieldValue("分类", wishlistItem.category)
        android.util.Log.d("WishlistEditViewModel", "   分类: ${wishlistItem.category}")
        wishlistItem.subCategory?.let { 
            saveFieldValue("子分类", it)
            android.util.Log.d("WishlistEditViewModel", "   子分类: $it")
        }
        wishlistItem.brand?.let { 
            saveFieldValue("品牌", it)
            android.util.Log.d("WishlistEditViewModel", "   品牌: $it")
        }
        wishlistItem.specification?.let { 
            saveFieldValue("规格", it)
            android.util.Log.d("WishlistEditViewModel", "   规格: $it")
        }
        
        // 价格相关
        android.util.Log.d("WishlistEditViewModel", "💰 填充价格信息")
        wishlistItem.price?.let { 
            saveFieldValue("单价", it)
            android.util.Log.d("WishlistEditViewModel", "   单价: $it")
        }
        wishlistItem.targetPrice?.let { 
            saveFieldValue("目标价格", it)
            android.util.Log.d("WishlistEditViewModel", "   目标价格: $it")
        }
        wishlistItem.budgetLimit?.let { 
            saveFieldValue("个人预算", it)
            android.util.Log.d("WishlistEditViewModel", "   个人预算: $it")
        }
        saveFieldValue("价格跟踪", wishlistItem.isPriceTrackingEnabled)
        android.util.Log.d("WishlistEditViewModel", "   价格跟踪: ${wishlistItem.isPriceTrackingEnabled}")
        
        // 购买计划
        android.util.Log.d("WishlistEditViewModel", "📋 填充购买计划")
        saveFieldValue("优先级", wishlistItem.priority.displayName)
        android.util.Log.d("WishlistEditViewModel", "   优先级: ${wishlistItem.priority.displayName}")
        saveFieldValue("紧急程度", wishlistItem.urgency.displayName)
        android.util.Log.d("WishlistEditViewModel", "   紧急程度: ${wishlistItem.urgency.displayName}")
        saveFieldValue("数量", wishlistItem.quantity)
        android.util.Log.d("WishlistEditViewModel", "   数量: ${wishlistItem.quantity}")
        saveFieldValue("数量单位", wishlistItem.quantityUnit)
        android.util.Log.d("WishlistEditViewModel", "   数量单位: ${wishlistItem.quantityUnit}")
        
        // 购买偏好
        android.util.Log.d("WishlistEditViewModel", "🛒 填充购买偏好")
        wishlistItem.purchaseChannel?.let { 
            saveFieldValue("首选渠道", it)
            android.util.Log.d("WishlistEditViewModel", "   首选渠道: $it")
        }
        wishlistItem.preferredBrand?.let { 
            saveFieldValue("首选品牌", it)
            android.util.Log.d("WishlistEditViewModel", "   首选品牌: $it")
        }
        wishlistItem.customNote?.let { 
            saveFieldValue("备注", it)
            android.util.Log.d("WishlistEditViewModel", "   备注: $it")
        }
        
        // 时间信息
        android.util.Log.d("WishlistEditViewModel", "📅 填充时间信息")
        saveFieldValue("添加日期", wishlistItem.addDate)
        android.util.Log.d("WishlistEditViewModel", "   添加日期: ${wishlistItem.addDate}")
        
        android.util.Log.d("WishlistEditViewModel", "📊 字段数据填充完成，当前fieldValues大小: ${fieldValues.size}")
        
        // 初始化默认选中字段（编辑模式）
        android.util.Log.d("WishlistEditViewModel", "🔄 初始化编辑模式默认字段")
        initializeEditDefaultFields()
    }
    
    /**
     * 构建更新后的WishlistItemEntity
     * 保留原有的系统字段，只更新用户编辑的字段
     */
    private fun buildUpdatedEntity(
        existingItem: com.example.itemmanagement.data.entity.wishlist.WishlistItemEntity,
        wishlistDetails: WishlistItemDetails
    ): com.example.itemmanagement.data.entity.wishlist.WishlistItemEntity {
        return existingItem.copy(
            // 更新基础信息
            name = wishlistDetails.name,
            category = wishlistDetails.category,
            subCategory = wishlistDetails.subCategory,
            brand = wishlistDetails.brand,
            specification = wishlistDetails.specification,
            customNote = wishlistDetails.notes,
            
            // 更新价格相关信息
            price = wishlistDetails.estimatedPrice,
            targetPrice = wishlistDetails.targetPrice,
            budgetLimit = wishlistDetails.budgetLimit,
            
            // 更新购买计划
            priority = wishlistDetails.priority,
            urgency = wishlistDetails.urgency,
            quantity = wishlistDetails.desiredQuantity,
            quantityUnit = wishlistDetails.quantityUnit,
            purchaseChannel = wishlistDetails.preferredStore,
            
            // 更新修改时间
            lastModified = java.util.Date(),
            
            // 保留原有的系统字段（ID、创建时间、价格历史等）
            // 这些字段不在copy中修改，会自动保留原值
        )
    }
    
    /**
     * 从字段值构建WishlistItemDetails对象
     */
    private fun buildWishlistItemDetails(): WishlistItemDetails {
        return WishlistItemDetails(
            name = getFieldValue("名称") as? String ?: "",
            category = getFieldValue("分类") as? String ?: "未分类",
            subCategory = getFieldValue("子分类") as? String,
            brand = getFieldValue("品牌") as? String,
            specification = getFieldValue("规格") as? String,
            estimatedPrice = getFieldValue("单价") as? Double,
            targetPrice = getFieldValue("目标价格") as? Double,
            priority = getWishlistPriority(getFieldValue("优先级") as? String),
            urgency = getWishlistUrgency(getFieldValue("紧急程度") as? String),
            desiredQuantity = (getFieldValue("数量") as? Double) ?: 1.0,
            quantityUnit = (getFieldValue("数量单位") as? String) ?: "个",
            budgetLimit = getFieldValue("个人预算") as? Double,
            preferredStore = getFieldValue("首选渠道") as? String,
            notes = getFieldValue("备注") as? String,
            sourceUrl = null, // 编辑时保持原有值
            imageUrl = null,  // 图片处理稍后实现
            addedReason = null // 编辑时不修改原因
        )
    }
    
    /**
     * 验证心愿单物品数据
     */
    private fun validateWishlistItem(itemDetails: WishlistItemDetails): Pair<Boolean, String?> {
        return when {
            itemDetails.name.isBlank() -> Pair(false, "物品名称不能为空")
            itemDetails.name.length > 100 -> Pair(false, "物品名称不能超过100个字符")
            itemDetails.category.isBlank() -> Pair(false, "请选择物品分类")
            itemDetails.estimatedPrice != null && itemDetails.estimatedPrice < 0 -> 
                Pair(false, "预估价格不能为负数")
            itemDetails.targetPrice != null && itemDetails.targetPrice < 0 -> 
                Pair(false, "目标价格不能为负数")
            itemDetails.budgetLimit != null && itemDetails.budgetLimit < 0 -> 
                Pair(false, "个人预算不能为负数")
            itemDetails.desiredQuantity <= 0 -> Pair(false, "期望数量必须大于0")
            itemDetails.estimatedPrice != null && itemDetails.targetPrice != null && 
                itemDetails.targetPrice > itemDetails.estimatedPrice -> 
                Pair(false, "目标价格不应高于预估价格")
            itemDetails.budgetLimit != null && itemDetails.targetPrice != null &&
                itemDetails.targetPrice > itemDetails.budgetLimit ->
                Pair(false, "目标价格不能超过个人预算")
            else -> Pair(true, null)
        }
    }
    
    /**
     * 将字符串转换为WishlistPriority枚举
     */
    private fun getWishlistPriority(priorityStr: String?): WishlistPriority {
        return when (priorityStr) {
            "低优先级", "低" -> WishlistPriority.LOW
            "普通" -> WishlistPriority.NORMAL
            "高优先级", "高" -> WishlistPriority.HIGH
            "紧急" -> WishlistPriority.URGENT
            else -> WishlistPriority.NORMAL
        }
    }
    
    /**
     * 将字符串转换为WishlistUrgency枚举
     */
    private fun getWishlistUrgency(urgencyStr: String?): WishlistUrgency {
        return when (urgencyStr) {
            "不急" -> WishlistUrgency.NOT_URGENT
            "一般" -> WishlistUrgency.NORMAL
            "急需" -> WishlistUrgency.URGENT
            "非常急需" -> WishlistUrgency.CRITICAL
            else -> WishlistUrgency.NORMAL
        }
    }
    
    /**
     * 初始化编辑模式的默认字段
     * 编辑模式下，显示所有有数据的字段
     */
    private fun initializeEditDefaultFields() {
        val fieldsWithData = mutableSetOf<String>()
        
        // 检查哪些字段有数据
        fieldValues.forEach { (fieldName, value) ->
            if (value != null && value.toString().isNotBlank()) {
                fieldsWithData.add(fieldName)
            }
        }
        
        // 确保基础字段都被选中
        val essentialFields = setOf("名称", "分类", "优先级", "紧急程度")
        fieldsWithData.addAll(essentialFields)
        
        // 创建Field对象并设置为选中
        val selectedFields = fieldsWithData.map { fieldName ->
            val group = WishlistFieldManager.getWishlistFieldGroup(fieldName)
            WishlistFieldManager.createWishlistField(group, fieldName, true)
        }.toSet()
        
        // 更新选中字段
        selectedFields.forEach { field ->
            updateFieldSelection(field, true)
        }
        
        android.util.Log.d("WishlistEditViewModel", "初始化编辑模式字段: ${selectedFields.size}个字段")
    }
}
