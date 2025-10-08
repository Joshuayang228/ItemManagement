package com.example.itemmanagement.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.adapter.CalendarListAdapter
import com.example.itemmanagement.adapter.CalendarMonthAdapter
import com.example.itemmanagement.databinding.FragmentItemCalendarBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ItemCalendarFragment : Fragment() {

    private var _binding: FragmentItemCalendarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemCalendarViewModel by viewModels {
        ItemCalendarViewModelFactory(
            (requireActivity().application as ItemManagementApplication).repository
        )
    }

    private val monthAdapter = CalendarMonthAdapter()
    private val selectedDateEventsAdapter = CalendarListAdapter()
    
    private val monthFormat = SimpleDateFormat("yyyy年MM月", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
    
    private var currentSelectedDate: Date? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        android.util.Log.d("CalendarFragment", "=== onViewCreated() 开始 ===")
        
        hideBottomNavigation()
        
        // 启用菜单
        setHasOptionsMenu(true)
        
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        // 直接设置为月视图模式
        viewModel.setViewMode(ItemCalendarViewModel.ViewMode.MONTH)
        
        // 设置初始的空状态
        binding.selectedDateEvents.visibility = View.GONE
        binding.emptyStateText.visibility = View.VISIBLE
        
        android.util.Log.d("CalendarFragment", "Fragment 初始化完成")
        android.util.Log.d("CalendarFragment", "=== onViewCreated() 结束 ===")
    }
    
    override fun onResume() {
        super.onResume()
        android.util.Log.d("CalendarFragment", "=== onResume() 开始 ===")
        
        hideBottomNavigation()
        
        // Fragment恢复时，检查并恢复选中日期的状态
        val selectedDate = viewModel.selectedDate.value
        android.util.Log.d("CalendarFragment", "ViewModel中的selectedDate: $selectedDate")
        
        if (selectedDate != null) {
            android.util.Log.d("CalendarFragment", "Fragment恢复时发现已选中日期: ${dateFormat.format(selectedDate)}")
            android.util.Log.d("CalendarFragment", "准备调用 monthAdapter.setSelectedDate()")
            monthAdapter.setSelectedDate(selectedDate)
            android.util.Log.d("CalendarFragment", "准备强制刷新 showSelectedDateEvents()")
            // 强制刷新，不受重复检查限制
            forceShowSelectedDateEvents(selectedDate)
            android.util.Log.d("CalendarFragment", "强制刷新完成")
        } else {
            android.util.Log.d("CalendarFragment", "ViewModel中没有选中日期，显示空状态")
            hideSelectedDateEvents()
        }
        
        android.util.Log.d("CalendarFragment", "=== onResume() 结束 ===")
    }
    
    private fun setupRecyclerView() {
        // 设置月视图适配器
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, 7) // 7天一周
            adapter = monthAdapter
        }
        
        // 设置月视图的日期点击监听器
        monthAdapter.setOnDateClickListener { date ->
            android.util.Log.d("CalendarFragment", "=== 日期被点击: ${dateFormat.format(date)} ===")
            android.util.Log.d("CalendarFragment", "准备调用 viewModel.selectDate()")
            viewModel.selectDate(date)
            android.util.Log.d("CalendarFragment", "准备调用 monthAdapter.setSelectedDate()")
            monthAdapter.setSelectedDate(date)
            android.util.Log.d("CalendarFragment", "准备调用 showSelectedDateEvents()")
            showSelectedDateEvents(date)
            android.util.Log.d("CalendarFragment", "日期点击处理完成")
        }
        
        // 设置选中日期事件列表适配器
        binding.selectedDateEventsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = selectedDateEventsAdapter
        }
        
        
        selectedDateEventsAdapter.setOnEventClickListener { event ->
            // 导航到物品详情页面
            android.util.Log.d("CalendarFragment", "=== 事件被点击，准备导航 ===")
            android.util.Log.d("CalendarFragment", "事件标题: ${event.title}, 物品ID: ${event.itemId}")
            android.util.Log.d("CalendarFragment", "当前选中日期: ${viewModel.selectedDate.value}")
            
            val action = ItemCalendarFragmentDirections
                .actionItemCalendarToItemDetail(event.itemId)
            findNavController().navigate(action)
            
            android.util.Log.d("CalendarFragment", "导航已发起")
        }
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.errorText.text = error
                binding.errorText.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.errorText.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }

        viewModel.calendarEvents.observe(viewLifecycleOwner) { events ->
            // 显示统计信息
            val today = Calendar.getInstance().time
            val overdueCount = events.count { 
                calculateDaysUntil(it.eventDate, today) < 0 
            }
            val urgentCount = events.count { 
                calculateDaysUntil(it.eventDate, today) == 0 
            }
            val soonCount = events.count { 
                val days = calculateDaysUntil(it.eventDate, today)
                days in 1..3 
            }
            
            binding.overdueCount.text = overdueCount.toString()
            binding.urgentCount.text = urgentCount.toString()
            binding.soonCount.text = soonCount.toString()
        }
        
        viewModel.calendarDays.observe(viewLifecycleOwner) { days ->
            monthAdapter.submitList(days)
            // 调试信息
            android.util.Log.d("CalendarFragment", "收到日历数据: ${days.size} 天")
        }
        
        viewModel.currentMonth.observe(viewLifecycleOwner) { calendar ->
            updateMonthDisplay(calendar)
        }
        
        viewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            android.util.Log.d("CalendarFragment", "=== selectedDate Observer 触发 ===")
            android.util.Log.d("CalendarFragment", "新的选中日期: ${date?.let { dateFormat.format(it) } ?: "null"}")
            android.util.Log.d("CalendarFragment", "当前Fragment状态: isAdded=$isAdded, isVisible=$isVisible, isResumed=$isResumed")
            if (date != null) {
                android.util.Log.d("CalendarFragment", "准备调用 showSelectedDateEvents")
                showSelectedDateEvents(date)
            } else {
                android.util.Log.d("CalendarFragment", "准备调用 hideSelectedDateEvents")
                hideSelectedDateEvents()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu, inflater: android.view.MenuInflater) {
        inflater.inflate(R.menu.menu_calendar, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }
    
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_calendar_settings -> {
                findNavController().navigate(R.id.action_item_calendar_to_reminder_settings)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun setupClickListeners() {
        // 月份导航按钮
        binding.previousMonthButton.setOnClickListener {
            viewModel.previousMonth()
        }
        
        binding.nextMonthButton.setOnClickListener {
            viewModel.nextMonth()
        }
    }

    private fun showSelectedDateEvents(date: Date) {
        android.util.Log.d("CalendarFragment", "=== showSelectedDateEvents 开始 ===")
        android.util.Log.d("CalendarFragment", "传入的日期: ${dateFormat.format(date)}")
        android.util.Log.d("CalendarFragment", "当前选中日期: ${currentSelectedDate?.let { dateFormat.format(it) } ?: "null"}")
        
        // 检查是否是相同的日期，避免重复处理
        if (currentSelectedDate != null && isSameDay(currentSelectedDate!!, date)) {
            android.util.Log.d("CalendarFragment", "相同日期，跳过重复处理: ${dateFormat.format(date)}")
            return
        }
        
        currentSelectedDate = date
        android.util.Log.d("CalendarFragment", "设置新的currentSelectedDate: ${dateFormat.format(date)}")
        
        binding.selectedDateTitle.text = "${dateFormat.format(date)}的事件"
        android.util.Log.d("CalendarFragment", "已设置事件卡片标题")
        
        // 获取选中日期的事件
        val allEvents = viewModel.calendarEvents.value
        android.util.Log.d("CalendarFragment", "ViewModel中总共有 ${allEvents?.size ?: 0} 个事件")
        
        val eventsForDate = allEvents?.filter { event ->
            val isSame = isSameDay(event.eventDate, date)
            android.util.Log.d("CalendarFragment", "事件: ${event.title}, 日期: ${dateFormat.format(event.eventDate)}, 匹配: $isSame")
            isSame
        } ?: emptyList()
        
        android.util.Log.d("CalendarFragment", "筛选后找到 ${eventsForDate.size} 个事件")
        
        if (eventsForDate.isNotEmpty()) {
            android.util.Log.d("CalendarFragment", "准备显示事件列表")
            selectedDateEventsAdapter.submitList(eventsForDate.sortedBy { it.eventDate })
            binding.selectedDateEvents.visibility = View.VISIBLE
            binding.emptyStateText.visibility = View.GONE
            android.util.Log.d("CalendarFragment", "事件列表已显示，卡片可见性: ${binding.selectedDateEvents.visibility}")
        } else {
            // 显示空状态但保持事件区域可见
            android.util.Log.d("CalendarFragment", "没有事件，显示空状态")
            binding.selectedDateTitle.text = "${dateFormat.format(date)}的事件 (暂无事件)"
            selectedDateEventsAdapter.submitList(emptyList())
            binding.selectedDateEvents.visibility = View.VISIBLE
            binding.emptyStateText.visibility = View.GONE
            android.util.Log.d("CalendarFragment", "空状态已显示，卡片可见性: ${binding.selectedDateEvents.visibility}")
        }
        
        android.util.Log.d("CalendarFragment", "=== showSelectedDateEvents 结束 ===")
    }
    
    private fun forceShowSelectedDateEvents(date: Date) {
        android.util.Log.d("CalendarFragment", "=== forceShowSelectedDateEvents 开始 ===")
        android.util.Log.d("CalendarFragment", "强制刷新日期: ${dateFormat.format(date)}")
        
        currentSelectedDate = date
        android.util.Log.d("CalendarFragment", "强制设置 currentSelectedDate: ${dateFormat.format(date)}")
        
        binding.selectedDateTitle.text = "${dateFormat.format(date)}的事件"
        android.util.Log.d("CalendarFragment", "已设置事件卡片标题")
        
        // 获取选中日期的事件
        val allEvents = viewModel.calendarEvents.value
        android.util.Log.d("CalendarFragment", "ViewModel中总共有 ${allEvents?.size ?: 0} 个事件")
        
        val eventsForDate = allEvents?.filter { event ->
            val isSame = isSameDay(event.eventDate, date)
            android.util.Log.d("CalendarFragment", "事件: ${event.title}, 日期: ${dateFormat.format(event.eventDate)}, 匹配: $isSame")
            isSame
        } ?: emptyList()
        
        android.util.Log.d("CalendarFragment", "筛选后找到 ${eventsForDate.size} 个事件")
        
        if (eventsForDate.isNotEmpty()) {
            android.util.Log.d("CalendarFragment", "准备强制显示事件列表")
            selectedDateEventsAdapter.submitList(eventsForDate.sortedBy { it.eventDate })
            binding.selectedDateEvents.visibility = View.VISIBLE
            binding.emptyStateText.visibility = View.GONE
            android.util.Log.d("CalendarFragment", "事件列表已强制显示，卡片可见性: ${binding.selectedDateEvents.visibility}")
        } else {
            // 显示空状态但保持事件区域可见
            android.util.Log.d("CalendarFragment", "没有事件，强制显示空状态")
            binding.selectedDateTitle.text = "${dateFormat.format(date)}的事件 (暂无事件)"
            selectedDateEventsAdapter.submitList(emptyList())
            binding.selectedDateEvents.visibility = View.VISIBLE
            binding.emptyStateText.visibility = View.GONE
            android.util.Log.d("CalendarFragment", "空状态已强制显示，卡片可见性: ${binding.selectedDateEvents.visibility}")
        }
        
        android.util.Log.d("CalendarFragment", "=== forceShowSelectedDateEvents 结束 ===")
    }
    
    private fun hideSelectedDateEvents() {
        currentSelectedDate = null
        android.util.Log.d("CalendarFragment", "hideSelectedDateEvents 被调用")
        binding.selectedDateEvents.visibility = View.GONE
        binding.emptyStateText.visibility = View.VISIBLE
    }
    
    private fun updateMonthDisplay(calendar: Calendar) {
        binding.monthTitle.text = monthFormat.format(calendar.time)
    }
    
    private fun calculateDaysUntil(eventDate: Date, today: Date): Int {
        val todayCal = Calendar.getInstance().apply {
            time = today
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val eventCal = Calendar.getInstance().apply {
            time = eventDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val diffInMillis = eventCal.timeInMillis - todayCal.timeInMillis
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    }
    
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    override fun onPause() {
        super.onPause()
        android.util.Log.d("CalendarFragment", "=== onPause() ===")
        android.util.Log.d("CalendarFragment", "当前选中日期: ${currentSelectedDate?.let { dateFormat.format(it) } ?: "null"}")
        android.util.Log.d("CalendarFragment", "ViewModel中选中日期: ${viewModel.selectedDate.value?.let { dateFormat.format(it) } ?: "null"}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        android.util.Log.d("CalendarFragment", "=== onDestroyView() ===")
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