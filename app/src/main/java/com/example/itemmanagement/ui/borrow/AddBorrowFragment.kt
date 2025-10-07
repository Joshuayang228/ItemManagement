package com.example.itemmanagement.ui.borrow

import com.example.itemmanagement.ui.utils.showFutureDatePicker
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.data.relation.ItemWithDetails
import com.example.itemmanagement.databinding.FragmentAddBorrowBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 添加借出记录Fragment
 * 提供添加借出记录的表单界面
 */
class AddBorrowFragment : Fragment() {

    private var _binding: FragmentAddBorrowBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AddBorrowViewModel by viewModels {
        AddBorrowViewModelFactory(
            (requireActivity().application as ItemManagementApplication).borrowRepository,
            (requireActivity().application as ItemManagementApplication).repository
        )
    }
    
    private lateinit var itemAdapter: ArrayAdapter<ItemDisplayWrapper>
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBorrowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupFormInputs()
        setupButtons()
        observeViewModel()
        
        // 检查是否为编辑模式
        val borrowId = arguments?.getLong("borrowId", -1L) ?: -1L
        if (borrowId > 0) {
            // 编辑模式：加载现有借还记录
            viewModel.loadBorrowRecord(borrowId)
        } else {
            // 新建模式：检查是否有预选择的物品
            arguments?.getLong("preSelectedItemId", -1L)?.let { itemId ->
                if (itemId > 0) {
                    viewModel.preSelectItem(itemId)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 设置工具栏
     */
    // Toolbar功能移除，导航由MainActivity统一管理

    /**
     * 设置表单输入
     */
    private fun setupFormInputs() {
        // 设置物品选择下拉列表
        setupItemSelector()
        
        // 设置日期选择器
        setupDateSelector()
        
        // 设置文本变化监听
        setupTextWatchers()
    }

    /**
     * 设置物品选择器
     */
    private fun setupItemSelector() {
        itemAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf<ItemDisplayWrapper>()
        )
        
        binding.itemSelectionLayout.setOnClickListener {
            showItemSelectionDialog()
        }
    }
    
    /**
     * 显示物品选择对话框
     */
    private fun showItemSelectionDialog() {
        val items = viewModel.availableItems.value ?: emptyList()
        if (items.isEmpty()) {
            Toast.makeText(requireContext(), "暂无可借出的物品", Toast.LENGTH_SHORT).show()
            return
        }
        
        val itemNames = items.map { "${it.item.name} (${it.item.category})" }.toTypedArray()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择物品")
            .setItems(itemNames) { _, which ->
                viewModel.setSelectedItem(items[which])
            }
            .setNegativeButton("取消") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    /**
     * 设置日期选择器
     */
    private fun setupDateSelector() {
        binding.returnDateLayout.setOnClickListener {
            showDatePicker()
        }
        
        // 显示当前选择的日期
        viewModel.expectedReturnDate.value?.let { date ->
            binding.etExpectedReturnDate.text = dateFormat.format(date)
        }
    }

    /**
     * 设置文本变化监听
     */
    private fun setupTextWatchers() {
        binding.etBorrowerName.addTextChangedListener { text ->
            viewModel.setBorrowerName(text.toString())
        }
        
        binding.etBorrowerContact.addTextChangedListener { text ->
            viewModel.setBorrowerContact(text.toString())
        }
        
        binding.etNotes.addTextChangedListener { text ->
            viewModel.setNotes(text.toString())
        }
    }

    /**
     * 设置按钮点击事件
     */
    private fun setupButtons() {
        binding.btnConfirm.setOnClickListener {
            showSaveConfirmDialog()
        }
    }

    /**
     * 观察ViewModel数据变化
     */
    private fun observeViewModel() {
        // 观察可选择的物品列表
        viewModel.availableItems.observe(viewLifecycleOwner) { items ->
            val displayItems = items.map { item ->
                ItemDisplayWrapper(item)
            }
            
            itemAdapter.clear()
            itemAdapter.addAll(displayItems)
            itemAdapter.notifyDataSetChanged()
        }
        
        // 观察选择的物品
        viewModel.selectedItem.observe(viewLifecycleOwner) { item ->
            if (item != null) {
                binding.itemSelectionText.text = item.item.name
            } else {
                binding.itemSelectionText.text = ""
            }
        }
        
        // 观察借用人姓名
        viewModel.borrowerName.observe(viewLifecycleOwner) { name ->
            if (binding.etBorrowerName.text.toString() != name) {
                binding.etBorrowerName.setText(name)
            }
        }
        
        // 观察借用人联系方式
        viewModel.borrowerContact.observe(viewLifecycleOwner) { contact ->
            if (binding.etBorrowerContact.text.toString() != contact) {
                binding.etBorrowerContact.setText(contact)
            }
        }
        
        // 观察备注
        viewModel.notes.observe(viewLifecycleOwner) { notes ->
            if (binding.etNotes.text.toString() != notes) {
                binding.etNotes.setText(notes)
            }
        }
        
        // 观察预计归还日期
        viewModel.expectedReturnDate.observe(viewLifecycleOwner) { date ->
            if (date != null) {
                binding.etExpectedReturnDate.text = dateFormat.format(date)
            }
        }
        
        // 观察表单验证状态
        viewModel.isFormValid.observe(viewLifecycleOwner) { isValid ->
            binding.btnConfirm.isEnabled = isValid
        }
        
        // 观察保存结果
        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                true -> {
                    Toast.makeText(requireContext(), "借出记录创建成功", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                false -> {
                    // 错误消息会通过errorMessage显示
                }
                null -> {
                    // 初始状态，不做处理
                }
            }
        }
        
        // 观察错误消息
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }
        
        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnConfirm.isEnabled = !isLoading && (viewModel.isFormValid.value == true)
        }
    }

    /**
     * 显示日期选择对话框
     */
    /**
     * 显示Material 3日期选择器
     */
    private fun showDatePicker() {
        val currentDate = viewModel.expectedReturnDate.value
        
        showFutureDatePicker(
            title = "选择预计归还日期",
            selectedDate = currentDate
        ) { selectedDate ->
            viewModel.setExpectedReturnDate(selectedDate)
        }
    }

    /**
     * 显示保存确认对话框
     */
    private fun showSaveConfirmDialog() {
        val selectedItem = viewModel.selectedItem.value
        val borrowerName = viewModel.borrowerName.value
        val expectedDate = viewModel.expectedReturnDate.value
        
        if (selectedItem == null || borrowerName.isNullOrBlank() || expectedDate == null) {
            Toast.makeText(requireContext(), "请填写完整信息", Toast.LENGTH_SHORT).show()
            return
        }
        
        val message = """
            确认创建借出记录？
            
            物品：${selectedItem.item.name}
            借用人：$borrowerName
            预计归还：${dateFormat.format(expectedDate)}
        """.trimIndent()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("确认借出")
            .setMessage(message)
            .setPositiveButton("确认") { _, _ ->
                viewModel.saveBorrowRecord()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 物品显示包装类，用于在下拉列表中显示物品名称
     */
    private data class ItemDisplayWrapper(val item: ItemWithDetails) {
        override fun toString(): String = item.item.name
    }
}
