package com.example.itemmanagement.export

import com.example.itemmanagement.data.dao.BorrowWithItemInfo
import com.example.itemmanagement.data.dao.WarrantyWithItemInfo
import com.example.itemmanagement.data.relation.ItemWithDetails
import java.text.SimpleDateFormat
import java.util.*

/**
 * CSV数据导出器
 * 负责将各种数据格式化为CSV格式
 */
object CSVExporter {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    /**
     * 导出物品数据到CSV
     */
    fun exportItems(items: List<ItemWithDetails>): String {
        if (items.isEmpty()) return "没有物品数据可导出"
        
        val csv = StringBuilder()
        
        // CSV表头
        csv.appendLine("物品名称,分类,品牌,型号,数量,单位,单价,总价值,购买日期,过期日期,位置区域,位置详情,保修到期,备注,标签,添加日期")
        
        // 数据行
        items.forEach { itemDetail ->
            val unifiedItem = itemDetail.unifiedItem
            val inventoryDetail = itemDetail.inventoryDetail
            val location = null // TODO: 需要从inventoryDetail.locationId查询LocationEntity
            val tags = itemDetail.tags.joinToString(";") { it.name }
            
            csv.appendLine(
                "${escapeCsvField(unifiedItem.name)}," +
                "${escapeCsvField(unifiedItem.category)}," +
                "${escapeCsvField(unifiedItem.brand ?: "")}," +
                "${escapeCsvField(unifiedItem.specification ?: "")}," +
                "${inventoryDetail?.quantity ?: ""}," +
                "${escapeCsvField(inventoryDetail?.unit ?: "")}," +
                "${inventoryDetail?.price ?: ""}," +
                "${calculateTotalValue(inventoryDetail?.quantity, inventoryDetail?.price)}," +
                "${formatDate(inventoryDetail?.purchaseDate)}," +
                "${formatDate(inventoryDetail?.expirationDate)}," +
                "${escapeCsvField("")}," + // TODO: 需要从inventoryDetail.locationId查询LocationEntity
                "${escapeCsvField("")}," + // TODO: 需要从LocationEntity获取container
                // 保修信息已移至 WarrantyEntity，通过单独的保修CSV导出
                "${""}," +
                "${escapeCsvField(unifiedItem.customNote ?: "")}," +
                "${escapeCsvField(tags)}," +
                "${formatDateTime(unifiedItem.createdDate)}"
            )
        }
        
        return csv.toString()
    }
    
    /**
     * 导出保修记录到CSV（基础版本）
     */
    fun exportBasicWarranties(warranties: List<com.example.itemmanagement.data.entity.WarrantyEntity>): String {
        if (warranties.isEmpty()) return "没有保修记录可导出"
        
        val csv = StringBuilder()
        
        // CSV表头
        csv.appendLine("物品ID,购买日期,保修期(月),保修到期日,保修商,联系方式,状态,备注,创建日期")
        
        // 数据行
        warranties.forEach { warranty ->
            csv.appendLine(
                "${warranty.itemId}," +
                "${formatDate(warranty.purchaseDate)}," +
                "${warranty.warrantyPeriodMonths}," +
                "${formatDate(warranty.warrantyEndDate)}," +
                "${escapeCsvField(warranty.warrantyProvider ?: "")}," +
                "${escapeCsvField(warranty.contactInfo ?: "")}," +
                "${escapeCsvField(warranty.status.name)}," +
                "${escapeCsvField(warranty.notes ?: "")}," +
                "${formatDateTime(warranty.createdDate)}"
            )
        }
        
        return csv.toString()
    }
    
    /**
     * 导出保修记录到CSV（兼容旧版本）
     */
    fun exportWarranties(warranties: List<com.example.itemmanagement.data.entity.WarrantyEntity>): String {
        return exportBasicWarranties(warranties)
    }
    
    /**
     * 导出借还记录到CSV
     */
    fun exportBorrows(borrows: List<BorrowWithItemInfo>): String {
        if (borrows.isEmpty()) return "没有借还记录可导出"
        
        val csv = StringBuilder()
        
        // CSV表头
        csv.appendLine("物品名称,分类,品牌,借用人,联系方式,借出日期,预计归还日期,实际归还日期,状态,备注,创建日期")
        
        // 数据行
        borrows.forEach { borrow ->
            val statusText = when(borrow.status.name) {
                "BORROWED" -> "已借出"
                "RETURNED" -> "已归还"
                "OVERDUE" -> "已逾期"
                else -> borrow.status.name
            }
            
            csv.appendLine(
                "${escapeCsvField(borrow.itemName)}," +
                "${escapeCsvField(borrow.category)}," +
                "${escapeCsvField(borrow.brand ?: "")}," +
                "${escapeCsvField(borrow.borrowerName)}," +
                "${escapeCsvField(borrow.borrowerContact ?: "")}," +
                "${formatDate(Date(borrow.borrowDate))}," +
                "${formatDate(Date(borrow.expectedReturnDate))}," +
                "${if (borrow.actualReturnDate != null) formatDate(Date(borrow.actualReturnDate)) else ""}," +
                "${escapeCsvField(statusText)}," +
                "${escapeCsvField(borrow.notes ?: "")}," +
                "${formatDateTime(Date(borrow.createdDate))}"
            )
        }
        
        return csv.toString()
    }
    
    /**
     * 导出完整数据摘要
     */
    fun exportSummary(
        itemCount: Int,
        warrantyCount: Int, 
        borrowCount: Int,
        exportDate: Date = Date()
    ): String {
        val csv = StringBuilder()
        
        csv.appendLine("=== 物品管理系统数据导出摘要 ===")
        csv.appendLine("导出时间,${formatDateTime(exportDate)}")
        csv.appendLine("物品总数,${itemCount}")
        csv.appendLine("保修记录数,${warrantyCount}")
        csv.appendLine("借还记录数,${borrowCount}")
        csv.appendLine("")
        csv.appendLine("注意：详细数据请查看对应的CSV文件")
        
        return csv.toString()
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 转义CSV字段（处理逗号、引号、换行符）
     */
    private fun escapeCsvField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"${field.replace("\"", "\"\"")}\"" 
        } else {
            field
        }
    }
    
    /**
     * 格式化日期
     */
    private fun formatDate(date: Date?): String {
        return date?.let { dateFormat.format(it) } ?: ""
    }
    
    /**
     * 格式化日期时间
     */
    private fun formatDateTime(date: Date?): String {
        return date?.let { dateTimeFormat.format(it) } ?: ""
    }
    
    /**
     * 计算总价值
     */
    private fun calculateTotalValue(quantity: Double?, price: Double?): String {
        return if (quantity != null && price != null) {
            String.format("%.2f", quantity * price)
        } else {
            ""
        }
    }
}
