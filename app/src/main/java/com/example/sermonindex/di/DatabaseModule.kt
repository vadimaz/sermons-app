package com.example.sermonindex.di

import android.content.Context
import androidx.room.Room
import com.example.sermonindex.data.local.AppDatabase
import com.example.sermonindex.data.local.dao.DownloadsDao
import com.example.sermonindex.data.local.dao.FavoritesDao
import com.example.sermonindex.data.local.dao.HistoryDao
import com.example.sermonindex.data.local.dao.SearchDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
    }

    @Provides
    fun provideFavoritesDao(database: AppDatabase): FavoritesDao {
        return database.favoritesDao()
    }

    @Provides
    fun provideHistoryDao(database: AppDatabase): HistoryDao {
        return database.historyDao()
    }

    @Provides
    fun provideDownloadsDao(database: AppDatabase): DownloadsDao {
        return database.downloadsDao()
    }

    @Provides
    fun provideSearchDao(database: AppDatabase): SearchDao {
        return database.searchDao()
    }
}
