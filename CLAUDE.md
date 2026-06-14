# Direnç — Telefon Sürtünme / İrade Uygulaması

Bu dosya, projeye ileride dönüldüğünde (Claude veya kullanıcı) tam bağlamla devam
edilebilmesi için yazıldı. Son güncelleme: 2026-06-14.

---

## 1. Uygulama nedir?

Ders çalışırken telefona dalmayı engellemek için kişisel bir **irade aracı**. Güvenlik
kilidi DEĞİL — kullanıcı hem kilidi koyan hem açmaya çalışan kişidir ve **bilinçli olarak
engellenmek ister**. Mantık: telefona girme dürtüsü geldiği an, kullanıcı kendine koyduğu
bir **geçiş ritüelinden** geçer. Ritüel yasaklamaz; bir an **durup düşünmeye zorlar** ve
**vazgeçmeyi asıl başarı olarak ödüllendirir** (direniş sayacı).

**Neden kendi uygulaması:** Kullanıcı hazır odak/kilit uygulamalarına güvenmiyor (gizlilik).
Bu yüzden uygulama **internet iznine bile sahip değildir**, hiçbir veri toplamaz, her şey
cihazda kalır, tüm kod kullanıcınındır.

### Geçiş ritüeli (3 adım + vazgeç)
1. **Caydırıcı/motive yazı** — kullanıcının kendi yazdığı metinlerden rastgele biri.
2. **Yüzleştiren soru** — "Telefonu şu an niçin açmak istiyorsun?" (boş geçilemez).
3. **Rastgele orta-zorluk matematik** (ör. 47 × 8) — doğru cevap istenir.
- Her adımda **"Vazgeçtim, çalışmaya dönüyorum"** butonu vardır → direniş +1, ekran kilitlenir.
- Üçü de geçilirse: telefon serbest (ekran kapanana kadar) → bu bir "giriş" olarak kaydedilir.

### Gözcü modu (otomatik engelleme)
Çalışma oturumu aktifken telefonun **kilidi her açıldığında** ritüel otomatik olarak her
şeyin önüne gelir. "Vazgeçtim"de ekran tekrar kilitlenir. Geçtikten sonra ekran kapanana
kadar serbest; sonraki kilit açılışında ritüel yeniden gelir (serbest süre/muafiyet yok).

---

## 2. Önemli tasarım kararları / kısıtlar (DEĞİŞTİRMEDEN ÖNCE OKU)

- **İNTERNET İZNİ YOK.** Asla `android.permission.INTERNET` eklenmeyecek, ağ çağrısı
  (OkHttp/Retrofit/URL/socket) yazılmayacak. Gizlilik vaadi bunun üzerine kurulu.
- **Tema:** Sıcak krem zemin, bol beyaz boşluk, ince/sakin tipografi, tek yumuşak toprak
  vurgu rengi (#B07D56). Minimal ve ferah; "boşluk hissi" bilinçli. Bkz. `ui/theme/Theme.kt`.
- **Tüm veri yerel** (Room). Yedekleme kapalı (`allowBackup=false`), `exportSchema=false`.
- **Oturumu bitirme** gözcü sayesinde fiilen ritüelin arkasındadır (bitirmek için önce
  kilit açıp ritüeli geçmen gerekir). Bkz. `ui/home/HomeScreen.kt` yorumları.
- **Matematik zorluğu:** orta (kafadan/kâğıtla; hesap makinesi gerektirmemeli).

---

## 3. Geliştirme ortamı (ÇOK ÖNEMLİ)

Bu makinede **non-interaktif kabuklar (Bash aracı, subagent'ler) ortam değişkenlerini
GÖRMEZ.** Bu yüzden gradle komutlarını her zaman JDK yolunu vererek çalıştır:

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew <görev>
```

- **JDK:** Android Studio gömülü → `/opt/android-studio/jbr` (JDK 21).
- **Android SDK:** `~/Android/Sdk` (platform-tools 37, build-tools 34, platforms;android-34).
  Projeye `local.properties` (`sdk.dir=/home/abis/Android/Sdk`) ile bağlı — git'e girmez.
- **Gradle:** 8.9 (proje wrapper'ı `./gradlew` mevcut; ayrıca `~/.local/gradle-8.9`).
- **adb:** `/usr/bin/adb` (35.0.2). Telefon: **Samsung Galaxy S24 FE (SM-S721B), Android 16**.
- `~/.zshrc`'ye kalıcı ortam değişkenleri eklendi (yalnızca etkileşimli terminaller için;
  `JAVA_HOME`, `ANDROID_HOME`, PATH'e gradle/platform-tools/cmdline-tools).
- `gradle.properties` içinde `org.gradle.java.home=/opt/android-studio/jbr` var.

### Sık kullanılan komutlar
```bash
# Birim testleri (28 test)
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:testDebugUnitTest

# Derleme
JAVA_HOME=/opt/android-studio/jbr ./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk

# Telefona kur + aç
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p com.abis.direnc -c android.intent.category.LAUNCHER 1

# Telefon "unauthorized" ise: telefonda "USB hata ayıklamasına izin ver" penceresini onayla
# Telefon hiç görünmüyorsa: veri kablosu/port kontrolü, ya da kablosuz hata ayıklama
adb kill-server && adb start-server && adb devices -l
```

---

## 4. Teknoloji ve sürümler

- **Kotlin** 2.0.20, **AGP** 8.5.2, **KSP** 2.0.20-1.0.25, **Gradle** 8.9
- **Jetpack Compose** (BOM 2024.09.02), **Material3**, **Navigation Compose** 2.8.0
- **Room** 2.6.1 (KSP)
- **Lifecycle** 2.8.6 (viewmodel-compose, runtime-compose)
- Test: JUnit4, kotlinx-coroutines-test 1.8.1, Robolectric 4.13, room-testing
- **Paket:** `com.abis.direnc` · **minSdk** 28 · **compile/targetSdk** 34

---

## 5. Mimari (MVVM) ve dosya haritası

Pür mantık saf Kotlin fonksiyonlarında ve TDD ile test edilir; ViewModel'ler dar adaptör
arayüzleriyle (HomeSource/RitualActions/StatsSource/SettingsSource) sahte repo ile test
edilir; Room DAO Robolectric ile; UI elle/cihazda doğrulanır.

```
app/src/main/java/com/abis/direnc/
  DirencApp.kt              # Application; DB singleton + varsayılan metin tohumu
  MainActivity.kt           # Tek Activity + NavHost; repo→ekran adaptörleri; servis başlat/durdur; onboarding rota
  data/
    entities.kt             # Session, ResistanceEvent, EntryEvent, MotivationText
    Daos.kt                 # SessionDao, EventDao, MotivationDao
    DirencDatabase.kt       # Room DB + DEFAULT_TEXTS tohumu
    DirencRepository.kt     # İş mantığı (oturum başlat/bitir, direniş/giriş kaydı, metin CRUD)
  domain/
    MathProblem.kt          # MathProblemGenerator (saf, TDD)
    Stats.kt                # countOnDay/countThisWeek/streak (saf, TDD). Hafta=Pazartesi başlangıç
  ui/
    theme/Theme.kt, Spacing.kt   # Krem tema, boşluk sabitleri
    home/                   # HomeViewModel (oturum + bugün/streak), HomeScreen
    ritual/                 # RitualViewModel (durum makinesi), RitualScreen — gözcü de bunu kullanır
    stats/                  # StatsViewModel, StatsScreen (toplam/bugün/hafta/streak/giriş)
    settings/               # SettingsViewModel, SettingsScreen (metin ekle/düzenle/sil)
    onboarding/             # OnboardingScreen (3 izni açıklayıp ister)
  watcher/                  # GÖZCÜ
    UnlockGate.kt           # Saf karar: shouldShowRitual(sessionActive, passedThisUnlock) (TDD)
    WatcherState.kt         # Paylaşılan uçucu durum (sessionActive, ritualPending, passedThisUnlock)
    WatcherService.kt       # Foreground servis (specialUse); USER_PRESENT/SCREEN_OFF dinler; overlay başlatır
    RitualOverlayActivity.kt# Kilit üstü ritüel ekranı (RitualScreen'i barındırır)
    GuardAccessibilityService.kt # Kaçışı engeller (yeniden öne getirir) + "Vazgeçtim"de ekranı kilitler
    Permissions.kt          # İzin kontrol + ayar ekranı açma yardımcıları

app/src/main/res/
  drawable/ic_launcher_(background|foreground).xml  # Krem zemin + minimal halka ikon
  mipmap-anydpi-v26/ic_launcher(_round).xml         # Adaptive icon (minSdk 28, PNG gerekmez)
  xml/accessibility_config.xml                      # Erişilebilirlik servis ayarı
  values/strings.xml                                # app_name, tema stili, accessibility_desc
```

### Gözcü akışı (özet)
Oturum başlar → `WatcherService.start()` (MainActivity'de) → servis `sessionActive=true`,
foreground bildirim, `ACTION_USER_PRESENT`/`ACTION_SCREEN_OFF` alıcısı kaydeder.
Kilit açılınca → `UnlockGate.shouldShowRitual` → `RitualOverlayActivity.launch()`.
Overlay: PASSED → `passedThisUnlock=true`, finish (telefon serbest). GAVE_UP →
`GuardAccessibilityService.instance?.lockScreen()` (GLOBAL_ACTION_LOCK_SCREEN), finish.
Ekran kapanınca → `passedThisUnlock=false`. Oturum bitince → `WatcherService.stop()`.

---

## 6. Bilinen sınırlar / cihaz uyarıları

- **Background-activity-launch (BAL):** Gözcü, arka plandan Activity başlatmayı
  `SYSTEM_ALERT_WINDOW` (üstte gösterme) iznine dayanarak yapar. Bazı OEM'lerde (özellikle
  Samsung One UI, Xiaomi) yine engellenebilir. **Fallback:** overlay'i Activity yerine
  `WatcherService` içinden `WindowManager` + `TYPE_APPLICATION_OVERLAY` penceresiyle çizmek.
- **OEM pil kısıtı:** Servis öldürülürse gözcü durur → onboarding'de pil muafiyeti zorunlu.
- **Ekran kilitleme:** `GLOBAL_ACTION_LOCK_SCREEN` API 28+ ve erişilebilirlik servisi aktif
  olmalı; aksi halde "Vazgeçtim" sessizce ekranı kilitlemez (instance null).
- Amaç kurşungeçmezlik değil, yeterli **sürtünme** — kabul edilmiş bir sınır.
- `specialUse` foreground servis: Play Store dağıtımında politika incelemesine takılabilir
  (yerel/sideload kurulum için sorun değil).

### Geliştirme sırasında yapılan pragmatik düzeltmeler (locale/JDK kaynaklı)
- `app/build.gradle.kts` `testOptions`: test JVM'ine `user.language=en`/`user.country=US`
  eklendi (Türkçe locale Robolectric'te `lınux` üreterek UnsatisfiedLinkError veriyordu).
- `Stats.countThisWeek`: locale'den bağımsız sabit Pazartesi başlangıcı
  (`TemporalAdjusters.previousOrSame(MONDAY)`) — `Locale("tr")` Pazar döndürüyordu.
- `HomeViewModel`: `SharingStarted.Eagerly` (test'in `advanceUntilIdle` sonrası ilk değer
  okuması için; diğer VM'ler `WhileSubscribed`).

---

## 7. Durum (2026-06-14)

- ✅ **Plan 1 (çekirdek):** tema, veri, ritüel, takip, ayarlar (ekle/düzenle/sil) — tam.
- ✅ **Plan 2 (gözcü):** servis, overlay, erişilebilirlik, onboarding, kilitleme — tam.
- ✅ **Uygulama ikonu:** krem zeminde minimal halka (adaptive icon).
- ✅ **28 birim testi** geçiyor, `assembleDebug` başarılı, internet izni yok.
- ✅ Telefona kuruldu. **Sırada:** cihazda gözcünün gerçek davranış doğrulaması
  (kilit açılışında ritüel geliyor mu, "Vazgeçtim" kilitliyor mu — özellikle Samsung BAL).

---

## 8. Dokümanlar / iş akışı

- **Tasarım:** `docs/superpowers/specs/2026-06-14-direnc-phone-friction-design.md`
- **Plan 1:** `docs/superpowers/plans/2026-06-14-direnc-plan1-cekirdek-uygulama.md`
- **Plan 2:** `docs/superpowers/plans/2026-06-14-direnc-plan2-gozcu.md`
- Geliştirme yöntemi: brainstorm → spec → plan → TDD ile uygulama, her task'ta atomik commit.
- Commit mesajları `Co-Authored-By: Claude Opus 4.8 (1M context)` ile bitiyor.

## 9. Sonraki olası işler (kapsam dışı / fikirler)
- Cihaz testinde BAL sorunu çıkarsa WindowManager overlay fallback'ine geçiş.
- Ayarlanabilir matematik zorluğu (kolay/orta/zor).
- Ek sürtünme türleri: zorunlu bekleme (geri sayım), uzun metin yazdırma.
- Uygulama-bazlı engelleme seçeneği (yalnızca seçili uygulamalar).
- "Direniş vs giriş" karşılaştırmalı grafikleri; bugün/bu hafta için takip ekranı zenginleştirme.
- Gece yarısı geçişinde takip ekranı görünümünün otomatik tazelenmesi (şu an yeni olayda yenilenir).
