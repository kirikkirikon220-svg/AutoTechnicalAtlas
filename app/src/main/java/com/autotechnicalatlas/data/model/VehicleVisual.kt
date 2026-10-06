package com.autotechnicalatlas.data.model

data class VisualPoint(
    val x: Float,
    val y: Float,
    val z: Float = 0f
)

data class VisualEdge(
    val from: Int,
    val to: Int,
    val group: String = "BODY"
)

data class VisualWheel(
    val x: Float,
    val y: Float,
    val z: Float,
    val radius: Float
)

data class VehicleVisual(
    val points: List<VisualPoint> = emptyList(),
    val edges: List<VisualEdge> = emptyList(),
    val wheels: List<VisualWheel> = emptyList()
) {
    companion object {
        fun defaultSedan(): VehicleVisual {
            val p = listOf(
                // LEFT SIDE
                VisualPoint(-2300f, 380f, -820f),
                VisualPoint(-2050f, 300f, -820f),
                VisualPoint(-1750f, 180f, -820f),
                VisualPoint(-1200f, -180f, -820f),
                VisualPoint(-650f, -440f, -820f),
                VisualPoint(700f, -440f, -820f),
                VisualPoint(1250f, -180f, -820f),
                VisualPoint(1750f, 120f, -820f),
                VisualPoint(2100f, 320f, -820f),
                VisualPoint(2100f, 500f, -820f),
                VisualPoint(-2300f, 500f, -820f),

                // RIGHT SIDE
                VisualPoint(-2300f, 380f, 820f),
                VisualPoint(-2050f, 300f, 820f),
                VisualPoint(-1750f, 180f, 820f),
                VisualPoint(-1200f, -180f, 820f),
                VisualPoint(-650f, -440f, 820f),
                VisualPoint(700f, -440f, 820f),
                VisualPoint(1250f, -180f, 820f),
                VisualPoint(1750f, 120f, 820f),
                VisualPoint(2100f, 320f, 820f),
                VisualPoint(2100f, 500f, 820f),
                VisualPoint(-2300f, 500f, 820f),

                // CABIN DETAILS
                VisualPoint(-1150f, -160f, -620f),
                VisualPoint(-620f, -390f, -620f),
                VisualPoint(680f, -390f, -620f),
                VisualPoint(1170f, -150f, -620f),

                VisualPoint(-1150f, -160f, 620f),
                VisualPoint(-620f, -390f, 620f),
                VisualPoint(680f, -390f, 620f),
                VisualPoint(1170f, -150f, 620f),

                // LOWER CHASSIS
                VisualPoint(-1900f, 470f, 0f),
                VisualPoint(-1200f, 500f, 0f),
                VisualPoint(-500f, 510f, 0f),
                VisualPoint(500f, 510f, 0f),
                VisualPoint(1200f, 500f, 0f),
                VisualPoint(1850f, 470f, 0f),

                // ENGINE BAY / FRONT STRUCTURE
                VisualPoint(-1850f, 120f, -420f),
                VisualPoint(-1850f, 120f, 420f),
                VisualPoint(-1450f, 250f, -420f),
                VisualPoint(-1450f, 250f, 420f),

                // REAR STRUCTURE
                VisualPoint(1550f, 260f, -420f),
                VisualPoint(1550f, 260f, 420f)
            )

            val e = mutableListOf<VisualEdge>()

            // Side silhouettes
            val side = listOf(
                0,1,2,3,4,5,6,7,8,9,10,0
            )

            val right = listOf(
                11,12,13,14,15,16,17,18,19,20,21,11
            )

            for (i in 0 until side.size - 1) {
                e += VisualEdge(side[i], side[i + 1], "BODY")
                e += VisualEdge(right[i], right[i + 1], "BODY")
            }

            // Cross sections
            listOf(
                0 to 11,
                3 to 14,
                4 to 15,
                5 to 16,
                6 to 17,
                8 to 19,
                9 to 20,
                10 to 21
            ).forEach { (a,b) ->
                e += VisualEdge(a,b,"BODY")
            }

            // Windows / cabin
            e += VisualEdge(22,23,"GLASS")
            e += VisualEdge(23,24,"GLASS")
            e += VisualEdge(24,25,"GLASS")

            e += VisualEdge(26,27,"GLASS")
            e += VisualEdge(27,28,"GLASS")
            e += VisualEdge(28,29,"GLASS")

            e += VisualEdge(22,26,"GLASS")
            e += VisualEdge(23,27,"GLASS")
            e += VisualEdge(24,28,"GLASS")
            e += VisualEdge(25,29,"GLASS")

            // Chassis
            e += VisualEdge(30,31,"CHASSIS")
            e += VisualEdge(31,32,"CHASSIS")
            e += VisualEdge(32,33,"CHASSIS")
            e += VisualEdge(33,34,"CHASSIS")
            e += VisualEdge(34,35,"CHASSIS")

            // Engine bay
            e += VisualEdge(36,37,"CHASSIS")
            e += VisualEdge(38,39,"CHASSIS")
            e += VisualEdge(36,38,"CHASSIS")
            e += VisualEdge(37,39,"CHASSIS")

            // Rear
            e += VisualEdge(40,41,"CHASSIS")

            return VehicleVisual(
                points = p,
                edges = e,
                wheels = listOf(
                    VisualWheel(-1450f, 370f, -850f, 360f),
                    VisualWheel(-1450f, 370f, 850f, 360f),
                    VisualWheel(1450f, 370f, -850f, 360f),
                    VisualWheel(1450f, 370f, 850f, 360f)
                )
            )
        }
    }
}
