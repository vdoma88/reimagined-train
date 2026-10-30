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
import com.animate.companion.ui.components.GlassCard
import com.animate.companion.ui.components.GradientButton
import com.animate.companion.ui.components.MangaStage
import com.animate.companion.ui.theme.AniMateTheme
import com.animate.companion.ui.theme.Palette
import org.junit.Rule
import org.junit.Test

class ExperienceRefreshSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test fun moodAndReadableCards() = paparazzi.snapshot("moods") {
        AniMateTheme {
            Column(Modifier.fillMaxSize().background(Palette.Night).padding(16.dp)) {
                Row(Modifier.fillMaxWidth().height(290.dp)) {
                    for (mood in listOf(Emotion.NEUTRAL, Emotion.HAPPY)) {
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            MangaStage(mood, Modifier.fillMaxSize())
                            AvatarView(Appearance(illustrationId = CartoonLook.STYLE_ID, cartoonLook = CartoonLook()),
                                Gender.FEMALE, Modifier.fillMaxSize(), emotion = mood, animated = false, fullBody = true)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Привет! Рада нашей встрече.", color = Palette.Text)
                        Text("Настроение и история персонажа", color = Palette.TextDim)
                    }
                }
                Spacer(Modifier.height(16.dp))
                GradientButton("Продолжить", {}, Modifier.fillMaxWidth())
            }
        }
    }
}
