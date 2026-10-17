package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.DialogResizeEffect
import com.example.ui.components.rememberDialogKeyboardHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.data.model.CustomCategoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Rose500
import com.example.util.AutoCategorizer
import com.example.util.CategoryIconHelper
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryManagementScreen(
    categories: List<CustomCategoryEntity>,
    transactions: List<TransactionEntity> = emptyList(),
    onAddCategory: (name: String, isExpense: Boolean, iconName: String, colorHex: String, monthlyBudgetLimit: Double) -> Unit,
    onUpdateCategory: (CustomCategoryEntity) -> Unit,
    onDeleteCategory: (CustomCategoryEntity) -> Unit,
    onDeleteCategoryWithMove: ((category: CustomCategoryEntity, targetCategoryForExistingTxs: String?) -> Unit)? = null,
    onMoveTransactions: ((sourceCategory: String, targetCategory: String) -> Unit)? = null,
    onAutoCategorize: ((proposals: List<Pair<TransactionEntity, CustomCategoryEntity>>?) -> Unit)? = null,
    onUpdateBudget: ((categoryId: Long, newBudget: Double) -> Unit)? = null,
    onTriggerTestAlert: ((CustomCategoryEntity) -> Unit)? = null,
    onRestoreDefaults: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showRestoreDefaultsDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var categoryToEdit by remember { mutableStateOf<CustomCategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CustomCategoryEntity?>(null) }
    var categoryToSetBudget by remember { mutableStateOf<CustomCategoryEntity?>(null) }
    var categoryToMoveTransactions by remember { mutableStateOf<CustomCategoryEntity?>(null) }
    var showAutoCategorizeDialog by remember { mutableStateOf(false) }

    var selectedTypeFilter by remember { mutableStateOf<Boolean?>(null) } // null = All, true = Expense, false = Income

    // Filter current month expense transactions
    val currentMonthExpenses = remember(transactions) {
        transactions.filter {
            it.type == TransactionType.EXPENSE && DateUtils.isCurrentMonth(it.dateEpochMillis)
        }
    }

    // Peringatan HANYA muncul saat pengeluaran sudah LEBIH DARI 80%. Jika belum (>80%), jangan munculkan.
    val categoriesOver80 = remember(categories, currentMonthExpenses) {
        categories.filter { cat ->
            if (!cat.isExpense || cat.monthlyBudgetLimit <= 0) false
            else {
                val spent = currentMonthExpenses.filter { tx -> isCategoryMatch(tx.categoryName, cat.name) }.sumOf { it.amount }
                (spent / cat.monthlyBudgetLimit) * 100.0 > 80.0
            }
        }
    }

    // Auto-categorize suggestions
    val autoCategorizeProposals = remember(transactions, categories) {
        AutoCategorizer.getAutoCategorizeProposals(transactions, categories)
    }

    val filteredCategories = remember(categories, selectedTypeFilter, searchQuery) {
        val typeFiltered = when (selectedTypeFilter) {
            true -> categories.filter { it.isExpense }
            false -> categories.filter { !it.isExpense }
            null -> categories
        }
        if (searchQuery.isBlank()) {
            typeFiltered
        } else {
            val q = searchQuery.trim().lowercase()
            typeFiltered.filter { it.name.lowercase().contains(q) }
        }
    }

    Box(modifier = modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = Emerald500,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Kelola Kategori & Anggaran",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Kategori kustom, auto-kategorisasi cerdas, dan kontrol anggaran",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // SMART AUTO-CATEGORIZATION HERO CARD
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Kategorisasi Cerdas",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (autoCategorizeProposals.isNotEmpty())
                                        "${autoCategorizeProposals.size} transaksi dapat disesuaikan ke kategori tepat"
                                    else
                                        "Semua transaksi (${transactions.size}) sudah rapi terorganisir",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Analisis otomatis catatan transaksi",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { showAutoCategorizeDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (autoCategorizeProposals.isNotEmpty()) Emerald500
                                    else MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("btn_auto_categorize_trigger")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (autoCategorizeProposals.isNotEmpty())
                                        "Kategorikan (${autoCategorizeProposals.size})"
                                    else
                                        "Pindai",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Banner Peringatan: HANYA muncul jika sudah ada kategori yang pengeluarannya LEBIH DARI 80%
            if (categoriesOver80.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Amber500.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Amber500.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Amber500,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "⚠️ Peringatan: ${categoriesOver80.size} Kategori Melebihi 80% Batas Anggaran",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Kategori (${categoriesOver80.joinToString { it.name }}) telah melampaui 80% batas anggaran bulanan yang ditentukan.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons: Tambah Kategori & Pulihkan Default
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_category_header_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tambah Kategori", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    if (onRestoreDefaults != null) {
                        OutlinedButton(
                            onClick = { showRestoreDefaultsDialog = true },
                            shape = CircleShape,
                            modifier = Modifier.testTag("restore_defaults_btn")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kategori Default", fontSize = 13.sp)
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari kategori (Makanan, Belanja, Tagihan, dll)...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_category_input")
                )
            }

            // Filter Tabs (Semua, Pengeluaran, Pendapatan)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { selectedTypeFilter = null },
                        shape = CircleShape,
                        label = { Text("Semua (${categories.size})", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedTypeFilter == true,
                        onClick = { selectedTypeFilter = true },
                        shape = CircleShape,
                        label = { Text("Pengeluaran (${categories.count { it.isExpense }})", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedTypeFilter == false,
                        onClick = { selectedTypeFilter = false },
                        shape = CircleShape,
                        label = { Text("Pendapatan (${categories.count { !it.isExpense }})", fontSize = 11.sp) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Kategori (${filteredCategories.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Tap ⇄ untuk pindah transaksi",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (filteredCategories.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (searchQuery.isNotEmpty()) Icons.Default.Search else Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Kategori '$searchQuery' tidak ditemukan"
                                else "Belum ada kategori",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Coba kata kunci lain atau tambahkan kategori baru dengan tombol di atas."
                                else "Klik 'Kategori Default' untuk memuat kategori standar atau buat kategori kustom baru.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (onRestoreDefaults != null && categories.isEmpty()) {
                                Button(
                                    onClick = { showRestoreDefaultsDialog = true },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Muat Kategori Default")
                                }
                            }
                        }
                    }
                }
            }

            items(filteredCategories, key = { it.id }) { cat ->
                val spentInMonth = if (cat.isExpense) {
                    currentMonthExpenses.filter { tx ->
                        isCategoryMatch(tx.categoryName, cat.name)
                    }.sumOf { it.amount }
                } else 0.0

                val txCount = transactions.count { isCategoryMatch(it.categoryName, cat.name) }

                CategoryItemCard(
                    category = cat,
                    spentInMonth = spentInMonth,
                    transactionCount = txCount,
                    onEdit = { categoryToEdit = cat },
                    onDelete = { categoryToDelete = cat },
                    onSetBudget = { categoryToSetBudget = cat },
                    onMoveTransactions = { categoryToMoveTransactions = cat },
                    onTriggerTest = onTriggerTestAlert?.let { { it(cat) } }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Add Category Dialog
    if (showAddDialog) {
        CategoryFormDialog(
            title = "Tambah Kategori Kustom",
            initialName = "",
            initialIsExpense = true,
            initialIcon = "Fastfood",
            initialColor = "#10B981",
            initialBudgetLimit = 0.0,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, isExpense, iconName, colorHex, budgetLimit, autoCategorizeExisting ->
                onAddCategory(name, isExpense, iconName, colorHex, budgetLimit)
                if (autoCategorizeExisting && onAutoCategorize != null) {
                    onAutoCategorize(null)
                    Toast.makeText(context, "Kategori '$name' dibuat & transaksi terkait disesuaikan!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Kategori '$name' berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                }
                showAddDialog = false
            }
        )
    }

    // Edit Category Dialog
    categoryToEdit?.let { cat ->
        CategoryFormDialog(
            title = "Edit Kategori",
            initialName = cat.name,
            initialIsExpense = cat.isExpense,
            initialIcon = cat.iconName,
            initialColor = cat.colorHex,
            initialBudgetLimit = cat.monthlyBudgetLimit,
            isEditing = true,
            onDismiss = { categoryToEdit = null },
            onConfirm = { name, isExpense, iconName, colorHex, budgetLimit, _ ->
                val oldName = cat.name
                if (oldName != name && onMoveTransactions != null) {
                    onMoveTransactions(oldName, name)
                }
                onUpdateCategory(
                    cat.copy(
                        name = name,
                        isExpense = isExpense,
                        iconName = iconName,
                        colorHex = colorHex,
                        monthlyBudgetLimit = budgetLimit
                    )
                )
                Toast.makeText(context, "Kategori '$name' berhasil diperbarui", Toast.LENGTH_SHORT).show()
                categoryToEdit = null
            }
        )
    }

    // Restore Defaults Dialog
    if (showRestoreDefaultsDialog && onRestoreDefaults != null) {
        AlertDialog(
            onDismissRequest = { showRestoreDefaultsDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Restore, contentDescription = null, tint = Emerald500)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Muat Kategori Default?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Aplikasi akan memulihkan daftar kategori pengeluaran dan pendapatan standar (Makanan & Minuman, Transportasi, Belanja, Tagihan & Utilitas, Hiburan, Kesehatan & Medis, Pendidikan, Tempat Tinggal, Olahraga, Lain-lain, dll.) yang mungkin belum ada atau terhapus.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "✓ Kategori kustom yang sudah Anda buat TETAP AMAN dan tidak akan terhapus.\n✓ Transaksi yang sudah ada TETAP AMAN dan tidak akan berubah.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreDefaults()
                        showRestoreDefaultsDialog = false
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                ) {
                    Text("Pulihkan Default")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRestoreDefaultsDialog = false },
                    shape = CircleShape
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Quick Set Budget Dialog
    categoryToSetBudget?.let { cat ->
        QuickSetBudgetDialog(
            category = cat,
            onDismiss = { categoryToSetBudget = null },
            onSave = { newBudget ->
                if (onUpdateBudget != null) {
                    onUpdateBudget(cat.id, newBudget)
                } else {
                    onUpdateCategory(cat.copy(monthlyBudgetLimit = newBudget))
                }
                categoryToSetBudget = null
            }
        )
    }

    // Move Transactions Dialog
    categoryToMoveTransactions?.let { sourceCat ->
        val candidateTargets = categories.filter { it.id != sourceCat.id && it.isExpense == sourceCat.isExpense }
        MoveCategoryTransactionsDialog(
            sourceCategory = sourceCat,
            availableTargets = candidateTargets,
            transactions = transactions.filter { isCategoryMatch(it.categoryName, sourceCat.name) },
            onDismiss = { categoryToMoveTransactions = null },
            onConfirm = { targetCatName ->
                if (onMoveTransactions != null) {
                    onMoveTransactions(sourceCat.name, targetCatName)
                }
                Toast.makeText(context, "Transaksi dari '${sourceCat.name}' dipindahkan ke '$targetCatName'", Toast.LENGTH_SHORT).show()
                categoryToMoveTransactions = null
            }
        )
    }

    // Auto-Categorization Review Dialog
    if (showAutoCategorizeDialog) {
        AutoCategorizationDialog(
            proposals = autoCategorizeProposals,
            onDismiss = { showAutoCategorizeDialog = false },
            onApply = { selectedProposals ->
                if (onAutoCategorize != null) {
                    onAutoCategorize(selectedProposals)
                }
                Toast.makeText(context, "${selectedProposals.size} transaksi berhasil dikategorikan!", Toast.LENGTH_SHORT).show()
                showAutoCategorizeDialog = false
            }
        )
    }

    // Delete Confirmation Dialog with optional transaction re-assignment
    categoryToDelete?.let { cat ->
        val txCount = transactions.count { isCategoryMatch(it.categoryName, cat.name) }
        val candidateTargets = categories.filter { it.id != cat.id && it.isExpense == cat.isExpense }
        var selectedTargetCategory by remember { mutableStateOf(candidateTargets.firstOrNull()?.name ?: "") }

        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Hapus Kategori?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Apakah Anda yakin ingin menghapus kategori '${cat.name}'?")

                    if (txCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Amber500.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Amber500.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "⚠️ Terdapat $txCount transaksi di kategori ini.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Amber500
                                )
                                Text(
                                    text = "Pindahkan seluruh transaksi tersebut ke:",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (candidateTargets.isNotEmpty()) {
                                    var dropdownOpen by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.padding(top = 6.dp)) {
                                        OutlinedButton(
                                            onClick = { dropdownOpen = true },
                                            shape = CircleShape,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(selectedTargetCategory.ifBlank { "Pilih Kategori Tujuan" }, fontSize = 12.sp)
                                        }
                                        DropdownMenu(
                                            expanded = dropdownOpen,
                                            onDismissRequest = { dropdownOpen = false }
                                        ) {
                                            candidateTargets.forEach { target ->
                                                DropdownMenuItem(
                                                    text = { Text(target.name) },
                                                    onClick = {
                                                        selectedTargetCategory = target.name
                                                        dropdownOpen = false
                                                    }
                                                )
                                            }
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
                        if (txCount > 0 && selectedTargetCategory.isNotBlank() && onDeleteCategoryWithMove != null) {
                            onDeleteCategoryWithMove(cat, selectedTargetCategory)
                        } else if (txCount > 0 && selectedTargetCategory.isNotBlank() && onMoveTransactions != null) {
                            onMoveTransactions(cat.name, selectedTargetCategory)
                            onDeleteCategory(cat)
                        } else {
                            onDeleteCategory(cat)
                        }
                        categoryToDelete = null
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { categoryToDelete = null },
                    shape = CircleShape
                ) { Text("Batal") }
            }
        )
    }
}

private fun isCategoryMatch(txCategory: String, customCatName: String): Boolean {
    if (txCategory.equals(customCatName, ignoreCase = true)) return true
    val normTx = txCategory.replace("&", "").replace("/", "").replace("-", "").replace(" ", "").lowercase()
    val normCat = customCatName.replace("&", "").replace("/", "").replace("-", "").replace(" ", "").lowercase()
    return normTx.contains(normCat) || normCat.contains(normTx)
}

@Composable
fun CategoryItemCard(
    category: CustomCategoryEntity,
    spentInMonth: Double = 0.0,
    transactionCount: Int = 0,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetBudget: () -> Unit,
    onMoveTransactions: () -> Unit,
    onTriggerTest: (() -> Unit)? = null
) {
    val categoryColor = CategoryIconHelper.parseColor(category.colorHex)
    val icon = CategoryIconHelper.getIcon(category.iconName)

    val hasBudget = category.isExpense && category.monthlyBudgetLimit > 0
    val budgetLimit = category.monthlyBudgetLimit
    val percentage = if (hasBudget) (spentInMonth / budgetLimit) * 100.0 else 0.0
    val isOver80 = hasBudget && percentage > 80.0
    val isOver100 = hasBudget && percentage > 100.0

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOver100) Rose500.copy(alpha = 0.08f)
            else if (isOver80) Amber500.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isOver100) BorderStroke(1.dp, Rose500.copy(alpha = 0.4f))
        else if (isOver80) BorderStroke(1.dp, Amber500.copy(alpha = 0.4f))
        else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(categoryColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = category.name,
                            tint = categoryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = category.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(categoryColor)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (category.isDefault) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = if (category.isDefault) "Default" else "Kustom",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (category.isDefault) MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${if (category.isExpense) "Pengeluaran" else "Pemasukan"} • $transactionCount tx",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pindah Transaksi Button
                    if (transactionCount > 0) {
                        IconButton(
                            onClick = onMoveTransactions,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Pindahkan Transaksi",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (category.isExpense && onTriggerTest != null) {
                        IconButton(
                            onClick = onTriggerTest,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("test_alert_${category.id}")
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = "Uji Notifikasi Alert",
                                tint = if (isOver80) Amber500 else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Kategori",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Hapus Kategori",
                            tint = Rose500.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Budget Progress & Alert Section for Expense Categories
            if (category.isExpense) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        if (hasBudget) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Batas Anggaran Bulanan",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onSetBudget() }
                                ) {
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(budgetLimit),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Tune,
                                        contentDescription = "Ubah Batas",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Progress Bar
                            val progressFraction = (percentage / 100.0).toFloat().coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (isOver100) Rose500 else if (isOver80) Amber500 else Emerald500,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Terpakai: ${CurrencyFormatter.formatRupiah(spentInMonth)}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format(Locale.getDefault(), "%.1f", percentage)}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOver100) Rose500 else if (isOver80) Amber500 else Emerald500
                                )
                            }

                            // Alert Tag if >= 80%
                            if (isOver80) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (isOver100) Rose500.copy(alpha = 0.15f) else Amber500.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, if (isOver100) Rose500 else Amber500)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (isOver100) Rose500 else Amber500,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isOver100) "🚨 Melebihi Batas Anggaran!"
                                            else "⚠️ Telah Melebihi 80% Anggaran!",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOver100) Rose500 else Amber500
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Batas Anggaran Bulanan",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Belum diatur (notifikasi non-aktif)",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                OutlinedButton(
                                    onClick = onSetBudget,
                                    shape = CircleShape,
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Atur Batas", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog to Move All Transactions from Source Category to a Selected Target Category.
 */
@Composable
fun MoveCategoryTransactionsDialog(
    sourceCategory: CustomCategoryEntity,
    availableTargets: List<CustomCategoryEntity>,
    transactions: List<TransactionEntity>,
    onDismiss: () -> Unit,
    onConfirm: (targetCategoryName: String) -> Unit
) {
    var selectedTargetName by remember { mutableStateOf(availableTargets.firstOrNull()?.name ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = Emerald500)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pindahkan Transaksi", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Pindahkan ${transactions.size} transaksi dari '${sourceCategory.name}' ke kategori lain:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (availableTargets.isEmpty()) {
                    Text(
                        text = "Belum ada kategori tujuan lainnya. Silakan buat kategori baru terlebih dahulu.",
                        fontSize = 12.sp,
                        color = Rose500
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableTargets.forEach { targetCat ->
                            val isSelected = selectedTargetName == targetCat.name
                            val catColor = CategoryIconHelper.parseColor(targetCat.colorHex)
                            val catIcon = CategoryIconHelper.getIcon(targetCat.iconName)

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) catColor.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) BorderStroke(1.5.dp, catColor) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTargetName = targetCat.name }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(catColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(catIcon, contentDescription = null, tint = catColor, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = targetCat.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = catColor)
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
                onClick = { if (selectedTargetName.isNotBlank()) onConfirm(selectedTargetName) },
                enabled = selectedTargetName.isNotBlank(),
                shape = CircleShape
            ) {
                Text("Pindahkan ${transactions.size} Transaksi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = CircleShape) { Text("Batal") }
        }
    )
}

/**
 * Review & Apply Auto-Categorization Proposals Dialog.
 */
@Composable
fun AutoCategorizationDialog(
    proposals: List<Pair<TransactionEntity, CustomCategoryEntity>>,
    onDismiss: () -> Unit,
    onApply: (selected: List<Pair<TransactionEntity, CustomCategoryEntity>>) -> Unit
) {
    val selectedItems = remember { mutableStateListOf<Pair<TransactionEntity, CustomCategoryEntity>>().apply { addAll(proposals) } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Emerald500.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Emerald500)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Kategorisasi Transaksi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = "${proposals.size} transaksi cocok dengan kategori spesifik",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (proposals.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Emerald500, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Semua transaksi sudah berada di kategori yang sesuai!", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Daftar Penyesuaian:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = {
                            if (selectedItems.size == proposals.size) selectedItems.clear()
                            else {
                                selectedItems.clear()
                                selectedItems.addAll(proposals)
                            }
                        }) {
                            Text(if (selectedItems.size == proposals.size) "Batal Pilih Semua" else "Pilih Semua", fontSize = 11.sp)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(proposals) { item ->
                            val (tx, targetCat) = item
                            val isChecked = selectedItems.contains(item)
                            val catColor = CategoryIconHelper.parseColor(targetCat.colorHex)

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = if (isChecked) BorderStroke(1.dp, catColor.copy(alpha = 0.5f)) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) selectedItems.remove(item) else selectedItems.add(item)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked) selectedItems.add(item) else selectedItems.remove(item)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Text(
                                                text = tx.categoryName,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                modifier = Modifier.size(10.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = targetCat.name,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = catColor
                                            )
                                        }
                                    }
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(tx.amount),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (tx.type == TransactionType.INCOME) Emerald500 else Rose500
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, shape = CircleShape) { Text("Tutup") }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (proposals.isNotEmpty()) {
                        Button(
                            onClick = { onApply(selectedItems.toList()) },
                            enabled = selectedItems.isNotEmpty(),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                        ) {
                            Text("Terapkan (${selectedItems.size})")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickSetBudgetDialog(
    category: CustomCategoryEntity,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var budgetStr by remember {
        mutableStateOf(
            if (category.monthlyBudgetLimit > 0) CurrencyFormatter.formatThousands(category.monthlyBudgetLimit.toLong().toString()) else ""
        )
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = Emerald500)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Atur Batas Anggaran", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }

                    Text(
                        text = "Tentukan batas pengeluaran bulanan untuk kategori '${category.name}'.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = budgetStr,
                        onValueChange = { budgetStr = CurrencyFormatter.formatThousands(it) },
                        label = { Text("Batas Anggaran (Rp)") },
                        placeholder = { Text("Contoh: 1.500.000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text(
                                text = "Notifikasi peringatan akan aktif otomatis jika pengeluaran > 80% dari batas ini.",
                                fontSize = 11.sp,
                                color = Amber500
                            )
                        }
                    )

                    // Quick preset buttons
                    Text("Pilihan Cepat:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(500000L, 1000000L, 2000000L, 3000000L).forEach { amount ->
                            val formattedPreset = CurrencyFormatter.formatThousands(amount.toString())
                            FilterChip(
                                selected = budgetStr == formattedPreset,
                                onClick = { budgetStr = formattedPreset },
                                shape = CircleShape,
                                label = { Text(CurrencyFormatter.formatRupiah(amount.toDouble()).replace(",00", ""), fontSize = 10.sp) }
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
                        ) { Text("Batal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val value = budgetStr.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                                onSave(value)
                            },
                            shape = CircleShape
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryFormDialog(
    title: String,
    initialName: String,
    initialIsExpense: Boolean,
    initialIcon: String,
    initialColor: String,
    initialBudgetLimit: Double = 0.0,
    isEditing: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (name: String, isExpense: Boolean, iconName: String, colorHex: String, monthlyBudgetLimit: Double, autoCategorizeExisting: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var isExpense by remember { mutableStateOf(initialIsExpense) }
    var selectedIcon by remember { mutableStateOf(initialIcon) }
    var selectedColor by remember { mutableStateOf(initialColor) }
    var autoCategorizeExisting by remember { mutableStateOf(true) }
    var budgetLimitStr by remember {
        mutableStateOf(if (initialBudgetLimit > 0) CurrencyFormatter.formatThousands(initialBudgetLimit.toLong().toString()) else "")
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
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                // Input Nama Kategori
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Kategori") },
                    placeholder = { Text("Contoh: Laundry, Pulsa, Kopi, Gym") },
                    singleLine = true,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_name_input")
                )

                // Pilih Tipe Kategori
                Text("Tipe Kategori:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isExpense,
                        onClick = { isExpense = true },
                        shape = CircleShape,
                        label = { Text("Pengeluaran", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = !isExpense,
                        onClick = { isExpense = false },
                        shape = CircleShape,
                        label = { Text("Pendapatan", fontSize = 12.sp) }
                    )
                }

                // Saran Kategori Cepat (Presets)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Saran Kategori Cepat:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val quickPresets = if (isExpense) {
                        listOf(
                            Triple("Makanan & Minuman", "Restaurant", "#EF4444"),
                            Triple("Transportasi", "DirectionsCar", "#F97316"),
                            Triple("Belanja", "ShoppingCart", "#EC4899"),
                            Triple("Tagihan & Utilitas", "Receipt", "#3B82F6"),
                            Triple("Hiburan", "Theaters", "#8B5CF6"),
                            Triple("Kesehatan & Medis", "LocalHospital", "#10B981"),
                            Triple("Pendidikan", "School", "#06B6D4"),
                            Triple("Tempat Tinggal", "Home", "#6366F1"),
                            Triple("Olahraga", "FitnessCenter", "#14B8A6"),
                            Triple("Kopi & Kafe", "LocalCafe", "#F59E0B"),
                            Triple("Lain-lain", "Build", "#64748B")
                        )
                    } else {
                        listOf(
                            Triple("Gaji Pokok", "AttachMoney", "#10B981"),
                            Triple("Bonus & Insentif", "TrendingUp", "#06B6D4"),
                            Triple("Usaha & Bisnis", "Work", "#3B82F6"),
                            Triple("Investasi & Dividen", "Savings", "#8B5CF6"),
                            Triple("Hadiah & Lainnya", "CardGiftcard", "#EC4899")
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickPresets) { (presetName, presetIcon, presetColor) ->
                            SuggestionChip(
                                onClick = {
                                    name = presetName
                                    selectedIcon = presetIcon
                                    selectedColor = presetColor
                                },
                                label = { Text(presetName, fontSize = 11.sp) },
                                shape = CircleShape
                            )
                        }
                    }
                }

                // Input Batas Anggaran (Khusus Pengeluaran)
                if (isExpense) {
                    OutlinedTextField(
                        value = budgetLimitStr,
                        onValueChange = { budgetLimitStr = CurrencyFormatter.formatThousands(it) },
                        label = { Text("Batas Anggaran Bulanan (Rp)") },
                        placeholder = { Text("Contoh: 1.500.000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_budget_input"),
                        supportingText = {
                            Text(
                                text = "Sistem akan otomatis mengirim notifikasi jika pengeluaran > 80% dari batas ini.",
                                fontSize = 11.sp,
                                color = Amber500
                            )
                        }
                    )
                }

                // Pilih Ikon
                Text("Pilih Ikon Visual:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryIconHelper.availableIcons.forEach { (iconKey, vector) ->
                        val isSelected = selectedIcon.equals(iconKey, ignoreCase = true)
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) CategoryIconHelper.parseColor(selectedColor).copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) BorderStroke(2.dp, CategoryIconHelper.parseColor(selectedColor)) else null,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { selectedIcon = iconKey }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = vector,
                                    contentDescription = iconKey,
                                    tint = if (isSelected) CategoryIconHelper.parseColor(selectedColor) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Pilih Warna
                Text("Pilih Warna Aksen:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryIconHelper.availableColors.forEach { (hex, color) ->
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Auto-categorize existing checkbox
                if (!isEditing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { autoCategorizeExisting = !autoCategorizeExisting },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = autoCategorizeExisting,
                            onCheckedChange = { autoCategorizeExisting = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Otomatis sesuaikan transaksi yang cocok dengan kategori ini",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Live Preview
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentColor = CategoryIconHelper.parseColor(selectedColor)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(currentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CategoryIconHelper.getIcon(selectedIcon),
                                contentDescription = null,
                                tint = currentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Pratinjau Kategori", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = name.ifBlank { "Nama Kategori" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = currentColor
                            )
                            if (isExpense && budgetLimitStr.isNotBlank()) {
                                Text(
                                    text = "Batas: ${CurrencyFormatter.formatRupiah(budgetLimitStr.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0)}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                        ) { Text("Batal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    val budget = if (isExpense) budgetLimitStr.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0 else 0.0
                                    onConfirm(name.trim(), isExpense, selectedIcon, selectedColor, budget, autoCategorizeExisting)
                                }
                            },
                            shape = CircleShape,
                            modifier = Modifier.testTag("save_category_button")
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }
}
