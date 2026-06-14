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
