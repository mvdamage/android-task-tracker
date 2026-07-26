package com.learning.tasktracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.learning.tasktracker.data.TaskEntity
import com.learning.tasktracker.ui.theme.extendedColors

@Stable
class TaskDayDragState {
    var draggedTask by mutableStateOf<TaskEntity?>(null)
        private set
    var hoveredDay by mutableStateOf<Long?>(null)
        private set
    val dropBounds = mutableStateMapOf<Long, Rect>()

    val isDragging: Boolean get() = draggedTask != null

    fun startDrag(task: TaskEntity) {
        draggedTask = task
        hoveredDay = null
    }

    fun updateHover(rootY: Float) {
        hoveredDay = dropBounds.entries.firstOrNull { (_, rect) ->
            rootY in rect.top..rect.bottom
        }?.key
    }

    fun registerBounds(day: Long, rect: Rect) {
        dropBounds[day] = rect
    }

    fun clear() {
        draggedTask = null
        hoveredDay = null
    }
}

@Composable
fun rememberTaskDayDragState(): TaskDayDragState = remember { TaskDayDragState() }

@Composable
fun QuickDropDayRow(
    today: Long,
    dragState: TaskDayDragState,
    modifier: Modifier = Modifier
) {
    val quickDays = listOf(
        today to "Сегодня",
        today + 1 to "Завтра",
        today + 2 to "Послезавтра",
        today + 7 to "+7 дней"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quickDays.forEach { (day, label) ->
            DropDayChip(
                label = label,
                day = day,
                dragState = dragState
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
    topPadding: androidx.compose.ui.unit.Dp = 4.dp
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
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (hovered) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.background
                }
            )
            .dropTarget(day, dragState)
            .padding(start = 4.dp, end = 4.dp, top = topPadding, bottom = 8.dp)
    )
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
                if (hovered) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .dropTarget(day, dragState)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (hovered) FontWeight.SemiBold else FontWeight.Normal,
            color = if (hovered) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

private fun Modifier.dropTarget(day: Long, dragState: TaskDayDragState): Modifier =
    onGloballyPositioned { coordinates ->
        dragState.registerBounds(day, coordinates.boundsInRoot())
    }

fun Modifier.draggableTaskRow(
    task: TaskEntity,
    dragState: TaskDayDragState,
    onTap: () -> Unit,
    onDrop: (TaskEntity, Long) -> Unit
): Modifier = composed {
    var rowCoordinates by remember(task.id) { mutableStateOf<LayoutCoordinates?>(null) }
    var dragOffset by remember(task.id) { mutableStateOf(Offset.Zero) }
    val fingerOffsetPx = with(LocalDensity.current) { 24.dp.toPx() }

    this
        .alpha(if (dragState.draggedTask?.id == task.id) 0.35f else 1f)
        .onGloballyPositioned { rowCoordinates = it }
        .pointerInput(task.id) {
            detectTapGestures(onTap = { onTap() })
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    dragOffset = Offset.Zero
                    dragState.startDrag(task)
                },
                onDrag = { _, dragAmount ->
                    dragOffset += dragAmount
                    val coords = rowCoordinates ?: return@detectDragGesturesAfterLongPress
                    val rootY = coords.localToRoot(Offset.Zero).y + dragOffset.y + fingerOffsetPx
                    dragState.updateHover(rootY)
                },
                onDragEnd = {
                    val targetDay = dragState.hoveredDay
                    val dragged = dragState.draggedTask
                    if (dragged != null && targetDay != null && targetDay != dragged.dueDateEpochDay) {
                        onDrop(dragged, targetDay)
                    }
                    dragState.clear()
                },
                onDragCancel = { dragState.clear() }
            )
        }
}
