package com.billing.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.theme.AddProductBg
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.HeaderGradientStart

enum class DokandaarTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    PRODUCTS("Products", Icons.Default.Inventory2),
    INVOICES("Invoices", Icons.AutoMirrored.Filled.ReceiptLong),
    MORE("More", Icons.Default.GridView)
}

@Composable
fun BottomDokandaarNav(
    selectedTab: DokandaarTab,
    onTabSelected: (DokandaarTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = appStrings()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DokandaarTheme.navBackground,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(thickness = 1.dp, color = DokandaarTheme.border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DokandaarTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val itemContentColor = if (isSelected) {
                        if (DokandaarTheme.isDark) DokandaarTheme.textPrimary else HeaderGradientStart
                    } else {
                        DokandaarTheme.textMuted
                    }
                    val localizedTitle = when (tab) {
                        DokandaarTab.HOME -> strings.navHome
                        DokandaarTab.PRODUCTS -> strings.navProducts
                        DokandaarTab.INVOICES -> strings.navInvoices
                        DokandaarTab.MORE -> strings.navMore
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) {
                                    if (DokandaarTheme.isDark) DokandaarTheme.border else AddProductBg
                                } else {
                                    Color.Transparent
                                }
                            )
                            .clickable { onTabSelected(tab) }
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = localizedTitle,
                                tint = itemContentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = localizedTitle,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = itemContentColor
                            )
                        }
                    }
                }
            }
        }
    }
}
