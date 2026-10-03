package com.animate.companion.ui.avatar

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import com.animate.companion.model.Appearance
import com.animate.companion.model.Emotion
import com.animate.companion.model.Gender
import com.animate.companion.model.IllustratedCharacters
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animated avatar: breathes, blinks at random intervals and moves its mouth while [talking].
 * Set [animated] to false for static thumbnails (lists, option pickers).
 */
@Composable
fun AvatarView(
    appearance: Appearance,
    gender: Gender,
    modifier: Modifier = Modifier,
    emotion: Emotion = Emotion.NEUTRAL,
    talking: Boolean = false,
    animated: Boolean = true,
    headOnly: Boolean = false,
    fullBody: Boolean = false,
) {
    val illustration = IllustratedCharacters.find(appearance.illustrationId)
    if (illustration != null && !(appearance.useCharacterLook && appearance.characterLook != null)) {
        IllustratedAvatar(illustration, modifier, headOnly, fullBody, animated, appearance.illustrationStyle, appearance.illustrationDetails)
        return
    }
    var blink by remember { mutableFloatStateOf(0f) }
    var mouthOpen by remember { mutableStateOf(false) }

    if (animated && (!appearance.useCharacterLook || appearance.illustrationStyle.motion)) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(Random.nextLong(1800, 4800))
                blink = 1f
                delay(110)
                blink = 0f
                if (Random.nextFloat() < 0.2f) { // occasional double blink
                    delay(140)
                    blink = 1f
                    delay(100)
                    blink = 0f
                }
            }
        }
        LaunchedEffect(talking) {
            if (!talking) {
                mouthOpen = false
                return@LaunchedEffect
            }
            while (true) {
                mouthOpen = !mouthOpen
                delay(Random.nextLong(90, 170))
            }
        }
    }

    val breathPhase = if (animated && (!appearance.useCharacterLook || appearance.illustrationStyle.motion)) {
        val t = rememberInfiniteTransition(label = "breath")
        t.animateFloat(
            0f, (2 * Math.PI).toFloat(),
            infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart),
            label = "breathPhase",
        ).value
    } else {
        0f
    }

    if (illustration != null && appearance.useCharacterLook && appearance.characterLook != null) {
        LayeredCharacter(appearance.characterLook, appearance.illustrationStyle, modifier,
            headOnly, fullBody, emotion, if (animated && appearance.illustrationStyle.motion) blink else 0f,
            sin(breathPhase) * 1.5f, animated && appearance.illustrationStyle.motion && mouthOpen)
        return
    }

    Canvas(modifier.clipToBounds()) {
        drawAvatar(
            appearance, gender,
            AvatarPose(emotion = emotion, blink = blink, breath = sin(breathPhase) * 0.7f, mouthOpen = mouthOpen),
            headOnly = headOnly,
        )
    }
}
