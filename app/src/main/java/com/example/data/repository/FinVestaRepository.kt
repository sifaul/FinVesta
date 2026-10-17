package com.example.data.repository

import com.example.data.local.FinVestaDao
import com.example.data.model.AssetType
import com.example.data.model.ExpenseCategory
import com.example.data.model.IncomeCategory
import com.example.data.model.InvestmentAssetEntity
import com.example.data.model.PaymentReminderEntity
import com.example.data.model.ReminderFrequency
import com.example.data.model.ReminderStatus
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.util.DefaultCategories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar

class FinVestaRepository(private val dao: FinVestaDao) {

    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val allIncomes: Flow<List<TransactionEntity>> = dao.getAllIncomes()
    val allDailyExpenses: Flow<List<TransactionEntity>> = dao.getAllDailyExpenses()
    val allAssets: Flow<List<InvestmentAssetEntity>> = dao.getAllAssets()
    val allReminders: Flow<List<PaymentReminderEntity>> = dao.getAllReminders()
    val allGoals: Flow<List<com.example.data.model.FinancialGoalEntity>> = dao.getAllGoals()
    val allCategories: Flow<List<com.example.data.model.CustomCategoryEntity>> = dao.getAllCategories()

    fun getRecentTransactions(limit: Int = 5): Flow<List<TransactionEntity>> = dao.getRecentTransactions(limit)
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>> = dao.getTransactionsByType(type)
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> = dao.getTransactionsByDateRange(startDate, endDate)
    fun getDailyExpensesForRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> = dao.getDailyExpensesForRange(startDate, endDate)
    fun getDailyExpenseTotalForRange(startDate: Long, endDate: Long): Flow<Double?> = dao.getDailyExpenseTotalForRange(startDate, endDate)
    fun getIncomesForRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> = dao.getIncomesForRange(startDate, endDate)
    fun searchTransactions(query: String): Flow<List<TransactionEntity>> = dao.searchTransactions(query)
    fun getTotalIncome(): Flow<Double?> = dao.getTotalIncome()
    fun getTotalExpense(): Flow<Double?> = dao.getTotalExpense()
    fun getTotalPortfolioCurrentValue(): Flow<Double?> = dao.getTotalPortfolioCurrentValue()
    fun getTotalPortfolioInvestedCost(): Flow<Double?> = dao.getTotalPortfolioInvestedCost()
    fun getAssetsByType(type: AssetType): Flow<List<InvestmentAssetEntity>> = dao.getAssetsByType(type)

    suspend fun insertTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
        dao.insertTransaction(transaction)
    }

    suspend fun insertTransactions(transactions: List<TransactionEntity>) = withContext(Dispatchers.IO) {
        dao.insertTransactions(transactions)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        dao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        dao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteTransactionById(id)
    }

    suspend fun clearAllTransactions() = withContext(Dispatchers.IO) {
        dao.clearAllTransactions()
    }

    suspend fun moveTransactionsCategory(oldCategoryName: String, newCategoryName: String): Int = withContext(Dispatchers.IO) {
        dao.moveTransactionsCategory(oldCategoryName, newCategoryName)
    }

    suspend fun updateTransactionCategory(transactionId: Long, newCategoryName: String): Int = withContext(Dispatchers.IO) {
        dao.updateTransactionCategory(transactionId, newCategoryName)
    }

    fun getTransactionsByCategory(categoryName: String): Flow<List<TransactionEntity>> =
        dao.getTransactionsByCategory(categoryName)

    suspend fun insertAsset(asset: InvestmentAssetEntity): Long = withContext(Dispatchers.IO) {
        dao.insertAsset(asset)
    }

    suspend fun insertAssets(assets: List<InvestmentAssetEntity>) = withContext(Dispatchers.IO) {
        dao.insertAssets(assets)
    }

    suspend fun updateAsset(asset: InvestmentAssetEntity) = withContext(Dispatchers.IO) {
        dao.updateAsset(asset)
    }

    suspend fun deleteAsset(asset: InvestmentAssetEntity) = withContext(Dispatchers.IO) {
        dao.deleteAsset(asset)
    }

    suspend fun deleteAssetById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteAssetById(id)
    }

    suspend fun insertReminder(reminder: PaymentReminderEntity): Long = withContext(Dispatchers.IO) {
        dao.insertReminder(reminder)
    }

    suspend fun insertReminders(reminders: List<PaymentReminderEntity>) = withContext(Dispatchers.IO) {
        dao.insertReminders(reminders)
    }

    suspend fun updateReminder(reminder: PaymentReminderEntity) = withContext(Dispatchers.IO) {
        dao.updateReminder(reminder)
    }

    suspend fun deleteReminder(reminder: PaymentReminderEntity) = withContext(Dispatchers.IO) {
        dao.deleteReminder(reminder)
    }

    suspend fun deleteReminderById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteReminderById(id)
    }

    // --- FINANCIAL GOALS ---
    suspend fun insertGoal(goal: com.example.data.model.FinancialGoalEntity): Long = withContext(Dispatchers.IO) {
        dao.insertGoal(goal)
    }

    suspend fun insertGoals(goals: List<com.example.data.model.FinancialGoalEntity>) = withContext(Dispatchers.IO) {
        dao.insertGoals(goals)
    }

    suspend fun updateGoal(goal: com.example.data.model.FinancialGoalEntity) = withContext(Dispatchers.IO) {
        dao.updateGoal(goal)
    }

    suspend fun deleteGoal(goal: com.example.data.model.FinancialGoalEntity) = withContext(Dispatchers.IO) {
        dao.deleteGoal(goal)
    }

    suspend fun allocateFundsToGoal(goalId: Long, addAmount: Double) = withContext(Dispatchers.IO) {
        val currentGoals = dao.getAllGoals().first()
        val target = currentGoals.find { it.id == goalId }
        if (target != null) {
            val updated = target.copy(currentSavedAmount = target.currentSavedAmount + addAmount)
            dao.updateGoal(updated)
        }
    }

    // --- CUSTOM CATEGORIES ---
    suspend fun insertCategory(category: com.example.data.model.CustomCategoryEntity): Long = withContext(Dispatchers.IO) {
        dao.insertCategory(category)
    }

    suspend fun insertCategories(categories: List<com.example.data.model.CustomCategoryEntity>) = withContext(Dispatchers.IO) {
        dao.insertCategories(categories)
    }

    suspend fun updateCategory(category: com.example.data.model.CustomCategoryEntity) = withContext(Dispatchers.IO) {
        dao.updateCategory(category)
    }

    suspend fun deleteCategory(category: com.example.data.model.CustomCategoryEntity) = withContext(Dispatchers.IO) {
        dao.deleteCategory(category)
    }

    suspend fun updateCategoryBudget(categoryId: Long, newBudgetLimit: Double) = withContext(Dispatchers.IO) {
        val all = dao.getAllCategories().first()
        val target = all.find { it.id == categoryId }
        if (target != null) {
            dao.updateCategory(target.copy(monthlyBudgetLimit = newBudgetLimit))
        }
    }

    suspend fun clearAllUserData() = withContext(Dispatchers.IO) {
        dao.clearAllTransactions()
        dao.clearAllAssets()
        dao.clearAllReminders()
        dao.clearAllGoals()
    }

    suspend fun deleteGoalSavingsTransactions() = withContext(Dispatchers.IO) {
        dao.deleteGoalSavingsTransactions()
    }

    suspend fun clearAllCategories() = withContext(Dispatchers.IO) {
        dao.clearAllCategories()
    }

    suspend fun populateInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingCategories = dao.getAllCategories().first()
        if (existingCategories.isEmpty()) {
            dao.insertCategories(DefaultCategories.ALL_DEFAULTS)
        }
    }

    /**
     * Restores missing default categories or resets default categories
     * while preserving user custom categories and existing transactions.
     */
    suspend fun restoreDefaultCategories(): Int = withContext(Dispatchers.IO) {
        val existingCategories = dao.getAllCategories().first()
        val existingNames = existingCategories.map { it.name.trim().lowercase() }.toSet()

        val missingDefaults = DefaultCategories.ALL_DEFAULTS.filter { defaultCat ->
            !existingNames.contains(defaultCat.name.trim().lowercase())
        }

        if (missingDefaults.isNotEmpty()) {
            dao.insertCategories(missingDefaults)
        }
        missingDefaults.size
    }
}
