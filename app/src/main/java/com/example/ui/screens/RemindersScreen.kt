package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.model.PaymentReminderEntity
import com.example.data.model.ReminderStatus
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Rose500
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@Composable
fun RemindersScreen(
    reminders: List<PaymentReminderEntity>,
    onAddReminder: () -> Unit,
    onToggleStatus: (PaymentReminderEntity) -> Unit,
    onDeleteReminder: (PaymentReminderEntity) -> Unit,
    onSendNotification: (Context, PaymentReminderEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pendingReminders = reminders.filter { it.status == ReminderStatus.PENDING }
    val paidReminders = reminders.filter { it.status == ReminderStatus.PAID }
    val totalPendingAmount = pendingReminders.sumOf { it.amount }

    Box(modifier = modifier.fillMaxSize().imePadding()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pengingat Pembayaran & Tagihan",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Notifikasi pengingat otomatis agar tidak terkena denda keterlambatan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Compact Summary Bar
            item {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Amber500.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Amber500,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Total Tagihan Menanti",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatRupiah(totalPendingAmount),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (pendingReminders.isNotEmpty()) Amber500.copy(alpha = 0.15f) else Emerald500.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (pendingReminders.isNotEmpty()) "${pendingReminders.size} Tertunda" else "Lunas",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pendingReminders.isNotEmpty()) Amber500 else Emerald500,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Pending Bills Section
            item {
                Text(
                    text = "Menunggu Pembayaran (${pendingReminders.size})",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (pendingReminders.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Semua tagihan lunas! Tidak ada tagihan tertunda.",
                                color = Emerald500,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                items(pendingReminders, key = { it.id }) { reminder ->
                    ReminderItemCard(
                        reminder = reminder,
                        onToggle = { onToggleStatus(reminder) },
                        onDelete = { onDeleteReminder(reminder) },
                        onNotify = { onSendNotification(context, reminder) }
                    )
                }
            }

            // Paid Bills Section
            if (paidReminders.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sudah Dibayar Bulan Ini (${paidReminders.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(paidReminders, key = { it.id }) { reminder ->
                    ReminderItemCard(
                        reminder = reminder,
                        onToggle = { onToggleStatus(reminder) },
                        onDelete = { onDeleteReminder(reminder) },
                        onNotify = { onSendNotification(context, reminder) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddReminder,
            containerColor = Amber500,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .testTag("fab_add_reminder")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tambah Pengingat")
        }
    }
}

@Composable
fun ReminderItemCard(
    reminder: PaymentReminderEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onNotify: () -> Unit
) {
    val isPaid = reminder.status == ReminderStatus.PAID
    val dueText = DateUtils.getRelativeDueText(reminder.dueDateEpochMillis)
    val isUrgent = !isPaid && (dueText.contains("HARI INI") || dueText.contains("Besok") || dueText.contains("Terlambat"))

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Sleek Circular Checkbox (ultra-compact & aligned with top line)
            Box(
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { onToggle() }
                    .then(
                        if (isPaid) {
                            Modifier.background(Emerald500)
                        } else {
                            Modifier.border(
                                BorderStroke(
                                    1.5.dp,
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                ),
                                CircleShape
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isPaid) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Lunas",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Main Content: Seamless ultra-compact flow between Lines 1, 2, 3
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Line 1: Title & Amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = reminder.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        lineHeight = 15.sp,
                        color = if (isPaid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = CurrencyFormatter.formatRupiah(reminder.amount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        lineHeight = 15.sp,
                        color = if (isPaid) Emerald500 else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Line 2: Category/Account & Due Status Pill (zero vertical gap)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${reminder.category} • ${reminder.paymentAccount}",
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = CircleShape,
                        color = when {
                            isPaid -> Emerald500.copy(alpha = 0.15f)
                            isUrgent -> Rose500.copy(alpha = 0.15f)
                            else -> Amber500.copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            text = if (isPaid) "Lunas" else dueText,
                            fontSize = 9.sp,
                            lineHeight = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isPaid -> Emerald500
                                isUrgent -> Rose500
                                else -> Amber500
                            },
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 0.5.dp)
                        )
                    }
                }

                // Line 3: Note / Frequency & Quick Action Buttons (Notify + Delete)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (reminder.note.isNotBlank()) "Catatan: ${reminder.note}" else reminder.frequency.displayName,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Small circular notify button
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { onNotify() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifikasi",
                            tint = Amber500,
                            modifier = Modifier.size(10.dp)
                        )
                    }

                    // Small delete button
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .clickable { onDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Hapus",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}
}
