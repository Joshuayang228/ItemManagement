package com.example.itemmanagement.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.itemmanagement.data.dao.ItemDao
import com.example.itemmanagement.data.dao.CalendarEventDao
import com.example.itemmanagement.data.dao.ShoppingDao
import com.example.itemmanagement.data.dao.ShoppingListDao
import com.example.itemmanagement.data.dao.ReminderSettingsDao
import com.example.itemmanagement.data.dao.CategoryThresholdDao
import com.example.itemmanagement.data.dao.CustomRuleDao
import com.example.itemmanagement.data.dao.WarrantyDao
import com.example.itemmanagement.data.dao.BorrowDao
import com.example.itemmanagement.data.dao.RecycleBinDao
import com.example.itemmanagement.data.dao.UserProfileDao
import com.example.itemmanagement.data.entity.*

@Database(
    entities = [
        ItemEntity::class,
        LocationEntity::class,
        PhotoEntity::class,
        TagEntity::class,
        ItemTagCrossRef::class,
        CalendarEventEntity::class,
        ShoppingItemEntity::class,
        ShoppingListEntity::class,
        ReminderSettingsEntity::class,
        CategoryThresholdEntity::class,
        CustomRuleEntity::class,
        WarrantyEntity::class,
        BorrowEntity::class,
        DeletedItemEntity::class,
        UserProfileEntity::class
    ],
    version = 19,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun reminderSettingsDao(): ReminderSettingsDao
    abstract fun categoryThresholdDao(): CategoryThresholdDao
    abstract fun customRuleDao(): CustomRuleDao
    abstract fun warrantyDao(): WarrantyDao
    abstract fun borrowDao(): BorrowDao
    abstract fun recycleBinDao(): RecycleBinDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "item_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_5, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. 创建新表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS locations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        area TEXT NOT NULL,
                        container TEXT,
                        sublocation TEXT
                    )
                """)

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS photos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        itemId INTEGER NOT NULL,
                        uri TEXT NOT NULL,
                        isMain INTEGER NOT NULL DEFAULT 0,
                        displayOrder INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(itemId) REFERENCES items(id) ON DELETE CASCADE
                    )
                """)

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS tags (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        color TEXT
                    )
                """)

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS item_tag_cross_ref (
                        itemId INTEGER NOT NULL,
                        tagId INTEGER NOT NULL,
                        PRIMARY KEY(itemId, tagId),
                        FOREIGN KEY(itemId) REFERENCES items(id) ON DELETE CASCADE,
                        FOREIGN KEY(tagId) REFERENCES tags(id) ON DELETE CASCADE
                    )
                """)

                // 2. 创建临时表存储旧数据
                database.execSQL("""
                    CREATE TABLE items_temp (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        unit TEXT NOT NULL,
                        locationId INTEGER,
                        category TEXT NOT NULL,
                        addDate INTEGER NOT NULL,
                        productionDate INTEGER,
                        expirationDate INTEGER,
                        openStatus TEXT NOT NULL,
                        openDate INTEGER,
                        brand TEXT,
                        specification TEXT,
                        status TEXT NOT NULL,
                        stockWarningThreshold INTEGER,
                        price REAL,
                        purchaseChannel TEXT,
                        storeName TEXT,
                        subCategory TEXT,
                        customNote TEXT,
                        FOREIGN KEY(locationId) REFERENCES locations(id) ON DELETE SET NULL
                    )
                """)

                // 3. 迁移数据
                // 3.1 迁移位置数据
                database.execSQL("""
                    INSERT INTO locations (area, container, sublocation)
                    SELECT DISTINCT 
                        location_area,
                        location_container,
                        location_sublocation
                    FROM items
                    WHERE location_area IS NOT NULL
                """)

                // 3.2 更新items表中的locationId
                database.execSQL("""
                    INSERT INTO items_temp (
                        id, name, quantity, unit, locationId, category,
                        addDate, productionDate, expirationDate, openStatus,
                        openDate, brand, specification, status,
                        stockWarningThreshold, price, purchaseChannel,
                        storeName, subCategory, customNote
                    )
                    SELECT 
                        i.id, i.name, i.quantity, i.unit, l.id, i.category,
                        i.addDate, i.productionDate, i.expirationDate, i.openStatus,
                        i.openDate, i.brand, i.specification, i.status,
                        i.stockWarningThreshold, i.price, i.purchaseChannel,
                        i.storeName, i.subCategory, i.customNote
                    FROM items i
                    LEFT JOIN locations l ON 
                        l.area = i.location_area AND
                        l.container = i.location_container AND
                        l.sublocation = i.location_sublocation
                """)

                // 4. 删除旧表并重命名新表
                database.execSQL("DROP TABLE items")
                database.execSQL("ALTER TABLE items_temp RENAME TO items")

                // 5. 创建必要的索引
                database.execSQL("CREATE INDEX IF NOT EXISTS index_photos_itemId ON photos(itemId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_items_locationId ON items(locationId)")
            }
        }
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加新字段到 items 表
                database.execSQL("ALTER TABLE items ADD COLUMN season TEXT")
                database.execSQL("ALTER TABLE items ADD COLUMN capacity REAL")
                database.execSQL("ALTER TABLE items ADD COLUMN rating REAL")
                database.execSQL("ALTER TABLE items ADD COLUMN totalPrice REAL")
                database.execSQL("ALTER TABLE items ADD COLUMN purchaseDate INTEGER")
                database.execSQL("ALTER TABLE items ADD COLUMN shelfLife INTEGER")
                database.execSQL("ALTER TABLE items ADD COLUMN warrantyPeriod INTEGER")
                database.execSQL("ALTER TABLE items ADD COLUMN warrantyEndDate INTEGER")
                database.execSQL("ALTER TABLE items ADD COLUMN serialNumber TEXT")
            }
        }
        
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加索引到item_tag_cross_ref表
                database.execSQL("CREATE INDEX IF NOT EXISTS index_item_tag_cross_ref_itemId ON item_tag_cross_ref(itemId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_item_tag_cross_ref_tagId ON item_tag_cross_ref(tagId)")
            }
        }
        
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加容量单位字段
                database.execSQL("ALTER TABLE items ADD COLUMN capacityUnit TEXT")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加价格单位和总价单位字段
                database.execSQL("ALTER TABLE items ADD COLUMN priceUnit TEXT")
                database.execSQL("ALTER TABLE items ADD COLUMN totalPriceUnit TEXT")
            }
        }
        
        // 添加从版本6降级到版本5的迁移
        private val MIGRATION_6_5 = object : Migration(6, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建临时表，不包含要删除的列
                database.execSQL("""
                    CREATE TABLE items_temp (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        unit TEXT NOT NULL,
                        locationId INTEGER,
                        category TEXT NOT NULL,
                        addDate INTEGER NOT NULL,
                        productionDate INTEGER,
                        expirationDate INTEGER,
                        openStatus TEXT NOT NULL,
                        openDate INTEGER,
                        brand TEXT,
                        specification TEXT,
                        status TEXT NOT NULL,
                        stockWarningThreshold INTEGER,
                        price REAL,
                        purchaseChannel TEXT,
                        storeName TEXT,
                        subCategory TEXT,
                        customNote TEXT,
                        season TEXT,
                        capacity REAL,
                        capacityUnit TEXT,
                        rating REAL,
                        totalPrice REAL,
                        purchaseDate INTEGER,
                        shelfLife INTEGER,
                        warrantyPeriod INTEGER,
                        warrantyEndDate INTEGER,
                        serialNumber TEXT,
                        FOREIGN KEY(locationId) REFERENCES locations(id) ON DELETE SET NULL
                    )
                """)
                
                // 复制数据到临时表
                database.execSQL("""
                    INSERT INTO items_temp (
                        id, name, quantity, unit, locationId, category, addDate, productionDate,
                        expirationDate, openStatus, openDate, brand, specification, status,
                        stockWarningThreshold, price, purchaseChannel, storeName, subCategory,
                        customNote, season, capacity, capacityUnit, rating, totalPrice,
                        purchaseDate, shelfLife, warrantyPeriod, warrantyEndDate, serialNumber
                    )
                    SELECT
                        id, name, quantity, unit, locationId, category, addDate, productionDate,
                        expirationDate, openStatus, openDate, brand, specification, status,
                        stockWarningThreshold, price, purchaseChannel, storeName, subCategory,
                        customNote, season, capacity, capacityUnit, rating, totalPrice,
                        purchaseDate, shelfLife, warrantyPeriod, warrantyEndDate, serialNumber
                    FROM items
                """)
                
                // 删除旧表
                database.execSQL("DROP TABLE items")
                
                // 重命名临时表
                database.execSQL("ALTER TABLE items_temp RENAME TO items")
                
                // 重新创建索引
                database.execSQL("CREATE INDEX IF NOT EXISTS index_items_locationId ON items(locationId)")
            }
        }

        // 添加从版本6升级到版本7的迁移，处理nullable openStatus字段
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建临时表，包含可为null的openStatus字段
                database.execSQL("""
                    CREATE TABLE items_temp (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        unit TEXT NOT NULL,
                        locationId INTEGER,
                        category TEXT NOT NULL,
                        addDate INTEGER NOT NULL,
                        productionDate INTEGER,
                        expirationDate INTEGER,
                        openStatus TEXT,
                        openDate INTEGER,
                        brand TEXT,
                        specification TEXT,
                        status TEXT NOT NULL,
                        stockWarningThreshold INTEGER,
                        price REAL,
                        priceUnit TEXT,
                        purchaseChannel TEXT,
                        storeName TEXT,
                        subCategory TEXT,
                        customNote TEXT,
                        season TEXT,
                        capacity REAL,
                        capacityUnit TEXT,
                        rating REAL,
                        totalPrice REAL,
                        totalPriceUnit TEXT,
                        purchaseDate INTEGER,
                        shelfLife INTEGER,
                        warrantyPeriod INTEGER,
                        warrantyEndDate INTEGER,
                        serialNumber TEXT,
                        FOREIGN KEY(locationId) REFERENCES locations(id) ON DELETE SET NULL
                    )
                """)
                
                // 复制数据到临时表
                database.execSQL("""
                    INSERT INTO items_temp (
                        id, name, quantity, unit, locationId, category, addDate, productionDate,
                        expirationDate, openStatus, openDate, brand, specification, status,
                        stockWarningThreshold, price, priceUnit, purchaseChannel, storeName, subCategory,
                        customNote, season, capacity, capacityUnit, rating, totalPrice, totalPriceUnit,
                        purchaseDate, shelfLife, warrantyPeriod, warrantyEndDate, serialNumber
                    )
                    SELECT
                        id, name, quantity, unit, locationId, category, addDate, productionDate,
                        expirationDate, openStatus, openDate, brand, specification, status,
                        stockWarningThreshold, price, priceUnit, purchaseChannel, storeName, subCategory,
                        customNote, season, capacity, capacityUnit, rating, totalPrice, totalPriceUnit,
                        purchaseDate, shelfLife, warrantyPeriod, warrantyEndDate, serialNumber
                    FROM items
                """)
                
                // 删除旧表
                database.execSQL("DROP TABLE items")
                
                // 重命名临时表
                database.execSQL("ALTER TABLE items_temp RENAME TO items")
                
                // 重新创建索引
                database.execSQL("CREATE INDEX IF NOT EXISTS index_items_locationId ON items(locationId)")
            }
        }

        // 添加从版本7升级到版本8的迁移，添加心愿单和高周转字段
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加心愿单和高周转字段
                database.execSQL("ALTER TABLE items ADD COLUMN isWishlistItem INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE items ADD COLUMN isHighTurnover INTEGER NOT NULL DEFAULT 0")
            }
        }

        // 添加从版本8升级到版本9的迁移，添加日历事件和购物清单功能
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建日历事件表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS calendar_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        itemId INTEGER NOT NULL,
                        eventType TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        eventDate INTEGER NOT NULL,
                        reminderDays TEXT NOT NULL,
                        priority TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        recurrenceType TEXT,
                        createdDate INTEGER NOT NULL,
                        completedDate INTEGER,
                        FOREIGN KEY(itemId) REFERENCES items(id) ON DELETE CASCADE
                    )
                """)
                
                // 创建日历事件表的索引
                database.execSQL("CREATE INDEX IF NOT EXISTS index_calendar_events_itemId ON calendar_events(itemId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_calendar_events_eventDate ON calendar_events(eventDate)")
                
                // 创建购物清单表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS shopping_lists (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        type TEXT NOT NULL,
                        createdDate INTEGER NOT NULL,
                        targetDate INTEGER,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        notes TEXT
                    )
                """)
                
                // 创建购物物品表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS shopping_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        listId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        category TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        unit TEXT NOT NULL,
                        estimatedPrice REAL,
                        actualPrice REAL,
                        priority TEXT NOT NULL DEFAULT 'NORMAL',
                        store TEXT,
                        notes TEXT,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        linkedItemId INTEGER,
                        addedReason TEXT NOT NULL DEFAULT 'USER_MANUAL',
                        createdDate INTEGER NOT NULL,
                        completedDate INTEGER,
                        FOREIGN KEY(listId) REFERENCES shopping_lists(id) ON DELETE CASCADE,
                        FOREIGN KEY(linkedItemId) REFERENCES items(id) ON DELETE SET NULL
                    )
                """)
                
                // 创建购物物品表的索引
                database.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_items_listId ON shopping_items(listId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_items_linkedItemId ON shopping_items(linkedItemId)")
            }
        }

        // 添加从版本9升级到版本10的迁移，简化购物清单功能
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. 创建新的简化购物物品表（与ShoppingItemEntity定义完全匹配）
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS shopping_items_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        unit TEXT NOT NULL,
                        notes TEXT,
                        isPurchased INTEGER NOT NULL DEFAULT 0,
                        category TEXT,
                        brand TEXT,
                        creationDate INTEGER NOT NULL,
                        sourceItemId INTEGER
                    )
                """)
                
                // 2. 迁移现有数据（如果有的话）
                database.execSQL("""
                    INSERT INTO shopping_items_new (
                        id, name, quantity, unit, notes, isPurchased, category, brand, creationDate, sourceItemId
                    )
                    SELECT 
                        id, name, quantity, unit, notes, isCompleted, category, '', createdDate, linkedItemId
                    FROM shopping_items
                """)
                
                // 3. 删除旧表
                database.execSQL("DROP TABLE IF EXISTS shopping_items")
                database.execSQL("DROP TABLE IF EXISTS shopping_lists")
                
                // 4. 重命名新表
                database.execSQL("ALTER TABLE shopping_items_new RENAME TO shopping_items")
                
                // 注意：不添加外键约束和索引，因为ShoppingItemEntity没有定义它们
            }
        }

        // 迁移 10 到 11：支持完整购物清单系统
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. 创建新的购物清单表
                database.execSQL("""
                    CREATE TABLE shopping_lists (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT,
                        type TEXT NOT NULL DEFAULT 'DAILY',
                        status TEXT NOT NULL DEFAULT 'ACTIVE',
                        createdDate INTEGER NOT NULL,
                        targetDate INTEGER,
                        estimatedBudget REAL,
                        actualSpent REAL,
                        notes TEXT
                    )
                """)
                
                // 2. 创建默认购物清单
                database.execSQL("""
                    INSERT INTO shopping_lists (name, description, type, status, createdDate)
                    VALUES ('我的购物清单', '默认购物清单', 'DAILY', 'ACTIVE', ${System.currentTimeMillis()})
                """)
                
                // 3. 备份现有购物物品数据
                database.execSQL("""
                    CREATE TABLE shopping_items_backup AS 
                    SELECT * FROM shopping_items
                """)
                
                // 4. 删除旧的购物物品表
                database.execSQL("DROP TABLE shopping_items")
                
                // 5. 创建新的购物物品表（与ItemEntity字段一致）
                database.execSQL("""
                    CREATE TABLE shopping_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        listId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        unit TEXT NOT NULL,
                        category TEXT NOT NULL,
                        brand TEXT,
                        specification TEXT,
                        subCategory TEXT,
                        customNote TEXT,
                        price REAL,
                        priceUnit TEXT,
                        totalPrice REAL,
                        totalPriceUnit TEXT,
                        estimatedPrice REAL,
                        actualPrice REAL,
                        purchaseChannel TEXT,
                        storeName TEXT,
                        purchaseDate INTEGER,
                        capacity REAL,
                        capacityUnit TEXT,
                        rating REAL,
                        isPurchased INTEGER NOT NULL DEFAULT 0,
                        priority TEXT NOT NULL DEFAULT 'NORMAL',
                        createdDate INTEGER NOT NULL,
                        sourceItemId INTEGER,
                        serialNumber TEXT,
                        season TEXT,
                        FOREIGN KEY(listId) REFERENCES shopping_lists(id) ON DELETE CASCADE
                    )
                """)
                
                // 6. 迁移旧数据到新表（关联到默认清单）
                database.execSQL("""
                    INSERT INTO shopping_items (
                        listId, name, quantity, unit, category, brand, customNote, 
                        isPurchased, createdDate, sourceItemId
                    )
                    SELECT 
                        1 as listId,
                        name, quantity, unit, 
                        COALESCE(category, '未分类') as category, 
                        brand, notes as customNote,
                        isPurchased, creationDate, sourceItemId
                    FROM shopping_items_backup
                """)
                
                // 7. 创建索引
                database.execSQL("CREATE INDEX index_shopping_items_listId ON shopping_items(listId)")
                
                // 8. 清理备份表
                database.execSQL("DROP TABLE shopping_items_backup")
            }
        }

        /**
         * 迁移 11 -> 12: 重构购物物品表字段
         * 移除不必要的unit字段，添加购物特有字段
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. 备份现有数据
                database.execSQL("CREATE TABLE shopping_items_backup AS SELECT * FROM shopping_items")
                
                // 2. 删除旧表
                database.execSQL("DROP TABLE shopping_items")
                
                // 3. 创建新的购物物品表（移除unit字段，添加新字段）
                database.execSQL("""
                    CREATE TABLE shopping_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        listId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        category TEXT NOT NULL,
                        subCategory TEXT,
                        brand TEXT,
                        specification TEXT,
                        customNote TEXT,
                        estimatedPrice REAL,
                        actualPrice REAL,
                        priceUnit TEXT DEFAULT '元',
                        budgetLimit REAL,
                        totalPrice REAL,
                        purchaseChannel TEXT,
                        storeName TEXT,
                        preferredStore TEXT,
                        purchaseDate INTEGER,
                        isPurchased INTEGER NOT NULL DEFAULT 0,
                        priority TEXT NOT NULL DEFAULT 'NORMAL',
                        urgencyLevel TEXT NOT NULL DEFAULT 'NORMAL',
                        deadline INTEGER,
                        capacity REAL,
                        capacityUnit TEXT,
                        rating REAL,
                        season TEXT,
                        sourceItemId INTEGER,
                        recommendationReason TEXT,
                        addedReason TEXT DEFAULT 'USER_MANUAL',
                        createdDate INTEGER NOT NULL,
                        completedDate INTEGER,
                        remindDate INTEGER,
                        serialNumber TEXT,
                        isRecurring INTEGER NOT NULL DEFAULT 0,
                        recurringInterval INTEGER,
                        tags TEXT,
                        FOREIGN KEY(listId) REFERENCES shopping_lists(id) ON DELETE CASCADE
                    )
                """)
                
                // 4. 迁移数据（保留兼容的字段）
                database.execSQL("""
                    INSERT INTO shopping_items (
                        id, listId, name, quantity, category, subCategory, brand, 
                        specification, customNote, estimatedPrice, actualPrice, 
                        priceUnit, totalPrice, purchaseChannel, storeName, 
                        purchaseDate, isPurchased, priority, capacity, capacityUnit, 
                        rating, season, sourceItemId, createdDate, serialNumber
                    )
                    SELECT 
                        id, listId, name, quantity, 
                        COALESCE(category, '未分类') as category,
                        subCategory, brand, specification, customNote,
                        estimatedPrice, actualPrice, priceUnit, totalPrice,
                        purchaseChannel, storeName, purchaseDate, 
                        COALESCE(isPurchased, 0) as isPurchased,
                        COALESCE(priority, 'NORMAL') as priority,
                        capacity, capacityUnit, rating, season, sourceItemId,
                        COALESCE(createdDate, datetime('now')) as createdDate,
                        serialNumber
                    FROM shopping_items_backup
                """)
                
                // 5. 创建索引
                database.execSQL("CREATE INDEX index_shopping_items_listId ON shopping_items(listId)")
                
                // 6. 清理备份表
                database.execSQL("DROP TABLE shopping_items_backup")
            }
        }
        
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建提醒设置表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `reminder_settings` (
                        `id` INTEGER NOT NULL,
                        `expirationAdvanceDays` INTEGER NOT NULL DEFAULT 7,
                        `includeWarranty` INTEGER NOT NULL DEFAULT 1,
                        `notificationTime` TEXT NOT NULL DEFAULT '09:00',
                        `stockReminderEnabled` INTEGER NOT NULL DEFAULT 1,
                        `pushNotificationEnabled` INTEGER NOT NULL DEFAULT 1,
                        `inAppReminderEnabled` INTEGER NOT NULL DEFAULT 1,
                        `quietHourStart` TEXT NOT NULL DEFAULT '22:00',
                        `quietHourEnd` TEXT NOT NULL DEFAULT '08:00',
                        `weekendPause` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """)
                
                // 创建分类阈值表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `category_thresholds` (
                        `category` TEXT NOT NULL,
                        `minQuantity` REAL NOT NULL DEFAULT 1.0,
                        `enabled` INTEGER NOT NULL DEFAULT 1,
                        `unit` TEXT NOT NULL DEFAULT '个',
                        `description` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`category`)
                    )
                """)
                
                // 创建自定义规则表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `custom_rules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `targetCategory` TEXT,
                        `targetItemName` TEXT,
                        `ruleType` TEXT NOT NULL,
                        `advanceDays` INTEGER,
                        `minQuantity` REAL,
                        `maxQuantity` REAL,
                        `customMessage` TEXT NOT NULL DEFAULT '',
                        `priority` TEXT NOT NULL DEFAULT 'NORMAL',
                        `enabled` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `lastTriggeredAt` INTEGER
                    )
                """)
                
                // 插入默认提醒设置
                database.execSQL("""
                    INSERT OR IGNORE INTO reminder_settings (
                        id, createdAt, updatedAt
                    ) VALUES (
                        1, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}
                    )
                """)
            }
        }
        
        /**
         * 迁移 13 -> 14: 添加保修管理功能
         * 创建warranties表来存储完整的保修信息
         */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建保修信息表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `warranties` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `itemId` INTEGER NOT NULL,
                        `purchaseDate` INTEGER NOT NULL,
                        `warrantyPeriodMonths` INTEGER NOT NULL,
                        `warrantyEndDate` INTEGER NOT NULL,
                        `receiptImageUris` TEXT,
                        `notes` TEXT,
                        `status` TEXT NOT NULL DEFAULT 'ACTIVE',
                        `warrantyProvider` TEXT,
                        `contactInfo` TEXT,
                        `createdDate` INTEGER NOT NULL,
                        `updatedDate` INTEGER NOT NULL,
                        FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON DELETE CASCADE
                    )
                """)
                
                // 创建索引以优化查询性能
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_warranties_itemId` ON `warranties` (`itemId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_warranties_warrantyEndDate` ON `warranties` (`warrantyEndDate`)")
                
                // 从现有的items表迁移保修信息到新的warranties表
                // 只迁移有保修信息的物品（warrantyPeriod不为null且大于0）
                database.execSQL("""
                    INSERT INTO warranties (
                        itemId, purchaseDate, warrantyPeriodMonths, warrantyEndDate, 
                        status, createdDate, updatedDate
                    )
                    SELECT 
                        id as itemId,
                        COALESCE(purchaseDate, addDate) as purchaseDate,
                        COALESCE(warrantyPeriod, 12) as warrantyPeriodMonths,
                        COALESCE(warrantyEndDate, 
                            datetime(COALESCE(purchaseDate, addDate)/1000 + COALESCE(warrantyPeriod, 12) * 30 * 24 * 3600, 'unixepoch') * 1000
                        ) as warrantyEndDate,
                        CASE 
                            WHEN warrantyEndDate IS NOT NULL AND warrantyEndDate < ${System.currentTimeMillis()} THEN 'EXPIRED'
                            ELSE 'ACTIVE'
                        END as status,
                        ${System.currentTimeMillis()} as createdDate,
                        ${System.currentTimeMillis()} as updatedDate
                    FROM items 
                    WHERE warrantyPeriod IS NOT NULL AND warrantyPeriod > 0
                """)
            }
        }
        
        /**
         * 数据库版本14到15的迁移
         * 添加借还管理功能，创建borrows表
         */
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建借还记录表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `borrows` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `itemId` INTEGER NOT NULL,
                        `borrowerName` TEXT NOT NULL,
                        `borrowerContact` TEXT,
                        `borrowDate` INTEGER NOT NULL,
                        `expectedReturnDate` INTEGER NOT NULL,
                        `actualReturnDate` INTEGER,
                        `status` TEXT NOT NULL DEFAULT 'BORROWED',
                        `notes` TEXT,
                        `createdDate` INTEGER NOT NULL,
                        `updatedDate` INTEGER NOT NULL,
                        FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON DELETE CASCADE
                    )
                """)
                
                // 创建索引以优化查询性能
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_borrows_itemId` ON `borrows` (`itemId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_borrows_status` ON `borrows` (`status`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_borrows_borrowDate` ON `borrows` (`borrowDate`)")
            }
        }
        
        /**
         * 数据库版本15到16的迁移
         * 添加回收站功能，创建deleted_items表
         */
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建已删除物品表（回收站）
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `deleted_items` (
                        `originalId` INTEGER PRIMARY KEY NOT NULL,
                        `name` TEXT NOT NULL,
                        `brand` TEXT,
                        `specification` TEXT,
                        `category` TEXT NOT NULL,
                        `quantity` REAL,
                        `unit` TEXT,
                        `price` REAL,
                        `purchaseDate` INTEGER,
                        `expirationDate` INTEGER,
                        `warrantyEndDate` INTEGER,
                        `customNote` TEXT,
                        `addDate` INTEGER NOT NULL,
                        `locationId` INTEGER,
                        `locationArea` TEXT,
                        `locationContainer` TEXT,
                        `locationSublocation` TEXT,
                        `photoUris` TEXT,
                        `tagNames` TEXT,
                        `deletedDate` INTEGER NOT NULL,
                        `deletedReason` TEXT,
                        `canRestore` INTEGER NOT NULL,
                        `hasWarranty` INTEGER NOT NULL,
                        `hasBorrowRecord` INTEGER NOT NULL
                    )
                """)
                
                // 创建索引优化查询性能
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_deleted_items_deletedDate` ON `deleted_items` (`deletedDate`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_deleted_items_category` ON `deleted_items` (`category`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_deleted_items_canRestore` ON `deleted_items` (`canRestore`)")
            }
        }
        
        /**
         * 数据库版本16到17的迁移
         * 添加用户资料功能，创建user_profile表
         */
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建用户资料表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_profile` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `nickname` TEXT NOT NULL DEFAULT '物品管家用户',
                        `avatarUri` TEXT,
                        `joinDate` INTEGER NOT NULL,
                        `totalItemsManaged` INTEGER NOT NULL DEFAULT 0,
                        `currentItemCount` INTEGER NOT NULL DEFAULT 0,
                        `expiredItemsAvoided` INTEGER NOT NULL DEFAULT 0,
                        `totalSavedValue` REAL NOT NULL DEFAULT 0.0,
                        `consecutiveDays` INTEGER NOT NULL DEFAULT 0,
                        `lastActiveDate` INTEGER NOT NULL,
                        `achievementLevel` INTEGER NOT NULL DEFAULT 1,
                        `experiencePoints` INTEGER NOT NULL DEFAULT 0,
                        `unlockedBadges` TEXT NOT NULL DEFAULT '',
                        `preferredTheme` TEXT NOT NULL DEFAULT 'AUTO',
                        `preferredLanguage` TEXT NOT NULL DEFAULT 'zh',
                        `enableNotifications` INTEGER NOT NULL DEFAULT 1,
                        `enableSoundEffects` INTEGER NOT NULL DEFAULT 1,
                        `defaultCategory` TEXT,
                        `defaultUnit` TEXT NOT NULL DEFAULT '个',
                        `enableAppLock` INTEGER NOT NULL DEFAULT 0,
                        `lockType` TEXT NOT NULL DEFAULT 'NONE',
                        `showStatsInProfile` INTEGER NOT NULL DEFAULT 1,
                        `dataBackupEnabled` INTEGER NOT NULL DEFAULT 1,
                        `autoBackupFreq` TEXT NOT NULL DEFAULT 'WEEKLY',
                        `reminderFreq` TEXT NOT NULL DEFAULT 'DAILY',
                        `compactModeEnabled` INTEGER NOT NULL DEFAULT 0,
                        `showTutorialTips` INTEGER NOT NULL DEFAULT 1,
                        `createdDate` INTEGER NOT NULL,
                        `updatedDate` INTEGER NOT NULL,
                        `appVersion` TEXT NOT NULL DEFAULT '1.0.0',
                        `profileVersion` INTEGER NOT NULL DEFAULT 1
                    )
                """)
                
                // 创建默认用户资料
                val currentTime = System.currentTimeMillis()
                database.execSQL("""
                    INSERT INTO user_profile (
                        id, nickname, joinDate, lastActiveDate, createdDate, updatedDate
                    ) VALUES (
                        1, '物品管家用户', $currentTime, $currentTime, $currentTime, $currentTime
                    )
                """)
            }
        }
        
        /**
         * 数据库版本17到18的迁移
         * 修复deleted_items表的默认值问题
         */
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 重新创建deleted_items表，移除默认值设置
                // 先备份现有数据（如果存在）
                database.execSQL("DROP TABLE IF EXISTS `deleted_items_temp`")
                
                // 创建正确的表结构
                database.execSQL("""
                    CREATE TABLE `deleted_items_temp` (
                        `originalId` INTEGER PRIMARY KEY NOT NULL,
                        `name` TEXT NOT NULL,
                        `brand` TEXT,
                        `specification` TEXT,
                        `category` TEXT NOT NULL,
                        `quantity` REAL,
                        `unit` TEXT,
                        `price` REAL,
                        `purchaseDate` INTEGER,
                        `expirationDate` INTEGER,
                        `warrantyEndDate` INTEGER,
                        `customNote` TEXT,
                        `addDate` INTEGER NOT NULL,
                        `locationId` INTEGER,
                        `locationArea` TEXT,
                        `locationContainer` TEXT,
                        `locationSublocation` TEXT,
                        `photoUris` TEXT,
                        `tagNames` TEXT,
                        `deletedDate` INTEGER NOT NULL,
                        `deletedReason` TEXT,
                        `canRestore` INTEGER NOT NULL,
                        `hasWarranty` INTEGER NOT NULL,
                        `hasBorrowRecord` INTEGER NOT NULL
                    )
                """)
                
                // 复制现有数据（如果表存在）
                database.execSQL("""
                    INSERT OR IGNORE INTO `deleted_items_temp` 
                    SELECT * FROM `deleted_items`
                    WHERE EXISTS (SELECT name FROM sqlite_master WHERE type='table' AND name='deleted_items')
                """)
                
                // 删除旧表并重命名新表
                database.execSQL("DROP TABLE IF EXISTS `deleted_items`")
                database.execSQL("ALTER TABLE `deleted_items_temp` RENAME TO `deleted_items`")
                
                // 重新创建索引
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_deleted_items_deletedDate` ON `deleted_items` (`deletedDate`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_deleted_items_category` ON `deleted_items` (`category`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_deleted_items_canRestore` ON `deleted_items` (`canRestore`)")
            }
        }
        
        /**
         * 数据库版本18到19的迁移
         * 完全重建deleted_items表以解决结构验证问题
         */
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 完全删除旧的deleted_items表
                database.execSQL("DROP TABLE IF EXISTS `deleted_items`")
                
                // 重新创建deleted_items表，严格按照Entity定义
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `deleted_items` (
                        `originalId` INTEGER PRIMARY KEY NOT NULL,
                        `name` TEXT NOT NULL,
                        `brand` TEXT,
                        `specification` TEXT,
                        `category` TEXT NOT NULL,
                        `quantity` REAL,
                        `unit` TEXT,
                        `price` REAL,
                        `purchaseDate` INTEGER,
                        `expirationDate` INTEGER,
                        `warrantyEndDate` INTEGER,
                        `customNote` TEXT,
                        `addDate` INTEGER NOT NULL,
                        `locationId` INTEGER,
                        `locationArea` TEXT,
                        `locationContainer` TEXT,
                        `locationSublocation` TEXT,
                        `photoUris` TEXT,
                        `tagNames` TEXT,
                        `deletedDate` INTEGER NOT NULL,
                        `deletedReason` TEXT,
                        `canRestore` INTEGER NOT NULL,
                        `hasWarranty` INTEGER NOT NULL,
                        `hasBorrowRecord` INTEGER NOT NULL
                    )
                """)
            }
        }
    }
} 