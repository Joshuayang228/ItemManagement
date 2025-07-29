package com.example.itemmanagement.ui.shopping

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.itemmanagement.data.entity.ShoppingItemEntity
import com.example.itemmanagement.data.mapper.ShoppingItemMapper
import com.example.itemmanagement.ui.add.AddItemViewModel
import com.example.itemmanagement.ui.add.AddItemViewModelFactory

import com.example.itemmanagement.ui.add.Field
import com.example.itemmanagement.ui.shopping.ShoppingFieldManager
import com.example.itemmanagement.ui.shopping.ShoppingDataManager
import com.example.itemmanagement.ui.base.BaseItemFragment
import kotlinx.coroutines.launch

/**
 * 购物物品添加/编辑页面
 * 复用BaseItemFragment，确保与库存物品操作体验一致
 */
class AddShoppingItemFragment : BaseItemFragment() {

    // 暂时使用传递的参数，后续可以通过 Safe Args 改进
    private var currentListId: Long = 1L
    private var editingShoppingItem: ShoppingItemEntity? = null
    
    // 使用购物专用的数据管理器
    private lateinit var shoppingDataManager: ShoppingDataManager
    
    private var isEditMode: Boolean = false
    

    
    override fun initializeMode(savedInstanceState: Bundle?) {
        // 实现 BaseItemFragment 的抽象方法
        // 获取参数（从 arguments 中读取，暂时使用默认值）
        currentListId = arguments?.getLong("listId", 1L) ?: 1L
        editingShoppingItem = arguments?.getParcelable("shoppingItem")
        isEditMode = editingShoppingItem != null
    }
    
    override fun setupTitleAndButtons() {
        // 实现 BaseItemFragment 的抽象方法
        val titleText = if (isEditMode) "编辑购物物品" else "添加购物物品"
        activity?.title = titleText
        
        // 设置保存按钮文本
        val buttonText = if (isEditMode) "更新到购物清单" else "添加到购物清单"
        binding.saveButton.text = buttonText
    }
    
    override fun observeSaveResult() {
        // 实现 BaseItemFragment 的抽象方法
        // 由于我们重写了保存按钮行为，这里不需要观察saveResult
        // 保持空实现即可
    }
    
    override fun getItemId(): Long {
        // 对于购物物品，返回编辑中的购物物品ID，新建时返回0
        return if (isEditMode) editingShoppingItem?.id ?: 0L else 0L
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化购物数据管理器
        shoppingDataManager = ShoppingDataManager(viewModel)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 初始化购物专用字段
        shoppingDataManager.initializeShoppingFields()
        
        // 如果是编辑模式，预填充数据
        if (isEditMode && editingShoppingItem != null) {
            prefillShoppingItemData(editingShoppingItem!!)
        }
        
        // 重写保存按钮行为，确保保存到购物清单而不是库存
        setupShoppingSaveButton()
        
        // 重写编辑字段按钮行为，支持购物模式
        setupShoppingEditFieldsButton()
    }
    
    /**
     * 设置购物模式的编辑字段按钮
     */
    private fun setupShoppingEditFieldsButton() {
        binding.editFieldsButton.setOnClickListener {
            // 在显示编辑字段对话框前，先保存当前字段的值
            if (fieldViews.isNotEmpty()) {
                fieldValueManager.saveFieldValues(fieldViews)
            }
            // 显示购物模式的编辑字段对话框
            com.example.itemmanagement.ui.add.EditFieldsFragment.newInstance(isShoppingMode = true).show(
                childFragmentManager,
                "EditFieldsFragment"
            )
        }
    }
    
    /**
     * 预填充购物物品数据
     */
    private fun prefillShoppingItemData(shoppingItem: ShoppingItemEntity) {
        lifecycleScope.launch {
            try {
                // 使用转换器将购物物品数据转换为字段映射
                val fieldMap = ShoppingItemMapper.shoppingItemToFieldMap(shoppingItem)
                
                // 使用ViewModel的方法逐个设置字段值
                fieldMap.forEach { (fieldName, value) ->
                    value?.let {
                        viewModel.saveFieldValue(fieldName, it)
                    }
                }
                
                Log.d("AddShoppingItem", "成功预填充购物物品数据: ${shoppingItem.name}")
                
            } catch (e: Exception) {
                Log.e("AddShoppingItem", "预填充数据失败", e)
            }
        }
    }
    

    
    /**
     * 设置保存按钮的购物清单保存行为
     */
    private fun setupShoppingSaveButton() {
        // 重写保存按钮的点击事件，直接保存到购物清单
        binding.saveButton.setOnClickListener {
            saveShoppingItem()
        }
    }
    
    /**
     * 保存购物物品（绕过BaseItemFragment的保存机制）
     */
    private fun saveShoppingItem() {
        // 保存字段值
        if (fieldViews.isNotEmpty()) {
            fieldValueManager.saveFieldValues(fieldViews)
        }
        
        lifecycleScope.launch {
            try {
                // 获取当前表单的所有字段值
                val fieldValues = viewModel.getAllFieldValues()
                
                // 转换为购物物品实体
                val shoppingItem = ShoppingItemMapper.fieldMapToShoppingItem(
                    fieldMap = fieldValues,
                    listId = currentListId,
                    originalId = if (isEditMode) editingShoppingItem?.id ?: 0 else 0
                )
                
                // 保存到数据库
                if (isEditMode) {
                    updateShoppingItem(shoppingItem)
                } else {
                    insertShoppingItem(shoppingItem)
                }
                
            } catch (e: Exception) {
                Log.e("AddShoppingItem", "保存购物物品失败", e)
                handleSaveError("保存失败: ${e.message}")
            }
        }
    }
    

    
    /**
     * 插入新的购物物品
     */
        private suspend fun insertShoppingItem(shoppingItem: ShoppingItemEntity) {
        try {
            // 这里需要调用Repository的方法保存购物物品
            val repository = viewModel.getRepository()
            repository.insertShoppingItemSimple(shoppingItem)
            
            Log.d("AddShoppingItem", "成功添加购物物品: ${shoppingItem.name}")
            
            // 显示成功消息
            android.widget.Toast.makeText(
                requireContext(),
                "已添加「${shoppingItem.name}」到购物清单",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            
            // 返回上一页
            findNavController().navigateUp()
            
        } catch (e: Exception) {
            Log.e("AddShoppingItem", "插入购物物品失败", e)
            handleSaveError("添加失败: ${e.message}")
        }
    }
    
    /**
     * 更新购物物品
     */
    private suspend fun updateShoppingItem(shoppingItem: ShoppingItemEntity) {
        try {
            val repository = viewModel.getRepository()
            repository.updateShoppingItem(shoppingItem)
            
            Log.d("AddShoppingItem", "成功更新购物物品: ${shoppingItem.name}")
            
            // 显示成功消息
            android.widget.Toast.makeText(
                requireContext(),
                "已更新「${shoppingItem.name}」",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            
            // 返回上一页
            findNavController().navigateUp()
            
        } catch (e: Exception) {
            Log.e("AddShoppingItem", "更新购物物品失败", e)
            handleSaveError("更新失败: ${e.message}")
        }
    }
    
    /**
     * 处理保存错误
     */
    private fun handleSaveError(message: String) {
        // 显示错误消息
        Log.e("AddShoppingItem", message)
        android.widget.Toast.makeText(
            requireContext(),
            message,
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
    
    /**
     * 获取当前清单ID（供BaseItemFragment使用）
     */
    fun getCurrentListId(): Long = currentListId
    
    /**
     * 检查是否为编辑模式（供BaseItemFragment使用）
     */
    fun isInEditMode(): Boolean = isEditMode
} 