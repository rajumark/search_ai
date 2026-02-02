package com.photo.searchai.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
        tableName = "image_keywords",
        primaryKeys = ["imageId", "word"],
        foreignKeys =
                [
                        ForeignKey(
                                entity = ImageEntity::class,
                                parentColumns = ["id"],
                                childColumns = ["imageId"],
                                onDelete = ForeignKey.CASCADE
                        )],
        indices = [Index(value = ["word"])]
)
data class KeywordEntity(val imageId: Long, val word: String, val weight: Float)
