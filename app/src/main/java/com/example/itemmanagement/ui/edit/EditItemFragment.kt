package com.example.itemmanagement.ui.edit

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R

import com.example.itemmanagement.ui.add.Field
import com.example.itemmanagement.ui.add.EditFieldsFragment
import com.example.itemmanagement.ui.base.BaseItemFragment

/**
 * 新的编辑物品 Fragment
 * 
 * 使用新的 ViewModel 架构，具有以下特性：
 * 1. 每个物品ID有独立的 ViewModel 实例和缓存空间
 * 2. 智能的状态缓存和恢复
 * 3. 与添加模式和其他编辑实例完全隔离的数据
 */
class EditItemFragment : BaseItemFragment<EditItemViewModel>() {

    // 获取导航参数
    private val args: EditItemFragmentArgs by navArgs()

    // 获取编辑物品专用的 ViewModel
    override val viewModel: EditItemViewModel by viewModels {
        val app = requireActivity().application as ItemManagementApplication
        val repository = app.repository
        val warrantyRepository = app.warrantyRepository  // ✅ 获取warrantyRepository
        EditItemViewModelFactory(repository, cacheViewModel, args.itemId, warrantyRepository)  // ✅ 传入warrantyRepository
    }

    override fun onViewModelReady() {
        // ViewModel 已准备就绪
        // EditItemViewModel 会自动加载物品数据或从缓存恢复
        // 不再调用 initializeDefaultFields()，让 EditItemViewModel 根据实际数据动态设置字段
    }

    /**
     * 初始化默认字段
     */
    private fun initializeDefaultFields() {
        // 创建编辑模式的默认字段集合
        val defaultFields = setOf(
            Field("基础信息", "名称", true, 1),
            Field("基础信息", "数量", true, 2),
            Field("基础信息", "位置", false, 3),
            Field("基础信息", "开封状态", false, 4),
            Field("基础信息", "备注", false, 5),
            Field("分类", "分类", true, 6),
            Field("分类", "子分类", false, 7),
            Field("日期类", "添加日期", true, 8),
            Field("日期类", "开封时间", false, 9)
        )
        
        // 设置默认字段
        defaultFields.forEach { field ->
            viewModel.updateFieldSelection(field, field.isSelected)
        }
    }

    override fun setupTitleAndButtons() {
        // 设置标题
        activity?.title = "编辑物品"
        
        // 设置按钮文本
        binding.saveButton.text = "保存修改"
        binding.editFieldsButton.text = "编辑字段"
    }

    override fun setupButtons() {
        // 保存按钮
        binding.saveButton.setOnClickListener {
            performSave()
        }
        
        // 编辑字段按钮（使用原有的EditFieldsFragment）
        binding.editFieldsButton.setOnClickListener {
            showEditFieldsDialog()
        }
    }

    /**
     * 显示重置确认对话框
     */
    private fun showResetConfirmDialog() {
        dialogFactory.createConfirmDialog(
            title = "确认重置",
            message = "确定要将所有字段重置为物品的原始值吗？您的修改将会丢失。",
            positiveButtonText = "确定",
            negativeButtonText = "取消",
            onPositiveClick = {
                resetToOriginalValues()
            }
        )
    }

    /**
     * 显示退出确认对话框
     */
    private fun showExitConfirmDialog() {
        // 检查是否有未保存的修改
        if (hasUnsavedChanges()) {
            dialogFactory.createConfirmDialog(
                title = "确认退出",
                message = "您有未保存的修改，确定要退出吗？修改将会自动保存为草稿。",
                positiveButtonText = "退出",
                negativeButtonText = "继续编辑",
                onPositiveClick = {
                    // 数据会自动保存到缓存，直接退出
                    activity?.onBackPressed()
                }
            )
        } else {
            activity?.onBackPressed()
        }
    }

    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmDialog() {
        dialogFactory.createConfirmDialog(
            title = "确认删除",
            message = "确定要删除这个物品吗？此操作不可撤销。",
            positiveButtonText = "删除",
            negativeButtonText = "取消",
            onPositiveClick = {
                deleteItem()
            }
        )
    }

    /**
     * 显示字段选择对话框
     */
    private fun showEditFieldsDialog() {
        // 在显示编辑字段对话框前，先保存当前字段的值
        if (fieldViews.isNotEmpty()) {
            fieldValueManager.saveFieldValues(fieldViews)
        }
        
        // 使用新架构的EditFieldsFragment（编辑模式）
        val editFieldsFragment = EditFieldsFragment.newInstance(viewModel, false)
        
        // 显示EditFieldsFragment
        editFieldsFragment.show(childFragmentManager, "EditFieldsFragment")
    }


    /**
     * 获取可用的字段列表（编辑模式可能与添加模式略有不同）
     */
    private fun getAvailableFields(): List<Field> {
        return listOf(
            Field("基础信息", "名称", true),
            Field("基础信息", "数量", false),
            Field("基础信息", "位置", false),
            Field("基础信息", "开封状态", false),
            Field("基础信息", "备注", false),
            Field("分类", "分类", false),
            Field("分类", "子分类", false),
            Field("分类", "标签", false),
            Field("分类", "季节", false),
            Field("数字类", "容量", false),
            Field("数字类", "评分", false),
            Field("数字类", "单价", false),
            Field("数字类", "总价", false),
            Field("日期类", "添加日期", false),
            Field("日期类", "开封时间", false),
            Field("日期类", "购买日期", false),
            Field("日期类", "生产日期", false),
            Field("日期类", "保质期", false),
            Field("日期类", "保质过期时间", false),
            Field("日期类", "保修期", false),
            Field("日期类", "保修到期时间", false),
            Field("商业类", "品牌", false),
            Field("商业类", "购买渠道", false),
            Field("商业类", "商家名称", false),
            Field("商业类", "序列号", false),
            Field("其他", "加入心愿单", false),
            Field("其他", "高周转", false)
        )
    }

    /**
     * 重置为原始值
     */
    private fun resetToOriginalValues() {
        // 清除缓存并重新加载原始数据
        viewModel.clearStateAndCache()
        
        // 清空当前UI
        binding.fieldsContainer.removeAllViews()
        fieldViews.clear()
        
        // 清空照片
        photoAdapter.clearPhotos()
        
        // ViewModel 会自动重新加载数据
    }

    /**
     * 删除物品
     */
    private fun deleteItem() {
        // TODO: 在 ViewModel 中实现删除功能
        /*
        viewModel.deleteItem { success ->
            if (success) {
                SnackbarHelper.showSuccess(requireView(), "物品已删除")
                activity?.onBackPressed()
            } else {
                SnackbarHelper.showError(requireView(), "删除失败")
            }
        }
        */
    }

    /**
     * 检查是否有未保存的修改
     */
    private fun hasUnsavedChanges(): Boolean {
        // 检查缓存中是否有这个物品的编辑数据
        return cacheViewModel.hasEditItemCache(args.itemId)
    }

    override fun onSaveSuccess() {
        // 不调用 super.onSaveSuccess()，因为我们要自定义行为
        
        // 清除该物品的编辑缓存
        cacheViewModel.clearEditItemCache(args.itemId)
        
        // 显示成功消息
        dialogFactory.createConfirmDialog(
            title = "保存成功",
            message = "物品信息已成功更新。",
            positiveButtonText = "确定",
            onPositiveClick = {
                // 返回上一页
                activity?.onBackPressed()
            }
        )
    }

    override fun onSaveFailure() {
        super.onSaveFailure()
        // 编辑保存失败的额外处理（如果需要）
    }
} 