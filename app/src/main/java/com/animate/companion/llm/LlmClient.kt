package com.animate.companion.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/** All providers speak the OpenAI-compatible chat completions protocol. */
enum class Provider(
    val label: String,
    val baseUrl: String,
    val defaultModel: String,
    val needsKey: Boolean,
    val keyUrl: String,
    val blurb: String,
) {
    GEMINI(
        "Google Gemini",
        "https://generativelanguage.googleapis.com/v1beta/openai/",
        "gemini-flash-latest",
        true,
        "https://aistudio.google.com/apikey",
        "Лучшее качество и русский язык. ~1500 запросов/день бесплатно.",
    ),
    GROQ(
        "Groq",
        "https://api.groq.com/openai/v1/",
        "openai/gpt-oss-120b",
        true,
        "https://console.groq.com/keys",
        "Самые быстрые ответы. Бесплатный тариф ~30 запросов/мин.",
    ),
    OPENROUTER(
        "OpenRouter",
        "https://openrouter.ai/api/v1/",
        "openrouter/free",
        true,
        "https://openrouter.ai/keys",
        "Десятки моделей с пометкой :free. ~50 запросов/день.",
    ),
    POLLINATIONS(
        "Pollinations",
        "https://gen.pollinations.ai/v1/",
        "openai",
        false,
        "https://enter.pollinations.ai",
        "Бесплатный ключ на enter.pollinations.ai. Без ключа — лишь редкие короткие ответы.",
    ),
    XAI(
        "xAI Grok",
        "https://api.x.ai/v1/",
        "grok-3-mini",
        true,
        "https://console.x.ai",
        "Оригинальный Grok. Платный, нужен ключ.",
    ),
}

@Serializable
data class ChatMessage(val role: String, val content: String)

class LlmException(message: String, val retryable: Boolean) : IOException(message)

data class ProviderConfig(val provider: Provider, val apiKey: String, val model: String)

class LlmClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build(),
) {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Serializable
    private data class Body(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Float? = null,
        val max_tokens: Int? = null,
    )

    suspend fun complete(
        config: ProviderConfig,
        messages: List<ChatMessage>,
        temperature: Float = 0.9f,
        maxTokens: Int = 700,
    ): String = withContext(Dispatchers.IO) {
        // Pollinations' anonymous tier rejects requests that tune sampling, so keep them bare.
        val bare = config.provider == Provider.POLLINATIONS && config.apiKey.isBlank()
        val body = json.encodeToString(
            Body.serializer(),
            Body(
                config.model.ifBlank { config.provider.defaultModel },
                messages,
                temperature.takeUnless { bare },
                maxTokens.takeUnless { bare },
            ),
        )
        val req = Request.Builder()
            .url(config.provider.baseUrl + "chat/completions")
            .post(body.toRequestBody("application/json".toMediaType()))
            .apply { if (config.apiKey.isNotBlank()) header("Authorization", "Bearer ${config.apiKey.trim()}") }
            .header("HTTP-Referer", "https://github.com/animate-companion")
            .header("X-Title", "AniMate")
            .build()
        http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val retryable = resp.code == 429 || resp.code >= 500 || resp.code == 408
                throw LlmException("${config.provider.label}: HTTP ${resp.code} ${extractError(raw)}", retryable)
            }
            parseContent(raw) ?: throw LlmException("${config.provider.label}: пустой ответ", true)
        }
    }

    suspend fun listModels(config: ProviderConfig): List<String> = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(config.provider.baseUrl + "models")
            .apply { if (config.apiKey.isNotBlank()) header("Authorization", "Bearer ${config.apiKey.trim()}") }
            .build()
        http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw LlmException("HTTP ${resp.code} ${extractError(raw)}", false)
            val data = json.parseToJsonElement(raw).jsonObject["data"]?.jsonArray ?: JsonArray(emptyList())
            data.mapNotNull { (it as? JsonObject)?.get("id")?.jsonPrimitive?.content }
                .map { it.removePrefix("models/") }
                .sorted()
        }
    }

    internal fun parseContent(raw: String): String? = runCatching {
        val choice = json.parseToJsonElement(raw).jsonObject["choices"]!!.jsonArray[0].jsonObject
        choice["message"]!!.jsonObject["content"]!!.jsonPrimitive.content
    }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }

    private fun extractError(raw: String): String = runCatching {
        val el = json.parseToJsonElement(raw)
        val obj = if (el is JsonArray) el[0].jsonObject else el.jsonObject
        obj["error"]?.let { e ->
            if (e is JsonObject) e["message"]?.jsonPrimitive?.content else e.jsonPrimitive.content
        }
    }.getOrNull()?.take(200) ?: raw.take(200)
}
