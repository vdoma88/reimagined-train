package com.animate.companion.llm

import com.animate.companion.data.AppDatabase
import com.animate.companion.data.CharacterEntity
import com.animate.companion.data.MessageEntity
import com.animate.companion.data.SettingsRepository
import com.animate.companion.model.Emotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ChatRepository(
    private val db: AppDatabase,
    private val settings: SettingsRepository,
    private val llm: LlmClient,
    private val appScope: CoroutineScope,
) {
    private val summarizeLock = Mutex()

    fun observeCharacters() = db.characters().observeAll()
    fun observeCharacter(id: Long) = db.characters().observe(id)
    fun observeMessages(id: Long) = db.messages().observe(id)

    suspend fun createCharacter(c: CharacterEntity): Long = db.characters().insert(c)
    suspend fun deleteCharacter(id: Long) = db.characters().delete(id)
    suspend fun deleteMessage(id: Long) = db.messages().delete(id)

    suspend fun clearHistory(id: Long) {
        db.messages().clear(id)
        db.characters().get(id)?.let {
            db.characters().update(it.copy(memory = "", summarizedUntilId = 0, lastMessagePreview = "", lastEmotion = "neutral"))
        }
    }

    suspend fun updateMemory(id: Long, memory: String) {
        db.characters().get(id)?.let { db.characters().update(it.copy(memory = memory)) }
    }

    /** Sends a request through the provider chain, falling back on any failure. */
    private suspend fun complete(messages: List<ChatMessage>, maxTokens: Int = 700): String {
        val s = settings.current()
        val errors = mutableListOf<String>()
        for (config in s.providerChain()) {
            try {
                return llm.complete(config, messages, s.temperature, maxTokens)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                errors += e.message ?: e.javaClass.simpleName
            }
        }
        throw LlmException(errors.joinToString("\n"), true)
    }

    suspend fun greet(characterId: Long): ParsedReply {
        val c = db.characters().get(characterId) ?: error("no character")
        val s = settings.current()
        val msgs = listOf(
            ChatMessage("system", PromptBuilder.systemPrompt(c, s.userName)),
            ChatMessage("user", PromptBuilder.greetingInstruction()),
        )
        val reply = runCatching { PromptBuilder.parseReply(complete(msgs)) }
            .getOrElse { FallbackLines.greeting(c) }
        saveReply(c, reply)
        return reply
    }

    suspend fun send(characterId: Long, text: String): ParsedReply {
        db.messages().insert(MessageEntity(characterId = characterId, role = MessageEntity.ROLE_USER, text = text.trim()))
        return reply(characterId)
    }

    /** Generates a reply to the current history (used after send and for retry/regenerate). */
    suspend fun reply(characterId: Long): ParsedReply {
        val c = db.characters().get(characterId) ?: error("no character")
        val s = settings.current()
        val history = db.messages().all(characterId)
        val parsed = PromptBuilder.parseReply(complete(PromptBuilder.buildContext(c, s.userName, history)))
        saveReply(c, parsed)
        appScope.launch { runCatching { summarizeIfNeeded(characterId) } }
        return parsed
    }

    suspend fun regenerate(characterId: Long): ParsedReply {
        val last = db.messages().last(characterId)
        if (last != null && !last.isUser) db.messages().delete(last.id)
        return reply(characterId)
    }

    private suspend fun saveReply(c: CharacterEntity, reply: ParsedReply) {
        db.messages().insert(
            MessageEntity(characterId = c.id, role = MessageEntity.ROLE_ASSISTANT, text = reply.text, emotion = reply.emotion.tag),
        )
        val fresh = db.characters().get(c.id) ?: return
        val bump = if (reply.emotion == Emotion.LOVE || reply.emotion == Emotion.SHY) 2 else 1
        db.characters().update(
            fresh.copy(
                lastMessageAt = System.currentTimeMillis(),
                lastMessagePreview = reply.text.take(120),
                lastEmotion = reply.emotion.tag,
                affection = (fresh.affection + bump).coerceAtMost(100),
            ),
        )
    }

    private suspend fun summarizeIfNeeded(characterId: Long) = summarizeLock.withLock {
        val c = db.characters().get(characterId) ?: return@withLock
        val pending = db.messages().after(characterId, c.summarizedUntilId)
        if (pending.size < PromptBuilder.SUMMARIZE_THRESHOLD) return@withLock
        val chunk = pending.dropLast(PromptBuilder.CONTEXT_MESSAGES / 2)
        if (chunk.isEmpty()) return@withLock
        val s = settings.current()
        val memory = complete(PromptBuilder.summaryPrompt(c, s.userName, chunk), maxTokens = 900).trim()
        val fresh = db.characters().get(characterId) ?: return@withLock
        db.characters().update(fresh.copy(memory = memory.take(2000), summarizedUntilId = chunk.last().id))
    }
}

/** Offline lines used when every provider is unreachable on first meeting. */
object FallbackLines {
    fun greeting(c: CharacterEntity): ParsedReply {
        val male = c.genderEnum == com.animate.companion.model.Gender.MALE
        val glad = if (male) "рад" else "рада"
        return greeting(c, glad)
    }

    private fun greeting(c: CharacterEntity, glad: String): ParsedReply = when (c.archetypeId) {
        "tsundere" -> ParsedReply(Emotion.ANGRY, "*отворачивается* Х-хмф! Я ${c.name}. Н-не думай, что я $glad знакомству… Ну? Чего молчишь?")
        "kuudere" -> ParsedReply(Emotion.NEUTRAL, "${c.name}. …Приятно познакомиться. О чём хочешь поговорить?")
        "dandere" -> ParsedReply(Emotion.SHY, "*прячется за рукавом* А-ано… я ${c.name}… Мы… можем поговорить?")
        "genki" -> ParsedReply(Emotion.HAPPY, "*подпрыгивает* Йахо-о! Я ${c.name}! Давай дружить! Что будем делать?!")
        "himedere" -> ParsedReply(Emotion.SMUG, "О-хо-хо! Перед тобой ${c.name}. Можешь считать себя счастливчиком. Представься же!")
        "chuuni" -> ParsedReply(Emotion.SMUG, "*закрывает глаз ладонью* Печать ослабла… Я — ${c.name}, носитель Тёмного Пламени. А кто ты, смертный?")
        "yandere" -> ParsedReply(Emotion.LOVE, "*сладко улыбается* Наконец-то ты здесь~ Я ${c.name}. Теперь ты ведь никуда не уйдёшь, правда?")
        "lazy" -> ParsedReply(Emotion.THINKING, "*зевает* Ммм… я ${c.name}. Привет. Расскажи что-нибудь интересное, ладно?")
        "onee" -> ParsedReply(Emotion.HAPPY, "Ара-ара, новое лицо~ Я ${c.name}. Устал(а)? Присаживайся, поболтаем.")
        else -> ParsedReply(Emotion.HAPPY, "*машет рукой* Привет! Я ${c.name}! Очень $glad познакомиться~ Как тебя зовут?")
    }
}
