package com.sanvalero.boutiquestyle.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sanvalero.boutiquestyle.ui.screens.*

private data class Destination(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun BoutiqueNavigation(viewModel: MainViewModel = hiltViewModel()) {
    val user by viewModel.user.collectAsState()
    val message by viewModel.message.collectAsState()
    val navController = rememberNavController()
    var customerMode by remember { mutableStateOf(false) }
    val destinations = listOf(
        Destination("dashboard", "Inicio", Icons.Default.Home),
        Destination("sales", "Ventas", Icons.AutoMirrored.Filled.ReceiptLong),
        Destination("inventory", "Stock", Icons.Default.Inventory2),
        Destination("reports", "Reportes", Icons.Default.Assessment),
        Destination("more", "Perfil", Icons.Default.Person)
    )

    LaunchedEffect(user) {
        if (user?.role == "ADMIN") {
            navController.navigate("dashboard") {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    if (user == null && !customerMode) {
        LoginScreen(
            onLogin = viewModel::login,
            onCustomerLogin = viewModel::loginCustomer,
            onBrowseStore = { customerMode = true },
            viewModel = viewModel
        )
    } else if (user?.role == "CUSTOMER" || (user == null && customerMode)) {
        CustomerShopScreen(
            viewModel = viewModel,
            customer = user?.takeIf { it.role == "CUSTOMER" },
            onExit = {
                if (user?.role == "CUSTOMER") {
                    viewModel.logout()
                    customerMode = true
                } else customerMode = false
            },
            onLogin = viewModel::loginCustomer,
            onRegister = viewModel::registerCustomer
        )
    } else if (user?.role == "OPERATOR") {
        EmployeeScreen(viewModel, user?.name.orEmpty(), user?.id ?: 0L, onLogout = viewModel::logout)
    } else {
        val currentEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentEntry?.destination?.route
        Scaffold(
            bottomBar = {
                NavigationBar {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo("dashboard") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.title) },
                            label = { Text(destination.title) }
                        )
                    }
                }
            },
            snackbarHost = { BoutiqueSnackbar(message, viewModel::clearMessage) }
        ) { padding ->
            NavHost(navController, startDestination = "dashboard", modifier = Modifier.padding(padding)) {
                composable("dashboard") {
                    DashboardScreen(viewModel, onOpenSales = { navController.navigate("sales") }, onOpenInventory = { navController.navigate("inventory") }, onOpenRequests = { navController.navigate("more") })
                }
                composable("inventory") { InventoryScreen(viewModel) }
                composable("sales") { SalesScreen(viewModel) }
                composable("layaways") { LayawaysScreen(viewModel) }
                composable("reports") { ReportsScreen(viewModel) }
                composable("more") {
                    MoreScreen(
                        userName = user?.name.orEmpty(),
                        isAdmin = user?.role == "ADMIN",
                        viewModel = viewModel,
                        onOpenLayaways = { navController.navigate("layaways") },
                        onLogout = {
                            viewModel.logout()
                        }
                    )
                }
                composable("login") { LoginScreen(onLogin = viewModel::login, onCustomerLogin = viewModel::loginCustomer, viewModel = viewModel) }
            }
        }
    }
}
