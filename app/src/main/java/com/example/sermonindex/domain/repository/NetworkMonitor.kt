package com.example.sermonindex.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Pure domain interface for monitoring network availability.
 */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
