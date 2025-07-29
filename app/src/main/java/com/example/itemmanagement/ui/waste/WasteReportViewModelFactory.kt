package com.example.itemmanagement.ui.waste

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.ItemRepository

class WasteReportViewModelFactory(
    private val repository: ItemRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WasteReportViewModel::class.java)) {
            return WasteReportViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 