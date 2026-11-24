package io.github.tgood91.bushidodapp

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Reads Dojo Pass status straight from the DojoPass contract on Monad mainnet (chain id 143).
 * Plain Kotlin/JVM: no Android types, so it runs and is tested on any JVM.
 */
object DojoPassReader {

    private const val RPC_URL = "https://rpc.monad.xyz"
    private const val DOJO_PASS = "0x2647faf3aaA708D0B9a110744790E1f637FCca53"

    // keccak256 selectors; both checked against the deployed bytecode on 2026-11-24.
    internal const val SEL_HAS_PASS = "0x912519c3" // hasDojoPass(address)
    internal const val SEL_PRACTITIONER = "0x89f5ffc7" // isPractitioner(address)

    private val ADDRESS = Regex("^0x[0-9a-fA-F]{40}$")
    private val WORD = Regex("^0x[0-9a-fA-F]{64}$")
    private val RESULT = Regex("\"result\"\\s*:\\s*\"(0x[0-9a-fA-F]*)\"")
    private val ERROR = Regex("\"error\"\\s*:\\s*\\{")
    private val MESSAGE = Regex("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")

    data class PassStatus(val hasPass: Boolean, val isPractitioner: Boolean)

    private val executor = Executors.newSingleThreadExecutor()

    /** Reads pass status for [wallet]. The callback runs on a background thread. */
    fun fetchStatus(wallet: String, callback: (Result<PassStatus>) -> Unit) {
        executor.execute {
            val result = runCatching {
                PassStatus(
                    hasPass = ethCallBool(SEL_HAS_PASS, wallet),
                    isPractitioner = ethCallBool(SEL_PRACTITIONER, wallet),
                )
            }
            callback(result)
        }
    }

    /** ABI-encodes `selector(address)`. Rejects anything that is not a 20-byte hex address. */
    internal fun encodeAddressCall(selector: String, wallet: String): String {
        require(ADDRESS.matches(wallet)) { "Not a wallet address: $wallet" }
        return selector + "0".repeat(24) + wallet.substring(2).lowercase()
    }

    /** The JSON-RPC `eth_call` body. Every value in it is a fixed constant or validated hex. */
    internal fun requestBody(data: String): String =
        """{"jsonrpc":"2.0","id":1,"method":"eth_call","params":[{"to":"$DOJO_PASS","data":"$data"},"latest"]}"""

    /** Pulls the hex `result` out of a JSON-RPC response, or throws with the node's error message. */
    internal fun parseResult(response: String): String {
        if (ERROR.containsMatchIn(response)) {
            val message = MESSAGE.find(response)?.groupValues?.get(1) ?: "unknown error"
            throw IOException("RPC error: $message")
        }
        return RESULT.find(response)?.groupValues?.get(1)
            ?: throw IOException("RPC response has no result")
    }

    /**
     * Decodes an ABI `bool` return word. Anything else ("0x" from a revert or a missing contract,
     * short or dirty words) is an error, never a silent `true`.
     */
    internal fun decodeBool(result: String): Boolean {
        if (!WORD.matches(result)) throw IOException("Unexpected eth_call result: $result")
        val word = result.substring(2).lowercase()
        if (word.substring(0, 63).any { it != '0' }) throw IOException("Not an ABI bool: $result")
        return when (word[63]) {
            '0' -> false
            '1' -> true
            else -> throw IOException("Not an ABI bool: $result")
        }
    }

    private fun ethCallBool(selector: String, wallet: String): Boolean {
        val body = requestBody(encodeAddressCall(selector, wallet))
        val conn = URL(RPC_URL).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.outputStream.use { it.write(body.toByteArray()) }

            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("RPC HTTP $code")

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            return decodeBool(parseResult(response))
        } finally {
            conn.disconnect()
        }
    }
}
