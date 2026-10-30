package com.animate.companion.snap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.animate.companion.model.CartoonLook
import com.animate.companion.model.Gender
import com.animate.companion.model.IllustrationStyle
import com.animate.companion.ui.avatar.CartoonAvatar
import com.animate.companion.ui.theme.AniMateTheme
import org.junit.Rule
import org.junit.Test

class FaceAnimationSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)
    @Test fun blinkAndSpeechIntermediateFrames() = paparazzi.snapshot("faces") {
        AniMateTheme {
            Column(Modifier.fillMaxSize().background(Color(0xFFFFF4E8))) {
                for (blink in listOf(0f, 0.5f, 1f)) {
                    Row(Modifier.fillMaxWidth().height(240.dp)) {
                        for (mouth in listOf(0f, 0.5f, 1f)) {
                            Column(Modifier.weight(1f)) {
                                Text("$blink / $mouth", color = Color.Black)
                                CartoonAvatar(CartoonLook(), IllustrationStyle(), Modifier.fillMaxWidth().height(205.dp),
                                    headOnly = true, fullBody = false, blink = blink, breath = 0f,
                                    speaking = true, mouthOpen = mouth > 0f, gender = Gender.FEMALE,
                                    mouthProgress = mouth)
                            }
                        }
                    }
                }
            }
        }
    }
}
