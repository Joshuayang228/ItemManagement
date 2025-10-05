package com.example.itemmanagement.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.ui.base.ItemStateCacheViewModel

class AddShoppingItemViewModelFactory(
    private val repository: ItemRepository,
    private val cacheViewModel: ItemStateCacheViewModel,
    private val listId: Long
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddShoppingItemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddShoppingItemViewModel(repository, cacheViewModel, listId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}