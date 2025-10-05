package com.example.itemmanagement.ui.warehouse

import androidx.lifecycle.*
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.data.entity.unified.UnifiedItemEntity
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

class WarehouseViewModel(private val repository: UnifiedItemRepository) : ViewModel() {
    
    // 筛选状态
    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()
    
    // 仓库物品列表 - 使用完整查询方法（包含位置、标签、照片）
    private val _warehouseItems = MutableStateFlow<List<WarehouseItem>>(emptyList())
    val warehouseItems: StateFlow<List<WarehouseItem>> = _warehouseItems.asStateFlow()
    
    init {
        // 监听筛选状态变化
        viewModelScope.launch {
            _filterState
                .debounce(300) // 添加防抖，避免频繁查询
                .collect { state ->
                    loadWarehouseItems()
                }
        }
        loadFilterOptions()
    }
    
    /**
     * 加载仓库物品数据（包含完整的位置、标签、照片信息）
     */
    private fun loadWarehouseItems() {
        viewModelScope.launch {
            try {
                android.util.Log.d("WarehouseViewModel", "🔄 WarehouseViewModel开始加载仓库物品")
                
                // 1. 获取原始数据
                val allItems = repository.getAllWarehouseItemsWithDetails()
                android.util.Log.d("WarehouseViewModel", "📊 获取到原始仓库物品：${allItems.size}个")
                
                // 2. 获取当前筛选状态
                val currentFilter = _filterState.value
                android.util.Log.d("WarehouseViewModel", "🎯 当前筛选状态: searchTerm='${currentFilter.searchTerm}', sortOption=${currentFilter.sortOption}, sortDirection=${currentFilter.sortDirection}")
                
                // 3. 应用搜索过滤
                val searchFiltered = applySearchFilter(allItems, currentFilter.searchTerm)
                android.util.Log.d("WarehouseViewModel", "🔍 搜索过滤后：${searchFiltered.size}个物品")
                
                // 4. 应用其他筛选条件
                val filtered = applyFilters(searchFiltered, currentFilter)
                android.util.Log.d("WarehouseViewModel", "🎛️ 筛选过滤后：${filtered.size}个物品")
                
                // 5. 应用排序
                val sorted = applySorting(filtered, currentFilter.sortOption, currentFilter.sortDirection)
                android.util.Log.d("WarehouseViewModel", "📊 排序后：${sorted.size}个物品")
                
                _warehouseItems.value = sorted
                android.util.Log.d("WarehouseViewModel", "✅ WarehouseViewModel已更新StateFlow，最终物品数量：${sorted.size}")
                
            } catch (e: Exception) {
                android.util.Log.e("WarehouseViewModel", "❌ WarehouseViewModel加载仓库物品失败", e)
                _errorMessage.value = "加载仓库物品失败：${e.message}"
            }
        }
    }
    
    /**
     * 刷新仓库数据（公共方法）
     */
    fun refreshWarehouseItems() {
        loadWarehouseItems()
    }
    
    /**
     * 应用搜索过滤
     */
    private fun applySearchFilter(items: List<WarehouseItem>, searchTerm: String): List<WarehouseItem> {
        if (searchTerm.isBlank()) {
            return items
        }
        
        val term = searchTerm.trim().lowercase()
        return items.filter { item ->
            item.name.lowercase().contains(term) ||
            item.category?.lowercase()?.contains(term) == true ||
            item.subCategory?.lowercase()?.contains(term) == true ||
            item.brand?.lowercase()?.contains(term) == true ||
            item.locationArea?.lowercase()?.contains(term) == true ||
            item.locationContainer?.lowercase()?.contains(term) == true ||
            item.locationSublocation?.lowercase()?.contains(term) == true ||
            item.tagsList?.lowercase()?.contains(term) == true ||
            item.customNote?.lowercase()?.contains(term) == true
        }
    }
    
    /**
     * 应用筛选条件
     */
    private fun applyFilters(items: List<WarehouseItem>, filter: FilterState): List<WarehouseItem> {
        var filtered = items
        
        // 分类筛选（支持多选）
        if (filter.categories.isNotEmpty()) {
            filtered = filtered.filter { item ->
                filter.categories.contains(item.category)
            }
        } else if (filter.category.isNotBlank()) {
            // 向后兼容单选分类
            filtered = filtered.filter { item ->
                item.category == filter.category
            }
        }
        
        // 子分类筛选
        if (filter.subCategory.isNotBlank()) {
            filtered = filtered.filter { item ->
                item.subCategory == filter.subCategory
            }
        }
        
        // 品牌筛选
        if (filter.brand.isNotBlank()) {
            filtered = filtered.filter { item ->
                item.brand == filter.brand
            }
        }
        
        // 位置区域筛选（支持多选）
        if (filter.locationAreas.isNotEmpty()) {
            filtered = filtered.filter { item ->
                filter.locationAreas.contains(item.locationArea)
            }
        } else if (filter.locationArea.isNotBlank()) {
            // 向后兼容单选位置
            filtered = filtered.filter { item ->
                item.locationArea == filter.locationArea
            }
        }
        
        // 容器筛选
        if (filter.container.isNotBlank()) {
            filtered = filtered.filter { item ->
                item.locationContainer == filter.container
            }
        }
        
        // 子位置筛选
        if (filter.sublocation.isNotBlank()) {
            filtered = filtered.filter { item ->
                item.locationSublocation == filter.sublocation
            }
        }
        
        // 开封状态筛选（支持多选）
        if (filter.openStatuses.isNotEmpty()) {
            filtered = filtered.filter { item ->
                filter.openStatuses.contains(item.openStatus)
            }
        } else if (filter.openStatus != null) {
            // 向后兼容单选开封状态
            filtered = filtered.filter { item ->
                item.openStatus == filter.openStatus
            }
        }
        
        // 评分筛选（支持多选和范围）
        if (filter.ratings.isNotEmpty()) {
            filtered = filtered.filter { item ->
                item.rating?.let { rating ->
                    filter.ratings.contains(rating)
                } ?: false
            }
        } else {
            // 评分范围筛选
            if (filter.minRating != null) {
                filtered = filtered.filter { item ->
                    item.rating?.let { it >= filter.minRating } ?: false
                }
            }
            if (filter.maxRating != null) {
                filtered = filtered.filter { item ->
                    item.rating?.let { it <= filter.maxRating } ?: false
                }
            }
        }
        
        // 季节筛选
        if (filter.seasons.isNotEmpty()) {
            filtered = filtered.filter { item ->
                item.season?.let { itemSeason ->
                    filter.seasons.any { filterSeason ->
                        itemSeason.contains(filterSeason, ignoreCase = true)
                    }
                } ?: false
            }
        }
        
        // 标签筛选
        if (filter.tags.isNotEmpty()) {
            filtered = filtered.filter { item ->
                item.tagsList?.let { itemTags ->
                    filter.tags.any { filterTag ->
                        itemTags.contains(filterTag, ignoreCase = true)
                    }
                } ?: false
            }
        }
        
        // 数量范围筛选
        if (filter.minQuantity != null) {
            filtered = filtered.filter { item ->
                item.quantity >= filter.minQuantity
            }
        }
        if (filter.maxQuantity != null) {
            filtered = filtered.filter { item ->
                item.quantity <= filter.maxQuantity
            }
        }
        
        // 价格范围筛选
        if (filter.minPrice != null) {
            filtered = filtered.filter { item ->
                item.price?.let { it >= filter.minPrice } ?: false
            }
        }
        if (filter.maxPrice != null) {
            filtered = filtered.filter { item ->
                item.price?.let { it <= filter.maxPrice } ?: false
            }
        }
        
        // 日期范围筛选
        // 过期日期范围
        if (filter.expirationStartDate != null || filter.expirationEndDate != null) {
            filtered = filtered.filter { item ->
                item.expirationDate?.let { expirationDate ->
                    val inRange = (filter.expirationStartDate?.let { expirationDate >= it } ?: true) &&
                                 (filter.expirationEndDate?.let { expirationDate <= it } ?: true)
                    inRange
                } ?: false
            }
        }
        
        // 购买日期范围 - WarehouseItem没有purchaseDate字段，暂时跳过
        // TODO: 如果需要购买日期筛选，需要在WarehouseItem中添加purchaseDate字段
        /*
        if (filter.purchaseStartDate != null || filter.purchaseEndDate != null) {
            filtered = filtered.filter { item ->
                item.purchaseDate?.let { purchaseDate ->
                    val inRange = (filter.purchaseStartDate?.let { purchaseDate >= it } ?: true) &&
                                 (filter.purchaseEndDate?.let { purchaseDate <= it } ?: true)
                    inRange
                } ?: false
            }
        }
        */
        
        // 生产日期范围 - WarehouseItem没有productionDate字段，暂时跳过
        // TODO: 如果需要生产日期筛选，需要在WarehouseItem中添加productionDate字段
        /*
        if (filter.productionStartDate != null || filter.productionEndDate != null) {
            filtered = filtered.filter { item ->
                item.productionDate?.let { productionDate ->
                    val inRange = (filter.productionStartDate?.let { productionDate >= it } ?: true) &&
                                 (filter.productionEndDate?.let { productionDate <= it } ?: true)
                    inRange
                } ?: false
            }
        }
        */
        
        return filtered
    }
    
    /**
     * 应用排序逻辑
     */
    private fun applySorting(items: List<WarehouseItem>, sortOption: SortOption, sortDirection: SortDirection): List<WarehouseItem> {
        val sorted = when (sortOption) {
            SortOption.COMPREHENSIVE -> {
                // 综合排序：优先级 评分 > 剩余保质期 > 添加时间
                items.sortedWith(compareByDescending<WarehouseItem> { it.rating ?: 0f }
                    .thenBy { item ->
                        // 剩余保质期计算（天数，越小越紧急）
                        item.expirationDate?.let { expDate ->
                            val currentTime = System.currentTimeMillis()
                            val remainingDays = (expDate - currentTime) / (24 * 60 * 60 * 1000)
                            remainingDays
                        } ?: Long.MAX_VALUE
                    }
                    .thenByDescending { it.addDate ?: 0L })
            }
            SortOption.QUANTITY -> {
                items.sortedBy { it.quantity }
            }
            SortOption.PRICE -> {
                items.sortedBy { it.price ?: 0.0 }
            }
            SortOption.RATING -> {
                items.sortedBy { it.rating ?: 0f }
            }
            SortOption.REMAINING_SHELF_LIFE -> {
                // 剩余保质期排序
                items.sortedBy { item ->
                    item.expirationDate?.let { expDate ->
                        val currentTime = System.currentTimeMillis()
                        (expDate - currentTime) / (24 * 60 * 60 * 1000) // 转换为天数
                    } ?: Long.MAX_VALUE // 没有过期日期的排在最后
                }
            }
            SortOption.UPDATE_TIME -> {
                items.sortedBy { it.addDate ?: 0L }
            }
        }
        
        return if (sortDirection == SortDirection.ASC) {
            sorted
        } else {
            sorted.reversed()
        }
    }

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

    /**
     * 删除物品
     * @param itemId 要删除的物品ID
     */
    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            try {
                val item = repository.getItemById(itemId)
                item?.let {
                    repository.deleteItem(it)
                    _deleteResult.value = true
                    // 删除成功后重新加载数据
                    loadWarehouseItems()
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
     * 更新多选分类列表
     * @param categories 分类名称集合
     */
    fun updateCategories(categories: Set<String>) {
        _filterState.value = _filterState.value.copy(categories = categories)
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
     * 更新多选位置区域列表
     * @param areas 区域名称集合
     */
    fun updateLocationAreas(areas: Set<String>) {
        _filterState.value = _filterState.value.copy(locationAreas = areas)
        // 当区域变化时，重新加载所有选中区域的容器列表
        if (areas.isNotEmpty()) {
            // 加载第一个区域的容器（多选情况下的简化处理）
            loadContainers(areas.first())
        } else {
            loadContainers("")
        }
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