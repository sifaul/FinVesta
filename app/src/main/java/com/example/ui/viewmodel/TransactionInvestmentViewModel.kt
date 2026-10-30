package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FinVestaDatabase
import com.example.data.model.AssetType
import com.example.data.model.Investment
import com.example.data.model.InvestmentAssetEntity
import com.example.data.model.Transaction
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.FinVestaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel that acts as the data layer between the UI and Room DAO,
 * providing reactive StateFlows and methods to insert, query, update,
 * and delete transactions and investments.
 */
open class TransactionInvestmentViewModel(
    application: Application,
    private val repository: FinVestaRepository = FinVestaRepository(
        FinVestaDatabase.getDatabase(application).finVestaDao()
    )
) : AndroidViewModel(application) {

    // =========================================================================
    // REACTIVE STATEFLOWS FOR UI OBSERVATION
    // =========================================================================

    /**
     * All financial transactions (incomes and expenses) sorted newest first.
     */
    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * All income transactions sorted newest first.
     */
    val allIncomes: StateFlow<List<Transaction>> = repository.allIncomes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * All daily expense transactions sorted newest first.
     */
    val allExpenses: StateFlow<List<Transaction>> = repository.allDailyExpenses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * All tracked investment assets and portfolio instruments.
     */
    val allInvestments: StateFlow<List<Investment>> = repository.allAssets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Total income aggregate.
     */
    val totalIncome: StateFlow<Double> = repository.getTotalIncome()
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    /**
     * Total expense aggregate.
     */
    val totalExpense: StateFlow<Double> = repository.getTotalExpense()
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    /**
     * Total portfolio valuation in IDR.
     */
    val totalPortfolioValue: StateFlow<Double> = repository.getTotalPortfolioCurrentValue()
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    /**
     * Total portfolio cost basis / invested capital in IDR.
     */
    val totalPortfolioInvestedCost: StateFlow<Double> = repository.getTotalPortfolioInvestedCost()
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    // =========================================================================
    // TRANSACTION: INSERT METHODS
    // =========================================================================

    /**
     * Insert a transaction entity directly into Room.
     */
    fun insertTransaction(transaction: Transaction, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val insertedId = repository.insertTransaction(transaction)
            onComplete?.invoke(insertedId)
        }
    }

    /**
     * Convenience method to insert a transaction with individual attributes.
     */
    fun insertTransaction(
        type: TransactionType,
        title: String,
        amount: Double,
        category: String,
        wallet: String = "BCA",
        note: String = "",
        date: Long = System.currentTimeMillis(),
        onComplete: ((Long) -> Unit)? = null
    ) {
        val entity = TransactionEntity(
            type = type,
            title = title.trim().ifEmpty { category },
            amount = amount,
            categoryName = category,
            wallet = wallet.trim().ifEmpty { "Utama" },
            note = note.trim(),
            dateEpochMillis = date,
            isEncrypted = true
        )
        insertTransaction(entity, onComplete)
    }

    /**
     * Insert an income transaction.
     */
    fun insertIncome(
        title: String,
        amount: Double,
        category: String,
        wallet: String = "BCA",
        note: String = "",
        date: Long = System.currentTimeMillis()
    ) {
        insertTransaction(
            type = TransactionType.INCOME,
            title = title,
            amount = amount,
            category = category,
            wallet = wallet,
            note = note,
            date = date
        )
    }

    /**
     * Insert an expense transaction.
     */
    fun insertExpense(
        title: String,
        amount: Double,
        category: String,
        wallet: String = "BCA",
        note: String = "",
        date: Long = System.currentTimeMillis()
    ) {
        insertTransaction(
            type = TransactionType.EXPENSE,
            title = title,
            amount = amount,
            category = category,
            wallet = wallet,
            note = note,
            date = date
        )
    }

    // =========================================================================
    // TRANSACTION: QUERY METHODS
    // =========================================================================

    /**
     * Query all transactions reactively.
     */
    fun queryAllTransactions(): Flow<List<Transaction>> = repository.allTransactions

    /**
     * Query transactions filtered by type (INCOME or EXPENSE).
     */
    fun queryTransactionsByType(type: TransactionType): Flow<List<Transaction>> =
        repository.getTransactionsByType(type)

    /**
     * Query recent transactions limited to [limit] rows.
     */
    fun queryRecentTransactions(limit: Int = 10): Flow<List<Transaction>> =
        repository.getRecentTransactions(limit)

    /**
     * Query transactions occurring within a timestamp window.
     */
    fun queryTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        repository.getTransactionsByDateRange(startDate, endDate)

    /**
     * Search transactions matching query keyword in title or category name.
     */
    fun searchTransactions(keyword: String): Flow<List<Transaction>> =
        repository.searchTransactions(keyword)

    // =========================================================================
    // TRANSACTION: UPDATE & DELETE METHODS
    // =========================================================================

    /**
     * Update an existing transaction in Room.
     */
    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    /**
     * Delete a transaction entity from Room.
     */
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    /**
     * Delete a transaction by primary key ID.
     */
    fun deleteTransactionById(id: Long) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
        }
    }

    /**
     * Delete all transactions from the local database.
     */
    fun clearAllTransactions() {
        viewModelScope.launch {
            repository.clearAllTransactions()
        }
    }

    // =========================================================================
    // INVESTMENT: INSERT METHODS
    // =========================================================================

    /**
     * Insert an investment asset into Room.
     */
    fun insertInvestment(investment: Investment, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val insertedId = repository.insertAsset(investment)
            onComplete?.invoke(insertedId)
        }
    }

    /**
     * Convenience method to insert an investment with individual attributes.
     */
    fun insertInvestment(
        name: String,
        assetType: AssetType,
        units: Double,
        unitLabel: String = "Unit",
        avgBuyPrice: Double,
        currentPrice: Double = avgBuyPrice,
        platform: String = "Bibit / Ajaib",
        note: String = "",
        onComplete: ((Long) -> Unit)? = null
    ) {
        val entity = InvestmentAssetEntity(
            name = name.trim(),
            assetType = assetType,
            units = units,
            unitLabel = unitLabel.trim().ifEmpty { "Unit" },
            avgBuyPrice = avgBuyPrice,
            currentPrice = currentPrice,
            platform = platform.trim().ifEmpty { "Portofolio Pribadi" },
            note = note.trim(),
            updatedAt = System.currentTimeMillis()
        )
        insertInvestment(entity, onComplete)
    }

    // =========================================================================
    // INVESTMENT: QUERY METHODS
    // =========================================================================

    /**
     * Query all investments reactively.
     */
    fun queryAllInvestments(): Flow<List<Investment>> = repository.allAssets

    /**
     * Query investments filtered by asset type (e.g. SAHAM, KRIPTO, REKSA_DANA, EMAS).
     */
    fun queryInvestmentsByType(assetType: AssetType): Flow<List<Investment>> =
        repository.getAssetsByType(assetType)

    // =========================================================================
    // INVESTMENT: UPDATE & DELETE METHODS
    // =========================================================================

    /**
     * Update an investment asset in Room.
     */
    fun updateInvestment(investment: Investment) {
        viewModelScope.launch {
            repository.updateAsset(investment.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    /**
     * Update current market price of an investment asset.
     */
    fun updateInvestmentPrice(investment: Investment, newPrice: Double) {
        viewModelScope.launch {
            repository.updateAsset(
                investment.copy(
                    currentPrice = newPrice,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Delete an investment asset from Room.
     */
    fun deleteInvestment(investment: Investment) {
        viewModelScope.launch {
            repository.deleteAsset(investment)
        }
    }

    /**
     * Delete an investment asset by its primary key ID.
     */
    fun deleteInvestmentById(id: Long) {
        viewModelScope.launch {
            repository.deleteAssetById(id)
        }
    }
}

/**
 * Typealias for TransactionViewModel matching standard naming conventions.
 */
typealias TransactionViewModel = TransactionInvestmentViewModel
