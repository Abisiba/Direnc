package com.abis.direnc.ui.home

import com.abis.direnc.data.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId

class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakeSource : HomeSource {
        val active = MutableStateFlow<Session?>(null)
        val resistance = MutableStateFlow<List<Long>>(emptyList())
        var started = false
        var ended = false
        override fun activeSession() = active
        override fun resistanceTimestamps() = resistance
        override suspend fun startSession(now: Long) { started = true }
        override suspend fun endActiveSession(now: Long) { ended = true }
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `aktif oturum yoksa sessionActive false`() = runTest {
        val src = FakeSource()
        val vm = HomeViewModel(src, ZoneId.of("Europe/Istanbul")) { 0L }
        advanceUntilIdle()
        assertFalse(vm.state.first().sessionActive)
    }

    @Test
    fun `aktif oturum varsa sessionActive true`() = runTest {
        val src = FakeSource()
        src.active.value = Session(id = 1, startedAt = 0L)
        val vm = HomeViewModel(src, ZoneId.of("Europe/Istanbul")) { 0L }
        advanceUntilIdle()
        assertTrue(vm.state.first().sessionActive)
    }

    @Test
    fun `startSession kaynaga iletilir`() = runTest {
        val src = FakeSource()
        val vm = HomeViewModel(src, ZoneId.of("Europe/Istanbul")) { 123L }
        vm.startSession()
        advanceUntilIdle()
        assertTrue(src.started)
    }

    @Test
    fun `bugunku direnis sayilir`() = runTest {
        val src = FakeSource()
        src.resistance.value = listOf(0L) // 1970-01-01; now da aynı gün
        val vm = HomeViewModel(src, ZoneId.of("UTC")) { 0L }
        advanceUntilIdle()
        assertEquals(1, vm.state.first().todayResistance)
    }
}
