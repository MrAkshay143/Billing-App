package com.billing.app.feature.home

import com.billing.app.core.printer.EscPosFormatter
import com.billing.app.core.util.Money
import com.billing.app.data.local.dao.CashMovementDao
import com.billing.app.data.local.dao.InvoiceDao
import com.billing.app.data.local.dao.ShiftDao
import com.billing.app.data.local.dao.UserDao
import com.billing.app.data.local.entity.CashMovementEntity
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.ShiftEntity
import com.billing.app.data.local.entity.UserEntity
import com.billing.app.data.repository.ShiftRepositoryImpl
import com.billing.app.domain.model.BluetoothPrinterDevice
import com.billing.app.domain.model.CartItem
import com.billing.app.domain.model.InvoiceReceipt
import com.billing.app.domain.model.ShiftZReport
import com.billing.app.domain.model.ShopProfile
import com.billing.app.domain.repository.PrinterRepository
import com.billing.app.domain.repository.ShiftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

class ShiftUiWorkflowTest {

    private class FakeShiftDao : ShiftDao {
        private var shiftStorage: ShiftEntity? = null
        private val shiftFlow = MutableStateFlow<ShiftEntity?>(null)

        override suspend fun getShiftById(id: Long): ShiftEntity? =
            if (shiftStorage?.id == id) shiftStorage else null

        override suspend fun insertShift(shift: ShiftEntity): Long {
            val assigned = shift.copy(id = 101L)
            shiftStorage = assigned
            shiftFlow.value = assigned
            return assigned.id
        }

        override suspend fun upsertShift(shift: ShiftEntity) {
            shiftStorage = shift
            shiftFlow.value = shift
        }

        override suspend fun upsertAll(shifts: List<ShiftEntity>) {
            shiftStorage = shifts.lastOrNull()
            shiftFlow.value = shiftStorage
        }

        override suspend fun updateShift(shift: ShiftEntity) {
            shiftStorage = shift
            shiftFlow.value = shift
        }

        override suspend fun getOpenShiftForUser(userId: String): ShiftEntity? =
            if (shiftStorage?.status == "OPEN" && shiftStorage?.userId == userId) shiftStorage else null

        override suspend fun getCurrentOpenShift(): ShiftEntity? =
            if (shiftStorage?.status == "OPEN") shiftStorage else null

        override fun getCurrentOpenShiftFlow(): Flow<ShiftEntity?> = shiftFlow

        override fun getAllShifts(): Flow<List<ShiftEntity>> =
            flowOf(listOfNotNull(shiftStorage))

        override fun getShiftsForUser(userId: String): Flow<List<ShiftEntity>> =
            flowOf(listOfNotNull(shiftStorage?.takeIf { it.userId == userId }))

        override suspend fun closeShift(
            id: Long,
            closedAt: Long,
            actualCashPaise: Long,
            expectedCashPaise: Long,
            variancePaise: Long,
            note: String?,
            status: String
        ) {
            shiftStorage = shiftStorage?.copy(
                closedAt = closedAt,
                actualCashPaise = actualCashPaise,
                expectedCashPaise = expectedCashPaise,
                variancePaise = variancePaise,
                note = note,
                status = status
            )
            shiftFlow.value = shiftStorage
        }

        override suspend fun deleteShift(id: Long) {
            if (shiftStorage?.id == id) {
                shiftStorage = null
                shiftFlow.value = null
            }
        }

        override fun getShiftCount(): Flow<Int> =
            flowOf(if (shiftStorage != null) 1 else 0)
    }

    private class FakeCashMovementDao : CashMovementDao {
        val movements = mutableListOf<CashMovementEntity>()
        private var nextId = 1L

        override suspend fun insertCashMovement(movement: CashMovementEntity): Long {
            val id = nextId++
            movements.add(movement.copy(id = id))
            return id
        }

        override suspend fun upsertCashMovement(movement: CashMovementEntity) {
            movements.removeAll { it.id == movement.id }
            movements.add(movement)
        }

        override suspend fun upsertAll(movements: List<CashMovementEntity>) {
            movements.forEach { upsertCashMovement(it) }
        }

        override fun getCashMovementsForShift(shiftId: Long): Flow<List<CashMovementEntity>> =
            flowOf(movements.filter { it.shiftId == shiftId })

        override suspend fun getCashMovementListForShift(shiftId: Long): List<CashMovementEntity> =
            movements.filter { it.shiftId == shiftId }

        override fun getAllCashMovements(): Flow<List<CashMovementEntity>> =
            flowOf(movements)

        override suspend fun getCashMovementById(id: Long): CashMovementEntity? =
            movements.find { it.id == id }

        override fun getTotalByMovementType(shiftId: Long, movementType: String): Flow<Long> =
            flowOf(movements.filter { it.shiftId == shiftId && it.movementType == movementType }.sumOf { it.amountPaise })

        override suspend fun getNetCashForShift(shiftId: Long): Long =
            movements.filter { it.shiftId == shiftId && it.reason != "Opening Float" }.sumOf { it.amountPaise }

        override fun getNetCashForShiftFlow(shiftId: Long): Flow<Long> =
            flowOf(movements.filter { it.shiftId == shiftId && it.reason != "Opening Float" }.sumOf { it.amountPaise })

        override suspend fun deleteCashMovement(id: Long) {
            movements.removeAll { it.id == id }
        }

        override suspend fun deleteCashMovementsForShift(shiftId: Long) {
            movements.removeAll { it.shiftId == shiftId }
        }
    }

    private class FakeUserDao(var users: Map<String, UserEntity> = emptyMap()) : UserDao {
        override fun getAllUsers(): Flow<List<UserEntity>> = flowOf(users.values.toList())
        override fun getActiveUsers(): Flow<List<UserEntity>> = flowOf(users.values.filter { it.isActive })
        override suspend fun getUserById(id: String): UserEntity? = users[id]
        override suspend fun getUserByUsername(username: String): UserEntity? = users.values.find { it.username == username }
        override fun getUsersByRole(role: String): Flow<List<UserEntity>> = flowOf(users.values.filter { it.role == role })
        override suspend fun upsertUser(user: UserEntity) {}
        override suspend fun upsertAll(users: List<UserEntity>) {}
        override suspend fun insertUser(user: UserEntity) {}
        override suspend fun updateUser(user: UserEntity) {}
        override suspend fun deleteUser(id: String) {}
        override fun getUserCount(): Flow<Int> = flowOf(users.size)
        override suspend fun getUserCountDirect(): Int = users.size
        override suspend fun updateFailedAttempts(id: String, attempts: Int, lockedUntil: Long?) {}
        override suspend fun resetFailedAttempts(id: String) {}
        override suspend fun updatePin(id: String, pinHash: String, pinSalt: String, mustSetPin: Boolean) {}
        override suspend fun setUserActiveStatus(id: String, isActive: Boolean) {}
    }

    private fun createFakeInvoiceDao(
        invoices: List<InvoiceEntity> = emptyList(),
        items: List<InvoiceItemEntity> = emptyList()
    ): InvoiceDao {
        return Proxy.newProxyInstance(
            InvoiceDao::class.java.classLoader,
            arrayOf(InvoiceDao::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getInvoicesByShiftId" -> invoices
                "getInvoiceItemsByShiftId" -> items
                else -> null
            }
        } as InvoiceDao
    }

    private class FakePrinterRepository : PrinterRepository {
        var printedReport: ShiftZReport? = null
        var isPrintCalled: Boolean = false
        var shouldSucceed: Boolean = true

        override fun getSavedPrinterMac(): Flow<String?> = flowOf("00:11:22:33:44:55")
        override fun getSavedPrinterName(): Flow<String?> = flowOf("BT_Thermal_80")
        override suspend fun getBondedPrinters(): List<BluetoothPrinterDevice> = emptyList()
        override suspend fun connect(macAddress: String): Result<Unit> = Result.success(Unit)
        override fun disconnect() {}
        override val isConnected: Boolean = true
        override suspend fun savePrinterData(mac: String, name: String) {}
        override suspend fun clearPrinterData() {}
        override suspend fun printReceipt(shop: ShopProfile, items: List<CartItem>): Result<Unit> = Result.success(Unit)
        override suspend fun printInvoiceReceipt(receipt: InvoiceReceipt): Result<Unit> = Result.success(Unit)
        override suspend fun printInvoice(invoice: InvoiceEntity, items: List<InvoiceItemEntity>, shop: ShopProfile): Result<Unit> = Result.success(Unit)
        override suspend fun printTestReceipt(shop: ShopProfile): Result<Unit> = Result.success(Unit)

        override suspend fun printZReport(zReport: ShiftZReport): Result<Unit> {
            isPrintCalled = true
            printedReport = zReport
            return if (shouldSucceed) Result.success(Unit) else Result.failure(Exception("Printer out of paper"))
        }
    }

    private lateinit var shiftDao: FakeShiftDao
    private lateinit var cashMovementDao: FakeCashMovementDao
    private lateinit var userDao: FakeUserDao
    private lateinit var printerRepository: FakePrinterRepository

    @Before
    fun setUp() {
        shiftDao = FakeShiftDao()
        cashMovementDao = FakeCashMovementDao()
        userDao = FakeUserDao(
            mapOf(
                "mgr1" to UserEntity(id = "mgr1", username = "babulal", displayName = "Babu Lal (Manager)", role = "MANAGER", pinHash = "pin", pinSalt = ""),
                "csh1" to UserEntity(id = "csh1", username = "rahul", displayName = "Rahul Ghosh", role = "CASHIER", pinHash = "pin", pinSalt = ""),
                "csh2" to UserEntity(id = "csh2", username = "priya", displayName = "Priya Roy", role = "CASHIER", pinHash = "pin", pinSalt = "")
            )
        )
        printerRepository = FakePrinterRepository()
    }

    @Test
    fun testOpenShiftAndExpectedCashCalculation() = runTest {
        val invoiceDao = createFakeInvoiceDao()
        val shiftRepository = ShiftRepositoryImpl(
            shiftDao = shiftDao,
            cashMovementDao = cashMovementDao,
            userDao = userDao,
            invoiceDao = invoiceDao
        )

        // 1. Open shift with float ₹1000 (100000 paise)
        val openResult = shiftRepository.openShift(userId = "mgr1", openingFloatPaise = 100000L, note = "Morning opening")
        assertTrue(openResult.isSuccess)
        val shift = openResult.getOrThrow()
        assertEquals(101L, shift.id)
        assertEquals(100000L, shift.openingFloatPaise)
        assertEquals("OPEN", shift.status)

        // 2. Record cash sales of ₹2500 (250000 paise)
        shiftRepository.recordCashMovement(
            movementType = "SALE_CASH",
            amountPaise = 250000L,
            reason = "Invoice Cash Sale"
        )

        // 3. Record Khata Collection of ₹500 (50000 paise)
        shiftRepository.recordCashMovement(
            movementType = "KHATA_COLLECTION",
            amountPaise = 50000L,
            reason = "Customer Khata repayment"
        )

        // 4. Record Cash Expense of ₹300 (30000 paise)
        shiftRepository.recordCashMovement(
            movementType = "EXPENSE_OUT",
            amountPaise = -30000L,
            reason = "Tea and snacks for staff"
        )

        // 5. Record Supplier payment in cash of ₹700 (70000 paise)
        shiftRepository.recordCashMovement(
            movementType = "PURCHASE_PAYMENT",
            amountPaise = -70000L,
            reason = "Vegetable wholesaler settlement"
        )

        // Expected cash should be:
        // Opening Float (100000) + Sales (250000) + Khata (50000) - Expense (30000) - Supplier (70000) = 300000 paise (₹3000.00)
        val expectedCash = shiftRepository.getExpectedCashForActiveShift()
        assertEquals(300000L, expectedCash)
        assertEquals("₹3000.00", Money(expectedCash).format())
    }

    @Test
    fun testShiftCloseBlindCountBalancedWorkflow() = runTest {
        val invoiceDao = createFakeInvoiceDao()
        val shiftRepository = ShiftRepositoryImpl(
            shiftDao = shiftDao,
            cashMovementDao = cashMovementDao,
            userDao = userDao,
            invoiceDao = invoiceDao
        )

        // Open shift with ₹1500 (150000 paise)
        shiftRepository.openShift("mgr1", 150000L)
        shiftRepository.recordCashMovement("SALE_CASH", 350000L, reason = "Sales")
        shiftRepository.recordCashMovement("EXPENSE_OUT", -20000L, reason = "Cleaning")

        // Expected Cash = 150000 + 350000 - 20000 = 480000 paise (₹4800.00)
        val expectedCash = shiftRepository.getExpectedCashForActiveShift()
        assertEquals(480000L, expectedCash)

        // Cashier blind count matches exactly: ₹4800.00 (480000 paise)
        val closeResult = shiftRepository.closeShift(actualCashPaise = 480000L, note = "Count verified")
        assertTrue(closeResult.isSuccess)
        val closedShift = closeResult.getOrThrow()

        assertEquals("CLOSED", closedShift.status)
        assertEquals(480000L, closedShift.expectedCashPaise)
        assertEquals(480000L, closedShift.actualCashPaise)
        assertEquals(0L, closedShift.variancePaise) // Balanced

        // Generate Z-Report
        val zReportResult = shiftRepository.generateZReport(closedShift.id)
        assertTrue(zReportResult.isSuccess)
        val zReport = zReportResult.getOrThrow()

        assertEquals(0L, zReport.variancePaise)
        assertEquals("Babu Lal (Manager)", zReport.managerName)
        assertEquals(150000L, zReport.openingFloatPaise)
        assertEquals(350000L, zReport.cashSalesPaise)
        assertEquals(20000L, zReport.expenseCashOutPaise)

        // Trigger thermal print
        val printResult = printerRepository.printZReport(zReport)
        assertTrue(printResult.isSuccess)
        assertTrue(printerRepository.isPrintCalled)
        assertEquals(zReport, printerRepository.printedReport)
    }

    @Test
    fun testShiftCloseShortageWorkflow() = runTest {
        val invoiceDao = createFakeInvoiceDao()
        val shiftRepository = ShiftRepositoryImpl(
            shiftDao = shiftDao,
            cashMovementDao = cashMovementDao,
            userDao = userDao,
            invoiceDao = invoiceDao
        )

        // Open shift with ₹2000
        shiftRepository.openShift("mgr1", 200000L)
        shiftRepository.recordCashMovement("SALE_CASH", 500000L)

        // Expected Cash = 700000 paise (₹7000.00)
        // Cashier physical count finds only ₹6850.00 (short by ₹150.00 = 15000 paise)
        val actualCountPaise = 685000L
        val closeResult = shiftRepository.closeShift(actualCashPaise = actualCountPaise, note = "Shortage of Rs. 150")
        assertTrue(closeResult.isSuccess)

        val closedShift = closeResult.getOrThrow()
        assertEquals(-15000L, closedShift.variancePaise)

        val zReportResult = shiftRepository.generateZReport(closedShift.id)
        val zReport = zReportResult.getOrThrow()
        assertEquals(-15000L, zReport.variancePaise)
        assertEquals("-₹150.00", Money(zReport.variancePaise).format())
    }

    @Test
    fun testShiftCloseExcessWorkflow() = runTest {
        val invoiceDao = createFakeInvoiceDao()
        val shiftRepository = ShiftRepositoryImpl(
            shiftDao = shiftDao,
            cashMovementDao = cashMovementDao,
            userDao = userDao,
            invoiceDao = invoiceDao
        )

        // Open shift with ₹1000
        shiftRepository.openShift("mgr1", 100000L)
        shiftRepository.recordCashMovement("SALE_CASH", 100000L)

        // Expected Cash = 200000 paise (₹2000.00)
        // Cashier physical count finds ₹2120.00 (excess by ₹120.00 = 12000 paise)
        val actualCountPaise = 212000L
        val closeResult = shiftRepository.closeShift(actualCashPaise = actualCountPaise, note = "Excess found")
        assertTrue(closeResult.isSuccess)

        val closedShift = closeResult.getOrThrow()
        assertEquals(12000L, closedShift.variancePaise)

        val zReportResult = shiftRepository.generateZReport(closedShift.id)
        val zReport = zReportResult.getOrThrow()
        assertEquals(12000L, zReport.variancePaise)
        assertEquals("₹120.00", Money(zReport.variancePaise).format())
    }

    @Test
    fun testZReportWithMultiCashierBreakdownAndEscPosPrintFormatting() = runTest {
        val shiftId = 101L

        // Seed Invoices from Cashier 1 (Rahul) and Cashier 2 (Priya)
        val invoices = listOf(
            InvoiceEntity(
                id = 1L,
                invoiceNumber = "INV-001",
                shiftId = shiftId,
                createdByUserId = "csh1",
                grandTotalPaise = 25000L,
                subtotalPaise = 25000L,
                upiPaidPaise = 0L,
                originalDuePaise = 0L
            ),
            InvoiceEntity(
                id = 2L,
                invoiceNumber = "INV-002",
                shiftId = shiftId,
                createdByUserId = "csh1",
                grandTotalPaise = 45000L,
                subtotalPaise = 45000L,
                upiPaidPaise = 45000L,
                originalDuePaise = 0L
            ),
            InvoiceEntity(
                id = 3L,
                invoiceNumber = "INV-003",
                shiftId = shiftId,
                createdByUserId = "csh2",
                grandTotalPaise = 30000L,
                subtotalPaise = 30000L,
                upiPaidPaise = 0L,
                originalDuePaise = 20000L
            )
        )

        // Seed items: 6 items in total (6000 milli-units)
        val invoiceItems = listOf(
            InvoiceItemEntity(
                id = 1L,
                invoiceId = 1L,
                productId = "P1",
                productName = "Atta 5kg",
                quantityMilli = 2000L,
                unitPricePaise = 25000L,
                lineTotalPaise = 25000L
            ),
            InvoiceItemEntity(
                id = 2L,
                invoiceId = 2L,
                productId = "P2",
                productName = "Mustard Oil",
                quantityMilli = 3000L,
                unitPricePaise = 45000L,
                lineTotalPaise = 45000L
            ),
            InvoiceItemEntity(
                id = 3L,
                invoiceId = 3L,
                productId = "P3",
                productName = "Sugar 1kg",
                quantityMilli = 1000L,
                unitPricePaise = 30000L,
                lineTotalPaise = 30000L
            )
        )

        val invoiceDao = createFakeInvoiceDao(invoices, invoiceItems)
        val shiftRepository = ShiftRepositoryImpl(
            shiftDao = shiftDao,
            cashMovementDao = cashMovementDao,
            userDao = userDao,
            invoiceDao = invoiceDao
        )

        shiftRepository.openShift("mgr1", 100000L)
        shiftRepository.recordCashMovement("SALE_CASH", 35000L) // 25000 + 10000 cash paid

        val closeResult = shiftRepository.closeShift(actualCashPaise = 135000L)
        assertTrue(closeResult.isSuccess)

        val zReportResult = shiftRepository.generateZReport(shiftId)
        assertTrue(zReportResult.isSuccess)
        val zReport = zReportResult.getOrThrow()

        // Verify summary
        assertEquals(3, zReport.invoicesCount)
        assertEquals(6L, zReport.itemsSoldCount)
        assertEquals(45000L, zReport.upiSalesPaise)
        assertEquals(20000L, zReport.creditSalesPaise)
        assertEquals(100000L, zReport.totalTurnoverPaise) // 25000 + 45000 + 30000

        // Verify cashier breakdown
        assertEquals(2, zReport.cashierBreakdowns.size)
        val rahulBreakdown = zReport.cashierBreakdowns.find { it.userId == "csh1" }
        assertNotNull(rahulBreakdown)
        assertEquals("Rahul Ghosh", rahulBreakdown?.userName)
        assertEquals(2, rahulBreakdown?.invoiceCount)
        assertEquals(70000L, rahulBreakdown?.totalSalesPaise)

        val priyaBreakdown = zReport.cashierBreakdowns.find { it.userId == "csh2" }
        assertNotNull(priyaBreakdown)
        assertEquals("Priya Roy", priyaBreakdown?.userName)
        assertEquals(1, priyaBreakdown?.invoiceCount)
        assertEquals(30000L, priyaBreakdown?.totalSalesPaise)

        // Test ESC/POS formatting for thermal printing
        val shop = ShopProfile(name = "Maa Tara Store", footerText = "Thank You")
        val printBytes = EscPosFormatter.formatZReportAuto(zReport, shop, paperWidth = "80mm")
        assertTrue(printBytes.isNotEmpty())

        val printBytes58 = EscPosFormatter.formatZReportAuto(zReport, shop, paperWidth = "58mm")
        assertTrue(printBytes58.isNotEmpty())
    }
}
