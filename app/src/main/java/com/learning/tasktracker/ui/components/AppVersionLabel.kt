package com.learning.tasktracker.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.learning.tasktracker.BuildConfig

@Composable
fun AppVersionLabel(modifier: Modifier = Modifier) {
    Text(
        text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
    )
}
