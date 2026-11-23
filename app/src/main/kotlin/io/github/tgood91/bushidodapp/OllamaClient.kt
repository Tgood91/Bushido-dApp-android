package io.github.tgood91.bushidodapp

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Blocking calls to one Ollama server. Run them off the main thread. */
class OllamaClient(private val baseUrl: String) {

    class OllamaException(message: String) : IOException(message)

    /** Sends the conversation to `/api/chat` and returns the model's reply. */
    fun chat(model: String, messages: List<OllamaApi.Message>): String {
        val conn = URL("$baseUrl/api/chat").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            // A local model can take a while, especially the first reply while it loads into memory.
            conn.readTimeout = READ_TIMEOUT_MS
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.outputStream.use { it.write(OllamaApi.chatRequest(model, messages).toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw OllamaException(OllamaApi.parseError(body) ?: "HTTP $code")
            return try {
                OllamaApi.parseChatReply(body)
            } catch (e: IllegalArgumentException) {
                throw OllamaException(e.message ?: "Unreadable reply")
            }
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 180_000
    }
}
