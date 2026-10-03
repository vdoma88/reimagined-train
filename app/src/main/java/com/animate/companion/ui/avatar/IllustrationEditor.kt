package com.animate.companion.ui.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.animate.companion.model.Appearance
import com.animate.companion.model.Gender
import com.animate.companion.model.IllustrationStyle
import com.animate.companion.ui.theme.Palette
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Edits a private draft. Dismissal never writes; failed saves retain the draft. */
@Composable
fun IllustrationEditor(
    initial: Appearance,
    gender: Gender,
    onDismiss: () -> Unit,
    onSave: suspend (Appearance) -> Boolean,
) {
    var draftJson by rememberSaveable { mutableStateOf(initial.toJson()) }
    var fullBody by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val draft = Appearance.fromJson(draftJson)
    Dialog(onDismissRequest = { if (!saving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = Palette.Night) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
                Text("Редактор образа", style = MaterialTheme.typography.headlineSmall, color = Palette.Text)
                Text("Настрой характер иллюстрации без потери исходника",
                    style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = { fullBody = false }) { Text(if (!fullBody) "✓ Портрет" else "Портрет") }
                    TextButton(onClick = { fullBody = true }) { Text(if (fullBody) "✓ Полный рост" else "Полный рост") }
                }
                AvatarView(draft, gender, Modifier.fillMaxWidth().weight(1f).background(Palette.Glass),
                    fullBody = fullBody)
                Column(Modifier.weight(1.15f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    IllustrationControls(draft.illustrationStyle, enabled = !saving) {
                        draftJson = draft.copy(illustrationStyle = it.normalized()).toJson()
                        error = false
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Волосы, одежда и мимика пока являются частью цельной иллюстрации. Следующий этап — послойные варианты.",
                        style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
                }
                if (error) Text("Не удалось сохранить. Попробуй ещё раз.", color = MaterialTheme.colorScheme.error)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(enabled = !saving, onClick = onDismiss) { Text("Отмена") }
                    Button(enabled = !saving, onClick = {
                        saving = true
                        scope.launch {
                            try {
                                if (onSave(draft)) onDismiss() else error = true
                            } catch (e: Exception) {
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                error = true
                            } finally { saving = false }
                        }
                    }) { Text(if (saving) "Сохраняем…" else "Сохранить") }
                }
            }
        }
    }
}

@Composable
internal fun IllustrationControls(style: IllustrationStyle, enabled: Boolean = true, onChange: (IllustrationStyle) -> Unit) {
    val s = style.normalized()

    EditorSection("Быстрый стиль")
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PresetChip("Натуральный", enabled) { onChange(IllustrationStyle.NATURAL.copy(mirrored = s.mirrored, motion = s.motion)) }
        PresetChip("Мягкий", enabled) { onChange(IllustrationStyle.SOFT.copy(mirrored = s.mirrored, motion = s.motion)) }
        PresetChip("Тёплый", enabled) { onChange(IllustrationStyle.WARM.copy(mirrored = s.mirrored, motion = s.motion)) }
        PresetChip("Манга", enabled) { onChange(IllustrationStyle.MANGA.copy(mirrored = s.mirrored, motion = s.motion)) }
        PresetChip("Кино", enabled) { onChange(IllustrationStyle.CINEMATIC.copy(mirrored = s.mirrored, motion = s.motion)) }
    }

    EditorSection("Кадр")
    EditorSlider("Масштаб", s.zoom, 0.8f..1.6f, enabled, "%.2f".format(s.zoom)) { onChange(s.copy(zoom = it)) }
    EditorSlider("По горизонтали", s.offsetX, -0.25f..0.25f, enabled, "${(s.offsetX * 100).roundToInt()}%") { onChange(s.copy(offsetX = it)) }
    EditorSlider("По вертикали", s.offsetY, -0.25f..0.25f, enabled, "${(s.offsetY * 100).roundToInt()}%") { onChange(s.copy(offsetY = it)) }
    EditorSlider("Наклон", s.rotation, -8f..8f, enabled, "${s.rotation.roundToInt()}°") { onChange(s.copy(rotation = it)) }

    EditorSection("Цвет")
    EditorSlider("Насыщенность", s.saturation, 0f..1.6f, enabled, "${(s.saturation * 100).roundToInt()}%") { onChange(s.copy(saturation = it)) }
    EditorSlider("Теплота", s.warmth, -1f..1f, enabled, signedPercent(s.warmth)) { onChange(s.copy(warmth = it)) }
    EditorSlider("Яркость", s.brightness, -0.35f..0.35f, enabled, signedPercent(s.brightness / 0.35f)) { onChange(s.copy(brightness = it)) }
    EditorSlider("Контраст", s.contrast, 0.7f..1.35f, enabled, "${(s.contrast * 100).roundToInt()}%") { onChange(s.copy(contrast = it)) }

    EditorSection("Отображение")
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Зеркально", Modifier.weight(1f), color = Palette.Text)
        Switch(s.mirrored, { onChange(s.copy(mirrored = it)) }, enabled = enabled)
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Плавное движение", color = Palette.Text)
            Text("Лёгкое дыхание персонажа", style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
        }
        Switch(s.motion, { onChange(s.copy(motion = it)) }, enabled = enabled)
    }
    TextButton(enabled = enabled, onClick = { onChange(IllustrationStyle()) }) { Text("Сбросить всё") }
}

@Composable
private fun PresetChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    AssistChip(onClick = onClick, enabled = enabled, label = { Text(label) })
}

@Composable
private fun EditorSection(title: String) {
    Spacer(Modifier.height(12.dp))
    Text(title, style = MaterialTheme.typography.titleSmall, color = Palette.Text)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun EditorSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    valueText: String,
    onChange: (Float) -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = Palette.Text, style = MaterialTheme.typography.labelLarge)
        Text(valueText, color = Palette.TextDim, style = MaterialTheme.typography.labelMedium)
    }
    Slider(value = value, onValueChange = onChange, valueRange = range, enabled = enabled)
}

private fun signedPercent(value: Float): String {
    val percent = (value * 100).roundToInt()
    return if (percent > 0) "+$percent%" else "$percent%"
}
