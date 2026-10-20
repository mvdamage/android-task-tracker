package com.learning.tasktracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.learning.tasktracker.data.DateUtils
import com.learning.tasktracker.data.HealthAnalysisEntity
import com.learning.tasktracker.data.HealthPrescriptionType
import com.learning.tasktracker.ui.theme.extendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DoctorVisitHistorySheet(
    history: List<DoctorVisitHistoryItem>,
    onDismiss: () -> Unit,
    onOpenVisit: (HealthAnalysisEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "История приёмов",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onDismiss) { Text("Закрыть") }
            }
            if (history.isEmpty()) {
                Text(
                    text = "Пока нет состоявшихся приёмов.\nОтметьте приём как «Состоялся», чтобы он появился здесь.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    items(history, key = { it.visit.id }) { item ->
                        DoctorVisitHistoryRow(
                            item = item,
                            onClick = { onOpenVisit(item.visit) }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.extendedColors.divider,
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorVisitHistoryRow(
    item: DoctorVisitHistoryItem,
    onClick: () -> Unit
) {
    val visit = item.visit
    val dateEpoch = visit.doneDateEpochDay ?: visit.plannedDateEpochDay
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = dateEpoch?.let { DateUtils.formatShort(it) } ?: "Без даты",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = visit.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (visit.comment.isNotBlank()) {
            Text(
                text = visit.comment,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (item.prescriptions.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Назначения",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            item.prescriptions.forEach { prescription ->
                val line = buildString {
                    append("· ")
                    append(prescription.title)
                    if (prescription.type == HealthPrescriptionType.MEDICATION &&
                        prescription.dosage.isNotBlank()
                    ) {
                        append(" (")
                        append(prescription.dosage)
                        append(")")
                    }
                }
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
