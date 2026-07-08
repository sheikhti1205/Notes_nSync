package com.notesnync.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notesnync.app.domain.Destination
import com.notesnync.app.domain.WorkspaceObject
import com.notesnync.app.domain.WorkspaceObjectType
import com.notesnync.app.ui.NotesUiState
import com.notesnync.app.ui.NotesViewModel
import com.notesnync.app.ui.components.EmptyNotes
import com.notesnync.app.ui.components.SearchField

private data class SearchResult(
    val title: String,
    val detail: String,
    val type: String,
    val icon: ImageVector,
    val weight: Int,
    val action: () -> Unit,
)

@Composable
fun SearchScreen(state: NotesUiState, viewModel: NotesViewModel) {
    val query = state.searchQuery.trim()
    val results = buildSearchResults(state, viewModel, query)
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Search", fontWeight = FontWeight.Black, fontSize = 30.sp)
        SearchField(state.searchQuery, viewModel::search, Modifier.padding(bottom = 2.dp))
        if (query.isBlank() && state.notes.isEmpty() && state.workspaceObjects.isEmpty()) {
            EmptyNotes(viewModel::createNote)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (query.isBlank()) {
                    item { SectionLabel("Recent workspace objects") }
                }
                items(results, key = { "${it.type}:${it.title}:${it.detail}" }) { result ->
                    SearchResultRow(result)
                }
            }
        }
    }
}

private fun buildSearchResults(state: NotesUiState, viewModel: NotesViewModel, query: String): List<SearchResult> {
    fun matches(vararg values: String): Boolean = query.isBlank() || values.any { it.contains(query, ignoreCase = true) }
    val objectResults = state.workspaceObjects
        .filter { matches(it.title, it.summary, it.tags, it.objectType.name) }
        .map { obj ->
            SearchResult(
                title = obj.title,
                detail = "${obj.objectType.name} · ${obj.summary.ifBlank { obj.tags.ifBlank { "Workspace object" } }}",
                type = obj.objectType.name,
                icon = iconFor(obj),
                weight = when {
                    obj.pinned -> 0
                    obj.objectType == WorkspaceObjectType.Note -> 1
                    else -> 3
                },
                action = { viewModel.openWorkspaceObject(obj) },
            )
        }
    val tagResults = state.tags
        .filter { matches(it.name, "tag") }
        .map {
            SearchResult(
                title = "#${it.name}",
                detail = "${state.notes.count { note -> note.tags.any { tag -> tag.name == it.name } }} notes",
                type = "Tag",
                icon = Icons.Outlined.Tag,
                weight = 4,
                action = { viewModel.go(Destination.Tags) },
            )
        }
    val settingsResults = listOf(
        "Appearance" to Destination.Settings,
        "Security & Vault" to Destination.Settings,
        "Sync settings" to Destination.Settings,
        "Backup & Import" to Destination.ImportExport,
        "Command Palette" to Destination.CommandPalette,
        "Knowledge Graph" to Destination.Graph,
    ).filter { matches(it.first, "settings", "command") }
        .map {
            SearchResult(
                title = it.first,
                detail = "Settings and command surface",
                type = "Command",
                icon = if (it.second == Destination.CommandPalette) Icons.Outlined.Code else Icons.Outlined.Settings,
                weight = 5,
                action = { viewModel.go(it.second) },
            )
        }
    return (objectResults + tagResults + settingsResults)
        .sortedWith(compareBy<SearchResult> { it.weight }.thenByDescending { it.title.contains(query, ignoreCase = true) }.thenBy { it.title.lowercase() })
        .take(80)
}

@Composable
private fun SearchResultRow(result: SearchResult) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = result.action),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(result.icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(result.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(result.detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(result.type, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Search, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(8.dp))
        Text(label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun iconFor(obj: WorkspaceObject): ImageVector = when (obj.objectType) {
    WorkspaceObjectType.Note -> Icons.Outlined.Folder
    WorkspaceObjectType.Task -> Icons.Outlined.Check
    WorkspaceObjectType.File -> Icons.Outlined.Folder
    WorkspaceObjectType.Canvas -> Icons.Outlined.GridView
    WorkspaceObjectType.ChatMessage -> Icons.Outlined.ChatBubbleOutline
    WorkspaceObjectType.DatabaseRow -> Icons.Outlined.GridView
    WorkspaceObjectType.Workspace -> Icons.Outlined.Folder
    WorkspaceObjectType.System -> Icons.Outlined.Settings
}
