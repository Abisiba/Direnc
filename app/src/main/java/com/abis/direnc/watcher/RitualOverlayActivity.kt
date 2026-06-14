package com.abis.direnc.watcher

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abis.direnc.DirencApp
import com.abis.direnc.data.DirencRepository
import com.abis.direnc.ui.ritual.RitualActions
import com.abis.direnc.ui.ritual.RitualScreen
import com.abis.direnc.ui.ritual.RitualViewModel
import com.abis.direnc.ui.theme.DirencTheme

/**
 * Kilit açılışında her şeyin önüne gelen ritüel ekranı. Plan 1'deki RitualScreen'i
 * yeniden kullanır. PASSED → telefon serbest (bu açılış için). GAVE_UP → ekran kilitlenir.
 */
class RitualOverlayActivity : ComponentActivity() {

    private val repo: DirencRepository by lazy {
        val db = (application as DirencApp).database
        DirencRepository(db.sessionDao(), db.eventDao(), db.motivationDao())
    }
    private val now: () -> Long = { System.currentTimeMillis() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kilit ekranının üstünde göster ve ekranı uyandır
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        WatcherState.ritualPending = true

        setContent {
            DirencTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val vm = viewModel<RitualViewModel> { RitualViewModel(actions()) }
                    RitualScreen(
                        viewModel = vm,
                        onPassed = {
                            WatcherState.passedThisUnlock = true
                            WatcherState.ritualPending = false
                            finish()
                        },
                        onGaveUp = {
                            WatcherState.ritualPending = false
                            GuardAccessibilityService.instance?.lockScreen()
                            finish()
                        },
                    )
                }
            }
        }
    }

    // Geri tuşuyla kaçışı engelle (ritüel beklerken)
    @Suppress("MissingSuperCall", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() { /* yut: geri ile çıkılamaz */ }

    private fun actions() = object : RitualActions {
        override suspend fun randomMotivationText() = repo.randomMotivationText()
        override suspend fun recordResistance() = repo.recordResistance(now())
        override suspend fun recordEntry() = repo.recordEntry(now())
    }

    companion object {
        fun launch(context: Context) {
            val intent = Intent(context, RitualOverlayActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            context.startActivity(intent)
        }
    }
}
