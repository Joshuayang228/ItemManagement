package com.example.itemmanagement.ui.add

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog as MaterialAlertDialog
import com.example.itemmanagement.R
import com.example.itemmanagement.ui.base.FieldInteractionViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*

/**
 * 负责创建和显示各种对话框的工厂类
 */
class DialogFactory(private val context: Context) {

    companion object {
        // 静态变量，用于记录当前应用会话中是否已显示过提示
        private var unitDialogTipShownThisSession = false
        private var optionDialogTipShownThisSession = false
    }

    // 检查是否在当前会话中已经显示过提示，如果没有则显示并记录
    private fun showTipIfFirstTimeInSession(isUnitDialog: Boolean, message: String) {
        if (isUnitDialog) {
            if (!unitDialogTipShownThisSession) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                unitDialogTipShownThisSession = true
            }
        } else {
            if (!optionDialogTipShownThisSession) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                optionDialogTipShownThisSession = true
            }
        }
    }

    /**
     * 显示日期选择器
     */
    fun showDatePicker(textView: TextView) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendar.set(year, month, day)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                textView.text = dateFormat.format(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /**
     * 显示标签操作对话框（编辑/删除）
     */
    fun showTagActionDialog(
        tagName: String,
        chip: Chip,
        selectedTags: MutableSet<String>,
        defaultTags: MutableList<String>,
        customTags: MutableList<String>,
        allTags: MutableList<String>
    ) {
        val items = arrayOf("编辑", "删除")
        MaterialAlertDialogBuilder(context)
            .setTitle("标签操作")
            .setItems(items) { dialog, which ->
                when (which) {
                    0 -> { // 编辑
                        showEditTagDialog(tagName, chip, selectedTags, defaultTags, customTags, allTags)
                    }
                    1 -> { // 删除
                        MaterialAlertDialogBuilder(context)
                            .setTitle("删除标签")
                            .setMessage("是否删除标签\"${tagName}\"？")
                            .setPositiveButton("删除") { _, _ ->
                                (chip.parent as? ViewGroup)?.removeView(chip)
                                selectedTags.remove(tagName)
                                Toast.makeText(context, "已从选中标签中移除\"${tagName}\"", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("取消", null)
                            .show()
                    }
                }
            }
            .show()
    }

    /**
     * 显示编辑标签对话框
     */
    private fun showEditTagDialog(
        tagName: String,
        chip: Chip,
        selectedTags: MutableSet<String>,
        defaultTags: MutableList<String>,
        customTags: MutableList<String>,
        allTags: MutableList<String>
    ) {
        val editText = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setText(tagName)
            setSelection(tagName.length)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("编辑标签")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newTagName = editText.text.toString().trim()
                if (newTagName.isNotEmpty() && newTagName != tagName) {
                    // 检查新名称是否已存在
                    if (!defaultTags.contains(newTagName) && !customTags.contains(newTagName) && !selectedTags.contains(newTagName)) {
                        // 更新标签名称
                        if (defaultTags.contains(tagName)) {
                            defaultTags[defaultTags.indexOf(tagName)] = newTagName
                        } else if (customTags.contains(tagName)) {
                            customTags[customTags.indexOf(tagName)] = newTagName
                        }

                        // 更新选中集合
                        selectedTags.remove(tagName)
                        selectedTags.add(newTagName)

                        // 更新标签UI
                        chip.text = newTagName

                        // 更新所有标签列表
                        allTags.clear()
                        allTags.addAll(defaultTags)
                        allTags.addAll(customTags)

                        Toast.makeText(context, "标签已更新", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "该标签名称已存在", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示标签选择对话框
     */
    fun showTagSelectionDialog(
        selectedTags: MutableSet<String>,
        defaultTags: MutableList<String>,
        customTags: MutableList<String>,
        allTags: MutableList<String>,
        selectedTagsContainer: ChipGroup,
        onTagSelected: (String) -> Unit,
        viewModel: FieldInteractionViewModel,
        fieldName: String
    ) {
        // 更新所有标签列表
        allTags.clear()
        allTags.addAll(defaultTags)
        allTags.addAll(customTags)

        // 创建自定义标题视图
        val customTitleView = LayoutInflater.from(context).inflate(R.layout.dialog_title_with_tags_button, null)
        val titleTextView = customTitleView.findViewById<TextView>(R.id.dialog_title)
        val addButton = customTitleView.findViewById<ImageButton>(R.id.btn_add)

        // 设置标题文本
        titleTextView.text = "选择标签"

        // 设置添加按钮点击事件
        addButton.setOnClickListener {
            val dialog = (it.parent.parent as? androidx.appcompat.app.AlertDialog)
            showAddNewTagDialog(
                selectedTags,
                defaultTags,
                customTags,
                allTags,
                selectedTagsContainer,
                onTagSelected,
                dialog,
                viewModel,
                fieldName
            )
        }

        // 创建多选对话框
        val items = allTags.toTypedArray()

        // 确保使用最新的UI状态来设置选中项
        val currentSelectedTags = mutableSetOf<String>()
        for (i in 0 until selectedTagsContainer.childCount) {
            val chip = selectedTagsContainer.getChildAt(i) as? Chip
            if (chip != null) {
                currentSelectedTags.add(chip.text.toString())
            }
        }

        // 使用UI中的实际选中状态
        val checkedItems = BooleanArray(items.size) { i -> currentSelectedTags.contains(items[i]) }

        // 同步selectedTags与UI状态
        selectedTags.clear()
        selectedTags.addAll(currentSelectedTags)

        val dialog = MaterialAlertDialogBuilder(context)
            .setCustomTitle(customTitleView)
            .setMultiChoiceItems(items, checkedItems) { dialog, which, isChecked ->
                val tagName = items[which]
                if (isChecked) {
                    if (!selectedTags.contains(tagName)) {
                        onTagSelected(tagName)
                    }
                } else {
                    if (selectedTags.contains(tagName)) {
                        selectedTags.remove(tagName)

                        // 从UI中移除对应的标签
                        for (i in 0 until selectedTagsContainer.childCount) {
                            val chip = selectedTagsContainer.getChildAt(i) as? Chip
                            if (chip?.text == tagName) {
                                selectedTagsContainer.removeView(chip)
                                break
                            }
                        }
                    }
                }
            }
            .setPositiveButton("确定") { _, _ ->
                // 保存最终的选中状态到ViewModel
                viewModel.saveFieldValue(fieldName, selectedTags.toSet())
            }
            .setNeutralButton("管理标签") { _, _ ->
                showManageTagsDialog(defaultTags, customTags, allTags, selectedTags, viewModel, fieldName)
            }
            .create()

        // 防止长按项目导致崩溃
        dialog.listView?.setOnItemLongClickListener { _, _, _, _ -> true }

        dialog.show()
    }

    /**
     * 显示添加新标签对话框
     */
    private fun showAddNewTagDialog(
        selectedTags: MutableSet<String>,
        defaultTags: MutableList<String>,
        customTags: MutableList<String>,
        allTags: MutableList<String>,
        selectedTagsContainer: ChipGroup,
        onTagAdded: (String) -> Unit,
        parentDialog: MaterialAlertDialog? = null,
        viewModel: FieldInteractionViewModel,
        fieldName: String
    ) {
        val editText = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            hint = "请输入标签名称"
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("添加新标签")
            .setView(editText)
            .setPositiveButton("确定") { dialog, _ ->
                val tagName = editText.text.toString().trim()
                if (tagName.isNotEmpty()) {
                    // 检查标签是否已存在
                    if (!defaultTags.contains(tagName) && !customTags.contains(tagName)) {
                        customTags.add(tagName)
                        allTags.add(tagName)
                        // 关闭所有对话框
                        dialog.dismiss()
                        parentDialog?.dismiss()
                        // 回调添加标签
                        onTagAdded(tagName)
                        // 重新显示标签选择对话框
                        showTagSelectionDialog(
                            selectedTags,
                            defaultTags,
                            customTags,
                            allTags,
                            selectedTagsContainer,
                            onTagAdded,
                            viewModel,
                            fieldName
                        )
                    } else if (!selectedTags.contains(tagName)) {
                        // 如果标签已存在但未选中，则选中它
                        // 关闭所有对话框
                        dialog.dismiss()
                        parentDialog?.dismiss()
                        onTagAdded(tagName)
                        // 重新显示标签选择对话框
                        showTagSelectionDialog(
                            selectedTags,
                            defaultTags,
                            customTags,
                            allTags,
                            selectedTagsContainer,
                            onTagAdded,
                            viewModel,
                            fieldName
                        )
                    } else {
                        Toast.makeText(context, "该标签已存在且已选中", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .create()

        dialog.show()
    }

    /**
     * 显示管理标签对话框
     */
    private fun showManageTagsDialog(
        defaultTags: MutableList<String>,
        customTags: MutableList<String>,
        allTags: MutableList<String>,
        selectedTags: MutableSet<String>,
        viewModel: FieldInteractionViewModel,
        fieldName: String
    ) {
        // 更新所有标签列表
        allTags.clear()
        allTags.addAll(defaultTags)
        allTags.addAll(customTags)

        // 创建用于批量操作的列表
        val items = allTags.toTypedArray()
        val checkedItems = BooleanArray(items.size) { false }
        val selectedForOperation = mutableSetOf<String>()

        // 创建对话框，但不立即显示
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("管理标签")
            .setMultiChoiceItems(items, checkedItems) { _, which, isChecked ->
                val tagName = items[which]
                if (isChecked) {
                    selectedForOperation.add(tagName)
                } else {
                    selectedForOperation.remove(tagName)
                }
            }
            .setPositiveButton("删除选中项", null) // 设置为 null，我们将在后面设置点击监听器
            .setNegativeButton("取消", null)
            .setNeutralButton("添加新标签") { _, _ ->
                showAddNewTagDialog(
                    selectedTags,
                    defaultTags,
                    customTags,
                    allTags,
                    ChipGroup(context),  // 传入一个临时的ChipGroup，因为这里不需要实际添加到UI
                    { _ -> },            // 空的回调函数
                    null,                // 无父对话框
                    viewModel,           // 添加 ViewModel 参数
                    fieldName            // 添加字段名参数
                )
            }
            .create()

        // 设置"删除选中项"按钮的点击监听器
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                if (selectedForOperation.isEmpty()) {
                    Toast.makeText(context, "请先选择要删除的标签", Toast.LENGTH_SHORT).show()
                } else {
                    // 创建确认删除的对话框
                    MaterialAlertDialogBuilder(context)
                        .setTitle("批量删除")
                        .setMessage("确定要删除选中的 ${selectedForOperation.size} 个标签吗？")
                        .setPositiveButton("确定") { _, _ ->
                            // 从所有列表中删除选中的标签
                            defaultTags.removeAll(selectedForOperation)
                            customTags.removeAll(selectedForOperation)

                            // 从已选中的标签中也删除这些标签
                            val tagsToRemove = selectedTags.intersect(selectedForOperation)
                            if (tagsToRemove.isNotEmpty()) {
                                selectedTags.removeAll(tagsToRemove)
                                // 通知 ViewModel 删除标签
                                tagsToRemove.forEach { tag ->
                                    viewModel.removeCustomTag(fieldName, tag)
                                }
                            }

                            // 更新所有标签列表
                            allTags.clear()
                            allTags.addAll(defaultTags)
                            allTags.addAll(customTags)

                            Toast.makeText(context, "已删除 ${selectedForOperation.size} 个标签", Toast.LENGTH_SHORT).show()
                            dialog.dismiss() // 只有在确认删除后才关闭管理标签对话框
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
            }
        }

        dialog.show()
    }

    /**
     * 显示单位选择对话框
     */
    fun showUnitSelectionDialog(
        title: String,
        units: List<String>,
        defaultUnits: MutableList<String>,
        customUnits: MutableList<String>,
        isCustomizable: Boolean,
        currentTextView: TextView,
        onUnitSelected: (String) -> Unit,
        onEditUnit: (String, String) -> Unit,
        onDeleteUnit: (String) -> Unit,
        onAddUnit: (String) -> Unit
    ) {
        val items = units.toTypedArray()

        // 使用XML布局文件创建自定义标题视图
        val customTitleView = LayoutInflater.from(context).inflate(R.layout.dialog_title_with_add_button, null)
        val titleTextView = customTitleView.findViewById<TextView>(R.id.dialog_title)
        val addButton = customTitleView.findViewById<ImageButton>(R.id.btn_add)

        // 设置标题文本
        titleTextView.text = title

        // 如果不支持自定义，隐藏添加按钮
        if (!isCustomizable) {
            addButton.visibility = android.view.View.GONE
        }

        // 创建选择对话框
        fun createSelectionDialog() {
            val dialog = MaterialAlertDialogBuilder(context)
                .setCustomTitle(customTitleView)
                .setItems(items) { dialog, which ->
                    onUnitSelected(items[which])
                    dialog.dismiss()
                }
                .create()

            // 设置列表项长按监听
            dialog.listView?.setOnItemLongClickListener { _, view, position, _ ->
                val selectedUnit = items[position]
                showUnitActionDialog(
                    selectedUnit,
                    defaultUnits,
                    customUnits,
                    onEditUnit,
                    onDeleteUnit
                ) {
                    // 在操作完成后重新创建对话框
                    dialog.dismiss()
                    showUnitSelectionDialog(
                        title,
                        defaultUnits + customUnits,
                        defaultUnits,
                        customUnits,
                        isCustomizable,
                        currentTextView,
                        onUnitSelected,
                        onEditUnit,
                        onDeleteUnit,
                        onAddUnit
                    )
                }
                true
            }

            // 设置添加按钮点击事件
            addButton.setOnClickListener {
                showAddCustomUnitDialog(
                    defaultUnits,
                    customUnits,
                    dialog
                ) { newUnit ->
                    onAddUnit(newUnit)
                    // 关闭当前对话框并重新显示
                    dialog.dismiss()
                    showUnitSelectionDialog(
                        title,
                        defaultUnits + customUnits,
                        defaultUnits,
                        customUnits,
                        isCustomizable,
                        currentTextView,
                        onUnitSelected,
                        onEditUnit,
                        onDeleteUnit,
                        onAddUnit
                    )
                }
            }

            dialog.show()

            // 在当前会话的第一次打开对话框时显示提示
            if (isCustomizable) {
                showTipIfFirstTimeInSession(true, "常按选项可编辑或删除")
            }
        }

        // 显示对话框
        createSelectionDialog()
    }

    /**
     * 显示单位操作对话框（编辑/删除）
     */
    private fun showUnitActionDialog(
        unit: String,
        defaultUnits: MutableList<String>,
        customUnits: MutableList<String>,
        onEditUnit: (String, String) -> Unit,
        onDeleteUnit: (String) -> Unit,
        onActionComplete: () -> Unit
    ) {
        val items = arrayOf("编辑", "删除")
        MaterialAlertDialogBuilder(context)
            .setTitle("选项操作")
            .setItems(items) { dialog, which ->
                when (which) {
                    0 -> { // 编辑
                        showEditUnitDialog(
                            unit,
                            defaultUnits,
                            customUnits,
                            onEditUnit
                        ) {
                            dialog.dismiss()
                            onActionComplete()
                        }
                    }
                    1 -> { // 删除
                        showDeleteUnitConfirmDialog(
                            unit,
                            onDeleteUnit
                        ) {
                            dialog.dismiss()
                            onActionComplete()
                        }
                    }
                }
            }
            .show()
    }

    /**
     * 显示编辑单位对话框
     */
    private fun showEditUnitDialog(
        unit: String,
        defaultUnits: MutableList<String>,
        customUnits: MutableList<String>,
        onUnitEdited: (String, String) -> Unit,
        onEditComplete: () -> Unit
    ) {
        val editText = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setText(unit)
            setSelection(unit.length)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("编辑选项")
            .setView(editText)
            .setPositiveButton("确定") { dialog, _ ->
                val newUnit = editText.text.toString().trim()
                if (newUnit.isNotEmpty() && newUnit != unit) {
                    // 检查是否已存在
                    if (!defaultUnits.contains(newUnit) && !customUnits.contains(newUnit)) {
                        onUnitEdited(unit, newUnit)
                        dialog.dismiss()
                        onEditComplete()
                    } else {
                        Toast.makeText(context, "该选项已存在", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示删除单位确认对话框
     */
    private fun showDeleteUnitConfirmDialog(
        unit: String,
        onUnitDeleted: (String) -> Unit,
        onDeleteComplete: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle("删除选项")
            .setMessage("是否删除选项\"${unit}\"？")
            .setPositiveButton("删除") { dialog, _ ->
                onUnitDeleted(unit)
                dialog.dismiss()
                onDeleteComplete()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示添加自定义单位对话框
     */
    private fun showAddCustomUnitDialog(
        defaultUnits: MutableList<String>,
        customUnits: MutableList<String>,
        parentDialog: MaterialAlertDialog,
        onUnitAdded: (String) -> Unit
    ) {
        val editText = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            hint = "请输入选项名称"
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("添加新选项")
            .setView(editText)
            .setPositiveButton("确定") { dialog, _ ->
                val unitName = editText.text.toString().trim()
                if (unitName.isNotEmpty()) {
                    // 检查是否已存在
                    if (!defaultUnits.contains(unitName) && !customUnits.contains(unitName)) {
                        customUnits.add(unitName)
                        dialog.dismiss()
                        parentDialog.dismiss()
                        onUnitAdded(unitName)
                    } else {
                        Toast.makeText(context, "该选项已存在", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示选项选择对话框
     */
    fun showOptionSelectionDialog(
        title: String,
        options: List<String>,
        defaultOptions: MutableList<String>,
        customOptions: MutableList<String>,
        isCustomizable: Boolean,
        currentTextView: TextView,
        onOptionSelected: (String) -> Unit,
        onEditOption: (String, String) -> Unit,
        onDeleteOption: (String) -> Unit,
        onAddOption: (String) -> Unit
    ) {
        val items = options.toTypedArray()

        // 使用XML布局文件创建自定义标题视图
        val customTitleView = LayoutInflater.from(context).inflate(R.layout.dialog_title_with_add_button, null)
        val titleTextView = customTitleView.findViewById<TextView>(R.id.dialog_title)
        val addButton = customTitleView.findViewById<ImageButton>(R.id.btn_add)

        // 设置标题文本
        titleTextView.text = title

        // 如果不支持自定义，隐藏添加按钮
        if (!isCustomizable) {
            addButton.visibility = android.view.View.GONE
        }

        // 创建选择对话框
        fun createSelectionDialog() {
            val dialog = MaterialAlertDialogBuilder(context)
                .setCustomTitle(customTitleView)
                .setItems(items) { dialog, which ->
                    onOptionSelected(items[which])
                    dialog.dismiss()
                }
                .create()

            // 设置列表项长按监听
            dialog.listView?.setOnItemLongClickListener { _, _, position, _ ->
                val selectedOption = items[position]
                showOptionActionDialog(
                    selectedOption,
                    defaultOptions,
                    customOptions,
                    onEditOption,
                    onDeleteOption
                ) {
                    // 在操作完成后重新创建对话框
                    dialog.dismiss()
                    showOptionSelectionDialog(
                        title,
                        defaultOptions + customOptions,
                        defaultOptions,
                        customOptions,
                        isCustomizable,
                        currentTextView,
                        onOptionSelected,
                        onEditOption,
                        onDeleteOption,
                        onAddOption
                    )
                }
                true
            }

            // 设置添加按钮点击事件
            if (isCustomizable) {
                addButton.setOnClickListener {
                    showAddCustomOptionDialog(
                        defaultOptions,
                        customOptions,
                        dialog
                    ) { newOption ->
                        onAddOption(newOption)
                        // 关闭当前对话框并重新显示
                        dialog.dismiss()
                        showOptionSelectionDialog(
                            title,
                            defaultOptions + customOptions,
                            defaultOptions,
                            customOptions,
                            isCustomizable,
                            currentTextView,
                            onOptionSelected,
                            onEditOption,
                            onDeleteOption,
                            onAddOption
                        )
                    }
                }
            }

            dialog.show()

            // 在当前会话的第一次打开对话框时显示提示
            if (isCustomizable) {
                showTipIfFirstTimeInSession(false, "常按选项可编辑或删除")
            }
        }

        // 显示对话框
        createSelectionDialog()
    }

    /**
     * 显示选项操作对话框（编辑/删除）
     */
    private fun showOptionActionDialog(
        option: String,
        defaultOptions: MutableList<String>,
        customOptions: MutableList<String>,
        onEditOption: (String, String) -> Unit,
        onDeleteOption: (String) -> Unit,
        onActionComplete: () -> Unit
    ) {
        val items = arrayOf("编辑", "删除")
        MaterialAlertDialogBuilder(context)
            .setTitle("选项操作")
            .setItems(items) { dialog, which ->
                when (which) {
                    0 -> { // 编辑
                        showEditOptionDialog(
                            option,
                            defaultOptions,
                            customOptions,
                            onEditOption
                        ) {
                            dialog.dismiss()
                            onActionComplete()
                        }
                    }
                    1 -> { // 删除
                        showDeleteOptionDialog(
                            option,
                            onDeleteOption
                        ) {
                            dialog.dismiss()
                            onActionComplete()
                        }
                    }
                }
            }
            .show()
    }

    /**
     * 显示编辑选项对话框
     */
    private fun showEditOptionDialog(
        option: String,
        defaultOptions: MutableList<String>,
        customOptions: MutableList<String>,
        onOptionEdited: (String, String) -> Unit,
        onEditComplete: () -> Unit
    ) {
        val editText = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setText(option)
            setSelection(option.length)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("编辑选项")
            .setView(editText)
            .setPositiveButton("确定") { dialog, _ ->
                val newOption = editText.text.toString().trim()
                if (newOption.isNotEmpty() && newOption != option) {
                    // 检查是否已存在
                    if (!defaultOptions.contains(newOption) && !customOptions.contains(newOption)) {
                        onOptionEdited(option, newOption)
                        dialog.dismiss()
                        onEditComplete()
                    } else {
                        Toast.makeText(context, "该选项已存在", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示删除选项确认对话框
     */
    private fun showDeleteOptionDialog(
        option: String,
        onOptionDeleted: (String) -> Unit,
        onDeleteComplete: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle("删除选项")
            .setMessage("是否删除选项\"${option}\"？")
            .setPositiveButton("删除") { dialog, _ ->
                onOptionDeleted(option)
                dialog.dismiss()
                onDeleteComplete()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示数字选择对话框
     */
    fun showNumberSelectionDialog(
        numbers: Array<String>,
        onNumberSelected: (String) -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle("选择数字")
            .setItems(numbers) { dialog, which ->
                onNumberSelected(numbers[which])
                dialog.dismiss()
            }
            .show()
    }

    /**
     * 显示添加自定义选项对话框
     */
    private fun showAddCustomOptionDialog(
        defaultOptions: MutableList<String>,
        customOptions: MutableList<String>,
        parentDialog: MaterialAlertDialog,
        onOptionAdded: (String) -> Unit
    ) {
        val editText = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            hint = "请输入选项名称"
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("添加新选项")
            .setView(editText)
            .setPositiveButton("确定") { dialog, _ ->
                val optionName = editText.text.toString().trim()
                if (optionName.isNotEmpty()) {
                    // 检查是否已存在
                    if (!defaultOptions.contains(optionName) && !customOptions.contains(optionName)) {
                        customOptions.add(optionName)
                        dialog.dismiss()
                        parentDialog.dismiss()
                        onOptionAdded(optionName)
                    } else {
                        Toast.makeText(context, "该选项已存在", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 创建一个基础的选择对话框
     * @param title 对话框标题
     * @param items 选项列表
     * @param onItemSelected 选项选中回调
     */
    fun createDialog(
        title: String,
        items: Array<String>,
        onItemSelected: (Int) -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setItems(items) { _: DialogInterface, which: Int ->
                onItemSelected(which)
            }
            .show()
    }

    /**
     * 创建一个确认对话框
     * @param title 对话框标题
     * @param message 对话框内容
     * @param positiveButtonText 确认按钮文本
     * @param negativeButtonText 取消按钮文本
     * @param onPositiveClick 确认按钮点击回调
     * @param onNegativeClick 取消按钮点击回调
     */
    fun createConfirmDialog(
        title: String,
        message: String,
        positiveButtonText: String = "确定",
        negativeButtonText: String = "取消",
        onPositiveClick: () -> Unit = {},
        onNegativeClick: () -> Unit = {}
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { _, _ -> onPositiveClick() }
            .setNegativeButton(negativeButtonText) { _, _ -> onNegativeClick() }
            .show()
    }

    /**
     * 创建一个单选对话框
     * @param title 对话框标题
     * @param items 选项列表
     * @param checkedItem 默认选中项
     * @param onItemSelected 选项选中回调
     */
    fun createSingleChoiceDialog(
        title: String,
        items: Array<String>,
        checkedItem: Int = 0,
        onItemSelected: (Int) -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setSingleChoiceItems(items, checkedItem) { dialog, which ->
                onItemSelected(which)
                dialog.dismiss()
            }
            .show()
    }

    /**
     * 创建一个多选对话框
     * @param title 对话框标题
     * @param items 选项列表
     * @param checkedItems 选中状态数组
     * @param onItemsSelected 选项选中状态变化回调
     */
    fun createMultiChoiceDialog(
        title: String,
        items: Array<String>,
        checkedItems: BooleanArray,
        onItemsSelected: (BooleanArray) -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMultiChoiceItems(items, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("确定") { _, _ -> onItemsSelected(checkedItems) }
            .setNegativeButton("取消", null)
            .show()
    }
} 