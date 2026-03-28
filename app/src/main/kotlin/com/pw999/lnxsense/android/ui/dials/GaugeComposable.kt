package com.pw999.lnxsense.android.ui.dials

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pw999.lnxsense.android.ui.theme.GaugeBackground
import com.pw999.lnxsense.android.ui.theme.GaugeGreen
import com.pw999.lnxsense.android.ui.theme.GaugeRed
import com.pw999.lnxsense.android.ui.theme.GaugeYellow
import com.pw999.lnxsense.api.Reading
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Arc geometry: 240-degree sweep starting at 150 degrees (7 o'clock position).
// This leaves a 120-degree gap at the bottom, giving the classic speedometer look.
// Compose measures angles clockwise from 3 o'clock (east), so 150 degrees = bottom-left.
private const val SWEEP_ANGLE = 240f
private const val START_ANGLE = 150f

@Composable
fun GaugeComposable(
    deviceName: String,
    sensorName: String,
    unit: String,
    value: Double?,
    minValue: Double,
    maxValue: Double,
    format: String,
    status: Reading.Status?,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val targetColor = gaugeValueColor(status)
    // Animate needle position and arc color for smooth transitions between readings
    // FastOutSlowIn: accelerates quickly then decelerates into position, like a real gauge needle
    val animationSpec = tween<Float>(durationMillis = 150, easing = FastOutSlowInEasing)
    val targetFraction = if (value != null && maxValue > minValue) {
        ((value - minValue) / (maxValue - minValue)).coerceIn(0.0, 1.0).toFloat()
    } else 0f
    val animatedFraction by animateFloatAsState(targetValue = targetFraction, animationSpec = animationSpec)
    val valueColor by animateColorAsState(targetValue = targetColor, animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing))
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier.padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Gauge arc + value
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.3f),
                contentAlignment = Alignment.BottomCenter
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = size.minDimension * 0.08f
                    val padding = strokeWidth + 4.dp.toPx()
                    val arcSize = Size(
                        size.width - padding * 2,
                        size.width - padding * 2
                    )
                    val topLeft = Offset(padding, padding)

                    // Background arc
                    drawArc(
                        color = GaugeBackground,
                        startAngle = START_ANGLE,
                        sweepAngle = SWEEP_ANGLE,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Value arc and needle — driven by animatedFraction for smooth transitions
                    if (value != null && maxValue > minValue) {
                        val valueSweep = animatedFraction * SWEEP_ANGLE
                        drawArc(
                            color = valueColor,
                            startAngle = START_ANGLE,
                            sweepAngle = valueSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        drawNeedle(
                            fraction = animatedFraction,
                            center = Offset(size.width / 2, size.width / 2),
                            radius = arcSize.width / 2 - strokeWidth,
                            color = onSurface
                        )
                    }

                    // Min/max labels
                    drawMinMaxLabels(
                        textMeasurer = textMeasurer,
                        minValue = minValue,
                        maxValue = maxValue,
                        center = Offset(size.width / 2, size.width / 2),
                        radius = arcSize.width / 2 + strokeWidth * 0.5f,
                        color = onSurfaceVariant
                    )
                }

                // Value text in the 120-degree open gap at the bottom of the arc
                val displayText = if (value != null) {
                    try {
                        String.format(format, value)
                    } catch (_: Exception) {
                        value.toString()
                    }
                } else {
                    "--"
                }
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (value != null) valueColor else onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Labels below the gauge
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (unit.isNotEmpty()) "$sensorName ($unit)" else sensorName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = deviceName,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun DrawScope.drawNeedle(
    fraction: Float,
    center: Offset,
    radius: Float,
    color: Color
) {
    val angleRad = Math.toRadians((START_ANGLE + fraction * SWEEP_ANGLE).toDouble())
    val needleLength = radius * 0.85f
    val endX = center.x + needleLength * cos(angleRad).toFloat()
    val endY = center.y + needleLength * sin(angleRad).toFloat()
    drawLine(
        color = color,
        start = center,
        end = Offset(endX, endY),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
    )
    // Center dot
    drawCircle(
        color = color,
        radius = 4.dp.toPx(),
        center = center
    )
}

private fun DrawScope.drawMinMaxLabels(
    textMeasurer: TextMeasurer,
    minValue: Double,
    maxValue: Double,
    center: Offset,
    radius: Float,
    color: Color
) {
    val labelStyle = TextStyle(fontSize = 10.sp, color = color)

    // Min label - at start of arc
    val minAngleRad = Math.toRadians(START_ANGLE.toDouble())
    val minText = formatLabel(minValue)
    val minResult = textMeasurer.measure(minText, labelStyle)
    val minX = center.x + (radius + 8.dp.toPx()) * cos(minAngleRad).toFloat() - minResult.size.width / 2
    val minY = center.y + (radius + 8.dp.toPx()) * sin(minAngleRad).toFloat() - minResult.size.height / 2
    drawText(minResult, topLeft = Offset(minX, minY))

    // Max label - at end of arc
    val maxAngleRad = Math.toRadians((START_ANGLE + SWEEP_ANGLE).toDouble())
    val maxText = formatLabel(maxValue)
    val maxResult = textMeasurer.measure(maxText, labelStyle)
    val maxX = center.x + (radius + 8.dp.toPx()) * cos(maxAngleRad).toFloat() - maxResult.size.width / 2
    val maxY = center.y + (radius + 8.dp.toPx()) * sin(maxAngleRad).toFloat() - maxResult.size.height / 2
    drawText(maxResult, topLeft = Offset(maxX, maxY))
}

private fun formatLabel(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format("%.1f", value)
    }
}

@Composable
private fun gaugeValueColor(status: Reading.Status?): Color = when (status) {
    Reading.Status.OK -> GaugeGreen
    Reading.Status.HIGH -> GaugeYellow
    Reading.Status.CRITICAL -> GaugeRed
    Reading.Status.LOW -> MaterialTheme.colorScheme.tertiary
    Reading.Status.ERROR, Reading.Status.DISABLED, null -> GaugeBackground
}
