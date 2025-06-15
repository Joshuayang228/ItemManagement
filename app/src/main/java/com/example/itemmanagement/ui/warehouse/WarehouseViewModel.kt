package com.example.itemmanagement.ui.warehouse

import androidx.lifecycle.*
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.entity.ItemEntity
import com.example.itemmanagement.data.model.Item
import com.example.itemmanagement.data.mapper.toItemEntity
import kotlinx.coroutines.launch

class WarehouseViewModel(private val repository: ItemRepository) : ViewModel() {
    /**
     * 所有物品的LiveData
     * 通过Flow转换为LiveData，以便在UI中观察数据变化
     */
    val items: LiveData<List<Item>> = repository.getAllItems().asLiveData()

    private val _deleteResult = MutableLiveData<Boolean>()
    val deleteResult: LiveData<Boolean> = _deleteResult

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    /**
     * 删除物品
     * @param item 要删除的物品
     */
    fun deleteItem(item: Item) {
        viewModelScope.launch {
            try {
                repository.deleteItem(item.toItemEntity())
                _deleteResult.value = true
            } catch (e: Exception) {
                _errorMessage.value = "删除失败：${e.message}"
                _deleteResult.value = false
            }
        }
    }
} 