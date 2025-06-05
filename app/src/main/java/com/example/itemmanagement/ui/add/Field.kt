package com.example.itemmanagement.ui.add

data class Field(
    val group: String,
    val name: String,
    var isSelected: Boolean = false,
    val order: Int = getDefaultOrder(name)
) {
    companion object {
        fun getDefaultOrder(name: String): Int = when(name) {
            "名称" -> 1
            "数量" -> 2
            "位置" -> 3
            "备注" -> 4
            "分类" -> 5
            "标签" -> 6
            "季节" -> 7
            "容量" -> 8
            "评分" -> 9
            "单价" -> 10
            "总价" -> 11
            "添加日期" -> 12
            "开封时间" -> 13
            "购买日期" -> 14
            "生产日期" -> 15
            "保质期" -> 16
            "保质过期时间" -> 17
            "保修期" -> 18
            "保修到期时间" -> 19
            "品牌" -> 20
            "开封状态" -> 21
            "购买渠道" -> 22
            "商家名称" -> 23
            "序列号" -> 24
            else -> Int.MAX_VALUE
        }
    }
}