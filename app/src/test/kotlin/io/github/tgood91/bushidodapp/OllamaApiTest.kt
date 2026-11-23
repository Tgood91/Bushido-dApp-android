package io.github.tgood91.bushidodapp

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class OllamaApiTest {

    @Test
    fun bareHostGetsHttpAndOllamaPort() {
        assertEquals("http://192.168.1.20:11434", OllamaApi.normalizeBaseUrl("192.168.1.20"))
        assertEquals("http://my-pc.local:11434", OllamaApi.normalizeBaseUrl("  my-pc.local/ "))
    }

    @Test
    fun explicitPortIsKept() {
        assertEquals("http://10.0.2.2:8080", OllamaApi.normalizeBaseUrl("10.0.2.2:8080"))
        assertEquals("http://10.0.2.2:11434", OllamaApi.normalizeBaseUrl("http://10.0.2.2:11434/"))
    }

    @Test
    fun explicitSchemeKeepsItsDefaultPort() {
        assertEquals("https://ollama.example.com", OllamaApi.normalizeBaseUrl("https://ollama.example.com"))
        assertEquals("https://example.com/ollama", OllamaApi.normalizeBaseUrl("https://example.com/ollama/"))
    }

    @Test
    fun rejectsNonsense() {
        assertNull(OllamaApi.normalizeBaseUrl(""))
        assertNull(OllamaApi.normalizeBaseUrl("   "))
        assertNull(OllamaApi.normalizeBaseUrl("ftp://host"))
        assertNull(OllamaApi.normalizeBaseUrl("javascript:alert(1)"))
        assertNull(OllamaApi.normalizeBaseUrl("http://user:pw@host"))
        assertNull(OllamaApi.normalizeBaseUrl("http://host/?x=1"))
        assertNull(OllamaApi.normalizeBaseUrl("not a url"))
    }

    @Test
    fun chatRequestHasModelMessagesAndNoStreaming() {
        val body = JSONObject(
            OllamaApi.chatRequest(
                "llama3.2",
                listOf(OllamaApi.Message.system("be brief"), OllamaApi.Message.user("say \"hi\"")),
            ),
        )
        assertEquals("llama3.2", body.getString("model"))
        assertFalse(body.getBoolean("stream"))
        val messages = body.getJSONArray("messages")
        assertEquals(2, messages.length())
        assertEquals("system", messages.getJSONObject(0).getString("role"))
        assertEquals("say \"hi\"", messages.getJSONObject(1).getString("content"))
    }

    @Test
    fun parsesReply() {
        val body = """{"model":"llama3.2","message":{"role":"assistant","content":" Hello! \n"},"done":true}"""
        assertEquals("Hello!", OllamaApi.parseChatReply(body))
    }

    @Test
    fun replyErrorsAreReported() {
        val notFound = assertThrows(IllegalArgumentException::class.java) {
            OllamaApi.parseChatReply("""{"error":"model 'x' not found"}""")
        }
        assertEquals("model 'x' not found", notFound.message)
        assertThrows(IllegalArgumentException::class.java) { OllamaApi.parseChatReply("<html>") }
        assertThrows(IllegalArgumentException::class.java) {
            OllamaApi.parseChatReply("""{"message":{"role":"assistant","content":""}}""")
        }
    }

    @Test
    fun parseErrorOnlyReturnsRealErrors() {
        assertEquals("boom", OllamaApi.parseError("""{"error":"boom"}"""))
        assertNull(OllamaApi.parseError("""{"message":{}}"""))
        assertNull(OllamaApi.parseError("not json"))
    }
}
