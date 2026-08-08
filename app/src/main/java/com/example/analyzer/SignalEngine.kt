package com.example.analyzer

import com.example.data.model.AnalysisResult
import com.example.data.model.ConfirmationItem
import com.example.data.model.DataQuality
import com.example.data.model.SignalType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SignalEngine {

    fun generateSignal(
        frameData: ChartFrameData,
        priceHistory: List<Double>,
        minConfidenceThreshold: Int = 65,
        assetName: String = "EUR/USD",
        timeframeName: String = "1 MIN"
    ): AnalysisResult {
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())

        // 1. Data Quality & Chart Validity Checks
        if (!frameData.isValidChart) {
            return AnalysisResult(
                asset = assetName,
                price = frameData.detectedPrice ?: 0.0,
                timeframe = timeframeName,
                signalType = SignalType.WAIT,
                confidence = 0,
                dataQuality = DataQuality.LOW,
                confirmations = emptyList(),
                reason = "WAIT — Chart not detected",
                timestamp = timestamp
            )
        }

        if (frameData.dataQuality == DataQuality.LOW) {
            return AnalysisResult(
                asset = assetName,
                price = frameData.detectedPrice ?: 0.0,
                timeframe = timeframeName,
                signalType = SignalType.WAIT,
                confidence = 25,
                dataQuality = DataQuality.LOW,
                confirmations = emptyList(),
                reason = "WAIT — DATA QUALITY TOO LOW",
                timestamp = timestamp
            )
        }

        // 2. Prepare Price Series for Indicators
        val prices = if (priceHistory.size >= 15) {
            priceHistory
        } else {
            frameData.recentCandles.map { it.close }
        }

        if (prices.size < 10) {
            return AnalysisResult(
                asset = assetName,
                price = frameData.detectedPrice ?: prices.lastOrNull() ?: 0.0,
                timeframe = timeframeName,
                signalType = SignalType.WAIT,
                confidence = 30,
                dataQuality = frameData.dataQuality,
                confirmations = emptyList(),
                reason = "WAIT — Insufficient price history data",
                timestamp = timestamp
            )
        }

        val currentPrice = frameData.detectedPrice ?: prices.last()

        // 3. Calculate Technical Indicators
        val ema20 = TechnicalIndicators.calculateEMA(prices, 20)
        val ema50 = TechnicalIndicators.calculateEMA(prices, 50)
        val ema200 = TechnicalIndicators.calculateEMA(prices, minOf(200, prices.size))

        val rsi14 = TechnicalIndicators.calculateRSI(prices, 14)
        val macd = TechnicalIndicators.calculateMACD(prices)

        val supRes = TechnicalIndicators.calculateSupportResistance(prices)
        val lastCandle = frameData.recentCandles.lastOrNull()
        val prevCandle = if (frameData.recentCandles.size >= 2) frameData.recentCandles[frameData.recentCandles.size - 2] else null
        val candlePattern = if (lastCandle != null) TechnicalIndicators.detectPattern(prevCandle, lastCandle) else TechnicalIndicators.CandlestickPattern.NEUTRAL

        // 4. Scoring System
        val confirmations = mutableListOf<ConfirmationItem>()
        var bullishPoints = 0
        var bearishPoints = 0
        var totalWeights = 0

        // Confirmation 1: EMA Trend Alignment (Weight 2)
        totalWeights += 2
        if (currentPrice > ema20 && ema20 >= ema50) {
            bullishPoints += 2
            confirmations.add(
                ConfirmationItem("EMA Trend", true, "Price above EMA 20 & EMA 50 (Uptrend)")
            )
        } else if (currentPrice < ema20 && ema20 <= ema50) {
            bearishPoints += 2
            confirmations.add(
                ConfirmationItem("EMA Trend", false, "Price below EMA 20 & EMA 50 (Downtrend)")
            )
        } else {
            confirmations.add(
                ConfirmationItem("EMA Trend", false, "EMA lines flat or crossing (Sideways)")
            )
        }

        // Confirmation 2: RSI Momentum (Weight 2)
        totalWeights += 2
        if (rsi14 < 35.0) {
            bullishPoints += 2
            confirmations.add(
                ConfirmationItem("RSI (14)", true, "RSI Oversold (%.1f) — Upward reversal likely".format(rsi14))
            )
        } else if (rsi14 > 65.0) {
            bearishPoints += 2
            confirmations.add(
                ConfirmationItem("RSI (14)", false, "RSI Overbought (%.1f) — Downward reversal likely".format(rsi14))
            )
        } else if (rsi14 > 52.0) {
            bullishPoints += 1
            confirmations.add(
                ConfirmationItem("RSI (14)", true, "RSI Bullish Momentum (%.1f)".format(rsi14))
            )
        } else if (rsi14 < 48.0) {
            bearishPoints += 1
            confirmations.add(
                ConfirmationItem("RSI (14)", false, "RSI Bearish Momentum (%.1f)".format(rsi14))
            )
        }

        // Confirmation 3: MACD Histogram (Weight 2)
        totalWeights += 2
        if (macd.histogram > 0.00005) {
            bullishPoints += 2
            confirmations.add(
                ConfirmationItem("MACD Signal", true, "MACD Line above Signal — Bullish momentum")
            )
        } else if (macd.histogram < -0.00005) {
            bearishPoints += 2
            confirmations.add(
                ConfirmationItem("MACD Signal", false, "MACD Line below Signal — Bearish momentum")
            )
        }

        // Confirmation 4: Price Action & Candlestick Pattern (Weight 2)
        totalWeights += 2
        when (candlePattern) {
            TechnicalIndicators.CandlestickPattern.BULLISH_ENGULFING -> {
                bullishPoints += 2
                confirmations.add(
                    ConfirmationItem("Price Action", true, "Bullish Engulfing pattern detected")
                )
            }
            TechnicalIndicators.CandlestickPattern.HAMMER -> {
                bullishPoints += 2
                confirmations.add(
                    ConfirmationItem("Price Action", true, "Hammer candle — Lower wick rejection")
                )
            }
            TechnicalIndicators.CandlestickPattern.BEARISH_ENGULFING -> {
                bearishPoints += 2
                confirmations.add(
                    ConfirmationItem("Price Action", false, "Bearish Engulfing pattern detected")
                )
            }
            TechnicalIndicators.CandlestickPattern.SHOOTING_STAR -> {
                bearishPoints += 2
                confirmations.add(
                    ConfirmationItem("Price Action", false, "Shooting Star — Upper wick rejection")
                )
            }
            else -> {
                if (frameData.bullishPixelCount > frameData.bearishPixelCount * 1.3) {
                    bullishPoints += 1
                    confirmations.add(ConfirmationItem("Price Action", true, "Bullish candle momentum"))
                } else if (frameData.bearishPixelCount > frameData.bullishPixelCount * 1.3) {
                    bearishPoints += 1
                    confirmations.add(ConfirmationItem("Price Action", false, "Bearish candle momentum"))
                }
            }
        }

        // Confirmation 5: Support & Resistance Bounce (Weight 2)
        totalWeights += 2
        val distToSupport = Math.abs(currentPrice - supRes.support)
        val distToResistance = Math.abs(currentPrice - supRes.resistance)
        if (distToSupport < 0.00030) {
            bullishPoints += 2
            confirmations.add(
                ConfirmationItem("Support / Resistance", true, "Price holding near Support level")
            )
        } else if (distToResistance < 0.00030) {
            bearishPoints += 2
            confirmations.add(
                ConfirmationItem("Support / Resistance", false, "Price rejecting near Resistance level")
            )
        }

        // 5. Final Signal Determination & Confidence %
        val maxPoints = maxOf(bullishPoints, bearishPoints)
        val calculatedConfidence = ((maxPoints.toDouble() / totalWeights) * 100).toInt().coerceIn(0, 98)

        val signalType: SignalType
        val reason: String

        if (calculatedConfidence >= minConfidenceThreshold) {
            if (bullishPoints > bearishPoints) {
                signalType = SignalType.UP
                reason = "Strong Bullish Confluence (EMA + RSI + Price Action)"
            } else if (bearishPoints > bullishPoints) {
                signalType = SignalType.DOWN
                reason = "Strong Bearish Confluence (EMA + RSI + Price Action)"
            } else {
                signalType = SignalType.WAIT
                reason = "WAIT — Conflicting Indicator Signals"
            }
        } else {
            signalType = SignalType.WAIT
            reason = "WAIT — Confidence (%d%%) below threshold (%d%%)".format(
                calculatedConfidence, minConfidenceThreshold
            )
        }

        val supertrendStatus = if (currentPrice > ema50) "BULLISH" else "BEARISH"

        return AnalysisResult(
            asset = assetName,
            price = currentPrice,
            timeframe = timeframeName,
            signalType = signalType,
            confidence = calculatedConfidence,
            dataQuality = frameData.dataQuality,
            confirmations = confirmations,
            reason = reason,
            timestamp = timestamp,
            ema20 = ema20,
            ema50 = ema50,
            rsi14 = rsi14,
            macdValue = macd.histogram,
            supertrend = supertrendStatus
        )
    }
}
