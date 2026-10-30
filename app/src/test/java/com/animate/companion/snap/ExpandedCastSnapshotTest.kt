package com.animate.companion.snap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.animate.companion.model.*
import com.animate.companion.ui.avatar.AvatarView
import com.animate.companion.ui.theme.AniMateTheme
import org.junit.Rule
import org.junit.Test

class ExpandedCastSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)
    @Test fun presetsInFullBodyAndPortrait() = paparazzi.snapshot("cast") {
        AniMateTheme {
            Column(Modifier.fillMaxSize().background(Color(0xFFFFF4E8))) {
                CartoonLooks.all.chunked(2).forEachIndexed { row, group ->
                    Row(Modifier.fillMaxWidth().height(210.dp)) {
                        group.forEachIndexed { col, preset ->
                            val gender = if ((row + col) % 2 == 0) Gender.FEMALE else Gender.MALE
                            val a = Appearance(illustrationId = CartoonLook.STYLE_ID, cartoonLook = preset.applyTo(CartoonLook()))
                            Column(Modifier.weight(1f)) {
                                Text(preset.title, color = Color.Black, maxLines = 1)
                                Row(Modifier.height(172.dp)) {
                                    AvatarView(a, gender, Modifier.weight(1f).fillMaxHeight(), animated = false, fullBody = true)
                                    AvatarView(a, gender, Modifier.weight(1f).fillMaxHeight(), animated = false, headOnly = true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
