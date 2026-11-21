package com.billing.app.data.repository

import androidx.room.withTransaction
import com.billing.app.core.database.AppDatabase
import com.billing.app.data.local.dao.CashMovementDao
import com.billing.app.data.local.dao.InvoiceDao
import com.billing.app.data.local.dao.ShiftDao
import com.billing.app.data.local.dao.UserDao
import com.billing.app.data.local.entity.CashMovementEntity
import com.billing.app.data.local.entity.ShiftEntity
import com.billing.app.domain.model.CashierSalesSummary
import com.billing.app.domain.model.ShiftZReport
import com.billing.app.domain.repository.ShiftRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class ShiftRepositoryImpl(
    private val shiftDao: ShiftDao,
    private val cashMovementDao: CashMovementDao,
    private val userDao: UserDao? = null,
    private val invoiceDao: InvoiceDao? = null,
    private val database: AppDatabase? = null
) : ShiftRepository {

    constructor(database: AppDatabase) : this(
        shiftDao = database.shiftDao(),
        cashMovementDao = database.cashMovementDao(),
        userDao = database.userDao(),
        invoiceDao = database.invoiceDao(),
        database = database
    )

    private suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return if (database != null) {
            database.withTransaction { block() }
        } else {
            block()
        }
    }

    override fun getActiveShiftFlow(): Flow<ShiftEntity?> =
        shiftDao.getCurrentOpenShiftFlow()

    override suspend fun getActiveShift(): ShiftEntity? = withContext(Dispatchers.IO) {
        shiftDao.getCurrentOpenShift()
    }

    override suspend fun openShift(
        userId: String,
        openingFloatPaise: Long,
        note: String?
    ): Result<ShiftEntity> = withContext(Dispatchers.IO) {
        try {
            if (getActiveShift() != null) {
                return@withContext Result.failure(IllegalStateException("A shift is already open"))
            }

            val shift = runInTransaction {
                val newShift = ShiftEntity(
                    userId = userId,
                    openingFloatPaise = openingFloatPaise,
                    note = note,
                    status = "OPEN"
                )
                val shiftId = shiftDao.insertShift(newShift)
                val createdShift = newShift.copy(id = shiftId)

                val initialMovement = CashMovementEntity(
                    shiftId = shiftId,
                    movementType = "CASH_IN",
                    amountPaise = openingFloatPaise,
                    reason = "Opening Float"
                )
                cashMovementDao.insertCashMovement(initialMovement)
                createdShift
            }
            Result.success(shift)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun closeShift(
        actualCashPaise: Long,
        note: String?
    ): Result<ShiftEntity> = withContext(Dispatchers.IO) {
        try {
            val activeShift = getActiveShift()
                ?: return@withContext Result.failure(IllegalStateException("No active shift open"))

            val expectedCashPaise = activeShift.openingFloatPaise + cashMovementDao.getNetCashForShift(activeShift.id)
            val variancePaise = actualCashPaise - expectedCashPaise
            val closedAt = System.currentTimeMillis()
            val finalNote = note ?: activeShift.note

            val updatedShift = activeShift.copy(
                closedAt = closedAt,
                actualCashPaise = actualCashPaise,
                expectedCashPaise = expectedCashPaise,
                variancePaise = variancePaise,
                note = finalNote,
                status = "CLOSED"
            )

            runInTransaction {
                shiftDao.updateShift(updatedShift)
            }
            Result.success(updatedShift)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordCashMovement(
        movementType: String,
        amountPaise: Long,
        referenceId: String?,
        reason: String?
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val activeShift = getActiveShift()
                ?: return@withContext Result.failure(IllegalStateException("No active shift open to record cash movement"))

            val movement = CashMovementEntity(
                shiftId = activeShift.id,
                movementType = movementType,
                amountPaise = amountPaise,
                referenceId = referenceId,
                reason = reason
            )
            val movementId = cashMovementDao.insertCashMovement(movement)
            Result.success(movementId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getMovementsForActiveShift(): Flow<List<CashMovementEntity>> {
        return getActiveShiftFlow().flatMapLatest { activeShift ->
            if (activeShift != null) {
                cashMovementDao.getCashMovementsForShift(activeShift.id)
            } else {
                flowOf(emptyList())
            }
        }
    }

    override suspend fun getExpectedCashForActiveShift(): Long = withContext(Dispatchers.IO) {
        val activeShift = getActiveShift() ?: return@withContext 0L
        val netMovements = cashMovementDao.getNetCashForShift(activeShift.id)
        activeShift.openingFloatPaise + netMovements
    }

    override suspend fun generateZReport(shiftId: Long): Result<ShiftZReport> = withContext(Dispatchers.IO) {
        try {
            val shift = shiftDao.getShiftById(shiftId)
                ?: return@withContext Result.failure(IllegalArgumentException("Shift not found with id: $shiftId"))

            val cashMovements = cashMovementDao.getCashMovementListForShift(shiftId)
            val invoices = invoiceDao?.getInvoicesByShiftId(shiftId) ?: emptyList()
            val invoiceItems = invoiceDao?.getInvoiceItemsByShiftId(shiftId) ?: emptyList()

            var cashSalesPaise = 0L
            var khataCollectionsPaise = 0L
            var expenseCashOutPaise = 0L
            var supplierPaymentPaise = 0L
            var otherCashInPaise = 0L
            var otherCashOutPaise = 0L
            var cashRefundsPaise = 0L
            var voidRefundsPaise = 0L

            for (movement in cashMovements) {
                when (movement.movementType) {
                    "SALE_CASH" -> cashSalesPaise += movement.amountPaise
                    "KHATA_COLLECTION" -> khataCollectionsPaise += movement.amountPaise
                    "EXPENSE_OUT" -> expenseCashOutPaise += kotlin.math.abs(movement.amountPaise)
                    "PURCHASE_PAYMENT" -> supplierPaymentPaise += kotlin.math.abs(movement.amountPaise)
                    "REFUND_CASH" -> cashRefundsPaise += kotlin.math.abs(movement.amountPaise)
                    "VOID_REFUND" -> voidRefundsPaise += kotlin.math.abs(movement.amountPaise)
                    "CASH_IN" -> {
                        if (movement.reason != "Opening Float" && movement.amountPaise > 0L) {
                            otherCashInPaise += movement.amountPaise
                        }
                    }
                    "CASH_OUT" -> {
                        otherCashOutPaise += kotlin.math.abs(movement.amountPaise)
                    }
                }
            }

            val openingFloatPaise = shift.openingFloatPaise
            val calculatedExpectedCash = openingFloatPaise + cashSalesPaise + khataCollectionsPaise + otherCashInPaise - expenseCashOutPaise - supplierPaymentPaise - otherCashOutPaise - cashRefundsPaise - voidRefundsPaise
            val expectedCashPaise = shift.expectedCashPaise ?: calculatedExpectedCash
            val actualCashPaise = shift.actualCashPaise ?: calculatedExpectedCash
            val variancePaise = shift.variancePaise ?: (actualCashPaise - expectedCashPaise)

            val upiSalesPaise = invoices.sumOf { it.upiPaidPaise }
            val creditSalesPaise = invoices.sumOf { it.originalDuePaise }
            val totalTurnoverPaise = invoices.sumOf { it.grandTotalPaise }
            val itemsSoldCount = invoiceItems.sumOf { it.quantityMilli / 1000L }

            val manager = userDao?.getUserById(shift.userId)
            val managerName = manager?.displayName?.ifBlank { null }
                ?: manager?.username?.ifBlank { null }
                ?: "Manager"

            val cashierBreakdowns = invoices.groupBy { it.createdByUserId ?: "Unknown" }
                .map { (userId, groupInvoices) ->
                    val user = if (userId != "Unknown") userDao?.getUserById(userId) else null
                    val userName = user?.displayName?.ifBlank { null }
                        ?: user?.username?.ifBlank { null }
                        ?: if (userId == "Unknown") "Cashier" else userId
                    CashierSalesSummary(
                        userId = userId,
                        userName = userName,
                        invoiceCount = groupInvoices.size,
                        totalSalesPaise = groupInvoices.sumOf { it.grandTotalPaise }
                    )
                }

            val report = ShiftZReport(
                shift = shift,
                managerName = managerName,
                openingFloatPaise = openingFloatPaise,
                cashSalesPaise = cashSalesPaise,
                upiSalesPaise = upiSalesPaise,
                creditSalesPaise = creditSalesPaise,
                khataCollectionsPaise = khataCollectionsPaise,
                expenseCashOutPaise = expenseCashOutPaise,
                supplierPaymentPaise = supplierPaymentPaise,
                otherCashInPaise = otherCashInPaise,
                otherCashOutPaise = otherCashOutPaise,
                expectedCashPaise = expectedCashPaise,
                actualCashPaise = actualCashPaise,
                variancePaise = variancePaise,
                totalTurnoverPaise = totalTurnoverPaise,
                invoicesCount = invoices.size,
                itemsSoldCount = itemsSoldCount,
                cashierBreakdowns = cashierBreakdowns,
                cashRefundsPaise = cashRefundsPaise,
                voidRefundsPaise = voidRefundsPaise
            )
            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
