package com.example.itemmanagement.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.entity.ItemEntity
import com.example.itemmanagement.data.model.Item
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: ItemRepository) : ViewModel() {
    
    // 获取所有物品数据
    val allItems: LiveData<List<Item>> = repository.getAllItems().asLiveData()
    
    // 删除物品
    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            // 先获取物品详情
            val item = repository.getItemWithDetailsById(itemId)
            // 如果物品存在，则删除
            item?.let { 
                repository.deleteItem(it.item)
            }
        }
    }
}

class HomeViewModelFactory(private val repository: ItemRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 