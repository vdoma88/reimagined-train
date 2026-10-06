package com.animate.companion.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.AlarmClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.animate.companion.MainActivity
import com.animate.companion.R
import com.animate.companion.data.AppDatabase
import com.animate.companion.data.MessageEntity
import com.animate.companion.data.ReminderEntity
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Result of confirming a proposal, phrased for the chat. */
sealed interface ReminderOutcome {
    val message: String

    data class ClockAlarm(val time: String) : ReminderOutcome {
        override val message get() = "⏰ Будильник на $time добавлен в «Часы» телефона."
    }
    data class Scheduled(val time: String, val exact: Boolean) : ReminderOutcome {
        override val message get() = "🔔 Напомню $time." +
            if (exact) "" else " Телефон может прислать его с опозданием на несколько минут."
    }
    data object TooMany : ReminderOutcome {
        override val message get() = "Уже слишком много напоминаний. Удали ненужные в меню ⋮ → «Напоминания»."
    }
    data object InPast : ReminderOutcome {
        override val message get() = "Это время уже прошло. Попроси персонажа выбрать новое."
    }
}

/**
 * Alarms go to the phone's Clock app (a real alarm with sound, snooze and the user's ringtone).
 * Reminders and timers are ours: a row in the database plus an AlarmManager wake-up that posts
 * a notification from the character and drops a line into the chat.
 */
class Reminders(private val context: Context, private val db: AppDatabase) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun observe(characterId: Long) = db.reminders().observe(characterId)

    /** Active reminders for the prompt, so the character can say what is already set. */
    suspend fun forPrompt(characterId: Long): List<Pair<String, Long>> =
        db.reminders().forCharacter(characterId).filter { it.triggerAt > System.currentTimeMillis() }.map { it.text to it.triggerAt }

    suspend fun add(characterId: Long, request: ReminderRequest, characterName: String): ReminderOutcome {
        val now = ZonedDateTime.now()
        if (request.triggerAt <= now.toInstant().toEpochMilli()) return ReminderOutcome.InPast
        val time = ReminderTags.describeTime(request.triggerAt, now)
        if (request.kind == ReminderKind.ALARM && setClockAlarm(request, characterName)) {
            return ReminderOutcome.ClockAlarm(time)
        }
        if (db.reminders().count() >= MAX_ACTIVE) return ReminderOutcome.TooMany
        val id = db.reminders().insert(
            ReminderEntity(characterId = characterId, kind = request.kind.name, text = request.text, triggerAt = request.triggerAt),
        )
        schedule(id, request.triggerAt)
        return ReminderOutcome.Scheduled(time, canScheduleExact())
    }

    suspend fun cancel(id: Long) {
        alarms.cancel(pendingIntent(id))
        db.reminders().delete(id)
    }

    /** After a reboot or an app update the system forgets our wake-ups: set them again. */
    suspend fun rescheduleAll() {
        val now = System.currentTimeMillis()
        db.reminders().all().forEach { schedule(it.id, maxOf(it.triggerAt, now + 5_000)) }
    }

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    fun notificationsAllowed(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun schedule(id: Long, at: Long) {
        val pi = pendingIntent(id)
        // The exact-alarm permission can be revoked at any moment: fall back to an inexact wake-up.
        val exact = canScheduleExact() && runCatching { alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi) }.isSuccess
        if (!exact) alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    private fun pendingIntent(id: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        id.toInt(),
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE).putExtra(EXTRA_ID, id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Hands the alarm to the Clock app without opening it; false when no clock app accepts it. */
    private fun setClockAlarm(request: ReminderRequest, characterName: String): Boolean {
        val at = ZonedDateTime.ofInstant(Instant.ofEpochMilli(request.triggerAt), ZoneId.systemDefault())
        // The Clock app only takes a time of day and rings at its next occurrence:
        // anything further than 24 hours away becomes our own reminder instead.
        if (request.triggerAt - System.currentTimeMillis() > 24 * 60 * 60 * 1000L) return false
        val intent = Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR, at.hour)
            .putExtra(AlarmClock.EXTRA_MINUTES, at.minute)
            .putExtra(AlarmClock.EXTRA_MESSAGE, "$characterName: ${request.text}".take(60))
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) == null) return false
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    /** Called by [ReminderReceiver] when a reminder is due. */
    internal suspend fun fire(id: Long) {
        val r = db.reminders().get(id) ?: return // cancelled, or the character was deleted
        db.reminders().delete(id)
        val c = db.characters().get(r.characterId) ?: return
        val kind = runCatching { ReminderKind.valueOf(r.kind) }.getOrDefault(ReminderKind.REMINDER)
        val line = "*${if (kind == ReminderKind.TIMER) "машет песочными часами" else "звонит в колокольчик"}* " +
            "${kind.emoji} Напоминаю: ${r.text}!"
        db.messages().insert(MessageEntity(characterId = c.id, role = MessageEntity.ROLE_ASSISTANT, text = line, emotion = "surprised"))
        db.characters().update(c.copy(lastMessageAt = System.currentTimeMillis(), lastMessagePreview = line.take(120), lastEmotion = "surprised"))
        notify(r, c.name, kind)
    }

    private fun notify(r: ReminderEntity, characterName: String, kind: ReminderKind) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel()
        val open = PendingIntent.getActivity(
            context,
            r.characterId.toInt(),
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_CHARACTER_ID, r.characterId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle("${kind.emoji} $characterName напоминает")
            .setContentText(r.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(r.text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(r.id.toInt(), n) }
    }

    private fun ensureChannel() {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Напоминания персонажей", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Напоминания и таймеры, которые ты поставил в чате"
                },
            )
        }
    }

    companion object {
        const val MAX_ACTIVE = 30
        const val CHANNEL = "reminders"
        const val ACTION_FIRE = "com.animate.companion.REMINDER"
        const val EXTRA_ID = "reminder_id"
    }
}
