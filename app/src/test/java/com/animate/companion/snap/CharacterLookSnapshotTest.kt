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
import com.animate.companion.ui.theme.AniMateTheme
import org.junit.Rule
import org.junit.Test

class CharacterLookSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)
    @Test fun fullBodyPortraitAndThumbnailForAllParts() {
        for (i in 0..5) {
            val look = CharacterLook(hair=i, hairColor=i, face=i%3, skin=i, eyes=i%4, eyeColor=i,
                outfit=i%4, outfitColor=i, accessory=i%5, freckles=i%2==0)
            paparazzi.snapshot("look_$i") {
                AniMateTheme {
                    Column(Modifier.fillMaxSize().background(Color(0xFFF3EBE4))) {
                        Row(Modifier.fillMaxWidth().height(500.dp)) {
                            AvatarView(Appearance(illustrationId="classic", characterLook=look, useCharacterLook=true), Gender.FEMALE, Modifier.weight(1f).fillMaxHeight(), animated=false, fullBody=true)
                            AvatarView(Appearance(illustrationId="classic", characterLook=look, useCharacterLook=true), Gender.FEMALE, Modifier.weight(1f).height(270.dp), animated=false, headOnly=true)
                        }
                        AvatarView(Appearance(illustrationId="classic", characterLook=look, useCharacterLook=true), Gender.FEMALE, Modifier.size(48.dp), animated=false, headOnly=true)
                    }
                }
            }
        }
    }
}
