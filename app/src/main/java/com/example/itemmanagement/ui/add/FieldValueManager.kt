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

/**
 * 负责管理字段值的保存和恢复
 */
class FieldValueManager(private val viewModel: AddItemViewModel) {
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
                                Log.d("FieldValueManager", "保存标签字段 $fieldName: $tags")
                            }
                        }
                    }
                    AddItemViewModel.DisplayStyle.RATING_STAR -> {
                        val ratingBar = findRatingBarInView(view)
                        if (ratingBar != null && ratingBar.rating > 0) {
                            viewModel.saveFieldValue(fieldName, ratingBar.rating)
                            Log.d("FieldValueManager", "保存评分字段 $fieldName: ${ratingBar.rating}")
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
                                Log.d("FieldValueManager", "保存期限字段 $fieldName: $value")
                            } else {
                                Log.d("FieldValueManager", "期限字段 $fieldName 未保存，number=${numberTextView?.text}, unit=${unitTextView?.text}")
                            }
                        } else {
                            Log.d("FieldValueManager", "期限字段 $fieldName 视图不是 LinearLayout")
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
                                    Log.d("FieldValueManager", "保存日期字段 $fieldName: ${dateTextView.text}")
                                }
                            }
                            properties.options != null -> {
                                val spinnerTextView = findTextViewInView(view, SPINNER_TAG)
                                if (spinnerTextView != null && spinnerTextView.text.isNotEmpty() &&
                                    spinnerTextView.text != properties.options.firstOrNull() &&
                                    spinnerTextView.text != fieldName) {
                                    viewModel.saveFieldValue(fieldName, spinnerTextView.text.toString())
                                    Log.d("FieldValueManager", "保存选项字段 $fieldName: ${spinnerTextView.text}")
                                }
                            }
                            properties.unitOptions != null -> {
                                Log.d("FieldValueManager", "处理带单位字段 $fieldName")
                                if (view !is LinearLayout) {
                                    Log.d("FieldValueManager", "带单位字段 $fieldName 视图不是 LinearLayout")
                                    return@forEach
                                }

                                val editText = findEditTextInView(view)
                                val unitTextView = findTextViewInView(view, "unit_textview")

                                Log.d("FieldValueManager", "带单位字段 $fieldName 找到控件: editText=${editText != null}, unitTextView=${unitTextView != null}")

                                if (editText == null || unitTextView == null) return@forEach

                                val value = editText.text.toString()
                                val unit = unitTextView.text.toString()

                                Log.d("FieldValueManager", "带单位字段 $fieldName 值: value=$value, unit=$unit")

                                // 保存 value 和 unit 值，移除过滤条件，即使是默认值也保存
                                if (value.isNotEmpty()) {
                                    viewModel.saveFieldValue(fieldName, value)
                                    Log.d("FieldValueManager", "保存带单位字段值 $fieldName: $value")
                                } else {
                                    // 如果为空，设置默认值为0或1
                                    val defaultValue = properties.defaultValue ?: "0"
                                    viewModel.saveFieldValue(fieldName, defaultValue)
                                    Log.d("FieldValueManager", "保存带单位字段默认值 $fieldName: $defaultValue")
                                }

                                // 即使是默认单位也保存
                                if (unit.isNotEmpty()) {
                                    viewModel.saveFieldValue("${fieldName}_unit", unit)
                                    Log.d("FieldValueManager", "保存带单位字段单位 ${fieldName}_unit: $unit")
                                } else if (properties.unitOptions?.isNotEmpty() == true) {
                                    // 如果单位为空，使用第一个可选单位
                                    val defaultUnit = properties.unitOptions.first()
                                    viewModel.saveFieldValue("${fieldName}_unit", defaultUnit)
                                    Log.d("FieldValueManager", "保存带单位字段默认单位 ${fieldName}_unit: $defaultUnit")
                                }
                            }
                            else -> {
                                when (view) {
                                    is EditText -> {
                                        val value = view.text.toString()
                                        if (value.isNotEmpty()) {
                                            viewModel.saveFieldValue(fieldName, value)
                                            Log.d("FieldValueManager", "保存文本字段 $fieldName: $value")
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
            Log.e("FieldValueManager", "保存字段值出错", e)
            e.printStackTrace()
        }
    }

    /**
     * 恢复所有字段的值
     */
    fun restoreFieldValues(fieldViews: Map<String, View>) {
        if (fieldViews.isEmpty()) return

        val savedValues = viewModel.getAllFieldValues()
        Log.d("FieldValueManager", "准备恢复字段值，保存的值：${savedValues.keys}")

        try {
            savedValues.forEach { (fieldName, value) ->
                // 跳过单位字段，这些会在各自的字段类型处理中被处理
                if (fieldName.endsWith("_unit")) return@forEach

                if (value == null) {
                    Log.d("FieldValueManager", "字段 $fieldName 的值为空，跳过恢复")
                    return@forEach
                }

                val view = fieldViews[fieldName]
                if (view == null) {
                    Log.d("FieldValueManager", "找不到字段 $fieldName 的视图，跳过恢复")
                    return@forEach
                }

                val properties = viewModel.getFieldProperties(fieldName)
                Log.d("FieldValueManager", "恢复字段 $fieldName 的值: $value, 类型: ${properties.displayStyle}")

                when (properties.displayStyle) {
                    AddItemViewModel.DisplayStyle.TAG -> {
                        // 标签恢复逻辑保持不变
                        val tags = value as? Set<String> ?: return@forEach
                        Log.d("FieldValueManager", "恢复标签字段 $fieldName: $tags")
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
                        Log.d("FieldValueManager", "恢复评分字段 $fieldName: $rating")
                        val ratingBar = findRatingBarInView(view)
                        ratingBar?.rating = rating
                    }
                    AddItemViewModel.DisplayStyle.PERIOD_SELECTOR -> {
                        val periodValue = value as? Pair<*, *> ?: return@forEach
                        Log.d("FieldValueManager", "恢复期限字段 $fieldName: $periodValue")
                        val container = view as? LinearLayout ?: return@forEach
                        val numberTextView = findTextViewInView(container, "period_number_textview")
                        val unitTextView = findTextViewInView(container, "period_unit_textview")
                        if (numberTextView != null && unitTextView != null) {
                            numberTextView.text = periodValue.first as? String ?: ""
                            unitTextView.text = periodValue.second as? String ?: ""
                            Log.d("FieldValueManager", "期限字段 $fieldName 设置为: number=${numberTextView.text}, unit=${unitTextView.text}")
                        } else {
                            Log.d("FieldValueManager", "期限字段 $fieldName 控件未找到: numberTextView=${numberTextView != null}, unitTextView=${unitTextView != null}")
                        }
                    }
                    else -> {
                        when {
                            properties.validationType == AddItemViewModel.ValidationType.DATE -> {
                                val dateTextView = findTextViewInView(view, DATE_TAG)
                                if (dateTextView != null && value is String && value != fieldName) {
                                    dateTextView.text = value
                                    Log.d("FieldValueManager", "恢复日期字段 $fieldName: ${dateTextView.text}")
                                }
                            }
                            properties.options != null -> {
                                val spinnerTextView = findTextViewInView(view, SPINNER_TAG)
                                if (spinnerTextView != null && value is String && value != fieldName) {
                                    spinnerTextView.text = value
                                    Log.d("FieldValueManager", "恢复选项字段 $fieldName: ${spinnerTextView.text}")
                                }
                            }
                            properties.unitOptions != null -> {
                                Log.d("FieldValueManager", "恢复带单位字段 $fieldName")
                                if (view !is LinearLayout) {
                                    Log.d("FieldValueManager", "带单位字段 $fieldName 视图不是 LinearLayout")
                                    return@forEach
                                }

                                val editText = findEditTextInView(view)
                                val unitTextView = findTextViewInView(view, "unit_textview")

                                Log.d("FieldValueManager", "带单位字段 $fieldName 找到控件: editText=${editText != null}, unitTextView=${unitTextView != null}")

                                if (editText == null || unitTextView == null) return@forEach

                                // 恢复数值
                                if (value is String) {
                                    editText.setText(value)
                                    Log.d("FieldValueManager", "带单位字段 $fieldName 设置值: $value")
                                }

                                // 恢复单位
                                val unitValue = viewModel.getFieldValue("${fieldName}_unit") as? String
                                Log.d("FieldValueManager", "带单位字段 $fieldName 获取单位: $unitValue")

                                if (!unitValue.isNullOrEmpty()) {
                                    unitTextView.text = unitValue
                                    Log.d("FieldValueManager", "带单位字段 $fieldName 设置单位: $unitValue")
                                } else if (properties.unitOptions?.isNotEmpty() == true) {
                                    // 如果没有保存单位或单位为空，使用第一个可选单位
                                    val defaultUnit = properties.unitOptions.first()
                                    unitTextView.text = defaultUnit
                                    Log.d("FieldValueManager", "带单位字段 $fieldName 设置默认单位: $defaultUnit")
                                }
                            }
                            else -> {
                                when (view) {
                                    is EditText -> {
                                        if (value is String && value.isNotEmpty()) {
                                            view.setText(value)
                                            Log.d("FieldValueManager", "恢复文本字段 $fieldName: $value")
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
                Log.d("FieldValueManager", "找到 EditText: ${view.id}")
                view
            }
            is ViewGroup -> {
                // 首先尝试直接获取，这样更快
                if (view is LinearLayout) {
                    for (i in 0 until view.childCount) {
                        val child = view.getChildAt(i)
                        if (child is EditText) {
                            Log.d("FieldValueManager", "在 LinearLayout 中直接找到 EditText: ${child.id}")
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

                Log.d("FieldValueManager", "在 ${view.javaClass.simpleName} 中未找到 EditText")
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
                    Log.d("FieldValueManager", "找到 TextView: tag=${view.tag}, 寻找的tag=$tag")
                    view
                } else null
            }
            is ViewGroup -> {
                // 首先尝试直接获取，这样更快
                if (view is LinearLayout) {
                    for (i in 0 until view.childCount) {
                        val child = view.getChildAt(i)
                        if (child is TextView && child !is EditText && (tag == null || child.tag == tag)) {
                            Log.d("FieldValueManager", "在 LinearLayout 中直接找到 TextView: tag=${child.tag}")
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

                if (tag != null) {
                    Log.d("FieldValueManager", "在 ${view.javaClass.simpleName} 中未找到 tag=$tag 的 TextView")
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
} 