package com.animate.companion.ui.avatar

import androidx.compose.foundation.background
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = { fullBody = false }) { Text(if (!fullBody) "✓ Портрет" else "Портрет") }
                    TextButton(onClick = { fullBody = true }) { Text(if (fullBody) "✓ Полный рост" else "Полный рост") }
                }
                AvatarView(draft, gender, Modifier.fillMaxWidth().weight(1f).background(Palette.Glass),
                    fullBody = fullBody)
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                    IllustrationControls(draft.illustrationStyle, enabled = !saving) {
                        draftJson = draft.copy(illustrationStyle = it.normalized()).toJson()
                        error = false
                    }
                    Text("Цвет меняется у всего образа. Волосы, одежда и мимика пока фиксированы.",
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
    EditorSlider("Масштаб", s.zoom, 0.8f..1.5f, enabled) { onChange(s.copy(zoom = it)) }
    EditorSlider("По горизонтали", s.offsetX, -0.2f..0.2f, enabled) { onChange(s.copy(offsetX = it)) }
    EditorSlider("По вертикали", s.offsetY, -0.2f..0.2f, enabled) { onChange(s.copy(offsetY = it)) }
    EditorSlider("Насыщенность", s.saturation, 0f..1.5f, enabled) { onChange(s.copy(saturation = it)) }
    EditorSlider("Теплота", s.warmth, -1f..1f, enabled) { onChange(s.copy(warmth = it)) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Зеркально", Modifier.weight(1f), color = Palette.Text)
        Switch(s.mirrored, { onChange(s.copy(mirrored = it)) }, enabled = enabled)
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Плавное движение", Modifier.weight(1f), color = Palette.Text)
        Switch(s.motion, { onChange(s.copy(motion = it)) }, enabled = enabled)
    }
    TextButton(enabled = enabled, onClick = { onChange(IllustrationStyle()) }) { Text("Сбросить настройки") }
}

@Composable
private fun EditorSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, enabled: Boolean, onChange: (Float) -> Unit) {
    Text(label, color = Palette.Text, style = MaterialTheme.typography.labelLarge)
    Slider(value = value, onValueChange = onChange, valueRange = range, enabled = enabled)
}
