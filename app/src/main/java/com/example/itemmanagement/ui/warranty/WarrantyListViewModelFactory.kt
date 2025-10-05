package com.example.itemmanagement.ui.warranty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.repository.WarrantyRepository

/**
 * 保修列表ViewModel工厂类
 */
class WarrantyListViewModelFactory(
    private val warrantyRepository: WarrantyRepository,
    private val context: android.content.Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WarrantyListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WarrantyListViewModel(warrantyRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
