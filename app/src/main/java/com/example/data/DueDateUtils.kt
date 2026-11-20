package com.example.data

import java.util.Calendar
import java.util.Locale

data class PaymentAlert(
    val installment: InstallmentEntity,
    val daysRemaining: Int
) {
    val urgencyLabel: String
        get() = when {
            daysRemaining < 0 -> "Vencida hace ${-daysRemaining} d"
            daysRemaining == 0 -> "Vence hoy"
            daysRemaining == 1 -> "Vence mañana"
            else -> "Vence en $daysRemaining días"
        }
}

object DueDateUtils {
    private val spanishMonths = mapOf(
        "ene" to Calendar.JANUARY,
        "feb" to Calendar.FEBRUARY,
        "mar" to Calendar.MARCH,
        "abr" to Calendar.APRIL,
        "may" to Calendar.MAY,
        "jun" to Calendar.JUNE,
        "jul" to Calendar.JULY,
        "ago" to Calendar.AUGUST,
        "sep" to Calendar.SEPTEMBER,
        "set" to Calendar.SEPTEMBER,
        "oct" to Calendar.OCTOBER,
        "nov" to Calendar.NOVEMBER,
        "dic" to Calendar.DECEMBER
    )

    fun daysUntilDue(dateText: String, referenceTimeMillis: Long = System.currentTimeMillis()): Int? {
        val targetCal = parseDateToMidnightCalendar(dateText) ?: return null
        val todayCal = Calendar.getInstance().apply {
            timeInMillis = referenceTimeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = targetCal.timeInMillis - todayCal.timeInMillis
        val dayMillis = 24L * 60L * 60L * 1000L
        return (diffMillis / dayMillis).toInt()
    }

    fun formatDateOffsetFromToday(
        daysFromToday: Int,
        referenceTimeMillis: Long = System.currentTimeMillis()
    ): String {
        val cal = Calendar.getInstance().apply {
            timeInMillis = referenceTimeMillis
            add(Calendar.DAY_OF_YEAR, daysFromToday)
        }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthIdx = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)
        val monthAbbr = when (monthIdx) {
            Calendar.JANUARY -> "Ene"
            Calendar.FEBRUARY -> "Feb"
            Calendar.MARCH -> "Mar"
            Calendar.APRIL -> "Abr"
            Calendar.MAY -> "May"
            Calendar.JUNE -> "Jun"
            Calendar.JULY -> "Jul"
            Calendar.AUGUST -> "Ago"
            Calendar.SEPTEMBER -> "Sep"
            Calendar.OCTOBER -> "Oct"
            Calendar.NOVEMBER -> "Nov"
            Calendar.DECEMBER -> "Dic"
            else -> "Nov"
        }
        return "$day $monthAbbr $year"
    }

    fun computeUpcomingAlerts(
        installments: List<InstallmentEntity>,
        thresholdDays: Int,
        referenceTimeMillis: Long = System.currentTimeMillis()
    ): List<PaymentAlert> {
        return installments
            .filter { !it.isCompleted }
            .mapNotNull { item ->
                val days = daysUntilDue(item.nextPaymentDate, referenceTimeMillis)
                if (days != null && days <= thresholdDays) {
                    PaymentAlert(installment = item, daysRemaining = days)
                } else {
                    null
                }
            }
            .sortedBy { it.daysRemaining }
    }

    private fun parseDateToMidnightCalendar(raw: String): Calendar? {
        val cleaned = raw.trim()
        if (cleaned.isEmpty() || cleaned.equals("Completado", ignoreCase = true)) return null

        // Format 1: "21 Nov 2026"
        val spaceParts = cleaned.split(Regex("\\s+"))
        if (spaceParts.size == 3) {
            val day = spaceParts[0].toIntOrNull()
            val month = spanishMonths[spaceParts[1].lowercase(Locale.ROOT).take(3)]
            val year = spaceParts[2].toIntOrNull()
            if (day != null && month != null && year != null) {
                return buildMidnightCalendar(year, month, day)
            }
        }

        // Format 2: "21/11/2026" or "21-11-2026"
        val slashParts = cleaned.split('/', '-')
        if (slashParts.size == 3) {
            val first = slashParts[0].toIntOrNull()
            val second = slashParts[1].toIntOrNull()
            val third = slashParts[2].toIntOrNull()
            if (first != null && second != null && third != null) {
                return if (first > 1900) {
                    buildMidnightCalendar(first, second - 1, third)
                } else {
                    buildMidnightCalendar(third, second - 1, first)
                }
            }
        }

        return null
    }

    private fun buildMidnightCalendar(year: Int, monthZeroBased: Int, day: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
