package com.photo.searchai.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "image_groups")
data class GroupEntity(
        @PrimaryKey(autoGenerate = true) val groupId: Long = 0,
        val groupKey: String, // sorted top 3 keywords
        val imageCount: Int,
        val lastUpdated: Long
)
