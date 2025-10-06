package com.example.itemmanagement.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
// import com.example.itemmanagement.data.dao.ItemDao // 已归档
import com.example.itemmanagement.data.dao.LocationDao
import com.example.itemmanagement.data.dao.TagDao
import com.example.itemmanagement.data.dao.PhotoDao
import com.example.itemmanagement.data.dao.CalendarEventDao
// import com.example.itemmanagement.data.dao.ShoppingDao // 已归档
import com.example.itemmanagement.data.dao.ShoppingListDao
import com.example.itemmanagement.data.dao.ReminderSettingsDao
import com.example.itemmanagement.data.dao.CategoryThresholdDao
import com.example.itemmanagement.data.dao.CustomRuleDao
import com.example.itemmanagement.data.dao.WarrantyDao
import com.example.itemmanagement.data.dao.BorrowDao
// import com.example.itemmanagement.data.dao.RecycleBinDao // 已归档
import com.example.itemmanagement.data.dao.UserProfileDao
// import com.example.itemmanagement.data.dao.wishlist.WishlistDao // 已归档
// import com.example.itemmanagement.data.dao.wishlist.WishlistPriceHistoryDao // 已删除
import com.example.itemmanagement.data.dao.PriceRecordDao
import com.example.itemmanagement.data.entity.*
// import com.example.itemmanagement.data.entity.wishlist.WishlistItemEntity // 已归档
// import com.example.itemmanagement.data.entity.wishlist.WishlistPriceHistoryEntity // 已删除
import com.example.itemmanagement.data.entity.unified.*
import com.example.itemmanagement.data.dao.unified.*
import com.example.itemmanagement.data.migration.UnifiedSchemaMigration

@Database(
    entities = [
        // === 新的统一架构 ===
        UnifiedItemEntity::class,
        ItemStateEntity::class,
        ShoppingDetailEntity::class,
        InventoryDetailEntity::class,
        
        // === 保留的组件表 ===
        LocationEntity::class,
        PhotoEntity::class,
        TagEntity::class,
        ItemTagCrossRef::class,
        CalendarEventEntity::class,
        ShoppingListEntity::class,
        ReminderSettingsEntity::class,
        CategoryThresholdEntity::class,
        CustomRuleEntity::class,
        WarrantyEntity::class,
        BorrowEntity::class,
        UserProfileEntity::class,
        PriceRecord::class,
        
        // === 旧表（已归档到archived文件夹） ===
        // ItemEntity::class,
        // ShoppingItemEntity::class,
        // DeletedItemEntity::class,
        // WishlistItemEntity::class
    ],
    version = 40,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    // === 新的统一架构Dao ===
    abstract fun unifiedItemDao(): UnifiedItemDao
    abstract fun itemStateDao(): ItemStateDao
    abstract fun shoppingDetailDao(): ShoppingDetailDao
    abstract fun inventoryDetailDao(): InventoryDetailDao
    
    // === 保留的组件Dao ===
    abstract fun locationDao(): LocationDao
    abstract fun tagDao(): TagDao
    abstract fun photoDao(): PhotoDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun reminderSettingsDao(): ReminderSettingsDao
    abstract fun categoryThresholdDao(): CategoryThresholdDao
    abstract fun customRuleDao(): CustomRuleDao
    abstract fun warrantyDao(): WarrantyDao
    abstract fun borrowDao(): BorrowDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun priceRecordDao(): PriceRecordDao
    
    // === 旧Dao（已归档到archived文件夹） ===
    // abstract fun itemDao(): ItemDao
    // abstract fun shoppingDao(): ShoppingDao
    // abstract fun recycleBinDao(): RecycleBinDao
    // abstract fun wishlistDao(): WishlistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return             INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "item_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_5, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, UnifiedSchemaMigration.MIGRATION_31_32, MIGRATION_32_33, MIGRATION_33_34, MIGRATION_34_35, MIGRATION_35_36, MIGRATION_36_37, MIGRATION_37_38, MIGRATION_38_39, MIGRATION_39_40)
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
        
        /**
         * 数据库版本19到20的迁移
         * 添加wasteDate字段到items表
         */
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 为items表添加wasteDate字段
                database.execSQL("ALTER TABLE items ADD COLUMN wasteDate INTEGER")
                
                // 为现有的浪费状态物品设置wasteDate
                // 对于已经是EXPIRED或DISCARDED状态的物品，使用addDate作为fallback
                database.execSQL("""
                    UPDATE items 
                    SET wasteDate = addDate 
                    WHERE status IN ('EXPIRED', 'DISCARDED') AND wasteDate IS NULL
                """)
            }
        }
        
        /**
         * 数据库版本20到21的迁移
         * 心愿单功能重构：创建独立的心愿单表，移除items表的isWishlistItem字段
         */
        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建心愿单物品表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `wishlist_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `subCategory` TEXT,
                        `brand` TEXT,
                        `specification` TEXT,
                        `notes` TEXT,
                        `estimatedPrice` REAL,
                        `targetPrice` REAL,
                        `priceUnit` TEXT NOT NULL DEFAULT '元',
                        `currentPrice` REAL,
                        `lowestPrice` REAL,
                        `highestPrice` REAL,
                        `priority` TEXT NOT NULL DEFAULT 'NORMAL',
                        `urgency` TEXT NOT NULL DEFAULT 'NORMAL',
                        `desiredQuantity` REAL NOT NULL DEFAULT 1.0,
                        `quantityUnit` TEXT NOT NULL DEFAULT '个',
                        `budgetLimit` REAL,
                        `preferredStore` TEXT,
                        `preferredBrand` TEXT,
                        `isPriceTrackingEnabled` INTEGER NOT NULL DEFAULT 1,
                        `priceDropThreshold` REAL,
                        `lastPriceCheck` INTEGER,
                        `priceCheckInterval` INTEGER NOT NULL DEFAULT 7,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `isPaused` INTEGER NOT NULL DEFAULT 0,
                        `createdDate` INTEGER NOT NULL,
                        `lastModified` INTEGER NOT NULL,
                        `achievedDate` INTEGER,
                        `sourceUrl` TEXT,
                        `imageUrl` TEXT,
                        `relatedItemId` INTEGER,
                        `addedReason` TEXT,
                        `viewCount` INTEGER NOT NULL DEFAULT 0,
                        `lastViewDate` INTEGER,
                        `priceChangeCount` INTEGER NOT NULL DEFAULT 0
                    )
                """)
                
                // 创建心愿单价格历史表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `wishlist_price_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `wishlistItemId` INTEGER NOT NULL,
                        `price` REAL NOT NULL,
                        `priceUnit` TEXT NOT NULL DEFAULT '元',
                        `currency` TEXT NOT NULL DEFAULT 'CNY',
                        `source` TEXT NOT NULL,
                        `sourceUrl` TEXT,
                        `storeName` TEXT,
                        `platform` TEXT,
                        `recordDate` INTEGER NOT NULL,
                        `isManual` INTEGER NOT NULL DEFAULT 0,
                        `isPromotional` INTEGER NOT NULL DEFAULT 0,
                        `promotionalInfo` TEXT,
                        `previousPrice` REAL,
                        `priceChange` REAL,
                        `changePercentage` REAL,
                        `confidence` REAL NOT NULL DEFAULT 1.0,
                        `verificationStatus` TEXT NOT NULL DEFAULT 'UNVERIFIED',
                        `notes` TEXT,
                        FOREIGN KEY(`wishlistItemId`) REFERENCES `wishlist_items`(`id`) ON DELETE CASCADE
                    )
                """)
                
                // 创建索引以提高查询性能
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_wishlist_items_category` ON `wishlist_items` (`category`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_wishlist_items_priority` ON `wishlist_items` (`priority`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_wishlist_items_isActive` ON `wishlist_items` (`isActive`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_wishlist_items_createdDate` ON `wishlist_items` (`createdDate`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_wishlist_price_history_wishlistItemId` ON `wishlist_price_history` (`wishlistItemId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_wishlist_price_history_recordDate` ON `wishlist_price_history` (`recordDate`)")
                
                // 渐进式迁移：创建心愿单表但保留isWishlistItem字段
                // 这样用户可以继续使用原有的心愿单功能，同时可以逐步迁移到新功能
                
                // 迁移现有的心愿单数据（从items表的isWishlistItem=1的记录）
                val currentTime = System.currentTimeMillis()
                database.execSQL("""
                    INSERT INTO wishlist_items (
                        name, category, subCategory, brand, specification, 
                        estimatedPrice, desiredQuantity, quantityUnit,
                        createdDate, lastModified, addedReason
                    )
                    SELECT 
                        name, category, subCategory, brand, specification,
                        price, quantity, unit,
                        addDate, $currentTime, 'migrated_from_inventory'
                    FROM items 
                    WHERE isWishlistItem = 1
                """)
                
                // 为迁移的物品创建初始价格历史记录
                database.execSQL("""
                    INSERT INTO wishlist_price_history (
                        wishlistItemId, price, source, recordDate, isManual, notes
                    )
                    SELECT 
                        w.id, i.price, 'migration_initial', i.addDate, 1, '从库存迁移的初始价格'
                    FROM wishlist_items w
                    JOIN items i ON (w.name = i.name AND w.category = i.category AND i.isWishlistItem = 1)
                    WHERE i.price IS NOT NULL
                """)
                
                // 添加备注说明已有新的心愿单功能
                database.execSQL("""
                    UPDATE items 
                    SET customNote = CASE 
                        WHEN customNote IS NULL OR customNote = '' THEN 
                            '已同步到新心愿单功能'
                        ELSE 
                            customNote || ' (已同步到新心愿单功能)'
                    END
                    WHERE isWishlistItem = 1
                """)
                
                // 注意：保留isWishlistItem字段和数据，实现渐进式迁移
                // 用户可以继续使用原有功能，同时可以尝试新的心愿单功能
            }
        }
        
        // 修复迁移问题：确保数据库结构一致性
        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 这个迁移主要是为了修复之前的迁移不一致问题
                // 由于 MIGRATION_20_21 已经处理了 isWishlistItem 字段的移除
                // 这里我们只需要确保表结构是正确的
                
                // 检查并确保所有必要的索引存在
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_items_locationId` ON `items` (`locationId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_items_category` ON `items` (`category`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_items_status` ON `items` (`status`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_items_expirationDate` ON `items` (`expirationDate`)")
                
                // 确保 wishlist 相关表和索引存在
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_wishlist_items_category` ON `wishlist_items` (`category`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_wishlist_items_priority` ON `wishlist_items` (`priority`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_wishlist_items_isActive` ON `wishlist_items` (`isActive`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_wishlist_items_createdDate` ON `wishlist_items` (`createdDate`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_wishlist_price_history_wishlistItemId` ON `wishlist_price_history` (`wishlistItemId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_wishlist_price_history_recordDate` ON `wishlist_price_history` (`recordDate`)")
            }
        }
        
        // 修复 isWishlistItem 字段默认值不匹配问题
        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 修复 isWishlistItem 和 isHighTurnover 字段的默认值定义
                // 重新创建 items 表以确保字段定义完全匹配 Entity
                
                // 1. 创建新的 items 表，确保所有字段定义与 Entity 完全一致
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `items_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `quantity` REAL NOT NULL,
                        `unit` TEXT NOT NULL,
                        `locationId` INTEGER,
                        `category` TEXT NOT NULL,
                        `addDate` INTEGER NOT NULL,
                        `productionDate` INTEGER,
                        `expirationDate` INTEGER,
                        `openStatus` TEXT,
                        `openDate` INTEGER,
                        `brand` TEXT,
                        `specification` TEXT,
                        `status` TEXT NOT NULL,
                        `stockWarningThreshold` INTEGER,
                        `price` REAL,
                        `priceUnit` TEXT,
                        `purchaseChannel` TEXT,
                        `storeName` TEXT,
                        `subCategory` TEXT,
                        `customNote` TEXT,
                        `season` TEXT,
                        `capacity` REAL,
                        `capacityUnit` TEXT,
                        `rating` REAL,
                        `totalPrice` REAL,
                        `totalPriceUnit` TEXT,
                        `purchaseDate` INTEGER,
                        `shelfLife` INTEGER,
                        `warrantyPeriod` INTEGER,
                        `warrantyEndDate` INTEGER,
                        `serialNumber` TEXT,
                        `isWishlistItem` INTEGER NOT NULL DEFAULT 0,
                        `isHighTurnover` INTEGER NOT NULL DEFAULT 0,
                        `wasteDate` INTEGER,
                        FOREIGN KEY(`locationId`) REFERENCES `locations`(`id`) ON DELETE SET NULL
                    )
                """)
                
                // 2. 复制数据，确保 Boolean 字段正确转换
                database.execSQL("""
                    INSERT INTO items_new SELECT 
                        id, name, quantity, unit, locationId, category, addDate,
                        productionDate, expirationDate, openStatus, openDate,
                        brand, specification, status, stockWarningThreshold,
                        price, priceUnit, purchaseChannel, storeName, subCategory,
                        customNote, season, capacity, capacityUnit, rating,
                        totalPrice, totalPriceUnit, purchaseDate, shelfLife,
                        warrantyPeriod, warrantyEndDate, serialNumber,
                        COALESCE(isWishlistItem, 0), COALESCE(isHighTurnover, 0), wasteDate
                    FROM items
                """)
                
                // 3. 删除旧表并重命名新表
                database.execSQL("DROP TABLE items")
                database.execSQL("ALTER TABLE items_new RENAME TO items")
                
                // 4. 重新创建所有索引
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_items_locationId` ON `items` (`locationId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_items_category` ON `items` (`category`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_items_status` ON `items` (`status`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_items_expirationDate` ON `items` (`expirationDate`)")
            }
        }
        
        // 清理旧心愿单系统 - 删除 isWishlistItem 字段
        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 删除 isWishlistItem 字段，完全迁移到新心愿单系统
                // 重新创建 items 表，移除 isWishlistItem 字段
                
                // 1. 创建新的 items 表（不包含 isWishlistItem）
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `items_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `quantity` REAL NOT NULL,
                        `unit` TEXT NOT NULL,
                        `locationId` INTEGER,
                        `category` TEXT NOT NULL,
                        `addDate` INTEGER NOT NULL,
                        `productionDate` INTEGER,
                        `expirationDate` INTEGER,
                        `openStatus` TEXT,
                        `openDate` INTEGER,
                        `brand` TEXT,
                        `specification` TEXT,
                        `status` TEXT NOT NULL,
                        `stockWarningThreshold` INTEGER,
                        `price` REAL,
                        `priceUnit` TEXT,
                        `purchaseChannel` TEXT,
                        `storeName` TEXT,
                        `subCategory` TEXT,
                        `customNote` TEXT,
                        `season` TEXT,
                        `capacity` REAL,
                        `capacityUnit` TEXT,
                        `rating` REAL,
                        `totalPrice` REAL,
                        `totalPriceUnit` TEXT,
                        `purchaseDate` INTEGER,
                        `shelfLife` INTEGER,
                        `warrantyPeriod` INTEGER,
                        `warrantyEndDate` INTEGER,
                        `serialNumber` TEXT,
                        `isHighTurnover` INTEGER NOT NULL DEFAULT 0,
                        `wasteDate` INTEGER,
                        FOREIGN KEY(`locationId`) REFERENCES `locations`(`id`) ON DELETE SET NULL
                    )
                """)
                
                // 2. 复制数据（排除 isWishlistItem 字段）
                database.execSQL("""
                    INSERT INTO items_new SELECT 
                        id, name, quantity, unit, locationId, category, addDate,
                        productionDate, expirationDate, openStatus, openDate,
                        brand, specification, status, stockWarningThreshold,
                        price, priceUnit, purchaseChannel, storeName, subCategory,
                        customNote, season, capacity, capacityUnit, rating,
                        totalPrice, totalPriceUnit, purchaseDate, shelfLife,
                        warrantyPeriod, warrantyEndDate, serialNumber,
                        COALESCE(isHighTurnover, 0), wasteDate
                    FROM items
                """)
                
                // 3. 删除旧表并重命名新表
                database.execSQL("DROP TABLE items")
                database.execSQL("ALTER TABLE items_new RENAME TO items")
                
                // 4. 重新创建所有索引
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_items_locationId` ON `items` (`locationId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_items_category` ON `items` (`category`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_items_status` ON `items` (`status`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_items_expirationDate` ON `items` (`expirationDate`)")
            }
        }
        
        // 修复表结构与Entity定义不匹配的问题
        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 重新创建 items 表，确保与 ItemEntity 定义完全匹配
                // 解决 Room 验证失败的问题
                
                // 1. 创建新的 items 表（与 ItemEntity 定义匹配）
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `items_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `quantity` REAL NOT NULL,
                        `unit` TEXT NOT NULL,
                        `locationId` INTEGER,
                        `category` TEXT NOT NULL,
                        `addDate` INTEGER NOT NULL,
                        `productionDate` INTEGER,
                        `expirationDate` INTEGER,
                        `openStatus` TEXT,
                        `openDate` INTEGER,
                        `brand` TEXT,
                        `specification` TEXT,
                        `status` TEXT NOT NULL,
                        `stockWarningThreshold` INTEGER,
                        `price` REAL,
                        `priceUnit` TEXT,
                        `purchaseChannel` TEXT,
                        `storeName` TEXT,
                        `subCategory` TEXT,
                        `customNote` TEXT,
                        `season` TEXT,
                        `capacity` REAL,
                        `capacityUnit` TEXT,
                        `rating` REAL,
                        `totalPrice` REAL,
                        `totalPriceUnit` TEXT,
                        `purchaseDate` INTEGER,
                        `shelfLife` INTEGER,
                        `warrantyPeriod` INTEGER,
                        `warrantyEndDate` INTEGER,
                        `serialNumber` TEXT,
                        `isHighTurnover` INTEGER NOT NULL,
                        `wasteDate` INTEGER,
                        FOREIGN KEY(`locationId`) REFERENCES `locations`(`id`) ON DELETE SET NULL
                    )
                """)

                // 2. 复制数据（不设置默认值，让Room处理）
                database.execSQL("""
                    INSERT INTO items_new SELECT
                        id, name, quantity, unit, locationId, category, addDate,
                        productionDate, expirationDate, openStatus, openDate,
                        brand, specification, status, stockWarningThreshold,
                        price, priceUnit, purchaseChannel, storeName, subCategory,
                        customNote, season, capacity, capacityUnit, rating,
                        totalPrice, totalPriceUnit, purchaseDate, shelfLife,
                        warrantyPeriod, warrantyEndDate, serialNumber,
                        COALESCE(isHighTurnover, 0), wasteDate
                    FROM items
                """)

                // 3. 删除旧表并重命名新表
                database.execSQL("DROP TABLE items")
                database.execSQL("ALTER TABLE items_new RENAME TO items")

                // 4. 创建与 ItemEntity 定义匹配的索引
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_items_locationId` ON `items` (`locationId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_items_category` ON `items` (`category`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_items_status` ON `items` (`status`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_items_expirationDate` ON `items` (`expirationDate`)")
            }
        }

        // 从版本25到26：wasteDate字段已在MIGRATION_24_25中添加，此迁移仅用于版本同步
        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // wasteDate字段已在MIGRATION_24_25中添加，无需额外操作
                // 此迁移仅用于同步版本号与Entity定义
            }
        }

        // 从版本26到27：修复MIGRATION_24_25中缺失的isWishlistItem字段
        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // isWishlistItem字段已在修复的MIGRATION_24_25中添加，无需额外操作
                // 此迁移仅用于同步版本号与修复后的Entity定义
            }
        }

        // 从版本27到28：移除isWishlistItem字段，完全使用独立心愿单系统
        val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // isWishlistItem字段已从MIGRATION_24_25中移除，无需额外操作
                // 此迁移仅用于同步版本号与移除isWishlistItem后的Entity定义
            }
        }

        // 从版本28到29：同步ItemEntity索引定义与MIGRATION_24_25创建的索引
        val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 索引已在MIGRATION_24_25中创建，无需额外操作
                // 此迁移仅用于同步版本号与ItemEntity的索引定义
            }
        }

        // 从版本29到30：统一wishlist_items表字段名
        val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 重新创建 wishlist_items 表，统一字段名
                // 1. 创建新的 wishlist_items 表（使用统一的字段名）
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `wishlist_items_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `subCategory` TEXT,
                        `brand` TEXT,
                        `specification` TEXT,
                        `notes` TEXT,
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
                        `preferredStore` TEXT,
                        `preferredBrand` TEXT,
                        `isPriceTrackingEnabled` INTEGER NOT NULL,
                        `priceDropThreshold` REAL,
                        `lastPriceCheck` INTEGER,
                        `priceCheckInterval` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `isPaused` INTEGER NOT NULL,
                        `createdDate` INTEGER NOT NULL,
                        `lastModified` INTEGER NOT NULL,
                        `achievedDate` INTEGER,
                        `sourceUrl` TEXT,
                        `imageUrl` TEXT,
                        `relatedItemId` INTEGER,
                        `addedReason` TEXT,
                        `viewCount` INTEGER NOT NULL,
                        `lastViewDate` INTEGER,
                        `priceChangeCount` INTEGER NOT NULL
                    )
                """)

                // 2. 复制数据（重命名字段）
                database.execSQL("""
                    INSERT INTO wishlist_items_new SELECT
                        id, name, category, subCategory, brand, specification, notes,
                        estimatedPrice, targetPrice, priceUnit, currentPrice, lowestPrice, highestPrice,
                        priority, urgency, desiredQuantity, quantityUnit, budgetLimit, preferredStore, preferredBrand,
                        isPriceTrackingEnabled, priceDropThreshold, lastPriceCheck, priceCheckInterval,
                        isActive, isPaused, createdDate, lastModified, achievedDate, sourceUrl, imageUrl,
                        relatedItemId, addedReason, viewCount, lastViewDate, priceChangeCount
                    FROM wishlist_items
                """)

                // 3. 删除旧表并重命名新表
                database.execSQL("DROP TABLE wishlist_items")
                database.execSQL("ALTER TABLE wishlist_items_new RENAME TO wishlist_items")
            }
        }

        val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 统一Entity字段命名：以ItemEntity为标准，其他Entity向它看齐
                
                // 1. 重命名 shopping_items 表的字段（向ItemEntity看齐）
                database.execSQL("ALTER TABLE shopping_items RENAME COLUMN estimatedPrice TO price")
                database.execSQL("ALTER TABLE shopping_items RENAME COLUMN createdDate TO addDate")
                
                // 2. 重命名 wishlist_items 表的字段（向ItemEntity看齐）
                database.execSQL("ALTER TABLE wishlist_items RENAME COLUMN preferredStore TO purchaseChannel")
                database.execSQL("ALTER TABLE wishlist_items RENAME COLUMN createdDate TO addDate")
                database.execSQL("ALTER TABLE wishlist_items RENAME COLUMN notes TO customNote")
            }
        }
        
        /**
         * 迁移 32 → 33：DetailEntity主键优化
         * 将 itemId 主键改为自增 id 主键，支持同一物品的多次历史记录
         */
        private val MIGRATION_32_33 = object : Migration(32, 33) {
            override fun migrate(database: SupportSQLiteDatabase) {
                android.util.Log.d("Migration", "开始迁移 32 → 33: DetailEntity主键优化")
                
                // 1. 重建 shopping_details 表（添加自增主键）
                database.execSQL("""
                    CREATE TABLE shopping_details_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        itemId INTEGER NOT NULL,
                        shoppingListId INTEGER NOT NULL,
                        quantity REAL NOT NULL,
                        quantityUnit TEXT NOT NULL DEFAULT '个',
                        estimatedPrice REAL,
                        actualPrice REAL,
                        priceUnit TEXT NOT NULL DEFAULT '元',
                        budgetLimit REAL,
                        totalPrice REAL,
                        totalPriceUnit TEXT,
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
                        serialNumber TEXT,
                        sourceItemId INTEGER,
                        recommendationReason TEXT,
                        addedReason TEXT NOT NULL DEFAULT 'USER_MANUAL',
                        addDate INTEGER NOT NULL,
                        completedDate INTEGER,
                        remindDate INTEGER,
                        isRecurring INTEGER NOT NULL DEFAULT 0,
                        recurringInterval INTEGER,
                        tags TEXT,
                        FOREIGN KEY(itemId) REFERENCES unified_items(id) ON DELETE CASCADE,
                        FOREIGN KEY(shoppingListId) REFERENCES shopping_lists(id) ON DELETE CASCADE
                    )
                """)
                
                // 复制数据（只复制活跃的购物物品，保留历史数据）
                database.execSQL("""
                    INSERT INTO shopping_details_new 
                    (itemId, shoppingListId, quantity, quantityUnit, estimatedPrice, actualPrice, 
                     priceUnit, budgetLimit, totalPrice, totalPriceUnit, purchaseChannel, storeName, 
                     preferredStore, purchaseDate, isPurchased, priority, urgencyLevel, deadline,
                     capacity, capacityUnit, rating, season, serialNumber, sourceItemId, 
                     recommendationReason, addedReason, addDate, completedDate, remindDate, 
                     isRecurring, recurringInterval, tags)
                    SELECT itemId, shoppingListId, quantity, quantityUnit, estimatedPrice, actualPrice,
                           priceUnit, budgetLimit, totalPrice, totalPriceUnit, purchaseChannel, storeName,
                           preferredStore, purchaseDate, isPurchased, priority, urgencyLevel, deadline,
                           capacity, capacityUnit, rating, season, serialNumber, sourceItemId,
                           recommendationReason, addedReason, addDate, completedDate, remindDate,
                           isRecurring, recurringInterval, tags
                    FROM shopping_details
                """)
                
                database.execSQL("DROP TABLE shopping_details")
                database.execSQL("ALTER TABLE shopping_details_new RENAME TO shopping_details")
                
                // 重建索引
                database.execSQL("CREATE INDEX index_shopping_details_itemId ON shopping_details(itemId)")
                database.execSQL("CREATE INDEX index_shopping_details_shoppingListId ON shopping_details(shoppingListId)")
                database.execSQL("CREATE INDEX index_shopping_details_priority ON shopping_details(priority)")
                database.execSQL("CREATE INDEX index_shopping_details_urgencyLevel ON shopping_details(urgencyLevel)")
                database.execSQL("CREATE INDEX index_shopping_details_isPurchased ON shopping_details(isPurchased)")
                database.execSQL("CREATE INDEX index_shopping_details_deadline ON shopping_details(deadline)")
                database.execSQL("CREATE INDEX index_shopping_details_shoppingListId_isPurchased ON shopping_details(shoppingListId, isPurchased)")
                
                android.util.Log.d("Migration", "✓ shopping_details 表重建完成")
                
                // 2. 重建 inventory_details 表（添加自增主键）
                database.execSQL("""
                    CREATE TABLE inventory_details_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        itemId INTEGER NOT NULL,
                        quantity REAL NOT NULL,
                        unit TEXT NOT NULL,
                        locationId INTEGER,
                        productionDate INTEGER,
                        expirationDate INTEGER,
                        openDate INTEGER,
                        purchaseDate INTEGER,
                        wasteDate INTEGER,
                        openStatus TEXT,
                        status TEXT NOT NULL DEFAULT 'IN_STOCK',
                        stockWarningThreshold INTEGER,
                        isHighTurnover INTEGER NOT NULL DEFAULT 0,
                        price REAL,
                        priceUnit TEXT,
                        totalPrice REAL,
                        totalPriceUnit TEXT,
                        purchaseChannel TEXT,
                        storeName TEXT,
                        capacity REAL,
                        capacityUnit TEXT,
                        rating REAL,
                        season TEXT,
                        serialNumber TEXT,
                        shelfLife INTEGER,
                        warrantyPeriod INTEGER,
                        warrantyEndDate INTEGER,
                        createdDate INTEGER NOT NULL,
                        updatedDate INTEGER NOT NULL,
                        FOREIGN KEY(itemId) REFERENCES unified_items(id) ON DELETE CASCADE,
                        FOREIGN KEY(locationId) REFERENCES locations(id) ON DELETE SET NULL
                    )
                """)
                
                // 复制数据（只复制活跃的库存物品）
                database.execSQL("""
                    INSERT INTO inventory_details_new 
                    (itemId, quantity, unit, locationId, productionDate, expirationDate, openDate,
                     purchaseDate, wasteDate, openStatus, status, stockWarningThreshold, isHighTurnover,
                     price, priceUnit, totalPrice, totalPriceUnit, purchaseChannel, storeName,
                     capacity, capacityUnit, rating, season, serialNumber, shelfLife, warrantyPeriod,
                     warrantyEndDate, createdDate, updatedDate)
                    SELECT itemId, quantity, unit, locationId, productionDate, expirationDate, openDate,
                           purchaseDate, wasteDate, openStatus, status, stockWarningThreshold, isHighTurnover,
                           price, priceUnit, totalPrice, totalPriceUnit, purchaseChannel, storeName,
                           capacity, capacityUnit, rating, season, serialNumber, shelfLife, warrantyPeriod,
                           warrantyEndDate, createdDate, updatedDate
                    FROM inventory_details
                """)
                
                database.execSQL("DROP TABLE inventory_details")
                database.execSQL("ALTER TABLE inventory_details_new RENAME TO inventory_details")
                
                // 重建索引
                database.execSQL("CREATE INDEX index_inventory_details_itemId ON inventory_details(itemId)")
                database.execSQL("CREATE INDEX index_inventory_details_locationId ON inventory_details(locationId)")
                database.execSQL("CREATE INDEX index_inventory_details_status ON inventory_details(status)")
                database.execSQL("CREATE INDEX index_inventory_details_expirationDate ON inventory_details(expirationDate)")
                database.execSQL("CREATE INDEX index_inventory_details_stockWarningThreshold ON inventory_details(stockWarningThreshold)")
                database.execSQL("CREATE INDEX index_inventory_details_status_expirationDate ON inventory_details(status, expirationDate)")
                
                android.util.Log.d("Migration", "✓ inventory_details 表重建完成")
                
                // 3. 重建 wishlist_details 表（添加自增主键）
                // 注意：必须与 WishlistDetailEntity 的字段定义完全匹配
                database.execSQL("""
                    CREATE TABLE wishlist_details_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        itemId INTEGER NOT NULL,
                        price REAL,
                        targetPrice REAL,
                        priceUnit TEXT NOT NULL DEFAULT '元',
                        currentPrice REAL,
                        lowestPrice REAL,
                        highestPrice REAL,
                        priority TEXT NOT NULL DEFAULT 'NORMAL',
                        urgency TEXT NOT NULL DEFAULT 'NORMAL',
                        quantity REAL NOT NULL DEFAULT 1.0,
                        quantityUnit TEXT NOT NULL DEFAULT '个',
                        budgetLimit REAL,
                        purchaseChannel TEXT,
                        preferredBrand TEXT,
                        isPriceTrackingEnabled INTEGER NOT NULL DEFAULT 1,
                        priceDropThreshold REAL,
                        lastPriceCheck INTEGER,
                        priceCheckInterval INTEGER NOT NULL DEFAULT 7,
                        isPaused INTEGER NOT NULL DEFAULT 0,
                        achievedDate INTEGER,
                        sourceUrl TEXT,
                        imageUrl TEXT,
                        relatedInventoryItemId INTEGER,
                        addedReason TEXT,
                        viewCount INTEGER NOT NULL DEFAULT 0,
                        lastViewDate INTEGER,
                        priceChangeCount INTEGER NOT NULL DEFAULT 0,
                        createdDate INTEGER NOT NULL,
                        lastModified INTEGER NOT NULL,
                        FOREIGN KEY(itemId) REFERENCES unified_items(id) ON DELETE CASCADE
                    )
                """)
                
                // 复制数据（旧表的 itemId 作为主键，新表使用自增 id）
                // 版本32的字段都存在，直接全部复制
                database.execSQL("""
                    INSERT INTO wishlist_details_new 
                    (itemId, price, targetPrice, priceUnit, currentPrice, lowestPrice, highestPrice,
                     priority, urgency, quantity, quantityUnit, budgetLimit, purchaseChannel, 
                     preferredBrand, isPriceTrackingEnabled, priceDropThreshold, lastPriceCheck,
                     priceCheckInterval, isPaused, achievedDate, sourceUrl, imageUrl,
                     relatedInventoryItemId, addedReason, viewCount, lastViewDate, 
                     priceChangeCount, createdDate, lastModified)
                    SELECT itemId, price, targetPrice, priceUnit, currentPrice, lowestPrice, highestPrice,
                           priority, urgency, quantity, quantityUnit, budgetLimit, purchaseChannel,
                           preferredBrand, isPriceTrackingEnabled, priceDropThreshold, lastPriceCheck,
                           priceCheckInterval, isPaused, achievedDate, sourceUrl, imageUrl,
                           relatedInventoryItemId, addedReason, viewCount, lastViewDate,
                           priceChangeCount, createdDate, lastModified
                    FROM wishlist_details
                """)
                
                database.execSQL("DROP TABLE wishlist_details")
                database.execSQL("ALTER TABLE wishlist_details_new RENAME TO wishlist_details")
                
                // 重建索引（必须与 Entity 的 @Index 注解匹配）
                database.execSQL("CREATE UNIQUE INDEX index_wishlist_details_itemId ON wishlist_details(itemId)")
                database.execSQL("CREATE INDEX index_wishlist_details_priority ON wishlist_details(priority)")
                database.execSQL("CREATE INDEX index_wishlist_details_urgency ON wishlist_details(urgency)")
                database.execSQL("CREATE INDEX index_wishlist_details_targetPrice ON wishlist_details(targetPrice)")
                database.execSQL("CREATE INDEX index_wishlist_details_isPriceTrackingEnabled ON wishlist_details(isPriceTrackingEnabled)")
                
                android.util.Log.d("Migration", "✓ wishlist_details 表重建完成")
                android.util.Log.d("Migration", "✅ 迁移 32 → 33 完成")
            }
        }
        
        /**
         * 迁移 33 → 34：价格单位字段独立化
         * 将 shopping_details 表的 priceUnit 拆分为三个独立字段：
         * - estimatedPriceUnit（预估价格单位）
         * - actualPriceUnit（实际价格单位）
         * - budgetLimitUnit（预算上限单位）
         */
        private val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(database: SupportSQLiteDatabase) {
                android.util.Log.d("Migration", "开始迁移 33 → 34: 价格单位字段独立化")
                
                // 1. 为 shopping_details 表添加三个新的价格单位字段
                database.execSQL("ALTER TABLE shopping_details ADD COLUMN estimatedPriceUnit TEXT NOT NULL DEFAULT '元'")
                database.execSQL("ALTER TABLE shopping_details ADD COLUMN actualPriceUnit TEXT NOT NULL DEFAULT '元'")
                database.execSQL("ALTER TABLE shopping_details ADD COLUMN budgetLimitUnit TEXT NOT NULL DEFAULT '元'")
                
                // 2. 将现有的 priceUnit 值复制到新字段（保持数据一致性）
                database.execSQL("""
                    UPDATE shopping_details 
                    SET estimatedPriceUnit = COALESCE(priceUnit, '元'),
                        actualPriceUnit = COALESCE(priceUnit, '元'),
                        budgetLimitUnit = COALESCE(priceUnit, '元')
                """)
                
                // 3. 删除旧的 priceUnit 字段（需要重建表）
                database.execSQL("""
                    CREATE TABLE shopping_details_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        itemId INTEGER NOT NULL,
                        shoppingListId INTEGER NOT NULL,
                        quantity REAL NOT NULL,
                        quantityUnit TEXT NOT NULL DEFAULT '个',
                        estimatedPrice REAL,
                        estimatedPriceUnit TEXT NOT NULL DEFAULT '元',
                        actualPrice REAL,
                        actualPriceUnit TEXT NOT NULL DEFAULT '元',
                        budgetLimit REAL,
                        budgetLimitUnit TEXT NOT NULL DEFAULT '元',
                        totalPrice REAL,
                        totalPriceUnit TEXT,
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
                        serialNumber TEXT,
                        sourceItemId INTEGER,
                        recommendationReason TEXT,
                        addedReason TEXT NOT NULL DEFAULT 'USER_MANUAL',
                        addDate INTEGER NOT NULL,
                        completedDate INTEGER,
                        remindDate INTEGER,
                        isRecurring INTEGER NOT NULL DEFAULT 0,
                        recurringInterval INTEGER,
                        tags TEXT,
                        FOREIGN KEY(itemId) REFERENCES unified_items(id) ON DELETE CASCADE,
                        FOREIGN KEY(shoppingListId) REFERENCES shopping_lists(id) ON DELETE CASCADE
                    )
                """)
                
                // 4. 复制数据到新表
                database.execSQL("""
                    INSERT INTO shopping_details_new 
                    (id, itemId, shoppingListId, quantity, quantityUnit, 
                     estimatedPrice, estimatedPriceUnit, actualPrice, actualPriceUnit,
                     budgetLimit, budgetLimitUnit, totalPrice, totalPriceUnit,
                     purchaseChannel, storeName, preferredStore, purchaseDate, isPurchased,
                     priority, urgencyLevel, deadline, capacity, capacityUnit, rating,
                     season, serialNumber, sourceItemId, recommendationReason, addedReason,
                     addDate, completedDate, remindDate, isRecurring, recurringInterval, tags)
                    SELECT id, itemId, shoppingListId, quantity, quantityUnit,
                           estimatedPrice, estimatedPriceUnit, actualPrice, actualPriceUnit,
                           budgetLimit, budgetLimitUnit, totalPrice, totalPriceUnit,
                           purchaseChannel, storeName, preferredStore, purchaseDate, isPurchased,
                           priority, urgencyLevel, deadline, capacity, capacityUnit, rating,
                           season, serialNumber, sourceItemId, recommendationReason, addedReason,
                           addDate, completedDate, remindDate, isRecurring, recurringInterval, tags
                    FROM shopping_details
                """)
                
                // 5. 删除旧表并重命名新表
                database.execSQL("DROP TABLE shopping_details")
                database.execSQL("ALTER TABLE shopping_details_new RENAME TO shopping_details")
                
                // 6. 重建索引
                database.execSQL("CREATE INDEX index_shopping_details_itemId ON shopping_details(itemId)")
                database.execSQL("CREATE INDEX index_shopping_details_shoppingListId ON shopping_details(shoppingListId)")
                database.execSQL("CREATE INDEX index_shopping_details_priority ON shopping_details(priority)")
                database.execSQL("CREATE INDEX index_shopping_details_urgencyLevel ON shopping_details(urgencyLevel)")
                database.execSQL("CREATE INDEX index_shopping_details_isPurchased ON shopping_details(isPurchased)")
                database.execSQL("CREATE INDEX index_shopping_details_deadline ON shopping_details(deadline)")
                database.execSQL("CREATE INDEX index_shopping_details_shoppingListId_isPurchased ON shopping_details(shoppingListId, isPurchased)")
                
                android.util.Log.d("Migration", "✅ 迁移 33 → 34 完成")
            }
        }
        
        /**
         * 迁移 34 → 35：合并商店字段
         * 删除 preferredStore 字段，统一使用 storeName 作为购买商店字段
         */
        private val MIGRATION_34_35 = object : Migration(34, 35) {
            override fun migrate(database: SupportSQLiteDatabase) {
                android.util.Log.d("Migration", "开始迁移 34 → 35: 合并商店字段")
                
                // 1. 合并 preferredStore 和 storeName 的数据（优先使用 preferredStore 的值）
                database.execSQL("""
                    UPDATE shopping_details 
                    SET storeName = CASE 
                        WHEN preferredStore IS NOT NULL AND preferredStore != '' THEN preferredStore
                        WHEN storeName IS NOT NULL AND storeName != '' THEN storeName
                        ELSE NULL
                    END
                """)
                
                android.util.Log.d("Migration", "✓ 数据合并完成")
                
                // 2. 重建表，删除 preferredStore 字段
                database.execSQL("""
                    CREATE TABLE shopping_details_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        itemId INTEGER NOT NULL,
                        shoppingListId INTEGER NOT NULL,
                        quantity REAL NOT NULL,
                        quantityUnit TEXT NOT NULL DEFAULT '个',
                        estimatedPrice REAL,
                        estimatedPriceUnit TEXT NOT NULL DEFAULT '元',
                        actualPrice REAL,
                        actualPriceUnit TEXT NOT NULL DEFAULT '元',
                        budgetLimit REAL,
                        budgetLimitUnit TEXT NOT NULL DEFAULT '元',
                        totalPrice REAL,
                        totalPriceUnit TEXT,
                        purchaseChannel TEXT,
                        storeName TEXT,
                        purchaseDate INTEGER,
                        isPurchased INTEGER NOT NULL DEFAULT 0,
                        priority TEXT NOT NULL DEFAULT 'NORMAL',
                        urgencyLevel TEXT NOT NULL DEFAULT 'NORMAL',
                        deadline INTEGER,
                        capacity REAL,
                        capacityUnit TEXT,
                        rating REAL,
                        season TEXT,
                        serialNumber TEXT,
                        sourceItemId INTEGER,
                        recommendationReason TEXT,
                        addedReason TEXT NOT NULL DEFAULT 'USER_MANUAL',
                        addDate INTEGER NOT NULL,
                        completedDate INTEGER,
                        remindDate INTEGER,
                        isRecurring INTEGER NOT NULL DEFAULT 0,
                        recurringInterval INTEGER,
                        tags TEXT,
                        FOREIGN KEY(itemId) REFERENCES unified_items(id) ON DELETE CASCADE,
                        FOREIGN KEY(shoppingListId) REFERENCES shopping_lists(id) ON DELETE CASCADE
                    )
                """)
                
                android.util.Log.d("Migration", "✓ 新表创建完成")
                
                // 3. 复制数据到新表（不包含 preferredStore 字段）
                database.execSQL("""
                    INSERT INTO shopping_details_new 
                    (id, itemId, shoppingListId, quantity, quantityUnit, 
                     estimatedPrice, estimatedPriceUnit, actualPrice, actualPriceUnit,
                     budgetLimit, budgetLimitUnit, totalPrice, totalPriceUnit,
                     purchaseChannel, storeName, purchaseDate, isPurchased,
                     priority, urgencyLevel, deadline, capacity, capacityUnit, rating,
                     season, serialNumber, sourceItemId, recommendationReason, addedReason,
                     addDate, completedDate, remindDate, isRecurring, recurringInterval, tags)
                    SELECT id, itemId, shoppingListId, quantity, quantityUnit,
                           estimatedPrice, estimatedPriceUnit, actualPrice, actualPriceUnit,
                           budgetLimit, budgetLimitUnit, totalPrice, totalPriceUnit,
                           purchaseChannel, storeName, purchaseDate, isPurchased,
                           priority, urgencyLevel, deadline, capacity, capacityUnit, rating,
                           season, serialNumber, sourceItemId, recommendationReason, addedReason,
                           addDate, completedDate, remindDate, isRecurring, recurringInterval, tags
                    FROM shopping_details
                """)
                
                android.util.Log.d("Migration", "✓ 数据迁移完成")
                
                // 4. 删除旧表并重命名新表
                database.execSQL("DROP TABLE shopping_details")
                database.execSQL("ALTER TABLE shopping_details_new RENAME TO shopping_details")
                
                // 5. 重建索引
                database.execSQL("CREATE INDEX index_shopping_details_itemId ON shopping_details(itemId)")
                database.execSQL("CREATE INDEX index_shopping_details_shoppingListId ON shopping_details(shoppingListId)")
                database.execSQL("CREATE INDEX index_shopping_details_priority ON shopping_details(priority)")
                database.execSQL("CREATE INDEX index_shopping_details_urgencyLevel ON shopping_details(urgencyLevel)")
                database.execSQL("CREATE INDEX index_shopping_details_isPurchased ON shopping_details(isPurchased)")
                database.execSQL("CREATE INDEX index_shopping_details_deadline ON shopping_details(deadline)")
                database.execSQL("CREATE INDEX index_shopping_details_shoppingListId_isPurchased ON shopping_details(shoppingListId, isPurchased)")
                
                android.util.Log.d("Migration", "✓ 索引重建完成")
                android.util.Log.d("Migration", "✅ 迁移 34 → 35 完成：preferredStore 字段已删除，storeName 作为统一的购买商店字段")
            }
        }
        
        /**
         * 迁移 35 → 36: 修复枚举值（URGENT → CRITICAL）
         * 
         * 背景：
         * - ShoppingItemPriority 枚举从 LOW/NORMAL/HIGH/URGENT 改为 LOW/NORMAL/HIGH/CRITICAL
         * - 需要更新数据库中的旧枚举值
         */
        private val MIGRATION_35_36 = object : Migration(35, 36) {
            override fun migrate(database: SupportSQLiteDatabase) {
                android.util.Log.d("Migration", "开始迁移 35 → 36: 修复枚举值")
                
                // 1. 更新 shopping_details 表中的 priority 枚举值
                database.execSQL("""
                    UPDATE shopping_details 
                    SET priority = 'CRITICAL' 
                    WHERE priority = 'URGENT'
                """)
                
                android.util.Log.d("Migration", "✓ priority 枚举值已更新: URGENT → CRITICAL")
                
                // 2. 验证更新结果
                val cursor = database.query("SELECT COUNT(*) FROM shopping_details WHERE priority = 'URGENT'")
                if (cursor.moveToFirst()) {
                    val count = cursor.getInt(0)
                    if (count > 0) {
                        android.util.Log.w("Migration", "警告: 仍有 $count 条记录的 priority 为 URGENT")
                    } else {
                        android.util.Log.d("Migration", "✓ 验证通过: 所有 URGENT 值已更新")
                    }
                }
                cursor.close()
                
                android.util.Log.d("Migration", "✅ 迁移 35 → 36 完成：枚举值已修复")
            }
        }
        
        private val MIGRATION_36_37 = object : Migration(36, 37) {
            override fun migrate(database: SupportSQLiteDatabase) {
                android.util.Log.d("Migration", "开始迁移 36 → 37: 添加价格记录表")
                
                // 创建 price_records 表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS price_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        itemId INTEGER NOT NULL,
                        recordDate INTEGER NOT NULL,
                        price REAL NOT NULL,
                        purchaseChannel TEXT NOT NULL,
                        notes TEXT
                    )
                """)
                
                android.util.Log.d("Migration", "✓ price_records 表已创建")
                
                // 创建索引以提高查询性能
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_price_records_itemId 
                    ON price_records(itemId)
                """)
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_price_records_recordDate 
                    ON price_records(recordDate)
                """)
                
                android.util.Log.d("Migration", "✓ 索引已创建")
                android.util.Log.d("Migration", "✅ 迁移 36 → 37 完成：价格记录功能已添加")
            }
        }
        
        private val MIGRATION_37_38 = object : Migration(37, 38) {
            override fun migrate(database: SupportSQLiteDatabase) {
                android.util.Log.d("Migration", "开始迁移 37 → 38: 优化物品属性字段")
                
                // === 第1步：为 unified_items 表添加物品固有属性字段 ===
                database.execSQL("ALTER TABLE unified_items ADD COLUMN capacity REAL")
                database.execSQL("ALTER TABLE unified_items ADD COLUMN capacityUnit TEXT")
                database.execSQL("ALTER TABLE unified_items ADD COLUMN rating REAL")
                database.execSQL("ALTER TABLE unified_items ADD COLUMN season TEXT")
                database.execSQL("ALTER TABLE unified_items ADD COLUMN serialNumber TEXT")
                android.util.Log.d("Migration", "✓ unified_items 新字段已添加")
                
                // === 第2步：从 inventory_details 表移除冗余字段 ===
                // 由于SQLite不支持直接DROP COLUMN，需要重建表
                database.execSQL("""
                    CREATE TABLE inventory_details_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        itemId INTEGER NOT NULL,
                        quantity REAL NOT NULL,
                        unit TEXT NOT NULL,
                        locationId INTEGER,
                        productionDate INTEGER,
                        expirationDate INTEGER,
                        openDate INTEGER,
                        purchaseDate INTEGER,
                        wasteDate INTEGER,
                        openStatus TEXT,
                        status TEXT NOT NULL,
                        stockWarningThreshold INTEGER,
                        isHighTurnover INTEGER NOT NULL,
                        price REAL,
                        priceUnit TEXT,
                        totalPrice REAL,
                        totalPriceUnit TEXT,
                        purchaseChannel TEXT,
                        storeName TEXT,
                        shelfLife INTEGER,
                        warrantyPeriod INTEGER,
                        warrantyEndDate INTEGER,
                        createdDate INTEGER NOT NULL,
                        updatedDate INTEGER NOT NULL,
                        FOREIGN KEY(itemId) REFERENCES unified_items(id) ON DELETE CASCADE,
                        FOREIGN KEY(locationId) REFERENCES locations(id) ON DELETE SET NULL
                    )
                """)
                
                // 复制数据（不包括capacity, capacityUnit, rating, season, serialNumber）
                database.execSQL("""
                    INSERT INTO inventory_details_new 
                    SELECT id, itemId, quantity, unit, locationId, productionDate, expirationDate, 
                           openDate, purchaseDate, wasteDate, openStatus, status, stockWarningThreshold,
                           isHighTurnover, price, priceUnit, totalPrice, totalPriceUnit, purchaseChannel,
                           storeName, shelfLife, warrantyPeriod, warrantyEndDate, createdDate, updatedDate
                    FROM inventory_details
                """)
                
                database.execSQL("DROP TABLE inventory_details")
                database.execSQL("ALTER TABLE inventory_details_new RENAME TO inventory_details")
                
                // 重建索引
                database.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_details_itemId ON inventory_details(itemId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_details_locationId ON inventory_details(locationId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_details_status ON inventory_details(status)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_details_expirationDate ON inventory_details(expirationDate)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_details_stockWarningThreshold ON inventory_details(stockWarningThreshold)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_details_status_expirationDate ON inventory_details(status, expirationDate)")
                android.util.Log.d("Migration", "✓ inventory_details 表已重建")
                
                // === 第3步：从 shopping_details 表移除冗余字段 ===
                database.execSQL("""
                    CREATE TABLE shopping_details_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        itemId INTEGER NOT NULL,
                        shoppingListId INTEGER NOT NULL,
                        quantity REAL NOT NULL,
                        quantityUnit TEXT NOT NULL,
                        estimatedPrice REAL,
                        estimatedPriceUnit TEXT NOT NULL,
                        actualPrice REAL,
                        actualPriceUnit TEXT NOT NULL,
                        budgetLimit REAL,
                        budgetLimitUnit TEXT NOT NULL,
                        totalPrice REAL,
                        totalPriceUnit TEXT,
                        purchaseChannel TEXT,
                        storeName TEXT,
                        purchaseDate INTEGER,
                        isPurchased INTEGER NOT NULL,
                        priority TEXT NOT NULL,
                        urgencyLevel TEXT NOT NULL,
                        deadline INTEGER,
                        sourceItemId INTEGER,
                        recommendationReason TEXT,
                        addedReason TEXT NOT NULL,
                        addDate INTEGER NOT NULL,
                        completedDate INTEGER,
                        remindDate INTEGER,
                        isRecurring INTEGER NOT NULL,
                        recurringInterval INTEGER,
                        tags TEXT,
                        FOREIGN KEY(itemId) REFERENCES unified_items(id) ON DELETE CASCADE,
                        FOREIGN KEY(shoppingListId) REFERENCES shopping_lists(id) ON DELETE CASCADE
                    )
                """)
                
                // 复制数据（不包括capacity, capacityUnit, rating, season, serialNumber）
                database.execSQL("""
                    INSERT INTO shopping_details_new 
                    SELECT id, itemId, shoppingListId, quantity, quantityUnit, estimatedPrice,
                           estimatedPriceUnit, actualPrice, actualPriceUnit, budgetLimit, budgetLimitUnit,
                           totalPrice, totalPriceUnit, purchaseChannel, storeName, purchaseDate, isPurchased,
                           priority, urgencyLevel, deadline, sourceItemId, recommendationReason, addedReason,
                           addDate, completedDate, remindDate, isRecurring, recurringInterval, tags
                    FROM shopping_details
                """)
                
                database.execSQL("DROP TABLE shopping_details")
                database.execSQL("ALTER TABLE shopping_details_new RENAME TO shopping_details")
                
                // 重建索引
                database.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_details_itemId ON shopping_details(itemId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_details_shoppingListId ON shopping_details(shoppingListId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_details_priority ON shopping_details(priority)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_details_urgencyLevel ON shopping_details(urgencyLevel)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_details_isPurchased ON shopping_details(isPurchased)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_details_deadline ON shopping_details(deadline)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_details_shoppingListId_isPurchased ON shopping_details(shoppingListId, isPurchased)")
                android.util.Log.d("Migration", "✓ shopping_details 表已重建")
                
                android.util.Log.d("Migration", "✅ 迁移 37 → 38 完成：物品属性字段已整合到基础表，冗余字段已移除")
            }
        }
        
        /**
         * 从版本38到39：删除心愿单功能
         * 移除 wishlist_details 表和相关索引
         * 移除 WISHLIST 状态类型的数据
         */
        private val MIGRATION_38_39 = object : Migration(38, 39) {
            override fun migrate(database: SupportSQLiteDatabase) {
                android.util.Log.d("Migration", "开始迁移 38 → 39: 删除心愿单功能")
                
                // 1. 删除 wishlist_details 表（如果存在）
                database.execSQL("DROP TABLE IF EXISTS wishlist_details")
                android.util.Log.d("Migration", "✓ wishlist_details 表已删除")
                
                // 2. 删除 wishlist_price_history 表（如果存在）
                database.execSQL("DROP TABLE IF EXISTS wishlist_price_history")
                android.util.Log.d("Migration", "✓ wishlist_price_history 表已删除")
                
                // 3. 删除旧的 wishlist_items 表（如果存在）
                database.execSQL("DROP TABLE IF EXISTS wishlist_items")
                android.util.Log.d("Migration", "✓ wishlist_items 表已删除")
                
                // 4. 删除 WISHLIST 状态类型的 item_states 记录
                database.execSQL("DELETE FROM item_states WHERE stateType = 'WISHLIST'")
                android.util.Log.d("Migration", "✓ WISHLIST 状态记录已删除")
                
                // 5. 删除没有任何活跃状态的孤立物品（可选，谨慎操作）
                // 这里我们保留这些物品，因为它们可能有历史价值
                
                android.util.Log.d("Migration", "✅ 迁移 38 → 39 完成：心愿单功能已完全移除")
            }
        }
        
        /**
         * 迁移 39 → 40：添加购买原因字段
         * 为 shopping_details 表添加 purchaseReason 字段
         */
        private val MIGRATION_39_40 = object : Migration(39, 40) {
            override fun migrate(database: SupportSQLiteDatabase) {
                android.util.Log.d("Migration", "开始迁移 39 → 40: 添加购买原因字段")
                
                // 添加购买原因字段到 shopping_details 表
                database.execSQL("ALTER TABLE shopping_details ADD COLUMN purchaseReason TEXT")
                android.util.Log.d("Migration", "✓ shopping_details 表已添加 purchaseReason 字段")
                
                android.util.Log.d("Migration", "✅ 迁移 39 → 40 完成：购买原因字段已添加")
            }
        }
    }
} 