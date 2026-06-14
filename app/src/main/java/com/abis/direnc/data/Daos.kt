package com.abis.direnc.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert suspend fun insert(session: Session): Long
    @Update suspend fun update(session: Session)

    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun activeSession(): Flow<Session?>

    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun activeSessionOnce(): Session?

    @Query("SELECT * FROM sessions WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun completedSessions(): Flow<List<Session>>
}

@Dao
interface EventDao {
    @Insert suspend fun insertResistance(event: ResistanceEvent)
    @Insert suspend fun insertEntry(event: EntryEvent)

    @Query("SELECT timestamp FROM resistance_events ORDER BY timestamp ASC")
    fun resistanceTimestamps(): Flow<List<Long>>

    @Query("SELECT timestamp FROM entry_events ORDER BY timestamp ASC")
    fun entryTimestamps(): Flow<List<Long>>

    @Query("SELECT sessionId FROM resistance_events WHERE sessionId IS NOT NULL")
    fun resistanceSessionIds(): Flow<List<Long>>

    @Query("SELECT sessionId FROM entry_events WHERE sessionId IS NOT NULL")
    fun entrySessionIds(): Flow<List<Long>>
}

@Dao
interface MotivationDao {
    @Insert suspend fun insert(text: MotivationText)
    @Delete suspend fun delete(text: MotivationText)
    @Update suspend fun update(text: MotivationText)

    @Query("SELECT * FROM motivation_texts ORDER BY id ASC")
    fun all(): Flow<List<MotivationText>>

    @Query("SELECT * FROM motivation_texts ORDER BY id ASC")
    suspend fun allOnce(): List<MotivationText>

    @Query("SELECT COUNT(*) FROM motivation_texts")
    suspend fun count(): Int
}
