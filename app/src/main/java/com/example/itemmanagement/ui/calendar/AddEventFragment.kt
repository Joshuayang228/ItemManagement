package com.example.itemmanagement.ui.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.data.entity.CalendarEventEntity
import com.example.itemmanagement.data.model.EventType
import com.example.itemmanagement.data.model.Priority
import com.example.itemmanagement.data.model.RecurrenceType
import com.example.itemmanagement.databinding.FragmentAddEventBinding
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddEventFragment : Fragment() {

    private var _binding: FragmentAddEventBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemCalendarViewModel by viewModels {
        ItemCalendarViewModelFactory(
            (requireActivity().application as ItemManagementApplication).repository
        )
    }

    private var selectedDate: Date = Date()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinners()
        setupDateTimePickers()
        setupButtons()
        
        // 初始化日期时间显示
        updateDateTimeDisplay()
    }

    private fun setupSpinners() {
        // 事件类型下拉菜单
        val eventTypes = EventType.values()
        val eventTypeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            eventTypes.map { it.displayName }
        )
        eventTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.eventTypeSpinner.adapter = eventTypeAdapter

        // 优先级下拉菜单
        val priorities = Priority.values()
        val priorityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            priorities.map { it.displayName }
        )
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.prioritySpinner.adapter = priorityAdapter

        // 周期性下拉菜单
        val recurrenceTypes = listOf("无") + RecurrenceType.values().map { it.displayName }
        val recurrenceAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            recurrenceTypes
        )
        recurrenceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.recurrenceSpinner.adapter = recurrenceAdapter
    }

    private fun setupDateTimePickers() {
        binding.dateButton.setOnClickListener {
            showDatePicker()
        }

        binding.timeButton.setOnClickListener {
            showTimePicker()
        }
    }

    private fun setupButtons() {
        binding.saveButton.setOnClickListener {
            saveEvent()
        }

        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance()
                newCalendar.time = selectedDate
                newCalendar.set(Calendar.YEAR, year)
                newCalendar.set(Calendar.MONTH, month)
                newCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                selectedDate = newCalendar.time
                updateDateTimeDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate

        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val newCalendar = Calendar.getInstance()
                newCalendar.time = selectedDate
                newCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                newCalendar.set(Calendar.MINUTE, minute)
                selectedDate = newCalendar.time
                updateDateTimeDisplay()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun updateDateTimeDisplay() {
        binding.dateButton.text = dateFormat.format(selectedDate)
        binding.timeButton.text = timeFormat.format(selectedDate)
    }

    private fun saveEvent() {
        val title = binding.titleEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()

        if (title.isEmpty()) {
            Snackbar.make(binding.root, "请输入事件标题", Snackbar.LENGTH_SHORT).show()
            return
        }

        // 获取选中的值
        val eventType = EventType.values()[binding.eventTypeSpinner.selectedItemPosition]
        val priority = Priority.values()[binding.prioritySpinner.selectedItemPosition]
        
        val recurrenceType = if (binding.recurrenceSpinner.selectedItemPosition == 0) {
            null
        } else {
            RecurrenceType.values()[binding.recurrenceSpinner.selectedItemPosition - 1]
        }

        // 解析提醒天数
        val reminderDaysText = binding.reminderDaysEditText.text.toString().trim()
        val reminderDays = if (reminderDaysText.isEmpty()) {
            listOf(1) // 默认提前1天提醒
        } else {
            reminderDaysText.split(",").mapNotNull { it.trim().toIntOrNull() }
        }

        // 创建事件实体
        val event = CalendarEventEntity(
            itemId = 0, // 自定义事件不关联物品
            eventType = eventType,
            title = title,
            description = description,
            eventDate = selectedDate,
            reminderDays = reminderDays,
            priority = priority,
            recurrenceType = recurrenceType
        )

        // 保存事件
        viewModel.addCustomEvent(event)

        // 显示成功消息并返回
        Snackbar.make(binding.root, "事件已添加", Snackbar.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 