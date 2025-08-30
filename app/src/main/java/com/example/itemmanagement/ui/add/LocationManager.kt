package com.example.itemmanagement.ui.add

import android.content.Context
import com.example.itemmanagement.data.model.CustomLocationData
import com.example.itemmanagement.ui.base.FieldInteractionViewModel

/**
 * 位置管理器，负责管理位置的层级关系
 */
class LocationManager(
    private val context: Context,
    private val viewModel: FieldInteractionViewModel
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

    // 所有区域（包括默认和自定义）
    // private val allAreas = mutableListOf<String>() // 移除这个变量，现在通过getAllAreas()方法动态获取

    // 用户自定义区域
    private val customAreas = mutableListOf<String>()

    // 用户自定义容器映射 (区域 -> 容器列表)
    private val customAreaContainerMap = mutableMapOf<String, MutableList<String>>()

    // 用户自定义子位置映射 (容器 -> 子位置列表)
    private val customContainerSublocationMap = mutableMapOf<String, MutableList<String>>()

    init {
        // 从 ViewModel 恢复自定义数据
        loadCustomData()
    }

    // 从 ViewModel 加载自定义数据
    private fun loadCustomData() {
        viewModel.getCustomLocations()?.let { customData ->
            customData.customAreas?.let { areas ->
                customAreas.addAll(areas)
            }

            customData.customAreaContainerMap?.forEach { (area, containers) ->
                customAreaContainerMap[area] = containers.toMutableList()
            }

            customData.customContainerSublocationMap?.forEach { (container, sublocations) ->
                customContainerSublocationMap[container] = sublocations.toMutableList()
            }
        }
    }

    // 保存自定义数据到 ViewModel
    private fun saveCustomData() {
        val customData = CustomLocationData(
            customAreas = customAreas,
            customAreaContainerMap = customAreaContainerMap,
            customContainerSublocationMap = customContainerSublocationMap
        )
        viewModel.saveCustomLocations(customData)
    }

    // 获取所有区域
    fun getAllAreas(): List<String> {
        val areas = mutableListOf<String>()
        
        // 添加默认区域
        areas.addAll(DEFAULT_AREAS)
        
        // 添加自定义区域
        customAreas.forEach { area ->
            if (!areas.contains(area)) {
                areas.add(area)
            }
        }
        
        // 处理被删除的默认区域
        val deletedAreas = customAreas.filter { it.startsWith("DELETED:") }
            .map { it.removePrefix("DELETED:") }
        areas.removeAll(deletedAreas)
        
        // 处理被编辑的默认区域
        val editedAreas = customAreas.filter { it.startsWith("EDIT:") }
        editedAreas.forEach { editMapping ->
            val mapping = editMapping.removePrefix("EDIT:")
            if (mapping.contains("->")) {
                val parts = mapping.split("->")
                if (parts.size == 2) {
                    val oldName = parts[0]
                    val newName = parts[1]
                    val index = areas.indexOf(oldName)
                    if (index >= 0) {
                        areas[index] = newName
                    }
                }
            }
        }
        
        return areas
    }

    // 根据区域获取容器列表
    fun getContainersByArea(area: String): List<String> {
        val containers = mutableListOf<String>()

        // 添加默认容器
        DEFAULT_AREA_CONTAINER_MAP[area]?.let { defaultContainers ->
            containers.addAll(defaultContainers)
        }

        // 添加自定义容器
        customAreaContainerMap[area]?.let { customContainers ->
            customContainers.forEach { container ->
                if (!containers.contains(container)) {
                    containers.add(container)
                }
            }
        }

        // 处理被删除的默认容器
        val deletedContainers = customAreaContainerMap["DELETED_CONTAINERS_$area"] ?: mutableListOf()
        containers.removeAll(deletedContainers)

        // 处理被编辑的默认容器
        val editedContainers = customAreaContainerMap["EDITED_CONTAINERS_$area"] ?: mutableListOf()
        editedContainers.forEach { editMapping ->
            if (editMapping.contains("->")) {
                val parts = editMapping.split("->")
                if (parts.size == 2) {
                    val oldName = parts[0]
                    val newName = parts[1]
                    val index = containers.indexOf(oldName)
                    if (index >= 0) {
                        containers[index] = newName
                    }
                }
            }
        }

        return containers
    }

    // 根据容器获取子位置列表
    fun getSublocationsByContainer(container: String): List<String> {
        val sublocations = mutableListOf<String>()

        // 添加默认子位置
        DEFAULT_CONTAINER_SUBLOCATION_MAP[container]?.let { defaultSublocations ->
            sublocations.addAll(defaultSublocations)
        }

        // 添加自定义子位置
        customContainerSublocationMap[container]?.let { customSublocations ->
            customSublocations.forEach { sublocation ->
                if (!sublocations.contains(sublocation)) {
                    sublocations.add(sublocation)
                }
            }
        }

        // 处理被删除的默认子位置
        val deletedSublocations = customContainerSublocationMap["DELETED_SUBLOCATIONS_$container"] ?: mutableListOf()
        sublocations.removeAll(deletedSublocations)

        // 处理被编辑的默认子位置
        val editedSublocations = customContainerSublocationMap["EDITED_SUBLOCATIONS_$container"] ?: mutableListOf()
        editedSublocations.forEach { editMapping ->
            if (editMapping.contains("->")) {
                val parts = editMapping.split("->")
                if (parts.size == 2) {
                    val oldName = parts[0]
                    val newName = parts[1]
                    val index = sublocations.indexOf(oldName)
                    if (index >= 0) {
                        sublocations[index] = newName
                    }
                }
            }
        }

        return sublocations
    }

    // 添加自定义区域
    fun addCustomArea(area: String) {
        if (area.isBlank()) {
            return
        }

        if (!getAllAreas().contains(area)) {
            customAreas.add(area)
            saveCustomData()
        }
    }

    // 添加自定义容器到区域
    fun addCustomContainerToArea(area: String, container: String) {
        if (area.isBlank() || container.isBlank()) {
            return
        }

        if (!customAreaContainerMap.containsKey(area)) {
            customAreaContainerMap[area] = mutableListOf()
        }

        val containers = customAreaContainerMap[area]!!
        if (!containers.contains(container) &&
            (DEFAULT_AREA_CONTAINER_MAP[area]?.contains(container) != true)) {
            containers.add(container)
            saveCustomData()
        }
    }

    // 添加自定义子位置到容器
    fun addCustomSublocationToContainer(container: String, sublocation: String) {
        if (container.isBlank() || sublocation.isBlank()) {
            return
        }

        if (!customContainerSublocationMap.containsKey(container)) {
            customContainerSublocationMap[container] = mutableListOf()
        }

        val sublocations = customContainerSublocationMap[container]!!
        if (!sublocations.contains(sublocation) &&
            (DEFAULT_CONTAINER_SUBLOCATION_MAP[container]?.contains(sublocation) != true)) {
            sublocations.add(sublocation)
            saveCustomData()
        }
    }

    // 删除区域（级联删除）
    fun removeCustomArea(area: String) {
        // 获取该区域下的所有容器
        val containers = getContainersByArea(area)

        // 级联删除该区域下的所有容器及子位置
        containers.forEach { container ->
            removeCustomContainer(area, container)
        }

        // 检查是否是默认区域
        val isDefaultArea = DEFAULT_AREAS.contains(area)
        
        if (isDefaultArea) {
            // 如果是默认区域，记录删除标记
            val deletedAreaMark = "DELETED:$area"
            if (!customAreas.contains(deletedAreaMark)) {
                customAreas.add(deletedAreaMark)
            }
        } else {
            // 如果是自定义区域，从customAreas中移除
            customAreas.remove(area)
        }

        // 从区域-容器映射中移除
        customAreaContainerMap.remove(area)

        // 保存更改
        saveCustomData()
    }

    // 删除自定义容器（级联删除）
    fun removeCustomContainer(area: String, container: String) {
        // 获取该容器下的所有子位置
        val sublocations = getSublocationsByContainer(container)

        // 级联删除该容器下的所有子位置
        sublocations.forEach { sublocation ->
            removeCustomSublocation(container, sublocation)
        }

        // 检查是否是默认容器
        val isDefaultContainer = getDefaultContainersByArea(area).contains(container)
        
        if (isDefaultContainer) {
            // 如果是默认容器，记录删除标记
            val deletedContainersKey = "DELETED_CONTAINERS_$area"
            if (!customAreaContainerMap.containsKey(deletedContainersKey)) {
                customAreaContainerMap[deletedContainersKey] = mutableListOf()
            }
            customAreaContainerMap[deletedContainersKey]!!.add(container)
        } else {
            // 如果是自定义容器，从区域-容器映射中移除
            customAreaContainerMap[area]?.let { containers ->
                if (containers.contains(container)) {
                    (containers as MutableList<String>).remove(container)

                    if (containers.isEmpty()) {
                        customAreaContainerMap.remove(area)
                    }
                }
            }
        }

        // 从容器-子位置映射中移除
        customContainerSublocationMap.remove(container)

        // 保存更改
        saveCustomData()
    }

    // 删除自定义子位置
    fun removeCustomSublocation(container: String, sublocation: String) {
        // 检查是否是默认子位置
        val isDefaultSublocation = getDefaultSublocationsByContainer(container).contains(sublocation)
        
        if (isDefaultSublocation) {
            // 如果是默认子位置，记录删除标记
            val deletedSublocationsKey = "DELETED_SUBLOCATIONS_$container"
            if (!customContainerSublocationMap.containsKey(deletedSublocationsKey)) {
                customContainerSublocationMap[deletedSublocationsKey] = mutableListOf()
            }
            customContainerSublocationMap[deletedSublocationsKey]!!.add(sublocation)
        } else {
            // 如果是自定义子位置，从容器-子位置映射中移除
            customContainerSublocationMap[container]?.let { sublocations ->
                if (sublocations.contains(sublocation)) {
                    (sublocations as MutableList<String>).remove(sublocation)

                    if (sublocations.isEmpty()) {
                        customContainerSublocationMap.remove(container)
                    }
                }
            }
        }

        // 保存更改
        saveCustomData()
    }

    // 获取默认容器列表
    fun getDefaultContainersByArea(area: String): List<String> {
        return DEFAULT_AREA_CONTAINER_MAP[area] ?: emptyList()
    }

    // 获取默认子位置列表
    fun getDefaultSublocationsByContainer(container: String): List<String> {
        return DEFAULT_CONTAINER_SUBLOCATION_MAP[container] ?: emptyList()
    }

    // 重命名区域
    fun renameCustomArea(oldName: String, newName: String) {
        if (oldName == newName || oldName.isBlank() || newName.isBlank()) {
            return
        }

        // 检查新名称是否已存在
        val allAreas = getAllAreas()
        if (allAreas.contains(newName)) {
            throw IllegalArgumentException("区域名称 '$newName' 已存在")
        }

        // 检查是否是默认区域
        val isDefaultArea = DEFAULT_AREAS.contains(oldName)
        
        if (isDefaultArea) {
            // 如果是默认区域，记录编辑映射
            val editMapping = "EDIT:$oldName->$newName"
            if (!customAreas.contains(editMapping)) {
                customAreas.add(editMapping)
            }
        } else {
            // 如果是自定义区域，直接更新
            if (customAreas.contains(oldName)) {
                customAreas.remove(oldName)
                customAreas.add(newName)
            }
        }

        // 更新容器映射
        val containers = customAreaContainerMap[oldName]
        if (containers != null) {
            customAreaContainerMap[newName] = containers
            customAreaContainerMap.remove(oldName)
        }

        // 保存更改
        saveCustomData()
    }

    // 重命名自定义容器
    fun renameCustomContainer(area: String, oldName: String, newName: String) {
        if (oldName == newName || oldName.isBlank() || newName.isBlank()) {
            return
        }

        // 检查新名称是否已存在
        val allContainers = getContainersByArea(area)
        if (allContainers.contains(newName)) {
            throw IllegalArgumentException("容器名称 '$newName' 已存在")
        }

        // 检查是否是默认容器
        val isDefaultContainer = getDefaultContainersByArea(area).contains(oldName)
        
        if (isDefaultContainer) {
            // 如果是默认容器，记录编辑映射
            val editedContainersKey = "EDITED_CONTAINERS_$area"
            if (!customAreaContainerMap.containsKey(editedContainersKey)) {
                customAreaContainerMap[editedContainersKey] = mutableListOf()
            }
            customAreaContainerMap[editedContainersKey]!!.add("$oldName->$newName")
        } else {
            // 如果是自定义容器，直接更新
            val containers = customAreaContainerMap[area]
            if (containers != null && containers.contains(oldName)) {
                val index = containers.indexOf(oldName)
                if (index >= 0) {
                    (containers as MutableList<String>)[index] = newName
                }
            }
        }

        // 更新子位置映射（容器名改变时）
        val sublocations = customContainerSublocationMap[oldName]
        if (sublocations != null) {
            customContainerSublocationMap[newName] = sublocations
            customContainerSublocationMap.remove(oldName)
        }

        // 保存更改
        saveCustomData()
    }

    // 重命名自定义子位置
    fun renameCustomSublocation(container: String, oldName: String, newName: String) {
        if (oldName == newName || oldName.isBlank() || newName.isBlank()) {
            return
        }

        // 检查新名称是否已存在
        val allSublocations = getSublocationsByContainer(container)
        if (allSublocations.contains(newName)) {
            throw IllegalArgumentException("子位置名称 '$newName' 已存在")
        }

        // 检查是否是默认子位置
        val isDefaultSublocation = getDefaultSublocationsByContainer(container).contains(oldName)
        
        if (isDefaultSublocation) {
            // 如果是默认子位置，记录编辑映射
            val editedSublocationsKey = "EDITED_SUBLOCATIONS_$container"
            if (!customContainerSublocationMap.containsKey(editedSublocationsKey)) {
                customContainerSublocationMap[editedSublocationsKey] = mutableListOf()
            }
            customContainerSublocationMap[editedSublocationsKey]!!.add("$oldName->$newName")
        } else {
            // 如果是自定义子位置，直接更新
            val sublocations = customContainerSublocationMap[container]
            if (sublocations != null && sublocations.contains(oldName)) {
                val index = sublocations.indexOf(oldName)
                if (index >= 0) {
                    (sublocations as MutableList<String>)[index] = newName
                }
            }
        }

        // 保存更改
        saveCustomData()
    }
}

/**
 * 自定义位置数据类
 */
data class CustomLocationData(
    val customAreas: List<String>? = null,
    val customAreaContainerMap: Map<String, List<String>>? = null,
    val customContainerSublocationMap: Map<String, List<String>>? = null
) : java.io.Serializable