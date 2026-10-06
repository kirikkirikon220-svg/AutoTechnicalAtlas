package com.autotechnicalatlas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.autotechnicalatlas.data.model.Vehicle
import com.autotechnicalatlas.data.repository.AtlasRepository
import com.autotechnicalatlas.ui.screens.HomeScreen
import com.autotechnicalatlas.ui.screens.ViewerScreen
import com.autotechnicalatlas.ui.theme.AtlasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AtlasTheme {
                AutoTechnicalAtlasApp()
            }
        }
    }
}

@Composable
fun AutoTechnicalAtlasApp() {
    val context = LocalContext.current
    val repository = remember(context) { AtlasRepository(context.applicationContext) }

    var selectedVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var refreshToken by remember { mutableIntStateOf(0) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val vehicles = remember(refreshToken) { repository.loadVehicles() }

    if (selectedVehicle == null) {
        HomeScreen(
            vehicles = vehicles,
            onVehicleSelected = { selectedVehicle = it },
            onImport = { uri ->
                runCatching {
                    repository.importVehicle(uri)
                    refreshToken++
                }.onFailure { error ->
                    errorText = error.message ?: "Ошибка импорта"
                }
            }
        )
    } else {
        ViewerScreen(
            vehicle = selectedVehicle!!,
            onBack = { selectedVehicle = null }
        )
    }

    errorText?.let { message ->
        LaunchedEffect(message) {
            errorText = null
        }
    }
}
