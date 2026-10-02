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
                Section("О тебе") {
                    DebouncedField(settings.userName, "Как персонажам тебя называть?") { scope.launch { repo.setUserName(it) } }
                }

                Section("Мозг персонажей (ИИ)") {
                    Text(
                        "Выбери провайдера и вставь свой бесплатный ключ. Если основной недоступен или упёрся в лимит, " +
                            "приложение само переключится на другие провайдеры с ключами, а в крайнем случае — на Pollinations без ключа.",
                        color = Palette.TextDim, style = MaterialTheme.typography.bodySmall,
                    )
                    Provider.entries.forEach { p -> ProviderCard(container, settings, p) }
                    ToggleRow("Автопереключение при ошибке", settings.autoFallback) { scope.launch { repo.setAutoFallback(it) } }
                    Text("Креативность: ${"%.1f".format(settings.temperature)}", color = Palette.Text)
                    Slider(
                        value = settings.temperature,
                        onValueChange = { v -> scope.launch { repo.setTemperature((v * 10).toInt() / 10f) } },
                        valueRange = 0.3f..1.3f,
                        colors = sliderColors(),
                    )
                }

                Section("Звук") {
                    ToggleRow("Лофай-музыка", settings.musicEnabled) { scope.launch { repo.setMusicEnabled(it) } }
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
                    ToggleRow("Голос персонажа (ня~, кья!)", settings.voiceEnabled) { scope.launch { repo.setVoiceEnabled(it) } }
                    if (settings.voiceEnabled) {
                        Text("Громкость голоса", color = Palette.TextDim, style = MaterialTheme.typography.labelMedium)
                        Slider(value = settings.voiceVolume, onValueChange = { v -> scope.launch { repo.setVoiceVolume(v) } }, colors = sliderColors())
                    }
                    ToggleRow("Звуки интерфейса", settings.sfxEnabled) { scope.launch { repo.setSfxEnabled(it) } }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ProviderCard(container: AppContainer, settings: AppSettings, p: Provider) {
    val scope = rememberCoroutineScope()
    val selected = settings.provider == p
    val uri = LocalUriHandler.current
    var status by remember { mutableStateOf<String?>(null) }
    var models by remember { mutableStateOf<List<String>>(emptyList()) }
    var modelsMenu by remember { mutableStateOf(false) }

    GlassCard(Modifier.fillMaxWidth(), selected = selected, onClick = { scope.launch { container.settings.setProvider(p) } }, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = { scope.launch { container.settings.setProvider(p) } }, colors = radioColors())
                Column(Modifier.weight(1f)) {
                    Text(p.label + if (!p.needsKey) " · без ключа" else "", color = Palette.Text, style = MaterialTheme.typography.titleSmall)
                    Text(p.blurb, color = Palette.TextDim, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (selected) {
                SecretField(settings.keys[p].orEmpty(), if (p.needsKey) "API-ключ" else "API-ключ (рекомендуется)") {
                    scope.launch { container.settings.setKey(p, it) }
                }
                TextButton(onClick = { uri.openUri(p.keyUrl) }) {
                    Text(if (p == Provider.XAI) "Получить ключ →" else "Получить ключ бесплатно →")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DebouncedField(settings.models[p].orEmpty(), "Модель (по умолчанию ${p.defaultModel})", Modifier.weight(1f)) {
                        scope.launch { container.settings.setModel(p, it) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        status = "Загружаю список…"
                        scope.launch {
                            runCatching { container.llm.listModels(settings.config(p)) }
                                .onSuccess { models = it; modelsMenu = true; status = "Моделей: ${it.size}" }
                                .onFailure { status = "Ошибка: ${it.message}" }
                        }
                    }) { Text("Список моделей") }
                    TextButton(onClick = {
                        status = "Проверяю…"
                        scope.launch {
                            runCatching {
                                container.llm.complete(settings.config(p), listOf(ChatMessage("user", "Скажи «ня» одним словом.")), 0.5f, 20)
                            }
                                .onSuccess { status = "✅ Работает: «${it.take(40)}»" }
                                .onFailure { status = "❌ ${it.message}" }
                        }
                    }) { Text("Проверить") }
                    DropdownMenu(expanded = modelsMenu, onDismissRequest = { modelsMenu = false }) {
                        models.take(80).forEach { m ->
                            DropdownMenuItem(text = { Text(m) }, onClick = {
                                modelsMenu = false
                                scope.launch { container.settings.setModel(p, m) }
                            })
                        }
                    }
                }
                status?.let { Text(it, color = Palette.TextDim, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Palette.Sakura)
            content()
        }
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!value) }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Palette.Text, modifier = Modifier.weight(1f))
        Switch(
            checked = value,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Palette.Sakura, checkedThumbColor = Palette.Text),
        )
    }
}

/** Text field that keeps local state and persists on every change (DataStore writes are cheap). */
@Composable
private fun DebouncedField(value: String, label: String, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (String) -> Unit) {
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
private fun SecretField(value: String, label: String, onChange: (String) -> Unit) {
    var text by remember { mutableStateOf(value) }
    var visible by remember { mutableStateOf(false) }
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
private fun sliderColors() = SliderDefaults.colors(thumbColor = Palette.Sakura, activeTrackColor = Palette.Sakura, inactiveTrackColor = Palette.GlassBorder)

@Composable
private fun radioColors() = RadioButtonDefaults.colors(selectedColor = Palette.Sakura, unselectedColor = Palette.TextDim)
