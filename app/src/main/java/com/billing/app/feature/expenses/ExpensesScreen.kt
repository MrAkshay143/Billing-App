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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.util.FeedbackManager
import com.billing.app.core.ui.components.DokandaarActionPill
import com.billing.app.core.ui.components.InputField
import com.billing.app.core.ui.components.SubPageHeader
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ExpensesScreen(
    expenseRepository: ExpenseRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = appStrings()
    val expenses by expenseRepository.getAllExpenses().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Shop Rent") }
    var amountStr by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("CASH") }

    val totalExpensesPaise = expenses.sumOf { it.amountPaise }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    var dateFilter by remember { mutableStateOf("All Time") }

    val filteredExpenses = remember(expenses, dateFilter) {
        when (dateFilter) {
            "Today" -> {
                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                expenses.filter { it.date >= startOfDay }
            }
            "This Month" -> {
                val startOfMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                expenses.filter { it.date >= startOfMonth }
            }
            else -> expenses
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(strings.addExpense, fontWeight = FontWeight.Bold, color = PrimaryPurple) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InputField(
                        label = strings.expenseTitle,
                        value = title,
                        onValueChange = { title = it },
                        placeholder = "e.g. Electricity Bill, Shop Rent, Tea"
                    )
                    InputField(
                        label = strings.expenseCategory,
                        value = category,
                        onValueChange = { category = it },
                        placeholder = "e.g. Rent, Utilities, Salary, Miscellaneous"
                    )

                    // Quick category selection chips
                    val quickCategories = listOf("Rent", "Electricity", "Salary", "Stock", "Transport", "Tea/Snacks", "Misc")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickCategories) { qc ->
                            val isSel = category.equals(qc, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) PrimaryPurple else DokandaarTheme.surfaceVariant,
                                modifier = Modifier.clickable { category = qc }
                            ) {
                                Text(
                                    text = qc,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else DokandaarTheme.textPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

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
                        } else {
                            FeedbackManager.showToast(context, "Please enter expense title and valid amount")
                        }
                    }
                ) {
                    Text(strings.save, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(strings.cancel, color = DokandaarTheme.textMuted)
                }
            },
            containerColor = DokandaarTheme.cardBackground,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Delete Confirmation Dialog
    if (expenseToDelete != null) {
        val exp = expenseToDelete!!
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text(strings.deleteExpenseTitle, fontWeight = FontWeight.Bold, color = ErrorColor) },
            text = { Text("Delete '${exp.title}' (${Money(exp.amountPaise).format()})? ${strings.deleteExpenseConfirm}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            expenseRepository.deleteExpense(exp.id)
                            expenseToDelete = null
                            FeedbackManager.showToast(context, strings.expenseDeletedToast)
                        }
                    }
                ) {
                    Text(strings.delete, color = ErrorColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text(strings.cancel, color = DokandaarTheme.textMuted)
                }
            },
            containerColor = DokandaarTheme.cardBackground,
            shape = RoundedCornerShape(16.dp)
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
                    DokandaarActionPill(
                        text = strings.add,
                        onClick = { showAddDialog = true }
                    )
                }
            )
        },
        containerColor = DokandaarTheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Date Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All Time", "Today", "This Month").forEach { filterOpt ->
                    val isSel = dateFilter == filterOpt
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) PrimaryPurple else DokandaarTheme.cardBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) PrimaryPurple else DokandaarTheme.border),
                        modifier = Modifier.clickable { dateFilter = filterOpt }
                    ) {
                        Text(
                            text = filterOpt,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color.White else DokandaarTheme.textPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
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
                            text = "No expenses recorded for this filter",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DokandaarTheme.textMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredExpenses, key = { it.id }) { exp ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border)
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
                                    expenseToDelete = exp
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
}
