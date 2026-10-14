package net.aetherium.phones.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object Api {
    private val exec = Executors.newSingleThreadExecutor()
    fun isInternetAvailable(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    data class CodeSetupResult(val ok: Boolean, val code: String?, val expiresInMs: Long, val retrievedCount: Int, val error: String?)
    fun phoneCodeSetup(ownId: String, peerId: String, cb: (CodeSetupResult) -> Unit) {
        exec.execute {
            try {
                val url = "https://api.aetheriumos.com/phone/codesetup?ownid=" + java.net.URLEncoder.encode(ownId, "UTF-8") + "&accountid=" + java.net.URLEncoder.encode(peerId, "UTF-8")
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                val code = conn.responseCode
                val isErr = code >= 400
                val br = BufferedReader(InputStreamReader(if (isErr) conn.errorStream else conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (br.readLine().also { line = it } != null) sb.append(line)
                br.close()
                conn.disconnect()
                if (isErr) {
                    cb(CodeSetupResult(false, null, 0, 0, sb.toString()))
                    return@execute
                }
                val body = sb.toString()
                fun extract(regex: String): String? = Regex(regex).find(body)?.groupValues?.getOrNull(1)
                val ok = extract("\\\"ok\\\":(true|false)")?.toBoolean() ?: false
                val codeStr = extract("\\\"code\\\":\\\"([^\\\"]+)\\\"")
                val exp = extract("\\\"expires_in_ms\\\":(\\d+)")?.toLongOrNull() ?: 0L
                val rc = extract("\\\"retrieved_count\\\":(\\d+)")?.toIntOrNull() ?: 0
                cb(CodeSetupResult(ok, codeStr, exp, rc, null))
            } catch (e: Exception) {
                cb(CodeSetupResult(false, null, 0, 0, e.message))
            }
        }
    }
}
