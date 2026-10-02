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
import com.animate.companion.llm.ParsedReply
import com.animate.companion.model.Emotion
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
    var talking by mutableStateOf(false)
        private set
    var bubble by mutableStateOf<String?>(null)
        private set

    private var reactJob: Job? = null

    init {
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
            talking = true
            delay((reply.text.length * 35L).coerceIn(900L, 3200L))
            talking = false
            delay(800)
            bubble = null
        }
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty() || busy) return
        input = ""
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

    fun saveMemory(text: String) = viewModelScope.launch { repo.updateMemory(characterId, text) }

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
            talking = true
            delay(500)
            talking = false
            delay(900)
            bubble = null
            emotion = prev
        }
    }
}
