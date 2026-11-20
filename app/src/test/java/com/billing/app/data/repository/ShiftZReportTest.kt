package com.billing.app.data.repository

import com.billing.app.core.printer.EscPosFormatter
import com.billing.app.data.local.dao.CashMovementDao
import com.billing.app.data.local.dao.InvoiceDao
import com.billing.app.data.local.dao.ShiftDao
import com.billing.app.data.local.dao.UserDao
import com.billing.app.data.local.entity.CashMovementEntity
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.ShiftEntity
import com.billing.app.data.local.entity.UserEntity
import com.billing.app.domain.model.ShopProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class ShiftZReportTest {

    private class FakeShiftDao(var shift: ShiftEntity? = null) : ShiftDao {
        override suspend fun getShiftById(id: Long): ShiftEntity? = shift
        override suspend fun insertShift(shift: ShiftEntity): Long = shift.id
        override suspend fun upsertShift(shift: ShiftEntity) {}
        override suspend fun upsertAll(shifts: List<ShiftEntity>) {}
        override suspend fun updateShift(shift: ShiftEntity) { this.shift = shift }
        override suspend fun getOpenShiftForUser(userId: String): ShiftEntity? = shift
        override suspend fun getCurrentOpenShift(): ShiftEntity? = shift
        override fun getCurrentOpenShiftFlow(): Flow<ShiftEntity?> = flowOf(shift)
        override fun getAllShifts(): Flow<List<ShiftEntity>> = flowOf(emptyList())
        override fun getShiftsForUser(userId: String): Flow<List<ShiftEntity>> = flowOf(emptyList())
        override suspend fun closeShift(id: Long, closedAt: Long, actualCashPaise: Long, expectedCashPaise: Long, variancePaise: Long, note: String?, status: String) {}
        override suspend fun deleteShift(id: Long) {}
        override fun getShiftCount(): Flow<Int> = flowOf(1)
    }

    private class FakeCashMovementDao(var movements: List<CashMovementEntity> = emptyList()) : CashMovementDao {
        override suspend fun insertCashMovement(movement: CashMovementEntity): Long = movement.id
        override suspend fun upsertCashMovement(movement: CashMovementEntity) {}
        override suspend fun upsertAll(movements: List<CashMovementEntity>) {}
        override fun getCashMovementsForShift(shiftId: Long): Flow<List<CashMovementEntity>> = flowOf(movements)
        override suspend fun getCashMovementListForShift(shiftId: Long): List<CashMovementEntity> = movements
        override fun getAllCashMovements(): Flow<List<CashMovementEntity>> = flowOf(movements)
        override suspend fun getCashMovementById(id: Long): CashMovementEntity? = movements.find { it.id == id }
        override fun getTotalByMovementType(shiftId: Long, movementType: String): Flow<Long> =
            flowOf(movements.filter { it.shiftId == shiftId && it.movementType == movementType }.sumOf { it.amountPaise })
        override suspend fun getNetCashForShift(shiftId: Long): Long =
            movements.filter { it.shiftId == shiftId && (it.reason != "Opening Float") }.sumOf { it.amountPaise }
        override fun getNetCashForShiftFlow(shiftId: Long): Flow<Long> =
            flowOf(movements.filter { it.shiftId == shiftId && (it.reason != "Opening Float") }.sumOf { it.amountPaise })
        override suspend fun deleteCashMovement(id: Long) {}
        override suspend fun deleteCashMovementsForShift(shiftId: Long) {}
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

    @Test
    fun testZReportReconciliationCalculationAndVariance() = runTest {
        val shiftId = 101L
        val openingFloat = 100000L // Rs. 1,000.00
        val actualCountedCash = 295000L // Rs. 2,950.00 (Rs. 50 shortage)

        val shift = ShiftEntity(
            id = shiftId,
            userId = "admin-user",
            openedAt = 1700000000000L,
            closedAt = 1700028800000L,
            openingFloatPaise = openingFloat,
            actualCashPaise = actualCountedCash,
            status = "CLOSED"
        )

        val movements = listOf(
            CashMovementEntity(shiftId = shiftId, movementType = "CASH_IN", amountPaise = openingFloat, reason = "Opening Float"),
            CashMovementEntity(shiftId = shiftId, movementType = "SALE_CASH", amountPaise = 250000L, notes = "Cash sales"),
            CashMovementEntity(shiftId = shiftId, movementType = "KHATA_COLLECTION", amountPaise = 50000L, notes = "Khata collection"),
            CashMovementEntity(shiftId = shiftId, movementType = "EXPENSE_OUT", amountPaise = -30000L, notes = "Tea & snacks"),
            CashMovementEntity(shiftId = shiftId, movementType = "PURCHASE_PAYMENT", amountPaise = -70000L, notes = "Supplier payment")
        )

        val shiftDao = FakeShiftDao(shift)
        val cashMovementDao = FakeCashMovementDao(movements)
        val userDao = FakeUserDao(mapOf("admin-user" to UserEntity(id = "admin-user", username = "admin", displayName = "Admin Boss", pinHash = "", pinSalt = "")))
        val invoiceDao = createFakeInvoiceDao()

        val repository = ShiftRepositoryImpl(
            shiftDao = shiftDao,
            cashMovementDao = cashMovementDao,
            userDao = userDao,
            invoiceDao = invoiceDao
        )

        val result = repository.generateZReport(shiftId)
        assertTrue(result.isSuccess)

        val report = result.getOrNull()
        assertNotNull(report)
        report!!

        // Expected cash: 1000 + 2500 + 500 - 300 - 700 = 3000 (300000 paise)
        assertEquals(100000L, report.openingFloatPaise)
        assertEquals(250000L, report.cashSalesPaise)
        assertEquals(50000L, report.khataCollectionsPaise)
        assertEquals(30000L, report.expenseCashOutPaise)
        assertEquals(70000L, report.supplierPaymentPaise)
        assertEquals(300000L, report.expectedCashPaise)

        // Actual cash & Shortage variance
        assertEquals(295000L, report.actualCashPaise)
        assertEquals(-5000L, report.variancePaise) // -Rs. 50 shortage
        assertEquals("Admin Boss", report.managerName)
    }

    @Test
    fun testZReportExcessVariance() = runTest {
        val shiftId = 102L
        val openingFloat = 50000L // Rs. 500.00
        val actualCountedCash = 160000L // Rs. 1600 (Expected 1500, Rs. 100 excess)

        val shift = ShiftEntity(
            id = shiftId,
            userId = "user-m",
            openingFloatPaise = openingFloat,
            actualCashPaise = actualCountedCash,
            status = "CLOSED"
        )

        val movements = listOf(
            CashMovementEntity(shiftId = shiftId, movementType = "SALE_CASH", amountPaise = 100000L)
        )

        val repository = ShiftRepositoryImpl(
            shiftDao = FakeShiftDao(shift),
            cashMovementDao = FakeCashMovementDao(movements),
            userDao = FakeUserDao(),
            invoiceDao = createFakeInvoiceDao()
        )

        val report = repository.generateZReport(shiftId).getOrThrow()
        assertEquals(150000L, report.expectedCashPaise)
        assertEquals(160000L, report.actualCashPaise)
        assertEquals(10000L, report.variancePaise) // Excess Rs. 100
    }

    @Test
    fun testCashierWiseSalesBreakdownAndTotals() = runTest {
        val shiftId = 200L
        val shift = ShiftEntity(id = shiftId, userId = "mgr", openingFloatPaise = 50000L, status = "OPEN")

        val invoices = listOf(
            InvoiceEntity(
                id = 1L,
                invoiceNumber = "INV-001",
                shiftId = shiftId,
                createdByUserId = "cashier-1",
                grandTotalPaise = 150000L,
                subtotalPaise = 150000L,
                upiPaidPaise = 150000L,
                originalDuePaise = 0L
            ),
            InvoiceEntity(
                id = 2L,
                invoiceNumber = "INV-002",
                shiftId = shiftId,
                createdByUserId = "cashier-1",
                grandTotalPaise = 100000L,
                subtotalPaise = 100000L,
                upiPaidPaise = 0L,
                originalDuePaise = 0L
            ),
            InvoiceEntity(
                id = 3L,
                invoiceNumber = "INV-003",
                shiftId = shiftId,
                createdByUserId = "cashier-2",
                grandTotalPaise = 200000L,
                subtotalPaise = 200000L,
                upiPaidPaise = 50000L,
                originalDuePaise = 150000L // Credit sale
            )
        )

        val invoiceItems = listOf(
            InvoiceItemEntity(id = 1L, invoiceId = 1L, productName = "A", quantityMilli = 2000L, unitPricePaise = 75000L, lineTotalPaise = 150000L),
            InvoiceItemEntity(id = 2L, invoiceId = 2L, productName = "B", quantityMilli = 3000L, unitPricePaise = 33333L, lineTotalPaise = 100000L),
            InvoiceItemEntity(id = 3L, invoiceId = 3L, productName = "C", quantityMilli = 5000L, unitPricePaise = 40000L, lineTotalPaise = 200000L)
        )

        val users = mapOf(
            "cashier-1" to UserEntity(id = "cashier-1", username = "ramesh", displayName = "Ramesh Kumar", pinHash = "", pinSalt = ""),
            "cashier-2" to UserEntity(id = "cashier-2", username = "suresh", displayName = "Suresh Patel", pinHash = "", pinSalt = "")
        )

        val repository = ShiftRepositoryImpl(
            shiftDao = FakeShiftDao(shift),
            cashMovementDao = FakeCashMovementDao(),
            userDao = FakeUserDao(users),
            invoiceDao = createFakeInvoiceDao(invoices, invoiceItems)
        )

        val report = repository.generateZReport(shiftId).getOrThrow()

        assertEquals(3, report.invoicesCount)
        assertEquals(10L, report.itemsSoldCount) // 2 + 3 + 5 = 10 items
        assertEquals(450000L, report.totalTurnoverPaise) // 1500 + 1000 + 2000 = 4500
        assertEquals(200000L, report.upiSalesPaise) // 1500 + 500 = 2000
        assertEquals(150000L, report.creditSalesPaise) // 1500

        // Cashier breakdown
        assertEquals(2, report.cashierBreakdowns.size)
        val ramesh = report.cashierBreakdowns.first { it.userId == "cashier-1" }
        assertEquals("Ramesh Kumar", ramesh.userName)
        assertEquals(2, ramesh.invoiceCount)
        assertEquals(250000L, ramesh.totalSalesPaise)

        val suresh = report.cashierBreakdowns.first { it.userId == "cashier-2" }
        assertEquals("Suresh Patel", suresh.userName)
        assertEquals(1, suresh.invoiceCount)
        assertEquals(200000L, suresh.totalSalesPaise)
    }

    @Test
    fun testEscPosTextFormattingGeneratesValidZReport() = runTest {
        val shiftId = 300L
        val shift = ShiftEntity(
            id = shiftId,
            userId = "mgr-1",
            openedAt = 1700000000000L,
            closedAt = 1700028800000L,
            openingFloatPaise = 100000L,
            actualCashPaise = 295000L,
            expectedCashPaise = 300000L,
            variancePaise = -5000L,
            status = "CLOSED"
        )

        val movements = listOf(
            CashMovementEntity(shiftId = shiftId, movementType = "SALE_CASH", amountPaise = 200000L),
            CashMovementEntity(shiftId = shiftId, movementType = "EXPENSE_OUT", amountPaise = -20000L)
        )

        val repository = ShiftRepositoryImpl(
            shiftDao = FakeShiftDao(shift),
            cashMovementDao = FakeCashMovementDao(movements),
            userDao = FakeUserDao(mapOf("mgr-1" to UserEntity(id = "mgr-1", username = "mgr", displayName = "General Manager", pinHash = "", pinSalt = ""))),
            invoiceDao = createFakeInvoiceDao()
        )

        val report = repository.generateZReport(shiftId).getOrThrow()
        val shop = ShopProfile(name = "Dokandaar Retail", addressLine1 = "10 MG Road", phoneNumber = "9876543210")

        val bytes = EscPosFormatter.formatZReportText(report, shop, columns = 32)
        assertTrue(bytes.isNotEmpty())

        val text = String(bytes, Charsets.US_ASCII)
        assertTrue(text.contains("Z-REPORT / SHIFT SUMMARY"))
        assertTrue(text.contains("Dokandaar Retail"))
        assertTrue(text.contains("10 MG Road"))
        assertTrue(text.contains("Shift ID: #300"))
        assertTrue(text.contains("General Manager"))
        assertTrue(text.contains("CASH RECONCILIATION"))
        assertTrue(text.contains("Opening Float:"))
        assertTrue(text.contains("Cash Sales:"))
        assertTrue(text.contains("Expected Cash:"))
        assertTrue(text.contains("Actual Cash:"))
        assertTrue(text.contains("Variance:"))
        assertTrue(text.contains("SHORTAGE"))
        assertTrue(text.contains("SALES SUMMARY"))
        assertTrue(text.contains("*** END OF Z-REPORT ***"))
    }
}
