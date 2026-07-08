package com.notesnync.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notesnync.app.branding.NotesNyncCutoutLogo
import com.notesnync.app.branding.palette
import com.notesnync.app.domain.Destination
import com.notesnync.app.domain.ThemeMode
import com.notesnync.app.ui.NotesUiState
import com.notesnync.app.ui.NotesViewModel
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileTopBar(state: NotesUiState, viewModel: NotesViewModel) {
    CenterAlignedTopAppBar(
        title = {
            Text(destinationTitle(state.destination), fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1)
        },
        navigationIcon = { IconButton(onClick = viewModel::toggleSidebar) { Icon(Icons.Outlined.Menu, null) } },
        actions = { IconButton(onClick = { viewModel.go(Destination.Search) }) { Icon(Icons.Outlined.Search, null) } },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.94f)),
    )
}

private fun destinationTitle(d: Destination): String = when (d) {
    Destination.WorkspaceHub -> "Home"
    Destination.Inbox -> "Inbox"
    Destination.Files -> "Files"
    Destination.Database -> "Database"
    Destination.Graph -> "Graph"
    Destination.ObjectDetail -> "Object"
    Destination.Activity -> "Activity"
    Destination.Templates -> "Templates"
    Destination.CommandPalette -> "Command"
    Destination.NotesHome -> "Notes"
    Destination.NoteEditor -> "Editor"
    Destination.Notebooks -> "Notebooks"
    Destination.Tags -> "Tags"
    Destination.Search -> "Search"
    Destination.Tasks -> "Tasks"
    Destination.Canvas -> "Canvas"
    Destination.Chat -> "Chat"
    Destination.ConflictReview -> "Conflicts"
    Destination.SyncMonitor -> "Sync"
    Destination.Vault -> "Vault"
    Destination.Settings -> "Settings"
    Destination.ImportExport -> "Import / Export"
}

@Composable
fun DesktopTopBar(state: NotesUiState, viewModel: NotesViewModel) {
    Row(
        Modifier.fillMaxWidth().padding(WindowInsets.statusBars.asPaddingValues()).padding(18.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        NotesNyncCutoutLogo(
            size = 44.dp,
        )
        Column(Modifier.weight(1f)) {
            Text("Notes’nync", fontWeight = FontWeight.Black, fontSize = 22.sp)
            Text("${state.notes.size} notes · ${state.tasks.size} tasks · local vault", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        SearchField(state.searchQuery, viewModel::search, Modifier.width(320.dp))
        IconButton(onClick = { viewModel.setTheme(if (state.settings.themeMode == ThemeMode.Dark) ThemeMode.Light else ThemeMode.Dark) }) {
            Icon(if (state.settings.themeMode == ThemeMode.Dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode, null)
        }
        IconButton(onClick = viewModel::lock) { Icon(Icons.Outlined.Lock, null) }
    }
}

@Composable
fun MobileBottomBar(state: NotesUiState, viewModel: NotesViewModel) {
    // A floating pill that sizes to its content and sits above the system gesture bar,
    // so labels are never clipped by the navigation-bar inset.
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 10.dp,
            tonalElevation = 2.dp,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavBarItem("Menu", Icons.Outlined.Menu, state.sidebarOpen, Modifier.weight(1f), viewModel::toggleSidebar)
                NavBarItem("Home", Icons.Outlined.Home, state.destination == Destination.WorkspaceHub, Modifier.weight(1f)) { viewModel.go(Destination.WorkspaceHub) }
                NavBarItem("Notes", Icons.Outlined.Folder, state.destination == Destination.NotesHome, Modifier.weight(1f)) { viewModel.go(Destination.NotesHome) }
                NavBarItem("Tasks", Icons.Outlined.Check, state.destination == Destination.Tasks, Modifier.weight(1f)) { viewModel.go(Destination.Tasks) }
                NavBarItem("Canvas", Icons.Outlined.GridView, state.destination == Destination.Canvas, Modifier.weight(1f)) { viewModel.go(Destination.Canvas) }
                NavBarItem("Chat", Icons.Outlined.ChatBubbleOutline, state.destination == Destination.Chat, Modifier.weight(1f)) { viewModel.go(Destination.Chat) }
            }
        }
    }
}
