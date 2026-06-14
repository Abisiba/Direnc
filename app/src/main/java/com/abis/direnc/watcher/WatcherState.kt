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
