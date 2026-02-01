package com.photo.searchai.core.duplicate_engine

import com.photo.searchai.core.media_index.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class DuplicateEngineImpl @Inject constructor() : DuplicateEngine {

    override suspend fun computeHash(path: String): String = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) return@withContext ""

            val digest = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(8192)
            FileInputStream(file).use { fis ->
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    override fun isNearDuplicate(media1: MediaItem, media2: MediaItem): Boolean {
        // Rule: Same resolution + Size delta < 5% + Time delta < 10 mins
        val sameResolution = media1.width == media2.width && media1.height == media2.height
        val sizeDelta = abs(media1.size - media2.size).toDouble() / media1.size
        val timeDelta = abs(media1.dateAdded - media2.dateAdded)
        
        return sameResolution && sizeDelta < 0.05 && timeDelta < 600
    }

    override fun rankDuplicates(mediaList: List<MediaItem>): Pair<MediaItem, List<MediaItem>> {
        if (mediaList.isEmpty()) throw IllegalArgumentException("List cannot be empty")
        
        // Rank by: 1. isFavorite, 2. Resolution (width * height), 3. DateModified (more recent)
        val sorted = mediaList.sortedWith(
            compareByDescending<MediaItem> { it.isFavorite }
                .thenByDescending { it.width * it.height }
                .thenByDescending { it.dateModified }
        )
        
        val original = sorted.first()
        val duplicates = sorted.drop(1)
        
        return Pair(original, duplicates)
    }
}
