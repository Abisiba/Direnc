package com.abis.direnc.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long? = null,
)

@Entity(tableName = "resistance_events")
data class ResistanceEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val sessionId: Long?,
)

@Entity(tableName = "entry_events")
data class EntryEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val sessionId: Long?,
)

@Entity(tableName = "motivation_texts")
data class MotivationText(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
)
