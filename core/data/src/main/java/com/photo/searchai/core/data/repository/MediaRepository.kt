package com.photo.searchai.core.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.sqlite.db.SimpleSQLiteQuery
import com.photo.searchai.core.database.dao.ImageDao
import com.photo.searchai.core.database.entity.ImageEntity
import com.photo.searchai.core.permission.PermissionChecker
import com.photo.searchai.core.permission.PermissionType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Singleton
class MediaRepository
@Inject
constructor(private val imageDao: ImageDao, @ApplicationContext private val context: Context) {
    fun getImageCountFlow(): Flow<Int> = imageDao.getImageCount()
    fun getOcrProcessedCountFlow(): Flow<Int> = imageDao.getOcrProcessedCount()
    fun getAllImages(): Flow<List<ImageEntity>> = imageDao.getAllImages()

    fun getAllImagesPager(): Flow<PagingData<ImageEntity>> {
        return Pager(
                        config = PagingConfig(pageSize = 60, enablePlaceholders = true),
                        pagingSourceFactory = { imageDao.getAllImagesPagingSource() }
                )
                .flow
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

    suspend fun updateOcrResult(id: Long, text: String) {
        imageDao.insertOcrResult(com.photo.searchai.core.database.entity.OcrEntity(id, text, true))
    }

    fun searchImages(query: String): Flow<List<ImageEntity>> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return flowOf(emptyList())

        val tokens = trimmedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }
        val sb = StringBuilder()
        val args = mutableListOf<Any>()

        sb.append("SELECT i.* FROM images i ")
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
