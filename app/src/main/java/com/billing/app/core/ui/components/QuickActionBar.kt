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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.theme.AddProductBg
import com.billing.app.core.ui.theme.AddProductContent
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.InvoicesCardBg
import com.billing.app.core.ui.theme.InvoicesCardContent
import com.billing.app.core.ui.theme.PrintCardBg
import com.billing.app.core.ui.theme.PrintCardContent
import com.billing.app.core.ui.theme.ProductsCardBg
import com.billing.app.core.ui.theme.ProductsCardContent

@Composable
fun QuickActionBar(
    onAddProductClick: () -> Unit,
    onProductsClick: () -> Unit,
    onInvoicesClick: () -> Unit,
    onPrintClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = appStrings()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickActionItem(
            title = strings.quickAddProduct,
            icon = Icons.Default.AddBox,
            bgColor = AddProductBg,
            contentColor = AddProductContent,
            onClick = onAddProductClick,
            modifier = Modifier.weight(1f)
        )
        QuickActionItem(
            title = strings.quickProducts,
            icon = Icons.Default.Inventory2,
            bgColor = ProductsCardBg,
            contentColor = ProductsCardContent,
            onClick = onProductsClick,
            modifier = Modifier.weight(1f)
        )
        QuickActionItem(
            title = strings.quickInvoices,
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            bgColor = InvoicesCardBg,
            contentColor = InvoicesCardContent,
            onClick = onInvoicesClick,
            modifier = Modifier.weight(1f)
        )
        QuickActionItem(
            title = strings.quickPrint,
            icon = Icons.Default.Print,
            bgColor = PrintCardBg,
            contentColor = PrintCardContent,
            onClick = onPrintClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionItem(
    title: String,
    icon: ImageVector,
    bgColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = DokandaarTheme.isDark
    val containerBg = if (isDark) DokandaarTheme.cardBackground else bgColor
    val borderModifier = if (isDark) Modifier.border(1.dp, contentColor.copy(alpha = 0.25f), RoundedCornerShape(16.dp)) else Modifier

    Box(
        modifier = modifier
            .height(74.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(borderModifier)
            .background(containerBg)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
