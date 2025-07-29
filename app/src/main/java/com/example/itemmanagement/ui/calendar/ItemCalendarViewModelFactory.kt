package com.example.itemmanagement.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.ItemRepository

class ItemCalendarViewModelFactory(
    private val repository: ItemRepository
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ItemCalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ItemCalendarViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 