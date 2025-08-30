package com.example.itemmanagement.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.ui.base.ItemStateCacheViewModel

/**
 * NewEditItemViewModel 的工厂类
 */
class NewEditItemViewModelFactory(
    private val repository: ItemRepository,
    private val cacheViewModel: ItemStateCacheViewModel,
    private val itemId: Long
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewEditItemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NewEditItemViewModel(repository, cacheViewModel, itemId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}