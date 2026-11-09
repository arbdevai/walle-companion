package com.walle.companion.data.preferences

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value.trim()).apply()

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var isShakeEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHAKE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SHAKE_ENABLED, value).apply()

    companion object {
        private const val PREFS_NAME = "walle_companion_prefs"
        private const val KEY_GEMINI_API_KEY = "key_gemini_api_key"
        private const val KEY_SOUND_ENABLED = "key_sound_enabled"
        private const val KEY_SHAKE_ENABLED = "key_shake_enabled"
    }
}
