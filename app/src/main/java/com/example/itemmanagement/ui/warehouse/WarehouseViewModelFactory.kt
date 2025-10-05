package com.example.itemmanagement.ui.warehouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.repository.UnifiedItemRepository

/**
 * 仓库ViewModel工厂类（基于统一架构）
 */
class WarehouseViewModelFactory(private val repository: UnifiedItemRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WarehouseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WarehouseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

