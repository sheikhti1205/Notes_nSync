package com.notesnync.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotesDao {
    // ---- Workspaces ----
    @Query("SELECT * FROM workspaces ORDER BY createdAt ASC")
    fun observeWorkspaces(): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaces ORDER BY createdAt ASC")
    suspend fun allWorkspaces(): List<WorkspaceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspace(entity: WorkspaceEntity): Long

    @Query("UPDATE workspaces SET name = :name, icon = :icon, iconKind = :iconKind, iconUri = :iconUri, backgroundUri = :backgroundUri, palette = :palette WHERE id = :id")
    suspend fun updateWorkspaceMeta(id: Long, name: String, icon: String, iconKind: String, iconUri: String?, backgroundUri: String?, palette: String)

    @Query(
        "UPDATE workspaces SET permRename = :permRename, permChangeIcon = :permChangeIcon, permInviteMembers = :permInviteMembers, " +
            "permDeleteNotes = :permDeleteNotes, permEditNotes = :permEditNotes, permCreateCanvas = :permCreateCanvas, permManageTasks = :permManageTasks WHERE id = :id",
    )
    suspend fun updateWorkspacePermissions(
        id: Long,
        permRename: Boolean,
        permChangeIcon: Boolean,
        permInviteMembers: Boolean,
        permDeleteNotes: Boolean,
        permEditNotes: Boolean,
        permCreateCanvas: Boolean,
        permManageTasks: Boolean,
    )

    @Query("DELETE FROM workspaces WHERE id = :id")
    suspend fun deleteWorkspaceById(id: Long)

    @Query("DELETE FROM notes WHERE workspaceId = :ws")
    suspend fun deleteNotesForWorkspace(ws: Long)

    @Query("DELETE FROM notebooks WHERE workspaceId = :ws")
    suspend fun deleteNotebooksForWorkspace(ws: Long)

    @Query("DELETE FROM tasks WHERE workspaceId = :ws")
    suspend fun deleteTasksForWorkspace(ws: Long)

    @Query("DELETE FROM chat_messages WHERE workspaceId = :ws")
    suspend fun deleteChatForWorkspace(ws: Long)

    @Query("DELETE FROM canvas_nodes WHERE workspaceId = :ws")
    suspend fun deleteCanvasForWorkspace(ws: Long)

    @Query("DELETE FROM canvas_edges WHERE workspaceId = :ws")
    suspend fun deleteCanvasEdgesForWorkspace(ws: Long)

    @Query("DELETE FROM workspace_objects WHERE workspaceId = :ws")
    suspend fun deleteWorkspaceObjectsForWorkspace(ws: Long)

    @Query("DELETE FROM workspace_object_links WHERE workspaceId = :ws")
    suspend fun deleteWorkspaceObjectLinksForWorkspace(ws: Long)

    @Query("DELETE FROM workspace_activities WHERE workspaceId = :ws")
    suspend fun deleteWorkspaceActivitiesForWorkspace(ws: Long)

    @Query("DELETE FROM workspace_object_history WHERE workspaceId = :ws")
    suspend fun deleteWorkspaceObjectHistoryForWorkspace(ws: Long)

    @Query("DELETE FROM workspace_comments WHERE workspaceId = :ws")
    suspend fun deleteWorkspaceCommentsForWorkspace(ws: Long)

    @Query("DELETE FROM workspace_files WHERE workspaceId = :ws")
    suspend fun deleteWorkspaceFilesForWorkspace(ws: Long)

    @Transaction
    @Query("SELECT * FROM notes WHERE archived = 0 AND workspaceId = :ws ORDER BY pinned DESC, updatedAt DESC")
    fun observeActiveNotes(ws: Long): Flow<List<NoteWithRelations>>

    @Transaction
    @Query("SELECT * FROM notes WHERE archived = 1 AND workspaceId = :ws ORDER BY updatedAt DESC")
    fun observeArchivedNotes(ws: Long): Flow<List<NoteWithRelations>>

    @Transaction
    @Query(
        """
        SELECT DISTINCT notes.* FROM notes
        LEFT JOIN note_tags ON note_tags.noteId = notes.id
        LEFT JOIN tags ON tags.id = note_tags.tagId
        WHERE notes.archived = 0 AND notes.workspaceId = :ws AND (
            notes.title LIKE '%' || :query || '%' OR
            notes.bodyMarkdown LIKE '%' || :query || '%' OR
            tags.name LIKE '%' || :query || '%'
        )
        ORDER BY notes.pinned DESC, notes.updatedAt DESC
        """,
    )
    fun searchNotes(query: String, ws: Long): Flow<List<NoteWithRelations>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun noteById(id: Long): NoteWithRelations?

    @Transaction
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun allNotesSnapshot(): List<NoteWithRelations>

    @Query("SELECT * FROM notebooks WHERE workspaceId = :ws ORDER BY sortOrder ASC, name ASC")
    fun observeNotebooks(ws: Long): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks ORDER BY sortOrder ASC, name ASC")
    suspend fun allNotebooks(): List<NotebookEntity>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun allTags(): List<TagEntity>

    @Query("SELECT * FROM attachments ORDER BY id ASC")
    suspend fun allAttachments(): List<AttachmentEntity>

    @Query("SELECT * FROM note_embeds ORDER BY createdAt ASC")
    suspend fun allNoteEmbeds(): List<NoteEmbedEntity>

    @Query("SELECT * FROM task_boards WHERE workspaceId = :ws ORDER BY updatedAt DESC")
    fun observeTaskBoards(ws: Long): Flow<List<TaskBoardEntity>>

    @Query("SELECT * FROM task_boards WHERE workspaceId = :ws ORDER BY updatedAt DESC")
    suspend fun taskBoardsForWorkspace(ws: Long): List<TaskBoardEntity>

    @Query("SELECT * FROM task_boards ORDER BY workspaceId ASC, updatedAt DESC")
    suspend fun allTaskBoards(): List<TaskBoardEntity>

    @Query("SELECT task_columns.* FROM task_columns INNER JOIN task_boards ON task_boards.id = task_columns.boardId WHERE task_boards.workspaceId = :ws ORDER BY task_columns.boardId ASC, task_columns.sortOrder ASC")
    fun observeTaskColumns(ws: Long): Flow<List<TaskColumnEntity>>

    @Query("SELECT * FROM task_columns WHERE boardId = :boardId ORDER BY sortOrder ASC")
    suspend fun taskColumnsForBoard(boardId: Long): List<TaskColumnEntity>

    @Query("SELECT * FROM task_columns WHERE boardId = :boardId AND status = :status ORDER BY sortOrder ASC LIMIT 1")
    suspend fun taskColumnForStatus(boardId: Long, status: String): TaskColumnEntity?

    @Query("SELECT * FROM task_columns ORDER BY boardId ASC, sortOrder ASC")
    suspend fun allTaskColumns(): List<TaskColumnEntity>

    @Query("SELECT * FROM tasks WHERE workspaceId = :ws ORDER BY taskBoardId ASC, taskColumnId ASC, sortOrder ASC, updatedAt DESC")
    fun observeTasks(ws: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY updatedAt DESC")
    suspend fun allTasks(): List<TaskEntity>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM tasks WHERE taskBoardId = :boardId AND taskColumnId = :columnId")
    suspend fun maxTaskSortOrder(boardId: Long, columnId: Long): Int

    @Query("SELECT * FROM tasks WHERE taskBoardId = :boardId AND taskColumnId = :columnId ORDER BY sortOrder ASC, updatedAt DESC")
    suspend fun tasksForColumn(boardId: Long, columnId: Long): List<TaskEntity>

    @Query("SELECT * FROM chat_messages WHERE workspaceId = :ws ORDER BY createdAt ASC")
    fun observeChatMessages(ws: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY createdAt ASC")
    suspend fun allChatMessages(): List<ChatMessageEntity>

    @Query("SELECT * FROM canvas_nodes WHERE workspaceId = :ws ORDER BY updatedAt ASC")
    fun observeCanvasNodes(ws: Long): Flow<List<CanvasNodeEntity>>

    @Query("SELECT * FROM canvas_nodes ORDER BY updatedAt ASC")
    suspend fun allCanvasNodes(): List<CanvasNodeEntity>

    @Query("SELECT * FROM canvas_edges WHERE workspaceId = :ws ORDER BY updatedAt ASC")
    fun observeCanvasEdges(ws: Long): Flow<List<CanvasEdgeEntity>>

    @Query("SELECT * FROM canvas_edges ORDER BY updatedAt ASC")
    suspend fun allCanvasEdges(): List<CanvasEdgeEntity>

    @Query("SELECT * FROM workspace_objects WHERE workspaceId = :ws AND archived = 0 ORDER BY pinned DESC, updatedAt DESC")
    fun observeWorkspaceObjects(ws: Long): Flow<List<WorkspaceObjectEntity>>

    @Query("SELECT * FROM workspace_objects ORDER BY updatedAt DESC")
    suspend fun allWorkspaceObjects(): List<WorkspaceObjectEntity>

    @Query("SELECT * FROM workspace_object_links WHERE workspaceId = :ws ORDER BY createdAt DESC")
    fun observeWorkspaceObjectLinks(ws: Long): Flow<List<WorkspaceObjectLinkEntity>>

    @Query("SELECT * FROM workspace_object_links ORDER BY createdAt DESC")
    suspend fun allWorkspaceObjectLinks(): List<WorkspaceObjectLinkEntity>

    @Query("SELECT * FROM workspace_activities WHERE workspaceId = :ws ORDER BY createdAt DESC LIMIT :limit")
    fun observeWorkspaceActivities(ws: Long, limit: Int = 80): Flow<List<WorkspaceActivityEntity>>

    @Query("SELECT * FROM workspace_activities ORDER BY createdAt DESC")
    suspend fun allWorkspaceActivities(): List<WorkspaceActivityEntity>

    @Query("SELECT * FROM workspace_object_history WHERE workspaceId = :ws ORDER BY createdAt DESC LIMIT :limit")
    fun observeWorkspaceObjectHistory(ws: Long, limit: Int = 160): Flow<List<WorkspaceObjectHistoryEntity>>

    @Query("SELECT * FROM workspace_object_history ORDER BY createdAt DESC")
    suspend fun allWorkspaceObjectHistory(): List<WorkspaceObjectHistoryEntity>

    @Query("SELECT * FROM workspace_comments WHERE workspaceId = :ws ORDER BY updatedAt DESC")
    fun observeWorkspaceComments(ws: Long): Flow<List<WorkspaceCommentEntity>>

    @Query("SELECT * FROM workspace_comments ORDER BY updatedAt DESC")
    suspend fun allWorkspaceComments(): List<WorkspaceCommentEntity>

    @Query("SELECT * FROM workspace_files WHERE workspaceId = :ws ORDER BY updatedAt DESC")
    fun observeWorkspaceFiles(ws: Long): Flow<List<WorkspaceFileEntity>>

    @Query("SELECT * FROM workspace_files ORDER BY updatedAt DESC")
    suspend fun allWorkspaceFiles(): List<WorkspaceFileEntity>

    @Query("SELECT * FROM settings WHERE id = 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun settings(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: AppSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotebook(entity: NotebookEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(entity: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(entity: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(entity: AttachmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteEmbed(entity: NoteEmbedEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskBoard(entity: TaskBoardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskColumn(entity: TaskColumnEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(entity: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(entity: ChatMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCanvasNode(entity: CanvasNodeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCanvasEdge(entity: CanvasEdgeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspaceObject(entity: WorkspaceObjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspaceObjectLink(entity: WorkspaceObjectLinkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspaceActivity(entity: WorkspaceActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspaceObjectHistory(entity: WorkspaceObjectHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspaceComment(entity: WorkspaceCommentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspaceFile(entity: WorkspaceFileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteTag(ref: NoteTagCrossRef)

    @Update
    suspend fun updateNote(entity: NoteEntity)

    @Query("UPDATE notes SET title = :title, bodyMarkdown = :body, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNoteContent(id: Long, title: String, body: String, updatedAt: Long)

    @Query("UPDATE notes SET coverUri = :coverUri, coverMimeType = :coverMimeType, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNoteCover(id: Long, coverUri: String?, coverMimeType: String?, updatedAt: Long)

    @Query("UPDATE notes SET pinned = :value, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPinned(id: Long, value: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET starred = :value, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setStarred(id: Long, value: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET archived = :value, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: Long, value: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET locked = :value, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setLocked(id: Long, value: Boolean, updatedAt: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun clearTagsForNote(noteId: Long)

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun tagByName(name: String): TagEntity?

    @Query("DELETE FROM notes")
    suspend fun clearNotes()

    @Query("DELETE FROM notebooks")
    suspend fun clearNotebooks()

    @Query("DELETE FROM tags")
    suspend fun clearTags()

    @Query("DELETE FROM attachments")
    suspend fun clearAttachments()

    @Query("DELETE FROM note_embeds")
    suspend fun clearNoteEmbeds()

    @Query("DELETE FROM task_columns")
    suspend fun clearTaskColumns()

    @Query("DELETE FROM task_boards")
    suspend fun clearTaskBoards()

    @Query("UPDATE tasks SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setTaskStatus(id: Long, status: String, updatedAt: Long)

    @Query("UPDATE tasks SET taskBoardId = :boardId, taskColumnId = :columnId, status = :status, sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setTaskColumn(id: Long, boardId: Long, columnId: Long, status: String, sortOrder: Int, updatedAt: Long)

    @Query("UPDATE tasks SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTaskOrder(id: Long, sortOrder: Int, updatedAt: Long)

    @Query("UPDATE tasks SET priority = :priority, dueAt = :dueAt, assignee = :assignee, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTaskMeta(id: Long, priority: String, dueAt: Long?, assignee: String, updatedAt: Long)

    @Query("UPDATE tasks SET title = :title, description = :description, status = :status, priority = :priority, dueAt = :dueAt, assignee = :assignee, labels = :labels, attachmentName = :attachmentName, attachmentMimeType = :attachmentMimeType, attachmentUri = :attachmentUri, attachmentSizeBytes = :attachmentSizeBytes, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTaskDetails(id: Long, title: String, description: String, status: String, priority: String, dueAt: Long?, assignee: String, labels: String, attachmentName: String?, attachmentMimeType: String?, attachmentUri: String?, attachmentSizeBytes: Long?, updatedAt: Long)

    @Query("UPDATE tasks SET attachmentName = :name, attachmentMimeType = :mimeType, attachmentUri = :uri, attachmentSizeBytes = :sizeBytes, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTaskAttachment(id: Long, name: String?, mimeType: String?, uri: String?, sizeBytes: Long?, updatedAt: Long)

    @Query("UPDATE canvas_nodes SET x = :x, y = :y, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCanvasNodePosition(id: Long, x: Float, y: Float, updatedAt: Long)

    @Query("UPDATE canvas_nodes SET title = :title, subtitle = :subtitle, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCanvasNodeContent(id: Long, title: String, subtitle: String, updatedAt: Long)

    @Query("UPDATE canvas_nodes SET targetUri = :uri, targetMimeType = :mimeType, targetName = :name, targetSizeBytes = :sizeBytes, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCanvasNodeTarget(id: Long, uri: String?, mimeType: String?, name: String?, sizeBytes: Long?, updatedAt: Long)

    @Query("DELETE FROM canvas_edges WHERE fromNodeId = :nodeId OR toNodeId = :nodeId")
    suspend fun deleteCanvasEdgesForNode(nodeId: Long)

    @Query("DELETE FROM canvas_edges WHERE id = :id")
    suspend fun deleteCanvasEdge(id: Long)

    @Query("DELETE FROM canvas_nodes WHERE id = :id")
    suspend fun deleteCanvasNode(id: Long)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatMessages()

    @Query("DELETE FROM canvas_nodes")
    suspend fun clearCanvasNodes()

    @Query("DELETE FROM canvas_edges")
    suspend fun clearCanvasEdges()

    @Query("DELETE FROM workspace_objects")
    suspend fun clearWorkspaceObjects()

    @Query("DELETE FROM workspace_object_links")
    suspend fun clearWorkspaceObjectLinks()

    @Query("DELETE FROM workspace_activities")
    suspend fun clearWorkspaceActivities()

    @Query("DELETE FROM workspace_object_history")
    suspend fun clearWorkspaceObjectHistory()

    @Query("DELETE FROM workspace_comments")
    suspend fun clearWorkspaceComments()

    @Query("DELETE FROM workspace_files")
    suspend fun clearWorkspaceFiles()
}
