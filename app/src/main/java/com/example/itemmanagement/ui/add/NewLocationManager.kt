package com.example.itemmanagement.ui.add

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.example.itemmanagement.data.model.CustomLocationData
import com.example.itemmanagement.ui.base.BaseItemViewModel

/**
 * 新架构的位置管理器，负责管理位置的层级关系
 * 基于BaseItemViewModel
 */
class NewLocationManager(
    private val context: Context,
    private val viewModel: BaseItemViewModel
) {
    companion object {
        // 默认区域列表
        private val DEFAULT_AREAS = listOf("厨房", "客厅", "主卧", "次卧", "卫生间", "阳台", "书房", "储物间", "其他")

        // 默认区域-容器映射
        private val DEFAULT_AREA_CONTAINER_MAP = mapOf(
            "厨房" to listOf("冰箱", "橱柜", "调料架", "水槽下", "微波炉上", "厨房台面", "垃圾桶旁"),
            "客厅" to listOf("电视柜", "茶几", "沙发", "装饰柜", "角落", "窗台"),
            "主卧" to listOf("衣柜", "床头柜", "梳妆台", "床下", "床上", "书桌"),
            "次卧" to listOf("衣柜", "床头柜", "书桌", "床下", "床上"),
            "卫生间" to listOf("洗漱台", "浴室柜", "马桶旁", "浴缸旁", "淋浴间"),
            "阳台" to listOf("晾衣架", "洗衣机旁", "花盆旁", "储物箱"),
            "书房" to listOf("书柜", "书桌", "抽屉", "文件柜"),
            "储物间" to listOf("杂物架", "工具箱", "储物柜")
        )

        // 默认容器-子位置映射
        private val DEFAULT_CONTAINER_SUBLOCATION_MAP = mapOf(
            "冰箱" to listOf("冷冻室", "冷藏室", "冰箱门", "第一层", "第二层", "第三层", "蔬果盒"),
            "衣柜" to listOf("上层", "中层", "下层", "左侧", "右侧", "抽屉", "挂衣区"),
            "橱柜" to listOf("上层", "中层", "下层", "左侧", "右侧", "第一格", "第二格")
        )
    }

    // 用户自定义区域
    private val customAreas = mutableListOf<String>()

    // 用户自定义容器映射 (区域 -> 容器列表)
    private val customAreaContainerMap = mutableMapOf<String, MutableList<String>>()

    // 用户自定义子位置映射 (容器 -> 子位置列表)
    private val customContainerSublocationMap = mutableMapOf<String, MutableList<String>>()

    init {
        // 从 ViewModel 恢复自定义数据
        restoreCustomLocationData()
    }

    /**
     * 获取所有区域
     */
    fun getAllAreas(): List<String> {
        return DEFAULT_AREAS + customAreas
    }

    /**
     * 根据区域获取容器列表
     */
    fun getContainersByArea(area: String): List<String> {
        val defaultContainers = DEFAULT_AREA_CONTAINER_MAP[area] ?: emptyList()
        val customContainers = customAreaContainerMap[area] ?: emptyList()
        return defaultContainers + customContainers
    }

    /**
     * 根据容器获取子位置列表
     */
    fun getSublocationsByContainer(container: String): List<String> {
        val defaultSublocations = DEFAULT_CONTAINER_SUBLOCATION_MAP[container] ?: emptyList()
        val customSublocations = customContainerSublocationMap[container] ?: emptyList()
        return defaultSublocations + customSublocations
    }

    /**
     * 添加自定义区域
     */
    fun addCustomArea(area: String) {
        if (!customAreas.contains(area) && !DEFAULT_AREAS.contains(area)) {
            customAreas.add(area)
            saveCustomLocationData()
        }
    }

    /**
     * 添加自定义容器到指定区域
     */
    fun addCustomContainer(area: String, container: String) {
        val containers = customAreaContainerMap.getOrPut(area) { mutableListOf() }
        if (!containers.contains(container)) {
            containers.add(container)
            saveCustomLocationData()
        }
    }

    /**
     * 添加自定义子位置到指定容器
     */
    fun addCustomSublocation(container: String, sublocation: String) {
        val sublocations = customContainerSublocationMap.getOrPut(container) { mutableListOf() }
        if (!sublocations.contains(sublocation)) {
            sublocations.add(sublocation)
            saveCustomLocationData()
        }
    }

    /**
     * 删除自定义区域
     */
    fun removeCustomArea(area: String) {
        if (customAreas.remove(area)) {
            customAreaContainerMap.remove(area)
            saveCustomLocationData()
        }
    }

    /**
     * 删除自定义容器
     */
    fun removeCustomContainer(area: String, container: String) {
        customAreaContainerMap[area]?.remove(container)
        customContainerSublocationMap.remove(container)
        saveCustomLocationData()
    }

    /**
     * 删除自定义子位置
     */
    fun removeCustomSublocation(container: String, sublocation: String) {
        customContainerSublocationMap[container]?.remove(sublocation)
        saveCustomLocationData()
    }

    /**
     * 显示添加区域对话框
     */
    fun showAddAreaDialog(onAreaAdded: (String) -> Unit) {
        val editText = android.widget.EditText(context).apply {
            hint = "输入新区域名称"
        }
        
        AlertDialog.Builder(context)
            .setTitle("添加新区域")
            .setView(editText)
            .setPositiveButton("添加") { _, _ ->
                val newArea = editText.text.toString().trim()
                if (newArea.isNotEmpty() && !getAllAreas().contains(newArea)) {
                    addCustomArea(newArea)
                    onAreaAdded(newArea)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示添加容器对话框
     */
    fun showAddContainerDialog(area: String, onContainerAdded: (String) -> Unit) {
        val editText = android.widget.EditText(context).apply {
            hint = "输入新容器名称"
        }
        
        AlertDialog.Builder(context)
            .setTitle("添加新容器到 $area")
            .setView(editText)
            .setPositiveButton("添加") { _, _ ->
                val newContainer = editText.text.toString().trim()
                if (newContainer.isNotEmpty() && !getContainersByArea(area).contains(newContainer)) {
                    addCustomContainer(area, newContainer)
                    onContainerAdded(newContainer)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示添加子位置对话框
     */
    fun showAddSublocationDialog(container: String, onSublocationAdded: (String) -> Unit) {
        val editText = android.widget.EditText(context).apply {
            hint = "输入新子位置名称"
        }
        
        AlertDialog.Builder(context)
            .setTitle("添加新子位置到 $container")
            .setView(editText)
            .setPositiveButton("添加") { _, _ ->
                val newSublocation = editText.text.toString().trim()
                if (newSublocation.isNotEmpty() && !getSublocationsByContainer(container).contains(newSublocation)) {
                    addCustomSublocation(container, newSublocation)
                    onSublocationAdded(newSublocation)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示删除确认对话框
     */
    fun showDeleteConfirmationDialog(
        itemName: String,
        itemType: String,
        onConfirmed: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle("删除$itemType")
            .setMessage("确定要删除 \"$itemName\" 吗？这将同时删除其下属的所有子项目。")
            .setPositiveButton("删除") { _, _ -> onConfirmed() }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示位置选择对话框
     */
    fun showLocationSelectionDialog(
        currentLocation: String?,
        onLocationSelected: (String) -> Unit
    ) {
        val areas = getAllAreas()
        var selectedArea: String? = null
        var selectedContainer: String? = null
        var selectedSublocation: String? = null

        // 解析当前位置
        if (!currentLocation.isNullOrBlank()) {
            val parts = currentLocation.split(" - ")
            if (parts.isNotEmpty()) selectedArea = parts[0]
            if (parts.size > 1) selectedContainer = parts[1]
            if (parts.size > 2) selectedSublocation = parts[2]
        }

        // 显示区域选择对话框
        AlertDialog.Builder(context)
            .setTitle("选择区域")
            .setItems(areas.toTypedArray()) { _, which ->
                selectedArea = areas[which]
                showContainerSelectionDialog(selectedArea!!, selectedContainer, selectedSublocation, onLocationSelected)
            }
            .setNeutralButton("添加新区域") { _, _ ->
                showAddAreaDialog { newArea ->
                    selectedArea = newArea
                    showContainerSelectionDialog(selectedArea!!, selectedContainer, selectedSublocation, onLocationSelected)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示容器选择对话框
     */
    private fun showContainerSelectionDialog(
        area: String,
        currentContainer: String?,
        currentSublocation: String?,
        onLocationSelected: (String) -> Unit
    ) {
        val containers = getContainersByArea(area)
        val containerOptions = containers + "直接选择此区域"

        AlertDialog.Builder(context)
            .setTitle("选择 $area 中的容器")
            .setItems(containerOptions.toTypedArray()) { _, which ->
                if (which == containers.size) {
                    // 直接选择区域
                    onLocationSelected(area)
                } else {
                    val selectedContainer = containers[which]
                    showSublocationSelectionDialog(area, selectedContainer, currentSublocation, onLocationSelected)
                }
            }
            .setNeutralButton("添加新容器") { _, _ ->
                showAddContainerDialog(area) { newContainer ->
                    showSublocationSelectionDialog(area, newContainer, currentSublocation, onLocationSelected)
                }
            }
            .setNegativeButton("返回", null)
            .show()
    }

    /**
     * 显示子位置选择对话框
     */
    private fun showSublocationSelectionDialog(
        area: String,
        container: String,
        currentSublocation: String?,
        onLocationSelected: (String) -> Unit
    ) {
        val sublocations = getSublocationsByContainer(container)
        
        if (sublocations.isEmpty()) {
            // 没有子位置，直接选择容器
            onLocationSelected("$area - $container")
            return
        }

        val sublocationOptions = sublocations + "直接选择此容器"

        AlertDialog.Builder(context)
            .setTitle("选择 $container 中的具体位置")
            .setItems(sublocationOptions.toTypedArray()) { _, which ->
                if (which == sublocations.size) {
                    // 直接选择容器
                    onLocationSelected("$area - $container")
                } else {
                    val selectedSublocation = sublocations[which]
                    onLocationSelected("$area - $container - $selectedSublocation")
                }
            }
            .setNeutralButton("添加新位置") { _, _ ->
                showAddSublocationDialog(container) { newSublocation ->
                    onLocationSelected("$area - $container - $newSublocation")
                }
            }
            .setNegativeButton("返回", null)
            .show()
    }

    /**
     * 保存自定义位置数据到ViewModel
     */
    private fun saveCustomLocationData() {
        val customData = CustomLocationData(
            customAreas = customAreas,
            customAreaContainerMap = customAreaContainerMap,
            customContainerSublocationMap = customContainerSublocationMap
        )
        viewModel.saveFieldValue("custom_location_data", customData)
    }

    /**
     * 从ViewModel恢复自定义位置数据
     */
    private fun restoreCustomLocationData() {
        val customData = viewModel.getFieldValue("custom_location_data") as? CustomLocationData
        if (customData != null) {
            customAreas.clear()
            customAreas.addAll(customData.customAreas)
            
            customAreaContainerMap.clear()
            customData.customAreaContainerMap.forEach { (area, containers) ->
                customAreaContainerMap[area] = containers.toMutableList()
            }
            
            customContainerSublocationMap.clear()
            customData.customContainerSublocationMap.forEach { (container, sublocations) ->
                customContainerSublocationMap[container] = sublocations.toMutableList()
            }
        }
    }

    /**
     * 获取位置显示文本
     */
    fun getLocationDisplayText(location: String?): String {
        return when {
            location.isNullOrBlank() -> "未指定"
            else -> location
        }
    }

    /**
     * 清除所有自定义位置数据
     */
    fun clearAllCustomData() {
        customAreas.clear()
        customAreaContainerMap.clear()
        customContainerSublocationMap.clear()
        saveCustomLocationData()
    }
}