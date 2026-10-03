package com.animate.companion.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.animate.companion.AppContainer
import com.animate.companion.data.AppSettings
import com.animate.companion.llm.ChatMessage
import com.animate.companion.llm.LlmException
import com.animate.companion.llm.Provider
import com.animate.companion.ui.components.GlassCard
import com.animate.companion.ui.theme.Palette
import kotlinx.coroutines.launch
import java.io.IOException

/** Kid-friendly AI setup: a status line, three big choices, key steps and a paste button. */
@Composable
internal fun AiSection(container: AppContainer, settings: AppSettings) {
    val scope = rememberCoroutineScope()
    val repo = container.settings
    var advanced by remember { mutableStateOf(settings.provider.advanced) }
    val ready = settings.isUsable(settings.provider) && (settings.keys[settings.provider].orEmpty().isNotBlank() || !settings.provider.needsKey)

    Section("Мозг персонажей") {
        StatusLine(ready, settings.provider)
        Provider.entries.filterNot { it.advanced }.forEach { p ->
            ProviderChoice(container, settings, p)
        }

        TextButton(onClick = { advanced = !advanced }) {
            Text(if (advanced) "▲ Скрыть настройки для взрослых" else "⚙️ Для взрослых: ещё настройки")
        }
        AnimatedVisibility(advanced) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Provider.entries.filter { it.advanced }.forEach { p -> ProviderChoice(container, settings, p) }
                CustomModel(container, settings)
                ToggleRow("Если не отвечает — пробовать другие сервисы", settings.autoFallback) { scope.launch { repo.setAutoFallback(it) } }
                Text("Фантазия персонажей: ${"%.1f".format(settings.temperature)}", color = Palette.Text)
                Slider(
                    value = settings.temperature,
                    onValueChange = { v -> scope.launch { repo.setTemperature((v * 10).toInt() / 10f) } },
                    valueRange = 0.3f..1.3f,
                    colors = sliderColors(),
                )
            }
        }
    }
}

@Composable
private fun StatusLine(ready: Boolean, provider: Provider) {
    val (emoji, text) = if (ready) {
        "✅" to "Готово! Персонажи думают с помощью ${provider.label}."
    } else {
        "🔑" to "Чтобы персонажи отвечали, нужен бесплатный ключ. Это займёт пару минут — выбери вариант ниже."
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(if (ready) Color(0x2234D399) else Palette.Sakura.copy(alpha = 0.15f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
        Text(text, color = Palette.Text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun ProviderChoice(container: AppContainer, settings: AppSettings, p: Provider) {
    val scope = rememberCoroutineScope()
    val selected = settings.provider == p
    val hasKey = settings.keys[p].orEmpty().isNotBlank()
    GlassCard(
        Modifier.fillMaxWidth(),
        selected = selected,
        onClick = { scope.launch { container.settings.setProvider(p) } },
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(22.dp).clip(CircleShape)
                        .background(if (selected) Palette.Sakura else Palette.GlassBorder),
                    contentAlignment = Alignment.Center,
                ) { if (selected) Text("✓", color = Palette.Ink, fontWeight = FontWeight.Bold) }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.label, color = Palette.Text, style = MaterialTheme.typography.titleSmall)
                        if (p == Provider.GEMINI) Badge("⭐ Советуем")
                        if (hasKey) Badge("🔑 ключ есть")
                    }
                    Text(p.tagline, color = Palette.TextDim, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (selected) KeySetup(container, settings, p)
        }
    }
}

@Composable
private fun Badge(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 6.dp).clip(RoundedCornerShape(50)).background(Palette.Lavender.copy(alpha = 0.25f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = Palette.Text,
        style = MaterialTheme.typography.labelSmall,
    )
}

@Composable
private fun KeySetup(container: AppContainer, settings: AppSettings, p: Provider) {
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    var status by remember(p) { mutableStateOf<String?>(null) }

    fun check(key: String = settings.keys[p].orEmpty()) {
        status = "⏳ Проверяю…"
        scope.launch {
            status = runCatching {
                container.llm.complete(settings.config(p).copy(apiKey = key), listOf(ChatMessage("user", "Скажи «ня» одним словом.")), 0.5f, 20)
            }.fold(
                onSuccess = { "✅ Работает! Персонаж ответил: «${it.take(30)}»" },
                onFailure = { friendlyError(it) },
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (settings.keys[p].isNullOrBlank()) p.keySteps.forEachIndexed { i, step ->
            Row {
                Text("${i + 1}.", color = Palette.Sakura, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 6.dp))
                Text(step, color = Palette.Text, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton("🌐 Открыть сайт", Modifier.weight(1f)) { uri.openUri(p.keyUrl) }
            PillButton("📋 Вставить ключ", Modifier.weight(1f)) {
                val pasted = clipboard.getText()?.text?.trim().orEmpty()
                if (pasted.length < 10 || pasted.contains(' ')) {
                    status = "🤔 В буфере нет ключа. Скопируй ключ на сайте и попробуй ещё раз."
                } else {
                    scope.launch { container.settings.setKey(p, pasted) }
                    check(pasted)
                }
            }
        }
        SecretField(settings.keys[p].orEmpty(), if (p.needsKey) "Ключ" else "Ключ (по желанию)") {
            scope.launch { container.settings.setKey(p, it) }
        }

        if (p.models.size > 1) {
            Text("Какой должна быть модель?", color = Palette.TextDim, style = MaterialTheme.typography.labelMedium)
            val current = settings.config(p).model
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                p.models.forEach { m ->
                    val on = m.id == current
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                            .background(if (on) Palette.Sakura.copy(alpha = 0.3f) else Palette.Glass)
                            .clickable { scope.launch { container.settings.setModel(p, if (m.id == p.defaultModel) "" else m.id) } }
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(m.label, color = Palette.Text, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
                        Text(m.hint, color = Palette.TextDim, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { check() }) { Text("Проверить") }
            status?.let { Text(it, color = Palette.TextDim, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun PillButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(Palette.accent).clickable(onClick = onClick).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Palette.Ink, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge) }
}

/** Advanced: any model id for the selected provider, with the provider's raw list as a helper. */
@Composable
private fun CustomModel(container: AppContainer, settings: AppSettings) {
    val scope = rememberCoroutineScope()
    val p = settings.provider
    var models by remember(p) { mutableStateOf<List<String>>(emptyList()) }
    var menu by remember { mutableStateOf(false) }
    var status by remember(p) { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DebouncedField(settings.models[p].orEmpty(), "Своя модель для ${p.label} (пусто — ${p.defaultModel})") {
            scope.launch { container.settings.setModel(p, it) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                status = "Загружаю…"
                scope.launch {
                    runCatching { container.llm.listModels(settings.config(p)) }
                        .onSuccess { models = it; menu = true; status = null }
                        .onFailure { status = friendlyError(it) }
                }
            }) { Text("Все модели сервиса") }
            status?.let { Text(it, color = Palette.TextDim, style = MaterialTheme.typography.bodySmall) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                models.take(100).forEach { m ->
                    DropdownMenuItem(text = { Text(m) }, onClick = {
                        menu = false
                        scope.launch { container.settings.setModel(p, m) }
                    })
                }
            }
        }
        Text(
            "Если выбранная модель перестанет работать, приложение само вернётся к стандартной.",
            color = Palette.TextDim, style = MaterialTheme.typography.labelSmall,
        )
    }
}

internal fun friendlyError(e: Throwable): String = when {
    e is LlmException && (e.code == 401 || e.code == 403) -> "❌ Ключ не подошёл. Проверь, что скопировал его целиком."
    e is LlmException && e.code == 429 -> "⏳ Сервис просит подождать — слишком много сообщений. Попробуй через минутку."
    e is LlmException && e.isUnknownModel -> "❌ Такой модели больше нет — выбери другую."
    e is LlmException && e.code >= 500 -> "😵 У сервиса сейчас сбой. Попробуй позже."
    e is IOException && e !is LlmException -> "📶 Нет связи с интернетом?"
    else -> "❌ Не получилось: ${e.message?.take(80)}"
}
