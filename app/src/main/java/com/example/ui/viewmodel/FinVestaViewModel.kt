package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.core.content.edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.example.data.local.FinVestaDatabase
import com.example.data.model.AssetType
import com.example.data.model.InvestmentAssetEntity
import com.example.data.model.PaymentReminderEntity
import com.example.data.model.ReminderFrequency
import com.example.data.model.ReminderStatus
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.FinVestaRepository
import com.example.data.security.E2EEncryptionManager
import com.example.data.sync.ConnectedDevice
import com.example.data.sync.SyncManager
import com.example.data.sync.SyncState
import com.example.ui.theme.AppThemeMode
import com.example.util.DateUtils
import com.example.util.NotificationHelper
import com.example.util.ReportExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

data class MonthlyFinanceSummary(
    val monthName: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val netSavings: Double
)

/**
 * State holding aggregated financial metrics and recent transactions for the Dashboard.
 */
data class DashboardFinancialState(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netCashFlow: Double = 0.0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val totalTransactionCount: Int = 0,
    val isLoading: Boolean = false
)

data class CategoryBudgetAlert(
    val categoryId: Long,
    val categoryName: String,
    val monthlyBudgetLimit: Double,
    val currentSpent: Double,
    val percentage: Double, // Persentase pengeluaran (misal 85.0%)
    val isOverBudget: Boolean, // true jika >= 100%
    val remainingBudget: Double,
    val iconName: String,
    val colorHex: String
)

class FinVestaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinVestaRepository
    private val syncManager = SyncManager()
    val firestoreSyncService: com.example.data.sync.FirestoreSyncService
    val backupRestoreManager: com.example.data.sync.BackupRestoreManager

    private val _lastBackupTime = MutableStateFlow(0L)
    val lastBackupTime: StateFlow<Long> = _lastBackupTime.asStateFlow()

    private val _isAutoBackupEnabled = MutableStateFlow(true)
    val isAutoBackupEnabled: StateFlow<Boolean> = _isAutoBackupEnabled.asStateFlow()

    init {
        val db = FinVestaDatabase.getDatabase(application)
        repository = FinVestaRepository(db.finVestaDao())
        firestoreSyncService = com.example.data.sync.FirestoreSyncService(application, repository)
        backupRestoreManager = com.example.data.sync.BackupRestoreManager(repository)
        _lastBackupTime.value = backupRestoreManager.getLastBackupTime(application)
        _isAutoBackupEnabled.value = backupRestoreManager.isAutoBackupEnabled(application)
        viewModelScope.launch(Dispatchers.IO) {
            // One-time cleanup of all transactions and example data for a clean slate
            val prefs = application.getSharedPreferences("finvesta_app_prefs", Context.MODE_PRIVATE)
            val cleaned = prefs.getBoolean("sample_data_cleared_empty_trend_v12", false)
            if (!cleaned) {
                repository.deleteGoalSavingsTransactions()
                repository.clearAllCategories()
                prefs.edit { putBoolean("sample_data_cleared_empty_trend_v12", true) }
            }
            // Hilangkan peringatan bawaan: reset anggaran default ke 0 agar tidak memicu peringatan semu (>80%) dari data awal
            try {
                db.finVestaDao().clearDefaultPresetBudgets()
                db.finVestaDao().resetBelanjaBudget()
            } catch (e: Exception) {
                // Ignore fallback
            }
        }
    }

    /**
     * Clear all user data (transactions, assets, reminders, goals) to give a clean slate.
     */
    fun clearAllUserData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearAllUserData()
            onComplete()
        }
    }

    // --- FLOWS FROM REPOSITORY ---
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allIncomes: StateFlow<List<TransactionEntity>> = repository.allIncomes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDailyExpenses: StateFlow<List<TransactionEntity>> = repository.allDailyExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 5 most recent transactions for dashboard
    val dashboardRecentTransactions: StateFlow<List<TransactionEntity>> = repository.getRecentTransactions(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Aggregated reactive financial state for dashboard (Income, Expense, Net Cashflow, Recent Items)
    // Sesuai permintaan pengguna: Saldo kas utama berkurang saat dana dialokasikan ke tabungan / pos spending,
    // dan dana tabungan / pos spending tidak tercatat dalam total saldo kas utama.
    // Pengeluaran dari tabungan / pos spending hanya mengurangi saldo tujuan, bukan saldo kas utama.
    val dashboardFinancialState: StateFlow<DashboardFinancialState> = allTransactions.map { txList ->
        val generalTxs = txList.filter { !it.categoryName.equals("Pengeluaran Tujuan", ignoreCase = true) }
        val income = generalTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val mainCashExpenses = generalTxs.filter { 
            it.type == TransactionType.EXPENSE && 
            !isFromSavings(it.wallet, it.categoryName) && 
            !isFromSpendingPos(it.wallet) 
        }.sumOf { it.amount }
        val totalExpense = generalTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        DashboardFinancialState(
            totalIncome = income,
            totalExpense = totalExpense,
            netCashFlow = income - mainCashExpenses,
            recentTransactions = generalTxs.take(5),
            totalTransactionCount = generalTxs.size,
            isLoading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardFinancialState(isLoading = true)
    )

    val allAssets: StateFlow<List<InvestmentAssetEntity>> = repository.allAssets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allReminders: StateFlow<List<PaymentReminderEntity>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allGoals: StateFlow<List<com.example.data.model.FinancialGoalEntity>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCategories: StateFlow<List<com.example.data.model.CustomCategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- BUDGET ALERTS (> 80% OF MONTHLY BUDGET LIMIT) ---
    private val _isBudgetAlertsEnabled = MutableStateFlow(true)
    val isBudgetAlertsEnabled: StateFlow<Boolean> = _isBudgetAlertsEnabled.asStateFlow()

    private val _dismissedAlertCategoryIds = MutableStateFlow<Set<Long>>(emptySet())
    val dismissedAlertCategoryIds: StateFlow<Set<Long>> = _dismissedAlertCategoryIds.asStateFlow()

    fun dismissCategoryAlert(categoryId: Long) {
        _dismissedAlertCategoryIds.value = _dismissedAlertCategoryIds.value + categoryId
    }

    fun dismissAllBudgetAlerts() {
        val currentIds = activeBudgetAlerts.value.map { it.categoryId }.toSet()
        _dismissedAlertCategoryIds.value = _dismissedAlertCategoryIds.value + currentIds
    }

    fun toggleBudgetAlerts(enabled: Boolean) {
        _isBudgetAlertsEnabled.value = enabled
    }

    val activeBudgetAlerts: StateFlow<List<CategoryBudgetAlert>> = combine(
        allTransactions,
        allCategories,
        _isBudgetAlertsEnabled,
        _dismissedAlertCategoryIds
    ) { txs, cats, enabled, dismissed ->
        if (!enabled) {
            emptyList()
        } else {
            computeBudgetAlerts(txs, cats).filter { it.categoryId !in dismissed }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lastBudgetAlertEvent = MutableStateFlow<CategoryBudgetAlert?>(null)
    val lastBudgetAlertEvent: StateFlow<CategoryBudgetAlert?> = _lastBudgetAlertEvent.asStateFlow()

    fun dismissBudgetAlertEvent() {
        _lastBudgetAlertEvent.value = null
    }

    // --- SYNC & SECURITY STATE ---
    val syncState: StateFlow<SyncState> = syncManager.syncState
    val lastSyncTimestamp: StateFlow<Long> = syncManager.lastSyncTimestamp
    val deviceList: StateFlow<List<ConnectedDevice>> = syncManager.deviceList
    val realtimeLatencyMs: StateFlow<Int> = syncManager.realtimeLatencyMs
    val localDeviceId: String = syncManager.localDeviceId
    val pairingCode: String = syncManager.pairingCode

    private val _isE2eeActive = MutableStateFlow(true)
    val isE2eeActive: StateFlow<Boolean> = _isE2eeActive.asStateFlow()

    val keyFingerprint: String = E2EEncryptionManager.getKeyFingerprint()

    // --- THEME & UI STATE ---
    private val _appThemeMode = MutableStateFlow(
        try {
            val saved = application.getSharedPreferences("finvesta_app_prefs", Context.MODE_PRIVATE)
                .getString("selected_theme_mode", null)
            if (saved != null) AppThemeMode.valueOf(saved) else AppThemeMode.SYSTEM
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
    )
    val appThemeMode: StateFlow<AppThemeMode> = _appThemeMode.asStateFlow()

    private val _selectedMonthOffset = MutableStateFlow(0) // 0 = current month, -1 = last month
    val selectedMonthOffset: StateFlow<Int> = _selectedMonthOffset.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _appThemeMode.value = mode
        try {
            val prefs = getApplication<Application>().getSharedPreferences("finvesta_app_prefs", Context.MODE_PRIVATE)
            prefs.edit { putString("selected_theme_mode", mode.name) }
        } catch (_: Exception) {}
    }

    fun setAppThemeMode(mode: AppThemeMode) = setThemeMode(mode)

    // --- MONTHLY SPENDING BUDGET ---
    private val _monthlySpendingLimit = MutableStateFlow(
        try {
            val prefs = getApplication<Application>().getSharedPreferences("finvesta_app_prefs", Context.MODE_PRIVATE)
            prefs.getFloat("monthly_spending_limit", 3_000_000f).toDouble()
        } catch (_: Exception) {
            3_000_000.0
        }
    )
    val monthlySpendingLimit: StateFlow<Double> = _monthlySpendingLimit.asStateFlow()

    fun updateMonthlySpendingLimit(newLimit: Double) {
        _monthlySpendingLimit.value = newLimit
        try {
            val prefs = getApplication<Application>().getSharedPreferences("finvesta_app_prefs", Context.MODE_PRIVATE)
            prefs.edit { putFloat("monthly_spending_limit", newLimit.toFloat()) }
        } catch (_: Exception) {}
    }

    fun toggleE2ee() {
        _isE2eeActive.value = !_isE2eeActive.value
    }

    // --- FIRESTORE CLOUD SYNC & BACKUP ---
    val connectedEmail: StateFlow<String> = firestoreSyncService.connectedEmail
    val cloudSyncStatus: StateFlow<com.example.data.sync.CloudSyncStatus> = firestoreSyncService.syncStatus
    val lastCloudBackupTime: StateFlow<Long> = firestoreSyncService.lastBackupTime
    val isAutoCloudSyncEnabled: StateFlow<Boolean> = firestoreSyncService.isAutoSyncEnabled

    fun connectEmail(email: String, context: Context? = null): Boolean {
        val success = firestoreSyncService.connectEmail(email)
        if (success) {
            context?.let {
                Toast.makeText(it, "Email terhubung: ${email.trim()}", Toast.LENGTH_SHORT).show()
            }
        }
        return success
    }

    fun disconnectEmail(context: Context? = null) {
        firestoreSyncService.disconnectEmail()
        context?.let {
            Toast.makeText(it, "Koneksi email dilepas. Cadangan cloud dinonaktifkan.", Toast.LENGTH_SHORT).show()
        }
    }

    fun setAutoCloudSyncEnabled(enabled: Boolean) {
        firestoreSyncService.setAutoSyncEnabled(enabled)
    }

    fun backupToCloudNow(context: Context? = null) {
        viewModelScope.launch {
            val result = firestoreSyncService.backupDataToCloud()
            context?.let { ctx ->
                when (result) {
                    is com.example.data.sync.CloudSyncStatus.Success -> {
                        Toast.makeText(ctx, result.message, Toast.LENGTH_LONG).show()
                    }
                    is com.example.data.sync.CloudSyncStatus.Error -> {
                        Toast.makeText(ctx, result.errorMessage, Toast.LENGTH_LONG).show()
                    }
                    else -> Unit
                }
            }
        }
    }

    fun restoreFromCloudNow(context: Context? = null) {
        viewModelScope.launch {
            val result = firestoreSyncService.restoreDataFromCloud()
            context?.let { ctx ->
                when (result) {
                    is com.example.data.sync.CloudSyncStatus.Success -> {
                        Toast.makeText(ctx, result.message, Toast.LENGTH_LONG).show()
                    }
                    is com.example.data.sync.CloudSyncStatus.Error -> {
                        Toast.makeText(ctx, result.errorMessage, Toast.LENGTH_LONG).show()
                    }
                    else -> Unit
                }
            }
        }
    }

    fun syncNow(context: Context? = null) {
        viewModelScope.launch {
            syncManager.performSync()
            if (firestoreSyncService.isUserConnected()) {
                firestoreSyncService.backupDataToCloud()
            }
            context?.let {
                Toast.makeText(it, "Sinkronisasi real-time & pencadangan awan diperbarui!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- GOOGLE DRIVE & LOCAL FILE BACKUP / RESTORE ---
    fun exportBackupToUri(context: Context, uri: android.net.Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: Exception) {
                        // Persistable permission might not be supported by some providers, continue safely
                    }
                    val outputStream = context.contentResolver.openOutputStream(uri, "wt")
                        ?: context.contentResolver.openOutputStream(uri)
                    if (outputStream == null) {
                        return@withContext null
                    }
                    backupRestoreManager.writeBackupToStream(context, outputStream)
                }

                withContext(Dispatchers.Main) {
                    if (success == true) {
                        _lastBackupTime.value = System.currentTimeMillis()
                        onResult(true, "Data berhasil dicadangkan ke Google Drive / Penyimpanan!")
                    } else if (success == false) {
                        onResult(false, "Gagal menulis berkas cadangan.")
                    } else {
                        onResult(false, "Tidak dapat membuka lokasi berkas di Google Drive / perangkat.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Terjadi kesalahan: ${e.localizedMessage ?: "Gagal mencadangkan"}")
                }
            }
        }
    }

    fun importBackupFromUri(context: Context, uri: android.net.Uri, onResult: (com.example.data.sync.RestoreSummary) -> Unit) {
        viewModelScope.launch {
            try {
                val summary = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: Exception) {
                        // Ignore
                    }
                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: return@withContext com.example.data.sync.RestoreSummary(false, "Tidak dapat membaca berkas cadangan.")
                    backupRestoreManager.restoreFromStream(inputStream)
                }

                withContext(Dispatchers.Main) {
                    if (summary.success) {
                        _lastBackupTime.value = System.currentTimeMillis()
                    }
                    onResult(summary)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(com.example.data.sync.RestoreSummary(false, "Gagal memproses berkas: ${e.localizedMessage ?: "Format tidak cocok"}"))
                }
            }
        }
    }

    fun shareToGoogleDrive(context: Context, onResult: (android.content.Intent?, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val intent = backupRestoreManager.createGoogleDriveIntent(context)
                withContext(Dispatchers.Main) {
                    if (intent != null) {
                        _lastBackupTime.value = System.currentTimeMillis()
                        onResult(intent, null)
                    } else {
                        onResult(null, "Gagal menyiapkan berkas cadangan untuk Google Drive.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(null, e.localizedMessage ?: "Kesalahan tidak diketahui")
                }
            }
        }
    }

    fun restoreFromLocalSnapshot(context: Context, onResult: (com.example.data.sync.RestoreSummary) -> Unit) {
        viewModelScope.launch {
            try {
                val summary = backupRestoreManager.restoreFromLocalSnapshot(context)
                withContext(Dispatchers.Main) {
                    if (summary.success) {
                        _lastBackupTime.value = System.currentTimeMillis()
                    }
                    onResult(summary)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(com.example.data.sync.RestoreSummary(false, "Gagal memulihkan snapshot: ${e.localizedMessage}"))
                }
            }
        }
    }

    fun hasLocalSnapshot(context: Context): Boolean {
        return backupRestoreManager.hasLocalSnapshot(context)
    }

    fun shareBackup(context: Context, onResult: (android.content.Intent?) -> Unit) {
        viewModelScope.launch {
            val intent = backupRestoreManager.createShareBackupIntent(context)
            withContext(Dispatchers.Main) {
                if (intent != null) {
                    _lastBackupTime.value = System.currentTimeMillis()
                }
                onResult(intent)
            }
        }
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        _isAutoBackupEnabled.value = enabled
        backupRestoreManager.setAutoBackupEnabled(context, enabled)
    }

    private fun triggerAutoSnapshot() {
        if (_isAutoBackupEnabled.value) {
            viewModelScope.launch(Dispatchers.IO) {
                backupRestoreManager.saveLocalSnapshot(getApplication())
            }
        }
    }

    // --- TRANSACTION ACTIONS ---
    fun addTransaction(
        type: TransactionType,
        title: String,
        amount: Double,
        categoryName: String,
        wallet: String = "BCA",
        note: String = "",
        dateEpochMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                type = type,
                title = title.trim(),
                amount = amount,
                categoryName = categoryName,
                wallet = wallet.trim().ifEmpty { "Utama" },
                note = note.trim(),
                dateEpochMillis = dateEpochMillis,
                isEncrypted = _isE2eeActive.value
            )
            repository.insertTransaction(entity)
            triggerAutoSnapshot()

            // Jika pengeluaran bersumber dari Tabungan, kurangi saldo tabungan tujuan yang sesuai
            if (type == TransactionType.EXPENSE && isFromSavings(wallet, categoryName)) {
                deductFromGoalSavings(wallet, amount)
            }

            // Evaluasi peringatan anggaran jika pengeluaran bulanan kategori mencapai/melebihi 80% dari batas
            if (type == TransactionType.EXPENSE && DateUtils.isCurrentMonth(dateEpochMillis) && !isFromSavings(wallet, categoryName)) {
                checkAndTriggerBudgetAlert(categoryName, amount)
            }
        }
    }

    private fun computeBudgetAlerts(
        transactions: List<TransactionEntity>,
        categories: List<com.example.data.model.CustomCategoryEntity>
    ): List<CategoryBudgetAlert> {
        val currentMonthExpenses = transactions.filter {
            it.type == TransactionType.EXPENSE && DateUtils.isCurrentMonth(it.dateEpochMillis)
        }

        val alerts = mutableListOf<CategoryBudgetAlert>()
        val expenseCats = categories.filter { it.isExpense && it.monthlyBudgetLimit > 0 }

        for (cat in expenseCats) {
            val spent = currentMonthExpenses.filter { tx ->
                isCategoryMatch(tx.categoryName, cat.name)
            }.sumOf { it.amount }

            val percentage = (spent / cat.monthlyBudgetLimit) * 100.0
            // Peringatan HANYA muncul jika sudah lebih dari 80% (> 80.0). Jika belum, jangan munculkan.
            if (percentage > 80.0) {
                alerts.add(
                    CategoryBudgetAlert(
                        categoryId = cat.id,
                        categoryName = cat.name,
                        monthlyBudgetLimit = cat.monthlyBudgetLimit,
                        currentSpent = spent,
                        percentage = percentage,
                        isOverBudget = percentage >= 100.0,
                        remainingBudget = (cat.monthlyBudgetLimit - spent).coerceAtLeast(0.0),
                        iconName = cat.iconName,
                        colorHex = cat.colorHex
                    )
                )
            }
        }
        return alerts.sortedByDescending { it.percentage }
    }

    private fun isCategoryMatch(txCategory: String, customCatName: String): Boolean {
        if (txCategory.equals(customCatName, ignoreCase = true)) return true
        val normTx = txCategory.replace("&", "").replace("/", "").replace("-", "").replace(" ", "").lowercase()
        val normCat = customCatName.replace("&", "").replace("/", "").replace("-", "").replace(" ", "").lowercase()
        return normTx.contains(normCat) || normCat.contains(normTx)
    }

    private fun checkAndTriggerBudgetAlert(categoryName: String, newTxAmount: Double) {
        if (!_isBudgetAlertsEnabled.value) return
        val currentMonthExpenses = allTransactions.value.filter {
            it.type == TransactionType.EXPENSE && DateUtils.isCurrentMonth(it.dateEpochMillis)
        }
        val cats = allCategories.value
        val matchedCat = cats.firstOrNull { it.isExpense && isCategoryMatch(categoryName, it.name) }

        if (matchedCat != null && matchedCat.monthlyBudgetLimit > 0) {
            val existingSpent = currentMonthExpenses.filter { isCategoryMatch(it.categoryName, matchedCat.name) }.sumOf { it.amount }
            val totalSpent = existingSpent + newTxAmount
            val percentage = (totalSpent / matchedCat.monthlyBudgetLimit) * 100.0

            // Peringatan HANYA muncul saat pengeluaran sudah lebih dari 80%. Jika belum (>80%), jangan munculkan.
            if (percentage > 80.0) {
                _dismissedAlertCategoryIds.value = _dismissedAlertCategoryIds.value - matchedCat.id
                val alert = CategoryBudgetAlert(
                    categoryId = matchedCat.id,
                    categoryName = matchedCat.name,
                    monthlyBudgetLimit = matchedCat.monthlyBudgetLimit,
                    currentSpent = totalSpent,
                    percentage = percentage,
                    isOverBudget = percentage >= 100.0,
                    remainingBudget = (matchedCat.monthlyBudgetLimit - totalSpent).coerceAtLeast(0.0),
                    iconName = matchedCat.iconName,
                    colorHex = matchedCat.colorHex
                )
                NotificationHelper.sendBudgetAlertNotification(
                    getApplication(),
                    matchedCat.name,
                    totalSpent,
                    matchedCat.monthlyBudgetLimit,
                    percentage
                )
                _lastBudgetAlertEvent.value = alert
            }
        }
    }

    fun insertTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
            if (transaction.type == TransactionType.EXPENSE && DateUtils.isCurrentMonth(transaction.dateEpochMillis)) {
                checkAndTriggerBudgetAlert(transaction.categoryName, transaction.amount)
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)

            // Jika transaksi yang dihapus adalah alokasi ke tabungan, kurangi kembali tabungan
            if (transaction.type == TransactionType.EXPENSE && transaction.categoryName == "Alokasi Tabungan") {
                val goalName = transaction.title.removePrefix("Alokasi: ").removePrefix("Alokasi Tabungan: ").removePrefix("Alokasi Awal: ").trim()
                val targetGoal = allGoals.value.find { it.name.equals(goalName, ignoreCase = true) }
                if (targetGoal != null) {
                    val newSaved = (targetGoal.currentSavedAmount - transaction.amount).coerceAtLeast(0.0)
                    repository.updateGoal(targetGoal.copy(currentSavedAmount = newSaved))
                }
            } else if (transaction.type == TransactionType.EXPENSE && transaction.categoryName == "Alokasi Pos Spending") {
                // Jika transaksi alokasi ke pos spending dihapus, kembalikan alokasi pos spending
                val posName = transaction.title.removePrefix("Alokasi Pos: ").removePrefix("Tambah Dana: ").removePrefix("Alokasi: ").trim()
                val targetCat = allCategories.value.find { it.isExpense && it.name.equals(posName, ignoreCase = true) }
                if (targetCat != null) {
                    val newLimit = (targetCat.monthlyBudgetLimit - transaction.amount).coerceAtLeast(0.0)
                    repository.updateCategory(targetCat.copy(monthlyBudgetLimit = newLimit))
                }
            } else if (transaction.type == TransactionType.INCOME && transaction.categoryName == "Pengembalian Pos Spending") {
                // Jika transaksi pengembalian sisa dihapus, pulihkan batas pos spending
                val posName = transaction.title.removePrefix("Pengembalian Sisa: ").trim()
                val targetCat = allCategories.value.find { it.isExpense && it.name.equals(posName, ignoreCase = true) }
                if (targetCat != null) {
                    val newLimit = targetCat.monthlyBudgetLimit + transaction.amount
                    repository.updateCategory(targetCat.copy(monthlyBudgetLimit = newLimit))
                }
            } else if (transaction.type == TransactionType.EXPENSE && isFromSavings(transaction.wallet, transaction.categoryName)) {
                // Kembalikan uang ke tabungan jika transaksi pengeluaran dari tabungan dihapus
                val goalName = extractGoalNameFromWallet(transaction.wallet)
                val targetGoal = if (goalName.isNotEmpty()) {
                    allGoals.value.find { it.name.equals(goalName, ignoreCase = true) }
                } else {
                    allGoals.value.firstOrNull()
                }
                if (targetGoal != null) {
                    val newSaved = targetGoal.currentSavedAmount + transaction.amount
                    repository.updateGoal(targetGoal.copy(currentSavedAmount = newSaved))
                }
            }
            triggerAutoSnapshot()
        }
    }

    private suspend fun deductFromGoalSavings(wallet: String, amount: Double) {
        val currentGoals = allGoals.value
        if (currentGoals.isEmpty()) return

        val goalName = extractGoalNameFromWallet(wallet)
        val targetGoal = if (goalName.isNotEmpty()) {
            currentGoals.find { it.name.equals(goalName, ignoreCase = true) }
        } else {
            currentGoals.firstOrNull { it.currentSavedAmount >= amount } ?: currentGoals.firstOrNull()
        }

        if (targetGoal != null) {
            val newSaved = (targetGoal.currentSavedAmount - amount).coerceAtLeast(0.0)
            repository.updateGoal(targetGoal.copy(currentSavedAmount = newSaved))
        }
    }

    private fun extractGoalNameFromWallet(wallet: String): String {
        return if (wallet.contains(":")) {
            wallet.substringAfter(":").trim()
        } else {
            ""
        }
    }

    fun isFromSavings(wallet: String, categoryName: String): Boolean {
        return wallet.startsWith("Tabungan", ignoreCase = true) ||
                wallet.startsWith("Tujuan", ignoreCase = true) ||
                categoryName.equals("Dari Tabungan", ignoreCase = true) ||
                categoryName.equals("Pengeluaran Tabungan", ignoreCase = true) ||
                categoryName.equals("Pengeluaran Tujuan", ignoreCase = true)
    }

    fun isFromSpendingPos(wallet: String): Boolean {
        return wallet.startsWith("Pos:", ignoreCase = true) ||
                wallet.startsWith("Pos Spending:", ignoreCase = true)
    }

    fun deleteTransactionById(id: Long) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            repository.clearAllTransactions()
        }
    }

    // --- CONVENIENCE ADD TRANSACTION METHODS ---
    fun addIncome(
        title: String,
        amount: Double,
        categoryName: String,
        wallet: String = "BCA",
        note: String = "",
        dateEpochMillis: Long = System.currentTimeMillis()
    ) {
        addTransaction(
            type = TransactionType.INCOME,
            title = title,
            amount = amount,
            categoryName = categoryName,
            wallet = wallet,
            note = note,
            dateEpochMillis = dateEpochMillis
        )
    }

    fun addExpense(
        title: String,
        amount: Double,
        categoryName: String,
        wallet: String = "BCA",
        note: String = "",
        dateEpochMillis: Long = System.currentTimeMillis()
    ) {
        addTransaction(
            type = TransactionType.EXPENSE,
            title = title,
            amount = amount,
            categoryName = categoryName,
            wallet = wallet,
            note = note,
            dateEpochMillis = dateEpochMillis
        )
    }

    // --- QUERY METHODS FOR DASHBOARD & TRANSACTIONS ---
    fun getRecentTransactions(limit: Int = 5) = repository.getRecentTransactions(limit)

    fun queryTransactionsByType(type: TransactionType) = repository.getTransactionsByType(type)

    fun queryTransactionsByDateRange(startDate: Long, endDate: Long) = repository.getTransactionsByDateRange(startDate, endDate)

    fun searchTransactions(query: String) = repository.searchTransactions(query)

    // --- ASSET ACTIONS ---
    fun addAsset(
        name: String,
        assetType: AssetType,
        units: Double,
        unitLabel: String,
        avgBuyPrice: Double,
        currentPrice: Double,
        platform: String,
        note: String
    ) {
        viewModelScope.launch {
            val entity = InvestmentAssetEntity(
                name = name.trim(),
                assetType = assetType,
                units = units,
                unitLabel = unitLabel.trim().ifEmpty { "Unit" },
                avgBuyPrice = avgBuyPrice,
                currentPrice = currentPrice,
                platform = platform.trim().ifEmpty { "Portofolio Pribadi" },
                note = note.trim()
            )
            repository.insertAsset(entity)
        }
    }

    fun insertInvestment(investment: InvestmentAssetEntity) {
        viewModelScope.launch {
            repository.insertAsset(investment)
        }
    }

    fun updateAsset(asset: InvestmentAssetEntity) {
        viewModelScope.launch {
            repository.updateAsset(asset.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun updateAssetPrice(asset: InvestmentAssetEntity, newPrice: Double) {
        viewModelScope.launch {
            repository.updateAsset(asset.copy(currentPrice = newPrice, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteAsset(asset: InvestmentAssetEntity) {
        viewModelScope.launch {
            repository.deleteAsset(asset)
        }
    }

    fun deleteAssetById(id: Long) {
        viewModelScope.launch {
            repository.deleteAssetById(id)
        }
    }

    fun deleteInvestment(investment: InvestmentAssetEntity) = deleteAsset(investment)

    fun deleteInvestmentById(id: Long) = deleteAssetById(id)

    fun queryAllTransactions() = repository.allTransactions

    fun queryAllInvestments() = repository.allAssets

    // --- REMINDER ACTIONS ---
    fun addReminder(
        title: String,
        amount: Double,
        dueDateEpochMillis: Long,
        frequency: ReminderFrequency = ReminderFrequency.BULANAN,
        category: String = "Tagihan",
        paymentAccount: String = "BCA Virtual Account",
        note: String = ""
    ) {
        viewModelScope.launch {
            val entity = PaymentReminderEntity(
                title = title.trim(),
                amount = amount,
                dueDateEpochMillis = dueDateEpochMillis,
                frequency = frequency,
                status = ReminderStatus.PENDING,
                category = category,
                paymentAccount = paymentAccount.trim().ifEmpty { "Transfer Bank" },
                note = note.trim()
            )
            repository.insertReminder(entity)
        }
    }

    fun toggleReminderStatus(reminder: PaymentReminderEntity) {
        viewModelScope.launch {
            val newStatus = if (reminder.status == ReminderStatus.PAID) ReminderStatus.PENDING else ReminderStatus.PAID
            repository.updateReminder(reminder.copy(status = newStatus))
        }
    }

    fun deleteReminder(reminder: PaymentReminderEntity) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    fun sendReminderNotification(context: Context, reminder: PaymentReminderEntity) {
        val success = NotificationHelper.sendPaymentReminderNotification(context, reminder)
        val msg = if (success) "Notifikasi pengingat pembayaran '${reminder.title}' terkirim!" else "Izin notifikasi diperlukan"
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    // --- FINANCIAL GOAL ACTIONS ---
    fun addGoal(
        name: String,
        targetAmount: Double,
        targetDateEpochMillis: Long,
        category: String = "Umum",
        iconName: String = "Home",
        colorHex: String = "#10B981",
        note: String = "",
        initialSavedAmount: Double = 0.0
    ) {
        viewModelScope.launch {
            val entity = com.example.data.model.FinancialGoalEntity(
                name = name.trim(),
                targetAmount = targetAmount,
                currentSavedAmount = initialSavedAmount,
                targetDateEpochMillis = targetDateEpochMillis,
                category = category.trim().ifEmpty { "Umum" },
                iconName = iconName,
                colorHex = colorHex,
                note = note.trim()
            )
            repository.insertGoal(entity)

            // Jika ada saldo awal tabungan, potong dari saldo awal kas via transaksi pengeluaran Alokasi Tabungan
            if (initialSavedAmount > 0) {
                repository.insertTransaction(
                    TransactionEntity(
                        type = TransactionType.EXPENSE,
                        title = "Alokasi Awal: ${entity.name}",
                        amount = initialSavedAmount,
                        categoryName = "Alokasi Tabungan",
                        wallet = "Utama",
                        note = "Setoran saldo awal tabungan ${entity.name}",
                        dateEpochMillis = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun updateGoal(goal: com.example.data.model.FinancialGoalEntity) {
        viewModelScope.launch {
            repository.updateGoal(goal)
        }
    }

    fun allocateFundsToGoal(goal: com.example.data.model.FinancialGoalEntity, amountToAdd: Double) {
        viewModelScope.launch {
            val updated = goal.copy(currentSavedAmount = (goal.currentSavedAmount + amountToAdd).coerceAtLeast(0.0))
            repository.updateGoal(updated)

            // Catat pengeluaran alokasi tabungan sehingga saldo kas utama berkurang dan dana terpisah di tabungan
            repository.insertTransaction(
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    title = "Alokasi: ${goal.name}",
                    amount = amountToAdd,
                    categoryName = "Alokasi Tabungan",
                    wallet = "Utama",
                    note = "Alokasi dana ke tabungan ${goal.name}",
                    dateEpochMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun spendGoalFunds(
        goal: com.example.data.model.FinancialGoalEntity,
        amount: Double,
        title: String,
        note: String = "",
        dateEpochMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            // 1. Kurangi saldo tersimpan sasaran tanpa menembus angka di bawah nol
            val updatedSaved = (goal.currentSavedAmount - amount).coerceAtLeast(0.0)
            repository.updateGoal(goal.copy(currentSavedAmount = updatedSaved))

            // 2. Catat pengeluaran tujuan yang terisolasi khusus di dalam sasaran ini
            repository.insertTransaction(
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    title = title.trim().ifEmpty { "Pengeluaran ${goal.name}" },
                    amount = amount,
                    categoryName = "Pengeluaran Tujuan",
                    wallet = "Tujuan: ${goal.name}",
                    note = note.trim(),
                    dateEpochMillis = dateEpochMillis,
                    isEncrypted = _isE2eeActive.value
                )
            )
            triggerAutoSnapshot()
        }
    }

    fun deleteGoal(goal: com.example.data.model.FinancialGoalEntity) {
        viewModelScope.launch {
            // Hapus sasaran dari database
            repository.deleteGoal(goal)
            // Hapus semua transaksi alokasi/pengeluaran/pengembalian tabungan terkait sasaran ini agar tren pemasukan/pengeluaran bersih
            val allTxs = allTransactions.value
            for (tx in allTxs) {
                if (tx.title.contains(goal.name, ignoreCase = true) ||
                    tx.note.contains(goal.name, ignoreCase = true) ||
                    tx.wallet.contains(goal.name, ignoreCase = true) ||
                    (tx.categoryName == "Alokasi Tabungan" && tx.title.contains(goal.name, ignoreCase = true)) ||
                    (tx.categoryName == "Pengembalian Tabungan" && tx.title.contains(goal.name, ignoreCase = true)) ||
                    (tx.categoryName == "Pengeluaran Tujuan" && (tx.wallet.contains(goal.name, ignoreCase = true) || tx.title.contains(goal.name, ignoreCase = true)))
                ) {
                    repository.deleteTransaction(tx)
                }
            }
            val remainingGoals = allGoals.value.filter { it.id != goal.id }
            if (remainingGoals.isEmpty()) {
                repository.deleteGoalSavingsTransactions()
            }
        }
    }

    // --- CUSTOM CATEGORY ACTIONS ---
    fun addCategory(
        name: String,
        isExpense: Boolean = true,
        iconName: String = "Fastfood",
        colorHex: String = "#10B981",
        monthlyBudgetLimit: Double = 0.0,
        autoCategorizeMatchingExisting: Boolean = true,
        onCategorizedCount: ((Int) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            val entity = com.example.data.model.CustomCategoryEntity(
                name = trimmedName,
                isExpense = isExpense,
                iconName = iconName,
                colorHex = colorHex,
                isDefault = false,
                monthlyBudgetLimit = monthlyBudgetLimit
            )
            repository.insertCategory(entity)
            if (isExpense && monthlyBudgetLimit > 0) {
                // Sesuai permintaan pengguna: jika ada pos spending maka uang yang di saldo kas itu terpotong sesuai nominal
                repository.insertTransaction(
                    TransactionEntity(
                        type = TransactionType.EXPENSE,
                        title = "Alokasi Pos: $trimmedName",
                        amount = monthlyBudgetLimit,
                        categoryName = "Alokasi Pos Spending",
                        wallet = "Utama",
                        note = "Alokasi dana ke pos spending $trimmedName",
                        dateEpochMillis = System.currentTimeMillis()
                    )
                )
                checkAndTriggerBudgetAlert(trimmedName, 0.0)
            }

            // Auto-categorize existing transactions matching this new category
            if (autoCategorizeMatchingExisting) {
                val currentTxs = allTransactions.value
                val updatedCategories = allCategories.value + entity
                var count = 0
                for (tx in currentTxs) {
                    if ((tx.type == TransactionType.EXPENSE) == isExpense) {
                        val best = com.example.util.AutoCategorizer.findBestCategory(tx, updatedCategories)
                        if (best?.name == trimmedName && tx.categoryName != trimmedName) {
                            repository.updateTransactionCategory(tx.id, trimmedName)
                            count++
                        }
                    }
                }
                onCategorizedCount?.invoke(count)
            }
        }
    }

    fun updateCategory(category: com.example.data.model.CustomCategoryEntity) {
        viewModelScope.launch {
            repository.updateCategory(category)
            if (category.isExpense && category.monthlyBudgetLimit > 0) {
                checkAndTriggerBudgetAlert(category.name, 0.0)
            }
        }
    }

    fun updateCategoryBudget(categoryId: Long, newBudgetLimit: Double) {
        viewModelScope.launch {
            repository.updateCategoryBudget(categoryId, newBudgetLimit)
            val cat = allCategories.value.find { it.id == categoryId }
            if (cat != null && newBudgetLimit > 0) {
                checkAndTriggerBudgetAlert(cat.name, 0.0)
            }
        }
    }

    fun triggerBudgetAlertTest(context: Context, category: com.example.data.model.CustomCategoryEntity) {
        val budgetLimit = if (category.monthlyBudgetLimit > 0) category.monthlyBudgetLimit else 1000000.0
        val simulatedSpent = budgetLimit * 0.85 // Simulasi pengeluaran 85%
        val percentage = 85.0
        val alert = CategoryBudgetAlert(
            categoryId = category.id,
            categoryName = category.name,
            monthlyBudgetLimit = budgetLimit,
            currentSpent = simulatedSpent,
            percentage = percentage,
            isOverBudget = false,
            remainingBudget = budgetLimit - simulatedSpent,
            iconName = category.iconName,
            colorHex = category.colorHex
        )
        val success = NotificationHelper.sendBudgetAlertNotification(
            context,
            category.name,
            simulatedSpent,
            budgetLimit,
            percentage
        )
        _lastBudgetAlertEvent.value = alert
        val msg = if (success) "Notifikasi Peringatan Anggaran (>80%) terkirim!" else "Izin notifikasi diperlukan untuk menampilkan di status bar"
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    /**
     * Move a single transaction to a new category.
     */
    fun moveTransactionCategory(transactionId: Long, newCategoryName: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.updateTransactionCategory(transactionId, newCategoryName)
            onComplete?.invoke()
        }
    }

    /**
     * Batch move all transactions from one category to another.
     */
    fun moveTransactionsBetweenCategories(
        sourceCategoryName: String,
        targetCategoryName: String,
        onComplete: ((Int) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val movedCount = repository.moveTransactionsCategory(sourceCategoryName, targetCategoryName)
            onComplete?.invoke(movedCount)
        }
    }

    /**
     * Automatically categorizes existing transactions into the most fitting custom or default category.
     * Can either categorize all suggested proposals or a specific subset.
     */
    fun autoCategorizeTransactions(
        specificProposals: List<Pair<TransactionEntity, com.example.data.model.CustomCategoryEntity>>? = null,
        onComplete: ((Int) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val categories = allCategories.value
            val transactions = allTransactions.value

            val proposalsToApply = specificProposals ?: com.example.util.AutoCategorizer.getAutoCategorizeProposals(
                transactions,
                categories
            )

            var appliedCount = 0
            for ((tx, targetCategory) in proposalsToApply) {
                if (tx.categoryName != targetCategory.name) {
                    repository.updateTransactionCategory(tx.id, targetCategory.name)
                    appliedCount++
                }
            }
            onComplete?.invoke(appliedCount)
        }
    }

    fun deleteCategory(
        category: com.example.data.model.CustomCategoryEntity,
        targetCategoryForExistingTxs: String? = null
    ) {
        viewModelScope.launch {
            if (!targetCategoryForExistingTxs.isNullOrBlank()) {
                repository.moveTransactionsCategory(category.name, targetCategoryForExistingTxs)
            }
            repository.deleteCategory(category)
            // Bersihkan transaksi alokasi/pengembalian terkait pos spending ini agar tren tetap bersih
            val allTxs = allTransactions.value
            for (tx in allTxs) {
                if ((tx.categoryName == "Alokasi Pos Spending" || tx.categoryName == "Pengembalian Pos Spending") &&
                    tx.title.contains(category.name, ignoreCase = true)
                ) {
                    repository.deleteTransaction(tx)
                }
            }
        }
    }

    /**
     * Buat pos spending baru dan potong saldo kas utama sesuai nominal alokasi.
     */
    fun createSpendingPos(
        name: String,
        initialAllocation: Double,
        iconName: String = "Fastfood",
        colorHex: String = "#10B981"
    ) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            val entity = com.example.data.model.CustomCategoryEntity(
                name = trimmedName,
                isExpense = true,
                iconName = iconName,
                colorHex = colorHex,
                isDefault = false,
                monthlyBudgetLimit = initialAllocation
            )
            repository.insertCategory(entity)

            // Sesuai permintaan: jika ada pos spending maka uang yang di saldo kas itu terpotong sesuai nominal
            if (initialAllocation > 0) {
                repository.insertTransaction(
                    TransactionEntity(
                        type = TransactionType.EXPENSE,
                        title = "Alokasi Pos: $trimmedName",
                        amount = initialAllocation,
                        categoryName = "Alokasi Pos Spending",
                        wallet = "Utama",
                        note = "Alokasi dana ke pos spending $trimmedName",
                        dateEpochMillis = System.currentTimeMillis()
                    )
                )
                checkAndTriggerBudgetAlert(trimmedName, 0.0)
            }
        }
    }

    /**
     * Tambah nominal di pos spending yang diambilkan dari saldo kas utama.
     */
    fun allocateFundsToSpendingPos(category: com.example.data.model.CustomCategoryEntity, amountToAdd: Double) {
        if (amountToAdd <= 0) return
        viewModelScope.launch {
            val updated = category.copy(monthlyBudgetLimit = category.monthlyBudgetLimit + amountToAdd)
            repository.updateCategory(updated)

            // Catat pengeluaran alokasi sehingga saldo kas utama terpotong sesuai nominal tambahan
            repository.insertTransaction(
                TransactionEntity(
                    type = TransactionType.EXPENSE,
                    title = "Tambah Dana: ${category.name}",
                    amount = amountToAdd,
                    categoryName = "Alokasi Pos Spending",
                    wallet = "Utama",
                    note = "Penambahan alokasi dana pos spending ${category.name} dari saldo kas",
                    dateEpochMillis = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Masukkan sisa uang pos spending kembali ke saldo kas utama.
     */
    fun returnRemainingSpendingToCash(
        category: com.example.data.model.CustomCategoryEntity,
        remainingAmount: Double,
        onComplete: () -> Unit = {}
    ) {
        if (remainingAmount <= 0) return
        viewModelScope.launch {
            // Catat pemasukan pengembalian sisa sehingga saldo kas utama bertambah
            repository.insertTransaction(
                TransactionEntity(
                    type = TransactionType.INCOME,
                    title = "Pengembalian Sisa: ${category.name}",
                    amount = remainingAmount,
                    categoryName = "Pengembalian Pos Spending",
                    wallet = "Utama",
                    note = "Pengembalian sisa dana pos spending ${category.name} ke saldo kas",
                    dateEpochMillis = System.currentTimeMillis()
                )
            )

            // Sesuaikan batas pos spending agar sisa dana menjadi 0
            val newLimit = (category.monthlyBudgetLimit - remainingAmount).coerceAtLeast(0.0)
            repository.updateCategory(category.copy(monthlyBudgetLimit = newLimit))
            onComplete()
        }
    }

    /**
     * Masukkan semua sisa uang dari seluruh pos spending kembali ke saldo kas utama.
     */
    fun returnAllRemainingSpendingToCash(
        categories: List<com.example.data.model.CustomCategoryEntity>,
        allTxs: List<TransactionEntity>,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val expenseCats = categories.filter { it.isExpense && it.name != "Alokasi Pos Spending" && it.name != "Alokasi Tabungan" }
            for (cat in expenseCats) {
                val spent = allTxs.filter {
                    it.type == TransactionType.EXPENSE &&
                    (it.categoryName.equals(cat.name, ignoreCase = true) || it.wallet.equals("Pos: ${cat.name}", ignoreCase = true))
                }.sumOf { it.amount }
                val sisa = cat.monthlyBudgetLimit - spent
                if (sisa > 0) {
                    repository.insertTransaction(
                        TransactionEntity(
                            type = TransactionType.INCOME,
                            title = "Pengembalian Sisa: ${cat.name}",
                            amount = sisa,
                            categoryName = "Pengembalian Pos Spending",
                            wallet = "Utama",
                            note = "Pengembalian sisa dana pos spending ${cat.name} ke saldo kas",
                            dateEpochMillis = System.currentTimeMillis()
                        )
                    )
                    repository.updateCategory(cat.copy(monthlyBudgetLimit = spent))
                }
            }
            onComplete()
        }
    }

    /**
     * Restore missing default categories (Food, Transport, Bills, Entertainment, Shopping, etc.)
     */
    fun restoreDefaultCategories(onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val restoredCount = repository.restoreDefaultCategories()
            onComplete?.invoke(restoredCount)
        }
    }

    // --- EXPORT ACTIONS ---
    fun exportReport(context: Context, isPdf: Boolean, monthName: String): File {
        val txList = allTransactions.value
        val assetList = allAssets.value

        return if (isPdf) {
            ReportExporter.generatePdf(context, monthName, txList, assetList)
        } else {
            ReportExporter.generateExcelCsv(context, monthName, txList, assetList)
        }
    }

    companion object {
        /**
         * Factory method to obtain or create a [FinVestaViewModel] scoped to a given [ViewModelStoreOwner].
         * This allows easy instantiation from Activities, Fragments, or NavBackStackEntry.
         */
        fun create(
            owner: ViewModelStoreOwner,
            application: Application
        ): FinVestaViewModel {
            val factory = FinVestaViewModelFactory(application)
            return ViewModelProvider(owner, factory)[FinVestaViewModel::class.java]
        }
    }
}

/**
 * ViewModelProvider.Factory for instantiating [FinVestaViewModel] with application parameter.
 */
class FinVestaViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinVestaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinVestaViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

/**
 * Composable helper function that provides a [FinVestaViewModel] scoped to the current or specified [ViewModelStoreOwner].
 */
@Composable
fun rememberFinVestaViewModel(
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }
): FinVestaViewModel {
    val context = LocalContext.current.applicationContext as Application
    return androidx.lifecycle.viewmodel.compose.viewModel(
        viewModelStoreOwner = viewModelStoreOwner,
        factory = FinVestaViewModelFactory(context)
    )
}
