package com.abis.direnc.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abis.direnc.domain.DayDuration
import com.abis.direnc.domain.SessionSummary
import com.abis.direnc.domain.formatDuration
import com.abis.direnc.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StatsScreen(viewModel: StatsViewModel, onBack: () -> Unit) {
    val s by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item {
            Text("Takip", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(Spacing.lg))
        }

        item {
            StatRow("Toplam direniş", s.totalResistance.toString())
            StatRow("Bugün", s.todayResistance.toString())
            StatRow("Bu hafta", s.weekResistance.toString())
            StatRow("Seri (gün)", s.streak.toString())
            StatRow("Giriş (telefonu kullandığın)", s.totalEntries.toString())
        }

        item {
            Spacer(Modifier.height(Spacing.lg))
            Text("Çalışma süresi", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(Spacing.sm))
            StatRow("Toplam", formatDuration(s.totalStudyMillis))
            StatRow("Bugün", formatDuration(s.todayStudyMillis))
            StatRow("Bu hafta", formatDuration(s.weekStudyMillis))
        }

        item {
            Spacer(Modifier.height(Spacing.lg))
            Text("Son 7 gün", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(Spacing.sm))
            WeeklyBarChart(s.dailyDurations)
        }

        item {
            Spacer(Modifier.height(Spacing.lg))
            Text("Oturumlar", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(Spacing.sm))
            if (s.sessions.isEmpty()) {
                Text(
                    "Henüz tamamlanmış oturum yok.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(s.sessions) { session ->
            SessionRow(session)
        }

        item {
            Spacer(Modifier.height(Spacing.xxl))
            TextButton(onClick = onBack) { Text("Geri") }
        }
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

@Composable
private fun WeeklyBarChart(days: List<DayDuration>) {
    val maxMillis = (days.maxOfOrNull { it.millis } ?: 0L).coerceAtLeast(1L)
    val maxBarHeight = 96.dp
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { day ->
            val fraction = day.millis.toFloat() / maxMillis.toFloat()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(maxBarHeight * fraction)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    dayInitial(day),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun dayInitial(day: DayDuration): String {
    // Pzt Sal Çar... yerine tek harf: P S Ç P C C P (kısalık için gün-of-week kısa adı)
    val names = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
    return names[day.date.dayOfWeek.value - 1]
}

@Composable
private fun SessionRow(session: SessionSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatSessionDate(session.startedAt), style = MaterialTheme.typography.bodyLarge)
            Text(formatDuration(session.durationMillis), style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            "${session.resistances} direniş · ${session.entries} giriş",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale("tr"))

private fun formatSessionDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dateFormatter)
