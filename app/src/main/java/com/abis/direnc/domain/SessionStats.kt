package com.abis.direnc.domain

import com.abis.direnc.data.Session
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Tamamlanmış bir oturumun özeti. */
data class SessionSummary(
    val startedAt: Long,
    val endedAt: Long,
    val durationMillis: Long,
    val resistances: Int,
    val entries: Int,
)

/** Bir günün toplam çalışma süresi (grafik için). */
data class DayDuration(
    val date: LocalDate,
    val millis: Long,
)

object SessionStats {

    private fun localDate(epochMillis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    private fun durationOf(s: Session): Long =
        ((s.endedAt ?: s.startedAt) - s.startedAt).coerceAtLeast(0)

    /** Tamamlanmış oturumların özeti, en yeni başta. */
    fun summaries(
        sessions: List<Session>,
        resistanceSessionIds: List<Long>,
        entrySessionIds: List<Long>,
    ): List<SessionSummary> {
        val rCounts = resistanceSessionIds.groupingBy { it }.eachCount()
        val eCounts = entrySessionIds.groupingBy { it }.eachCount()
        return sessions
            .filter { it.endedAt != null }
            .sortedByDescending { it.startedAt }
            .map { s ->
                SessionSummary(
                    startedAt = s.startedAt,
                    endedAt = s.endedAt!!,
                    durationMillis = durationOf(s),
                    resistances = rCounts[s.id] ?: 0,
                    entries = eCounts[s.id] ?: 0,
                )
            }
    }

    fun totalStudyMillis(sessions: List<Session>): Long =
        sessions.filter { it.endedAt != null }.sumOf { durationOf(it) }

    fun studyMillisOnDay(sessions: List<Session>, now: Long, zone: ZoneId): Long {
        val today = localDate(now, zone)
        return sessions
            .filter { it.endedAt != null && localDate(it.startedAt, zone) == today }
            .sumOf { durationOf(it) }
    }

    fun studyMillisThisWeek(sessions: List<Session>, now: Long, zone: ZoneId): Long {
        val today = localDate(now, zone)
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return sessions
            .filter {
                it.endedAt != null &&
                    localDate(it.startedAt, zone).let { d -> !d.isBefore(weekStart) && !d.isAfter(today) }
            }
            .sumOf { durationOf(it) }
    }

    /** Son [days] gün (bugün dahil), en eskisi başta. */
    fun dailyStudyMillis(
        sessions: List<Session>,
        now: Long,
        zone: ZoneId,
        days: Int = 7,
    ): List<DayDuration> {
        val today = localDate(now, zone)
        val completed = sessions.filter { it.endedAt != null }
        return (days - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val millis = completed
                .filter { localDate(it.startedAt, zone) == date }
                .sumOf { durationOf(it) }
            DayDuration(date, millis)
        }
    }
}

/** Süreyi "1s 12dk" / "45dk" biçiminde gösterir. */
fun formatDuration(millis: Long): String {
    val totalMin = millis / 60_000
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h}s ${m}dk" else "${m}dk"
}
