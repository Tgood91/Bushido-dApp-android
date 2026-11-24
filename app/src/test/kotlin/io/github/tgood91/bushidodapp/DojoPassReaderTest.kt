package io.github.tgood91.bushidodapp

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DojoPassReaderTest {
    private val wallet = "0x000000000000000000000000000000000000dEaD"

    @Test
    fun `encodes selector plus left-padded lowercase address`() {
        assertEquals(
            "0x912519c3" + "0".repeat(24) + "000000000000000000000000000000000000dead",
            DojoPassReader.encodeAddressCall(DojoPassReader.SEL_HAS_PASS, wallet),
        )
    }

    @Test
    fun `rejects malformed addresses`() {
        for (bad in listOf("", "0x", "dead", "0x1234", wallet + "00", "0x" + "g".repeat(40))) {
            assertThrows(IllegalArgumentException::class.java) {
                DojoPassReader.encodeAddressCall(DojoPassReader.SEL_HAS_PASS, bad)
            }
        }
    }

    @Test
    fun `decodes ABI bools`() {
        assertFalse(DojoPassReader.decodeBool("0x" + "0".repeat(64)))
        assertTrue(DojoPassReader.decodeBool("0x" + "0".repeat(63) + "1"))
    }

    @Test
    fun `empty or dirty results are errors, not true`() {
        for (bad in listOf("0x", "0x01", "0x" + "0".repeat(63) + "2", "0x1" + "0".repeat(63))) {
            assertThrows(IOException::class.java) { DojoPassReader.decodeBool(bad) }
        }
    }

    @Test
    fun `request body is a well-formed eth_call`() {
        val data = DojoPassReader.encodeAddressCall(DojoPassReader.SEL_PRACTITIONER, wallet)
        assertEquals(
            """{"jsonrpc":"2.0","id":1,"method":"eth_call","params":[""" +
                """{"to":"0x2647faf3aaA708D0B9a110744790E1f637FCca53","data":"$data"},"latest"]}""",
            DojoPassReader.requestBody(data),
        )
    }

    @Test
    fun `parses result and surfaces RPC errors`() {
        val word = "0x" + "0".repeat(63) + "1"
        assertEquals(word, DojoPassReader.parseResult("""{"jsonrpc":"2.0","id":1,"result":"$word"}"""))
        assertEquals("0x", DojoPassReader.parseResult("""{"jsonrpc":"2.0","id":1, "result" : "0x"}"""))

        val err = assertThrows(IOException::class.java) {
            DojoPassReader.parseResult(
                """{"jsonrpc":"2.0","id":1,"error":{"code":-32000,"message":"execution \"reverted\""}}""",
            )
        }
        assertEquals("RPC error: execution \\\"reverted\\\"", err.message)

        assertThrows(IOException::class.java) { DojoPassReader.parseResult("""{"jsonrpc":"2.0","id":1}""") }
    }
}
