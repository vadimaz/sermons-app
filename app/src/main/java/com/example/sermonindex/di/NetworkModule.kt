package com.example.sermonindex.di

import android.content.Context
import com.example.sermonindex.data.network.NetworkClient
import com.example.sermonindex.data.network.SermonIndexApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSermonIndexApiService(@ApplicationContext context: Context): SermonIndexApiService {
        return NetworkClient.createApiService(context)
    }
}
