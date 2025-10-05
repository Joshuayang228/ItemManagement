package com.example.itemmanagement.ui.borrow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.repository.BorrowRepository
import com.example.itemmanagement.data.repository.UnifiedItemRepository

/**
 * 添加借出记录ViewModel工厂类（基于统一架构）
 */
class AddBorrowViewModelFactory(
    private val borrowRepository: BorrowRepository,
    private val itemRepository: UnifiedItemRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddBorrowViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddBorrowViewModel(borrowRepository, itemRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

