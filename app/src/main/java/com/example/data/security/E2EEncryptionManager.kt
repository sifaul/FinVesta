package com.example.data.security

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object E2EEncryptionManager {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12
    private const val DEFAULT_PASSPHRASE = "FinVesta_E2EE_Vault_Key_2026_SecureSync"

    // Derive 256-bit AES key from passphrase
    private fun getSecretKey(passphrase: String = DEFAULT_PASSPHRASE): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(passphrase.toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts plain text using AES-256 GCM.
     * Returns Base64-encoded string containing [12-byte IV + Ciphertext].
     */
    fun encrypt(plainText: String, passphrase: String = DEFAULT_PASSPHRASE): String {
        try {
            val key = getSecretKey(passphrase)
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(IV_LENGTH_BYTE)
            SecureRandom().nextBytes(iv)
            val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)

            cipher.init(Cipher.ENCRYPT_MODE, key, spec)
            val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

            // Prepend IV to ciphertext
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            return "ENC_ERR:${e.localizedMessage}"
        }
    }

    /**
     * Decrypts Base64-encoded string [IV + Ciphertext] using AES-256 GCM.
     */
    fun decrypt(encryptedBase64: String, passphrase: String = DEFAULT_PASSPHRASE): String {
        try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size < IV_LENGTH_BYTE) return ""

            val iv = ByteArray(IV_LENGTH_BYTE)
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTE)

            val cipherTextSize = combined.size - IV_LENGTH_BYTE
            val cipherText = ByteArray(cipherTextSize)
            System.arraycopy(combined, IV_LENGTH_BYTE, cipherText, 0, cipherTextSize)

            val key = getSecretKey(passphrase)
            val cipher = Cipher.getInstance(ALGORITHM)
            val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)

            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val plainBytes = cipher.doFinal(cipherText)
            return String(plainBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            return "DEC_ERR:${e.localizedMessage}"
        }
    }

    /**
     * Generates a human-friendly SHA-256 key fingerprint for UI verification.
     */
    fun getKeyFingerprint(passphrase: String = DEFAULT_PASSPHRASE): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(passphrase.toByteArray(StandardCharsets.UTF_8))
        val sb = StringBuilder()
        for (i in 0 until 8) {
            sb.append(String.format(Locale.US, "%02X", hash[i]))
            if (i % 2 == 1 && i < 7) sb.append(":")
        }
        return "E2EE-SHA256-${sb.toString()}"
    }
}
