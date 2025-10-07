package com.example.itemmanagement.ui.reminder

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.data.entity.CategoryThresholdEntity
import com.example.itemmanagement.data.entity.ReminderSettingsEntity
import com.example.itemmanagement.data.repository.ReminderSettingsRepository
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.*

class ReminderSettingsFragment : Fragment() {
    
    private val viewModel: ReminderSettingsViewModel by viewModels {
        ReminderSettingsViewModelFactory(
            ReminderSettingsRepository.getInstance(
                (requireActivity().application as ItemManagementApplication).database
            )
        )
    }
    
    // UI组件
    private lateinit var loadingProgressBar: ProgressBar
    
    // 基础设置
    private lateinit var expirationDaysSeekBar: SeekBar
    private lateinit var expirationDaysText: TextView
    private lateinit var includeWarrantySwitch: SwitchMaterial
    private lateinit var notificationTimeButton: Button
    private lateinit var stockReminderSwitch: SwitchMaterial
    private lateinit var pushNotificationSwitch: SwitchMaterial
    private lateinit var inAppReminderSwitch: SwitchMaterial
    private lateinit var quietStartButton: Button
    private lateinit var quietEndButton: Button
    private lateinit var weekendPauseSwitch: SwitchMaterial
    
    // 分类阈值设置
    private lateinit var categoryThresholdsRecyclerView: RecyclerView
    private lateinit var addCategoryButton: Button
    private var categoryThresholdAdapter: CategoryThresholdAdapter? = null
    
    // 操作按钮
    private lateinit var resetButton: Button
    private lateinit var saveButton: Button
    
    private var currentSettings: ReminderSettingsEntity? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reminder_settings, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }
    
    private fun initViews(view: View) {
        // 加载指示器
        loadingProgressBar = view.findViewById(R.id.progressBar)
        
        // 基础设置控件
        expirationDaysSeekBar = view.findViewById(R.id.seekBarExpirationDays)
        expirationDaysText = view.findViewById(R.id.textExpirationDays)
        includeWarrantySwitch = view.findViewById(R.id.switchIncludeWarranty)
        notificationTimeButton = view.findViewById(R.id.buttonNotificationTime)
        stockReminderSwitch = view.findViewById(R.id.switchStockReminder)
        pushNotificationSwitch = view.findViewById(R.id.switchPushNotification)
        inAppReminderSwitch = view.findViewById(R.id.switchInAppReminder)
        quietStartButton = view.findViewById(R.id.buttonQuietStart)
        quietEndButton = view.findViewById(R.id.buttonQuietEnd)
        weekendPauseSwitch = view.findViewById(R.id.switchWeekendPause)
        
        // 分类阈值设置
        categoryThresholdsRecyclerView = view.findViewById(R.id.recyclerViewCategoryThresholds)
        addCategoryButton = view.findViewById(R.id.buttonAddCategory)
        
        // 操作按钮
        resetButton = view.findViewById(R.id.buttonReset)
        saveButton = view.findViewById(R.id.buttonSave)
    }
    
    private fun setupRecyclerView() {
        categoryThresholdAdapter = CategoryThresholdAdapter(
            onUpdateThreshold = { category, minQuantity ->
                viewModel.updateCategoryThreshold(category, minQuantity)
            },
            onDeleteThreshold = { category ->
                viewModel.deleteCategoryThreshold(category)
            },
            onToggleEnabled = { category, enabled ->
                viewModel.toggleCategoryThreshold(category, enabled)
            }
        )
        
        categoryThresholdsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryThresholdAdapter
        }
    }
    
    private fun setupClickListeners() {
        // 到期提前天数
        expirationDaysSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                expirationDaysText.text = "${progress}天"
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.progress?.let { days ->
                    viewModel.updateExpirationAdvanceDays(days)
                }
            }
        })
        
        // 通知时间设置
        notificationTimeButton.setOnClickListener {
            showTimePickerDialog { time ->
                viewModel.updateNotificationTime(time)
            }
        }
        
        // 勿扰时间设置
        quietStartButton.setOnClickListener {
            showTimePickerDialog { time ->
                currentSettings?.let { settings ->
                    val updated = settings.copy(quietHourStart = time)
                    viewModel.updateSettings(updated)
                }
            }
        }
        
        quietEndButton.setOnClickListener {
            showTimePickerDialog { time ->
                currentSettings?.let { settings ->
                    val updated = settings.copy(quietHourEnd = time)
                    viewModel.updateSettings(updated)
                }
            }
        }
        
        // 开关监听
        stockReminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateStockReminderEnabled(isChecked)
        }
        
        pushNotificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            currentSettings?.let { settings ->
                val updated = settings.copy(pushNotificationEnabled = isChecked)
                viewModel.updateSettings(updated)
            }
        }
        
        inAppReminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            currentSettings?.let { settings ->
                val updated = settings.copy(inAppReminderEnabled = isChecked)
                viewModel.updateSettings(updated)
            }
        }
        
        includeWarrantySwitch.setOnCheckedChangeListener { _, isChecked ->
            currentSettings?.let { settings ->
                val updated = settings.copy(includeWarranty = isChecked)
                viewModel.updateSettings(updated)
            }
        }
        
        weekendPauseSwitch.setOnCheckedChangeListener { _, isChecked ->
            currentSettings?.let { settings ->
                val updated = settings.copy(weekendPause = isChecked)
                viewModel.updateSettings(updated)
            }
        }
        
        // 添加分类按钮
        addCategoryButton.setOnClickListener {
            showAddCategoryDialog()
        }
        
        // 重置按钮
        resetButton.setOnClickListener {
            showResetConfirmDialog()
        }
        
        // 保存按钮
        saveButton.setOnClickListener {
            currentSettings?.let { settings ->
                viewModel.validateSettings(settings)?.let { error ->
                    showSnackbar(error)
                    return@setOnClickListener
                }
                showSnackbar("设置已保存")
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.settings.observe(viewLifecycleOwner) { settings ->
            currentSettings = settings
            updateUI(settings)
        }
        
        viewModel.categoryThresholds.observe(viewLifecycleOwner) { thresholds ->
            categoryThresholdAdapter?.submitList(thresholds.filter { it.enabled })
        }
        
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            loadingProgressBar.visibility = if (loading) View.VISIBLE else View.GONE
            saveButton.isEnabled = !loading
        }
        
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                showSnackbar(it)
                viewModel.clearErrorMessage()
            }
        }
        
        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                showSnackbar("保存成功")
                viewModel.clearSaveSuccess()
            }
        }
    }
    
    private fun updateUI(settings: ReminderSettingsEntity) {
        // 更新UI控件值（不触发监听器）
        expirationDaysSeekBar.progress = settings.expirationAdvanceDays
        expirationDaysText.text = "${settings.expirationAdvanceDays}天"
        
        includeWarrantySwitch.isChecked = settings.includeWarranty
        notificationTimeButton.text = "通知时间：${settings.notificationTime}"
        stockReminderSwitch.isChecked = settings.stockReminderEnabled
        pushNotificationSwitch.isChecked = settings.pushNotificationEnabled
        inAppReminderSwitch.isChecked = settings.inAppReminderEnabled
        quietStartButton.text = "开始：${settings.quietHourStart}"
        quietEndButton.text = "结束：${settings.quietHourEnd}"
        weekendPauseSwitch.isChecked = settings.weekendPause
    }
    
    private fun showTimePickerDialog(onTimeSet: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val time = String.format("%02d:%02d", selectedHour, selectedMinute)
                onTimeSet(time)
            },
            hour,
            minute,
            true
        ).show()
    }
    
    private fun showAddCategoryDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_category_threshold, null)
        
        val categoryEditText = dialogView.findViewById<EditText>(R.id.editTextCategory)
        val quantityEditText = dialogView.findViewById<EditText>(R.id.editTextMinQuantity)
        val unitEditText = dialogView.findViewById<EditText>(R.id.editTextUnit)
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("添加分类阈值")
            .setView(dialogView)
            .setPositiveButton("添加") { _, _ ->
                val category = categoryEditText.text.toString().trim()
                val quantityStr = quantityEditText.text.toString().trim()
                val unit = unitEditText.text.toString().trim().ifEmpty { "个" }
                
                if (category.isNotEmpty() && quantityStr.isNotEmpty()) {
                    val quantity = quantityStr.toDoubleOrNull()
                    if (quantity != null && quantity > 0) {
                        val threshold = CategoryThresholdEntity(
                            category = category,
                            minQuantity = quantity,
                            unit = unit,
                            enabled = true
                        )
                        viewModel.addCategoryThreshold(threshold)
                    } else {
                        showSnackbar("请输入有效的数量")
                    }
                } else {
                    showSnackbar("请填写所有必需字段")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showResetConfirmDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("重置设置")
            .setMessage("确定要将所有设置重置为默认值吗？此操作无法撤销。")
            .setPositiveButton("重置") { _, _ ->
                viewModel.resetToDefaults()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showSnackbar(message: String) {
        view?.let { v ->
            Snackbar.make(v, message, Snackbar.LENGTH_SHORT).show()
        }
    }
}
