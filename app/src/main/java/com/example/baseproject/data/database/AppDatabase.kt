package com.example.baseproject.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.baseproject.data.database.dao.ToolDao
import com.example.baseproject.data.database.entity.ToolUsageEntity

/**
 * Cơ sở dữ liệu chính của ứng dụng MobiTools.
 */
@Database(entities = [ToolUsageEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        const val DATABASE_NAME = "mobitools_db"
    }

    // Cung cấp DAO quản lý trạng thái công cụ.
    abstract fun toolDao(): ToolDao
}
