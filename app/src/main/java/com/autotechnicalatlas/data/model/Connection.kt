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
