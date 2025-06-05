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
        val container = FlowLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(8, 8, 8, 8)
            background = ContextCompat.getDrawable(context, R.drawable.bg_input_borderless)
        }

        val selectedTags = mutableSetOf<String>()

        properties.options?.forEach { option ->
            val chip = Chip(context).apply {
                text = option
                isCheckable = true
                isChecked = false
                textSize = 14f
                chipMinHeight = resources.getDimensionPixelSize(R.dimen.chip_min_height).toFloat()
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedTags.add(option)
                    } else {
                        selectedTags.remove(option)
                    }
                }
            }
            container.addView(chip)
        }

        if (properties.isCustomizable) {
            val addChip = Chip(context).apply {
                text = "添加"
                chipIcon = ContextCompat.getDrawable(context, R.drawable.ic_add)
                textSize = 14f
                chipMinHeight = resources.getDimensionPixelSize(R.dimen.chip_min_height).toFloat()
                setOnClickListener {
                    showAddTagDialog(context, container, selectedTags)
                }
            }
            container.addView(addChip)
        }

        return container
    }

    private fun createRatingBar(context: Context): RatingBar {
        return RatingBar(context, null, android.R.attr.ratingBarStyleSmall).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            numStars = 5
            stepSize = 1f
        }
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

        val numberSpinner = Spinner(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            adapter = ArrayAdapter(
                context,
                R.layout.spinner_item,
                (properties.periodRange ?: 1..36).toList()
            ).apply {
                setDropDownViewResource(R.layout.spinner_dropdown_item)
            }
        }

        val unitSpinner = Spinner(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.margin_small)
            }
            adapter = ArrayAdapter(
                context,
                R.layout.spinner_item,
                properties.periodUnits ?: listOf("年", "月", "日")
            ).apply {
                setDropDownViewResource(R.layout.spinner_dropdown_item)
            }
        }

        container.addView(numberSpinner)
        container.addView(unitSpinner)
        return container
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

        val unitSpinner = Spinner(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.margin_small)
            }

            val items = (properties.unitOptions?.toMutableList() ?: mutableListOf()).apply {
                if (properties.isCustomizable) {
                    add("添加自定义...")
                }
            }

            adapter = ArrayAdapter(
                context,
                R.layout.spinner_item,
                items
            ).apply {
                setDropDownViewResource(R.layout.spinner_dropdown_item)
            }

            if (properties.isCustomizable) {
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position == items.size - 1) {
                            showAddCustomOptionDialog(context, this@apply, items)
                            setSelection(0)
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
        }

        container.addView(input)
        container.addView(unitSpinner)
        return container
    }

    private fun showAddTagDialog(context: Context, container: ViewGroup, selectedTags: MutableSet<String>) {
        val editText = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            hint = "请输入标签名称"
        }

        AlertDialog.Builder(context)
            .setTitle("添加标签")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val tagName = editText.text.toString().trim()
                if (tagName.isNotEmpty()) {
                    val chip = Chip(context).apply {
                        text = tagName
                        isCheckable = true
                        isChecked = true
                        setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                selectedTags.add(tagName)
                            } else {
                                selectedTags.remove(tagName)
                            }
                        }
                    }
                    container.addView(chip, container.childCount - 1)
                    selectedTags.add(tagName)
                }
            }
            .setNegativeButton("取消", null)
            .show()
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

    private fun createSpinner(context: Context, properties: AddItemViewModel.FieldProperties): Spinner {
        return Spinner(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val items = (properties.options?.toMutableList() ?: mutableListOf()).apply {
                if (properties.isCustomizable) {
                    add("添加自定义...")
                }
            }

            adapter = ArrayAdapter(
                context,
                R.layout.spinner_item,
                items
            ).apply {
                setDropDownViewResource(R.layout.spinner_dropdown_item)
            }

            if (properties.isCustomizable) {
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (position == items.size - 1) {
                            showAddCustomOptionDialog(context, this@apply, items)
                            setSelection(0)
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
        }
    }

    private fun showAddCustomOptionDialog(context: Context, spinner: Spinner, items: MutableList<String>) {
        val editText = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            hint = "请输入自定义选项"
        }

        AlertDialog.Builder(context)
            .setTitle("添加自定义选项")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newOption = editText.text.toString().trim()
                if (newOption.isNotEmpty()) {
                    items.add(items.size - 1, newOption)
                    (spinner.adapter as ArrayAdapter<String>).notifyDataSetChanged()
                    spinner.setSelection(items.indexOf(newOption))
                }
            }
            .setNegativeButton("取消", null)
            .show()
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
}