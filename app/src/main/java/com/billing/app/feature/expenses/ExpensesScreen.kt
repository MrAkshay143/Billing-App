package com.billing.app.feature.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.ui.components.InputField
import com.billing.app.core.ui.components.SubPageHeader
import com.billing.app.core.ui.theme.BorderColor
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.ErrorColor
import com.billing.app.core.ui.theme.PastelPinkBg
import com.billing.app.core.ui.theme.PastelPinkContent
import com.billing.app.core.ui.theme.PrimaryPurple
import com.billing.app.core.util.Money
import com.billing.app.data.local.entity.ExpenseEntity
import com.billing.app.domain.repository.ExpenseRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExpensesScreen(
    expenseRepository: ExpenseRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val expenses by expenseRepository.getAllExpenses().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Shop Rent") }
    var amountStr by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("CASH") }

    val totalExpensesPaise = expenses.sumOf { it.amountPaise }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Expense", fontWeight = FontWeight.Bold, color = PrimaryPurple) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InputField(
                        label = "Expense Title *",
                        value = title,
                        onValueChange = { title = it },
                        placeholder = "e.g. Electricity Bill, Shop Rent, Tea"
                    )
                    InputField(
                        label = "Category",
                        value = category,
                        onValueChange = { category = it },
                        placeholder = "e.g. Rent, Utilities, Salary, Miscellaneous"
                    )
                    InputField(
                        label = "Amount (₹) *",
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        placeholder = "0.00",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val money = Money.fromRupees(amountStr)
                        if (title.isNotBlank() && money.paise > 0L) {
                            scope.launch {
                                expenseRepository.addExpense(
                                    ExpenseEntity(
                                        title = title.trim(),
                                        category = category.trim(),
                                        amountPaise = money.paise,
                                        date = System.currentTimeMillis(),
                                        paymentMethod = paymentMethod,
                                        description = ""
                                    )
                                )
                                showAddDialog = false
                                title = ""
                                amountStr = ""
                            }
                        }
                    }
                ) {
                    Text("Save", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = DokandaarTheme.textMuted)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            SubPageHeader(
                title = "Expenses",
                subtitle = "Total: ${Money(totalExpensesPaise).format()}",
                icon = Icons.Default.Payments,
                onNavigateBack = onNavigateBack,
                actions = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        modifier = Modifier.clickable { showAddDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add",
                                color = PrimaryPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        },
        containerColor = DokandaarTheme.background
    ) { paddingValues ->
        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = DokandaarTheme.textMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No expenses recorded yet",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DokandaarTheme.textMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(expenses, key = { it.id }) { exp ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(PastelPinkBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = PastelPinkContent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exp.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DokandaarTheme.textPrimary
                                )
                                Text(
                                    text = "${exp.category} • ${dateFormat.format(Date(exp.date))}",
                                    fontSize = 11.sp,
                                    color = DokandaarTheme.textMuted
                                )
                            }
                            Text(
                                text = Money(exp.amountPaise).format(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PastelPinkContent
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        expenseRepository.deleteExpense(exp.id)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorColor, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
