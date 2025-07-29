package com.example.itemmanagement.ui.waste

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.adapter.WastedItemAdapter
import com.example.itemmanagement.adapter.WasteInsightAdapter
import com.example.itemmanagement.data.model.*
import com.example.itemmanagement.databinding.FragmentWasteReportBinding
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartAnimationType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartZoomType
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class WasteReportFragment : Fragment() {

    private var _binding: FragmentWasteReportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WasteReportViewModel by viewModels {
        WasteReportViewModelFactory(
            (requireActivity().application as ItemManagementApplication).repository
        )
    }

    private lateinit var wastedItemAdapter: WastedItemAdapter
    private lateinit var insightAdapter: WasteInsightAdapter
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)

    private var customStartDate: Date? = null
    private var customEndDate: Date? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWasteReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupObservers()
        
        // 先检查并更新过期物品状态，然后加载报告
        viewModel.checkExpiredItemsAndGenerateReport(ReportPeriodType.LAST_MONTH)
    }

    private fun setupUI() {
        // 调试按钮：手动更新过期物品状态
        binding.buttonUpdateExpired.setOnClickListener {
            // 先调试检查数据库状态
            debugDatabaseStatus()
            // 然后更新和生成报告
            viewModel.checkExpiredItemsAndGenerateReport(ReportPeriodType.LAST_MONTH)
        }

        // 设置浪费物品列表
        wastedItemAdapter = WastedItemAdapter { wastedItem ->
            // 点击浪费物品，可以跳转到物品详情页面
            // TODO: 实现跳转到物品详情
        }
        
        binding.recyclerViewWastedItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = wastedItemAdapter
        }

        // 设置智能洞察列表
        insightAdapter = WasteInsightAdapter()
        binding.recyclerViewInsights.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = insightAdapter
        }

        // 设置时间范围选择器
        setupPeriodSelector()
    }

    private fun setupPeriodSelector() {
        with(binding) {
            // 设置默认选中上个月
            chipLastMonth.isChecked = true

            chipGroupPeriod.setOnCheckedStateChangeListener { _, checkedIds ->
                if (checkedIds.isNotEmpty()) {
                    when (checkedIds[0]) {
                        R.id.chipLastWeek -> {
                            hideCustomDateRange()
                            viewModel.checkExpiredItemsAndGenerateReport(ReportPeriodType.LAST_WEEK)
                        }
                        R.id.chipLastMonth -> {
                            hideCustomDateRange()
                            viewModel.checkExpiredItemsAndGenerateReport(ReportPeriodType.LAST_MONTH)
                        }
                        R.id.chipLastThreeMonths -> {
                            hideCustomDateRange()
                            viewModel.checkExpiredItemsAndGenerateReport(ReportPeriodType.LAST_THREE_MONTHS)
                        }
                        R.id.chipPastYear -> {
                            hideCustomDateRange()
                            viewModel.checkExpiredItemsAndGenerateReport(ReportPeriodType.PAST_YEAR)
                        }
                        R.id.chipThisYear -> {
                            hideCustomDateRange()
                            viewModel.checkExpiredItemsAndGenerateReport(ReportPeriodType.THIS_YEAR)
                        }
                        R.id.chipCustom -> {
                            showCustomDateRange()
                        }
                    }
                }
            }

            // 设置自定义日期选择
            buttonStartDate.setOnClickListener { showDatePicker(true) }
            buttonEndDate.setOnClickListener { showDatePicker(false) }
        }
    }

    private fun showCustomDateRange() {
        binding.layoutCustomDateRange.visibility = View.VISIBLE
        
        // 如果还没有设置自定义日期，使用默认值
        if (customStartDate == null || customEndDate == null) {
            val calendar = Calendar.getInstance()
            customEndDate = calendar.time
            calendar.add(Calendar.MONTH, -1)
            customStartDate = calendar.time
            
            updateCustomDateButtons()
        }
    }

    private fun hideCustomDateRange() {
        binding.layoutCustomDateRange.visibility = View.GONE
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        val currentDate = if (isStartDate) customStartDate else customEndDate
        currentDate?.let { calendar.time = it }

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                val selectedDate = selectedCalendar.time

                if (isStartDate) {
                    customStartDate = selectedDate
                } else {
                    customEndDate = selectedDate
                }

                updateCustomDateButtons()

                // 如果两个日期都已设置，生成报告
                if (customStartDate != null && customEndDate != null) {
                    val dateRange = DateRange(customStartDate!!, customEndDate!!)
                    viewModel.checkExpiredItemsAndGenerateReport(ReportPeriodType.CUSTOM, dateRange)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateCustomDateButtons() {
        customStartDate?.let {
            binding.buttonStartDate.text = dateFormat.format(it)
        }
        customEndDate?.let {
            binding.buttonEndDate.text = dateFormat.format(it)
        }
    }

    private fun setupObservers() {
        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // 观察错误消息
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAction("重试") { viewModel.refreshReport() }
                    .show()
                viewModel.clearError()
            }
        }

        // 观察浪费报告数据
        viewModel.wasteReportData.observe(viewLifecycleOwner) { wasteReportData ->
            if (wasteReportData != null) {
                displayWasteReport(wasteReportData)
            } else {
                showEmptyState()
            }
        }

        // 观察智能洞察数据
        viewModel.insights.observe(viewLifecycleOwner) { insights ->
            if (insights.isNotEmpty()) {
                insightAdapter.submitList(insights)
            }
        }
    }

    private fun displayWasteReport(data: WasteReportData) {
        binding.layoutReportContent.visibility = View.VISIBLE
        binding.layoutEmptyState.visibility = View.GONE

        // 更新统计概览
        updateSummaryCards(data)

        // 检查是否有数据，控制图表可见性
        val hasData = data.totalWastedItems > 0
        
        if (hasData) {
            // 更新图表
            updateCategoryChart(data.wasteByCategory)
            updateTrendChart(data.wasteByTime)
            updateReasonChart(data)
            
            // 显示浪费物品详情卡片
            binding.cardWastedItems.visibility = View.VISIBLE
        } else {
            // 隐藏所有图表卡片
            binding.cardCategoryChart.visibility = View.GONE
            binding.cardTrendChart.visibility = View.GONE
            binding.cardReasonChart.visibility = View.GONE
            
            // 隐藏浪费物品详情卡片
            binding.cardWastedItems.visibility = View.GONE
        }

        // 更新浪费物品列表（只有在有数据时才会被看到）
        wastedItemAdapter.submitList(data.topWastedItems)
    }

    private fun updateSummaryCards(data: WasteReportData) {
        with(binding) {
            textTotalItems.text = data.totalWastedItems.toString()
            textTotalValue.text = currencyFormat.format(data.totalWastedValue)
            textExpiredItems.text = data.expiredItems.toString()
            textDiscardedItems.text = data.discardedItems.toString()
        }
    }

    private fun updateCategoryChart(categoryData: List<WasteCategoryData>) {
        if (categoryData.isEmpty()) {
            binding.cardCategoryChart.visibility = View.GONE
            return
        }

        binding.cardCategoryChart.visibility = View.VISIBLE
        
        // 准备饼图数据 - 按照文档格式
        val seriesData = categoryData.map { category ->
            arrayOf(category.category as Any, category.totalValue as Any)
        }.toTypedArray() as Array<Any>

        // 创建饼图配置
        val aaChartModel = AAChartModel()
            .chartType(AAChartType.Pie)
            .title("浪费分类统计")
            .subtitle("按类别统计浪费金额")
            .backgroundColor("#FFFFFF")
            .dataLabelsEnabled(true)
            .legendEnabled(true)
            .animationType(AAChartAnimationType.EaseOutBounce)
            .animationDuration(1200)
            .colorsTheme(arrayOf("#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", "#DDA0DD", "#F39C12", "#8E44AD"))
            .series(arrayOf(
                AASeriesElement()
                    .name("浪费金额")
                    .data(seriesData)
            ))

        // 显示图表
        binding.categoryChart.aa_drawChartWithChartModel(aaChartModel)
    }

    private fun updateTrendChart(timeData: List<WasteTimeData>) {
        if (timeData.isEmpty()) {
            binding.cardTrendChart.visibility = View.GONE
            return
        }

        binding.cardTrendChart.visibility = View.VISIBLE
        
        // 准备时间趋势数据
        val chartData = timeData.map { it.totalValue as Any }.toTypedArray()
        val categories = timeData.map { dateFormat.format(it.date) }.toTypedArray()

        // 创建折线图配置
        val aaChartModel = AAChartModel()
            .chartType(AAChartType.Line)
            .title("浪费趋势分析")
            .subtitle("按时间统计浪费金额变化")
            .backgroundColor("#FFFFFF")
            .dataLabelsEnabled(false)
            .categories(categories)
            .yAxisTitle("浪费金额 (元)")
            .markerRadius(5)
            .legendEnabled(true)
            .touchEventEnabled(true)
            .zoomType(AAChartZoomType.X)
            .animationType(AAChartAnimationType.EaseOutQuart)
            .animationDuration(1000)
            .series(arrayOf(
                AASeriesElement()
                    .name("浪费金额")
                    .data(chartData)
                    .color("#FF6B6B")
                    .lineWidth(3)
            ))

        // 显示图表
        binding.trendChart.aa_drawChartWithChartModel(aaChartModel)
    }

    private fun updateReasonChart(data: WasteReportData) {
        if (data.totalWastedItems == 0) {
            binding.cardReasonChart.visibility = View.GONE
            return
        }

        binding.cardReasonChart.visibility = View.VISIBLE
        
        // 准备浪费原因对比数据
        val reasonData = arrayOf(
            data.expiredItems.toDouble() as Any,
            data.discardedItems.toDouble() as Any
        )
        
        val categories = arrayOf("过期", "丢弃")

        // 创建柱状图配置
        val aaChartModel = AAChartModel()
            .chartType(AAChartType.Column)
            .title("浪费原因对比")
            .subtitle("过期 vs 主动丢弃")
            .backgroundColor("#FFFFFF")
            .dataLabelsEnabled(true)
            .categories(categories)
            .yAxisTitle("物品数量")
            .legendEnabled(false)
            .animationType(AAChartAnimationType.EaseOutBack)
            .animationDuration(800)
            .colorsTheme(arrayOf("#FF6B6B", "#4ECDC4"))
            .series(arrayOf(
                AASeriesElement()
                    .name("物品数量")
                    .data(reasonData)
            ))

        // 显示图表
        binding.reasonChart.aa_drawChartWithChartModel(aaChartModel)
    }

    private fun showEmptyState() {
        binding.layoutReportContent.visibility = View.GONE
        binding.layoutEmptyState.visibility = View.VISIBLE
    }

    private fun debugDatabaseStatus() {
        lifecycleScope.launch {
            try {
                val application = requireActivity().application as ItemManagementApplication
                val repository = application.repository
                
                // 获取所有物品来检查状态
                repository.getAllItems().collect { items ->
                    val currentTime = System.currentTimeMillis()
                    val expiredItems = items.filter { item ->
                        item.expirationDate != null && item.expirationDate!!.time <= currentTime
                    }
                    
                    println("=== 数据库状态调试 ===")
                    println("总物品数: ${items.size}")
                    println("已过期物品数: ${expiredItems.size}")
                    
                    expiredItems.forEach { item ->
                        println("过期物品: ${item.name}, 状态: ${item.status}, 过期日期: ${item.expirationDate}")
                    }
                    
                    val wastedItems = items.filter { 
                        it.status == com.example.itemmanagement.data.model.ItemStatus.EXPIRED || 
                        it.status == com.example.itemmanagement.data.model.ItemStatus.DISCARDED 
                    }
                    println("数据库中浪费状态物品数: ${wastedItems.size}")
                    println("==================")
                    return@collect
                }
            } catch (e: Exception) {
                println("调试失败: ${e.message}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 