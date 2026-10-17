package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AssetType
import com.example.data.model.CustomCategoryEntity
import com.example.data.model.FinancialGoalEntity
import com.example.data.model.InvestmentAssetEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.util.AutoCategorizer
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.example.util.FinancialCalculators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("FinVesta", appName)
  }

  @Test
  fun `currency formatter formats positive, zero, and negative amounts correctly`() {
    assertEquals("Rp 0", CurrencyFormatter.formatRupiah(0.0))
    assertEquals("Rp 100.000", CurrencyFormatter.formatRupiah(100_000.0))
    assertEquals("Rp 1.500.000", CurrencyFormatter.formatRupiah(1_500_000.0))

    // Compact formatting
    assertEquals("Rp 500 Rb", CurrencyFormatter.formatCompactRupiah(500_000.0))
    assertEquals("Rp 2,5 Jt", CurrencyFormatter.formatCompactRupiah(2_500_000.0))
    assertEquals("Rp 1,2 M", CurrencyFormatter.formatCompactRupiah(1_200_000_000.0))
    assertEquals("-Rp 2,5 Jt", CurrencyFormatter.formatCompactRupiah(-2_500_000.0))

    // Thousand separator input helper
    assertEquals("500.000", CurrencyFormatter.formatThousands("500000"))
    assertEquals("1.000.000", CurrencyFormatter.formatThousands("1000000"))
    assertEquals("", CurrencyFormatter.formatThousands(""))
  }

  @Test
  fun `financial calculators handle edge cases and calculate correctly`() {
    // 1. KPR normal calculation
    val kprNormal = FinancialCalculators.calculateKpr(
      propertyPrice = 500_000_000.0,
      downPayment = 100_000_000.0,
      annualInterestRatePercent = 8.0,
      tenorYears = 15
    )
    assertTrue(kprNormal.loanPrincipal == 400_000_000.0)
    assertTrue(kprNormal.monthlyInstallment > 0.0)
    assertTrue(kprNormal.totalPayment > kprNormal.loanPrincipal)
    assertTrue(kprNormal.recommendedMinIncome > kprNormal.monthlyInstallment)

    // KPR zero principal edge case (DP >= price)
    val kprZero = FinancialCalculators.calculateKpr(
      propertyPrice = 500_000_000.0,
      downPayment = 500_000_000.0,
      annualInterestRatePercent = 8.0,
      tenorYears = 15
    )
    assertEquals(0.0, kprZero.loanPrincipal, 0.01)
    assertEquals(0.0, kprZero.monthlyInstallment, 0.01)

    // KPR 0% interest rate
    val kprZeroInterest = FinancialCalculators.calculateKpr(
      propertyPrice = 120_000_000.0,
      downPayment = 0.0,
      annualInterestRatePercent = 0.0,
      tenorYears = 10
    )
    assertEquals(1_000_000.0, kprZeroInterest.monthlyInstallment, 0.01)

    // 2. Personal Loan (Flat vs Effective)
    val flatLoan = FinancialCalculators.calculatePersonalLoan(
      principal = 12_000_000.0,
      annualInterestRatePercent = 10.0,
      tenorMonths = 12,
      isFlat = true
    )
    assertEquals(1_100_000.0, flatLoan.monthlyInstallment, 0.01)

    // 3. Compound Interest
    val compound = FinancialCalculators.calculateCompoundInterest(
      initialDeposit = 10_000_000.0,
      monthlyContribution = 1_000_000.0,
      annualReturnPercent = 12.0,
      durationYears = 5
    )
    assertTrue(compound.futureValue > compound.totalPrincipalInvested)
    assertTrue(compound.totalInterestEarned > 0.0)
  }

  @Test
  fun `financial goal entity calculations avoid division by zero and compute accurately`() {
    val normalGoal = FinancialGoalEntity(
      name = "Beli Mobil",
      targetAmount = 200_000_000.0,
      currentSavedAmount = 50_000_000.0,
      targetDateEpochMillis = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000)
    )
    assertEquals(0.25f, normalGoal.progressFraction, 0.001f)
    assertEquals(25.0, normalGoal.progressPercent, 0.001)
    assertEquals(150_000_000.0, normalGoal.remainingAmount, 0.001)
    assertFalse(normalGoal.isCompleted)

    // Zero target amount edge case
    val zeroGoal = FinancialGoalEntity(
      name = "Target Kosong",
      targetAmount = 0.0,
      currentSavedAmount = 10_000.0,
      targetDateEpochMillis = System.currentTimeMillis()
    )
    assertEquals(0f, zeroGoal.progressFraction, 0.001f)
    assertEquals(0.0, zeroGoal.progressPercent, 0.001)
    assertEquals(0.0, zeroGoal.remainingAmount, 0.001)
  }

  @Test
  fun `investment asset entity calculates PnL and handles zero invested correctly`() {
    val asset = InvestmentAssetEntity(
      name = "BBCA",
      assetType = AssetType.SAHAM,
      units = 1000.0,
      avgBuyPrice = 9000.0,
      currentPrice = 10000.0
    )
    assertEquals(9_000_000.0, asset.totalInvested, 0.01)
    assertEquals(10_000_000.0, asset.currentValue, 0.01)
    assertEquals(1_000_000.0, asset.unrealizedPnl, 0.01)
    assertEquals(11.11, asset.unrealizedPnlPercent, 0.05)

    // Zero investment edge case
    val zeroAsset = InvestmentAssetEntity(
      name = "Empty",
      assetType = AssetType.KAS,
      units = 0.0,
      avgBuyPrice = 0.0,
      currentPrice = 0.0
    )
    assertEquals(0.0, zeroAsset.unrealizedPnlPercent, 0.01)
  }

  @Test
  fun `auto categorizer detects appropriate categories from title keywords`() {
    val categories = listOf(
      CustomCategoryEntity(id = 1, name = "Makanan", isExpense = true),
      CustomCategoryEntity(id = 2, name = "Transportasi", isExpense = true),
      CustomCategoryEntity(id = 3, name = "Gaji", isExpense = false)
    )

    val txFood = TransactionEntity(
      type = TransactionType.EXPENSE,
      title = "Makan siang bakso solo",
      amount = 25000.0,
      categoryName = "Umum"
    )
    val bestCatFood = AutoCategorizer.findBestCategory(txFood, categories)
    assertNotNull(bestCatFood)
    assertEquals("Makanan", bestCatFood?.name)

    val txTransport = TransactionEntity(
      type = TransactionType.EXPENSE,
      title = "Isi bensin pertamax",
      amount = 50000.0,
      categoryName = "Umum"
    )
    val bestCatTransport = AutoCategorizer.findBestCategory(txTransport, categories)
    assertNotNull(bestCatTransport)
    assertEquals("Transportasi", bestCatTransport?.name)

    val txSalary = TransactionEntity(
      type = TransactionType.INCOME,
      title = "Gaji bulanan kantor",
      amount = 10_000_000.0,
      categoryName = "Umum"
    )
    val bestCatSalary = AutoCategorizer.findBestCategory(txSalary, categories)
    assertNotNull(bestCatSalary)
    assertEquals("Gaji", bestCatSalary?.name)
  }

  @Test
  fun `date utils checks month and relative due dates correctly`() {
    val now = System.currentTimeMillis()
    assertTrue(DateUtils.isCurrentMonth(now))

    val dueToday = DateUtils.getRelativeDueText(now)
    assertEquals("Jatuh tempo HARI INI", dueToday)

    val dueTomorrow = DateUtils.getRelativeDueText(now + (24L * 60 * 60 * 1000) + 1000)
    assertEquals("Besok", dueTomorrow)
  }
}
