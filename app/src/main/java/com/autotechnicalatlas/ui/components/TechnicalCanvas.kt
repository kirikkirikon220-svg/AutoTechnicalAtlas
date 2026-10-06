package com.autotechnicalatlas.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.autotechnicalatlas.data.model.Component
import com.autotechnicalatlas.data.model.Connection
import com.autotechnicalatlas.data.model.ConnectionType
import com.autotechnicalatlas.data.model.VehicleVisual
import com.autotechnicalatlas.data.model.VisualPoint
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private data class Projection(
    val point: Offset,
    val depth: Float
)

@Composable
fun TechnicalCanvas(
    visual: VehicleVisual,
    components: List<Component>,
    connections: List<Connection>,
    selectedComponentId: String?,
    activeSystemId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var angle by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(0.82f) }
    var verticalPan by remember { mutableFloatStateOf(0f) }

    val selectedConnections =
        remember(
            selectedComponentId,
            connections
        ) {
            if (selectedComponentId == null) {
                emptySet()
            } else {
                connections
                    .filter {
                        it.fromComponentId ==
                            selectedComponentId ||
                            it.toComponentId ==
                            selectedComponentId
                    }
                    .map { it.id }
                    .toSet()
            }
        }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures {
                        _,
                        panChange,
                        zoomChange,
                        rotationChange ->

                        angle +=
                            panChange.x * 0.45f +
                                rotationChange * 0.8f

                        zoom =
                            (
                                zoom *
                                    zoomChange
                            ).coerceIn(
                                0.45f,
                                2.7f
                            )

                        verticalPan =
                            (
                                verticalPan +
                                    panChange.y * 0.20f
                            ).coerceIn(
                                -350f,
                                350f
                            )
                    }
                }
                .pointerInput(
                    visual,
                    components,
                    connections,
                    angle,
                    zoom,
                    verticalPan
                ) {
                    detectTapGestures { tap ->
                        val canvasWidth =
                            size.width.toFloat()

                        val canvasHeight =
                            size.height.toFloat()

                        val projected =
                            components
                                .mapNotNull { component ->
                                    val p =
                                        project(
                                            VisualPoint(
                                                component.x,
                                                component.y,
                                                component.z
                                            ),
                                            angle,
                                            zoom,
                                            canvasWidth,
                                            canvasHeight,
                                            verticalPan
                                        )

                                    if (
                                        hypot(
                                            p.point.x - tap.x,
                                            p.point.y - tap.y
                                        ) < 48f
                                    ) {
                                        component to p
                                    } else {
                                        null
                                    }
                                }
                                .minByOrNull {
                                    it.second.depth
                                }

                        onSelect(
                            projected
                                ?.first
                                ?.id
                        )
                    }
                }
        ) {
            drawBackgroundGrid()

            val componentMap =
                components.associateBy {
                    it.id
                }

            // BODY
            drawVehicleWireframe(
                visual = visual,
                angle = angle,
                zoom = zoom,
                verticalPan = verticalPan
            )

            // CONNECTIONS
            connections.forEach { connection ->
                val from =
                    componentMap[
                        connection.fromComponentId
                    ]

                val to =
                    componentMap[
                        connection.toComponentId
                    ]

                if (
                    from == null ||
                    to == null
                ) {
                    return@forEach
                }

                val selected =
                    selectedComponentId == null ||
                        selectedConnections.contains(
                            connection.id
                        )

                val systemMatch =
                    activeSystemId == null ||
                        from.systemId ==
                            activeSystemId ||
                        to.systemId ==
                            activeSystemId

                val alpha =
                    when {
                        selectedComponentId != null &&
                            !selected ->
                            0.08f

                        activeSystemId != null &&
                            !systemMatch ->
                            0.10f

                        else ->
                            0.95f
                    }

                drawConnection3D(
                    connection = connection,
                    from = from,
                    to = to,
                    angle = angle,
                    zoom = zoom,
                    verticalPan = verticalPan,
                    alpha = alpha,
                    selected = selected
                )
            }

            // COMPONENT NODES
            components.forEach { component ->
                val systemMatch =
                    activeSystemId == null ||
                        component.systemId ==
                            activeSystemId

                val selected =
                    component.id ==
                        selectedComponentId

                val connected =
                    selectedConnections.any {
                        connectionId ->
                        connections.any {
                            it.id ==
                                connectionId &&
                                (
                                    it.fromComponentId ==
                                        component.id ||
                                    it.toComponentId ==
                                        component.id
                                )
                        }
                    }

                val alpha =
                    when {
                        selected -> 1f
                        selectedComponentId != null &&
                            !connected -> 0.22f
                        activeSystemId != null &&
                            !systemMatch -> 0.18f
                        else -> 1f
                    }

                drawComponentNode(
                    component = component,
                    angle = angle,
                    zoom = zoom,
                    verticalPan = verticalPan,
                    canvasWidth = size.width.toFloat(),
                    canvasHeight = size.height.toFloat(),
                    selected = selected,
                    alpha = alpha
                )
            }

            selectedComponentId?.let {
                id ->
                componentMap[id]?.let {
                    drawCallout(
                        component = it,
                        angle = angle,
                        zoom = zoom,
                        verticalPan = verticalPan,
                        canvasWidth = size.width.toFloat(),
                        canvasHeight = size.height.toFloat()
                    )
                }
            }

            drawOrientationIndicator(
                angle = angle,
                canvasWidth = size.width.toFloat(),
                canvasHeight = size.height.toFloat()
            )
        }
    }
}

private fun project(
    point: VisualPoint,
    angle: Float,
    zoom: Float,
    width: Float,
    height: Float,
    verticalPan: Float
): Projection {
    val rad =
        Math.toRadians(
            angle.toDouble()
        )

    val c =
        cos(rad).toFloat()

    val s =
        sin(rad).toFloat()

    // Y-axis is vertical.
    // X/Z rotate around vehicle vertical axis.
    val rotatedX =
        point.x * c +
            point.z * s

    val depth =
        -point.x * s +
            point.z * c

    val perspective =
        1f /
            (
                1f +
                    depth / 7000f
                )

    val scale =
        min(
            width / 5400f,
            height / 2800f
        ) * zoom

    return Projection(
        point = Offset(
            width / 2f +
                rotatedX *
                scale *
                perspective,

            height / 2f +
                point.y *
                scale *
                perspective +
                verticalPan
        ),
        depth = depth
    )
}

private fun DrawScope.drawBackgroundGrid() {
    val minor =
        Color(0xFFF0F2F3)

    val major =
        Color(0xFFD8DEE2)

    for (x in -2000..2000 step 80) {
        drawLine(
            minor,
            Offset(x.toFloat(), -1600f),
            Offset(x.toFloat(), 1600f),
            1f
        )
    }

    for (y in -1600..1600 step 80) {
        drawLine(
            minor,
            Offset(-2200f, y.toFloat()),
            Offset(2200f, y.toFloat()),
            1f
        )
    }

    for (x in -2000..2000 step 400) {
        drawLine(
            major,
            Offset(x.toFloat(), -1600f),
            Offset(x.toFloat(), 1600f),
            1.2f
        )
    }

    for (y in -1600..1600 step 400) {
        drawLine(
            major,
            Offset(-2200f, y.toFloat()),
            Offset(2200f, y.toFloat()),
            1.2f
        )
    }
}

private fun DrawScope.drawVehicleWireframe(
    visual: VehicleVisual,
    angle: Float,
    zoom: Float,
    verticalPan: Float
) {
    val projected =
        visual.points.map {
            project(
                it,
                angle,
                zoom,
                size.width,
                size.height,
                verticalPan
            )
        }

    visual.edges
        .sortedBy {
            val a =
                projected.getOrNull(
                    it.from
                )?.depth ?: 0f

            val b =
                projected.getOrNull(
                    it.to
                )?.depth ?: 0f

            (a + b) / 2f
        }
        .forEach { edge ->

            val a =
                projected.getOrNull(
                    edge.from
                ) ?: return@forEach

            val b =
                projected.getOrNull(
                    edge.to
                ) ?: return@forEach

            val hidden =
                (a.depth + b.depth) / 2f >
                    1100f

            val color =
                when (
                    edge.group
                ) {
                    "GLASS" ->
                        Color(0xFF81909A)

                    "CHASSIS" ->
                        Color(0xFF647078)

                    else ->
                        Color(0xFF2D373D)
                }.copy(
                    alpha =
                        if (hidden)
                            0.20f
                        else
                            0.62f
                )

            val effect =
                if (hidden) {
                    PathEffect.dashPathEffect(
                        floatArrayOf(
                            11f,
                            10f
                        )
                    )
                } else {
                    null
                }

            drawLine(
                color = color,
                start = a.point,
                end = b.point,
                strokeWidth =
                    if (
                        edge.group ==
                            "BODY"
                    ) 2.8f else 2f,
                pathEffect = effect
            )
        }

    visual.wheels.forEach { wheel ->
        val p =
            project(
                VisualPoint(
                    wheel.x,
                    wheel.y,
                    wheel.z
                ),
                angle,
                zoom,
                size.width,
                size.height,
                verticalPan
            )

        val scale =
            min(
                size.width / 5400f,
                size.height / 2800f
            ) * zoom

        val radius =
            wheel.radius *
                scale *
                (
                    1f /
                        (
                            1f +
                                p.depth /
                                    7000f
                            )
                    )

        drawCircle(
            Color(0xFF343D42)
                .copy(
                    alpha = 0.55f
                ),
            radius.coerceAtLeast(
                12f
            ),
            p.point,
            style = Stroke(
                width = 3f
            )
        )

        drawCircle(
            Color(0xFF89949A)
                .copy(
                    alpha = 0.55f
                ),
            (
                radius * 0.48f
            ).coerceAtLeast(
                7f
            ),
            p.point,
            style = Stroke(
                width = 2f
            )
        )

        drawCircle(
            Color(0xFF343D42)
                .copy(
                    alpha = 0.65f
                ),
            (
                radius * 0.11f
            ).coerceAtLeast(
                3f
            ),
            p.point
        )
    }
}

private fun DrawScope.drawComponentNode(
    component: Component,
    angle: Float,
    zoom: Float,
    verticalPan: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    selected: Boolean,
    alpha: Float
) {
    val p =
        project(
            VisualPoint(
                component.x,
                component.y,
                component.z
            ),
            angle,
            zoom,
            canvasWidth,
            canvasHeight,
            verticalPan
        )

    val radius =
        when {
            selected -> 17f
            else -> 12f
        }

    val color =
        systemColor(
            component.systemId
        ).copy(
            alpha = alpha
        )

    if (selected) {
        drawCircle(
            color.copy(
                alpha = alpha * 0.18f
            ),
            radius + 13f,
            p.point
        )
    }

    drawCircle(
        Color.White.copy(
            alpha = alpha
        ),
        radius,
        p.point
    )

    drawCircle(
        color,
        radius,
        p.point,
        style = Stroke(
            width =
                if (selected)
                    4f
                else
                    2.5f
        )
    )

    drawNodeSymbol(
        component.shape,
        p.point,
        color,
        alpha
    )

    if (
        selected ||
        zoom > 1.35f
    ) {
        val paint =
            android.graphics.Paint(
                android.graphics.Paint.ANTI_ALIAS_FLAG
            ).apply {
                this.color =
                    android.graphics.Color.argb(
                        (230f * alpha).toInt(),
                        25,
                        32,
                        36
                    )

                textSize =
                    if (selected)
                        21f
                    else
                        17f

                typeface =
                    android.graphics.Typeface.create(
                        android.graphics.Typeface.SANS_SERIF,
                        android.graphics.Typeface.BOLD
                    )
            }

        drawContext.canvas.nativeCanvas.drawText(
            component.name,
            p.point.x + radius + 7f,
            p.point.y - radius - 5f,
            paint
        )
    }
}

private fun DrawScope.drawNodeSymbol(
    shape: String,
    center: Offset,
    color: Color,
    alpha: Float
) {
    when (
        shape.lowercase()
    ) {
        "engine" -> {
            drawCircle(
                color,
                7f,
                center,
                style = Stroke(
                    width = 2f
                )
            )

            drawLine(
                color,
                Offset(
                    center.x - 9f,
                    center.y
                ),
                Offset(
                    center.x + 9f,
                    center.y
                ),
                2f
            )

            drawLine(
                color,
                Offset(
                    center.x,
                    center.y - 9f
                ),
                Offset(
                    center.x,
                    center.y + 9f
                ),
                2f
            )
        }

        "gearbox" -> {
            drawCircle(
                color,
                8f,
                center,
                style = Stroke(
                    width = 2f
                )
            )

            drawCircle(
                color,
                3f,
                center
            )
        }

        "battery" -> {
            drawLine(
                color,
                Offset(
                    center.x - 6f,
                    center.y
                ),
                Offset(
                    center.x + 6f,
                    center.y
                ),
                3f
            )

            drawLine(
                color,
                Offset(
                    center.x + 3f,
                    center.y - 6f
                ),
                Offset(
                    center.x + 3f,
                    center.y + 6f
                ),
                3f
            )
        }

        "ecu" -> {
            drawRect(
                color,
                Offset(
                    center.x - 7f,
                    center.y - 5f
                ),
                androidx.compose.ui.geometry.Size(
                    14f,
                    10f
                ),
                style = Stroke(
                    width = 2f
                )
            )

            for (i in -1..1) {
                drawLine(
                    color,
                    Offset(
                        center.x - 10f,
                        center.y +
                            i * 4f
                    ),
                    Offset(
                        center.x - 6f,
                        center.y +
                            i * 4f
                    ),
                    1.5f
                )

                drawLine(
                    color,
                    Offset(
                        center.x + 6f,
                        center.y +
                            i * 4f
                    ),
                    Offset(
                        center.x + 10f,
                        center.y +
                            i * 4f
                    ),
                    1.5f
                )
            }
        }

        "radiator" -> {
            for (i in -2..2) {
                drawLine(
                    color,
                    Offset(
                        center.x +
                            i * 4f,
                        center.y - 7f
                    ),
                    Offset(
                        center.x +
                            i * 4f,
                        center.y + 7f
                    ),
                    1.5f
                )
            }
        }

        "tank" -> {
            drawOval(
                color,
                Offset(
                    center.x - 8f,
                    center.y - 6f
                ),
                androidx.compose.ui.geometry.Size(
                    16f,
                    12f
                ),
                style = Stroke(
                    width = 2f
                )
            )
        }

        "abs" -> {
            drawLine(
                color,
                Offset(
                    center.x - 8f,
                    center.y
                ),
                Offset(
                    center.x + 8f,
                    center.y
                ),
                2f
            )

            drawLine(
                color,
                Offset(
                    center.x,
                    center.y - 8f
                ),
                Offset(
                    center.x,
                    center.y + 8f
                ),
                2f
            )
        }

        "hvac" -> {
            drawCircle(
                color,
                7f,
                center,
                style = Stroke(
                    width = 2f
                )
            )

            drawLine(
                color,
                Offset(
                    center.x,
                    center.y
                ),
                Offset(
                    center.x + 5f,
                    center.y - 4f
                ),
                2f
            )
        }

        else -> {
            drawCircle(
                color,
                4f,
                center
            )
        }
    }
}

private fun DrawScope.drawConnection3D(
    connection: Connection,
    from: Component,
    to: Component,
    angle: Float,
    zoom: Float,
    verticalPan: Float,
    alpha: Float,
    selected: Boolean
) {
    val route =
        if (connection.route.isNotEmpty()) {
            connection.route.map {
                VisualPoint(
                    it.x,
                    it.y,
                    it.z
                )
            }
        } else {
            listOf(
                VisualPoint(
                    from.x,
                    from.y,
                    from.z
                ),
                VisualPoint(
                    to.x,
                    to.y,
                    to.z
                )
            )
        }

    if (route.size < 2) {
        return
    }

    val projected =
        route.map {
            project(
                it,
                angle,
                zoom,
                size.width,
                size.height,
                verticalPan
            )
        }

    val path =
        Path().apply {
            moveTo(
                projected.first().point.x,
                projected.first().point.y
            )

            projected.drop(1).forEach {
                lineTo(
                    it.point.x,
                    it.point.y
                )
            }
        }

    val color =
        connectionColor(
            connection.type
        ).copy(
            alpha = alpha
        )

    when (
        connection.type
    ) {
        ConnectionType.ELECTRICAL -> {
            drawPath(
                path,
                color,
                style = Stroke(
                    width =
                        if (selected)
                            6f
                        else
                            4f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            drawPath(
                path = path,
                color = Color.White.copy(
                    alpha = alpha * 0.9f
                ),
                style = Stroke(
                    width = 1.6f,
                    cap = StrokeCap.Round
                )
            )
        }

        ConnectionType.CAN,
        ConnectionType.LIN -> {
            drawPath(
                path,
                color,
                style = Stroke(
                    width =
                        if (selected)
                            5f
                        else
                            3.5f,
                    pathEffect =
                        PathEffect.dashPathEffect(
                            floatArrayOf(
                                14f,
                                7f
                            )
                        ),
                    cap = StrokeCap.Round
                )
            )
        }

        ConnectionType.FUEL,
        ConnectionType.COOLANT,
        ConnectionType.OIL,
        ConnectionType.BRAKE,
        ConnectionType.HYDRAULIC -> {
            drawPath(
                path,
                color,
                style = Stroke(
                    width =
                        if (selected)
                            8f
                        else
                            6f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            drawFlowArrow(
                projected,
                color
            )
        }

        ConnectionType.VACUUM,
        ConnectionType.AIR -> {
            drawPath(
                path,
                color,
                style = Stroke(
                    width = 5f,
                    pathEffect =
                        PathEffect.dashPathEffect(
                            floatArrayOf(
                                8f,
                                7f
                            )
                        ),
                    cap = StrokeCap.Round
                )
            )
        }

        ConnectionType.EXHAUST -> {
            drawPath(
                path,
                color,
                style = Stroke(
                    width = 7f,
                    pathEffect =
                        PathEffect.dashPathEffect(
                            floatArrayOf(
                                22f,
                                8f
                            )
                        ),
                    cap = StrokeCap.Round
                )
            )
        }

        ConnectionType.MECHANICAL -> {
            drawPath(
                path,
                color,
                style = Stroke(
                    width =
                        if (selected)
                            8f
                        else
                            5f,
                    cap = StrokeCap.Round
                )
            )
        }
    }

    val first =
        projected.first().point

    val last =
        projected.last().point

    drawCircle(
        color,
        5f,
        first
    )

    drawCircle(
        color,
        5f,
        last
    )
}

private fun DrawScope.drawFlowArrow(
    points: List<Projection>,
    color: Color
) {
    if (points.size < 2) {
        return
    }

    val a =
        points[
            points.lastIndex - 1
        ].point

    val b =
        points.last().point

    val dx =
        b.x - a.x

    val dy =
        b.y - a.y

    val length =
        sqrt(
            dx * dx +
                dy * dy
        )

    if (length < 1f) {
        return
    }

    val ux =
        dx / length

    val uy =
        dy / length

    val px = -uy
    val py = ux

    val size = 12f

    drawLine(
        color,
        b,
        Offset(
            b.x -
                ux * size +
                px * size * 0.7f,
            b.y -
                uy * size +
                py * size * 0.7f
        ),
        2.5f
    )

    drawLine(
        color,
        b,
        Offset(
            b.x -
                ux * size -
                px * size * 0.7f,
            b.y -
                uy * size -
                py * size * 0.7f
        ),
        2.5f
    )
}

private fun DrawScope.drawCallout(
    component: Component,
    angle: Float,
    zoom: Float,
    verticalPan: Float,
    canvasWidth: Float,
    canvasHeight: Float
) {
    val p =
        project(
            VisualPoint(
                component.x,
                component.y,
                component.z
            ),
            angle,
            zoom,
            canvasWidth,
            canvasHeight,
            verticalPan
        )

    val boxWidth =
        250f

    val boxHeight =
        62f

    val x =
        (
            p.point.x + 38f
        ).coerceIn(
            12f,
            canvasWidth -
                boxWidth -
                12f
        )

    val y =
        (
            p.point.y - 80f
        ).coerceIn(
            12f,
            canvasHeight -
                boxHeight -
                12f
        )

    drawRoundRect(
        Color.White.copy(
            alpha = 0.96f
        ),
        Offset(x, y),
        androidx.compose.ui.geometry.Size(
            boxWidth,
            boxHeight
        ),
        androidx.compose.ui.geometry.CornerRadius(
            12f,
            12f
        )
    )

    drawRoundRect(
        Color(0xFF214B68),
        Offset(x, y),
        androidx.compose.ui.geometry.Size(
            boxWidth,
            boxHeight
        ),
        androidx.compose.ui.geometry.CornerRadius(
            12f,
            12f
        ),
        style = Stroke(
            width = 2.5f
        )
    )

    val paint =
        android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color =
                android.graphics.Color.BLACK

            textSize = 19f

            typeface =
                android.graphics.Typeface.create(
                    android.graphics.Typeface.SANS_SERIF,
                    android.graphics.Typeface.BOLD
                )
        }

    drawContext.canvas.nativeCanvas.drawText(
        component.name,
        x + 13f,
        y + 25f,
        paint
    )

    paint.textSize = 14f
    paint.typeface =
        android.graphics.Typeface.create(
            android.graphics.Typeface.SANS_SERIF,
            android.graphics.Typeface.NORMAL
        )

    drawContext.canvas.nativeCanvas.drawText(
        "Система: ${component.systemId}",
        x + 13f,
        y + 47f,
        paint
    )

    drawLine(
        Color(0xFF214B68),
        p.point,
        Offset(
            x,
            y + boxHeight / 2f
        ),
        2f
    )
}

private fun DrawScope.drawOrientationIndicator(
    angle: Float,
    canvasWidth: Float,
    canvasHeight: Float
) {
    val center =
        Offset(
            canvasWidth - 70f,
            canvasHeight - 65f
        )

    drawCircle(
        Color.White.copy(
            alpha = 0.92f
        ),
        31f,
        center
    )

    drawCircle(
        Color(0xFF66737B),
        31f,
        center,
        style = Stroke(
            width = 2f
        )
    )

    val rad =
        Math.toRadians(
            angle.toDouble()
        )

    val x =
        cos(rad).toFloat()

    val z =
        sin(rad).toFloat()

    drawLine(
        Color(0xFF214B68),
        center,
        Offset(
            center.x + x * 20f,
            center.y - z * 20f
        ),
        3f
    )

    val paint =
        android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG
        ).apply {
            color =
                android.graphics.Color.DKGRAY

            textSize = 11f

            textAlign =
                android.graphics.Paint.Align.CENTER
        }

    drawContext.canvas.nativeCanvas.drawText(
        "360°",
        center.x,
        center.y + 49f,
        paint
    )
}

private fun systemColor(
    systemId: String
): Color =
    when (
        systemId
    ) {
        "electrical" ->
            Color(0xFF1769AA)

        "cooling" ->
            Color(0xFF2A7B62)

        "fuel" ->
            Color(0xFFB46D00)

        "brakes" ->
            Color(0xFFA84242)

        "chassis" ->
            Color(0xFF5F6970)

        "climate" ->
            Color(0xFF4D8490)

        "powertrain" ->
            Color(0xFF4C5358)

        else ->
            Color(0xFF214B68)
    }

private fun connectionColor(
    type: ConnectionType
): Color =
    when (
        type
    ) {
        ConnectionType.ELECTRICAL ->
            Color(0xFF1976C5)

        ConnectionType.CAN ->
            Color(0xFF214B68)

        ConnectionType.LIN ->
            Color(0xFF587A8D)

        ConnectionType.FUEL ->
            Color(0xFFB46D00)

        ConnectionType.COOLANT ->
            Color(0xFF2A7B62)

        ConnectionType.OIL ->
            Color(0xFF6A625A)

        ConnectionType.VACUUM ->
            Color(0xFF79559B)

        ConnectionType.AIR ->
            Color(0xFF4E8F98)

        ConnectionType.EXHAUST ->
            Color(0xFF7F5555)

        ConnectionType.BRAKE ->
            Color(0xFFA84242)

        ConnectionType.HYDRAULIC ->
            Color(0xFF9B512F)

        ConnectionType.MECHANICAL ->
            Color(0xFF454D52)
    }
