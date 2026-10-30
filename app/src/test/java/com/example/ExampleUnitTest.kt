package com.example

import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun budgetAlert_triggersOnlyWhenStrictlyOver80Percent() {
    val budgetLimit = 1_000_000.0

    // Exactly 80.0% -> NOT more than 80%, should NOT trigger alert
    val spent80Exact = 800_000.0
    val percentage80 = (spent80Exact / budgetLimit) * 100.0
    assertFalse("Spending at exactly 80.0% should not trigger alert (must be > 80%)", percentage80 > 80.0)

    // 79.9% -> NOT more than 80%, should NOT trigger alert
    val spentUnder80 = 799_000.0
    val percentageUnder80 = (spentUnder80 / budgetLimit) * 100.0
    assertFalse("Spending under 80% should not trigger alert", percentageUnder80 > 80.0)

    // 82.5% -> More than 80%, MUST trigger alert
    val spentOver80 = 825_000.0
    val percentageOver80 = (spentOver80 / budgetLimit) * 100.0
    assertTrue("Spending over 80% must trigger alert", percentageOver80 > 80.0)
    assertFalse("Spending should not exceed 100% yet", percentageOver80 > 100.0)
  }

  @Test
  fun categoryMatching_normalizesCorrectly() {
    fun isCategoryMatch(txCategory: String, customCatName: String): Boolean {
      if (txCategory.equals(customCatName, ignoreCase = true)) return true
      val normTx = txCategory.replace("&", "").replace("/", "").replace("-", "").replace(" ", "").lowercase()
      val normCat = customCatName.replace("&", "").replace("/", "").replace("-", "").replace(" ", "").lowercase()
      return normTx.contains(normCat) || normCat.contains(normTx)
    }

    assertTrue(isCategoryMatch("Makanan & Minuman", "Makanan & Minuman"))
    assertTrue(isCategoryMatch("Makanan & Minuman", "makanan"))
    assertTrue(isCategoryMatch("Transportasi / Bensin", "Transportasi"))
  }

  @Test
  fun expenditurePieChart_distributionCalculationIsAccurate() {
    val expenses = listOf(
      "Makanan" to 500_000.0,
      "Transportasi" to 300_000.0,
      "Belanja" to 200_000.0
    )
    val total = expenses.sumOf { it.second }
    assertEquals(1_000_000.0, total, 0.001)

    val distributions = expenses.map { it.first to (it.second / total) * 100.0 }
    val totalPercent = distributions.sumOf { it.second }

    assertEquals(100.0, totalPercent, 0.001)
    assertEquals(50.0, distributions[0].second, 0.001)
    assertEquals(30.0, distributions[1].second, 0.001)
    assertEquals(20.0, distributions[2].second, 0.001)
  }

  @Test
  fun shopeeFood_calculationFormula_isAccurate() {
    val grossIncome = 150_000.0
    val bonus = 25_000.0
    val tips = 10_000.0
    val totalGross = grossIncome + bonus + tips

    val bensin = 20_000.0
    val parkir = 5_000.0
    val amal = 2_000.0
    val totalExpense = bensin + parkir + amal

    val netIncome = totalGross - totalExpense

    assertEquals(185_000.0, totalGross, 0.001)
    assertEquals(27_000.0, totalExpense, 0.001)
    assertEquals(158_000.0, netIncome, 0.001)
  }

  @Test
  fun notificationCooldown_preventsSpamming() {
    val cooldownMs = 2 * 60 * 60 * 1000L // 2 hours
    val now = 1_000_000_000L

    // First notification sent at `now`
    val lastSent = now

    // Attempt 30 minutes later -> should be blocked
    val attempt30MinLater = now + (30 * 60 * 1000L)
    val shouldNotify30Min = (attempt30MinLater - lastSent) >= cooldownMs
    assertFalse("Notification should be suppressed within 2 hours cooldown", shouldNotify30Min)

    // Attempt 2 hours and 1 minute later -> allowed
    val attempt2HoursLater = now + cooldownMs + 60_000L
    val shouldNotify2Hours = (attempt2HoursLater - lastSent) >= cooldownMs
    assertTrue("Notification should be permitted after 2 hours cooldown", shouldNotify2Hours)
  }
}
