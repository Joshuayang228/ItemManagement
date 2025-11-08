package com.example.itemmanagement.ui.add

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.*
import androidx.core.content.ContextCompat
import com.example.itemmanagement.R
import com.example.itemmanagement.ui.base.FieldInteractionViewModel
import com.example.itemmanagement.ui.utils.Material3DialogFactory
import com.example.itemmanagement.utils.SnackbarHelper

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
    private lateinit var viewModel: FieldInteractionViewModel

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
    fun initialize(locationManager: LocationManager, viewModel: FieldInteractionViewModel) {
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

        // 创建Material 3对话框
        val builder = Material3DialogFactory.createSelectionDialog(
            context = context,
            title = "选择区域",
            items = areas,
            onItemSelected = { selectedAreaName, _ ->
                setSelectedLocation(selectedAreaName, null, null)
                notifyLocationChanged()
            },
            onNeutralClick = { showAddCustomAreaDialog() },
            onPositiveClick = if (!selectedArea.isNullOrEmpty()) {
                { clearSelection(); notifyLocationChanged() }
            } else null,
            neutralText = "添加自定义",
            positiveText = if (!selectedArea.isNullOrEmpty()) "清除" else null
        )

        // 创建ListView并设置长按监听器
        val dialog = builder.create()
        dialog.setOnShowListener {
            // 如果是第一次显示对话框，显示提示
            if (!hasShownAreaDialog) {
                SnackbarHelper.show(this, "长按选项可编辑或删除")
                hasShownAreaDialog = true
            }

            val listView = dialog.listView
            listView?.setOnItemLongClickListener { _, _, position, _ ->
                val areaName = areas[position]

                // 显示编辑/删除选项
                val options = arrayOf("编辑", "删除")
                Material3DialogFactory.createActionDialog(
                    context = context,
                    title = "区域操作",
                    actions = options,
                    onActionSelected = { which ->
                        when (which) {
                            0 -> showEditAreaDialogFromList(areaName)
                            1 -> showDeleteAreaConfirmDialogFromList(areaName)
                        }
                        dialog.dismiss() // 关闭选择对话框
                    }
                ).show()

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
            SnackbarHelper.show(this, "请先选择区域")
            showAreaSelectionDialog()
            return
        }

        if (!::locationManager.isInitialized) {
            return
        }

        val containers = locationManager.getContainersByArea(selectedArea!!)

        // 创建Material 3对话框
        val builder = Material3DialogFactory.createSelectionDialog(
            context = context,
            title = "选择容器",
            items = containers,
            onItemSelected = { selectedContainerName, _ ->
                setSelectedLocation(selectedArea, selectedContainerName, null)
                notifyLocationChanged()
            },
            onNeutralClick = { showAddCustomContainerDialog() },
            onPositiveClick = if (!selectedContainer.isNullOrEmpty()) {
                { setSelectedLocation(selectedArea, null, null); notifyLocationChanged() }
            } else null,
            neutralText = "添加自定义",
            positiveText = if (!selectedContainer.isNullOrEmpty()) "清除" else null
        )

        // 创建ListView并设置长按监听器
        val dialog = builder.create()
        dialog.setOnShowListener {
            // 如果是第一次显示对话框，显示提示
            if (!hasShownContainerDialog) {
                SnackbarHelper.show(this, "长按选项可编辑或删除")
                hasShownContainerDialog = true
            }

            val listView = dialog.listView
            listView?.setOnItemLongClickListener { _, _, position, _ ->
                val containerName = containers[position]

                // 显示编辑/删除选项
                val options = arrayOf("编辑", "删除")
                Material3DialogFactory.createActionDialog(
                    context = context,
                    title = "容器操作",
                    actions = options,
                    onActionSelected = { which ->
                        when (which) {
                            0 -> showEditContainerDialogFromList(containerName)
                            1 -> showDeleteContainerConfirmDialogFromList(containerName)
                        }
                        dialog.dismiss() // 关闭选择对话框
                    }
                ).show()

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
            SnackbarHelper.show(this, "请先选择容器")
            showContainerSelectionDialog()
            return
        }

        if (!::locationManager.isInitialized) {
            return
        }

        val sublocations = locationManager.getSublocationsByContainer(selectedContainer!!)
        val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, sublocations)

        // 创建Material 3对话框
        val builder = Material3DialogFactory.createSelectionDialog(
            context = context,
            title = "选择子位置",
            items = sublocations,
            onItemSelected = { selectedSublocationName, _ ->
                setSelectedLocation(selectedArea, selectedContainer, selectedSublocationName)
                notifyLocationChanged()
            },
            onNeutralClick = { showAddCustomSublocationDialog() },
            onPositiveClick = if (!selectedSublocation.isNullOrEmpty()) {
                { setSelectedLocation(selectedArea, selectedContainer, null); notifyLocationChanged() }
            } else null,
            neutralText = "添加自定义",
            positiveText = if (!selectedSublocation.isNullOrEmpty()) "清除" else null
        )

        // 创建ListView并设置长按监听器
        val dialog = builder.create()
        dialog.setOnShowListener {
            // 如果是第一次显示对话框，显示提示
            if (!hasShownSublocationDialog) {
                SnackbarHelper.show(this, "长按选项可编辑或删除")
                hasShownSublocationDialog = true
            }

            val listView = dialog.listView
            listView?.setOnItemLongClickListener { _, _, position, _ ->
                val sublocationName = sublocations[position]

                // 显示编辑/删除选项
                val options = arrayOf("编辑", "删除")
                Material3DialogFactory.createActionDialog(
                    context = context,
                    title = "子位置操作",
                    actions = options,
                    onActionSelected = { which ->
                        when (which) {
                            0 -> showEditSublocationDialogFromList(sublocationName)
                            1 -> showDeleteSublocationConfirmDialogFromList(sublocationName)
                        }
                        dialog.dismiss() // 关闭选择对话框
                    }
                ).show()

                true
            }
        }

        dialog.show()
    }

    /**
     * 显示添加自定义区域对话框
     */
    private fun showAddCustomAreaDialog() {
        Material3DialogFactory.createInputDialog(
            context = context,
            title = "添加自定义区域",
            hint = "请输入自定义区域名称",
            onConfirm = { newArea ->
                if (newArea.isNotEmpty()) {
                    locationManager.addCustomArea(newArea)
                    setSelectedLocation(newArea, null, null)
                    notifyLocationChanged()
                    updateViewState()
                } else {
                    Toast.makeText(context, "区域名称不能为空", Toast.LENGTH_SHORT).show()
                }
            }
        ).show()
    }

    /**
     * 显示添加自定义容器对话框
     */
    private fun showAddCustomContainerDialog() {
        if (selectedArea.isNullOrEmpty()) {
            SnackbarHelper.show(this, "请先选择区域")
            return
        }

        Material3DialogFactory.createInputDialog(
            context = context,
            title = "添加自定义容器",
            hint = "请输入自定义容器名称",
            onConfirm = { newContainer ->
                if (newContainer.isNotEmpty()) {
                    locationManager.addCustomContainerToArea(selectedArea!!, newContainer)
                    setSelectedLocation(selectedArea, newContainer, null)
                    notifyLocationChanged()
                    updateViewState()
                } else {
                    Toast.makeText(context, "容器名称不能为空", Toast.LENGTH_SHORT).show()
                }
            }
        ).show()
    }

    /**
     * 显示添加自定义子位置对话框
     */
    private fun showAddCustomSublocationDialog() {
        if (selectedContainer.isNullOrEmpty()) {
            SnackbarHelper.show(this, "请先选择容器")
            return
        }

        Material3DialogFactory.createInputDialog(
            context = context,
            title = "添加自定义子位置",
            hint = "请输入自定义子位置名称",
            onConfirm = { newSublocation ->
                if (newSublocation.isNotEmpty()) {
                    locationManager.addCustomSublocationToContainer(selectedContainer!!, newSublocation)
                    setSelectedLocation(selectedArea, selectedContainer, newSublocation)
                    notifyLocationChanged()
                    updateViewState()
                } else {
                    Toast.makeText(context, "子位置名称不能为空", Toast.LENGTH_SHORT).show()
                }
            }
        ).show()
    }

    /**
     * 更新视图状态
     */
    private fun updateViewState() {
        // 更新区域显示
        if (selectedArea.isNullOrEmpty()) {
            areaTextView.text = ""
            areaTextView.hint = "选择位置"
            areaTextView.setHintTextColor(ContextCompat.getColor(context, R.color.hint_text_color))
        } else {
            areaTextView.text = selectedArea
            areaTextView.setTextColor(ContextCompat.getColor(context, R.color.field_value_color))
        }

        // 更新容器显示
        if (selectedContainer.isNullOrEmpty()) {
            containerTextView.text = ""
            containerTextView.hint = "-"
            containerTextView.setHintTextColor(ContextCompat.getColor(context, R.color.hint_text_color))
        } else {
            containerTextView.text = selectedContainer
            containerTextView.setTextColor(ContextCompat.getColor(context, R.color.field_value_color))
        }

        // 更新子位置显示
        if (selectedSublocation.isNullOrEmpty()) {
            sublocationTextView.text = ""
            sublocationTextView.hint = "-"
            sublocationTextView.setHintTextColor(ContextCompat.getColor(context, R.color.hint_text_color))
        } else {
            sublocationTextView.text = selectedSublocation
            sublocationTextView.setTextColor(ContextCompat.getColor(context, R.color.field_value_color))
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
        Material3DialogFactory.createInputDialog(
            context = context,
            title = "编辑区域",
            hint = "请输入区域名称",
            initialText = areaName,
            onConfirm = { newAreaName ->
                if (newAreaName.isEmpty()) {
                    Toast.makeText(context, "区域名称不能为空", Toast.LENGTH_SHORT).show()
                    return@createInputDialog
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
        ).show()
    }

    /**
     * 从列表中删除区域
     */
    private fun showDeleteAreaConfirmDialogFromList(areaName: String) {
        val message = "确定删除区域\"${areaName}\"吗？"

        Material3DialogFactory.createConfirmDialog(
            context = context,
            title = "删除区域",
            message = message,
            positiveText = "删除",
            onConfirm = {
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
        ).show()
    }

    /**
     * 从列表中编辑容器
     */
    private fun showEditContainerDialogFromList(containerName: String) {
        Material3DialogFactory.createInputDialog(
            context = context,
            title = "编辑容器",
            hint = "请输入容器名称",
            initialText = containerName,
            onConfirm = { newContainerName ->
                if (newContainerName.isEmpty()) {
                    Toast.makeText(context, "容器名称不能为空", Toast.LENGTH_SHORT).show()
                    return@createInputDialog
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
        ).show()
    }

    /**
     * 从列表中删除容器
     */
    private fun showDeleteContainerConfirmDialogFromList(containerName: String) {
        val message = "确定删除容器\"${containerName}\"吗？"

        Material3DialogFactory.createConfirmDialog(
            context = context,
            title = "删除容器",
            message = message,
            positiveText = "删除",
            onConfirm = {
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
        ).show()
    }

    /**
     * 从列表中编辑子位置
     */
    private fun showEditSublocationDialogFromList(sublocationName: String) {
        Material3DialogFactory.createInputDialog(
            context = context,
            title = "编辑子位置",
            hint = "请输入子位置名称",
            initialText = sublocationName,
            onConfirm = { newSublocationName ->
                if (newSublocationName.isEmpty()) {
                    Toast.makeText(context, "子位置名称不能为空", Toast.LENGTH_SHORT).show()
                    return@createInputDialog
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
        ).show()
    }

    /**
     * 从列表中删除子位置
     */
    private fun showDeleteSublocationConfirmDialogFromList(sublocationName: String) {
        if (selectedContainer.isNullOrEmpty()) return

        Material3DialogFactory.createConfirmDialog(
            context = context,
            title = "删除子位置",
            message = "确定删除子位置\"${sublocationName}\"吗？",
            positiveText = "删除",
            onConfirm = {
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
        ).show()
    }
}