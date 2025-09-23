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

    /**
     * 心愿单物品模式的缓存
     * 用于保存用户在"添加心愿单物品"页面的所有输入
     * 与其他缓存完全独立，避免数据污染
     */
    data class WishlistItemCache(
        var fieldValues: MutableMap<String, Any?> = mutableMapOf(),
        var selectedFields: Set<Field> = setOf(),
        var photoUris: List<Uri> = emptyList(),
        var selectedTags: Map<String, Set<String>> = mapOf(),
        var customOptions: MutableMap<String, MutableList<String>> = mutableMapOf(),
        var customUnits: MutableMap<String, MutableList<String>> = mutableMapOf(),
        var customTags: MutableMap<String, MutableList<String>> = mutableMapOf(),
        // 心愿单专用字段
        var priorityLevel: String? = null,        // 优先级
        var urgencyLevel: String? = null,         // 紧急程度
        var targetPrice: Double? = null,          // 目标价格
        var priceTrackingEnabled: Boolean = true, // 价格跟踪开关
        var purchaseTiming: String? = null        // 购买时机
    )

    /**
     * 心愿单编辑模式的缓存
     * 用于保存用户在"编辑特定心愿单物品"页面的所有修改
     */
    data class WishlistEditCache(
        var fieldValues: MutableMap<String, Any?> = mutableMapOf(),
        var selectedFields: Set<Field> = setOf(),
        var photoUris: List<Uri> = emptyList(),
        var selectedTags: Map<String, Set<String>> = mapOf(),
        var customOptions: MutableMap<String, MutableList<String>> = mutableMapOf(),
        var customUnits: MutableMap<String, MutableList<String>> = mutableMapOf(),
        var customTags: MutableMap<String, MutableList<String>> = mutableMapOf(),
        var originalWishlistItemId: Long? = null, // 原始心愿单物品ID
        // 心愿单专用字段
        var priorityLevel: String? = null,
        var urgencyLevel: String? = null,
        var targetPrice: Double? = null,
        var priceTrackingEnabled: Boolean = true,
        var purchaseTiming: String? = null
    )

    // 添加物品模式的缓存
    private val _addItemCache = AddItemCache()
    
    // 编辑物品模式的缓存 - 按物品ID分组
    private val _editItemCaches = mutableMapOf<Long, EditItemCache>()

    // 购物清单物品模式的缓存 - 按购物清单ID分组
    private val _shoppingItemCaches = mutableMapOf<Long, ShoppingItemCache>()

    // 心愿单添加模式的缓存
    private val _wishlistAddCache = WishlistItemCache()
    
    // 心愿单编辑模式的缓存 - 按心愿单物品ID分组
    private val _wishlistEditCaches = mutableMapOf<Long, WishlistEditCache>()

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
     * 获取心愿单添加模式的缓存
     */
    fun getWishlistAddCache(): WishlistItemCache {
        android.util.Log.d("ItemStateCacheViewModel", "获取心愿单添加缓存，当前fieldValues: ${_wishlistAddCache.fieldValues}")
        return _wishlistAddCache
    }

    /**
     * 获取特定心愿单物品的编辑缓存
     * @param itemId 心愿单物品ID
     * @return 该心愿单物品的编辑缓存，如果不存在则创建新的
     */
    fun getWishlistEditCache(itemId: Long): WishlistEditCache {
        val cache = _wishlistEditCaches.getOrPut(itemId) { 
            WishlistEditCache(originalWishlistItemId = itemId) 
        }
        android.util.Log.d("ItemStateCacheViewModel", "获取心愿单编辑缓存(ID:$itemId)，当前fieldValues: ${cache.fieldValues}")
        return cache
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
     * 清除心愿单添加模式的缓存
     * 通常在心愿单物品成功保存后调用
     */
    fun clearWishlistAddCache() {
        _wishlistAddCache.fieldValues.clear()
        _wishlistAddCache.selectedFields = setOf()
        _wishlistAddCache.photoUris = emptyList()
        _wishlistAddCache.selectedTags = mapOf()
        _wishlistAddCache.customOptions.clear()
        _wishlistAddCache.customUnits.clear()
        _wishlistAddCache.customTags.clear()
        _wishlistAddCache.priorityLevel = null
        _wishlistAddCache.urgencyLevel = null
        _wishlistAddCache.targetPrice = null
        _wishlistAddCache.priceTrackingEnabled = true
        _wishlistAddCache.purchaseTiming = null
    }

    /**
     * 清除特定心愿单物品的编辑缓存
     * 通常在心愿单物品成功更新后调用
     * @param itemId 心愿单物品ID
     */
    fun clearWishlistEditCache(itemId: Long) {
        _wishlistEditCaches.remove(itemId)
    }

    /**
     * 清除所有缓存
     * 通常在用户退出整个添加/编辑流程时调用
     */
    fun clearAllCaches() {
        clearAddItemCache()
        _editItemCaches.clear()
        _shoppingItemCaches.clear()
        clearWishlistAddCache()
        _wishlistEditCaches.clear()
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
     * 检查心愿单添加缓存是否有数据
     */
    fun hasWishlistAddCache(): Boolean {
        val hasData = _wishlistAddCache.fieldValues.isNotEmpty() || 
                     _wishlistAddCache.selectedFields.isNotEmpty() ||
                     _wishlistAddCache.photoUris.isNotEmpty()
        android.util.Log.d("ItemStateCacheViewModel", "检查心愿单添加缓存: fieldValues=${_wishlistAddCache.fieldValues.size}, selectedFields=${_wishlistAddCache.selectedFields.size}, photoUris=${_wishlistAddCache.photoUris.size}, hasData=$hasData")
        return hasData
    }

    /**
     * 检查特定心愿单物品是否有编辑缓存
     * @param itemId 心愿单物品ID
     */
    fun hasWishlistEditCache(itemId: Long): Boolean {
        val cache = _wishlistEditCaches[itemId]
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
            // 优先检查心愿单添加缓存
            _wishlistAddCache.customUnits.containsKey(fieldName) -> _wishlistAddCache.customUnits[fieldName] ?: mutableListOf()
            // 然后检查添加物品缓存
            _addItemCache.customUnits.containsKey(fieldName) -> _addItemCache.customUnits[fieldName] ?: mutableListOf()
            // 如果需要更精确的区分，可以在BaseItemViewModel中传递缓存类型标识
            else -> mutableListOf()
        }
    }

    /**
     * 获取指定字段的自定义标签列表
     * 优先返回当前缓存中的自定义标签，没有则返回空列表
     */
    fun getCustomTags(fieldName: String): MutableList<String> {
        return when {
            // 优先检查心愿单添加缓存
            _wishlistAddCache.customTags.containsKey(fieldName) -> _wishlistAddCache.customTags[fieldName] ?: mutableListOf()
            // 然后检查添加物品缓存
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