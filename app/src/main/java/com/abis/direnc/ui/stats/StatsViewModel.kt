package com.abis.direnc.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abis.direnc.data.Session
import com.abis.direnc.domain.DayDuration
import com.abis.direnc.domain.SessionStats
import com.abis.direnc.domain.SessionSummary
import com.abis.direnc.domain.Stats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId

interface StatsSource {
    fun resistanceTimestamps(): Flow<List<Long>>
    fun entryTimestamps(): Flow<List<Long>>
    fun completedSessions(): Flow<List<Session>>
    fun resistanceSessionIds(): Flow<List<Long>>
    fun entrySessionIds(): Flow<List<Long>>
}

data class StatsUiState(
    val totalResistance: Int = 0,
    val todayResistance: Int = 0,
    val weekResistance: Int = 0,
    val streak: Int = 0,
    val totalEntries: Int = 0,
    val totalStudyMillis: Long = 0,
    val todayStudyMillis: Long = 0,
    val weekStudyMillis: Long = 0,
    val dailyDurations: List<DayDuration> = emptyList(),
    val sessions: List<SessionSummary> = emptyList(),
)

class StatsViewModel(
    source: StatsSource,
    zone: ZoneId,
    now: () -> Long,
) : ViewModel() {

    val state: StateFlow<StatsUiState> =
        combine(
            source.resistanceTimestamps(),
            source.entryTimestamps(),
            source.completedSessions(),
            source.resistanceSessionIds(),
            source.entrySessionIds(),
        ) { resistance, entries, sessions, rSessionIds, eSessionIds ->
            val t = now()
            StatsUiState(
                totalResistance = resistance.size,
                todayResistance = Stats.countOnDay(resistance, t, zone),
                weekResistance = Stats.countThisWeek(resistance, t, zone),
                streak = Stats.streak(resistance, t, zone),
                totalEntries = entries.size,
                totalStudyMillis = SessionStats.totalStudyMillis(sessions),
                todayStudyMillis = SessionStats.studyMillisOnDay(sessions, t, zone),
                weekStudyMillis = SessionStats.studyMillisThisWeek(sessions, t, zone),
                dailyDurations = SessionStats.dailyStudyMillis(sessions, t, zone, days = 7),
                sessions = SessionStats.summaries(sessions, rSessionIds, eSessionIds),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())
}
