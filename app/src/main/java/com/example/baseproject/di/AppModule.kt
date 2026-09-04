package com.example.baseproject.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.example.baseproject.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    @Suppress("DEPRECATION")
    fun provideSharedPreferences (
        @ApplicationContext appContext : Context
    ) : SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(appContext)
    }

//    @Provides
//    @Singleton
//    fun provideAppDatabase(
//        @ApplicationContext context: Context
//    ) : AppDatabase {
//        return Room.databaseBuilder(
//            context,
//            AppDatabase::class.java,
//            AppDatabase.DATABASE_NAME
//        )
//            .addMigrations(*AppDatabase.MIGRATIONS)
//            .fallbackToDestructiveMigration()
//            .fallbackToDestructiveMigrationOnDowngrade()
//            .build()
//    }
}