package com.example.itemmanagement.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.repository.UnifiedItemRepository

/**
 * 库存分析ViewModel工厂类（基于统一架构）
 */
class InventoryAnalysisViewModelFactory(private val repository: UnifiedItemRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryAnalysisViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryAnalysisViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

