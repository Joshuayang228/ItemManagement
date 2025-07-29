package com.example.itemmanagement.ui.add

import android.view.View
import android.widget.*
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.example.itemmanagement.R
import android.view.ViewGroup
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.SavedStateHandle
import android.widget.Spinner
import android.widget.TextView
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RatingBar
import android.widget.LinearLayout
import android.content.Context
import androidx.core.content.ContextCompat
import android.util.Log

/**
 * 负责管理字段值的保存和恢复
 */
class FieldValueManager(
    private val context: Context,
    private val viewModel: AddItemViewModel,
    private val dialogFactory: DialogFactory
) {
    companion object {
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private const val SPINNER_TAG_PREFIX = "spinner_textview_"
        private const val DATE_TAG_PREFIX = "date_textview_"
        private const val UNIT_TAG_PREFIX = "unit_textview_"
        private const val PERIOD_NUMBER_TAG_PREFIX = "period_number_textview_"
        private const val PERIOD_UNIT_TAG_PREFIX = "period_unit_textview_"
    }

    /**
     * 保存所有字段的值
     */
    fun saveFieldValues(fieldViews: Map<String, View>) {
        if (fieldViews.isEmpty()) {
            return
        }

        try {
            // 特殊处理开封状态字段 - 先处理开封状态，确保它的值被正确保存
            if (fieldViews.containsKey("开封状态")) {
                val openStatusView = fieldViews["开封状态"]
                if (openStatusView != null) {
                    val radioGroup = findRadioGroupInView(openStatusView)
                    if (radioGroup != null) {
                        val selectedId = radioGroup.checkedRadioButtonId
                        val radioButton = if (selectedId != -1) radioGroup.findViewById<RadioButton>(selectedId) else null
                        val value = radioButton?.text?.toString()
                        
                        // 直接保存UI中的值，不设置默认值
                        if (value != null) {
                            viewModel.saveFieldValue("开封状态", value)
                        } else {
                            // 如果UI中没有选中值，则清除该字段值
                            viewModel.clearFieldValue("开封状态")
                        }
                    }
                }
            }
            
            // 处理其他字段
            fieldViews.forEach { (fieldName, view) ->
                // 跳过开封状态字段，因为已经处理过了
                if (fieldName == "开封状态") {
                    return@forEach
                }
                
                val properties = viewModel.getFieldProperties(fieldName)
                
                when (properties.displayStyle) {
                    AddItemViewModel.DisplayStyle.TAG -> {
                        val selectedTagsContainer = view.findViewById<ChipGroup>(R.id.selected_tags_container)
                        if (selectedTagsContainer != null) {
                            val tags = mutableSetOf<String>()
                            for (i in 0 until selectedTagsContainer.childCount) {
                                val chip = selectedTagsContainer.getChildAt(i) as? Chip
                                if (chip != null) {
                                    tags.add(chip.text.toString())
                                }
                            }
                            if (tags.isNotEmpty()) {
                                viewModel.saveFieldValue(fieldName, tags)
                            } else {
                                viewModel.clearFieldValue(fieldName)
                            }
                        }
                    }
                    AddItemViewModel.DisplayStyle.RATING_STAR -> {
                        val ratingBar = findRatingBarInView(view)
                        if (ratingBar != null && ratingBar.rating > 0) {
                            viewModel.saveFieldValue(fieldName, ratingBar.rating)
                        } else {
                            viewModel.clearFieldValue(fieldName)
                        }
                    }
                    AddItemViewModel.DisplayStyle.LOCATION_SELECTOR -> {
                        val locationSelector = findLocationSelectorInView(view)
                        if (locationSelector != null) {
                            val area = locationSelector.getSelectedArea()
                            val container = locationSelector.getSelectedContainer()
                            val sublocation = locationSelector.getSelectedSublocation()
                            
                            // 保存位置信息到ViewModel
                            if (!area.isNullOrEmpty()) {
                                viewModel.saveFieldValue("位置_area", area)
                                
                                if (!container.isNullOrEmpty()) {
                                    viewModel.saveFieldValue("位置_container", container)
                                } else {
                                    viewModel.clearFieldValue("位置_container")
                                }
                                
                                if (!sublocation.isNullOrEmpty()) {
                                    viewModel.saveFieldValue("位置_sublocation", sublocation)
                                } else {
                                    viewModel.clearFieldValue("位置_sublocation")
                                }
                            } else {
                                // 只有在确认用户清除了位置时才清除位置字段
                                val existingArea = viewModel.getFieldValue("位置_area") as? String
                                viewModel.clearFieldValue("位置_area")
                                viewModel.clearFieldValue("位置_container")
                                viewModel.clearFieldValue("位置_sublocation")
                            }
                        } else {
                            // 如果找不到位置选择器但ViewModel中有位置值，保留这些值
                            val existingArea = viewModel.getFieldValue("位置_area") as? String
                            if (existingArea != null && existingArea.isNotEmpty()) {
                                // 已经有位置信息，不做任何操作，保留现有值
                            } else {
                                // 没有现有位置信息，清除所有位置字段
                                viewModel.clearFieldValue("位置_area")
                                viewModel.clearFieldValue("位置_container")
                                viewModel.clearFieldValue("位置_sublocation")
                            }
                        }
                    }
                    AddItemViewModel.DisplayStyle.PERIOD_SELECTOR -> {
                        val container = view as? LinearLayout
                        if (container != null) {
                            val numberTextView = findTextViewInView(container, PERIOD_NUMBER_TAG_PREFIX, fieldName)
                            val unitTextView = findTextViewInView(container, PERIOD_UNIT_TAG_PREFIX, fieldName)
                            if (numberTextView != null && unitTextView != null &&
                                numberTextView.text.isNotEmpty() && numberTextView.text.toString() != "选择数值" &&
                                unitTextView.text.isNotEmpty() && unitTextView.text.toString() != "选择单位"
                            ) {
                                val value = Pair(numberTextView.text.toString(), unitTextView.text.toString())
                                viewModel.saveFieldValue(fieldName, value)
                            } else {
                                viewModel.clearFieldValue(fieldName)
                            }
                        }
                    }

                    else -> {
                        when {
                            properties.validationType == AddItemViewModel.ValidationType.DATE -> {
                                val dateTextView = findTextViewInView(view, DATE_TAG_PREFIX, fieldName)
                                if (dateTextView != null && dateTextView.text.isNotEmpty() &&
                                    dateTextView.text.toString() != "点击选择日期" &&
                                    (fieldName == "添加日期" || dateTextView.textColors.defaultColor != ContextCompat.getColor(context, R.color.hint_text_color))
                                ) {
                                    viewModel.saveFieldValue(fieldName, dateTextView.text.toString())
                                } else if (fieldName != "添加日期") {
                                    viewModel.clearFieldValue(fieldName)
                                }
                            }
                            properties.options != null -> {
                                val spinnerTextView = findTextViewInView(view, SPINNER_TAG_PREFIX, fieldName)
                                val textContent = spinnerTextView?.text?.toString() ?: ""

                                val isHintText = textContent == "选择分类" ||
                                        textContent == "选择渠道" ||
                                        textContent == "请选择" ||
                                        (spinnerTextView != null && spinnerTextView.textColors.defaultColor == ContextCompat.getColor(context, R.color.hint_text_color))

                                if (spinnerTextView != null && textContent.isNotEmpty() && !isHintText) {
                                    // 对于所有下拉选择类字段，如果值是"未指定"或为空，直接清除该字段
                                    if (textContent == "未指定" || textContent.isBlank()) {
                                        viewModel.clearFieldValue(fieldName)
                                        // 重置选择器的显示
                                        val hintText = when (fieldName) {
                                            "分类" -> "选择分类"
                                            "购买渠道" -> "选择渠道"
                                            else -> "请选择"
                                        }
                                        spinnerTextView.text = hintText
                                        spinnerTextView.setTextColor(ContextCompat.getColor(context, R.color.hint_text_color))
                                    } else {
                                        viewModel.saveFieldValue(fieldName, textContent)
                                    }
                                } else {
                                    viewModel.clearFieldValue(fieldName)
                                }
                            }
                            properties.unitOptions != null -> {
                                if (view !is LinearLayout) {
                                    return@forEach
                                }

                                val editText = findEditTextInView(view)
                                val unitTextView = findTextViewInView(view, UNIT_TAG_PREFIX, fieldName)

                                if (editText == null || unitTextView == null) return@forEach

                                val value = editText.text.toString()
                                val unit = unitTextView.text.toString()

                                val isUnitHintText = unit == "选择单位" ||
                                        unitTextView.textColors.defaultColor == ContextCompat.getColor(context, R.color.hint_text_color)

                                if (value.isNotEmpty()) {
                                    viewModel.saveFieldValue(fieldName, value)

                                    if (!isUnitHintText) {
                                        viewModel.saveFieldValue("${fieldName}_unit", unit)
                                    }
                                } else {
                                    viewModel.clearFieldValue(fieldName)
                                    viewModel.clearFieldValue("${fieldName}_unit")
                                }
                            }
                            else -> {
                                when (view) {
                                    is EditText -> {
                                        val value = view.text.toString()
                                        if (value.isNotEmpty()) {
                                            viewModel.saveFieldValue(fieldName, value)
                                        } else {
                                            viewModel.clearFieldValue(fieldName)
                                        }
                                    }
                                    is RadioGroup -> {
                                        val selectedId = view.checkedRadioButtonId
                                        if (selectedId != -1) {
                                            val radioButton = view.findViewById<RadioButton>(selectedId)
                                            val value = radioButton?.text.toString()
                                            if (value.isNotEmpty()) {
                                                viewModel.saveFieldValue(fieldName, value)
                                            }
                                        } else {
                                            viewModel.clearFieldValue(fieldName)
                                        }
                                    }
                                    is LinearLayout -> {
                                        val editText = findEditTextInView(view)
                                        val radioGroup = findRadioGroupInView(view)
                                        val ratingBar = findRatingBarInView(view)
                                        val switchView = findSwitchInView(view)
                                        val spinnerTextView = findTextViewInView(view, SPINNER_TAG_PREFIX, fieldName)

                                        when {
                                            editText != null -> {
                                                val value = editText.text.toString()
                                                if (value.isNotEmpty()) {
                                                    viewModel.saveFieldValue(fieldName, value)
                                                } else {
                                                    viewModel.clearFieldValue(fieldName)
                                                }
                                            }
                                            radioGroup != null -> {
                                                val selectedId = radioGroup.checkedRadioButtonId
                                                if (selectedId != -1) {
                                                    val radioButton = radioGroup.findViewById<RadioButton>(selectedId)
                                                    val value = radioButton?.text.toString()
                                                    if (value.isNotEmpty()) {
                                                        viewModel.saveFieldValue(fieldName, value)
                                                    }
                                                } else {
                                                    viewModel.clearFieldValue(fieldName)
                                                }
                                            }
                                            ratingBar != null -> {
                                                if (ratingBar.rating > 0) {
                                                    viewModel.saveFieldValue(fieldName, ratingBar.rating)
                                                } else {
                                                    viewModel.clearFieldValue(fieldName)
                                                }
                                            }
                                            switchView != null -> {
                                                // 保存Switch的状态
                                                viewModel.saveFieldValue(fieldName, switchView.isChecked.toString())
                                            }
                                            spinnerTextView != null -> {
                                                val value = spinnerTextView.text.toString()
                                                if (value.isNotEmpty() && !value.startsWith("DELETED:") && !value.startsWith("EDIT:") && value != "选择分类" && value != "请选择分类" && value != "选择子分类" && value != "请选择子分类" && value != "选择渠道" && value != "请选择") {
                                                    viewModel.saveFieldValue(fieldName, value)
                                                } else {
                                                    viewModel.clearFieldValue(fieldName)
                                                }
                                            }
                                            else -> {
                                                viewModel.clearFieldValue(fieldName)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    /**
     * 恢复所有字段的值
     */
    fun restoreFieldValues(fieldViews: Map<String, View>) {
        if (fieldViews.isEmpty()) {
            return
        }

        try {
            val savedValues = viewModel.getAllFieldValues()
            
            // 首先清除所有字段的值
            fieldViews.forEach { (fieldName, view) ->
                val properties = viewModel.getFieldProperties(fieldName)
                
                // 清除EditText
                val editText = findEditTextInView(view)
                editText?.setText("")
                
                // 清除RadioGroup
                val radioGroup = findRadioGroupInView(view)
                radioGroup?.clearCheck()
                
                // 清除RatingBar
                val ratingBar = findRatingBarInView(view)
                ratingBar?.rating = 0f
                
                // 清除标签
                if (properties.displayStyle == AddItemViewModel.DisplayStyle.TAG) {
                    val selectedTagsContainer = view.findViewById<ChipGroup>(R.id.selected_tags_container)
                    selectedTagsContainer?.removeAllViews()
                    
                    // 显示提示文本
                    val placeholderText = findTagPlaceholderTextView(view)
                    placeholderText?.visibility = View.VISIBLE
                }
                
                // 清除位置选择器
                if (properties.displayStyle == AddItemViewModel.DisplayStyle.LOCATION_SELECTOR) {
                    val locationSelectorView = findLocationSelectorInView(view)
                    locationSelectorView?.clearSelection()
                }
                
                // 清除下拉选择框
                val spinnerText = findTextViewInView(view, SPINNER_TAG_PREFIX, fieldName)
                if (spinnerText != null) {
                    val text = when (fieldName) {
                        "分类" -> "请选择分类"
                        "子分类" -> "请选择子分类"
                        "渠道" -> "选择渠道"
                        else -> "请选择"
                    }
                    spinnerText.text = text
                    spinnerText.setTextColor(ContextCompat.getColor(context, R.color.hint_text_color))
                }

                // 清除单位选择
                if (properties.unitOptions != null) {
                    findTextViewInView(view, UNIT_TAG_PREFIX, fieldName)?.let { unitTextView ->
                        unitTextView.text = "选择单位"
                        unitTextView.setTextColor(ContextCompat.getColor(context, R.color.hint_text_color))
                    }
                }

                // 清除周期选择器
                if (fieldName == "保质期" || fieldName == "保修期") {
                    findTextViewInView(view, PERIOD_NUMBER_TAG_PREFIX, fieldName)?.let { numberText ->
                        numberText.text = ""
                        numberText.setTextColor(ContextCompat.getColor(context, R.color.hint_text_color))
                    }
                    findTextViewInView(view, PERIOD_UNIT_TAG_PREFIX, fieldName)?.let { unitText ->
                        unitText.text = "选择单位"
                        unitText.setTextColor(ContextCompat.getColor(context, R.color.hint_text_color))
                    }
                }
                
                // 清除日期选择
                if (properties.validationType == AddItemViewModel.ValidationType.DATE) {
                    val dateTextView = findTextViewInView(view, DATE_TAG_PREFIX, fieldName)
                    if (dateTextView != null) {
                        dateTextView.text = "点击选择日期"
                        dateTextView.setTextColor(ContextCompat.getColor(context, R.color.hint_text_color))
                    }
                }
            }

            // 特殊处理开封状态字段
            if (fieldViews.containsKey("开封状态")) {
                val openStatusView = fieldViews["开封状态"]
                val openStatusValue = viewModel.getFieldValue("开封状态") as? String
                
                if (openStatusView != null) {
                    val radioGroup = findRadioGroupInView(openStatusView)
                    if (radioGroup != null) {
                        // 首先清除选择
                        radioGroup.clearCheck()
                        
                        // 只有在有值的情况下才设置选中状态
                        if (openStatusValue != null) {
                            // 尝试设置选中状态
                            for (i in 0 until radioGroup.childCount) {
                                val radioButton = radioGroup.getChildAt(i) as? RadioButton
                                if (radioButton != null && radioButton.text.toString() == openStatusValue) {
                                    radioButton.isChecked = true
                                    break
                                }
                            }
                        }
                    }
                }
            }

            // 特殊处理位置信息的恢复
            val areaValue = savedValues["位置_area"] as? String
            val containerValue = savedValues["位置_container"] as? String
            val sublocationValue = savedValues["位置_sublocation"] as? String
            
            // 只有当位置区域不是"未指定"或空时才恢复位置字段
            if (!areaValue.isNullOrEmpty() && areaValue != "未指定") {
                fieldViews.forEach { (fieldName, view) ->
                    val properties = viewModel.getFieldProperties(fieldName)
                    if (properties.displayStyle == AddItemViewModel.DisplayStyle.LOCATION_SELECTOR) {
                        val locationSelector = findLocationSelectorInView(view)
                        locationSelector?.setSelectedLocation(areaValue, containerValue, sublocationValue)
                    }
                }
            }
            
            // 然后恢复有保存值的字段
            savedValues.forEach { (fieldName, value) ->
                // 跳过开封状态字段，因为已经特殊处理过了
                if (fieldName == "开封状态") {
                    return@forEach
                }
                
                if (fieldName.endsWith("_unit")) {
                    return@forEach
                }
                
                // 跳过位置相关字段，因为已经特殊处理过了
                if (fieldName == "位置_area" || fieldName == "位置_container" || fieldName == "位置_sublocation") {
                    return@forEach
                }

                if (value == null) {
                    return@forEach
                }

                // 特殊处理所有字段，如果值是"未指定"或为空，则不恢复
                if (value is String && (value == "未指定" || value.isBlank())) {
                    return@forEach
                }

                val view = fieldViews[fieldName]
                if (view == null) {
                    return@forEach
                }

                val properties = viewModel.getFieldProperties(fieldName)

                when (properties.displayStyle) {
                    AddItemViewModel.DisplayStyle.TAG -> {
                        val tags = value as? Set<String> ?: return@forEach
                        val selectedTagsContainer = view.findViewById<ChipGroup>(R.id.selected_tags_container)
                        selectedTagsContainer?.removeAllViews()

                        // 获取提示文本视图
                        val placeholderText = findTagPlaceholderTextView(view)

                        if (tags.isEmpty()) {
                            // 如果没有标签，显示提示文本
                            placeholderText?.visibility = View.VISIBLE
                        } else {
                            // 如果有标签，隐藏提示文本
                            placeholderText?.visibility = View.GONE

                            tags.forEach { tag ->
                                val chip = Chip(view.context).apply {
                                    text = tag
                                    isCheckable = false
                                    isCloseIconVisible = true
                                    textSize = 14f
                                    chipMinHeight = android.util.TypedValue.applyDimension(
                                        android.util.TypedValue.COMPLEX_UNIT_DIP,
                                        28f,
                                        view.resources.displayMetrics
                                    )
                                    chipStartPadding = 6f
                                    chipEndPadding = 6f
                                    closeIconEndPadding = 2f
                                    closeIconStartPadding = 2f
                                    layoutParams = ViewGroup.MarginLayoutParams(
                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT
                                    ).apply {
                                        bottomMargin = android.util.TypedValue.applyDimension(
                                            android.util.TypedValue.COMPLEX_UNIT_DIP, 2f, view.resources.displayMetrics).toInt()
                                    }
                                    setOnCloseIconClickListener {
                                        selectedTagsContainer.removeView(this)

                                        // 检查是否所有标签都被移除，如果是则显示提示文本
                                        if (selectedTagsContainer.childCount == 0) {
                                            placeholderText?.visibility = View.VISIBLE
                                        }
                                    }
                                    setOnLongClickListener {
                                        val items = arrayOf("编辑", "删除")
                                        androidx.appcompat.app.AlertDialog.Builder(context)
                                            .setTitle("标签操作")
                                            .setItems(items) { dialog, which ->
                                                when (which) {
                                                    0 -> {
                                                        val editText = EditText(context).apply {
                                                            layoutParams = LinearLayout.LayoutParams(
                                                                LinearLayout.LayoutParams.MATCH_PARENT,
                                                                LinearLayout.LayoutParams.WRAP_CONTENT
                                                            )
                                                            setText(tag)
                                                            setSelection(tag.length)
                                                        }

                                                        androidx.appcompat.app.AlertDialog.Builder(context)
                                                            .setTitle("编辑标签")
                                                            .setView(editText)
                                                            .setPositiveButton("确定") { _, _ ->
                                                                val newTagName = editText.text.toString().trim()
                                                                if (newTagName.isNotEmpty() && newTagName != tag) {
                                                                    text = newTagName
                                                                }
                                                            }
                                                            .setNegativeButton("取消", null)
                                                            .show()
                                                    }
                                                    1 -> {
                                                        androidx.appcompat.app.AlertDialog.Builder(context)
                                                            .setTitle("删除标签")
                                                            .setMessage("是否删除标签\"${tag}\"？")
                                                            .setPositiveButton("删除") { _, _ ->
                                                                selectedTagsContainer.removeView(this)

                                                                // 检查是否所有标签都被移除，如果是则显示提示文本
                                                                if (selectedTagsContainer.childCount == 0) {
                                                                    placeholderText?.visibility = View.VISIBLE
                                                                }
                                                            }
                                                            .setNegativeButton("取消", null)
                                                            .show()
                                                    }
                                                }
                                            }
                                            .show()
                                        true
                                    }
                                }
                                selectedTagsContainer.addView(chip)
                            }
                        }
                    }
                    AddItemViewModel.DisplayStyle.RATING_STAR -> {
                        val rating = when (value) {
                            is Float -> value
                            is Double -> value.toFloat()
                            is Int -> value.toFloat()
                            else -> return@forEach
                        }
                        val ratingBar = findRatingBarInView(view)
                        ratingBar?.rating = rating
                    }
                    AddItemViewModel.DisplayStyle.PERIOD_SELECTOR -> {
                        val periodValue = value as? Pair<*, *> ?: return@forEach
                        val container = view as? LinearLayout ?: return@forEach
                        val numberTextView = findTextViewInView(container, PERIOD_NUMBER_TAG_PREFIX, fieldName)
                        val unitTextView = findTextViewInView(container, PERIOD_UNIT_TAG_PREFIX, fieldName)
                        if (numberTextView != null && unitTextView != null) {
                            val number = periodValue.first as? String ?: ""
                            val unit = periodValue.second as? String ?: ""

                            if (number.isNotEmpty()) {
                                numberTextView.text = number
                                numberTextView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                            }

                            if (unit.isNotEmpty()) {
                                unitTextView.text = unit
                                unitTextView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                            }
                        }
                    }

                    else -> {
                        when {
                            properties.validationType == AddItemViewModel.ValidationType.DATE -> {
                                val dateTextView = findTextViewInView(view, DATE_TAG_PREFIX, fieldName)
                                if (dateTextView != null && value is String) {
                                    dateTextView.text = value
                                    dateTextView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                                }
                            }
                            properties.options != null -> {
                                val spinnerTextView = findTextViewInView(view, SPINNER_TAG_PREFIX, fieldName)
                                if (spinnerTextView != null && value is String) {
                                    spinnerTextView.text = value
                                    spinnerTextView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                                }
                            }
                            properties.unitOptions != null -> {
                                if (view !is LinearLayout) {
                                    return@forEach
                                }

                                val editText = findEditTextInView(view)
                                val unitTextView = findTextViewInView(view, UNIT_TAG_PREFIX, fieldName)

                                if (editText == null || unitTextView == null) return@forEach

                                if (value is String && value.isNotEmpty()) {
                                    editText.setText(value)
                                }

                                val unitValue = viewModel.getFieldValue("${fieldName}_unit") as? String

                                if (!unitValue.isNullOrEmpty()) {
                                    unitTextView.text = unitValue
                                    unitTextView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                                } else if (value is String && value.isNotEmpty()) {
                                    unitTextView.text = "选择单位"
                                    unitTextView.setTextColor(ContextCompat.getColor(context, R.color.hint_text_color))
                                }
                            }
                            else -> {
                                when (view) {
                                    is EditText -> {
                                        if (value is String && value.isNotEmpty()) {
                                            view.setText(value)
                                        }
                                    }
                                    is RadioGroup -> {
                                        val selectedText = value as? String ?: return@forEach
                                        for (i in 0 until view.childCount) {
                                            val radioButton = view.getChildAt(i) as? RadioButton
                                            if (radioButton != null && radioButton.text == selectedText) {
                                                radioButton.isChecked = true
                                                break
                                            }
                                        }
                                    }
                                    is LinearLayout -> {
                                        val editText = findEditTextInView(view)
                                        val radioGroup = findRadioGroupInView(view)
                                        val ratingBar = findRatingBarInView(view)
                                        val switchView = findSwitchInView(view)

                                        when {
                                            editText != null -> {
                                                if (value is String && value.isNotEmpty()) {
                                                    editText.setText(value)
                                                }
                                            }
                                            radioGroup != null -> {
                                                val selectedText = value as? String ?: return@forEach
                                                for (i in 0 until radioGroup.childCount) {
                                                    val radioButton = radioGroup.getChildAt(i) as? RadioButton
                                                    if (radioButton != null && radioButton.text == selectedText) {
                                                        radioButton.isChecked = true
                                                        break
                                                    }
                                                }
                                            }
                                            ratingBar != null -> {
                                                val rating = when (value) {
                                                    is Float -> value
                                                    is Double -> value.toFloat()
                                                    is Int -> value.toFloat()
                                                    else -> 0f
                                                }
                                                if (rating > 0) {
                                                    ratingBar.rating = rating
                                                }
                                            }
                                            switchView != null -> {
                                                // 恢复Switch的状态
                                                val isChecked = when (value) {
                                                    is Boolean -> value
                                                    is String -> value.toBoolean()
                                                    else -> false
                                                }
                                                switchView.isChecked = isChecked
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }

    /**
     * 在视图层次结构中查找EditText
     */
    private fun findEditTextInView(view: View): EditText? {
        return when (view) {
            is EditText -> {
                view
            }
            is ViewGroup -> {
                if (view is LinearLayout) {
                    for (i in 0 until view.childCount) {
                        val child = view.getChildAt(i)
                        if (child is EditText) {
                            return child
                        }
                    }
                }

                for (i in 0 until view.childCount) {
                    val child = view.getChildAt(i)
                    val result = findEditTextInView(child)
                    if (result != null) {
                        return result
                    }
                }

                null
            }
            else -> null
        }
    }

    /**
     * 在视图层次结构中查找TextView（排除EditText），支持字段名特定的tag
     */
    private fun findTextViewInView(view: View, tagPrefix: String? = null, fieldName: String? = null): TextView? {
        val targetTag = if (tagPrefix != null && fieldName != null) {
            "$tagPrefix$fieldName"
        } else {
            tagPrefix
        }

        return when (view) {
            is TextView -> {
                if (view !is EditText && (targetTag == null || view.tag == targetTag)) {
                    view
                } else null
            }
            is ViewGroup -> {
                if (view is LinearLayout) {
                    for (i in 0 until view.childCount) {
                        val child = view.getChildAt(i)
                        if (child is TextView && child !is EditText && (targetTag == null || child.tag == targetTag)) {
                            return child
                        }
                    }
                }

                for (i in 0 until view.childCount) {
                    val child = view.getChildAt(i)
                    val result = findTextViewInView(child, targetTag)
                    if (result != null) {
                        return result
                    }
                }

                null
            }
            else -> null
        }
    }

    /**
     * 在视图层次结构中查找RatingBar
     */
    private fun findRatingBarInView(view: View): RatingBar? {
        return when (view) {
            is RatingBar -> view
            is ViewGroup -> {
                for (i in 0 until view.childCount) {
                    val child = view.getChildAt(i)
                    val result = findRatingBarInView(child)
                    if (result != null) {
                        return result
                    }
                }
                null
            }
            else -> null
        }
    }

    /**
     * 在视图层次结构中查找RadioGroup
     */
    fun findRadioGroupInView(view: View): RadioGroup? {
        return when (view) {
            is RadioGroup -> view
            is ViewGroup -> {
                for (i in 0 until view.childCount) {
                    val child = view.getChildAt(i)
                    val result = findRadioGroupInView(child)
                    if (result != null) {
                        return result
                    }
                }
                null
            }
            else -> null
        }
    }

    /**
     * 在视图层次结构中查找Switch控件
     */
    private fun findSwitchInView(view: View): Switch? {
        return when (view) {
            is Switch -> view
            is ViewGroup -> {
                for (i in 0 until view.childCount) {
                    val child = view.getChildAt(i)
                    val result = findSwitchInView(child)
                    if (result != null) {
                        return result
                    }
                }
                null
            }
            else -> null
        }
    }

    /**
     * 获取所有字段的值用于保存物品
     */
    fun getFieldValues(fieldViews: Map<String, View>): Map<String, Any?> {
        // 不再重复调用saveFieldValues，因为在BaseItemFragment.saveItem()中已经调用过了
        // 重复调用可能会导致位置信息丢失
        // saveFieldValues(fieldViews)
        
        // 确保位置信息被正确保存
        val values = viewModel.getAllFieldValues()
        
        return values
    }

    /**
     * 清除所有保存的字段值
     */
    fun clearFieldValues() {
        viewModel.clearAllFieldValues()
    }

    // 保存字段值
    fun saveFieldValue(view: View, savedStateHandle: SavedStateHandle, fieldKey: String) {
        try {
            when (view) {
                is EditText -> {
                    val value = view.text.toString()
                    savedStateHandle[fieldKey] = value
                }
                is TextView -> {
                    val tag = view.tag?.toString() ?: ""
                    val value = view.text.toString()
                    when {
                        tag.startsWith(SPINNER_TAG_PREFIX) -> savedStateHandle[fieldKey] = value
                        tag.startsWith(DATE_TAG_PREFIX) -> savedStateHandle[fieldKey] = value
                        tag.startsWith(UNIT_TAG_PREFIX) -> savedStateHandle["${fieldKey}_unit"] = value
                        tag.startsWith(PERIOD_NUMBER_TAG_PREFIX) -> savedStateHandle["${fieldKey}_number"] = value
                        tag.startsWith(PERIOD_UNIT_TAG_PREFIX) -> savedStateHandle["${fieldKey}_unit"] = value
                        else -> savedStateHandle[fieldKey] = value
                    }
                }
                is RadioGroup -> {
                    val selectedId = view.checkedRadioButtonId
                    savedStateHandle[fieldKey] = selectedId
                }
                is LinearLayout -> {
                    val fieldName = fieldKey.substringAfterLast('_', fieldKey)

                    val editText = findEditTextInView(view)
                    val unitTextView = findTextViewInView(view, UNIT_TAG_PREFIX, fieldName)
                    val periodNumberTextView = findTextViewInView(view, PERIOD_NUMBER_TAG_PREFIX, fieldName)
                    val periodUnitTextView = findTextViewInView(view, PERIOD_UNIT_TAG_PREFIX, fieldName)

                    if (editText != null && unitTextView != null) {
                        savedStateHandle[fieldKey] = editText.text.toString()
                        savedStateHandle["${fieldKey}_unit"] = unitTextView.text.toString()
                    } else if (periodNumberTextView != null && periodUnitTextView != null) {
                        savedStateHandle["${fieldKey}_number"] = periodNumberTextView.text.toString()
                        savedStateHandle["${fieldKey}_unit"] = periodUnitTextView.text.toString()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 恢复字段值
    fun restoreFieldValue(view: View, savedStateHandle: SavedStateHandle, fieldKey: String) {
        try {
            when (view) {
                is EditText -> {
                    savedStateHandle.get<String>(fieldKey)?.let { value ->
                        view.setText(value)
                    }
                }
                is TextView -> {
                    val tag = view.tag?.toString() ?: ""
                    when {
                        tag.startsWith(SPINNER_TAG_PREFIX) -> savedStateHandle.get<String>(fieldKey)?.let { value ->
                            view.text = value
                            view.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                        }
                        tag.startsWith(DATE_TAG_PREFIX) -> savedStateHandle.get<String>(fieldKey)?.let { value ->
                            view.text = value
                            view.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                        }
                        tag.startsWith(UNIT_TAG_PREFIX) -> savedStateHandle.get<String>("${fieldKey}_unit")?.let { value ->
                            view.text = value
                            view.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                        }
                        tag.startsWith(PERIOD_NUMBER_TAG_PREFIX) -> savedStateHandle.get<String>("${fieldKey}_number")?.let { value ->
                            view.text = value
                            view.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                        }
                        tag.startsWith(PERIOD_UNIT_TAG_PREFIX) -> savedStateHandle.get<String>("${fieldKey}_unit")?.let { value ->
                            view.text = value
                            view.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                        }
                        else -> savedStateHandle.get<String>(fieldKey)?.let { value ->
                            view.text = value
                        }
                    }
                }
                is RadioGroup -> {
                    val selectedId = savedStateHandle.get<Int>(fieldKey)
                    if (selectedId != null && selectedId != -1) {
                        view.check(selectedId)
                    }
                }
                is LinearLayout -> {
                    val fieldName = fieldKey.substringAfterLast('_', fieldKey)

                    val editText = findEditTextInView(view)
                    val unitTextView = findTextViewInView(view, UNIT_TAG_PREFIX, fieldName)
                    val periodNumberTextView = findTextViewInView(view, PERIOD_NUMBER_TAG_PREFIX, fieldName)
                    val periodUnitTextView = findTextViewInView(view, PERIOD_UNIT_TAG_PREFIX, fieldName)

                    if (editText != null && unitTextView != null) {
                        savedStateHandle.get<String>(fieldKey)?.let { savedValue ->
                            if (savedValue.isNotEmpty()) {
                                editText.setText(savedValue)
                            }

                            val unitValue = savedStateHandle.get<String>("${fieldKey}_unit")

                            if (!unitValue.isNullOrEmpty()) {
                                unitTextView.text = unitValue
                                unitTextView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                            } else if (savedValue.isNotEmpty()) {
                                unitTextView.text = "选择单位"
                                unitTextView.setTextColor(ContextCompat.getColor(context, R.color.hint_text_color))
                            }
                        }
                    } else if (periodNumberTextView != null && periodUnitTextView != null) {
                        savedStateHandle.get<String>("${fieldKey}_number")?.let { value ->
                            periodNumberTextView.text = value
                            periodNumberTextView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                        }
                        savedStateHandle.get<String>("${fieldKey}_unit")?.let { value ->
                            periodUnitTextView.text = value
                            periodUnitTextView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun findSpinnerInView(view: ViewGroup): Spinner? {
        if (view is Spinner) {
            return view
        }
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            if (child is Spinner) {
                return child
            }
            if (child is ViewGroup) {
                val result = findSpinnerInView(child)
                if (result != null) {
                    return result
                }
            }
        }
        return null
    }

    /**
     * 在视图层次结构中查找LocationSelectorView
     */
    private fun findLocationSelectorInView(view: View): LocationSelectorView? {
        try {
            return when (view) {
                is LocationSelectorView -> {
                    view
                }
                is ViewGroup -> {
                    // 先检查直接子视图
                    for (i in 0 until view.childCount) {
                        val child = view.getChildAt(i)
                        if (child is LocationSelectorView) {
                            return child
                        }
                    }
                    
                    // 然后递归检查
                    for (i in 0 until view.childCount) {
                        val child = view.getChildAt(i)
                        val result = findLocationSelectorInView(child)
                        if (result != null) {
                            return result
                        }
                    }
                    null
                }
                else -> {
                    null
                }
            }
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 在标签选择器视图中查找提示文本视图
     */
    private fun findTagPlaceholderTextView(view: View): TextView? {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child is TextView && child.text == "点击选择标签") {
                    return child
                } else if (child is ViewGroup) {
                    val result = findTagPlaceholderTextView(child)
                    if (result != null) {
                        return result
                    }
                }
            }
        }
        return null
    }
} 