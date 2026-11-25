package com.bobsdevattic.localllm.data

/**
 * A linked GitHub identity. The access token is NOT held here — it lives in the
 * encrypted [GitHubAccountStore], keyed by [id], so account metadata can be
 * passed around freely without exposing secrets.
 */
data class GitHubAccount(
    val id: String,
    val label: String,
    val login: String,
    val avatarUrl: String? = null,
    val tokenType: String = "pat"
)
