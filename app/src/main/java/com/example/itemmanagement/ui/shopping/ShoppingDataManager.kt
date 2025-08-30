package com.example.itemmanagement.ui.shopping

import androidx.lifecycle.SavedStateHandle
import com.example.itemmanagement.ui.base.FieldInteractionViewModel
import com.example.itemmanagement.ui.add.Field

/**
 * 购物数据管理器
 * 负责购物专用的数据存储，避免与添加库存物品的数据混合
 * 使用唯一的键值前缀来分离数据存储
 * 
 * 现在使用FieldInteractionViewModel接口，与具体实现解耦
 */
class ShoppingDataManager(
    private val viewModel: FieldInteractionViewModel
) {
    
    companion object {
        // 购物专用的存储键值前缀
        private const val SHOPPING_PREFIX = "shopping_"
        
        // 购物专用的存储键值
        private const val SHOPPING_SELECTED_FIELDS = "${SHOPPING_PREFIX}selected_fields"
        private const val SHOPPING_FIELD_VALUES = "${SHOPPING_PREFIX}field_values"
        private const val SHOPPING_SELECTED_TAGS = "${SHOPPING_PREFIX}selected_tags"
    }
    
    /**
     * 初始化购物专用字段和数据
     */
    fun initializeShoppingFields() {
        // 添加购物专用字段属性
        val shoppingProperties = ShoppingFieldManager.getShoppingFieldProperties()
        shoppingProperties.forEach { (fieldName, properties) ->
            viewModel.setFieldProperties(fieldName, properties)
        }
        
        // 设置默认字段（让viewModel管理字段选择状态）
        setupDefaultShoppingFields()
        
        // 恢复保存的字段值
        restoreShoppingFieldValues()
    }
    
    private fun setupDefaultShoppingFields() {
        val defaultFields = ShoppingFieldManager.getDefaultShoppingFields()
        val defaultValues = ShoppingFieldManager.getDefaultShoppingValues()
        
        // 设置默认选中字段
        defaultFields.forEach { fieldName ->
            val group = ShoppingFieldManager.getShoppingFieldGroup(fieldName)
            val field = ShoppingFieldManager.createShoppingField(group, fieldName, true)
            updateShoppingFieldSelection(field, true)
        }
        
        // 设置默认值
        defaultValues.forEach { (fieldName, value) ->
            saveShoppingFieldValue(fieldName, value)
        }
    }
    
    /**
     * 保存购物字段值（使用独立的存储键值）
     */
    fun saveShoppingFieldValue(fieldName: String, value: Any?) {
        // 使用带前缀的字段名来避免与库存物品数据冲突
        val shoppingFieldKey = "${SHOPPING_PREFIX}${fieldName}"
        
        // 直接使用viewModel保存，但使用特殊的键值
        viewModel.saveFieldValue(shoppingFieldKey, value)
        
        // 同时保存到正常字段名以便UI显示
        viewModel.saveFieldValue(fieldName, value)
    }
    
    /**
     * 获取购物字段值
     */
    fun getShoppingFieldValue(fieldName: String): Any? {
        val shoppingFieldKey = "${SHOPPING_PREFIX}${fieldName}"
        
        // 优先从购物专用存储获取
        return viewModel.getFieldValue(shoppingFieldKey) ?: viewModel.getFieldValue(fieldName)
    }
    
    /**
     * 更新购物字段选择状态
     */
    fun updateShoppingFieldSelection(field: Field, isSelected: Boolean) {
        // 直接使用viewModel的方法
        viewModel.updateFieldSelection(field, isSelected)
    }
    
    private fun restoreShoppingFieldValues() {
        // 购物字段值由viewModel自动恢复，这里不需要特殊处理
    }
    
    /**
     * 获取所有购物字段的值
     */
    fun getAllShoppingFieldValues(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        
        // 获取所有购物字段的值
        ShoppingFieldManager.getDefaultShoppingFields().forEach { fieldName ->
            val value = viewModel.getFieldValue(fieldName)
            if (value != null) {
                result[fieldName] = value
            }
        }
        
        return result
    }
    
    /**
     * 清除购物专用数据（如果需要的话）
     */
    fun clearShoppingData() {
        // 由于使用viewModel管理数据，清除操作由viewModel处理
        // 这里暂时保留方法以备将来使用
    }
} 