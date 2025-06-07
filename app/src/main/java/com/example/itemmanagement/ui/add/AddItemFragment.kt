package com.example.itemmanagement.ui.add

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentAddItemBinding
import com.example.itemmanagement.data.model.Item
import com.example.itemmanagement.data.model.Location
import com.example.itemmanagement.data.model.OpenStatus
import java.text.SimpleDateFormat
import java.util.*

class AddItemFragment : Fragment() {

    private var _binding: FragmentAddItemBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddItemViewModel by activityViewModels()

    // 工具类实例
    private lateinit var fieldViewFactory: FieldViewFactory
    private lateinit var dialogFactory: DialogFactory
    private lateinit var fieldValueManager: FieldValueManager

    // 字段视图映射
    private val fieldViews = mutableMapOf<String, View>()

    // 预定义的选项数据
    private val units = arrayOf("个", "件", "包", "盒", "瓶", "袋", "箱", "克", "千克", "升", "毫升")
    private val rooms = arrayOf("客厅", "主卧", "次卧", "厨房", "卫生间", "阳台", "储物间")
    private val categories = arrayOf("食品", "药品", "日用品", "电子产品", "衣物", "文具", "其他")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // 初始化工具类
        dialogFactory = DialogFactory(requireContext())
        fieldViewFactory = FieldViewFactory(requireContext(), viewModel, dialogFactory, resources)
        fieldValueManager = FieldValueManager(viewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeDefaultFields()
        setupViews()
        observeSelectedFields()
        setupButtons()
        hideBottomNavigation()
    }

    private fun initializeDefaultFields() {
        // 只在首次创建时初始化默认字段
        if (viewModel.getSelectedFieldsValue().isEmpty()) {
            viewModel.initializeDefaultFieldProperties()
            // 设置默认选中的字段
            listOf(
                "名称",
                "数量",
                "位置",
                "分类",
                "生产日期",
                "保质过期时间"
            ).forEach { fieldName ->
                viewModel.updateFieldSelection(Field("", fieldName, fieldName == "名称"), true)
            }
        }
    }

    private fun setupViews() {
        // 设置添加照片卡片的图标和点击事件
        binding.itemPhotoView.setImageResource(R.drawable.ic_add_photo)
        binding.photoCard.setOnClickListener {
            // TODO: 实现照片选择功能
        }
    }

    private fun observeSelectedFields() {
        viewModel.selectedFields.observe(viewLifecycleOwner) { fields ->
            updateFields(fields)
        }
    }

    private fun updateFields(fields: Set<Field>) {
        // 保存当前字段的值
        if (fieldViews.isNotEmpty()) {
            fieldValueManager.saveFieldValues(fieldViews)
        }

        binding.fieldsContainer.removeAllViews()
        fieldViews.clear()

        // 使用Field类中定义的order属性进行排序
        val sortedFields = fields.sortedBy { it.order }

        sortedFields.forEach { field ->
            val fieldView = fieldViewFactory.createFieldView(field)
            binding.fieldsContainer.addView(fieldView)
            fieldViews[field.name] = fieldView
        }

        // 恢复保存的字段值
        if (fieldViews.isNotEmpty()) {
            fieldValueManager.restoreFieldValues(fieldViews)
        }
    }

    private fun setupButtons() {
        binding.editFieldsButton.setOnClickListener {
            // 在显示编辑字段对话框前，先保存当前字段的值
            if (fieldViews.isNotEmpty()) {
                fieldValueManager.saveFieldValues(fieldViews)
            }
            showEditFieldsDialog()
        }

        binding.saveButton.setOnClickListener {
            saveItem()
        }
    }

    private fun showEditFieldsDialog() {
        EditFieldsFragment.newInstance().show(
            childFragmentManager,
            "EditFieldsFragment"
        )
    }

    private fun hideBottomNavigation() {
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.GONE
    }

    private fun saveItem() {
        // 在保存之前确保所有字段的值都已保存
        if (fieldViews.isNotEmpty()) {
            fieldValueManager.saveFieldValues(fieldViews)
        }

        // 获取所有字段的值
        val values = fieldValueManager.getFieldValues(fieldViews)

        // 验证必填字段
        val nameValue = values["名称"] as? String
        if (nameValue.isNullOrBlank()) {
            Toast.makeText(context, "请输入物品名称", Toast.LENGTH_SHORT).show()
            return
        }

        // 创建物品对象
        val item = Item(
            name = nameValue,
            quantity = (values["数量"] as? String)?.toDoubleOrNull() ?: 0.0,
            unit = values["单位"] as? String ?: "",
            location = Location(
                room = values["位置"] as? String ?: "未指定",
                container = values["容器"] as? String
            ),
            category = values["分类"] as? String ?: "未指定",
            productionDate = parseDate(values["生产日期"] as? String),
            expirationDate = parseDate(values["到期日期"] as? String),
            openStatus = if (values["开封状态"] == "已开封") OpenStatus.OPENED else OpenStatus.UNOPENED
        )

        // 保存物品
        viewModel.saveItem(item)
    }

    private fun parseDate(dateStr: String?): Date? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }

    override fun onPause() {
        super.onPause()
        // 在Fragment暂停时保存所有字段的值
        if (fieldViews.isNotEmpty()) {
            fieldValueManager.saveFieldValues(fieldViews)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.VISIBLE
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_add_item, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                saveItem()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}