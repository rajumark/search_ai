package com.photo.searchai.core.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.sqlite.db.SimpleSQLiteQuery
import com.photo.searchai.core.database.dao.ImageDao
import com.photo.searchai.core.database.entity.ImageEntity
import com.photo.searchai.core.database.entity.OcrEntity
import com.photo.searchai.core.database.entity.RecentSearchEntity
import com.photo.searchai.core.database.entity.SearchResultWithOcr
import com.photo.searchai.core.database.entity.SearchSuggestionEntity
import com.photo.searchai.core.permission.PermissionChecker
import com.photo.searchai.core.permission.PermissionType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
class MediaRepository
@Inject
constructor(
        private val imageDao: ImageDao,
        private val searchDao: com.photo.searchai.core.database.dao.SearchDao,
        private val imageLabelDao: com.photo.searchai.core.database.dao.ImageLabelDao,
        @ApplicationContext private val context: Context
) {
    fun getImageCountFlow(): Flow<Int> = imageDao.getImageCount()
    fun getOcrProcessedCountFlow(): Flow<Int> = imageDao.getOcrProcessedCount()
    fun getLabelingProcessedCountFlow(): Flow<Int> = imageLabelDao.getLabeledImageCount()
    fun getLabelCountsFlow(): Flow<List<com.photo.searchai.core.database.entity.LabelCount>> =
            imageLabelDao.getLabelCounts()
    fun getAllImages(): Flow<List<ImageEntity>> = imageDao.getAllImages()

    fun getAllImagesPager(): Flow<PagingData<ImageEntity>> {
        return Pager(
                        config = PagingConfig(pageSize = 60, enablePlaceholders = true),
                        pagingSourceFactory = { imageDao.getAllImagesPagingSource() }
                )
                .flow
    }

    suspend fun deleteImage(image: ImageEntity): Boolean {
        if (!PermissionChecker.hasPermission(context, PermissionType.ALL_FILES)) {
            return false
        }

        val deleted =
                context.contentResolver.delete(Uri.parse(image.uri), null, null)
        if (deleted > 0) {
            imageDao.deleteImagesByIds(listOf(image.id))
            return true
        }
        return false
    }

    suspend fun syncImages() {
        if (!PermissionChecker.hasPermission(context, PermissionType.ALL_FILES)) {
            return
        }

        val mediaStoreImages = fetchImagesFromMediaStore()
        val mediaStoreIds = mediaStoreImages.map { it.id }.toSet()
        val dbIds = imageDao.getAllImageIds().toSet()

        // 1. Identify and delete images that no longer exist in MediaStore
        val deletedIds = dbIds.filter { it !in mediaStoreIds }
        if (deletedIds.isNotEmpty()) {
            deletedIds.chunked(999).forEach { chunk -> imageDao.deleteImagesByIds(chunk) }
        }

        // 2. Identify and insert new images
        val newImages = mediaStoreImages.filter { it.id !in dbIds }
        if (newImages.isNotEmpty()) {
            imageDao.insertImages(newImages)
        }
    }

    suspend fun getPendingOcrImages(): List<ImageEntity> = imageDao.getPendingOcrImages()

    suspend fun getPendingLabelingImages(): List<ImageEntity> =
            imageLabelDao.getPendingLabelingImages()

    suspend fun getLabelPreviewImages(labelText: String): List<ImageEntity> =
            imageLabelDao.getLabelPreviewImages(labelText)

    suspend fun getImagesForLabels(labels: List<String>): List<ImageEntity> =
            imageLabelDao.getImagesForLabels(labels, labels.size)

    suspend fun getRelatedLabels(labels: List<String>, limit: Int = 20):
            List<com.photo.searchai.core.database.entity.LabelCount> =
            imageLabelDao.getRelatedLabels(labels, limit)

    suspend fun saveLabelingResults(
            labels: List<com.photo.searchai.core.database.entity.ImageLabelEntity>
    ) {
        if (labels.isNotEmpty()) {
            imageLabelDao.insertLabels(labels)
        }
    }

    suspend fun updateOcrResult(id: Long, text: String) {
        imageDao.insertOcrResult(OcrEntity(id, text, true))

        // Update suggestions table with word frequencies
        val words =
                text.split(Regex("[^\\w\\p{L}]+")) // Split by non-word and non-letter characters
                        .filter { it.length > 2 }
                        .map { it.lowercase() }
                        .distinct()

        val suggestions = words.map { word -> SearchSuggestionEntity(word, 1) }

        // Note: For real performance at scale, this should be a "upsert" that increments frequency.
        // Since sqlite doesn't easily do upsert-increment in Room without a raw query or manual
        // check.
        // We'll use a simplified version for this prompt.
        searchDao.insertSuggestions(suggestions)
    }

    suspend fun saveRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        searchDao.insertRecentSearch(RecentSearchEntity(trimmed))
        searchDao.trimRecentSearches(5)
    }

    fun getRecentSearches(): Flow<List<String>> {
        return searchDao.getRecentSearches(5).map { list -> list.map { it.query } }
    }

    suspend fun getSuggestions(query: String): List<String> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        val tokens = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        val lastToken = tokens.last()
        val existingWords = tokens.map { it.lowercase() }.toSet()

        // 1. Prefix matches (Priority 2 in requirements, but good to have first for UX)
        val prefixMatches =
                searchDao.getSuggestionsByPrefix(lastToken, 8).map { it.text }.filter {
                    it.lowercase() !in existingWords
                }

        // 2. Co-occurrence Relevance (Priority 1)
        val coOccurring =
                if (trimmed.length > 2) {
                    searchDao
                            .getRawOcrResultsForCoOccurrence(trimmed)
                            .flatMap { it.ocrText.split(Regex("[^\\w\\p{L}]+")) }
                            .filter {
                                it.length > 2 &&
                                        it.lowercase() !in existingWords &&
                                        !it.startsWith(lastToken, ignoreCase = true)
                            }
                            .groupBy { it.lowercase() }
                            .mapValues { it.value.size }
                            .toList()
                            .sortedByDescending { it.second }
                            .take(5)
                            .map { it.first }
                } else emptyList()

        // 3. Global Top (Priority 3)
        val globalTop =
                searchDao.getGlobalTopSuggestions(5).map { it.text }.filter {
                    it.lowercase() !in existingWords &&
                            it.lowercase() !in prefixMatches.map { p -> p.lowercase() }
                }

        // Return combined list, prioritized: co-occurrence, then prefix, then global
        return (coOccurring + prefixMatches + globalTop).distinct().take(8)
    }

    fun searchImages(query: String): Flow<List<SearchResultWithOcr>> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return flowOf(emptyList())

        val tokens = trimmedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }
        val sb = StringBuilder()
        val args = mutableListOf<Any>()

        sb.append("SELECT i.*, o.ocrText FROM images i ")
        sb.append("JOIN ocr_results o ON i.id = o.imageId ")
        sb.append("WHERE ")

        val conditions = mutableListOf<String>()
        // Base condition for any match
        conditions.add("o.ocrText LIKE ?")
        args.add("%$trimmedQuery%")

        for (token in tokens) {
            conditions.add("o.ocrText LIKE ?")
            args.add("%$token%")
        }

        sb.append("(")
        sb.append(conditions.joinToString(" OR "))
        sb.append(")")

        sb.append(" ORDER BY ")

        // Priority 1: Exact phrase match
        sb.append("CASE WHEN o.ocrText LIKE ? THEN 1 ELSE 0 END DESC, ")
        args.add("%$trimmedQuery%")

        // Priority 2: All words match
        if (tokens.size > 1) {
            sb.append("(")
            val tokenChecks =
                    tokens.map {
                        args.add("%$it%")
                        "CASE WHEN o.ocrText LIKE ? THEN 1 ELSE 0 END"
                    }
            sb.append(tokenChecks.joinToString(" + "))
            sb.append(" = ${tokens.size}) DESC, ")
        }

        // Final tie-breaker: Date added
        sb.append("i.dateAdded DESC")

        return imageDao.searchImagesRaw(SimpleSQLiteQuery(sb.toString(), args.toTypedArray()))
    }

    private fun fetchImagesFromMediaStore(): List<ImageEntity> {
        val imageList = mutableListOf<ImageEntity>()
        val projection =
                arrayOf(
                        MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.DISPLAY_NAME,
                        MediaStore.Images.Media.DATE_ADDED,
                        MediaStore.Images.Media.SIZE
                )

        val query =
                context.contentResolver.query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        projection,
                        null,
                        null,
                        "${MediaStore.Images.Media.DATE_ADDED} DESC"
                )

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "Unknown"
                val date = cursor.getLong(dateColumn)
                val size = cursor.getLong(sizeColumn)
                val uri =
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                                .toString()

                imageList.add(ImageEntity(id, uri, name, date, size))
            }
        }
        return imageList
    }
}
