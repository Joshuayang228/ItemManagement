package com.example.itemmanagement.ui.borrow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.repository.BorrowRepository

/**
 * 借出列表ViewModel工厂类（基于统一架构）
 */
class BorrowListViewModelFactory(
    private val borrowRepository: BorrowRepository,
    private val context: android.content.Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BorrowListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BorrowListViewModel(borrowRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
