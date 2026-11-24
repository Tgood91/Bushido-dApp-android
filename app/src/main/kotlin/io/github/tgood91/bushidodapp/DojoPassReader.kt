package io.github.tgood91.bushidodapp

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONObject

/** Reads Dojo Pass status straight from the DojoPass contract on Monad mainnet (chain id 143). */
object DojoPassReader {

    private const val RPC_URL = "https://rpc.monad.xyz"
    private const val DOJO_PASS = "0x2647faf3aaA708D0B9a110744790E1f637FCca53"

    // keccak256 selectors; both checked against the deployed bytecode on 2026-11-24.
    internal const val SEL_HAS_PASS = "0x912519c3" // hasDojoPass(address)
    internal const val SEL_PRACTITIONER = "0x89f5ffc7" // isPractitioner(address)

    private val ADDRESS = Regex("^0x[0-9a-fA-F]{40}$")
    private val WORD = Regex("^0x[0-9a-fA-F]{64}$")

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
        val body = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 1)
            .put("method", "eth_call")
            .put(
                "params",
                JSONArray()
                    .put(JSONObject().put("to", DOJO_PASS).put("data", encodeAddressCall(selector, wallet)))
                    .put("latest"),
            )
            .toString()

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

            val response = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            response.optJSONObject("error")?.let {
                throw IOException("RPC error ${it.optInt("code")}: ${it.optString("message")}")
            }
            return decodeBool(response.getString("result"))
        } finally {
            conn.disconnect()
        }
    }
}
