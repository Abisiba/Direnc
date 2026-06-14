package com.abis.direnc.ui.settings

import com.abis.direnc.data.MotivationText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakeSource : SettingsSource {
        val store = MutableStateFlow<List<MotivationText>>(emptyList())
        var added: String? = null
        var updated: MotivationText? = null
        var deleted: MotivationText? = null
        override fun motivationTexts() = store
        override suspend fun addMotivation(text: String) { added = text }
        override suspend fun updateMotivation(text: MotivationText) { updated = text }
        override suspend fun deleteMotivation(text: MotivationText) { deleted = text }
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `add bos metni reddeder`() = runTest {
        val src = FakeSource()
        SettingsViewModel(src).add("   ")
        advanceUntilIdle()
        assertNull(src.added)
    }

    @Test
    fun `add metni trimler`() = runTest {
        val src = FakeSource()
        SettingsViewModel(src).add("  dayan  ")
        advanceUntilIdle()
        assertEquals("dayan", src.added)
    }

    @Test
    fun `edit yeni metinle gunceller ve trimler`() = runTest {
        val src = FakeSource()
        val item = MotivationText(id = 7, text = "eski")
        SettingsViewModel(src).edit(item, "  yeni  ")
        advanceUntilIdle()
        assertEquals(MotivationText(id = 7, text = "yeni"), src.updated)
    }

    @Test
    fun `edit bos metni reddeder`() = runTest {
        val src = FakeSource()
        SettingsViewModel(src).edit(MotivationText(id = 1, text = "x"), "  ")
        advanceUntilIdle()
        assertNull(src.updated)
    }

    @Test
    fun `delete kaynaga iletilir`() = runTest {
        val src = FakeSource()
        val item = MotivationText(id = 3, text = "sil beni")
        SettingsViewModel(src).delete(item)
        advanceUntilIdle()
        assertEquals(item, src.deleted)
    }
}
