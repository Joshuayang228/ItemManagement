package com.example.itemmanagement.ui.add

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.model.Item
import kotlinx.coroutines.launch

class AddItemViewModel : ViewModel() {

    private val _selectedFields = MutableLiveData<Set<Field>>(setOf())
    val selectedFields: LiveData<Set<Field>> = _selectedFields

    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // 字段属性定义
    data class FieldProperties(
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
    )

    enum class ValidationType {
        TEXT, NUMBER, DATE, DATETIME, EMAIL, PHONE, URL
    }

    enum class DisplayStyle {
        DEFAULT,           // 默认显示
        TAG,              // 标签样式
        RATING_STAR,      // 评分星星
        PERIOD_SELECTOR,  // 期限选择器
        LOCATION_SELECTOR // 位置选择器（三级）
    }

    // 字段属性映射
    private val fieldProperties = mutableMapOf<String, FieldProperties>()

    fun updateFieldSelection(field: Field, isSelected: Boolean) {
        val currentFields = _selectedFields.value?.toMutableSet() ?: mutableSetOf()

        if (isSelected) {
            // 如果已存在同名字段，先移除
            currentFields.removeAll { it.name == field.name }
            currentFields.add(field)
        } else {
            // 不允许取消选择"名称"字段
            if (field.name == "名称") {
                return
            }
            currentFields.removeAll { it.name == field.name }
        }

        _selectedFields.value = currentFields
    }

    fun getSelectedFieldsValue(): Set<Field> {
        return _selectedFields.value ?: setOf()
    }

    // 设置字段属性
    fun setFieldProperties(fieldName: String, properties: FieldProperties) {
        fieldProperties[fieldName] = properties
    }

    // 获取字段属性
    fun getFieldProperties(fieldName: String): FieldProperties {
        return fieldProperties[fieldName] ?: FieldProperties()
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
            defaultValue = "1",
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
            defaultDate = true
        ))

        // 购买日期字段
        setFieldProperties("购买日期", FieldProperties(
            validationType = ValidationType.DATE,
            defaultDate = true
        ))

        // 生产日期字段
        setFieldProperties("生产日期", FieldProperties(
            validationType = ValidationType.DATE,
            defaultDate = true
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
            defaultDate = true
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
            defaultDate = true
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
    }

    fun saveItem(item: Item) {
        viewModelScope.launch {
            try {
                // TODO: 实现保存物品到数据库的逻辑
                _saveResult.value = true
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "保存失败"
                _saveResult.value = false
            }
        }
    }
}