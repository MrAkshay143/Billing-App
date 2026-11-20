package com.billing.app.feature.categories

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.components.CategoryPresets
import com.billing.app.core.ui.components.CategoryVisual
import com.billing.app.core.ui.components.DokandaarActionPill
import com.billing.app.core.ui.components.InputField
import com.billing.app.core.ui.components.ProductThumbnail
import com.billing.app.core.ui.components.SubPageHeader
import com.billing.app.core.ui.theme.BorderColor
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.ErrorColor
import com.billing.app.core.ui.theme.PastelPurpleBg
import com.billing.app.core.ui.theme.PastelRedBg
import com.billing.app.core.ui.theme.PrimaryPurple
import com.billing.app.data.local.entity.CategoryEntity
import com.billing.app.domain.repository.CategoryRepository
import kotlinx.coroutines.launch

@Composable
fun CategoriesScreen(
    categoryRepository: CategoryRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val strings = appStrings()
    val categories by categoryRepository.getAllCategories().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    // Add Category Dialog
    if (showAddDialog) {
        CategoryEditorDialog(
            title = strings.addCategory,
            initialName = "",
            initialIconName = "grocery",
            initialImageUri = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, iconName, imageUri ->
                scope.launch {
                    categoryRepository.addCategory(
                        name = name.trim(),
                        iconName = iconName,
                        imageUri = imageUri
                    )
                    showAddDialog = false
                }
            }
        )
    }

    // Edit Category Dialog
    categoryToEdit?.let { cat ->
        CategoryEditorDialog(
            title = strings.editCategory,
            initialName = cat.name,
            initialIconName = cat.iconName ?: "others",
            initialImageUri = cat.imageUri,
            onDismiss = { categoryToEdit = null },
            onSave = { name, iconName, imageUri ->
                scope.launch {
                    categoryRepository.updateCategory(
                        cat.copy(
                            name = name.trim(),
                            iconName = iconName,
                            imageUri = imageUri
                        )
                    )
                    categoryToEdit = null
                }
            }
        )
    }

    // Delete Confirmation Dialog
    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text(strings.deleteCategoryTitle, fontWeight = FontWeight.Bold, color = ErrorColor) },
            text = { Text(strings.deleteCategoryPrompt(cat.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            categoryRepository.deleteCategory(cat.id)
                            categoryToDelete = null
                        }
                    }
                ) {
                    Text(strings.delete, color = ErrorColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
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
                title = strings.categoriesTitle,
                subtitle = strings.itemCategoriesCount(categories.size),
                icon = Icons.Default.Category,
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
        if (categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = DokandaarTheme.textMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.noCategoriesFound,
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
                items(categories, key = { it.id }) { cat ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Category Visual (preset badge or uploaded thumbnail)
                            CategoryVisual(
                                iconName = cat.iconName,
                                imageUri = cat.imageUri,
                                size = 44.dp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cat.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DokandaarTheme.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (!cat.imageUri.isNullOrBlank()) strings.customImage
                                    else CategoryPresets.getPreset(cat.iconName).title,
                                    fontSize = 11.sp,
                                    color = DokandaarTheme.textMuted
                                )
                            }

                            // Edit Button
                            IconButton(
                                onClick = { categoryToEdit = cat },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = strings.editCategory,
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Delete Button
                            IconButton(
                                onClick = { categoryToDelete = cat },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = strings.delete,
                                    tint = ErrorColor,
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

@Composable
private fun CategoryEditorDialog(
    title: String,
    initialName: String,
    initialIconName: String?,
    initialImageUri: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, iconName: String?, imageUri: String?) -> Unit
) {
    val strings = appStrings()
    var name by remember { mutableStateOf(initialName) }
    var selectedIconName by remember { mutableStateOf(initialIconName ?: "grocery") }
    var selectedImageUri by remember { mutableStateOf(initialImageUri) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = PrimaryPurple) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                InputField(
                    label = "${strings.categoryName} *",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = strings.placeholderCategoryName
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = strings.categoryVisual,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DokandaarTheme.textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Custom Photo Uploader / Preview
                if (!selectedImageUri.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PastelPurpleBg, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProductThumbnail(
                            imageUri = selectedImageUri,
                            size = 46.dp,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.customImageSelected, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                            Text(strings.shownOnCategoryCard, fontSize = 10.sp, color = DokandaarTheme.textMuted)
                        }
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = strings.remove, tint = ErrorColor, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { imagePickerLauncher.launch("image/*") },
                        color = DokandaarTheme.background,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.uploadCustomPhoto,
                                color = PrimaryPurple,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = strings.choosePresetIcon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DokandaarTheme.textMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Horizontal scroll list of 11 preset icons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryPresets.presets.forEach { preset ->
                        val isSelected = selectedImageUri.isNullOrBlank() && selectedIconName == preset.key
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedIconName = preset.key
                                    selectedImageUri = null
                                }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) PastelPurpleBg else preset.badgeBg)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) PrimaryPurple else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = preset.icon,
                                    contentDescription = preset.title,
                                    tint = if (isSelected) PrimaryPurple else preset.iconColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = preset.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) PrimaryPurple else DokandaarTheme.textMuted
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), selectedIconName, selectedImageUri)
                    }
                }
            ) {
                Text(appStrings().save, color = PrimaryPurple, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(appStrings().cancel, color = DokandaarTheme.textMuted)
            }
        },
        containerColor = DokandaarTheme.cardBackground,
        shape = RoundedCornerShape(16.dp)
    )
}
