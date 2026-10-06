package com.animate.companion.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.animate.companion.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** GitHub answered 404: there are no releases, or the repository is private. */
class ReleasesUnavailable : IOException("releases not found")

data class ReleaseInfo(
    val version: String,
    val title: String,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long,
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: ReleaseInfo) : UpdateState
    data class Downloading(val release: ReleaseInfo, val progress: Float) : UpdateState
    data class ReadyToInstall(val release: ReleaseInfo, val file: File) : UpdateState
    data class Failed(val message: String, val release: ReleaseInfo? = null) : UpdateState
}

/**
 * Checks GitHub Releases for a newer APK, downloads it and hands it to the system installer.
 * Android verifies the signature on install, so only builds signed with the same key update.
 */
class UpdateManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val repo: String = BuildConfig.UPDATE_REPO,
    private val currentVersion: String = BuildConfig.VERSION_NAME,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state

    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)
    private var job: Job? = null

    val installedVersion: String get() = currentVersion

    /** Silent background check on app start, at most every [AUTO_CHECK_INTERVAL_MS]. */
    fun autoCheck(now: Long = System.currentTimeMillis()) {
        if (now - prefs.getLong(KEY_LAST_CHECK, 0) < AUTO_CHECK_INTERVAL_MS) return
        check(manual = false)
    }

    /** [manual] checks report "up to date" and errors; automatic ones stay quiet unless an update exists. */
    fun check(manual: Boolean = true) {
        if (job?.isActive == true) return
        job = scope.launch {
            if (manual) _state.value = UpdateState.Checking
            val result = runCatching { fetchLatest() }
            prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
            val release = result.getOrNull()
            _state.value = when {
                result.isFailure -> if (!manual) UpdateState.Idle else UpdateState.Failed(
                    if (result.exceptionOrNull() is ReleasesUnavailable) "Страница обновлений недоступна. Попроси взрослого проверить, что репозиторий открыт."
                    else "Не удалось проверить обновления. Есть интернет?",
                )
                release != null && isNewer(release.version, currentVersion) &&
                    (manual || prefs.getString(KEY_SKIPPED, null) != release.version) -> UpdateState.Available(release)
                manual -> UpdateState.UpToDate
                else -> UpdateState.Idle
            }
        }
    }

    /** "Later": hide this version from automatic checks (manual checks still offer it). */
    fun skip(release: ReleaseInfo) {
        prefs.edit().putString(KEY_SKIPPED, release.version).apply()
        dismiss()
    }

    fun dismiss() {
        if (_state.value !is UpdateState.Downloading) _state.value = UpdateState.Idle
    }

    fun download(release: ReleaseInfo) {
        if (job?.isActive == true && _state.value is UpdateState.Downloading) return
        job = scope.launch {
            _state.value = UpdateState.Downloading(release, 0f)
            _state.value = runCatching {
                val file = downloadApk(release) { p -> _state.value = UpdateState.Downloading(release, p) }
                UpdateState.ReadyToInstall(release, file)
            }.getOrElse { UpdateState.Failed("Загрузка прервалась. Попробуй ещё раз.", release) }
            (_state.value as? UpdateState.ReadyToInstall)?.let { install(it.file) }
        }
    }

    /** Opens the system installer, or first the "install unknown apps" permission screen. */
    fun install(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { _state.value = UpdateState.Failed("Не получилось открыть установку.") }
    }

    private suspend fun fetchLatest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("https://api.github.com/repos/$repo/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .build()
        http.newCall(req).execute().use { resp ->
            // 404 means "no releases" *or* a private repo; either way we cannot say the app is up to date.
            if (resp.code == 404) throw ReleasesUnavailable()
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            parseRelease(resp.body?.string().orEmpty())
        }
    }

    private suspend fun downloadApk(release: ReleaseInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // keep only the latest download
        val target = File(dir, "AniMate-${release.version}.apk")
        val part = File(dir, target.name + ".part")
        http.newCall(Request.Builder().url(release.apkUrl).build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            val body = resp.body ?: throw IOException("empty body")
            val total = body.contentLength().takeIf { it > 0 } ?: release.apkSize
            var read = 0L
            body.byteStream().use { input ->
                part.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        read += n
                        if (total > 0) onProgress((read.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
        }
        if (!part.renameTo(target)) throw IOException("rename failed")
        target
    }

    companion object {
        const val AUTO_CHECK_INTERVAL_MS = 12 * 60 * 60 * 1000L
        private const val KEY_LAST_CHECK = "last_check"
        private const val KEY_SKIPPED = "skipped_version"
        private val json = Json { ignoreUnknownKeys = true }

        /** Parses a GitHub "latest release" response; null when it has no APK asset. */
        fun parseRelease(raw: String): ReleaseInfo? {
            val obj = json.parseToJsonElement(raw).jsonObject
            val tag = obj["tag_name"]?.jsonPrimitive?.content ?: return null
            val apk = obj["assets"]?.jsonArray?.map { it.jsonObject }
                ?.firstOrNull { it["name"]?.jsonPrimitive?.content.orEmpty().endsWith(".apk") }
                ?: return null
            return ReleaseInfo(
                version = tag.removePrefix("v"),
                title = obj["name"]?.jsonPrimitive?.content ?: tag,
                notes = cleanNotes(obj["body"]?.jsonPrimitive?.content.orEmpty()),
                apkUrl = apk["browser_download_url"]!!.jsonPrimitive.content,
                apkSize = (apk["size"] as? JsonPrimitive)?.longOrNull ?: 0L,
            )
        }

        /** Strips markdown noise so release notes read nicely in a dialog. */
        fun cleanNotes(body: String): String = body.lines()
            .map { it.trim() }
            // Install instructions are for the GitHub page, not for an app that is already installed.
            .filterNot { it.startsWith("**Full Changelog**") || it.startsWith("<!--") || it.startsWith("Установка:") || it.startsWith("Для ответов персонажей") }
            .map {
                it.replace(Regex("""\s+by\s+@\S+(\s+in\s+https?://\S+)?"""), "")
                    .replace(Regex("""\*\*|`"""), "")
                    .replace(Regex("""^#+\s*"""), "")
                    .replace(Regex("""^[*-]\s+"""), "• ")
            }
            .joinToString("\n")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
            .take(1200)

        /** Semantic-version comparison: "1.10.0" > "1.9.2"; non-numeric parts count as 0. */
        fun isNewer(candidate: String, current: String): Boolean {
            fun parts(v: String) = v.removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
            val a = parts(candidate)
            val b = parts(current)
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x > y
            }
            return false
        }
    }
}

