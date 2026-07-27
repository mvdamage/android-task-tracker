package com.learning.tasktracker.ui.voice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.learning.tasktracker.R
import com.learning.tasktracker.TaskTrackerApp
import com.learning.tasktracker.voice.VoiceCaptureController
import com.learning.tasktracker.voice.VoiceCapturePhase

@Composable
fun VoiceCaptureFabColumn(
    onPrimaryClick: () -> Unit,
    primaryContentDescription: String,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SmallFloatingActionButton(
            onClick = onMicClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp)
        ) {
            Icon(
                Icons.Filled.Mic,
                contentDescription = stringResource(R.string.voice_mic_content_description)
            )
        }
        FloatingActionButton(
            onClick = onPrimaryClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = primaryContentDescription)
        }
    }
}

@Composable
fun rememberVoiceCaptureSession(
    onTextRecognized: (String) -> Unit
): VoiceCaptureSession {
    val context = LocalContext.current
    val app = context.applicationContext as TaskTrackerApp
    val settingsStore = app.settingsStore
    var phase by remember { mutableStateOf<VoiceCapturePhase>(VoiceCapturePhase.Idle) }
    val latestOnTextRecognized = rememberUpdatedState(onTextRecognized)

    val controller = remember {
        VoiceCaptureController(
            context = context,
            onTextRecognized = { text -> latestOnTextRecognized.value(text) },
            onPhaseChanged = { phase = it }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            controller.startListening()
        } else {
            val activity = context as? android.app.Activity
            val showRationale = activity?.shouldShowRequestPermissionRationale(
                Manifest.permission.RECORD_AUDIO
            ) == true
            if (showRationale) {
                controller.showPermissionRationale()
            } else {
                controller.showPermissionDenied()
            }
        }
    }

    fun beginCapture() {
        if (!settingsStore.isVoiceDisclaimerAccepted()) {
            controller.showDisclaimer()
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            controller.showPermissionRationale()
            return
        }
        controller.startListening()
    }

    fun acceptDisclaimer() {
        settingsStore.setVoiceDisclaimerAccepted(true)
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            controller.startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun requestPermissionFromRationale() {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                controller.cancel()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.release()
        }
    }

    return VoiceCaptureSession(
        phase = phase,
        onMicClick = ::beginCapture,
        onAcceptDisclaimer = ::acceptDisclaimer,
        onDismissDisclaimer = { controller.cancel() },
        onRequestPermission = ::requestPermissionFromRationale,
        onDismissPermissionRationale = { controller.cancel() },
        onOpenSettings = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
            controller.cancel()
        },
        onDismissPermissionDenied = { controller.cancel() },
        onFinishListening = { controller.finishListening() },
        onCancelListening = { controller.cancel() },
        onDismissError = { controller.dismissError() },
        onCancelSession = { controller.cancel() }
    )
}

data class VoiceCaptureSession(
    val phase: VoiceCapturePhase,
    val onMicClick: () -> Unit,
    val onAcceptDisclaimer: () -> Unit,
    val onDismissDisclaimer: () -> Unit,
    val onRequestPermission: () -> Unit,
    val onDismissPermissionRationale: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onDismissPermissionDenied: () -> Unit,
    val onFinishListening: () -> Unit,
    val onCancelListening: () -> Unit,
    val onDismissError: () -> Unit,
    val onCancelSession: () -> Unit
)

@Composable
fun VoiceCaptureDialogs(session: VoiceCaptureSession) {
    when (val phase = session.phase) {
        VoiceCapturePhase.Idle -> Unit
        VoiceCapturePhase.Disclaimer -> {
            AlertDialog(
                onDismissRequest = session.onDismissDisclaimer,
                title = { Text(stringResource(R.string.voice_disclaimer_title)) },
                text = { Text(stringResource(R.string.voice_disclaimer_body)) },
                confirmButton = {
                    TextButton(onClick = session.onAcceptDisclaimer) {
                        Text(stringResource(R.string.voice_disclaimer_continue))
                    }
                },
                dismissButton = {
                    TextButton(onClick = session.onDismissDisclaimer) {
                        Text(stringResource(R.string.voice_disclaimer_cancel))
                    }
                }
            )
        }
        VoiceCapturePhase.PermissionRationale -> {
            AlertDialog(
                onDismissRequest = session.onDismissPermissionRationale,
                title = { Text(stringResource(R.string.voice_permission_title)) },
                text = { Text(stringResource(R.string.voice_permission_rationale)) },
                confirmButton = {
                    TextButton(onClick = session.onRequestPermission) {
                        Text(stringResource(R.string.voice_permission_allow))
                    }
                },
                dismissButton = {
                    TextButton(onClick = session.onDismissPermissionRationale) {
                        Text(stringResource(R.string.voice_disclaimer_cancel))
                    }
                }
            )
        }
        VoiceCapturePhase.PermissionDenied -> {
            AlertDialog(
                onDismissRequest = session.onDismissPermissionDenied,
                title = { Text(stringResource(R.string.voice_permission_title)) },
                text = { Text(stringResource(R.string.voice_permission_denied)) },
                confirmButton = {
                    TextButton(onClick = session.onOpenSettings) {
                        Text(stringResource(R.string.voice_open_settings))
                    }
                },
                dismissButton = {
                    TextButton(onClick = session.onDismissPermissionDenied) {
                        Text(stringResource(R.string.voice_disclaimer_cancel))
                    }
                }
            )
        }
        VoiceCapturePhase.Listening -> {
            AlertDialog(
                onDismissRequest = session.onCancelListening,
                title = { Text(stringResource(R.string.voice_listening_title)) },
                text = { Text(stringResource(R.string.voice_listening_body)) },
                confirmButton = {
                    TextButton(onClick = session.onFinishListening) {
                        Text(stringResource(R.string.voice_done))
                    }
                },
                dismissButton = {
                    TextButton(onClick = session.onCancelListening) {
                        Text(stringResource(R.string.voice_disclaimer_cancel))
                    }
                }
            )
        }
        VoiceCapturePhase.Processing -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(stringResource(R.string.voice_processing_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.voice_processing_body))
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                },
                confirmButton = {}
            )
        }
        is VoiceCapturePhase.Error -> {
            AlertDialog(
                onDismissRequest = session.onDismissError,
                title = { Text(stringResource(R.string.voice_error_title)) },
                text = { Text(stringResource(phase.messageResId)) },
                confirmButton = {
                    TextButton(onClick = session.onDismissError) {
                        Text(stringResource(R.string.voice_ok))
                    }
                }
            )
        }
    }
}
