package com.abis.direnc.data

import kotlinx.coroutines.flow.Flow

class DirencRepository(
    private val sessionDao: SessionDao,
    private val eventDao: EventDao,
    private val motivationDao: MotivationDao,
) {
    fun activeSession(): Flow<Session?> = sessionDao.activeSession()
    fun resistanceTimestamps(): Flow<List<Long>> = eventDao.resistanceTimestamps()
    fun entryTimestamps(): Flow<List<Long>> = eventDao.entryTimestamps()
    fun motivationTexts(): Flow<List<MotivationText>> = motivationDao.all()

    suspend fun startSession(now: Long) {
        if (sessionDao.activeSessionOnce() == null) {
            sessionDao.insert(Session(startedAt = now))
        }
    }

    suspend fun endActiveSession(now: Long) {
        sessionDao.activeSessionOnce()?.let {
            sessionDao.update(it.copy(endedAt = now))
        }
    }

    suspend fun recordResistance(now: Long) {
        val sid = sessionDao.activeSessionOnce()?.id
        eventDao.insertResistance(ResistanceEvent(timestamp = now, sessionId = sid))
    }

    suspend fun recordEntry(now: Long) {
        val sid = sessionDao.activeSessionOnce()?.id
        eventDao.insertEntry(EntryEvent(timestamp = now, sessionId = sid))
    }

    suspend fun randomMotivationText(): String? =
        motivationDao.allOnce().randomOrNull()?.text

    suspend fun addMotivation(text: String) =
        motivationDao.insert(MotivationText(text = text))

    suspend fun deleteMotivation(text: MotivationText) =
        motivationDao.delete(text)

    suspend fun updateMotivation(text: MotivationText) =
        motivationDao.update(text)
}
