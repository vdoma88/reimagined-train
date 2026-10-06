package com.animate.companion.snap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.animate.companion.model.*
import com.animate.companion.ui.avatar.AvatarView
import com.animate.companion.ui.avatar.CartoonAvatar
import com.animate.companion.ui.avatar.CartoonLookControls
import com.animate.companion.ui.theme.AniMateTheme
import org.junit.Rule
import org.junit.Test

class CartoonSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test fun outfitsFacesAccessoriesAndSmallHeads() {
        paparazzi.snapshot("wardrobe") {
            AniMateTheme {
                Column(Modifier.fillMaxSize().background(Color(0xFFFFF4E8))) {
                    repeat(4) { row ->
                        Row(Modifier.fillMaxWidth().height(155.dp)) {
                            repeat(2) { col ->
                                val i = row * 2 + col
                                val appearance = Appearance(illustrationId = CartoonLook.STYLE_ID,
                                    cartoonLook = CartoonLook(hair = i, face = i % 3, top = i,
                                        bottom = i % 5, eyes = i % 2, mouth = i % 2, accessory = i % 6, shoeStyle = i % 2))
                                Column(Modifier.weight(1f)) {
                                    val gender = if (row < 2) Gender.FEMALE else Gender.MALE
                                    AvatarView(appearance, gender, Modifier.fillMaxWidth().height(120.dp),
                                        animated = false, fullBody = true)
                                    AvatarView(appearance, gender, Modifier.size(32.dp), animated = false, headOnly = true)
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().height(140.dp)) {
                        CartoonAvatar(CartoonLook(), IllustrationStyle(mirrored = true, warmth = 0.5f),
                            Modifier.weight(1f).fillMaxHeight(), false, false, 1f, 0f, true, true)
                        CartoonAvatar(CartoonLook(hair = 3), IllustrationStyle(motion = false),
                            Modifier.weight(1f).fillMaxHeight(), true, false, 0f, 0f, false, false)
                    }
                }
            }
        }
    }

    @Test fun rasterEditorControls() {
        paparazzi.snapshot("controls") {
            AniMateTheme {
                Column(Modifier.fillMaxSize().background(Color(0xFF241A36)).padding(12.dp)) {
                    val appearance = Appearance(illustrationId = CartoonLook.STYLE_ID, cartoonLook = CartoonLook(hair = 1, top = 1))
                    AvatarView(appearance, Gender.NEUTRAL, Modifier.fillMaxWidth().height(180.dp), animated = false)
                    CartoonLookControls(appearance, enabled = true) {}
                }
            }
        }
    }
}
