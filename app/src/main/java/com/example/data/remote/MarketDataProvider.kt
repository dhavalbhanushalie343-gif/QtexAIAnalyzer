package com.example.data.remote

import com.example.data.MarketCandle
import com.example.data.MarketDataApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale

class MarketDataProvider : MarketDataApi {

    /*
     * यहां अपनी Twelve Data API key डालो।
     *
     * उदाहरण:
     * private val apiKey = "abc123456789"
     *
     * अपनी असली key मुझे मत भेजना।
     */
    private val apiKey = "PASTE_YOUR_API_KEY_HERE"

    override suspend fun getLatestCandles(
        pair: String,
        timeframe: String,
        limit: Int
    ): List<MarketCandle> = withContext(Dispatchers.IO) {

        if (apiKey == "PASTE_YOUR_API_KEY_HERE") {
            return@withContext emptyList()
        }

        try {
            val interval = when (timeframe.uppercase()) {
                "1 MIN", "1M", "1 MINUTE" -> "1min"
                "5 MIN", "5M" -> "5min"
                "15 MIN", "15M" -> "15min"
                "30 MIN", "30M" -> "30min"
                "1 HOUR", "1H", "60 MIN" -> "1h"
                "4 HOUR", "4H" -> "4h"
                "1 DAY", "1D" -> "1day"
                else -> "1min"
            }

            val encodedSymbol =
                URLEncoder.encode(pair, "UTF-8")

            val urlString =
                "https://api.twelvedata.com/time_series" +
                        "?symbol=$encodedSymbol" +
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

            val response =
                connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

            connection.disconnect()

            val json = JSONObject(response)

            if (json.has("status") &&
                json.optString("status") == "error"
            ) {
                return@withContext emptyList()
            }

            val values = json.optJSONArray("values")
                ?: return@withContext emptyList()

            val result = mutableListOf<MarketCandle>()

            val dateFormats = listOf(
                SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.US
                ),
                SimpleDateFormat(
                    "yyyy-MM-dd HH:mm",
                    Locale.US
                )
            )

            for (i in values.length() - 1 downTo 0) {

                val item = values.optJSONObject(i)
                    ?: continue

                val dateText =
                    item.optString("datetime")

                var timestamp = 0L

                for (format in dateFormats) {
                    try {
                        timestamp =
                            format.parse(dateText)?.time ?: 0L

                        if (timestamp > 0L) break

                    } catch (_: Exception) {
                    }
                }

                val open =
                    item.optString("open").toDoubleOrNull()
                        ?: continue

                val high =
                    item.optString("high").toDoubleOrNull()
                        ?: continue

                val low =
                    item.optString("low").toDoubleOrNull()
                        ?: continue

                val close =
                    item.optString("close").toDoubleOrNull()
                        ?: continue

                val volume =
                    item.optString("volume")
                        .toDoubleOrNull() ?: 0.0

                result.add(
                    MarketCandle(
                        timestamp = timestamp,
                        open = open,
                        high = high,
                        low = low,
                        close = close,
                        volume = volume
                    )
                )
            }

            result

        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getCurrentPrice(
        pair: String
    ): Double? = withContext(Dispatchers.IO) {

        if (apiKey == "73193e6f008f48feab3486b3676e7e16") {
            return@withContext null
        }

        try {

            val encodedSymbol =
                URLEncoder.encode(pair, "UTF-8")

            val urlString =
                "https://api.twelvedata.com/price" +
                        "?symbol=$encodedSymbol" +
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

            val response =
                connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

            connection.disconnect()

            val json = JSONObject(response)

            json.optString("price")
                .toDoubleOrNull()

        } catch (e: Exception) {
            null
        }
    }
}
