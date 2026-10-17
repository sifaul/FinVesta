package com.example.util

import kotlin.math.pow

data class KprResult(
    val loanPrincipal: Double,
    val monthlyInstallment: Double,
    val totalPayment: Double,
    val totalInterest: Double,
    val interestPercentage: Double,
    val principalPercentage: Double,
    val recommendedMinIncome: Double
)

data class PersonalLoanResult(
    val principal: Double,
    val monthlyInstallment: Double,
    val totalInterest: Double,
    val totalRepayment: Double,
    val isFlat: Boolean
)

data class CompoundInterestResult(
    val initialDeposit: Double,
    val totalMonthlyDeposits: Double,
    val totalPrincipalInvested: Double,
    val totalInterestEarned: Double,
    val futureValue: Double,
    val interestMultiplier: Double
)

object FinancialCalculators {

    /**
     * Kalkulator KPR (Anuitas Standar Perbankan)
     */
    fun calculateKpr(
        propertyPrice: Double,
        downPayment: Double,
        annualInterestRatePercent: Double,
        tenorYears: Int
    ): KprResult {
        val principal = (propertyPrice - downPayment).coerceAtLeast(0.0)
        val totalMonths = (tenorYears * 12).coerceAtLeast(1)

        if (principal <= 0.0) {
            return KprResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        val monthlyRate = (annualInterestRatePercent / 100.0) / 12.0
        val monthlyInstallment = if (monthlyRate > 0.0) {
            val factor = (1.0 + monthlyRate).pow(totalMonths.toDouble())
            principal * (monthlyRate * factor) / (factor - 1.0)
        } else {
            principal / totalMonths.toDouble()
        }

        val totalPayment = monthlyInstallment * totalMonths
        val totalInterest = (totalPayment - principal).coerceAtLeast(0.0)
        val interestPct = if (totalPayment > 0) (totalInterest / totalPayment) * 100.0 else 0.0
        val principalPct = if (totalPayment > 0) (principal / totalPayment) * 100.0 else 100.0
        val minIncome = monthlyInstallment / 0.35 // Batas Debt Service Ratio 35%

        return KprResult(
            loanPrincipal = principal,
            monthlyInstallment = monthlyInstallment,
            totalPayment = totalPayment,
            totalInterest = totalInterest,
            interestPercentage = interestPct,
            principalPercentage = principalPct,
            recommendedMinIncome = minIncome
        )
    }

    /**
     * Kalkulator Pinjaman Pribadi / Konsumtif (Flat vs Efektif)
     */
    fun calculatePersonalLoan(
        principal: Double,
        annualInterestRatePercent: Double,
        tenorMonths: Int,
        isFlat: Boolean
    ): PersonalLoanResult {
        val cleanMonths = tenorMonths.coerceAtLeast(1)
        if (principal <= 0.0) {
            return PersonalLoanResult(0.0, 0.0, 0.0, 0.0, isFlat)
        }

        return if (isFlat) {
            val annualRate = annualInterestRatePercent / 100.0
            val totalInterest = principal * annualRate * (cleanMonths / 12.0)
            val totalRepayment = principal + totalInterest
            val monthlyInstallment = totalRepayment / cleanMonths
            PersonalLoanResult(principal, monthlyInstallment, totalInterest, totalRepayment, true)
        } else {
            val monthlyRate = (annualInterestRatePercent / 100.0) / 12.0
            val monthlyInstallment = if (monthlyRate > 0.0) {
                val factor = (1.0 + monthlyRate).pow(cleanMonths.toDouble())
                principal * (monthlyRate * factor) / (factor - 1.0)
            } else {
                principal / cleanMonths
            }
            val totalRepayment = monthlyInstallment * cleanMonths
            val totalInterest = (totalRepayment - principal).coerceAtLeast(0.0)
            PersonalLoanResult(principal, monthlyInstallment, totalInterest, totalRepayment, false)
        }
    }

    /**
     * Kalkulator Tabungan Berbunga Majemuk (Compound Interest)
     */
    fun calculateCompoundInterest(
        initialDeposit: Double,
        monthlyContribution: Double,
        annualReturnPercent: Double,
        durationYears: Int
    ): CompoundInterestResult {
        val totalMonths = (durationYears * 12).coerceAtLeast(1)
        val monthlyRate = (annualReturnPercent / 100.0) / 12.0
        val totalMonthlyDeposits = monthlyContribution * totalMonths
        val totalPrincipalInvested = initialDeposit + totalMonthlyDeposits

        val futureValue = if (monthlyRate > 0.0) {
            val factor = (1.0 + monthlyRate).pow(totalMonths.toDouble())
            val fvInitial = initialDeposit * factor
            val fvAnnuity = monthlyContribution * ((factor - 1.0) / monthlyRate)
            fvInitial + fvAnnuity
        } else {
            totalPrincipalInvested
        }

        val totalInterest = (futureValue - totalPrincipalInvested).coerceAtLeast(0.0)
        val multiplier = if (totalPrincipalInvested > 0) futureValue / totalPrincipalInvested else 1.0

        return CompoundInterestResult(
            initialDeposit = initialDeposit,
            totalMonthlyDeposits = totalMonthlyDeposits,
            totalPrincipalInvested = totalPrincipalInvested,
            totalInterestEarned = totalInterest,
            futureValue = futureValue,
            interestMultiplier = multiplier
        )
    }
}
