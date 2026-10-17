package com.example.data.model

/**
 * Convenience reference and factory for Room Entity 'Investment' structuring local data storage.
 * Maps directly to [InvestmentAssetEntity].
 */
fun createInvestment(
    name: String,
    assetType: AssetType,
    units: Double,
    unitLabel: String = "Unit",
    avgBuyPrice: Double,
    currentPrice: Double = avgBuyPrice,
    platform: String = "Bibit / Ajaib",
    note: String = ""
): Investment {
    return Investment(
        name = name,
        assetType = assetType,
        units = units,
        unitLabel = unitLabel,
        avgBuyPrice = avgBuyPrice,
        currentPrice = currentPrice,
        platform = platform,
        note = note,
        updatedAt = System.currentTimeMillis()
    )
}
