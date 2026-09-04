package com.example.baseproject.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

//@Database(entities = arrayOf(DetailsSound::class), version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
//    companion object {
//        const val DATABASE_NAME = "ringtone"
//    }
//
//    abstract fun favouriteDao() : FavouriteDao
}