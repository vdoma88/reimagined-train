package com.animate.companion.ui.chat

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.animate.companion.data.ReminderEntity
import com.animate.companion.reminders.ReminderKind
import com.animate.companion.reminders.ReminderRequest
import com.animate.companion.reminders.ReminderTags
import com.animate.companion.ui.components.GlassCard
import com.animate.companion.ui.theme.Palette
import java.time.ZonedDateTime

/** The character's proposal: nothing is set until the user taps «Поставить». */
@Composable
internal fun ReminderProposalCard(r: ReminderRequest, onAccept: () -> Unit, onDismiss: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), selected = true) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${r.kind.emoji} ${r.kind.label} · ${ReminderTags.describeTime(r.triggerAt, ZonedDateTime.now())}",
                color = Palette.Amber,
                style = MaterialTheme.typography.titleSmall,
            )
            Text("«${r.text}»", color = Palette.Text, style = MaterialTheme.typography.bodyMedium)
            if (r.kind == ReminderKind.ALARM) {
                Text("Появится в приложении «Часы» телефона.", color = Palette.TextDim, style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Не надо") }
                TextButton(onClick = onAccept) { Text("Поставить") }
            }
        }
    }
}

@Composable
internal fun NoticeCard(text: String) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text(text, Modifier.padding(horizontal = 14.dp, vertical = 10.dp), color = Palette.Text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun RemindersDialog(
    reminders: List<ReminderEntity>,
    exactAllowed: Boolean,
    notificationsAllowed: Boolean,
    onCancel: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val now = ZonedDateTime.now()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Напоминания") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Напиши в чат: «разбуди в 7:30», «напомни завтра в 18:00 полить цветы» или «поставь таймер на 15 минут». " +
                        "Будильники попадают в «Часы» телефона, остальное — сюда.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextDim,
                )
                if (!notificationsAllowed) {
                    Text("Уведомления выключены — напоминание появится только в чате.", color = Palette.Danger, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    }) { Text("Включить уведомления") }
                }
                if (!exactAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Text("Телефон может присылать напоминания с опозданием.", color = Palette.TextDim, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    }) { Text("Разрешить точное время") }
                }
                if (reminders.isEmpty()) {
                    Text("Пока ничего не поставлено.", color = Palette.Text)
                } else {
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(reminders, key = { it.id }) { r ->
                            val kind = runCatching { ReminderKind.valueOf(r.kind) }.getOrDefault(ReminderKind.REMINDER)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("${kind.emoji} ${ReminderTags.describeTime(r.triggerAt, now)}", color = Palette.Amber, style = MaterialTheme.typography.labelLarge)
                                    Text(r.text, color = Palette.Text, style = MaterialTheme.typography.bodyMedium)
                                }
                                IconButton(onClick = { onCancel(r.id) }) {
                                    Icon(Icons.Rounded.Delete, "Удалить напоминание", tint = Palette.TextDim)
                                }
                            }
                            Spacer(Modifier.padding(2.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}
