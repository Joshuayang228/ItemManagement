package com.example.itemmanagement.ui.calendar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.data.entity.CalendarEventEntity
import com.example.itemmanagement.data.model.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*
import java.text.SimpleDateFormat
import java.util.Locale

class ItemCalendarViewModel(private val repository: UnifiedItemRepository) : ViewModel() {

    private val _currentMonth = MutableLiveData<Calendar>()
    val currentMonth: LiveData<Calendar> = _currentMonth

    private val _calendarEvents = MutableLiveData<List<CalendarEvent>>()
    val calendarEvents: LiveData<List<CalendarEvent>> = _calendarEvents

    private val _calendarDays = MutableLiveData<List<CalendarDay>>()
    val calendarDays: LiveData<List<CalendarDay>> = _calendarDays

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _selectedDate = MutableLiveData<Date?>()
    val selectedDate: LiveData<Date?> = _selectedDate

    enum class ViewMode {
        MONTH
    }

    init {
        _currentMonth.value = Calendar.getInstance()
        cleanupDuplicateEvents()
        loadEventsForMonth()
        // 恢复自动事件生成，但使用改进的去重逻辑
        generateAutoEvents()
    }
    
    private fun cleanupDuplicateEvents() {
        viewModelScope.launch {
            try {
                val allEvents: List<CalendarEventEntity> = repository.getAllCalendarEvents().first()
                android.util.Log.d("CalendarViewModel", "开始清理重复事件，总共 ${allEvents.size} 个事件")
                
                // 更详细的去重逻辑：基于多个字段组合
                val eventGroups: Map<String, List<CalendarEventEntity>> = allEvents.groupBy { event ->
                    "${event.itemId}_${event.eventType}_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(event.eventDate)}_${event.title}"
                }
                
                var deletedCount = 0
                var groupsWithDuplicates = 0
                
                eventGroups.forEach { (key, events) ->
                    if (events.size > 1) {
                        groupsWithDuplicates++
                        android.util.Log.d("CalendarViewModel", "发现重复事件组: $key，共 ${events.size} 个")
                        
                        // 保留最新创建的，删除其余的
                        val sortedEvents = events.sortedByDescending { it.createdDate }
                        val eventsToDelete = sortedEvents.drop(1)
                        
                        eventsToDelete.forEach { event ->
                            repository.deleteCalendarEvent(event.id)
                            deletedCount++
                            android.util.Log.d("CalendarViewModel", "删除重复事件 ID: ${event.id}, 标题: ${event.title}")
                        }
                    }
                }
                
                android.util.Log.d("CalendarViewModel", "发现 $groupsWithDuplicates 个重复组，总共删除了 $deletedCount 个重复事件")
                
                // 如果删除了事件，重新加载数据
                if (deletedCount > 0) {
                    loadEventsForMonth()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("CalendarViewModel", "清理重复事件失败", e)
            }
        }
    }

    fun setViewMode(mode: ViewMode) {
        // 只支持月视图，直接加载月视图数据
        loadEventsForMonth()
    }

    fun selectDate(date: Date) {
        android.util.Log.d("CalendarViewModel", "selectDate 被调用: $date")
        _selectedDate.value = date
    }

    fun navigateToMonth(year: Int, month: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        _currentMonth.value = calendar
        loadEventsForMonth()
    }

    fun previousMonth() {
        _currentMonth.value?.let { calendar ->
            calendar.add(Calendar.MONTH, -1)
            _currentMonth.value = calendar
            loadEventsForMonth()
        }
    }

    fun nextMonth() {
        _currentMonth.value?.let { calendar ->
            calendar.add(Calendar.MONTH, 1)
            _currentMonth.value = calendar
            loadEventsForMonth()
        }
    }



    private fun loadEventsForMonth() {
        _currentMonth.value?.let { calendar ->
            viewModelScope.launch {
                try {
                    _isLoading.value = true
                    _error.value = null

                    val startOfMonth = Calendar.getInstance().apply {
                        set(Calendar.YEAR, calendar.get(Calendar.YEAR))
                        set(Calendar.MONTH, calendar.get(Calendar.MONTH))
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time

                    val endOfMonth = Calendar.getInstance().apply {
                        set(Calendar.YEAR, calendar.get(Calendar.YEAR))
                        set(Calendar.MONTH, calendar.get(Calendar.MONTH))
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.time

                    val events: List<CalendarEventEntity> = repository.getCalendarEventsBetweenDates(startOfMonth, endOfMonth).first()
                    val calendarEvents: List<CalendarEvent> = events.map { entity ->
                        CalendarEvent(
                            id = entity.id,
                            itemId = entity.itemId,
                            eventType = entity.eventType,
                            title = entity.title,
                            description = entity.description,
                            eventDate = entity.eventDate,
                            reminderDays = entity.reminderDays,
                            priority = entity.priority,
                            isCompleted = entity.isCompleted,
                            recurrenceType = entity.recurrenceType
                        )
                    }
                    _calendarEvents.value = calendarEvents
                    android.util.Log.d("CalendarViewModel", "加载了 ${calendarEvents.size} 个月视图事件")
                    
                    // 重新生成日历网格
                    generateCalendarDays()

                } catch (e: Exception) {
                    _error.value = "加载日历事件失败: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }



    private fun generateAutoEvents() {
        viewModelScope.launch {
            try {
                android.util.Log.d("CalendarViewModel", "开始智能生成自动事件")
                
                // 获取所有物品
                val items = repository.getAllItems().first()
                android.util.Log.d("CalendarViewModel", "找到 ${items.size} 个物品")
                
                // 获取现有的事件，按类型分组
                val existingEvents: List<CalendarEventEntity> = repository.getAllCalendarEvents().first()
                val existingEventKeys: Set<String> = existingEvents.map { event ->
                    "${event.itemId}_${event.eventType}_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(event.eventDate)}"
                }.toSet()
                
                android.util.Log.d("CalendarViewModel", "现有事件数量: ${existingEvents.size}")
                
                val autoEvents = mutableListOf<CalendarEventEntity>()
                
                items.forEach { item ->
                    android.util.Log.d("CalendarViewModel", "处理物品: ${item.name}")
                    
                    // 过期提醒事件
                    item.expirationDate?.let { expDate ->
                        val key = "${item.id}_${EventType.EXPIRATION}_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(expDate)}"
                        if (key !in existingEventKeys) {
                            autoEvents.add(
                                CalendarEventEntity(
                                    itemId = item.id,
                                    eventType = EventType.EXPIRATION,
                                    title = "${item.name} 即将过期",
                                    description = "请注意检查保质期",
                                    eventDate = expDate,
                                    reminderDays = listOf(1, 3, 7),
                                    priority = Priority.HIGH
                                )
                            )
                            android.util.Log.d("CalendarViewModel", "生成过期事件: ${item.name}")
                        }
                    }

                    // 保修到期事件
                    item.warrantyEndDate?.let { warrantyDate ->
                        val key = "${item.id}_${EventType.WARRANTY}_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(warrantyDate)}"
                        if (key !in existingEventKeys) {
                            autoEvents.add(
                                CalendarEventEntity(
                                    itemId = item.id,
                                    eventType = EventType.WARRANTY,
                                    title = "${item.name} 保修到期",
                                    description = "保修期即将结束",
                                    eventDate = warrantyDate,
                                    reminderDays = listOf(7, 30),
                                    priority = Priority.NORMAL
                                )
                            )
                            android.util.Log.d("CalendarViewModel", "生成保修事件: ${item.name}")
                        }
                    }

                    // 购买纪念日事件 - 更合理的条件
                    item.purchaseDate?.let { purchaseDate ->
                        val daysSincePurchase = ((System.currentTimeMillis() - purchaseDate.time) / (1000 * 60 * 60 * 24)).toInt()
                        if (daysSincePurchase > 30) { // 只为购买超过30天的物品生成纪念日
                            val calendar = Calendar.getInstance()
                            calendar.time = purchaseDate
                            calendar.add(Calendar.YEAR, 1) // 一周年纪念
                            
                            val key = "${item.id}_${EventType.ANNIVERSARY}_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)}"
                            if (key !in existingEventKeys) {
                                autoEvents.add(
                                    CalendarEventEntity(
                                        itemId = item.id,
                                        eventType = EventType.ANNIVERSARY,
                                        title = "${item.name} 购买一周年",
                                        description = "购买于 ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(purchaseDate)}",
                                        eventDate = calendar.time,
                                        reminderDays = listOf(7),
                                        priority = Priority.LOW
                                    )
                                )
                                android.util.Log.d("CalendarViewModel", "生成纪念日事件: ${item.name}")
                            }
                        }
                    }
                }

                // 批量插入新生成的事件
                if (autoEvents.isNotEmpty()) {
                    repository.insertCalendarEvents(autoEvents)
                    android.util.Log.d("CalendarViewModel", "成功生成 ${autoEvents.size} 个新的自动事件")
                    loadEventsForMonth() // 重新加载事件
                } else {
                    android.util.Log.d("CalendarViewModel", "没有需要生成的新事件")
                }
            } catch (e: Exception) {
                android.util.Log.e("CalendarViewModel", "自动事件生成失败", e)
            }
        }
    }

    fun markEventCompleted(eventId: Long) {
        viewModelScope.launch {
            try {
                repository.markCalendarEventCompleted(eventId)
                loadEventsForMonth() // 重新加载事件
            } catch (e: Exception) {
                _error.value = "标记事件完成失败: ${e.message}"
            }
        }
    }

    fun deleteEvent(eventId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteCalendarEvent(eventId)
                loadEventsForMonth() // 重新加载事件
            } catch (e: Exception) {
                _error.value = "删除事件失败: ${e.message}"
            }
        }
    }

    fun addCustomEvent(event: CalendarEventEntity) {
        viewModelScope.launch {
            try {
                repository.insertCalendarEvent(event)
                loadEventsForMonth() // 重新加载事件
            } catch (e: Exception) {
                _error.value = "添加事件失败: ${e.message}"
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

    private fun calculateUrgencyLevel(daysUntil: Int): UrgencyLevel {
        return when {
            daysUntil < 0 -> UrgencyLevel.OVERDUE
            daysUntil == 0 -> UrgencyLevel.URGENT
            daysUntil <= 3 -> UrgencyLevel.SOON
            daysUntil <= 7 -> UrgencyLevel.UPCOMING
            else -> UrgencyLevel.NORMAL
        }
    }

    private fun generateCalendarDays() {
        _currentMonth.value?.let { calendar ->
            viewModelScope.launch {
                try {
                    _isLoading.value = true
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH)
                    
                    // 创建月份的第一天
                    val firstDayOfMonth = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    // 计算月份开始的星期几 (0=周日, 1=周一...)
                    val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 1
                    
                    // 月份的天数
                    val daysInMonth = firstDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
                    
                    // 创建日历网格 (6周 x 7天 = 42天)
                    val calendarDays = mutableListOf<CalendarDay>()
                    val today = Calendar.getInstance().time
                    
                    // 开始日期 = 月份第一天 - 星期偏移
                    val startCalendar = firstDayOfMonth.clone() as Calendar
                    startCalendar.add(Calendar.DAY_OF_MONTH, -firstDayOfWeek)
                    
                    // 生成42天的网格
                    for (i in 0 until 42) {
                        val currentDate = Date(startCalendar.timeInMillis) // 创建新的Date对象
                        val isCurrentMonth = startCalendar.get(Calendar.MONTH) == month
                        val isToday = isSameDay(currentDate, today)
                        
                        // 获取当日的事件
                        val dayEvents = _calendarEvents.value?.filter { event ->
                            isSameDay(event.eventDate, currentDate)
                        } ?: emptyList()
                        
                        calendarDays.add(
                            CalendarDay(
                                date = currentDate,
                                events = dayEvents,
                                isToday = isToday,
                                isCurrentMonth = isCurrentMonth
                            )
                        )
                        
                        startCalendar.add(Calendar.DAY_OF_MONTH, 1)
                    }
                    
                    _calendarDays.value = calendarDays
                    android.util.Log.d("CalendarViewModel", "生成了 ${calendarDays.size} 天的日历数据")
                    
                } catch (e: Exception) {
                    _error.value = "生成日历网格失败: ${e.message}"
                    e.printStackTrace()
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
    
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun getEventsForDate(date: Date): List<CalendarEvent> {
        return _calendarEvents.value?.filter { event ->
            isSameDay(event.eventDate, date)
        } ?: emptyList()
    }

    // 添加一个强制清理所有自动生成事件的方法
    fun forceCleanupAutoEvents() {
        viewModelScope.launch {
            try {
                val allEvents: List<CalendarEventEntity> = repository.getAllCalendarEvents().first()
                android.util.Log.d("CalendarViewModel", "开始强制清理自动生成的事件")
                
                // 删除所有自动生成的事件（基于事件类型）
                val autoEventTypes = listOf(EventType.EXPIRATION, EventType.WARRANTY, EventType.ANNIVERSARY)
                val autoEvents: List<CalendarEventEntity> = allEvents.filter { it.eventType in autoEventTypes }
                
                android.util.Log.d("CalendarViewModel", "找到 ${autoEvents.size} 个自动生成的事件")
                
                autoEvents.forEach { event ->
                    repository.deleteCalendarEvent(event.id)
                    android.util.Log.d("CalendarViewModel", "删除自动事件: ${event.title}")
                }
                
                android.util.Log.d("CalendarViewModel", "已删除所有自动生成的事件")
                loadEventsForMonth()
                
            } catch (e: Exception) {
                android.util.Log.e("CalendarViewModel", "强制清理失败", e)
            }
        }
    }
} 