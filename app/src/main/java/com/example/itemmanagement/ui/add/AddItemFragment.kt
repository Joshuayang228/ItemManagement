package com.example.itemmanagement.ui.add

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.drawable.ScaleDrawable
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentAddItemBinding
import com.google.android.material.textfield.TextInputLayout
import com.example.itemmanagement.data.model.Item
import com.example.itemmanagement.data.model.Location
import com.example.itemmanagement.data.model.OpenStatus
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*
import com.example.itemmanagement.ui.add.AddItemViewModel.DisplayStyle

class AddItemFragment : Fragment() {

    private var _binding: FragmentAddItemBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddItemViewModel by activityViewModels()
    private val fieldViews = mutableMapOf<String, View>()

    // 预定义的选项数据
    private val units = arrayOf("个", "件", "包", "盒", "瓶", "袋", "箱", "克", "千克", "升", "毫升")
    private val rooms = arrayOf("客厅", "主卧", "次卧", "厨房", "卫生间", "阳台", "储物间")
    private val categories = arrayOf("食品", "药品", "日用品", "电子产品", "衣物", "文具", "其他")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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
        binding.fieldsContainer.removeAllViews()
        fieldViews.clear()

        // 使用Field类中定义的order属性进行排序
        val sortedFields = fields.sortedBy { it.order }

        sortedFields.forEach { field ->
            val fieldView = createFieldView(field)
            binding.fieldsContainer.addView(fieldView)
            fieldViews[field.name] = fieldView
        }
    }

    private fun createFieldView(field: Field): View {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            gravity = Gravity.CENTER_VERTICAL  // 整个容器垂直居中
        }

        // 添加标签
        val label = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.field_label_width),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = field.name
            textSize = 14f
            gravity = Gravity.START or Gravity.CENTER_VERTICAL  // 左对齐且垂直居中
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
        }
        container.addView(label)

        // 获取字段属性
        val properties = viewModel.getFieldProperties(field.name)

        // 根据字段类型创建不同的输入控件
        val input = when {
            field.name == "开封状态" -> createRadioGroup(context)
            properties.displayStyle == DisplayStyle.TAG -> createTagSelector(context, properties)
            properties.displayStyle == DisplayStyle.RATING_STAR -> createRatingBar(context)
            properties.displayStyle == DisplayStyle.PERIOD_SELECTOR -> createPeriodSelector(context, properties)
            properties.displayStyle == DisplayStyle.LOCATION_SELECTOR -> createLocationSelector(context)
            properties.validationType == AddItemViewModel.ValidationType.DATE -> createDatePicker(context, properties)
            properties.isMultiline -> createMultilineInput(context, properties)
            properties.unitOptions != null -> createNumberWithUnitInput(context, properties)
            properties.options != null -> createSpinner(context, properties)
            properties.validationType == AddItemViewModel.ValidationType.NUMBER -> createNumberInput(context, properties)
            else -> createTextInput(context, properties)
        }

        // 根据输入控件类型决定是否需要包装在容器中
        val inputContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            gravity = Gravity.CENTER_VERTICAL // Default gravity for the container
        }

        when (input) {
            is RatingBar -> {
                input.apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        this.gravity = Gravity.CENTER_HORIZONTAL
                    }
                    setIsIndicator(false)
                    numStars = 5
                    stepSize = 1f
                    scaleX = 0.7f
                    scaleY = 0.7f
                }
                inputContainer.gravity = Gravity.CENTER // Center the WRAP_CONTENT RatingBar in its container
                inputContainer.addView(input)
            }
            is Spinner -> {
                inputContainer.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                inputContainer.addView(input)
            }
            is EditText -> {
                input.apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    background = ContextCompat.getDrawable(context, R.drawable.bg_input_borderless)
                    textSize = 14f
                    gravity = Gravity.END or Gravity.CENTER_VERTICAL
                    setTextColor(ContextCompat.getColor(context, android.R.color.black))
                }
                inputContainer.addView(input) // EditText will fill the inputContainer by default
            }
            is TextView -> { // Handles DatePicker TextView primarily
                // If the TextView (e.g., DatePicker) is WRAP_CONTENT, align it to the end.
                inputContainer.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                inputContainer.addView(input)
            }
            else -> { // Handles other ViewGroups like RadioGroup, FlowLayout, or complex LinearLayouts
                inputContainer.addView(input)
            }
        }
        container.addView(inputContainer)

        return container
    }

    private fun createRadioGroup(context: Context): RadioGroup {
        return RadioGroup(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            orientation = RadioGroup.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL

            addView(RadioButton(context).apply {
                id = View.generateViewId()
                text = "未开封"
                isChecked = true
            })

            addView(Space(context).apply {
                layoutParams = RadioGroup.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.margin_normal),
                    RadioGroup.LayoutParams.WRAP_CONTENT
                )
            })

            addView(RadioButton(context).apply {
                id = View.generateViewId()
                text = "已开封"
            })
        }
    }

    private fun createTagSelector(context: Context, properties: AddItemViewModel.FieldProperties): View {
        // 创建主容器（垂直布局）
        val mainContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // 已选标签容器
        val selectedTagsContainer = FlowLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(8, 8, 8, 8)
            background = ContextCompat.getDrawable(context, R.drawable.bg_input_borderless)
        }

        // 标签选择按钮容器
        val tagSelectorContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.margin_small)
            }
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }

        // 添加已选标签容器和标签选择按钮容器到主容器
        mainContainer.addView(selectedTagsContainer)
        mainContainer.addView(tagSelectorContainer)

        val selectedTags = mutableSetOf<String>()
        val defaultTags = properties.options?.toMutableList() ?: mutableListOf()
        val customTags = mutableListOf<String>()
        val allTags = mutableListOf<String>()

        // 更新所有标签列表
        fun updateAllTags() {
            allTags.clear()
            allTags.addAll(defaultTags)
            allTags.addAll(customTags)
        }

        // 初始化标签列表
        updateAllTags()

        // 添加标签到已选容器
        fun addTagToSelectedContainer(tagName: String) {
            // 如果标签已经被选中，不重复添加
            if (selectedTags.contains(tagName)) {
                return
            }

            val chip = Chip(context).apply {
                text = tagName
                isCheckable = false
                isCloseIconVisible = true
                textSize = 14f
                chipMinHeight = resources.getDimensionPixelSize(R.dimen.chip_min_height).toFloat()

                // 点击关闭图标移除标签
                setOnCloseIconClickListener {
                    selectedTagsContainer.removeView(this)
                    selectedTags.remove(tagName)
                }

                // 长按编辑或删除标签
                setOnLongClickListener {
                    val items = arrayOf("编辑", "删除")
                    MaterialAlertDialogBuilder(context)
                        .setTitle("标签操作")
                        .setItems(items) { dialog, which ->
                            when (which) {
                                0 -> { // 编辑
                                    showEditTagDialog(context, tagName, this, selectedTags, defaultTags, customTags, allTags)
                                }
                                1 -> { // 删除
                                    MaterialAlertDialogBuilder(context)
                                        .setTitle("删除标签")
                                        .setMessage("是否删除标签\"${tagName}\"？")
                                        .setPositiveButton("删除") { _, _ ->
                                            selectedTagsContainer.removeView(this)
                                            selectedTags.remove(tagName)
                                            Toast.makeText(context, "已从选中标签中移除\"${tagName}\"", Toast.LENGTH_SHORT).show()
                                        }
                                        .setNegativeButton("取消", null)
                                        .show()
                                }
                            }
                        }
                        .show()
                    true
                }
            }
            selectedTagsContainer.addView(chip)
            selectedTags.add(tagName)
        }

        // 创建标签选择按钮（只用图标）
        val tagSelectorButton = ImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setImageResource(R.drawable.ic_arrow_drop_down)
            background = null
            setPadding(
                resources.getDimensionPixelSize(R.dimen.padding_normal),
                resources.getDimensionPixelSize(R.dimen.padding_normal),
                resources.getDimensionPixelSize(R.dimen.padding_normal),
                resources.getDimensionPixelSize(R.dimen.padding_normal)
            )

            // 点击显示标签选择对话框
            setOnClickListener {
                showTagSelectionDialog(
                    context,
                    selectedTags,
                    defaultTags,
                    customTags,
                    allTags
                ) { selectedTag ->
                    addTagToSelectedContainer(selectedTag)
                }
            }
        }

        // 添加按钮到按钮容器
        tagSelectorContainer.addView(tagSelectorButton)

        return mainContainer
    }

    // 显示编辑标签对话框
    private fun showEditTagDialog(
        context: Context,
        tagName: String,
        chip: Chip,
        selectedTags: MutableSet<String>,
        defaultTags: MutableList<String>,
        customTags: MutableList<String>,
        allTags: MutableList<String>
    ) {
        val editText = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setText(tagName)
            setSelection(tagName.length)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("编辑标签")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newTagName = editText.text.toString().trim()
                if (newTagName.isNotEmpty() && newTagName != tagName) {
                    // 检查新名称是否已存在
                    if (!defaultTags.contains(newTagName) && !customTags.contains(newTagName) && !selectedTags.contains(newTagName)) {
                        // 更新标签名称
                        if (defaultTags.contains(tagName)) {
                            defaultTags[defaultTags.indexOf(tagName)] = newTagName
                        } else if (customTags.contains(tagName)) {
                            customTags[customTags.indexOf(tagName)] = newTagName
                        }

                        // 更新选中集合
                        selectedTags.remove(tagName)
                        selectedTags.add(newTagName)

                        // 更新标签UI
                        chip.text = newTagName

                        // 更新所有标签列表
                        allTags.clear()
                        allTags.addAll(defaultTags)
                        allTags.addAll(customTags)

                        Toast.makeText(context, "标签已更新", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "该标签名称已存在", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun createNumberWithUnitInput(context: Context, properties: AddItemViewModel.FieldProperties): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }

        val input = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = properties.hint
            textSize = 14f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_input_borderless)
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setHintTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            minWidth = resources.getDimensionPixelSize(R.dimen.input_min_width)
        }

        // 获取默认单位和自定义单位
        val defaultUnits = properties.unitOptions?.toMutableList() ?: mutableListOf()
        val customUnits = mutableListOf<String>()
        val allUnits = mutableListOf<String>()

        // 更新所有单位列表
        fun updateAllUnits() {
            allUnits.clear()
            allUnits.addAll(defaultUnits)
            allUnits.addAll(customUnits)
        }

        // 初始化单位列表
        updateAllUnits()

        // 创建自适应宽度的单位选择器
        val unitTextView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.margin_small)
            }
            textSize = 14f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_input_borderless)
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setPadding(8, 8, 8, 8)

            // 设置默认值
            text = defaultUnits.firstOrNull() ?: "个"

            // 添加下拉箭头图标
            setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down, 0)
            compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.margin_small)

            // 点击事件处理
            setOnClickListener { view ->
                showUnitSelectionDialog(
                    context = context,
                    title = "选择单位",
                    units = allUnits,
                    defaultUnits = defaultUnits,
                    customUnits = customUnits,
                    isCustomizable = properties.isCustomizable,
                    currentTextView = this,
                    onUnitSelected = { selectedUnit ->
                        text = selectedUnit
                    },
                    onEditUnit = { unit, newUnit ->
                        // 编辑单位
                        if (defaultUnits.contains(unit)) {
                            defaultUnits[defaultUnits.indexOf(unit)] = newUnit
                        } else if (customUnits.contains(unit)) {
                            customUnits[customUnits.indexOf(unit)] = newUnit
                        }
                        updateAllUnits()
                        if (text == unit) {
                            text = newUnit
                        }
                    },
                    onDeleteUnit = { unit ->
                        // 删除单位
                        if (defaultUnits.contains(unit)) {
                            defaultUnits.remove(unit)
                        } else if (customUnits.contains(unit)) {
                            customUnits.remove(unit)
                        }
                        updateAllUnits()
                        if (text == unit && allUnits.isNotEmpty()) {
                            text = allUnits[0]
                        }
                    },
                    onAddUnit = { newUnit ->
                        // 添加单位
                        customUnits.add(newUnit)
                        updateAllUnits()
                        text = newUnit
                    }
                )
            }
        }

        container.addView(input)
        container.addView(unitTextView)
        return container
    }

    // 显示单位选择对话框
    private fun showUnitSelectionDialog(
        context: Context,
        title: String,
        units: List<String>,
        defaultUnits: MutableList<String>,
        customUnits: MutableList<String>,
        isCustomizable: Boolean,
        currentTextView: TextView,
        onUnitSelected: (String) -> Unit,
        onEditUnit: (String, String) -> Unit,
        onDeleteUnit: (String) -> Unit,
        onAddUnit: (String) -> Unit
    ) {
        val items = units.toTypedArray()

        // 使用AlertDialog代替PopupMenu，这样可以添加长按监听
        val builder = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setItems(items) { dialog, which ->
                onUnitSelected(items[which])
                dialog.dismiss()
            }

        val dialog = builder.create()

        // 设置列表项长按监听
        dialog.listView?.setOnItemLongClickListener { parent, itemView, position, id ->
            val selectedItem = items[position]
            dialog.dismiss()

            // 显示操作选项（编辑/删除）
            val options = arrayOf("编辑", "删除")
            MaterialAlertDialogBuilder(context)
                .setTitle("单位操作")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> { // 编辑
                            showEditUnitDialogWithCallback(
                                context = context,
                                unit = selectedItem,
                                defaultUnits = defaultUnits,
                                customUnits = customUnits,
                                onUnitEdited = { oldUnit, newUnit ->
                                    onEditUnit(oldUnit, newUnit)
                                }
                            )
                        }
                        1 -> { // 删除
                            showDeleteUnitDialogWithCallback(
                                context = context,
                                unit = selectedItem,
                                onUnitDeleted = {
                                    onDeleteUnit(selectedItem)
                                }
                            )
                        }
                    }
                }
                .show()

            true
        }

        // 如果支持自定义，添加"添加"按钮到对话框右上角
        if (isCustomizable) {
            dialog.setOnShowListener {
                // 添加菜单按钮到对话框标题栏
                val decorView = dialog.window?.decorView as? ViewGroup
                val titleBar = decorView?.findViewById<ViewGroup>(androidx.appcompat.R.id.title_template)

                titleBar?.let { titleBarView ->
                    // 创建添加按钮
                    val addButton = ImageButton(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        setImageResource(R.drawable.ic_add)
                        background = null
                        setPadding(
                            resources.getDimensionPixelSize(R.dimen.padding_normal),
                            resources.getDimensionPixelSize(R.dimen.padding_normal),
                            resources.getDimensionPixelSize(R.dimen.padding_normal),
                            resources.getDimensionPixelSize(R.dimen.padding_normal)
                        )

                        // 点击添加自定义单位
                        setOnClickListener {
                            showAddCustomUnitDialogWithCallback(
                                context = context,
                                defaultUnits = defaultUnits,
                                customUnits = customUnits,
                                onUnitAdded = { newUnit ->
                                    onAddUnit(newUnit)
                                    dialog.dismiss()
                                }
                            )
                        }
                    }

                    // 将按钮添加到标题栏
                    titleBarView.addView(addButton)
                }
            }
        }

        dialog.show()
    }

    // 显示添加自定义单位对话框（带回调）
    private fun showAddCustomUnitDialogWithCallback(
        context: Context,
        defaultUnits: List<String>,
        customUnits: List<String>,
        onUnitAdded: (String) -> Unit
    ) {
        val editText = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            hint = "请输入自定义单位"
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("添加自定义单位")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newUnit = editText.text.toString().trim()
                if (newUnit.isNotEmpty()) {
                    // 检查是否已存在
                    if (!defaultUnits.contains(newUnit) && !customUnits.contains(newUnit)) {
                        onUnitAdded(newUnit)
                    } else {
                        Toast.makeText(context, "该单位已存在", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 显示编辑单位对话框（带回调）
    private fun showEditUnitDialogWithCallback(
        context: Context,
        unit: String,
        defaultUnits: List<String>,
        customUnits: List<String>,
        onUnitEdited: (String, String) -> Unit
    ) {
        val editText = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setText(unit)
            setSelection(unit.length)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("编辑单位")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newUnit = editText.text.toString().trim()
                if (newUnit.isNotEmpty() && newUnit != unit) {
                    // 检查是否已存在
                    if (!defaultUnits.contains(newUnit) && !customUnits.contains(newUnit)) {
                        onUnitEdited(unit, newUnit)
                        Toast.makeText(context, "单位已更新", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "该单位已存在", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 显示删除单位确认对话框（带回调）
    private fun showDeleteUnitDialogWithCallback(
        context: Context,
        unit: String,
        onUnitDeleted: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle("删除单位")
            .setMessage("是否删除单位\"${unit}\"？")
            .setPositiveButton("删除") { _, _ ->
                onUnitDeleted()
                Toast.makeText(context, "已删除单位\"${unit}\"", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun createPeriodSelector(context: Context, properties: AddItemViewModel.FieldProperties): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }

        // 数字选择器
        val numberSelector = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 14f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_input_borderless)
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setPadding(8, 8, 8, 8)

            // 设置默认值
            text = (properties.periodRange?.first ?: 1).toString()

            // 添加下拉箭头图标
            setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down, 0)
            compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.margin_small)

            // 点击事件处理
            setOnClickListener { view ->
                val numbers = (properties.periodRange ?: 1..36).toList().map { it.toString() }.toTypedArray()

                // 使用AlertDialog代替PopupMenu
                MaterialAlertDialogBuilder(context)
                    .setTitle("选择数字")
                    .setItems(numbers) { dialog, which ->
                        text = numbers[which]
                        dialog.dismiss()
                    }
                    .show()
            }
        }

        // 获取默认单位和自定义单位
        val defaultUnits = properties.periodUnits?.toMutableList() ?: mutableListOf("年", "月", "日")
        val customUnits = mutableListOf<String>()
        val allUnits = mutableListOf<String>()

        // 更新所有单位列表
        fun updateAllUnits() {
            allUnits.clear()
            allUnits.addAll(defaultUnits)
            allUnits.addAll(customUnits)
        }

        // 初始化单位列表
        updateAllUnits()

        // 单位选择器
        val periodUnitTextView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.margin_small)
            }
            textSize = 14f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_input_borderless)
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setPadding(8, 8, 8, 8)

            // 设置默认值
            text = defaultUnits.firstOrNull() ?: "月"

            // 添加下拉箭头图标
            setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down, 0)
            compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.margin_small)

            // 点击事件处理
            setOnClickListener { view ->
                showUnitSelectionDialog(
                    context = context,
                    title = "选择单位",
                    units = allUnits,
                    defaultUnits = defaultUnits,
                    customUnits = customUnits,
                    isCustomizable = true,
                    currentTextView = this,
                    onUnitSelected = { selectedUnit ->
                        text = selectedUnit
                    },
                    onEditUnit = { unit, newUnit ->
                        // 编辑单位
                        if (defaultUnits.contains(unit)) {
                            defaultUnits[defaultUnits.indexOf(unit)] = newUnit
                        } else if (customUnits.contains(unit)) {
                            customUnits[customUnits.indexOf(unit)] = newUnit
                        }
                        updateAllUnits()
                        if (text == unit) {
                            text = newUnit
                        }
                    },
                    onDeleteUnit = { unit ->
                        // 删除单位
                        if (defaultUnits.contains(unit)) {
                            defaultUnits.remove(unit)
                        } else if (customUnits.contains(unit)) {
                            customUnits.remove(unit)
                        }
                        updateAllUnits()
                        if (text == unit && allUnits.isNotEmpty()) {
                            text = allUnits[0]
                        }
                    },
                    onAddUnit = { newUnit ->
                        // 添加单位
                        customUnits.add(newUnit)
                        updateAllUnits()
                        text = newUnit
                    }
                )
            }
        }

        container.addView(numberSelector)
        container.addView(periodUnitTextView)
        return container
    }

    private fun createSpinner(context: Context, properties: AddItemViewModel.FieldProperties): View {
        return TextView(context).apply {
            val spinnerTextView = this // Capture the TextView instance
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 14f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_input_borderless)
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setPadding(8, 8, 8, 8)

            // 设置默认值
            spinnerTextView.text = properties.options?.firstOrNull() ?: ""

            // 添加下拉箭头图标
            setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down, 0)
            compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.margin_small)

            // 获取默认选项和自定义选项
            val defaultOptions = properties.options?.toMutableList() ?: mutableListOf()
            val customOptions = mutableListOf<String>()
            val allOptions = mutableListOf<String>()

            // 更新所有选项列表
            fun updateAllOptions() {
                allOptions.clear()
                allOptions.addAll(defaultOptions)
                allOptions.addAll(customOptions)
            }

            // 初始化选项列表
            updateAllOptions()

            // 点击事件处理
            setOnClickListener { view ->
                val items = allOptions.toTypedArray()

                // 使用AlertDialog代替PopupMenu，这样可以添加长按监听
                val builder = MaterialAlertDialogBuilder(context)
                    .setTitle("选择选项")
                    .setItems(items) { dialog, which ->
                        text = items[which]
                        dialog.dismiss()
                    }

                val dialog = builder.create()

                // 设置列表项长按监听
                dialog.listView?.setOnItemLongClickListener { parent, itemView, position, id ->
                    val selectedItem = items[position]
                    dialog.dismiss()

                    // 显示操作选项（编辑/删除）
                    val options = arrayOf("编辑", "删除")
                    MaterialAlertDialogBuilder(context)
                        .setTitle("选项操作")
                        .setItems(options) { _, which ->
                            when (which) {
                                0 -> { // 编辑
                                    showEditOptionDialog(context, selectedItem, this, defaultOptions, customOptions, allOptions)
                                }
                                1 -> { // 删除
                                    showDeleteOptionDialog(context, selectedItem, this, defaultOptions, customOptions, allOptions)
                                }
                            }
                        }
                        .show()

                    true
                }

                // 如果支持自定义，添加"添加"按钮到对话框右上角
                if (properties.isCustomizable) {
                    dialog.setOnShowListener {
                        // 添加菜单按钮到对话框标题栏
                        val decorView = dialog.window?.decorView as? ViewGroup
                        val titleBar = decorView?.findViewById<ViewGroup>(androidx.appcompat.R.id.title_template)

                        titleBar?.let {
                            // 创建添加按钮
                            val addButton = ImageButton(context).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                setImageResource(R.drawable.ic_add)
                                background = null
                                setPadding(
                                    resources.getDimensionPixelSize(R.dimen.padding_normal),
                                    resources.getDimensionPixelSize(R.dimen.padding_normal),
                                    resources.getDimensionPixelSize(R.dimen.padding_normal),
                                    resources.getDimensionPixelSize(R.dimen.padding_normal)
                                )

                                // 点击添加自定义选项
                                setOnClickListener {
                                    val editText = EditText(context).apply {
                                        layoutParams = LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                        )
                                        hint = "请输入自定义选项"
                                    }

                                    val targetTextView = spinnerTextView  // Capture the reference

                                    MaterialAlertDialogBuilder(context)
                                        .setTitle("添加自定义选项")
                                        .setView(editText)
                                        .setPositiveButton("确定") { _, _ ->
                                            val newOption = editText.text.toString().trim()
                                            if (newOption.isNotEmpty()) {
                                                // 检查是否已存在
                                                if (!defaultOptions.contains(newOption) && !customOptions.contains(newOption)) {
                                                    customOptions.add(newOption)
                                                    // 更新所有选项列表
                                                    allOptions.clear()
                                                    allOptions.addAll(defaultOptions)
                                                    allOptions.addAll(customOptions)

                                                    // 更新文本
                                                    targetTextView.text = newOption

                                                    // 关闭原对话框
                                                    dialog.dismiss()
                                                } else {
                                                    Toast.makeText(context, "该选项已存在", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                        .setNegativeButton("取消", null)
                                        .show()
                                }
                            }

                            // 将按钮添加到标题栏
                            it.addView(addButton)
                        }
                    }
                }

                dialog.show()
            }
        }
    }

    private fun createTextInput(context: Context, properties: AddItemViewModel.FieldProperties): EditText {
        return EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            hint = properties.hint
            textSize = 14f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_input_borderless)
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setHintTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        }
    }

    private fun createNumberInput(context: Context, properties: AddItemViewModel.FieldProperties): EditText {
        return createTextInput(context, properties).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            properties.defaultValue?.let { setText(it) }
        }
    }

    private fun createMultilineInput(context: Context, properties: AddItemViewModel.FieldProperties): EditText {
        return createTextInput(context, properties).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 1
            maxLines = properties.maxLines ?: 5
            gravity = Gravity.TOP
        }
    }

    private fun createDatePicker(context: Context, properties: AddItemViewModel.FieldProperties): TextView {
        return TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            background = ContextCompat.getDrawable(context, R.drawable.bg_input_borderless)
            setPadding(8, 8, 8, 8)
            textSize = 14f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            hint = "点击选择日期"
            setTextColor(ContextCompat.getColor(context, android.R.color.black))

            if (properties.defaultDate) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                text = dateFormat.format(Date())
            }

            setOnClickListener {
                showDatePicker(this)
            }
        }
    }

    private fun createLocationSelector(context: Context): View {
        // TODO: 实现三级位置选择器
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
    }

    private fun createRatingBar(context: Context): RatingBar {
        return RatingBar(context, null, android.R.attr.ratingBarStyle).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            numStars = 5
            stepSize = 1f
            scaleX = 0.7f
            scaleY = 0.7f
        }
    }

    private fun showDatePicker(textView: TextView) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                textView.text = dateFormat.format(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setupButtons() {
        binding.editFieldsButton.setOnClickListener {
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
        // 获取所有字段的值
        val values = mutableMapOf<String, Any?>()

        fieldViews.forEach { (fieldName, view) ->
            val value = when (val input = if (view is LinearLayout) view.getChildAt(1) else view) {
                is EditText -> input.text.toString()
                is Spinner -> input.selectedItem?.toString()
                is TextView -> input.text.toString()
                is RadioGroup -> if (input.checkedRadioButtonId == input.getChildAt(0).id) "未开封" else "已开封"
                else -> null
            }
            values[fieldName] = value
        }

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

    // 显示标签选择对话框
    private fun showTagSelectionDialog(
        context: Context,
        selectedTags: MutableSet<String>,
        defaultTags: MutableList<String>,
        customTags: MutableList<String>,
        allTags: MutableList<String>,
        onTagSelected: (String) -> Unit
    ) {
        // 更新所有标签列表
        allTags.clear()
        allTags.addAll(defaultTags)
        allTags.addAll(customTags)

        // 创建多选对话框
        val items = allTags.toTypedArray()
        val checkedItems = BooleanArray(items.size) { i -> selectedTags.contains(items[i]) }

        val builder = MaterialAlertDialogBuilder(context)
            .setTitle("选择标签")
            .setMultiChoiceItems(items, checkedItems) { dialog, which, isChecked ->
                val tagName = items[which]
                if (isChecked) {
                    onTagSelected(tagName)
                } else {
                    // 如果取消选中，从已选标签中移除
                    selectedTags.remove(tagName)
                    // 这里需要刷新已选标签的UI，但由于我们无法直接访问selectedTagsContainer，
                    // 所以这个功能需要在对话框关闭后手动处理
                }
            }
            .setPositiveButton("确定", null)
            .setNeutralButton("管理标签") { _, _ ->
                showManageTagsDialog(context, defaultTags, customTags, allTags, selectedTags)
            }

        val dialog = builder.create()

        // 添加"添加标签"按钮到对话框右上角
        dialog.setOnShowListener {
            // 添加菜单按钮到对话框标题栏
            val decorView = dialog.window?.decorView as? ViewGroup
            val titleBar = decorView?.findViewById<ViewGroup>(androidx.appcompat.R.id.title_template)

            titleBar?.let {
                // 创建添加按钮
                val addButton = ImageButton(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setImageResource(R.drawable.ic_add)
                    background = null
                    setPadding(
                        resources.getDimensionPixelSize(R.dimen.padding_normal),
                        resources.getDimensionPixelSize(R.dimen.padding_normal),
                        resources.getDimensionPixelSize(R.dimen.padding_normal),
                        resources.getDimensionPixelSize(R.dimen.padding_normal)
                    )

                    // 点击添加新标签
                    setOnClickListener {
                        showAddNewTagDialog(context, selectedTags, defaultTags, customTags, allTags) { newTag ->
                            onTagSelected(newTag)
                            // 刷新对话框列表
                            dialog.dismiss()
                            showTagSelectionDialog(context, selectedTags, defaultTags, customTags, allTags, onTagSelected)
                        }
                    }
                }

                // 将按钮添加到标题栏
                it.addView(addButton)
            }
        }

        dialog.show()
    }

    // 显示管理标签对话框
    private fun showManageTagsDialog(
        context: Context,
        defaultTags: MutableList<String>,
        customTags: MutableList<String>,
        allTags: MutableList<String>,
        selectedTags: MutableSet<String>
    ) {
        // 更新所有标签列表
        allTags.clear()
        allTags.addAll(defaultTags)
        allTags.addAll(customTags)

        // 创建用于批量操作的列表
        val items = allTags.toTypedArray()
        val checkedItems = BooleanArray(items.size) { false }
        val selectedForOperation = mutableSetOf<String>()

        val builder = MaterialAlertDialogBuilder(context)
            .setTitle("管理标签")
            .setMultiChoiceItems(items, checkedItems) { _, which, isChecked ->
                val tagName = items[which]
                if (isChecked) {
                    selectedForOperation.add(tagName)
                } else {
                    selectedForOperation.remove(tagName)
                }
            }
            .setPositiveButton("删除选中项") { _, _ ->
                if (selectedForOperation.isNotEmpty()) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle("批量删除")
                        .setMessage("确定要删除选中的 ${selectedForOperation.size} 个标签吗？")
                        .setPositiveButton("确定") { _, _ ->
                            // 从所有列表中删除选中的标签
                            defaultTags.removeAll(selectedForOperation)
                            customTags.removeAll(selectedForOperation)
                            selectedTags.removeAll(selectedForOperation)

                            // 更新所有标签列表
                            allTags.clear()
                            allTags.addAll(defaultTags)
                            allTags.addAll(customTags)

                            Toast.makeText(context, "已删除 ${selectedForOperation.size} 个标签", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("取消", null)
                        .show()
                } else {
                    Toast.makeText(context, "请先选择要删除的标签", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .setNeutralButton("添加新标签") { _, _ ->
                showAddNewTagDialog(context, selectedTags, defaultTags, customTags, allTags) { newTag ->
                    // 不自动选中新添加的标签
                }
            }

        val dialog = builder.create()
        dialog.show()
    }

    // 显示添加新标签对话框
    private fun showAddNewTagDialog(
        context: Context,
        selectedTags: MutableSet<String>,
        defaultTags: MutableList<String>,
        customTags: MutableList<String>,
        allTags: MutableList<String>,
        onTagAdded: (String) -> Unit
    ) {
        val editText = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            hint = "请输入标签名称"
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("添加新标签")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val tagName = editText.text.toString().trim()
                if (tagName.isNotEmpty()) {
                    // 检查标签是否已存在
                    if (!defaultTags.contains(tagName) && !customTags.contains(tagName)) {
                        customTags.add(tagName)
                        allTags.add(tagName)
                        onTagAdded(tagName)
                    } else {
                        // 如果标签已存在但未选中，则选中它
                        if (!selectedTags.contains(tagName)) {
                            onTagAdded(tagName)
                        } else {
                            Toast.makeText(context, "该标签已存在且已选中", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 显示编辑选项对话框
    private fun showEditOptionDialog(
        context: Context,
        currentOption: String,
        textView: TextView,
        defaultOptions: MutableList<String>,
        customOptions: MutableList<String>,
        allOptions: MutableList<String>
    ) {
        val editText = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setText(currentOption)
            setSelection(currentOption.length)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("编辑选项")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newOption = editText.text.toString().trim()
                if (newOption.isNotEmpty() && newOption != currentOption) {
                    // 检查是否已存在
                    if (!defaultOptions.contains(newOption) && !customOptions.contains(newOption)) {
                        // 从相应列表中更新
                        if (defaultOptions.contains(currentOption)) {
                            val index = defaultOptions.indexOf(currentOption)
                            defaultOptions[index] = newOption
                        } else if (customOptions.contains(currentOption)) {
                            val index = customOptions.indexOf(currentOption)
                            customOptions[index] = newOption
                        }

                        // 如果当前显示的是被编辑的选项，更新显示
                        if (textView.text == currentOption) {
                            textView.text = newOption
                        }

                        // 更新所有选项列表
                        allOptions.clear()
                        allOptions.addAll(defaultOptions)
                        allOptions.addAll(customOptions)

                        Toast.makeText(context, "选项已更新", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "该选项已存在", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 显示删除选项确认对话框
    private fun showDeleteOptionDialog(
        context: Context,
        option: String,
        textView: TextView,
        defaultOptions: MutableList<String>,
        customOptions: MutableList<String>,
        allOptions: MutableList<String>
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle("删除选项")
            .setMessage("是否删除选项\"${option}\"？")
            .setPositiveButton("删除") { _, _ ->
                // 从相应列表中删除
                if (defaultOptions.contains(option)) {
                    defaultOptions.remove(option)
                } else if (customOptions.contains(option)) {
                    customOptions.remove(option)
                }

                // 如果当前显示的是被删除的选项，则重置为第一个选项
                if (textView.text == option) {
                    // 更新所有选项列表
                    allOptions.clear()
                    allOptions.addAll(defaultOptions)
                    allOptions.addAll(customOptions)

                    textView.text = if (allOptions.isNotEmpty()) {
                        allOptions[0]
                    } else {
                        ""
                    }
                } else {
                    // 更新所有选项列表
                    allOptions.clear()
                    allOptions.addAll(defaultOptions)
                    allOptions.addAll(customOptions)
                }

                Toast.makeText(context, "已删除选项\"${option}\"", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}