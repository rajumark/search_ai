package com.photo.searchai.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.photo.searchai.core.database.dao.ImageDao
import com.photo.searchai.core.database.entity.ImageEntity

@Database(entities = [ImageEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao
}
