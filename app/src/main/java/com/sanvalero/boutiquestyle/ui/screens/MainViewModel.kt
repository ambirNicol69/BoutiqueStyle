package com.sanvalero.boutiquestyle.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanvalero.boutiquestyle.data.local.entity.*
import com.sanvalero.boutiquestyle.domain.model.*
import com.sanvalero.boutiquestyle.domain.repository.StoreRepository
import com.sanvalero.boutiquestyle.domain.usecase.CreateSaleUseCase
import com.sanvalero.boutiquestyle.domain.usecase.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: StoreRepository,
    private val loginUseCase: LoginUseCase,
    private val createSaleUseCase: CreateSaleUseCase
) : ViewModel() {
    init {
        launchAction { repository.initialize() }
    }

    private val storeData = combine(
        repository.garments(),
        repository.variants(),
        repository.sales(),
        repository.layaways(),
        repository.users()
    ) { garments, variants, sales, layaways, users ->
        StoreUiState(garments = garments, variants = variants, sales = sales, layaways = layaways, users = users)
    }

    val state: StateFlow<StoreUiState> = combine(
        storeData,
        repository.saleItems(),
        repository.stockRequests(),
        repository.stockArrivals(),
        repository.dashboard()
    ) { data, saleItems, stockRequests, stockArrivals, dashboard ->
        data.copy(saleItems = saleItems, stockRequests = stockRequests, stockArrivals = stockArrivals, dashboard = dashboard)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StoreUiState())

    private val _user = MutableStateFlow<UserEntity?>(null)
    val user: StateFlow<UserEntity?> = _user.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun login(username: String, password: String) = launchAction {
        val authenticated = loginUseCase(username, password) ?: error("Usuario o contraseña incorrectos.")
        require(authenticated.role == "ADMIN" || authenticated.role == "OPERATOR") { "Esta cuenta es de cliente; entra desde la tienda." }
        _user.value = authenticated
    }

    fun loginCustomer(username: String, password: String) = launchAction {
        val authenticated = loginUseCase(username, password) ?: error("Usuario o contraseña incorrectos.")
        require(authenticated.role == "CUSTOMER") { "Usa las credenciales de una cuenta de cliente." }
        _user.value = authenticated
    }

    fun registerCustomer(username: String, password: String, name: String) = launchAction {
        _user.value = repository.registerCustomer(username, password, name)
    }
    fun logout() { _user.value = null }

    fun saveGarment(garment: GarmentEntity) = launchAction {
        require(garment.name.isNotBlank() && garment.category.isNotBlank()) { "Completa nombre y categoría." }
        require(garment.basePrice > 0) { "El precio debe ser mayor que cero." }
        repository.saveGarment(garment)
        _message.value = "Prenda guardada."
    }

    fun deleteGarment(garment: GarmentEntity) = launchAction { repository.removeGarment(garment) }

    fun saveVariant(variant: VariantEntity) = launchAction {
        require(variant.size.isNotBlank() && variant.color.isNotBlank() && variant.sku.isNotBlank()) { "Completa talla, color y SKU." }
        repository.saveVariant(variant)
        _message.value = "Variante guardada."
    }

    fun registerStockProduct(garment: GarmentEntity, variants: List<VariantEntity>) = launchAction {
        require(user.value?.role == "OPERATOR") { "Esta acción está disponible para el empleado." }
        repository.registerStockProduct(garment, variants)
        _message.value = "Prenda y variantes registradas en el inventario."
    }

    fun createStockRequest(variant: VariantEntity, quantity: Int, reason: String) = launchAction {
        val employee = user.value?.takeIf { it.role == "OPERATOR" } ?: error("Inicia sesión como empleado.")
        repository.requestStock(StockRequestEntity(variantId = variant.id, quantity = quantity, reason = reason.trim(), employeeUserId = employee.id))
        _message.value = "Solicitud enviada a administración."
    }

    fun reviewStockRequest(request: StockRequestEntity, approve: Boolean, note: String = "") = launchAction {
        require(user.value?.role == "ADMIN") { "Solo ADMIN puede revisar solicitudes." }
        repository.reviewStockRequest(request.copy(status = if (approve) "APPROVED" else "REJECTED", adminNote = note.trim().ifBlank { null }))
        _message.value = "Solicitud ${if (approve) "aprobada" else "rechazada"}."
    }

    fun recordStockArrivals(variantQuantities: List<Pair<VariantEntity, Int>>, date: Long) = launchAction {
        val employee = user.value?.takeIf { it.role == "OPERATOR" } ?: error("Inicia sesión como empleado.")
        repository.recordStockArrivals(variantQuantities.map { (variant, quantity) ->
            StockArrivalEntity(variantId = variant.id, quantity = quantity, arrivalDate = date, employeeUserId = employee.id)
        })
        _message.value = "Llegada registrada y stock actualizado."
    }

    fun deleteVariant(variant: VariantEntity) = launchAction { repository.removeVariant(variant) }

    fun saveUser(user: UserEntity, password: String) = launchAction {
        val current = this.user.value ?: error("Inicia sesión nuevamente.")
        require(current.role == "ADMIN") { "Solo ADMIN puede administrar usuarios." }
        require(user.id != current.id || user.role == "ADMIN") { "No puedes quitarte tu propio rol ADMIN." }
        repository.saveUser(user, password)
        _message.value = "Usuario guardado."
    }

    fun deleteUser(user: UserEntity) = launchAction {
        val current = this.user.value ?: error("Inicia sesión nuevamente.")
        require(current.role == "ADMIN") { "Solo ADMIN puede administrar usuarios." }
        require(user.id != current.id) { "No puedes eliminar tu propia sesión." }
        repository.removeUser(user)
        _message.value = "Usuario eliminado."
    }

    fun createSale(items: List<CartLine>, customer: String, customerId: String, payment: String, onSuccess: () -> Unit = {}) = launchAction {
        createSaleUseCase(
            SaleRequest(
                customerName = customer,
                customerId = customerId,
                paymentMethod = payment,
                userId = user.value?.id
                    ?: state.value.users.firstOrNull { it.role == "ADMIN" }?.id
                    ?: error("No se encontró una cuenta administradora para registrar la venta."),
                items = items
            )
        )
        _message.value = "Venta registrada con ITBIS incluido."
        onSuccess()
    }

    fun updateSale(sale: SaleEntity, customer: String, customerId: String, payment: String) = launchAction {
        require(com.sanvalero.boutiquestyle.domain.usecase.isValidDominicanId(customerId)) { "La cédula debe tener 11 dígitos o el RNC 9, con su dígito verificador correcto." }
        repository.updateSale(sale.copy(customerName = customer.trim(), customerCedulaOrRnc = customerId, paymentMethod = payment))
        _message.value = "Venta actualizada."
    }

    fun deleteSale(sale: SaleEntity) = launchAction {
        repository.removeSale(sale)
        _message.value = "Venta eliminada y existencias repuestas."
    }

    fun createLayaway(variant: VariantEntity, quantity: Int, customer: String, phone: String, cedula: String, paid: Double) = launchAction {
        val garment = state.value.garments.firstOrNull { it.id == variant.garmentId } ?: error("No se encontró la prenda.")
        val total = garment.basePrice * quantity
        require(customer.isNotBlank()) { "El nombre del cliente es obligatorio." }
        require(phone.isNotBlank()) { "El teléfono es obligatorio." }
        require(quantity > 0 && paid in 0.0..total) { "Revisa la cantidad y el abono." }
        require(com.sanvalero.boutiquestyle.domain.usecase.isValidDominicanId(cedula)) { "La cédula debe tener 11 dígitos o el RNC 9, con su dígito verificador correcto." }
        val due = LocalDate.now().plusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        repository.createLayaway(
            LayawayEntity(
                customerName = customer,
                customerPhone = phone,
                customerCedula = cedula.filter(Char::isDigit),
                variantId = variant.id,
                quantity = quantity,
                totalAmount = total,
                amountPaid = paid,
                balance = total - paid,
                dueDate = due,
                userId = user.value?.id ?: error("Inicia sesión nuevamente.")
            )
        )
        _message.value = "Apartado creado; el inventario fue reservado."
    }

    fun completeLayaway(layaway: LayawayEntity) = launchAction {
        repository.updateLayaway(layaway.copy(status = "COMPLETED", amountPaid = layaway.totalAmount, balance = 0.0))
    }

    fun cancelLayaway(layaway: LayawayEntity) = launchAction {
        repository.updateLayaway(layaway.copy(status = "CANCELLED"))
    }

    fun clearMessage() { _message.value = null }

    fun exportBackup(destination: Uri) = launchAction {
        repository.exportBackup(destination)
        _message.value = "Respaldo SQLite guardado."
    }

    fun importBackup(source: Uri) = launchAction {
        repository.importBackup(source)
        _message.value = "Respaldo restaurado correctamente."
    }

    private fun launchAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _message.value = exception.message ?: "No se pudo completar la operación."
            }
        }
    }
}
