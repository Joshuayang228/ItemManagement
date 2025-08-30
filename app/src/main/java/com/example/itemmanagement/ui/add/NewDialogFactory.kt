package com.example.itemmanagement.ui.add

import android.app.DatePickerDialog
import android.content.Context
import android.widget.*
import androidx.appcompat.app.AlertDialog as MaterialAlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*

/**
 * 新架构的对话框工厂类
 * 简化版本，专门为新架构设计，不依赖旧的AddItemViewModel
 */
class NewDialogFactory(private val context: Context) {

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
     * 显示日期选择器（回调版本）
     */
    fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendar.set(year, month, day)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                onDateSelected(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /**
     * 创建简单的选项对话框
     */
    fun createDialog(
        title: String,
        items: Array<String>,
        onItemSelected: (Int) -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setItems(items) { _, which ->
                onItemSelected(which)
            }
            .show()
    }

    /**
     * 创建多选对话框
     */
    fun createMultiChoiceDialog(
        title: String,
        items: Array<String>,
        checkedItems: BooleanArray,
        onSelectionChanged: (BooleanArray) -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMultiChoiceItems(items, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("确定") { _, _ ->
                onSelectionChanged(checkedItems)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示单选对话框
     */
    fun showSingleChoiceDialog(
        title: String,
        items: Array<String>,
        selectedIndex: Int = -1,
        onItemSelected: (Int, String) -> Unit
    ) {
        var selection = selectedIndex
        
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setSingleChoiceItems(items, selectedIndex) { _, which ->
                selection = which
            }
            .setPositiveButton("确定") { _, _ ->
                if (selection != -1) {
                    onItemSelected(selection, items[selection])
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示输入对话框
     */
    fun showInputDialog(
        title: String,
        hint: String = "",
        defaultValue: String = "",
        onInputConfirmed: (String) -> Unit
    ) {
        val editText = EditText(context).apply {
            this.hint = hint
            setText(defaultValue)
            if (defaultValue.isNotEmpty()) {
                selectAll()
            }
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val input = editText.text.toString().trim()
                if (input.isNotEmpty()) {
                    onInputConfirmed(input)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示数字输入对话框
     */
    fun showNumberInputDialog(
        title: String,
        hint: String = "",
        defaultValue: String = "",
        onNumberConfirmed: (String) -> Unit
    ) {
        val editText = EditText(context).apply {
            this.hint = hint
            setText(defaultValue)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            if (defaultValue.isNotEmpty()) {
                selectAll()
            }
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val input = editText.text.toString().trim()
                if (input.isNotEmpty()) {
                    onNumberConfirmed(input)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示确认对话框
     */
    fun showConfirmationDialog(
        title: String,
        message: String,
        onConfirmed: () -> Unit,
        onCancelled: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("确定") { _, _ -> onConfirmed() }
            .setNegativeButton("取消") { _, _ -> onCancelled?.invoke() }
            .show()
    }

    /**
     * 显示信息对话框
     */
    fun showInfoDialog(
        title: String,
        message: String,
        onDismissed: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("知道了") { _, _ -> onDismissed?.invoke() }
            .show()
    }

    /**
     * 显示数字带单位的输入对话框
     */
    fun showNumberWithUnitDialog(
        title: String,
        numberHint: String = "输入数字",
        unitOptions: Array<String>,
        defaultNumber: String = "",
        defaultUnitIndex: Int = 0,
        onConfirmed: (String, String) -> Unit
    ) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
        }

        val numberEdit = EditText(context).apply {
            hint = numberHint
            setText(defaultNumber)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val unitSpinner = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, unitOptions).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(defaultUnitIndex)
        }

        container.addView(numberEdit)
        container.addView(unitSpinner)

        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(container)
            .setPositiveButton("确定") { _, _ ->
                val number = numberEdit.text.toString().trim()
                val unit = unitOptions[unitSpinner.selectedItemPosition]
                if (number.isNotEmpty()) {
                    onConfirmed(number, unit)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示评分对话框
     */
    fun showRatingDialog(
        title: String = "评分",
        currentRating: Float = 0f,
        onRatingChanged: (Float) -> Unit
    ) {
        val ratingBar = RatingBar(context).apply {
            numStars = 5
            stepSize = 0.5f
            rating = currentRating
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(ratingBar)
            .setPositiveButton("确定") { _, _ ->
                onRatingChanged(ratingBar.rating)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}