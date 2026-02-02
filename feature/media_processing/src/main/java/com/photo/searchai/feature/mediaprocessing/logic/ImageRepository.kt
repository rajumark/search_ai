package com.photo.searchai.feature.mediaprocessing.logic

import android.content.Context
import android.provider.MediaStore
import com.photo.searchai.core.database.entity.ImageEntity
import com.photo.searchai.core.permissions.logic.PermissionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ImageRepository
@Inject
constructor(
        @ApplicationContext private val context: Context,
        private val permissionManager: PermissionManager
) {

        fun getAllImages(): List<ImageEntity> {
                if (!permissionManager.hasStoragePermission(context)) {
                        return emptyList()
                }
                val images = mutableListOf<ImageEntity>()
                val projection =
                        arrayOf(
                                MediaStore.Images.Media._ID,
                                MediaStore.Images.Media.DATA,
                                MediaStore.Images.Media.DATE_ADDED
                        )

                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

                context.contentResolver.query(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                projection,
                                null,
                                null,
                                sortOrder
                        )
                        ?.use { cursor ->
                                val idColumn =
                                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                                val pathColumn =
                                        cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                                val dateAddedColumn =
                                        cursor.getColumnIndexOrThrow(
                                                MediaStore.Images.Media.DATE_ADDED
                                        )

                                while (cursor.moveToNext()) {
                                        val id = cursor.getLong(idColumn).toString()
                                        val path = cursor.getString(pathColumn)
                                        val dateAdded = cursor.getLong(dateAddedColumn)

                                        images.add(
                                                ImageEntity(
                                                        imageId = id,
                                                        path = path,
                                                        dateAdded =
                                                                dateAdded *
                                                                        1000 // Convert to millis if
                                                        // needed, usually
                                                        // seconds in MediaStore
                                                        )
                                        )
                                }
                        }
                return images
        }
}
