package com.learning.tasktracker.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.MainThread

sealed class VoiceCapturePhase {
    data object Idle : VoiceCapturePhase()
    data object Disclaimer : VoiceCapturePhase()
    data object PermissionRationale : VoiceCapturePhase()
    data object PermissionDenied : VoiceCapturePhase()
    data object Listening : VoiceCapturePhase()
    data object Processing : VoiceCapturePhase()
    data class Error(val messageResId: Int) : VoiceCapturePhase()
}

class VoiceCaptureController(
    context: Context,
    private val onTextRecognized: (String) -> Unit,
    private val onPhaseChanged: (VoiceCapturePhase) -> Unit
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var phase: VoiceCapturePhase = VoiceCapturePhase.Idle
        set(value) {
            field = value
            onPhaseChanged(value)
        }

    val currentPhase: VoiceCapturePhase get() = phase

    @MainThread
    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            phase = VoiceCapturePhase.Error(com.learning.tasktracker.R.string.voice_error_unavailable)
            return
        }
        cancelRecognizer()
        phase = VoiceCapturePhase.Listening
        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).also {
            recognizer = it
        }
        speechRecognizer.setRecognitionListener(listener)
        speechRecognizer.startListening(buildIntent())
    }

    @MainThread
    fun finishListening() {
        if (phase !is VoiceCapturePhase.Listening) return
        phase = VoiceCapturePhase.Processing
        recognizer?.stopListening()
    }

    @MainThread
    fun cancel() {
        cancelRecognizer()
        phase = VoiceCapturePhase.Idle
    }

    @MainThread
    fun showDisclaimer() {
        phase = VoiceCapturePhase.Disclaimer
    }

    @MainThread
    fun showPermissionRationale() {
        phase = VoiceCapturePhase.PermissionRationale
    }

    @MainThread
    fun showPermissionDenied() {
        phase = VoiceCapturePhase.PermissionDenied
    }

    @MainThread
    fun showError(messageResId: Int) {
        cancelRecognizer()
        phase = VoiceCapturePhase.Error(messageResId)
    }

    @MainThread
    fun dismissError() {
        phase = VoiceCapturePhase.Idle
    }

    @MainThread
    fun release() {
        cancelRecognizer()
        phase = VoiceCapturePhase.Idle
    }

    private fun cancelRecognizer() {
        recognizer?.let {
            try {
                it.cancel()
            } catch (_: Exception) {
            }
            try {
                it.destroy()
            } catch (_: Exception) {
            }
        }
        recognizer = null
    }

    private fun buildIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
        }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onPartialResults(partialResults: Bundle?) = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onEndOfSpeech() {
            mainHandler.post {
                if (phase is VoiceCapturePhase.Listening) {
                    phase = VoiceCapturePhase.Processing
                }
            }
        }

        override fun onError(error: Int) {
            mainHandler.post {
                cancelRecognizer()
                val resId = when (error) {
                    SpeechRecognizer.ERROR_AUDIO,
                    SpeechRecognizer.ERROR_CLIENT ->
                        com.learning.tasktracker.R.string.voice_error_microphone
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        com.learning.tasktracker.R.string.voice_error_network
                    SpeechRecognizer.ERROR_NO_MATCH ->
                        com.learning.tasktracker.R.string.voice_error_no_match
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                        com.learning.tasktracker.R.string.voice_error_speech_timeout
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                        com.learning.tasktracker.R.string.voice_error_busy
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                        com.learning.tasktracker.R.string.voice_error_microphone
                    else -> com.learning.tasktracker.R.string.voice_error_no_match
                }
                phase = VoiceCapturePhase.Error(resId)
            }
        }

        override fun onResults(results: Bundle?) {
            mainHandler.post {
                cancelRecognizer()
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (text.isEmpty()) {
                    phase = VoiceCapturePhase.Error(
                        com.learning.tasktracker.R.string.voice_error_no_match
                    )
                } else {
                    phase = VoiceCapturePhase.Idle
                    onTextRecognized(text)
                }
            }
        }
    }
}
