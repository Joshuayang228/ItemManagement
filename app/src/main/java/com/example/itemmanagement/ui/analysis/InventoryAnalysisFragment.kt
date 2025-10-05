package com.example.itemmanagement.ui.analysis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.databinding.FragmentInventoryAnalysisBinding
import com.example.itemmanagement.data.model.InventoryAnalysisData
import com.example.itemmanagement.data.model.InventoryStats
import com.example.itemmanagement.data.model.CategoryValue
import com.example.itemmanagement.data.model.LocationValue
import com.example.itemmanagement.data.model.MonthlyTrend
import com.example.itemmanagement.data.model.TagValue
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartAnimationType
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAStyle
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AADataLabels
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartFontWeightType
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AALabels

class InventoryAnalysisFragment : Fragment() {

    private var _binding: FragmentInventoryAnalysisBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InventoryAnalysisViewModel by viewModels {
        InventoryAnalysisViewModelFactory(
            (requireActivity().application as ItemManagementApplication).repository
        )
    }

    // 存储当前数据，用于图表切换
    private var currentData: InventoryAnalysisData? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupRefreshListener()
        setupChartToggleListeners() // 添加图表切换监听器
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.contentContainer.visibility = if (isLoading) View.GONE else View.VISIBLE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.errorText.text = error
                binding.errorText.visibility = View.VISIBLE
                binding.contentContainer.visibility = View.GONE
            } else {
                binding.errorText.visibility = View.GONE
            }
        }

        viewModel.analysisData.observe(viewLifecycleOwner) { data ->
            if (data != null) {
                binding.errorText.visibility = View.GONE
                binding.contentContainer.visibility = View.VISIBLE
                currentData = data
                bindData(data)
            }
        }
    }

    private fun bindData(data: InventoryAnalysisData) {
        // 绑定库存概览统计
        bindInventoryStats(data.inventoryStats)
        
        // 显示默认图表
        displayCategoryChart(data.categoryAnalysis, AAChartType.Pie, isCountMode = true)
        displayLocationChart(data.locationAnalysis, AAChartType.Column, isCountMode = true)
        displayTagChart(data.tagAnalysis, AAChartType.Column, isCountMode = true) // 新增标签分析
        displayTrendChart(data.monthlyTrends, AAChartType.Line)
    }

    private fun bindInventoryStats(stats: InventoryStats) {
        binding.apply {
            // 基础统计
            totalItemsValue.text = stats.totalItems.toString()
            totalValueText.text = "¥${String.format("%.2f", stats.totalValue)}"
            expiringItemsValue.text = stats.expiringItems.toString()
            expiredItemsValue.text = stats.expiredItems.toString()
            lowStockItemsValue.text = stats.lowStockItems.toString()
            
            // 扩展统计
            categoriesCountValue.text = stats.categoriesCount.toString()
            locationsCountValue.text = stats.locationsCount.toString()
            recentlyAddedValue.text = stats.recentlyAddedItems.toString()

            // 设置警告状态颜色
            expiringItemsCard.setCardBackgroundColor(
                if (stats.expiringItems > 0) 
                    requireContext().getColor(com.example.itemmanagement.R.color.status_warning_bg)
                else 
                    requireContext().getColor(com.example.itemmanagement.R.color.card_background)
            )

            expiredItemsCard.setCardBackgroundColor(
                if (stats.expiredItems > 0) 
                    requireContext().getColor(com.example.itemmanagement.R.color.status_error_bg)
                else 
                    requireContext().getColor(com.example.itemmanagement.R.color.card_background)
            )

            lowStockCard.setCardBackgroundColor(
                if (stats.lowStockItems > 0) 
                    requireContext().getColor(com.example.itemmanagement.R.color.status_warning_bg)
                else 
                    requireContext().getColor(com.example.itemmanagement.R.color.card_background)
            )
        }
    }

    private fun displayCategoryChart(
        data: List<CategoryValue>,
        chartType: AAChartType = AAChartType.Pie,
        isCountMode: Boolean = true,
        innerSize: String? = null
    ) {
        if (data.isEmpty()) return
        
        val seriesName = if (isCountMode) "数量" else "金额"
        
        val chartModel = AAChartModel()
            .chartType(chartType)
            .title("标题") // 使用实际文字
            .titleStyle(AAStyle().color("transparent")) // 设置标题为透明
            .subtitle("副标题")
            .subtitleStyle(AAStyle().color("transparent")) // 设置副标题为透明
            .dataLabelsEnabled(chartType == AAChartType.Pie) // 饼图启用数据标签显示百分比
            .legendEnabled(false) // 禁用所有图表的图例
            .yAxisTitle("")
            .backgroundColor("#F8F9FA")
            .animationType(AAChartAnimationType.EaseInOutQuart)
            .colorsTheme(arrayOf("#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FECA57", "#FF9FF3") as Array<Any>)

        val seriesArray = if (chartType == AAChartType.Pie) {
            // 饼图数据格式，配置百分比显示
            val dataValues: List<Number> = if (isCountMode) data.map { it.count } else data.map { it.totalValue }
            val pieData = data.mapIndexed { index, category -> 
                arrayOf(category.category, dataValues[index])
            }.toTypedArray()
            val series = AASeriesElement()
                .name(seriesName)
                .data(pieData as Array<Any>)
                .dataLabels(AADataLabels()
                    .enabled(true)
                    .format("{point.name}: {point.percentage:.1f}%") // 显示类目名和百分比
                    .style(AAStyle()
                        .fontWeight(AAChartFontWeightType.Regular) // 取消加粗
                        .color("#333333") // 设置文字颜色
                    )
                    .backgroundColor("transparent") // 移除白色背景
                    .borderWidth(0) // 移除边框
                )
            
            if (innerSize != null) {
                series.innerSize(innerSize)
            }
            
            arrayOf(series) as Array<Any>
        } else {
            // 柱状图数据格式
            val dataValues: List<Number> = if (isCountMode) data.map { it.count } else data.map { it.totalValue }
            arrayOf(
                AASeriesElement()
                    .name(seriesName)
                    .data(dataValues.toTypedArray() as Array<Any>)
            ) as Array<Any>
        }

        chartModel.series(seriesArray)
        
        if (chartType != AAChartType.Pie) {
            chartModel.categories(data.map { it.category }.toTypedArray())
        }

        binding.categoryAnalysisChart.aa_drawChartWithChartModel(chartModel)
    }

    private fun displayLocationChart(
        data: List<LocationValue>,
        chartType: AAChartType = AAChartType.Column,
        isCountMode: Boolean = true
    ) {
        if (data.isEmpty()) return
        
        val seriesName = if (isCountMode) "数量" else "金额"
        val dataValues: List<Number> = if (isCountMode) data.map { it.count } else data.map { it.totalValue }
        
        val chartModel = AAChartModel()
            .chartType(chartType)
            .title("标题") // 使用实际文字
            .titleStyle(AAStyle().color("transparent")) // 设置标题为透明
            .subtitle("副标题")
            .subtitleStyle(AAStyle().color("transparent")) // 设置副标题为透明
            .dataLabelsEnabled(false)
            .legendEnabled(false)
            .yAxisTitle("")
            .backgroundColor("#F1F2F6")
            .animationType(AAChartAnimationType.EaseInOutQuart)
            .categories(data.map { it.location }.toTypedArray())
            .colorsTheme(arrayOf("#3742FA", "#2F3542", "#FF3838", "#FF6348", "#2ED573") as Array<Any>)

        // 为横向柱状图设置适当的高度和间距
        if (chartType == AAChartType.Bar) {
            chartModel.margin(arrayOf(80, 80, 80, 120) as Array<Number>) // 增加边距确保标签显示完整
        }

        chartModel.series(arrayOf(
                AASeriesElement()
                    .name(seriesName)
                    .data(dataValues.toTypedArray() as Array<Any>)
            ) as Array<Any>)

        binding.locationAnalysisChart.aa_drawChartWithChartModel(chartModel)
    }

    private fun displayTagChart(
        data: List<TagValue>,
        chartType: AAChartType = AAChartType.Column,
        isCountMode: Boolean = true
    ) {
        if (data.isEmpty()) return
        
        val seriesName = if (isCountMode) "数量" else "金额"
        
        val chartModel = AAChartModel()
            .chartType(chartType)
            .title("标题") // 使用实际文字
            .titleStyle(AAStyle().color("transparent")) // 设置标题为透明
            .subtitle("副标题")
            .subtitleStyle(AAStyle().color("transparent")) // 设置副标题为透明
            .dataLabelsEnabled(chartType == AAChartType.Pie) // 饼图启用数据标签显示百分比
            .legendEnabled(false)
            .yAxisTitle("")
            .backgroundColor("#F0F8FF")
            .animationType(AAChartAnimationType.EaseInOutQuart)
            .colorsTheme(arrayOf("#FF6B9D", "#4ECDC4", "#45B7D1", "#96CEB4", "#FECA57", "#FF9FF3", "#6C5CE7", "#A8E6CF") as Array<Any>)

        val seriesArray = if (chartType == AAChartType.Pie) {
            // 饼图数据格式，配置百分比显示
            val dataValues: List<Number> = if (isCountMode) data.map { it.count } else data.map { it.totalValue }
            val pieData = data.mapIndexed { index, tag -> 
                arrayOf(tag.tag, dataValues[index])
            }.toTypedArray()
            val series = AASeriesElement()
                .name(seriesName)
                .data(pieData as Array<Any>)
                .dataLabels(AADataLabels()
                    .enabled(true)
                    .format("{point.name}: {point.percentage:.1f}%") // 显示标签名和百分比
                    .style(AAStyle()
                        .fontWeight(AAChartFontWeightType.Regular) // 取消加粗
                        .color("#333333") // 设置文字颜色
                    )
                    .backgroundColor("transparent") // 移除白色背景
                    .borderWidth(0) // 移除边框
                )
            
            arrayOf(series) as Array<Any>
        } else {
            // 柱状图数据格式  
            val dataValues: List<Number> = if (isCountMode) data.map { it.count } else data.map { it.totalValue }
            chartModel.categories(data.map { it.tag }.toTypedArray())
            arrayOf(
                AASeriesElement()
                    .name(seriesName)
                    .data(dataValues.toTypedArray() as Array<Any>)
            ) as Array<Any>
        }
        
        chartModel.series(seriesArray)
        binding.tagAnalysisChart.aa_drawChartWithChartModel(chartModel)
    }

    private fun displayTrendChart(
        data: List<MonthlyTrend>, 
        chartType: AAChartType = AAChartType.Line
    ) {
        if (data.isEmpty()) return
        
        val sortedData = data.sortedBy { it.month }
        val categories = sortedData.map { 
            if (it.month.length >= 7) it.month.substring(5) else it.month 
        }.toTypedArray()
        
        val chartModel = AAChartModel()
            .chartType(chartType)
            .title("标题") // 使用实际文字
            .titleStyle(AAStyle().color("transparent")) // 设置标题为透明
            .subtitle("副标题")
            .subtitleStyle(AAStyle().color("transparent")) // 设置副标题为透明
            .dataLabelsEnabled(false)
            .backgroundColor("#F5F3FF")
            .animationType(AAChartAnimationType.EaseInOutQuart)
            .categories(categories)
            .colorsTheme(arrayOf("#8B5CF6", "#06FFA5", "#FFDD59") as Array<Any>)
            
        val seriesArray = when (chartType) {
            AAChartType.Line -> arrayOf(
                AASeriesElement()
                    .name("新增物品")
                    .data(sortedData.map { it.count }.toTypedArray() as Array<Any>),
                AASeriesElement()
                    .name("累计价值")
                    .data(sortedData.map { it.totalValue }.toTypedArray() as Array<Any>)
            ) as Array<Any>
            AAChartType.Area -> arrayOf(
                AASeriesElement()
                    .name("新增物品")
                    .data(sortedData.map { it.count }.toTypedArray() as Array<Any>)
            ) as Array<Any>
            else -> arrayOf(
                AASeriesElement()
                    .name("新增物品")
                    .data(sortedData.map { it.count }.toTypedArray() as Array<Any>)
            ) as Array<Any>
        }
        
        chartModel.series(seriesArray)
        binding.trendAnalysisChart.aa_drawChartWithChartModel(chartModel)
    }

    private fun setupRefreshListener() {
        // 添加错误重试功能
        binding.errorText.setOnClickListener {
            viewModel.refresh()
        }
    }

    // 存储当前选择的数据类型
    private var categoryIsCountMode = true
    private var locationIsCountMode = true  
    private var tagIsCountMode = true

    private fun setupChartToggleListeners() {
        // 分类分析图表类型切换
        binding.categoryChartTypeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && currentData != null) {
                val data = currentData!!.categoryAnalysis
                when (checkedId) {
                    binding.categoryPieButton.id -> {
                        displayCategoryChart(data, AAChartType.Pie, categoryIsCountMode)
                    }
                    binding.categoryBarButton.id -> {
                        displayCategoryChart(data, AAChartType.Column, categoryIsCountMode)
                    }
                }
            }
        }
        
        // 分类分析数据类型切换
        binding.categoryDataTypeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && currentData != null) {
                categoryIsCountMode = checkedId == binding.categoryCountButton.id
                val selectedChartType = getCurrentCategoryChartType()
                displayCategoryChart(currentData!!.categoryAnalysis, selectedChartType, categoryIsCountMode)
            }
        }
        
        // 位置分析图表类型切换
        binding.locationChartTypeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && currentData != null) {
                val data = currentData!!.locationAnalysis
                when (checkedId) {
                    binding.locationBarButton.id -> {
                        displayLocationChart(data, AAChartType.Column, locationIsCountMode)
                    }
                    binding.locationHorizontalBarButton.id -> {
                        displayLocationChart(data, AAChartType.Bar, locationIsCountMode)
                    }
                }
            }
        }
        
        // 位置分析数据类型切换
        binding.locationDataTypeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && currentData != null) {
                locationIsCountMode = checkedId == binding.locationCountButton.id
                val selectedChartType = getCurrentLocationChartType()
                displayLocationChart(currentData!!.locationAnalysis, selectedChartType, locationIsCountMode)
            }
        }
        
        // 标签分析图表类型切换
        binding.tagChartTypeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && currentData != null) {
                val data = currentData!!.tagAnalysis
                when (checkedId) {
                    binding.tagBarButton.id -> {
                        displayTagChart(data, AAChartType.Column, tagIsCountMode)
                    }
                    binding.tagPieButton.id -> {
                        displayTagChart(data, AAChartType.Pie, tagIsCountMode)
                    }
                }
            }
        }
        
        // 标签分析数据类型切换
        binding.tagDataTypeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && currentData != null) {
                tagIsCountMode = checkedId == binding.tagCountButton.id
                val selectedChartType = getCurrentTagChartType()
                displayTagChart(currentData!!.tagAnalysis, selectedChartType, tagIsCountMode)
            }
        }
        
        // 趋势分析图表切换
        binding.trendChartTypeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && currentData != null) {
                val data = currentData!!.monthlyTrends
                when (checkedId) {
                    binding.trendLineButton.id -> {
                        displayTrendChart(data, AAChartType.Line)
                    }
                    binding.trendAreaButton.id -> {
                        displayTrendChart(data, AAChartType.Area)
                    }
                    binding.trendBarButton.id -> {
                        displayTrendChart(data, AAChartType.Column)
                    }
                }
            }
        }
    }
    
    private fun getCurrentCategoryChartType(): AAChartType {
        return when (binding.categoryChartTypeToggle.checkedButtonId) {
            binding.categoryPieButton.id -> AAChartType.Pie
            binding.categoryBarButton.id -> AAChartType.Column
            else -> AAChartType.Pie
        }
    }
    
    private fun getCurrentLocationChartType(): AAChartType {
        return when (binding.locationChartTypeToggle.checkedButtonId) {
            binding.locationBarButton.id -> AAChartType.Column
            binding.locationHorizontalBarButton.id -> AAChartType.Bar
            else -> AAChartType.Column
        }
    }
    
    private fun getCurrentTagChartType(): AAChartType {
        return when (binding.tagChartTypeToggle.checkedButtonId) {
            binding.tagBarButton.id -> AAChartType.Column
            binding.tagPieButton.id -> AAChartType.Pie
            else -> AAChartType.Column
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}