package com.sanvalero.boutiquestyle.di

import android.content.Context
import androidx.room.Room
import com.sanvalero.boutiquestyle.data.local.AppDatabase
import com.sanvalero.boutiquestyle.data.local.dao.BoutiqueDao
import com.sanvalero.boutiquestyle.data.repository.DefaultStoreRepository
import com.sanvalero.boutiquestyle.domain.repository.StoreRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindStoreRepository(repository: DefaultStoreRepository): StoreRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "boutiquestyle.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideDao(database: AppDatabase): BoutiqueDao = database.boutiqueDao()
}
