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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.*

/**
 * 负责创建各种类型的字段视图的工厂类
 */
class FieldViewFactory(
    private val context: Context,
    private val viewModel: AddItemViewModel,
    private val dialogFactory: DialogFactory,
    private val resources: android.content.res.Resources
) {

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
            properties.displayStyle == AddItemViewModel.DisplayStyle.TAG -> createTagSelector(field.name, properties)
            properties.displayStyle == AddItemViewModel.DisplayStyle.RATING_STAR -> createRatingBar()
            properties.displayStyle == AddItemViewModel.DisplayStyle.PERIOD_SELECTOR -> createPeriodSelector(field.name, properties)
            properties.displayStyle == AddItemViewModel.DisplayStyle.LOCATION_SELECTOR -> createLocationSelector()
            properties.validationType == AddItemViewModel.ValidationType.DATE -> createDatePicker(properties)
            properties.isMultiline -> createMultilineInput(properties)
            properties.unitOptions != null -> createNumberWithUnitInput(field.name, properties)
            properties.options != null -> createSpinner(field.name, properties)
            properties.validationType == AddItemViewModel.ValidationType.NUMBER -> createNumberInput(properties)
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
                if (properties.displayStyle == AddItemViewModel.DisplayStyle.RATING_STAR) {
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
            else -> { // Handles other ViewGroups like RadioGroup, FlowLayout, or complex LinearLayouts
                inputContainer.addView(input)
            }
        }
        container.addView(inputContainer)

        return container
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

            addView(RadioButton(context).apply {
                id = View.generateViewId()
                text = "未开封"
                isChecked = true
            })

            addView(Space(context).apply {
                layoutParams = RadioGroup.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.margin_normal),
                    RadioGroup.LayoutParams.WRAP_CONTENT
                )
            })

            addView(RadioButton(context).apply {
                id = View.generateViewId()
                text = "已开封"
            })
        }
    }

    private fun createTagSelector(fieldName: String, properties: AddItemViewModel.FieldProperties): View {
        // 直接加载标签容器
        val selectedTagsContainer = LayoutInflater.from(context).inflate(R.layout.tag_selector_layout, null, false) as ChipGroup

        // 为当前字段创建一个专用的 TagManager 实例
        val tagManager = TagManager(context, dialogFactory, viewModel, fieldName)

        // 初始化 TagManager 的默认标签
        tagManager.initialize(properties.options)

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
            setImageResource(R.drawable.ic_arrow_drop_down)
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

        return relativeLayout
    }

    private fun createNumberWithUnitInput(fieldName: String, properties: AddItemViewModel.FieldProperties): View {
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
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setPadding(8, 8, 8, 8)

            // 设置唯一标识
            tag = "unit_textview"  // 保持与 FieldValueManager 中一致

            // 设置默认值
            text = defaultUnits.firstOrNull() ?: "个"

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

    private fun createPeriodSelector(fieldName: String, properties: AddItemViewModel.FieldProperties): View {
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
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setPadding(8, 8, 8 + resources.getDimensionPixelSize(R.dimen.margin_normal), 8)

            // 设置唯一标识
            tag = "period_number_textview"  // 保持与 FieldValueManager 中一致

            // 设置默认值
            text = (properties.periodRange?.first ?: 1).toString()

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
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setPadding(8, 8, 8 + resources.getDimensionPixelSize(R.dimen.margin_normal), 8)

            // 设置唯一标识
            tag = "period_unit_textview"  // 保持与 FieldValueManager 中一致

            // 设置默认值
            text = defaultUnits.firstOrNull() ?: "月"

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

    private fun createSpinner(fieldName: String, properties: AddItemViewModel.FieldProperties): View {
        return TextView(context).apply {
            val spinnerTextView = this // Capture the TextView instance
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 14f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_input_borderless)
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setPadding(8, 8, 8, 8)

            // 设置唯一标识
            tag = "spinner_textview"  // 保持与 FieldValueManager.SPINNER_TAG 常量一致

            // 获取默认选项和自定义选项
            val defaultOptions = properties.options?.toMutableList() ?: mutableListOf()
            val customOptions = viewModel.getCustomOptions(fieldName)

            // 设置默认值
            spinnerTextView.text = properties.options?.firstOrNull() ?: ""

            // 添加下拉箭头图标
            setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down, 0)
            compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.margin_small)

            // 更新所有选项列表
            fun updateAllOptions(): List<String> {
                return defaultOptions + customOptions
            }

            // 点击事件处理
            setOnClickListener {
                dialogFactory.showOptionSelectionDialog(
                    "选择选项",
                    updateAllOptions(),
                    defaultOptions,
                    customOptions,
                    true, // 允许自定义
                    spinnerTextView,
                    { selectedOption ->
                        spinnerTextView.text = selectedOption
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
                        // 如果当前显示的是被删除的选项，则重置为第一个选项
                        if (spinnerTextView.text == option && updateAllOptions().isNotEmpty()) {
                            spinnerTextView.text = updateAllOptions().first()
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

    private fun createTextInput(properties: AddItemViewModel.FieldProperties): EditText {
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

    private fun createNumberInput(properties: AddItemViewModel.FieldProperties): EditText {
        return createTextInput(properties).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            properties.defaultValue?.let { setText(it) }
        }
    }

    private fun createMultilineInput(properties: AddItemViewModel.FieldProperties): EditText {
        return createTextInput(properties).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 1
            maxLines = properties.maxLines ?: 5
            gravity = Gravity.TOP
        }
    }

    private fun createDatePicker(properties: AddItemViewModel.FieldProperties): TextView {
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

            // 设置唯一标识
            tag = "date_textview"  // 保持与 FieldValueManager.DATE_TAG 常量一致

            // 添加日历图标
            setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_calendar, 0)
            compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.margin_small)

            if (properties.defaultDate) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                text = dateFormat.format(Date())
            } else {
                text = ""
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
            val starSize = (22 * density).toInt() // 缩小到原来的90%左右 (24dp -> 22dp)
            progressDrawable.setBounds(0, 0, starSize * numStars, starSize)

            // 设置星星之间的间距
            setPadding(0, 0, 0, 0)
        }

        container.addView(ratingBar)
        return container
    }
}