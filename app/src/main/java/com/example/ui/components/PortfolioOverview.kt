package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssetType
import com.example.data.model.InvestmentAssetEntity
import com.example.ui.theme.Amber500
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.OrangeTag
import com.example.ui.theme.PurpleTag
import com.example.ui.theme.Rose500
import com.example.util.CurrencyFormatter
import java.util.Locale

/**
 * Model slice kategori portofolio
 */
data class PortfolioCategorySlice(
    val type: AssetType,
    val title: String,
    val shortName: String,
    val totalValue: Double,
    val totalInvested: Double,
    val count: Int,
    val percentage: Double,
    val pnl: Double,
    val pnlPercent: Double,
    val color: Color,
    val icon: ImageVector,
    val assets: List<InvestmentAssetEntity> = emptyList()
)

/**
 * Komponen 'PortfolioOverview' yang sederhana, rapi, kompak, dan hemat tempat.
 * Menyajikan ringkasan aset per kategori dan memungkinkan pengguna untuk langsung
 * melihat, mengubah (Edit), dan menghapus (Delete) aset di dalam portofolio.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PortfolioOverview(
    assets: List<InvestmentAssetEntity>,
    modifier: Modifier = Modifier,
    selectedCategory: AssetType? = null,
    onCategorySelected: ((AssetType?) -> Unit)? = null,
    onEditAsset: ((InvestmentAssetEntity) -> Unit)? = null,
    onDeleteAsset: ((InvestmentAssetEntity) -> Unit)? = null,
    isCollapsible: Boolean = true,
    initiallyExpanded: Boolean = true,
    showCategoryList: Boolean = true
) {
    var internalSelectedType by remember { mutableStateOf<AssetType?>(null) }
    val activeSelectedType = selectedCategory ?: internalSelectedType

    var isExpanded by rememberSaveable { mutableStateOf(if (isCollapsible) initiallyExpanded else true) }

    val totalInvested = remember(assets) { assets.sumOf { it.totalInvested } }
    val totalCurrentValue = remember(assets) { assets.sumOf { it.currentValue } }
    val totalPnl = totalCurrentValue - totalInvested
    val totalPnlPercent = if (totalInvested > 0) (totalPnl / totalInvested) * 100.0 else 0.0

    // Kelompokkan aset per kategori
    val slices = remember(assets, totalCurrentValue) {
        if (assets.isEmpty() || totalCurrentValue <= 0) {
            emptyList()
        } else {
            val grouped = assets.groupBy { it.assetType }
            val sortedTypes = grouped.keys.sortedByDescending { grouped[it]?.sumOf { a -> a.currentValue } ?: 0.0 }
            sortedTypes.map { type ->
                val typeAssets = grouped[type] ?: emptyList()
                val value = typeAssets.sumOf { it.currentValue }
                val cost = typeAssets.sumOf { it.totalInvested }
                val pnl = value - cost
                val pnlPct = if (cost > 0) (pnl / cost) * 100.0 else 0.0
                val pct = (value / totalCurrentValue) * 100.0
                val (icon, color) = getCategoryVisuals(type)

                PortfolioCategorySlice(
                    type = type,
                    title = type.displayName,
                    shortName = type.shortName,
                    totalValue = value,
                    totalInvested = cost,
                    count = typeAssets.size,
                    percentage = pct,
                    pnl = pnl,
                    pnlPercent = pnlPct,
                    color = color,
                    icon = icon,
                    assets = typeAssets
                )
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("portfolio_overview_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Judul + Ringkasan Singkat + Tombol Buka/Tutup
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isCollapsible) Modifier.clickable { isExpanded = !isExpanded }
                        else Modifier
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Cyan500.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = "Portfolio Overview",
                            tint = Cyan500,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Portofolio Aset",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (assets.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "${assets.size} Aset",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // PnL Badge Ringkas
                    if (assets.isNotEmpty()) {
                        val isGain = totalPnl >= 0
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isGain) Emerald500.copy(alpha = 0.14f) else Rose500.copy(alpha = 0.14f),
                            border = BorderStroke(0.6.dp, if (isGain) Emerald500.copy(alpha = 0.4f) else Rose500.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isGain) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = if (isGain) Emerald500 else Rose500,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = CurrencyFormatter.formatPercent(totalPnlPercent),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGain) Emerald500 else Rose500
                                )
                            }
                        }
                    }

                    if (isCollapsible) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Tutup" else "Buka",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !isCollapsible || isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Baris 1: Nilai Pasar & Modal (1 baris hemat tempat)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Nilai Pasar Saat Ini",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.formatRupiah(totalCurrentValue),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Modal Investasi",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.formatRupiah(totalInvested),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                val isGain = totalPnl >= 0
                                Text(
                                    text = "Untung / Rugi",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${if (isGain) "+" else ""}${CurrencyFormatter.formatRupiah(totalPnl)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGain) Emerald500 else Rose500
                                )
                            }
                        }
                    }

                    // Baris 2: Mini Visual Multi-Color Allocation Bar (tinggi hanya 6dp)
                    if (slices.isNotEmpty() && totalCurrentValue > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            slices.forEach { slice ->
                                val weight = (slice.percentage.toFloat() / 100f).coerceIn(0.02f, 1f)
                                Box(
                                    modifier = Modifier
                                        .weight(weight)
                                        .height(6.dp)
                                        .background(slice.color)
                                )
                            }
                        }
                    }

                    // Baris 3: Daftar Kategori & Aset di dalamnya (Bisa Edit & Hapus!)
                    if (slices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Rincian Kategori & Aset Portofolio",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            slices.forEach { slice ->
                                val isCategoryActive = activeSelectedType == slice.type

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isCategoryActive) slice.color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                    border = BorderStroke(
                                        width = if (isCategoryActive) 1.dp else 0.6.dp,
                                        color = if (isCategoryActive) slice.color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("portfolio_slice_${slice.type.name}")
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
                                        // Header Kategori
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val newSelection = if (isCategoryActive) null else slice.type
                                                    internalSelectedType = newSelection
                                                    onCategorySelected?.invoke(newSelection)
                                                },
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .clip(RoundedCornerShape(7.dp))
                                                        .background(slice.color.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = slice.icon,
                                                        contentDescription = slice.shortName,
                                                        tint = slice.color,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = slice.title,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "(${slice.count})",
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = CurrencyFormatter.formatRupiah(slice.totalValue),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    val isGain = slice.pnl >= 0
                                                    Text(
                                                        text = "${String.format(Locale.US, "%.1f", slice.percentage)}% • ${if (isGain) "+" else ""}${CurrencyFormatter.formatCompactRupiah(slice.pnl)}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isGain) Emerald500 else Rose500
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = if (isCategoryActive) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        // DAFTAR ASET DI DALAM PORTOFOLIO: Bisa EDIT & HAPUS langsung di sini!
                                        if (isCategoryActive && slice.assets.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            HorizontalDivider(
                                                thickness = 0.6.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))

                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                slice.assets.forEach { asset ->
                                                    PortfolioAssetRow(
                                                        asset = asset,
                                                        onEdit = { onEditAsset?.invoke(asset) },
                                                        onDelete = { onDeleteAsset?.invoke(asset) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum Ada Aset Investasi di Portofolio",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Baris item aset individu di dalam rincian portofolio dengan tombol Edit dan Hapus yang rapi dan hemat tempat.
 */
@Composable
private fun PortfolioAssetRow(
    asset: InvestmentAssetEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isProfit = asset.unrealizedPnl >= 0
    val pnlColor = if (isProfit) Emerald500 else Rose500

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kolom Kiri: Nama Aset, Platform, Unit Holdings
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = asset.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = asset.platform,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = "${if (asset.units % 1.0 == 0.0) asset.units.toLong().toString() else asset.units.toString()} ${asset.unitLabel} • Nilai: ${CurrencyFormatter.formatRupiah(asset.currentValue)}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Laba/Rugi: ${if (isProfit) "+" else ""}${CurrencyFormatter.formatCompactRupiah(asset.unrealizedPnl)} (${CurrencyFormatter.formatPercent(asset.unrealizedPnlPercent)})",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pnlColor
                )
            }

            // Kolom Kanan: Tombol Edit (Ubah) & Hapus
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tombol Edit
                Surface(
                    onClick = onEdit,
                    shape = RoundedCornerShape(7.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
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

                // Tombol Hapus
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Aset",
                        tint = Rose500.copy(alpha = 0.85f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

/**
 * Visual ikon dan warna untuk masing-masing tipe instrumen aset.
 */
fun getCategoryVisuals(type: AssetType): Pair<ImageVector, Color> {
    return when (type) {
        AssetType.SAHAM -> Icons.AutoMirrored.Filled.ShowChart to Emerald500
        AssetType.REKSA_DANA -> Icons.Default.PieChart to Cyan500
        AssetType.KRIPTO -> Icons.Default.CurrencyBitcoin to Amber500
        AssetType.EMAS -> Icons.Default.MonetizationOn to Color(0xFFEAB308)
        AssetType.PROPERTI -> Icons.Default.AccountBalance to OrangeTag
        AssetType.DEPOSITO_P2P -> Icons.Default.Savings to PurpleTag
        AssetType.KAS -> Icons.AutoMirrored.Filled.TrendingUp to Color(0xFF6366F1)
    }
}
