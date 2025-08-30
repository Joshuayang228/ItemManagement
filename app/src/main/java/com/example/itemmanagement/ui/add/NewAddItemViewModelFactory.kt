package com.example.itemmanagement.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.ui.base.ItemStateCacheViewModel

class NewAddItemViewModelFactory(
    private val repository: ItemRepository,
    private val cacheViewModel: ItemStateCacheViewModel
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewAddItemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NewAddItemViewModel(repository, cacheViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 