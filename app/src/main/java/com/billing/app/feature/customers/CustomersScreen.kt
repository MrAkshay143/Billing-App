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
import com.billing.app.core.ui.theme.PastelBlueBg
import com.billing.app.core.ui.theme.PastelBlueContent
import com.billing.app.core.ui.theme.PrimaryPurple
import com.billing.app.data.local.entity.CustomerEntity
import com.billing.app.domain.repository.CustomerRepository
import kotlinx.coroutines.launch

@Composable
fun CustomersScreen(
    customerRepository: CustomerRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val customers by customerRepository.getAllCustomers().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<CustomerEntity?>(null) }

    var formName by remember { mutableStateOf("") }
    var formPhone by remember { mutableStateOf("") }
    var formEmail by remember { mutableStateOf("") }
    var formAddress by remember { mutableStateOf("") }

    val filteredCustomers = customers.filter {
        searchQuery.isBlank() ||
        it.name.contains(searchQuery.trim(), ignoreCase = true) ||
        it.phone.contains(searchQuery.trim())
    }

    if (showAddDialog || customerToEdit != null) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                customerToEdit = null
            },
            title = {
                Text(
                    text = if (customerToEdit == null) "Add Customer" else "Edit Customer",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InputField(
                        label = "Customer Name *",
                        value = formName,
                        onValueChange = { formName = it },
                        placeholder = "e.g. Ramesh Kumar"
                    )
                    InputField(
                        label = "Mobile Number *",
                        value = formPhone,
                        onValueChange = { formPhone = it },
                        placeholder = "e.g. 9876543210",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    InputField(
                        label = "Address (Optional)",
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
                        }
                    }
                ) {
                    Text("Save", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        customerToEdit = null
                    }
                ) {
                    Text("Cancel", color = DokandaarTheme.textMuted)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            SubPageHeader(
                title = "Customers",
                subtitle = "${customers.size} Registered Customers",
                icon = Icons.Default.People,
                onNavigateBack = onNavigateBack,
                actions = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        modifier = Modifier.clickable {
                            formName = ""
                            formPhone = ""
                            formAddress = ""
                            showAddDialog = true
                        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search field
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or mobile number...", fontSize = 13.sp, color = DokandaarTheme.textMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DokandaarTheme.textMuted) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = DokandaarTheme.textMuted)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = DokandaarTheme.cardBackground,
                        unfocusedContainerColor = DokandaarTheme.cardBackground
                    ),
                    singleLine = true
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
                            text = "No customers found",
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
                    items(filteredCustomers, key = { it.id }) { cust ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = null,
                                            tint = DokandaarTheme.textMuted,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = cust.phone,
                                            fontSize = 12.sp,
                                            color = DokandaarTheme.textMuted
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
                                        scope.launch {
                                            customerRepository.deleteCustomer(cust.id)
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
}
