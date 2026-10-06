package com.autotechnicalatlas.data.model

data class Component(
    val id: String,
    val name: String,
    val systemId: String,
    val description: String,
    val shape: String = "node",
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val width: Float = 120f,
    val height: Float = 70f,
    val layer: Int = 0,
    val verified: Boolean = false,
    val sourceId: String? = null
)
