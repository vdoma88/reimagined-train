package com.animate.companion.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

data class LofiTrack(
    val title: String,
    val bpm: Float,
    /** One chord per bar, MIDI note numbers. The progression is played twice per loop. */
    val chords: List<IntArray>,
    val bassRoots: IntArray,
    val scale: IntArray,
    val rain: Boolean,
    val seed: Int,
)

object LofiTracks {
    val all = listOf(
        LofiTrack(
            "Сакура под дождём", 72f,
            listOf(intArrayOf(53, 57, 60, 64), intArrayOf(52, 55, 59, 62), intArrayOf(50, 53, 57, 60), intArrayOf(48, 52, 55, 59)),
            intArrayOf(41, 40, 38, 36),
            intArrayOf(72, 74, 76, 79, 81, 84), rain = true, seed = 11,
        ),
        LofiTrack(
            "Ночной Токио", 68f,
            listOf(intArrayOf(57, 60, 64, 67, 71), intArrayOf(50, 53, 57, 60, 64), intArrayOf(55, 59, 62, 65, 69), intArrayOf(48, 52, 55, 59, 62)),
            intArrayOf(45, 38, 43, 36),
            intArrayOf(69, 72, 74, 76, 79, 81), rain = false, seed = 23,
        ),
        LofiTrack(
            "Кафе у станции", 80f,
            listOf(intArrayOf(51, 55, 58, 62), intArrayOf(48, 51, 55, 58), intArrayOf(53, 56, 60, 63), intArrayOf(50, 53, 56, 60)),
            intArrayOf(39, 36, 41, 46),
            intArrayOf(70, 72, 75, 77, 79, 82), rain = false, seed = 37,
        ),
    )
}

/** Renders seamless procedural lo-fi loops (Rhodes-like keys, bass, swung drums, vinyl). */
object LofiComposer {
    private fun midiHz(n: Int) = 440f * 2f.pow((n - 69) / 12f)

    fun render(track: LofiTrack): ShortArray {
        val rnd = Random(track.seed)
        val beat = 60f / track.bpm
        val bars = 8
        val n = (bars * 4 * beat * SAMPLE_RATE).toInt()
        val l = FloatArray(n)
        val r = FloatArray(n)

        fun add(start: Float, len: Float, pan: Float, gen: (t: Float) -> Float) {
            val s0 = (start * SAMPLE_RATE).toInt()
            val count = (len * SAMPLE_RATE).toInt()
            val gl = 1f - pan.coerceAtLeast(0f)
            val gr = 1f + pan.coerceAtMost(0f)
            for (i in 0 until count) {
                val v = gen(i.toFloat() / SAMPLE_RATE)
                val idx = (s0 + i) % n // wrap tails around for a seamless loop
                l[idx] += v * gl
                r[idx] += v * gr
            }
        }

        fun keys(start: Float, note: Int, vel: Float, hold: Float, pan: Float) {
            val f = midiHz(note)
            val detune = 1f + (rnd.nextFloat() - 0.5f) * 0.004f
            add(start, hold + 0.6f, pan) { t ->
                val att = (t / 0.006f).coerceAtMost(1f)
                val rel = if (t > hold) exp(-(t - hold) / 0.15f) else 1f
                val env = att * exp(-t / 1.6f) * rel
                val index = 1.4f * exp(-t / 0.35f)
                val ph = TWO_PI * f * detune * t
                val body = sin(ph + index * sin(ph))
                val tine = 0.12f * sin(ph * 7f) * exp(-t / 0.05f)
                val trem = 1f + 0.12f * sin(TWO_PI * 4.5f * t)
                (body + tine) * env * trem * vel * 0.16f
            }
        }

        fun bass(start: Float, note: Int, len: Float) {
            val f = midiHz(note)
            add(start, len + 0.2f, 0f) { t ->
                val att = (t / 0.01f).coerceAtMost(1f)
                val rel = if (t > len) exp(-(t - len) / 0.05f) else 1f
                val v = kotlin.math.tanh(1.6f * sin(TWO_PI * f * t)) + 0.2f * sin(TWO_PI * 2 * f * t)
                v * att * rel * exp(-t / 1.2f) * 0.32f
            }
        }

        val noise = Noise(track.seed * 7 + 1)

        fun kick(start: Float, vel: Float) {
            var phase = 0f
            add(start, 0.45f, 0f) { t ->
                val f = 48f + 75f * exp(-t / 0.035f)
                phase += TWO_PI * f / SAMPLE_RATE
                sin(phase) * exp(-t / 0.22f) * vel * 0.55f
            }
        }

        fun snare(start: Float, vel: Float) {
            val bp = BandPass(1800f, 0.8f)
            add(start, 0.35f, 0.1f) { t ->
                val nz = bp.process(noise.next()) * exp(-t / 0.08f)
                val tone = sin(TWO_PI * 185f * t) * exp(-t / 0.045f)
                (nz * 0.9f + tone * 0.35f) * vel * 0.32f
            }
        }

        fun hat(start: Float, vel: Float) {
            val bp = BandPass(7500f, 1.2f)
            add(start, 0.12f, -0.25f) { t -> bp.process(noise.next()) * exp(-t / 0.022f) * vel * 0.22f }
        }

        fun bell(start: Float, note: Int, vel: Float) {
            val f = midiHz(note)
            add(start, 2.2f, 0.3f) { t ->
                val vib = 1f + 0.004f * sin(TWO_PI * 5f * t) * (t / 0.4f).coerceAtMost(1f)
                val att = (t / 0.01f).coerceAtMost(1f)
                (sin(TWO_PI * f * vib * t) + 0.25f * sin(TWO_PI * 2f * f * t) * exp(-t / 0.3f)) *
                    att * exp(-t / 0.7f) * vel * 0.09f
            }
        }

        val swing = beat * 0.17f
        for (bar in 0 until bars) {
            val b0 = bar * 4 * beat
            val chord = track.chords[bar % track.chords.size]
            val lastBar = bar == bars - 1
            // Keys: a slightly strummed hit on 1, a softer stab on the "and" of 2.
            chord.forEachIndexed { i, note ->
                keys(b0 + i * 0.012f, note, 0.9f - i * 0.05f, beat * 2.2f, (i - chord.size / 2f) * 0.15f)
                if (!lastBar || rnd.nextBoolean()) {
                    keys(b0 + beat * 2.5f + swing + i * 0.01f, note, 0.5f, beat * 1.2f, (i - chord.size / 2f) * 0.15f)
                }
            }
            val root = track.bassRoots[bar % track.bassRoots.size]
            bass(b0, root, beat * 1.6f)
            bass(b0 + beat * 2.5f + swing, if (rnd.nextBoolean()) root else root + 7, beat * 1.2f)

            kick(b0, 1f)
            kick(b0 + beat * 2.5f + swing, 0.8f)
            if (rnd.nextFloat() < 0.4f) kick(b0 + beat * 1.5f + swing, 0.55f)
            snare(b0 + beat, 0.9f)
            snare(b0 + beat * 3, if (lastBar) 1f else 0.9f)
            if (lastBar) snare(b0 + beat * 3.5f + swing, 0.4f)
            for (e in 0 until 8) {
                val off = if (e % 2 == 1) swing else 0f
                hat(b0 + e * beat / 2 + off, if (e % 2 == 0) 0.9f else 0.55f + rnd.nextFloat() * 0.2f)
            }
            // Sparse pentatonic melody.
            for (step in 0 until 8) {
                if (rnd.nextFloat() < 0.28f) {
                    val off = if (step % 2 == 1) swing else 0f
                    bell(b0 + step * beat / 2 + off, track.scale.random(rnd), 0.6f + rnd.nextFloat() * 0.4f)
                }
            }
        }

        // Vinyl hiss, crackle and (optionally) rain.
        val hiss = OnePoleLowPass(3000f)
        val rainBp = BandPass(2500f, 0.5f)
        for (i in 0 until n) {
            var v = hiss.process(noise.next()) * 0.015f
            if (rnd.nextFloat() < 6f / SAMPLE_RATE) {
                val amp = 0.08f + rnd.nextFloat() * 0.18f
                for (k in 0 until 40) {
                    val idx = (i + k) % n
                    val c = noise.next() * amp * exp(-k / 6f)
                    l[idx] += c; r[idx] += c
                }
            }
            if (track.rain) {
                v += rainBp.process(noise.next()) * 0.03f
                if (rnd.nextFloat() < 25f / SAMPLE_RATE) {
                    val amp = 0.03f + rnd.nextFloat() * 0.05f
                    val f = 2000f + rnd.nextFloat() * 3000f
                    for (k in 0 until 200) {
                        val idx = (i + k) % n
                        val d = sin(TWO_PI * f * k / SAMPLE_RATE) * amp * exp(-k / 40f)
                        l[idx] += d * 0.7f; r[idx] += d
                    }
                }
            }
            l[i] += v; r[i] += v
        }

        // Warm low-pass on the master bus, then interleave.
        val lpL = OnePoleLowPass(4200f)
        val lpR = OnePoleLowPass(4200f)
        val out = FloatArray(n * 2)
        for (i in 0 until n) {
            out[2 * i] = lpL.process(l[i])
            out[2 * i + 1] = lpR.process(r[i])
        }
        return out.toPcm(peak = 0.8f, drive = 1.4f)
    }
}

/**
 * Streams a rendered loop endlessly with fades. A writer thread feeds the track in chunks,
 * so no device-specific limit on static buffer size applies. Public calls are main-thread safe.
 */
class LofiPlayer(private val scope: CoroutineScope) {
    private val cache = HashMap<Int, ShortArray>()
    private val lock = Mutex()
    @Volatile private var track: AudioTrack? = null
    private var writer: Thread? = null
    private var currentIndex = -1
    @Volatile private var volume = 0.5f
    private var job: Job? = null

    fun play(index: Int, vol: Float) {
        volume = vol
        job?.cancel()
        job = scope.launch {
            lock.withLock {
                val i = index.coerceIn(0, LofiTracks.all.lastIndex)
                val existing = track
                if (currentIndex == i && existing != null) {
                    existing.setVolume(volume)
                    if (existing.playState != AudioTrack.PLAYSTATE_PLAYING) existing.play()
                    return@withLock
                }
                fadeOutAndRelease()
                val pcm = cache.getOrPut(i) { withContext(Dispatchers.Default) { LofiComposer.render(LofiTracks.all[i]) } }
                val t = build()
                track = t
                currentIndex = i
                t.setVolume(0f)
                t.play()
                startWriter(t, pcm)
                for (step in 1..20) {
                    t.setVolume(volume * step / 20f)
                    delay(60)
                }
            }
        }
    }

    fun setVolume(vol: Float) {
        volume = vol
        track?.setVolume(vol)
    }

    fun pause() {
        job?.cancel()
        runCatching { track?.pause() }
    }

    fun stop() {
        job?.cancel()
        job = scope.launch { lock.withLock { fadeOutAndRelease() } }
    }

    private fun startWriter(t: AudioTrack, pcm: ShortArray) {
        writer = Thread({
            val chunk = 4096
            var pos = 0
            while (track === t) {
                val n = minOf(chunk, pcm.size - pos)
                // Blocks while the track buffer is full (or paused); returns <0 once released.
                if (t.write(pcm, pos, n) < 0) break
                pos = (pos + n) % pcm.size
            }
        }, "lofi-writer").apply {
            isDaemon = true
            start()
        }
    }

    private suspend fun fadeOutAndRelease() {
        val t = track ?: return
        for (step in 10 downTo 0) {
            runCatching { t.setVolume(volume * step / 10f) }
            delay(40)
        }
        track = null
        runCatching { t.pause(); t.flush(); t.stop(); t.release() }
        writer = null
        currentIndex = -1
    }

    private fun build(): AudioTrack {
        val minBuf = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(maxOf(minBuf * 4, 32 * 1024))
            .build()
    }
}
