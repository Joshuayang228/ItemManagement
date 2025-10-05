package com.example.itemmanagement.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.ItemRepository

class InventoryAnalysisViewModelFactory(
    private val repository: ItemRepository
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryAnalysisViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryAnalysisViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 