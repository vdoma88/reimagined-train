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
    /** Original eighth-note melody; -1 is a rest. */
    val melody: IntArray = intArrayOf(),
)

/** Original mystery-adventure themes; stable indices preserve saved music settings. */
object LofiTracks {
    val all = listOf(
        LofiTrack("Тропа к загадкам", 96f,
            listOf(intArrayOf(47,54,59,62), intArrayOf(43,50,55,59), intArrayOf(40,47,52,55), intArrayOf(42,49,54,58)),
            intArrayOf(35,31,28,30), intArrayOf(71,74,76,78,81), rain = true, seed = 101,
            melody = intArrayOf(71,-1,74,78,76,-1,74,71, 69,-1,71,74,76,-1,74,69,
                67,71,74,-1,76,74,71,-1, 70,-1,73,78,76,73,70,-1)),
        LofiTrack("Чердачный дневник", 82f,
            listOf(intArrayOf(50,57,62,65), intArrayOf(46,53,58,62), intArrayOf(43,50,55,58), intArrayOf(45,52,57,61)),
            intArrayOf(38,34,31,33), intArrayOf(74,76,77,79,81), rain = true, seed = 211,
            melody = intArrayOf(74,-1,77,81,-1,79,77,-1, 74,77,82,-1,81,77,74,-1,
                79,-1,82,81,79,-1,77,74, 73,-1,76,81,79,76,73,-1)),
        LofiTrack("Летний лагерь", 94f,
            listOf(intArrayOf(52,59,64,67), intArrayOf(48,55,60,64), intArrayOf(45,52,57,60), intArrayOf(47,54,59,63)),
            intArrayOf(40,36,33,35), intArrayOf(76,78,79,81,83), rain = false, seed = 307,
            melody = intArrayOf(76,79,-1,83,81,79,76,-1, 76,79,84,-1,83,79,76,-1,
                81,-1,84,83,81,79,76,-1, 78,83,87,-1,83,78,75,-1)),
    )
}

/** Renders original forest-adventure loops: plucked strings, flute, bells and wooden percussion. */
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
                val att = (t / 0.003f).coerceAtMost(1f)
                val rel = if (t > hold) exp(-(t - hold) / 0.09f) else 1f
                val ph = TWO_PI * f * detune * t
                val string = sin(ph) + 0.32f * sin(2f * ph) * exp(-t / 0.16f) +
                    0.18f * sin(3f * ph) * exp(-t / 0.10f)
                string * att * exp(-t / 0.38f) * rel * vel * 0.15f
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

        fun flute(start: Float, note: Int, vel: Float) {
            val f = midiHz(note)
            val hold = beat * 0.42f
            add(start, hold + 0.15f, -0.12f) { t ->
                val env = (t / 0.025f).coerceAtMost(1f) *
                    (if (t > hold) exp(-(t - hold) / 0.05f) else 1f)
                val phase = TWO_PI * f * t + 0.045f * sin(TWO_PI * 5f * t)
                (sin(phase) + 0.12f * sin(2f * phase)) * env * vel * 0.16f
            }
        }

        fun wood(start: Float, vel: Float) {
            add(start, 0.1f, 0.22f) { t ->
                (sin(TWO_PI * 720f * t) + 0.3f * sin(TWO_PI * 1140f * t)) *
                    exp(-t / 0.021f) * vel * 0.12f
            }
        }

        for (bar in 0 until bars) {
            val b0 = bar * 4 * beat
            val chord = track.chords[bar % track.chords.size]
            // Light finger-picked arpeggio rather than an electric-piano chord pad.
            for (step in 0 until 8) {
                val note = chord[step % chord.size]
                keys(b0 + step * beat / 2f, note, if (step % 2 == 0) 0.75f else 0.45f,
                    beat * 0.4f, if (step % 2 == 0) -0.2f else 0.2f)
                wood(b0 + step * beat / 2f, if (step % 2 == 0) 0.4f else 0.18f)
                if (track.melody.isNotEmpty()) {
                    val lead = track.melody[(bar * 8 + step) % track.melody.size]
                    if (lead >= 0) flute(b0 + step * beat / 2f, lead, if (bar < 4) 0.85f else 0.7f)
                }
            }
            val root = track.bassRoots[bar % track.bassRoots.size]
            bass(b0, root, beat * 1.3f)
            bass(b0 + beat * 2f, root + 7, beat * 1.1f)
            kick(b0, 0.45f)
            kick(b0 + beat * 2f, 0.3f)
            snare(b0 + beat, 0.35f)
            snare(b0 + beat * 3f, 0.3f)
            hat(b0 + beat * 1.5f, 0.2f)
            hat(b0 + beat * 3.5f, 0.15f)
            if (bar % 2 == 0) bell(b0 + beat * 3.5f, chord.last() + 24, 0.45f)
        }

        // Very quiet forest air. No old vinyl crackle or anime café texture.
        if (track.rain) {
            val wind = OnePoleLowPass(750f)
            for (i in 0 until n) {
                val v = wind.process(noise.next()) * 0.006f
                l[i] += v; r[i] += v
            }
        }

        // Warm low-pass on the master bus, then interleave.
        val lpL = OnePoleLowPass(4200f)
        val lpR = OnePoleLowPass(4200f)
        // Prime the filters with the wrapped tail to avoid a startup click on every loop.
        for (i in maxOf(0, n - 2048) until n) { lpL.process(l[i]); lpR.process(r[i]) }
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

