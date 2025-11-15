package com.example.itemmanagement.ui.add

import java.io.Serializable

data class Field(
    val group: String,
    val name: String,
    var isSelected: Boolean = false,
    val order: Int = getDefaultOrder(name)
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L  // 添加序列化版本ID

        fun getDefaultOrder(name: String): Int = when(name) {
            // === 基础信息 ===
            "名称" -> 1
            "数量" -> 2
            "位置" -> 3
            "地点" -> 4
            "备注" -> 5
            
            // === 分类 ===
            "分类" -> 6
            "子分类" -> 7
            "标签" -> 8
            "季节" -> 9
            
            // === 数字类 ===
            "容量" -> 10
            "规格" -> 11
            "评分" -> 12
            "单价" -> 13
            "总价" -> 14
            
            // === 日期类 ===
            "添加日期" -> 15
            "购买日期" -> 16
            "生产日期" -> 17
            "保质期" -> 18
            "保质过期时间" -> 19
            "保修期" -> 20
            "保修到期时间" -> 21
            
            // === 商业类 ===
            "品牌" -> 22
            "开封状态" -> 23
            "商家名称" -> 24
            "序列号" -> 25
            
            // === 购物专用字段（连续order，便于排序）===
            "预估价格" -> 50      // 价格规划
            "购买渠道" -> 51      // 购买方式
            "购买商店" -> 52      // 购买地点
            "预算上限" -> 53      // 价格控制
            "重要程度" -> 54      // 重要程度（Important）- 事情重要性
            "紧急程度" -> 55      // 紧急程度（Urgent）- 时间紧迫性
            "截止日期" -> 56      // 时间约束
            "购买原因" -> 57      // 购买理由
            
            // === 已废弃字段（保留兼容性）===
            "优先级" -> 54        // 已废弃，使用"重要程度"或"紧急程度"
            "推荐原因" -> 56      // 已废弃，使用"购买原因"
            "实际价格" -> 99      // 已废弃，不在前端显示
            "提醒日期" -> 99      // 已废弃，不在前端显示
            "是否重复购买" -> 99  // 已废弃，不在前端显示
            "重复间隔" -> 99      // 已废弃，不在前端显示
            
            else -> Int.MAX_VALUE
        }
    }
}