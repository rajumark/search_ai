package com.photo.searchai.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
        tableName = "ocr_results",
        foreignKeys =
                [
                        ForeignKey(
                                entity = ImageEntity::class,
                                parentColumns = ["id"],
                                childColumns = ["imageId"],
                                onDelete = ForeignKey.CASCADE
                        )]
)
data class OcrEntity(
        @PrimaryKey val imageId: Long,
        val ocrText: String,
        val isOcrProcessed: Boolean = false
)
