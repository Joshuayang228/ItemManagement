package com.example.itemmanagement.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.itemmanagement.R
import com.example.itemmanagement.data.model.TimelineEvent
import com.example.itemmanagement.data.model.UrgencyLevel
import com.example.itemmanagement.databinding.ItemTimelineEventBinding
import java.text.SimpleDateFormat
import java.util.Locale

class TimelineAdapter : ListAdapter<TimelineEvent, TimelineAdapter.TimelineViewHolder>(DiffCallback()) {

    private var onEventActionListener: ((String, Long) -> Unit)? = null

    fun setOnEventActionListener(listener: (String, Long) -> Unit) {
        onEventActionListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimelineViewHolder {
        val binding = ItemTimelineEventBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TimelineViewHolder(binding, onEventActionListener)
    }

    override fun onBindViewHolder(holder: TimelineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TimelineViewHolder(
        private val binding: ItemTimelineEventBinding,
        private val onEventActionListener: ((String, Long) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MM月dd日", Locale.getDefault())
        private val timeFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun bind(timelineEvent: TimelineEvent) {
            val event = timelineEvent.event
            
            binding.apply {
                // 基本信息
                eventTitle.text = event.title
                eventDescription.text = event.description
                eventType.text = event.eventType.displayName
                eventDate.text = dateFormat.format(event.eventDate)
                fullDate.text = timeFormat.format(event.eventDate)

                // 设置事件类型图标
                eventTypeIcon.text = event.eventType.icon

                // 设置紧急程度
                setupUrgencyDisplay(timelineEvent)

                // 设置优先级颜色
                val priorityColorRes = when (event.priority.name.uppercase()) {
                    "HIGH" -> R.color.red_500
                    "MEDIUM" -> R.color.orange_500
                    "LOW" -> R.color.green_500
                    else -> R.color.green_500
                }
                val priorityColor = ContextCompat.getColor(root.context, priorityColorRes)
                priorityIndicator.setBackgroundColor(priorityColor)

                // 设置完成状态
                if (event.isCompleted) {
                    root.alpha = 0.6f
                    completeButton.text = "已完成"
                    completeButton.isEnabled = false
                } else {
                    root.alpha = 1.0f
                    completeButton.text = "标记完成"
                    completeButton.isEnabled = true
                }

                // 设置点击事件
                completeButton.setOnClickListener {
                    if (!event.isCompleted) {
                        onEventActionListener?.invoke("complete", event.id)
                    }
                }

                deleteButton.setOnClickListener {
                    onEventActionListener?.invoke("delete", event.id)
                }

                // 设置整个项目的点击事件（查看详情）
                root.setOnClickListener {
                    // TODO: 导航到事件详情页面
                }
            }
        }

        private fun setupUrgencyDisplay(timelineEvent: TimelineEvent) {
            val urgency = timelineEvent.urgencyLevel
            val daysUntil = timelineEvent.daysUntil

            binding.apply {
                // 设置紧急程度背景色
                val urgencyColor = Color.parseColor(urgency.color)
                urgencyBadge.setBackgroundColor(urgencyColor)

                // 设置紧急程度文本
                when {
                    daysUntil < 0 -> {
                        urgencyText.text = "已过期 ${-daysUntil} 天"
                        urgencyText.setTextColor(Color.WHITE)
                    }
                    daysUntil == 0 -> {
                        urgencyText.text = "今日"
                        urgencyText.setTextColor(Color.WHITE)
                    }
                    daysUntil == 1 -> {
                        urgencyText.text = "明日"
                        urgencyText.setTextColor(Color.WHITE)
                    }
                    daysUntil <= 7 -> {
                        urgencyText.text = "${daysUntil} 天后"
                        urgencyText.setTextColor(if (daysUntil <= 3) Color.WHITE else Color.BLACK)
                    }
                    else -> {
                        urgencyText.text = "${daysUntil} 天后"
                        urgencyText.setTextColor(Color.BLACK)
                    }
                }

                // 设置时间线样式
                when (urgency) {
                    UrgencyLevel.OVERDUE -> {
                        timelineDot.setBackgroundResource(R.drawable.timeline_dot_overdue)
                        timelineLine.setBackgroundColor(urgencyColor)
                    }
                    UrgencyLevel.URGENT -> {
                        timelineDot.setBackgroundResource(R.drawable.timeline_dot_urgent)
                        timelineLine.setBackgroundColor(urgencyColor)
                    }
                    UrgencyLevel.SOON -> {
                        timelineDot.setBackgroundResource(R.drawable.timeline_dot_soon)
                        timelineLine.setBackgroundColor(urgencyColor)
                    }
                    UrgencyLevel.UPCOMING -> {
                        timelineDot.setBackgroundResource(R.drawable.timeline_dot_upcoming)
                        timelineLine.setBackgroundColor(urgencyColor)
                    }
                    UrgencyLevel.NORMAL -> {
                        timelineDot.setBackgroundResource(R.drawable.timeline_dot_normal)
                        timelineLine.setBackgroundColor(urgencyColor)
                    }
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TimelineEvent>() {
        override fun areItemsTheSame(oldItem: TimelineEvent, newItem: TimelineEvent): Boolean {
            return oldItem.event.id == newItem.event.id
        }

        override fun areContentsTheSame(oldItem: TimelineEvent, newItem: TimelineEvent): Boolean {
            return oldItem == newItem
        }
    }
} 