package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_categories")
data class CustomCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isExpense: Boolean = true, // Default to expense categories
    val iconName: String = "Fastfood", // Fastfood, DirectionsCar, ShoppingBag, Receipt, Movie, MedicalServices, School, FitnessCenter, LocalCafe, Pets, Flight, Home, Work, TrendingUp, AttachMoney
    val colorHex: String = "#10B981", // Hex string e.g. #10B981, #06B6D4, #F59E0B, #EF4444, #8B5CF6, #EC4899
    val isDefault: Boolean = false,
    val monthlyBudgetLimit: Double = 0.0, // Batas anggaran bulanan pengguna (0.0 = belum diatur)
    val createdAt: Long = System.currentTimeMillis()
)
