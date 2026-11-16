package com.billing.app.feature.more.subscreens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.ui.components.SubPageHeader
import com.billing.app.core.ui.theme.AddProductBg
import com.billing.app.core.ui.theme.BorderColor
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.HeaderGradientStart
import com.billing.app.core.ui.theme.TextMutedColor
import com.billing.app.core.ui.util.FeedbackManager
import com.billing.app.feature.settings.SettingsViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var backupFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isWorking by remember { mutableStateOf(false) }
    var fileToRestore by remember { mutableStateOf<File?>(null) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }

    fun refreshList() {
        backupFiles = viewModel.listBackups(context)
    }

    LaunchedEffect(Unit) {
        refreshList()
    }

    // SAF Document Exporter
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            isWorking = true
            scope.launch {
                val result = viewModel.exportBackupToUri(context, it)
                isWorking = false
                FeedbackManager.showToast(context, result.getOrNull() ?: "Export failed", Toast.LENGTH_LONG)
            }
        }
    }

    // SAF Document Importer
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            isWorking = true
            scope.launch {
                val result = viewModel.importBackupFromUri(context, it)
                isWorking = false
                refreshList()
                FeedbackManager.showToast(context, result.getOrNull() ?: "Import failed", Toast.LENGTH_LONG)
            }
        }
    }

    // Restore Confirmation Dialog
    if (fileToRestore != null) {
        val target = fileToRestore!!
        AlertDialog(
            onDismissRequest = { fileToRestore = null },
            title = { Text("Restore Database?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Restoring from '${target.name}' will verify data integrity, restore your store profile, and safely merge all products and invoices into your offline database.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val file = target
                        fileToRestore = null
                        isWorking = true
                        scope.launch {
                            val result = viewModel.restoreDatabaseBackup(file)
                            isWorking = false
                            refreshList()
                            FeedbackManager.showToast(context, result.getOrNull() ?: "Restore failed", Toast.LENGTH_LONG)
                        }
                    }
                ) {
                    Text("Confirm Restore", color = HeaderGradientStart, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRestore = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Delete Backup Confirmation Dialog
    if (fileToDelete != null) {
        val target = fileToDelete!!
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete Backup?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${target.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBackup(target)
                        fileToDelete = null
                        refreshList()
                        FeedbackManager.showToast(context, "Backup deleted")
                    }
                ) {
                    Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            SubPageHeader(
                title = "Backup & Restore",
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
            // Backup Actions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AddProductBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = HeaderGradientStart, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Database Durability", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DokandaarTheme.textPrimary)
                            Text("Export verified JSON snapshot with SHA-256 checksum", fontSize = 12.sp, color = TextMutedColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Local Backup Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isWorking) {
                                isWorking = true
                                scope.launch {
                                    val result = viewModel.createDatabaseBackup(context)
                                    isWorking = false
                                    refreshList()
                                    FeedbackManager.showToast(context, result.getOrNull() ?: "Backup failed", Toast.LENGTH_LONG)
                                }
                            },
                        color = HeaderGradientStart
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isWorking) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Processing...", color = Color.White, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Local Backup", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // SAF Export & Import Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, HeaderGradientStart, RoundedCornerShape(12.dp))
                                .clickable(enabled = !isWorking) {
                                    val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                    exportLauncher.launch("dokandaar_backup_$time.json")
                                },
                            color = AddProductBg
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 11.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, tint = HeaderGradientStart, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export to File", color = HeaderGradientStart, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                .clickable(enabled = !isWorking) {
                                    importLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                            color = Color.White
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 11.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import File", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Saved Backups List Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.History, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Saved Backups (${backupFiles.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (backupFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No backup files saved yet.\nTap 'Create Local Backup' or 'Export to File' to safeguard your database.",
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
                    items(backupFiles, key = { it.name }) { file ->
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DokandaarTheme.textPrimary,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${file.length() / 1024} KB • ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(file.lastModified()))}",
                                        fontSize = 11.sp,
                                        color = TextMutedColor
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Share Button
                                    IconButton(
                                        onClick = { viewModel.shareBackup(context, file) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share Backup",
                                            tint = HeaderGradientStart,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Restore Button
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { fileToRestore = file },
                                        color = AddProductBg
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.Restore, contentDescription = null, tint = HeaderGradientStart, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Restore", color = HeaderGradientStart, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Delete Button
                                    IconButton(
                                        onClick = { fileToDelete = file },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Backup", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
