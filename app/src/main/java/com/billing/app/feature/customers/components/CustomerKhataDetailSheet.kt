package com.billing.app.feature.customers.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.ErrorColor
import com.billing.app.core.ui.theme.PastelBlueBg
import com.billing.app.core.ui.theme.PastelBlueContent
import com.billing.app.core.ui.theme.PastelGreenBg
import com.billing.app.core.ui.theme.PastelGreenContent
import com.billing.app.core.ui.theme.PastelOrangeBg
import com.billing.app.core.ui.theme.PastelOrangeContent
import com.billing.app.core.ui.theme.PastelPurpleBg
import com.billing.app.core.ui.theme.PastelPurpleContent
import com.billing.app.core.ui.theme.PastelRedBg
import com.billing.app.core.ui.theme.PastelRedContent
import com.billing.app.core.ui.theme.PastelSlateBg
import com.billing.app.core.ui.theme.PastelSlateContent
import com.billing.app.core.ui.theme.PrimaryPurple
import com.billing.app.core.ui.util.FeedbackManager
import com.billing.app.core.util.Money
import com.billing.app.data.local.entity.CustomerEntity
import com.billing.app.data.local.entity.CustomerLedgerEntity
import com.billing.app.domain.model.ShopProfile
import com.billing.app.domain.repository.CustomerRepository
import com.billing.app.domain.repository.ShiftRepository
import com.billing.app.domain.repository.ShopRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerKhataDetailSheet(
    customer: CustomerEntity,
    customerRepository: CustomerRepository,
    shiftRepository: ShiftRepository,
    shopRepository: ShopRepository,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val ledgerEntries by customerRepository.getCustomerLedger(customer.id).collectAsState(initial = emptyList())
    val liveBalance by customerRepository.getCustomerBalance(customer.id).collectAsState(initial = customer.openingBalancePaise)
    val shopProfile by shopRepository.getShopProfile().collectAsState(initial = ShopProfile())

    val currentDuePaise = if (ledgerEntries.isNotEmpty()) liveBalance else customer.openingBalancePaise

    var showReceivePaymentDialog by remember { mutableStateOf(false) }

    if (showReceivePaymentDialog) {
        ReceivePaymentDialog(
            customer = customer,
            currentDuePaise = currentDuePaise,
            customerRepository = customerRepository,
            shiftRepository = shiftRepository,
            onDismissRequest = { showReceivePaymentDialog = false }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = DokandaarTheme.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header: Customer Avatar, Info & Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(PastelPurpleBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DokandaarTheme.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (customer.phone.isNotBlank()) {
                        Text(
                            text = customer.phone,
                            fontSize = 13.sp,
                            color = DokandaarTheme.textMuted
                        )
                    }
                    if (!customer.address.isNullOrBlank()) {
                        Text(
                            text = customer.address,
                            fontSize = 12.sp,
                            color = DokandaarTheme.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = DokandaarTheme.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Due Balance & Credit Limit Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentDuePaise > 0L) PastelOrangeBg.copy(alpha = 0.5f) else PastelGreenBg.copy(alpha = 0.5f)
                ),
                border = BorderStroke(
                    1.dp,
                    if (currentDuePaise > 0L) PastelOrangeContent.copy(alpha = 0.4f) else PastelGreenContent.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Current Due Balance / মোট বাকি",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = DokandaarTheme.textMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = Money(currentDuePaise).format(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (currentDuePaise > 0L) PastelOrangeContent else PastelGreenContent
                            )
                        }

                        // Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (currentDuePaise > 0L) PastelOrangeBg else PastelGreenBg,
                            border = BorderStroke(
                                1.dp,
                                if (currentDuePaise > 0L) PastelOrangeContent else PastelGreenContent
                            )
                        ) {
                            Text(
                                text = if (currentDuePaise > 0L) "Due / বাকি" else "Clear / Advance / পরিশোধ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentDuePaise > 0L) PastelOrangeContent else PastelGreenContent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        color = (if (currentDuePaise > 0L) PastelOrangeContent else PastelGreenContent).copy(alpha = 0.2f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Credit Limit Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = DokandaarTheme.textMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (customer.creditLimitPaise > 0L) {
                            val available = maxOf(0L, customer.creditLimitPaise - currentDuePaise)
                            Text(
                                text = "Credit Limit: ${Money(customer.creditLimitPaise).format()} | Available: ${Money(available).format()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DokandaarTheme.textPrimary
                            )
                        } else {
                            Text(
                                text = "Credit Limit: No limit set",
                                fontSize = 12.sp,
                                color = DokandaarTheme.textMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons:
            // Row 1: + Receive Payment / জমা নিন
            Button(
                onClick = { showReceivePaymentDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                Icon(
                    imageVector = Icons.Default.Payment,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "+ Receive Payment / জমা নিন",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: WhatsApp Reminder & Call Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // WhatsApp Payment Reminder Button
                OutlinedButton(
                    onClick = {
                        if (customer.phone.isBlank()) {
                            FeedbackManager.showToast(context, "Customer phone number is missing")
                        } else {
                            val url = customerRepository.generatePaymentReminderWhatsAppUrl(
                                customer = customer,
                                duePaise = currentDuePaise,
                                upiId = shopProfile.upiId,
                                shopName = shopProfile.name
                            )
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                FeedbackManager.showToast(context, "Could not open WhatsApp: ${e.message}")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PastelGreenBg.copy(alpha = 0.4f),
                        contentColor = Color(0xFF15803D)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF22C55E))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "WhatsApp",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF15803D)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WhatsApp / তাগাদা",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Call Button
                OutlinedButton(
                    onClick = {
                        if (customer.phone.isBlank()) {
                            FeedbackManager.showToast(context, "Customer phone number is missing")
                        } else {
                            try {
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone.trim()}"))
                                context.startActivity(dialIntent)
                            } catch (e: Exception) {
                                FeedbackManager.showToast(context, "Could not open dialer")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PastelBlueBg.copy(alpha = 0.4f),
                        contentColor = PastelBlueContent
                    ),
                    border = BorderStroke(1.dp, PastelBlueContent.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call",
                        modifier = Modifier.size(16.dp),
                        tint = PastelBlueContent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Call / কল করুন",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Double-Entry Ledger Statement Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ListAlt,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ledger Statement / খাতা লেনদেন",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DokandaarTheme.textPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PastelSlateBg
                ) {
                    Text(
                        text = "${ledgerEntries.size} entries",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PastelSlateContent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Double-Entry Ledger List
            if (ledgerEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ListAlt,
                            contentDescription = null,
                            tint = DokandaarTheme.textMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No ledger transactions yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DokandaarTheme.textMuted
                        )
                        Text(
                            text = "Credit sales and payments will appear here.",
                            fontSize = 12.sp,
                            color = DokandaarTheme.textMuted
                        )
                    }
                }
            } else {
                val sortedEntries = remember(ledgerEntries) {
                    ledgerEntries.sortedByDescending { it.createdAt }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedEntries, key = { it.id }) { entry ->
                        CustomerLedgerEntryCard(entry)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerLedgerEntryCard(entry: CustomerLedgerEntity) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(entry.createdAt) { dateFormat.format(Date(entry.createdAt)) }

    val config = remember(entry.transactionType) {
        when (entry.transactionType) {
            "OPENING_BALANCE" -> LedgerTypeConfig(
                label = "Opening Balance / পূর্বের বাকি",
                badgeBg = PastelOrangeBg,
                badgeContent = PastelOrangeContent,
                icon = Icons.Default.AccountBalance,
                isDebit = true
            )
            "CREDIT_SALE" -> LedgerTypeConfig(
                label = "Credit Sale / বাকি বিক্রয়",
                badgeBg = PastelRedBg,
                badgeContent = PastelRedContent,
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                isDebit = true
            )
            "PAYMENT" -> LedgerTypeConfig(
                label = "Payment (${entry.paymentMode ?: "CASH"}) / জমা",
                badgeBg = PastelGreenBg,
                badgeContent = PastelGreenContent,
                icon = Icons.Default.CheckCircle,
                isDebit = false
            )
            "RETURN_CREDIT" -> LedgerTypeConfig(
                label = "Return Credit / ফেরত জমা",
                badgeBg = PastelBlueBg,
                badgeContent = PastelBlueContent,
                icon = Icons.AutoMirrored.Filled.Undo,
                isDebit = false
            )
            else -> LedgerTypeConfig(
                label = entry.transactionType,
                badgeBg = if (entry.debitPaise > 0) PastelRedBg else PastelGreenBg,
                badgeContent = if (entry.debitPaise > 0) PastelRedContent else PastelGreenContent,
                icon = Icons.AutoMirrored.Filled.ListAlt,
                isDebit = entry.debitPaise > 0
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
        border = BorderStroke(1.dp, DokandaarTheme.border)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Type Badge + Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = config.badgeBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = config.icon,
                            contentDescription = null,
                            tint = config.badgeContent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = config.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = config.badgeContent
                        )
                    }
                }

                // Amount
                val amountText = if (config.isDebit) {
                    "+${Money(entry.debitPaise).format()}"
                } else {
                    "-${Money(entry.creditPaise).format()}"
                }
                val amountColor = if (config.isDebit) ErrorColor else PastelGreenContent

                Text(
                    text = amountText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = amountColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Note or Invoice Reference
            val noteText = buildString {
                if (entry.invoiceId != null) {
                    append("Invoice #${entry.invoiceId}")
                }
                if (!entry.note.isNullOrBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(entry.note)
                }
            }
            if (noteText.isNotBlank()) {
                Text(
                    text = noteText,
                    fontSize = 12.sp,
                    color = DokandaarTheme.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Footer Row: Date & Balance After
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = DokandaarTheme.textMuted
                )

                Text(
                    text = "Balance: ${Money(entry.balanceAfterPaise).format()}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (entry.balanceAfterPaise > 0L) PastelOrangeContent else DokandaarTheme.textMuted
                )
            }
        }
    }
}

private data class LedgerTypeConfig(
    val label: String,
    val badgeBg: Color,
    val badgeContent: Color,
    val icon: ImageVector,
    val isDebit: Boolean
)
