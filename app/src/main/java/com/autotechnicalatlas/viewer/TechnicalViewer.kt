package com.autotechnicalatlas.viewer

data class ViewerState(
    val rotation: Float = 0f,
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val exploded: Float = 0f
)
