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
import com.animate.companion.ui.avatar.CartoonAvatar
import com.animate.companion.ui.theme.AniMateTheme
import org.junit.Rule
import org.junit.Test

class CartoonFitSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test fun tallAndStockyWardrobeAndPortraits() {
        for (page in 0..3) paparazzi.snapshot("fit_$page") {
            AniMateTheme {
                Column(Modifier.fillMaxSize().background(Color(0xFFFFF4E8))) {
                    repeat(2) { row ->
                        Row(Modifier.fillMaxWidth().height(310.dp)) {
                            for (gender in Gender.selectable) {
                                val i = page * 2 + row
                                val look = CartoonLook(hair = i, top = i, bottom = i % 5,
                                    shoeStyle = i % 2, build = if (row == 0) 2 else 1)
                                Column(Modifier.weight(1f)) {
                                    Text("${gender.label}: $i", color = Color.Black)
                                    CartoonAvatar(look, IllustrationStyle(mirrored = row == 1),
                                        Modifier.fillMaxWidth().height(285.dp), false, true,
                                        0f, if (row == 0) 1.5f else -1.5f, false, false, gender)
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().height(120.dp)) {
                        CartoonAvatar(CartoonLook(top = page*2), IllustrationStyle(),
                            Modifier.weight(1f).fillMaxHeight(), false, false, 0f, 0f, false, false, Gender.FEMALE)
                        CartoonAvatar(CartoonLook(hair = page*2), IllustrationStyle(),
                            Modifier.weight(1f).fillMaxHeight(), true, false, 0f, 0f, false, false, Gender.MALE)
                    }
                }
            }
        }
    }
}
