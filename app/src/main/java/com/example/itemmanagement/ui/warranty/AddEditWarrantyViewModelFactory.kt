package com.example.itemmanagement.ui.warranty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.data.repository.WarrantyRepository

/**
 * 添加/编辑保修ViewModel工厂类（基于统一架构）
 */
class AddEditWarrantyViewModelFactory(
    private val warrantyRepository: WarrantyRepository,
    private val itemRepository: UnifiedItemRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEditWarrantyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddEditWarrantyViewModel(warrantyRepository, itemRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

