package com.animate.companion.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.animate.companion.AppContainer
import com.animate.companion.audio.SfxType
import com.animate.companion.data.CharacterEntity
import com.animate.companion.model.Appearance
import com.animate.companion.model.Emotion
import com.animate.companion.model.Gender
import com.animate.companion.model.PersonaPresets
import com.animate.companion.ui.avatar.AvatarView
import com.animate.companion.ui.components.AvatarFrame
import com.animate.companion.ui.components.GlassCard
import com.animate.companion.ui.components.GradientButton
import com.animate.companion.ui.components.Pill
import com.animate.companion.ui.components.SakuraBackground
import com.animate.companion.ui.theme.Palette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(container: AppContainer, onCreate: () -> Unit, onOpen: (Long) -> Unit, onSettings: () -> Unit) {
    val characters by container.chat.observeCharacters().collectAsStateWithLifecycle(initialValue = null)
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)

    SakuraBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "AniMate",
                        style = MaterialTheme.typography.headlineLarge.merge(TextStyle(brush = Palette.accentWide)),
                    )
                    Text("твои аниме-компаньоны", color = Palette.TextDim, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = { container.sound.sfx(SfxType.TAP); onSettings() }) {
                    Icon(Icons.Rounded.Settings, "Настройки", tint = Palette.Text)
                }
            }

            if (settings?.hasAnyKey == false) KeyBanner { container.sound.sfx(SfxType.TAP); onSettings() }

            val list = characters
            when {
                list == null -> Spacer(Modifier.weight(1f))
                list.isEmpty() -> EmptyState(Modifier.weight(1f))
                else -> LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(list, key = { it.id }) { c ->
                        CharacterRow(c) { container.sound.sfx(SfxType.TAP); onOpen(c.id) }
                    }
                }
            }

            GradientButton(
                "Создать персонажа",
                onClick = { container.sound.sfx(SfxType.SPARKLE); onCreate() },
                icon = Icons.Rounded.AutoAwesome,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            )
        }
    }
}

@Composable
private fun KeyBanner(onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp), selected = true, onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🔑", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Подключи бесплатный ИИ", style = MaterialTheme.typography.titleSmall, color = Palette.Text)
                Text(
                    "Персонажам нужен «мозг». Бесплатный ключ Google Gemini получается за минуту, без карты.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextDim,
                )
            }
            Text("→", style = MaterialTheme.typography.titleLarge, color = Palette.Sakura)
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    val demo = remember { Appearance(hairColor = 0, hairStyle = 1, eyeColor = 3, ears = 1, accessory = 1, ahoge = true) }
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        AvatarView(demo, Gender.FEMALE, Modifier.size(260.dp), emotion = Emotion.HAPPY)
        Spacer(Modifier.height(16.dp))
        Text("Здесь пока пусто~", style = MaterialTheme.typography.headlineMedium, color = Palette.Text)
        Spacer(Modifier.height(8.dp))
        Text(
            "Собери своего персонажа из классических аниме-штампов, дай ему характер — и болтай сколько хочешь!",
            color = Palette.TextDim,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun CharacterRow(c: CharacterEntity, onClick: () -> Unit) {
    val arch = PersonaPresets.archetype(c.archetypeId)
    val prof = PersonaPresets.profession(c.professionId)
    GlassCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AvatarFrame(Modifier.size(72.dp)) {
                AvatarView(c.appearance, c.genderEnum, Modifier.size(72.dp), emotion = Emotion.fromTag(c.lastEmotion), animated = false, headOnly = true)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.name, style = MaterialTheme.typography.titleMedium, color = Palette.Text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatTime(c.lastMessageAt), style = MaterialTheme.typography.labelSmall, color = Palette.TextDim)
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pill("${arch.emoji} ${arch.label}")
                    Pill("${prof.emoji} ${prof.label(c.genderEnum)}", color = Palette.Sky)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        c.lastMessagePreview.ifBlank { "Новая встреча…" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Palette.TextDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Rounded.Favorite, null, tint = Palette.Sakura, modifier = Modifier.size(14.dp))
                    Text(" ${c.affection}", style = MaterialTheme.typography.labelSmall, color = Palette.Sakura)
                }
            }
        }
    }
}

private fun formatTime(ts: Long): String {
    val now = System.currentTimeMillis()
    val pattern = if (now - ts < 24 * 3600_000L) "HH:mm" else "d MMM"
    return SimpleDateFormat(pattern, Locale("ru")).format(Date(ts))
}
