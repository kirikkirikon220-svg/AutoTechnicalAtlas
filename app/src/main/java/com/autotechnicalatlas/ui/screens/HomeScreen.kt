package com.autotechnicalatlas.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.autotechnicalatlas.data.model.Vehicle

@Composable
fun HomeScreen(
    vehicles: List<Vehicle>,
    onVehicleSelected: (Vehicle) -> Unit,
    onImport: (android.net.Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImport(uri)
    }

    val filtered = vehicles.filter { vehicle ->
        val q = query.trim().lowercase()
        q.isBlank() || listOf(
            vehicle.manufacturer,
            vehicle.model,
            vehicle.generation,
            vehicle.modification,
            vehicle.engine,
            vehicle.year.toString()
        ).any { it.lowercase().contains(q) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AUTO TECHNICAL ATLAS")
                        Text("OFFLINE • ENGINEERING VIEWER", style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    TextButton(
                        onClick = { picker.launch(arrayOf("application/json", "text/json", "text/plain")) }
                    ) {
                        Text("ИМПОРТ JSON")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Поиск автомобиля") },
                placeholder = { Text("BMW F10 M5 S63, Passat B3 2E…") }
            )

            Spacer(Modifier.height(12.dp))
            Text("КОНФИГУРАЦИИ", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))

            if (filtered.isEmpty()) {
                Text("Нет подходящих конфигураций.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 18.dp)
                ) {
                    items(filtered, key = { it.id }) { vehicle ->
                        VehicleCard(vehicle, onVehicleSelected)
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleCard(vehicle: Vehicle, onClick: (Vehicle) -> Unit) {
    OutlinedCard(onClick = { onClick(vehicle) }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "${vehicle.manufacturer} ${vehicle.model}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "${vehicle.generation} • ${vehicle.year} • ${vehicle.modification}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${vehicle.engine} • ${vehicle.transmission} • ${vehicle.drivetrain}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Систем: ${vehicle.systems.size}   Деталей: ${vehicle.components.size}   Связей: ${vehicle.connections.size}",
                style = MaterialTheme.typography.labelSmall
            )
            val verified = vehicle.sources.any { it.verified }
            Spacer(Modifier.height(6.dp))
            Text(
                if (verified) "ИСТОЧНИКИ: ПРОВЕРЕНО" else "ИСТОЧНИКИ: ТРЕБУЮТ ПРОВЕРКИ",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
