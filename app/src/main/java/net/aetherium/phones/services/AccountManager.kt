package net.aetherium.phones.services

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object AccountManager {
    private fun secure(ctx: Context, name: String): EncryptedSharedPreferences {
        val mk = MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
            ctx,
            name,
            mk,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }
    fun getOwnAccountId(ctx: Context): String? {
        val possible = listOf("aetherium_services_account", "aetherium_services", "aetherium_account", "aetherium_auth")
        for (p in possible) {
            try {
                val pref = secure(ctx, p)
                val keys = listOf("account_id", "accountId", "id", "user_id", "uid")
                for (k in keys) {
                    val v = pref.getString(k, null)
                    if (!v.isNullOrBlank()) return v
                }
            } catch (e: Exception) { /* ignore */ }
        }
        return null
    }
}
