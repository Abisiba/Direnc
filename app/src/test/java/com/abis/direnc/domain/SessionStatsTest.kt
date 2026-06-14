package com.abis.direnc.domain

import com.abis.direnc.data.Session
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SessionStatsTest {

    private val zone: ZoneId = ZoneId.of("Europe/Istanbul")

    private fun day(y: Int, m: Int, d: Int, hour: Int = 12): Long =
        LocalDate.of(y, m, d).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private val now = day(2026, 6, 14, 18) // Pazar

    @Test fun `summaries tamamlanmamis oturumu eler ve en yeni basta siralar`() {
        val sessions = listOf(
            Session(id = 1, startedAt = day(2026, 6, 12), endedAt = day(2026, 6, 12) + 3_600_000),
            Session(id = 2, startedAt = day(2026, 6, 14), endedAt = null), // aktif → elenir
            Session(id = 3, startedAt = day(2026, 6, 13), endedAt = day(2026, 6, 13) + 1_800_000),
        )
        val result = SessionStats.summaries(sessions, listOf(1L, 1L, 3L), listOf(3L))
        assertEquals(2, result.size)
        assertEquals(day(2026, 6, 13), result[0].startedAt) // en yeni (id3) başta
        assertEquals(1, result[0].entries)      // id3'te 1 giriş
        assertEquals(1, result[0].resistances)  // id3'te 1 direniş
        assertEquals(2, result[1].resistances)  // id1'de 2 direniş
        assertEquals(3_600_000L, result[1].durationMillis)
    }

    @Test fun `totalStudyMillis tamamlanmis oturum surelerini toplar`() {
        val sessions = listOf(
            Session(id = 1, startedAt = 0, endedAt = 1_000_000),
            Session(id = 2, startedAt = 0, endedAt = null),         // elenir
            Session(id = 3, startedAt = 0, endedAt = 2_000_000),
        )
        assertEquals(3_000_000L, SessionStats.totalStudyMillis(sessions))
    }

    @Test fun `studyMillisOnDay yalnizca o gun baslayan oturumlari toplar`() {
        val sessions = listOf(
            Session(id = 1, startedAt = day(2026, 6, 14, 9), endedAt = day(2026, 6, 14, 10)),  // bugün 1 saat
            Session(id = 2, startedAt = day(2026, 6, 13, 9), endedAt = day(2026, 6, 13, 10)),  // dün
        )
        assertEquals(3_600_000L, SessionStats.studyMillisOnDay(sessions, now, zone))
    }

    @Test fun `dailyStudyMillis son 7 gunu eskiden yeniye dondurur`() {
        val sessions = listOf(
            Session(id = 1, startedAt = day(2026, 6, 14, 9), endedAt = day(2026, 6, 14, 10)),
        )
        val result = SessionStats.dailyStudyMillis(sessions, now, zone, days = 7)
        assertEquals(7, result.size)
        assertEquals(LocalDate.of(2026, 6, 8), result.first().date)   // en eski
        assertEquals(LocalDate.of(2026, 6, 14), result.last().date)   // bugün
        assertEquals(3_600_000L, result.last().millis)
        assertEquals(0L, result.first().millis)
    }

    @Test fun `formatDuration saat ve dakikayi bicimler`() {
        assertEquals("0dk", formatDuration(0))
        assertEquals("45dk", formatDuration(45 * 60_000L))
        assertEquals("1s 12dk", formatDuration((72 * 60_000L)))
        assertEquals("2s 0dk", formatDuration(2 * 3_600_000L))
    }
}
