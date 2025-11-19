package com.example.itemmanagement.ui.base

import android.net.Uri
import androidx.lifecycle.LiveData
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.ui.common.FieldProperties
import com.example.itemmanagement.ui.add.Field

/**
 * UI字段交互接口
 * 
 * 这个接口定义了UI层（FieldViewFactory、FieldValueManager）与ViewModel交互所需的所有方法。
 * 它将UI层从具体的ViewModel实现中解耦，使新架构更加清晰和可维护。
 * 
 * 核心功能包括：
 * 1. 字段数据的读写操作
 * 2. 字段属性和配置管理
 * 3. 动态数据源提供（子分类、位置等）
 * 4. 用户自定义内容管理（选项、单位、标签）
 * 5. UI状态的响应式更新
 * 
 * 实现类：BaseItemViewModel
 * 使用者：FieldViewFactory, FieldValueManager
 */
interface FieldInteractionViewModel {

    // === 核心数据读写 ===
    
    /**
     * 保存字段值
     * @param fieldName 字段名称
     * @param value 字段值
     */
    fun saveFieldValue(fieldName: String, value: Any?)
    
    /**
     * 获取字段值
     * @param fieldName 字段名称
     * @return 字段值，可能为null
     */
    fun getFieldValue(fieldName: String): Any?
    
    /**
     * 获取所有字段值
     * @return 字段名到值的映射
     */
    fun getAllFieldValues(): Map<String, Any?>

    /**
     * 清除单个字段的值
     * @param fieldName 字段名称
     */
    fun clearFieldValue(fieldName: String)

    /**
     * 清除所有字段值
     */
    fun clearAllFieldValues()
    
    /**
     * 只清除字段值和照片，但保留已选中的字段
     * 用于清除按钮功能
     */
    fun clearFieldValuesOnly()

    // === 字段属性与配置 ===
    
    /**
     * 获取字段属性
     * @param fieldName 字段名称
     * @return 字段属性配置
     */
    fun getFieldProperties(fieldName: String): FieldProperties
    
    /**
     * 设置字段属性
     * @param fieldName 字段名称
     * @param properties 字段属性配置
     */
    fun setFieldProperties(fieldName: String, properties: FieldProperties)
    
    /**
     * 获取所有字段属性
     * @return 字段名到属性的映射
     */
    fun getAllFieldProperties(): Map<String, FieldProperties>

    // === 动态数据源 ===
    
    /**
     * 获取指定分类的子分类列表
     * @param category 分类名称
     * @return 子分类列表
     */
    fun getSubCategoriesForCategory(category: String): List<String>
    
    /**
     * 获取原始的（未经处理的）子分类列表
     * @param category 分类名称
     * @return 原始子分类列表
     */
    fun getOriginalSubCategoriesForCategory(category: String): List<String>
    
    /**
     * 检查分类是否已选择
     * @return true表示已选择分类
     */
    fun isCategorySelected(): Boolean
    
    /**
     * 更新子分类选项
     * @param category 分类名称
     */
    fun updateSubCategoryOptions(category: String)
    
    /**
     * 获取数据仓库实例
     * 用于位置选择器等组件异步获取数据
     * @return UnifiedItemRepository实例
     */
    fun getItemRepository(): UnifiedItemRepository

    /**
     * 保存自定义位置数据
     * @param customData 自定义位置数据
     */
    fun saveCustomLocations(customData: com.example.itemmanagement.data.model.CustomLocationData)

    /**
     * 获取自定义位置数据
     * @return 自定义位置数据，可能为null
     */
    fun getCustomLocations(): com.example.itemmanagement.data.model.CustomLocationData?

    // === 用户自定义内容管理 ===
    
    /**
     * 获取字段的自定义选项
     * @param fieldName 字段名称
     * @return 自定义选项列表
     */
    fun getCustomOptions(fieldName: String, contextKey: String? = null): MutableList<String>
    
    /**
     * 设置字段的自定义选项
     * @param fieldName 字段名称
     * @param options 选项列表
     */
    fun setCustomOptions(fieldName: String, options: MutableList<String>, contextKey: String? = null)
    
    /**
     * 添加字段的自定义选项
     * @param fieldName 字段名称
     * @param option 选项内容
     */
    fun addCustomOption(fieldName: String, option: String, contextKey: String? = null)
    
    /**
     * 删除字段的自定义选项
     * @param fieldName 字段名称
     * @param option 要删除的选项内容
     */
    fun removeCustomOption(fieldName: String, option: String, contextKey: String? = null)
    
    /**
     * 获取字段的自定义单位
     * @param fieldName 字段名称
     * @return 自定义单位列表
     */
    fun getCustomUnits(fieldName: String): MutableList<String>
    
    /**
     * 设置字段的自定义单位
     * @param fieldName 字段名称
     * @param units 单位列表
     */
    fun setCustomUnits(fieldName: String, units: MutableList<String>)
    
    /**
     * 添加字段的自定义单位
     * @param fieldName 字段名称
     * @param unit 单位内容
     */
    fun addCustomUnit(fieldName: String, unit: String)
    
    /**
     * 删除字段的自定义单位
     * @param fieldName 字段名称
     * @param unit 要删除的单位内容
     */
    fun removeCustomUnit(fieldName: String, unit: String)
    
    /**
     * 获取字段的自定义标签
     * @param fieldName 字段名称
     * @return 自定义标签列表
     */
    fun getCustomTags(fieldName: String): MutableList<String>
    
    /**
     * 添加字段的自定义标签
     * @param fieldName 字段名称
     * @param tag 标签内容
     */
    fun addCustomTag(fieldName: String, tag: String)
    
    /**
     * 删除字段的自定义标签
     * @param fieldName 字段名称
     * @param tag 要删除的标签内容
     */
    fun removeCustomTag(fieldName: String, tag: String)

    /**
     * 解析模板中存储的原始单值字段，转换为当前可用的显示值
     */
    fun resolveTemplateSingleValue(fieldName: String, rawValue: String?, contextKey: String? = null): String?

    /**
     * 解析模板中存储的多值字段（如标签），转换为当前可用的显示值列表
     */
    fun resolveTemplateMultiValues(fieldName: String, rawValues: Collection<String>, contextKey: String? = null): List<String>

    // === UI状态的响应式更新 ===
    
    /**
     * 选中的标签LiveData
     * UI通过观察这个LiveData来动态显示标签Chip
     */
    val selectedTags: LiveData<Map<String, Set<String>>>
    
    /**
     * 更新选中的标签
     * @param fieldName 字段名称
     * @param tags 选中的标签集合
     */
    fun updateSelectedTags(fieldName: String, tags: Set<String>)
    
    // === 照片管理 ===
    
    /**
     * 添加照片URI
     * @param uri 照片URI
     */
    fun addPhotoUri(uri: Uri)
    
    /**
     * 移除照片URI
     * @param position 照片位置
     */
    fun removePhotoUri(position: Int)
    
    // === 字段选择管理 ===
    
    /**
     * 更新字段选择状态
     * @param field 字段对象
     * @param isSelected 是否选中
     */
    fun updateFieldSelection(field: Field, isSelected: Boolean)
}
