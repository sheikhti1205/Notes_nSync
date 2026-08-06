package com.notesnync.app.domain

data class Note(
    val id: Long,
    val title: String,
    val bodyMarkdown: String,
    val notebookId: Long?,
    val pinned: Boolean,
    val starred: Boolean,
    val archived: Boolean,
    val locked: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val tags: List<Tag> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val embeds: List<NoteEmbedItem> = emptyList(),
    val coverUri: String? = null,
    val coverMimeType: String? = null,
)

data class Notebook(
    val id: Long,
    val name: String,
    val parentId: Long?,
    val color: Long,
    val sortOrder: Int,
)

data class Tag(
    val id: Long,
    val name: String,
    val color: Long,
)

data class Attachment(
    val id: Long,
    val noteId: Long,
    val displayName: String,
    val mimeType: String,
    val uri: String,
    val sizeBytes: Long,
)

data class NoteEmbedItem(
    val id: Long,
    val noteId: Long,
    val type: NoteEmbedType,
    val title: String,
    val target: String,
    val preview: String,
    val createdAt: Long,
)

enum class NoteEmbedType { Link, File, Image, Video, Audio, Canvas, Task }

data class TaskItem(
    val id: Long,
    val title: String,
    val description: String,
    val assignee: String,
    val status: TaskStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val priority: TaskPriority = TaskPriority.Normal,
    val dueAt: Long? = null,
    val labels: String = "",
    val attachmentName: String? = null,
    val attachmentMimeType: String? = null,
    val attachmentUri: String? = null,
    val attachmentSizeBytes: Long? = null,
    val taskBoardId: Long = 1,
    val taskColumnId: Long? = null,
    val sortOrder: Int = 0,
)

enum class TaskStatus { Todo, Doing, Done }
enum class TaskPriority { Low, Normal, High, Urgent }

data class TaskBoardItem(
    val id: Long,
    val name: String,
    val workspaceId: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

data class TaskColumnItem(
    val id: Long,
    val boardId: Long,
    val name: String,
    val status: TaskStatus?,
    val color: Long,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

data class ChatMessageItem(
    val id: Long,
    val authorUsername: String,
    val authorDisplayName: String,
    val body: String,
    val color: Long,
    val createdAt: Long,
    val system: Boolean = false,
    val attachmentName: String? = null,
    val attachmentMimeType: String? = null,
    val attachmentUri: String? = null,
    val attachmentSizeBytes: Long? = null,
)

data class CanvasNodeItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val type: CanvasNodeType,
    val x: Float,
    val y: Float,
    val color: Long,
    val linkedNoteId: Long?,
    val targetUri: String? = null,
    val targetMimeType: String? = null,
    val targetName: String? = null,
    val targetSizeBytes: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class CanvasNodeType { Text, Note, File, Shape, Link, Media }

data class CanvasEdgeItem(
    val id: Long,
    val fromNodeId: Long,
    val toNodeId: Long,
    val label: String,
    val color: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class WorkspaceObjectType { Note, Task, File, Canvas, ChatMessage, DatabaseRow, Workspace, System }

data class WorkspaceObject(
    val id: Long,
    val objectType: WorkspaceObjectType,
    val sourceId: Long?,
    val title: String,
    val summary: String,
    val tags: String,
    val icon: String,
    val color: Long,
    val pinned: Boolean,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class WorkspaceLinkType { Reference, Embed, Mention, Attachment, Related }

data class WorkspaceObjectLink(
    val id: Long,
    val fromObjectId: Long,
    val toObjectId: Long,
    val linkType: WorkspaceLinkType,
    val label: String,
    val createdAt: Long,
)

enum class WorkspaceActivityType { Created, Updated, Linked, Uploaded, Synced, Conflict, Commented, Completed }

data class WorkspaceActivity(
    val id: Long,
    val objectId: Long?,
    val activityType: WorkspaceActivityType,
    val actor: String,
    val title: String,
    val detail: String,
    val createdAt: Long,
)

enum class WorkspaceHistoryType { Created, Updated, Deleted, Restored, Moved, Linked, Attachment, Comment, Sync, Conflict }

data class WorkspaceObjectHistory(
    val id: Long,
    val objectId: Long?,
    val historyType: WorkspaceHistoryType,
    val actor: String,
    val summary: String,
    val beforeValue: String,
    val afterValue: String,
    val createdAt: Long,
)

data class WorkspaceComment(
    val id: Long,
    val objectId: Long,
    val authorUsername: String,
    val authorDisplayName: String,
    val body: String,
    val resolved: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

data class WorkspaceFileItem(
    val id: Long,
    val objectId: Long?,
    val displayName: String,
    val mimeType: String,
    val uri: String,
    val sizeBytes: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

data class Workspace(
    val id: Long,
    val name: String,
    val icon: String,
    val palette: ThemeProfile,
    val createdAt: Long,
    val iconKind: WorkspaceIconKind = WorkspaceIconKind.Text,
    val iconUri: String? = null,
    val backgroundUri: String? = null,
    // Per-workspace member permissions (mockup 12)
    val permRename: Boolean = false,
    val permChangeIcon: Boolean = false,
    val permInviteMembers: Boolean = false,
    val permDeleteNotes: Boolean = false,
    val permEditNotes: Boolean = true,
    val permCreateCanvas: Boolean = true,
    val permManageTasks: Boolean = true,
)

enum class WorkspaceIconKind { Text, Emoji, Image, Gif }
enum class ThemeMode { System, Light, Dark }
enum class ThemeProfile { Neon, Sunset, Ocean, Forest, Fire, Candy, Midnight, Gold }
enum class EditorLineWidth { Narrow, Comfortable, Wide }
enum class EditorFontFamily { Sans, Serif }
enum class NoteGestureAction { Actions, Pin, Star, Lock, Archive, Delete, None }
enum class SyncProvider { None, GoogleDrive, OneDrive, LocalFolder }
enum class SyncFolderAction { CreateChain, RestoreChain }

data class AppSettings(
    val themeMode: ThemeMode,
    val themeProfile: ThemeProfile,
    val accentColor: Long,
    val activeWorkspaceId: Long = 1,
    val backupFolderUri: String?,
    val vaultLockEnabled: Boolean,
    val vaultSecretHash: String?,
    val syncProvider: SyncProvider = SyncProvider.None,
    val syncFolderUri: String? = null,
    val syncChainId: String? = null,
    val syncDeviceName: String = "Android device",
    val syncUserName: String = "",
    val syncPublicName: String = "",
    val lastSyncAt: Long? = null,
    val lastSyncHash: String? = null,
    val lastSyncStatus: String = "Sync chain not configured",
    val syncConflictCount: Int = 0,
    val profileBackgroundUri: String? = null,
    val profileImageUri: String? = null,
    val workspaceName: String = "Team Notebook",
    val workspaceIcon: String = "N",
    val workspaceIconKind: WorkspaceIconKind = WorkspaceIconKind.Text,
    val workspaceIconUri: String? = null,
    val workspaceBackgroundUri: String? = null,
    val adminsControlWorkspaceVisuals: Boolean = true,
    val allowMembersCreateNotes: Boolean = true,
    val allowMembersInvite: Boolean = false,
    val uiScale: Float = 0.88f,
    val editorLineWidth: EditorLineWidth = EditorLineWidth.Comfortable,
    val editorFontFamily: EditorFontFamily = EditorFontFamily.Sans,
    val showMarkdownSyntax: Boolean = true,
    val noteLongPressAction: NoteGestureAction = NoteGestureAction.Actions,
    val noteSwipeStartAction: NoteGestureAction = NoteGestureAction.Pin,
    val noteSwipeEndAction: NoteGestureAction = NoteGestureAction.Archive,
    val blockScreenshots: Boolean = false,
    val requireBiometricOnOpen: Boolean = false,
    val reduceMotion: Boolean = false,
    // Appearance (persisted)
    val uiDensityCompact: Boolean = false,
    val appFont: String = "Inter",
    // Editor & Markdown (persisted)
    val defaultEditMode: Boolean = true,
    val editorFontSize: Float = 0.55f,
    val tabSize: Int = 4,
    val showLineNumbers: Boolean = true,
    val autoPairBrackets: Boolean = true,
    val syntaxColorful: Boolean = true,
    val autoConvertOnPaste: Boolean = true,
    // Security (persisted)
    val appLockOnExit: Boolean = false,
    val autoLockMinutes: Int = 5,
    // Backup (persisted)
    val autoBackup: Boolean = false,
    val backupFrequency: String = "Daily",
    // Sync engine (persisted)
    val autoSync: Boolean = true,
    val backgroundSync: Boolean = true,
    val syncIntervalMinutes: Int = 30,
    val syncOnMobileData: Boolean = false,
    val syncOnBatterySaver: Boolean = false,
    val notifyOnErrors: Boolean = true,
    val selectiveSync: Boolean = false,
    // Conflict resolution (persisted)
    val conflictDefaultAction: String = "Ask me",
    val autoMergeNonConflicting: Boolean = true,
    val keepBothCopies: Boolean = false,
)

data class SyncFolderRequest(
    val provider: SyncProvider,
    val action: SyncFolderAction,
    val secret: String,
    val username: String,
    val publicName: String,
    val deviceName: String,
)

enum class HomeTab { AllNotes, Pinned, Tags }
enum class Destination {
    WorkspaceHub,
    Inbox,
    Files,
    Database,
    Graph,
    ObjectDetail,
    Activity,
    Templates,
    CommandPalette,
    NotesHome,
    NoteEditor,
    Notebooks,
    Tags,
    Search,
    Tasks,
    Canvas,
    Chat,
    ConflictReview,
    SyncMonitor,
    Vault,
    Settings,
    ImportExport,
}
enum class EditorMode { Page, Edit, Preview }

data class NoteDocument(
    val title: String,
    val folder: String,
    val tag: String,
    val markdown: String,
)
