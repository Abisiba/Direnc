package com.abis.direnc.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DirencRepositoryTest {

    private lateinit var db: DirencDatabase
    private lateinit var repo: DirencRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DirencDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = DirencRepository(db.sessionDao(), db.eventDao(), db.motivationDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `oturum baslat ve bitir`() = runTest {
        assertNull(repo.activeSession().first())
        repo.startSession(now = 1000L)
        val active = repo.activeSession().first()
        assertNotNull(active)
        assertEquals(1000L, active!!.startedAt)

        repo.endActiveSession(now = 5000L)
        assertNull(repo.activeSession().first())
    }

    @Test
    fun `direnis kaydedilir ve sayilir`() = runTest {
        repo.startSession(now = 1000L)
        repo.recordResistance(now = 1100L)
        repo.recordResistance(now = 1200L)
        assertEquals(2, repo.resistanceTimestamps().first().size)
    }

    @Test
    fun `giris kaydedilir`() = runTest {
        repo.recordEntry(now = 2000L)
        assertEquals(listOf(2000L), repo.entryTimestamps().first())
    }
}
