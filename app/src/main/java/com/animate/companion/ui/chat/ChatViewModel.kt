package com.animate.companion.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.animate.companion.AppContainer
import com.animate.companion.audio.SfxType
import com.animate.companion.data.CharacterEntity
import com.animate.companion.data.MessageEntity
import com.animate.companion.audio.SpeakerStatus
import com.animate.companion.audio.SpeechText
import com.animate.companion.llm.ParsedReply
import com.animate.companion.model.PersonaPresets
import kotlinx.coroutines.withTimeoutOrNull
import com.animate.companion.llm.SafetyPolicy
import com.animate.companion.model.Emotion
import com.animate.companion.model.IllustratedCharacters
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val container: AppContainer,
    val characterId: Long,
    greet: Boolean,
) : ViewModel() {
    private val repo = container.chat

    val character: StateFlow<CharacterEntity?> =
        repo.observeCharacter(characterId).stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val messages: StateFlow<List<MessageEntity>> =
        repo.observeMessages(characterId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var input by mutableStateOf("")
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var emotion by mutableStateOf<Emotion?>(null)
        private set
    /** Lip-sync: on while TTS speaks or while a short reaction (interjection, poke) plays. */
    private var speechTalking by mutableStateOf(false)
    private var reactionTalking by mutableStateOf(false)
    val talking: Boolean get() = speechTalking || reactionTalking
    var bubble by mutableStateOf<String?>(null)
        private set

    /** Set when the user's message touches self-harm, abuse, bullying etc.; shows the help card. */
    var concern by mutableStateOf<SafetyPolicy.Concern?>(null)
        private set

    private var reactJob: Job? = null

    /** Id of the message being read aloud ("msg-<id>"), drives the 🔊 buttons and lip-sync. */
    val speakingId: StateFlow<String?> = container.speaker.speakingId

    init {
        viewModelScope.launch {
            container.speaker.speakingId.collect { speechTalking = it != null }
        }
        viewModelScope.launch {
            val c = character.filterNotNull().first()
            emotion = Emotion.fromTag(c.lastEmotion)
            if (greet && repo.observeMessages(characterId).first().isEmpty()) {
                perform { repo.greet(characterId) }
            }
        }
    }

    private suspend fun perform(block: suspend () -> ParsedReply) {
        busy = true
        error = null
        emotion = Emotion.THINKING
        try {
            react(block())
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            error = e.message ?: "Не удалось получить ответ"
            emotion = Emotion.SAD
        } finally {
            busy = false
        }
    }

    private fun react(reply: ParsedReply) {
        emotion = reply.emotion
        container.sound.sfx(SfxType.RECEIVE)
        reactJob?.cancel()
        reactJob = viewModelScope.launch {
            val c = character.value ?: return@launch
            delay(250)
            bubble = container.sound.voice(c, reply.emotion)
            val spoken = readAloud(c, reply.text, latestReplyId(reply.text), afterInterjection = bubble != null)
            if (!spoken) {
                reactionTalking = true
                delay((reply.text.length * 35L).coerceIn(900L, 3200L))
                reactionTalking = false
            }
            delay(800)
            bubble = null
        }
    }

    /** Id of the just-saved assistant message, so its 🔊 button lights up while it is read. */
    private suspend fun latestReplyId(text: String): Long? = withTimeoutOrNull(1500) {
        messages.first { list -> list.lastOrNull()?.let { !it.isUser && it.text == text } == true }.last().id
    }

    private suspend fun readAloud(c: CharacterEntity, text: String, messageId: Long?, afterInterjection: Boolean = false): Boolean {
        val s = container.settings.current()
        if (!s.speechEnabled) return false
        if (afterInterjection) delay(450)
        val arch = PersonaPresets.archetype(c.archetypeId)
        val voice = SpeechText.voiceFor(c.genderEnum, c.archetypeId, arch.voicePitch, c.voiceSeed, s.speechRate)
        return container.speaker.speak(SpeechText.clean(text), voice, "msg-${messageId ?: 0}")
    }

    /** 🔊 on a message: read it aloud, or stop if it is already being read. */
    fun toggleSpeech(m: MessageEntity) {
        if (speakingId.value == "msg-${m.id}") {
            container.speaker.stop()
            return
        }
        val c = character.value ?: return
        viewModelScope.launch {
            emotion = Emotion.fromTag(m.emotion)
            val s = container.settings.current()
            val arch = PersonaPresets.archetype(c.archetypeId)
            val voice = SpeechText.voiceFor(c.genderEnum, c.archetypeId, arch.voicePitch, c.voiceSeed, s.speechRate)
            val text = SpeechText.clean(m.text)
            if (text.isBlank()) return@launch // only *actions*: nothing to read, not an error
            if (!container.speaker.speak(text, voice, "msg-${m.id}")) {
                error = speechProblem()
            }
        }
    }

    private fun speechProblem(): String = when (container.speaker.status.value) {
        SpeakerStatus.NO_RUSSIAN_VOICE -> "На телефоне нет русского голоса. Его можно скачать в Настройки → Озвучка."
        SpeakerStatus.INITIALIZING -> "Голос ещё загружается, попробуй через секунду."
        else -> "Синтез речи недоступен на этом телефоне."
    }

    fun stopSpeech() = container.stopSpeech()

    override fun onCleared() {
        container.stopSpeech()
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty() || busy) return
        input = ""
        container.speaker.stop()
        SafetyPolicy.detect(text)?.let { concern = it }
        container.sound.sfx(SfxType.SEND)
        viewModelScope.launch { perform { repo.send(characterId, text) } }
    }

    fun retry() {
        if (busy) return
        viewModelScope.launch {
            val last = messages.value.lastOrNull()
            perform {
                when {
                    last == null -> repo.greet(characterId)
                    last.isUser -> repo.reply(characterId)
                    else -> repo.regenerate(characterId)
                }
            }
        }
    }

    fun regenerate() {
        if (busy) return
        viewModelScope.launch { perform { repo.regenerate(characterId) } }
    }

    fun deleteMessage(id: Long) = viewModelScope.launch { repo.deleteMessage(id) }

    fun clearHistory() = viewModelScope.launch {
        repo.clearHistory(characterId)
        error = null
        perform { repo.greet(characterId) }
    }

    fun deleteCharacter(then: () -> Unit) = viewModelScope.launch {
        repo.deleteCharacter(characterId)
        then()
    }

    suspend fun saveIllustrationStyle(draft: com.animate.companion.model.Appearance): Boolean {
        val c = container.db.characters().get(characterId) ?: return false
        // Saves style and detail layers; refuses if the artwork changed while the editor was open.
        val merged = com.animate.companion.model.Appearance.mergeStudioEdit(c.appearance, draft) ?: return false
        container.db.characters().updateAppearance(characterId, merged.toJson())
        return true
    }

    fun setIllustration(id: String?) = viewModelScope.launch {
        if (id != null && IllustratedCharacters.find(id) == null) return@launch
        try {
            val c = container.db.characters().get(characterId) ?: return@launch
            container.db.characters().updateAppearance(characterId, c.appearance.selectIllustration(id).toJson())
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            error = "Не удалось сохранить образ. Попробуйте ещё раз."
        }
    }

    fun saveMemory(text: String) = viewModelScope.launch { repo.updateMemory(characterId, text) }

    fun dismissConcern() {
        concern = null
    }

    fun dismissError() {
        error = null
    }

    fun poke() {
        val c = character.value ?: return
        reactJob?.cancel()
        reactJob = viewModelScope.launch {
            val e = listOf(Emotion.SURPRISED, Emotion.SHY, Emotion.HAPPY, Emotion.ANGRY, Emotion.LOVE).random()
            val prev = emotion
            emotion = e
            bubble = container.sound.voice(c, e, force = true)
            reactionTalking = true
            delay(500)
            reactionTalking = false
            delay(900)
            bubble = null
            emotion = prev
        }
    }
}
