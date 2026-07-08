package com.notesnync.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        WorkspaceEntity::class,
        NoteEntity::class,
        NotebookEntity::class,
        TagEntity::class,
        AttachmentEntity::class,
        NoteEmbedEntity::class,
        NoteTagCrossRef::class,
        TaskBoardEntity::class,
        TaskColumnEntity::class,
        TaskEntity::class,
        ChatMessageEntity::class,
        CanvasNodeEntity::class,
        CanvasEdgeEntity::class,
        WorkspaceObjectEntity::class,
        WorkspaceObjectLinkEntity::class,
        WorkspaceActivityEntity::class,
        WorkspaceObjectHistoryEntity::class,
        WorkspaceCommentEntity::class,
        WorkspaceFileEntity::class,
        AppSettingsEntity::class,
    ],
    version = 17,
    exportSchema = true,
)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun dao(): NotesDao

    companion object {
        @Volatile private var instance: NotesDatabase? = null

        fun get(context: Context): NotesDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, NotesDatabase::class.java, "notesnync.db")
                .addMigrations(
                    MIGRATION_1_12,
                    MIGRATION_2_12,
                    MIGRATION_3_12,
                    MIGRATION_4_12,
                    MIGRATION_5_12,
                    MIGRATION_6_12,
                    MIGRATION_7_12,
                    MIGRATION_8_12,
                    MIGRATION_9_12,
                    MIGRATION_10_12,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_16,
                    MIGRATION_15_16,
                    MIGRATION_16_17,
                )
                .build()
                .also { instance = it }
        }

        private val MIGRATION_1_12 = schemaMigration(1, 12)
        private val MIGRATION_2_12 = schemaMigration(2, 12)
        private val MIGRATION_3_12 = schemaMigration(3, 12)
        private val MIGRATION_4_12 = schemaMigration(4, 12)
        private val MIGRATION_5_12 = schemaMigration(5, 12)
        private val MIGRATION_6_12 = schemaMigration(6, 12)
        private val MIGRATION_7_12 = schemaMigration(7, 12)
        private val MIGRATION_8_12 = schemaMigration(8, 12)
        private val MIGRATION_9_12 = schemaMigration(9, 12)
        private val MIGRATION_10_12 = schemaMigration(10, 12)
        private val MIGRATION_11_12 = schemaMigration(11, 12)
        private val MIGRATION_12_13 = schemaMigration(12, 13)
        private val MIGRATION_13_14 = schemaMigration(13, 14)
        private val MIGRATION_14_16 = schemaMigration(14, 16)
        private val MIGRATION_15_16 = schemaMigration(15, 16)
        private val MIGRATION_16_17 = schemaMigration(16, 17)

        private fun schemaMigration(from: Int, to: Int) = object : Migration(from, to) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.ensureCurrentSchema()
            }
        }

        private fun SupportSQLiteDatabase.ensureCurrentSchema() {
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS workspaces (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    icon TEXT NOT NULL DEFAULT 'N',
                    iconKind TEXT NOT NULL DEFAULT 'Text',
                    iconUri TEXT,
                    backgroundUri TEXT,
                    palette TEXT NOT NULL DEFAULT 'Neon',
                    createdAt INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS notebooks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    parentId INTEGER,
                    color INTEGER NOT NULL,
                    sortOrder INTEGER NOT NULL,
                    workspaceId INTEGER NOT NULL DEFAULT 1
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS tags (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    color INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS notes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    bodyMarkdown TEXT NOT NULL,
                    notebookId INTEGER,
                    coverUri TEXT,
                    coverMimeType TEXT,
                    pinned INTEGER NOT NULL,
                    starred INTEGER NOT NULL,
                    archived INTEGER NOT NULL,
                    locked INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    workspaceId INTEGER NOT NULL DEFAULT 1,
                    FOREIGN KEY(notebookId) REFERENCES notebooks(id) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS attachments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    noteId INTEGER NOT NULL,
                    displayName TEXT NOT NULL,
                    mimeType TEXT NOT NULL,
                    uri TEXT NOT NULL,
                    sizeBytes INTEGER NOT NULL,
                    FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS note_embeds (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    noteId INTEGER NOT NULL,
                    type TEXT NOT NULL,
                    title TEXT NOT NULL,
                    target TEXT NOT NULL,
                    preview TEXT NOT NULL DEFAULT '',
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS note_tags (
                    noteId INTEGER NOT NULL,
                    tagId INTEGER NOT NULL,
                    PRIMARY KEY(noteId, tagId),
                    FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(tagId) REFERENCES tags(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS task_boards (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    workspaceId INTEGER NOT NULL DEFAULT 1,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS task_columns (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    boardId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    status TEXT,
                    color INTEGER NOT NULL,
                    sortOrder INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL DEFAULT '',
                    assignee TEXT NOT NULL DEFAULT '',
                    status TEXT NOT NULL DEFAULT 'Todo',
                    priority TEXT NOT NULL DEFAULT 'Normal',
                    dueAt INTEGER,
                    labels TEXT NOT NULL DEFAULT '',
                    attachmentName TEXT,
                    attachmentMimeType TEXT,
                    attachmentUri TEXT,
                    attachmentSizeBytes INTEGER,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    workspaceId INTEGER NOT NULL DEFAULT 1,
                    taskBoardId INTEGER NOT NULL DEFAULT 1,
                    taskColumnId INTEGER,
                    sortOrder INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS chat_messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    authorUsername TEXT NOT NULL,
                    authorDisplayName TEXT NOT NULL,
                    body TEXT NOT NULL,
                    color INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    system INTEGER NOT NULL DEFAULT 0,
                    attachmentName TEXT,
                    attachmentMimeType TEXT,
                    attachmentUri TEXT,
                    attachmentSizeBytes INTEGER,
                    workspaceId INTEGER NOT NULL DEFAULT 1
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS canvas_nodes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    subtitle TEXT NOT NULL,
                    type TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    color INTEGER NOT NULL,
                    linkedNoteId INTEGER,
                    targetUri TEXT,
                    targetMimeType TEXT,
                    targetName TEXT,
                    targetSizeBytes INTEGER,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    workspaceId INTEGER NOT NULL DEFAULT 1
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS canvas_edges (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    fromNodeId INTEGER NOT NULL,
                    toNodeId INTEGER NOT NULL,
                    label TEXT NOT NULL DEFAULT '',
                    color INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    workspaceId INTEGER NOT NULL DEFAULT 1
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS workspace_objects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    objectType TEXT NOT NULL,
                    sourceId INTEGER,
                    title TEXT NOT NULL,
                    summary TEXT NOT NULL DEFAULT '',
                    tags TEXT NOT NULL DEFAULT '',
                    icon TEXT NOT NULL DEFAULT '',
                    color INTEGER NOT NULL DEFAULT 4287327478,
                    pinned INTEGER NOT NULL DEFAULT 0,
                    archived INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    workspaceId INTEGER NOT NULL DEFAULT 1
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS workspace_object_links (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    fromObjectId INTEGER NOT NULL,
                    toObjectId INTEGER NOT NULL,
                    linkType TEXT NOT NULL DEFAULT 'Reference',
                    label TEXT NOT NULL DEFAULT '',
                    createdAt INTEGER NOT NULL,
                    workspaceId INTEGER NOT NULL DEFAULT 1
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS workspace_activities (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    objectId INTEGER,
                    activityType TEXT NOT NULL,
                    actor TEXT NOT NULL,
                    title TEXT NOT NULL,
                    detail TEXT NOT NULL DEFAULT '',
                    createdAt INTEGER NOT NULL,
                    workspaceId INTEGER NOT NULL DEFAULT 1
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS workspace_object_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    objectId INTEGER,
                    historyType TEXT NOT NULL,
                    actor TEXT NOT NULL,
                    summary TEXT NOT NULL,
                    beforeValue TEXT NOT NULL DEFAULT '',
                    afterValue TEXT NOT NULL DEFAULT '',
                    createdAt INTEGER NOT NULL,
                    workspaceId INTEGER NOT NULL DEFAULT 1
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS workspace_comments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    objectId INTEGER NOT NULL,
                    authorUsername TEXT NOT NULL,
                    authorDisplayName TEXT NOT NULL,
                    body TEXT NOT NULL,
                    resolved INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    workspaceId INTEGER NOT NULL DEFAULT 1
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS workspace_files (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    objectId INTEGER,
                    displayName TEXT NOT NULL,
                    mimeType TEXT NOT NULL,
                    uri TEXT NOT NULL,
                    sizeBytes INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    workspaceId INTEGER NOT NULL DEFAULT 1
                )
                """.trimIndent(),
            )
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS settings (
                    id INTEGER NOT NULL,
                    themeMode TEXT NOT NULL DEFAULT 'System',
                    themeProfile TEXT NOT NULL DEFAULT 'Neon',
                    accentColor INTEGER NOT NULL DEFAULT 4287327478,
                    activeWorkspaceId INTEGER NOT NULL DEFAULT 1,
                    backupFolderUri TEXT,
                    vaultLockEnabled INTEGER NOT NULL DEFAULT 0,
                    vaultSecretHash TEXT,
                    syncProvider TEXT NOT NULL DEFAULT 'None',
                    syncFolderUri TEXT,
                    syncChainId TEXT,
                    syncDeviceName TEXT NOT NULL DEFAULT 'Android device',
                    syncUserName TEXT NOT NULL DEFAULT '',
                    syncPublicName TEXT NOT NULL DEFAULT '',
                    lastSyncAt INTEGER,
                    lastSyncHash TEXT,
                    lastSyncStatus TEXT NOT NULL DEFAULT 'Sync chain not configured',
                    syncConflictCount INTEGER NOT NULL DEFAULT 0,
                    profileBackgroundUri TEXT,
                    profileImageUri TEXT,
                    workspaceName TEXT NOT NULL DEFAULT 'Team Notebook',
                    workspaceIcon TEXT NOT NULL DEFAULT 'N',
                    workspaceIconKind TEXT NOT NULL DEFAULT 'Text',
                    workspaceIconUri TEXT,
                    workspaceBackgroundUri TEXT,
                    adminsControlWorkspaceVisuals INTEGER NOT NULL DEFAULT 1,
                    allowMembersCreateNotes INTEGER NOT NULL DEFAULT 1,
                    allowMembersInvite INTEGER NOT NULL DEFAULT 0,
                    uiScale REAL NOT NULL DEFAULT 0.88,
                    editorLineWidth TEXT NOT NULL DEFAULT 'Comfortable',
                    editorFontFamily TEXT NOT NULL DEFAULT 'Sans',
                    showMarkdownSyntax INTEGER NOT NULL DEFAULT 1,
                    noteLongPressAction TEXT NOT NULL DEFAULT 'Actions',
                    noteSwipeStartAction TEXT NOT NULL DEFAULT 'Pin',
                    noteSwipeEndAction TEXT NOT NULL DEFAULT 'Archive',
                    blockScreenshots INTEGER NOT NULL DEFAULT 0,
                    requireBiometricOnOpen INTEGER NOT NULL DEFAULT 0,
                    reduceMotion INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )

            addColumnIfMissing("workspaces", "icon", "TEXT NOT NULL DEFAULT 'N'")
            addColumnIfMissing("workspaces", "iconKind", "TEXT NOT NULL DEFAULT 'Text'")
            addColumnIfMissing("workspaces", "iconUri", "TEXT")
            addColumnIfMissing("workspaces", "backgroundUri", "TEXT")
            addColumnIfMissing("workspaces", "palette", "TEXT NOT NULL DEFAULT 'Neon'")
            addColumnIfMissing("notebooks", "workspaceId", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing("notes", "coverUri", "TEXT")
            addColumnIfMissing("notes", "coverMimeType", "TEXT")
            addColumnIfMissing("notes", "workspaceId", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing("tasks", "description", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing("tasks", "assignee", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing("tasks", "status", "TEXT NOT NULL DEFAULT 'Todo'")
            addColumnIfMissing("tasks", "priority", "TEXT NOT NULL DEFAULT 'Normal'")
            addColumnIfMissing("tasks", "dueAt", "INTEGER")
            addColumnIfMissing("tasks", "labels", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing("tasks", "attachmentName", "TEXT")
            addColumnIfMissing("tasks", "attachmentMimeType", "TEXT")
            addColumnIfMissing("tasks", "attachmentUri", "TEXT")
            addColumnIfMissing("tasks", "attachmentSizeBytes", "INTEGER")
            addColumnIfMissing("tasks", "workspaceId", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing("tasks", "taskBoardId", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing("tasks", "taskColumnId", "INTEGER")
            addColumnIfMissing("tasks", "sortOrder", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing("chat_messages", "system", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing("chat_messages", "attachmentName", "TEXT")
            addColumnIfMissing("chat_messages", "attachmentMimeType", "TEXT")
            addColumnIfMissing("chat_messages", "attachmentUri", "TEXT")
            addColumnIfMissing("chat_messages", "attachmentSizeBytes", "INTEGER")
            addColumnIfMissing("chat_messages", "workspaceId", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing("canvas_nodes", "linkedNoteId", "INTEGER")
            addColumnIfMissing("canvas_nodes", "targetUri", "TEXT")
            addColumnIfMissing("canvas_nodes", "targetMimeType", "TEXT")
            addColumnIfMissing("canvas_nodes", "targetName", "TEXT")
            addColumnIfMissing("canvas_nodes", "targetSizeBytes", "INTEGER")
            addColumnIfMissing("canvas_nodes", "workspaceId", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing("settings", "themeProfile", "TEXT NOT NULL DEFAULT 'Neon'")
            addColumnIfMissing("settings", "activeWorkspaceId", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing("settings", "syncProvider", "TEXT NOT NULL DEFAULT 'None'")
            addColumnIfMissing("settings", "syncFolderUri", "TEXT")
            addColumnIfMissing("settings", "syncChainId", "TEXT")
            addColumnIfMissing("settings", "syncDeviceName", "TEXT NOT NULL DEFAULT 'Android device'")
            addColumnIfMissing("settings", "syncUserName", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing("settings", "syncPublicName", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing("settings", "lastSyncAt", "INTEGER")
            addColumnIfMissing("settings", "lastSyncHash", "TEXT")
            addColumnIfMissing("settings", "lastSyncStatus", "TEXT NOT NULL DEFAULT 'Sync chain not configured'")
            addColumnIfMissing("settings", "syncConflictCount", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing("settings", "profileBackgroundUri", "TEXT")
            addColumnIfMissing("settings", "profileImageUri", "TEXT")
            addColumnIfMissing("settings", "workspaceName", "TEXT NOT NULL DEFAULT 'Team Notebook'")
            addColumnIfMissing("settings", "workspaceIcon", "TEXT NOT NULL DEFAULT 'N'")
            addColumnIfMissing("settings", "workspaceIconKind", "TEXT NOT NULL DEFAULT 'Text'")
            addColumnIfMissing("settings", "workspaceIconUri", "TEXT")
            addColumnIfMissing("settings", "workspaceBackgroundUri", "TEXT")
            addColumnIfMissing("settings", "adminsControlWorkspaceVisuals", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing("settings", "allowMembersCreateNotes", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing("settings", "allowMembersInvite", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing("settings", "uiScale", "REAL NOT NULL DEFAULT 0.88")
            addColumnIfMissing("settings", "editorLineWidth", "TEXT NOT NULL DEFAULT 'Comfortable'")
            addColumnIfMissing("settings", "editorFontFamily", "TEXT NOT NULL DEFAULT 'Sans'")
            addColumnIfMissing("settings", "showMarkdownSyntax", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing("settings", "noteLongPressAction", "TEXT NOT NULL DEFAULT 'Actions'")
            addColumnIfMissing("settings", "noteSwipeStartAction", "TEXT NOT NULL DEFAULT 'Pin'")
            addColumnIfMissing("settings", "noteSwipeEndAction", "TEXT NOT NULL DEFAULT 'Archive'")
            addColumnIfMissing("settings", "blockScreenshots", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing("settings", "requireBiometricOnOpen", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing("settings", "reduceMotion", "INTEGER NOT NULL DEFAULT 0")

            val now = System.currentTimeMillis()
            execSQL("INSERT OR IGNORE INTO task_boards (id, name, workspaceId, createdAt, updatedAt) VALUES (1, 'Default board', 1, $now, $now)")
            execSQL("INSERT OR IGNORE INTO task_columns (id, boardId, name, status, color, sortOrder, createdAt, updatedAt) VALUES (1, 1, 'To do', 'Todo', 10320895, 0, $now, $now)")
            execSQL("INSERT OR IGNORE INTO task_columns (id, boardId, name, status, color, sortOrder, createdAt, updatedAt) VALUES (2, 1, 'Doing', 'Doing', 5229822, 1, $now, $now)")
            execSQL("INSERT OR IGNORE INTO task_columns (id, boardId, name, status, color, sortOrder, createdAt, updatedAt) VALUES (3, 1, 'Done', 'Done', 5688472, 2, $now, $now)")
            execSQL("UPDATE tasks SET taskBoardId = 1 WHERE taskBoardId IS NULL OR taskBoardId = 0")
            execSQL("UPDATE tasks SET taskColumnId = CASE status WHEN 'Doing' THEN 2 WHEN 'Done' THEN 3 ELSE 1 END WHERE taskColumnId IS NULL")

            execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name ON tags(name)")
            execSQL("CREATE INDEX IF NOT EXISTS index_note_tags_tagId ON note_tags(tagId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_notes_notebookId ON notes(notebookId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_notes_updatedAt ON notes(updatedAt)")
            execSQL("CREATE INDEX IF NOT EXISTS index_notes_pinned ON notes(pinned)")
            execSQL("CREATE INDEX IF NOT EXISTS index_notes_archived ON notes(archived)")
            execSQL("CREATE INDEX IF NOT EXISTS index_notes_workspaceId ON notes(workspaceId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_notebooks_workspaceId ON notebooks(workspaceId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_attachments_noteId ON attachments(noteId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_note_embeds_noteId ON note_embeds(noteId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_note_embeds_type ON note_embeds(type)")
            execSQL("CREATE INDEX IF NOT EXISTS index_task_boards_workspaceId ON task_boards(workspaceId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_task_boards_updatedAt ON task_boards(updatedAt)")
            execSQL("CREATE INDEX IF NOT EXISTS index_task_columns_boardId ON task_columns(boardId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_task_columns_sortOrder ON task_columns(sortOrder)")
            execSQL("CREATE INDEX IF NOT EXISTS index_tasks_status ON tasks(status)")
            execSQL("CREATE INDEX IF NOT EXISTS index_tasks_updatedAt ON tasks(updatedAt)")
            execSQL("CREATE INDEX IF NOT EXISTS index_tasks_workspaceId ON tasks(workspaceId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_tasks_taskBoardId ON tasks(taskBoardId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_tasks_taskColumnId ON tasks(taskColumnId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_createdAt ON chat_messages(createdAt)")
            execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_workspaceId ON chat_messages(workspaceId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_canvas_nodes_updatedAt ON canvas_nodes(updatedAt)")
            execSQL("CREATE INDEX IF NOT EXISTS index_canvas_nodes_type ON canvas_nodes(type)")
            execSQL("CREATE INDEX IF NOT EXISTS index_canvas_nodes_workspaceId ON canvas_nodes(workspaceId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_canvas_edges_fromNodeId ON canvas_edges(fromNodeId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_canvas_edges_toNodeId ON canvas_edges(toNodeId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_canvas_edges_workspaceId ON canvas_edges(workspaceId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_objects_workspaceId ON workspace_objects(workspaceId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_objects_objectType ON workspace_objects(objectType)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_objects_sourceId ON workspace_objects(sourceId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_objects_updatedAt ON workspace_objects(updatedAt)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_objects_pinned ON workspace_objects(pinned)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_object_links_workspaceId ON workspace_object_links(workspaceId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_object_links_fromObjectId ON workspace_object_links(fromObjectId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_object_links_toObjectId ON workspace_object_links(toObjectId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_object_links_linkType ON workspace_object_links(linkType)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_activities_workspaceId ON workspace_activities(workspaceId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_activities_objectId ON workspace_activities(objectId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_activities_createdAt ON workspace_activities(createdAt)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_activities_activityType ON workspace_activities(activityType)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_object_history_workspaceId ON workspace_object_history(workspaceId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_object_history_objectId ON workspace_object_history(objectId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_object_history_historyType ON workspace_object_history(historyType)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_object_history_createdAt ON workspace_object_history(createdAt)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_comments_workspaceId ON workspace_comments(workspaceId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_comments_objectId ON workspace_comments(objectId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_comments_authorUsername ON workspace_comments(authorUsername)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_comments_resolved ON workspace_comments(resolved)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_comments_updatedAt ON workspace_comments(updatedAt)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_files_workspaceId ON workspace_files(workspaceId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_files_objectId ON workspace_files(objectId)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_files_mimeType ON workspace_files(mimeType)")
            execSQL("CREATE INDEX IF NOT EXISTS index_workspace_files_updatedAt ON workspace_files(updatedAt)")
        }

        private fun SupportSQLiteDatabase.addColumnIfMissing(table: String, column: String, definition: String) {
            query("PRAGMA table_info($table)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == column) return
                }
            }
            execSQL("ALTER TABLE $table ADD COLUMN $column $definition")
        }
    }
}
