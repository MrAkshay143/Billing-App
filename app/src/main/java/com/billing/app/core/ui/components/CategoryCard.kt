package com.billing.app.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.ui.theme.BorderColor
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.PastelPurpleBg
import com.billing.app.core.ui.theme.PrimaryPurple

@Composable
fun CategoryCard(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    isAllItem: Boolean = false,
    iconName: String? = null,
    imageUri: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .width(70.dp)
            .height(78.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) (if (DokandaarTheme.isDark) PrimaryPurple.copy(alpha = 0.22f) else PastelPurpleBg) else DokandaarTheme.cardBackground
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) PrimaryPurple else DokandaarTheme.border
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(34.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isAllItem) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = "All Products",
                        tint = PrimaryPurple,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    CategoryVisual(
                        iconName = iconName,
                        imageUri = imageUri,
                        size = 32.dp,
                        isSelected = isSelected
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) PrimaryPurple else DokandaarTheme.textPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
