# Direnç — Plan 3: Oturum Geçmişi & Süre Takibi (Implementation Plan)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Takip ekranını zenginleştirmek: toplam/bugün/bu hafta **çalışma süresi**, son 7 günün **çubuk grafiği** ve **oturum listesi** (tarih · süre · direniş/giriş). Tüm hesap mantığı saf Kotlin'de ve TDD ile test edilir. İnternet izni eklenmez.

**Architecture:** Mevcut Plan 1/2 mimarisi korunur. Yeni saf domain (`SessionStats`) oturum özetlerini ve süre toplamlarını hesaplar (TDD). Room'a tamamlanmış oturumları ve olayların oturum kimliklerini döndüren Flow sorguları eklenir. `StatsViewModel` bunları `combine` ile birleştirir; `StatsScreen` kaydırılabilir hâle gelir ve süre özetleri + basit Compose çubuk grafiği + oturum listesi gösterir.

**Tech Stack:** Plan 1/2 ile aynı (Kotlin, Compose, Room, coroutines/Flow, JUnit4).

**Önkoşul:** Plan 1 + Plan 2 tamamlanmış (bu makinede tamam). Paket: `com.abis.direnc`.

---

## Dosya Yapısı

```
app/src/main/java/com/abis/direnc/
  data/Daos.kt                 # MODIFY: completedSessions(), resistanceSessionIds(), entrySessionIds()
  data/DirencRepository.kt     # MODIFY: yukarıdakileri açan Flow'lar
  domain/SessionStats.kt       # CREATE: SessionSummary, DayDuration, SessionStats, formatDuration (saf, TDD)
  ui/stats/StatsViewModel.kt   # MODIFY: StatsSource + StatsUiState genişletilir
  ui/stats/StatsScreen.kt      # MODIFY: kaydırılabilir; süre özetleri + grafik + oturum listesi
  MainActivity.kt              # MODIFY: repoAsStatsSource() yeni alanları bağlar
app/src/test/java/com/abis/direnc/
  domain/SessionStatsTest.kt   # CREATE (TDD)
```

---

## Task 1: Veri katmanı — DAO sorguları + repository Flow'ları

**Files:**
- Modify: `app/src/main/java/com/abis/direnc/data/Daos.kt`
- Modify: `app/src/main/java/com/abis/direnc/data/DirencRepository.kt`

- [ ] **Step 1: SessionDao'ya tamamlanmış oturum sorgusu ekle**

`app/src/main/java/com/abis/direnc/data/Daos.kt` — `SessionDao` içine, `activeSessionOnce()`'tan sonra ekle:
```kotlin
    @Query("SELECT * FROM sessions WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun completedSessions(): Flow<List<Session>>
```

- [ ] **Step 2: EventDao'ya oturum kimliği sorgularını ekle**

`app/src/main/java/com/abis/direnc/data/Daos.kt` — `EventDao` içine, `entryTimestamps()`'tan sonra ekle:
```kotlin
    @Query("SELECT sessionId FROM resistance_events WHERE sessionId IS NOT NULL")
    fun resistanceSessionIds(): Flow<List<Long>>

    @Query("SELECT sessionId FROM entry_events WHERE sessionId IS NOT NULL")
    fun entrySessionIds(): Flow<List<Long>>
```

- [ ] **Step 3: Repository'e Flow'ları ekle**

`app/src/main/java/com/abis/direnc/data/DirencRepository.kt` — sınıfın en üstündeki diğer Flow tanımlarının (örn. `entryTimestamps()`) yanına ekle:
```kotlin
    fun completedSessions(): Flow<List<Session>> = sessionDao.completedSessions()
    fun resistanceSessionIds(): Flow<List<Long>> = eventDao.resistanceSessionIds()
    fun entrySessionIds(): Flow<List<Long>> = eventDao.entrySessionIds()
```

- [ ] **Step 4: Derle**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (Room yeni sorguları üretir)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/abis/direnc/data/Daos.kt app/src/main/java/com/abis/direnc/data/DirencRepository.kt
git commit -m "feat(geçmiş): tamamlanmış oturum + olay-oturum kimliği sorguları"
```

---

## Task 2: Domain — SessionStats (saf mantık, TDD)

**Files:**
- Create: `app/src/main/java/com/abis/direnc/domain/SessionStats.kt`
- Test: `app/src/test/java/com/abis/direnc/domain/SessionStatsTest.kt`

- [ ] **Step 1: Başarısız testi yaz**

`app/src/test/java/com/abis/direnc/domain/SessionStatsTest.kt`:
```kotlin
package com.abis.direnc.domain

import com.abis.direnc.data.Session
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SessionStatsTest {

    private val zone: ZoneId = ZoneId.of("Europe/Istanbul")

    private fun day(y: Int, m: Int, d: Int, hour: Int = 12): Long =
        LocalDate.of(y, m, d).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private val now = day(2026, 6, 14, 18) // Pazar

    @Test fun `summaries tamamlanmamis oturumu eler ve en yeni basta siralar`() {
        val sessions = listOf(
            Session(id = 1, startedAt = day(2026, 6, 12), endedAt = day(2026, 6, 12) + 3_600_000),
            Session(id = 2, startedAt = day(2026, 6, 14), endedAt = null), // aktif → elenir
            Session(id = 3, startedAt = day(2026, 6, 13), endedAt = day(2026, 6, 13) + 1_800_000),
        )
        val result = SessionStats.summaries(sessions, listOf(1L, 1L, 3L), listOf(3L))
        assertEquals(2, result.size)
        assertEquals(day(2026, 6, 13), result[0].startedAt) // en yeni (id3) başta
        assertEquals(1, result[0].entries)      // id3'te 1 giriş
        assertEquals(1, result[0].resistances)  // id3'te 1 direniş
        assertEquals(2, result[1].resistances)  // id1'de 2 direniş
        assertEquals(3_600_000L, result[1].durationMillis)
    }

    @Test fun `totalStudyMillis tamamlanmis oturum surelerini toplar`() {
        val sessions = listOf(
            Session(id = 1, startedAt = 0, endedAt = 1_000_000),
            Session(id = 2, startedAt = 0, endedAt = null),         // elenir
            Session(id = 3, startedAt = 0, endedAt = 2_000_000),
        )
        assertEquals(3_000_000L, SessionStats.totalStudyMillis(sessions))
    }

    @Test fun `studyMillisOnDay yalnizca o gun baslayan oturumlari toplar`() {
        val sessions = listOf(
            Session(id = 1, startedAt = day(2026, 6, 14, 9), endedAt = day(2026, 6, 14, 10)),  // bugün 1 saat
            Session(id = 2, startedAt = day(2026, 6, 13, 9), endedAt = day(2026, 6, 13, 10)),  // dün
        )
        assertEquals(3_600_000L, SessionStats.studyMillisOnDay(sessions, now, zone))
    }

    @Test fun `dailyStudyMillis son 7 gunu eskiden yeniye dondurur`() {
        val sessions = listOf(
            Session(id = 1, startedAt = day(2026, 6, 14, 9), endedAt = day(2026, 6, 14, 10)),
        )
        val result = SessionStats.dailyStudyMillis(sessions, now, zone, days = 7)
        assertEquals(7, result.size)
        assertEquals(LocalDate.of(2026, 6, 8), result.first().date)   // en eski
        assertEquals(LocalDate.of(2026, 6, 14), result.last().date)   // bugün
        assertEquals(3_600_000L, result.last().millis)
        assertEquals(0L, result.first().millis)
    }

    @Test fun `formatDuration saat ve dakikayi bicimler`() {
        assertEquals("0dk", formatDuration(0))
        assertEquals("45dk", formatDuration(45 * 60_000L))
        assertEquals("1s 12dk", formatDuration((72 * 60_000L)))
        assertEquals("2s 0dk", formatDuration(2 * 3_600_000L))
    }
}
```

- [ ] **Step 2: Testin başarısız olduğunu doğrula**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:testDebugUnitTest --tests "com.abis.direnc.domain.SessionStatsTest"`
Expected: FAIL — "Unresolved reference: SessionStats"

- [ ] **Step 3: SessionStats'i yaz**

`app/src/main/java/com/abis/direnc/domain/SessionStats.kt`:
```kotlin
package com.abis.direnc.domain

import com.abis.direnc.data.Session
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Tamamlanmış bir oturumun özeti. */
data class SessionSummary(
    val startedAt: Long,
    val endedAt: Long,
    val durationMillis: Long,
    val resistances: Int,
    val entries: Int,
)

/** Bir günün toplam çalışma süresi (grafik için). */
data class DayDuration(
    val date: LocalDate,
    val millis: Long,
)

object SessionStats {

    private fun localDate(epochMillis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    private fun durationOf(s: Session): Long =
        ((s.endedAt ?: s.startedAt) - s.startedAt).coerceAtLeast(0)

    /** Tamamlanmış oturumların özeti, en yeni başta. */
    fun summaries(
        sessions: List<Session>,
        resistanceSessionIds: List<Long>,
        entrySessionIds: List<Long>,
    ): List<SessionSummary> {
        val rCounts = resistanceSessionIds.groupingBy { it }.eachCount()
        val eCounts = entrySessionIds.groupingBy { it }.eachCount()
        return sessions
            .filter { it.endedAt != null }
            .sortedByDescending { it.startedAt }
            .map { s ->
                SessionSummary(
                    startedAt = s.startedAt,
                    endedAt = s.endedAt!!,
                    durationMillis = durationOf(s),
                    resistances = rCounts[s.id] ?: 0,
                    entries = eCounts[s.id] ?: 0,
                )
            }
    }

    fun totalStudyMillis(sessions: List<Session>): Long =
        sessions.filter { it.endedAt != null }.sumOf { durationOf(it) }

    fun studyMillisOnDay(sessions: List<Session>, now: Long, zone: ZoneId): Long {
        val today = localDate(now, zone)
        return sessions
            .filter { it.endedAt != null && localDate(it.startedAt, zone) == today }
            .sumOf { durationOf(it) }
    }

    fun studyMillisThisWeek(sessions: List<Session>, now: Long, zone: ZoneId): Long {
        val today = localDate(now, zone)
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return sessions
            .filter {
                it.endedAt != null &&
                    localDate(it.startedAt, zone).let { d -> !d.isBefore(weekStart) && !d.isAfter(today) }
            }
            .sumOf { durationOf(it) }
    }

    /** Son [days] gün (bugün dahil), en eskisi başta. */
    fun dailyStudyMillis(
        sessions: List<Session>,
        now: Long,
        zone: ZoneId,
        days: Int = 7,
    ): List<DayDuration> {
        val today = localDate(now, zone)
        val completed = sessions.filter { it.endedAt != null }
        return (days - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val millis = completed
                .filter { localDate(it.startedAt, zone) == date }
                .sumOf { durationOf(it) }
            DayDuration(date, millis)
        }
    }
}

/** Süreyi "1s 12dk" / "45dk" biçiminde gösterir. */
fun formatDuration(millis: Long): String {
    val totalMin = millis / 60_000
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h}s ${m}dk" else "${m}dk"
}
```

- [ ] **Step 4: Testin geçtiğini doğrula**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:testDebugUnitTest --tests "com.abis.direnc.domain.SessionStatsTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/abis/direnc/domain/SessionStats.kt app/src/test/java/com/abis/direnc/domain/SessionStatsTest.kt
git commit -m "feat(geçmiş): SessionStats saf hesaplar + süre biçimleme (TDD)"
```

---

## Task 3: StatsViewModel — kaynak ve durum genişletme

**Files:**
- Modify: `app/src/main/java/com/abis/direnc/ui/stats/StatsViewModel.kt`

- [ ] **Step 1: StatsViewModel'i güncelle (tüm dosyayı değiştir)**

`app/src/main/java/com/abis/direnc/ui/stats/StatsViewModel.kt`:
```kotlin
package com.abis.direnc.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abis.direnc.data.Session
import com.abis.direnc.domain.DayDuration
import com.abis.direnc.domain.SessionStats
import com.abis.direnc.domain.SessionSummary
import com.abis.direnc.domain.Stats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId

interface StatsSource {
    fun resistanceTimestamps(): Flow<List<Long>>
    fun entryTimestamps(): Flow<List<Long>>
    fun completedSessions(): Flow<List<Session>>
    fun resistanceSessionIds(): Flow<List<Long>>
    fun entrySessionIds(): Flow<List<Long>>
}

data class StatsUiState(
    val totalResistance: Int = 0,
    val todayResistance: Int = 0,
    val weekResistance: Int = 0,
    val streak: Int = 0,
    val totalEntries: Int = 0,
    val totalStudyMillis: Long = 0,
    val todayStudyMillis: Long = 0,
    val weekStudyMillis: Long = 0,
    val dailyDurations: List<DayDuration> = emptyList(),
    val sessions: List<SessionSummary> = emptyList(),
)

class StatsViewModel(
    source: StatsSource,
    zone: ZoneId,
    now: () -> Long,
) : ViewModel() {

    val state: StateFlow<StatsUiState> =
        combine(
            source.resistanceTimestamps(),
            source.entryTimestamps(),
            source.completedSessions(),
            source.resistanceSessionIds(),
            source.entrySessionIds(),
        ) { resistance, entries, sessions, rSessionIds, eSessionIds ->
            val t = now()
            StatsUiState(
                totalResistance = resistance.size,
                todayResistance = Stats.countOnDay(resistance, t, zone),
                weekResistance = Stats.countThisWeek(resistance, t, zone),
                streak = Stats.streak(resistance, t, zone),
                totalEntries = entries.size,
                totalStudyMillis = SessionStats.totalStudyMillis(sessions),
                todayStudyMillis = SessionStats.studyMillisOnDay(sessions, t, zone),
                weekStudyMillis = SessionStats.studyMillisThisWeek(sessions, t, zone),
                dailyDurations = SessionStats.dailyStudyMillis(sessions, t, zone, days = 7),
                sessions = SessionStats.summaries(sessions, rSessionIds, eSessionIds),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())
}
```

- [ ] **Step 2: Derle**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug`
Expected: Bu noktada `MainActivity` hâlâ eski `StatsSource`'u uyguladığı için derleme HATA verebilir ("object is not abstract / 3 üye eksik"). Bu beklenen — Task 4 Step 2'de MainActivity güncellenince düzelir. İstersen Task 3 ve 4'ü birlikte uygula, derlemeyi Task 4 sonunda yap.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/abis/direnc/ui/stats/StatsViewModel.kt
git commit -m "feat(geçmiş): StatsViewModel'e süre ve oturum özetleri"
```

---

## Task 4: StatsScreen (kaydırılabilir + grafik + liste) ve MainActivity bağlama

**Files:**
- Modify: `app/src/main/java/com/abis/direnc/ui/stats/StatsScreen.kt`
- Modify: `app/src/main/java/com/abis/direnc/MainActivity.kt`

- [ ] **Step 1: StatsScreen'i yeniden yaz (tüm dosyayı değiştir)**

`app/src/main/java/com/abis/direnc/ui/stats/StatsScreen.kt`:
```kotlin
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
```

- [ ] **Step 2: MainActivity'de StatsSource adaptörünü genişlet**

`app/src/main/java/com/abis/direnc/MainActivity.kt` — `repoAsStatsSource()` fonksiyonunu şununla değiştir:
```kotlin
    private fun repoAsStatsSource() = object : StatsSource {
        override fun resistanceTimestamps(): Flow<List<Long>> = repo.resistanceTimestamps()
        override fun entryTimestamps(): Flow<List<Long>> = repo.entryTimestamps()
        override fun completedSessions(): Flow<List<Session>> = repo.completedSessions()
        override fun resistanceSessionIds(): Flow<List<Long>> = repo.resistanceSessionIds()
        override fun entrySessionIds(): Flow<List<Long>> = repo.entrySessionIds()
    }
```
(`Session` importu MainActivity'de zaten var.)

- [ ] **Step 3: Derle**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Tüm birim testleri çalıştır (regresyon)**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:testDebugUnitTest`
Expected: tüm testler PASS (önceki 28 + SessionStats'in 5'i = 33)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/abis/direnc/ui/stats/StatsScreen.kt app/src/main/java/com/abis/direnc/MainActivity.kt
git commit -m "feat(geçmiş): Takip ekranına süre özetleri, 7 gün grafiği ve oturum listesi"
```

---

## Task 5: Cihazda doğrulama (bağlı telefon)

- [ ] **Step 1: Kur ve aç**

Run:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p com.abis.direnc -c android.intent.category.LAUNCHER 1
```

- [ ] **Step 2: Elle kontrol**

1. Bir-iki kısa oturum başlat/bitir (gözcüden "Çalışmam bitti" veya uygulamadan "Oturumu bitir").
2. Takip ekranını aç → "Çalışma süresi" (toplam/bugün/hafta) dolu görünmeli.
3. "Son 7 gün" çubuk grafiğinde bugüne ait çubuk görünmeli.
4. "Oturumlar" listesinde tamamlanan oturumlar (tarih · süre · direniş/giriş) en yeni başta listelenmeli.
5. Ekran kaydırılabilir olmalı.

---

## Tamamlanma Kriteri (Plan 3)

- [ ] `JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:testDebugUnitTest` tüm testler geçer (33).
- [ ] `assembleDebug` başarılı.
- [ ] Takip ekranında çalışma süresi özetleri, 7 günlük grafik ve oturum listesi görünür.
- [ ] Hâlâ internet izni yok.
