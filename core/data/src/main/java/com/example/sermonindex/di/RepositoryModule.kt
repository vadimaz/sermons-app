package com.example.sermonindex.di

import com.example.sermonindex.data.network.ConnectivityNetworkMonitor
import com.example.sermonindex.data.repository.SermonRepositoryImpl
import com.example.sermonindex.data.repository.UserPreferencesRepositoryImpl
import com.example.sermonindex.domain.repository.NetworkMonitor
import com.example.sermonindex.domain.repository.SermonRepository
import com.example.sermonindex.domain.repository.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSermonRepository(
        sermonRepositoryImpl: SermonRepositoryImpl
    ): SermonRepository

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(
        connectivityNetworkMonitor: ConnectivityNetworkMonitor
    ): NetworkMonitor

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        userPreferencesRepositoryImpl: UserPreferencesRepositoryImpl
    ): UserPreferencesRepository
}

