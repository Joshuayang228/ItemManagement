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
            "备注" -> 4
            
            // === 分类 ===
            "分类" -> 5
            "子分类" -> 6
            "标签" -> 7
            "季节" -> 8
            
            // === 数字类 ===
            "容量" -> 9
            "评分" -> 10
            "单价" -> 11
            "总价" -> 12
            
            // === 日期类 ===
            "添加日期" -> 13
            "开封时间" -> 14
            "购买日期" -> 15
            "生产日期" -> 16
            "保质期" -> 17
            "保质过期时间" -> 18
            "保修期" -> 19
            "保修到期时间" -> 20
            
            // === 商业类 ===
            "品牌" -> 21
            "开封状态" -> 22
            "购买渠道" -> 23
            "商家名称" -> 24
            "序列号" -> 25
            
            // === 其他 ===
            "加入心愿单" -> 26
            "高周转" -> 27
            "规格" -> 28
            
            // === 购物专用字段 ===
            "预估价格" -> 50      // 价格规划
            "预算上限" -> 51      // 价格控制
            "实际价格" -> 52      // 实际支出（转入库存时使用）
            "购买商店" -> 53      // 购买商店（统一字段，合并了原"首选商店"和"商店名称"）
            "重要程度" -> 54      // 重要程度（Important）- 物品的重要性
            "优先级" -> 54        // 优先级（已废弃，保留兼容性，使用"重要程度"）
            "紧急程度" -> 55      // 紧急程度（Urgent）- 时间紧迫性
            "截止日期" -> 56      // 时间约束
            "提醒日期" -> 57      // 提醒时间
            "推荐原因" -> 58      // 推荐理由
            "是否重复购买" -> 59  // 周期性购买
            "重复间隔" -> 60      // 重复购买间隔
            
            else -> Int.MAX_VALUE
        }
    }
}