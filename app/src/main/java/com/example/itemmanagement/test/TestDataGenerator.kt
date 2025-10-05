package com.example.itemmanagement.test

import com.example.itemmanagement.data.entity.*
import com.example.itemmanagement.data.entity.unified.*
import com.example.itemmanagement.data.model.ItemStatus
import com.example.itemmanagement.data.model.OpenStatus
import java.util.*

/**
 * 测试数据生成器
 * 用于在数据库中生成包含全字段的测试物品
 */
object TestDataGenerator {
    
    /**
     * 生成全字段测试物品数据
     */
    fun generateFullFieldTestItems(): List<TestItemData> {
        val testItems = mutableListOf<TestItemData>()
        
        // 1. 药品类测试物品
        testItems.add(TestItemData(
            unifiedItem = UnifiedItemEntity(
                name = "阿司匹林肠溶片",
                category = "药品",
                subCategory = "处方药",
                brand = "拜耳",
                specification = "100mg*100片",
                customNote = "心血管疾病预防用药，需要医生指导使用",
                capacity = 100.0,
                capacityUnit = "片",
                rating = 4.5,
                season = "全年",
                serialNumber = "BY2024001234",
                createdDate = Date(),
                updatedDate = Date()
            ),
            inventoryDetail = InventoryDetailEntity(
                itemId = 0, // 将在插入时设置
                quantity = 100.0,
                unit = "片",
                locationId = null, // 将在插入时设置
                productionDate = getDateDaysAgo(365), // 1年前生产
                expirationDate = getDateDaysFromNow(730), // 2年后过期
                openStatus = OpenStatus.UNOPENED,
                openDate = null,
                status = ItemStatus.IN_STOCK,
                stockWarningThreshold = 20,
                price = 25.8,
                priceUnit = "元",
                purchaseChannel = "京东药房",
                storeName = "京东自营药房",
                totalPrice = 25.8,
                totalPriceUnit = "元",
                purchaseDate = getDateDaysAgo(30), // 30天前购买
                shelfLife = 1095, // 3年保质期
                warrantyPeriod = null,
                warrantyEndDate = null,
                isHighTurnover = true,
                wasteDate = null
                // 注意：capacity, rating, season, serialNumber 已移至 UnifiedItemEntity
            ),
            location = LocationEntity(
                area = "卧室",
                container = "床头柜",
                sublocation = "第一层抽屉"
            ),
            photos = listOf(
                PhotoEntity(itemId = 0, uri = "https://picsum.photos/400/300?random=medicine1", isMain = true, displayOrder = 1),
                PhotoEntity(itemId = 0, uri = "https://picsum.photos/400/300?random=medicine2", isMain = false, displayOrder = 2)
            ),
            tags = listOf(
                TagEntity(name = "处方药", color = "#FF5722"),
                TagEntity(name = "心血管", color = "#2196F3"),
                TagEntity(name = "重要", color = "#F44336")
            )
        ))
        
        // 2. 食品类测试物品
        testItems.add(TestItemData(
            unifiedItem = UnifiedItemEntity(
                name = "有机纯牛奶",
                category = "食品",
                subCategory = "乳制品",
                brand = "特仑苏",
                specification = "250ml*12盒",
                customNote = "有机认证，营养丰富，适合全家饮用",
                capacity = 250.0,
                capacityUnit = "ml",
                rating = 4.8,
                season = "全年",
                serialNumber = null,
                createdDate = Date(),
                updatedDate = Date()
            ),
            inventoryDetail = InventoryDetailEntity(
                itemId = 0, // 将在插入时设置
                quantity = 12.0,
                unit = "盒",
                locationId = null, // 将在插入时设置
                productionDate = getDateDaysAgo(7), // 7天前生产
                expirationDate = getDateDaysFromNow(23), // 30天保质期
                openStatus = OpenStatus.OPENED,
                openDate = getDateDaysAgo(2), // 2天前开封
                status = ItemStatus.IN_STOCK,
                stockWarningThreshold = 3,
                price = 4.5,
                priceUnit = "元",
                purchaseChannel = "盒马鲜生",
                storeName = "盒马鲜生(万达店)",
                totalPrice = 54.0,
                totalPriceUnit = "元",
                purchaseDate = getDateDaysAgo(5), // 5天前购买
                shelfLife = 30, // 30天保质期
                warrantyPeriod = null,
                warrantyEndDate = null,
                isHighTurnover = true,
                wasteDate = null
                // 注意：capacity, rating, season, serialNumber 已移至 UnifiedItemEntity
            ),
            location = LocationEntity(
                area = "厨房",
                container = "冰箱",
                sublocation = "冷藏室"
            ),
            photos = listOf(
                PhotoEntity(itemId = 0, uri = "https://picsum.photos/400/300?random=milk1", isMain = true, displayOrder = 1)
            ),
            tags = listOf(
                TagEntity(name = "有机", color = "#4CAF50"),
                TagEntity(name = "乳制品", color = "#FFC107"),
                TagEntity(name = "日常", color = "#9C27B0")
            )
        ))
        
        // 3. 电子产品类测试物品
        testItems.add(TestItemData(
            unifiedItem = UnifiedItemEntity(
                name = "iPhone 15 Pro Max",
                category = "电子产品",
                subCategory = "智能手机",
                brand = "Apple",
                specification = "256GB 钛原色",
                customNote = "主力手机，配置顶级，拍照效果极佳",
                capacity = 256.0,
                capacityUnit = "GB",
                rating = 5.0,
                season = "全年",
                serialNumber = "F2LMHV9HQ1MN",
                createdDate = Date(),
                updatedDate = Date()
            ),
            inventoryDetail = InventoryDetailEntity(
                itemId = 0, // 将在插入时设置
                quantity = 1.0,
                unit = "台",
                locationId = null, // 将在插入时设置
                productionDate = getDateDaysAgo(60), // 2个月前生产
                expirationDate = null, // 电子产品无过期日期
                openStatus = OpenStatus.OPENED,
                openDate = getDateDaysAgo(30), // 1个月前开封
                status = ItemStatus.IN_STOCK,
                stockWarningThreshold = null,
                price = 9999.0,
                priceUnit = "元",
                purchaseChannel = "Apple官网",
                storeName = "Apple中国官网",
                totalPrice = 9999.0,
                totalPriceUnit = "元",
                purchaseDate = getDateDaysAgo(30), // 1个月前购买
                shelfLife = null,
                warrantyPeriod = 365, // 1年保修
                warrantyEndDate = getDateDaysFromNow(335), // 还有335天保修
                isHighTurnover = false,
                wasteDate = null
                // 注意：capacity, rating, season, serialNumber 已移至 UnifiedItemEntity
            ),
            location = LocationEntity(
                area = "卧室",
                container = "书桌",
                sublocation = "抽屉"
            ),
            photos = listOf(
                PhotoEntity(itemId = 0, uri = "https://picsum.photos/400/300?random=iphone1", isMain = true, displayOrder = 1),
                PhotoEntity(itemId = 0, uri = "https://picsum.photos/400/300?random=iphone2", isMain = false, displayOrder = 2),
                PhotoEntity(itemId = 0, uri = "https://picsum.photos/400/300?random=iphone3", isMain = false, displayOrder = 3)
            ),
            tags = listOf(
                TagEntity(name = "数码", color = "#607D8B"),
                TagEntity(name = "高价值", color = "#FF9800"),
                TagEntity(name = "日常使用", color = "#795548")
            )
        ))
        
        // 4. 服装类测试物品
        testItems.add(TestItemData(
            unifiedItem = UnifiedItemEntity(
                name = "羊毛大衣",
                category = "服装",
                subCategory = "外套",
                brand = "优衣库",
                specification = "L码 深蓝色",
                customNote = "100%羊毛材质，保暖性好，适合秋冬季节",
                capacity = null,
                capacityUnit = null,
                rating = 4.3,
                season = "秋冬",
                serialNumber = "UQ2024WC001",
                createdDate = Date(),
                updatedDate = Date()
            ),
            inventoryDetail = InventoryDetailEntity(
                itemId = 0, // 将在插入时设置
                quantity = 1.0,
                unit = "件",
                locationId = null, // 将在插入时设置
                productionDate = getDateDaysAgo(90), // 3个月前生产
                expirationDate = null,
                openStatus = null, // 服装不需要开封状态
                openDate = null,
                status = ItemStatus.IN_STOCK,
                stockWarningThreshold = null,
                price = 599.0,
                priceUnit = "元",
                purchaseChannel = "优衣库官网",
                storeName = "优衣库旗舰店",
                totalPrice = 599.0,
                totalPriceUnit = "元",
                purchaseDate = getDateDaysAgo(60), // 2个月前购买
                shelfLife = null,
                warrantyPeriod = 90, // 3个月质保
                warrantyEndDate = getDateDaysFromNow(30), // 还有30天质保
                isHighTurnover = false,
                wasteDate = null
                // 注意：capacity, rating, season, serialNumber 已移至 UnifiedItemEntity
            ),
            location = LocationEntity(
                area = "卧室",
                container = "衣柜",
                sublocation = "挂衣区"
            ),
            photos = listOf(
                PhotoEntity(itemId = 0, uri = "https://picsum.photos/400/300?random=coat1", isMain = true, displayOrder = 1)
            ),
            tags = listOf(
                TagEntity(name = "羊毛", color = "#8BC34A"),
                TagEntity(name = "秋冬", color = "#3F51B5"),
                TagEntity(name = "外套", color = "#E91E63")
            )
        ))
        
        // 5. 化妆品类测试物品
        testItems.add(TestItemData(
            unifiedItem = UnifiedItemEntity(
                name = "兰蔻小黑瓶精华",
                category = "化妆品",
                subCategory = "护肤品",
                brand = "兰蔻",
                specification = "30ml",
                customNote = "抗衰老精华，含二裂酵母发酵产物溶胞物",
                capacity = 30.0,
                capacityUnit = "ml",
                rating = 4.7,
                season = "全年",
                serialNumber = "LC2024030001",
                createdDate = Date(),
                updatedDate = Date()
            ),
            inventoryDetail = InventoryDetailEntity(
                itemId = 0, // 将在插入时设置
                quantity = 1.0,
                unit = "瓶",
                locationId = null, // 将在插入时设置
                productionDate = getDateDaysAgo(180), // 6个月前生产
                expirationDate = getDateDaysFromNow(730), // 2年后过期
                openStatus = OpenStatus.OPENED,
                openDate = getDateDaysAgo(30), // 1个月前开封
                status = ItemStatus.IN_STOCK,
                stockWarningThreshold = null,
                price = 780.0,
                priceUnit = "元",
                purchaseChannel = "天猫旗舰店",
                storeName = "兰蔻官方旗舰店",
                totalPrice = 780.0,
                totalPriceUnit = "元",
                purchaseDate = getDateDaysAgo(45), // 45天前购买
                shelfLife = 1095, // 3年保质期
                warrantyPeriod = null,
                warrantyEndDate = null,
                isHighTurnover = false,
                wasteDate = null
                // 注意：capacity, rating, season, serialNumber 已移至 UnifiedItemEntity
            ),
            location = LocationEntity(
                area = "卧室",
                container = "梳妆台",
                sublocation = "第二层"
            ),
            photos = listOf(
                PhotoEntity(itemId = 0, uri = "https://picsum.photos/400/300?random=cosmetic1", isMain = true, displayOrder = 1)
            ),
            tags = listOf(
                TagEntity(name = "护肤", color = "#E1BEE7"),
                TagEntity(name = "抗衰老", color = "#FFCDD2"),
                TagEntity(name = "高端", color = "#FFE0B2")
            )
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
 * 测试物品数据包装类
 */
data class TestItemData(
    val unifiedItem: UnifiedItemEntity,
    val inventoryDetail: InventoryDetailEntity,
    val location: LocationEntity,
    val photos: List<PhotoEntity>,
    val tags: List<TagEntity>
)
