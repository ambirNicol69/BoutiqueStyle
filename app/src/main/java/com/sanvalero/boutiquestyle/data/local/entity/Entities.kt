package com.sanvalero.boutiquestyle.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users", indices = [Index(value = ["username"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val role: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "garments")
data class GarmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val category: String,
    val brand: String = "",
    val basePrice: Double,
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "variants",
    foreignKeys = [ForeignKey(
        entity = GarmentEntity::class,
        parentColumns = ["id"],
        childColumns = ["garmentId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("garmentId"), Index(value = ["sku"], unique = true)]
)
data class VariantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val garmentId: Long,
    val size: String,
    val color: String,
    val stock: Int,
    val sku: String,
    val barcode: String? = null
)

@Entity(
    tableName = "sales",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index("userId"), Index("saleDate")]
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerName: String,
    val customerCedulaOrRnc: String,
    val saleDate: Long = System.currentTimeMillis(),
    val subtotal: Double,
    val itbis: Double,
    val total: Double,
    val paymentMethod: String,
    val userId: Long,
    val notes: String = ""
)

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(entity = SaleEntity::class, parentColumns = ["id"], childColumns = ["saleId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = VariantEntity::class, parentColumns = ["id"], childColumns = ["variantId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("saleId"), Index("variantId")]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val variantId: Long,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double
)

@Entity(
    tableName = "layaways",
    foreignKeys = [
        ForeignKey(entity = VariantEntity::class, parentColumns = ["id"], childColumns = ["variantId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("variantId"), Index("userId"), Index("status"), Index("createdAt")]
)
data class LayawayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerName: String,
    val customerPhone: String,
    val customerCedula: String,
    val variantId: Long,
    val quantity: Int,
    val totalAmount: Double,
    val amountPaid: Double,
    val balance: Double,
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis(),
    val dueDate: Long,
    val userId: Long
)

@Entity(
    tableName = "stock_requests",
    foreignKeys = [
        ForeignKey(entity = VariantEntity::class, parentColumns = ["id"], childColumns = ["variantId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["employeeUserId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("variantId"), Index("employeeUserId"), Index("status")]
)
data class StockRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val variantId: Long,
    val quantity: Int,
    val reason: String,
    val employeeUserId: Long,
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val adminNote: String? = null
)

@Entity(
    tableName = "stock_arrivals",
    foreignKeys = [
        ForeignKey(entity = VariantEntity::class, parentColumns = ["id"], childColumns = ["variantId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["employeeUserId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("variantId"), Index("employeeUserId"), Index("arrivalDate")]
)
data class StockArrivalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val variantId: Long,
    val quantity: Int,
    val arrivalDate: Long,
    val employeeUserId: Long,
    val createdAt: Long = System.currentTimeMillis()
)
