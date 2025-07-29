package com.example.itemmanagement.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.itemmanagement.R
import com.example.itemmanagement.data.model.CalendarDay
import com.example.itemmanagement.data.model.UrgencyLevel
import com.example.itemmanagement.databinding.ItemCalendarDayBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarMonthAdapter : ListAdapter<CalendarDay, CalendarMonthAdapter.CalendarDayViewHolder>(DiffCallback()) {

    private var onDateClickListener: ((Date) -> Unit)? = null
    private var selectedDate: Date? = null

    fun setOnDateClickListener(listener: (Date) -> Unit) {
        onDateClickListener = listener
    }
    
    fun setSelectedDate(date: Date?) {
        val oldSelectedDate = selectedDate
        selectedDate = date
        
        // 刷新旧的选中项和新的选中项
        if (oldSelectedDate != null) {
            notifyItemChanged(findPositionByDate(oldSelectedDate))
        }
        if (date != null) {
            notifyItemChanged(findPositionByDate(date))
        }
    }
    
    private fun findPositionByDate(date: Date): Int {
        for (i in 0 until itemCount) {
            if (isSameDay(getItem(i).date, date)) {
                return i
            }
        }
        return -1
    }
    
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarDayViewHolder {
        val binding = ItemCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CalendarDayViewHolder(binding, onDateClickListener)
    }

    override fun onBindViewHolder(holder: CalendarDayViewHolder, position: Int) {
        val calendarDay = getItem(position)
        val isSelected = selectedDate?.let { isSameDay(calendarDay.date, it) } ?: false
        holder.bind(calendarDay, isSelected)
    }

    class CalendarDayViewHolder(
        private val binding: ItemCalendarDayBinding,
        private val onDateClickListener: ((Date) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dayFormat = SimpleDateFormat("d", Locale.getDefault())

        fun bind(calendarDay: CalendarDay, isSelected: Boolean) {
            binding.apply {
                // 设置日期数字
                dayNumber.text = dayFormat.format(calendarDay.date)

                // 设置选中状态背景
                selectedBackground.visibility = if (isSelected) View.VISIBLE else View.GONE

                // 设置今日高亮
                if (calendarDay.isToday) {
                    dayNumber.setBackgroundResource(R.drawable.circle_background)
                    dayNumber.setTextColor(Color.WHITE)
                } else {
                    dayNumber.background = null
                    dayNumber.setTextColor(
                        if (calendarDay.isCurrentMonth) 
                            Color.parseColor("#333333") 
                        else 
                            Color.parseColor("#CCCCCC")
                    )
                }

                // 设置事件指示器
                setupEventIndicators(calendarDay)

                // 设置点击事件
                root.setOnClickListener {
                    android.util.Log.d("CalendarAdapter", "日期项被点击: ${dayFormat.format(calendarDay.date)}")
                    onDateClickListener?.invoke(calendarDay.date)
                }
            }
        }

        private fun setupEventIndicators(calendarDay: CalendarDay) {
            val events = calendarDay.events
            binding.apply {
                // 重置所有指示器
                eventIndicator1.visibility = View.GONE
                eventIndicator2.visibility = View.GONE
                eventIndicator3.visibility = View.GONE
                moreEventsText.visibility = View.GONE

                when {
                    events.isEmpty() -> {
                        // 没有事件
                    }
                    events.size == 1 -> {
                        moreEventsText.visibility = View.VISIBLE
                        moreEventsText.text = "1"
                        moreEventsText.setTextColor(getEventColor(events[0]))
                    }
                    else -> {
                        moreEventsText.visibility = View.VISIBLE
                        moreEventsText.text = events.size.toString()
                        moreEventsText.setTextColor(getEventColor(events[0]))
                    }
                }
            }
        }

        private fun getEventColor(event: com.example.itemmanagement.data.model.CalendarEvent): Int {
            val today = Calendar.getInstance().time
            val daysUntil = calculateDaysUntil(event.eventDate, today)
            
            return when {
                daysUntil < 0 -> Color.parseColor(UrgencyLevel.OVERDUE.color)
                daysUntil == 0 -> Color.parseColor(UrgencyLevel.URGENT.color)
                daysUntil <= 3 -> Color.parseColor(UrgencyLevel.SOON.color)
                daysUntil <= 7 -> Color.parseColor(UrgencyLevel.UPCOMING.color)
                else -> Color.parseColor(UrgencyLevel.NORMAL.color)
            }
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
    }

    class DiffCallback : DiffUtil.ItemCallback<CalendarDay>() {
        override fun areItemsTheSame(oldItem: CalendarDay, newItem: CalendarDay): Boolean {
            return oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: CalendarDay, newItem: CalendarDay): Boolean {
            return oldItem == newItem
        }
    }
} 