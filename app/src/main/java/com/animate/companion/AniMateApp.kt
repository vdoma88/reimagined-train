package com.animate.companion

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.animate.companion.audio.SoundDirector
import com.animate.companion.data.AppDatabase
import com.animate.companion.data.SettingsRepository
import com.animate.companion.llm.ChatRepository
import com.animate.companion.llm.LlmClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.animate.companion.update.UpdateManager
import com.animate.companion.audio.Speaker
import com.animate.companion.reminders.Reminders

/** Manual dependency container; small enough not to need a DI framework. */
class AppContainer(app: android.content.Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val db = AppDatabase.create(app)
    val settings = SettingsRepository(app)
    val llm = LlmClient()
    val chat = ChatRepository(db, settings, llm, scope)
    val sound = SoundDirector(scope)
    val updates = UpdateManager(app, scope)
    val reminders = Reminders(app, db)

    /** Created on first use: binding the TTS service is not free and tests never need it. */
    private val speakerLazy = lazy {
        Speaker(app).apply { onSpeakingChanged = { speaking -> scope.launch { sound.duck(speaking) } } }
    }
    val speaker: Speaker by speakerLazy

    fun stopSpeech() {
        if (speakerLazy.isInitialized()) speaker.stop()
    }
}

class AniMateApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.settings.settings.onEach { container.sound.apply(it) }.launchIn(container.scope)
        // Wake-ups are lost on force-stop; re-arming the pending ones is cheap and idempotent.
        container.scope.launch(kotlinx.coroutines.Dispatchers.IO) { runCatching { container.reminders.rescheduleAll() } }
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                container.sound.onForeground()
                container.scope.launch { if (container.settings.current().autoUpdate) container.updates.autoCheck() }
            }
            override fun onStop(owner: LifecycleOwner) {
                container.sound.onBackground()
                container.stopSpeech()
            }
        })
    }
}
