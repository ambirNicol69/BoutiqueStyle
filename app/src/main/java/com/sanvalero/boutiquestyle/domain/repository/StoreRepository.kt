package com.sanvalero.boutiquestyle.domain.repository

import android.net.Uri
import com.sanvalero.boutiquestyle.data.local.entity.*
import com.sanvalero.boutiquestyle.domain.model.*
import kotlinx.coroutines.flow.Flow

interface StoreRepository {
    suspend fun initialize()
    suspend fun login(username: String, password: String): UserEntity?
    suspend fun registerCustomer(username: String, password: String, name: String): UserEntity
    fun garments(): Flow<List<GarmentEntity>>
    fun variants(): Flow<List<VariantEntity>>
    fun variants(garmentId: Long): Flow<List<VariantEntity>>
    fun sales(): Flow<List<SaleEntity>>
    fun layaways(): Flow<List<LayawayEntity>>
    fun saleItems(): Flow<List<SaleItemEntity>>
    fun stockRequests(): Flow<List<StockRequestEntity>>
    fun stockArrivals(): Flow<List<StockArrivalEntity>>
    fun users(): Flow<List<UserEntity>>
    fun dashboard(): Flow<DashboardSummary>
    suspend fun saveGarment(garment: GarmentEntity)
    suspend fun removeGarment(garment: GarmentEntity)
    suspend fun saveVariant(variant: VariantEntity)
    suspend fun registerStockProduct(garment: GarmentEntity, variants: List<VariantEntity>)
    suspend fun requestStock(request: StockRequestEntity)
    suspend fun reviewStockRequest(request: StockRequestEntity)
    suspend fun recordStockArrivals(arrivals: List<StockArrivalEntity>)
    suspend fun removeVariant(variant: VariantEntity)
    suspend fun saveUser(user: UserEntity, newPassword: String)
    suspend fun removeUser(user: UserEntity)
    suspend fun createSale(request: SaleRequest): Long
    suspend fun updateSale(sale: SaleEntity)
    suspend fun removeSale(sale: SaleEntity)
    suspend fun createLayaway(layaway: LayawayEntity): Long
    suspend fun updateLayaway(layaway: LayawayEntity)
    suspend fun exportBackup(destination: Uri)
    suspend fun importBackup(source: Uri)
}
