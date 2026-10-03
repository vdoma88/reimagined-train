package com.animate.companion.ui.settings

import androidx.compose.foundation.layout.Row
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
    LaunchedEffect(Unit) { speaker.refresh() } // picks up a voice installed while the app was away

    Section("Озвучка") {
        ToggleRow("Персонаж читает ответы вслух", settings.speechEnabled) {
            scope.launch { container.settings.setSpeechEnabled(it) }
            if (!it) speaker.stop()
        }
        if (settings.speechEnabled) {
            Text("Скорость речи: ${"%.1f".format(settings.speechRate)}×", color = Palette.Text)
            Slider(
                value = settings.speechRate,
                onValueChange = { v -> scope.launch { container.settings.setSpeechRate((v * 10).toInt() / 10f) } },
                valueRange = 0.6f..1.5f,
                colors = sliderColors(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    val voice = SpeechText.voiceFor(Gender.FEMALE, "deredere", 1f, 7, settings.speechRate)
                    speaker.speak("Привет! Вот так я буду читать свои ответы. Ня!", voice, "preview")
                }) { Text("▶ Послушать") }
                val hint = when (status) {
                    SpeakerStatus.INITIALIZING -> "⏳ Голос загружается…"
                    SpeakerStatus.READY -> null
                    SpeakerStatus.NO_RUSSIAN_VOICE -> "На телефоне нет русского голоса."
                    SpeakerStatus.UNAVAILABLE -> "Синтез речи недоступен на этом телефоне."
                }
                hint?.let { Text(it, color = Palette.TextDim, style = MaterialTheme.typography.bodySmall) }
            }
            if (status == SpeakerStatus.NO_RUSSIAN_VOICE) {
                TextButton(onClick = { runCatching { context.startActivity(speaker.installVoiceIntent()) } }) {
                    Text("⬇ Скачать русский голос")
                }
            }
            Text(
                "Каждый персонаж говорит своим голосом. Нажми 🔊 рядом с сообщением, чтобы послушать его ещё раз.",
                color = Palette.TextDim,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
