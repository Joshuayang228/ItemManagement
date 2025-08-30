package com.example.itemmanagement.ui.export

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.databinding.FragmentDataExportBinding
import com.example.itemmanagement.export.ExportManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/**
 * 数据导出页面
 */
class DataExportFragment : Fragment() {
    
    private var _binding: FragmentDataExportBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: DataExportViewModel by viewModels {
        DataExportViewModelFactory(
            requireActivity().application,
            (requireActivity().application as ItemManagementApplication).repository,
            (requireActivity().application as ItemManagementApplication).warrantyRepository,
            (requireActivity().application as ItemManagementApplication).borrowRepository
        )
    }
    
    // 权限请求
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 权限获取成功，可以继续导出操作
        } else {
            showPermissionDeniedDialog()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDataExportBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupButtons()
        observeViewModel()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    // ==================== UI设置 ====================
    
    /**
     * 设置按钮点击事件
     */
    private fun setupButtons() {
        binding.btnExportAll.setOnClickListener {
            checkPermissionAndExport {
                viewModel.exportAllData()
            }
        }
        
        binding.btnExportItems.setOnClickListener {
            checkPermissionAndExport {
                viewModel.exportItems()
            }
        }
        
        binding.btnExportWarranties.setOnClickListener {
            checkPermissionAndExport {
                viewModel.exportWarranties()
            }
        }
        
        binding.btnExportBorrows.setOnClickListener {
            checkPermissionAndExport {
                viewModel.exportBorrows()
            }
        }
    }
    
    // ==================== 数据观察 ====================
    
    /**
     * 观察ViewModel数据变化
     */
    private fun observeViewModel() {
        // 观察数据统计
        viewModel.dataStatistics.observe(viewLifecycleOwner) { stats ->
            updateStatisticsUI(stats)
            updateButtonsEnabled(stats.hasData)
        }
        
        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // 观察导出结果
        viewModel.exportResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                handleExportResult(it)
                viewModel.clearExportResult()
            }
        }
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 更新统计数据UI
     */
    private fun updateStatisticsUI(stats: DataExportViewModel.DataStatistics) {
        binding.tvItemCount.text = stats.itemCount.toString()
        binding.tvWarrantyCount.text = stats.warrantyCount.toString()
        binding.tvBorrowCount.text = stats.borrowCount.toString()
    }
    
    /**
     * 更新按钮可用状态
     */
    private fun updateButtonsEnabled(hasData: Boolean) {
        binding.btnExportAll.isEnabled = hasData
        binding.btnExportItems.isEnabled = hasData
        binding.btnExportWarranties.isEnabled = hasData
        binding.btnExportBorrows.isEnabled = hasData
        
        if (!hasData) {
            showNoDataMessage()
        }
    }
    
    /**
     * 显示没有数据的提示
     */
    private fun showNoDataMessage() {
        Snackbar.make(binding.root, "暂无数据可导出，请先添加物品、保修或借还记录", Snackbar.LENGTH_LONG)
            .setAction("去添加") {
                findNavController().navigateUp()
            }
            .show()
    }
    
    /**
     * 检查权限并执行导出
     */
    private fun checkPermissionAndExport(exportAction: () -> Unit) {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                // 权限已获取，执行导出
                exportAction()
            }
            
            shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                // 需要向用户解释为什么需要权限
                showPermissionExplanationDialog(exportAction)
            }
            
            else -> {
                // 直接请求权限
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
    
    /**
     * 显示权限说明对话框
     */
    private fun showPermissionExplanationDialog(exportAction: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("需要存储权限")
            .setMessage("为了将导出的文件保存到您的设备，我们需要访问存储权限。这样您就可以在文件管理器中找到导出的CSV文件。")
            .setPositiveButton("授予权限") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 显示权限被拒绝的对话框
     */
    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("权限被拒绝")
            .setMessage("没有存储权限，无法保存导出文件。您可以在应用设置中手动开启权限。")
            .setPositiveButton("知道了", null)
            .show()
    }
    
    /**
     * 处理导出结果
     */
    private fun handleExportResult(result: DataExportViewModel.ExportResult) {
        if (result.success) {
            showSuccessDialog(result)
        } else {
            showErrorMessage(result.message)
        }
    }
    
    /**
     * 显示成功对话框
     */
    private fun showSuccessDialog(result: DataExportViewModel.ExportResult) {
        val message = StringBuilder().apply {
            appendLine(result.message)
            appendLine()
            appendLine("您可以：")
            appendLine("• 在文件管理器中查看导出文件")
            appendLine("• 分享文件到微信、邮件等应用")
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("✅ 导出成功")
            .setMessage(message.toString())
            .setPositiveButton("分享文件") { _, _ ->
                shareFirstExportedFile(result)
            }
            .setNegativeButton("关闭", null)
            .show()
    }
    
    /**
     * 分享第一个导出的文件
     */
    private fun shareFirstExportedFile(result: DataExportViewModel.ExportResult) {
        val firstFile = result.exportedFiles.firstOrNull { it.success }
        firstFile?.let { exportResult ->
            viewModel.shareExportedFile(exportResult, ExportManager.ExportType.ALL)
        }
    }
    
    /**
     * 显示错误消息
     */
    private fun showErrorMessage(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("❌ 导出失败")
            .setMessage(message)
            .setPositiveButton("知道了", null)
            .show()
    }
}
