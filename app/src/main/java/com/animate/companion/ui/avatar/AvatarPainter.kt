package com.animate.companion.ui.avatar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import com.animate.companion.model.Appearance
import com.animate.companion.model.AppearancePresets
import com.animate.companion.model.Emotion
import com.animate.companion.model.Gender

/** Dynamic, per-frame state of the avatar. */
data class AvatarPose(
    val emotion: Emotion = Emotion.NEUTRAL,
    /** 0 = open, 1 = closed. */
    val blink: Float = 0f,
    /** Vertical breathing offset in avatar units. */
    val breath: Float = 0f,
    /** True while the mouth should be open (talking animation frame). */
    val mouthOpen: Boolean = false,
)

private fun argb(v: Long) = Color(
    red = ((v shr 16) and 0xFF) / 255f,
    green = ((v shr 8) and 0xFF) / 255f,
    blue = (v and 0xFF) / 255f,
    alpha = ((v shr 24) and 0xFF) / 255f,
)

private fun Color.darker(f: Float) = lerp(this, Color(0xFF1A1030), f)
private fun Color.lighter(f: Float) = lerp(this, Color.White, f)

private inline fun path(block: Path.() -> Unit) = Path().apply(block)

/**
 * Draws the character in a 100x100 unit space scaled to the canvas.
 * Layers: back hair → ears behind → body → face → features → front hair → ears/accessories on top.
 */
fun DrawScope.drawAvatar(a: Appearance, gender: Gender, pose: AvatarPose, headOnly: Boolean = false) {
    val p = AppearancePresets
    val skin = argb(p.skinTones[a.skinTone.coerceIn(0, p.skinTones.lastIndex)].argb)
    val hair = argb(p.hairColors[a.hairColor.coerceIn(0, p.hairColors.lastIndex)].argb)
    val eye = argb(p.eyeColors[a.eyeColor.coerceIn(0, p.eyeColors.lastIndex)].argb)
    val eye2 = if (a.heterochromia) argb(p.eyeColors[(a.eyeColor + 3) % p.eyeColors.size].argb) else eye
    val cloth = argb(p.outfitColors[a.outfitColor.coerceIn(0, p.outfitColors.lastIndex)].argb)

    val k = size.minDimension / 100f
    val zoom = if (headOnly) 1.7f else 1f
    withTransform({
        translate((size.width - 100f * k) / 2f, (size.height - 100f * k) / 2f)
        scale(k * zoom, k * zoom, Offset.Zero)
        if (headOnly) {
            translate(-20.6f, -14f)
        } else {
            // Leave headroom for bunny ears, halos and ahoge.
            scale(0.92f, 0.92f, Offset.Zero)
            translate(4.3f, 8.5f)
        }
    }) {
        val c = Ctx(this, a, gender, pose, skin, hair, eye, eye2, cloth)
        c.backHair()
        translate(0f, pose.breath * 0.4f) { c.body() }
        translate(0f, pose.breath) {
            c.earsBehind()
            c.hairBesideFace()
            c.face()
            c.features()
            c.frontHair()
            c.brows()
            c.earsOnTop()
            c.accessory()
            c.expressionMarks()
        }
    }
}

private class Ctx(
    val d: DrawScope,
    val a: Appearance,
    val g: Gender,
    val pose: AvatarPose,
    val skin: Color,
    val hair: Color,
    val eye: Color,
    val eye2: Color,
    val cloth: Color,
) {
    // Softer manga ink: still crisp, but less harsh against pastel skin/hair.
    val line = Color(0xFF35213F)
    val hairDark = hair.darker(0.30f)
    val hairShadow = hair.darker(0.16f)
    val skinShadow = lerp(skin, Color(0xFFC87892), 0.18f)
    val outline = 0.48f
    val e get() = pose.emotion

    fun fillOutlined(path: Path, color: Color, stroke: Color = line, width: Float = outline) {
        d.drawPath(path, color)
        d.drawPath(path, stroke, style = Stroke(width, join = StrokeJoin.Round, cap = StrokeCap.Round))
    }

    // ---------------------------------------------------------------- hair (back)

    fun backHair() {
        val bp = path {
            when (a.hairStyle) {
                0, 7 -> { // long straight / hime
                    moveTo(24f, 40f)
                    cubicTo(20f, 20f, 80f, 20f, 76f, 40f)
                    cubicTo(80f, 60f, 82f, 85f, 84f, 100f)
                    lineTo(16f, 100f)
                    cubicTo(18f, 85f, 20f, 60f, 24f, 40f)
                }
                5 -> { // wavy
                    moveTo(24f, 40f)
                    cubicTo(20f, 20f, 80f, 20f, 76f, 40f)
                    cubicTo(84f, 55f, 76f, 65f, 84f, 78f)
                    cubicTo(90f, 88f, 82f, 96f, 86f, 100f)
                    lineTo(14f, 100f)
                    cubicTo(18f, 96f, 10f, 88f, 16f, 78f)
                    cubicTo(24f, 65f, 16f, 55f, 24f, 40f)
                }
                2 -> { // bob
                    moveTo(24f, 40f)
                    cubicTo(20f, 20f, 80f, 20f, 76f, 40f)
                    cubicTo(78f, 52f, 80f, 64f, 76f, 72f)
                    lineTo(24f, 72f)
                    cubicTo(20f, 64f, 22f, 52f, 24f, 40f)
                }
                12 -> { // wolf cut: long jagged mane
                    moveTo(24f, 40f)
                    cubicTo(20f, 20f, 80f, 20f, 76f, 40f)
                    lineTo(80f, 52f); lineTo(77f, 54f); lineTo(82f, 64f); lineTo(77f, 65f)
                    lineTo(80f, 76f); lineTo(72f, 72f); lineTo(70f, 80f)
                    lineTo(30f, 80f); lineTo(28f, 72f); lineTo(20f, 76f)
                    lineTo(23f, 65f); lineTo(18f, 64f); lineTo(23f, 54f); lineTo(20f, 52f)
                }
                14 -> { // fluffy: scalloped medium
                    moveTo(24f, 40f)
                    cubicTo(20f, 20f, 80f, 20f, 76f, 40f)
                    cubicTo(84f, 44f, 82f, 52f, 79f, 54f)
                    cubicTo(84f, 58f, 81f, 66f, 76f, 66f)
                    cubicTo(76f, 72f, 68f, 72f, 66f, 68f)
                    lineTo(34f, 68f)
                    cubicTo(32f, 72f, 24f, 72f, 24f, 66f)
                    cubicTo(19f, 66f, 16f, 58f, 21f, 54f)
                    cubicTo(18f, 52f, 16f, 44f, 24f, 40f)
                }
                1, 3, 6, 9, 10, 11 -> { // tails / ponytail / buns / drills / braids: medium back
                    moveTo(25f, 40f)
                    cubicTo(22f, 20f, 78f, 20f, 75f, 40f)
                    cubicTo(77f, 50f, 76f, 58f, 72f, 64f)
                    lineTo(28f, 64f)
                    cubicTo(24f, 58f, 23f, 50f, 25f, 40f)
                }
                else -> { // short / spiky
                    moveTo(26f, 44f)
                    cubicTo(22f, 20f, 78f, 20f, 74f, 44f)
                    lineTo(72f, 56f)
                    lineTo(28f, 56f)
                    close()
                }
            }
            close()
        }
        // Twin tails and ponytail live behind the head.
        when (a.hairStyle) {
            1 -> {
                for (side in listOf(-1f, 1f)) {
                    val x = 50f + side * 27f
                    val tail = path {
                        moveTo(x, 30f)
                        cubicTo(x + side * 16f, 38f, x + side * 18f, 62f, x + side * 10f, 92f)
                        cubicTo(x + side * 8f, 96f, x + side * 4f, 98f, x + side * 2f, 96f)
                        cubicTo(x + side * 8f, 70f, x + side * 2f, 50f, x - side * 3f, 38f)
                        close()
                    }
                    fillOutlined(tail, hairShadow)
                    d.drawPath(
                        path {
                            moveTo(x + side * 6f, 40f)
                            cubicTo(x + side * 12f, 55f, x + side * 11f, 70f, x + side * 7f, 86f)
                        },
                        hair.lighter(0.25f), style = Stroke(1.2f, cap = StrokeCap.Round),
                    )
                }
            }
            3 -> {
                val tail = path {
                    moveTo(60f, 18f)
                    cubicTo(84f, 14f, 90f, 40f, 84f, 70f)
                    cubicTo(82f, 78f, 78f, 82f, 76f, 80f)
                    cubicTo(82f, 56f, 78f, 34f, 62f, 26f)
                    close()
                }
                fillOutlined(tail, hairShadow)
            }
        }
        if (a.hairStyle == 10) { // side ponytail with scrunchie
            val tail = path {
                moveTo(70f, 24f)
                cubicTo(92f, 24f, 96f, 52f, 88f, 82f)
                cubicTo(86f, 88f, 82f, 90f, 80f, 86f)
                cubicTo(86f, 62f, 84f, 42f, 72f, 34f)
                close()
            }
            fillOutlined(tail, hairShadow)
            d.drawPath(path { moveTo(80f, 34f); cubicTo(88f, 46f, 88f, 64f, 85f, 78f) }, hair.lighter(0.25f), style = Stroke(1.2f, cap = StrokeCap.Round))
        }
        fillOutlined(bp, hairShadow)
        if (a.hairStyle == 10) {
            d.drawCircle(Color(0xFFFF8FBF), 3.4f, Offset(74f, 27f))
            d.drawCircle(line, 3.4f, Offset(74f, 27f), style = Stroke(outline))
        }
        if (a.hairStyle == 6) { // odango buns
            for (side in listOf(-1f, 1f)) {
                val center = Offset(50f + side * 21f, 17f)
                d.drawCircle(hair, 9f, center)
                d.drawCircle(line, 9f, center, style = Stroke(outline))
                d.drawArc(hair.lighter(0.35f), 200f, 70f, false, Offset(center.x - 6f, center.y - 6f), Size(12f, 12f), style = Stroke(1.2f, cap = StrokeCap.Round))
            }
        }
    }

    // ---------------------------------------------------------------- body

    fun body() {
        // Neck
        val neck = path {
            moveTo(44.5f, 64f); lineTo(55.5f, 64f); lineTo(56.5f, 80f); lineTo(43.5f, 80f); close()
        }
        fillOutlined(neck, skin)
        d.drawPath(path { moveTo(44.5f, 66f); quadraticBezierTo(50f, 71f, 55.5f, 66f); lineTo(55.5f, 64f); lineTo(44.5f, 64f); close() }, skinShadow)

        // Slightly narrower shoulders make the full-body silhouette read more chibi/kawaii.
        val shoulders = if (g == Gender.MALE) 15f else 19f
        val torso = path {
            moveTo(shoulders - 4f, 101f)
            cubicTo(shoulders - 2f, 88f, 22f, 81f, 40f, 78f)
            lineTo(60f, 78f)
            cubicTo(78f, 81f, 100f - shoulders + 2f, 88f, 100f - shoulders + 4f, 101f)
            close()
        }
        val main = when (a.outfit) {
            0 -> Color(0xFFF7F7FC) // sailor top is white, collar coloured
            else -> cloth
        }
        fillOutlined(torso, main)
        val accent = if (cloth.luminance() > 0.6f) Color(0xFF2B3160) else Color(0xFFF7F7FC)

        when (a.outfit) {
            0 -> { // seifuku
                for (side in listOf(-1f, 1f)) {
                    val collar = path {
                        moveTo(50f, 92f)
                        lineTo(50f + side * 7f, 78f)
                        lineTo(50f + side * 26f, 82f)
                        lineTo(50f + side * 20f, 90f)
                        close()
                    }
                    fillOutlined(collar, cloth)
                    d.drawLine(Color.White, Offset(50f + side * 9f, 80f), Offset(50f + side * 22f, 83f), 0.6f)
                }
                val bow = path {
                    moveTo(50f, 87f); lineTo(42f, 83f); lineTo(43f, 92f); close()
                    moveTo(50f, 87f); lineTo(58f, 83f); lineTo(57f, 92f); close()
                }
                fillOutlined(bow, Color(0xFFE5485D))
                d.drawCircle(Color(0xFFC0304A), 1.8f, Offset(50f, 87f))
            }
            1 -> { // blazer
                val shirt = path { moveTo(43f, 78f); lineTo(57f, 78f); lineTo(50f, 101f); close() }
                fillOutlined(shirt, Color(0xFFF7F7FC))
                val tie = path { moveTo(48.5f, 80f); lineTo(51.5f, 80f); lineTo(53f, 96f); lineTo(50f, 100f); lineTo(47f, 96f); close() }
                fillOutlined(tie, Color(0xFFD8334A))
                for (side in listOf(-1f, 1f)) {
                    d.drawLine(line, Offset(50f + side * 7f, 78f), Offset(50f + side * 3f, 101f), outline)
                }
                d.drawCircle(Color(0xFFE0B04A), 1f, Offset(38f, 94f))
            }
            2 -> { // hoodie
                val hood = path {
                    moveTo(32f, 82f); cubicTo(36f, 74f, 64f, 74f, 68f, 82f); cubicTo(60f, 86f, 40f, 86f, 32f, 82f); close()
                }
                fillOutlined(hood, cloth.darker(0.2f))
                d.drawLine(Color.White, Offset(46f, 85f), Offset(45f, 97f), 0.8f, cap = StrokeCap.Round)
                d.drawLine(Color.White, Offset(54f, 85f), Offset(55f, 97f), 0.8f, cap = StrokeCap.Round)
            }
            3 -> { // maid
                val apron = path { moveTo(38f, 86f); cubicTo(40f, 82f, 60f, 82f, 62f, 86f); lineTo(64f, 101f); lineTo(36f, 101f); close() }
                fillOutlined(apron, Color.White)
                for (i in 0..5) d.drawCircle(Color.White, 2f, Offset(38.5f + i * 4.6f, 85f))
                val collar = path { moveTo(42f, 78f); lineTo(58f, 78f); lineTo(54f, 83f); lineTo(46f, 83f); close() }
                fillOutlined(collar, Color.White)
                val bow = path { moveTo(50f, 82f); lineTo(45f, 79f); lineTo(45f, 85f); close(); moveTo(50f, 82f); lineTo(55f, 79f); lineTo(55f, 85f); close() }
                fillOutlined(bow, Color(0xFFE5485D))
            }
            4 -> { // kimono
                val left = path { moveTo(42f, 78f); lineTo(58f, 101f); lineTo(50f, 101f); lineTo(38f, 82f); close() }
                val right = path { moveTo(58f, 78f); lineTo(46f, 96f); lineTo(42f, 92f); lineTo(54f, 78f); close() }
                fillOutlined(right, Color(0xFFF7F7FC))
                fillOutlined(left, cloth.lighter(0.15f))
                d.drawLine(accent, Offset(42f, 78f), Offset(58f, 101f), 1.2f)
                val obi = path { moveTo(20f, 97f); lineTo(80f, 97f); lineTo(81f, 101f); lineTo(19f, 101f); close() }
                fillOutlined(obi, Color(0xFFE0B04A))
                for (i in 0..3) d.drawCircle(cloth.lighter(0.5f), 1.2f, Offset(24f + i * 5f, 90f - i * 1.5f), alpha = 0.7f)
            }
            5 -> { // magical girl
                for (side in listOf(-1f, 1f)) {
                    val puff = path {
                        moveTo(50f + side * 28f, 84f)
                        cubicTo(50f + side * 30f, 76f, 50f + side * 42f, 78f, 50f + side * 42f, 88f)
                        cubicTo(50f + side * 38f, 90f, 50f + side * 32f, 89f, 50f + side * 28f, 84f)
                        close()
                    }
                    fillOutlined(puff, Color.White)
                }
                val bow = path {
                    moveTo(50f, 86f); cubicTo(42f, 78f, 34f, 82f, 38f, 90f); cubicTo(42f, 94f, 46f, 90f, 50f, 86f)
                    moveTo(50f, 86f); cubicTo(58f, 78f, 66f, 82f, 62f, 90f); cubicTo(58f, 94f, 54f, 90f, 50f, 86f)
                }
                fillOutlined(bow, Color(0xFFFF6FB5))
                d.drawCircle(Color(0xFFFFD983), 2.4f, Offset(50f, 86f))
                d.drawCircle(line, 2.4f, Offset(50f, 86f), style = Stroke(outline))
            }
            6 -> { // gakuran
                val collar = path { moveTo(41f, 76f); lineTo(59f, 76f); lineTo(60f, 81f); lineTo(40f, 81f); close() }
                fillOutlined(collar, cloth.darker(0.3f))
                for (i in 0..3) d.drawCircle(Color(0xFFE0B04A), 1f, Offset(50f, 84f + i * 4.5f))
                d.drawLine(line, Offset(50f, 81f), Offset(50f, 101f), outline)
            }
            8 -> { // idol stage costume
                val frill = path {
                    moveTo(30f, 84f)
                    for (i in 0..10) quadraticBezierTo(32f + i * 4f, 88f, 34f + i * 4f, 84f)
                    lineTo(74f, 86f); lineTo(26f, 86f); close()
                }
                fillOutlined(frill, Color.White, width = 0.4f)
                val tie = path { moveTo(50f, 81f); lineTo(44f, 78f); lineTo(44f, 84f); close(); moveTo(50f, 81f); lineTo(56f, 78f); lineTo(56f, 84f); close() }
                fillOutlined(tie, accent)
                for ((sx, sy) in listOf(32f to 94f, 66f to 92f, 58f to 98f)) with(d) { sparkle(Offset(sx, sy), 2.4f, Color(0xFFFFD983)) }
            }
            9 -> { // miko: white haori, red hakama ties
                val haori = path { moveTo(42f, 78f); lineTo(58f, 78f); lineTo(64f, 101f); lineTo(36f, 101f); close() }
                fillOutlined(haori, Color(0xFFF7F7FC))
                d.drawLine(line, Offset(42f, 78f), Offset(54f, 101f), outline)
                d.drawLine(line, Offset(58f, 78f), Offset(48f, 92f), outline)
                val hakama = path { moveTo(30f, 96f); lineTo(70f, 96f); lineTo(71f, 101f); lineTo(29f, 101f); close() }
                fillOutlined(hakama, Color(0xFFD8334A))
                d.drawCircle(Color(0xFFD8334A), 1.8f, Offset(50f, 96f))
            }
            10 -> { // ninja: scarf around the neck
                val scarf = path { moveTo(40f, 72f); cubicTo(44f, 69f, 56f, 69f, 60f, 72f); lineTo(61f, 81f); cubicTo(55f, 84f, 45f, 84f, 39f, 81f); close() }
                fillOutlined(scarf, accent.takeIf { cloth.luminance() < 0.3f } ?: Color(0xFFD8334A))
                val tail = path { moveTo(57f, 80f); lineTo(70f, 92f); lineTo(66f, 95f); lineTo(54f, 82f); close() }
                fillOutlined(tail, accent.takeIf { cloth.luminance() < 0.3f } ?: Color(0xFFD8334A))
                d.drawLine(line, Offset(40f, 92f), Offset(60f, 92f), 1.4f)
            }
            11 -> { // butler tailcoat: vest + bow tie
                val shirt = path { moveTo(43f, 78f); lineTo(57f, 78f); lineTo(54f, 101f); lineTo(46f, 101f); close() }
                fillOutlined(shirt, Color.White)
                val vest = path { moveTo(45f, 86f); lineTo(55f, 86f); lineTo(56f, 101f); lineTo(44f, 101f); close() }
                fillOutlined(vest, Color(0xFF6B4A8F))
                for (side in listOf(-1f, 1f)) d.drawLine(line, Offset(50f + side * 7f, 78f), Offset(50f + side * 9f, 101f), outline * 1.4f)
                val bow = path { moveTo(50f, 80.5f); lineTo(46f, 78.5f); lineTo(46f, 82.5f); close(); moveTo(50f, 80.5f); lineTo(54f, 78.5f); lineTo(54f, 82.5f); close() }
                fillOutlined(bow, Color(0xFF1E1E26))
                d.drawCircle(Color(0xFFE0B04A), 0.8f, Offset(50f, 92f))
                d.drawCircle(Color(0xFFE0B04A), 0.8f, Offset(50f, 97f))
            }
            12 -> { // turtleneck sweater
                val collar = path { moveTo(43f, 70f); lineTo(57f, 70f); lineTo(58f, 80f); cubicTo(53f, 82f, 47f, 82f, 42f, 80f); close() }
                fillOutlined(collar, cloth.lighter(0.1f))
                for (i in 0..3) d.drawLine(cloth.darker(0.25f), Offset(44.5f + i * 3.5f, 71f), Offset(44.5f + i * 3.5f, 80f), 0.5f)
                for (x in listOf(34f, 42f, 50f, 58f, 66f)) d.drawLine(cloth.darker(0.18f), Offset(x, 85f), Offset(x, 101f), 0.6f)
            }
            13 -> { // gothic lolita: lace collar + cross
                val collar = path {
                    moveTo(40f, 78f); lineTo(60f, 78f); lineTo(58f, 84f)
                    for (i in 0..4) quadraticBezierTo(56f - i * 4f, 87f, 54f - i * 4f, 84f)
                    close()
                }
                fillOutlined(collar, Color.White, width = 0.4f)
                d.drawLine(Color(0xFFE0B04A), Offset(50f, 87f), Offset(50f, 96f), 1.2f)
                d.drawLine(Color(0xFFE0B04A), Offset(46.5f, 90f), Offset(53.5f, 90f), 1.2f)
                for (side in listOf(-1f, 1f)) for (k in 0..3) d.drawCircle(Color(0xFFD8334A), 0.7f, Offset(50f + side * 4f, 88f + k * 3.5f))
            }
            14 -> { // mage robe with hood and clasp
                val hood = path { moveTo(26f, 86f); cubicTo(30f, 72f, 70f, 72f, 74f, 86f); cubicTo(64f, 80f, 36f, 80f, 26f, 86f); close() }
                fillOutlined(hood, cloth.darker(0.25f))
                val trim = path { moveTo(44f, 82f); lineTo(56f, 82f); lineTo(53f, 101f); lineTo(47f, 101f); close() }
                fillOutlined(trim, cloth.darker(0.4f))
                for (side in listOf(-1f, 1f)) d.drawLine(Color(0xFFE0B04A), Offset(50f + side * 6f, 82f), Offset(50f + side * 3f, 101f), 0.8f)
                d.drawCircle(Color(0xFF9CCBFF), 2.4f, Offset(50f, 84f))
                d.drawCircle(Color(0xFFE0B04A), 2.4f, Offset(50f, 84f), style = Stroke(0.8f))
            }
            15 -> { // cyber jacket with neon trim
                val neon = Color(0xFF3FF2E6)
                val collar = path { moveTo(38f, 76f); lineTo(46f, 78f); lineTo(44f, 86f); lineTo(36f, 82f); close(); moveTo(62f, 76f); lineTo(54f, 78f); lineTo(56f, 86f); lineTo(64f, 82f); close() }
                fillOutlined(collar, cloth.darker(0.3f))
                d.drawLine(line, Offset(50f, 80f), Offset(50f, 101f), 0.9f)
                d.drawLine(neon, Offset(24f, 92f), Offset(38f, 88f), 0.9f, cap = StrokeCap.Round)
                d.drawLine(neon, Offset(76f, 92f), Offset(62f, 88f), 0.9f, cap = StrokeCap.Round)
                d.drawLine(Color(0xFFFF4FD8), Offset(53f, 86f), Offset(53f, 96f), 0.7f, cap = StrokeCap.Round)
                d.drawRoundRect(neon.copy(alpha = 0.8f), Offset(40f, 94f), Size(6f, 2.4f), androidx.compose.ui.geometry.CornerRadius(1f))
            }
            16 -> { // sports jersey
                for (side in listOf(-1f, 1f)) {
                    d.drawLine(accent, Offset(50f + side * 18f, 82f), Offset(50f + side * 34f, 94f), 2f, cap = StrokeCap.Round)
                    d.drawLine(accent, Offset(50f + side * 16f, 85f), Offset(50f + side * 31f, 97f), 1f, cap = StrokeCap.Round)
                }
                val zip = path { moveTo(44f, 77f); lineTo(56f, 77f); lineTo(50f, 85f); close() }
                fillOutlined(zip, accent)
                d.drawLine(line, Offset(50f, 85f), Offset(50f, 101f), 0.7f)
            }
            17 -> { // qipao: high collar, diagonal placket, frog buttons
                val collar = path { moveTo(43f, 71f); lineTo(57f, 71f); lineTo(57.5f, 79f); lineTo(42.5f, 79f); close() }
                fillOutlined(collar, cloth)
                d.drawLine(Color(0xFFE0B04A), Offset(43f, 78f), Offset(57f, 78f), 0.6f)
                val placket = path { moveTo(50f, 79f); quadraticBezierTo(58f, 82f, 64f, 92f) }
                d.drawPath(placket, Color(0xFFE0B04A), style = Stroke(0.9f, cap = StrokeCap.Round))
                for ((bx, by) in listOf(55f to 81.5f, 60f to 86f, 63f to 90.5f)) {
                    d.drawLine(Color(0xFFE0B04A), Offset(bx - 2f, by), Offset(bx + 2f, by), 1f, cap = StrokeCap.Round)
                    d.drawCircle(Color(0xFFE0B04A), 0.8f, Offset(bx + 2.2f, by))
                }
                for (i in 0..2) with(d) { sparkle(Offset(32f + i * 7f, 94f - i * 2f), 1.6f, Color(0xFFFFD983).copy(alpha = 0.8f)) }
            }
            7 -> { // armor
                for (side in listOf(-1f, 1f)) {
                    val pad = path {
                        moveTo(50f + side * 16f, 80f)
                        cubicTo(50f + side * 30f, 74f, 50f + side * 44f, 80f, 50f + side * 44f, 94f)
                        lineTo(50f + side * 26f, 92f)
                        close()
                    }
                    fillOutlined(pad, Color(0xFFC9CEDD))
                    d.drawLine(Color.White, Offset(50f + side * 22f, 80f), Offset(50f + side * 38f, 82f), 0.8f, alpha = 0.8f)
                }
                val plate = path { moveTo(38f, 82f); lineTo(62f, 82f); lineTo(60f, 101f); lineTo(40f, 101f); close() }
                fillOutlined(plate, Color(0xFFB3B9CC))
                d.drawCircle(cloth, 2.5f, Offset(50f, 90f))
            }
        }
    }

    // ---------------------------------------------------------------- face

    fun facePath(): Path = path {
        moveTo(28f, 42f)
        when (a.faceShape) {
            1 -> {
                cubicTo(27f, 57f, 37f, 67.5f, 50f, 68.5f)
                cubicTo(63f, 67.5f, 73f, 57f, 72f, 42f)
            }
            2 -> {
                cubicTo(28f, 54.5f, 41f, 65f, 50f, 70f)
                cubicTo(59f, 65f, 72f, 54.5f, 72f, 42f)
            }
            else -> {
                cubicTo(28f, 56f, 39f, 66f, 50f, 69f)
                cubicTo(61f, 66f, 72f, 56f, 72f, 42f)
            }
        }
        cubicTo(72f, 18f, 28f, 18f, 28f, 42f)
        close()
    }

    fun hairBesideFace() {
        when (a.hairStyle) {
            9 -> { // ojou drills
                for (side in listOf(-1f, 1f)) {
                    val x = 50f + side * 26f
                    for (k in 0 until 5) {
                        val w = 11f - k * 1.4f
                        val cy = 46f + k * 7.5f
                        val seg = path { addOval(androidx.compose.ui.geometry.Rect(x - w / 2f, cy - 5f, x + w / 2f, cy + 5f)) }
                        fillOutlined(seg, hair)
                        d.drawLine(hairDark.copy(alpha = 0.6f), Offset(x - w * 0.4f, cy - 2f), Offset(x + w * 0.35f, cy + 3f), 0.5f, cap = StrokeCap.Round)
                    }
                    d.drawCircle(Color(0xFFE5485D), 2.2f, Offset(x, 41f))
                }
            }
            11 -> { // two braids over the shoulders
                for (side in listOf(-1f, 1f)) {
                    val x = 50f + side * 23f
                    for (k in 0 until 6) {
                        val cx = x + (if (k % 2 == 0) -1f else 1f) * 1.2f * side + side * k * 0.6f
                        val cy = 58f + k * 5.5f
                        val seg = path { addOval(androidx.compose.ui.geometry.Rect(cx - 3.6f, cy - 3.6f, cx + 3.6f, cy + 3.6f)) }
                        fillOutlined(seg, hair)
                    }
                    val tipX = x + side * 3.6f
                    d.drawCircle(Color(0xFF9CCBFF), 2f, Offset(tipX, 91f))
                    fillOutlined(path { moveTo(tipX - 2f, 92f); lineTo(tipX - 3.5f, 99f); lineTo(tipX + 3.5f, 99f); lineTo(tipX + 2f, 92f); close() }, hair)
                }
            }
            13 -> { // low tail draped over the shoulder
                val tail = path {
                    moveTo(58f, 60f)
                    cubicTo(70f, 64f, 74f, 78f, 72f, 98f)
                    lineTo(66f, 99f)
                    cubicTo(66f, 82f, 62f, 72f, 54f, 66f)
                    close()
                }
                fillOutlined(tail, hair)
                d.drawRoundRect(Color(0xFFE5485D), Offset(59f, 63f), Size(6f, 3f), androidx.compose.ui.geometry.CornerRadius(1f))
                d.drawPath(path { moveTo(64f, 70f); cubicTo(69f, 78f, 70f, 88f, 69f, 96f) }, hair.lighter(0.3f), style = Stroke(0.8f, cap = StrokeCap.Round))
            }
        }
    }

    fun face() {
        fillOutlined(facePath(), skin)
    }

    fun earsBehind() {
        if (a.ears == 4) { // elf ears
            for (side in listOf(-1f, 1f)) {
                val ear = path {
                    moveTo(50f + side * 21f, 44f)
                    lineTo(50f + side * 36f, 34f)
                    cubicTo(50f + side * 32f, 46f, 50f + side * 26f, 52f, 50f + side * 21f, 54f)
                    close()
                }
                fillOutlined(ear, skin)
                d.drawLine(skinShadow, Offset(50f + side * 23f, 47f), Offset(50f + side * 31f, 40f), 0.8f)
            }
        } else {
            for (side in listOf(-1f, 1f)) {
                val ear = path {
                    moveTo(50f + side * 21.5f, 44f)
                    cubicTo(50f + side * 26f, 42f, 50f + side * 26f, 53f, 50f + side * 21f, 54f)
                    close()
                }
                fillOutlined(ear, skin)
            }
        }
    }

    // ---------------------------------------------------------------- features

    fun features() {
        // Blush
        val blushStrength = when (e) {
            Emotion.SHY, Emotion.LOVE -> 0.75f
            Emotion.ANGRY -> 0.4f
            else -> if (a.blush) 0.4f else 0f
        }
        if (blushStrength > 0f) {
            for (side in listOf(-1f, 1f)) {
                val c = Offset(50f + side * 13.6f, 59.4f)
                d.drawOval(
                    Brush.radialGradient(listOf(Color(0xFFFF7FA6).copy(alpha = blushStrength), Color.Transparent), c, 5.5f),
                    topLeft = Offset(c.x - 6f, c.y - 3f), size = Size(12f, 6f),
                )
                if (e == Emotion.SHY || a.blush) {
                    for (i in 0..2) {
                        val x = c.x - 2.5f + i * 2f
                        d.drawLine(Color(0xFFE85C8A).copy(alpha = blushStrength), Offset(x + 0.8f, c.y - 1.2f), Offset(x - 0.4f, c.y + 1.2f), 0.45f, cap = StrokeCap.Round)
                    }
                }
            }
        }
        if (e == Emotion.LOVE || e == Emotion.SHY) {
            for (side in listOf(-1f, 1f)) {
                val x = 50f + side * 18f
                d.drawCircle(Color.White.copy(alpha = 0.82f), 0.75f, Offset(x, 57.3f))
                d.drawCircle(Color(0xFFFFB7D3).copy(alpha = 0.72f), 0.48f, Offset(x + side * 2.1f, 59.1f))
            }
        }
        if (a.beautyMark) d.drawCircle(line, 0.55f, Offset(60.5f, 58.5f))
        if (a.bandaid) {
            d.rotate(-20f, Offset(37f, 61f)) {
                d.drawRoundRect(Color(0xFFF4D3B5), Offset(33f, 59.5f), Size(8f, 3f), androidx.compose.ui.geometry.CornerRadius(1.5f))
                d.drawRect(Color(0xFFE8BC98), Offset(36f, 59.5f), Size(2f, 3f))
            }
        }

        eyes()

        // Nose
        d.drawLine(skinShadow.darker(0.2f), Offset(50.6f, 58.6f), Offset(50.1f, 59.6f), 0.5f, cap = StrokeCap.Round)

        mouth()
    }

    private enum class Pupil { ROUND, SLIT, STAR, HEART, NONE, RING }

    private data class EyeShape(
        val w: Float,
        val h: Float,
        val tilt: Float,
        val lidDrop: Float,
        val pupil: Pupil,
        val sparkle: Boolean,
        val shine: Boolean = true,
        val lashes: Boolean = false,
    )

    private fun eyeShape(): EyeShape = when (a.eyeStyle) {
        1 -> EyeShape(12.2f, 10.2f, 2.2f, 0f, Pupil.ROUND, false)
        2 -> EyeShape(11.8f, 12f, -1.8f, 0.05f, Pupil.ROUND, false)
        3 -> EyeShape(10.8f, 13.2f, 0f, 0f, Pupil.ROUND, true)
        4 -> EyeShape(12.2f, 12f, 1.6f, 0f, Pupil.SLIT, false)
        5 -> EyeShape(11.8f, 11.2f, 0.3f, 0.33f, Pupil.ROUND, false)
        6 -> EyeShape(11.8f, 13.6f, 0.4f, 0f, Pupil.STAR, true)
        7 -> EyeShape(11.8f, 13.6f, 0.2f, 0f, Pupil.HEART, true)
        8 -> EyeShape(11.8f, 12f, 0.2f, 0.12f, Pupil.NONE, false, shine = false)
        9 -> EyeShape(13.2f, 7.5f, 2.6f, 0f, Pupil.ROUND, false)
        10 -> EyeShape(13.2f, 16f, 0f, 0f, Pupil.ROUND, true)
        11 -> EyeShape(12.2f, 13f, 1f, 0f, Pupil.ROUND, true, lashes = true)
        12 -> EyeShape(11.8f, 13f, 0.5f, 0f, Pupil.RING, true)
        else -> EyeShape(12f, 14.2f, 0.4f, 0f, Pupil.ROUND, true)
    }

    private fun eyes() {
        val s = eyeShape()
        val closedHappy = e == Emotion.LAUGH || (e == Emotion.HAPPY && a.eyeStyle == 5)
        val blinkClosed = pose.blink > 0.5f
        for (side in listOf(-1f, 1f)) {
            val cx = 50f + side * 10.2f
            val cy = 51.6f
            val iris = if (side < 0) eye else eye2
            if (closedHappy) {
                // ^ ^
                val p = path {
                    moveTo(cx - 5f, cy + 1.5f)
                    quadraticBezierTo(cx, cy - 4.5f, cx + 5f, cy + 1.5f)
                }
                d.drawPath(p, line, style = Stroke(1.4f, cap = StrokeCap.Round))
                continue
            }
            if (blinkClosed) {
                val p = path {
                    moveTo(cx - 5f, cy)
                    quadraticBezierTo(cx, cy + 3f, cx + 5f, cy)
                }
                d.drawPath(p, line, style = Stroke(1.3f, cap = StrokeCap.Round))
                // little outer lash
                d.drawLine(line, Offset(cx + side * 5f, cy), Offset(cx + side * 6.5f, cy - 1f), 0.9f, cap = StrokeCap.Round)
                continue
            }

            val wide = if (e == Emotion.SURPRISED) 1.12f else 1f
            val genderScale = when (g) {
                Gender.MALE -> 0.8f
                Gender.NEUTRAL -> 0.92f
                Gender.FEMALE -> 1f
            }
            val h = s.h * wide * genderScale
            val w = s.w
            val xo = cx + side * w / 2f // outer corner
            val xi = cx - side * w / 2f // inner corner
            val outerY = cy - h * 0.2f - s.tilt
            val innerY = cy - h * 0.08f
            val sclera = path {
                moveTo(xi, innerY)
                cubicTo(xi + (xo - xi) * 0.1f, cy - h * 0.62f, xo - (xo - xi) * 0.18f, cy - h * 0.6f - s.tilt, xo, outerY)
                cubicTo(xo - (xo - xi) * 0.02f, cy + h * 0.42f, xi + (xo - xi) * 0.12f, cy + h * 0.5f, xi, innerY)
                close()
            }
            d.drawPath(sclera, Color.White)
            d.clipPath(sclera) {
                val irisCenter = Offset(cx + side * 0.2f, cy + h * 0.06f)
                val rx = w * 0.37f
                val ry = h * 0.47f
                val irisColors = if (s.shine) listOf(iris.darker(0.55f), iris, iris.lighter(0.45f)) else listOf(iris.darker(0.7f), iris.darker(0.35f), iris.darker(0.15f))
                drawOval(
                    Brush.verticalGradient(irisColors, irisCenter.y - ry, irisCenter.y + ry),
                    Offset(irisCenter.x - rx, irisCenter.y - ry), Size(rx * 2, ry * 2),
                )
                drawOval(iris.darker(0.6f), Offset(irisCenter.x - rx, irisCenter.y - ry), Size(rx * 2, ry * 2), style = Stroke(0.5f))
                // Pupil
                val pupilScale = if (e == Emotion.SURPRISED) 0.55f else 1f
                val pupilKind = if (e == Emotion.LOVE) Pupil.HEART else s.pupil
                val pr = rx * 0.45f * pupilScale
                when (pupilKind) {
                    Pupil.HEART -> heart(
                        irisCenter.copy(y = irisCenter.y + 0.5f), 3.2f * pupilScale.coerceAtLeast(0.8f),
                        if (e == Emotion.LOVE) Color(0xFFFF4F8B) else lerp(iris, Color(0xFFFF4F8B), 0.65f),
                    )
                    Pupil.SLIT -> drawOval(iris.darker(0.75f), Offset(irisCenter.x - 0.9f, irisCenter.y - ry * 0.8f), Size(1.8f, ry * 1.6f))
                    Pupil.STAR -> {
                        drawOval(iris.darker(0.6f), Offset(irisCenter.x - pr, irisCenter.y - pr * 1.3f), Size(pr * 2, pr * 2.6f))
                        star(irisCenter.copy(y = irisCenter.y + 0.3f), rx * 0.62f, Color(0xFFFFF3B0))
                    }
                    Pupil.NONE -> drawOval(iris.darker(0.6f), Offset(irisCenter.x - pr, irisCenter.y - pr * 1.3f), Size(pr * 2, pr * 2.6f))
                    Pupil.RING -> {
                        drawOval(iris.lighter(0.6f), Offset(irisCenter.x - rx * 0.75f, irisCenter.y - ry * 0.75f), Size(rx * 1.5f, ry * 1.5f), style = Stroke(0.45f))
                        drawOval(iris.lighter(0.6f), Offset(irisCenter.x - rx * 0.48f, irisCenter.y - ry * 0.48f), Size(rx * 0.96f, ry * 0.96f), style = Stroke(0.45f))
                        drawCircle(iris.darker(0.8f), pr * 0.6f, irisCenter)
                    }
                    Pupil.ROUND -> drawOval(iris.darker(0.75f), Offset(irisCenter.x - pr, irisCenter.y - pr * 1.3f), Size(pr * 2, pr * 2.6f))
                }
                // Upper-lid shadow inside the eye
                drawRect(Brush.verticalGradient(listOf(line.copy(alpha = 0.35f), Color.Transparent), cy - h * 0.6f, cy - h * 0.15f), Offset(cx - w, cy - h), Size(w * 2, h * 0.85f))
                // Highlights
                if (s.shine) {
                    // Triple catchlight gives the eyes a softer shoujo-manga sparkle.
                    drawCircle(Color.White, rx * 0.44f, Offset(irisCenter.x - side * rx * 0.25f - rx * 0.15f, irisCenter.y - ry * 0.46f))
                    drawCircle(Color.White.copy(alpha = 0.88f), rx * 0.19f, Offset(irisCenter.x + rx * 0.36f, irisCenter.y + ry * 0.42f))
                    drawCircle(Color.White.copy(alpha = 0.72f), rx * 0.10f, Offset(irisCenter.x - side * rx * 0.48f, irisCenter.y + ry * 0.18f))
                    drawOval(
                        iris.lighter(0.62f).copy(alpha = 0.32f),
                        Offset(irisCenter.x - rx * 0.72f, irisCenter.y + ry * 0.30f),
                        Size(rx * 1.44f, ry * 0.34f),
                    )
                }
                if (s.sparkle) sparkle(Offset(irisCenter.x + rx * 0.3f, irisCenter.y - ry * 0.1f), 1.3f, Color.White)
                if (e == Emotion.SAD) {
                    drawOval(Color.White.copy(alpha = 0.35f), Offset(cx - w / 2f, cy + h * 0.15f), Size(w, h * 0.35f))
                }
                // Half-closed lids
                val drop = s.lidDrop + when (e) {
                    Emotion.SMUG -> 0.3f
                    Emotion.ANGRY -> 0.22f
                    Emotion.THINKING -> 0.1f
                    else -> 0f
                } + pose.blink * 0.8f
                if (drop > 0.01f) {
                    val lidY = cy - h * 0.6f + h * drop
                    drawRect(skin, Offset(cx - w, cy - h * 1.2f), Size(w * 2, lidY - (cy - h * 1.2f)))
                    drawLine(line, Offset(cx - w, lidY + (if (e == Emotion.ANGRY) -side * 1.5f else 0f)), Offset(cx + w, lidY + (if (e == Emotion.ANGRY) side * 1.5f else 0f)), 1.6f)
                }
            }
            // Upper lash line with an outer flick
            val lash = path {
                moveTo(xi - side * 0.3f, innerY + 0.4f)
                cubicTo(xi + (xo - xi) * 0.1f, cy - h * 0.64f, xo - (xo - xi) * 0.18f, cy - h * 0.62f - s.tilt, xo, outerY)
                lineTo(xo + side * 2.2f, outerY - 1.4f)
            }
            d.drawPath(lash, line, style = Stroke(1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            if (g != Gender.MALE) {
                d.drawLine(line, Offset(xo - side * 0.5f, outerY - 0.2f), Offset(xo + side * 1.7f, outerY + 1.5f), 0.85f, cap = StrokeCap.Round)
                d.drawLine(line, Offset(xo - side * 1.7f, outerY - 0.8f), Offset(xo + side * 0.8f, outerY - 2.7f), 0.62f, cap = StrokeCap.Round)
            }
            if (s.lashes) {
                for (k in 0..2) {
                    val bx = xo - side * (k * 2.2f + 0.8f)
                    val by = outerY - 1.2f - k * 0.9f
                    d.drawLine(line, Offset(bx, by), Offset(bx + side * 1.6f, by - 2.2f + k * 0.3f), 0.7f, cap = StrokeCap.Round)
                }
            }
            // Lower lash hint
            d.drawLine(line.copy(alpha = 0.7f), Offset(cx + side * w * 0.05f, cy + h * 0.47f), Offset(xo - side * w * 0.05f, cy + h * 0.3f), 0.5f, cap = StrokeCap.Round)

            if (e == Emotion.SAD) {
                val tear = path {
                    moveTo(cx + side * 3f, cy + h * 0.5f)
                    cubicTo(cx + side * 1.5f, cy + h * 0.9f, cx + side * 4.5f, cy + h * 0.9f, cx + side * 3f, cy + h * 0.5f)
                }
                d.drawPath(tear, Color(0xFF8FD3FF))
            }
        }
    }

    private fun DrawScope.star(c: Offset, r: Float, color: Color) {
        val p = path {
            for (i in 0 until 10) {
                val ang = Math.toRadians(i * 36.0 - 90).toFloat()
                val rad = if (i % 2 == 0) r else r * 0.45f
                val x = c.x + kotlin.math.cos(ang) * rad
                val y = c.y + kotlin.math.sin(ang) * rad
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(p, color)
    }

    private fun DrawScope.heart(c: Offset, r: Float, color: Color) {
        val p = path {
            moveTo(c.x, c.y + r * 0.9f)
            cubicTo(c.x - r * 1.4f, c.y - r * 0.1f, c.x - r * 0.8f, c.y - r * 1.2f, c.x, c.y - r * 0.4f)
            cubicTo(c.x + r * 0.8f, c.y - r * 1.2f, c.x + r * 1.4f, c.y - r * 0.1f, c.x, c.y + r * 0.9f)
            close()
        }
        drawPath(p, color)
        drawCircle(Color.White, r * 0.22f, Offset(c.x - r * 0.45f, c.y - r * 0.45f))
    }

    private fun DrawScope.sparkle(c: Offset, r: Float, color: Color) {
        val p = path {
            moveTo(c.x, c.y - r)
            quadraticBezierTo(c.x, c.y, c.x + r, c.y)
            quadraticBezierTo(c.x, c.y, c.x, c.y + r)
            quadraticBezierTo(c.x, c.y, c.x - r, c.y)
            quadraticBezierTo(c.x, c.y, c.x, c.y - r)
            close()
        }
        drawPath(p, color)
    }

    private fun mouth() {
        val mx = 50f
        val my = 63.8f
        val talking = pose.mouthOpen
        val style = when {
            talking -> -1
            e == Emotion.LAUGH || e == Emotion.LOVE -> -2
            e == Emotion.SURPRISED -> -3
            e == Emotion.SAD -> -4
            e == Emotion.ANGRY -> -5
            e == Emotion.SHY -> -6
            e == Emotion.SMUG -> 4
            e == Emotion.HAPPY && a.mouth == 3 -> 0
            else -> a.mouth
        }
        val mouthFill = Color(0xFFB8455F)
        when (style) {
            -1 -> { // talking
                val p = path { moveTo(mx - 2.2f, my - 0.6f); quadraticBezierTo(mx, my - 1.2f, mx + 2.2f, my - 0.6f); quadraticBezierTo(mx, my + 3f, mx - 2.2f, my - 0.6f); close() }
                fillOutlined(p, mouthFill, width = 0.5f)
            }
            -2 -> { // open laugh "D"
                val p = path { moveTo(mx - 3.8f, my - 1.2f); lineTo(mx + 3.8f, my - 1.2f); quadraticBezierTo(mx + 3.5f, my + 4.2f, mx, my + 4.2f); quadraticBezierTo(mx - 3.5f, my + 4.2f, mx - 3.8f, my - 1.2f); close() }
                fillOutlined(p, mouthFill, width = 0.5f)
                d.clipPath(p) { drawOval(Color(0xFFFF8FA3), Offset(mx - 2.5f, my + 1.6f), Size(5f, 4f)) }
                if (a.fang) fang(mx + 2f, my - 1.2f)
            }
            -3 -> {
                d.drawOval(mouthFill, Offset(mx - 1.6f, my - 1.2f), Size(3.2f, 3.8f))
                d.drawOval(line, Offset(mx - 1.6f, my - 1.2f), Size(3.2f, 3.8f), style = Stroke(0.5f))
            }
            -4 -> d.drawPath(path { moveTo(mx - 2.6f, my + 1f); quadraticBezierTo(mx, my - 1.4f, mx + 2.6f, my + 1f) }, line, style = Stroke(0.7f, cap = StrokeCap.Round))
            -5 -> {
                val p = path { moveTo(mx - 2.8f, my + 1.2f); lineTo(mx, my - 0.6f); lineTo(mx + 2.8f, my + 1.2f); close() }
                fillOutlined(p, mouthFill, width = 0.5f)
                if (a.fang) fang(mx + 1.5f, my + 0.1f)
            }
            -6 -> d.drawPath(path { moveTo(mx - 3f, my); quadraticBezierTo(mx - 1.5f, my - 1f, mx, my); quadraticBezierTo(mx + 1.5f, my + 1f, mx + 3f, my) }, line, style = Stroke(0.6f, cap = StrokeCap.Round))
            1 -> d.drawPath(path { moveTo(mx - 2.8f, my - 0.6f); quadraticBezierTo(mx - 1.4f, my + 1.4f, mx, my - 0.2f); quadraticBezierTo(mx + 1.4f, my + 1.4f, mx + 2.8f, my - 0.6f) }, line, style = Stroke(0.65f, cap = StrokeCap.Round))
            2 -> {
                val p = path { moveTo(mx - 2.8f, my - 0.8f); lineTo(mx + 2.8f, my - 0.8f); quadraticBezierTo(mx, my + 3f, mx - 2.8f, my - 0.8f); close() }
                fillOutlined(p, mouthFill, width = 0.5f)
                fang(mx + 1.6f, my - 0.8f)
            }
            3 -> {
                d.drawOval(mouthFill, Offset(mx - 1.2f, my - 0.7f), Size(2.4f, 2.4f))
                d.drawOval(line, Offset(mx - 1.2f, my - 0.7f), Size(2.4f, 2.4f), style = Stroke(0.45f))
            }
            4 -> d.drawPath(path { moveTo(mx - 2.5f, my + 0.2f); quadraticBezierTo(mx + 0.5f, my + 1.2f, mx + 3f, my - 1.2f) }, line, style = Stroke(0.65f, cap = StrokeCap.Round))
            else -> {
                d.drawPath(path { moveTo(mx - 2.6f, my - 0.4f); quadraticBezierTo(mx, my + 1.8f, mx + 2.6f, my - 0.4f) }, line, style = Stroke(0.65f, cap = StrokeCap.Round))
                if (a.fang) fang(mx + 1.3f, my + 0.4f)
            }
        }
    }

    private fun fang(x: Float, y: Float) {
        val p = path { moveTo(x - 0.7f, y); lineTo(x + 0.7f, y); lineTo(x, y + 1.4f); close() }
        d.drawPath(p, Color.White)
        d.drawPath(p, line, style = Stroke(0.3f))
    }

    // ---------------------------------------------------------------- hair (front)

    fun frontHair() {
        // Side locks framing the face.
        val lockEnd = when (a.hairStyle) {
            4, 8 -> 56f
            13 -> 60f
            14 -> 64f
            2 -> 68f
            12 -> 70f
            7 -> 66f
            else -> 74f
        }
        for (side in listOf(-1f, 1f)) {
            val x = 50f + side * 22f
            val lock = path {
                moveTo(x + side * 1.5f, 32f)
                if (a.hairStyle == 7) {
                    cubicTo(x + side * 2f, 46f, x + side * 1f, 58f, x + side * 0.5f, lockEnd)
                    lineTo(x - side * 5.5f, lockEnd)
                    cubicTo(x - side * 5f, 56f, x - side * 4f, 44f, x - side * 6f, 34f)
                } else {
                    cubicTo(x + side * 3f, 46f, x + side * 1f, 60f, x - side * 1f, lockEnd)
                    cubicTo(x - side * 3f, 64f, x - side * 4f, 50f, x - side * 7f, 36f)
                }
                close()
            }
            fillOutlined(lock, hair)
        }

        // Cap of the head + bangs
        val top = path {
            moveTo(25f, 47f)
            cubicTo(21f, 22f, 36f, 11.5f, 50f, 11.5f)
            cubicTo(64f, 11.5f, 79f, 22f, 75f, 47f)
            bangs(this)
            close()
        }
        fillOutlined(top, hair)

        // Strand lines
        for (sx in listOf(38f, 46f, 55f, 63f)) {
            d.drawPath(
                path { moveTo(50f + (sx - 50f) * 0.3f, 16f); quadraticBezierTo(sx - 1f, 26f, sx, 38f) },
                hairDark.copy(alpha = 0.45f), style = Stroke(0.45f, cap = StrokeCap.Round),
            )
        }

        // Angel-ring highlight (tenshi no wa)
        d.clipPath(top) {
            val ring = path {
                moveTo(30f, 27f)
                quadraticBezierTo(50f, 18f, 70f, 27f)
                quadraticBezierTo(50f, 22f, 30f, 27f)
                close()
            }
            drawPath(ring, Color.White.copy(alpha = 0.58f))
            drawPath(
                path {
                    moveTo(36f, 25f)
                    quadraticBezierTo(50f, 20.5f, 64f, 25f)
                },
                hair.lighter(0.78f).copy(alpha = 0.42f),
                style = Stroke(1.1f, cap = StrokeCap.Round),
            )
            for (i in 0..4) {
                val x = 34f + i * 8f
                val y = 22f + kotlin.math.abs(i - 2) * 1.2f
                drawPath(path { moveTo(x, y); lineTo(x + 1.6f, y + 3.5f); lineTo(x + 3.2f, y); close() }, hair.lighter(0.55f).copy(alpha = 0.6f))
            }
        }

        if (a.ahoge) {
            d.drawPath(
                path { moveTo(50f, 12.5f); cubicTo(48f, 4f, 58f, 2f, 56f, 8f); cubicTo(55f, 10f, 52f, 9f, 53f, 7f) },
                hair, style = Stroke(2.2f, cap = StrokeCap.Round),
            )
            d.drawPath(
                path { moveTo(50f, 12.5f); cubicTo(48f, 4f, 58f, 2f, 56f, 8f); cubicTo(55f, 10f, 52f, 9f, 53f, 7f) },
                hairDark, style = Stroke(0.4f, cap = StrokeCap.Round),
            )
        }
    }

    /** Appends the bang edge from the right temple (75,47) to the left temple (25,47). */
    private fun bangs(p: Path) = with(p) {
        when (a.bangs) {
            0 -> { // patsun: blunt
                lineTo(73f, 41f)
                cubicTo(66f, 43.5f, 34f, 43.5f, 27f, 41f)
                lineTo(25f, 47f)
            }
            2 -> { // side-swept
                lineTo(72f, 38f)
                quadraticBezierTo(66f, 44f, 64f, 47f)
                quadraticBezierTo(60f, 40f, 54f, 37f)
                quadraticBezierTo(48f, 40f, 44f, 44f)
                quadraticBezierTo(40f, 37f, 34f, 34f)
                quadraticBezierTo(30f, 38f, 27f, 43f)
                lineTo(25f, 47f)
            }
            3 -> { // center part
                quadraticBezierTo(70f, 40f, 64f, 44f)
                quadraticBezierTo(60f, 34f, 52f, 27f)
                lineTo(50f, 30f)
                lineTo(48f, 27f)
                quadraticBezierTo(40f, 34f, 36f, 44f)
                quadraticBezierTo(30f, 40f, 25f, 47f)
            }
            4 -> { // spiky
                lineTo(73f, 38f)
                lineTo(68f, 48f); lineTo(64f, 36f)
                lineTo(58f, 47f); lineTo(54f, 35f)
                lineTo(48f, 46f); lineTo(44f, 34f)
                lineTo(38f, 47f); lineTo(34f, 36f)
                lineTo(29f, 46f); lineTo(27f, 38f)
                lineTo(25f, 47f)
            }
            else -> { // classic pointed strands
                val pts = listOf(
                    73f to 38f, 69f to 47f, 65f to 37f, 59f to 46f, 55f to 36f, 50f to 44f,
                    45f to 35f, 40f to 46f, 35f to 37f, 31f to 47f, 27f to 39f,
                )
                var prev = 75f to 47f
                for ((x, y) in pts) {
                    quadraticBezierTo(x, prev.second, x, y)
                    prev = x to y
                }
                quadraticBezierTo(25f, prev.second, 25f, 47f)
            }
        }
    }

    fun brows() {
        // Drawn on top of the bangs, slightly transparent, as anime convention does.
        val rise = when (e) {
            Emotion.SURPRISED -> -2f
            else -> 0f
        }
        for (side in listOf(-1f, 1f)) {
            val cx = 50f + side * 10.5f
            val y = 42.5f + rise
            val (innerDy, outerDy) = when (e) {
                Emotion.ANGRY -> 2.2f to -1f
                Emotion.SAD, Emotion.SHY -> -1.8f to 0.8f
                Emotion.SMUG -> (if (side > 0) -1f else 0.6f) to 0f
                else -> 0f to 0f
            }
            val inner = Offset(cx - side * 4f, y + innerDy)
            val outer = Offset(cx + side * 4.5f, y + outerDy)
            val mid = Offset(cx, y - 1.2f + (innerDy + outerDy) / 2f)
            val width = if (g == Gender.MALE) 1.2f else 0.75f
            d.drawPath(
                path { moveTo(inner.x, inner.y); quadraticBezierTo(mid.x, mid.y, outer.x, outer.y) },
                hairDark.copy(alpha = 0.85f), style = Stroke(width, cap = StrokeCap.Round),
            )
        }
    }

    // ---------------------------------------------------------------- ears & accessories on top

    fun earsOnTop() {
        when (a.ears) {
            1, 2 -> { // cat / fox
                val tall = if (a.ears == 2) 4f else 0f
                for (side in listOf(-1f, 1f)) {
                    val bx = 50f + side * 16f
                    val ear = path {
                        moveTo(bx - side * 7f, 21f)
                        lineTo(bx + side * 6f, 5f - tall)
                        lineTo(bx + side * 9f, 25f)
                        close()
                    }
                    fillOutlined(ear, hair)
                    val inner = path {
                        moveTo(bx - side * 2.5f, 20f)
                        lineTo(bx + side * 5.5f, 9f - tall)
                        lineTo(bx + side * 6.5f, 22f)
                        close()
                    }
                    d.drawPath(inner, if (a.ears == 2) Color(0xFFFFF4F8) else Color(0xFFFFB8D2))
                    d.drawCircle(Color.White.copy(alpha = 0.72f), 0.7f, Offset(bx + side * 3.5f, 15f - tall * 0.3f))
                    if (a.ears == 2) {
                        d.drawPath(path { moveTo(bx + side * 4f, 9f - tall); lineTo(bx + side * 6f, 5f - tall); lineTo(bx + side * 7f, 10f - tall); close() }, Color.White)
                    }
                }
            }
            3 -> { // bunny
                for (side in listOf(-1f, 1f)) {
                    d.rotate(side * 12f, Offset(50f + side * 10f, 18f)) {
                        val earPath = path { addOval(androidx.compose.ui.geometry.Rect(50f + side * 10f - 4.5f, -10f, 50f + side * 10f + 4.5f, 20f)) }
                        drawPath(earPath, hair.lighter(0.6f))
                        drawPath(earPath, line, style = Stroke(outline))
                        drawOval(Color(0xFFFFB3C9), Offset(50f + side * 10f - 2f, -6f), Size(4f, 22f))
                    }
                }
            }
            5 -> { // horns
                for (side in listOf(-1f, 1f)) {
                    val bx = 50f + side * 13f
                    val horn = path {
                        moveTo(bx - side * 2.5f, 17f)
                        quadraticBezierTo(bx + side * 2f, 4f, bx + side * 8f, 3f)
                        quadraticBezierTo(bx + side * 3f, 9f, bx + side * 3f, 18f)
                        close()
                    }
                    fillOutlined(horn, Color(0xFF3B2A4F))
                    d.drawLine(Color(0xFF8D6FB8), Offset(bx, 13f), Offset(bx + side * 4f, 7f), 0.6f)
                }
            }
            6 -> { // halo
                d.drawOval(Color(0xFFFFE27A).copy(alpha = 0.35f), Offset(33f, 1.5f), Size(34f, 9f), style = Stroke(3f))
                d.drawOval(Color(0xFFFFE27A), Offset(34f, 2.5f), Size(32f, 7f), style = Stroke(1.4f))
            }
        }
    }

    fun accessory() {
        when (a.accessory) {
            1 -> { // big ribbon
                val c = Offset(68f, 17f)
                val bow = path {
                    moveTo(c.x, c.y)
                    cubicTo(c.x - 4f, c.y - 9f, c.x - 13f, c.y - 6f, c.x - 10f, c.y + 2f)
                    cubicTo(c.x - 8f, c.y + 6f, c.x - 3f, c.y + 3f, c.x, c.y)
                    cubicTo(c.x + 4f, c.y - 9f, c.x + 13f, c.y - 6f, c.x + 10f, c.y + 2f)
                    cubicTo(c.x + 8f, c.y + 6f, c.x + 3f, c.y + 3f, c.x, c.y)
                    close()
                }
                fillOutlined(bow, Color(0xFFE5485D))
                d.drawCircle(Color(0xFFC0304A), 2.2f, c)
                d.drawCircle(line, 2.2f, c, style = Stroke(outline))
            }
            2 -> { // X hairclip
                d.drawLine(Color(0xFFFFD983), Offset(62f, 30f), Offset(68f, 36f), 1.4f, cap = StrokeCap.Round)
                d.drawLine(Color(0xFFFFD983), Offset(68f, 30f), Offset(62f, 36f), 1.4f, cap = StrokeCap.Round)
            }
            3 -> { // glasses
                for (side in listOf(-1f, 1f)) {
                    val c = Offset(50f + side * 10.5f, 52.5f)
                    d.drawCircle(Color.White.copy(alpha = 0.12f), 7f, c)
                    d.drawCircle(Color(0xFF3B2A4F), 7f, c, style = Stroke(0.8f))
                    d.drawLine(Color.White.copy(alpha = 0.6f), Offset(c.x - 3f, c.y - 4f), Offset(c.x - 5f, c.y - 1f), 0.6f, cap = StrokeCap.Round)
                }
                d.drawLine(Color(0xFF3B2A4F), Offset(46.5f, 52f), Offset(53.5f, 52f), 0.7f)
            }
            4 -> { // headphones
                d.drawArc(Color(0xFF3B3F5C), 190f, 160f, false, Offset(22f, 9f), Size(56f, 50f), style = Stroke(3f, cap = StrokeCap.Round))
                for (side in listOf(-1f, 1f)) {
                    d.drawRoundRect(Color(0xFFFF8FBF), Offset(50f + side * 26f - 4f, 38f), Size(8f, 13f), androidx.compose.ui.geometry.CornerRadius(3f))
                    d.drawRoundRect(line, Offset(50f + side * 26f - 4f, 38f), Size(8f, 13f), androidx.compose.ui.geometry.CornerRadius(3f), style = Stroke(outline))
                }
            }
            5 -> { // flower
                val c = Offset(69f, 27f)
                for (i in 0 until 5) {
                    val ang = Math.toRadians(i * 72.0 - 90).toFloat()
                    val pc = Offset(c.x + kotlin.math.cos(ang) * 3.2f, c.y + kotlin.math.sin(ang) * 3.2f)
                    d.drawCircle(Color(0xFFFFC2DA), 2.6f, pc)
                    d.drawCircle(line, 2.6f, pc, style = Stroke(0.35f))
                }
                d.drawCircle(Color(0xFFFFD983), 1.6f, c)
            }
            6 -> { // tiara
                val t = path {
                    moveTo(38f, 19f); lineTo(41f, 12f); lineTo(45f, 16f); lineTo(50f, 8f); lineTo(55f, 16f); lineTo(59f, 12f); lineTo(62f, 19f)
                    quadraticBezierTo(50f, 16f, 38f, 19f); close()
                }
                fillOutlined(t, Color(0xFFFFD983))
                d.drawCircle(Color(0xFF9CCBFF), 1.4f, Offset(50f, 13.5f))
            }
            8 -> { // choker with a bell
                d.drawRoundRect(Color(0xFF1E1E26), Offset(44f, 72.5f), Size(12f, 2.4f), androidx.compose.ui.geometry.CornerRadius(1f))
                d.drawCircle(Color(0xFFFFD34D), 2f, Offset(50f, 76.5f))
                d.drawCircle(line, 2f, Offset(50f, 76.5f), style = Stroke(0.4f))
                d.drawLine(line, Offset(48.8f, 77.3f), Offset(51.2f, 77.3f), 0.4f)
                d.drawCircle(Color.White, 0.5f, Offset(49.2f, 75.8f))
            }
            9 -> { // chuuni eyepatch over the right eye
                d.drawLine(Color(0xFF1E1E26), Offset(26f, 40f), Offset(74f, 46f), 0.9f)
                val patch = path { addOval(androidx.compose.ui.geometry.Rect(54.5f, 46f, 66.5f, 58f)) }
                fillOutlined(patch, Color(0xFF1E1E26))
                d.drawLine(Color(0xFFD8334A), Offset(57f, 49f), Offset(64f, 55f), 0.8f, cap = StrokeCap.Round)
                d.drawLine(Color(0xFFD8334A), Offset(64f, 49f), Offset(57f, 55f), 0.8f, cap = StrokeCap.Round)
            }
            10 -> { // kitsune mask worn on the side of the head
                val mask = path {
                    moveTo(64f, 24f); lineTo(66f, 15f); lineTo(70f, 21f); lineTo(76f, 20f); lineTo(78f, 12f); lineTo(80f, 22f)
                    cubicTo(84f, 30f, 80f, 38f, 74f, 40f)
                    cubicTo(68f, 38f, 62f, 32f, 64f, 24f)
                    close()
                }
                fillOutlined(mask, Color.White)
                d.drawLine(Color(0xFFD8334A), Offset(67f, 27f), Offset(71f, 29f), 0.9f, cap = StrokeCap.Round)
                d.drawLine(Color(0xFFD8334A), Offset(75f, 29f), Offset(79f, 27f), 0.9f, cap = StrokeCap.Round)
                d.drawLine(Color(0xFFD8334A), Offset(73f, 33f), Offset(73f, 36f), 0.8f, cap = StrokeCap.Round)
                d.drawPath(path { moveTo(70f, 22f); lineTo(72f, 25f); lineTo(74f, 22f) }, Color(0xFFD8334A), style = Stroke(0.6f))
            }
            11 -> { // witch hat
                val brim = path { addOval(androidx.compose.ui.geometry.Rect(16f, 15f, 84f, 25f)) }
                val cone = path {
                    moveTo(32f, 20f)
                    cubicTo(38f, 6f, 46f, -2f, 66f, -4f)
                    cubicTo(60f, 2f, 62f, 10f, 68f, 20f)
                    close()
                }
                val hat = Color(0xFF3B2A6B)
                fillOutlined(brim, hat)
                fillOutlined(cone, hat)
                d.drawPath(path { moveTo(33f, 17.5f); quadraticBezierTo(50f, 20.5f, 67.5f, 17.5f); lineTo(66.5f, 14f); quadraticBezierTo(50f, 17f, 34.5f, 14f); close() }, Color(0xFFFF8FBF))
                with(d) { star(Offset(58f, 9f), 2.4f, Color(0xFFFFD983)) }
            }
            12 -> { // star hairpins
                with(d) {
                    star(Offset(64f, 30f), 3f, Color(0xFFFFD983))
                    star(Offset(69f, 35f), 2.2f, Color(0xFF9CCBFF))
                    star(Offset(34f, 32f), 2.2f, Color(0xFFFF8FBF))
                }
            }
            13 -> { // beret
                val beret = path {
                    moveTo(30f, 21f)
                    cubicTo(28f, 8f, 66f, 4f, 76f, 14f)
                    cubicTo(80f, 19f, 74f, 23f, 68f, 22f)
                    cubicTo(56f, 19f, 42f, 19f, 30f, 21f)
                    close()
                }
                fillOutlined(beret, cloth.takeIf { it.luminance() < 0.8f } ?: Color(0xFFD8334A))
                d.drawLine(line, Offset(52f, 7f), Offset(53f, 4.5f), 1f, cap = StrokeCap.Round)
            }
            14 -> { // hachimaki headband
                val band = path { moveTo(25.5f, 33f); quadraticBezierTo(50f, 26f, 74.5f, 33f); lineTo(74.5f, 37f); quadraticBezierTo(50f, 30f, 25.5f, 37f); close() }
                fillOutlined(band, Color.White)
                d.drawCircle(Color(0xFFD8334A), 2f, Offset(50f, 31f))
                for (k in 0..1) {
                    val tail = path { moveTo(74f, 34f); lineTo(84f, 38f + k * 5f); lineTo(83f, 41f + k * 5f); lineTo(74f, 36f); close() }
                    fillOutlined(tail, Color.White)
                }
            }
            15 -> { // goggles on the forehead
                val strap = Color(0xFF6B4A35)
                d.drawPath(path { moveTo(25f, 30f); quadraticBezierTo(50f, 22f, 75f, 30f) }, strap, style = Stroke(2.2f))
                for (side in listOf(-1f, 1f)) {
                    val c = Offset(50f + side * 8f, 26.5f)
                    d.drawCircle(Color(0xFFB08A4A), 5f, c)
                    d.drawCircle(line, 5f, c, style = Stroke(0.5f))
                    d.drawCircle(Brush.radialGradient(listOf(Color(0xFF9FE6FF), Color(0xFF3D7BFF)), c, 3.6f), 3.6f, c)
                    d.drawCircle(Color.White.copy(alpha = 0.8f), 1f, Offset(c.x - 1.3f, c.y - 1.3f))
                }
            }
            7 -> { // maid headband
                val band = path { moveTo(29f, 24f); quadraticBezierTo(50f, 12f, 71f, 24f); lineTo(70f, 27f); quadraticBezierTo(50f, 16f, 30f, 27f); close() }
                fillOutlined(band, Color(0xFF1E1E26))
                for (i in 0..6) {
                    val x = 32f + i * 6f
                    val y = 20.5f - (3 - kotlin.math.abs(i - 3)) * 1.6f
                    d.drawCircle(Color.White, 2.6f, Offset(x, y))
                    d.drawCircle(line, 2.6f, Offset(x, y), style = Stroke(0.35f))
                }
            }
        }
    }

    fun expressionMarks() {
        when (e) {
            Emotion.ANGRY -> { // 💢
                val c = Offset(70f, 30f)
                for (i in 0 until 4) {
                    d.rotate(i * 90f, c) {
                        drawPath(path { moveTo(c.x + 1f, c.y - 4f); quadraticBezierTo(c.x + 1f, c.y - 1f, c.x + 4f, c.y - 1f) }, Color(0xFFE5485D), style = Stroke(1.1f, cap = StrokeCap.Round))
                    }
                }
            }
            Emotion.SHY -> { // sweat drop
                val p = path { moveTo(73f, 36f); cubicTo(71f, 41f, 76f, 41f, 74.5f, 36f); quadraticBezierTo(74f, 34f, 73f, 36f); close() }
                d.drawPath(p, Color(0xFF9FD9FF))
                d.drawPath(p, line, style = Stroke(0.35f))
            }
            Emotion.LOVE -> heartFloat(Offset(74f, 30f))
            Emotion.SURPRISED -> {
                d.drawLine(line, Offset(72f, 26f), Offset(76f, 22f), 0.8f, cap = StrokeCap.Round)
                d.drawLine(line, Offset(74f, 30f), Offset(79f, 29f), 0.8f, cap = StrokeCap.Round)
            }
            Emotion.THINKING -> {
                d.drawCircle(Color.White.copy(alpha = 0.85f), 1.2f, Offset(73f, 30f))
                d.drawCircle(Color.White.copy(alpha = 0.85f), 1.8f, Offset(76f, 25f))
                d.drawCircle(Color.White.copy(alpha = 0.85f), 2.6f, Offset(80f, 19f))
            }
            else -> {}
        }
    }

    private fun heartFloat(c: Offset) {
        with(d) { heart(c, 3f, Color(0xFFFF4F8B)) }
    }
}

/** Luminance helper (Compose has one, but this keeps the painter self-contained). */
private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
