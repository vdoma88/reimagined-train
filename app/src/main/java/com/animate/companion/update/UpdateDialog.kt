package com.animate.companion.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.animate.companion.ui.theme.Palette

/** App-wide update prompt. Silent states (checking, up to date) are shown in Settings instead. */
@Composable
fun UpdateDialog(manager: UpdateManager) {
    val state by manager.state.collectAsStateWithLifecycle()
    when (val s = state) {
        is UpdateState.Available -> AlertDialog(
            onDismissRequest = { manager.dismiss() },
            title = { Text("Вышло обновление ✨") },
            text = {
                Column {
                    Text(
                        "Версия ${s.release.version} (у тебя ${manager.installedVersion})" +
                            (if (s.release.apkSize > 0) sizeLabel(s.release.apkSize) else ""),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    if (s.release.notes.isNotBlank()) {
                        Text(
                            s.release.notes,
                            modifier = Modifier.padding(top = 8.dp).heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
                            style = MaterialTheme.typography.bodySmall,
                            color = Palette.TextDim,
                        )
                    }
                    Text(
                        "Персонажи и переписка сохранятся.",
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { manager.download(s.release) }) { Text("Обновить") } },
            dismissButton = { TextButton(onClick = { manager.skip(s.release) }) { Text("Позже") } },
        )

        is UpdateState.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Скачиваю обновление…") },
            text = {
                Column {
                    LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                    Text("${(s.progress * 100).toInt()}%", modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {},
        )

        is UpdateState.ReadyToInstall -> AlertDialog(
            onDismissRequest = { manager.dismiss() },
            title = { Text("Обновление скачано 🎉") },
            text = {
                Text(
                    "Нажми «Установить». Если Android спросит разрешение устанавливать приложения из AniMate — " +
                        "разреши его (можно попросить взрослого), вернись сюда и нажми «Установить» ещё раз.",
                )
            },
            confirmButton = { TextButton(onClick = { manager.install(s.file) }) { Text("Установить") } },
            dismissButton = { TextButton(onClick = { manager.dismiss() }) { Text("Позже") } },
        )

        is UpdateState.Failed -> if (s.release != null) {
            AlertDialog(
                onDismissRequest = { manager.dismiss() },
                title = { Text("Не получилось обновить") },
                text = { Text(s.message) },
                confirmButton = { TextButton(onClick = { manager.download(s.release) }) { Text("Ещё раз") } },
                dismissButton = { TextButton(onClick = { manager.dismiss() }) { Text("Закрыть") } },
            )
        }

        else -> Unit
    }
}

internal fun sizeLabel(bytes: Long): String = " · %.1f МБ".format(bytes / 1_048_576.0)
