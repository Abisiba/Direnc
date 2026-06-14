# Direnç — Telefon Sürtünme Uygulaması (Tasarım Dokümanı)

**Tarih:** 2026-06-14
**Durum:** Onaylandı, plana hazır
**Platform:** Android (Kotlin + Jetpack Compose)

---

## 1. Amaç ve Felsefe

Ders çalışırken telefona dalmayı engellemek için kişisel bir **irade aracı**. Bu bir
güvenlik kilidi *değildir* — kullanıcı hem kilidi koyan hem açmaya çalışan kişidir ve
**bilinçli olarak engellenmek ister**.

Temel fikir: Telefona girme dürtüsü geldiği an, kullanıcı kendine koyduğu bir
**geçiş ritüelinden** geçer. Ritüel yasaklamaz; bir an **durup düşünmeye zorlar** ve
vazgeçişi asıl başarı olarak ödüllendirir. Bir şeye ulaşmak zorlaştıkça onu yapma
isteği azalır.

### Neden kendi uygulaması?
Kullanıcı mevcut odak/kilit uygulamalarının yazılımına güvenmiyor (gizlilik, veri
toplama). Bu uygulama:
- **İnternet iznine dahi sahip değildir.**
- Hiçbir veri toplamaz, hiçbir sunucuya bağlanmaz.
- Tüm veriler yalnızca cihazda (yerel veritabanı) saklanır.
- Tüm kaynak kod kullanıcıya aittir — güvenilmesi gereken üçüncü taraf yoktur.

---

## 2. Tema ve Görünüm

- **Sıcak krem zemin** (örn. kâğıt/krem tonları), tek yumuşak vurgu rengi.
- Bol **beyaz boşluk**, minimal yerleşim.
- İnce, sakin tipografi.
- Göz yormayan, ferah bir his — bu "boşluk hissi" kullanıcıyı düşünmeye sevk eder.

---

## 3. Çalışma Akışı

1. Kullanıcı uygulamada **"Çalışmaya başla"** der → çalışma oturumu açılır.
   Arka planda sessiz bir **gözcü servisi** çalışır (kalıcı bildirim: "Oturum aktif").
2. Oturum açıkken **telefonun kilidi her açıldığında** → **ritüel overlay** her şeyin
   önüne gelir ve şu üç adımı sırayla sunar:
   1. **Caydırıcı/motive yazı** — kullanıcının kendi yazdığı metinlerden biri.
   2. **Yüzleştiren soru** — "Telefonu şu an niçin açmak istiyorsun?" (serbest metin girişi).
   3. **Rastgele matematik işlemi** — orta zorluk (ör. 47 × 8), doğru cevap istenir;
      yanlışsa tekrar denetir. Hesap makinesi gerektirmeyecek kadar kafadan/kâğıtla
      yapılabilir olmalı.
3. Tüm adımlar boyunca ekranda hep **"Vazgeçtim, çalışmaya dönüyorum"** butonu durur.
   - Basınca: **direniş sayacı +1**, kısa kutlama mesajı, ve **telefon tekrar kilitlenir**
     (kullanıcı fiziksel olarak çalışmaya döner).
4. Üç adımı da geçerse: telefon serbest kalır (**ekranı kapatana kadar**). Bu olay bir
   **giriş** olarak kaydedilir (suçluluk yok, dürüst ayna). Sonraki kilit açılışında
   ritüel yeniden gelir — serbest süre / muafiyet yoktur.
5. **Oturumu bitirme** de ritüelin arkasındadır: kullanıcı bir açılışta ritüeli
   geçtikten sonra uygulamayı açıp **"Oturumu bitir"** e basar. Böylece oturumu bitirmek
   kolay bir "kaçış kapısı" olmaz — bedeli, telefonu kullanmaya hak kazanmakla aynıdır.

---

## 4. Ekranlar

Tüm ekranlar krem/minimal temaya uyar.

- **İlk kurulum (Onboarding):** Gerekli izinleri tek tek, sade dille açıklayıp ister
  (üstte gösterme, erişilebilirlik, bildirim, pil optimizasyonundan muafiyet).
- **Ana ekran:**
  - Oturum yokken: büyük **"Çalışmaya başla"** + seri (streak) + bugünkü direniş sayısı.
  - Oturum açıkken: geçen süre + **"Oturumu bitir"**.
- **Ritüel overlay:** Bölüm 3'teki 3 adım + her zaman görünür "Vazgeçtim" butonu.
- **Takip ekranı:**
  - Toplam direniş sayısı
  - Bugün / bu hafta direniş sayısı
  - Seri (streak) — üst üste en az 1 kez direnilen gün sayısı
  - Giriş sayısı (sade biçimde, dürüst ayna için)
- **Ayarlar:** Kullanıcının kendi motive/caydırıcı metinlerini ekle / düzenle / sil.

---

## 5. Teknik Tasarım

### Genel
- **Dil/UI:** Kotlin + Jetpack Compose, tek Activity + ritüel için overlay.
- **Veri:** Room (yerel SQLite). İnternet izni yok.
- **Min SDK:** 28 (Android 9) — programatik ekran kilitleme için gerekli.

### Veri modeli (taslak)
- `Session` — başlangıç zamanı, bitiş zamanı (null = aktif).
- `ResistanceEvent` — zaman damgası, (opsiyonel) oturum referansı.
- `EntryEvent` — ritüelin geçildiği (telefonun kullanıldığı) olay, zaman damgası.
- `MotivationText` — id, metin.
- Türetilenler: toplam/bugün/bu hafta direniş, seri (streak), giriş sayısı.

### Gözcü (Watcher) bileşenleri
Tümü, ilk kurulumda **bir kez elle verilen** izinlerle çalışır:
- **Foreground Service** — oturum boyunca süreci canlı tutar, kalıcı bildirim gösterir.
- **Kilit açılış algılama** — `ACTION_USER_PRESENT` BroadcastReceiver. Oturum aktifse
  ritüel overlay'i tetikler.
- **Üstte gösterme** — `SYSTEM_ALERT_WINDOW` izni; ritüeli diğer her şeyin önüne çizer.
- **Erişilebilirlik servisi (AccessibilityService):**
  - (a) Kullanıcı Home/Geri ile kaçmaya çalışırsa ritüeli yeniden öne getirir
    (kaçışı zorlaştırır).
  - (b) "Vazgeçtim" e basınca ekranı kilitler (`GLOBAL_ACTION_LOCK_SCREEN`, API 28+).

### Dürüst teknik notlar / riskler
- İzinler ilk kurulumda elle verilir (uygulama Play Store'a değil, doğrudan kendi
  telefonuna kurulur).
- Bazı üretici ROM'ları (Xiaomi, Samsung, vb.) arka plan servisini pil tasarrufuyla
  öldürebilir → onboarding'de "pil optimizasyonundan muaf tut" adımı.
- Overlay + erişilebilirlik kombinasyonu kaçışı zorlaştırır ama **%100 kurşungeçmez
  değildir**. Amaç kurşungeçmezlik değil, yeterli sürtünmedir; bu kabul edilebilir.

---

## 6. Kapsam Dışı (Sonraki Sürümler)

- Ayarlanabilir matematik zorluğu (kolay/orta/zor).
- Ek sürtünme türleri: zorunlu bekleme (geri sayım), uzun metin yazdırma görevi.
- Uygulama-bazlı engelleme seçeneği (yalnızca seçili uygulamalar).
- "Direniş vs giriş" karşılaştırmalı grafikleri.

---

## 7. Karar Özeti

| Konu | Karar |
|------|-------|
| Platform | Android, native Kotlin + Compose |
| Engel türü | Gözcü modu v1'de dahil (gerçek engelleme) |
| Gözcü kapsamı | Telefonun her kilit açılışında ritüel |
| Sürtünme adımları | Caydırıcı yazı → yüzleştiren soru → rastgele orta-zorluk matematik |
| Matematik zorluğu | Orta (kafadan/kâğıtla yapılabilir) |
| Vazgeçince | Direniş +1, telefon tekrar kilitlenir |
| Geçtikten sonra | Ekranı kapatana kadar serbest; her açılışta ritüel tekrar |
| Oturum bitirme | Ritüelin arkasında (kaçış kapısı değil) |
| Oturum modeli | Açık oturum (başlat/bitir) |
| Takip | Toplam + bugün/hafta direniş, seri (streak), giriş sayısı |
| Tema | Krem, minimal, ferah |
| Gizlilik | İnternet izni yok, yerel veri, veri toplama yok |
