package com.billing.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.billing.app.data.local.dao.InvoiceDao
import com.billing.app.data.local.dao.ProductDao
import com.billing.app.data.local.dao.ShopDao
import com.billing.app.data.local.entity.InvoiceCounterEntity
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.ProductEntity
import com.billing.app.data.local.entity.ShopEntity

@Database(
    entities = [
        ProductEntity::class,
        ShopEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        InvoiceCounterEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun shopDao(): ShopDao
    abstract fun invoiceDao(): InvoiceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add cash payment fields if migrating from v1
                try {
                    db.execSQL("ALTER TABLE invoices ADD COLUMN cashReceivedPaise INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE invoices ADD COLUMN changeReturnedPaise INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create invoice_counter table
                db.execSQL("CREATE TABLE IF NOT EXISTS invoice_counter (id INTEGER PRIMARY KEY NOT NULL, lastNumber INTEGER NOT NULL)")

                // 2. Recreate invoices table with autoincrement id and duePaise
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS invoices_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        invoiceNumber TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        subtotalPaise INTEGER NOT NULL,
                        taxPaise INTEGER NOT NULL DEFAULT 0,
                        discountPaise INTEGER NOT NULL DEFAULT 0,
                        grandTotalPaise INTEGER NOT NULL,
                        duePaise INTEGER NOT NULL DEFAULT 0,
                        status TEXT NOT NULL,
                        paymentMethod TEXT NOT NULL,
                        customerName TEXT,
                        customerPhone TEXT,
                        cashReceivedPaise INTEGER NOT NULL DEFAULT 0,
                        changeReturnedPaise INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // 3. Copy existing invoice records
                db.execSQL("""
                    INSERT INTO invoices_new (
                        invoiceNumber, timestamp, subtotalPaise, taxPaise, discountPaise,
                        grandTotalPaise, duePaise, status, paymentMethod, customerName,
                        customerPhone, cashReceivedPaise, changeReturnedPaise
                    )
                    SELECT
                        invoiceNumber, timestamp, subtotalPaise, taxPaise, discountPaise,
                        grandTotalPaise, 0, status, paymentMethod, customerName,
                        customerPhone, cashReceivedPaise, changeReturnedPaise
                    FROM invoices
                """.trimIndent())

                // 4. Drop old table and rename new table
                db.execSQL("DROP TABLE invoices")
                db.execSQL("ALTER TABLE invoices_new RENAME TO invoices")

                // 5. Create indices
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_invoices_invoiceNumber ON invoices (invoiceNumber)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_invoices_timestamp ON invoices (timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_invoices_status ON invoices (status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_invoices_customerName ON invoices (customerName)")

                // 6. Initialize invoice counter if existing invoices exist
                db.execSQL("""
                    INSERT OR IGNORE INTO invoice_counter (id, lastNumber)
                    VALUES (1, (SELECT COALESCE(COUNT(*), 0) FROM invoices))
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "billing_app.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
