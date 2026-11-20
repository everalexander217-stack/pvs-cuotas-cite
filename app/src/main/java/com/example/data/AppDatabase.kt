package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        InstallmentEntity::class,
        RecentSearchEntity::class,
        PriceTargetEntity::class,
        ScannedPriceEntity::class,
        ProductNoteEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun installmentDao(): InstallmentDao
    abstract fun recentSearchDao(): RecentSearchDao
    abstract fun priceTargetDao(): PriceTargetDao
    abstract fun scannedPriceDao(): ScannedPriceDao
    abstract fun productNoteDao(): ProductNoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recent_searches` (
                        `code` TEXT NOT NULL,
                        `productName` TEXT NOT NULL,
                        `searchedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`code`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `price_targets` (
                        `code` TEXT NOT NULL,
                        `productName` TEXT NOT NULL,
                        `targetPrice` REAL NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`code`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `scanned_prices` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `productCode` TEXT NOT NULL,
                        `productName` TEXT NOT NULL,
                        `scannedPrice` REAL NOT NULL,
                        `marketAvgPrice` REAL NOT NULL,
                        `rawBarcode` TEXT,
                        `scannedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_scanned_prices_productCode_scannedAt` ON `scanned_prices` (`productCode`, `scannedAt`)"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `product_notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `productCode` TEXT NOT NULL,
                        `productName` TEXT NOT NULL,
                        `noteText` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_product_notes_productCode_updatedAt` ON `product_notes` (`productCode`, `updatedAt`)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pvs_cuotas_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
