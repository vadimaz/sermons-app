package com.example.sermonindex

import android.app.Application
import com.example.sermonindex.data.local.AppDatabase
import com.example.sermonindex.data.sync.CatalogUpdateCheckWorker
import com.example.sermonindex.di.IoDispatcher
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SermonApp : Application() {

    @Inject
    lateinit var database: AppDatabase

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    override fun onCreate() {
        super.onCreate()
        CatalogUpdateCheckWorker.createNotificationChannel(this)
        CatalogUpdateCheckWorker.enqueuePeriodicCheck(this)

        // Asynchronously pre-warm the Room database connection off the main thread
        CoroutineScope(SupervisorJob() + ioDispatcher).launch {
            try {
                database.openHelper.writableDatabase
            } catch (_: Exception) {
                // Ignore pre-warm failure if database is busy
            }
        }
    }
}
