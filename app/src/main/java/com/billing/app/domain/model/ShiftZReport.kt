package com.billing.app.domain.model

import com.billing.app.data.local.entity.ShiftEntity

data class CashierSalesSummary(
    val userId: String,
    val userName: String,
    val invoiceCount: Int,
    val totalSalesPaise: Long
)

data class ShiftZReport(
    val shift: ShiftEntity,
    val managerName: String = "Manager",
    val openingFloatPaise: Long,
    val cashSalesPaise: Long,
    val upiSalesPaise: Long,
    val creditSalesPaise: Long,
    val khataCollectionsPaise: Long,
    val expenseCashOutPaise: Long,
    val supplierPaymentPaise: Long,
    val otherCashInPaise: Long,
    val otherCashOutPaise: Long,
    val expectedCashPaise: Long,
    val actualCashPaise: Long,
    val variancePaise: Long,
    val totalTurnoverPaise: Long,
    val invoicesCount: Int,
    val itemsSoldCount: Long,
    val cashierBreakdowns: List<CashierSalesSummary>
)
