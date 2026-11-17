package com.billing.app.feature.importexport

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.billing.app.core.ui.components.SubPageHeader
import com.billing.app.core.ui.theme.DokandaarTheme
import com.billing.app.core.ui.theme.PastelBlueBg
import com.billing.app.core.ui.theme.PastelBlueContent
import com.billing.app.core.ui.theme.PastelGreenBg
import com.billing.app.core.ui.theme.PastelGreenContent
import com.billing.app.core.ui.theme.PrimaryPurple
import com.billing.app.core.ui.util.FeedbackManager
import com.billing.app.core.util.Money
import com.billing.app.domain.model.Product
import com.billing.app.domain.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@Composable
fun ImportExportScreen(
    productRepository: ProductRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val products by productRepository.getProducts().collectAsState(initial = emptyList())
    var isProcessing by remember { mutableStateOf(false) }

    // File Picker for CSV Import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                try {
                    val count = importProductsFromCsv(context, uri, productRepository)
                    FeedbackManager.showToast(context, "Successfully imported $count products!")
                } catch (e: Exception) {
                    FeedbackManager.showToast(context, "Import failed: ${e.message}")
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            SubPageHeader(
                title = "Import / Export",
                subtitle = "Excel & CSV Data Management",
                icon = Icons.Default.TableChart,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = DokandaarTheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Export Products Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PastelGreenBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = PastelGreenContent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Export Products to CSV",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DokandaarTheme.textPrimary
                            )
                            Text(
                                text = "Export ${products.size} items with prices, barcodes, and stock",
                                fontSize = 12.sp,
                                color = DokandaarTheme.textMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable(enabled = !isProcessing && products.isNotEmpty()) {
                                scope.launch {
                                    isProcessing = true
                                    try {
                                        exportProductsToCsv(context, products)
                                    } catch (e: Exception) {
                                        FeedbackManager.showToast(context, "Export error: ${e.message}")
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            },
                        color = PastelGreenBg
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Export ${products.size} Products (.CSV)",
                                color = PastelGreenContent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Import Products Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DokandaarTheme.cardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PastelBlueBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = PastelBlueContent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Import Products from CSV",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DokandaarTheme.textPrimary
                            )
                            Text(
                                text = "Bulk upload products from spreadsheet file",
                                fontSize = 12.sp,
                                color = DokandaarTheme.textMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable(enabled = !isProcessing) {
                                importLauncher.launch("text/*")
                            },
                        color = PrimaryPurple
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Choose CSV File to Import",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable(enabled = !isProcessing) {
                                scope.launch {
                                    try {
                                        shareSampleCsvTemplate(context)
                                    } catch (e: Exception) {
                                        FeedbackManager.showToast(context, "Error sharing template: ${e.message}")
                                    }
                                }
                            },
                        color = DokandaarTheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DokandaarTheme.border)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Share / Download Sample CSV Template",
                                color = PrimaryPurple,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun exportProductsToCsv(context: Context, products: List<Product>) = withContext(Dispatchers.IO) {
    val fileName = "dokandaar_products_${System.currentTimeMillis()}.csv"
    val file = File(context.cacheDir, fileName)

    file.printWriter().use { out ->
        out.println("ID,Name,Barcode,SKU,Category,Price(Rupees),Stock,Unit")
        products.forEach { p ->
            val cleanName = p.name.replace(",", " ")
            val cleanCat = p.category.replace(",", " ")
            val priceRupees = p.price.format(includeSymbol = false)
            out.println("${p.id},$cleanName,${p.barcode},${p.sku},$cleanCat,$priceRupees,${p.stock},${p.unit}")
        }
    }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Export Products CSV"))
}

private suspend fun shareSampleCsvTemplate(context: Context) = withContext(Dispatchers.IO) {
    val file = File(context.cacheDir, "dokandaar_products_sample_template.csv")
    file.printWriter().use { out ->
        out.println("ID,Name,Barcode,SKU,Category,Price(Rupees),Stock,Unit")
        out.println(",Aashirvaad Atta 5kg,8901512567890,ATTA-5KG,Grocery,285.00,28,kg")
        out.println(",Fortune Sunflower Oil 1L,8901156101234,OIL-SF-1L,Oil & Ghee,155.00,42,l")
        out.println(",Tata Salt 1kg,8901052000123,SALT-1KG,Grocery,28.00,60,kg")
        out.println(",Amul Gold Milk 1L,8901262078901,MILK-1L,Dairy Products,68.00,8,l")
        out.println(",Sugar 1kg,8906001056789,SUGAR-1KG,Grocery,44.00,75,kg")
    }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Sample Products Template"))
}

private fun parseCsvLine(line: String): List<String> {
    val tokens = mutableListOf<String>()
    val sb = StringBuilder()
    var inQuotes = false
    for (ch in line) {
        when {
            ch == '\"' -> inQuotes = !inQuotes
            ch == ',' && !inQuotes -> {
                tokens.add(sb.toString().trim().removeSurrounding("\""))
                sb.clear()
            }
            else -> sb.append(ch)
        }
    }
    tokens.add(sb.toString().trim().removeSurrounding("\""))
    return tokens
}

private suspend fun importProductsFromCsv(
    context: Context,
    uri: Uri,
    productRepository: ProductRepository
): Int = withContext(Dispatchers.IO) {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext 0
    val batch = mutableListOf<Product>()

    inputStream.bufferedReader().useLines { lines ->
        val lineList = lines.toList()
        if (lineList.size <= 1) return@withContext 0

        // Parse header row to locate column indices dynamically
        val header = parseCsvLine(lineList[0]).map { it.lowercase() }
        val nameIdx = header.indexOfFirst { it.contains("name") }.takeIf { it >= 0 } ?: 1
        val barcodeIdx = header.indexOfFirst { it.contains("barcode") }.takeIf { it >= 0 } ?: 2
        val skuIdx = header.indexOfFirst { it.contains("sku") }.takeIf { it >= 0 } ?: 3
        val categoryIdx = header.indexOfFirst { it.contains("category") }.takeIf { it >= 0 } ?: 4
        val priceIdx = header.indexOfFirst { it.contains("price") }.takeIf { it >= 0 } ?: 5
        val stockIdx = header.indexOfFirst { it.contains("stock") }.takeIf { it >= 0 } ?: 6
        val unitIdx = header.indexOfFirst { it.contains("unit") }.takeIf { it >= 0 } ?: 7

        // Process data rows
        for (i in 1 until lineList.size) {
            val line = lineList[i].trim()
            if (line.isBlank()) continue

            val parts = parseCsvLine(line)
            if (parts.isNotEmpty()) {
                val name = parts.getOrNull(nameIdx)?.trim() ?: ""
                val barcode = parts.getOrNull(barcodeIdx)?.trim() ?: ""
                val sku = parts.getOrNull(skuIdx)?.trim() ?: ""
                val category = parts.getOrNull(categoryIdx)?.trim().takeUnless { it.isNullOrBlank() } ?: "Grocery"
                val priceStr = parts.getOrNull(priceIdx)?.trim() ?: "0"
                val stockStr = parts.getOrNull(stockIdx)?.trim() ?: "0"
                val unit = parts.getOrNull(unitIdx)?.trim().takeUnless { it.isNullOrBlank() } ?: "Piece"

                if (name.isNotBlank() && barcode.isNotBlank()) {
                    val price = Money.fromRupees(priceStr)
                    val stock = maxOf(0, stockStr.toIntOrNull() ?: 0)
                    batch.add(
                        Product(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            barcode = barcode,
                            sku = if (sku.isNotBlank()) sku else "SKU-${System.currentTimeMillis().toString().takeLast(6)}",
                            category = category,
                            unit = unit,
                            price = price,
                            stock = stock
                        )
                    )
                }
            }
        }
    }

    if (batch.isNotEmpty()) {
        productRepository.addProducts(batch)
    }
    batch.size
}
