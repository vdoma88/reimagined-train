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

class OutfitPinSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)
    @Test fun pinsOnDifferentOutfitsAndBuilds() = paparazzi.snapshot("pins") {
        AniMateTheme {
            Column(Modifier.fillMaxSize().background(Color(0xFFFFF4E8))) {
                (1..3).forEach { pin ->
                    Row(Modifier.fillMaxWidth().height(230.dp)) {
                        (0..2).forEach { build ->
                            val gender = if (build == 1) Gender.MALE else Gender.FEMALE
                            val look = CartoonLook(hair = if (gender == Gender.MALE) 8 else 9, top = (pin * 3 + build) % CartoonLook.topNames.size, build = build, pin = pin)
                            Column(Modifier.weight(1f)) {
                                Text(CartoonLook.pinNames[pin], color = Color.Black)
                                AvatarView(Appearance(illustrationId = CartoonLook.STYLE_ID, cartoonLook = look), gender,
                                    Modifier.fillMaxWidth().height(200.dp), animated = false, fullBody = true)
                            }
                        }
                    }
                }
            }
        }
    }
}
