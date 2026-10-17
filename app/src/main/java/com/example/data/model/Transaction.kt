package com.example.data.model

/**
 * Convenience reference and factory for Room Entity 'Transaction' (tracking type, amount, category, date).
 * Maps directly to [TransactionEntity].
 */
fun createTransaction(
    type: TransactionType,
    amount: Double,
    category: String,
    date: Long = System.currentTimeMillis(),
    title: String = category,
    wallet: String = "BCA",
    note: String = "",
    isEncrypted: Boolean = true
): Transaction {
    return Transaction(
        type = type,
        title = title,
        amount = amount,
        categoryName = category,
        dateEpochMillis = date,
        wallet = wallet,
        note = note,
        isEncrypted = isEncrypted
    )
}
