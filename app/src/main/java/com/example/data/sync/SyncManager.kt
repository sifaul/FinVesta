package com.example.data.sync

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ConnectedDevice(
    val id: String,
    val deviceName: String,
    val platform: String, // Android, iOS, Web, MacOS
    val isCurrentDevice: Boolean = false,
    val lastActiveMillis: Long = System.currentTimeMillis(),
    val ipAddress: String = "192.168.1.104"
)

enum class SyncState {
    CONNECTED,
    SYNCING,
    OFFLINE,
    ERROR
}

class SyncManager {
    private val _syncState = MutableStateFlow(SyncState.CONNECTED)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _deviceList = MutableStateFlow(
        listOf(
            ConnectedDevice("DEV-01", "Perangkat Ini (Android Phone)", "Android", isCurrentDevice = true),
            ConnectedDevice("DEV-02", "Samsung Galaxy Tab S9", "Android Tablet", lastActiveMillis = System.currentTimeMillis() - 120_000),
            ConnectedDevice("DEV-03", "MacBook Pro M3 (Web Client)", "Web Dashboard", lastActiveMillis = System.currentTimeMillis() - 360_000)
        )
    )
    val deviceList: StateFlow<List<ConnectedDevice>> = _deviceList.asStateFlow()

    private val _realtimeLatencyMs = MutableStateFlow(24)
    val realtimeLatencyMs: StateFlow<Int> = _realtimeLatencyMs.asStateFlow()

    val localDeviceId: String = "FIN-NODE-9942A"
    val pairingCode: String = "842-193"

    suspend fun performSync(): Boolean {
        _syncState.value = SyncState.SYNCING
        delay(1200) // Realistic secure handshake & payload exchange
        _lastSyncTimestamp.value = System.currentTimeMillis()
        _realtimeLatencyMs.value = (18..35).random()
        _syncState.value = SyncState.CONNECTED
        return true
    }
}
