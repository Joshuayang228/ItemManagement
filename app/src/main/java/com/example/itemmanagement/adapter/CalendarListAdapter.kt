package com.example.itemmanagement.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.itemmanagement.data.model.CalendarEvent
import com.example.itemmanagement.databinding.ItemCalendarEventListBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarListAdapter : ListAdapter<CalendarEvent, CalendarListAdapter.EventListViewHolder>(DiffCallback()) {

    private var onEventClickListener: ((CalendarEvent) -> Unit)? = null
    private var onEventActionListener: ((String, Long) -> Unit)? = null

    fun setOnEventClickListener(listener: (CalendarEvent) -> Unit) {
        onEventClickListener = listener
    }

    fun setOnEventActionListener(listener: (String, Long) -> Unit) {
        onEventActionListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventListViewHolder {
        val binding = ItemCalendarEventListBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EventListViewHolder(binding, onEventClickListener, onEventActionListener)
    }

    override fun onBindViewHolder(holder: EventListViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EventListViewHolder(
        private val binding: ItemCalendarEventListBinding,
        private val onEventClickListener: ((CalendarEvent) -> Unit)?,
        private val onEventActionListener: ((String, Long) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MM月dd日 E", Locale.getDefault())
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun bind(event: CalendarEvent) {
            binding.apply {
                // 基本信息
                eventTitle.text = event.title
                eventDescription.text = event.description
                eventType.text = event.eventType.displayName
                eventDate.text = dateFormat.format(event.eventDate)

                // 设置事件类型图标
                eventTypeIcon.text = event.eventType.icon

                // 设置优先级颜色
                val priorityColor = Color.parseColor(event.priority.color)
                priorityIndicator.setBackgroundColor(priorityColor)

                // 设置距离时间
                val daysUntil = calculateDaysUntil(event.eventDate)
                daysUntilText.text = when {
                    daysUntil < 0 -> "已过期 ${-daysUntil} 天"
                    daysUntil == 0 -> "今日"
                    daysUntil == 1 -> "明日"
                    else -> "${daysUntil} 天后"
                }

                // 设置距离时间颜色
                val textColor = when {
                    daysUntil < 0 -> Color.parseColor("#F44336") // 红色
                    daysUntil == 0 -> Color.parseColor("#FF5722") // 深橙色
                    daysUntil <= 3 -> Color.parseColor("#FF9800") // 橙色
                    else -> Color.parseColor("#666666") // 灰色
                }
                daysUntilText.setTextColor(textColor)

                // 设置完成状态
                if (event.isCompleted) {
                    root.alpha = 0.6f
                    completeButton.text = "已完成"
                    completeButton.isEnabled = false
                } else {
                    root.alpha = 1.0f
                    completeButton.text = "完成"
                    completeButton.isEnabled = true
                }

                // 设置点击事件
                root.setOnClickListener {
                    onEventClickListener?.invoke(event)
                }

                completeButton.setOnClickListener {
                    if (!event.isCompleted) {
                        onEventActionListener?.invoke("complete", event.id)
                    }
                }

                deleteButton.setOnClickListener {
                    onEventActionListener?.invoke("delete", event.id)
                }
            }
        }

        private fun calculateDaysUntil(eventDate: Date): Int {
            val now = Calendar.getInstance()
            val event = Calendar.getInstance().apply { time = eventDate }
            
            // 重置时间到午夜以便准确计算天数
            now.set(Calendar.HOUR_OF_DAY, 0)
            now.set(Calendar.MINUTE, 0)
            now.set(Calendar.SECOND, 0)
            now.set(Calendar.MILLISECOND, 0)
            
            event.set(Calendar.HOUR_OF_DAY, 0)
            event.set(Calendar.MINUTE, 0)
            event.set(Calendar.SECOND, 0)
            event.set(Calendar.MILLISECOND, 0)
            
            val diffInMillis = event.timeInMillis - now.timeInMillis
            return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CalendarEvent>() {
        override fun areItemsTheSame(oldItem: CalendarEvent, newItem: CalendarEvent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CalendarEvent, newItem: CalendarEvent): Boolean {
            return oldItem == newItem
        }
    }
} 