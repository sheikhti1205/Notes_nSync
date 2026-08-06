package com.notesnync.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.activity.compose.BackHandler

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.automirrored.outlined.Note
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.ShapeLine
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notesnync.app.data.displayName
import com.notesnync.app.domain.CanvasEdgeItem
import com.notesnync.app.domain.CanvasNodeItem
import com.notesnync.app.domain.CanvasNodeType
import com.notesnync.app.domain.ChatMessageItem
import com.notesnync.app.domain.ConflictSideSummary
import com.notesnync.app.domain.Destination
import com.notesnync.app.domain.TaskColumnItem
import com.notesnync.app.domain.TaskItem
import com.notesnync.app.domain.TaskPriority
import com.notesnync.app.domain.TaskStatus
import com.notesnync.app.ui.NotesUiState
import com.notesnync.app.ui.NotesViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class TaskView(val label: String) { Kanban("Board"), List("List"), Table("Table"), Calendar("Calendar"), Timeline("Timeline"), Chart("Chart") }
private enum class TaskSort(val label: String) { Manual("Manual"), Title("Title A-Z"), Recent("Recently updated") }

private fun List<com.notesnync.app.domain.TaskItem>.sortedByTask(sort: TaskSort) = when (sort) {
    TaskSort.Manual -> sortedWith(compareBy<com.notesnync.app.domain.TaskItem> { it.sortOrder }.thenByDescending { it.updatedAt })
    TaskSort.Title -> sortedBy { it.title.lowercase() }
    TaskSort.Recent -> sortedByDescending { it.updatedAt }
}

private fun List<TaskItem>.filteredTasks(
    query: String,
    assignee: String,
    label: String,
    priority: TaskPriority?,
): List<TaskItem> {
    val cleanQuery = query.trim()
    val cleanAssignee = assignee.trim()
    val cleanLabel = label.trim()
    return filter { task ->
        val labelValues = task.labels.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val matchesQuery = cleanQuery.isBlank() ||
            task.title.contains(cleanQuery, ignoreCase = true) ||
            task.description.contains(cleanQuery, ignoreCase = true) ||
            task.assignee.contains(cleanQuery, ignoreCase = true) ||
            labelValues.any { it.contains(cleanQuery, ignoreCase = true) }
        val matchesAssignee = cleanAssignee.isBlank() || task.assignee.ifBlank { "@owner" } == cleanAssignee
        val matchesLabel = cleanLabel.isBlank() || labelValues.any { it.equals(cleanLabel, ignoreCase = true) }
        val matchesPriority = priority == null || task.priority == priority
        matchesQuery && matchesAssignee && matchesLabel && matchesPriority
    }
}

private fun TaskItem.belongsToColumn(column: TaskColumnItem): Boolean =
    taskColumnId?.let { it == column.id } ?: (taskBoardId == column.boardId && column.status == status)

@Composable
fun TasksBoardScreen(state: NotesUiState, viewModel: NotesViewModel, onPickTaskAttachment: (TaskItem) -> Unit = {}) {
    var view by remember { mutableStateOf(TaskView.Kanban) }
    var sort by remember { mutableStateOf(TaskSort.Manual) }
    var query by remember { mutableStateOf("") }
    var assigneeFilter by remember { mutableStateOf("") }
    var labelFilter by remember { mutableStateOf("") }
    var priorityFilter by remember { mutableStateOf<TaskPriority?>(null) }
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }
    var selectedBoardId by remember { mutableStateOf<Long?>(null) }
    var newBoardName by remember { mutableStateOf("") }
    var newColumnName by remember { mutableStateOf("") }
    val selectedBoard = state.taskBoards.firstOrNull { it.id == selectedBoardId } ?: state.taskBoards.firstOrNull()
    val selectedBoardIdValue = selectedBoard?.id ?: 1L
    val boardColumns = state.taskColumns.filter { it.boardId == selectedBoardIdValue }.sortedBy { it.sortOrder }
    val boardListState = rememberLazyListState()
    val assignees = remember(state.tasks) {
        state.tasks.map { it.assignee.ifBlank { "@owner" } }.distinct().sorted()
    }
    val labels = remember(state.tasks) {
        state.tasks.flatMap { task -> task.labels.split(",").map { it.trim() } }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }
    val boardTasks = remember(state.tasks, selectedBoardIdValue) {
        state.tasks.filter { it.taskBoardId == selectedBoardIdValue || (selectedBoard == null && it.taskBoardId == 1L) }
    }
    val filteredTasks = remember(boardTasks, query, assigneeFilter, labelFilter, priorityFilter) {
        boardTasks.filteredTasks(query, assigneeFilter, labelFilter, priorityFilter)
    }
    editingTask?.let { task ->
        TaskDetailDialog(
            task = task,
            viewModel = viewModel,
            onPickAttachment = onPickTaskAttachment,
            onDismiss = { editingTask = null },
        )
    }
    WorkspaceScreenFrame("Tasks", "Workspace task board") {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.taskBoards, key = { it.id }) { board ->
                                AssistChip(
                                    onClick = { selectedBoardId = board.id },
                                    label = { Text(board.name) },
                                    leadingIcon = if (board.id == selectedBoardIdValue) ({ Icon(Icons.Outlined.CheckCircle, null, Modifier.size(16.dp)) }) else null,
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(newBoardName, { newBoardName = it }, Modifier.weight(1f), singleLine = true, placeholder = { Text("New board") }, shape = RoundedCornerShape(14.dp))
                            Button(
                                onClick = {
                                    viewModel.createTaskBoard(newBoardName)
                                    newBoardName = ""
                                    selectedBoardId = null
                                },
                                enabled = newBoardName.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                            ) { Text("Create") }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(newColumnName, { newColumnName = it }, Modifier.weight(1f), singleLine = true, placeholder = { Text("New column in ${selectedBoard?.name ?: "board"}") }, shape = RoundedCornerShape(14.dp))
                            FilledTonalButton(
                                onClick = {
                                    viewModel.createTaskColumn(selectedBoardIdValue, newColumnName)
                                    newColumnName = ""
                                },
                                enabled = selectedBoard != null && newColumnName.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                            ) { Text("Add column") }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        selectedBoard?.name ?: "To-Do List",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                    TaskViewMenu(view) { view = it }
                    Spacer(Modifier.width(8.dp))
                    TaskSortMenu(sort) { sort = it }
                }
                TaskFilterPanel(
                    query = query,
                    onQueryChange = { query = it },
                    assignees = assignees,
                    assigneeFilter = assigneeFilter,
                    onAssigneeChange = { assigneeFilter = it },
                    labels = labels,
                    labelFilter = labelFilter,
                    onLabelChange = { labelFilter = it },
                    priorityFilter = priorityFilter,
                    onPriorityChange = { priorityFilter = it },
                    filteredCount = filteredTasks.size,
                    totalCount = boardTasks.size,
                    onClear = {
                        query = ""
                        assigneeFilter = ""
                        labelFilter = ""
                        priorityFilter = null
                    },
                )
            }
        }
        when (view) {
            TaskView.Kanban -> {
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 430.dp),
                        state = boardListState,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(boardColumns, key = { _, column -> column.id }) { columnIndex, column ->
                            TrelloTaskColumn(
                                column = column,
                                columnIndex = columnIndex,
                                columns = boardColumns,
                                tasks = filteredTasks.filter { it.belongsToColumn(column) }.sortedByTask(sort),
                                boardListState = boardListState,
                                viewModel = viewModel,
                                onOpen = { editingTask = it },
                            )
                        }
                    }
                }
            }
            TaskView.List -> {
                item {
                    SectionCard(spacing = 4.dp) {
                        val all = filteredTasks.sortedByTask(sort)
                        if (all.isEmpty()) Text("No tasks yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        all.forEach { CompactTaskRow(it, it.status, viewModel) { editingTask = it } }
                    }
                }
            }
            TaskView.Table -> {
                item { TaskTableCard(filteredTasks.sortedByTask(sort), viewModel) { editingTask = it } }
            }
            TaskView.Calendar -> {
                val grouped = filteredTasks.sortedWith(
                    compareBy<TaskItem> { it.dueAt ?: Long.MAX_VALUE }
                        .thenBy { it.title.lowercase() }
                ).groupBy { taskDueDate(it) }
                if (grouped.isEmpty()) {
                    item {
                        SectionCard { Text("No tasks match these filters", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
                    }
                } else {
                    grouped.forEach { (date, tasksForDate) ->
                        item { TaskCalendarSection(date, tasksForDate, viewModel) { editingTask = it } }
                    }
                }
            }
            TaskView.Chart -> {
                item { TaskChartCard(filteredTasks) }
            }
            TaskView.Timeline -> {
                item {
                    TaskTimelineCard(filteredTasks.sortedWith(compareBy<TaskItem> { it.dueAt ?: it.updatedAt }.thenBy { it.title.lowercase() }), viewModel) {
                        editingTask = it
                    }
                }
            }
        }
    }
}

@Composable
private fun TrelloTaskColumn(
    column: TaskColumnItem,
    columnIndex: Int,
    columns: List<TaskColumnItem>,
    tasks: List<TaskItem>,
    boardListState: LazyListState,
    viewModel: NotesViewModel,
    onOpen: (TaskItem) -> Unit,
) {
    var newTaskTitle by remember(column.id) { mutableStateOf("") }
    val columnTint = Color(column.color).copy(alpha = 0.18f)
    Surface(
        modifier = Modifier.width(284.dp).heightIn(min = 420.dp),
        shape = RoundedCornerShape(22.dp),
        color = Brush.verticalGradient(
            listOf(columnTint.copy(alpha = 0.44f), MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)),
        ).let { MaterialTheme.colorScheme.surface.copy(alpha = 0.84f) },
        shadowElevation = 6.dp,
        tonalElevation = 2.dp,
    ) {
        Column(
            Modifier
                .background(Brush.verticalGradient(listOf(columnTint.copy(alpha = 0.62f), Color.Transparent)))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(column.name, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.weight(1f))
                Text(
                    tasks.size.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            if (tasks.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(112.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Drop tasks here", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                tasks.forEachIndexed { taskIndex, task ->
                    KanbanTaskCard(
                        task = task,
                        taskIndex = taskIndex,
                        column = column,
                        columnIndex = columnIndex,
                        columns = columns,
                        columnTaskCount = tasks.size,
                        boardListState = boardListState,
                        viewModel = viewModel,
                        onOpen = onOpen,
                    )
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
            ) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("+ New") },
                        shape = RoundedCornerShape(12.dp),
                    )
                    IconButton(
                    onClick = {
                        viewModel.addTaskToColumn(newTaskTitle, column)
                        newTaskTitle = ""
                    },
                    enabled = newTaskTitle.isNotBlank(),
                    ) {
                        Icon(Icons.Outlined.Add, null)
                    }
                }
            }
        }
    }
}

@Composable
private fun KanbanTaskCard(
    task: TaskItem,
    taskIndex: Int,
    column: TaskColumnItem,
    columnIndex: Int,
    columns: List<TaskColumnItem>,
    columnTaskCount: Int,
    boardListState: LazyListState,
    viewModel: NotesViewModel,
    onOpen: (TaskItem) -> Unit,
) {
    var dragOffset by remember(task.id, column.id) { mutableStateOf(Offset.Zero) }
    var dragging by remember(task.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val columnStepPx = with(LocalDensity.current) { 296.dp.toPx() }
    val cardStepPx = with(LocalDensity.current) { 116.dp.toPx() }
    val dragTarget = remember(dragOffset, column.id, columns) {
        val targetIndex = (columnIndex + (dragOffset.x / columnStepPx).roundToInt()).coerceIn(0, columns.lastIndex)
        columns.getOrNull(targetIndex)?.takeIf { it.id != column.id || kotlin.math.abs(dragOffset.y) > cardStepPx * 0.45f }
    }
    val dragTargetOrder = remember(dragOffset, taskIndex, columnTaskCount, cardStepPx) {
        (taskIndex + (dragOffset.y / cardStepPx).roundToInt()).coerceIn(0, columnTaskCount.coerceAtLeast(1))
    }
    Surface(
        modifier = Modifier
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .zIndex(if (dragging) 1f else 0f)
            .fillMaxWidth()
            .animateContentSize()
            .pointerInput(task.id, column.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { dragging = true },
                    onDragCancel = {
                        dragging = false
                        dragOffset = Offset.Zero
                    },
                    onDragEnd = {
                        dragTarget?.let { target ->
                            viewModel.moveTaskToColumnAt(task, target, dragTargetOrder)
                        }
                        dragging = false
                        dragOffset = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                        when {
                            dragOffset.x > columnStepPx * 0.42f -> scope.launch { boardListState.scrollBy(22f) }
                            dragOffset.x < -columnStepPx * 0.42f -> scope.launch { boardListState.scrollBy(-22f) }
                        }
                    },
                )
            }
            .clickable { if (!dragging) onOpen(task) },
        shape = RoundedCornerShape(16.dp),
        color = if (dragging) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        shadowElevation = if (dragging) 10.dp else 0.dp,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    task.title,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                IconButton(onClick = { viewModel.deleteTask(task) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (task.description.isNotBlank()) {
                Text(
                    task.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                PriorityPill(task.priority, onClick = { viewModel.updateTaskMeta(task, task.priority.next(), task.dueAt, task.assignee) })
                task.dueAt?.let { TinyTaskChip(formatTaskDue(it)) }
                if (!task.attachmentName.isNullOrBlank()) TinyTaskChip("File")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                TinyTaskChip(task.assignee.ifBlank { "@owner" })
                task.labels.split(",").filter { it.isNotBlank() }.take(2).forEach { TinyTaskChip("#$it") }
            }
            if (dragging) {
                Text(
                    dragTarget?.let { "Move to ${it.name} at ${dragTargetOrder + 1}" } ?: "Drag to reorder",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            TaskStatusMoveRow(task, task.status, viewModel)
        }
    }
}

@Composable
private fun TaskFilterPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    assignees: List<String>,
    assigneeFilter: String,
    onAssigneeChange: (String) -> Unit,
    labels: List<String>,
    labelFilter: String,
    onLabelChange: (String) -> Unit,
    priorityFilter: TaskPriority?,
    onPriorityChange: (TaskPriority?) -> Unit,
    filteredCount: Int,
    totalCount: Int,
    onClear: () -> Unit,
) {
    val hasFilters = query.isNotBlank() || assigneeFilter.isNotBlank() || labelFilter.isNotBlank() || priorityFilter != null
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Filter by title, description, assignee, label") },
                shape = RoundedCornerShape(14.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StringFilterMenu("Assignee", assigneeFilter, assignees, Modifier.weight(1f), onAssigneeChange)
                StringFilterMenu("Label", labelFilter, labels, Modifier.weight(1f), onLabelChange)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PriorityFilterMenu(priorityFilter, Modifier.weight(1f), onPriorityChange)
                Text(
                    "$filteredCount/$totalCount shown",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClear, enabled = hasFilters) { Text("Clear") }
            }
        }
    }
}

@Composable
private fun StringFilterMenu(
    title: String,
    current: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        AssistChip(
            enabled = options.isNotEmpty(),
            onClick = { open = true },
            label = {
                Text(
                    if (current.isBlank()) "$title: All" else "$title: $current",
                    maxLines = 1,
                    fontSize = 12.sp,
                )
            },
        )
        androidx.compose.material3.DropdownMenu(open, { open = false }) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("All") },
                onClick = { onSelect(""); open = false },
                trailingIcon = { if (current.isBlank()) Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp)) },
            )
            options.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(option); open = false },
                    trailingIcon = { if (option == current) Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp)) },
                )
            }
        }
    }
}

@Composable
private fun PriorityFilterMenu(
    current: TaskPriority?,
    modifier: Modifier = Modifier,
    onSelect: (TaskPriority?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        AssistChip(
            onClick = { open = true },
            label = { Text(if (current == null) "Priority: All" else "Priority: ${current.label}", maxLines = 1, fontSize = 12.sp) },
        )
        androidx.compose.material3.DropdownMenu(open, { open = false }) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("All") },
                onClick = { onSelect(null); open = false },
                trailingIcon = { if (current == null) Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp)) },
            )
            TaskPriority.entries.forEach { priority ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(priority.label) },
                    onClick = { onSelect(priority); open = false },
                    trailingIcon = { if (priority == current) Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp)) },
                )
            }
        }
    }
}

@Composable
private fun TaskTableCard(tasks: List<TaskItem>, viewModel: NotesViewModel, onOpen: (TaskItem) -> Unit) {
    SectionCard(spacing = 6.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Task", Modifier.weight(1.4f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Text("Status", Modifier.weight(0.8f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Text("Owner", Modifier.weight(0.8f), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        }
        if (tasks.isEmpty()) {
            Text("No tasks match these filters", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        tasks.forEach { task ->
            Surface(
                Modifier.fillMaxWidth().clickable { onOpen(task) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1.4f)) {
                        Text(task.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 3.dp)) {
                            TinyTaskChip(task.priority.label)
                            task.dueAt?.let { TinyTaskChip(formatTaskDue(it)) }
                            if (!task.attachmentName.isNullOrBlank()) TinyTaskChip("File")
                        }
                    }
                    TinyTaskChip(task.status.label, Modifier.weight(0.8f))
                    Text(
                        task.assignee.ifBlank { "@owner" },
                        Modifier.weight(0.8f),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    IconButton(onClick = { viewModel.moveTask(task, task.status.next()) }, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Outlined.CheckCircle, task.status.nextLabel(), Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCalendarSection(date: LocalDate?, tasks: List<TaskItem>, viewModel: NotesViewModel, onOpen: (TaskItem) -> Unit) {
    val today = LocalDate.now()
    val title = when (date) {
        null -> "No due date"
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
    SectionCard(spacing = 6.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(
                "${tasks.size}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        tasks.forEach { task -> CompactTaskRow(task, task.status, viewModel, onOpen = onOpen) }
    }
}

@Composable
private fun TaskViewMenu(current: TaskView, onSelect: (TaskView) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { open = true },
            label = { Text(current.label, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Outlined.GridView, null, Modifier.size(16.dp)) },
        )
        androidx.compose.material3.DropdownMenu(open, { open = false }) {
            TaskView.entries.forEach { v ->
                androidx.compose.material3.DropdownMenuItem(text = { Text(v.label) }, onClick = { onSelect(v); open = false }, trailingIcon = { if (v == current) Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp)) })
            }
        }
    }
}

@Composable
private fun TaskSortMenu(current: TaskSort, onSelect: (TaskSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { open = true },
            label = { Text("Sort", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Sort, null, Modifier.size(16.dp)) },
        )
        androidx.compose.material3.DropdownMenu(open, { open = false }) {
            TaskSort.entries.forEach { s ->
                androidx.compose.material3.DropdownMenuItem(text = { Text(s.label) }, onClick = { onSelect(s); open = false }, trailingIcon = { if (s == current) Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp)) })
            }
        }
    }
}

@Composable
private fun TaskChartCard(tasks: List<com.notesnync.app.domain.TaskItem>) {
    val counts = TaskStatus.entries.map { st -> st to tasks.count { it.status == st } }
    val total = tasks.size.coerceAtLeast(1)
    val colors = mapOf(
        TaskStatus.Todo to Color(0xFF9D7BFF),
        TaskStatus.Doing to Color(0xFF4FACFE),
        TaskStatus.Done to Color(0xFF56CC98),
    )
    SectionCard {
        Text("By status", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Canvas(Modifier.size(130.dp)) {
                var start = -90f
                val stroke = size.minDimension * 0.22f
                val inset = stroke / 2
                if (tasks.isEmpty()) {
                    drawArc(Color(0xFFBBBBBB).copy(alpha = 0.3f), 0f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round), topLeft = Offset(inset, inset), size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke))
                } else {
                    counts.forEach { (st, c) ->
                        if (c > 0) {
                            val sweep = c / total.toFloat() * 360f
                            drawArc(colors.getValue(st), start, sweep - 3f, false, style = Stroke(stroke, cap = StrokeCap.Round), topLeft = Offset(inset, inset), size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke))
                            start += sweep
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                counts.forEach { (st, c) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(colors.getValue(st)))
                        Text(st.label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("$c", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("${tasks.size} total", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
        // Simple proportional bars
        counts.forEach { (st, c) ->
            Column {
                Text("${st.label} · $c", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(Modifier.fillMaxWidth(c / total.toFloat()).height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.getValue(st)))
                }
            }
        }
    }
}

@Composable
private fun CompactTaskRow(task: TaskItem, status: TaskStatus, viewModel: NotesViewModel, showStatusActions: Boolean = false, onOpen: (TaskItem) -> Unit) {
    val done = status == TaskStatus.Done
    Surface(
        Modifier.fillMaxWidth().clickable { onOpen(task) },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Tap the circle to advance status
            IconButton(onClick = { viewModel.moveTask(task, status.next()) }, modifier = Modifier.size(26.dp)) {
                Icon(
                    if (done) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = status.nextLabel(),
                    tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textDecoration = if (done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                    color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                if (task.description.isNotBlank()) {
                    Text(task.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                if (task.dueAt != null) {
                    Text("Due ${formatTaskDue(task.dueAt)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary, maxLines = 1)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 3.dp)) {
                    TinyTaskChip(status.label)
                    TinyTaskChip(task.assignee.ifBlank { "@owner" })
                    task.labels.split(",").filter { it.isNotBlank() }.take(2).forEach { label ->
                        TinyTaskChip("#$label")
                    }
                    if (!task.attachmentName.isNullOrBlank()) TinyTaskChip("File")
                }
                if (showStatusActions) {
                    TaskStatusMoveRow(task, status, viewModel)
                }
            }
            PriorityPill(task.priority, onClick = { viewModel.updateTaskMeta(task, task.priority.next(), task.dueAt, task.assignee) })
            IconButton(onClick = { viewModel.deleteTask(task) }, modifier = Modifier.size(26.dp)) {
                Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun TaskStatusMoveRow(task: TaskItem, status: TaskStatus, viewModel: NotesViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
        val previous = status.previous()
        val next = status.forward()
        if (previous != status) {
            Text(
                "Move to ${previous.label}",
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.74f))
                    .clickable { viewModel.moveTask(task, previous) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (next != status) {
            Text(
                "Move to ${next.label}",
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable { viewModel.moveTask(task, next) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun TaskTimelineCard(tasks: List<TaskItem>, viewModel: NotesViewModel, onOpen: (TaskItem) -> Unit) {
    SectionCard(spacing = 0.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Timeline", fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.weight(1f))
            Text("${tasks.size} tasks", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        if (tasks.isEmpty()) {
            Text("No tasks match these filters", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        } else {
            tasks.forEachIndexed { index, task ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(taskStatusColor(task.status)))
                        if (index != tasks.lastIndex) {
                            Box(Modifier.width(2.dp).height(66.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)))
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f).padding(bottom = 8.dp).clickable { onOpen(task) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(task.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                TinyTaskChip(task.status.label)
                            }
                            Text(task.description.ifBlank { task.assignee.ifBlank { "@owner" } }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                TinyTaskChip(task.dueAt?.let(::formatTaskDue) ?: "No date")
                                PriorityPill(task.priority) { viewModel.updateTaskMeta(task, task.priority.next(), task.dueAt, task.assignee) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriorityPill(priority: TaskPriority, onClick: () -> Unit) {
    val (color, label) = when (priority) {
        TaskPriority.Urgent -> Color(0xFFFF4E64) to "Urgent"
        TaskPriority.High -> Color(0xFFFF8A4C) to "High"
        TaskPriority.Normal -> Color(0xFF7E57FF) to "Normal"
        TaskPriority.Low -> Color(0xFF56CC98) to "Low"
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun CanvasBoardScreen(state: NotesUiState, viewModel: NotesViewModel, onPickCanvasTarget: (CanvasNodeItem) -> Unit = {}) {
    var selectedNode by remember { mutableStateOf<CanvasNodeItem?>(null) }
    var viewportOffset by remember { mutableStateOf(Offset(18f, 18f)) }
    var viewportScale by remember { mutableStateOf(1f) }
    var focusMode by remember { mutableStateOf(false) }
    val activity = LocalContext.current.findActivity()
    BackHandler(enabled = focusMode) { focusMode = false }
    DisposableEffect(focusMode, activity) {
        val previous = activity?.requestedOrientation
        if (focusMode) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            if (focusMode && previous != null) activity.requestedOrientation = previous
        }
    }
    selectedNode?.let { node ->
        CanvasNodeDetailDialog(
            node = node,
            allNodes = state.canvasNodes,
            edges = state.canvasEdges,
            linkedNote = node.linkedNoteId?.let { id -> state.notes.firstOrNull { it.id == id } },
            viewModel = viewModel,
            onPickTarget = onPickCanvasTarget,
            onDismiss = { selectedNode = null },
        )
    }
    WorkspaceScreenFrame("Canvas", "Project board") {
        item {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val boardHeight = when {
                    focusMode -> 760.dp
                    maxWidth < 380.dp -> 520.dp
                    maxWidth < 620.dp -> 600.dp
                    else -> 680.dp
                }
                Card(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = boardHeight, max = boardHeight),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    BoxWithConstraints(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .pointerInput(state.canvasNodes, viewportScale) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    viewportScale = (viewportScale * zoom).coerceIn(0.55f, 2.25f)
                                    viewportOffset = constrainCanvasOffset(
                                        viewportOffset + pan,
                                        canvasWorldBounds(state.canvasNodes),
                                        size.width.toFloat(),
                                        size.height.toFloat(),
                                        viewportScale,
                                    )
                                }
                            },
                    ) {
                        val density = LocalDensity.current
                        val localPositions = remember { mutableStateMapOf<Long, Pair<Float, Float>>() }
                        val nodes = remember(state.canvasNodes) { adjustedCanvasNodes(state.canvasNodes) }
                        val effectiveNodes = nodes.map { node ->
                            localPositions[node.id]?.let { (x, y) -> node.copy(x = x, y = y) } ?: node
                        }
                        val nodeMap = effectiveNodes.associateBy { it.id }
                        val nodeWidth = when {
                            maxWidth < 420.dp -> maxWidth * 0.56f
                            maxWidth < 720.dp -> maxWidth * 0.40f
                            else -> 230.dp
                        }
                        val nodeHeight = 112.dp
                        val toolbarHeight = 84.dp
                        val boardWidthPx = with(density) { maxWidth.toPx() - 32.dp.toPx() }
                        val boardHeightPx = with(density) { boardHeight.toPx() - 32.dp.toPx() }
                        val nodeWidthPx = with(density) { nodeWidth.toPx() }
                        val nodeHeightPx = with(density) { nodeHeight.toPx() }
                        val toolbarHeightPx = with(density) { toolbarHeight.toPx() }
                        val worldUnitPx = min(boardWidthPx, boardHeightPx - toolbarHeightPx).coerceAtLeast(1f)
                        val worldToScreen: (CanvasNodeItem) -> Offset = { node ->
                            Offset(
                                x = viewportOffset.x + node.x * worldUnitPx * viewportScale,
                                y = viewportOffset.y + node.y * worldUnitPx * viewportScale,
                            )
                        }
                        val nodeRects = effectiveNodes.associate { node ->
                            val position = worldToScreen(node)
                            node.id to CanvasNodeRect(
                                position.x,
                                position.y,
                                position.x + nodeWidthPx,
                                position.y + nodeHeightPx,
                            )
                        }

                        Canvas(Modifier.fillMaxSize()) {
                            val grid = (72.dp.toPx() * viewportScale).coerceIn(36.dp.toPx(), 144.dp.toPx())
                            val startX = viewportOffset.x % grid - grid
                            val startY = viewportOffset.y % grid - grid
                            var x = startX
                            while (x <= size.width + grid) {
                                drawLine(Color(0xFFE8ECF6).copy(alpha = 0.55f), Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                                x += grid
                            }
                            var y = startY
                            while (y <= size.height + grid) {
                                drawLine(Color(0xFFE8ECF6).copy(alpha = 0.55f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                                y += grid
                            }
                        }

                        Canvas(Modifier.fillMaxSize()) {
                            state.canvasEdges.forEachIndexed { index, edge ->
                                val from = nodeMap[edge.fromNodeId] ?: return@forEachIndexed
                                val to = nodeMap[edge.toNodeId] ?: return@forEachIndexed
                                val fromRect = nodeRects[from.id] ?: return@forEachIndexed
                                val toRect = nodeRects[to.id] ?: return@forEachIndexed
                                val points = routeCanvasEdge(
                                    fromRect = fromRect,
                                    toRect = toRect,
                                    obstacles = nodeRects.filterKeys { it != from.id && it != to.id }.values.toList(),
                                    boardWidth = size.width,
                                    boardHeight = size.height,
                                    toolbarHeight = toolbarHeightPx,
                                    laneIndex = index,
                                )
                                val path = Path().apply {
                                    moveTo(points.first().x, points.first().y)
                                    points.drop(1).forEach { lineTo(it.x, it.y) }
                                }
                                drawPath(path, Color(edge.color).copy(alpha = 0.70f), style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
                            }
                        }

                        effectiveNodes.forEach { node ->
                            val position = worldToScreen(node)
                            CanvasNodeCard(
                                node = node,
                                width = nodeWidth,
                                modifier = Modifier
                                    .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
                                    .pointerInput(node.id, viewportScale, worldUnitPx) {
                                        detectDragGestures(
                                            onDragEnd = {
                                                val pos = localPositions[node.id]
                                                if (pos != null) {
                                                    viewModel.moveCanvasNode(node, pos.first, pos.second)
                                                    localPositions.remove(node.id)
                                                }
                                            },
                                            onDragCancel = { localPositions.remove(node.id) },
                                        ) { change, dragAmount ->
                                            change.consume()
                                            val current = localPositions[node.id] ?: (node.x to node.y)
                                            val dx = dragAmount.x / (worldUnitPx * viewportScale).coerceAtLeast(1f)
                                            val dy = dragAmount.y / (worldUnitPx * viewportScale).coerceAtLeast(1f)
                                            localPositions[node.id] = current.first + dx to current.second + dy
                                        }
                                    },
                                onClick = { selectedNode = node },
                            )
                        }

                        Surface(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            shadowElevation = 4.dp,
                        ) {
                            Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${(viewportScale * 100).roundToInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(onClick = { focusMode = !focusMode }) { Text(if (focusMode) "Exit" else "Wide") }
                                TextButton(onClick = { viewportScale = (viewportScale * 0.85f).coerceIn(0.55f, 2.25f) }) { Text("-") }
                                TextButton(onClick = { viewportScale = (viewportScale * 1.15f).coerceIn(0.55f, 2.25f) }) { Text("+") }
                                TextButton(
                                    onClick = {
                                        viewportScale = 1f
                                        viewportOffset = fitCanvasOffset(canvasWorldBounds(effectiveNodes), boardWidthPx, boardHeightPx, nodeWidthPx, nodeHeightPx, toolbarHeightPx, worldUnitPx)
                                    },
                                ) { Text("Fit") }
                            }
                        }

                        if (focusMode) {
                            Surface(
                                Modifier
                                    .align(Alignment.TopStart)
                                    .padding(4.dp),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                shadowElevation = 4.dp,
                            ) {
                                TextButton(onClick = { focusMode = false }) { Text("Back") }
                            }
                        }

                    if (effectiveNodes.isEmpty()) {
                        Column(
                            Modifier.align(Alignment.Center).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Canvas is ready", fontWeight = FontWeight.Black, fontSize = 20.sp)
                            Text("Add notes, files, media, links, or shapes.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }

                        // Bottom toolbar with icons
                        Surface(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 8.dp,
                        ) {
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CanvasTool(Icons.Outlined.TextFields, "Text") { viewModel.addCanvasNode(CanvasNodeType.Text) }
                                CanvasTool(Icons.AutoMirrored.Outlined.Note, "Note") { viewModel.addCanvasNode(CanvasNodeType.Note) }
                                CanvasTool(Icons.Outlined.AttachFile, "File") { viewModel.addCanvasNode(CanvasNodeType.File) }
                                CanvasTool(Icons.Outlined.Image, "Media") { viewModel.addCanvasNode(CanvasNodeType.Media) }
                                CanvasTool(Icons.Outlined.Link, "Link") { viewModel.addCanvasNode(CanvasNodeType.Link) }
                                CanvasTool(Icons.Outlined.ShapeLine, "Shape") { viewModel.addCanvasNode(CanvasNodeType.Shape) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CanvasNodeDetailDialog(
    node: CanvasNodeItem,
    allNodes: List<CanvasNodeItem>,
    edges: List<CanvasEdgeItem>,
    linkedNote: com.notesnync.app.domain.Note?,
    viewModel: NotesViewModel,
    onPickTarget: (CanvasNodeItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var title by remember(node.id) { mutableStateOf(node.title) }
    var subtitle by remember(node.id) { mutableStateOf(node.subtitle) }
    var target by remember(node.id) { mutableStateOf(node.targetUri.orEmpty()) }
    fun openTarget() {
        val uri = target.ifBlank { node.targetUri.orEmpty() }
        if (uri.isNotBlank()) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            }.onFailure { viewModel.showMessage(it.localizedMessage ?: "Cannot open target") }
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp,
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(Color(node.color)))
                    Column(Modifier.weight(1f)) {
                        Text("${node.type.name} block", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Canvas item", fontWeight = FontWeight.Black, fontSize = 22.sp)
                    }
                    TextButton(
                        onClick = {
                            viewModel.openCanvasNodeObject(node)
                            onDismiss()
                        },
                    ) {
                        Text("Object")
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Content / description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(14.dp),
                )
                if (node.type == CanvasNodeType.Link || node.type == CanvasNodeType.File || node.type == CanvasNodeType.Media) {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text(if (node.type == CanvasNodeType.Link) "URL" else "Target URI") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Target", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                node.targetName ?: target.ifBlank { "No file or link attached" },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                            if (!node.targetMimeType.isNullOrBlank() || node.targetSizeBytes != null) {
                                Text(
                                    "${node.targetMimeType.orEmpty()} · ${formatBytes(node.targetSizeBytes ?: 0)}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                if (node.type != CanvasNodeType.Link) {
                                    ElevatedButton(onClick = { onPickTarget(node) }, modifier = Modifier.weight(1f)) {
                                        Text(if (node.targetUri.isNullOrBlank()) "Attach" else "Replace")
                                    }
                                }
                                ElevatedButton(
                                    onClick = ::openTarget,
                                    enabled = target.isNotBlank() || !node.targetUri.isNullOrBlank(),
                                    modifier = Modifier.weight(1f),
                                ) { Text("Open") }
                                TextButton(
                                    onClick = {
                                        target = ""
                                        viewModel.updateCanvasNodeTarget(node, null, null, null, null)
                                    },
                                    enabled = target.isNotBlank() || !node.targetUri.isNullOrBlank(),
                                    modifier = Modifier.weight(1f),
                                ) { Text("Clear") }
                            }
                        }
                    }
                }
                linkedNote?.let { note ->
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Linked note", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(note.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text(
                                note.bodyMarkdown.replace(Regex("[#*`>]"), "").replace("\n", " ").trim(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                fontSize = 12.sp,
                            )
                            TextButton(onClick = { viewModel.select(note); onDismiss() }) { Text("Open note") }
                        }
                    }
                }
                CanvasConnectionSection(
                    node = node,
                    allNodes = allNodes,
                    edges = edges,
                    viewModel = viewModel,
                )
                Text(
                    "Position ${(node.x * 100).roundToInt()}%, ${(node.y * 100).roundToInt()}% · ${node.type.name}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = {
                            viewModel.deleteCanvasNode(node)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            viewModel.updateCanvasNodeContent(node, title, subtitle)
                            if (node.type == CanvasNodeType.Link) {
                                viewModel.updateCanvasNodeTarget(node, target.ifBlank { null }, "text/uri-list", target.ifBlank { null }, null)
                            }
                            onDismiss()
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun CanvasConnectionSection(
    node: CanvasNodeItem,
    allNodes: List<CanvasNodeItem>,
    edges: List<CanvasEdgeItem>,
    viewModel: NotesViewModel,
) {
    var menuOpen by remember(node.id, edges) { mutableStateOf(false) }
    val existingTargets = edges.filter { it.fromNodeId == node.id }.map { it.toNodeId }.toSet()
    val connectableNodes = allNodes.filter { it.id != node.id && it.id !in existingTargets }
    val nodeTitles = allNodes.associate { it.id to it.title }
    val relatedEdges = edges.filter { it.fromNodeId == node.id || it.toNodeId == node.id }

    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Link, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text("Connections", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Box {
                    TextButton(onClick = { menuOpen = true }, enabled = connectableNodes.isNotEmpty()) {
                        Text("Connect")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        connectableNodes.forEach { target ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(target.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        Text(target.type.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    viewModel.addCanvasEdge(node.id, target.id, "link")
                                    menuOpen = false
                                },
                            )
                        }
                    }
                }
            }
            if (relatedEdges.isEmpty()) {
                Text("No connections yet. Link this block to another canvas item.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                relatedEdges.forEach { edge ->
                    val outgoing = edge.fromNodeId == node.id
                    val otherId = if (outgoing) edge.toNodeId else edge.fromNodeId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            if (outgoing) "To" else "From",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(nodeTitles[otherId] ?: "Missing block", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text(edge.label.ifBlank { "link" }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        TextButton(onClick = { viewModel.deleteCanvasEdge(edge) }) {
                            Text("Remove")
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TaskDetailDialog(task: TaskItem, viewModel: NotesViewModel, onPickAttachment: (TaskItem) -> Unit, onDismiss: () -> Unit) {
    var title by remember(task.id) { mutableStateOf(task.title) }
    var description by remember(task.id) { mutableStateOf(task.description) }
    var assignee by remember(task.id) { mutableStateOf(task.assignee.ifBlank { "@owner" }) }
    var status by remember(task.id) { mutableStateOf(task.status) }
    var priority by remember(task.id) { mutableStateOf(task.priority) }
    var dueText by remember(task.id) { mutableStateOf(task.dueAt?.let(::formatTaskDue).orEmpty()) }
    var labels by remember(task.id) { mutableStateOf(task.labels) }
    var showDuePicker by remember(task.id) { mutableStateOf(false) }
    if (showDuePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = parseTaskDue(dueText) ?: task.dueAt ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDuePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dueText = formatTaskDue(it) }
                        showDuePicker = false
                    },
                ) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { showDuePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp,
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Task details", fontWeight = FontWeight.Black, fontSize = 22.sp)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TaskStatusPicker(status, Modifier.weight(1f)) { status = it }
                    TaskPriorityPicker(priority, Modifier.weight(1f)) { priority = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = assignee,
                        onValueChange = { assignee = it },
                        label = { Text("Assignee") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                    OutlinedTextField(
                        value = dueText,
                        onValueChange = { dueText = it },
                        label = { Text("Due") },
                        placeholder = { Text("2026-07-07") },
                        trailingIcon = { TextButton(onClick = { showDuePicker = true }) { Text("Pick") } },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                }
                OutlinedTextField(
                    value = labels,
                    onValueChange = { labels = it },
                    label = { Text("Labels") },
                    placeholder = { Text("planning, design") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                TaskAttachmentSection(task, viewModel, onPickAttachment)
                Text("Due accepts YYYY-MM-DD or a raw timestamp.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            viewModel.updateTaskDetails(task, title, description, status, priority, parseTaskDue(dueText), assignee, labels)
                            onDismiss()
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun TaskAttachmentSection(task: TaskItem, viewModel: NotesViewModel, onPickAttachment: (TaskItem) -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.AttachFile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text("Attachment", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onPickAttachment(task) }) { Text(if (task.attachmentUri.isNullOrBlank()) "Add" else "Replace") }
            }
            if (!task.attachmentUri.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(task.attachmentName.orEmpty(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text("${task.attachmentMimeType.orEmpty()} · ${formatBytes(task.attachmentSizeBytes ?: 0)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    TextButton(onClick = { viewModel.clearTaskAttachment(task) }) { Text("Remove") }
                }
            } else {
                Text("Attach briefs, images, docs, audio, or any local file metadata to this task.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TinyTaskChip(label: String, modifier: Modifier = Modifier) {
    Text(
        label,
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.clip(RoundedCornerShape(999.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun TaskStatusPicker(current: TaskStatus, modifier: Modifier = Modifier, onSelect: (TaskStatus) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        AssistChip(onClick = { open = true }, label = { Text(current.label, fontSize = 13.sp) }, leadingIcon = { Icon(Icons.Outlined.CheckCircle, null, Modifier.size(16.dp)) })
        androidx.compose.material3.DropdownMenu(open, { open = false }) {
            TaskStatus.entries.forEach { status ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(status.label) },
                    onClick = { onSelect(status); open = false },
                    trailingIcon = { if (status == current) Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp)) },
                )
            }
        }
    }
}

@Composable
private fun TaskPriorityPicker(current: TaskPriority, modifier: Modifier = Modifier, onSelect: (TaskPriority) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        AssistChip(onClick = { open = true }, label = { Text(current.label, fontSize = 13.sp) }, leadingIcon = { Icon(Icons.Outlined.RadioButtonUnchecked, null, Modifier.size(16.dp)) })
        androidx.compose.material3.DropdownMenu(open, { open = false }) {
            TaskPriority.entries.forEach { priority ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(priority.label) },
                    onClick = { onSelect(priority); open = false },
                    trailingIcon = { if (priority == current) Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp)) },
                )
            }
        }
    }
}

@Composable
private fun CanvasTool(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ChatScreen(state: NotesUiState, viewModel: NotesViewModel, onPickAttachment: () -> Unit = {}) {
    var message by remember { mutableStateOf("") }
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    Column(Modifier.fillMaxSize()) {
        // Message list grows to fill; input pinned at the bottom above the keyboard.
        val messages = state.chatMessages
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        androidx.compose.runtime.LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp),
        ) {
            itemsIndexed(messages, key = { _, it -> it.id }) { index, chat ->
                val prev = messages.getOrNull(index - 1)
                val next = messages.getOrNull(index + 1)
                val firstOfGroup = prev?.authorUsername != chat.authorUsername
                val lastOfGroup = next?.authorUsername != chat.authorUsername
                ChatBubble(chat, firstOfGroup, lastOfGroup)
            }
        }
        Surface(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = if (imeVisible) 6.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message", fontSize = 14.sp) },
                    maxLines = 4,
                    shape = RoundedCornerShape(22.dp),
                )
                IconButton(
                    onClick = onPickAttachment,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Icon(Icons.Outlined.AttachFile, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                val canSend = message.isNotBlank()
                IconButton(
                    onClick = { viewModel.sendChat(message); message = "" },
                    enabled = canSend,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Send, null, tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun ConflictReviewScreen(state: NotesUiState, viewModel: NotesViewModel) {
    val localChanges = state.workspaceObjectHistory.sortedByDescending { it.createdAt }.take(8)
    val recentActivity = state.workspaceActivities.sortedByDescending { it.createdAt }.take(5)
    val report = state.conflictReport
    WorkspaceScreenFrame("Conflict review", "Resolve sync-chain merge differences") {
        item {
            SectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Outlined.Difference, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text("Sync chain conflict", fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Text("${state.settings.syncProvider.displayName} · ${state.settings.syncDeviceName}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
                Text("Status: ${state.settings.lastSyncStatus}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = viewModel::loadConflictReport, modifier = Modifier.weight(1f)) { Text("Load report") }
                    FilledTonalButton(onClick = { viewModel.go(Destination.SyncMonitor) }, modifier = Modifier.weight(1f)) { Text("Sync monitor") }
                }
                if (report != null) {
                    Text(
                        "${report.format} · ${report.deviceName.ifBlank { "Unknown device" }} · ${relativeSyncTime(report.createdAt)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                    Text(
                        "Local ${report.localHash.take(8)} · Remote ${report.remoteHash.take(8)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
                DifferenceLine("- Remote snapshot changed outside this local base", Color(0xFFFF6780).copy(alpha = 0.20f))
                DifferenceLine("+ Local snapshot also has changes waiting to sync", Color(0xFF56CC98).copy(alpha = 0.20f))
                DifferenceLine("+ New conflict files include object counts and changed item lists", Color(0xFF56CC98).copy(alpha = 0.20f))
            }
        }
        item {
            SectionCard {
                Text("Local snapshot", fontWeight = FontWeight.Black, fontSize = 18.sp)
                if (report != null) {
                    ConflictSideStats(report.local)
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ConflictStat("Notes", state.notes.size.toString(), Modifier.weight(1f))
                        ConflictStat("Tasks", state.tasks.size.toString(), Modifier.weight(1f))
                        ConflictStat("Canvas", state.canvasNodes.size.toString(), Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ConflictStat("Files", state.workspaceFiles.size.toString(), Modifier.weight(1f))
                        ConflictStat("Links", state.workspaceObjectLinks.size.toString(), Modifier.weight(1f))
                        ConflictStat("History", state.workspaceObjectHistory.size.toString(), Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            SectionCard {
                Text("Recent local changes", fontWeight = FontWeight.Black, fontSize = 18.sp)
                if (localChanges.isEmpty()) {
                    Text("No local object history is available yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                } else {
                    localChanges.forEach { event ->
                        DifferenceLine("+ ${event.historyType.name}: ${event.summary}", Color(0xFF56CC98).copy(alpha = 0.16f))
                    }
                }
            }
        }
        item {
            SectionCard {
                Text("Remote side", fontWeight = FontWeight.Black, fontSize = 18.sp)
                if (report == null) {
                    DifferenceLine("- Load the latest notesnync conflict JSON to show decrypted remote counts and changed item labels.", Color(0xFFFF6780).copy(alpha = 0.17f))
                    recentActivity.forEach { activity ->
                        Text("${activity.activityType.name} · ${activity.title} · ${relativeSyncTime(activity.createdAt)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                } else {
                    ConflictSideStats(report.remote)
                    ConflictReportList("Only on this device", report.diff.localOnly, "+", Color(0xFF56CC98).copy(alpha = 0.16f))
                    ConflictReportList("Only on remote", report.diff.remoteOnly, "-", Color(0xFFFF6780).copy(alpha = 0.17f))
                    ConflictReportList("Newer locally", report.diff.localNewer, "+", Color(0xFF56CC98).copy(alpha = 0.16f))
                    ConflictReportList("Newer remotely", report.diff.remoteNewer, "-", Color(0xFFFF6780).copy(alpha = 0.17f))
                    Text("${report.diff.changedCount} changed shared objects found in the conflict report.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
        item {
            SectionCard {
                Text("Resolution", fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text("Choose local only if this device should overwrite the remote encrypted snapshot on the next manual sync. Use restore if the remote copy should win.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = viewModel::useLocalForNextConflictSync, modifier = Modifier.weight(1f)) { Text("Use local next sync") }
                    FilledTonalButton(onClick = { viewModel.go(Destination.SyncMonitor) }, modifier = Modifier.weight(1f)) { Text("Monitor") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { viewModel.go(Destination.Settings) }) { Text("Sync settings") }
                    FilledTonalButton(onClick = { viewModel.go(Destination.ImportExport) }) { Text("Backup / import") }
                }
            }
        }
    }
}

@Composable
private fun ConflictStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ColumnScope.ConflictSideStats(summary: ConflictSideSummary) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ConflictStat("Notes", summary.notes.toString(), Modifier.weight(1f))
        ConflictStat("Tasks", summary.tasks.toString(), Modifier.weight(1f))
        ConflictStat("Canvas", summary.canvasNodes.toString(), Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ConflictStat("Files", summary.files.toString(), Modifier.weight(1f))
        ConflictStat("Links", summary.objectLinks.toString(), Modifier.weight(1f))
        ConflictStat("History", summary.history.toString(), Modifier.weight(1f))
    }
    if (summary.recent.isNotEmpty()) {
        Text("Recent", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        summary.recent.take(5).forEach { label ->
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ColumnScope.ConflictReportList(title: String, items: List<String>, prefix: String, color: Color) {
    if (items.isEmpty()) return
    Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    items.forEach { item ->
        DifferenceLine("$prefix $item", color)
    }
}

@Composable
fun SyncMonitorScreen(state: NotesUiState, viewModel: NotesViewModel) {
    WorkspaceScreenFrame("Sync monitor", "Authorized folder sync chain status") {
        item {
            SectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Outlined.CloudDone, null, tint = MaterialTheme.colorScheme.tertiary)
                    Text(state.settings.lastSyncStatus, fontWeight = FontWeight.Black, fontSize = 22.sp)
                }
                LinearProgressIndicator(
                    progress = { if (state.syncing) 0.62f else 1f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Provider: ${state.settings.syncProvider.displayName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Folder: ${if (state.settings.syncFolderUri.isNullOrBlank()) "Not authorized" else "Authorized"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Device: ${state.settings.syncDeviceName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.settings.lastSyncAt?.let {
                    Text("Last sync: ${relativeSyncTime(it)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (state.settings.syncConflictCount > 0) {
                    Button(onClick = { viewModel.go(Destination.ConflictReview) }) { Text("Open conflict review") }
                } else {
                    FilledTonalButton(onClick = { viewModel.go(Destination.Settings) }) { Text("Open sync settings") }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceScreenFrame(
    title: String,
    subtitle: String,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
        }
        content()
    }
}

@Composable
private fun SectionCard(spacing: androidx.compose.ui.unit.Dp = 12.dp, content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content,
        )
    }
}

@Composable
private fun CanvasNodeCard(
    node: CanvasNodeItem,
    width: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier
            .width(width)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 5.dp,
        tonalElevation = 2.dp,
    ) {
        Column(
            Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(node.color)),
                contentAlignment = Alignment.Center,
            ) {
                val icon = when (node.type) {
                    CanvasNodeType.Text -> Icons.Outlined.TextFields
                    CanvasNodeType.Note -> Icons.AutoMirrored.Outlined.Note
                    CanvasNodeType.File -> Icons.Outlined.Add
                    CanvasNodeType.Shape -> Icons.Outlined.ShapeLine
                    CanvasNodeType.Link -> Icons.Outlined.Link
                    CanvasNodeType.Media -> Icons.Outlined.PlayArrow
                }
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
            Text(node.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(node.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, lineHeight = 13.sp)
        }
    }
}

private fun adjustedCanvasNodes(nodes: List<CanvasNodeItem>): List<CanvasNodeItem> {
    if (nodes.isEmpty()) return emptyList()
    val minDistance = 0.18f
    val placed = mutableListOf<CanvasNodeItem>()
    nodes.sortedBy { it.id }.forEachIndexed { index, node ->
        var x = node.x
        var y = node.y
        var attempts = 0
        while (placed.any { kotlin.math.abs(it.x - x) < minDistance && kotlin.math.abs(it.y - y) < 0.12f } && attempts < 8) {
            x += 0.22f + ((index + attempts) % 3) * 0.08f
            y += 0.14f + ((index + attempts) / 3) * 0.06f
            attempts++
        }
        placed += node.copy(x = x, y = y)
    }
    return placed
}

private data class CanvasWorldBounds(val left: Float, val top: Float, val right: Float, val bottom: Float)

private fun canvasWorldBounds(nodes: List<CanvasNodeItem>): CanvasWorldBounds {
    if (nodes.isEmpty()) return CanvasWorldBounds(0f, 0f, 1f, 1f)
    return CanvasWorldBounds(
        left = nodes.minOf { it.x },
        top = nodes.minOf { it.y },
        right = nodes.maxOf { it.x },
        bottom = nodes.maxOf { it.y },
    )
}

private fun fitCanvasOffset(
    bounds: CanvasWorldBounds,
    boardWidth: Float,
    boardHeight: Float,
    nodeWidth: Float,
    nodeHeight: Float,
    toolbarHeight: Float,
    worldUnit: Float,
): Offset {
    val contentWidth = ((bounds.right - bounds.left).coerceAtLeast(0.2f) * worldUnit) + nodeWidth
    val contentHeight = ((bounds.bottom - bounds.top).coerceAtLeast(0.2f) * worldUnit) + nodeHeight
    val targetX = ((boardWidth - contentWidth) / 2f) - bounds.left * worldUnit
    val targetY = ((boardHeight - toolbarHeight - contentHeight) / 2f) - bounds.top * worldUnit
    return Offset(targetX.coerceIn(-3_600f, 3_600f), targetY.coerceIn(-3_600f, 3_600f))
}

private fun constrainCanvasOffset(
    offset: Offset,
    bounds: CanvasWorldBounds,
    boardWidth: Float,
    boardHeight: Float,
    scale: Float,
): Offset {
    val safeDrift = max(boardWidth, boardHeight) * 3f
    val unit = min(boardWidth, boardHeight).coerceAtLeast(1f)
    val contentCenterX = ((bounds.left + bounds.right) / 2f) * unit * scale
    val contentCenterY = ((bounds.top + bounds.bottom) / 2f) * unit * scale
    return Offset(
        x = offset.x.coerceIn(-contentCenterX - safeDrift, boardWidth - contentCenterX + safeDrift),
        y = offset.y.coerceIn(-contentCenterY - safeDrift, boardHeight - contentCenterY + safeDrift),
    )
}

private data class CanvasNodeRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val center: Offset get() = Offset((left + right) / 2f, (top + bottom) / 2f)
    fun expanded(padding: Float) = CanvasNodeRect(left - padding, top - padding, right + padding, bottom + padding)
}

private fun canvasNodeRect(
    node: CanvasNodeItem,
    boardWidth: Float,
    boardHeight: Float,
    nodeWidth: Float,
    nodeHeight: Float,
    toolbarHeight: Float,
): CanvasNodeRect {
    val x = node.x.coerceIn(0.02f, 0.86f) * (boardWidth - nodeWidth).coerceAtLeast(1f)
    val y = node.y.coerceIn(0.03f, 0.82f) * (boardHeight - nodeHeight - toolbarHeight).coerceAtLeast(1f)
    return CanvasNodeRect(x, y, x + nodeWidth, y + nodeHeight)
}

private fun routeCanvasEdge(
    fromRect: CanvasNodeRect,
    toRect: CanvasNodeRect,
    obstacles: List<CanvasNodeRect>,
    boardWidth: Float,
    boardHeight: Float,
    toolbarHeight: Float,
    laneIndex: Int,
): List<Offset> {
    val fromCenter = fromRect.center
    val toCenter = toRect.center
    val exitsRight = toCenter.x >= fromCenter.x
    val start = Offset(if (exitsRight) fromRect.right else fromRect.left, fromCenter.y)
    val end = Offset(if (exitsRight) toRect.left else toRect.right, toCenter.y)
    val lane = 36f + (laneIndex % 5) * 18f
    val minX = 18f
    val maxX = boardWidth - 18f
    val minY = 18f
    val maxY = boardHeight - toolbarHeight - 18f
    val midX = ((start.x + end.x) / 2f + if (laneIndex % 2 == 0) lane else -lane).coerceIn(minX, maxX)
    val midY = ((start.y + end.y) / 2f + if (laneIndex % 3 == 0) lane else -lane).coerceIn(minY, maxY)
    val sideX = (if (exitsRight) fromRect.right + lane else fromRect.left - lane).coerceIn(minX, maxX)
    val railY = (if (laneIndex % 2 == 0) minY else maxY).coerceIn(minY, maxY)
    val candidates = listOf(
        listOf(start, Offset(midX, start.y), Offset(midX, end.y), end),
        listOf(start, Offset(start.x, midY), Offset(end.x, midY), end),
        listOf(start, Offset(sideX, start.y), Offset(sideX, end.y), end),
        listOf(start, Offset(start.x, railY), Offset(end.x, railY), end),
        listOf(start, Offset(midX, start.y), Offset(midX, midY), Offset(end.x, midY), end),
    )
    return candidates.firstOrNull { path -> !pathIntersectsObstacles(path, obstacles) } ?: candidates.last()
}

private fun pathIntersectsObstacles(path: List<Offset>, obstacles: List<CanvasNodeRect>): Boolean {
    if (path.size < 2) return false
    val padded = obstacles.map { it.expanded(10f) }
    return path.zipWithNext().any { (a, b) -> padded.any { segmentIntersectsRect(a, b, it) } }
}

private fun segmentIntersectsRect(a: Offset, b: Offset, rect: CanvasNodeRect): Boolean {
    val horizontal = kotlin.math.abs(a.y - b.y) < 0.5f
    val vertical = kotlin.math.abs(a.x - b.x) < 0.5f
    return when {
        horizontal -> a.y in rect.top..rect.bottom && rangesOverlap(min(a.x, b.x), max(a.x, b.x), rect.left, rect.right)
        vertical -> a.x in rect.left..rect.right && rangesOverlap(min(a.y, b.y), max(a.y, b.y), rect.top, rect.bottom)
        else -> false
    }
}

private fun rangesOverlap(aStart: Float, aEnd: Float, bStart: Float, bEnd: Float): Boolean =
    max(aStart, bStart) <= min(aEnd, bEnd)

@Composable
private fun ChatBubble(chat: ChatMessageItem, firstOfGroup: Boolean, lastOfGroup: Boolean) {
    if (chat.system) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Center) {
            Text(
                chat.body,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
        return
    }
    val mine = chat.authorUsername.equals("you", true)
    val bubbleColor = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val textColor = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val tailBottom = 5.dp
    val roundTop = if (firstOfGroup) 18.dp else 6.dp
    Row(
        Modifier.fillMaxWidth().padding(top = if (firstOfGroup) 8.dp else 1.dp),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        // Avatar gutter (incoming only), rendered on the last message of a run
        if (!mine) {
            if (lastOfGroup) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(Color(chat.color)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(chat.authorDisplayName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            } else {
                Spacer(Modifier.width(28.dp))
            }
            Spacer(Modifier.width(6.dp))
        }
        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            if (firstOfGroup && !mine) {
                Text(
                    chat.authorDisplayName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = Color(chat.color),
                    modifier = Modifier.padding(start = 10.dp, bottom = 2.dp),
                )
            }
            Surface(
                modifier = Modifier.widthIn(max = 300.dp),
                shape = RoundedCornerShape(
                    topStart = if (mine) 18.dp else roundTop,
                    topEnd = if (mine) roundTop else 18.dp,
                    bottomStart = if (mine || !lastOfGroup) 18.dp else tailBottom,
                    bottomEnd = if (!mine || !lastOfGroup) 18.dp else tailBottom,
                ),
                color = bubbleColor,
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Column(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (chat.body.isNotBlank()) Text(chat.body, color = textColor, fontSize = 14.sp, lineHeight = 19.sp)
                        if (!chat.attachmentName.isNullOrBlank()) {
                            ChatAttachmentCard(chat, mine)
                        }
                    }
                    Text(
                        relativeSyncTime(chat.createdAt),
                        fontSize = 9.sp,
                        color = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatAttachmentCard(chat: ChatMessageItem, mine: Boolean) {
    val fg = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val bg = if (mine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface
    Surface(shape = RoundedCornerShape(12.dp), color = bg) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.AttachFile, null, tint = fg, modifier = Modifier.size(18.dp))
            Column {
                Text(chat.attachmentName.orEmpty(), color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(formatBytes(chat.attachmentSizeBytes ?: 0), color = fg.copy(alpha = 0.7f), fontSize = 10.sp)
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes <= 0 -> "File"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${bytes / (1024 * 1024)} MB"
}

@Composable
private fun DifferenceLine(text: String, color: Color) {
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
    )
}

private fun relativeSyncTime(timestamp: Long): String {
    val minutes = ((System.currentTimeMillis() - timestamp).coerceAtLeast(0) / 60_000).toInt()
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m"
        minutes < 1440 -> "${minutes / 60}h"
        else -> "${minutes / 1440}d"
    }
}

private val TaskStatus.label: String
    get() = when (this) {
        TaskStatus.Todo -> "To do"
        TaskStatus.Doing -> "Doing"
        TaskStatus.Done -> "Done"
    }

private fun TaskStatus.next(): TaskStatus = when (this) {
    TaskStatus.Todo -> TaskStatus.Doing
    TaskStatus.Doing -> TaskStatus.Done
    TaskStatus.Done -> TaskStatus.Todo
}

private fun TaskStatus.previous(): TaskStatus = when (this) {
    TaskStatus.Todo -> TaskStatus.Todo
    TaskStatus.Doing -> TaskStatus.Todo
    TaskStatus.Done -> TaskStatus.Doing
}

private fun TaskStatus.forward(): TaskStatus = when (this) {
    TaskStatus.Todo -> TaskStatus.Doing
    TaskStatus.Doing -> TaskStatus.Done
    TaskStatus.Done -> TaskStatus.Done
}

private fun TaskStatus.nextLabel(): String = when (this) {
    TaskStatus.Todo -> "Start"
    TaskStatus.Doing -> "Done"
    TaskStatus.Done -> "Reopen"
}

private val TaskPriority.label: String
    get() = when (this) {
        TaskPriority.Low -> "Low"
        TaskPriority.Normal -> "Normal"
        TaskPriority.High -> "High"
        TaskPriority.Urgent -> "Urgent"
    }

private fun taskStatusColor(status: TaskStatus): Color = when (status) {
    TaskStatus.Todo -> Color(0xFF9D7BFF)
    TaskStatus.Doing -> Color(0xFF4FACFE)
    TaskStatus.Done -> Color(0xFF56CC98)
}

private fun TaskPriority.next(): TaskPriority = when (this) {
    TaskPriority.Low -> TaskPriority.Normal
    TaskPriority.Normal -> TaskPriority.High
    TaskPriority.High -> TaskPriority.Urgent
    TaskPriority.Urgent -> TaskPriority.Low
}

private val taskDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

private fun taskDueDate(task: TaskItem): LocalDate? =
    task.dueAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }

private fun formatTaskDue(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate().format(taskDateFormatter)

private fun parseTaskDue(value: String): Long? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return null
    trimmed.toLongOrNull()?.let { return it }
    return runCatching {
        LocalDate.parse(trimmed, taskDateFormatter)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
