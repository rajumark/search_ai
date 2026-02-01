package com.photo.searchai.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a group of duplicate or near-duplicate images.
 */
@Entity(
    tableName = "duplicate_groups",
    indices = [Index(value = ["groupHash"])]
)
data class DuplicateGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupHash: String, // MD5/SHA-1 of file or composite metadata hash
    val type: String, // "EXACT", "NEAR"
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Entity mapping an image to a duplicate group.
 */
@Entity(
    tableName = "duplicate_mappings",
    primaryKeys = ["mediaStoreId", "groupId"],
    foreignKeys = [
        ForeignKey(
            entity = ImageEntity::class,
            parentColumns = ["mediaStoreId"],
            childColumns = ["mediaStoreId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DuplicateGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["groupId"])]
)
data class DuplicateMappingEntity(
    val mediaStoreId: Long,
    val groupId: Long,
    val isOriginal: Boolean = false, // Suggestion for which one to keep
    val score: Float = 0f // Quality score for comparison
)
