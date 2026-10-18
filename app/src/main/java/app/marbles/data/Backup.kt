package app.marbles.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Everything in one zip: a JSON index of the children, the time together, interests, plans and
 * evenings, and every picture. Nothing leaves the phone unless the parent exports it.
 */
object Backup {
    private const val VERSION = 1
    private const val INDEX = "marbles.json"

    fun encode(all: Everything): String = JSONObject()
        .put("app", "marbles")
        .put("version", VERSION)
        .put("children", JSONArray(all.children.map {
            JSONObject().put("id", it.id).put("name", it.name).put("born", it.born).put("gender", it.gender).put("glass", it.glass).put("createdAt", it.createdAt)
        }))
        .put("together", JSONArray(all.together.map {
            JSONObject().put("id", it.id).put("kids", it.kids).put("start", it.start).put("minutes", it.minutes).put("timed", it.timed)
                .put("phoneMinutes", it.phoneMinutes).put("kind", it.kind).put("note", it.note).put("quote", it.quote).put("photo", it.photo)
                .put("idea", it.idea).put("question", it.question).put("alone", it.alone).put("first", it.first)
        }))
        .put("interests", JSONArray(all.interests.map {
            JSONObject().put("id", it.id).put("childId", it.childId).put("text", it.text).put("since", it.since).put("until", it.until)
        }))
        .put("plans", JSONArray(all.plans.map {
            JSONObject().put("id", it.id).put("kids", it.kids).put("day", it.day).put("text", it.text).put("idea", it.idea).put("alone", it.alone).put("done", it.done)
        }))
        .put("evenings", JSONArray(all.evenings.map {
            JSONObject().put("day", it.day).put("from", it.from).put("to", it.to).put("phoneMinutes", it.phoneMinutes).put("apps", it.apps)
        }))
        .toString(2)

    private fun JSONObject.list(key: String) = optJSONArray(key)?.let { a -> (0 until a.length()).map { a.getJSONObject(it) } }.orEmpty()

    /** Throws [IllegalArgumentException] when the text is not a Marbles index. */
    fun decode(text: String): Everything {
        val root = runCatching { JSONObject(text) }.getOrNull()
        require(root != null && root.optString("app") == "marbles") { "Not a Marbles backup" }
        return Everything(
            children = root.list("children").map {
                Child(id = it.getLong("id"), name = it.getString("name"), born = it.getLong("born"), gender = it.optString("gender"), glass = it.optInt("glass"), createdAt = it.optLong("createdAt"))
            },
            together = root.list("together").map {
                Together(
                    id = it.getLong("id"), kids = it.getString("kids"), start = it.getLong("start"), minutes = it.getInt("minutes"), timed = it.optBoolean("timed"),
                    phoneMinutes = it.optInt("phoneMinutes"), kind = it.optString("kind", Kind.PLAY.name), note = it.optString("note"), quote = it.optString("quote"),
                    photo = it.optString("photo"), idea = it.optString("idea"), question = it.optString("question"), alone = it.optBoolean("alone"), first = it.optBoolean("first"),
                )
            },
            interests = root.list("interests").map {
                Interest(id = it.getLong("id"), childId = it.getLong("childId"), text = it.getString("text"), since = it.getLong("since"), until = it.optLong("until"))
            },
            plans = root.list("plans").map {
                Plan(id = it.getLong("id"), kids = it.getString("kids"), day = it.getLong("day"), text = it.optString("text"), idea = it.optString("idea"), alone = it.optBoolean("alone"), done = it.optLong("done"))
            },
            evenings = root.list("evenings").map {
                Evening(day = it.getLong("day"), from = it.getInt("from"), to = it.getInt("to"), phoneMinutes = it.getInt("phoneMinutes"), apps = it.optString("apps"))
            },
        )
    }

    fun write(context: Context, uri: Uri, all: Everything, vault: Vault) {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            ZipOutputStream(os.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(INDEX))
                zip.write(encode(all).toByteArray())
                zip.closeEntry()
                all.together.map { it.photo }.filter { it.isNotEmpty() }.distinct().forEach { name ->
                    val file = vault.file(name)
                    if (!file.exists()) return@forEach
                    zip.putNextEntry(ZipEntry("media/$name"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        } ?: error("Can't write there")
    }

    /** Reads a backup into a fresh folder first, so a broken file never wipes what is here. */
    fun read(context: Context, uri: Uri): Pair<Everything, File> {
        val tmp = File(context.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        var index: String? = null
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val e = zip.nextEntry ?: break
                    when {
                        e.name == INDEX -> index = zip.readBytes().decodeToString()
                        e.name.startsWith("media/") && !e.isDirectory -> {
                            val name = e.name.removePrefix("media/")
                            require(!name.contains('/') && !name.contains("..")) { "Bad entry" }
                            File(tmp, name).outputStream().use { zip.copyTo(it) }
                        }
                    }
                }
            }
        } ?: error("Can't read that")
        return decode(index ?: throw IllegalArgumentException("Not a Marbles backup")) to tmp
    }
}
