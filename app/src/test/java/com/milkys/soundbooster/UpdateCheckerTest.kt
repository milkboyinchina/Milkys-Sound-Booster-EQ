package com.milkys.soundbooster

import com.milkys.soundbooster.update.FdroidUpdateChecker
import com.milkys.soundbooster.update.GitHubUpdateChecker
import com.milkys.soundbooster.update.HttpFetchException
import com.milkys.soundbooster.update.HttpFetcher
import com.milkys.soundbooster.update.UpdateResult
import com.milkys.soundbooster.update.VersionComparer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UpdateCheckerTest {

    @Test
    fun `github reports a newer apk asset`() = runTest {
        val checker = GitHubUpdateChecker(
            repoUrl = "https://github.com/milkboyinchina/Milkys-Sound-Booster-EQ",
            currentVersion = "1.0.4",
            fetcher = HttpFetcher { _, _ ->
                """
                {
                  "tag_name": "v1.0.5",
                  "html_url": "https://github.com/milkboyinchina/Milkys-Sound-Booster-EQ/releases/tag/v1.0.5",
                  "assets": [
                    {"name": "milkys-release.apk", "browser_download_url": "https://example.com/milkys-release.apk"}
                  ]
                }
                """.trimIndent()
            },
        )

        val result = checker.check()

        assertTrue(result is UpdateResult.Available)
        result as UpdateResult.Available
        assertEquals("1.0.5", result.latestVersion)
        assertEquals("https://example.com/milkys-release.apk", result.url)
    }

    @Test
    fun `github falls back to the release page without an apk asset`() = runTest {
        val checker = GitHubUpdateChecker(
            repoUrl = "https://github.com/milkboyinchina/Milkys-Sound-Booster-EQ",
            currentVersion = "1.0.4",
            fetcher = HttpFetcher { _, _ ->
                """
                {
                  "tag_name": "v1.0.5",
                  "html_url": "https://github.com/milkboyinchina/Milkys-Sound-Booster-EQ/releases/tag/v1.0.5",
                  "assets": []
                }
                """.trimIndent()
            },
        )

        val result = checker.check()

        assertTrue(result is UpdateResult.Available)
        assertEquals(
            "https://github.com/milkboyinchina/Milkys-Sound-Booster-EQ/releases/tag/v1.0.5",
            (result as UpdateResult.Available).url,
        )
    }

    @Test
    fun `github reports up to date`() = runTest {
        val checker = GitHubUpdateChecker(
            repoUrl = "https://github.com/milkboyinchina/Milkys-Sound-Booster-EQ",
            currentVersion = "1.0.5",
            fetcher = HttpFetcher { _, _ ->
                """{"tag_name":"v1.0.5","html_url":"https://example.com","assets":[]}"""
            },
        )

        assertEquals(UpdateResult.UpToDate("1.0.5"), checker.check())
    }

    @Test
    fun `github reports not found on 404`() = runTest {
        val checker = GitHubUpdateChecker(
            repoUrl = "https://github.com/milkboyinchina/Milkys-Sound-Booster-EQ",
            currentVersion = "1.0.4",
            fetcher = HttpFetcher { _, _ -> throw HttpFetchException(404) },
        )

        assertEquals(UpdateResult.NotFound(com.milkys.soundbooster.update.UpdateChannel.GITHUB), checker.check())
    }

    @Test
    fun `github reports an error for malformed json`() = runTest {
        val checker = GitHubUpdateChecker(
            repoUrl = "https://github.com/milkboyinchina/Milkys-Sound-Booster-EQ",
            currentVersion = "1.0.4",
            fetcher = HttpFetcher { _, _ -> "not-json" },
        )

        assertTrue(checker.check() is UpdateResult.Error)
    }

    @Test
    fun `fdroid reports a newer version code`() = runTest {
        val checker = FdroidUpdateChecker(
            packageName = "com.milkys.soundbooster",
            currentVersionCode = 26101202,
            fetcher = HttpFetcher { _, _ ->
                """
                {
                  "packageName": "com.milkys.soundbooster",
                  "suggestedVersionCode": 26101203,
                  "packages": [
                    {"versionName": "1.0.5", "versionCode": 26101203}
                  ]
                }
                """.trimIndent()
            },
        )

        val result = checker.check()

        assertTrue(result is UpdateResult.Available)
        result as UpdateResult.Available
        assertEquals("1.0.5", result.latestVersion)
        assertEquals("https://f-droid.org/packages/com.milkys.soundbooster/", result.url)
    }

    @Test
    fun `fdroid reports up to date`() = runTest {
        val checker = FdroidUpdateChecker(
            packageName = "com.milkys.soundbooster",
            currentVersionCode = 26101202,
            fetcher = HttpFetcher { _, _ ->
                """{"suggestedVersionCode":26101202,"packages":[{"versionName":"1.0.4","versionCode":26101202}]}"""
            },
        )

        assertEquals(UpdateResult.UpToDate("1.0.4"), checker.check())
    }

    @Test
    fun `fdroid reports not found on 404`() = runTest {
        val checker = FdroidUpdateChecker(
            packageName = "com.milkys.soundbooster",
            currentVersionCode = 26101202,
            fetcher = HttpFetcher { _, _ -> throw HttpFetchException(404) },
        )

        assertEquals(UpdateResult.NotFound(com.milkys.soundbooster.update.UpdateChannel.FDROID), checker.check())
    }

    @Test
    fun `build targets map to the right update channel`() {
        assertEquals(com.milkys.soundbooster.update.UpdateChannel.PLAY_STORE, com.milkys.soundbooster.update.UpdateChannel.fromBuildConfig("playstore"))
        assertEquals(com.milkys.soundbooster.update.UpdateChannel.GITHUB, com.milkys.soundbooster.update.UpdateChannel.fromBuildConfig("github"))
        assertEquals(com.milkys.soundbooster.update.UpdateChannel.GITHUB, com.milkys.soundbooster.update.UpdateChannel.fromBuildConfig("github-noads"))
        assertEquals(com.milkys.soundbooster.update.UpdateChannel.FDROID, com.milkys.soundbooster.update.UpdateChannel.fromBuildConfig("fdroid"))
    }

    @Test
    fun `version comparer handles v prefix suffixes and prereleases`() {
        assertEquals(1, VersionComparer.compare("1.0.5", "1.0.4"))
        assertEquals(0, VersionComparer.compare("v1.0.5", "1.0.5"))
        assertEquals(0, VersionComparer.compare("1.0.5-github.42", "1.0.5"))
        assertEquals(1, VersionComparer.compare("1.0.5", "1.0.5-beta"))
        assertEquals(-1, VersionComparer.compare("1.0.4", "1.0.5"))
    }
}
