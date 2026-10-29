package com.sanvalero.boutiquestyle.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueryBuilder
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sanvalero.boutiquestyle.data.local.entity.GarmentEntity
import com.sanvalero.boutiquestyle.data.local.entity.StockRequestEntity
import com.sanvalero.boutiquestyle.data.local.entity.VariantEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class EmployeeTab(val title: String) { HOME("Inicio"), STOCK("Stock"), REQUESTS("Solicitudes"), REPORTS("Reportes"), PROFILE("Perfil") }
private data class VariantDraft(val size: String = "", val color: String = "", val quantity: String = "0")

@Composable
fun EmployeeScreen(viewModel: MainViewModel, employeeName: String, employeeId: Long, onLogout: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(EmployeeTab.HOME) }
    var showNewProduct by remember { mutableStateOf(false) }
    var showRequest by remember { mutableStateOf(false) }
    var showArrival by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var lowStockOnly by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("BoutiqueStyle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Empleado · $employeeName", style = MaterialTheme.typography.bodySmall) }
                IconButton(onClick = onLogout) { Icon(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión") }
            }
        },
        bottomBar = {
            NavigationBar {
                EmployeeTab.entries.forEach { item ->
                    NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = {
                        Icon(when (item) {
                            EmployeeTab.HOME -> Icons.Default.Home
                            EmployeeTab.STOCK -> Icons.Default.Inventory2
                            EmployeeTab.REQUESTS -> Icons.Default.Assessment
                            EmployeeTab.REPORTS -> Icons.Default.Storefront
                            EmployeeTab.PROFILE -> Icons.Default.Person
                        }, item.title)
                    }, label = { Text(item.title) })
                }
            }
        },
        snackbarHost = { BoutiqueSnackbar(message, viewModel::clearMessage) }
    ) { padding ->
        when (tab) {
            EmployeeTab.HOME -> EmployeeHome(
                employeeName = employeeName,
                pendingRequests = state.stockRequests.count { it.employeeUserId == employeeId && it.status == "PENDING" },
                onNewStock = { showNewProduct = true },
                onRequest = { tab = EmployeeTab.REQUESTS; showRequest = true },
                onArrival = { tab = EmployeeTab.REPORTS; showArrival = true },
                onInventory = { tab = EmployeeTab.STOCK }, modifier = Modifier.padding(padding)
            )
            EmployeeTab.STOCK -> EmployeeInventory(state.garments, state.variants, search, { search = it }, lowStockOnly, { lowStockOnly = it }, Modifier.padding(padding))
            EmployeeTab.REQUESTS -> EmployeeRequests(state, employeeId, onCreate = { showRequest = true }, modifier = Modifier.padding(padding))
            EmployeeTab.REPORTS -> ArrivalHistory(state, onRegister = { showArrival = true }, modifier = Modifier.padding(padding))
            EmployeeTab.PROFILE -> EmployeeProfile(employeeName, onLogout, Modifier.padding(padding))
        }
    }
    if (showNewProduct) NewProductDialog(
        onDismiss = { showNewProduct = false },
        onSave = { garment, variants -> viewModel.registerStockProduct(garment, variants); showNewProduct = false }
    )
    if (showRequest) RequestStockDialog(state.garments, state.variants, onDismiss = { showRequest = false }, onSubmit = { variant, quantity, reason -> viewModel.createStockRequest(variant, quantity, reason); showRequest = false })
    if (showArrival) ArrivalDialog(state.garments, state.variants, onDismiss = { showArrival = false }, onSubmit = { lines, date -> viewModel.recordStockArrivals(lines, date); showArrival = false })
}

@Composable
private fun EmployeeHome(employeeName: String, pendingRequests: Int, onNewStock: () -> Unit, onRequest: () -> Unit, onArrival: () -> Unit, onInventory: () -> Unit, modifier: Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF4C9D6)), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("EMPLEADO", style = MaterialTheme.typography.labelLarge, color = Color(0xFF9D3F5B), fontWeight = FontWeight.Bold)
                    Text("Hola, $employeeName 👋", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Todo en orden para hoy ♥", color = Color(0xFF3E2723))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionTile("Registrar nuevo stock", "Agrega prendas y variantes", Icons.Default.Add, onNewStock, Modifier.weight(1f), primary = true)
                ActionTile("Solicitar reposición", "$pendingRequests pendientes", Icons.Default.Inventory2, onRequest, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionTile("Registrar llegada", "Añade lo recibido al stock", Icons.Default.QueryBuilder, onArrival, Modifier.weight(1f))
                ActionTile("Ver inventario", "Consulta tallas y colores", Icons.Default.Assessment, onInventory, Modifier.weight(1f))
            }
        }
        item { Text("Tu jornada", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item { Text("Registra las prendas recibidas para mantener las existencias al día. Las solicitudes quedan disponibles para administración.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun ActionTile(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier, primary: Boolean = false) {
    Card(onClick = onClick, modifier.height(154.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = if (primary) Color(0xFF9D3F5B) else Color(0xFFF9E5EA))) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, null, tint = if (primary) Color.White else Color(0xFF3E2723), modifier = Modifier.size(28.dp))
            Column { Text(title, fontWeight = FontWeight.Bold, color = if (primary) Color.White else Color(0xFF3E2723)); Text(subtitle, style = MaterialTheme.typography.labelSmall, color = if (primary) Color.White else Color(0xFF3E2723)) }
        }
    }
}

@Composable
private fun EmployeeInventory(garments: List<GarmentEntity>, variants: List<VariantEntity>, search: String, onSearch: (String) -> Unit, lowStockOnly: Boolean, onLowStock: (Boolean) -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("Inventario", Modifier.padding(top = 16.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(search, onSearch, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Buscar prenda, marca o SKU") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            FilterChip(!lowStockOnly, { onLowStock(false) }, label = { Text("Todos") })
            FilterChip(lowStockOnly, { onLowStock(true) }, label = { Text("Bajo stock") })
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            items(garments.filter { garment ->
                (garment.name.contains(search, true) || garment.brand.contains(search, true) || variants.filter { it.garmentId == garment.id }.any { it.sku.contains(search, true) }) &&
                    variants.any { it.garmentId == garment.id && it.stock > 0 } && (!lowStockOnly || variants.filter { it.garmentId == garment.id }.any { it.stock <= 3 })
            }, key = { it.id }) { garment ->
                val productVariants = variants.filter { it.garmentId == garment.id }
                Card {
                    Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(garment.brand.ifBlank { garment.category }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(garment.name, fontWeight = FontWeight.Bold)
                        Text("Stock total: ${productVariants.sumOf { it.stock }}", color = Color(0xFF9D3F5B), fontWeight = FontWeight.SemiBold)
                        productVariants.forEach { variant ->
                            Text("${variant.size} · ${variant.color} · ${variant.stock} unidades · ${variant.sku}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeRequests(state: com.sanvalero.boutiquestyle.domain.model.StoreUiState, employeeId: Long, onCreate: () -> Unit, modifier: Modifier) {
    var filter by remember { mutableStateOf("Todos") }
    val requests = state.stockRequests.filter { request ->
        request.employeeUserId == employeeId && when (filter) {
            "Pendientes" -> request.status == "PENDING"
            "Aprobadas" -> request.status == "APPROVED"
            "Rechazadas" -> request.status == "REJECTED"
            else -> true
        }
    }
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Mis solicitudes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            FilledTonalButton(onClick = onCreate) { Icon(Icons.Default.Add, null); Text("Nueva") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("Todos", "Pendientes", "Aprobadas", "Rechazadas").forEach { value ->
                FilterChip(selected = filter == value, onClick = { filter = value }, label = { Text(value) })
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(requests, key = { it.id }) { request ->
                val variant = state.variants.firstOrNull { it.id == request.variantId }
                val garment = state.garments.firstOrNull { it.id == variant?.garmentId }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(garment?.name ?: "Prenda", fontWeight = FontWeight.Bold)
                        Text("Talla ${variant?.size.orEmpty()} · Color ${variant?.color.orEmpty()} · Cantidad ${request.quantity}")
                        Text(request.reason, style = MaterialTheme.typography.bodySmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusPill(request.status)
                            Spacer(Modifier.width(8.dp))
                            Text(java.time.Instant.ofEpochMilli(request.createdAt).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), style = MaterialTheme.typography.labelSmall)
                        }
                        request.adminNote?.takeIf { it.isNotBlank() }?.let { Text("Nota: $it", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
            if (requests.isEmpty()) item { Text(if (state.stockRequests.none { it.employeeUserId == employeeId }) "Aún no has enviado solicitudes." else "No hay solicitudes en este filtro.", Modifier.padding(14.dp)) }
        }
    }
}

@Composable
private fun ArrivalHistory(state: com.sanvalero.boutiquestyle.domain.model.StoreUiState, onRegister: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Reportes de inventario", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            FilledTonalButton(onClick = onRegister) { Icon(Icons.Default.Add, null); Text("Registrar") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 12.dp)) {
            EmployeeMetric("Llegadas", state.stockArrivals.size.toString(), Modifier.weight(1f))
            EmployeeMetric("Unidades recibidas", state.stockArrivals.sumOf { it.quantity }.toString(), Modifier.weight(1f))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            item { Text("Historial de llegadas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(state.stockArrivals, key = { it.id }) { arrival ->
                val variant = state.variants.firstOrNull { it.id == arrival.variantId }
                val garment = state.garments.firstOrNull { it.id == variant?.garmentId }
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(garment?.name ?: "Prenda", fontWeight = FontWeight.Bold)
                            Text("Talla ${variant?.size.orEmpty()} · Color ${variant?.color.orEmpty()}")
                            Text("${arrival.quantity} unidades recibidas", color = Color(0xFF9D3F5B), fontWeight = FontWeight.SemiBold)
                        }
            Text(arrival.arrivalDate.toLocalDateTimeLabel(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (state.stockArrivals.isEmpty()) item { Text("Todavía no hay llegadas registradas.", Modifier.padding(14.dp)) }
        }
    }
}

@Composable
private fun EmployeeMetric(title: String, value: String, modifier: Modifier) {
    Card(modifier) { Column(Modifier.padding(13.dp)) { Text(title, style = MaterialTheme.typography.labelMedium); Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF9D3F5B)) } }
}

@Composable
private fun EmployeeProfile(name: String, onLogout: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("EMPLEADO", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onLogout, modifier = Modifier.padding(top = 18.dp)) { Icon(Icons.AutoMirrored.Filled.Logout, null); Text("Cerrar sesión") }
    }
}

@Composable
private fun StatusPill(status: String) {
    val (label, color) = when (status) {
        "APPROVED" -> "Aprobada" to Color(0xFF2E7D54)
        "REJECTED" -> "Rechazada" to Color(0xFFB23B4B)
        else -> "Pendiente" to Color(0xFFBD741D)
    }
    Surface(color = color.copy(alpha = .13f), shape = RoundedCornerShape(20.dp)) { Text(label, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
}

@Composable
private fun NewProductDialog(onDismiss: () -> Unit, onSave: (GarmentEntity, List<VariantEntity>) -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<String?>(null) }
    var variants by remember { mutableStateOf(listOf(VariantDraft(size = "M", color = "", quantity = "1"))) }
    var error by remember { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            imageUri = uri.toString()
        }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Registrar nuevo stock") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Información de la prenda · Tallas y colores", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(name, { name = it }, label = { Text("Nombre de la prenda") }, singleLine = true)
                OutlinedTextField(brand, { brand = it }, label = { Text("Marca") }, singleLine = true)
                OutlinedTextField(category, { category = it }, label = { Text("Categoría") }, singleLine = true)
                OutlinedTextField(price, { price = it }, label = { Text("Precio RD$") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedButton(onClick = { photoPicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (imageUri == null) "Agregar foto de la prenda" else "Cambiar foto de la prenda")
                }
                imageUri?.let { selectedImage ->
                    ProductPhoto(selectedImage, category.ifBlank { "Prenda" }, Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(12.dp)))
                }
                Text("Variantes", fontWeight = FontWeight.Bold)
                variants.forEachIndexed { index, row ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F8))) {
                        Column(Modifier.fillMaxWidth().padding(9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Variante ${index + 1}", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                if (variants.size > 1) TextButton(onClick = { variants = variants.filterIndexed { i, _ -> i != index } }) { Text("Quitar") }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(row.size, { value -> variants = variants.mapIndexed { i, old -> if (i == index) old.copy(size = value) else old } }, Modifier.weight(1f), label = { Text("Talla") }, singleLine = true)
                                OutlinedTextField(row.color, { value -> variants = variants.mapIndexed { i, old -> if (i == index) old.copy(color = value) else old } }, Modifier.weight(1f), label = { Text("Color") }, singleLine = true)
                            }
                            OutlinedTextField(row.quantity, { value -> variants = variants.mapIndexed { i, old -> if (i == index) old.copy(quantity = value) else old } }, label = { Text("Cantidad") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                    }
                }
                TextButton(onClick = { variants = variants + VariantDraft() }) { Icon(Icons.Default.Add, null); Text("Agregar talla / color") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = {
            val parsedPrice = price.toDoubleOrNull()
            val validVariants = variants.all { it.size.isNotBlank() && it.color.isNotBlank() && (it.quantity.toIntOrNull() ?: 0) >= 0 }
            if (name.isBlank() || category.isBlank() || parsedPrice == null || parsedPrice <= 0 || !validVariants || imageUri == null) error = "Completa los datos, agrega una foto y revisa cada talla, color y cantidad."
            else {
                val garment = GarmentEntity(name = name.trim(), brand = brand.trim(), category = category.trim(), basePrice = parsedPrice, imageUri = imageUri)
                val stockVariants = variants.map { row ->
                    VariantEntity(garmentId = 0, size = row.size.trim(), color = row.color.trim(), stock = row.quantity.toInt(), sku = "")
                }
                onSave(garment, stockVariants)
            }
        }) { Text("Registrar en inventario") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun RequestStockDialog(garments: List<GarmentEntity>, variants: List<VariantEntity>, onDismiss: () -> Unit, onSubmit: (VariantEntity, Int, String) -> Unit) {
    var selected by remember { mutableStateOf<VariantEntity?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var quantity by remember { mutableStateOf("1") }
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Solicitar reposición") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Elige la variante y la cantidad que necesita reposición.")
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selected?.let { variantText(it, garments) } ?: "Seleccionar prenda") }
                DropdownMenu(expanded, { expanded = false }) {
                    variants.forEach { variant -> DropdownMenuItem(text = { Text(variantText(variant, garments)) }, onClick = { selected = variant; expanded = false }) }
                }
            }
            OutlinedTextField(quantity, { quantity = it }, label = { Text("Cantidad solicitada") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            OutlinedTextField(reason, { reason = it.take(200) }, label = { Text("Motivo") }, minLines = 2, supportingText = { Text("${reason.length}/200") })
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(onClick = {
        val amount = quantity.toIntOrNull() ?: 0
        if (selected == null || amount <= 0 || reason.isBlank()) error = "Selecciona una variante e indica cantidad y motivo."
        else onSubmit(selected!!, amount, reason)
    }) { Icon(Icons.AutoMirrored.Filled.Send, null); Text("Enviar solicitud") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

private fun variantText(variant: VariantEntity, garments: List<GarmentEntity>): String {
    val garment = garments.firstOrNull { it.id == variant.garmentId }?.name ?: "Prenda"
    return "$garment · ${variant.size} · ${variant.color} · stock ${variant.stock}"
}

private data class ArrivalDraft(val variant: VariantEntity, val quantity: Int)

@Composable
private fun ArrivalDialog(garments: List<GarmentEntity>, variants: List<VariantEntity>, onDismiss: () -> Unit, onSubmit: (List<Pair<VariantEntity, Int>>, Long) -> Unit) {
    var date by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) }
    var time by remember { mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))) }
    var selected by remember { mutableStateOf<VariantEntity?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var quantity by remember { mutableStateOf("1") }
    var arrivals by remember { mutableStateOf(emptyList<ArrivalDraft>()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Registrar hora de llegada") }, text = {
        Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Registra las prendas recibidas. El stock se actualizará al guardar.")
            OutlinedTextField(date, { date = it }, label = { Text("Fecha de llegada · dd/MM/yyyy") }, singleLine = true)
            OutlinedTextField(time, { time = it }, label = { Text("Hora de llegada · HH:mm") }, singleLine = true)
            Text("Prendas recibidas", fontWeight = FontWeight.Bold)
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selected?.let { variantText(it, garments) } ?: "Seleccionar variante") }
                DropdownMenu(expanded, { expanded = false }) {
                    variants.forEach { variant -> DropdownMenuItem(text = { Text(variantText(variant, garments)) }, onClick = { selected = variant; expanded = false }) }
                }
            }
            OutlinedTextField(quantity, { quantity = it }, label = { Text("Cantidad recibida") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            FilledTonalButton(onClick = {
                val amount = quantity.toIntOrNull() ?: 0
                if (selected == null || amount <= 0) error = "Selecciona una variante y una cantidad válida."
                else {
                    arrivals = arrivals + ArrivalDraft(selected!!, amount)
                    selected = null
                    quantity = "1"
                    error = null
                }
            }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Text("Agregar otra prenda") }
            arrivals.forEachIndexed { index, item ->
                val name = variantText(item.variant, garments)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$name · ${item.quantity}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { arrivals = arrivals.filterIndexed { i, _ -> i != index } }) { Text("Quitar") }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(onClick = {
        val parsedDateTime = runCatching {
            LocalDateTime.of(LocalDate.parse(date, DateTimeFormatter.ofPattern("dd/MM/yyyy")), LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm")))
        }.getOrNull()
        if (arrivals.isEmpty() || parsedDateTime == null) error = "Agrega prendas y revisa la fecha y hora."
        else {
            val timestamp = parsedDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            onSubmit(arrivals.map { it.variant to it.quantity }, timestamp)
        }
    }) { Text("Registrar llegada") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

private fun Long.toLocalDateTimeLabel(): String = java.time.Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
