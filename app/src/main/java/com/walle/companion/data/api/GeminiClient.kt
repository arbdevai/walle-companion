package com.walle.companion.data.api

import com.walle.companion.data.model.GeminiContent
import com.walle.companion.data.model.GeminiGenerationConfig
import com.walle.companion.data.model.GeminiPart
import com.walle.companion.data.model.GeminiRequest
import com.walle.companion.data.model.GeminiResponse
import com.walle.companion.data.model.WalleAiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiClient(
    private val apiKeyProvider: () -> String
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun query(userInput: String): Result<WalleAiResponse> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("API Key Gemini belum disetel"))
        }

        val systemInstruction = """
            Kamu adalah robot asisten meja bernama Wall-E. Sifatmu imut, ramah, penasaran, dan sedikit polos.
            Jawab pertanyaan pengguna dalam Bahasa Indonesia secara ringkas, to-the-point, maksimal 1 sampai 2 kalimat saja.
            Wajib format output JSON murni tanpa markdown:
            {"expression": "IDLE|HAPPY|CURIOUS|SURPRISED|SLEEPY", "reply": "jawaban kamu"}
        """.trimIndent()

        val fullPrompt = "$systemInstruction\n\nPertanyaan pengguna: $userInput"

        val requestPayload = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = fullPrompt))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.7f
            )
        )

        val requestBodyString = json.encodeToString(GeminiRequest.serializer(), requestPayload)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBodyString.toRequestBody(mediaType))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string().orEmpty()
                    return@withContext Result.failure(
                        Exception("HTTP ${response.code}: $errorBody")
                    )
                }

                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Respons server kosong"))

                val geminiResp = json.decodeFromString(GeminiResponse.serializer(), responseBody)
                val rawText = geminiResp.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text.orEmpty().trim()

                if (rawText.isBlank()) {
                    return@withContext Result.success(
                        WalleAiResponse(expression = "CURIOUS", reply = "Wall-E mendengarkan!")
                    )
                }

                val sanitizedJson = cleanJsonString(rawText)
                val aiResponse = try {
                    json.decodeFromString(WalleAiResponse.serializer(), sanitizedJson)
                } catch (_: Exception) {
                    WalleAiResponse(expression = "HAPPY", reply = rawText)
                }

                Result.success(aiResponse)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanJsonString(text: String): String {
        var cleaned = text.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json")
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```")
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```")
        }
        return cleaned.trim()
    }
}
