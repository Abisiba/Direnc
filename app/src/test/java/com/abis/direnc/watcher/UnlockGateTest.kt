package com.abis.direnc.watcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlockGateTest {
    @Test fun `oturum aktif ve bu acilista gecilmediyse ritual gosterilir`() {
        assertTrue(UnlockGate.shouldShowRitual(sessionActive = true, passedThisUnlock = false))
    }
    @Test fun `oturum aktif ama bu acilista zaten gecildiyse gosterilmez`() {
        assertFalse(UnlockGate.shouldShowRitual(sessionActive = true, passedThisUnlock = true))
    }
    @Test fun `oturum aktif degilse gosterilmez`() {
        assertFalse(UnlockGate.shouldShowRitual(sessionActive = false, passedThisUnlock = false))
    }
}
