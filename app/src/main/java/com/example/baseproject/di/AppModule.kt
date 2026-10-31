package com.example.baseproject.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.example.baseproject.data.database.AppDatabase
import com.example.baseproject.data.database.dao.ToolDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Cung cấp các phụ thuộc cấp ứng dụng qua Hilt.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Cung cấp SharedPreferences mặc định.
    @Provides
    @Singleton
    @Suppress("DEPRECATION")
    fun provideSharedPreferences(
        @ApplicationContext appContext: Context
    ): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(appContext)
    }

    // Cung cấp thể hiện Room Database của ứng dụng.
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    // Cung cấp ToolDao từ cơ sở dữ liệu Room.
    @Provides
    @Singleton
    fun provideToolDao(database: AppDatabase): ToolDao {
        return database.toolDao()
    }
}
