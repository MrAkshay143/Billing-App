package com.billing.app.feature.suppliers

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
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
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.ui.components.InputField
import com.billing.app.core.ui.components.SubPageHeader
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.ErrorColor
import com.billing.app.core.ui.theme.PastelOrangeBg
import com.billing.app.core.ui.theme.PastelOrangeContent
import com.billing.app.core.ui.theme.PrimaryPurple
import com.billing.app.data.local.entity.SupplierEntity
import com.billing.app.domain.repository.SupplierRepository
import kotlinx.coroutines.launch

@Composable
fun SuppliersScreen(
    supplierRepository: SupplierRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val suppliers by supplierRepository.getAllSuppliers().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var supplierToEdit by remember { mutableStateOf<SupplierEntity?>(null) }

    var formName by remember { mutableStateOf("") }
    var formContact by remember { mutableStateOf("") }
    var formPhone by remember { mutableStateOf("") }
    var formAddress by remember { mutableStateOf("") }

    val filteredSuppliers = suppliers.filter {
        searchQuery.isBlank() ||
        it.name.contains(searchQuery.trim(), ignoreCase = true) ||
        it.phone.contains(searchQuery.trim())
    }

    if (showAddDialog || supplierToEdit != null) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                supplierToEdit = null
            },
            title = {
                Text(
                    text = if (supplierToEdit == null) "Add Supplier" else "Edit Supplier",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InputField(
                        label = "Supplier / Agency Name *",
                        value = formName,
                        onValueChange = { formName = it },
                        placeholder = "e.g. Parle Distributors"
                    )
                    InputField(
                        label = "Contact Person",
                        value = formContact,
                        onValueChange = { formContact = it },
                        placeholder = "e.g. Anil Sharma"
                    )
                    InputField(
                        label = "Mobile Number *",
                        value = formPhone,
                        onValueChange = { formPhone = it },
                        placeholder = "e.g. 9811223344",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    InputField(
                        label = "Address",
                        value = formAddress,
                        onValueChange = { formAddress = it },
                        placeholder = "e.g. Wholesale Mandi"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (formName.isNotBlank() && formPhone.isNotBlank()) {
                            scope.launch {
                                if (supplierToEdit == null) {
                                    supplierRepository.addSupplier(
                                        SupplierEntity(
                                            name = formName.trim(),
                                            contactPerson = formContact.trim(),
                                            phone = formPhone.trim(),
                                            email = "",
                                            address = formAddress.trim(),
                                            gstin = null,
                                            openingBalancePaise = 0L
                                        )
                                    )
                                } else {
                                    supplierRepository.updateSupplier(
                                        supplierToEdit!!.copy(
                                            name = formName.trim(),
                                            contactPerson = formContact.trim(),
                                            phone = formPhone.trim(),
                                            address = formAddress.trim()
                                        )
                                    )
                                }
                                showAddDialog = false
                                supplierToEdit = null
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
                        supplierToEdit = null
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
                title = "Suppliers",
                subtitle = "${suppliers.size} Registered Suppliers",
                icon = Icons.Default.LocalShipping,
                onNavigateBack = onNavigateBack,
                actions = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        modifier = Modifier.clickable {
                            formName = ""
                            formContact = ""
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
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search suppliers...", fontSize = 13.sp, color = DokandaarTheme.textMuted) },
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
                        unfocusedBorderColor = DokandaarTheme.border,
                        focusedContainerColor = DokandaarTheme.cardBackground,
                        unfocusedContainerColor = DokandaarTheme.cardBackground
                    ),
                    singleLine = true
                )
            }

            if (filteredSuppliers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = DokandaarTheme.textMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No suppliers found",
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
                    items(filteredSuppliers, key = { it.id }) { sup ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                                        .background(PastelOrangeBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        tint = PastelOrangeContent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sup.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DokandaarTheme.textPrimary
                                    )
                                    if (!sup.contactPerson.isNullOrBlank()) {
                                        Text(
                                            text = "Contact: ${sup.contactPerson}",
                                            fontSize = 12.sp,
                                            color = DokandaarTheme.textMuted
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable(enabled = sup.phone.isNotBlank()) {
                                            try {
                                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${sup.phone.trim()}"))
                                                context.startActivity(dialIntent)
                                            } catch (_: Exception) {}
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "Call",
                                            tint = if (sup.phone.isNotBlank()) PrimaryPurple else DokandaarTheme.textMuted,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = sup.phone.ifBlank { "No phone" },
                                            fontSize = 12.sp,
                                            color = if (sup.phone.isNotBlank()) PrimaryPurple else DokandaarTheme.textMuted
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        supplierToEdit = sup
                                        formName = sup.name
                                        formContact = sup.contactPerson ?: ""
                                        formPhone = sup.phone
                                        formAddress = sup.address ?: ""
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryPurple, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            supplierRepository.deleteSupplier(sup.id)
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
