package com.example.data.remote

import com.example.data.MarketCandle
import com.example.data.MarketDataApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MarketDataProvider : MarketDataApi {

    // अपनी Twelve Data API key यहाँ डालो
    // API key चैट या GitHub में किसी को मत भेजना
    private val apiKey = "YOUR_TWELVE_DATA_API_KEY"

    override suspend fun getLatestCandles(
        pair: String,
        timeframe: String,
        limit: Int
    ): List<MarketCandle> = withContext(Dispatchers.IO) {

        try {
            val interval = when (timeframe.uppercase()) {
                "1 MIN", "1M", "1 MINUTE" -> "1min"
                "5 MIN", "5M" -> "5min"
                "15 MIN", "15M" -> "15min"
                "30 MIN", "30M" -> "30min"
                "1 HOUR", "1H" -> "1h"
                "4 HOUR", "4H" -> "4h"
                "1 DAY", "1D" -> "1day"
                else -> "1min"
            }

            val encodedPair = pair.replace("/", "%2F")

            val urlString =
                "https://api.twelvedata.com/time_series" +
                "?symbol=$encodedPair" +
                "&interval=$interval" +
                "&outputsize=$limit" +
                "&apikey=$apiKey"

            val connection =
                URL(urlString).openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode

            if (responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return@withContext emptyList()
            }

            val response = connection.inputStream
                .bufferedReader()
                .use { it.readText() }

            connection.disconnect()

            val json = JSONObject(response)

            if (json.optString("status") == "error") {
                return@withContext emptyList()
            }

            val values = json.optJSONArray("values")
                ?: return@withContext emptyList()

            val candles = mutableListOf<MarketCandle>()

            for (i in values.length() - 1 downTo 0) {
                val item = values.getJSONObject(i)

                candles.add(
                    MarketCandle(
                        timestamp = System.currentTimeMillis() -
                            ((values.length() - 1 - i) * 60_000L),

                        open = item.optString("open").toDoubleOrNull() ?: 0.0,
                        high = item.optString("high").toDoubleOrNull() ?: 0.0,
                        low = item.optString("low").toDoubleOrNull() ?: 0.0,
                        close = item.optString("close").toDoubleOrNull() ?: 0.0,
                        volume = item.optString("volume")
                            .toDoubleOrNull() ?: 0.0
                    )
                )
            }

            candles

        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getCurrentPrice(pair: String): Double? =
        withContext(Dispatchers.IO) {

            try {
                val encodedPair = pair.replace("/", "%2F")

                val urlString =
                    "https://api.twelvedata.com/price" +
                    "?symbol=$encodedPair" +
                    "&apikey=$apiKey"

                val connection =
                    URL(urlString).openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    connection.disconnect()
                    return@withContext null
                }

                val response = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                connection.disconnect()

                val json = JSONObject(response)

                json.optString("price")
                    .toDoubleOrNull()

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
}
