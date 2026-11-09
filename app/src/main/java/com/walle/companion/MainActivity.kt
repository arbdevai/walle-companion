package com.walle.companion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.walle.companion.audio.SoundEffectManager
import com.walle.companion.audio.TextToSpeechHelper
import com.walle.companion.data.api.GeminiClient
import com.walle.companion.data.model.WalleExpression
import com.walle.companion.data.preferences.AppPreferences
import com.walle.companion.sensor.ShakeDetector
import com.walle.companion.speech.SpeechRecognizerHelper
import com.walle.companion.ui.WalleEyeView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var eyeView: WalleEyeView
    private lateinit var tvClock: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvSpeechSubtitle: TextView
    private lateinit var rootContainer: View

    private lateinit var prefs: AppPreferences
    private lateinit var soundManager: SoundEffectManager
    private lateinit var ttsHelper: TextToSpeechHelper
    private lateinit var speechHelper: SpeechRecognizerHelper
    private lateinit var shakeDetector: ShakeDetector
    private lateinit var geminiClient: GeminiClient

    private val clockHandler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListeningFlow()
        } else {
            Toast.makeText(this, "Izin mikrofon diperlukan untuk mendengarkan", Toast.LENGTH_SHORT).show()
            resetToIdleState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = AppPreferences(this)
        soundManager = SoundEffectManager { prefs.isSoundEnabled }
        geminiClient = GeminiClient { prefs.geminiApiKey }

        initViews()
        initAudioAndSpeech()
        initSensors()
        startClockLoop()
    }

    private fun initViews() {
        eyeView = findViewById(R.id.walleEyeView)
        tvClock = findViewById(R.id.tvClock)
        tvStatus = findViewById(R.id.tvStatus)
        tvSpeechSubtitle = findViewById(R.id.tvSpeechSubtitle)
        rootContainer = findViewById(R.id.rootContainer)

        eyeView.onBlinkListener = {
            soundManager.playBlink()
        }

        eyeView.onEyeInteractionListener = {
            onScreenTapped()
        }

        rootContainer.setOnClickListener {
            onScreenTapped()
        }

        rootContainer.setOnLongClickListener {
            showApiKeyDialog()
            true
        }
    }

    private fun initAudioAndSpeech() {
        ttsHelper = TextToSpeechHelper(this) { success ->
            if (success) {
                // Initial greeting sound
                soundManager.playHappyChirp()
            }
        }

        ttsHelper.onSpeechStarted = {
            runOnUiThread {
                tvStatus.setText(R.string.status_speaking)
            }
        }

        ttsHelper.onSpeechFinished = {
            runOnUiThread {
                resetToIdleState()
            }
        }

        speechHelper = SpeechRecognizerHelper(this).apply {
            onListeningStarted = {
                runOnUiThread {
                    tvStatus.setText(R.string.status_listening)
                    eyeView.setExpression(WalleExpression.LISTENING)
                }
            }

            onSpeechResult = { recognizedText ->
                runOnUiThread {
                    handleUserSpeech(recognizedText)
                }
            }

            onSpeechError = { errorMsg ->
                runOnUiThread {
                    tvStatus.text = errorMsg
                    soundManager.playCuriousChirp()
                    eyeView.setExpression(WalleExpression.CURIOUS)
                    clockHandler.postDelayed({ resetToIdleState() }, 2000)
                }
            }
        }
    }

    private fun initSensors() {
        shakeDetector = ShakeDetector(
            context = this,
            onShakeDetected = {
                runOnUiThread {
                    if (prefs.isShakeEnabled) {
                        handleDeviceShake()
                    }
                }
            },
            onTiltChanged = { tiltX, tiltY ->
                runOnUiThread {
                    eyeView.setTiltOffset(tiltX, tiltY)
                }
            }
        )
    }

    private fun onScreenTapped() {
        soundManager.playTap()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        startListeningFlow()
    }

    private fun startListeningFlow() {
        ttsHelper.stop()
        tvSpeechSubtitle.visibility = View.GONE
        tvStatus.setText(R.string.status_listening)
        eyeView.setExpression(WalleExpression.LISTENING)
        speechHelper.startListening()
    }

    private fun handleUserSpeech(userInput: String) {
        tvSpeechSubtitle.text = "\"$userInput\""
        tvSpeechSubtitle.visibility = View.VISIBLE
        tvStatus.setText(R.string.status_thinking)
        eyeView.setExpression(WalleExpression.THINKING)

        val apiKey = prefs.geminiApiKey
        if (apiKey.isBlank()) {
            tvStatus.setText(R.string.gemini_api_key_prompt)
            soundManager.playCuriousChirp()
            eyeView.setExpression(WalleExpression.CURIOUS)
            ttsHelper.speak("Tolong pasang API Key Gemini dulu dengan tekan lama layar.")
            return
        }

        lifecycleScope.launch {
            val result = geminiClient.query(userInput)
            result.onSuccess { aiResponse ->
                val targetExpression = WalleExpression.fromString(aiResponse.expression)
                eyeView.setExpression(targetExpression)

                when (targetExpression) {
                    WalleExpression.HAPPY -> soundManager.playHappyChirp()
                    WalleExpression.CURIOUS -> soundManager.playCuriousChirp()
                    WalleExpression.SURPRISED -> soundManager.playSurprise()
                    else -> {}
                }

                tvStatus.setText(R.string.status_speaking)
                tvSpeechSubtitle.text = aiResponse.reply
                ttsHelper.speak(aiResponse.reply)
            }.onFailure { error ->
                tvStatus.text = error.message ?: "Koneksi bermasalah"
                soundManager.playCuriousChirp()
                eyeView.setExpression(WalleExpression.CURIOUS)
                ttsHelper.speak("Maaf, Wall-E sedang kesulitan terhubung.")
            }
        }
    }

    private fun handleDeviceShake() {
        soundManager.playSurprise()
        eyeView.setExpression(WalleExpression.SURPRISED)
        tvStatus.text = "Wah, goyang!"
        clockHandler.postDelayed({ resetToIdleState() }, 2200)
    }

    private fun resetToIdleState() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val defaultExpr = if (hour >= 22 || hour < 6) WalleExpression.SLEEPY else WalleExpression.IDLE
        eyeView.setExpression(defaultExpr)
        tvStatus.setText(R.string.prompt_tap_to_talk)
        tvSpeechSubtitle.visibility = View.GONE
    }

    private fun startClockLoop() {
        val clockRunnable = object : Runnable {
            override fun run() {
                val now = Calendar.getInstance().time
                tvClock.text = timeFormat.format(now)
                clockHandler.postDelayed(this, 1000)
            }
        }
        clockHandler.post(clockRunnable)
    }

    private fun showApiKeyDialog() {
        val input = EditText(this).apply {
            hint = "Tempel Gemini API Key di sini"
            setText(prefs.geminiApiKey)
            setSingleLine(true)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("Pengaturan Gemini API")
            .setMessage("Masukkan API Key gratis dari Google AI Studio:")
            .setView(container)
            .setPositiveButton("Simpan") { _, _ ->
                val key = input.text.toString().trim()
                prefs.geminiApiKey = key
                Toast.makeText(this, "API Key berhasil disimpan", Toast.LENGTH_SHORT).show()
                soundManager.playHappyChirp()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        shakeDetector.start()
    }

    override fun onPause() {
        super.onPause()
        shakeDetector.stop()
        speechHelper.stopListening()
        ttsHelper.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        clockHandler.removeCallbacksAndMessages(null)
        shakeDetector.stop()
        speechHelper.destroy()
        ttsHelper.shutdown()
    }
}
