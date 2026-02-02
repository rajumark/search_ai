package com.photo.searchai.core.database.entity

import androidx.room.ColumnInfo

data class AlbumSummary(
        @ColumnInfo(name = "bucketId") val bucketId: Long,
        @ColumnInfo(name = "bucketName") val bucketName: String,
        @ColumnInfo(name = "imageCount") val imageCount: Int,
        @ColumnInfo(name = "thumbnailUri") val thumbnailUri: String
)
