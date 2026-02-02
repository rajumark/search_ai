package com.photo.searchai.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
        tableName = "group_images",
        primaryKeys = ["groupId", "imageId"],
        foreignKeys =
                [
                        ForeignKey(
                                entity = GroupEntity::class,
                                parentColumns = ["groupId"],
                                childColumns = ["groupId"],
                                onDelete = ForeignKey.CASCADE
                        ),
                        ForeignKey(
                                entity = ImageEntity::class,
                                parentColumns = ["id"],
                                childColumns = ["imageId"],
                                onDelete = ForeignKey.CASCADE
                        )],
        indices = [Index(value = ["imageId"])]
)
data class GroupImageEntity(val groupId: Long, val imageId: Long)
