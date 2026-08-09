package com.example.data.remote

import com.example.data.MarketCandle
import com.example.data.MarketDataApi
import kotlin.math.abs
import kotlin.random.Random

class MarketDataProvider : MarketDataApi {

    private val prices = mutableMapOf(
        "EUR/USD" to 1.17342,
        "GBP/USD" to 1.31250,
        "USD/JPY" to 154.200,
        "USD/CHF" to 0.88450,
        "AUD/USD" to 0.65820,
        "USD/CAD" to 1.36500,
        "NZD/USD" to 0.59810
    )

    override suspend fun getLatestCandles(
        pair: String,
        timeframe: String,
        limit: Int
    ): List<MarketCandle> {

        var currentPrice = prices[pair] ?: 1.00000
        val candles = mutableListOf<MarketCandle>()

        repeat(limit) { index ->
            val open = currentPrice
            val change = (Random.nextDouble() - 0.5) *
                getVolatility(pair)

            val close = (open + change).coerceAtLeast(0.00001)
            val high = maxOf(open, close) +
                abs(Random.nextDouble() * getVolatility(pair) * 0.3)

            val low = minOf(open, close) -
                abs(Random.nextDouble() * getVolatility(pair) * 0.3)

            candles.add(
                MarketCandle(
                    timestamp = System.currentTimeMillis() -
                        ((limit - index) * 60_000L),
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = Random.nextDouble(100.0, 1000.0)
                )
            )

            currentPrice = close
        }

        prices[pair] = currentPrice
        return candles
    }

    override suspend fun getCurrentPrice(pair: String): Double {
        val current = prices[pair] ?: 1.00000
        val newPrice = (current +
            (Random.nextDouble() - 0.5) * getVolatility(pair))
            .coerceAtLeast(0.00001)

        prices[pair] = newPrice
        return newPrice
    }

    private fun getVolatility(pair: String): Double {
        return when (pair) {
            "USD/JPY" -> 0.05
            else -> 0.0005
        }
    }
}
