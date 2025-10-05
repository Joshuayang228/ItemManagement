package com.example.itemmanagement.data.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 统一ID架构迁移
 * 从版本31迁移到版本32，实现统一ID系统
 */
object UnifiedSchemaMigration {
    
    val MIGRATION_31_32 = object : Migration(31, 32) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                database.beginTransaction()
                
                // 第一步：创建新的统一架构表
                createUnifiedTables(database)
                
                // 第二步：迁移现有数据
                migrateExistingData(database)
                
                // 第三步：更新组件表外键引用
                updateComponentReferences(database)
                
                // 第四步：创建优化索引
                createOptimizedIndexes(database)
                
                // 第五步：清理旧表
                cleanupOldTables(database)
                
                database.setTransactionSuccessful()
            } catch (e: Exception) {
                // 迁移失败，记录错误（在实际应用中应该使用日志系统）
                throw RuntimeException("统一架构迁移失败: ${e.message}", e)
            } finally {
                database.endTransaction()
            }
        }
        
        /**
         * 创建新的统一架构表
         */
        private fun createUnifiedTables(database: SupportSQLiteDatabase) {
            // 1. 创建统一物品表
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `unified_items` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `subCategory` TEXT,
                    `brand` TEXT,
                    `specification` TEXT,
                    `customNote` TEXT,
                    `createdDate` INTEGER NOT NULL,
                    `updatedDate` INTEGER NOT NULL
                )
            """)
            
            // 2. 创建物品状态表
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `item_states` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `itemId` INTEGER NOT NULL,
                    `stateType` TEXT NOT NULL,
                    `isActive` INTEGER NOT NULL,
                    `activatedDate` INTEGER NOT NULL,
                    `deactivatedDate` INTEGER,
                    `contextId` INTEGER,
                    `metadata` TEXT,
                    `notes` TEXT,
                    `createdDate` INTEGER NOT NULL,
                    FOREIGN KEY(`itemId`) REFERENCES `unified_items`(`id`) ON DELETE CASCADE
                )
            """)
            
            // 3. 创建心愿单详情表
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `wishlist_details` (
                    `itemId` INTEGER PRIMARY KEY NOT NULL,
                    `price` REAL,
                    `targetPrice` REAL,
                    `priceUnit` TEXT NOT NULL,
                    `currentPrice` REAL,
                    `lowestPrice` REAL,
                    `highestPrice` REAL,
                    `priority` TEXT NOT NULL,
                    `urgency` TEXT NOT NULL,
                    `quantity` REAL NOT NULL,
                    `quantityUnit` TEXT NOT NULL,
                    `budgetLimit` REAL,
                    `purchaseChannel` TEXT,
                    `preferredBrand` TEXT,
                    `isPriceTrackingEnabled` INTEGER NOT NULL,
                    `priceDropThreshold` REAL,
                    `lastPriceCheck` INTEGER,
                    `priceCheckInterval` INTEGER NOT NULL,
                    `isPaused` INTEGER NOT NULL,
                    `achievedDate` INTEGER,
                    `sourceUrl` TEXT,
                    `imageUrl` TEXT,
                    `relatedInventoryItemId` INTEGER,
                    `addedReason` TEXT,
                    `viewCount` INTEGER NOT NULL,
                    `lastViewDate` INTEGER,
                    `priceChangeCount` INTEGER NOT NULL,
                    `createdDate` INTEGER NOT NULL,
                    `lastModified` INTEGER NOT NULL,
                    FOREIGN KEY(`itemId`) REFERENCES `unified_items`(`id`) ON DELETE CASCADE
                )
            """)
            
            // 4. 创建购物详情表
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `shopping_details` (
                    `itemId` INTEGER PRIMARY KEY NOT NULL,
                    `shoppingListId` INTEGER NOT NULL,
                    `quantity` REAL NOT NULL,
                    `quantityUnit` TEXT NOT NULL,
                    `estimatedPrice` REAL,
                    `actualPrice` REAL,
                    `priceUnit` TEXT NOT NULL,
                    `budgetLimit` REAL,
                    `totalPrice` REAL,
                    `totalPriceUnit` TEXT,
                    `purchaseChannel` TEXT,
                    `storeName` TEXT,
                    `preferredStore` TEXT,
                    `purchaseDate` INTEGER,
                    `isPurchased` INTEGER NOT NULL,
                    `priority` TEXT NOT NULL,
                    `urgencyLevel` TEXT NOT NULL,
                    `deadline` INTEGER,
                    `capacity` REAL,
                    `capacityUnit` TEXT,
                    `rating` REAL,
                    `season` TEXT,
                    `serialNumber` TEXT,
                    `sourceItemId` INTEGER,
                    `recommendationReason` TEXT,
                    `addedReason` TEXT NOT NULL,
                    `addDate` INTEGER NOT NULL,
                    `completedDate` INTEGER,
                    `remindDate` INTEGER,
                    `isRecurring` INTEGER NOT NULL,
                    `recurringInterval` INTEGER,
                    `tags` TEXT,
                    FOREIGN KEY(`itemId`) REFERENCES `unified_items`(`id`) ON DELETE CASCADE,
                    FOREIGN KEY(`shoppingListId`) REFERENCES `shopping_lists`(`id`) ON DELETE CASCADE
                )
            """)
            
            // 5. 创建库存详情表
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `inventory_details` (
                    `itemId` INTEGER PRIMARY KEY NOT NULL,
                    `quantity` REAL NOT NULL,
                    `unit` TEXT NOT NULL,
                    `locationId` INTEGER,
                    `productionDate` INTEGER,
                    `expirationDate` INTEGER,
                    `openDate` INTEGER,
                    `purchaseDate` INTEGER,
                    `wasteDate` INTEGER,
                    `openStatus` TEXT,
                    `status` TEXT NOT NULL,
                    `stockWarningThreshold` INTEGER,
                    `isHighTurnover` INTEGER NOT NULL,
                    `price` REAL,
                    `priceUnit` TEXT,
                    `totalPrice` REAL,
                    `totalPriceUnit` TEXT,
                    `purchaseChannel` TEXT,
                    `storeName` TEXT,
                    `capacity` REAL,
                    `capacityUnit` TEXT,
                    `rating` REAL,
                    `season` TEXT,
                    `serialNumber` TEXT,
                    `shelfLife` INTEGER,
                    `warrantyPeriod` INTEGER,
                    `warrantyEndDate` INTEGER,
                    `createdDate` INTEGER NOT NULL,
                    `updatedDate` INTEGER NOT NULL,
                    FOREIGN KEY(`itemId`) REFERENCES `unified_items`(`id`) ON DELETE CASCADE,
                    FOREIGN KEY(`locationId`) REFERENCES `locations`(`id`) ON DELETE SET NULL
                )
            """)
        }
        
        /**
         * 迁移现有数据到新架构
         */
        private fun migrateExistingData(database: SupportSQLiteDatabase) {
            val currentTime = System.currentTimeMillis()
            
            // === 迁移库存物品（items表） ===
            migrateInventoryItems(database, currentTime)
            
            // === 迁移心愿单物品（wishlist_items表） ===
            migrateWishlistItems(database, currentTime)
            
            // === 迁移购物物品（shopping_items表） ===
            migrateShoppingItems(database, currentTime)
        }
        
        /**
         * 迁移库存物品
         */
        private fun migrateInventoryItems(database: SupportSQLiteDatabase, currentTime: Long) {
            // 1. 从items表创建unified_items记录
            database.execSQL("""
                INSERT INTO unified_items (
                    name, category, subCategory, brand, specification, customNote, createdDate, updatedDate
                )
                SELECT 
                    name, category, subCategory, brand, specification, customNote, addDate, $currentTime
                FROM items
            """)
            
            // 2. 为每个库存物品创建INVENTORY状态
            database.execSQL("""
                INSERT INTO item_states (
                    itemId, stateType, isActive, activatedDate, createdDate
                )
                SELECT 
                    (SELECT u.id FROM unified_items u WHERE u.name = i.name AND u.category = i.category LIMIT 1),
                    'INVENTORY',
                    1,
                    i.addDate,
                    $currentTime
                FROM items i
            """)
            
            // 3. 创建库存详情记录（去重处理）
            database.execSQL("""
                INSERT OR IGNORE INTO inventory_details (
                    itemId, quantity, unit, locationId, productionDate, expirationDate, 
                    openDate, purchaseDate, wasteDate, openStatus, status, 
                    stockWarningThreshold, isHighTurnover, price, priceUnit, 
                    totalPrice, totalPriceUnit, purchaseChannel, storeName,
                    capacity, capacityUnit, rating, season, serialNumber,
                    shelfLife, warrantyPeriod, warrantyEndDate, createdDate, updatedDate
                )
                SELECT 
                    (SELECT u.id FROM unified_items u WHERE u.name = i.name AND u.category = i.category LIMIT 1),
                    i.quantity, i.unit, i.locationId, i.productionDate, i.expirationDate,
                    i.openDate, i.purchaseDate, i.wasteDate, i.openStatus, i.status,
                    i.stockWarningThreshold, i.isHighTurnover, i.price, i.priceUnit,
                    i.totalPrice, i.totalPriceUnit, i.purchaseChannel, i.storeName,
                    i.capacity, i.capacityUnit, i.rating, i.season, i.serialNumber,
                    i.shelfLife, i.warrantyPeriod, i.warrantyEndDate, i.addDate, $currentTime
                FROM items i
            """)
        }
        
        /**
         * 迁移心愿单物品
         */
        private fun migrateWishlistItems(database: SupportSQLiteDatabase, currentTime: Long) {
            // 1. 从wishlist_items表创建unified_items记录（如果不存在）
            database.execSQL("""
                INSERT OR IGNORE INTO unified_items (
                    name, category, subCategory, brand, specification, customNote, createdDate, updatedDate
                )
                SELECT 
                    name, category, subCategory, brand, specification, customNote, addDate, lastModified
                FROM wishlist_items
            """)
            
            // 2. 为每个心愿单物品创建WISHLIST状态
            database.execSQL("""
                INSERT INTO item_states (
                    itemId, stateType, isActive, activatedDate, createdDate
                )
                SELECT 
                    (SELECT u.id FROM unified_items u 
                     WHERE u.name = w.name AND u.category = w.category 
                     AND (u.brand = w.brand OR (u.brand IS NULL AND w.brand IS NULL))
                     LIMIT 1),
                    'WISHLIST',
                    CASE WHEN w.isActive = 1 THEN 1 ELSE 0 END,
                    w.addDate,
                    $currentTime
                FROM wishlist_items w
            """)
            
            // 3. 创建心愿单详情记录（去重处理）
            database.execSQL("""
                INSERT OR IGNORE INTO wishlist_details (
                    itemId, price, targetPrice, priceUnit, currentPrice, lowestPrice, highestPrice,
                    priority, urgency, quantity, quantityUnit, budgetLimit, purchaseChannel, preferredBrand,
                    isPriceTrackingEnabled, priceDropThreshold, lastPriceCheck, priceCheckInterval,
                    isPaused, achievedDate, sourceUrl, imageUrl, relatedInventoryItemId, addedReason,
                    viewCount, lastViewDate, priceChangeCount, createdDate, lastModified
                )
                SELECT 
                    (SELECT u.id FROM unified_items u 
                     WHERE u.name = w.name AND u.category = w.category 
                     AND (u.brand = w.brand OR (u.brand IS NULL AND w.brand IS NULL))
                     LIMIT 1),
                    w.price, w.targetPrice, w.priceUnit, w.currentPrice, w.lowestPrice, w.highestPrice,
                    w.priority, w.urgency, w.quantity, w.quantityUnit, w.budgetLimit, w.purchaseChannel, w.preferredBrand,
                    w.isPriceTrackingEnabled, w.priceDropThreshold, w.lastPriceCheck, w.priceCheckInterval,
                    w.isPaused, w.achievedDate, w.sourceUrl, w.imageUrl, w.relatedItemId, w.addedReason,
                    w.viewCount, w.lastViewDate, w.priceChangeCount, w.addDate, w.lastModified
                FROM wishlist_items w
            """)
        }
        
        /**
         * 迁移购物物品
         */
        private fun migrateShoppingItems(database: SupportSQLiteDatabase, currentTime: Long) {
            // 1. 从shopping_items表创建unified_items记录（如果不存在）
            database.execSQL("""
                INSERT OR IGNORE INTO unified_items (
                    name, category, subCategory, brand, specification, customNote, createdDate, updatedDate
                )
                SELECT 
                    name, category, subCategory, brand, specification, customNote, addDate, $currentTime
                FROM shopping_items
            """)
            
            // 2. 为每个购物物品创建SHOPPING状态
            database.execSQL("""
                INSERT INTO item_states (
                    itemId, stateType, isActive, activatedDate, contextId, createdDate
                )
                SELECT 
                    (SELECT u.id FROM unified_items u 
                     WHERE u.name = s.name AND u.category = s.category 
                     AND (u.brand = s.brand OR (u.brand IS NULL AND s.brand IS NULL))
                     LIMIT 1),
                    'SHOPPING',
                    CASE WHEN s.isPurchased = 0 THEN 1 ELSE 0 END,
                    s.addDate,
                    s.listId,
                    $currentTime
                FROM shopping_items s
            """)
            
            // 3. 创建购物详情记录（去重处理）
            database.execSQL("""
                INSERT OR IGNORE INTO shopping_details (
                    itemId, shoppingListId, quantity, quantityUnit, estimatedPrice, actualPrice,
                    priceUnit, budgetLimit, totalPrice, totalPriceUnit, purchaseChannel, storeName,
                    preferredStore, purchaseDate, isPurchased, priority, urgencyLevel, deadline,
                    capacity, capacityUnit, rating, season, serialNumber, sourceItemId,
                    recommendationReason, addedReason, addDate, completedDate, remindDate,
                    isRecurring, recurringInterval, tags
                )
                SELECT 
                    (SELECT u.id FROM unified_items u 
                     WHERE u.name = s.name AND u.category = s.category 
                     AND (u.brand = s.brand OR (u.brand IS NULL AND s.brand IS NULL))
                     LIMIT 1),
                    s.listId, s.quantity, '个', s.price, s.actualPrice,
                    s.priceUnit, s.budgetLimit, s.totalPrice, s.totalPriceUnit, s.purchaseChannel, s.storeName,
                    s.preferredStore, s.purchaseDate, s.isPurchased, s.priority, s.urgencyLevel, s.deadline,
                    s.capacity, s.capacityUnit, s.rating, s.season, s.serialNumber, s.sourceItemId,
                    s.recommendationReason, s.addedReason, s.addDate, s.completedDate, s.remindDate,
                    s.isRecurring, s.recurringInterval, s.tags
                FROM shopping_items s
            """)
        }
        
        /**
         * 更新组件表的外键引用
         */
        private fun updateComponentReferences(database: SupportSQLiteDatabase) {
            // 更新photos表的itemId引用
            database.execSQL("""
                UPDATE photos 
                SET itemId = (
                    SELECT u.id FROM unified_items u 
                    JOIN items i ON (u.name = i.name AND u.category = i.category)
                    WHERE i.id = photos.itemId
                    LIMIT 1
                )
                WHERE EXISTS (
                    SELECT 1 FROM items i WHERE i.id = photos.itemId
                )
            """)
            
            // 更新item_tag_cross_ref表的itemId引用
            database.execSQL("""
                UPDATE item_tag_cross_ref 
                SET itemId = (
                    SELECT u.id FROM unified_items u 
                    JOIN items i ON (u.name = i.name AND u.category = i.category)
                    WHERE i.id = item_tag_cross_ref.itemId
                    LIMIT 1
                )
                WHERE EXISTS (
                    SELECT 1 FROM items i WHERE i.id = item_tag_cross_ref.itemId
                )
            """)
            
            // 更新calendar_events表的itemId引用
            database.execSQL("""
                UPDATE calendar_events 
                SET itemId = (
                    SELECT u.id FROM unified_items u 
                    JOIN items i ON (u.name = i.name AND u.category = i.category)
                    WHERE i.id = calendar_events.itemId
                    LIMIT 1
                )
                WHERE EXISTS (
                    SELECT 1 FROM items i WHERE i.id = calendar_events.itemId
                )
            """)
            
            // 更新warranties表的itemId引用
            database.execSQL("""
                UPDATE warranties 
                SET itemId = (
                    SELECT u.id FROM unified_items u 
                    JOIN items i ON (u.name = i.name AND u.category = i.category)
                    WHERE i.id = warranties.itemId
                    LIMIT 1
                )
                WHERE EXISTS (
                    SELECT 1 FROM items i WHERE i.id = warranties.itemId
                )
            """)
            
            // 更新borrows表的itemId引用
            database.execSQL("""
                UPDATE borrows 
                SET itemId = (
                    SELECT u.id FROM unified_items u 
                    JOIN items i ON (u.name = i.name AND u.category = i.category)
                    WHERE i.id = borrows.itemId
                    LIMIT 1
                )
                WHERE EXISTS (
                    SELECT 1 FROM items i WHERE i.id = borrows.itemId
                )
            """)
            
            // 更新wishlist_price_history表的wishlistItemId引用
            database.execSQL("""
                UPDATE wishlist_price_history 
                SET wishlistItemId = (
                    SELECT u.id FROM unified_items u 
                    JOIN wishlist_items w ON (u.name = w.name AND u.category = w.category)
                    WHERE w.id = wishlist_price_history.wishlistItemId
                    LIMIT 1
                )
                WHERE EXISTS (
                    SELECT 1 FROM wishlist_items w WHERE w.id = wishlist_price_history.wishlistItemId
                )
            """)
        }
        
        /**
         * 创建优化索引
         */
        private fun createOptimizedIndexes(database: SupportSQLiteDatabase) {
            // unified_items 索引
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_unified_items_category` ON `unified_items` (`category`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_unified_items_name` ON `unified_items` (`name`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_unified_items_createdDate` ON `unified_items` (`createdDate`)")
            
            // item_states 索引（重要的组合索引）
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_item_states_itemId` ON `item_states` (`itemId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_item_states_stateType` ON `item_states` (`stateType`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_item_states_isActive` ON `item_states` (`isActive`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_item_states_itemId_stateType_isActive` ON `item_states` (`itemId`, `stateType`, `isActive`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_item_states_activatedDate` ON `item_states` (`activatedDate`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_item_states_contextId` ON `item_states` (`contextId`)")
            
            // wishlist_details 索引
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_wishlist_details_itemId` ON `wishlist_details` (`itemId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_wishlist_details_priority` ON `wishlist_details` (`priority`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_wishlist_details_urgency` ON `wishlist_details` (`urgency`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_wishlist_details_targetPrice` ON `wishlist_details` (`targetPrice`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_wishlist_details_isPriceTrackingEnabled` ON `wishlist_details` (`isPriceTrackingEnabled`)")
            
            // shopping_details 索引
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_details_itemId` ON `shopping_details` (`itemId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_details_shoppingListId` ON `shopping_details` (`shoppingListId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_details_priority` ON `shopping_details` (`priority`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_details_urgencyLevel` ON `shopping_details` (`urgencyLevel`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_details_isPurchased` ON `shopping_details` (`isPurchased`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_details_deadline` ON `shopping_details` (`deadline`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_shopping_details_shoppingListId_isPurchased` ON `shopping_details` (`shoppingListId`, `isPurchased`)")
            
            // inventory_details 索引
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_details_itemId` ON `inventory_details` (`itemId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_details_locationId` ON `inventory_details` (`locationId`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_details_status` ON `inventory_details` (`status`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_details_expirationDate` ON `inventory_details` (`expirationDate`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_details_stockWarningThreshold` ON `inventory_details` (`stockWarningThreshold`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_details_status_expirationDate` ON `inventory_details` (`status`, `expirationDate`)")
        }
        
        /**
         * 清理旧表（备份后删除）
         */
        private fun cleanupOldTables(database: SupportSQLiteDatabase) {
            // 创建备份表（以防需要回滚）
            database.execSQL("CREATE TABLE items_backup_v31 AS SELECT * FROM items")
            database.execSQL("CREATE TABLE wishlist_items_backup_v31 AS SELECT * FROM wishlist_items")
            database.execSQL("CREATE TABLE shopping_items_backup_v31 AS SELECT * FROM shopping_items")
            
            // 删除旧表
            database.execSQL("DROP TABLE items")
            database.execSQL("DROP TABLE wishlist_items") 
            database.execSQL("DROP TABLE shopping_items")
            
            // 删除deleted_items表，因为现在用item_states的DELETED状态管理
            database.execSQL("CREATE TABLE deleted_items_backup_v31 AS SELECT * FROM deleted_items")
            database.execSQL("DROP TABLE deleted_items")
        }
    }
}
