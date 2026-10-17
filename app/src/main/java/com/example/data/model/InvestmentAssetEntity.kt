package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing investment assets (Saham, Kripto, Reksa Dana, Emas, Properti, Deposito, etc.)
 * stored in the encrypted SQLCipher Room database.
 */
@Entity(
    tableName = "investment_assets",
    indices = [
        Index(value = ["assetType"]),
        Index(value = ["name"]),
        Index(value = ["updatedAt"])
    ]
)
data class InvestmentAssetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String, // e.g. "BBCA", "Bitcoin (BTC)", "Reksa Dana Pasar Uang Sucor", "Emas Antam"
    val assetType: AssetType,
    val units: Double, // e.g. 500 lembar / 0.05 BTC / 25 gram
    val unitLabel: String = "Unit", // e.g. Lembar, Lot, Gram, Koin
    val avgBuyPrice: Double, // Average buy price per unit in IDR
    val currentPrice: Double, // Current market price per unit in IDR
    val platform: String = "Bibit / Ajaib", // e.g. Ajaib, Indodax, Pegadaian, Pluang
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalInvested: Double
        get() = units * avgBuyPrice

    val currentValue: Double
        get() = units * currentPrice

    val unrealizedPnl: Double
        get() = currentValue - totalInvested

    val unrealizedPnlPercent: Double
        get() = if (totalInvested > 0) (unrealizedPnl / totalInvested) * 100.0 else 0.0

    /**
     * Category representing the asset type display name.
     */
    val category: String
        get() = assetType.displayName

    /**
     * Date of last record or valuation update.
     */
    val date: Long
        get() = updatedAt

    /**
     * Total valuation amount in IDR.
     */
    val amount: Double
        get() = currentValue
}

/**
 * Typealias representing 'Investment' Room Entity structuring local investment asset storage.
 */
typealias Investment = InvestmentAssetEntity
