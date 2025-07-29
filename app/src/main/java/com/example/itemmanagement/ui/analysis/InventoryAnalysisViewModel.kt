package com.example.itemmanagement.ui.analysis

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.model.InventoryAnalysisData
import kotlinx.coroutines.launch

class InventoryAnalysisViewModel(private val repository: ItemRepository) : ViewModel() {

    private val _analysisData = MutableLiveData<InventoryAnalysisData>()
    val analysisData: LiveData<InventoryAnalysisData> = _analysisData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadAnalysisData()
    }

    fun loadAnalysisData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val data = repository.getInventoryAnalysisData()
                _analysisData.value = data
                
            } catch (e: Exception) {
                _error.value = "加载数据失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        loadAnalysisData()
    }
} 