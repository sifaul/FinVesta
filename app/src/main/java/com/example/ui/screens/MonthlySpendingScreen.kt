package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import com.example.ui.components.DialogResizeEffect
import com.example.ui.components.rememberDialogKeyboardHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CustomCategoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Rose500
import com.example.util.CategoryIconHelper
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun MonthlySpendingScreen(
    transactions: List<TransactionEntity>,
    categories: List<CustomCategoryEntity>,
    monthlySpendingLimit: Double,
    onUpdateMonthlySpendingLimit: (Double) -> Unit,
    onUpdateCategoryBudget: (categoryId: Long, newBudget: Double) -> Unit,
    onAddCategory: (name: String, budget: Double, iconName: String, colorHex: String) -> Unit = { _, _, _, _ -> },
    onAllocateSpendingFunds: (category: CustomCategoryEntity, amountToAdd: Double) -> Unit = { _, _ -> },
    onReturnRemainingSpendingToCash: (category: CustomCategoryEntity, remainingAmount: Double) -> Unit = { _, _ -> },
    onReturnAllRemainingSpendingToCash: () -> Unit = {},
    onUpdateCategory: (CustomCategoryEntity) -> Unit = {},
    onDeleteCategory: (CustomCategoryEntity) -> Unit = {},
    onAddExpense: (prefilledCategory: String?) -> Unit,
    onOpenCategoryManager: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    // Navigasi bulan: 0 = bulan ini, -1 = bulan lalu, +1 = bulan depan
    var monthOffset by remember { mutableIntStateOf(0) }
    var showSetBudgetDialog by remember { mutableStateOf(false) }
    var showAddSpendingDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CustomCategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CustomCategoryEntity?>(null) }
    var categoryToReturnCash by remember { mutableStateOf<Pair<CustomCategoryEntity, Double>?>(null) }
    var categoryToAddFunds by remember { mutableStateOf<CustomCategoryEntity?>(null) }
    var showReturnAllDialog by remember { mutableStateOf(false) }

    val targetCalendar = remember(monthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, monthOffset)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val monthYearTitle = remember(targetCalendar) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("id-ID"))
        sdf.format(targetCalendar.time)
    }

    val isCurrentMonth = monthOffset == 0

    // Filter transaksi pengeluaran operasional bulan ini (kecualikan transaksi alokasi/pengembalian internal)
    val monthExpenses = remember(transactions, targetCalendar) {
        val year = targetCalendar.get(Calendar.YEAR)
        val month = targetCalendar.get(Calendar.MONTH)
        transactions.filter { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.dateEpochMillis }
            tx.type == TransactionType.EXPENSE &&
                    tx.categoryName != "Alokasi Pos Spending" &&
                    tx.categoryName != "Pengembalian Pos Spending" &&
                    tx.categoryName != "Alokasi Tabungan" &&
                    cal.get(Calendar.YEAR) == year &&
                    cal.get(Calendar.MONTH) == month
        }
    }

    val totalSpent = remember(monthExpenses) { monthExpenses.sumOf { it.amount } }
    val effectiveLimit = if (monthlySpendingLimit > 0) monthlySpendingLimit else 3_000_000.0
    val percentage = (totalSpent / effectiveLimit) * 100.0
    val remainingBudget = effectiveLimit - totalSpent
    val isOverBudget = remainingBudget < 0

    val expenseCategories = remember(categories) {
        categories.filter { it.isExpense && it.name != "Alokasi Pos Spending" && it.name != "Alokasi Tabungan" }
    }

    // Hitung total sisa uang dari seluruh pos spending yang memiliki alokasi
    val totalSisaAllPos = remember(expenseCategories, monthExpenses) {
        expenseCategories.sumOf { cat ->
            val spent = monthExpenses.filter {
                it.categoryName.equals(cat.name, ignoreCase = true) ||
                it.wallet.equals("Pos: ${cat.name}", ignoreCase = true)
            }.sumOf { it.amount }
            if (cat.monthlyBudgetLimit > 0) {
                (cat.monthlyBudgetLimit - spent).coerceAtLeast(0.0)
            } else 0.0
        }
    }

    Box(modifier = modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("monthly_spending_screen")
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 84.dp)
        ) {
            // 1. Top Bar Rapi: Tombol Kembali + Judul & Bulan + Tombol Tambah (Hanya Ikon +)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(36.dp).testTag("spending_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Spending & Anggaran",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Kelola pos dan batas pengeluaran",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Tombol Tambah MURNI IKON TAMBAH (Tanpa Kalimat!)
                    Surface(
                        onClick = { showAddSpendingDialog = true },
                        shape = CircleShape,
                        color = Emerald500,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("spending_add_icon_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tambah Pos Spending",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 2. Baris Navigasi Bulan Ramping & Rapi
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
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
                            onClick = { monthOffset-- },
                            modifier = Modifier.size(30.dp).testTag("spending_prev_month")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBackIosNew,
                                contentDescription = "Bulan Lalu",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { monthOffset = 0 }
                        ) {
                            Text(
                                text = monthYearTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!isCurrentMonth) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Emerald500.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Bulan Ini",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald500,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { monthOffset++ },
                            modifier = Modifier.size(30.dp).testTag("spending_next_month")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "Bulan Depan",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // 3. Hero Card: Ringkasan Pengeluaran & Batas Total Bulanan
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth().testTag("spending_hero_progress_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Pengeluaran",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Edit Batas Anggaran Total
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.clickable { showSetBudgetDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Ubah Batas",
                                        tint = Emerald500,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Batas: ${CurrencyFormatter.formatCompactRupiah(effectiveLimit)}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = CurrencyFormatter.formatRupiah(totalSpent),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isOverBudget) Rose500 else MaterialTheme.colorScheme.onSurface
                            )

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isOverBudget) Rose500.copy(alpha = 0.15f) else Emerald500.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", percentage)}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOverBudget) Rose500 else Emerald500,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val animProgress by animateFloatAsState(
                            targetValue = (percentage / 100.0).coerceIn(0.0, 1.0).toFloat(),
                            animationSpec = tween(500),
                            label = "overallLimitProgress"
                        )
                        LinearProgressIndicator(
                            progress = { animProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isOverBudget) Rose500 else Emerald500,
                            trackColor = MaterialTheme.colorScheme.surface,
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isOverBudget) {
                                    "Melebihi Batas: ${CurrencyFormatter.formatRupiah(-remainingBudget)}"
                                } else {
                                    "Sisa Anggaran: ${CurrencyFormatter.formatRupiah(remainingBudget)}"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isOverBudget) Rose500 else Emerald500
                            )

                            Text(
                                text = "Batas: ${CurrencyFormatter.formatRupiah(effectiveLimit)}",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3.5 Banner Masukkan Sisa Uang Pos Spending ke Saldo Kas
            if (totalSisaAllPos > 0) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Emerald500.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("spending_return_all_cash_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Emerald500.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = Emerald500,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Total Sisa Uang Pos Spending",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(totalSisaAllPos),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Emerald500
                                    )
                                }
                            }

                            Button(
                                onClick = { showReturnAllDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("spending_return_all_cash_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ke Saldo Kas", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 4. Section Header: Judul Pos Spending & Counter
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Daftar Pos Spending",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${expenseCategories.size} Pos",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Tombol Tambah Ikon Cepat di Header
                    IconButton(
                        onClick = { showAddSpendingDialog = true },
                        modifier = Modifier.size(28.dp).testTag("spending_header_add_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Pos Spending",
                            tint = Emerald500,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 5. Daftar Item Pos Spending Rapi, Teratur, & Tombol Seragam
            if (expenseCategories.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Emerald500.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = Emerald500,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Belum Ada Pos Spending",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ketuk ikon (+) untuk membuat pos pengeluaran seperti Makan, Transport, Belanja, dll.",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                onClick = { showAddSpendingDialog = true },
                                shape = CircleShape,
                                color = Emerald500,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                items(expenseCategories, key = { it.id }) { cat ->
                    val catIcon = CategoryIconHelper.getIcon(cat.iconName)
                    val catColor = CategoryIconHelper.parseColor(cat.colorHex)
                    val spentAmount = remember(monthExpenses, cat.name) {
                        monthExpenses.filter {
                            it.categoryName.equals(cat.name, ignoreCase = true) ||
                            it.wallet.equals("Pos: ${cat.name}", ignoreCase = true)
                        }.sumOf { it.amount }
                    }
                    val hasBudget = cat.monthlyBudgetLimit > 0
                    val pct = if (hasBudget) (spentAmount / cat.monthlyBudgetLimit) * 100.0 else 0.0
                    val isCatOver = hasBudget && spentAmount > cat.monthlyBudgetLimit
                    val sisa = if (hasBudget) cat.monthlyBudgetLimit - spentAmount else 0.0

                    SpendingCategoryCard(
                        category = cat,
                        icon = catIcon,
                        color = catColor,
                        spentAmount = spentAmount,
                        hasBudget = hasBudget,
                        percentage = pct,
                        isOverBudget = isCatOver,
                        remainingBudget = sisa,
                        onAddExpense = { onAddExpense(cat.name) },
                        onAddFunds = { categoryToAddFunds = cat },
                        onReturnToCash = { categoryToReturnCash = cat to sisa },
                        onEdit = { editingCategory = cat },
                        onDelete = { categoryToDelete = cat }
                    )
                }
            }
        }

        // FAB Tambah Pos Spending (Murni Ikon Tambah +)
        FloatingActionButton(
            onClick = { showAddSpendingDialog = true },
            containerColor = Emerald500,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .testTag("fab_add_spending")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tambah Pos Spending")
        }
    }

    // Dialog: Buat Pos Spending Baru
    if (showAddSpendingDialog) {
        SpendingCategoryFormDialog(
            title = "Tambah Pos Spending",
            initialName = "",
            initialBudget = 0.0,
            initialIcon = "Fastfood",
            initialColor = "#10B981",
            onDismiss = { showAddSpendingDialog = false },
            onConfirm = { name, budget, iconName, colorHex ->
                onAddCategory(name, budget, iconName, colorHex)
                showAddSpendingDialog = false
            }
        )
    }

    // Dialog: Edit Pos Spending
    editingCategory?.let { cat ->
        SpendingCategoryFormDialog(
            title = "Ubah Pos Spending",
            initialName = cat.name,
            initialBudget = cat.monthlyBudgetLimit,
            initialIcon = cat.iconName,
            initialColor = cat.colorHex,
            onDismiss = { editingCategory = null },
            onConfirm = { name, budget, iconName, colorHex ->
                onUpdateCategory(
                    cat.copy(
                        name = name,
                        monthlyBudgetLimit = budget,
                        iconName = iconName,
                        colorHex = colorHex
                    )
                )
                editingCategory = null
            }
        )
    }

    // Dialog: Tambah Nominal di Pos Spending (Diambil dari Saldo Kas)
    categoryToAddFunds?.let { cat ->
        val currentSpent = monthExpenses.filter {
            it.categoryName.equals(cat.name, ignoreCase = true) ||
            it.wallet.equals("Pos: ${cat.name}", ignoreCase = true)
        }.sumOf { it.amount }
        val deficit = (currentSpent - cat.monthlyBudgetLimit).coerceAtLeast(0.0)
        AddFundsToSpendingDialog(
            category = cat,
            initialSuggestedAmount = deficit,
            onDismiss = { categoryToAddFunds = null },
            onConfirm = { amountToAdd ->
                onAllocateSpendingFunds(cat, amountToAdd)
                categoryToAddFunds = null
            }
        )
    }

    // Dialog: Masukkan Sisa Uang Pos Spending ke Saldo Kas
    categoryToReturnCash?.let { (cat, sisa) ->
        ReturnRemainingToCashDialog(
            category = cat,
            remainingAmount = sisa,
            onDismiss = { categoryToReturnCash = null },
            onConfirm = {
                onReturnRemainingSpendingToCash(cat, sisa)
                categoryToReturnCash = null
            }
        )
    }

    // Dialog: Masukkan Semua Sisa Uang Pos Spending ke Saldo Kas
    if (showReturnAllDialog) {
        ReturnAllRemainingToCashDialog(
            totalRemainingAmount = totalSisaAllPos,
            onDismiss = { showReturnAllDialog = false },
            onConfirm = {
                onReturnAllRemainingSpendingToCash()
                showReturnAllDialog = false
            }
        )
    }

    // Dialog: Konfirmasi Hapus Pos Spending
    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = {
                Text("Hapus Pos Spending", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Apakah Anda yakin ingin menghapus pos spending '${cat.name}'? Data transaksi pengeluaran yang pernah tercatat akan tetap tersimpan.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(cat)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500),
                    shape = CircleShape
                ) {
                    Text("Hapus", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { categoryToDelete = null },
                    shape = CircleShape
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog: Ubah Batas Total Bulanan
    if (showSetBudgetDialog) {
        SetMonthlyBudgetDialog(
            currentLimit = effectiveLimit,
            onDismiss = { showSetBudgetDialog = false },
            onConfirm = { newLimit ->
                onUpdateMonthlySpendingLimit(newLimit)
                showSetBudgetDialog = false
            }
        )
    }
}

/**
 * Kartu Pos Spending yang teratur, rapi, dan tombol aksi terstruktur rapi di kanan
 */
@Composable
private fun SpendingCategoryCard(
    category: CustomCategoryEntity,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    spentAmount: Double,
    hasBudget: Boolean,
    percentage: Double,
    isOverBudget: Boolean,
    remainingBudget: Double,
    onAddExpense: () -> Unit,
    onAddFunds: () -> Unit,
    onReturnToCash: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("spending_pos_item_${category.name}")
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            // Baris 1: Ikon + Nama + Pengeluaran & Persentase
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
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(color.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = category.name,
                            tint = color,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(9.dp))
                    Column {
                        Text(
                            text = category.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (hasBudget) "Alokasi: ${CurrencyFormatter.formatRupiah(category.monthlyBudgetLimit)}" else "Tanpa Batas",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.formatRupiah(spentAmount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isOverBudget) Rose500 else MaterialTheme.colorScheme.onSurface
                    )
                    if (hasBudget) {
                        Text(
                            text = if (isOverBudget) "Defisit ${(percentage - 100).toInt()}%" else "${percentage.toInt()}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverBudget) Rose500 else color
                        )
                    }
                }
            }

            // Baris 2: Linear Progress Bar
            if (hasBudget) {
                Spacer(modifier = Modifier.height(7.dp))
                val progressFraction = (percentage / 100.0).coerceIn(0.0, 1.0).toFloat()
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.5.dp)),
                    color = if (isOverBudget) Rose500 else if (percentage >= 80) Amber500 else color,
                    trackColor = MaterialTheme.colorScheme.surface,
                    strokeCap = StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            // Baris 3: Status Sisa di Kiri & Tombol Aksi Seragam di Kanan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info Sisa / Defisit Anggaran
                if (hasBudget) {
                    Text(
                        text = if (remainingBudget >= 0) {
                            "Sisa: ${CurrencyFormatter.formatRupiah(remainingBudget)}"
                        } else {
                            "Defisit: ${CurrencyFormatter.formatRupiah(-remainingBudget)}"
                        },
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (remainingBudget >= 0) Emerald500 else Rose500
                    )
                } else {
                    Text(
                        text = "Pengeluaran Berjalan",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Cluster Tombol Aksi Rapi
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Tombol Masukkan Sisa ke Saldo Kas (jika ada sisa uang)
                    if (hasBudget && remainingBudget > 0) {
                        Surface(
                            onClick = onReturnToCash,
                            shape = RoundedCornerShape(8.dp),
                            color = Emerald500.copy(alpha = 0.16f),
                            border = BorderStroke(0.8.dp, Emerald500.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("spending_return_cash_${category.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Masukkan Sisa ke Kas",
                                    tint = Emerald500,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Ke Kas",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald500
                                )
                            }
                        }
                    }

                    // Tombol Tambah Dana dari Saldo Kas (jika pengeluaran spending lebih besar / defisit)
                    if (hasBudget && isOverBudget) {
                        Surface(
                            onClick = onAddFunds,
                            shape = RoundedCornerShape(8.dp),
                            color = Amber500.copy(alpha = 0.18f),
                            border = BorderStroke(0.8.dp, Amber500.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("spending_add_funds_${category.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Tambah Dana dari Saldo Kas",
                                    tint = Amber500,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Tambah Dana",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Amber500
                                )
                            }
                        }
                    } else if (hasBudget) {
                        // Ikon tambah nominal dari kas untuk top up kapan saja
                        IconButton(
                            onClick = onAddFunds,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tambah Nominal dari Kas",
                                tint = Amber500,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Tombol Tambah Transaksi Pengeluaran Cepat (Ikon + Saja)
                    Surface(
                        onClick = onAddExpense,
                        shape = CircleShape,
                        color = color.copy(alpha = 0.14f),
                        border = BorderStroke(0.8.dp, color.copy(alpha = 0.35f)),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = "Tambah Pengeluaran Pos Ini",
                                tint = color,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Tombol Edit
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Pos",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Tombol Hapus
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Hapus Pos",
                            tint = Rose500.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddFundsToSpendingDialog(
    category: CustomCategoryEntity,
    initialSuggestedAmount: Double = 0.0,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var rawInput by remember {
        mutableStateOf(
            if (initialSuggestedAmount > 0) CurrencyFormatter.formatThousands(initialSuggestedAmount.toLong().toString())
            else ""
        )
    }

    val presets = listOf(
        50_000L to "+50rb",
        100_000L to "+100rb",
        200_000L to "+200rb",
        500_000L to "+500rb",
        1_000_000L to "+1jt"
    )

    val composeImePadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val measuredKeyboardHeight = rememberDialogKeyboardHeight()
    val activeKeyboardHeight = maxOf(composeImePadding, measuredKeyboardHeight)
    val isKeyboardOpen = activeKeyboardHeight > 50.dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        DialogResizeEffect()
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = activeKeyboardHeight)
                .systemBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = if (isKeyboardOpen) Alignment.TopCenter else Alignment.Center
        ) {
            val maxCardHeight = maxHeight - (if (isKeyboardOpen) 8.dp else 16.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = if (isKeyboardOpen) 6.dp else 10.dp)
                    .widthIn(max = 440.dp)
                    .heightIn(max = maxCardHeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Tambah Dana Pos Spending",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Dana akan diambil dari Saldo Kas utama untuk menambah alokasi pos '${category.name}'.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = rawInput,
                        onValueChange = { rawInput = CurrencyFormatter.formatThousands(it) },
                        label = { Text("Nominal Tambahan (Rp)") },
                        prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                        placeholder = { Text("Contoh: 100.000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_add_spending_funds")
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presets) { (nominal, label) ->
                            Surface(
                                onClick = {
                                    val current = rawInput.filter { it.isDigit() }.toLongOrNull() ?: 0L
                                    rawInput = CurrencyFormatter.formatThousands((current + nominal).toString())
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Amber500.copy(alpha = 0.12f),
                                border = BorderStroke(0.8.dp, Amber500.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Amber500,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            shape = CircleShape
                        ) {
                            Text("Batal")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amount = rawInput.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                                if (amount > 0) {
                                    onConfirm(amount)
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Color.White),
                            modifier = Modifier.testTag("btn_confirm_add_funds")
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReturnRemainingToCashDialog(
    category: CustomCategoryEntity,
    remainingAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Masukkan Sisa Uang ke Saldo Kas",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Sisa saldo di pos '${category.name}' sebesar:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Emerald500.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = CurrencyFormatter.formatRupiah(remainingAmount),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Emerald500,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
                Text(
                    text = "Uang sisa ini akan langsung dimasukkan kembali ke Saldo Kas utama dan alokasi pos disesuaikan.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                modifier = Modifier.testTag("btn_confirm_return_cash")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = CircleShape
            ) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun ReturnAllRemainingToCashDialog(
    totalRemainingAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Masukkan Sisa Semua Pos ke Kas",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Total sisa saldo dari seluruh pos spending:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Emerald500.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = CurrencyFormatter.formatRupiah(totalRemainingAmount),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Emerald500,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
                Text(
                    text = "Seluruh sisa saldo dari pos spending akan dimasukkan kembali ke Saldo Kas utama.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                modifier = Modifier.testTag("btn_confirm_return_all_cash")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = CircleShape
            ) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun SpendingCategoryFormDialog(
    title: String,
    initialName: String,
    initialBudget: Double,
    initialIcon: String,
    initialColor: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, budget: Double, iconName: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var budgetText by remember {
        mutableStateOf(if (initialBudget > 0) CurrencyFormatter.formatThousands(initialBudget.toLong().toString()) else "")
    }
    var selectedIcon by remember { mutableStateOf(initialIcon) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    val presetIcons = listOf(
        "Restaurant" to "Makan",
        "Fastfood" to "Jajan",
        "LocalCafe" to "Kopi",
        "ShoppingCart" to "Belanja",
        "DirectionsCar" to "Transport",
        "LocalGasStation" to "Bensin",
        "Receipt" to "Tagihan",
        "Theaters" to "Hiburan",
        "LocalHospital" to "Kesehatan",
        "Home" to "Rumah",
        "School" to "Edukasi",
        "Payments" to "Harian"
    )

    val presetColors = listOf(
        "#10B981", "#3B82F6", "#F59E0B", "#EF4444", "#8B5CF6", "#EC4899", "#06B6D4", "#F97316"
    )

    val isValid = name.isNotBlank()
    val submitForm = {
        if (isValid) {
            val budget = budgetText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
            onConfirm(name.trim(), budget, selectedIcon, selectedColor)
        }
    }

    val composeImePadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val measuredKeyboardHeight = rememberDialogKeyboardHeight()
    val activeKeyboardHeight = maxOf(composeImePadding, measuredKeyboardHeight)
    val isKeyboardOpen = activeKeyboardHeight > 50.dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        DialogResizeEffect()
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = activeKeyboardHeight)
                .systemBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = if (isKeyboardOpen) Alignment.TopCenter else Alignment.Center
        ) {
            val maxCardHeight = maxHeight - (if (isKeyboardOpen) 8.dp else 16.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = if (isKeyboardOpen) 6.dp else 10.dp)
                    .widthIn(max = 440.dp)
                    .heightIn(max = maxCardHeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isValid) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Emerald500,
                                modifier = Modifier
                                    .clickable { submitForm() }
                                    .testTag("header_save_spending_cat_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Simpan",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Simpan",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Pos Spending") },
                    placeholder = { Text("Contoh: Untuk Makan, Untuk Harian", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = CurrencyFormatter.formatThousands(it) },
                    label = { Text("Batas Anggaran Bulanan") },
                    placeholder = { Text("Contoh: 500.000 (Opsional)", fontSize = 12.sp) },
                    prefix = { Text("Rp ", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Pilih Ikon",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetIcons) { (iconKey, label) ->
                        val isSelected = selectedIcon == iconKey
                        Surface(
                            onClick = { selectedIcon = iconKey },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Emerald500.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) Emerald500 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = CategoryIconHelper.getIcon(iconKey),
                                    contentDescription = null,
                                    tint = if (isSelected) Emerald500 else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Emerald500 else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Pilih Warna Aksen",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presetColors.forEach { hex ->
                        val color = CategoryIconHelper.parseColor(hex)
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                                .padding(2.dp)
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.6f))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = submitForm,
                        enabled = isValid,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
}

@Composable
fun SetMonthlyBudgetDialog(
    currentLimit: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var rawInput by remember {
        mutableStateOf(CurrencyFormatter.formatThousands(currentLimit.toLong().toString()))
    }

    val presets = listOf(
        1_000_000L to "1 Jt",
        2_500_000L to "2.5 Jt",
        3_000_000L to "3 Jt",
        5_000_000L to "5 Jt",
        7_500_000L to "7.5 Jt",
        10_000_000L to "10 Jt"
    )

    val composeImePadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val measuredKeyboardHeight = rememberDialogKeyboardHeight()
    val activeKeyboardHeight = maxOf(composeImePadding, measuredKeyboardHeight)
    val isKeyboardOpen = activeKeyboardHeight > 50.dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        DialogResizeEffect()
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = activeKeyboardHeight)
                .systemBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = if (isKeyboardOpen) Alignment.TopCenter else Alignment.Center
        ) {
            val maxCardHeight = maxHeight - (if (isKeyboardOpen) 8.dp else 16.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = if (isKeyboardOpen) 6.dp else 10.dp)
                    .widthIn(max = 440.dp)
                    .heightIn(max = maxCardHeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Batas Anggaran Bulanan Total",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Tentukan batas total seluruh pengeluaran per bulan.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = rawInput,
                        onValueChange = { rawInput = CurrencyFormatter.formatThousands(it) },
                        label = { Text("Target Batas Bulanan") },
                        prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                        placeholder = { Text("3.000.000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_monthly_spending_limit")
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presets) { (nominal, label) ->
                            val formattedNominal = CurrencyFormatter.formatThousands(nominal.toString())
                            val isSelected = rawInput == formattedNominal
                            FilterChip(
                                selected = isSelected,
                                onClick = { rawInput = formattedNominal },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            shape = CircleShape
                        ) {
                            Text("Batal")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val digits = rawInput.filter { it.isDigit() }
                                val amount = digits.toDoubleOrNull() ?: 3_000_000.0
                                onConfirm(amount)
                            },
                            shape = CircleShape,
                            modifier = Modifier.testTag("btn_save_monthly_spending_limit")
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }
}
