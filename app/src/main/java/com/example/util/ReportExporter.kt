package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt
import com.example.data.model.InvestmentAssetEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    fun generateExcelCsv(
        context: Context,
        monthName: String,
        transactions: List<TransactionEntity>,
        assets: List<InvestmentAssetEntity>
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(reportsDir, "FinVesta_Laporan_${monthName.replace(" ", "_")}_$timeStamp.csv")

        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netSavings = totalIncome - totalExpense
        val totalAssetValuation = assets.sumOf { it.currentValue }

        val sb = StringBuilder()
        sb.append("LAPORAN KEUANGAN FINVESTA - BULAN ").append(monthName.uppercase()).append("\n")
        sb.append("Dicetak pada:,").append(DateUtils.formatFullDate(System.currentTimeMillis())).append("\n\n")

        sb.append("--- RINGKASAN EKSEKUTIF KEUANGAN ---\n")
        sb.append("Total Pendapatan (Gaji ShopeeFood dll),Rp ").append(totalIncome.toLong()).append("\n")
        sb.append("Total Pengeluaran Harian,Rp ").append(totalExpense.toLong()).append("\n")
        sb.append("Sisa Arus Kas Bersih (Net Savings),Rp ").append(netSavings.toLong()).append("\n")
        sb.append("Total Nilai Portofolio Investasi,Rp ").append(totalAssetValuation.toLong()).append("\n\n")

        sb.append("--- RINCIAN TRANSAKSI PENDAPATAN & PENGELUARAN ---\n")
        sb.append("Tanggal,Tipe,Kategori,Keterangan,Akun / Dompet,Nominal (Rp),Catatan\n")
        for (tx in transactions) {
            val typeStr = if (tx.type == TransactionType.INCOME) "PENDAPATAN" else "PENGELUARAN"
            val sign = if (tx.type == TransactionType.INCOME) "+" else "-"
            sb.append(DateUtils.formatShortDate(tx.dateEpochMillis)).append(",")
                .append(typeStr).append(",")
                .append("\"").append(tx.categoryName).append("\",")
                .append("\"").append(tx.title.replace("\"", "\"\"")).append("\",")
                .append("\"").append(tx.wallet).append("\",")
                .append(sign).append(tx.amount.toLong()).append(",")
                .append("\"").append(tx.note.replace("\"", "\"\"")).append("\"\n")
        }

        sb.append("\n--- PORTOFOLIO ASSET & INVESTASI ---\n")
        sb.append("Nama Aset,Kategori,Unit/Lot,Harga Rata Beli (Rp),Harga Pasar (Rp),Total Modal (Rp),Nilai Pasar (Rp),Unrealized Gain/Loss (Rp),Return (%)\n")
        for (a in assets) {
            sb.append("\"").append(a.name).append("\",")
                .append(a.assetType.name).append(",")
                .append(a.units).append(" ").append(a.unitLabel).append(",")
                .append(a.avgBuyPrice.toLong()).append(",")
                .append(a.currentPrice.toLong()).append(",")
                .append(a.totalInvested.toLong()).append(",")
                .append(a.currentValue.toLong()).append(",")
                .append(a.unrealizedPnl.toLong()).append(",")
                .append(String.format(Locale.US, "%.2f%%", a.unrealizedPnlPercent)).append("\n")
        }

        FileOutputStream(file).use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }
        return file
    }

    fun generatePdf(
        context: Context,
        monthName: String,
        transactions: List<TransactionEntity>,
        assets: List<InvestmentAssetEntity>
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(reportsDir, "FinVesta_Laporan_${monthName.replace(" ", "_")}_$timeStamp.pdf")

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }

        // Header Background
        paint.color = "#0F172A".toColorInt() // Deep navy
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        // Header Title
        paint.color = "#10B981".toColorInt() // Emerald
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("FINVESTA FINANCIAL REPORT", 30f, 40f, paint)

        // Subtitle
        paint.color = Color.WHITE
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Periode: $monthName | Tanggal Ekspor: ${DateUtils.formatShortDate(System.currentTimeMillis())}", 30f, 65f, paint)

        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netSavings = totalIncome - totalExpense
        val totalAssetValuation = assets.sumOf { it.currentValue }

        // Summary Metric Boxes
        var yPos = 115f
        paint.color = "#F1F5F9".toColorInt()
        canvas.drawRoundRect(30f, yPos, 280f, yPos + 60f, 8f, 8f, paint)
        canvas.drawRoundRect(305f, yPos, 565f, yPos + 60f, 8f, 8f, paint)

        paint.color = "#64748B".toColorInt()
        paint.textSize = 10f
        canvas.drawText("TOTAL PENDAPATAN", 42f, yPos + 22f, paint)
        canvas.drawText("TOTAL PENGELUARAN", 317f, yPos + 22f, paint)

        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = "#059669".toColorInt() // Green
        canvas.drawText(CurrencyFormatter.formatRupiah(totalIncome), 42f, yPos + 48f, paint)

        paint.color = "#DC2626".toColorInt() // Red
        canvas.drawText(CurrencyFormatter.formatRupiah(totalExpense), 317f, yPos + 48f, paint)

        yPos += 72f
        paint.color = "#F1F5F9".toColorInt()
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawRoundRect(30f, yPos, 280f, yPos + 60f, 8f, 8f, paint)
        canvas.drawRoundRect(305f, yPos, 565f, yPos + 60f, 8f, 8f, paint)

        paint.color = "#64748B".toColorInt()
        paint.textSize = 10f
        canvas.drawText("ARUS KAS BERSIH (SAVINGS)", 42f, yPos + 22f, paint)
        canvas.drawText("NILAI PORTOFOLIO ASSET", 317f, yPos + 22f, paint)

        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = if (netSavings >= 0) "#0284C7".toColorInt() else "#DC2626".toColorInt()
        canvas.drawText(CurrencyFormatter.formatRupiah(netSavings), 42f, yPos + 48f, paint)

        paint.color = "#D97706".toColorInt() // Gold
        canvas.drawText(CurrencyFormatter.formatRupiah(totalAssetValuation), 317f, yPos + 48f, paint)

        // Transaction Section Header
        yPos += 85f
        paint.color = "#0F172A".toColorInt()
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Riwayat Transaksi Terbaru ($monthName)", 30f, yPos, paint)

        // Table Header
        yPos += 15f
        paint.color = "#E2E8F0".toColorInt()
        canvas.drawRect(30f, yPos, 565f, yPos + 22f, paint)

        paint.color = "#334155".toColorInt()
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Tanggal", 36f, yPos + 15f, paint)
        canvas.drawText("Kategori", 110f, yPos + 15f, paint)
        canvas.drawText("Deskripsi", 210f, yPos + 15f, paint)
        canvas.drawText("Akun", 380f, yPos + 15f, paint)
        canvas.drawText("Nominal (Rp)", 460f, yPos + 15f, paint)

        // Table Rows
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        yPos += 22f
        val displayTx = transactions.take(12) // Top items for page
        for (tx in displayTx) {
            paint.color = if (displayTx.indexOf(tx) % 2 == 0) Color.WHITE else "#F8FAFC".toColorInt()
            canvas.drawRect(30f, yPos, 565f, yPos + 22f, paint)

            paint.color = "#475569".toColorInt()
            paint.textSize = 8.5f
            canvas.drawText(DateUtils.formatShortDate(tx.dateEpochMillis), 36f, yPos + 15f, paint)
            canvas.drawText(tx.categoryName.take(15), 110f, yPos + 15f, paint)
            canvas.drawText(tx.title.take(28), 210f, yPos + 15f, paint)
            canvas.drawText(tx.wallet, 380f, yPos + 15f, paint)

            paint.color = if (tx.type == TransactionType.INCOME) "#059669".toColorInt() else "#DC2626".toColorInt()
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val sign = if (tx.type == TransactionType.INCOME) "+" else "-"
            canvas.drawText("$sign${CurrencyFormatter.formatRupiah(tx.amount)}", 460f, yPos + 15f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            yPos += 22f
        }

        // Investment Section Header
        yPos += 15f
        if (yPos < 720f) {
            paint.color = "#0F172A".toColorInt()
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Ringkasan Aset Investasi Terdaftar", 30f, yPos, paint)

            yPos += 15f
            paint.color = "#E2E8F0".toColorInt()
            canvas.drawRect(30f, yPos, 565f, yPos + 22f, paint)

            paint.color = "#334155".toColorInt()
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Nama Aset", 36f, yPos + 15f, paint)
            canvas.drawText("Tipe", 200f, yPos + 15f, paint)
            canvas.drawText("Unit", 280f, yPos + 15f, paint)
            canvas.drawText("Nilai Pasar (Rp)", 370f, yPos + 15f, paint)
            canvas.drawText("P&L (%)", 485f, yPos + 15f, paint)

            yPos += 22f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            for (asset in assets.take(5)) {
                paint.color = if (assets.indexOf(asset) % 2 == 0) Color.WHITE else "#F8FAFC".toColorInt()
                canvas.drawRect(30f, yPos, 565f, yPos + 20f, paint)

                paint.color = "#475569".toColorInt()
                paint.textSize = 8.5f
                canvas.drawText(asset.name.take(25), 36f, yPos + 14f, paint)
                canvas.drawText(asset.assetType.name, 200f, yPos + 14f, paint)
                canvas.drawText("${asset.units} ${asset.unitLabel}", 280f, yPos + 14f, paint)
                canvas.drawText(CurrencyFormatter.formatRupiah(asset.currentValue), 370f, yPos + 14f, paint)

                paint.color = if (asset.unrealizedPnl >= 0) "#059669".toColorInt() else "#DC2626".toColorInt()
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(CurrencyFormatter.formatPercent(asset.unrealizedPnlPercent), 485f, yPos + 14f, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                yPos += 20f
            }
        }

        // Footer
        paint.color = "#94A3B8".toColorInt()
        paint.textSize = 8f
        canvas.drawText("FinVesta - Terenkripsi End-to-End | Dokumen Resmi Keuangan Pribadi", 30f, 815f, paint)

        document.finishPage(page)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    fun createShareIntent(context: Context, file: File, mimeType: String): Intent {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)

        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Laporan Keuangan FinVesta - ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
