package com.billing.app.feature.more.subscreens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.ui.components.DokandaarActionPill
import com.billing.app.core.ui.components.PinVerificationDialog
import com.billing.app.core.ui.components.SubPageHeader
import com.billing.app.core.ui.theme.AddProductBg
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.HeaderGradientStart
import com.billing.app.core.ui.theme.TextMutedColor
import com.billing.app.core.ui.util.FeedbackManager
import com.billing.app.feature.settings.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAccessScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val strings = com.billing.app.core.localization.LocalAppStrings.current
    val scope = rememberCoroutineScope()
    val staffList by viewModel.staffList.collectAsState()
    val isPinEnabled by viewModel.isPinEnabled.collectAsState()
    val hasPinSet by viewModel.hasPinSet.collectAsState()
    val autoLockMinutes by viewModel.autoLockMinutes.collectAsState()

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showSetPinDialog by rememberSaveable { mutableStateOf(false) }
    var showVerifyPinForRemove by rememberSaveable { mutableStateOf(false) }
    var showVerifyPinForChange by rememberSaveable { mutableStateOf(false) }
    var showVerifyPinForDisable by rememberSaveable { mutableStateOf(false) }
    var staffToDelete by remember { mutableStateOf<com.billing.app.feature.settings.StaffMember?>(null) }

    // Dialog for confirming staff member deletion
    staffToDelete?.let { staff ->
        AlertDialog(
            onDismissRequest = { staffToDelete = null },
            title = {
                Text(
                    text = strings.removeStaffTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = DokandaarTheme.textPrimary
                )
            },
            text = {
                Text(
                    text = "Remove '${staff.name}' from your store staff list?",
                    fontSize = 14.sp,
                    color = TextMutedColor
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteStaffMember(staff.id)
                        FeedbackManager.showToast(context, "Staff member removed")
                        staffToDelete = null
                    }
                ) {
                    Text(strings.delete, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { staffToDelete = null }) {
                    Text(strings.cancel, color = DokandaarTheme.textMuted)
                }
            },
            containerColor = DokandaarTheme.cardBackground,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Dialog for verifying current PIN before changing PIN
    if (showVerifyPinForChange) {
        PinVerificationDialog(
            title = "Verify Current PIN",
            subtitle = "Please enter your current PIN before setting a new PIN",
            onDismissRequest = { showVerifyPinForChange = false },
            onVerify = { pin, onError ->
                scope.launch {
                    val verified = viewModel.verifySecurityPin(pin)
                    if (verified) {
                        showVerifyPinForChange = false
                        showSetPinDialog = true
                    } else {
                        onError("Incorrect PIN. Please try again.")
                    }
                }
            }
        )
    }

    // Dialog for verifying PIN before removing PIN
    if (showVerifyPinForRemove) {
        PinVerificationDialog(
            title = "Verify Owner PIN",
            subtitle = "Enter current PIN to remove security lock",
            onDismissRequest = { showVerifyPinForRemove = false },
            onVerify = { pin, onError ->
                scope.launch {
                    val verified = viewModel.verifySecurityPin(pin)
                    if (verified) {
                        viewModel.removeSecurityPin()
                        showVerifyPinForRemove = false
                        FeedbackManager.showToast(context, "PIN removed successfully")
                    } else {
                        onError("Incorrect PIN. Please try again.")
                    }
                }
            }
        )
    }

    // Dialog for verifying PIN before disabling PIN security switch
    if (showVerifyPinForDisable) {
        PinVerificationDialog(
            title = "Verify Owner PIN",
            subtitle = "Enter PIN to disable security lock",
            onDismissRequest = { showVerifyPinForDisable = false },
            onVerify = { pin, onError ->
                scope.launch {
                    val verified = viewModel.verifySecurityPin(pin)
                    if (verified) {
                        viewModel.togglePinSecurity(false)
                        showVerifyPinForDisable = false
                        FeedbackManager.showToast(context, "PIN lock disabled")
                    } else {
                        onError("Incorrect PIN. Please try again.")
                    }
                }
            }
        )
    }

    // Set / Change PIN Dialog
    if (showSetPinDialog) {
        var pinInput by rememberSaveable { mutableStateOf("") }
        var confirmPinInput by rememberSaveable { mutableStateOf("") }
        var pinError by rememberSaveable { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            title = { Text(if (hasPinSet) strings.changePin else strings.setPin, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "This PIN protects sensitive store settings, sales reports, database restore, and voiding sales.",
                        fontSize = 12.sp,
                        color = TextMutedColor
                    )

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6) pinInput = it },
                        label = { Text(strings.fourToSixDigitPin) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { if (it.length <= 6) confirmPinInput = it },
                        label = { Text(strings.fourToSixDigitPin) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (pinError != null) {
                        Text(text = pinError!!, color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pinInput.length < 4) {
                            pinError = "PIN must be at least 4 digits"
                            return@TextButton
                        }
                        if (pinInput != confirmPinInput) {
                            pinError = "PINs do not match"
                            return@TextButton
                        }
                        scope.launch {
                            viewModel.setSecurityPin(pinInput)
                            showSetPinDialog = false
                            FeedbackManager.showToast(context, "Owner PIN saved successfully!")
                        }
                    }
                ) {
                    Text(strings.save, color = HeaderGradientStart, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPinDialog = false }) {
                    Text(strings.cancel, color = Color.Gray)
                }
            }
        )
    }

    if (showAddDialog) {
        var name by rememberSaveable { mutableStateOf("") }
        var phone by rememberSaveable { mutableStateOf("") }
        var role by rememberSaveable { mutableStateOf("Cashier (Billing Only)") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(strings.addStaffMember, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(strings.fullName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(strings.phoneNumber) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(strings.accessRole, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DokandaarTheme.textPrimary)

                    listOf(
                        "Cashier (Billing Only)" to "Can scan products and complete sales",
                        "Store Manager" to "Can manage inventory and view sales reports",
                        "Administrator (Full Access)" to "Full unrestricted system permissions"
                    ).forEach { (r, description) ->
                        val isSelected = role == r
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { role = r },
                            color = if (isSelected) AddProductBg else DokandaarTheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) HeaderGradientStart else DokandaarTheme.border
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = HeaderGradientStart,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = r,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) HeaderGradientStart else DokandaarTheme.textPrimary,
                                        fontSize = 13.sp
                                    )
                                }
                                Text(text = description, fontSize = 11.sp, color = DokandaarTheme.textMuted)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addStaffMember(name, phone, role)
                            showAddDialog = false
                        }
                    }
                ) {
                    Text(strings.addStaffMember, color = HeaderGradientStart, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(strings.cancel, color = Color.Gray)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            SubPageHeader(
                title = strings.userAccess,
                subtitle = "PIN security & staff accounts",
                icon = Icons.Default.Lock,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = DokandaarTheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Owner PIN Security Card (H3)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AddProductBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = HeaderGradientStart,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(strings.ownerPinLock, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DokandaarTheme.textPrimary)
                                Text(
                                    text = if (isPinEnabled) "PIN protection is active" else "Protect reports & settings with PIN",
                                    fontSize = 12.sp,
                                    color = TextMutedColor
                                )
                            }
                        }

                        Switch(
                            checked = isPinEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !hasPinSet) {
                                    showSetPinDialog = true
                                } else if (!enabled && hasPinSet) {
                                    showVerifyPinForDisable = true
                                } else {
                                    scope.launch { viewModel.togglePinSecurity(enabled) }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = HeaderGradientStart
                            )
                        )
                    }

                    if (isPinEnabled || hasPinSet) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (hasPinSet) {
                                            showVerifyPinForChange = true
                                        } else {
                                            showSetPinDialog = true
                                        }
                                    },
                                color = AddProductBg
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = HeaderGradientStart, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (hasPinSet) strings.changePin else strings.setPin,
                                        color = HeaderGradientStart,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            if (hasPinSet) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            showVerifyPinForRemove = true
                                        },
                                    color = Color(0xFFFEE2E2)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(strings.removePin, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Header Info Card with [+ Add Staff]
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AddProductBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = HeaderGradientStart,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.staffAndCashiers,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DokandaarTheme.textPrimary
                        )
                        Text(
                            text = "${staffList.size} active team member(s)",
                            fontSize = 12.sp,
                            color = TextMutedColor
                        )
                    }

                    DokandaarActionPill(
                        text = strings.addStaffMember,
                        backgroundColor = HeaderGradientStart,
                        contentColor = Color.White,
                        onClick = { showAddDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Staff List
            if (staffList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No staff members added yet.\nTap 'Add Staff' to register cashiers and managers.",
                        fontSize = 13.sp,
                        color = TextMutedColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(staffList, key = { it.id }) { staff ->
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(AddProductBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = staff.name.take(1).uppercase(),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HeaderGradientStart
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = staff.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DokandaarTheme.textPrimary
                                        )
                                        if (staff.phone.isNotBlank()) {
                                            Text(
                                                text = staff.phone,
                                                fontSize = 12.sp,
                                                color = TextMutedColor
                                            )
                                        }
                                        Text(
                                            text = staff.role,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = HeaderGradientStart
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { staffToDelete = staff },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove Staff",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
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
