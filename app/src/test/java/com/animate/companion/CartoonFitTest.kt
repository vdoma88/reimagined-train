package com.animate.companion

import com.animate.companion.model.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import android.graphics.BitmapFactory
import app.cash.paparazzi.Paparazzi
import org.junit.Rule

class CartoonFitTest {
    @get:Rule val paparazzi = Paparazzi()
    private val assets = File("src/main/assets").takeIf { it.exists() } ?: File("app/src/main/assets")
    private val specs = Json.decodeFromString<Map<String, CartoonSpriteSpec>>(File(assets, "cartoon_layers.json").readText())

    @Test fun everyBuildAndGenderFitsIncludingBreathingAndTallShoes() {
        for (gender in Gender.selectable) for (build in 0..2) for ((key, spec) in specs) {
            val b = CartoonRig.bounds(key, spec, CartoonLook(build = build), gender)
            assertTrue("$gender/$build/$key top", b.top - CartoonRig.BREATH_MARGIN >= CartoonRig.FRAME_TOP)
            assertTrue("$gender/$build/$key bottom", b.bottom + CartoonRig.BREATH_MARGIN <= CartoonRig.FRAME_TOP + CartoonRig.FULL_HEIGHT)
            assertTrue("$gender/$build/$key left", b.left >= 0)
            assertTrue("$gender/$build/$key right", b.right <= CartoonRig.FRAME_WIDTH)
        }
    }

    @Test fun trousersCoverShoeShaftsAndTopsCoverWaistbands() {
        for (bottom in CartoonLook.bottomNames.indices) for (shoe in 0..1) {
            val layers = CartoonLook(bottom = bottom, shoeStyle = shoe).layers(gender = Gender.MALE)
            val shoeKey = if (shoe == 0) "boots" else "sneakers"
            assertTrue(layers.indexOf(shoeKey) < layers.indexOf("bottom.$bottom"))
            assertTrue(layers.indexOf("bottom.$bottom") < layers.indexOf("top.0"))
        }
    }

    @Test fun opaqueWaistbandsOverlapAllTopHems() {
        fun alpha(key: String, x: Int, y: Int): Int {
            val s = specs.getValue(key)
            val image = images.getValue(key)
            val sx = ((x - s.x) / s.width * image.width).toInt()
            val sy = (s.sourceY + (y - s.y) / s.height * (image.height - s.sourceY)).toInt()
            return if (x < s.x || y < s.y || x >= s.x+s.width || y >= s.y+s.height || sx !in 0 until image.width || sy !in 0 until image.height) 0 else image.getPixel(sx,sy) ushr 24
        }
        for (top in CartoonLook.topNames.indices) for (bottom in CartoonLook.bottomNames.indices) {
            for (x in listOf(188, 200, 212)) {
                assertTrue("gap at top.$top/bottom.$bottom x=$x",
                    (375..420).count { y -> alpha("top.$top",x,y) > 128 && alpha("bottom.$bottom",x,y) > 128 } >= 3)
            }
        }
    }
    private val images by lazy { specs.mapValues { (_, s) -> requireNotNull(BitmapFactory.decodeFile(File(assets, "cartoon/${s.file}").absolutePath)) } }
}
