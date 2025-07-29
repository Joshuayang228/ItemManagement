package com.example.itemmanagement.ui.shopping

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.entity.ShoppingItemEntity
import kotlinx.coroutines.launch

class ShoppingListViewModel(private val repository: ItemRepository) : ViewModel() {

    // 所有购物清单项目
    val shoppingItems: LiveData<List<ShoppingItemEntity>> = 
        repository.getAllShoppingItems().asLiveData()

    // 购物清单 (用于兼容性)
    val shoppingLists: LiveData<List<ShoppingItemEntity>> = shoppingItems

    // 当前默认使用第一个购物清单（后续可改为动态选择）
    private var currentListId: Long = 1L
    
    // 待购买项目数量
    val pendingItemsCount: LiveData<Int> = 
        repository.getPendingShoppingItemsCountByListId(currentListId).asLiveData()

    // 推荐商品 (暂时为空列表，后续可扩展)
    private val _recommendations = MutableLiveData<List<String>>(emptyList())
    val recommendations: LiveData<List<String>> = _recommendations

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    /**
     * 添加新的购物项目到当前清单
     */
    fun addShoppingItem(
        name: String,
        quantity: Double,
        category: String,
        notes: String? = null,
        brand: String? = null
    ) {
        viewModelScope.launch {
            try {
                val item = ShoppingItemEntity(
                    listId = currentListId,
                    name = name.trim(),
                    quantity = quantity,
                    category = category.trim(),
                    customNote = notes?.trim()?.takeIf { it.isNotEmpty() },
                    brand = brand?.trim()?.takeIf { it.isNotEmpty() }
                )
                repository.insertShoppingItemSimple(item)
                _message.value = "已添加「$name」到购物清单"
            } catch (e: Exception) {
                _error.value = "添加失败: ${e.message}"
            }
        }
    }

    /**
     * 切换购物项目的购买状态
     */
    fun toggleItemPurchaseStatus(item: ShoppingItemEntity, isPurchased: Boolean) {
        viewModelScope.launch {
            try {
                val updatedItem = item.copy(isPurchased = isPurchased)
                repository.updateShoppingItem(updatedItem)
                
                val statusText = if (isPurchased) "已购买" else "待购买"
                _message.value = "「${item.name}」已标记为$statusText"
            } catch (e: Exception) {
                _error.value = "更新状态失败: ${e.message}"
            }
        }
    }

    /**
     * 删除购物项目
     */
    fun deleteShoppingItem(item: ShoppingItemEntity) {
        viewModelScope.launch {
            try {
                repository.deleteShoppingItem(item)
                _message.value = "已删除「${item.name}」"
            } catch (e: Exception) {
                _error.value = "删除失败: ${e.message}"
            }
        }
    }

    /**
     * 清除当前清单的已购买项目
     */
    fun clearPurchasedItems() {
        viewModelScope.launch {
            try {
                repository.clearPurchasedShoppingItemsByListId(currentListId)
                _message.value = "已清除所有已购买项目"
            } catch (e: Exception) {
                _error.value = "清除失败: ${e.message}"
            }
        }
    }

    /**
     * 从库存物品创建购物项目
     */
    fun addItemFromInventory(itemId: Long) {
        viewModelScope.launch {
            try {
                val shoppingItemId = repository.createShoppingItemFromInventory(itemId, currentListId)
                if (shoppingItemId != null) {
                    _message.value = "已添加到购物清单"
                } else {
                    _error.value = "添加失败：找不到该物品"
                }
            } catch (e: Exception) {
                _error.value = "添加失败: ${e.message}"
            }
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * 清除消息
     */
    fun clearMessage() {
        _message.value = null
    }
} 