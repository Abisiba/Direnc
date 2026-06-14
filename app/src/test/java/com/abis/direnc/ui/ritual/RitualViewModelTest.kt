package com.abis.direnc.ui.ritual

import com.abis.direnc.domain.MathProblem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RitualViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakeActions : RitualActions {
        var resistanceCount = 0
        var entryCount = 0
        override suspend fun randomMotivationText() = "Dayan."
        override suspend fun recordResistance() { resistanceCount++ }
        override suspend fun recordEntry() { entryCount++ }
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(actions: RitualActions = FakeActions()) =
        RitualViewModel(actions, fixedProblem = MathProblem("2 × 2 = ?", 4))

    @Test
    fun `baslangic adimi motivasyon metnidir`() = runTest {
        val vm = vm()
        advanceUntilIdle()
        assertEquals(0, vm.state.value.stepIndex)
        assertEquals("Dayan.", vm.state.value.motivationText)
    }

    @Test
    fun `tum adimlar gecilince PASSED ve giris kaydi`() = runTest {
        val actions = FakeActions()
        val vm = vm(actions)
        advanceUntilIdle()
        vm.onMotivationContinue()
        vm.onConfrontSubmit("Acil arama")
        vm.onMathSubmit("4")
        advanceUntilIdle()
        assertEquals(RitualResult.PASSED, vm.state.value.result)
        assertEquals(1, actions.entryCount)
    }

    @Test
    fun `yanlis matematik hata gosterir ve gecmez`() = runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.onMotivationContinue()
        vm.onConfrontSubmit("sebep")
        vm.onMathSubmit("5")
        advanceUntilIdle()
        assertTrue(vm.state.value.mathError)
        assertEquals(null, vm.state.value.result)
    }

    @Test
    fun `bos yuzlestirme cevabi gecmez`() = runTest {
        val vm = vm()
        advanceUntilIdle()
        vm.onMotivationContinue()
        vm.onConfrontSubmit("   ")
        assertEquals(1, vm.state.value.stepIndex) // hâlâ yüzleştirme adımında
    }

    @Test
    fun `vazgec direnis kaydeder ve GAVE_UP olur`() = runTest {
        val actions = FakeActions()
        val vm = vm(actions)
        advanceUntilIdle()
        vm.onGiveUp()
        advanceUntilIdle()
        assertEquals(RitualResult.GAVE_UP, vm.state.value.result)
        assertEquals(1, actions.resistanceCount)
    }
}
