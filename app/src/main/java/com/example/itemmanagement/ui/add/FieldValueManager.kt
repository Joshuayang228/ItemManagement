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
import android.util.Log
import android.widget.Spinner
import android.widget.TextView
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RatingBar
import android.widget.LinearLayout
import android.content.Context

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
        private const val SPINNER_TAG = "spinner_textview"
        private const val DATE_TAG = "date_textview"
    }

    /**
     * 保存所有字段的值
     */
    fun saveFieldValues(fieldViews: Map<String, View>) {
        if (fieldViews.isEmpty()) return

        try {
            fieldViews.forEach { (fieldName, view) ->
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
                            }
                        }
                    }
                    AddItemViewModel.DisplayStyle.RATING_STAR -> {
                        val ratingBar = findRatingBarInView(view)
                        if (ratingBar != null && ratingBar.rating > 0) {
                            viewModel.saveFieldValue(fieldName, ratingBar.rating)
                        }
                    }
                    AddItemViewModel.DisplayStyle.PERIOD_SELECTOR -> {
                        val container = view as? LinearLayout
                        if (container != null) {
                            val numberTextView = findTextViewInView(container, "period_number_textview")
                            val unitTextView = findTextViewInView(container, "period_unit_textview")
                            if (numberTextView != null && unitTextView != null &&
                                numberTextView.text.isNotEmpty() && unitTextView.text.isNotEmpty()
                            ) {
                                val value = Pair(numberTextView.text.toString(), unitTextView.text.toString())
                                viewModel.saveFieldValue(fieldName, value)
                            }
                        }
                    }
                    else -> {
                        when {
                            properties.validationType == AddItemViewModel.ValidationType.DATE -> {
                                val dateTextView = findTextViewInView(view, DATE_TAG)
                                if (dateTextView != null && dateTextView.text.isNotEmpty() &&
                                    dateTextView.text != "点击选择日期" &&
                                    dateTextView.text != fieldName) {
                                    viewModel.saveFieldValue(fieldName, dateTextView.text.toString())
                                }
                            }
                            properties.options != null -> {
                                val spinnerTextView = findTextViewInView(view, SPINNER_TAG)
                                if (spinnerTextView != null && spinnerTextView.text.isNotEmpty() &&
                                    spinnerTextView.text != properties.options.firstOrNull() &&
                                    spinnerTextView.text != fieldName) {
                                    viewModel.saveFieldValue(fieldName, spinnerTextView.text.toString())
                                }
                            }
                            properties.unitOptions != null -> {
                                if (view !is LinearLayout) {
                                    return@forEach
                                }

                                val editText = findEditTextInView(view)
                                val unitTextView = findTextViewInView(view, "unit_textview")

                                if (editText == null || unitTextView == null) return@forEach

                                val value = editText.text.toString()
                                val unit = unitTextView.text.toString()

                                // 保存 value 和 unit 值，移除过滤条件，即使是默认值也保存
                                if (value.isNotEmpty()) {
                                    viewModel.saveFieldValue(fieldName, value)
                                } else {
                                    // 如果为空，设置默认值为0或1
                                    val defaultValue = properties.defaultValue ?: "0"
                                    viewModel.saveFieldValue(fieldName, defaultValue)
                                }

                                // 即使是默认单位也保存
                                if (unit.isNotEmpty()) {
                                    viewModel.saveFieldValue("${fieldName}_unit", unit)
                                } else if (properties.unitOptions?.isNotEmpty() == true) {
                                    // 如果单位为空，使用第一个可选单位
                                    val defaultUnit = properties.unitOptions.first()
                                    viewModel.saveFieldValue("${fieldName}_unit", defaultUnit)
                                }
                            }
                            else -> {
                                when (view) {
                                    is EditText -> {
                                        val value = view.text.toString()
                                        if (value.isNotEmpty()) {
                                            viewModel.saveFieldValue(fieldName, value)
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
                                        }
                                    }
                                    is LinearLayout -> {
                                        val editText = findEditTextInView(view)
                                        val radioGroup = findRadioGroupInView(view)
                                        val ratingBar = findRatingBarInView(view)

                                        when {
                                            editText != null -> {
                                                val value = editText.text.toString()
                                                if (value.isNotEmpty()) {
                                                    viewModel.saveFieldValue(fieldName, value)
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
                                                }
                                            }
                                            ratingBar != null && ratingBar.rating > 0 -> {
                                                viewModel.saveFieldValue(fieldName, ratingBar.rating)
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

        val savedValues = viewModel.getAllFieldValues()
        if (savedValues.isEmpty()) {
            // 清空所有字段
            fieldViews.forEach { (fieldName, view) ->
                when (view) {
                    is EditText -> {
                        view.setText("")
                    }
                    is ViewGroup -> {
                        // 处理复合视图
                        findSpinnerInView(view)?.let { spinner ->
                            if (spinner.count > 0) {
                                spinner.setSelection(0)
                            }
                        }

                        findRatingBarInView(view)?.let { ratingBar ->
                            ratingBar.rating = 0f
                        }

                        findRadioGroupInView(view)?.let { radioGroup ->
                            radioGroup.clearCheck()
                        }

                        // 清空日期选择器
                        findTextViewInView(view, DATE_TAG)?.let { dateTextView ->
                            dateTextView.text = ""
                        }

                        // 处理带单位的字段
                        val properties = viewModel.getFieldProperties(fieldName)
                        if (properties.unitOptions?.isNotEmpty() == true) {
                            // 清空数值
                            findEditTextInView(view)?.let { editText ->
                                editText.setText(properties.defaultValue ?: "0")
                            }

                            // 重置单位为默认值
                            findTextViewInView(view, "unit_textview")?.let { unitTextView ->
                                val defaultUnit = properties.unitOptions.first()
                                unitTextView.text = defaultUnit
                            }
                        } else {
                            // 如果不是带单位字段，正常清空
                            findEditTextInView(view)?.let { editText ->
                                editText.setText("")
                            }
                        }

                        // 清空期限选择器
                        if (fieldName == "保质期" || fieldName == "保修期") {
                            // 清空数值输入框
                            findTextViewInView(view, "period_number_textview")?.let { numberText ->
                                numberText.text = ""
                            }
                            // 重置单位选择器
                            findTextViewInView(view, "period_unit_textview")?.let { unitText ->
                                val properties = viewModel.getFieldProperties(fieldName)
                                properties.periodUnits?.firstOrNull()?.let { firstUnit ->
                                    unitText.text = firstUnit
                                }
                            }
                        }

                        // 清空位置选择器
                        if (fieldName == "位置") {
                            findLocationSelectorInView(view)?.let { locationSelector ->
                                // 设置位置选择监听器
                                locationSelector.setOnLocationSelectedListener { area, container, sublocation ->
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

                                // 清除选择
                                locationSelector.clearSelection()
                            }
                        }

                        // 清空标签选择器
                        if (fieldName == "标签" || fieldName == "季节") {
                            // 获取 TagManager 实例
                            val tagManager = TagManager(context, dialogFactory, viewModel, fieldName)

                            // 清空ChipGroup
                            view.findViewById<ChipGroup>(R.id.selected_tags_container)?.let { chipGroup ->
                                tagManager.clearTags(chipGroup)
                            }

                            // 保存未选中状态到ViewModel
                            viewModel.saveFieldValue(fieldName, emptySet<String>())
                        }

                        // 清空下拉选择器的显示文本
                        findTextViewInView(view, SPINNER_TAG)?.let { spinnerText ->
                            val properties = viewModel.getFieldProperties(fieldName)
                            if (properties.options?.isNotEmpty() == true) {
                                spinnerText.text = properties.options.first()
                            } else {
                                spinnerText.text = ""
                            }
                        }
                    }
                }
            }
            return
        }

        try {
            savedValues.forEach { (fieldName, value) ->
                // 跳过单位字段，这些会在各自的字段类型处理中被处理
                if (fieldName.endsWith("_unit")) {
                    return@forEach
                }

                if (value == null) {
                    return@forEach
                }

                val view = fieldViews[fieldName]
                if (view == null) {
                    return@forEach
                }

                val properties = viewModel.getFieldProperties(fieldName)

                when (properties.displayStyle) {
                    AddItemViewModel.DisplayStyle.TAG -> {
                        // 标签恢复逻辑保持不变
                        val tags = value as? Set<String> ?: return@forEach
                        val selectedTagsContainer = view.findViewById<ChipGroup>(R.id.selected_tags_container)
                        selectedTagsContainer?.removeAllViews()

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
                                }
                                // 长按编辑或删除标签
                                setOnLongClickListener {
                                    val items = arrayOf("编辑", "删除")
                                    androidx.appcompat.app.AlertDialog.Builder(context)
                                        .setTitle("标签操作")
                                        .setItems(items) { dialog, which ->
                                            when (which) {
                                                0 -> { // 编辑
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
                                                1 -> { // 删除
                                                    androidx.appcompat.app.AlertDialog.Builder(context)
                                                        .setTitle("删除标签")
                                                        .setMessage("是否删除标签\"${tag}\"？")
                                                        .setPositiveButton("删除") { _, _ ->
                                                            selectedTagsContainer.removeView(this)
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
                        val numberTextView = findTextViewInView(container, "period_number_textview")
                        val unitTextView = findTextViewInView(container, "period_unit_textview")
                        if (numberTextView != null && unitTextView != null) {
                            numberTextView.text = periodValue.first as? String ?: ""
                            unitTextView.text = periodValue.second as? String ?: ""
                        }
                    }
                    else -> {
                        when {
                            properties.validationType == AddItemViewModel.ValidationType.DATE -> {
                                val dateTextView = findTextViewInView(view, DATE_TAG)
                                if (dateTextView != null && value is String && value != fieldName) {
                                    dateTextView.text = value
                                }
                            }
                            properties.options != null -> {
                                val spinnerTextView = findTextViewInView(view, SPINNER_TAG)
                                if (spinnerTextView != null && value is String && value != fieldName) {
                                    spinnerTextView.text = value
                                }
                            }
                            properties.unitOptions != null -> {
                                if (view !is LinearLayout) {
                                    return@forEach
                                }

                                val editText = findEditTextInView(view)
                                val unitTextView = findTextViewInView(view, "unit_textview")

                                if (editText == null || unitTextView == null) return@forEach

                                // 恢复数值
                                if (value is String) {
                                    editText.setText(value)
                                }

                                // 恢复单位
                                val unitValue = viewModel.getFieldValue("${fieldName}_unit") as? String

                                if (!unitValue.isNullOrEmpty()) {
                                    unitTextView.text = unitValue
                                } else if (properties.unitOptions?.isNotEmpty() == true) {
                                    // 如果没有保存单位或单位为空，使用第一个可选单位
                                    val defaultUnit = properties.unitOptions.first()
                                    unitTextView.text = defaultUnit
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
                // 首先尝试直接获取，这样更快
                if (view is LinearLayout) {
                    for (i in 0 until view.childCount) {
                        val child = view.getChildAt(i)
                        if (child is EditText) {
                            return child
                        }
                    }
                }

                // 如果直接获取失败，则进行深度搜索
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
     * 在视图层次结构中查找TextView（排除EditText）
     */
    private fun findTextViewInView(view: View, tag: String? = null): TextView? {
        return when (view) {
            is TextView -> {
                if (view !is EditText && (tag == null || view.tag == tag)) {
                    view
                } else null
            }
            is ViewGroup -> {
                // 首先尝试直接获取，这样更快
                if (view is LinearLayout) {
                    for (i in 0 until view.childCount) {
                        val child = view.getChildAt(i)
                        if (child is TextView && child !is EditText && (tag == null || child.tag == tag)) {
                            return child
                        }
                    }
                }

                // 如果直接获取失败，则进行深度搜索
                for (i in 0 until view.childCount) {
                    val child = view.getChildAt(i)
                    val result = findTextViewInView(child, tag)
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
    private fun findRadioGroupInView(view: View): RadioGroup? {
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
     * 获取所有字段的值用于保存物品
     */
    fun getFieldValues(fieldViews: Map<String, View>): Map<String, Any?> {
        saveFieldValues(fieldViews)
        return viewModel.getAllFieldValues()
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
                    savedStateHandle[fieldKey] = view.text.toString()
                }
                is TextView -> {
                    when (view.tag) {
                        "spinner_textview" -> savedStateHandle[fieldKey] = view.text.toString()
                        "date_textview" -> savedStateHandle[fieldKey] = view.text.toString()
                        "unit_textview" -> savedStateHandle["${fieldKey}_unit"] = view.text.toString()
                        "period_number_textview" -> savedStateHandle["${fieldKey}_number"] = view.text.toString()
                        "period_unit_textview" -> savedStateHandle["${fieldKey}_unit"] = view.text.toString()
                        else -> savedStateHandle[fieldKey] = view.text.toString()
                    }
                }
                is RadioGroup -> {
                    val selectedId = view.checkedRadioButtonId
                    savedStateHandle[fieldKey] = selectedId
                }
                is LinearLayout -> {
                    val editText = view.findViewWithTag<EditText>(null)
                    val unitTextView = view.findViewWithTag<TextView>("unit_textview")
                    val periodNumberTextView = view.findViewWithTag<TextView>("period_number_textview")
                    val periodUnitTextView = view.findViewWithTag<TextView>("period_unit_textview")

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
                    when (view.tag) {
                        "spinner_textview" -> savedStateHandle.get<String>(fieldKey)?.let { value ->
                            view.text = value
                        }
                        "date_textview" -> savedStateHandle.get<String>(fieldKey)?.let { value ->
                            view.text = value
                        }
                        "unit_textview" -> savedStateHandle.get<String>("${fieldKey}_unit")?.let { value ->
                            view.text = value
                        }
                        "period_number_textview" -> savedStateHandle.get<String>("${fieldKey}_number")?.let { value ->
                            view.text = value
                        }
                        "period_unit_textview" -> savedStateHandle.get<String>("${fieldKey}_unit")?.let { value ->
                            view.text = value
                        }
                        else -> savedStateHandle.get<String>(fieldKey)?.let { value ->
                            view.text = value
                        }
                    }
                }
                is RadioGroup -> {
                    savedStateHandle.get<Int>(fieldKey)?.let { selectedId ->
                        if (selectedId != -1) {
                            view.check(selectedId)
                        }
                    }
                }
                is LinearLayout -> {
                    val editText = view.findViewWithTag<EditText>(null)
                    val unitTextView = view.findViewWithTag<TextView>("unit_textview")
                    val periodNumberTextView = view.findViewWithTag<TextView>("period_number_textview")
                    val periodUnitTextView = view.findViewWithTag<TextView>("period_unit_textview")

                    if (editText != null && unitTextView != null) {
                        savedStateHandle.get<String>(fieldKey)?.let { value ->
                            editText.setText(value)
                        }
                        savedStateHandle.get<String>("${fieldKey}_unit")?.let { value ->
                            unitTextView.text = value
                        }
                    } else if (periodNumberTextView != null && periodUnitTextView != null) {
                        savedStateHandle.get<String>("${fieldKey}_number")?.let { value ->
                            periodNumberTextView.text = value
                        }
                        savedStateHandle.get<String>("${fieldKey}_unit")?.let { value ->
                            periodUnitTextView.text = value
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun findSpinnerInView(view: ViewGroup): Spinner? {
        if (view is Spinner) return view
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            if (child is Spinner) return child
            if (child is ViewGroup) {
                val result = findSpinnerInView(child)
                if (result != null) return result
            }
        }
        return null
    }

    /**
     * 在视图层次结构中查找LocationSelectorView
     */
    private fun findLocationSelectorInView(view: View): LocationSelectorView? {
        return when (view) {
            is LocationSelectorView -> view
            is ViewGroup -> {
                for (i in 0 until view.childCount) {
                    val child = view.getChildAt(i)
                    val result = findLocationSelectorInView(child)
                    if (result != null) {
                        return result
                    }
                }
                null
            }
            else -> null
        }
    }
} 