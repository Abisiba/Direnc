package com.abis.direnc.ui.ritual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abis.direnc.domain.MathProblem
import com.abis.direnc.domain.MathProblemGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface RitualActions {
    suspend fun randomMotivationText(): String?
    suspend fun recordResistance()
    suspend fun recordEntry()
}

enum class RitualResult { PASSED, GAVE_UP }

data class RitualUiState(
    val stepIndex: Int = 0,            // 0: motivasyon, 1: yüzleştirme, 2: matematik
    val motivationText: String = "",
    val problem: MathProblem = MathProblem("", 0),
    val mathError: Boolean = false,
    val result: RitualResult? = null,
)

class RitualViewModel(
    private val actions: RitualActions,
    fixedProblem: MathProblem? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(
        RitualUiState(problem = fixedProblem ?: MathProblemGenerator.generate())
    )
    val state: StateFlow<RitualUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val text = actions.randomMotivationText()
                ?: "Bir an dur. Bu dürtü geçecek."
            _state.update { it.copy(motivationText = text) }
        }
    }

    fun onMotivationContinue() {
        _state.update { if (it.stepIndex == 0) it.copy(stepIndex = 1) else it }
    }

    fun onConfrontSubmit(answer: String) {
        if (answer.isBlank()) return
        _state.update { if (it.stepIndex == 1) it.copy(stepIndex = 2) else it }
    }

    fun onMathSubmit(answer: String) {
        val value = answer.trim().toIntOrNull()
        if (value == _state.value.problem.answer) {
            viewModelScope.launch {
                actions.recordEntry()
                _state.update { it.copy(mathError = false, result = RitualResult.PASSED) }
            }
        } else {
            _state.update { it.copy(mathError = true) }
        }
    }

    fun onGiveUp() {
        viewModelScope.launch {
            actions.recordResistance()
            _state.update { it.copy(result = RitualResult.GAVE_UP) }
        }
    }
}
