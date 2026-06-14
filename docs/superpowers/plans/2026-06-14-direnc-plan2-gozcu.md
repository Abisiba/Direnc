# Direnç — Plan 2: Gözcü (Implementation Plan)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Çalışma oturumu aktifken telefonun kilidi her açıldığında ritüeli otomatik olarak her şeyin önüne getiren; "Vazgeçtim"e basınca ekranı tekrar kilitleyen; Home/Geri ile kaçmayı zorlaştıran bir gözcü sistemi eklemek. Plan 1'in çekirdeği değişmeden kalır; ritüel ekranı/ViewModel'i aynen yeniden kullanılır.

**Architecture:** Bir foreground Service (`WatcherService`) oturum boyunca canlı kalır ve kilit-açılış (`ACTION_USER_PRESENT`) / ekran-kapanış (`ACTION_SCREEN_OFF`) yayınlarını dinler. Kilit açılışında, paylaşılan `WatcherState` ve saf `UnlockGate` mantığına göre ritüel gerekiyorsa `RitualOverlayActivity` (kilit ekranı üstünde gösterilebilen, mevcut `RitualScreen`'i barındıran Activity) başlatılır. Bir `AccessibilityService` (`GuardAccessibilityService`) iki iş yapar: (1) ritüel beklemedeyken kullanıcı başka uygulamaya kaçarsa overlay'i yeniden öne getirir, (2) "Vazgeçtim"de ekranı kilitler (`GLOBAL_ACTION_LOCK_SCREEN`). Arka plandan Activity başlatma kısıtı, "diğer uygulamaların üzerinde göster" (SYSTEM_ALERT_WINDOW) izni sayesinde aşılır.

**Tech Stack:** Plan 1 ile aynı (Kotlin, Compose, Room) + Android Service, BroadcastReceiver, AccessibilityService, WindowManager izinleri.

**Önkoşul:** Plan 1 tamamlanmış olmalı (bu makinede tamamlandı). Paket: `com.abis.direnc`.

---

## Dosya Yapısı

```
app/src/main/
  AndroidManifest.xml                 # MODIFY: izinler, service, accessibility, overlay activity
  res/xml/accessibility_config.xml    # CREATE: erişilebilirlik servis ayarı
  res/values/strings.xml              # MODIFY: accessibility açıklaması
  java/com/abis/direnc/
    watcher/
      WatcherState.kt                 # CREATE: paylaşılan uçucu durum
      UnlockGate.kt                   # CREATE: saf karar mantığı (TDD)
      WatcherService.kt               # CREATE: foreground servis + alıcılar
      GuardAccessibilityService.kt    # CREATE: yeniden-öne-getir + ekran kilitle
      RitualOverlayActivity.kt        # CREATE: kilit üstü ritüel Activity
      Permissions.kt                  # CREATE: izin kontrol yardımcıları
    ui/onboarding/
      OnboardingScreen.kt             # CREATE: izinleri açıklayıp isteyen ekran
    MainActivity.kt                   # MODIFY: oturum durumuna göre servisi başlat/durdur + onboarding rota
    ui/home/HomeScreen.kt             # MODIFY: izin eksikse onboarding'e yönlendir
  test/java/com/abis/direnc/watcher/
    UnlockGateTest.kt                 # CREATE (TDD)
```

---

## Task 1: Paylaşılan durum + UnlockGate (saf mantık, TDD) + manifest izinleri

**Files:**
- Create: `app/src/main/java/com/abis/direnc/watcher/WatcherState.kt`
- Create: `app/src/main/java/com/abis/direnc/watcher/UnlockGate.kt`
- Test: `app/src/test/java/com/abis/direnc/watcher/UnlockGateTest.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Başarısız UnlockGate testini yaz**

`app/src/test/java/com/abis/direnc/watcher/UnlockGateTest.kt`:
```kotlin
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
```

- [ ] **Step 2: Testin başarısız olduğunu doğrula**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:testDebugUnitTest --tests "com.abis.direnc.watcher.UnlockGateTest"`
Expected: FAIL — "Unresolved reference: UnlockGate"

- [ ] **Step 3: UnlockGate'i yaz**

`app/src/main/java/com/abis/direnc/watcher/UnlockGate.kt`:
```kotlin
package com.abis.direnc.watcher

/** Kilit açıldığında ritüelin gösterilip gösterilmeyeceğine karar veren saf mantık. */
object UnlockGate {
    fun shouldShowRitual(sessionActive: Boolean, passedThisUnlock: Boolean): Boolean =
        sessionActive && !passedThisUnlock
}
```

- [ ] **Step 4: WatcherState'i yaz**

`app/src/main/java/com/abis/direnc/watcher/WatcherState.kt`:
```kotlin
package com.abis.direnc.watcher

/**
 * Servis, overlay ve erişilebilirlik servisi arasında paylaşılan uçucu durum.
 * Kalıcı veri DEĞİL — yalnızca anlık gözcü koordinasyonu.
 */
object WatcherState {
    @Volatile var sessionActive: Boolean = false
    @Volatile var ritualPending: Boolean = false   // overlay gösterilmeli/gösteriliyor
    @Volatile var passedThisUnlock: Boolean = false // bu ekran-açılışında ritüel geçildi
}
```

- [ ] **Step 5: Testin geçtiğini doğrula**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:testDebugUnitTest --tests "com.abis.direnc.watcher.UnlockGateTest"`
Expected: PASS

- [ ] **Step 6: Manifest'e izinleri ve özellikleri ekle**

`app/src/main/AndroidManifest.xml` — `<manifest>` etiketinin hemen altına, `<application>`'dan ÖNCE şu izinleri ekle (İNTERNET YOK; bunlar yerel sistem izinleri):
```xml
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
```

- [ ] **Step 7: Derle ve commit**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL
```bash
git add app/src/main/java/com/abis/direnc/watcher/ app/src/test/java/com/abis/direnc/watcher/ app/src/main/AndroidManifest.xml
git commit -m "feat(gözcü): UnlockGate (TDD) + WatcherState + sistem izinleri"
```

---

## Task 2: RitualOverlayActivity (kilit üstü ritüel)

**Files:**
- Create: `app/src/main/java/com/abis/direnc/watcher/RitualOverlayActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: RitualOverlayActivity'i yaz**

`app/src/main/java/com/abis/direnc/watcher/RitualOverlayActivity.kt`:
```kotlin
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
```

- [ ] **Step 2: Manifest'e overlay activity'i ekle**

`app/src/main/AndroidManifest.xml` — `<application>` içine, mevcut `.MainActivity` activity'sinden SONRA ekle:
```xml
        <activity
            android:name=".watcher.RitualOverlayActivity"
            android:exported="false"
            android:launchMode="singleInstance"
            android:excludeFromRecents="true"
            android:taskAffinity=".ritualOverlay"
            android:showWhenLocked="true"
            android:turnScreenOn="true"
            android:theme="@style/Theme.Direnc" />
```

- [ ] **Step 3: Not — GuardAccessibilityService henüz yok**

`GuardAccessibilityService.instance?.lockScreen()` satırı Task 4'te tanımlanacak servise referans verir. Bu task'ta derleme için Task 4 da gerekebilir; bu yüzden Task 2–4'ü birlikte ele al ve derlemeyi Task 4 sonunda yap. (Sırayla yaz, derlemeyi Task 4 Step sonunda çalıştır.)

---

## Task 3: WatcherService (foreground servis + ekran yayını alıcıları)

**Files:**
- Create: `app/src/main/java/com/abis/direnc/watcher/WatcherService.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: WatcherService'i yaz**

`app/src/main/java/com/abis/direnc/watcher/WatcherService.kt`:
```kotlin
package com.abis.direnc.watcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Oturum boyunca canlı kalan foreground servis. Kilit açılışını yakalar ve gerekirse
 * ritüel overlay'ini başlatır; ekran kapanınca "bu açılışta geçildi" durumunu sıfırlar.
 */
class WatcherService : Service() {

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    WatcherState.passedThisUnlock = false
                }
                Intent.ACTION_USER_PRESENT -> {
                    if (UnlockGate.shouldShowRitual(
                            WatcherState.sessionActive,
                            WatcherState.passedThisUnlock,
                        )
                    ) {
                        RitualOverlayActivity.launch(this@WatcherService)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        WatcherState.sessionActive = true
        startForeground(NOTIF_ID, buildNotification())
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
        WatcherState.sessionActive = false
        WatcherState.passedThisUnlock = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Çalışma oturumu",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Gözcü aktifken görünür" }
            mgr.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Direnç — oturum aktif")
            .setContentText("Çalışıyorsun. Gözcü açık.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "direnc_watcher"
        private const val NOTIF_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, WatcherService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WatcherService::class.java))
        }
    }
}
```

- [ ] **Step 2: Manifest'e servisi ekle**

`app/src/main/AndroidManifest.xml` — `<application>` içine ekle:
```xml
        <service
            android:name=".watcher.WatcherService"
            android:exported="false"
            android:foregroundServiceType="specialUse">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="self_imposed_focus_lock" />
        </service>
```

---

## Task 4: GuardAccessibilityService (yeniden-öne-getir + ekran kilitle)

**Files:**
- Create: `app/src/main/java/com/abis/direnc/watcher/GuardAccessibilityService.kt`
- Create: `app/src/main/res/xml/accessibility_config.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Erişilebilirlik ayar XML'ini oluştur**

`app/src/main/res/xml/accessibility_config.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="100"
    android:canPerformGestures="false"
    android:description="@string/accessibility_desc" />
```

- [ ] **Step 2: strings.xml'e açıklama ekle**

`app/src/main/res/values/strings.xml` — `<resources>` içine ekle:
```xml
    <string name="accessibility_desc">Çalışma oturumu açıkken ritüel ekranından kaçmayı engeller ve "Vazgeçtim" dediğinde telefonu kilitler. Hiçbir veri toplanmaz, internete gönderilmez.</string>
```

- [ ] **Step 3: GuardAccessibilityService'i yaz**

`app/src/main/java/com/abis/direnc/watcher/GuardAccessibilityService.kt`:
```kotlin
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
```

- [ ] **Step 4: Manifest'e erişilebilirlik servisini ekle**

`app/src/main/AndroidManifest.xml` — `<application>` içine ekle:
```xml
        <service
            android:name=".watcher.GuardAccessibilityService"
            android:exported="false"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_config" />
        </service>
```

- [ ] **Step 5: Task 2–4 birlikte derle ve commit**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (RitualOverlayActivity, WatcherService, GuardAccessibilityService birbirine referans verdiği için birlikte derlenir)
```bash
git add app/src/main/java/com/abis/direnc/watcher/ app/src/main/res/ app/src/main/AndroidManifest.xml
git commit -m "feat(gözcü): overlay activity, foreground servis, erişilebilirlik servisi"
```

---

## Task 5: İzin yardımcıları + Onboarding ekranı

**Files:**
- Create: `app/src/main/java/com/abis/direnc/watcher/Permissions.kt`
- Create: `app/src/main/java/com/abis/direnc/ui/onboarding/OnboardingScreen.kt`

- [ ] **Step 1: İzin kontrol yardımcılarını yaz**

`app/src/main/java/com/abis/direnc/watcher/Permissions.kt`:
```kotlin
package com.abis.direnc.watcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils

/** Gözcü için gereken izinlerin durumunu kontrol eden ve ayar ekranlarını açan yardımcılar. */
object Permissions {

    fun canDrawOverlay(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    fun isAccessibilityEnabled(context: Context): Boolean {
        val expected = "${context.packageName}/${GuardAccessibilityService::class.java.name}"
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expected, ignoreCase = true)) return true
        }
        return false
    }

    fun isBatteryUnrestricted(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun allGranted(context: Context): Boolean =
        canDrawOverlay(context) && isAccessibilityEnabled(context) && isBatteryUnrestricted(context)

    fun openOverlaySettings(context: Context) {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    @Suppress("BatteryLife")
    fun openBatterySettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}"),
            )
        } else {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        }
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
```

- [ ] **Step 2: Onboarding ekranını yaz**

`app/src/main/java/com/abis/direnc/ui/onboarding/OnboardingScreen.kt`:
```kotlin
package com.abis.direnc.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abis.direnc.ui.theme.Spacing

/**
 * İzinleri tek tek açıklayıp ilgili ayar ekranını açan onboarding. Durum (granted)
 * dışarıdan verilir; kullanıcı ayara gidip dönünce MainActivity yeniden hesaplar.
 */
@Composable
fun OnboardingScreen(
    overlayGranted: Boolean,
    accessibilityGranted: Boolean,
    batteryGranted: Boolean,
    onOpenOverlay: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenBattery: () -> Unit,
    onAllReady: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xl),
    ) {
        Text("Gözcü kurulumu", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(Spacing.md))
        Text(
            "Gözcünün çalışması için üç izin gerekiyor. Hepsi cihazında kalır; " +
                "hiçbir veri toplanmaz veya internete gönderilmez.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(Spacing.xl))

        PermissionRow(
            title = "Diğer uygulamaların üstünde göster",
            desc = "Ritüel ekranını her şeyin önüne getirebilmek için.",
            granted = overlayGranted,
            onOpen = onOpenOverlay,
        )
        PermissionRow(
            title = "Erişilebilirlik servisi",
            desc = "Kaçmayı engellemek ve 'Vazgeçtim'de ekranı kilitlemek için.",
            granted = accessibilityGranted,
            onOpen = onOpenAccessibility,
        )
        PermissionRow(
            title = "Pil kısıtlamasından muafiyet",
            desc = "Telefonun gözcüyü arka planda öldürmemesi için.",
            granted = batteryGranted,
            onOpen = onOpenBattery,
        )

        Spacer(Modifier.height(Spacing.xl))
        Button(
            onClick = onAllReady,
            enabled = overlayGranted && accessibilityGranted && batteryGranted,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Hazır — devam et") }
        TextButton(onClick = onBack) { Text("Geri") }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    desc: String,
    granted: Boolean,
    onOpen: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm)) {
        Text(
            (if (granted) "✓ " else "• ") + title,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(desc, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.sm))
        OutlinedButton(onClick = onOpen, enabled = !granted) {
            Text(if (granted) "Verildi" else "Ayarı aç")
        }
        Spacer(Modifier.height(Spacing.sm))
    }
}
```

- [ ] **Step 3: Derle ve commit**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL
```bash
git add app/src/main/java/com/abis/direnc/watcher/Permissions.kt app/src/main/java/com/abis/direnc/ui/onboarding/
git commit -m "feat(gözcü): izin yardımcıları + onboarding ekranı"
```

---

## Task 6: Servisi oturum durumuna bağla + onboarding rotası (MainActivity, HomeScreen)

**Files:**
- Modify: `app/src/main/java/com/abis/direnc/MainActivity.kt`
- Modify: `app/src/main/java/com/abis/direnc/ui/home/HomeScreen.kt`

- [ ] **Step 1: HomeScreen'e onboarding'e gitme yolu ekle**

`app/src/main/java/com/abis/direnc/ui/home/HomeScreen.kt` — `HomeScreen` imzasına yeni parametre ekle ve "Çalışmaya başla" davranışını izne göre kur. Mevcut imza:
```kotlin
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartRitual: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
)
```
şu hâle gelsin:
```kotlin
fun HomeScreen(
    viewModel: HomeViewModel,
    permissionsReady: Boolean,
    onStartRitual: () -> Unit,
    onStartSessionGuarded: () -> Unit,
    onOpenOnboarding: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
)
```
"Çalışmaya başla" butonunun `onClick`'i şu olsun:
```kotlin
            Button(onClick = {
                if (permissionsReady) onStartSessionGuarded() else onOpenOnboarding()
            }) { Text("Çalışmaya başla") }
```
(`onStartSessionGuarded` hem `viewModel.startSession()` çağrısını hem de servis başlatmayı MainActivity'de yapacak. `viewModel.startSession()` çağrısını buradan KALDIR — MainActivity üstlenecek.)

- [ ] **Step 2: MainActivity'i güncelle — servis başlat/durdur, onboarding rota, izin durumu**

`app/src/main/java/com/abis/direnc/MainActivity.kt` — şu değişiklikleri yap:

(a) Importlara ekle:
```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.abis.direnc.ui.onboarding.OnboardingScreen
import com.abis.direnc.watcher.Permissions
import com.abis.direnc.watcher.WatcherService
```

(b) `home` composable bloğunu şununla değiştir:
```kotlin
                        composable("home") {
                            val vm = viewModel<HomeViewModel> {
                                HomeViewModel(repoAsHomeSource(), zone, now)
                            }
                            // İzin durumunu ekran her öne geldiğinde tazele
                            var ready by remember { mutableStateOf(Permissions.allGranted(this@MainActivity)) }
                            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                                ready = Permissions.allGranted(this@MainActivity)
                            }
                            HomeScreen(
                                viewModel = vm,
                                permissionsReady = ready,
                                onStartRitual = { navController.navigate("ritual") },
                                onStartSessionGuarded = {
                                    vm.startSession()
                                    WatcherService.start(this@MainActivity)
                                },
                                onOpenOnboarding = { navController.navigate("onboarding") },
                                onOpenStats = { navController.navigate("stats") },
                                onOpenSettings = { navController.navigate("settings") },
                            )
                        }
                        composable("onboarding") {
                            var overlay by remember { mutableStateOf(Permissions.canDrawOverlay(this@MainActivity)) }
                            var acc by remember { mutableStateOf(Permissions.isAccessibilityEnabled(this@MainActivity)) }
                            var batt by remember { mutableStateOf(Permissions.isBatteryUnrestricted(this@MainActivity)) }
                            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                                overlay = Permissions.canDrawOverlay(this@MainActivity)
                                acc = Permissions.isAccessibilityEnabled(this@MainActivity)
                                batt = Permissions.isBatteryUnrestricted(this@MainActivity)
                            }
                            OnboardingScreen(
                                overlayGranted = overlay,
                                accessibilityGranted = acc,
                                batteryGranted = batt,
                                onOpenOverlay = { Permissions.openOverlaySettings(this@MainActivity) },
                                onOpenAccessibility = { Permissions.openAccessibilitySettings(this@MainActivity) },
                                onOpenBattery = { Permissions.openBatterySettings(this@MainActivity) },
                                onAllReady = { navController.popBackStack() },
                                onBack = { navController.popBackStack() },
                            )
                        }
```

(c) Oturum bittiğinde servisi durdurmak için: `HomeViewModel.endSession()` repo'yu güncelliyor; servisi de durdurmak gerek. En basit ve güvenilir yol: oturum aktiflik durumunu izleyip servis durdurmayı MainActivity'de yapmak. `home` composable'ına şunu ekle (HomeScreen çağrısından hemen önce):
```kotlin
                            val homeState by vm.state.collectAsStateWithLifecycle()
                            LaunchedEffect(homeState.sessionActive) {
                                if (!homeState.sessionActive) WatcherService.stop(this@MainActivity)
                            }
```
ve importlara `import androidx.compose.runtime.LaunchedEffect` ekle.

- [ ] **Step 3: Derle**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Tüm birim testleri çalıştır (regresyon)**

Run: `JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:testDebugUnitTest`
Expected: tüm testler PASS (Plan 1'in 25 testi + UnlockGate'in 3 testi = 28)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/abis/direnc/MainActivity.kt app/src/main/java/com/abis/direnc/ui/home/HomeScreen.kt
git commit -m "feat(gözcü): servisi oturuma bağla + onboarding rotası + izin kapısı"
```

---

## Task 7: Cihazda gerçek doğrulama (bağlı telefon)

**Files:** (yok — yalnızca kurulum ve elle test)

- [ ] **Step 1: Kur ve aç**

Run:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p com.abis.direnc -c android.intent.category.LAUNCHER 1
```
Expected: "Success" ve uygulama açılır.

- [ ] **Step 2: Onboarding / izinler**

Manuel:
1. Ana ekranda "Çalışmaya başla"ya bas → izinler eksikse **Gözcü kurulumu** ekranı açılmalı.
2. Üç izni de ver (Ayarı aç → ver → geri dön). Her dönüşte ilgili satır "✓ Verildi" olmalı.
3. Üçü de verilince "Hazır — devam et" aktifleşir; bas → ana ekrana döner.

- [ ] **Step 3: Gözcü davranışı**

Manuel:
1. "Çalışmaya başla"ya bas → kalıcı bildirim ("Direnç — oturum aktif") görünmeli.
2. Telefonu kilitle (güç tuşu), sonra **kilidini aç** → ritüel ekranı otomatik gelmeli.
3. Ritüeli geç → telefon serbest; ekranı kapatmadan tekrar aç (sadece güç tuşuyla uyandır) → ritüel GELMEMELİ (bu açılış için geçildi). Ekranı kapat-aç (gerçek kilit açma) → ritüel TEKRAR gelmeli.
4. Ritüel gelince **Home tuşuna** bas → ritüel kısa sürede geri gelmeli (erişilebilirlik yeniden öne getirir).
5. Ritüelde **"Vazgeçtim"e** bas → ekran kilitlenmeli ve direniş sayacı artmalı.
6. Oturumu bitir (ritüeli geçip uygulamaya gir → "Oturumu bitir") → bildirim kaybolmalı, kilit açınca ritüel artık GELMEMELİ.

- [ ] **Step 4: Bulguları not et**

Cihaza özgü davranışlar (özellikle Samsung One UI'da arka plan kısıtları, overlay gecikmesi) görülürse not al. Background-activity-launch engellenirse, çözüm: overlay'i `RitualOverlayActivity` yerine `WatcherService` içinden `WindowManager` + `TYPE_APPLICATION_OVERLAY` penceresiyle çizmeye geçmek (sonraki iterasyon).

- [ ] **Step 5: Commit (varsa düzeltmeler)**

Cihaz testinde düzeltme gerekirse uygula, sonra:
```bash
git add -A && git commit -m "fix(gözcü): cihaz testi düzeltmeleri"
```

---

## Tamamlanma Kriteri (Plan 2)

- [ ] `JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:testDebugUnitTest` tüm testler geçer (28).
- [ ] `assembleDebug` başarılı.
- [ ] Cihazda: oturum açıkken kilit açılışında ritüel otomatik geliyor; "Vazgeçtim" ekranı kilitliyor; oturum bitince gözcü duruyor.
- [ ] Hâlâ internet izni yok (manifest'te yalnızca yerel sistem izinleri).

Plan 2 bittiğinde gözcü tam çalışır: çalışma oturumu boyunca telefonun her kilit açılışı bir geçiş ritüeliyle karşılanır, vazgeçince telefon kilitlenir — tasarımdaki "telefona her dokunuşta küçük bir savaş" hedefi gerçekleşir.

### Dürüst sınırlar (tasarımda kabul edildi)
- Background-activity-launch ve OEM pil kısıtları cihaza göre değişebilir; gözcü %100 kurşungeçmez değildir (amaç sürtünme, kurşungeçmezlik değil).
- "Vazgeçtim → ekran kilitle" GLOBAL_ACTION_LOCK_SCREEN'e dayanır (API 28+); cihazda erişilebilirlik servisi aktif olmalı.
