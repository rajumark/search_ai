package com.photo.searchai.core.metadata_index

import androidx.exifinterface.media.ExifInterface
import com.photo.searchai.core.database.entity.ExifEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataIndexerImpl @Inject constructor() : MetadataIndexer {

    override suspend fun extractMetadata(mediaStoreId: Long, path: String): ExifEntity = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) {
            return@withContext createDefaultEntity(mediaStoreId, path, hasExif = false)
        }

        try {
            val exif = ExifInterface(path)
            
            val latLong = exif.latLong
            val latitude = latLong?.get(0)
            val longitude = latLong?.get(1)

            ExifEntity(
                mediaStoreId = mediaStoreId,
                make = exif.getAttribute(ExifInterface.TAG_MAKE),
                model = exif.getAttribute(ExifInterface.TAG_MODEL),
                flash = exif.getAttributeInt(ExifInterface.TAG_FLASH, -1).takeIf { it != -1 },
                focalLength = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, -1.0).takeIf { it != -1.0 },
                iso = exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, -1).takeIf { it != -1 },
                exposureTime = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, -1.0).takeIf { it != -1.0 },
                aperture = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, -1.0).takeIf { it != -1.0 },
                width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0),
                height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0),
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL),
                dateTaken = exif.dateTime,
                latitude = latitude,
                longitude = longitude,
                software = exif.getAttribute(ExifInterface.TAG_SOFTWARE),
                isEdited = file.lastModified() > (exif.dateTime ?: 0L),
                hasExif = true
            )
        } catch (e: Exception) {
            createDefaultEntity(mediaStoreId, path, hasExif = false)
        }
    }

    private fun createDefaultEntity(mediaStoreId: Long, path: String, hasExif: Boolean): ExifEntity {
        val file = File(path)
        return ExifEntity(
            mediaStoreId = mediaStoreId,
            make = null,
            model = null,
            flash = null,
            focalLength = null,
            iso = null,
            exposureTime = null,
            aperture = null,
            width = 0,
            height = 0,
            orientation = 1, // NORMAL
            dateTaken = if (file.exists()) file.lastModified() else null,
            latitude = null,
            longitude = null,
            software = null,
            isEdited = false,
            hasExif = hasExif
        )
    }
}
