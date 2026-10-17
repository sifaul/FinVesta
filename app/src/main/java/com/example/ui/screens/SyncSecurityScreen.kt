package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.fragment.app.FragmentActivity
import android.widget.Toast
import com.example.data.security.BiometricAuthManager
import com.example.data.security.BiometricStatus
import com.example.data.security.DatabaseKeyManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.E2EEncryptionManager
import com.example.data.sync.ConnectedDevice
import com.example.data.sync.SyncState
import com.example.ui.theme.Amber500
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Rose500
import com.example.util.DateUtils

@Composable
fun SyncSecurityScreen(
    syncState: SyncState,
    lastSyncTimestamp: Long,
    deviceList: List<ConnectedDevice>,
    latencyMs: Int,
    localDeviceId: String,
    pairingCode: String,
    isE2eeActive: Boolean,
    keyFingerprint: String,
    appThemeMode: AppThemeMode,
    connectedEmail: String = "",
    cloudSyncStatus: com.example.data.sync.CloudSyncStatus = com.example.data.sync.CloudSyncStatus.Idle,
    lastCloudBackupTime: Long = 0L,
    isAutoCloudSyncEnabled: Boolean = true,
    lastBackupTime: Long = 0L,
    isAutoBackupEnabled: Boolean = true,
    onBackupToGoogleDrive: () -> Unit = {},
    onBackupViaStoragePicker: () -> Unit = {},
    onRestoreFromGoogleDrive: () -> Unit = {},
    onRestoreFromLocalSnapshot: () -> Unit = {},
    onShareBackup: () -> Unit = {},
    onToggleAutoBackup: (Boolean) -> Unit = {},
    onConnectEmail: (String, Context) -> Unit = { _, _ -> },
    onDisconnectEmail: (Context) -> Unit = {},
    onBackupToCloudNow: (Context) -> Unit = {},
    onRestoreFromCloudNow: (Context) -> Unit = {},
    onToggleAutoCloudSync: (Boolean) -> Unit = {},
    onToggleE2ee: () -> Unit,
    onSyncNow: (Context) -> Unit,
    onChangeThemeMode: (AppThemeMode) -> Unit,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit,
    onClearAllData: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showBackupChoiceDialog by remember { mutableStateOf(false) }
    var showRestoreChoiceDialog by remember { mutableStateOf(false) }
    var showConfirmClearDialog by remember { mutableStateOf(false) }
    var testPlaintext by remember { mutableStateOf("Catatan Gaji & Investasi Rahasia FinVesta") }
    var showCryptoTester by remember { mutableStateOf(false) }
    var isBiometricEnabled by remember { mutableStateOf(BiometricAuthManager.isBiometricEnabled(context)) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    val sqlcipherFingerprint = remember { DatabaseKeyManager.getPassphraseFingerprint(context) }
    val biometricStatus = remember { BiometricAuthManager.checkBiometricAvailability(context) }
    val modality = remember { BiometricAuthManager.getAvailableBiometricModalities(context) }
    var isAutoLockEnabled by remember { mutableStateOf(BiometricAuthManager.isAutoLockOnBackgroundEnabled(context)) }

    val encryptedPreview = remember(testPlaintext, isE2eeActive) {
        if (isE2eeActive) E2EEncryptionManager.encrypt(testPlaintext) else "Enkripsi Nonaktif"
    }
    val decryptedPreview = remember(encryptedPreview) {
        if (isE2eeActive) E2EEncryptionManager.decrypt(encryptedPreview) else testPlaintext
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pusat Keamanan & Enkripsi",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Card A: Autentikasi Biometrik (Sidik Jari / Wajah)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("biometric_security_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Emerald500.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (modality.hasFace && !modality.hasFingerprint) Icons.Default.Face else Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = Emerald500,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Autentikasi Biometrik", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(modality.label, fontSize = 11.sp, color = Emerald500, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = {
                                isBiometricEnabled = it
                                BiometricAuthManager.setBiometricEnabled(context, it)
                                Toast.makeText(
                                    context,
                                    if (it) "Kunci Biometrik Diaktifkan" else "Kunci Biometrik Dinonaktifkan",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Emerald500),
                            modifier = Modifier.testTag("toggle_biometric_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto-lock when entering background toggle
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kunci Otomatis di Latar Belakang",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = isAutoLockEnabled,
                            onCheckedChange = {
                                isAutoLockEnabled = it
                                BiometricAuthManager.setAutoLockOnBackgroundEnabled(context, it)
                                Toast.makeText(
                                    context,
                                    if (it) "Auto-lock latar belakang aktif" else "Auto-lock latar belakang nonaktif",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Emerald500),
                            modifier = Modifier.testTag("toggle_autolock_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Actions: Test Biometric & Manage PIN
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val act = context.findFragmentActivity()
                                if (act != null) {
                                    BiometricAuthManager.promptBiometric(
                                        activity = act,
                                        title = "Uji Sensor Biometrik",
                                        subtitle = "Pindai sidik jari atau pengenalan wajah Anda",
                                        negativeButtonText = "Tutup",
                                        onSuccess = {
                                            Toast.makeText(context, "Verifikasi Biometrik Berhasil!", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { _, err ->
                                            Toast.makeText(context, "Uji Biometrik: $err", Toast.LENGTH_SHORT).show()
                                        },
                                        onFailed = {
                                            Toast.makeText(context, "Biometrik tidak cocok!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    Toast.makeText(context, "Sensor: ${biometricStatus.label}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                            modifier = Modifier.weight(1f).testTag("test_biometric_button")
                        ) {
                            Icon(
                                imageVector = if (modality.hasFace && !modality.hasFingerprint) Icons.Default.Face else Icons.Default.Fingerprint,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Uji Biometrik", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { showChangePinDialog = true },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).testTag("change_backup_pin_button")
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PIN Cadangan", fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Card B: SQLCipher 256-bit Local Database Encryption
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("sqlcipher_encryption_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Cyan500.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Storage, contentDescription = null, tint = Cyan500, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Enkripsi Database Room", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("SQLCipher 256-bit AES", fontSize = 11.sp, color = Cyan500, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Cyan500.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "TERENKRIPSI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Cyan500,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Algorithm & Key Fingerprint Details
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Cyan500, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Algoritma: 256-bit AES-CBC (SQLCipher)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Emerald500, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Fingerprint: $sqlcipherFingerprint",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Emerald500,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. End-to-End Encryption (E2EE) Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Emerald500.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Emerald500, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Enkripsi End-to-End (E2EE)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("AES-256 GCM", fontSize = 11.sp, color = Emerald500, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Switch(
                            checked = isE2eeActive,
                            onCheckedChange = { onToggleE2ee() },
                            colors = SwitchDefaults.colors(checkedThumbColor = Emerald500),
                            modifier = Modifier.testTag("toggle_e2ee_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Key Fingerprint Display
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = Amber500, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Fingerprint Kunci", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = keyFingerprint,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Toggle Live Cryptography Tester
                    Text(
                        text = if (showCryptoTester) "Sembunyikan Pengujian Kriptografi ▲" else "Uji Enkripsi Data Langsung ▼",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { showCryptoTester = !showCryptoTester }
                    )

                    AnimatedVisibility(visible = showCryptoTester) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = testPlaintext,
                                onValueChange = { testPlaintext = it },
                                label = { Text("Teks Sampel Finansial") },
                                singleLine = true,
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Ciphertext Terenkripsi (Base64 + IV):", fontSize = 10.sp, color = Amber500, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = encryptedPreview,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Hasil Dekripsi Kembali:", fontSize = 10.sp, color = Emerald500, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = decryptedPreview,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1.4. Google Drive Cloud Backup & Anti-Hilang Protection Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("google_drive_backup_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Emerald500.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = Emerald500,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Cadangan Google Drive",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Emerald500)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Proteksi Anti-Hilang Aktif",
                                        fontSize = 11.sp,
                                        color = Emerald500,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status Box: Penjelasan saat Update / Uninstall
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Emerald500,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Saat Update Aplikasi: Seluruh data transaksi, aset, dan kategori otomatis tersimpan di database ponsel tanpa hilang.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = Cyan500,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Saat Uninstall / Ganti HP: Android Auto-Backup Google Drive memulihkan database secara otomatis pada akun Google yang sama.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Waktu Cadangan Terakhir
                    Text(
                        text = if (lastBackupTime > 0) {
                            "Cadangan terakhir: ${DateUtils.formatDateTime(lastBackupTime)}"
                        } else {
                            "Belum ada berkas cadangan manual dibuat"
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Buttons: Cadangkan ke Google Drive & Pulihkan dari Google Drive
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showBackupChoiceDialog = true },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                            modifier = Modifier.weight(1f).testTag("backup_to_gdrive_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cadangkan ke Drive", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { showRestoreChoiceDialog = true },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).testTag("restore_from_gdrive_button")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pulihkan dari Drive", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Opsi Bagikan / Simpan File Cadangan
                    OutlinedButton(
                        onClick = onShareBackup,
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                        modifier = Modifier.fillMaxWidth().testTag("share_backup_file_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Simpan File Cadangan ke Google Drive / Kirim Berkas",
                            fontSize = 11.sp,
                            lineHeight = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Auto-Backup Snapshot Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Snapshot Otomatis",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Otomatis perbarui file cadangan lokal setiap kali ada transaksi baru",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isAutoBackupEnabled,
                            onCheckedChange = { onToggleAutoBackup(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Emerald500),
                            modifier = Modifier.testTag("toggle_auto_backup_switch")
                        )
                    }
                }
            }

            // Dialog Pilihan Cadangkan ke Google Drive
            if (showBackupChoiceDialog) {
                AlertDialog(
                    onDismissRequest = { showBackupChoiceDialog = false },
                    icon = {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Emerald500, modifier = Modifier.size(32.dp))
                    },
                    title = {
                        Text("Cadangkan ke Google Drive", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Pilih metode pencadangan yang Anda inginkan:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Option 1: Direct Google Drive
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Emerald500.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showBackupChoiceDialog = false
                                        onBackupToGoogleDrive()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = Emerald500, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Simpan Langsung ke Google Drive",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Emerald500,
                                            lineHeight = 15.sp
                                        )
                                        Text(
                                            text = "Buka aplikasi Google Drive untuk memilih akun & folder tujuan",
                                            fontSize = 11.sp,
                                            lineHeight = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Option 2: File Picker (SAF)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showBackupChoiceDialog = false
                                        onBackupViaStoragePicker()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = Cyan500, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Pilih Folder / Pengelola Berkas (SAF)",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            lineHeight = 15.sp
                                        )
                                        Text(
                                            text = "Simpan berkas .json ke Dokumen atau folder Google Drive Anda",
                                            fontSize = 11.sp,
                                            lineHeight = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showBackupChoiceDialog = false }) {
                            Text("Tutup")
                        }
                    }
                )
            }

            // Dialog Pilihan Pulihkan dari Google Drive / Cadangan
            if (showRestoreChoiceDialog) {
                AlertDialog(
                    onDismissRequest = { showRestoreChoiceDialog = false },
                    icon = {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Cyan500, modifier = Modifier.size(32.dp))
                    },
                    title = {
                        Text("Pulihkan Data Cadangan", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Pilih sumber data yang ingin Anda pulihkan:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Option 1: File from Google Drive / Storage
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Cyan500.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showRestoreChoiceDialog = false
                                        onRestoreFromGoogleDrive()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Cyan500, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Pilih Berkas dari Google Drive / HP",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Cyan500,
                                            lineHeight = 15.sp
                                        )
                                        Text(
                                            text = "Buka berkas .json cadangan terenkripsi yang tersimpan",
                                            fontSize = 11.sp,
                                            lineHeight = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Option 2: Restore from Local Auto-Snapshot
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showRestoreChoiceDialog = false
                                        onRestoreFromLocalSnapshot()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = null, tint = Amber500, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Pulihkan dari Snapshot Lokal",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            lineHeight = 15.sp
                                        )
                                        Text(
                                            text = "Pulihkan langsung dari arsip lokal otomatis ponsel",
                                            fontSize = 11.sp,
                                            lineHeight = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showRestoreChoiceDialog = false }) {
                            Text("Tutup")
                        }
                    }
                )
            }
        }

        // 1.5. Encrypted Cloud Backup & Real-Time Sync via Firestore (Email Connection)
        item {
            var inputEmailText by remember { mutableStateOf("") }
            var showEmailDialog by remember { mutableStateOf(false) }

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("firestore_cloud_sync_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Cyan500.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = Cyan500,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = "Cadangan Cloud & Firestore",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (connectedEmail.isNotBlank()) "Terkoneksi: $connectedEmail" else "Belum terhubung email",
                                    fontSize = 11.sp,
                                    color = if (connectedEmail.isNotBlank()) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (connectedEmail.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (connectedEmail.isBlank()) {
                            Button(
                                onClick = { showEmailDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("connect_email_button")
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sambungkan",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onDisconnectEmail(context) },
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("disconnect_email_button")
                            ) {
                                Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(14.dp), tint = Rose500)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Putus", fontSize = 11.sp, color = Rose500, maxLines = 1, softWrap = false)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (connectedEmail.isNotBlank()) {
                        // Email Connected state with status and backup actions
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Emerald500,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Email Aktif: $connectedEmail",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (cloudSyncStatus is com.example.data.sync.CloudSyncStatus.Syncing) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Cyan500)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = if (lastCloudBackupTime > 0) {
                                        "Terakhir dicadangkan: ${DateUtils.formatDateTime(lastCloudBackupTime)}"
                                    } else {
                                        "Belum ada cadangan awan tersimpan"
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Auto-sync toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Sinkronisasi Otomatis Antar Perangkat",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Switch(
                                        checked = isAutoCloudSyncEnabled,
                                        onCheckedChange = { onToggleAutoCloudSync(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Cyan500
                                        ),
                                        modifier = Modifier.testTag("auto_cloud_sync_switch")
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Backup & Restore Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onBackupToCloudNow(context) },
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
                                        modifier = Modifier.weight(1f).testTag("backup_to_cloud_button"),
                                        enabled = cloudSyncStatus !is com.example.data.sync.CloudSyncStatus.Syncing
                                    ) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Cadangkan", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }

                                    OutlinedButton(
                                        onClick = { onRestoreFromCloudNow(context) },
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f).testTag("restore_from_cloud_button"),
                                        enabled = cloudSyncStatus !is com.example.data.sync.CloudSyncStatus.Syncing
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Pulihkan", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    }
                                }
                            }
                        }
                    } else {
                        // Notice when email is not yet connected
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Amber500.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Amber500, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Untuk memakai fitur cadangkan ke cloud ini, Anda harus menghubungkan email pribadi terlebih dahulu.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Dialog Sambungkan Email Pribadi
            if (showEmailDialog) {
                var emailInput by remember { mutableStateOf(connectedEmail.ifBlank { "" }) }
                var isEmailError by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { showEmailDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = Cyan500)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Hubungkan Email Pribadi", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Masukkan email pribadi Anda untuk mengaktifkan sinkronisasi real-time Firestore dan pencadangan terenkripsi E2EE antar perangkat.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = {
                                    emailInput = it
                                    isEmailError = false
                                },
                                label = { Text("Alamat Email Pribadi") },
                                placeholder = { Text("nama@gmail.com") },
                                singleLine = true,
                                isError = isEmailError,
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth().testTag("email_input_field")
                            )
                            if (isEmailError) {
                                Text("Format email tidak valid.", fontSize = 11.sp, color = Rose500)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val clean = emailInput.trim()
                                if (clean.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(clean).matches()) {
                                    onConnectEmail(clean, context)
                                    showEmailDialog = false
                                } else {
                                    isEmailError = true
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
                            modifier = Modifier.testTag("confirm_connect_email_btn")
                        ) {
                            Text("Sambungkan", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showEmailDialog = false },
                            shape = CircleShape,
                            modifier = Modifier.testTag("cancel_connect_email_btn")
                        ) {
                            Text("Batal")
                        }
                    }
                )
            }
        }

        // 2. Real-Time Cross-Device Synchronization Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Cyan500.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = Cyan500, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text("Sinkronisasi Real-Time", fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (syncState == SyncState.CONNECTED) Emerald500 else Amber500)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (syncState == SyncState.CONNECTED) "Terhubung ($latencyMs ms)" else "Sinkronisasi...",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (syncState == SyncState.CONNECTED) Emerald500 else Amber500,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { onSyncNow(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan500),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("sync_now_button")
                        ) {
                            if (syncState == SyncState.SYNCING) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.surface, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.surface)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sinkronkan",
                                color = MaterialTheme.colorScheme.surface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Node Info & Pairing Code
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Node Perangkat", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(localDeviceId, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Kode Pasangan Cepat", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(pairingCode, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Cyan500)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Perangkat Terhubung (${deviceList.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(6.dp))

                    // Connected Devices list
                    deviceList.forEach { dev ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (dev.isCurrentDevice) Icons.Default.PhoneAndroid else Icons.Default.Devices,
                                    contentDescription = null,
                                    tint = if (dev.isCurrentDevice) Emerald500 else Cyan500,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(dev.deviceName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${dev.platform} • ${dev.ipAddress}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (dev.isCurrentDevice) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = Emerald500.copy(alpha = 0.2f)) {
                                        Text("Aktif", fontSize = 10.sp, color = Emerald500, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Theme & Mode Gelap Settings Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Amber500.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Brightness4, contentDescription = null, tint = Amber500, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Mode Tampilan Aplikasi", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Pilih mode terang, gelap (malam), atau ikuti sistem perangkat", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemeMode.values().forEach { mode ->
                            FilterChip(
                                selected = appThemeMode == mode,
                                onClick = { onChangeThemeMode(mode) },
                                label = { Text(mode.title, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f).testTag("theme_chip_${mode.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // 4. Data Export & Report Center
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Emerald500.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = Emerald500, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Pusat Ekspor Laporan Otomatis", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Dokumen resmi PDF dan Spreadsheet Excel / CSV", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onExportPdf,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                            modifier = Modifier.weight(1f).testTag("export_pdf_button")
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.surface)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ekspor PDF", color = MaterialTheme.colorScheme.surface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onExportExcel,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Amber500),
                            modifier = Modifier.weight(1f).testTag("export_excel_button")
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.surface)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ekspor Excel", color = MaterialTheme.colorScheme.surface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 7. Reset & Clean Data Card (Test User Mode)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("clean_data_test_mode_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Rose500.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Rose500, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Pembersihan Data Uji (Test User)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Mulai dari lembar bersih (kosong)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Fitur ini menghapus seluruh transaksi, aset investasi, tagihan, dan target tabungan untuk pengujian mandiri dari awal.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { showConfirmClearDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose500),
                        border = BorderStroke(1.dp, Rose500.copy(alpha = 0.6f)),
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().testTag("btn_clean_all_data")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp), tint = Rose500)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bersihkan Semua Data Aplikasi", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // APK Download & Export Information Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("apk_download_info_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Emerald500.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = Emerald500, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Download APK FinVesta v1.2.2", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Instalasi Mandiri di Perangkat Android", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Untuk mengunduh APK FinVesta versi terbaru (v1.2.2), buka menu Settings / Titik Tiga di panel pojok kanan atas AI Studio Build, lalu pilih 'Export' ➔ 'Download APK' atau 'Download ZIP'.",
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            Toast.makeText(
                                context,
                                "Klik icon menu di pojok kanan atas AI Studio ➔ Export ➔ Download APK",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_show_download_apk_guide")
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Petunjuk Unduh APK v1.2.2", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "FinVesta v1.2.2",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Aplikasi Keuangan Pribadi & Driver Terenkripsi",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Rose500)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Konfirmasi Pembersihan Data", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus seluruh data transaksi, portofolio investasi, pengingat tagihan, dan target keuangan?\n\nAplikasi akan bersih sepenuhnya siap digunakan oleh user uji.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showConfirmClearDialog = false
                        Toast.makeText(context, "Seluruh data berhasil dibersihkan!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500),
                    shape = CircleShape
                ) {
                    Text("Ya, Bersihkan Data")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showConfirmClearDialog = false },
                    shape = CircleShape
                ) {
                    Text("Batal")
                }
            }
        )
    }

    if (showChangePinDialog) {
        var pinInput by remember { mutableStateOf(BiometricAuthManager.getBackupPin(context)) }
        var emailInput by remember { mutableStateOf(BiometricAuthManager.getRecoveryEmail(context)) }
        var questionInput by remember { mutableStateOf(BiometricAuthManager.getSecurityQuestion(context)) }
        var answerInput by remember { mutableStateOf(BiometricAuthManager.getSecurityAnswer(context)) }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = Emerald500)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pengaturan PIN & Pemulihan", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "PIN dan opsi pemulihan digunakan untuk mengamankan dan mereset akses aplikasi jika lupa PIN.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                                pinInput = it
                                isError = false
                            }
                        },
                        label = { Text("PIN Cadangan (6 Digit)") },
                        singleLine = true,
                        isError = isError,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().testTag("new_pin_input_field")
                    )
                    if (isError) {
                        Text("PIN harus terdiri dari tepat 6 digit angka.", fontSize = 11.sp, color = Rose500)
                    }

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email Pemulihan OTP") },
                        singleLine = true,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().testTag("recovery_email_field")
                    )

                    OutlinedTextField(
                        value = questionInput,
                        onValueChange = { questionInput = it },
                        label = { Text("Pertanyaan Keamanan") },
                        singleLine = true,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().testTag("recovery_question_field")
                    )

                    OutlinedTextField(
                        value = answerInput,
                        onValueChange = { answerInput = it },
                        label = { Text("Jawaban Pemulihan") },
                        singleLine = true,
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().testTag("recovery_answer_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length == 6 && pinInput.all { it.isDigit() }) {
                            BiometricAuthManager.setBackupPin(context, pinInput)
                            BiometricAuthManager.setRecoveryEmail(context, emailInput)
                            BiometricAuthManager.setSecurityQuestionAndAnswer(context, questionInput, answerInput)
                            showChangePinDialog = false
                            Toast.makeText(context, "PIN & Opsi Pemulihan Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                        } else {
                            isError = true
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showChangePinDialog = false },
                    shape = CircleShape
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is android.content.ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
