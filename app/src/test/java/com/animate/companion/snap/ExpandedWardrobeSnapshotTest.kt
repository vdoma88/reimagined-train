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

class ExpandedWardrobeSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)
    @Test fun newHairstylesAndGarments() {
        for (page in 0..3) paparazzi.snapshot("catalog_$page") {
            AniMateTheme {
                Row(Modifier.fillMaxSize().background(Color(0xFFF3EBE4)).padding(8.dp)) {
                    for (column in 0..1) {
                        val index = page * 2 + column
                        val look = CharacterLook(hair = index + 6, outfit = index + 4, hairColor = index, outfitColor = index)
                        val appearance = Appearance(illustrationId = "classic", characterLook = look, useCharacterLook = true)
                        Column(Modifier.weight(1f)) {
                            Text(CharacterLookCatalog.hairstyles[look.hair], color = Color.Black)
                            AvatarView(appearance, Gender.NEUTRAL, Modifier.fillMaxWidth().height(150.dp), animated = false, headOnly = true)
                            Text(CharacterLookCatalog.outfits[look.outfit], color = Color.Black)
                            AvatarView(appearance, Gender.NEUTRAL, Modifier.fillMaxWidth().height(440.dp), animated = false, fullBody = true)
                            AvatarView(appearance, Gender.NEUTRAL, Modifier.size(48.dp), animated = false, headOnly = true)
                        }
                    }
                }
            }
        }
    }
}
