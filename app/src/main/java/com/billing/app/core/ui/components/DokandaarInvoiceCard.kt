package com.billing.app.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.ErrorColor
import com.billing.app.core.ui.theme.PastelBlueBg
import com.billing.app.core.ui.theme.PastelBlueContent
import com.billing.app.core.ui.theme.PastelGreenBg
import com.billing.app.core.ui.theme.PastelGreenContent
import com.billing.app.core.ui.theme.PastelOrangeBg
import com.billing.app.core.ui.theme.PastelOrangeContent
import com.billing.app.core.ui.theme.PastelPurpleBg
import com.billing.app.core.ui.theme.PastelRedBg
import com.billing.app.core.ui.theme.PastelRedContent
import com.billing.app.core.ui.theme.PrimaryPurple
import com.billing.app.core.util.Money
import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceStatus

/**
 * Standardized Dokandaar Invoice Item Card matching Reference Snapshot 5.
 *
 * Features:
 * 1. Left Section:
 *    - Purple Document / Receipt Icon in rounded square box
 *    - Invoice Number (bold), Customer Name (medium), Phone Number (with phone icon)
 * 2. Vertical Divider:
 *    - Prominent thin vertical separator line between Left and Middle sections
 * 3. Middle Section:
 *    - Calendar icon + Date & Time
 *    - Shopping cart icon + Items Count
 *    - Status Badge Capsule (Paid, Pending, Returned, Draft) with icon and colored pill
 * 4. Right Section:
 *    - Total Amount (large bold purple, or red for returns)
 *    - 3-dots Menu with full action dropdown (View Details, Print, Settle Due, Resume Draft, etc.)
 */
@Composable
fun DokandaarInvoiceCard(
    invoice: InvoiceEntity,
    formattedDate: String,
    onViewDetails: () -> Unit,
    onPrint: () -> Unit,
    modifier: Modifier = Modifier,
    onResumeDraft: () -> Unit = {},
    onDiscardDraft: () -> Unit = {},
    onSettleDue: () -> Unit = {},
    onVoidInvoice: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val strings = appStrings()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (invoice.status == InvoiceStatus.DRAFT) {
                    onResumeDraft()
                } else {
                    onViewDetails()
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
        border = BorderStroke(1.dp, DokandaarTheme.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Purple Document Icon Box
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (DokandaarTheme.isDark) PastelPurpleBg.copy(alpha = 0.2f) else PastelPurpleBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Section 1: Customer & Invoice Details
            Column(
                modifier = Modifier.weight(1.05f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = invoice.invoiceNumber,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = DokandaarTheme.textPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = invoice.customerName?.ifBlank { strings.walkInCustomer } ?: strings.walkInCustomer,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = DokandaarTheme.textPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                if (!invoice.customerPhone.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = DokandaarTheme.textMuted,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = invoice.customerPhone,
                            fontSize = 10.5.sp,
                            color = DokandaarTheme.textMuted,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // VERTICAL DIVIDER LINE matching Screenshot 5
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(48.dp)
                    .background(DokandaarTheme.border)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Section 2: Date, Items Count & Status Badge
            Column(
                modifier = Modifier.weight(1.15f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = DokandaarTheme.textMuted,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 10.sp,
                        color = DokandaarTheme.textMuted,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = DokandaarTheme.textMuted,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${invoice.itemsCount} ${strings.itemsCount}",
                        fontSize = 10.sp,
                        color = DokandaarTheme.textMuted,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Status Badge
                val (badgeBg, badgeText, badgeColor, badgeIcon) = when {
                    invoice.status == InvoiceStatus.DRAFT -> listOf(PastelBlueBg, strings.badgeDraft, PastelBlueContent, Icons.Default.Edit)
                    invoice.status == InvoiceStatus.CANCELLED -> listOf(PastelRedBg, strings.badgeReturned, PastelRedContent, Icons.AutoMirrored.Filled.Undo)
                    invoice.duePaise > 0 -> listOf(PastelOrangeBg, strings.badgePending, PastelOrangeContent, Icons.Default.Schedule)
                    else -> listOf(PastelGreenBg, strings.badgePaid, PastelGreenContent, Icons.Default.CheckCircle)
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (DokandaarTheme.isDark) (badgeColor as Color).copy(alpha = 0.18f) else (badgeBg as Color)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = badgeIcon as ImageVector,
                            contentDescription = null,
                            tint = badgeColor as Color,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = badgeText as String,
                            color = badgeColor,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Section 3: Amount & 3-dots Menu
            val isCancelled = invoice.status == InvoiceStatus.CANCELLED
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isCancelled) "-${Money(invoice.grandTotalPaise).format()}" else Money(invoice.grandTotalPaise).format(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCancelled) ErrorColor else PrimaryPurple,
                    maxLines = 1,
                    softWrap = false
                )

                Spacer(modifier = Modifier.width(2.dp))

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = strings.cdOptions,
                            tint = DokandaarTheme.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.viewDetails) },
                            onClick = {
                                menuExpanded = false
                                onViewDetails()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(strings.printReceipt) },
                            onClick = {
                                menuExpanded = false
                                onPrint()
                            }
                        )
                        if (invoice.status == InvoiceStatus.DRAFT) {
                            DropdownMenuItem(
                                text = { Text(strings.resumeInPos, color = PrimaryPurple, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    menuExpanded = false
                                    onResumeDraft()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.discardDraft, color = ErrorColor) },
                                onClick = {
                                    menuExpanded = false
                                    onDiscardDraft()
                                }
                            )
                        }
                        if (invoice.status != InvoiceStatus.DRAFT && invoice.status != InvoiceStatus.CANCELLED) {
                            if (invoice.duePaise > 0) {
                                DropdownMenuItem(
                                    text = { Text("${strings.settleDueTitle} (${Money(invoice.duePaise).format()})", color = PastelOrangeContent, fontWeight = FontWeight.SemiBold) },
                                    onClick = {
                                        menuExpanded = false
                                        onSettleDue()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(strings.voidOrReturnInvoice, color = ErrorColor) },
                                onClick = {
                                    menuExpanded = false
                                    onVoidInvoice()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
