package com.example.itemmanagement.ui.add

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R

import com.example.itemmanagement.ui.add.Field
import com.example.itemmanagement.ui.base.BaseItemFragment

/**
 * 新的添加物品 Fragment
 * 
 * 使用新的 ViewModel 架构，具有以下特性：
 * 1. 独立的 ViewModel 实例（Fragment 作用域）
 * 2. 智能的状态缓存和恢复
 * 3. 与编辑模式完全隔离的数据
 */
class AddItemFragment : BaseItemFragment<AddItemViewModel>() {

    // 获取添加物品专用的 ViewModel
    override val viewModel: AddItemViewModel by viewModels {
        val app = (requireActivity().application as ItemManagementApplication)
        val repository = app.repository
        val warrantyRepository = app.warrantyRepository
        AddItemViewModelFactory(repository, cacheViewModel, warrantyRepository)
    }

    override fun onViewModelReady() {
        // ViewModel 已准备就绪，可以进行初始化
        // 初始化默认字段
        initializeDefaultFields()
        
        // 启用菜单
        setHasOptionsMenu(true)
    }

    /**
     * 初始化默认字段
     */
    private fun initializeDefaultFields() {
        // 创建默认的字段集合 - 增加位置和备注为默认选中
        // 使用Field类的默认order，不手动指定order，保持一致性
        val defaultFields = setOf(
            Field("基础信息", "名称", true),
            Field("基础信息", "数量", true),
            Field("基础信息", "位置", true),
            Field("其他", "备注", true),
            Field("分类", "分类", true),
            Field("分类", "标签", true),  // 添加标签为默认选中
            Field("日期类", "添加日期", true)
        )
        
        // 设置默认字段
        defaultFields.forEach { field ->
            viewModel.updateFieldSelection(field, field.isSelected)
        }
    }

    override fun setupTitleAndButtons() {
        // 这个布局没有 titleText，所以我们可以设置 Activity 标题
        activity?.title = "添加物品"
        
        // 设置按钮文本
        binding.saveButton.text = "保存物品"
        binding.editFieldsButton.text = "编辑字段"
    }

    override fun setupButtons() {
        // 保存按钮
        binding.saveButton.setOnClickListener {
            performSave()
        }
        
        // 编辑字段按钮（使用原有的EditFieldsFragment）
        binding.editFieldsButton.setOnClickListener {
            showEditFieldsDialog()
        }
    }

    /**
     * 显示清空确认对话框
     */
    private fun showClearConfirmDialog() {
        dialogFactory.createConfirmDialog(
            title = "确认清空",
            message = "确定要清空所有已输入的内容吗？\n注意：这只会清空字段值，不会改变字段选择状态。",
            positiveButtonText = "确定",
            negativeButtonText = "取消",
            onPositiveClick = {
                clearAllFields()
            }
        )
    }

    /**
     * 显示退出确认对话框
     */
    private fun showExitConfirmDialog() {
        // 检查是否有未保存的内容
        if (hasUnsavedContent()) {
            dialogFactory.createConfirmDialog(
                title = "确认退出",
                message = "您有未保存的内容，确定要退出吗？内容将会自动保存为草稿。",
                positiveButtonText = "退出",
                negativeButtonText = "继续编辑",
                onPositiveClick = {
                    // 数据会自动保存到缓存，直接退出
                    activity?.onBackPressed()
                }
            )
        } else {
            activity?.onBackPressed()
        }
    }

    /**
     * 显示编辑字段对话框（使用原有的EditFieldsFragment）
     */
    private fun showEditFieldsDialog() {
        // 在显示编辑字段对话框前，先保存当前字段的值
        if (fieldViews.isNotEmpty()) {
            fieldValueManager.saveFieldValues(fieldViews)
        }
        
        // 使用新架构的EditFieldsFragment（不需要Activity级别的ViewModel兼容性）
        val editFieldsFragment = EditFieldsFragment.newInstance(viewModel, false)
        
        // 显示EditFieldsFragment（新架构直接操作本Fragment的ViewModel，不需要生命周期观察者）
        editFieldsFragment.show(childFragmentManager, "EditFieldsFragment")
    }
    


    /**
     * 获取可用的字段列表
     */
    private fun getAvailableFields(): List<Field> {
        return listOf(
            Field("基础信息", "名称", true),
            Field("基础信息", "数量", false),
            Field("基础信息", "位置", false),
            Field("基础信息", "备注", false),
            Field("分类", "分类", false),
            Field("分类", "子分类", false),
            Field("分类", "标签", false),
            Field("分类", "季节", false),
            Field("数字类", "容量", false),
            Field("数字类", "评分", false),
            Field("数字类", "单价", false),
            Field("数字类", "总价", false),
            Field("日期类", "添加日期", false),
            Field("日期类", "开封时间", false),
            Field("日期类", "购买日期", false),
            Field("日期类", "生产日期", false),
            Field("日期类", "保质期", false),
            Field("日期类", "保质过期时间", false),
            Field("日期类", "保修期", false),
            Field("日期类", "保修到期时间", false),
            Field("商业类", "品牌", false),
            Field("商业类", "购买渠道", false),
            Field("商业类", "商家名称", false),
            Field("商业类", "序列号", false),
            Field("其他", "加入心愿单", false),
            Field("其他", "高周转", false)
        )
    }

    /**
     * 清空所有字段值（但保留字段选择）
     */
    private fun clearAllFields() {
        // 检查Fragment是否还存活，避免在Fragment销毁后操作UI
        if (!isAdded || _binding == null) {
            android.util.Log.w("AddItemFragment", "尝试在Fragment销毁后清空字段，跳过操作")
            return
        }
        
        // 只清空字段值和照片，保留字段选择状态
        viewModel.clearFieldValuesOnly()
        
        // 重新设置默认值（特别是添加日期）
        initializeDefaultValues()
        
        // 刷新UI
        binding.fieldsContainer.removeAllViews()
        fieldViews.clear()
        
        // 清空照片
        photoAdapter.clearPhotos()
        
        // 重新生成UI（基于现有的字段选择）
        recreateFieldViews()
    }

    /**
     * 初始化默认值
     */
    private fun initializeDefaultValues() {
        // 为添加日期字段设置默认值为当前日期
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val currentDate = dateFormat.format(java.util.Date())
        viewModel.saveFieldValue("添加日期", currentDate)
    }

    /**
     * 重新创建字段视图
     */
    private fun recreateFieldViews() {
        // 触发UI重新生成
        setupFields()
    }

    /**
     * 检查是否有未保存的内容
     */
    private fun hasUnsavedContent(): Boolean {
        val fieldValues = viewModel.getAllFieldValues()
        val hasFieldValues = fieldValues.isNotEmpty() && fieldValues.values.any { 
            it != null && it.toString().isNotBlank() 
        }
        val hasPhotos = viewModel.getPhotoUris().isNotEmpty()
        
        return hasFieldValues || hasPhotos
    }

    override fun onSaveSuccess() {
        super.onSaveSuccess()
        // 添加物品成功后，可以选择清空表单继续添加，或者返回
        dialogFactory.createConfirmDialog(
            title = "添加成功",
            message = "物品已成功添加到库存。是否继续添加其他物品？",
            positiveButtonText = "继续添加",
            negativeButtonText = "返回",
            onPositiveClick = {
                // 检查Fragment是否还存活，避免内存泄漏和崩溃
                if (isAdded && activity != null) {
                    // 导航到新的添加物品页面
                    navigateToNewAddItem()
                }
            },
            onNegativeClick = {
                // 检查Activity是否还存活
                if (isAdded && activity != null) {
                    // 返回上一页
                    activity?.onBackPressed()
                }
            }
        )
    }

    override fun onSaveFailure() {
        super.onSaveFailure()
        // 保存失败的额外处理（如果需要）
    }

    // === 菜单相关方法 ===
    
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_add_item, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_test_fill -> {
                // 处理测试填充
                handleTestFill()
                true
            }
            R.id.action_scan -> {
                // 处理扫描条码
                handleScanBarcode()
                true
            }
            R.id.action_camera -> {
                // 处理拍照
                handleTakePhoto()
                true
            }
            R.id.action_clear -> {
                // 处理清除
                showClearConfirmDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * 处理测试填充
     */
    private fun handleTestFill() {
        // 基础信息
        viewModel.saveFieldValue("名称", "测试物品 ${System.currentTimeMillis() % 1000}")
        viewModel.saveFieldValue("数量", "5")
        viewModel.saveFieldValue("数量_unit", "瓶")  // 数量单位
        
        // 位置信息 - 设置完整的位置结构
        viewModel.saveFieldValue("位置", "厨房-冰箱-冷藏室")
        viewModel.saveFieldValue("位置_area", "厨房")
        viewModel.saveFieldValue("位置_container", "冰箱")
        viewModel.saveFieldValue("位置_sublocation", "冷藏室")
        
        viewModel.saveFieldValue("备注", "这是一个测试物品，用于功能验证")
        viewModel.saveFieldValue("分类", "食品")
        viewModel.saveFieldValue("子分类", "饮料")
        viewModel.saveFieldValue("品牌", "测试品牌")
        
        // 价格信息（带单位）
        viewModel.saveFieldValue("单价", "12.5")
        viewModel.saveFieldValue("单价_unit", "元")  // 单价单位
        viewModel.saveFieldValue("总价", "62.5")
        viewModel.saveFieldValue("总价_unit", "元")  // 总价单位
        
        // 容量信息（带单位）
        viewModel.saveFieldValue("容量", "500")
        viewModel.saveFieldValue("容量_unit", "毫升")  // 容量单位
        
        viewModel.saveFieldValue("评分", "4.5")
        viewModel.saveFieldValue("购买渠道", "超市")
        viewModel.saveFieldValue("商家名称", "测试超市")
        viewModel.saveFieldValue("序列号", "TEST${System.currentTimeMillis()}")
        
        // 日期字段
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val today = dateFormat.format(java.util.Date())
        val futureDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000)) // 一年后
        
        viewModel.saveFieldValue("添加日期", today)
        viewModel.saveFieldValue("购买日期", today)
        viewModel.saveFieldValue("生产日期", "2024-01-01")
        viewModel.saveFieldValue("保质过期时间", "2025-12-31")
        viewModel.saveFieldValue("开封时间", today)  // 开封日期
        viewModel.saveFieldValue("保修到期时间", futureDate)  // 保修到期时间
        
        // 标签和季节
        viewModel.updateSelectedTags("标签", setOf("测试", "重要", "常用"))
        viewModel.saveFieldValue("季节", setOf("春", "夏"))
        
        // 开封状态
        viewModel.saveFieldValue("开封状态", "未开封")
        
        // 期限字段（数字 + 单位）
        viewModel.saveFieldValue("保质期", "24")
        viewModel.saveFieldValue("保质期_unit", "月")  // 保质期单位
        viewModel.saveFieldValue("保修期", "12")
        viewModel.saveFieldValue("保修期_unit", "月")  // 保修期单位
        
        // 其他布尔字段
        viewModel.saveFieldValue("加入心愿单", false)
        viewModel.saveFieldValue("高周转", true)
        
        // 库存预警
        viewModel.saveFieldValue("库存预警值", "2")
        
        // 重新创建字段视图以显示填充的数据
        recreateFieldViews()
        
        // 显示提示
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("测试填充完成")
            .setMessage("已自动填充完整的测试数据，包括位置、单位、日期等所有字段。")
            .setPositiveButton("确定", null)
            .show()
    }

    /**
     * 处理扫描条码
     */
    private fun handleScanBarcode() {
        // TODO: 实现条码扫描功能
        // 暂时创建一个简单的信息对话框
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("扫描条码")
            .setMessage("条码扫描功能暂未实现，敬请期待！")
            .setPositiveButton("确定", null)
            .show()
    }

    /**
     * 处理拍照
     */
    private fun handleTakePhoto() {
        // TODO: 实现拍照功能
        // 暂时创建一个简单的信息对话框
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("拍照")
            .setMessage("拍照功能暂未实现，您可以通过点击照片区域来添加照片！")
            .setPositiveButton("确定", null)
            .show()
    }

    /**
     * 导航到新的添加物品页面
     */
    private fun navigateToNewAddItem() {
        try {
            // 方法1：尝试重新创建Fragment - 返回主页面再进入添加页面
            findNavController().popBackStack()
            
        } catch (e: Exception) {
            // 方法2：如果导航失败，回退到清空当前页面的方式，但提供更好的用户体验
            android.util.Log.w("AddItemFragment", "导航失败，使用清空页面方式: ${e.message}")
            if (isAdded && _binding != null) {
                // 先显示"正在准备新的添加页面"的提示
                android.widget.Toast.makeText(requireContext(), "正在准备新的添加页面...", android.widget.Toast.LENGTH_SHORT).show()
                
                // 延迟一下再清空，给用户更好的反馈
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (isAdded && _binding != null) {
                        clearAllFields()
                        android.widget.Toast.makeText(requireContext(), "已准备好新的添加页面", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }, 500)
            }
        }
    }
} 