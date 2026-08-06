package com.notesnync.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notesnync.app.R
import com.notesnync.app.branding.BuiltInCovers
import com.notesnync.app.branding.palette
import com.notesnync.app.domain.Destination
import com.notesnync.app.domain.EditorFontFamily
import com.notesnync.app.domain.EditorLineWidth
import com.notesnync.app.domain.NoteGestureAction
import com.notesnync.app.domain.SyncFolderAction
import com.notesnync.app.domain.SyncFolderRequest
import com.notesnync.app.domain.SyncProvider
import com.notesnync.app.domain.ThemeMode
import com.notesnync.app.domain.ThemeProfile
import com.notesnync.app.ui.NotesUiState
import com.notesnync.app.ui.NotesViewModel
import com.notesnync.app.ui.components.AccentDot
import com.notesnync.app.ui.components.EditFieldDialog
import com.notesnync.app.ui.components.OptionPickerDialog
import com.notesnync.app.ui.components.RowChevron
import com.notesnync.app.ui.components.RowDivider
import com.notesnync.app.ui.components.RowSwitch
import com.notesnync.app.ui.components.RowValue
import com.notesnync.app.ui.components.SettingsGroup
import com.notesnync.app.ui.components.SettingsRow
import com.notesnync.app.ui.components.SettingsSectionLabel
import coil.compose.AsyncImage

private enum class SettingsSection(val title: String, val subtitle: String, val icon: ImageVector) {
    Profile("Profile", "Username, public name, device", Icons.Outlined.Person),
    Workspace("Workspace", "Visuals, background, permissions", Icons.Outlined.Workspaces),
    Appearance("Appearance", "Theme, palette, UI scale", Icons.Outlined.Palette),
    Editor("Editor & Markdown", "Line width, font, syntax", Icons.Outlined.Code),
    Security("Security & Vault", "Vault, biometrics, screenshots", Icons.Outlined.Security),
    Sync("Account & Restore", "Google restore, advanced sync, conflicts", Icons.Outlined.CloudSync),
    SyncEngine("Sync Settings", "Auto sync, interval, network rules", Icons.Outlined.CloudSync),
    Permissions("Permissions", "Who can do what in this workspace", Icons.Outlined.Security),
    Conflicts("Conflict Resolution", "Merge strategy, records, ignored files", Icons.Outlined.Difference),
    Backup("Backup & Import", "Encrypted backup, Markdown import", Icons.Outlined.Backup),
    Diagnostics("Diagnostics", "Crash logs, local reports, sharing", Icons.Outlined.Settings),
    App("App Info", "Version and package details", Icons.Outlined.Settings),
}

@Composable
fun SettingsScreen(
    state: NotesUiState,
    viewModel: NotesViewModel,
    onPickSyncFolder: (SyncFolderRequest) -> Unit,
    onPickMarkdown: () -> Unit,
    onPickBackupFolder: () -> Unit,
    onPickBackupFile: (String) -> Unit,
    onConnectGoogleDrive: () -> Unit,
) {
    var section by remember { mutableStateOf<SettingsSection?>(null) }
    var showWorkspaceDialog by remember { mutableStateOf(false) }
    if (showWorkspaceDialog) WorkspaceVisualDialog(state, viewModel) { showWorkspaceDialog = false }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            if (section == null) {
                Column {
                    Text("Settings", fontWeight = FontWeight.Black, fontSize = 30.sp)
                    Text("Organized controls for your workspace, vault, restore, and backups.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { section = null }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null) }
                    Text(section!!.title, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                }
            }
        }
        if (section == null) {
            item { SettingsIndex { section = it } }
        } else {
            item {
                when (section) {
                    SettingsSection.Profile -> ProfileSettings(state, viewModel)
                    SettingsSection.Workspace -> WorkspaceSettings(state) { showWorkspaceDialog = true }
                    SettingsSection.Appearance -> AppearanceSettings(state, viewModel)
                    SettingsSection.Editor -> EditorSettings(state, viewModel)
                    SettingsSection.Security -> SecuritySettings(state, viewModel)
                    SettingsSection.Sync -> SyncSettings(state, viewModel, onPickSyncFolder, onConnectGoogleDrive) { section = it }
                    SettingsSection.SyncEngine -> SyncEngineSettings(state, viewModel)
                    SettingsSection.Permissions -> PermissionsSettings(state, viewModel)
                    SettingsSection.Conflicts -> ConflictSettings(state, viewModel)
                    SettingsSection.Backup -> BackupImportSettings(state, viewModel, onPickMarkdown, onPickBackupFolder, onPickBackupFile)
                    SettingsSection.Diagnostics -> DiagnosticsSettings(state, viewModel)
                    SettingsSection.App -> AppInfoSettings()
                    null -> Unit
                }
            }
        }
    }
}

@Composable
private fun SettingsIndex(onOpen: (SettingsSection) -> Unit) {
    SettingsGroup {
        SettingsSection.entries.forEachIndexed { i, section ->
            SettingsRow(
                title = section.title,
                subtitle = section.subtitle,
                icon = section.icon,
                onClick = { onOpen(section) },
                trailing = { RowChevron() },
            )
            if (i < SettingsSection.entries.size - 1) RowDivider()
        }
    }
}

@Composable
private fun ProfileSettings(state: NotesUiState, viewModel: NotesViewModel) {
    val username = state.settings.syncUserName.ifBlank { "owner" }
    val publicName = state.settings.syncPublicName
    val deviceName = state.settings.syncDeviceName
    // null = no dialog; otherwise which field is being edited
    var editing by remember { mutableStateOf<String?>(null) }
    val profileImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.updateProfileVisuals(uri?.toString(), state.settings.profileBackgroundUri)
    }
    val profileCoverPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.updateProfileVisuals(state.settings.profileImageUri, uri?.toString())
    }

    when (editing) {
        "username" -> EditFieldDialog("Username", username, "Used for mentions and links", { editing = null }) { viewModel.updateProfile(it, publicName, deviceName) }
        "public" -> EditFieldDialog("Public name", publicName, "Shown to other members", { editing = null }) { viewModel.updateProfile(username, it, deviceName) }
        "device" -> EditFieldDialog("Device name", deviceName, "Shown in sessions and sync", { editing = null }) { viewModel.updateProfile(username, publicName, it) }
        "picture" -> EditFieldDialog("Profile picture URI", state.settings.profileImageUri.orEmpty(), "Image URI", { editing = null }) { viewModel.updateProfileVisuals(it.ifBlank { null }, state.settings.profileBackgroundUri) }
        "cover" -> EditFieldDialog("Cover image / GIF URI", state.settings.profileBackgroundUri.orEmpty(), "Image/GIF URI", { editing = null }) { viewModel.updateProfileVisuals(state.settings.profileImageUri, it.ifBlank { null }) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        ProfileBannerCard(
            username = username,
            publicName = publicName,
            profileImageUri = state.settings.profileImageUri,
            coverUri = state.settings.profileBackgroundUri,
            onEditPicture = { profileImagePicker.launch(arrayOf("image/*")) },
            onEditCover = { profileCoverPicker.launch(arrayOf("image/*")) },
        )
        SettingsGroup {
            SettingsRow("Username", subtitle = username, icon = Icons.Outlined.Person, trailing = { EditPencil() }, onClick = { editing = "username" })
            RowDivider()
            SettingsRow("Public name", subtitle = publicName.ifBlank { "Shown to other members" }, icon = Icons.Outlined.Person, trailing = { EditPencil() }, onClick = { editing = "public" })
            RowDivider()
            SettingsRow("Device name", subtitle = deviceName.ifBlank { "Shown in sessions and sync" }, icon = Icons.Outlined.PhoneAndroid, trailing = { EditPencil() }, onClick = { editing = "device" })
        }
        SettingsGroup(header = "Profile customization") {
            SettingsRow("Profile picture", subtitle = "Pick from device", icon = Icons.Outlined.Person, trailing = { EditPencil() }, onClick = { profileImagePicker.launch(arrayOf("image/*")) })
            RowDivider()
            SettingsRow("Cover image / GIF", subtitle = "Pick from device", icon = Icons.Outlined.Palette, trailing = { EditPencil() }, onClick = { profileCoverPicker.launch(arrayOf("image/*")) })
            RowDivider()
            SettingsRow("Accent color", subtitle = state.settings.themeProfile.name, icon = Icons.Outlined.Palette, trailing = { RowChevron() })
        }
    }
}

@Composable
private fun EditPencil() {
    Icon(Icons.Outlined.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
}

@Composable
private fun WorkspaceSettings(state: NotesUiState, onEdit: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        // Current workspace hero card
        WorkspaceBannerCard(
            name = state.settings.workspaceName,
            icon = state.settings.workspaceIcon.ifBlank { "P" },
            backgroundUri = state.settings.workspaceBackgroundUri,
            onEdit = onEdit,
        )
        SettingsGroup {
            SettingsRow("Workspace icon", icon = Icons.Outlined.Workspaces, trailing = { RowChevron() }, onClick = onEdit)
            RowDivider()
            SettingsRow("Workspace background", icon = Icons.Outlined.Palette, trailing = { RowChevron() }, onClick = onEdit)
            RowDivider()
            SettingsRow("Details & Info", icon = Icons.Outlined.Settings, trailing = { RowChevron() }, onClick = onEdit)
            RowDivider()
            SettingsRow("Members", subtitle = "1", icon = Icons.Outlined.Person, trailing = { RowChevron() })
            RowDivider()
            SettingsRow("Permissions", icon = Icons.Outlined.Security, trailing = { RowChevron() })
            RowDivider()
            SettingsRow("Sections", subtitle = "Customize visible sections", icon = Icons.Outlined.Code, trailing = { RowChevron() })
            RowDivider()
            SettingsRow("Advanced", subtitle = "Transfer ownership, delete workspace", icon = Icons.Outlined.Difference, trailing = { RowChevron() }, onClick = onEdit)
        }
    }
}

@Composable
private fun ProfileBannerCard(
    username: String,
    publicName: String,
    profileImageUri: String?,
    coverUri: String?,
    onEditPicture: () -> Unit,
    onEditCover: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f), tonalElevation = 2.dp) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.95f).clickable(onClick = onEditCover)) {
            if (!coverUri.isNullOrBlank()) {
                AsyncImage(coverUri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(Color(0xFF20124A), Color(0xFF081426), MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)))),
                )
            }
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.58f)))))
            Row(
                Modifier.align(Alignment.BottomStart).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(68.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)).clickable(onClick = onEditPicture), contentAlignment = Alignment.Center) {
                    if (!profileImageUri.isNullOrBlank()) {
                        AsyncImage(profileImageUri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Text(publicName.ifBlank { username }.take(1).uppercase(), fontWeight = FontWeight.Black, fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Box(Modifier.align(Alignment.BottomEnd).size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Edit, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(13.dp))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(publicName.ifBlank { username }, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("@$username", color = Color.White.copy(alpha = 0.76f), fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun WorkspaceBannerCard(name: String, icon: String, backgroundUri: String?, onEdit: () -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f), tonalElevation = 2.dp) {
        Box(Modifier.fillMaxWidth().aspectRatio(2.12f).clickable(onClick = onEdit)) {
            if (!backgroundUri.isNullOrBlank()) {
                AsyncImage(backgroundUri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(Color(0xFF0C1430), Color(0xFF20124A), Color(0xFF4F2DB8)))),
                )
            }
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f)))))
            Row(
                Modifier.align(Alignment.BottomStart).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Text(icon.take(2), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary, fontSize = 22.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text(name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("1 member · Owner", color = Color.White.copy(alpha = 0.76f), fontSize = 12.sp)
                }
                Icon(Icons.Outlined.Edit, null, tint = Color.White.copy(alpha = 0.84f))
            }
        }
    }
}

@Composable
private fun AppearanceSettings(state: NotesUiState, viewModel: NotesViewModel) {
    val s = state.settings
    var showFontPicker by remember { mutableStateOf(false) }
    if (showFontPicker) OptionPickerDialog("Font", listOf("Inter", "System", "Serif", "Mono"), s.appFont, { showFontPicker = false }) { picked -> viewModel.patchSettings { it.copy(appFont = picked) } }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        // Theme tiles
        Column {
            SettingsSectionLabel("Theme")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeTile("System", Icons.Outlined.Settings, state.settings.themeMode == ThemeMode.System, Modifier.weight(1f)) { viewModel.setTheme(ThemeMode.System) }
                ThemeTile("Light", Icons.Outlined.LightMode, state.settings.themeMode == ThemeMode.Light, Modifier.weight(1f)) { viewModel.setTheme(ThemeMode.Light) }
                ThemeTile("Dark", Icons.Outlined.DarkMode, state.settings.themeMode == ThemeMode.Dark, Modifier.weight(1f)) { viewModel.setTheme(ThemeMode.Dark) }
            }
        }
        // Accent color dots (wired to palette)
        Column {
            SettingsSectionLabel("Accent color")
            SettingsGroup {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ThemeProfile.entries.take(6).forEach { profile ->
                        AccentDot(profile.palette().accent, profile == state.settings.themeProfile) { viewModel.setThemeProfile(profile) }
                    }
                    Spacer(Modifier.weight(1f))
                    Text("More", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        // Density / font / font-size / animations / reduce-motion
        SettingsGroup {
            SettingsRow("UI density", trailing = { RowValue(if (s.uiDensityCompact) "Compact" else "Comfortable", accent = true) }, onClick = { viewModel.patchSettings { it.copy(uiDensityCompact = !it.uiDensityCompact) } })
            RowDivider(inset = false)
            SettingsRow("Font", trailing = { RowValue(s.appFont, accent = true) }, onClick = { showFontPicker = true })
            RowDivider(inset = false)
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text("Font size", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("A", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(s.editorFontSize, { v -> viewModel.patchSettings { it.copy(editorFontSize = v) } }, modifier = Modifier.weight(1f))
                    Text("A", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            RowDivider(inset = false)
            SettingsRow("Animations", trailing = { RowSwitch(!s.reduceMotion) { viewModel.setPrivacyOption(reduceMotion = !it) } })
            RowDivider(inset = false)
            SettingsRow("Reduce motion", trailing = { RowSwitch(s.reduceMotion) { viewModel.setPrivacyOption(reduceMotion = it) } })
        }
    }
}

@Composable
private fun ThemeTile(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun EditorSettings(state: NotesUiState, viewModel: NotesViewModel) {
    val s = state.settings
    val lineWidths = EditorLineWidth.entries
    var showModePicker by remember { mutableStateOf(false) }
    var showLinePicker by remember { mutableStateOf(false) }
    var showFontPicker by remember { mutableStateOf(false) }
    if (showModePicker) OptionPickerDialog("Default mode", listOf("Edit", "Preview"), if (s.defaultEditMode) "Edit" else "Preview", { showModePicker = false }) { p -> viewModel.patchSettings { it.copy(defaultEditMode = p == "Edit") } }
    if (showLinePicker) OptionPickerDialog("Line width", lineWidths.map { it.name }, s.editorLineWidth.name, { showLinePicker = false }) { p -> viewModel.setEditorLineWidth(EditorLineWidth.valueOf(p)) }
    if (showFontPicker) OptionPickerDialog("Editor font", EditorFontFamily.entries.map { it.name }, s.editorFontFamily.name, { showFontPicker = false }) { p -> viewModel.setEditorFontFamily(EditorFontFamily.valueOf(p)) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        SettingsGroup(header = "Editor") {
            SettingsRow("Default mode", trailing = { RowChevron(if (s.defaultEditMode) "Edit" else "Preview") }, onClick = { showModePicker = true })
            RowDivider(inset = false)
            SettingsRow("Line width", trailing = { RowChevron(s.editorLineWidth.name) }, onClick = { showLinePicker = true })
            RowDivider(inset = false)
            SettingsRow("Font", trailing = { RowChevron(s.editorFontFamily.name) }, onClick = { showFontPicker = true })
            RowDivider(inset = false)
            SliderRow("Font size", s.editorFontSize) { v -> viewModel.patchSettings { it.copy(editorFontSize = v) } }
            RowDivider(inset = false)
            SliderRow("Tab size", (s.tabSize - 2) / 6f) { v -> viewModel.patchSettings { it.copy(tabSize = (2 + (v * 6)).toInt().coerceIn(2, 8)) } }
            RowDivider(inset = false)
            SettingsRow("Show line numbers", trailing = { RowSwitch(s.showLineNumbers) { v -> viewModel.patchSettings { it.copy(showLineNumbers = v) } } })
            RowDivider(inset = false)
            SettingsRow("Auto pair brackets", trailing = { RowSwitch(s.autoPairBrackets) { v -> viewModel.patchSettings { it.copy(autoPairBrackets = v) } } })
        }
        SettingsGroup(header = "Markdown") {
            SettingsRow("Live preview", trailing = { RowSwitch(s.showMarkdownSyntax) { viewModel.setShowMarkdownSyntax(it) } })
            RowDivider(inset = false)
            SettingsRow("Syntax highlight", trailing = { RowValue(if (s.syntaxColorful) "Colorful" else "Minimal", accent = true) }, onClick = { viewModel.patchSettings { it.copy(syntaxColorful = !it.syntaxColorful) } })
            RowDivider(inset = false)
            SettingsRow("Auto convert on paste", trailing = { RowSwitch(s.autoConvertOnPaste) { v -> viewModel.patchSettings { it.copy(autoConvertOnPaste = v) } } })
        }
        SettingsGroup(header = "Note interactions") {
            GestureRow("Long press", state.settings.noteLongPressAction) { viewModel.setNoteGestureActions(longPress = it) }
            RowDivider(inset = false)
            GestureRow("Swipe right", state.settings.noteSwipeStartAction) { viewModel.setNoteGestureActions(swipeStart = it) }
            RowDivider(inset = false)
            GestureRow("Swipe left", state.settings.noteSwipeEndAction) { viewModel.setNoteGestureActions(swipeEnd = it) }
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Float, onChange: (Float) -> Unit) {
    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Slider(value, onChange, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun GestureRow(label: String, selected: NoteGestureAction, onSelect: (NoteGestureAction) -> Unit) {
    val actions = NoteGestureAction.entries
    SettingsRow(label, trailing = { RowChevron(selected.name) }, onClick = {
        onSelect(actions[(actions.indexOf(selected) + 1) % actions.size])
    })
}

@Composable
private fun GestureActionRow(selected: NoteGestureAction, onSelect: (NoteGestureAction) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        NoteGestureAction.entries.forEach { action ->
            FilterChip(
                selected = selected == action,
                onClick = { onSelect(action) },
                label = { Text(action.name) },
            )
        }
    }
}

@Composable
private fun SecuritySettings(state: NotesUiState, viewModel: NotesViewModel) {
    val s = state.settings
    var vaultSecret by remember { mutableStateOf("") }
    var showPasswordField by remember { mutableStateOf(false) }
    var showAutoLockPicker by remember { mutableStateOf(false) }
    val autoLockOptions = mapOf("Immediately" to 0, "After 1 minute" to 1, "After 5 minutes" to 5, "After 15 minutes" to 15)
    fun autoLockLabel(m: Int) = autoLockOptions.entries.firstOrNull { it.value == m }?.key ?: "After 5 minutes"
    if (showAutoLockPicker) OptionPickerDialog("Auto lock", autoLockOptions.keys.toList(), autoLockLabel(s.autoLockMinutes), { showAutoLockPicker = false }) { p -> viewModel.patchSettings { it.copy(autoLockMinutes = autoLockOptions[p] ?: 5) } }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        SettingsGroup(header = "Vault") {
            SettingsRow(
                "Vault status",
                subtitle = if (s.vaultLockEnabled) "Locked" else "Not set up",
                icon = Icons.Outlined.Lock,
                trailing = { RowChevron() },
                onClick = { viewModel.lock() },
            )
            RowDivider()
            SettingsRow("Change vault password", icon = Icons.Outlined.Security, trailing = { RowChevron() }, onClick = { showPasswordField = !showPasswordField })
            RowDivider()
            SettingsRow("Biometric unlock", icon = Icons.Outlined.PhoneAndroid, trailing = { RowSwitch(s.requireBiometricOnOpen) { viewModel.setPrivacyOption(biometricOnOpen = it) } })
            RowDivider()
            SettingsRow("Auto lock", subtitle = autoLockLabel(s.autoLockMinutes), icon = Icons.Outlined.Lock, trailing = { RowChevron() }, onClick = { showAutoLockPicker = true })
        }
        if (showPasswordField) {
            SettingsGroup {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(vaultSecret, { vaultSecret = it }, label = { Text("PIN or password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.setVaultSecret(vaultSecret); showPasswordField = false }) { Text("Enable") }
                        ElevatedButton(onClick = viewModel::disableVault) { Text("Disable") }
                    }
                }
            }
        }
        SettingsGroup(header = "Security") {
            SettingsRow("Allow screenshots", subtitle = "Prevents screenshots in the app", trailing = { RowSwitch(!s.blockScreenshots) { viewModel.setPrivacyOption(blockScreenshots = !it) } })
            RowDivider(inset = false)
            SettingsRow("Hide content in recents", trailing = { RowSwitch(s.blockScreenshots) { viewModel.setPrivacyOption(blockScreenshots = it) } })
            RowDivider(inset = false)
            SettingsRow("App lock on exit", trailing = { RowSwitch(s.appLockOnExit) { v -> viewModel.patchSettings { it.copy(appLockOnExit = v) } } })
            RowDivider(inset = false)
            SettingsRow("Advanced", subtitle = "Manage encryption, keys, and sessions", icon = Icons.Outlined.Settings, trailing = { RowChevron() })
        }
    }
}

@Composable
private fun SyncSettings(
    state: NotesUiState,
    viewModel: NotesViewModel,
    onPickSyncFolder: (SyncFolderRequest) -> Unit,
    onConnectGoogleDrive: () -> Unit,
    onNavigate: (SettingsSection) -> Unit,
) {
    var showManual by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        SettingsGroup(header = "Account") {
            SettingsRow(
                state.settings.syncUserName.ifBlank { "aftaburdesign@gmail.com" },
                subtitle = if (state.googleDriveConnected) "Connected" else "Not connected",
                icon = Icons.Outlined.Person,
                onClick = onConnectGoogleDrive,
                trailing = { RowChevron() },
            )
            RowDivider()
            SettingsRow("Manage account", icon = Icons.Outlined.Settings, trailing = { RowChevron() }, onClick = onConnectGoogleDrive)
        }
        SettingsGroup {
            SettingsRow(
                "Google Drive",
                subtitle = if (state.googleDriveConnected) "Connected · Last sync: ${relativeSyncStatus(state)}" else "Not connected",
                icon = Icons.Outlined.CloudSync,
                onClick = onConnectGoogleDrive,
                trailing = { RowChevron() },
            )
            RowDivider()
            SettingsRow("Manage Google Drive", icon = Icons.Outlined.CloudSync, trailing = { RowChevron() }, onClick = onConnectGoogleDrive)
        }
        SettingsGroup(header = "Advanced") {
            SettingsRow("Restore from backup", icon = Icons.Outlined.Backup, trailing = { RowChevron() }, onClick = { showManual = true })
            RowDivider()
            SettingsRow("Sync settings", icon = Icons.Outlined.CloudSync, trailing = { RowChevron() }, onClick = { onNavigate(SettingsSection.SyncEngine) })
            RowDivider()
            SettingsRow("Conflict resolution", icon = Icons.Outlined.Difference, trailing = { RowChevron() }, onClick = { onNavigate(SettingsSection.Conflicts) })
            RowDivider()
            SettingsRow("Device sessions", subtitle = "1 active", icon = Icons.Outlined.PhoneAndroid, trailing = { RowChevron() })
        }
        if (showManual) {
            SettingsSectionLabel("Manual sync & restore")
            ManualSyncCard(state, viewModel, onPickSyncFolder, onConnectGoogleDrive)
        }
    }
}

private fun relativeSyncStatus(state: NotesUiState): String =
    state.settings.lastSyncAt?.let { relativeSettingsTime(it) + " ago" } ?: "Just now"

@Composable
private fun ManualSyncCard(
    state: NotesUiState,
    viewModel: NotesViewModel,
    onPickSyncFolder: (SyncFolderRequest) -> Unit,
    onConnectGoogleDrive: () -> Unit,
) = SettingsCard {
    var syncSecret by remember { mutableStateOf("") }
    var syncUsername by remember(state.settings.syncUserName) { mutableStateOf(state.settings.syncUserName.ifBlank { "owner" }) }
    var syncPublicName by remember(state.settings.syncPublicName) { mutableStateOf(state.settings.syncPublicName) }
    var syncDeviceName by remember(state.settings.syncDeviceName) { mutableStateOf(state.settings.syncDeviceName) }
    var syncProvider by remember(state.settings.syncProvider) { mutableStateOf(if (state.settings.syncProvider == SyncProvider.None) SyncProvider.GoogleDrive else state.settings.syncProvider) }
    Text("Status: ${state.settings.lastSyncStatus}", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text("Google account restore", fontWeight = FontWeight.Bold)
    Text(
        if (state.googleDriveConnected) "Connected. Notes'nync can back up and restore encrypted data from hidden Google Drive app data."
        else "Continue with Google to restore Notes'nync data after reinstalling or changing devices.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = onConnectGoogleDrive, enabled = !state.syncing) {
            Icon(Icons.Outlined.CloudSync, null)
            Text(if (state.googleDriveConnected) "Reconnect Google" else "Continue with Google")
        }
        ElevatedButton(onClick = viewModel::disconnectGoogleDrive, enabled = state.googleDriveConnected && !state.syncing) {
            Text("Sign out")
        }
    }
    Text(
        "Drive app data is private to this app. The visible Drive/OneDrive folder picker remains available below for advanced manual sync.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
    )
    OutlinedTextField(syncSecret, { syncSecret = it }, label = { Text("Sync key") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(syncUsername, { syncUsername = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(syncPublicName, { syncPublicName = it }, label = { Text("Public name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedTextField(syncDeviceName, { syncDeviceName = it }, label = { Text("Device name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    Text("Cloud restore", fontWeight = FontWeight.Bold)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = state.googleDriveConnected && syncSecret.isNotBlank() && !state.syncing,
            onClick = { viewModel.createGoogleDriveApiChain(syncSecret, syncUsername, syncPublicName, syncDeviceName) },
        ) { Text("Create Google backup") }
        ElevatedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = state.googleDriveConnected && syncSecret.isNotBlank() && !state.syncing,
            onClick = { viewModel.restoreGoogleDriveApiChain(syncSecret, syncUsername, syncPublicName, syncDeviceName) },
        ) { Text("Restore from Google") }
    }
    Text("Advanced manual sync", fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FilterChip(syncProvider == SyncProvider.GoogleDrive, { syncProvider = SyncProvider.GoogleDrive }, label = { Text("Drive folder") })
        FilterChip(syncProvider == SyncProvider.OneDrive, { syncProvider = SyncProvider.OneDrive }, label = { Text("OneDrive folder") })
        FilterChip(syncProvider == SyncProvider.LocalFolder, { syncProvider = SyncProvider.LocalFolder }, label = { Text("Local folder") })
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(modifier = Modifier.fillMaxWidth(), enabled = syncSecret.isNotBlank() && !state.syncing, onClick = { onPickSyncFolder(SyncFolderRequest(syncProvider, SyncFolderAction.CreateChain, syncSecret, syncUsername, syncPublicName, syncDeviceName)) }) { Text("Create manual chain") }
        ElevatedButton(modifier = Modifier.fillMaxWidth(), enabled = syncSecret.isNotBlank() && !state.syncing, onClick = { onPickSyncFolder(SyncFolderRequest(syncProvider, SyncFolderAction.RestoreChain, syncSecret, syncUsername, syncPublicName, syncDeviceName)) }) { Text("Restore manual chain") }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(enabled = syncSecret.isNotBlank() && !state.syncing, onClick = { viewModel.syncNow(syncSecret) }) { Icon(Icons.Outlined.CloudSync, null); Text("Sync now") }
        ElevatedButton(onClick = { viewModel.go(Destination.ConflictReview) }) { Icon(Icons.Outlined.Difference, null); Text("Conflicts") }
    }
}

// ---------- Mockup 13: Sync Settings ----------
@Composable
private fun SyncEngineSettings(state: NotesUiState, viewModel: NotesViewModel) {
    val s = state.settings
    var showInterval by remember { mutableStateOf(false) }
    val intervals = mapOf("15 minutes" to 15, "30 minutes" to 30, "1 hour" to 60, "6 hours" to 360, "Manual only" to 0)
    fun intervalLabel(m: Int) = intervals.entries.firstOrNull { it.value == m }?.key ?: "30 minutes"
    if (showInterval) OptionPickerDialog("Sync interval", intervals.keys.toList(), intervalLabel(s.syncIntervalMinutes), { showInterval = false }) { p -> viewModel.patchSettings { it.copy(syncIntervalMinutes = intervals[p] ?: 30) } }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        SettingsGroup(header = "Sync behavior") {
            SettingsRow("Auto sync", subtitle = "Sync on app exit and network return", trailing = { RowSwitch(s.autoSync) { v -> viewModel.patchSettings { it.copy(autoSync = v) } } })
            RowDivider(inset = false)
            SettingsRow("Background sync", subtitle = "Sync periodically in background", trailing = { RowSwitch(s.backgroundSync) { v -> viewModel.patchSettings { it.copy(backgroundSync = v) } } })
            RowDivider(inset = false)
            SettingsRow("Sync interval", trailing = { RowChevron(intervalLabel(s.syncIntervalMinutes)) }, onClick = { showInterval = true })
            RowDivider(inset = false)
            SettingsRow("Sync on mobile data", trailing = { RowSwitch(s.syncOnMobileData) { v -> viewModel.patchSettings { it.copy(syncOnMobileData = v) } } })
            RowDivider(inset = false)
            SettingsRow("Sync on battery saver", trailing = { RowSwitch(s.syncOnBatterySaver) { v -> viewModel.patchSettings { it.copy(syncOnBatterySaver = v) } } })
            RowDivider(inset = false)
            SettingsRow("Notify on errors", trailing = { RowSwitch(s.notifyOnErrors) { v -> viewModel.patchSettings { it.copy(notifyOnErrors = v) } } })
        }
        SettingsGroup(header = "Advanced") {
            SettingsRow("Selective sync", subtitle = "Choose what to sync", icon = Icons.Outlined.CloudSync, trailing = { RowSwitch(s.selectiveSync) { v -> viewModel.patchSettings { it.copy(selectiveSync = v) } } })
            RowDivider()
            SettingsRow("Rebuild local index", subtitle = "Re-index all content", icon = Icons.Outlined.Settings, trailing = { RowChevron() }, onClick = { viewModel.go(Destination.SyncMonitor) })
        }
    }
}

// ---------- Mockup 12: Permissions (per-workspace) ----------
@Composable
private fun PermissionsSettings(state: NotesUiState, viewModel: NotesViewModel) {
    val ws = state.workspaces.firstOrNull { it.id == state.settings.activeWorkspaceId }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Manage who can do what in this workspace.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
        if (ws == null) {
            SettingsGroup { SettingsRow("No active workspace", icon = Icons.Outlined.Workspaces) }
        } else {
            SettingsGroup {
                SettingsRow("Workspace owner", subtitle = "Only owner can change", icon = Icons.Outlined.Person, trailing = { RowValue("You") })
                RowDivider()
                SettingsRow("Rename workspace", subtitle = "Members", icon = Icons.Outlined.Workspaces, trailing = { RowSwitch(ws.permRename) { v -> viewModel.patchActiveWorkspacePermissions { it.copy(permRename = v) } } })
                RowDivider()
                SettingsRow("Change icon & background", subtitle = "Members", icon = Icons.Outlined.Palette, trailing = { RowSwitch(ws.permChangeIcon) { v -> viewModel.patchActiveWorkspacePermissions { it.copy(permChangeIcon = v) } } })
                RowDivider()
                SettingsRow("Invite members", subtitle = "Members", icon = Icons.Outlined.Person, trailing = { RowSwitch(ws.permInviteMembers) { v -> viewModel.patchActiveWorkspacePermissions { it.copy(permInviteMembers = v) } } })
                RowDivider()
                SettingsRow("Delete notes", subtitle = "Members", icon = Icons.Outlined.Difference, trailing = { RowSwitch(ws.permDeleteNotes) { v -> viewModel.patchActiveWorkspacePermissions { it.copy(permDeleteNotes = v) } } })
                RowDivider()
                SettingsRow("Edit notes", subtitle = "Members", icon = Icons.Outlined.Code, trailing = { RowSwitch(ws.permEditNotes) { v -> viewModel.patchActiveWorkspacePermissions { it.copy(permEditNotes = v) } } })
                RowDivider()
                SettingsRow("Create canvas boards", subtitle = "Members", icon = Icons.Outlined.Palette, trailing = { RowSwitch(ws.permCreateCanvas) { v -> viewModel.patchActiveWorkspacePermissions { it.copy(permCreateCanvas = v) } } })
                RowDivider()
                SettingsRow("Manage tasks", subtitle = "Members", icon = Icons.Outlined.Check, trailing = { RowSwitch(ws.permManageTasks) { v -> viewModel.patchActiveWorkspacePermissions { it.copy(permManageTasks = v) } } })
            }
            ElevatedButton(
                onClick = {
                    viewModel.patchActiveWorkspacePermissions {
                        it.copy(permRename = false, permChangeIcon = false, permInviteMembers = false, permDeleteNotes = false, permEditNotes = true, permCreateCanvas = true, permManageTasks = true)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Reset to default") }
        }
    }
}

// ---------- Mockup 14: Conflict Resolution ----------
@Composable
private fun ConflictSettings(state: NotesUiState, viewModel: NotesViewModel) {
    val s = state.settings
    var showAction by remember { mutableStateOf(false) }
    if (showAction) OptionPickerDialog("Default conflict action", listOf("Ask me", "Keep local", "Keep remote", "Keep both"), s.conflictDefaultAction, { showAction = false }) { p -> viewModel.patchSettings { it.copy(conflictDefaultAction = p) } }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        SettingsGroup {
            SettingsRow("Default conflict action", trailing = { RowChevron(s.conflictDefaultAction) }, onClick = { showAction = true })
            RowDivider(inset = false)
            SettingsRow("Auto-merge", subtitle = "Automatically merge non-conflicting changes", trailing = { RowSwitch(s.autoMergeNonConflicting) { v -> viewModel.patchSettings { it.copy(autoMergeNonConflicting = v) } } })
            RowDivider(inset = false)
            SettingsRow("Keep both copies", subtitle = "Keep a copy when conflicts occur", trailing = { RowSwitch(s.keepBothCopies) { v -> viewModel.patchSettings { it.copy(keepBothCopies = v) } } })
        }
        SettingsGroup {
            SettingsRow("Conflict records", subtitle = "View history of resolved conflicts", icon = Icons.Outlined.Difference, trailing = { RowChevron() }, onClick = { viewModel.go(Destination.ConflictReview) })
            RowDivider()
            SettingsRow("Ignored files", subtitle = "Manage files to ignore in conflicts", icon = Icons.Outlined.CloudOff, trailing = { RowChevron() })
        }
        ElevatedButton(onClick = { viewModel.go(Destination.ConflictReview) }, modifier = Modifier.fillMaxWidth()) { Text("Open detailed conflict report") }
    }
}

@Composable
private fun BackupImportSettings(
    state: NotesUiState,
    viewModel: NotesViewModel,
    onPickMarkdown: () -> Unit,
    onPickBackupFolder: () -> Unit,
    onPickBackupFile: (String) -> Unit,
) {
    val s = state.settings
    var showAdvanced by remember { mutableStateOf(false) }
    var showFreqPicker by remember { mutableStateOf(false) }
    if (showFreqPicker) OptionPickerDialog("Backup frequency", listOf("Daily", "Weekly", "Monthly"), s.backupFrequency, { showFreqPicker = false }) { p -> viewModel.patchSettings { it.copy(backupFrequency = p) } }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        SettingsGroup(header = "Backup") {
            SettingsRow("Create encrypted backup", subtitle = "Manual backup to device", icon = Icons.Outlined.Backup, trailing = { RowChevron() }, onClick = { showAdvanced = true })
            RowDivider()
            SettingsRow("Auto backup", icon = Icons.Outlined.CloudSync, trailing = { RowSwitch(s.autoBackup) { v -> viewModel.patchSettings { it.copy(autoBackup = v) } } })
            RowDivider()
            SettingsRow("Backup frequency", icon = Icons.Outlined.Settings, trailing = { RowChevron(s.backupFrequency) }, onClick = { showFreqPicker = true })
            RowDivider()
            SettingsRow("Backup location", subtitle = if (s.backupFolderUri.isNullOrBlank()) "Internal storage" else "Custom folder", icon = Icons.Outlined.PhoneAndroid, trailing = { RowChevron() }, onClick = onPickBackupFolder)
        }
        SettingsGroup(header = "Import") {
            SettingsRow("Import from Markdown", subtitle = ".md files and folders", icon = Icons.Outlined.ImportExport, trailing = { RowChevron() }, onClick = onPickMarkdown)
            RowDivider()
            SettingsRow("Import from ZIP", subtitle = "Notes'nync or other apps export", icon = Icons.Outlined.FileUpload, trailing = { RowChevron() }, onClick = { showAdvanced = true })
        }
        if (showAdvanced) BackupAdvancedCard(state, viewModel, onPickMarkdown, onPickBackupFolder, onPickBackupFile)
    }
}

@Composable
private fun BackupAdvancedCard(
    state: NotesUiState,
    viewModel: NotesViewModel,
    onPickMarkdown: () -> Unit,
    onPickBackupFolder: () -> Unit,
    onPickBackupFile: (String) -> Unit,
) = SettingsCard {
    var backupSecret by remember { mutableStateOf("") }
    var backupPayload by remember { mutableStateOf("") }
    Text("Backup folder", fontWeight = FontWeight.Bold)
    Text(
        if (state.settings.backupFolderUri.isNullOrBlank()) "No folder selected. Exports stay in the encrypted payload field."
        else "Selected folder is authorized. Exports also write encrypted .enc files.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = onPickBackupFolder) {
            Icon(Icons.Outlined.PhoneAndroid, null)
            Text(if (state.settings.backupFolderUri.isNullOrBlank()) "Choose folder" else "Change folder")
        }
        ElevatedButton(
            enabled = !state.settings.backupFolderUri.isNullOrBlank(),
            onClick = { viewModel.setBackupFolder("") },
        ) { Text("Clear") }
    }
    OutlinedTextField(backupSecret, { backupSecret = it }, label = { Text("Backup password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { viewModel.exportBackup(backupSecret) { backupPayload = it } }) { Icon(Icons.Outlined.FileDownload, null); Text("Export") }
        ElevatedButton(onClick = { viewModel.importBackup(backupSecret, backupPayload) }) { Icon(Icons.Outlined.FileUpload, null); Text("Import") }
    }
    ElevatedButton(
        enabled = backupSecret.isNotBlank(),
        onClick = { onPickBackupFile(backupSecret) },
    ) {
        Icon(Icons.Outlined.FileUpload, null)
        Text("Import .enc file")
    }
    Button(onClick = onPickMarkdown) { Icon(Icons.Outlined.ImportExport, null); Text("Import Markdown") }
    OutlinedTextField(backupPayload, { backupPayload = it }, label = { Text("Encrypted backup payload") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
}

@Composable
private fun DiagnosticsSettings(state: NotesUiState, viewModel: NotesViewModel) = SettingsCard {
    val diagnostics = state.diagnostics
    Text("Local diagnostics", fontWeight = FontWeight.Black, fontSize = 18.sp)
    Text(
        "Logs stay on this device unless you share them. No remote analytics service is used.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
    )
    SettingSwitch("Enable local logging", diagnostics.enabled) { viewModel.setDiagnosticsOptions(enabled = it) }
    SettingSwitch("Include device model and Android version", diagnostics.includeDeviceInfo) { viewModel.setDiagnosticsOptions(includeDeviceInfo = it) }
    SettingSwitch("Ask before sharing after a crash", diagnostics.askBeforeSharing) { viewModel.setDiagnosticsOptions(askBeforeSharing = it) }
    HorizontalDivider()
    Text("Last crash", fontWeight = FontWeight.Bold)
    Text(
        diagnostics.lastCrashSummary ?: "No crash recorded.",
        color = if (diagnostics.lastCrashSummary == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
        fontSize = 12.sp,
    )
    diagnostics.lastCrashAt?.let {
        Text("Recorded ${relativeSettingsTime(it)} ago", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
    Text("Log size: ${formatDiagnosticsBytes(diagnostics.logBytes)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    if (diagnostics.pendingCrashPrompt) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Notes'nync detected a previous crash.", fontWeight = FontWeight.Bold)
                Text("Share diagnostics if you want to inspect or send the local crash context.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = viewModel::shareDiagnostics, modifier = Modifier.weight(1f)) { Text("Share") }
                    ElevatedButton(onClick = viewModel::acknowledgeCrashPrompt, modifier = Modifier.weight(1f)) { Text("Dismiss") }
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Button(onClick = viewModel::shareDiagnostics, modifier = Modifier.weight(1f)) { Text("Share logs") }
        ElevatedButton(onClick = viewModel::clearDiagnostics, modifier = Modifier.weight(1f)) { Text("Clear logs") }
    }
}

@Composable
private fun AppInfoSettings() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        // Header card with logo + name + version
        SettingsGroup {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.notesnync_cutout), null, Modifier.size(48.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Notes'nync", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text("Version 1.0.0 (100)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Text("Your ideas, perfectly organized.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
        SettingsGroup {
            SettingsRow("What's new", icon = Icons.Outlined.Difference, trailing = { RowChevron() })
            RowDivider()
            SettingsRow("Help & Support", icon = Icons.Outlined.Security, trailing = { RowChevron() })
            RowDivider()
            SettingsRow("Privacy policy", icon = Icons.Outlined.Lock, trailing = { RowChevron() })
            RowDivider()
            SettingsRow("Terms of service", icon = Icons.Outlined.Difference, trailing = { RowChevron() })
            RowDivider()
            SettingsRow("Open source licenses", icon = Icons.Outlined.Code, trailing = { RowChevron() })
            RowDivider()
            SettingsRow("Rate the app", icon = Icons.Outlined.Check, trailing = { RowChevron() })
        }
        Text("Application ID com.notesnync.app · Open-source, Android-first.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun BuiltInSettingsCoverStrip(selectedUri: String, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BuiltInCovers.forEach { cover ->
            val selected = selectedUri == cover.uri
            Box(
                Modifier
                    .size(width = 104.dp, height = 58.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelect(cover.uri) },
            ) {
                Image(painterResource(cover.drawableRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.18f)))
                Text(
                    cover.title,
                    Modifier.align(Alignment.BottomStart).padding(7.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun relativeSettingsTime(timestamp: Long): String {
    val minutes = ((System.currentTimeMillis() - timestamp).coerceAtLeast(0) / 60_000).toInt()
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m"
        minutes < 1440 -> "${minutes / 60}h"
        else -> "${minutes / 1440}d"
    }
}

private fun formatDiagnosticsBytes(bytes: Long): String = when {
    bytes <= 0 -> "0 B"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${bytes / (1024 * 1024)} MB"
}

@Composable
private fun PaletteGrid(selected: ThemeProfile, onSelect: (ThemeProfile) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ThemeProfile.entries.toList().chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { profile ->
                    val p = profile.palette()
                    val isSel = profile == selected
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(p.c1, p.c2, p.c3)))
                            .border(if (isSel) 3.dp else 0.dp, if (isSel) MaterialTheme.colorScheme.onSurface else Color.Transparent, RoundedCornerShape(14.dp))
                            .clickable { onSelect(profile) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSel) Box(Modifier.size(24.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.28f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(checked, onChange)
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
