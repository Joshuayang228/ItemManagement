package com.example.itemmanagement.ui.shopping

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import com.example.itemmanagement.data.model.Item
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.data.entity.unified.InventoryDetailEntity
import com.example.itemmanagement.data.entity.PriceRecord
import com.example.itemmanagement.data.repository.PriceStatistics
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import java.util.Date

/**
 * 购物物品详情ViewModel
 * 管理购物物品的详细信息展示和操作
 */
class ShoppingItemDetailViewModel(
    private val repository: UnifiedItemRepository,
    private val itemId: Long,
    private val listId: Long
) : ViewModel() {

    private val _itemDetail = MutableLiveData<Item?>()
    val itemDetail: LiveData<Item?> = _itemDetail

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _navigateBack = MutableLiveData<Boolean>()
    val navigateBack: LiveData<Boolean> = _navigateBack

    // 价格记录相关
    val priceRecords: LiveData<List<PriceRecord>> = repository.getPriceRecords(itemId).asLiveData()
    
    private val _priceStatistics = MutableLiveData<PriceStatistics?>()
    val priceStatistics: LiveData<PriceStatistics?> = _priceStatistics
    
    private val _latestPricesByChannel = MutableLiveData<List<PriceRecord>>()
    val latestPricesByChannel: LiveData<List<PriceRecord>> = _latestPricesByChannel

    init {
        android.util.Log.d("ShoppingDetail", "========== ViewModel初始化 ==========")
        android.util.Log.d("ShoppingDetail", "物品ID: $itemId")
        android.util.Log.d("ShoppingDetail", "清单ID: $listId")
        loadItemDetail()
        loadPriceData()
    }

    /**
     * 加载物品详情
     */
    fun loadItemDetail() {
        android.util.Log.d("ShoppingDetail", "开始加载物品详情...")
        viewModelScope.launch {
            try {
                _isLoading.value = true
                android.util.Log.d("ShoppingDetail", "调用Repository查询物品: $itemId")
                val item = repository.getItemById(itemId)
                android.util.Log.d("ShoppingDetail", "查询结果: ${item?.name ?: "null"}")
                if (item != null) {
                    android.util.Log.d("ShoppingDetail", "ShoppingDetail存在: ${item.shoppingDetail != null}")
                    android.util.Log.d("ShoppingDetail", "照片数量: ${item.photos.size}")
                    android.util.Log.d("ShoppingDetail", "标签数量: ${item.tags.size}")
                }
                _itemDetail.value = item
                _isLoading.value = false
            } catch (e: Exception) {
                android.util.Log.e("ShoppingDetail", "加载失败", e)
                _error.value = "加载物品详情失败: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * 切换已购买状态
     */
    fun togglePurchaseStatus() {
        val item = _itemDetail.value ?: return
        val shoppingDetail = item.shoppingDetail ?: return
        
        viewModelScope.launch {
            try {
                // 切换购买状态
                val newPurchasedStatus = !shoppingDetail.isPurchased
                val updatedShoppingDetail = shoppingDetail.copy(isPurchased = newPurchasedStatus)
                
                // 创建UnifiedItemEntity
                val unifiedItem = com.example.itemmanagement.data.entity.unified.UnifiedItemEntity(
                    id = itemId,
                    name = item.name,
                    category = item.category ?: "未分类",
                    subCategory = item.subCategory,
                    brand = item.brand,
                    specification = item.specification,
                    customNote = item.customNote,
                    createdDate = item.addDate
                )
                
                // 更新数据库
                repository.updateShoppingItem(
                    itemId = itemId,
                    unifiedItem = unifiedItem,
                    shoppingDetail = updatedShoppingDetail,
                    photoUris = emptyList(),
                    tags = emptyMap()
                )
                
                // 重新加载数据
                loadItemDetail()
                
                _message.value = if (newPurchasedStatus) "已标记为已购买" else "已取消购买标记"
            } catch (e: Exception) {
                android.util.Log.e("ShoppingDetail", "切换购买状态失败", e)
                _error.value = "操作失败: ${e.message}"
            }
        }
    }

    /**
     * 删除物品（软删除，移至回收站）
     */
    fun deleteItem() {
        viewModelScope.launch {
            try {
                repository.softDeleteItem(itemId, "用户从详情页删除")
                _message.value = "物品已移至回收站"
                _navigateBack.value = true
            } catch (e: Exception) {
                _error.value = "删除失败: ${e.message}"
            }
        }
    }

    /**
     * 转入库存
     */
    fun transferToInventory() {
        val item = _itemDetail.value ?: return
        val shoppingDetail = item.shoppingDetail ?: return
        
        viewModelScope.launch {
            try {
                // 创建库存详情
                val inventoryDetail = InventoryDetailEntity(
                    itemId = itemId,
                    quantity = shoppingDetail.quantity,
                    unit = shoppingDetail.quantityUnit,
                    price = shoppingDetail.actualPrice ?: shoppingDetail.estimatedPrice,
                    priceUnit = shoppingDetail.actualPriceUnit ?: shoppingDetail.estimatedPriceUnit ?: "CNY",
                    purchaseDate = Date(),
                    storeName = shoppingDetail.storeName,
                    locationId = null,
                    expirationDate = null,
                    status = com.example.itemmanagement.data.model.ItemStatus.IN_STOCK,
                    openStatus = com.example.itemmanagement.data.model.OpenStatus.UNOPENED
                )
                
                // 调用Repository的转换方法
                repository.transferShoppingToInventory(
                    itemId = itemId,
                    shoppingDetail = shoppingDetail,
                    inventoryDetail = inventoryDetail
                )
                
                _message.value = "已转入库存"
                _navigateBack.value = true
            } catch (e: Exception) {
                _error.value = "转入库存失败: ${e.message}"
            }
        }
    }

    /**
     * 错误已处理
     */
    fun onErrorHandled() {
        _error.value = null
    }

    /**
     * 消息已显示
     */
    fun onMessageShown() {
        _message.value = null
    }

    /**
     * 导航已完成
     */
    fun onNavigationComplete() {
        _navigateBack.value = false
    }
    
    // ========================================
    // 价格记录管理
    // ========================================
    
    /**
     * 加载价格数据
     */
    private fun loadPriceData() {
        viewModelScope.launch {
            try {
                // 加载价格统计
                val statistics = repository.getPriceStatistics(itemId)
                _priceStatistics.value = statistics
                
                // 加载各渠道最新价格
                val latestPrices = repository.getLatestPricesByChannel(itemId)
                _latestPricesByChannel.value = latestPrices
                
                android.util.Log.d("PriceTracking", "价格数据已加载: ${statistics.recordCount}条记录")
            } catch (e: Exception) {
                android.util.Log.e("PriceTracking", "加载价格数据失败", e)
            }
        }
    }
    
    /**
     * 添加价格记录
     */
    fun addPriceRecord(record: PriceRecord) {
        viewModelScope.launch {
            try {
                repository.addPriceRecord(record)
                loadPriceData() // 重新加载价格数据
                _message.value = "价格已记录"
            } catch (e: Exception) {
                _error.value = "记录价格失败: ${e.message}"
            }
        }
    }
    
    /**
     * 删除价格记录
     */
    fun deletePriceRecord(record: PriceRecord) {
        viewModelScope.launch {
            try {
                repository.deletePriceRecord(record)
                loadPriceData() // 重新加载价格数据
                _message.value = "价格记录已删除"
            } catch (e: Exception) {
                _error.value = "删除失败: ${e.message}"
            }
        }
    }
}

/**
 * ViewModelFactory
 */
class ShoppingItemDetailViewModelFactory(
    private val repository: UnifiedItemRepository,
    private val itemId: Long,
    private val listId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingItemDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShoppingItemDetailViewModel(repository, itemId, listId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
