package com.example.itemmanagement.ui.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.repository.ReminderSettingsRepository

class ReminderSettingsViewModelFactory(
    private val repository: ReminderSettingsRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReminderSettingsViewModel::class.java)) {
            return ReminderSettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
