package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.InvestmentAssetEntity
import com.example.data.model.PaymentReminderEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface FinVestaDao : TransactionDao {

    // --- TRANSACTIONS (Inherited from TransactionDao) ---

    // --- INVESTMENT ASSETS ---
    @Query("SELECT * FROM investment_assets ORDER BY updatedAt DESC")
    fun getAllAssets(): Flow<List<InvestmentAssetEntity>>

    @Query("SELECT * FROM investment_assets WHERE assetType = :assetType ORDER BY updatedAt DESC")
    fun getAssetsByType(assetType: com.example.data.model.AssetType): Flow<List<InvestmentAssetEntity>>

    @Query("SELECT SUM(units * currentPrice) FROM investment_assets")
    fun getTotalPortfolioCurrentValue(): Flow<Double?>

    @Query("SELECT SUM(units * avgBuyPrice) FROM investment_assets")
    fun getTotalPortfolioInvestedCost(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: InvestmentAssetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<InvestmentAssetEntity>)

    @Update
    suspend fun updateAsset(asset: InvestmentAssetEntity)

    @Delete
    suspend fun deleteAsset(asset: InvestmentAssetEntity)

    @Query("DELETE FROM investment_assets WHERE id = :id")
    suspend fun deleteAssetById(id: Long)

    @Query("DELETE FROM investment_assets")
    suspend fun clearAllAssets()

    // --- PAYMENT REMINDERS ---
    @Query("SELECT * FROM payment_reminders ORDER BY dueDateEpochMillis ASC")
    fun getAllReminders(): Flow<List<PaymentReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: PaymentReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<PaymentReminderEntity>)

    @Update
    suspend fun updateReminder(reminder: PaymentReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: PaymentReminderEntity)

    @Query("DELETE FROM payment_reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("DELETE FROM payment_reminders")
    suspend fun clearAllReminders()

    // --- FINANCIAL GOALS (TUJUAN KEUANGAN) ---
    @Query("SELECT * FROM financial_goals ORDER BY targetDateEpochMillis ASC")
    fun getAllGoals(): Flow<List<com.example.data.model.FinancialGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: com.example.data.model.FinancialGoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<com.example.data.model.FinancialGoalEntity>)

    @Update
    suspend fun updateGoal(goal: com.example.data.model.FinancialGoalEntity)

    @Delete
    suspend fun deleteGoal(goal: com.example.data.model.FinancialGoalEntity)

    @Query("DELETE FROM financial_goals WHERE id = :id")
    suspend fun deleteGoalById(id: Long)

    @Query("DELETE FROM financial_goals")
    suspend fun clearAllGoals()

    // --- CUSTOM CATEGORIES (KATEGORI PENGELUARAN KUSTOM) ---
    @Query("SELECT * FROM custom_categories ORDER BY createdAt ASC")
    fun getAllCategories(): Flow<List<com.example.data.model.CustomCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: com.example.data.model.CustomCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<com.example.data.model.CustomCategoryEntity>)

    @Update
    suspend fun updateCategory(category: com.example.data.model.CustomCategoryEntity)

    @Delete
    suspend fun deleteCategory(category: com.example.data.model.CustomCategoryEntity)

    @Query("DELETE FROM custom_categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    @Query("UPDATE custom_categories SET monthlyBudgetLimit = 0.0 WHERE isDefault = 1 AND monthlyBudgetLimit <= 1000000.0")
    suspend fun clearDefaultPresetBudgets()

    @Query("UPDATE custom_categories SET monthlyBudgetLimit = 0.0 WHERE name LIKE '%Belanja%'")
    suspend fun resetBelanjaBudget()

    @Query("UPDATE custom_categories SET monthlyBudgetLimit = :limit WHERE name LIKE :namePattern")
    suspend fun updateBudgetByNamePattern(namePattern: String, limit: Double)

    @Query("DELETE FROM custom_categories")
    suspend fun clearAllCategories()
}
