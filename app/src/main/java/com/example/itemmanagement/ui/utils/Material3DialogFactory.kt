package com.example.itemmanagement.ui.utils

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Material 3 风格对话框工厂类
 * 统一管理所有对话框的创建，确保符合Material 3设计规范
 */
object Material3DialogFactory {

    /**
     * 创建基础的Material 3对话框构建器
     */
    fun createBuilder(context: Context): MaterialAlertDialogBuilder {
        return MaterialAlertDialogBuilder(context)
    }

    /**
     * 创建选择对话框
     */
    fun createSelectionDialog(
        context: Context,
        title: String,
        items: List<String>,
        onItemSelected: (String, Int) -> Unit,
        onNeutralClick: (() -> Unit)? = null,
        onPositiveClick: (() -> Unit)? = null,
        neutralText: String? = null,
        positiveText: String? = null
    ): MaterialAlertDialogBuilder {
        // 使用自定义适配器来避免文字加粗
        val adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, items) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<android.widget.TextView>(android.R.id.text1)
                textView?.typeface = android.graphics.Typeface.DEFAULT
                return view
            }
        }
        
        val builder = createBuilder(context)
            .setTitle(title)
            .setAdapter(adapter) { _, which ->
                onItemSelected(items[which], which)
            }
            .setNegativeButton("取消", null)

        // 添加可选按钮
        neutralText?.let { text ->
            builder.setNeutralButton(text) { _, _ -> onNeutralClick?.invoke() }
        }

        positiveText?.let { text ->
            builder.setPositiveButton(text) { _, _ -> onPositiveClick?.invoke() }
        }

        return builder
    }

    /**
     * 创建输入对话框
     */
    fun createInputDialog(
        context: Context,
        title: String,
        hint: String,
        initialText: String = "",
        onConfirm: (String) -> Unit
    ): MaterialAlertDialogBuilder {
        val editText = EditText(context).apply {
            this.hint = hint
            setText(initialText)
            if (initialText.isNotEmpty()) {
                setSelection(initialText.length)
            }
        }

        // 创建布局容器
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        layout.addView(editText)

        return createBuilder(context)
            .setTitle(title)
            .setView(layout)
            .setPositiveButton("确定") { _, _ ->
                val text = editText.text.toString().trim()
                onConfirm(text)
            }
            .setNegativeButton("取消", null)
    }

    /**
     * 创建确认对话框
     */
    fun createConfirmDialog(
        context: Context,
        title: String,
        message: String,
        onConfirm: () -> Unit,
        positiveText: String = "确定",
        negativeText: String = "取消"
    ): MaterialAlertDialogBuilder {
        return createBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText) { _, _ -> onConfirm() }
            .setNegativeButton(negativeText, null)
    }

    /**
     * 创建操作选择对话框
     */
    fun createActionDialog(
        context: Context,
        title: String,
        actions: Array<String>,
        onActionSelected: (Int) -> Unit
    ): MaterialAlertDialogBuilder {
        // 使用自定义适配器来避免文字加粗
        val adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, actions.toList()) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<android.widget.TextView>(android.R.id.text1)
                textView?.typeface = android.graphics.Typeface.DEFAULT
                return view
            }
        }
        
        return createBuilder(context)
            .setTitle(title)
            .setAdapter(adapter) { _, which ->
                onActionSelected(which)
            }
    }

    /**
     * 创建数字选择对话框
     */
    fun createNumberSelectionDialog(
        context: Context,
        title: String,
        numbers: Array<String>,
        onNumberSelected: (String) -> Unit
    ): MaterialAlertDialogBuilder {
        // 使用自定义适配器来避免文字加粗
        val adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, numbers.toList()) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<android.widget.TextView>(android.R.id.text1)
                textView?.typeface = android.graphics.Typeface.DEFAULT
                return view
            }
        }
        
        return createBuilder(context)
            .setTitle(title)
            .setAdapter(adapter) { _, which ->
                onNumberSelected(numbers[which])
            }
            .setNegativeButton("取消", null)
    }
}
