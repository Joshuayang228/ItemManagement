package com.example.itemmanagement.ui.profile.settings

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentAppSettingsBinding
import com.example.itemmanagement.data.repository.ReminderSettingsRepository
import com.example.itemmanagement.ui.reminder.ReminderSettingsViewModel
import com.example.itemmanagement.ui.reminder.ReminderSettingsViewModelFactory
import com.example.itemmanagement.ui.reminder.CategoryThresholdAdapter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 应用设置Fragment
 * 简化版：不使用LiveData观察，直接控制UI
 */
class AppSettingsFragment : Fragment() {

    private var _binding: FragmentAppSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AppSettingsViewModel
    
    // 提醒设置 ViewModel
    private val reminderViewModel: ReminderSettingsViewModel by viewModels {
        ReminderSettingsViewModelFactory(
            ReminderSettingsRepository.getInstance(
                (requireActivity().application as ItemManagementApplication).database
            )
        )
    }
    
    // RecyclerView 适配器
    private var categoryThresholdAdapter: CategoryThresholdAdapter? = null
    
    // 防抖：避免快速点击导致重复触发
    private var lastClickTime = 0L
    private val DEBOUNCE_DELAY = 300L
    
    // 标志位：正在初始化UI时忽略监听器触发
    private var isInitializing = true
    
    // 标志位：监听器是否已经设置
    private var isListenerSet = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppSettingsBinding.inflate(inflater, container, false)
        val application = requireActivity().application as ItemManagementApplication
        val userProfileRepository = application.userProfileRepository
        val factory = AppSettingsViewModelFactory(userProfileRepository)
        viewModel = ViewModelProvider(this, factory).get(AppSettingsViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        hideBottomNavigation()
        
        val isRecreated = savedInstanceState != null
        
        // 初始化 RecyclerView
        setupRecyclerView()
        
        // 观察 ViewModel
        observeViewModel()
        
        // 初始化UI（在协程中完成后才设置监听器）
        initializeUI(isRecreated)
    }

    override fun onResume() {
        super.onResume()
        hideBottomNavigation()
    }
    
    /**
     * 设置 RecyclerView
     */
    private fun setupRecyclerView() {
        categoryThresholdAdapter = CategoryThresholdAdapter(
            onUpdateThreshold = { category, minQuantity ->
                reminderViewModel.updateCategoryThreshold(category, minQuantity)
            },
            onDeleteThreshold = { category ->
                reminderViewModel.deleteCategoryThreshold(category)
            },
            onToggleEnabled = { category, enabled ->
                reminderViewModel.toggleCategoryThreshold(category, enabled)
            }
        )
        
        binding.recyclerViewCategoryThresholds.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryThresholdAdapter
        }
    }
    
    /**
     * 观察 ViewModel 数据变化
     */
    private fun observeViewModel() {
        reminderViewModel.settings.observe(viewLifecycleOwner) { settings ->
            // 更新 UI
            binding.apply {
                seekBarExpirationDays.progress = settings.expirationAdvanceDays
                textExpirationDays.text = "${settings.expirationAdvanceDays}天"
                switchIncludeWarranty.isChecked = settings.includeWarranty
                switchStockReminder.isChecked = settings.stockReminderEnabled
                switchPushNotification.isChecked = settings.pushNotificationEnabled
                switchInAppReminder.isChecked = settings.inAppReminderEnabled
                textNotificationTime.text = settings.notificationTime
                textQuietStart.text = settings.quietHourStart
                textQuietEnd.text = settings.quietHourEnd
                switchWeekendPause.isChecked = settings.weekendPause
                
                // 根据库存提醒开关动态显示/隐藏库存阈值容器
                layoutCategoryThresholds.visibility = if (settings.stockReminderEnabled) View.VISIBLE else View.GONE
            }
        }
        
        reminderViewModel.categoryThresholds.observe(viewLifecycleOwner) { thresholds ->
            categoryThresholdAdapter?.submitList(thresholds.filter { it.enabled })
        }
        
        // 加载已有物品的分类并自动创建阈值
        lifecycleScope.launch {
            try {
                reminderViewModel.ensureCategoryThresholdsFromItems()
            } catch (e: Exception) {
                android.util.Log.e("AppSettings", "加载分类失败: ${e.message}")
            }
        }
    }
    
    /**
     * 初始化UI：从数据库读取主题设置
     * @param isRecreated 是否是配置更改后重建（如主题切换导致的Activity重建）
     */
    private fun initializeUI(isRecreated: Boolean) {
        lifecycleScope.launch {
            // 标记开始初始化
            isInitializing = true
            
            val theme = viewModel.getTheme()
            
            // 设置UI状态
            setThemeRadioButton(theme)
            
            // 初始化完成后，才设置监听器
            setupClickListeners()
            
            // 标记初始化完成
            isInitializing = false
            
            // 只在非重建时应用主题
            // Activity因主题切换而重建时，系统已经应用了新主题，无需重复应用
            if (!isRecreated) {
                applyTheme(theme)
            }
        }
    }
    
    /**
     * 设置RadioButton选中状态（不会触发监听器，因为此时监听器还未设置）
     */
    private fun setThemeRadioButton(theme: String) {
        // 设置选中状态
        val targetId = when (theme) {
            "LIGHT" -> R.id.rbThemeLight
            "DARK" -> R.id.rbThemeDark
            else -> R.id.rbThemeAuto
        }
        
        // 直接check，不用担心触发监听器（因为还没设置）
        if (binding.rgTheme.checkedRadioButtonId != targetId) {
            binding.rgTheme.check(targetId)
        }
    }
    
    private fun getThemeName(id: Int): String {
        return when (id) {
            R.id.rbThemeLight -> "LIGHT"
            R.id.rbThemeDark -> "DARK"
            R.id.rbThemeAuto -> "AUTO"
            else -> "UNKNOWN"
        }
    }
    
    /**
     * 主题切换监听器
     */
    private val themeChangeListener = RadioGroup.OnCheckedChangeListener { _, checkedId ->
        // 如果正在初始化，忽略所有触发（双重保险）
        if (isInitializing) {
            return@OnCheckedChangeListener
        }
        
        // 防抖：300ms内的重复点击忽略
        val now = System.currentTimeMillis()
        if (now - lastClickTime < DEBOUNCE_DELAY) {
            return@OnCheckedChangeListener
        }
        lastClickTime = now
        
        // 获取选中的主题
        val theme = when (checkedId) {
            R.id.rbThemeLight -> "LIGHT"
            R.id.rbThemeDark -> "DARK"
            else -> "AUTO"
        }
        
        // 在协程中保存并应用主题
        lifecycleScope.launch {
            // 先同步保存到数据库（确保保存完成）
            viewModel.saveThemeSync(theme)
            
            // 再应用主题（会触发Activity重建）
            applyTheme(theme)
            
            // 显示提示
            Toast.makeText(context, "主题已切换", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 设置点击监听器（只在初始化完成后调用，且只设置一次）
     */
    private fun setupClickListeners() {
        if (isListenerSet) {
            return
        }
        
        binding.apply {
            // 先移除可能存在的旧监听器
            rgTheme.setOnCheckedChangeListener(null)
            
            // 主题切换监听器
            rgTheme.setOnCheckedChangeListener(themeChangeListener)
            
            // 重置设置按钮
            btnResetSettings.setOnClickListener {
                showResetConfirmation()
            }
            
            // === 提醒设置监听器 ===
            
            // 到期提前天数 SeekBar
            seekBarExpirationDays.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        textExpirationDays.text = "${progress}天"
                    }
                }
                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                    seekBar?.let {
                        reminderViewModel.updateExpirationAdvanceDays(it.progress)
                    }
                }
            })
            
            // 保修期提醒
            switchIncludeWarranty.setOnCheckedChangeListener { _, isChecked ->
                reminderViewModel.updateIncludeWarranty(isChecked)
            }
            
            // 库存提醒
            switchStockReminder.setOnCheckedChangeListener { _, isChecked ->
                reminderViewModel.updateStockReminder(isChecked)
                // 动态显示/隐藏库存阈值容器
                layoutCategoryThresholds.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
            
            // 推送通知
            switchPushNotification.setOnCheckedChangeListener { _, isChecked ->
                reminderViewModel.updatePushNotification(isChecked)
            }
            
            // 应用内提醒
            switchInAppReminder.setOnCheckedChangeListener { _, isChecked ->
                reminderViewModel.updateInAppReminder(isChecked)
            }
            
            // 周末暂停
            switchWeekendPause.setOnCheckedChangeListener { _, isChecked ->
                reminderViewModel.updateWeekendPause(isChecked)
            }
            
            // 通知时间（点击整行或时间文本）
            layoutNotificationTime.setOnClickListener {
                showTimePicker { time ->
                    reminderViewModel.updateNotificationTime(time)
                }
            }
            
            // 免打扰开始时间
            textQuietStart.setOnClickListener {
                showTimePicker { time ->
                    reminderViewModel.updateQuietHourStart(time)
                }
            }
            
            // 免打扰结束时间
            textQuietEnd.setOnClickListener {
                showTimePicker { time ->
                    reminderViewModel.updateQuietHourEnd(time)
                }
            }
        }
        
        isListenerSet = true
    }
    
    /**
     * 应用主题到系统
     */
    private fun applyTheme(theme: String) {
        val mode = when (theme) {
            "LIGHT" -> AppCompatDelegate.MODE_NIGHT_NO
            "DARK" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
    
    /**
     * 显示重置确认对话框
     */
    private fun showResetConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("重置设置")
            .setMessage("确定要将所有设置重置为默认值吗？")
            .setPositiveButton("重置") { _, _ ->
                lifecycleScope.launch {
                    // 重置到AUTO模式
                    viewModel.resetSettings()
                    setThemeRadioButton("AUTO")
                    applyTheme("AUTO")
                    Toast.makeText(context, "设置已重置", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 显示时间选择器
     */
    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val timeString = String.format("%02d:%02d", selectedHour, selectedMinute)
                onTimeSelected(timeString)
            },
            hour,
            minute,
            true
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        showBottomNavigation()
        categoryThresholdAdapter = null
        _binding = null
    }

    /**
     * 隐藏底部导航栏
     */
    private fun hideBottomNavigation() {
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.GONE
    }

    /**
     * 显示底部导航栏
     */
    private fun showBottomNavigation() {
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.VISIBLE
    }
}
 