package com.abis.direnc.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abis.direnc.ui.theme.Spacing

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    permissionsReady: Boolean,
    onStartRitual: () -> Unit,
    onStartSessionGuarded: () -> Unit,
    onOpenOnboarding: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Direnç",
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(Modifier.height(Spacing.xl))

        if (state.sessionActive) {
            Text("Çalışma oturumu aktif", textAlign = TextAlign.Center)
            Spacer(Modifier.height(Spacing.lg))
            // Plan 1'de ritüel uygulama içi butonla tetiklenir (Plan 2'de otomatik olacak)
            Button(onClick = onStartRitual) { Text("Telefonu açmak istiyorum") }
            Spacer(Modifier.height(Spacing.md))
            // Plan 1'de oturum doğrudan bitirilebilir. Plan 2'de (gözcü) bitirme
            // ritüelin arkasına alınacak; böylece "kaçış kapısı" olmayacak (spec §3.5).
            OutlinedButton(onClick = { viewModel.endSession() }) { Text("Oturumu bitir") }
        } else {
            Text("Bugün ${state.todayResistance} kez direndin · Seri: ${state.streak} gün",
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(Spacing.lg))
            Button(onClick = {
                if (permissionsReady) onStartSessionGuarded() else onOpenOnboarding()
            }) { Text("Çalışmaya başla") }
        }

        Spacer(Modifier.height(Spacing.xxl))
        TextButton(onClick = onOpenStats) { Text("Takip") }
        TextButton(onClick = onOpenSettings) { Text("Ayarlar") }
    }
}
