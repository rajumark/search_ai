package com.photo.searchai.datasource

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.max

/**
 * Data source for accessing photos from device storage via MediaStore.
 */
class PhotoDataSource @Inject constructor(
    private val contentResolver: ContentResolver
) {
    companion object {
        private const val MAX_BITMAP_DIMENSION = 1024
    }
    
    /**
     * Gets the total count of photos on the device.
     * Runs on IO dispatcher to avoid blocking main thread.
     */
    suspend fun getTotalPhotoCount(): Int = withContext(Dispatchers.IO) {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)
        
        contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            cursor.count
        } ?: 0
    }
    
    /**
     * Gets all image metadata from MediaStore.
     * Returns list of ImageMetadata with id, path, and dateAdded.
     */
    suspend fun getAllImageMetadata(): List<ImageMetadata> = withContext(Dispatchers.IO) {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED
        )
        
        val images = mutableListOf<ImageMetadata>()
        
        contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val path = cursor.getString(pathColumn) ?: continue
                val dateAdded = cursor.getLong(dateColumn)
                
                images.add(ImageMetadata(id, path, dateAdded))
            }
        }
        
        images
    }
    
    /**
     * Gets image path by mediaStoreId.
     */
    suspend fun getImagePath(mediaStoreId: Long): String? = withContext(Dispatchers.IO) {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val selection = "${MediaStore.Images.Media._ID} = ?"
        val selectionArgs = arrayOf(mediaStoreId.toString())
        
        contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
            } else null
        }
    }
    
    /**
     * Gets a scaled bitmap for OCR processing.
     * Max dimension is 1024px to avoid memory issues.
     * Returns null if bitmap cannot be decoded.
     */
    suspend fun getScaledBitmap(path: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // First, decode bounds only to calculate sample size
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)
            
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return@withContext null
            }
            
            // Calculate sample size
            val maxDimension = max(options.outWidth, options.outHeight)
            var sampleSize = 1
            while (maxDimension / sampleSize > MAX_BITMAP_DIMENSION) {
                sampleSize *= 2
            }
            
            // Decode with sample size
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Less memory
            }
            
            BitmapFactory.decodeFile(path, decodeOptions)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Gets content URI for an image by mediaStoreId.
     */
    fun getContentUri(mediaStoreId: Long): Uri {
        return Uri.withAppendedPath(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            mediaStoreId.toString()
        )
    }
}
