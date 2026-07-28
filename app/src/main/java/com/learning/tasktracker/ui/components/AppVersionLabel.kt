package com.learning.tasktracker.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.learning.tasktracker.BuildConfig
import com.learning.tasktracker.ui.TestTags

@Composable
fun AppVersionLabel(modifier: Modifier = Modifier) {
    Text(
        text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
        modifier = modifier.testTag(TestTags.APP_VERSION),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
    )
}
