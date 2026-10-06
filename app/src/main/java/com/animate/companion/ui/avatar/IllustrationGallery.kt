package com.animate.companion.ui.avatar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.animate.companion.model.Appearance
import com.animate.companion.model.IllustratedCharacters
import com.animate.companion.ui.components.GlassCard
import com.animate.companion.ui.theme.Palette

@Composable
fun IllustrationGallery(selectedId: String?, onSelect: (String?) -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalGrid(columns = GridCells.Adaptive(145.dp), modifier = modifier,
        contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text("Собери героя из нарисованных слоёв в «Лесных приключениях» или выбери готовый образ.",
                color = Palette.TextDim, style = MaterialTheme.typography.bodySmall)
        }
        items(IllustratedCharacters.all, key = { it.id }) { art ->
            GlassCard(selected = art.id == selectedId, onClick = { onSelect(art.id) }) {
                Column(Modifier.padding(10.dp)) {
                    AvatarView(Appearance(illustrationId = art.id), art.gender,
                        Modifier.fillMaxWidth().aspectRatio(0.8f), animated = false)
                    Text(art.title, color = Palette.Text, style = MaterialTheme.typography.titleSmall)
                    Text(art.subtitle, color = Palette.TextDim, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            GlassCard(selected = selectedId == null, onClick = { onSelect(null) }) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text("Настраиваемый персонаж", color = Palette.Text, style = MaterialTheme.typography.titleSmall)
                    Text("Прежний конструктор: меняй волосы, глаза, одежду и мимику по отдельности.",
                        color = Palette.TextDim, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

