package com.example.itemmanagement.ui.warranty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.repository.WarrantyRepository
import android.content.Context

class WarrantyListViewModelFactory(
    private val warrantyRepository: WarrantyRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WarrantyListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WarrantyListViewModel(warrantyRepository, context) as T
        }
        throw IllegalArgumentException("未知的ViewModel类: ${modelClass.name}")
    }
}
