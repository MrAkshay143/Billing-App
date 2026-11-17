package com.billing.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.billing.app.data.local.dao.CategoryDao
import com.billing.app.data.local.dao.CustomerDao
import com.billing.app.data.local.dao.ExpenseDao
import com.billing.app.data.local.dao.InvoiceDao
import com.billing.app.data.local.dao.ProductDao
import com.billing.app.data.local.dao.PurchaseDao
import com.billing.app.data.local.dao.ShopDao
import com.billing.app.data.local.dao.SupplierDao
import com.billing.app.data.local.entity.CategoryEntity
import com.billing.app.data.local.entity.CustomerEntity
import com.billing.app.data.local.entity.ExpenseEntity
import com.billing.app.data.local.entity.InvoiceCounterEntity
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.ProductEntity
import com.billing.app.data.local.entity.PurchaseEntity
import com.billing.app.data.local.entity.PurchaseItemEntity
import com.billing.app.data.local.entity.ShopEntity
import com.billing.app.data.local.entity.SupplierEntity

@Database(
    entities = [
        ProductEntity::class,
        ShopEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        InvoiceCounterEntity::class,
        CustomerEntity::class,
        CategoryEntity::class,
        SupplierEntity::class,
        ExpenseEntity::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun shopDao(): ShopDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun customerDao(): CustomerDao
    abstract fun categoryDao(): CategoryDao
    abstract fun supplierDao(): SupplierDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun purchaseDao(): PurchaseDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
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
                    VALUES (1, (SELECT COALESCE(MAX(id), COUNT(*)) FROM invoices))
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add new columns to products
                try {
                    db.execSQL("ALTER TABLE products ADD COLUMN sku TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE products ADD COLUMN category TEXT NOT NULL DEFAULT 'Grocery'")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE products ADD COLUMN unit TEXT NOT NULL DEFAULT 'Piece'")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE products ADD COLUMN purchasePricePaise INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE products ADD COLUMN mrpPaise INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE products ADD COLUMN minStockAlert INTEGER NOT NULL DEFAULT 5")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE products ADD COLUMN gstRate INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE products ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}

                db.execSQL("CREATE INDEX IF NOT EXISTS index_products_category ON products (category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_products_sku ON products (sku)")

                // 2. Add invoiceType & itemsCount columns to invoices
                try {
                    db.execSQL("ALTER TABLE invoices ADD COLUMN invoiceType TEXT NOT NULL DEFAULT 'SALE'")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE invoices ADD COLUMN itemsCount INTEGER NOT NULL DEFAULT 1")
                } catch (_: Exception) {}
                db.execSQL("CREATE INDEX IF NOT EXISTS index_invoices_invoiceType ON invoices (invoiceType)")

                // 3. Create customers table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS customers (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        email TEXT,
                        address TEXT,
                        gstin TEXT,
                        openingBalancePaise INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_customers_phone ON customers (phone)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_name ON customers (name)")

                // 4. Create categories table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        iconName TEXT,
                        displayOrder INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_categories_name ON categories (name)")

                // 5. Create suppliers table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS suppliers (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        contactPerson TEXT,
                        phone TEXT NOT NULL,
                        email TEXT,
                        address TEXT,
                        gstin TEXT,
                        openingBalancePaise INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_suppliers_phone ON suppliers (phone)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_suppliers_name ON suppliers (name)")

                // 6. Create expenses table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS expenses (
                        id TEXT PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        category TEXT NOT NULL,
                        amountPaise INTEGER NOT NULL,
                        date INTEGER NOT NULL,
                        paymentMethod TEXT NOT NULL,
                        description TEXT,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_date ON expenses (date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_category ON expenses (category)")

                // 7. Create purchases & purchase_items tables
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS purchases (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        purchaseNumber TEXT NOT NULL,
                        supplierId TEXT,
                        supplierName TEXT,
                        timestamp INTEGER NOT NULL,
                        totalPaise INTEGER NOT NULL,
                        paymentStatus TEXT NOT NULL DEFAULT 'PAID'
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_purchases_purchaseNumber ON purchases (purchaseNumber)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_purchases_timestamp ON purchases (timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_purchases_supplierId ON purchases (supplierId)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS purchase_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        purchaseId INTEGER NOT NULL,
                        productId TEXT NOT NULL,
                        productName TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        unitPricePaise INTEGER NOT NULL,
                        lineTotalPaise INTEGER NOT NULL,
                        FOREIGN KEY(purchaseId) REFERENCES purchases(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_items_purchaseId ON purchase_items (purchaseId)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "billing_app.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        db.execSQL("PRAGMA foreign_keys = ON;")
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
