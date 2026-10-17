package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomCategoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.Amber500
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.OrangeTag
import com.example.ui.theme.PurpleTag
import com.example.ui.theme.Rose500
import com.example.util.CategoryIconHelper
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class ChartStyleMode {
    DONUT,
    SOLID_PIE
}

data class CategoryExpenditureItem(
    val categoryName: String,
    val amount: Double,
    val percentage: Double,
    val color: Color,
    val iconName: String,
    val transactionCount: Int
)

val VibrantPiePalette = listOf(
    Emerald500,
    Cyan500,
    Amber500,
    Rose500,
    PurpleTag,
    OrangeTag,
    Color(0xFF3B82F6), // Blue
    Color(0xFFEC4899), // Pink
    Color(0xFF8B5CF6), // Violet
    Color(0xFF14B8A6), // Teal
    Color(0xFFF97316), // Orange
    Color(0xFF84CC16)  // Lime
)

/**
 * Dynamic Pie / Donut Chart visualizing monthly expenditure distributions by category.
 * Features:
 * - Native Compose Canvas with interactive direct-slice tap detection (angle & radius).
 * - Interactive slice expansion / offset pop-out on selection.
 * - Toggleable between Donut (with center metrics) and Solid Pie modes.
 * - Dynamic category chips with icons, colors, and percentages.
 * - Detailed callout card for selected slice with transaction count and amount.
 * - Empty state with quick action to record expenditure.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthlyExpenditurePieChartCard(
    transactions: List<TransactionEntity>,
    categories: List<CustomCategoryEntity> = emptyList(),
    onOpenAddExpense: () -> Unit = {},
    onOpenCategoryManager: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var chartMode by remember { mutableStateOf(ChartStyleMode.DONUT) }
    var selectedCategoryName by remember { mutableStateOf<String?>(null) }
    var onlyCurrentMonth by remember { mutableStateOf(true) }

    // Filter expense transactions (current month vs all)
    val expenseTransactions = remember(transactions, onlyCurrentMonth) {
        transactions.filter {
            it.type == TransactionType.EXPENSE &&
            it.categoryName != "Alokasi Pos Spending" &&
            it.categoryName != "Pengembalian Pos Spending" &&
            it.categoryName != "Alokasi Tabungan" &&
            (!onlyCurrentMonth || DateUtils.isCurrentMonth(it.dateEpochMillis))
        }
    }

    val totalExpense = remember(expenseTransactions) {
        expenseTransactions.sumOf { it.amount }
    }

    // Group transactions by category name
    val expenditureItems = remember(expenseTransactions, categories, totalExpense) {
        if (totalExpense <= 0.0) {
            emptyList()
        } else {
            expenseTransactions.groupBy { it.categoryName }
                .map { (catName, txList) ->
                    val sum = txList.sumOf { it.amount }
                    val percent = (sum / totalExpense) * 100.0

                    // Try to match custom category color and icon
                    val matchedCustom = categories.find {
                        it.name.equals(catName, ignoreCase = true) ||
                                catName.contains(it.name, ignoreCase = true) ||
                                it.name.contains(catName, ignoreCase = true)
                    }

                    val color = if (matchedCustom != null) {
                        CategoryIconHelper.parseColor(matchedCustom.colorHex)
                    } else {
                        val hash = Math.abs(catName.hashCode())
                        VibrantPiePalette[hash % VibrantPiePalette.size]
                    }

                    val icon = matchedCustom?.iconName ?: CategoryIconHelper.inferIconForCategory(catName)

                    CategoryExpenditureItem(
                        categoryName = catName,
                        amount = sum,
                        percentage = percent,
                        color = color,
                        iconName = icon,
                        transactionCount = txList.size
                    )
                }
                .sortedByDescending { it.amount }
        }
    }

    // Determine current selected item
    val selectedItem = remember(selectedCategoryName, expenditureItems) {
        expenditureItems.find { it.categoryName == selectedCategoryName }
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_expenditure_pie_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Title, Period Chip & Style Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Rose500.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (chartMode == ChartStyleMode.DONUT) Icons.Default.DonutLarge else Icons.Default.PieChart,
                            contentDescription = null,
                            tint = Rose500,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Distribusi Pengeluaran",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (onlyCurrentMonth) "Bulan Ini • ${DateUtils.formatMonthYear(System.currentTimeMillis())}" else "Semua Periode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Chart Style Toggle (Pie / Donut)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (chartMode == ChartStyleMode.DONUT) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { chartMode = ChartStyleMode.DONUT }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Donut",
                                fontSize = 11.sp,
                                fontWeight = if (chartMode == ChartStyleMode.DONUT) FontWeight.Bold else FontWeight.Normal,
                                color = if (chartMode == ChartStyleMode.DONUT) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (chartMode == ChartStyleMode.SOLID_PIE) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { chartMode = ChartStyleMode.SOLID_PIE }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Pie",
                                fontSize = 11.sp,
                                fontWeight = if (chartMode == ChartStyleMode.SOLID_PIE) FontWeight.Bold else FontWeight.Normal,
                                color = if (chartMode == ChartStyleMode.SOLID_PIE) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Time Filter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = onlyCurrentMonth,
                        onClick = { onlyCurrentMonth = true },
                        label = { Text("Bulan Ini", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald500.copy(alpha = 0.2f),
                            selectedLabelColor = Emerald500
                        ),
                        modifier = Modifier.height(30.dp)
                    )
                    FilterChip(
                        selected = !onlyCurrentMonth,
                        onClick = { onlyCurrentMonth = false },
                        label = { Text("Semua Periode", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald500.copy(alpha = 0.2f),
                            selectedLabelColor = Emerald500
                        ),
                        modifier = Modifier.height(30.dp)
                    )
                }

                // Total display
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Belanja",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatRupiah(totalExpense),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Rose500
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (expenditureItems.isEmpty()) {
                // Empty state
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum ada pengeluaran di periode ini",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Catat pengeluaran harian Anda untuk melihat visualisasi distribusi per kategori",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onOpenAddExpense,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald500),
                            border = BorderStroke(1.dp, Emerald500)
                        ) {
                            Text("+ Catat Pengeluaran", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // Interactive Dynamic Pie Chart Canvas
                DynamicCanvasPieChart(
                    items = expenditureItems,
                    totalAmount = totalExpense,
                    chartMode = chartMode,
                    selectedCategory = selectedCategoryName,
                    onSelectCategory = { tappedName ->
                        selectedCategoryName = if (selectedCategoryName == tappedName) null else tappedName
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Hint row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ketuk irisan bagan atau chip kategori untuk melihat detail",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Selected Category Callout Card
                AnimatedVisibility(
                    visible = selectedItem != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    selectedItem?.let { item ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = item.color.copy(alpha = 0.12f),
                            border = BorderStroke(1.5.dp, item.color.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategoryName = null }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(item.color.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = CategoryIconHelper.getIcon(item.iconName),
                                            contentDescription = null,
                                            tint = item.color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = item.categoryName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${item.transactionCount} transaksi dicatat",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(item.amount),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = item.color
                                    )
                                    Text(
                                        text = "${String.format(Locale.getDefault(), "%.1f", item.percentage)}% dari total",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive Category Chips Legend
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    expenditureItems.forEach { item ->
                        val isSelected = selectedCategoryName == item.categoryName
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) item.color.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            border = if (isSelected) BorderStroke(1.5.dp, item.color) else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedCategoryName = if (isSelected) null else item.categoryName
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(item.color)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.categoryName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${String.format(Locale.getDefault(), "%.0f", item.percentage)}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = item.color
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Footer link to Category Management
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenCategoryManager() }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kelola Kategori & Anggaran",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Canvas implementation with direct gesture angle calculations
 * and animated pop-out slice transitions.
 */
@Composable
private fun DynamicCanvasPieChart(
    items: List<CategoryExpenditureItem>,
    totalAmount: Double,
    chartMode: ChartStyleMode,
    selectedCategory: String?,
    onSelectCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Smooth entry animation for sweep angles
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "pieSweepAnimation"
    )

    val selectedItem = items.find { it.categoryName == selectedCategory }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(185.dp)
            .fillMaxWidth()
    ) {
        Canvas(
            modifier = Modifier
                .size(175.dp)
                .pointerInput(items, totalAmount) {
                    detectTapGestures { tapOffset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = tapOffset.x - centerX
                        val dy = tapOffset.y - centerY
                        val distance = sqrt(dx * dx + dy * dy)

                        val minDim = kotlin.math.min(size.width, size.height).toFloat()
                        val outerRadius = minDim / 2f
                        val innerRadius = if (chartMode == ChartStyleMode.DONUT) outerRadius * 0.55f else 0f

                        // Check if tap is within ring/disk
                        if (distance in innerRadius..outerRadius) {
                            val rad = atan2(dy.toDouble(), dx.toDouble())
                            var deg = Math.toDegrees(rad).toFloat()
                            // -90deg is 12 o'clock
                            var normalizedAngle = (deg + 90f) % 360f
                            if (normalizedAngle < 0f) normalizedAngle += 360f

                            var currentAngle = 0f
                            for (item in items) {
                                val sweep = (item.amount / totalAmount).toFloat() * 360f
                                if (normalizedAngle >= currentAngle && normalizedAngle < currentAngle + sweep) {
                                    onSelectCategory(item.categoryName)
                                    return@detectTapGestures
                                }
                                currentAngle += sweep
                            }
                        } else if (distance < innerRadius && chartMode == ChartStyleMode.DONUT) {
                            // Tapped center: deselect
                            onSelectCategory("")
                        }
                    }
                }
        ) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val baseRadius = (size.minDimension / 2f) - 16.dp.toPx()
            val donutStrokeWidth = 32.dp.toPx()

            var startAngle = -90f

            for (item in items) {
                val fullSweep = ((item.amount / totalAmount).toFloat() * 360f)
                val animatedSweep = fullSweep * animatedProgress
                val isSelected = item.categoryName == selectedCategory

                if (chartMode == ChartStyleMode.DONUT) {
                    // Donut Ring style
                    val stroke = if (isSelected) donutStrokeWidth + 8.dp.toPx() else donutStrokeWidth
                    val currentRadius = if (isSelected) baseRadius + 3.dp.toPx() else baseRadius
                    val gap = if (items.size > 1) 2f else 0f
                    val effectiveSweep = (animatedSweep - gap).coerceAtLeast(1.5f)

                    drawArc(
                        color = item.color,
                        startAngle = startAngle + (gap / 2f),
                        sweepAngle = effectiveSweep,
                        useCenter = false,
                        topLeft = Offset(centerX - currentRadius, centerY - currentRadius),
                        size = Size(currentRadius * 2, currentRadius * 2),
                        style = Stroke(width = stroke, cap = StrokeCap.Butt)
                    )
                } else {
                    // Solid Pie style with slice pop-out
                    val popDistance = if (isSelected) 8.dp.toPx() else 0f
                    val bisectorAngle = startAngle + (animatedSweep / 2f)
                    val popX = (popDistance * cos(Math.toRadians(bisectorAngle.toDouble()))).toFloat()
                    val popY = (popDistance * sin(Math.toRadians(bisectorAngle.toDouble()))).toFloat()

                    val sliceRadius = if (isSelected) baseRadius + 18.dp.toPx() else baseRadius + 12.dp.toPx()

                    drawArc(
                        color = item.color,
                        startAngle = startAngle,
                        sweepAngle = animatedSweep.coerceAtLeast(1f),
                        useCenter = true,
                        topLeft = Offset((centerX + popX) - sliceRadius, (centerY + popY) - sliceRadius),
                        size = Size(sliceRadius * 2, sliceRadius * 2),
                        style = Fill
                    )

                    // Subtle separator line
                    if (items.size > 1) {
                        val rad = Math.toRadians(startAngle.toDouble())
                        val edgeX = (centerX + popX) + (sliceRadius * cos(rad)).toFloat()
                        val edgeY = (centerY + popY) + (sliceRadius * sin(rad)).toFloat()
                        drawLine(
                            color = Color.White.copy(alpha = 0.8f),
                            start = Offset(centerX + popX, centerY + popY),
                            end = Offset(edgeX, edgeY),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }

                startAngle += fullSweep
            }
        }

        // Center Content for Donut Mode
        if (chartMode == ChartStyleMode.DONUT) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .size(110.dp)
                    .padding(8.dp)
            ) {
                if (selectedItem != null) {
                    Text(
                        text = selectedItem.categoryName,
                        style = MaterialTheme.typography.labelSmall,
                        color = selectedItem.color,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.1f", selectedItem.percentage)}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = CurrencyFormatter.formatCompactRupiah(selectedItem.amount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Total Pengeluaran",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = CurrencyFormatter.formatCompactRupiah(totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${items.size} Kategori",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
