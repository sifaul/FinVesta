package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.Amber500
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.OrangeTag
import com.example.ui.theme.PurpleTag
import com.example.ui.theme.Rose500
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

data class ChartSlice(
    val label: String,
    val value: Double,
    val color: Color
)

val ChartColorPalette = listOf(
    Emerald500,
    Cyan500,
    Amber500,
    Rose500,
    PurpleTag,
    OrangeTag,
    Color(0xFF3B82F6),
    Color(0xFFEC4899),
    Color(0xFF84CC16)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DonutExpenseChart(
    slices: List<ChartSlice>,
    totalAmount: Double,
    modifier: Modifier = Modifier
) {
    var selectedSlice by remember { mutableStateOf<ChartSlice?>(null) }
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800),
        label = "donutProgress"
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val strokeWidth = 28.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerOffset = Offset(size.width / 2, size.height / 2)

                if (totalAmount <= 0 || slices.isEmpty()) {
                    // Empty grey ring
                    drawCircle(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        radius = radius,
                        center = centerOffset,
                        style = Stroke(width = strokeWidth)
                    )
                } else {
                    var startAngle = -90f
                    for (slice in slices) {
                        val sweepAngle = ((slice.value / totalAmount).toFloat() * 360f) * animatedProgress
                        val isSelected = selectedSlice?.label == slice.label
                        val actualStroke = if (isSelected) strokeWidth + 6f else strokeWidth

                        drawArc(
                            color = slice.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle.coerceAtLeast(1.5f),
                            useCenter = false,
                            topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = actualStroke, cap = StrokeCap.Butt)
                        )
                        startAngle += sweepAngle
                    }
                }
            }

            // Center Text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = selectedSlice?.label ?: "Total Pengeluaran",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = CurrencyFormatter.formatCompactRupiah(selectedSlice?.value ?: totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slices.forEach { slice ->
                val percentage = if (totalAmount > 0) (slice.value / totalAmount) * 100.0 else 0.0
                val isSelected = selectedSlice?.label == slice.label

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) slice.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable {
                            selectedSlice = if (isSelected) null else slice
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(slice.color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${slice.label} (${String.format(Locale.getDefault(), "%.0f%%", percentage)})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

data class MonthlyBarData(
    val monthLabel: String,
    val income: Double,
    val expense: Double
)

/**
 * Recharts-style Interactive Monthly Bar Chart Component.
 * Features:
 * - Cartesian grid lines (dashed horizontal guides)
 * - Side-by-side animated bars for Pendapatan (Emerald) and Pengeluaran (Rose)
 * - Interactive Tap to inspect: displays a floating Recharts tooltip card with exact values, net cashflow & savings rate
 * - Active bar focus effect (dims non-selected bars)
 * - Clean M3 theme adaptability (Dark/Light)
 */
@Composable
fun IncomeExpenseBarChart(
    dataList: List<MonthlyBarData>,
    modifier: Modifier = Modifier
) {
    var selectedItem by remember { mutableStateOf<MonthlyBarData?>(null) }
    val maxVal = remember(dataList) {
        val maxIncome = dataList.maxOfOrNull { it.income } ?: 1.0
        val maxExpense = dataList.maxOfOrNull { it.expense } ?: 1.0
        max(max(maxIncome, maxExpense), 1.0)
    }

    Column(modifier = modifier) {
        // Recharts Header: Legend Pills & Reset button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Legend Emerald
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Emerald500)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Masuk",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(14.dp))

                // Legend Rose
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Rose500)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Keluar",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (selectedItem != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.clickable { selectedItem = null }
                ) {
                    Text(
                        text = "Tutup Detail ✕",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(
                    text = "Sentuh batang untuk detail",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Recharts Interactive Tooltip (Floating Popover)
        if (selectedItem != null) {
            val item = selectedItem!!
            val net = item.income - item.expense
            val savingsRate = if (item.income > 0) ((net / item.income) * 100.0).coerceAtLeast(0.0) else 0.0

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bulan ${item.monthLabel}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (net >= 0) Emerald500.copy(alpha = 0.15f) else Rose500.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (net >= 0) "Surplus +${String.format(Locale.getDefault(), "%.1f", savingsRate)}%" else "Defisit",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (net >= 0) Emerald500 else Rose500,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Pendapatan", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                CurrencyFormatter.formatRupiah(item.income),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald500
                            )
                        }

                        Column {
                            Text("Pengeluaran", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                CurrencyFormatter.formatRupiah(item.expense),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Rose500
                            )
                        }

                        Column {
                            Text("Arus Kas Bersih", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                (if (net >= 0) "+" else "") + CurrencyFormatter.formatRupiah(net),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (net >= 0) Cyan500 else Rose500
                            )
                        }
                    }
                }
            }
        }

        // Recharts Cartesian Chart Area (Gridlines + Bars)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            // Horizontal Cartesian Grid Guide Lines
            val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 26.dp) // space for month labels
            ) {
                val h = size.height
                val w = size.width
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                // 4 grid lines: 100%, 66%, 33%, 0%
                val steps = 3
                for (i in 0..steps) {
                    val y = h * (i.toFloat() / steps)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f,
                        pathEffect = dashEffect
                    )
                }
            }

            // Bars Row
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                dataList.forEach { item ->
                    val isSelected = selectedItem == item
                    val isDimmed = selectedItem != null && !isSelected
                    val barAlpha = if (isDimmed) 0.35f else 1.0f

                    val incomeRatio = (item.income / maxVal).toFloat().coerceIn(0.04f, 1f)
                    val expenseRatio = (item.expense / maxVal).toFloat().coerceIn(0.04f, 1f)

                    val animatedIncomeRatio by animateFloatAsState(
                        targetValue = incomeRatio,
                        animationSpec = tween(durationMillis = 650),
                        label = "incomeBar"
                    )
                    val animatedExpenseRatio by animateFloatAsState(
                        targetValue = expenseRatio,
                        animationSpec = tween(durationMillis = 650),
                        label = "expenseBar"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedItem = if (selectedItem == item) null else item
                            }
                            .padding(horizontal = 4.dp)
                    ) {
                        // Bars Pair
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.height(145.dp)
                        ) {
                            // Income Bar (Emerald)
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .height((135 * animatedIncomeRatio).dp)
                                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                    .background(Emerald500.copy(alpha = barAlpha))
                                    .then(
                                        if (isSelected) Modifier.border(1.5.dp, Emerald500, RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                        else Modifier
                                    )
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            // Expense Bar (Rose)
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .height((135 * animatedExpenseRatio).dp)
                                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                    .background(Rose500.copy(alpha = barAlpha))
                                    .then(
                                        if (isSelected) Modifier.border(1.5.dp, Rose500, RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                        else Modifier
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Month Label
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        ) {
                            Text(
                                text = item.monthLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recharts-style Monthly Analysis Card visualizing Income versus Expense trends.
 * Powered by custom canvas-based drawing (supporting smooth spline area curves,
 * paired bar charts, interactive crosshairs, and popover tooltips).
 */
@Composable
fun MonthlyIncomeExpenseAnalysisCard(
    transactions: List<TransactionEntity>,
    onOpenFullAnalytics: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    MonthlyIncomeExpenseTrendChart(
        transactions = transactions,
        onOpenFullAnalytics = onOpenFullAnalytics,
        modifier = modifier
    )
}

