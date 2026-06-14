package com.abis.direnc.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abis.direnc.data.Session
import com.abis.direnc.domain.Stats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId

interface HomeSource {
    fun activeSession(): Flow<Session?>
    fun resistanceTimestamps(): Flow<List<Long>>
    suspend fun startSession(now: Long)
    suspend fun endActiveSession(now: Long)
}

data class HomeUiState(
    val sessionActive: Boolean = false,
    val sessionStartedAt: Long? = null,
    val todayResistance: Int = 0,
    val streak: Int = 0,
)

class HomeViewModel(
    private val source: HomeSource,
    private val zone: ZoneId,
    private val now: () -> Long,
) : ViewModel() {

    val state: StateFlow<HomeUiState> =
        combine(source.activeSession(), source.resistanceTimestamps()) { session, resistance ->
            HomeUiState(
                sessionActive = session != null,
                sessionStartedAt = session?.startedAt,
                todayResistance = Stats.countOnDay(resistance, now(), zone),
                streak = Stats.streak(resistance, now(), zone),
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    fun startSession() = viewModelScope.launch { source.startSession(now()) }
    fun endSession() = viewModelScope.launch { source.endActiveSession(now()) }
}
