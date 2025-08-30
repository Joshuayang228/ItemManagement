package com.example.itemmanagement.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.entity.ItemEntity
import com.example.itemmanagement.data.model.Item
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: ItemRepository) : ViewModel() {
    
    // 搜索关键词的MutableLiveData
    private val _searchQuery = MutableLiveData<String>()
    
    // 根据搜索关键词动态获取物品数据
    val items: LiveData<List<Item>> = _searchQuery.switchMap { query ->
        if (query.isNullOrBlank()) {
            // 如果搜索关键词为空，显示所有物品
            repository.getAllItems().asLiveData()
        } else {
            // 普通搜索
            repository.getAllItems().map { allItems ->
                allItems.filter { item ->
                    // 在物品名称、备注和品牌中搜索
                    item.name.contains(query, ignoreCase = true) ||
                    (!item.customNote.isNullOrBlank() && item.customNote.contains(query, ignoreCase = true)) ||
                    (!item.brand.isNullOrBlank() && item.brand.contains(query, ignoreCase = true)) ||
                    (!item.category.isNullOrBlank() && item.category.contains(query, ignoreCase = true)) ||
                    (!item.subCategory.isNullOrBlank() && item.subCategory.contains(query, ignoreCase = true)) ||
                    // 也在标签中搜索
                    item.tags.any { tag -> tag.name.contains(query, ignoreCase = true) }
                }
            }.asLiveData()
        }
    }
    
    // 为了保持向后兼容，保留原有的allItems属性
    val allItems: LiveData<List<Item>> = items
    
    // 搜索状态
    private val _isSearching = MutableLiveData<Boolean>(false)
    val isSearching: LiveData<Boolean> = _isSearching
    
    init {
        // 初始化时显示所有物品
        _searchQuery.value = ""
    }
    
    /**
     * 设置搜索关键词
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _isSearching.value = query.isNotBlank()
    }
    
    /**
     * 清空搜索
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _isSearching.value = false
    }
    
    /**
     * 获取当前搜索关键词
     */
    fun getCurrentSearchQuery(): String? {
        return _searchQuery.value
    }
    
    /**
     * 刷新数据
     * 通过重新设置搜索关键词来触发数据刷新
     */
    fun refreshData() {
        val currentQuery = _searchQuery.value ?: ""
        _searchQuery.value = currentQuery
    }
    
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