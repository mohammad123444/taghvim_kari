package ir.taghvimkari.app.data

import ir.taghvimkari.app.domain.Version
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val version: String, val pageUrl: String, val apkUrl: String?, val notes: String)

sealed interface UpdateResult {
    data class Available(val info: UpdateInfo) : UpdateResult
    data object UpToDate : UpdateResult
    data object Failed : UpdateResult
}

/** Looks at the latest GitHub Release of [repo] ("owner/name"). Works for public repositories. */
object UpdateChecker {
    suspend fun fetchLatest(repo: String, currentVersion: String): UpdateResult = withContext(Dispatchers.IO) {
        runCatching<UpdateResult> {
            val conn = (URL("https://api.github.com/repos/$repo/releases/latest").openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            try {
                when (conn.responseCode) {
                    200 -> Unit
                    404 -> return@runCatching UpdateResult.UpToDate // no release published yet
                    else -> return@runCatching UpdateResult.Failed
                }
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val tag = json.getString("tag_name")
                if (!Version.isNewer(tag, currentVersion)) return@runCatching UpdateResult.UpToDate

                var apk: String? = null
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val a = assets.getJSONObject(i)
                        if (a.optString("name").endsWith(".apk")) {
                            apk = a.optString("browser_download_url")
                            break
                        }
                    }
                }
                UpdateResult.Available(
                    UpdateInfo(
                        version = tag.removePrefix("v"),
                        pageUrl = json.optString("html_url"),
                        apkUrl = apk,
                        notes = json.optString("body")
                    )
                )
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(UpdateResult.Failed)
    }
}
