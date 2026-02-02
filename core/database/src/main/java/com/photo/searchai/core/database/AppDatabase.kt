package com.photo.searchai.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.photo.searchai.core.database.dao.ImageDao
import com.photo.searchai.core.database.dao.SnapshotDao
import com.photo.searchai.core.database.entity.ImageEntity
import com.photo.searchai.core.database.entity.ProcessingSnapshotEntity

@Database(
        entities = [ImageEntity::class, ProcessingSnapshotEntity::class],
        version = 3,
        exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao
    abstract fun snapshotDao(): SnapshotDao
}
