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
