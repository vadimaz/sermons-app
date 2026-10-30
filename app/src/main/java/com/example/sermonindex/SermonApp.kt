package com.example.sermonindex

import android.app.Application
import com.example.sermonindex.data.local.AppDatabase
import com.example.sermonindex.data.sync.CatalogUpdateCheckWorker
import com.example.sermonindex.di.IoDispatcher
import com.example.sermonindex.domain.repository.UserPreferencesRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SermonApp : Application() {

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    override fun onCreate() {
        super.onCreate()
        CatalogUpdateCheckWorker.createNotificationChannel(this)

        CoroutineScope(SupervisorJob() + ioDispatcher).launch {
            if (userPreferencesRepository.isAutoCatalogUpdateCheckEnabled.first()) {
                CatalogUpdateCheckWorker.enqueuePeriodicCheck(this@SermonApp)
            }
            try {
                database.openHelper.writableDatabase
            } catch (_: Exception) {
                // Ignore pre-warm failure if database is busy
            }
        }
    }
}
