package com.sanvalero.boutiquestyle.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sanvalero.boutiquestyle.data.local.dao.BoutiqueDao
import com.sanvalero.boutiquestyle.data.local.entity.*

@Database(
    entities = [UserEntity::class, GarmentEntity::class, VariantEntity::class, SaleEntity::class, SaleItemEntity::class, LayawayEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun boutiqueDao(): BoutiqueDao
}
