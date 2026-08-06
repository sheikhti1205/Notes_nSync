package com.notesnync.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notesnync.app.domain.Destination
import com.notesnync.app.domain.EditorFontFamily
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.notesnync.app.domain.Attachment
import com.notesnync.app.domain.EditorLineWidth
import com.notesnync.app.domain.EditorMode
import com.notesnync.app.domain.MarkdownBlock
import com.notesnync.app.domain.MarkdownExporter
import com.notesnync.app.domain.Note
import com.notesnync.app.domain.NoteEmbedItem
import com.notesnync.app.domain.NoteEmbedType
import com.notesnync.app.ui.NotesUiState
import com.notesnync.app.ui.NotesViewModel
import com.notesnync.app.ui.components.EmptyEditor
import com.notesnync.app.ui.components.MarkdownWebView
import coil.compose.AsyncImage
import kotlin.math.roundToInt

@Composable
fun NoteEditorScreen(
    state: NotesUiState,
    viewModel: NotesViewModel,
    modifier: Modifier,
    onPickAttachment: () -> Unit = {},
    onPickEmbed: () -> Unit = {},
    onPickCover: () -> Unit = {},
) {
    val note = state.selectedNote
    if (note == null) {
        EmptyEditor(viewModel::createNote, modifier)
        return
    }
    var title by remember(note.id) { mutableStateOf(note.title) }
    var body by remember(note.id) { mutableStateOf(TextFieldValue(note.bodyMarkdown)) }
    val undoStack = remember(note.id) { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember(note.id) { mutableStateListOf<TextFieldValue>() }
    var readOnlySource by remember(note.id) { mutableStateOf(false) }
    var toolbarCollapsed by remember(note.id) { mutableStateOf(false) }
    var toolbarOffset by remember(note.id) { mutableStateOf(androidx.compose.ui.geometry.Offset(18f, 18f)) }
    fun commitBody(value: TextFieldValue, trackHistory: Boolean = true) {
        if (trackHistory && value.text != body.text) {
            undoStack += body
            if (undoStack.size > 80) undoStack.removeAt(0)
            redoStack.clear()
        }
        body = value
        viewModel.updateNote(note, title, value.text)
    }
    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack += body
        commitBody(previous, trackHistory = false)
    }
    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack += body
        commitBody(next, trackHistory = false)
    }
    fun attachNoteLink(target: Note) {
        commitBody(insertBlock(body, "\n[[${target.title.ifBlank { "Untitled note" }}]]\n"))
    }
    val inlineImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val label = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Image"
        commitBody(insertBlock(body, "\n![$label]($uri)\n"))
        viewModel.embedFileToSelectedNote(uri)
    }
    val maxLineWidth = when (state.settings.editorLineWidth) {
        EditorLineWidth.Narrow -> 560.dp
        EditorLineWidth.Comfortable -> 720.dp
        EditorLineWidth.Wide -> 980.dp
    }
    val editorFont = when (state.settings.editorFontFamily) {
        EditorFontFamily.Sans -> FontFamily.SansSerif
        EditorFontFamily.Serif -> FontFamily.Serif
    }

    BoxWithConstraints(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp)
            .imePadding(),
    ) {
        val density = LocalDensity.current
        val imeVisible = WindowInsets.ime.getBottom(density) > 0
        val maxFloatingX = with(density) { (maxWidth - 164.dp).toPx().coerceAtLeast(0f) }
        val maxFloatingY = with(density) { (maxHeight - 118.dp).toPx().coerceAtLeast(0f) }
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        // Top action bar
        EditorTopBar(
            note = note,
            title = title,
            body = body.text,
            state = state,
            viewModel = viewModel,
            onPickAttachment = onPickAttachment,
            onPickEmbed = onPickEmbed,
            onPickCover = onPickCover,
            maxLineWidth = maxLineWidth,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            readOnlySource = readOnlySource,
            onUndo = ::undo,
            onRedo = ::redo,
            onToggleReadOnly = { readOnlySource = !readOnlySource },
            onSave = { viewModel.updateNote(note, title, body.text) },
        ) { newTitle ->
            title = newTitle
        }

        // Page editor / Source / Preview
        if (state.editorMode == EditorMode.Page) {
            VisualPageEditor(
                note = note.copy(title = title, bodyMarkdown = body.text),
                notes = state.notes,
                editorFont = editorFont,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = maxLineWidth),
                onPickAttachment = onPickAttachment,
                onPickEmbed = onPickEmbed,
                onPickImage = { inlineImagePicker.launch(arrayOf("image/*")) },
                onAttachNote = ::attachNoteLink,
                onMarkdownChange = { commitBody(TextFieldValue(it)) },
                onInsertBlock = { block -> commitBody(insertBlock(body, block)) },
            )
        } else if (state.editorMode == EditorMode.Edit) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = maxLineWidth),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            ) {
                Box(Modifier.fillMaxSize()) {
                    // Subtle line numbers / margin guide
                    Box(
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 8.dp, top = 16.dp, bottom = 16.dp)
                            .width(2.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                    )
                    BasicTextField(
                        value = body,
                        onValueChange = { if (!readOnlySource) commitBody(it) },
                        readOnly = readOnlySource,
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 26.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = editorFont,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .verticalScroll(rememberScrollState()),
                    )
                }
            }
        } else {
            MarkdownPreview(
                note.copy(title = title, bodyMarkdown = body.text),
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = maxLineWidth),
                onPickCover = onPickCover,
            )
        }

        // Bottom formatting toolbar (only useful while editing)
        if (state.editorMode != EditorMode.Preview && !toolbarCollapsed) {
            EditorBottomToolbar(
                maxLineWidth = maxLineWidth,
                imeVisible = imeVisible,
                onCollapse = { toolbarCollapsed = true },
                onAttach = onPickAttachment,
                onEmbed = onPickEmbed,
                onImage = { inlineImagePicker.launch(arrayOf("image/*")) },
            ) { action -> commitBody(applyMarkdown(body, action)) }
        }
        }
        if (state.editorMode != EditorMode.Preview && toolbarCollapsed) {
            FloatingEditorToolbar(
                modifier = Modifier.offset { IntOffset(toolbarOffset.x.roundToInt(), toolbarOffset.y.roundToInt()) },
                onDrag = { delta ->
                    toolbarOffset = androidx.compose.ui.geometry.Offset(
                        x = (toolbarOffset.x + delta.x).coerceIn(0f, maxFloatingX),
                        y = (toolbarOffset.y + delta.y).coerceIn(0f, maxFloatingY),
                    )
                },
                onExpand = { toolbarCollapsed = false },
                onFormat = { action -> commitBody(applyMarkdown(body, action)) },
            )
        }
    }
}

@Composable
private fun NoteEmbedsBlock(
    note: Note,
    notes: List<Note>,
    viewModel: NotesViewModel,
    onPickEmbed: () -> Unit,
    onAttachNote: (Note) -> Unit,
    maxLineWidth: androidx.compose.ui.unit.Dp,
) {
    var link by remember(note.id) { mutableStateOf("") }
    val attachableNotes = remember(notes, note.id) {
        notes.filter { it.id != note.id && !it.archived }.take(8)
    }
    Surface(
        modifier = Modifier.fillMaxWidth().widthIn(max = maxLineWidth),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Link, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text("Embeds", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text("${note.embeds.size}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (note.embeds.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    note.embeds.forEach { embed ->
                        EmbedCard(embed)
                    }
                }
            }
            if (attachableNotes.isNotEmpty()) {
                Text("Attach note", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(attachableNotes, key = { it.id }) { linkedNote ->
                        Surface(
                            modifier = Modifier.clickable { onAttachNote(linkedNote) },
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        ) {
                            Text(
                                linkedNote.title.ifBlank { "Untitled note" },
                                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicTextField(
                    value = link,
                    onValueChange = { link = it },
                    textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)).padding(horizontal = 10.dp, vertical = 8.dp),
                    singleLine = true,
                )
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary) {
                    Text(
                        "Add",
                        Modifier.clickable {
                            viewModel.addLinkEmbedToSelectedNote(link)
                            link = ""
                        }.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) {
                    Row(
                        Modifier.clickable(onClick = onPickEmbed).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Outlined.AttachFile, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Embed file/media", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text("Images, video, audio, PDFs, and files stay linked as syncable metadata.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EmbedCard(embed: NoteEmbedItem) {
    val context = LocalContext.current
    val icon = when (embed.type) {
        NoteEmbedType.Link -> Icons.Outlined.Link
        NoteEmbedType.Image -> Icons.Outlined.Image
        NoteEmbedType.Video -> Icons.Outlined.PlayArrow
        NoteEmbedType.Audio -> Icons.Outlined.Mic
        NoteEmbedType.File -> Icons.Outlined.AttachFile
        NoteEmbedType.Canvas -> Icons.Outlined.Code
        NoteEmbedType.Task -> Icons.Outlined.Book
    }
    val accent = when (embed.type) {
        NoteEmbedType.Image -> Color(0xFF4AADFF)
        NoteEmbedType.Video -> Color(0xFFF276E2)
        NoteEmbedType.Audio -> Color(0xFF56CC98)
        NoteEmbedType.File -> Color(0xFFFFCF52)
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
        Column(Modifier.width(172.dp).padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(embed.type.name, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(embed.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
            if (embed.type == NoteEmbedType.Image) {
                AsyncImage(
                    model = embed.target,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(78.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    embed.preview.ifBlank { embed.target },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            Text(embed.target, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                EmbedActionButton("Open", Icons.AutoMirrored.Outlined.OpenInNew, Modifier.weight(1f)) {
                    context.openEmbedTarget(embed)
                }
                EmbedActionButton("Share", Icons.Outlined.Share, Modifier.weight(1f)) {
                    context.shareEmbedTarget(embed)
                }
            }
        }
    }
}

@Composable
private fun EmbedActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
    ) {
        Row(
            Modifier.clickable(onClick = onClick).padding(horizontal = 7.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun AttachmentInlineCard(attachment: Attachment) {
    val context = LocalContext.current
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
        Column(Modifier.width(172.dp).padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Icon(
                        if (attachment.mimeType.startsWith("image/")) Icons.Outlined.Image else Icons.Outlined.AttachFile,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("Attachment", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(attachment.displayName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
            if (attachment.mimeType.startsWith("image/") || imageLikeUri(attachment.uri)) {
                AsyncImage(
                    model = attachment.uri,
                    contentDescription = attachment.displayName,
                    modifier = Modifier.fillMaxWidth().height(78.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    attachment.mimeType,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            Text(attachment.uri, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                EmbedActionButton("Open", Icons.AutoMirrored.Outlined.OpenInNew, Modifier.weight(1f)) {
                    context.openAttachmentTarget(attachment)
                }
                EmbedActionButton("Share", Icons.Outlined.Share, Modifier.weight(1f)) {
                    context.shareAttachmentTarget(attachment)
                }
            }
        }
    }
}

private fun Context.openEmbedTarget(embed: NoteEmbedItem) {
    val target = embed.target.trim()
    if (target.isBlank()) return
    val uri = embed.toOpenUri()
    val intent = Intent(Intent.ACTION_VIEW).apply {
        if (embed.type == NoteEmbedType.Link) {
            data = uri
        } else {
            setDataAndType(uri, embed.mimeType())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    runCatching {
        startActivity(Intent.createChooser(intent, "Open ${embed.title.ifBlank { embed.type.name }}"))
    }
}

private fun Context.openAttachmentTarget(attachment: Attachment) {
    val uri = Uri.parse(attachment.uri)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, attachment.mimeType.ifBlank { "application/octet-stream" })
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        startActivity(Intent.createChooser(intent, "Open ${attachment.displayName.ifBlank { "attachment" }}"))
    }
}

private fun Context.shareAttachmentTarget(attachment: Attachment) {
    val uri = Uri.parse(attachment.uri)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = attachment.mimeType.ifBlank { "application/octet-stream" }
        putExtra(Intent.EXTRA_TITLE, attachment.displayName.ifBlank { "attachment" })
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        startActivity(Intent.createChooser(intent, "Share ${attachment.displayName.ifBlank { "attachment" }}"))
    }
}

private fun Context.shareEmbedTarget(embed: NoteEmbedItem) {
    val target = embed.target.trim()
    if (target.isBlank()) return
    val uri = embed.toOpenUri()
    val intent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TITLE, embed.title.ifBlank { embed.type.name })
        if (embed.type == NoteEmbedType.Link) {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, uri.toString())
        } else {
            type = embed.mimeType()
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    runCatching {
        startActivity(Intent.createChooser(intent, "Share ${embed.title.ifBlank { embed.type.name }}"))
    }
}

@Composable
private fun VisualPageEditor(
    note: Note,
    notes: List<Note>,
    editorFont: FontFamily,
    modifier: Modifier,
    onPickAttachment: () -> Unit,
    onPickEmbed: () -> Unit,
    onPickImage: () -> Unit,
    onAttachNote: (Note) -> Unit,
    onMarkdownChange: (String) -> Unit,
    onInsertBlock: (String) -> Unit,
) {
    val blocks = remember(note.bodyMarkdown) { MarkdownExporter.blocks(note.bodyMarkdown) }
    val attachableNotes = remember(notes, note.id) { notes.filter { it.id != note.id && !it.archived }.take(8) }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        ) {
            item {
                InlineMediaTools(
                    note = note,
                    attachableNotes = attachableNotes,
                    onPickAttachment = onPickAttachment,
                    onPickEmbed = onPickEmbed,
                    onPickImage = onPickImage,
                    onAttachNote = onAttachNote,
                )
            }
            if (blocks.isEmpty()) {
                item {
                    EmptyVisualBlock(onInsertBlock, onPickImage)
                }
            } else {
                items(blocks.size, key = { index -> "block-${note.id}-$index-${blocks[index].hashCode()}" }) { index ->
                    VisualMarkdownBlock(
                        block = blocks[index],
                        editorFont = editorFont,
                        onChange = { changed ->
                            onMarkdownChange(blocks.rebuildWith(index, changed))
                        },
                        onDelete = {
                            onMarkdownChange(blocks.filterIndexed { i, _ -> i != index }.joinMarkdown())
                        },
                    )
                }
            }
            item {
                AddBlockRow(onInsertBlock, onPickImage)
            }
        }
    }
}

@Composable
private fun EmptyVisualBlock(onInsertBlock: (String) -> Unit, onPickImage: () -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("A quiet page is ready", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text("Add a heading, paragraph, checklist, image, or code block.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            AddBlockRow(onInsertBlock, onPickImage)
        }
    }
}

@Composable
private fun AddBlockRow(onInsertBlock: (String) -> Unit, onPickImage: () -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BlockInsertChip("Text") { onInsertBlock("\nNew paragraph\n") }
        BlockInsertChip("Heading") { onInsertBlock("\n## Heading\n") }
        BlockInsertChip("Task") { onInsertBlock("\n- [ ] New task\n") }
        BlockInsertChip("Quote") { onInsertBlock("\n> Quote\n") }
        BlockInsertChip("Code") { onInsertBlock("\n```text\ncode\n```\n") }
        BlockInsertChip("Image") { onPickImage() }
    }
}

@Composable
private fun InlineMediaTools(
    note: Note,
    attachableNotes: List<Note>,
    onPickAttachment: () -> Unit,
    onPickEmbed: () -> Unit,
    onPickImage: () -> Unit,
    onAttachNote: (Note) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BlockInsertChip("Image") { onPickImage() }
            BlockInsertChip("File") { onPickAttachment() }
            BlockInsertChip("Embed") { onPickEmbed() }
            attachableNotes.forEach { linkedNote ->
                BlockInsertChip("[[${linkedNote.title.ifBlank { "Untitled" }.take(18)}]]") { onAttachNote(linkedNote) }
            }
        }
        if (note.attachments.isNotEmpty() || note.embeds.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(note.attachments, key = { "attachment-${it.id}" }) { attachment ->
                    AttachmentInlineCard(attachment)
                }
                items(note.embeds, key = { "embed-${it.id}" }) { embed ->
                    EmbedCard(embed)
                }
            }
        }
    }
}

@Composable
private fun BlockInsertChip(label: String, onClick: () -> Unit) {
    Surface(Modifier.clickable(onClick = onClick), shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
        Text(label, Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun VisualMarkdownBlock(
    block: MarkdownBlock,
    editorFont: FontFamily,
    onChange: (MarkdownBlock) -> Unit,
    onDelete: () -> Unit,
) {
    var editing by remember(block) { mutableStateOf(false) }
    val text = block.editableText()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = blockSurfaceColor(block),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (editing) 0.32f else 0.10f)),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            BlockGlyph(block)
            if (editing) {
                var value by remember(block) { mutableStateOf(TextFieldValue(text)) }
                BasicTextField(
                    value = value,
                    onValueChange = {
                        value = it
                        onChange(block.withEditableText(it.text))
                    },
                    textStyle = blockTextStyle(block, editorFont),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f).padding(top = 2.dp),
                )
            } else {
                Column(Modifier.weight(1f).clickable { editing = true }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    RenderVisualBlock(block, editorFont)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { editing = !editing }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.72f))
                }
            }
        }
    }
}

@Composable
private fun BlockGlyph(block: MarkdownBlock) {
    val icon = when (block) {
        is MarkdownBlock.Heading -> Icons.Outlined.Title
        is MarkdownBlock.ListItem -> if (block.checked == true) Icons.Outlined.CheckBox else Icons.Outlined.RadioButtonUnchecked
        is MarkdownBlock.Quote -> Icons.Outlined.FormatQuote
        is MarkdownBlock.Code -> Icons.Outlined.Code
        MarkdownBlock.Rule -> Icons.Outlined.Title
        is MarkdownBlock.Paragraph -> Icons.Outlined.DragIndicator
    }
    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp).padding(top = 2.dp))
}

@Composable
private fun RenderVisualBlock(block: MarkdownBlock, editorFont: FontFamily) {
    when (block) {
        is MarkdownBlock.Heading -> Text(block.text, fontWeight = FontWeight.Black, fontSize = when (block.level) { 1 -> 26.sp; 2 -> 22.sp; else -> 18.sp }, fontFamily = editorFont)
        is MarkdownBlock.Paragraph -> {
            val image = markdownImage(block.text)
            if (image != null) {
                AsyncImage(image.second, image.first, Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                Text(image.first.ifBlank { image.second }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            } else {
                Text(block.text, fontSize = 16.sp, lineHeight = 25.sp, fontFamily = editorFont)
            }
        }
        is MarkdownBlock.ListItem -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            Text(if (block.checked == true) "✓" else if (block.checked == false) "□" else "•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(block.text, fontSize = 15.sp, lineHeight = 23.sp, fontFamily = editorFont)
        }
        is MarkdownBlock.Quote -> Text(block.text, fontSize = 15.sp, lineHeight = 23.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = editorFont)
        is MarkdownBlock.Code -> Text(block.code, fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
        MarkdownBlock.Rule -> Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)))
    }
}

@Composable
private fun blockSurfaceColor(block: MarkdownBlock): Color = when (block) {
    is MarkdownBlock.Quote -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    is MarkdownBlock.Code -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f)
    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
}

@Composable
private fun blockTextStyle(block: MarkdownBlock, editorFont: FontFamily): TextStyle = when (block) {
    is MarkdownBlock.Heading -> TextStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, fontSize = when (block.level) { 1 -> 26.sp; 2 -> 22.sp; else -> 18.sp }, fontFamily = editorFont)
    is MarkdownBlock.Code -> TextStyle(color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
    else -> TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, lineHeight = 24.sp, fontFamily = editorFont)
}

private fun MarkdownBlock.editableText(): String = when (this) {
    is MarkdownBlock.Heading -> text
    is MarkdownBlock.Paragraph -> text
    is MarkdownBlock.ListItem -> text
    is MarkdownBlock.Quote -> text
    is MarkdownBlock.Code -> code
    MarkdownBlock.Rule -> "---"
}

private fun MarkdownBlock.withEditableText(text: String): MarkdownBlock = when (this) {
    is MarkdownBlock.Heading -> copy(text = text)
    is MarkdownBlock.Paragraph -> copy(text = text)
    is MarkdownBlock.ListItem -> copy(text = text)
    is MarkdownBlock.Quote -> copy(text = text)
    is MarkdownBlock.Code -> copy(code = text)
    MarkdownBlock.Rule -> this
}

private fun List<MarkdownBlock>.rebuildWith(index: Int, block: MarkdownBlock): String =
    mapIndexed { i, existing -> if (i == index) block else existing }.joinMarkdown()

private fun List<MarkdownBlock>.joinMarkdown(): String = joinToString("\n\n") { it.toMarkdown() }.trim()

private fun MarkdownBlock.toMarkdown(): String = when (this) {
    is MarkdownBlock.Heading -> "${"#".repeat(level.coerceIn(1, 6))} $text"
    is MarkdownBlock.Paragraph -> text
    is MarkdownBlock.ListItem -> when (checked) {
        true -> "- [x] $text"
        false -> "- [ ] $text"
        null -> "- $text"
    }
    is MarkdownBlock.Quote -> "> $text"
    is MarkdownBlock.Code -> "```text\n$code\n```"
    MarkdownBlock.Rule -> "---"
}

private fun markdownImage(text: String): Pair<String, String>? {
    val match = Regex("""!\[([^]]*)]\(([^)]+)\)""").find(text.trim()) ?: return null
    return match.groupValues[1] to match.groupValues[2]
}

private fun NoteEmbedItem.toOpenUri(): Uri {
    val target = target.trim()
    return if (type == NoteEmbedType.Link && !target.contains("://")) {
        Uri.parse("https://$target")
    } else {
        Uri.parse(target)
    }
}

private fun NoteEmbedItem.mimeType(): String = when (type) {
    NoteEmbedType.Image -> "image/*"
    NoteEmbedType.Video -> "video/*"
    NoteEmbedType.Audio -> "audio/*"
    NoteEmbedType.File -> "application/octet-stream"
    NoteEmbedType.Link -> "text/plain"
    NoteEmbedType.Canvas,
    NoteEmbedType.Task -> "text/plain"
}

@Composable
private fun EditorTopBar(
    note: Note,
    title: String,
    body: String,
    state: NotesUiState,
    viewModel: NotesViewModel,
    onPickAttachment: () -> Unit,
    onPickEmbed: () -> Unit,
    onPickCover: () -> Unit,
    maxLineWidth: androidx.compose.ui.unit.Dp,
    canUndo: Boolean,
    canRedo: Boolean,
    readOnlySource: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleReadOnly: () -> Unit,
    onSave: () -> Unit,
    onTitleChange: (String) -> Unit,
) {
    val editorFont = when (state.settings.editorFontFamily) {
        EditorFontFamily.Sans -> FontFamily.SansSerif
        EditorFontFamily.Serif -> FontFamily.Serif
    }

    Column(Modifier.fillMaxWidth().widthIn(max = maxLineWidth)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onSave(); viewModel.go(Destination.NotesHome) }) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back to notes")
            }
            Column(Modifier.weight(1f)) {
                BasicTextField(
                    value = title,
                    onValueChange = {
                        onTitleChange(it)
                        viewModel.updateNote(note, it, body)
                    },
                    textStyle = TextStyle(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = editorFont,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Text(
                    "Saved · ${note.bodyMarkdown.split(Regex("\\s+")).filter { it.isNotBlank() }.size} words",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
            IconButton(onClick = { viewModel.toggleStar(note) }) {
                Icon(Icons.Outlined.Star, null, tint = if (note.starred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { viewModel.togglePin(note) }) {
                Icon(Icons.Outlined.PushPin, null, tint = if (note.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { viewModel.toggleLock(note) }) {
                Icon(if (note.locked) Icons.Outlined.Lock else Icons.Outlined.LockOpen, null)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(Icons.AutoMirrored.Outlined.Undo, null, modifier = Modifier.size(19.dp))
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(Icons.AutoMirrored.Outlined.Redo, null, modifier = Modifier.size(19.dp))
            }
            FilterChip(
                state.editorMode == EditorMode.Page,
                { viewModel.setEditorMode(EditorMode.Page) },
                label = { Text("Page") },
                leadingIcon = { Icon(Icons.Outlined.Book, null, modifier = Modifier.size(18.dp)) },
            )
            FilterChip(
                state.editorMode == EditorMode.Edit,
                { viewModel.setEditorMode(EditorMode.Edit) },
                label = { Text("Source") },
                leadingIcon = { Icon(Icons.Outlined.Code, null, modifier = Modifier.size(18.dp)) },
            )
            FilterChip(
                state.editorMode == EditorMode.Preview,
                { viewModel.setEditorMode(EditorMode.Preview) },
                label = { Text("Preview") },
                leadingIcon = { Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(18.dp)) },
            )
            FilterChip(
                selected = readOnlySource,
                onClick = onToggleReadOnly,
                label = { Text(if (readOnlySource) "Read code" else "Write code") },
                leadingIcon = { Icon(Icons.Outlined.Code, null, modifier = Modifier.size(18.dp)) },
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onSave) { Text("Save") }
            if (state.editorMode == EditorMode.Preview) {
                IconButton(onClick = onPickCover) { Icon(Icons.Outlined.Image, null, modifier = Modifier.size(20.dp)) }
            }
            IconButton(onClick = { viewModel.archive(note) }) { Icon(Icons.Outlined.Archive, null, modifier = Modifier.size(20.dp)) }
            IconButton(onClick = { viewModel.delete(note) }) { Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)) }
        }
    }
}

@Composable
private fun NoteCoverBlock(
    note: Note,
    viewModel: NotesViewModel,
    onPickCover: () -> Unit,
    maxLineWidth: androidx.compose.ui.unit.Dp,
) {
    if (note.coverUri.isNullOrBlank()) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = maxLineWidth).clickable(onClick = onPickCover),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Image, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text("Add cover image or GIF", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    Surface(
        modifier = Modifier.fillMaxWidth().widthIn(max = maxLineWidth),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Box {
            AsyncImage(
                model = note.coverUri,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(150.dp),
                contentScale = ContentScale.Crop,
            )
            Row(
                Modifier.align(Alignment.BottomEnd).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)) {
                    Text("Change", Modifier.clickable(onClick = onPickCover).padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f)) {
                    Text("Remove", Modifier.clickable(onClick = viewModel::clearSelectedNoteCover).padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private enum class EditorAction { H1, H2, Bold, Italic, Bullet, Numbered, Checklist, Quote, Code, Link, Table, Divider }

@Composable
private fun EditorBottomToolbar(
    maxLineWidth: androidx.compose.ui.unit.Dp,
    imeVisible: Boolean,
    onCollapse: () -> Unit,
    onAttach: () -> Unit,
    onEmbed: () -> Unit,
    onImage: () -> Unit,
    onFormat: (EditorAction) -> Unit,
) {
    val scroll = rememberScrollState()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = maxLineWidth)
            .imePadding()
            .navigationBarsPadding()
            .horizontalScroll(scroll),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolbarButton(Icons.Outlined.DragIndicator, "Float") { onCollapse() }
            ToolbarDivider()
            ToolbarButton(Icons.Outlined.Title, "H1") { onFormat(EditorAction.H1) }
            ToolbarButton(Icons.Outlined.FormatSize, "H2") { onFormat(EditorAction.H2) }
            ToolbarDivider()
            ToolbarButton(Icons.Outlined.FormatBold, "Bold") { onFormat(EditorAction.Bold) }
            ToolbarButton(Icons.Outlined.FormatItalic, "Italic") { onFormat(EditorAction.Italic) }
            ToolbarDivider()
            ToolbarButton(Icons.AutoMirrored.Outlined.FormatListBulleted, "List") { onFormat(EditorAction.Bullet) }
            ToolbarButton(Icons.Outlined.FormatListNumbered, "Num") { onFormat(EditorAction.Numbered) }
            ToolbarButton(Icons.Outlined.CheckBox, "Task") { onFormat(EditorAction.Checklist) }
            ToolbarDivider()
            ToolbarButton(Icons.Outlined.FormatQuote, "Quote") { onFormat(EditorAction.Quote) }
            ToolbarButton(Icons.Outlined.Code, "Code") { onFormat(EditorAction.Code) }
            ToolbarButton(Icons.Outlined.Link, "Link") { onFormat(EditorAction.Link) }
            ToolbarButton(Icons.Outlined.TableChart, "Table") { onFormat(EditorAction.Table) }
            ToolbarButton(Icons.Outlined.Title, "Rule") { onFormat(EditorAction.Divider) }
            ToolbarDivider()
            ToolbarButton(Icons.Outlined.AttachFile, "Attach") { onAttach() }
            ToolbarButton(Icons.Outlined.PlayArrow, "Embed") { onEmbed() }
            ToolbarButton(Icons.Outlined.Image, "Image") { onImage() }
        }
    }
}

@Composable
private fun FloatingEditorToolbar(
    modifier: Modifier = Modifier,
    onDrag: (androidx.compose.ui.geometry.Offset) -> Unit,
    onExpand: () -> Unit,
    onFormat: (EditorAction) -> Unit,
) {
    Surface(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(androidx.compose.ui.geometry.Offset(dragAmount.x, dragAmount.y))
                }
            },
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 8.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            ToolbarButton(Icons.Outlined.DragIndicator, "Move") {}
            ToolbarButton(Icons.Outlined.FormatBold, "Bold") { onFormat(EditorAction.Bold) }
            ToolbarButton(Icons.Outlined.CheckBox, "Task") { onFormat(EditorAction.Checklist) }
            ToolbarButton(Icons.Outlined.Link, "Link") { onFormat(EditorAction.Link) }
            ToolbarButton(Icons.Outlined.Code, "Code") { onFormat(EditorAction.Code) }
            ToolbarButton(Icons.Outlined.Edit, "Full") { onExpand() }
        }
    }
}

@Composable
private fun ToolbarDivider() {
    Box(Modifier.width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))
}

/** Applies a markdown formatting action to the current selection/cursor. */
private fun applyMarkdown(value: TextFieldValue, action: EditorAction): TextFieldValue = when (action) {
    EditorAction.Bold -> wrap(value, "**")
    EditorAction.Italic -> wrap(value, "*")
    EditorAction.Code -> wrap(value, "`")
    EditorAction.Link -> link(value)
    EditorAction.H1 -> linePrefix(value, "# ")
    EditorAction.H2 -> linePrefix(value, "## ")
    EditorAction.Bullet -> linePrefix(value, "- ")
    EditorAction.Numbered -> linePrefix(value, "1. ")
    EditorAction.Checklist -> linePrefix(value, "- [ ] ")
    EditorAction.Quote -> linePrefix(value, "> ")
    EditorAction.Table -> insertBlock(value, "\n| Column | Detail |\n| --- | --- |\n|  |  |\n")
    EditorAction.Divider -> insertBlock(value, "\n---\n")
}

private fun wrap(v: TextFieldValue, token: String): TextFieldValue {
    val start = minOf(v.selection.start, v.selection.end)
    val end = maxOf(v.selection.start, v.selection.end)
    val selected = v.text.substring(start, end)
    val newText = v.text.substring(0, start) + token + selected + token + v.text.substring(end)
    val cursor = if (selected.isEmpty()) start + token.length else end + 2 * token.length
    return TextFieldValue(newText, TextRange(cursor))
}

private fun link(v: TextFieldValue): TextFieldValue {
    val start = minOf(v.selection.start, v.selection.end)
    val end = maxOf(v.selection.start, v.selection.end)
    val selected = v.text.substring(start, end).ifEmpty { "text" }
    val insert = "[$selected](url)"
    val newText = v.text.substring(0, start) + insert + v.text.substring(end)
    // Place cursor inside the (url) placeholder.
    val urlStart = start + insert.length - 4
    return TextFieldValue(newText, TextRange(urlStart, urlStart + 3))
}

private fun linePrefix(v: TextFieldValue, prefix: String): TextFieldValue {
    val cursor = v.selection.start
    val lineStart = if (cursor <= 0) 0 else v.text.lastIndexOf('\n', cursor - 1).let { if (it < 0) 0 else it + 1 }
    val newText = v.text.substring(0, lineStart) + prefix + v.text.substring(lineStart)
    return TextFieldValue(newText, TextRange(cursor + prefix.length))
}

private fun insertBlock(v: TextFieldValue, block: String): TextFieldValue {
    val start = minOf(v.selection.start, v.selection.end)
    val end = maxOf(v.selection.start, v.selection.end)
    val newText = v.text.substring(0, start) + block + v.text.substring(end)
    val cursor = start + block.length
    return TextFieldValue(newText, TextRange(cursor))
}

@Composable
private fun ToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Icon(icon, null, tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MarkdownPreview(note: Note, modifier: Modifier, onPickCover: () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accentHex = String.format("#%06X", 0xFFFFFF and MaterialTheme.colorScheme.primary.toArgb())
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(
            note.title,
            fontWeight = FontWeight.Black,
            fontSize = 26.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(10.dp))
        if (!note.coverUri.isNullOrBlank()) {
            AsyncImage(
                model = note.coverUri,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onPickCover),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.height(14.dp))
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onPickCover),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Image, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text("Add preview cover image or GIF", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        PreviewMediaStrip(note)
        // Full Markdown + LaTeX via WebView (marked.js + MathJax-SVG, offline).
        MarkdownWebView(
            markdown = note.bodyMarkdown,
            dark = dark,
            accentHex = accentHex,
            modifier = Modifier.fillMaxWidth().height(920.dp),
        )
    }
}

@Composable
private fun PreviewMediaStrip(note: Note) {
    val imageAttachments = note.attachments.filter { it.mimeType.startsWith("image/") || imageLikeUri(it.uri) }
    val imageEmbeds = note.embeds.filter { it.type == NoteEmbedType.Image || imageLikeUri(it.target) }
    val files = note.attachments.filterNot { it.mimeType.startsWith("image/") || imageLikeUri(it.uri) }
    val nonImageEmbeds = note.embeds.filterNot { it.type == NoteEmbedType.Image || imageLikeUri(it.target) }
    if (imageAttachments.isEmpty() && imageEmbeds.isEmpty() && files.isEmpty() && nonImageEmbeds.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 14.dp)) {
        imageAttachments.take(4).forEach { attachment ->
            PreviewImageCard(attachment.uri, attachment.displayName)
        }
        imageEmbeds.take(4).forEach { embed ->
            PreviewImageCard(embed.target, embed.title)
        }
        if (files.isNotEmpty() || nonImageEmbeds.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(files, key = { "preview-attachment-${it.id}" }) { attachment ->
                    AttachmentInlineCard(attachment)
                }
                items(nonImageEmbeds, key = { "preview-embed-${it.id}" }) { embed ->
                    EmbedCard(embed)
                }
            }
        }
    }
}

@Composable
private fun PreviewImageCard(uri: String, title: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
    ) {
        Column {
            AsyncImage(
                model = uri,
                contentDescription = title,
                modifier = Modifier.fillMaxWidth().height(220.dp),
                contentScale = ContentScale.Crop,
            )
            Text(
                title.ifBlank { "Image" },
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun imageLikeUri(value: String): Boolean {
    val clean = value.substringBefore('?').lowercase()
    return clean.endsWith(".png") || clean.endsWith(".jpg") || clean.endsWith(".jpeg") || clean.endsWith(".webp") || clean.endsWith(".gif")
}
