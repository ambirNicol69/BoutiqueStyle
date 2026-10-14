package com.sanvalero.boutiquestyle.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sanvalero.boutiquestyle.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BoutiqueDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findUser(username: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun userCount(): Int

    @Query("SELECT * FROM users ORDER BY name")
    fun observeUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findUserById(id: Long): UserEntity?

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("SELECT COUNT(*) FROM users WHERE role = 'ADMIN' AND id != :userId")
    suspend fun otherAdminCount(userId: Long): Int

    @Insert
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users")
    suspend fun allUsers(): List<UserEntity>

    @Query("SELECT * FROM garments")
    suspend fun allGarments(): List<GarmentEntity>

    @Insert
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("SELECT * FROM garments ORDER BY name")
    fun observeGarments(): Flow<List<GarmentEntity>>

    @Query("SELECT * FROM garments WHERE id = :id")
    suspend fun getGarment(id: Long): GarmentEntity?

    @Insert
    suspend fun insertGarment(garment: GarmentEntity): Long

    @Update
    suspend fun updateGarment(garment: GarmentEntity)

    @Delete
    suspend fun deleteGarment(garment: GarmentEntity)

    @Insert
    suspend fun insertGarments(garments: List<GarmentEntity>)

    @Query("SELECT * FROM variants ORDER BY garmentId, size, color")
    fun observeVariants(): Flow<List<VariantEntity>>

    @Query("SELECT * FROM variants WHERE garmentId = :garmentId ORDER BY size, color")
    fun observeVariantsForGarment(garmentId: Long): Flow<List<VariantEntity>>

    @Query("SELECT * FROM variants WHERE id = :id")
    suspend fun getVariant(id: Long): VariantEntity?

    @Insert
    suspend fun insertVariant(variant: VariantEntity): Long

    @Update
    suspend fun updateVariant(variant: VariantEntity)

    @Delete
    suspend fun deleteVariant(variant: VariantEntity)

    @Query("SELECT * FROM variants")
    suspend fun allVariants(): List<VariantEntity>

    @Insert
    suspend fun insertVariants(variants: List<VariantEntity>)

    @Query("UPDATE variants SET stock = stock - :quantity WHERE id = :variantId AND stock >= :quantity")
    suspend fun decrementStock(variantId: Long, quantity: Int): Int

    @Query("UPDATE variants SET stock = stock + :quantity WHERE id = :variantId")
    suspend fun restoreStock(variantId: Long, quantity: Int)

    @Query("UPDATE variants SET stock = stock + :quantity WHERE id = :variantId")
    suspend fun increaseStock(variantId: Long, quantity: Int): Int

    @Query("SELECT * FROM sales ORDER BY saleDate DESC")
    fun observeSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun saleItems(saleId: Long): List<SaleItemEntity>

    @Query("SELECT * FROM sale_items ORDER BY id")
    fun observeSaleItems(): Flow<List<SaleItemEntity>>

    @Insert
    suspend fun insertSale(sale: SaleEntity): Long

    @Query("SELECT * FROM sales")
    suspend fun allSales(): List<SaleEntity>

    @Update
    suspend fun updateSale(sale: SaleEntity)

    @Delete
    suspend fun deleteSale(sale: SaleEntity)

    @Insert
    suspend fun insertSales(sales: List<SaleEntity>)

    @Insert
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Query("SELECT * FROM sale_items")
    suspend fun allSaleItems(): List<SaleItemEntity>

    @Insert
    suspend fun insertAllSaleItems(items: List<SaleItemEntity>)

    @Query("SELECT * FROM layaways ORDER BY createdAt DESC")
    fun observeLayaways(): Flow<List<LayawayEntity>>

    @Query("SELECT * FROM layaways WHERE id = :id")
    suspend fun getLayaway(id: Long): LayawayEntity?

    @Insert
    suspend fun insertLayaway(layaway: LayawayEntity): Long

    @Update
    suspend fun updateLayaway(layaway: LayawayEntity)

    @Delete
    suspend fun deleteLayaway(layaway: LayawayEntity)

    @Query("SELECT * FROM layaways")
    suspend fun allLayaways(): List<LayawayEntity>

    @Insert
    suspend fun insertLayaways(layaways: List<LayawayEntity>)

    @Query("SELECT * FROM stock_requests ORDER BY createdAt DESC")
    fun observeStockRequests(): Flow<List<StockRequestEntity>>

    @Query("SELECT * FROM stock_requests ORDER BY createdAt DESC")
    suspend fun allStockRequests(): List<StockRequestEntity>

    @Insert
    suspend fun insertStockRequest(request: StockRequestEntity): Long

    @Insert
    suspend fun insertStockRequests(requests: List<StockRequestEntity>)

    @Update
    suspend fun updateStockRequest(request: StockRequestEntity)

    @Query("SELECT * FROM stock_arrivals ORDER BY arrivalDate DESC, id DESC")
    fun observeStockArrivals(): Flow<List<StockArrivalEntity>>

    @Query("SELECT * FROM stock_arrivals ORDER BY arrivalDate DESC, id DESC")
    suspend fun allStockArrivals(): List<StockArrivalEntity>

    @Insert
    suspend fun insertStockArrivals(arrivals: List<StockArrivalEntity>)

    @Query("SELECT COUNT(*) FROM sales WHERE saleDate >= :start AND saleDate < :end")
    fun observeSalesCount(start: Long, end: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(total), 0) FROM sales WHERE saleDate >= :start AND saleDate < :end")
    fun observeSalesTotal(start: Long, end: Long): Flow<Double>

    @Query("SELECT COUNT(*) FROM layaways WHERE status = 'ACTIVE'")
    fun observeActiveLayawayCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM variants WHERE stock <= :threshold")
    fun observeLowStockCount(threshold: Int): Flow<Int>
}
