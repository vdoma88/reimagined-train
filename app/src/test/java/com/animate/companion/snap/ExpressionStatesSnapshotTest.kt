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

class ExpressionStatesSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)
    @Test fun emotionsKeepFacesAndAccessoriesAligned() = paparazzi.snapshot("expressions") {
        AniMateTheme {
            Column(Modifier.fillMaxSize().background(Color(0xFFFFF4E8))) {
                listOf(Emotion.NEUTRAL, Emotion.HAPPY, Emotion.SURPRISED, Emotion.THINKING).forEach { emotion ->
                    Row(Modifier.fillMaxWidth().height(190.dp)) {
                        listOf(Gender.FEMALE, Gender.MALE).forEach { gender ->
                            val look = CartoonLook(hair = if (gender == Gender.MALE) 8 else 9, eyes = 1, accessory = 1, pin = 1)
                            Column(Modifier.weight(1f)) {
                                Text(emotion.label, color = Color.Black)
                                AvatarView(Appearance(illustrationId = CartoonLook.STYLE_ID, cartoonLook = look), gender,
                                    Modifier.fillMaxWidth().height(165.dp), emotion = emotion, animated = false, headOnly = true)
                            }
                        }
                    }
                }
            }
        }
    }
}
