package com.example.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing financial transactions, storing both income and daily expenses
 * in the encrypted SQLCipher database.
 * Includes fields for amount, category, date, and transaction type (income/expense).
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["dateEpochMillis"]),
        Index(value = ["type"]),
        Index(value = ["categoryName"]),
        Index(value = ["type", "dateEpochMillis"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: TransactionType,
    val title: String,
    val amount: Double,
    val categoryName: String, // from IncomeCategory or ExpenseCategory
    val dateEpochMillis: Long = System.currentTimeMillis(),
    val wallet: String = "BCA", // e.g. BCA, GoPay, ShopeePay, Cash
    val note: String = "",
    val isEncrypted: Boolean = true,
    val syncStatus: String = "SYNCED",
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Secondary constructor matching standard daily transaction fields:
     * amount, category, date, and transaction type (income/expense).
     */
    @Ignore
    constructor(
        amount: Double,
        category: String,
        date: Long,
        type: TransactionType,
        title: String = category,
        wallet: String = "BCA",
        note: String = ""
    ) : this(
        id = 0L,
        type = type,
        title = title,
        amount = amount,
        categoryName = category,
        dateEpochMillis = date,
        wallet = wallet,
        note = note
    )

    /**
     * Alias for categoryName to easily track category.
     */
    @get:Ignore
    val category: String
        get() = categoryName

    /**
     * Alias for dateEpochMillis to easily track transaction date.
     */
    @get:Ignore
    val date: Long
        get() = dateEpochMillis

    /**
     * Alias for type to easily track transactionType (income/expense).
     */
    @get:Ignore
    val transactionType: TransactionType
        get() = type
}

/**
 * Typealiases representing 'Transaction' and 'DailyTransaction' Room Entity.
 */
typealias Transaction = TransactionEntity
typealias DailyTransaction = TransactionEntity
