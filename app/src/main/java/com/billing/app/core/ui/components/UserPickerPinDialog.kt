package com.billing.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.BillingApplication
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.HeaderGradientStart
import com.billing.app.core.ui.theme.PrimaryPurple
import com.billing.app.data.local.entity.UserEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UserPickerPinDialog(
    onDismissRequest: () -> Unit = {},
    onLoginSuccess: (UserEntity) -> Unit
) {
    val strings = appStrings()
    val activeUsers by BillingApplication.instance.userRepository
        .getAllActiveUsers()
        .collectAsState(initial = emptyList())

    var selectedUser by remember { mutableStateOf<UserEntity?>(null) }
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockoutRemainingSeconds by remember { mutableStateOf<Long?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Countdown effect for lockout timer
    LaunchedEffect(lockoutRemainingSeconds) {
        val currentSeconds = lockoutRemainingSeconds
        if (currentSeconds != null && currentSeconds > 0) {
            delay(1000L)
            val next = currentSeconds - 1
            lockoutRemainingSeconds = if (next > 0) next else null
            if (next <= 0) {
                errorMessage = null
            }
        }
    }

    val performLogin: () -> Unit = {
        val user = selectedUser
        if (user != null && pinInput.length == 4 && !isLoggingIn && (lockoutRemainingSeconds == null || lockoutRemainingSeconds!! <= 0)) {
            coroutineScope.launch {
                isLoggingIn = true
                errorMessage = null
                val result = BillingApplication.instance.userRepository.login(user.id, pinInput)
                isLoggingIn = false
                result.fold(
                    onSuccess = { loggedInUser ->
                        onLoginSuccess(loggedInUser)
                    },
                    onFailure = { error ->
                        if (error is com.billing.app.domain.model.AppError) {
                            errorMessage = error.toMessage(strings)
                            if (error is com.billing.app.domain.model.AppError.AccountLocked) {
                                lockoutRemainingSeconds = error.seconds
                            }
                        } else {
                            val msg = error.message ?: strings.loginError
                            errorMessage = msg
                            val now = System.currentTimeMillis()
                            val lockedUntil = user.lockedUntil
                            if (lockedUntil != null && lockedUntil > now) {
                                lockoutRemainingSeconds = kotlin.math.ceil((lockedUntil - now) / 1000.0).toLong().coerceAtLeast(1L)
                            } else {
                                val regex = Regex("""(\d+)\s*seconds""")
                                val match = regex.find(msg)
                                if (match != null) {
                                    lockoutRemainingSeconds = match.groupValues[1].toLongOrNull() ?: 30L
                                } else if (msg.contains("locked", ignoreCase = true)) {
                                    lockoutRemainingSeconds = 30L
                                }
                            }
                        }
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
            if (selectedUser == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = HeaderGradientStart,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.selectUserProfile,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DokandaarTheme.textPrimary
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            selectedUser = null
                            pinInput = ""
                            errorMessage = null
                            lockoutRemainingSeconds = null
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DokandaarTheme.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = strings.enterSecurityPin,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DokandaarTheme.textPrimary
                    )
                }
            }
        },
        text = {
            if (selectedUser == null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = strings.chooseStaffAccount,
                        fontSize = 13.sp,
                        color = DokandaarTheme.textMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (activeUsers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.noActiveUsersFound,
                                fontSize = 13.sp,
                                color = DokandaarTheme.textMuted
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(activeUsers, key = { it.id }) { user ->
                                val now = System.currentTimeMillis()
                                val isLocked = user.lockedUntil != null && user.lockedUntil > now

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedUser = user
                                            pinInput = ""
                                            errorMessage = null
                                            if (isLocked) {
                                                lockoutRemainingSeconds = kotlin.math.ceil((user.lockedUntil!! - now) / 1000.0).toLong().coerceAtLeast(1L)
                                                errorMessage = "Account is locked. Please try again in $lockoutRemainingSeconds seconds."
                                            } else {
                                                lockoutRemainingSeconds = null
                                            }
                                        },
                                    color = DokandaarTheme.cardBackground,
                                    shape = RoundedCornerShape(12.dp),
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
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryPurple.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = user.displayName.take(1).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = PrimaryPurple
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = user.displayName,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 15.sp,
                                                color = DokandaarTheme.textPrimary
                                            )
                                            Text(
                                                text = "@${user.username} • ${user.role}",
                                                fontSize = 12.sp,
                                                color = DokandaarTheme.textMuted
                                            )
                                        }
                                        if (isLocked) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFFEE2E2))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = strings.locked,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFDC2626)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val user = selectedUser!!
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryPurple.copy(alpha = 0.08f))
                            .padding(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.displayName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = user.displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = DokandaarTheme.textPrimary
                            )
                            Text(
                                text = "${strings.role}: ${user.role}",
                                fontSize = 12.sp,
                                color = DokandaarTheme.textMuted
                            )
                        }
                    }

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                pinInput = it
                                errorMessage = null
                            }
                        },
                        label = { Text(strings.fourToSixDigitPin) },
                        placeholder = { Text("••••") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { performLogin() }
                        ),
                        singleLine = true,
                        isError = errorMessage != null || lockoutRemainingSeconds != null,
                        enabled = !isLoggingIn && (lockoutRemainingSeconds == null || lockoutRemainingSeconds!! <= 0),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (lockoutRemainingSeconds != null && lockoutRemainingSeconds!! > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockClock,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.accountLockedSeconds(lockoutRemainingSeconds!!),
                                color = Color(0xFFDC2626),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (selectedUser != null) {
                TextButton(
                    onClick = performLogin,
                    enabled = pinInput.length == 4 && !isLoggingIn && (lockoutRemainingSeconds == null || lockoutRemainingSeconds!! <= 0)
                ) {
                    if (isLoggingIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = HeaderGradientStart
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.loggingIn, color = HeaderGradientStart, fontWeight = FontWeight.Bold)
                    } else {
                        Text(strings.authorize, color = HeaderGradientStart, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (selectedUser != null) {
                        selectedUser = null
                        pinInput = ""
                        errorMessage = null
                        lockoutRemainingSeconds = null
                    } else {
                        onDismissRequest()
                    }
                }
            ) {
                Text(
                    text = if (selectedUser != null) strings.selectUserProfile else strings.cancel,
                    color = DokandaarTheme.textMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}
