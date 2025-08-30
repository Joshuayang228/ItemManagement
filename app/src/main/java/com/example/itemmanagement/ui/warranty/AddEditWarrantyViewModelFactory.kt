package com.example.itemmanagement.ui.warranty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.repository.WarrantyRepository

class AddEditWarrantyViewModelFactory(
    private val warrantyRepository: WarrantyRepository,
    private val itemRepository: ItemRepository
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEditWarrantyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddEditWarrantyViewModel(warrantyRepository, itemRepository) as T
        }
        throw IllegalArgumentException("未知的ViewModel类: ${modelClass.name}")
    }
}
