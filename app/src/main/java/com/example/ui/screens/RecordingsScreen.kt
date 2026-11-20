package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.audio.PlaybackUiState
import com.example.data.db.RecordingEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.RecordingItemCard
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberDeepNavy
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberVoid
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.RecordingFilter

@Composable
fun RecordingsScreen(
    recordings: List<RecordingEntity>,
    searchQuery: String,
    selectedFilter: RecordingFilter,
    playbackState: PlaybackUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (RecordingFilter) -> Unit,
    onTogglePlay: (RecordingEntity) -> Unit,
    onToggleFavorite: (RecordingEntity) -> Unit,
    onRenameRecording: (Long, String) -> Unit,
    onShareRecording: (RecordingEntity) -> Unit,
    onExportRecording: (RecordingEntity) -> Unit,
    onDeleteRecording: (RecordingEntity) -> Unit,
    onDeleteAllRecordings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var renameTarget by remember { mutableStateOf<RecordingEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<RecordingEntity?>(null) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "آرشیو مکالمات",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${recordings.size} فایل صوتی ذخیره شده",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    if (recordings.isNotEmpty()) {
                        IconButton(
                            onClick = { confirmDeleteAll = true },
                            modifier = Modifier.testTag("delete_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = "پاکسازی همه مکالمات",
                                tint = CyberCrimson
                            )
                        }
                    }
                }
            }

            // Glass Search Input
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("جستجو در نام یا فرمت فایل…", color = TextMuted) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = CyberCyan)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "پاک کردن جستجو", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = GlassBorderSubtle,
                        focusedContainerColor = CyberSurface.copy(alpha = 0.6f),
                        unfocusedContainerColor = CyberSurface.copy(alpha = 0.45f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_recordings_input")
                )
            }

            // Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(RecordingFilter.entries) { filter ->
                        val selected = selectedFilter == filter
                        FilterChip(
                            selected = selected,
                            onClick = { onFilterChange(filter) },
                            label = { Text(filter.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                                selectedLabelColor = CyberCyan,
                                containerColor = CyberSurface.copy(alpha = 0.55f),
                                labelColor = TextSecondary
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (selected) CyberCyan else GlassBorderSubtle
                            ),
                            modifier = Modifier.testTag("filter_chip_${filter.name}")
                        )
                    }
                }
            }

            if (recordings.isEmpty()) {
                item {
                    GlassCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = CyberCyan.copy(alpha = 0.65f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "مکالمه‌ای در این بخش یافت نشد",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "مکالمات ضبط‌شده به‌طور خودکار در حافظه گوشی و پوشه Music/Cyberi ذخیره می‌شوند.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(recordings, key = { it.id }) { recording ->
                    val isPlayingThis = playbackState.activeRecordingId == recording.id && playbackState.isPlaying
                    val progress = if (playbackState.activeRecordingId == recording.id && playbackState.durationMs > 0) {
                        playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()
                    } else 0f

                    RecordingItemCard(
                        recording = recording,
                        isPlayingThis = isPlayingThis,
                        playbackProgress = progress,
                        onTogglePlay = { onTogglePlay(recording) },
                        onToggleFavorite = { onToggleFavorite(recording) },
                        onRenameClick = { renameTarget = recording },
                        onShareClick = { onShareRecording(recording) },
                        onExportClick = { onExportRecording(recording) },
                        onDeleteClick = { deleteTarget = recording }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    renameTarget?.let { target ->
        RenameRecordingDialog(
            currentTitle = target.title,
            onDismiss = { renameTarget = null },
            onConfirm = { newTitle ->
                onRenameRecording(target.id, newTitle)
                renameTarget = null
            }
        )
    }

    deleteTarget?.let { target ->
        DeleteRecordingDialog(
            title = target.title,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                onDeleteRecording(target)
                deleteTarget = null
            }
        )
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            containerColor = CyberDeepNavy,
            title = { Text("حذف تمام مکالمات؟", color = TextPrimary) },
            text = { Text("تمام فایل‌های صوتی ذخیره شده برای همیشه حذف خواهند شد.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllRecordings()
                        confirmDeleteAll = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCrimson)
                ) {
                    Text("حذف همه", color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) {
                    Text("انصراف", color = CyberCyan)
                }
            }
        )
    }
}

@Composable
fun RenameRecordingDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember(currentTitle) { mutableStateOf(currentTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberDeepNavy,
        title = { Text("تغییر نام مکالمه", color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("عنوان جدید") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberVoid)
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف", color = TextSecondary)
            }
        }
    )
}

@Composable
fun DeleteRecordingDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberDeepNavy,
        title = { Text("حذف فایل صوتی", color = TextPrimary) },
        text = { Text("آیا از حذف «$title» مطمئن هستید؟", color = TextSecondary) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCrimson)
            ) {
                Text("حذف", color = TextPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف", color = TextSecondary)
            }
        }
    )
}
