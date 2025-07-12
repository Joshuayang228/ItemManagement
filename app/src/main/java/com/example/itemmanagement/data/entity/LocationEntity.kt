package com.example.itemmanagement.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val area: String,
    val container: String?,
    val sublocation: String?
) {
    /**
     * 获取完整位置字符串
     * @return 格式化的位置字符串，如："厨房 > 冰箱 > 第二层"
     */
    fun getFullLocationString(): String {
        return buildString {
            append(area)
            if (!container.isNullOrBlank()) {
                append(" > ").append(container)
                if (!sublocation.isNullOrBlank()) {
                    append(" > ").append(sublocation)
                }
            }
        }
    }
} 