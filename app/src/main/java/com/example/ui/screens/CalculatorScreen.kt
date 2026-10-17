package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Amber500
import com.example.ui.theme.Cyan500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Rose500
import com.example.util.CurrencyFormatter
import com.example.util.FinancialCalculators

@Composable
fun CalculatorScreen(
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Calculate,
                contentDescription = null,
                tint = Emerald500,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Kalkulator Finansial",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Simulasi KPR, Pinjaman Pribadi, & Tabungan Bunga Majemuk",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clip(CircleShape)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("KPR Rumah", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_calc_kpr")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Pinjaman", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_calc_loan")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Bunga Majemuk", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("tab_calc_compound")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> KprCalculatorSection()
                1 -> PersonalLoanCalculatorSection()
                2 -> CompoundSavingsCalculatorSection()
            }
        }
    }
}

/**
 * 1. Kalkulator KPR (Kredit Pemilikan Rumah)
 */
@Composable
fun KprCalculatorSection() {
    var propertyPriceText by remember { mutableStateOf("650.000.000") } // Rp 650 Juta
    var downPaymentText by remember { mutableStateOf("130.000.000") } // DP 20% = 130 Juta
    var interestRate by remember { mutableDoubleStateOf(7.5) } // 7.5% per tahun
    var tenorYears by remember { mutableIntStateOf(15) } // 15 tahun

    val propertyPrice = propertyPriceText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
    val downPayment = downPaymentText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0

    val kprResult = remember(propertyPrice, downPayment, interestRate, tenorYears) {
        FinancialCalculators.calculateKpr(propertyPrice, downPayment, interestRate, tenorYears)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Form Inputs Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Input Data KPR", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = propertyPriceText,
                    onValueChange = { propertyPriceText = CurrencyFormatter.formatThousands(it) },
                    label = { Text("Harga Properti / Rumah") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().testTag("kpr_property_price_input")
                )

                OutlinedTextField(
                    value = downPaymentText,
                    onValueChange = { downPaymentText = CurrencyFormatter.formatThousands(it) },
                    label = { Text("Uang Muka / DP") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().testTag("kpr_down_payment_input")
                )

                // Quick DP buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10 to "DP 10%", 20 to "DP 20%", 30 to "DP 30%").forEach { (pct, label) ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                val dpCalc = propertyPrice * (pct / 100.0)
                                downPaymentText = CurrencyFormatter.formatThousands(dpCalc.toLong().toString())
                            },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                // Suku Bunga Slider & Selector
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Suku Bunga KPR Tahunan:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${String.format(Locale.getDefault(), "%.1f", interestRate)}% p.a.", fontWeight = FontWeight.Bold, color = Emerald500)
                    }
                    Slider(
                        value = interestRate.toFloat(),
                        onValueChange = { interestRate = (Math.round(it * 10.0) / 10.0) },
                        valueRange = 3.0f..18.0f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Tenor Selector
                Text("Jangka Waktu (Tenor): $tenorYears Tahun", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5, 10, 15, 20, 25).forEach { yrs ->
                        FilterChip(
                            selected = tenorYears == yrs,
                            onClick = { tenorYears = yrs },
                            label = { Text("$yrs Thn", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Result Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Emerald500.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Estimasi Cicilan Bulanan KPR",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = CurrencyFormatter.formatRupiah(kprResult.monthlyInstallment) + " / bln",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Emerald500
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Proportion Bar (Pokok Pinjaman vs Total Bunga)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Plafon Pokok: ${String.format(Locale.getDefault(), "%.0f%%", kprResult.principalPercentage)}", fontSize = 11.sp, color = Cyan500)
                            Text("Total Bunga: ${String.format(Locale.getDefault(), "%.0f%%", kprResult.interestPercentage)}", fontSize = 11.sp, color = Rose500)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (kprResult.principalPercentage / 100.0).toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Cyan500,
                            trackColor = Rose500
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Plafon Pinjaman Pokok", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CurrencyFormatter.formatRupiah(kprResult.loanPrincipal), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Akumulasi Bunga", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CurrencyFormatter.formatRupiah(kprResult.totalInterest), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Rose500)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Pembayaran (Pokok + Bunga)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CurrencyFormatter.formatRupiah(kprResult.totalPayment), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Minimum income suggestion
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Emerald500.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald500, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Saran Gaji Minimal: ${CurrencyFormatter.formatRupiah(kprResult.recommendedMinIncome)}/bln (Maksimal DSR 35%)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Emerald500
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

/**
 * 2. Kalkulator Pinjaman Pribadi / Konsumtif
 */
@Composable
fun PersonalLoanCalculatorSection() {
    var principalText by remember { mutableStateOf("25.000.000") } // Rp 25 Juta
    var interestRate by remember { mutableDoubleStateOf(10.5) } // 10.5%
    var tenorMonths by remember { mutableIntStateOf(12) } // 12 bulan
    var isFlatInterest by remember { mutableStateOf(true) } // Flat vs Efektif

    val principal = principalText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
    val result = remember(principal, interestRate, tenorMonths, isFlatInterest) {
        FinancialCalculators.calculatePersonalLoan(principal, interestRate, tenorMonths, isFlatInterest)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Input Pinjaman Pribadi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = principalText,
                    onValueChange = { principalText = CurrencyFormatter.formatThousands(it) },
                    label = { Text("Jumlah Pinjaman Pokok (Rp)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().testTag("loan_principal_input")
                )

                // Metode Bunga
                Text("Tipe Perhitungan Suku Bunga:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isFlatInterest,
                        onClick = { isFlatInterest = true },
                        label = { Text("Bunga Flat", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = !isFlatInterest,
                        onClick = { isFlatInterest = false },
                        label = { Text("Bunga Efektif / Anuitas", fontSize = 12.sp) }
                    )
                }

                // Suku Bunga
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Suku Bunga Tahunan:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${String.format(Locale.getDefault(), "%.1f", interestRate)}% / tahun", fontWeight = FontWeight.Bold, color = Cyan500)
                }
                Slider(
                    value = interestRate.toFloat(),
                    onValueChange = { interestRate = (Math.round(it * 10.0) / 10.0) },
                    valueRange = 2.0f..36.0f,
                    modifier = Modifier.fillMaxWidth()
                )

                // Tenor Pinjaman Bulan
                Text("Jangka Waktu Pinjaman: $tenorMonths Bulan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(6, 12, 18, 24, 36, 48).forEach { m ->
                        FilterChip(
                            selected = tenorMonths == m,
                            onClick = { tenorMonths = m },
                            label = { Text("$m Bln", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Result Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Cyan500.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Angsuran Bulanan (${if (isFlatInterest) "Bunga Flat" else "Bunga Efektif"})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = CurrencyFormatter.formatRupiah(result.monthlyInstallment) + " / bln",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Cyan500
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Pinjaman Pokok Diterima", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CurrencyFormatter.formatRupiah(result.principal), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Bunga yang Dibayar", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CurrencyFormatter.formatRupiah(result.totalInterest), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Rose500)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Keseluruhan Pengembalian", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CurrencyFormatter.formatRupiah(result.totalRepayment), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

/**
 * 3. Kalkulator Tabungan Berbunga Majemuk (Compound Interest)
 */
@Composable
fun CompoundSavingsCalculatorSection() {
    var initialDepositText by remember { mutableStateOf("10.000.000") } // Rp 10 Juta
    var monthlyContributionText by remember { mutableStateOf("2.000.000") } // Rp 2 Juta / bln
    var annualReturn by remember { mutableDoubleStateOf(10.0) } // 10% per tahun
    var durationYears by remember { mutableIntStateOf(5) } // 5 tahun

    val initialDeposit = initialDepositText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
    val monthlyContribution = monthlyContributionText.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0

    val compoundResult = remember(initialDeposit, monthlyContribution, annualReturn, durationYears) {
        FinancialCalculators.calculateCompoundInterest(initialDeposit, monthlyContribution, annualReturn, durationYears)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Parameter Tabungan Majemuk", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = initialDepositText,
                    onValueChange = { initialDepositText = CurrencyFormatter.formatThousands(it) },
                    label = { Text("Setoran Awal (Modal Awal)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().testTag("compound_initial_input")
                )

                OutlinedTextField(
                    value = monthlyContributionText,
                    onValueChange = { monthlyContributionText = CurrencyFormatter.formatThousands(it) },
                    label = { Text("Setoran Rutin Setiap Bulan") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().testTag("compound_monthly_input")
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Estimasi Imbal Hasil (Return):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${String.format(Locale.getDefault(), "%.1f", annualReturn)}% p.a.", fontWeight = FontWeight.Bold, color = Amber500)
                }
                Slider(
                    value = annualReturn.toFloat(),
                    onValueChange = { annualReturn = (Math.round(it * 10.0) / 10.0) },
                    valueRange = 3.0f..25.0f,
                    modifier = Modifier.fillMaxWidth()
                )

                // Presets for return rate
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5.0 to "Deposito 5%", 8.0 to "Obligasi 8%", 12.0 to "Saham/ETF 12%").forEach { (ret, label) ->
                        FilterChip(
                            selected = annualReturn == ret,
                            onClick = { annualReturn = ret },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                // Durasi Investasi
                Text("Jangka Waktu Investasi: $durationYears Tahun", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 3, 5, 10, 15, 20).forEach { yr ->
                        FilterChip(
                            selected = durationYears == yr,
                            onClick = { durationYears = yr },
                            label = { Text("$yr Thn", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Result Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Amber500.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Proyeksi Nilai Tabungan Akhir ($durationYears Tahun)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = CurrencyFormatter.formatRupiah(compoundResult.futureValue),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Amber500
                    )

                    // Growth factor badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Emerald500.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Pertumbuhan Modal: ${String.format(Locale.getDefault(), "%.2f", compoundResult.interestMultiplier)}x lipat",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald500,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Modal Pokok Disetor", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CurrencyFormatter.formatRupiah(compoundResult.totalPrincipalInvested), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Keuntungan Bunga Majemuk", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CurrencyFormatter.formatRupiah(compoundResult.totalInterestEarned), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald500)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
