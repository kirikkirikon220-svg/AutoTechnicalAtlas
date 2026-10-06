@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.autotechnicalatlas.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.autotechnicalatlas.data.model.Component
import com.autotechnicalatlas.data.model.Connection
import com.autotechnicalatlas.data.model.ConnectionType
import com.autotechnicalatlas.data.model.Vehicle
import com.autotechnicalatlas.ui.components.TechnicalCanvas

@Composable
fun ViewerScreen(
    vehicle: Vehicle,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSystem by remember(vehicle.id) {
        mutableStateOf<String?>(null)
    }

    var selectedComponentId by remember(vehicle.id) {
        mutableStateOf<String?>(null)
    }

    val selected =
        vehicle.components.firstOrNull {
            it.id == selectedComponentId
        }

    val selectedConnections =
        selected?.let { component ->
            vehicle.connections.filter {
                it.fromComponentId ==
                    component.id ||
                    it.toComponentId ==
                    component.id
            }
        }.orEmpty()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    TextButton(
                        onClick = onBack
                    ) {
                        Text("‹ НАЗАД")
                    }
                },
                title = {
                    Column {
                        Text(
                            "${vehicle.manufacturer} ${vehicle.model}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            "${vehicle.generation} • ${vehicle.year} • ${vehicle.modification} • ${vehicle.engine}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
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

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                color = Color.White,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(
                            rememberScrollState()
                        )
                        .padding(
                            horizontal = 7.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected =
                            activeSystem == null,
                        onClick = {
                            activeSystem = null
                            selectedComponentId = null
                        },
                        label = {
                            Text("ВСЕ")
                        }
                    )

                    vehicle.systems.forEach {
                        system ->

                        FilterChip(
                            selected =
                                activeSystem ==
                                    system.id,
                            onClick = {
                                activeSystem =
                                    if (
                                        activeSystem ==
                                            system.id
                                    ) {
                                        null
                                    } else {
                                        system.id
                                    }

                                selectedComponentId =
                                    null
                            },
                            label = {
                                Text(
                                    system.name
                                )
                            }
                        )
                    }
                }
            }

            Spacer(
                Modifier.height(7.dp)
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = Color.White,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline
                ),
                shape =
                    MaterialTheme.shapes.medium
            ) {
                TechnicalCanvas(
                    visual = vehicle.visual,
                    components =
                        vehicle.components,
                    connections =
                        vehicle.connections,
                    selectedComponentId =
                        selectedComponentId,
                    activeSystemId =
                        activeSystem,
                    onSelect = {
                        selectedComponentId = it
                    },
                    modifier =
                        Modifier.fillMaxSize()
                )
            }

            Spacer(
                Modifier.height(7.dp)
            )

            TechnicalLegend()

            selected?.let {
                component ->

                ComponentDetail(
                    vehicle = vehicle,
                    component = component,
                    selectedConnections =
                        selectedConnections,
                    onClear = {
                        selectedComponentId = null
                    }
                )
            }
        }
    }
}

@Composable
private fun TechnicalLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(
                rememberScrollState()
            ),
        horizontalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {
        LegendItem(
            "ПИТАНИЕ",
            Color(0xFF1976C5)
        )

        LegendItem(
            "CAN/LIN",
            Color(0xFF214B68)
        )

        LegendItem(
            "ОЖ",
            Color(0xFF2A7B62)
        )

        LegendItem(
            "ТОПЛИВО",
            Color(0xFFB46D00)
        )

        LegendItem(
            "ТОРМОЗ",
            Color(0xFFA84242)
        )

        LegendItem(
            "МЕХАНИКА",
            Color(0xFF454D52)
        )
    }
}

@Composable
private fun LegendItem(
    name: String,
    color: Color
) {
    Surface(
        color = color.copy(
            alpha = 0.09f
        ),
        shape =
            MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 5.dp
            ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(8.dp),
                color = color,
                shape =
                    MaterialTheme.shapes.small
            ) {}

            Spacer(
                Modifier.width(5.dp)
            )

            Text(
                name,
                style =
                    MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun ComponentDetail(
    vehicle: Vehicle,
    component: Component,
    selectedConnections:
        List<Connection>,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 7.dp,
                bottom = 8.dp
            ),
        shape =
            MaterialTheme.shapes.medium,
        color = Color.White,
        tonalElevation = 2.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(
                12.dp
            )
        ) {
            Row(
                verticalAlignment =
                    Alignment.Top
            ) {
                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {
                    Text(
                        component.name,
                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    Spacer(
                        Modifier.height(2.dp)
                    )

                    Text(
                        component.description,
                        style =
                            MaterialTheme.typography.bodySmall
                    )

                    Text(
                        "Система: ${systemName(vehicle, component)}",
                        style =
                            MaterialTheme.typography.labelSmall
                    )
                }

                TextButton(
                    onClick = onClear
                ) {
                    Text("×")
                }
            }

            if (
                selectedConnections.isNotEmpty()
            ) {
                Spacer(
                    Modifier.height(6.dp)
                )

                Text(
                    "СХЕМА ПОДКЛЮЧЕНИЙ",
                    style =
                        MaterialTheme.typography.labelMedium
                )

                Spacer(
                    Modifier.height(4.dp)
                )

                selectedConnections.forEach {
                    connection ->

                    val from =
                        vehicle.components
                            .firstOrNull {
                                it.id ==
                                    connection
                                        .fromComponentId
                            }

                    val to =
                        vehicle.components
                            .firstOrNull {
                                it.id ==
                                    connection
                                        .toComponentId
                            }

                    Surface(
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(
                                    vertical = 2.dp
                                ),
                        color = Color(0xFFF7F9FA),
                        shape =
                            MaterialTheme.shapes.small,
                        border =
                            BorderStroke(
                                1.dp,
                                MaterialTheme
                                    .colorScheme
                                    .outlineVariant
                            )
                    ) {
                        Column(
                            modifier =
                                Modifier.padding(
                                    9.dp
                                )
                        ) {
                            Text(
                                connection
                                    .type
                                    .titleRu(),
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelSmall,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                            )

                            Text(
                                "${from?.name ?: connection.fromComponentId}  →  ${to?.name ?: connection.toComponentId}",
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )

                            if (
                                connection
                                    .purpose
                                    .isNotBlank()
                            ) {
                                Text(
                                    "Зачем: ${connection.purpose}",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelSmall
                                )
                            }

                            if (
                                connection
                                    .direction
                                    .isNotBlank()
                            ) {
                                Text(
                                    "Направление: ${connection.direction}",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelSmall
                                )
                            }

                            if (
                                connection
                                    .label
                                    .isNotBlank()
                            ) {
                                Text(
                                    connection.label,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelSmall,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun systemName(
    vehicle: Vehicle,
    component: Component
): String =
    vehicle.systems
        .firstOrNull {
            it.id == component.systemId
        }
        ?.name
        ?: component.systemId

private fun ConnectionType.titleRu():
    String =
    when (this) {
        ConnectionType.ELECTRICAL ->
            "ЭЛЕКТРИЧЕСКАЯ ЦЕПЬ"

        ConnectionType.CAN ->
            "CAN ШИНА"

        ConnectionType.LIN ->
            "LIN ШИНА"

        ConnectionType.FUEL ->
            "ТОПЛИВНАЯ ЛИНИЯ"

        ConnectionType.COOLANT ->
            "КОНТУР ОХЛАЖДЕНИЯ"

        ConnectionType.OIL ->
            "МАСЛЯНЫЙ КОНТУР"

        ConnectionType.VACUUM ->
            "ВАКУУМ"

        ConnectionType.AIR ->
            "ВОЗДУХ"

        ConnectionType.EXHAUST ->
            "ВЫПУСК"

        ConnectionType.BRAKE ->
            "ТОРМОЗНАЯ ЛИНИЯ"

        ConnectionType.HYDRAULIC ->
            "ГИДРАВЛИКА"

        ConnectionType.MECHANICAL ->
            "МЕХАНИЧЕСКАЯ СВЯЗЬ"
    }
