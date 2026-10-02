package com.animate.companion.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.tanh

const val SAMPLE_RATE = 22050
const val TWO_PI = (2 * PI).toFloat()

/** Normalizes to [peak], applies gentle saturation and converts to 16-bit PCM. */
fun FloatArray.toPcm(peak: Float = 0.85f, drive: Float = 1.2f): ShortArray {
    var m = 1e-6f
    for (v in this) m = max(m, abs(v))
    val g = peak / m
    return ShortArray(size) { i ->
        val v = tanh(this[i] * g * drive) / tanh(drive)
        (v.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
    }
}

/** Fire-and-forget playback of a short mono clip. */
object PcmPlayer {
    private val main = Handler(Looper.getMainLooper())

    fun play(pcm: ShortArray, volume: Float) {
        if (pcm.isEmpty() || volume <= 0f) return
        runCatching {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build()
            track.write(pcm, 0, pcm.size)
            track.setVolume(volume.coerceIn(0f, 1f))
            track.play()
            val ms = pcm.size * 1000L / SAMPLE_RATE + 300
            main.postDelayed({ runCatching { track.stop(); track.release() } }, ms)
        }
    }
}

/** Tiny xorshift noise generator: deterministic per seed, cheap per sample. */
class Noise(seed: Int) {
    private var s = if (seed == 0) 0x2545F491 else seed

    fun next(): Float {
        s = s xor (s shl 13)
        s = s xor (s ushr 17)
        s = s xor (s shl 5)
        return (s and 0xFFFFFF) / 8388608f - 1f
    }
}

/** RBJ band-pass biquad (constant 0 dB peak gain). */
class BandPass(freq: Float, q: Float) {
    private val b0: Float
    private val b2: Float
    private val a1: Float
    private val a2: Float
    private var x1 = 0f
    private var x2 = 0f
    private var y1 = 0f
    private var y2 = 0f

    init {
        val w = TWO_PI * freq.coerceAtMost(SAMPLE_RATE * 0.45f) / SAMPLE_RATE
        val alpha = kotlin.math.sin(w) / (2 * q)
        val a0 = 1 + alpha
        b0 = alpha / a0
        b2 = -alpha / a0
        a1 = -2 * kotlin.math.cos(w) / a0
        a2 = (1 - alpha) / a0
    }

    fun process(x: Float): Float {
        val y = b0 * x + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1; x1 = x; y2 = y1; y1 = y
        return y
    }
}

class OnePoleLowPass(cutoff: Float) {
    private val a = kotlin.math.exp(-TWO_PI * cutoff / SAMPLE_RATE)
    private var z = 0f
    fun process(x: Float): Float {
        z = x * (1 - a) + z * a
        return z
    }
}
