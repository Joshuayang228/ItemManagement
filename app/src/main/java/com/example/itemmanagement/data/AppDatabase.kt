package com.example.itemmanagement.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.itemmanagement.data.dao.ItemDao
import com.example.itemmanagement.data.entity.*

@Database(
    entities = [
        ItemEntity::class,
        LocationEntity::class,
        PhotoEntity::class,
        TagEntity::class,
        ItemTagCrossRef::class
    ],
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_5, MIGRATION_6_7)
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
    }
} 