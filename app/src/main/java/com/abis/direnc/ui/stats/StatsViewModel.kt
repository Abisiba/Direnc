package com.abis.direnc.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
}

data class StatsUiState(
    val totalResistance: Int = 0,
    val todayResistance: Int = 0,
    val weekResistance: Int = 0,
    val streak: Int = 0,
    val totalEntries: Int = 0,
)

class StatsViewModel(
    source: StatsSource,
    zone: ZoneId,
    now: () -> Long,
) : ViewModel() {

    val state: StateFlow<StatsUiState> =
        combine(source.resistanceTimestamps(), source.entryTimestamps()) { resistance, entries ->
            val t = now()
            StatsUiState(
                totalResistance = resistance.size,
                todayResistance = Stats.countOnDay(resistance, t, zone),
                weekResistance = Stats.countThisWeek(resistance, t, zone),
                streak = Stats.streak(resistance, t, zone),
                totalEntries = entries.size,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())
}
