package com.notesnync.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.notesnync.app.branding.palette
import com.notesnync.app.data.GoogleDriveOAuth
import com.notesnync.app.domain.CanvasNodeItem
import com.notesnync.app.domain.Destination
import com.notesnync.app.domain.SyncFolderRequest
import com.notesnync.app.domain.TaskItem
import com.notesnync.app.ui.components.AnimatedFab
import com.notesnync.app.ui.components.ComposeLoadingScreen
import com.notesnync.app.ui.components.MobileBottomBar
import com.notesnync.app.ui.screens.LockScreen
import com.notesnync.app.ui.screens.NoteEditorScreen
import com.notesnync.app.ui.screens.NotebookScreen
import com.notesnync.app.ui.screens.NotesHome
import com.notesnync.app.ui.screens.ObjectDetailScreen
import com.notesnync.app.ui.screens.CanvasBoardScreen
import com.notesnync.app.ui.screens.ChatScreen
import com.notesnync.app.ui.screens.ConflictReviewScreen
import com.notesnync.app.ui.screens.SearchScreen
import com.notesnync.app.ui.screens.SettingsScreen
import com.notesnync.app.ui.screens.SectionSidebarOverlay
import com.notesnync.app.ui.screens.Sidebar
import com.notesnync.app.ui.screens.SyncMonitorScreen
import com.notesnync.app.ui.screens.TagsScreen
import com.notesnync.app.ui.screens.TasksBoardScreen
import com.notesnync.app.ui.screens.WorkspaceHubScreen
import com.notesnync.app.ui.screens.FilesLibraryScreen
import com.notesnync.app.ui.screens.DatabaseScreen
import com.notesnync.app.ui.screens.GraphScreen
import com.notesnync.app.ui.screens.ActivityScreen
import com.notesnync.app.ui.screens.TemplatesScreen
import com.notesnync.app.ui.screens.CommandPaletteScreen
import kotlinx.coroutines.delay
import net.openid.appauth.AuthorizationService

@Composable
fun NotesNyncRoot(viewModel: NotesViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val context = view.context
    val googleAuthService = remember(context) { AuthorizationService(context) }
    var loading by remember { mutableStateOf(true) }
    var pendingSyncRequest by remember { mutableStateOf<SyncFolderRequest?>(null) }
    val syncFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        viewModel.handleSyncFolderPicked(uri, pendingSyncRequest)
        pendingSyncRequest = null
    }
    val backupFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        viewModel.handleBackupFolderPicked(uri)
    }
    val markdownImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.importMarkdown(uri)
    }
    var pendingBackupImportSecret by remember { mutableStateOf("") }
    val backupImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.importBackupFromUri(pendingBackupImportSecret, uri)
        pendingBackupImportSecret = ""
    }
    val attachmentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.attachFileToSelectedNote(uri)
    }
    val embedLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.embedFileToSelectedNote(uri)
    }
    val coverLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.setSelectedNoteCover(uri)
    }
    val chatAttachmentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.sendChatAttachment(uri)
    }
    val workspaceFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.addWorkspaceFile(uri)
    }
    var pendingTaskAttachment by remember { mutableStateOf<TaskItem?>(null) }
    val taskAttachmentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingTaskAttachment?.let { viewModel.attachFileToTask(it, uri) }
        pendingTaskAttachment = null
    }
    var pendingCanvasTarget by remember { mutableStateOf<CanvasNodeItem?>(null) }
    val canvasTargetLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingCanvasTarget?.let { viewModel.attachTargetToCanvasNode(it, uri) }
        pendingCanvasTarget = null
    }
    val googleDriveAuthLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.handleGoogleDriveAuthorizationResult(result.data)
    }

    LaunchedEffect(Unit) {
        delay(2100)
        loading = false
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.autoSyncIfPossible()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(googleAuthService) {
        onDispose { googleAuthService.dispose() }
    }

    DisposableEffect(state.settings.blockScreenshots) {
        val window = (view.context as? Activity)?.window
        if (state.settings.blockScreenshots) {
            window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose { }
    }

    val biometricAuthenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
    val biometricAvailable = remember(context) {
        BiometricManager.from(context).canAuthenticate(biometricAuthenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }
    fun authenticateBiometric() {
        val activity = context.findFragmentActivity()
        if (activity == null) {
            viewModel.showMessage("Biometric unlock is unavailable")
            return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.unlockWithBiometric()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    viewModel.showMessage(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    viewModel.showMessage("Biometric check failed")
                }
            },
        )
        runCatching {
            prompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Notes'nync")
                    .setSubtitle("Use your fingerprint or face to unlock the vault")
                    .setNegativeButtonText("Use password")
                    .setAllowedAuthenticators(biometricAuthenticators)
                    .build(),
            )
        }.onFailure {
            viewModel.showMessage(it.localizedMessage ?: "Biometric unlock failed")
        }
    }

    NotesNyncTheme(state.settings) {
        Surface(Modifier.fillMaxSize()) {
            if (state.diagnostics.pendingCrashPrompt && state.diagnostics.askBeforeSharing && !loading) {
                AlertDialog(
                    onDismissRequest = viewModel::acknowledgeCrashPrompt,
                    title = { Text("Notes'nync noticed a crash") },
                    text = { Text("A local diagnostics log is ready. You can share it through your email or any app from the share sheet.") },
                    confirmButton = { Button(onClick = viewModel::shareDiagnostics) { Text("Share log") } },
                    dismissButton = { TextButton(onClick = viewModel::acknowledgeCrashPrompt) { Text("Not now") } },
                )
            }
            when {
                loading -> ComposeLoadingScreen(palette = state.settings.themeProfile.palette())
                state.locked -> LockScreen(
                    biometricEnabled = state.settings.requireBiometricOnOpen,
                    biometricAvailable = biometricAvailable,
                    onUnlock = viewModel::unlock,
                    onBiometricUnlock = ::authenticateBiometric,
                )
                else -> NotesApp(
                    state = state,
                    viewModel = viewModel,
                    onPickSyncFolder = { request ->
                        pendingSyncRequest = request
                        syncFolderLauncher.launch(null)
                    },
                    onPickMarkdown = { markdownImportLauncher.launch(arrayOf("text/*", "text/markdown", "application/octet-stream")) },
                    onPickAttachment = { attachmentLauncher.launch(arrayOf("*/*")) },
                    onPickEmbed = { embedLauncher.launch(arrayOf("*/*")) },
                    onPickCover = { coverLauncher.launch(arrayOf("image/*")) },
                    onPickChatAttachment = { chatAttachmentLauncher.launch(arrayOf("*/*")) },
                    onPickWorkspaceFile = { workspaceFileLauncher.launch(arrayOf("*/*")) },
                    onPickTaskAttachment = { task ->
                        pendingTaskAttachment = task
                        taskAttachmentLauncher.launch(arrayOf("*/*"))
                    },
                    onPickCanvasTarget = { node ->
                        pendingCanvasTarget = node
                        canvasTargetLauncher.launch(
                            when (node.type.name) {
                                "Media" -> arrayOf("image/*", "video/*", "audio/*")
                                else -> arrayOf("*/*")
                            },
                        )
                    },
                    onPickBackupFolder = { backupFolderLauncher.launch(null) },
                    onPickBackupFile = { secret ->
                        pendingBackupImportSecret = secret
                        backupImportLauncher.launch(arrayOf("application/octet-stream", "text/*", "*/*"))
                    },
                    onConnectGoogleDrive = {
                        googleDriveAuthLauncher.launch(googleAuthService.getAuthorizationRequestIntent(GoogleDriveOAuth.authorizationRequest()))
                    },
                )
            }
        }
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

@Composable
private fun NotesApp(
    state: NotesUiState,
    viewModel: NotesViewModel,
    onPickSyncFolder: (SyncFolderRequest) -> Unit,
    onPickMarkdown: () -> Unit,
    onPickAttachment: () -> Unit,
    onPickEmbed: () -> Unit,
    onPickCover: () -> Unit,
    onPickChatAttachment: () -> Unit,
    onPickWorkspaceFile: () -> Unit,
    onPickTaskAttachment: (TaskItem) -> Unit,
    onPickCanvasTarget: (CanvasNodeItem) -> Unit,
    onPickBackupFolder: () -> Unit,
    onPickBackupFile: (String) -> Unit,
    onConnectGoogleDrive: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    BackHandler { viewModel.handleBack() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 720.dp
        val medium = maxWidth in 720.dp..1040.dp
        val density = LocalDensity.current
        val imeVisible = WindowInsets.ime.getBottom(density) > 0
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = { if (compact && !imeVisible) MobileBottomBar(state, viewModel) },
            floatingActionButton = {
                if (compact && !imeVisible && state.destination in listOf(Destination.WorkspaceHub, Destination.NotesHome, Destination.Tasks, Destination.Canvas, Destination.Files)) {
                    AnimatedFab {
                        when (state.destination) {
                            Destination.Tasks -> viewModel.createTaskAndOpen()
                            Destination.Canvas -> viewModel.createCanvasAndOpen()
                            Destination.Files -> onPickWorkspaceFile()
                            else -> viewModel.createNote()
                        }
                    }
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .statusBarsPadding()
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                            ),
                        ),
                    ),
            ) {
                when {
                    compact -> CompactContent(state, viewModel, onPickSyncFolder, onPickMarkdown, onPickAttachment, onPickEmbed, onPickCover, onPickChatAttachment, onPickWorkspaceFile, onPickTaskAttachment, onPickCanvasTarget, onPickBackupFolder, onPickBackupFile, onConnectGoogleDrive)
                    medium -> MediumContent(state, viewModel, onPickSyncFolder, onPickMarkdown, onPickAttachment, onPickEmbed, onPickCover, onPickChatAttachment, onPickWorkspaceFile, onPickTaskAttachment, onPickCanvasTarget, onPickBackupFolder, onPickBackupFile, onConnectGoogleDrive)
                    else -> ExpandedContent(state, viewModel, onPickSyncFolder, onPickMarkdown, onPickAttachment, onPickEmbed, onPickCover, onPickChatAttachment, onPickWorkspaceFile, onPickTaskAttachment, onPickCanvasTarget, onPickBackupFolder, onPickBackupFile, onConnectGoogleDrive)
                }
                if (compact && state.sidebarOpen) {
                    SectionSidebarOverlay(state, viewModel)
                }
            }
        }
    }
}

@Composable
private fun CompactContent(
    state: NotesUiState,
    viewModel: NotesViewModel,
    onPickSyncFolder: (SyncFolderRequest) -> Unit,
    onPickMarkdown: () -> Unit,
    onPickAttachment: () -> Unit,
    onPickEmbed: () -> Unit,
    onPickCover: () -> Unit,
    onPickChatAttachment: () -> Unit,
    onPickWorkspaceFile: () -> Unit,
    onPickTaskAttachment: (TaskItem) -> Unit,
    onPickCanvasTarget: (CanvasNodeItem) -> Unit,
    onPickBackupFolder: () -> Unit,
    onPickBackupFile: (String) -> Unit,
    onConnectGoogleDrive: () -> Unit,
) {
    DestinationContent(state, viewModel, Modifier.fillMaxSize(), onPickSyncFolder, onPickMarkdown, onPickAttachment, onPickEmbed, onPickCover, onPickChatAttachment, onPickWorkspaceFile, onPickTaskAttachment, onPickCanvasTarget, onPickBackupFolder, onPickBackupFile, onConnectGoogleDrive)
}

@Composable
private fun DestinationContent(
    state: NotesUiState,
    viewModel: NotesViewModel,
    modifier: Modifier,
    onPickSyncFolder: (SyncFolderRequest) -> Unit,
    onPickMarkdown: () -> Unit,
    onPickAttachment: () -> Unit,
    onPickEmbed: () -> Unit,
    onPickCover: () -> Unit,
    onPickChatAttachment: () -> Unit,
    onPickWorkspaceFile: () -> Unit,
    onPickTaskAttachment: (TaskItem) -> Unit,
    onPickCanvasTarget: (CanvasNodeItem) -> Unit,
    onPickBackupFolder: () -> Unit,
    onPickBackupFile: (String) -> Unit,
    onConnectGoogleDrive: () -> Unit,
) {
    Box(modifier) {
        Crossfade(targetState = state.destination, label = "destination-crossfade") { destination ->
            when (destination) {
                Destination.NoteEditor -> NoteEditorScreen(state, viewModel, Modifier.fillMaxSize(), onPickAttachment, onPickEmbed, onPickCover)
                Destination.WorkspaceHub -> WorkspaceHubScreen(state, viewModel)
                Destination.Inbox -> WorkspaceHubScreen(state, viewModel, inboxMode = true)
                Destination.Files -> FilesLibraryScreen(state, viewModel, onPickWorkspaceFile)
                Destination.Database -> DatabaseScreen(state, viewModel)
                Destination.Graph -> GraphScreen(state, viewModel)
                Destination.ObjectDetail -> ObjectDetailScreen(state, viewModel, Modifier.fillMaxSize())
                Destination.Activity -> ActivityScreen(state, viewModel)
                Destination.Templates -> TemplatesScreen(state, viewModel)
                Destination.CommandPalette -> CommandPaletteScreen(state, viewModel)
                Destination.Notebooks -> NotebookScreen(state, viewModel)
                Destination.Tags -> TagsScreen(state, viewModel)
                Destination.Search -> SearchScreen(state, viewModel)
                Destination.Tasks -> TasksBoardScreen(state, viewModel, onPickTaskAttachment)
                Destination.Canvas -> CanvasBoardScreen(state, viewModel, onPickCanvasTarget)
                Destination.Chat -> ChatScreen(state, viewModel, onPickChatAttachment)
                Destination.ConflictReview -> ConflictReviewScreen(state, viewModel)
                Destination.SyncMonitor -> SyncMonitorScreen(state, viewModel)
                Destination.Settings, Destination.Vault, Destination.ImportExport -> SettingsScreen(state, viewModel, onPickSyncFolder, onPickMarkdown, onPickBackupFolder, onPickBackupFile, onConnectGoogleDrive)
                Destination.NotesHome -> NotesHome(state, viewModel, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun MediumContent(
    state: NotesUiState,
    viewModel: NotesViewModel,
    onPickSyncFolder: (SyncFolderRequest) -> Unit,
    onPickMarkdown: () -> Unit,
    onPickAttachment: () -> Unit,
    onPickEmbed: () -> Unit,
    onPickCover: () -> Unit,
    onPickChatAttachment: () -> Unit,
    onPickWorkspaceFile: () -> Unit,
    onPickTaskAttachment: (TaskItem) -> Unit,
    onPickCanvasTarget: (CanvasNodeItem) -> Unit,
    onPickBackupFolder: () -> Unit,
    onPickBackupFile: (String) -> Unit,
    onConnectGoogleDrive: () -> Unit,
) {
    if (state.destination == Destination.NotesHome || state.destination == Destination.NoteEditor) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            NotesHome(state, viewModel, Modifier.weight(0.42f).fillMaxHeight(), showHeader = false)
            NoteEditorScreen(state, viewModel, Modifier.weight(0.58f).fillMaxHeight(), onPickAttachment, onPickEmbed, onPickCover)
        }
    } else {
        DestinationContent(state, viewModel, Modifier.fillMaxSize(), onPickSyncFolder, onPickMarkdown, onPickAttachment, onPickEmbed, onPickCover, onPickChatAttachment, onPickWorkspaceFile, onPickTaskAttachment, onPickCanvasTarget, onPickBackupFolder, onPickBackupFile, onConnectGoogleDrive)
    }
}

@Composable
private fun ExpandedContent(
    state: NotesUiState,
    viewModel: NotesViewModel,
    onPickSyncFolder: (SyncFolderRequest) -> Unit,
    onPickMarkdown: () -> Unit,
    onPickAttachment: () -> Unit,
    onPickEmbed: () -> Unit,
    onPickCover: () -> Unit,
    onPickChatAttachment: () -> Unit,
    onPickWorkspaceFile: () -> Unit,
    onPickTaskAttachment: (TaskItem) -> Unit,
    onPickCanvasTarget: (CanvasNodeItem) -> Unit,
    onPickBackupFolder: () -> Unit,
    onPickBackupFile: (String) -> Unit,
    onConnectGoogleDrive: () -> Unit,
) {
    Row(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 6.dp)) {
        Sidebar(state, viewModel, Modifier.width(230.dp).fillMaxHeight())
        if (state.destination == Destination.NotesHome || state.destination == Destination.NoteEditor) {
            NotesHome(state, viewModel, Modifier.width(370.dp).fillMaxHeight(), showHeader = false)
            NoteEditorScreen(state, viewModel, Modifier.weight(1f).fillMaxHeight(), onPickAttachment, onPickEmbed, onPickCover)
        } else {
            DestinationContent(state, viewModel, Modifier.weight(1f).fillMaxHeight(), onPickSyncFolder, onPickMarkdown, onPickAttachment, onPickEmbed, onPickCover, onPickChatAttachment, onPickWorkspaceFile, onPickTaskAttachment, onPickCanvasTarget, onPickBackupFolder, onPickBackupFile, onConnectGoogleDrive)
        }
    }
}
