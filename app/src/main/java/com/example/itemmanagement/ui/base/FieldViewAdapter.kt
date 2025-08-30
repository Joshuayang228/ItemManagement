package com.example.itemmanagement.ui.base

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.itemmanagement.R
import com.example.itemmanagement.ui.add.DialogFactory
import com.example.itemmanagement.ui.add.Field
import com.example.itemmanagement.ui.common.FieldProperties
import com.example.itemmanagement.ui.common.ValidationType
import com.example.itemmanagement.ui.common.DisplayStyle

/**
 * 字段视图适配器 (新架构)
 * 直接基于BaseItemViewModel构建字段视图和管理逻辑
 * 移除了对旧AddItemViewModel的依赖
 */
class FieldViewAdapter(
    private val context: Context,
    private val baseViewModel: BaseItemViewModel,
    private val dialogFactory: DialogFactory,
    private val resources: android.content.res.Resources
) {
    
    // 使用 AddItemViewModel 中的类型定义
    
    /**
     * 获取字段属性配置
     */
    fun getFieldProperties(fieldName: String): FieldProperties {
        return baseViewModel.getFieldProperties(fieldName) ?: FieldProperties()
    }
    
    /**
     * 创建字段视图（简化版本，基于新架构）
     */
    fun createFieldView(field: Field): View {
        val properties = getFieldProperties(field.name)
        return createSimpleFieldView(field, properties)
    }
    
    /**
     * 创建简化的字段视图
     */
    private fun createSimpleFieldView(field: Field, properties: FieldProperties): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val padding = (8 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }
        
        // 添加字段标签
        val label = TextView(context).apply {
            text = field.name
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.purple_700))
        }
        container.addView(label)
        
        // 根据字段类型创建输入控件
        val inputView = when (properties.validationType) {
            ValidationType.NUMBER -> createNumberInput(properties)
            ValidationType.DATE -> createDateInput(field.name)
            else -> createTextInput(properties)
        }
        
        container.addView(inputView)
        return container
    }
    
    private fun createTextInput(properties: FieldProperties): View {
        return android.widget.EditText(context).apply {
            hint = properties.hint?.ifEmpty { "请输入" } ?: "请输入"
            if (properties.isMultiline) {
                minLines = 3
                maxLines = 5
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }
    
    private fun createNumberInput(properties: FieldProperties): View {
        return android.widget.EditText(context).apply {
            hint = "请输入数字"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }
    
    private fun createDateInput(fieldName: String): View {
        return android.widget.Button(context).apply {
            text = "选择日期"
            setOnClickListener {
                // 简化的日期选择
                val calendar = java.util.Calendar.getInstance()
                val datePickerDialog = android.app.DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val selectedDate = "$year-${month + 1}-$dayOfMonth"
                        text = selectedDate
                        baseViewModel.saveFieldValue(fieldName, selectedDate)
                    },
                    calendar.get(java.util.Calendar.YEAR),
                    calendar.get(java.util.Calendar.MONTH),
                    calendar.get(java.util.Calendar.DAY_OF_MONTH)
                )
                datePickerDialog.show()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }
    
    /**
     * 创建分组标题（使用原有的样式）
     */
    fun createGroupHeader(container: LinearLayout, groupName: String) {
        val headerView = TextView(context).apply {
            text = groupName
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, R.color.purple_700))
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding / 2)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(headerView)
    }
    
    /**
     * 创建字段选择对话框（使用原有的样式）
     */
    fun createFieldSelectionDialog(
        availableFields: List<Field>,
        selectedFields: Set<Field>,
        onFieldSelectionChanged: (List<Field>) -> Unit
    ) {
        // 创建简单的多选对话框
        val fieldNames = availableFields.map { it.name }.toTypedArray()
        val checkedItems = availableFields.map { field ->
            selectedFields.any { it.name == field.name && it.isSelected }
        }.toBooleanArray()

        dialogFactory.createMultiChoiceDialog(
            title = "选择字段",
            items = fieldNames,
            checkedItems = checkedItems
        ) { updatedCheckedItems ->
            // 更新字段选择状态
            val newSelectedFields = availableFields.mapIndexed { index, field ->
                val isSelected = updatedCheckedItems[index]
                field.copy(isSelected = isSelected)
            }
            
            // 同步到ViewModel
            newSelectedFields.forEach { field ->
                baseViewModel.updateFieldSelection(field, field.isSelected)
            }
            
            // 调用回调
            onFieldSelectionChanged(newSelectedFields)
        }
    }
    
    /**
     * 设置字段值
     */
    fun setFieldValue(fieldView: View, fieldName: String, value: Any?) {
        // 保存到ViewModel
        baseViewModel.saveFieldValue(fieldName, value)
        
        // 更新UI控件
        try {
            val editText = findEditTextInView(fieldView)
            editText?.setText(value?.toString() ?: "")
            
            val button = findButtonInView(fieldView)
            if (button != null && value != null) {
                button.text = value.toString()
            }
        } catch (e: Exception) {
            // 如果无法设置值，先忽略错误
        }
    }
    
    /**
     * 在视图中查找EditText
     */
    private fun findEditTextInView(view: View): android.widget.EditText? {
        if (view is android.widget.EditText) {
            return view
        }
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                val result = findEditTextInView(child)
                if (result != null) return result
            }
        }
        return null
    }
    
    /**
     * 在视图中查找Button
     */
    private fun findButtonInView(view: View): android.widget.Button? {
        if (view is android.widget.Button) {
            return view
        }
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                val result = findButtonInView(child)
                if (result != null) return result
            }
        }
        return null
    }
    
    /**
     * 保存所有字段值
     */
    fun saveFieldValues(fieldViews: Map<String, View>) {
        fieldViews.forEach { (fieldName, view) ->
            try {
                val editText = findEditTextInView(view)
                if (editText != null) {
                    val value = editText.text.toString()
                    if (value.isNotBlank()) {
                        baseViewModel.saveFieldValue(fieldName, value)
                    }
                }
            } catch (e: Exception) {
                // 忽略保存错误
            }
        }
    }
    
    /**
     * 恢复字段值
     */
    fun restoreFieldValues(fieldViews: Map<String, View>) {
        fieldViews.forEach { (fieldName, view) ->
            try {
                val value = baseViewModel.getFieldValue(fieldName)
                if (value != null) {
                    setFieldValue(view, fieldName, value)
                }
            } catch (e: Exception) {
                // 忽略恢复错误
            }
        }
    }
    

} 