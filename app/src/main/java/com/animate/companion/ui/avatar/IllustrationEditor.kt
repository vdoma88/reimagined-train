package com.animate.companion.ui.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.animate.companion.model.Appearance
import com.animate.companion.model.Gender
import com.animate.companion.model.IllustrationStyle
import com.animate.companion.model.IllustrationDetails
import com.animate.companion.model.IllustrationExpression
import com.animate.companion.model.IllustrationAccent
import com.animate.companion.model.IllustrationAccessory
import com.animate.companion.model.IllustrationLayerAsset
import com.animate.companion.model.IllustrationLayerCategory
import com.animate.companion.model.IllustrationLayerRegistry
import com.animate.companion.ui.theme.Palette
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class EditorTab(val title: String) {
    LOOK("Образ"),
    DETAILS("Внешность"),
    COLOR("Цвет"),
    FRAME("Кадр"),
}

/** Edits a private draft. Dismissal never writes; failed saves retain the draft. */
@Composable
fun IllustrationEditor(
    initial: Appearance,
    gender: Gender,
    onDismiss: () -> Unit,
    onSave: suspend (Appearance) -> Boolean,
) {
    var draftJson by rememberSaveable { mutableStateOf(initial.toJson()) }
    var tabName by rememberSaveable { mutableStateOf(EditorTab.DETAILS.name) }
    var fullBody by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val previewHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.34f).coerceIn(100.dp, 300.dp)
    val draft = Appearance.fromJson(draftJson)
    val tab = EditorTab.entries.firstOrNull { it.name == tabName } ?: EditorTab.LOOK

    fun update(next: Appearance) {
        draftJson = next.toJson()
        error = false
    }

    Dialog(
        onDismissRequest = { if (!saving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(color = Palette.Night) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(enabled = !saving, onClick = onDismiss) { Text("Закрыть") }
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Студия образа",
                            style = MaterialTheme.typography.titleMedium,
                            color = Palette.Text,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Все изменения видны сразу",
                            style = MaterialTheme.typography.labelSmall,
                            color = Palette.TextDim,
                        )
                    }
                    Button(
                        enabled = !saving,
                        onClick = {
                            saving = true
                            scope.launch {
                                try {
                                    if (onSave(draft)) onDismiss() else error = true
                                } catch (e: Exception) {
                                    if (e is kotlinx.coroutines.CancellationException) throw e
                                    error = true
                                } finally {
                                    saving = false
                                }
                            }
                        }
                    ) { Text(if (saving) "…" else "Готово") }
                }

                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(previewHeight)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Palette.Glass)
                    ) {
                        AvatarView(
                            appearance = draft,
                            gender = gender,
                            modifier = Modifier.fillMaxSize(),
                            fullBody = fullBody,
                        )
                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = Palette.Night.copy(alpha = 0.82f),
                        ) {
                            Row(Modifier.padding(4.dp)) {
                                FilterChip(
                                    selected = !fullBody,
                                    onClick = { fullBody = false },
                                    label = { Text("Портрет") },
                                )
                                Spacer(Modifier.width(6.dp))
                                FilterChip(
                                    selected = fullBody,
                                    onClick = { fullBody = true },
                                    label = { Text("Полный рост") },
                                )
                            }
                        }
                    }
                }

                ScrollableTabRow(
                    selectedTabIndex = tab.ordinal,
                    edgePadding = 12.dp,
                    containerColor = Palette.Night,
                ) {
                    EditorTab.entries.forEach { item ->
                        Tab(
                            selected = item == tab,
                            onClick = { tabName = item.name },
                            text = { Text(item.title) },
                        )
                    }
                }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    when (tab) {
                        EditorTab.LOOK -> IllustrationLookControls(
                            draft.illustrationStyle,
                            enabled = !saving,
                        ) { update(draft.copy(illustrationStyle = it.normalized())) }

                        EditorTab.DETAILS -> CartoonLookControls(draft, enabled = !saving) { update(it) }

                        EditorTab.COLOR -> IllustrationColorControls(
                            draft.illustrationStyle,
                            enabled = !saving,
                        ) { update(draft.copy(illustrationStyle = it.normalized())) }

                        EditorTab.FRAME -> IllustrationFrameControls(
                            draft.illustrationStyle,
                            enabled = !saving,
                        ) { update(draft.copy(illustrationStyle = it.normalized())) }
                    }

                    if (error) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Не удалось сохранить изменения. Попробуй ещё раз.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
internal fun IllustrationDetailControls(
    characterId: String?,
    details: IllustrationDetails,
    enabled: Boolean = true,
    onChange: (IllustrationDetails) -> Unit
) {
    EditorSection("Лицо и настроение")
    LayerChoiceRow(
        options = IllustrationLayerRegistry.forCharacter(characterId, IllustrationLayerCategory.EXPRESSION),
        selectedId = details.expressionLayerId ?: "expression.${details.expression.name.lowercase()}",
        enabled = enabled,
        onSelect = { onChange(details.copy(expressionLayerId = it.id)) }
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Румянец", Modifier.weight(1f), color = Palette.Text)
        Switch(details.blush, { onChange(details.copy(blush = it)) }, enabled = enabled)
    }

    EditorSection("Волосы")
    LayerChoiceRow(
        options = IllustrationLayerRegistry.forCharacter(characterId, IllustrationLayerCategory.HAIR),
        selectedId = details.hairLayerId ?: accentLayerId("hair", details.hairAccent),
        enabled = enabled,
        onSelect = { onChange(details.copy(hairLayerId = it.id)) }
    )

    EditorSection("Одежда")
    LayerChoiceRow(
        options = IllustrationLayerRegistry.forCharacter(characterId, IllustrationLayerCategory.OUTFIT),
        selectedId = details.outfitLayerId ?: accentLayerId("outfit", details.outfitAccent),
        enabled = enabled,
        onSelect = { onChange(details.copy(outfitLayerId = it.id)) }
    )

    EditorSection("Аксессуар")
    LayerChoiceRow(
        options = IllustrationLayerRegistry.forCharacter(characterId, IllustrationLayerCategory.ACCESSORY),
        selectedId = details.accessoryLayerId ?: accessoryLayerId(details.accessory),
        enabled = enabled,
        onSelect = { onChange(details.copy(accessoryLayerId = it.id)) }
    )

    TextButton(
        enabled = enabled,
        onClick = { onChange(IllustrationDetails()) }
    ) { Text("Сбросить детали") }
}

@Composable
private fun EditorSection(title: String) {
    Spacer(Modifier.height(8.dp))
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = Palette.Text,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun LayerChoiceRow(
    options: List<IllustrationLayerAsset>,
    selectedId: String,
    enabled: Boolean,
    onSelect: (IllustrationLayerAsset) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selectedId == option.id,
                onClick = { onSelect(option) },
                enabled = enabled,
                label = { Text(option.title) }
            )
        }
    }
}

private fun accentLayerId(prefix: String, accent: IllustrationAccent): String =
    "$prefix.${accent.name.lowercase()}"

private fun accessoryLayerId(accessory: IllustrationAccessory): String =
    "accessory.${accessory.name.lowercase()}"

@Composable
private fun IllustrationLookControls(
    style: IllustrationStyle,
    enabled: Boolean,
    onChange: (IllustrationStyle) -> Unit,
) {
    val s = style.normalized()
    EditorCard("Быстрый образ", "Готовая атмосфера одним нажатием") {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PresetChip("Натуральный", enabled) { onChange(IllustrationStyle.NATURAL.copy(mirrored = s.mirrored, motion = s.motion)) }
            PresetChip("Мягкий", enabled) { onChange(IllustrationStyle.SOFT.copy(mirrored = s.mirrored, motion = s.motion)) }
            PresetChip("Тёплый", enabled) { onChange(IllustrationStyle.WARM.copy(mirrored = s.mirrored, motion = s.motion)) }
            PresetChip("Манга", enabled) { onChange(IllustrationStyle.MANGA.copy(mirrored = s.mirrored, motion = s.motion)) }
            PresetChip("Кино", enabled) { onChange(IllustrationStyle.CINEMATIC.copy(mirrored = s.mirrored, motion = s.motion)) }
        }
    }
    EditorCard("Живость", "Поведение персонажа на экране") {
        ToggleRow("Плавное движение", "Лёгкое дыхание и микродвижение", s.motion, enabled) {
            onChange(s.copy(motion = it))
        }
        ToggleRow("Зеркально", "Развернуть персонажа в другую сторону", s.mirrored, enabled) {
            onChange(s.copy(mirrored = it))
        }
    }
    TextButton(enabled = enabled, onClick = { onChange(IllustrationStyle()) }) {
        Text("Сбросить образ")
    }
}

@Composable
private fun IllustrationColorControls(
    style: IllustrationStyle,
    enabled: Boolean,
    onChange: (IllustrationStyle) -> Unit,
) {
    val s = style.normalized()
    EditorCard("Цвет и свет", "Тонкая настройка общей иллюстрации") {
        EditorSlider("Насыщенность", s.saturation, 0f..1.6f, enabled, "${(s.saturation * 100).roundToInt()}%") { onChange(s.copy(saturation = it)) }
        EditorSlider("Теплота", s.warmth, -1f..1f, enabled, signedPercent(s.warmth)) { onChange(s.copy(warmth = it)) }
        EditorSlider("Яркость", s.brightness, -0.35f..0.35f, enabled, signedPercent(s.brightness / 0.35f)) { onChange(s.copy(brightness = it)) }
        EditorSlider("Контраст", s.contrast, 0.7f..1.35f, enabled, "${(s.contrast * 100).roundToInt()}%") { onChange(s.copy(contrast = it)) }
    }
    TextButton(enabled = enabled, onClick = {
        onChange(s.copy(saturation = 1f, warmth = 0f, brightness = 0f, contrast = 1f))
    }) { Text("Сбросить цвет") }
}

@Composable
private fun IllustrationFrameControls(
    style: IllustrationStyle,
    enabled: Boolean,
    onChange: (IllustrationStyle) -> Unit,
) {
    val s = style.normalized()
    EditorCard("Композиция", "Положение персонажа внутри кадра") {
        EditorSlider("Масштаб", s.zoom, 0.8f..1.6f, enabled, "%.2f".format(s.zoom)) { onChange(s.copy(zoom = it)) }
        EditorSlider("Горизонталь", s.offsetX, -0.25f..0.25f, enabled, "${(s.offsetX * 100).roundToInt()}%") { onChange(s.copy(offsetX = it)) }
        EditorSlider("Вертикаль", s.offsetY, -0.25f..0.25f, enabled, "${(s.offsetY * 100).roundToInt()}%") { onChange(s.copy(offsetY = it)) }
        EditorSlider("Наклон", s.rotation, -8f..8f, enabled, "${s.rotation.roundToInt()}°") { onChange(s.copy(rotation = it)) }
    }
    TextButton(enabled = enabled, onClick = {
        onChange(s.copy(zoom = 1f, offsetX = 0f, offsetY = 0f, rotation = 0f))
    }) { Text("Центрировать кадр") }
}

@Composable
private fun EditorCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Palette.Glass),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Palette.Text, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Palette.TextDim)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Palette.Text, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, color = Palette.TextDim, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun PresetChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    AssistChip(onClick = onClick, enabled = enabled, label = { Text(label) })
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
        Text(label, Modifier.weight(1f), color = Palette.Text, style = MaterialTheme.typography.bodyMedium)
        Text(valueText, color = Palette.TextDim, style = MaterialTheme.typography.labelMedium)
    }
    Slider(value = value, onValueChange = onChange, valueRange = range, enabled = enabled)
}

private fun signedPercent(value: Float): String {
    val percent = (value * 100).roundToInt()
    return if (percent > 0) "+$percent%" else "$percent%"
}

