package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import com.example.ui.components.DialogResizeEffect
import com.example.ui.components.rememberDialogKeyboardHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FinancialGoalEntity
import com.example.data.model.TransactionEntity
import com.example.ui.theme.Amber500
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Rose500
import com.example.util.CategoryIconHelper
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import java.util.Calendar

@Composable
fun GoalsScreen(
    goals: List<FinancialGoalEntity>,
    transactions: List<TransactionEntity> = emptyList(),
    onAddGoal: (
        name: String,
        targetAmount: Double,
        targetDateEpochMillis: Long,
        category: String,
        iconName: String,
        colorHex: String,
        note: String,
        initialSaved: Double
    ) -> Unit,
    onAllocateFunds: (FinancialGoalEntity, Double) -> Unit,
    onDeleteGoal: (FinancialGoalEntity) -> Unit,
    onSpendGoalFunds: (goal: FinancialGoalEntity, amount: Double, title: String, note: String, dateMillis: Long) -> Unit = { _, _, _, _, _ -> },
    onDeleteGoalExpense: (goal: FinancialGoalEntity, transaction: TransactionEntity) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalToAllocate by remember { mutableStateOf<FinancialGoalEntity?>(null) }
    var goalToSpend by remember { mutableStateOf<FinancialGoalEntity?>(null) }
    var goalForHistory by remember { mutableStateOf<FinancialGoalEntity?>(null) }
    var allocateAmountText by remember { mutableStateOf("") }

    val totalTarget = goals.sumOf { it.targetAmount }
    val totalSaved = goals.sumOf { it.currentSavedAmount }
    val totalRemaining = (totalTarget - totalSaved).coerceAtLeast(0.0)
    val overallProgress = if (totalTarget > 0) ((totalSaved / totalTarget) * 100.0).coerceIn(0.0, 100.0) else 0.0
    val completedCount = goals.count { it.isCompleted }

    Box(modifier = modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Tujuan Keuangan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Rencana tabungan & sasaran impian",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showAddGoalDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_top_add_goal")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (goals.isNotEmpty()) {
                // Overview Summary Card (Only shown when there are goals)
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Emerald500.copy(alpha = 0.22f),
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Total Tabungan",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = Emerald500.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = "${String.format(Locale.getDefault(), "%.1f", overallProgress)}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Emerald500,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = CurrencyFormatter.formatRupiah(totalSaved),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = "Target: ${CurrencyFormatter.formatRupiah(totalTarget)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                val animProgress by animateFloatAsState(
                                    targetValue = (overallProgress / 100.0).toFloat(),
                                    label = "overallGoalProgress"
                                )
                                LinearProgressIndicator(
                                    progress = { animProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Emerald500,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Sisa Target", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            CurrencyFormatter.formatCompactRupiah(totalRemaining),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Amber500
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Tercapai", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            "$completedCount dari ${goals.size}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Goals List Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daftar Sasaran",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "+ Tambah Sasaran",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Emerald500,
                            modifier = Modifier
                                .clickable { showAddGoalDialog = true }
                                .padding(4.dp)
                                .testTag("btn_text_add_goal")
                        )
                    }
                }

                items(goals, key = { it.id }) { goal ->
                    val goalExpenses = remember(transactions, goal.name) {
                        transactions.filter {
                            it.categoryName.equals("Pengeluaran Tujuan", ignoreCase = true) &&
                            (it.wallet.equals("Tujuan: ${goal.name}", ignoreCase = true) ||
                             it.wallet.equals("Tabungan: ${goal.name}", ignoreCase = true) ||
                             it.wallet.contains(goal.name, ignoreCase = true))
                        }
                    }
                    GoalCardItem(
                        goal = goal,
                        goalExpenses = goalExpenses,
                        onAllocate = {
                            goalToAllocate = goal
                            allocateAmountText = ""
                        },
                        onSpend = {
                            goalToSpend = goal
                        },
                        onViewHistory = {
                            goalForHistory = goal
                        },
                        onDelete = { onDeleteGoal(goal) }
                    )
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(36.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Emerald500.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = Emerald500,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Belum Ada Sasaran",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Buat target tabungan pertama Anda untuk mulai mengalokasikan dana.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { showAddGoalDialog = true },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tambah Sasaran", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // FAB to Add Goal
        FloatingActionButton(
            onClick = { showAddGoalDialog = true },
            containerColor = Emerald500,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .testTag("fab_add_goal")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tambah Sasaran")
        }
    }

    // Add Goal Dialog
    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { name, targetAmount, targetDateEpochMillis, category, iconName, colorHex, note, initialSaved ->
                onAddGoal(name, targetAmount, targetDateEpochMillis, category, iconName, colorHex, note, initialSaved)
                showAddGoalDialog = false
            }
        )
    }

    // Allocate Funds Dialog
    goalToAllocate?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToAllocate = null },
            title = {
                Text("Alokasikan Dana", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${goal.name} • Terkumpul ${CurrencyFormatter.formatRupiah(goal.currentSavedAmount)} / ${CurrencyFormatter.formatRupiah(goal.targetAmount)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = allocateAmountText,
                        onValueChange = { allocateAmountText = CurrencyFormatter.formatThousands(it) },
                        label = { Text("Nominal Alokasi") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().testTag("allocate_funds_input")
                    )

                    val enteredVal = allocateAmountText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                    if (enteredVal > 0) {
                        val newTotal = goal.currentSavedAmount + enteredVal
                        val newPct = if (goal.targetAmount > 0) ((newTotal / goal.targetAmount) * 100.0).coerceIn(0.0, 100.0) else 0.0
                        Surface(
                            shape = CircleShape,
                            color = Emerald500.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Progres baru: ${String.format(Locale.getDefault(), "%.1f%%", newPct)} (${CurrencyFormatter.formatRupiah(newTotal)})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald500,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = allocateAmountText.filter { it.isDigit() }.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            onAllocateFunds(goal, amount)
                            goalToAllocate = null
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier.testTag("confirm_allocate_button")
                ) {
                    Text("Alokasikan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { goalToAllocate = null },
                    shape = CircleShape
                ) { Text("Batal") }
            }
        )
    }

    // Spend Goal Funds Dialog
    goalToSpend?.let { goal ->
        SpendGoalFundsDialog(
            goal = goal,
            onDismiss = { goalToSpend = null },
            onConfirm = { amount, title, note, dateMillis ->
                onSpendGoalFunds(goal, amount, title, note, dateMillis)
                goalToSpend = null
            }
        )
    }

    // Goal Expense History Dialog
    goalForHistory?.let { goal ->
        val expenses = remember(transactions, goal.name) {
            transactions.filter {
                it.categoryName.equals("Pengeluaran Tujuan", ignoreCase = true) &&
                (it.wallet.equals("Tujuan: ${goal.name}", ignoreCase = true) ||
                 it.wallet.equals("Tabungan: ${goal.name}", ignoreCase = true) ||
                 it.wallet.contains(goal.name, ignoreCase = true))
            }
        }
        GoalExpensesHistoryDialog(
            goal = goal,
            expenses = expenses,
            onDismiss = { goalForHistory = null },
            onDeleteExpense = { tx ->
                onDeleteGoalExpense(goal, tx)
            }
        )
    }
}

@Composable
fun GoalCardItem(
    goal: FinancialGoalEntity,
    goalExpenses: List<TransactionEntity> = emptyList(),
    onAllocate: () -> Unit,
    onSpend: () -> Unit,
    onViewHistory: () -> Unit,
    onDelete: () -> Unit
) {
    val themeColor = CategoryIconHelper.parseColor(goal.colorHex)
    val icon = CategoryIconHelper.getIcon(goal.iconName)
    val isDone = goal.isCompleted
    val animFraction by animateFloatAsState(targetValue = goal.progressFraction, label = "goalProgress")
    val totalSpent = goalExpenses.sumOf { it.amount }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row: Icon badge + Title/Subtitle + Percentage Badge
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Emerald500.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Emerald500,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = goal.name.uppercase(Locale.getDefault()),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${goal.category} • Target: ${DateUtils.formatShortDate(goal.targetDateEpochMillis)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (isDone) Emerald500.copy(alpha = 0.2f) else Emerald500.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isDone) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald500, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Tercapai!", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Emerald500)
                        } else {
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", goal.progressPercent)}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald500
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sleek thin progress bar
            LinearProgressIndicator(
                progress = { animFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.5.dp)),
                color = Emerald500,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Metrics row: Terkumpul, Target, Sisa Dana
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Terkumpul", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        CurrencyFormatter.formatRupiah(goal.currentSavedAmount),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Target", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        CurrencyFormatter.formatRupiah(goal.targetAmount),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("Sisa Dana", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        CurrencyFormatter.formatCompactRupiah(goal.remainingAmount),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDone) Emerald500 else Amber500
                    )
                }
            }

            // Indicator for expenses recorded in this goal
            if (totalSpent > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Rose500.copy(alpha = 0.12f),
                    border = BorderStroke(0.7.dp, Rose500.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onViewHistory() }
                        .testTag("goal_spent_indicator_${goal.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Rose500,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Dipakai: ${CurrencyFormatter.formatRupiah(totalSpent)} (${goalExpenses.size} pengeluaran)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Rose500
                            )
                        }
                        Text(
                            text = "Detail >",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Rose500
                        )
                    }
                }
            }

            // Recommendation pill capsule
            if (!isDone && goal.remainingDays > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = Emerald500,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tabung ~${CurrencyFormatter.formatRupiah(goal.recommendedMonthlySaving)}/bln (${goal.remainingDays} hari lagi)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (goal.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Catatan: ${goal.note}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row: + Alokasikan Dana pill, - Pakai Dana pill, Riwayat icon & Delete icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // + Alokasikan Dana
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onAllocate() }
                            .testTag("goal_allocate_btn_${goal.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Savings,
                                contentDescription = null,
                                tint = Emerald500,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "+ Alokasikan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald500
                            )
                        }
                    }

                    // - Pakai Dana
                    val canSpend = goal.currentSavedAmount > 0
                    Surface(
                        shape = CircleShape,
                        color = if (canSpend) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (canSpend) Rose500.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(enabled = canSpend) { onSpend() }
                            .testTag("goal_spend_btn_${goal.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Payments,
                                contentDescription = null,
                                tint = if (canSpend) Rose500 else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "- Pakai",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (canSpend) Rose500 else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }

                    // History button if any
                    if (goalExpenses.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onViewHistory() }
                                .testTag("goal_history_btn_${goal.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    "${goalExpenses.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Hapus Sasaran",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        targetAmount: Double,
        targetDateEpochMillis: Long,
        category: String,
        iconName: String,
        colorHex: String,
        note: String,
        initialSaved: Double
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetAmountText by remember { mutableStateOf("") }
    var initialSavedText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Properti & Rumah") }
    var monthsAhead by remember { mutableStateOf(12) } // 1 year default
    var selectedIconName by remember { mutableStateOf("Home") }
    var selectedColorHex by remember { mutableStateOf("#10B981") }
    var note by remember { mutableStateOf("") }

    val categories = listOf(
        "Properti & Rumah" to "🏠 Rumah",
        "Pensiun" to "🧓 Pensiun",
        "Dana Darurat" to "🚨 Darurat",
        "Kendaraan" to "🚗 Kendaraan",
        "Liburan" to "✈️ Liburan",
        "Pendidikan" to "🎓 Pendidikan",
        "Bisnis" to "💼 Bisnis",
        "Lain-lain" to "🎯 Lain-lain"
    )

    val targetAmount = targetAmountText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
    val initialSaved = initialSavedText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
    val neededAmount = (targetAmount - initialSaved).coerceAtLeast(0.0)
    val monthlySaving = if (monthsAhead > 0) neededAmount / monthsAhead else 0.0

    val isValid = name.isNotBlank() && targetAmount > 0
    val submitGoal = {
        if (isValid) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, monthsAhead)
            onConfirm(
                name,
                targetAmount,
                cal.timeInMillis,
                selectedCategory,
                selectedIconName,
                selectedColorHex,
                note,
                initialSaved
            )
            onDismiss()
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
                shape = RoundedCornerShape(24.dp),
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                // Header: Icon + Title + Close Button
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Emerald500.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Flag,
                                contentDescription = null,
                                tint = Emerald500,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Tambah Tujuan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("close_add_goal_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Baris 1: Nama Tujuan
                OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Tujuan") },
                        placeholder = { Text("Contoh: DP Rumah, Kendaraan, Tabungan", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("goal_name_input")
                    )

                    // Baris 2: Target Dana + Dana Awal (2 kolom kompak)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = targetAmountText,
                            onValueChange = { targetAmountText = CurrencyFormatter.formatThousands(it) },
                            label = { Text("Target Dana") },
                            prefix = { Text("Rp ", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f).testTag("goal_target_amount_input")
                        )

                        OutlinedTextField(
                            value = initialSavedText,
                            onValueChange = { initialSavedText = CurrencyFormatter.formatThousands(it) },
                            label = { Text("Dana Awal (Opsional)") },
                            placeholder = { Text("Rp 0", fontSize = 12.sp) },
                            prefix = { Text("Rp ", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Baris 3: Target Waktu (Horizontal LazyRow ringkas)
                    Text(
                        text = "Tenggat Waktu",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val timeOptions = listOf(
                            1 to "1 Bln",
                            2 to "2 Bln",
                            3 to "3 Bln",
                            6 to "6 Bln",
                            9 to "9 Bln",
                            12 to "1 Thn",
                            18 to "1.5 Thn",
                            24 to "2 Thn",
                            36 to "3 Thn",
                            48 to "4 Thn",
                            60 to "5 Thn",
                            120 to "10 Thn"
                        )
                        items(timeOptions) { (months, label) ->
                            val isSelected = monthsAhead == months
                            Surface(
                                onClick = { monthsAhead = months },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Emerald500.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) Emerald500 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Emerald500 else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Baris 4: Kategori Tujuan (Horizontal LazyRow ringkas)
                    Text(
                        text = "Kategori",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { (catKey, catLabel) ->
                            val isSelected = selectedCategory == catKey
                            Surface(
                                onClick = {
                                    selectedCategory = catKey
                                    selectedIconName = when (catKey) {
                                        "Properti & Rumah" -> "Home"
                                        "Pensiun" -> "Savings"
                                        "Dana Darurat" -> "LocalHospital"
                                        "Kendaraan" -> "DirectionsCar"
                                        "Liburan" -> "Flight"
                                        "Pendidikan" -> "School"
                                        "Bisnis" -> "Work"
                                        else -> "Savings"
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Text(
                                    text = catLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Baris 5: Live Insight Perhitungan Tabungan Bulanan
                    if (targetAmount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Rekomendasi Tabung:",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "~${CurrencyFormatter.formatRupiah(monthlySaving)} / bln",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald500
                                    )
                                }
                                val durationText = if (monthsAhead >= 12) "${monthsAhead / 12} tahun" else "$monthsAhead bulan"
                                Text(
                                    text = "Estimasi tercapai dalam $durationText",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Baris 6: Warna Aksen
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Warna Aksen",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CategoryIconHelper.availableColors.take(6).forEach { (hex, color) ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { selectedColorHex = hex }
                                        .padding(2.dp)
                                ) {
                                    if (selectedColorHex == hex) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Baris 7: Catatan
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan (Opsional)") },
                        placeholder = { Text("Contoh: Tabung tiap tanggal 25", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = submitGoal,
                        enabled = isValid,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_goal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan")
                    }
                }
            }
        }
    }
}
}

@Composable
fun SpendGoalFundsDialog(
    goal: FinancialGoalEntity,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, title: String, note: String, dateMillis: Long) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var titleText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    val dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    val enteredAmount = amountText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
    val isOverBudget = enteredAmount > goal.currentSavedAmount
    val isValid = enteredAmount > 0 && !isOverBudget && titleText.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Rose500.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = Rose500,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Pakai Dana Sasaran",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = goal.name,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Info Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Saldo Tabungan Tersedia",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                CurrencyFormatter.formatRupiah(goal.currentSavedAmount),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald500
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Pengeluaran ini terisolasi khusus di dalam sasaran ini dan tidak mempengaruhi pengeluaran umum kas harian.",
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Nominal Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = CurrencyFormatter.formatThousands(it) },
                    label = { Text("Nominal Pengeluaran") },
                    prefix = { Text("Rp ") },
                    isError = isOverBudget,
                    supportingText = {
                        if (isOverBudget) {
                            Text(
                                "Nominal melebihi saldo tabungan (${CurrencyFormatter.formatRupiah(goal.currentSavedAmount)})",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 10.sp
                            )
                        } else if (enteredAmount > 0) {
                            val remaining = (goal.currentSavedAmount - enteredAmount).coerceAtLeast(0.0)
                            Text(
                                "Sisa saldo: ${CurrencyFormatter.formatRupiah(remaining)}",
                                color = Emerald500,
                                fontSize = 10.sp
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("spend_funds_amount_input")
                )

                // Quick percentage chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val p25 = goal.currentSavedAmount * 0.25
                    val p50 = goal.currentSavedAmount * 0.50
                    val p100 = goal.currentSavedAmount

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable { amountText = CurrencyFormatter.formatThousands(p25.toLong().toString()) }
                    ) {
                        Text(
                            "25%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable { amountText = CurrencyFormatter.formatThousands(p50.toLong().toString()) }
                    ) {
                        Text(
                            "50%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable { amountText = CurrencyFormatter.formatThousands(p100.toLong().toString()) }
                    ) {
                        Text(
                            "Semua",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // Keperluan / Judul
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Keperluan / Keterangan") },
                    placeholder = { Text("Contoh: Beli Tiket Pesawat, DP Rumah, dll", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("spend_funds_title_input")
                )

                // Catatan
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Catatan (Opsional)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("spend_funds_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(enteredAmount, titleText, noteText, dateMillis)
                    }
                },
                enabled = isValid,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Rose500),
                modifier = Modifier.testTag("confirm_spend_funds_button")
            ) {
                Text("Catat Pengeluaran", fontWeight = FontWeight.Bold)
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
fun GoalExpensesHistoryDialog(
    goal: FinancialGoalEntity,
    expenses: List<TransactionEntity>,
    onDismiss: () -> Unit,
    onDeleteExpense: (TransactionEntity) -> Unit
) {
    var txToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    val totalSpent = expenses.sumOf { it.amount }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Riwayat Pengeluaran",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = goal.name,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Summary Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Rose500.copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, Rose500.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Dana Terpakai", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                CurrencyFormatter.formatRupiah(totalSpent),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Rose500
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = Rose500.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${expenses.size} Catatan",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Rose500,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Pengeluaran ini khusus dari sasaran ini dan terpisah dari pengeluaran umum.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )

                if (expenses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada pengeluaran yang dicatat untuk sasaran ini.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(expenses, key = { it.id }) { tx ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = DateUtils.formatFullDate(tx.dateEpochMillis),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (tx.note.isNotBlank()) {
                                            Text(
                                                text = tx.note,
                                                fontSize = 9.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "- ${CurrencyFormatter.formatRupiah(tx.amount)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = Rose500
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { txToDelete = tx },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus Catatan Pengeluaran",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(15.dp)
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
                onClick = onDismiss,
                shape = CircleShape
            ) {
                Text("Tutup")
            }
        }
    )

    // Deletion confirmation
    txToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            title = { Text("Batalkan Pengeluaran?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Hapus catatan \"${tx.title}\"? Dana sebesar ${CurrencyFormatter.formatRupiah(tx.amount)} akan dikembalikan ke saldo tabungan sasaran ${goal.name}.",
                    fontSize = 12.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExpense(tx)
                        txToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500),
                    shape = CircleShape
                ) {
                    Text("Hapus & Kembalikan Dana")
                }
            },
            dismissButton = {
                TextButton(onClick = { txToDelete = null }, shape = CircleShape) {
                    Text("Batal")
                }
            }
        )
    }
}
