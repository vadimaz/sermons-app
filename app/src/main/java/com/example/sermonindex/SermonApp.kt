package com.example.sermonindex

import android.app.Application
import com.example.sermonindex.data.sync.CatalogUpdateCheckWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SermonApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CatalogUpdateCheckWorker.createNotificationChannel(this)
        CatalogUpdateCheckWorker.enqueuePeriodicCheck(this)
    }
}
