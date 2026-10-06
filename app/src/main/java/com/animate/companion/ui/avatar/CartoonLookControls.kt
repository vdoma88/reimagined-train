package com.animate.companion.ui.avatar

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.animate.companion.model.Appearance
import com.animate.companion.model.CartoonLook
import com.animate.companion.model.Gender
import com.animate.companion.ui.theme.Palette

@Composable
internal fun CartoonLookControls(appearance: Appearance, enabled: Boolean, gender: Gender = Gender.FEMALE, onChange: (Appearance) -> Unit) {
    val look = appearance.resolvedCartoonLook(gender)
    fun update(value: CartoonLook) = onChange(appearance.copy(cartoonLook = value.normalized()))
    Text("Нарисованные слои", color = Palette.Text, style = MaterialTheme.typography.titleMedium)
    Text("Собери своего героя: все детали меняются независимо и сразу видны в превью.",
        color = Palette.TextDim, style = MaterialTheme.typography.bodySmall)
    CartoonOptions("Телосложение", CartoonLook.buildNames, look.build, enabled) { update(look.copy(build = it)) }
    CartoonOptions("Причёска", CartoonLook.hairNames, look.hair, enabled) { update(look.copy(hair = it)) }
    CartoonOptions("Лицо", if (gender == Gender.MALE) CartoonLook.maleFaceNames else CartoonLook.femaleFaceNames, look.face, enabled) { update(look.copy(face = it)) }
    CartoonOptions("Глаза", if (gender == Gender.MALE) listOf("Выразительные брови", "Прищур") else listOf("Открытый взгляд", "Миндалевидные"), look.eyes, enabled) { update(look.copy(eyes = it)) }
    CartoonOptions("Рот", listOf("Лёгкая улыбка", "Широкая улыбка"), look.mouth, enabled) { update(look.copy(mouth = it)) }
    CartoonOptions("Верх", CartoonLook.topNames, look.top, enabled) { update(look.copy(top = it)) }
    CartoonOptions("Низ", CartoonLook.bottomNames, look.bottom, enabled) { update(look.copy(bottom = it)) }
    CartoonOptions("Обувь", listOf("Босиком", "Ботинки", "Кеды"), if (look.boots) look.shoeStyle + 1 else 0, enabled) {
        update(look.copy(boots = it > 0, shoeStyle = (it - 1).coerceAtLeast(0)))
    }
    CartoonOptions("Аксессуары", listOf("Нет", "Очки", "Кепка", "Очки и кепка", "Шапка", "Очки и шапка"), look.accessory, enabled) { update(look.copy(accessory = it)) }
    TextButton(enabled = enabled, onClick = { update(CartoonLook(hair = if (gender == Gender.MALE) 0 else 1)) }) { Text("Сбросить внешность") }
}

@Composable
private fun CartoonOptions(title: String, options: List<String>, selected: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    Spacer(Modifier.height(12.dp))
    Text(title, color = Palette.Text, style = MaterialTheme.typography.titleSmall)
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, label ->
            FilterChip(selected = i == selected, enabled = enabled, onClick = { onSelect(i) }, label = { Text(label) })
        }
    }
}
