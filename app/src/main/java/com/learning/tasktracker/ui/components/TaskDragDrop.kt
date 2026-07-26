package com.learning.tasktracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.learning.tasktracker.data.DateUtils
import com.learning.tasktracker.data.TaskEntity
import com.learning.tasktracker.ui.theme.extendedColors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Stable
class TaskDayDragState {
    var draggedTask by mutableStateOf<TaskEntity?>(null)
        private set
    var hoveredDay by mutableStateOf<Long?>(null)
        private set
    var dragPosition by mutableStateOf(Offset.Unspecified)
        private set
    var overlayOrigin by mutableStateOf(Offset.Zero)
        private set
    private var viewportBounds: Rect? = null
    private val dropBounds = mutableStateMapOf<Long, Rect>()

    val isDragging: Boolean get() = draggedTask != null

    fun startDrag(task: TaskEntity, position: Offset) {
        draggedTask = task
        dragPosition = position
        hoveredDay = null
    }

    fun updateDragPosition(position: Offset) {
        dragPosition = position
        hoveredDay = dropBounds.entries
            .asSequence()
            .filter { (_, rect) ->
                rect.contains(position) && viewportBounds?.overlaps(rect) != false
            }
            .minByOrNull { (_, rect) -> rect.width * rect.height }
            ?.key
    }

    fun updateOverlayOrigin(origin: Offset) {
        overlayOrigin = origin
    }

    fun updateViewport(bounds: Rect) {
        viewportBounds = bounds
    }

    fun registerDropZone(day: Long, rect: Rect, merge: Boolean = true) {
        dropBounds[day] = if (merge) {
            dropBounds[day]?.let { existing ->
                Rect(
                    left = min(existing.left, rect.left),
                    top = min(existing.top, rect.top),
                    right = max(existing.right, rect.right),
                    bottom = max(existing.bottom, rect.bottom)
                )
            } ?: rect
        } else {
            rect
        }
    }

    fun unregisterDropZone(day: Long) {
        dropBounds.remove(day)
    }

    fun clear() {
        draggedTask = null
        hoveredDay = null
        dragPosition = Offset.Unspecified
    }
}

private fun Rect.contains(point: Offset): Boolean =
    point.x in left..right && point.y in top..bottom

private fun Rect.overlaps(other: Rect): Boolean =
    left < other.right && right > other.left && top < other.bottom && bottom > other.top

@Composable
fun rememberTaskDayDragState(): TaskDayDragState = remember { TaskDayDragState() }

fun Modifier.dayDropZone(
    day: Long,
    dragState: TaskDayDragState,
    merge: Boolean = true
): Modifier = composed {
    DisposableEffect(day) {
        onDispose { dragState.unregisterDropZone(day) }
    }
    this.onGloballyPositioned { coordinates ->
        dragState.registerDropZone(day, coordinates.boundsInRoot(), merge = merge)
    }
}

fun Modifier.taskDragSource(
    task: TaskEntity,
    dragState: TaskDayDragState,
    onDrop: (TaskEntity, Long) -> Unit,
    onTap: () -> Unit
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    fun rootPosition(change: PointerInputChange): Offset {
        val coordinates = layoutCoordinates ?: return change.position
        return if (coordinates.isAttached) {
            coordinates.localToRoot(change.position)
        } else {
            change.position
        }
    }

    this
        .onGloballyPositioned { layoutCoordinates = it }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onTap
        )
        .pointerInput(task.id) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    dragState.startDrag(task, Offset.Unspecified)
                },
                onDrag = { change, _ ->
                    change.consume()
                    val position = rootPosition(change)
                    if (dragState.draggedTask == null) {
                        dragState.startDrag(task, position)
                    } else {
                        dragState.updateDragPosition(position)
                    }
                },
                onDragEnd = {
                    val targetDay = dragState.hoveredDay
                    val dragged = dragState.draggedTask
                    if (dragged != null && targetDay != null &&
                        targetDay != DateUtils.toGroupKey(dragged.dueDateEpochDay)
                    ) {
                        onDrop(dragged, targetDay)
                    }
                    dragState.clear()
                },
                onDragCancel = { dragState.clear() }
            )
        }
}

@Composable
fun TaskDragGhost(
    task: TaskEntity,
    dragState: TaskDayDragState,
    modifier: Modifier = Modifier
) {
    val position = dragState.dragPosition
    val origin = dragState.overlayOrigin
    if (!dragState.isDragging || position == Offset.Unspecified) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(100f)
    ) {
        Surface(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (position.x - origin.x - 120.dp.toPx()).roundToInt().coerceAtLeast(0),
                        y = (position.y - origin.y - 48.dp.toPx()).roundToInt().coerceAtLeast(0)
                    )
                }
                .widthIn(max = 240.dp)
                .shadow(8.dp, RoundedCornerShape(10.dp)),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
fun DaySectionHeader(
    title: String,
    day: Long,
    dragState: TaskDayDragState,
    modifier: Modifier = Modifier,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val hovered = dragState.isDragging && dragState.hoveredDay == day
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.titleSmall,
        color = if (hovered) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.extendedColors.sectionHeader
        },
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (hovered) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                } else {
                    Modifier
                }
            )
            .padding(start = 4.dp, end = 4.dp, top = topPadding, bottom = 4.dp)
            .dayDropZone(day, dragState, merge = false)
    )
}

@Composable
fun QuickDropDayRow(
    today: Long,
    dragState: TaskDayDragState,
    modifier: Modifier = Modifier
) {
    val chips = listOf(
        today to "Сегодня",
        today + 1 to "Завтра",
        today + 2 to "Послезавтра",
        today + 7 to "+7 дней",
        DateUtils.UNDATED_GROUP_KEY to "Без даты"
    )
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        items(chips, key = { it.first }) { (day, label) ->
            DropDayChip(label = label, day = day, dragState = dragState)
        }
    }
}

@Composable
private fun DropDayChip(
    label: String,
    day: Long,
    dragState: TaskDayDragState
) {
    val hovered = dragState.isDragging && dragState.hoveredDay == day
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (hovered) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .dayDropZone(day, dragState, merge = true)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (hovered) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun Modifier.draggingRowAlpha(task: TaskEntity, dragState: TaskDayDragState): Modifier =
    alpha(if (dragState.draggedTask?.id == task.id) 0.35f else 1f)
