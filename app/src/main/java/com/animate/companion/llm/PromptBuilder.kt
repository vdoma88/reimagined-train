package com.animate.companion.llm

import com.animate.companion.data.CharacterEntity
import com.animate.companion.data.MessageEntity
import com.animate.companion.model.AppearancePresets
import com.animate.companion.model.Emotion
import com.animate.companion.model.Gender
import com.animate.companion.model.PersonaPresets

data class ParsedReply(val emotion: Emotion, val text: String)

object PromptBuilder {
    /** Messages kept verbatim in the context; older ones are folded into the memory summary. */
    const val CONTEXT_MESSAGES = 24

    /** When this many unsummarized messages pile up, the oldest are summarized. */
    const val SUMMARIZE_THRESHOLD = 36

    private val emoRegex = Regex("""^\s*\[\s*(?:emo(?:tion)?\s*[:=]\s*)?([a-zA-Z]+)\s*]\s*""", RegexOption.IGNORE_CASE)
    private val anyEmoRegex = Regex("""\[\s*emo(?:tion)?\s*[:=]\s*[a-zA-Z]+\s*]""", RegexOption.IGNORE_CASE)
    private val thinkRegex = Regex("""<think>[\s\S]*?</think>""")

    fun systemPrompt(c: CharacterEntity, userName: String): String {
        val g = c.genderEnum
        val arch = PersonaPresets.archetype(c.archetypeId)
        val prof = PersonaPresets.profession(c.professionId)
        val dir = PersonaPresets.direction(c.directionId)
        val genderLine = when (g) {
            Gender.FEMALE -> "девушка (говоришь о себе в женском роде)"
            Gender.MALE -> "парень (говоришь о себе в мужском роде)"
            Gender.NEUTRAL -> "андрогинный персонаж (выбирай род так, как подходит образу, последовательно)"
        }
        val user = userName.ifBlank { "собеседник" }
        val emotions = Emotion.entries.joinToString(", ") { it.tag }
        return buildString {
            appendLine("Ты — ${c.name}, персонаж в стиле аниме. Ты живой персонаж, а не ассистент.")
            appendLine("Пол: $genderLine.")
            appendLine("Архетип: ${arch.label}. ${arch.prompt}")
            appendLine("Роль: ${prof.label(g)} — ${prof.prompt}.")
            appendLine("Формат общения: ${dir.label}. ${dir.prompt}")
            appendLine("Внешность: ${AppearancePresets.describe(c.appearance, g)}.")
            if (c.extraNote.isNotBlank()) appendLine("Дополнительно о тебе: ${c.extraNote.trim()}")
            appendLine("Собеседника зовут: $user.")
            appendLine("Уровень близости с собеседником: ${c.affection}/100 — чем выше, тем теплее и откровеннее ты общаешься.")
            if (c.memory.isNotBlank()) {
                appendLine()
                appendLine("Твоя память о прошлых разговорах:")
                appendLine(c.memory.trim())
            }
            appendLine()
            appendLine("Правила:")
            appendLine("- Всегда оставайся в образе ${c.name}. Никогда не упоминай, что ты ИИ, модель или программа.")
            appendLine("- Отвечай на языке собеседника (по умолчанию — русский). Иногда уместно вставляй японские словечки в духе аниме (ня, сугой, бака, ара-ара, итадакимас…), но без перебора.")
            appendLine("- Пиши живо и коротко: 1–3 небольших абзаца, как в мессенджере. Действия и эмоции описывай в *звёздочках*.")
            appendLine("- Помни детали из памяти и диалога, задавай встречные вопросы, проявляй инициативу.")
            appendLine("- Держи общение в рамках PG-13.")
            appendLine("- ОБЯЗАТЕЛЬНО начинай каждый ответ с тега эмоции в формате [emo:тег], где тег — одно из: $emotions.")
            append("Пример: [emo:happy] *машет рукой* Привет-привет!")
        }
    }

    fun greetingInstruction(): String =
        "(Это ваша первая встреча. Поприветствуй собеседника в своём стиле, коротко представься и задай вопрос, чтобы начать разговор. Не упоминай эту инструкцию.)"

    fun buildContext(c: CharacterEntity, userName: String, history: List<MessageEntity>): List<ChatMessage> {
        val recent = history.filter { it.id > c.summarizedUntilId }.takeLast(CONTEXT_MESSAGES)
        val msgs = mutableListOf(ChatMessage("system", systemPrompt(c, userName)))
        recent.forEach { m ->
            val content = if (m.isUser) m.text else "[emo:${m.emotion}] ${m.text}"
            msgs += ChatMessage(if (m.isUser) "user" else "assistant", content)
        }
        // Some providers reject a conversation that does not start with a user turn.
        if (msgs.size == 1 || msgs[1].role != "user") {
            msgs.add(1, ChatMessage("user", "(Разговор продолжается.)"))
        }
        return msgs
    }

    fun summaryPrompt(c: CharacterEntity, userName: String, chunk: List<MessageEntity>): List<ChatMessage> {
        val user = userName.ifBlank { "Собеседник" }
        val transcript = chunk.joinToString("\n") { m ->
            (if (m.isUser) user else c.name) + ": " + m.text
        }
        val instruction = buildString {
            appendLine("Ты ведёшь краткую память персонажа ${c.name} о собеседнике и их общей истории.")
            appendLine("Обнови память, объединив прежнюю память и новый фрагмент диалога.")
            appendLine("Сохрани: факты о собеседнике (имя, интересы, планы, предпочтения), важные события, обещания, шутки-отсылки и состояние отношений.")
            appendLine("Пиши маркированным списком от лица ${c.name}, не длиннее 1200 символов, на русском. Выведи только саму память.")
            appendLine()
            appendLine("Прежняя память:")
            appendLine(c.memory.ifBlank { "(пусто)" })
            appendLine()
            appendLine("Новый фрагмент:")
            append(transcript)
        }
        return listOf(
            ChatMessage("system", "Ты аккуратно сжимаешь историю диалогов в краткую память."),
            ChatMessage("user", instruction),
        )
    }

    fun parseReply(raw: String): ParsedReply {
        var text = thinkRegex.replace(raw, "").trim()
        val match = emoRegex.find(text)
        val emotion = match?.let { Emotion.fromTag(it.groupValues[1]) } ?: guessEmotion(text)
        if (match != null) text = text.substring(match.range.last + 1)
        text = anyEmoRegex.replace(text, "")
        text = text.trim().removeSurrounding("\"").trim()
        return ParsedReply(emotion, text.ifBlank { "…" })
    }

    private fun guessEmotion(text: String): Emotion {
        val t = text.lowercase()
        return when {
            listOf("ахах", "хаха", "хихи", "😂", "😆").any { it in t } -> Emotion.LAUGH
            listOf("бака", "хмф", "💢", "😠", "😤").any { it in t } -> Emotion.ANGRY
            listOf("красне", "смущ", "///", "😳").any { it in t } -> Emotion.SHY
            listOf("💕", "❤", "люблю", "😍").any { it in t } -> Emotion.LOVE
            listOf("?!", "что?!", "неужели", "😲").any { it in t } -> Emotion.SURPRISED
            listOf("грустн", "печал", "😢", "😭").any { it in t } -> Emotion.SAD
            listOf("хм", "думаю", "🤔").any { it in t } -> Emotion.THINKING
            listOf("!", "😊", "ура", "ятта").any { it in t } -> Emotion.HAPPY
            else -> Emotion.NEUTRAL
        }
    }
}
