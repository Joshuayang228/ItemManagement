package com.example.itemmanagement.ui.wishlist

import com.example.itemmanagement.ui.common.FieldProperties
import com.example.itemmanagement.ui.common.ValidationType
import com.example.itemmanagement.ui.common.DisplayStyle
import com.example.itemmanagement.ui.add.Field

/**
 * 心愿单专用字段管理器
 * 完全独立于添加库存物品和购物清单的字段管理
 * 专注于心愿单的核心定位："想要购买但还没购买的物品管理"
 */
object WishlistFieldManager {
    
    /**
     * 心愿单字段专用排序
     * 基于心愿单的业务逻辑优化排序
     */
    fun getWishlistFieldOrder(name: String): Int = when(name) {
        // 基础信息字段 - 核心必需信息
        "名称" -> 1
        "分类" -> 2
        "品牌" -> 3
        "规格" -> 4
        
        // 核心功能字段 - 心愿单特色（前移）
        "优先级" -> 5
        "紧急程度" -> 6
        "单价" -> 7
        "目标价格" -> 8
        
        // 购买计划字段
        "数量" -> 9
        "购买计划" -> 10
        
        // 购买偏好字段
        "首选渠道" -> 11
        
        // 可选详细信息字段（后移）
        "个人预算" -> 12
        "价格跟踪" -> 13
        "备注" -> 14
        "添加日期" -> 15
        
        // 其他通用字段（优先级较低）
        "子分类" -> 20
        "标签" -> 21
        "位置" -> 22
        "评分" -> 23
        "季节" -> 24
        else -> Int.MAX_VALUE
    }
    
    /**
     * 获取心愿单字段的分组
     * 基于心愿单的业务逻辑进行分组
     */
    fun getWishlistFieldGroup(fieldName: String): String = when (fieldName) {
        "名称", "分类", "品牌", "规格", "子分类" -> "基础信息"
        "优先级", "紧急程度", "单价", "目标价格" -> "核心功能"
        "数量", "购买计划", "首选渠道" -> "购买计划"
        "个人预算", "价格跟踪", "备注", "添加日期" -> "可选信息"
        else -> "其他"
    }
    
    /**
     * 获取心愿单专用的默认字段列表
     * 根据心愿单的核心价值和用户体验选择
     */
    fun getDefaultWishlistFields(): Set<String> {
        android.util.Log.d("WishlistFieldManager", "📋 获取默认心愿单字段列表")
        
        val fields = setOf(
            // 基础信息组 - 必需字段
            "名称",
            "分类", 
            "品牌",
            "规格",
            
            // 核心功能组 - 心愿单特色（前移）
            "优先级",
            "紧急程度",
            "单价",
            "目标价格",
            
            // 购买计划组
            "数量",
            "购买计划",
            "首选渠道",
            
            // 可选信息组
            "个人预算",
            "价格跟踪",
            "备注",
            "添加日期"
        )
        
        android.util.Log.d("WishlistFieldManager", "📊 默认字段总数: ${fields.size}")
        android.util.Log.d("WishlistFieldManager", "📋 默认字段列表: $fields")
        
        return fields
    }
    
    /**
     * 获取心愿单字段的默认值
     * 基于心愿单的常见使用场景
     */
    fun getDefaultWishlistValues(): Map<String, String> {
        android.util.Log.d("WishlistFieldManager", "💾 获取默认心愿单字段值")
        
        val values = mapOf(
            "数量" to "1",
            "分类" to "未分类",
            "优先级" to "普通",
            "紧急程度" to "不急",
            "价格跟踪" to "true",
            "购买计划" to "随时"
        )
        
        android.util.Log.d("WishlistFieldManager", "📊 默认值数量: ${values.size}")
        android.util.Log.d("WishlistFieldManager", "💾 默认值映射: $values")
        
        return values
    }
    
    /**
     * 创建心愿单专用的Field对象，使用心愿单字段排序
     */
    fun createWishlistField(group: String, name: String, isSelected: Boolean = false): Field {
        return Field(group, name, isSelected)
    }
    
    /**
     * 获取心愿单字段的属性定义
     * 专门针对心愿单的业务需求设计
     */
    fun getWishlistFieldProperties(): Map<String, FieldProperties> {
        android.util.Log.d("WishlistFieldManager", "🔧 获取心愿单字段属性定义")
        
        val properties = mapOf(
        
        // === 基础信息字段 ===
        "名称" to FieldProperties(
            isRequired = true,
            validationType = ValidationType.TEXT,
            hint = "请输入心愿单物品名称"
        ),
        
        "规格" to FieldProperties(
            validationType = ValidationType.TEXT,
            isMultiline = true,
            maxLines = 3,
            hint = "详细规格说明（如：iPhone 15 Pro Max 256GB 天然钛金色）"
        ),
        
        // === 价格管理字段 ===
        "单价" to FieldProperties(
            validationType = ValidationType.NUMBER,
            min = 0.0,
            hint = "当前市场价格",
            unitOptions = listOf("元", "美元", "欧元"),
            isCustomizable = true
        ),
        
        "目标价格" to FieldProperties(
            validationType = ValidationType.NUMBER,
            min = 0.0,
            hint = "您期望的购买价格",
            unitOptions = listOf("元", "美元", "欧元"),
            isCustomizable = true
        ),
        
        "个人预算" to FieldProperties(
            validationType = ValidationType.NUMBER,
            min = 0.0,
            hint = "个人预算上限",
            unitOptions = listOf("元", "美元", "欧元"),
            isCustomizable = true
        ),
        
        "价格跟踪" to FieldProperties(
            displayStyle = DisplayStyle.DEFAULT,
            defaultValue = "true",
            hint = "开启后将跟踪价格变化并提醒",
            options = listOf("true", "false")
        ),
        
        // === 购买计划字段 ===
        "优先级" to FieldProperties(
            options = listOf("低", "普通", "高", "紧急"),
            defaultValue = "普通",
            isCustomizable = false
        ),
        
        "紧急程度" to FieldProperties(
            options = listOf("不急", "普通", "急需", "非常急需"),
            defaultValue = "不急",
            isCustomizable = false
        ),
        
        "数量" to FieldProperties(
            validationType = ValidationType.NUMBER,
            min = 1.0,
            defaultValue = "1",
            hint = "想要购买的数量",
            unitOptions = listOf("个", "件", "包", "盒", "瓶", "袋", "套"),
            isCustomizable = true
        ),
        
        "购买计划" to FieldProperties(
            options = listOf("随时", "打折时", "发工资后", "特定日期", "有活动时", "价格达到目标时"),
            defaultValue = "随时",
            isCustomizable = true,
            hint = "购买时机和计划"
        ),
        
        // === 购买偏好字段 ===
        "首选渠道" to FieldProperties(
            options = listOf("淘宝", "京东", "天猫", "拼多多", "实体店", "专卖店", "海外购"),
            isCustomizable = true,
            hint = "首选购买渠道"
        ),
        
        
        "备注" to FieldProperties(
            validationType = ValidationType.TEXT,
            isMultiline = true,
            maxLines = 4,
            hint = "购买原因、特殊要求等备注信息"
        ),
        
        // === 时间管理字段 ===
        "添加日期" to FieldProperties(
            validationType = ValidationType.DATE,
            defaultDate = true,
            hint = "添加到心愿单的日期"
        ),
        
        )
        
        android.util.Log.d("WishlistFieldManager", "📊 心愿单字段属性总数: ${properties.size}")
        
        // 打印关键字段的属性配置
        val keyFields = listOf("优先级", "紧急程度", "添加日期", "购买计划", "价格跟踪")
        keyFields.forEach { fieldName ->
            val prop = properties[fieldName]
            android.util.Log.d("WishlistFieldManager", "🔍 关键字段 '$fieldName' 属性:")
            android.util.Log.d("WishlistFieldManager", "   ValidationType: ${prop?.validationType}")
            android.util.Log.d("WishlistFieldManager", "   DisplayStyle: ${prop?.displayStyle}")
            android.util.Log.d("WishlistFieldManager", "   Options: ${prop?.options}")
            android.util.Log.d("WishlistFieldManager", "   DefaultValue: ${prop?.defaultValue}")
        }
        
        android.util.Log.d("WishlistFieldManager", "✅ 心愿单字段属性获取完成")
        return properties
    }
    
    /**
     * 获取心愿单字段的验证规则
     * 确保数据的完整性和合理性
     */
    fun validateWishlistField(fieldName: String, value: Any?): Pair<Boolean, String?> {
        return when (fieldName) {
            "名称" -> {
                val name = value as? String
                if (name.isNullOrBlank()) {
                    Pair(false, "物品名称不能为空")
                } else if (name.length > 100) {
                    Pair(false, "物品名称不能超过100个字符")
                } else {
                    Pair(true, null)
                }
            }
            
            "单价", "目标价格", "个人预算" -> {
                val price = value as? Double
                if (price != null && price < 0) {
                    Pair(false, "${fieldName}不能为负数")
                } else if (price != null && price > 1000000) {
                    Pair(false, "${fieldName}不能超过100万")
                } else {
                    Pair(true, null)
                }
            }
            
            "数量" -> {
                val quantity = value as? Double
                if (quantity != null && quantity <= 0) {
                    Pair(false, "数量必须大于0")
                } else if (quantity != null && quantity > 9999) {
                    Pair(false, "数量不能超过9999")
                } else {
                    Pair(true, null)
                }
            }
            
            else -> Pair(true, null)
        }
    }
    
    /**
     * 检查必填字段是否完整
     */
    fun validateRequiredFields(fieldValues: Map<String, Any?>): List<String> {
        val requiredFields = listOf("名称")
        val missingFields = mutableListOf<String>()
        
        requiredFields.forEach { fieldName ->
            val value = fieldValues[fieldName]
            val (isValid, _) = validateWishlistField(fieldName, value)
            if (!isValid) {
                missingFields.add(fieldName)
            }
        }
        
        return missingFields
    }
}
