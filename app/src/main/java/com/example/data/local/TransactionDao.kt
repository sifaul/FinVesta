package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing financial transactions (incomes and expenses).
 * Provides reactive queries via Flow for Compose UI observation and suspend methods
 * for asynchronous database operations.
 */
@Dao
interface TransactionDao {

    /**
     * Retrieve all transactions sorted by date descending (newest first).
     */
    @Query("SELECT * FROM transactions ORDER BY dateEpochMillis DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    /**
     * Query transactions filtered by type (INCOME or EXPENSE).
     */
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY dateEpochMillis DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>>

    /**
     * Query the latest N transactions for dashboard display.
     */
    @Query("SELECT * FROM transactions ORDER BY dateEpochMillis DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    /**
     * Query transactions within a specified date range.
     */
    @Query("SELECT * FROM transactions WHERE dateEpochMillis BETWEEN :startDate AND :endDate ORDER BY dateEpochMillis DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    /**
     * Search transactions matching a title or category name keyword.
     */
    @Query("SELECT * FROM transactions WHERE title LIKE '%' || :query || '%' OR categoryName LIKE '%' || :query || '%' ORDER BY dateEpochMillis DESC")
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    /**
     * Get a single transaction by its primary key ID.
     */
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    /**
     * Aggregate total income across all transactions.
     */
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME'")
    fun getTotalIncome(): Flow<Double?>

    /**
     * Aggregate total expense across all transactions.
     */
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE'")
    fun getTotalExpense(): Flow<Double?>

    /**
     * Retrieve all daily expense transactions ordered by date descending.
     */
    @Query("SELECT * FROM transactions WHERE type = 'EXPENSE' ORDER BY dateEpochMillis DESC")
    fun getAllDailyExpenses(): Flow<List<TransactionEntity>>

    /**
     * Retrieve all income transactions ordered by date descending.
     */
    @Query("SELECT * FROM transactions WHERE type = 'INCOME' ORDER BY dateEpochMillis DESC")
    fun getAllIncomes(): Flow<List<TransactionEntity>>

    /**
     * Query daily expenses within a date range (e.g. today, this week, this month).
     */
    @Query("SELECT * FROM transactions WHERE type = 'EXPENSE' AND dateEpochMillis BETWEEN :startDate AND :endDate ORDER BY dateEpochMillis DESC")
    fun getDailyExpensesForRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    /**
     * Calculate total daily expense amount within a date range.
     */
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND dateEpochMillis BETWEEN :startDate AND :endDate")
    fun getDailyExpenseTotalForRange(startDate: Long, endDate: Long): Flow<Double?>

    /**
     * Query income transactions within a date range.
     */
    @Query("SELECT * FROM transactions WHERE type = 'INCOME' AND dateEpochMillis BETWEEN :startDate AND :endDate ORDER BY dateEpochMillis DESC")
    fun getIncomesForRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    /**
     * Calculate total income amount within a date range.
     */
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND dateEpochMillis BETWEEN :startDate AND :endDate")
    fun getIncomeTotalForRange(startDate: Long, endDate: Long): Flow<Double?>

    /**
     * Insert a single financial transaction.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    /**
     * Batch insert financial transactions.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    /**
     * Update an existing financial transaction.
     */
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    /**
     * Delete a financial transaction.
     */
    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    /**
     * Delete a financial transaction by primary key ID.
     */
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    /**
     * Clear all financial transactions.
     */
    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    /**
     * Delete all internal goal savings transactions (allocations and refunds).
     */
    @Query("DELETE FROM transactions WHERE categoryName IN ('Alokasi Tabungan', 'Pengembalian Tabungan') OR title LIKE 'Alokasi:%' OR title LIKE 'Alokasi Awal:%' OR title LIKE 'Pengembalian Tabungan:%'")
    suspend fun deleteGoalSavingsTransactions()

    /**
     * Query daily transactions filtered by date range.
     */
    @Query("SELECT * FROM transactions WHERE dateEpochMillis BETWEEN :startDate AND :endDate ORDER BY dateEpochMillis DESC")
    fun getDailyTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    /**
     * Query transactions for a single day.
     */
    @Query("SELECT * FROM transactions WHERE dateEpochMillis BETWEEN :startOfDay AND :endOfDay ORDER BY dateEpochMillis DESC")
    fun getTransactionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<TransactionEntity>>

    /**
     * Move all transactions from one category to another category.
     */
    @Query("UPDATE transactions SET categoryName = :newCategoryName, updatedAt = :updatedAt WHERE categoryName = :oldCategoryName")
    suspend fun moveTransactionsCategory(oldCategoryName: String, newCategoryName: String, updatedAt: Long = System.currentTimeMillis()): Int

    /**
     * Move/update category for a specific transaction by ID.
     */
    @Query("UPDATE transactions SET categoryName = :newCategoryName, updatedAt = :updatedAt WHERE id = :transactionId")
    suspend fun updateTransactionCategory(transactionId: Long, newCategoryName: String, updatedAt: Long = System.currentTimeMillis()): Int

    /**
     * Query transactions for a specific category.
     */
    @Query("SELECT * FROM transactions WHERE categoryName = :categoryName ORDER BY dateEpochMillis DESC")
    fun getTransactionsByCategory(categoryName: String): Flow<List<TransactionEntity>>
}

/**
 * Typealias for DailyTransactionDao pointing to TransactionDao.
 */
typealias DailyTransactionDao = TransactionDao
