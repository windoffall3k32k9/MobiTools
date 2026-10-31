package com.example.baseproject.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.baseproject.data.database.dao.ToolDao
import com.example.baseproject.data.database.entity.ToolUsageEntity

/**
 * Cơ sở dữ liệu chính của ứng dụng MobiTools theo mô hình Singleton.
 */
@Database(entities = [ToolUsageEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        const val DATABASE_NAME = "mobitools_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Lấy thể hiện duy nhất của cơ sở dữ liệu ứng dụng.
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }

    // Cung cấp DAO quản lý trạng thái công cụ.
    abstract fun toolDao(): ToolDao
}
