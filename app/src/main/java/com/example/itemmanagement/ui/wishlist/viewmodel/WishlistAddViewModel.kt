package com.example.itemmanagement.ui.wishlist.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.data.entity.wishlist.WishlistPriority
import com.example.itemmanagement.data.entity.wishlist.WishlistUrgency
import com.example.itemmanagement.data.model.wishlist.WishlistItemDetails
import com.example.itemmanagement.data.repository.WishlistRepository
import com.example.itemmanagement.data.entity.WishlistItemEntity
import com.example.itemmanagement.ui.base.BaseItemViewModel
import com.example.itemmanagement.ui.base.ItemStateCacheViewModel
import com.example.itemmanagement.ui.add.WishlistFieldManager
import com.example.itemmanagement.ui.add.Field
import kotlinx.coroutines.launch

/**
 * 心愿单添加ViewModel
 * 基于BaseItemViewModel，专门用于添加新的心愿单物品
 * 
 * 核心功能：
 * 1. 复用BaseItemViewModel的完整字段管理系统
 * 2. 使用独立的心愿单缓存空间
 * 3. 将字段数据转换为心愿单专用的数据结构
 * 4. 提供心愿单专用的验证逻辑
 */
class WishlistAddViewModel(
    repository: UnifiedItemRepository,  // 复用现有repository，用于位置等通用数据
    cacheViewModel: ItemStateCacheViewModel,
    private val wishlistRepository: WishlistRepository
) : BaseItemViewModel(repository, cacheViewModel) {

    override fun getCurrentCache(): ItemStateCacheViewModel.WishlistItemCache {
        return cacheViewModel.getWishlistAddCache()
    }
    
    override fun getCacheKey(): String = "WishlistAdd"
    
    override fun loadDataFromCache() {
        android.util.Log.d("WishlistAddViewModel", "🔄 开始从缓存加载数据")
        
        val cache = getCurrentCache()
        android.util.Log.d("WishlistAddViewModel", "📦 获取当前缓存: $cache")
        android.util.Log.d("WishlistAddViewModel", "💾 缓存中的fieldValues: ${cache.fieldValues}")
        android.util.Log.d("WishlistAddViewModel", "📋 缓存中的selectedFields: ${cache.selectedFields}")
        android.util.Log.d("WishlistAddViewModel", "📷 缓存中的photoUris: ${cache.photoUris}")
        android.util.Log.d("WishlistAddViewModel", "🏷️ 缓存中的selectedTags: ${cache.selectedTags}")
        android.util.Log.d("WishlistAddViewModel", "⚙️ 缓存中的customOptions: ${cache.customOptions}")
        android.util.Log.d("WishlistAddViewModel", "📏 缓存中的customUnits: ${cache.customUnits}")
        android.util.Log.d("WishlistAddViewModel", "🏷️ 缓存中的customTags: ${cache.customTags}")
        
        // 加载基础字段数据
        android.util.Log.d("WishlistAddViewModel", "📝 开始加载字段数据")
        fieldValues = cache.fieldValues
        android.util.Log.d("WishlistAddViewModel", "   ✅ fieldValues加载完成: $fieldValues")
        
        _selectedFields.value = cache.selectedFields
        android.util.Log.d("WishlistAddViewModel", "   ✅ selectedFields加载完成: ${_selectedFields.value}")
        
        _photoUris.value = cache.photoUris
        android.util.Log.d("WishlistAddViewModel", "   ✅ photoUris加载完成: ${_photoUris.value}")
        
        _selectedTags.value = cache.selectedTags
        android.util.Log.d("WishlistAddViewModel", "   ✅ selectedTags加载完成: ${_selectedTags.value}")
        
        customOptionsMap = cache.customOptions
        android.util.Log.d("WishlistAddViewModel", "   ✅ customOptionsMap加载完成: $customOptionsMap")
        
        customUnitsMap = cache.customUnits
        android.util.Log.d("WishlistAddViewModel", "   ✅ customUnitsMap加载完成: $customUnitsMap")
        
        customTagsMap = cache.customTags
        android.util.Log.d("WishlistAddViewModel", "   ✅ customTagsMap加载完成: $customTagsMap")
        
        android.util.Log.d("WishlistAddViewModel", "🎉 从缓存加载数据完成")
    }
    
    override fun saveDataToCache() {
        android.util.Log.d("WishlistAddViewModel", "💾 开始保存数据到缓存")
        
        val cache = getCurrentCache()
        android.util.Log.d("WishlistAddViewModel", "📦 获取当前缓存对象: $cache")
        
        android.util.Log.d("WishlistAddViewModel", "📝 保存基础字段数据")
        android.util.Log.d("WishlistAddViewModel", "   💾 fieldValues: $fieldValues")
        cache.fieldValues = fieldValues.toMutableMap()
        
        android.util.Log.d("WishlistAddViewModel", "   📋 selectedFields: ${_selectedFields.value}")
        cache.selectedFields = _selectedFields.value ?: setOf()
        
        android.util.Log.d("WishlistAddViewModel", "   📷 photoUris: ${_photoUris.value}")
        cache.photoUris = _photoUris.value ?: emptyList()
        
        android.util.Log.d("WishlistAddViewModel", "   🏷️ selectedTags: ${_selectedTags.value}")
        cache.selectedTags = _selectedTags.value ?: mapOf()
        
        android.util.Log.d("WishlistAddViewModel", "   ⚙️ customOptionsMap: $customOptionsMap")
        cache.customOptions = customOptionsMap.toMutableMap()
        
        android.util.Log.d("WishlistAddViewModel", "   📏 customUnitsMap: $customUnitsMap")
        cache.customUnits = customUnitsMap.toMutableMap()
        
        android.util.Log.d("WishlistAddViewModel", "   🏷️ customTagsMap: $customTagsMap")
        cache.customTags = customTagsMap.toMutableMap()
        
        // 保存心愿单专用字段
        android.util.Log.d("WishlistAddViewModel", "🎯 保存心愿单专用字段")
        
        val priorityLevel = getFieldValue("优先级") as? String
        android.util.Log.d("WishlistAddViewModel", "   🎯 priorityLevel: $priorityLevel")
        cache.priorityLevel = priorityLevel
        
        val urgencyLevel = getFieldValue("紧急程度") as? String
        android.util.Log.d("WishlistAddViewModel", "   ⚡ urgencyLevel: $urgencyLevel")
        cache.urgencyLevel = urgencyLevel
        
        val targetPrice = getFieldValue("目标价格") as? Double
        android.util.Log.d("WishlistAddViewModel", "   💰 targetPrice: $targetPrice")
        cache.targetPrice = targetPrice
        
        val priceTracking = getFieldValue("价格跟踪")
        val priceTrackingEnabled = (priceTracking as? Boolean) ?: true
        android.util.Log.d("WishlistAddViewModel", "   🔄 priceTracking原始值: $priceTracking, 转换后: $priceTrackingEnabled")
        cache.priceTrackingEnabled = priceTrackingEnabled
        
        val purchaseTiming = getFieldValue("购买计划") as? String
        android.util.Log.d("WishlistAddViewModel", "   ⏰ purchaseTiming: $purchaseTiming")
        cache.purchaseTiming = purchaseTiming
        
        android.util.Log.d("WishlistAddViewModel", "✅ 保存数据到缓存完成")
        android.util.Log.d("WishlistAddViewModel", "📊 最终缓存状态: fieldValues=${cache.fieldValues.size}项, selectedFields=${cache.selectedFields.size}项")
    }
    
    override suspend fun saveOrUpdateItem() {
        android.util.Log.d("WishlistAddViewModel", "开始保存心愿单物品")
        
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
            // 将WishlistItemDetails转换为WishlistItemEntity
            val entity = WishlistItemEntity(
                name = wishlistDetails.name,
                category = wishlistDetails.category,
                subCategory = wishlistDetails.subCategory,
                brand = wishlistDetails.brand,
                specification = wishlistDetails.specification,
                customNote = wishlistDetails.notes,
                price = wishlistDetails.estimatedPrice,
                targetPrice = wishlistDetails.targetPrice,
                priority = wishlistDetails.priority,
                urgency = wishlistDetails.urgency,
                quantity = wishlistDetails.desiredQuantity,
                quantityUnit = wishlistDetails.quantityUnit,
                budgetLimit = wishlistDetails.budgetLimit,
                purchaseChannel = wishlistDetails.preferredStore,
                sourceUrl = wishlistDetails.sourceUrl,
                imageUrl = wishlistDetails.imageUrl,
                addedReason = wishlistDetails.addedReason
            )
            val itemId = wishlistRepository.addWishlistItem(entity)
            
            if (itemId > 0) {
                _saveResult.value = true
                _errorMessage.value = "已成功添加到心愿单"
                
                // 清理缓存
                cacheViewModel.clearWishlistAddCache()
                android.util.Log.d("WishlistAddViewModel", "心愿单物品保存成功，ID: $itemId")
            } else {
                _errorMessage.value = "添加失败：数据库插入返回无效ID"
                _saveResult.value = false
            }
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "添加失败：未知错误"
            _saveResult.value = false
            android.util.Log.e("WishlistAddViewModel", "保存心愿单物品失败", e)
        }
    }
    
    override fun initializeDefaultFieldProperties() {
        android.util.Log.d("WishlistAddViewModel", "🚀 开始初始化心愿单字段属性")
        android.util.Log.d("WishlistAddViewModel", "📋 当前fieldProperties大小: ${fieldProperties.size}")
        
        // 先调用父类初始化
        android.util.Log.d("WishlistAddViewModel", "📞 调用父类initializeDefaultFieldProperties()")
        super.initializeDefaultFieldProperties()
        android.util.Log.d("WishlistAddViewModel", "✅ 父类字段属性初始化完成，当前fieldProperties大小: ${fieldProperties.size}")
        
        // 添加心愿单专用字段属性
        android.util.Log.d("WishlistAddViewModel", "🔧 开始添加心愿单专用字段属性")
        // 暂时跳过字段属性设置，使用默认配置
        android.util.Log.d("WishlistAddViewModel", "📊 心愿单字段属性设置完成")
        
        android.util.Log.d("WishlistAddViewModel", "🎉 初始化心愿单字段属性完成，最终fieldProperties大小: ${fieldProperties.size}")
        
        // 打印最终的字段属性映射
        fieldProperties.forEach { (name: String, properties: com.example.itemmanagement.ui.common.FieldProperties) ->
            android.util.Log.d("WishlistAddViewModel", "📋 最终字段属性 $name: ValidationType=${properties.validationType}, Options=${properties.options}")
        }
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
            sourceUrl = null, // 手动添加，无链接
            imageUrl = null,  // 图片处理稍后实现
            addedReason = "手动添加"
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
            "低" -> WishlistPriority.LOW
            "普通" -> WishlistPriority.NORMAL
            "高" -> WishlistPriority.HIGH
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
     * 初始化心愿单默认字段
     * 在Fragment中调用，设置默认选中的字段
     */
    fun initializeWishlistDefaultFields() {
        android.util.Log.d("WishlistAddViewModel", "🎯 开始初始化心愿单默认字段")
        
        // 确保字段属性已设置
        android.util.Log.d("WishlistAddViewModel", "🔍 检查字段属性是否已设置，当前fieldProperties大小: ${fieldProperties.size}")
        if (fieldProperties.isEmpty()) {
            android.util.Log.w("WishlistAddViewModel", "⚠️ 字段属性未设置，先初始化字段属性")
            initializeDefaultFieldProperties()
        } else {
            android.util.Log.d("WishlistAddViewModel", "✅ 字段属性已设置完成")
        }
        
        // 创建心愿单专用的Field对象
        android.util.Log.d("WishlistAddViewModel", "🏗️ 创建心愿单Field对象")
        val wishlistFields = setOf(
            Field(group = "基本信息", name = "名称", isSelected = true, order = 1),
            Field(group = "基本信息", name = "分类", isSelected = true, order = 5),
            Field(group = "基本信息", name = "子分类", isSelected = true, order = 6),
            Field(group = "基本信息", name = "品牌", isSelected = true, order = 21),
            Field(group = "基本信息", name = "规格", isSelected = false, order = 25),
            Field(group = "价格信息", name = "单价", isSelected = true, order = 11),
            Field(group = "价格信息", name = "目标价格", isSelected = true, order = 12),
            Field(group = "价格信息", name = "个人预算", isSelected = true, order = 13),
            Field(group = "价格信息", name = "价格跟踪", isSelected = true, order = 14),
            Field(group = "购买计划", name = "优先级", isSelected = true, order = 15),
            Field(group = "购买计划", name = "紧急程度", isSelected = true, order = 16),
            Field(group = "购买计划", name = "数量", isSelected = true, order = 2),
            Field(group = "购买计划", name = "数量单位", isSelected = true, order = 3),
            Field(group = "购买计划", name = "购买计划", isSelected = false, order = 17),
            Field(group = "购买偏好", name = "首选渠道", isSelected = true, order = 18),
            Field(group = "其他", name = "备注", isSelected = true, order = 4)
        )
        
        // 设置选中的字段
        android.util.Log.d("WishlistAddViewModel", "📋 设置选中字段，总共${wishlistFields.size}个字段")
        _selectedFields.value = wishlistFields
        android.util.Log.d("WishlistAddViewModel", "✅ 选中字段设置完成: ${wishlistFields.map { it.name }}")
        
        // 设置默认值 - 映射到实际字段名
        android.util.Log.d("WishlistAddViewModel", "💾 设置默认值")
        
        // 映射默认值到UI字段名
        val defaultValueMappings = mapOf(
            "名称" to "",
            "分类" to "未分类",
            "子分类" to "",
            "品牌" to "",
            "规格" to "",
            "单价" to 0.0,
            "目标价格" to 0.0,
            "个人预算" to 0.0,
            "价格跟踪" to true,
            "优先级" to "普通",
            "紧急程度" to "普通",
            "数量" to 1.0,
            "数量单位" to "个",
            "购买计划" to "随时",
            "首选渠道" to "",
            "备注" to ""
        )
        
        defaultValueMappings.forEach { (fieldName, value) ->
            android.util.Log.d("WishlistAddViewModel", "   💾 设置默认值: $fieldName = $value")
            saveFieldValue(fieldName, value)
            
            // 验证保存结果
            val savedValue = getFieldValue(fieldName)
            android.util.Log.d("WishlistAddViewModel", "   ✔️ 验证保存结果: $fieldName = $savedValue")
        }
        
        android.util.Log.d("WishlistAddViewModel", "🎉 初始化心愿单默认字段完成")
        android.util.Log.d("WishlistAddViewModel", "   📊 选中字段数量: ${_selectedFields.value?.size}")
        android.util.Log.d("WishlistAddViewModel", "   📋 字段属性数量: ${fieldProperties.size}")
        
        // 保存到缓存
        android.util.Log.d("WishlistAddViewModel", "💽 保存数据到缓存")
        saveToCache()
        android.util.Log.d("WishlistAddViewModel", "✅ 缓存保存完成")
    }
}
