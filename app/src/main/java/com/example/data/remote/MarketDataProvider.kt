package com.example.data.remote

import com.example.data.MarketCandle
import com.example.data.MarketDataApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MarketDataProvider : MarketDataApi {

    /*
     * IMPORTANT:
     * API key को source code में hard-code मत करो.
     *
     * अभी testing के लिए खाली छोड़ा गया है।
     * बाद में इसे BuildConfig / secure backend से लेना बेहतर है.
     */
    private val apiKey = ""

    override suspend fun getLatestCandles(
        pair: String,
        timeframe: String,
        limit: Int
    ): List<MarketCandle> = withContext(Dispatchers.IO) {

        if (apiKey.isBlank()) {
            return@withContext emptyList()
        }

        try {
            val interval = getInterval(timeframe)
            val candleIntervalMs = getIntervalMillis(interval)

            val encodedPair =
                URLEncoder.encode(pair, "UTF-8")

            val urlString =
                "https://api.twelvedata.com/time_series" +
                        "?symbol=$encodedPair" +
                        "&interval=$interval" +
                        "&outputsize=${limit.coerceIn(10, 5000)}" +
                        "&apikey=$apiKey"

            val connection =
                URL(urlString).openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.useCaches = false

            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext emptyList()
                }

                val response =
                    connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                val json = JSONObject(response)

                if (json.optString("status").equals(
                        "error",
                        ignoreCase = true
                    )
                ) {
                    return@withContext emptyList()
                }

                val values =
                    json.optJSONArray("values")
                        ?: return@withContext emptyList()

                val candles = mutableListOf<MarketCandle>()

                /*
                 * Twelve Data normally returns newest first.
                 * We reverse it so indicators receive
                 * oldest -> newest candles.
                 */
                for (i in values.length() - 1 downTo 0) {

                    val item =
                        values.optJSONObject(i)
                            ?: continue

                    val open =
                        item.optString("open")
                            .toDoubleOrNull()
                            ?: continue

                    val high =
                        item.optString("high")
                            .toDoubleOrNull()
                            ?: continue

                    val low =
                        item.optString("low")
                            .toDoubleOrNull()
                            ?: continue

                    val close =
                        item.optString("close")
                            .toDoubleOrNull()
                            ?: continue

                    val volume =
                        item.optString("volume")
                            .toDoubleOrNull()
                            ?: 0.0

                    val timestamp =
                        System.currentTimeMillis() -
                                ((values.length() - 1 - i) *
                                        candleIntervalMs)

                    candles.add(
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

                return@withContext candles

            } finally {
                connection.disconnect()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getCurrentPrice(
        pair: String
    ): Double? = withContext(Dispatchers.IO) {

        if (apiKey.isBlank()) {
            return@withContext null
        }

        try {
            val encodedPair =
                URLEncoder.encode(pair, "UTF-8")

            val urlString =
                "https://api.twelvedata.com/price" +
                        "?symbol=$encodedPair" +
                        "&apikey=$apiKey"

            val connection =
                URL(urlString).openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.useCaches = false

            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext null
                }

                val response =
                    connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                val json =
                    JSONObject(response)

                return@withContext json
                    .optString("price")
                    .toDoubleOrNull()

            } finally {
                connection.disconnect()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getInterval(
        timeframe: String
    ): String {

        return when (timeframe.trim().uppercase()) {

            "1 MIN",
            "1M",
            "1 MINUTE" ->
                "1min"

            "5 MIN",
            "5M",
            "5 MINUTE" ->
                "5min"

            "15 MIN",
            "15M",
            "15 MINUTE" ->
                "15min"

            "30 MIN",
            "30M",
            "30 MINUTE" ->
                "30min"

            "1 HOUR",
            "1H",
            "60 MIN" ->
                "1h"

            "4 HOUR",
            "4H" ->
                "4h"

            "1 DAY",
            "1D" ->
                "1day"

            else ->
                "1min"
        }
    }

    private fun getIntervalMillis(
        interval: String
    ): Long {

        return when (interval) {

            "1min" ->
                60_000L

            "5min" ->
                5 * 60_000L

            "15min" ->
                15 * 60_000L

            "30min" ->
                30 * 60_000L

            "1h" ->
                60 * 60_000L

            "4h" ->
                4 * 60 * 60_000L

            "1day" ->
                24 * 60 * 60_000L

            else ->
                60_000L
        }
    }
}
