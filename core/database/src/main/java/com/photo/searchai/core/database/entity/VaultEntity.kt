package com.photo.searchai.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity tracking images moved to the private vault.
 */
@Entity(
    tableName = "vault_entries",
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
data class VaultEntity(
    @PrimaryKey val mediaStoreId: Long,
    val originalPath: String,
    val vaultPath: String,
    val movedAt: Long = System.currentTimeMillis(),
    val isLocked: Boolean = true
)
