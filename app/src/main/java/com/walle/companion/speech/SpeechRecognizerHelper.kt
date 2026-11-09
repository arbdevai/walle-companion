package com.walle.companion.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class SpeechRecognizerHelper(
    private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    var onListeningStarted: (() -> Unit)? = null
    var onRmsLevelChanged: ((Float) -> Unit)? = null
    var onSpeechResult: ((String) -> Unit)? = null
    var onSpeechError: ((String) -> Unit)? = null

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)?.apply {
                    setRecognitionListener(createListener())
                }
            }
        } catch (_: Throwable) {
            speechRecognizer = null
        }
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            isListening = true
            onListeningStarted?.invoke()
        }

        override fun onRmsChanged(rmsdB: Float) {
            onRmsLevelChanged?.invoke(rmsdB)
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull().orEmpty().trim()
            if (recognizedText.isNotEmpty()) {
                onSpeechResult?.invoke(recognizedText)
            } else {
                onSpeechError?.invoke("Suara tidak terdengar jelas")
            }
        }

        override fun onError(error: Int) {
            isListening = false
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Galat perekaman audio"
                SpeechRecognizer.ERROR_CLIENT -> "Galat klien speech"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Izin mikrofon belum diberikan"
                SpeechRecognizer.ERROR_NETWORK -> "Koneksi jaringan bermasalah"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Batas waktu jaringan habis"
                SpeechRecognizer.ERROR_NO_MATCH -> "Tidak ada suara terdeteksi"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Layanan pengenal suara sedang sibuk"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tidak ada suara yang masuk"
                else -> "Gagal mengenali ucapan"
            }
            onSpeechError?.invoke(message)
        }

        override fun onBeginningOfSpeech() {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            isListening = false
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
        override fun onPartialResults(partialResults: Bundle?) {}
    }

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context) && speechRecognizer != null
    }

    fun startListening() {
        if (speechRecognizer == null) {
            initRecognizer()
        }

        if (speechRecognizer == null) {
            onSpeechError?.invoke("Layanan pengenal suara tidak tersedia di perangkat")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("id", "ID").toString())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            onSpeechError?.invoke("Gagal memulai mic: ${e.message}")
        }
    }

    fun stopListening() {
        if (isListening) {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {
            }
            isListening = false
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {
        }
        isListening = false
    }
}
