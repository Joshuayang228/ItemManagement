package com.example.itemmanagement.ui.utils

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*

/**
 * Material 3 DatePicker 工具类
 * 提供统一的Material You风格日期选择功能
 */
object Material3DatePicker {
    
    private const val TAG = "Material3DatePicker"
    
    /**
     * 显示Material 3日期选择器
     * 
     * @param fragmentManager Fragment管理器
     * @param title 选择器标题
     * @param selectedDate 当前选择的日期（可选）
     * @param minDate 最小可选日期（可选）
     * @param maxDate 最大可选日期（可选）
     * @param onDateSelected 日期选择回调，参数为选择的Date对象
     */
    fun showDatePicker(
        fragmentManager: FragmentManager,
        title: String = "选择日期",
        selectedDate: Date? = null,
        minDate: Date? = null,
        maxDate: Date? = null,
        onDateSelected: (Date) -> Unit
    ) {
        // 构建约束条件
        val constraintsBuilder = CalendarConstraints.Builder()
        
        minDate?.let { 
            constraintsBuilder.setStart(it.time)
        }
        
        maxDate?.let { 
            constraintsBuilder.setEnd(it.time)
        }
        
        // 构建DatePicker
        val datePickerBuilder = MaterialDatePicker.Builder.datePicker()
            .setTitleText(title)
            .setTheme(com.google.android.material.R.style.ThemeOverlay_Material3_MaterialCalendar)
            .setCalendarConstraints(constraintsBuilder.build())
        
        // 设置默认选择日期
        selectedDate?.let { 
            datePickerBuilder.setSelection(it.time)
        }
        
        val datePicker = datePickerBuilder.build()
        
        // 设置选择监听器
        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDate = Date(selection)
            onDateSelected(selectedDate)
        }
        
        // 显示选择器
        datePicker.show(fragmentManager, TAG)
    }
    
    /**
     * 显示未来日期选择器（最小日期为明天）
     * 适用于借出归还日期等场景
     */
    fun showFutureDatePicker(
        fragmentManager: FragmentManager,
        title: String = "选择日期",
        selectedDate: Date? = null,
        onDateSelected: (Date) -> Unit
    ) {
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        showDatePicker(
            fragmentManager = fragmentManager,
            title = title,
            selectedDate = selectedDate,
            minDate = tomorrow,
            onDateSelected = onDateSelected
        )
    }
    
    /**
     * 显示过去和未来日期选择器
     * 适用于生产日期、购买日期等场景
     */
    fun showAnyDatePicker(
        fragmentManager: FragmentManager,
        title: String = "选择日期",
        selectedDate: Date? = null,
        onDateSelected: (Date) -> Unit
    ) {
        showDatePicker(
            fragmentManager = fragmentManager,
            title = title,
            selectedDate = selectedDate,
            minDate = null,
            maxDate = null,
            onDateSelected = onDateSelected
        )
    }
    
    /**
     * 将日期格式化为显示字符串
     */
    fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(date)
    }
    
    /**
     * 将日期格式化为中文显示
     */
    fun formatDateChinese(date: Date): String {
        val formatter = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINESE)
        return formatter.format(date)
    }
}

/**
 * Fragment扩展函数 - 显示Material 3日期选择器
 */
fun Fragment.showMaterial3DatePicker(
    title: String = "选择日期",
    selectedDate: Date? = null,
    minDate: Date? = null,
    maxDate: Date? = null,
    onDateSelected: (Date) -> Unit
) {
    Material3DatePicker.showDatePicker(
        fragmentManager = parentFragmentManager,
        title = title,
        selectedDate = selectedDate,
        minDate = minDate,
        maxDate = maxDate,
        onDateSelected = onDateSelected
    )
}

/**
 * Fragment扩展函数 - 显示未来日期选择器
 */
fun Fragment.showFutureDatePicker(
    title: String = "选择归还日期",
    selectedDate: Date? = null,
    onDateSelected: (Date) -> Unit
) {
    Material3DatePicker.showFutureDatePicker(
        fragmentManager = parentFragmentManager,
        title = title,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected
    )
}

/**
 * FragmentActivity扩展函数 - 显示Material 3日期选择器
 */
fun FragmentActivity.showMaterial3DatePicker(
    title: String = "选择日期",
    selectedDate: Date? = null,
    minDate: Date? = null,
    maxDate: Date? = null,
    onDateSelected: (Date) -> Unit
) {
    Material3DatePicker.showDatePicker(
        fragmentManager = supportFragmentManager,
        title = title,
        selectedDate = selectedDate,
        minDate = minDate,
        maxDate = maxDate,
        onDateSelected = onDateSelected
    )
}
