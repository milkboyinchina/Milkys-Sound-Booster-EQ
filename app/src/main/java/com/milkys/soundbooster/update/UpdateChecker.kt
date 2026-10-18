package com.milkys.soundbooster.update

import android.content.Context
import com.milkys.soundbooster.BuildConfig

/**
 * Distribution channel baked into the APK. Each channel is checked against the
 * store/repository that actually distributes that build.
 */
enum class UpdateChannel {
    PLAY_STORE,
    GITHUB,
    FDROID;

    companion object {
        fun fromBuildConfig(value: String): UpdateChannel = when (value.lowercase()) {
            "github", "github-noads" -> GITHUB
            "fdroid" -> FDROID
            else -> PLAY_STORE
        }
    }
}

sealed interface UpdateResult {
    data class UpToDate(val latestVersion: String? = null) : UpdateResult

    data class Available(
        val latestVersion: String,
        val url: String,
    ) : UpdateResult

    /**
     * The Play Store owns update delivery. We intentionally open the listing
     * instead of comparing versions ourselves.
     */
    data class OpenStore(
        val url: String,
        val fallbackUrl: String,
    ) : UpdateResult

    data class NotFound(val channel: UpdateChannel) : UpdateResult

    data class Error(val message: String) : UpdateResult
}

interface UpdateChecker {
    val channel: UpdateChannel
    suspend fun check(): UpdateResult
}

object UpdateCheckerFactory {
    fun create(context: Context): UpdateChecker {
        val channel = UpdateChannel.fromBuildConfig(BuildConfig.UPDATE_CHANNEL)
        return when (channel) {
            UpdateChannel.PLAY_STORE -> PlayStoreLinkChecker(context.packageName)
            UpdateChannel.GITHUB -> GitHubUpdateChecker(
                repoUrl = BuildConfig.GITHUB_REPO_URL,
                currentVersion = BuildConfig.VERSION_NAME,
            )
            UpdateChannel.FDROID -> FdroidUpdateChecker(
                packageName = context.packageName,
                currentVersionCode = BuildConfig.VERSION_CODE.toIntOrNull() ?: 0,
            )
        }
    }
}

/**
 * Numeric semantic-version comparison. Handles tags such as `v1.2.3`,
 * `1.2.3-beta.1`, and build suffixes such as `1.2.3-github.42`.
 */
object VersionComparer {
    fun compare(left: String, right: String): Int {
        val (leftBase, leftPre) = splitVersion(left)
        val (rightBase, rightPre) = splitVersion(right)

        val leftParts = leftBase.split('.').map { it.toIntOrNull() ?: 0 }
        val rightParts = rightBase.split('.').map { it.toIntOrNull() ?: 0 }
        val maxSize = maxOf(leftParts.size, rightParts.size)

        for (index in 0 until maxSize) {
            val leftValue = leftParts.getOrElse(index) { 0 }
            val rightValue = rightParts.getOrElse(index) { 0 }
            if (leftValue != rightValue) return leftValue.compareTo(rightValue)
        }

        // A stable release sorts above a pre-release with the same numbers.
        return when {
            leftPre == null && rightPre == null -> 0
            leftPre == null -> 1
            rightPre == null -> -1
            else -> leftPre.compareTo(rightPre)
        }
    }

    private fun splitVersion(raw: String): Pair<String, String?> {
        val cleaned = raw.trim()
            .removePrefix("v")
            .substringBefore('+')
            // Channel/build suffixes are metadata, not prerelease identifiers.
            .substringBefore("-github")
            .substringBefore("-noads")
        val base = cleaned.substringBefore('-')
        val pre = cleaned.substringAfter('-', "").ifBlank { null }
        return base to pre
    }
}
