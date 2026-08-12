package com.example.analyzer

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.model.DataQuality
import kotlin.math.max

data class ChartFrameData(
    val isValidChart: Boolean,
    val dataQualityScore: Int,
    val dataQuality: DataQuality,
    val bullishPixelCount: Int,
    val bearishPixelCount: Int,
    val detectedPrice: Double?,
    val detectedPair: String?,
    val detectedTimeframe: String?,
    val recentCandles: List<TechnicalIndicators.Candle>
)

object ChartDetector {

    fun analyzeFrame(
        bitmap: Bitmap?,
        fallbackPair: String = "EUR/USD",
        fallbackTimeframe: String = "1 MIN",
        lastKnownPrice: Double = 0.0
    ): ChartFrameData {

        if (
            bitmap == null ||
            bitmap.isRecycled ||
            bitmap.width < 200 ||
            bitmap.height < 200
        ) {
            return lowQuality(
                fallbackPair,
                fallbackTimeframe
            )
        }

        val width = bitmap.width
        val height = bitmap.height

        /*
         * We inspect the central chart area.
         * No synthetic candles are created here.
         */
        val left = (width * 0.08f).toInt()
        val right = (width * 0.92f).toInt()
        val top = (height * 0.15f).toInt()
        val bottom = (height * 0.82f).toInt()

        var green = 0
        var red = 0
        var dark = 0
        var samples = 0

        val stepX = max(2, (right - left) / 100)
        val stepY = max(2, (bottom - top) / 80)

        for (x in left until right step stepX) {
            for (y in top until bottom step stepY) {

                val pixel = bitmap.getPixel(x, y)

                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                samples++

                /*
                 * Green candle / bullish pixel.
                 */
                if (
                    g >= 100 &&
                    g > r * 1.25 &&
                    g > b * 1.15
                ) {
                    green++
                }

                /*
                 * Red candle / bearish pixel.
                 */
                if (
                    r >= 100 &&
                    r > g * 1.25 &&
                    r > b * 1.15
                ) {
                    red++
                }

                /*
                 * Typical dark trading-chart background.
                 */
                if (
                    r < 70 &&
                    g < 70 &&
                    b < 70
                ) {
                    dark++
                }
            }
        }

        if (samples == 0) {
            return lowQuality(
                fallbackPair,
                fallbackTimeframe
            )
        }

        val candlePixels = green + red

        val colorRatio =
            candlePixels.toDouble() / samples

        val darkRatio =
            dark.toDouble() / samples

        /*
         * Conservative quality score.
         *
         * This score describes how clearly the screen
         * resembles a trading chart.
         *
         * It does NOT claim that OHLC data was extracted.
         */
        var score = 0

        if (darkRatio > 0.20) {
            score += 30
        }

        if (darkRatio > 0.40) {
            score += 20
        }

        if (candlePixels >= 10) {
            score += 20
        }

        if (candlePixels >= 30) {
            score += 15
        }

        if (colorRatio > 0.01) {
            score += 10
        }

        if (green > 0 && red > 0) {
            score += 5
        }

        score = score.coerceIn(0, 100)

        val quality = when {
            score >= 70 -> DataQuality.HIGH
            score >= 45 -> DataQuality.MEDIUM
            else -> DataQuality.LOW
        }

        /*
         * Important:
         *
         * We DO NOT generate fake OHLC candles.
         *
         * Without actual candle extraction,
         * technical indicators must not pretend
         * that synthetic data is real market data.
         */
        val candles =
            emptyList<TechnicalIndicators.Candle>()

        val valid =
            score >= 45 &&
            candlePixels >= 10

        return ChartFrameData(
            isValidChart = valid,
            dataQualityScore = score,
            dataQuality = quality,
            bullishPixelCount = green,
            bearishPixelCount = red,
            detectedPrice =
                if (valid && lastKnownPrice > 0.0) {
                    lastKnownPrice
                } else {
                    null
                },
            detectedPair = fallbackPair,
            detectedTimeframe = fallbackTimeframe,
            recentCandles = candles
        )
    }

    private fun lowQuality(
        pair: String,
        timeframe: String
    ): ChartFrameData {

        return ChartFrameData(
            isValidChart = false,
            dataQualityScore = 0,
            dataQuality = DataQuality.LOW,
            bullishPixelCount = 0,
            bearishPixelCount = 0,
            detectedPrice = null,
            detectedPair = pair,
            detectedTimeframe = timeframe,
            recentCandles = emptyList()
        )
    }
}
