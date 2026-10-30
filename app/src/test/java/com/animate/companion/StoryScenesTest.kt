package com.animate.companion

import com.animate.companion.model.CharacterNoteGenerator
import com.animate.companion.model.StoryScenes
import org.junit.Assert.*
import org.junit.Test

class StoryScenesTest {
    @Test fun onlyRecognizedFeaturesUnlockAChoiceInEveryScene() {
        StoryScenes.all.forEach { scene ->
            assertEquals(2, StoryScenes.choices(scene, "обычное описание").size)
            assertEquals(2, StoryScenes.choices(scene, "Особенность: выдуманная способность.").size)
            CharacterNoteGenerator.perks.forEach { perk ->
                val choices = StoryScenes.choices(scene, "Путешественник. Особенность: $perk.")
                assertEquals(3, choices.size)
                assertTrue(choices.last().contains(perk))
                val message = StoryScenes.message(scene, choices.last())
                assertTrue(message.contains(scene.setup))
                assertTrue(message.contains(perk))
            }
        }
    }
}
