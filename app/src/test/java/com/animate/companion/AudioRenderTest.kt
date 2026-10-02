package com.animate.companion

import com.animate.companion.audio.LofiComposer
import com.animate.companion.audio.LofiTracks
import com.animate.companion.audio.SAMPLE_RATE
import com.animate.companion.audio.Sfx
import com.animate.companion.audio.VoiceSynth
import com.animate.companion.model.Gender
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.sqrt

class AudioRenderTest {
    private fun rms(pcm: ShortArray) = sqrt(pcm.sumOf { (it.toDouble() / Short.MAX_VALUE).let { v -> v * v } } / pcm.size)

    private fun writeWav(name: String, pcm: ShortArray, channels: Int) {
        val dir = File(System.getProperty("java.io.tmpdir"), "animate-audio").apply { mkdirs() }
        RandomAccessFile(File(dir, "$name.wav"), "rw").use { f ->
            f.setLength(0)
            fun i32(v: Int) = f.write(byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte()))
            fun i16(v: Int) = f.write(byteArrayOf(v.toByte(), (v shr 8).toByte()))
            f.writeBytes("RIFF"); i32(36 + pcm.size * 2); f.writeBytes("WAVEfmt "); i32(16); i16(1); i16(channels)
            i32(SAMPLE_RATE); i32(SAMPLE_RATE * channels * 2); i16(channels * 2); i16(16); f.writeBytes("data"); i32(pcm.size * 2)
            val buf = ByteArray(pcm.size * 2)
            pcm.forEachIndexed { i, s -> buf[2 * i] = s.toByte(); buf[2 * i + 1] = (s.toInt() shr 8).toByte() }
            f.write(buf)
        }
    }

    @Test
    fun lofiTracksRenderQuicklyAndAreAudible() {
        LofiTracks.all.forEachIndexed { i, t ->
            val start = System.nanoTime()
            val pcm = LofiComposer.render(t)
            val ms = (System.nanoTime() - start) / 1_000_000
            val seconds = pcm.size / 2f / SAMPLE_RATE
            println("lofi ${t.title}: ${"%.1f".format(seconds)}s rendered in ${ms}ms, rms=${"%.3f".format(rms(pcm))}")
            writeWav("lofi$i", pcm, 2)
            assertTrue(seconds in 20f..40f)
            assertTrue(rms(pcm) in 0.05..0.5)
        }
    }

    @Test
    fun allInterjectionsRender() {
        val all = listOf(
            VoiceSynth.NYA, VoiceSynth.EHEHE, VoiceSynth.KYAA, VoiceSynth.UUU, VoiceSynth.HMPH, VoiceSynth.EH,
            VoiceSynth.HAWAWA, VoiceSynth.UFUFU, VoiceSynth.MMM, VoiceSynth.YATTA, VoiceSynth.BAKA, VoiceSynth.ANO,
            VoiceSynth.AHAHA, VoiceSynth.ARAARA, VoiceSynth.SUGOI, VoiceSynth.FUWAA, VoiceSynth.KYUN, VoiceSynth.UWAA,
            VoiceSynth.YOSH, VoiceSynth.HEHE_M, VoiceSynth.OI, VoiceSynth.NANI, VoiceSynth.OOH, VoiceSynth.HAHA, VoiceSynth.UGH,
        )
        for (g in Gender.entries) {
            val voice = VoiceSynth.voiceFor(g, 1f, 42)
            for (ij in all) {
                val pcm = VoiceSynth.render(ij, voice)
                val dur = pcm.size.toFloat() / SAMPLE_RATE
                assertTrue("${ij.id} too long: $dur", dur in 0.2f..1.2f)
                assertTrue("${ij.id} silent", rms(pcm) > 0.05)
                if (g == Gender.FEMALE || ij.id in setOf("yosh", "nani")) writeWav("voice_${g.name.lowercase()}_${ij.id}", pcm, 1)
            }
        }
        listOf(Sfx.send, Sfx.receive, Sfx.tap, Sfx.sparkle, Sfx.dice).forEach { assertTrue(it.isNotEmpty() && rms(it) > 0.01) }
    }
}
