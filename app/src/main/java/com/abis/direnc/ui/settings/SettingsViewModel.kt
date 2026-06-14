package com.abis.direnc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abis.direnc.data.MotivationText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface SettingsSource {
    fun motivationTexts(): Flow<List<MotivationText>>
    suspend fun addMotivation(text: String)
    suspend fun updateMotivation(text: MotivationText)
    suspend fun deleteMotivation(text: MotivationText)
}

class SettingsViewModel(private val source: SettingsSource) : ViewModel() {

    val texts: StateFlow<List<MotivationText>> =
        source.motivationTexts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { source.addMotivation(text.trim()) }
    }

    fun edit(text: MotivationText, newText: String) {
        if (newText.isBlank()) return
        viewModelScope.launch { source.updateMotivation(text.copy(text = newText.trim())) }
    }

    fun delete(text: MotivationText) = viewModelScope.launch { source.deleteMotivation(text) }
}
