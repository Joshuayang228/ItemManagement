package com.example.itemmanagement.ui.export

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.data.repository.BorrowRepository
import com.example.itemmanagement.data.repository.WarrantyRepository
import com.example.itemmanagement.export.CSVExporter
import com.example.itemmanagement.export.ExportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 数据导出ViewModel
 * 负责协调数据获取和导出逻辑
 */
class DataExportViewModel(
    application: Application,
    private val itemRepository: UnifiedItemRepository,
    private val warrantyRepository: WarrantyRepository? = null,
    private val borrowRepository: BorrowRepository? = null
) : AndroidViewModel(application) {
    
    private val exportManager = ExportManager(getApplication<Application>())
    
    // ==================== LiveData ====================
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _exportResult = MutableLiveData<ExportResult>()
    val exportResult: LiveData<ExportResult> = _exportResult
    
    private val _dataStatistics = MutableLiveData<DataStatistics>()
    val dataStatistics: LiveData<DataStatistics> = _dataStatistics
    
    // ==================== 数据类 ====================
    
    /**
     * 导出结果
     */
    data class ExportResult(
        val success: Boolean,
        val message: String,
        val exportedFiles: List<ExportManager.ExportResult> = emptyList()
    )
    
    /**
     * 数据统计
     */
    data class DataStatistics(
        val itemCount: Int = 0,
        val warrantyCount: Int = 0,
        val borrowCount: Int = 0,
        val shoppingCount: Int = 0,
        val hasData: Boolean = false
    ) {
        val totalCount: Int get() = itemCount + warrantyCount + borrowCount + shoppingCount
    }
    
    // ==================== 初始化 ====================
    
    init {
        loadDataStatistics()
    }
    
    // ==================== 公共方法 ====================
    
    /**
     * 导出所有数据
     */
    fun exportAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val result = performFullExport()
                _exportResult.value = result
                
            } catch (e: Exception) {
                _exportResult.value = ExportResult(
                    success = false,
                    message = "导出失败: ${e.message}"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 导出物品清单
     */
    fun exportItems() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val items = itemRepository.getAllItemsWithDetails().first()
                val locations = itemRepository.getAllLocationsSync()
                val locationsMap = locations.associateBy { it.id }
                
                val csvContent = CSVExporter.exportItems(items, locationsMap)
                val result = exportManager.saveCSVFile(csvContent, ExportManager.ExportType.ITEMS)
                
                _exportResult.value = ExportResult(
                    success = result.success,
                    message = result.message,
                    exportedFiles = listOf(result)
                )
                
            } catch (e: Exception) {
                _exportResult.value = ExportResult(
                    success = false,
                    message = "导出物品清单失败: ${e.message}"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 导出购物清单
     */
    fun exportShoppingList() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val shoppingItems = itemRepository.getAllShoppingItemsWithUnifiedItem()
                val csvContent = CSVExporter.exportShoppingList(shoppingItems)
                val result = exportManager.saveCSVFile(csvContent, ExportManager.ExportType.SHOPPING)
                
                _exportResult.value = ExportResult(
                    success = result.success,
                    message = result.message,
                    exportedFiles = listOf(result)
                )
                
            } catch (e: Exception) {
                _exportResult.value = ExportResult(
                    success = false,
                    message = "导出购物清单失败: ${e.message}"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 导出保修记录
     */
    fun exportWarranties() {
        if (warrantyRepository == null) {
            _exportResult.value = ExportResult(false, "保修功能未启用")
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // 获取所有保修记录 
                val warranties = warrantyRepository.getAllWarrantiesStream().first()
                // 为了简化，暂时导出基础保修信息，后续可以扩展
                val csvContent = CSVExporter.exportBasicWarranties(warranties)
                val result = exportManager.saveCSVFile(csvContent, ExportManager.ExportType.WARRANTIES)
                
                _exportResult.value = ExportResult(
                    success = result.success,
                    message = result.message,
                    exportedFiles = listOf(result)
                )
                
            } catch (e: Exception) {
                _exportResult.value = ExportResult(
                    success = false,
                    message = "导出保修记录失败: ${e.message}"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 导出借还记录
     */
    fun exportBorrows() {
        if (borrowRepository == null) {
            _exportResult.value = ExportResult(false, "借还管理功能未启用")
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val borrows = borrowRepository.getAllBorrowsWithItemInfoFlow().first()
                val csvContent = CSVExporter.exportBorrows(borrows)
                val result = exportManager.saveCSVFile(csvContent, ExportManager.ExportType.BORROWS)
                
                _exportResult.value = ExportResult(
                    success = result.success,
                    message = result.message,
                    exportedFiles = listOf(result)
                )
                
            } catch (e: Exception) {
                _exportResult.value = ExportResult(
                    success = false,
                    message = "导出借还记录失败: ${e.message}"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 分享导出文件
     */
    fun shareExportedFile(exportResult: ExportManager.ExportResult, exportType: ExportManager.ExportType) {
        exportResult.fileUri?.let { uri ->
            exportManager.shareCSVFile(uri, exportType)
        }
    }
    
    /**
     * 刷新数据统计
     */
    fun refreshStatistics() {
        loadDataStatistics()
    }
    
    /**
     * 清除导出结果
     */
    fun clearExportResult() {
        _exportResult.value = null
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 加载数据统计
     */
    private fun loadDataStatistics() {
        viewModelScope.launch {
            try {
                val itemCount = withContext(Dispatchers.IO) {
                    itemRepository.getAllItemsWithDetails().first().size
                }
                
                val warrantyCount = warrantyRepository?.let {
                    withContext(Dispatchers.IO) {
                        it.getAllWarrantiesStream().first().size
                    }
                } ?: 0
                
                val borrowCount = borrowRepository?.let {
                    withContext(Dispatchers.IO) {
                        it.getAllBorrowsWithItemInfoFlow().first().size
                    }
                } ?: 0
                
                val shoppingCount = withContext(Dispatchers.IO) {
                    try {
                        itemRepository.getAllShoppingDetails().first().size
                    } catch (e: Exception) {
                        0
                    }
                }
                
                _dataStatistics.value = DataStatistics(
                    itemCount = itemCount,
                    warrantyCount = warrantyCount,
                    borrowCount = borrowCount,
                    shoppingCount = shoppingCount,
                    hasData = itemCount > 0 || warrantyCount > 0 || borrowCount > 0 || shoppingCount > 0
                )
                
            } catch (e: Exception) {
                _dataStatistics.value = DataStatistics()
            }
        }
    }
    
    /**
     * 执行完整导出
     */
    private suspend fun performFullExport(): ExportResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<ExportManager.ExportResult>()
        
        // 获取所有数据
        val items = itemRepository.getAllItemsWithDetails().first()
        val warranties = warrantyRepository?.getAllWarrantiesStream()?.first() ?: emptyList()
        val borrows = borrowRepository?.getAllBorrowsWithItemInfoFlow()?.first() ?: emptyList()
        val shoppingItems = try {
            itemRepository.getAllShoppingItemsWithUnifiedItem()
        } catch (e: Exception) {
            emptyList()
        }
        
        // 获取位置信息
        val locations = itemRepository.getAllLocationsSync()
        val locationsMap = locations.associateBy { it.id }
        
        // 生成CSV内容
        val itemsContent = CSVExporter.exportItems(items, locationsMap)
        val warrantiesContent = CSVExporter.exportWarranties(warranties)
        val borrowsContent = CSVExporter.exportBorrows(borrows)
        val shoppingContent = CSVExporter.exportShoppingList(shoppingItems)
        val summaryContent = CSVExporter.exportSummary(items.size, warranties.size, borrows.size, shoppingItems.size)
        
        // 批量导出
        val exportResults = exportManager.exportAllData(
            itemsContent, warrantiesContent, borrowsContent, shoppingContent, summaryContent
        )
        
        results.addAll(exportResults)
        
        val successCount = results.count { it.success }
        val totalCount = results.size
        
        ExportResult(
            success = successCount > 0,
            message = if (successCount == totalCount) {
                "全部数据导出成功！共导出 ${totalCount} 个文件"
            } else {
                "部分导出成功：${successCount}/${totalCount} 个文件"
            },
            exportedFiles = results
        )
    }
}
