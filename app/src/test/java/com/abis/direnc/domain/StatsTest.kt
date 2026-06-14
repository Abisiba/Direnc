package com.abis.direnc.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StatsTest {

    private val zone: ZoneId = ZoneId.of("Europe/Istanbul")

    // Belirli bir yerel tarihte gün ortasının epoch-millis değeri
    private fun day(year: Int, month: Int, dayOfMonth: Int): Long =
        LocalDate.of(year, month, dayOfMonth)
            .atTime(12, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    private val now = day(2026, 6, 14)

    @Test
    fun `bugun sayisi yalnizca bugunku olaylari sayar`() {
        val ts = listOf(day(2026, 6, 14), day(2026, 6, 14), day(2026, 6, 13))
        assertEquals(2, Stats.countOnDay(ts, now, zone))
    }

    @Test
    fun `bu hafta pazartesiden itibaren sayar`() {
        // 2026-06-14 Pazar. Haftanın başı (Pzt) 2026-06-08.
        val ts = listOf(
            day(2026, 6, 8),   // Pzt - dahil
            day(2026, 6, 14),  // Paz - dahil
            day(2026, 6, 7),   // önceki Paz - hariç
        )
        assertEquals(2, Stats.countThisWeek(ts, now, zone))
    }

    @Test
    fun `streak bugun dahil ust uste gunleri sayar`() {
        val ts = listOf(day(2026, 6, 14), day(2026, 6, 13), day(2026, 6, 12))
        assertEquals(3, Stats.streak(ts, now, zone))
    }

    @Test
    fun `streak bir gun bos kalinca kirilir`() {
        val ts = listOf(day(2026, 6, 14), day(2026, 6, 12)) // 13 boş
        assertEquals(1, Stats.streak(ts, now, zone))
    }

    @Test
    fun `streak dun bitiyorsa da sayar (bugun henuz olay yok)`() {
        val ts = listOf(day(2026, 6, 13), day(2026, 6, 12))
        assertEquals(2, Stats.streak(ts, now, zone))
    }

    @Test
    fun `hic olay yoksa streak sifir`() {
        assertEquals(0, Stats.streak(emptyList(), now, zone))
    }
}
