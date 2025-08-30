package com.example.itemmanagement.ui.shopping

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.data.entity.ShoppingItemEntity
import com.example.itemmanagement.ui.add.AddItemViewModel
import com.example.itemmanagement.ui.add.AddItemViewModelFactory
import com.example.itemmanagement.ui.base.BaseItemFragment
import com.example.itemmanagement.ui.add.Field
import kotlinx.coroutines.launch

/**
 * 新架构的购物清单物品入库Fragment
 * 基于BaseItemFragment，用于将购物清单项目转入库存
 */
class AddFromShoppingListFragment : BaseItemFragment<AddItemViewModel>() {

    private val args: AddFromShoppingListFragmentArgs by navArgs()

    // 使用AddItemViewModel
    override val viewModel: AddItemViewModel by viewModels<AddItemViewModel> {
        val app = (requireActivity().application as ItemManagementApplication)
        val repository = app.repository
        val warrantyRepository = app.warrantyRepository
        AddItemViewModelFactory(repository, cacheViewModel, warrantyRepository)
    }

    override fun onViewModelReady() {
        // ViewModel准备就绪，从购物清单项目预填充数据
        args.shoppingItem?.let { shoppingItem ->
            prepopulateFromShoppingItem(shoppingItem)
        }
    }

    override fun setupTitleAndButtons() {
        // 设置标题
        activity?.title = "物品入库"
        
        // 设置按钮文本
        binding.saveButton.text = "入库"
        binding.editFieldsButton.text = "编辑字段"
    }

    override fun setupButtons() {
        // 保存按钮
        binding.saveButton.setOnClickListener {
            performSave()
        }
        
        // 编辑字段按钮
        binding.editFieldsButton.setOnClickListener {
            showEditFieldsDialog()
        }
    }

    override fun onSaveSuccess() {
        // 保存成功后，删除对应的购物清单项目
        args.shoppingItem?.let { shoppingItem ->
            deleteShoppingItemAfterSuccess(shoppingItem.id)
        }
        super.onSaveSuccess() // 调用父类方法，显示Toast并返回
    }

    /**
     * 从购物清单项目预填充表单数据
     */
    private fun prepopulateFromShoppingItem(shoppingItem: ShoppingItemEntity) {
        // 基础信息
        viewModel.saveFieldValue("名称", shoppingItem.name)
        viewModel.saveFieldValue("数量", shoppingItem.quantity.toString())
        // 购物清单项目没有unit字段，使用默认值"个"
        
        // 分类信息
        if (shoppingItem.category.isNotBlank()) {
            viewModel.saveFieldValue("分类", shoppingItem.category)
        }
        shoppingItem.subCategory?.let { if (it.isNotBlank()) viewModel.saveFieldValue("子分类", it) }
        
        // 商业信息 - 使用estimatedPrice作为单价
        shoppingItem.estimatedPrice?.let { if (it > 0) viewModel.saveFieldValue("单价", it.toString()) }
        shoppingItem.priceUnit?.let { if (it.isNotBlank()) viewModel.saveFieldValue("单价_unit", it) }
        shoppingItem.totalPrice?.let { if (it > 0) viewModel.saveFieldValue("总价", it.toString()) }
        
        // 其他信息
        shoppingItem.brand?.let { if (it.isNotBlank()) viewModel.saveFieldValue("品牌", it) }
        shoppingItem.specification?.let { if (it.isNotBlank()) viewModel.saveFieldValue("规格", it) }
        shoppingItem.customNote?.let { if (it.isNotBlank()) viewModel.saveFieldValue("备注", it) }
        
        // 设置需要显示的字段
        val fieldsToShow = mutableSetOf<Field>()
        
        // 基础字段
        fieldsToShow.add(Field("基础信息", "名称", true))
        fieldsToShow.add(Field("基础信息", "数量", true))
        
        // 根据数据添加相关字段
        if (shoppingItem.category.isNotBlank()) {
            fieldsToShow.add(Field("分类", "分类", true))
        }
        if (!shoppingItem.subCategory.isNullOrBlank()) {
            fieldsToShow.add(Field("分类", "子分类", true))
        }
        if (shoppingItem.estimatedPrice != null && shoppingItem.estimatedPrice!! > 0) {
            fieldsToShow.add(Field("数字类", "单价", true))
        }
        if (shoppingItem.totalPrice != null && shoppingItem.totalPrice!! > 0) {
            fieldsToShow.add(Field("数字类", "总价", true))
        }
        if (!shoppingItem.brand.isNullOrBlank()) {
            fieldsToShow.add(Field("其他信息", "品牌", true))
        }
        if (!shoppingItem.specification.isNullOrBlank()) {
            fieldsToShow.add(Field("其他信息", "规格", true))
        }
        if (!shoppingItem.customNote.isNullOrBlank()) {
            fieldsToShow.add(Field("基础信息", "备注", true))
        }
        
        // 手动更新选中字段 - 使用viewModel的updateFieldSelection方法
        fieldsToShow.forEach { field ->
            viewModel.updateFieldSelection(field, true)
        }
    }

    /**
     * 显示编辑字段对话框
     */
    private fun showEditFieldsDialog() {
        // 使用EditFieldsFragment
        val dialog = com.example.itemmanagement.ui.add.EditFieldsFragment.newInstance(viewModel, false)
        dialog.show(parentFragmentManager, "edit_fields")
    }

    /**
     * 成功入库后删除购物清单项目
     */
    private fun deleteShoppingItemAfterSuccess(shoppingItemId: Long) {
        lifecycleScope.launch {
            try {
                val repository = (requireActivity().application as ItemManagementApplication).repository
                repository.deleteShoppingItemById(shoppingItemId)
            } catch (e: Exception) {
                // 删除失败不影响主流程，只是清单项目不会自动清除

                android.util.Log.w("AddFromShoppingListFragment", "删除购物清单项目失败", e)
            }
        }
    }
}
