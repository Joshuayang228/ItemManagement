package com.example.itemmanagement.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.repository.UserProfileRepository

/**
 * ProfileViewModel的工厂类
 */
class ProfileViewModelFactory(
    private val userProfileRepository: UserProfileRepository,
    private val itemRepository: ItemRepository
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(userProfileRepository, itemRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
