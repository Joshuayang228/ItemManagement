package com.example.itemmanagement.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.data.entity.unified.UnifiedItemEntity
import com.example.itemmanagement.data.entity.unified.ShoppingDetailEntity
import com.example.itemmanagement.data.entity.unified.ItemStateType
import com.example.itemmanagement.data.entity.PriceRecord
import com.example.itemmanagement.data.mapper.toItem
import com.example.itemmanagement.data.model.Item
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ItemDetailViewModel(private val repository: UnifiedItemRepository) : ViewModel() {
    private val _item = MutableLiveData<Item>()
    val item: LiveData<Item> = _item

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    private val _navigateBack = MutableLiveData<Boolean>()
    val navigateBack: LiveData<Boolean> = _navigateBack
    
    // 来源信息：购物清单详情（如果存在）
    private val _shoppingSource = MutableLiveData<ShoppingDetailEntity?>()
    val shoppingSource: LiveData<ShoppingDetailEntity?> = _shoppingSource
    
    // 价格记录（用于来源信息的价格跟踪）
    private val _sourcePriceRecords = MutableLiveData<List<PriceRecord>>()
    val sourcePriceRecords: LiveData<List<PriceRecord>> = _sourcePriceRecords
    
    // 保修信息
    private val _warranty = MutableLiveData<com.example.itemmanagement.data.entity.WarrantyEntity?>()
    val warranty: LiveData<com.example.itemmanagement.data.entity.WarrantyEntity?> = _warranty

    /**
     * 加载物品详情
     * @param id 物品ID
     */
    fun loadItem(id: Long) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ItemDetailViewModel", "🔍 开始加载物品详情，ID: $id")
                val itemWithDetails = repository.getItemWithDetailsById(id)
                android.util.Log.d("ItemDetailViewModel", "📦 获取到ItemWithDetails: $itemWithDetails")
                
                if (itemWithDetails != null) {
                    android.util.Log.d("ItemDetailViewModel", "✅ ItemWithDetails不为空，开始转换为Item")
                    android.util.Log.d("ItemDetailViewModel", "📋 UnifiedItem: ${itemWithDetails.unifiedItem}")
                    android.util.Log.d("ItemDetailViewModel", "📋 InventoryDetail: ${itemWithDetails.inventoryDetail}")
                    android.util.Log.d("ItemDetailViewModel", "📸 Photos: ${itemWithDetails.photos}")
                    android.util.Log.d("ItemDetailViewModel", "🏷️ Tags: ${itemWithDetails.tags}")
                    
                    val item = itemWithDetails.toItem()
                    android.util.Log.d("ItemDetailViewModel", "🎯 转换后的Item: $item")
                    _item.value = item
                    
                    // 加载来源信息（检查是否从购物清单转入）
                    loadSourceInfo(id)
                    
                    // 加载保修信息
                    loadWarrantyInfo(id)
                    
                    android.util.Log.d("ItemDetailViewModel", "✅ 物品详情加载成功")
                } else {
                    android.util.Log.w("ItemDetailViewModel", "❌ ItemWithDetails为空")
                    _errorMessage.value = "找不到该物品"
                }
            } catch (e: Exception) {
                android.util.Log.e("ItemDetailViewModel", "❌ 加载物品失败", e)
                _errorMessage.value = "加载物品失败：${e.message}"
            }
        }
    }
    
    /**
     * 加载来源信息
     * 检查物品是否从购物清单转入，如果是，加载购物详情作为来源信息
     */
    private suspend fun loadSourceInfo(itemId: Long) {
        try {
            android.util.Log.d("ItemDetailViewModel", "🔍 开始加载来源信息，itemId: $itemId")
            
            // 获取所有已停用的 SHOPPING 状态
            val shoppingStates = repository.getItemStatesByItemIdAndType(itemId, ItemStateType.SHOPPING)
            val deactivatedShoppingState = shoppingStates.find { !it.isActive }
            
            if (deactivatedShoppingState != null) {
                android.util.Log.d("ItemDetailViewModel", "✅ 找到已停用的购物状态，说明是从购物清单转入")
                
                // 获取购物详情
                val shoppingDetail = repository.getShoppingDetailByItemId(itemId)
                if (shoppingDetail != null) {
                    android.util.Log.d("ItemDetailViewModel", "✅ 获取到购物详情: $shoppingDetail")
                    _shoppingSource.value = shoppingDetail
                    
                    // 加载价格记录
                    loadSourcePriceRecords(itemId)
                } else {
                    android.util.Log.w("ItemDetailViewModel", "⚠️ 找到购物状态但未找到购物详情")
                    _shoppingSource.value = null
                    _sourcePriceRecords.value = emptyList()
                }
            } else {
                android.util.Log.d("ItemDetailViewModel", "📝 未找到已停用的购物状态，物品为手动添加")
                _shoppingSource.value = null
                _sourcePriceRecords.value = emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("ItemDetailViewModel", "❌ 加载来源信息失败", e)
            _shoppingSource.value = null
            _sourcePriceRecords.value = emptyList()
        }
    }
    
    /**
     * 加载来源物品的价格记录
     */
    private suspend fun loadSourcePriceRecords(itemId: Long) {
        try {
            android.util.Log.d("ItemDetailViewModel", "🔍 开始加载价格记录，itemId: $itemId")
            val records = repository.getPriceRecordsByItemId(itemId)
            _sourcePriceRecords.value = records
            android.util.Log.d("ItemDetailViewModel", "✅ 获取到 ${records.size} 条价格记录")
        } catch (e: Exception) {
            android.util.Log.e("ItemDetailViewModel", "❌ 加载价格记录失败", e)
            _sourcePriceRecords.value = emptyList()
        }
    }
    
    /**
     * 加载保修信息
     */
    private suspend fun loadWarrantyInfo(itemId: Long) {
        try {
            android.util.Log.d("ItemDetailViewModel", "🔍 开始加载保修信息，itemId: $itemId")
            val warranty = repository.getWarrantyByItemId(itemId)
            _warranty.value = warranty
            android.util.Log.d("ItemDetailViewModel", "✅ 获取到保修信息: ${if (warranty != null) "有效" else "无"}")
        } catch (e: Exception) {
            android.util.Log.e("ItemDetailViewModel", "❌ 加载保修信息失败", e)
            _warranty.value = null
        }
    }
    
    /**
     * 删除物品
     * @param id 物品ID
     */
    fun deleteItem(id: Long) {
        viewModelScope.launch {
            try {
                // 获取物品详情
                val itemWithDetails = repository.getItemWithDetailsById(id)
                if (itemWithDetails != null) {
                    // 删除物品
                    repository.deleteItem(itemWithDetails.toItem())
                    _errorMessage.value = "物品已删除"
                    // 导航回上一页
                    _navigateBack.value = true
                } else {
                    _errorMessage.value = "找不到该物品"
                }
            } catch (e: Exception) {
                _errorMessage.value = "删除物品失败：${e.message}"
            }
        }
    }
    
    /**
     * 重置导航状态
     */
    fun onNavigationComplete() {
        _navigateBack.value = false
    }
    
    /**
     * 加载所有活跃的购物清单
     */
    fun loadActiveShoppingLists(callback: (List<com.example.itemmanagement.data.entity.ShoppingListEntity>) -> Unit) {
        viewModelScope.launch {
            try {
                // 只获取第一个值，不持续监听
                val lists = repository.getActiveShoppingLists().first()
                callback(lists)
            } catch (e: Exception) {
                android.util.Log.e("ItemDetailViewModel", "❌ 加载购物清单失败", e)
                callback(emptyList())
            }
        }
    }
    
    /**
     * 将库存物品添加到购物清单
     */
    fun addToShoppingList(
        itemId: Long,
        shoppingListId: Long,
        quantity: Double,
        purchaseReason: String
    ) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ItemDetailViewModel", "🛒 开始添加到购物清单: itemId=$itemId, listId=$shoppingListId, quantity=$quantity")
                
                // 获取物品详情
                val itemWithDetails = repository.getItemWithDetailsById(itemId)
                if (itemWithDetails == null) {
                    _errorMessage.value = "找不到该物品"
                    return@launch
                }
                
                val unifiedItem = itemWithDetails.unifiedItem
                val inventoryDetail = itemWithDetails.inventoryDetail
                
                if (inventoryDetail == null) {
                    _errorMessage.value = "该物品不是库存物品"
                    return@launch
                }
                
                // 使用 ShoppingItemMapper 转换
                val mapper = com.example.itemmanagement.data.mapper.ShoppingItemMapper
                val (updatedUnifiedItem, shoppingDetail) = mapper.inventoryToShoppingItem(
                    unifiedItem = unifiedItem,
                    inventoryDetail = inventoryDetail,
                    shoppingListId = shoppingListId,
                    quantity = quantity,
                    priority = com.example.itemmanagement.data.entity.ShoppingItemPriority.NORMAL,
                    purchaseReason = if (purchaseReason.isNotEmpty()) purchaseReason else null
                )
                
                // 保存到数据库（使用事务）
                repository.addShoppingItemToExistingItem(
                    itemId = itemId,
                    shoppingDetail = shoppingDetail
                )
                
                _errorMessage.value = "已添加到购物清单"
                android.util.Log.d("ItemDetailViewModel", "✅ 添加到购物清单成功")
                
            } catch (e: Exception) {
                android.util.Log.e("ItemDetailViewModel", "❌ 添加到购物清单失败", e)
                _errorMessage.value = "添加失败：${e.message}"
            }
        }
    }
} 