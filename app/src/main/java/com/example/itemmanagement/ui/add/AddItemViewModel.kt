package com.example.itemmanagement.ui.add

import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.entity.ItemEntity
import com.example.itemmanagement.data.entity.LocationEntity
import com.example.itemmanagement.data.entity.PhotoEntity
import com.example.itemmanagement.data.entity.TagEntity
import com.example.itemmanagement.data.mapper.toItemEntity
import com.example.itemmanagement.data.mapper.toLocationEntity
import com.example.itemmanagement.data.mapper.toItem
import com.example.itemmanagement.data.model.Item
import com.example.itemmanagement.data.model.OpenStatus
import com.example.itemmanagement.data.model.CustomLocationData
import kotlinx.coroutines.launch
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

/**
 * 模式类型枚举
 * 用于区分当前ViewModel的工作模式
 */
enum class ItemMode {
    ADD,    // 添加模式
    EDIT    // 编辑模式
}

class AddItemViewModel(
    private val repository: ItemRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 添加模式变量，使用枚举类型
    private val _mode = MutableLiveData<ItemMode>(ItemMode.ADD)
    val mode: LiveData<ItemMode> = _mode

    private val _selectedFields = savedStateHandle.getLiveData<Set<Field>>("selected_fields", setOf())
    val selectedFields: LiveData<Set<Field>> = _selectedFields

    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // 用于保存字段值的映射
    private var fieldValues: MutableMap<String, Any?> = savedStateHandle.get<MutableMap<String, Any?>>("field_values") ?: mutableMapOf()

    // 用于保存每个字段的自定义选项
    private var customOptionsMap: MutableMap<String, MutableList<String>> = savedStateHandle.get<MutableMap<String, MutableList<String>>>("custom_options") ?: mutableMapOf()

    // 用于保存每个字段的自定义单位
    private var customUnitsMap: MutableMap<String, MutableList<String>> = savedStateHandle.get<MutableMap<String, MutableList<String>>>("custom_units") ?: mutableMapOf()

    // 用于保存每个字段的自定义标签
    private var customTagsMap: MutableMap<String, MutableList<String>> = savedStateHandle.get<MutableMap<String, MutableList<String>>>("custom_tags") ?: mutableMapOf()

    // 照片URI列表
    private val _photoUris = MutableLiveData<List<Uri>>(emptyList())
    val photoUris: LiveData<List<Uri>> = _photoUris

    // 添加一个 LiveData 来跟踪选中的标签
    private val _selectedTags = MutableLiveData<Map<String, Set<String>>>(mapOf())
    val selectedTags: LiveData<Map<String, Set<String>>> = _selectedTags

    private var editingItemId: Long? = null
    
    // 用于标记是否处于编辑模式的临时状态
    private var isInEditMode = false
    
    // 用于防止模式切换过程中的数据污染
    private var isModeSwitchInProgress = false
    
    // 用于标记是否已经初始化过（防止重复初始化）
    private var isInitialized = false

    // 字段属性定义
    data class FieldProperties(
        val fieldName: String? = null,
        val defaultValue: String? = null,
        val options: List<String>? = null,
        val min: Number? = null,
        val max: Number? = null,
        val unit: String? = null,
        val isRequired: Boolean = false,
        val validationType: ValidationType? = null,
        val hint: String? = null,
        val isMultiline: Boolean = false,
        val isCustomizable: Boolean = false,  // 是否允许自定义选项
        val defaultDate: Boolean = false,     // 是否默认选择当前日期
        val maxLines: Int? = null,           // 最大行数
        val unitOptions: List<String>? = null, // 单位选项
        val isMultiSelect: Boolean = false,   // 是否允许多选
        val displayStyle: DisplayStyle = DisplayStyle.DEFAULT, // 显示样式
        val periodRange: IntRange? = null,    // 期限范围（如1-36个月）
        val periodUnits: List<String>? = null // 期限单位（如年、月、日）
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    enum class ValidationType : Serializable {
        TEXT, NUMBER, DATE, DATETIME, EMAIL, PHONE, URL
    }

    enum class DisplayStyle : Serializable {
        DEFAULT,           // 默认显示
        TAG,              // 标签样式
        RATING_STAR,      // 评分星星
        PERIOD_SELECTOR,  // 期限选择器
        LOCATION_SELECTOR // 位置选择器（三级）
    }

    // 字段属性映射
    private val fieldProperties = mutableMapOf<String, FieldProperties>()

    // 草稿保险箱：用于保存添加模式下的草稿数据
    private var addDraftFieldValues: MutableMap<String, Any?> = savedStateHandle.get<MutableMap<String, Any?>>("add_draft_field_values") ?: mutableMapOf()
    private var addDraftSelectedFields: Set<Field> = savedStateHandle.get<Set<Field>>("add_draft_selected_fields") ?: setOf()
    private var addDraftPhotoUris: List<Uri> = savedStateHandle.get<List<Uri>>("add_draft_photo_uris") ?: emptyList()
    private var addDraftSelectedTags: Map<String, Set<String>> = savedStateHandle.get<Map<String, Set<String>>>("add_draft_selected_tags") ?: mapOf()

    // 初始化代码块，确保ViewModel创建时就初始化字段属性
    init {
        initializeDefaultFieldProperties()
        
        // 注意：初始化时不自动恢复草稿，必须通过明确的指令触发
        // 这样可以避免在Fragment重建时意外恢复草稿
    }

    fun updateFieldSelection(field: Field, isSelected: Boolean) {
        val currentFields = _selectedFields.value?.toMutableSet() ?: mutableSetOf()

        if (isSelected) {
            currentFields.removeAll { it.name == field.name }
            currentFields.add(field)
        } else {
            if (field.name == "名称") {
                return
            }
            currentFields.removeAll { it.name == field.name }
        }

        savedStateHandle["selected_fields"] = currentFields
        _selectedFields.value = currentFields
        
        // 如果是添加模式且不在模式切换过程中，同步更新草稿保险箱中的已选字段
        if (_mode.value == ItemMode.ADD && !isInEditMode && !isModeSwitchInProgress) {
            addDraftSelectedFields = currentFields
            savedStateHandle["add_draft_selected_fields"] = addDraftSelectedFields
        }
    }

    fun getSelectedFieldsValue(): Set<Field> {
        return _selectedFields.value ?: setOf()
    }

    // 设置字段属性
    fun setFieldProperties(fieldName: String, properties: FieldProperties) {
        // 确保FieldProperties包含字段名
        val updatedProperties = properties.copy(fieldName = fieldName)
        fieldProperties[fieldName] = updatedProperties
    }

    // 获取字段属性
    fun getFieldProperties(fieldName: String): FieldProperties {
        return fieldProperties[fieldName] ?: FieldProperties()
    }

    // 保存字段值
    fun saveFieldValue(fieldName: String, value: Any?) {
        // 如果值不为null，或者是明确设置为空，则更新
        if (value != null || fieldValues.containsKey(fieldName)) {
            // 直接设置到fieldValues和savedStateHandle
            fieldValues[fieldName] = value
            savedStateHandle[fieldName] = value
            
            // 如果是开封状态字段，确保同步到草稿中
            if (fieldName == "开封状态" && _mode.value == ItemMode.ADD && !isInEditMode && !isModeSwitchInProgress) {
                addDraftFieldValues[fieldName] = value
                savedStateHandle["add_draft_field_values"] = addDraftFieldValues
            }

            // 如果是标签字段，更新 selectedTags
            if (getFieldProperties(fieldName).displayStyle == DisplayStyle.TAG) {
                val currentSelectedTags = _selectedTags.value?.toMutableMap() ?: mutableMapOf()
                if (value is Set<*>) {
                    @Suppress("UNCHECKED_CAST")
                    currentSelectedTags[fieldName] = value as Set<String>
                    _selectedTags.value = currentSelectedTags
                }
            }
            
            // 如果是添加模式且不在模式切换过程中，同步更新草稿保险箱中的字段值
            if (_mode.value == ItemMode.ADD && !isInEditMode && !isModeSwitchInProgress) {
                addDraftFieldValues[fieldName] = value
                savedStateHandle["add_draft_field_values"] = addDraftFieldValues
                
                // 如果是标签字段，同步更新草稿保险箱中的已选标签
                if (getFieldProperties(fieldName).displayStyle == DisplayStyle.TAG && _selectedTags.value != null) {
                    addDraftSelectedTags = _selectedTags.value!!
                    savedStateHandle["add_draft_selected_tags"] = addDraftSelectedTags
                }
            }
        }
    }

    // 获取字段值
    fun getFieldValue(fieldName: String): Any? {
        // 对于"添加日期"字段，如果没有值则返回当前日期
        if (fieldName == "添加日期" && !fieldValues.containsKey(fieldName)) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return dateFormat.format(Date())
        }

        // 如果是开封状态字段，特殊处理
        if (fieldName == "开封状态") {
            // 首先尝试从fieldValues获取
            val fieldValue = if (fieldValues.containsKey(fieldName)) fieldValues[fieldName] else null
            // 然后尝试从savedStateHandle获取
            val stateHandleValue = savedStateHandle.get<Any>(fieldName)
            
            // 优先返回fieldValues中的值，如果没有则返回savedStateHandle中的值
            return fieldValue ?: stateHandleValue
        }

        // 确保明确返回存储的值，即使是null
        return if (fieldValues.containsKey(fieldName)) {
            fieldValues[fieldName]
        } else {
            // 尝试从savedStateHandle获取
            savedStateHandle.get<Any>(fieldName)
        }
    }
    
    // 获取当前正在编辑的物品的位置信息
    fun getEditingItemLocation(): com.example.itemmanagement.data.model.Location? {
        val area = fieldValues["位置_area"] as? String
        val container = fieldValues["位置_container"] as? String
        val sublocation = fieldValues["位置_sublocation"] as? String
        
        return if (!area.isNullOrEmpty()) {
            com.example.itemmanagement.data.model.Location(
                area = area,
                container = container,
                sublocation = sublocation
            )
        } else {
            null
        }
    }

    // 获取所有字段值
    fun getAllFieldValues(): Map<String, Any?> {
        return fieldValues.toMap()
    }

    // 清除字段值
    fun clearFieldValue(fieldName: String) {
        fieldValues.remove(fieldName)
        savedStateHandle["field_values"] = fieldValues
    }

    // 清除所有字段值
    fun clearAllFieldValues() {
        fieldValues.clear()
        savedStateHandle["field_values"] = fieldValues
    }

    // 初始化默认字段属性
    fun initializeDefaultFieldProperties() {
        // 名称字段
        setFieldProperties("名称", FieldProperties(
            isRequired = true,
            validationType = ValidationType.TEXT,
            hint = "请输入名称"
        ))

        // 数量字段
        setFieldProperties("数量", FieldProperties(
            defaultValue = null,
            validationType = ValidationType.NUMBER,
            min = 0,
            hint = "请输入数量",
            unitOptions = listOf("个", "件", "包", "盒", "瓶", "袋", "箱", "克", "千克", "升", "毫升"),
            isCustomizable = true
        ))

        // 位置字段
        setFieldProperties("位置", FieldProperties(
            displayStyle = DisplayStyle.LOCATION_SELECTOR,
            hint = "请选择位置"
        ))

        // 备注字段
        setFieldProperties("备注", FieldProperties(
            validationType = ValidationType.TEXT,
            hint = "请输入备注",
            isMultiline = true,
            maxLines = 5
        ))

        // 分类字段
        setFieldProperties("分类", FieldProperties(
            options = listOf("食品", "药品", "日用品", "电子产品", "衣物", "文具", "其他"),
            isCustomizable = true
        ))

        // 子分类字段 - 修改为下拉框形式
        setFieldProperties("子分类", FieldProperties(
            options = emptyList(), // 初始为空，将根据选择的分类动态加载
            isCustomizable = true
        ))

        // 标签字段
        setFieldProperties("标签", FieldProperties(
            displayStyle = DisplayStyle.TAG,
            isMultiSelect = true,
            isCustomizable = true,
            options = listOf("重要", "易碎", "易腐", "贵重", "常用")
        ))

        // 季节字段
        setFieldProperties("季节", FieldProperties(
            displayStyle = DisplayStyle.TAG,
            isMultiSelect = true,
            options = listOf("春", "夏", "秋", "冬")
        ))

        // 容量字段
        setFieldProperties("容量", FieldProperties(
            validationType = ValidationType.NUMBER,
            hint = "请输入容量",
            unitOptions = listOf("克", "千克", "升", "毫升"),
            isCustomizable = true
        ))

        // 评分字段
        setFieldProperties("评分", FieldProperties(
            displayStyle = DisplayStyle.RATING_STAR
        ))

        // 单价字段
        setFieldProperties("单价", FieldProperties(
            validationType = ValidationType.NUMBER,
            hint = "请输入价格",
            unitOptions = listOf("元", "美元", "日元", "欧元"),
            isCustomizable = true
        ))

        // 总价字段
        setFieldProperties("总价", FieldProperties(
            validationType = ValidationType.NUMBER,
            hint = "请输入总价",
            unitOptions = listOf("元", "美元", "日元", "欧元"),
            isCustomizable = true
        ))

        // 添加日期字段
        setFieldProperties("添加日期", FieldProperties(
            validationType = ValidationType.DATE,
            defaultDate = true
        ))

        // 开封时间字段
        setFieldProperties("开封时间", FieldProperties(
            validationType = ValidationType.DATE,
            defaultDate = false
        ))

        // 购买日期字段
        setFieldProperties("购买日期", FieldProperties(
            validationType = ValidationType.DATE,
            defaultDate = false
        ))

        // 生产日期字段
        setFieldProperties("生产日期", FieldProperties(
            validationType = ValidationType.DATE,
            defaultDate = false
        ))

        // 保质期字段
        setFieldProperties("保质期", FieldProperties(
            displayStyle = DisplayStyle.PERIOD_SELECTOR,
            periodRange = 1..36,
            periodUnits = listOf("年", "月", "日")
        ))

        // 保质过期时间字段
        setFieldProperties("保质过期时间", FieldProperties(
            validationType = ValidationType.DATE,
            defaultDate = false
        ))

        // 保修期字段
        setFieldProperties("保修期", FieldProperties(
            displayStyle = DisplayStyle.PERIOD_SELECTOR,
            periodRange = 1..36,
            periodUnits = listOf("年", "月", "日")
        ))

        // 保修到期时间字段
        setFieldProperties("保修到期时间", FieldProperties(
            validationType = ValidationType.DATE,
            defaultDate = false
        ))

        // 品牌字段
        setFieldProperties("品牌", FieldProperties(
            validationType = ValidationType.TEXT,
            hint = "请输入品牌"
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

        // 加入心愿单字段 - Switch类型
        setFieldProperties("加入心愿单", FieldProperties(
            validationType = ValidationType.TEXT,
            defaultValue = "false",
            hint = "是否加入心愿单"
        ))

        // 高周转字段 - Switch类型  
        setFieldProperties("高周转", FieldProperties(
            validationType = ValidationType.TEXT,
            defaultValue = "false", 
            hint = "是否为高周转物品"
        ))
        
        // 为缺失的字段添加属性定义
        // 规格字段
        setFieldProperties("规格", FieldProperties(
            validationType = ValidationType.TEXT,
            hint = "请输入规格"
        ))
        
        // 单位字段（虽然通常作为数量的一部分，但有时也单独使用）
        setFieldProperties("单位", FieldProperties(
            options = listOf("个", "件", "包", "盒", "瓶", "袋", "箱", "克", "千克", "升", "毫升"),
            isCustomizable = true
        ))
        
        // 开封状态字段
        setFieldProperties("开封状态", FieldProperties(
            options = listOf("已开封", "未开封"),
            isCustomizable = false
        ))
        
        // 到期日期字段
        setFieldProperties("到期日期", FieldProperties(
            validationType = ValidationType.DATE,
            defaultDate = false
        ))
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

    // 获取指定分类的子分类列表
    fun getSubCategoriesForCategory(category: String): List<String> {
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

    // 获取原始的（未经处理的）子分类列表
    fun getOriginalSubCategoriesForCategory(category: String): List<String> {
        return subCategoryMap[category] ?: emptyList()
    }

    // 检查分类是否已选择
    fun isCategorySelected(): Boolean {
        val categoryValue = getFieldValue("分类")
        return categoryValue != null && categoryValue.toString().isNotBlank() && 
               categoryValue.toString() != "选择分类" && categoryValue.toString() != "请选择分类"
    }

    // 更新子分类选项
    fun updateSubCategoryOptions(category: String) {
        val defaultSubCategories = getSubCategoriesForCategory(category)
        val properties = getFieldProperties("子分类")
        
        // 保留现有的自定义选项，只更新默认选项
        val existingCustomOptions = getCustomOptions("子分类")
        
        // 设置新的默认选项，但保留自定义选项
        setFieldProperties("子分类", properties.copy(options = defaultSubCategories))
    }

    // 获取指定区域的容器列表（包含自定义选项）
    suspend fun getContainersForArea(area: String): List<String> {
        return try {
            val defaultContainers = repository.getContainersByArea(area)
            val customContainers = getCustomOptions("位置_容器_${area}")
            (defaultContainers + customContainers).distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 获取指定区域和容器的子位置列表（包含自定义选项）
    suspend fun getSublocationsForContainer(area: String, container: String): List<String> {
        return try {
            val defaultSublocations = repository.getSublocations(area, container)
            val customSublocations = getCustomOptions("位置_子位置_${area}_${container}")
            (defaultSublocations + customSublocations).distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 获取所有区域列表（包含自定义选项）
    suspend fun getAllAreas(): List<String> {
        return try {
            val defaultAreas = repository.getAllLocationAreas()
            val customAreas = getCustomOptions("位置_区域")
            (defaultAreas + customAreas).distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 获取字段的自定义选项
    fun getCustomOptions(fieldName: String): MutableList<String> {
        if (!customOptionsMap.containsKey(fieldName)) {
            customOptionsMap[fieldName] = mutableListOf()
            savedStateHandle["custom_options"] = customOptionsMap
        }
        return customOptionsMap[fieldName]!!
    }

    // 设置字段的自定义选项
    fun setCustomOptions(fieldName: String, options: MutableList<String>) {
        customOptionsMap[fieldName] = options
        savedStateHandle["custom_options"] = customOptionsMap
    }

    // 添加字段的自定义选项
    fun addCustomOption(fieldName: String, option: String) {
        val options = getCustomOptions(fieldName)
        if (!options.contains(option)) {
            options.add(option)
            setCustomOptions(fieldName, options)
        }
    }

    // 删除字段的自定义选项
    fun removeCustomOption(fieldName: String, option: String) {
        val options = getCustomOptions(fieldName)
        options.remove(option)
        setCustomOptions(fieldName, options)
    }

    // 获取字段的自定义单位
    fun getCustomUnits(fieldName: String): MutableList<String> {
        if (!customUnitsMap.containsKey(fieldName)) {
            customUnitsMap[fieldName] = mutableListOf()
            savedStateHandle["custom_units"] = customUnitsMap
        }
        return customUnitsMap[fieldName]!!
    }

    // 设置字段的自定义单位
    fun setCustomUnits(fieldName: String, units: MutableList<String>) {
        customUnitsMap[fieldName] = units
        savedStateHandle["custom_units"] = customUnitsMap
    }

    // 添加字段的自定义单位
    fun addCustomUnit(fieldName: String, unit: String) {
        val units = getCustomUnits(fieldName)
        if (!units.contains(unit)) {
            units.add(unit)
            setCustomUnits(fieldName, units)
        }
    }

    // 删除字段的自定义单位
    fun removeCustomUnit(fieldName: String, unit: String) {
        val units = getCustomUnits(fieldName)
        units.remove(unit)
        setCustomUnits(fieldName, units)
    }

    // 获取字段的自定义标签
    fun getCustomTags(fieldName: String): MutableList<String> {
        if (!customTagsMap.containsKey(fieldName)) {
            customTagsMap[fieldName] = mutableListOf()
            savedStateHandle["custom_tags"] = customTagsMap
        }
        return customTagsMap[fieldName]!!
    }

    // 设置字段的自定义标签
    fun setCustomTags(fieldName: String, tags: MutableList<String>) {
        customTagsMap[fieldName] = tags
        savedStateHandle["custom_tags"] = customTagsMap
    }

    // 添加字段的自定义标签
    fun addCustomTag(fieldName: String, tag: String) {
        val tags = getCustomTags(fieldName)
        if (!tags.contains(tag)) {
            tags.add(tag)
            setCustomTags(fieldName, tags)
        }
    }

    // 删除字段的自定义标签
    fun removeCustomTag(fieldName: String, tag: String) {
        val tags = getCustomTags(fieldName)
        tags.remove(tag)
        setCustomTags(fieldName, tags)

        // 从已选中的标签中移除被删除的标签
        val currentSelectedTags = _selectedTags.value?.toMutableMap() ?: mutableMapOf()
        currentSelectedTags[fieldName]?.let { selectedTags ->
            if (selectedTags.contains(tag)) {
                val updatedTags = selectedTags.toMutableSet()
                updatedTags.remove(tag)
                currentSelectedTags[fieldName] = updatedTags
                _selectedTags.value = currentSelectedTags
                // 同时更新 fieldValues
                saveFieldValue(fieldName, updatedTags)
            }
        }
    }

    /**
     * 验证Item对象的数据有效性
     * @param item 要验证的Item对象
     * @return Pair<Boolean, String?> 第一个值表示验证是否通过，第二个值为错误信息（如果验证失败）
     */
    private fun validateItem(item: Item): Pair<Boolean, String?> {
        return when {
            item.name.isBlank() -> {
                Pair(false, "物品名称不能为空")
            }
            item.quantity <= 0.0 -> {
                Pair(false, "数量必须大于0")
            }
            item.category.isEmpty() -> {
                Pair(false, "类别不能为空")
            }
            item.productionDate != null && item.expirationDate != null &&
                    item.expirationDate.before(item.productionDate) -> {
                Pair(false, "过期日期必须晚于生产日期")
            }
            else -> Pair(true, null)
        }
    }

    /**
     * 设置要编辑的物品
     * @param item 要编辑的物品
     */
    fun setEditingItem(item: Item) {
        editingItemId = item.id
        // 将物品的字段值设置到表单中
        item.name.let { saveFieldValue("名称", it) }
        item.quantity.let { saveFieldValue("数量", it.toString()) }
        item.unit?.let { saveFieldValue("单位", it) }
        
        // 只有当分类不是"未指定"时才保存分类字段
        if (!item.category.isNullOrBlank() && item.category != "未指定") {
            saveFieldValue("分类", item.category)
        }
        
        // 只有当位置区域不是"未指定"或空时才保存位置字段
        item.location?.let { location ->
            if (!location.area.isNullOrBlank() && location.area != "未指定") {
                saveFieldValue("位置_area", location.area)
                location.container?.let { if (it.isNotBlank()) saveFieldValue("位置_container", it) }
                location.sublocation?.let { if (it.isNotBlank()) saveFieldValue("位置_sublocation", it) }
            }
        }
        
        item.productionDate?.let { saveFieldValue("生产日期", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)) }
        item.expirationDate?.let { saveFieldValue("保质过期时间", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)) }
        item.openStatus?.let { saveFieldValue("开封状态", if (it == OpenStatus.OPENED) "已开封" else "未开封") }
        item.openDate?.let { saveFieldValue("开封时间", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)) }
        
        // 其他字段 - 只保存非空且非"未指定"的值
        item.brand?.let { if (it.isNotBlank() && it != "未指定") saveFieldValue("品牌", it) }
        item.specification?.let { if (it.isNotBlank() && it != "未指定") saveFieldValue("规格", it) }
        item.price?.let { saveFieldValue("单价", it.toString()) }
        item.purchaseChannel?.let { if (it.isNotBlank() && it != "未指定") saveFieldValue("购买渠道", it) }
        item.storeName?.let { if (it.isNotBlank() && it != "未指定") saveFieldValue("商家名称", it) }
        item.subCategory?.let { if (it.isNotBlank() && it != "未指定") saveFieldValue("子分类", it) }
        item.customNote?.let { if (it.isNotBlank()) saveFieldValue("备注", it) }
        
        // 季节字段 - 只保存非空且非"未指定"的值
        item.season?.let { if (it.isNotBlank() && it != "未指定") saveFieldValue("季节", it) }
        
        item.capacity?.let { saveFieldValue("容量", it.toString()) }
        item.rating?.let { saveFieldValue("评分", it.toString()) }
        item.totalPrice?.let { saveFieldValue("总价", it.toString()) }
        item.purchaseDate?.let { saveFieldValue("购买日期", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)) }
    }

    /**
     * 保存或更新物品
     * @param item 要保存的物品
     */
    fun saveItem(item: Item) {
        viewModelScope.launch {
            try {
                // 数据验证
                val (isValid, errorMessage) = validateItem(item)
                if (!isValid) {
                    _errorMessage.value = errorMessage ?: "未知错误"
                    _saveResult.value = false
                    return@launch
                }

                // 验证通过，保存数据
                val itemEntity = item.toItemEntity()
                
                // 准备位置实体（如果有）
                val locationEntity = item.location?.let { 
                    // 如果区域为"未指定"且是编辑模式，检查是否应该保留现有位置
                    if (it.area == "未指定" && editingItemId != null) {
                        // 获取现有位置信息
                        val existingArea = fieldValues["位置_area"] as? String
                        val existingContainer = fieldValues["位置_container"] as? String
                        val existingSublocation = fieldValues["位置_sublocation"] as? String
                        
                        if (!existingArea.isNullOrEmpty() && existingArea != "未指定") {
                            LocationEntity(
                                id = 0, // 数据库会处理ID
                                area = existingArea,
                                container = existingContainer,
                                sublocation = existingSublocation
                            )
                        } else {
                            it.toLocationEntity()
                        }
                    } else if (it.area != "未指定") {
                        // 如果有有效的位置信息，转换为实体
                        it.toLocationEntity()
                    } else {
                        // 如果位置是"未指定"，返回null
                        null
                    }
                }
                
                // 记录位置信息
                
                // 准备照片实体
                val photoEntities = mutableListOf<PhotoEntity>()
                
                // 将_photoUris中的URI转换为PhotoEntity
                _photoUris.value?.forEachIndexed { index, uri ->
                    photoEntities.add(
                        PhotoEntity(
                            itemId = editingItemId ?: 0, // 如果是新增模式，itemId会在DAO中被更新
                            uri = uri.toString(),
                            isMain = index == 0, // 第一张照片设为主照片
                            displayOrder = index
                        )
                    )
                }
                
                // 准备标签实体
                val tagEntities = mutableListOf<TagEntity>()
                
                item.tags.forEach { tag ->
                    tagEntities.add(TagEntity(name = tag.name, color = tag.color))
                }
                
                if (editingItemId != null) {
                    // 更新模式
                    repository.updateItemWithDetails(editingItemId!!, itemEntity, locationEntity, photoEntities, tagEntities)
                    _saveResult.value = true
                    _errorMessage.value = "物品更新成功"
                } else {
                    // 新增模式
                    val id = repository.insertItemWithDetails(itemEntity, locationEntity, photoEntities, tagEntities)
                    
                    if (id > 0) {
                        _saveResult.value = true
                        _errorMessage.value = "物品保存成功"
                    } else {
                        _errorMessage.value = "保存失败：数据库插入返回无效ID"
                        _saveResult.value = false
                    }
                }
            } catch (e: Exception) {
                Log.e("AddItemViewModel", if (editingItemId != null) "更新物品失败" else "保存物品失败", e)
                _errorMessage.value = e.message ?: if (editingItemId != null) "更新失败：未知错误" else "保存失败：未知错误"
                _saveResult.value = false
            }
        }
    }

    // 保存自定义位置数据
    fun saveCustomLocations(customData: CustomLocationData) {
        try {
            savedStateHandle["custom_locations"] = customData
        } catch (e: Exception) {
        }
    }

    // 获取自定义位置数据
    fun getCustomLocations(): CustomLocationData? {
        return try {
            val data = savedStateHandle.get<CustomLocationData>("custom_locations")
            data
        } catch (e: Exception) {
            null
        }
    }

    // 添加图片URI管理方法
    fun addPhotoUri(uri: Uri) {
        val currentUris = _photoUris.value?.toMutableList() ?: mutableListOf()
        currentUris.add(uri)
        savedStateHandle["photo_uris"] = currentUris
        _photoUris.value = currentUris
        
        // 如果是添加模式且不在模式切换过程中，同步更新草稿保险箱中的照片
        if (_mode.value == ItemMode.ADD && !isInEditMode && !isModeSwitchInProgress) {
            addDraftPhotoUris = currentUris
            savedStateHandle["add_draft_photo_uris"] = addDraftPhotoUris
        }
    }

    fun removePhotoUri(position: Int) {
        val currentUris = _photoUris.value?.toMutableList() ?: mutableListOf()
        if (position in currentUris.indices) {
            currentUris.removeAt(position)
            savedStateHandle["photo_uris"] = currentUris
            _photoUris.value = currentUris
            
            // 如果是添加模式且不在模式切换过程中，同步更新草稿保险箱中的照片
            if (_mode.value == ItemMode.ADD && !isInEditMode && !isModeSwitchInProgress) {
                addDraftPhotoUris = currentUris
                savedStateHandle["add_draft_photo_uris"] = addDraftPhotoUris
            }
        }
    }

    fun getPhotoUris(): List<Uri> {
        return _photoUris.value ?: listOf()
    }

    fun clearPhotos() {
        _photoUris.value = emptyList()
        savedStateHandle["photo_uris"] = emptyList<Uri>()
    }

    fun clearAllData() {
        // 清除所有字段值
        fieldValues.clear()
        savedStateHandle.remove<MutableMap<String, Any?>>("field_values")

        // 清除位置相关的数据
        savedStateHandle.remove<String>("位置_area")
        savedStateHandle.remove<String>("位置_container")
        savedStateHandle.remove<String>("位置_sublocation")
        
        // 清除开封状态数据
        savedStateHandle.remove<String>("开封状态")

        // 清除所有照片
        _photoUris.value = emptyList()
        savedStateHandle.remove<List<Uri>>("photo_uris")
        
        // 清除已选中的字段列表
        _selectedFields.value = emptySet()
        savedStateHandle.remove<Set<Field>>("selected_fields")
        
        // 清除已选中的标签
        _selectedTags.value = emptyMap()
    }

    /**
     * 只清除字段值和照片，但保留已选中的字段
     * 用于清除按钮功能
     */
    fun clearFieldValuesOnly() {
        // 清除所有字段值
        fieldValues.clear()
        savedStateHandle.remove<MutableMap<String, Any?>>("field_values")

        // 清除位置相关的数据
        savedStateHandle.remove<String>("位置_area")
        savedStateHandle.remove<String>("位置_container")
        savedStateHandle.remove<String>("位置_sublocation")
        
        // 确保从fieldValues中也删除位置相关数据
        fieldValues.remove("位置_area")
        fieldValues.remove("位置_container")
        fieldValues.remove("位置_sublocation")
        
        // 清除开封状态数据
        savedStateHandle.remove<String>("开封状态")
        // 确保从fieldValues中也删除开封状态数据
        fieldValues.remove("开封状态")
        
        // 清除单位相关的数据
        val fieldsWithUnits = listOf("数量", "容量", "单价", "总价")
        fieldsWithUnits.forEach { fieldName ->
            savedStateHandle.remove<String>("${fieldName}_unit")
        }

        // 清除所有照片
        _photoUris.value = emptyList()
        savedStateHandle.remove<List<Uri>>("photo_uris")
        
        // 清除已选中的标签
        _selectedTags.value = emptyMap()
        savedStateHandle.remove<Map<String, Set<String>>>("selected_tags")
        
        // 注意：不清除已选中的字段列表 _selectedFields
    }

    // 将当前状态保存到草稿保险箱
    private fun saveCurrentStateToAddDraft() {
        // 只有在添加模式下才保存草稿
        // 注意：允许在模式切换过程中保存草稿，这是必要的
        if (_mode.value == ItemMode.ADD) {
            // 保存字段值
            addDraftFieldValues = HashMap(fieldValues)
            savedStateHandle["add_draft_field_values"] = addDraftFieldValues
            
            // 保存已选字段
            addDraftSelectedFields = _selectedFields.value ?: setOf()
            savedStateHandle["add_draft_selected_fields"] = addDraftSelectedFields
            
            // 保存照片URI
            addDraftPhotoUris = _photoUris.value ?: emptyList()
            savedStateHandle["add_draft_photo_uris"] = addDraftPhotoUris
            
            // 保存已选标签
            addDraftSelectedTags = _selectedTags.value ?: mapOf()
            savedStateHandle["add_draft_selected_tags"] = addDraftSelectedTags
            
            // 记录日志，便于调试
        }
    }
    
    // 从草稿保险箱恢复状态
    private fun restoreStateFromAddDraft() {
        // 设置模式切换标志，防止数据污染
        isModeSwitchInProgress = true
        
        try {
            // 清除当前状态
            clearAllData()
            
            // 恢复字段值
            fieldValues = HashMap(addDraftFieldValues)
            savedStateHandle["field_values"] = fieldValues
            
            // 恢复已选字段
            _selectedFields.value = addDraftSelectedFields
            savedStateHandle["selected_fields"] = addDraftSelectedFields
            
            // 恢复照片URI
            _photoUris.value = addDraftPhotoUris
            savedStateHandle["photo_uris"] = addDraftPhotoUris
            
            // 恢复已选标签
            _selectedTags.value = addDraftSelectedTags
            savedStateHandle["selected_tags"] = addDraftSelectedTags
            
            // 恢复位置相关数据
            addDraftFieldValues["位置_area"]?.let { savedStateHandle["位置_area"] = it }
            addDraftFieldValues["位置_container"]?.let { savedStateHandle["位置_container"] = it }
            addDraftFieldValues["位置_sublocation"]?.let { savedStateHandle["位置_sublocation"] = it }
            
            // 恢复开封状态数据
            if (addDraftFieldValues.containsKey("开封状态")) {
                val openStatusValue = addDraftFieldValues["开封状态"]
                savedStateHandle["开封状态"] = openStatusValue
                // 强制设置到fieldValues中
                fieldValues["开封状态"] = openStatusValue
            }
            
            // 记录日志，便于调试
        } finally {
            // 无论如何都要重置模式切换标志
            isModeSwitchInProgress = false
        }
    }

    /**
     * 设置ViewModel的工作模式
     * @param mode 模式字符串，"add"或"edit"
     */
    fun setMode(mode: String) {
        val newMode = if (mode == "edit") ItemMode.EDIT else ItemMode.ADD
        
        // 如果模式没有变化，不做任何操作
        if (_mode.value == newMode) {
            return
        }
        
        // 设置模式切换标志，防止数据污染
        isModeSwitchInProgress = true
        
        try {
            // 如果从编辑模式切换到添加模式，清除编辑状态
            if (_mode.value == ItemMode.EDIT && newMode == ItemMode.ADD) {
                isInEditMode = false
                editingItemId = null
                
                // 恢复添加模式的草稿
                restoreAddModeDraft()
            }
            // 如果从添加模式切换到编辑模式，保存添加模式的草稿
            else if (_mode.value == ItemMode.ADD && newMode == ItemMode.EDIT) {
                isInEditMode = true
                
                // 保存添加模式的草稿
                saveCurrentStateToAddDraft()
                
                // 清除当前工作区数据，准备加载编辑数据
                clearAllData()
            }
            
            // 更新模式
            _mode.value = newMode
            
            // 标记已初始化
            isInitialized = true
        } finally {
            // 无论如何都要重置模式切换标志
            isModeSwitchInProgress = false
        }
    }

    /**
     * 准备添加模式
     * 恢复草稿或初始化默认字段
     */
    fun prepareForAddMode() {
        // 设置模式切换标志，防止数据污染
        isModeSwitchInProgress = true
        
        try {
            // 设置为添加模式
            _mode.value = ItemMode.ADD
            isInEditMode = false
            editingItemId = null
            
            // 如果有草稿，恢复草稿
            if (addDraftSelectedFields.isNotEmpty()) {
                restoreAddModeDraft()
            } else {
                // 初始化默认字段
                initializeDefaultFields()
            }
            
            // 标记已初始化
            isInitialized = true
        } finally {
            // 无论如何都要重置模式切换标志
            isModeSwitchInProgress = false
        }
    }
    
    /**
     * 初始化默认字段
     */
    private fun initializeDefaultFields() {
        initializeDefaultFieldProperties()
        
        // 创建一个有序的默认字段列表，包含分类和顺序
        val defaultFields = listOf(
            Field("基础信息", "名称", true),
            Field("基础信息", "数量", false),
            Field("基础信息", "位置", false),
            Field("分类", "分类", false),
            Field("日期类", "生产日期", false),
            Field("日期类", "保质过期时间", false),
            Field("日期类", "添加日期", false),
            Field("基础信息", "加入心愿单", false),
            Field("基础信息", "高周转", false)
        )
        
        // 清除当前选中的字段
        val currentFields = _selectedFields.value?.toMutableSet() ?: mutableSetOf()
        currentFields.clear()
        
        // 添加默认字段
        defaultFields.forEach { field ->
            currentFields.add(field)
        }
        
        // 更新选中的字段
        savedStateHandle["selected_fields"] = currentFields
        _selectedFields.value = currentFields
    }



    /**
     * 恢复添加模式草稿
     */
    private fun restoreAddModeDraft() {
        if (isModeSwitchInProgress) return
        
        // 设置模式切换标志，防止数据污染
        isModeSwitchInProgress = true
        
        try {
            // 清除当前数据
            clearAllData()
            
            // 恢复字段值
            fieldValues = addDraftFieldValues.toMutableMap()
            savedStateHandle["field_values"] = fieldValues
            
            // 恢复已选字段
            _selectedFields.value = addDraftSelectedFields
            savedStateHandle["selected_fields"] = addDraftSelectedFields
            
            // 恢复照片URI
            _photoUris.value = addDraftPhotoUris
            
            // 恢复已选标签
            _selectedTags.value = addDraftSelectedTags
        } finally {
            // 无论如何都要重置模式切换标志
            isModeSwitchInProgress = false
        }
    }

    /**
     * 保存物品成功后清理添加模式的草稿
     * 这个方法应该在物品成功保存到数据库后调用
     */
    fun clearAddDraftAfterSave() {
        addDraftFieldValues.clear()
        addDraftSelectedFields = setOf()
        addDraftPhotoUris = emptyList()
        addDraftSelectedTags = mapOf()
        
        // 保存到SavedStateHandle
        savedStateHandle["add_draft_field_values"] = addDraftFieldValues
        savedStateHandle["add_draft_selected_fields"] = addDraftSelectedFields
        savedStateHandle["add_draft_photo_uris"] = addDraftPhotoUris
        savedStateHandle["add_draft_selected_tags"] = addDraftSelectedTags
    }

    /**
     * 从导航参数加载物品数据
     * @param item 要加载的物品对象
     * 注意：此方法应该只在用户明确点击某个物品进行编辑时调用，通常在导航到编辑界面之前
     */
    fun loadItemData(item: Item) {
        
        // 设置模式切换标志，防止数据污染
        isModeSwitchInProgress = true
        
        try {
            // 如果当前是添加模式，先保存草稿
            if (_mode.value == ItemMode.ADD) {
                saveCurrentStateToAddDraft()
            }
            
            // 设置为编辑模式
            _mode.value = ItemMode.EDIT
            editingItemId = item.id
            // 设置编辑模式标记
            isInEditMode = true
            
            // 清除当前工作区数据
            clearAllData()
            
            // 将物品的字段值设置到表单中
            item.name.let { saveFieldValue("名称", it) }
            
            // 处理数量字段 - 如果是整数，去掉小数点后的.0
            item.quantity.let { 
                val quantityStr = if (it == it.toInt().toDouble()) {
                    it.toInt().toString()
                } else {
                    it.toString()
                }
                saveFieldValue("数量", quantityStr) 
            }
            
            // 保存单位
            item.unit?.let { saveFieldValue("单位", it) }
            item.unit?.let { saveFieldValue("数量_unit", it) }
            
            // 只有当分类不是"未指定"时才保存分类字段
            if (!item.category.isNullOrBlank() && item.category != "未指定") {
                saveFieldValue("分类", item.category)
            }
            
            // 只有当位置区域不是"未指定"或空时才保存位置字段
            item.location?.let { location ->
                if (!location.area.isNullOrBlank() && location.area != "未指定") {
                    saveFieldValue("位置_area", location.area)
                    location.container?.let { if (it.isNotBlank()) saveFieldValue("位置_container", it) }
                    location.sublocation?.let { if (it.isNotBlank()) saveFieldValue("位置_sublocation", it) }
                }
            }
            
            // 日期类字段
            item.productionDate?.let { saveFieldValue("生产日期", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)) }
            item.expirationDate?.let { saveFieldValue("保质过期时间", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)) }
            item.addDate.let { saveFieldValue("添加日期", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)) }
            
            // 开封状态
            item.openStatus?.let { 
                val openStatusValue = if (it == OpenStatus.OPENED) "已开封" else "未开封"
                saveFieldValue("开封状态", openStatusValue)
                // 确保同时保存到savedStateHandle
                savedStateHandle["开封状态"] = openStatusValue
            }
            item.openDate?.let { saveFieldValue("开封时间", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)) }
            
            // 其他字段 - 只保存非空且非"未指定"的值
            item.brand?.let { if (it.isNotBlank() && it != "未指定") saveFieldValue("品牌", it) }
            item.specification?.let { if (it.isNotBlank() && it != "未指定") saveFieldValue("规格", it) }
            
            // 处理单价字段 - 如果是整数，去掉小数点后的.0
            item.price?.let { 
                val priceStr = if (it == it.toInt().toDouble()) {
                    it.toInt().toString()
                } else {
                    it.toString()
                }
                saveFieldValue("单价", priceStr)
                item.priceUnit?.let { unit -> saveFieldValue("单价_unit", unit) }
            }
            
            // 其他字段 - 只保存非空且非"未指定"的值
            item.purchaseChannel?.let { if (it.isNotBlank() && it != "未指定") saveFieldValue("购买渠道", it) }
            item.storeName?.let { if (it.isNotBlank() && it != "未指定") saveFieldValue("商家名称", it) }
            item.subCategory?.let { if (it.isNotBlank() && it != "未指定") saveFieldValue("子分类", it) }
            item.customNote?.let { if (it.isNotBlank()) saveFieldValue("备注", it) }
            
            // 处理季节字段 - 将逗号分隔的字符串转换为Set
            item.season?.let { 
                if (it.isNotBlank() && it != "未指定") {
                    val seasonSet = it.split(",").map { s -> s.trim() }.toSet()
                    saveFieldValue("季节", seasonSet)
                }
            }
            
            // 处理容量字段 - 如果是整数，去掉小数点后的.0
            item.capacity?.let { 
                val capacityStr = if (it == it.toInt().toDouble()) {
                    it.toInt().toString()
                } else {
                    it.toString()
                }
                saveFieldValue("容量", capacityStr)
                item.capacityUnit?.let { unit -> saveFieldValue("容量_unit", unit) }
            }
            
            // 评分字段
            item.rating?.let { saveFieldValue("评分", it) }
            
            // 处理总价字段 - 如果是整数，去掉小数点后的.0
            item.totalPrice?.let { 
                val totalPriceStr = if (it == it.toInt().toDouble()) {
                    it.toInt().toString()
                } else {
                    it.toString()
                }
                saveFieldValue("总价", totalPriceStr)
                item.totalPriceUnit?.let { unit -> saveFieldValue("总价_unit", unit) }
            }
            
            item.purchaseDate?.let { saveFieldValue("购买日期", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)) }
            
            // 处理保质期字段 - 保存为Pair<String, String>格式
            item.shelfLife?.let {
                val days = it
                val pair = when {
                    days % 365 == 0 -> Pair(days / 365, "年")
                    days % 30 == 0 -> Pair(days / 30, "月")
                    else -> Pair(days, "日")
                }
                saveFieldValue("保质期", Pair(pair.first.toString(), pair.second))
            }
            
            // 处理保修期字段 - 保存为Pair<String, String>格式
            item.warrantyPeriod?.let {
                val days = it
                val pair = when {
                    days % 365 == 0 -> Pair(days / 365, "年")
                    days % 30 == 0 -> Pair(days / 30, "月")
                    else -> Pair(days, "日")
                }
                saveFieldValue("保修期", Pair(pair.first.toString(), pair.second))
            }
            
            item.warrantyEndDate?.let { saveFieldValue("保修到期时间", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)) }
            item.serialNumber?.let { if (it.isNotBlank()) saveFieldValue("序列号", it) }
            
            // 处理心愿单和高周转字段
            saveFieldValue("加入心愿单", item.isWishlistItem.toString())
            saveFieldValue("高周转", item.isHighTurnover.toString())
            
            // 处理标签数据
            if (item.tags.isNotEmpty()) {
                val tagSet = item.tags.map { it.name }.toSet()
                saveFieldValue("标签", tagSet)
            }
            
            // 将Photo对象转换为Uri列表
            val uris = item.photos.map { Uri.parse(it.uri) }
            _photoUris.value = uris
            
            // 创建一个全新的字段集合，而不是在现有基础上添加
            val fieldsToAdd = mutableSetOf<Field>()
            
            // 添加必要的字段
            fieldsToAdd.add(Field("基础信息", "名称", true))
            if (item.quantity > 0) fieldsToAdd.add(Field("基础信息", "数量", true))
            if (!item.category.isNullOrBlank() && item.category != "未指定") fieldsToAdd.add(Field("分类", "分类", true))
            if (item.location != null && !item.location.area.isNullOrBlank() && item.location.area != "未指定") fieldsToAdd.add(Field("基础信息", "位置", true))
            if (item.productionDate != null) fieldsToAdd.add(Field("日期类", "生产日期", true))
            if (item.expirationDate != null) fieldsToAdd.add(Field("日期类", "保质过期时间", true))
            if (item.openStatus != null) fieldsToAdd.add(Field("基础信息", "开封状态", true))
            if (item.openDate != null) fieldsToAdd.add(Field("日期类", "开封时间", true))
            if (!item.brand.isNullOrBlank() && item.brand != "未指定") fieldsToAdd.add(Field("商业类", "品牌", true))
            if (!item.specification.isNullOrBlank() && item.specification != "未指定") fieldsToAdd.add(Field("基础信息", "规格", true))
            if (item.price != null) fieldsToAdd.add(Field("数字类", "单价", true))
            if (!item.purchaseChannel.isNullOrBlank() && item.purchaseChannel != "未指定") fieldsToAdd.add(Field("商业类", "购买渠道", true))
            if (!item.storeName.isNullOrBlank() && item.storeName != "未指定") fieldsToAdd.add(Field("商业类", "商家名称", true))
            if (!item.subCategory.isNullOrBlank() && item.subCategory != "未指定") fieldsToAdd.add(Field("分类", "子分类", true))
            if (!item.customNote.isNullOrBlank()) fieldsToAdd.add(Field("其他", "备注", true))
            // 总是添加心愿单和高周转字段，因为它们有默认值
            fieldsToAdd.add(Field("基础信息", "加入心愿单", true))
            fieldsToAdd.add(Field("基础信息", "高周转", true))
            if (!item.season.isNullOrBlank() && item.season != "未指定") fieldsToAdd.add(Field("分类", "季节", true))
            if (item.capacity != null) fieldsToAdd.add(Field("数字类", "容量", true))
            if (item.rating != null) fieldsToAdd.add(Field("数字类", "评分", true))
            if (item.totalPrice != null) fieldsToAdd.add(Field("数字类", "总价", true))
            if (item.purchaseDate != null) fieldsToAdd.add(Field("日期类", "购买日期", true))
            if (item.shelfLife != null) fieldsToAdd.add(Field("日期类", "保质期", true))
            if (item.warrantyPeriod != null) fieldsToAdd.add(Field("日期类", "保修期", true))
            if (item.warrantyEndDate != null) fieldsToAdd.add(Field("日期类", "保修到期时间", true))
            if (!item.serialNumber.isNullOrBlank()) fieldsToAdd.add(Field("商业类", "序列号", true))
            if (item.tags.isNotEmpty()) fieldsToAdd.add(Field("分类", "标签", true))
            // 始终添加添加日期字段
            fieldsToAdd.add(Field("日期类", "添加日期", true))
            
            // 更新选中的字段（直接替换，而不是追加）
            savedStateHandle["selected_fields"] = fieldsToAdd
            _selectedFields.value = fieldsToAdd
            
            // 标记为已初始化
            isInitialized = true
            
        } finally {
            // 无论如何都要重置模式切换标志
            isModeSwitchInProgress = false
        }
    }

    /**
     * 根据ID加载物品
     * @param itemId 物品ID
     */
    fun loadItemById(itemId: Long) {
        
        viewModelScope.launch {
            try {
                // 设置模式切换标志，防止数据污染
                isModeSwitchInProgress = true
                
                // 如果当前是添加模式，先保存草稿
                if (_mode.value == ItemMode.ADD) {
                    saveCurrentStateToAddDraft()
                }
                
                val item = repository.getItemById(itemId)
                item?.let {
                    // 设置为编辑模式
                    _mode.value = ItemMode.EDIT
                    editingItemId = it.id
                    // 设置编辑模式标记
                    isInEditMode = true
                    
                    // 在编辑模式下，总是清除所有数据
                    clearAllData()
                    
                    // 调用已有的loadItemData方法加载数据
                    loadItemData(it)
                }
            } catch (e: Exception) {
                _errorMessage.value = "加载物品失败：${e.message}"
            } finally {
                // 无论如何都要重置模式切换标志
                isModeSwitchInProgress = false
            }
        }
    }

    /**
     * 检查ViewModel是否已经初始化
     * 如果未初始化，Fragment应该调用prepareForAddMode()或loadItemById()进行初始化
     */
    fun isViewModelInitialized(): Boolean {
        return isInitialized
    }

    /**
     * 从编辑模式返回到添加模式
     */
    fun returnFromEditToAdd() {
        // 设置模式切换标志，防止数据污染
        isModeSwitchInProgress = true
        
        try {
            // 清除所有数据
            clearAllData()
            
            // 清除编辑ID
            editingItemId = null
            
            // 重置编辑模式标记
            isInEditMode = false
            
            // 设置为添加模式
            _mode.value = ItemMode.ADD
            
            // 恢复添加模式的草稿
            if (addDraftSelectedFields.isNotEmpty()) {
                restoreAddModeDraft()
            } else {
                // 如果没有草稿，初始化默认字段
                initializeDefaultFields()
            }
        } finally {
            // 无论如何都要重置模式切换标志
            isModeSwitchInProgress = false
        }
    }
    
    /**
     * 检查当前是否处于编辑模式
     */
    fun isInEditMode(): Boolean {
        return isInEditMode
    }
    
    /**
     * 获取当前正在编辑的物品ID
     * @return 物品ID，如果不是编辑模式则返回null
     */
    fun getEditingItemId(): Long? {
        return editingItemId
    }
    
    /**
     * 当保存结果被UI消费后，调用此方法重置状态
     */
    fun onSaveResultConsumed() {
        _saveResult.value = null
    }

    // 获取当前正在编辑的物品
    fun getEditingItem(): Item? {
        return try {
            editingItemId?.let { id ->
                // 尝试从内存中获取已加载的物品
                // _loadedItem.value // This line was not in the original file, so it's not added.
                // Assuming _loadedItem.value is a LiveData<Item> that holds the item loaded by loadItemData
                // For now, we'll return null as _loadedItem.value is not defined in the original file.
                // If _loadedItem.value is meant to be a LiveData<Item>, it needs to be declared.
                // For now, we'll return null as per the original file's state.
                null // Placeholder: _loadedItem.value is not defined
            }
        } catch (e: Exception) {
            Log.e("AddItemViewModel", "获取编辑中物品失败", e)
            null
        }
    }

    /**
     * 从购物清单项目预填充表单数据
     * 这个方法将购物清单中的信息自动填入相应的字段，减少用户重复输入
     */
    fun prepareFormFromShoppingItem(shoppingItemEntity: com.example.itemmanagement.data.entity.ShoppingItemEntity) {
        viewModelScope.launch {
            try {
                // 基础信息填充
                saveFieldValue("名称", shoppingItemEntity.name)
                saveFieldValue("数量", shoppingItemEntity.quantity)
                saveFieldValue("分类", shoppingItemEntity.category)
                
                // 可选信息填充
                shoppingItemEntity.customNote?.let { notes ->
                    saveFieldValue("自定义备注", notes)
                }
                
                shoppingItemEntity.brand?.let { brand ->
                    saveFieldValue("品牌", brand)
                }
                
                shoppingItemEntity.specification?.let { spec ->
                    saveFieldValue("规格", spec)
                }
                
                shoppingItemEntity.subCategory?.let { subCat ->
                    saveFieldValue("子分类", subCat)
                }
                
                // 价格信息填充
                shoppingItemEntity.estimatedPrice?.let { price ->
                    saveFieldValue("预估价格", price)
                }
                
                shoppingItemEntity.actualPrice?.let { actualPrice ->
                    saveFieldValue("实际价格", actualPrice)
                }
                
                shoppingItemEntity.totalPrice?.let { total ->
                    saveFieldValue("总价", total)
                }
                
                // 购买信息填充
                shoppingItemEntity.purchaseChannel?.let { channel ->
                    saveFieldValue("购买渠道", channel)
                }
                
                shoppingItemEntity.storeName?.let { store ->
                    saveFieldValue("商店名称", store)
                }
                
                // 容量信息填充
                shoppingItemEntity.capacity?.let { capacity ->
                    saveFieldValue("容量", capacity)
                }
                
                shoppingItemEntity.capacityUnit?.let { unit ->
                    saveFieldValue("容量单位", unit)
                }
                
                // 评分
                shoppingItemEntity.rating?.let { rating ->
                    saveFieldValue("评分", rating)
                }
                
                // 深度填充：如果购物项目有源物品ID，从源物品加载更多信息
                shoppingItemEntity.sourceItemId?.let { sourceItemId ->
                    val originalItemWithDetails = repository.getItemWithDetailsById(sourceItemId)
                    originalItemWithDetails?.let { itemWithDetails ->
                        val item = itemWithDetails.toItem()
                        
                        // 从原始物品填充更多字段（如果购物清单中没有指定的话）
                        if (shoppingItemEntity.brand.isNullOrBlank()) {
                            item.brand?.let { brandValue: String ->
                                saveFieldValue("品牌", brandValue)
                            }
                        }
                        
                        if (shoppingItemEntity.specification.isNullOrBlank()) {
                            item.specification?.let { specValue: String ->
                                saveFieldValue("规格", specValue)
                            }
                        }
                        
                        // 填充其他有用的信息（从源物品补充）
                        
                        // 如果原物品有标签，也可以预设标签
                        if (item.tags.isNotEmpty()) {
                            val tagNames = item.tags.map { tag: com.example.itemmanagement.data.model.Tag -> tag.name }.toSet()
                            saveFieldValue("标签", tagNames)
                        }
                        
                        // 预设默认的存放位置（可能用户想放在同一个地方）
                        item.location?.let { locationValue: com.example.itemmanagement.data.model.Location ->
                            // 简单地保存位置字符串到备注中，而不是使用复杂的CustomLocationData
                            val locationText = locationValue.getFullLocationString()
                            saveFieldValue("存放位置备注", "建议存放位置：$locationText")
                        }
                        
                        // 预设价格单位和总价单位
                        item.priceUnit?.let { priceUnitValue: String ->
                            saveFieldValue("价格单位", priceUnitValue)
                        }
                        
                        item.totalPriceUnit?.let { totalPriceUnitValue: String ->
                            saveFieldValue("总价单位", totalPriceUnitValue)
                        }
                        
                        // 预设容量单位
                        item.capacityUnit?.let { capacityUnitValue: String ->
                            saveFieldValue("容量单位", capacityUnitValue)
                        }
                    }
                }
                
                // 设置当前日期为添加日期
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                saveFieldValue("添加日期", dateFormat.format(Date()))
                
                Log.d("AddItemViewModel", "成功从购物清单项目预填充表单数据: ${shoppingItemEntity.name}")
                
            } catch (e: Exception) {
                Log.e("AddItemViewModel", "从购物清单预填充数据失败", e)
                _errorMessage.value = "预填充数据失败: ${e.message}"
            }
        }
    }
    
    /**
     * 获取Repository实例（供其他Fragment使用）
     */
    fun getRepository(): com.example.itemmanagement.data.ItemRepository = repository
    

}