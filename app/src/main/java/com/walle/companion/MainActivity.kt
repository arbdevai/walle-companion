package com.walle.companion

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
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
import com.walle.companion.ui.DigitalClockView
import com.walle.companion.ui.WalleEyeView
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var eyeView: WalleEyeView
    private lateinit var digitalClockView: DigitalClockView
    private lateinit var tvStatus: TextView
    private lateinit var tvSpeechSubtitle: TextView
    private lateinit var rootContainer: View

    private lateinit var btnChangeColor: TextView
    private lateinit var btnChangeClockStyle: TextView
    private lateinit var btnQuickChat: TextView

    private lateinit var prefs: AppPreferences
    private lateinit var soundManager: SoundEffectManager
    private lateinit var ttsHelper: TextToSpeechHelper
    private lateinit var speechHelper: SpeechRecognizerHelper
    private lateinit var shakeDetector: ShakeDetector
    private lateinit var geminiClient: GeminiClient
    private lateinit var gestureDetector: GestureDetector

    private val resetHandler = Handler(Looper.getMainLooper())

    // Theme Color Palette
    private val colorPalette = intArrayOf(
        Color.parseColor("#00E5FF"), // Electric Cyan (Default)
        Color.parseColor("#FFB300"), // Warm Amber
        Color.parseColor("#00E676"), // Emerald Green
        Color.parseColor("#D500F9"), // Cyber Violet
        Color.parseColor("#FF1744"), // Ruby Red
        Color.parseColor("#FFFFFF")  // Pure White
    )
    private var currentColorIndex = 0

    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListeningFlow()
        } else {
            showInteractiveChatDialog(defaultPrompt = null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Global crash guard to prevent abrupt Force Close on legacy Android runtimes
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("WalleCompanion", "Fatal crash on thread ${thread.name}", throwable)
            try {
                val crashFile = java.io.File(filesDir, "last_crash.log")
                crashFile.writeText("${throwable.javaClass.name}: ${throwable.message}\n${throwable.stackTraceToString()}")
            } catch (_: Throwable) {
            }
        }

        setContentView(R.layout.activity_main)

        prefs = AppPreferences(this)
        soundManager = SoundEffectManager { prefs.isSoundEnabled }
        geminiClient = GeminiClient { prefs.geminiApiKey }

        initViews()
        initGestures()
        initAudioAndSpeech()
        initSensors()

        // Check if there was a previous crash to notify developer
        try {
            val crashFile = java.io.File(filesDir, "last_crash.log")
            if (crashFile.exists()) {
                val lastError = crashFile.readLines().firstOrNull().orEmpty()
                if (lastError.isNotEmpty()) {
                    tvStatus.text = "Info crash sebelumnya: $lastError"
                }
            }
        } catch (_: Throwable) {
        }
    }

    private fun initViews() {
        eyeView = findViewById(R.id.walleEyeView)
        digitalClockView = findViewById(R.id.digitalClockView)
        tvStatus = findViewById(R.id.tvStatus)
        tvSpeechSubtitle = findViewById(R.id.tvSpeechSubtitle)
        rootContainer = findViewById(R.id.rootContainer)

        btnChangeColor = findViewById(R.id.btnChangeColor)
        btnChangeClockStyle = findViewById(R.id.btnChangeClockStyle)
        btnQuickChat = findViewById(R.id.btnQuickChat)

        eyeView.onBlinkListener = {
            soundManager.playBlink()
        }

        btnChangeColor.setOnClickListener {
            cycleColorPalette()
        }

        btnChangeClockStyle.setOnClickListener {
            digitalClockView.cycleNextStyle()
            soundManager.playTap()
        }

        btnQuickChat.setOnClickListener {
            onTriggerInteraction()
        }

        rootContainer.setOnLongClickListener {
            showApiKeyDialog()
            true
        }
    }

    private fun initGestures() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onTriggerInteraction()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                handleDoubleTapReaction()
                return true
            }
        })

        eyeView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun cycleColorPalette() {
        currentColorIndex = (currentColorIndex + 1) % colorPalette.size
        val newColor = colorPalette[currentColorIndex]
        eyeView.setEyeColor(newColor)
        digitalClockView.setColor(newColor)
        soundManager.playTap()
    }

    private fun handleDoubleTapReaction() {
        soundManager.playGiggle()
        eyeView.setExpression(WalleExpression.LAUGHING)
        tvStatus.text = "Geli haha!"

        resetHandler.postDelayed({
            eyeView.setExpression(WalleExpression.WINK)
            soundManager.playHappyChirp()
        }, 600)

        resetHandler.postDelayed({
            resetToIdleState()
        }, 1600)
    }

    private fun initAudioAndSpeech() {
        ttsHelper = TextToSpeechHelper(this) { success ->
            if (success) {
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
                    handleUserQuery(recognizedText)
                }
            }

            onSpeechError = { errorMsg ->
                runOnUiThread {
                    tvStatus.text = errorMsg
                    soundManager.playCuriousChirp()
                    eyeView.setExpression(WalleExpression.CURIOUS)
                    resetHandler.postDelayed({ resetToIdleState() }, 2000)
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

    private fun onTriggerInteraction() {
        soundManager.playTap()

        if (speechHelper.isAvailable()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                return
            }
            startListeningFlow()
        } else {
            // SpeechRecognizer unavailable on legacy/custom ROM -> Open interactive dialog
            showInteractiveChatDialog(defaultPrompt = null)
        }
    }

    private fun startListeningFlow() {
        ttsHelper.stop()
        tvSpeechSubtitle.visibility = View.GONE
        tvStatus.setText(R.string.status_listening)
        eyeView.setExpression(WalleExpression.LISTENING)
        speechHelper.startListening()
    }

    private fun showInteractiveChatDialog(defaultPrompt: String?) {
        val quickPrompts = arrayOf(
            "Jam berapa sekarang?",
            "Ceritakan lelucon lucu",
            "Kamu robot apa?",
            "Sapa aku dong!",
            "Tidur dulu ya"
        )

        val input = EditText(this).apply {
            hint = "Ketik pertanyaan..."
            if (!defaultPrompt.isNullOrBlank()) {
                setText(defaultPrompt)
            }
            setSingleLine(true)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("Ajak Ngobrol Wall-E")
            .setItems(quickPrompts) { _, which ->
                val chosen = quickPrompts[which]
                handleUserQuery(chosen)
            }
            .setView(container)
            .setPositiveButton("Kirim") { _, _ ->
                val userText = input.text.toString().trim()
                if (userText.isNotEmpty()) {
                    handleUserQuery(userText)
                }
            }
            .setNeutralButton("Gunakan Suara") { _, _ ->
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    startListeningFlow()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun handleUserQuery(userInput: String) {
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
                    WalleExpression.LAUGHING -> soundManager.playGiggle()
                    WalleExpression.CURIOUS -> soundManager.playCuriousChirp()
                    WalleExpression.SURPRISED -> soundManager.playSurprise()
                    WalleExpression.DIZZY -> soundManager.playDizzy()
                    WalleExpression.WINK -> soundManager.playHappyChirp()
                    else -> {}
                }

                tvStatus.setText(R.string.status_speaking)
                tvSpeechSubtitle.text = aiResponse.reply
                ttsHelper.speak(aiResponse.reply)
            }.onFailure { error ->
                tvStatus.text = error.message ?: "Koneksi bermasalah"
                soundManager.playCuriousChirp()
                eyeView.setExpression(WalleExpression.CURIOUS)
                ttsHelper.speak("Maaf, Wall-E kesulitan terhubung.")
            }
        }
    }

    private fun handleDeviceShake() {
        soundManager.playDizzy()
        eyeView.setExpression(WalleExpression.DIZZY)
        tvStatus.text = "Aduh, pusing!"
        resetHandler.postDelayed({ resetToIdleState() }, 2400)
    }

    private fun resetToIdleState() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val defaultExpr = if (hour >= 22 || hour < 6) WalleExpression.SLEEPY else WalleExpression.IDLE
        eyeView.setExpression(defaultExpr)
        tvStatus.setText(R.string.prompt_tap_to_talk)
        tvSpeechSubtitle.visibility = View.GONE
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
        resetHandler.removeCallbacksAndMessages(null)
        shakeDetector.stop()
        speechHelper.destroy()
        ttsHelper.shutdown()
    }
}
