package com.animate.companion.llm

import com.animate.companion.data.AppDatabase
import com.animate.companion.data.CharacterEntity
import com.animate.companion.data.MessageEntity
import com.animate.companion.data.SettingsRepository
import com.animate.companion.model.Emotion
import com.animate.companion.model.NameGenerator
import kotlinx.coroutines.flow.map
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

    /** App-wide chat session: resets after a long pause, used for "take a break" nudges. */
    private var sessionStart = 0L
    private var lastActivity = 0L

    private fun sessionMinutes(now: Long): Long {
        if (now - lastActivity > SESSION_GAP_MS) sessionStart = now
        lastActivity = now
        return (now - sessionStart) / 60_000
    }

    private fun CharacterEntity.withCurrentName(): CharacterEntity = copy(
        name = NameGenerator.refreshLegacyName(name, genderEnum, voiceSeed xor id.hashCode()),
    )

    private suspend fun character(id: Long): CharacterEntity? = db.characters().get(id)?.withCurrentName()

    fun observeCharacters() = db.characters().observeAll().map { list -> list.map { it.withCurrentName() } }
    fun observeCharacter(id: Long) = db.characters().observe(id).map { it?.withCurrentName() }
    fun observeMessages(id: Long) = db.messages().observe(id)

    suspend fun createCharacter(c: CharacterEntity): Long = db.characters().insert(c)
    suspend fun deleteCharacter(id: Long) = db.characters().delete(id)
    suspend fun deleteMessage(id: Long) = db.messages().delete(id)

    suspend fun clearHistory(id: Long) {
        db.messages().clear(id)
        character(id)?.let {
            db.characters().update(it.copy(memory = "", summarizedUntilId = 0, lastMessagePreview = "", lastEmotion = "neutral"))
        }
    }

    suspend fun updateMemory(id: Long, memory: String) {
        character(id)?.let { db.characters().update(it.copy(memory = memory)) }
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
                // A retired or mistyped model: switch back to the provider's default and retry.
                if (e is LlmException && e.isUnknownModel && config.model != config.provider.defaultModel) {
                    settings.setModel(config.provider, "")
                    try {
                        return llm.complete(config.copy(model = config.provider.defaultModel), messages, s.temperature, maxTokens)
                    } catch (e2: Exception) {
                        if (e2 is kotlinx.coroutines.CancellationException) throw e2
                        errors += e2.message ?: e2.javaClass.simpleName
                        continue
                    }
                }
                errors += e.message ?: e.javaClass.simpleName
            }
        }
        throw LlmException(errors.joinToString("\n"), true)
    }

    suspend fun greet(characterId: Long): ParsedReply {
        val c = character(characterId) ?: error("no character")
        val s = settings.current()
        val msgs = listOf(
            ChatMessage("system", PromptBuilder.systemPrompt(c, s.userName, System.currentTimeMillis(), sessionMinutes(System.currentTimeMillis()))),
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
        val c = character(characterId) ?: error("no character")
        val s = settings.current()
        val history = db.messages().all(characterId)
        val now = System.currentTimeMillis()
        val context = PromptBuilder.buildContext(c, s.userName, history, now, sessionMinutes(now))
        val parsed = PromptBuilder.parseReply(complete(context))
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
        val fresh = character(c.id) ?: return
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
        val c = character(characterId) ?: return@withLock
        val pending = db.messages().after(characterId, c.summarizedUntilId)
        if (pending.size < PromptBuilder.SUMMARIZE_THRESHOLD) return@withLock
        val chunk = pending.dropLast(PromptBuilder.CONTEXT_MESSAGES / 2)
        if (chunk.isEmpty()) return@withLock
        val s = settings.current()
        val memory = complete(PromptBuilder.summaryPrompt(c, s.userName, chunk), maxTokens = 900).trim()
        val fresh = character(characterId) ?: return@withLock
        db.characters().update(fresh.copy(memory = memory.take(2000), summarizedUntilId = chunk.last().id))
    }
}

private const val SESSION_GAP_MS = 20 * 60_000L

/** Offline lines used when every provider is unreachable on first meeting. */
object FallbackLines {
    fun greeting(c: CharacterEntity): ParsedReply {
        val male = c.genderEnum == com.animate.companion.model.Gender.MALE
        val glad = if (male) "рад" else "рада"
        return greeting(c, glad)
    }

    private fun greeting(c: CharacterEntity, glad: String): ParsedReply = when (c.archetypeId) {
        "tsundere" -> ParsedReply(Emotion.SMUG, "*проверяет фонарик* Я ${c.name}. Если вывеска обещает настоящего снежного человека, сначала проверь, кто сидит внутри костюма. Что тебя привело в наш городок?")
        "kuudere" -> ParsedReply(Emotion.NEUTRAL, "Я ${c.name}. На карте у нас один мост, а на открытках — два. Пока считаю это ошибкой печати. Как проходит твой день?")
        "deredere" -> ParsedReply(Emotion.HAPPY, "*машет ярким блокнотом* Привет! Я ${c.name}. Сегодня делаю значки для друзей — один получился похожим на сердитую вафлю. Чем ты любишь заниматься?")
        "dandere" -> ParsedReply(Emotion.SHY, "*закрывает старую карту* Привет, я ${c.name}. Нашёлся план городка с закусочной прямо посреди озера. Может, у печатника был трудный день. Как тебя зовут?")
        "genki" -> ParsedReply(Emotion.HAPPY, "*ставит рюкзак у двери* Я ${c.name}! План на сегодня: прогулка, пирог и выяснить, зачем фестивалю семнадцать резиновых уток. Какое у тебя настроение?")
        "himedere" -> ParsedReply(Emotion.SMUG, "*поправляет самодельную звезду на куртке* Я ${c.name}, будущая легенда Кедрового Перевала. Пока мне доверили только плакат фестиваля. Чем ты увлекаешься?")
        "chuuni" -> ParsedReply(Emotion.THINKING, "*убирает приёмник* Привет! Я ${c.name}. Радио передало три свиста. Секретный код? Или чайник соседа? Проверим когда-нибудь. О чём хочешь поговорить?")
        "yandere" -> ParsedReply(Emotion.SURPRISED, "*торжественно разводит руками* Новое знакомство! Я ${c.name}. Сегодняшняя драма: последний кусок пирога исчез. Подозреваемый — мой аппетит. Как твой день?")
        "lazy" -> ParsedReply(Emotion.THINKING, "*отодвигает коробку с деталями* Привет, я ${c.name}. Изобретаю будильник, который уговаривает ещё поспать. Кажется, слишком успешно. Чем займёмся?")
        "onee" -> ParsedReply(Emotion.HAPPY, "Привет, я ${c.name}. В закусочной нашлось тихое место и какао. Сегодняшняя загадка может подождать. Хочешь поболтать или нужна помощь с чем-нибудь?")
        else -> ParsedReply(Emotion.HAPPY, "*машет рукой* Привет! Я ${c.name}, $glad знакомству. Добро пожаловать в Кедровый Перевал — у нас даже афиши иногда с сюрпризом. Как тебя зовут?")
    }
}
