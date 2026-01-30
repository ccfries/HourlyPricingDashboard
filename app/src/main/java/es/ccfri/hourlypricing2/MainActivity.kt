package es.ccfri.hourlypricing2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = SettingsRepository(applicationContext)
        
        setContent {
            val navController = rememberNavController()
            val pricingViewModel: PricingViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return PricingViewModel(repository) as T
                    }
                }
            )

            NavHost(navController = navController, startDestination = "dashboard") {
                composable("dashboard") {
                    DashboardScreen(pricingViewModel) {
                        navController.navigate("settings") {
                            launchSingleTop = true
                        }
                    }
                }
                composable("settings") {
                    SettingsScreen(repository) {
                        // Use popBackStack with a route and inclusive=false to ensure 
                        // we never pop the dashboard itself, which avoids the white screen.
                        navController.popBackStack("dashboard", inclusive = false)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: PricingViewModel, onSettingsClick: () -> Unit) {
    val price by viewModel.displayPrice.collectAsState(initial = null)
    val deliveryPrice by viewModel.deliveryPrice.collectAsState(initial = 0.0)
    val colorOffset = when {
        deliveryPrice > 0.0 -> 6.0
        else -> 0.0
    }
    val backgroundColor = when {
        price == null -> Color.Gray
        price!! < (1.0 + colorOffset) -> Color(0xFF006F20) // Green
        price!! < (8.0 + colorOffset) -> Color(0xFF4CAF50) // Green
        price!! < (11.0 + colorOffset) -> Color(0xFFFFC107) // Yellow
        price!! < (14.0 + colorOffset) -> Color(0xFFFF8000) // Orange
        price!! < (20.0 + colorOffset) -> Color(0xFFF44336) // Reddish
        else -> Color(0xFF800000) // Red
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable { onSettingsClick() }
    ) {
        val isTablet = maxWidth >= 600.dp
        
        // Scale font sizes based on device type
        val priceFontSize = if (isTablet) 200.sp else 90.sp
        val unitFontSize = if (isTablet) 40.sp else 20.sp
        val deliveryFontSize = if (isTablet) 28.sp else 14.sp
        val settingsIconSize = if (isTablet) 48.dp else 32.dp

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Push price to vertical center
            Spacer(modifier = Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Text(
                    text = price?.let { String.format("%.1f¢", it) } ?: "Loading...",
                    fontSize = priceFontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = priceFontSize
                )
                Text(
                    text = "per kWh",
                    fontSize = unitFontSize,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // Space between price and delivery note
            Spacer(modifier = Modifier.weight(1f))
            
            if (deliveryPrice > 0) {
                Text(
                    text = String.format("Includes %.1f¢ delivery", deliveryPrice),
                    modifier = Modifier.padding(bottom = if (isTablet) 64.dp else 32.dp),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = deliveryFontSize,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(if (isTablet) 24.dp else 8.dp)
        ) {
            Icon(
                Icons.Default.Settings, 
                contentDescription = "Settings", 
                tint = Color.White,
                modifier = Modifier.size(settingsIconSize)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(repository: SettingsRepository, onBack: () -> Unit) {
    val includeDelivery by repository.includeDelivery.collectAsState(initial = false)
    val deliveryType by repository.deliveryType.collectAsState(initial = DeliveryType.FIXED)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Include Delivery Pricing")
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = includeDelivery,
                    onCheckedChange = { 
                        scope.launch { repository.setIncludeDelivery(it) } 
                    }
                )
            }

            if (includeDelivery) {
                Spacer(Modifier.height(16.dp))
                Text("Delivery Type", style = MaterialTheme.typography.titleMedium)
                
                DeliveryType.entries.filter { it != DeliveryType.NONE }.forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { repository.setDeliveryType(type) }
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = deliveryType == type,
                            onClick = {
                                scope.launch { repository.setDeliveryType(type) }
                            }
                        )
                        Text(type.name.replace("_", " "))
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        }
    }
}
