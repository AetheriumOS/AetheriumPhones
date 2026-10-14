package net.aetherium.phones.pairing

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import kotlin.math.abs

class PairingManager(private val ctx: Context) {
    private val masterKey = MasterKey.Builder(ctx)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    private val prefs = EncryptedSharedPreferences.create(
        ctx,
        "aetherium_pairing",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    private val chars6 = "0123456789"
    private val chars60 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    fun generateCode6(): String {
        val rnd = SecureRandom()
        val v = abs(rnd.nextLong()) % 1_000_000L
        return String.format("%06d", v)
    }
    fun generateCode60(): String {
        val rnd = SecureRandom()
        val sb = StringBuilder(60)
        repeat(60) { sb.append(chars60[rnd.nextInt(chars60.length)]) }
        return sb.toString()
    }
    fun isPaired(number: String): Boolean = prefs.contains("pk:$number")
    fun storePeer(number: String, pubKeyHex: String) {
        prefs.edit().putString("pk:$number", pubKeyHex).apply()
    }
    fun getPeer(number: String): String? = prefs.getString("pk:$number", null)
    fun storePhysicalPairCode(number: String, code60: String) {
        prefs.edit().putString("pp:$number", code60).apply()
    }
    fun getPhysicalPairCode(number: String): String? = prefs.getString("pp:$number", null)
    fun hasPhysicalPair(number: String): Boolean = prefs.contains("pp:$number")
}
