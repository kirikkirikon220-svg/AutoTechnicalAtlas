@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.autotechnicalatlas.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.autotechnicalatlas.data.model.Component
import com.autotechnicalatlas.data.model.ConnectionType
import com.autotechnicalatlas.data.model.Vehicle
import com.autotechnicalatlas.ui.components.TechnicalCanvas

@Composable
fun ViewerScreen(
    vehicle: Vehicle,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSystem by remember(vehicle.id) { mutableStateOf("all") }
    var componentQuery by remember(vehicle.id) { mutableStateOf("") }
    var selectedComponentId by remember(vehicle.id) { mutableStateOf<String?>(null) }
    var exploded by remember(vehicle.id) { mutableFloatStateOf(0f) }

    val visibleComponents = vehicle.components.filter { component ->
        val systemOk = activeSystem == "all" || component.systemId == activeSystem
        val query = componentQuery.trim().lowercase()
        val queryOk = query.isBlank() || component.name.lowercase().contains(query)
        systemOk && queryOk
    }

    val visibleIds = visibleComponents.map { it.id }.toSet()
    val visibleConnections = vehicle.connections.filter {
        it.fromComponentId in visibleIds && it.toComponentId in visibleIds
    }

    val selected = vehicle.components.firstOrNull { it.id == selectedComponentId }
    val selectedConnections = selected?.let { component ->
        vehicle.connections.filter {
            it.fromComponentId == component.id || it.toComponentId == component.id
        }
    }.orEmpty()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("${vehicle.manufacturer} ${vehicle.model}")
                        Text(
                            "${vehicle.generation} • ${vehicle.year} • ${vehicle.modification} • ${vehicle.engine}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("‹ НАЗАД") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = activeSystem == "all",
                    onClick = { activeSystem = "all" },
                    label = { Text("ВСЕ") }
                )
                vehicle.systems.forEach { system ->
                    FilterChip(
                        selected = activeSystem == system.id,
                        onClick = { activeSystem = system.id },
                        label = { Text(system.name) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = componentQuery,
                onValueChange = { componentQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Поиск компонента") },
                placeholder = { Text("ECU, датчик, насос, блок, разъём…") }
            )

            Spacer(Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                tonalElevation = 1.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                TechnicalCanvas(
                    components = visibleComponents,
                    connections = visibleConnections,
                    selectedComponentId = selectedComponentId,
                    exploded = exploded,
                    onSelect = { selectedComponentId = it },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("РАЗНЕСЕНИЕ", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = exploded,
                    onValueChange = { exploded = it },
                    modifier = Modifier.weight(1f),
                    valueRange = 0f..1f
                )
                TextButton(onClick = { exploded = 0f }) { Text("СБРОС") }
            }

            selected?.let { component ->
                ComponentDetail(
                    vehicle = vehicle,
                    component = component,
                    selectedConnections = selectedConnections,
                    onClear = { selectedComponentId = null }
                )
            }
        }
    }
}

@Composable
private fun ComponentDetail(
    vehicle: Vehicle,
    component: Component,
    selectedConnections: List<com.autotechnicalatlas.data.model.Connection>,
    onClear: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(component.name, style = MaterialTheme.typography.titleMedium)
                    Text(component.description, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onClear) { Text("ЗАКРЫТЬ") }
            }

            Spacer(Modifier.height(6.dp))
            val systemName = vehicle.systems.firstOrNull { it.id == component.systemId }?.name ?: component.systemId
            Text("Система: $systemName", style = MaterialTheme.typography.labelSmall)
            Text(
                if (component.verified) "Компонент: ПРОВЕРЕНО" else "Компонент: НЕ ПРОВЕРЕНО",
                style = MaterialTheme.typography.labelSmall
            )

            if (selectedConnections.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("СВЯЗИ", style = MaterialTheme.typography.labelMedium)
                selectedConnections.take(8).forEach { connection ->
                    val otherId = if (connection.fromComponentId == component.id) {
                        connection.toComponentId
                    } else connection.fromComponentId
                    val otherName = vehicle.components.firstOrNull { it.id == otherId }?.name ?: otherId
                    Text(
                        "${connection.type.name}: $otherName${if (connection.label.isNotBlank()) " • ${connection.label}" else ""}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            val source = component.sourceId?.let { id -> vehicle.sources.firstOrNull { it.id == id } }
            if (source != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Источник: ${source.manufacturer} • ${source.document}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
