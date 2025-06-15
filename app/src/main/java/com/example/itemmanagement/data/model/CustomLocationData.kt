package com.example.itemmanagement.data.model

import java.io.Serializable

/**
 * 自定义位置数据，用于保存用户自定义的位置信息
 */
data class CustomLocationData(
    // 自定义区域列表
    val customAreas: List<String> = emptyList(),
    
    // 区域-容器映射：记录每个区域下的自定义容器
    val customAreaContainerMap: Map<String, List<String>> = emptyMap(),
    
    // 容器-子位置映射：记录每个容器下的自定义子位置
    val customContainerSublocationMap: Map<String, List<String>> = emptyMap()
) : Serializable 