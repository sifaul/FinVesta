package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.AssetType
import com.example.data.model.ReminderFrequency
import com.example.data.model.ReminderStatus
import com.example.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType =
        try { TransactionType.valueOf(value) } catch (e: Exception) { TransactionType.EXPENSE }

    @TypeConverter
    fun fromAssetType(type: AssetType): String = type.name

    @TypeConverter
    fun toAssetType(value: String): AssetType =
        try { AssetType.valueOf(value) } catch (e: Exception) { AssetType.SAHAM }

    @TypeConverter
    fun fromReminderStatus(status: ReminderStatus): String = status.name

    @TypeConverter
    fun toReminderStatus(value: String): ReminderStatus =
        try { ReminderStatus.valueOf(value) } catch (e: Exception) { ReminderStatus.PENDING }

    @TypeConverter
    fun fromReminderFrequency(freq: ReminderFrequency): String = freq.name

    @TypeConverter
    fun toReminderFrequency(value: String): ReminderFrequency =
        try { ReminderFrequency.valueOf(value) } catch (e: Exception) { ReminderFrequency.BULANAN }
}
