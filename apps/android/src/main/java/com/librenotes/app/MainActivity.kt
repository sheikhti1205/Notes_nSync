package com.librenotes.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.librenotes.app.domain.MarkdownExporter
import com.librenotes.app.domain.NoteDocument

data class NoteUi(
    val title: String,
    val folder: String,
    val tag: String,
    val markdown: String,
    val starred: Boolean = false,
)

data class SetupState(
    val folderUri: String = "",
    val secret: String = "",
    val confirmSecret: String = "",
    val allowLettersSymbols: Boolean = false,
    val biometricEnabled: Boolean = false,
    val accentName: String = "Gold",
    val exportDefault: String = "PDF",
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LibreNotesTheme {
                LibreNotesApp()
            }
        }
    }
}

@Composable
fun LibreNotesTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = Color(0xFF181612),
        onPrimary = Color(0xFFFFF8E8),
        secondary = Color(0xFF0F766E),
        tertiary = Color(0xFFF6C737),
        surface = Color(0xFFFFFAF0),
        surfaceVariant = Color(0xFFF1EADC),
        outlineVariant = Color(0xFFDFD6C2),
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun LibreNotesApp() {
    var setupComplete by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }
    var setup by remember { mutableStateOf(SetupState()) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        when {
            !setupComplete -> SetupScreen(
                setup = setup,
                onSetupChange = { setup = it },
                onComplete = { setupComplete = true },
            )
            locked -> UnlockScreen(onUnlock = { locked = false })
            else -> WorkspaceScreen(
                setup = setup,
                onLock = { locked = true },
            )
        }
    }
}

@Composable
fun SetupScreen(setup: SetupState, onSetupChange: (SetupState) -> Unit, onComplete: () -> Unit) {
    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let { onSetupChange(setup.copy(folderUri = it.toString())) }
    }
    val secretValid = setup.secret.length >= 4 && setup.secret == setup.confirmSecret

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth > 780.dp
        if (wide) {
            Row(Modifier.fillMaxSize().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SetupIntro(Modifier.weight(1f).fillMaxHeight().padding(12.dp))
                SetupForm(
                    setup = setup,
                    secretValid = secretValid,
                    onChooseFolder = { folderLauncher.launch(null) },
                    onSetupChange = onSetupChange,
                    onComplete = onComplete,
                    modifier = Modifier.weight(1f).padding(12.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { SetupIntro(Modifier.fillMaxWidth().padding(12.dp)) }
                item {
                    SetupForm(
                        setup = setup,
                        secretValid = secretValid,
                        onChooseFolder = { folderLauncher.launch(null) },
                        onSetupChange = onSetupChange,
                        onComplete = onComplete,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun SetupIntro(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.libre_notes_icon),
            contentDescription = null,
            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.height(22.dp))
        Text("Libre Notes", fontSize = 56.sp, lineHeight = 54.sp, fontWeight = FontWeight.Black)
        Text(
            "Set up a private Markdown vault in a folder you control. Notes, folders, tags, and preferences are saved as one encrypted libre-notes.vault file.",
            modifier = Modifier.padding(top = 14.dp),
            fontSize = 18.sp,
            lineHeight = 28.sp,
        )
    }
}

@Composable
fun SetupForm(
    setup: SetupState,
    secretValid: Boolean,
    onChooseFolder: () -> Unit,
    onSetupChange: (SetupState) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("First-run setup", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            SetupRow("Choose vault folder", setup.folderUri.ifBlank { "Required" }, Icons.Outlined.Folder)
            Button(onClick = onChooseFolder, modifier = Modifier.fillMaxWidth()) {
                Text(if (setup.folderUri.isBlank()) "Choose folder" else "Change folder")
            }
            OutlinedTextField(
                value = setup.secret,
                onValueChange = { value ->
                    val filtered = if (setup.allowLettersSymbols) value else value.filter(Char::isDigit)
                    onSetupChange(setup.copy(secret = filtered))
                },
                label = { Text(if (setup.allowLettersSymbols) "Password" else "PIN") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = setup.confirmSecret,
                onValueChange = { value ->
                    val filtered = if (setup.allowLettersSymbols) value else value.filter(Char::isDigit)
                    onSetupChange(setup.copy(confirmSecret = filtered))
                },
                label = { Text("Confirm") },
                modifier = Modifier.fillMaxWidth(),
            )
            ToggleRow(
                checked = setup.allowLettersSymbols,
                label = "Include letters and symbols",
                onChange = { onSetupChange(setup.copy(allowLettersSymbols = it, secret = "", confirmSecret = "")) },
            )
            ToggleRow(
                checked = setup.biometricEnabled,
                label = "Enable fingerprint after password setup",
                onChange = { onSetupChange(setup.copy(biometricEnabled = it)) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { onSetupChange(setup.copy(accentName = "Gold")) }, label = { Text("Gold") })
                AssistChip(onClick = { onSetupChange(setup.copy(accentName = "Teal")) }, label = { Text("Teal") })
                AssistChip(onClick = { onSetupChange(setup.copy(exportDefault = "PDF")) }, label = { Text("PDF default") })
            }
            Button(
                enabled = setup.folderUri.isNotBlank() && secretValid,
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Security, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create encrypted vault")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(setup: SetupState, onLock: () -> Unit) {
    val notes = remember {
        listOf(
            NoteUi("Project Roadmap", "Projects", "#planning", SampleMarkdown, true),
            NoteUi("Privacy checklist", "Security", "#vault", "## Vault checks\n\n- [x] Folder selected\n- [x] AES-GCM payload\n- [ ] Attachment bundle option"),
            NoteUi("Study outline", "Study", "#markdown", "## CommonMark\n\n> Render once, export many.\n\n- HTML\n- PDF\n- DOC"),
        )
    }
    var selected by remember { mutableStateOf(notes.first()) }
    val exported = remember(selected) {
        MarkdownExporter.html(NoteDocument(selected.title, selected.folder, selected.tag, selected.markdown))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Libre Notes", fontWeight = FontWeight.Black) },
                actions = {
                    IconButton(onClick = onLock) { Icon(Icons.Outlined.Lock, contentDescription = "Lock") }
                    IconButton(onClick = {}) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            val wide = maxWidth > 920.dp
            if (wide) {
                Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NotesColumn(setup, notes, selected, onSelect = { selected = it }, Modifier.weight(0.24f).fillMaxHeight())
                    EditorColumn(selected, Modifier.weight(0.38f).fillMaxHeight())
                    PreviewColumn(exported, Modifier.weight(0.38f).fillMaxHeight())
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { NotesColumn(setup, notes, selected, onSelect = { selected = it }, Modifier.fillMaxWidth()) }
                    item { EditorColumn(selected, Modifier.fillMaxWidth()) }
                    item { PreviewColumn(exported, Modifier.fillMaxWidth()) }
                }
            }
        }
    }
}

@Composable
fun NotesColumn(setup: SetupState, notes: List<NoteUi>, selected: NoteUi, onSelect: (NoteUi) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VaultStatus(setup = setup)
        notes.forEach { note ->
            NoteCard(note = note, selected = note.title == selected.title, onClick = { onSelect(note) })
        }
    }
}

@Composable
fun EditorColumn(selected: NoteUi, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Editor", "Original Markdown source")
        Text(selected.markdown, modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFDFD6C2), RoundedCornerShape(18.dp)).padding(16.dp))
        ExportStrip()
    }
}

@Composable
fun PreviewColumn(exported: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Preview", "Rendered export structure")
        Text(
            exported.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim(),
            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFDFD6C2), RoundedCornerShape(18.dp)).padding(16.dp),
            lineHeight = 24.sp,
        )
    }
}

@Composable
fun UnlockScreen(onUnlock: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(86.dp), tint = Color(0xFF0F766E))
        Text("Unlock Libre Notes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text("Use fingerprint or your password after the phone has locked.", modifier = Modifier.padding(vertical = 12.dp))
        Button(onClick = onUnlock) { Text("Unlock session") }
    }
}

@Composable
fun VaultStatus(setup: SetupState) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF6EDCF)), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Security, contentDescription = null)
            Text("libre-notes.vault", fontWeight = FontWeight.Bold)
            Text("Encrypted folder storage", color = Color(0xFF70695B))
            Text(setup.folderUri.ifBlank { "Folder pending" }, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
        }
    }
}

@Composable
fun NoteCard(note: NoteUi, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF181612) else Color.White),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(note.title, modifier = Modifier.weight(1f), color = if (selected) Color.White else Color(0xFF181612), fontWeight = FontWeight.Bold)
                if (note.starred) Icon(Icons.Outlined.Article, contentDescription = null, tint = Color(0xFFF6C737))
            }
            Text("${note.folder} · ${note.tag}", color = if (selected) Color(0xFFD7CCB4) else Color(0xFF70695B), fontSize = 13.sp)
        }
    }
}

@Composable
fun ExportStrip() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("MD", "HTML", "PDF", "DOC").forEach {
            AssistChip(onClick = {}, label = { Text(it) }, leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null) })
        }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(subtitle, color = Color(0xFF70695B), fontSize = 13.sp)
        }
        Icon(Icons.Outlined.Visibility, contentDescription = null)
    }
}

@Composable
fun SetupRow(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0xFFF1EADC)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
        }
        Column {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, color = Color(0xFF70695B), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ToggleRow(checked: Boolean, label: String, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label)
    }
}

private const val SampleMarkdown = """# Project Roadmap

Libre Notes is a local-first Markdown notebook for focused writing.

## This sprint

- [x] First-run presenter
- [x] Encrypted vault setup
- [ ] Android native parity

> Notes should stay readable, portable, and private.

```kotlin
val vault = "libre-notes.vault"
```
"""
