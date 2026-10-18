package com.milkys.soundbooster.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class GitHubUpdateChecker(
    private val repoUrl: String,
    private val currentVersion: String,
    private val fetcher: HttpFetcher = UrlConnectionHttpFetcher(),
) : UpdateChecker {

    override val channel: UpdateChannel = UpdateChannel.GITHUB

    override suspend fun check(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val repoPath = repoUrl
                .removePrefix("https://github.com/")
                .removePrefix("http://github.com/")
                .trim('/')
            require(repoPath.contains('/')) { "Invalid GitHub repository URL" }

            val json = fetcher.fetch(
                url = "https://api.github.com/repos/$repoPath/releases/latest",
                headers = mapOf(
                    "Accept" to "application/vnd.github+json",
                    "X-GitHub-Api-Version" to "2022-11-28",
                ),
            )
            val release = JSONObject(json)
            val tag = release.optString("tag_name").ifBlank { return@withContext UpdateResult.Error("GitHub release has no tag") }
            val latestVersion = tag.removePrefix("v")
            val releaseUrl = release.optString("html_url")

            if (VersionComparer.compare(latestVersion, currentVersion) <= 0) {
                return@withContext UpdateResult.UpToDate(latestVersion)
            }

            UpdateResult.Available(
                latestVersion = latestVersion,
                url = findApkAsset(release) ?: releaseUrl,
            )
        } catch (e: HttpFetchException) {
            if (e.statusCode == 404) {
                UpdateResult.NotFound(UpdateChannel.GITHUB)
            } else {
                UpdateResult.Error(e.message ?: "GitHub request failed")
            }
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "GitHub update check failed")
        }
    }

    private fun findApkAsset(release: JSONObject): String? {
        val assets = release.optJSONArray("assets") ?: return null
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            val name = asset.optString("name")
            val downloadUrl = asset.optString("browser_download_url")
            if (downloadUrl.isNotBlank() && name.endsWith(".apk", ignoreCase = true)) {
                return downloadUrl
            }
        }
        return null
    }
}
