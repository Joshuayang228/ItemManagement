package com.example.itemmanagement.ui.base

import android.net.Uri
import androidx.lifecycle.*
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.ui.add.Field
import com.example.itemmanagement.ui.base.FieldInteractionViewModel
import com.example.itemmanagement.ui.common.FieldProperties
import com.example.itemmanagement.ui.common.ValidationType  
import com.example.itemmanagement.ui.common.DisplayStyle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 物品管理基础 ViewModel
 * 
 * 这是所有物品相关 ViewModel 的抽象基类，封装了通用的状态管理和缓存逻辑。
 * 子类只需要实现具体的业务逻辑（如保存、更新）即可。
 * 
 * 核心功能：
 * 1. 统一的字段状态管理（LiveData）
 * 2. 自动的缓存保存和恢复
 * 3. 通用的数据验证逻辑
 * 4. 照片和标签管理
 */
abstract class BaseItemViewModel(
    protected val repository: UnifiedItemRepository,
    protected val cacheViewModel: ItemStateCacheViewModel
) : ViewModel(), FieldInteractionViewModel {

    // --- 通用状态 LiveData ---
    
    // 字段相关
    protected val _selectedFields = MutableLiveData<Set<Field>>(setOf())
    val selectedFields: LiveData<Set<Field>> = _selectedFields

    protected val _photoUris = MutableLiveData<List<Uri>>(emptyList())
    val photoUris: LiveData<List<Uri>> = _photoUris

    protected val _selectedTags = MutableLiveData<Map<String, Set<String>>>(mapOf())
    override val selectedTags: LiveData<Map<String, Set<String>>> = _selectedTags

    // 操作状态
    protected val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    protected val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // 字段值存储
    protected var fieldValues: MutableMap<String, Any?> = mutableMapOf()
    
    // 自定义选项存储
    protected var customOptionsMap: MutableMap<String, MutableList<String>> = mutableMapOf()
    protected var customUnitsMap: MutableMap<String, MutableList<String>> = mutableMapOf()
    protected var customTagsMap: MutableMap<String, MutableList<String>> = mutableMapOf()

    // 字段属性存储
    protected val fieldProperties = mutableMapOf<String, FieldProperties>()

    // --- 抽象方法：子类必须实现 ---

    /**
     * 获取当前模式对应的缓存对象
     * AddItemViewModel 返回 addItemCache
     * EditItemViewModel 返回对应 itemId 的 editItemCache
     */
    protected abstract fun getCurrentCache(): Any

    /**
     * 保存或更新物品的具体实现
     */
    abstract suspend fun saveOrUpdateItem()

    /**
     * 获取缓存的关键标识
     * 用于日志和调试
     */
    protected abstract fun getCacheKey(): String

    /**
     * 从具体缓存类型加载数据到ViewModel
     * 子类实现具体的类型安全加载逻辑
     */
    protected abstract fun loadDataFromCache()

    /**
     * 将ViewModel数据保存到具体缓存类型
     * 子类实现具体的类型安全保存逻辑
     */
    protected abstract fun saveDataToCache()

    // --- 通用方法实现 ---

    /**
     * 从缓存加载状态到当前 ViewModel（类型安全版本）
     */
    fun loadFromCache() {
        try {
            loadDataFromCache() // 委托给子类的类型安全实现
        } catch (e: Exception) {
            // 缓存加载失败时的容错处理
            android.util.Log.w("BaseItemViewModel", "Failed to load cache for ${getCacheKey()}: ${e.message}")
            // 不抛出异常，让应用继续正常运行
        }
    }

    /**
     * 将当前状态保存到缓存（类型安全版本）
     */
    fun saveToCache() {
        try {
            saveDataToCache() // 委托给子类的类型安全实现
        } catch (e: Exception) {
            // 缓存保存失败时的容错处理
            android.util.Log.w("BaseItemViewModel", "Failed to save cache for ${getCacheKey()}: ${e.message}")
            // 不抛出异常，让应用继续正常运行
        }
    }

    /**
     * 清除当前状态和缓存
     */
    open fun clearStateAndCache() {
        // 清除当前状态
        fieldValues.clear()
        _selectedFields.value = setOf()
        _photoUris.value = emptyList()
        _selectedTags.value = mapOf()
        customOptionsMap.clear()
        customUnitsMap.clear()
        customTagsMap.clear()
        
        // 清除对应的缓存
        when (val cache = getCurrentCache()) {
            is ItemStateCacheViewModel.AddItemCache -> {
                cacheViewModel.clearAddItemCache()
            }
            is ItemStateCacheViewModel.EditItemCache -> {
                cache.originalItemId?.let { itemId ->
                    cacheViewModel.clearEditItemCache(itemId)
                }
            }
        }
    }

    // --- 字段管理方法 ---

    /**
     * 更新字段选择状态
     */
    override fun updateFieldSelection(field: Field, isSelected: Boolean) {
        val currentFields = _selectedFields.value?.toMutableSet() ?: mutableSetOf()

        if (isSelected) {
            currentFields.removeAll { it.name == field.name }
            currentFields.add(field)
        } else {
            if (field.name == "名称") {
                return // 名称字段不能取消选择
            }
            currentFields.removeAll { it.name == field.name }
        }

        _selectedFields.value = currentFields
        saveToCache() // 自动保存到缓存
    }

    /**
     * 批量设置选中字段 - 性能优化版本，只触发一次LiveData更新
     */
    fun setSelectedFields(fields: Set<Field>) {
        // 确保名称字段始终被包含
        val fieldsWithName = fields.toMutableSet()
        if (!fieldsWithName.any { it.name == "名称" }) {
            fieldsWithName.add(Field("基础信息", "名称", true, 1))
        }
        
        _selectedFields.value = fieldsWithName
        saveToCache() // 只保存一次到缓存
    }

    /**
     * 保存字段值
     */
    override fun saveFieldValue(fieldName: String, value: Any?) {
        if (value != null || fieldValues.containsKey(fieldName)) {
            fieldValues[fieldName] = value

            // 如果是真正的标签字段，更新 selectedTags（排除季节等其他TAG样式字段）
            if (getFieldProperties(fieldName).displayStyle == DisplayStyle.TAG && fieldName == "标签") {
                val currentSelectedTags = _selectedTags.value?.toMutableMap() ?: mutableMapOf()
                if (value is Set<*>) {
                    @Suppress("UNCHECKED_CAST")
                    currentSelectedTags[fieldName] = value as Set<String>
                    _selectedTags.value = currentSelectedTags
                }
            }
            
            saveToCache() // 自动保存到缓存
        }
    }

    /**
     * 获取字段值
     */
    override fun getFieldValue(fieldName: String): Any? {
        // 对于"添加日期"字段，如果没有值则返回当前日期
        if (fieldName == "添加日期" && !fieldValues.containsKey(fieldName)) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return dateFormat.format(Date())
        }

        return fieldValues[fieldName]
    }

    /**
     * 获取所有字段值
     */
    override fun getAllFieldValues(): Map<String, Any?> {
        return fieldValues.toMap()
    }
    
    /**
     * 清除单个字段的值
     */
    override fun clearFieldValue(fieldName: String) {
        fieldValues.remove(fieldName)
        saveToCache()
    }
    
    /**
     * 清除所有字段值
     */
    override fun clearAllFieldValues() {
        fieldValues.clear()
        _selectedFields.value = emptySet()
        _selectedTags.value = emptyMap()
        saveToCache()
    }
    
    /**
     * 只清除字段值，保留已选字段
     */
    override fun clearFieldValuesOnly() {
        fieldValues.clear()
        saveToCache()
    }

    // --- 字段属性管理 ---

    /**
     * 设置字段属性
     */
    override fun setFieldProperties(fieldName: String, properties: FieldProperties) {
        android.util.Log.d("BaseItemViewModel", "🔧 设置字段属性: $fieldName")
        android.util.Log.d("BaseItemViewModel", "   属性内容: $properties")
        android.util.Log.d("BaseItemViewModel", "   ValidationType: ${properties.validationType}")
        android.util.Log.d("BaseItemViewModel", "   DisplayStyle: ${properties.displayStyle}")
        android.util.Log.d("BaseItemViewModel", "   Options: ${properties.options}")
        
        val updatedProperties = properties.copy(fieldName = fieldName)
        android.util.Log.d("BaseItemViewModel", "   🔄 复制属性并设置fieldName: $updatedProperties")
        
        fieldProperties[fieldName] = updatedProperties
        android.util.Log.d("BaseItemViewModel", "   ✅ 字段属性设置完成")
        android.util.Log.d("BaseItemViewModel", "   📊 当前fieldProperties总数: ${fieldProperties.size}")
    }

    /**
     * 获取字段属性
     */
    override fun getFieldProperties(fieldName: String): FieldProperties {
        android.util.Log.d("BaseItemViewModel", "🔍 获取字段属性: $fieldName")
        android.util.Log.d("BaseItemViewModel", "   📋 当前fieldProperties总数: ${fieldProperties.size}")
        
        val properties = fieldProperties[fieldName]
        if (properties != null) {
            android.util.Log.d("BaseItemViewModel", "   ✅ 找到字段属性: $properties")
            android.util.Log.d("BaseItemViewModel", "      ValidationType: ${properties.validationType}")
            android.util.Log.d("BaseItemViewModel", "      DisplayStyle: ${properties.displayStyle}")
            android.util.Log.d("BaseItemViewModel", "      Options: ${properties.options}")
        } else {
            android.util.Log.w("BaseItemViewModel", "   ⚠️ 未找到字段属性，使用默认属性")
            android.util.Log.d("BaseItemViewModel", "   📋 当前所有字段属性键: ${fieldProperties.keys}")
        }
        
        val result = properties ?: FieldProperties()
        android.util.Log.d("BaseItemViewModel", "   📊 返回属性: $result")
        return result
    }

    /**
     * 获取所有字段属性
     */
    override fun getAllFieldProperties(): Map<String, FieldProperties> {
        android.util.Log.d("BaseItemViewModel", "🗺️ 获取所有字段属性")
        android.util.Log.d("BaseItemViewModel", "📊 字段属性总数: ${fieldProperties.size}")
        
        val result = fieldProperties.toMap()
        android.util.Log.d("BaseItemViewModel", "📋 所有字段属性键: ${result.keys}")
        return result
    }

    // --- 照片管理 ---

    /**
     * 添加照片URI
     */
    override fun addPhotoUri(uri: Uri) {
        val currentUris = _photoUris.value?.toMutableList() ?: mutableListOf()
        currentUris.add(uri)
        _photoUris.value = currentUris
        saveToCache() // 自动保存到缓存
    }

    /**
     * 移除照片URI
     */
    override fun removePhotoUri(position: Int) {
        val currentUris = _photoUris.value?.toMutableList() ?: mutableListOf()
        if (position in currentUris.indices) {
            currentUris.removeAt(position)
            _photoUris.value = currentUris
            saveToCache() // 自动保存到缓存
        }
    }

    /**
     * 获取照片URI列表
     */
    fun getPhotoUris(): List<Uri> {
        return _photoUris.value ?: listOf()
    }

    /**
     * 清空所有照片
     */
    fun clearAllPhotos() {
        _photoUris.value = emptyList()
        saveToCache()
    }

    /**
     * 设置照片URI列表（用于拖动排序后更新）
     */
    fun setPhotoUris(uris: List<Uri>) {
        _photoUris.value = uris
        saveToCache() // 自动保存到缓存
    }

    // --- 自定义选项管理 ---

    /**
     * 获取字段的自定义选项
     */
    override fun getCustomOptions(fieldName: String): MutableList<String> {
        if (!customOptionsMap.containsKey(fieldName)) {
            customOptionsMap[fieldName] = mutableListOf()
        }
        return customOptionsMap[fieldName]!!
    }

    // 子分类与分类的映射关系
    private val subCategoryMap = mutableMapOf<String, List<String>>(
        "食品" to listOf("零食", "饮料", "水果", "蔬菜", "肉类", "调味品", "干货", "速食", "乳制品"),
        "药品" to listOf("感冒药", "消炎药", "止痛药", "维生素", "胃药", "外用药", "眼药水", "创可贴"),
        "日用品" to listOf("洗护用品", "清洁用品", "厨房用品", "卫生用品", "收纳用品", "纸巾", "洗衣液"),
        "电子产品" to listOf("手机", "电脑", "相机", "耳机", "充电器", "数据线", "移动电源", "智能手表"),
        "衣物" to listOf("上衣", "裤子", "鞋子", "袜子", "内衣", "外套", "帽子", "围巾", "手套"),
        "文具" to listOf("笔", "本子", "纸张", "胶带", "剪刀", "尺子", "订书机", "文件夹"),
        "其他" to listOf("礼品", "玩具", "工具", "装饰品", "宠物用品", "植物", "书籍", "运动用品")
    )

    /**
     * 获取指定分类的子分类列表
     */
    override fun getSubCategoriesForCategory(category: String): List<String> {
        val defaultSubCategories = subCategoryMap[category] ?: emptyList()
        val customOptions = getCustomOptions("子分类")
        
        // 找出被标记为删除的选项
        val deletedOptions = customOptions.filter { it.startsWith("DELETED:") }
            .map { it.removePrefix("DELETED:") }
        
        // 找出编辑映射关系
        val editMappings = customOptions.filter { it.startsWith("EDIT:") }
            .associate { 
                val parts = it.removePrefix("EDIT:").split("->")
                if (parts.size == 2) parts[0] to parts[1] else null to null
            }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
            .mapValues { it.value!! }
        
        // 应用编辑映射和删除过滤
        val processedDefaults = defaultSubCategories
            .filter { !deletedOptions.contains(it) } // 过滤删除的选项
            .map { editMappings[it] ?: it } // 应用编辑映射
        
        // 获取真正的自定义选项（不包括删除标记和编辑映射）
        val realCustomOptions = customOptions.filter { 
            !it.startsWith("DELETED:") && !it.startsWith("EDIT:") 
        }
        
        return (processedDefaults + realCustomOptions).distinct()
    }

    /**
     * 获取原始的（未经处理的）子分类列表
     */
    override fun getOriginalSubCategoriesForCategory(category: String): List<String> {
        return subCategoryMap[category] ?: emptyList()
    }

    /**
     * 检查分类是否已选择
     */
    override fun isCategorySelected(): Boolean {
        val categoryValue = getFieldValue("分类")
        return categoryValue != null && categoryValue.toString().isNotBlank() && 
               categoryValue.toString() != "选择分类" && categoryValue.toString() != "请选择分类"
    }

    /**
     * 获取Repository（用于适配器）
     */
    override fun getItemRepository(): UnifiedItemRepository {
        return repository
    }

    /**
     * 添加字段的自定义选项
     */
    override fun addCustomOption(fieldName: String, option: String) {
        val options = getCustomOptions(fieldName)
        if (!options.contains(option)) {
            options.add(option)
            saveToCache() // 自动保存到缓存
        }
    }

    /**
     * 删除字段的自定义选项
     */
    override fun removeCustomOption(fieldName: String, option: String) {
        val options = getCustomOptions(fieldName)
        options.remove(option)
        saveToCache() // 自动保存到缓存
    }

    // --- 生命周期管理 ---

    override fun onCleared() {
        super.onCleared()
        // ViewModel 销毁时自动保存状态到缓存
        saveToCache()
    }

    /**
     * 保存成功后重置保存结果状态
     */
    fun onSaveResultConsumed() {
        _saveResult.value = null
    }

    // --- 通用的保存方法 ---

    /**
     * 执行保存操作
     */
    fun performSave() {
        viewModelScope.launch {
            try {
                saveOrUpdateItem()
                clearStateAndCache() // 保存成功后清除缓存
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "保存失败：未知错误"
                _saveResult.value = false
            }
        }
    }

    /**
     * 初始化默认字段属性
     * 由于代码较长，这里只包含核心字段，子类可以覆写添加更多字段
     */
    open fun initializeDefaultFieldProperties() {
        // 名称字段
        setFieldProperties("名称", FieldProperties(
            isRequired = true,
            validationType = ValidationType.TEXT,
            hint = "请输入名称"
        ))

        // 数量字段 - 只使用计数单位
        setFieldProperties("数量", FieldProperties(
            defaultValue = null,
            validationType = ValidationType.NUMBER,
            min = 0,
            hint = "请输入数量",
            unitOptions = listOf("个", "件", "包", "盒", "瓶", "袋", "箱"),
            isCustomizable = true,
            unit = "个"
        ))

        // 位置字段
        setFieldProperties("位置", FieldProperties(
            displayStyle = DisplayStyle.LOCATION_SELECTOR,
            hint = "请选择位置"
        ))

        // 分类字段
        setFieldProperties("分类", FieldProperties(
            options = listOf("食品", "药品", "日用品", "电子产品", "衣物", "文具", "其他"),
            isCustomizable = true
        ))

        // 子分类字段
        setFieldProperties("子分类", FieldProperties(
            options = emptyList(),
            isCustomizable = true
        ))

        // 评分字段
        setFieldProperties("评分", FieldProperties(
            displayStyle = DisplayStyle.RATING_STAR,
            validationType = ValidationType.NUMBER,
            min = 0,
            max = 5,
            hint = "请评分"
        ))

        // 季节字段 - 多选字段，类似标签
        setFieldProperties("季节", FieldProperties(
            displayStyle = DisplayStyle.TAG,
            options = listOf("春", "夏", "秋", "冬", "全年"),
            isCustomizable = true
        ))

        // 单价字段
        setFieldProperties("单价", FieldProperties(
            validationType = ValidationType.NUMBER,
            min = 0,
            hint = "请输入单价",
            unitOptions = listOf("元", "美元", "日元", "欧元"),
            isCustomizable = true,
            unit = "元"  // 默认单位为元
        ))

        // 总价字段
        setFieldProperties("总价", FieldProperties(
            validationType = ValidationType.NUMBER,
            min = 0,
            hint = "请输入总价",
            unitOptions = listOf("元", "美元", "日元", "欧元"),
            isCustomizable = true,
            unit = "元"
        ))

        // 容量字段 - 包含重量和体积单位
        setFieldProperties("容量", FieldProperties(
            validationType = ValidationType.NUMBER,
            min = 0,
            hint = "请输入容量",
            unitOptions = listOf("毫升", "升", "克", "千克", "公斤", "磅", "盎司"),
            isCustomizable = true
        ))

        // 品牌字段
        setFieldProperties("品牌", FieldProperties(
            validationType = ValidationType.TEXT,
            hint = "请输入品牌"
        ))

        // 规格字段
        setFieldProperties("规格", FieldProperties(
            validationType = ValidationType.TEXT,
            hint = "请输入规格"
        ))

        // 备注字段
        setFieldProperties("备注", FieldProperties(
            validationType = ValidationType.TEXT,
            isMultiline = true,
            maxLines = 3,
            hint = "请输入备注"
        ))

        // 日期类字段
        setFieldProperties("添加日期", FieldProperties(
            validationType = ValidationType.DATE,
            defaultDate = true,
            hint = "点击选择日期"
        ))

        setFieldProperties("生产日期", FieldProperties(
            validationType = ValidationType.DATE,
            hint = "点击选择日期"
        ))

        setFieldProperties("购买日期", FieldProperties(
            validationType = ValidationType.DATE,
            hint = "点击选择日期"
        ))

        setFieldProperties("保修到期时间", FieldProperties(
            validationType = ValidationType.DATE,
            hint = "点击选择日期"
        ))

        setFieldProperties("保质过期时间", FieldProperties(
            validationType = ValidationType.DATE,
            hint = "点击选择日期"
        ))

        // 期间选择器字段
        setFieldProperties("保修期", FieldProperties(
            displayStyle = DisplayStyle.PERIOD_SELECTOR,
            periodRange = 1..120,
            periodUnits = listOf("年", "月", "日"),
            hint = "请选择保修期",
            unit = "月"
        ))

        setFieldProperties("保质期", FieldProperties(
            displayStyle = DisplayStyle.PERIOD_SELECTOR,
            periodRange = 1..3650,
            periodUnits = listOf("年", "月", "日"),
            hint = "请选择保质期",
            unit = "月"
        ))

        // 购买渠道字段
        setFieldProperties("购买渠道", FieldProperties(
            options = listOf("淘宝", "天猫", "拼多多", "京东", "美团", "其他网购", "线下店"),
            isCustomizable = true
        ))

        // 商家名称字段
        setFieldProperties("商家名称", FieldProperties(
            validationType = ValidationType.TEXT,
            hint = "请输入商家名称"
        ))

        // 序列号字段
        setFieldProperties("序列号", FieldProperties(
            validationType = ValidationType.TEXT,
            hint = "请输入序列号"
        ))

        // 开封状态字段
        setFieldProperties("开封状态", FieldProperties(
            options = listOf("已开封", "未开封"),
            isCustomizable = false
        ))

        // 标签字段
        setFieldProperties("标签", FieldProperties(
            displayStyle = DisplayStyle.TAG,
            options = emptyList(),
            isCustomizable = true
        ))

    }
    
    /**
     * 更新选中的标签
     */
    override fun updateSelectedTags(fieldName: String, tags: Set<String>) {
        val currentSelectedTags = _selectedTags.value?.toMutableMap() ?: mutableMapOf()
        currentSelectedTags[fieldName] = tags
        _selectedTags.value = currentSelectedTags
        saveToCache() // 自动保存到缓存
    }
    
    // --- 缺失的接口方法实现 ---
    
    override fun setCustomOptions(fieldName: String, options: MutableList<String>) {
        // 通过字段属性设置自定义选项
        val currentProperties = getFieldProperties(fieldName)
        setFieldProperties(fieldName, currentProperties.copy(options = options))
        saveToCache()
    }
    
    override fun getCustomUnits(fieldName: String): MutableList<String> {
        // 返回真正的自定义单位（从持久化存储中获取），而不是字段属性中的默认单位
        // 这样避免了在 FieldViewFactory 中重复添加默认单位的问题
        return cacheViewModel.getCustomUnits(fieldName)
    }
    
    override fun setCustomUnits(fieldName: String, units: MutableList<String>) {
        val currentProperties = getFieldProperties(fieldName)
        setFieldProperties(fieldName, currentProperties.copy(unitOptions = units))
        saveToCache()
    }
    
    override fun addCustomUnit(fieldName: String, unit: String) {
        val currentUnits = getCustomUnits(fieldName)
        if (!currentUnits.contains(unit)) {
            currentUnits.add(unit)
            saveToCache()
        }
    }
    
    override fun removeCustomUnit(fieldName: String, unit: String) {
        val currentUnits = getCustomUnits(fieldName)
        if (currentUnits.remove(unit)) {
            saveToCache()
        }
    }
    
    override fun getCustomTags(fieldName: String): MutableList<String> {
        // 返回真正的自定义标签（从持久化存储中获取），而不是字段属性中的默认标签
        // 这样避免了在UI中重复显示默认标签的问题
        return cacheViewModel.getCustomTags(fieldName)
    }
    
    override fun addCustomTag(fieldName: String, tag: String) {
        val currentTags = getCustomTags(fieldName)
        if (!currentTags.contains(tag)) {
            currentTags.add(tag)
            saveToCache()
        }
    }
    
    override fun removeCustomTag(fieldName: String, tag: String) {
        val currentTags = getCustomTags(fieldName)
        if (currentTags.remove(tag)) {
            saveToCache()
        }
    }
    
    override fun updateSubCategoryOptions(category: String) {
        // 当分类改变时更新子分类选项的逻辑
        // 这里可以根据选择的分类更新相关字段的子分类选项
        saveToCache()
    }
    
    // 用于存储自定义位置数据
    private var customLocationData: com.example.itemmanagement.data.model.CustomLocationData? = null
    
    override fun saveCustomLocations(customData: com.example.itemmanagement.data.model.CustomLocationData) {
        this.customLocationData = customData
        saveToCache()
    }
    
    override fun getCustomLocations(): com.example.itemmanagement.data.model.CustomLocationData? {
        return customLocationData
    }
} 