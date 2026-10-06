package com.autotechnicalatlas.data.model

data class Component(
    val id: String,
    val name: String,
    val systemId: String,
    val description: String,
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val drawingId: String? = null
)
