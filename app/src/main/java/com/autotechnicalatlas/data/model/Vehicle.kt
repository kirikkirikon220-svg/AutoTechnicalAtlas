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
