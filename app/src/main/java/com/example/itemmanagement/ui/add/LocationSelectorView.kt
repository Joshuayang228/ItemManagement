package com.example.itemmanagement.ui.add

import android.app.AlertDialog
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.*
import androidx.core.content.ContextCompat
import com.example.itemmanagement.R

/**
 * 自定义位置选择器视图
 * 实现三级级联位置选择（区域 > 容器 > 子位置）
 */
class LocationSelectorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    // 主要显示区域
    private val areaTextView: TextView
    private val containerTextView: TextView
    private val sublocationTextView: TextView

    // 位置管理器
    private lateinit var locationManager: LocationManager

    // ViewModel引用
    private lateinit var viewModel: AddItemViewModel

    // 当前选择的值
    private var selectedArea: String? = null
    private var selectedContainer: String? = null
    private var selectedSublocation: String? = null

    // 值变化监听器
    private var onLocationSelectedListener: ((String?, String?, String?) -> Unit)? = null

    // 对话框是否已经显示过的标记
    private var hasShownAreaDialog = false
    private var hasShownContainerDialog = false
    private var hasShownSublocationDialog = false

    init {
        // 加载布局
        LayoutInflater.from(context).inflate(R.layout.view_location_selector, this, true)

        // 初始化视图
        areaTextView = findViewById(R.id.area_text_view)
        containerTextView = findViewById(R.id.container_text_view)
        sublocationTextView = findViewById(R.id.sublocation_text_view)

        // 确保视图可长按
        areaTextView.isLongClickable = true
        containerTextView.isLongClickable = true
        sublocationTextView.isLongClickable = true

        // 设置点击事件
        areaTextView.setOnClickListener { showAreaSelectionDialog() }
        containerTextView.setOnClickListener { showContainerSelectionDialog() }
        sublocationTextView.setOnClickListener { showSublocationSelectionDialog() }

        // 初始状态
        updateViewState()
    }

    /**
     * 初始化位置管理器
     */
    fun initialize(locationManager: LocationManager, viewModel: AddItemViewModel) {
        this.locationManager = locationManager
        this.viewModel = viewModel

        // 检查是否有已保存的位置值
        val savedArea = viewModel.getFieldValue("位置_area") as? String
        val savedContainer = viewModel.getFieldValue("位置_container") as? String
        val savedSublocation = viewModel.getFieldValue("位置_sublocation") as? String

        if (!savedArea.isNullOrEmpty()) {
            setSelectedLocation(savedArea, savedContainer, savedSublocation)
        }
    }

    /**
     * 设置位置选择监听器
     */
    fun setOnLocationSelectedListener(listener: (String?, String?, String?) -> Unit) {
        this.onLocationSelectedListener = listener
    }

    /**
     * 设置已选择的位置
     */
    fun setSelectedLocation(area: String?, container: String?, sublocation: String?) {
        this.selectedArea = area
        this.selectedContainer = container
        this.selectedSublocation = sublocation

        // 确保层级关系正确
        if (area.isNullOrEmpty()) {
            this.selectedContainer = null
            this.selectedSublocation = null
        } else if (container.isNullOrEmpty()) {
            this.selectedSublocation = null
        }

        // 更新UI
        updateViewState()
    }

    /**
     * 获取已选择的区域
     */
    fun getSelectedArea(): String? = selectedArea

    /**
     * 获取已选择的容器
     */
    fun getSelectedContainer(): String? = selectedContainer

    /**
     * 获取已选择的子位置
     */
    fun getSelectedSublocation(): String? = selectedSublocation

    /**
     * 获取完整位置字符串
     */
    fun getFullLocationString(): String {
        val parts = mutableListOf<String>()
        selectedArea?.let { parts.add(it) }
        selectedContainer?.let { parts.add(it) }
        selectedSublocation?.let { parts.add(it) }
        return if (parts.isEmpty()) "" else parts.joinToString(" > ")
    }

    /**
     * 清除所有选择
     */
    fun clearSelection() {
        selectedArea = null
        selectedContainer = null
        selectedSublocation = null
        updateViewState()
    }

    /**
     * 显示区域选择对话框
     */
    private fun showAreaSelectionDialog() {
        if (!::locationManager.isInitialized) {
            return
        }

        // 获取最新的区域列表
        val areas = locationManager.getAllAreas()

        // 创建新的适配器
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, areas)

        // 创建对话框
        val builder = AlertDialog.Builder(context)
            .setTitle("选择区域")
            .setAdapter(adapter) { _, which ->
                val selectedArea = areas[which]
                setSelectedLocation(selectedArea, null, null)
                notifyLocationChanged()
            }
            .setNeutralButton("添加自定义") { _, _ ->
                showAddCustomAreaDialog()
            }
            .setNegativeButton("取消", null)

        // 如果已有选择，提供清除选项
        if (!selectedArea.isNullOrEmpty()) {
            builder.setPositiveButton("清除") { _, _ ->
                clearSelection()
                notifyLocationChanged()
            }
        }

        // 创建ListView并设置长按监听器
        val dialog = builder.create()
        dialog.setOnShowListener {
            // 如果是第一次显示对话框，显示提示
            if (!hasShownAreaDialog) {
                Toast.makeText(context, "长按选项可编辑或删除", Toast.LENGTH_SHORT).show()
                hasShownAreaDialog = true
            }

            val listView = dialog.listView
            listView?.setOnItemLongClickListener { _, _, position, _ ->
                val areaName = areas[position]

                // 显示编辑/删除选项
                val options = arrayOf("编辑", "删除")
                AlertDialog.Builder(context)
                    .setTitle("区域操作")
                    .setItems(options) { _, which ->
                        when (which) {
                            0 -> showEditAreaDialogFromList(areaName)
                            1 -> showDeleteAreaConfirmDialogFromList(areaName)
                        }
                        dialog.dismiss() // 关闭选择对话框
                    }
                    .show()

                true
            }
        }

        dialog.show()
    }

    /**
     * 显示容器选择对话框
     */
    private fun showContainerSelectionDialog() {
        if (selectedArea.isNullOrEmpty()) {
            Toast.makeText(context, "请先选择区域", Toast.LENGTH_SHORT).show()
            showAreaSelectionDialog()
            return
        }

        if (!::locationManager.isInitialized) {
            return
        }

        val containers = locationManager.getContainersByArea(selectedArea!!)
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, containers)

        // 创建对话框
        val builder = AlertDialog.Builder(context)
            .setTitle("选择容器")
            .setAdapter(adapter) { _, which ->
                val selectedContainer = containers[which]
                setSelectedLocation(selectedArea, selectedContainer, null)
                notifyLocationChanged()
            }
            .setNeutralButton("添加自定义") { _, _ ->
                showAddCustomContainerDialog()
            }
            .setNegativeButton("取消", null)

        // 如果已有选择，提供清除选项
        if (!selectedContainer.isNullOrEmpty()) {
            builder.setPositiveButton("清除") { _, _ ->
                setSelectedLocation(selectedArea, null, null)
                notifyLocationChanged()
            }
        }

        // 创建ListView并设置长按监听器
        val dialog = builder.create()
        dialog.setOnShowListener {
            // 如果是第一次显示对话框，显示提示
            if (!hasShownContainerDialog) {
                Toast.makeText(context, "长按选项可编辑或删除", Toast.LENGTH_SHORT).show()
                hasShownContainerDialog = true
            }

            val listView = dialog.listView
            listView?.setOnItemLongClickListener { _, _, position, _ ->
                val containerName = containers[position]

                // 显示编辑/删除选项
                val options = arrayOf("编辑", "删除")
                AlertDialog.Builder(context)
                    .setTitle("容器操作")
                    .setItems(options) { _, which ->
                        when (which) {
                            0 -> showEditContainerDialogFromList(containerName)
                            1 -> showDeleteContainerConfirmDialogFromList(containerName)
                        }
                        dialog.dismiss() // 关闭选择对话框
                    }
                    .show()

                true
            }
        }

        dialog.show()
    }

    /**
     * 显示子位置选择对话框
     */
    private fun showSublocationSelectionDialog() {
        if (selectedContainer.isNullOrEmpty()) {
            Toast.makeText(context, "请先选择容器", Toast.LENGTH_SHORT).show()
            showContainerSelectionDialog()
            return
        }

        if (!::locationManager.isInitialized) {
            return
        }

        val sublocations = locationManager.getSublocationsByContainer(selectedContainer!!)
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, sublocations)

        // 创建对话框
        val builder = AlertDialog.Builder(context)
            .setTitle("选择子位置")
            .setAdapter(adapter) { _, which ->
                val selectedSublocation = sublocations[which]
                setSelectedLocation(selectedArea, selectedContainer, selectedSublocation)
                notifyLocationChanged()
            }
            .setNeutralButton("添加自定义") { _, _ ->
                showAddCustomSublocationDialog()
            }
            .setNegativeButton("取消", null)

        // 如果已有选择，提供清除选项
        if (!selectedSublocation.isNullOrEmpty()) {
            builder.setPositiveButton("清除") { _, _ ->
                setSelectedLocation(selectedArea, selectedContainer, null)
                notifyLocationChanged()
            }
        }

        // 创建ListView并设置长按监听器
        val dialog = builder.create()
        dialog.setOnShowListener {
            // 如果是第一次显示对话框，显示提示
            if (!hasShownSublocationDialog) {
                Toast.makeText(context, "长按选项可编辑或删除", Toast.LENGTH_SHORT).show()
                hasShownSublocationDialog = true
            }

            val listView = dialog.listView
            listView?.setOnItemLongClickListener { _, _, position, _ ->
                val sublocationName = sublocations[position]

                // 显示编辑/删除选项
                val options = arrayOf("编辑", "删除")
                AlertDialog.Builder(context)
                    .setTitle("子位置操作")
                    .setItems(options) { _, which ->
                        when (which) {
                            0 -> showEditSublocationDialogFromList(sublocationName)
                            1 -> showDeleteSublocationConfirmDialogFromList(sublocationName)
                        }
                        dialog.dismiss() // 关闭选择对话框
                    }
                    .show()

                true
            }
        }

        dialog.show()
    }

    /**
     * 显示添加自定义区域对话框
     */
    private fun showAddCustomAreaDialog() {
        val editText = EditText(context).apply {
            hint = "请输入自定义区域名称"
        }

        AlertDialog.Builder(context)
            .setTitle("添加自定义区域")
            .setView(editText)
            .setPositiveButton("添加") { _, _ ->
                val newArea = editText.text.toString().trim()
                if (newArea.isNotEmpty()) {
                    locationManager.addCustomArea(newArea)
                    setSelectedLocation(newArea, null, null)
                    notifyLocationChanged()
                    updateViewState()
                } else {
                    Toast.makeText(context, "区域名称不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示添加自定义容器对话框
     */
    private fun showAddCustomContainerDialog() {
        if (selectedArea.isNullOrEmpty()) {
            Toast.makeText(context, "请先选择区域", Toast.LENGTH_SHORT).show()
            return
        }

        val editText = EditText(context).apply {
            hint = "请输入自定义容器名称"
        }

        AlertDialog.Builder(context)
            .setTitle("添加自定义容器")
            .setView(editText)
            .setPositiveButton("添加") { _, _ ->
                val newContainer = editText.text.toString().trim()
                if (newContainer.isNotEmpty()) {
                    locationManager.addCustomContainerToArea(selectedArea!!, newContainer)
                    setSelectedLocation(selectedArea, newContainer, null)
                    notifyLocationChanged()
                    updateViewState()
                } else {
                    Toast.makeText(context, "容器名称不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示添加自定义子位置对话框
     */
    private fun showAddCustomSublocationDialog() {
        if (selectedContainer.isNullOrEmpty()) {
            Toast.makeText(context, "请先选择容器", Toast.LENGTH_SHORT).show()
            return
        }

        val editText = EditText(context).apply {
            hint = "请输入自定义子位置名称"
        }

        AlertDialog.Builder(context)
            .setTitle("添加自定义子位置")
            .setView(editText)
            .setPositiveButton("添加") { _, _ ->
                val newSublocation = editText.text.toString().trim()
                if (newSublocation.isNotEmpty()) {
                    locationManager.addCustomSublocationToContainer(selectedContainer!!, newSublocation)
                    setSelectedLocation(selectedArea, selectedContainer, newSublocation)
                    notifyLocationChanged()
                    updateViewState()
                } else {
                    Toast.makeText(context, "子位置名称不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 更新视图状态
     */
    private fun updateViewState() {
        // 更新区域显示
        if (selectedArea.isNullOrEmpty()) {
            areaTextView.text = "-"
            areaTextView.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        } else {
            areaTextView.text = selectedArea
            areaTextView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
        }

        // 更新容器显示
        if (selectedContainer.isNullOrEmpty()) {
            containerTextView.text = "-"
            containerTextView.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        } else {
            containerTextView.text = selectedContainer
            containerTextView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
        }

        // 更新子位置显示
        if (selectedSublocation.isNullOrEmpty()) {
            sublocationTextView.text = "-"
            sublocationTextView.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        } else {
            sublocationTextView.text = selectedSublocation
            sublocationTextView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
        }

        // 根据选择状态设置子级控件的可点击状态
        containerTextView.isEnabled = !selectedArea.isNullOrEmpty()
        sublocationTextView.isEnabled = !selectedContainer.isNullOrEmpty()
    }

    /**
     * 通知位置变化
     */
    private fun notifyLocationChanged() {
        onLocationSelectedListener?.invoke(selectedArea, selectedContainer, selectedSublocation)
    }

    /**
     * 从列表中编辑区域
     */
    private fun showEditAreaDialogFromList(areaName: String) {
        // 创建一个包含EditText的LinearLayout
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 30, 50, 30)

        val editText = EditText(context).apply {
            setText(areaName)
            setSelection(areaName.length)
            hint = "请输入区域名称"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        layout.addView(editText)

        AlertDialog.Builder(context)
            .setTitle("编辑区域")
            .setView(layout)
            .setPositiveButton("确定") { _, _ ->
                val newAreaName = editText.text.toString().trim()
                if (newAreaName.isEmpty()) {
                    Toast.makeText(context, "区域名称不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newAreaName != areaName) {
                    // 更新区域名称
                    locationManager.renameCustomArea(areaName, newAreaName)

                    // 如果当前选中的是这个区域，更新选中状态
                    if (selectedArea == areaName) {
                        setSelectedLocation(newAreaName, selectedContainer, selectedSublocation)
                        notifyLocationChanged()
                    }

                    // 重新打开区域选择对话框以显示更新后的内容
                    showAreaSelectionDialog()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 从列表中删除区域
     */
    private fun showDeleteAreaConfirmDialogFromList(areaName: String) {
        val message = "确定删除区域\"${areaName}\"吗？"

        AlertDialog.Builder(context)
            .setTitle("删除区域")
            .setMessage(message)
            .setPositiveButton("删除") { _, _ ->
                // 删除区域
                locationManager.removeCustomArea(areaName)

                // 如果当前选中的是这个区域，清除选择
                if (selectedArea == areaName) {
                    setSelectedLocation(null, null, null)
                    notifyLocationChanged()
                    updateViewState()
                }

                // 重新打开区域选择对话框以显示更新后的内容
                showAreaSelectionDialog()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 从列表中编辑容器
     */
    private fun showEditContainerDialogFromList(containerName: String) {
        // 创建一个包含EditText的LinearLayout
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 30, 50, 30)

        val editText = EditText(context).apply {
            setText(containerName)
            setSelection(containerName.length)
            hint = "请输入容器名称"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        layout.addView(editText)

        AlertDialog.Builder(context)
            .setTitle("编辑容器")
            .setView(layout)
            .setPositiveButton("确定") { _, _ ->
                val newContainerName = editText.text.toString().trim()
                if (newContainerName.isEmpty()) {
                    Toast.makeText(context, "容器名称不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newContainerName != containerName) {
                    // 更新容器名称
                    locationManager.renameCustomContainer(selectedArea!!, containerName, newContainerName)

                    // 如果当前选中的是这个容器，更新选中状态
                    if (selectedContainer == containerName) {
                        setSelectedLocation(selectedArea, newContainerName, selectedSublocation)
                        notifyLocationChanged()
                        updateViewState()
                    }

                    // 重新打开容器选择对话框以显示更新后的内容
                    showContainerSelectionDialog()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 从列表中删除容器
     */
    private fun showDeleteContainerConfirmDialogFromList(containerName: String) {
        val message = "确定删除容器\"${containerName}\"吗？"

        AlertDialog.Builder(context)
            .setTitle("删除容器")
            .setMessage(message)
            .setPositiveButton("删除") { _, _ ->
                // 删除容器
                locationManager.removeCustomContainer(selectedArea!!, containerName)

                // 如果当前选中的是这个容器，清除选择
                if (selectedContainer == containerName) {
                    setSelectedLocation(selectedArea, null, null)
                    notifyLocationChanged()
                    updateViewState()
                }

                // 重新打开容器选择对话框以显示更新后的内容
                showContainerSelectionDialog()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 从列表中编辑子位置
     */
    private fun showEditSublocationDialogFromList(sublocationName: String) {
        // 创建一个包含EditText的LinearLayout
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 30, 50, 30)

        val editText = EditText(context).apply {
            setText(sublocationName)
            setSelection(sublocationName.length)
            hint = "请输入子位置名称"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        layout.addView(editText)

        AlertDialog.Builder(context)
            .setTitle("编辑子位置")
            .setView(layout)
            .setPositiveButton("确定") { _, _ ->
                val newSublocationName = editText.text.toString().trim()
                if (newSublocationName.isEmpty()) {
                    Toast.makeText(context, "子位置名称不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newSublocationName != sublocationName) {
                    // 更新子位置名称
                    locationManager.renameCustomSublocation(selectedContainer!!, sublocationName, newSublocationName)

                    // 如果当前选中的是这个子位置，更新选中状态
                    if (selectedSublocation == sublocationName) {
                        setSelectedLocation(selectedArea, selectedContainer, newSublocationName)
                        notifyLocationChanged()
                        updateViewState()
                    }

                    // 重新打开子位置选择对话框以显示更新后的内容
                    showSublocationSelectionDialog()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 从列表中删除子位置
     */
    private fun showDeleteSublocationConfirmDialogFromList(sublocationName: String) {
        if (selectedContainer.isNullOrEmpty()) return

        AlertDialog.Builder(context)
            .setTitle("删除子位置")
            .setMessage("确定删除子位置\"${sublocationName}\"吗？")
            .setPositiveButton("删除") { _, _ ->
                // 删除子位置
                locationManager.removeCustomSublocation(selectedContainer!!, sublocationName)

                // 如果当前选中的是这个子位置，清除选择
                if (selectedSublocation == sublocationName) {
                    setSelectedLocation(selectedArea, selectedContainer, null)
                    notifyLocationChanged()
                    updateViewState()
                }

                // 重新打开子位置选择对话框以显示更新后的内容
                showSublocationSelectionDialog()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}