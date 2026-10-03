package com.animate.companion.snap

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.animate.companion.AppContainer
import com.animate.companion.data.AppSettings
import com.animate.companion.llm.Provider
import com.animate.companion.ui.components.SakuraBackground
import com.animate.companion.ui.settings.AiSection
import com.animate.companion.ui.theme.AniMateTheme
import org.junit.Rule
import org.junit.Test

class SettingsSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 2600), maxPercentDifference = 1.0)

    private fun shot(name: String, settings: AppSettings) {
        val container = AppContainer(paparazzi.context)
        paparazzi.snapshot(name) {
            AniMateTheme {
                SakuraBackground {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) { AiSection(container, settings) }
                }
            }
        }
    }

    @Test
    fun firstRun() = shot("first_run", AppSettings(provider = Provider.GEMINI))

    @Test
    fun ready() = shot("ready", AppSettings(provider = Provider.GROQ, keys = mapOf(Provider.GROQ to "gsk_test_key_123456")))
}
