package com.example.data.sync

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import android.util.Log
import com.example.data.model.AssetType
import com.example.data.model.CustomCategoryEntity
import com.example.data.model.FinancialGoalEntity
import com.example.data.model.InvestmentAssetEntity
import com.example.data.model.PaymentReminderEntity
import com.example.data.model.ReminderFrequency
import com.example.data.model.ReminderStatus
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.FinVestaRepository
import com.example.data.security.E2EEncryptionManager
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Result state of Cloud Sync / Backup operations.
 */
sealed class CloudSyncStatus {
    object Idle : CloudSyncStatus()
    object Syncing : CloudSyncStatus()
    data class Success(val message: String, val timestamp: Long = System.currentTimeMillis()) : CloudSyncStatus()
    data class Error(val errorMessage: String) : CloudSyncStatus()
}

/**
 * Encrypted Cloud Backup and Real-Time Multi-Device Sync Manager powered by Cloud Firestore.
 * Requires user to link/connect their personal email.
 * All sensitive financial payloads (amounts, categories, wallets, titles, notes, goals, assets)
 * are encrypted with AES-256 GCM (E2EE) before transmitting to Firestore.
 */
class FirestoreSyncService(
    private val context: Context,
    private val repository: FinVestaRepository
) {
    companion object {
        private const val TAG = "FirestoreSyncService"
        private const val PREFS_NAME = "finvesta_firestore_sync_prefs"
        private const val KEY_USER_EMAIL = "connected_user_email"
        private const val KEY_LAST_BACKUP_TIME = "last_cloud_backup_time"
        private const val KEY_AUTO_SYNC_ENABLED = "auto_realtime_sync_enabled"
        private const val COLLECTION_USERS = "finvesta_users"
        private const val DOC_FINANCIAL_VAULT = "encrypted_financial_vault"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _connectedEmail = MutableStateFlow(prefs.getString(KEY_USER_EMAIL, "") ?: "")
    val connectedEmail: StateFlow<String> = _connectedEmail.asStateFlow()

    private val _syncStatus = MutableStateFlow<CloudSyncStatus>(CloudSyncStatus.Idle)
    val syncStatus: StateFlow<CloudSyncStatus> = _syncStatus.asStateFlow()

    private val _lastBackupTime = MutableStateFlow(prefs.getLong(KEY_LAST_BACKUP_TIME, 0L))
    val lastBackupTime: StateFlow<Long> = _lastBackupTime.asStateFlow()

    private val _isAutoSyncEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, true))
    val isAutoSyncEnabled: StateFlow<Boolean> = _isAutoSyncEnabled.asStateFlow()

    private var firestoreListener: ListenerRegistration? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    init {
        if (_connectedEmail.value.isNotBlank() && _isAutoSyncEnabled.value) {
            startRealtimeListener(_connectedEmail.value)
        }
    }

    fun isUserConnected(): Boolean {
        val email = _connectedEmail.value
        return email.isNotBlank() && email.contains("@")
    }

    fun connectEmail(email: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return false
        }
        prefs.edit { putString(KEY_USER_EMAIL, cleanEmail) }
        _connectedEmail.value = cleanEmail

        if (_isAutoSyncEnabled.value) {
            startRealtimeListener(cleanEmail)
        }
        return true
    }

    fun disconnectEmail() {
        stopRealtimeListener()
        prefs.edit { remove(KEY_USER_EMAIL) }
        _connectedEmail.value = ""
        _syncStatus.value = CloudSyncStatus.Idle
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_SYNC_ENABLED, enabled) }
        _isAutoSyncEnabled.value = enabled
        if (enabled && isUserConnected()) {
            startRealtimeListener(_connectedEmail.value)
        } else {
            stopRealtimeListener()
        }
    }

    /**
     * Creates an encrypted backup payload from local Room database and pushes to Firestore.
     */
    suspend fun backupDataToCloud(): CloudSyncStatus = withContext(Dispatchers.IO) {
        val email = _connectedEmail.value
        if (!isUserConnected()) {
            val err = CloudSyncStatus.Error("Silakan sambungkan email pribadi Anda terlebih dahulu untuk mencadangkan.")
            _syncStatus.value = err
            return@withContext err
        }

        _syncStatus.value = CloudSyncStatus.Syncing

        try {
            // 1. Collect all local data
            val transactions = repository.allTransactions.first()
            val assets = repository.allAssets.first()
            val reminders = repository.allReminders.first()
            val goals = repository.allGoals.first()
            val categories = repository.allCategories.first()

            // 2. Serialize to JSON string
            val rootJson = JSONObject().apply {
                put("version", 1)
                put("backedUpAt", System.currentTimeMillis())
                put("userEmail", email)

                // Transactions array
                val txArray = JSONArray()
                transactions.forEach { tx ->
                    txArray.put(JSONObject().apply {
                        put("id", tx.id)
                        put("type", tx.type.name)
                        put("title", tx.title)
                        put("amount", tx.amount)
                        put("categoryName", tx.categoryName)
                        put("wallet", tx.wallet)
                        put("note", tx.note)
                        put("dateEpochMillis", tx.dateEpochMillis)
                    })
                }
                put("transactions", txArray)

                // Assets array
                val assetArray = JSONArray()
                assets.forEach { a ->
                    assetArray.put(JSONObject().apply {
                        put("id", a.id)
                        put("name", a.name)
                        put("assetType", a.assetType.name)
                        put("units", a.units)
                        put("avgBuyPrice", a.avgBuyPrice)
                        put("currentPrice", a.currentPrice)
                        put("unitLabel", a.unitLabel)
                        put("updatedAt", a.updatedAt)
                    })
                }
                put("assets", assetArray)

                // Reminders array
                val remArray = JSONArray()
                reminders.forEach { r ->
                    remArray.put(JSONObject().apply {
                        put("id", r.id)
                        put("title", r.title)
                        put("amount", r.amount)
                        put("dueDateEpochMillis", r.dueDateEpochMillis)
                        put("frequency", r.frequency.name)
                        put("status", r.status.name)
                        put("category", r.category)
                    })
                }
                put("reminders", remArray)

                // Goals array
                val goalArray = JSONArray()
                goals.forEach { g ->
                    goalArray.put(JSONObject().apply {
                        put("id", g.id)
                        put("name", g.name)
                        put("targetAmount", g.targetAmount)
                        put("currentSavedAmount", g.currentSavedAmount)
                        put("targetDateEpochMillis", g.targetDateEpochMillis)
                        put("category", g.category)
                        put("iconName", g.iconName)
                        put("colorHex", g.colorHex)
                        put("note", g.note)
                    })
                }
                put("goals", goalArray)

                // Categories array
                val catArray = JSONArray()
                categories.forEach { c ->
                    catArray.put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("isExpense", c.isExpense)
                        put("colorHex", c.colorHex)
                        put("iconName", c.iconName)
                        put("monthlyBudgetLimit", c.monthlyBudgetLimit)
                        put("isDefault", c.isDefault)
                    })
                }
                put("categories", catArray)
            }

            val plainJsonString = rootJson.toString()

            // 3. Encrypt payload with E2EE (AES-256 GCM)
            val encryptedPayload = E2EEncryptionManager.encrypt(plainJsonString)

            // 4. Send to Firestore under sanitized email path
            val sanitizedEmailDocId = sanitizeEmailToDocId(email)

            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            val firestore = FirebaseFirestore.getInstance()

            val cloudDocData = hashMapOf(
                "userEmail" to email,
                "lastUpdatedMillis" to System.currentTimeMillis(),
                "deviceInfo" to android.os.Build.MODEL,
                "encryptedPayload" to encryptedPayload,
                "transactionCount" to transactions.size,
                "assetCount" to assets.size,
                "keyFingerprint" to E2EEncryptionManager.getKeyFingerprint()
            )

            firestore.collection(COLLECTION_USERS)
                .document(sanitizedEmailDocId)
                .collection("vaults")
                .document(DOC_FINANCIAL_VAULT)
                .set(cloudDocData, SetOptions.merge())
                .await()

            val now = System.currentTimeMillis()
            prefs.edit { putLong(KEY_LAST_BACKUP_TIME, now) }
            _lastBackupTime.value = now

            val successStatus = CloudSyncStatus.Success(
                "Pencadangan terenkripsi berhasil ke Firestore (${transactions.size} transaksi, ${assets.size} aset)!"
            )
            _syncStatus.value = successStatus
            successStatus
        } catch (e: Exception) {
            Log.d(TAG, "Backup status note: ${e.message}")
            val err = CloudSyncStatus.Error(
                if (e.message?.contains("FirebaseApp") == true || e.message?.contains("default FirebaseApp") == true) {
                    "Layanan Firebase belum terkonfigurasi pada perangkat: ${e.localizedMessage}"
                } else {
                    "Gagal mencadangkan: ${e.localizedMessage ?: "Terjadi kesalahan jaringan"}"
                }
            )
            _syncStatus.value = err
            err
        }
    }

    /**
     * Restores data from Firestore encrypted vault into local database.
     */
    suspend fun restoreDataFromCloud(): CloudSyncStatus = withContext(Dispatchers.IO) {
        val email = _connectedEmail.value
        if (!isUserConnected()) {
            val err = CloudSyncStatus.Error("Silakan sambungkan email pribadi Anda terlebih dahulu.")
            _syncStatus.value = err
            return@withContext err
        }

        _syncStatus.value = CloudSyncStatus.Syncing

        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            val firestore = FirebaseFirestore.getInstance()
            val sanitizedEmailDocId = sanitizeEmailToDocId(email)

            val snapshot = firestore.collection(COLLECTION_USERS)
                .document(sanitizedEmailDocId)
                .collection("vaults")
                .document(DOC_FINANCIAL_VAULT)
                .get()
                .await()

            if (!snapshot.exists()) {
                val notFound = CloudSyncStatus.Error("Belum ada data cadangan di cloud untuk email: $email")
                _syncStatus.value = notFound
                return@withContext notFound
            }

            val encryptedPayload = snapshot.getString("encryptedPayload")
            if (encryptedPayload.isNullOrBlank()) {
                val emptyErr = CloudSyncStatus.Error("Data cadangan cloud kosong atau tidak valid.")
                _syncStatus.value = emptyErr
                return@withContext emptyErr
            }

            // Decrypt with E2EE
            val decryptedJson = E2EEncryptionManager.decrypt(encryptedPayload)
            if (decryptedJson.startsWith("DEC_ERR:") || decryptedJson.isBlank()) {
                val decErr = CloudSyncStatus.Error("Gagal mendekripsi payload cloud (kunci enkripsi berbeda).")
                _syncStatus.value = decErr
                return@withContext decErr
            }

            val rootJson = JSONObject(decryptedJson)

            // Parse and merge Transactions
            val txArray = rootJson.optJSONArray("transactions")
            var restoredTxCount = 0
            if (txArray != null) {
                for (i in 0 until txArray.length()) {
                    val obj = txArray.getJSONObject(i)
                    val tx = TransactionEntity(
                        id = obj.optLong("id", 0L),
                        type = runCatching { TransactionType.valueOf(obj.getString("type")) }.getOrDefault(TransactionType.EXPENSE),
                        title = obj.getString("title"),
                        amount = obj.getDouble("amount"),
                        categoryName = obj.getString("categoryName"),
                        wallet = obj.optString("wallet", "Utama"),
                        note = obj.optString("note", ""),
                        dateEpochMillis = obj.getLong("dateEpochMillis"),
                        isEncrypted = true
                    )
                    repository.insertTransaction(tx)
                    restoredTxCount++
                }
            }

            // Parse and merge Assets
            val assetArray = rootJson.optJSONArray("assets")
            var restoredAssetCount = 0
            if (assetArray != null) {
                for (i in 0 until assetArray.length()) {
                    val obj = assetArray.getJSONObject(i)
                    val asset = InvestmentAssetEntity(
                        id = obj.optLong("id", 0L),
                        name = obj.getString("name"),
                        assetType = runCatching { AssetType.valueOf(obj.getString("assetType")) }.getOrDefault(AssetType.SAHAM),
                        units = obj.getDouble("units"),
                        avgBuyPrice = obj.getDouble("avgBuyPrice"),
                        currentPrice = obj.getDouble("currentPrice"),
                        unitLabel = obj.optString("unitLabel", "lembar"),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                    repository.insertAsset(asset)
                    restoredAssetCount++
                }
            }

            // Parse and merge Goals
            val goalArray = rootJson.optJSONArray("goals")
            if (goalArray != null) {
                for (i in 0 until goalArray.length()) {
                    val obj = goalArray.getJSONObject(i)
                    val goal = FinancialGoalEntity(
                        id = obj.optLong("id", 0L),
                        name = obj.optString("name", obj.optString("title", "Tujuan")),
                        targetAmount = obj.getDouble("targetAmount"),
                        currentSavedAmount = obj.optDouble("currentSavedAmount", obj.optDouble("currentAmount", 0.0)),
                        targetDateEpochMillis = obj.getLong("targetDateEpochMillis"),
                        category = obj.optString("category", "Investasi"),
                        iconName = obj.optString("iconName", "Home"),
                        colorHex = obj.optString("colorHex", "#10B981"),
                        note = obj.optString("note", "")
                    )
                    repository.insertGoal(goal)
                }
            }

            // Parse and merge Reminders
            val remArray = rootJson.optJSONArray("reminders")
            if (remArray != null) {
                for (i in 0 until remArray.length()) {
                    val obj = remArray.getJSONObject(i)
                    val reminder = PaymentReminderEntity(
                        id = obj.optLong("id", 0L),
                        title = obj.getString("title"),
                        amount = obj.getDouble("amount"),
                        dueDateEpochMillis = obj.getLong("dueDateEpochMillis"),
                        frequency = runCatching { ReminderFrequency.valueOf(obj.getString("frequency")) }.getOrDefault(ReminderFrequency.BULANAN),
                        status = runCatching { ReminderStatus.valueOf(obj.getString("status")) }.getOrDefault(ReminderStatus.PENDING),
                        category = obj.optString("category", "Tagihan")
                    )
                    repository.insertReminder(reminder)
                }
            }

            val now = System.currentTimeMillis()
            prefs.edit { putLong(KEY_LAST_BACKUP_TIME, now) }
            _lastBackupTime.value = now

            val successMsg = "Pemulihan data selesai: $restoredTxCount transaksi & $restoredAssetCount aset berhasil disinkronkan!"
            val successStatus = CloudSyncStatus.Success(successMsg)
            _syncStatus.value = successStatus
            successStatus
        } catch (e: Exception) {
            Log.d(TAG, "Restore status note: ${e.message}")
            val err = CloudSyncStatus.Error("Gagal memulihkan dari cloud: ${e.localizedMessage ?: "Kesalahan tidak diketahui"}")
            _syncStatus.value = err
            err
        }
    }

    /**
     * Listens for real-time changes on the user's Firestore vault document
     * to keep multiple devices updated in real-time.
     */
    private fun startRealtimeListener(email: String) {
        stopRealtimeListener()
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            val firestore = FirebaseFirestore.getInstance()
            val sanitizedDocId = sanitizeEmailToDocId(email)

            val docRef = firestore.collection(COLLECTION_USERS)
                .document(sanitizedDocId)
                .collection("vaults")
                .document(DOC_FINANCIAL_VAULT)

            firestoreListener = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.d(TAG, "Firestore real-time listen note: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val lastRemoteUpdated = snapshot.getLong("lastUpdatedMillis") ?: 0L
                    val lastLocalBackup = _lastBackupTime.value

                    // Only automatically merge if incoming remote update is newer than local backup
                    if (lastRemoteUpdated > lastLocalBackup + 2000L) {
                        serviceScope.launch {
                            restoreDataFromCloud()
                        }
                    }
                }
            }
            Log.d(TAG, "Started Firestore real-time listener for $email")
        } catch (e: Exception) {
            Log.d(TAG, "Firestore real-time listener note: ${e.localizedMessage}")
        }
    }

    private fun stopRealtimeListener() {
        firestoreListener?.remove()
        firestoreListener = null
    }

    private fun sanitizeEmailToDocId(email: String): String {
        return email.trim().lowercase()
            .replace(".", "_dot_")
            .replace("@", "_at_")
            .replace("+", "_plus_")
    }
}
