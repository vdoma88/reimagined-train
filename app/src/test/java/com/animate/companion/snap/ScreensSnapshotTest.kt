package com.animate.companion.snap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.animate.companion.data.CharacterEntity
import com.animate.companion.data.MessageEntity
import com.animate.companion.model.Appearance
import com.animate.companion.model.Emotion
import com.animate.companion.model.Gender
import com.animate.companion.ui.avatar.AvatarView
import com.animate.companion.ui.chat.ErrorCard
import com.animate.companion.ui.chat.Hero
import com.animate.companion.ui.chat.MessageBubble
import com.animate.companion.ui.chat.TypingBubble
import com.animate.companion.ui.components.GradientButton
import com.animate.companion.ui.components.MangaStage
import com.animate.companion.ui.create.EmotionStrip
import com.animate.companion.ui.components.SakuraBackground
import com.animate.companion.ui.create.AppearanceStep
import com.animate.companion.ui.create.CreatorViewModel
import com.animate.companion.ui.create.IdentityStep
import com.animate.companion.ui.create.PersonaStep
import com.animate.companion.ui.create.StepTabs
import com.animate.companion.ui.theme.AniMateTheme
import org.junit.Rule
import org.junit.Test

class ScreensSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, maxPercentDifference = 1.0)

    private val character = CharacterEntity(
        id = 1, name = "Сакура Хошино", gender = Gender.FEMALE.name,
        appearanceJson = Appearance(hairStyle = 1, hairColor = 0, eyeColor = 3, ears = 1, accessory = 1, ahoge = true).toJson(),
        archetypeId = "tsundere", professionId = "maid", directionId = "romance", affection = 34,
    )

    @Test
    fun chat() {
        val msgs = listOf(
            MessageEntity(1, 1, "assistant", "*отворачивается и краснеет* Х-хмф! Я Сакура. Н-не думай, что я рада тебя видеть, бака!", "angry"),
            MessageEntity(2, 1, "user", "Привет! Ты сегодня очень милая :)"),
            MessageEntity(3, 1, "assistant", "*роняет поднос* Ч-что?! Глупости не говори… *тихо* …спасибо.", "shy"),
        )
        paparazzi.snapshot {
            AniMateTheme {
                SakuraBackground {
                    Column(Modifier.fillMaxSize().padding(top = 24.dp)) {
                        Hero(character, Emotion.SHY, false, "Хававаа~") {}
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            msgs.forEach { MessageBubble(it, isLastAssistant = it.id == 3L, onDelete = {}, onRegenerate = {}) }
                            TypingBubble()
                            ErrorCard("Google Gemini: HTTP 429 quota exceeded", {}, {}, {})
                        }
                    }
                }
            }
        }
    }

    @Test
    fun creator() {
        for (step in 0..2) {
            val vm = CreatorViewModel().apply {
                appearance = Appearance(hairStyle = 1, hairColor = 6, eyeColor = 2, ears = 3, accessory = 5)
                gender = Gender.FEMALE
                name = "Хината Цукиширо"
                this.step = step
            }
            paparazzi.snapshot("step$step") {
                AniMateTheme {
                    SakuraBackground {
                        Column(Modifier.fillMaxSize().padding(top = 24.dp)) {
                            Box(Modifier.fillMaxWidth().weight(0.42f), contentAlignment = Alignment.Center) {
                                MangaStage(Emotion.HAPPY, Modifier.aspectRatio(1f))
                                AvatarView(vm.appearance, vm.gender, Modifier.aspectRatio(1f), emotion = Emotion.HAPPY, animated = false)
                            }
                            EmotionStrip(Emotion.HAPPY) {}
                            StepTabs(step) {}
                            Box(Modifier.weight(0.58f).fillMaxWidth()) {
                                when (step) {
                                    0 -> AppearanceStep(vm) {}
                                    1 -> IdentityStep(vm, {}, {})
                                    else -> PersonaStep(vm) {}
                                }
                            }
                            GradientButton("Далее", {}, Modifier.fillMaxWidth().padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}
