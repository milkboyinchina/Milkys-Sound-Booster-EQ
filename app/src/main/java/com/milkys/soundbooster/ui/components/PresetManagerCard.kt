package com.milkys.soundbooster.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.milkys.soundbooster.AudioEffectManager
import com.milkys.soundbooster.R
import com.milkys.soundbooster.ui.theme.AppColors

@Composable
fun PresetManagerCard(
    isEnabled: Boolean,
    currentPreset: String,
    defaultPreset: String,
    customPresets: Map<String, IntArray>,
    favoritePresets: Set<String>,
    eqBands: IntArray,
    cardColor: Color,
    borderDivider: Color,
    textPrimary: Color,
    textSecondary: Color,
    primaryAccent: Color,
    onApplyPreset: (String) -> Unit,
    onSaveCustomPreset: (String, IntArray) -> Boolean,
    onDeleteCustomPreset: (String) -> Unit,
    onDeleteCustomPresets: (Set<String>) -> Unit = {},
    onToggleFavorite: (String) -> Boolean = { true },
    onSetDefaultPreset: (String) -> Unit,
    onExportPreset: (String) -> String,
    onExportAllPresets: () -> String,
    onImportPreset: (String) -> String?,
    showText: Boolean = true
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showSaveDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var newPresetName by remember { mutableStateOf("") }
    var exportJsonText by remember { mutableStateOf("") }
    var exportTitleText by remember { mutableStateOf("") }
    var importJsonInput by remember { mutableStateOf("") }

    var isDeleteMode by remember { mutableStateOf(false) }
    var selectedForDelete by remember { mutableStateOf(setOf<String>()) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonText = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (jsonText.isNotEmpty()) {
                    importJsonInput = jsonText
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val matchedPresetName = remember(eqBands, customPresets) {
        AudioEffectManager.getMatchedPresetName(eqBands)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, borderDivider)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.preset_manager_title),
                color = textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (isEnabled) {
                            val defaultName = "Custom ${customPresets.size + 1}"
                            newPresetName = defaultName.take(10)
                            showSaveDialog = true
                        }
                    },
                    enabled = isEnabled,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.WarningContainer),
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.content_desc_save_preset), tint = Color.White, modifier = Modifier.size(15.dp))
                        if (showText) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(stringResource(R.string.action_save), fontSize = 11.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Button(
                    onClick = {
                        if (isEnabled) {
                            importJsonInput = ""
                            showImportDialog = true
                        }
                    },
                    enabled = isEnabled,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.CardAlt2),
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = stringResource(R.string.content_desc_import_preset), tint = primaryAccent, modifier = Modifier.size(15.dp))
                        if (showText) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(stringResource(R.string.action_import), fontSize = 11.sp, color = primaryAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Button(
                    onClick = {
                        if (isEnabled) {
                            val targetPreset = matchedPresetName ?: currentPreset
                            exportJsonText = onExportPreset(targetPreset)
                            exportTitleText = targetPreset
                            showExportDialog = true
                        }
                    },
                    enabled = isEnabled,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.CardAlt2),
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Upload, contentDescription = stringResource(R.string.content_desc_export_presets), tint = primaryAccent, modifier = Modifier.size(15.dp))
                        if (showText) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(stringResource(R.string.action_export), fontSize = 11.sp, color = primaryAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Button(
                    onClick = {
                        isDeleteMode = !isDeleteMode
                        if (!isDeleteMode) selectedForDelete = emptySet()
                    },
                    enabled = isEnabled,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDeleteMode) AppColors.Error else AppColors.CardAlt2),
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.content_desc_delete_mode), tint = if (isDeleteMode) Color.White else AppColors.ErrorLight, modifier = Modifier.size(15.dp))
                        if (showText) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(text = if (isDeleteMode) "Cancel" else "Delete", fontSize = 11.sp, color = if (isDeleteMode) Color.White else AppColors.ErrorLight, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            val builtInList = listOf("Flat", "Bass Booster", "Vocal Booster", "Rock", "Pop", "Jazz")
            val customKeys = customPresets.keys.filter { !builtInList.contains(it) }
            val allPresets = builtInList + customKeys

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (preset in allPresets) {
                    val isBuiltIn = builtInList.contains(preset)
                    val isSelected = matchedPresetName == preset || currentPreset == preset
                    val isFav = favoritePresets.contains(preset)
                    val isCheckedForDelete = selectedForDelete.contains(preset)
                    Surface(
                        onClick = {
                            if (isDeleteMode) {
                                if (!isBuiltIn) {
                                    selectedForDelete = if (isCheckedForDelete) selectedForDelete - preset else selectedForDelete + preset
                                }
                            } else if (isEnabled) {
                                onApplyPreset(preset)
                            }
                        },
                        enabled = isEnabled,
                        shape = RoundedCornerShape(16.dp),
                        color = when { isSelected -> primaryAccent.copy(alpha = 0.2f) else -> AppColors.SurfaceVariant },
                        border = BorderStroke(width = if (isSelected) 1.5.dp else 1.dp, color = if (isSelected) primaryAccent else AppColors.DarkCardAlt),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = preset, color = if (isSelected) primaryAccent else textPrimary, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(end = 8.dp))
                            if (isDeleteMode) {
                                Checkbox(checked = isCheckedForDelete, onCheckedChange = { checked -> if (!isBuiltIn) selectedForDelete = if (checked) selectedForDelete + preset else selectedForDelete - preset }, enabled = !isBuiltIn && isEnabled, colors = CheckboxDefaults.colors(checkedColor = AppColors.Error, disabledUncheckedColor = AppColors.BorderDark.copy(alpha = 0.3f)))
                            } else {
                                androidx.compose.material3.IconButton(onClick = { val ok = onToggleFavorite(preset); if (!ok) Toast.makeText(context, context.getString(R.string.preset_favorite_limit_reached), Toast.LENGTH_SHORT).show() }, enabled = isEnabled, modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp).size(48.dp)) {
                                    Icon(imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = stringResource(R.string.content_desc_favorite_preset), tint = if (isFav) AppColors.WarningTitle else textSecondary.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
            if (isDeleteMode) {
                Button(onClick = { if (selectedForDelete.isNotEmpty()) showDeleteConfirmDialog = true }, enabled = selectedForDelete.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Error, disabledContainerColor = AppColors.Error.copy(alpha = 0.4f)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "${stringResource(R.string.btn_delete_selected)} (${selectedForDelete.size})", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(onDismissRequest = { showSaveDialog = false }, title = { Text(stringResource(R.string.dialog_save_preset_title)) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.dialog_save_preset_desc), fontSize = 13.sp)
                OutlinedTextField(value = newPresetName, onValueChange = { if (it.length <= 10) newPresetName = it }, label = { Text(stringResource(R.string.dialog_preset_name_hint)) }, supportingText = { Text(text = "${newPresetName.length}/10", color = if (newPresetName.length >= 10) AppColors.SuccessLightAlt else textSecondary, fontSize = 11.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text(text = "Bands: ${eqBands.joinToString { if (it > 0) "+$it" else "$it" }} dB", fontSize = 12.sp, color = textSecondary)
            }
        }, confirmButton = { TextButton(onClick = { val resultMsg = AudioEffectManager.saveCustomPresetWithResult(newPresetName, eqBands); if (resultMsg == null) { Toast.makeText(context, "Saved custom preset '$newPresetName'", Toast.LENGTH_SHORT).show(); showSaveDialog = false } else { Toast.makeText(context, resultMsg, Toast.LENGTH_LONG).show() } }) { Text(stringResource(R.string.label_save_preset), fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text(stringResource(R.string.action_cancel)) } })
    }
    if (showExportDialog) {
        AlertDialog(onDismissRequest = { showExportDialog = false }, title = { Text(stringResource(R.string.dialog_export_title_prefix, exportTitleText)) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.dialog_export_desc), fontSize = 12.sp)
                OutlinedTextField(value = exportJsonText, onValueChange = {}, readOnly = true, maxLines = 8, modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp))
            }
        }, confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { clipboardManager.setText(AnnotatedString(exportJsonText)); Toast.makeText(context, "Preset copied to clipboard!", Toast.LENGTH_SHORT).show() }) { Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text(stringResource(R.string.action_copy)) }
                Button(onClick = { try { val shareIntent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, exportJsonText) }; context.startActivity(Intent.createChooser(shareIntent, "Share Preset JSON")) } catch (e: Exception) { Toast.makeText(context, "Share failed", Toast.LENGTH_SHORT).show() } }) { Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text(stringResource(R.string.action_share)) }
            }
        }, dismissButton = { TextButton(onClick = { showExportDialog = false }) { Text(stringResource(R.string.action_close)) } })
    }
    if (showImportDialog) {
        AlertDialog(onDismissRequest = { showImportDialog = false }, title = { Text(stringResource(R.string.dialog_import_title)) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.dialog_import_desc), fontSize = 12.sp)
                OutlinedTextField(value = importJsonInput, onValueChange = { importJsonInput = it }, placeholder = { Text("{\"name\": \"MyPreset\", \"values\": [8, 5, 2, 0, 0]}") }, maxLines = 8, modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { filePickerLauncher.launch("application/json") }) { Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text(stringResource(R.string.action_pick_file), fontSize = 12.sp) }
                    TextButton(onClick = { val text = clipboardManager.getText()?.text; if (!text.isNullOrEmpty()) importJsonInput = text else Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show() }) { Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text(stringResource(R.string.action_paste_clipboard), fontSize = 12.sp) }
                }
            }
        }, confirmButton = { Button(onClick = { val (importedName, errorMsg) = AudioEffectManager.importPresetWithResult(importJsonInput); if (importedName != null) { Toast.makeText(context, "Successfully imported '$importedName'!", Toast.LENGTH_SHORT).show(); showImportDialog = false } else { Toast.makeText(context, errorMsg ?: "Invalid preset JSON format.", Toast.LENGTH_LONG).show() } }) { Text(stringResource(R.string.action_save_apply), fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = { showImportDialog = false }) { Text(stringResource(R.string.action_cancel)) } })
    }
    if (showDeleteConfirmDialog) {
        AlertDialog(onDismissRequest = { showDeleteConfirmDialog = false }, title = { Text(stringResource(R.string.dialog_delete_title)) }, text = { Text(stringResource(R.string.dialog_delete_confirm)) }, confirmButton = { TextButton(onClick = { onDeleteCustomPresets(selectedForDelete); selectedForDelete = emptySet(); isDeleteMode = false; showDeleteConfirmDialog = false }) { Text(stringResource(R.string.action_yes), fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = { showDeleteConfirmDialog = false }) { Text(stringResource(R.string.action_no)) } })
    }
}
