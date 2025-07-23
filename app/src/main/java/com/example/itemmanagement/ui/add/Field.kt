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
            "名称" -> 1
            "数量" -> 2
            "位置" -> 3
            "备注" -> 4
            "分类" -> 5
            "子分类" -> 6
            "标签" -> 7
            "季节" -> 8
            "容量" -> 9
            "评分" -> 10
            "单价" -> 11
            "总价" -> 12
            "添加日期" -> 13
            "开封时间" -> 14
            "购买日期" -> 15
            "生产日期" -> 16
            "保质期" -> 17
            "保质过期时间" -> 18
            "保修期" -> 19
            "保修到期时间" -> 20
            "品牌" -> 21
            "开封状态" -> 22
            "购买渠道" -> 23
            "商家名称" -> 24
            "序列号" -> 25
            else -> Int.MAX_VALUE
        }
    }
}