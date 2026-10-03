package com.animate.companion.ui.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.animate.companion.model.*
import com.animate.companion.ui.theme.Palette

@Composable
internal fun CharacterLookControls(appearance: Appearance, enabled: Boolean, onChange: (Appearance) -> Unit) {
    val look = (appearance.characterLook ?: CharacterLook.forCharacter(appearance.illustrationId)).normalized()
    fun update(value: CharacterLook) = onChange(appearance.copy(characterLook = value.normalized(), useCharacterLook = true))
    Text("Редактируемая версия", color = Palette.Text, style = MaterialTheme.typography.titleMedium)
    Text("Послойная векторная рисовка по мотивам образа. Исходную иллюстрацию можно вернуть в любой момент.",
        color = Palette.TextDim, style = MaterialTheme.typography.bodySmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = !appearance.useCharacterLook, enabled = enabled,
            onClick = { onChange(appearance.copy(useCharacterLook = false)) }, label = { Text("Иллюстрация") })
        FilterChip(selected = appearance.useCharacterLook, enabled = enabled,
            onClick = { update(look) }, label = { Text("Редактировать") })
    }
    if (!appearance.useCharacterLook) return
    LookOptions("Причёска", listOf("Волны", "Боб", "Каре", "Хвост", "Два хвоста", "Короткая"), look.hair, enabled) { update(look.copy(hair = it)) }
    LookColors("Цвет волос", hairPalette, look.hairColor, enabled) { update(look.copy(hairColor = it)) }
    LookOptions("Форма лица", listOf("Мягкая", "Сердечко", "Угловатая"), look.face, enabled) { update(look.copy(face = it)) }
    LookColors("Тон кожи", skinPalette, look.skin, enabled) { update(look.copy(skin = it)) }
    LookOptions("Форма глаз", listOf("Миндаль", "Узкие", "Округлые", "Мягкие"), look.eyes, enabled) { update(look.copy(eyes = it)) }
    LookColors("Радужка", irisPalette, look.eyeColor, enabled) { update(look.copy(eyeColor = it)) }
    LookOptions("Брови", listOf("Дуга", "Прямые", "Приподнятые"), look.brows, enabled) { update(look.copy(brows = it)) }
    LookOptions("Рот", listOf("Спокойный", "Улыбка", "Открытый"), look.mouth, enabled) { update(look.copy(mouth = it)) }
    LookOptions("Одежда", listOf("Жакет и юбка", "Худи и карго", "Походный образ", "Платье"), look.outfit, enabled) { update(look.copy(outfit = it)) }
    LookColors("Цвет одежды", clothPalette, look.outfitColor, enabled) { update(look.copy(outfitColor = it)) }
    LookOptions("Аксессуар", listOf("Нет", "Очки", "Бант", "Серьги", "Чокер"), look.accessory, enabled) { update(look.copy(accessory = it)) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = look.blush, enabled = enabled, onClick = { update(look.copy(blush = !look.blush)) }, label = { Text("Румянец") })
        FilterChip(selected = look.freckles, enabled = enabled, onClick = { update(look.copy(freckles = !look.freckles)) }, label = { Text("Веснушки") })
    }
    TextButton(enabled = enabled, onClick = { update(CharacterLook.forCharacter(appearance.illustrationId)) }) { Text("Сбросить внешность") }
}

@Composable
private fun LookOptions(title: String, options: List<String>, selected: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    Spacer(Modifier.height(12.dp))
    Text(title, color = Palette.Text, style = MaterialTheme.typography.titleSmall)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, label ->
            FilterChip(selected = i == selected, enabled = enabled, onClick = { onSelect(i) }, label = { Text(label) })
        }
    }
}

@Composable
private fun LookColors(title: String, colors: List<Long>, selected: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    Text(title, color = Palette.Text, style = MaterialTheme.typography.labelLarge)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        colors.forEachIndexed { i, color ->
            FilterChip(selected = i == selected, enabled = enabled, onClick = { onSelect(i) },
                label = { Text(if (i == selected) "✓ ${i+1}" else "${i+1}") },
                leadingIcon = { Box(Modifier.size(20.dp).background(Color(color), androidx.compose.foundation.shape.CircleShape)) })
        }
    }
}
