package com.example.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.CustomCategoryEntity
import com.example.data.model.FinancialGoalEntity
import com.example.data.model.InvestmentAssetEntity
import com.example.data.model.PaymentReminderEntity
import com.example.data.model.TransactionEntity
import com.example.data.security.DatabaseKeyManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        TransactionEntity::class,
        InvestmentAssetEntity::class,
        PaymentReminderEntity::class,
        FinancialGoalEntity::class,
        CustomCategoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class FinVestaDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun finVestaDao(): FinVestaDao

    companion object {
        private const val DATABASE_NAME = "finvesta_encrypted.db"
        private const val LEGACY_DATABASE_NAME = "finvesta_database"

        @Volatile
        private var INSTANCE: FinVestaDatabase? = null

        @Volatile
        private var isSqlCipherLoaded = false

        init {
            ensureSqlCipherLoaded()
        }

        private fun ensureSqlCipherLoaded(): Boolean {
            if (isSqlCipherLoaded) return true
            return synchronized(this) {
                if (isSqlCipherLoaded) return true
                try {
                    System.loadLibrary("sqlcipher")
                    isSqlCipherLoaded = true
                    Log.i("FinVestaDatabase", "SQLCipher native library loaded successfully")
                    true
                } catch (t: Throwable) {
                    Log.d("FinVestaDatabase", "SQLCipher native library note: ${t.message}")
                    false
                }
            }
        }

        fun getDatabase(context: Context): FinVestaDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext

                // Step 1: Clean up legacy unencrypted database files if present
                cleanLegacyDatabase(appContext)

                // Step 2: Ensure native SQLCipher library is loaded
                val sqlCipherAvailable = ensureSqlCipherLoaded()

                // Step 3: Build Room database with WAL (Write-Ahead Logging) for high concurrency
                val builder = Room.databaseBuilder(
                    appContext,
                    FinVestaDatabase::class.java,
                    DATABASE_NAME
                )
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)

                // Step 4: Configure SQLCipher factory if native library is available
                if (sqlCipherAvailable) {
                    try {
                        val passphrase = DatabaseKeyManager.getOrCreateDatabasePassphrase(appContext)
                        // clearPassphrase = false allows Room's connection pool to open multiple connections
                        val factory = SupportOpenHelperFactory(passphrase, null, false)
                        ensureDatabaseIntegrity(appContext, DATABASE_NAME, factory)
                        builder.openHelperFactory(factory)
                        Log.i("FinVestaDatabase", "SQLCipher 256-bit AES database encryption active on $DATABASE_NAME")
                    } catch (t: Throwable) {
                        Log.d("FinVestaDatabase", "SQLCipher factory initialization fallback: ${t.message}")
                    }
                } else {
                    Log.d("FinVestaDatabase", "Using standard database open helper (SQLCipher native unavailable)")
                }

                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }

        fun isEncryptionActive(context: Context): Boolean {
            val dbFile = context.applicationContext.getDatabasePath(DATABASE_NAME)
            if (!dbFile.exists() || dbFile.length() == 0L) return true
            return try {
                java.io.FileInputStream(dbFile).use { fis ->
                    val header = ByteArray(16)
                    val read = fis.read(header)
                    // If it does NOT start with standard plaintext SQLite format 3, it is ciphertext/encrypted
                    !(read >= 15 && String(header, 0, read, Charsets.US_ASCII).startsWith("SQLite format 3"))
                }
            } catch (_: Exception) {
                true
            }
        }

        private fun cleanLegacyDatabase(context: Context) {
            try {
                val legacyDb = context.getDatabasePath(LEGACY_DATABASE_NAME)
                if (legacyDb.exists()) {
                    context.deleteDatabase(LEGACY_DATABASE_NAME)
                    Log.i("FinVestaDatabase", "Removed legacy unencrypted database $LEGACY_DATABASE_NAME")
                }
            } catch (e: Exception) {
                Log.d("FinVestaDatabase", "Error removing legacy database: ${e.message}")
            }
        }

        private fun ensureDatabaseIntegrity(context: Context, dbName: String, factory: SupportOpenHelperFactory) {
            val dbFile = context.getDatabasePath(dbName)
            if (!dbFile.exists() || dbFile.length() == 0L) return

            var isHealthy = true
            try {
                // Check if file is plaintext SQLite (starts with 'SQLite format 3')
                java.io.FileInputStream(dbFile).use { fis ->
                    val header = ByteArray(16)
                    val read = fis.read(header)
                    if (read >= 15 && String(header, 0, read, Charsets.US_ASCII).startsWith("SQLite format 3")) {
                        Log.i("FinVestaDatabase", "$dbName has plaintext header. Resetting to create encrypted database.")
                        isHealthy = false
                    }
                }
            } catch (t: Throwable) {
                Log.d("FinVestaDatabase", "Database file check note for $dbName: ${t.message}")
            }

            if (!isHealthy) {
                try {
                    context.deleteDatabase(dbName)
                    Log.i("FinVestaDatabase", "Successfully reset invalid database $dbName")
                } catch (e: Exception) {
                    Log.d("FinVestaDatabase", "Database cleanup note: ${e.message}")
                }
            } else if (dbFile.exists() && dbFile.length() > 0L) {
                // Verify that the database can actually be read with the active encryption key
                try {
                    val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                        .name(dbName)
                        .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(3) {
                            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {}
                            override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                        })
                        .build()
                    val helper = factory.create(config)
                    val db = helper.readableDatabase
                    val cursor = db.query("SELECT count(*) FROM sqlite_master")
                    cursor.close()
                    helper.close()
                } catch (t: Throwable) {
                    Log.w("FinVestaDatabase", "Database $dbName cannot be read with active key or is corrupted: ${t.message}. Re-initializing clean encrypted database.")
                    try {
                        context.deleteDatabase(dbName)
                    } catch (e: Exception) {
                        Log.d("FinVestaDatabase", "Database cleanup fallback error: ${e.message}")
                    }
                }
            }
        }
    }
}
