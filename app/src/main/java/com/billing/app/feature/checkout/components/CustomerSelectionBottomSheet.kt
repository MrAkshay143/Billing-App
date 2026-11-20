package com.billing.app.feature.checkout.components

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.ErrorColor
import com.billing.app.core.ui.theme.PastelGreenContent
import com.billing.app.core.ui.theme.PastelPurpleBg
import com.billing.app.core.ui.theme.PrimaryPurple
import com.billing.app.core.util.Money
import com.billing.app.data.local.entity.CustomerEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerSelectionBottomSheet(
    customers: List<CustomerEntity>,
    onDismissRequest: () -> Unit,
    onSelectCustomer: (CustomerEntity) -> Unit,
    onSelectWalkIn: () -> Unit,
    onAddNewCustomer: () -> Unit,
    onReactivateCustomer: (CustomerEntity) -> Unit
) {
    val strings = appStrings()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var customerToReactivate by remember { mutableStateOf<CustomerEntity?>(null) }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) {
            customers
        } else {
            val q = searchQuery.trim().lowercase()
            customers.filter {
                it.name.lowercase().contains(q) || it.phone.contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = DokandaarTheme.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.customersTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DokandaarTheme.textPrimary
                    )
                    Text(
                        text = "Total ${customers.size} registered",
                        fontSize = 12.sp,
                        color = DokandaarTheme.textMuted
                    )
                }

                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = DokandaarTheme.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Walk-in & Add New
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Walk-in Customer Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PastelPurpleBg,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            onSelectWalkIn()
                            onDismissRequest()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.walkInCustomer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple
                        )
                    }
                }

                // Add New Customer Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryPurple,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            onAddNewCustomer()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.newCustomer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name or phone...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = PrimaryPurple
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = DokandaarTheme.textMuted
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Customer List
            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No customers match '$searchQuery'" else "No customers found",
                        fontSize = 13.sp,
                        color = DokandaarTheme.textMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        val isInactive = !customer.isActive
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isInactive) DokandaarTheme.cardBackground.copy(alpha = 0.6f) else DokandaarTheme.cardBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isInactive) Color.Gray.copy(alpha = 0.3f) else DokandaarTheme.border
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isInactive) {
                                        customerToReactivate = customer
                                    } else {
                                        onSelectCustomer(customer)
                                        onDismissRequest()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isInactive) Color.LightGray.copy(alpha = 0.4f) else PastelPurpleBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isInactive) Color.Gray else PrimaryPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = customer.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isInactive) DokandaarTheme.textMuted else DokandaarTheme.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isInactive) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFEF4444).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "Inactive",
                                                    color = Color(0xFFEF4444),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = customer.phone.ifBlank { "No phone" },
                                        fontSize = 11.sp,
                                        color = DokandaarTheme.textMuted
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Balance / Due info
                                Column(horizontalAlignment = Alignment.End) {
                                    val duePaise = customer.openingBalancePaise
                                    if (duePaise > 0L) {
                                        Text(
                                            text = "Due: ${Money(duePaise).format()}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ErrorColor
                                        )
                                    } else {
                                        Text(
                                            text = "Clear",
                                            fontSize = 11.sp,
                                            color = PastelGreenContent,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Inactive customer reactivation prompt
    customerToReactivate?.let { inactiveCust ->
        AlertDialog(
            onDismissRequest = { customerToReactivate = null },
            title = {
                Text(
                    text = strings.reactivateCustomerTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "This customer (${inactiveCust.name}) is marked inactive. Reactivate?",
                    fontSize = 14.sp,
                    color = DokandaarTheme.textPrimary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReactivateCustomer(inactiveCust)
                        onSelectCustomer(inactiveCust.copy(isActive = true))
                        customerToReactivate = null
                        onDismissRequest()
                    }
                ) {
                    Text(strings.reactivateAndSelect, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToReactivate = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
