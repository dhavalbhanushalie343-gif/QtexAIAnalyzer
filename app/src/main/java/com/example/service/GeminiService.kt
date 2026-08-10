package com.example.service

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiService {

    // ⚠️ अपनी Google AI Studio वाली API Key यहाँ डबल कोट्स के बीच पेस्ट करें
    private val apiKey = "AQ.Ab8RN6J9m30QkSqzI8eUMVlV23bSXGGJUcHS1rQ63_csAsifUA "

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )

    suspend fun analyzeChartImage(bitmap: Bitmap): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = content {
                    image(bitmap)
                    text(
                        "You are an expert Forex and Binary Options (Quotex) technical analyst. " +
                        "Analyze this trading chart screenshot. Look at candlestick patterns, trend direction, " +
                        "Support/Resistance, and indicators. Give response in this exact format:\n" +
                        "SIGNAL: [CALL / PUT / WAIT]\n" +
                        "CONFIDENCE: [Percentage, e.g. 80%]\n" +
                        "REASON: [Short 1-sentence technical reason in Hindi]"
                    )
                }

                val response = generativeModel.generateContent(prompt)
                response.text ?: "SIGNAL: WAIT\nCONFIDENCE: 0%\nREASON: चार्ट इमेज रीड नहीं हो सकी।"
            } catch (e: Exception) {
                "SIGNAL: WAIT\nCONFIDENCE: 0%\nREASON: Error - ${e.localizedMessage}"
            }
        }
    }
}
