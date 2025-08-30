package com.example.itemmanagement.ui.add

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.example.itemmanagement.ui.base.FieldInteractionViewModel

/**
 * 新架构的标签管理器
 * 负责管理标签相关的逻辑，基于FieldInteractionViewModel接口
 */
class TagManager(
    private val context: Context,
    private val viewModel: FieldInteractionViewModel,
    private val fieldName: String = "标签" // 默认字段名
) {

    // 标签集合
    private val selectedTags = mutableSetOf<String>()
    private val defaultTags = mutableListOf<String>()

    /**
     * 初始化标签管理器
     */
    fun initialize(defaultTagList: List<String>?) {
        defaultTags.clear()
        if (defaultTagList != null) {
            defaultTags.addAll(defaultTagList)
        }
    }

    /**
     * 获取自定义标签
     */
    private fun getCustomTags(): List<String> {
        return viewModel.getCustomOptions(fieldName)
    }

    /**
     * 获取所有标签
     */
    private fun getAllTags(): List<String> {
        return defaultTags + getCustomTags()
    }

    /**
     * 添加标签到已选容器
     */
    fun addTagToContainer(tagName: String, container: ChipGroup): Boolean {
        // 如果标签已经被选中，不重复添加
        if (selectedTags.contains(tagName)) {
            return false
        }

        val chip = createChip(tagName, container)
        container.addView(chip)
        selectedTags.add(tagName)

        // 保存选中状态到 ViewModel
        viewModel.saveFieldValue(fieldName, selectedTags.toSet())

        // 隐藏提示文本
        findPlaceholderTextView(container)?.visibility = View.GONE

        return true
    }

    /**
     * 创建标签Chip
     */
    private fun createChip(tagName: String, container: ChipGroup): Chip {
        return Chip(context).apply {
            text = tagName
            isCheckable = false
            isCloseIconVisible = true

            // 设置Chip的样式和大小
            chipMinHeight = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                28f,
                context.resources.displayMetrics
            )

            chipStartPadding = 6f
            chipEndPadding = 6f
            closeIconEndPadding = 2f
            closeIconStartPadding = 2f

            // 点击关闭图标移除标签
            setOnCloseIconClickListener {
                container.removeView(this)
                selectedTags.remove(tagName)

                // 保存更新后的状态到 ViewModel
                viewModel.saveFieldValue(fieldName, selectedTags.toSet())

                // 如果没有标签了，显示提示文本
                if (container.childCount == 0) {
                    findPlaceholderTextView(container)?.visibility = View.VISIBLE
                }
            }

            // 长按编辑或删除标签
            setOnLongClickListener {
                showTagActionDialog(tagName, this)
                true
            }
        }
    }

    /**
     * 显示标签操作对话框（长按时）
     */
    private fun showTagActionDialog(tagName: String, chip: Chip) {
        val options = arrayOf("编辑标签", "删除标签")
        
        AlertDialog.Builder(context)
            .setTitle(tagName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditTagDialog(tagName, chip)
                    1 -> showDeleteTagDialog(tagName, chip)
                }
            }
            .show()
    }

    /**
     * 显示编辑标签对话框
     */
    private fun showEditTagDialog(oldTagName: String, chip: Chip) {
        val editText = android.widget.EditText(context).apply {
            setText(oldTagName)
            selectAll()
        }
        
        AlertDialog.Builder(context)
            .setTitle("编辑标签")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newTagName = editText.text.toString().trim()
                if (newTagName.isNotEmpty() && newTagName != oldTagName) {
                    // 更新标签
                    selectedTags.remove(oldTagName)
                    selectedTags.add(newTagName)
                    chip.text = newTagName
                    
                    // 如果是自定义标签，更新自定义标签列表
                    if (!defaultTags.contains(oldTagName)) {
                        viewModel.removeCustomOption(fieldName, oldTagName)
                        viewModel.addCustomOption(fieldName, newTagName)
                    }
                    
                    // 保存到ViewModel
                    viewModel.saveFieldValue(fieldName, selectedTags.toSet())
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示删除标签确认对话框
     */
    private fun showDeleteTagDialog(tagName: String, chip: Chip) {
        AlertDialog.Builder(context)
            .setTitle("删除标签")
            .setMessage("确定要删除标签 \"$tagName\" 吗？")
            .setPositiveButton("删除") { _, _ ->
                // 从容器中移除chip
                (chip.parent as? ChipGroup)?.removeView(chip)
                selectedTags.remove(tagName)
                
                // 如果是自定义标签，从自定义标签列表中删除
                if (!defaultTags.contains(tagName)) {
                    viewModel.removeCustomOption(fieldName, tagName)
                }
                
                // 保存到ViewModel
                viewModel.saveFieldValue(fieldName, selectedTags.toSet())
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 添加标签到ChipGroup
     */
    fun addChipToGroup(container: ChipGroup, tagName: String) {
        val chip = createChip(tagName, container)
        container.addView(chip)
        selectedTags.add(tagName)
        viewModel.saveFieldValue(fieldName, selectedTags.toSet())

        // 隐藏提示文本
        findPlaceholderTextView(container)?.visibility = View.GONE
    }

    /**
     * 显示标签选择对话框
     */
    fun showTagSelectionDialog(container: ChipGroup) {
        // 使用DialogFactory创建完整的标签选择对话框
        val dialogFactory = DialogFactory(context)
        val selectedTagsSet = mutableSetOf<String>()
        val allTags = mutableListOf<String>()
        val customTags = viewModel.getCustomOptions(fieldName).toMutableList()
        
        // 添加当前已选中的标签
        for (i in 0 until container.childCount) {
            val chip = container.getChildAt(i) as? Chip
            if (chip != null) {
                selectedTagsSet.add(chip.text.toString())
            }
        }
        
        // 同步内存中的选中状态
        selectedTags.clear()
        selectedTags.addAll(selectedTagsSet)
        
        dialogFactory.showTagSelectionDialog(
            selectedTags = selectedTagsSet,
            defaultTags = defaultTags,
            customTags = customTags,
            allTags = allTags,
            selectedTagsContainer = container,
            onTagSelected = { tagName ->
                // 只添加到对话框的选中列表，UI更新在确定时统一处理
                selectedTagsSet.add(tagName)
            },
            viewModel = viewModel,
            fieldName = fieldName
        )
    }
    
    /**
     * 同步选中状态到内存
     */
    fun syncSelectedTags(container: ChipGroup) {
        selectedTags.clear()
        for (i in 0 until container.childCount) {
            val chip = container.getChildAt(i) as? Chip
            if (chip != null) {
                selectedTags.add(chip.text.toString())
            }
        }
        viewModel.saveFieldValue(fieldName, selectedTags.toSet())
    }



    /**
     * 从容器中移除标签
     */
    private fun removeTagFromContainer(tagName: String, container: ChipGroup) {
        for (i in 0 until container.childCount) {
            val chip = container.getChildAt(i) as? Chip
            if (chip?.text.toString() == tagName) {
                container.removeView(chip)
                selectedTags.remove(tagName)
                viewModel.saveFieldValue(fieldName, selectedTags.toSet())
                
                // 如果没有标签了，显示提示文本
                if (container.childCount == 0) {
                    findPlaceholderTextView(container)?.visibility = View.VISIBLE
                }
                break
            }
        }
    }

    /**
     * 获取已选标签
     */
    fun getSelectedTags(): Set<String> {
        return selectedTags.toSet()
    }

    /**
     * 设置已选标签
     */
    fun setSelectedTags(tags: Set<String>) {
        selectedTags.clear()
        selectedTags.addAll(tags)
    }

    /**
     * 清除所有标签
     */
    fun clearTags(container: ChipGroup) {
        container.removeAllViews()
        selectedTags.clear()

        // 保存空集合到 ViewModel
        viewModel.saveFieldValue(fieldName, emptySet<String>())

        // 显示提示文本
        findPlaceholderTextView(container)?.visibility = View.VISIBLE
    }

    /**
     * 恢复标签到容器
     */
    fun restoreTags(tags: Set<String>, container: ChipGroup) {
        clearTags(container)

        if (tags.isNotEmpty()) {
            // 隐藏提示文本
            findPlaceholderTextView(container)?.visibility = View.GONE

            tags.forEach { tag ->
                addTagToContainer(tag, container)
            }
        } else {
            // 显示提示文本
            findPlaceholderTextView(container)?.visibility = View.VISIBLE
        }
    }

    /**
     * 查找提示文本视图
     */
    private fun findPlaceholderTextView(container: View): TextView? {
        // 首先在当前视图层次结构中查找
        val viewToSearch = findRootViewForSearch(container)

        // 递归查找具有"点击选择标签"文本的TextView
        return findTextViewWithText(viewToSearch, "点击选择标签")
    }

    /**
     * 查找合适的根视图来搜索提示文本
     */
    private fun findRootViewForSearch(view: View): ViewGroup {
        // 获取父视图
        var current = view
        var parent = view.parent as? ViewGroup

        // 向上查找到RelativeLayout，这通常是我们放置提示文本的地方
        while (parent != null) {
            if (parent is RelativeLayout) {
                return parent
            }
            current = parent
            parent = parent.parent as? ViewGroup
        }

        // 如果找不到RelativeLayout，则返回最顶层的ViewGroup
        return (current.parent as? ViewGroup) ?: (current as? ViewGroup) ?: throw IllegalStateException("Cannot find suitable root view")
    }

    /**
     * 递归查找具有特定文本的TextView
     */
    private fun findTextViewWithText(view: View, text: String): TextView? {
        if (view is TextView && view.text == text) {
            return view
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                val result = findTextViewWithText(child, text)
                if (result != null) {
                    return result
                }
            }
        }

        return null
    }
}