package com.example.data

data class MarketCandle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

interface MarketDataApi {

    suspend fun getLatestCandles(
        pair: String,
        timeframe: String,
        limit: Int = 100
    ): List<MarketCandle>

    suspend fun getCurrentPrice(
        pair: String
    ): Double?
}
