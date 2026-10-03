package com.animate.companion.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.animate.companion.AppContainer
import com.animate.companion.audio.LofiTracks
import com.animate.companion.data.AppSettings
import com.animate.companion.llm.ChatMessage
import com.animate.companion.llm.Provider
import com.animate.companion.ui.components.GlassCard
import com.animate.companion.ui.components.SakuraBackground
import com.animate.companion.ui.create.fieldColors
import com.animate.companion.ui.theme.Palette
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(container: AppContainer, onBack: () -> Unit) {
    val s by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val settings = s ?: return
    val repo = container.settings
    val scope = rememberCoroutineScope()

    SakuraBackground(petals = 6) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Palette.Text) }
                Text("Настройки", style = MaterialTheme.typography.titleLarge, color = Palette.Text)
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Section("Ты в этом мире") {
                    DebouncedField(settings.userName, "Как персонажам тебя называть?") { scope.launch { repo.setUserName(it) } }
                }

                AiSection(container, settings)

                Section("Звук и атмосфера") {
                    ToggleRow("Фоновая музыка", settings.musicEnabled) { scope.launch { repo.setMusicEnabled(it) } }
                    if (settings.musicEnabled) {
                        LofiTracks.all.forEachIndexed { i, t ->
                            Row(
                                Modifier.fillMaxWidth().clickable { scope.launch { repo.setMusicTrack(i) } },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = settings.musicTrack == i, onClick = { scope.launch { repo.setMusicTrack(i) } }, colors = radioColors())
                                Text("♪ ${t.title}", color = Palette.Text)
                            }
                        }
                        Text("Громкость музыки", color = Palette.TextDim, style = MaterialTheme.typography.labelMedium)
                        Slider(value = settings.musicVolume, onValueChange = { v -> scope.launch { repo.setMusicVolume(v) } }, colors = sliderColors())
                    }
                    ToggleRow("Голос персонажа", settings.voiceEnabled) { scope.launch { repo.setVoiceEnabled(it) } }
                    if (settings.voiceEnabled) {
                        Text("Громкость голоса", color = Palette.TextDim, style = MaterialTheme.typography.labelMedium)
                        Slider(value = settings.voiceVolume, onValueChange = { v -> scope.launch { repo.setVoiceVolume(v) } }, colors = sliderColors())
                    }
                    ToggleRow("Звуки интерфейса и окружения", settings.sfxEnabled) { scope.launch { repo.setSfxEnabled(it) } }
                }
                SpeechSection(container, settings)
                UpdatesSection(container, settings)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
internal fun Section(title: String, content: @Composable () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Palette.Amber)
            content()
        }
    }
}

@Composable
internal fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!value) }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Palette.Text, modifier = Modifier.weight(1f))
        Switch(
            checked = value,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Palette.Amber, checkedThumbColor = Palette.Text),
        )
    }
}

/** Text field that keeps local state and persists on every change (DataStore writes are cheap). */
@Composable
internal fun DebouncedField(value: String, label: String, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (String) -> Unit) {
    var text by remember { mutableStateOf(value) }
    LaunchedEffect(value) { if (value != text) text = value }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; onChange(it) },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = fieldColors(),
    )
}

@Composable
internal fun SecretField(value: String, label: String, onChange: (String) -> Unit) {
    var text by remember { mutableStateOf(value) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(value) { if (value != text) text = value }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it.trim(); onChange(it.trim()) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null, tint = Palette.TextDim)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = fieldColors(),
    )
}

@Composable
internal fun sliderColors() = SliderDefaults.colors(thumbColor = Palette.Amber, activeTrackColor = Palette.Amber, inactiveTrackColor = Palette.GlassBorder)

@Composable
internal fun radioColors() = RadioButtonDefaults.colors(selectedColor = Palette.Amber, unselectedColor = Palette.TextDim)
