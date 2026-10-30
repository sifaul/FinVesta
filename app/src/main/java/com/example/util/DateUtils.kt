package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val fullDateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID"))
    private val shortDateFormat = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("id-ID"))
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.forLanguageTag("id-ID"))
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("id-ID"))
    private val dateTimeFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))

    fun formatFullDate(millis: Long): String = fullDateFormat.format(Date(millis))
    fun formatShortDate(millis: Long): String = shortDateFormat.format(Date(millis))
    fun formatTime(millis: Long): String = timeFormat.format(Date(millis))
    fun formatMonthYear(millis: Long): String = monthYearFormat.format(Date(millis))
    fun formatDateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

    fun getRelativeDueText(dueMillis: Long): String {
        val now = System.currentTimeMillis()
        val diffMillis = dueMillis - now
        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

        return when {
            diffDays < 0 -> "Terlambat ${-diffDays} hari"
            diffDays == 0 -> "Jatuh tempo HARI INI"
            diffDays == 1 -> "Besok"
            else -> "$diffDays hari lagi"
        }
    }

    fun isCurrentMonth(millis: Long): Boolean {
        return isSameMonthAndYear(millis, System.currentTimeMillis())
    }

    fun isSameMonthAndYear(millis1: Long, millis2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }
}
