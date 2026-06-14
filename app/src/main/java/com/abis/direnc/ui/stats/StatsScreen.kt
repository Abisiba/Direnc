package com.abis.direnc.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abis.direnc.ui.theme.Spacing

@Composable
fun StatsScreen(viewModel: StatsViewModel, onBack: () -> Unit) {
    val s by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl),
    ) {
        Text("Takip", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(Spacing.xl))

        StatRow("Toplam direniş", s.totalResistance.toString())
        StatRow("Bugün", s.todayResistance.toString())
        StatRow("Bu hafta", s.weekResistance.toString())
        StatRow("Seri (gün)", s.streak.toString())
        Spacer(Modifier.height(Spacing.lg))
        StatRow("Giriş (telefonu kullandığın)", s.totalEntries.toString())

        Spacer(Modifier.height(Spacing.xxl))
        TextButton(onClick = onBack) { Text("Geri") }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
