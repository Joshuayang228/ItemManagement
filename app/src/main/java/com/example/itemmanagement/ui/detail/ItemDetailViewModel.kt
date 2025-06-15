package com.example.itemmanagement.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.mapper.toItem
import com.example.itemmanagement.data.model.Item
import kotlinx.coroutines.launch

class ItemDetailViewModel(private val repository: ItemRepository) : ViewModel() {
    private val _item = MutableLiveData<Item>()
    val item: LiveData<Item> = _item

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    /**
     * 加载物品详情
     * @param id 物品ID
     */
    fun loadItem(id: Long) {
        viewModelScope.launch {
            try {
                val itemWithDetails = repository.getItemWithDetailsById(id)
                if (itemWithDetails != null) {
                    _item.value = itemWithDetails.toItem()
                } else {
                    _errorMessage.value = "找不到该物品"
                }
            } catch (e: Exception) {
                _errorMessage.value = "加载物品失败：${e.message}"
            }
        }
    }
} 