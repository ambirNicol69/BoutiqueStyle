package com.sanvalero.boutiquestyle.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.room.withTransaction
import com.sanvalero.boutiquestyle.data.local.entity.SaleItemEntity
import com.sanvalero.boutiquestyle.data.local.AppDatabase
import com.sanvalero.boutiquestyle.data.local.dao.BoutiqueDao
import com.sanvalero.boutiquestyle.data.local.entity.*
import com.sanvalero.boutiquestyle.domain.model.*
import com.sanvalero.boutiquestyle.domain.repository.StoreRepository
import com.sanvalero.boutiquestyle.domain.usecase.CalculateSaleTotalsUseCase
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultStoreRepository @Inject constructor(
    private val database: AppDatabase,
    private val dao: BoutiqueDao,
    @ApplicationContext private val context: Context,
    private val calculateSaleTotals: CalculateSaleTotalsUseCase
) : StoreRepository {
    override suspend fun initialize() {
        database.withTransaction {
            if (dao.userCount() == 0) {
                dao.insertUser(UserEntity(username = "admin", passwordHash = hash("admin123"), role = "ADMIN", name = "Administradora"))
                dao.insertUser(UserEntity(username = "empleado", passwordHash = hash("oper123"), role = "OPERATOR", name = "Empleado"))
            } else {
                val oldEmployee = dao.findUser("operador")
                if (oldEmployee != null && dao.findUser("empleado") == null) {
                    dao.updateUser(oldEmployee.copy(username = "empleado", name = "Empleado"))
                }
            }
            if (dao.observeGarmentsSnapshotIsEmpty()) {
                val dress = dao.insertGarment(GarmentEntity(name = "Vestido Rosa Aurora", description = "Vestido de corte elegante para ocasiones especiales.", category = "Vestido", brand = "BoutiqueStyle", basePrice = 2450.0))
                val blouse = dao.insertGarment(GarmentEntity(name = "Blusa Lino Caribe", description = "Lino fresco, ideal para el clima tropical.", category = "Blusa", brand = "Isla", basePrice = 1350.0))
                val dressM = dao.insertVariant(VariantEntity(garmentId = dress, size = "M", color = "Rosa", stock = 3, sku = "BS-VRA-M-RS"))
                dao.insertVariant(VariantEntity(garmentId = dress, size = "L", color = "Rosa", stock = 2, sku = "BS-VRA-L-RS"))
                dao.insertVariant(VariantEntity(garmentId = blouse, size = "S", color = "Marfil", stock = 8, sku = "BS-BLC-S-MF"))
                val blouseM = dao.insertVariant(VariantEntity(garmentId = blouse, size = "M", color = "Marfil", stock = 5, sku = "BS-BLC-M-MF"))
                val adminId = dao.findUser("admin")?.id ?: error("No se encontró el usuario ADMIN de demostración.")
                val employeeId = dao.findUser("empleado")?.id ?: adminId
                val sampleSaleId = dao.insertSale(
                    SaleEntity(
                        customerName = "Cliente de muestra",
                        customerCedulaOrRnc = "00112345673",
                        subtotal = 1350.0,
                        itbis = 243.0,
                        total = 1593.0,
                        paymentMethod = "Efectivo",
                        userId = adminId,
                        notes = "Venta de demostración"
                    )
                )
                dao.insertSaleItems(
                    listOf(SaleItemEntity(saleId = sampleSaleId, variantId = blouseM, quantity = 1, unitPrice = 1350.0, lineTotal = 1350.0))
                )
                check(dao.decrementStock(blouseM, 1) == 1) { "No se pudo reservar el stock de la venta de demostración." }
                dao.insertLayaway(
                    LayawayEntity(
                        customerName = "María Ejemplo",
                        customerPhone = "809-555-0100",
                        customerCedula = "402123455",
                        variantId = dressM,
                        quantity = 1,
                        totalAmount = 2450.0,
                        amountPaid = 500.0,
                        balance = 1950.0,
                        dueDate = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
                        userId = employeeId
                    )
                )
                check(dao.decrementStock(dressM, 1) == 1) { "No se pudo reservar el stock del apartado de demostración." }
            }
        }
    }

    override suspend fun login(username: String, password: String): UserEntity? {
        initialize()
        val user = dao.findUser(username) ?: return null
        return user.takeIf { verify(password, it.passwordHash) }
    }

    override suspend fun registerCustomer(username: String, password: String, name: String): UserEntity {
        initialize()
        return database.withTransaction {
            val normalizedUsername = username.trim()
            val normalizedName = name.trim()
            require(normalizedUsername.length >= 3) { "El usuario debe tener al menos 3 caracteres." }
            require(password.length >= 6) { "La contraseña debe tener al menos 6 caracteres." }
            require(normalizedName.isNotBlank()) { "Escribe tu nombre." }
            require(dao.findUser(normalizedUsername) == null) { "Ese usuario ya está registrado." }
            val customer = UserEntity(username = normalizedUsername, passwordHash = hash(password), role = "CUSTOMER", name = normalizedName)
            customer.copy(id = dao.insertUser(customer))
        }
    }

    override fun garments() = dao.observeGarments()
    override fun variants() = dao.observeVariants()
    override fun variants(garmentId: Long) = dao.observeVariantsForGarment(garmentId)
    override fun sales() = dao.observeSales()
    override fun layaways() = dao.observeLayaways()
    override fun saleItems() = dao.observeSaleItems()
    override fun stockRequests() = dao.observeStockRequests()
    override fun stockArrivals() = dao.observeStockArrivals()
    override fun users() = dao.observeUsers()

    override fun dashboard(): Flow<DashboardSummary> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val monthStart = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val monthEnd = today.withDayOfMonth(1).plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return combine(
            dao.observeSalesCount(dayStart, dayEnd),
            dao.observeSalesCount(monthStart, monthEnd),
            dao.observeActiveLayawayCount(),
            dao.observeLowStockCount(3),
            dao.observeSalesTotal(monthStart, monthEnd)
        ) { day, month, active, lowStock, income ->
            DashboardSummary(day, month, active, lowStock, income)
        }
    }

    override suspend fun saveGarment(garment: GarmentEntity) {
        if (garment.id == 0L) dao.insertGarment(garment) else dao.updateGarment(garment.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun removeGarment(garment: GarmentEntity) = dao.deleteGarment(garment)

    override suspend fun saveVariant(variant: VariantEntity) {
        require(variant.stock >= 0) { "El inventario no puede ser negativo." }
        val variantWithSku = variant.copy(sku = variant.sku.ifBlank { generateSku() })
        if (variant.id == 0L) dao.insertVariant(variantWithSku) else dao.updateVariant(variantWithSku)
    }

    override suspend fun registerStockProduct(garment: GarmentEntity, variants: List<VariantEntity>) = database.withTransaction {
        require(garment.name.isNotBlank() && garment.category.isNotBlank()) { "Completa nombre y categoría." }
        require(garment.basePrice > 0.0) { "El precio debe ser mayor que cero." }
        require(variants.isNotEmpty()) { "Agrega al menos una talla y color." }
        require(variants.all { it.size.isNotBlank() && it.color.isNotBlank() && it.stock >= 0 }) { "Revisa talla, color y cantidad de cada variante." }
        val garmentId = dao.insertGarment(garment)
        variants.forEach { variant ->
            dao.insertVariant(variant.copy(id = 0, garmentId = garmentId, sku = variant.sku.ifBlank { generateSku() }))
        }
    }

    private fun generateSku(): String = "BS-${UUID.randomUUID().toString().replace("-", "").take(12).uppercase()}"

    override suspend fun requestStock(request: StockRequestEntity) {
        require(request.quantity > 0) { "La cantidad solicitada debe ser mayor que cero." }
        require(request.reason.isNotBlank()) { "Indica por qué se necesita la reposición." }
        dao.insertStockRequest(request)
    }

    override suspend fun reviewStockRequest(request: StockRequestEntity) {
        require(request.status == "APPROVED" || request.status == "REJECTED") { "El estado de la solicitud no es válido." }
        dao.updateStockRequest(request.copy(reviewedAt = System.currentTimeMillis()))
    }

    override suspend fun recordStockArrivals(arrivals: List<StockArrivalEntity>) = database.withTransaction {
        require(arrivals.isNotEmpty()) { "Agrega al menos una prenda recibida." }
        require(arrivals.all { it.quantity > 0 }) { "Cada cantidad recibida debe ser mayor que cero." }
        arrivals.groupBy { it.variantId }.forEach { (variantId, entries) ->
            check(dao.increaseStock(variantId, entries.sumOf { it.quantity }) == 1) { "No se encontró una variante para actualizar." }
        }
        dao.insertStockArrivals(arrivals)
    }

    override suspend fun removeVariant(variant: VariantEntity) = dao.deleteVariant(variant)

    override suspend fun saveUser(user: UserEntity, newPassword: String) {
        require(user.username.isNotBlank() && user.name.isNotBlank()) { "Completa el usuario y el nombre." }
        require(user.role == "ADMIN" || user.role == "OPERATOR") { "El rol seleccionado no es válido." }
        val current = if (user.id == 0L) null else dao.findUserById(user.id)
        val passwordHash = if (newPassword.isNotBlank()) hash(newPassword) else current?.passwordHash
        require(passwordHash != null) { "La contraseña es obligatoria para un usuario nuevo." }
        val saved = user.copy(passwordHash = passwordHash)
        if (user.id == 0L) dao.insertUser(saved) else dao.updateUser(saved)
    }

    override suspend fun removeUser(user: UserEntity) {
        if (user.role == "ADMIN") {
            require(dao.otherAdminCount(user.id) > 0) { "Debe permanecer al menos una cuenta ADMIN." }
        }
        dao.deleteUser(user)
    }

    override suspend fun createSale(request: SaleRequest): Long = database.withTransaction {
        require(request.items.isNotEmpty() && request.items.all { it.quantity > 0 && it.unitPrice > 0.0 }) {
            "La venta necesita artículos con cantidad y precio válidos."
        }
        require(request.customerName.isNotBlank()) { "El nombre del cliente es obligatorio." }
        require(com.sanvalero.boutiquestyle.domain.usecase.isValidDominicanId(request.customerId)) {
            "La cédula o el RNC no es válido. Verifica el número y su dígito verificador."
        }
        request.items.groupBy { it.variant.id }.forEach { (id, lines) ->
            val quantity = lines.sumOf { it.quantity }
            check(dao.decrementStock(id, quantity) == 1) { "Inventario insuficiente para una variante de la venta." }
        }
        val totals = calculateSaleTotals(request.items)
        val saleId = dao.insertSale(
            SaleEntity(
                customerName = request.customerName,
                customerCedulaOrRnc = request.customerId.filter(Char::isDigit),
                subtotal = totals.subtotal,
                itbis = totals.itbis,
                total = totals.total,
                paymentMethod = request.paymentMethod,
                userId = request.userId,
                notes = request.notes
            )
        )
        dao.insertSaleItems(request.items.map { line ->
            val lineTotal = java.math.BigDecimal.valueOf(line.unitPrice)
                .multiply(java.math.BigDecimal.valueOf(line.quantity.toLong()))
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .toDouble()
            SaleItemEntity(saleId = saleId, variantId = line.variant.id, quantity = line.quantity, unitPrice = line.unitPrice, lineTotal = lineTotal)
        })
        saleId
    }

    override suspend fun updateSale(sale: SaleEntity) = database.withTransaction {
        require(sale.customerName.isNotBlank()) { "El nombre del cliente es obligatorio." }
        require(com.sanvalero.boutiquestyle.domain.usecase.isValidDominicanId(sale.customerCedulaOrRnc)) {
            "La cédula o el RNC no es válido. Verifica el dígito verificador."
        }
        dao.updateSale(sale.copy(customerCedulaOrRnc = sale.customerCedulaOrRnc.filter(Char::isDigit)))
    }

    override suspend fun removeSale(sale: SaleEntity) = database.withTransaction {
        dao.saleItems(sale.id).forEach { item ->
            dao.restoreStock(item.variantId, item.quantity)
        }
        dao.deleteSale(sale)
    }

    override suspend fun createLayaway(layaway: LayawayEntity): Long = database.withTransaction {
        require(layaway.quantity > 0) { "La cantidad debe ser mayor que cero." }
        require(com.sanvalero.boutiquestyle.domain.usecase.isValidDominicanId(layaway.customerCedula)) {
            "La cédula o el RNC no es válido. Verifica el número y su dígito verificador."
        }
        require(layaway.totalAmount > 0.0) { "El total del apartado debe ser mayor que cero." }
        require(layaway.amountPaid in 0.0..layaway.totalAmount) { "El abono debe estar entre cero y el total." }
        check(dao.decrementStock(layaway.variantId, layaway.quantity) == 1) { "Inventario insuficiente para el apartado." }
        dao.insertLayaway(layaway.copy(balance = layaway.totalAmount - layaway.amountPaid))
    }

    override suspend fun updateLayaway(layaway: LayawayEntity) = database.withTransaction {
        val previous = dao.getLayaway(layaway.id)
        if (previous?.status == "ACTIVE" && layaway.status == "CANCELLED") {
            dao.restoreStock(layaway.variantId, layaway.quantity)
        }
        dao.updateLayaway(layaway.copy(balance = (layaway.totalAmount - layaway.amountPaid).coerceAtLeast(0.0)))
    }

    override suspend fun exportBackup(destination: Uri) {
        val snapshot = database.withTransaction {
            BackupSnapshot(dao.allUsers(), dao.allGarments(), dao.allVariants(), dao.allSales(), dao.allSaleItems(), dao.allLayaways(), dao.allStockRequests(), dao.allStockArrivals())
        }
        val file = File(context.cacheDir, "boutiquestyle-backup.db")
        file.delete()
        val backupDb = Room.databaseBuilder(context, AppDatabase::class.java, file.absolutePath)
            .addMigrations(AppDatabase.MIGRATION_1_2).build()
        try {
            backupDb.withTransaction {
                val backupDao = backupDb.boutiqueDao()
                backupDao.insertUsers(snapshot.users)
                backupDao.insertGarments(snapshot.garments)
                backupDao.insertVariants(snapshot.variants)
                backupDao.insertSales(snapshot.sales)
                backupDao.insertAllSaleItems(snapshot.saleItems)
                backupDao.insertLayaways(snapshot.layaways)
                backupDao.insertStockRequests(snapshot.stockRequests)
                backupDao.insertStockArrivals(snapshot.stockArrivals)
            }
            backupDb.close()
            context.contentResolver.openOutputStream(destination, "w")?.use { output ->
                file.inputStream().use { it.copyTo(output) }
            } ?: error("No se pudo escribir el respaldo seleccionado.")
        } finally {
            backupDb.close()
            file.delete()
            File(file.path + "-wal").delete()
            File(file.path + "-shm").delete()
        }
    }

    override suspend fun importBackup(source: Uri) {
        val file = File(context.cacheDir, "boutiquestyle-restore.db")
        context.contentResolver.openInputStream(source)?.use { input ->
            file.outputStream().use { input.copyTo(it) }
        } ?: error("No se pudo leer el archivo de respaldo.")
        val backupDb = Room.databaseBuilder(context, AppDatabase::class.java, file.absolutePath)
            .addMigrations(AppDatabase.MIGRATION_1_2).build()
        val snapshot = try {
            backupDb.withTransaction {
                val backupDao = backupDb.boutiqueDao()
                BackupSnapshot(backupDao.allUsers(), backupDao.allGarments(), backupDao.allVariants(), backupDao.allSales(), backupDao.allSaleItems(), backupDao.allLayaways(), backupDao.allStockRequests(), backupDao.allStockArrivals())
            }
        } finally {
            backupDb.close()
            File(file.path + "-wal").delete()
            File(file.path + "-shm").delete()
        }
        require(snapshot.users.isNotEmpty()) { "El respaldo no contiene usuarios; no se importó." }
        database.withTransaction {
            database.clearAllTables()
            dao.insertUsers(snapshot.users)
            dao.insertGarments(snapshot.garments)
            dao.insertVariants(snapshot.variants)
            dao.insertSales(snapshot.sales)
            dao.insertAllSaleItems(snapshot.saleItems)
            dao.insertLayaways(snapshot.layaways)
            dao.insertStockRequests(snapshot.stockRequests)
            dao.insertStockArrivals(snapshot.stockArrivals)
        }
        file.delete()
    }

    private fun hash(password: String): String {
        val salt = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
        val spec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return "pbkdf2\$120000\$${salt.toHex()}\$${key.toHex()}"
    }

    private data class BackupSnapshot(
        val users: List<UserEntity>,
        val garments: List<GarmentEntity>,
        val variants: List<VariantEntity>,
        val sales: List<SaleEntity>,
        val saleItems: List<SaleItemEntity>,
        val layaways: List<LayawayEntity>,
        val stockRequests: List<StockRequestEntity>,
        val stockArrivals: List<StockArrivalEntity>
    )

    private fun verify(password: String, encoded: String): Boolean {
        val parts = encoded.split('$')
        if (parts.size != 4 || parts[0] != "pbkdf2") return false
        val spec = PBEKeySpec(password.toCharArray(), parts[2].hexToBytes(), parts[1].toInt(), 256)
        val actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return java.security.MessageDigest.isEqual(actual, parts[3].hexToBytes())
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
    private fun String.hexToBytes() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

private suspend fun BoutiqueDao.observeGarmentsSnapshotIsEmpty(): Boolean =
    observeGarments().first().isEmpty()
