package com.example.util

import com.example.data.model.CustomCategoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType

/**
 * Intelligent Auto-Categorization Engine.
 * Analyzes transaction titles and notes using rich semantic keywords (Indonesian & English)
 * and dynamic similarity with user-defined custom categories to suggest or automatically assign
 * the most accurate category.
 */
object AutoCategorizer {

    // Default keyword clusters for standard and custom categories
    private val EXPENSE_KEYWORD_MAP: Map<String, List<String>> = mapOf(
        "makanan" to listOf(
            "makan", "minum", "resto", "restoran", "cafe", "kafe", "kopi", "coffee",
            "starbucks", "janji jiwa", "kulo", "gofood", "grabfood", "shopeefood", "mcd",
            "kfc", "burger", "pizza", "warung", "padang", "mie", "bakso", "ayam", "sate",
            "nasi", "snack", "roti", "sarapan", "lunch", "dinner", "kantin", "angkringan",
            "jus", "boba", "teh", "es", "chattime", "mixue", "hokben", "solaria"
        ),
        "transportasi" to listOf(
            "trans", "bensin", "spbu", "pertamina", "shell", "pertalite", "pertamax", "solar",
            "grab", "gojek", "gocar", "goride", "maxim", "taxi", "taksi", "toll", "tol",
            "parkir", "kereta", "krl", "mrt", "lrt", "busway", "transjakarta", "garuda",
            "lion", "kai", "service", "oli", "ban", "motor", "mobil", "angkot", "travel"
        ),
        "belanja" to listOf(
            "belanja", "shopping", "indomaret", "alfamart", "supermarket", "hypermart",
            "tokopedia", "shopee", "lazada", "tiktok", "mall", "pakaian", "baju", "celana",
            "sepatu", "skincare", "kosmetik", "pasar", "sayur", "sabun", "shampoo", "dapur",
            "sembako", "uniqlo", "zara", "h&m", "miniso", "ikea", "ace"
        ),
        "tagihan" to listOf(
            "tagihan", "listrik", "pln", "air", "pdam", "wifi", "indihome", "firstmedia",
            "biznet", "telkom", "pulsa", "kuota", "paket data", "telkomsel", "xl", "indosat",
            "tri", "smartfren", "bpjs", "iuran", "cicilan", "kontrakan", "pbb", "pajak",
            "asuransi", "sewa", "ipl", "kartu kredit", "paylater"
        ),
        "hiburan" to listOf(
            "hiburan", "netflix", "spotify", "youtube", "bioskop", "cinema", "xxi", "cgv",
            "game", "steam", "playstation", "hobi", "konser", "tiket", "liburan", "hotel",
            "villa", "wisata", "rekreasi", "disney", "karaoke", "play", "nonton", "voucher"
        ),
        "kesehatan" to listOf(
            "kesehatan", "obat", "apotek", "kimia farma", "k24", "dokter", "rs", "rumah sakit",
            "klinik", "periksa", "dental", "gigi", "vitamin", "lab", "darah", "kacamata",
            "optik", "rapid", "vaksin", "rontgen", "fisioterapi", "halodoc", "alodokter"
        ),
        "edukasi" to listOf(
            "edukasi", "buku", "gramedia", "kursus", "les", "udemy", "coursera", "sekolah",
            "spp", "kuliah", "ukt", "seminar", "workshop", "webinar", "pelatihan", "ujian",
            "sertifikasi", "toefl", "ielts", "alat tulis", "fotokopi"
        ),
        "investasi" to listOf(
            "investasi", "invest", "saham", "reksadana", "crypto", "bibit", "ajaib", "bareksa",
            "binance", "tokocrypto", "emas", "antam", "obligasi", "sbn", "ori", "deposito",
            "pluang", "stockbit"
        ),
        "keluarga" to listOf(
            "keluarga", "anak", "susu", "pampers", "mainan", "rumah", "perabot", "renovasi",
            "laundry", "cuci", "setrika", "asisten", "art", "babysitter"
        )
    )

    private val INCOME_KEYWORD_MAP: Map<String, List<String>> = mapOf(
        "gaji" to listOf("gaji", "salary", "upah", "payroll", "bulanan", "kantor", "penghasilan"),
        "bonus" to listOf("bonus", "thr", "insentif", "reward", "komisi", "tantiem"),
        "bisnis" to listOf("bisnis", "usaha", "omset", "penjualan", "jual", "toko", "untung", "profit", "dagang"),
        "freelance" to listOf("freelance", "proyek", "project", "desain", "koding", "jasa", "konsultasi"),
        "investasi" to listOf("dividen", "bunga", "yield", "imbal", "capital gain", "kupon", "profit investasi"),
        "hadiah" to listOf("hadiah", "gift", "cashback", "giveaway", "angpao", "reimbursement", "klaim", "kembalian")
    )

    /**
     * Finds the best matching CustomCategoryEntity for a given transaction.
     */
    fun findBestCategory(
        transaction: TransactionEntity,
        categories: List<CustomCategoryEntity>
    ): CustomCategoryEntity? {
        val isExpense = transaction.type == TransactionType.EXPENSE
        val relevantCategories = categories.filter { it.isExpense == isExpense }
        if (relevantCategories.isEmpty()) return null

        val textToMatch = "${transaction.title} ${transaction.note}".lowercase()
        return scoreAndFindBest(textToMatch, relevantCategories, isExpense)
    }

    /**
     * Suggests a category dynamically as the user types a transaction title.
     */
    fun suggestCategory(
        title: String,
        note: String = "",
        isExpense: Boolean,
        categories: List<CustomCategoryEntity>
    ): CustomCategoryEntity? {
        if (title.isBlank() && note.isBlank()) return null
        val relevantCategories = categories.filter { it.isExpense == isExpense }
        if (relevantCategories.isEmpty()) return null

        val textToMatch = "$title $note".lowercase()
        return scoreAndFindBest(textToMatch, relevantCategories, isExpense)
    }

    /**
     * Evaluates all transactions against available categories, returning a list of
     * proposed category updates for transactions that are currently miscategorized or generic.
     */
    fun getAutoCategorizeProposals(
        transactions: List<TransactionEntity>,
        categories: List<CustomCategoryEntity>
    ): List<Pair<TransactionEntity, CustomCategoryEntity>> {
        val proposals = mutableListOf<Pair<TransactionEntity, CustomCategoryEntity>>()

        for (tx in transactions) {
            val bestCategory = findBestCategory(tx, categories) ?: continue
            // If the best category is different from current category, propose change
            val isCurrentMatch = isCategoryNameMatch(tx.categoryName, bestCategory.name)
            if (!isCurrentMatch) {
                proposals.add(tx to bestCategory)
            }
        }
        return proposals
    }

    private fun scoreAndFindBest(
        textToMatch: String,
        categories: List<CustomCategoryEntity>,
        isExpense: Boolean
    ): CustomCategoryEntity? {
        val keywordMap = if (isExpense) EXPENSE_KEYWORD_MAP else INCOME_KEYWORD_MAP

        var bestCategory: CustomCategoryEntity? = null
        var maxScore = 0

        for (category in categories) {
            var score = 0
            val catNameLower = category.name.lowercase()

            // 1. Direct name match (e.g. title contains category name)
            val nameTokens = catNameLower.split(" ", "&", "/", "-", ",").map { it.trim() }.filter { it.length >= 3 }
            for (token in nameTokens) {
                if (textToMatch.contains(token)) {
                    score += 15 // High confidence for category name words
                }
            }

            // 2. Keyword cluster match
            for ((keyCluster, keywords) in keywordMap) {
                if (catNameLower.contains(keyCluster)) {
                    for (kw in keywords) {
                        if (containsWord(textToMatch, kw)) {
                            score += 10
                        }
                    }
                }
            }

            // 3. Exact word boundary check for category name
            if (containsWord(textToMatch, catNameLower)) {
                score += 25
            }

            if (score > maxScore) {
                maxScore = score
                bestCategory = category
            }
        }

        // Only return if confidence threshold is met
        return if (maxScore >= 10) bestCategory else null
    }

    private fun containsWord(source: String, word: String): Boolean {
        if (word.isBlank() || source.isBlank()) return false
        if (source.contains(word)) {
            // Check word boundary or substring of sufficient length
            if (word.length <= 3) {
                val pattern = "\\b${Regex.escape(word)}\\b".toRegex()
                return pattern.containsMatchIn(source)
            }
            return true
        }
        return false
    }

    private fun isCategoryNameMatch(cat1: String, cat2: String): Boolean {
        if (cat1.equals(cat2, ignoreCase = true)) return true
        val clean1 = cat1.replace("&", "").replace("/", "").replace("-", "").replace(" ", "").lowercase()
        val clean2 = cat2.replace("&", "").replace("/", "").replace("-", "").replace(" ", "").lowercase()
        return clean1.contains(clean2) || clean2.contains(clean1)
    }
}
