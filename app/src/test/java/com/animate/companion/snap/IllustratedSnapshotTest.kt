package com.animate.companion.snap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.animate.companion.model.Appearance
import com.animate.companion.model.IllustratedCharacters
import com.animate.companion.ui.avatar.AvatarView
import com.animate.companion.ui.theme.AniMateTheme
import org.junit.Rule
import org.junit.Test

class IllustratedSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test fun portraitFullBodyAndThumbnail() {
        IllustratedCharacters.all.forEach { art ->
            paparazzi.snapshot(art.id) {
                AniMateTheme {
                    Column(Modifier.fillMaxSize().background(Color(0xFF241A36)).padding(16.dp)) {
                        Text(art.title)
                        Row(Modifier.fillMaxWidth().height(320.dp)) {
                            AvatarView(Appearance(illustrationId = art.id), art.gender,
                                Modifier.weight(1f).fillMaxHeight(), animated = false)
                            AvatarView(Appearance(illustrationId = art.id), art.gender,
                                Modifier.weight(1f).fillMaxHeight(), animated = false, fullBody = true)
                        }
                        Row(Modifier.fillMaxWidth().background(Color(0xFFFFF4E8)).padding(12.dp)) {
                            AvatarView(Appearance(illustrationId = art.id), art.gender,
                                Modifier.size(72.dp), animated = false, headOnly = true)
                            AvatarView(Appearance(illustrationId = art.id), art.gender,
                                Modifier.size(44.dp), animated = false, headOnly = true)
                        }
                    }
                }
            }
        }
    }
}
