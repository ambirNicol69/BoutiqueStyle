package com.sanvalero.boutiquestyle.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sanvalero.boutiquestyle.R
import com.sanvalero.boutiquestyle.data.local.entity.GarmentEntity
import com.sanvalero.boutiquestyle.data.local.entity.UserEntity
import com.sanvalero.boutiquestyle.data.local.entity.VariantEntity
import com.sanvalero.boutiquestyle.domain.model.CartLine
import com.sanvalero.boutiquestyle.domain.model.StoreUiState
import com.sanvalero.boutiquestyle.report.CustomerInvoice
import com.sanvalero.boutiquestyle.report.CustomerInvoiceExporter
import com.sanvalero.boutiquestyle.report.CustomerInvoiceItem
import com.sanvalero.boutiquestyle.util.formatCurrency
import com.sanvalero.boutiquestyle.util.formatDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ShopTab(val title: String) { HOME("Inicio"), CATEGORIES("Categorías"), FAVORITES("Favoritos"), CART("Carrito"), PROFILE("Perfil") }

@Composable
fun CustomerShopScreen(
    viewModel: MainViewModel,
    customer: UserEntity?,
    onExit: () -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(ShopTab.HOME) }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    var product by remember { mutableStateOf<GarmentEntity?>(null) }
    var favorites by remember { mutableStateOf(setOf<Long>()) }
    var cart by remember { mutableStateOf(emptyList<CartLine>()) }
    var checkout by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
    var orderCustomer by remember { mutableStateOf("") }
    var orderId by remember { mutableStateOf("") }
    var orderPayment by remember { mutableStateOf("") }
    var orderDate by remember { mutableStateOf(0L) }
    var showCustomerAccess by remember { mutableStateOf(false) }
    LaunchedEffect(customer) {
        if (customer != null) showCustomerAccess = false
    }
    if (completed) {
        OrderComplete(cart, state, orderCustomer, orderId, orderPayment, orderDate) { completed = false; checkout = false; cart = emptyList(); tab = ShopTab.HOME }
        return
    }
    if (checkout) {
        CheckoutScreen(cart, state, message, initialName = customer?.name.orEmpty(), onBack = { checkout = false }) { name, id, payment ->
            orderCustomer = name
            orderId = id
            orderPayment = payment
            viewModel.createSale(cart, name, id, payment) {
                orderDate = System.currentTimeMillis()
                completed = true
            }
        }
        return
    }
    val filtered = state.garments.filter { garment ->
        val textMatch = search.isBlank() || listOf(garment.name, garment.brand, garment.category).any { it.contains(search, true) }
        val categoryMatch = category == null || garment.category.equals(category, true)
        textMatch && categoryMatch && state.variants.any { it.garmentId == garment.id && it.stock > 0 }
    }
    Scaffold(
        topBar = { ShopHeader(search, { search = it }, cart.sumOf { it.quantity }, { tab = ShopTab.CART }, onExit) },
        bottomBar = {
            if (product == null) NavigationBar {
                ShopTab.entries.forEach { item ->
                    NavigationBarItem(selected = tab == item, onClick = { product = null; tab = item; category = null },
                        icon = { ShopTabIcon(item) }, label = { Text(item.title) })
                }
            }
        }
    ) { padding ->
        when {
            product != null -> ProductDetail(product!!, state, product!!.id in favorites,
                onBack = { product = null }, onFavorite = { id -> favorites = favorites.toggle(id) },
                onAdd = { variant ->
                    val current = cart.firstOrNull { it.variant.id == variant.id }
                    cart = if (current == null) cart + CartLine(variant, 1, product!!.basePrice)
                    else cart.map { if (it.variant.id == variant.id) it.copy(quantity = it.quantity + 1) else it }
                    product = null; tab = ShopTab.CART
                }, modifier = Modifier.padding(padding))
            tab == ShopTab.CART -> CartScreen(cart, state,
                onQuantity = { line, amount -> cart = cart.map { if (it.variant.id == line.variant.id) it.copy(quantity = amount) else it }.filter { it.quantity > 0 } },
                onRemove = { line -> cart = cart.filterNot { it.variant.id == line.variant.id } },
                onBrowse = { tab = ShopTab.HOME },
                onCheckout = {
                    viewModel.clearMessage()
                    if (customer == null) showCustomerAccess = true else checkout = true
                },
                modifier = Modifier.padding(padding))
            tab == ShopTab.CATEGORIES -> CategoryScreen(state.garments.map { it.category }.distinct().sorted(), state,
                onSelect = { category = it; tab = ShopTab.HOME }, modifier = Modifier.padding(padding))
            tab == ShopTab.FAVORITES -> ProductList("Tus favoritos", state.garments.filter { it.id in favorites }, state, favorites,
                { product = it }, { favorites = favorites.toggle(it) }, Modifier.padding(padding))
            tab == ShopTab.PROFILE -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Image(painterResource(R.drawable.logo_boutiquestyle), "Logo BoutiqueStyle", Modifier.size(110.dp))
                Text("Tu perfil", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(if (customer == null) "Explora la tienda sin iniciar sesión; entra para comprar." else "Gestiona tu sesión de cliente.")
                if (customer == null) {
                    OutlinedButton(onClick = { showCustomerAccess = true }, modifier = Modifier.padding(top = 16.dp)) { Text("Iniciar sesión como cliente") }
                    TextButton(onClick = onExit) { Text("Volver al acceso de la boutique") }
                } else {
                    Text("Sesión iniciada como ${customer.name} (@${customer.username}).")
                    OutlinedButton(onClick = onExit, modifier = Modifier.padding(top = 16.dp)) { Text("Cerrar sesión") }
                }
            }
            else -> HomeCatalog(filtered, state, favorites, category,
                state.garments.map { it.category }.distinct().sorted(), onCategory = { category = it },
                onProduct = { product = it }, onFavorite = { favorites = favorites.toggle(it) }, modifier = Modifier.padding(padding))
        }
    }
    if (showCustomerAccess) CustomerAccessDialog(
        message = message,
        onDismiss = { showCustomerAccess = false },
        onLogin = onLogin,
        onRegister = onRegister
    )
}

@Composable
private fun CustomerAccessDialog(
    message: String?,
    onDismiss: () -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit
) {
    var registering by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordMismatch by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (registering) "Crear cuenta de cliente" else "Iniciar sesión para comprar") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (registering) OutlinedTextField(name, { name = it }, label = { Text("Nombre completo") }, singleLine = true)
                OutlinedTextField(username, { username = it }, label = { Text("Usuario") }, singleLine = true)
                OutlinedTextField(password, { password = it }, label = { Text("Contraseña (mínimo 6 caracteres)") }, singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                if (registering) OutlinedTextField(
                    confirmPassword,
                    { confirmPassword = it; passwordMismatch = false },
                    label = { Text("Confirmar contraseña") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                if (passwordMismatch) Text("Las contraseñas no coinciden.", color = MaterialTheme.colorScheme.error)
                message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (registering) {
                    if (password == confirmPassword) onRegister(username, password, name) else passwordMismatch = true
                } else onLogin(username, password)
            }) { Text(if (registering) "Crear cuenta" else "Entrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Ahora no") } }
    )
}

private fun Set<Long>.toggle(id: Long) = if (id in this) this - id else this + id

@Composable
private fun ShopHeader(search: String, onSearch: (String) -> Unit, cartCount: Int, onCart: () -> Unit, onExit: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 14.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.logo_boutiquestyle), "Logo BoutiqueStyle", Modifier.size(42.dp))
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text("BoutiqueStyle", fontWeight = FontWeight.Bold)
                Text("Moda para tu estilo", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onCart) { BadgedBox(badge = { if (cartCount > 0) Badge { Text(cartCount.toString()) } }) { Icon(Icons.Default.ShoppingBag, "Carrito") } }
            TextButton(onClick = onExit) { Text("Salir") }
        }
        OutlinedTextField(search, onSearch, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Buscar ropa, marcas...") },
            leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = RoundedCornerShape(24.dp))
    }
}

@Composable
private fun ShopTabIcon(tab: ShopTab) {
    val icon = when (tab) {
        ShopTab.HOME -> Icons.Default.Home
        ShopTab.CATEGORIES -> Icons.Default.Storefront
        ShopTab.FAVORITES -> Icons.Default.FavoriteBorder
        ShopTab.CART -> Icons.Default.ShoppingCart
        ShopTab.PROFILE -> Icons.Default.Person
    }
    Icon(icon, contentDescription = tab.title)
}

@Composable
private fun HomeCatalog(products: List<GarmentEntity>, state: StoreUiState, favorites: Set<Long>, selectedCategory: String?, categories: List<String>, onCategory: (String?) -> Unit, onProduct: (GarmentEntity) -> Unit, onFavorite: (Long) -> Unit, modifier: Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF4C9D6)), shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Nuevas colecciones", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723))
                        Text("Looks que te encantan", color = Color(0xFF3E2723))
                        Text("Descubre tu próximo favorito ✦", style = MaterialTheme.typography.labelMedium, color = Color(0xFF3E2723))
                    }
                    Image(painterResource(R.drawable.logo_boutiquestyle), "BoutiqueStyle", Modifier.size(68.dp))
                }
            }
        }
        item { Text("Categorías", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FilterChip(selectedCategory == null, { onCategory(null) }, label = { Text("Todo") })
                categories.take(3).forEach { value -> FilterChip(selectedCategory == value, { onCategory(if (selectedCategory == value) null else value) }, label = { Text(value) }) }
            }
        }
        item { Text(selectedCategory ?: "Recomendados para ti", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (products.isEmpty()) item { EmptyShop("No hay prendas disponibles con esos filtros.") }
        else items(products.chunked(2)) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { garment ->
                    ProductCard(garment, state.variants.filter { it.garmentId == garment.id }.sumOf { it.stock }, garment.id in favorites,
                        { onProduct(garment) }, { onFavorite(garment.id) }, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProductList(title: String, products: List<GarmentEntity>, state: StoreUiState, favorites: Set<Long>, onProduct: (GarmentEntity) -> Unit, onFavorite: (Long) -> Unit, modifier: Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        if (products.isEmpty()) item { EmptyShop("Todavía no tienes prendas guardadas.") }
        else items(products.chunked(2)) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { garment -> ProductCard(garment, state.variants.filter { it.garmentId == garment.id }.sumOf { it.stock }, garment.id in favorites, { onProduct(garment) }, { onFavorite(garment.id) }, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProductCard(garment: GarmentEntity, stock: Int, favorite: Boolean, onClick: () -> Unit, onFavorite: () -> Unit, modifier: Modifier) {
    Card(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp)) {
        Column {
            Box(Modifier.fillMaxWidth().height(148.dp).background(Color(0xFFF9E5EA)), contentAlignment = Alignment.Center) {
                ProductPhoto(garment.imageUri, garment.category, Modifier.fillMaxSize())
                IconButton(onFavorite, Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.White.copy(alpha = .9f), CircleShape)) {
                    Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito", tint = Color(0xFFBC4E72))
                }
            }
            Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (garment.brand.isNotBlank()) Text(garment.brand, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(garment.name, fontWeight = FontWeight.SemiBold, maxLines = 2, minLines = 2)
                Text(formatCurrency(garment.basePrice), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF9D3F5B))
                Text("$stock disponibles", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun ProductPhoto(imageUri: String?, category: String, modifier: Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null, imageUri) {
        value = imageUri?.let { uri -> withContext(Dispatchers.IO) { runCatching { context.contentResolver.openInputStream(Uri.parse(uri))?.use(BitmapFactory::decodeStream) }.getOrNull() } }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap!!.asImageBitmap(), "Prenda de $category", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(44.dp), tint = Color(0xFF9D3F5B))
            Text(category, color = Color(0xFF3E2723))
        }
    }
}

@Composable
private fun CategoryScreen(categories: List<String>, state: StoreUiState, onSelect: (String) -> Unit, modifier: Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Categorías", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        items(categories) { category ->
            Card(Modifier.fillMaxWidth().clickable { onSelect(category) }, colors = CardDefaults.cardColors(containerColor = Color(0xFFF9E5EA)), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).background(Color(0xFFF4C9D6), CircleShape), contentAlignment = Alignment.Center) { Text(category.take(1).uppercase(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723)) }
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723), maxLines = 2, softWrap = true)
                        Text("${state.garments.count { it.category == category }} estilos", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF6D5550))
                    }
                    Text("›", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
        if (categories.isEmpty()) item { EmptyShop("Las categorías aparecerán cuando agregues prendas al catálogo.") }
    }
}

@Composable
private fun ProductDetail(garment: GarmentEntity, state: StoreUiState, isFavorite: Boolean, onBack: () -> Unit, onFavorite: (Long) -> Unit, onAdd: (VariantEntity) -> Unit, modifier: Modifier) {
    val variants = state.variants.filter { it.garmentId == garment.id && it.stock > 0 }
    val colors = variants.map { it.color }.distinct()
    var selectedColor by remember(garment.id) { mutableStateOf(colors.firstOrNull().orEmpty()) }
    val sizes = variants.filter { it.color == selectedColor }.map { it.size }.distinct()
    var selectedSize by remember(garment.id) { mutableStateOf(sizes.firstOrNull().orEmpty()) }
    val selectedVariant = variants.firstOrNull { it.color == selectedColor && it.size == selectedSize }
    Column(modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
            Text(garment.name, Modifier.weight(1f), maxLines = 1, fontWeight = FontWeight.SemiBold)
            IconButton({ onFavorite(garment.id) }) { Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito", tint = Color(0xFFBC4E72)) }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Box(Modifier.fillMaxWidth().height(255.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFFF9E5EA))) { ProductPhoto(garment.imageUri, garment.category, Modifier.fillMaxSize()) } }
            item {
                if (garment.brand.isNotBlank()) Text(garment.brand, color = Color(0xFF9D3F5B), fontWeight = FontWeight.Bold)
                Text(garment.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(formatCurrency(garment.basePrice), style = MaterialTheme.typography.headlineSmall, color = Color(0xFF9D3F5B), fontWeight = FontWeight.Bold)
                if (garment.description.isNotBlank()) Text(garment.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { Text("Color: ${selectedColor.ifBlank { "No disponible" }}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colors.forEach { color ->
                        val border = if (color == selectedColor) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF9D3F5B)) else null
                        Surface(onClick = { selectedColor = color; selectedSize = variants.firstOrNull { it.color == color }?.size.orEmpty() }, shape = CircleShape, color = swatchColor(color), border = border, modifier = Modifier.size(40.dp)) {}
                    }
                }
            }
            item { Text("Talla: ${selectedSize.ifBlank { "Selecciona" }}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    sizes.forEach { size -> FilterChip(selectedSize == size, { selectedSize = size }, label = { Text(size) }) }
                }
            }
            item { Text(selectedVariant?.let { "${it.stock} unidades disponibles" } ?: "Elige una combinación disponible.", style = MaterialTheme.typography.bodySmall) }
        }
        Button(onClick = { selectedVariant?.let(onAdd) }, enabled = selectedVariant != null,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(52.dp), shape = RoundedCornerShape(26.dp)) {
            Icon(Icons.Default.ShoppingCart, null); Spacer(Modifier.width(8.dp)); Text("Agregar al carrito · ${formatCurrency(garment.basePrice)}")
        }
    }
}

@Composable
private fun CartScreen(cart: List<CartLine>, state: StoreUiState, onQuantity: (CartLine, Int) -> Unit, onRemove: (CartLine) -> Unit, onBrowse: () -> Unit, onCheckout: () -> Unit, modifier: Modifier) {
    if (cart.isEmpty()) {
        Column(modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.ShoppingCart, null, Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Tu carrito está vacío", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onBrowse, Modifier.padding(top = 12.dp)) { Text("Explorar prendas") }
        }
        return
    }
    val subtotal = cart.sumOf { it.quantity * it.unitPrice }
    val tax = moneyTax(subtotal)
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Mi carrito", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        items(cart, key = { it.variant.id }) { line ->
            val garment = state.garments.firstOrNull { it.id == line.variant.garmentId }
            Card {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(70.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF9E5EA))) {
                        ProductPhoto(garment?.imageUri, garment?.category ?: "Prenda", Modifier.fillMaxSize())
                    }
                    Column(Modifier.weight(1f)) {
                        Text(garment?.brand.orEmpty(), style = MaterialTheme.typography.labelMedium)
                        Text(garment?.name ?: "Prenda", fontWeight = FontWeight.SemiBold)
                        Text("${line.variant.color} · Talla ${line.variant.size}", style = MaterialTheme.typography.bodySmall)
                        Text(formatCurrency(line.unitPrice), fontWeight = FontWeight.Bold, color = Color(0xFF9D3F5B))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onQuantity(line, line.quantity - 1) }) { Text("−") }
                            Text(line.quantity.toString())
                            TextButton(onClick = { if (line.quantity < line.variant.stock) onQuantity(line, line.quantity + 1) }) { Text("+") }
                            TextButton(onClick = { onRemove(line) }) { Text("Quitar") }
                        }
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resumen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    PriceLine("Subtotal", subtotal); PriceLine("ITBIS (18 %)", tax); HorizontalDivider()
                    PriceLine("Total", subtotal + tax, true)
                    Button(onCheckout, Modifier.fillMaxWidth()) { Text("Continuar · ${formatCurrency(subtotal + tax)}") }
                }
            }
        }
    }
}

@Composable
private fun CheckoutScreen(cart: List<CartLine>, state: StoreUiState, message: String?, initialName: String, onBack: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var identification by remember { mutableStateOf("") }
    var payment by remember { mutableStateOf("Efectivo") }
    val subtotal = cart.sumOf { it.quantity * it.unitPrice }
    val total = subtotal + moneyTax(subtotal)
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
            Text("Finalizar compra", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("Datos del cliente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item { OutlinedTextField(name, { name = it }, label = { Text("Nombre completo") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item { IdentificationField(identification, { identification = it }) }
            item { Text("Método de pago", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(listOf("Efectivo", "Tarjeta", "Transferencia")) { option ->
                Row(Modifier.fillMaxWidth().clickable { payment = option }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(payment == option, { payment = option }); Text(option)
                }
            }
            item {
                Card {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("Tu pedido · ${cart.sumOf { it.quantity }} artículos", fontWeight = FontWeight.Bold)
                        cart.forEach { line ->
                            val garment = state.garments.firstOrNull { it.id == line.variant.garmentId }
                            Text("${garment?.name ?: "Prenda"} · ${line.variant.color} · ${line.variant.size} × ${line.quantity}", style = MaterialTheme.typography.bodySmall)
                        }
                        HorizontalDivider(); PriceLine("Subtotal", subtotal); PriceLine("ITBIS (18 %)", moneyTax(subtotal)); PriceLine("Total", total, true)
                    }
                }
            }
            if (!message.isNullOrBlank()) item { Text(message, color = MaterialTheme.colorScheme.error) }
        }
        Button(onClick = { onConfirm(name, identification, payment) }, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(52.dp)) { Text("Confirmar compra · ${formatCurrency(total)}") }
    }
}

@Composable
private fun OrderComplete(
    cart: List<CartLine>,
    state: StoreUiState,
    customer: String,
    identification: String,
    payment: String,
    date: Long,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportMessage by remember { mutableStateOf<String?>(null) }
    val invoice = remember(cart, state.garments, customer, identification, payment, date) {
        CustomerInvoice(
            date = date,
            customer = customer,
            identification = identification,
            payment = payment,
            items = cart.map { line ->
                val garment = state.garments.firstOrNull { it.id == line.variant.garmentId }
                CustomerInvoiceItem(
                    name = garment?.name ?: "Prenda",
                    variant = "${line.variant.color} · Talla ${line.variant.size}",
                    quantity = line.quantity,
                    unitPrice = line.unitPrice
                )
            }
        )
    }
    fun saveInvoice(uri: Uri?, asPdf: Boolean) {
        if (uri == null) return
        exportMessage = null
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(uri)
                        ?: error("No se pudo abrir el archivo seleccionado.")
                    output.use {
                        if (asPdf) CustomerInvoiceExporter.writePdf(invoice, it)
                        else CustomerInvoiceExporter.writeImage(invoice, it)
                    }
                }
            }.onSuccess {
                exportMessage = "Factura guardada correctamente."
            }.onFailure { error ->
                exportMessage = error.message ?: "No se pudo guardar la factura."
            }
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        saveInvoice(uri, asPdf = false)
    }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        saveInvoice(uri, asPdf = true)
    }
    val subtotal = invoice.subtotal
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, null, Modifier.size(78.dp), tint = Color(0xFFBC4E72))
        Text("¡Compra realizada!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Tu pedido quedó registrado en BoutiqueStyle.")
        Card(Modifier.padding(top = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF4C9D6))) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Factura de compra", fontWeight = FontWeight.Bold)
                Text("${formatDate(date)} · $payment", style = MaterialTheme.typography.bodySmall)
                Text("Cliente: $customer", style = MaterialTheme.typography.bodySmall)
                Text("Cédula / RNC: $identification", style = MaterialTheme.typography.bodySmall)
                invoice.items.forEach { item ->
                    Text("${item.name} · ${item.variant} × ${item.quantity} = ${formatCurrency(item.quantity * item.unitPrice)}", style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
                PriceLine("Subtotal", subtotal)
                PriceLine("ITBIS (18 %)", invoice.tax)
                PriceLine("Total", subtotal + invoice.tax, true)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { imagePicker.launch("factura_boutiquestyle_${date}.png") }, modifier = Modifier.weight(1f)) {
                Text("Guardar imagen")
            }
            Button(onClick = { pdfPicker.launch("factura_boutiquestyle_${date}.pdf") }, modifier = Modifier.weight(1f)) {
                Text("Guardar PDF")
            }
        }
        exportMessage?.let { Text(it, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.primary) }
        Button(onContinue, Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("Seguir comprando") }
    }
}
@Composable
private fun PriceLine(label: String, value: Double, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(formatCurrency(value), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun EmptyShop(message: String) { Card(Modifier.fillMaxWidth()) { Text(message, Modifier.padding(18.dp)) } }

private fun moneyTax(subtotal: Double) = kotlin.math.round(subtotal * 0.18 * 100.0) / 100.0

private fun swatchColor(name: String): Color = when {
    "rosa" in name.lowercase() || "pink" in name.lowercase() -> Color(0xFFE99AB5)
    "rojo" in name.lowercase() -> Color(0xFFBA3B4B)
    "azul" in name.lowercase() -> Color(0xFF6794C1)
    "verde" in name.lowercase() -> Color(0xFF6D9B78)
    "negro" in name.lowercase() -> Color(0xFF292929)
    "blanco" in name.lowercase() || "marfil" in name.lowercase() -> Color(0xFFF5F0E6)
    "gris" in name.lowercase() -> Color(0xFF9B9B9B)
    "lila" in name.lowercase() || "morado" in name.lowercase() -> Color(0xFFC09BC9)
    else -> Color(0xFFD9B7C2)
}
