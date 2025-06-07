package com.example.itemmanagement.ui.add

import android.content.Context
import android.util.Log
import android.util.TypedValue
import android.view.ViewGroup
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * 负责管理标签相关的逻辑
 */
class TagManager(
    private val context: Context,
    private val dialogFactory: DialogFactory,
    private val viewModel: AddItemViewModel,
    private val fieldName: String = "标签" // 默认字段名
) {

    // 标签集合
    private val selectedTags = mutableSetOf<String>()
    private val defaultTags = mutableListOf<String>()
    private val customTags: MutableList<String>
        get() = viewModel.getCustomTags(fieldName)

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
     * 获取所有标签
     */
    private fun getAllTags(): List<String> {
        return defaultTags + customTags
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

            chipStartPadding = 8f
            chipEndPadding = 8f
            closeIconEndPadding = 2f
            closeIconStartPadding = 2f

            // 点击关闭图标移除标签
            setOnCloseIconClickListener {
                container.removeView(this)
                selectedTags.remove(tagName)
            }

            // 长按编辑或删除标签
            setOnLongClickListener {
                dialogFactory.showTagActionDialog(
                    tagName,
                    this,
                    selectedTags,
                    defaultTags,
                    customTags,
                    getAllTags().toMutableList()
                )
                true
            }
        }
    }

    /**
     * 显示标签选择对话框
     */
    fun showTagSelectionDialog(container: ChipGroup) {
        dialogFactory.showTagSelectionDialog(
            selectedTags,
            defaultTags,
            customTags,
            getAllTags().toMutableList(),
            container
        ) { selectedTag ->
            addTagToContainer(selectedTag, container)
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
    }

    /**
     * 恢复标签到容器
     */
    fun restoreTags(tags: Set<String>, container: ChipGroup) {
        clearTags(container)
        tags.forEach { tag ->
            addTagToContainer(tag, container)
        }
    }
}