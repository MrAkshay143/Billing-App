package com.billing.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.BillingApplication
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.HeaderGradientStart
import com.billing.app.core.ui.theme.PastelGreenBg
import com.billing.app.core.ui.theme.PastelGreenContent
import com.billing.app.core.ui.theme.PastelOrangeBg
import com.billing.app.core.ui.theme.PastelOrangeContent
import com.billing.app.core.ui.theme.PastelRedBg
import com.billing.app.core.ui.theme.PastelRedContent
import com.billing.app.core.util.Money
import com.billing.app.domain.model.ShiftZReport
import com.billing.app.domain.repository.PrinterRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ShiftSummaryDialog(
    zReport: ShiftZReport,
    printerRepository: PrinterRepository = BillingApplication.instance.printerRepository,
    storeName: String = "Dokandaar Store",
    onDismissRequest: () -> Unit
) {
    val strings = appStrings()
    val coroutineScope = rememberCoroutineScope()
    var isPrinting by remember { mutableStateOf(false) }
    var printStatus by remember { mutableStateOf<String?>(null) }
    var isPrintSuccess by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val openedAtStr = remember(zReport.shift.openedAt) { dateFormat.format(Date(zReport.shift.openedAt)) }
    val closedAtStr = remember(zReport.shift.closedAt) {
        zReport.shift.closedAt?.let { dateFormat.format(Date(it)) } ?: "In Progress"
    }

    val doPrint: () -> Unit = {
        if (!isPrinting) {
            coroutineScope.launch {
                isPrinting = true
                printStatus = "Printing Z-Report..."
                val result = printerRepository.printZReport(zReport)
                isPrinting = false
                result.fold(
                    onSuccess = {
                        isPrintSuccess = true
                        printStatus = "Z-Report printed successfully!"
                    },
                    onFailure = { error ->
                        isPrintSuccess = false
                        printStatus = error.message ?: "Printer not connected"
                    }
                )
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(20.dp),
        containerColor = DokandaarTheme.cardBackground,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    tint = HeaderGradientStart,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = strings.shiftZReportTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DokandaarTheme.textPrimary
                    )
                    Text(
                        text = storeName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = DokandaarTheme.textMuted
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Metadata Header Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (DokandaarTheme.isDark) DokandaarTheme.surfaceVariant else Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Shift #${zReport.shift.id}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = HeaderGradientStart
                            )
                            Text(
                                text = "Manager: ${zReport.managerName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = DokandaarTheme.textPrimary
                            )
                        }
                        Text(
                            text = "Opened: $openedAtStr",
                            fontSize = 11.sp,
                            color = DokandaarTheme.textMuted
                        )
                        Text(
                            text = "Closed: $closedAtStr",
                            fontSize = 11.sp,
                            color = DokandaarTheme.textMuted
                        )
                    }
                }

                // 1. Cash Drawer Reconciliation Card
                SectionCard(title = "Cash Drawer Reconciliation / ক্যাশ মিলান") {
                    SummaryRow(
                        label = "Expected Cash in Drawer:",
                        value = Money(zReport.expectedCashPaise).format(),
                        isBold = true
                    )
                    SummaryRow(
                        label = "Actual Counted Cash:",
                        value = Money(zReport.actualCashPaise).format(),
                        isBold = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Variance Badge
                    val variancePaise = zReport.variancePaise
                    val badgeBg = when {
                        variancePaise == 0L -> if (DokandaarTheme.isDark) DokandaarTheme.surfaceVariant else PastelGreenBg
                        variancePaise < 0L -> if (DokandaarTheme.isDark) DokandaarTheme.surfaceVariant else PastelRedBg
                        else -> if (DokandaarTheme.isDark) DokandaarTheme.surfaceVariant else PastelOrangeBg
                    }
                    val badgeTextColor = when {
                        variancePaise == 0L -> PastelGreenContent
                        variancePaise < 0L -> PastelRedContent
                        else -> PastelOrangeContent
                    }
                    val badgeLabel = when {
                        variancePaise == 0L -> "Balanced / নিখুঁত (${Money(0L).format()})"
                        variancePaise < 0L -> "Shortage / ঘাটতি: -${Money(kotlin.math.abs(variancePaise)).format()}"
                        else -> "Excess / অতিরিক্ত: +${Money(variancePaise).format()}"
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeBg)
                            .border(1.dp, badgeTextColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(vertical = 6.dp, horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor
                        )
                    }
                }

                // 2. Financial Breakdown Card
                SectionCard(title = "Cash Flow Breakdown / নগদ প্রবাহ") {
                    SummaryRow(label = "Opening Float:", value = Money(zReport.openingFloatPaise).format())
                    SummaryRow(label = "Cash Sales:", value = "+${Money(zReport.cashSalesPaise).format()}", valueColor = PastelGreenContent)
                    SummaryRow(label = "Khata Collections:", value = "+${Money(zReport.khataCollectionsPaise).format()}", valueColor = PastelGreenContent)
                    if (zReport.otherCashInPaise > 0L) {
                        SummaryRow(label = "Other Cash In:", value = "+${Money(zReport.otherCashInPaise).format()}", valueColor = PastelGreenContent)
                    }
                    SummaryRow(label = "Cash Expenses:", value = "-${Money(zReport.expenseCashOutPaise).format()}", valueColor = PastelRedContent)
                    SummaryRow(label = "Supplier Payments:", value = "-${Money(zReport.supplierPaymentPaise).format()}", valueColor = PastelRedContent)
                    if (zReport.otherCashOutPaise > 0L) {
                        SummaryRow(label = "Other Cash Out:", value = "-${Money(zReport.otherCashOutPaise).format()}", valueColor = PastelRedContent)
                    }
                }

                // 3. Digital & Total Sales Card
                SectionCard(title = "Digital & Total Turnover / মোট লেনদেন") {
                    SummaryRow(label = "UPI / Online Sales:", value = Money(zReport.upiSalesPaise).format())
                    SummaryRow(label = "Credit (Khata) Sales:", value = Money(zReport.creditSalesPaise).format())
                    SummaryRow(label = "Total Invoices:", value = "${zReport.invoicesCount}")
                    SummaryRow(label = "Total Items Sold:", value = "${zReport.itemsSoldCount}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = DokandaarTheme.border)
                    SummaryRow(
                        label = "Gross Turnover:",
                        value = Money(zReport.totalTurnoverPaise).format(),
                        valueColor = HeaderGradientStart,
                        isBold = true
                    )
                }

                // 4. Cashier-wise Breakdown List
                if (zReport.cashierBreakdowns.isNotEmpty()) {
                    SectionCard(title = "Cashier-wise Performance / ক্যাশিয়ার ভিত্তিক") {
                        zReport.cashierBreakdowns.forEach { cashier ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = cashier.userName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DokandaarTheme.textPrimary
                                    )
                                    Text(
                                        text = "${cashier.invoiceCount} invoices",
                                        fontSize = 11.sp,
                                        color = DokandaarTheme.textMuted
                                    )
                                }
                                Text(
                                    text = Money(cashier.totalSalesPaise).format(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DokandaarTheme.textPrimary
                                )
                            }
                        }
                    }
                }

                // Print feedback message if any
                if (printStatus != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isPrintSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isPrintSuccess) PastelGreenContent else Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = printStatus ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isPrintSuccess) PastelGreenContent else Color(0xFFDC2626)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismissRequest,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HeaderGradientStart)
            ) {
                Text(
                    text = strings.done,
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = doPrint,
                enabled = !isPrinting,
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isPrinting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = HeaderGradientStart
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.printing,
                        fontSize = 12.sp,
                        color = HeaderGradientStart
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = HeaderGradientStart
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.printZReportThermal,
                        fontSize = 12.sp,
                        color = HeaderGradientStart
                    )
                }
            }
        }
    )
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (DokandaarTheme.isDark) DokandaarTheme.surfaceVariant else Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = DokandaarTheme.textPrimary
            )
            content()
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = DokandaarTheme.textPrimary,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = DokandaarTheme.textMuted
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = valueColor
        )
    }
}
