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

/** A hand-picked model; [label] is what a kid sees ("Быстрая", "Умная"). */
data class ModelOption(val id: String, val label: String, val hint: String)

/**
 * All providers speak the OpenAI-compatible chat completions protocol. Each lists only a few
 * curated, currently supported models; the first one is the default.
 */
enum class Provider(
    val label: String,
    val baseUrl: String,
    val needsKey: Boolean,
    val keyUrl: String,
    /** One plain-language line for the settings card. */
    val tagline: String,
    /** Step-by-step key instructions, written for a child with a grown-up nearby. */
    val keySteps: List<String>,
    val models: List<ModelOption>,
    /** Hidden under "advanced" settings: paid or unreliable without a key. */
    val advanced: Boolean = false,
) {
    GEMINI(
        "Google Gemini",
        "https://generativelanguage.googleapis.com/v1beta/openai/",
        true,
        "https://aistudio.google.com/apikey",
        "Самый умный и бесплатный. Лучше всех говорит по-русски.",
        listOf(
            "Нажми «Открыть сайт» и войди в Google-аккаунт (попроси взрослого помочь).",
            "Нажми «Create API key» и скопируй ключ.",
            "Вернись сюда и нажми «Вставить ключ».",
        ),
        listOf(
            ModelOption("gemini-flash-latest", "🧠 Умная", "Лучшие ответы"),
            ModelOption("gemini-flash-lite-latest", "⚡ Быстрая", "Отвечает быстрее, чуть проще"),
        ),
    ),
    GROQ(
        "Groq",
        "https://api.groq.com/openai/v1/",
        true,
        "https://console.groq.com/keys",
        "Отвечает очень быстро. Тоже бесплатно.",
        listOf(
            "Нажми «Открыть сайт» и войди (можно через Google-аккаунт, попроси взрослого помочь).",
            "Нажми «Create API Key», придумай любое имя и скопируй ключ.",
            "Вернись сюда и нажми «Вставить ключ».",
        ),
        listOf(
            ModelOption("openai/gpt-oss-120b", "🧠 Умная", "Лучшие ответы"),
            ModelOption("openai/gpt-oss-20b", "⚡ Быстрая", "Мгновенные ответы, чуть проще"),
        ),
    ),
    OPENROUTER(
        "OpenRouter",
        "https://openrouter.ai/api/v1/",
        true,
        "https://openrouter.ai/keys",
        "Запасной вариант: сам выбирает бесплатную модель.",
        listOf(
            "Нажми «Открыть сайт» и войди (попроси взрослого помочь).",
            "Нажми «Create Key» и скопируй ключ.",
            "Вернись сюда и нажми «Вставить ключ».",
        ),
        listOf(ModelOption("openrouter/free", "🎲 Авто", "Бесплатная модель на выбор сервиса")),
    ),
    POLLINATIONS(
        "Pollinations",
        "https://gen.pollinations.ai/v1/",
        false,
        "https://enter.pollinations.ai",
        "Бесплатный ключ на сайте. Без ключа почти не отвечает.",
        listOf(
            "Нажми «Открыть сайт» и войди.",
            "Создай ключ и скопируй его.",
            "Вернись сюда и нажми «Вставить ключ».",
        ),
        listOf(ModelOption("openai", "🎲 Авто", "Модель по умолчанию")),
        advanced = true,
    ),
    XAI(
        "xAI Grok",
        "https://api.x.ai/v1/",
        true,
        "https://console.x.ai",
        "Оригинальный Grok. Платный.",
        listOf(
            "Открой сайт и войди.",
            "Пополни баланс и создай API-ключ.",
            "Вставь ключ сюда.",
        ),
        listOf(
            ModelOption("grok-4.3", "⚡ Стандарт", "Дешевле"),
            ModelOption("grok-4.6", "🧠 Флагман", "Лучшие ответы"),
        ),
        advanced = true,
    ),
    ;

    val defaultModel: String get() = models.first().id
}

@Serializable
data class ChatMessage(val role: String, val content: String)

class LlmException(message: String, val retryable: Boolean, val code: Int = 0) : IOException(message) {
    /** The provider no longer knows the requested model (retired or misspelled). */
    val isUnknownModel get() = code == 404 || (code == 400 && message.orEmpty().contains("model", ignoreCase = true))
}

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
                throw LlmException("${config.provider.label}: HTTP ${resp.code} ${extractError(raw)}", retryable, resp.code)
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
