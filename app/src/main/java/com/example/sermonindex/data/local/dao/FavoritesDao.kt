package com.example.sermonindex.data.local.dao

import com.example.sermonindex.data.local.entities.FavoriteSermonEntity
import kotlinx.coroutines.flow.Flow

interface FavoritesDao {
    fun getAllFavorites(): Flow<List<FavoriteSermonEntity>>
    fun isFavorite(id: String): Flow<Boolean>
    suspend fun insertFavorite(entity: FavoriteSermonEntity)
    suspend fun deleteFavorite(id: String)
}
