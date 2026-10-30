package com.animate.companion.ui.avatar

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.animate.companion.model.Appearance
import com.animate.companion.model.Emotion
import com.animate.companion.model.Gender
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
    var blink by remember { mutableFloatStateOf(0f) }
    var mouthOpen by remember { mutableStateOf(false) }

    if (animated && appearance.illustrationStyle.motion) {
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

    val breathPhase = if (animated && appearance.illustrationStyle.motion) {
        val t = rememberInfiniteTransition(label = "breath")
        t.animateFloat(
            0f, (2 * Math.PI).toFloat(),
            infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart),
            label = "breathPhase",
        ).value
    } else {
        0f
    }

    val moving = animated && appearance.illustrationStyle.motion
    val smoothBlink by animateFloatAsState(if (moving) blink else 0f,
        animationSpec = tween(if (blink > 0f) 55 else 115), label = "blink")
    val smoothMouth by animateFloatAsState(if (moving && talking && mouthOpen) 1f else 0f,
        animationSpec = tween(75), label = "mouth")
    val originalLook = appearance.resolvedCartoonLook(gender)
    val expressiveLook = if (!talking && emotion in listOf(Emotion.HAPPY, Emotion.LAUGH, Emotion.LOVE))
        originalLook.copy(mouth = 1) else originalLook
    val animatedModifier = if (moving && !headOnly) modifier.graphicsLayer {
        // Small whole-character sway shares the breath clock, so garments never drift apart.
        rotationZ = sin(breathPhase) * 0.45f
    } else modifier
    CartoonAvatar(expressiveLook, appearance.illustrationStyle, animatedModifier,
        headOnly, fullBody, if (moving) smoothBlink else 0f, sin(breathPhase) * 1.5f,
        moving && talking, moving && mouthOpen, gender,
        mouthProgress = if (moving && talking) smoothMouth else 0f)
}
