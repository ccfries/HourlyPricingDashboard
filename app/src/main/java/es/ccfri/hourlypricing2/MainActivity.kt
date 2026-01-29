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
                        navController.navigate("settings")
                    }
                }
                composable("settings") {
                    SettingsScreen(repository) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: PricingViewModel, onSettingsClick: () -> Unit) {
    val price by viewModel.displayPrice.collectAsState(initial = null)

    val backgroundColor = when {
        price == null -> Color.Gray
        price!! < 7.0 -> Color(0xFF4CAF50) // Green
        price!! < 14.0 -> Color(0xFFFFC107) // Yellow
        else -> Color(0xFFF44336) // Red
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable { onSettingsClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = price?.let { String.format("%.1f¢", it) } ?: "Loading...",
                fontSize = 120.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "per kWh",
                fontSize = 24.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
        
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
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
