package com.example.itemmanagement.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.model.WarehouseItem
import com.example.itemmanagement.ui.warehouse.FilterState
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ItemListViewModel(private val repository: ItemRepository) : ViewModel() {
    
    private val _items = MutableLiveData<List<WarehouseItem>>()
    val items: LiveData<List<WarehouseItem>> = _items
    
    /**
     * 加载所有物品
     */
    fun loadAllItems() {
        viewModelScope.launch {
            repository.getWarehouseItems(FilterState()).asLiveData().observeForever { itemList ->
                _items.value = itemList
            }
        }
    }
    
    /**
     * 加载即将过期的物品
     */
    fun loadExpiringItems() {
        viewModelScope.launch {
            repository.getAllItems().map { allItems ->
                val now = System.currentTimeMillis()
                val sevenDaysFromNow = now + (7 * 24 * 60 * 60 * 1000L)
                
                allItems.filter { item ->
                    item.expirationDate != null && 
                    item.expirationDate!!.time > now && 
                    item.expirationDate!!.time <= sevenDaysFromNow
                }.map { item ->
                    // 转换为WarehouseItem格式
                    WarehouseItem(
                        id = item.id,
                        name = item.name,
                        primaryPhotoUri = item.photos.firstOrNull()?.uri,
                        quantity = item.quantity.toInt(),
                        expirationDate = item.expirationDate?.time,
                        locationArea = item.location?.area,
                        locationContainer = item.location?.container,
                        locationSublocation = item.location?.sublocation,
                        category = item.category,
                        subCategory = item.subCategory,
                        brand = item.brand,
                        rating = item.rating?.toFloat(),
                        price = item.price,
                        priceUnit = item.priceUnit,
                        openStatus = item.openStatus == com.example.itemmanagement.data.model.OpenStatus.OPENED,
                        addDate = item.addDate.time,
                        tagsList = item.tags.joinToString(",") { it.name },
                        customNote = item.customNote
                    )
                }
            }.asLiveData().observeForever { itemList ->
                _items.value = itemList
            }
        }
    }
    
    /**
     * 加载过期物品
     */
    fun loadExpiredItems() {
        viewModelScope.launch {
            repository.getAllItems().map { allItems ->
                val now = System.currentTimeMillis()
                
                allItems.filter { item ->
                    item.expirationDate != null && item.expirationDate!!.time <= now
                }.map { item ->
                    // 转换为WarehouseItem格式
                    WarehouseItem(
                        id = item.id,
                        name = item.name,
                        primaryPhotoUri = item.photos.firstOrNull()?.uri,
                        quantity = item.quantity.toInt(),
                        expirationDate = item.expirationDate?.time,
                        locationArea = item.location?.area,
                        locationContainer = item.location?.container,
                        locationSublocation = item.location?.sublocation,
                        category = item.category,
                        subCategory = item.subCategory,
                        brand = item.brand,
                        rating = item.rating?.toFloat(),
                        price = item.price,
                        priceUnit = item.priceUnit,
                        openStatus = item.openStatus == com.example.itemmanagement.data.model.OpenStatus.OPENED,
                        addDate = item.addDate.time,
                        tagsList = item.tags.joinToString(",") { it.name },
                        customNote = item.customNote
                    )
                }
            }.asLiveData().observeForever { itemList ->
                _items.value = itemList
            }
        }
    }
    
    /**
     * 加载库存不足的物品
     */
    fun loadLowStockItems() {
        viewModelScope.launch {
            repository.getAllItems().map { allItems ->
                allItems.filter { item ->
                    item.quantity <= 3 && item.quantity > 0
                }.map { item ->
                    // 转换为WarehouseItem格式
                    WarehouseItem(
                        id = item.id,
                        name = item.name,
                        primaryPhotoUri = item.photos.firstOrNull()?.uri,
                        quantity = item.quantity.toInt(),
                        expirationDate = item.expirationDate?.time,
                        locationArea = item.location?.area,
                        locationContainer = item.location?.container,
                        locationSublocation = item.location?.sublocation,
                        category = item.category,
                        subCategory = item.subCategory,
                        brand = item.brand,
                        rating = item.rating?.toFloat(),
                        price = item.price,
                        priceUnit = item.priceUnit,
                        openStatus = item.openStatus == com.example.itemmanagement.data.model.OpenStatus.OPENED,
                        addDate = item.addDate.time,
                        tagsList = item.tags.joinToString(",") { it.name },
                        customNote = item.customNote
                    )
                }
            }.asLiveData().observeForever { itemList ->
                _items.value = itemList
            }
        }
    }
    
    /**
     * 加载心愿单物品
     */
    fun loadWishlistItems() {
        viewModelScope.launch {
            repository.getAllItems().map { allItems ->
                allItems.filter { item ->
                    item.isWishlistItem // 筛选出标记为心愿单的物品
                }.map { item ->
                    // 转换为WarehouseItem格式
                    WarehouseItem(
                        id = item.id,
                        name = item.name,
                        primaryPhotoUri = item.photos.firstOrNull()?.uri,
                        quantity = item.quantity.toInt(),
                        expirationDate = item.expirationDate?.time,
                        locationArea = item.location?.area,
                        locationContainer = item.location?.container,
                        locationSublocation = item.location?.sublocation,
                        category = item.category,
                        subCategory = item.subCategory,
                        brand = item.brand,
                        rating = item.rating?.toFloat(),
                        price = item.price,
                        priceUnit = item.priceUnit,
                        openStatus = item.openStatus == com.example.itemmanagement.data.model.OpenStatus.OPENED,
                        addDate = item.addDate.time,
                        tagsList = item.tags.joinToString(",") { it.name },
                        customNote = item.customNote
                    )
                }
            }.asLiveData().observeForever { itemList ->
                _items.value = itemList
            }
        }
    }
}

class ItemListViewModelFactory(private val repository: ItemRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ItemListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ItemListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 