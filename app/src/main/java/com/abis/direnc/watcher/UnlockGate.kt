package com.abis.direnc.watcher

/** Kilit açıldığında ritüelin gösterilip gösterilmeyeceğine karar veren saf mantık. */
object UnlockGate {
    fun shouldShowRitual(sessionActive: Boolean, passedThisUnlock: Boolean): Boolean =
        sessionActive && !passedThisUnlock
}
