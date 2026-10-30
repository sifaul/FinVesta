package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import com.example.data.model.AssetType
import com.example.data.model.InvestmentAssetEntity
import com.example.ui.theme.Amber500
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.OrangeTag
import com.example.ui.theme.PurpleTag
import com.example.ui.theme.Rose500
import com.example.util.CurrencyFormatter
import com.example.ui.components.PortfolioOverview
import com.example.ui.components.AddAssetDialog

data class AssetTypeSummary(
    val type: AssetType,
    val count: Int,
    val totalValue: Double,
    val totalInvested: Double,
    val percentage: Double,
    val pnl: Double,
    val pnlPercent: Double,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InvestmentScreen(
    assets: List<InvestmentAssetEntity>,
    onAddAsset: () -> Unit,
    onUpdatePrice: (InvestmentAssetEntity, Double) -> Unit,
    onEditAsset: ((InvestmentAssetEntity) -> Unit)? = null,
    onDeleteAsset: (InvestmentAssetEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTypeFilter by remember { mutableStateOf<AssetType?>(null) }
    var assetToEdit by remember { mutableStateOf<InvestmentAssetEntity?>(null) }
    var assetToDelete by remember { mutableStateOf<InvestmentAssetEntity?>(null) }

    val totalInvested = assets.sumOf { it.totalInvested }
    val totalCurrentValue = assets.sumOf { it.currentValue }
    val totalPnl = totalCurrentValue - totalInvested
    val totalPnlPercent = if (totalInvested > 0) (totalPnl / totalInvested) * 100.0 else 0.0

    val typeSummaries = remember(assets, totalCurrentValue) {
        if (assets.isEmpty() || totalCurrentValue <= 0) emptyList()
        else {
            assets.groupBy { it.assetType }
                .map { (type, typeAssets) ->
                    val typeValue = typeAssets.sumOf { it.currentValue }
                    val typeInvested = typeAssets.sumOf { it.totalInvested }
                    val typePnl = typeValue - typeInvested
                    val typePnlPct = if (typeInvested > 0) (typePnl / typeInvested) * 100.0 else 0.0
                    val (icon, color) = when (type) {
                        AssetType.SAHAM -> Icons.AutoMirrored.Filled.ShowChart to Emerald500
                        AssetType.KRIPTO -> Icons.Default.CurrencyBitcoin to Amber500
                        AssetType.REKSA_DANA -> Icons.Default.PieChart to Cyan500
                        AssetType.EMAS -> Icons.Default.MonetizationOn to Color(0xFFF59E0B)
                        AssetType.PROPERTI -> Icons.Default.AccountBalance to OrangeTag
                        AssetType.DEPOSITO_P2P -> Icons.Default.AccountBalance to PurpleTag
                        AssetType.KAS -> Icons.AutoMirrored.Filled.TrendingUp to Emerald500
                    }
                    AssetTypeSummary(
                        type = type,
                        count = typeAssets.size,
                        totalValue = typeValue,
                        totalInvested = typeInvested,
                        percentage = (typeValue / totalCurrentValue) * 100.0,
                        pnl = typePnl,
                        pnlPercent = typePnlPct,
                        color = color,
                        icon = icon
                    )
                }.sortedByDescending { it.totalValue }
        }
    }

    val filteredAssets = remember(assets, selectedTypeFilter) {
        if (selectedTypeFilter == null) assets
        else assets.filter { it.assetType == selectedTypeFilter }
    }

    Box(modifier = modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Komponen PortfolioOverview Sederhana, Rapi, Hemat Ruang, & Bisa Edit/Hapus
            item {
                PortfolioOverview(
                    assets = assets,
                    selectedCategory = selectedTypeFilter,
                    onCategorySelected = { type ->
                        selectedTypeFilter = type
                    },
                    onEditAsset = { assetToEdit = it },
                    onDeleteAsset = { assetToDelete = it },
                    isCollapsible = true,
                    initiallyExpanded = true,
                    showCategoryList = true
                )
            }

            // Filter Chips for Asset Types (CircleShape)
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == null,
                            onClick = { selectedTypeFilter = null },
                            shape = CircleShape,
                            label = {
                                Text(
                                    "Semua (${assets.size})",
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTypeFilter == null) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                    items(AssetType.values().filter { type -> assets.any { it.assetType == type } }) { type ->
                        val count = assets.count { it.assetType == type }
                        val isSelected = selectedTypeFilter == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTypeFilter = if (isSelected) null else type },
                            shape = CircleShape,
                            label = {
                                Text(
                                    "${type.shortName} ($count)",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }

            // Asset Cards List
            if (filteredAssets.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Belum ada aset investasi pada kategori ini.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(filteredAssets, key = { it.id }) { asset ->
                    AssetCardItem(
                        asset = asset,
                        onEdit = { assetToEdit = asset },
                        onDelete = { assetToDelete = asset }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddAsset,
            containerColor = Amber500,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .testTag("fab_add_asset")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tambah Aset")
        }
    }

    // Full Edit Asset Dialog
    assetToEdit?.let { asset ->
        AddAssetDialog(
            assetToEdit = asset,
            onDismiss = { assetToEdit = null },
            onConfirm = { name, type, units, unitLabel, buyPrice, currentPrice, platform, note ->
                val updated = asset.copy(
                    name = name,
                    assetType = type,
                    units = units,
                    unitLabel = unitLabel,
                    avgBuyPrice = buyPrice,
                    currentPrice = currentPrice,
                    platform = platform,
                    note = note
                )
                onEditAsset?.invoke(updated) ?: onUpdatePrice(asset, currentPrice)
                assetToEdit = null
            }
        )
    }

    // Confirmation Dialog before deleting an Investment Asset to prevent accidental deletion
    assetToDelete?.let { asset ->
        AlertDialog(
            onDismissRequest = { assetToDelete = null },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.testTag("asset_delete_confirmation_dialog"),
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Rose500.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = Rose500,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Hapus Aset Investasi?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "Apakah Anda yakin ingin menghapus aset investasi ini? Tindakan ini tidak dapat dibatalkan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = asset.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = asset.assetType.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${asset.units} ${asset.unitLabel}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.formatRupiah(asset.currentValue),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAsset(asset)
                        assetToDelete = null
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500),
                    modifier = Modifier.testTag("confirm_delete_asset_btn")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hapus Aset", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { assetToDelete = null },
                    shape = CircleShape,
                    modifier = Modifier.testTag("cancel_delete_asset_btn")
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun AssetCardItem(
    asset: InvestmentAssetEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isProfit = asset.unrealizedPnl >= 0
    val pnlColor = if (isProfit) Emerald500 else Rose500
    val (assetIcon, assetColor) = when (asset.assetType) {
        AssetType.SAHAM -> Icons.AutoMirrored.Filled.ShowChart to Emerald500
        AssetType.KRIPTO -> Icons.Default.CurrencyBitcoin to Amber500
        AssetType.REKSA_DANA -> Icons.Default.PieChart to Cyan500
        AssetType.EMAS -> Icons.Default.MonetizationOn to Amber500
        AssetType.PROPERTI -> Icons.Default.AccountBalance to OrangeTag
        AssetType.DEPOSITO_P2P -> Icons.Default.AccountBalance to PurpleTag
        else -> Icons.AutoMirrored.Filled.TrendingUp to Emerald500
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Header (Icon + Name + Platform on Left; Edit & Delete on Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(assetColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = assetIcon,
                            contentDescription = null,
                            tint = assetColor,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(9.dp))
                    Column {
                        Text(
                            text = asset.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = asset.assetType.shortName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = assetColor
                            )
                            Text(
                                text = " • ${asset.platform}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Edit & Delete Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Aset",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Ubah",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Hapus Aset",
                            tint = Rose500.copy(alpha = 0.8f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Row 2: Holdings & Values (Single sleek row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${formatAssetUnits(asset.units)} ${asset.unitLabel} • Modal ${formatPricePerUnit(asset.avgBuyPrice)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatRupiah(asset.currentValue),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = pnlColor.copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, pnlColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isProfit) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = pnlColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "${CurrencyFormatter.formatPercent(asset.unrealizedPnlPercent)} (${if (isProfit) "+" else ""}${CurrencyFormatter.formatCompactRupiah(asset.unrealizedPnl)})",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = pnlColor
                        )
                    }
                }
            }

            // Row 4: Note (if present)
            if (asset.note.isNotBlank()) {
                Text(
                    text = "📝 ${asset.note}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatAssetUnits(units: Double): String {
    return if (units % 1.0 == 0.0) {
        CurrencyFormatter.formatNumberOnly(units)
    } else {
        String.format(Locale.forLanguageTag("id-ID"), "%,.2f", units)
            .trimEnd('0')
            .trimEnd(',')
    }
}

private fun formatPricePerUnit(price: Double): String {
    return if (price >= 10_000_000) {
        CurrencyFormatter.formatCompactRupiah(price)
    } else {
        CurrencyFormatter.formatRupiah(price)
    }
}
