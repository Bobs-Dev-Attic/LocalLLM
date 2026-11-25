package com.bobsdevattic.localllm.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists GitHub accounts and their access tokens in an Android Keystore-backed
 * [EncryptedSharedPreferences] file. Tokens are only ever read to make API calls
 * and never leave the device except to api.github.com over TLS.
 */
class GitHubAccountStore(context: Context) {

    private val prefs = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "github_accounts_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun loadAccounts(): List<GitHubAccount> {
        val raw = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                GitHubAccount(
                    id = o.getString("id"),
                    label = o.getString("label"),
                    login = o.getString("login"),
                    avatarUrl = o.optString("avatarUrl").ifBlank { null },
                    tokenType = o.optString("tokenType", "pat")
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveAccounts(accounts: List<GitHubAccount>) {
        val arr = JSONArray()
        accounts.forEach { a ->
            arr.put(
                JSONObject().apply {
                    put("id", a.id)
                    put("label", a.label)
                    put("login", a.login)
                    put("avatarUrl", a.avatarUrl ?: "")
                    put("tokenType", a.tokenType)
                }
            )
        }
        prefs.edit().putString(KEY_ACCOUNTS, arr.toString()).apply()
    }

    fun token(accountId: String): String? = prefs.getString(tokenKey(accountId), null)

    fun putToken(accountId: String, token: String) {
        prefs.edit().putString(tokenKey(accountId), token).apply()
    }

    fun removeToken(accountId: String) {
        prefs.edit().remove(tokenKey(accountId)).apply()
    }

    fun activeAccountId(): String? = prefs.getString(KEY_ACTIVE, null)

    fun setActiveAccountId(id: String?) {
        prefs.edit().apply { if (id == null) remove(KEY_ACTIVE) else putString(KEY_ACTIVE, id) }.apply()
    }

    private fun tokenKey(id: String) = "token_$id"

    companion object {
        private const val KEY_ACCOUNTS = "accounts"
        private const val KEY_ACTIVE = "active"
    }
}
