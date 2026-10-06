package com.autotechnicalatlas.viewer

data class ViewerState(
    val rotationX: Float = 0f,
    val rotationY: Float = 0f,
    val zoom: Float = 1f,
    val exploded: Float = 0f
)

class TechnicalViewer {

    private var state = ViewerState()

    fun rotate(dx: Float, dy: Float) {
        state = state.copy(
            rotationX = state.rotationX + dx,
            rotationY = state.rotationY + dy
        )
    }

    fun zoom(scale: Float) {
        state = state.copy(
            zoom = (state.zoom * scale).coerceIn(0.2f, 10f)
        )
    }

    fun setExploded(value: Float) {
        state = state.copy(
            exploded = value.coerceIn(0f, 1f)
        )
    }

    fun state(): ViewerState = state
}
