package io.github.tgood91.bushidodapp

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

/**
 * The parts of Ollama's HTTP API (https://github.com/ollama/ollama/blob/main/docs/api.md) the
 * assistant uses, as plain Kotlin with no Android classes, so it is unit-tested on the JVM.
 *
 * The phone does not run the model itself: it talks to an Ollama server on the user's computer
 * or local network, started with `OLLAMA_HOST=0.0.0.0 ollama serve` so the phone can reach it.
 */
object OllamaApi {
    /** Ollama's own port, added when the user types just a host such as `192.168.1.20`. */
    const val DEFAULT_PORT = 11434

    /** The host computer as seen from the Android emulator. A real phone needs the computer's LAN address. */
    const val DEFAULT_BASE_URL = "http://10.0.2.2:$DEFAULT_PORT"

    const val DEFAULT_MODEL = "llama3.2"

    data class Message(val role: String, val content: String) {
        companion object {
            fun system(content: String) = Message("system", content)
            fun user(content: String) = Message("user", content)
            fun assistant(content: String) = Message("assistant", content)
        }
    }

    /**
     * Turns what the user typed into a server address, or null when it is not one.
     * `192.168.1.20` becomes `http://192.168.1.20:11434`. An address typed with its own scheme
     * (for example `https://ollama.example.com` behind a proxy) keeps that scheme's default port.
     * A trailing slash is dropped, so paths can be appended.
     */
    fun normalizeBaseUrl(input: String): String? {
        val trimmed = input.trim().trimEnd('/')
        if (trimmed.isEmpty()) return null
        val hadScheme = "://" in trimmed
        val uri = runCatching { URI(if (hadScheme) trimmed else "http://$trimmed") }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host ?: return null
        if (uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null) return null
        val port = when {
            uri.port != -1 -> ":${uri.port}"
            hadScheme -> ""
            else -> ":$DEFAULT_PORT"
        }
        return "$scheme://$host$port${uri.rawPath.orEmpty()}"
    }

    /** Body for `POST /api/chat`, asking for the whole reply at once rather than a stream. */
    fun chatRequest(model: String, messages: List<Message>): String {
        val array = JSONArray()
        messages.forEach { array.put(JSONObject().put("role", it.role).put("content", it.content)) }
        return JSONObject()
            .put("model", model)
            .put("messages", array)
            .put("stream", false)
            .toString()
    }

    /** The assistant's text from a `/api/chat` reply. Throws [IllegalArgumentException] if there is none. */
    fun parseChatReply(body: String): String {
        val json = runCatching { JSONObject(body) }.getOrNull()
            ?: throw IllegalArgumentException("The server did not answer with JSON")
        parseError(body)?.let { throw IllegalArgumentException(it) }
        val content = json.optJSONObject("message")?.optString("content", "")
        require(!content.isNullOrBlank()) { "The model sent an empty reply" }
        return content.trim()
    }

    /** Ollama reports failures as `{"error": "..."}`; returns that text, or null. */
    fun parseError(body: String): String? =
        runCatching { JSONObject(body).optString("error", "") }.getOrNull()?.takeIf { it.isNotBlank() }
}
