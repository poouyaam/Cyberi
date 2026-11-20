package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackUiState
import com.example.data.db.RecordingEntity
import com.example.data.repository.DeviceContact
import com.example.model.RecordingSettings
import com.example.ui.components.GlassCard
import com.example.ui.components.RecordingItemCard
import com.example.ui.theme.CyberiTheme
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.viewmodel.RecordingFilter
import com.example.util.DeviceCompatibilityManager

@Composable
fun RecordingsScreen(
    recordings: List<RecordingEntity>,
    allCount: Int,
    totalStorageBytes: Long,
    settings: RecordingSettings,
    hasContactsPermission: Boolean,
    contacts: List<DeviceContact>,
    searchQuery: String,
    selectedFilter: RecordingFilter,
    playbackState: PlaybackUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (RecordingFilter) -> Unit,
    onTogglePlay: (RecordingEntity) -> Unit,
    onToggleFavorite: (RecordingEntity) -> Unit,
    onLoadContacts: (String) -> Unit,
    onAssignContact: (Long, String?) -> Unit,
    onRequestContactsPermission: () -> Unit,
    onRenameRecording: (Long, String) -> Unit,
    onShareRecording: (RecordingEntity) -> Unit,
    onExportRecording: (RecordingEntity) -> Unit,
    onDeleteRecording: (RecordingEntity) -> Unit,
    onDeleteAllRecordings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CyberiTheme.colors
    var contactTarget by remember { mutableStateOf<RecordingEntity?>(null) }
    var renameTarget by remember { mutableStateOf<RecordingEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<RecordingEntity?>(null) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    val totalDurationMs = remember(recordings) { recordings.sumOf { it.durationMs } }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
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
                            text = "تاریخچه مکالمات",
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "$allCount فایل ذخیره شده • ${DeviceCompatibilityManager.formatDuration(totalDurationMs)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                    if (allCount > 0) {
                        IconButton(
                            onClick = { confirmDeleteAll = true },
                            modifier = Modifier.testTag("delete_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = "پاکسازی همه مکالمات",
                                tint = colors.crimson
                            )
                        }
                    }
                }
            }

            item {
                val limitBytes = settings.storageLimit.limitBytes
                val ratio = if (limitBytes > 0L) {
                    (totalStorageBytes.toFloat() / limitBytes.toFloat()).coerceIn(0f, 1f)
                } else 0.1f

                GlassCard(accentColor = colors.indigo, contentPadding = 12.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "حافظه مصرفی: ${DeviceCompatibilityManager.formatFileSize(totalStorageBytes)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "سقف مجاز: ${settings.storageLimit.label}",
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 12.sp,
                            color = colors.cyan
                        )
                    }
                    if (limitBytes > 0L) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { ratio },
                            color = if (ratio > 0.85f) colors.crimson else colors.cyan,
                            trackColor = colors.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("جستجو در نام، مخاطب یا فرمت فایل…", color = colors.textMuted) },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.cyan)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "پاک کردن جستجو", tint = colors.textSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.cyan,
                        unfocusedBorderColor = colors.glassBorderSubtle,
                        focusedContainerColor = colors.surface.copy(alpha = 0.8f),
                        unfocusedContainerColor = colors.surface.copy(alpha = 0.6f),
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_recordings_input")
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(RecordingFilter.entries) { filter ->
                        val selected = selectedFilter == filter
                        val chipColor = when (filter) {
                            RecordingFilter.ALL -> colors.cyan
                            RecordingFilter.INCOMING -> colors.emerald
                            RecordingFilter.OUTGOING -> colors.blue
                            RecordingFilter.MANUAL -> colors.amber
                            RecordingFilter.FAVORITES -> colors.crimson
                        }
                        FilterChip(
                            selected = selected,
                            onClick = { onFilterChange(filter) },
                            label = { Text(filter.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chipColor.copy(alpha = 0.18f),
                                selectedLabelColor = chipColor,
                                containerColor = colors.surface.copy(alpha = 0.7f),
                                labelColor = colors.textSecondary
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (selected) chipColor else colors.glassBorderSubtle
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
                                tint = colors.cyan.copy(alpha = 0.7f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "مکالمه‌ای در این بخش یافت نشد",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "مکالمات ضبط‌شده به‌طور خودکار در دیتابیس برنامه و پوشه Music/Cyberi ذخیره می‌شوند.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
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
                        onAssignContactClick = {
                            onLoadContacts("")
                            contactTarget = recording
                        },
                        onRenameClick = { renameTarget = recording },
                        onShareClick = { onShareRecording(recording) },
                        onExportClick = { onExportRecording(recording) },
                        onDeleteClick = { deleteTarget = recording }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(92.dp)) }
        }
    }

    contactTarget?.let { target ->
        ContactPickerDialog(
            contacts = contacts,
            hasPermission = hasContactsPermission,
            onSearch = onLoadContacts,
            onRequestPermission = onRequestContactsPermission,
            onSelectContact = { name ->
                onAssignContact(target.id, name)
                contactTarget = null
            },
            onDismiss = { contactTarget = null }
        )
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
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("حذف تمام مکالمات؟", color = colors.textPrimary) },
            text = { Text("تمام فایل‌های صوتی ذخیره شده برای همیشه حذف خواهند شد.", color = colors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAllRecordings()
                        confirmDeleteAll = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.crimson)
                ) {
                    Text("حذف همه", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) {
                    Text("انصراف", color = colors.cyan)
                }
            }
        )
    }
}

@Composable
fun ContactPickerDialog(
    contacts: List<DeviceContact>,
    hasPermission: Boolean,
    onSearch: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onSelectContact: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = CyberiTheme.colors
    var query by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("اتصال به مخاطب گوشی", color = colors.textPrimary) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!hasPermission) {
                    Text(
                        text = "برای نمایش لیست مخاطبین گوشی، دسترسی مخاطبین (READ_CONTACTS) را تایید کنید.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.amber
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.amber, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تایید دسترسی مخاطبین")
                    }
                } else {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            onSearch(it)
                        },
                        singleLine = true,
                        placeholder = { Text("جستجوی نام مخاطب…", color = colors.textMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.cyan,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item {
                            TextButton(onClick = { onSelectContact(null) }) {
                                Text("حذف برچسب مخاطب", color = colors.crimson)
                            }
                        }
                        items(contacts, key = { it.id }) { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectContact(contact.displayName) }
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = colors.indigo.copy(alpha = 0.18f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.Person, null, tint = colors.cyan, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(contact.displayName, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                                    if (!contact.phoneNumber.isNullOrBlank()) {
                                        Text(
                                            text = contact.phoneNumber,
                                            fontFamily = SpaceGroteskFontFamily,
                                            fontSize = 11.sp,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن", color = colors.cyan)
            }
        }
    )
}

@Composable
fun RenameRecordingDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = CyberiTheme.colors
    var text by remember(currentTitle) { mutableStateOf(currentTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("تغییر نام مکالمه", color = colors.textPrimary) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("عنوان جدید") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.cyan,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                colors = ButtonDefaults.buttonColors(containerColor = colors.cyan, contentColor = Color.White)
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف", color = colors.textSecondary)
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
    val colors = CyberiTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("حذف فایل صوتی", color = colors.textPrimary) },
        text = { Text("آیا از حذف «$title» مطمئن هستید؟", color = colors.textSecondary) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = colors.crimson)
            ) {
                Text("حذف", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف", color = colors.textSecondary)
            }
        }
    )
}
