package com.example.itemmanagement.test

import com.example.itemmanagement.data.entity.wishlist.WishlistItemEntity
import com.example.itemmanagement.data.entity.wishlist.WishlistPriority
import com.example.itemmanagement.data.entity.wishlist.WishlistUrgency
import com.example.itemmanagement.data.entity.wishlist.WishlistPriceHistoryEntity
import com.example.itemmanagement.data.entity.wishlist.PriceVerificationStatus
import java.util.*

/**
 * 心愿单测试数据生成器
 * 用于生成包含完整字段的心愿单测试数据
 */
object WishlistTestDataGenerator {
    
    /**
     * 生成心愿单测试数据
     */
    fun generateWishlistTestItems(): List<WishlistTestData> {
        val testItems = mutableListOf<WishlistTestData>()
        
        // 1. 数码产品类 - MacBook Pro
        testItems.add(WishlistTestData(
            item = WishlistItemEntity(
                name = "MacBook Pro 16英寸",
                category = "数码产品",
                subCategory = "笔记本电脑",
                brand = "Apple",
                specification = "M3 Max芯片 / 32GB内存 / 1TB SSD",
                customNote = "专业级笔记本，用于视频编辑和软件开发",
                price = 25999.0,
                targetPrice = 23000.0,
                priceUnit = "元",
                currentPrice = 24999.0,
                lowestPrice = 23500.0,
                highestPrice = 26999.0,
                priority = WishlistPriority.HIGH,
                urgency = WishlistUrgency.NORMAL,
                quantity = 1.0,
                quantityUnit = "台",
                budgetLimit = 25000.0,
                purchaseChannel = "Apple官网",
                preferredBrand = "Apple",
                isPriceTrackingEnabled = true,
                priceDropThreshold = 5.0,
                lastPriceCheck = getDateDaysAgo(1),
                priceCheckInterval = 3,
                isActive = true,
                isPaused = false,
                addDate = getDateDaysAgo(15),
                lastModified = getDateDaysAgo(1),
                achievedDate = null,
                sourceUrl = "https://www.apple.com.cn/macbook-pro/",
                imageUrl = "https://picsum.photos/400/300?random=macbook",
                relatedItemId = null,
                addedReason = "工作需要，现有电脑性能不足",
                viewCount = 25,
                lastViewDate = getDateDaysAgo(1),
                priceChangeCount = 3
            ),
            priceHistory = listOf(
                WishlistPriceHistoryEntity(
                    wishlistItemId = 0,
                    price = 26999.0,
                    priceUnit = "元",
                    currency = "CNY",
                    source = "Apple官网",
                    sourceUrl = "https://www.apple.com.cn/macbook-pro/",
                    storeName = "Apple中国官网",
                    platform = "官网",
                    recordDate = getDateDaysAgo(15),
                    isManual = false,
                    isPromotional = false,
                    previousPrice = null,
                    priceChange = null,
                    changePercentage = null,
                    confidence = 1.0,
                    verificationStatus = PriceVerificationStatus.VERIFIED
                ),
                WishlistPriceHistoryEntity(
                    wishlistItemId = 0,
                    price = 24999.0,
                    priceUnit = "元",
                    currency = "CNY",
                    source = "京东商城",
                    sourceUrl = "https://item.jd.com/macbook-pro",
                    storeName = "京东自营",
                    platform = "京东",
                    recordDate = getDateDaysAgo(1),
                    isManual = false,
                    isPromotional = true,
                    promotionalInfo = "618大促活动",
                    previousPrice = 26999.0,
                    priceChange = -2000.0,
                    changePercentage = -7.4,
                    confidence = 0.95,
                    verificationStatus = PriceVerificationStatus.VERIFIED
                )
            )
        ))
        
        // 2. 家居用品类 - 空气净化器
        testItems.add(WishlistTestData(
            item = WishlistItemEntity(
                name = "小米空气净化器Pro",
                category = "家居用品",
                subCategory = "家电",
                brand = "小米",
                specification = "HEPA滤网 / 智能控制 / 静音运行",
                customNote = "新房装修后需要净化甲醛，选择性价比高的产品",
                price = 1299.0,
                targetPrice = 1000.0,
                priceUnit = "元",
                currentPrice = 1199.0,
                lowestPrice = 999.0,
                highestPrice = 1399.0,
                priority = WishlistPriority.URGENT,
                urgency = WishlistUrgency.URGENT,
                quantity = 2.0,
                quantityUnit = "台",
                budgetLimit = 2500.0,
                purchaseChannel = "小米商城",
                preferredBrand = "小米",
                isPriceTrackingEnabled = true,
                priceDropThreshold = 8.0,
                lastPriceCheck = getDateDaysAgo(0),
                priceCheckInterval = 1,
                isActive = true,
                isPaused = false,
                addDate = getDateDaysAgo(7),
                lastModified = getDateDaysAgo(0),
                achievedDate = null,
                sourceUrl = "https://www.mi.com/air-purifier-pro",
                imageUrl = "https://picsum.photos/400/300?random=airpurifier",
                relatedItemId = null,
                addedReason = "新房装修，急需空气净化",
                viewCount = 45,
                lastViewDate = getDateDaysAgo(0),
                priceChangeCount = 5
            ),
            priceHistory = listOf(
                WishlistPriceHistoryEntity(
                    wishlistItemId = 0,
                    price = 1299.0,
                    priceUnit = "元",
                    currency = "CNY",
                    source = "小米商城",
                    sourceUrl = "https://www.mi.com/air-purifier-pro",
                    storeName = "小米官方商城",
                    platform = "小米商城",
                    recordDate = getDateDaysAgo(7),
                    isManual = false,
                    isPromotional = false,
                    previousPrice = null,
                    priceChange = null,
                    changePercentage = null,
                    confidence = 1.0,
                    verificationStatus = PriceVerificationStatus.VERIFIED
                )
            )
        ))
        
        // 3. 化妆品类 - 护肤套装
        testItems.add(WishlistTestData(
            item = WishlistItemEntity(
                name = "雅诗兰黛小棕瓶套装",
                category = "化妆品",
                subCategory = "护肤品",
                brand = "雅诗兰黛",
                specification = "精华50ml + 眼霜15ml + 面霜30ml",
                customNote = "抗衰老套装，特别适合熬夜后的肌肤修复",
                price = 1680.0,
                targetPrice = 1400.0,
                priceUnit = "元",
                currentPrice = 1580.0,
                lowestPrice = 1350.0,
                highestPrice = 1780.0,
                priority = WishlistPriority.NORMAL,
                urgency = WishlistUrgency.NORMAL,
                quantity = 1.0,
                quantityUnit = "套",
                budgetLimit = 1600.0,
                purchaseChannel = "天猫旗舰店",
                preferredBrand = "雅诗兰黛",
                isPriceTrackingEnabled = true,
                priceDropThreshold = 10.0,
                lastPriceCheck = getDateDaysAgo(2),
                priceCheckInterval = 7,
                isActive = true,
                isPaused = false,
                addDate = getDateDaysAgo(20),
                lastModified = getDateDaysAgo(2),
                achievedDate = null,
                sourceUrl = "https://esteelauder.tmall.com/",
                imageUrl = "https://picsum.photos/400/300?random=skincare",
                relatedItemId = null,
                addedReason = "护肤品快用完了，想尝试高端产品",
                viewCount = 18,
                lastViewDate = getDateDaysAgo(2),
                priceChangeCount = 2
            ),
            priceHistory = listOf()
        ))
        
        // 4. 运动健身类 - 健身器材
        testItems.add(WishlistTestData(
            item = WishlistItemEntity(
                name = "哑铃可调节套装",
                category = "运动健身",
                subCategory = "健身器材",
                brand = "Keep",
                specification = "5-40kg可调节 / 快速锁定 / 防滑把手",
                customNote = "在家健身必备，占地面积小，重量可调节",
                price = 899.0,
                targetPrice = 700.0,
                priceUnit = "元",
                currentPrice = 799.0,
                lowestPrice = 699.0,
                highestPrice = 999.0,
                priority = WishlistPriority.LOW,
                urgency = WishlistUrgency.NOT_URGENT,
                quantity = 1.0,
                quantityUnit = "套",
                budgetLimit = 800.0,
                purchaseChannel = "Keep官方商城",
                preferredBrand = "Keep",
                isPriceTrackingEnabled = true,
                priceDropThreshold = 12.0,
                lastPriceCheck = getDateDaysAgo(5),
                priceCheckInterval = 14,
                isActive = true,
                isPaused = false,
                addDate = getDateDaysAgo(30),
                lastModified = getDateDaysAgo(5),
                achievedDate = null,
                sourceUrl = "https://store.gotokeep.com/dumbbells",
                imageUrl = "https://picsum.photos/400/300?random=dumbbells",
                relatedItemId = null,
                addedReason = "办了健身卡但经常没时间去，想在家练习",
                viewCount = 8,
                lastViewDate = getDateDaysAgo(5),
                priceChangeCount = 1
            ),
            priceHistory = listOf()
        ))
        
        // 5. 图书教育类 - 编程书籍
        testItems.add(WishlistTestData(
            item = WishlistItemEntity(
                name = "Kotlin实战第二版",
                category = "图书教育",
                subCategory = "技术书籍",
                brand = "人民邮电出版社",
                specification = "精装版 / 中文译本 / 600页",
                customNote = "提升Android开发技能，学习最新的Kotlin特性",
                price = 119.0,
                targetPrice = 80.0,
                priceUnit = "元",
                currentPrice = 89.0,
                lowestPrice = 75.0,
                highestPrice = 139.0,
                priority = WishlistPriority.NORMAL,
                urgency = WishlistUrgency.NORMAL,
                quantity = 1.0,
                quantityUnit = "本",
                budgetLimit = 100.0,
                purchaseChannel = "当当网",
                preferredBrand = "人民邮电出版社",
                isPriceTrackingEnabled = true,
                priceDropThreshold = 15.0,
                lastPriceCheck = getDateDaysAgo(3),
                priceCheckInterval = 10,
                isActive = true,
                isPaused = false,
                addDate = getDateDaysAgo(12),
                lastModified = getDateDaysAgo(3),
                achievedDate = null,
                sourceUrl = "https://product.dangdang.com/kotlin-guide",
                imageUrl = "https://picsum.photos/400/300?random=book",
                relatedItemId = null,
                addedReason = "技能提升需要，项目中开始使用Kotlin",
                viewCount = 12,
                lastViewDate = getDateDaysAgo(3),
                priceChangeCount = 2
            ),
            priceHistory = listOf()
        ))
        
        // 6. 厨房用品类 - 咖啡机
        testItems.add(WishlistTestData(
            item = WishlistItemEntity(
                name = "德龙全自动咖啡机",
                category = "厨房用品",
                subCategory = "小家电",
                brand = "德龙",
                specification = "意式浓缩 / 自动清洁 / 15bar压力",
                customNote = "提升生活品质，每天早上喝现磨咖啡",
                price = 3299.0,
                targetPrice = 2800.0,
                priceUnit = "元",
                currentPrice = 3099.0,
                lowestPrice = 2699.0,
                highestPrice = 3599.0,
                priority = WishlistPriority.HIGH,
                urgency = WishlistUrgency.NORMAL,
                quantity = 1.0,
                quantityUnit = "台",
                budgetLimit = 3500.0,
                purchaseChannel = "德龙官方旗舰店",
                preferredBrand = "德龙",
                isPriceTrackingEnabled = true,
                priceDropThreshold = 6.0,
                lastPriceCheck = getDateDaysAgo(1),
                priceCheckInterval = 5,
                isActive = true,
                isPaused = false,
                addDate = getDateDaysAgo(25),
                lastModified = getDateDaysAgo(1),
                achievedDate = null,
                sourceUrl = "https://delonghi.tmall.com/coffee-machine",
                imageUrl = "https://picsum.photos/400/300?random=coffee",
                relatedItemId = null,
                addedReason = "咖啡爱好者，想要专业级咖啡体验",
                viewCount = 32,
                lastViewDate = getDateDaysAgo(1),
                priceChangeCount = 4
            ),
            priceHistory = listOf()
        ))
        
        return testItems
    }
    
    /**
     * 获取几天前的日期
     */
    private fun getDateDaysAgo(days: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -days)
        return calendar.time
    }
    
    /**
     * 获取几天后的日期
     */
    private fun getDateDaysFromNow(days: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, days)
        return calendar.time
    }
}

/**
 * 心愿单测试数据包装类
 */
data class WishlistTestData(
    val item: WishlistItemEntity,
    val priceHistory: List<WishlistPriceHistoryEntity>
)
