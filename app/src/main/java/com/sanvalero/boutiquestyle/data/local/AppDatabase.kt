package com.sanvalero.boutiquestyle.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sanvalero.boutiquestyle.data.local.dao.BoutiqueDao
import com.sanvalero.boutiquestyle.data.local.entity.*

@Database(
    entities = [UserEntity::class, GarmentEntity::class, VariantEntity::class, SaleEntity::class, SaleItemEntity::class, LayawayEntity::class, StockRequestEntity::class, StockArrivalEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun boutiqueDao(): BoutiqueDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS stock_requests (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, variantId INTEGER NOT NULL, quantity INTEGER NOT NULL, reason TEXT NOT NULL, employeeUserId INTEGER NOT NULL, status TEXT NOT NULL, createdAt INTEGER NOT NULL, reviewedAt INTEGER, adminNote TEXT, FOREIGN KEY(variantId) REFERENCES variants(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(employeeUserId) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_stock_requests_variantId ON stock_requests(variantId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_stock_requests_employeeUserId ON stock_requests(employeeUserId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_stock_requests_status ON stock_requests(status)")
                database.execSQL("CREATE TABLE IF NOT EXISTS stock_arrivals (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, variantId INTEGER NOT NULL, quantity INTEGER NOT NULL, arrivalDate INTEGER NOT NULL, employeeUserId INTEGER NOT NULL, createdAt INTEGER NOT NULL, FOREIGN KEY(variantId) REFERENCES variants(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(employeeUserId) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_stock_arrivals_variantId ON stock_arrivals(variantId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_stock_arrivals_employeeUserId ON stock_arrivals(employeeUserId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_stock_arrivals_arrivalDate ON stock_arrivals(arrivalDate)")
            }
        }
    }
}
