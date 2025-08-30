package com.example.itemmanagement.ui.add

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.text.InputType
import android.util.TypedValue
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import com.example.itemmanagement.R
import com.example.itemmanagement.ui.base.FieldInteractionViewModel
import com.example.itemmanagement.ui.common.FieldProperties
import com.example.itemmanagement.ui.common.ValidationType
import com.example.itemmanagement.ui.common.DisplayStyle
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
import android.widget.Switch

/**
 * 负责创建各种类型的字段视图的工厂类
 */
class FieldViewFactory(
    private val context: Context,
    private val viewModel: FieldInteractionViewModel,
    private val dialogFactory: DialogFactory,
    private val resources: android.content.res.Resources
) {

    // 查找子分类控件的辅助方法
    private fun findSubCategoryView(parent: View): TextView? {
        // 获取当前视图的父视图
        var currentParent: View? = parent
        while (currentParent != null && currentParent !is ViewGroup) {
            currentParent = currentParent.parent as? View
        }
        
        // 如果找到了ViewGroup父视图
        if (currentParent is ViewGroup) {
            // 遍历所有子视图
            for (i in 0 until currentParent.childCount) {
                val child = currentParent.getChildAt(i)
                // 查找子分类的TextView
                if (child is ViewGroup) {
                    for (j in 0 until child.childCount) {
                        val grandChild = child.getChildAt(j)
                        if (grandChild is TextView && grandChild.tag == "spinner_textview_子分类") {
                            return grandChild
                        }
                    }
                }
            }
        }
        
        // 如果没找到，向上继续查找
        currentParent = currentParent?.parent as? ViewGroup
        if (currentParent is ViewGroup) {
            for (i in 0 until currentParent.childCount) {
                val child = currentParent.getChildAt(i)
                if (child is ViewGroup) {
                    val subCategoryView = findSubCategoryViewInViewGroup(child)
                    if (subCategoryView != null) {
                        return subCategoryView
                    }
                }
            }
        }
        
        return null
    }
    
    // 在ViewGroup中递归查找子分类控件
    private fun findSubCategoryViewInViewGroup(viewGroup: ViewGroup): TextView? {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is TextView && child.tag == "spinner_textview_子分类") {
                return child
            } else if (child is ViewGroup) {
                val result = findSubCategoryViewInViewGroup(child)
                if (result != null) {
                    return result
                }
            }
        }
        return null
    }

    /**
     * 创建字段视图
     */
    fun createFieldView(field: Field): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            gravity = Gravity.CENTER_VERTICAL  // 整个容器垂直居中
        }

        // 添加标签
        val label = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.field_label_width),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = field.name
            textSize = 14f
            gravity = Gravity.START or Gravity.CENTER_VERTICAL  // 左对齐且垂直居中
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
        }
        container.addView(label)

        // 获取字段属性
        val properties = viewModel.getFieldProperties(field.name)

        // 根据字段类型创建不同的输入控件
        val input = when {
            field.name == "开封状态" -> createRadioGroup()
            field.name == "加入心愿单" || field.name == "高周转" -> createSwitchView(field.name, properties)
            properties.displayStyle == DisplayStyle.TAG -> createTagSelector(field.name, properties)
            properties.displayStyle == DisplayStyle.RATING_STAR -> createRatingBar()
            properties.displayStyle == DisplayStyle.PERIOD_SELECTOR -> createPeriodSelector(field.name, properties)
            properties.displayStyle == DisplayStyle.LOCATION_SELECTOR -> createLocationSelector()
            properties.validationType == ValidationType.DATE -> createDatePicker(properties)
            properties.isMultiline -> createMultilineInput(properties)
            properties.unitOptions != null -> createNumberWithUnitInput(field.name, properties)
            properties.options != null -> createSpinner(field.name, properties)
            properties.validationType == ValidationType.NUMBER -> createNumberInput(properties)
            else -> createTextInput(properties)
        }

        // 根据输入控件类型决定是否需要包装在容器中
        val inputContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            gravity = Gravity.CENTER_VERTICAL // Default gravity for the container
        }

        when (input) {
            is LinearLayout -> {
                if (properties.displayStyle == DisplayStyle.RATING_STAR) {
                    // 直接使用我们已经创建的带有评分控件的LinearLayout容器
                    container.addView(input)
                    return container
                } else {
                    inputContainer.addView(input)
                }
            }
            is RatingBar -> {
                // 右对齐评分控件
                inputContainer.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                inputContainer.addView(input)
            }
            is Spinner -> {
                inputContainer.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                inputContainer.addView(input)
            }
            is EditText -> {
                input.apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    background = ContextCompat.getDrawable(context, R.drawable.bg_input_borderless)
                    textSize = 14f
                    gravity = Gravity.END or Gravity.CENTER_VERTICAL
                    setTextColor(ContextCompat.getColor(context, android.R.color.black))
                }
                inputContainer.addView(input) // EditText will fill the inputContainer by default
            }
            is TextView -> { // Handles DatePicker TextView primarily
                // If the TextView (e.g., DatePicker) is WRAP_CONTENT, align it to the end.
                inputContainer.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                inputContainer.addView(input)
            }
            is Switch -> {
                // 右对齐Switch控件
                inputContainer.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                inputContainer.addView(input)
            }
            else -> { // Handles other ViewGroups like RadioGroup, FlowLayout, or complex LinearLayouts
                inputContainer.addView(input)
            }
        }
        container.addView(inputContainer)

        return container
    }

    private fun createSpinner(fieldName: String, properties: FieldProperties): View {
        return TextView(context).apply {
            val spinnerTextView = this // Capture the TextView instance
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 14f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_input_borderless)
            setTextColor(ContextCompat.getColor(context, R.color.hint_text_color))
            setPadding(8, 8, 8, 8)

            // 设置唯一标识，包含字段名
            tag = "spinner_textview_${fieldName}"

            // 获取默认选项和自定义选项
            val defaultOptions = properties.options?.toMutableList() ?: mutableListOf()
            val customOptions = viewModel.getCustomOptions(fieldName)

            // 根据字段名设置默认提示文本
            spinnerTextView.text = when(fieldName) {
                "分类" -> "请选择分类"
                "子分类" -> "请选择子分类"
                "购买渠道" -> "选择渠道"
                else -> "请选择"
            }

            // 添加下拉箭头图标
            setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down, 0)
            compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.margin_small)

            // 更新所有选项列表
            fun updateAllOptions(): List<String> {
                // 过滤掉删除标记和编辑映射标记的选项
                val filteredCustomOptions = customOptions.filter { 
                    !it.startsWith("DELETED:") && !it.startsWith("EDIT:") 
                }
                return (defaultOptions + filteredCustomOptions).distinct()
            }

            // 点击事件处理
            setOnClickListener {
                // 特殊处理子分类字段
                if (fieldName == "子分类") {
                    // 检查分类是否已选择
                    if (!viewModel.isCategorySelected()) {
                        // 显示Toast提示用户先选择分类
                        Toast.makeText(context, "请先选择分类", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    
                    // 获取当前选择的分类
                    val currentCategory = viewModel.getFieldValue("分类")?.toString() ?: ""
                    // 更新和合并子分类选项
                    if (currentCategory.isNotBlank() && currentCategory != "选择分类" && currentCategory != "请选择分类") {
                        // 1. 获取原始的默认子分类（未经过编辑映射处理的）
                        val originalDefaultSubCategories = viewModel.getOriginalSubCategoriesForCategory(currentCategory)
                        
                        // 2. 获取该字段的自定义选项（包含编辑映射和删除标记）
                        val allCustomOptions = viewModel.getCustomOptions("子分类")
                        
                        // 3. 分离出真正的自定义添加选项（不包括系统标记）
                        val pureCustomOptions = allCustomOptions.filter { 
                            !it.startsWith("DELETED:") && !it.startsWith("EDIT:") 
                        }
                        
                        // 4. 使用ViewModel的方法获取处理后的完整列表
                        val processedOptions = viewModel.getSubCategoriesForCategory(currentCategory)
                        
                        // 5. 更新defaultOptions - 使用原始默认选项
                        defaultOptions.clear()
                        defaultOptions.addAll(originalDefaultSubCategories)
                        
                        // 6. 更新customOptions - 包含所有自定义信息
                        customOptions.clear()
                        customOptions.addAll(allCustomOptions)
                    }
                }
                
                // 根据字段类型选择显示逻辑
                if (fieldName == "子分类") {
                    val currentCategory = viewModel.getFieldValue("分类")?.toString() ?: ""
                    if (currentCategory.isBlank() || currentCategory == "选择分类" || currentCategory == "请选择分类") {
                        return@setOnClickListener
                    }
                    
                    // 为子分类字段设置专用的选项数据
                    defaultOptions.clear()
                    defaultOptions.addAll(viewModel.getOriginalSubCategoriesForCategory(currentCategory))
                    customOptions.clear()
                    customOptions.addAll(viewModel.getCustomOptions("子分类").filter { 
                        !it.startsWith("DELETED:") && !it.startsWith("EDIT:") 
                    })
                    
                    dialogFactory.showOptionSelectionDialog(
                        "选择选项",
                        viewModel.getSubCategoriesForCategory(currentCategory),
                        defaultOptions,
                        customOptions,
                        true,
                        spinnerTextView,
                        { selectedOption ->
                            spinnerTextView.text = selectedOption
                            spinnerTextView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                            viewModel.saveFieldValue(fieldName, selectedOption)
                        },
                        { oldOption, newOption ->
                            // 子分类编辑逻辑 - 使用与其他字段相同的方式更新选项
                            val originalDefaults = viewModel.getOriginalSubCategoriesForCategory(currentCategory)
                            val allCustomOptions = viewModel.getCustomOptions("子分类").toMutableList()
                            
                            if (originalDefaults.contains(oldOption)) {
                                // 编辑默认选项 - 添加编辑映射
                                val existingMappings = allCustomOptions.filter { 
                                    it.startsWith("EDIT:") && (it.endsWith("->$oldOption") || it.startsWith("EDIT:$oldOption->"))
                                }
                                allCustomOptions.removeAll(existingMappings)
                                allCustomOptions.add("EDIT:$oldOption->$newOption")
                                viewModel.setCustomOptions(fieldName, allCustomOptions)
                                
                                // 更新本地defaultOptions中的显示（模拟编辑效果）
                                val index = defaultOptions.indexOf(oldOption)
                                if (index >= 0) {
                                    defaultOptions[index] = newOption
                                }
                            } else {
                                // 编辑自定义选项 - 直接更新
                                val pureCustomOptions = allCustomOptions.filter { 
                                    !it.startsWith("DELETED:") && !it.startsWith("EDIT:") 
                                }.toMutableList()
                                val index = pureCustomOptions.indexOf(oldOption)
                                if (index >= 0) {
                                    pureCustomOptions[index] = newOption
                                    val systemMarkers = allCustomOptions.filter { 
                                        it.startsWith("DELETED:") || it.startsWith("EDIT:") 
                                    }
                                    allCustomOptions.clear()
                                    allCustomOptions.addAll(systemMarkers + pureCustomOptions)
                                    viewModel.setCustomOptions(fieldName, allCustomOptions)
                                    
                                    // 更新本地customOptions中的显示
                                    val localIndex = customOptions.indexOf(oldOption)
                                    if (localIndex >= 0) {
                                        customOptions[localIndex] = newOption
                                    }
                                }
                            }
                            
                            if (spinnerTextView.text == oldOption) {
                                spinnerTextView.text = newOption
                                viewModel.saveFieldValue(fieldName, newOption)
                            }
                        },
                        { option ->
                            // 子分类删除逻辑 - 使用与其他字段相同的方式更新选项
                            val originalDefaults = viewModel.getOriginalSubCategoriesForCategory(currentCategory)
                            val allCustomOptions = viewModel.getCustomOptions("子分类").toMutableList()
                            
                            if (originalDefaults.contains(option)) {
                                // 删除默认选项 - 添加删除标记
                                val deletedOption = "DELETED:$option"
                                if (!allCustomOptions.contains(deletedOption)) {
                                    allCustomOptions.add(deletedOption)
                                    viewModel.setCustomOptions(fieldName, allCustomOptions)
                                    
                                    // 从本地defaultOptions中移除（模拟删除效果）
                                    defaultOptions.remove(option)
                                }
                            } else {
                                // 删除自定义选项 - 直接移除
                                val pureCustomOptions = allCustomOptions.filter { 
                                    !it.startsWith("DELETED:") && !it.startsWith("EDIT:") 
                                }.toMutableList()
                                pureCustomOptions.remove(option)
                                val systemMarkers = allCustomOptions.filter { 
                                    it.startsWith("DELETED:") || it.startsWith("EDIT:") 
                                }
                                allCustomOptions.clear()
                                allCustomOptions.addAll(systemMarkers + pureCustomOptions)
                                viewModel.setCustomOptions(fieldName, allCustomOptions)
                                
                                // 从本地customOptions中移除
                                customOptions.remove(option)
                            }
                            
                            if (spinnerTextView.text == option) {
                                val remainingOptions = defaultOptions + customOptions
                                if (remainingOptions.isNotEmpty()) {
                                    spinnerTextView.text = remainingOptions.first()
                                    viewModel.saveFieldValue(fieldName, remainingOptions.first())
                                } else {
                                    spinnerTextView.text = "请选择子分类"
                                    spinnerTextView.setTextColor(ContextCompat.getColor(context, R.color.hint_text_color))
                                    viewModel.clearFieldValue(fieldName)
                                }
                            }
                        },
                        { newOption ->
                            // 子分类添加逻辑 - 使用与其他字段相同的方式
                            val originalDefaults = viewModel.getOriginalSubCategoriesForCategory(currentCategory)
                            val allCustomOptions = viewModel.getCustomOptions("子分类").toMutableList()
                            val pureCustomOptions = allCustomOptions.filter { 
                                !it.startsWith("DELETED:") && !it.startsWith("EDIT:") 
                            }.toMutableList()
                            
                            if (!originalDefaults.contains(newOption) && !pureCustomOptions.contains(newOption)) {
                                pureCustomOptions.add(newOption)
                                val systemMarkers = allCustomOptions.filter { 
                                    it.startsWith("DELETED:") || it.startsWith("EDIT:") 
                                }
                                allCustomOptions.clear()
                                allCustomOptions.addAll(systemMarkers + pureCustomOptions)
                                viewModel.setCustomOptions(fieldName, allCustomOptions)
                                
                                // 添加到本地customOptions
                                if (!customOptions.contains(newOption)) {
                                    customOptions.add(newOption)
                                }
                            }
                        }
                    )
                } else {
                    dialogFactory.showOptionSelectionDialog(
                        "选择选项",
                        updateAllOptions(),
                        defaultOptions,
                        customOptions,
                        true, // 允许自定义
                        spinnerTextView,
                        { selectedOption ->
                            spinnerTextView.text = selectedOption
                            // 选择后设置为黑色文本
                            spinnerTextView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                            
                            // 保存字段值到ViewModel
                            viewModel.saveFieldValue(fieldName, selectedOption)
                            
                            // 特殊处理分类字段，当分类变化时更新子分类选项
                            if (fieldName == "分类") {
                                // 更新子分类选项
                                viewModel.updateSubCategoryOptions(selectedOption)
                                // 清空子分类的当前选择
                                viewModel.clearFieldValue("子分类")
                                // 查找子分类控件并重置其显示
                                val subCategoryView = findSubCategoryView(this)
                                subCategoryView?.let {
                                    it.text = "请选择子分类"
                                    it.setTextColor(ContextCompat.getColor(context, R.color.hint_text_color))
                                }
                            }
                        },
                        { oldOption, newOption ->
                            // 更新选项
                            if (defaultOptions.contains(oldOption)) {
                                val index = defaultOptions.indexOf(oldOption)
                                defaultOptions[index] = newOption
                            } else if (customOptions.contains(oldOption)) {
                                val index = customOptions.indexOf(oldOption)
                                customOptions[index] = newOption
                                viewModel.setCustomOptions(fieldName, customOptions)
                            }
                            
                            // 如果当前显示的是被编辑的选项，则更新显示
                            if (spinnerTextView.text == oldOption) {
                                spinnerTextView.text = newOption
                                viewModel.saveFieldValue(fieldName, newOption)
                            }
                        },
                        { option ->
                            // 删除选项
                            if (defaultOptions.contains(option)) {
                                defaultOptions.remove(option)
                            } else if (customOptions.contains(option)) {
                                customOptions.remove(option)
                                viewModel.setCustomOptions(fieldName, customOptions)
                            }
                            
                            // 如果当前显示的是被删除的选项，则重置显示
                            if (spinnerTextView.text == option) {
                                val remainingOptions = updateAllOptions()
                                
                                if (remainingOptions.isNotEmpty()) {
                                    spinnerTextView.text = remainingOptions.first()
                                    viewModel.saveFieldValue(fieldName, remainingOptions.first())
                                } else {
                                    val defaultText = when(fieldName) {
                                        "分类" -> "请选择分类"
                                        "购买渠道" -> "选择渠道"
                                        else -> "请选择"
                                    }
                                    spinnerTextView.text = defaultText
                                    spinnerTextView.setTextColor(ContextCompat.getColor(context, R.color.hint_text_color))
                                    viewModel.clearFieldValue(fieldName)
                                }
                            }
                        },
                        { newOption ->
                            // 添加新选项
                            if (!defaultOptions.contains(newOption) && !customOptions.contains(newOption)) {
                                customOptions.add(newOption)
                                viewModel.setCustomOptions(fieldName, customOptions)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun createRadioGroup(): RadioGroup {
        return RadioGroup(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            orientation = RadioGroup.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL

            val unopenedButton = RadioButton(context).apply {
                id = View.generateViewId()
                text = "未开封"
                // 不设置默认选中
                isChecked = false
            }
            addView(unopenedButton)

            addView(Space(context).apply {
                layoutParams = RadioGroup.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.margin_normal),
                    RadioGroup.LayoutParams.WRAP_CONTENT
                )
            })

            addView(RadioButton(context).apply {
                id = View.generateViewId()
                text = "已开封"
                // 不设置默认选中
                isChecked = false
            })
        }
    }

    private fun createTagSelector(fieldName: String, properties: FieldProperties): View {
        // 创建一个容器来包含标签选择器和提示文本
        val containerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // 直接加载标签容器
        val selectedTagsContainer = LayoutInflater.from(context).inflate(R.layout.tag_selector_layout, null, false) as ChipGroup

        // 为当前字段创建一个专用的 TagManager 实例
        val tagManager = TagManager(context, dialogFactory, viewModel, fieldName)

        // 初始化 TagManager 的默认标签
        tagManager.initialize(properties.options)

        // 创建提示文本视图
        val placeholderText = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = "点击选择标签"
            textSize = 14f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setTextColor(ContextCompat.getColor(context, R.color.hint_text_color))
            id = View.generateViewId() // 生成唯一ID以便后续引用

            // 设置点击事件，点击时显示标签选择对话框
            setOnClickListener {
                tagManager.showTagSelectionDialog(selectedTagsContainer)
                // 隐藏提示文本，因为对话框关闭后可能会添加标签
                visibility = View.GONE
            }
        }

        // 创建标签选择按钮
        val tagSelectorButton = ImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                // 将按钮定位到右侧
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                // 增加左侧margin，使按钮向右移动
                marginStart = resources.getDimensionPixelSize(R.dimen.margin_normal)
            }
            // 使用已有的右箭头图标
            setImageResource(R.drawable.ic_chevron_right)
            background = null
            setPadding(
                resources.getDimensionPixelSize(R.dimen.padding_normal),
                resources.getDimensionPixelSize(R.dimen.padding_normal),
                resources.getDimensionPixelSize(R.dimen.padding_normal),
                resources.getDimensionPixelSize(R.dimen.padding_normal)
            )

            // 点击显示标签选择对话框
            setOnClickListener {
                tagManager.showTagSelectionDialog(selectedTagsContainer)
                // 隐藏提示文本，因为对话框关闭后可能会添加标签
                placeholderText.visibility = View.GONE
            }
        }

        // 创建一个相对布局来包含标签容器和按钮
        val relativeLayout = RelativeLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // 将标签容器添加到相对布局
        val tagsContainerParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        relativeLayout.addView(selectedTagsContainer, tagsContainerParams)

        // 将按钮添加到相对布局的右侧
        val buttonParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            addRule(RelativeLayout.CENTER_VERTICAL)
        }
        relativeLayout.addView(tagSelectorButton, buttonParams)

        // 将提示文本添加到相对布局
        val placeholderParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(RelativeLayout.CENTER_VERTICAL)
            addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            // 设置右边距，避免与按钮重叠，并向左移动一些
            rightMargin = resources.getDimensionPixelSize(R.dimen.padding_normal) * 4
        }
        relativeLayout.addView(placeholderText, placeholderParams)

        // 设置标签容器的监听器，根据是否有标签来显示/隐藏提示文本
        selectedTagsContainer.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            placeholderText.visibility = if (selectedTagsContainer.childCount > 0) View.GONE else View.VISIBLE
        }

        return relativeLayout
    }

    private fun createNumberWithUnitInput(fieldName: String, properties: FieldProperties): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }

        val input = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = properties.hint
            // 不设置默认值
            setText("")
            textSize = 14f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_input_borderless)
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setHintTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            minWidth = resources.getDimensionPixelSize(R.dimen.input_min_width)
        }

        // 获取默认单位和自定义单位
        val defaultUnits = properties.unitOptions?.toMutableList() ?: mutableListOf()
        val customUnits = viewModel.getCustomUnits(fieldName)

        // 更新所有单位列表
        fun updateAllUnits(): List<String> {
            return defaultUnits + customUnits
        }

        // 创建自适应宽度的单位选择器
        val unitTextView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.margin_small)
            }
            textSize = 14f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_input_borderless)
            setTextColor(ContextCompat.getColor(context, R.color.hint_text_color))
            setPadding(8, 8, 8, 8)

            // 设置唯一标识
            tag = "unit_textview_${fieldName}"  // 使用字段名创建唯一tag

            // 设置默认值
            text = "选择单位"  // 修改默认值

            // 添加下拉箭头图标
            setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down, 0)
            compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.margin_small)

            // 点击事件处理
            setOnClickListener {
                dialogFactory.showUnitSelectionDialog(
                    "选择单位",
                    updateAllUnits(),
                    defaultUnits,
                    customUnits,
                    true, // 允许自定义
                    this,
                    { selectedUnit ->
                        text = selectedUnit
                        // 选择后设置为黑色文本
                        setTextColor(ContextCompat.getColor(context, android.R.color.black))
                    },
                    { oldUnit, newUnit ->
                        // 更新单位
                        if (defaultUnits.contains(oldUnit)) {
                            val index = defaultUnits.indexOf(oldUnit)
                            defaultUnits[index] = newUnit
                        } else if (customUnits.contains(oldUnit)) {
                            val index = customUnits.indexOf(oldUnit)
                            customUnits[index] = newUnit
                            viewModel.setCustomUnits(fieldName, customUnits)
                        }
                        // 如果当前显示的是被编辑的单位，则更新显示
                        if (text == oldUnit) {
                            text = newUnit
                        }
                    },
                    { unit ->
                        // 删除单位
                        if (defaultUnits.contains(unit)) {
                            defaultUnits.remove(unit)
                        } else if (customUnits.contains(unit)) {
                            customUnits.remove(unit)
                            viewModel.setCustomUnits(fieldName, customUnits)
                        }
                        // 如果当前显示的是被删除的单位，则重置为第一个单位
                        if (text == unit && updateAllUnits().isNotEmpty()) {
                            text = updateAllUnits().first()
                        }
                    },
                    { newUnit ->
                        // 添加新单位
                        if (!defaultUnits.contains(newUnit) && !customUnits.contains(newUnit)) {
                            customUnits.add(newUnit)
                            viewModel.setCustomUnits(fieldName, customUnits)
                        }
                    }
                )
            }
        }

        container.addView(input)
        container.addView(unitTextView)
        return container
    }

    private fun createPeriodSelector(fieldName: String, properties: FieldProperties): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }

        // 数字选择器
        val numberSelector = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 14f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_input_borderless)
            setTextColor(ContextCompat.getColor(context, R.color.hint_text_color))
            setPadding(8, 8, 8 + resources.getDimensionPixelSize(R.dimen.margin_normal), 8)

            // 设置唯一标识，包含字段名
            tag = "period_number_textview_${fieldName}"

            // 设置默认值为空
            text = ""
            hint = "选择数值"

            // 添加下拉箭头图标
            setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down, 0)
            compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.margin_small)

            // 点击事件处理
            setOnClickListener { view ->
                val numbers = (properties.periodRange ?: 1..36).toList().map { it.toString() }.toTypedArray()
                AlertDialog.Builder(context)
                    .setTitle("选择数值")
                    .setItems(numbers) { dialog, which ->
                        text = numbers[which]
                        // 设置文本颜色为黑色表示已选择
                        setTextColor(ContextCompat.getColor(context, android.R.color.black))
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }

        // 获取默认单位和自定义单位
        val defaultUnits = properties.periodUnits?.toMutableList() ?: mutableListOf("年", "月", "日")
        val customUnits = viewModel.getCustomUnits(fieldName)

        // 更新所有单位列表
        fun updateAllUnits(): List<String> {
            return defaultUnits + customUnits
        }

        // 单位选择器
        val periodUnitTextView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.margin_small)
            }
            textSize = 14f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_input_borderless)
            setTextColor(ContextCompat.getColor(context, R.color.hint_text_color))
            setPadding(8, 8, 8 + resources.getDimensionPixelSize(R.dimen.margin_normal), 8)

            // 设置唯一标识，包含字段名
            tag = "period_unit_textview_${fieldName}"

            // 设置默认值为空
            text = "选择单位"

            // 添加下拉箭头图标
            setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down, 0)
            compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.margin_small)

            // 点击事件处理
            setOnClickListener {
                dialogFactory.showUnitSelectionDialog(
                    "选择单位",
                    updateAllUnits(),
                    defaultUnits,
                    customUnits,
                    true, // 允许自定义
                    this,
                    { selectedUnit ->
                        text = selectedUnit
                        // 设置文本颜色为黑色表示已选择
                        setTextColor(ContextCompat.getColor(context, android.R.color.black))
                    },
                    { oldUnit, newUnit ->
                        // 更新单位
                        if (defaultUnits.contains(oldUnit)) {
                            val index = defaultUnits.indexOf(oldUnit)
                            defaultUnits[index] = newUnit
                        } else if (customUnits.contains(oldUnit)) {
                            val index = customUnits.indexOf(oldUnit)
                            customUnits[index] = newUnit
                            viewModel.setCustomUnits(fieldName, customUnits)
                        }
                        // 如果当前显示的是被编辑的单位，则更新显示
                        if (text == oldUnit) {
                            text = newUnit
                        }
                    },
                    { unit ->
                        // 删除单位
                        if (defaultUnits.contains(unit)) {
                            defaultUnits.remove(unit)
                        } else if (customUnits.contains(unit)) {
                            customUnits.remove(unit)
                            viewModel.setCustomUnits(fieldName, customUnits)
                        }
                        // 如果当前显示的是被删除的单位，则重置为第一个单位
                        if (text == unit && updateAllUnits().isNotEmpty()) {
                            text = updateAllUnits().first()
                        }
                    },
                    { newUnit ->
                        // 添加新单位
                        if (!defaultUnits.contains(newUnit) && !customUnits.contains(newUnit)) {
                            customUnits.add(newUnit)
                            viewModel.setCustomUnits(fieldName, customUnits)
                        }
                    }
                )
            }
        }

        container.addView(numberSelector)
        container.addView(periodUnitTextView)
        return container
    }

    private fun createTextInput(properties: FieldProperties): EditText {
        return EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            hint = properties.hint
            textSize = 14f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_input_borderless)
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setHintTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        }
    }

    private fun createNumberInput(properties: FieldProperties): EditText {
        return createTextInput(properties).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            properties.defaultValue?.let { setText(it) }
        }
    }

    private fun createMultilineInput(properties: FieldProperties): EditText {
        return createTextInput(properties).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 1
            maxLines = properties.maxLines ?: 5
            gravity = Gravity.TOP
        }
    }

    private fun createDatePicker(properties: FieldProperties): TextView {
        return TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            background = ContextCompat.getDrawable(context, R.drawable.bg_input_borderless)
            setPadding(8, 8, 8, 8)
            textSize = 14f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            hint = "点击选择日期"
            setTextColor(ContextCompat.getColor(context, android.R.color.black))

            // 获取当前字段名
            val fieldName = properties.fieldName ?: ""

            // 设置唯一标识，包含字段名
            tag = "date_textview_${fieldName}"

            // 添加日历图标
            setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_calendar, 0)
            compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.margin_small)

            if (properties.defaultDate) {  // 仅对"添加日期"使用当前日期
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                text = dateFormat.format(Date())
                setTextColor(ContextCompat.getColor(context, android.R.color.black))
            } else {
                text = ""  // 其他日期字段默认为空
                setTextColor(ContextCompat.getColor(context, R.color.hint_text_color))
            }

            setOnClickListener {
                val calendar = Calendar.getInstance()
                val currentDate = if (text.isNotEmpty() && text != "点击选择日期") {
                    try {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        dateFormat.parse(text.toString())?.let {
                            calendar.time = it
                            calendar
                        } ?: calendar
                    } catch (e: Exception) {
                        calendar
                    }
                } else {
                    calendar
                }

                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        calendar.set(year, month, day)
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        text = dateFormat.format(calendar.time)
                        // 用户选择日期后，设置文本颜色为黑色
                        setTextColor(ContextCompat.getColor(context, android.R.color.black))
                    },
                    currentDate.get(Calendar.YEAR),
                    currentDate.get(Calendar.MONTH),
                    currentDate.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        }
    }

    private fun createLocationSelector(): View {
        try {
            // 创建位置选择器
            val locationSelectorView = LocationSelectorView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                // 创建并初始化位置管理器
                val locationManager = LocationManager(context, viewModel)

                // 初始化位置选择器
                initialize(locationManager, viewModel)

                // 设置位置选择监听器
                setOnLocationSelectedListener { area, container, sublocation ->
                    // 保存选择的值到 ViewModel
                    if (area != null) {
                        viewModel.saveFieldValue("位置_area", area)

                        if (container != null) {
                            viewModel.saveFieldValue("位置_container", container)

                            if (sublocation != null) {
                                viewModel.saveFieldValue("位置_sublocation", sublocation)
                            } else {
                                viewModel.clearFieldValue("位置_sublocation")
                            }
                        } else {
                            viewModel.clearFieldValue("位置_container")
                            viewModel.clearFieldValue("位置_sublocation")
                        }
                    } else {
                        viewModel.clearFieldValue("位置_area")
                        viewModel.clearFieldValue("位置_container")
                        viewModel.clearFieldValue("位置_sublocation")
                    }
                }
            }

            return locationSelectorView

        } catch (e: Exception) {
            // 创建失败时返回一个空的视图
            return View(context)
        }
    }

    private fun createRatingBar(): View {
        // 创建一个容器
        val container = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 0)
        }

        // 使用标准 RatingBar，但设置合适的样式和大小
        val ratingBar = RatingBar(context, null, android.R.attr.ratingBarStyleIndicator).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            numStars = 5
            stepSize = 1f
            setIsIndicator(false)  // 确保可以进行评分

            // 通过自定义样式调整大小
            val density = resources.displayMetrics.density
            val starSize = (20 * density).toInt() // 缩小到原来的90%左右 (24dp -> 20dp)
            progressDrawable.setBounds(0, 0, starSize * numStars, starSize)

            // 设置星星之间的间距
            setPadding(0, 0, 0, 0)
        }

        container.addView(ratingBar)
        return container
    }

    private fun createSwitchView(fieldName: String, properties: FieldProperties): View {
        return Switch(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            
            // 设置唯一标识
            tag = "switch_${fieldName}"
            
            // 设置默认值为false（关闭状态）
            isChecked = false
            
            // 设置切换监听器
            setOnCheckedChangeListener { _, isChecked ->
                // 保存开关状态到ViewModel
                viewModel.saveFieldValue(fieldName, isChecked.toString())
            }
            
            // 设置开关样式
            textSize = 14f
            setPadding(8, 8, 8, 8)
        }
    }
}