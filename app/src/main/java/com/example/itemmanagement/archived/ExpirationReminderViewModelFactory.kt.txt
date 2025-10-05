package com.example.itemmanagement.ui.expiration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.repository.ReminderSettingsRepository
import com.example.itemmanagement.reminder.ReminderManager

class ExpirationReminderViewModelFactory(
    private val repository: ItemRepository,
    private val settingsRepository: ReminderSettingsRepository,
    private val reminderManager: ReminderManager
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpirationReminderViewModel::class.java)) {
            return ExpirationReminderViewModel(
                repository, 
                settingsRepository, 
                reminderManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
