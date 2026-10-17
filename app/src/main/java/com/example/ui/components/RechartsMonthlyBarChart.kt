package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Transaction
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Rose500
import com.example.ui.viewmodel.FinVestaViewModel
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

/**
 * Data model representing a monthly bar data point in the Recharts visualization.
 */
data class RechartsMonthlyPoint(
    val monthKey: String, // e.g. "2026-09"
    val monthLabel: String, // e.g. "Sep"
    val fullLabel: String, // e.g. "September 2026"
    val income: Double,
    val expense: Double
) {
    val netCashflow: Double get() = income - expense
    val savingsRate: Double get() = if (income > 0) (netCashflow / income) * 100.0 else 0.0
}

/**
 * Recharts-style Bar Chart Composable that visualizes monthly income and expenses
 * using data from the Room database (via [FinVestaViewModel]).
 *
 * Implements Recharts paradigms:
 * - `<ResponsiveContainer>` adaptive height and width
 * - `<CartesianGrid strokeDasharray="3 3" />` dotted guide lines
 * - `<XAxis />` and `<YAxis />` scale calibration
 * - `<Bar dataKey="income" fill="#10B981" />` & `<Bar dataKey="expense" fill="#F43F5E" />`
 * - `<Tooltip />` interactive card inspector with detailed breakdown
 * - `<Legend />` toggleable indicators
 */
@Composable
fun RechartsMonthlyBarChart(
    viewModel: FinVestaViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    RechartsMonthlyBarChart(
        transactions = transactions,
        modifier = modifier
    )
}

/**
 * Overload accepting direct Room entities list for previewing or custom integration.
 */
@Composable
fun RechartsMonthlyBarChart(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier,
    monthsCount: Int = 6
) {
    if (transactions.isEmpty()) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Emerald500.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = Emerald500,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Visualisasi Arus Kas Bulanan (Recharts)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Grafik batang ganda Recharts dan perbandingan pendapatan vs pengeluaran akan ditampilkan otomatis berdasarkan transaksi bulanan Anda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    // 1. Process and aggregate data from Room transactions into monthly points
    val monthlyData = remember(transactions, monthsCount) {
        val cal = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMM", Locale.forLanguageTag("id-ID"))
        val fullFormat = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("id-ID"))
        val keyFormat = SimpleDateFormat("yyyy-MM", Locale.US)

        val points = mutableListOf<RechartsMonthlyPoint>()

        // Generate past N months sequence (oldest to current)
        for (i in (monthsCount - 1) downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -i)
            val monthKey = keyFormat.format(c.time)
            val monthLabel = monthFormat.format(c.time).replace(".", "")
            val fullLabel = fullFormat.format(c.time)

            // Aggregate from Room transactions for this month
            val startCal = Calendar.getInstance().apply {
                time = c.time
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                time = c.time
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }

            val startMillis = startCal.timeInMillis
            val endMillis = endCal.timeInMillis

            val monthTx = transactions.filter { it.dateEpochMillis in startMillis..endMillis }
            val inc = monthTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val exp = monthTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

            points.add(
                RechartsMonthlyPoint(
                    monthKey = monthKey,
                    monthLabel = monthLabel,
                    fullLabel = fullLabel,
                    income = inc,
                    expense = exp
                )
            )
        }
        points
    }

    var selectedPoint by remember { mutableStateOf<RechartsMonthlyPoint?>(null) }
    var showIncomeBar by remember { mutableStateOf(true) }
    var showExpenseBar by remember { mutableStateOf(true) }

    val maxVal = remember(monthlyData) {
        val mInc = monthlyData.maxOfOrNull { it.income } ?: 0.0
        val mExp = monthlyData.maxOfOrNull { it.expense } ?: 0.0
        max(max(mInc, mExp), 100_000.0)
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_monthly_bar_chart")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Title and Legend
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
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Grafik Arus Kas",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Tren Arus Kas Bulanan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pemasukan vs Pengeluaran",
                            fontSize = 11.sp,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Interactive Legend Pills (Fixed horizontal width & no vertical wrapping)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Masuk Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (showIncomeBar) Emerald500.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            0.8.dp,
                            if (showIncomeBar) Emerald500 else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.clickable { showIncomeBar = !showIncomeBar }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (showIncomeBar) Emerald500 else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Masuk",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                softWrap = false,
                                maxLines = 1,
                                color = if (showIncomeBar) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Keluar Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (showExpenseBar) Rose500.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            0.8.dp,
                            if (showExpenseBar) Rose500 else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.clickable { showExpenseBar = !showExpenseBar }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (showExpenseBar) Rose500 else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Keluar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                softWrap = false,
                                maxLines = 1,
                                color = if (showExpenseBar) Rose500 else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Recharts Interactive Floating Tooltip (when bar is tapped/selected)
            AnimatedVisibility(
                visible = selectedPoint != null,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(150))
            ) {
                selectedPoint?.let { point ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = point.fullLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Ketuk lagi untuk tutup",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Income
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = Emerald500,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Masuk: ${CurrencyFormatter.formatRupiah(point.income)}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Emerald500
                                    )
                                }
                                // Expense
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = Rose500,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Keluar: ${CurrencyFormatter.formatRupiah(point.expense)}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Rose500
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val net = point.netCashflow
                                Text(
                                    text = "Arus Kas: ${if (net >= 0) "+" else ""}${CurrencyFormatter.formatRupiah(net)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (net >= 0) Cyan500 else Rose500
                                )
                                Text(
                                    text = "Savings: ${String.format(Locale.US, "%.1f", point.savingsRate)}%",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Recharts Cartesian Chart Canvas & Bar Columns
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
            ) {
                // Cartesian Grid Dotted lines & Y-Axis labels
                val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                val axisTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 24.dp)
                ) {
                    val h = size.height
                    val w = size.width
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                    // 4 Cartesian guide lines: 100%, 66%, 33%, 0%
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

                // Bar Clusters & X-Axis Labels
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    monthlyData.forEach { point ->
                        val isSelected = selectedPoint == point
                        val isDimmed = selectedPoint != null && !isSelected
                        val barAlpha = if (isDimmed) 0.32f else 1.0f

                        val incomeRatio = (point.income / maxVal).toFloat().coerceIn(0.02f, 1f)
                        val expenseRatio = (point.expense / maxVal).toFloat().coerceIn(0.02f, 1f)

                        val animatedIncomeRatio by animateFloatAsState(
                            targetValue = if (showIncomeBar) incomeRatio else 0.001f,
                            animationSpec = tween(durationMillis = 550),
                            label = "animIncome"
                        )
                        val animatedExpenseRatio by animateFloatAsState(
                            targetValue = if (showExpenseBar) expenseRatio else 0.001f,
                            animationSpec = tween(durationMillis = 550),
                            label = "animExpense"
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedPoint = if (selectedPoint == point) null else point
                                }
                                .padding(horizontal = 3.dp)
                        ) {
                            // Dual side-by-side bars
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(145.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Income Bar (Emerald500)
                                if (showIncomeBar) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(animatedIncomeRatio)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(
                                                Emerald500.copy(
                                                    alpha = if (isSelected) 1f else barAlpha
                                                )
                                            )
                                    )
                                }

                                if (showIncomeBar && showExpenseBar) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                }

                                // Expense Bar (Rose500)
                                if (showExpenseBar) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(animatedExpenseRatio)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(
                                                Rose500.copy(
                                                    alpha = if (isSelected) 1f else barAlpha
                                                )
                                            )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // X-Axis Month Label
                            Text(
                                text = point.monthLabel,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Summary Footer Note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Ketuk batang grafik untuk melihat rincian nominal.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}
