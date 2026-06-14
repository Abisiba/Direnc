package com.abis.direnc.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

object Stats {

    private fun toLocalDate(epochMillis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    fun countOnDay(timestamps: List<Long>, now: Long, zone: ZoneId): Int {
        val today = toLocalDate(now, zone)
        return timestamps.count { toLocalDate(it, zone) == today }
    }

    fun countThisWeek(timestamps: List<Long>, now: Long, zone: ZoneId): Int {
        val today = toLocalDate(now, zone)
        // Hafta Pazartesi başlar (locale verisinden bağımsız, sabit)
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return timestamps.count {
            val d = toLocalDate(it, zone)
            !d.isBefore(weekStart) && !d.isAfter(today)
        }
    }

    /**
     * Bugünden (ya da olay bugün yoksa dünden) geriye doğru, üst üste en az bir
     * olay olan gün sayısı.
     */
    fun streak(timestamps: List<Long>, now: Long, zone: ZoneId): Int {
        if (timestamps.isEmpty()) return 0
        val days = timestamps.map { toLocalDate(it, zone) }.toSet()
        val today = toLocalDate(now, zone)

        var cursor = when {
            days.contains(today) -> today
            days.contains(today.minusDays(1)) -> today.minusDays(1)
            else -> return 0
        }
        var count = 0
        while (days.contains(cursor)) {
            count++
            cursor = cursor.minus(1, ChronoUnit.DAYS)
        }
        return count
    }
}
