@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.IncomeCategory
import com.example.data.model.TransactionType
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Rose500
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val ShopeeOrange = Color(0xFFEE4D2D)
val ShopeeOrangeDark = Color(0xFFE64A19)
val ShopeePeachBorder = Color(0xFFFFCCBC)
val ShopeeButtonBlue = Color(0xFF3B82F6)

/**
 * Modern, clean ShopeeFood Driver Form component.
 * Exact inputs matching daily driver ledger:
 * ORD | KOTOR | BONUS | TIP | BENSIN | PARKIR | AMAL | INVEST | MOTOR
 *
 * - Tanggal otomatis hari ini (auto masuk, tidak ditampilkan di form)
 * - Harmonized uniform 48dp input heights for pixel-perfect mobile alignment
 * - Bold, clear uppercase field headers
 * - Real-time net income calculation card
 */
@Composable
fun ShopeeFoodDriverForm(
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    onSaveTransaction: (
        type: TransactionType,
        title: String,
        amount: Double,
        category: String,
        wallet: String,
        note: String
    ) -> Unit
) {
    val focusManager = LocalFocusManager.current
    // Tanggal otomatis masuk hari ini (tidak ditampilkan di UI form)
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val tglText = remember { dateFormat.format(Date()) }

    var ordText by remember { mutableStateOf("") }
    var kotorText by remember { mutableStateOf("") }
    var bonusText by remember { mutableStateOf("") }
    var tipText by remember { mutableStateOf("") }

    var bensinText by remember { mutableStateOf("") }
    var parkirText by remember { mutableStateOf("") }
    var amalText by remember { mutableStateOf("") }
    var investText by remember { mutableStateOf("") }
    var motorText by remember { mutableStateOf("") }

    val kotorNum = kotorText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
    val bonusNum = bonusText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
    val tipNum = tipText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0

    val bensinNum = bensinText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
    val parkirNum = parkirText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
    val amalNum = amalText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
    val investNum = investText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
    val motorNum = motorText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0

    val totalIncome = kotorNum + bonusNum + tipNum
    val totalExpense = bensinNum + parkirNum + amalNum + investNum + motorNum
    val bersih = totalIncome - totalExpense

    val isSavable = kotorNum > 0 || bersih > 0 || ordText.isNotBlank() || totalIncome > 0

    val handleSave = {
        val ordLabel = if (ordText.isNotBlank()) "$ordText ORD" else "Harian"
        val titleStr = "ShopeeFood ($ordLabel)"
        val finalAmount = if (bersih > 0) bersih else if (totalIncome > 0) totalIncome else 0.0
        val noteParts = mutableListOf<String>()
        noteParts.add("TGL: $tglText")
        if (ordText.isNotBlank()) noteParts.add("ORD: $ordText")
        if (kotorNum > 0) noteParts.add("KOTOR: ${CurrencyFormatter.formatCompactRupiah(kotorNum)}")
        if (bonusNum > 0) noteParts.add("BONUS: ${CurrencyFormatter.formatCompactRupiah(bonusNum)}")
        if (tipNum > 0) noteParts.add("TIP: ${CurrencyFormatter.formatCompactRupiah(tipNum)}")
        if (bensinNum > 0) noteParts.add("BENSIN: ${CurrencyFormatter.formatCompactRupiah(bensinNum)}")
        if (parkirNum > 0) noteParts.add("PARKIR: ${CurrencyFormatter.formatCompactRupiah(parkirNum)}")
        if (amalNum > 0) noteParts.add("AMAL: ${CurrencyFormatter.formatCompactRupiah(amalNum)}")
        if (investNum > 0) noteParts.add("INVEST: ${CurrencyFormatter.formatCompactRupiah(investNum)}")
        if (motorNum > 0) noteParts.add("MOTOR: ${CurrencyFormatter.formatCompactRupiah(motorNum)}")
        noteParts.add("BERSIH: ${CurrencyFormatter.formatCompactRupiah(bersih)}")
        val noteStr = noteParts.joinToString(" | ")

        onSaveTransaction(
            TransactionType.INCOME,
            titleStr,
            finalAmount,
            IncomeCategory.SHOPEEFOOD.displayName,
            "ShopeePay",
            noteStr
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("shopee_food_driver_card_root"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Outer Card Container
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Badge only shown if standalone (onClose != null)
                if (onClose != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeliveryDining,
                                contentDescription = null,
                                tint = ShopeeOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ShopeeFood Driver",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ShopeeOrange
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSavable) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ShopeeOrange,
                                    modifier = Modifier
                                        .clickable { handleSave() }
                                        .testTag("header_save_shopee_button")
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
                                onClick = onClose,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Row 1: ORD & KOTOR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShopeeCompactField(
                        value = ordText,
                        onValueChange = { ordText = CurrencyFormatter.formatThousands(it) },
                        label = "ORD",
                        placeholder = "0",
                        testTag = "shopee_input_ord",
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                        modifier = Modifier.weight(1f)
                    )

                    ShopeeCompactField(
                        value = kotorText,
                        onValueChange = { kotorText = CurrencyFormatter.formatThousands(it) },
                        label = "KOTOR",
                        prefix = "Rp ",
                        placeholder = "0",
                        testTag = "shopee_input_kotor",
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: BONUS & TIP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShopeeCompactField(
                        value = bonusText,
                        onValueChange = { bonusText = CurrencyFormatter.formatThousands(it) },
                        label = "BONUS",
                        prefix = "Rp ",
                        placeholder = "0",
                        testTag = "shopee_input_bonus",
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                        modifier = Modifier.weight(1f)
                    )

                    ShopeeCompactField(
                        value = tipText,
                        onValueChange = { tipText = CurrencyFormatter.formatThousands(it) },
                        label = "TIP",
                        prefix = "Rp ",
                        placeholder = "0",
                        testTag = "shopee_input_tip",
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3: BENSIN & PARKIR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShopeeCompactField(
                        value = bensinText,
                        onValueChange = { bensinText = CurrencyFormatter.formatThousands(it) },
                        label = "BENSIN",
                        prefix = "Rp ",
                        placeholder = "0",
                        testTag = "shopee_input_bensin",
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                        modifier = Modifier.weight(1f)
                    )

                    ShopeeCompactField(
                        value = parkirText,
                        onValueChange = { parkirText = CurrencyFormatter.formatThousands(it) },
                        label = "PARKIR",
                        prefix = "Rp ",
                        placeholder = "0",
                        testTag = "shopee_input_parkir",
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 4: AMAL & MOTOR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShopeeCompactField(
                        value = amalText,
                        onValueChange = { amalText = CurrencyFormatter.formatThousands(it) },
                        label = "AMAL",
                        prefix = "Rp ",
                        placeholder = "0",
                        testTag = "shopee_input_amal",
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                        modifier = Modifier.weight(1f)
                    )

                    ShopeeCompactField(
                        value = motorText,
                        onValueChange = { motorText = CurrencyFormatter.formatThousands(it) },
                        label = "MOTOR",
                        prefix = "Rp ",
                        placeholder = "0",
                        testTag = "shopee_input_motor",
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 5: INVEST
                ShopeeCompactField(
                    value = investText,
                    onValueChange = { investText = CurrencyFormatter.formatThousands(it) },
                    label = "INVEST",
                    prefix = "Rp ",
                    placeholder = "0",
                    testTag = "shopee_input_invest",
                    imeAction = ImeAction.Done,
                    onImeAction = { focusManager.clearFocus() },
                    modifier = Modifier.fillMaxWidth()
                )

                // Live Summary Card (Bersih, Rapi & Elegan)
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ShopeeOrange.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .testTag("shopee_bersih_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Kotor: ${CurrencyFormatter.formatRupiah(totalIncome)}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Emerald500
                            )
                            if (totalExpense > 0) {
                                Text(
                                    text = "Keluar: -${CurrencyFormatter.formatRupiah(totalExpense)}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Rose500
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "BERSIH",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (totalIncome == 0.0 && totalExpense == 0.0) "Rp 0"
                                else CurrencyFormatter.formatRupiah(bersih),
                                fontWeight = FontWeight.ExtraBold,
                                color = if (bersih >= 0) ShopeeOrange else Rose500,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons: Batal & Simpan
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onClose != null) {
                OutlinedButton(
                    onClick = onClose,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text("Batal")
                }
            }

            Button(
                onClick = handleSave,
                enabled = isSavable,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ShopeeOrange,
                    disabledContainerColor = ShopeeOrange.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .weight(if (onClose != null) 1.4f else 1f)
                    .height(44.dp)
                    .testTag("shopee_simpan_transaksi_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simpan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

/**
 * Compact, unified textfield with consistent label placement above the box
 * and harmonized 48dp height for pixel-perfect alignment on any mobile screen.
 */
@Composable
private fun ShopeeCompactField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    prefix: String? = null,
    isNumeric: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    testTag: String = "",
    modifier: Modifier = Modifier
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = if (placeholder.isNotBlank()) {
                {
                    Text(
                        text = placeholder,
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        maxLines = 1
                    )
                }
            } else null,
            prefix = if (prefix != null) {
                {
                    Text(
                        text = prefix,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
            } else null,
            singleLine = true,
            keyboardOptions = if (isNumeric) {
                KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction)
            } else {
                KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = imeAction)
            },
            keyboardActions = KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() }
            ),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ShopeeOrange,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = ShopeeOrange
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusEvent {
                    if (it.isFocused) {
                        coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                    }
                }
                .testTag(testTag)
        )
    }
}

/**
 * Dedicated Standalone Dialog for Shopee Food Driver.
 * Renders the clean UI in a modal dialog adapting to light/dark mode.
 */
@Composable
fun ShopeeFoodDriverDialog(
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
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = if (isKeyboardOpen) 6.dp else 10.dp)
                    .widthIn(max = 480.dp)
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
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    ShopeeFoodDriverForm(
                        onClose = onDismiss,
                        onSaveTransaction = { type, title, amount, category, wallet, note ->
                            onConfirm(type, title, amount, category, wallet, note)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}
