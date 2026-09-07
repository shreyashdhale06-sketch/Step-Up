package com.example.data.remote.gemini

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiCoachService {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GeminiGenerateRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiGenerateResponse::class.java)

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateResponse(
        apiKey: String,
        conversationHistory: List<Pair<String, Boolean>>, // (text, isUser)
        userPrompt: String,
        systemInstructionText: String,
        onChunkReceived: ((String) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalArgumentException("No valid Gemini API key configured"))
        }

        // Build contents list preserving multi-turn context
        val contents = mutableListOf<GeminiContent>()
        for ((text, isUser) in conversationHistory) {
            if (text.isNotBlank()) {
                contents.add(
                    GeminiContent(
                        role = if (isUser) "user" else "model",
                        parts = listOf(GeminiPart(text = text))
                    )
                )
            }
        }
        // Add current prompt
        contents.add(
            GeminiContent(
                role = "user",
                parts = listOf(GeminiPart(text = userPrompt))
            )
        )

        val systemInstruction = if (systemInstructionText.isNotBlank()) {
            GeminiContent(
                role = "system",
                parts = listOf(GeminiPart(text = systemInstructionText))
            )
        } else null

        val geminiRequest = GeminiGenerateRequest(
            contents = contents,
            systemInstruction = systemInstruction,
            generationConfig = GeminiGenerationConfig(
                temperature = 0.7f,
                topP = 0.95f,
                topK = 40,
                maxOutputTokens = 1024
            )
        )

        val requestJson = requestAdapter.toJson(geminiRequest)
        val requestBody = requestJson.toRequestBody(jsonMediaType)

        // Try supported modern models
        val modelCandidates = listOf("gemini-2.5-flash", "gemini-1.5-flash", "gemini-3.5-flash")
        var lastException: Exception? = null

        for (model in modelCandidates) {
            try {
                // Try streaming endpoint first if callback is provided
                if (onChunkReceived != null) {
                    val streamUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?key=$apiKey&alt=sse"
                    val request = Request.Builder()
                        .url(streamUrl)
                        .post(requestBody)
                        .build()

                    val response = okHttpClient.newCall(request).execute()
                    if (response.isSuccessful && response.body != null) {
                        val fullAccumulator = StringBuilder()
                        response.body!!.byteStream().bufferedReader().use { reader ->
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                val currentLine = line?.trim() ?: continue
                                if (currentLine.startsWith("data: ")) {
                                    val dataPayload = currentLine.removePrefix("data: ").trim()
                                    if (dataPayload == "[DONE]") break
                                    try {
                                        val chunkResp = responseAdapter.fromJson(dataPayload)
                                        val chunkText = chunkResp?.candidates?.firstOrNull()
                                            ?.content?.parts?.firstOrNull()?.text
                                        if (!chunkText.isNullOrEmpty()) {
                                            fullAccumulator.append(chunkText)
                                            onChunkReceived(fullAccumulator.toString())
                                        }
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                        val finalText = fullAccumulator.toString()
                        if (finalText.isNotBlank()) {
                            return@withContext Result.success(finalText)
                        }
                    }
                }

                // Fallback to standard unary generateContent
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBodyString = response.body?.string()

                if (response.isSuccessful && !responseBodyString.isNullOrBlank()) {
                    val parsed = responseAdapter.fromJson(responseBodyString)
                    val text = parsed?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!text.isNullOrBlank()) {
                        onChunkReceived?.invoke(text)
                        return@withContext Result.success(text)
                    } else if (parsed?.error != null) {
                        throw RuntimeException("Gemini error: ${parsed.error.message}")
                    }
                } else {
                    val errorMsg = responseBodyString ?: "HTTP ${response.code}"
                    throw RuntimeException("Gemini API call failed ($model): $errorMsg")
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        Result.failure(lastException ?: RuntimeException("Unable to contact Gemini AI Service"))
    }
}
