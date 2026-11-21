package com.billing.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.billing.app.data.local.dao.AuditLogDao
import com.billing.app.data.local.dao.CashMovementDao
import com.billing.app.data.local.dao.CategoryDao
import com.billing.app.data.local.dao.CustomUnitDao
import com.billing.app.data.local.dao.CustomerDao
import com.billing.app.data.local.dao.CustomerLedgerDao
import com.billing.app.data.local.dao.ExpenseDao
import com.billing.app.data.local.dao.InvoiceDao
import com.billing.app.data.local.dao.InvoiceItemBatchDao
import com.billing.app.data.local.dao.PaymentAllocationDao
import com.billing.app.data.local.dao.ProductBatchDao
import com.billing.app.data.local.dao.ProductDao
import com.billing.app.data.local.dao.PurchaseDao
import com.billing.app.data.local.dao.ReturnDao
import com.billing.app.data.local.dao.ReturnItemBatchDao
import com.billing.app.data.local.dao.ShiftDao
import com.billing.app.data.local.dao.ShopDao
import com.billing.app.data.local.dao.StockAdjustmentDao
import com.billing.app.data.local.dao.SupplierDao
import com.billing.app.data.local.dao.SupplierLedgerDao
import com.billing.app.data.local.dao.UserDao
import com.billing.app.data.local.entity.AuditLogEntity
import com.billing.app.data.local.entity.CashMovementEntity
import com.billing.app.data.local.entity.CategoryEntity
import com.billing.app.data.local.entity.CustomUnitEntity
import com.billing.app.data.local.entity.CustomerEntity
import com.billing.app.data.local.entity.CustomerLedgerEntity
import com.billing.app.data.local.entity.ExpenseEntity
import com.billing.app.data.local.entity.InvoiceCounterEntity
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemBatchEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.PaymentAllocationEntity
import com.billing.app.data.local.entity.ProductBatchEntity
import com.billing.app.data.local.entity.ProductEntity
import com.billing.app.data.local.entity.PurchaseEntity
import com.billing.app.data.local.entity.PurchaseItemEntity
import com.billing.app.data.local.entity.ReturnEntity
import com.billing.app.data.local.entity.ReturnItemBatchEntity
import com.billing.app.data.local.entity.ReturnItemEntity
import com.billing.app.data.local.entity.ShiftEntity
import com.billing.app.data.local.entity.ShopEntity
import com.billing.app.data.local.entity.StockAdjustmentEntity
import com.billing.app.data.local.entity.SupplierEntity
import com.billing.app.data.local.entity.SupplierLedgerEntity
import com.billing.app.data.local.entity.UserEntity

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
        PurchaseItemEntity::class,
        CustomerLedgerEntity::class,
        SupplierLedgerEntity::class,
        PaymentAllocationEntity::class,
        ShiftEntity::class,
        CashMovementEntity::class,
        UserEntity::class,
        ProductBatchEntity::class,
        InvoiceItemBatchEntity::class,
        ReturnEntity::class,
        ReturnItemEntity::class,
        ReturnItemBatchEntity::class,
        StockAdjustmentEntity::class,
        AuditLogEntity::class,
        CustomUnitEntity::class
    ],
    version = 8,
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
    abstract fun userDao(): UserDao
    abstract fun shiftDao(): ShiftDao
    abstract fun cashMovementDao(): CashMovementDao
    abstract fun customerLedgerDao(): CustomerLedgerDao
    abstract fun supplierLedgerDao(): SupplierLedgerDao
    abstract fun paymentAllocationDao(): PaymentAllocationDao
    abstract fun productBatchDao(): ProductBatchDao
    abstract fun invoiceItemBatchDao(): InvoiceItemBatchDao
    abstract fun returnDao(): ReturnDao
    abstract fun returnItemBatchDao(): ReturnItemBatchDao
    abstract fun stockAdjustmentDao(): StockAdjustmentDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun customUnitDao(): CustomUnitDao

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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE categories ADD COLUMN imageUri TEXT DEFAULT NULL")
                } catch (_: Exception) {}
            }
        }

        private fun createV6Tables(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS customer_ledger (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    customerId TEXT NOT NULL,
                    transactionType TEXT NOT NULL,
                    invoiceId INTEGER,
                    debitPaise INTEGER NOT NULL,
                    creditPaise INTEGER NOT NULL,
                    balanceAfterPaise INTEGER NOT NULL,
                    paymentMode TEXT,
                    note TEXT,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(customerId) REFERENCES customers(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_ledger_customerId ON customer_ledger (customerId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_customer_ledger_createdAt ON customer_ledger (createdAt)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS supplier_ledger (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    supplierId TEXT NOT NULL,
                    transactionType TEXT NOT NULL,
                    purchaseId INTEGER,
                    debitPaise INTEGER NOT NULL,
                    creditPaise INTEGER NOT NULL,
                    balanceAfterPaise INTEGER NOT NULL,
                    paymentMode TEXT,
                    note TEXT,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(supplierId) REFERENCES suppliers(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_supplier_ledger_supplierId ON supplier_ledger (supplierId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_supplier_ledger_createdAt ON supplier_ledger (createdAt)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS payment_allocations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    ledgerEntryId INTEGER NOT NULL,
                    invoiceId INTEGER,
                    allocatedAmountPaise INTEGER NOT NULL,
                    reversesAllocationId INTEGER,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(ledgerEntryId) REFERENCES customer_ledger(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(invoiceId) REFERENCES invoices(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(reversesAllocationId) REFERENCES payment_allocations(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_allocations_ledgerEntryId ON payment_allocations (ledgerEntryId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_allocations_invoiceId ON payment_allocations (invoiceId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_payment_allocations_reversesAllocationId ON payment_allocations (reversesAllocationId)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS shifts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId TEXT NOT NULL,
                    openedAt INTEGER NOT NULL,
                    closedAt INTEGER,
                    openingFloatPaise INTEGER NOT NULL,
                    actualCashPaise INTEGER,
                    expectedCashPaise INTEGER,
                    variancePaise INTEGER,
                    note TEXT,
                    status TEXT NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_shifts_userId ON shifts (userId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_shifts_openedAt ON shifts (openedAt)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS cash_movements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    shiftId INTEGER NOT NULL,
                    movementType TEXT NOT NULL,
                    amountPaise INTEGER NOT NULL,
                    referenceId TEXT,
                    reason TEXT,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(shiftId) REFERENCES shifts(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_cash_movements_shiftId ON cash_movements (shiftId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_cash_movements_createdAt ON cash_movements (createdAt)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS users (
                    id TEXT NOT NULL,
                    username TEXT NOT NULL,
                    displayName TEXT NOT NULL,
                    pinHash TEXT NOT NULL,
                    pinSalt TEXT NOT NULL,
                    role TEXT NOT NULL,
                    isActive INTEGER NOT NULL,
                    failedAttempts INTEGER NOT NULL,
                    lockedUntil INTEGER,
                    mustSetPin INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    PRIMARY KEY(id)
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_users_role ON users (role)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_username ON users (username)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS product_batches (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    productId TEXT NOT NULL,
                    batchNumber TEXT NOT NULL,
                    expiryDate INTEGER,
                    purchasePricePaise INTEGER NOT NULL,
                    mrpPaise INTEGER NOT NULL,
                    initialStockMilli INTEGER NOT NULL,
                    currentStockMilli INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(productId) REFERENCES products(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_product_batches_productId ON product_batches (productId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_product_batches_expiryDate ON product_batches (expiryDate)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS invoice_item_batches (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    invoiceItemId INTEGER NOT NULL,
                    batchId INTEGER NOT NULL,
                    quantityMilli INTEGER NOT NULL,
                    costPaise INTEGER NOT NULL,
                    FOREIGN KEY(invoiceItemId) REFERENCES invoice_items(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(batchId) REFERENCES product_batches(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_invoice_item_batches_invoiceItemId ON invoice_item_batches (invoiceItemId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_invoice_item_batches_batchId ON invoice_item_batches (batchId)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS returns (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    returnNumber TEXT NOT NULL,
                    invoiceId INTEGER NOT NULL,
                    shiftId INTEGER NOT NULL,
                    totalRefundPaise INTEGER NOT NULL,
                    cashRefundPaise INTEGER NOT NULL,
                    upiRefundPaise INTEGER NOT NULL,
                    khataCreditPaise INTEGER NOT NULL,
                    reason TEXT NOT NULL,
                    processedByUserId TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(invoiceId) REFERENCES invoices(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(shiftId) REFERENCES shifts(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_returns_invoiceId ON returns (invoiceId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_returns_shiftId ON returns (shiftId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_returns_returnNumber ON returns (returnNumber)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS return_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    returnId INTEGER NOT NULL,
                    invoiceItemId INTEGER NOT NULL,
                    quantityMilli INTEGER NOT NULL,
                    refundAmountPaise INTEGER NOT NULL,
                    taxableRefundPaise INTEGER NOT NULL,
                    taxRefundPaise INTEGER NOT NULL,
                    restock INTEGER NOT NULL,
                    FOREIGN KEY(returnId) REFERENCES returns(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(invoiceItemId) REFERENCES invoice_items(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_return_items_returnId ON return_items (returnId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_return_items_invoiceItemId ON return_items (invoiceItemId)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS return_item_batches (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    returnItemId INTEGER NOT NULL,
                    batchId INTEGER NOT NULL,
                    quantityMilli INTEGER NOT NULL,
                    FOREIGN KEY(returnItemId) REFERENCES return_items(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(batchId) REFERENCES product_batches(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_return_item_batches_returnItemId ON return_item_batches (returnItemId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_return_item_batches_batchId ON return_item_batches (batchId)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS stock_adjustments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    productId TEXT NOT NULL,
                    batchId INTEGER,
                    adjustedQuantityMilli INTEGER NOT NULL,
                    reason TEXT NOT NULL,
                    adjustedByUserId TEXT NOT NULL,
                    adjustedAt INTEGER NOT NULL,
                    FOREIGN KEY(productId) REFERENCES products(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_adjustments_productId ON stock_adjustments (productId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_adjustments_adjustedAt ON stock_adjustments (adjustedAt)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS audit_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId TEXT NOT NULL,
                    action TEXT NOT NULL,
                    entityType TEXT NOT NULL,
                    entityId TEXT NOT NULL,
                    details TEXT NOT NULL,
                    timestamp INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_logs_userId ON audit_logs (userId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_logs_timestamp ON audit_logs (timestamp)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_logs_action ON audit_logs (action)")
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createV6Tables(db)
                try {
                    db.execSQL("ALTER TABLE suppliers ADD COLUMN duePaise INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE invoice_items ADD COLUMN unit TEXT NOT NULL DEFAULT 'PCS'")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE purchase_items ADD COLUMN unit TEXT NOT NULL DEFAULT 'PCS'")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE return_items ADD COLUMN unit TEXT NOT NULL DEFAULT 'PCS'")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS custom_units (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        code TEXT NOT NULL,
                        name TEXT NOT NULL,
                        baseUnitCode TEXT NOT NULL,
                        conversionFactor REAL NOT NULL,
                        allowsFractional INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_custom_units_code ON custom_units (code)")

                try {
                    db.execSQL("DROP INDEX IF EXISTS index_products_barcode")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_products_barcode ON products (barcode)")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_products_barcode_non_empty ON products (barcode) WHERE barcode != '' AND barcode IS NOT NULL")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_5_8 = object : Migration(5, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createV6Tables(db)

                try {
                    db.execSQL("ALTER TABLE suppliers ADD COLUMN duePaise INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE invoice_items ADD COLUMN unit TEXT NOT NULL DEFAULT 'PCS'")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE purchase_items ADD COLUMN unit TEXT NOT NULL DEFAULT 'PCS'")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE return_items ADD COLUMN unit TEXT NOT NULL DEFAULT 'PCS'")
                } catch (_: Exception) {}

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS custom_units (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        code TEXT NOT NULL,
                        name TEXT NOT NULL,
                        baseUnitCode TEXT NOT NULL,
                        conversionFactor REAL NOT NULL,
                        allowsFractional INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_custom_units_code ON custom_units (code)")

                try {
                    db.execSQL("DROP INDEX IF EXISTS index_products_barcode")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_products_barcode ON products (barcode)")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_products_barcode_non_empty ON products (barcode) WHERE barcode != '' AND barcode IS NOT NULL")
                } catch (_: Exception) {}
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "billing_app.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_5_8)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        db.execSQL("PRAGMA foreign_keys = ON;")
                    }
                })

                if (com.billing.app.BuildConfig.DEBUG) {
                    builder.fallbackToDestructiveMigration()
                }

                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }
    }
}
