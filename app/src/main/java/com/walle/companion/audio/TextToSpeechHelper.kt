package com.walle.companion.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TextToSpeechHelper(
    context: Context,
    private val onInitComplete: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isReady = false

    var onSpeechStarted: (() -> Unit)? = null
    var onSpeechFinished: (() -> Unit)? = null
    var onSpeechError: ((String) -> Unit)? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("id", "ID"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default system language if Indonesian voice data isn't installed
                tts?.language = Locale.getDefault()
            }
            tts?.setPitch(1.18f) // Slightly friendly higher pitch for robot companion
            tts?.setSpeechRate(1.05f)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onSpeechStarted?.invoke()
                }

                override fun onDone(utteranceId: String?) {
                    onSpeechFinished?.invoke()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onSpeechError?.invoke("Gagal menyuarakan teks")
                }
            })

            isReady = true
            onInitComplete(true)
        } else {
            isReady = false
            onInitComplete(false)
        }
    }

    fun speak(text: String, utteranceId: String = "walle_voice_${System.currentTimeMillis()}") {
        if (!isReady || text.isBlank()) {
            onSpeechFinished?.invoke()
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        if (isReady) {
            tts?.stop()
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isReady = false
        } catch (_: Exception) {
            // Ignore clean shutdown error
        }
    }
}
