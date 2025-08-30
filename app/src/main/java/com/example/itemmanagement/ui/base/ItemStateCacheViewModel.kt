package com.example.itemmanagement.ui.base

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.itemmanagement.ui.add.Field

/**
 * 物品状态缓存 ViewModel
 * 
 * 这个 ViewModel 的生命周期与导航图绑定，用作内存中的"草稿箱"。
 * 它负责在用户切换页面时临时保存未保存的表单数据，解决数据混淆问题。
 * 
 * 关键设计原则：
 * 1. 缓存隔离：不同模式（添加、编辑不同物品）的数据完全分离
 * 2. 临时存储：只在导航流程中保持数据，保存成功后自动清除
 * 3. 智能恢复：用户返回时能够恢复之前的输入状态
 */
class ItemStateCacheViewModel : ViewModel() {

    /**
     * 添加物品模式的缓存
     * 用于保存用户在"添加新物品"页面的所有输入
     */
    data class AddItemCache(
        var fieldValues: MutableMap<String, Any?> = mutableMapOf(),
        var selectedFields: Set<Field> = setOf(),
        var photoUris: List<Uri> = emptyList(),
        var selectedTags: Map<String, Set<String>> = mapOf(),
        var customOptions: MutableMap<String, MutableList<String>> = mutableMapOf(),
        var customUnits: MutableMap<String, MutableList<String>> = mutableMapOf(),
        var customTags: MutableMap<String, MutableList<String>> = mutableMapOf()
    )

    /**
     * 编辑物品模式的缓存
     * 用于保存用户在"编辑特定物品"页面的所有修改
     */
    data class EditItemCache(
        var fieldValues: MutableMap<String, Any?> = mutableMapOf(),
        var selectedFields: Set<Field> = setOf(),
        var photoUris: List<Uri> = emptyList(),
        var selectedTags: Map<String, Set<String>> = mapOf(),
        var customOptions: MutableMap<String, MutableList<String>> = mutableMapOf(),
        var customUnits: MutableMap<String, MutableList<String>> = mutableMapOf(),
        var customTags: MutableMap<String, MutableList<String>> = mutableMapOf(),
        var originalItemId: Long? = null
    )

    /**
     * 购物清单物品模式的缓存
     * 用于保存用户在"添加购物清单物品"页面的所有输入
     * 与添加物品缓存完全独立，避免数据污染
     */
    data class ShoppingItemCache(
        var fieldValues: MutableMap<String, Any?> = mutableMapOf(),
        var selectedFields: Set<Field> = setOf(),
        var photoUris: List<Uri> = emptyList(),
        var selectedTags: Map<String, Set<String>> = mapOf(),
        var customOptions: MutableMap<String, MutableList<String>> = mutableMapOf(),
        var customUnits: MutableMap<String, MutableList<String>> = mutableMapOf(),
        var customTags: MutableMap<String, MutableList<String>> = mutableMapOf(),
        var shoppingListId: Long? = null,  // 购物清单专用字段
        var priority: String? = null,      // 优先级
        var urgencyLevel: String? = null   // 紧急程度
    )

    // 添加物品模式的缓存
    private val _addItemCache = AddItemCache()
    
    // 编辑物品模式的缓存 - 按物品ID分组
    private val _editItemCaches = mutableMapOf<Long, EditItemCache>()

    // 购物清单物品模式的缓存 - 按购物清单ID分组
    private val _shoppingItemCaches = mutableMapOf<Long, ShoppingItemCache>()

    /**
     * 获取添加物品模式的缓存
     */
    fun getAddItemCache(): AddItemCache {
        android.util.Log.d("ItemStateCacheViewModel", "获取添加物品缓存，当前fieldValues: ${_addItemCache.fieldValues}")
        return _addItemCache
    }

    /**
     * 获取特定物品的编辑缓存
     * @param itemId 物品ID
     * @return 该物品的编辑缓存，如果不存在则创建新的
     */
    fun getEditItemCache(itemId: Long): EditItemCache {
        val cache = _editItemCaches.getOrPut(itemId) { 
            EditItemCache(originalItemId = itemId) 
        }
        android.util.Log.d("ItemStateCacheViewModel", "获取编辑物品缓存(ID:$itemId)，当前fieldValues: ${cache.fieldValues}")
        return cache
    }

    /**
     * 获取特定购物清单的购物清单物品缓存
     * @param shoppingListId 购物清单ID
     * @return 该购物清单的购物清单物品缓存，如果不存在则创建新的
     */
    fun getShoppingItemCache(shoppingListId: Long): ShoppingItemCache {
        return _shoppingItemCaches.getOrPut(shoppingListId) {
            ShoppingItemCache(shoppingListId = shoppingListId)
        }
    }

    /**
     * 清除添加物品模式的缓存
     * 通常在物品成功保存后调用
     */
    fun clearAddItemCache() {
        _addItemCache.fieldValues.clear()
        _addItemCache.selectedFields = setOf()
        _addItemCache.photoUris = emptyList()
        _addItemCache.selectedTags = mapOf()
        _addItemCache.customOptions.clear()
        _addItemCache.customUnits.clear()
        _addItemCache.customTags.clear()
    }

    /**
     * 清除特定物品的编辑缓存
     * 通常在物品成功更新后调用
     * @param itemId 物品ID
     */
    fun clearEditItemCache(itemId: Long) {
        _editItemCaches.remove(itemId)
    }

    /**
     * 清除特定购物清单的购物清单物品缓存
     * @param shoppingListId 购物清单ID
     */
    fun clearShoppingItemCache(shoppingListId: Long) {
        _shoppingItemCaches.remove(shoppingListId)
    }

    /**
     * 清除所有缓存
     * 通常在用户退出整个添加/编辑流程时调用
     */
    fun clearAllCaches() {
        clearAddItemCache()
        _editItemCaches.clear()
        _shoppingItemCaches.clear()
    }

    /**
     * 检查添加物品缓存是否有数据
     */
    fun hasAddItemCache(): Boolean {
        val hasData = _addItemCache.fieldValues.isNotEmpty() || 
                     _addItemCache.selectedFields.isNotEmpty() ||
                     _addItemCache.photoUris.isNotEmpty()
        android.util.Log.d("ItemStateCacheViewModel", "检查添加缓存: fieldValues=${_addItemCache.fieldValues.size}, selectedFields=${_addItemCache.selectedFields.size}, photoUris=${_addItemCache.photoUris.size}, hasData=$hasData")
        return hasData
    }

    /**
     * 检查特定物品是否有编辑缓存
     * @param itemId 物品ID
     */
    fun hasEditItemCache(itemId: Long): Boolean {
        val cache = _editItemCaches[itemId]
        return cache != null && (
            cache.fieldValues.isNotEmpty() || 
            cache.selectedFields.isNotEmpty() ||
            cache.photoUris.isNotEmpty()
        )
    }

    /**
     * 检查特定购物清单是否有购物清单物品缓存
     * @param shoppingListId 购物清单ID
     */
    fun hasShoppingItemCache(shoppingListId: Long): Boolean {
        val cache = _shoppingItemCaches[shoppingListId]
        return cache != null && (
            cache.fieldValues.isNotEmpty() || 
            cache.selectedFields.isNotEmpty() ||
            cache.photoUris.isNotEmpty()
        )
    }

    /**
     * 获取指定字段的自定义单位列表
     * 优先返回当前缓存中的自定义单位，没有则返回空列表
     */
    fun getCustomUnits(fieldName: String): MutableList<String> {
        // 根据当前使用的缓存类型获取对应的自定义单位
        return when {
            // 这里简化处理，返回添加物品缓存中的自定义单位
            // 如果需要更精确的区分，可以在BaseItemViewModel中传递缓存类型标识
            _addItemCache.customUnits.containsKey(fieldName) -> _addItemCache.customUnits[fieldName] ?: mutableListOf()
            else -> mutableListOf()
        }
    }

    /**
     * 获取指定字段的自定义标签列表
     * 优先返回当前缓存中的自定义标签，没有则返回空列表
     */
    fun getCustomTags(fieldName: String): MutableList<String> {
        return when {
            _addItemCache.customTags.containsKey(fieldName) -> _addItemCache.customTags[fieldName] ?: mutableListOf()
            else -> mutableListOf()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // ViewModel 被销毁时清理所有缓存
        clearAllCaches()
    }
} 