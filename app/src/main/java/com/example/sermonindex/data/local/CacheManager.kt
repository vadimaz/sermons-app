package com.example.sermonindex.data.local

import android.content.Context
import com.example.sermonindex.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    suspend fun getCacheSizeBytes(): Long = withContext(ioDispatcher) {
        calculateDirectorySize(context.cacheDir)
    }

    suspend fun getFormattedCacheSize(): String = withContext(ioDispatcher) {
        val bytes = calculateDirectorySize(context.cacheDir)
        formatSizeBytes(bytes)
    }

    suspend fun clearCache(): Boolean = withContext(ioDispatcher) {
        try {
            val cacheFiles = context.cacheDir.listFiles()
            var allDeleted = true
            cacheFiles?.forEach { file ->
                val deleted = file.deleteRecursively()
                if (!deleted) allDeleted = false
            }
            allDeleted
        } catch (_: Exception) {
            false
        }
    }

    private fun calculateDirectorySize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var total = 0L
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            total += if (file.isDirectory) {
                calculateDirectorySize(file)
            } else {
                file.length()
            }
        }
        return total
    }

    private fun formatSizeBytes(bytes: Long): String {
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return if (mb >= 1.0) {
            "${(mb * 10).toLong() / 10.0} MB"
        } else {
            "${bytes / 1024} KB"
        }
    }
}
