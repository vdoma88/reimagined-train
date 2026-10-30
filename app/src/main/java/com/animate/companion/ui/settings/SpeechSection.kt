package com.animate.companion.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.animate.companion.AppContainer
import com.animate.companion.audio.SpeakerStatus
import com.animate.companion.audio.SpeechText
import com.animate.companion.data.AppSettings
import com.animate.companion.model.Gender
import com.animate.companion.ui.theme.Palette
import kotlinx.coroutines.launch

@Composable
internal fun SpeechSection(container: AppContainer, settings: AppSettings) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val speaker = container.speaker
    val status by speaker.status.collectAsStateWithLifecycle()
    val voices by speaker.voices.collectAsStateWithLifecycle()
    var voiceMenu by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { speaker.refresh() } // picks up a voice installed while the app was away

    Section("Озвучка") {
        ToggleRow("Персонаж читает ответы вслух", settings.speechEnabled) {
            scope.launch { container.settings.setSpeechEnabled(it) }
            if (!it) speaker.stop()
        }
        if (settings.speechEnabled) {
            Text("Голос устройства", color = Palette.Text, style = MaterialTheme.typography.labelLarge)
            val selectedIndex = voices.indexOfFirst { it.id == settings.speechVoiceId }
            val voiceLabel = when {
                settings.speechVoiceId.isEmpty() -> "Автоматически"
                selectedIndex >= 0 -> "Голос ${selectedIndex + 1}"
                else -> "Сохранённый голос недоступен · автоматически"
            }
            androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth()) {
                androidx.compose.material3.OutlinedButton(onClick = { voiceMenu = true }, modifier = Modifier.fillMaxWidth(), enabled = status == SpeakerStatus.READY) {
                    Text(voiceLabel, maxLines = 1)
                }
                DropdownMenu(expanded = voiceMenu, onDismissRequest = { voiceMenu = false }) {
                    DropdownMenuItem(text = { Text("Автоматически") }, onClick = {
                        voiceMenu = false; scope.launch { container.settings.setSpeechVoiceId("") }
                    })
                    voices.forEachIndexed { i, voice ->
                        DropdownMenuItem(text = { Text("Голос ${i + 1} · ${voice.id}", maxLines = 1) }, onClick = {
                            voiceMenu = false; scope.launch { container.settings.setSpeechVoiceId(voice.id) }
                        })
                    }
                }
            }
            Text("Доступность и звучание зависят от синтезатора речи устройства.", color = Palette.TextDim, style = MaterialTheme.typography.bodySmall)
            Text("Скорость речи: ${"%.1f".format(settings.speechRate)}×", color = Palette.Text)
            Slider(
                value = settings.speechRate,
                onValueChange = { v -> scope.launch { container.settings.setSpeechRate((v * 10).toInt() / 10f) } },
                valueRange = 0.6f..1.5f,
                colors = sliderColors(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = status == SpeakerStatus.READY, onClick = {
                    val voice = SpeechText.voiceFor(Gender.FEMALE, "deredere", 1f, 7, settings.speechRate)
                        .copy(deviceVoiceId = settings.speechVoiceId)
                    speaker.speak("Привет! Рада нашей встрече.", voice, "preview-high")
                }) { Text("▶ Выше") }
                TextButton(enabled = status == SpeakerStatus.READY, onClick = {
                    val voice = SpeechText.voiceFor(Gender.MALE, "deredere", 1f, 7, settings.speechRate)
                        .copy(deviceVoiceId = settings.speechVoiceId)
                    speaker.speak("Привет! Рад нашей встрече.", voice, "preview-low")
                }) { Text("▶ Ниже") }
            }
            val hint = when (status) {
                SpeakerStatus.INITIALIZING -> "Голос загружается…"
                SpeakerStatus.READY -> null
                SpeakerStatus.NO_RUSSIAN_VOICE -> "На телефоне нет русского голоса."
                SpeakerStatus.UNAVAILABLE -> "Синтез речи недоступен на этом телефоне."
            }
            hint?.let { Text(it, color = Palette.TextDim, style = MaterialTheme.typography.bodySmall) }
            if (status == SpeakerStatus.NO_RUSSIAN_VOICE || (status == SpeakerStatus.READY && voices.isEmpty())) {
                TextButton(onClick = { runCatching { context.startActivity(speaker.installVoiceIntent()) } }) {
                    Text("⬇ Скачать русский голос")
                }
            }
            Text(
                "Темп и высота речи зависят от персонажа. Нажми 🔊 рядом с сообщением, чтобы послушать его ещё раз.",
                color = Palette.TextDim,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
