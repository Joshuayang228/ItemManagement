package com.example.itemmanagement.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.repository.UnifiedItemRepository

/**
 * 物品日历ViewModel工厂类（基于统一架构）
 */
class ItemCalendarViewModelFactory(
    private val repository: UnifiedItemRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ItemCalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ItemCalendarViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

