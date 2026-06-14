package com.abis.direnc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.abis.direnc.data.DirencRepository
import com.abis.direnc.data.MotivationText
import com.abis.direnc.data.Session
import com.abis.direnc.ui.home.HomeScreen
import com.abis.direnc.ui.home.HomeSource
import com.abis.direnc.ui.home.HomeViewModel
import com.abis.direnc.ui.ritual.RitualActions
import com.abis.direnc.ui.ritual.RitualScreen
import com.abis.direnc.ui.ritual.RitualViewModel
import com.abis.direnc.ui.settings.SettingsScreen
import com.abis.direnc.ui.settings.SettingsSource
import com.abis.direnc.ui.settings.SettingsViewModel
import com.abis.direnc.ui.stats.StatsScreen
import com.abis.direnc.ui.stats.StatsSource
import com.abis.direnc.ui.stats.StatsViewModel
import com.abis.direnc.ui.theme.DirencTheme
import kotlinx.coroutines.flow.Flow
import java.time.ZoneId

class MainActivity : ComponentActivity() {

    private val repo: DirencRepository by lazy {
        val db = (application as DirencApp).database
        DirencRepository(db.sessionDao(), db.eventDao(), db.motivationDao())
    }
    private val zone: ZoneId = ZoneId.systemDefault()
    private val now: () -> Long = { System.currentTimeMillis() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DirencTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            val vm = viewModel<HomeViewModel> {
                                HomeViewModel(repoAsHomeSource(), zone, now)
                            }
                            HomeScreen(
                                viewModel = vm,
                                onStartRitual = { navController.navigate("ritual") },
                                onOpenStats = { navController.navigate("stats") },
                                onOpenSettings = { navController.navigate("settings") },
                            )
                        }
                        composable("ritual") {
                            val vm = viewModel<RitualViewModel> {
                                RitualViewModel(repoAsRitualActions())
                            }
                            RitualScreen(
                                viewModel = vm,
                                onPassed = { navController.popBackStack("home", inclusive = false) },
                                onGaveUp = { navController.popBackStack("home", inclusive = false) },
                            )
                        }
                        composable("stats") {
                            val vm = viewModel<StatsViewModel> {
                                StatsViewModel(repoAsStatsSource(), zone, now)
                            }
                            StatsScreen(vm) { navController.popBackStack() }
                        }
                        composable("settings") {
                            val vm = viewModel<SettingsViewModel> {
                                SettingsViewModel(repoAsSettingsSource())
                            }
                            SettingsScreen(vm) { navController.popBackStack() }
                        }
                    }
                }
            }
        }
    }

    // Repository'i her ekranın dar arayüzüne uyarlayan adaptörler
    private fun repoAsHomeSource() = object : HomeSource {
        override fun activeSession(): Flow<Session?> = repo.activeSession()
        override fun resistanceTimestamps(): Flow<List<Long>> = repo.resistanceTimestamps()
        override suspend fun startSession(now: Long) = repo.startSession(now)
        override suspend fun endActiveSession(now: Long) = repo.endActiveSession(now)
    }

    private fun repoAsRitualActions() = object : RitualActions {
        override suspend fun randomMotivationText() = repo.randomMotivationText()
        override suspend fun recordResistance() = repo.recordResistance(now())
        override suspend fun recordEntry() = repo.recordEntry(now())
    }

    private fun repoAsStatsSource() = object : StatsSource {
        override fun resistanceTimestamps(): Flow<List<Long>> = repo.resistanceTimestamps()
        override fun entryTimestamps(): Flow<List<Long>> = repo.entryTimestamps()
    }

    private fun repoAsSettingsSource() = object : SettingsSource {
        override fun motivationTexts(): Flow<List<MotivationText>> = repo.motivationTexts()
        override suspend fun addMotivation(text: String) = repo.addMotivation(text)
        override suspend fun updateMotivation(text: MotivationText) = repo.updateMotivation(text)
        override suspend fun deleteMotivation(text: MotivationText) = repo.deleteMotivation(text)
    }
}
