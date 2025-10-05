package com.example.itemmanagement.ui.shopping

import com.example.itemmanagement.ui.common.FieldProperties
import com.example.itemmanagement.ui.common.ValidationType
import com.example.itemmanagement.ui.common.DisplayStyle
import com.example.itemmanagement.ui.add.Field

/**
 * 购物专用字段管理器
 * 完全独立于添加库存物品的字段管理
 */
object ShoppingFieldManager {
    
    /**
     * 购物字段专用排序
     * 不影响添加库存物品的字段排序
     */
    fun getShoppingFieldOrder(name: String): Int = when(name) {
        // 基础信息字段
        "名称" -> 1
        "数量" -> 2
        "分类" -> 3
        "子分类" -> 4
        "品牌" -> 5
        "备注" -> 6
        
        // 购物特有字段 - 优先显示
        "预估价格" -> 7
        "实际价格" -> 8
        "预算上限" -> 9
        "首选商店" -> 10
        "紧急程度" -> 11
        "截止日期" -> 12
        "提醒日期" -> 13
        "周期性购买" -> 14
        "周期间隔" -> 15
        "推荐原因" -> 16
        
        // 购买相关字段
        "购买渠道" -> 17
        "商家名称" -> 18
        "购买日期" -> 19
        
        // 其他通用字段
        "位置" -> 25
        "标签" -> 26
        "季节" -> 27
        "容量" -> 28
        "评分" -> 29
        "添加日期" -> 30
        "开封时间" -> 31
        "生产日期" -> 32
        "保质期" -> 33
        "保质过期时间" -> 34
        "保修期" -> 35
        "保修到期时间" -> 36
        "开封状态" -> 37
        "序列号" -> 38
        "加入心愿单" -> 39
        "高周转" -> 40
        else -> Int.MAX_VALUE
    }
    
    /**
     * 获取购物字段的分组
     */
    fun getShoppingFieldGroup(fieldName: String): String = when (fieldName) {
        "名称", "数量", "分类", "子分类", "品牌", "备注" -> "基础信息"
        "预估价格", "实际价格", "预算上限" -> "价格信息"
        "首选商店", "购买渠道", "商家名称", "购买日期" -> "购买信息"
        "紧急程度", "截止日期", "提醒日期" -> "优先级管理"
        "周期性购买", "周期间隔", "推荐原因" -> "特殊设置"
        else -> "其他"
    }
    
    /**
     * 获取购物专用的默认字段列表
     */
    fun getDefaultShoppingFields(): Set<String> = setOf(
        // 基础信息字段
        "名称",
        "数量", 
        "分类",
        "品牌",
        "备注",
        
        // 购物特有字段
        "预估价格",
        "实际价格", 
        "预算上限",
        "首选商店",
        "紧急程度",
        "截止日期",
        "购买渠道",
        "商家名称",
        "周期性购买",
        "提醒日期"
    )
    
    /**
     * 获取购物字段的默认值
     */
    fun getDefaultShoppingValues(): Map<String, String> = mapOf(
        "数量" to "1",
        "分类" to "未分类",
        "紧急程度" to "普通",
        "周期性购买" to "否"
    )
    
    /**
     * 创建购物专用的Field对象，使用购物字段排序
     */
    fun createShoppingField(group: String, name: String, isSelected: Boolean = false): Field {
        return Field(group, name, isSelected).apply {
            // 这里可以添加购物专用的field属性设置
        }
    }
    
    /**
     * 获取购物字段的属性定义
     */
    fun getShoppingFieldProperties(): Map<String, FieldProperties> = mapOf(
        // 购物特有字段属性
        "预估价格" to FieldProperties(
            validationType = ValidationType.NUMBER,
            min = 0,
            hint = "预估价格",
            unit = "元"
        ),
        
        "实际价格" to FieldProperties(
            validationType = ValidationType.NUMBER,
            min = 0,
            hint = "实际价格",
            unit = "元"
        ),
        
        "预算上限" to FieldProperties(
            validationType = ValidationType.NUMBER,
            min = 0,
            hint = "预算上限",
            unit = "元"
        ),
        
        "首选商店" to FieldProperties(
            options = listOf("超市", "便利店", "网购", "专卖店", "商场", "菜市场"),
            isCustomizable = true,
            hint = "首选购买地点"
        ),
        
        "紧急程度" to FieldProperties(
            options = listOf("不急", "普通", "急需", "非常急需"),
            defaultValue = "普通"
        ),
        
        "截止日期" to FieldProperties(
            validationType = ValidationType.DATE,
            defaultDate = false,
            hint = "需要购买的截止日期"
        ),
        
        "提醒日期" to FieldProperties(
            validationType = ValidationType.DATE,
            defaultDate = false,
            hint = "提醒购买的日期"
        ),
        
        "周期性购买" to FieldProperties(
            options = listOf("否", "是"),
            defaultValue = "否"
        ),
        
        "周期间隔" to FieldProperties(
            validationType = ValidationType.NUMBER,
            min = 1,
            max = 365,
            hint = "间隔天数",
            unit = "天"
        ),
        
        "推荐原因" to FieldProperties(
            validationType = ValidationType.TEXT,
            hint = "为什么推荐购买此物品",
            isMultiline = true,
            maxLines = 3
        )
    )
} 