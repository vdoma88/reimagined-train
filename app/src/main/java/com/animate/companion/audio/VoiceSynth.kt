package com.animate.companion.audio

import com.animate.companion.model.Emotion
import com.animate.companion.model.Gender
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Formant synthesizer for short anime-style vocal interjections ("nya~", "ehehe", "kyaa!").
 *
 * Voiced sound is additive: harmonics of the pitch weighted by a vowel formant envelope,
 * so there is no aliasing and vowels glide smoothly. Consonants are filtered noise bursts
 * or formant transitions.
 */
object VoiceSynth {
    enum class Cons { NONE, N, M, K, T, H, F, B, Y, NY, KY, W, S, SH, R, G }

    /** [vowel] is one of a i u e o, 'm' for a closed-mouth hum, '_' for consonant only. */
    data class Syl(val cons: Cons, val vowel: Char, val dur: Float, val pitch: FloatArray, val amp: Float = 1f)

    data class Interjection(val id: String, val text: String, val syllables: List<Syl>, val vibrato: Float = 0.02f)

    data class Voice(val f0: Float, val formantScale: Float, val breath: Float)

    private val formants = mapOf(
        'a' to floatArrayOf(850f, 1350f, 2900f),
        'i' to floatArrayOf(320f, 2650f, 3300f),
        'u' to floatArrayOf(350f, 1450f, 2600f),
        'e' to floatArrayOf(500f, 2150f, 2900f),
        'o' to floatArrayOf(520f, 950f, 2800f),
        'm' to floatArrayOf(260f, 1100f, 2500f),
        '_' to floatArrayOf(500f, 1500f, 2500f),
    )
    private val gains = floatArrayOf(1f, 0.55f, 0.22f)
    private val bandwidths = floatArrayOf(90f, 120f, 180f)

    private fun syl(s: String, dur: Float, vararg pitch: Float, amp: Float = 1f): Syl {
        val prefixes = listOf(
            "ky" to Cons.KY, "ny" to Cons.NY, "sh" to Cons.SH, "k" to Cons.K, "g" to Cons.G, "n" to Cons.N,
            "m" to Cons.M, "t" to Cons.T, "h" to Cons.H, "f" to Cons.F, "b" to Cons.B, "y" to Cons.Y,
            "w" to Cons.W, "s" to Cons.S, "r" to Cons.R,
        )
        if (s == "m" || s == "n") return Syl(Cons.NONE, 'm', dur, pitch, amp)
        val (prefix, cons) = prefixes.firstOrNull { s.startsWith(it.first) && s.length > it.first.length } ?: ("" to Cons.NONE)
        return Syl(cons, s.substring(prefix.length).first(), dur, pitch, amp)
    }

    val NYA = Interjection("nya", "Ня~", listOf(syl("nya", 0.4f, 1f, 1.3f, 1.15f, 0.95f)), 0.03f)
    val EHEHE = Interjection("ehehe", "Эхехе~", listOf(syl("e", 0.1f, 1.05f, 1.1f), syl("he", 0.09f, 1.2f, 1.15f), syl("he", 0.18f, 1.25f, 1.1f, 1f)))
    val KYAA = Interjection("kyaa", "Кья~!", listOf(syl("kya", 0.48f, 1.2f, 1.55f, 1.45f, 1.3f)), 0.035f)
    val UUU = Interjection("uuu", "Ууу…", listOf(syl("u", 0.65f, 1f, 0.96f, 0.86f, 0.78f, amp = 0.8f)), 0.05f)
    val HMPH = Interjection("hmph", "Хмф!", listOf(syl("fu", 0.07f, 1.15f, amp = 0.6f), syl("m", 0.22f, 1.15f, 1f, 0.88f)))
    val EH = Interjection("eh", "Э?!", listOf(syl("e", 0.3f, 0.95f, 1.1f, 1.6f)))
    val HAWAWA = Interjection("hawawa", "Хававаа~", listOf(syl("ha", 0.1f, 1.2f), syl("wa", 0.1f, 1.3f), syl("wa", 0.28f, 1.25f, 1.38f, 1.2f)), 0.04f)
    val UFUFU = Interjection("ufufu", "Уфуфу~", listOf(syl("u", 0.09f, 1f), syl("fu", 0.09f, 1.1f), syl("fu", 0.22f, 1.15f, 1.05f)))
    val MMM = Interjection("mmm", "Хмм~", listOf(syl("m", 0.55f, 1f, 0.95f, 1.05f, 1f, amp = 0.75f)))
    val YATTA = Interjection("yatta", "Ятта!", listOf(syl("ya", 0.13f, 1.1f, 1.2f), syl("ta", 0.32f, 1.4f, 1.5f, 1.35f)))
    val BAKA = Interjection("baka", "Бака!", listOf(syl("ba", 0.12f, 1.2f, 1.3f), syl("ka", 0.3f, 1.4f, 1.1f)))
    val ANO = Interjection("ano", "А-ано…", listOf(syl("a", 0.14f, 1f, amp = 0.7f), syl("no", 0.34f, 1.05f, 0.95f, 0.9f, amp = 0.7f)))
    val AHAHA = Interjection("ahaha", "Ахаха!", listOf(syl("a", 0.1f, 1.2f), syl("ha", 0.09f, 1.3f), syl("ha", 0.09f, 1.25f), syl("ha", 0.2f, 1.2f, 1f)))
    val ARAARA = Interjection("araara", "Ара-ара~", listOf(syl("a", 0.1f, 1f), syl("ra", 0.15f, 1.1f, 1f), syl("a", 0.1f, 1.05f), syl("ra", 0.32f, 1.15f, 1f, 0.9f)))
    val SUGOI = Interjection("sugoi", "Сугой!", listOf(syl("su", 0.12f, 1.1f), syl("go", 0.12f, 1.2f), syl("i", 0.28f, 1.35f, 1.5f, 1.4f)))
    val FUWAA = Interjection("fuwaa", "Фуваа~", listOf(syl("fu", 0.12f, 1.1f), syl("wa", 0.65f, 1.1f, 1f, 0.8f, 0.7f, amp = 0.8f)))
    val KYUN = Interjection("kyun", "Кюн~", listOf(syl("kyu", 0.2f, 1.2f, 1.5f), syl("n", 0.22f, 1.45f, 1.3f)), 0.04f)
    val UWAA = Interjection("uwaa", "Уваа!", listOf(syl("u", 0.08f, 1.1f), syl("wa", 0.42f, 1.2f, 1.6f, 1.4f)))

    val YOSH = Interjection("yosh", "Ёш!", listOf(syl("yo", 0.17f, 1f, 1.2f), Syl(Cons.SH, '_', 0.1f, floatArrayOf(1f))))
    val HEHE_M = Interjection("heh", "Хе-хе", listOf(syl("he", 0.11f, 1f, 0.95f), syl("he", 0.16f, 1f, 0.9f)))
    val OI = Interjection("oi", "Ой!", listOf(syl("o", 0.1f, 1.1f), syl("i", 0.2f, 1.3f, 1.2f)))
    val NANI = Interjection("nani", "Нани?!", listOf(syl("na", 0.13f, 1f), syl("ni", 0.32f, 1.1f, 1.5f)))
    val OOH = Interjection("ooh", "Оо~", listOf(syl("o", 0.42f, 1f, 1.2f, 1.1f)))
    val HAHA = Interjection("haha", "Ха-ха!", listOf(syl("ha", 0.12f, 1.1f), syl("ha", 0.22f, 1.15f, 1f)))
    val UGH = Interjection("ugh", "Угх…", listOf(syl("u", 0.4f, 1f, 0.85f, amp = 0.8f), syl("gu", 0.1f, 0.8f, amp = 0.5f)))

    fun choose(emotion: Emotion, gender: Gender, catEars: Boolean, sleepy: Boolean, random: Random = Random.Default): Interjection? {
        val male = gender == Gender.MALE
        val options: List<Interjection> = when (emotion) {
            Emotion.HAPPY -> if (male) listOf(YOSH, HAHA, OOH) else listOf(EHEHE, YATTA, SUGOI) + if (catEars) listOf(NYA, NYA) else emptyList()
            Emotion.LAUGH -> if (male) listOf(HAHA, HEHE_M) else listOf(AHAHA, EHEHE)
            Emotion.SHY -> if (male) listOf(ANO) else listOf(HAWAWA, ANO)
            Emotion.ANGRY -> if (male) listOf(HMPH, OI) else listOf(HMPH, BAKA)
            Emotion.SAD -> if (male) listOf(UGH) else listOf(UUU)
            Emotion.SURPRISED -> if (male) listOf(NANI, EH, OI) else listOf(EH, UWAA, KYAA)
            Emotion.SMUG -> if (male) listOf(HEHE_M) else listOf(UFUFU, ARAARA)
            Emotion.LOVE -> if (male) listOf(OOH, HEHE_M) else listOf(KYUN, KYAA) + if (catEars) listOf(NYA) else emptyList()
            Emotion.THINKING -> if (sleepy) listOf(FUWAA, MMM) else listOf(MMM)
            Emotion.NEUTRAL -> if (catEars && !male) listOf(NYA) else if (sleepy) listOf(FUWAA) else emptyList()
        }
        return options.randomOrNull(random)
    }

    fun voiceFor(gender: Gender, archetypePitch: Float, seed: Int): Voice {
        val jitter = 0.93f + Random(seed).nextFloat() * 0.14f
        return when (gender) {
            Gender.FEMALE -> Voice(400f * archetypePitch * jitter, 1.12f, 0.03f)
            Gender.MALE -> Voice(165f * archetypePitch * jitter, 0.92f, 0.02f)
            Gender.NEUTRAL -> Voice(285f * archetypePitch * jitter, 1.03f, 0.025f)
        }
    }

    private fun contour(p: FloatArray, x: Float): Float {
        if (p.size == 1) return p[0]
        val pos = x.coerceIn(0f, 1f) * (p.size - 1)
        val i = pos.toInt().coerceAtMost(p.size - 2)
        val f = pos - i
        return p[i] * (1 - f) + p[i + 1] * f
    }

    private fun envelope(freq: Float, f: FloatArray): Float {
        var e = 0.015f
        for (i in 0..2) {
            val d = (freq - f[i]) / bandwidths[i]
            e += gains[i] / (1 + d * d)
        }
        return e
    }

    fun render(ij: Interjection, voice: Voice, seed: Int = 1): ShortArray {
        val noise = Noise(seed)
        val breathLp = OnePoleLowPass(2500f)
        val total = ij.syllables.sumOf { (it.dur + 0.025).toDouble() }.toFloat() + 0.08f
        val out = FloatArray((total * SAMPLE_RATE).toInt())
        var cursor = 0
        var phase = 0f
        val maxH = 28
        val amps = FloatArray(maxH + 1)
        val cur = FloatArray(3)

        for (s in ij.syllables) {
            val target = formants.getValue(s.vowel).map { it * voice.formantScale }.toFloatArray()
            val locus: FloatArray? = when (s.cons) {
                Cons.N -> floatArrayOf(250f, 1700f, 2700f)
                Cons.M, Cons.B -> floatArrayOf(260f, 950f, 2400f)
                Cons.Y, Cons.NY, Cons.KY -> formants.getValue('i')
                Cons.W -> floatArrayOf(320f, 750f, 2400f)
                Cons.G -> floatArrayOf(300f, 1900f, 2500f)
                else -> null
            }?.map { it * voice.formantScale }?.toFloatArray()
            val (burstLen, burstFreq, burstQ) = when (s.cons) {
                Cons.K, Cons.KY -> Triple(0.035f, 2400f, 2f)
                Cons.T -> Triple(0.03f, 4200f, 1.5f)
                Cons.S -> Triple(0.09f, 6500f, 2f)
                Cons.SH -> Triple(0.1f, 3200f, 1.5f)
                Cons.H -> Triple(0.06f, target[1], 1f)
                Cons.F -> Triple(0.06f, 1300f, 0.7f)
                Cons.G -> Triple(0.015f, 2000f, 1.5f)
                else -> Triple(0f, 1000f, 1f)
            }
            val nasalStart = s.cons == Cons.N || s.cons == Cons.NY || s.cons == Cons.M
            val bp = BandPass(burstFreq, burstQ)
            val voiced = s.vowel != '_'
            val glide = if (s.cons == Cons.Y || s.cons == Cons.NY || s.cons == Cons.KY || s.cons == Cons.W) 0.09f else 0.05f
            val n = ((s.dur + burstLen) * SAMPLE_RATE).toInt()
            for (i in 0 until n) {
                val idx = cursor + i
                if (idx >= out.size) break
                val t = i.toFloat() / SAMPLE_RATE
                var v = 0f
                if (t < burstLen) {
                    val bEnv = if (s.cons == Cons.S || s.cons == Cons.SH) sin(Math.PI.toFloat() * t / burstLen) else exp(-t / (burstLen * 0.5f))
                    v += bp.process(noise.next()) * bEnv * 0.9f
                }
                val tv = t - burstLen
                if (voiced && tv >= 0f) {
                    val p = tv / s.dur
                    val vib = 1f + ij.vibrato * sin(TWO_PI * 5.5f * tv) * min(1f, tv / 0.15f)
                    val f0 = voice.f0 * contour(s.pitch, p) * vib
                    phase += TWO_PI * f0 / SAMPLE_RATE
                    if (phase > TWO_PI) phase -= TWO_PI
                    val g = if (locus != null) (tv / glide).coerceIn(0f, 1f) else 1f
                    for (k in 0..2) cur[k] = if (locus != null) locus[k] + (target[k] - locus[k]) * g else target[k]
                    val nh = min(maxH, (5200f / f0).toInt())
                    for (h in 1..nh) amps[h] = envelope(h * f0, cur) / h.toFloat().pow(0.7f)
                    var s0 = 0f
                    for (h in 1..nh) s0 += amps[h] * sin(phase * h)
                    val att = (tv / 0.018f).coerceAtMost(1f)
                    val rel = ((s.dur - tv) / 0.07f).coerceIn(0f, 1f)
                    val nasal = if (nasalStart) (0.45f + 0.55f * (tv / 0.06f).coerceAtMost(1f)) else 1f
                    val hum = if (s.vowel == 'm') 0.6f else 1f
                    val flap = if (s.cons == Cons.R) 1f - 0.7f * exp(-tv / 0.02f) else 1f
                    val env = att * rel * nasal * hum * flap * s.amp
                    v += s0 * env + breathLp.process(noise.next()) * voice.breath * 2f * env
                }
                out[idx] += v
            }
            cursor += n + (0.012f * SAMPLE_RATE).toInt()
        }
        return out.toPcm(peak = 0.9f, drive = 1.1f)
    }
}

/** Synthesized UI sounds. */
object Sfx {
    private fun bell(out: FloatArray, start: Int, f: Float, amp: Float, decay: Float) {
        for (i in 0 until out.size - start) {
            val t = i.toFloat() / SAMPLE_RATE
            val v = (sin(TWO_PI * f * t) + 0.3f * sin(TWO_PI * f * 2.76f * t) * exp(-t / 0.05f)) *
                exp(-t / decay) * (t / 0.003f).coerceAtMost(1f)
            out[start + i] += v * amp
        }
    }

    private fun midi(n: Int) = 440f * 2f.pow((n - 69) / 12f)

    val send: ShortArray by lazy {
        val n = (0.12f * SAMPLE_RATE).toInt()
        var ph = 0f
        FloatArray(n) { i ->
            val t = i.toFloat() / SAMPLE_RATE
            ph += TWO_PI * (520f + 900f * (t / 0.12f)) / SAMPLE_RATE
            sin(ph) * exp(-t / 0.04f) * (t / 0.004f).coerceAtMost(1f)
        }.toPcm(0.5f)
    }

    val receive: ShortArray by lazy {
        val out = FloatArray((0.7f * SAMPLE_RATE).toInt())
        bell(out, 0, midi(88), 0.6f, 0.25f)
        bell(out, (0.07f * SAMPLE_RATE).toInt(), midi(95), 0.5f, 0.35f)
        out.toPcm(0.45f)
    }

    val tap: ShortArray by lazy {
        val n = (0.05f * SAMPLE_RATE).toInt()
        FloatArray(n) { i ->
            val t = i.toFloat() / SAMPLE_RATE
            sin(TWO_PI * 1400f * t) * exp(-t / 0.012f)
        }.toPcm(0.3f)
    }

    val sparkle: ShortArray by lazy {
        val out = FloatArray((1.3f * SAMPLE_RATE).toInt())
        listOf(84, 86, 88, 91, 93, 96).forEachIndexed { i, note ->
            bell(out, (i * 0.07f * SAMPLE_RATE).toInt(), midi(note), 0.5f, 0.4f)
        }
        out.toPcm(0.55f)
    }

    val dice: ShortArray by lazy {
        val out = FloatArray((0.35f * SAMPLE_RATE).toInt())
        listOf(79, 83, 86).forEachIndexed { i, note ->
            bell(out, (i * 0.06f * SAMPLE_RATE).toInt(), midi(note), 0.5f, 0.06f)
        }
        out.toPcm(0.45f)
    }
}
