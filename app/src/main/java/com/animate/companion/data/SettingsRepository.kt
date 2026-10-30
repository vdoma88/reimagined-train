package com.animate.companion.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.animate.companion.llm.Provider
import com.animate.companion.llm.ProviderConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class CharacterVoiceSettings(val voiceId: String = "", val pitch: Float = 1f, val rate: Float = 1f)

data class AppSettings(
    val userName: String = "",
    val provider: Provider = Provider.GEMINI,
    val keys: Map<Provider, String> = emptyMap(),
    val models: Map<Provider, String> = emptyMap(),
    val autoFallback: Boolean = true,
    val temperature: Float = 0.9f,
    val musicEnabled: Boolean = true,
    val musicVolume: Float = 0.5f,
    val musicTrack: Int = 0,
    val voiceEnabled: Boolean = true,
    val voiceVolume: Float = 0.8f,
    val sfxEnabled: Boolean = true,
    val autoUpdate: Boolean = true,
    val speechEnabled: Boolean = true,
    val speechRate: Float = 1f,
    val speechVoiceId: String = "",
) {
    fun config(p: Provider) = ProviderConfig(p, keys[p].orEmpty(), models[p].orEmpty().ifBlank { p.defaultModel })

    fun isUsable(p: Provider) = !p.needsKey || !keys[p].isNullOrBlank()

    /** True once any provider has a key, i.e. chatting will work reliably. */
    val hasAnyKey get() = keys.values.any { it.isNotBlank() }

    /** The selected provider first, then every other usable provider, keyless Pollinations last. */
    fun providerChain(): List<ProviderConfig> {
        val primary = listOf(provider).filter { isUsable(it) }
        if (!autoFallback && primary.isNotEmpty()) return primary.map { config(it) }
        val rest = Provider.entries.filter { it != provider && it != Provider.POLLINATIONS && it != Provider.XAI && isUsable(it) }
        return (primary + rest + Provider.POLLINATIONS).distinct().map { config(it) }
    }
}

class SettingsRepository(private val context: Context) {
    private fun characterVoiceKey(id: Long) = stringPreferencesKey("character_voice_$id")
    private fun characterPitchKey(id: Long) = floatPreferencesKey("character_pitch_$id")
    private fun characterRateKey(id: Long) = floatPreferencesKey("character_rate_$id")

    fun characterVoice(id: Long): Flow<CharacterVoiceSettings> = context.dataStore.data.map {
        CharacterVoiceSettings(it[characterVoiceKey(id)].orEmpty(), it[characterPitchKey(id)] ?: 1f, it[characterRateKey(id)] ?: 1f)
    }

    suspend fun setCharacterVoice(id: Long, voice: CharacterVoiceSettings) = context.dataStore.edit {
        it[characterVoiceKey(id)] = voice.voiceId
        it[characterPitchKey(id)] = voice.pitch.coerceIn(0.7f, 1.3f)
        it[characterRateKey(id)] = voice.rate.coerceIn(0.7f, 1.3f)
    }

    suspend fun clearCharacterVoice(id: Long) = context.dataStore.edit {
        it.remove(characterVoiceKey(id)); it.remove(characterPitchKey(id)); it.remove(characterRateKey(id))
    }
    private object K {
        val userName = stringPreferencesKey("user_name")
        val provider = stringPreferencesKey("provider")
        val autoFallback = booleanPreferencesKey("auto_fallback")
        val temperature = floatPreferencesKey("temperature")
        val musicEnabled = booleanPreferencesKey("music_enabled")
        val musicVolume = floatPreferencesKey("music_volume")
        val musicTrack = intPreferencesKey("music_track")
        val voiceEnabled = booleanPreferencesKey("voice_enabled")
        val voiceVolume = floatPreferencesKey("voice_volume")
        val sfxEnabled = booleanPreferencesKey("sfx_enabled")
        val autoUpdate = booleanPreferencesKey("auto_update")
        val speechEnabled = booleanPreferencesKey("speech_enabled")
        val speechRate = floatPreferencesKey("speech_rate")
        val speechVoiceId = stringPreferencesKey("speech_voice_id")
        fun key(p: Provider) = stringPreferencesKey("key_${p.name}")
        fun model(p: Provider) = stringPreferencesKey("model_${p.name}")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    private fun Preferences.toSettings() = AppSettings(
        userName = this[K.userName].orEmpty(),
        provider = this[K.provider]?.let { runCatching { Provider.valueOf(it) }.getOrNull() } ?: Provider.GEMINI,
        keys = Provider.entries.associateWith { this[K.key(it)].orEmpty() },
        models = Provider.entries.associateWith { this[K.model(it)].orEmpty() },
        autoFallback = this[K.autoFallback] ?: true,
        temperature = this[K.temperature] ?: 0.9f,
        musicEnabled = this[K.musicEnabled] ?: true,
        musicVolume = this[K.musicVolume] ?: 0.5f,
        musicTrack = this[K.musicTrack] ?: 0,
        voiceEnabled = this[K.voiceEnabled] ?: true,
        voiceVolume = this[K.voiceVolume] ?: 0.8f,
        sfxEnabled = this[K.sfxEnabled] ?: true,
        autoUpdate = this[K.autoUpdate] ?: true,
        speechEnabled = this[K.speechEnabled] ?: true,
        speechRate = this[K.speechRate] ?: 1f,
        speechVoiceId = this[K.speechVoiceId].orEmpty(),
    )

    suspend fun setUserName(v: String) = context.dataStore.edit { it[K.userName] = v }
    suspend fun setProvider(v: Provider) = context.dataStore.edit { it[K.provider] = v.name }
    suspend fun setKey(p: Provider, v: String) = context.dataStore.edit { it[K.key(p)] = v.trim() }
    suspend fun setModel(p: Provider, v: String) = context.dataStore.edit { it[K.model(p)] = v.trim() }
    suspend fun setAutoFallback(v: Boolean) = context.dataStore.edit { it[K.autoFallback] = v }
    suspend fun setTemperature(v: Float) = context.dataStore.edit { it[K.temperature] = v }
    suspend fun setMusicEnabled(v: Boolean) = context.dataStore.edit { it[K.musicEnabled] = v }
    suspend fun setMusicVolume(v: Float) = context.dataStore.edit { it[K.musicVolume] = v }
    suspend fun setMusicTrack(v: Int) = context.dataStore.edit { it[K.musicTrack] = v }
    suspend fun setVoiceEnabled(v: Boolean) = context.dataStore.edit { it[K.voiceEnabled] = v }
    suspend fun setVoiceVolume(v: Float) = context.dataStore.edit { it[K.voiceVolume] = v }
    suspend fun setSfxEnabled(v: Boolean) = context.dataStore.edit { it[K.sfxEnabled] = v }
    suspend fun setAutoUpdate(v: Boolean) = context.dataStore.edit { it[K.autoUpdate] = v }
    suspend fun setSpeechEnabled(v: Boolean) = context.dataStore.edit { it[K.speechEnabled] = v }
    suspend fun setSpeechVoiceId(v: String) = context.dataStore.edit { it[K.speechVoiceId] = v }
    suspend fun setSpeechRate(v: Float) = context.dataStore.edit { it[K.speechRate] = v }
}
