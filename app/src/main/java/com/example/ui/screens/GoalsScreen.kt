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
    modifier: Modifier = Modifier
) {
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalToAllocate by remember { mutableStateOf<FinancialGoalEntity?>(null) }
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tujuan Keuangan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
                    Text(
                        text = "Daftar Sasaran",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(goals, key = { it.id }) { goal ->
                    GoalCardItem(
                        goal = goal,
                        onAllocate = {
                            goalToAllocate = goal
                            allocateAmountText = ""
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
}

@Composable
fun GoalCardItem(
    goal: FinancialGoalEntity,
    onAllocate: () -> Unit,
    onDelete: () -> Unit
) {
    val themeColor = CategoryIconHelper.parseColor(goal.colorHex)
    val icon = CategoryIconHelper.getIcon(goal.iconName)
    val isDone = goal.isCompleted
    val animFraction by animateFloatAsState(targetValue = goal.progressFraction, label = "goalProgress")

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

            Spacer(modifier = Modifier.height(8.dp))

            // Action row: + Alokasikan Dana pill & Delete icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onAllocate() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Savings,
                            contentDescription = null,
                            tint = Emerald500,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            "+ Alokasikan Dana",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald500
                        )
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isValid) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { submitGoal() }
                                    .testTag("header_save_goal_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
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
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Tutup",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                }

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
