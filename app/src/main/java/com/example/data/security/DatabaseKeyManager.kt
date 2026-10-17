package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object DatabaseKeyManager {
    private const val PREFS_NAME = "finvesta_sec_vault"
    private const val KEY_ENCRYPTED_PASSPHRASE = "db_sqlcipher_enc_passphrase_v2"
    private const val KEY_LEGACY_PASSPHRASE = "db_sqlcipher_passphrase_v1"
    private const val KEYSTORE_ALIAS = "finvesta_master_db_key_alias"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val PASSPHRASE_BYTE_SIZE = 32 // 256-bit AES key

    @Synchronized
    fun getOrCreateDatabasePassphrase(context: Context): ByteArray {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 1. Try to read Keystore-encrypted passphrase (v2)
        val encryptedBlobBase64 = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null)
        if (!encryptedBlobBase64.isNullOrEmpty()) {
            try {
                val decrypted = decryptWithKeystore(encryptedBlobBase64)
                if (decrypted != null && decrypted.size == PASSPHRASE_BYTE_SIZE) {
                    return decrypted
                }
            } catch (_: Exception) {}
        }

        // 2. Check for legacy v1 unencrypted passphrase to migrate seamlessly
        val legacyBase64 = prefs.getString(KEY_LEGACY_PASSPHRASE, null)
        val rawPassphrase: ByteArray = if (!legacyBase64.isNullOrEmpty()) {
            try {
                val decoded = Base64.decode(legacyBase64, Base64.NO_WRAP)
                if (decoded.size == PASSPHRASE_BYTE_SIZE) decoded else generateFreshPassphrase()
            } catch (_: Exception) {
                generateFreshPassphrase()
            }
        } else {
            generateFreshPassphrase()
        }

        // 3. Encrypt raw passphrase using hardware-backed Android KeyStore
        try {
            val encryptedBase64 = encryptWithKeystore(rawPassphrase)
            if (encryptedBase64 != null) {
                prefs.edit {
                    putString(KEY_ENCRYPTED_PASSPHRASE, encryptedBase64)
                    remove(KEY_LEGACY_PASSPHRASE) // Wipe unencrypted legacy key
                }
            }
        } catch (_: Exception) {
            // Fallback for environments where KeyStore is unavailable
            val fallbackEncoded = Base64.encodeToString(rawPassphrase, Base64.NO_WRAP)
            prefs.edit { putString(KEY_LEGACY_PASSPHRASE, fallbackEncoded) }
        }

        return rawPassphrase
    }

    private fun generateFreshPassphrase(): ByteArray {
        val randomBytes = ByteArray(PASSPHRASE_BYTE_SIZE)
        SecureRandom().nextBytes(randomBytes)
        return randomBytes
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(KEYSTORE_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    private fun encryptWithKeystore(data: ByteArray): String? {
        return try {
            val masterKey = getOrCreateMasterKey()
            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            cipher.init(Cipher.ENCRYPT_MODE, masterKey)
            val iv = cipher.iv ?: return null
            val cipherText = cipher.doFinal(data)
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun decryptWithKeystore(base64Cipher: String): ByteArray? {
        return try {
            val combined = Base64.decode(base64Cipher, Base64.NO_WRAP)
            if (combined.size <= GCM_IV_LENGTH) return null
            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            val cipherTextLength = combined.size - GCM_IV_LENGTH
            val cipherText = ByteArray(cipherTextLength)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherTextLength)

            val masterKey = getOrCreateMasterKey()
            val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)
            cipher.doFinal(cipherText)
        } catch (e: Exception) {
            null
        }
    }

    fun getPassphraseFingerprint(context: Context): String {
        return try {
            val key = getOrCreateDatabasePassphrase(context)
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(key)
            val sb = StringBuilder()
            for (i in 0 until 8) {
                sb.append(String.format(Locale.US, "%02X", hash[i]))
                if (i % 2 == 1 && i < 7) sb.append(":")
            }
            "SQLCIPHER-${sb.toString()}"
        } catch (_: Exception) {
            "SQLCIPHER-AES256-ACTIVE"
        }
    }
}
