package com.example.itemmanagement.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.data.repository.WarrantyRepository
import com.example.itemmanagement.ui.base.ItemStateCacheViewModel

class AddItemViewModelFactory(
    private val repository: UnifiedItemRepository,
    private val cacheViewModel: ItemStateCacheViewModel,
    private val warrantyRepository: WarrantyRepository? = null  // 可选参数，保持向后兼容
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddItemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddItemViewModel(repository, cacheViewModel, warrantyRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 