package com.photo.searchai.core.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.photo.searchai.core.database.dao.ImageDao
import com.photo.searchai.core.database.entity.ImageEntity
import com.photo.searchai.core.permission.PermissionChecker
import com.photo.searchai.core.permission.PermissionType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class MediaRepository
@Inject
constructor(private val imageDao: ImageDao, @ApplicationContext private val context: Context) {
    fun getImageCountFlow(): Flow<Int> = imageDao.getImageCount()
    fun getOcrProcessedCountFlow(): Flow<Int> = imageDao.getOcrProcessedCount()

    suspend fun syncImages() {
        if (!PermissionChecker.hasPermission(context, PermissionType.ALL_FILES)) {
            return
        }

        val images = fetchImagesFromMediaStore()
        if (images.isNotEmpty()) {
            imageDao.upsertImages(images)
        }
    }

    suspend fun getPendingOcrImages(): List<ImageEntity> = imageDao.getPendingOcrImages()

    suspend fun updateOcrResult(id: Long, text: String) {
        imageDao.updateOcrResult(id, text)
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
