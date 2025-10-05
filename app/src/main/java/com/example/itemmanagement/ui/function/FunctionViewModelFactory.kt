package com.example.itemmanagement.ui.function

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.repository.UnifiedItemRepository

/**
 * 功能页面ViewModel工厂类（基于统一架构）
 */
class FunctionViewModelFactory(private val repository: UnifiedItemRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FunctionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FunctionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

