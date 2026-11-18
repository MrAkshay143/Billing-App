package com.billing.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billing.app.core.localization.appStrings
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.PrimaryPurple

enum class DokandaarTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    PRODUCTS("Products", Icons.Outlined.Inventory2),
    INVOICES("Invoices", Icons.AutoMirrored.Outlined.ReceiptLong),
    REPORTS("Reports", Icons.Outlined.BarChart),
    MORE("More", Icons.Outlined.GridView)
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
            HorizontalDivider(thickness = 0.8.dp, color = DokandaarTheme.border.copy(alpha = 0.6f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DokandaarTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val localizedTitle = when (tab) {
                        DokandaarTab.HOME -> strings.navHome
                        DokandaarTab.PRODUCTS -> strings.navProducts
                        DokandaarTab.INVOICES -> strings.navInvoices
                        DokandaarTab.REPORTS -> strings.navReports
                        DokandaarTab.MORE -> strings.navMore
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(tab) }
                            .padding(vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(30.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) PrimaryPurple else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = localizedTitle,
                                tint = if (isSelected) Color.White else DokandaarTheme.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = localizedTitle,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            color = if (isSelected) {
                                if (DokandaarTheme.isDark) Color.White else PrimaryPurple
                            } else {
                                DokandaarTheme.textMuted
                            }
                        )
                    }
                }
            }
        }
    }
}
