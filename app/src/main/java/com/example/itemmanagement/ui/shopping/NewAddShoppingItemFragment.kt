package com.example.itemmanagement.ui.shopping

import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import com.example.itemmanagement.ItemManagementApplication

import com.example.itemmanagement.ui.add.Field
import com.example.itemmanagement.ui.add.NewEditFieldsFragment
import com.example.itemmanagement.ui.base.NewBaseItemFragment

/**
 * 新的添加购物清单物品 Fragment
 * 
 * 使用新的 ViewModel 架构，具有以下特性：
 * 1. 专门为购物清单物品设计的字段和逻辑
 * 2. 与普通物品添加完全隔离的数据空间
 * 3. 支持从库存物品预填充数据
 */
class NewAddShoppingItemFragment : NewBaseItemFragment<AddShoppingItemViewModel>() {

    // 获取导航参数
    private val args: NewAddShoppingItemFragmentArgs by navArgs()

    // 获取购物清单物品专用的 ViewModel
    override val viewModel: AddShoppingItemViewModel by viewModels<AddShoppingItemViewModel> {
        val repository = (requireActivity().application as ItemManagementApplication).repository
        AddShoppingItemViewModelFactory(repository, cacheViewModel, args.listId)
    }

        override fun onViewModelReady() {
        // ViewModel 已准备就绪
        // 初始化默认字段
        initializeDefaultFields()

        // 如果有来源物品ID，预填充数据
        if (args.sourceItemId > 0) {
            viewModel.prepareFromInventoryItem(args.sourceItemId)
        }
    }

    /**
     * 初始化默认字段
     */
    private fun initializeDefaultFields() {
        // 创建购物清单专用的默认字段集合
        val defaultFields = setOf(
            Field("基础信息", "名称", true, 1),
            Field("基础信息", "数量", true, 2),
            Field("基础信息", "备注", false, 3),
            Field("分类", "分类", true, 4),
            Field("分类", "子分类", false, 5),
            Field("价格", "预估价格", false, 6),
            Field("价格", "实际价格", false, 7),
            Field("优先级", "优先级", false, 8),
            Field("优先级", "紧急程度", false, 9),
            Field("购买信息", "购买渠道", false, 10),
            Field("购买信息", "商店名称", false, 11)
        )
        
        // 设置默认字段
        defaultFields.forEach { field ->
            viewModel.updateFieldSelection(field, field.isSelected)
        }
    }

    override fun setupTitleAndButtons() {
        // 设置标题
        activity?.title = "添加购物物品"
        
        // 设置按钮文本
        binding.saveButton.text = "添加到清单"
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
     * 显示清空确认对话框
     */
    private fun showClearConfirmDialog() {
        dialogFactory.createConfirmDialog(
            title = "确认清空",
            message = "确定要清空所有已输入的内容吗？此操作不可撤销。",
            positiveButtonText = "确定",
            negativeButtonText = "取消",
            onPositiveClick = {
                clearAllFields()
            }
        )
    }

    /**
     * 显示退出确认对话框
     */
    private fun showExitConfirmDialog() {
        // 检查是否有未保存的内容
        if (hasUnsavedContent()) {
            dialogFactory.createConfirmDialog(
                title = "确认退出",
                message = "您有未保存的内容，确定要退出吗？内容将会自动保存为草稿。",
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
     * 显示字段选择对话框
     */
    private fun showEditFieldsDialog() {
        // 在显示编辑字段对话框前，先保存当前字段的值
        if (fieldViews.isNotEmpty()) {
            fieldValueManager.saveFieldValues(fieldViews)
        }
        
        // 使用新架构的EditFieldsFragment（购物模式）
        val editFieldsFragment = NewEditFieldsFragment.newInstance(viewModel, true)
        
        // 显示EditFieldsFragment
        editFieldsFragment.show(childFragmentManager, "EditFieldsFragment")
    }
    


    /**
     * 获取购物清单专用的可用字段列表
     */
    private fun getAvailableShoppingFields(): List<Field> {
        return listOf(
            // 基础信息
            Field("基础信息", "名称", true),
            Field("基础信息", "数量", false),
            Field("基础信息", "备注", false),
            
            // 分类信息
            Field("分类", "分类", false),
            Field("分类", "子分类", false),
            Field("分类", "标签", false),
            Field("分类", "季节", false),
            
            // 价格信息（购物特有）
            Field("价格", "预估价格", false),
            Field("价格", "实际价格", false),
            Field("价格", "预算上限", false),
            Field("价格", "价格单位", false),
            
            // 购买信息
            Field("购买信息", "购买渠道", false),
            Field("购买信息", "商店名称", false),
            Field("购买信息", "首选商店", false),
            
            // 优先级信息（购物特有）
            Field("优先级", "优先级", false),
            Field("优先级", "紧急程度", false),
            
            // 时间信息
            Field("时间", "截止日期", false),
            Field("时间", "提醒日期", false),
            
            // 商品属性
            Field("商品属性", "品牌", false),
            Field("商品属性", "规格", false),
            Field("商品属性", "容量", false),
            Field("商品属性", "容量单位", false),
            Field("商品属性", "评分", false),
            
            // 其他信息（购物特有）
            Field("其他", "推荐原因", false),
            Field("其他", "周期性购买", false),
            Field("其他", "周期间隔", false)
        )
    }

    /**
     * 清空所有字段
     */
    private fun clearAllFields() {
        // 清空 ViewModel 中的所有数据
        viewModel.clearStateAndCache()
        
        // 刷新UI
        binding.fieldsContainer.removeAllViews()
        fieldViews.clear()
        
        // 清空照片
        photoAdapter.clearPhotos()
    }

    /**
     * 检查是否有未保存的内容
     */
    private fun hasUnsavedContent(): Boolean {
        val fieldValues = viewModel.getAllFieldValues()
        val hasFieldValues = fieldValues.isNotEmpty() && fieldValues.values.any { 
            it != null && it.toString().isNotBlank() 
        }
        val hasPhotos = viewModel.getPhotoUris().isNotEmpty()
        
        return hasFieldValues || hasPhotos
    }

    override fun onSaveSuccess() {
        // 购物清单物品添加成功
        dialogFactory.createConfirmDialog(
            title = "添加成功",
            message = "物品已成功添加到购物清单。是否继续添加其他物品？",
            positiveButtonText = "继续添加",
            negativeButtonText = "返回清单",
            onPositiveClick = {
                // 清空表单，继续添加
                clearAllFields()
            },
            onNegativeClick = {
                // 返回购物清单
                activity?.onBackPressed()
            }
        )
    }

    override fun onSaveFailure() {
        super.onSaveFailure()
        // 购物清单物品保存失败的额外处理（如果需要）
    }
} 