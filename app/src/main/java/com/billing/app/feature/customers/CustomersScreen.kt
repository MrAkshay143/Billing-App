package com.billing.app.feature.customers

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableIntStateOf
import com.billing.app.core.pagination.OnBottomReached
import com.billing.app.core.pagination.LazyLoadingFooter
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.BillingApplication
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.util.FeedbackManager
import com.billing.app.core.ui.components.DokandaarActionPill
import com.billing.app.core.ui.components.DokandaarSearchBar
import com.billing.app.core.ui.components.InputField
import com.billing.app.core.ui.components.SubPageHeader
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.ErrorColor
import com.billing.app.core.ui.theme.PastelBlueBg
import com.billing.app.core.ui.theme.PastelBlueContent
import com.billing.app.core.ui.theme.PastelGreenContent
import com.billing.app.core.ui.theme.PastelOrangeBg
import com.billing.app.core.ui.theme.PastelOrangeContent
import com.billing.app.core.ui.theme.PrimaryPurple
import com.billing.app.core.util.Money
import com.billing.app.data.local.entity.CustomerEntity
import com.billing.app.domain.repository.CustomerRepository
import com.billing.app.domain.repository.ShiftRepository
import com.billing.app.domain.repository.ShopRepository
import com.billing.app.feature.customers.components.CustomerKhataDetailSheet
import kotlinx.coroutines.launch

@Composable
fun CustomersScreen(
    customerRepository: CustomerRepository,
    shiftRepository: ShiftRepository = BillingApplication.instance.shiftRepository,
    shopRepository: ShopRepository = BillingApplication.instance.shopRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = appStrings()
    val customers by customerRepository.getAllCustomers().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerToDelete by remember { mutableStateOf<CustomerEntity?>(null) }
    var selectedCustomerForKhata by remember { mutableStateOf<CustomerEntity?>(null) }

    var formName by remember { mutableStateOf("") }
    var formPhone by remember { mutableStateOf("") }
    var formEmail by remember { mutableStateOf("") }
    var formAddress by remember { mutableStateOf("") }

    var visibleItemCount by remember { mutableIntStateOf(20) }
    var isLoadingMore by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        visibleItemCount = 20
        isLoadingMore = false
    }

    val filteredCustomers = customers.filter {
        searchQuery.isBlank() ||
        it.name.contains(searchQuery.trim(), ignoreCase = true) ||
        it.phone.contains(searchQuery.trim())
    }

    val displayedCustomers = remember(filteredCustomers, visibleItemCount) {
        filteredCustomers.take(visibleItemCount)
    }
    val hasMore = visibleItemCount < filteredCustomers.size

    val listState = rememberLazyListState()
    listState.OnBottomReached(buffer = 5, enabled = hasMore && !isLoadingMore) {
        if (!isLoadingMore && hasMore) {
            isLoadingMore = true
            scope.launch {
                delay(150)
                visibleItemCount = minOf(visibleItemCount + 20, filteredCustomers.size)
                isLoadingMore = false
            }
        }
    }

    if (showAddDialog || customerToEdit != null) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                customerToEdit = null
            },
            title = {
                Text(
                    text = if (customerToEdit == null) strings.addCustomer else strings.editCustomer,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InputField(
                        label = strings.customerName,
                        value = formName,
                        onValueChange = { formName = it },
                        placeholder = "e.g. Ramesh Kumar"
                    )
                    InputField(
                        label = strings.customerPhone,
                        value = formPhone,
                        onValueChange = { formPhone = it },
                        placeholder = "e.g. 9876543210",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    InputField(
                        label = strings.customerAddress,
                        value = formAddress,
                        onValueChange = { formAddress = it },
                        placeholder = "e.g. Shop 4, Main Market"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (formName.isNotBlank() && formPhone.isNotBlank()) {
                            scope.launch {
                                if (customerToEdit == null) {
                                    customerRepository.addCustomer(
                                        CustomerEntity(
                                            name = formName.trim(),
                                            phone = formPhone.trim(),
                                            email = formEmail.trim().ifBlank { null },
                                            address = formAddress.trim().ifBlank { null }
                                        )
                                    )
                                } else {
                                    customerRepository.updateCustomer(
                                        customerToEdit!!.copy(
                                            name = formName.trim(),
                                            phone = formPhone.trim(),
                                            address = formAddress.trim()
                                        )
                                    )
                                }
                                showAddDialog = false
                                customerToEdit = null
                                formName = ""
                                formPhone = ""
                                formAddress = ""
                            }
                        } else {
                            FeedbackManager.showToast(context, "Please enter customer name and 10-digit mobile number")
                        }
                    }
                ) {
                    Text(strings.save, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        customerToEdit = null
                    }
                ) {
                    Text(strings.cancel, color = DokandaarTheme.textMuted)
                }
            },
            containerColor = DokandaarTheme.cardBackground,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Delete Confirmation Dialog
    if (customerToDelete != null) {
        val cust = customerToDelete!!
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text(strings.deleteCustomerTitle, fontWeight = FontWeight.Bold, color = ErrorColor) },
            text = { Text("Remove '${cust.name}'? ${strings.deleteCustomerConfirm}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            customerRepository.deleteCustomer(cust.id)
                            customerToDelete = null
                            FeedbackManager.showToast(context, strings.customerDeletedToast)
                        }
                    }
                ) {
                    Text(strings.delete, color = ErrorColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) {
                    Text(strings.cancel, color = DokandaarTheme.textMuted)
                }
            },
            containerColor = DokandaarTheme.cardBackground,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (selectedCustomerForKhata != null) {
        val liveCustomer = customers.find { it.id == selectedCustomerForKhata!!.id } ?: selectedCustomerForKhata!!
        CustomerKhataDetailSheet(
            customer = liveCustomer,
            customerRepository = customerRepository,
            shiftRepository = shiftRepository,
            shopRepository = shopRepository,
            onDismissRequest = { selectedCustomerForKhata = null }
        )
    }

    Scaffold(
        topBar = {
            SubPageHeader(
                title = strings.customersTitle,
                subtitle = "${customers.size} ${strings.customers} • Khata",
                icon = Icons.Default.People,
                onNavigateBack = onNavigateBack,
                actions = {
                    DokandaarActionPill(
                        text = strings.add,
                        onClick = {
                            formName = ""
                            formPhone = ""
                            formAddress = ""
                            showAddDialog = true
                        }
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
            // Search field
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                DokandaarSearchBar(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search by name or mobile number...",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = DokandaarTheme.textMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = strings.noCustomersFound,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DokandaarTheme.textMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayedCustomers, key = { it.id }) { cust ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCustomerForKhata = cust },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(PastelBlueBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = PastelBlueContent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cust.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DokandaarTheme.textPrimary
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable(enabled = cust.phone.isNotBlank()) {
                                            try {
                                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cust.phone.trim()}"))
                                                context.startActivity(dialIntent)
                                            } catch (_: Exception) {}
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "Call",
                                            tint = if (cust.phone.isNotBlank()) PrimaryPurple else DokandaarTheme.textMuted,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = cust.phone.ifBlank { strings.noPhone },
                                            fontSize = 12.sp,
                                            color = if (cust.phone.isNotBlank()) PrimaryPurple else DokandaarTheme.textMuted
                                        )
                                    }
                                    if (cust.openingBalancePaise != 0L) {
                                        Text(
                                            text = if (cust.openingBalancePaise > 0L) {
                                                strings.dueKhata(Money(cust.openingBalancePaise).format())
                                            } else {
                                                strings.advanceKhata(Money(-cust.openingBalancePaise).format())
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (cust.openingBalancePaise > 0L) Color(0xFFD97706) else PastelGreenContent
                                        )
                                    } else {
                                        Text(
                                            text = strings.khataAllClear,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = PastelGreenContent
                                        )
                                    }
                                    if (!cust.address.isNullOrBlank()) {
                                        Text(
                                            text = cust.address,
                                            fontSize = 11.sp,
                                            color = DokandaarTheme.textMuted
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        customerToEdit = cust
                                        formName = cust.name
                                        formPhone = cust.phone
                                        formAddress = cust.address ?: ""
                                        formEmail = cust.email ?: ""
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryPurple, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = {
                                        customerToDelete = cust
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorColor, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    item(key = "pagination_footer") {
                        LazyLoadingFooter(
                            isLoadingMore = isLoadingMore,
                            hasMore = hasMore,
                            totalCount = filteredCustomers.size,
                            itemLabel = "customers"
                        )
                    }
                }
            }
        }
    }
}
