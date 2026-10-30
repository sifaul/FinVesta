package com.example.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {

    val availableIcons: List<Pair<String, ImageVector>> = listOf(
        "Restaurant" to Icons.Default.Restaurant,
        "LocalCafe" to Icons.Default.LocalCafe,
        "DirectionsCar" to Icons.Default.DirectionsCar,
        "LocalGasStation" to Icons.Default.LocalGasStation,
        "DirectionsBus" to Icons.Default.DirectionsBus,
        "ShoppingCart" to Icons.Default.ShoppingCart,
        "Storefront" to Icons.Default.Storefront,
        "Receipt" to Icons.Default.Receipt,
        "ElectricBolt" to Icons.Default.ElectricBolt,
        "WaterDrop" to Icons.Default.WaterDrop,
        "Wifi" to Icons.Default.Wifi,
        "PhoneAndroid" to Icons.Default.PhoneAndroid,
        "Theaters" to Icons.Default.Theaters,
        "Subscriptions" to Icons.Default.Subscriptions,
        "SportsEsports" to Icons.Default.SportsEsports,
        "LocalHospital" to Icons.Default.LocalHospital,
        "School" to Icons.Default.School,
        "FitnessCenter" to Icons.Default.FitnessCenter,
        "Home" to Icons.Default.Home,
        "Pets" to Icons.Default.Pets,
        "Flight" to Icons.Default.Flight,
        "FlightTakeoff" to Icons.Default.FlightTakeoff,
        "Work" to Icons.Default.Work,
        "Savings" to Icons.Default.Savings,
        "TrendingUp" to Icons.AutoMirrored.Filled.TrendingUp,
        "AttachMoney" to Icons.Default.AttachMoney,
        "CreditCard" to Icons.Default.CreditCard,
        "CardGiftcard" to Icons.Default.CardGiftcard,
        "AccountBalanceWallet" to Icons.Default.AccountBalanceWallet,
        "Build" to Icons.Default.Build
    )

    val availableColors: List<Pair<String, Color>> = listOf(
        "#10B981" to Color(0xFF10B981), // Emerald
        "#059669" to Color(0xFF059669), // Dark Emerald
        "#06B6D4" to Color(0xFF06B6D4), // Cyan
        "#0284C7" to Color(0xFF0284C7), // Sky
        "#3B82F6" to Color(0xFF3B82F6), // Blue
        "#6366F1" to Color(0xFF6366F1), // Indigo
        "#8B5CF6" to Color(0xFF8B5CF6), // Purple
        "#A855F7" to Color(0xFFA855F7), // Violet
        "#EC4899" to Color(0xFFEC4899), // Pink
        "#F43F5E" to Color(0xFFF43F5E), // Rose
        "#EF4444" to Color(0xFFEF4444), // Red
        "#F97316" to Color(0xFFF97316), // Orange
        "#F59E0B" to Color(0xFFF59E0B), // Amber
        "#84CC16" to Color(0xFF84CC16), // Lime
        "#14B8A6" to Color(0xFF14B8A6), // Teal
        "#64748B" to Color(0xFF64748B)  // Slate
    )

    fun getIcon(name: String): ImageVector {
        return availableIcons.find { it.first.equals(name, ignoreCase = true) }?.second ?: Icons.Default.Category
    }

    fun inferIconForCategory(categoryName: String): String {
        val lower = categoryName.lowercase()
        return when {
            lower.contains("makan") || lower.contains("food") || lower.contains("kuliner") || lower.contains("kopi") -> "Restaurant"
            lower.contains("trans") || lower.contains("bensin") || lower.contains("mobil") || lower.contains("motor") || lower.contains("ojek") -> "DirectionsCar"
            lower.contains("belanja") || lower.contains("shop") || lower.contains("mall") || lower.contains("pasar") -> "ShoppingCart"
            lower.contains("tagihan") || lower.contains("listrik") || lower.contains("air") || lower.contains("wifi") || lower.contains("pulsa") -> "Receipt"
            lower.contains("hiburan") || lower.contains("nonton") || lower.contains("game") -> "Theaters"
            lower.contains("kesehatan") || lower.contains("obat") || lower.contains("dokter") || lower.contains("rs") -> "LocalHospital"
            lower.contains("didik") || lower.contains("sekolah") || lower.contains("kursus") || lower.contains("kuliah") -> "School"
            lower.contains("olahraga") || lower.contains("gym") || lower.contains("fit") -> "FitnessCenter"
            lower.contains("rumah") || lower.contains("sewa") || lower.contains("kost") -> "Home"
            lower.contains("libur") || lower.contains("hotel") || lower.contains("travel") || lower.contains("tiket") -> "Flight"
            lower.contains("hewan") || lower.contains("pet") -> "Pets"
            lower.contains("invest") || lower.contains("saham") || lower.contains("emas") || lower.contains("crypto") -> "TrendingUp"
            lower.contains("tabung") || lower.contains("alokasi") || lower.contains("target") -> "Savings"
            lower.contains("gaji") || lower.contains("bonus") || lower.contains("freelance") -> "AttachMoney"
            else -> "Category"
        }
    }

    fun parseColor(hex: String): Color {
        return try {
            val clean = hex.removePrefix("#")
            val colorLong = clean.toLong(16)
            if (clean.length == 6) {
                Color(0xFF000000 or colorLong)
            } else {
                Color(colorLong)
            }
        } catch (e: Exception) {
            Color(0xFF10B981)
        }
    }
}
