package com.example.itemmanagement.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.entity.ItemEntity
import com.example.itemmanagement.data.mapper.toItem
import com.example.itemmanagement.data.model.Item
import kotlinx.coroutines.launch

class ItemDetailViewModel(private val repository: ItemRepository) : ViewModel() {
    private val _item = MutableLiveData<Item>()
    val item: LiveData<Item> = _item

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    private val _navigateBack = MutableLiveData<Boolean>()
    val navigateBack: LiveData<Boolean> = _navigateBack

    /**
     * 加载物品详情
     * @param id 物品ID
     */
    fun loadItem(id: Long) {
        viewModelScope.launch {
            try {
                val itemWithDetails = repository.getItemWithDetailsById(id)
                if (itemWithDetails != null) {
                    val item = itemWithDetails.toItem()
                    _item.value = item
                } else {
                    _errorMessage.value = "找不到该物品"
                }
            } catch (e: Exception) {
                _errorMessage.value = "加载物品失败：${e.message}"
            }
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
                    repository.deleteItem(itemWithDetails.item)
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
} 