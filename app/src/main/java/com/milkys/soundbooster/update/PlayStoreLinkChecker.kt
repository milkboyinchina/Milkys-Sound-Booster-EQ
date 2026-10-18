package com.milkys.soundbooster.update

class PlayStoreLinkChecker(
    private val packageName: String,
) : UpdateChecker {

    override val channel: UpdateChannel = UpdateChannel.PLAY_STORE

    override suspend fun check(): UpdateResult = UpdateResult.OpenStore(
        url = "market://details?id=$packageName",
        fallbackUrl = "https://play.google.com/store/apps/details?id=$packageName",
    )
}
