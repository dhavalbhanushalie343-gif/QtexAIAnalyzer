package com.example.data.model

enum class SignalType(val label: String, val emoji: String) {
    UP("UP", "🟢"),
    DOWN("DOWN", "🔴"),
    WAIT("WAIT", "⚪")
}

enum class DataQuality(val label: String) {
    HIGH("HIGH"),
    MEDIUM("MEDIUM"),
    LOW("LOW")
}

data class ConfirmationItem(
    val name: String,
    val isBullish: Boolean,
    val description: String
)

data class AnalysisResult(
    val asset: String,
    val price: Double,
    val timeframe: String,
    val signalType: SignalType,
    val confidence: Int, // 0-100
    val dataQuality: DataQuality,
    val confirmations: List<ConfirmationItem>,
    val reason: String,
    val timestamp: String,
    val ema20: Double = 0.0,
    val ema50: Double = 0.0,
    val rsi14: Double = 50.0,
    val macdValue: Double = 0.0,
    val supertrend: String = "NEUTRAL"
)

data class ForexPair(
    val symbol: String,
    val name: String,
    val basePrice: Double,
    val spread: Double = 0.00012
)
