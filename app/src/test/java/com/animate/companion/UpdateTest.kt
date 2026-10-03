package com.animate.companion

import com.animate.companion.update.UpdateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateTest {
    @Test
    fun comparesSemanticVersions() {
        assertTrue(UpdateManager.isNewer("1.3.0", "1.2.0"))
        assertTrue(UpdateManager.isNewer("v1.10.0", "1.9.9"))
        assertTrue(UpdateManager.isNewer("2.0", "1.99.99"))
        assertTrue(UpdateManager.isNewer("1.2.1", "1.2"))
        assertFalse(UpdateManager.isNewer("1.2.0", "1.2.0"))
        assertFalse(UpdateManager.isNewer("1.1.9", "1.2.0"))
        assertFalse(UpdateManager.isNewer("1.2.0-beta", "1.2.0"))
    }

    @Test
    fun parsesRealGithubRelease() {
        val raw = javaClass.classLoader!!.getResource("github_latest_release.json")!!.readText()
        val r = UpdateManager.parseRelease(raw)
        assertNotNull(r)
        assertFalse(r!!.version.startsWith("v"))
        assertTrue(r.apkUrl.startsWith("https://github.com/vdoma88/reimagined-train/releases/download/"))
        assertTrue(r.apkUrl.endsWith(".apk"))
        assertTrue(r.apkSize > 500_000)
    }

    @Test
    fun releaseWithoutApkIsIgnored() {
        assertNull(UpdateManager.parseRelease("""{"tag_name":"v9.9.9","assets":[{"name":"notes.txt","browser_download_url":"x"}]}"""))
    }

    @Test
    fun cleansMarkdownNotes() {
        val notes = UpdateManager.cleanNotes("## What's Changed\n* **New** look by @me in https://github.com/x/y/pull/1\n\n\n\n**Full Changelog**: https://x")
        assertEquals("What's Changed\n• New look", notes)
    }

    @Test
    fun dropsInstallInstructionsAndPrLinks() {
        val raw = javaClass.classLoader!!.getResource("github_latest_release.json")!!.readText()
        val notes = UpdateManager.parseRelease(raw)!!.notes
        assertFalse(notes, notes.contains("Установка:"))
        assertFalse(notes, notes.contains("https://github.com"))
        assertTrue(notes, notes.contains("• "))
    }
}
