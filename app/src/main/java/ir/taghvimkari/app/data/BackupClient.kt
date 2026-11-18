package ir.taghvimkari.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class BackupException(message: String) : Exception(message)

/** Stores the backup as a private GitHub Gist of the user (needs a token with the "gist" permission). */
object BackupClient {
    private const val FILE = "taghvim-kari-backup.json"
    private const val API = "https://api.github.com/gists"
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Creates the gist on first use, otherwise updates it. Returns the gist id. */
    suspend fun upload(token: String, gistId: String?, content: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val file = JSONObject().put("content", content)
            val body = JSONObject().put("files", JSONObject().put(FILE, file))
            val creating = gistId.isNullOrBlank()
            if (creating) body.put("description", "Taghvim Kari backup").put("public", false)
            val builder = request(token, if (creating) API else "$API/$gistId")
            val req = (if (creating) builder.post(body.toString().toRequestBody(JSON)) else builder.patch(body.toString().toRequestBody(JSON))).build()
            execute(req) { JSONObject(it).getString("id") }
        }
    }

    /** Id of an existing backup gist of this user (e.g. after reinstalling the app), or null. */
    suspend fun findExisting(token: String): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            execute(request(token, "$API?per_page=100").get().build()) { text ->
                val array = JSONArray(text)
                var found: String? = null
                for (i in 0 until array.length()) {
                    val gist = array.getJSONObject(i)
                    if (gist.optJSONObject("files")?.has(FILE) == true) {
                        found = gist.getString("id")
                        break
                    }
                }
                found
            }
        }
    }

    /** Returns the backup JSON text. */
    suspend fun download(token: String, gistId: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val meta = execute(request(token, "$API/$gistId").get().build()) { JSONObject(it) }
            val file = meta.getJSONObject("files").optJSONObject(FILE)
                ?: throw BackupException("فایل پشتیبان در این شناسه پیدا نشد.")
            if (file.optBoolean("truncated", false)) {
                execute(Request.Builder().url(file.getString("raw_url")).header("Authorization", "Bearer $token").get().build()) { it }
            } else {
                file.getString("content")
            }
        }
    }

    private fun request(token: String, url: String) = Request.Builder()
        .url(url)
        .header("Authorization", "Bearer $token")
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")

    private fun <T> execute(req: Request, parse: (String) -> T): T {
        try {
            client.newCall(req).execute().use { resp ->
                when {
                    resp.code == 401 -> throw BackupException("توکن نامعتبر است.")
                    resp.code == 403 -> throw BackupException("توکن اجازهٔ gist ندارد یا محدودیت درخواست پر شده است.")
                    resp.code == 404 -> throw BackupException("شناسهٔ پشتیبان پیدا نشد (یا توکن به آن دسترسی ندارد).")
                    !resp.isSuccessful -> throw BackupException("خطای سرور (${resp.code}).")
                }
                return parse(resp.body?.string() ?: throw BackupException("پاسخ خالی از سرور."))
            }
        } catch (e: IOException) {
            throw BackupException("اتصال اینترنت برقرار نیست.")
        }
    }
}
