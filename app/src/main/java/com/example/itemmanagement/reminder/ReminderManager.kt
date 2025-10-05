package com.example.itemmanagement.reminder

import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.data.repository.ReminderSettingsRepository
import com.example.itemmanagement.data.repository.WarrantyRepository
import com.example.itemmanagement.data.repository.BorrowRepository
import com.example.itemmanagement.data.entity.CustomRuleEntity
import com.example.itemmanagement.data.relation.ItemWithDetails
import com.example.itemmanagement.reminder.model.*
import kotlinx.coroutines.flow.first
import java.util.*
import java.util.concurrent.TimeUnit

class ReminderManager(
    private val itemRepository: UnifiedItemRepository,
    private val settingsRepository: ReminderSettingsRepository,
    private val warrantyRepository: WarrantyRepository? = null,  // 可选参数，保持向后兼容
    private val borrowRepository: BorrowRepository? = null       // 可选参数，保持向后兼容
) {
    
    /**
     * 获取所有需要提醒的物品汇总
     */
    suspend fun getAllReminders(): ReminderSummary {
        val settings = settingsRepository.getSettings()
        val items = itemRepository.getAllItemsWithDetails().first()
        
        return ReminderSummary(
            expiredItems = getExpiredItems(items),
            expiringItems = getExpiringItems(items, settings.expirationAdvanceDays, settings.includeWarranty),
            lowStockItems = if (settings.stockReminderEnabled) getLowStockItems(items) else emptyList(),
            customRuleMatches = getCustomRuleMatches(items),
            warrantyExpiringItems = if (warrantyRepository != null && settings.includeWarranty) {
                getWarrantyExpiringItems(settings.expirationAdvanceDays)
            } else emptyList(),
            borrowExpiringItems = if (borrowRepository != null) {
                getBorrowExpiringItems(settings.expirationAdvanceDays)
            } else emptyList()
        )
    }
    
    /**
     * 获取已过期的物品
     */
    private fun getExpiredItems(items: List<ItemWithDetails>): List<ItemWithDetails> {
        val now = Date()
        return items.filter { item ->
            val expiredByDate = item.inventoryDetail?.expirationDate?.let { it.before(now) } ?: false
            val expiredByWarranty = item.inventoryDetail?.warrantyEndDate?.let { it.before(now) } ?: false
            expiredByDate || expiredByWarranty
        }
    }
    
    /**
     * 获取即将到期的物品
     */
    private fun getExpiringItems(
        items: List<ItemWithDetails>, 
        advanceDays: Int, 
        includeWarranty: Boolean
    ): List<ItemWithDetails> {
        val now = Date()
        val futureDate = Calendar.getInstance().apply {
            time = now
            add(Calendar.DAY_OF_YEAR, advanceDays)
        }.time
        
        return items.filter { item ->
            val expiringByDate = item.inventoryDetail?.expirationDate?.let { 
                it.after(now) && it.before(futureDate) 
            } ?: false
            
            val expiringByWarranty = if (includeWarranty) {
                item.inventoryDetail?.warrantyEndDate?.let { 
                    it.after(now) && it.before(futureDate) 
                } ?: false
            } else false
            
            expiringByDate || expiringByWarranty
        }
    }
    
    /**
     * 获取库存不足的物品
     */
    private suspend fun getLowStockItems(items: List<ItemWithDetails>): List<LowStockItem> {
        val thresholds = settingsRepository.getEnabledThresholdsMap()
        
        return items.mapNotNull { item ->
            val threshold = thresholds[item.unifiedItem.category]
            val quantity = item.inventoryDetail?.quantity ?: 0.0
            if (threshold != null && quantity < threshold.minQuantity) {
                LowStockItem(
                    item = item,
                    currentQuantity = quantity,
                    requiredQuantity = threshold.minQuantity.toDouble()
                )
            } else null
        }
    }
    
    /**
     * 匹配自定义规则
     */
    private suspend fun getCustomRuleMatches(items: List<ItemWithDetails>): List<CustomRuleMatch> {
        val activeRules = settingsRepository.getActiveRules()
        val matches = mutableListOf<CustomRuleMatch>()
        
        activeRules.forEach { rule ->
            val matchingItems = items.filter { item ->
                isItemMatchingRule(item, rule)
            }
            
            matchingItems.forEach { item ->
                if (isRuleConditionMet(item, rule)) {
                    matches.add(
                        CustomRuleMatch(
                            item = item,
                            rule = rule,
                            matchReason = generateMatchReason(item, rule),
                            calculatedValue = calculateRuleValue(item, rule)
                        )
                    )
                }
            }
        }
        
        return matches
    }
    
    /**
     * 检查物品是否匹配规则的目标范围
     */
    private fun isItemMatchingRule(item: ItemWithDetails, rule: CustomRuleEntity): Boolean {
        // 检查分类匹配
        if (rule.targetCategory != null && item.item.category != rule.targetCategory) {
            return false
        }
        
        // 检查物品名称匹配（模糊匹配）
        if (rule.targetItemName != null && 
            !item.item.name.contains(rule.targetItemName, ignoreCase = true)) {
            return false
        }
        
        return true
    }
    
    /**
     * 检查规则条件是否满足
     */
    private fun isRuleConditionMet(item: ItemWithDetails, rule: CustomRuleEntity): Boolean {
        val now = Date()
        
        return when (rule.ruleType) {
            "EXPIRATION" -> {
                rule.advanceDays?.let { days ->
                    val futureDate = Calendar.getInstance().apply {
                        time = now
                        add(Calendar.DAY_OF_YEAR, days)
                    }.time
                    
                    item.inventoryDetail?.expirationDate?.let { 
                        it.after(now) && it.before(futureDate) 
                    } ?: false
                } ?: false
            }
            
            "WARRANTY" -> {
                rule.advanceDays?.let { days ->
                    val futureDate = Calendar.getInstance().apply {
                        time = now
                        add(Calendar.DAY_OF_YEAR, days)
                    }.time
                    
                    item.inventoryDetail?.warrantyEndDate?.let { 
                        it.after(now) && it.before(futureDate) 
                    } ?: false
                } ?: false
            }
            
            "STOCK" -> {
                rule.minQuantity?.let { minQty ->
                    val quantity = item.inventoryDetail?.quantity ?: 0.0
                    quantity < minQty
                } ?: false
            }
            
            else -> false
        }
    }
    
    /**
     * 生成匹配原因说明
     */
    private fun generateMatchReason(item: ItemWithDetails, rule: CustomRuleEntity): String {
        return when (rule.ruleType) {
            "EXPIRATION" -> "将在${rule.advanceDays}天内到期"
            "WARRANTY" -> "保修期将在${rule.advanceDays}天内到期"
            "STOCK" -> "库存${item.inventoryDetail?.quantity ?: 0.0}低于阈值${rule.minQuantity}"
            else -> "满足自定义条件"
        }
    }
    
    /**
     * 计算规则相关的数值
     */
    private fun calculateRuleValue(item: ItemWithDetails, rule: CustomRuleEntity): Double? {
        return when (rule.ruleType) {
            "STOCK" -> item.inventoryDetail?.quantity
            "EXPIRATION" -> {
                item.inventoryDetail?.expirationDate?.let { expDate ->
                    val diffInMillis = expDate.time - System.currentTimeMillis()
                    (diffInMillis / TimeUnit.DAYS.toMillis(1)).toDouble()
                }
            }
            "WARRANTY" -> {
                item.inventoryDetail?.warrantyEndDate?.let { warrantyDate ->
                    val diffInMillis = warrantyDate.time - System.currentTimeMillis()
                    (diffInMillis / TimeUnit.DAYS.toMillis(1)).toDouble()
                }
            }
            else -> null
        }
    }
    
    /**
     * 将汇总数据转换为提醒项目列表（用于通知）
     */
    suspend fun convertToReminderItems(summary: ReminderSummary): List<ReminderItem> {
        val items = mutableListOf<ReminderItem>()
        
        // 已过期物品
        summary.expiredItems.forEach { item ->
            items.add(
                ReminderItem(
                    title = "物品已过期",
                    message = "${item.item.name} 已过期，请检查处理",
                    priority = ReminderPriority.URGENT,
                    type = ReminderType.EXPIRED,
                    itemId = item.item.id,
                    daysUntilEvent = calculateDaysUntilExpiration(item)
                )
            )
        }
        
        // 即将到期物品
        summary.expiringItems.forEach { item ->
            val days = calculateDaysUntilExpiration(item)
            items.add(
                ReminderItem(
                    title = "物品即将到期",
                    message = "${item.item.name} ${if (days != null) "将在${days}天后到期" else "即将到期"}",
                    priority = if (days != null && days <= 1) ReminderPriority.URGENT else ReminderPriority.IMPORTANT,
                    type = ReminderType.EXPIRING,
                    itemId = item.item.id,
                    daysUntilEvent = days
                )
            )
        }
        
        // 库存不足物品
        summary.lowStockItems.forEach { lowStockItem ->
            items.add(
                ReminderItem(
                    title = if (lowStockItem.isUrgent()) "物品已缺货" else "库存不足",
                    message = "${lowStockItem.item.item.name} 当前库存${lowStockItem.currentQuantity}，建议补充至${lowStockItem.requiredQuantity}",
                    priority = if (lowStockItem.isUrgent()) ReminderPriority.URGENT else ReminderPriority.IMPORTANT,
                    type = if (lowStockItem.isUrgent()) ReminderType.OUT_OF_STOCK else ReminderType.LOW_STOCK,
                    itemId = lowStockItem.item.item.id,
                    relatedQuantity = lowStockItem.shortfallQuantity
                )
            )
        }
        
        // 自定义规则匹配
        summary.customRuleMatches.forEach { match ->
            items.add(
                ReminderItem(
                    title = match.rule.name,
                    message = match.getMessage(),
                    priority = match.getPriority(),
                    type = ReminderType.CUSTOM_RULE,
                    itemId = match.item.item.id,
                    ruleId = match.rule.id
                )
            )
        }
        
        return items.sortedWith(compareBy<ReminderItem> { 
            it.priority.ordinal 
        }.thenBy { 
            it.daysUntilEvent ?: Int.MAX_VALUE 
        })
    }
    
    /**
     * 计算距离过期的天数
     */
    private fun calculateDaysUntilExpiration(item: ItemWithDetails): Int? {
        val now = Date()
        val expirationDate = item.inventoryDetail?.expirationDate
        val warrantyDate = item.inventoryDetail?.warrantyEndDate
        
        // 选择最早的到期日期
        val earliestDate = when {
            expirationDate != null && warrantyDate != null -> {
                if (expirationDate.before(warrantyDate)) expirationDate else warrantyDate
            }
            expirationDate != null -> expirationDate
            warrantyDate != null -> warrantyDate
            else -> return null
        }
        
        val diffInMillis = earliestDate.time - now.time
        return (diffInMillis / TimeUnit.DAYS.toMillis(1)).toInt()
    }
    
    /**
     * 更新规则的最后触发时间
     */
    suspend fun markRuleAsTriggered(ruleId: Long) {
        settingsRepository.updateRuleLastTriggered(ruleId)
    }
    
    /**
     * 获取即将到期的保修记录
     */
    private suspend fun getWarrantyExpiringItems(advanceDays: Int): List<WarrantyExpiringItem> {
        return warrantyRepository?.let { repository ->
            try {
                val warrantiesWithItems = repository.getWarrantiesNearingExpirationWithItems(advanceDays)
                val now = Calendar.getInstance()
                
                warrantiesWithItems.mapNotNull { warrantyWithItem ->
                    try {
                        // 计算距离到期的天数 - warrantyEndDate是Long时间戳
                        val warrantyEndDate = Date(warrantyWithItem.warrantyEndDate)
                        val warrantyEndCalendar = Calendar.getInstance().apply {
                            time = warrantyEndDate
                        }
                        val diffInMillis = warrantyEndCalendar.timeInMillis - now.timeInMillis
                        val daysUntilExpiration = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
                        
                        WarrantyExpiringItem(
                            warrantyId = warrantyWithItem.id,
                            itemId = warrantyWithItem.itemId,
                            itemName = warrantyWithItem.itemName,
                            itemCategory = warrantyWithItem.category,
                            itemBrand = warrantyWithItem.brand ?: "",
                            warrantyEndDate = warrantyEndDate,
                            warrantyProvider = warrantyWithItem.warrantyProvider,
                            contactInfo = warrantyWithItem.contactInfo,
                            daysUntilExpiration = daysUntilExpiration
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }
    
    /**
     * 获取即将到期的借还记录
     * @param advanceDays 提前天数
     */
    private suspend fun getBorrowExpiringItems(advanceDays: Int): List<BorrowExpiringItem> {
        return borrowRepository?.let { repository ->
            try {
                val borrowsWithItems = repository.getSoonExpireRecordsWithItemInfo(advanceDays)
                val now = Calendar.getInstance()
                
                borrowsWithItems.mapNotNull { borrowWithItem ->
                    try {
                        // 计算距离归还日期的天数 - expectedReturnDate是Long时间戳
                        val expectedReturnDate = Date(borrowWithItem.expectedReturnDate)
                        val returnCalendar = Calendar.getInstance().apply {
                            time = expectedReturnDate
                        }
                        val diffInMillis = returnCalendar.timeInMillis - now.timeInMillis
                        val daysUntilReturn = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
                        
                        BorrowExpiringItem(
                            borrowId = borrowWithItem.id,
                            itemId = borrowWithItem.itemId,
                            itemName = borrowWithItem.itemName,
                            itemCategory = borrowWithItem.category,
                            itemBrand = borrowWithItem.brand,
                            borrowerName = borrowWithItem.borrowerName,
                            borrowerContact = borrowWithItem.borrowerContact,
                            expectedReturnDate = expectedReturnDate,
                            daysUntilReturn = daysUntilReturn
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }
    
    companion object {
        @Volatile
        private var INSTANCE: ReminderManager? = null
        
        fun getInstance(
            itemRepository: UnifiedItemRepository,
            settingsRepository: ReminderSettingsRepository,
            warrantyRepository: WarrantyRepository? = null,  // 新增参数，可选
            borrowRepository: BorrowRepository? = null        // 新增参数，可选
        ): ReminderManager {
            return INSTANCE ?: synchronized(this) {
                val instance = ReminderManager(itemRepository, settingsRepository, warrantyRepository, borrowRepository)
                INSTANCE = instance
                instance
            }
        }
    }
}
