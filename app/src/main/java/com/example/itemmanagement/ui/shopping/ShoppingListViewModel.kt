package com.example.itemmanagement.ui.shopping

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.data.entity.unified.ItemStateType
import com.example.itemmanagement.data.entity.unified.InventoryDetailEntity
import com.example.itemmanagement.data.model.ItemStatus
import com.example.itemmanagement.data.model.OpenStatus
import com.example.itemmanagement.data.model.Item
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.util.Date

/**
 * 购物清单ViewModel
 * 管理单个购物清单的物品列表和操作
 */
class ShoppingListViewModel(
    private val repository: UnifiedItemRepository,
    private val listId: Long
) : ViewModel() {

    // 当前购物清单的所有物品
    val shoppingItems: LiveData<List<Item>> = 
        repository.getItemsByShoppingList(listId).asLiveData()

    // 待购买项目数量
    private val _pendingItemsCount = MutableLiveData<Int>()
    val pendingItemsCount: LiveData<Int> = _pendingItemsCount
    
    // 已购买项目数量
    private val _purchasedItemsCount = MutableLiveData<Int>()
    val purchasedItemsCount: LiveData<Int> = _purchasedItemsCount

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    init {
        loadStatistics()
    }

    /**
     * 加载统计信息
     */
    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                val items = repository.getItemsByShoppingList(listId).first()
                _pendingItemsCount.value = items.count { it.shoppingDetail?.isPurchased == false }
                _purchasedItemsCount.value = items.count { it.shoppingDetail?.isPurchased == true }
            } catch (e: Exception) {
                _error.value = "加载统计信息失败: ${e.message}"
            }
        }
    }

    /**
     * 切换物品的购买状态
     */
    fun toggleItemPurchaseStatus(item: Item, isPurchased: Boolean) {
        viewModelScope.launch {
            try {
                val shoppingDetail = item.shoppingDetail ?: return@launch
                val updatedDetail = shoppingDetail.copy(
                    isPurchased = isPurchased,
                    purchaseDate = if (isPurchased) java.util.Date() else null
                )
                repository.updateShoppingDetail(updatedDetail)
                
                val statusText = if (isPurchased) "已购买" else "待购买"
                _message.value = "已将「${item.name}」标记为$statusText"
                
                // 刷新统计
                loadStatistics()
            } catch (e: Exception) {
                _error.value = "更新购买状态失败: ${e.message}"
            }
        }
    }

    /**
     * 删除购物物品（软删除，进入回收站）
     */
    fun deleteShoppingItem(item: Item) {
        viewModelScope.launch {
            try {
                // ✅ 使用软删除，物品进入回收站
                // - ShoppingDetailEntity 保留在数据库中作为历史记录
                // - ItemStateEntity 从 SHOPPING 变为 DELETED
                // - 可以在回收站中恢复
                repository.softDeleteItem(item.id, "从购物清单删除")
                
                _message.value = "已将「${item.name}」移至回收站"
                loadStatistics()
            } catch (e: Exception) {
                _error.value = "删除失败: ${e.message}"
            }
        }
    }

    /**
     * 清除已购买的物品（软删除，进入回收站）
     */
    fun clearPurchasedItems() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val items = repository.getItemsByShoppingList(listId).first()
                val purchasedItems = items.filter { it.shoppingDetail?.isPurchased == true }
                
                // ✅ 使用软删除，物品进入回收站
                // - 保留ShoppingDetailEntity作为购物历史记录
                // - 可以分析预算准确性（预估价格 vs 实际价格）
                // - 可以在回收站中恢复
                purchasedItems.forEach { item ->
                    repository.softDeleteItem(
                        item.id, 
                        "清除已购买物品 - 购买日期: ${item.shoppingDetail?.purchaseDate?.toString() ?: "未知"}"
                    )
                }
                
                _message.value = "已将${purchasedItems.size}个已购买物品移至回收站"
                loadStatistics()
            } catch (e: Exception) {
                _error.value = "清除失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 转入库存
     * 将购物物品转移到库存管理系统
     */
    fun transferItemToInventory(item: Item) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val shoppingDetail = item.shoppingDetail ?: run {
                    _error.value = "购物详情数据缺失"
                    return@launch
                }
                
                // 创建库存详情（从购物详情转换）
                val inventoryDetail = InventoryDetailEntity(
                    itemId = item.id,
                    quantity = shoppingDetail.quantity,
                    unit = shoppingDetail.quantityUnit,
                    price = shoppingDetail.actualPrice ?: shoppingDetail.estimatedPrice,
                    priceUnit = shoppingDetail.actualPriceUnit ?: shoppingDetail.estimatedPriceUnit,
                    purchaseDate = shoppingDetail.purchaseDate ?: Date(),
                    status = ItemStatus.IN_STOCK,
                    openStatus = OpenStatus.UNOPENED,
                    storeName = shoppingDetail.storeName
                )
                
                // 使用Repository的转移方法
                repository.transferShoppingToInventory(
                    itemId = item.id,
                    shoppingDetail = shoppingDetail,
                    inventoryDetail = inventoryDetail
                )
                
                _message.value = "「${item.name}」已成功转入库存"
                loadStatistics()
            } catch (e: Exception) {
                android.util.Log.e("ShoppingListVM", "转入库存失败", e)
                _error.value = "转入库存失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 转入库存（带用户输入数据）
     * 使用用户在对话框中填写的数据转入库存
     */
    suspend fun transferToInventoryWithData(
        item: Item,
        transferData: TransferData
    ): TransferResult {
        return try {
            _isLoading.postValue(true)
            
            val shoppingDetail = item.shoppingDetail ?: return TransferResult(
                success = false,
                error = "购物详情数据缺失"
            )
            
            // 创建库存详情记录
            val inventoryDetail = InventoryDetailEntity(
                itemId = item.id,
                quantity = transferData.quantity,
                unit = transferData.unit,
                locationId = transferData.locationId,
                price = transferData.price,
                priceUnit = transferData.priceUnit,
                status = transferData.status ?: ItemStatus.IN_STOCK,
                productionDate = transferData.productionDate,
                expirationDate = transferData.expirationDate,
                purchaseDate = Date(),
                openStatus = transferData.openStatus,
                storeName = shoppingDetail.storeName,
                purchaseChannel = shoppingDetail.purchaseChannel,
                createdDate = Date(),
                updatedDate = Date()
            )
            
            // 使用Repository的转移方法
            repository.transferShoppingToInventory(
                itemId = item.id,
                shoppingDetail = shoppingDetail,
                inventoryDetail = inventoryDetail
            )
            
            // 可选：记录价格追踪
            if (transferData.recordPrice && transferData.price != null) {
                val priceRecord = com.example.itemmanagement.data.entity.PriceRecord(
                    itemId = item.id,
                    recordDate = Date(),
                    price = transferData.price,
                    purchaseChannel = shoppingDetail.storeName ?: "其他",
                    notes = "从购物清单转入时记录"
                )
                repository.addPriceRecord(priceRecord)
            }
            
            _message.postValue("「${item.name}」已成功转入库存")
            loadStatistics()
            
            TransferResult(
                success = true,
                itemId = item.id
            )
        } catch (e: Exception) {
            android.util.Log.e("ShoppingListVM", "转入库存失败", e)
            _error.postValue("转入库存失败: ${e.message}")
            TransferResult(
                success = false,
                error = e.message
            )
        } finally {
            _isLoading.postValue(false)
        }
    }

    /**
     * 消息已显示
     */
    fun onMessageShown() {
        _message.value = null
    }

    /**
     * 错误已处理
     */
    fun onErrorHandled() {
        _error.value = null
    }
}

/**
 * 转入数据传输对象
 */
data class TransferData(
    val quantity: Double,
    val unit: String,
    val locationId: Long?,
    val price: Double?,
    val priceUnit: String?,
    val status: ItemStatus?,
    val productionDate: Date?,
    val expirationDate: Date?,
    val openStatus: OpenStatus?,
    val notes: String?,
    val recordPrice: Boolean
)

/**
 * 转入结果
 */
data class TransferResult(
    val success: Boolean,
    val itemId: Long? = null,
    val error: String? = null
)

