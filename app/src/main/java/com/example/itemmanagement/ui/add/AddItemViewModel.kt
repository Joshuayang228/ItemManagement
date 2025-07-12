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
import com.example.itemmanagement.data.model.Item
import com.example.itemmanagement.data.model.OpenStatus
import com.example.itemmanagement.data.model.CustomLocationData
import kotlinx.coroutines.launch
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

class AddItemViewModel(
    private val repository: ItemRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 添加模式变量
    private val _mode = MutableLiveData<String>("add")
    val mode: LiveData<String> = _mode

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
        Log.d("AddItemViewModel", "ViewModel已创建，等待指令")
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
        if (!isInEditMode && !isModeSwitchInProgress) {
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
            fieldValues[fieldName] = value
            savedStateHandle["field_values"] = fieldValues

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
            if (!isInEditMode && !isModeSwitchInProgress) {
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

        // 确保明确返回存储的值，即使是null
        return if (fieldValues.containsKey(fieldName)) {
            fieldValues[fieldName]
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
        
        // 为缺失的字段添加属性定义
        // 规格字段
        setFieldProperties("规格", FieldProperties(
            validationType = ValidationType.TEXT,
            hint = "请输入规格"
        ))
        
        // 子分类字段
        setFieldProperties("子分类", FieldProperties(
            validationType = ValidationType.TEXT,
            hint = "请输入子分类",
            isCustomizable = true
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
        item.category.let { saveFieldValue("分类", it) }
        item.location?.let { location ->
            saveFieldValue("位置_area", location.area)
            saveFieldValue("位置_container", location.container)
            saveFieldValue("位置_sublocation", location.sublocation)
        }
        item.productionDate?.let { saveFieldValue("生产日期", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)) }
        item.expirationDate?.let { saveFieldValue("保质过期时间", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)) }
        item.openStatus?.let { saveFieldValue("开封状态", if (it == OpenStatus.OPENED) "已开封" else "未开封") }
        item.openDate?.let { saveFieldValue("开封时间", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)) }
        item.brand?.let { saveFieldValue("品牌", it) }
        item.specification?.let { saveFieldValue("规格", it) }
        item.price?.let { saveFieldValue("单价", it.toString()) }
        item.purchaseChannel?.let { saveFieldValue("购买渠道", it) }
        item.storeName?.let { saveFieldValue("商家名称", it) }
        item.subCategory?.let { saveFieldValue("子分类", it) }
        item.customNote?.let { saveFieldValue("备注", it) }
        item.season?.let { saveFieldValue("季节", it) }
        item.capacity?.let { saveFieldValue("容量", it.toString()) }
        item.rating?.let { saveFieldValue("评分", it.toString()) }
        item.totalPrice?.let { saveFieldValue("总价", it.toString()) }
        item.purchaseDate?.let { saveFieldValue("购买日期", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)) }
        item.shelfLife?.let { saveFieldValue("保质期", it.toString()) }
        item.warrantyPeriod?.let { saveFieldValue("保修期", it.toString()) }
        item.warrantyEndDate?.let { saveFieldValue("保修到期时间", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)) }
        item.serialNumber?.let { saveFieldValue("序列号", it) }
        
        // 将Photo对象转换为Uri列表
        val uris = item.photos.map { Uri.parse(it.uri) }
        _photoUris.value = uris
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
                val locationEntity = item.location?.let { it.toLocationEntity() }
                
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
        if (!isInEditMode && !isModeSwitchInProgress) {
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
            if (!isInEditMode && !isModeSwitchInProgress) {
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

        // 清除所有照片
        _photoUris.value = emptyList()
        savedStateHandle.remove<List<Uri>>("photo_uris")
        
        // 清除已选中的标签
        _selectedTags.value = emptyMap()
        
        // 注意：不清除已选中的字段列表 _selectedFields
    }

    // 将当前状态保存到草稿保险箱
    private fun saveCurrentStateToAddDraft() {
        // 只有在添加模式下才保存草稿
        // 注意：允许在模式切换过程中保存草稿，这是必要的
        if (!isInEditMode) {
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
            Log.d("AddItemViewModel", "草稿已保存: ${fieldValues.size} 个字段值, ${_selectedFields.value?.size ?: 0} 个已选字段")
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
            
            // 记录日志，便于调试
            Log.d("AddItemViewModel", "草稿已恢复: ${fieldValues.size} 个字段值, ${_selectedFields.value?.size ?: 0} 个已选字段")
        } finally {
            // 无论如何都要重置模式切换标志
            isModeSwitchInProgress = false
        }
    }

    /**
     * 设置操作模式
     * @param newMode 模式："add" 或 "edit"
     * 警告：请勿直接调用此方法来切换模式，应该使用 prepareForAddMode() 或 loadItemById() 方法
     */
    fun setMode(newMode: String) {
        if (newMode == "add" || newMode == "edit") {
            _mode.value = newMode
        }
    }

    /**
     * 重置为添加模式，但保留草稿数据
     * 警告：此方法已弃用，请使用prepareForAddMode()代替
     * 警告：请勿在Fragment的生命周期方法中调用此方法，否则可能导致数据污染
     */
    @Deprecated("此方法可能导致数据污染，请使用prepareForAddMode()代替")
    fun resetToAddMode() {
        // 警告：此方法已弃用，请使用 prepareForAddMode()
        Log.w("AddItemViewModel", "resetToAddMode() 已弃用，请使用 prepareForAddMode()")
        
        // 重置为添加模式
        setMode("add")
        // 清除编辑ID
        editingItemId = null
        // 重置编辑模式标记
        isInEditMode = false
        // 注意：我们不清除字段值，以保留草稿功能
    }
    
    /**
     * 智能准备添加模式
     * 恢复添加模式的草稿数据
     * 注意：此方法应该只在用户明确点击"添加物品"按钮时调用，通常在导航到添加界面之前
     */
    fun prepareForAddMode() {
        Log.d("AddItemViewModel", "收到指令：准备添加模式")
        
        // 设置模式切换标志，防止数据污染
        isModeSwitchInProgress = true
        
        try {
            // 从草稿保险箱恢复状态
            restoreStateFromAddDraft()
            
            // 如果草稿保险箱是空的（首次使用），设置默认的添加模式字段
            if (addDraftSelectedFields.isEmpty()) {
                Log.d("AddItemViewModel", "草稿为空，设置默认字段")
                val defaultFields = setOf(
                    Field("基础信息", "名称", true),
                    Field("基础信息", "数量", true),
                    Field("基础信息", "位置", true),
                    Field("分类", "分类", true),
                    Field("数字类", "评分", true),
                    Field("日期类", "添加日期", true)
                )
                _selectedFields.value = defaultFields
                savedStateHandle["selected_fields"] = defaultFields
                
                // 同步更新草稿保险箱
                addDraftSelectedFields = defaultFields
                savedStateHandle["add_draft_selected_fields"] = defaultFields
            }
            
            // 设置为添加模式
            setMode("add")
            // 清除编辑ID
            editingItemId = null
            // 重置编辑模式标记
            isInEditMode = false
            
            // 标记为已初始化
            isInitialized = true
            
            Log.d("AddItemViewModel", "添加模式准备完成")
        } finally {
            // 无论如何都要重置模式切换标志
            isModeSwitchInProgress = false
        }
    }

    /**
     * 从导航参数加载物品数据
     * @param item 要加载的物品对象
     * 注意：此方法应该只在用户明确点击某个物品进行编辑时调用，通常在导航到编辑界面之前
     */
    fun loadItemData(item: Item) {
        Log.d("AddItemViewModel", "收到指令：加载物品数据，ID=${item.id}")
        
        // 设置模式切换标志，防止数据污染
        isModeSwitchInProgress = true
        
        try {
            // 如果当前是添加模式，先保存草稿
            if (!isInEditMode) {
                Log.d("AddItemViewModel", "从添加模式切换到编辑模式，保存草稿")
                saveCurrentStateToAddDraft()
            }
            
            // 设置为编辑模式
            setMode("edit")
            editingItemId = item.id
            // 设置编辑模式标记
            isInEditMode = true
            
            // 清除当前工作区数据
            clearAllData()
            
            // 将物品的字段值设置到表单中
            item.name.let { saveFieldValue("名称", it) }
            item.quantity.let { saveFieldValue("数量", it.toString()) }
            item.unit?.let { saveFieldValue("单位", it) }
            item.category.let { saveFieldValue("分类", it) }
            item.location?.let { location ->
                saveFieldValue("位置_area", location.area)
                saveFieldValue("位置_container", location.container)
                saveFieldValue("位置_sublocation", location.sublocation)
            }
            item.productionDate?.let { saveFieldValue("生产日期", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)) }
            item.expirationDate?.let { saveFieldValue("到期日期", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)) }
            item.openStatus?.let { saveFieldValue("开封状态", if (it == OpenStatus.OPENED) "已开封" else "未开封") }
            item.openDate?.let { saveFieldValue("开封时间", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)) }
            item.brand?.let { saveFieldValue("品牌", it) }
            item.specification?.let { saveFieldValue("规格", it) }
            item.price?.let { saveFieldValue("单价", it.toString()) }
            item.purchaseChannel?.let { saveFieldValue("购买渠道", it) }
            item.storeName?.let { saveFieldValue("商家名称", it) }
            item.subCategory?.let { saveFieldValue("子分类", it) }
            item.customNote?.let { saveFieldValue("备注", it) }
            item.season?.let { saveFieldValue("季节", it) }
            item.capacity?.let { saveFieldValue("容量", it.toString()) }
            item.rating?.let { saveFieldValue("评分", it.toString()) }
            item.totalPrice?.let { saveFieldValue("总价", it.toString()) }
            item.purchaseDate?.let { saveFieldValue("购买日期", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)) }
            item.shelfLife?.let { saveFieldValue("保质期", it.toString()) }
            item.warrantyPeriod?.let { saveFieldValue("保修期", it.toString()) }
            item.warrantyEndDate?.let { saveFieldValue("保修到期时间", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)) }
            item.serialNumber?.let { saveFieldValue("序列号", it) }
            
            // 将Photo对象转换为Uri列表
            val uris = item.photos.map { Uri.parse(it.uri) }
            _photoUris.value = uris
            
            // 创建一个全新的字段集合，而不是在现有基础上添加
            val fieldsToAdd = mutableSetOf<Field>()
            
            // 添加必要的字段
            fieldsToAdd.add(Field("基础信息", "名称", true))
            if (item.quantity > 0) fieldsToAdd.add(Field("基础信息", "数量", true))
            if (!item.category.isNullOrBlank()) fieldsToAdd.add(Field("分类", "分类", true))
            if (item.location != null) fieldsToAdd.add(Field("基础信息", "位置", true))
            if (item.productionDate != null) fieldsToAdd.add(Field("日期类", "生产日期", true))
            if (item.expirationDate != null) fieldsToAdd.add(Field("日期类", "保质过期时间", true))
            if (item.openDate != null) fieldsToAdd.add(Field("日期类", "开封时间", true))
            if (!item.brand.isNullOrBlank()) fieldsToAdd.add(Field("商业类", "品牌", true))
            if (!item.specification.isNullOrBlank()) fieldsToAdd.add(Field("基础信息", "规格", true))
            if (item.price != null) fieldsToAdd.add(Field("数字类", "单价", true))
            if (!item.purchaseChannel.isNullOrBlank()) fieldsToAdd.add(Field("商业类", "购买渠道", true))
            if (!item.storeName.isNullOrBlank()) fieldsToAdd.add(Field("商业类", "商家名称", true))
            if (!item.subCategory.isNullOrBlank()) fieldsToAdd.add(Field("分类", "子分类", true))
            if (!item.customNote.isNullOrBlank()) fieldsToAdd.add(Field("其他", "备注", true))
            if (!item.season.isNullOrBlank()) fieldsToAdd.add(Field("分类", "季节", true))
            if (item.capacity != null) fieldsToAdd.add(Field("数字类", "容量", true))
            if (item.rating != null) fieldsToAdd.add(Field("数字类", "评分", true))
            if (item.totalPrice != null) fieldsToAdd.add(Field("数字类", "总价", true))
            if (item.purchaseDate != null) fieldsToAdd.add(Field("日期类", "购买日期", true))
            if (item.shelfLife != null) fieldsToAdd.add(Field("日期类", "保质期", true))
            if (item.warrantyPeriod != null) fieldsToAdd.add(Field("日期类", "保修期", true))
            if (item.warrantyEndDate != null) fieldsToAdd.add(Field("日期类", "保修到期时间", true))
            if (!item.serialNumber.isNullOrBlank()) fieldsToAdd.add(Field("商业类", "序列号", true))
            
            // 更新选中的字段（直接替换，而不是追加）
            savedStateHandle["selected_fields"] = fieldsToAdd
            _selectedFields.value = fieldsToAdd
            
            // 标记为已初始化
            isInitialized = true
            
            Log.d("AddItemViewModel", "编辑模式准备完成，物品ID=${item.id}")
        } finally {
            // 无论如何都要重置模式切换标志
            isModeSwitchInProgress = false
        }
    }

    /**
     * 根据ID加载物品数据
     * @param itemId 物品ID
     * 注意：此方法应该只在用户明确点击某个物品进行编辑时调用，通常在导航到编辑界面之前
     */
    fun loadItemById(itemId: Long) {
        Log.d("AddItemViewModel", "收到指令：根据ID加载物品，ID=$itemId")
        
        viewModelScope.launch {
            try {
                // 设置模式切换标志，防止数据污染
                isModeSwitchInProgress = true
                
                // 如果当前是添加模式，先保存草稿
                if (!isInEditMode) {
                    Log.d("AddItemViewModel", "从添加模式切换到编辑模式，保存草稿")
                    saveCurrentStateToAddDraft()
                }
                
                val item = repository.getItemById(itemId)
                item?.let {
                    // 设置为编辑模式
                    setMode("edit")
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
     * 保存物品成功后清理添加模式的草稿
     * 这个方法应该在物品成功保存到数据库后调用
     */
    fun clearAddDraftAfterSave() {
        // 只有在添加模式下才清理草稿
        if (!isInEditMode) {
            // 清空草稿保险箱
            addDraftFieldValues.clear()
            savedStateHandle.remove<MutableMap<String, Any?>>("add_draft_field_values")
            
            addDraftSelectedFields = emptySet()
            savedStateHandle.remove<Set<Field>>("add_draft_selected_fields")
            
            addDraftPhotoUris = emptyList()
            savedStateHandle.remove<List<Uri>>("add_draft_photo_uris")
            
            addDraftSelectedTags = emptyMap()
            savedStateHandle.remove<Map<String, Set<String>>>("add_draft_selected_tags")
            
            Log.d("AddItemViewModel", "物品保存成功，草稿已清理")
        }
    }
    
    /**
     * 从编辑模式返回到添加模式
     * 这个方法应该在用户从编辑界面返回到主界面，再进入添加界面时调用
     * 它会恢复之前保存的草稿数据，而不是保留编辑模式的数据
     */
    fun returnToAddMode() {
        Log.d("AddItemViewModel", "收到指令：从编辑模式返回到添加模式")
        
        // 设置模式切换标志，防止数据污染
        isModeSwitchInProgress = true
        
        try {
            // 清除当前的编辑状态
            editingItemId = null
            isInEditMode = false
            
            // 从草稿保险箱恢复状态
            restoreStateFromAddDraft()
            
            // 设置为添加模式
            setMode("add")
            
            Log.d("AddItemViewModel", "已从编辑模式返回到添加模式，并恢复草稿")
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
}