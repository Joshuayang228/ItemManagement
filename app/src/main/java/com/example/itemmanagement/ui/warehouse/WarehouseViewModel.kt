package com.example.itemmanagement.ui.warehouse

import androidx.lifecycle.*
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.entity.ItemEntity
import com.example.itemmanagement.data.model.Item
import com.example.itemmanagement.data.model.WarehouseItem
import com.example.itemmanagement.data.mapper.toItemEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WarehouseViewModel(private val repository: ItemRepository) : ViewModel() {
    
    // 筛选状态
    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()
    
    // 仓库物品列表 - 使用新的查询方法
    val warehouseItems: StateFlow<List<WarehouseItem>> = _filterState
        .debounce(300) // 添加防抖，避免频繁查询
        .flatMapLatest { state ->
            try {
                repository.getWarehouseItems(state)
            } catch (e: Exception) {
                throw e
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
    
    // 删除结果
    private val _deleteResult = MutableLiveData<Boolean>()
    val deleteResult: LiveData<Boolean> = _deleteResult

    // 错误信息
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    // 分类列表（用于筛选器）
    private val _categories = MutableLiveData<List<String>>()
    val categories: LiveData<List<String>> = _categories
    
    // 子分类列表
    private val _subCategories = MutableLiveData<List<String>>()
    val subCategories: LiveData<List<String>> = _subCategories
    
    // 品牌列表
    private val _brands = MutableLiveData<List<String>>()
    val brands: LiveData<List<String>> = _brands
    
    // 位置区域列表
    private val _locationAreas = MutableLiveData<List<String>>()
    val locationAreas: LiveData<List<String>> = _locationAreas
    
    // 容器列表
    private val _containers = MutableLiveData<List<String>>()
    val containers: LiveData<List<String>> = _containers
    
    // 子位置列表
    private val _sublocations = MutableLiveData<List<String>>()
    val sublocations: LiveData<List<String>> = _sublocations
    
    // 初始化
    init {
        loadFilterOptions()
    }

    /**
     * 删除物品
     * @param itemId 要删除的物品ID
     */
    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            try {
                val item = repository.getItemById(itemId)
                item?.let {
                    repository.deleteItem(it.toItemEntity())
                    _deleteResult.value = true
                } ?: run {
                    _errorMessage.value = "找不到要删除的物品"
                    _deleteResult.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "删除失败：${e.message}"
                _deleteResult.value = false
            }
        }
    }
    
    /**
     * 设置搜索词
     * @param term 搜索词
     */
    fun setSearchTerm(term: String) {
        _filterState.value = _filterState.value.copy(searchTerm = term)
    }
    
    /**
     * 设置分类
     * @param category 分类名称
     */
    fun setCategory(category: String) {
        _filterState.value = _filterState.value.copy(category = category)
    }
    
    /**
     * 设置子分类
     * @param subCategory 子分类名称
     */
    fun setSubCategory(subCategory: String) {
        _filterState.value = _filterState.value.copy(subCategory = subCategory)
    }
    
    /**
     * 设置品牌
     * @param brand 品牌名称
     */
    fun setBrand(brand: String) {
        _filterState.value = _filterState.value.copy(brand = brand)
    }
    
    /**
     * 设置位置区域
     * @param area 区域名称
     */
    fun setLocationArea(area: String) {
        _filterState.value = _filterState.value.copy(locationArea = area)
        // 当区域变化时，加载该区域的容器列表
        loadContainers(area)
    }
    
    /**
     * 设置容器
     * @param container 容器名称
     */
    fun setContainer(container: String) {
        _filterState.value = _filterState.value.copy(container = container)
        // 当容器变化时，加载该区域和容器的子位置列表
        loadSublocations(_filterState.value.locationArea, container)
    }
    
    /**
     * 设置排序选项
     * @param option 排序选项
     */
    fun setSortOption(option: SortOption) {
        _filterState.value = _filterState.value.copy(sortOption = option)
    }
    
    /**
     * 设置排序方向
     * @param direction 排序方向
     */
    fun setSortDirection(direction: SortDirection) {
        _filterState.value = _filterState.value.copy(sortDirection = direction)
    }
    
    /**
     * 重置筛选
     */
    fun resetFilter() {
        _filterState.value = FilterState()
    }
    
    /**
     * 更新筛选状态
     * @param filter 筛选状态
     */
    fun updateFilterState(filter: FilterState) {
        _filterState.value = filter
    }
    
    /**
     * 加载筛选选项
     */
    fun loadFilterOptions() {
        viewModelScope.launch {
            try {
                val categories = repository.getAllCategories()
                _categories.value = categories
                
                val subCategories = repository.getAllSubCategories()
                _subCategories.value = subCategories
                
                val brands = repository.getAllBrands()
                _brands.value = brands
                
                val locationAreas = repository.getAllLocationAreas()
                _locationAreas.value = locationAreas
            } catch (e: Exception) {
                _errorMessage.value = "加载筛选选项失败：${e.message}"
            }
        }
    }
    
    /**
     * 根据区域加载容器列表
     * @param area 区域名称
     */
    fun loadContainers(area: String) {
        if (area.isBlank()) {
            _containers.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            try {
                val containers = repository.getContainersByArea(area)
                _containers.value = containers
            } catch (e: Exception) {
                _errorMessage.value = "加载容器列表失败：${e.message}"
            }
        }
    }
    
    /**
     * 根据区域和容器加载子位置列表
     * @param area 区域名称
     * @param container 容器名称
     */
    fun loadSublocations(area: String, container: String) {
        if (area.isBlank() || container.isBlank()) {
            _sublocations.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            try {
                val sublocations = repository.getSublocations(area, container)
                _sublocations.value = sublocations
            } catch (e: Exception) {
                _errorMessage.value = "加载子位置列表失败：${e.message}"
            }
        }
    }
} 