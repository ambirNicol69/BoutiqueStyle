package com.sanvalero.boutiquestyle.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sanvalero.boutiquestyle.R
import com.sanvalero.boutiquestyle.data.local.entity.*
import com.sanvalero.boutiquestyle.domain.model.CartLine
import com.sanvalero.boutiquestyle.domain.model.StoreUiState
import com.sanvalero.boutiquestyle.report.ReportExporter
import com.sanvalero.boutiquestyle.util.formatCurrency
import com.sanvalero.boutiquestyle.util.formatDate
import java.time.LocalDate

@Composable
fun LoginScreen(onLogin: (String, String) -> Unit, onBrowseStore: () -> Unit = {}, viewModel: MainViewModel = hiltViewModel()) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val message by viewModel.message.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Card(Modifier.fillMaxWidth().padding(24.dp), shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Image(painterResource(R.drawable.logo_boutiquestyle), "Logo de BoutiqueStyle", Modifier.size(150.dp))
                Text("Bienvenida", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Inicia sesión para administrar tu boutique", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(username, { username = it }, label = { Text("Usuario") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(password, { password = it }, label = { Text("Contraseña") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                if (message != null) Text(message.orEmpty(), color = MaterialTheme.colorScheme.error)
                Button(onClick = { onLogin(username, password) }, modifier = Modifier.fillMaxWidth()) { Text("Entrar") }
                TextButton(onClick = onBrowseStore) { Text("Explorar tienda como cliente") }
                Text("Acceso inicial: admin / admin123", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun BoutiqueSnackbar(message: String?, clear: () -> Unit) {
    if (message != null) {
        Snackbar(
            modifier = Modifier.padding(12.dp),
            action = { TextButton(onClick = clear) { Text("Cerrar") } }
        ) { Text(message) }
    }
}

@Composable
fun DashboardScreen(viewModel: MainViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentPadding = PaddingValues(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("BoutiqueStyle", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Tu resumen de hoy · ${formatDate(System.currentTimeMillis())}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Ventas hoy", state.dashboard.daySales.toString(), Modifier.weight(1f))
                    MetricCard("Ventas del mes", state.dashboard.monthSales.toString(), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Apartados activos", state.dashboard.activeLayaways.toString(), Modifier.weight(1f))
                    MetricCard("Stock bajo (≤ 3)", state.dashboard.lowStock.toString(), Modifier.weight(1f))
                }
            }
        }
        item { MetricCard("Ingresos del mes", formatCurrency(state.dashboard.monthIncome), Modifier.fillMaxWidth()) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Ventas recientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Últimos 7 días", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SalesChart(state.sales)
                }
            }
        }
        item { Text("Inventario por revisar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        items(state.variants.filter { it.stock <= 3 }) { variant ->
            val garment = state.garments.firstOrNull { it.id == variant.garmentId }
            ListCard(title = garment?.name ?: "Prenda", subtitle = "${variant.size} · ${variant.color} · SKU ${variant.sku}", trailing = "Stock ${variant.stock}")
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SalesChart(sales: List<SaleEntity>) {
    val days = remember(sales) {
        (6 downTo 0).map { offset ->
            val date = LocalDate.now().minusDays(offset.toLong())
            date to sales.filter {
                java.time.Instant.ofEpochMilli(it.saleDate).atZone(java.time.ZoneId.systemDefault()).toLocalDate() == date
            }.size
        }
    }
    Canvas(Modifier.fillMaxWidth().height(142.dp).padding(top = 18.dp)) {
        val max = (days.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
        val step = size.width / 7f
        val barWidth = step * 0.44f
        days.forEachIndexed { index, entry ->
            val height = size.height * entry.second / max.toFloat()
            drawRoundRect(
                color = if (index == 6) Color(0xFF3E2723) else Color(0xFFF4C9D6),
                topLeft = androidx.compose.ui.geometry.Offset(index * step + (step - barWidth) / 2, size.height - height),
                size = androidx.compose.ui.geometry.Size(barWidth, height.coerceAtLeast(4.dp.toPx())),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
            )
        }
    }
}

@Composable
fun InventoryScreen(viewModel: MainViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var garmentEditor by remember { mutableStateOf<GarmentEntity?>(null) }
    var addingGarment by remember { mutableStateOf(false) }
    var variantFor by remember { mutableStateOf<GarmentEntity?>(null) }
    var variantEditor by remember { mutableStateOf<VariantEntity?>(null) }
    var query by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("Prendas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("${state.garments.size} estilos en catálogo") }
                FilledTonalButton(onClick = { garmentEditor = null; addingGarment = true }) { Icon(Icons.Default.Add, null); Text("Nueva") }
            }
        }
        item { InputField(query, { query = it }, "Buscar prenda o SKU") }
        items(
            state.garments.filter { garment ->
                garment.name.contains(query, true) ||
                    state.variants.any { it.garmentId == garment.id && it.sku.contains(query, true) }
            },
            key = { it.id }
        ) { garment ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(garment.name, fontWeight = FontWeight.SemiBold)
                            Text("${garment.category} · ${formatCurrency(garment.basePrice)}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { garmentEditor = garment; addingGarment = true }) { Icon(Icons.Default.Edit, "Editar") }
                        IconButton(onClick = { viewModel.deleteGarment(garment) }) { Icon(Icons.Default.Delete, "Eliminar") }
                    }
                    val variants = state.variants.filter { it.garmentId == garment.id }
                    variants.forEach { variant ->
                        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp)).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${variant.size} · ${variant.color} · Stock ${variant.stock} · ${variant.sku}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = { variantEditor = variant; variantFor = garment }) { Icon(Icons.Default.Edit, "Editar variante") }
                            IconButton(onClick = { viewModel.deleteVariant(variant) }) { Icon(Icons.Default.Delete, "Eliminar variante") }
                        }
                    }
                    TextButton(onClick = { variantEditor = null; variantFor = garment }) { Icon(Icons.Default.Add, null); Text("Añadir talla / color") }
                }
            }
        }
    }
    if (addingGarment) GarmentDialog(garmentEditor, onDismiss = { addingGarment = false }, onSave = { viewModel.saveGarment(it); addingGarment = false })
    if (variantFor != null) {
        VariantDialog(variantFor!!, variantEditor, onDismiss = { variantFor = null }, onSave = {
            viewModel.saveVariant(it)
            variantFor = null
        })
    }
}

@Composable
private fun GarmentDialog(initial: GarmentEntity?, onDismiss: () -> Unit, onSave: (GarmentEntity) -> Unit) {
    val context = LocalContext.current
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var category by remember(initial) { mutableStateOf(initial?.category.orEmpty()) }
    var price by remember(initial) { mutableStateOf(initial?.basePrice?.toString().orEmpty()) }
    var brand by remember(initial) { mutableStateOf(initial?.brand.orEmpty()) }
    var description by remember(initial) { mutableStateOf(initial?.description.orEmpty()) }
    var imageUri by remember(initial) { mutableStateOf(initial?.imageUri) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            imageUri = uri.toString()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nueva prenda" else "Editar prenda") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InputField(name, { name = it }, "Nombre")
                InputField(category, { category = it }, "Categoría")
                InputField(price, { price = it }, "Precio RD$", KeyboardType.Decimal)
                InputField(brand, { brand = it }, "Marca")
                OutlinedButton(onClick = { photoPicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (imageUri == null) "Añadir foto de la prenda" else "Cambiar foto de la prenda")
                }
                if (imageUri != null) Box(Modifier.size(88.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer)) {
                    ProductPhoto(imageUri, category.ifBlank { "Prenda" }, Modifier.fillMaxSize())
                }
                if (imageUri != null) TextButton(onClick = { imageUri = null }) { Text("Quitar foto") }
                InputField(description, { description = it }, "Descripción")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(GarmentEntity(id = initial?.id ?: 0, name = name, category = category, basePrice = price.toDoubleOrNull() ?: 0.0, brand = brand, description = description, imageUri = imageUri, createdAt = initial?.createdAt ?: System.currentTimeMillis()))
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun VariantDialog(garment: GarmentEntity, initial: VariantEntity?, onDismiss: () -> Unit, onSave: (VariantEntity) -> Unit) {
    var size by remember(initial) { mutableStateOf(initial?.size ?: "M") }
    var color by remember(initial) { mutableStateOf(initial?.color.orEmpty()) }
    var stock by remember(initial) { mutableStateOf(initial?.stock?.toString() ?: "0") }
    var sku by remember(initial) { mutableStateOf(initial?.sku.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${if (initial == null) "Nueva variante" else "Editar variante"} · ${garment.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InputField(size, { size = it }, "Talla")
                InputField(color, { color = it }, "Color")
                InputField(stock, { stock = it }, "Existencias", KeyboardType.Number)
                InputField(sku, { sku = it }, "SKU")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(VariantEntity(id = initial?.id ?: 0, garmentId = garment.id, size = size, color = color, stock = stock.toIntOrNull() ?: 0, sku = sku, barcode = initial?.barcode))
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun SalesScreen(viewModel: MainViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showSale by remember { mutableStateOf(false) }
    var saleToEdit by remember { mutableStateOf<SaleEntity?>(null) }
    var saleToDelete by remember { mutableStateOf<SaleEntity?>(null) }
    var query by remember { mutableStateOf("") }
    var fromDate by remember { mutableStateOf("") }
    var throughDate by remember { mutableStateOf("") }
    var invoice by remember { mutableStateOf<SaleInvoice?>(null) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Ventas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            FilledTonalButton(onClick = { showSale = true }) { Icon(Icons.Default.Add, null); Text("Registrar") }
        }
        InputField(query, { query = it }, "Buscar cliente o cédula")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InputField(fromDate, { fromDate = it }, "Desde dd/MM/yyyy", modifier = Modifier.weight(1f))
            InputField(throughDate, { throughDate = it }, "Hasta dd/MM/yyyy", modifier = Modifier.weight(1f))
        }
        LazyColumn(contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.sales.filter {
                (it.customerName.contains(query, true) || it.customerCedulaOrRnc.contains(query)) &&
                    it.saleDate.inDateRange(fromDate, throughDate)
            }) { sale ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                            Text(sale.customerName, fontWeight = FontWeight.SemiBold)
                            Text("${formatDate(sale.saleDate)} · ${sale.paymentMethod} · ITBIS ${formatCurrency(sale.itbis)}", style = MaterialTheme.typography.bodySmall)
                            Text(formatCurrency(sale.total), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { saleToEdit = sale }) { Icon(Icons.Default.Edit, "Editar venta") }
                        IconButton(onClick = { saleToDelete = sale }) { Icon(Icons.Default.Delete, "Eliminar venta") }
                    }
                }
            }
        }
    }
    if (showSale) SaleDialog(state, viewModel, { showSale = false }) { invoice = it; showSale = false }
    invoice?.let { SaleInvoiceDialog(it) { invoice = null } }
    saleToEdit?.let { sale ->
        SaleEditDialog(sale, onDismiss = { saleToEdit = null }, onSave = { customer, id, payment ->
            viewModel.updateSale(sale, customer, id, payment)
            saleToEdit = null
        })
    }
    saleToDelete?.let { sale ->
        AlertDialog(
            onDismissRequest = { saleToDelete = null },
            title = { Text("Eliminar venta") },
            text = { Text("La cantidad vendida volverá al inventario. ¿Eliminar la venta de ${sale.customerName}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSale(sale)
                    saleToDelete = null
                }) { Text("Eliminar y reponer stock") }
            },
            dismissButton = { TextButton(onClick = { saleToDelete = null }) { Text("Cancelar") } }
        )
    }
}

private data class SaleInvoice(
    val customer: String,
    val customerId: String,
    val payment: String,
    val date: Long,
    val items: List<CartLine>,
    val garmentNames: Map<Long, String>
) {
    val subtotal get() = items.sumOf { it.quantity * it.unitPrice }
    val itbis get() = kotlin.math.round(subtotal * 0.18 * 100) / 100
    val total get() = subtotal + itbis
}

@Composable
private fun SaleInvoiceDialog(invoice: SaleInvoice, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Factura de venta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("BoutiqueStyle · ${formatDate(invoice.date)}")
                Text("Cliente: ${invoice.customer}")
                Text("Cédula / RNC: ${invoice.customerId}")
                Text("Pago: ${invoice.payment}")
                HorizontalDivider()
                invoice.items.forEach { line ->
                    Text("${invoice.garmentNames[line.variant.garmentId] ?: "Prenda"} · ${line.variant.size} · ${line.variant.color} · ${line.quantity} × ${formatCurrency(line.unitPrice)} = ${formatCurrency(line.quantity * line.unitPrice)}")
                }
                HorizontalDivider()
                Text("Subtotal: ${formatCurrency(invoice.subtotal)}")
                Text("ITBIS (18 %): ${formatCurrency(invoice.itbis)}")
                Text("Total: ${formatCurrency(invoice.total)}", fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } }
    )
}

@Composable
private fun SaleEditDialog(
    sale: SaleEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var customer by remember(sale.id) { mutableStateOf(sale.customerName) }
    var customerId by remember(sale.id) { mutableStateOf(sale.customerCedulaOrRnc) }
    var payment by remember(sale.id) { mutableStateOf(sale.paymentMethod) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar venta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InputField(customer, { customer = it }, "Nombre del cliente")
                InputField(customerId, { customerId = it }, "Cédula / RNC")
                InputField(payment, { payment = it }, "Método de pago")
            }
        },
        confirmButton = { TextButton(onClick = { onSave(customer, customerId, payment) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun SaleDialog(state: StoreUiState, viewModel: MainViewModel, dismiss: () -> Unit, onInvoice: (SaleInvoice) -> Unit) {
    var selected by remember { mutableStateOf<VariantEntity?>(null) }
    val cart = remember { mutableStateListOf<CartLine>() }
    var expanded by remember { mutableStateOf(false) }
    var quantity by remember { mutableStateOf("1") }
    var customer by remember { mutableStateOf("") }
    var id by remember { mutableStateOf("") }
    var payment by remember { mutableStateOf("Efectivo") }
    var formError by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Registrar venta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selected?.let { variantLabel(it, state) } ?: "Seleccionar variante")
                    }
                    DropdownMenu(expanded, { expanded = false }) {
                        state.variants.filter { it.stock > 0 }.forEach { variant ->
                            DropdownMenuItem(text = { Text(variantLabel(variant, state)) }, onClick = { selected = variant; expanded = false })
                        }
                    }
                }
                InputField(quantity, { quantity = it }, "Cantidad", KeyboardType.Number)
                    OutlinedButton(
                        onClick = {
                            val variant = selected
                            val amount = quantity.toIntOrNull() ?: 0
                            val garment = state.garments.firstOrNull { it.id == variant?.garmentId }
                            if (variant == null || garment == null) {
                                formError = "Selecciona una variante disponible."
                            } else if (amount <= 0) {
                                formError = "La cantidad debe ser mayor que cero."
                            } else {
                                val existing = cart.firstOrNull { it.variant.id == variant.id }
                                if (amount + (existing?.quantity ?: 0) > variant.stock) {
                                    formError = "La cantidad supera las existencias disponibles."
                                } else {
                                    if (existing != null) cart.remove(existing)
                                    cart.add(CartLine(variant, amount + (existing?.quantity ?: 0), garment.basePrice))
                                    formError = null
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Añadir al carrito") }
                    cart.forEach { line ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${variantLabel(line.variant, state)} × ${line.quantity}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            Text(formatCurrency(line.quantity * line.unitPrice), style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = { cart.remove(line) }) { Icon(Icons.Default.Delete, "Quitar del carrito") }
                        }
                    }
                    InputField(customer, { customer = it }, "Nombre del cliente")
                    InputField(id, { id = it }, "Cédula / RNC")
                    InputField(payment, { payment = it }, "Método de pago")
                    val subtotal = cart.sumOf { it.quantity * it.unitPrice }
                    Text("Subtotal ${formatCurrency(subtotal)} · ITBIS ${formatCurrency(kotlin.math.round(subtotal * 0.18 * 100) / 100)}", style = MaterialTheme.typography.bodySmall)
                formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (cart.isEmpty()) {
                    formError = "Añade al menos una variante al carrito."
                } else {
                    val purchasedItems = cart.toList()
                    viewModel.createSale(purchasedItems, customer, id, payment) {
                        onInvoice(SaleInvoice(customer.trim(), id.trim(), payment, System.currentTimeMillis(), purchasedItems, state.garments.associate { it.id to it.name }))
                    }
                }
            }) { Text("Confirmar") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancelar") } }
    )
}

@Composable
fun LayawaysScreen(viewModel: MainViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showNew by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var fromDate by remember { mutableStateOf("") }
    var throughDate by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Apartados", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            FilledTonalButton(onClick = { showNew = true }) { Icon(Icons.Default.Add, null); Text("Nuevo") }
        }
        InputField(query, { query = it }, "Buscar cliente o cédula")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InputField(fromDate, { fromDate = it }, "Desde dd/MM/yyyy", modifier = Modifier.weight(1f))
            InputField(throughDate, { throughDate = it }, "Hasta dd/MM/yyyy", modifier = Modifier.weight(1f))
        }
        LazyColumn(contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.layaways.filter {
                (it.customerName.contains(query, true) || it.customerCedula.contains(query)) &&
                    it.createdAt.inDateRange(fromDate, throughDate)
            }) { layaway ->
                Card {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(layaway.customerName, fontWeight = FontWeight.SemiBold)
                        Text("${layaway.status} · ${formatDate(layaway.createdAt)} · saldo ${formatCurrency(layaway.balance)}")
                        if (layaway.status == "ACTIVE") Row {
                            TextButton(onClick = { viewModel.completeLayaway(layaway) }) { Text("Completar") }
                            TextButton(onClick = { viewModel.cancelLayaway(layaway) }) { Text("Cancelar apartado") }
                        }
                    }
                }
            }
        }
    }
    if (showNew) LayawayDialog(state, viewModel, { showNew = false })
}

@Composable
private fun LayawayDialog(state: StoreUiState, viewModel: MainViewModel, dismiss: () -> Unit) {
    var selected by remember { mutableStateOf<VariantEntity?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var quantity by remember { mutableStateOf("1") }
    var customer by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var cedula by remember { mutableStateOf("") }
    var paid by remember { mutableStateOf("0") }
    var formError by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Nuevo apartado (30 días)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selected?.let { variantLabel(it, state) } ?: "Seleccionar variante") }
                    DropdownMenu(expanded, { expanded = false }) {
                        state.variants.filter { it.stock > 0 }.forEach { variant ->
                            DropdownMenuItem(text = { Text(variantLabel(variant, state)) }, onClick = { selected = variant; expanded = false })
                        }
                    }
                }
                InputField(quantity, { quantity = it }, "Cantidad", KeyboardType.Number)
                InputField(customer, { customer = it }, "Nombre")
                InputField(phone, { phone = it }, "Teléfono", KeyboardType.Phone)
                InputField(cedula, { cedula = it }, "Cédula / RNC", KeyboardType.Number)
                InputField(paid, { paid = it }, "Abono inicial RD$", KeyboardType.Decimal)
                formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val variant = selected
                if (variant == null) {
                    formError = "Selecciona una variante con existencias."
                } else {
                    viewModel.createLayaway(variant, quantity.toIntOrNull() ?: 0, customer, phone, cedula, paid.toDoubleOrNull() ?: 0.0)
                    dismiss()
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancelar") } }
    )
}

@Composable
fun MoreScreen(userName: String, isAdmin: Boolean, viewModel: MainViewModel, onLogout: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var reportMessage by remember { mutableStateOf<String?>(null) }
    var confirmRestore by remember { mutableStateOf(false) }
    var reportFromDate by remember { mutableStateOf("") }
    var reportThroughDate by remember { mutableStateOf("") }
    var userToEdit by remember { mutableStateOf<UserEntity?>(null) }
    var showUserEditor by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<UserEntity?>(null) }
    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.sqlite3")) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importBackup(uri)
    }
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
        item { Text("Más opciones", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("$userName · ${if (isAdmin) "ADMINISTRADORA" else "EMPLEADO"}") }
        if (isAdmin) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Usuarios", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    FilledTonalButton(onClick = { userToEdit = null; showUserEditor = true }) { Icon(Icons.Default.Add, null); Text("Añadir") }
                }
            }
            items(state.users, key = { "user-${it.id}" }) { managedUser ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                            Text(managedUser.name, fontWeight = FontWeight.SemiBold)
                            Text("${managedUser.username} · ${if (managedUser.role == "ADMIN") "ADMINISTRADOR" else "EMPLEADO"}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { userToEdit = managedUser; showUserEditor = true }) { Icon(Icons.Default.Edit, "Editar usuario") }
                        IconButton(onClick = { userToDelete = managedUser }) { Icon(Icons.Default.Delete, "Eliminar usuario") }
                    }
                }
            }
            item { Text("Reposiciones pendientes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
            val pendingRequests = state.stockRequests.filter { it.status == "PENDING" }
            if (pendingRequests.isEmpty()) item { Text("No hay solicitudes por revisar.", style = MaterialTheme.typography.bodySmall) }
            items(pendingRequests, key = { "stock-request-${it.id}" }) { request ->
                val variant = state.variants.firstOrNull { it.id == request.variantId }
                val garment = state.garments.firstOrNull { it.id == variant?.garmentId }
                val employee = state.users.firstOrNull { it.id == request.employeeUserId }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(garment?.name ?: "Prenda", fontWeight = FontWeight.SemiBold)
                        Text("${variant?.size.orEmpty()} · ${variant?.color.orEmpty()} · Stock ${variant?.stock ?: 0} · Solicita ${request.quantity}")
                        Text("Por ${employee?.name ?: "Empleado"} · ${request.reason}", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.reviewStockRequest(request, approve = true) }) { Text("Aprobar") }
                            OutlinedButton(onClick = { viewModel.reviewStockRequest(request, approve = false) }) { Text("Rechazar") }
                        }
                    }
                }
            }
        }
        item { Text("Reportes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InputField(reportFromDate, { reportFromDate = it }, "Desde dd/MM/yyyy", modifier = Modifier.weight(1f))
                InputField(reportThroughDate, { reportThroughDate = it }, "Hasta dd/MM/yyyy", modifier = Modifier.weight(1f))
            }
        }
        item { OutlinedButton(onClick = { runCatching { ReportExporter(context).stockReport(state) }.onSuccess { reportMessage = "Inventario PDF guardado en Descargas." }.onFailure { reportMessage = it.message } }, modifier = Modifier.fillMaxWidth()) { Text("Inventario y variantes · PDF") } }
        item {
            OutlinedButton(onClick = {
                val sales = state.sales.filter { it.saleDate.inDateRange(reportFromDate, reportThroughDate) }
                val reportState = state.copy(sales = sales, saleItems = state.saleItems.filter { item -> sales.any { it.id == item.saleId } })
                runCatching { ReportExporter(context).salesReport(reportState) }
                    .onSuccess { reportMessage = "Reporte PDF y CSV guardados en Descargas." }
                    .onFailure { reportMessage = it.message }
            }, modifier = Modifier.fillMaxWidth()) { Text("Ventas por período · PDF + CSV") }
        }
        item {
            OutlinedButton(onClick = {
                val sales = state.sales.filter { it.saleDate.inDateRange(reportFromDate, reportThroughDate) }
                val reportState = state.copy(sales = sales, saleItems = state.saleItems.filter { item -> sales.any { it.id == item.saleId } })
                runCatching { ReportExporter(context).managementReport(reportState) }
                    .onSuccess { reportMessage = "Resumen gerencial PDF guardado en Descargas." }
                    .onFailure { reportMessage = it.message }
            }, modifier = Modifier.fillMaxWidth()) { Text("Resumen gerencial · PDF") }
        }
        item { if (reportMessage != null) Text(reportMessage.orEmpty(), color = MaterialTheme.colorScheme.primary) }
        item { Text("Respaldo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        item { OutlinedButton(onClick = { backupPicker.launch("boutiquestyle-backup.db") }, modifier = Modifier.fillMaxWidth()) { Text("Exportar respaldo SQLite") } }
        item { OutlinedButton(onClick = { confirmRestore = true }, modifier = Modifier.fillMaxWidth()) { Text("Importar respaldo SQLite") } }
        item { Text("La importación reemplaza todos los datos locales.", style = MaterialTheme.typography.bodySmall) }
        item { OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Icon(Icons.AutoMirrored.Filled.Logout, null); Text("Cerrar sesión") } }
        item { Text("BoutiqueStyle · RD$ · ITBIS 18%", style = MaterialTheme.typography.bodySmall) }
    }
    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Reemplazar datos") },
            text = { Text("Se reemplazarán prendas, ventas, apartados y usuarios por los del respaldo. ¿Continuar?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    restorePicker.launch(arrayOf("application/vnd.sqlite3", "application/octet-stream", "application/x-sqlite3"))
                }) { Text("Elegir respaldo") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Cancelar") } }
        )
    }
    if (showUserEditor) {
        UserEditorDialog(
            initial = userToEdit,
            onDismiss = { showUserEditor = false },
            onSave = { editedUser, password ->
                viewModel.saveUser(editedUser, password)
                showUserEditor = false
            }
        )
    }
    userToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Eliminar usuario") },
            text = { Text("¿Eliminar la cuenta ${target.username}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteUser(target)
                    userToDelete = null
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { userToDelete = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun UserEditorDialog(
    initial: UserEntity?,
    onDismiss: () -> Unit,
    onSave: (UserEntity, String) -> Unit
) {
    var username by remember(initial) { mutableStateOf(initial?.username.orEmpty()) }
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var role by remember(initial) { mutableStateOf(initial?.role ?: "OPERATOR") }
    var password by remember(initial) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nuevo usuario" else "Editar usuario") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InputField(username, { username = it }, "Usuario")
                InputField(name, { name = it }, "Nombre")
                TextButton(onClick = { role = if (role == "ADMIN") "OPERATOR" else "ADMIN" }) { Text("Rol: ${if (role == "ADMIN") "ADMINISTRADOR" else "EMPLEADO"} · cambiar") }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (initial == null) "Contraseña" else "Nueva contraseña (opcional)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    UserEntity(
                        id = initial?.id ?: 0,
                        username = username.trim(),
                        passwordHash = initial?.passwordHash.orEmpty(),
                        role = role,
                        name = name.trim(),
                        createdAt = initial?.createdAt ?: System.currentTimeMillis()
                    ),
                    password
                )
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboard: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(value, onValueChange, label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = keyboard), singleLine = true, modifier = modifier.fillMaxWidth())
}

@Composable
private fun ListCard(title: String, subtitle: String, trailing: String) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(trailing, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun variantLabel(variant: VariantEntity, state: StoreUiState): String {
    val garment = state.garments.firstOrNull { it.id == variant.garmentId }?.name ?: "Prenda"
    return "$garment · ${variant.size} ${variant.color} · stock ${variant.stock}"
}

private fun Long.inDateRange(from: String, through: String): Boolean {
    val zone = java.time.ZoneId.systemDefault()
    val date = java.time.Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
    val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val fromDate = runCatching { java.time.LocalDate.parse(from, formatter) }.getOrNull()
    val throughDate = runCatching { java.time.LocalDate.parse(through, formatter) }.getOrNull()
    return (fromDate == null || !date.isBefore(fromDate)) &&
        (throughDate == null || !date.isAfter(throughDate))
}
