from pathlib import Path
from textwrap import dedent
import os
import subprocess
import sys

PROJECT = Path.home() / 'AutoTechnicalAtlas'

if not PROJECT.exists():
    print(f'Проект не найден: {PROJECT}')
    sys.exit(1)


def write_file(rel: str, content: str):
    path = PROJECT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(dedent(content).lstrip(), encoding='utf-8')
    print('WRITE', rel)

# ---------- Gradle ----------
write_file('app/build.gradle.kts', r'''
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.autotechnicalatlas"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.autotechnicalatlas"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "0.2"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
}
''')

# ---------- Models ----------
write_file('app/src/main/java/com/autotechnicalatlas/data/model/Vehicle.kt', r'''
package com.autotechnicalatlas.data.model

data class Vehicle(
    val id: String,
    val manufacturer: String,
    val model: String,
    val generation: String,
    val year: Int,
    val modification: String,
    val engine: String,
    val transmission: String,
    val drivetrain: String,
    val description: String = "",
    val systems: List<VehicleSystem> = emptyList(),
    val components: List<Component> = emptyList(),
    val connections: List<Connection> = emptyList(),
    val sources: List<TechnicalSource> = emptyList()
)
''')

write_file('app/src/main/java/com/autotechnicalatlas/data/model/Component.kt', r'''
package com.autotechnicalatlas.data.model

data class Component(
    val id: String,
    val name: String,
    val systemId: String,
    val description: String,
    val shape: String = "block",
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 120f,
    val height: Float = 70f,
    val layer: Int = 0,
    val verified: Boolean = false,
    val sourceId: String? = null
)
''')

write_file('app/src/main/java/com/autotechnicalatlas/data/model/Connection.kt', r'''
package com.autotechnicalatlas.data.model

enum class ConnectionType {
    ELECTRICAL,
    CAN,
    LIN,
    FUEL,
    COOLANT,
    OIL,
    VACUUM,
    AIR,
    EXHAUST,
    BRAKE,
    HYDRAULIC,
    MECHANICAL
}

data class RoutePoint(
    val x: Float,
    val y: Float
)

data class Connection(
    val id: String,
    val fromComponentId: String,
    val toComponentId: String,
    val type: ConnectionType,
    val label: String = "",
    val route: List<RoutePoint> = emptyList()
)
''')

write_file('app/src/main/java/com/autotechnicalatlas/data/model/System.kt', r'''
package com.autotechnicalatlas.data.model

data class VehicleSystem(
    val id: String,
    val name: String,
    val description: String,
    val order: Int = 0
)
''')

write_file('app/src/main/java/com/autotechnicalatlas/data/model/Source.kt', r'''
package com.autotechnicalatlas.data.model

data class TechnicalSource(
    val id: String,
    val manufacturer: String,
    val document: String,
    val schemeNumber: String? = null,
    val year: Int? = null,
    val configuration: String? = null,
    val verified: Boolean = false,
    val note: String = ""
)
''')

# ---------- Repository ----------
write_file('app/src/main/java/com/autotechnicalatlas/data/repository/AtlasRepository.kt', r'''
package com.autotechnicalatlas.data.repository

import android.content.Context
import android.net.Uri
import com.autotechnicalatlas.data.model.Component
import com.autotechnicalatlas.data.model.Connection
import com.autotechnicalatlas.data.model.ConnectionType
import com.autotechnicalatlas.data.model.RoutePoint
import com.autotechnicalatlas.data.model.TechnicalSource
import com.autotechnicalatlas.data.model.Vehicle
import com.autotechnicalatlas.data.model.VehicleSystem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AtlasRepository(private val context: Context) {

    private val importedDir: File = File(context.filesDir, "vehicles").also { it.mkdirs() }

    fun loadVehicles(): List<Vehicle> {
        val result = linkedMapOf<String, Vehicle>()

        val assetFiles = context.assets.list("vehicles").orEmpty()
            .filter { it.endsWith(".json", ignoreCase = true) }

        assetFiles.forEach { fileName ->
            runCatching {
                val json = context.assets.open("vehicles/$fileName")
                    .bufferedReader()
                    .use { it.readText() }
                parseVehicle(json)
            }.onSuccess { vehicle -> result[vehicle.id] = vehicle }
        }

        importedDir.listFiles()
            .orEmpty()
            .filter { it.extension.equals("json", ignoreCase = true) }
            .forEach { file ->
                runCatching { parseVehicle(file.readText()) }
                    .onSuccess { vehicle -> result[vehicle.id] = vehicle }
            }

        return result.values.sortedWith(
            compareBy<Vehicle> { it.manufacturer }
                .thenBy { it.model }
                .thenByDescending { it.year }
        )
    }

    fun importVehicle(uri: Uri): Vehicle {
        val json = context.contentResolver.openInputStream(uri)?.use {
            it.bufferedReader().use { reader -> reader.readText() }
        } ?: error("Не удалось открыть JSON")

        val vehicle = parseVehicle(json)
        val safeId = vehicle.id.replace(Regex("[^A-Za-z0-9_.-]"), "_")
        File(importedDir, "$safeId.json").writeText(json)
        return vehicle
    }

    private fun parseVehicle(json: String): Vehicle {
        val root = JSONObject(json)

        val systems = mutableListOf<VehicleSystem>()
        val systemsJson = root.optJSONArray("systems") ?: JSONArray()
        for (i in 0 until systemsJson.length()) {
            val item = systemsJson.getJSONObject(i)
            systems += VehicleSystem(
                id = item.optString("id"),
                name = item.optString("name"),
                description = item.optString("description"),
                order = item.optInt("order", i)
            )
        }

        val components = mutableListOf<Component>()
        val componentsJson = root.optJSONArray("components") ?: JSONArray()
        for (i in 0 until componentsJson.length()) {
            val item = componentsJson.getJSONObject(i)
            components += Component(
                id = item.optString("id"),
                name = item.optString("name"),
                systemId = item.optString("systemId"),
                description = item.optString("description"),
                shape = item.optString("shape", "block"),
                x = item.optDouble("x", 0.0).toFloat(),
                y = item.optDouble("y", 0.0).toFloat(),
                width = item.optDouble("width", 120.0).toFloat(),
                height = item.optDouble("height", 70.0).toFloat(),
                layer = item.optInt("layer", 0),
                verified = item.optBoolean("verified", false),
                sourceId = item.optString("sourceId").takeIf { it.isNotBlank() }
            )
        }

        val connections = mutableListOf<Connection>()
        val connectionsJson = root.optJSONArray("connections") ?: JSONArray()
        for (i in 0 until connectionsJson.length()) {
            val item = connectionsJson.getJSONObject(i)
            val route = mutableListOf<RoutePoint>()
            val routeJson = item.optJSONArray("route") ?: JSONArray()
            for (j in 0 until routeJson.length()) {
                val point = routeJson.getJSONObject(j)
                route += RoutePoint(
                    x = point.optDouble("x", 0.0).toFloat(),
                    y = point.optDouble("y", 0.0).toFloat()
                )
            }

            val type = runCatching {
                ConnectionType.valueOf(item.optString("type"))
            }.getOrDefault(ConnectionType.MECHANICAL)

            connections += Connection(
                id = item.optString("id"),
                fromComponentId = item.optString("fromComponentId"),
                toComponentId = item.optString("toComponentId"),
                type = type,
                label = item.optString("label"),
                route = route
            )
        }

        val sources = mutableListOf<TechnicalSource>()
        val sourcesJson = root.optJSONArray("sources") ?: JSONArray()
        for (i in 0 until sourcesJson.length()) {
            val item = sourcesJson.getJSONObject(i)
            sources += TechnicalSource(
                id = item.optString("id"),
                manufacturer = item.optString("manufacturer"),
                document = item.optString("document"),
                schemeNumber = item.optString("schemeNumber").takeIf { it.isNotBlank() },
                year = if (item.has("year")) item.optInt("year") else null,
                configuration = item.optString("configuration").takeIf { it.isNotBlank() },
                verified = item.optBoolean("verified", false),
                note = item.optString("note")
            )
        }

        return Vehicle(
            id = root.optString("id"),
            manufacturer = root.optString("manufacturer"),
            model = root.optString("model"),
            generation = root.optString("generation"),
            year = root.optInt("year"),
            modification = root.optString("modification"),
            engine = root.optString("engine"),
            transmission = root.optString("transmission"),
            drivetrain = root.optString("drivetrain"),
            description = root.optString("description"),
            systems = systems.sortedBy { it.order },
            components = components,
            connections = connections,
            sources = sources
        )
    }
}
''')

# ---------- Viewer ----------
write_file('app/src/main/java/com/autotechnicalatlas/viewer/TechnicalViewer.kt', r'''
package com.autotechnicalatlas.viewer

data class ViewerState(
    val rotation: Float = 0f,
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val exploded: Float = 0f
)
''')

write_file('app/src/main/java/com/autotechnicalatlas/ui/theme/Theme.kt', r'''
package com.autotechnicalatlas.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AtlasColors = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = Color(0xFF555555),
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = Color(0xFF333333),
    outline = Color(0xFFBDBDBD)
)

@Composable
fun AtlasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AtlasColors,
        typography = Typography(),
        content = content
    )
}
''')

# ---------- Canvas ----------
write_file('app/src/main/java/com/autotechnicalatlas/ui/components/TechnicalCanvas.kt', r'''
package com.autotechnicalatlas.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.autotechnicalatlas.data.model.Component
import com.autotechnicalatlas.data.model.Connection
import com.autotechnicalatlas.data.model.ConnectionType
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TechnicalCanvas(
    components: List<Component>,
    connections: List<Connection>,
    selectedComponentId: String?,
    exploded: Float,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var zoom by remember { mutableFloatStateOf(0.72f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    fun explodeOffset(component: Component): Offset {
        val length = kotlin.math.sqrt(component.x * component.x + component.y * component.y)
        if (length < 1f) return Offset.Zero
        val amount = exploded * 85f
        return Offset(component.x / length * amount, component.y / length * amount)
    }

    fun centerOf(component: Component): Offset = Offset(
        component.x + explodeOffset(component).x,
        component.y + explodeOffset(component).y
    )

    fun screenToWorld(point: Offset, size: Size): Offset {
        val dx = (point.x - size.width / 2f - pan.x) / zoom
        val dy = (point.y - size.height / 2f - pan.y) / zoom
        val angle = Math.toRadians((-rotation).toDouble())
        return Offset(
            (dx * cos(angle) - dy * sin(angle)).toFloat(),
            (dx * sin(angle) + dy * cos(angle)).toFloat()
        )
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(components, zoom, rotation, pan, exploded) {
                    detectTransformGestures { _, panChange, zoomChange, rotationChange ->
                        pan += panChange
                        zoom = (zoom * zoomChange).coerceIn(0.25f, 4.5f)
                        rotation += rotationChange
                    }
                }
                .pointerInput(components, zoom, rotation, pan, exploded) {
                    detectTapGestures { tap ->
                        val world = screenToWorld(tap, size)
                        val hit = components.asReversed().firstOrNull { component ->
                            val c = centerOf(component)
                            world.x in (c.x - component.width / 2f)..(c.x + component.width / 2f) &&
                                world.y in (c.y - component.height / 2f)..(c.y + component.height / 2f)
                        }
                        onSelect(hit?.id)
                    }
                }
        ) {
            val canvasCenter = Offset(size.width / 2f, size.height / 2f)

            translate(canvasCenter.x + pan.x, canvasCenter.y + pan.y) {
                rotate(rotation) {
                    scale(zoom, zoom) {
                        drawTechnicalGrid()

                        val componentMap = components.associateBy { it.id }
                        connections.forEach { connection ->
                            val from = componentMap[connection.fromComponentId] ?: return@forEach
                            val to = componentMap[connection.toComponentId] ?: return@forEach
                            drawConnection(
                                connection = connection,
                                from = centerOf(from),
                                to = centerOf(to),
                                selected = selectedComponentId == from.id || selectedComponentId == to.id
                            )
                        }

                        components.sortedBy { it.layer }.forEach { component ->
                            drawComponent(
                                component = component,
                                center = centerOf(component),
                                selected = component.id == selectedComponentId
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTechnicalGrid() {
    val grid = 50f
    for (i in -1200..1200 step 50) {
        drawLine(
            Color(0xFFE8E8E8),
            Offset(i.toFloat(), -900f),
            Offset(i.toFloat(), 900f),
            1f
        )
        drawLine(
            Color(0xFFE8E8E8),
            Offset(-1200f, i.toFloat()),
            Offset(1200f, i.toFloat()),
            1f
        )
    }

    drawLine(Color(0xFF9A9A9A), Offset(-1200f, 0f), Offset(1200f, 0f), 1.2f)
    drawLine(Color(0xFF9A9A9A), Offset(0f, -900f), Offset(0f, 900f), 1.2f)

    drawCircle(Color(0xFF777777), radius = 3f, center = Offset.Zero)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawComponent(
    component: Component,
    center: Offset,
    selected: Boolean
) {
    val left = center.x - component.width / 2f
    val top = center.y - component.height / 2f
    val outline = if (selected) Color.Black else Color(0xFF444444)
    val fill = if (selected) Color(0xFFEAEAEA) else Color.White
    val stroke = if (selected) 3f else 1.8f

    when (component.shape) {
        "circle" -> {
            drawCircle(fill, component.width.coerceAtMost(component.height) / 2f, center)
            drawCircle(outline, component.width.coerceAtMost(component.height) / 2f, center, style = Stroke(stroke))
        }
        else -> {
            drawRect(fill, androidx.compose.ui.geometry.Offset(left, top), Size(component.width, component.height))
            drawRect(
                outline,
                androidx.compose.ui.geometry.Offset(left, top),
                Size(component.width, component.height),
                style = Stroke(width = stroke, join = StrokeJoin.Round)
            )
        }
    }

    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = if (selected) 25f else 21f
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)
    }

    drawContext.canvas.nativeCanvas.drawText(
        component.name,
        left,
        top - 10f,
        paint
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConnection(
    connection: Connection,
    from: Offset,
    to: Offset,
    selected: Boolean
) {
    val path = Path().apply {
        if (connection.route.isNotEmpty()) {
            moveTo(connection.route.first().x, connection.route.first().y)
            connection.route.drop(1).forEach { point ->
                lineTo(point.x, point.y)
            }
        } else {
            moveTo(from.x, from.y)
            lineTo(to.x, to.y)
        }
    }

    val dash = when (connection.type) {
        ConnectionType.ELECTRICAL, ConnectionType.CAN, ConnectionType.LIN ->
            PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f)
        ConnectionType.VACUUM, ConnectionType.AIR ->
            PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        else -> null
    }

    drawPath(
        path = path,
        color = if (selected) Color.Black else Color(0xFF6A6A6A),
        style = Stroke(
            width = if (selected) 4f else 2f,
            pathEffect = dash,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    drawCircle(Color(0xFF555555), 4f, from)
    drawCircle(Color(0xFF555555), 4f, to)
}
''')

# ---------- Home screen ----------
write_file('app/src/main/java/com/autotechnicalatlas/ui/screens/HomeScreen.kt', r'''
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
''')

# ---------- Viewer screen ----------
write_file('app/src/main/java/com/autotechnicalatlas/ui/screens/ViewerScreen.kt', r'''
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
''')

# ---------- Main ----------
write_file('app/src/main/java/com/autotechnicalatlas/MainActivity.kt', r'''
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
''')

# ---------- Demo data package ----------
write_file('app/src/main/assets/vehicles/bmw_f10_m5_s63_atlas_package.json', r'''
{
  "id": "demo_bmw_f10_m5_s63",
  "manufacturer": "BMW",
  "model": "5 Series",
  "generation": "F10",
  "year": 2017,
  "modification": "M5",
  "engine": "S63",
  "transmission": "7-speed M-DCT",
  "drivetrain": "RWD",
  "description": "Инженерный пакет для проверки интерфейса. Геометрия и маршруты ниже являются НЕПРОВЕРЕННЫМ демонстрационным набором, а не заводской документацией.",
  "systems": [
    {"id":"powertrain","name":"Двигатель/КПП","description":"Силовой агрегат","order":1},
    {"id":"electrical","name":"Электрика","description":"Питание, ECU, шины","order":2},
    {"id":"cooling","name":"Охлаждение","description":"Радиаторы и контуры","order":3},
    {"id":"fuel","name":"Топливо","description":"Бак и топливный контур","order":4},
    {"id":"brakes","name":"Тормоза","description":"Тормозная система и ABS/DSC","order":5},
    {"id":"chassis","name":"Шасси","description":"Рулевое и ходовая часть","order":6},
    {"id":"climate","name":"Климат","description":"HVAC","order":7}
  ],
  "components": [
    {"id":"engine","name":"S63 engine","systemId":"powertrain","description":"Двигатель / основной силовой блок","shape":"block","x":-120,"y":-20,"width":260,"height":130,"layer":1,"verified":false,"sourceId":"demo_source"},
    {"id":"dct","name":"M-DCT","systemId":"powertrain","description":"Корпус трансмиссии","shape":"block","x":250,"y":20,"width":190,"height":100,"layer":1,"verified":false,"sourceId":"demo_source"},
    {"id":"dme","name":"DME / ECU","systemId":"electrical","description":"Узел управления двигателем","shape":"block","x":-300,"y":-160,"width":160,"height":70,"layer":2,"verified":false,"sourceId":"demo_source"},
    {"id":"battery","name":"Battery","systemId":"electrical","description":"Источник питания","shape":"block","x":-420,"y":220,"width":150,"height":80,"layer":2,"verified":false,"sourceId":"demo_source"},
    {"id":"fusebox","name":"Fuse / relay box","systemId":"electrical","description":"Распределение питания","shape":"block","x":340,"y":230,"width":170,"height":80,"layer":2,"verified":false,"sourceId":"demo_source"},
    {"id":"radiator","name":"Radiator module","systemId":"cooling","description":"Передний теплообменник","shape":"block","x":-40,"y":-300,"width":330,"height":70,"layer":0,"verified":false,"sourceId":"demo_source"},
    {"id":"fuel","name":"Fuel tank","systemId":"fuel","description":"Топливный модуль","shape":"block","x":470,"y":300,"width":190,"height":90,"layer":0,"verified":false,"sourceId":"demo_source"},
    {"id":"abs","name":"ABS / DSC","systemId":"brakes","description":"Гидроблок и электронный блок","shape":"block","x":120,"y":300,"width":170,"height":80,"layer":2,"verified":false,"sourceId":"demo_source"},
    {"id":"steering","name":"Steering rack","systemId":"chassis","description":"Рулевой механизм","shape":"block","x":-430,"y":40,"width":180,"height":65,"layer":0,"verified":false,"sourceId":"demo_source"},
    {"id":"hvac","name":"HVAC unit","systemId":"climate","description":"Климатический модуль","shape":"block","x":-20,"y":400,"width":190,"height":75,"layer":0,"verified":false,"sourceId":"demo_source"}
  ],
  "connections": [
    {"id":"c1","fromComponentId":"engine","toComponentId":"dct","type":"MECHANICAL","label":"mechanical","route":[{"x":40,"y":40},{"x":140,"y":35},{"x":250,"y":20}]},
    {"id":"c2","fromComponentId":"dme","toComponentId":"engine","type":"CAN","label":"control bus","route":[{"x":-220,"y":-125},{"x":-180,"y":-80},{"x":-100,"y":-40}]},
    {"id":"c3","fromComponentId":"battery","toComponentId":"fusebox","type":"ELECTRICAL","label":"power distribution","route":[{"x":-350,"y":220},{"x":-80,"y":250},{"x":190,"y":250},{"x":340,"y":230}]},
    {"id":"c4","fromComponentId":"fusebox","toComponentId":"dme","type":"ELECTRICAL","label":"ECU supply","route":[{"x":340,"y":230},{"x":20,"y":170},{"x":-300,"y":-160}]},
    {"id":"c5","fromComponentId":"radiator","toComponentId":"engine","type":"COOLANT","label":"coolant","route":[{"x":-40,"y":-265},{"x":-20,"y":-170},{"x":-60,"y":-70}]},
    {"id":"c6","fromComponentId":"engine","toComponentId":"fuel","type":"FUEL","label":"fuel path","route":[{"x":60,"y":40},{"x":250,"y":120},{"x":470,"y":300}]},
    {"id":"c7","fromComponentId":"abs","toComponentId":"steering","type":"ELECTRICAL","label":"control","route":[{"x":120,"y":300},{"x":-110,"y":260},{"x":-430,"y":40}]},
    {"id":"c8","fromComponentId":"abs","toComponentId":"dme","type":"CAN","label":"vehicle bus","route":[{"x":120,"y":300},{"x":-50,"y":40},{"x":-300,"y":-160}]},
    {"id":"c9","fromComponentId":"hvac","toComponentId":"fusebox","type":"ELECTRICAL","label":"HVAC power","route":[{"x":-20,"y":400},{"x":170,"y":350},{"x":340,"y":230}]},
    {"id":"c10","fromComponentId":"battery","toComponentId":"dme","type":"ELECTRICAL","label":"supply","route":[{"x":-420,"y":220},{"x":-360,"y":0},{"x":-300,"y":-160}]}
  ],
  "sources": [
    {"id":"demo_source","manufacturer":"NOT VERIFIED","document":"Atlas interface demo dataset","schemeNumber":"DEMO-01","year":2017,"configuration":"F10 M5 S63","verified":false,"note":"Replace this package with verified manufacturer or service-document data before treating routes as authoritative."}
  ]
}
''')

# ---------- README ----------
write_file('README.md', r'''
# Auto Technical Atlas

Personal offline Android technical atlas for vehicle configurations.

## Current application layer

- vehicle selector;
- offline JSON vehicle packages;
- JSON import from Android file picker;
- professional monochrome engineering viewer;
- zoom / pan / rotation gestures;
- exploded-view slider;
- system isolation;
- component search;
- component selection;
- connection tracing/highlighting;
- source and verification metadata;
- GitHub Actions Release APK publishing.

## Data architecture

Vehicle
→ model
→ generation
→ year
→ modification
→ engine
→ transmission
→ drivetrain
→ systems
→ components
→ connections
→ sources

Each exact configuration is a separate dataset. The app does not assume that two engines, gearboxes or model years share the same technical routing.

## JSON package

A vehicle package contains:

- `systems` — technical systems;
- `components` — physical parts/nodes and drawing coordinates;
- `connections` — wires, CAN/LIN, hoses, fuel/oil/coolant, brake and mechanical links;
- `sources` — origin, scheme number, configuration and verification state.

The built-in BMW package is deliberately marked `verified: false`. It exists to test the application engine and must not be treated as factory documentation.

## Next data stage

Verified packages can be imported through `ИМПОРТ JSON` without changing the application code.
''')

# ---------- Remove duplicate debug workflow ----------
debug_workflow = PROJECT / '.github/workflows/build.yml'
if debug_workflow.exists():
    subprocess.run(['git', 'rm', '-f', '.github/workflows/build.yml'], cwd=PROJECT, check=False)

# ---------- Git ----------
os.chdir(PROJECT)
subprocess.run(['git', 'add', '.'], check=True)

commit = subprocess.run(
    ['git', 'commit', '-m', 'Build interactive Auto Technical Atlas foundation'],
    text=True
)

if commit.returncode != 0:
    print('Commit не создан: возможно, изменений нет.')

push = subprocess.run(['git', 'push', 'origin', 'main'], text=True)
if push.returncode != 0:
    print('Первый push не прошёл. Пробую rebase с origin/main...')
    pull = subprocess.run(['git', 'pull', '--rebase', 'origin', 'main'], text=True)
    if pull.returncode != 0:
        print('Rebase не выполнен. Разреши конфликт в Git и запусти скрипт снова.')
        sys.exit(1)
    subprocess.run(['git', 'push', 'origin', 'main'], check=True)

print('\n' + '=' * 65)
print('AUTO TECHNICAL ATLAS V0.2 ОТПРАВЛЕН')
print('=' * 65)
print('GitHub Actions должен автоматически собрать Release APK.')
print('Проверка: gh run list --repo kirikkirikon220-svg/AutoTechnicalAtlas')
print('Релизы:  gh release list --repo kirikkirikon220-svg/AutoTechnicalAtlas')
