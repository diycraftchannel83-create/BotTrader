package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PriceCandle
import com.example.ui.theme.*

enum class ChartType {
    CANDLESTICK,
    LINE
}

enum class ChartTimeframe(val label: String) {
    TF_1M("1M"),
    TF_5M("5M"),
    TF_15M("15M"),
    TF_1H("1H")
}

@Composable
fun TechnicalChart(
    candles: List<PriceCandle>,
    currentPriceSol: Double,
    modifier: Modifier = Modifier
) {
    var selectedChartType by remember { mutableStateOf(ChartType.CANDLESTICK) }
    var selectedTimeframe by remember { mutableStateOf(ChartTimeframe.TF_1M) }

    // Indicators are calculated only when actual OHLC candles are available.
    val ma7 = remember(candles) {
        candles.takeLast(7).takeIf { it.size == 7 }?.map { it.close }?.average()
    }
    val ma25 = remember(candles) {
        candles.takeLast(25).takeIf { it.size == 25 }?.map { it.close }?.average()
    }
    val rsiValue = remember(candles) {
        if (candles.size < 15) null else {
            val changes = candles.takeLast(15).zipWithNext { a, b -> b.close - a.close }
            val gains = changes.filter { it > 0.0 }.sumOf { it } / 14.0
            val losses = changes.filter { it < 0.0 }.sumOf { -it } / 14.0
            when {
                losses == 0.0 && gains > 0.0 -> 100.0
                losses == 0.0 -> 50.0
                else -> 100.0 - (100.0 / (1.0 + gains / losses))
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("technical_chart_card"),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Timeframe tabs and Candlestick/Line toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timeframe Selector
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChartTimeframe.values().forEach { tf ->
                        val isSelected = selectedTimeframe == tf
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) NeonGreen.copy(alpha = 0.15f) else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) NeonGreen else CardBorder,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedTimeframe = tf }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tf.label,
                                color = if (isSelected) NeonGreen else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Type Toggle (Candle / Line)
                Row(
                    modifier = Modifier
                        .background(CardHighlight, RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isCandle = selectedChartType == ChartType.CANDLESTICK
                    Button(
                        onClick = { selectedChartType = ChartType.CANDLESTICK },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCandle) NeonCyan.copy(alpha = 0.2f) else Color.Transparent,
                            contentColor = if (isCandle) NeonCyan else TextMuted
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("Candle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { selectedChartType = ChartType.LINE },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isCandle) NeonCyan.copy(alpha = 0.2f) else Color.Transparent,
                            contentColor = if (!isCandle) NeonCyan else TextMuted
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("Line", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Indicators Bar (MA7, MA25, RSI)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MA(7): ${ma7?.let { String.format("%.7f", it) } ?: "—"}",
                    color = DexPaidGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "MA(25): ${ma25?.let { String.format("%.7f", it) } ?: "—"}",
                    color = NeonCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                val rsiColor = when {
                    rsiValue == null -> TextMuted
                    rsiValue > 70 -> DangerRed
                    rsiValue < 35 -> NeonGreen
                    else -> TextSecondary
                }
                val rsiStatus = when {
                    rsiValue == null -> "Unavailable"
                    rsiValue > 70 -> "Overbought"
                    rsiValue < 35 -> "Oversold"
                    else -> "Neutral"
                }
                Text(
                    text = if (rsiValue == null) "RSI(14): —" else "RSI(14): ${String.format("%.1f", rsiValue)} ($rsiStatus)",
                    color = rsiColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Canvas Chart Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (candles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Historical OHLC candles are not available from the current market feed.", color = TextMuted, fontSize = 12.sp)
                    }
                } else {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("chart_canvas")
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val chartHeight = canvasHeight * 0.78f
                        val volumeHeight = canvasHeight * 0.20f
                        val volumeTop = canvasHeight * 0.80f

                        val minPrice = candles.minOf { it.low } * 0.985
                        val maxPrice = maxOf(candles.maxOf { it.high }, currentPriceSol) * 1.015
                        val priceRange = if (maxPrice > minPrice) maxPrice - minPrice else 0.00001
                        val maxVolume = (candles.maxOfOrNull { it.volume } ?: 1.0).coerceAtLeast(10.0)

                        val candleCount = candles.size
                        val slotWidth = canvasWidth / candleCount
                        val candleBarWidth = (slotWidth * 0.65f).coerceAtLeast(3f)

                        // Draw Grid Lines
                        val gridLines = 4
                        for (i in 0..gridLines) {
                            val y = (chartHeight / gridLines) * i
                            drawLine(
                                color = CardBorder.copy(alpha = 0.5f),
                                start = Offset(0f, y),
                                end = Offset(canvasWidth, y),
                                strokeWidth = 1f
                            )
                        }

                        if (selectedChartType == ChartType.CANDLESTICK) {
                            // Render Candlesticks
                            candles.forEachIndexed { i, candle ->
                                val xCenter = i * slotWidth + (slotWidth / 2f)
                                val isBullish = candle.close >= candle.open
                                val color = if (isBullish) NeonGreen else DangerRed

                                // Price to Y coordinate
                                val highY = chartHeight - ((candle.high - minPrice) / priceRange * chartHeight).toFloat()
                                val lowY = chartHeight - ((candle.low - minPrice) / priceRange * chartHeight).toFloat()
                                val openY = chartHeight - ((candle.open - minPrice) / priceRange * chartHeight).toFloat()
                                val closeY = chartHeight - ((candle.close - minPrice) / priceRange * chartHeight).toFloat()

                                // Draw Wick
                                drawLine(
                                    color = color,
                                    start = Offset(xCenter, highY),
                                    end = Offset(xCenter, lowY),
                                    strokeWidth = 1.5f
                                )

                                // Draw Body
                                val bodyTop = minOf(openY, closeY)
                                val bodyHeight = kotlin.math.abs(closeY - openY).coerceAtLeast(2.5f)
                                drawRect(
                                    color = color,
                                    topLeft = Offset(xCenter - (candleBarWidth / 2f), bodyTop),
                                    size = Size(candleBarWidth, bodyHeight)
                                )

                                // Draw Volume Bar at bottom
                                val volBarHeight = (candle.volume / maxVolume * volumeHeight).toFloat()
                                drawRect(
                                    color = color.copy(alpha = 0.35f),
                                    topLeft = Offset(xCenter - (candleBarWidth / 2f), canvasHeight - volBarHeight),
                                    size = Size(candleBarWidth, volBarHeight)
                                )
                            }
                        } else {
                            // Render Line Chart with glowing fill
                            val linePath = Path()
                            val fillPath = Path()

                            candles.forEachIndexed { i, candle ->
                                val x = i * slotWidth + (slotWidth / 2f)
                                val y = chartHeight - ((candle.close - minPrice) / priceRange * chartHeight).toFloat()

                                if (i == 0) {
                                    linePath.moveTo(x, y)
                                    fillPath.moveTo(x, chartHeight)
                                    fillPath.lineTo(x, y)
                                } else {
                                    linePath.lineTo(x, y)
                                    fillPath.lineTo(x, y)
                                }

                                if (i == candles.lastIndex) {
                                    fillPath.lineTo(x, chartHeight)
                                    fillPath.close()
                                }
                            }

                            // Draw subtle fill gradient
                            drawPath(
                                path = fillPath,
                                color = NeonGreen.copy(alpha = 0.12f)
                            )

                            // Draw main line
                            drawPath(
                                path = linePath,
                                color = NeonGreen,
                                style = Stroke(width = 2.5f)
                            )
                        }

                        // Draw Current Live Price line (dashed glow)
                        val liveY = chartHeight - ((currentPriceSol - minPrice) / priceRange * chartHeight).toFloat()
                        drawLine(
                            color = NeonCyan,
                            start = Offset(0f, liveY),
                            end = Offset(canvasWidth, liveY),
                            strokeWidth = 1.5f
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Footer Price Range
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val minPrice = candles.minOfOrNull { it.low } ?: currentPriceSol
                val maxPrice = candles.maxOfOrNull { it.high } ?: currentPriceSol
                Text(
                    text = "24h Low: ${String.format("%.7f", minPrice)} SOL",
                    color = TextMuted,
                    fontSize = 10.sp
                )
                Text(
                    text = "Live: ${String.format("%.7f", currentPriceSol)} SOL",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "24h High: ${String.format("%.7f", maxPrice)} SOL",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}
