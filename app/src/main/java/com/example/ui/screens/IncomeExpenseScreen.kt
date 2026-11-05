package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CustomCategoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Rose500
import com.example.util.CategoryIconHelper
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import java.util.Calendar

@Composable
fun IncomeExpenseScreen(
    transactions: List<TransactionEntity>,
    categories: List<CustomCategoryEntity> = emptyList(),
    onAddTransaction: () -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onMoveTransactionCategory: ((transactionId: Long, newCategoryName: String) -> Unit)? = null,
    onOpenCategoryManager: (() -> Unit)? = null,
    onOpenShopeeFoodDriver: (() -> Unit)? = null,
    onOpenMonthlySpending: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = Semua, 1 = Pendapatan, 2 = Pengeluaran
    var searchQuery by remember { mutableStateOf("") }

    // Pengeluaran sasaran (Pengeluaran Tujuan) dicatat di luar pengeluaran umum dan hanya ada di dalam menu tujuan
    val generalTransactions = remember(transactions) {
        transactions.filter { !it.categoryName.equals("Pengeluaran Tujuan", ignoreCase = true) }
    }

    val filteredByType = remember(generalTransactions, selectedTab) {
        when (selectedTab) {
            1 -> generalTransactions.filter { it.type == TransactionType.INCOME }
            2 -> generalTransactions.filter { it.type == TransactionType.EXPENSE }
            else -> generalTransactions
        }
    }

    val finalFilteredList = remember(filteredByType, searchQuery) {
        if (searchQuery.isBlank()) {
            filteredByType
        } else {
            filteredByType.filter { tx ->
                tx.title.contains(searchQuery, ignoreCase = true) ||
                        tx.categoryName.contains(searchQuery, ignoreCase = true) ||
                        tx.wallet.contains(searchQuery, ignoreCase = true) ||
                        tx.note.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val isFilterActive = searchQuery.isNotBlank() || selectedTab != 0

    val totalIncome = generalTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = generalTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    // Deletion confirmation dialog state
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    // Category relocation dialog state
    var transactionToMoveCategory by remember { mutableStateOf<TransactionEntity?>(null) }

    Box(modifier = modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
                // Compact Top Bar with Title and Category Manager button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Daftar Transaksi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${transactions.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (onOpenCategoryManager != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clickable { onOpenCategoryManager() }
                                .testTag("btn_open_category_manager")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Palette,
                                    contentDescription = "Kelola Kategori",
                                    tint = Emerald500,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Kategori",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Compact Financial Summary Bar (Income & Expense in Single Row)
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pemasukan
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Emerald500.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = Emerald500,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Pemasukan",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "+${CurrencyFormatter.formatRupiah(totalIncome)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald500,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(20.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        )

                        // Pengeluaran
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Pengeluaran",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "-${CurrencyFormatter.formatRupiah(totalExpense)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Rose500,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Rose500.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = Rose500,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Access Actions: ShopeeFood & Spending Sejajar 1 Baris lewat Icon Tanpa Kalimat (Rapi & Simpel)
            if (onOpenShopeeFoodDriver != null || onOpenMonthlySpending != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onOpenShopeeFoodDriver != null) {
                            Surface(
                                onClick = onOpenShopeeFoodDriver,
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEE4D2D).copy(alpha = 0.10f),
                                border = BorderStroke(0.8.dp, Color(0xFFEE4D2D).copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("btn_open_shopeefood_from_transactions")
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("🛵", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.DeliveryDining,
                                            contentDescription = "ShopeeFood Driver",
                                            tint = Color(0xFFEE4D2D),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (onOpenMonthlySpending != null) {
                            Surface(
                                onClick = onOpenMonthlySpending,
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("btn_open_spending_from_transactions")
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("📊", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Payments,
                                            contentDescription = "Spending & Anggaran",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Compact Search Bar (Rapi & Tidak Memakan Tempat)
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.5.sp
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("transaction_search_input"),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Cari transaksi, kategori, dompet...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { searchQuery = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Hapus Pencarian",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Compact Segmented Filter Tabs & Transaction Count (1 Baris Ringkas)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val tabs = listOf(
                            Triple(0, "Semua", transactions.size),
                            Triple(1, "Masuk", transactions.count { it.type == TransactionType.INCOME }),
                            Triple(2, "Keluar", transactions.count { it.type == TransactionType.EXPENSE })
                        )

                        tabs.forEach { (tabIndex, label, count) ->
                            val isSelected = selectedTab == tabIndex
                            val activeColor = when (tabIndex) {
                                1 -> Emerald500
                                2 -> Rose500
                                else -> MaterialTheme.colorScheme.primary
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) activeColor else Color.Transparent)
                                    .clickable { selectedTab = tabIndex }
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$label ($count)",
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    val totalSelected = finalFilteredList.sumOf { if (it.type == TransactionType.INCOME) it.amount else -it.amount }
                    Text(
                        text = "${finalFilteredList.size} item • " + (if (totalSelected >= 0) "+" else "") + CurrencyFormatter.formatCompactRupiah(totalSelected),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (totalSelected >= 0) Emerald500 else Rose500
                    )
                }
            }

            // List of Transactions or Empty State
            if (finalFilteredList.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "Tidak ada transaksi yang cocok",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tidak ditemukan transaksi sesuai pencarian atau filter yang dipilih.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            if (isFilterActive) {
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = {
                                        searchQuery = ""
                                        selectedTab = 0
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reset Pencarian & Filter", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                items(finalFilteredList, key = { it.id }) { tx ->
                    TransactionItemRow(
                        transaction = tx,
                        onDelete = { transactionToDelete = tx },
                        onMoveCategory = { transactionToMoveCategory = tx }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Move Single Transaction Category Dialog
        transactionToMoveCategory?.let { tx ->
            val relevantCategories = categories.filter {
                if (tx.type == TransactionType.INCOME) !it.isExpense else it.isExpense
            }
            var selectedCatName by remember { mutableStateOf(tx.categoryName) }

            AlertDialog(
                onDismissRequest = { transactionToMoveCategory = null },
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Emerald500)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pindahkan Kategori", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Pindahkan transaksi '${tx.title}' (${CurrencyFormatter.formatRupiah(tx.amount)}) ke kategori lain:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text("Kategori saat ini: ${tx.categoryName}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

                        if (relevantCategories.isEmpty()) {
                            Text(
                                text = "Tidak ada kategori yang tersedia untuk tipe ini.",
                                fontSize = 12.sp,
                                color = Rose500
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                relevantCategories.forEach { cat ->
                                    val isSelected = selectedCatName.equals(cat.name, ignoreCase = true)
                                    val catColor = CategoryIconHelper.parseColor(cat.colorHex)
                                    val catIcon = CategoryIconHelper.getIcon(cat.iconName)

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) catColor.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isSelected) BorderStroke(1.5.dp, catColor) else null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedCatName = cat.name }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(catColor.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(catIcon, contentDescription = null, tint = catColor, modifier = Modifier.size(16.dp))
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = cat.name,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = catColor, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (onMoveTransactionCategory != null && selectedCatName.isNotBlank()) {
                                onMoveTransactionCategory(tx.id, selectedCatName)
                                android.widget.Toast.makeText(
                                    context,
                                    "Kategori '${tx.title}' diubah ke '$selectedCatName'",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                            transactionToMoveCategory = null
                        },
                        enabled = selectedCatName.isNotBlank() && selectedCatName != tx.categoryName,
                        shape = CircleShape
                    ) {
                        Text("Pindahkan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { transactionToMoveCategory = null }, shape = CircleShape) {
                        Text("Batal")
                    }
                }
            )
        }

        // Deletion Confirmation Dialog to prevent accidental data loss
        if (transactionToDelete != null) {
            val tx = transactionToDelete!!
            AlertDialog(
                onDismissRequest = { transactionToDelete = null },
                modifier = Modifier.testTag("income_expense_delete_dialog"),
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
                        text = "Hapus Transaksi?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Apakah Anda yakin ingin menghapus transaksi ini secara permanen? Tindakan ini tidak dapat dibatalkan.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = tx.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${tx.categoryName} • ${tx.wallet}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = (if (tx.type == TransactionType.INCOME) "+ " else "- ") +
                                                CurrencyFormatter.formatRupiah(tx.amount),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (tx.type == TransactionType.INCOME) Emerald500 else Rose500
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteTransaction(tx)
                            transactionToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Rose500),
                        shape = CircleShape,
                        modifier = Modifier.testTag("confirm_delete_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hapus Permanen", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { transactionToDelete = null },
                        shape = CircleShape,
                        modifier = Modifier.testTag("cancel_delete_btn")
                    ) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
fun TransactionItemRow(
    transaction: TransactionEntity,
    onDelete: () -> Unit,
    onMoveCategory: (() -> Unit)? = null
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val badgeColor = if (isIncome) Emerald500 else Rose500
    val catIcon = CategoryIconHelper.getIcon(CategoryIconHelper.inferIconForCategory(transaction.categoryName))

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge with income/expense color tint
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = catIcon,
                    contentDescription = transaction.categoryName,
                    tint = badgeColor,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.width(9.dp))

            // Main Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Clickable Category Badge for quick moving
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable(enabled = onMoveCategory != null) {
                            onMoveCategory?.invoke()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = transaction.categoryName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (onMoveCategory != null) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Pindah Kategori",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = " • ",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = transaction.wallet,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.note,
                        fontSize = 9.5.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Column: Amount & (Date + Delete)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                val sign = if (isIncome) "+" else "-"
                Text(
                    text = "$sign${CurrencyFormatter.formatRupiah(transaction.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = badgeColor,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = DateUtils.formatShortDate(transaction.dateEpochMillis),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Transaksi",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}
