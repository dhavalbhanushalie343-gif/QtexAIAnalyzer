package com.example.analyzer

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object TechnicalIndicators {

    /**
     * Calculates Exponential Moving Average (EMA) for given period
     */
    fun calculateEMA(prices: List<Double>, period: Int): Double {
        if (prices.isEmpty()) return 0.0
        if (prices.size < period) return prices.average()

        val k = 2.0 / (period + 1.0)
        var ema = prices.take(period).average()

        for (i in period until prices.size) {
            ema = (prices[i] * k) + (ema * (1.0 - k))
        }
        return ema
    }

    /**
     * Calculates Relative Strength Index (RSI 14)
     */
    fun calculateRSI(prices: List<Double>, period: Int = 14): Double {
        if (prices.size < period + 1) return 50.0

        var gains = 0.0
        var losses = 0.0

        for (i in 1..period) {
            val change = prices[i] - prices[i - 1]
            if (change >= 0) {
                gains += change
            } else {
                losses += abs(change)
            }
        }

        var avgGain = gains / period
        var avgLoss = losses / period

        for (i in period + 1 until prices.size) {
            val change = prices[i] - prices[i - 1]
            val gain = if (change >= 0) change else 0.0
            val loss = if (change < 0) abs(change) else 0.0

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
        }

        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    /**
     * Calculates MACD (Fast 12, Slow 26, Signal 9)
     */
    data class MACDResult(val macdLine: Double, val signalLine: Double, val histogram: Double)

    fun calculateMACD(
        prices: List<Double>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): MACDResult {
        if (prices.size < slowPeriod + signalPeriod) {
            return MACDResult(0.0, 0.0, 0.0)
        }

        val macdValues = mutableListOf<Double>()
        for (i in slowPeriod..prices.size) {
            val subList = prices.subList(0, i)
            val fastEma = calculateEMA(subList, fastPeriod)
            val slowEma = calculateEMA(subList, slowPeriod)
            macdValues.add(fastEma - slowEma)
        }

        val currentMacd = macdValues.lastOrNull() ?: 0.0
        val signalLine = calculateEMA(macdValues, signalPeriod)
        val histogram = currentMacd - signalLine

        return MACDResult(currentMacd, signalLine, histogram)
    }

    /**
     * Finds Support & Resistance levels from recent high/low swings
     */
    data class SupportResistance(val support: Double, val resistance: Double)

    fun calculateSupportResistance(prices: List<Double>): SupportResistance {
        if (prices.isEmpty()) return SupportResistance(0.0, 0.0)
        val minPrice = prices.minOrNull() ?: 0.0
        val maxPrice = prices.maxOrNull() ?: 0.0
        return SupportResistance(minPrice, maxPrice)
    }

    /**
     * Detects candlestick pattern type from open, high, low, close
     */
    enum class CandlestickPattern {
        BULLISH_ENGULFING,
        BEARISH_ENGULFING,
        HAMMER,
        SHOOTING_STAR,
        NEUTRAL
    }

    data class Candle(val open: Double, val high: Double, val low: Double, val close: Double)

    fun detectPattern(prev: Candle?, current: Candle): CandlestickPattern {
        if (prev == null) return CandlestickPattern.NEUTRAL

        val currentBody = abs(current.close - current.open)
        val prevBody = abs(prev.close - prev.open)
        val currentIsGreen = current.close > current.open
        val prevIsRed = prev.close < prev.open

        // Bullish Engulfing
        if (prevIsRed && currentIsGreen && current.open <= prev.close && current.close >= prev.open && currentBody > prevBody) {
            return CandlestickPattern.BULLISH_ENGULFING
        }

        // Bearish Engulfing
        val prevIsGreen = prev.close > prev.open
        val currentIsRed = current.close < current.open
        if (prevIsGreen && currentIsRed && current.open >= prev.close && current.close <= prev.open && currentBody > prevBody) {
            return CandlestickPattern.BEARISH_ENGULFING
        }

        // Hammer (long lower wick, small body at top)
        val lowerWick = if (currentIsGreen) current.open - current.low else current.close - current.low
        if (lowerWick > 2 * currentBody && (current.high - max(current.open, current.close)) < currentBody) {
            return CandlestickPattern.HAMMER
        }

        // Shooting Star (long upper wick, small body at bottom)
        val upperWick = current.high - max(current.open, current.close)
        if (upperWick > 2 * currentBody && (min(current.open, current.close) - current.low) < currentBody) {
            return CandlestickPattern.SHOOTING_STAR
        }

        return CandlestickPattern.NEUTRAL
    }
}
