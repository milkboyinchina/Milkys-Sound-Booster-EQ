package com.example

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class VolumeBoosterTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        // Ensure state is initialized
        AudioEffectManager.init(this)
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isEnabled = AudioEffectManager.isBoostEnabled.value
        val boost = AudioEffectManager.boostProgress.value
        
        tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Volume Booster"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isEnabled) "+${boost}%" else "Off"
        }
        
        try {
            tile.updateTile()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onClick() {
        super.onClick()
        val isEnabled = AudioEffectManager.isBoostEnabled.value
        val newState = !isEnabled
        
        AudioEffectManager.setBoostEnabled(newState)
        
        val serviceIntent = Intent(this, VolumeBoosterService::class.java).apply {
            action = if (newState) VolumeBoosterService.ACTION_START else VolumeBoosterService.ACTION_STOP
        }
        
        try {
            if (newState) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        updateTileState()
    }
}
