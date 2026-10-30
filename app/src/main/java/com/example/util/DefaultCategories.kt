package com.example.util

import com.example.data.model.CustomCategoryEntity

/**
 * Daftar Kategori Default Bawaan yang Dapat Disesuaikan (Customizable Default Categories)
 * untuk FinVesta Finance Management App.
 *
 * Mencakup kategori pengeluaran umum:
 * - Makanan & Minuman
 * - Transportasi & Kendaraan
 * - Hiburan & Rekreasi
 * - Tagihan & Utilitas
 * - Belanja & Kebutuhan
 * Serta kategori penunjang penting lainnya:
 * - Kesehatan & Medis
 * - Pendidikan & Edukasi
 * - Tempat Tinggal & Rumah
 * - Olahraga & Kebugaran
 * - Lain-lain & Tak Terduga
 *
 * Dan kategori pendapatan standar:
 * - Gaji Pokok
 * - Bonus & Insentif
 * - Usaha & Bisnis
 * - Investasi & Dividen
 * - Pendapatan Lainnya
 */
object DefaultCategories {

    val DEFAULT_EXPENSE_CATEGORIES = listOf(
        CustomCategoryEntity(
            name = "Makanan & Minuman",
            isExpense = true,
            iconName = "Restaurant",
            colorHex = "#EF4444",
            isDefault = true,
            monthlyBudgetLimit = 0.0
        ),
        CustomCategoryEntity(
            name = "Transportasi",
            isExpense = true,
            iconName = "DirectionsCar",
            colorHex = "#F97316",
            isDefault = true,
            monthlyBudgetLimit = 0.0
        ),
        CustomCategoryEntity(
            name = "Belanja",
            isExpense = true,
            iconName = "ShoppingCart",
            colorHex = "#EC4899",
            isDefault = true,
            monthlyBudgetLimit = 0.0
        ),
        CustomCategoryEntity(
            name = "Tagihan & Utilitas",
            isExpense = true,
            iconName = "Receipt",
            colorHex = "#3B82F6",
            isDefault = true,
            monthlyBudgetLimit = 0.0
        ),
        CustomCategoryEntity(
            name = "Hiburan",
            isExpense = true,
            iconName = "Theaters",
            colorHex = "#8B5CF6",
            isDefault = true,
            monthlyBudgetLimit = 0.0
        ),
        CustomCategoryEntity(
            name = "Kesehatan & Medis",
            isExpense = true,
            iconName = "LocalHospital",
            colorHex = "#10B981",
            isDefault = true,
            monthlyBudgetLimit = 0.0
        ),
        CustomCategoryEntity(
            name = "Pendidikan & Kursus",
            isExpense = true,
            iconName = "School",
            colorHex = "#06B6D4",
            isDefault = true,
            monthlyBudgetLimit = 0.0
        ),
        CustomCategoryEntity(
            name = "Tempat Tinggal",
            isExpense = true,
            iconName = "Home",
            colorHex = "#6366F1",
            isDefault = true,
            monthlyBudgetLimit = 0.0
        ),
        CustomCategoryEntity(
            name = "Olahraga & Kebugaran",
            isExpense = true,
            iconName = "FitnessCenter",
            colorHex = "#14B8A6",
            isDefault = true,
            monthlyBudgetLimit = 0.0
        ),
        CustomCategoryEntity(
            name = "Alokasi Tabungan",
            isExpense = true,
            iconName = "Savings",
            colorHex = "#10B981",
            isDefault = true,
            monthlyBudgetLimit = 0.0
        ),
        CustomCategoryEntity(
            name = "Dari Tabungan",
            isExpense = true,
            iconName = "AccountBalanceWallet",
            colorHex = "#06B6D4",
            isDefault = true,
            monthlyBudgetLimit = 0.0
        ),
        CustomCategoryEntity(
            name = "Lain-lain",
            isExpense = true,
            iconName = "Build",
            colorHex = "#64748B",
            isDefault = true,
            monthlyBudgetLimit = 0.0
        )
    )

    val DEFAULT_INCOME_CATEGORIES = listOf(
        CustomCategoryEntity(
            name = "Gaji Pokok",
            isExpense = false,
            iconName = "AttachMoney",
            colorHex = "#10B981",
            isDefault = true,
            monthlyBudgetLimit = 0.0
        ),
        CustomCategoryEntity(
            name = "Bonus & Insentif",
            isExpense = false,
            iconName = "TrendingUp",
            colorHex = "#06B6D4",
            isDefault = true,
            monthlyBudgetLimit = 0.0
        ),
        CustomCategoryEntity(
            name = "Usaha & Bisnis",
            isExpense = false,
            iconName = "Work",
            colorHex = "#3B82F6",
            isDefault = true,
            monthlyBudgetLimit = 0.0
        ),
        CustomCategoryEntity(
            name = "Investasi & Dividen",
            isExpense = false,
            iconName = "Savings",
            colorHex = "#8B5CF6",
            isDefault = true,
            monthlyBudgetLimit = 0.0
        ),
        CustomCategoryEntity(
            name = "Pendapatan Lainnya",
            isExpense = false,
            iconName = "CardGiftcard",
            colorHex = "#EC4899",
            isDefault = true,
            monthlyBudgetLimit = 0.0
        )
    )

    val ALL_DEFAULTS = DEFAULT_EXPENSE_CATEGORIES + DEFAULT_INCOME_CATEGORIES
}
