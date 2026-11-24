package com.dumbhabit.app.data

import java.time.LocalDate
import kotlin.math.abs

/** Bit 0 = lundi ... bit 6 = dimanche. */
const val EVERY_DAY = 0b1111111

private val DAY_LETTERS = listOf("L", "M", "M", "J", "V", "S", "D")

fun todayIso(): String = LocalDate.now().toString()

fun dayBit(date: LocalDate): Int = 1 shl (date.dayOfWeek.value - 1)

fun isActiveOn(daysMask: Int, date: LocalDate = LocalDate.now()): Boolean =
    (daysMask and dayBit(date)) != 0

fun describeDays(mask: Int): String = when (mask) {
    EVERY_DAY -> "Tous les jours"
    0 -> "Aucun jour"
    else -> DAY_LETTERS.filterIndexed { i, _ -> (mask and (1 shl i)) != 0 }.joinToString(" ")
}

/** 510 -> "08:30" */
fun minutesToLabel(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

/** 125 -> "02:05". Valeur négative : préfixée par "-". */
fun formatSeconds(total: Long): String {
    val sign = if (total < 0) "-" else ""
    val s = abs(total)
    return "%s%02d:%02d".format(sign, s / 60, s % 60)
}
