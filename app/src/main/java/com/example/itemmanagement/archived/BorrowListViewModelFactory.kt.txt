package com.example.itemmanagement.ui.borrow

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.repository.BorrowRepository

/**
 * BorrowListViewModel的工厂类
 * 用于创建带参数的ViewModel实例
 */
class BorrowListViewModelFactory(
    private val borrowRepository: BorrowRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BorrowListViewModel::class.java)) {
            return BorrowListViewModel(borrowRepository, context) as T
        }
        throw IllegalArgumentException("未知的ViewModel类: ${modelClass.name}")
    }
}
