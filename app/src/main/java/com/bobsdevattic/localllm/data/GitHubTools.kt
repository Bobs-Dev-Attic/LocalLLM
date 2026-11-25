package com.bobsdevattic.localllm.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * OpenAI tool (function) definitions for the GitHub connector and their
 * execution against [GitHubClient] using a single account's token.
 */
object GitHubTools {

    private const val MAX_TOOL_RESULT_CHARS = 12000
    private const val MAX_TREE_PATHS = 400
    private const val MAX_SEARCH_RESULTS = 30

    /** The tools array sent to the model (OpenAI function-calling format). */
    fun specs(): JSONArray {
        val tools = JSONArray()

        tools.put(
            function(
                name = "github_list_repos",
                description = "List the GitHub repositories the active account can access (full names like owner/repo).",
                properties = JSONObject(),
                required = emptyList()
            )
        )

        tools.put(
            function(
                name = "github_get_file",
                description = "Read a UTF-8 text file from a GitHub repository.",
                properties = JSONObject()
                    .put("repo", strProp("Repository as owner/repo."))
                    .put("path", strProp("File path within the repository."))
                    .put("ref", strProp("Branch, tag, or commit SHA (optional).")),
                required = listOf("repo", "path")
            )
        )

        tools.put(
            function(
                name = "github_get_issue",
                description = "Read a GitHub issue or pull request by number.",
                properties = JSONObject()
                    .put("repo", strProp("Repository as owner/repo."))
                    .put("number", JSONObject().put("type", "integer").put("description", "Issue or PR number.")),
                required = listOf("repo", "number")
            )
        )

        tools.put(
            function(
                name = "github_list_tree",
                description = "List the file paths in a repository (recursive). Use this to discover the exact path of a file before reading it.",
                properties = JSONObject()
                    .put("repo", strProp("Repository as owner/repo."))
                    .put("ref", strProp("Branch, tag, or commit SHA (optional; defaults to the default branch).")),
                required = listOf("repo")
            )
        )

        tools.put(
            function(
                name = "github_search_code",
                description = "Search code within a repository and return matching file paths. Use this to find where a symbol or text appears.",
                properties = JSONObject()
                    .put("repo", strProp("Repository as owner/repo."))
                    .put("query", strProp("Search terms (GitHub code-search syntax).")),
                required = listOf("repo", "query")
            )
        )

        return tools
    }

    /** Executes one tool call; always returns a string (errors included) so the loop can continue. */
    suspend fun execute(
        client: GitHubClient,
        token: String,
        name: String,
        argumentsJson: String
    ): String {
        val args = runCatching { JSONObject(argumentsJson) }.getOrDefault(JSONObject())
        return try {
            when (name) {
                "github_list_repos" ->
                    client.listRepos(token).joinToString("\n").ifBlank { "(no accessible repositories)" }

                "github_get_file" -> {
                    val (owner, repo) = splitRepo(args.optString("repo"))
                        ?: return "Error: 'repo' must be owner/repo."
                    val path = args.optString("path")
                    if (path.isBlank()) return "Error: 'path' is required."
                    val ref = args.optString("ref").ifBlank { null }
                    client.getFile(token, owner, repo, path, ref).content.take(MAX_TOOL_RESULT_CHARS)
                }

                "github_get_issue" -> {
                    val (owner, repo) = splitRepo(args.optString("repo"))
                        ?: return "Error: 'repo' must be owner/repo."
                    val number = if (args.has("number")) args.optInt("number") else return "Error: 'number' is required."
                    val issue = client.getIssue(token, owner, repo, number)
                    "#${issue.number} (${issue.state}): ${issue.title}\n\n${issue.body}".take(MAX_TOOL_RESULT_CHARS)
                }

                "github_list_tree" -> {
                    val (owner, repo) = splitRepo(args.optString("repo"))
                        ?: return "Error: 'repo' must be owner/repo."
                    val ref = args.optString("ref").ifBlank { null }
                    val paths = client.listTree(token, owner, repo, ref)
                    if (paths.isEmpty()) {
                        "(no files)"
                    } else {
                        val shown = paths.take(MAX_TREE_PATHS)
                        val overflow = if (paths.size > shown.size) "\n…(${paths.size - shown.size} more paths)" else ""
                        shown.joinToString("\n") + overflow
                    }
                }

                "github_search_code" -> {
                    val (owner, repo) = splitRepo(args.optString("repo"))
                        ?: return "Error: 'repo' must be owner/repo."
                    val query = args.optString("query")
                    if (query.isBlank()) return "Error: 'query' is required."
                    val paths = client.searchCode(token, owner, repo, query)
                    if (paths.isEmpty()) "(no matches)" else paths.take(MAX_SEARCH_RESULTS).joinToString("\n")
                }

                else -> "Error: unknown tool '$name'."
            }
        } catch (e: GitHubException) {
            "Error: ${e.message}"
        } catch (e: Exception) {
            "Error: ${e.message ?: "GitHub request failed."}"
        }
    }

    private fun splitRepo(repoFullName: String): Pair<String, String>? {
        val parts = repoFullName.trim().removePrefix("https://github.com/").trim('/').split("/")
        if (parts.size < 2 || parts[0].isBlank() || parts[1].isBlank()) return null
        return parts[0] to parts[1]
    }

    private fun strProp(description: String): JSONObject =
        JSONObject().put("type", "string").put("description", description)

    private fun function(
        name: String,
        description: String,
        properties: JSONObject,
        required: List<String>
    ): JSONObject = JSONObject()
        .put("type", "function")
        .put(
            "function",
            JSONObject()
                .put("name", name)
                .put("description", description)
                .put(
                    "parameters",
                    JSONObject()
                        .put("type", "object")
                        .put("properties", properties)
                        .put("required", JSONArray(required))
                )
        )
}
