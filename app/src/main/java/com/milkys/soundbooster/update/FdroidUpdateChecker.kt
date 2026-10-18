package com.milkys.soundbooster.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class FdroidUpdateChecker(
    private val packageName: String,
    private val currentVersionCode: Int,
    private val fetcher: HttpFetcher = UrlConnectionHttpFetcher(),
) : UpdateChecker {

    override val channel: UpdateChannel = UpdateChannel.FDROID

    override suspend fun check(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            val json = fetcher.fetch(
                url = "https://f-droid.org/api/v1/packages/$packageName",
                headers = mapOf("Accept" to "application/json"),
            )
            val payload = JSONObject(json)
            val suggestedVersionCode = payload.optInt("suggestedVersionCode", -1)
            if (suggestedVersionCode <= 0) {
                return@withContext UpdateResult.NotFound(UpdateChannel.FDROID)
            }

            val latestVersionName = findVersionName(payload, suggestedVersionCode)
            if (suggestedVersionCode > currentVersionCode) {
                UpdateResult.Available(
                    latestVersion = latestVersionName ?: suggestedVersionCode.toString(),
                    url = "https://f-droid.org/packages/$packageName/",
                )
            } else {
                UpdateResult.UpToDate(latestVersionName)
            }
        } catch (e: HttpFetchException) {
            if (e.statusCode == 404) {
                UpdateResult.NotFound(UpdateChannel.FDROID)
            } else {
                UpdateResult.Error(e.message ?: "F-Droid request failed")
            }
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "F-Droid update check failed")
        }
    }

    private fun findVersionName(payload: JSONObject, versionCode: Int): String? {
        val packages = payload.optJSONArray("packages") ?: return null
        for (index in 0 until packages.length()) {
            val entry = packages.optJSONObject(index) ?: continue
            if (entry.optInt("versionCode", -1) == versionCode) {
                return entry.optString("versionName").ifBlank { null }
            }
        }
        return null
    }
}
