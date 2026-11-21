package ir.taghvimkari.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import ir.taghvimkari.app.domain.Person
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/**
 * Sending and receiving all data as one JSON file:
 * {"app":"taghvim-kari","format":1,"exportedAt":"2026-11-20","people":[ ... ]}
 */
object DataExchange {
    private const val APP = "taghvim-kari"
    private const val FORMAT = 1
    private const val MAX_BYTES = 5 * 1024 * 1024
    const val MIME = "application/json"

    fun encode(people: List<Person>): String = JSONObject()
        .put("app", APP)
        .put("format", FORMAT)
        .put("exportedAt", LocalDate.now().toString())
        .put("people", LocalStore.encode(people))
        .toString(2)

    /** Parses a received file. Throws [IllegalArgumentException] with a Persian message if it is not valid. */
    fun decode(text: String): List<Person> {
        val trimmed = text.trim().removePrefix("\uFEFF")
        val array: JSONArray = try {
            if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                val obj = JSONObject(trimmed)
                require(obj.optString("app") == APP) { "این فایل مربوط به این برنامه نیست." }
                obj.getJSONArray("people")
            }
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("فایل معتبر نیست.")
        }
        val people = LocalStore.decode(array)
        require(people.isNotEmpty()) { "در این فایل اطلاعاتی وجود ندارد." }
        return people
    }

    fun readFile(context: Context, uri: Uri): String {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("فایل باز نشد.")
        stream.use {
            val bytes = it.readBytes()
            require(bytes.size <= MAX_BYTES) { "فایل بیش از حد بزرگ است." }
            return bytes.toString(Charsets.UTF_8)
        }
    }

    /** Writes the JSON file and opens Android's share sheet (Telegram, WhatsApp, e-mail, Bluetooth ...). */
    fun share(context: Context, people: List<Person>): Boolean = runCatching {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "taghvim-kari-${LocalDate.now()}.json")
        file.writeText(encode(people), Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType(MIME)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, "ارسال اطلاعات").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
}
