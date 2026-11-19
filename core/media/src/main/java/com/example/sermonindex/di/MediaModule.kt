package com.example.sermonindex.di

import android.content.Context
import com.example.sermonindex.playback.PlaybackManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    fun providePlaybackManager(@ApplicationContext context: Context): PlaybackManager {
        return PlaybackManager(context)
    }
}
