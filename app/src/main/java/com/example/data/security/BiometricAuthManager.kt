package com.example.data.security

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import com.example.util.NotificationHelper

enum class BiometricStatus(val label: String, val isReady: Boolean) {
    AVAILABLE("Sensor Biometrik Siap (Sidik Jari / Wajah)", true),
    NOT_ENROLLED("Biometrik belum didaftarkan di pengaturan HP", false),
    NO_HARDWARE("Perangkat tidak memiliki sensor biometrik", false),
    HARDWARE_UNAVAILABLE("Sensor biometrik sedang tidak tersedia", false),
    SECURITY_UPDATE_REQUIRED("Pembaruan keamanan diperlukan untuk biometrik", false),
    UNKNOWN("Status sensor biometrik tidak diketahui", false)
}

enum class BiometricModality(val label: String, val hasFace: Boolean, val hasFingerprint: Boolean) {
    FINGERPRINT_AND_FACE("Sidik Jari & Pengenalan Wajah", true, true),
    FINGERPRINT_ONLY("Pemindai Sidik Jari", false, true),
    FACE_ONLY("Pengenalan Wajah (Face Unlock)", true, false),
    GENERIC_BIOMETRIC("Sensor Biometrik (Wajah / Sidik Jari)", true, true)
}

object BiometricAuthManager {

    private const val PREFS_NAME = "finvesta_biometric_prefs"
    private const val KEY_BIOMETRIC_ENABLED = "pref_biometric_lock_enabled"
    private const val KEY_AUTO_LOCK_BACKGROUND = "pref_auto_lock_background_enabled"
    private const val KEY_BACKUP_PIN = "pref_backup_security_pin"
    private const val KEY_RECOVERY_EMAIL = "pref_recovery_security_email"
    private const val KEY_SECURITY_QUESTION = "pref_recovery_security_question"
    private const val KEY_SECURITY_ANSWER = "pref_recovery_security_answer"
    private const val KEY_RECOVERY_OTP = "pref_temp_recovery_otp"
    private const val KEY_RECOVERY_OTP_TIMESTAMP = "pref_temp_recovery_otp_time"

    private const val KEY_FAILED_PIN_ATTEMPTS = "pref_failed_pin_attempts"
    private const val KEY_LOCKOUT_UNTIL = "pref_lockout_until"
    private const val KEY_FAILED_OTP_ATTEMPTS = "pref_failed_otp_attempts"
    private const val MAX_ATTEMPTS = 5
    private const val LOCKOUT_DURATION_MS = 300_000L // 5 menit lockout

    private const val DEFAULT_PIN = "123456"
    private const val DEFAULT_RECOVERY_EMAIL = "sifaularif3@gmail.com"
    private const val DEFAULT_SECURITY_QUESTION = "Di kota mana Anda dilahirkan?"
    private const val DEFAULT_SECURITY_ANSWER = ""

    @Volatile
    var isPromptActive: Boolean = false
        private set

    fun isLockedOut(context: Context): Boolean {
        val lockoutUntil = getPrefs(context).getLong(KEY_LOCKOUT_UNTIL, 0L)
        return System.currentTimeMillis() < lockoutUntil
    }

    fun getRemainingLockoutSeconds(context: Context): Long {
        val lockoutUntil = getPrefs(context).getLong(KEY_LOCKOUT_UNTIL, 0L)
        val remainingMs = lockoutUntil - System.currentTimeMillis()
        return if (remainingMs > 0) remainingMs / 1000 else 0L
    }

    fun isBiometricEnabled(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_BIOMETRIC_ENABLED, enabled) }
    }

    fun isAutoLockOnBackgroundEnabled(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.getBoolean(KEY_AUTO_LOCK_BACKGROUND, true)
    }

    fun setAutoLockOnBackgroundEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(KEY_AUTO_LOCK_BACKGROUND, enabled) }
    }

    fun setAutoLockOnBackground(context: Context, enabled: Boolean) = setAutoLockOnBackgroundEnabled(context, enabled)

    @Suppress("DEPRECATION")
    @SuppressLint("InlinedApi")
    fun getAvailableBiometricModalities(context: Context): BiometricModality {
        val pm = context.packageManager
        val hasFingerprint = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) ||
                pm.hasSystemFeature("android.hardware.fingerprint")
        val hasFace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pm.hasSystemFeature(PackageManager.FEATURE_FACE) ||
                    pm.hasSystemFeature("android.hardware.biometrics.face")
        } else {
            pm.hasSystemFeature("android.hardware.biometrics.face")
        }
        val hasIris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pm.hasSystemFeature(PackageManager.FEATURE_IRIS)
        } else {
            false
        }

        return when {
            hasFingerprint && (hasFace || hasIris) -> BiometricModality.FINGERPRINT_AND_FACE
            hasFace && !hasFingerprint -> BiometricModality.FACE_ONLY
            hasFingerprint -> BiometricModality.FINGERPRINT_ONLY
            else -> BiometricModality.GENERIC_BIOMETRIC
        }
    }

    fun getBackupPin(context: Context): String {
        val pin = getPrefs(context).getString(KEY_BACKUP_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
        return if (pin.length == 6 && pin.all { it.isDigit() }) pin else DEFAULT_PIN
    }

    fun setBackupPin(context: Context, newPin: String) {
        getPrefs(context).edit { putString(KEY_BACKUP_PIN, newPin) }
    }

    fun verifyPin(context: Context, enteredPin: String): Boolean {
        val prefs = getPrefs(context)
        if (isLockedOut(context)) {
            return false
        }

        val currentPin = getBackupPin(context)
        return if (currentPin == enteredPin) {
            prefs.edit {
                putInt(KEY_FAILED_PIN_ATTEMPTS, 0)
                putLong(KEY_LOCKOUT_UNTIL, 0L)
            }
            true
        } else {
            val failed = prefs.getInt(KEY_FAILED_PIN_ATTEMPTS, 0) + 1
            prefs.edit {
                putInt(KEY_FAILED_PIN_ATTEMPTS, failed)
                if (failed >= MAX_ATTEMPTS) {
                    putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + LOCKOUT_DURATION_MS)
                }
            }
            false
        }
    }

    fun getRecoveryEmail(context: Context): String {
        return getPrefs(context).getString(KEY_RECOVERY_EMAIL, DEFAULT_RECOVERY_EMAIL) ?: DEFAULT_RECOVERY_EMAIL
    }

    fun setRecoveryEmail(context: Context, email: String) {
        getPrefs(context).edit { putString(KEY_RECOVERY_EMAIL, email.trim()) }
    }

    fun getSecurityQuestion(context: Context): String {
        return getPrefs(context).getString(KEY_SECURITY_QUESTION, DEFAULT_SECURITY_QUESTION) ?: DEFAULT_SECURITY_QUESTION
    }

    fun getSecurityAnswer(context: Context): String {
        return getPrefs(context).getString(KEY_SECURITY_ANSWER, DEFAULT_SECURITY_ANSWER) ?: DEFAULT_SECURITY_ANSWER
    }

    fun setSecurityQuestionAndAnswer(context: Context, question: String, answer: String) {
        getPrefs(context).edit {
            putString(KEY_SECURITY_QUESTION, question.trim())
            putString(KEY_SECURITY_ANSWER, answer.trim())
        }
    }

    fun verifySecurityAnswer(context: Context, answer: String): Boolean {
        val expected = getSecurityAnswer(context).trim()
        if (expected.isEmpty()) return false
        return answer.trim().equals(expected, ignoreCase = true)
    }

    fun generateAndSendRecoveryOtp(context: Context, overrideEmail: String? = null): String {
        val email = overrideEmail?.takeIf { it.isNotBlank() } ?: getRecoveryEmail(context)
        val otp = (100000..999999).random().toString()
        getPrefs(context).edit {
            putString(KEY_RECOVERY_OTP, otp)
            putLong(KEY_RECOVERY_OTP_TIMESTAMP, System.currentTimeMillis())
            putInt(KEY_FAILED_OTP_ATTEMPTS, 0)
        }

        // Kirim notifikasi lokal agar kode OTP langsung muncul di baki notifikasi perangkat
        NotificationHelper.sendSecurityOtpNotification(context, otp, email)
        return otp
    }

    fun verifyRecoveryOtp(context: Context, enteredOtp: String): Boolean {
        val prefs = getPrefs(context)
        val savedOtp = prefs.getString(KEY_RECOVERY_OTP, "") ?: ""
        val savedTime = prefs.getLong(KEY_RECOVERY_OTP_TIMESTAMP, 0L)
        val isExpired = System.currentTimeMillis() - savedTime > 15 * 60 * 1000L // 15 menit

        val failedAttempts = prefs.getInt(KEY_FAILED_OTP_ATTEMPTS, 0)
        if (failedAttempts >= MAX_ATTEMPTS) {
            // Hapus OTP jika sudah melebihi 5x percobaan salah
            prefs.edit {
                remove(KEY_RECOVERY_OTP)
                remove(KEY_RECOVERY_OTP_TIMESTAMP)
                putInt(KEY_FAILED_OTP_ATTEMPTS, 0)
            }
            return false
        }

        val cleanEntered = enteredOtp.trim()
        if (cleanEntered.isNotEmpty() && savedOtp.isNotEmpty() && cleanEntered == savedOtp && !isExpired) {
            // Sukses: One-time use, langsung bersihkan OTP
            prefs.edit {
                remove(KEY_RECOVERY_OTP)
                remove(KEY_RECOVERY_OTP_TIMESTAMP)
                putInt(KEY_FAILED_OTP_ATTEMPTS, 0)
            }
            return true
        }

        // Catat kegagalan
        prefs.edit { putInt(KEY_FAILED_OTP_ATTEMPTS, failedAttempts + 1) }
        return false
    }

    fun resetPin(context: Context, newPin: String): Boolean {
        if (newPin.length == 6 && newPin.all { it.isDigit() }) {
            setBackupPin(context, newPin)
            getPrefs(context).edit {
                remove(KEY_RECOVERY_OTP)
                remove(KEY_RECOVERY_OTP_TIMESTAMP)
            }
            return true
        }
        return false
    }

    fun checkBiometricAvailability(context: Context): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.SECURITY_UPDATE_REQUIRED
            else -> BiometricStatus.UNKNOWN
        }
    }

    fun promptBiometric(
        activity: FragmentActivity,
        title: String = "Autentikasi FinVesta",
        subtitle: String = "Verifikasi sidik jari atau pengenalan wajah untuk mengakses dasbor keuangan",
        negativeButtonText: String = "Gunakan PIN",
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        isPromptActive = true
        val executor = ContextCompat.getMainExecutor(activity)
        val promptInfo = try {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
                .build()
        } catch (e: Exception) {
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .build()
        }

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isPromptActive = false
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    isPromptActive = false
                    onError(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    isPromptActive = false
                    onFailed()
                }
            }
        )

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            isPromptActive = false
            onError(-1, e.localizedMessage ?: "Gagal memuat sensor biometrik")
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
