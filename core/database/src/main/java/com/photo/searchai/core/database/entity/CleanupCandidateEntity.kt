package com.photo.searchai.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a candidate for storage cleanup.
 */
@Entity(
    tableName = "cleanup_candidates",
    foreignKeys = [
        ForeignKey(
            entity = ImageEntity::class,
            parentColumns = ["mediaStoreId"],
            childColumns = ["mediaStoreId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["mediaStoreId"])]
)
data class CleanupCandidateEntity(
    @PrimaryKey val mediaStoreId: Long,
    val reason: String, // "LARGE", "DUPLICATE", "SCREENSHOT", "OLD_WHATSAPP"
    val reclaimableSize: Long,
    val suggestion: String, // "DELETE", "ARCHIVE"
    val identifiedAt: Long = System.currentTimeMillis()
)
