package com.example.sermonindex

import android.app.Application
import com.example.sermonindex.data.local.AppDatabase
import com.example.sermonindex.data.network.NetworkClient
import com.example.sermonindex.data.repository.SermonRepository

class SermonApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: SermonRepository
        private set

    lateinit var playbackManager: com.example.sermonindex.playback.PlaybackManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        val apiService = NetworkClient.createApiService(this)
        repository = SermonRepository(apiService, database)
        playbackManager = com.example.sermonindex.playback.PlaybackManager(this)
    }

    companion object {
        lateinit var instance: SermonApp
            private set
    }
}
