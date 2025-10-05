package com.example.itemmanagement.ui.borrow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.repository.BorrowRepository

/**
 * AddBorrowViewModel的工厂类
 * 用于创建带参数的ViewModel实例
 */
class AddBorrowViewModelFactory(
    private val borrowRepository: BorrowRepository,
    private val itemRepository: ItemRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddBorrowViewModel::class.java)) {
            return AddBorrowViewModel(borrowRepository, itemRepository) as T
        }
        throw IllegalArgumentException("未知的ViewModel类: ${modelClass.name}")
    }
}
