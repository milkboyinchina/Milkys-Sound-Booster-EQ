package com.milkys.soundbooster.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.milkys.soundbooster.R
import com.milkys.soundbooster.update.UpdateChannel
import com.milkys.soundbooster.update.UpdateResult
import com.milkys.soundbooster.update.UpdateUiState

@Composable
fun UpdateCard(
    state: UpdateUiState,
    cardColor: Color,
    borderDivider: Color,
    textPrimary: Color,
    textSecondary: Color,
    primaryAccent: Color,
    onCheckForUpdates: () -> Unit,
    onOpenUrl: (url: String, fallbackUrl: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val channelLabel = channelLabel(state.channel)

    Card(
        modifier = modifier.fillMaxWidth().testTag("update_card"),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, borderDivider),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.update_title),
                color = textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.update_current_version, state.currentVersion, state.currentVersionCode),
                color = textSecondary,
                fontSize = 12.sp,
            )
            Text(
                text = stringResource(R.string.update_source, channelLabel),
                color = textSecondary,
                fontSize = 12.sp,
            )

            UpdateStatus(
                state = state,
                channelLabel = channelLabel,
                textSecondary = textSecondary,
                primaryAccent = primaryAccent,
                onOpenUrl = onOpenUrl,
            )

            Button(
                onClick = onCheckForUpdates,
                enabled = !state.isChecking,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("check_for_updates_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryAccent),
            ) {
                if (state.isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.update_checking), fontSize = 13.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.update_check_button), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun UpdateStatus(
    state: UpdateUiState,
    channelLabel: String,
    textSecondary: Color,
    primaryAccent: Color,
    onOpenUrl: (url: String, fallbackUrl: String?) -> Unit,
) {
    when (val result = state.result) {
        null -> Text(
            text = stringResource(R.string.update_idle_description, channelLabel),
            color = textSecondary,
            fontSize = 12.sp,
        )

        is UpdateResult.UpToDate -> Text(
            text = stringResource(R.string.update_up_to_date),
            color = textSecondary,
            fontSize = 13.sp,
        )

        is UpdateResult.Available -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.update_available, result.latestVersion),
                color = primaryAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedButton(
                onClick = { onOpenUrl(result.url, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("open_update_source_button"),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.update_open_source, channelLabel),
                    fontSize = 13.sp,
                )
            }
        }

        is UpdateResult.OpenStore -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.update_play_store_description),
                color = textSecondary,
                fontSize = 12.sp,
            )
            OutlinedButton(
                onClick = { onOpenUrl(result.url, result.fallbackUrl) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("open_update_source_button"),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.update_open_play_store),
                    fontSize = 13.sp,
                )
            }
        }

        is UpdateResult.NotFound -> Text(
            text = stringResource(R.string.update_not_published, channelLabel),
            color = textSecondary,
            fontSize = 12.sp,
        )

        is UpdateResult.Error -> Text(
            text = stringResource(R.string.update_error),
            color = textSecondary,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun channelLabel(channel: UpdateChannel): String = stringResource(
    when (channel) {
        UpdateChannel.PLAY_STORE -> R.string.update_source_play_store
        UpdateChannel.GITHUB -> R.string.update_source_github
        UpdateChannel.FDROID -> R.string.update_source_fdroid
    },
)
