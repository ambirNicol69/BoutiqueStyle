package com.sanvalero.boutiquestyle.domain.model

import com.sanvalero.boutiquestyle.data.local.entity.*

data class DashboardSummary(
    val daySales: Int = 0,
    val monthSales: Int = 0,
    val activeLayaways: Int = 0,
    val lowStock: Int = 0,
    val monthIncome: Double = 0.0
)

data class StoreUiState(
    val garments: List<GarmentEntity> = emptyList(),
    val variants: List<VariantEntity> = emptyList(),
    val sales: List<SaleEntity> = emptyList(),
    val layaways: List<LayawayEntity> = emptyList(),
    val saleItems: List<SaleItemEntity> = emptyList(),
    val users: List<UserEntity> = emptyList(),
    val dashboard: DashboardSummary = DashboardSummary()
)

data class CartLine(val variant: VariantEntity, val quantity: Int, val unitPrice: Double)

data class SaleRequest(
    val customerName: String,
    val customerId: String,
    val paymentMethod: String,
    val userId: Long,
    val items: List<CartLine>,
    val notes: String = ""
)
