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

    fun screenToWorld(
        point: Offset,
        width: Float,
        height: Float
    ): Offset {
        val dx = (point.x - width / 2f - pan.x) / zoom
        val dy = (point.y - height / 2f - pan.y) / zoom
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
                        val world = screenToWorld(
                            tap,
                            size.width.toFloat(),
                            size.height.toFloat()
                        )
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
