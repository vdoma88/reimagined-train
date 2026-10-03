package com.animate.companion.ui.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.animate.companion.AppContainer
import com.animate.companion.data.AppSettings
import com.animate.companion.ui.theme.Palette
import com.animate.companion.update.UpdateState
import kotlinx.coroutines.launch

@Composable
internal fun UpdatesSection(container: AppContainer, settings: AppSettings) {
    val scope = rememberCoroutineScope()
    val state by container.updates.state.collectAsStateWithLifecycle()
    Section("Обновления") {
        Text("Версия приложения: ${container.updates.installedVersion}", color = Palette.Text)
        ToggleRow("Проверять обновления автоматически", settings.autoUpdate) {
            scope.launch { container.settings.setAutoUpdate(it) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { container.updates.check(manual = true) }) { Text("Проверить сейчас") }
            val status = when (val s = state) {
                UpdateState.Checking -> "⏳ Проверяю…"
                UpdateState.UpToDate -> "✅ У тебя последняя версия"
                is UpdateState.Failed -> if (s.release == null) "❌ ${s.message}" else null
                is UpdateState.Available -> "✨ Есть версия ${s.release.version}"
                else -> null
            }
            status?.let { Text(it, color = Palette.TextDim, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
