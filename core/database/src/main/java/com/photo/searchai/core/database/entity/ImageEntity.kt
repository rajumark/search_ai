package com.photo.searchai.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "images")
data class ImageEntity(
        @PrimaryKey val id: Long,
        val uri: String,
        val name: String,
        val dateAdded: Long,
        val size: Long
)
