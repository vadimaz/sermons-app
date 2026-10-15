package com.example.sermonindex.di

import com.example.sermonindex.data.repository.SermonRepositoryImpl
import com.example.sermonindex.domain.repository.SermonRepository
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
}
