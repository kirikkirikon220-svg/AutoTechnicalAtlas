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
import com.autotechnicalatlas.data.model.VehicleVisual
import com.autotechnicalatlas.data.model.VisualEdge
import com.autotechnicalatlas.data.model.VisualPoint
import com.autotechnicalatlas.data.model.VisualWheel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AtlasRepository(
    private val context: Context
) {

    private val importedDir =
        File(context.filesDir, "vehicles").also { it.mkdirs() }

    fun loadVehicles(): List<Vehicle> {
        val result = linkedMapOf<String, Vehicle>()

        val assets = context.assets.list("vehicles")
            .orEmpty()
            .filter {
                it.endsWith(
                    ".json",
                    ignoreCase = true
                )
            }

        assets.forEach { fileName ->
            runCatching {
                val json =
                    context.assets
                        .open("vehicles/$fileName")
                        .bufferedReader()
                        .use { it.readText() }

                parseVehicle(json)
            }.onSuccess { vehicle ->
                result[vehicle.id] = vehicle
            }
        }

        importedDir.listFiles()
            .orEmpty()
            .filter {
                it.extension.equals(
                    "json",
                    ignoreCase = true
                )
            }
            .forEach { file ->
                runCatching {
                    parseVehicle(file.readText())
                }.onSuccess { vehicle ->
                    result[vehicle.id] = vehicle
                }
            }

        return result.values.sortedWith(
            compareBy<Vehicle> {
                it.manufacturer
            }.thenBy {
                it.model
            }.thenByDescending {
                it.year
            }
        )
    }

    fun importVehicle(uri: Uri): Vehicle {
        val json =
            context.contentResolver
                .openInputStream(uri)
                ?.use {
                    it.bufferedReader().use { reader ->
                        reader.readText()
                    }
                }
                ?: error("Не удалось открыть JSON")

        val vehicle = parseVehicle(json)

        val safeId =
            vehicle.id.replace(
                Regex("[^A-Za-z0-9_.-]"),
                "_"
            )

        File(
            importedDir,
            "$safeId.json"
        ).writeText(json)

        return vehicle
    }

    private fun parseVehicle(
        json: String
    ): Vehicle {
        val root = JSONObject(json)

        val systems = mutableListOf<VehicleSystem>()

        val systemsJson =
            root.optJSONArray("systems")
                ?: JSONArray()

        for (i in 0 until systemsJson.length()) {
            val item =
                systemsJson.getJSONObject(i)

            systems += VehicleSystem(
                id = item.optString("id"),
                name = item.optString("name"),
                description = item.optString(
                    "description"
                ),
                order = item.optInt(
                    "order",
                    i
                )
            )
        }

        val components =
            mutableListOf<Component>()

        val componentsJson =
            root.optJSONArray("components")
                ?: JSONArray()

        for (i in 0 until componentsJson.length()) {
            val item =
                componentsJson.getJSONObject(i)

            components += Component(
                id = item.optString("id"),
                name = item.optString("name"),
                systemId = item.optString("systemId"),
                description = item.optString(
                    "description"
                ),
                shape = item.optString(
                    "shape",
                    "node"
                ),
                x = item.optDouble(
                    "x",
                    0.0
                ).toFloat(),
                y = item.optDouble(
                    "y",
                    0.0
                ).toFloat(),
                z = item.optDouble(
                    "z",
                    0.0
                ).toFloat(),
                width = item.optDouble(
                    "width",
                    120.0
                ).toFloat(),
                height = item.optDouble(
                    "height",
                    70.0
                ).toFloat(),
                layer = item.optInt(
                    "layer",
                    0
                ),
                verified = item.optBoolean(
                    "verified",
                    false
                ),
                sourceId = item
                    .optString("sourceId")
                    .takeIf { it.isNotBlank() }
            )
        }

        val connections =
            mutableListOf<Connection>()

        val connectionsJson =
            root.optJSONArray("connections")
                ?: JSONArray()

        for (i in 0 until connectionsJson.length()) {
            val item =
                connectionsJson.getJSONObject(i)

            val route =
                mutableListOf<RoutePoint>()

            val routeJson =
                item.optJSONArray("route")
                    ?: JSONArray()

            for (j in 0 until routeJson.length()) {
                val point =
                    routeJson.getJSONObject(j)

                route += RoutePoint(
                    x = point.optDouble(
                        "x",
                        0.0
                    ).toFloat(),
                    y = point.optDouble(
                        "y",
                        0.0
                    ).toFloat(),
                    z = point.optDouble(
                        "z",
                        0.0
                    ).toFloat()
                )
            }

            val type =
                runCatching {
                    ConnectionType.valueOf(
                        item.optString("type")
                    )
                }.getOrDefault(
                    ConnectionType.MECHANICAL
                )

            connections += Connection(
                id = item.optString("id"),
                fromComponentId =
                    item.optString(
                        "fromComponentId"
                    ),
                toComponentId =
                    item.optString(
                        "toComponentId"
                    ),
                type = type,
                label = item.optString(
                    "label"
                ),
                purpose = item.optString(
                    "purpose"
                ),
                direction = item.optString(
                    "direction"
                ),
                route = route
            )
        }

        val sources =
            mutableListOf<TechnicalSource>()

        val sourcesJson =
            root.optJSONArray("sources")
                ?: JSONArray()

        for (i in 0 until sourcesJson.length()) {
            val item =
                sourcesJson.getJSONObject(i)

            sources += TechnicalSource(
                id = item.optString("id"),
                manufacturer =
                    item.optString(
                        "manufacturer"
                    ),
                document =
                    item.optString(
                        "document"
                    ),
                schemeNumber =
                    item.optString(
                        "schemeNumber"
                    ).takeIf {
                        it.isNotBlank()
                    },
                year =
                    if (item.has("year")) {
                        item.optInt("year")
                    } else {
                        null
                    },
                configuration =
                    item.optString(
                        "configuration"
                    ).takeIf {
                        it.isNotBlank()
                    },
                verified =
                    item.optBoolean(
                        "verified",
                        false
                    ),
                note =
                    item.optString("note")
            )
        }

        val visual =
            parseVisual(
                root.optJSONObject(
                    "visual"
                )
            )

        return Vehicle(
            id = root.optString("id"),
            manufacturer =
                root.optString(
                    "manufacturer"
                ),
            model =
                root.optString("model"),
            generation =
                root.optString(
                    "generation"
                ),
            year =
                root.optInt("year"),
            modification =
                root.optString(
                    "modification"
                ),
            engine =
                root.optString("engine"),
            transmission =
                root.optString(
                    "transmission"
                ),
            drivetrain =
                root.optString(
                    "drivetrain"
                ),
            description =
                root.optString(
                    "description"
                ),
            systems =
                systems.sortedBy {
                    it.order
                },
            components = components,
            connections = connections,
            sources = sources,
            visual = visual
        )
    }

    private fun parseVisual(
        json: JSONObject?
    ): VehicleVisual {
        if (json == null) {
            return VehicleVisual.defaultSedan()
        }

        val points =
            mutableListOf<VisualPoint>()

        val pointsJson =
            json.optJSONArray("points")
                ?: JSONArray()

        for (i in 0 until pointsJson.length()) {
            val p =
                pointsJson.getJSONObject(i)

            points += VisualPoint(
                x = p.optDouble(
                    "x",
                    0.0
                ).toFloat(),
                y = p.optDouble(
                    "y",
                    0.0
                ).toFloat(),
                z = p.optDouble(
                    "z",
                    0.0
                ).toFloat()
            )
        }

        val edges =
            mutableListOf<VisualEdge>()

        val edgesJson =
            json.optJSONArray("edges")
                ?: JSONArray()

        for (i in 0 until edgesJson.length()) {
            val e =
                edgesJson.getJSONObject(i)

            edges += VisualEdge(
                from = e.optInt(
                    "from"
                ),
                to = e.optInt(
                    "to"
                ),
                group = e.optString(
                    "group",
                    "BODY"
                )
            )
        }

        val wheels =
            mutableListOf<VisualWheel>()

        val wheelsJson =
            json.optJSONArray("wheels")
                ?: JSONArray()

        for (i in 0 until wheelsJson.length()) {
            val w =
                wheelsJson.getJSONObject(i)

            wheels += VisualWheel(
                x = w.optDouble(
                    "x",
                    0.0
                ).toFloat(),
                y = w.optDouble(
                    "y",
                    0.0
                ).toFloat(),
                z = w.optDouble(
                    "z",
                    0.0
                ).toFloat(),
                radius = w.optDouble(
                    "radius",
                    300.0
                ).toFloat()
            )
        }

        if (
            points.isEmpty() ||
            edges.isEmpty()
        ) {
            return VehicleVisual.defaultSedan()
        }

        return VehicleVisual(
            points = points,
            edges = edges,
            wheels = wheels
        )
    }
}
