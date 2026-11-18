package com.billing.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.ui.theme.InStockBg
import com.billing.app.core.ui.theme.InStockText
import com.billing.app.core.ui.theme.LowStockBg
import com.billing.app.core.ui.theme.LowStockText
import com.billing.app.core.ui.theme.OutOfStockBg
import com.billing.app.core.ui.theme.OutOfStockText

@Composable
fun StockStatusBadge(
    stock: Int,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label, icon) = when {
        stock > 5 -> Tuple4(InStockBg, InStockText, "In Stock: $stock", Icons.Default.CheckCircle)
        stock in 1..5 -> Tuple4(LowStockBg, LowStockText, "Low Stock: $stock", Icons.Default.Warning)
        else -> Tuple4(OutOfStockBg, OutOfStockText, "Out of Stock", Icons.Default.DoNotDisturbOn)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
