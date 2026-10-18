package com.milkys.soundbooster.update

import com.milkys.soundbooster.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UpdateUiState(
    val channel: UpdateChannel,
    val currentVersion: String,
    val currentVersionCode: Int,
    val isChecking: Boolean = false,
    val result: UpdateResult? = null,
)

class UpdateController(
    private val checker: UpdateChecker,
    private val scope: CoroutineScope,
    currentVersion: String = BuildConfig.VERSION_NAME,
    currentVersionCode: Int = BuildConfig.VERSION_CODE.toIntOrNull() ?: 0,
) {
    private val _state = MutableStateFlow(
        UpdateUiState(
            channel = checker.channel,
            currentVersion = currentVersion,
            currentVersionCode = currentVersionCode,
        ),
    )
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    fun checkForUpdates() {
        if (_state.value.isChecking) return

        _state.update { it.copy(isChecking = true, result = null) }
        scope.launch {
            val result = try {
                checker.check()
            } catch (e: Exception) {
                UpdateResult.Error(e.message ?: "Update check failed")
            }
            _state.update { it.copy(isChecking = false, result = result) }
        }
    }

    fun clearResult() {
        _state.update { it.copy(result = null) }
    }
}
