package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.data.PriceHistoryPoint
import com.example.ui.theme.PvsForestDark
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.point
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.shader.verticalGradient
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import kotlin.math.floor
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Vico-powered Material 3 Line Chart displaying the evolution of a product's scanned prices
 * from the last 5 Room database entries compared against the market benchmark.
 */
@Composable
fun VicoScannedPriceLineChart(
    points: List<PriceHistoryPoint>,
    primaryColor: Color,
    contentDesc: String,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    val safePoints = remember(points) {
        if (points.isEmpty()) {
            listOf(PriceHistoryPoint("#1", "#1", 0.0, 0.0))
        } else {
            points
        }
    }

    LaunchedEffect(safePoints) {
        modelProducer.runTransaction {
            lineSeries {
                series(safePoints.map { it.storePrice })
                series(safePoints.map { it.marketAvgPrice })
            }
        }
    }

    val allValues = safePoints.flatMap { listOf(it.storePrice, it.marketAvgPrice) }
    val rawMin = allValues.minOrNull() ?: 0.0
    val rawMax = allValues.maxOrNull() ?: 100.0
    val padding = ((rawMax - rawMin) * 0.22).coerceAtLeast(8.0)
    val boundMinY = floor((rawMin - padding).coerceAtLeast(0.0))
    val boundMaxY = ceil(rawMax + padding)

    val rangeProvider = remember(boundMinY, boundMaxY) {
        CartesianLayerRangeProvider.fixed(minY = boundMinY, maxY = boundMaxY)
    }

    val yFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            "$${value.roundToInt()}"
        }
    }

    val xFormatter = remember(safePoints) {
        CartesianValueFormatter { _, value, _ ->
            val idx = value.roundToInt().coerceIn(0, safePoints.lastIndex)
            safePoints[idx].labelEs
        }
    }

    val pointShape = rememberShapeComponent(
        fill = fill(primaryColor),
        shape = CorneredShape.Pill
    )
    val benchmarkPoint = rememberShapeComponent(
        fill = fill(PvsForestDark.copy(alpha = 0.7f)),
        shape = CorneredShape.Pill
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .semantics { contentDescription = contentDesc }
            .testTag("vico_room_price_line_chart")
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        LineCartesianLayer.rememberLine(
                            fill = LineCartesianLayer.LineFill.single(fill(primaryColor)),
                            areaFill = LineCartesianLayer.AreaFill.single(
                                fill(
                                    ShaderProvider.verticalGradient(
                                        arrayOf(
                                            primaryColor.copy(alpha = 0.34f),
                                            primaryColor.copy(alpha = 0.02f)
                                        )
                                    )
                                )
                            ),
                            pointProvider = LineCartesianLayer.PointProvider.single(
                                LineCartesianLayer.point(component = pointShape, size = 8.dp)
                            )
                        ),
                        LineCartesianLayer.rememberLine(
                            fill = LineCartesianLayer.LineFill.single(fill(PvsForestDark.copy(alpha = 0.55f))),
                            pointProvider = LineCartesianLayer.PointProvider.single(
                                LineCartesianLayer.point(component = benchmarkPoint, size = 5.dp)
                            )
                        )
                    ),
                    rangeProvider = rangeProvider
                ),
                startAxis = VerticalAxis.rememberStart(valueFormatter = yFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = xFormatter)
            ),
            modelProducer = modelProducer,
            scrollState = rememberVicoScrollState(scrollEnabled = false),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
    }
}
