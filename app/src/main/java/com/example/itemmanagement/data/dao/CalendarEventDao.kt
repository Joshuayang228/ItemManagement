package com.example.itemmanagement.data.dao

import androidx.room.*
import com.example.itemmanagement.data.entity.CalendarEventEntity
import com.example.itemmanagement.data.model.EventType
import com.example.itemmanagement.data.model.Priority
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface CalendarEventDao {
    
    @Query("SELECT * FROM calendar_events ORDER BY eventDate ASC")
    fun getAllEvents(): Flow<List<CalendarEventEntity>>
    
    @Query("SELECT * FROM calendar_events WHERE eventDate BETWEEN :startDate AND :endDate ORDER BY eventDate ASC")
    fun getEventsBetweenDates(startDate: Date, endDate: Date): Flow<List<CalendarEventEntity>>
    
    @Query("SELECT * FROM calendar_events WHERE itemId = :itemId ORDER BY eventDate ASC")
    fun getEventsByItem(itemId: Long): Flow<List<CalendarEventEntity>>
    
    @Query("SELECT * FROM calendar_events WHERE eventType = :eventType ORDER BY eventDate ASC")
    fun getEventsByType(eventType: EventType): Flow<List<CalendarEventEntity>>
    
    @Query("SELECT * FROM calendar_events WHERE priority = :priority ORDER BY eventDate ASC")
    fun getEventsByPriority(priority: Priority): Flow<List<CalendarEventEntity>>
    
    @Query("SELECT * FROM calendar_events WHERE isCompleted = 0 ORDER BY eventDate ASC")
    fun getPendingEvents(): Flow<List<CalendarEventEntity>>
    
    @Query("SELECT * FROM calendar_events WHERE isCompleted = 0 AND eventDate <= :date ORDER BY eventDate ASC")
    fun getOverdueEvents(date: Date): Flow<List<CalendarEventEntity>>
    
    @Query("SELECT * FROM calendar_events WHERE isCompleted = 0 AND eventDate BETWEEN :startDate AND :endDate ORDER BY eventDate ASC")
    fun getUpcomingEvents(startDate: Date, endDate: Date): Flow<List<CalendarEventEntity>>
    
    @Query("SELECT * FROM calendar_events WHERE id = :eventId")
    suspend fun getEventById(eventId: Long): CalendarEventEntity?
    
    @Insert
    suspend fun insertEvent(event: CalendarEventEntity): Long
    
    @Insert
    suspend fun insertEvents(events: List<CalendarEventEntity>): List<Long>
    
    @Update
    suspend fun updateEvent(event: CalendarEventEntity)
    
    @Delete
    suspend fun deleteEvent(event: CalendarEventEntity)
    
    @Query("DELETE FROM calendar_events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: Long)
    
    @Query("DELETE FROM calendar_events WHERE itemId = :itemId")
    suspend fun deleteEventsByItem(itemId: Long)
    
    @Query("UPDATE calendar_events SET isCompleted = 1, completedDate = :completedDate WHERE id = :eventId")
    suspend fun markEventCompleted(eventId: Long, completedDate: Date = Date())
    
    @Query("UPDATE calendar_events SET isCompleted = 0, completedDate = NULL WHERE id = :eventId")
    suspend fun markEventPending(eventId: Long)
    
    // 统计查询
    @Query("SELECT COUNT(*) FROM calendar_events WHERE isCompleted = 0")
    suspend fun getPendingEventsCount(): Int
    
    @Query("SELECT COUNT(*) FROM calendar_events WHERE isCompleted = 0 AND eventDate <= :date")
    suspend fun getOverdueEventsCount(date: Date): Int
    
    @Query("SELECT COUNT(*) FROM calendar_events WHERE eventType = :eventType AND isCompleted = 0")
    suspend fun getPendingEventsByTypeCount(eventType: EventType): Int
} 