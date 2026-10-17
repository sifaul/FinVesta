package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_reminders")
data class PaymentReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String, // e.g. "Tagihan Listrik PLN", "Internet Indihome", "Cicilan KPR", "BPJS Kesehatan"
    val amount: Double,
    val dueDateEpochMillis: Long,
    val frequency: ReminderFrequency = ReminderFrequency.BULANAN,
    val status: ReminderStatus = ReminderStatus.PENDING,
    val category: String = "Tagihan",
    val autoReminderEnabled: Boolean = true,
    val reminderDaysAhead: Int = 3, // e.g. Remind 3 days before
    val paymentAccount: String = "BCA Virtual Account",
    val note: String = ""
)
