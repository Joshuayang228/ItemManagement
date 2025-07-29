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
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        // 直接设置为月视图模式
        viewModel.setViewMode(ItemCalendarViewModel.ViewMode.MONTH)
        
        // 确保初始状态正确显示
        binding.selectedDateEvents.visibility = View.GONE
        binding.emptyStateText.visibility = View.VISIBLE
        
        android.util.Log.d("CalendarFragment", "Fragment 初始化完成")
    }
    
    private fun setupRecyclerView() {
        // 设置月视图适配器
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, 7) // 7天一周
            adapter = monthAdapter
        }
        
        // 设置月视图的日期点击监听器
        monthAdapter.setOnDateClickListener { date ->
            android.util.Log.d("CalendarFragment", "日期被点击: ${dateFormat.format(date)}")
            viewModel.selectDate(date)
            monthAdapter.setSelectedDate(date)
            showSelectedDateEvents(date)
        }
        
        // 设置选中日期事件列表适配器
        binding.selectedDateEventsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = selectedDateEventsAdapter
        }
        
        selectedDateEventsAdapter.setOnEventActionListener { action, eventId ->
            when (action) {
                "complete" -> viewModel.markEventCompleted(eventId)
                "delete" -> viewModel.deleteEvent(eventId)
            }
        }
        
        selectedDateEventsAdapter.setOnEventClickListener { event ->
            // TODO: 导航到事件详情页面
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
            android.util.Log.d("CalendarFragment", "selectedDate Observer 触发: $date")
            if (date != null) {
                showSelectedDateEvents(date)
            } else {
                hideSelectedDateEvents()
            }
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
        
        // 添加事件按钮
        binding.addEventFab.setOnClickListener {
            // TODO: 导航到添加事件页面
            // findNavController().navigate(R.id.action_calendar_to_add_event)
        }
    }

    private fun showSelectedDateEvents(date: Date) {
        // 检查是否是相同的日期，避免重复处理
        if (currentSelectedDate != null && isSameDay(currentSelectedDate!!, date)) {
            android.util.Log.d("CalendarFragment", "相同日期，跳过重复处理: ${dateFormat.format(date)}")
            return
        }
        
        currentSelectedDate = date
        android.util.Log.d("CalendarFragment", "showSelectedDateEvents 被调用，日期: ${dateFormat.format(date)}")
        
        binding.selectedDateTitle.text = "${dateFormat.format(date)}的事件"
        
        // 获取选中日期的事件
        val eventsForDate = viewModel.calendarEvents.value?.filter { event ->
            isSameDay(event.eventDate, date)
        } ?: emptyList()
        
        android.util.Log.d("CalendarFragment", "找到 ${eventsForDate.size} 个事件")
        
        if (eventsForDate.isNotEmpty()) {
            selectedDateEventsAdapter.submitList(eventsForDate.sortedBy { it.eventDate })
            binding.selectedDateEvents.visibility = View.VISIBLE
            binding.emptyStateText.visibility = View.GONE
            android.util.Log.d("CalendarFragment", "显示事件列表")
        } else {
            // 显示空状态但保持事件区域可见
            binding.selectedDateTitle.text = "${dateFormat.format(date)}的事件 (暂无事件)"
            selectedDateEventsAdapter.submitList(emptyList())
            binding.selectedDateEvents.visibility = View.VISIBLE
            binding.emptyStateText.visibility = View.GONE
            android.util.Log.d("CalendarFragment", "显示空状态")
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 