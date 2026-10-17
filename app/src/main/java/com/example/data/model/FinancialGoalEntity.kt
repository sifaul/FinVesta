package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "financial_goals")
data class FinancialGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String, // e.g. "Uang Muka Rumah", "Pensiun", "Dana Darurat", "Liburan"
    val targetAmount: Double,
    val currentSavedAmount: Double = 0.0,
    val targetDateEpochMillis: Long,
    val category: String = "Umum", // Properti, Pensiun, Darurat, Kendaraan, Liburan, Pendidikan
    val iconName: String = "Home",
    val colorHex: String = "#10B981",
    val note: String = "",
    val createdAtEpochMillis: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = if (targetAmount > 0) ((currentSavedAmount / targetAmount).toFloat()).coerceIn(0f, 1f) else 0f

    val progressPercent: Double
        get() = if (targetAmount > 0) ((currentSavedAmount / targetAmount) * 100.0).coerceIn(0.0, 100.0) else 0.0

    val remainingAmount: Double
        get() = (targetAmount - currentSavedAmount).coerceAtLeast(0.0)

    val isCompleted: Boolean
        get() = currentSavedAmount >= targetAmount

    val remainingDays: Long
        get() {
            val diff = targetDateEpochMillis - System.currentTimeMillis()
            return if (diff > 0) diff / (1000L * 60 * 60 * 24) else 0L
        }

    val recommendedMonthlySaving: Double
        get() {
            val months = (remainingDays / 30.0).coerceAtLeast(1.0)
            return remainingAmount / months
        }
}
