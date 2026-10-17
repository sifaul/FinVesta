package com.example.data.sync

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Summary outcome of a data restore operation.
 */
data class RestoreSummary(
    val success: Boolean,
    val message: String,
    val transactionsRestored: Int = 0,
    val assetsRestored: Int = 0,
    val remindersRestored: Int = 0,
    val goalsRestored: Int = 0,
    val categoriesRestored: Int = 0
)

/**
 * Manages full database backup and restore for Google Drive, local storage,
 * and cross-device data preservation across updates or app uninstalls.
 */
class BackupRestoreManager(
    private val repository: FinVestaRepository
) {
    companion object {
        private const val TAG = "BackupRestoreManager"
        private const val PREFS_NAME = "finvesta_backup_prefs"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_epoch_millis"
        private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
        private const val SNAPSHOT_FILE_NAME = "FinVesta_AutoBackup_Snapshot.json"

        fun getSuggestedBackupFileName(): String {
            val df = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
            return "FinVesta_Backup_${df.format(Date())}.json"
        }
    }

    /**
     * Serializes all user financial records into a structured JSON string.
     */
    suspend fun generateBackupJson(): String = withContext(Dispatchers.IO) {
        val txs = repository.allTransactions.first()
        val assets = repository.allAssets.first()
        val reminders = repository.allReminders.first()
        val goals = repository.allGoals.first()
        val categories = repository.allCategories.first()

        val root = JSONObject().apply {
            put("appName", "FinVesta")
            put("schemaVersion", 2)
            put("createdAt", System.currentTimeMillis())
            put("createdDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            put("device", "${Build.MANUFACTURER} ${Build.MODEL}")

            // Transactions
            val txArray = JSONArray()
            txs.forEach { tx ->
                txArray.put(JSONObject().apply {
                    put("id", tx.id)
                    put("title", tx.title)
                    put("amount", tx.amount)
                    put("type", tx.type.name)
                    put("categoryName", tx.categoryName)
                    put("dateEpochMillis", tx.dateEpochMillis)
                    put("wallet", tx.wallet)
                    put("note", tx.note)
                })
            }
            put("transactions", txArray)

            // Assets / Portfolio
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
                    put("platform", a.platform)
                    put("note", a.note)
                    put("updatedAt", a.updatedAt)
                })
            }
            put("assets", assetArray)

            // Payment Reminders
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
                    put("paymentAccount", r.paymentAccount)
                    put("note", r.note)
                })
            }
            put("reminders", remArray)

            // Goals
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

            // Categories
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

        root.toString(2)
    }

    /**
     * Writes backup content directly to an OutputStream (e.g. from Google Drive SAF Uri).
     */
    suspend fun writeBackupToStream(
        context: Context,
        outputStream: OutputStream
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonContent = generateBackupJson()
            // Enkripsi seluruh data cadangan menggunakan AES-256-GCM
            val encryptedBackup = E2EEncryptionManager.encrypt(jsonContent)
            outputStream.use { os ->
                os.write(encryptedBackup.toByteArray(Charsets.UTF_8))
                os.flush()
            }
            recordBackupSuccess(context)
            // Also update local snapshot
            saveLocalSnapshot(context)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing backup to stream", e)
            false
        }
    }

    /**
     * Restores records from an InputStream (e.g. selected file from Google Drive or local storage).
     */
    suspend fun restoreFromStream(inputStream: InputStream): RestoreSummary = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val sb = java.lang.StringBuilder()
            var line: String? = reader.readLine()
            while (line != null) {
                sb.append(line)
                line = reader.readLine()
            }
            val raw = sb.toString().trim()
            if (raw.isEmpty()) {
                return@withContext RestoreSummary(false, "Berkas cadangan kosong!")
            }

            // Validasi & Dekripsi berkas cadangan terenkripsi
            val jsonString = if (!raw.startsWith("{") && !raw.startsWith("[")) {
                try {
                    val decrypted = E2EEncryptionManager.decrypt(raw)
                    if (decrypted.startsWith("DEC_ERR:") || decrypted.isEmpty()) {
                        return@withContext RestoreSummary(false, "Integritas data rusak: Berkas cadangan telah dimodifikasi atau kunci enkripsi tidak cocok.")
                    }
                    decrypted
                } catch (e: Exception) {
                    return@withContext RestoreSummary(false, "Gagal mendekripsi berkas cadangan: ${e.localizedMessage}")
                }
            } else {
                raw
            }

            val root = try {
                JSONObject(jsonString)
            } catch (e: Exception) {
                return@withContext RestoreSummary(false, "Format berkas cadangan tidak valid atau rusak.")
            }
            if (!root.has("transactions") && !root.has("assets") && !root.has("reminders")) {
                return@withContext RestoreSummary(false, "Format berkas cadangan tidak valid untuk FinVesta.")
            }

            var txCount = 0
            var assetCount = 0
            var remCount = 0
            var goalCount = 0
            var catCount = 0

            // 1. Categories
            if (root.has("categories")) {
                val catArray = root.getJSONArray("categories")
                val restoredCats = mutableListOf<CustomCategoryEntity>()
                for (i in 0 until catArray.length()) {
                    val c = catArray.getJSONObject(i)
                    restoredCats.add(
                        CustomCategoryEntity(
                            id = c.optLong("id", 0L),
                            name = c.optString("name", "Kategori"),
                            isExpense = c.optBoolean("isExpense", true),
                            colorHex = c.optString("colorHex", "#3B82F6"),
                            iconName = c.optString("iconName", "Category"),
                            monthlyBudgetLimit = c.optDouble("monthlyBudgetLimit", 0.0),
                            isDefault = c.optBoolean("isDefault", false)
                        )
                    )
                }
                if (restoredCats.isNotEmpty()) {
                    repository.insertCategories(restoredCats)
                    catCount = restoredCats.size
                }
            }

            // 2. Transactions
            if (root.has("transactions")) {
                val txArray = root.getJSONArray("transactions")
                val restoredTxs = mutableListOf<TransactionEntity>()
                for (i in 0 until txArray.length()) {
                    val tx = txArray.getJSONObject(i)
                    val typeStr = tx.optString("type", "EXPENSE")
                    val txType = try {
                        TransactionType.valueOf(typeStr)
                    } catch (_: Exception) {
                        TransactionType.EXPENSE
                    }
                    restoredTxs.add(
                        TransactionEntity(
                            id = tx.optLong("id", 0L),
                            title = tx.optString("title", "Transaksi"),
                            amount = tx.optDouble("amount", 0.0),
                            type = txType,
                            categoryName = tx.optString("categoryName", "Lainnya"),
                            dateEpochMillis = tx.optLong("dateEpochMillis", System.currentTimeMillis()),
                            wallet = tx.optString("wallet", "Dompet Utama"),
                            note = tx.optString("note", "")
                        )
                    )
                }
                if (restoredTxs.isNotEmpty()) {
                    repository.insertTransactions(restoredTxs)
                    txCount = restoredTxs.size
                }
            }

            // 3. Assets
            if (root.has("assets")) {
                val assetArray = root.getJSONArray("assets")
                val restoredAssets = mutableListOf<InvestmentAssetEntity>()
                for (i in 0 until assetArray.length()) {
                    val a = assetArray.getJSONObject(i)
                    val typeStr = a.optString("assetType", "KRIPTO")
                    val aType = try {
                        AssetType.valueOf(typeStr)
                    } catch (_: Exception) {
                        AssetType.KRIPTO
                    }
                    restoredAssets.add(
                        InvestmentAssetEntity(
                            id = a.optLong("id", 0L),
                            name = a.optString("name", "Aset"),
                            assetType = aType,
                            units = a.optDouble("units", 0.0),
                            avgBuyPrice = a.optDouble("avgBuyPrice", 0.0),
                            currentPrice = a.optDouble("currentPrice", 0.0),
                            unitLabel = a.optString("unitLabel", "unit"),
                            platform = a.optString("platform", "Bibit / Ajaib"),
                            note = a.optString("note", ""),
                            updatedAt = a.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
                if (restoredAssets.isNotEmpty()) {
                    repository.insertAssets(restoredAssets)
                    assetCount = restoredAssets.size
                }
            }

            // 4. Reminders
            if (root.has("reminders")) {
                val remArray = root.getJSONArray("reminders")
                val restoredRems = mutableListOf<PaymentReminderEntity>()
                for (i in 0 until remArray.length()) {
                    val r = remArray.getJSONObject(i)
                    val freqStr = r.optString("frequency", "BULANAN")
                    val freq = try {
                        ReminderFrequency.valueOf(freqStr)
                    } catch (_: Exception) {
                        ReminderFrequency.BULANAN
                    }
                    val statStr = r.optString("status", "PENDING")
                    val status = try {
                        ReminderStatus.valueOf(statStr)
                    } catch (_: Exception) {
                        ReminderStatus.PENDING
                    }
                    restoredRems.add(
                        PaymentReminderEntity(
                            id = r.optLong("id", 0L),
                            title = r.optString("title", "Pengingat"),
                            amount = r.optDouble("amount", 0.0),
                            dueDateEpochMillis = r.optLong("dueDateEpochMillis", System.currentTimeMillis()),
                            frequency = freq,
                            status = status,
                            category = r.optString("category", "Tagihan"),
                            paymentAccount = r.optString("paymentAccount", "Transfer Bank"),
                            note = r.optString("note", "")
                        )
                    )
                }
                if (restoredRems.isNotEmpty()) {
                    repository.insertReminders(restoredRems)
                    remCount = restoredRems.size
                }
            }

            // 5. Goals
            if (root.has("goals")) {
                val goalArray = root.getJSONArray("goals")
                val restoredGoals = mutableListOf<FinancialGoalEntity>()
                for (i in 0 until goalArray.length()) {
                    val g = goalArray.getJSONObject(i)
                    restoredGoals.add(
                        FinancialGoalEntity(
                            id = g.optLong("id", 0L),
                            name = g.optString("name", "Target"),
                            targetAmount = g.optDouble("targetAmount", 0.0),
                            currentSavedAmount = g.optDouble("currentSavedAmount", 0.0),
                            targetDateEpochMillis = g.optLong("targetDateEpochMillis", System.currentTimeMillis()),
                            category = g.optString("category", "Tabungan"),
                            iconName = g.optString("iconName", "Savings"),
                            colorHex = g.optString("colorHex", "#10B981"),
                            note = g.optString("note", "")
                        )
                    )
                }
                if (restoredGoals.isNotEmpty()) {
                    repository.insertGoals(restoredGoals)
                    goalCount = restoredGoals.size
                }
            }

            val msg = "Pemulihan Berhasil! Dikembalikan: $txCount transaksi, $assetCount aset investasi, $remCount pengingat tagihan, $goalCount target."
            RestoreSummary(
                success = true,
                message = msg,
                transactionsRestored = txCount,
                assetsRestored = assetCount,
                remindersRestored = remCount,
                goalsRestored = goalCount,
                categoriesRestored = catCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring backup", e)
            RestoreSummary(false, "Gagal memulihkan: ${e.localizedMessage ?: "Format tidak cocok"}")
        }
    }

    /**
     * Saves a snapshot file in app's persistent storage so user can recover if needed.
     */
    suspend fun saveLocalSnapshot(context: Context): File? = withContext(Dispatchers.IO) {
        try {
            val json = generateBackupJson()
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(dir, SNAPSHOT_FILE_NAME)
            file.writeText(json, Charsets.UTF_8)
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving local snapshot", e)
            null
        }
    }

    /**
     * Creates a Share Intent so the user can tap "Save to Google Drive" or share the backup file anywhere.
     */
    suspend fun createShareBackupIntent(context: Context): Intent? = withContext(Dispatchers.IO) {
        try {
            val json = generateBackupJson()
            val encryptedBackup = E2EEncryptionManager.encrypt(json)
            val backupFileName = getSuggestedBackupFileName()
            val dir = File(context.cacheDir, "backups").apply { mkdirs() }
            val file = File(dir, backupFileName)
            file.writeText(encryptedBackup, Charsets.UTF_8)

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Cadangan Data FinVesta ($backupFileName)")
                putExtra(Intent.EXTRA_TEXT, "File cadangan data keuangan FinVesta. Simpan file ini ke Google Drive agar data tidak hilang saat update atau ganti perangkat.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating share intent", e)
            null
        }
    }

    /**
     * Creates an Intent to save directly to Google Drive app if installed, or fallback to general share chooser.
     */
    suspend fun createGoogleDriveIntent(context: Context): Intent? = withContext(Dispatchers.IO) {
        try {
            val baseIntent = createShareBackupIntent(context) ?: return@withContext null
            val pm = context.packageManager
            val drivePkg = "com.google.android.apps.docs"
            val isDriveInstalled = try {
                pm.getPackageInfo(drivePkg, 0)
                true
            } catch (e: Exception) {
                false
            }

            if (isDriveInstalled) {
                Intent(Intent.ACTION_SEND).apply {
                    setPackage(drivePkg)
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, baseIntent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
                    putExtra(Intent.EXTRA_SUBJECT, baseIntent.getStringExtra(Intent.EXTRA_SUBJECT))
                    putExtra(Intent.EXTRA_TEXT, baseIntent.getStringExtra(Intent.EXTRA_TEXT))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                baseIntent.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating Google Drive intent", e)
            null
        }
    }

    fun hasLocalSnapshot(context: Context): Boolean {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, SNAPSHOT_FILE_NAME)
        return file.exists() && file.length() > 0
    }

    suspend fun restoreFromLocalSnapshot(context: Context): RestoreSummary = withContext(Dispatchers.IO) {
        try {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(dir, SNAPSHOT_FILE_NAME)
            if (!file.exists() || file.length() == 0L) {
                return@withContext RestoreSummary(false, "Tidak ada berkas cadangan snapshot lokal ditemukan.")
            }
            file.inputStream().use { stream ->
                restoreFromStream(stream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed restoring local snapshot", e)
            RestoreSummary(false, "Gagal memulihkan snapshot lokal: ${e.localizedMessage}")
        }
    }

    fun getLastBackupTime(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_BACKUP_TIME, 0L)
    }

    fun isAutoBackupEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_BACKUP_ENABLED, true)
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled)
            .apply()
    }

    private fun recordBackupSuccess(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_BACKUP_TIME, System.currentTimeMillis())
            .apply()
    }
}
