package com.billing.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.billing.app.core.ui.theme.PastelBlueBg
import com.billing.app.core.ui.theme.PastelBlueContent
import com.billing.app.core.ui.theme.PastelGreenBg
import com.billing.app.core.ui.theme.PastelGreenContent
import com.billing.app.core.ui.theme.PastelOrangeBg
import com.billing.app.core.ui.theme.PastelOrangeContent
import com.billing.app.core.ui.theme.PastelPinkBg
import com.billing.app.core.ui.theme.PastelPinkContent
import com.billing.app.core.ui.theme.PastelPurpleBg
import com.billing.app.core.ui.theme.PastelPurpleContent
import com.billing.app.core.ui.theme.PastelRedBg
import com.billing.app.core.ui.theme.PastelRedContent
import com.billing.app.core.ui.theme.PastelTealBg
import com.billing.app.core.ui.theme.PastelTealContent
import com.billing.app.core.ui.theme.PrimaryPurple

data class CategoryPreset(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val iconColor: Color,
    val badgeBg: Color
)

object CategoryPresets {
    val presets = listOf(
        CategoryPreset("grocery", "Grocery", Icons.Default.LocalGroceryStore, PastelOrangeContent, PastelOrangeBg),
        CategoryPreset("oil", "Oil & Ghee", Icons.Default.WaterDrop, Color(0xFFD97706), Color(0xFFFEF3C7)),
        CategoryPreset("pulses", "Pulses", Icons.Default.ShoppingBag, PastelGreenContent, PastelGreenBg),
        CategoryPreset("spices", "Spices", Icons.Default.Whatshot, PastelRedContent, PastelRedBg),
        CategoryPreset("snacks", "Snacks", Icons.Default.Cookie, Color(0xFFEA580C), Color(0xFFFFEDD5)),
        CategoryPreset("beverages", "Beverages", Icons.Default.LocalCafe, Color(0xFF92400E), Color(0xFFF7F0E8)),
        CategoryPreset("household", "Household", Icons.Default.CleaningServices, PastelBlueContent, PastelBlueBg),
        CategoryPreset("dairy", "Dairy", Icons.Default.LocalDrink, PastelTealContent, PastelTealBg),
        CategoryPreset("fruits", "Fruits & Veg", Icons.Default.Eco, PastelGreenContent, PastelGreenBg),
        CategoryPreset("personal_care", "Personal Care", Icons.Default.Fastfood, PastelPinkContent, PastelPinkBg),
        CategoryPreset("others", "Others", Icons.Default.Category, PastelPurpleContent, PastelPurpleBg)
    )

    fun getPreset(key: String?): CategoryPreset {
        val normalized = key?.lowercase()?.trim() ?: "others"
        return presets.firstOrNull { it.key == normalized } ?: presets.firstOrNull {
            normalized.contains(it.key) || it.title.lowercase().contains(normalized)
        } ?: presets.last()
    }
}

@Composable
fun CategoryVisual(
    iconName: String?,
    imageUri: String?,
    size: Dp = 34.dp,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!imageUri.isNullOrBlank()) {
        // User custom photo
        ProductThumbnail(
            imageUri = imageUri,
            size = size,
            shape = RoundedCornerShape(8.dp),
            modifier = modifier
        )
    } else {
        val preset = CategoryPresets.getPreset(iconName)
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(if (isSelected) PastelPurpleBg else preset.badgeBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = preset.icon,
                contentDescription = preset.title,
                tint = if (isSelected) PrimaryPurple else preset.iconColor,
                modifier = Modifier.size(size * 0.58f)
            )
        }
    }
}
