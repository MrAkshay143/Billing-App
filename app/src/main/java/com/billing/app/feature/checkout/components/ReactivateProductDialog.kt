package com.billing.app.feature.checkout.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.PrimaryPurple
import com.billing.app.domain.model.Product

@Composable
fun ReactivateProductDialog(
    product: Product,
    onDismissRequest: () -> Unit,
    onConfirmReactivate: () -> Unit
) {
    val strings = appStrings()
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = strings.reactivateProductTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = DokandaarTheme.textPrimary
            )
        },
        text = {
            Text(
                text = "This product '${product.name}' is inactive/deleted. Reactivate?",
                fontSize = 14.sp,
                color = DokandaarTheme.textPrimary
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmReactivate()
                }
            ) {
                Text(strings.reactivate, color = PrimaryPurple, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(strings.cancel)
            }
        }
    )
}
