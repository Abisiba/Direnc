# Direnç — Plan 1: Çekirdek Uygulama (Implementation Plan)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Krem temalı, gizliliği koruyan (internet izni yok), uygulama içi bir butonla tetiklenen geçiş ritüeli (caydırıcı yazı → yüzleştiren soru → rastgele matematik → vazgeç/geç) ve direniş takibi olan, tam çalışan bir Android uygulaması kurmak.

**Architecture:** Tek Activity + Jetpack Compose UI. MVVM: pür mantık (matematik üreteci, istatistik/streak hesapları) saf Kotlin fonksiyonlarında ve TDD ile JVM testlidir; ViewModel'ler sahte (fake) repository ile JVM testlidir; Room DAO instrumented testle doğrulanır; ekranlar elle çalıştırılarak doğrulanır. İnternet izni hiç tanımlanmaz.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), Room (KSP), Kotlin Coroutines/Flow, JUnit4, kotlinx-coroutines-test, Robolectric (Room JVM testi için), AndroidX Navigation Compose.

**Paket adı:** `com.abis.direnc`

---

## Dosya Yapısı

```
app/
  build.gradle.kts
  src/main/
    AndroidManifest.xml
    java/com/abis/direnc/
      DirencApp.kt                      # Application sınıfı (DB singleton)
      MainActivity.kt                   # Tek Activity + NavHost
      data/
        entities.kt                     # Session, ResistanceEvent, EntryEvent, MotivationText
        Daos.kt                         # SessionDao, EventDao, MotivationDao
        DirencDatabase.kt               # RoomDatabase + seed
        DirencRepository.kt             # İş mantığı + Flow'lar
      domain/
        MathProblem.kt                  # MathProblem + MathProblemGenerator (saf)
        Stats.kt                        # total/today/week/streak (saf fonksiyonlar)
      ui/
        theme/Theme.kt                  # Krem renk paleti, tipografi, Theme
        theme/Spacing.kt                # Boşluk sabitleri
        home/HomeViewModel.kt
        home/HomeScreen.kt
        ritual/RitualViewModel.kt
        ritual/RitualScreen.kt
        stats/StatsViewModel.kt
        stats/StatsScreen.kt
        settings/SettingsViewModel.kt
        settings/SettingsScreen.kt
  src/test/java/com/abis/direnc/        # JVM birim testleri
    domain/MathProblemGeneratorTest.kt
    domain/StatsTest.kt
    data/DirencRepositoryTest.kt        # Robolectric + in-memory Room
    ui/ritual/RitualViewModelTest.kt
    ui/home/HomeViewModelTest.kt
```

---

## Task 1: Proje iskeleti ve Gradle yapılandırması

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts` (kök)
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/abis/direnc/DirencApp.kt`
- Create: `app/src/main/java/com/abis/direnc/MainActivity.kt`
- Create: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Kök Gradle dosyalarını oluştur**

`settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Direnc"
include(":app")
```

`build.gradle.kts` (kök):
```kotlin
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("com.google.devtools.ksp") version "2.0.20-1.0.25" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
}
```

`gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m
android.useAndroidX=true
kotlin.code.style=official
```

- [ ] **Step 2: app/build.gradle.kts oluştur**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.abis.direnc"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.abis.direnc"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.navigation:navigation-compose:2.8.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.room:room-testing:2.6.1")
}
```

- [ ] **Step 3: AndroidManifest.xml oluştur (İNTERNET İZNİ YOK)**

`app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:name=".DirencApp"
        android:allowBackup="false"
        android:label="Direnç"
        android:supportsRtl="true"
        android:theme="@style/Theme.Direnc">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Direnc">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 4: strings.xml ve placeholder tema stili oluştur**

`app/src/main/res/values/strings.xml`:
```xml
<resources>
    <string name="app_name">Direnç</string>
    <style name="Theme.Direnc" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

- [ ] **Step 5: Application sınıfını oluştur (DB Task 4'te eklenecek, şimdilik boş)**

`app/src/main/java/com/abis/direnc/DirencApp.kt`:
```kotlin
package com.abis.direnc

import android.app.Application

class DirencApp : Application()
```

- [ ] **Step 6: Geçici MainActivity oluştur**

`app/src/main/java/com/abis/direnc/MainActivity.kt`:
```kotlin
package com.abis.direnc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Text("Direnç") }
    }
}
```

- [ ] **Step 7: Derlemeyi doğrula**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (Gradle wrapper yoksa önce `gradle wrapper --gradle-version 8.9` çalıştırılır).

- [ ] **Step 8: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties app/
git commit -m "feat: Android proje iskeleti (internet izni yok)"
```

---

## Task 2: Krem tema (renkler, tipografi, boşluk)

**Files:**
- Create: `app/src/main/java/com/abis/direnc/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/abis/direnc/ui/theme/Spacing.kt`

- [ ] **Step 1: Boşluk sabitlerini oluştur**

`app/src/main/java/com/abis/direnc/ui/theme/Spacing.kt`:
```kotlin
package com.abis.direnc.ui.theme

import androidx.compose.ui.unit.dp

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 40.dp
    val xxl = 64.dp   // ferah "boşluk hissi" için bol dış kenar boşluğu
}
```

- [ ] **Step 2: Krem temayı oluştur**

`app/src/main/java/com/abis/direnc/ui/theme/Theme.kt`:
```kotlin
package com.abis.direnc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Sıcak krem palet: kâğıt zemin, koyu mürekkep yazı, tek yumuşak vurgu (toprak/terra)
private val CreamBackground = Color(0xFFF4ECDD)
private val CreamSurface = Color(0xFFFBF6EA)
private val InkPrimaryText = Color(0xFF3A352C)
private val SoftAccent = Color(0xFFB07D56)
private val OnAccent = Color(0xFFFBF6EA)
private val MutedText = Color(0xFF8A8275)

private val DirencColors = lightColorScheme(
    primary = SoftAccent,
    onPrimary = OnAccent,
    background = CreamBackground,
    onBackground = InkPrimaryText,
    surface = CreamSurface,
    onSurface = InkPrimaryText,
    surfaceVariant = CreamBackground,
    onSurfaceVariant = MutedText,
    outline = MutedText,
)

// İnce, sakin tipografi
private val DirencTypography = Typography(
    headlineLarge = Typography().headlineLarge.copy(fontWeight = FontWeight.Light, fontSize = 34.sp),
    headlineSmall = Typography().headlineSmall.copy(fontWeight = FontWeight.Light),
    bodyLarge = Typography().bodyLarge.copy(fontWeight = FontWeight.Normal, fontSize = 18.sp),
    labelLarge = Typography().labelLarge.copy(fontWeight = FontWeight.Normal),
)

@Composable
fun DirencTheme(content: @Composable () -> Unit) {
    // Bilinçli olarak her zaman aydınlık krem tema (göz yormayan, ferah his)
    @Suppress("UNUSED_EXPRESSION")
    isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = DirencColors,
        typography = DirencTypography,
        content = content
    )
}
```

- [ ] **Step 3: Derlemeyi doğrula**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/abis/direnc/ui/theme/
git commit -m "feat: krem minimal tema (renk, tipografi, boşluk)"
```

---

## Task 3: Matematik üreteci (saf mantık, TDD)

**Files:**
- Create: `app/src/main/java/com/abis/direnc/domain/MathProblem.kt`
- Test: `app/src/test/java/com/abis/direnc/domain/MathProblemGeneratorTest.kt`

- [ ] **Step 1: Başarısız testi yaz**

`app/src/test/java/com/abis/direnc/domain/MathProblemGeneratorTest.kt`:
```kotlin
package com.abis.direnc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MathProblemGeneratorTest {

    @Test
    fun `answer her zaman question ile tutarli`() {
        // 200 farklı tohumda üretilen sorunun cevabı doğru olmalı
        repeat(200) { seed ->
            val p = MathProblemGenerator.generate(Random(seed.toLong()))
            assertEquals(
                "Soru: ${p.question}",
                evaluate(p.question),
                p.answer
            )
        }
    }

    @Test
    fun `orta zorluk araliginda kalir`() {
        repeat(200) { seed ->
            val p = MathProblemGenerator.generate(Random(seed.toLong()))
            // Sonuç negatif olmamalı ve makul büyüklükte kalmalı (kafadan/kâğıtla)
            assertTrue("Cevap negatif olmamalı: ${p.question}", p.answer >= 0)
            assertTrue("Cevap çok büyük olmamalı: ${p.question}", p.answer <= 100000)
        }
    }

    // Test yardımcı: "a op b" stringini değerlendirir (yalnızca test amaçlı)
    private fun evaluate(q: String): Int {
        val cleaned = q.removeSuffix(" = ?").trim()
        return when {
            cleaned.contains('×') -> {
                val (a, b) = cleaned.split('×').map { it.trim().toInt() }
                a * b
            }
            cleaned.contains('+') -> {
                val (a, b) = cleaned.split('+').map { it.trim().toInt() }
                a + b
            }
            cleaned.contains('-') -> {
                val (a, b) = cleaned.split('-').map { it.trim().toInt() }
                a - b
            }
            else -> error("Bilinmeyen işlem: $cleaned")
        }
    }
}
```

- [ ] **Step 2: Testin başarısız olduğunu doğrula**

Run: `./gradlew :app:testDebugUnitTest --tests "com.abis.direnc.domain.MathProblemGeneratorTest"`
Expected: FAIL — "Unresolved reference: MathProblemGenerator"

- [ ] **Step 3: Minimal implementasyonu yaz**

`app/src/main/java/com/abis/direnc/domain/MathProblem.kt`:
```kotlin
package com.abis.direnc.domain

import kotlin.random.Random

data class MathProblem(val question: String, val answer: Int)

object MathProblemGenerator {

    /**
     * Orta zorlukta, hesap makinesi gerektirmeyen ama düşündüren bir işlem üretir.
     * Üç tür: 2 basamaklı × 1 basamaklı, 3 basamaklı + 3 basamaklı, 3 basamaklı - 3 basamaklı.
     */
    fun generate(random: Random = Random.Default): MathProblem {
        return when (random.nextInt(3)) {
            0 -> {
                val a = random.nextInt(12, 100)   // 12..99
                val b = random.nextInt(3, 10)     // 3..9
                MathProblem("$a × $b = ?", a * b)
            }
            1 -> {
                val a = random.nextInt(100, 1000)
                val b = random.nextInt(100, 1000)
                MathProblem("$a + $b = ?", a + b)
            }
            else -> {
                val hi = random.nextInt(500, 1000)
                val lo = random.nextInt(100, 500)
                MathProblem("$hi - $lo = ?", hi - lo)
            }
        }
    }
}
```

- [ ] **Step 4: Testin geçtiğini doğrula**

Run: `./gradlew :app:testDebugUnitTest --tests "com.abis.direnc.domain.MathProblemGeneratorTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/abis/direnc/domain/MathProblem.kt app/src/test/java/com/abis/direnc/domain/MathProblemGeneratorTest.kt
git commit -m "feat: rastgele orta-zorluk matematik üreteci (TDD)"
```

---

## Task 4: Veri katmanı — Room entity, DAO, database

**Files:**
- Create: `app/src/main/java/com/abis/direnc/data/entities.kt`
- Create: `app/src/main/java/com/abis/direnc/data/Daos.kt`
- Create: `app/src/main/java/com/abis/direnc/data/DirencDatabase.kt`
- Modify: `app/src/main/java/com/abis/direnc/DirencApp.kt`

- [ ] **Step 1: Entity'leri oluştur**

`app/src/main/java/com/abis/direnc/data/entities.kt`:
```kotlin
package com.abis.direnc.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long? = null,
)

@Entity(tableName = "resistance_events")
data class ResistanceEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val sessionId: Long?,
)

@Entity(tableName = "entry_events")
data class EntryEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val sessionId: Long?,
)

@Entity(tableName = "motivation_texts")
data class MotivationText(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
)
```

- [ ] **Step 2: DAO'ları oluştur**

`app/src/main/java/com/abis/direnc/data/Daos.kt`:
```kotlin
package com.abis.direnc.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert suspend fun insert(session: Session): Long
    @Update suspend fun update(session: Session)

    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun activeSession(): Flow<Session?>

    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun activeSessionOnce(): Session?
}

@Dao
interface EventDao {
    @Insert suspend fun insertResistance(event: ResistanceEvent)
    @Insert suspend fun insertEntry(event: EntryEvent)

    @Query("SELECT timestamp FROM resistance_events ORDER BY timestamp ASC")
    fun resistanceTimestamps(): Flow<List<Long>>

    @Query("SELECT timestamp FROM entry_events ORDER BY timestamp ASC")
    fun entryTimestamps(): Flow<List<Long>>
}

@Dao
interface MotivationDao {
    @Insert suspend fun insert(text: MotivationText)
    @Delete suspend fun delete(text: MotivationText)
    @Update suspend fun update(text: MotivationText)

    @Query("SELECT * FROM motivation_texts ORDER BY id ASC")
    fun all(): Flow<List<MotivationText>>

    @Query("SELECT * FROM motivation_texts ORDER BY id ASC")
    suspend fun allOnce(): List<MotivationText>

    @Query("SELECT COUNT(*) FROM motivation_texts")
    suspend fun count(): Int
}
```

- [ ] **Step 3: Database sınıfını oluştur (varsayılan metin tohumu ile)**

`app/src/main/java/com/abis/direnc/data/DirencDatabase.kt`:
```kotlin
package com.abis.direnc.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Session::class, ResistanceEvent::class, EntryEvent::class, MotivationText::class],
    version = 1,
    exportSchema = false,
)
abstract class DirencDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun eventDao(): EventDao
    abstract fun motivationDao(): MotivationDao

    companion object {
        @Volatile private var INSTANCE: DirencDatabase? = null

        val DEFAULT_TEXTS = listOf(
            "Bu dürtü birazdan geçecek. Sen kalıcı şeyler inşa ediyorsun.",
            "Sınav günü, şu an verdiğin karara teşekkür edecek.",
            "Telefon kaçmıyor. Ama bu odak anı kaçıyor.",
        )

        fun get(context: Context): DirencDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DirencDatabase::class.java,
                    "direnc.db"
                ).build().also { INSTANCE = it }
            }
    }
}
```

- [ ] **Step 4: Application'da DB'yi hazır et ve varsayılan metinleri tohumla**

`app/src/main/java/com/abis/direnc/DirencApp.kt` (tüm dosyayı değiştir):
```kotlin
package com.abis.direnc

import android.app.Application
import com.abis.direnc.data.DirencDatabase
import com.abis.direnc.data.MotivationText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DirencApp : Application() {
    val database by lazy { DirencDatabase.get(this) }

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            val dao = database.motivationDao()
            if (dao.count() == 0) {
                DirencDatabase.DEFAULT_TEXTS.forEach { dao.insert(MotivationText(text = it)) }
            }
        }
    }
}
```

- [ ] **Step 5: Derlemeyi doğrula (Room şeması üretilmeli)**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (KSP, Room implementasyonlarını üretir)

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/abis/direnc/data/ app/src/main/java/com/abis/direnc/DirencApp.kt
git commit -m "feat: Room veri katmanı (oturum, olaylar, metinler) + varsayılan tohum"
```

---

## Task 5: İstatistik hesapları (saf fonksiyonlar, TDD)

**Files:**
- Create: `app/src/main/java/com/abis/direnc/domain/Stats.kt`
- Test: `app/src/test/java/com/abis/direnc/domain/StatsTest.kt`

- [ ] **Step 1: Başarısız testi yaz**

`app/src/test/java/com/abis/direnc/domain/StatsTest.kt`:
```kotlin
package com.abis.direnc.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StatsTest {

    private val zone: ZoneId = ZoneId.of("Europe/Istanbul")

    // Belirli bir yerel tarihte gün ortasının epoch-millis değeri
    private fun day(year: Int, month: Int, dayOfMonth: Int): Long =
        LocalDate.of(year, month, dayOfMonth)
            .atTime(12, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    private val now = day(2026, 6, 14)

    @Test
    fun `bugun sayisi yalnizca bugunku olaylari sayar`() {
        val ts = listOf(day(2026, 6, 14), day(2026, 6, 14), day(2026, 6, 13))
        assertEquals(2, Stats.countOnDay(ts, now, zone))
    }

    @Test
    fun `bu hafta pazartesiden itibaren sayar`() {
        // 2026-06-14 Pazar. Haftanın başı (Pzt) 2026-06-08.
        val ts = listOf(
            day(2026, 6, 8),   // Pzt - dahil
            day(2026, 6, 14),  // Paz - dahil
            day(2026, 6, 7),   // önceki Paz - hariç
        )
        assertEquals(2, Stats.countThisWeek(ts, now, zone))
    }

    @Test
    fun `streak bugun dahil ust uste gunleri sayar`() {
        val ts = listOf(day(2026, 6, 14), day(2026, 6, 13), day(2026, 6, 12))
        assertEquals(3, Stats.streak(ts, now, zone))
    }

    @Test
    fun `streak bir gun bos kalinca kirilir`() {
        val ts = listOf(day(2026, 6, 14), day(2026, 6, 12)) // 13 boş
        assertEquals(1, Stats.streak(ts, now, zone))
    }

    @Test
    fun `streak dun bitiyorsa da sayar (bugun henuz olay yok)`() {
        val ts = listOf(day(2026, 6, 13), day(2026, 6, 12))
        assertEquals(2, Stats.streak(ts, now, zone))
    }

    @Test
    fun `hic olay yoksa streak sifir`() {
        assertEquals(0, Stats.streak(emptyList(), now, zone))
    }
}
```

- [ ] **Step 2: Testin başarısız olduğunu doğrula**

Run: `./gradlew :app:testDebugUnitTest --tests "com.abis.direnc.domain.StatsTest"`
Expected: FAIL — "Unresolved reference: Stats"

- [ ] **Step 3: Minimal implementasyonu yaz**

`app/src/main/java/com/abis/direnc/domain/Stats.kt`:
```kotlin
package com.abis.direnc.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

object Stats {

    private fun toLocalDate(epochMillis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    fun countOnDay(timestamps: List<Long>, now: Long, zone: ZoneId): Int {
        val today = toLocalDate(now, zone)
        return timestamps.count { toLocalDate(it, zone) == today }
    }

    fun countThisWeek(timestamps: List<Long>, now: Long, zone: ZoneId): Int {
        val today = toLocalDate(now, zone)
        val firstDayOfWeek = WeekFields.of(Locale("tr")).firstDayOfWeek // Pazartesi
        val weekStart = today.with(firstDayOfWeek)
        return timestamps.count {
            val d = toLocalDate(it, zone)
            !d.isBefore(weekStart) && !d.isAfter(today)
        }
    }

    /**
     * Bugünden (ya da olay bugün yoksa dünden) geriye doğru, üst üste en az bir
     * olay olan gün sayısı.
     */
    fun streak(timestamps: List<Long>, now: Long, zone: ZoneId): Int {
        if (timestamps.isEmpty()) return 0
        val days = timestamps.map { toLocalDate(it, zone) }.toSet()
        val today = toLocalDate(now, zone)

        var cursor = when {
            days.contains(today) -> today
            days.contains(today.minusDays(1)) -> today.minusDays(1)
            else -> return 0
        }
        var count = 0
        while (days.contains(cursor)) {
            count++
            cursor = cursor.minus(1, ChronoUnit.DAYS)
        }
        return count
    }
}
```

- [ ] **Step 4: Testin geçtiğini doğrula**

Run: `./gradlew :app:testDebugUnitTest --tests "com.abis.direnc.domain.StatsTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/abis/direnc/domain/Stats.kt app/src/test/java/com/abis/direnc/domain/StatsTest.kt
git commit -m "feat: istatistik hesapları - bugün/hafta/streak (TDD)"
```

---

## Task 6: Repository (iş mantığı, Robolectric + in-memory Room ile TDD)

**Files:**
- Create: `app/src/main/java/com/abis/direnc/data/DirencRepository.kt`
- Test: `app/src/test/java/com/abis/direnc/data/DirencRepositoryTest.kt`

- [ ] **Step 1: Başarısız testi yaz**

`app/src/test/java/com/abis/direnc/data/DirencRepositoryTest.kt`:
```kotlin
package com.abis.direnc.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DirencRepositoryTest {

    private lateinit var db: DirencDatabase
    private lateinit var repo: DirencRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DirencDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = DirencRepository(db.sessionDao(), db.eventDao(), db.motivationDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `oturum baslat ve bitir`() = runTest {
        assertNull(repo.activeSession().first())
        repo.startSession(now = 1000L)
        val active = repo.activeSession().first()
        assertNotNull(active)
        assertEquals(1000L, active!!.startedAt)

        repo.endActiveSession(now = 5000L)
        assertNull(repo.activeSession().first())
    }

    @Test
    fun `direnis kaydedilir ve sayilir`() = runTest {
        repo.startSession(now = 1000L)
        repo.recordResistance(now = 1100L)
        repo.recordResistance(now = 1200L)
        assertEquals(2, repo.resistanceTimestamps().first().size)
    }

    @Test
    fun `giris kaydedilir`() = runTest {
        repo.recordEntry(now = 2000L)
        assertEquals(listOf(2000L), repo.entryTimestamps().first())
    }
}
```

- [ ] **Step 2: Testin başarısız olduğunu doğrula**

Run: `./gradlew :app:testDebugUnitTest --tests "com.abis.direnc.data.DirencRepositoryTest"`
Expected: FAIL — "Unresolved reference: DirencRepository"

- [ ] **Step 3: Minimal implementasyonu yaz**

`app/src/main/java/com/abis/direnc/data/DirencRepository.kt`:
```kotlin
package com.abis.direnc.data

import kotlinx.coroutines.flow.Flow

class DirencRepository(
    private val sessionDao: SessionDao,
    private val eventDao: EventDao,
    private val motivationDao: MotivationDao,
) {
    fun activeSession(): Flow<Session?> = sessionDao.activeSession()
    fun resistanceTimestamps(): Flow<List<Long>> = eventDao.resistanceTimestamps()
    fun entryTimestamps(): Flow<List<Long>> = eventDao.entryTimestamps()
    fun motivationTexts(): Flow<List<MotivationText>> = motivationDao.all()

    suspend fun startSession(now: Long) {
        if (sessionDao.activeSessionOnce() == null) {
            sessionDao.insert(Session(startedAt = now))
        }
    }

    suspend fun endActiveSession(now: Long) {
        sessionDao.activeSessionOnce()?.let {
            sessionDao.update(it.copy(endedAt = now))
        }
    }

    suspend fun recordResistance(now: Long) {
        val sid = sessionDao.activeSessionOnce()?.id
        eventDao.insertResistance(ResistanceEvent(timestamp = now, sessionId = sid))
    }

    suspend fun recordEntry(now: Long) {
        val sid = sessionDao.activeSessionOnce()?.id
        eventDao.insertEntry(EntryEvent(timestamp = now, sessionId = sid))
    }

    suspend fun randomMotivationText(): String? =
        motivationDao.allOnce().randomOrNull()?.text

    suspend fun addMotivation(text: String) =
        motivationDao.insert(MotivationText(text = text))

    suspend fun deleteMotivation(text: MotivationText) =
        motivationDao.delete(text)

    suspend fun updateMotivation(text: MotivationText) =
        motivationDao.update(text)
}
```

- [ ] **Step 4: Testin geçtiğini doğrula**

Run: `./gradlew :app:testDebugUnitTest --tests "com.abis.direnc.data.DirencRepositoryTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/abis/direnc/data/DirencRepository.kt app/src/test/java/com/abis/direnc/data/DirencRepositoryTest.kt
git commit -m "feat: DirencRepository iş mantığı (TDD, Robolectric + Room)"
```

---

## Task 7: Ritual ViewModel (durum makinesi, sahte repo ile TDD)

**Files:**
- Create: `app/src/main/java/com/abis/direnc/ui/ritual/RitualViewModel.kt`
- Test: `app/src/test/java/com/abis/direnc/ui/ritual/RitualViewModelTest.kt`

Not: ViewModel'i test edilebilir kılmak için repository çağrılarını yalnızca ihtiyaç
duyulan üç fonksiyona indirgeyen küçük bir arayüz (`RitualActions`) kullanıyoruz.

- [ ] **Step 1: Başarısız testi yaz**

`app/src/test/java/com/abis/direnc/ui/ritual/RitualViewModelTest.kt`:
```kotlin
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
```

- [ ] **Step 2: Testin başarısız olduğunu doğrula**

Run: `./gradlew :app:testDebugUnitTest --tests "com.abis.direnc.ui.ritual.RitualViewModelTest"`
Expected: FAIL — "Unresolved reference: RitualViewModel"

- [ ] **Step 3: Minimal implementasyonu yaz**

`app/src/main/java/com/abis/direnc/ui/ritual/RitualViewModel.kt`:
```kotlin
package com.abis.direnc.ui.ritual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abis.direnc.domain.MathProblem
import com.abis.direnc.domain.MathProblemGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface RitualActions {
    suspend fun randomMotivationText(): String?
    suspend fun recordResistance()
    suspend fun recordEntry()
}

enum class RitualResult { PASSED, GAVE_UP }

data class RitualUiState(
    val stepIndex: Int = 0,            // 0: motivasyon, 1: yüzleştirme, 2: matematik
    val motivationText: String = "",
    val problem: MathProblem = MathProblem("", 0),
    val mathError: Boolean = false,
    val result: RitualResult? = null,
)

class RitualViewModel(
    private val actions: RitualActions,
    fixedProblem: MathProblem? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(
        RitualUiState(problem = fixedProblem ?: MathProblemGenerator.generate())
    )
    val state: StateFlow<RitualUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val text = actions.randomMotivationText()
                ?: "Bir an dur. Bu dürtü geçecek."
            _state.update { it.copy(motivationText = text) }
        }
    }

    fun onMotivationContinue() {
        _state.update { if (it.stepIndex == 0) it.copy(stepIndex = 1) else it }
    }

    fun onConfrontSubmit(answer: String) {
        if (answer.isBlank()) return
        _state.update { if (it.stepIndex == 1) it.copy(stepIndex = 2) else it }
    }

    fun onMathSubmit(answer: String) {
        val value = answer.trim().toIntOrNull()
        if (value == _state.value.problem.answer) {
            viewModelScope.launch {
                actions.recordEntry()
                _state.update { it.copy(mathError = false, result = RitualResult.PASSED) }
            }
        } else {
            _state.update { it.copy(mathError = true) }
        }
    }

    fun onGiveUp() {
        viewModelScope.launch {
            actions.recordResistance()
            _state.update { it.copy(result = RitualResult.GAVE_UP) }
        }
    }
}
```

- [ ] **Step 4: Testin geçtiğini doğrula**

Run: `./gradlew :app:testDebugUnitTest --tests "com.abis.direnc.ui.ritual.RitualViewModelTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/abis/direnc/ui/ritual/RitualViewModel.kt app/src/test/java/com/abis/direnc/ui/ritual/RitualViewModelTest.kt
git commit -m "feat: RitualViewModel durum makinesi (TDD)"
```

---

## Task 8: Ritual ekranı (Compose UI)

**Files:**
- Create: `app/src/main/java/com/abis/direnc/ui/ritual/RitualScreen.kt`

- [ ] **Step 1: Ritüel ekranını yaz**

`app/src/main/java/com/abis/direnc/ui/ritual/RitualScreen.kt`:
```kotlin
package com.abis.direnc.ui.ritual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abis.direnc.ui.theme.Spacing

/**
 * Ritüel akışı. result PASSED/GAVE_UP olunca ilgili callback tetiklenir.
 */
@Composable
fun RitualScreen(
    viewModel: RitualViewModel,
    onPassed: () -> Unit,
    onGaveUp: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.result) {
        when (state.result) {
            RitualResult.PASSED -> onPassed()
            RitualResult.GAVE_UP -> onGaveUp()
            null -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (state.stepIndex) {
                0 -> MotivationStep(state.motivationText, viewModel::onMotivationContinue)
                1 -> ConfrontStep(viewModel::onConfrontSubmit)
                else -> MathStep(
                    question = state.problem.question,
                    error = state.mathError,
                    onSubmit = viewModel::onMathSubmit,
                )
            }
            Spacer(Modifier.height(Spacing.xxl))
            TextButton(onClick = viewModel::onGiveUp) {
                Text("Vazgeçtim, çalışmaya dönüyorum")
            }
        }
    }
}

@Composable
private fun MotivationStep(text: String, onContinue: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(Spacing.xl))
    Button(onClick = onContinue) { Text("Devam") }
}

@Composable
private fun ConfrontStep(onSubmit: (String) -> Unit) {
    var answer by remember { mutableStateOf("") }
    Text(
        text = "Telefonu şu an niçin açmak istiyorsun?",
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(Spacing.lg))
    OutlinedTextField(
        value = answer,
        onValueChange = { answer = it },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(Spacing.lg))
    Button(onClick = { onSubmit(answer) }, enabled = answer.isNotBlank()) {
        Text("Devam")
    }
}

@Composable
private fun MathStep(question: String, error: Boolean, onSubmit: (String) -> Unit) {
    var answer by remember { mutableStateOf("") }
    Text(text = question, style = MaterialTheme.typography.headlineLarge)
    Spacer(Modifier.height(Spacing.lg))
    OutlinedTextField(
        value = answer,
        onValueChange = { answer = it },
        isError = error,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    if (error) {
        Spacer(Modifier.height(Spacing.sm))
        Text("Yanlış. Tekrar dene.", color = MaterialTheme.colorScheme.error)
    }
    Spacer(Modifier.height(Spacing.lg))
    Button(onClick = { onSubmit(answer) }, enabled = answer.isNotBlank()) {
        Text("Onayla")
    }
}
```

- [ ] **Step 2: Derlemeyi doğrula**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/abis/direnc/ui/ritual/RitualScreen.kt
git commit -m "feat: ritüel ekranı (motivasyon, yüzleştirme, matematik, vazgeç)"
```

---

## Task 9: Home ViewModel + ekran

**Files:**
- Create: `app/src/main/java/com/abis/direnc/ui/home/HomeViewModel.kt`
- Create: `app/src/main/java/com/abis/direnc/ui/home/HomeScreen.kt`
- Test: `app/src/test/java/com/abis/direnc/ui/home/HomeViewModelTest.kt`

- [ ] **Step 1: Başarısız HomeViewModel testini yaz**

`app/src/test/java/com/abis/direnc/ui/home/HomeViewModelTest.kt`:
```kotlin
package com.abis.direnc.ui.home

import com.abis.direnc.data.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId

class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakeSource : HomeSource {
        val active = MutableStateFlow<Session?>(null)
        val resistance = MutableStateFlow<List<Long>>(emptyList())
        var started = false
        var ended = false
        override fun activeSession() = active
        override fun resistanceTimestamps() = resistance
        override suspend fun startSession(now: Long) { started = true }
        override suspend fun endActiveSession(now: Long) { ended = true }
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `aktif oturum yoksa sessionActive false`() = runTest {
        val src = FakeSource()
        val vm = HomeViewModel(src, ZoneId.of("Europe/Istanbul")) { 0L }
        advanceUntilIdle()
        assertFalse(vm.state.first().sessionActive)
    }

    @Test
    fun `aktif oturum varsa sessionActive true`() = runTest {
        val src = FakeSource()
        src.active.value = Session(id = 1, startedAt = 0L)
        val vm = HomeViewModel(src, ZoneId.of("Europe/Istanbul")) { 0L }
        advanceUntilIdle()
        assertTrue(vm.state.first().sessionActive)
    }

    @Test
    fun `startSession kaynaga iletilir`() = runTest {
        val src = FakeSource()
        val vm = HomeViewModel(src, ZoneId.of("Europe/Istanbul")) { 123L }
        vm.startSession()
        advanceUntilIdle()
        assertTrue(src.started)
    }

    @Test
    fun `bugunku direnis sayilir`() = runTest {
        val src = FakeSource()
        src.resistance.value = listOf(0L) // 1970-01-01; now da aynı gün
        val vm = HomeViewModel(src, ZoneId.of("UTC")) { 0L }
        advanceUntilIdle()
        assertEquals(1, vm.state.first().todayResistance)
    }
}
```

- [ ] **Step 2: Testin başarısız olduğunu doğrula**

Run: `./gradlew :app:testDebugUnitTest --tests "com.abis.direnc.ui.home.HomeViewModelTest"`
Expected: FAIL — "Unresolved reference: HomeViewModel"

- [ ] **Step 3: HomeViewModel implementasyonunu yaz**

`app/src/main/java/com/abis/direnc/ui/home/HomeViewModel.kt`:
```kotlin
package com.abis.direnc.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abis.direnc.data.Session
import com.abis.direnc.domain.Stats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId

interface HomeSource {
    fun activeSession(): Flow<Session?>
    fun resistanceTimestamps(): Flow<List<Long>>
    suspend fun startSession(now: Long)
    suspend fun endActiveSession(now: Long)
}

data class HomeUiState(
    val sessionActive: Boolean = false,
    val sessionStartedAt: Long? = null,
    val todayResistance: Int = 0,
    val streak: Int = 0,
)

class HomeViewModel(
    private val source: HomeSource,
    private val zone: ZoneId,
    private val now: () -> Long,
) : ViewModel() {

    val state: StateFlow<HomeUiState> =
        combine(source.activeSession(), source.resistanceTimestamps()) { session, resistance ->
            HomeUiState(
                sessionActive = session != null,
                sessionStartedAt = session?.startedAt,
                todayResistance = Stats.countOnDay(resistance, now(), zone),
                streak = Stats.streak(resistance, now(), zone),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun startSession() = viewModelScope.launch { source.startSession(now()) }
    fun endSession() = viewModelScope.launch { source.endActiveSession(now()) }
}
```

- [ ] **Step 4: Testin geçtiğini doğrula**

Run: `./gradlew :app:testDebugUnitTest --tests "com.abis.direnc.ui.home.HomeViewModelTest"`
Expected: PASS

- [ ] **Step 5: Home ekranını yaz**

`app/src/main/java/com/abis/direnc/ui/home/HomeScreen.kt`:
```kotlin
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
    onStartRitual: () -> Unit,
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
            OutlinedButton(onClick = { viewModel.endSession() }) { Text("Oturumu bitir") }
        } else {
            Text("Bugün ${state.todayResistance} kez direndin · Seri: ${state.streak} gün",
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(Spacing.lg))
            Button(onClick = { viewModel.startSession() }) { Text("Çalışmaya başla") }
        }

        Spacer(Modifier.height(Spacing.xxl))
        TextButton(onClick = onOpenStats) { Text("Takip") }
        TextButton(onClick = onOpenSettings) { Text("Ayarlar") }
    }
}
```

- [ ] **Step 6: Derlemeyi doğrula**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/abis/direnc/ui/home/
git commit -m "feat: Home ekranı ve ViewModel (oturum + günlük direniş/streak)"
```

---

## Task 10: Takip (Stats) ViewModel + ekran

**Files:**
- Create: `app/src/main/java/com/abis/direnc/ui/stats/StatsViewModel.kt`
- Create: `app/src/main/java/com/abis/direnc/ui/stats/StatsScreen.kt`

Not: Stats hesap mantığı zaten Task 5'te TDD ile test edildi; bu ekran onu Flow'lara bağlar.

- [ ] **Step 1: StatsViewModel'i yaz**

`app/src/main/java/com/abis/direnc/ui/stats/StatsViewModel.kt`:
```kotlin
package com.abis.direnc.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
}

data class StatsUiState(
    val totalResistance: Int = 0,
    val todayResistance: Int = 0,
    val weekResistance: Int = 0,
    val streak: Int = 0,
    val totalEntries: Int = 0,
)

class StatsViewModel(
    source: StatsSource,
    zone: ZoneId,
    now: () -> Long,
) : ViewModel() {

    val state: StateFlow<StatsUiState> =
        combine(source.resistanceTimestamps(), source.entryTimestamps()) { resistance, entries ->
            val t = now()
            StatsUiState(
                totalResistance = resistance.size,
                todayResistance = Stats.countOnDay(resistance, t, zone),
                weekResistance = Stats.countThisWeek(resistance, t, zone),
                streak = Stats.streak(resistance, t, zone),
                totalEntries = entries.size,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())
}
```

- [ ] **Step 2: Stats ekranını yaz**

`app/src/main/java/com/abis/direnc/ui/stats/StatsScreen.kt`:
```kotlin
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
```

- [ ] **Step 3: Derlemeyi doğrula**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/abis/direnc/ui/stats/
git commit -m "feat: takip ekranı (toplam/bugün/hafta/streak/giriş)"
```

---

## Task 11: Ayarlar (motivasyon metinleri CRUD) ViewModel + ekran

**Files:**
- Create: `app/src/main/java/com/abis/direnc/ui/settings/SettingsViewModel.kt`
- Create: `app/src/main/java/com/abis/direnc/ui/settings/SettingsScreen.kt`

- [ ] **Step 1: SettingsViewModel'i yaz**

`app/src/main/java/com/abis/direnc/ui/settings/SettingsViewModel.kt`:
```kotlin
package com.abis.direnc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abis.direnc.data.MotivationText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface SettingsSource {
    fun motivationTexts(): Flow<List<MotivationText>>
    suspend fun addMotivation(text: String)
    suspend fun deleteMotivation(text: MotivationText)
}

class SettingsViewModel(private val source: SettingsSource) : ViewModel() {

    val texts: StateFlow<List<MotivationText>> =
        source.motivationTexts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { source.addMotivation(text.trim()) }
    }

    fun delete(text: MotivationText) = viewModelScope.launch { source.deleteMotivation(text) }
}
```

- [ ] **Step 2: Ayarlar ekranını yaz**

`app/src/main/java/com/abis/direnc/ui/settings/SettingsScreen.kt`:
```kotlin
package com.abis.direnc.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abis.direnc.ui.theme.Spacing

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val texts by viewModel.texts.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl),
    ) {
        Text("Ayarlar — Metinlerin", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(Spacing.lg))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                label = { Text("Yeni motive/caydırıcı metin") },
            )
            Spacer(Modifier.height(Spacing.sm))
            Button(
                onClick = { viewModel.add(draft); draft = "" },
                enabled = draft.isNotBlank(),
            ) { Text("Ekle") }
        }

        Spacer(Modifier.height(Spacing.lg))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(texts, key = { it.id }) { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(item.text, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.delete(item) }) { Text("Sil") }
                }
            }
        }

        TextButton(onClick = onBack) { Text("Geri") }
    }
}
```

- [ ] **Step 3: Derlemeyi doğrula**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/abis/direnc/ui/settings/
git commit -m "feat: ayarlar ekranı (motivasyon metinleri ekle/sil)"
```

---

## Task 12: Navigasyon + bağımlılık bağlama (MainActivity)

**Files:**
- Modify: `app/src/main/java/com/abis/direnc/MainActivity.kt`

- [ ] **Step 1: MainActivity'i NavHost ve ViewModel fabrikalarıyla yaz**

`app/src/main/java/com/abis/direnc/MainActivity.kt` (tüm dosyayı değiştir):
```kotlin
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
        override suspend fun deleteMotivation(text: MotivationText) = repo.deleteMotivation(text)
    }
}
```

Not: `viewModel<VM> { ... }` lambda-fabrika imzası
`androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6` ile gelir; lambda alıcısı
`CreationExtras` olduğundan ekstra fabrika sınıfına gerek yoktur.

- [ ] **Step 2: Derlemeyi doğrula**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Tüm birim testleri çalıştır**

Run: `./gradlew :app:testDebugUnitTest`
Expected: tüm testler PASS (MathProblemGenerator, Stats, DirencRepository, RitualViewModel, HomeViewModel)

- [ ] **Step 4: Elle doğrulama (emülatör veya cihaz)**

Run: `./gradlew :app:installDebug` (cihaz/emülatör bağlıyken)
Manuel kontrol listesi:
1. Uygulama krem temayla açılır, "Direnç" başlığı görünür.
2. "Çalışmaya başla" → "oturum aktif" + "Telefonu açmak istiyorum" + "Oturumu bitir" görünür.
3. "Telefonu açmak istiyorum" → motivasyon metni → Devam → yüzleştirme sorusu (boşken Devam pasif) → matematik (yanlış cevap hata gösterir, doğru cevap geçer) → Home'a döner.
4. "Vazgeçtim" her adımda görünür; basınca Home'a döner.
5. Takip ekranında direniş/giriş sayıları artmış görünür.
6. Ayarlar'da yeni metin eklenip silinebilir; eklenen metin ritüelde çıkabilir.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/abis/direnc/MainActivity.kt
git commit -m "feat: navigasyon ve ekran bağlama (çalışan çekirdek uygulama)"
```

---

## Tamamlanma Kriteri (Plan 1)

- [ ] `./gradlew :app:testDebugUnitTest` tüm testler geçer.
- [ ] `./gradlew :app:assembleDebug` başarılı.
- [ ] Cihazda elle doğrulama listesi (Task 12 Step 4) tamamlandı.
- [ ] Uygulama internet izni içermiyor (AndroidManifest.xml'de doğrulandı).

Plan 1 bittiğinde: tema, veri, ritüel, takip ve ayarlar **tam çalışır**. Ritüel şu an
uygulama içi butonla tetikleniyor. **Plan 2** bu tetiği gerçek gözcüyle (kilit açılışında
otomatik overlay + vazgeçince ekran kilidi) değiştirecek.
