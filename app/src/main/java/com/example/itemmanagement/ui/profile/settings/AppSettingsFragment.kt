package com.example.itemmanagement.ui.profile.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentAppSettingsBinding
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

/**
 * 应用设置Fragment
 * 包含主题切换、通知偏好等设置项
 */
class AppSettingsFragment : Fragment() {

    private var _binding: FragmentAppSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AppSettingsViewModel

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

        observeViewModel()
        setupClickListeners()
    }

    // Toolbar功能移除，导航由MainActivity统一管理

    private fun observeViewModel() {
        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let { updateUI(it) }
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessage()
            }
        }
    }

    private fun updateUI(profile: com.example.itemmanagement.data.entity.UserProfileEntity) {
        binding.apply {
            // 主题设置
            when (profile.preferredTheme) {
                "LIGHT" -> rgTheme.check(R.id.rbThemeLight)
                "DARK" -> rgTheme.check(R.id.rbThemeDark)
                else -> rgTheme.check(R.id.rbThemeAuto)
            }

            // 通知设置
            switchNotifications.isChecked = profile.enableNotifications
            switchSoundEffects.isChecked = profile.enableSoundEffects

            // 其他设置
            switchCompactMode.isChecked = profile.compactModeEnabled
            switchTutorialTips.isChecked = profile.showTutorialTips
            switchShowStats.isChecked = profile.showStatsInProfile

            // 默认设置
            etDefaultUnit.setText(profile.defaultUnit)
            etDefaultCategory.setText(profile.defaultCategory ?: "")
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            // 主题切换
            rgTheme.setOnCheckedChangeListener { _, checkedId ->
                val theme = when (checkedId) {
                    R.id.rbThemeLight -> "LIGHT"
                    R.id.rbThemeDark -> "DARK"
                    else -> "AUTO"
                }
                
                // 先保存设置，再应用主题
                viewModel.updateTheme(theme)
                
                // 提供视觉反馈，然后应用主题
                view?.let { v ->
                    com.example.itemmanagement.ui.utils.Material3Feedback.showInfo(v, "正在切换主题...")
                }
                applyTheme(theme)
            }

            // 通知设置
            switchNotifications.setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateNotifications(isChecked)
            }

            switchSoundEffects.setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateSoundEffects(isChecked)
            }

            // 其他设置
            switchCompactMode.setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateCompactMode(isChecked)
            }

            switchTutorialTips.setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateTutorialTips(isChecked)
            }

            switchShowStats.setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateShowStats(isChecked)
            }

            // 默认值设置
            btnSaveDefaults.setOnClickListener {
                val unit = etDefaultUnit.text.toString().trim()
                val category = etDefaultCategory.text.toString().trim()
                
                if (unit.isNotEmpty()) {
                    viewModel.updateDefaultUnit(unit)
                }
                
                if (category.isNotEmpty()) {
                    viewModel.updateDefaultCategory(category)
                } else {
                    viewModel.updateDefaultCategory(null)
                }
            }

            // 重置设置
            btnResetSettings.setOnClickListener {
                showResetConfirmation()
            }

            // 清理数据
            btnCleanupData.setOnClickListener {
                showCleanupConfirmation()
            }
        }
    }

    private fun applyTheme(theme: String) {
        val mode = when (theme) {
            "LIGHT" -> AppCompatDelegate.MODE_NIGHT_NO
            "DARK" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        
        // 添加Material 3主题切换动画
        view?.let { v ->
            // 播放过渡动画
            val animation = android.view.animation.AnimationUtils.loadAnimation(
                requireContext(), 
                com.example.itemmanagement.R.anim.material3_theme_transition
            )
            
            v.startAnimation(animation)
            
            // 在动画播放时切换主题
            v.postDelayed({
                // 在主线程中应用主题切换
                AppCompatDelegate.setDefaultNightMode(mode)
            }, 200) // 与动画时长同步
        }
    }

    private fun showResetConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("重置设置")
            .setMessage("确定要重置所有应用设置吗？这将清除您的个性化配置。")
            .setPositiveButton("重置") { _, _ ->
                viewModel.resetSettings()
                Toast.makeText(context, "设置已重置", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showCleanupConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("清理数据")
            .setMessage("这将清理回收站中超过30天的已删除物品，无法恢复。")
            .setPositiveButton("清理") { _, _ ->
                viewModel.performDataCleanup()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
