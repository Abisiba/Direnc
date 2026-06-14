package com.abis.direnc.watcher

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * İki görev:
 * 1) Ritüel beklemedeyken kullanıcı başka uygulamaya/launcher'a kaçarsa overlay'i geri getirir.
 * 2) "Vazgeçtim" deyince ekranı kilitler (GLOBAL_ACTION_LOCK_SCREEN, API 28+).
 */
class GuardAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!WatcherState.ritualPending) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            // Ritüel beklenirken öne kendi paketimiz dışında bir şey gelirse geri getir
            if (pkg != null && pkg != packageName) {
                RitualOverlayActivity.launch(this)
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    fun lockScreen() {
        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }

    companion object {
        @Volatile var instance: GuardAccessibilityService? = null
    }
}
