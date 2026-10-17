package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Rose500
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

import androidx.compose.ui.text.style.TextAlign

enum class TrendChartMode {
    SPLINE_AREA, // Recharts Smooth Dual Curved Area Chart
    GROUPED_BAR  // Recharts Side-by-Side Paired Bar Chart
}

data class MonthlyTrendPoint(
    val monthIndex: Int,
    val monthLabel: String,
    val fullMonthName: String,
    val year: Int,
    val income: Double,
    val expense: Double,
    val isCurrentMonth: Boolean = false
) {
    val netCashflow: Double get() = income - expense
    val savingsRate: Double get() = if (income > 0) ((netCashflow / income) * 100.0).coerceAtLeast(0.0) else 0.0
}

/**
 * Recharts-inspired Interactive Monthly Income vs. Expense Trend Component.
 */
@Composable
fun MonthlyIncomeExpenseTrendChart(
    transactions: List<TransactionEntity>,
    modifier: Modifier = Modifier,
    onOpenFullAnalytics: (() -> Unit)? = null,
    initialMode: TrendChartMode = TrendChartMode.SPLINE_AREA
) {
    val operationalTransactions = remember(transactions) {
        transactions.filter { tx ->
            tx.categoryName != "Alokasi Tabungan" &&
            tx.categoryName != "Pengembalian Tabungan" &&
            tx.categoryName != "Dari Tabungan" &&
            tx.categoryName != "Alokasi Pos Spending" &&
            tx.categoryName != "Pengembalian Pos Spending" &&
            !tx.title.startsWith("Alokasi:", ignoreCase = true) &&
            !tx.title.startsWith("Alokasi Pos:", ignoreCase = true) &&
            !tx.title.startsWith("Tambah Dana:", ignoreCase = true) &&
            !tx.title.startsWith("Pengembalian Sisa:", ignoreCase = true) &&
            !tx.title.startsWith("Alokasi Awal:", ignoreCase = true) &&
            !tx.title.startsWith("Pengembalian Tabungan:", ignoreCase = true)
        }
    }

    var chartMode by remember { mutableStateOf(initialMode) }
    var selectedMonthCount by remember { mutableIntStateOf(6) } // 6 or 12 months
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val monthFormat = remember { SimpleDateFormat("MMM", Locale.forLanguageTag("id-ID")) }
    val fullMonthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("id-ID")) }

    // Build dynamic monthly buckets from real transactions
    val trendData = remember(operationalTransactions, selectedMonthCount) {
        val nowCal = Calendar.getInstance()
        val buckets = ((selectedMonthCount - 1) downTo 0).map { offset ->
            val c = Calendar.getInstance().apply {
                timeInMillis = nowCal.timeInMillis
                add(Calendar.MONTH, -offset)
            }
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val shortLabel = monthFormat.format(c.time)
            val fullLabel = fullMonthFormat.format(c.time)
            val isCurrent = offset == 0
            object {
                val year = year
                val month = month
                val shortLabel = if (isCurrent) "$shortLabel*" else shortLabel
                val fullLabel = fullLabel
                val isCurrent = isCurrent
                val offset = offset
            }
        }

        buckets.mapIndexed { idx, bucket ->
            val txInMonth = operationalTransactions.filter { tx ->
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.dateEpochMillis }
                txCal.get(Calendar.YEAR) == bucket.year && txCal.get(Calendar.MONTH) == bucket.month
            }
            val realIncome = txInMonth.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val realExpense = txInMonth.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

            MonthlyTrendPoint(
                monthIndex = idx,
                monthLabel = bucket.shortLabel,
                fullMonthName = bucket.fullLabel,
                year = bucket.year,
                income = realIncome,
                expense = realExpense,
                isCurrentMonth = bucket.isCurrent
            )
        }
    }

    val avgIncome = remember(trendData) { trendData.map { it.income }.average() }
    val avgExpense = remember(trendData) { trendData.map { it.expense }.average() }
    val avgNet = avgIncome - avgExpense
    val avgSavingsRate = if (avgIncome > 0) ((avgNet / avgIncome) * 100.0).coerceAtLeast(0.0) else 0.0

    val maxScaleValue = remember(trendData) {
        val peak = trendData.maxOfOrNull { max(it.income, it.expense) } ?: 0.0
        if (peak <= 0.0) {
            1_000_000.0
        } else {
            val rounded = ((peak * 1.15) / 1_000_000.0).toInt() + 1
            (rounded * 1_000_000.0).coerceAtLeast(1_000_000.0)
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 700),
        label = "chartAnimProgress"
    )

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.testTag("monthly_income_expense_trend_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Title & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Emerald500.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = Emerald500,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tren Pemasukan vs Pengeluaran",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Visualisasi Arus Kas Bulanan",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (onOpenFullAnalytics != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenFullAnalytics() }
                    ) {
                        Text(
                            text = "Detail >",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Control Ribbon: Mode Switcher (Spline Area vs Bar) + Timeframe Selector (6 vs 12 Bulan)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View Mode Tabs
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        val isSpline = chartMode == TrendChartMode.SPLINE_AREA
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSpline) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    chartMode = TrendChartMode.SPLINE_AREA
                                    selectedIndex = null
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timeline,
                                    contentDescription = null,
                                    tint = if (isSpline) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Garis Tren",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSpline) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSpline) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val isBar = chartMode == TrendChartMode.GROUPED_BAR
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isBar) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    chartMode = TrendChartMode.GROUPED_BAR
                                    selectedIndex = null
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = if (isBar) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Batang",
                                    fontSize = 11.sp,
                                    fontWeight = if (isBar) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isBar) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Timeframe Dropdown (3 Bulan, 6 Bulan, 1 Tahun)
                var timeframeDropdownExpanded by remember { mutableStateOf(false) }
                val timeframeLabel = when (selectedMonthCount) {
                    3 -> "3 Bulan"
                    6 -> "6 Bulan"
                    12 -> "1 Tahun"
                    else -> "$selectedMonthCount Bulan"
                }

                Box {
                    Surface(
                        onClick = { timeframeDropdownExpanded = true },
                        shape = RoundedCornerShape(8.dp),
                        color = Emerald500.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Emerald500,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = timeframeLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald500
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Pilih Jangka Waktu",
                                tint = Emerald500,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = timeframeDropdownExpanded,
                        onDismissRequest = { timeframeDropdownExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        listOf(
                            3 to "3 Bulan Terakhir",
                            6 to "6 Bulan Terakhir",
                            12 to "1 Tahun (12 Bulan)"
                        ).forEach { (count, label) ->
                            val isSelected = selectedMonthCount == count
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Emerald500 else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Emerald500,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedMonthCount = count
                                    selectedIndex = null
                                    timeframeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recharts Legend & Quick Instructions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Emerald Legend (Income)
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Emerald500)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Pemasukan",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    // Rose Legend (Expense)
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Rose500)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Pengeluaran",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (selectedIndex != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedIndex = null }
                    ) {
                        Text(
                            text = "✕ Tutup",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "Geser / sentuh grafik untuk detail",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Floating / Docked Recharts Tooltip Card
            val activePoint = selectedIndex?.let { trendData.getOrNull(it) }
            AnimatedVisibility(
                visible = activePoint != null,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(150))
            ) {
                if (activePoint != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .testTag("recharts_tooltip_card")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Cyan500,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = activePoint.fullMonthName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                val isSurplus = activePoint.netCashflow >= 0
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSurplus) Emerald500.copy(alpha = 0.15f) else Rose500.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (isSurplus) "Surplus +${String.format(Locale.forLanguageTag("id-ID"), "%.1f", activePoint.savingsRate)}%" else "Defisit",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSurplus) Emerald500 else Rose500,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Pendapatan", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        CurrencyFormatter.formatRupiah(activePoint.income),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald500
                                    )
                                }

                                Column {
                                    Text("Pengeluaran", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        CurrencyFormatter.formatRupiah(activePoint.expense),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Rose500
                                    )
                                }

                                Column {
                                    Text("Arus Kas Bersih", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        (if (activePoint.netCashflow >= 0) "+" else "") + CurrencyFormatter.formatRupiah(activePoint.netCashflow),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activePoint.netCashflow >= 0) Cyan500 else Rose500
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (operationalTransactions.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Emerald500,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Belum ada transaksi pada periode ini. Catat transaksi untuk melihat dinamika grafik.",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Custom Canvas Chart Drawing
            val textMeasurer = rememberTextMeasurer()
            val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            val axisTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            val surfaceColor = MaterialTheme.colorScheme.surface
            val onSurfaceColor = MaterialTheme.colorScheme.onSurface
            val primaryColor = MaterialTheme.colorScheme.primary

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .testTag("monthly_trend_chart_canvas")
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(trendData, chartMode) {
                            detectTapGestures { offset ->
                                val leftPad = 48.dp.toPx()
                                val rightPad = 14.dp.toPx()
                                val plotW = size.width - leftPad - rightPad
                                if (plotW > 0 && offset.x in leftPad..(size.width - rightPad)) {
                                    val relX = offset.x - leftPad
                                    val step = plotW / (trendData.size - 1).coerceAtLeast(1)
                                    val closest = (relX / step).toInt().coerceIn(0, trendData.lastIndex)
                                    selectedIndex = if (selectedIndex == closest) null else closest
                                }
                            }
                        }
                        .pointerInput(trendData, chartMode) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val leftPad = 48.dp.toPx()
                                val rightPad = 14.dp.toPx()
                                val plotW = size.width - leftPad - rightPad
                                if (plotW > 0) {
                                    val relX = (change.position.x - leftPad).coerceIn(0f, plotW)
                                    val step = plotW / (trendData.size - 1).coerceAtLeast(1)
                                    val closest = Math.round(relX / step).toInt().coerceIn(0, trendData.lastIndex)
                                    selectedIndex = closest
                                }
                            }
                        }
                ) {
                    val leftPad = 48.dp.toPx()
                    val rightPad = 14.dp.toPx()
                    val topPad = 18.dp.toPx()
                    val bottomPad = 32.dp.toPx()

                    val plotW = size.width - leftPad - rightPad
                    val plotH = size.height - topPad - bottomPad

                    if (plotW <= 0 || plotH <= 0 || trendData.isEmpty()) return@Canvas

                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                    // 1. Draw Cartesian Gridlines (4 levels: 100%, 66%, 33%, 0%)
                    val steps = 3
                    for (i in 0..steps) {
                        val ratio = i.toFloat() / steps
                        val y = topPad + plotH * (1f - ratio)
                        val valAtStep = maxScaleValue * ratio

                        // Guideline
                        drawLine(
                            color = gridLineColor,
                            start = Offset(leftPad, y),
                            end = Offset(size.width - rightPad, y),
                            strokeWidth = 1f,
                            pathEffect = dashEffect
                        )

                        // Y-Axis label (e.g. 15Jt, 10Jt, 5Jt, 0)
                        val labelText = if (valAtStep <= 0) "0" else CurrencyFormatter.formatCompactRupiah(valAtStep)
                        drawText(
                            textMeasurer = textMeasurer,
                            text = labelText,
                            topLeft = Offset(2.dp.toPx(), y - 7.dp.toPx()),
                            style = TextStyle(
                                fontSize = 9.sp,
                                color = axisTextColor,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }

                    // 2. Render depending on Mode: Spline Area vs Grouped Bars
                    val count = trendData.size
                    val stepX = plotW / (count - 1).coerceAtLeast(1)

                    if (chartMode == TrendChartMode.SPLINE_AREA) {
                        // Smooth Cubic Bézier Spline Curves + Area Under Fill
                        val incomePoints = trendData.mapIndexed { idx, item ->
                            val x = leftPad + idx * stepX
                            val yRatio = (item.income / maxScaleValue).toFloat().coerceIn(0.02f, 1f) * animatedProgress
                            val y = topPad + plotH * (1f - yRatio)
                            Offset(x, y)
                        }

                        val expensePoints = trendData.mapIndexed { idx, item ->
                            val x = leftPad + idx * stepX
                            val yRatio = (item.expense / maxScaleValue).toFloat().coerceIn(0.02f, 1f) * animatedProgress
                            val y = topPad + plotH * (1f - yRatio)
                            Offset(x, y)
                        }

                        val baselineY = topPad + plotH

                        // Draw Income Area Gradient & Line
                        drawSplineArea(
                            points = incomePoints,
                            baselineY = baselineY,
                            fillBrush = Brush.verticalGradient(
                                colors = listOf(Emerald500.copy(alpha = 0.35f), Emerald500.copy(alpha = 0.02f)),
                                startY = topPad,
                                endY = baselineY
                            ),
                            strokeColor = Emerald500,
                            strokeWidth = 2.8.dp.toPx()
                        )

                        // Draw Expense Area Gradient & Line
                        drawSplineArea(
                            points = expensePoints,
                            baselineY = baselineY,
                            fillBrush = Brush.verticalGradient(
                                colors = listOf(Rose500.copy(alpha = 0.28f), Rose500.copy(alpha = 0.02f)),
                                startY = topPad,
                                endY = baselineY
                            ),
                            strokeColor = Rose500,
                            strokeWidth = 2.8.dp.toPx()
                        )

                        // Draw Recharts Data Dots
                        incomePoints.forEachIndexed { idx, pt ->
                            val isSel = selectedIndex == idx
                            val radius = if (isSel) 6.dp.toPx() else 3.5.dp.toPx()
                            // Outer ring
                            drawCircle(
                                color = surfaceColor,
                                radius = radius + 2.dp.toPx(),
                                center = pt
                            )
                            // Inner core
                            drawCircle(
                                color = Emerald500,
                                radius = radius,
                                center = pt
                            )
                        }

                        expensePoints.forEachIndexed { idx, pt ->
                            val isSel = selectedIndex == idx
                            val radius = if (isSel) 6.dp.toPx() else 3.5.dp.toPx()
                            drawCircle(
                                color = surfaceColor,
                                radius = radius + 2.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = Rose500,
                                radius = radius,
                                center = pt
                            )
                        }

                    } else {
                        // GROUPED_BAR Mode: Paired rounded bars for income & expense
                        val barGroupWidth = (plotW / count) * 0.75f
                        val singleBarWidth = (barGroupWidth / 2f).coerceAtLeast(8.dp.toPx())

                        trendData.forEachIndexed { idx, item ->
                            val isSel = selectedIndex == idx
                            val isDimmed = selectedIndex != null && !isSel
                            val alpha = if (isDimmed) 0.35f else 1.0f

                            val centerX = leftPad + (idx + 0.5f) * (plotW / count)

                            val incomeRatio = (item.income / maxScaleValue).toFloat().coerceIn(0.03f, 1f) * animatedProgress
                            val expenseRatio = (item.expense / maxScaleValue).toFloat().coerceIn(0.03f, 1f) * animatedProgress

                            val incomeBarH = plotH * incomeRatio
                            val expenseBarH = plotH * expenseRatio
                            val baselineY = topPad + plotH

                            // Income Bar (Left in pair)
                            val incomeLeft = centerX - singleBarWidth - 1.dp.toPx()
                            drawRoundRect(
                                color = Emerald500.copy(alpha = alpha),
                                topLeft = Offset(incomeLeft, baselineY - incomeBarH),
                                size = Size(singleBarWidth, incomeBarH),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )

                            // Expense Bar (Right in pair)
                            val expenseLeft = centerX + 1.dp.toPx()
                            drawRoundRect(
                                color = Rose500.copy(alpha = alpha),
                                topLeft = Offset(expenseLeft, baselineY - expenseBarH),
                                size = Size(singleBarWidth, expenseBarH),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }

                    // 3. Interactive Crosshair Guideline & Target Indicator (when hovering/touching)
                    if (selectedIndex != null) {
                        val selIdx = selectedIndex!!
                        val cursorX = if (chartMode == TrendChartMode.SPLINE_AREA) {
                            leftPad + selIdx * stepX
                        } else {
                            leftPad + (selIdx + 0.5f) * (plotW / count)
                        }

                        // Vertical dashed crosshair line
                        drawLine(
                            color = onSurfaceColor.copy(alpha = 0.55f),
                            start = Offset(cursorX, topPad),
                            end = Offset(cursorX, topPad + plotH),
                            strokeWidth = 1.5f,
                            pathEffect = dashEffect
                        )
                    }

                    // 4. X-Axis Month Labels
                    trendData.forEachIndexed { idx, item ->
                        val isSel = selectedIndex == idx
                        val posX = if (chartMode == TrendChartMode.SPLINE_AREA) {
                            leftPad + idx * stepX
                        } else {
                            leftPad + (idx + 0.5f) * (plotW / count)
                        }

                        val measured = textMeasurer.measure(
                            text = item.monthLabel,
                            style = TextStyle(
                                fontSize = if (isSel) 10.sp else 9.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) primaryColor else axisTextColor
                            )
                        )
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(posX - (measured.size.width / 2f), topPad + plotH + 8.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Summary KPI Ribbon
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Rata2 Masuk",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = CurrencyFormatter.formatCompactRupiah(avgIncome),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald500
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Rata2 Keluar",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = CurrencyFormatter.formatCompactRupiah(avgExpense),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Rose500
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Tabungan",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Emerald500,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${String.format(Locale.forLanguageTag("id-ID"), "%.1f", avgSavingsRate)}%",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald500
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper to draw a smooth cubic Bézier spline curve with under-curve gradient fill.
 */
private fun DrawScope.drawSplineArea(
    points: List<Offset>,
    baselineY: Float,
    fillBrush: Brush,
    strokeColor: Color,
    strokeWidth: Float
) {
    if (points.size < 2) return

    val strokePath = Path()
    val fillPath = Path()

    strokePath.moveTo(points.first().x, points.first().y)
    fillPath.moveTo(points.first().x, baselineY)
    fillPath.lineTo(points.first().x, points.first().y)

    for (i in 0 until points.size - 1) {
        val p0 = if (i > 0) points[i - 1] else points[i]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = if (i + 2 < points.size) points[i + 2] else p2

        val cp1x = p1.x + (p2.x - p0.x) / 6f
        val cp1y = p1.y + (p2.y - p0.y) / 6f
        val cp2x = p2.x - (p3.x - p1.x) / 6f
        val cp2y = p2.y - (p3.y - p1.y) / 6f

        strokePath.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
        fillPath.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
    }

    fillPath.lineTo(points.last().x, baselineY)
    fillPath.close()

    // Draw translucent gradient fill under the curve
    drawPath(path = fillPath, brush = fillBrush, style = Fill)

    // Draw solid smoothed trend line
    drawPath(
        path = strokePath,
        color = strokeColor,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
