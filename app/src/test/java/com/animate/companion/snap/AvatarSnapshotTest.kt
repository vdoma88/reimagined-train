package com.animate.companion.snap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.animate.companion.model.Appearance
import com.animate.companion.model.Emotion
import com.animate.companion.model.Gender
import com.animate.companion.ui.avatar.AvatarView
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

class AvatarSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(screenWidth = 1600, screenHeight = 1400, density = com.android.resources.Density.MEDIUM), maxPercentDifference = 1.0)

    @Test
    fun grid() {
        paparazzi.snapshot {
            Column(Modifier.background(Color(0xFF1B1140))) {
                for (row in 0 until 4) {
                    Row {
                        for (col in 0 until 6) {
                            val i = row * 6 + col
                            val g = Gender.entries[i % 3]
                            val a = Appearance.random(g, Random(i + 5))
                            Column {
                                AvatarView(a, g, Modifier.size(260.dp), emotion = Emotion.entries[i % Emotion.entries.size], animated = false)
                                Text("${g.name} ${Emotion.entries[i % Emotion.entries.size].tag}", color = Color.White, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

class AvatarDetailSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(screenWidth = 1500, screenHeight = 1000, density = com.android.resources.Density.MEDIUM), maxPercentDifference = 1.0)

    @Test
    fun emotions() {
        val a = Appearance(hairStyle = 1, bangs = 1, hairColor = 0, eyeStyle = 0, eyeColor = 3, ears = 1, accessory = 1, ahoge = true, fang = true)
        val m = Appearance(hairStyle = 4, bangs = 4, hairColor = 3, eyeStyle = 1, eyeColor = 0, outfit = 6, outfitColor = 3, skinTone = 1)
        paparazzi.snapshot {
            Column(Modifier.background(Color(0xFF1B1140))) {
                Row {
                    listOf(Emotion.NEUTRAL, Emotion.HAPPY, Emotion.LAUGH, Emotion.SHY, Emotion.ANGRY).forEach {
                        AvatarView(a, Gender.FEMALE, Modifier.size(300.dp), emotion = it, animated = false)
                    }
                }
                Row {
                    listOf(Emotion.SAD, Emotion.SURPRISED, Emotion.SMUG, Emotion.LOVE, Emotion.THINKING).forEach {
                        AvatarView(m, Gender.MALE, Modifier.size(300.dp), emotion = it, animated = false)
                    }
                }
                Row {
                    listOf(a, m, a.copy(ears = 3, hairStyle = 0), m.copy(ears = 6), a.copy(ears = 5, hairStyle = 6)).forEach {
                        AvatarView(it, Gender.FEMALE, Modifier.size(120.dp), animated = false, headOnly = true)
                    }
                    listOf(a.copy(ears = 3), m.copy(ears = 6, accessory = 4)).forEach {
                        AvatarView(it, Gender.FEMALE, Modifier.size(260.dp), animated = false)
                    }
                }
            }
        }
    }
}

class AvatarBigSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(screenWidth = 1200, screenHeight = 600, density = com.android.resources.Density.MEDIUM), maxPercentDifference = 1.0)

    @Test
    fun big() {
        val a = Appearance(hairStyle = 0, bangs = 1, hairColor = 1, eyeStyle = 0, eyeColor = 6, ears = 2, accessory = 3, outfit = 5, outfitColor = 2, ahoge = true)
        val b = Appearance(hairStyle = 5, bangs = 3, hairColor = 6, eyeStyle = 4, eyeColor = 4, ears = 0, accessory = 7, outfit = 3, outfitColor = 3, skinTone = 2)
        paparazzi.snapshot {
            Row(Modifier.background(Color(0xFF1B1140))) {
                AvatarView(a, Gender.FEMALE, Modifier.size(600.dp), emotion = Emotion.LOVE, animated = false)
                AvatarView(b, Gender.FEMALE, Modifier.size(600.dp), emotion = Emotion.SMUG, animated = false)
            }
        }
    }
}

class AvatarCatalogSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, maxPercentDifference = 1.0)

    @Test
    fun hairAndOutfits() {
        val p = com.animate.companion.model.AppearancePresets
        paparazzi.snapshot {
            Column(Modifier.background(Color(0xFF1B1140))) {
                (p.hairStyles.indices.toList() + p.outfits.indices.map { 100 + it }).chunked(5).forEach { row ->
                    Row {
                        row.forEach { i ->
                            val a = if (i < 100) Appearance(hairStyle = i, hairColor = i % 11, bangs = i % 5, eyeColor = i % 9)
                            else Appearance(outfit = i - 100, outfitColor = (i - 100) % 8, hairStyle = 2, hairColor = 3)
                            Column {
                                AvatarView(a, Gender.FEMALE, Modifier.size(78.dp), animated = false)
                                Text(if (i < 100) p.hairStyles[i].label else p.outfits[i - 100].label, color = Color.White, fontSize = 7.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

class AvatarEyesAccessoriesSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, maxPercentDifference = 1.0)

    @Test
    fun eyesAndAccessories() {
        val p = com.animate.companion.model.AppearancePresets
        paparazzi.snapshot {
            Column(Modifier.background(Color(0xFF1B1140))) {
                (p.eyeStyles.indices.toList() + p.accessories.indices.map { 100 + it }).chunked(5).forEach { row ->
                    Row {
                        row.forEach { i ->
                            val a = if (i < 100) Appearance(eyeStyle = i, eyeColor = i % 9, hairStyle = 2, hairColor = 3)
                            else Appearance(accessory = i - 100, hairStyle = 2, hairColor = (i - 100) % 11, outfitColor = 5)
                            Column {
                                AvatarView(a, Gender.FEMALE, Modifier.size(78.dp), animated = false, headOnly = i < 100)
                                Text(if (i < 100) p.eyeStyles[i].label else p.accessories[i - 100].label, color = Color.White, fontSize = 7.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
