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
    
    // 可用标签列表
    private val _availableTags = MutableLiveData<List<String>>()
    val availableTags: LiveData<List<String>> = _availableTags
    
    private val _availableSeasons = MutableLiveData<List<String>>()
    val availableSeasons: LiveData<List<String>> = _availableSeasons
    
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
     * 设置子位置
     * @param sublocation 子位置名称
     */
    fun setSublocation(sublocation: String) {
        _filterState.value = _filterState.value.copy(sublocation = sublocation)
    }
    
    /**
     * 设置开封状态
     * @param openStatus 开封状态，true为已开封，false为未开封，null为不限制
     */
    fun updateOpenStatus(openStatus: Boolean?) {
        _filterState.value = _filterState.value.copy(openStatus = openStatus)
    }

    /**
     * 更新多选开封状态
     * @param openStatuses 选中的开封状态集合
     */
    fun updateOpenStatuses(openStatuses: Set<Boolean>) {
        _filterState.value = _filterState.value.copy(openStatuses = openStatuses)
    }
    
    /**
     * 设置最低评分
     * @param minRating 最低评分
     */
    fun updateMinRating(minRating: Float?) {
        _filterState.value = _filterState.value.copy(minRating = minRating)
    }
    
    /**
     * 设置评分范围
     * @param minRating 最低评分
     * @param maxRating 最高评分
     */
    fun updateRatingRange(minRating: Float?, maxRating: Float?) {
        _filterState.value = _filterState.value.copy(
            minRating = minRating,
            maxRating = maxRating
        )
    }

    /**
     * 更新多选评分
     * @param ratings 选中的评分集合
     */
    fun updateRatings(ratings: Set<Float>) {
        _filterState.value = _filterState.value.copy(ratings = ratings)
    }
    
    /**
     * 设置季节筛选
     * @param seasons 选中的季节集合
     */
    fun updateSeasons(seasons: Set<String>) {
        _filterState.value = _filterState.value.copy(seasons = seasons)
    }
    
    /**
     * 设置标签筛选
     * @param tags 选中的标签集合
     */
    fun updateTags(tags: Set<String>) {
        _filterState.value = _filterState.value.copy(tags = tags)
    }
    
    /**
     * 设置数量范围
     * @param minQuantity 最小数量
     * @param maxQuantity 最大数量
     */
    fun updateQuantityRange(minQuantity: Int?, maxQuantity: Int?) {
        _filterState.value = _filterState.value.copy(
            minQuantity = minQuantity,
            maxQuantity = maxQuantity
        )
    }
    
    /**
     * 设置价格范围
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     */
    fun updatePriceRange(minPrice: Double?, maxPrice: Double?) {
        _filterState.value = _filterState.value.copy(
            minPrice = minPrice,
            maxPrice = maxPrice
        )
    }
    
    /**
     * 设置日期类型 - 保持兼容性，已弃用
     * @param dateType 日期类型
     */
    @Deprecated("使用具体的日期范围更新方法")
    fun updateDateType(dateType: DateType?) {
        // 兼容性实现，不做实际操作
    }
    
    /**
     * 设置日期范围 - 保持兼容性，已弃用
     * @param startDate 开始日期时间戳
     * @param endDate 结束日期时间戳
     */
    @Deprecated("使用具体的日期范围更新方法")
    fun updateDateRange(startDate: Long?, endDate: Long?) {
        // 兼容性实现，更新过期日期范围
        updateExpirationDateRange(startDate, endDate)
    }
    
    /**
     * 更新过期日期范围
     * @param startDate 开始日期时间戳
     * @param endDate 结束日期时间戳
     */
    fun updateExpirationDateRange(startDate: Long?, endDate: Long?) {
        _filterState.value = _filterState.value.copy(
            expirationStartDate = startDate,
            expirationEndDate = endDate
        )
    }
    
    /**
     * 更新添加日期范围
     * @param startDate 开始日期时间戳
     * @param endDate 结束日期时间戳
     */
    fun updateCreationDateRange(startDate: Long?, endDate: Long?) {
        _filterState.value = _filterState.value.copy(
            creationStartDate = startDate,
            creationEndDate = endDate
        )
    }
    
    /**
     * 更新购买日期范围
     * @param startDate 开始日期时间戳
     * @param endDate 结束日期时间戳
     */
    fun updatePurchaseDateRange(startDate: Long?, endDate: Long?) {
        _filterState.value = _filterState.value.copy(
            purchaseStartDate = startDate,
            purchaseEndDate = endDate
        )
    }
    
    /**
     * 更新生产日期范围
     * @param startDate 开始日期时间戳
     * @param endDate 结束日期时间戳
     */
    fun updateProductionDateRange(startDate: Long?, endDate: Long?) {
        _filterState.value = _filterState.value.copy(
            productionStartDate = startDate,
            productionEndDate = endDate
        )
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
    fun resetFilters() {
        _filterState.value = FilterState()
    }
    
    /**
     * 重置筛选（保持兼容性）
     */
    fun resetFilter() {
        resetFilters()
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
                
                val tags = repository.getAllTags()
                _availableTags.value = tags
                
                val seasons = repository.getAllSeasons()
                _availableSeasons.value = seasons
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