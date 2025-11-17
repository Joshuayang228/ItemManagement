package com.example.itemmanagement.ui.waste

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.adapter.WastedItemAdapter
import com.example.itemmanagement.adapter.WasteInsightAdapter
import com.example.itemmanagement.data.model.*
import com.example.itemmanagement.databinding.FragmentWasteReportBinding
import com.example.itemmanagement.ui.utils.showMaterial3DatePicker
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartAnimationType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartZoomType
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAStyle
import com.google.android.material.snackbar.Snackbar
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
        
        hideBottomNavigation()
        
        // 延迟显示内容，避免与导航动画冲突
        setupDelayedContentDisplay()
        
        setupUI()
        setupObservers()
        
        // 先检查并更新过期物品状态，然后加载报告
        viewModel.checkExpiredItemsAndGenerateReport(ReportPeriodType.LAST_MONTH)
    }
    
    /**
     * 延迟显示内容，避免Fragment切换时的视觉重叠
     */
    private fun setupDelayedContentDisplay() {
        // 找到NestedScrollView（整个内容区域）
        val contentView = binding.root.getChildAt(0)
        
        // 先将内容设为透明
        contentView.alpha = 0f
        
        // 延迟80ms后淡入显示
        binding.root.postDelayed({
            if (_binding != null) {
                contentView.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
        }, 80)
    }

    override fun onResume() {
        super.onResume()
        hideBottomNavigation()
    }

    private fun setupUI() {
        // 设置浪费物品列表
        wastedItemAdapter = WastedItemAdapter { wastedItem ->
            // 点击浪费物品，跳转到物品详情页面
            val action = WasteReportFragmentDirections
                .actionWasteReportToItemDetail(wastedItem.id)
            findNavController().navigate(action)
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
            
            // 自动生成默认时间范围的报告
            val dateRange = DateRange(customStartDate!!, customEndDate!!)
            viewModel.checkExpiredItemsAndGenerateReport(ReportPeriodType.CUSTOM, dateRange)
        }
    }

    private fun hideCustomDateRange() {
        binding.layoutCustomDateRange.visibility = View.GONE
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val currentDate = if (isStartDate) customStartDate else customEndDate
        val title = if (isStartDate) "选择开始日期" else "选择结束日期"
        
        showMaterial3DatePicker(
            title = title,
            selectedDate = currentDate
        ) { selectedDate ->
            if (isStartDate) {
                customStartDate = selectedDate
            } else {
                customEndDate = selectedDate
            }

            updateCustomDateButtons()

            // 如果两个日期都已设置，验证并生成报告
            if (customStartDate != null && customEndDate != null) {
                if (validateDateRange(customStartDate!!, customEndDate!!)) {
                    val dateRange = DateRange(customStartDate!!, customEndDate!!)
                    viewModel.checkExpiredItemsAndGenerateReport(ReportPeriodType.CUSTOM, dateRange)
                }
            }
        }
    }

    private fun updateCustomDateButtons() {
        customStartDate?.let {
            binding.buttonStartDate.text = dateFormat.format(it)
        }
        customEndDate?.let {
            binding.buttonEndDate.text = dateFormat.format(it)
        }
    }

    private fun validateDateRange(startDate: Date, endDate: Date): Boolean {
        val currentTime = System.currentTimeMillis()
        
        when {
            startDate.time > endDate.time -> {
                Snackbar.make(binding.root, "开始日期不能晚于结束日期", Snackbar.LENGTH_SHORT).show()
                return false
            }
            endDate.time > currentTime -> {
                Snackbar.make(binding.root, "结束日期不能超过当前时间", Snackbar.LENGTH_SHORT).show()
                return false
            }
            (endDate.time - startDate.time) > (365L * 24 * 60 * 60 * 1000) -> {
                Snackbar.make(binding.root, "日期范围不能超过一年", Snackbar.LENGTH_SHORT).show()
                return false
            }
            else -> return true
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
                    .setAction("重试") { 
                        viewModel.refreshReport()
                    }
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
        val hasData = data.summary.totalItems > 0
        
        if (hasData) {
            // 更新图表
            updateCategoryChart(data.categoryData)
            updateTrendChart(data.dateData)
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

        // 更新浪费物品列表（转换为WastedItemData）
        val wastedItemDataList = data.wastedItems.take(10).map { info ->
            WastedItemData(
                id = info.id,
                name = info.name,
                category = info.category,
                wasteReason = if (info.status == "EXPIRED") WasteReason.EXPIRED else WasteReason.OTHER,
                wasteDate = info.wasteDate,
                originalValue = info.value,
                quantity = info.quantity,
                unit = info.unit,
                isQuantityUserInput = info.isQuantityUserInput,
                photoUri = info.photoUri
            )
        }
        wastedItemAdapter.submitList(wastedItemDataList)
    }

    private fun updateSummaryCards(data: WasteReportData) {
        with(binding) {
            textTotalItems.text = data.summary.totalItems.toString()
            textTotalValue.text = currencyFormat.format(data.summary.totalValue)
            textExpiredItems.text = data.summary.expiredItems.toString()
            textDiscardedItems.text = data.summary.discardedItems.toString()
        }
    }

    private fun updateCategoryChart(categoryData: List<WasteCategoryInfo>) {
        if (categoryData.isEmpty()) {
            binding.cardCategoryChart.visibility = View.GONE
            return
        }

        binding.cardCategoryChart.visibility = View.VISIBLE
        
        // 准备饼图数据
        val seriesData = categoryData.map { category ->
            arrayOf(category.category as Any, category.value as Any)
        }.toTypedArray()

        // 创建饼图配置
        val aaChartModel = AAChartModel()
            .chartType(AAChartType.Pie)
            .title("标题")
            .titleStyle(AAStyle().color("transparent"))
            .subtitle("")
            .subtitleStyle(AAStyle().color("transparent"))
            .backgroundColor("#FFFFFF")
            .dataLabelsEnabled(true)
            .legendEnabled(true)
            .animationType(AAChartAnimationType.EaseOutBounce)
            .animationDuration(1200)
            .colorsTheme(arrayOf("#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", "#DDA0DD", "#F39C12", "#8E44AD"))
            .series(arrayOf(
                AASeriesElement()
                    .name("浪费金额")
                    .data(seriesData as Array<Any>)
            ))

        binding.categoryChart.aa_drawChartWithChartModel(aaChartModel)
    }

    private fun updateTrendChart(dateData: List<WasteDateInfo>) {
        if (dateData.isEmpty()) {
            binding.cardTrendChart.visibility = View.GONE
            return
        }

        binding.cardTrendChart.visibility = View.VISIBLE
        
        // 准备时间趋势数据
        val chartData = dateData.map { it.value }.toTypedArray()
        val categories = dateData.map { it.date }.toTypedArray()

        // 创建折线图配置
        val aaChartModel = AAChartModel()
            .chartType(AAChartType.Line)
            .title("标题")
            .titleStyle(AAStyle().color("transparent"))
            .subtitle("")
            .subtitleStyle(AAStyle().color("transparent"))
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
                    .data(chartData as Array<Any>)
                    .color("#FF6B6B")
                    .lineWidth(3)
            ))

        binding.trendChart.aa_drawChartWithChartModel(aaChartModel)
    }

    private fun updateReasonChart(data: WasteReportData) {
        if (data.summary.totalItems == 0) {
            binding.cardReasonChart.visibility = View.GONE
            return
        }

        binding.cardReasonChart.visibility = View.VISIBLE
        
        // 准备浪费原因对比数据
        val reasonData = arrayOf(
            data.summary.expiredItems.toDouble() as Any,
            data.summary.discardedItems.toDouble() as Any
        )
        
        val categories = arrayOf("过期", "丢弃")

        // 创建柱状图配置
        val aaChartModel = AAChartModel()
            .chartType(AAChartType.Column)
            .title("标题")
            .titleStyle(AAStyle().color("transparent"))
            .subtitle("")
            .subtitleStyle(AAStyle().color("transparent"))
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

        binding.reasonChart.aa_drawChartWithChartModel(aaChartModel)
    }

    private fun showEmptyState() {
        binding.layoutReportContent.visibility = View.GONE
        binding.layoutEmptyState.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        showBottomNavigation()
        _binding = null
    }

    /**
     * 隐藏底部导航栏
     */
    private fun hideBottomNavigation() {
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.GONE
    }

    /**
     * 显示底部导航栏
     */
    private fun showBottomNavigation() {
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.VISIBLE
    }
}

