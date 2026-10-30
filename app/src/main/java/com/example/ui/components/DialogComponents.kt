@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import android.view.WindowManager
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.window.DialogWindowProvider
import com.example.data.model.InvestmentAssetEntity
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import android.widget.Toast
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AssetType
import com.example.data.model.ExpenseCategory
import com.example.data.model.IncomeCategory
import com.example.data.model.ReminderFrequency
import com.example.data.model.TransactionType
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Rose500
import com.example.util.CategoryIconHelper
import com.example.util.CurrencyFormatter
import java.util.Calendar
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.BorderStroke

/**
 * Automatically capitalizes the first letter of each sentence in text input.
 */
fun formatSentenceCapitalization(input: String): String {
    if (input.isEmpty()) return input
    val chars = input.toCharArray()
    var capitalizeNext = true
    for (i in chars.indices) {
        if (chars[i].isLetter()) {
            if (capitalizeNext) {
                chars[i] = chars[i].uppercaseChar()
                capitalizeNext = false
            }
        } else if (chars[i] in listOf('.', '!', '?', '\n')) {
            capitalizeNext = true
        }
    }
    return String(chars)
}

/**
 * Traverses view parent hierarchy to locate the host Dialog Window.
 */
fun findDialogWindow(view: android.view.View): android.view.Window? {
    var parent: Any? = view
    while (parent != null) {
        if (parent is DialogWindowProvider) {
            return parent.window
        }
        parent = (parent as? android.view.ViewParent)?.parent ?: (parent as? android.view.View)?.parent
    }
    return null
}

/**
 * Ensures the dialog window responds to keyboard resizing on Android without
 * interfering with the system WindowInsetsAnimation pipeline.
 */
@Composable
fun DialogResizeEffect() {
    val view = LocalView.current
    DisposableEffect(view) {
        val dialogWindow = findDialogWindow(view)
        dialogWindow?.let { window ->
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
        onDispose {}
    }
}

/**
 * Measures the on-screen keyboard (IME) height for Dialogs using
 * Compose's official WindowInsets.ime APIs, guaranteeing zero interference with
 * Android's InsetsAnimationControl and InteractionJankMonitor (CUJ_IME_INSETS_SHOW_ANIMATION).
 */
@Composable
fun rememberDialogKeyboardHeight(): Dp {
    return WindowInsets.ime.asPaddingValues().calculateBottomPadding()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    initialType: TransactionType = TransactionType.EXPENSE,
    initialCategory: String? = null,
    customCategories: List<com.example.data.model.CustomCategoryEntity> = emptyList(),
    goals: List<com.example.data.model.FinancialGoalEntity> = emptyList(),
    onOpenShopeeFoodDriver: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        type: TransactionType,
        title: String,
        amount: Double,
        category: String,
        wallet: String,
        note: String
    ) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var selectedType by remember { mutableStateOf(initialType) }
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember {
        mutableStateOf(
            initialCategory ?: if (selectedType == TransactionType.INCOME) IncomeCategory.GAJI.displayName
            else ExpenseCategory.MAKANAN.displayName
        )
    }
    var userSelectedCategoryManually by remember { mutableStateOf(initialCategory != null) }
    var autoSuggestedCategoryName by remember { mutableStateOf<String?>(null) }
    var isCustomCategory by remember { mutableStateOf(false) }
    var customCategoryText by remember { mutableStateOf("") }
    var wallet by remember {
        val matchingPos = if (initialCategory != null && initialType == TransactionType.EXPENSE) {
            customCategories.firstOrNull { it.isExpense && it.name.equals(initialCategory, ignoreCase = true) }
        } else null
        mutableStateOf(if (matchingPos != null) "Pos: ${matchingPos.name}" else "BCA")
    }
    var isCustomWallet by remember { mutableStateOf(false) }
    var customWalletText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val titleFocusRequester = remember { FocusRequester() }
    val customCategoryFocusRequester = remember { FocusRequester() }
    val customWalletFocusRequester = remember { FocusRequester() }
    val noteFocusRequester = remember { FocusRequester() }

    val coroutineScope = rememberCoroutineScope()
    val amountBringIntoView = remember { BringIntoViewRequester() }
    val customCatBringIntoView = remember { BringIntoViewRequester() }
    val titleBringIntoView = remember { BringIntoViewRequester() }
    val customWalletBringIntoView = remember { BringIntoViewRequester() }
    val noteBringIntoView = remember { BringIntoViewRequester() }

    val numericAmount = amountText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
    val isIncome = selectedType == TransactionType.INCOME
    val tintColor = if (isIncome) Emerald500 else Rose500

    val submitTransaction = {
        if (title.isBlank()) {
            Toast.makeText(context, "Silakan isi keterangan / judul transaksi", Toast.LENGTH_SHORT).show()
        } else if (numericAmount <= 0) {
            Toast.makeText(context, "Nominal transaksi harus lebih dari Rp 0", Toast.LENGTH_SHORT).show()
        } else {
            val resolvedCategory = if (isCustomCategory) {
                customCategoryText.trim().ifBlank { if (isIncome) "Pendapatan Lain-lain" else "Pengeluaran Lainnya" }
            } else {
                selectedCategory
            }
            val resolvedWallet = if (isCustomWallet) {
                customWalletText.trim().ifBlank { "Lainnya" }
            } else {
                wallet
            }
            onConfirm(selectedType, title.trim(), numericAmount, resolvedCategory, resolvedWallet, note.trim())
            onDismiss()
        }
    }

    val isShopeeFoodMode = isIncome &&
            (selectedCategory == IncomeCategory.SHOPEEFOOD.displayName || selectedCategory.contains("Shopee", ignoreCase = true))

    val quickAddAmounts = listOf(
        "+10rb" to 10_000L,
        "+20rb" to 20_000L,
        "+50rb" to 50_000L,
        "+100rb" to 100_000L,
        "+500rb" to 500_000L,
        "+1jt" to 1_000_000L
    )

    val expenseSuggestions = listOf("Makan Siang", "Bensin", "Kopi", "Belanja", "Tagihan", "Parkir")
    val incomeSuggestions = listOf("Gaji Pokok", "Bonus", "Freelance", "Penjualan", "Hasil Usaha")

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
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = if (isKeyboardOpen) 6.dp else 10.dp)
                    .widthIn(max = 460.dp)
                    .heightIn(max = maxCardHeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(tintColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = tintColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isShopeeFoodMode) "ShopeeFood Driver"
                            else if (isIncome) "Catat Pendapatan"
                            else "Catat Pengeluaran",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isShopeeFoodMode) "Catat order, bonus & bensin"
                            else if (isIncome) "Tambah catatan pemasukan keuangan"
                            else "Kelola pengeluaran agar tetap terkontrol",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("close_transaction_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Type Switcher Tabs (Pengeluaran vs Pendapatan)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isIncome) Rose500 else Color.Transparent)
                            .clickable {
                                selectedType = TransactionType.EXPENSE
                                selectedCategory = ExpenseCategory.MAKANAN.displayName
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (!isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Pengeluaran",
                                color = if (!isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isIncome) Emerald500 else Color.Transparent)
                            .clickable {
                                selectedType = TransactionType.INCOME
                                selectedCategory = IncomeCategory.GAJI.displayName
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Pendapatan",
                                color = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Mode Selector for Income: Umum vs ShopeeFood Driver
                if (isIncome) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = {
                                if (isShopeeFoodMode) {
                                    selectedCategory = IncomeCategory.GAJI.displayName
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (!isShopeeFoodMode) Emerald500.copy(alpha = 0.18f) else Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (!isShopeeFoodMode) Emerald500 else Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "💼 Umum",
                                fontSize = 11.5.sp,
                                fontWeight = if (!isShopeeFoodMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isShopeeFoodMode) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }

                        Surface(
                            onClick = {
                                selectedCategory = IncomeCategory.SHOPEEFOOD.displayName
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isShopeeFoodMode) Emerald500.copy(alpha = 0.18f) else Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (isShopeeFoodMode) Emerald500 else Color.Transparent
                            ),
                            modifier = Modifier.weight(1.15f)
                        ) {
                            Text(
                                text = "🛵 ShopeeFood",
                                fontSize = 11.5.sp,
                                fontWeight = if (isShopeeFoodMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (isShopeeFoodMode) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                if (isShopeeFoodMode) {
                    // Form ShopeeFood Driver
                    ShopeeFoodDriverForm(
                        onSaveTransaction = { type, tTitle, amount, cat, wlt, nte ->
                            onConfirm(type, tTitle, amount, cat, wlt, nte)
                            onDismiss()
                        }
                    )
                } else {
                    // Hero Nominal Amount Section
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = tintColor.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, tintColor.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(tintColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Nominal " + (if (isIncome) "Pendapatan" else "Pengeluaran"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tintColor
                                    )
                                }
                                if (amountText.isNotEmpty()) {
                                    Text(
                                        text = "Reset",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = tintColor,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { amountText = "" }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Clean Currency Input
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, tintColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = tintColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Rp",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = tintColor,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                 BasicTextField(
                                    value = amountText,
                                    onValueChange = { input ->
                                        amountText = CurrencyFormatter.formatThousands(input)
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = {
                                            if (isCustomCategory) {
                                                customCategoryFocusRequester.requestFocus()
                                                coroutineScope.launch { customCatBringIntoView.bringIntoView() }
                                            } else {
                                                titleFocusRequester.requestFocus()
                                                coroutineScope.launch { titleBringIntoView.bringIntoView() }
                                            }
                                        }
                                    ),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .bringIntoViewRequester(amountBringIntoView)
                                        .onFocusEvent {
                                            if (it.isFocused) {
                                                coroutineScope.launch { amountBringIntoView.bringIntoView() }
                                            }
                                        }
                                        .testTag("transaction_amount_input"),
                                    decorationBox = { innerTextField ->
                                        if (amountText.isEmpty()) {
                                            Text(
                                                text = "0",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                if (numericAmount > 0) {
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(numericAmount),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = tintColor
                                    )
                                }
                            }

                            // Single-Row Quick-Add Amount Presets
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(quickAddAmounts) { (label, value) ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(0.8.dp, tintColor.copy(alpha = 0.25f)),
                                        modifier = Modifier.clickable {
                                            val current = amountText.filter { it.isDigit() }.toLongOrNull() ?: 0L
                                            val updated = current + value
                                            amountText = CurrencyFormatter.formatThousands(updated.toString())
                                        }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = tintColor,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Category Selectbox (Dropdown)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Kategori Transaksi",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { categoryDropdownExpanded = true }
                                    .testTag("category_selectbox")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        val catIcon = CategoryIconHelper.getIcon(CategoryIconHelper.inferIconForCategory(selectedCategory))
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(tintColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = catIcon,
                                                contentDescription = null,
                                                tint = tintColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = selectedCategory,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Icon(
                                        imageVector = if (categoryDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Pilih Kategori",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = categoryDropdownExpanded,
                                onDismissRequest = { categoryDropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.86f)
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                val categoryList = if (isIncome) {
                                    val base = if (customCategories.isNotEmpty()) {
                                        customCategories.filter { !it.isExpense }.map { it.name }
                                            .ifEmpty { IncomeCategory.values().map { it.displayName } }
                                    } else {
                                        IncomeCategory.values().map { it.displayName }
                                    }
                                    base + listOf("Lainnya (Tulis Sendiri)")
                                } else {
                                    val base = if (customCategories.isNotEmpty()) {
                                        customCategories.filter { it.isExpense }.map { it.name }
                                            .ifEmpty { ExpenseCategory.values().map { it.displayName } }
                                    } else {
                                        ExpenseCategory.values().map { it.displayName }
                                    }
                                    val withTabungan = if (!base.any { it.equals("Dari Tabungan", ignoreCase = true) }) {
                                        listOf("Dari Tabungan") + base
                                    } else base
                                    withTabungan + listOf("Lainnya (Tulis Sendiri)")
                                }

                                categoryList.forEach { catName ->
                                    val isSelected = if (catName.startsWith("Lainnya")) isCustomCategory else (selectedCategory == catName && !isCustomCategory)
                                    val itemIcon = CategoryIconHelper.getIcon(CategoryIconHelper.inferIconForCategory(catName))
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                imageVector = itemIcon,
                                                contentDescription = null,
                                                tint = if (isSelected) tintColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        text = {
                                            Text(
                                                text = catName,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) tintColor else MaterialTheme.colorScheme.onSurface,
                                                fontSize = 13.sp
                                            )
                                        },
                                        trailingIcon = {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = tintColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            if (catName.startsWith("Lainnya")) {
                                                isCustomCategory = true
                                                selectedCategory = "Lainnya"
                                            } else {
                                                isCustomCategory = false
                                                selectedCategory = catName
                                            }
                                            userSelectedCategoryManually = true
                                            autoSuggestedCategoryName = null
                                            categoryDropdownExpanded = false
                                        },
                                        modifier = Modifier.testTag("category_option_$catName")
                                    )
                                }
                            }
                        }

                        // Input Mandiri jika memilih kategori 'Lainnya'
                        if (isCustomCategory) {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = customCategoryText,
                                onValueChange = { customCategoryText = it.uppercase() },
                                label = { Text(if (isIncome) "Tulis Sumber Pemasukan Lainnya" else "Tulis Kategori Pengeluaran Lainnya") },
                                placeholder = { Text(if (isIncome) "Contoh: HADIAH, JUAL BARANG, REFUND" else "Contoh: DONASI, PAJAK, RENOVASI", fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = {
                                        titleFocusRequester.requestFocus()
                                        coroutineScope.launch { titleBringIntoView.bringIntoView() }
                                    }
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = tintColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(customCategoryFocusRequester)
                                    .bringIntoViewRequester(customCatBringIntoView)
                                    .onFocusEvent {
                                        if (it.isFocused) {
                                            coroutineScope.launch { customCatBringIntoView.bringIntoView() }
                                        }
                                    }
                                    .testTag("custom_category_input")
                            )
                        }
                    }

                    // Title Input with Single-Row Quick Suggestions (Auto Kapital Semua)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Keterangan / Judul",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = { newTitle ->
                                val capitalized = newTitle.uppercase()
                                title = capitalized
                                if (!userSelectedCategoryManually) {
                                    val suggested = com.example.util.AutoCategorizer.suggestCategory(capitalized, note, !isIncome, customCategories)
                                    if (suggested != null) {
                                        selectedCategory = suggested.name
                                        autoSuggestedCategoryName = suggested.name
                                        isCustomCategory = false
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    if (isCustomWallet) {
                                        customWalletFocusRequester.requestFocus()
                                        coroutineScope.launch { customWalletBringIntoView.bringIntoView() }
                                    } else {
                                        noteFocusRequester.requestFocus()
                                        coroutineScope.launch { noteBringIntoView.bringIntoView() }
                                    }
                                }
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            placeholder = {
                                Text(
                                    if (isIncome) "Contoh: Gaji, Bonus, Freelance"
                                    else "Contoh: Makan Siang, Bensin, Belanja",
                                    fontSize = 12.sp
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = tintColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(titleFocusRequester)
                                .bringIntoViewRequester(titleBringIntoView)
                                .onFocusEvent {
                                    if (it.isFocused) {
                                        coroutineScope.launch { titleBringIntoView.bringIntoView() }
                                    }
                                }
                                .testTag("transaction_title_input")
                        )

                        // Auto-categorization indicator badge
                        if (autoSuggestedCategoryName != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = tintColor.copy(alpha = 0.12f),
                                border = BorderStroke(0.8.dp, tintColor.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = tintColor,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Kategori otomatis: $autoSuggestedCategoryName",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = tintColor
                                    )
                                }
                            }
                        }

                        // Single-Row Quick Suggestions (LazyRow - Never clutters vertical space)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val suggestions = if (isIncome) incomeSuggestions else expenseSuggestions
                            items(suggestions) { suggestion ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable {
                                        val cap = suggestion.uppercase()
                                        title = cap
                                        if (!userSelectedCategoryManually) {
                                            val suggested = com.example.util.AutoCategorizer.suggestCategory(cap, note, !isIncome, customCategories)
                                            if (suggested != null) {
                                                selectedCategory = suggested.name
                                                autoSuggestedCategoryName = suggested.name
                                                isCustomCategory = false
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        text = suggestion,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Wallet / Source of Funds (Single-Row Horizontal Scroll + Opsi Lainnya)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Sumber Dana / Akun",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val defaultWallets = listOf("BCA", "Mandiri", "BRI", "BNI", "GoPay", "ShopeePay", "OVO", "DANA", "Tunai (Cash)")
                        val posSpendingWallets = if (!isIncome) {
                            customCategories.filter { it.isExpense && it.name != "Alokasi Pos Spending" && it.name != "Alokasi Tabungan" }
                                .map { "Pos: ${it.name}" }
                        } else {
                            emptyList()
                        }
                        val savingsWallets = if (goals.isNotEmpty()) {
                            goals.map { "Tabungan: ${it.name}" }
                        } else {
                            listOf("Tabungan")
                        }
                        // Sesuai permintaan pengguna: jika melakukan pengeluaran, di auto tampil pos spending tersebut paling awal
                        val displayedWallets = if (!isIncome) {
                            posSpendingWallets + defaultWallets + savingsWallets + listOf("Lainnya")
                        } else {
                            defaultWallets + listOf("Lainnya")
                        }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(displayedWallets) { w ->
                                val isSelected = if (w == "Lainnya") isCustomWallet else (wallet == w && !isCustomWallet)
                                val isPosSpending = w.startsWith("Pos:")
                                val isSavings = w.startsWith("Tabungan")
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) {
                                        if (isPosSpending) Amber500.copy(alpha = 0.22f)
                                        else if (isSavings) Emerald500.copy(alpha = 0.2f)
                                        else tintColor.copy(alpha = 0.15f)
                                    } else {
                                        if (isPosSpending) Amber500.copy(alpha = 0.09f)
                                        else if (isSavings) Emerald500.copy(alpha = 0.08f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    },
                                    border = BorderStroke(
                                        width = if (isSelected) 1.2.dp else 0.8.dp,
                                        color = if (isSelected) {
                                            if (isPosSpending) Amber500
                                            else if (isSavings) Emerald500
                                            else tintColor
                                        } else {
                                            if (isPosSpending) Amber500.copy(alpha = 0.4f)
                                            else if (isSavings) Emerald500.copy(alpha = 0.35f)
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        }
                                    ),
                                    modifier = Modifier.clickable {
                                        if (w == "Lainnya") {
                                            isCustomWallet = true
                                            wallet = "Lainnya"
                                        } else {
                                            isCustomWallet = false
                                            wallet = w
                                            if (w.startsWith("Pos:")) {
                                                val posCatName = w.removePrefix("Pos:").trim()
                                                selectedCategory = posCatName
                                                userSelectedCategoryManually = true
                                            }
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isPosSpending) {
                                            Icon(
                                                imageVector = Icons.Default.Payments,
                                                contentDescription = null,
                                                tint = Amber500,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        } else if (isSavings) {
                                            Icon(
                                                imageVector = Icons.Default.Savings,
                                                contentDescription = null,
                                                tint = Emerald500,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        } else if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = tintColor,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = w,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) {
                                                if (isPosSpending) Amber500
                                                else if (isSavings) Emerald500
                                                else tintColor
                                            } else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Informational banner when spending from Pos Spending
                        val isFromPosSpendingActive = wallet.startsWith("Pos:")
                        if (isFromPosSpendingActive) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Amber500.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Amber500.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = Amber500,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Sumber dari Pos Spending: Saldo pos spending akan berkurang dan saldo kas utama tidak terpotong.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Informational banner when spending from savings
                        val isFromSavingsActive = wallet.startsWith("Tabungan") || selectedCategory.equals("Dari Tabungan", ignoreCase = true)
                        if (isFromSavingsActive) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Emerald500.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = Emerald500,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Sumber dari Tabungan: Saldo tabungan akan berkurang dan tidak memotong saldo kas utama.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Input Mandiri jika memilih sumber dana 'Lainnya'
                        if (isCustomWallet) {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = customWalletText,
                                onValueChange = { customWalletText = it.uppercase() },
                                label = { Text("Tulis Sumber Dana / Akun Lainnya") },
                                placeholder = { Text("Contoh: BANK JAGO, SEABANK, BSI, KAS TOKO", fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = {
                                        noteFocusRequester.requestFocus()
                                        coroutineScope.launch { noteBringIntoView.bringIntoView() }
                                    }
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = tintColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(customWalletFocusRequester)
                                    .bringIntoViewRequester(customWalletBringIntoView)
                                    .onFocusEvent {
                                        if (it.isFocused) {
                                            coroutineScope.launch { customWalletBringIntoView.bringIntoView() }
                                        }
                                    }
                                    .testTag("custom_wallet_input")
                            )
                        }
                    }

                    // Additional Note (Compact)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Catatan Tambahan (Opsional)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it.uppercase() },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            placeholder = { Text("Tulis catatan atau memo jika diperlukan...", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = tintColor,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(noteFocusRequester)
                                .bringIntoViewRequester(noteBringIntoView)
                                .onFocusEvent {
                                    if (it.isFocused) {
                                        coroutineScope.launch { noteBringIntoView.bringIntoView() }
                                    }
                                }
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Main Action Button
                    Button(
                        onClick = submitTransaction,
                        enabled = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tintColor,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("save_transaction_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Simpan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Cancel Text Button
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                    ) {
                        Text(
                            text = "Batal",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddAssetDialog(
    assetToEdit: InvestmentAssetEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        assetType: AssetType,
        units: Double,
        unitLabel: String,
        avgBuyPrice: Double,
        currentPrice: Double,
        platform: String,
        note: String
    ) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var selectedType by remember { mutableStateOf(assetToEdit?.assetType ?: AssetType.EMAS) }

    // Platform options per asset type
    val goldPlatforms = listOf("Pegadaian", "Treasury", "Pluang", "Antam", "Lainnya")
    val cryptoExchanges = listOf("Indodax", "Pintu", "Pluang", "Tokocrypto", "Binance", "Lain-lain")
    val stockBrokers = listOf("Ajaib", "Stockbit", "IPOT", "Mirae Asset", "Mandiri Sekuritas", "Lainnya")
    val mutualFundPlatforms = listOf("Bibit", "Bareksa", "Ajaib", "Pluang", "Makmur", "Lainnya")
    val otherPlatforms = listOf("Bank Mandiri", "BCA", "Fisik / SHM", "Lainnya")

    val initialPlatform = assetToEdit?.platform ?: when (selectedType) {
        AssetType.EMAS -> "Pegadaian"
        AssetType.KRIPTO -> "Indodax"
        AssetType.SAHAM -> "Ajaib"
        AssetType.REKSA_DANA -> "Bibit"
        else -> "Bank Mandiri"
    }

    var selectedPlatformChoice by remember {
        mutableStateOf(
            when (selectedType) {
                AssetType.EMAS -> if (initialPlatform in goldPlatforms) initialPlatform else "Lainnya"
                AssetType.KRIPTO -> if (initialPlatform in cryptoExchanges) initialPlatform else "Lain-lain"
                AssetType.SAHAM -> if (initialPlatform in stockBrokers) initialPlatform else "Lainnya"
                AssetType.REKSA_DANA -> if (initialPlatform in mutualFundPlatforms) initialPlatform else "Lainnya"
                else -> if (initialPlatform in otherPlatforms) initialPlatform else "Lainnya"
            }
        )
    }

    var customPlatformText by remember {
        mutableStateOf(
            if (selectedPlatformChoice == "Lainnya" || selectedPlatformChoice == "Lain-lain") initialPlatform else ""
        )
    }

    // Gold specific state: hanya jumlah emas (bisa desimal 0,004) dan nominal beli saja
    var goldGramsText by remember {
        mutableStateOf(
            if (assetToEdit != null && assetToEdit.assetType == AssetType.EMAS) {
                if (assetToEdit.units % 1.0 == 0.0) assetToEdit.units.toLong().toString()
                else assetToEdit.units.toString().replace('.', ',')
            } else ""
        )
    }
    var goldNominalText by remember {
        mutableStateOf(
            if (assetToEdit != null && assetToEdit.assetType == AssetType.EMAS) {
                CurrencyFormatter.formatThousands(assetToEdit.totalInvested.toLong().toString())
            } else ""
        )
    }

    // Standard / Crypto / Stocks / Funds state
    var name by remember {
        mutableStateOf(
            assetToEdit?.name ?: when (selectedType) {
                AssetType.EMAS -> "Emas Pegadaian"
                AssetType.KRIPTO -> "BTC"
                AssetType.SAHAM -> "BBCA"
                AssetType.REKSA_DANA -> "Pasar Uang"
                else -> ""
            }
        )
    }
    var unitsText by remember {
        mutableStateOf(
            if (assetToEdit != null && assetToEdit.assetType != AssetType.EMAS) {
                if (assetToEdit.units % 1.0 == 0.0) assetToEdit.units.toLong().toString()
                else assetToEdit.units.toString().replace('.', ',')
            } else ""
        )
    }
    var unitLabel by remember {
        mutableStateOf(
            assetToEdit?.unitLabel ?: when (selectedType) {
                AssetType.SAHAM -> "Lot"
                AssetType.KRIPTO -> "Koin"
                AssetType.EMAS -> "Gram"
                AssetType.REKSA_DANA -> "Unit"
                else -> "Unit"
            }
        )
    }
    var buyPriceText by remember {
        mutableStateOf(
            if (assetToEdit != null && assetToEdit.assetType != AssetType.EMAS) {
                CurrencyFormatter.formatThousands(assetToEdit.avgBuyPrice.toLong().toString())
            } else ""
        )
    }
    var currentPriceText by remember {
        mutableStateOf(
            if (assetToEdit != null && assetToEdit.assetType != AssetType.EMAS) {
                CurrencyFormatter.formatThousands(assetToEdit.currentPrice.toLong().toString())
            } else ""
        )
    }
    var note by remember { mutableStateOf(assetToEdit?.note ?: "") }

    val goldGramsFocusRequester = remember { FocusRequester() }
    val goldNominalFocusRequester = remember { FocusRequester() }
    val assetNameFocusRequester = remember { FocusRequester() }
    val unitLabelFocusRequester = remember { FocusRequester() }
    val unitsFocusRequester = remember { FocusRequester() }
    val buyPriceFocusRequester = remember { FocusRequester() }
    val currentPriceFocusRequester = remember { FocusRequester() }
    val assetNoteFocusRequester = remember { FocusRequester() }

    val assetCoroutineScope = rememberCoroutineScope()
    val customPlatformBringIntoView = remember { BringIntoViewRequester() }
    val goldGramsBringIntoView = remember { BringIntoViewRequester() }
    val goldNominalBringIntoView = remember { BringIntoViewRequester() }
    val goldNameBringIntoView = remember { BringIntoViewRequester() }
    val assetNameBringIntoView = remember { BringIntoViewRequester() }
    val unitLabelBringIntoView = remember { BringIntoViewRequester() }
    val unitsBringIntoView = remember { BringIntoViewRequester() }
    val buyPriceBringIntoView = remember { BringIntoViewRequester() }
    val currentPriceBringIntoView = remember { BringIntoViewRequester() }
    val assetNoteBringIntoView = remember { BringIntoViewRequester() }

    // Computations
    val resolvedPlatform = if (selectedPlatformChoice == "Lainnya" || selectedPlatformChoice == "Lain-lain") {
        customPlatformText.trim().ifBlank { if (selectedType == AssetType.KRIPTO) "Crypto Exchange" else "Platform Investasi" }
    } else {
        selectedPlatformChoice
    }

    val parsedGoldGrams = goldGramsText.replace(',', '.').trim().toDoubleOrNull() ?: 0.0
    val parsedGoldNominal = goldNominalText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0

    val generalUnits = unitsText.replace(',', '.').trim().toDoubleOrNull() ?: 0.0
    val generalBuyPrice = buyPriceText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
    val generalCurrentPrice = currentPriceText.filter { it.isDigit() }.toDoubleOrNull() ?: generalBuyPrice

    val totalInvested = if (selectedType == AssetType.EMAS) parsedGoldNominal else generalUnits * generalBuyPrice
    val currentValue = if (selectedType == AssetType.EMAS) parsedGoldNominal else generalUnits * generalCurrentPrice
    val profitLoss = currentValue - totalInvested
    val profitLossPct = if (totalInvested > 0) (profitLoss / totalInvested) * 100.0 else 0.0

    val submitAsset = {
        if (selectedType == AssetType.EMAS) {
            if (parsedGoldGrams <= 0.0) {
                Toast.makeText(context, "Jumlah emas dalam gram harus lebih dari 0", Toast.LENGTH_SHORT).show()
            } else if (parsedGoldNominal <= 0.0) {
                Toast.makeText(context, "Nominal pembelian emas harus lebih dari Rp 0", Toast.LENGTH_SHORT).show()
            } else {
                val finalName = name.trim().ifBlank { "Emas $resolvedPlatform" }
                val pricePerGram = parsedGoldNominal / parsedGoldGrams
                onConfirm(
                    finalName,
                    AssetType.EMAS,
                    parsedGoldGrams,
                    "Gram",
                    pricePerGram,
                    pricePerGram,
                    resolvedPlatform,
                    note.trim()
                )
                onDismiss()
            }
        } else {
            val finalName = name.trim()
            if (finalName.isBlank()) {
                Toast.makeText(context, "Mohon isi nama atau ticker aset investasi", Toast.LENGTH_SHORT).show()
            } else if (generalUnits <= 0.0) {
                Toast.makeText(context, "Jumlah unit harus lebih dari 0", Toast.LENGTH_SHORT).show()
            } else if (generalBuyPrice <= 0.0) {
                Toast.makeText(context, "Harga beli harus lebih dari Rp 0", Toast.LENGTH_SHORT).show()
            } else {
                onConfirm(
                    finalName,
                    selectedType,
                    generalUnits,
                    unitLabel.trim().ifBlank { "Unit" },
                    generalBuyPrice,
                    generalCurrentPrice,
                    resolvedPlatform,
                    note.trim()
                )
                onDismiss()
            }
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
                        .padding(18.dp)
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
                                .background(Amber500.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (selectedType) {
                                    AssetType.EMAS -> Icons.Default.MonetizationOn
                                    AssetType.KRIPTO -> Icons.Default.CurrencyBitcoin
                                    AssetType.SAHAM -> Icons.AutoMirrored.Filled.ShowChart
                                    AssetType.REKSA_DANA -> Icons.Default.PieChart
                                    else -> Icons.AutoMirrored.Filled.TrendingUp
                                },
                                contentDescription = null,
                                tint = Amber500,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (assetToEdit != null) "Ubah Aset Investasi" else "Tambah Aset Investasi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (selectedType) {
                                    AssetType.EMAS -> "Pilih platform & isi jumlah emas dan nominal"
                                    AssetType.KRIPTO -> "Pilih exchange kripto & masukkan koin"
                                    AssetType.SAHAM -> "Pilih sekuritas & kode saham"
                                    else -> "Isi data instrumen investasi Anda"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { submitAsset() }
                                .testTag("header_save_asset_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Simpan",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Simpan",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
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

                Spacer(modifier = Modifier.height(6.dp))

                // Form Body
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Selector Jenis Instrumen
                    Text(
                        text = "Jenis Instrumen Investasi",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(listOf(AssetType.EMAS, AssetType.KRIPTO, AssetType.SAHAM, AssetType.REKSA_DANA, AssetType.PROPERTI, AssetType.DEPOSITO_P2P, AssetType.KAS)) { type ->
                            val isSelected = selectedType == type
                            Surface(
                                onClick = {
                                    selectedType = type
                                    unitLabel = when (type) {
                                        AssetType.EMAS -> "Gram"
                                        AssetType.KRIPTO -> "Koin"
                                        AssetType.SAHAM -> "Lot"
                                        AssetType.REKSA_DANA -> "Unit"
                                        AssetType.PROPERTI -> "Unit"
                                        AssetType.DEPOSITO_P2P -> "Akun"
                                        AssetType.KAS -> "Valas"
                                    }
                                    selectedPlatformChoice = when (type) {
                                        AssetType.EMAS -> "Pegadaian"
                                        AssetType.KRIPTO -> "Indodax"
                                        AssetType.SAHAM -> "Ajaib"
                                        AssetType.REKSA_DANA -> "Bibit"
                                        else -> "Bank Mandiri"
                                    }
                                    if (name.isBlank() || name.startsWith("Emas") || name in listOf("BTC", "BBCA", "Pasar Uang")) {
                                        name = when (type) {
                                            AssetType.EMAS -> "Emas Pegadaian"
                                            AssetType.KRIPTO -> "BTC"
                                            AssetType.SAHAM -> "BBCA"
                                            AssetType.REKSA_DANA -> "Pasar Uang"
                                            else -> ""
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Amber500.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    if (isSelected) 1.2.dp else 0.8.dp,
                                    if (isSelected) Amber500 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (type) {
                                            AssetType.EMAS -> "🥇 Emas"
                                            AssetType.KRIPTO -> "🪙 Kripto"
                                            AssetType.SAHAM -> "📈 Saham"
                                            AssetType.REKSA_DANA -> "📊 Reksa Dana"
                                            AssetType.PROPERTI -> "🏢 Properti"
                                            AssetType.DEPOSITO_P2P -> "🏦 Deposito"
                                            AssetType.KAS -> "💵 Valas"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Amber500 else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // 2. Platform / Exchange Selector (Pilihan Pegadaian, Treasury, Indodax, Pintu, Pluang, dll.)
                    Text(
                        text = when (selectedType) {
                            AssetType.EMAS -> "Pilihan Platform Emas"
                            AssetType.KRIPTO -> "Pilihan Exchange Kripto"
                            AssetType.SAHAM -> "Pilihan Sekuritas Saham"
                            AssetType.REKSA_DANA -> "Pilihan Agen Penjual Reksa Dana"
                            else -> "Pilihan Platform / Bank"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val currentPlatformOptions = when (selectedType) {
                        AssetType.EMAS -> goldPlatforms
                        AssetType.KRIPTO -> cryptoExchanges
                        AssetType.SAHAM -> stockBrokers
                        AssetType.REKSA_DANA -> mutualFundPlatforms
                        else -> otherPlatforms
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(currentPlatformOptions) { option ->
                            val isSelected = selectedPlatformChoice == option
                            Surface(
                                onClick = {
                                    selectedPlatformChoice = option
                                    if (selectedType == AssetType.EMAS && (name.isBlank() || name.startsWith("Emas"))) {
                                        name = if (option != "Lainnya") "Emas $option" else "Emas Logam Mulia"
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    if (isSelected) 1.2.dp else 0.8.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Jika 'Lainnya' / 'Lain-lain' dipilih -> Masukkan Sendiri
                    if (selectedPlatformChoice == "Lainnya" || selectedPlatformChoice == "Lain-lain") {
                        OutlinedTextField(
                            value = customPlatformText,
                            onValueChange = { customPlatformText = it },
                            label = { Text(if (selectedType == AssetType.KRIPTO) "Masukkan Nama Exchange Sendiri" else "Masukkan Nama Platform Sendiri") },
                            placeholder = { Text(if (selectedType == AssetType.KRIPTO) "Contoh: OKX, Bybit, KuCoin" else "Contoh: BSI Mas Emas, Sharia Gold", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewRequester(customPlatformBringIntoView)
                                .onFocusEvent {
                                    if (it.isFocused) {
                                        assetCoroutineScope.launch { customPlatformBringIntoView.bringIntoView() }
                                    }
                                }
                        )
                    }

                    // 3. Khusus EMAS: Form hanya Jumlah Emas (bisa 0,004) dan Nominal Saja
                    if (selectedType == AssetType.EMAS) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Amber500.copy(alpha = 0.08f),
                            border = BorderStroke(0.8.dp, Amber500.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💡 Form Emas: Masukkan jumlah gram emas (contoh: 0,004 atau 1,5) dan nominal pembelian rupiah.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        // Baris 1: Jumlah Emas (Gram)
                        OutlinedTextField(
                            value = goldGramsText,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() || it == '.' || it == ',' }) {
                                    goldGramsText = input
                                }
                            },
                            label = { Text("Jumlah Emas (Gram)") },
                            placeholder = { Text("Contoh: 0,004 atau 0.5 atau 1", fontSize = 12.sp) },
                            suffix = { Text("gram", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    goldNominalFocusRequester.requestFocus()
                                    assetCoroutineScope.launch { goldNominalBringIntoView.bringIntoView() }
                                }
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(goldGramsFocusRequester)
                                .bringIntoViewRequester(goldGramsBringIntoView)
                                .onFocusEvent {
                                    if (it.isFocused) {
                                        assetCoroutineScope.launch { goldGramsBringIntoView.bringIntoView() }
                                    }
                                }
                                .testTag("gold_grams_input")
                        )

                        // Baris 2: Nominal Beli (Rp)
                        OutlinedTextField(
                            value = goldNominalText,
                            onValueChange = { goldNominalText = CurrencyFormatter.formatThousands(it) },
                            label = { Text("Nominal Pembelian (Rp)") },
                            placeholder = { Text("Contoh: 50.000 atau 1.500.000", fontSize = 12.sp) },
                            prefix = { Text("Rp ", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    assetNameFocusRequester.requestFocus()
                                    assetCoroutineScope.launch { goldNameBringIntoView.bringIntoView() }
                                }
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(goldNominalFocusRequester)
                                .bringIntoViewRequester(goldNominalBringIntoView)
                                .onFocusEvent {
                                    if (it.isFocused) {
                                        assetCoroutineScope.launch { goldNominalBringIntoView.bringIntoView() }
                                    }
                                }
                                .testTag("gold_nominal_input")
                        )

                        // Nama Aset Emas (bisa otomatis terisi)
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Keterangan Aset Emas (Opsional)") },
                            placeholder = { Text("Contoh: Emas ${resolvedPlatform}", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    assetNoteFocusRequester.requestFocus()
                                    assetCoroutineScope.launch { assetNoteBringIntoView.bringIntoView() }
                                }
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(assetNameFocusRequester)
                                .bringIntoViewRequester(goldNameBringIntoView)
                                .onFocusEvent {
                                    if (it.isFocused) {
                                        assetCoroutineScope.launch { goldNameBringIntoView.bringIntoView() }
                                    }
                                }
                        )
                    } else {
                        // 4. Form Non-Emas (Kripto, Saham, Reksa Dana, dll.)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it.uppercase() },
                                label = {
                                    Text(
                                        when (selectedType) {
                                            AssetType.KRIPTO -> "Nama / Koin Crypto"
                                            AssetType.SAHAM -> "Kode Saham"
                                            AssetType.REKSA_DANA -> "Nama Reksa Dana"
                                            else -> "Nama Aset"
                                        }
                                    )
                                },
                                placeholder = {
                                    Text(
                                        when (selectedType) {
                                            AssetType.KRIPTO -> "BTC, ETH, SOL"
                                            AssetType.SAHAM -> "BBCA, BBRI"
                                            AssetType.REKSA_DANA -> "Sucorinvest Pasar Uang"
                                            else -> "Nama Aset"
                                        },
                                        fontSize = 12.sp
                                    )
                                },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(
                                    onNext = {
                                        unitLabelFocusRequester.requestFocus()
                                        assetCoroutineScope.launch { unitLabelBringIntoView.bringIntoView() }
                                    }
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .focusRequester(assetNameFocusRequester)
                                    .bringIntoViewRequester(assetNameBringIntoView)
                                    .onFocusEvent {
                                        if (it.isFocused) {
                                            assetCoroutineScope.launch { assetNameBringIntoView.bringIntoView() }
                                        }
                                    }
                                    .testTag("asset_name_input")
                            )

                            OutlinedTextField(
                                value = unitLabel,
                                onValueChange = { unitLabel = it },
                                label = { Text("Satuan") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(
                                    onNext = {
                                        unitsFocusRequester.requestFocus()
                                        assetCoroutineScope.launch { unitsBringIntoView.bringIntoView() }
                                    }
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .focusRequester(unitLabelFocusRequester)
                                    .bringIntoViewRequester(unitLabelBringIntoView)
                                    .onFocusEvent {
                                        if (it.isFocused) {
                                            assetCoroutineScope.launch { unitLabelBringIntoView.bringIntoView() }
                                        }
                                    }
                            )
                        }

                        // Jumlah Unit
                        OutlinedTextField(
                            value = unitsText,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() || it == '.' || it == ',' }) {
                                    unitsText = input
                                }
                            },
                            label = {
                                Text(
                                    when (selectedType) {
                                        AssetType.KRIPTO -> "Jumlah Koin"
                                        AssetType.SAHAM -> "Jumlah Lot"
                                        else -> "Jumlah Unit"
                                    }
                                )
                            },
                            placeholder = { Text(if (selectedType == AssetType.KRIPTO) "0.005 atau 1" else "1", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    buyPriceFocusRequester.requestFocus()
                                    assetCoroutineScope.launch { buyPriceBringIntoView.bringIntoView() }
                                }
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(unitsFocusRequester)
                                .bringIntoViewRequester(unitsBringIntoView)
                                .onFocusEvent {
                                    if (it.isFocused) {
                                        assetCoroutineScope.launch { unitsBringIntoView.bringIntoView() }
                                    }
                                }
                                .testTag("asset_units_input")
                        )

                        // Harga Beli & Harga Pasar Sekarang
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = buyPriceText,
                                onValueChange = { buyPriceText = CurrencyFormatter.formatThousands(it) },
                                label = { Text("Harga Beli (Rp)") },
                                prefix = { Text("Rp ", fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(
                                    onNext = {
                                        currentPriceFocusRequester.requestFocus()
                                        assetCoroutineScope.launch { currentPriceBringIntoView.bringIntoView() }
                                    }
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(buyPriceFocusRequester)
                                    .bringIntoViewRequester(buyPriceBringIntoView)
                                    .onFocusEvent {
                                        if (it.isFocused) {
                                            assetCoroutineScope.launch { buyPriceBringIntoView.bringIntoView() }
                                        }
                                    }
                            )

                            OutlinedTextField(
                                value = currentPriceText,
                                onValueChange = { currentPriceText = CurrencyFormatter.formatThousands(it) },
                                label = { Text("Harga Sekarang") },
                                placeholder = { Text("= Beli", fontSize = 12.sp) },
                                prefix = { Text("Rp ", fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(
                                    onNext = {
                                        assetNoteFocusRequester.requestFocus()
                                        assetCoroutineScope.launch { assetNoteBringIntoView.bringIntoView() }
                                    }
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(currentPriceFocusRequester)
                                    .bringIntoViewRequester(currentPriceBringIntoView)
                                    .onFocusEvent {
                                        if (it.isFocused) {
                                            assetCoroutineScope.launch { currentPriceBringIntoView.bringIntoView() }
                                        }
                                    }
                            )
                        }
                    }

                    // Live Calculation Mini-Card
                    if (totalInvested > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Total Modal",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(totalInvested),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Nilai Pasar",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(currentValue),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Catatan Strategi
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan (Opsional)") },
                        placeholder = { Text("Contoh: Beli rutin bulanan / target jangka panjang", fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(assetNoteFocusRequester)
                            .bringIntoViewRequester(assetNoteBringIntoView)
                            .onFocusEvent {
                                if (it.isFocused) {
                                    assetCoroutineScope.launch { assetNoteBringIntoView.bringIntoView() }
                                }
                            }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer Buttons - Tombol bertuliskan hanya "Simpan"
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
                        onClick = submitAsset,
                        enabled = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_asset_button")
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
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        amount: Double,
        dueDate: Long,
        frequency: ReminderFrequency,
        paymentAccount: String,
        note: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var daysAhead by remember { mutableStateOf(3) }
    var selectedFrequency by remember { mutableStateOf(ReminderFrequency.BULANAN) }
    var paymentAccount by remember { mutableStateOf("BCA Virtual Account") }
    var note by remember { mutableStateOf("") }

    val numericAmount = amountText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0

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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Tambah Pengingat Tagihan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nama Tagihan / Pembayaran") },
                        placeholder = { Text("Listrik PLN, WiFi, Cicilan, BPJS") },
                        singleLine = true,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().testTag("reminder_title_input")
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = CurrencyFormatter.formatThousands(it) },
                        label = { Text("Nominal Pembayaran (Rp)") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Jatuh Tempo:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1 to "Besok", 3 to "3 Hari lagi", 7 to "1 Minggu", 14 to "2 Minggu").forEach { (days, label) ->
                            FilterChip(
                                selected = daysAhead == days,
                                onClick = { daysAhead = days },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Text("Frekuensi:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ReminderFrequency.values().forEach { freq ->
                            FilterChip(
                                selected = selectedFrequency == freq,
                                onClick = { selectedFrequency = freq },
                                label = { Text(freq.displayName, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = paymentAccount,
                        onValueChange = { paymentAccount = it },
                        label = { Text("Metode / Rekening Bayar") },
                        placeholder = { Text("BCA VA, Autodebet, Tokopedia") },
                        singleLine = true,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan / Nomor Pelanggan") },
                        singleLine = true,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth()
                    )

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
                                if (title.isNotBlank() && numericAmount > 0) {
                                    val cal = Calendar.getInstance()
                                    cal.add(Calendar.DAY_OF_MONTH, daysAhead)
                                    onConfirm(title, numericAmount, cal.timeInMillis, selectedFrequency, paymentAccount, note)
                                    onDismiss()
                                }
                            },
                            enabled = title.isNotBlank() && numericAmount > 0,
                            shape = CircleShape,
                            modifier = Modifier.testTag("save_reminder_button")
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
fun ExportReportDialog(
    monthName: String,
    onDismiss: () -> Unit,
    onExport: (isPdf: Boolean) -> Unit
) {
    var isPdfSelected by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FileDownload, contentDescription = null, tint = Emerald500)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ekspor Laporan Keuangan", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Ekspor ringkasan pendapatan, pengeluaran, dan portofolio investasi periode $monthName secara otomatis.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = CircleShape,
                    color = if (isPdfSelected) Emerald500.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPdfSelected = true }
                        .padding(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isPdfSelected, onClick = { isPdfSelected = true })
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Description, contentDescription = null, tint = Emerald500)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Format Dokumen PDF", fontWeight = FontWeight.Bold)
                            Text("Laporan rapi, resmi, siap cetak & bagikan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (!isPdfSelected) Emerald500.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPdfSelected = false }
                        .padding(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = !isPdfSelected, onClick = { isPdfSelected = false })
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.TableChart, contentDescription = null, tint = Amber500)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Format Excel / Spreadsheet (CSV)", fontWeight = FontWeight.Bold)
                            Text("Kompatibel Microsoft Excel & Google Sheets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                var isCustomScope by remember { mutableStateOf(false) }
                var customReportNote by remember { mutableStateOf("") }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isCustomScope = !isCustomScope }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isCustomScope,
                        onCheckedChange = { isCustomScope = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Keterangan / Catatan Laporan Lainnya",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (isCustomScope) {
                    OutlinedTextField(
                        value = customReportNote,
                        onValueChange = { customReportNote = it.uppercase() },
                        label = { Text("Tulis Catatan / Judul Laporan Lainnya") },
                        placeholder = { Text("Contoh: LAPORAN KAS USAHA, AUDIT KELUARGA", fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("custom_report_note_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onExport(isPdfSelected)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                shape = CircleShape,
                modifier = Modifier.testTag("export_confirm_button")
            ) {
                Text("Ekspor Sekarang", color = MaterialTheme.colorScheme.surface)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = CircleShape
            ) { Text("Batal") }
        }
    )
}
