package com.example.itemmanagement.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.TypeConverters
import com.example.itemmanagement.data.Converters
import com.example.itemmanagement.data.model.EventType
import com.example.itemmanagement.data.model.Priority
import com.example.itemmanagement.data.model.RecurrenceType
import java.util.Date

@Entity(
    tableName = "calendar_events",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId"), Index("eventDate")]
)
@TypeConverters(Converters::class)
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: Long,
    val eventType: EventType,
    val title: String,
    val description: String,
    val eventDate: Date,
    val reminderDays: List<Int>,
    val priority: Priority,
    val isCompleted: Boolean = false,
    val recurrenceType: RecurrenceType? = null,
    val createdDate: Date = Date(),
    val completedDate: Date? = null
) 