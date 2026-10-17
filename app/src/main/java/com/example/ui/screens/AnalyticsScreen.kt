package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InvestmentAssetEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.ChartColorPalette
import com.example.ui.components.ChartSlice
import com.example.ui.components.DonutExpenseChart
import com.example.ui.components.MonthlyIncomeExpenseTrendChart
import com.example.ui.components.PortfolioOverview
import com.example.ui.components.RechartsMonthlyBarChart
import com.example.ui.theme.Amber500
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Rose500
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class MonthlyRecapRowData(
    val offset: Int,
    val monthLabel: String,
    val fullLabel: String,
    val income: Double,
    val expense: Double,
    val net: Double,
    val isCurrent: Boolean
) {
    val savingsRate: Double get() = if (income > 0) ((net / income) * 100.0).coerceAtLeast(0.0) else 0.0
}

@Composable
fun AnalyticsScreen(
    transactions: List<TransactionEntity>,
    assets: List<InvestmentAssetEntity>,
    onOpenExportDialog: (monthName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Interactive Month Navigator state (0 = Bulan Ini, 1 = 1 Bulan Lalu, dst.)
    var selectedMonthOffset by remember { mutableIntStateOf(0) }

    val selectedCal = remember(selectedMonthOffset) {
        Calendar.getInstance().apply { add(Calendar.MONTH, -selectedMonthOffset) }
    }

    val monthHeaderFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("id-ID")) }
    val selectedMonthName = remember(selectedCal) { monthHeaderFormat.format(selectedCal.time) }

    // Filter transactions for the selected month
    val selectedMonthTransactions = remember(transactions, selectedMonthOffset) {
        val startCal = Calendar.getInstance().apply {
            time = selectedCal.time
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            time = selectedCal.time
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val startMillis = startCal.timeInMillis
        val endMillis = endCal.timeInMillis
        transactions.filter { it.dateEpochMillis in startMillis..endMillis }
    }

    val selectedMonthIncomes = selectedMonthTransactions.filter { it.type == TransactionType.INCOME }
    val selectedMonthExpenses = selectedMonthTransactions.filter { it.type == TransactionType.EXPENSE }

    val monthIncome = selectedMonthIncomes.sumOf { it.amount }
    val monthExpense = selectedMonthExpenses.sumOf { it.amount }
    val monthNet = monthIncome - monthExpense
    val monthSavingsRate = if (monthIncome > 0) ((monthNet / monthIncome) * 100.0).coerceAtLeast(0.0) else 0.0

    // Group selected month expenses by category
    val expenseByCategory = remember(selectedMonthExpenses) {
        selectedMonthExpenses.groupBy { it.categoryName }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val expenseChartSlices = remember(expenseByCategory) {
        expenseByCategory.mapIndexed { index, (category, amount) ->
            ChartSlice(
                label = category,
                value = amount,
                color = ChartColorPalette[index % ChartColorPalette.size]
            )
        }
    }

    // Group selected month income by source/category
    val incomeByCategory = remember(selectedMonthIncomes) {
        selectedMonthIncomes.groupBy { it.categoryName }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    // Multi-month summary list for the In-App Recap Table (Past 6 months)
    val multiMonthRecapList = remember(transactions) {
        val labelFmt = SimpleDateFormat("MMM yyyy", Locale.forLanguageTag("id-ID"))
        val fullFmt = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("id-ID"))
        (0..5).map { offset ->
            val c = Calendar.getInstance().apply { add(Calendar.MONTH, -offset) }
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
            val sMillis = startCal.timeInMillis
            val eMillis = endCal.timeInMillis
            val txInMonth = transactions.filter { it.dateEpochMillis in sMillis..eMillis }
            val inc = txInMonth.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val exp = txInMonth.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val net = inc - exp
            MonthlyRecapRowData(
                offset = offset,
                monthLabel = labelFmt.format(c.time),
                fullLabel = fullFmt.format(c.time),
                income = inc,
                expense = exp,
                net = net,
                isCurrent = offset == 0
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Laporan & Rekap Bulanan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Visualisasi Recharts, tren arus kas, & rekap bulan ke bulan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Export Button (Icon only)
                FilledIconButton(
                    onClick = { onOpenExportDialog(selectedMonthName) },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Emerald500,
                        contentColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("export_report_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Ekspor Laporan",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.surface
                    )
                }
            }
        }

        // 1. In-App Interactive Month Navigator Ribbon
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selectedMonthOffset++ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Bulan Sebelumnya",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Emerald500,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedMonthName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (selectedMonthOffset != 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = Emerald500.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { selectedMonthOffset = 0 }
                            ) {
                                Text(
                                    text = "Bulan Ini",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald500,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { if (selectedMonthOffset > 0) selectedMonthOffset-- },
                        enabled = selectedMonthOffset > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Bulan Berikutnya",
                            tint = if (selectedMonthOffset > 0) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }

        // 2. Selected Month KPI Summary Cards (Pemasukan, Pengeluaran, Surplus, Laju Tabungan)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Income Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Pemasukan", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.formatCompactRupiah(monthIncome),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${selectedMonthIncomes.size} transaksi",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Expense Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Pengeluaran", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.formatCompactRupiah(monthExpense),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Rose500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${selectedMonthExpenses.size} transaksi",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Net Surplus Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Sisa Surplus", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = (if (monthNet >= 0) "+" else "") + CurrencyFormatter.formatCompactRupiah(monthNet),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (monthNet >= 0) Emerald500 else Rose500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "Tabungan %.0f%%", monthSavingsRate),
                            fontSize = 9.5.sp,
                            color = if (monthSavingsRate >= 20) Emerald500 else Amber500
                        )
                    }
                }
            }
        }

        // 3. Visualisasi Arus Kas Bulanan (Recharts-style Paired Bar Chart)
        item {
            RechartsMonthlyBarChart(
                transactions = transactions,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 4. Tren Pemasukan vs Pengeluaran Bulanan (Spline Area & Bar Chart)
        item {
            MonthlyIncomeExpenseTrendChart(
                transactions = transactions,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 5. Tabel Rekapitulasi Bulan ke Bulan di Dalam Aplikasi
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Cyan500.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = Cyan500,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Tabel Rekap Bulan ke Bulan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Riwayat ringkasan 6 bulan terakhir", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Table Header
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bulan", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.2f))
                            Text("Pemasukan", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Emerald500, modifier = Modifier.weight(1.4f))
                            Text("Pengeluaran", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Rose500, modifier = Modifier.weight(1.4f))
                            Text("Surplus", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1.3f))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Rows
                    multiMonthRecapList.forEach { row ->
                        val isRowSelected = selectedMonthOffset == row.offset
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isRowSelected) Emerald500.copy(alpha = 0.12f) else Color.Transparent,
                            border = if (isRowSelected) BorderStroke(1.dp, Emerald500.copy(alpha = 0.4f)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMonthOffset = row.offset }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text(
                                        text = row.monthLabel,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isRowSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isRowSelected) Emerald500 else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (row.isCurrent) {
                                        Text("Bulan ini", fontSize = 9.sp, color = Emerald500)
                                    }
                                }

                                Text(
                                    text = if (row.income > 0) CurrencyFormatter.formatCompactRupiah(row.income) else "Rp 0",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Emerald500,
                                    modifier = Modifier.weight(1.4f)
                                )

                                Text(
                                    text = if (row.expense > 0) CurrencyFormatter.formatCompactRupiah(row.expense) else "Rp 0",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Rose500,
                                    modifier = Modifier.weight(1.4f)
                                )

                                Text(
                                    text = (if (row.net > 0) "+" else "") + CurrencyFormatter.formatCompactRupiah(row.net),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (row.net >= 0) Emerald500 else Rose500,
                                    modifier = Modifier.weight(1.3f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        }

        // 6. Donut Expense Chart Card for Selected Month
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PieChart, contentDescription = null, tint = Rose500, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pengeluaran Kategori: $selectedMonthName", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Text(
                        "Rincian distribusi pos pengeluaran bulan ini",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (expenseChartSlices.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Belum ada pengeluaran yang tercatat pada bulan $selectedMonthName.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        DonutExpenseChart(
                            slices = expenseChartSlices,
                            totalAmount = monthExpense,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 7. Sumber Pendapatan Breakdown for Selected Month
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QueryStats, contentDescription = null, tint = Emerald500, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sumber Pendapatan: $selectedMonthName", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Text("Gaji, Gig / ShopeeFood, Bisnis, Freelance, Dividen", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(14.dp))

                    if (incomeByCategory.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Belum ada pemasukan yang tercatat pada bulan $selectedMonthName.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        incomeByCategory.forEach { (category, amount) ->
                            val percent = if (monthIncome > 0) (amount / monthIncome) * 100.0 else 0.0
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(category, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${CurrencyFormatter.formatRupiah(amount)} (${String.format(Locale.getDefault(), "%.0f%%", percent)})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald500
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                ) {
                                    val ratio = if (monthIncome > 0) (amount / monthIncome).toFloat().coerceIn(0.01f, 1f) else 1f
                                    Box(
                                        modifier = Modifier
                                            .weight(ratio)
                                            .height(6.dp)
                                            .background(Emerald500)
                                    )
                                    val remaining = (1f - ratio).coerceAtLeast(0.001f)
                                    Box(modifier = Modifier.weight(remaining).height(6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 8. Portofolio Aset Investasi
        if (assets.isNotEmpty()) {
            item {
                PortfolioOverview(
                    assets = assets,
                    modifier = Modifier.fillMaxWidth(),
                    isCollapsible = true,
                    initiallyExpanded = true,
                    showCategoryList = true
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
