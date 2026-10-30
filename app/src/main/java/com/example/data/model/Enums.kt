package com.example.data.model

enum class TransactionType {
    INCOME,
    EXPENSE
}

enum class IncomeCategory(val displayName: String, val iconName: String) {
    GAJI("Gaji Pokok", "payments"),
    SHOPEEFOOD("ShopeeFood / Gig", "delivery_dining"),
    FREELANCE("Freelance & Proyek", "laptop_mac"),
    BISNIS("Bisnis & Dagang", "storefront"),
    DIVIDEN("Dividen & Bunga", "trending_up"),
    BONUS("Bonus & THR", "card_giftcard"),
    LAIN_LAIN("Pendapatan Lain-lain", "account_balance_wallet")
}

enum class ExpenseCategory(val displayName: String, val iconName: String) {
    MAKANAN("Makanan & Minuman", "restaurant"),
    TRANSPORT("Transport & Bensin", "directions_car"),
    BELANJA("Belanja Kebutuhan", "shopping_cart"),
    TAGIHAN("Tagihan & Utilitas", "receipt_long"),
    HIBURAN("Hiburan & Langganan", "movie"),
    KESEHATAN("Kesehatan & Obat", "medical_services"),
    EDUKASI("Pendidikan & Buku", "school"),
    INVESTASI_KELUAR("Alokasi Investasi", "savings"),
    LAIN_LAIN("Pengeluaran Lainnya", "more_horiz")
}

enum class AssetType(val displayName: String, val iconName: String, val shortName: String = displayName) {
    SAHAM("Saham (IDX / US)", "show_chart", "Saham"),
    KRIPTO("Kripto (BTC/ETH)", "currency_bitcoin", "Kripto"),
    REKSA_DANA("Reksa Dana", "pie_chart", "Reksa Dana"),
    EMAS("Emas & Logam Mulia", "monetization_on", "Emas"),
    PROPERTI("Properti & Tanah", "home", "Properti"),
    DEPOSITO_P2P("Deposito & P2P", "savings", "Deposito"),
    KAS("Kas / Valas", "attach_money", "Kas")
}

enum class ReminderStatus {
    PENDING,
    PAID
}

enum class ReminderFrequency(val displayName: String) {
    SEKALI("Satu Kali"),
    BULANAN("Setiap Bulan"),
    MINGGUAN("Setiap Minggu"),
    TAHUNAN("Setiap Tahun")
}
