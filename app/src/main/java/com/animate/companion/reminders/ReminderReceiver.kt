package com.animate.companion.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.animate.companion.AniMateApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires due reminders and restores them after a reboot or an app update. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminders = (context.applicationContext as AniMateApp).container.reminders
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    Reminders.ACTION_FIRE -> reminders.fire(intent.getLongExtra(Reminders.EXTRA_ID, -1))
                    Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> reminders.rescheduleAll()
                }
            } finally {
                pending.finish()
            }
        }
    }
}
